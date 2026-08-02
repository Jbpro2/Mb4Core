package com.penguinehis.ultrasshservice.config;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.pm.PackageInfo;
import java.util.Calendar;

import android.content.pm.PackageManager;
import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.util.FileUtils;
import java.io.InputStream;
import com.penguinehis.ultrasshservice.R;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import android.content.SharedPreferences;


import com.penguinehis.ultrasshservice.util.Limo9;
import com.penguinehis.ultrasshservice.util.VPNUtils;
import com.penguinehis.ultrasshservice.util.securepreferences.crypto.Cryptor;
import com.penguinehis.ultrasshservice.util.securepreferences.model.SecurityConfig;

public class ConfigParser
{

	private static final String TAG = ConfigParser.class.getSimpleName();
	public static final String CONVERTED_PROFILE = "converted Profile";
	
	public static final String FILE_EXTENSAO = "skrvoid";

	private static final String
		SETTING_VERSION = "file.appVersionCode",
		SETTING_VALIDADE = "file.validade",
		SETTING_PROTEGER = "file.proteger",
		SETTING_AUTOR_MSG = "file.msg";

	
	public static boolean convertInputAndSave(InputStream input, Context mContext)
			throws IOException {
		Properties mConfigFile = new Properties();
		
		Settings settings = new Settings(mContext);
		SharedPreferences.Editor prefsEdit = settings.getPrefsPrivate()
			.edit();
		
		try {
			
			InputStream decodedInput = decodeInput(input);
			
			try {
				mConfigFile.loadFromXML(decodedInput);
			} catch(FileNotFoundException e) {
				throw new IOException("File Not Found");
			} catch(IOException e) {
				throw new Exception("Error Unknown", e);
			}

			// versão check
			int versionCode = Integer.parseInt(mConfigFile.getProperty(SETTING_VERSION));

			if (versionCode > getBuildId(mContext)) {
				throw new IOException(mContext.getString(R.string.alert_update_app));
			}

			// validade check
			String msg = mConfigFile.getProperty(SETTING_AUTOR_MSG);
			boolean mIsProteger = mConfigFile.getProperty(SETTING_PROTEGER).equals("1") ? true : false;
			long mValidade = 0;
			
			try {
				mValidade = Long.parseLong(mConfigFile.getProperty(SETTING_VALIDADE));
			} catch(Exception e) {
				throw new IOException(mContext.getString(R.string.alert_update_app));
			}

			if (!mIsProteger || mValidade < 0) {
				mValidade = 0;
			}
			else if (mValidade > 0 && isValidadeExpirou(mValidade)){
				throw new IOException(mContext.getString(R.string.error_settings_expired));
			}
			
			// bloqueia root
			boolean isBloquearRoot = false;
			String _blockRoot = mConfigFile.getProperty("bloquearRoot");
			if (_blockRoot != null) {
				isBloquearRoot = _blockRoot.equals("1") ? true : false;
				if (isBloquearRoot) {
					if (isDeviceRooted(mContext)) {
						throw new IOException(mContext.getString(R.string.error_root_detected));
					}
				} 
			}



			// bloqueia root
			//boolean isBloquearHWID = false;



			try {
				String mServidor = mConfigFile.getProperty(Settings.SERVIDOR_KEY);
				String mServidorPorta = mConfigFile.getProperty(Settings.SERVIDOR_PORTA_KEY);
				String mUsuario = mConfigFile.getProperty(Settings.USUARIO_KEY);
				String mSenha = mConfigFile.getProperty(Settings.SENHA_KEY);
				int mPortaLocal = Integer.parseInt(mConfigFile.getProperty(Settings.PORTA_LOCAL_KEY));
				int mTunnelType = Settings.bTUNNEL_TYPE_SSH_DIRECT;
				
				String _tunnelType = mConfigFile.getProperty(Settings.TUNNELTYPE_KEY);
				if (!_tunnelType.isEmpty()) {
					/**
					* Mantêm compatibilidade
					*/
					if (_tunnelType.equals(Settings.TUNNEL_TYPE_SSH_PROXY)) {
						mTunnelType = Settings.bTUNNEL_TYPE_SSH_PROXY;
					}
					else if (!_tunnelType.equals(Settings.TUNNEL_TYPE_SSH_DIRECT)) {
						mTunnelType = Integer.parseInt(_tunnelType);
					}
				}
				
				if (mServidor == null) {
					throw new Exception();
				}

				String _proxyIp = mConfigFile.getProperty(Settings.PROXY_IP_KEY);
				String _proxyPort = mConfigFile.getProperty(Settings.PROXY_PORTA_KEY);
				prefsEdit.putString(Settings.PROXY_IP_KEY, _proxyIp != null ? _proxyIp : "");
				prefsEdit.putString(Settings.PROXY_PORTA_KEY, _proxyPort != null ? _proxyPort : "");

				prefsEdit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, !mConfigFile.getProperty(Settings.PROXY_USAR_DEFAULT_PAYLOAD).equals("1") ? false : true);
				
				String _customPayload = mConfigFile.getProperty(Settings.CUSTOM_PAYLOAD_KEY);
				prefsEdit.putString(Settings.CUSTOM_PAYLOAD_KEY, _customPayload != null ? _customPayload : "");
				String _customSNI = mConfigFile.getProperty(Settings.CUSTOM_SNI);
				String _tlsmode = mConfigFile.getProperty(Settings.TLS12);
				String _pubkey = mConfigFile.getProperty(Settings.CUSTOM_PUBKEY);
				String _customNS = mConfigFile.getProperty(Settings.SLOW_NAMESERVER_KEY);
				String _customKEY = mConfigFile.getProperty(Settings.SLOW_CHAVE_KEY);
				String _customDNS = mConfigFile.getProperty(Settings.SLOW_DNSKEY);
				//String _hwidlock2 = mConfigFile.getProperty(Settings.hwid);
				prefsEdit.putString(Settings.CUSTOM_SNI, _customSNI != null ? _customSNI : "");
				prefsEdit.putString(Settings.TLS12, _tlsmode != null ? _tlsmode : "");
				prefsEdit.putString(Settings.CUSTOM_PUBKEY, _pubkey != null ? _pubkey : "");
				prefsEdit.putString(Settings.SLOW_NAMESERVER_KEY, _customNS != null ? _customNS : "");
				prefsEdit.putString(Settings.SLOW_CHAVE_KEY, _customKEY != null ? _customKEY : "");
				prefsEdit.putString(Settings.SLOW_DNSKEY, _customDNS != null ? _customDNS : "");
				//prefsEdit.putString(Settings.hwid, _hwidlock2 != null ? _hwidlock2 : "");
				
				if (mIsProteger) {
					prefsEdit.putString(Settings.CONFIG_MENSAGEM_KEY, msg != null ? msg : "");
					
					new Settings(mContext)
						.setModoDebug(false);

					String pedirLogin = mConfigFile.getProperty("file.pedirLogin");
					if (pedirLogin != null)
						prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, pedirLogin.equals("1") ? true : false);
					else
						prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false);
				}
				else {
					prefsEdit.putString(Settings.CONFIG_MENSAGEM_KEY, "");
					prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false);
				}
				
				prefsEdit.putString(Settings.SERVIDOR_KEY, mServidor);
				prefsEdit.putString(Settings.SERVIDOR_PORTA_KEY, mServidorPorta);
				prefsEdit.putString(Settings.USUARIO_KEY, mUsuario);
				prefsEdit.putString(Settings.SENHA_KEY, mSenha);
				prefsEdit.putString(Settings.PORTA_LOCAL_KEY, Integer.toString(mPortaLocal));

				prefsEdit.putInt(Settings.TUNNELTYPE_KEY, mTunnelType);
				prefsEdit.putBoolean(Settings.CONFIG_PROTEGER_KEY, mIsProteger);
				prefsEdit.putLong(Settings.CONFIG_VALIDADE_KEY, mValidade);
				prefsEdit.putBoolean(Settings.BLOQUEAR_ROOT_KEY, isBloquearRoot);
				//prefsEdit.putBoolean(Settings.hwidlock, isBloquearHWID);
				String _isDnsForward = mConfigFile.getProperty(Settings.DNSFORWARD_KEY);
				boolean isDnsForward = _isDnsForward != null && _isDnsForward.equals("0") ? false : true;
				String dnsResolver = mConfigFile.getProperty(Settings.DNSRESOLVER_KEY);
				settings.setVpnDnsForward(isDnsForward);
				settings.setVpnDnsResolver(dnsResolver);
				
				String _isUdpForward = mConfigFile.getProperty(Settings.UDPFORWARD_KEY);
				boolean isUdpForward = _isUdpForward != null && _isUdpForward.equals("1") ? true : false;
				String udpResolver = mConfigFile.getProperty(Settings.UDPRESOLVER_KEY);
				settings.setVpnUdpForward(isUdpForward);
				settings.setVpnUdpResolver(udpResolver);
				
			} catch(Exception e) {
				if (settings.getModoDebug()) {
					SkStatus.logException("Error Settings", e);
				}
				throw new IOException(mContext.getString(R.string.error_file_settings_invalid));
			}
			
			return prefsEdit.commit();
		
		} catch(IOException e) {
			throw e;
		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_file_invalid), e);
		} catch (Throwable e) {
			throw new IOException(mContext.getString(R.string.error_file_invalid));
		}
	}

	public static void convertDataToFile(OutputStream fileOut, Context mContext,
			boolean mIsProteger, boolean mPedirSenha, boolean isBloquearRoot,  String mMensagem, long mValidade)
				throws IOException {
		
		Properties mConfigFile = new Properties();
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		
		Settings settings = new Settings(mContext);
		SharedPreferences prefs = settings.getPrefsPrivate();
		
		try {
			int targerId = getBuildId(mContext);
			// para versões betas
			//targerId = 40;
			
			mConfigFile.setProperty(SETTING_VERSION, Integer.toString(targerId));

			mConfigFile.setProperty(SETTING_AUTOR_MSG, mMensagem);
			mConfigFile.setProperty(SETTING_PROTEGER, mIsProteger ? "1" : "0");
			mConfigFile.setProperty("bloquearRoot", isBloquearRoot ? "1" : "0");
			//mConfigFile.setProperty("bloquearhwid", isBloquearHWID ? "1" : "0");

			mConfigFile.setProperty(SETTING_VALIDADE, Long.toString(mValidade));
			mConfigFile.setProperty("file.pedirLogin", mPedirSenha ? "1" : "0");

			String server = prefs.getString(Settings.SERVIDOR_KEY, "");
			String server_port = prefs.getString(Settings.SERVIDOR_PORTA_KEY, "");
			
			if (mIsProteger && (server.isEmpty() || server_port.isEmpty())) {
				throw new Exception();
			}
						
			mConfigFile.setProperty(Settings.SERVIDOR_KEY, server);
			mConfigFile.setProperty(Settings.SERVIDOR_PORTA_KEY, server_port);
			mConfigFile.setProperty(Settings.USUARIO_KEY, prefs.getString(Settings.USUARIO_KEY, ""));
			mConfigFile.setProperty(Settings.SENHA_KEY, prefs.getString(Settings.SENHA_KEY, ""));
			mConfigFile.setProperty(Settings.PORTA_LOCAL_KEY, prefs.getString(Settings.PORTA_LOCAL_KEY, "1080"));

			mConfigFile.setProperty(Settings.TUNNELTYPE_KEY, Integer.toString(prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT)));
			
			mConfigFile.setProperty(Settings.DNSFORWARD_KEY, settings.getVpnDnsForward() ? "1" : "0");
			mConfigFile.setProperty(Settings.DNSRESOLVER_KEY, settings.getVpnDnsResolver());
			
			mConfigFile.setProperty(Settings.UDPFORWARD_KEY, settings.getVpnUdpForward() ? "1" : "0");
			mConfigFile.setProperty(Settings.UDPRESOLVER_KEY, settings.getVpnUdpResolver());
			
			mConfigFile.setProperty(Settings.PROXY_IP_KEY, prefs.getString(Settings.PROXY_IP_KEY, ""));
			mConfigFile.setProperty(Settings.PROXY_PORTA_KEY, prefs.getString(Settings.PROXY_PORTA_KEY, ""));

			String isDefaultPayload = prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true) ? "1" : "0";
			String customPayload = prefs.getString(Settings.CUSTOM_PAYLOAD_KEY, "");
			String customSNI = prefs.getString(Settings.CUSTOM_SNI, "");
			String tlsmode = prefs.getString(Settings.TLS12, "");
			String pubkey = prefs.getString(Settings.CUSTOM_PUBKEY, "");
			//String hwid = prefs.getString(Settings.hwid, "");
			String customNS = prefs.getString(Settings.SLOW_NAMESERVER_KEY, "");
			String customDNS = prefs.getString(Settings.SLOW_DNSKEY, "");
			String customKEY = prefs.getString(Settings.SLOW_CHAVE_KEY, "");
			if (mIsProteger && isDefaultPayload.equals("0") && customPayload.isEmpty()) {
				throw new IOException();
			}
			
			mConfigFile.setProperty(Settings.PROXY_USAR_DEFAULT_PAYLOAD, isDefaultPayload);
			mConfigFile.setProperty(Settings.CUSTOM_PAYLOAD_KEY, customPayload);
			mConfigFile.setProperty(Settings.CUSTOM_SNI, customSNI);
			mConfigFile.setProperty(Settings.TLS12, tlsmode);
			mConfigFile.setProperty(Settings.CUSTOM_PUBKEY, pubkey);
			//mConfigFile.setProperty(Settings.hwid, hwid);
			mConfigFile.setProperty(Settings.SLOW_DNSKEY, customDNS);
			mConfigFile.setProperty(Settings.SLOW_CHAVE_KEY, customKEY);
			mConfigFile.setProperty(Settings.SLOW_NAMESERVER_KEY, customNS);

		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_file_settings_invalid));
		}

		try {
			mConfigFile.storeToXML(tempOut,
				"Arquivo de Configuração");
		} catch(FileNotFoundException e) {
			throw new IOException("File Not Found");
		} catch(IOException e) {
			throw new IOException("Error Unknown", e);
		}
		
		try {
			InputStream input_encoded = encodeInput(
				new ByteArrayInputStream(tempOut.toByteArray()), mContext);
			
			FileUtils.copiarArquivo(input_encoded, fileOut);
		} catch(Throwable e) {
			throw new IOException(mContext.getString(R.string.error_save_settings));
		}
	}
	
	
	/**
	* Criptografia
	*/
	
	private static Cryptor mCrypto;

	private static Cryptor mCrypto2;

	private static Cryptor mCrypto9;
	private static Settings mConfig2;

	private static Cryptor mCrypto3;
	private static String password = new String(Limo9.PASSWORD_BYTES, StandardCharsets.UTF_8);

	private static String password2 = new String(Limo9.PASSWORD_BYTES2, StandardCharsets.UTF_8);

	static {
		mCrypto = Cryptor.initWithSecurityConfig(
			new SecurityConfig.Builder(password2).build());
	}
	
	private static InputStream encodeInput(InputStream in, Context mContext) throws Throwable {
		mConfig2 = new Settings(mContext);

		if (mConfig2.getPrivString(Settings.CONFIG_HWDI_STRING).isEmpty()){
			String strBase64 = mCrypto.encryptToBase64(getBytesArrayInputStream(in)
					.toByteArray());
			//Log.d("cakeloek0", mConfig2.getPrivString(Settings.CONFIG_HWDI_STRING));
			return new ByteArrayInputStream(strBase64.getBytes());
		}else {
			mCrypto9 = Cryptor.initWithSecurityConfig(
					new SecurityConfig.Builder(mConfig2.getPrivString(Settings.CONFIG_HWDI_STRING)).build());
			String strBase64 = mCrypto9.encryptToBase64(getBytesArrayInputStream(in)
					.toByteArray());
			//Log.d("cakeloek9", mConfig2.getPrivString(Settings.CONFIG_HWDI_STRING));
			return new ByteArrayInputStream(strBase64.getBytes());
		}

	}

	private static InputStream decodeInput(InputStream in) throws Throwable {
		byte[] byteDecript;

		ByteArrayOutputStream byteArrayOut = getBytesArrayInputStream(in);

		try {
			byteDecript = mCrypto.decryptFromBase64(byteArrayOut.toString());


		} catch (Exception e) {
			String error = String.valueOf(e);
			//Log.d("cakeoek1", String.valueOf(e));
			if (error.contains("java.lang.IllegalStateException")){
				try {
					mCrypto2 = Cryptor.initWithSecurityConfig(
							new SecurityConfig.Builder(password).build());
					byteDecript = mCrypto2.decryptFromBase64(byteArrayOut.toString());
					return new ByteArrayInputStream(byteDecript);
				}
				catch (Exception e2) {
					//Log.d("cakeoek2", String.valueOf(e2));
					String error2 = String.valueOf(e2);
					if (error2.contains("java.lang.IllegalStateException")){
						try {
							mCrypto2 = Cryptor.initWithSecurityConfig(
									new SecurityConfig.Builder(VPNUtils.getHWID()).build());
							byteDecript = mCrypto2.decryptFromBase64(byteArrayOut.toString());
							return new ByteArrayInputStream(byteDecript);
						}catch (Exception e3) {
							//Log.d("cakeoek3", String.valueOf(e3));
							//Log.d("cakeoek2", VPNUtils.getHWID2());
							String error3 = String.valueOf(e3);
							if (error3.contains("java.lang.IllegalStateException")){
								mCrypto3 = Cryptor.initWithSecurityConfig(
										new SecurityConfig.Builder(VPNUtils.getHWID2()).build());
								byteDecript = mCrypto3.decryptFromBase64(byteArrayOut.toString());
								return new ByteArrayInputStream(byteDecript);
							}else {
								throw new RuntimeException(e2);
							}
						}

					}
					else {
						throw new RuntimeException(e2);
					}

				}
			}else {


				throw new RuntimeException(e);
			}
		}

		return new ByteArrayInputStream(byteDecript);
	}

	public static ByteArrayOutputStream getBytesArrayInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		
		return buffer;
	}

	
	/**
	* Utils
	*/
	
	public static boolean isValidadeExpirou(long validadeDateMillis) {
		if (validadeDateMillis == 0) {
			return false;
		}
		
		// Get Current Date
		long date_atual = Calendar.getInstance()
			.getTime().getTime();
		
		if (date_atual >= validadeDateMillis) {
			return true;
		}
		
		return false;
	}
	
	public static int getBuildId(Context context) throws IOException {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			throw new IOException("Build ID not found");
		}
	}
	
	public static boolean isDeviceRooted(Context context) {
		

		return false;
	}

}
