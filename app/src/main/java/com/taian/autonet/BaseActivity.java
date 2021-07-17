package com.taian.autonet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.utils.ActivityUtil;
import com.taian.autonet.client.utils.PermissionUtils;
import com.taian.autonet.client.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Consumer;

public class BaseActivity extends Activity implements NetworkChangeReceiver.NetStateChangeObserver {

    protected RxPermissions mRxPermission;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.addActivity(this);
        NetworkChangeReceiver.registerReceiver(this);
        NetworkChangeReceiver.registerObserver(this);
        Log.i(getClass().getSimpleName(), "==>onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(getClass().getSimpleName(), "==>onNewIntent");
    }

    /**
     * 初始化权限
     */
    protected void initPermission() {
        List<Integer> index = PermissionUtils.checkPermissions(this, PermissionUtils.permissionsREAD);
        if (index.isEmpty()) {
            initFolder();
        } else {
            mRxPermission = new RxPermissions(this);
            mRxPermission.request(PermissionUtils.permissionsREAD)
                    .subscribe(new Consumer<Boolean>() {
                        @SuppressLint("CheckResult")
                        @Override
                        public void accept(Boolean agree) throws Exception {
                            if (agree) {
                                initFolder();
                            } else {
                                new AlertDialog.Builder(BaseActivity.this).
                                        setMessage(R.string.none_permission_warning).
                                        show();
                            }
                        }
                    });
        }
    }

    private void initFolder() {
        //初始化log文件夹
        AppApplication.COMPLETE_LOG_PATH = Utils.initFolderPath(this, Constants.LOG_PATH);
        //初始化文件缓存文件夹
        AppApplication.COMPLETE_CACHE_PATH =
                Utils.initFolderPath(this, Constants.CACHE_PATH);
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(this, R.string.net_not_available, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMobileConnect() {
        onNetConnect();
    }

    @Override
    public void onWifiConnect() {
        Log.e(getClass().getSimpleName(), "onWifiConnect");
        onNetConnect();
    }

    protected void onNetConnect() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkChangeReceiver.unRegisterObserver(this);
        NetworkChangeReceiver.unRegisterReceiver(this);
    }
}
