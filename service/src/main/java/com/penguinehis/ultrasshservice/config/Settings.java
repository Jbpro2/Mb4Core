package com.penguinehis.ultrasshservice.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.penguinehis.ultrasshservice.util.securepreferences.SecurePreferences;
import com.penguinehis.ultrasshservice.util.securepreferences.model.SecurityConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
* Configurações
*/

public class Settings implements SettingsConstants
{

    private Context mContext;
	private SharedPreferences mPrefs;
	private SecurePreferences mPrefsPrivate;
	
	private static SecurityConfig minimumConfig = new SecurityConfig.Builder("fubgf777gf6")
		.build();
	
	public Settings(Context context) {
		mContext = context;
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefsPrivate = SecurePreferences.getInstance(mContext, "SecureData", minimumConfig);
	}
	
	
	public String getPrivString(String key) {
		String defaultStr = "";
	
		switch (key) {
			case PORTA_LOCAL_KEY:
				defaultStr = "1080";
			break;
		}
		
		return mPrefsPrivate.getString(key, defaultStr);
	}
	
	public SecurePreferences getPrefsPrivate() {
		return mPrefsPrivate;
	}
	
	
	/**
	* Config File
	*/
	
	public String getMensagemConfigExportar() {
		return mPrefs.getString(CONFIG_MENSAGEM_EXPORTAR_KEY, "");
	}

