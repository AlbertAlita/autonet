package com.taian.autonet.client.utils;

import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by baotaian on 2016/8/22.
 * 联网请求
 */
public class GsonUtil {
    private static Gson gson = null;

    static {
        if (gson == null) {
            gson = new Gson();
        }
    }

    private GsonUtil() {
    }

    public static <T> T fromJson(String jsonString, Class<T> cls) {
        T t = null;
        try {
            if (gson != null) {
                t = gson.fromJson(jsonString, cls);
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("TAG", "fromJson: " + e.getMessage());
            e.printStackTrace();
        }
        return t;
    }

    public static String toJson(Object obj) {
        String str = "";
        try {
            if (gson != null) {
                str = gson.toJson(obj);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return str;
    }
}
