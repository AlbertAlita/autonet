package com.taian.autonet.client.listener;


import android.util.Log;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.taian.autonet.client.Config;
import com.taian.autonet.client.handler.WrapNettyClient;

import java.util.List;
import java.util.Map;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;


public class CusDownloadListener extends DownloadListener4WithSpeed {

    protected long totalLength, currentOffset;
    protected String speed;


    @Override
    public void taskStart(@NonNull DownloadTask task) {
        Log.e("CusDownloadListener", "taskStart");
    }

    @Override
    public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "connectStart");
    }

    @Override
    public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "connectEnd");
    }

    @Override
    public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint, @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "infoReady");
        totalLength = info.getTotalLength();
    }

    @Override
    public void progressBlock(@NonNull DownloadTask task, int blockIndex, long currentBlockOffset, @NonNull SpeedCalculator blockSpeed) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "progressBlock");
    }

    @Override
    public void progress(@NonNull DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "progress");
        this.currentOffset = currentOffset;
        speed = taskSpeed.speed();
    }

    @Override
    public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info, @NonNull SpeedCalculator blockSpeed) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "blockEnd");
    }

    @Override
    public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull SpeedCalculator taskSpeed) {
        if (Config.LOG_TOGGLE)
            Log.e("CusDownloadListener", "taskEnd ------ " + (cause == null ? "cause == null" : cause.toString()) +
                    " ---- " + (realCause == null ? "realCause == null" : realCause.getMessage()));
    }

}
