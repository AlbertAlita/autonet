package com.taian.autonet.client;

import com.taian.autonet.bean.VideoInfo;
import com.taian.autonet.client.constant.Constants;
import java.util.List;

public class DownloadDelegate {


    private List<VideoInfo> cachedVideoList;
    private int index = 1;

    public DownloadDelegate(List<VideoInfo> cachedVideoList) {
        this.cachedVideoList = cachedVideoList;
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
            e.printStackTrace();
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setCode(Constants.ERROR);
            videoInfo.setVideoName("找不到节目单");
            return videoInfo;
        }
    }

    public void updateIndex() {
        index += 1;
        if (index > cachedVideoList.size()) index = 1;
        //缺这个编号就跳到下一个
        if (getCurrentVideo() == null) {
            index += 1;
            if (index > cachedVideoList.size()) index = 1;
        }
    }

}
