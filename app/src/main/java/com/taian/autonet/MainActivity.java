package com.taian.autonet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.taian.autonet.bean.ApkInfo;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.DownloadDelegate;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.CusDownloadListener;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.utils.PermissionUtils;
import com.taian.autonet.client.utils.SpUtils;
import com.taian.autonet.client.utils.Utils;
import com.taian.autonet.view.StandardVideoController;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.video.netty.protobuf.CommandDataInfo;


import java.io.File;
import java.util.List;

import io.reactivex.functions.Consumer;
import xyz.doikki.videoplayer.player.VideoView;

public class MainActivity extends BaseActivity {

    private RxPermissions mRxPermission;
    private ProgressDialog mProgressDialog;
    private Handler mHandler;
    private VideoView mVideoView;
    private StandardVideoController mController;
    private DownloadDelegate mDownloadDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(getMainLooper());
        mVideoView = findViewById(R.id.player);

        initPermission();
        initPlayer();
        downloadVideos();
        registerCommandListenter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) mVideoView.resume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) mVideoView.pause();
    }


    private void initPlayer() {
        mController = new StandardVideoController(this);
        mController.addDefaultControlComponent("", false);
        mController.setListener(new StandardVideoController.PlayStateChangedListener() {
            @Override
            public void onPlayStateChanged(int state) {
                switch (state) {
                    //播放结束
                    case VideoView.STATE_PLAYBACK_COMPLETED:
                        if (mDownloadDelegate != null) {
                            mDownloadDelegate.updateIndex();
                            mDownloadDelegate.startDownloadTask();
                        }
                        break;
                    case VideoView.STATE_ERROR:
                        new AlertDialog.Builder(MainActivity.this).
                                setMessage(R.string.play_error).
                                setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
//                                        if (mDownloadDelegate != null)
//                                            mDownloadDelegate.reDownload();
                                    }
                                }).
                                setNegativeButton(R.string.cancle, null).
                                show();
                        break;
                }
            }
        });
    }

    /**
     * 初始化权限
     */
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
                                new AlertDialog.Builder(MainActivity.this).
                                        setMessage(R.string.none_permission_warning).
                                        setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Utils.exitApp(MainActivity.this);
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
        Utils.initFolderPath(MainActivity.this, Constants.LOG_PATH);
        //初始化文件缓存文件夹
        AppApplication.COMPLETE_CACHE_PATH =
                Utils.initFolderPath(MainActivity.this, Constants.CACHE_PATH);
    }


    /**
     * 下载视频
     */
    private void downloadVideos() {
        List<VideoInfo> cachedVideoList = SpUtils.
                getCachedVideoList(this, Constants.VIDEO_LIST);
        mDownloadDelegate = new DownloadDelegate(cachedVideoList);
        mDownloadDelegate.addDownloadListener(new CusDownloadListener() {
            @Override
            public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
                super.fetchProgress(task, blockIndex, increaseBytes);
                float percent = (float) increaseBytes / totalSize * 100;
                showProgressDialog(getString(R.string.system_upgrading), percent);
            }

            @Override
            public void taskEnd(@NonNull final DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
                super.taskEnd(task, cause, realCause);
                if (cause == EndCause.COMPLETED) {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + task.getFilename();
                    mVideoView.release();
                    mController.getTitleView().setTitle(task.getFilename());
                    mVideoView.setVideoController(mController); //设置控制器
                    mVideoView.setUrl(localPath);
                    mVideoView.start();
                } else if (cause == EndCause.ERROR) {
                    if (mProgressDialog != null) mProgressDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this).
                            setMessage(R.string.download_error).
                            setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mDownloadDelegate != null)
                                        mDownloadDelegate.reDownload(task.getUrl());
                                }
                            }).
                            setNegativeButton(R.string.cancle, null).
                            show();
                }
            }
        });
    }


    /**
     * 接收指令
     */
    private void registerCommandListenter() {
        WrapNettyClient.getInstance().addNettyClientListener(getClass().getSimpleName(),
                new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
                    @Override
                    public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage msg, int index) {

                    }

                    @Override
                    public void onClientStatusConnectChanged(int statusCode, int index) {

                    }
                });
    }


    private void showProgressDialog(String title, final float progress) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(MainActivity.this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) mVideoView.release();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
