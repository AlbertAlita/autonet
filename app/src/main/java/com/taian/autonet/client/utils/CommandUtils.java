package com.taian.autonet.client.utils;

import android.content.Context;
import android.content.Intent;

import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.net.Net;
import com.video.netty.protobuf.CommandDataInfo;

public class CommandUtils {

    public static boolean powerOnOffByAlarm(Context context, CommandDataInfo.BrakeTimingCommand brakeTimingCommand) {
        Intent intent = new Intent(Constants.POWER_ON_OFF_BY_ALARM);
        if (Utils.amPm() == Constants.AM) {
            int[] pmOpenTime = getFormatTime(brakeTimingCommand.getPmOpenBrake());
            int[] amCloseTime = getFormatTime(brakeTimingCommand.getAmCloseBrake());
            if (pmOpenTime == null || amCloseTime == null) {
                WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_ERROR);
                return false;
            }
            intent.putExtra(Constants.TIME_ON, pmOpenTime);
            intent.putExtra(Constants.TIME_OFF, amCloseTime);
            intent.putExtra("enable", true);
            context.sendBroadcast(intent);
        } else if (Utils.amPm() == Constants.PM) {
            int[] amOpenTime = getFormatTime(brakeTimingCommand.getAmOpenBrake());
            int[] pmCloseTime = getFormatTime(brakeTimingCommand.getPmCloseBrake());
            if (amOpenTime == null || pmCloseTime == null) {
                WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_ERROR);
                return false;
            }
            intent.putExtra(Constants.TIME_ON, amOpenTime);
            intent.putExtra(Constants.TIME_OFF, pmCloseTime);
            intent.putExtra("enable", true);
            context.sendBroadcast(intent);
        }
        return true;
    }

    public static void startOrShutDownDevice(Context context, int brakeValue) {
        if (brakeValue == 0) {
            Intent intent = new Intent(Constants.SHUT_DOWN);
            context.sendBroadcast(intent);
        } else if (brakeValue == 1) {
            //重启
            Intent intent = new Intent(Constants.RE_BOOT);
            context.sendBroadcast(intent);
        }
        if (brakeValue != 0 && brakeValue != 1)
            WrapNettyClient.getInstance().responseServer(Net.BRAKE_ERROR);
        else WrapNettyClient.getInstance().responseServer(Net.BRAKE_SUCCESS);
    }

    public static void updateVolume(Context context, int voiceValue) {
        boolean hasError = false;
        try {
            Intent intent = new Intent(Constants.UPDATE_VOLUME);
            intent.putExtra(Constants.VOLUME, voiceValue);  //声音值为0~100
            context.sendBroadcast(intent);
        } catch (Exception e) {
            hasError = true;
            WrapNettyClient.getInstance().responseServer(Net.UPDATE_VOLUME_ERROR);
        }
        if (!hasError)
            WrapNettyClient.getInstance().responseServer(Net.UPDATE_VOLUME);
    }

    public static int[] getFormatTime(String time) {
        int[] timeIntArray = null;
        try {
            String[] timeStringArray = time.split(" ");
            String[] yymmdd = timeStringArray[0].split("-");
            String[] hhss = timeStringArray[1].split(":");
            timeIntArray = new int[]{Integer.parseInt(yymmdd[0]),
                    Integer.parseInt(yymmdd[1]),
                    Integer.parseInt(yymmdd[2]),
                    Integer.parseInt(hhss[0]),
                    Integer.parseInt(hhss[1])};
        } catch (Exception e) {
            timeIntArray = null;
        }
        return timeIntArray;
    }
}
