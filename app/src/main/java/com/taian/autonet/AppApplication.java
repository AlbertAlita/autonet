package com.taian.autonet;


import android.app.Application;
import android.os.Process;
import android.util.Log;

import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AppApplication extends Application {

    public static AppApplication globleContext;
    public static String COMPLETE_CACHE_PATH;

    @Override
    public void onCreate() {
        super.onCreate();
        globleContext = this;
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }

    public static String getMacAdress() {
        return "abcd";
//        return Utils.getMac(globleContext);
    }

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    System.err.println(sw);

                    try {
                        String timeString = new SimpleDateFormat("yyyyMMddHHmmssSSS")
                                .format(Calendar.getInstance().getTime());
                        File file = new File(Utils.initFolderPath(globleContext, Constants.LOG_PATH),
                                "crash@" + timeString + ".log");
                        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                        PrintStream printStream = new PrintStream(fileOutputStream);
                        ex.printStackTrace(printStream);
                        printStream.flush();
                        printStream.close();
                        fileOutputStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            };

}
