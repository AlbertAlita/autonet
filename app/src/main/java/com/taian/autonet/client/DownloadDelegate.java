package com.taian.autonet.client;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

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

//    public List<VideoInfo> getCachedVideoList() {
//        return cachedVideoList == null ? new ArrayList<VideoInfo>() : cachedVideoList;
//    }
//
//    public DownloadEntity startDownloadTask(Activity activity) {
//        String url = getCurrentVideo().getVideoPath();
//        String fileName = getCurrentVideo().getVideoName();
//        List<DownloadEntity> downloadEntities = Aria.download(activity).getAllCompleteTask();
////        RecordUtil.delTaskRecord(downloadEntities.get(0).getFilePath(),downloadEntities.get(0).getTaskType(),true,true);
//        DownloadEntity entity = null;
//        if (downloadEntities != null && !downloadEntities.isEmpty()) {
//            for (DownloadEntity downloadEntity : downloadEntities) {
//                if (TextUtils.equals(downloadEntity.getFileName(), fileName) || TextUtils.equals(downloadEntity.getRealUrl(), url)) {
//                    entity = downloadEntity;
//                    break;
//                }
//            }
//            if (entity == null) {
//                Aria.download(activity).load(url).setFilePath(AppApplication.getDefaultRootPath()
//                        + getCurrentVideo().getVideoName()).ignoreFilePathOccupy().create();
//            }
//            return entity;
//        } else {
//            Aria.download(activity).load(url).setFilePath(AppApplication.getDefaultRootPath()
//                    + getCurrentVideo().getVideoName()).ignoreFilePathOccupy().create();
//            return null;
//        }
//    }

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

    public void reDownload(String url) {
//        tasks.get(url).execute(listeners.get(url));
    }
}
