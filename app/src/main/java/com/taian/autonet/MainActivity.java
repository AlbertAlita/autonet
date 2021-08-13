package com.taian.autonet;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.FileUtil;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.Config;
import com.taian.autonet.client.DownloadDelegate;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.listener.CusDownloadListener;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.ActivityUtil;
import com.taian.autonet.client.utils.CommandUtils;
import com.taian.autonet.client.utils.SpUtils;
import com.taian.autonet.client.utils.ThreadPoolUtil;
import com.taian.autonet.client.utils.Utils;
import com.taian.autonet.view.StandardVideoController;
import com.video.netty.protobuf.CommandDataInfo;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import xyz.doikki.videoplayer.player.VideoView;

public class MainActivity extends BaseActivity {

    private ProgressDialog mProgressDialog;
    private VideoView mVideoView;
    private StandardVideoController mController;
    private DownloadDelegate mDownloadDelegate;
    private AlertDialog errorDiaolog;
    private DownloadTask mTask;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = findViewById(R.id.player);

        mHandler = new Handler(getMainLooper());
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
                            realDownload();
                        }
                        break;
                    case VideoView.STATE_ERROR:
                        Toast.makeText(MainActivity.this, R.string.video_play_error, Toast.LENGTH_LONG).show();
                        if (mDownloadDelegate != null) {
                            String url = mDownloadDelegate.getCurrentVideo().getVideoPath();
//                            boolean deleteFile = Utils.deleteFile(videoName);
                            Aria.download(MainActivity.this).load(url).cancel(true);
                        }
                        break;
                }
            }
        });
    }


    /**
     * 下载视频
     */
    private void downloadVideos() {
        List<VideoInfo> cachedVideoList = SpUtils.
                getCachedVideoList(this, Constants.VIDEO_LIST);
        mDownloadDelegate = new DownloadDelegate(cachedVideoList);
        Aria.download(this).register();
        realDownload();
    }

    @Download.onWait
    void taskWait(DownloadTask task) {
        long totalLength = task.getEntity().getFileSize();
        if (Config.LOG_TOGGLE)
            Log.e(getClass().getSimpleName(), task.getEntity().toString() + "--- totalLength ----" + totalLength);
        int haveSpace = Utils.haveSpace(totalLength);
        if (haveSpace == -1) {
            List<VideoInfo> cachedVideoList = mDownloadDelegate.getCachedVideoList();
            List<String> list = new ArrayList<>();
            for (VideoInfo videoInfo : cachedVideoList) {
                list.add(AppApplication.COMPLETE_CACHE_PATH + File.separator + videoInfo.getVideoName());
            }
            boolean deleteDirectory = Utils.deleteDirectory(AppApplication.COMPLETE_CACHE_PATH, list);
            Utils.deleteDirectory(AppApplication.COMPLETE_LOG_PATH);
            task.cancel();
            if (deleteDirectory) {
                haveSpace = Utils.haveSpace(totalLength);
                if (haveSpace == -1)
                    showErrorDialog(getString(R.string.insufficient_disk_space));
                else {
                    realDownload();
                }
            } else {
                showErrorDialog(getString(R.string.insufficient_disk_space));
            }
        }
    }

    @Download.onPre
    void onPre(DownloadTask task) {
        long totalLength = task.getEntity().getFileSize();
        if (Config.LOG_TOGGLE)
            Log.e(getClass().getSimpleName(), task.getEntity().toString() + "--- totalLength ----" + totalLength);
    }

    @Download.onTaskStart
    void taskStart(DownloadTask task) {

    }

    @Download.onTaskRunning
    void taskRunning(DownloadTask task) {
        showProgressDialog(getString(R.string.video_downloading), task.getPercent(), task.getConvertSpeed());
    }

    @Download.onTaskFail
    void taskFail(DownloadTask task) {
        try {
            String errorMsg = task.getTaskWrapper().getErrorEvent().errorMsg;
            downloadError(errorMsg);
        } catch (Exception e) {
            downloadError(getString(R.string.unkown_error_1));
        }
    }

    @Download.onTaskComplete
    void taskComplete(DownloadTask task) {
        downloadComplete(task);
    }

    private void realDownload() {
        mDownloadDelegate.startDownloadTask(this);
    }

    private void downloadComplete(@NonNull DownloadTask task) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        String localPath = AppApplication.COMPLETE_CACHE_PATH + File.separator + task.getEntity().getFileName();
        mVideoView.release();
        mController.getTitleView().setTitle(task.getEntity().getFileName());
        mVideoView.setVideoController(mController); //设置控制器
        mVideoView.setUrl(localPath);
        mVideoView.start();
    }

    private void downloadError(String msg) {
        if (mProgressDialog != null) mProgressDialog.dismiss();
        if (Utils.checkNet(this)) {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            realDownload();
        }
    }

    private void showErrorDialog(String reason) {
        if (errorDiaolog == null)
            errorDiaolog = new AlertDialog.Builder(this)
                    .create();
        errorDiaolog.setMessage(reason);
        errorDiaolog.show();
    }

    @Override
    protected void onNetConnect() {
        realDownload();
    }

    /**
     * 接收指令
     */
    private void registerCommandListenter() {
        WrapNettyClient.getInstance().addNettyClientListener(getClass().getSimpleName(),
                new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
                    @Override
                    public void onMessageResponseClient(final CommandDataInfo.CommandDataInfoMessage message, int index) {
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                showErrorDialog(message.toString());
//                            }
//                        });
                        if (message.getDataType() == CommandDataInfo.CommandDataInfoMessage.CommandType.VoiceType) {
                            int voiceValue = message.getVoiceCommand().getVoiceValue();
                            CommandUtils.updateVolume(MainActivity.this, voiceValue);
                        } else if (CommandDataInfo.CommandDataInfoMessage.CommandType.BrakeType == message.getDataType()) {
                            CommandDataInfo.BrakeCommand brakeCommand = message.getBrakeCommand();
                            int brakeValue = brakeCommand.getBrakeValue();
                            CommandUtils.startOrShutDownDevice(MainActivity.this, brakeValue);
                        } else if (CommandDataInfo.CommandDataInfoMessage.CommandType.BrakeTimingType == message.getDataType()) {
                            CommandDataInfo.BrakeTimingCommand brakeCommand = message.getBrakeTimingCommand();
                            boolean isSuccess = CommandUtils.powerOnOffByAlarm(MainActivity.this, brakeCommand);
                            if (isSuccess)
                                WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_SUCCESS);
                        }
                    }

                    @Override
                    public void onClientStatusConnectChanged(int statusCode, int index) {
                        if (statusCode == ConnectState.STATUS_CONNECT_ERROR) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, R.string.net_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (statusCode == ConnectState.STATUS_CONNECT_SUCCESS) {
                            CommandDataInfo.TokenCommand tokenCommand = CommandDataInfo.TokenCommand.newBuilder()
                                    .setToken(AppApplication.getMacAdress())
                                    .build();
                            CommandDataInfo.CommandDataInfoMessage command = CommandDataInfo.CommandDataInfoMessage.newBuilder()
                                    .setDataType(CommandDataInfo.CommandDataInfoMessage.CommandType.TokenType)
                                    .setTokenCommand(tokenCommand)
                                    .build();
                            WrapNettyClient.getInstance().sendMsgToServer(command);
                        }
                    }
                });
    }


    private void showProgressDialog(String title, final float progress, final String speed) {
        if (this.isFinishing()) return;
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setProgress(0);
            mProgressDialog.setTitle(title);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(100);
        }
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.speed, speed));
            mProgressDialog.setProgress((int) progress);
        }
        if (!mProgressDialog.isShowing()) mProgressDialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
        Aria.download(this).unRegister();
        WrapNettyClient.getInstance().disConnect(this);
        WrapNettyClient.getInstance().removeListener(getClass().getSimpleName());
        if (mTask != null) mTask.cancel();
    }

    @Override
    public void onBackPressed() {
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
        super.onBackPressed();
    }
}
