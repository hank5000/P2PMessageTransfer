package com.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import com.via.libnice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by HankWu_Office on 2015/8/25.
 */
public class SendingLiveViewThread extends Thread {
    final static String TAG = "VIA-RTC SLVThread";

    boolean bStart = true;
    String ip_address = null;
    InputStream in = null;
    byte[] transferBuffer = new byte[1024 * 1024];
    ByteBuffer transByteBuffer = ByteBuffer.allocateDirect(1024 * 1024);
    // TODO: Currently using hardcode. Feature:  it need to send from OV Extractor
    final String OV_SPS = "00000001674d0028a9500a00b742000007d00001d4c008";
    final String OV_PPS = "0000000168ee3c8000";
    int DEFAULT_DIVIDED_SIZE = 1024 * 1024;
    public ReadableByteChannel readableByteChannel;
    int currentFPS = 0;
    long startTime = -1;

    libnice mAgent = null;
    int mStreamId = -1;
    int mCompId = -1;
    int bitRate = 0;

    public SendingLiveViewThread(libnice nice, int stream_id, int component_id, String ip) {
        mAgent = nice;
        mStreamId = stream_id;
        mCompId = component_id + 1;
        ip_address = ip;
    }

    void sendVideoData(ByteBuffer bb, int length) {
        mAgent.sendDataDirect(bb, length, mStreamId, mCompId);
    }

    void sendVideoMsg(String msg) {
        mAgent.sendMsg(msg, mStreamId, mCompId);
    }

    void sendVideoStartMsg(String mime,String w,String h,String sps,String pps,String ip_address) {
        String videoMsg = "Video"+":"+mime+":"+w+":"+h+":"+sps+":"+pps+":"+ip_address+":";
        sendVideoMsg(videoMsg);
    }

    public void setStop() {
        bStart = false;
    }

    @Override
    public void run() {
        // wait for Default setting
        try {
            this.sleep(1000);
        } catch (Exception e) {
            Log.d(TAG, "Sleep fail");
        }
        LocalSocket localSocket = new LocalSocket();
        try {
            localSocket.connect(new LocalSocketAddress(ip_address + "-video"));
            in = localSocket.getInputStream();
            readableByteChannel = Channels.newChannel(in);
            localSocket.setSoTimeout(500);
            if (localSocket.isConnected()) {
                byte[] extra_data = new byte[512];
                int n = in.read(extra_data);
                String extra_msg = new String(extra_data, "UTF-8");
                String[] extra_split = extra_msg.split(":");
                Log.d(TAG, extra_msg);
                String source_type = extra_split[0];
                String source_width = "";
                String source_height = "";
                String source_sps = "";
                String source_pps = "";

                if (source_type.equalsIgnoreCase("RTSP")) {
                    Log.d(TAG, "RTSP Source");
                    source_width = extra_split[1];
                    source_height = extra_split[2];
                    // find pps start code
                    int pps_pos = extra_split[3].indexOf("00000001", 5);
                    source_sps = extra_split[3].substring(0, pps_pos);
                    source_pps = extra_split[3].substring(pps_pos, extra_split[3].length());

                } else if (source_type.equalsIgnoreCase("OV")) {
                    //TODO: Remove hardcode
                    Log.d(TAG, "OV Source");

                    source_width = "1280";
                    source_height = "720";
                    source_sps = OV_SPS;
                    source_pps = OV_PPS;
                }

                /*
                    trigger decode thread of client side, if sending the following message.
                 */
                sendVideoStartMsg("video/avc", source_width + "", source_height + "", source_sps, source_pps, ip_address);
                //sendVideoMsg("Video:video/avc:" + source_width + ":" + source_height + ":" + source_sps + ":" + source_pps + ":");


            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "connect fail");
            sendVideoMsg("Video:Something Wrong");
            bStart = false;
        }

        // raw data ..
        int minus1Counter = 0;
        mAgent.setDirectBufferIndex(transByteBuffer,mCompId);
        while (!Thread.interrupted() && bStart) {
            int naluSize = 0;
            try {

                naluSize = in.read(transferBuffer);
                //transByteBuffer.clear();
               // naluSize = readableByteChannel.read(transByteBuffer);

                if (naluSize == -1) {
                    minus1Counter++;
                    Log.d(TAG, "minus1Counter :" + minus1Counter);

                    if (minus1Counter > 30) {
                        bStart = false;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "something wrong at inputstream read : " + e);
                bStart = false;

//                if (true) {
//
//                    try {
//                        sleep(3500);
//                        int Count = 0;
//                        while (Count <= 10) {
//                            try {
//                                localSocket = null;
//                                localSocket = new LocalSocket();
//                                localSocket.connect(new LocalSocketAddress(ip_address + "-video"));
//                                localSocket.setSoTimeout(500);
//
//                                in = localSocket.getInputStream();
//                                byte[] extra_data = new byte[512];
//                                in.read(extra_data);
//                                bStart = true;
//                                break;
//                            } catch (IOException eee) {
//                                Count++;
//                                sleep(500);
//                            }
//                        }
//
//
//                    } catch (InterruptedException ee) {
//
//                    }
//                }


            }

            if (naluSize > 0) {
                minus1Counter = 0;
                transByteBuffer.position(0);
                transByteBuffer.put(transferBuffer, 0, naluSize);

                int sendSize = mAgent.sendDataDirectByIndex(transByteBuffer,naluSize,mCompId,mStreamId,mCompId);
                if(sendSize==-1) {
                    Log.d(TAG,"Something wrong when send buffer send Size = -1");
                } else {
                    bitRate += sendSize;

                    currentFPS++;
                    if(startTime==-1) {
                        startTime = System.currentTimeMillis();
                    }

                    if(System.currentTimeMillis()-startTime>=1000) {
                        Log.d(TAG,"IP :"+ip_address+" ===>  FPS : "+currentFPS+", bitrate : "+bitRate);
                        bitRate = 0;
                        currentFPS = 0;
                        startTime = System.currentTimeMillis();
                    }


                }


            }
        }

        while (true) {
            try {
                localSocket.close();
                break;
            } catch (Exception e) {
                Log.d(TAG, "LocalSocket close fail");
            }
        }

    }
}


