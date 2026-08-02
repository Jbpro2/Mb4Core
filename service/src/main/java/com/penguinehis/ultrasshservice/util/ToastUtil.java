package com.penguinehis.ultrasshservice.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    private Context mContext;

    public ToastUtil(Context c) {
        mContext = c;
    }

    public void showSuccessToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showWarningToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showErrorToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showInfoToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showDefaultToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showConfusingToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    private void showToast(String msg, int length) {
        Toast.makeText(mContext, msg, length).show();
    }
}
