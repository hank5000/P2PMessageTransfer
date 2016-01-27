package com.utils;

/**
 * Created by HankWu_Office on 2015/8/28.
 *
 * Only Can Support G711-alaw mulaw AAC Currently.
 *
 */
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class NormalAudioThread extends Thread {
    final private String TAG = "VIA-RTC NAThread";

    ByteBuffer bb_tmp = ByteBuffer.allocate(1024 * 1024 * 10);
    byte[] abuff_tmp = new byte[1024*1024];
    byte[] dst  = new byte[1024*1024];

    InputStream is = null;
    String codecType = null;
    int    numberOfChannel = 1;
    int    sampleRate = 0;

    AudioPlayThread apt = null;
    MediaFormat format = null;
    MediaCodec decoder = null;

    private void releaseAll() {
        if(apt!=null) {
            apt.interrupt();
            try {
                apt.join();
            } catch (InterruptedException e) {

            }
            apt=null;
        }

        decoder.release();
        decoder = null;
        format = null;

    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private boolean bStart = true;

    public void setStop() {
        bStart = false;
    }

    public NormalAudioThread(InputStream input, String param) {
        this.is = input;
        String[] params = param.split(":");
        String codec = params[1];
        format = new MediaFormat();
        numberOfChannel = 1;
        sampleRate = 8000;
        if(codec.equalsIgnoreCase("AAC")) {
            Log.d(TAG, "audio type : AAC");
            codecType = "audio/mp4a-latm";
            numberOfChannel = Integer.valueOf(params[2]);
            sampleRate = Integer.valueOf(params[3]);

            byte[] extraDataByte = hexStringToByteArray(params[4]);
            ByteBuffer extra_bb = ByteBuffer.wrap(extraDataByte);
            format.setByteBuffer("csd-0", extra_bb);
        } else if(codec.equalsIgnoreCase("mG711")) {
            codecType = "audio/g711-mlaw";
            numberOfChannel = Integer.valueOf(params[2]);
        } else if(codec.equalsIgnoreCase("aG711")) {
            codecType = "audio/g711-alaw";
            numberOfChannel = Integer.valueOf(params[2]);
        } else if(codec.equalsIgnoreCase("G726")) {
            // no support currently.
        }

        format.setString(MediaFormat.KEY_MIME, codecType);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT,numberOfChannel);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);

        try {
            decoder = MediaCodec.createDecoderByType(codecType);
            decoder.configure(format, null, null, 0);

            decoder.start();
        } catch (IOException e) {
            Log.e(TAG, "Create audio decoder (" + codecType + ") fail");
        }


    }

    public void run() {

        ByteBuffer[] inputBuffersAudio = null;
        ByteBuffer[] outputBuffersAudio= null;
        MediaCodec.BufferInfo infoAudio = null;
        apt = null;
        inputBuffersAudio = decoder.getInputBuffers();
        outputBuffersAudio= decoder.getOutputBuffers();
        infoAudio = new MediaCodec.BufferInfo();

        if (apt == null) {
            Log.d(TAG, "Create audio play thread");
            apt = new AudioPlayThread(decoder, outputBuffersAudio, format, infoAudio);
            apt.start();
        }

        while (!Thread.interrupted() && bStart) {

            int inIndex = -1;
            inIndex = decoder.dequeueInputBuffer(1000);
            if (inIndex >= 0) {
                int n = 0;
                int remaining = 0;
                while (!Thread.interrupted() && bStart) {
                    try {
                        n = is.read(abuff_tmp);
                    } catch (IOException e) {
                        //Log.e(TAG, "Audio read inputstream timeout");
                    }

                    bb_tmp.put(abuff_tmp, 0, n);

                    remaining = bb_tmp.position();
                    bb_tmp.flip();

                    int length = (bb_tmp.get(0) << 0) & 0x000000ff | (bb_tmp.get(1) << 8) & 0x0000ff00 | (bb_tmp.get(2) << 16) & 0x00ff0000 | (bb_tmp.get(3) << 24) & 0xff000000;
                    if ((length+4) < remaining && length > 0) {
                        bb_tmp.get();
                        bb_tmp.get();
                        bb_tmp.get();
                        bb_tmp.get();

                        bb_tmp.get(dst, 0, length);
                        bb_tmp.compact();

                        ByteBuffer buffer = inputBuffersAudio[inIndex];
                        buffer.clear();
                        buffer.put(dst, 0, length);
                        decoder.queueInputBuffer(inIndex, 0, length, 0, 0);
                        break;
                    } else {
                        bb_tmp.compact();
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        releaseAll();

    }

    public class AudioPlayThread extends Thread {
        MediaCodec decoder = null;
        MediaCodec.BufferInfo infoAudio = null;
        ByteBuffer[] outputBuffersAudio = null;
        String TAG = "VIA-RTC AudioPlay";
        MediaFormat format = null;
        AudioTrack audioTrack = null;

        public AudioPlayThread(MediaCodec codec, ByteBuffer[] bba, MediaFormat form, MediaCodec.BufferInfo info) {
            this.decoder = codec;
            this.outputBuffersAudio = bba;
            this.format = form;
            this.infoAudio = info;

            int minBufferSize = AudioTrack.getMinBufferSize(format.getInteger(MediaFormat.KEY_SAMPLE_RATE), AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = 4 * minBufferSize;
            this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, format.getInteger(MediaFormat.KEY_SAMPLE_RATE), AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

            this.audioTrack.play();
        }

        public void run() {
            while(bStart){
                int outIndex = -1;
                outIndex = this.decoder.dequeueOutputBuffer(this.infoAudio, 10000);
                switch (outIndex)
                {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        this.outputBuffersAudio = this.decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(TAG, "New format " + this.decoder.getOutputFormat());
                        format = this.decoder.getOutputFormat();
                        this.audioTrack.setPlaybackRate(this.format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        if(outIndex>=0 && this.decoder!= null)
                        {
                            ByteBuffer buffer = this.outputBuffersAudio[outIndex];
                            byte[] sampleData = new byte[this.infoAudio.size];
                            buffer.position(0);
                            buffer.get(sampleData);

                            buffer.clear();
                            if(sampleData.length>0)
                            {
                                this.audioTrack.write(sampleData,0,sampleData.length);
                            }
                            this.decoder.releaseOutputBuffer(outIndex, false);
                        }
                        break;
                }
            }
        }
    }


}