package com.taian.autonet.client.listener;


import android.util.Log;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CusDownloadListener implements DownloadListener {

    @Override
    public void taskStart(@NonNull DownloadTask task) {
        Log.e("CusDownloadListener", "taskStart");
    }

    @Override
    public void connectTrialStart(@NonNull DownloadTask task, @NonNull Map<String, List<String>> requestHeaderFields) {
        Log.e("CusDownloadListener", "connectTrialStart");
    }

    @Override
    public void connectTrialEnd(@NonNull DownloadTask task, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
        Log.e("CusDownloadListener", "connectTrialEnd");
    }

    @Override
    public void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info, @NonNull ResumeFailedCause cause) {
        Log.e("CusDownloadListener", "downloadFromBeginning");
    }

    @Override
    public void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        Log.e("CusDownloadListener", "downloadFromBreakpoint");
    }

    @Override
    public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {
        Log.e("CusDownloadListener", "connectStart");
    }

    @Override
    public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
        Log.e("CusDownloadListener", "connectEnd");
    }

    @Override
    public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
        Log.e("CusDownloadListener", "fetchStart");
    }

    @Override
    public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
        Log.e("CusDownloadListener", "fetchProgress");
    }

    @Override
    public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
        Log.e("CusDownloadListener", "fetchEnd");
    }

    @Override
    public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
        Log.e("CusDownloadListener", "taskEnd ------ " + cause.toString() +
                " ---- " + (realCause == null ? "" : realCause.getMessage());
    }
}
