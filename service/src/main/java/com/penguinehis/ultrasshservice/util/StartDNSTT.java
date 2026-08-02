package com.penguinehis.ultrasshservice.util;

import android.content.Context;

import com.penguinehis.ultrasshservice.R;

public class StartDNSTT {

	private static final String TAG = StartDNSTT.class.getSimpleName();

	private static final String APP_BASE3 = "com.penguinehis.socksrevive";

	// Assinatura da Google Play
	// private static final String APP_SIGNATURE = "XbhYZ4Bz/9F4cWLIDMg0wl/+jl8=\n";

	private String strMainMOD = hexToString("736f636b73726576697665"); // "socksrevive"
	private String APP_BASE = hexToString("636f6d2e70656e6775696e656869732e736f636b73726576697665"); // "com.penguinehis.socksrevive"
	private static final String APP_BASE2 = "115,111,99,107,115,114,101,118,105,118,101";

	private static StartDNSTT mInstance;
	private Context mContext;

	public static void init(Context context) {
		if (mInstance == null) {
			mInstance = new StartDNSTT(context);
			// AndroidTamperingProtectionUtils.getCertificateSignature(context);
		}
	}

	private StartDNSTT(Context context) {
		mContext = context;
	}

	public void simpleProtect() {
		// MOD BY @CKEDRAGON
		if (!APP_BASE.equals(mContext.getPackageName()) ||
			!mContext.getString(R.string.app_name).equals(strMainMOD) ||
			!mContext.getString(R.string.cake).equals(APP_BASE2) ||
			!APP_BASE3.equals(mContext.getPackageName())) {
			//throw new RuntimeException();
		}
	}

	public static void CharlieProtect() {
		if (mInstance == null) {
			throw new IllegalStateException("StartDNSTT is not initialized. Call StartDNSTT.init(context) first.");
		}
		mInstance.simpleProtect();
	}

private static String hexToString(String hex) {
	try {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
								+ Character.digit(hex.charAt(i+1), 16));
		}
		return new String(data, "UTF-8");
	} catch (Exception e) {
		e.printStackTrace();
		return null;
	}
}

}
