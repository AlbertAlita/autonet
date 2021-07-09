package com.taian.autonet.client.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class Utils {

    private static String TAG = "Utils";

    /**
     * Android 6.0 之前（不包括6.0）获取mac地址
     * 必须的权限 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
     *
     * @param context * @return
     */
    public static String getMacDefault(Context context) {
        String mac = "";
        if (context == null) {
            return mac;
        }
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = null;
        try {
            info = wifi.getConnectionInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (info == null) {
            return null;
        }
        mac = info.getMacAddress();
        if (!TextUtils.isEmpty(mac)) {
            mac = mac.toUpperCase(Locale.ENGLISH);
        }
        return mac;
    }

    /**
     * Android 6.0-Android 7.0 获取mac地址
     */
    public static String getMacAddress() {
        String macSerial = null;
        String str = "";

        try {
            Process pp = Runtime.getRuntime().exec("cat/sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            while (null != str) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();//去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }

        return macSerial;
    }

    /**
     * 获取设备HardwareAddress地址
     *
     * @return
     */
    public static String getMachineHardwareAddress() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        String hardWareAddress = null;
        NetworkInterface iF = null;
        if (interfaces == null) {
            return null;
        }
        while (interfaces.hasMoreElements()) {
            iF = interfaces.nextElement();
            try {
                hardWareAddress = bytesToString(iF.getHardwareAddress());
                if (hardWareAddress != null)
                    break;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return hardWareAddress;
    }

    /***
     * byte转为String
     *
     * @param bytes
     * @return
     */
    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            buf.append(String.format("%02X:", b));
        }
        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }
        return buf.toString();
    }

    public static String getMac(Context context) {
        String mac = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mac = getMacDefault(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mac = getMacAddress();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mac = getMachineHardwareAddress();
        }
        return mac;
    }

    public static int getAppVersionCode(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return -1;
        } else {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                return pi == null ? -1 : pi.versionCode;
            } catch (PackageManager.NameNotFoundException var4) {
                var4.printStackTrace();
                return -1;
            }
        }
    }

    /**
     * 删除文件
     *
     * @param fileName
     * @return
     */
    public static boolean deleteFile(String fileName) {
        boolean status;
        SecurityManager checker = new SecurityManager();
        File file = new File(Constants.FILE_PATH + fileName);
        if (file.exists()) {
            checker.checkDelete(file.toString());
            if (file.isFile()) {
                try {
                    file.delete();
                    status = true;
                } catch (SecurityException se) {
                    se.printStackTrace();
                    status = false;
                }
            } else
                status = false;
        } else
            status = false;
        return status;
    }


    public static String initFolderPath(Context context, String folder) {
        String filePath = "";
        if (isSDCardExist()) {
            filePath = Environment.getExternalStorageDirectory().toString() + Constants.FILE_PATH + folder;
            File directory = new File(filePath);
            try {
                if (!directory.exists()) directory.mkdirs();
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
            }
        } else {
            filePath = Environment.getDownloadCacheDirectory().toString() + Constants.FILE_PATH + folder;
            File directory = new File(filePath);
            try {
                if (!directory.exists()) directory.mkdirs();
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
            }
        }
        return filePath;
    }

    public static boolean isSDCardExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }


    public static void exitApp(Context context) {
        ActivityUtil.finishAllActivity();
        ActivityUtil.AppExit(context);
    }

    public static void updateLocalVideos(Context context, CommandDataInfo.ProgramCommand programCommand) {
        List<CommandDataInfo.VideoInfo> videoInfoList = programCommand.getVideoInfoList();
        List<VideoInfo> videoInfos = new ArrayList<>();
        for (CommandDataInfo.VideoInfo info : videoInfoList) {
            videoInfos.add(VideoInfo.toVideoInfo(info));
        }
        //保存信息
        SpUtils.putString(context, Constants.VIDEO_LIST, GsonUtil.toJson(videoInfos));
    }
}
