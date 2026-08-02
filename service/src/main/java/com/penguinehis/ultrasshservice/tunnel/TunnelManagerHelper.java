package com.penguinehis.ultrasshservice.tunnel;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.penguinehis.ultrasshservice.SocksReviveService;
import com.penguinehis.ultrasshservice.config.Settings;
import com.service.xray.XrayVpnService;

public class TunnelManagerHelper
{
    public static void startSocksRevive(Context context) {
        Settings s = new Settings(context);
        int tType = s.getPrefsPrivate().getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);

        Intent startVPN;
        if (tType == Settings.bTUNNEL_TYPE_XRAY) {
            startVPN = new Intent(context, XrayVpnService.class);
            startVPN.setAction(XrayVpnService.ACTION_START);
        } else {
            startVPN = new Intent(context, SocksReviveService.class);
        }

        if (startVPN != null) {
            TunnelUtils.restartRotateAndRandom();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                //noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);
        }
    }

    public static void stopSocksRevive(Context context) {
        Intent stopTunnel = new Intent(SocksReviveService.TUNNEL_SSH_STOP_SERVICE);
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(stopTunnel);
    }
}
