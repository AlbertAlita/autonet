package com.taian.autonet.download;


import android.text.TextUtils;
import android.util.Log;

import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.download.base.IDownload;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class DownloadDelegate implements IDownload {

    private List<VideoInfo> cachedVideoList;
    private String downloadingUrl;

    private int index = 0;

    public void setIndex(int index) {
        this.index = index;
    }

    public DownloadDelegate(List<VideoInfo> cachedVideoList) {
        this.cachedVideoList = cachedVideoList;
    }

    @Override
    public void startDownload() {
        if (cachedVideoList != null) {
            if (index >= cachedVideoList.size()) {
                sendDownStatus(DownloadInfo.DOWNLOAD_BEYOND_INDEX, "");
            }
            try {
                VideoInfo videoInfo = cachedVideoList.get(0);
                String videoPath = videoInfo.getVideoPath();
                Log.e("TAG", videoPath);
                DownloadManager.getInstance().download(videoPath);
            } catch (Exception e) {
                e.printStackTrace();
                sendDownStatus(DownloadInfo.DOWNLOAD_ERROR, e.getMessage());
            }
        }
    }

    private void sendDownStatus(String status, String msg) {
        DownloadInfo downloadInfo = new DownloadInfo(null);
        downloadInfo.setDownloadStatus(status);
        downloadInfo.setMsg(msg);
        EventBus.getDefault().post(downloadInfo);
    }

    @Override
    public void pauseDownload(String url) {
        if (!TextUtils.isEmpty(downloadingUrl))
            DownloadManager.getInstance().pauseDownload(downloadingUrl);
    }

    @Override
    public void cancelDownload(DownloadInfo downloadInfo) {
        if (downloadInfo != null)
            DownloadManager.getInstance().cancelDownload(downloadInfo);
    }
}
