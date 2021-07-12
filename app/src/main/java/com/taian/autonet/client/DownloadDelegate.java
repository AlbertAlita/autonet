package com.taian.autonet.client;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.taian.autonet.AppApplication;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadDelegate {


    private List<VideoInfo> cachedVideoList;
    private int index = 1;
    private Map<String, DownloadTask> tasks = new HashMap<>();
    private Map<String, DownloadListener> listeners = new HashMap<>();

    public DownloadDelegate(List<VideoInfo> cachedVideoList) {
        this.cachedVideoList = cachedVideoList;
    }

    public void addDownloadListener(DownloadListener listener) {
        VideoInfo currentVideo = getCurrentVideo();
        if (currentVideo != null) listeners.put(currentVideo.getVideoPath(), listener);
    }

    public void startDownloadTask() {
        if (cachedVideoList == null || cachedVideoList.isEmpty()) return;
        if (index > cachedVideoList.size()) index = 1;
        VideoInfo currentVideo = getCurrentVideo();
        if (currentVideo == null) return;
        DownloadTask mTask = new DownloadTask.Builder(currentVideo.getVideoPath(),
                AppApplication.COMPLETE_CACHE_PATH, Constants.APP_APK_NAME) //设置下载地址和下载目录，这两个是必须的参数
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


        mTask.enqueue(listeners.get(currentVideo.getVideoPath()));
        tasks.put(currentVideo.getVideoPath(), mTask);
    }

    private VideoInfo getCurrentVideo() {
        if (cachedVideoList != null) {
            for (VideoInfo videoInfo : cachedVideoList) {
                if (videoInfo.getVideoNumber() == index) {
                    return videoInfo;
                }
            }
        }
        return null;
    }

    public void updateIndex() {
        index += 1;
    }

    public void reDownload(String url) {
        tasks.get(url).execute(listeners.get(url));
    }
}
