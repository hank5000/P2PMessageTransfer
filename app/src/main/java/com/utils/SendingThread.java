package com.utils;

import com.via.libnice;

import java.nio.ByteBuffer;

/**
 * Created by HankWu_Office on 2015/11/24.
 */
abstract public class SendingThread extends Thread {
    private libnice mAgent = null;
    private int mStreamId = -1;
    private int mCompId = -1;
    ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
    boolean bStart = true;

    public void stopThread() {
        bStart = false;
    }

    public SendingThread(libnice nice, int stream_id, int component_id) {
        mAgent = nice;
        mStreamId = stream_id;
        mCompId = component_id;
    }

    private void sendData(ByteBuffer bb, int length) {
        mAgent.sendDataDirect(bb, length, mStreamId, mCompId);
    }

    private void sendMsg(String msg) {
        mAgent.sendMsg(msg, mStreamId, mCompId);
    }
}
