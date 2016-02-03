package com.via.cloudwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.utils.CommunicationChannel;
import com.utils.DefaultSetting;
import com.utils.LiveViewInfo;
import com.utils.SendingLiveViewThread;
import com.utils.SendingLocalVideoThread;
import com.utils.VideoRecvCallback;
import com.utils.Item;
import com.utils.ItemArrayAdapter;
import com.via.libnice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    MainActivity instance = this;
    Handler handler = new Handler(); // The delay event handler.

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    // layout part
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    SurfaceView[] videoSurfaceViews = new SurfaceView[4];
    ImageButton[] addBtns = new ImageButton[4];
    ImageButton[] rmBtns = new ImageButton[4];
    public TextView[] fpsViews = new TextView[4];
    RelativeLayout[] rLayouts = new RelativeLayout[4];
    LinearLayout[] lLayouts = new LinearLayout[2];
    DisplayMetrics metrics = null;
    Fragment currentFragment = null;
    static int currentPosition = -1;
    int fullScreenNumber = -1;
    LinearLayout.LayoutParams hidingLinear = new LinearLayout.LayoutParams(0, 0, 0.0f);
    LinearLayout.LayoutParams normalLinear = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, 1.0f);

    ProgressDialog mProgressBar = null;
    // libnice part
    libnice mNice = null;
    int mStreamId = -1;
    String localSdp = null;
    String remoteSdp = null;
    private final static String stunServerIp = DefaultSetting.stunServerIp;
    private final static int stunServerPort = DefaultSetting.stunServerPort;
    // peer to peer communicate channel
    CommunicationChannel msgChannel = null;

    // streaming receiver thread / streaming out thread
    VideoRecvCallback[] videoRecvCallbacks = new VideoRecvCallback[4];
    SendingLocalVideoThread[] sendingThreads = new SendingLocalVideoThread[4];
    SendingLiveViewThread[] sendingLiveThreads = new SendingLiveViewThread[4];

    // Register Server part
    private Socket mSocket;
    private final static String serverUrl = DefaultSetting.serverUrl;
    public String mSourcePeerUsername = DefaultSetting.sourcePeerUsername;
    public String mFindPeerName = DefaultSetting.findPeerUsername;
    public String mFindPeerPassword = DefaultSetting.findPeerPassword;

    P2PThread p2pthread = null;
    public boolean bShowFPS = true;

    // status recording
    boolean[] isAllReadyTable = new boolean[5];
    boolean isAllReady = false;
    public ArrayList<LiveViewInfo> liveViewList = new ArrayList<LiveViewInfo>();
    boolean bClient = false;
    boolean bSource = false;
    boolean bHiddingAllBtn = false;


    int serverClickCounter = 0;
    public String[] channelsState = new String[4];

    // CloudWatch can using mediaplayer to play OV/RTSP and p2p stream out by itself
    MediaPlayer[] mediaplayers = new MediaPlayer[4];
    boolean bLogin = false;
    boolean checkAllReady() {
        isAllReady = (isAllReadyTable[0] && isAllReadyTable[1] && isAllReadyTable[2] && isAllReadyTable[3] && isAllReadyTable[4]);

        if (isAllReady) {
            mProgressBar.dismiss();
            onNavigationDrawerItemSelected(0);
            showToast("I", "CONNECT!");
            bLogin = true;
        }

        if (isItAllReady()) {
            msgChannel.sendMessage(CommunicationChannel.REQUEST_LIVE_VIEW_INFO);
        }

        return isAllReady;
    }

    boolean isItAllReady() {
        return isAllReadyTable[0] && isAllReadyTable[1] && isAllReadyTable[2] && isAllReadyTable[3] && isAllReadyTable[4];
    }

    void disableReady() {
        for(int i=0;i<isAllReadyTable.length;i++) {
            isAllReadyTable[i] = false;
        }
    }

    @Override
    protected void onPause() {
        for(int i=0;i<4;i++) {
            if (mediaplayers[i] != null) {
                mediaplayers[i].release();
                mediaplayers[i] = null;
            }
        }
        if(mSocket!=null) {
            mSocket.disconnect();
        }
        super.onPause();
    }

    void initButtonAndSurfaceView() {
        rLayouts[0] = (RelativeLayout) findViewById(R.id.relate1);
        rLayouts[1] = (RelativeLayout) findViewById(R.id.relate2);
        rLayouts[2] = (RelativeLayout) findViewById(R.id.relate3);
        rLayouts[3] = (RelativeLayout) findViewById(R.id.relate4);

        lLayouts[0] = (LinearLayout) findViewById(R.id.linear1);
        lLayouts[1] = (LinearLayout) findViewById(R.id.linear2);

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

        fpsViews[0] = (TextView) findViewById(R.id.fpsView1);
        fpsViews[1] = (TextView) findViewById(R.id.fpsView2);
        fpsViews[2] = (TextView) findViewById(R.id.fpsView3);
        fpsViews[3] = (TextView) findViewById(R.id.fpsView4);

        for (int i = 0; i < 4; i++) {
            addBtns[i].setOnClickListener(requestLiveView);
            rmBtns[i].setOnClickListener(requestLiveView);
            addBtns[i].setOnLongClickListener(fullScreenListener);
            rmBtns[i].setOnLongClickListener(fullScreenListener);
            channelsState[i] = "STOP";
        }

        mProgressBar = new ProgressDialog(instance);
        mProgressBar.setTitle("Wait for Connection");
        mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
//        /*
//            set all button/view/layout to instance.
//         */
        if(!bSource) {
            initButtonAndSurfaceView();

            mNice = new libnice();
            mNice.init();
            mNice.createAgent(DefaultSetting.isReliableMode());
            mNice.setStunAddress(stunServerIp, stunServerPort);
            mNice.setControllingMode(0);
            mStreamId = mNice.addStream(DefaultSetting.streamName, 5);

            // Component 1 is using for message transfer
            int forComponentIndex = 1;
            msgChannel = new CommunicationChannel(instance, mNice, mStreamId, forComponentIndex);
            mNice.registerReceiveCallback(msgChannel, mStreamId, forComponentIndex);
            forComponentIndex = 2;
            videoRecvCallbacks[0] = new VideoRecvCallback(videoSurfaceViews[0],instance,0);
            mNice.registerReceiveCallback(videoRecvCallbacks[0], mStreamId, forComponentIndex);
            forComponentIndex = 3;
            videoRecvCallbacks[1] = new VideoRecvCallback(videoSurfaceViews[1],instance,1);
            mNice.registerReceiveCallback(videoRecvCallbacks[1], mStreamId, forComponentIndex);
            forComponentIndex = 4;
            videoRecvCallbacks[2] = new VideoRecvCallback(videoSurfaceViews[2],instance,2);
            mNice.registerReceiveCallback(videoRecvCallbacks[2], mStreamId, forComponentIndex);
            forComponentIndex = 5;
            videoRecvCallbacks[3] = new VideoRecvCallback(videoSurfaceViews[3],instance,3);
            mNice.registerReceiveCallback(videoRecvCallbacks[3], mStreamId, forComponentIndex);

            mNice.registerStateObserver(new libnice.StateObserver() {
                @Override
                public void cbCandidateGatheringDone(final int i) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }

                @Override
                public void cbComponentStateChanged(final int i, final int i1, final int i2) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready") || libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("failed")) {
                                showToast("D", "Stream[" + i + "]Component[" + i1 + "]:" + libnice.StateObserver.STATE_TABLE[i2]);
                                if (libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready")) {
                                    isAllReadyTable[i1 - 1] = true;
                                    checkAllReady();
                                }

                                if (libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("failed")) {
                                    showToast("I", "CONNECT TO SOURCE PEER FAIL, PLEASE TRY AGAIN");

                                    if(tmpUET!=null)
                                        tmpUET.setEnabled(true);
                                    if(tmpPET!=null)
                                        tmpPET.setEnabled(true);
                                }
                            }
                        }
                    });
                }
            });
            localSdp = mNice.getLocalSdp(mStreamId);

            // use for measure the size of AlertDialog.
            metrics = getResources().getDisplayMetrics();

            try {
                mSocket = IO.socket(serverUrl);
                mSocket.on("response", onResponse);
                mSocket.on("get sdp", onGetSdp);
                mSocket.on("restart stream", onRestartStream);
                mSocket.connect();
            } catch (URISyntaxException e) {
                showToast("I","Server is offline, please contact your application provider!");
                throw new RuntimeException(e);
            }
        } else {
            p2pthread = new P2PThread();
            p2pthread.start();
        }

        onNavigationDrawerItemSelected(2);
    }


    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String message;
            try {
                message = data.getString("message");
                showToast("D","onRespone:" + message);
                if(message.startsWith("OCCUPY")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showOccupyMessage();
                        }
                    });
                } else if (message.startsWith("UNDEFINED")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showUndefinedMessage();
                        }
                    });
                } else if (message.startsWith("OFFLINE")) {
                    //showOfflineMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showOfflineMessage();
                        }
                    });
                }

            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onGetSdp = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            JSONObject data = (JSONObject) args[0];
            String SDP;
            try {
                SDP = data.getString("SDP");
                mNice.setRemoteSdp(SDP);
                showToast("D","GetSDP:" + SDP);
            } catch (JSONException e) {
                return;
            }

        }
    };

    private Emitter.Listener onRestartStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            showToast("I","on Restart Stream");
            // stop all sending thread
            for(int i=0;i<4;i++) {
                stopSendingThread(mStreamId, i+1);
            }
            mNice.restartStream(mStreamId);
            localSdp = mNice.getLocalSdp(mStreamId);
            mSocket.emit("set local sdp", localSdp);
        }
    };


    public void showSettingFragment() {

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        currentPosition = position;

        if(bHiddingAllBtn) {
            // get setting fragment value
            bShowFPS = ((CheckBox)currentFragment.getView().findViewById(R.id.showFPSCheckBox)).isChecked();
            mFindPeerName = ((EditText) currentFragment.getView().findViewById(R.id.usernameEditText)).getText().toString();
            mFindPeerPassword = ((EditText) currentFragment.getView().findViewById(R.id.passwordEditText)).getText().toString();
        }

        if (position == 0) {
            if (currentFragment != null) {
                fragmentManager.beginTransaction().remove(currentFragment).commit();
                mTitle = getString(R.string.title_section1);
            }

            restoreAllBtn();
        } else if (position == 1) {
            if(DefaultSetting.bShowConnectMenu || getDeviceName().contains("Elite1000") || getDeviceName().contains("elite1000")) {
                if (currentFragment != null) {
                    fragmentManager.beginTransaction().remove(currentFragment).commit();
                    mTitle = getString(R.string.title_section1);
                }
                currentFragment = PlaceholderFragment.newInstance(position + 1);
                fragmentManager.beginTransaction()
                        .replace(R.id.container2, currentFragment)
                        .commit();
                restoreAllBtn();
            } else {

            }
        } else if (position == 2) {
            if (currentFragment != null) {
                fragmentManager.beginTransaction().remove(currentFragment).commit();
                mTitle = getString(R.string.title_section1);
            }
            currentFragment = PlaceholderFragment.newInstance(position + 1);

            fragmentManager.beginTransaction()
                    .replace(R.id.container3, currentFragment)
                    .commit();

            hiddingAllBtn();
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

    public void hiddingAllBtn() {
        if(!bHiddingAllBtn) {
            bHiddingAllBtn = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 4; i++) {
                        rmBtns[i].setVisibility(View.INVISIBLE);
                        addBtns[i].setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    }

    public void restoreAllBtn() {
        if(bHiddingAllBtn) {
            bHiddingAllBtn = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 4; i++) {
                        if (channelsState[i].equalsIgnoreCase("RUN")) {
                            rmBtns[i].setVisibility(View.VISIBLE);
                        } else {
                            addBtns[i].setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    static TextView tmpUET = null;
    static TextView tmpPET = null;

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

            View rootView = null;
            final MainActivity tmp = (MainActivity) getActivity();

            if(tmp.currentPosition==1) {
                rootView = inflater.inflate(R.layout.fragment_main, container, false);

                rootView.findViewById(R.id.connectServerBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(tmp.serverClickCounter!=0) {
                            tmp.mNice.restartStream(tmp.mStreamId);
                            tmp.localSdp = tmp.mNice.getLocalSdp(tmp.mStreamId);
                        }

                        tmp.serverClickCounter++;
                        //new Thread(tmp.registerTask).start();
                        //tmp.handler.postDelayed(tmp.serverTask, 1000);
                        tmp.mSocket.emit("add user",tmp.mSourcePeerUsername);
                        tmp.mSocket.emit("set local sdp", tmp.localSdp);
                        //TODO: Test use only
                        String[] path = new String[4];
                        path[0] = "ov://192.168.12.121:1000";
                        path[1] = "ov://192.168.12.112:1000";
                        path[2] = "ov://192.168.12.114:1000";
                        path[3] = "";
                        for(int i=0;i<4;i++) {
                            if(!path[i].equals("")) {
                                tmp.mediaplayers[i] = new MediaPlayer();
                                try {
                                    tmp.mediaplayers[i].setDataSource(path[i]);
                                    tmp.mediaplayers[i].setDisplay(tmp.videoSurfaceViews[i].getHolder());
                                    tmp.mediaplayers[i].prepare();
                                    tmp.mediaplayers[i].start();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });

                rootView.findViewById(R.id.connectClientBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tmp.bClient = true;
                        //new Thread(tmp.clientTask).start();
                        tmp.showToast("D","get remote sdp");

                        tmp.mSocket.emit("get remote sdp",tmp.mFindPeerName+":"+tmp.localSdp);
                    }
                });

                rootView.findViewById(R.id.restartBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tmp.showToast("D","force restart source peer sdp");

                        tmp.mSocket.emit("force get sdp",tmp.mFindPeerName);
                    }
                });

            } else if (tmp.currentPosition==2) {
                rootView = inflater.inflate(R.layout.fragment_setting, container, false);
                EditText usernameET = ((EditText) rootView.findViewById(R.id.usernameEditText));
                usernameET.setText(tmp.mFindPeerName);
                EditText passwordET = ((EditText) rootView.findViewById(R.id.passwordEditText));
                passwordET.setText(tmp.mFindPeerPassword);
                CheckBox showFPSCB = (CheckBox) rootView.findViewById(R.id.showFPSCheckBox);
                showFPSCB.setChecked(tmp.bShowFPS);

                if(tmp.bLogin) {
//                    usernameET.setKeyListener(null);
//                    passwordET.setKeyListener(null);
                    usernameET.setEnabled(false);
                    passwordET.setEnabled(false);
                    tmpUET = usernameET;
                    tmpPET = passwordET;

                } else {
                    usernameET.setEnabled(true);
                    passwordET.setEnabled(true);
                }

                final EditText uET = usernameET;
                final EditText pET = passwordET;

                rootView.findViewById(R.id.saveBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

//                        tmp.mSocket.emit("force get sdp", tmp.mFindPeerName);
//                        tmp.mProgressBar.show();
//                        tmp.handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                               tmp.mSocket.emit("get remote sdp",tmp.mFindPeerName+":"+tmp.localSdp);
//                            }
//                        },2000);
                        tmp.mSocket.emit("force get sdp", tmp.mFindPeerName);
                        tmp.bClient = true;
                        uET.setEnabled(false);
                        pET.setEnabled(false);
                        tmp.mProgressBar.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                tmp.mSocket.emit("get remote sdp", tmp.mFindPeerName + ":" + tmp.localSdp);

                            }
                        }).start();

                    }
                });


                rootView.findViewById(R.id.logoutBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tmp.bLogin = false;
                        tmp.isAllReady = false;
                        for(int i=0;i<5;i++) {
                            tmp.isAllReadyTable[i] = false;
                        }
                        tmp.stopAllRecv();
                        tmp.resetNiceAgent();

                        uET.setEnabled(true);
                        pET.setEnabled(true);
                    }
                });
            }

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    Runnable setSdpTask = new Runnable() {
        @Override
        public void run() {
            mNice.setRemoteSdp(remoteSdp);
        }
    };

    public void showToast(String level,final String tmp) {

        if(level.equalsIgnoreCase("D") && DefaultSetting.printLevelD) {
            Log.d(DefaultSetting.WTAG,tmp);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(instance, tmp, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if(level.equalsIgnoreCase("I") && DefaultSetting.printLevelI) {
            Log.d(DefaultSetting.WTAG,tmp);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(instance, tmp, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    View.OnLongClickListener fullScreenListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            int number = -1;

            switch (v.getId()) {
                case R.id.addBtn1:
                    number = 0;
                    break;
                case R.id.addBtn2:
                    number = 1;
                    break;
                case R.id.addBtn3:
                    number = 2;
                    break;
                case R.id.addBtn4:
                    number = 3;
                    break;
                case R.id.rmBtn1:
                    number = 0;
                    break;
                case R.id.rmBtn2:
                    number = 1;
                    break;
                case R.id.rmBtn3:
                    number = 2;
                    break;
                case R.id.rmBtn4:
                    number = 3;
                    break;
            }

            if (fullScreenNumber == -1) {
                for (int i = 0; i < 4; i++) {
                    if (i == number) {
                        rLayouts[i].setVisibility(View.VISIBLE);
                        fullScreenNumber = number;

                        if (i > 1) {
                            lLayouts[0].setLayoutParams(hidingLinear);
                        } else {
                            lLayouts[1].setLayoutParams(hidingLinear);
                        }

                    } else {
                        videoRecvCallbacks[i].setRender(false);
                        rLayouts[i].setVisibility(View.INVISIBLE);
                        rLayouts[i].setLayoutParams(hidingLinear);
                        videoSurfaceViews[i].setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
                    }
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    rLayouts[i].setVisibility(View.VISIBLE);
                    rLayouts[i].setLayoutParams(normalLinear);
                    fullScreenNumber = -1;
                    videoRecvCallbacks[i].setRender(true);
                    videoSurfaceViews[i].setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                }
                for (int i = 0; i < 2; i++) {
                    lLayouts[i].setLayoutParams(normalLinear);
                }
            }
            return true;
        }
    };

    View.OnClickListener requestLiveView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isItAllReady()) {
                Toast.makeText(instance, "please wait for connect", Toast.LENGTH_SHORT).show();
                return;
            }

            String msg = "";
            String state = "";
            int number = -1;
            switch (v.getId()) {
                case R.id.addBtn1:
                    msg = "addbtn1";
                    state = "RUN";
                    number = 1;
                    break;
                case R.id.addBtn2:
                    msg = "addbtn2";
                    state = "RUN";
                    number = 2;
                    break;
                case R.id.addBtn3:
                    msg = "addbtn3";
                    state = "RUN";
                    number = 3;
                    break;
                case R.id.addBtn4:
                    msg = "addbtn4";
                    state = "RUN";
                    number = 4;
                    break;
                case R.id.rmBtn1:
                    msg = "rmBtn1";
                    state = "STOP";
                    number = 1;
                    break;
                case R.id.rmBtn2:
                    msg = "rmBtn2";
                    state = "STOP";
                    number = 2;
                    break;
                case R.id.rmBtn3:
                    msg = "rmBtn3";
                    state = "STOP";
                    number = 3;
                    break;
                case R.id.rmBtn4:
                    msg = "rmBtn4";
                    state = "STOP";
                    number = 4;
                    break;
            }
            showMenu(number);
        }
    };

    void changeState(String state, int number) {
        if (state.equalsIgnoreCase("RUN")) {
            addBtns[number].setVisibility(View.INVISIBLE);
            rmBtns[number].setVisibility(View.VISIBLE);
            channelsState[number] = "RUN";
        } else if (state.equalsIgnoreCase("STOP")) {
            addBtns[number].setVisibility(View.VISIBLE);
            rmBtns[number].setVisibility(View.INVISIBLE);
            fpsViews[number].setVisibility(View.INVISIBLE);
            channelsState[number] = "STOP";
        }
    }

    public void createLocalVideoSendingThread(int Stream_id, int onChannel, String path) {
        if (sendingThreads[onChannel - 1] == null) {
            showToast("D", "Create sending Thread, path : " + path + ",on Channel :" + onChannel);
            sendingThreads[onChannel - 1] = new SendingLocalVideoThread(mNice, Stream_id, onChannel, path);
            sendingThreads[onChannel - 1].start();
        }
    }

    public void createLiveViewSendingThread(int Stream_id, int onChannel, String ip) {
        if (sendingLiveThreads[onChannel - 1] == null) {
            showToast("D", "create live view thread");
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
            showToast("D","Stop sending Thread " + onChannel);
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
            showToast("D", "Stop sending Thread " + onChannel);
        }
    }

    public void showMenu(final int onChannel) {
        View v = View.inflate(this, R.layout.mode_list, null);
        final AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setView(v);
        final Dialog dialog = MyAlertDialog.show();
        //v.findViewById(R.id.menu_play).setVisibility(View.INVISIBLE);

        float density = metrics.density;
        if(density<1.0) {
            density = 1;
        }
        //dialog.getWindow().setLayout(300, 120);
        View.OnClickListener tmpListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state = "";
                switch (v.getId()) {
                    case R.id.menu_camera:
                        Toast.makeText(instance, "camera on [" + onChannel + "]", Toast.LENGTH_SHORT).show();
                        //state = "RUN";
                        showLiveViewDialog(onChannel);

                        break;
//                    case R.id.menu_play:
//                        Toast.makeText(instance, "play on [" + onChannel + "]", Toast.LENGTH_SHORT).show();
//                        state = "RUN";
//                        msgChannel.sendMessage("VIDEO:" + state + ":" + onChannel + ":"+"/mnt/sata/test1.mp4");
//
//                        break;
                    case R.id.menu_remove:
                        Toast.makeText(instance, "remove on [" + onChannel + "]", Toast.LENGTH_SHORT).show();
                        state = "STOP";
                        msgChannel.sendMessage("VIDEO:" + state + ":" + onChannel + ":");

                        break;
                }

                if (state.equalsIgnoreCase("STOP")) {
                    if (videoRecvCallbacks[onChannel - 1].isStart()) {
                        videoRecvCallbacks[onChannel - 1].setStop();
                    }

                    setLiveViewIdleByChannel(onChannel);

                }

                final String st = state;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeState(st, onChannel - 1);
                    }
                });
                dialog.dismiss();
            }
        };
        //v.findViewById(R.id.menu_play).setOnClickListener(tmpListener);
        if(videoRecvCallbacks[onChannel-1].isStart()) {
            v.findViewById(R.id.menu_camera).setAlpha(0.3f);
        } else {
            v.findViewById(R.id.menu_camera).setOnClickListener(tmpListener);
        }

        v.findViewById(R.id.menu_remove).setOnClickListener(tmpListener);
    }


    boolean checkVideoChannelStart(int index) {
        if(videoRecvCallbacks[index-1].isStart()) {
            return true;
        }
        return false;
    }

    void stopVideoOnChannel(final int index){
        videoRecvCallbacks[index-1].setStop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeState("STOP", index - 1);
            }
        });
    }

    void stopAllRecv() {
        for(int i=1;i<=4;i++) {
            if(checkVideoChannelStart(i)) {
                stopVideoOnChannel(i);
            }
        }
    }

    void resetNiceAgent() {
        mNice.restartStream(mStreamId);
        localSdp = mNice.getLocalSdp(mStreamId);
        remoteSdp = "";
    }


    private void showLiveViewDialog(final int channelIndex) {

        if (liveViewList.size() > 0) {
            final View v = View.inflate(this, R.layout.request_live_view1, null);
            GridView gv = (GridView) v.findViewById(R.id.camera_gridview);
            List<Item> item = new ArrayList<Item>();
            String[] itemStringArray = new String[liveViewList.size()];
            for (int i = 0; i < liveViewList.size(); i++) {
                if (liveViewList.get(i).getStatus().equals("IDLE")) {
                    Item n = new Item(liveViewList.get(i).getName());
                    item.add(n);
                }
            }
            final List<Item> finalItem = item;

            ItemArrayAdapter itemArrayAdapter = new ItemArrayAdapter(this, R.layout.view_camera, item);
            gv.setNumColumns(liveViewList.size());
            gv.setAdapter(itemArrayAdapter);

            final AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);

            MyAlertDialog.setTitle("Live View List")
                    .setView(v);

            final AlertDialog dialog1 = MyAlertDialog.create();
            dialog1.show();
            //dialog1.getWindow().setLayout((int) metrics.density * liveViewList.size() * 110 + 50, (int) metrics.density * 220 + 50);


            gv.setOnItemClickListener(new GridView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    msgChannel.sendMessage("LiveView:RUN:"+finalItem.get(position).getName()+":"+channelIndex);
                    setLiveViewStatusByName(finalItem.get(position).getName(), channelIndex + "");
                    dialog1.dismiss();
                    final String st = "RUN";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeState(st, channelIndex - 1);
                        }
                    });
                }
            });

