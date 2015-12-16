package com.utils;

/**
 * Created by HankWu_Office on 2015/12/3.
 */
public class DefaultSetting {
    final static int reliableMode = 0;
    final static public String WTAG = "VIA-CloudWatch/W";
    // register server / signaling server
    public final static String serverUrl = "http://122.147.15.216:3000/";
    // stun server
    public final static String stunServerIp = "74.125.204.127";
    public final static int stunServerPort = 19302;
    //
    public final static String sourcePeerUsername = "DefaultUsername";
    public final static String sourcePassword     = "DefaultPassWord";
    public final static String findPeerUsername   = sourcePeerUsername;
    public final static String findPeerPassword   = sourcePassword;

    public final static String streamName = "DefaultStream";


    public final static boolean printLevelD = false;
    public final static boolean printLevelI = true;

    public final static boolean bShowConnectMenu = false;

    public static int isReliableMode() {
        return reliableMode;
    }


}
