syntax = "proto2";
package com.video.netty.protobuf;
option optimize_for = SPEED;
option java_package = "com.video.netty.protobuf";
option java_outer_classname = "CommandDataInfo";
message CommandDataInfoMessage{
    enum CommandType{
        VoiceType = 1;  //声音控制命令
        BrakeType = 2;  // 开机或关机命令
        BrakeTimingType = 3;    //定时开关机时间命令
        ApkVersionType = 4; //APK版本信息
        ProgramType = 5; //节目单命令
        TokenType = 6; //身份验证
        HeartbeatType = 7; //心跳
        ResponseType = 8; //服务器响应
    }
    required CommandType data_type = 1; //必须要有
    oneof dataBody{
        VoiceCommand voiceCommand = 2; //从2开始，1被上面的data_type用了
        BrakeCommand brakeCommand = 3;
        BrakeTimingCommand brakeTimingCommand = 4;
        ApkVersionCommand apkVersionCommand = 5;
        ProgramCommand programCommand = 6;
        TokenCommand tokenCommand = 7;
        HeartbeatCommand heartbeatCommand = 8;
        ResponseCommand responseCommand = 9;
    }
}
message VoiceCommand {
    required int32 voiceValue = 1;  //声音值0-100
}
message BrakeCommand{
    required int32 brakeValue = 1; //开机=1  关机=0
}
message BrakeTimingCommand{
    required string openBrake = 1;  //定时开机时间
    required string closeBrake = 2;  //定时关机时间
}
message ApkVersionCommand{
    required int32 version = 1; //APK版本号
    required string filePath = 2; //APK下载地址
}
message VideoInfo{
    required int64 id = 1; //视频ID
    required string videoName = 2; //视频名称
    required string videoPath = 3; //视频下载地址，有效期1小时
    required int32 videoNumber = 4; //视频播放序号
}
message ProgramCommand{
    required int64 id = 1; //节目单ID
    required string programName = 2; //节目单名称
    repeated VideoInfo videoInfo = 3; //节目单里视频
}
message TokenCommand{
    required string token = 1; //客户端mac地址
}
message HeartbeatCommand{
    required string token = 1; //客户端mac地址
}
message ResponseCommand{
    required int32 responseCode = 1; //服务器响应代码 200成功
}