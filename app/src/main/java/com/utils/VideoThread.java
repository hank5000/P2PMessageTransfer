package com.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
/**
 * Created by HankWu_Office on 2015/8/20.
 */
public class VideoThread extends Thread {
    final static String TAG = "libnice-vt";

    byte[] inputStreamTmp = new byte[1024*1024];
    ByteBuffer rawDataCollectBuffer = ByteBuffer.allocate(1024*1024*10);
    byte[] dst  = new byte[1024*1024];
    private MediaCodec decoder;
    private Surface surface;
    private InputStream is;
    private boolean bStart = true;
    VideoDisplayThread vdt = null;
    public boolean bIsEnd = false;

    public void setStop() {
        bStart = false;
    }

    String mMime;
    int    mWidth;
    int    mHeight;
    String mSPS;
    String mPPS;
    int collectLen = 0;
    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public VideoThread(Surface surf,String mime,int width,int height,String sps,String pps,InputStream inputStream) {
        this.surface  = surf;
        this.mMime    = mime;
        this.mWidth   = width;
        this.mHeight  = height;
        this.mSPS     = sps;
        this.mPPS     = pps;
        this.is       = inputStream;
    }

    final static String MediaFormat_SPS = "csd-0";
    final static String MediaFormat_PPS = "csd-1";

    public void run() {

        /// Create Decoder -START- ///
        try {
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, mMime);
            format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
            format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);
            format.setByteBuffer(MediaFormat_SPS, ByteBuffer.wrap(hexStringToByteArray(mSPS)));
            format.setByteBuffer(MediaFormat_PPS, ByteBuffer.wrap(hexStringToByteArray(mPPS)));

            decoder = MediaCodec.createDecoderByType(mMime);
            if(decoder == null) {
                Log.d(TAG, "This device cannot support codec :" + mMime);
            }
            decoder.configure(format, surface, null, 0);
        } catch (Exception e) {
            Log.d(TAG,"Create Decoder Fail, because "+e);
            //Log.d(TAG, "This device cannot support codec :" + mMime);
        }
        if (decoder == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }
        decoder.start();
        /// Create Decoder -END- ///
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        /// Decode -START- ///
        int readSize = 0;
        int firstNalu = 0;
        int secondNalu = 0;

        if(vdt == null) {
            vdt = new VideoDisplayThread(decoder,outputBuffers,info);
            vdt.start();
        }

        while (!Thread.interrupted() && bStart && decoder!=null ) {
            int inIndex = decoder.dequeueInputBuffer(10000);
            if (inIndex > 0) {

                while (!Thread.interrupted() && bStart && decoder!=null && is!=null) {
                    try {
                        readSize = is.read(inputStreamTmp);
                        if(readSize>0) {
                            rawDataCollectBuffer.put(inputStreamTmp, 0, readSize);
                        }
                    } catch (Exception e) {
                        Log.d(TAG,"inputstream cannot read : "+e);

                        if(!bStart) {
                            break;
                        }
                    }

                    firstNalu = findNalu(0,rawDataCollectBuffer);
                    
                    //Log.d(TAG,"firstNalue : "+ firstNalu +"rawDataCollectBuffer :" +rawDataCollectBuffer.get(0)+rawDataCollectBuffer.get(1)+rawDataCollectBuffer.get(2)+rawDataCollectBuffer.get(3));

                    if(firstNalu!=-1) {
                    	secondNalu = findNalu(firstNalu+3,rawDataCollectBuffer);

                    	if(secondNalu!=-1 && secondNalu > firstNalu ) {
                    		rawDataCollectBuffer.flip();
                    		rawDataCollectBuffer.position(firstNalu);
                    		
                        	//Log.d(TAG,"FirstNALU :"+firstNalu+" ,SecondNALU :"+secondNalu+"size :"+ (secondNalu-firstNalu)+", rawDataCollectBuffer remaining:"+rawDataCollectBuffer.remaining());

                    		rawDataCollectBuffer.get(dst, 0, secondNalu-firstNalu);
                    		rawDataCollectBuffer.compact();
                    		
                    		int nalu_unit_type = (dst[4] & 0x1F);
                    		//Log.d(TAG,"NALU TYPE :" +nalu_unit_type);
                    		//if(nalu_unit_type!=8 && nalu_unit_type!=7 && nalu_unit_type!=6)
                    		{
	                    		ByteBuffer buffer = inputBuffers[inIndex];
	                    		buffer.clear();
	                    		buffer.put(dst, 0, secondNalu-firstNalu);
	                    		decoder.queueInputBuffer(inIndex, 0, secondNalu-firstNalu, 0, 0);
	                    		break;
                    		}
                    	}
                    } else {
                    	Log.d(TAG,"Something wrong");
                    }

                }
            }
        }

        vdt.interrupt();
        try {
            vdt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vdt = null;

        decoder.stop();
        decoder.release();

        bIsEnd = true;
    }
    /// Decode -END- ///

    public class VideoDisplayThread extends Thread {
        MediaCodec decoder = null;
        ByteBuffer[] outputBuffers = null;
        MediaCodec.BufferInfo info = null;
        public VideoDisplayThread(MediaCodec codec,ByteBuffer[] bbs, MediaCodec.BufferInfo bi) {
            this.decoder = codec;
            this.outputBuffers = bbs;
            this.info = bi;
        }

        public void run() {
            while (bStart && decoder!=null ) {
                //Log.d("libnice", "coming");
                int outIndex = this.decoder.dequeueOutputBuffer(this.info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("libnice", "INFO_OUTPUT_BUFFERS_CHANGED");
                        this.outputBuffers = this.decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("libnice", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        //Log.d("libnice", "INFO_TRY_AGAIN_LATER");
                        //Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        // ByteBuffer buffer = outputBuffers[outIndex];
                        // Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);
                        try {
                            sleep(30);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        //Log.d("libnice", "coming2");
                        this.decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }
            }
        }
    }
    
    int findNalu(int offset,ByteBuffer bb) {
    	int limit = bb.limit();
    	int ret = -1;
    	int currentPos = bb.position();
    	
    	if(offset > currentPos) {
    		return ret;
    	}
    	
    	for(int i=offset;i<(currentPos-4);i++) {
	    	if ((bb.get(i)==0 && bb.get(i+1) == 0 && bb.get(i+2) == 0 && bb.get(i+3) == 1 )) {
	    		return i;
	    	}
    	}
    	return ret;
    }

}

