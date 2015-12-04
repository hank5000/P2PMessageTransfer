package com.via.cloudwatch;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.utils.DefaultSetting;
import com.utils.P2PCommunicationChannel;
import com.utils.QueryToServer;
import com.utils.SendingLiveViewThread;
import com.utils.SendingLocalVideoThread;
import com.via.libnice;

import java.io.UnsupportedEncodingException;
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

    public void setCameraUrlAndNick(int number,String[] urls,String[] nicks) {
        cameraNumber = number;
        cameraUrl = urls;
        cameraNick = nicks;
    }

    public P2PThread() {
        mNice = new libnice();
        mNice.init();
        mNice.createAgent(DefaultSetting.isReliableMode());
        mNice.setStunAddress("74.125.204.127", 19302);
        mNice.setControllingMode(0);
        mStreamId = mNice.addStream("HankWu", 5);

        // Component 1 is using for message transfer
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
                    showToast("Stream[" + i + "]Component[" + i1 + "]:" + libnice.StateObserver.STATE_TABLE[i2]);
                }
            }
        });

        localSdp = mNice.getLocalSdp(mStreamId);
    }

    Runnable runTask = new Runnable() {
        @Override
        public void run() {
            semph = null;
            semph = new Semaphore(1);

            mNice.restartStream(mStreamId);
            localSdp = mNice.getLocalSdp(mStreamId);

            final String method = "Server";
            try {
                final String postParams = "register=" + URLEncoder.encode("TRUE", "UTF-8")
                        + "&username=" + URLEncoder.encode("HankWu", "UTF-8")
                        + "&SDP=" + URLEncoder.encode(localSdp, "UTF-8");
                Log.d(TAG, "post method:" + method + ",postParams:" + postParams);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        QueryToServer.excutePost(method, postParams);
                    }
                }).start();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d(TAG, "something wrong");
            }

            try {
                semph.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        try {
                            String postParameters = "register=" + URLEncoder.encode("FALSE", "UTF-8")
                                    + "&username=" + URLEncoder.encode("HankWu", "UTF-8");
                            Log.d(TAG, "post method:" + method + ",postParams:" + postParameters);
                            remoteSdp = QueryToServer.excutePost(method, postParameters);
                            if (remoteSdp.startsWith("NOBODY")) {

                            } else {
                                semph.release();
                                break;
                            }

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            Log.d(TAG, "something wrong");
                        }
                    }
                }
            }).start();
            try {
                semph.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mNice.setRemoteSdp(remoteSdp);
            showToast("set Remote Sdp lo.");
        }
    };


    @Override
    public void run() {
        super.run();
        final String method = "Server";
        try {
            final String postParams = "register=" + URLEncoder.encode("TRUE", "UTF-8")
                    + "&username=" + URLEncoder.encode("HankWu", "UTF-8")
                    + "&SDP=" + URLEncoder.encode(localSdp, "UTF-8");
            Log.d(TAG, "post method:" + method + ",postParams:" + postParams);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    QueryToServer.excutePost(method, postParams);
                }
            }).start();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, "something wrong");
        }

        try {
            semph.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        String postParameters = "register=" + URLEncoder.encode("FALSE", "UTF-8")
                                + "&username=" + URLEncoder.encode("HankWu", "UTF-8");
                        Log.d(TAG, "post method:" + method + ",postParams:" + postParameters);
                        remoteSdp = QueryToServer.excutePost(method, postParameters);
                        if (remoteSdp.startsWith("NOBODY")) {

                        } else {
                            semph.release();
                            break;
                        }

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        Log.d(TAG, "something wrong");
                    }
                }
            }
        }).start();
        try {
            semph.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNice.setRemoteSdp(remoteSdp);
        showToast("set Remote Sdp lo.");

        semph.release();
    }

    void setContext(Context app_ctx) {
        application_ctx = app_ctx;
    }

    void showToast(String msg) {
        Log.d(TAG,msg);
        if (application_ctx!=null) {
            Toast.makeText(application_ctx,msg,Toast.LENGTH_SHORT).show();
        }
    }



    public void createLocalVideoSendingThread(int Stream_id, int onChannel, String path) {
        showToast("create Local Video Sending Thread");
        if (sendingThreads[onChannel - 1] == null) {
            showToast("create Sending Thread");
            Log.d("hank", "Create sending Thread");
            sendingThreads[onChannel - 1] = new SendingLocalVideoThread(mNice, Stream_id, onChannel, path);
            sendingThreads[onChannel - 1].start();
        }
    }

    public void createLiveViewSendingThread(int Stream_id, int onChannel, String ip) {
        if (sendingLiveThreads[onChannel - 1] == null) {
            showToast("create live view thread");
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
            showToast("Stop sending Thread " + onChannel);
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
            showToast("Stop sending Thread " + onChannel);
        }
    }
}
