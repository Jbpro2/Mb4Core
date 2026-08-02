package com.service.xray;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.penguinehis.ultrasshservice.SocksReviveService;
import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.tunnel.vpn.Tun2SocksUdpRelay;
import com.penguinehis.ultrasshservice.tunnel.vpn.CIDRIP;
import com.penguinehis.ultrasshservice.tunnel.vpn.NetworkSpace;
import com.penguinehis.ultrasshservice.R;
import com.penguinehis.ultrasshservice.tunnel.vpn.VpnUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;

/**
 * XRAY (libv2ray) VPN service that:
 * - Starts Xray-core (gomobile AAR) via reflection
 * - Waits for local SOCKS port
 * - Starts existing Tun2Socks runner from your project
 */
public class XrayVpnService extends VpnService {

    public static final String ACTION_START = "com.penguinehis.ultrasshservice.XRAY_START";
    public static final String ACTION_STOP  = "com.penguinehis.ultrasshservice.XRAY_STOP";

    private static final String TAG = "XrayVpnService";
    private static final int NOTIF_ID = 1001;
    private static final String NOTIF_CHANNEL_ID = "xray_vpn";
    // Must match XrayLinkParser defaults.
    private static final int LOCAL_SOCKS_PORT = 10808;
    private VpnUtils.PrivateAddress mPrivateAddress;
    private static final int MTU = 1280; // IPv6 minimum MTU, safest on mobile networks
    private static final String VPN_NETMASK_V4 = "255.255.255.0";


    // Optional IPv6 address for dual-stack routing through the VPN
    private static final String VPN_ADDR_V6 = "fd00:1:fd00:1::1";
    private static final String VPN_ROUTER_IPV6_ADDRESS = "fd00:1:fd00:1::2";
    private static final int VPN_PREFIX_V6 = 64;
private static final long CORE_STARTUP_TIMEOUT_SEC = 30L;

    @SuppressWarnings("FieldCanBeLocal")
    private static volatile XrayVpnService serviceInstance;

    /**
     * Called by Go (via gomobile) to protect sockets from being routed into the VPN.
     */
    @SuppressWarnings("unused")
    public static boolean protectFd(int fd) {
        XrayVpnService inst = serviceInstance;
        if (inst == null) {
            Log.e(TAG, "protectFd: serviceInstance is null");
            return false;
        }
        return inst.protect(fd);
    }

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Server IPs excluded from the VPN routes to avoid routing loops when the core
     * (or its DNS/direct outbounds) are not protected on specific devices/ROMs.
     *
     * <p>We compute these IPs from the XRAY JSON config before establishing the VPN.
     */
    private volatile String[] excludedServerIps = new String[0];

    private ParcelFileDescriptor vpnInterface;
    private Tun2SocksUdpRelay tun2Socks;
    private Thread workerThread;

    // Core state
    private boolean coreStarted = false;
    private boolean goSeqInited = false;
    private Object controllerObj;
    private CountDownLatch coreReadyLatch;

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopAll();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        serviceInstance = this;

        IntentFilter f = new IntentFilter();
        f.addAction(SocksReviveService.TUNNEL_SSH_STOP_SERVICE);
        f.addAction(ACTION_STOP);

        LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver, f);
        SkStatus.logInfo("XrayVpnService criado");
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopReceiver);
        stopAll();
        serviceInstance = null;
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        stopAll();
        super.onRevoke();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (action == null || ACTION_START.equals(action)) {
            if (!running.get()) {
                startAll();
            } else {
                SkStatus.logInfo("XRAY já está em execução");
            }
        } else if (ACTION_STOP.equals(action)) {
            stopAll();
        }

        return START_STICKY;
    }

    // -------------------------------------------------------------------------
    // Start/Stop orchestration
    // -------------------------------------------------------------------------

    private void startAll() {
        if (!running.compareAndSet(false, true)) return;

        startForegroundNotification("XRAY VPN");
        SkStatus.updateStateString(SkStatus.SSH_CONECTANDO, "Iniciando XRAY");

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = new Settings(XrayVpnService.this);
                    String rawConfig = settings.getPrefsPrivate().getString(Settings.XRAY_CONFIG_KEY, "");
                    String uuidOverride = settings.getPrefsPrivate().getString(Settings.XRAY_UUID_KEY, "");

                    String envPath = getFilesDir().getAbsolutePath();
                    String configJson = resolvePanelConfigToXrayJson(rawConfig, uuidOverride);

                    // Compute and store the upstream server IPs so we can exclude them from
                    // the VPN routes (defensive against routing loops on real devices).
                    excludedServerIps = resolveServerIpsFromXrayConfig(configJson);
                   // SkStatus.logInfo("XRAY excludeIps=" + Arrays.toString(excludedServerIps));

                    startCore(configJson, envPath, "");

                    if (!waitForSocksReady()) {
                        SkStatus.logInfo("Timeout aguardando SOCKS local " + LOCAL_SOCKS_PORT);
                        SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, "Falha ao iniciar XRAY");
                        stopAll();
                        return;
                    }

                    setupVpnInterface(excludedServerIps);
                    startTun2Socks();

                    SkStatus.updateStateString(SkStatus.SSH_CONECTADO, "VPN conectada");

                } catch (Throwable t) {
                    Log.e(TAG, "Erro ao iniciar XRAY VPN", t);
                    SkStatus.logInfo("Erro XRAY: " + (t.getMessage() != null ? t.getMessage() : ""));
                    SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, "Erro: " + (t.getMessage() != null ? t.getMessage() : ""));
                    stopAll();
                }
            }
        }, "xray-vpn-worker");

        workerThread.start();
    }

    private synchronized void stopAll() {
        if (!running.getAndSet(false)) return;

        SkStatus.logInfo("Parando XRAY...");

        // stop tun2socks
        if (tun2Socks != null) {

            try {

                tun2Socks.interrupt();

                try { tun2Socks.join(1500); } catch (Throwable ignored) { }

            } catch (Throwable ignored) { }

            tun2Socks = null;

        }

        // close vpn interface
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) { }
            vpnInterface = null;
        }

        // stop core
        stopCore();

        // stop worker
        if (workerThread != null) {
            try {
                workerThread.interrupt();
            } catch (Throwable ignored) { }
            workerThread = null;
        }

        try {
            stopForeground(true);
        } catch (Throwable ignored) { }

        SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, "Desconectado");
        stopSelf();
    }



/**
 * Returns DNS servers of the underlying (non-VPN) active network.
 * These are typically the resolvers used by the OS before the VPN comes up.
 * Excluding them from VPN routes prevents XRAY's own resolver/direct-DNS sockets
 * from being captured by the VPN on devices where protect()/disallow-app is unreliable.
 */
private List<String> getSystemDnsServerStrings() {
    final List<String> out = new ArrayList<>();
    try {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return out;
        Network n = cm.getActiveNetwork();
        if (n == null) return out;
        LinkProperties lp = cm.getLinkProperties(n);
        if (lp == null) return out;
        for (InetAddress a : lp.getDnsServers()) {
            if (a == null) continue;
            String ip = a.getHostAddress();
            if (ip != null && !ip.trim().isEmpty()) out.add(ip.trim());
        }
    } catch (Throwable t) {
        SkStatus.logInfo("XRAY could not read system DNS: " + (t.getMessage() != null ? t.getMessage() : ""));
    }
    return out;
}
// -------------------------------------------------------------------------
    // VPN interface + Tun2Socks
    // -------------------------------------------------------------------------

    private static String stripBracketsAndZone(String s) {
        if (s == null) return "";
        s = s.trim();
        // Strip [ ] (e.g. URLs like [2001:db8::1])
        if (s.startsWith("[") && s.endsWith("]") && s.length() > 2) {
            s = s.substring(1, s.length() - 1);
        }
        // Strip zone id (e.g. fe80::1%wlan0)
        int pct = s.indexOf('%');
        if (pct >= 0) s = s.substring(0, pct);
        return s.trim();
    }
    private void setupVpnInterface(String[] excludeIps) {
        // Close previous interface if any
        try {
            mPrivateAddress = VpnUtils.selectPrivateAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) { }
            vpnInterface = null;
        }

        final Builder builder = new Builder()
                .setSession("XRAY VPN")
                .setMtu(MTU)
                .addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength)
                .allowFamily(AF_INET)
                .allowFamily(AF_INET6);

        // ------------------------------
        // IPv4 routes (exclude server)
        // ------------------------------
        final NetworkSpace routes = new NetworkSpace();
        routes.addIP(new CIDRIP("0.0.0.0", 0), true);

        // Keep local/LAN routes out of the tunnel (optional but consistent with Tunnel.java)
        routes.addIP(new CIDRIP("10.0.0.0", 8), false);
        routes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), false);


