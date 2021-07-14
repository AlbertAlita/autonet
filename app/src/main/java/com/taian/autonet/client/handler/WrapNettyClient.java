package com.taian.autonet.client.handler;

import android.text.TextUtils;

import com.google.protobuf.GeneratedMessageV3;
import com.taian.autonet.AppApplication;
import com.taian.autonet.R;
import com.taian.autonet.SplashActivity;
import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.net.Net;
import com.video.netty.protobuf.CommandDataInfo;

import java.util.HashMap;
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

    public void resetIp() {
        mNettyTcpClient.setHost(AppApplication.getIP());
        mNettyTcpClient.setTcp_port(AppApplication.getPort());
    }

    private WrapNettyClient() {
        String ip = AppApplication.getIP();
        int port = AppApplication.getPort();
        mNettyTcpClient = new NettyTcpClient.Builder()
                .setHost(TextUtils.isEmpty(ip) ? Net.URL : ip)    //设置服务端地址
                .setTcpPort(port == -1 ? Net.port : port) //设置服务端端口号
                .setMaxReconnectTimes(Net.MAX_CONNECT_TIMES)    //设置最大重连次数
                .setReconnectIntervalTime(Net.RECONNECT_INTERVAL_TIME)    //设置重连间隔时间。单位：秒
                .setSendheartBeat(true) //设置是否发送心跳
                .setHeartBeatInterval(Net.HEARTBEAT_INTERVAL)    //设置心跳间隔时间。单位：秒
                .build();

        mNettyTcpClient.setListener(new NettyClientListener<CommandDataInfo.CommandDataInfoMessage>() {
            @Override
            public void onMessageResponseClient(CommandDataInfo.CommandDataInfoMessage message, int index) {
                for (Map.Entry<String, NettyClientListener> listenerEntry : nettyClientListeners.entrySet()) {
                    listenerEntry.getValue().onMessageResponseClient(message, index);
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

    public void disConnect() {
        if (mNettyTcpClient != null)
            mNettyTcpClient.disconnect();
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

    //收到数据给服务器反馈
    public void responseServer(int code) {
        CommandDataInfo.ResponseCommand response = CommandDataInfo.ResponseCommand.newBuilder().
                setResponseCode(code).
                build();
        WrapNettyClient.getInstance().sendMsgToServer(response);
    }
}
