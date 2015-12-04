package com.utils;

import android.util.Log;

import com.via.cloudwatch.ServerService;
import com.via.libnice;
import com.via.cloudwatch.P2PThread;

/**
 * Created by HankWu_Office on 2015/11/18.
 */
public class P2PCommunicationChannel implements libnice.ReceiveCallback {
    private String log = "";
    final static int MAX_LOG_SIZE = 30;
    P2PThread p2pthread = null;
    libnice mNice = null;
    int mStreamId = 0;
    int mComponentId = 0;
    final static String TAG = "nice CChannel";
    public final static String REQUEST_LIVE_VIEW_INFO = "REQUEST_LIVE_VIEW_INFO";
    public final static String FEEDBACK_LIVE_VIEW_INFO = "FEEDBACK_LIVE_VIEW_INFO";



    public P2PCommunicationChannel(P2PThread s, libnice nice, int stream_id, int comp_id) {
        p2pthread = s;
        mNice = nice;
        mStreamId = stream_id;
        mComponentId = comp_id;
    }

    public void sendMessage(String msg) {
        mNice.sendData(msg.getBytes(), msg.getBytes().length, mStreamId, mComponentId);
    }

    public void onMessage(byte[] input) {

        final String inputMsg = new String(input);
        Log.d(TAG,inputMsg);
        log += inputMsg + "\n";

        if (inputMsg.startsWith("REQUEST_LIVE_VIEW_INFO")) {
            // TODO: Send the information of live view to request side
            String ret = "";
            for (int i=0;i<p2pthread.cameraNumber; i++) {
                ret += (p2pthread.cameraNick[i] + ":");
            }
            sendMessage(FEEDBACK_LIVE_VIEW_INFO + ":" + ret);
        }

        if (inputMsg.startsWith("VIDEO")) {
            String[] msgs = inputMsg.split(":");
            int onChannel = -1;
            if (msgs[1].equals("RUN")) {
                onChannel = Integer.valueOf(msgs[2]);
                String path = "/mnt/sata/720.mp4";
                if(msgs.length>=3) {
                    path = "/mnt/sata/720.mp4";//msgs[3];
                }
                /*
                    test video path
                 */
                p2pthread.createLocalVideoSendingThread(mStreamId, onChannel, path);
            } else if (msgs[1].equals("STOP")) {
                onChannel = Integer.valueOf(msgs[2]);
                p2pthread.stopSendingThread(mStreamId, onChannel);
            }
        }
        Log.d(TAG,"Live View");
        if (inputMsg.startsWith("LiveView")) {
            String[] msgs = inputMsg.split(":");
            Log.d(TAG,"in Live View");

            int onChannel = -1;
            if (msgs[1].equals("RUN")) {
                onChannel = Integer.valueOf(msgs[3]);
                String nickName = msgs[2];

                //TODO: get url by using nickname
                String url = getUrlByUsingNickname(nickName);
                Log.d(TAG,"Get url:"+url+", onChannel:"+onChannel);
                p2pthread.createLiveViewSendingThread(mStreamId, onChannel, url);

            } else if (msgs[1].equals("STOP")) {

                onChannel = Integer.valueOf(msgs[2]);
                p2pthread.stopSendingThread(mStreamId, onChannel);

            }
        }
    }

    String getUrlByUsingNickname(String nickname) {
        for(int i=0;i<p2pthread.cameraNumber;i++) {
            if (p2pthread.cameraNick[i].equals(nickname)) {
                return p2pthread.cameraUrl[i];
            }
        }
        return "";
    }

}
