package me.dawson.proxyserver.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.penguinehis.socksrevive.BuildConfig;
import com.penguinehis.socksrevive.R;
import com.penguinehis.ultrasshservice.tunnel.TunnelUtils;
import com.penguinehis.ultrasshservice.util.ToastUtil;

public class ProxySettings extends Activity implements ServiceConnection,
OnCheckedChangeListener {
    public static final String TAG = "ProxySettings";

    protected static final String KEY_PREFS = "proxy_pref";
    protected static final String KEY_ENABALE = "proxy_enable";

    private static final int NOTIFICATION_ID = 20140701;
    private ToastUtil toastutil;

    private IProxyControl proxyControl = null;

    private TextView tvInfo;
    private CheckBox cbEnable;
	private TextView ipproxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mandarin);


        if (!BuildConfig.DEBUG) {
            //adsBannerView.setAdUnitId(SocksHttpApp.ADS_UNITID_BANNER_SOBRE);
        }
        toastutil = new ToastUtil(this);

        // carrega anúncio
        if (TunnelUtils.isNetworkOnline(this)) {




        }

        tvInfo = (TextView) findViewById(R.id.tv_info);
		TextView ipproxy = (TextView)findViewById ( R.id.ipproxy); // ipproxy
		ipproxy.setText("IP: " + TunnelUtils.getLocalIpAddress()); //version
        cbEnable = (CheckBox) findViewById(R.id.cb_enable);
        cbEnable.setOnCheckedChangeListener(this);

        Intent intent = new Intent(this, ProxyService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName cn, IBinder binder) {
        proxyControl = (IProxyControl) binder;
        if (proxyControl != null) {
            updateProxy();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName cn) {
        proxyControl = null;
    }

    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sp = getSharedPreferences(KEY_PREFS, MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABALE, isChecked).apply();
        updateProxy();
    }

    private void updateProxy() {
        if (proxyControl == null) {
            return;
        }

        boolean isRunning = false;
        try {
            isRunning = proxyControl.isRunning();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        boolean shouldRun = getSharedPreferences(KEY_PREFS, MODE_PRIVATE)
			.getBoolean(KEY_ENABALE, false);
        if (shouldRun && !isRunning) {
            startProxy();
        } else if (!shouldRun && isRunning) {
            stopProxy();
        }

        try {
            isRunning = proxyControl.isRunning();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (isRunning) {
            tvInfo.setText(R.string.proxy_on);
            cbEnable.setChecked(true);
        } else {
            tvInfo.setText(R.string.proxy_off);

            cbEnable.setChecked(false);
        }
    }

    private void startProxy() {
        boolean started = false;
        try {
            started = proxyControl.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (!started) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Context context = getApplicationContext();

        Notification notification = new Notification();
        notification.icon = R.drawable.ic_wifi_tethering_white_24dp;
        notification.tickerText = getResources().getString(R.string.proxy_on);
        notification.when = System.currentTimeMillis();

        CharSequence contentTitle = getResources().getString(R.string.app_name);

        CharSequence contentText = ("Proxy ONLINE "+ "IP: " + TunnelUtils.getLocalIpAddress()+":6821");


        Intent intent = new Intent(this, ProxySettings.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(this, PendingIntent.FLAG_IMMUTABLE,
																										  intent, PendingIntent.FLAG_IMMUTABLE);

        notificationManager (contentTitle, contentText, pendingIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        manager.notify(NOTIFICATION_ID, notification);

        toastutil.showSuccessToast("Proxy Online");
    }

    private void notificationManager(CharSequence contentTitle, CharSequence contentText, PendingIntent pendingIntent) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "proxy_notification_channel";
            NotificationChannel channel = new NotificationChannel(channelId, "Proxy Notification", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_wifi_tethering_white_24dp)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        Notification notification = builder.build();

        manager.notify(NOTIFICATION_ID, notification);
    }


    private void stopProxy() {
        boolean stopped = false;

        try {
            stopped = proxyControl.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (!stopped) {
            return;
        }

        tvInfo.setText(R.string.proxy_off);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
        toastutil.showWarningToast("Proxy Offline");
    }

}