	public void setMensagemConfigExportar(String str) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(CONFIG_MENSAGEM_EXPORTAR_KEY, str);
		editor.commit();
	}
	
	
	/**
	* Geral
	*/
	
	public String getIdioma() {
		return mPrefs.getString(IDIOMA_KEY, "default");
	}
	
	public void setIdioma(String str) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(IDIOMA_KEY, str);
		editor.commit();
	}

	
	public boolean getModoDebug() {
		return mPrefs.getBoolean(MODO_DEBUG_KEY, false);
	}
	
	public void setModoDebug(boolean is) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(MODO_DEBUG_KEY, is);
		editor.commit();
	}
	
	public int getMaximoThreadsSocks() {
		String n = mPrefs.getString(MAXIMO_THREADS_KEY, "8th");
		if (n == null || n.isEmpty()) {
			n = "8th";
		}
		return Integer.parseInt(n.replace("th", ""));
	}

	public int getMaximoThreadspayload() {
		String n = mPrefs.getString(MAXIMO_THREADSPAY_KEY, "25th");
		if (n == null || n.isEmpty()) {
			n = "25th";
		}
		return Integer.parseInt(n.replace("th", ""));
	}
	
	public boolean getHideLog() {
		return mPrefs.getBoolean(HIDE_LOG_KEY, false);
	}
	
	public boolean getAutoClearLog() {
		return mPrefs.getBoolean(AUTO_CLEAR_LOGS_KEY, false);
	}
	
	public boolean getIsFilterApps() {
		return mPrefs.getBoolean(FILTER_APPS, true);
	}
	
	public boolean getIsFilterBypassMode() {
		return mPrefs.getBoolean(FILTER_BYPASS_MODE, false);
	}
	
	public String[] getFilterApps() {
		String txt = mPrefs.getString(FILTER_APPS_LIST, "");
		if (txt.isEmpty()) {
			return new String[]{};
		}
		else {
			return txt.split("\n");
		}
	}
	
	public boolean getIsTetheringSubnet() {
		return mPrefs.getBoolean(TETHERING_SUBNET, false);
	}
	
	public boolean getIsDisabledDelaySSH() {
		return mPrefs.getBoolean(DISABLE_DELAY_KEY, false);
	}

	
	/**
	* Vpn Settings
	*/
	
	public boolean getVpnDnsForward(){
		return mPrefs.getBoolean(DNSFORWARD_KEY, false);
	}
	
	public void setVpnDnsForward(boolean use){
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(DNSFORWARD_KEY, use);
		editor.commit();
	}
	
	public String getVpnDnsResolver(){
		return mPrefs.getString(DNSRESOLVER_KEY, "8.8.8.8");
	}

	public boolean ssh_compression() {
		return mPrefs.getBoolean(SSH_COMPRESSION, false);
	}

	public boolean getWakelock() {
		return mPrefs.getBoolean(WAKELOCK_KEY, false);
	}
	public void setVpnDnsResolver(String str) {
		if (str == null || str.isEmpty()) {
			str = "8.8.8.8";
		}
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(DNSRESOLVER_KEY, str);
		editor.commit();
	}

	public boolean getVpnUdpForward(){
		return mPrefs.getBoolean(UDPFORWARD_KEY, false);
	}
	
	public void setVpnUdpForward(boolean use){
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(UDPFORWARD_KEY, use);
		editor.commit();
	}

	public String getVpnUdpResolver(){
		return mPrefs.getString(UDPRESOLVER_KEY, "127.0.0.1:7300");
	}
	
	public void setVpnUdpResolver(String str) {
		if (str == null || str.isEmpty()) {
			str = "127.0.0.1:7300";
		}
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(UDPRESOLVER_KEY, str);
		editor.commit();
	}
	
	/**
	* SSH Settings
	*/


	private void writeTextToFile( String filename) throws IOException {
		Context context = mContext.getApplicationContext();
		File file = new File(context.getFilesDir(), filename);
		FileWriter writer = new FileWriter(file);
		String limon = getPrivString(Settings.CUSTOM_PUBKEY);
		//Log.d("cakeloek", limon);
		writer.write(limon);
		writer.close();
	}
	public String getSSHKeypath() throws IOException {
		String filesDirPath = mContext.getApplicationContext().getFilesDir().getAbsolutePath();
		String limakdop_path = filesDirPath + "/coollemon2.txt";
		File publicKeyFile = new File(limakdop_path);
		if (publicKeyFile.exists()) {
			boolean deleted = publicKeyFile.delete();
			if (deleted) {
				//SkStatus.logInfo("<strong> DELETADO</strong>");
			} else {
				//SkStatus.logInfo("<strong> NAO DELETO</strong>");
			}
		} else {
			//SkStatus.logInfo("<strong> NAO EXISTE</strong>");
		}
		writeTextToFile("coollemon2.txt");
		String limakdop_path2 = filesDirPath + "/coollemon2.txt";
		//Log.d("cakeloek", limakdop_path);
		return mPrefs.getString(KEYPATH_KEY, limakdop_path2);
	}

	public int getSSHPinger() {
		String ping = mPrefs.getString(PINGER_KEY, "3");
		if (ping == null || ping.isEmpty()) {
			ping = "3";
		}
		return Integer.parseInt(ping);
	}
	

	/**
	* Utils
	*/
	
	public static void setDefaultConfig(Context context){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putBoolean(DNSFORWARD_KEY, true);
		editor.putString(DNSRESOLVER_KEY, "8.8.8.8");
		editor.putBoolean(UDPFORWARD_KEY, true);
		editor.putString(UDPRESOLVER_KEY, "127.0.0.1:7300");
		editor.putString(PINGER_KEY, "3");
		editor.putBoolean(SSH_COMPRESSION, false);
		editor.putString(MAXIMO_THREADS_KEY, "8th");
		editor.putString(MAXIMO_THREADSPAY_KEY, "25th");
		editor.putBoolean(WAKELOCK_KEY, false);
		editor.remove(MODO_DEBUG_KEY);
		editor.remove(HIDE_LOG_KEY);
		editor.remove(AUTO_CLEAR_LOGS_KEY);
		editor.remove(FILTER_APPS);
		editor.remove(FILTER_BYPASS_MODE);
		editor.remove(TETHERING_SUBNET);
		editor.remove(DISABLE_DELAY_KEY);
		
		editor.commit();
	}
	
	public static void clearSettings(Context context) {
		SharedPreferences priv = SecurePreferences.getInstance(context, "SecureData", minimumConfig);
		SharedPreferences.Editor edit = priv.edit();
		edit.clear();
		edit.commit();
	}
	
}
