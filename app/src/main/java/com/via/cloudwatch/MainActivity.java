package com.via.cloudwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.utils.CommunicationChannel;
import com.utils.QueryToServer;
import com.utils.SendingLocalVideoThread;
import com.utils.VideoRecvCallback;
import com.via.libnice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;


    libnice mNice = null;
    int     mStreamId = -1;

    SurfaceView[] videoSurfaceViews = new SurfaceView[4];
    ImageButton[] addBtns = new ImageButton[4];
    ImageButton[] rmBtns  = new ImageButton[4];
    RelativeLayout[] rLayouts = new RelativeLayout[4];
    SendingLocalVideoThread[] sendingThreads = new SendingLocalVideoThread[4];
    VideoRecvCallback[] videoRecvCallbacks = new VideoRecvCallback[4];


    void initButtonAndSurfaceView() {

        rLayouts[0] = (RelativeLayout) findViewById(R.id.relate1);
        rLayouts[1] = (RelativeLayout) findViewById(R.id.relate2);
        rLayouts[2] = (RelativeLayout) findViewById(R.id.relate3);
        rLayouts[3] = (RelativeLayout) findViewById(R.id.relate4);

        videoSurfaceViews[0] = (SurfaceView) findViewById(R.id.surfaceView1);
        videoSurfaceViews[1] = (SurfaceView) findViewById(R.id.surfaceView2);
        videoSurfaceViews[2] = (SurfaceView) findViewById(R.id.surfaceView3);
        videoSurfaceViews[3] = (SurfaceView) findViewById(R.id.surfaceView4);

        addBtns[0] = (ImageButton) findViewById(R.id.addBtn1);
        addBtns[1] = (ImageButton) findViewById(R.id.addBtn2);
        addBtns[2] = (ImageButton) findViewById(R.id.addBtn3);
        addBtns[3] = (ImageButton) findViewById(R.id.addBtn4);



        rmBtns[0] = (ImageButton) findViewById(R.id.rmBtn1);
        rmBtns[1] = (ImageButton) findViewById(R.id.rmBtn2);
        rmBtns[2] = (ImageButton) findViewById(R.id.rmBtn3);
        rmBtns[3] = (ImageButton) findViewById(R.id.rmBtn4);

        for(int i=0;i<4;i++) {
            addBtns[i].setOnLongClickListener(requestLiveView);
            rmBtns[i].setOnLongClickListener(requestLiveView);
        }
    }

    MainActivity instance = this;
    String localSdp = null;
    Handler handler = new Handler();
    CommunicationChannel msgChannel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        /*
            set all button/view/layout to instance.
         */
        initButtonAndSurfaceView();




        mNice = new libnice();
        mNice.init();
        mNice.createAgent(1);
        mNice.setStunAddress("74.125.204.127", 19302);
        mNice.setControllingMode(0);
        mStreamId = mNice.addStream("HankWu",5);


        // Component 1 is using for message transfer
        int forComponentIndex = 1;
        msgChannel = new CommunicationChannel(instance,mNice,mStreamId,forComponentIndex);
        mNice.registerReceiveCallback(msgChannel, mStreamId, forComponentIndex);
        forComponentIndex = 2;
        videoRecvCallbacks[0] = new VideoRecvCallback(videoSurfaceViews[0]);
        mNice.registerReceiveCallback(videoRecvCallbacks[0], mStreamId, forComponentIndex);
        forComponentIndex = 3;
        videoRecvCallbacks[1] = new VideoRecvCallback(videoSurfaceViews[1]);
        mNice.registerReceiveCallback(videoRecvCallbacks[1], mStreamId, forComponentIndex);
        forComponentIndex = 4;
        videoRecvCallbacks[2] = new VideoRecvCallback(videoSurfaceViews[2]);
        mNice.registerReceiveCallback(videoRecvCallbacks[2], mStreamId, forComponentIndex);
        forComponentIndex = 5;
        videoRecvCallbacks[3] = new VideoRecvCallback(videoSurfaceViews[3]);
        mNice.registerReceiveCallback(videoRecvCallbacks[3], mStreamId, forComponentIndex);

        mNice.registerStateObserver(new libnice.StateObserver() {
            @Override
            public void cbCandidateGatheringDone(final int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(instance , "Candidate Gathering Done Stream["+i+"]", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void cbComponentStateChanged(final int i,final int i1,final int i2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready")||libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("fail")) {
                            Toast.makeText(instance, "Stream[" + i + "]Component[" + i1 + "]:" + libnice.StateObserver.STATE_TABLE[i2], Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        localSdp = mNice.getLocalSdp(mStreamId);
    }

    Fragment currentFragment = null;
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (position == 0) {
            if(currentFragment!=null) {
                fragmentManager.beginTransaction().remove(currentFragment).commit();
                mTitle = getString(R.string.title_section1);
            }
        } else {
            currentFragment = PlaceholderFragment.newInstance(position + 1);
            fragmentManager.beginTransaction()
                    .replace(R.id.container, currentFragment)
                    .commit();
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_server) {
            new Thread(registerTask).start();
            handler.postDelayed(serverTask, 1000);


            return true;
        }
        if (id == R.id.action_client) {
            new Thread(clientTask).start();

            return true;
        }

        if (id == R.id.action_sending) {
            a.start();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    String remoteSdp = null;

    Runnable serverTask = new Runnable(){
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String method = "Server";
                        String postParameters = "register="+URLEncoder.encode("FALSE", "UTF-8")
                                +"&username="+URLEncoder.encode("HankWu","UTF-8");
                        String getSdp = QueryToServer.excutePost(method, postParameters);
                        if(getSdp.startsWith("NOBODY")) {
                           //showToast("No Remote SDP");
                            handler.postDelayed(serverTask,1000);

                        } else {
                            showToast("Get remote SDP "+getSdp);
                            remoteSdp = getSdp;
                            handler.removeCallbacks(serverTask);
                            handler.post(setSdpTask);
                        }
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    };

    Runnable clientTask = new Runnable(){
        @Override
        public void run() {
            try {
                String method = "Client";
                String findusername = "HankWu";
                String postParameters = "findusername="+URLEncoder.encode(findusername,"UTF-8") + "&SDP="+URLEncoder.encode(localSdp,"UTF-8");
                String getSdp = QueryToServer.excutePost(method, postParameters);
                if(getSdp.equals("OFFLINE")) {
                    showToast(findusername+"is OFFLINE");
                } else {
                    showToast("Get remote SDP "+getSdp);
                    remoteSdp = getSdp;
                    handler.post(setSdpTask);
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    Runnable registerTask = new Runnable() {
        @Override
        public void run() {
            String method = "Server";
            String postParams;
            try {
                postParams = "register=" + URLEncoder.encode("TRUE", "UTF-8")
                        + "&username=" + URLEncoder.encode("HankWu", "UTF-8")
                        + "&SDP=" + URLEncoder.encode(localSdp, "UTF-8");
                QueryToServer.excutePost(method, postParams);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    Runnable setSdpTask = new Runnable() {
        @Override
        public void run() {
            mNice.setRemoteSdp(remoteSdp);
        }
    };

    void showToast(final String tmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(instance, tmp,Toast.LENGTH_SHORT).show();
            }
        });
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

            for(int i=0;i<me.getTrackCount();i++) {
                mf = me.getTrackFormat(i);
                mime = mf.getString(MediaFormat.KEY_MIME);


                if(mime.startsWith("video")) {
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

                    videoMsg = videoMsg + ":" + mime + ":" + w + ":" + h + ":" + s_sps + ":" + s_pps + ":";

                    mNice.sendMsg(videoMsg, mStreamId, 2);
                    mNice.sendMsg(videoMsg, mStreamId, 3);
//                    mNice.sendMsg(videoMsg, mStreamId, 4);
//                    mNice.sendMsg(videoMsg, mStreamId, 5);
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
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    ByteBuffer naluBuffer = ByteBuffer.allocateDirect(1024*1024);

    int DEFAULT_DIVIDED_SIZE = 1024*1024;
    boolean bInit = false;
    Thread a = new Thread(new Runnable(){
        @Override
        public void run() {
            int counter = 0;
            if(!bInit) {
                initMediaExtractor("/mnt/sata/H264_2M.mp4");
                bInit = true;
            }
            for(;;){
                // TODO Auto-generated method stub
                int naluSize = me.readSampleData(naluBuffer, 0);

                int divideSize = DEFAULT_DIVIDED_SIZE;
                int sentSize = 0;
                //nice.sendMsg("NALU", 1);

                //for(;;) {
                if(naluSize > 0)
                {
                    for(;;) {
                        if((naluSize-sentSize) < divideSize) {
                            divideSize = naluSize-sentSize;
                        }

                        naluBuffer.position(sentSize);
                        naluBuffer.limit(divideSize+sentSize);
                        // Reliable mode : if send buffer size bigger than MTU, the destination side will received data partition which is divided by 1284.
                        // Normal mode   : if send buffer size bigger than MTU, the destination side will received all data in once receive.
                        mNice.sendDataDirect(naluBuffer.slice(),divideSize,mStreamId,2);
                        mNice.sendDataDirect(naluBuffer.slice(),divideSize,mStreamId,3);

//                        mNice.sendDataDirect(naluBuffer.slice(),divideSize,mStreamId,4);
//                        mNice.sendDataDirect(naluBuffer.slice(),divideSize,mStreamId,5);


                        naluBuffer.limit(naluBuffer.capacity());

                        sentSize += divideSize;
                        if(sentSize >= naluSize) {
                            break;
                        }
                    }
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
    });

    View.OnLongClickListener requestLiveView = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            String msg = "";
            String state = "";
            int number = -1;
            switch (v.getId()) {
                case R.id.addBtn1 :
                    msg = "addbtn1";
                    state = "RUN";
                    number = 1;
                    break;
                case R.id.addBtn2 :
                    msg = "addbtn2";
                    state = "RUN";
                    number = 2;
                    break;
                case R.id.addBtn3 :
                    msg = "addbtn3";
                    state = "RUN";
                    number = 3;
                    break;
                case R.id.addBtn4 :
                    msg = "addbtn4";
                    state = "RUN";
                    number = 4;
                    break;
                case R.id.rmBtn1 :
                    msg = "rmBtn1";
                    state = "STOP";
                    number = 1;
                    break;
                case R.id.rmBtn2 :
                    msg = "rmBtn2";
                    state = "STOP";
                    number = 2;
                    break;
                case R.id.rmBtn3 :
                    msg = "rmBtn3";
                    state = "STOP";
                    number = 3;
                    break;
                case R.id.rmBtn4 :
                    msg = "rmBtn4";
                    state = "STOP";
                    number = 4;
                    break;
            }
            showToast("VIDEO:"+state+":"+number+":");
            msgChannel.sendMessage("VIDEO:"+state+":"+number+":");

            if(state.equalsIgnoreCase("STOP")) {
                if(videoRecvCallbacks[number-1].isStart()) {
                    videoRecvCallbacks[number - 1].setStop();
                }
            }

            final String fstate = state;
            final int    fnumber = number;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeState(fstate,fnumber-1);
                }
            });
            Toast.makeText(instance,msg,Toast.LENGTH_SHORT).show();

            return false;
        }
    };

    void changeState(String state,int number){
        if (state.equalsIgnoreCase("RUN")) {
            addBtns[number].setVisibility(View.INVISIBLE);
            rmBtns[number].setVisibility(View.VISIBLE);
        } else if (state.equalsIgnoreCase("STOP")) {
            addBtns[number].setVisibility(View.VISIBLE);
            rmBtns[number].setVisibility(View.INVISIBLE);
        }
    }

    public void createSendingThread(int Stream_id,int onChannel) {
        if(sendingThreads[onChannel-1]==null) {
            showToast("create Sending Thread");
            Log.d("hank","Create sending Thread");
            sendingThreads[onChannel-1] = new SendingLocalVideoThread(mNice,Stream_id,onChannel,"/mnt/sata/H264_2M.mp4");
            sendingThreads[onChannel-1].start();
        }
    }

    public void stopSendingThread(int Stream_id,int onChannel) {
        if(sendingThreads[onChannel-1]!=null){
            sendingThreads[onChannel-1].setStop();
            sendingThreads[onChannel-1].interrupt();
            try {
                sendingThreads[onChannel-1].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendingThreads[onChannel-1] = null;
            showToast("Stop sending Thread "+onChannel);
        }
    }

}
