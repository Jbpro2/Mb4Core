package com.service.xray;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base XRAY config factory. This config creates:
 * - A local SOCKS inbound (127.0.0.1:localSocksPort) for tun2socks
 * - A placeholder proxy outbound ("proxy") that will be filled by XrayLinkParser (VMess/VLESS)
 * - A "direct" outbound and a "block" outbound
 */
public final class XrayConfigFactory {

    private XrayConfigFactory() { }

    public static JSONObject createBaseConfig() {
        return createBaseConfig(10808, 8, "127.0.0.1");
    }

    public static JSONObject createBaseConfig(int localSocksPort, int userLevel) {
        return createBaseConfig(localSocksPort, userLevel, "127.0.0.1");
    }

    public static JSONObject createBaseConfig(int localSocksPort, int userLevel, String listen) {
        try {

        // ============= DNS Configuration =============
        JSONObject dnsHosts = new JSONObject();
        dnsHosts.put("domain:googleapis.cn", "googleapis.com");
        dnsHosts.put("dns.alidns.com", arr("223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"));
        dnsHosts.put("one.one.one.one", arr("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"));
        dnsHosts.put("dns.cloudflare.com", arr("104.16.132.229", "104.16.133.229", "2606:4700::6810:84e5", "2606:4700::6810:85e5"));
        dnsHosts.put("cloudflare-dns.com", arr("104.16.248.249", "104.16.249.249", "2606:4700::6810:f8f9", "2606:4700::6810:f9f9"));
        dnsHosts.put("dot.pub", arr("1.12.12.12", "120.53.53.53"));
        dnsHosts.put("dns.google", arr("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"));
        dnsHosts.put("dns.quad9.net", arr("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"));
        dnsHosts.put("common.dot.dns.yandex.net", arr("77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff"));

        JSONObject dnsConfig = new JSONObject();
        dnsConfig.put("hosts", dnsHosts);
        dnsConfig.put("servers", new JSONArray().put("151.244.242.6"));
        dnsConfig.put("tag", "dns-module");

        // ============= SOCKS Inbound =============
        JSONObject socksSettings = new JSONObject();
        socksSettings.put("auth", "noauth");
        socksSettings.put("udp", true);
        socksSettings.put("userLevel", userLevel);

        JSONObject sniffing = new JSONObject();
        sniffing.put("enabled", false);
        sniffing.put("destOverride", new JSONArray());
        sniffing.put("routeOnly", false);

        JSONObject socksInbound = new JSONObject();
        socksInbound.put("listen", listen);
        socksInbound.put("port", localSocksPort);
        socksInbound.put("protocol", "socks");
        socksInbound.put("tag", "socks");
        socksInbound.put("settings", socksSettings);
        socksInbound.put("sniffing", sniffing);

        // ============= Proxy Outbound (VMess default) =============
        JSONObject proxyUser = new JSONObject();
        proxyUser.put("id", "00000000-0000-0000-0000-000000000000");
        proxyUser.put("security", "auto");
        proxyUser.put("level", userLevel);

        JSONObject proxyServer = new JSONObject();
        proxyServer.put("address", "127.0.0.1");
        proxyServer.put("port", 1);
        proxyServer.put("users", new JSONArray().put(proxyUser));

        JSONObject happyEyeballs = new JSONObject();
        happyEyeballs.put("interleave", 2);
        happyEyeballs.put("maxConcurrentTry", 4);
        happyEyeballs.put("prioritizeIPv6", false);
        happyEyeballs.put("tryDelayMs", 250);

        JSONObject sockopt = new JSONObject();
        sockopt.put("domainStrategy", "UseIP");
        sockopt.put("happyEyeballs", happyEyeballs);

        JSONObject proxyOutbound = new JSONObject();
        proxyOutbound.put("tag", "proxy");
        proxyOutbound.put("protocol", "vmess");
        proxyOutbound.put("settings", new JSONObject().put("vnext", new JSONArray().put(proxyServer)));
        proxyOutbound.put("streamSettings", new JSONObject()
                .put("network", "tcp")
                .put("sockopt", sockopt)
        );
        proxyOutbound.put("mux", new JSONObject()
                .put("enabled", false)
                .put("concurrency", -1)
                .put("xudpConcurrency", 8)
                .put("xudpProxyUDP443", "")
        );
// ============= Direct Outbound =============
        JSONObject directOutbound = new JSONObject();
        directOutbound.put("tag", "direct");
        directOutbound.put("protocol", "freedom");
        directOutbound.put("settings", new JSONObject().put("domainStrategy", "UseIP"));

        // ============= Block Outbound =============
        JSONObject blockOutbound = new JSONObject();
        blockOutbound.put("tag", "block");
        blockOutbound.put("protocol", "blackhole");
        blockOutbound.put("settings", new JSONObject().put("response", new JSONObject().put("type", "http")));

        // ============= Routing =============
        JSONArray routingRules = new JSONArray();

        // DNS direct
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("port", "53")
                .put("outboundTag", "direct")
        );

        // Block UDP 443
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("network", "udp")
                .put("port", "443")
                .put("outboundTag", "block")
        );

        // Private IPs direct
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("ip", new JSONArray().put("geoip:private"))
                .put("outboundTag", "direct")
        );

        // Private domains direct
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("domain", new JSONArray().put("geosite:private"))
                .put("outboundTag", "direct")
        );

        // domestic-dns inbound direct
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("inboundTag", new JSONArray().put("domestic-dns"))
                .put("outboundTag", "direct")
        );

        // dns-module inbound direct
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("inboundTag", new JSONArray().put("dns-module"))
                .put("outboundTag", "direct")
        );

        // all else -> proxy
        routingRules.put(new JSONObject()
                .put("type", "field")
                .put("port", "0-65535")
                .put("outboundTag", "proxy")
        );

        JSONObject routing = new JSONObject();
        routing.put("domainStrategy", "AsIs");
        routing.put("rules", routingRules);

        // ============= Final Config =============
        JSONObject root = new JSONObject();
        root.put("log", new JSONObject().put("loglevel", "warning"));
        root.put("dns", dnsConfig);
        root.put("remarks", "");
        root.put("inbounds", new JSONArray().put(socksInbound));
        root.put("outbounds", new JSONArray()
                .put(proxyOutbound)
                .put(directOutbound)
                .put(blockOutbound)
        );
        root.put("routing", routing);
        return root;
    
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build XRAY base config", e);
        }
}

    private static JSONArray arr(Object... values) {
        JSONArray a = new JSONArray();
        if (values != null) {
            for (Object v : values) {
                a.put(v);
            }
        }
        return a;
    }
}
