package com.taian.autonet.client.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;


import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;

public class PermissionUtils {

    /**
     * 读写权限
     */
    public static String[] permissionsREAD = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public static List<Integer> checkPermissions(Context context, String... permissions) {
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(context, permissions[i])
                    != PackageManager.PERMISSION_GRANTED) {
                index.add(i);
            }
        }
        return index;
    }

}
