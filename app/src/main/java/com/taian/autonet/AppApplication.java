package com.taian.autonet;


import android.app.Application;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.liulishuo.filedownloader.FileDownloader;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.utils.SpUtils;
import com.taian.autonet.client.utils.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.player.VideoViewConfig;
import xyz.doikki.videoplayer.player.VideoViewManager;

public class AppApplication extends Application {

    public static AppApplication globleContext;
    public static String COMPLETE_CACHE_PATH;
    public static String COMPLETE_LOG_PATH;

    @Override
    public void onCreate() {
        super.onCreate();
        globleContext = this;
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        VideoViewManager.setConfig(VideoViewConfig.newBuilder()
                .setPlayerFactory(ExoMediaPlayerFactory.create())
                .build());
//        Aria.get(this).getDownloadConfig().setMaxTaskNum(Constants.MAX_DOWNLOAD_NUM);
        FileDownloader.setup(this);
    }

    public static String getMacAdress() {
        String mac = Utils.getMac(globleContext);
        if (!TextUtils.isEmpty(mac)) mac.toUpperCase(Locale.ENGLISH);
        return mac;
    }

    public static String getIP() {
        return SpUtils.getString(globleContext, Constants.IP);
    }

    public static int getPort() {
        return SpUtils.getInt(globleContext, Constants.PORT);
    }

    public static void setIP(String IP) {
        SpUtils.putString(globleContext, Constants.IP, IP);
    }

    public static void setPort(int port) {
        SpUtils.putInt(globleContext, Constants.PORT, port);
    }

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    System.err.println(sw);
                    String timeString = new SimpleDateFormat("yyyyMMddHHmmss")
                            .format(Calendar.getInstance().getTime());
                    File file = new File(Utils.initFolderPath(globleContext, Constants.LOG_PATH),
                            "crash@" + timeString + ".log");
                    Utils.generateFile(ex, file);
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            };


    public static String getDefaultRootPath() {
        return COMPLETE_CACHE_PATH + File.separator;
    }

}
