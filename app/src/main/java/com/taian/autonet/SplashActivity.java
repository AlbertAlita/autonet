package com.taian.autonet;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.ActivityUtil;
import com.taian.autonet.client.utils.GsonUtil;
import com.taian.autonet.client.utils.SpUtils;
import com.taian.autonet.client.utils.Utils;
import com.video.netty.protobuf.CommandDataInfo;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class SplashActivity extends BaseActivity {

    boolean isServerResponsed, isTimeOvered;
    private Handler mHandler;
    private CommandDataInfo.PackageConfigCommand packageConfigCommand;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initHandler();
        connectServerAndGetBaiscInfo();
    }

    private void initHandler() {
        mHandler = new Handler(getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isTimeOvered = true;
                if (isServerResponsed) {
                    saveDataAndSkipToLogin();
                }
            }
        }, 2000);
    }

    private void connectServerAndGetBaiscInfo() {
        WrapNettyClient.getInstance().connect();
        WrapNettyClient.getInstance().addNettyClientListener(SplashActivity.class.getSimpleName(),
                new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
                    @Override
                    public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage message, int index) {
//                        Log.e("TAG", message.toString());
                        if (CommandDataInfo.CommandDataInfoMessage.CommandType.PackageConfigType == message.getDataType()) {
                            CommandDataInfo.PackageConfigCommand packageConfigCommand = message.getPackageConfigCommand();
                            if (packageConfigCommand.getResponseCommand().getResponseCode() == Net.SUCCESS) {
                                isServerResponsed = true;
                                SplashActivity.this.packageConfigCommand = packageConfigCommand;
                                if (isTimeOvered) {
                                    saveDataAndSkipToLogin();
                                }
                            } else {
                                //断开链接
                                WrapNettyClient.getInstance().disConnect();
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(SplashActivity.this).
                                                setMessage(R.string.illegal_device).
                                                setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Utils.exitApp(SplashActivity.this);
                                                    }
                                                }).
                                                show();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onClientStatusConnectChanged(int statusCode, int index) {
                        if (statusCode == ConnectState.STATUS_CONNECT_SUCCESS) {
                            //tcp链接成功
                            CommandDataInfo.TokenCommand tokenCommand = CommandDataInfo.TokenCommand.newBuilder()
                                    .setToken(AppApplication.getMacAdress())
                                    .build();
                            CommandDataInfo.CommandDataInfoMessage command = CommandDataInfo.CommandDataInfoMessage.newBuilder()
                                    .setDataType(CommandDataInfo.CommandDataInfoMessage.CommandType.TokenType)
                                    .setTokenCommand(tokenCommand)
                                    .build();
                            WrapNettyClient.getInstance().sendMsgToServer(command);
                        } else if (statusCode == ConnectState.STATUS_CONNECT_ERROR) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //tcp链接失败
                                    new AlertDialog.Builder(SplashActivity.this).
                                            setMessage(R.string.net_error).
                                            setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    saveDataAndSkipToLogin();
                                                }
                                            }).
                                            show();
                                }
                            });
                        }
                    }
                });
    }

    private void saveDataAndSkipToLogin() {
        if (packageConfigCommand == null) {
            return;
        }
        //获取版本号信息
        CommandDataInfo.ApkVersionCommand apkVersionCommand = packageConfigCommand.getApkVersionCommand();
        //保存信息
        SpUtils.putString(this, Constants.LATEST_VERSION_APK_INFO,
                GsonUtil.toJson(apkVersionCommand));
        //获取节目单信息
        CommandDataInfo.ProgramCommand programCommand = packageConfigCommand.getProgramCommand();
        //保存节目单到本地
        Utils.updateLocalVideos(this, programCommand);
        Intent intent = new Intent(this, UserLoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WrapNettyClient.getInstance().removeListener(SplashActivity.class.getSimpleName());
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }
}
