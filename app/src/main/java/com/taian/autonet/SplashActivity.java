package com.taian.autonet;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.ThreadPoolUtil;
import com.video.netty.protobuf.CommandDataInfo;


import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class SplashActivity extends BaseActivity {

    boolean isServerResponsed, isTimeOvered;
    private Handler mHandler;

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
                        if (CommandDataInfo.CommandDataInfoMessage.CommandType.ResponseType == message.getDataType()) {
                            if (message.getResponseCommand().getResponseCode() == Net.SUCCESS) {
                                isServerResponsed = true;
                                if (isTimeOvered) {
                                    saveDataAndSkipToLogin();
                                }
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(SplashActivity.this).
                                                setMessage(R.string.illegal_device).
                                                setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        finish();
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
                                    .setToken("abcd1")
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
        //todo 保存app版本信息及节目单信息，到首页去下载节目单
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
