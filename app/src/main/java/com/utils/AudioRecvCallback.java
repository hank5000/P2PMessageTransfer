package com.utils;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.SurfaceView;

import com.via.cloudwatch.MainActivity;
import com.via.libnice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.Random;

public class AudioRecvCallback implements libnice.ReceiveCallback {

    boolean bAudio = false;

    LocalServerSocket mLss = null;
    LocalSocket mReceiver = null;
    LocalSocket mSender = null;
    int mSocketId;
    final String LOCAL_ADDR = "DataChannelToAudioDecodeThread-";
    public OutputStream os = null;
    public WritableByteChannel writableByteChannel;
    public InputStream is = null;
    final static String TAG = "AudioRecvCallback";
    String remote_address = "";
    MainActivity act = null;
    int index = -1;
    NormalAudioThread nat = null;

    public AudioRecvCallback(SurfaceView sv, MainActivity a, int i) {
        act = a;
        index = i;
    }

    private void LOGD(String msg) {
        Log.d(TAG, msg);
    }

    public boolean isStart() {
        return bAudio;
    }

    public void setStop() {
        bAudio = false;

        if (nat != null) {
            nat.setStop();
            nat.interrupt();
            try {
                nat.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nat = null;
        }
    }

    @Override
    public void onMessage(byte[] msg) {

        if (!bAudio) {
            String tmp = new String(msg);
            // Audio:AAC:
            if (tmp.startsWith("Audio")) {

                bAudio = true;

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
                nat = new NormalAudioThread(is,tmp);
                nat.start();
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