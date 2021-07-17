package com.taian.autonet.bean;


import android.content.Context;

import com.taian.autonet.R;

public class ApkInfo {

    private boolean isInstallSuccess;
    private String errorMessage;

    public boolean isInstallSuccess() {
        return isInstallSuccess;
    }

    public void setInstallSuccess(boolean installSuccess) {
        isInstallSuccess = installSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static final int NONE_ROOT = 0x01, INSTALL_FAILED = 0x02, INSTALLING = 0x03, DOWNLOAD_FAILED = 0x04;

    public static String getErrorTips(Context context, int state) {
        switch (state) {
            case NONE_ROOT:
                return context.getString(R.string.none_root);
            case INSTALL_FAILED:
                return context.getString(R.string.upgrading_and_install_failed);
            case DOWNLOAD_FAILED:
                return context.getString(R.string.download_error_apk);
        }
        return context.getString(R.string.unkown_error);
    }
}
