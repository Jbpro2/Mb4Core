package com.penguinehis.ultrasshservice.tunnel.vpn;

import android.net.VpnService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Centralized helper to ensure control sockets (SSH / proxy / TLS) are excluded
 * from the VPN routing.
 *
 * <p>When a VPN is established and a control socket is not protected, Android
 * will route that socket into the VPN itself, creating a loop (VPN -> SOCKS ->
 * VPN). This is especially visible with IPv6, where ::/0 routing is commonly
 * installed.
 */
public final class SocketProtector {

    private static volatile VpnService sVpnService;

    // Sockets created before the VpnService instance is available.
    // We keep weak refs to avoid leaking in edge cases.
    private static final Object sLock = new Object();
    private static final List<WeakReference<Object>> sPending = new ArrayList<>();

    private SocketProtector() {
    }

    public static void setVpnService(VpnService vpnService) {
        sVpnService = vpnService;
        if (vpnService != null) {
            flushPending(vpnService);
        } else {
            synchronized (sLock) {
                sPending.clear();
            }
        }
    }

    public static boolean protect(Socket socket) {
        if (socket == null) return false;
        final VpnService svc = sVpnService;
        if (svc == null) {
            enqueue(socket);
            return false;
        }
        return doProtect(svc, socket);
    }

    public static boolean protect(DatagramSocket socket) {
        if (socket == null) return false;
        final VpnService svc = sVpnService;
        if (svc == null) {
            enqueue(socket);
            return false;
        }
        return doProtect(svc, socket);
    }

    private static void enqueue(Object socket) {
        synchronized (sLock) {
            // Avoid unlimited growth if protect() is called repeatedly.
            if (sPending.size() > 128) {
                // Drop oldest.
                sPending.remove(0);
            }
            sPending.add(new WeakReference<>(socket));
        }
    }

    private static void flushPending(VpnService svc) {
        final List<Object> toProtect = new ArrayList<>();
        synchronized (sLock) {
            for (WeakReference<Object> ref : sPending) {
                Object obj = ref.get();
                if (obj != null) {
                    toProtect.add(obj);
                }
            }
            sPending.clear();
        }

        for (Object obj : toProtect) {
            if (obj instanceof Socket) {
                doProtect(svc, (Socket) obj);
            } else if (obj instanceof DatagramSocket) {
                doProtect(svc, (DatagramSocket) obj);
            }
        }
    }

    private static boolean doProtect(VpnService svc, Socket socket) {
        try {
            return svc.protect(socket);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean doProtect(VpnService svc, DatagramSocket socket) {
        try {
            return svc.protect(socket);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
