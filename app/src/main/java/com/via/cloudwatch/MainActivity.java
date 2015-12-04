package com.via.cloudwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.media.MediaPlayer;
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
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.utils.CommunicationChannel;
import com.utils.DefaultSetting;
import com.utils.LiveViewInfo;
import com.utils.QueryToServer;
import com.utils.SendingLiveViewThread;
import com.utils.SendingLocalVideoThread;
import com.utils.VideoRecvCallback;
import com.utils.Item;
import com.utils.ItemArrayAdapter;
import com.via.libnice;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private CharSequence mTitle;

    libnice mNice = null;
    int mStreamId = -1;

    SurfaceView[] videoSurfaceViews = new SurfaceView[4];
    ImageButton[] addBtns = new ImageButton[4];
    ImageButton[] rmBtns = new ImageButton[4];
    RelativeLayout[] rLayouts = new RelativeLayout[4];
    LinearLayout[] lLayouts = new LinearLayout[2];
    SendingLocalVideoThread[] sendingThreads = new SendingLocalVideoThread[4];
    SendingLiveViewThread[] sendingLiveThreads = new SendingLiveViewThread[4];
    VideoRecvCallback[] videoRecvCallbacks = new VideoRecvCallback[4];
    DisplayMetrics metrics = null;
    boolean[] isAllReadyTable = new boolean[5];
    boolean isAllReady = false;

    public ArrayList<LiveViewInfo> liveViewList = new ArrayList<LiveViewInfo>();

    boolean checkAllReady() {
        isAllReady = (isAllReadyTable[0] && isAllReadyTable[1] && isAllReadyTable[2] && isAllReadyTable[3] && isAllReadyTable[4]);
        if (isAllReady) {
            Toast.makeText(MainActivity.this, "It has been connected!", Toast.LENGTH_LONG).show();
        }
        if (bClient && isAllReady) {
            msgChannel.sendMessage(CommunicationChannel.REQUEST_LIVE_VIEW_INFO);
        }

        return isAllReady;
    }

    @Override
    protected void onPause() {
//        if (mediaplayer != null) {
//            mediaplayer.release();
//            mediaplayer = null;
//        }
        super.onPause();
    }

    boolean bClient = false;
    boolean bSource = false;

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


        for (int i = 0; i < 4; i++) {
            addBtns[i].setOnClickListener(requestLiveView);
            rmBtns[i].setOnClickListener(requestLiveView);
            addBtns[i].setOnLongClickListener(fullScreenListener);
            rmBtns[i].setOnLongClickListener(fullScreenListener);
        }

    }

    MainActivity instance = this;
    String localSdp = null;
    Handler handler = new Handler();
    CommunicationChannel msgChannel = null;

    P2PThread p2pthread = null;
    int reliableMode = 1;

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
            mNice.setStunAddress("74.125.204.127", 19302);
            mNice.setControllingMode(0);
            mStreamId = mNice.addStream("HankWu", 5);

            // Component 1 is using for message transfer
            int forComponentIndex = 1;
            msgChannel = new CommunicationChannel(instance, mNice, mStreamId, forComponentIndex);
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
                public void cbComponentStateChanged(final int i, final int i1, final int i2) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready") || libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("failed")) {
                                Toast.makeText(instance, "Stream[" + i + "]Component[" + i1 + "]:" + libnice.StateObserver.STATE_TABLE[i2], Toast.LENGTH_SHORT).show();
                                if (bClient && libnice.StateObserver.STATE_TABLE[i2].equalsIgnoreCase("ready")) {
                                    isAllReadyTable[i1 - 1] = true;
                                    checkAllReady();
                                }
                            }
                        }
                    });
                }
            });
            localSdp = mNice.getLocalSdp(mStreamId);

            // use for measure the size of AlertDialog.
            metrics = getResources().getDisplayMetrics();
        } else {

            p2pthread = new P2PThread();
            p2pthread.start();
        }
    }

    Fragment currentFragment = null;

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (position == 0) {
            if (currentFragment != null) {
                fragmentManager.beginTransaction().remove(currentFragment).commit();
                mTitle = getString(R.string.title_section1);
            }
        } else {
            currentFragment = PlaceholderFragment.newInstance(position + 1);
            fragmentManager.beginTransaction()
                    .replace(R.id.container2, currentFragment)
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

    MediaPlayer mediaplayer = null;
    int serverClickCounter = 0;

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

            final MainActivity tmp = (MainActivity) getActivity();

            rootView.findViewById(R.id.connectServerBtn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(tmp.serverClickCounter!=0) {
                        tmp.mNice.restartStream(tmp.mStreamId);
                        tmp.localSdp = tmp.mNice.getLocalSdp(tmp.mStreamId);
                    }

                    tmp.serverClickCounter++;
                    new Thread(tmp.registerTask).start();
                    tmp.handler.postDelayed(tmp.serverTask, 1000);
                }
            });
            rootView.findViewById(R.id.connectClientBtn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tmp.bClient = true;
                    new Thread(tmp.clientTask).start();
                }
            });

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

    Runnable serverTask = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String method = "Server";
                        String postParameters = "register=" + URLEncoder.encode("FALSE", "UTF-8")
                                + "&username=" + URLEncoder.encode("HankWu", "UTF-8");
                        String getSdp = QueryToServer.excutePost(method, postParameters);
                        if (getSdp.startsWith("NOBODY")) {
                            //showToast("No Remote SDP");
                            handler.postDelayed(serverTask, 1000);

                        } else {
                            showToast("Get remote SDP " + getSdp);
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

    Runnable clientTask = new Runnable() {
        @Override
        public void run() {
            try {
                String method = "Client";
                String findusername = "HankWu";
                String postParameters = "findusername=" + URLEncoder.encode(findusername, "UTF-8") + "&SDP=" + URLEncoder.encode(localSdp, "UTF-8");
                String getSdp = QueryToServer.excutePost(method, postParameters);
                if (getSdp.equals("OFFLINE")) {
                    showToast(findusername + "is OFFLINE");
                } else {
                    showToast("Get remote SDP " + getSdp);
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

    public void showToast(final String tmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(instance, tmp, Toast.LENGTH_SHORT).show();
            }
        });
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    int fullScreenNumber = -1;

    LinearLayout.LayoutParams hidingLinear = new LinearLayout.LayoutParams(0, 0, 0.0f);
    LinearLayout.LayoutParams normalLinear = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, 1.0f);
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
            if (!isAllReady) {
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
        } else if (state.equalsIgnoreCase("STOP")) {
            addBtns[number].setVisibility(View.VISIBLE);
            rmBtns[number].setVisibility(View.INVISIBLE);
        }
    }

    public void createLocalVideoSendingThread(int Stream_id, int onChannel, String path) {
        if (sendingThreads[onChannel - 1] == null) {
            showToast("create Sending Thread");
            Log.d("hank", "Create sending Thread, path : "+path+",on Channel :"+onChannel);
            sendingThreads[onChannel - 1] = new SendingLocalVideoThread(mNice, Stream_id, onChannel, path);
            sendingThreads[onChannel - 1].start();
        }
    }

    public void createLiveViewSendingThread(int Stream_id, int onChannel, String ip) {
        if (sendingLiveThreads[onChannel - 1] == null) {
            showToast("create live view thread");
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
            showToast("Stop sending Thread " + onChannel);
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
            showToast("Stop sending Thread " + onChannel);
        }
    }

    public void showMenu(final int onChannel) {
        View v = View.inflate(this, R.layout.mode_list, null);
        final AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setView(v);
        final Dialog dialog = MyAlertDialog.show();
        float density = metrics.density;
        if(density<1.0) {
            density = 1;
        }
        dialog.getWindow().setLayout((int) density * 330, (int) density * 120);
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
                    case R.id.menu_play:
                        Toast.makeText(instance, "play on [" + onChannel + "]", Toast.LENGTH_SHORT).show();
                        state = "RUN";
                        msgChannel.sendMessage("VIDEO:" + state + ":" + onChannel + ":"+"/mnt/sata/test1.mp4");

                        break;
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
        v.findViewById(R.id.menu_play).setOnClickListener(tmpListener);
        v.findViewById(R.id.menu_camera).setOnClickListener(tmpListener);
        v.findViewById(R.id.menu_remove).setOnClickListener(tmpListener);
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
            dialog1.getWindow().setLayout((int) metrics.density * liveViewList.size() * 110 + 50, (int) metrics.density * 220 + 50);

            gv.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
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


                    return true;
                }
            });
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


}
