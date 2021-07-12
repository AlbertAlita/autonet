package com.taian.autonet;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;
import com.taian.autonet.bean.ApkInfo;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.CusDownloadListener;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.PermissionUtils;
import com.taian.autonet.client.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.functions.Consumer;

public class SplashActivity extends BaseActivity {

    boolean isServerResponsed, isTimeOvered;
    private Handler mHandler;
    private CommandDataInfo.PackageConfigCommand packageConfigCommand;
    private ProgressDialog mProgressDialog;
    private AlertDialog errorDiaolog;
    private RxPermissions mRxPermission;

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
    }

    private void initHandler() {
        mHandler = new Handler(getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isTimeOvered = true;
                if (isServerResponsed) {
                    checkApkInfoAndSaveDataAndSkipToLogin();
                }
            }
        }, 3000);
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
                                                    checkApkInfoAndSaveDataAndSkipToLogin();
                                                }
                                            }).
                                            show();
                                }
                            });
                        }
                    }
                });
    }

    private void initPermission() {
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
                                new AlertDialog.Builder(SplashActivity.this).
                                        setMessage(R.string.none_permission_warning).
                                        setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Utils.exitApp(SplashActivity.this);
                                            }
                                        }).
                                        show();
                            }
                        }
                    });
        }
    }

    private void initFolder() {
        //初始化log文件夹
        Utils.initFolderPath(SplashActivity.this, Constants.LOG_PATH);
        //初始化文件缓存文件夹
        AppApplication.COMPLETE_CACHE_PATH =
                Utils.initFolderPath(SplashActivity.this, Constants.CACHE_PATH);
    }

    private void checkApkInfoAndSaveDataAndSkipToLogin() {
        if (packageConfigCommand == null) {
            return;
        }
        //获取版本号信息
        CommandDataInfo.ApkVersionCommand apkVersionCommand = packageConfigCommand.getApkVersionCommand();
//        if (Utils.getAppVersionCode(this, getPackageName()) < apkVersionCommand.getVersion()) {
        String url = apkVersionCommand.getFilePath();
        startDownload(url);
//        }
//        else saveVideos();
    }

    private void startDownload(String url) {
        DownloadTask task = new DownloadTask.Builder(url, AppApplication.COMPLETE_CACHE_PATH, Constants.APP_APK_NAME) //设置下载地址和下载目录，这两个是必须的参数
                .setFilename(Constants.APP_APK_NAME)
                .setFilenameFromResponse(false)//是否使用 response header or url path 作为文件名，此时会忽略指定的文件名，默认false
                .setConnectionCount(1)  //需要用几个线程来下载文件，默认根据文件大小确定；如果文件已经 split block，则设置后无效
                .setPreAllocateLength(false) //在获取资源长度后，设置是否需要为文件预分配长度，默认false
                .setMinIntervalMillisCallbackProcess(1000) //通知调用者的频率，避免anr，默认3000
                .setWifiRequired(false)//是否只允许wifi下载，默认为false
                .setAutoCallbackToUIThread(true) //是否在主线程通知调用者，默认为true
                .setPriority(0)//设置优先级，默认值是0，值越大下载优先级越高
                .setReadBufferSize(4096)//设置读取缓存区大小，默认4096
                .setFlushBufferSize(16384)//设置写入缓存区大小，默认16384
                .setSyncBufferSize(65536)//写入到文件的缓冲区大小，默认65536
                .setSyncBufferIntervalMillis(2000) //写入文件的最小时间间隔，默认2000
                .build();
        task.enqueue(listener);
    }


    private void saveVideos() {
        //获取节目单信息
        CommandDataInfo.ProgramCommand programCommand = packageConfigCommand.getProgramCommand();
        //保存节目单到本地
        Utils.updateLocalVideos(this, programCommand);
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        finish();
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
        public void taskStart(@NonNull DownloadTask task) {
            super.taskStart(task);
        }

        @Override
        public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
            super.fetchProgress(task, blockIndex, increaseBytes);

        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
            super.taskEnd(task, cause, realCause);
            if (cause == EndCause.COMPLETED) {
                String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + task.getFilename();
                int state = Utils.installPkg(localPath);
                if (state != ApkInfo.INSTALLING) showErrorDialog(state);
            } else if (cause == EndCause.ERROR) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                showErrorDialog(ApkInfo.DOWNLOAD_FAILED);
            }
        }
    };


//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void update(final DownloadInfo info) {
//        if (info == null) return;
//        if (DownloadInfo.DOWNLOAD.equals(info.getDownloadStatus())) {
//            float progress = (float) info.getProgress() / (float) info.getTotal() * 100;
//            showProgressDialog(getString(R.string.upgrading_app), progress);
//        } else if (DownloadInfo.DOWNLOAD_OVER.equals(info.getDownloadStatus())) {
//            String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + info.getFileName();
//            int state = Utils.installPkg(localPath);
//            if (state != ApkInfo.INSTALLING) showErrorDialog(state);
//        } else if (DownloadInfo.DOWNLOAD_PAUSE.equals(info.getDownloadStatus())) {
//            //暂停下载。目前没这功能
//        } else if (DownloadInfo.DOWNLOAD_CANCEL.equals(info.getDownloadStatus())) {
//            //取消下载，目前没这功能
//        } else if (DownloadInfo.DOWNLOAD_ERROR.equals(info.getDownloadStatus())) {
//            if (mProgressDialog != null) mProgressDialog.dismiss();
//            showErrorDialog(ApkInfo.DOWNLOAD_FAILED);
//        } else if (DownloadInfo.DOWNLOAD_BEYOND_INDEX.equals(info.getDownloadStatus())) {
//            new AlertDialog.Builder(this).
//                    setMessage(R.string.last_video).
//                    setNegativeButton(R.string.confirm, null).
//                    show();
//        }
//    }

    private void showErrorDialog(final int state) {
        if (errorDiaolog == null)
            errorDiaolog = new AlertDialog.Builder(this).
                    setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveVideos();
                        }
                    }).
                    setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Utils.exitApp(SplashActivity.this);
                        }
                    }).create();
        errorDiaolog.setMessage(ApkInfo.getErrorTips(this, state));
        errorDiaolog.show();
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
