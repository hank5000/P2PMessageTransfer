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
    libnice mNice=null;
    int mStreamId = 0;
    int mComponentId = 0;
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
        //Toast.makeText(activity,inputMsg,Toast.LENGTH_SHORT).show();

        // logging the inputMsg
        log += inputMsg + "\n";

        if (inputMsg.startsWith("VIDEO")) {
            String[] msgs = inputMsg.split(":");
            int onChannel = -1;
            if (msgs[1].equals("RUN")) {
                onChannel = Integer.valueOf(msgs[2]);
                activity.createSendingThread(mStreamId,onChannel);
            } else if (msgs[1].equals("STOP")) {
                onChannel = Integer.valueOf(msgs[2]);
                activity.stopSendingThread(mStreamId,onChannel);
            }
        }

    }




}
