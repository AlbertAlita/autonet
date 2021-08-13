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

import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.CommonUtil;
import com.taian.autonet.client.Config;
import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
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
    private long taskId;

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
        int activeListener = WrapNettyClient.getInstance().getActiveListener();
        Log.e(getClass().getSimpleName(), activeListener + "");
        if (activeListener != 0) WrapNettyClient.getInstance().disConnect(this);
        WrapNettyClient.getInstance().connect(NettyTcpClient.reconnectIntervalTime);
        WrapNettyClient.getInstance().addNettyClientListener(SplashActivity.class.getSimpleName(),
                new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
                    @Override
                    public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage message, int index) {
                        if (Config.LOG_TOGGLE)
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
        Aria.download(this).register();
        Aria.download(this)
                .load(url) // 下载地址
                .setFilePath(AppApplication.COMPLETE_CACHE_PATH + File.separator + Constants.APP_APK_NAME) // 设置文件保存路径
                .ignoreFilePathOccupy()
                .create();
    }

    @Download.onWait
    public void onWait(DownloadTask task) {

    }

    @Download.onPre
    public void onPre(DownloadTask task) {
//        DownloadEntity entity = task.getEntity();

    }

    @Download.onTaskStart
    public void onTaskStart(DownloadTask task) {
        DownloadEntity entity = task.getDownloadEntity();
        long fileSize = entity.getFileSize();

        if (Config.LOG_TOGGLE) Log.e(getClass().getSimpleName(), fileSize + "fileSize");
        int haveSpace = Utils.haveSpace(fileSize);
        if (haveSpace == -1) {
            boolean deleteDirectory = Utils.deleteDirectory(AppApplication.COMPLETE_CACHE_PATH);
            Utils.deleteDirectory(AppApplication.COMPLETE_LOG_PATH);
            task.cancel();
            if (deleteDirectory) {
                haveSpace = Utils.haveSpace(fileSize);
                if (haveSpace == -1) {
                    showErrorDialog(getString(R.string.insufficient_disk_space));
                    Aria.download(this).load(entity.getId()).stop();
                } else {
                    Aria.download(this).load(entity.getId()).resume();
                }
            } else {
                showErrorDialog(getString(R.string.insufficient_disk_space));
            }
        }

    }

    @Download.onTaskRunning
    public void onTaskRunning(DownloadTask task) {
        DownloadEntity entity = task.getDownloadEntity();
        long speed = entity.getSpeed();
        if (Config.LOG_TOGGLE) Log.e(getClass().getSimpleName(), "speed" + speed);
        showProgressDialog(getString(R.string.system_upgrading), entity.getPercent(),
                CommonUtil.formatFileSize(speed < 0 ? 0 : speed) + "/s");
    }

    @Download.onTaskFail
    public void onTaskFail(DownloadTask task, Exception e) {
        if (mProgressDialog != null) mProgressDialog.dismiss();
        String reason = getString(R.string.upgrading_and_install_failed);
        DTaskWrapper taskWrapper = task.getTaskWrapper();
        showErrorDialog(taskWrapper == null ?
                reason : reason + "-->" + taskWrapper.getErrorEvent().errorMsg);
    }

    @Download.onTaskComplete
    public void onTaskComplete(final DownloadTask task) {
        if (mProgressDialog != null) mProgressDialog.dismiss();
        final DownloadEntity entity = task.getEntity();
        String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + entity.getFileName();
        AutoInstaller installer = AutoInstaller.getDefault(SplashActivity.this);
        installer.install(localPath);
        installer.setOnStateChangedListener(new AutoInstaller.OnStateChangedListener() {
            @Override
            public void onStart() {
                showProgressBar(getString(R.string.apk_installing));
            }

            @Override
            public void onComplete() {
                hideProgressBar();
                if (Config.LOG_TOGGLE) Log.e("TAG", entity.getFileName());
                Aria.download(SplashActivity.this).load(task.getEntity().getId()).cancel(true);
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
        Aria.download(this).load(taskId).cancel(true);
        Aria.download(this).unRegister();
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
