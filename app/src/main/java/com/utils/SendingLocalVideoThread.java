package com.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.via.libnice;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by HankWu_Office on 2015/11/18.
 */
public class SendingLocalVideoThread extends Thread {
    libnice mAgent = null;
    int mStreamId = -1;
    int mCompId = -1;
    String path = "";
    int DEFAULT_DIVIDED_SIZE = 1024 * 1024;
    ByteBuffer naluBuffer = ByteBuffer.allocateDirect(1024 * 1024);
    boolean bStop = false;

    public SendingLocalVideoThread(libnice nice, int stream_id, int component_id, String p) {
        mAgent = nice;
        mStreamId = stream_id;
        mCompId = component_id + 1;
        path = p;
        bStop = false;
    }

    void sendVideoData(ByteBuffer bb, int length) {
        mAgent.sendDataDirect(bb, length, mStreamId, mCompId);
    }

    void sendVideoMsg(String msg) {
        mAgent.sendMsg(msg, mStreamId, mCompId);
    }

    public void setStop() {
        bStop = true;
    }

    @Override
    public void run() {
        initMediaExtractor(path);

        while (!bStop) {
            // TODO Auto-generated method stub
            int naluSize = me.readSampleData(naluBuffer, 0);

            int divideSize = DEFAULT_DIVIDED_SIZE;
            int sentSize = 0;

            if (naluSize > 0) {

                sendVideoData(naluBuffer, naluSize);

                me.advance();

                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                me.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            }
        }
    }

    MediaExtractor me = new MediaExtractor();

    void initMediaExtractor(String path) {
        try {
            me.setDataSource(path);
            MediaFormat mf = null;
            String mime = null;
            String videoMsg = "Video";
            int w = 0;
            int h = 0;
            String s_sps = null;
            String s_pps = null;

            for (int i = 0; i < me.getTrackCount(); i++) {
                mf = me.getTrackFormat(i);
                mime = mf.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith("video")) {
                    me.selectTrack(i);
                    mime = mf.getString(MediaFormat.KEY_MIME);

                    w = mf.getInteger(MediaFormat.KEY_WIDTH);
                    h = mf.getInteger(MediaFormat.KEY_HEIGHT);

                    ByteBuffer sps_b = mf.getByteBuffer("csd-0");
                    byte[] sps_ba = new byte[sps_b.remaining()];
                    sps_b.get(sps_ba);
                    s_sps = bytesToHex(sps_ba);

                    mf.getByteBuffer("csd-1");
                    ByteBuffer pps_b = mf.getByteBuffer("csd-1");
                    byte[] pps_ba = new byte[pps_b.remaining()];
                    pps_b.get(pps_ba);
                    s_pps = bytesToHex(pps_ba);

                    videoMsg = videoMsg + ":" + mime + ":" + w + ":" + h + ":" + s_sps + ":" + s_pps + ":"+ path + ":";

                    sendVideoMsg(videoMsg);

                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
