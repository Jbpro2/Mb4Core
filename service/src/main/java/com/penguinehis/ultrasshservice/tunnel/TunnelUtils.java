package com.penguinehis.ultrasshservice.tunnel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import androidx.collection.ArrayMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TunnelUtils
{
	public static Map<String, CharSequence> BBCODES_LIST;

	public static String formatCustomPayload(String hostname, int port, String payload) {
		BBCODES_LIST = new ArrayMap<>();

		BBCODES_LIST.put("[method]", "CONNECT");
		BBCODES_LIST.put("[host]", hostname);
		BBCODES_LIST.put("[port]", Integer.toString(port));
		BBCODES_LIST.put("[host_port]", String.format("%s:%d", hostname, port));
		BBCODES_LIST.put("[protocol]", "HTTP/1.0");
		BBCODES_LIST.put("[ssh]", String.format("%s:%d", hostname, port));

		BBCODES_LIST.put("[crlf]", "\r\n");
		BBCODES_LIST.put("[cr]", "\r");
		BBCODES_LIST.put("[lf]", "\n");
		BBCODES_LIST.put("[lfcr]", "\n\r");

		// para corrigir bugs
		BBCODES_LIST.put("\\n", "\n");
		BBCODES_LIST.put("\\r", "\r");

		String ua = System.getProperty("http.agent");
		BBCODES_LIST.put("[ua]", ua == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : ua);

		if (!payload.isEmpty()) {
			for (String key : BBCODES_LIST.keySet()) {
				key = key.toLowerCase();
				payload = payload.replace(key, BBCODES_LIST.get(key));
			}


			payload = parseRotatehash(parseRandom(parseRotate(payload)));

			//SkStatus.logDebug("Payload: " + payload.replace("\n", "\\n").replace("\r", "\\r"));
		}

		return payload;
	}

	public static boolean injectSplitPayload(String requestPayload, OutputStream out) throws IOException {
		if (requestPayload.contains("[delay_split]")) {
			String[] split = requestPayload.split(Pattern.quote("[delay_split]"));

			for (int n = 0; n < split.length; n++) {
				String str = split[n];

				if (!injectInstantSplit(str, out)) { // first try instant split
					if (!injectSimpleSplit(str, out)) { // if not, try simple split
						try {
							out.write(str.getBytes("ISO-8859-1"));
						} catch (UnsupportedEncodingException e2) {
							out.write(str.getBytes());
						}
						out.flush();
					}
				}

				// create delay
				try {
					if (n != (split.length - 1))
						Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

			return true;
		} else if (injectInstantSplit(requestPayload, out)) { // if instant split is present in the payload, call the method
			return true;
		} else if (injectSimpleSplit(requestPayload, out)) {
			return true;
		}

		return false;
	}


	private static boolean injectSimpleSplit(String requestPayload, OutputStream out) throws IOException {
		if (requestPayload.contains("[split]")) {
			String[] split2 = requestPayload.split(Pattern.quote("[split]"));

			for (int i = 0; i < split2.length; i++) {
				try {
					out.write(split2[i].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split2[i].getBytes());
				}
				out.flush();
			}

			return true;
		}

		return false;
	}

	private static boolean injectInstantSplit(String requestPayload, OutputStream out) throws IOException {
		if (requestPayload.contains("[instant_split]")) {
			String[] split2 = requestPayload.split(Pattern.quote("[instant_split]"));

			for (int i = 0; i < split2.length; i++) {
				try {
					out.write(split2[i].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split2[i].getBytes());
				}
				out.flush();
			}

			return true;
		}

		return false;
	}
	
	
	/**
	* Rotate
	*/
	
	private static Map<Integer,Integer> lastRotateList = new ArrayMap<>();
	private static String lastPayload = "";

	public static String parseRotate(String payload) {
		Matcher match = Pattern.compile("\\[rotate=(.*?)\\]")
			.matcher(payload);

		// limpa dados quando a payload fôr alterada
		if (!lastPayload.equals(payload)) {
			restartRotateAndRandom();
			lastPayload = payload;
		}

		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			int split_key;
			if (lastRotateList.containsKey(i)) {
				split_key = lastRotateList.get(i)+1;
				if (split_key >= split.length) {
					split_key = 0;
				}
			}
			else  {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);

			lastRotateList.put(i, split_key);

			i++;
		}

		return payload;
	}

	private static int lastProcessedPayloadIndex = -1;
	private static int lastProcessedProxyIndex = -1;
	private static int proxyoff = 0;


	public static String parseRotatehash(String payload) {
		if (!payload.contains("#")) {
			return payload;
		}

		String[] payloads = payload.split("#");

		// If index was never initialized, start at 0
		if (lastProcessedPayloadIndex < 0 || lastProcessedPayloadIndex >= payloads.length) {
			lastProcessedPayloadIndex = 0;
		}

		// Increment logic
		if (proxyoff == 1 || lastProcessedProxyIndex == 0) {
			lastProcessedPayloadIndex = (lastProcessedPayloadIndex + 1) % payloads.length;
		}

		return payloads[lastProcessedPayloadIndex];
	}


	public static String getnewsni(String proxies, int newproxy) {

		if (!proxies.contains("#")) {
			proxyoff = 1;
			return proxies;
		}
		String[] sniList = proxies.split("#");
		if (sniList.length == 1) {
			return sniList[0].trim();
		}
		proxyoff = 0;
		if (newproxy == 1) {
			lastProcessedProxyIndex = (lastProcessedProxyIndex + 1) % sniList.length;
		}
		String currentProxy = sniList[lastProcessedProxyIndex].trim();

		return currentProxy;
	}

	public static String getnewserver(String proxies, int newproxy) {

		if (!proxies.contains("#")) {
			proxyoff = 1;
			return proxies;
		}
		String[] serverList = proxies.split("#");
		if (serverList.length == 1) {
			return serverList[0].trim();
		}
		proxyoff = 0;
		if (newproxy == 1) {
			lastProcessedProxyIndex = (lastProcessedProxyIndex + 1) % serverList.length;
		}
		String currentProxy = serverList[lastProcessedProxyIndex].trim();

		return currentProxy;
	}

	public static String getnetHostname(String proxies, int newproxy) {

		if (!proxies.contains("#")) {
			proxyoff = 1;
			return proxies;
		}
		String[] hostnameList = proxies.split("#");
		if (hostnameList.length == 1) {
			return hostnameList[0].trim();
		}
		proxyoff = 0;
		if (newproxy == 1) {
			lastProcessedProxyIndex = (lastProcessedProxyIndex + 1) % hostnameList.length;
		}
		String currentProxy = hostnameList[lastProcessedProxyIndex].trim();

		return currentProxy;
	}


	public static String getNextProxy(String proxies, int newproxy) {

		if (!proxies.contains("#")) {
			proxyoff = 1;
			return proxies;
		}
		String[] proxyList = proxies.split("#");
		if (proxyList.length == 1) {
			return proxyList[0].trim();
		}
		proxyoff = 0;
		if (newproxy == 1) {
			lastProcessedProxyIndex = (lastProcessedProxyIndex + 1) % proxyList.length;
		}
		String currentProxy = proxyList[lastProcessedProxyIndex].trim();

		return currentProxy;
	}

	public static int getPayloadCount(String payload) {
		if (payload == null || !payload.contains("#")) {
			return 1;
		}
		return payload.split("#").length;
	}

	public static String counterpl(String payload) {

		if (!payload.contains("#")) {
			return "Payload (1/1)";
		}

		String[] payloads = payload.split("#");
		int totalPayloads = payloads.length;
		String payloadCount = (lastProcessedPayloadIndex + 1) + "/" + totalPayloads;

		return "Payload (" + payloadCount + ")";
	}

	public static String counterx(String payload) {

		if (!payload.contains("#")) {
			return "Proxy (1/1)";
		}

		String[] payloads = payload.split("#");
		int totalPayloads = payloads.length;
		String payloadCount = (lastProcessedProxyIndex + 1) + "/" + totalPayloads;

		return "Proxy (" + payloadCount + ")";
	}

	public static int counterx2(String payload) {


		String[] payloads = payload.split("#");
		int totalPayloads = payloads.length;

		return totalPayloads;
	}

	/**
	* Random
	*/
	
	//private static List<Integer> lastRandomHostsList = new ArrayList<>();
	
	// precisa melhorar
	public static String parseRandom(String payload) {
		Matcher match = Pattern.compile("\\[random=(.*?)\\]")
			.matcher(payload);

		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			Random r = new Random();
			int split_key = r.nextInt(split.length);

			if (split_key >= split.length || split_key < 0) {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);
			
			i++;
		}

		return payload;
	}
	
	public static void restartRotateAndRandom() {
		lastRotateList.clear();
		//lastRandomHostsList.clear();
	}
	
	
	public static boolean isNetworkOnline(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context
			.getSystemService(context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		
		return (networkInfo != null && networkInfo.isConnectedOrConnecting());
	}
	
	public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
					 .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
						 .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						String sAddr = inetAddress.getHostAddress();
						
						return sAddr.toString();
                    }
                }
            }
        } catch (SocketException ex) {
            return "ERROR Obtaining IP";
        }
        return "No IP Available";
    }
	
	/*public static String getLocationIp(String ip) throws IOException {
		//String ipapihost = TransportManager.createInetAddress("")
			//.getHostAddress();
		
		URL ipapi = new URL("https://ipapi.co/"+ ip + "/country/");

		URLConnection conn = ipapi.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36");
		//conn.setRequestProperty("Host", ipapihost);

		InputStream input = conn.getInputStream();

		StringBuilder location = new StringBuilder();
		
		int len;
		while ((len = input.read()) != -1) {
			location.append((char) len);
		}
		
		try {
			input.close();
		} catch(IOException e){}

		return location.toString();
	}*/
	
	public static boolean isActiveVpn(Context mContext) {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			Network network = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            
           return (capabilities!= null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
		}
		else {
			NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);
			
			return (info != null && info.isConnectedOrConnecting());
		}
	}
}
