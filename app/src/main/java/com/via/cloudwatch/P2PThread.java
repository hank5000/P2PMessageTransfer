package com.via.cloudwatch;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.utils.DefaultSetting;
import com.utils.P2PCommunicationChannel;
import com.utils.QueryToServer;
import com.utils.SendingLiveViewThread;
import com.utils.SendingLocalVideoThread;
import com.via.libnice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.concurrent.Semaphore;

/**
 * Created by HankWu_Office on 2015/11/26.
 */
public class P2PThread extends Thread {
    libnice mNice;
    int mStreamId;
    P2PCommunicationChannel msgChannel = null;
    String TAG = "P2PThread";
    String remoteSdp = "";
    String localSdp = "";
    Semaphore semph = new Semaphore(1);
    Context application_ctx = null;
    SendingLocalVideoThread[] sendingThreads = new SendingLocalVideoThread[4];
    SendingLiveViewThread[] sendingLiveThreads = new SendingLiveViewThread[4];
    P2PThread instance = this;
    public int cameraNumber = 0;
    public String[] cameraUrl = null;
    public String[] cameraNick = null;
    private Socket mSocket;

    public void setCameraUrlAndNick(int number,String[] urls,String[] nicks) {
        cameraNumber = number;
        cameraUrl = urls;
        cameraNick = nicks;
    }

    public P2PThread() {
        mNice = new libnice();
        mNice.init();
        mNice.createAgent(DefaultSetting.isReliableMode());
        mNice.setStunAddress(DefaultSetting.stunServerIp, DefaultSetting.stunServerPort);
        mNice.setControllingMode(0);
        mStreamId = mNice.addStream(DefaultSetting.streamName, 5);

        // If is the service mode, then only need to register 1 component which use for communicating
        int forComponentIndex = 1;
        msgChannel = new P2PCommunicationChannel(instance, mNice, mStreamId, forComponentIndex);
        mNice.registerReceiveCallback(msgChannel, mStreamId, forComponentIndex);

        mNice.registerStateObserver(new libnice.StateObserver() {
            @Override
            public void cbCandidateGatheringDone(final int i) {

            }

            @Override
            public void cbComponentStateChanged(final int i, final int i1, final int i2) {
                if (libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready") || libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("failed")) {
                    showToast("D", "Stream[" + i + "]Component[" + i1 + "]:" + libnice.StateObserver.STATE_TABLE[i2]);
                }
            }
        });

        localSdp = mNice.getLocalSdp(mStreamId);
    }

    Runnable runTask = new Runnable() {
        @Override
        public void run() {
            for(int i=0;i<4;i++) {
                stopSendingThread(mStreamId, i+1);
            }
            mNice.restartStream(mStreamId);
            localSdp = mNice.getLocalSdp(mStreamId);

            mSocket.emit("set local sdp", localSdp);
        }
    };


    @Override
    public void run() {
        super.run();

        try {
            mSocket = IO.socket(DefaultSetting.serverUrl);
            mSocket.on("response", onResponse);
            mSocket.on("get sdp", onGetSdp);
            mSocket.on("restart stream", onRestartStream);
            mSocket.connect();
            mSocket.emit("add user", DefaultSetting.sourcePeerUsername);
            mSocket.emit("set local sdp",localSdp);
        } catch (URISyntaxException e) {
            showToast("I","Server is offline, please contact your application provider!");
            throw new RuntimeException(e);
        }
    }


    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String message;
            try {
                message = data.getString("message");
                showToast("D","onRespone:" + message);

            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onGetSdp = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            JSONObject data = (JSONObject) args[0];
            String SDP;
            try {
                remoteSdp = data.getString("SDP");
                mNice.setRemoteSdp(remoteSdp);
                showToast("D","GetSDP:" + remoteSdp);
            } catch (JSONException e) {
                return;
            }

        }
    };

    private Emitter.Listener onRestartStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            showToast("I","on Restart Stream");
            // stop all sending thread
            for(int i=0;i<4;i++) {
                stopSendingThread(mStreamId, i+1);
            }
            mNice.restartStream(mStreamId);
            localSdp = mNice.getLocalSdp(mStreamId);
            mSocket.emit("set local sdp", localSdp);
        }
    };

    void setContext(Context app_ctx) {
        application_ctx = app_ctx;
    }

    public void showToast(String level,final String tmp) {

        if(level.equalsIgnoreCase("D") && DefaultSetting.printLevelD) {
            Log.d(DefaultSetting.WTAG+"/"+TAG, tmp);
            if(application_ctx!=null)
                Toast.makeText(application_ctx, tmp, Toast.LENGTH_SHORT).show();
        }

        if(level.equalsIgnoreCase("I") && DefaultSetting.printLevelI) {
            Log.d(DefaultSetting.WTAG+"/"+TAG, tmp);
            if(application_ctx!=null)
                Toast.makeText(application_ctx, tmp, Toast.LENGTH_SHORT).show();
        }

    }



    public void createLocalVideoSendingThread(int Stream_id, int onChannel, String path) {
        showToast("D","create Local Video Sending Thread");

        if (sendingThreads[onChannel - 1] == null) {
            showToast("D","create Sending Thread");
            Log.d("hank", "Create sending Thread");
            sendingThreads[onChannel - 1] = new SendingLocalVideoThread(mNice, Stream_id, onChannel, path);
            sendingThreads[onChannel - 1].start();
        }
    }

    public void createLiveViewSendingThread(int Stream_id, int onChannel, String ip) {
        if (sendingLiveThreads[onChannel - 1] == null) {
            showToast("D","create live view thread");
            sendingLiveThreads[onChannel - 1] = new SendingLiveViewThread(mNice, Stream_id, onChannel, ip);
            sendingLiveThreads[onChannel - 1].start();
        }
    }

    public void stopSendingThread(int Stream_id, int onChannel) {
        if (sendingThreads[onChannel - 1] != null) {
            sendingThreads[onChannel - 1].setStop();
            sendingThreads[onChannel - 1].interrupt();
            try {
                sendingThreads[onChannel - 1].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendingThreads[onChannel - 1] = null;
            showToast("D","Stop sending Thread " + onChannel);

        }

        if (sendingLiveThreads[onChannel - 1] != null) {
            sendingLiveThreads[onChannel - 1].setStop();
            sendingLiveThreads[onChannel - 1].interrupt();
            try {
                sendingLiveThreads[onChannel - 1].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendingLiveThreads[onChannel - 1] = null;
            showToast("D","Stop sending Thread " + onChannel);
        }
    }
}
