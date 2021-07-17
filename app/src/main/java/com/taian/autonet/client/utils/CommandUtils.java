package com.taian.autonet.client.utils;

import android.content.Context;
import android.content.Intent;

import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.net.Net;

public class CommandUtils {

    public static boolean powerOnOffByAlarm(Context context, String openBrake, String closeBrake) {
        boolean isSuccess = false;
        Intent intent = new Intent(Constants.POWER_ON_OFF_BY_ALARM);
        try {
            String[] onTime = openBrake.split(" ");
            String[] yymmdd = onTime[0].split("-");
            String[] hhss = onTime[1].split(":");
            int[] on = {Integer.parseInt(yymmdd[0]),
                    Integer.parseInt(yymmdd[1]),
                    Integer.parseInt(yymmdd[2]),
                    Integer.parseInt(hhss[0]),
                    Integer.parseInt(hhss[1])};
            String[] offTime = closeBrake.split(" ");
            String[] offyymmdd = offTime[0].split("-");
            String[] offhhss = offTime[1].split(":");
            int[] off = {Integer.parseInt(offyymmdd[0]),
                    Integer.parseInt(offyymmdd[1]),
                    Integer.parseInt(offyymmdd[2]),
                    Integer.parseInt(offhhss[0]),
                    Integer.parseInt(offhhss[1])};
            intent.putExtra(Constants.TIME_ON, on);
            intent.putExtra(Constants.TIME_OFF, off);
            intent.putExtra("enable", true);
            context.sendBroadcast(intent);
            isSuccess = true;
        } catch (Exception e) {
            WrapNettyClient.getInstance().responseServer(Net.BRAKE_TIME_ERROR);
        }
        return isSuccess;
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
            WrapNettyClient.getInstance().responseServer(Net.UPDATE_VOLUME_ERROR);
        }
        if (!hasError)
            WrapNettyClient.getInstance().responseServer(Net.UPDATE_VOLUME);
    }
}
