/*
 * Copyright (c) 2015, Psiphon Inc.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.penguinehis.ultrasshservice.tunnel.vpn;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.penguinehis.ultrasshservice.SocksReviveService;
import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.logger.SkStatus;

/**
 * Baseado no Cordova Plugin Tun2Socks
 *
 * @author dFiR30n
 */

public class Tunnel {

    public interface HostService {
        public String getAppName();

        public Context getContext();

        // Object must be a VpnService; Android < 4 cannot reference this class name
        public Object getVpnService();

        // Object must be a VpnService.Builder;
        // Android < 4 cannot reference this class name
        public Object newVpnServiceBuilder();

        public void onDiagnosticMessage(String message);

        public void onTunnelConnected();

        public void onVpnEstablished();
    }

    private final HostService mHostService;
    private VpnUtils.PrivateAddress mPrivateAddress;
    private AtomicReference<ParcelFileDescriptor> mTunFd;
    private AtomicBoolean mRoutingThroughTunnel;
    private Tun2Socks mTun2Socks;
    private Pdnsd mPdnsd;
    private NetworkSpace mRoutes;
    private Settings mConfig;
    // Only StartNET VPNService instance may exist at a time, as the underlying
    // tun2socks implementation contains global state.
    private static Tunnel mTunnel;

    public static synchronized Tunnel newTunnel(HostService hostService) {
        if (mTunnel != null) {
            mTunnel.stop();
        }
        mTunnel = new Tunnel(hostService);
        return mTunnel;
    }

