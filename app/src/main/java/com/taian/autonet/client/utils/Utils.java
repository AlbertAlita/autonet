package com.taian.autonet.client.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.taian.autonet.AppApplication;
import com.taian.autonet.R;
import com.taian.autonet.bean.ApkInfo;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class Utils {

    private static String TAG = "Utils";
    private static final String[] SU_BINARY_DIRS = {
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin"
    };

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
        File file = new File(AppApplication.COMPLETE_CACHE_PATH + File.separator + fileName);

        if (file.exists()) {
            checker.checkDelete(file.toString());
            if (file.isFile()) {
                try {
                    file.delete();
                    status = true;
                } catch (SecurityException se) {
                    Log.e(TAG, "deleteFile: " + se.getMessage());
                    status = false;
                }
            } else {
                Log.e(TAG, "deleteFile:1 ");
                status = false;
            }
        } else {
            status = false;

        }
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

    @SuppressLint("LogUtilsNotUsed")
    public static ApkInfo runRootCmd(String cmd) {
        ApkInfo apkInfo = new ApkInfo();
        boolean grandted;
        DataOutputStream outputStream = null;
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes(cmd + "\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            apkInfo.setInstallSuccess(true);
            String msg = reader.readLine();
            if (msg != null) {
                Log.e(TAG, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            apkInfo.setInstallSuccess(false);
            apkInfo.setErrorMessage("错误：" + e.getMessage());
            closeIO(outputStream);
            closeIO(reader);
        }
        return apkInfo;
    }

    private static void closeIO(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
        }
    }


    public static boolean checkRoot() {
        boolean isRoot = false;
        try {
            for (String dir : SU_BINARY_DIRS) {
                File su = new File(dir, "su");
                if (su.exists()) {
                    isRoot = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        return isRoot;
    }

    public static ApkInfo installPkg(Context context,String apkPath) {
        if (checkRoot()) {
            ApkInfo apkInfo = runRootCmd("pm install -i 包名 --user 0 " + apkPath);
            return apkInfo;
        } else {
            ApkInfo apkInfo = new ApkInfo();
            apkInfo.setErrorMessage(context.getString(R.string.none_root));
            return apkInfo;
        }
    }

    public static void reStartApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * 判断手机接入点（APN）是否处于可以使用的状态
     *
     * @param context
     * @return
     */
    public static boolean isMobileConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前wifi是否是处于可以使用状态
     *
     * @param context
     * @return
     */
    public static boolean isWIFIConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean checkNet(Context context) {
        // 判断是否具有可以用于通信渠道
        boolean mobileConnection = isMobileConnection(context);
        boolean wifiConnection = isWIFIConnection(context);
        if (mobileConnection == false && wifiConnection == false) {
            // 没有网络
            return false;
        }
        return true;
    }

    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;


    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return value.contains(".");
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static boolean isNumber(String value) {
        return isInteger(value) || isDouble(value);
    }

    public static int haveSpace(long neededSapce) {
        long ret = readSDCard();
        int value = 0;
        long temp = ret / 1024 / 1024;
        if (temp > neededSapce)
            value = 0;
        else if (ret == -1)
            value = -1;
        else
            value = 1;
        return value;
    }

    static public long readSDCard() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
            long freespace = availCount * blockSize;// / 1024 / 1024;
            return freespace;
        } else {
            return -1;
        }

    }
}
