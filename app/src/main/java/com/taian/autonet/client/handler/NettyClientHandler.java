package com.taian.autonet.client.handler;

import android.text.TextUtils;
import android.util.Log;


import com.taian.autonet.AppApplication;
import com.taian.autonet.client.NettyTcpClient;
import com.taian.autonet.client.constant.Constants;
import com.taian.autonet.client.listener.NettyClientListener;
import com.taian.autonet.client.status.ConnectState;
import com.taian.autonet.client.utils.Utils;
import com.video.netty.protobuf.CommandDataInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


public class NettyClientHandler extends SimpleChannelInboundHandler<CommandDataInfo.CommandDataInfoMessage> {

    private static final String TAG = "NettyClientHandler";
    private final boolean isSendheartBeat;
    private NettyClientListener listener;
    private int index;
    private Object heartBeatData;
    private String packetSeparator;

    //    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat"+System.getProperty("line.separator"),
//            CharsetUtil.UTF_8));
    byte[] requestBody = {(byte) 0xFE, (byte) 0xED, (byte) 0xFE, 5, 4, (byte) 0xFF, 0x0a};


    public NettyClientHandler(NettyClientListener listener, int index, boolean isSendheartBeat, Object heartBeatData) {
        this(listener, index, isSendheartBeat, heartBeatData, null);
    }

    public NettyClientHandler(NettyClientListener listener, int index, boolean isSendheartBeat, Object heartBeatData, String separator) {
        this.listener = listener;
        this.index = index;
        this.isSendheartBeat = isSendheartBeat;
        this.heartBeatData = heartBeatData;
        this.packetSeparator = TextUtils.isEmpty(separator) ? System.getProperty("line.separator") : separator;
    }

    /**
     * <p>设定IdleStateHandler心跳检测每x秒进行一次读检测，
     * 如果x秒内ChannelRead()方法未被调用则触发一次userEventTrigger()方法 </p>
     *
     * @param ctx ChannelHandlerContext
     * @param evt IdleStateEvent
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            File file = new File(Utils.initFolderPath(null, Constants.LOG_PATH),
                    "heartbeat" + ".log");
            String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(Calendar.getInstance().getTime());
            if (event.state() == IdleState.WRITER_IDLE) {   //发送心跳
                if (isSendheartBeat) {
                    Log.e("userEventTriggered", timeString + "已发送心跳==token=>" + AppApplication.getMacAdress());
                    Utils.TextToFile(file, timeString + "已发送心跳==token=>" + AppApplication.getMacAdress());
                    ctx.channel().writeAndFlush(CommandDataInfo.CommandDataInfoMessage.newBuilder().
                            setDataType(CommandDataInfo.CommandDataInfoMessage.CommandType.HeartbeatType).
                            setHeartbeatCommand(CommandDataInfo.HeartbeatCommand.newBuilder().setToken(AppApplication.getMacAdress())));
                } else {
                    Utils.TextToFile(file, timeString + "未发送心跳==token=>" + AppApplication.getMacAdress());
                    Log.e(TAG, "不发送心跳");
                }
            }
        }
    }

    /**
     * <p>客户端上线</p>
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        NettyTcpClient.getInstance().setConnectStatus(true);
        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Calendar.getInstance().getTime());
//        Log.e("channelActive", timeString + "已发送心跳==token=>" + AppApplication.getMacAdress());
        File file = new File(Utils.initFolderPath(null, Constants.LOG_PATH), "heartbeat" + ".log");
        Utils.TextToFile(file, timeString + "已发送心跳==token=>" + AppApplication.getMacAdress());
        ctx.channel().writeAndFlush(CommandDataInfo.CommandDataInfoMessage.newBuilder().
                setDataType(CommandDataInfo.CommandDataInfoMessage.CommandType.HeartbeatType).
                setHeartbeatCommand(CommandDataInfo.HeartbeatCommand.newBuilder().setToken(AppApplication.getMacAdress())));
        listener.onClientStatusConnectChanged(ConnectState.STATUS_CONNECT_SUCCESS, index);
    }

    /**
     * <p>客户端下线</p>
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Log.e(TAG, "channelInactive" + ctx.name());
//        NettyTcpClient.getInstance().setConnectStatus(false);
//        listener.onServiceStatusConnectChanged(NettyClientListener.STATUS_CONNECT_CLOSED);
        // NettyTcpClient.getInstance().reconnect();
        WrapNettyClient.getInstance().disConnect(null);
        WrapNettyClient.getInstance().connect(new Random().nextInt(3000));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }

    /**
     * 客户端收到消息
     *
     * @param channelHandlerContext ChannelHandlerContext
     * @param msg                   消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CommandDataInfo.CommandDataInfoMessage msg) {
        Log.e(getClass().getSimpleName(), "channelRead0" + msg.toString());
        listener.onMessageResponseClient(msg, index);
    }

    /**
     * @param ctx   ChannelHandlerContext
     * @param cause 异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
//        NettyTcpClient.getInstance().setConnectStatus(false);

        Log.e(TAG, "exceptionCaught" + cause.getLocalizedMessage());
        listener.onClientStatusConnectChanged(ConnectState.STATUS_CONNECT_ERROR, index);
        cause.printStackTrace();
        ctx.close();
        WrapNettyClient.getInstance().disConnect(null);
        WrapNettyClient.getInstance().connect(new Random().nextInt(5000));
    }
}
