package com.taian.autonet.download;

public class DownloadInfo {

    /**
     * 下载状态
     */
    public static final String DOWNLOAD = "download";    // 下载中
    public static final String DOWNLOAD_PAUSE = "pause"; // 下载暂停
    public static final String DOWNLOAD_WAIT = "wait";  // 等待下载
    public static final String DOWNLOAD_CANCEL = "cancel"; // 下载取消
    public static final String DOWNLOAD_OVER = "over";    // 下载结束
    public static final String DOWNLOAD_ERROR = "error";  // 下载出错
    public static final String DOWNLOAD_BEYOND_INDEX = "beyond_index";  // 超出数组范围


    public static final long TOTAL_ERROR = -1;//获取进度失败

    private String url;
    private String fileName;
    private String downloadStatus;
    private long total;
    private long progress;
    private int fileType;
    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DownloadInfo(String url) {
        this.url = url;
    }

    public DownloadInfo(String url, String downloadStatus) {
        this.url = url;
        this.downloadStatus = downloadStatus;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public String getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(String downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
}
