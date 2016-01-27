package com.utils;

import android.util.Log;
import android.widget.Toast;

import com.via.cloudwatch.MainActivity;
import com.via.libnice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HankWu_Office on 2015/11/18.
 */
public class CommunicationChannel implements libnice.ReceiveCallback {
    private String log = "";
    final static int MAX_LOG_SIZE = 30;
    MainActivity activity = null;
    libnice mNice = null;
    int mStreamId = 0;
    int mComponentId = 0;

    public final static String REQUEST_LIVE_VIEW_INFO = "REQUEST_LIVE_VIEW_INFO";
    public final static String FEEDBACK_LIVE_VIEW_INFO = "FEEDBACK_LIVE_VIEW_INFO";

    public CommunicationChannel(MainActivity act, libnice nice, int stream_id, int comp_id) {
        activity = act;
        mNice = nice;
        mStreamId = stream_id;
        mComponentId = comp_id;
    }

    public void sendMessage(String msg) {
        mNice.sendData(msg.getBytes(), msg.getBytes().length, mStreamId, mComponentId);
    }

    public void onMessage(byte[] input) {

        String inputMsg = new String(input);
        Log.d("CommunicateChannel", inputMsg);

        // logging the inputMsg
        log += inputMsg + "\n";

        if (inputMsg.startsWith("VIDEO")) {
            String[] msgs = inputMsg.split(":");
            int onChannel = -1;
            if (msgs[1].equals("RUN")) {
                onChannel = Integer.valueOf(msgs[2]);
                String path = msgs[3];

                activity.createLocalVideoSendingThread(mStreamId, onChannel, path);
            } else if (msgs[1].equals("STOP")) {
                onChannel = Integer.valueOf(msgs[2]);
                activity.stopSendingThread(mStreamId, onChannel);
            }
        }

        if (inputMsg.startsWith("LiveView")) {
            String[] msgs = inputMsg.split(":");
            int onChannel = -1;
            if (msgs[1].equals("RUN")) {
                onChannel = Integer.valueOf(msgs[3]);
                String nickName = msgs[2];

                String url = getUrlByUsingNickname(nickName);
                activity.createLiveViewSendingThread(mStreamId, onChannel, url);

            } else if (msgs[1].equals("STOP")) {

                onChannel = Integer.valueOf(msgs[2]);
                activity.stopSendingThread(mStreamId, onChannel);

            }
        }

        if (inputMsg.startsWith(REQUEST_LIVE_VIEW_INFO)) {

            //Test use only, the main function is implemented in P2PCommuncationChannel, that is
            //work in Service.
            sendMessage(FEEDBACK_LIVE_VIEW_INFO + ":" + "OV112:OV114:OV121:");
        }

        if (inputMsg.startsWith(FEEDBACK_LIVE_VIEW_INFO)) {
            //TODO: Store the feedback information of live view
            String[] inputMsgSplits = inputMsg.split(":");
            activity.liveViewList.clear();
            for (int i = 1; i < inputMsgSplits.length; i++) {
                // FEEDBACK_LIVE_VIEW_INFO => i=0
                // Camera01 => i=1
                // Camera02 => i=2
                // etc.
                // ......
                LiveViewInfo liveView = new LiveViewInfo(inputMsgSplits[i], "IDLE");
                activity.liveViewList.add(liveView);
            }
            activity.showToast("D",inputMsg);

        }
    }


    String getUrlByUsingNickname(String nickname) {
        String url = "";
        if(nickname.equals("OV121")) {
            url = "ov://192.168.12.121:1000";
        }

        if(nickname.equals("OV112")) {
            url = "ov://192.168.12.112:1000";
        }

        if(nickname.equals("OV114")) {
            url = "ov://192.168.12.114:1000";
        }

        if(nickname.equals("RTSP202")) {
            url = "rtsp://192.168.12.202:554/rtpvideo1.sdp";
        }

        return url;
    }

}
