package com.taian.autonet.client.handler;
/*
 Created by baotaian on 2021/7/6 0006.
*/


import android.location.Address;
import android.os.Looper;

import com.google.protobuf.GeneratedMessageV3;
import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.video.netty.protobuf.CommandDataInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class WrapNettyClient {

    private static WrapNettyClient instance;
    private NettyTcpClient mNettyTcpClient;
    private Map<String, NettyClientListener> nettyClientListeners = new HashMap<>();

    public static WrapNettyClient getInstance() {
        if (instance == null) {
            synchronized (WrapNettyClient.class) {
                if (instance == null) {
                    instance = new WrapNettyClient();
                }
            }
            return instance;
        }
        return instance;
    }

    private WrapNettyClient() {
        mNettyTcpClient = new NettyTcpClient.Builder()
                .setHost(Net.URL)    //设置服务端地址
                .setTcpPort(Net.port) //设置服务端端口号
                .setMaxReconnectTimes(Net.MAX_CONNECT_TIMES)    //设置最大重连次数
                .setReconnectIntervalTime(Net.RECONNECT_INTERVAL_TIME)    //设置重连间隔时间。单位：秒
                .setSendheartBeat(true) //设置是否发送心跳
                .setHeartBeatInterval(Net.HEARTBEAT_INTERVAL)    //设置心跳间隔时间。单位：秒
                .build();

        mNettyTcpClient.setListener(new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
            @Override
            public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage msg, int index) {
                for (Map.Entry<String, NettyClientListener> listenerEntry : nettyClientListeners.entrySet()) {
                    listenerEntry.getValue().onMessageResponseClient(msg, index);
                }
            }

            @Override
            public void onClientStatusConnectChanged(int statusCode, int index) {
                for (Map.Entry<String, NettyClientListener> listenerEntry : nettyClientListeners.entrySet()) {
                    listenerEntry.getValue().onClientStatusConnectChanged(statusCode, index);
                }
            }
        });
    }

    public void connect() {
        if (mNettyTcpClient != null)
            mNettyTcpClient.connect();
    }

    public void addNettyClientListener(String tag, NettyClientListener listener) {
        nettyClientListeners.put(tag, listener);
    }

    public void removeListener(String tag) {
        nettyClientListeners.remove(tag);
    }

    public void sendMsgToServer(GeneratedMessageV3 command) {
        if (mNettyTcpClient != null)
            mNettyTcpClient.sendMsgToServer(command);
    }
}