    private Tunnel(HostService hostService) {
        mHostService = hostService;
        mTunFd = new AtomicReference<ParcelFileDescriptor>();
        mRoutingThroughTunnel = new AtomicBoolean(false);
        mRoutes = new NetworkSpace();

        //org.uproxy.tun2socks.Tun2SocksJni.init();
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    //----------------------------------------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------------------------------------

    // To start, call in sequence: startRouting(), then startTunneling(). After startRouting()
    // succeeds, the caller must call stop() to clean up.

    // Returns true when the VPN routing is established; returns false if the VPN could not
    // be started due to lack of prepare or revoked permissions (called should re-prepare and
    // try again); throws exception for other error conditions.
    public synchronized boolean startRouting(TunnelVpnSettings settings) throws java.lang.Exception {
        return startVpn(settings.mDnsForward, settings.mDnsResolver, settings.mExcludeIps,
                settings.mEnableFilterApps, settings.mFilterBypassMode, settings.mFilterApps, settings.mTetheringSubnet);
    }

    // Starts tun2socks. Returns true on success.
    public synchronized boolean startTunneling(String socksServerAddress, String[] dnsResolver, boolean forwardDns, String udpResolver, boolean udpDnsRelay)
            throws Exception {
        return routeThroughTunnel(socksServerAddress, dnsResolver, forwardDns, udpResolver, udpDnsRelay);
    }

    // Stops routing traffic through the tunnel by stopping tun2socks.
    // The VPN is unaffected by this method.
    public synchronized void stopTunneling() {
        stopRoutingThroughTunnel();
    }

    // Note: to avoid deadlock, do not call directly from a HostService callback;
    // instead post to a Handler if necessary to trigger from a HostService callback.
    public synchronized void stop() {
        stopVpn();
    }

    //----------------------------------------------------------------------------
    // VPN Routing
    //----------------------------------------------------------------------------

    private static final String VPN_INTERFACE_NETMASK = "255.255.255.0";
    // IPv6 addressing for the TUN interface and the internal tun2socks router.
    // Using an RFC4193 ULA prefix avoids collisions with real networks.
    private static final String VPN_INTERFACE_IPV6_ADDRESS = "fd00:1:fd00:1::1";
    private static final int VPN_INTERFACE_IPV6_PREFIX_LENGTH = 64;
    private static final String VPN_ROUTER_IPV6_ADDRESS = "fd00:1:fd00:1::2";
    private static final String DNS_RESOLVER_IP = "8.8.8.8";
    private static final int DNS_RESOLVER_PORT = 53;
    //private String dns6 = "0";
    private int mMtu = 1500;

    // Note: Atomic variables used for getting/setting local proxy port, routing flag, and
    // tun fd, as these functions may be called via callbacks. Do not use
    // synchronized functions as stop() is synchronized and a deadlock is possible as callbacks
    // can be called while stop holds the lock.
    //
    private boolean startVpn(boolean forwardDns, String[] dnsResolver, String[] excludeIps,
                             boolean enabledFilter, boolean filterBypassMode, String[] filterApps, boolean enableTethering) throws java.lang.Exception {

        mPrivateAddress = VpnUtils.selectPrivateAddress();

        // Make sure control sockets (SSH/proxy/TLS) are excluded from the VPN.
        // This prevents routing loops, especially when IPv6 ::/0 is installed.
        SocketProtector.setVpnService((VpnService) mHostService.getVpnService());

        // routes list
        // For IPv4 we use NetworkSpace to compute a minimal set of routes excluding the server.
        // For IPv6, Android does not provide a direct "exclude this single address" API when
        // installing ::/0. As a defensive fallback (in addition to VpnService.protect), we build
        // a set of IPv6 prefixes that cover all IPv6 addresses *except* the excluded server /128.
        // This avoids the control connection being routed into the VPN when the server is IPv6.
        String excludedServerIpv6 = null;
        for (String ip : excludeIps) {
            if (ip != null && ip.contains(":")) {
                excludedServerIpv6 = ip;
                SkStatus.logInfo("IPV6 DETECTED: " + ip);
            } else {
                SkStatus.logInfo("IPV4 DETECTED");
                mRoutes.addIP(new CIDRIP(ip, 32), false);
            }
        }

        for (String ip : filterApps) {
            if (ip.contains(":")) {
                SkStatus.logInfo("IPV6 DETECTED: " + ip);
            } else {
                SharedPreferences prefs = mHostService.getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
                SkStatus.logInfo("IPV4 DETECTED");
                mRoutes.addIP(new CIDRIP(ip, 32), false);

            }
        }


        Locale previousLocale = Locale.getDefault();

        final String errorMessage = "startVpn failed";
        try {

            // Workaround for https://code.google.com/p/android/issues/detail?id=61096
            Locale.setDefault(new Locale("en"));

            ParcelFileDescriptor tunFd = null;

            // inicia VpnBuilder
            VpnService.Builder builder = ((VpnService.Builder) mHostService.newVpnServiceBuilder())
                    .addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength)
                    .allowFamily(AF_INET)
                    .allowFamily(AF_INET6);

            // Configure an IPv6 address on the VPN interface so IPv6 flows can be
            // routed through tun2socks. Without an IPv6 address on the interface,
            // Android will not reliably deliver IPv6 packets to the TUN device.
            builder.addAddress(VPN_INTERFACE_IPV6_ADDRESS, VPN_INTERFACE_IPV6_PREFIX_LENGTH);
            if (excludedServerIpv6 != null) {
                SkStatus.logInfo("Excluding server IPv6 from VPN routes: " + excludedServerIpv6);
                addIpv6RoutesExcludingSingleAddress(builder, excludedServerIpv6);
            } else {
                builder.addRoute("::", 0);
            }
            mRoutes.addIP(new CIDRIP("0.0.0.0", 0), true);
            mRoutes.addIP(new CIDRIP("10.0.0.0", 8), false);
            mRoutes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), false);

            // ainda pra testar, subnet routing pesquisar "allow lan"
            if (enableTethering) {
                // USB tethering 192.168.42.x
                // Wi-Fi tethering 192.168.43.x
                mRoutes.addIP(new CIDRIP("192.168.42.0", 23), false);
                // Bluetooth tethering 192.168.44.x
                mRoutes.addIP(new CIDRIP("192.168.44.0", 24), false);
                // Wi-Fi direct 192.168.49.x
                mRoutes.addIP(new CIDRIP("192.168.49.0", 24), false);
            }

