package com.penguinehis.ultrasshservice.tunnel.vpn;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;

import androidx.core.content.ContextCompat;

import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.util.CustomNativeLoader;
import com.penguinehis.ultrasshservice.util.StreamGobbler;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Tun2Socks runner compiled with ANDROID_UDP (SOCKS5 UDP ASSOCIATE / udprelay).
 *
 * Intended for XRAY/libv2ray mode where the local SOCKS inbound supports UDP.
 * This avoids the need for a remote badvpn-udpgw server.
 */
public class Tun2SocksUdpRelay extends Thread implements StreamGobbler.OnLineListener {

    private static final String TAG = Tun2SocksUdpRelay.class.getSimpleName();
    private static final String TUN2SOCKS_BIN = "libtun2socks_udprelay";

    private OnTun2SocksListener mListener;

    public interface OnTun2SocksListener {
        void onStart();
        void onStop();
    }

    private Process tun2SocksProcess;
    private final ParcelFileDescriptor mVpnInterfaceFileDescriptor;
    private final int mVpnInterfaceMTU;
    private final String mVpnIpAddress;
    private final String mVpnIp6Address;
    private final String mVpnNetMask;
    private final String mSocksServerAddress;
    private final String mDnsGatewayAddress; // optional --dnsgw <ip:port>
    private final Integer mUdpRelayMaxConnections; // optional --udprelay-max-connections
    private final Context mContext;

    private File fileTun2Socks;

    /**
     * @param dnsGatewayAddress Optional. When set, all DNS queries are redirected to this address.
     * @param udpRelayMaxConnections Optional. When > 0, sets udprelay max connections.
     */
    public Tun2SocksUdpRelay(Context context,
                             ParcelFileDescriptor vpnInterfaceFileDescriptor,
                             int vpnInterfaceMTU,
                             String vpnIpAddress,
                             String vpnIp6Address,
                             String vpnNetMask,
                             String socksServerAddress,
                             String dnsGatewayAddress,
                             Integer udpRelayMaxConnections) {

        mContext = context;
        mVpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
        mVpnInterfaceMTU = vpnInterfaceMTU;
        mVpnIpAddress = vpnIpAddress;
        mVpnIp6Address = vpnIp6Address;
        mVpnNetMask = vpnNetMask;
        mSocksServerAddress = socksServerAddress;
        mDnsGatewayAddress = dnsGatewayAddress;
        mUdpRelayMaxConnections = udpRelayMaxConnections;
    }

    /**
     * Convenience ctor (IPv4-only, no dnsgw, no max-connections override).
     */
    public Tun2SocksUdpRelay(Context context,
                             ParcelFileDescriptor vpnInterfaceFileDescriptor,
                             int vpnInterfaceMTU,
                             String vpnIpAddress,
                             String vpnNetMask,
                             String socksServerAddress) {
        this(context, vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, null, vpnNetMask,
                socksServerAddress, null, null);
    }

    @Override
    public void run() {

        if (mListener != null) {
            mListener.onStart();
        }

        try {
            StringBuilder cmd = new StringBuilder();

            fileTun2Socks = CustomNativeLoader.loadNativeBinary(
                    mContext,
                    TUN2SOCKS_BIN,
                    new File(mContext.getFilesDir(), TUN2SOCKS_BIN)
            );

            if (fileTun2Socks == null) {
                throw new IOException("Bin Tun2SocksUdpRelay não encontrado");
            }

            if (mVpnInterfaceFileDescriptor != null) {
                File fileSockPath = new File(ContextCompat.getDataDir(mContext), "sock_path_xray");

                try {
                    if (!fileSockPath.exists()) {
                        fileSockPath.createNewFile();
                    }
                } catch (IOException e) {
                    throw new IOException("Falha ao criar arquivo: " + fileSockPath.getCanonicalPath());
                }

                cmd.append(fileTun2Socks.getCanonicalPath());
                cmd.append(" --netif-ipaddr ").append(mVpnIpAddress);

                if (mVpnIp6Address != null && !mVpnIp6Address.isEmpty()) {
                    cmd.append(" --netif-ip6addr ").append(mVpnIp6Address);
                }

                cmd.append(" --netif-netmask ").append(mVpnNetMask);
                cmd.append(" --socks-server-addr ").append(mSocksServerAddress);

                // ANDROID_UDP build: enable SOCKS5 UDP ASSOCIATE.
                cmd.append(" --enable-udprelay");
                if (mUdpRelayMaxConnections != null && mUdpRelayMaxConnections > 0) {
                    cmd.append(" --udprelay-max-connections ").append(mUdpRelayMaxConnections);
                }

                if (mDnsGatewayAddress != null && !mDnsGatewayAddress.trim().isEmpty()) {
                    cmd.append(" --dnsgw ").append(mDnsGatewayAddress.trim());
                }

                cmd.append(" --tunmtu ").append(Integer.toString(mVpnInterfaceMTU));
                cmd.append(" --tunfd ").append(mVpnInterfaceFileDescriptor.getFd());
                cmd.append(" --sock ").append(fileSockPath.getAbsolutePath());
                cmd.append(" --loglevel ").append(Integer.toString(3));

                tun2SocksProcess = Runtime.getRuntime().exec(cmd.toString());

                StreamGobbler stdoutGobbler = new StreamGobbler(tun2SocksProcess.getInputStream(), this);
                StreamGobbler stderrGobbler = new StreamGobbler(tun2SocksProcess.getErrorStream(), this);

                stdoutGobbler.start();
                stderrGobbler.start();

                if (!sendFd(mVpnInterfaceFileDescriptor, fileSockPath)) {
                    throw new IOException("Falha ao enviar Fd para sock (sock_path_xray)");
                }

                tun2SocksProcess.waitFor();
            }

        } catch (IOException e) {
            SkStatus.logException(TAG, e);
        } catch (Exception e) {
            SkStatus.logDebug(TAG + " error: " + e.getMessage());
        }

        tun2SocksProcess = null;
        if (mListener != null) {
            mListener.onStop();
        }
    }

    @Override
    public synchronized void interrupt() {
        super.interrupt();

        if (tun2SocksProcess != null) {
            try {
                tun2SocksProcess.destroy();
            } catch (Throwable ignored) {
            }
        }

        try {
            if (fileTun2Socks != null) {
                VpnUtils.killProcess(fileTun2Socks);
            }
        } catch (Throwable ignored) {
        }

        tun2SocksProcess = null;
        fileTun2Socks = null;
    }

    public void setOnTun2SocksListener(OnTun2SocksListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onLine(String log) {
        SkStatus.logDebug("Tun2SocksUdpRelay: " + log);
    }

    private boolean sendFd(ParcelFileDescriptor fileDescriptor, File toFile) throws InterruptedException {
        SkStatus.logDebug("Enviando Fd para sock (udprelay)");

        for (int tries = 10; tries >= 0; tries--) {
            try {
                LocalSocket localSocket = new LocalSocket();
                localSocket.connect(new LocalSocketAddress(toFile.getAbsolutePath(), LocalSocketAddress.Namespace.FILESYSTEM));
                localSocket.setFileDescriptorsForSend(new FileDescriptor[]{
                        fileDescriptor.getFileDescriptor()
                });
                localSocket.getOutputStream().write(42);
                localSocket.shutdownOutput();
                localSocket.close();
                return true;
            } catch (IOException unused) {
                Thread.sleep(500);
            }
        }

        return false;
    }
}
