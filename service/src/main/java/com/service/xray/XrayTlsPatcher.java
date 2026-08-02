package com.service.xray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Forces streamSettings.tlsSettings.allowInsecure = true for every TLS-enabled
 * inbound/outbound found in a V2Ray/Xray JSON config.
 *
 * Note: This affects only TLS ("security": "tls"). It does not apply to REALITY
 * ("security": "reality") which uses realitySettings instead. :contentReference[oaicite:1]{index=1}
 */
public final class XrayTlsPatcher {

    private XrayTlsPatcher() {}

    public static String forceAllowInsecureEverywhere(String configJson) {
        try {
            JSONObject root = new JSONObject(configJson);

            patchArray(root.optJSONArray("outbounds"));
            patchArray(root.optJSONArray("inbounds"));

            // Rare, but harmless to cover: some configs may use a top-level "transport".
            JSONObject transport = root.optJSONObject("transport");
            if (transport != null) {
                patchStreamSettingsObject(transport);
            }

            return root.toString();
        } catch (Throwable ignored) {
            // If parsing fails, do not break the VPN start; just return original.
            return configJson;
        }
    }

    private static void patchArray(JSONArray arr) throws JSONException {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) patchStreamSettingsObject(obj);
        }
    }

    private static void patchStreamSettingsObject(JSONObject parent) throws JSONException {
        JSONObject stream = parent.optJSONObject("streamSettings");
        if (stream == null) return;

        // Apply when TLS is explicitly enabled OR tlsSettings exists.
        String security = stream.optString("security", "");
        JSONObject tlsSettings = stream.optJSONObject("tlsSettings");

        boolean tlsEnabled = "tls".equalsIgnoreCase(security) || tlsSettings != null;
        if (!tlsEnabled) return;

        if (tlsSettings == null) {
            tlsSettings = new JSONObject();
            stream.put("tlsSettings", tlsSettings);
        }

        // The actual force:
        tlsSettings.put("allowInsecure", true);

        // Optional: only if you specifically need old/weak cipher suites (usually not needed).
        // See V2Ray transport docs. :contentReference[oaicite:2]{index=2}
        // tlsSettings.put("allowInsecureCiphers", true);
    }
}
