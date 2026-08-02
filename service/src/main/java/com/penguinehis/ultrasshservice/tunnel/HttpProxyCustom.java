package com.penguinehis.ultrasshservice.tunnel;

import static com.penguinehis.ultrasshservice.config.SettingsConstants.MAXIMO_THREADSPAY_KEY;

import android.content.Context;
import android.util.Log;

import com.penguinehis.ultrasshservice.R;
import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.config.SettingsConstants;
import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.tunnel.vpn.SocketProtector;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.transport.ClientServerHello;
import com.trilead.ssh2.transport.TransportManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * By Skank3r
 */
public class HttpProxyCustom
implements ProxyData
{

	private final String proxyHost;
    private final String proxyPass;
    private final int proxyPort;
    private final String proxyUser;
    private final String requestPayload;
	private boolean modoDropbear = false;

	private Socket sock;
	private Context mContext;

	public HttpProxyCustom(String proxyHost, int proxyPort, Context context) {
        this(proxyHost, proxyPort, null, null, context);
    }

    public HttpProxyCustom(String proxyHost, int proxyPort, String proxyUser, String proxyPass, Context context) {
        this(proxyHost, proxyPort, proxyUser, proxyPass, null, false, context);
    }

    public HttpProxyCustom(String proxyHost, int proxyPort, String proxyUser, String proxyPass, String requestPayload, boolean modoDropbear, Context context) {
        if (proxyHost == null) {
            throw new IllegalArgumentException("proxyHost must be non-null");
        } else if (proxyPort < 0) {
            throw new IllegalArgumentException("proxyPort must be non-negative");
        } else {
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUser = proxyUser;
            this.proxyPass = proxyPass;
            this.requestPayload = requestPayload;
			this.modoDropbear = modoDropbear;
			this.mContext = context;
        }
    }

	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException {
		SkStatus.SSH_SCONNECT = false;
		Settings mConfig = new Settings(this.mContext);
		//int maxRetries = Math.max(1, TunnelUtils.getPayloadCount(this.requestPayload));
		int totalProxies = TunnelUtils.counterx2(this.proxyHost);
		int maxThreads = Math.min(mConfig.getMaximoThreadspayload(), totalProxies);

		//for (int attempt = 0; attempt < maxRetries; attempt++) {

			ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
			CompletionService<ProxySocketResult> completionService = new ExecutorCompletionService<>(executor);
			List<Future<ProxySocketResult>> futureTasks = new ArrayList<>();
			AtomicBoolean gotSocket = new AtomicBoolean(false);

			for (int i = 0; i < maxThreads; i++) {
				String requestPayload = getRequestPayload(hostname, port);
				Future<ProxySocketResult> future = completionService.submit(() -> {
					String selectedProxy = TunnelUtils.getNextProxy(this.proxyHost, 1);
					Socket result = attemptConnectionWithProxy(selectedProxy, hostname, port, connectTimeout, readTimeout, requestPayload);
					if (result != null && result.isConnected() && gotSocket.compareAndSet(false, true)) {
						return new ProxySocketResult(result, selectedProxy);
					} else if (result != null) {
						try { result.close(); } catch (IOException ignored) {}
					}
					return null;
				});
				futureTasks.add(future);
			}

			ProxySocketResult successfulResult = null;

			try {
				for (int i = 0; i < maxThreads; i++) {
					Future<ProxySocketResult> completed = completionService.take();
					try {
						ProxySocketResult result = completed.get();
						if (result != null && result.socket != null && result.socket.isConnected()) {
							successfulResult = result;
							break;
						}
					} catch (Exception ignored) {}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			for (Future<ProxySocketResult> task : futureTasks) {
				task.cancel(true);
			}
			executor.shutdownNow();

			if (successfulResult != null) {
				if (!SkStatus.SSH_SCONNECT) {
					SkStatus.SSH_PROXYOK = successfulResult.proxy;
					return successfulResult.socket;
				} else {
					try { successfulResult.socket.close(); } catch (IOException ignored) {}
				}
			}
		//}

		return null;
	}


	private static class ProxySocketResult {
		public final Socket socket;
		public final String proxy;

		public ProxySocketResult(Socket socket, String proxy) {
			this.socket = socket;
			this.proxy = proxy;
		}
	}







	private Socket attemptConnectionWithProxy(String proxy, String hostname, int port, int connectTimeout, int readTimeout, String requestPayload) {
		Socket sock = new Socket();
		// Ensure the proxy socket bypasses the VPN to prevent routing loops.
		SocketProtector.protect(sock);
		try {
			InetAddress addr = TransportManager.createInetAddress(proxy);
			sock.connect(new InetSocketAddress(addr, this.proxyPort), connectTimeout);
			sock.setSoTimeout(readTimeout);

			//SkStatus.logInfo(R.string.state_proxy_inject);
			OutputStream out = sock.getOutputStream();

			if (!TunnelUtils.injectSplitPayload(requestPayload, out)) {
				try {
					out.write(requestPayload.getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(requestPayload.getBytes());
				}
				out.flush();
			}

			if (modoDropbear) return sock;

			InputStream in = sock.getInputStream();
			byte[] buffer = new byte[1024];

			if (!requestPayload.contains("delay_split")) {
				sock.setSoTimeout(2000);
			} else {
				SkStatus.logInfo("DelaySplit TimeOut 30s");
				sock.setSoTimeout(30000);
			}

			int len = ClientServerHello.readLineRN(in, buffer);
			String httpReponseFirstLine = new String(buffer, 0, len, "ISO-8859-1");
			String httpReponseAll1 = httpReponseFirstLine;

			while ((len = ClientServerHello.readLineRN(in, buffer)) != 0) {
				httpReponseAll1 += "\n" + new String(buffer, 0, len, "ISO-8859-1");
			}

			int responseCode = Integer.parseInt(httpReponseFirstLine.substring(9, 12));
			Pattern pattern = Pattern.compile("HTTP/\\d\\.\\d? \\d{3}");
			Matcher matcher = pattern.matcher(httpReponseAll1);

			if (matcher.find()) {
				SkStatus.logInfo("Connection success with response: " + httpReponseFirstLine);
				sock.setSoTimeout(0); // Connection stable
				return sock;
			}

		} catch (SocketTimeoutException e) {
			SkStatus.logInfo("Socket read timed out after 2000ms");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try { sock.close(); } catch (IOException ignored) {}
		return null;
	}


	private String getRequestPayload(String hostname, int port) {
		String payload = this.requestPayload;
		SkStatus.logInfo(TunnelUtils.counterpl(this.requestPayload));

		if (payload != null) {
			payload = TunnelUtils.formatCustomPayload(hostname, port, payload);
        }
		else {
			StringBuffer sb = new StringBuffer();

			sb.append("CONNECT ");
			sb.append(hostname);
			sb.append(':');
			sb.append(port);
			sb.append(" HTTP/1.1\r\n");
			if (!(this.proxyUser == null || this.proxyPass == null)) {
				char[] encoded;
				String credentials = this.proxyUser + ":" + this.proxyPass;
				try {
					encoded = Base64.encode(credentials.getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e) {
					encoded = Base64.encode(credentials.getBytes());
				}
				sb.append("Proxy-Authorization: Basic ");
				sb.append(encoded);
				sb.append("\r\n");
			}
			sb.append("\r\n");

			payload = sb.toString();
		}

		return payload;
	}

	@Override
	public void close()
	{
		if (sock == null) return;

		try {
			sock.close();
		} catch (IOException e) { /* failed */ }
	}

}
