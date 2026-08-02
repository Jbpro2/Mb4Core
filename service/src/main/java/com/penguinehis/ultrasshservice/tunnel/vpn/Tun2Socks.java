package com.penguinehis.ultrasshservice.tunnel.vpn;

import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.io.File;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.FileDescriptor;
import android.content.Context;

import com.penguinehis.ultrasshservice.util.StreamGobbler;
import androidx.core.content.ContextCompat;
import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.util.CustomNativeLoader;

public class Tun2Socks extends Thread implements StreamGobbler.OnLineListener {
	
    private static final String TAG = Tun2Socks.class.getSimpleName();
    private static final String TUN2SOCKS_BIN = "libtun2socks";
	
	private OnTun2SocksListener mListener;
	public interface OnTun2SocksListener {
		public void onStart();
		public void onStop();
	}
	
	private Process tun2SocksProcess;
    private ParcelFileDescriptor mVpnInterfaceFileDescriptor;
    private int mVpnInterfaceMTU;
    /**
     * IPv4 address assigned to the VPN interface.  Historically only a single
     * IPv4 address was supported.  To support IPv6 natively, a separate
     * {@code mVpnIp6Address} field has been added.  Existing code that
     * constructs {@link Tun2Socks} without specifying an IPv6 address will
     * continue to work – the IPv6 address will simply remain {@code null} and
     * no {@code --netif-ip6addr} argument will be passed to the underlying
     * tun2socks binary.
     */
    private String mVpnIpAddress;
    /**
     * IPv6 address assigned to the VPN interface.  This may be {@code null}
     * when only IPv4 connectivity is required.  When provided, the
     * constructor will append a {@code --netif-ip6addr} flag when spawning
     * the tun2socks process.
     */
    private String mVpnIp6Address;
    private String mVpnNetMask;
    private String mSocksServerAddress;
    private String mUdpgwServerAddress;
	private String mDnsResolverAddress;
    private boolean mUdpgwTransparentDNS;
	private Context mContext;
	
	private File fileTun2Socks;

    /**
     * Construct a {@link Tun2Socks} instance.  This constructor has been
     * updated to accept an optional IPv6 address.  If the provided
     * {@code vpnIp6Address} is {@code null} or empty, only an IPv4 address will
     * be configured on the tun interface.  To maintain backwards
     * compatibility with existing callers, an overloaded constructor without
     * the {@code vpnIp6Address} parameter is provided below.
     *
     * @param context the context used to resolve resources
     * @param vpnInterfaceFileDescriptor file descriptor of the VPN interface
     * @param vpnInterfaceMTU MTU of the VPN interface
     * @param vpnIpAddress IPv4 address assigned to the interface
     * @param vpnIp6Address IPv6 address assigned to the interface (may be null)
     * @param vpnNetMask network mask for the IPv4 address
     * @param socksServerAddress address of the SOCKS proxy
     * @param udpgwServerAddress address of the UDP gateway (may be null)
     * @param dnsResolverAddress address of the DNS resolver (may be null)
     * @param udpgwTransparentDNS whether to enable transparent DNS over UDP
     */
    public Tun2Socks(Context context, ParcelFileDescriptor vpnInterfaceFileDescriptor, int vpnInterfaceMTU,
                     String vpnIpAddress, String vpnIp6Address, String vpnNetMask,
                     String socksServerAddress, String udpgwServerAddress,
                     String dnsResolverAddress, boolean udpgwTransparentDNS) {
        mContext = context;

        mVpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
        mVpnInterfaceMTU = vpnInterfaceMTU;
        mVpnIpAddress = vpnIpAddress;
        mVpnIp6Address = vpnIp6Address;
        mVpnNetMask = vpnNetMask;
        mSocksServerAddress = socksServerAddress;
        mUdpgwServerAddress = udpgwServerAddress;
        mDnsResolverAddress = dnsResolverAddress;
        mUdpgwTransparentDNS = udpgwTransparentDNS;
    }

    /**
     * Backwards compatible constructor.  Invokes the full constructor without
     * assigning an IPv6 address.
     */
    public Tun2Socks(Context context, ParcelFileDescriptor vpnInterfaceFileDescriptor, int vpnInterfaceMTU,
                     String vpnIpAddress, String vpnNetMask, String socksServerAddress,
                     String udpgwServerAddress, String dnsResolverAddress, boolean udpgwTransparentDNS) {
        this(context, vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, null, vpnNetMask,
             socksServerAddress, udpgwServerAddress, dnsResolverAddress, udpgwTransparentDNS);
    }

