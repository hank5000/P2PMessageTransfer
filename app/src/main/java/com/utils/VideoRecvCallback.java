package com.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Random;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.SurfaceView;

import com.via.libnice;

public class VideoRecvCallback implements libnice.ReceiveCallback {

    boolean bVideo = false;
    int w = 0;
    int h = 0;
    String sps = null;
    String pps = null;
    String mime = null;

    LocalServerSocket mLss = null;
    LocalSocket mReceiver = null;
    LocalSocket mSender = null;
    int mSocketId;
    final String LOCAL_ADDR = "DataChannelToVideoDecodeThread-";
    public OutputStream os = null;
    public WritableByteChannel writableByteChannel;
    public InputStream is = null;
    VideoThread vt = null;
    SurfaceView videosv = null;
    final static String TAG = "VideoRecvCallback";
    boolean bRender = true;
    String remote_address = "";

    public void setRender(boolean b) {
        if(vt!=null) {
            vt.setRender(b);
        }
    }

    public VideoRecvCallback(SurfaceView sv) {
        videosv = sv;
    }

    private void LOGD(String msg) {
        Log.d(TAG, msg);
    }

    public boolean isStart() {
        return bVideo;
    }

    public void setStop() {
        bVideo = false;

        if (vt != null) {
            vt.setStop();
            vt.interrupt();
            try {
                vt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            vt = null;
        }
    }

    @Override
    public void onMessage(byte[] msg) {

        if (!bVideo) {
            String tmp = new String(msg);
            if (tmp.startsWith("Video")) {

                bVideo = true;
                String[] tmps = tmp.split(":");
                mime = tmps[1];
                w = Integer.valueOf(tmps[2]);
                h = Integer.valueOf(tmps[3]);
                sps = tmps[4];
                pps = tmps[5];
                remote_address = tmps[6];

                for (int jj = 0; jj < 10; jj++) {
                    try {
                        mSocketId = new Random().nextInt();
                        mLss = new LocalServerSocket(LOCAL_ADDR + mSocketId);
                        break;
                    } catch (IOException e) {
                        LOGD("fail to create localserversocket :" + e);
                    }
                }
                //    DECODE FLOW
                //
                //    Intermediary:                             Localsocket       MediaCodec inputBuffer     MediaCodec outputBuffer
                //        Flow    : Data Channel =======> Sender ========> Receiver ==================> Decoder =================> Display to surface/ Play by Audio Track
                //       Thread   : |<---Data Channel thread--->|          |<--------- Decode Thread --------->|                 |<--------- Display/play Thread -------->|
                //
                mReceiver = new LocalSocket();
                try {
                    mReceiver.connect(new LocalSocketAddress(LOCAL_ADDR + mSocketId));
                    mReceiver.setReceiveBufferSize(100000);
                    mReceiver.setSoTimeout(2000);
                    mSender = mLss.accept();
                    mSender.setSendBufferSize(100000);
                } catch (IOException e) {
                    LOGD("fail to create mSender mReceiver :" + e);
                    e.printStackTrace();
                }
                try {
                    os = mSender.getOutputStream();
                    is = mReceiver.getInputStream();
                } catch (IOException e) {
                    LOGD("fail to get mSender mReceiver :" + e);
                    e.printStackTrace();
                }
                LOGD("Ready to play ("+mime+"), resolution "+w+"x"+"h");
                vt = new VideoThread(videosv.getHolder().getSurface(), mime, w, h, sps, pps, is,remote_address);
                vt.start();
            }

        } else {

            try {
                os.write(msg);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGD("os write fail" + e);
            }
        }
    }

}