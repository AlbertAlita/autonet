package com.taian.autonet;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.taian.autonet.bean.ApkInfo;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.CusDownloadListener;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.ActivityUtil;
import com.taian.autonet.client.utils.Utils;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.File;
import java.util.concurrent.Delayed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;


public class SplashActivity extends BaseActivity {

    boolean isServerResponsed, isTimeOvered;
    private Handler mHandler;
    private CommandDataInfo.PackageConfigCommand packageConfigCommand;
    private ProgressDialog mProgressDialog;
    private AlertDialog errorDiaolog;

    public interface Const {
        int FOR_NEW_IP = 0x01;
        int DELAYED_OPT = 0x02;

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initView();
        initPermission();
        initHandler();
        connectServerAndGetBaiscInfo();
    }

    private void initView() {
        Typeface fontFace = Typeface.createFromAsset(getAssets(), "xingshu.TTF");
        TextView title = findViewById(R.id.title);
        TextView subtitle = findViewById(R.id.subtitle);
        title.setTypeface(fontFace);
        subtitle.setTypeface(fontFace);
        findViewById(R.id.iv_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHandler != null) {
                    mHandler.removeCallbacksAndMessages(null);
                    startActivityForResult(new Intent(SplashActivity.this, SettingActivity.class), Const.FOR_NEW_IP);
                }
            }
        });
    }

    private void initHandler() {
        if (mHandler == null)
            mHandler = new Handler(getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case Const.DELAYED_OPT:
                            isTimeOvered = true;
                            if (isServerResponsed) {
                                checkApkInfoAndSaveDataAndSkipToLogin();
                            }
                            break;
                    }
                }
            };
        mHandler.sendEmptyMessageDelayed(Const.DELAYED_OPT, 3000);
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
                                if (packageConfigCommand == null || packageConfigCommand.getProgramCommand().getVideoInfoList() == null)
                                    WrapNettyClient.getInstance().responseServer(Net.PROGRAM_ERROR);
                                else
                                    WrapNettyClient.getInstance().responseServer(Net.PROGRAM_SUCCESS);
                                if (isTimeOvered) {
                                    checkApkInfoAndSaveDataAndSkipToLogin();
                                }
                            } else {
                                //断开链接
                                WrapNettyClient.getInstance().disConnect();
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(SplashActivity.this).
                                                setMessage(R.string.illegal_device).
                                                show();
                                    }
                                });
                            }
                        } else if (CommandDataInfo.CommandDataInfoMessage.CommandType.BrakeType == message.getDataType()) {
                            CommandDataInfo.BrakeCommand brakeCommand = message.getBrakeCommand();
                            int brakeValue = brakeCommand.getBrakeValue();
                            if (brakeValue == 0) {
                                Intent intent = new Intent(Constants.SHUT_DOWN);
                                sendBroadcast(intent);
                            } else if (brakeValue == 1) {
                                //重启
                                Intent intent = new Intent(Constants.RE_BOOT);
                                sendBroadcast(intent);
                            }
                            if (brakeValue != 0 && brakeValue != 1)
                                WrapNettyClient.getInstance().responseServer(Net.BRAKE_ERROR);
                            else WrapNettyClient.getInstance().responseServer(Net.BRAKE_SUCCESS);
                        } else if (CommandDataInfo.CommandDataInfoMessage.CommandType.BrakeTimingType == message.getDataType()) {
//                            CommandDataInfo.BrakeTimingCommand brakeCommand = message.getBrakeTimingCommand();
//                            String openBrake = brakeCommand.getOpenBrake();
//                            String closeBrake = brakeCommand.getCloseBrake();
//                            WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_SUCCESS);
                        }
                    }

                    @Override
                    public void onClientStatusConnectChanged(int statusCode, int index) {
                        if (statusCode == ConnectState.STATUS_CONNECT_SUCCESS) {
                            //tcp链接成功
                            Log.e("TAG", "tcp链接成功");
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
                                    Toast.makeText(SplashActivity.this, R.string.net_error, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
    }


    private void checkApkInfoAndSaveDataAndSkipToLogin() {
        if (packageConfigCommand == null) {
            return;
        }
        //获取版本号信息
        CommandDataInfo.ApkVersionCommand apkVersionCommand = packageConfigCommand.getApkVersionCommand();
        if (Utils.getAppVersionCode(this, getPackageName()) < apkVersionCommand.getVersion()) {
            String url = apkVersionCommand.getFilePath();
            startDownload(url);
        } else saveVideos();
    }

    private void startDownload(String url) {
        DownloadTask task = new DownloadTask.Builder(url,
                AppApplication.COMPLETE_CACHE_PATH, Constants.APP_APK_NAME) //设置下载地址和下载目录，这两个是必须的参数
                .setSyncBufferIntervalMillis(64) //写入文件的最小时间间隔，默认2000
                .build();
        task.enqueue(listener);
    }


    private void saveVideos() {
        //获取节目单信息
        CommandDataInfo.ProgramCommand programCommand = packageConfigCommand.getProgramCommand();
        //保存节目单到本地
        Utils.updateLocalVideos(this, programCommand);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgressDialog(String title, final float progress) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgress(0);
            mProgressDialog.setTitle(title);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(100);
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setMax(100);
                    mProgressDialog.setProgress((int) progress);
                }
            }
        }, 1000);
        if (!mProgressDialog.isShowing()) mProgressDialog.show();
    }

    CusDownloadListener listener = new CusDownloadListener() {

        @Override
        public void progress(@NonNull DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed) {
            super.progress(task, currentOffset, taskSpeed);
            float percent = (float) currentOffset / totalLength * 100;
            showProgressDialog(getString(R.string.system_upgrading), percent);
        }

        @Override
        public void taskEnd(@NonNull final DownloadTask task, @NonNull EndCause cause,
                            @Nullable Exception realCause, @NonNull SpeedCalculator taskSpeed) {
            super.taskEnd(task, cause, realCause);
            if (cause == null) return;
            if (cause == EndCause.COMPLETED) {
                String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + task.getFilename();
                int state = Utils.installPkg(localPath);
                if (mProgressDialog != null) mProgressDialog.dismiss();
                if (state != ApkInfo.INSTALLING) showErrorDialog(state);
                else {
                    //重启机器
                    Intent intent = new Intent(Constants.RE_BOOT);
                    sendBroadcast(intent);
                }
            } else if (cause == EndCause.ERROR) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                showErrorDialog(ApkInfo.DOWNLOAD_FAILED);
            }
        }
    };


    private void showErrorDialog(final int state) {
//        if (errorDiaolog == null)
//            errorDiaolog = new AlertDialog.Builder(this).
//                    setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            saveVideos();
//                        }
//                    }).
//                    setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Utils.exitApp(SplashActivity.this);
//                        }
//                    }).create();
        Toast.makeText(this, ApkInfo.getErrorTips(this, state), Toast.LENGTH_LONG).show();
//        errorDiaolog.setMessage(ApkInfo.getErrorTips(this, state));
//        errorDiaolog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.FOR_NEW_IP) {
            if (mHandler != null)
                mHandler.sendEmptyMessageDelayed(Const.DELAYED_OPT, 3000);
            WrapNettyClient.getInstance().connect();
        }
    }

    @Override
    protected void onNetConnect() {
//        Toast.makeText(this, R.string.net_available, Toast.LENGTH_LONG).show();
        WrapNettyClient.getInstance().connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (errorDiaolog != null) errorDiaolog.dismiss();
        WrapNettyClient.getInstance().removeListener(SplashActivity.class.getSimpleName());
        WrapNettyClient.getInstance().disConnect();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Utils.exitApp(this);
    }
}
