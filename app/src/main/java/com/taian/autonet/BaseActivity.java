package com.taian.autonet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.utils.ActivityUtil;
import com.taian.autonet.client.utils.PermissionUtils;
import com.taian.autonet.client.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

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

    protected void showProgressBar(String tip) {
        View loadingView = LayoutInflater.from(this).inflate(R.layout.loading_item, null);
        TextView tipTv = loadingView.findViewById(R.id.tip);
        if (!TextUtils.isEmpty(tip)) tipTv.setText(tip);
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(loadingView);
    }

    protected void hideProgressBar() {
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        View child = decorView.getChildAt(decorView.getChildCount() - 1);
        if (TextUtils.equals((String) child.getTag(), "loading")) {
            decorView.removeView(child);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkChangeReceiver.unRegisterObserver(this);
        NetworkChangeReceiver.unRegisterReceiver(this);
    }
}
