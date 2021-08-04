package com.taian.autonet.client;

import android.util.Log;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.taian.autonet.AppApplication;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.utils.ThreadPoolUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadDelegate {


    private List<VideoInfo> cachedVideoList;
    private int index = 1;
    private ThreadPoolUtil threadPoolUtil;

    public DownloadDelegate(List<VideoInfo> cachedVideoList) {
        this.cachedVideoList = cachedVideoList;
    }

    public List<VideoInfo> getCachedVideoList() {
        return cachedVideoList == null ? new ArrayList<VideoInfo>() : cachedVideoList;
    }

    public void startDownloadTask(final DownloadListener listener) {
        if (cachedVideoList == null || cachedVideoList.isEmpty()) return;
        if (threadPoolUtil == null)
            threadPoolUtil = new ThreadPoolUtil(ThreadPoolUtil.Type.SingleThread, 1);
        threadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                VideoInfo currentVideo = getCurrentVideo();
                DownloadTask mTask = new DownloadTask.Builder(currentVideo.getVideoPath(),
                        AppApplication.COMPLETE_CACHE_PATH, currentVideo.getVideoName())
                        .setMinIntervalMillisCallbackProcess(64)
                        .build();
                mTask.execute(listener);
            }
        });
    }

    public VideoInfo getCurrentVideo() {
        int size = cachedVideoList.size();
        int positon = -1;
        for (int i = 0; i < size; i++) {
            int pos = cachedVideoList.indexOf(new VideoInfo(index));
            if (pos != -1) {
                positon = pos;
                break;
            } else {
                index += 1;
                if (index > size) index = 1;
            }
        }
        try {
            return cachedVideoList.get(positon);
        } catch (Exception e) {
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setCode(Constants.ERROR);
            videoInfo.setVideoName("找不到节目单");
            return videoInfo;
        }
    }

    public BreakpointInfo updateIndex() {
        index += 1;
        if (index > cachedVideoList.size()) index = 1;
        //缺这个编号就跳到下一个
        if (getCurrentVideo() == null) {
            index += 1;
            if (index > cachedVideoList.size()) index = 1;
        }
        BreakpointInfo currentInfo = StatusUtil.getCurrentInfo(getCurrentVideo().getVideoPath(),
                AppApplication.COMPLETE_CACHE_PATH, getCurrentVideo().getVideoName());
        if (currentInfo != null) {
            long totalOffset = currentInfo.getTotalOffset();
            long totalLength = currentInfo.getTotalLength();
            if (Config.LOG_TOGGLE)
                Log.e("TAG", totalOffset + "------" + totalLength);
        }
        return currentInfo;
    }

    public void reDownload(String url) {
//        tasks.get(url).execute(listeners.get(url));
    }
}
