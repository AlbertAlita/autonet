package com.taian.autonet;


import android.app.Application;

import com.taian.autonet.client.utils.Utils;

public class AppApplication extends Application {

    public static AppApplication globleContext;

    @Override
    public void onCreate() {
        super.onCreate();
        globleContext = this;
    }

    public static String getMacAdress() {
        return "abcd";
//        return Utils.getMac(globleContext);
    }
}