//
//
//            gv.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
//                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                    msgChannel.sendMessage("LiveView:RUN:"+finalItem.get(position).getName()+":"+channelIndex);
//                    setLiveViewStatusByName(finalItem.get(position).getName(), channelIndex + "");
//                    dialog1.dismiss();
//                    final String st = "RUN";
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            changeState(st, channelIndex - 1);
//                        }
//                    });
//
//
//                    return true;
//                }
//            });
        } else {
            Toast.makeText(MainActivity.this, "It has no Live View", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean setLiveViewStatusByName(String n,String s) {
        for(int i=0;i<liveViewList.size();i++) {
            if(liveViewList.get(i).getName().equalsIgnoreCase(n)) {
                liveViewList.get(i).setStatus(s);
                return true;
            }
        }
        return false;
    }

    public boolean setLiveViewIdleByChannel(int channelIndex) {
        for(int i=0;i<liveViewList.size();i++) {
            if(liveViewList.get(i).getStatus().equalsIgnoreCase(channelIndex+"")) {
                liveViewList.get(i).setStatus("IDLE");
                return true;
            }
        }
        return false;
    }


    void showOccupyMessage() {
        mProgressBar.dismiss();
        //final View v = View.inflate(this, R.layout.message, null);
        new AlertDialog.Builder(this)
                .setTitle("Source Peer is occupied")
                .setMessage("Do you wanna force reset source peer and try again?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSocket.emit("force get sdp", mFindPeerName);
                        mProgressBar.show();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                disableReady();
                                mNice.restartStream(mStreamId);
                                localSdp = mNice.getLocalSdp(mStreamId);
                                mSocket.emit("get remote sdp",mFindPeerName+":"+localSdp);
                            }
                        },2000);
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
    void showUndefinedMessage() {
        mProgressBar.dismiss();
        //final View v = View.inflate(this, R.layout.message, null);
        new AlertDialog.Builder(this)
                .setTitle("Source Peer status is undefined")
                .setMessage("Do you wanna force reset source peer and try again?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSocket.emit("force get sdp", mFindPeerName);
                        mProgressBar.show();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mNice.restartStream(mStreamId);
                                localSdp = mNice.getLocalSdp(mStreamId);
                                mSocket.emit("get remote sdp",mFindPeerName+":"+localSdp);
                            }
                        },2000);

                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
    void showOfflineMessage() {
        mProgressBar.dismiss();

        new AlertDialog.Builder(this)
                .setTitle("Source Peer is offline")
                .setMessage("You need to check your source peer network status")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //mSocket.emit("force get sdp", mFindPeerName);
                    }
                }).show();
    }


    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

}