final List<String> excludedIpv6 = new ArrayList<>();

// Exclude the underlying system DNS servers (from the active, non-VPN network) from VPN routes.
// XRAY's core may use the system resolver or "direct" DNS sockets; on some devices those sockets
// get captured by the VPN even when protect()/addDisallowedApplication() is used, creating a loop.
final List<String> systemDns = getSystemDnsServerStrings();
if (!systemDns.isEmpty()) {
    //SkStatus.logInfo("XRAY system DNS detected: " + systemDns);
    for (String dnsIp : systemDns) {
        if (dnsIp == null) continue;
        String s = dnsIp.trim();
        if (s.isEmpty()) continue;
        if (s.contains("151.244.242.6") || s.contains("1.0.0.1") )  {
            continue;
        }
        if (isIpv4Literal(s)) {
            routes.addIP(new CIDRIP(s, 32), false);
        } else if (isIpv6Literal(s)) {
            excludedIpv6.add(s);
        }
    }
}
        if (excludeIps != null) {
            for (String ip : excludeIps) {
                if (ip == null) continue;
                String s = ip.trim();
                if (s.isEmpty()) continue;
                if (s.contains("151.244.242.6") || s.contains("1.0.0.1") )  {
                    continue;
                }
                if (isIpv4Literal(s)) {
                    routes.addIP(new CIDRIP(s, 32), false);
                } else if (isIpv6Literal(s)) {
                    excludedIpv6.add(s);
                }
            }
        }

        // Install the computed positive IPv4 routes.
        final NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);
        for (NetworkSpace.IpAddress r : routes.getPositiveIPList()) {
            try {
                if (!multicastRange.containsNet(r)) {
                    builder.addRoute(r.getIPv4Address(), r.networkMask);
                }
            } catch (Throwable ignored) {
            }
        }

        // ------------------------------
        // IPv6 routes (exclude server)
        // ------------------------------
        boolean ipv6Configured = false;
        try {
            builder.addAddress(VPN_ADDR_V6, VPN_PREFIX_V6);
            ipv6Configured = true;

            if (!excludedIpv6.isEmpty()) {
               // SkStatus.logInfo("XRAY excluding IPv6 from VPN routes: " + excludedIpv6);
                addIpv6RoutesExcludingAddresses(builder, excludedIpv6);
            } else {
                builder.addRoute("::", 0);
            }
        } catch (Throwable t) {
            // Device/network does not support IPv6 for the VPN interface; continue IPv4-only.
            SkStatus.logInfo("XRAY IPv6 VPN disabled: " + (t.getMessage() != null ? t.getMessage() : ""));
        }

        // DNS for the VPN. If IPv6 routing is not installed, do not advertise an IPv6 DNS.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }

        // Best-effort: exclude this app (and therefore the core) from the VPN routing.
        // Even when this works, we still keep the route-exclusion above as a defensive fallback.

        vpnInterface = builder.establish();
        if (vpnInterface == null) {
            throw new IllegalStateException("Falha ao estabelecer interface VPN");
        }
    }

    private void startTun2Socks() {
        final ParcelFileDescriptor vIntf = vpnInterface;
        if (vIntf == null) throw new IllegalStateException("VPN interface indisponível");

        Tun2SocksUdpRelay t = new Tun2SocksUdpRelay(
                this,
                vIntf,
                MTU,
                mPrivateAddress.mRouter,
                VPN_ROUTER_IPV6_ADDRESS,
                VPN_NETMASK_V4,
                "127.0.0.1:" + LOCAL_SOCKS_PORT,
                null,
                1024
        );

        t.setOnTun2SocksListener(new Tun2SocksUdpRelay.OnTun2SocksListener() {
            @Override
            public void onStart() {
                SkStatus.logInfo("tun2socks iniciado");
            }

            @Override
            public void onStop() {
                SkStatus.logInfo("tun2socks finalizado");
                if (running.get()) {
                    stopAll();
                }
            }
        });

        tun2Socks = t;
        t.start();
    }

    // -------------------------------------------------------------------------
    // SOCKS readiness
    // -------------------------------------------------------------------------

    private boolean waitForSocksReady() {
        try {
            if (coreReadyLatch != null) {
                coreReadyLatch.await(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ignored) { }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < CORE_STARTUP_TIMEOUT_SEC * 1000L) {
            if (!running.get()) return false;
            if (isSocksReady()) return true;
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) { }
        }
        return false;
    }

    private boolean isSocksReady() {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1", LOCAL_SOCKS_PORT), 500);
            return s.isConnected();
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (s != null) {
                try { s.close(); } catch (Throwable ignored) { }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Route exclusion helpers (avoid routing loops)
    // -------------------------------------------------------------------------

    private static boolean isIpv4Literal(String s) {
        if (s == null) return false;
        if (s.indexOf(':') >= 0) return false;
        try {
            InetAddress ia = InetAddress.getByName(s);
            return ia.getAddress() != null && ia.getAddress().length == 4;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isIpv6Literal(String s) {
        if (s == null) return false;
        if (s.indexOf(':') < 0) return false;
        try {
            InetAddress ia = InetAddress.getByName(s);
            return ia instanceof Inet6Address;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Extracts the upstream server hostname from the XRAY JSON and resolves it to A/AAAA,
     * returning a unique list of IP literals.
     */
    private static String[] resolveServerIpsFromXrayConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new String[0];
        }
        try {
            JSONObject root = new JSONObject(configJson);
            JSONArray outbounds = root.optJSONArray("outbounds");
            if (outbounds == null || outbounds.length() == 0) {
                return new String[0];
            }

            // Prefer the outbound tagged "proxy"; otherwise default to the first outbound.
            JSONObject proxy = null;
            for (int i = 0; i < outbounds.length(); i++) {
                JSONObject ob = outbounds.optJSONObject(i);
                if (ob == null) continue;
                String tag = ob.optString("tag", "");
                if ("proxy".equalsIgnoreCase(tag)) {
                    proxy = ob;
                    break;
                }
            }
            if (proxy == null) proxy = outbounds.optJSONObject(0);
            if (proxy == null) return new String[0];

            String host = null;
            JSONObject settings = proxy.optJSONObject("settings");
            if (settings != null) {
                JSONArray vnext = settings.optJSONArray("vnext");
                if (vnext != null && vnext.length() > 0) {
                    JSONObject v0 = vnext.optJSONObject(0);
                    if (v0 != null) host = v0.optString("address", null);
                }
                if (host == null || host.trim().isEmpty()) {
                    JSONArray servers = settings.optJSONArray("servers");
                    if (servers != null && servers.length() > 0) {
                        JSONObject s0 = servers.optJSONObject(0);
                        if (s0 != null) host = s0.optString("address", null);
                    }
                }
            }

            if (host == null) {
                return new String[0];
            }
            host = host.trim();
            if (host.isEmpty()) {
                return new String[0];
            }

            // Collect excludes in a stable, de-duplicated order.
            Set<String> uniq = new LinkedHashSet<>();

            // If the config already contains an IP literal, add it directly.
            if (isIpv4Literal(host) || isIpv6Literal(host)) {
                uniq.add(host);
            } else {
                // Otherwise resolve hostname to A/AAAA before the VPN is established.
                InetAddress[] all = InetAddress.getAllByName(host);
                if (all != null) {
                    for (InetAddress ia : all) {
                        if (ia == null) continue;
                        String ip = ia.getHostAddress();
                        if (ip != null && !ip.trim().isEmpty()) {
                            uniq.add(ip.trim());
                        }
                    }
                }
            }

            // Also exclude literal DNS servers specified in the XRAY JSON (common source of loops
            // when DNS is routed "direct" and sockets are not protected on the device).
            try {
                JSONObject dns = root.optJSONObject("dns");
                if (dns != null) {
                    JSONArray servers = dns.optJSONArray("servers");
                    if (servers != null) {
                        for (int i = 0; i < servers.length(); i++) {
                            String s = servers.optString(i, "");
                            if (s == null) continue;
                            s = s.trim();
                            if (isIpv4Literal(s) || isIpv6Literal(s)) {
                                uniq.add(s);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            return uniq.toArray(new String[0]);
        } catch (Throwable t) {
            return new String[0];
        }
    }

    /**
     * Adds IPv6 routes that cover all IPv6 space except the provided excluded /128 addresses.
     * This is a defensive fallback for devices where VpnService.protect()/disallowed-app is unreliable.
     */
    private static void addIpv6RoutesExcludingAddresses(VpnService.Builder builder, List<String> excludedIpv6) {
        if (builder == null || excludedIpv6 == null || excludedIpv6.isEmpty()) {
            try { builder.addRoute("::", 0); } catch (Throwable ignored) {}
            return;
        }

        // Normalize and de-duplicate.
        final LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String s : excludedIpv6) {
            if (s == null) continue;
            String v = s.trim();
            if (v.isEmpty()) continue;
            if (isIpv6Literal(v)) {
                uniq.add(v);
            }
        }

        if (uniq.isEmpty()) {
            try { builder.addRoute("::", 0); } catch (Throwable ignored) {}
            return;
        }

        try {
            List<byte[]> excluded = new ArrayList<>();
            for (String ip : uniq) {
                InetAddress ia = InetAddress.getByName(ip);
                if (ia instanceof Inet6Address) {
                    excluded.add(ia.getAddress());
                }
            }

            if (excluded.isEmpty()) {
                builder.addRoute("::", 0);
                return;
            }

            // Start with ::/0 and subtract each excluded /128.
            List<Ipv6Prefix> prefixes = new ArrayList<>();
            prefixes.add(new Ipv6Prefix(new byte[16], 0));

            for (byte[] ex : excluded) {
                prefixes = subtractSingleIpv6(prefixes, ex);
                // Safety guard: do not let route list grow without bound.
                if (prefixes.size() > 512) {
                    throw new IllegalStateException("Too many IPv6 routes: " + prefixes.size());
                }
            }

            for (Ipv6Prefix p : prefixes) {
                String addr = InetAddress.getByAddress(p.addr).getHostAddress();
                builder.addRoute(addr, p.prefixLen);
            }
        } catch (Throwable t) {
            // Fallback: route all IPv6 through the VPN.
            try { builder.addRoute("::", 0); } catch (Throwable ignored) {}
        }
    }

    private static final class Ipv6Prefix {
        final byte[] addr; // masked network address
        final int prefixLen;

        Ipv6Prefix(byte[] addr, int prefixLen) {
            this.addr = maskIpv6(addr, prefixLen);
            this.prefixLen = prefixLen;
        }
    }

    private static List<Ipv6Prefix> subtractSingleIpv6(List<Ipv6Prefix> prefixes, byte[] excluded) {
        List<Ipv6Prefix> out = new ArrayList<>();
        for (Ipv6Prefix p : prefixes) {
            if (!ipv6PrefixContains(p.addr, p.prefixLen, excluded)) {
                out.add(p);
            } else {
                excludeFromPrefix(p.addr, p.prefixLen, excluded, out);
            }
        }
        return out;
    }

    private static void excludeFromPrefix(byte[] baseAddr, int baseLen, byte[] excluded, List<Ipv6Prefix> out) {
        if (baseLen >= 128) {
            // This is the excluded /128 itself: drop it.
            return;
        }

        int bit = ipv6GetBit(excluded, baseLen);
        Ipv6Prefix child0 = ipv6Child(baseAddr, baseLen, 0);
        Ipv6Prefix child1 = ipv6Child(baseAddr, baseLen, 1);

        if (bit == 0) {
            out.add(child1);
            excludeFromPrefix(child0.addr, child0.prefixLen, excluded, out);
        } else {
            out.add(child0);
            excludeFromPrefix(child1.addr, child1.prefixLen, excluded, out);
        }
    }

    private static Ipv6Prefix ipv6Child(byte[] baseAddr, int baseLen, int nextBitValue) {
        int childLen = baseLen + 1;
        byte[] child = maskIpv6(baseAddr, baseLen);
        ipv6SetBit(child, baseLen, nextBitValue);
        child = maskIpv6(child, childLen);
        return new Ipv6Prefix(child, childLen);
    }

    private static boolean ipv6PrefixContains(byte[] prefixAddr, int prefixLen, byte[] addr) {
        if (prefixLen <= 0) return true;
        for (int i = 0; i < prefixLen; i++) {
            if (ipv6GetBit(prefixAddr, i) != ipv6GetBit(addr, i)) {
                return false;
            }
        }
        return true;
    }

    private static int ipv6GetBit(byte[] addr, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitInByte = 7 - (bitIndex % 8);
        return (addr[byteIndex] >> bitInByte) & 0x01;
    }

    private static void ipv6SetBit(byte[] addr, int bitIndex, int value) {
        int byteIndex = bitIndex / 8;
        int bitInByte = 7 - (bitIndex % 8);
        int mask = 1 << bitInByte;
        if (value == 0) {
            addr[byteIndex] = (byte) (addr[byteIndex] & ~mask);
        } else {
            addr[byteIndex] = (byte) (addr[byteIndex] | mask);
        }
    }

    private static byte[] maskIpv6(byte[] addr, int prefixLen) {
        byte[] out = addr == null ? new byte[16] : Arrays.copyOf(addr, 16);
        if (prefixLen <= 0) {
            Arrays.fill(out, (byte) 0);
            return out;
        }
        if (prefixLen >= 128) {
            return out;
        }

        int fullBytes = prefixLen / 8;
        int remBits = prefixLen % 8;

        // Zero bytes after the prefix.
        for (int i = fullBytes + (remBits == 0 ? 0 : 1); i < 16; i++) {
            out[i] = 0;
        }

        if (remBits != 0) {
            int mask = 0xFF << (8 - remBits);
            out[fullBytes] = (byte) (out[fullBytes] & mask);
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // libv2ray core (reflection)
    // -------------------------------------------------------------------------

    private void ensureGoMobileInit() {
        if (goSeqInited) return;
        synchronized (this) {
            if (goSeqInited) return;
            try {
                Class<?> seq = Class.forName("go.Seq");
                seq.getMethod("setContext", Context.class).invoke(null, getApplicationContext());
                seq.getMethod("touch").invoke(null);
            } catch (Throwable t) {
                Log.e(TAG, "Falha ao inicializar go.Seq", t);
            }
            goSeqInited = true;
        }
    }

    private synchronized void startCore(String configJson, String envPath, String xudpKey) throws Exception {
        if (coreStarted) return;

        coreReadyLatch = new CountDownLatch(1);

        Class<?> libCls = Class.forName("libv2ray.Libv2ray");
        Class<?> cbIface = Class.forName("libv2ray.CoreCallbackHandler");

        ensureGoMobileInit();

        // enableProtect(): makes core call back into protectFd(fd).
        try {
            invokeStaticByNameIgnoreCase(libCls, "enableProtect", new Object[]{}, new Class<?>[]{});
           // SkStatus.logInfo("Protect habilitado (enableProtect)");
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao habilitar protect", t);
        }

        // initCoreEnv(envPath, xudpKey) OR initCoreEnv(envPath)
        try {
            invokeStaticByNameIgnoreCase(libCls, "initCoreEnv", new Object[]{envPath, xudpKey}, new Class<?>[]{String.class, String.class});
        } catch (NoSuchMethodException e) {
            invokeStaticByNameIgnoreCase(libCls, "initCoreEnv", new Object[]{envPath}, new Class<?>[]{String.class});
        }

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                String name = method.getName() != null ? method.getName().toLowerCase() : "";
                if ("startup".equals(name)) {
                    SkStatus.logInfo("XRAY core Startup");
                    if (coreReadyLatch != null) coreReadyLatch.countDown();
                    return 0L;
                }
                if ("shutdown".equals(name)) {
                    SkStatus.logInfo("XRAY core Shutdown");
                    return 0L;
                }
                if ("onemitstatus".equals(name)) {
                    long code = 0L;
                    String msg = "";
                    if (args != null && args.length > 0 && args[0] instanceof Number) code = ((Number) args[0]).longValue();
                    if (args != null && args.length > 1 && args[1] instanceof String) msg = (String) args[1];
                    SkStatus.logInfo("XRAY status [" + code + "] " + msg);
                    return 0L;
                }
                return 0L;
            }
        };

        Object cbProxy = Proxy.newProxyInstance(cbIface.getClassLoader(), new Class<?>[]{cbIface}, handler);

        Object controller = invokeStaticReturnByNameIgnoreCase(libCls, "newCoreController", new Object[]{cbProxy}, new Class<?>[]{cbIface});
        if (controller == null) throw new IllegalStateException("newCoreController retornou null");

        invokeInstanceByNameIgnoreCase(controller, "startLoop", new Object[]{configJson}, new Class<?>[]{String.class});

        controllerObj = controller;
        coreStarted = true;
        SkStatus.logInfo("XRAY core startLoop chamado");
    }

    private synchronized void stopCore() {
        if (!coreStarted) return;

        Object ctrl = controllerObj;
        controllerObj = null;
        coreReadyLatch = null;

        if (ctrl != null) {
            try {
                invokeInstanceByNameIgnoreCase(ctrl, "stopLoop", new Object[]{}, new Class<?>[]{});
            } catch (Throwable t) {
                Log.e(TAG, "Erro ao parar XRAY core", t);
            }
        }

        coreStarted = false;
    }

    private static void invokeStaticByNameIgnoreCase(Class<?> cls, String methodName, Object[] args, Class<?>[] argTypes) throws Exception {
        java.lang.reflect.Method m = null;
        for (java.lang.reflect.Method mm : cls.getMethods()) {
            if (mm.getName().equalsIgnoreCase(methodName) && sameParamTypes(mm.getParameterTypes(), argTypes)) {
                m = mm;
                break;
            }
        }
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "." + methodName);
        }
        try {
            m.invoke(null, args);
        } catch (Throwable t) {
            throw unwrapInvocation(t);
        }
    }

    private static Object invokeStaticReturnByNameIgnoreCase(Class<?> cls, String methodName, Object[] args, Class<?>[] argTypes) throws Exception {
        java.lang.reflect.Method m = null;
        for (java.lang.reflect.Method mm : cls.getMethods()) {
            if (mm.getName().equalsIgnoreCase(methodName) && sameParamTypes(mm.getParameterTypes(), argTypes)) {
                m = mm;
                break;
            }
        }
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "." + methodName);
        }
        try {
            return m.invoke(null, args);
        } catch (Throwable t) {
            throw unwrapInvocation(t);
        }
    }

    private static Object invokeInstanceByNameIgnoreCase(Object obj, String methodName, Object[] args, Class<?>[] argTypes) throws Exception {
        Class<?> cls = obj.getClass();
        java.lang.reflect.Method m = null;
        for (java.lang.reflect.Method mm : cls.getMethods()) {
            if (mm.getName().equalsIgnoreCase(methodName) && sameParamTypes(mm.getParameterTypes(), argTypes)) {
                m = mm;
                break;
            }
        }
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "." + methodName);
        }
        try {
            return m.invoke(obj, args);
        } catch (Throwable t) {
            throw unwrapInvocation(t);
        }
    }

    private static boolean sameParamTypes(Class<?>[] a, Class<?>[] b) {
        if (a == null) a = new Class<?>[]{};
        if (b == null) b = new Class<?>[]{};
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) return false;
        }
        return true;
    }

    private static Exception unwrapInvocation(Throwable t) {
        if (t instanceof InvocationTargetException) {
            Throwable te = ((InvocationTargetException) t).getTargetException();
            if (te != null) {
                if (te instanceof Exception) return (Exception) te;
                return new Exception(te);
            }
        }
        if (t instanceof Exception) return (Exception) t;
        return new Exception(t);
    }

    // -------------------------------------------------------------------------
    // Foreground Notification
    // -------------------------------------------------------------------------

    private void startForegroundNotification(String title) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "XRAY VPN",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }

        Notification notif = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText("VPN ativa")
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    // -------------------------------------------------------------------------
    // Panel config normalization (base64, vmess, vless, json)
    // -------------------------------------------------------------------------

    private String resolvePanelConfigToXrayJson(String raw, String uuidOverride) throws JSONException {
        String input = raw == null ? "" : raw.trim();
        if (input.isEmpty() || "null".equalsIgnoreCase(input)) {
            throw new IllegalArgumentException("XRAY config_v2ray vazio");
        }

        // If it's already a link (vmess://, vless://, json:// etc)
        if (input.contains("://")) {
            return XrayLinkParser.parseToXrayJson(input, LOCAL_SOCKS_PORT, 8, uuidOverride);
        }

        // Try base64 decode: sometimes the panel stores base64("vmess://....") or base64(JSON)
        String decoded = tryDecodeBase64(input);
        if (decoded != null) {
            decoded = decoded.trim();
            if (!decoded.isEmpty()) {
                if (decoded.contains("://")) {
                    return XrayLinkParser.parseToXrayJson(decoded, LOCAL_SOCKS_PORT, 8, uuidOverride);
                }
                if (decoded.startsWith("{") && decoded.contains("\"inbounds\"") && decoded.contains("\"outbounds\"")) {
                    // Full XRAY JSON already
                    return decoded;
                }
                // If decoded is a VMess JSON object, treat original as vmess payload.
                if (decoded.startsWith("{") && decoded.contains("\"id\"") && decoded.contains("\"add\"")) {
                    return XrayLinkParser.parseToXrayJson("vmess://" + input, LOCAL_SOCKS_PORT, 8, uuidOverride);
                }
            }
        }

        // Last fallback: assume it's the base64 payload of a VMess link.
        return XrayLinkParser.parseToXrayJson("vmess://" + input, LOCAL_SOCKS_PORT, 8, uuidOverride);
    }

    private String tryDecodeBase64(String s) {
        if (s == null) return null;

        int[] candidates = new int[] {
                Base64.DEFAULT,
                Base64.NO_WRAP,
                Base64.URL_SAFE | Base64.NO_WRAP,
                Base64.URL_SAFE
        };

        for (int flags : candidates) {
            try {
                byte[] bytes = Base64.decode(s, flags);
                String out = new String(bytes, StandardCharsets.UTF_8);
                if (!out.trim().isEmpty()) return out;
            } catch (Throwable ignored) { }
        }
        return null;
    }
}