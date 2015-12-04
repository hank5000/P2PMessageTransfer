package com.via.cloudwatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by HankWu_Office on 2015/11/25.
 */
public class ServerService extends Service {

    public static final String TAG = "nice Service";
    public static final String ServerStart = "com.via.rtc";

    P2PThread p2pThread = null;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        p2pThread = new P2PThread();
        //p2pThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy lo.");
    }


    public ServerService() {
        // super(TAG);
        // TODO Auto-generated constructor stub
    }
    public int cameraNumber = 0;
    public String cameraUrl[] = new String[8];
    public String cameraNick[] = new String[8];

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");

        if(intent==null) {
            return super.onStartCommand(intent,flags,startId);
        }
        if (intent.getAction().equals(ServerStart)) {
            new Thread(p2pThread.runTask).start();

            cameraNumber = intent.getIntExtra("NUMBER_OF_CAMERA",0);
            Log.d(TAG, "GET Camera number :" + cameraNumber);

            for (int i=0;i<cameraNumber;i++) {
                cameraUrl[i] = intent.getStringExtra("CAMERA_URI_"+i);
                cameraNick[i] = intent.getStringExtra("CAMERA_NAME_"+i);

                Log.d(TAG,cameraNick[i]+":"+cameraUrl[i]);
                if (p2pThread!=null) {
                    p2pThread.setCameraUrlAndNick(cameraNumber,cameraUrl,cameraNick);
                }

            }

        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return super.onStartCommand(intent,flags,startId);
    }




}
