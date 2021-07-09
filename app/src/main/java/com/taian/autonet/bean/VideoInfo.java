package com.taian.autonet.bean;


import com.video.netty.protobuf.CommandDataInfo;

public class VideoInfo {

    private long id;
    private String videoName;
    private String videoPath;
    private int videoNumber;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getVideoNumber() {
        return videoNumber;
    }

    public void setVideoNumber(int videoNumber) {
        this.videoNumber = videoNumber;
    }

    public static VideoInfo toVideoInfo(CommandDataInfo.VideoInfo info) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.id = info.getId();
        videoInfo.videoName = info.getVideoName();
        videoInfo.videoPath = info.getVideoPath();
        videoInfo.videoNumber = info.getVideoNumber();
        return videoInfo;
    }
}
