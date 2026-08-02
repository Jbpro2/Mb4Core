package com.service.xray;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Converts V2Ray/Xray links into a full XRAY JSON configuration.
 *
 * Supported:
 * - vmess://<base64(json)>
 * - vless://<uuid>@host:port?...#tag
 * - json://<base64(full-xray-json)>
 * - xray://<base64(full-xray-json)>
 * - config://<base64(full-xray-json)>
 *
 * Notes:
 * - This is intentionally minimal; it fills the placeholder "proxy" outbound
 *   created by {@link XrayConfigFactory#createBaseConfig}.
 */
public final class XrayLinkParser {

    private XrayLinkParser() { }

    public static String parseToXrayJson(String link) {
        return parseToXrayJson(link, 10808, 8, null);
    }

    public static String parseToXrayJson(String link, int localSocksPort, int userLevel, String uuidOverride) {
        try {
        if (link == null) throw new IllegalArgumentException("link is null");

        String raw = link.trim();
        int idx = raw.indexOf("://");
        if (idx <= 0) {
            throw new IllegalArgumentException("Link inválido (faltou ://): " + raw);
        }

        String scheme = raw.substring(0, idx).toLowerCase();
        String after = raw.substring(idx + 3);

        if ("json".equals(scheme) || "xray".equals(scheme) || "config".equals(scheme)) {
            String decoded = decodeBase64Flexible(after);
            JSONObject obj = new JSONObject(decoded);
            if (!obj.has("inbounds") || !obj.has("outbounds")) {
                throw new IllegalArgumentException("JSON inválido: faltou inbounds/outbounds");
            }
            return obj.toString();
        }

        if ("vmess".equals(scheme)) {
            JSONObject root = XrayConfigFactory.createBaseConfig(localSocksPort, userLevel);
            String decoded = decodeBase64Flexible(after);
            JSONObject vmessJson = new JSONObject(decoded);
            return applyVmess(root, vmessJson, userLevel, uuidOverride).toString();
        }

        if ("vless".equals(scheme)) {
            JSONObject root = XrayConfigFactory.createBaseConfig(localSocksPort, userLevel);

            final String vlessUri;
            if (after.contains("@") || after.contains("?") || after.contains("#")) {
                vlessUri = raw;
            } else {
                // sometimes panel stores base64 of the whole vless link
                String decoded = decodeBase64Flexible(after);
                if (decoded.trim().toLowerCase().startsWith("vless://")) {
                    vlessUri = decoded.trim();
                } else {
                    vlessUri = raw;
                }
            }

            return applyVless(root, vlessUri, userLevel, uuidOverride).toString();
        }

        throw new IllegalArgumentException("Protocolo não suportado: " + scheme);
    
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar config/link XRAY", e);
        }
}

    private static String decodeBase64Flexible(String b64) {
        if (b64 == null) return "";
        String s = b64.replace("\n", "").replace("\r", "").trim();

        // padding
        int pad = (4 - (s.length() % 4)) % 4;
        if (pad != 0) {
            StringBuilder sb = new StringBuilder(s);
            for (int i = 0; i < pad; i++) sb.append('=');
            s = sb.toString();
        }

        int[] flags = new int[] {
                Base64.URL_SAFE | Base64.NO_WRAP,
                Base64.NO_WRAP,
                Base64.DEFAULT,
                Base64.URL_SAFE
        };

        Throwable last = null;
        for (int f : flags) {
            try {
                byte[] bytes = Base64.decode(s, f);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Throwable t) {
                last = t;
            }
        }
        if (last != null) {
            throw new IllegalArgumentException("Base64 inválido", last);
        }
        throw new IllegalArgumentException("Base64 inválido");
    }

    // ========= VMESS =========
    private static JSONObject applyVmess(JSONObject root, JSONObject vmessJson, int userLevel, String uuidOverride) {
        try {
        JSONObject proxy = root.getJSONArray("outbounds").getJSONObject(0);

        root.put("remarks", vmessJson.optString("ps", ""));
        proxy.put("protocol", "vmess");

        // Settings.vnext[0]
        JSONObject vnext0 = proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0);
        vnext0.put("address", vmessJson.getString("add"));
        vnext0.put("port", vmessJson.getInt("port"));

        JSONArray usersArr = vnext0.getJSONArray("users");
        JSONObject user0 = usersArr.optJSONObject(0);
        if (user0 == null) {
            user0 = new JSONObject();
            usersArr.put(user0);
        }

        // VMess should NOT have "encryption" field (used by VLESS).
        user0.remove("encryption");

        String finalUuid = (uuidOverride != null && !uuidOverride.trim().isEmpty())
                ? uuidOverride.trim()
                : vmessJson.getString("id");
        user0.put("id", finalUuid);
        user0.put("security", vmessJson.optString("scy", "none"));
        user0.put("level", userLevel);

        // StreamSettings
        JSONObject stream = proxy.optJSONObject("streamSettings");
        if (stream == null) {
            stream = new JSONObject();
            proxy.put("streamSettings", stream);
        }

        String net = vmessJson.optString("net", "tcp");
        stream.put("network", net);

        // TLS
        String tls = vmessJson.optString("tls", "");
        if ("tls".equalsIgnoreCase(tls)) {
            stream.put("security", "tls");

            JSONObject tlsSettings = stream.optJSONObject("tlsSettings");
            if (tlsSettings == null) {
                tlsSettings = new JSONObject();
                stream.put("tlsSettings", tlsSettings);
            }

            String host = vmessJson.optString("host", "");
            String sni = vmessJson.optString("sni", host);
            if (sni != null && !sni.trim().isEmpty()) {
                tlsSettings.put("serverName", sni);
            }

            tlsSettings.put("allowInsecure", true);
            tlsSettings.put("show", false);
            tlsSettings.put("fingerprint", "chrome");
            if (!tlsSettings.has("alpn")) {
                tlsSettings.put("alpn", new JSONArray().put("h2").put("http/1.1"));
            }
        }

        // Transport
        String netLower = net == null ? "" : net.toLowerCase();
        switch (netLower) {
            case "ws": {
                JSONObject ws = stream.optJSONObject("wsSettings");
                if (ws == null) {
                    ws = new JSONObject();
                    stream.put("wsSettings", ws);
                }

                String host = vmessJson.optString("host", "");
                String path = vmessJson.optString("path", "");
                if (path != null && !path.trim().isEmpty()) ws.put("path", path);

                if (host != null && !host.trim().isEmpty()) {
                    JSONObject headers = ws.optJSONObject("headers");
                    if (headers == null) {
                        headers = new JSONObject();
                        ws.put("headers", headers);
                    }
                    headers.put("Host", host);
                }
                break;
            }

            case "grpc": {
                JSONObject grpc = stream.optJSONObject("grpcSettings");
                if (grpc == null) {
                    grpc = new JSONObject();
                    stream.put("grpcSettings", grpc);
                }
                String serviceName = vmessJson.optString("path", "");
                if (serviceName != null && !serviceName.trim().isEmpty()) grpc.put("serviceName", serviceName);
                break;
            }

            case "xhttp":
            case "splithttp": {
                JSONObject xhttp = stream.optJSONObject("xhttpSettings");
                if (xhttp == null) {
                    xhttp = new JSONObject();
                    stream.put("xhttpSettings", xhttp);
                }
                String host = vmessJson.optString("host", "");
                String path = vmessJson.optString("path", "/");
                if (host != null && !host.trim().isEmpty()) xhttp.put("host", host);
                if (path != null && !path.trim().isEmpty()) xhttp.put("path", path);
                break;
            }

            case "h2":
            case "http": {
                JSONObject http = stream.optJSONObject("httpSettings");
                if (http == null) {
                    http = new JSONObject();
                    stream.put("httpSettings", http);
                }
                String host = vmessJson.optString("host", "");
                String path = vmessJson.optString("path", "/");
                if (host != null && !host.trim().isEmpty()) http.put("host", new JSONArray().put(host));
                if (path != null && !path.trim().isEmpty()) http.put("path", path);
                break;
            }

            default:
                break;
        }

        return root;
    
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao aplicar VMess no config XRAY", e);
        }
}

    // ========= VLESS =========
    private static JSONObject applyVless(JSONObject root, String vlessUri, int userLevel, String uuidOverride) {
        try {
        JSONObject proxy = root.getJSONArray("outbounds").getJSONObject(0);

        Uri uri = Uri.parse(vlessUri);

        String userInfo = uri.getUserInfo();
        if (userInfo == null) throw new IllegalArgumentException("VLESS inválido: faltou UUID (userInfo)");

        String uuidFromLink = userInfo.split(":", 2)[0];
        String finalUuid = (uuidOverride != null && !uuidOverride.trim().isEmpty())
                ? uuidOverride.trim()
                : uuidFromLink;

        String host = uri.getHost();
        if (host == null) throw new IllegalArgumentException("VLESS inválido: faltou host");

        int port = uri.getPort();
        if (port <= 0) throw new IllegalArgumentException("VLESS inválido: faltou porta");

        String tag = uri.getFragment();
        if (tag != null && !tag.trim().isEmpty()) {
            root.put("remarks", tag);
        }

        proxy.put("protocol", "vless");

        // Settings.vnext[0]
        JSONObject vnext0 = proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0);
        vnext0.put("address", host);
        vnext0.put("port", port);

        JSONArray usersArr = vnext0.getJSONArray("users");

        JSONObject user0 = new JSONObject();
        user0.put("id", finalUuid);
        user0.put("encryption", "none");
        user0.put("level", userLevel);

        String flow = uri.getQueryParameter("flow");
        if (flow != null && !flow.trim().isEmpty()) user0.put("flow", flow);

        if (usersArr.length() > 0) usersArr.put(0, user0);
        else usersArr.put(user0);

        // StreamSettings
        JSONObject stream = proxy.optJSONObject("streamSettings");
        if (stream == null) {
            stream = new JSONObject();
            proxy.put("streamSettings", stream);
        }

        String type = uri.getQueryParameter("type");
        if (type == null || type.trim().isEmpty()) type = "tcp";
        stream.put("network", type);

        // TLS/Security
        String security = uri.getQueryParameter("security");
        security = security == null ? "" : security.toLowerCase();
        if ("tls".equals(security) || "reality".equals(security)) {
            stream.put("security", security);

            JSONObject tlsSettings = stream.optJSONObject("tlsSettings");
            if (tlsSettings == null) {
                tlsSettings = new JSONObject();
                stream.put("tlsSettings", tlsSettings);
            }

            String sni = uri.getQueryParameter("sni");
            if (sni == null || sni.trim().isEmpty()) sni = uri.getQueryParameter("host");
            if (sni != null && !sni.trim().isEmpty()) {
                tlsSettings.put("serverName", sni);
            }

            tlsSettings.put("allowInsecure", true);
            tlsSettings.put("show", false);
            String fp = uri.getQueryParameter("fp");
            if (fp == null || fp.trim().isEmpty()) fp = "chrome";
            tlsSettings.put("fingerprint", fp);
            if (!tlsSettings.has("alpn")) {
                tlsSettings.put("alpn", new JSONArray().put("h2").put("http/1.1"));
            }
        }

        // Transport
        String typeLower = type.toLowerCase();
        switch (typeLower) {
            case "ws": {
                JSONObject ws = stream.optJSONObject("wsSettings");
                if (ws == null) {
                    ws = new JSONObject();
                    stream.put("wsSettings", ws);
                }

                String path = uri.getQueryParameter("path");
                if (path != null && !path.trim().isEmpty()) ws.put("path", path);

                String h = uri.getQueryParameter("host");
                if (h != null && !h.trim().isEmpty()) {
                    JSONObject headers = ws.optJSONObject("headers");
                    if (headers == null) {
                        headers = new JSONObject();
                        ws.put("headers", headers);
                    }
                    headers.put("Host", h);
                }
                break;
            }

            case "grpc": {
                JSONObject grpc = stream.optJSONObject("grpcSettings");
                if (grpc == null) {
                    grpc = new JSONObject();
                    stream.put("grpcSettings", grpc);
                }
                String serviceName = uri.getQueryParameter("serviceName");
                if (serviceName != null && !serviceName.trim().isEmpty()) grpc.put("serviceName", serviceName);
                break;
            }

            case "xhttp":
            case "splithttp": {
                JSONObject xhttp = stream.optJSONObject("xhttpSettings");
                if (xhttp == null) {
                    xhttp = new JSONObject();
                    stream.put("xhttpSettings", xhttp);
                }
                String h = uri.getQueryParameter("host");
                String path = uri.getQueryParameter("path");
                if (path == null || path.trim().isEmpty()) path = "/";
                if (h != null && !h.trim().isEmpty()) xhttp.put("host", h);
                if (path != null && !path.trim().isEmpty()) xhttp.put("path", path);
                break;
            }

            case "h2":
            case "http": {
                JSONObject http = stream.optJSONObject("httpSettings");
                if (http == null) {
                    http = new JSONObject();
                    stream.put("httpSettings", http);
                }
                String h = uri.getQueryParameter("host");
                String path = uri.getQueryParameter("path");
                if (path == null || path.trim().isEmpty()) path = "/";
                if (h != null && !h.trim().isEmpty()) http.put("host", new JSONArray().put(h));
                if (path != null && !path.trim().isEmpty()) http.put("path", path);
                break;
            }

            default:
                break;
        }

        return root;
    
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao aplicar VLESS no config XRAY", e);
        }
}
}