	@Override
	public void run() {
		
		if (mListener != null) {
			mListener.onStart();
		}
		
 		try {
			
			StringBuilder cmd = new StringBuilder();

			//File fileTun2Socks = CustomNativeLoader.loadExecutableBinary(mContext, "libtun2socks.so");
			fileTun2Socks = CustomNativeLoader.loadNativeBinary(mContext, TUN2SOCKS_BIN, new File(mContext.getFilesDir(),TUN2SOCKS_BIN));
			
			if (fileTun2Socks == null){
				throw new IOException("Bin Tun2Socks não encontrado");
			}

			if (mVpnInterfaceFileDescriptor != null){
				File file_path = new File(ContextCompat.getDataDir(mContext), "sock_path");
				
				try {
					if (!file_path.exists())
						file_path.createNewFile();
				} catch(IOException e){
					throw new IOException("Falha ao criar arquivo: " + file_path.getCanonicalPath());
				}
				
                cmd.append(fileTun2Socks.getCanonicalPath());
                // Always configure the IPv4 address on the tun interface
                cmd.append(" --netif-ipaddr " + mVpnIpAddress);
                // Optionally configure an IPv6 address when provided.  When
                // mVpnIp6Address is null or empty, no IPv6 address is passed
                // to the tun2socks binary.  This allows clients to continue
                // operating in IPv4-only environments while enabling native
                // dual-stack operation when possible.
                if (mVpnIp6Address != null && !mVpnIp6Address.isEmpty()) {
                    cmd.append(" --netif-ip6addr " + mVpnIp6Address);
                }
                cmd.append(" --netif-netmask " + mVpnNetMask);
				cmd.append(" --socks-server-addr " + mSocksServerAddress);
				cmd.append(" --tunmtu " + Integer.toString(mVpnInterfaceMTU));
				cmd.append(" --tunfd " + mVpnInterfaceFileDescriptor.getFd());
				cmd.append(" --sock " + file_path.getAbsolutePath()); 
				cmd.append(" --loglevel " + Integer.toString(3));

				if (mUdpgwServerAddress != null) {
					if (mUdpgwTransparentDNS) {
						cmd.append(" --udpgw-transparent-dns");
					}
					String udpgwAddr = mUdpgwServerAddress;
					// Normalize IPv6 host:port into [host]:port for badvpn-tun2socks parser.
					// Accept user input as:
					//  - host:port (IPv4/hostname)
					//  - ipv6 (no port) -> default 7300
					//  - ipv6:port (no brackets) -> brackets will be added
					//  - [ipv6]:port (already OK)
					if (udpgwAddr != null) {
						udpgwAddr = udpgwAddr.trim();
						if (!udpgwAddr.isEmpty()) {
							// If it's a bare IPv6 without brackets and has multiple ':' then bracket it.
							boolean looksIpv6 = udpgwAddr.contains(":") && udpgwAddr.indexOf(':') != udpgwAddr.lastIndexOf(':');
							if (looksIpv6 && !udpgwAddr.startsWith("[")) {
								// Split last ':' as port if present, otherwise default.
								int lastColon = udpgwAddr.lastIndexOf(':');
								String hostPart = udpgwAddr;
								String portPart = "7300";
								if (lastColon > 0 && lastColon < udpgwAddr.length() - 1) {
									hostPart = udpgwAddr.substring(0, lastColon);
									String maybePort = udpgwAddr.substring(lastColon + 1);
									if (maybePort.matches("\\d+")) {
										portPart = maybePort;
									} else {
										hostPart = udpgwAddr; // treat as no-port
									}
								}
								udpgwAddr = "[" + hostPart + "]:" + portPart;
							} else if (udpgwAddr.matches("^[0-9a-fA-F:]+$")) {
								// Pure IPv6 without port
								udpgwAddr = "[" + udpgwAddr + "]:7300";
							}
						}
					}
					cmd.append(" --udpgw-remote-server-addr " + udpgwAddr);
				}
				
				if (mDnsResolverAddress != null) {
					cmd.append(" --dnsgw " + mDnsResolverAddress);
				}

				// executa comando
				tun2SocksProcess = Runtime.getRuntime().exec(cmd.toString());

				StreamGobbler stdoutGobbler = new StreamGobbler(tun2SocksProcess.getInputStream(), this);
				StreamGobbler stderrGobbler = new StreamGobbler(tun2SocksProcess.getErrorStream(), this);

				stdoutGobbler.start();
				stderrGobbler.start();
				
				// send Fd
				if (!sendFd(mVpnInterfaceFileDescriptor, file_path)) {
					throw new IOException("Falha ao enviar Fd para sock, talvez isso não seja suportado em seu aparelho. Entre em contato com o desenvolvedor.");
				}

				tun2SocksProcess.waitFor();
			}
		
		} catch (IOException e) {
			SkStatus.logException("Tun2Socks Error", e);
		} catch (Exception e) {
			SkStatus.logDebug("Tun2Socks Error: " + e.getMessage());
		}
		
		tun2SocksProcess = null;
		if (mListener != null) {
			mListener.onStop();
		}
	}

	@Override
	public synchronized void interrupt()
	{
		// TODO: Implement this method
		super.interrupt();
		
		//net.typeblog.socks.System.jniclose(mVpnInterfaceFileDescriptor.getFd());
		
		if (tun2SocksProcess != null)
        	tun2SocksProcess.destroy();
		
		try {
			if (fileTun2Socks != null)
				VpnUtils.killProcess(fileTun2Socks);
		} catch (Exception e) {}
		
		tun2SocksProcess = null;
		fileTun2Socks = null;
	}
	
	public void setOnTun2SocksListener(OnTun2SocksListener listener){
		this.mListener = listener;
	}
	
	
	/**
	* StreamGobbler OnLine Listener
	* implementação
	*/
	
	@Override
	public void onLine(String log){
		SkStatus.logDebug("Tun2Socks: " + log);
	}
	
	
	//----------------------------------------------------------------------------
	// Utils
	//----------------------------------------------------------------------------

	private boolean sendFd(ParcelFileDescriptor fileDescriptor, File toFile) throws InterruptedException {

		SkStatus.logDebug("Enviando Fd para sock");

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
			} catch(IOException unused) {
				Thread.sleep(500);
			}
			
			/*if (net.typeblog.socks.System.sendfd(fileDescriptor.getFd(), toFile.getAbsolutePath()) != -1) {
                return true;
            }

			try {
				Thread.sleep(500);
			} catch(InterruptedException e){}*/
		}

		return false;
	}
}