            // Add Dns
            for (String dns : dnsResolver) {
                try {
                    builder.addDnsServer(dns);
                    if (CIDRIP.InetAddressUtils.isIPv4Address(dns)) {
                        SkStatus.logInfo("DNS IPV4 ON");
                        mRoutes.addIP(new CIDRIP(dns, 32), forwardDns);
                    }
                    if (CIDRIP.InetAddressUtils.isIPv6Address(Arrays.toString(excludeIps))) {
                        SkStatus.logInfo("DNS IPV6 ON");
                        builder.addDnsServer("2606:4700:4700::1111");
                        builder.addDnsServer("2606:4700:4700::1001");
                    }
                } catch (IllegalArgumentException iae) {
                    mHostService.onDiagnosticMessage(String.format("Erro ao adicionar DNS %s: %s", dns, iae.getLocalizedMessage()));
                }
            }

            // set MTU
            String release = Build.VERSION.RELEASE;
            if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                    && mMtu < 1280) {
                SkStatus.logInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround Android Bug #70916", mMtu));
                mMtu = 1280;
            }
            builder.setMtu(mMtu);

            // loop routes
            NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);

            for (NetworkSpace.IpAddress route : mRoutes.getPositiveIPList()) {
                try {
                    if (multicastRange.containsNet(route))
                        SkStatus.logDebug("VPN: Ignoring multicast route: " + route.toString());
                    else
                        builder.addRoute(route.getIPv4Address(), route.networkMask);
                } catch (IllegalArgumentException ia) {
                    //mHostService.onDiagnosticMessage("Route rejeitada: " + route + " " + ia.getLocalizedMessage());
                }
            }

            if (Build.VERSION.SDK_INT >= 21) {
                mHostService.onDiagnosticMessage(String.format("IT WORKS THE DRAGON WORKS ✔"));
                mHostService.onDiagnosticMessage(String.format("I Like The SISU DRAGON YOU LIKE THE SISU DRAGON?✔"));
            }

            // TunFd
            tunFd = builder
                    .setSession(mHostService.getAppName())
                    .setConfigureIntent(SocksReviveService.getGraphPendingIntent(mHostService.getContext()))
                    .establish();

            if (tunFd == null) {
                // As per http://developer.android.com/reference/android/net/VpnService.Builder.html#establish%28%29,
                // this application is no longer prepared or was revoked.
                return false;
            }

            mTunFd.set(tunFd);
            mRoutingThroughTunnel.set(false);
            mHostService.onVpnEstablished();

            mRoutes.clear();

        } catch (IllegalArgumentException e) {
            throw new Exception("IllegalArgumentException", e);
        } catch (SecurityException e) {
            throw new Exception("IllegalArgumentException", e);
        } catch (IllegalStateException e) {
            throw new Exception("IllegalStateException", e);
        } finally {
            // Restore the original locale.
            Locale.setDefault(previousLocale);
        }

        return true;
    }

    private boolean routeThroughTunnel(final String socksServerAddress, final String[] dnsResolver, boolean forwardDns, final String udpResolver, final boolean transparentDns) {
        if (!mRoutingThroughTunnel.compareAndSet(false, true)) {
            return false;
        }

        final ParcelFileDescriptor tunFd = mTunFd.get();
        if (tunFd == null) {
            return false;
        }

        // Pdnsd
        String dnsgwRelay = null;
        if (forwardDns) {

            int pdnsdPort = VpnUtils.findAvailablePort(8091, 10);

            String[] mServidorDNS = dnsResolver;
            dnsgwRelay = String.format("%s:%d", mPrivateAddress.mIpAddress, pdnsdPort);

            mPdnsd = new Pdnsd(mHostService.getContext(), mServidorDNS, DNS_RESOLVER_PORT,
                    mPrivateAddress.mIpAddress, pdnsdPort);
            mPdnsd.setOnPdnsdListener(new Pdnsd.OnPdnsdListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void onStop() {
                    stop();
                }
            });

            mPdnsd.start();
        }

        // Tun2socks
        mTun2Socks = new Tun2Socks(mHostService.getContext(), tunFd, mMtu,
                mPrivateAddress.mRouter, VPN_ROUTER_IPV6_ADDRESS, VPN_INTERFACE_NETMASK,
                socksServerAddress, udpResolver, dnsgwRelay, transparentDns);

        mTun2Socks.setOnTun2SocksListener(new Tun2Socks.OnTun2SocksListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStop() {
                stop();
            }
        });

        mTun2Socks.start();

        mHostService.onTunnelConnected();

        // TODO: should double-check tunnel routing; see:
        // https://bitbucket.org/psiphon/psiphon-circumvention-system/src/1dc5e4257dca99790109f3bf374e8ab3a0ead4d7/Android/PsiphonAndroidLibrary/src/com/psiphon3/psiphonlibrary/TunnelCore.java?at=default#cl-779
        return true;
    }

    /**
     * Adds IPv6 routes that cover the entire IPv6 space except for a single excluded /128.
     *
     * <p>This is a defensive fallback for cases where {@link VpnService#protect(java.net.Socket)}
     * is not effective on a given device/ROM. For a single excluded address, the complement can be
     * represented by at most 128 prefixes.
     */
    private static void addIpv6RoutesExcludingSingleAddress(VpnService.Builder builder, String excludedIpv6) {
        if (builder == null || excludedIpv6 == null) {
            return;
        }
        try {
            java.net.InetAddress ia = java.net.InetAddress.getByName(excludedIpv6);
            if (!(ia instanceof java.net.Inet6Address)) {
                builder.addRoute("::", 0);
                return;
            }

            final byte[] excluded = ia.getAddress(); // 16 bytes

            // For each bit position i, add a prefix (i+1) that matches the excluded address for the
            // first i bits and has the i-th bit flipped. The union of these prefixes equals the
            // full IPv6 space minus the excluded /128.
            for (int i = 0; i < 128; i++) {
                byte[] prefix = excluded.clone();

                // Flip the i-th bit (MSB-first).
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);
                prefix[byteIndex] = (byte) (prefix[byteIndex] ^ (1 << bitIndex));

                // Zero all bits after i to create a proper network prefix address.
                int nextBit = i + 1;
                int zeroFromByte = nextBit / 8;
                int zeroFromBit = 7 - (nextBit % 8);

                // If nextBit is byte-aligned, zero whole bytes from there.
                if (nextBit % 8 == 0) {
                    for (int b = zeroFromByte; b < 16; b++) {
                        prefix[b] = 0;
                    }
                } else {
                    // Mask the current byte to keep only the prefix bits, then zero subsequent bytes.
                    int mask = 0xFF << (zeroFromBit + 1);
                    prefix[zeroFromByte] = (byte) (prefix[zeroFromByte] & mask);
                    for (int b = zeroFromByte + 1; b < 16; b++) {
                        prefix[b] = 0;
                    }
                }

                String routeAddr = java.net.InetAddress.getByAddress(prefix).getHostAddress();
                builder.addRoute(routeAddr, i + 1);
            }
        } catch (Throwable t) {
            // If anything goes wrong, fall back to routing all IPv6 through the VPN.
            try {
                builder.addRoute("::", 0);
            } catch (Throwable ignored) {
            }
        }
    }

    private void stopRoutingThroughTunnel() {
        if (mTun2Socks != null && mTun2Socks.isAlive()) {
            mTun2Socks.interrupt();
        }

        mTun2Socks = null;

        // teste
        //org.torproject.android.service.vpn.Tun2Socks.Stop();
        //ca.psiphon.PsiphonTunnel.Stop();

        if (mPdnsd != null && mPdnsd.isAlive()) {
            mPdnsd.interrupt();
        }

        mPdnsd = null;
    }

    private void stopVpn() {
        stopRoutingThroughTunnel();

        // Drop the VPN reference when stopping.
        SocketProtector.setVpnService(null);

        ParcelFileDescriptor tunFd = mTunFd.getAndSet(null);
        if (tunFd != null) {
            try {
                tunFd.close();
            } catch (IOException e) {
            }
        }
    }

}
