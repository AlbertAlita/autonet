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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.CusDownloadListener;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.CommandUtils;
import com.taian.autonet.client.utils.ThreadPoolUtil;
import com.taian.autonet.client.utils.Utils;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.File;

import androidx.appcompat.app.AlertDialog;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import top.wuhaojie.installerlibrary.AutoInstaller;


public class SplashActivity extends BaseActivity {

    boolean isServerResponsed, isTimeOvered;
    private Handler mHandler;
    private CommandDataInfo.PackageConfigCommand packageConfigCommand;
    private ProgressDialog mProgressDialog;
    private AlertDialog errorDiaolog;
    private ThreadPoolUtil threadPoolUtil;
    private DownloadTask task;
    private int state;

    public interface Const {
        int FOR_NEW_IP = 0x01;
        int DELAYED_OPT = 0x02;
        int INSTALLING = -1;
        int UPGRADE_PROGREE = 0x03;
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
        int activeListener = WrapNettyClient.getInstance().getActiveListener();
        Log.e(getClass().getSimpleName(), activeListener + "");
        if (activeListener != 0) WrapNettyClient.getInstance().disConnect(this);
        WrapNettyClient.getInstance().connect(NettyTcpClient.reconnectIntervalTime);
        WrapNettyClient.getInstance().addNettyClientListener(SplashActivity.class.getSimpleName(),
                new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
                    @Override
                    public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage message, int index) {
                        Log.e("TAG", message.toString());
                        if (CommandDataInfo.CommandDataInfoMessage.CommandType.PackageConfigType == message.getDataType()) {
                            CommandDataInfo.PackageConfigCommand packageConfigCommand = message.getPackageConfigCommand();
                            if (packageConfigCommand.getResponseCommand().getResponseCode() == Net.SUCCESS) {
                                SplashActivity.this.packageConfigCommand = packageConfigCommand;
                                if (packageConfigCommand == null || packageConfigCommand.getProgramCommand().getVideoInfoList() == null ||
                                        packageConfigCommand.getProgramCommand().getVideoInfoList().isEmpty()) {
                                    WrapNettyClient.getInstance().responseServer(Net.PROGRAM_ERROR);
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            new AlertDialog.Builder(SplashActivity.this).
                                                    setMessage("尚未配置节目单！").
                                                    show();
                                        }
                                    });
                                } else {
                                    isServerResponsed = true;
                                    WrapNettyClient.getInstance().responseServer(Net.PROGRAM_SUCCESS);
                                    if (isTimeOvered) {
                                        checkApkInfoAndSaveDataAndSkipToLogin();
                                    }
                                }
                            } else {
                                //断开链接
                                WrapNettyClient.getInstance().disConnect(SplashActivity.this);
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
                            CommandUtils.startOrShutDownDevice(SplashActivity.this, brakeValue);
                        } else if (CommandDataInfo.CommandDataInfoMessage.CommandType.BrakeTimingType == message.getDataType()) {
                            CommandDataInfo.BrakeTimingCommand brakeCommand = message.getBrakeTimingCommand();
                            boolean isSuccess = CommandUtils.powerOnOffByAlarm(SplashActivity.this, brakeCommand);
                            if (isSuccess)
                                WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_SUCCESS);
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
        } else otherOpt();
    }

    private void startDownload(final String url) {
        if (threadPoolUtil == null)
            threadPoolUtil = new ThreadPoolUtil(ThreadPoolUtil.Type.SingleThread, 1);
        threadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                if (task == null)
                    task = new DownloadTask.Builder(url,
                            AppApplication.COMPLETE_CACHE_PATH, Constants.APP_APK_NAME) //设置下载地址和下载目录，这两个是必须的参数
                            .setSyncBufferIntervalMillis(64) //写入文件的最小时间间隔，默认2000
                            .build();
                task.enqueue(listener);
            }
        });
    }

    private void otherOpt() {
        //设置定时开关机
        CommandDataInfo.BrakeTimingCommand brakeTimingCommand = packageConfigCommand.getBrakeTimingCommand();
        boolean isSuccess = CommandUtils.powerOnOffByAlarm(this, brakeTimingCommand);
        if (isSuccess) WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_SUCCESS);
        //获取节目单信息
        CommandDataInfo.ProgramCommand programCommand = packageConfigCommand.getProgramCommand();
        //保存节目单到本地
        Utils.updateLocalVideos(this, programCommand);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgressDialog(String title, float progress, String speed) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setTitle(title);
        }
        if (mProgressDialog != null && progress != -1) {
            mProgressDialog.setMessage(getString(R.string.speed, speed));
            mProgressDialog.setProgress((int) progress);
        }
        if (!mProgressDialog.isShowing()) mProgressDialog.show();
    }

    CusDownloadListener listener = new CusDownloadListener() {

        @Override
        public void infoReady(final DownloadTask task, BreakpointInfo info, boolean fromBreakpoint, Listener4SpeedAssistExtend.Listener4SpeedModel model) {
            super.infoReady(task, info, fromBreakpoint, model);
            int haveSpace = Utils.haveSpace(totalLength);
            if (haveSpace == -1) {
                boolean deleteDirectory = Utils.deleteDirectory(AppApplication.COMPLETE_CACHE_PATH);
                Utils.deleteDirectory(AppApplication.COMPLETE_LOG_PATH);
                task.cancel();
                if (deleteDirectory) {
                    haveSpace = Utils.haveSpace(totalLength);
                    if (haveSpace == -1)
                        showErrorDialog(getString(R.string.insufficient_disk_space));
                    else {
                        threadPoolUtil.execute(new Runnable() {
                            @Override
                            public void run() {
                                task.execute(listener);
                            }
                        });
                    }
                } else {
                    showErrorDialog(getString(R.string.insufficient_disk_space));
                }
            }
        }

        @Override
        public void progress(@NonNull DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed) {
            super.progress(task, currentOffset, taskSpeed);
            float percent = (float) currentOffset / totalLength * 100;
            showProgressDialog(getString(R.string.system_upgrading), percent, taskSpeed.speed());
        }

        @Override
        public void taskEnd(@NonNull final DownloadTask task, @NonNull EndCause cause,
                            @Nullable Exception realCause, @NonNull SpeedCalculator taskSpeed) {
            super.taskEnd(task, cause, realCause, taskSpeed);
            if (cause == null) return;
            if (cause == EndCause.COMPLETED) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + task.getFilename();
                AutoInstaller installer = AutoInstaller.getDefault(SplashActivity.this);
                installer.install(localPath);
                installer.setOnStateChangedListener(new AutoInstaller.OnStateChangedListener() {
                    @Override
                    public void onStart() {
                        showProgressBar(getString(R.string.apk_installing));
                    }

                    @Override
                    public void onComplete() {
                        if (mProgressDialog != null) mProgressDialog.dismiss();
                        Utils.deleteFile(task.getFilename());
                        Intent intent = new Intent(Constants.RE_BOOT);
                        sendBroadcast(intent);
                        hideProgressBar();
                    }

                    @Override
                    public void onNeed2OpenService() {
                        hideProgressBar();
                        Toast.makeText(SplashActivity.this, "请打开辅助功能服务", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void needPermission() {
                        hideProgressBar();
                        Toast.makeText(SplashActivity.this, "需要申请存储空间权限", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (cause == EndCause.ERROR) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                showErrorDialog(realCause == null ?
                        getString(R.string.upgrading_and_install_failed) : realCause.getMessage());
            }
        }
    };


    private void showErrorDialog(String reason) {
        if (errorDiaolog == null) {
            errorDiaolog = new AlertDialog.Builder(this)
                    .create();
            errorDiaolog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    otherOpt();
                }
            });
        }
        errorDiaolog.setMessage(reason);
        errorDiaolog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.FOR_NEW_IP) {
            if (mHandler != null)
                mHandler.sendEmptyMessageDelayed(Const.DELAYED_OPT, 3000);
            WrapNettyClient.getInstance().connect(NettyTcpClient.reconnectIntervalTime);
        }
    }

    @Override
    protected void onNetConnect() {
        WrapNettyClient.getInstance().connect(NettyTcpClient.reconnectIntervalTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (errorDiaolog != null) errorDiaolog.dismiss();
        WrapNettyClient.getInstance().removeListener(SplashActivity.class.getSimpleName());
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        WrapNettyClient.getInstance().disConnect(this);
        WrapNettyClient.getInstance().removeListener(SplashActivity.class.getSimpleName());
        Utils.exitApp(this);
    }
}
