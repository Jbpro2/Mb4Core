package com.penguinehis.ultrasshservice.util;

import android.util.Base64;

public class VPNUtil {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static int getHWID() {
		return 0;
	}

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[(j * 2) + 1] = hexArray[v & 15];
        }
        return new String(hexChars);
    }

	public static String decrypt(String input){


		return new String(Base64.decode(input,Base64.DEFAULT));
	}
	public static String encrypt(String input){

		return Base64.encodeToString(input.getBytes(),Base64.DEFAULT);
	}

}
