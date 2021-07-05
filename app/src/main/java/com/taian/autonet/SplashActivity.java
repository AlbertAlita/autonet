package com.taian.autonet;


import android.os.Bundle;
import android.util.Log;


import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.listener.NettyClientListener;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/*
 Created by baotaian on 2021/7/5 0005.
*/
public class SplashActivity extends AppCompatActivity {

    private NettyTcpClient mNettyTcpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        mNettyTcpClient = new NettyTcpClient.Builder()
                .setHost("free.idcfengye.com")    //设置服务端地址
                .setTcpPort(10003) //设置服务端端口号
                .setMaxReconnectTimes(5)    //设置最大重连次数
                .setReconnectIntervalTime(5)    //设置重连间隔时间。单位：秒
                .setSendheartBeat(true) //设置是否发送心跳
                .setHeartBeatInterval(30)    //设置心跳间隔时间。单位：秒
                .setHeartBeatData("I'm is HeartBeatData") //设置心跳数据，心跳数据在后面写死了发送数据
                .setIndex(0)    //设置客户端标识.(因为可能存在多个tcp连接)
//                .setPacketSeparator("#")//用特殊字符，作为分隔符，解决粘包问题，默认是用换行符作为分隔符
//                .setMaxPacketLong(1024)//设置一次发送数据的最大长度，默认是1024
                .build();

        mNettyTcpClient.connect();//连接服务器

        CommandDataInfo.TokenCommand tokenCommand = CommandDataInfo.TokenCommand.newBuilder().setToken("abcd").build();
        CommandDataInfo.CommandDataInfoMessage command = CommandDataInfo.CommandDataInfoMessage.newBuilder().
                setDataType(CommandDataInfo.CommandDataInfoMessage.CommandType.TokenType)
                .setTokenCommand(tokenCommand).build();
        boolean b = mNettyTcpClient.sendMsgToServer(command);
        Log.e("TAG",b +"");

        mNettyTcpClient.setListener(new NettyClientListener() {
            @Override
            public void onMessageResponseClient(Object msg, int index) {
                Log.e("TAG",msg.toString());
            }

            @Override
            public void onClientStatusConnectChanged(int statusCode, int index) {

            }
        }); //设置TCP监听
    }

}
