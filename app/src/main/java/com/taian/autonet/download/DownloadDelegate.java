package com.taian.autonet.download;


import android.text.TextUtils;
import android.util.Log;

import com.taian.autonet.AppApplication;
import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.utils.Utils;
import com.taian.autonet.download.base.IDownload;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

public class DownloadDelegate implements IDownload {

    private List<VideoInfo> cachedVideoList;
    private String downloadingUrl;

    private int index = 1;//就是记录videoNumber的变量

    public void setIndex(int index) {
        this.index = index;
    }

    public DownloadDelegate(List<VideoInfo> cachedVideoList) {
        this.cachedVideoList = cachedVideoList;
    }

    @Override
    public void startDownload() {
        if (cachedVideoList != null) {
            if (index > cachedVideoList.size()) {
                index = 1;
            }
            try {
                VideoInfo videoInfo = getNextVideoInfo(index);
                String videoPath = videoInfo.getVideoPath();
                DownloadManager.getInstance().download(videoPath, videoInfo.getVideoName());
            } catch (Exception e) {
                e.printStackTrace();
                sendDownStatus(DownloadInfo.DOWNLOAD_ERROR, e.getMessage());
            }
        }
    }

    public void reDownload() {
        try {
            VideoInfo videoInfo = getNextVideoInfo(index);
            boolean deleteFile = Utils.deleteFile(videoInfo.getVideoName());
            Log.e("TAG1",deleteFile +"");
            if (deleteFile)
                DownloadManager.getInstance().download(videoInfo.getVideoPath(), videoInfo.getVideoName());
        } catch (Exception e) {
//            Log.e("TAG",e.getMessage());
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

    private VideoInfo getNextVideoInfo(int index) {
        if (cachedVideoList == null) return null;
        for (VideoInfo data : cachedVideoList) {
            if (data.getVideoNumber() == index)
                return data;
        }
        return null;
    }

    public void updateIndex() {
        index += 1;
    }
}
