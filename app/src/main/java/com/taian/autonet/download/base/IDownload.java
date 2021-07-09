package com.taian.autonet.download.base;


import com.taian.autonet.download.DownloadInfo;

public interface IDownload {
    void startDownload();

    void pauseDownload(String url);

    void cancelDownload(DownloadInfo downloadInfo);
}
