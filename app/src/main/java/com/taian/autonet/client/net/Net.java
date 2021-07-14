package com.taian.autonet.client.net;


public class Net {

    public static String URL = "175.27.250.67";
    public static int port = 10003;
    public static final int MAX_CONNECT_TIMES = 5;
    public static final int RECONNECT_INTERVAL_TIME = 5;
    public static final int HEARTBEAT_INTERVAL = 30;

    public static final int SUCCESS = 200;

    public static final int PROGRAM_ERROR = -101;
    public static final int BRAKE_ERROR = -102;

    public static final int PROGRAM_SUCCESS = 101;
    public static final int BRAKE_SUCCESS = 102;
    public static final int BRAKE_TIME_SUCCESS = 103;
    public static final int UPDATE_VOLUME = 104;
    public static final int UPDATE_VOLUME_ERROR = -104;

}
