package com.penguinehis.socksrevive;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.penguinehis.ultrasshservice.SocksReviveCore;

/**
* App
*/
public class SocksReviveApp extends Application
{
	private static final String TAG = SocksReviveApp.class.getSimpleName();
	public static final String PREFS_GERAL = "SocksReviveGERAL";

	
	private static SocksReviveApp mApp;
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		mApp = this;
			
		// inicia
		SocksReviveCore.init(this);
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		//LocaleHelper.setLocale(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//LocaleHelper.setLocale(this);
	}

	
	public static SocksReviveApp getApp() {
		return mApp;
	}
}
