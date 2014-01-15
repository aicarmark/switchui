package com.motorola.firewall;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Window;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.motorola.firewall.FireWall.BlockLog;
import com.motorola.firewall.FireWall.Name;
import android.util.Log;
import android.app.ActionBar;
import android.app.ActionBar.Tab;

public class FireWallTab extends Activity {
    private static final String TAG = "FirewallTab";
    private ActionBar.Tab TAB_SETTING = null;
    private ActionBar.Tab TAB_BLACK_LIST = null;
    private ActionBar.Tab TAB_WHITE_LIST = null;
    private ActionBar.Tab TAB_FIREWALLLOG = null;
    
    private static final String CALL_LOG_COMPONENT = "com.motorola.firewall.CallLogListActivity";
    private static final String SMS_LOG_COMPONENT = "com.motorola.firewall.SmsLogListActivity";
    private static final String ACTION_WIDGET_VIP = "VipWidget";
    static final String EXTRA_IGNORE_STATE = "ignore-state";
    
    private SharedPreferences mPre;
    
     // Gesture support
     private static final int GESTURE_MIN_DISTANCE = 40;
     private static final int GESTURE_SLOPE_THRESHOLD = 2;
     private static final int INVALID = -1;
     private static final int PREV = 1;
     private static final int NEXT = 2;

     private String lastTab = null;

     private GestureDetector mSlideshowGestureDetector = null;
     private SlideGestureListener mSlideGestureListener = null;
     protected int mDirection = INVALID;
     private ArrayList<ActionBar.Tab> tabsArray = null;
     private ActionBar bar = null;
     
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        setContentView(R.layout.firewall_tab);
        mPre = PreferenceManager.getDefaultSharedPreferences(this);
        
        mSlideGestureListener = new SlideGestureListener();
        mSlideshowGestureDetector = new GestureDetector(this, mSlideGestureListener);

        TextView mTextView;
        ImageView mImageView;

        // Setup the tabs
        TAB_SETTING = bar.newTab()
                .setText(getString(R.string.setting_tab))//.setIcon(R.drawable.ic_tab_setting)
                .setTabListener(new TabListener<FireWallSetting>(this, "Setting", FireWallSetting.class));
        //TAB_SETTING.setCustomView(R.layout.tab_bar_item);
        //mTextView = (TextView) TAB_SETTING.getCustomView().findViewById(R.id.tab_text);
        //mTextView.setText(R.string.setting_tab);
        //mImageView = (ImageView) TAB_SETTING.getCustomView().findViewById(R.id.tab_icon);
        //mImageView.setImageResource(R.drawable.ic_tab_setting);

        TAB_BLACK_LIST = bar.newTab()
                .setText(getString(R.string.black_list_tab))//.setIcon(R.drawable.ic_tab_blacklist)
                .setTabListener(new TabListener<BlackNameListFragment>(this, "BlackNameList", BlackNameListFragment.class));
        //TAB_BLACK_LIST.setCustomView(R.layout.tab_bar_item);
        //mTextView = (TextView) TAB_BLACK_LIST.getCustomView().findViewById(R.id.tab_text);
        //mTextView.setText(R.string.black_list_tab);
        //mImageView = (ImageView) TAB_BLACK_LIST.getCustomView().findViewById(R.id.tab_icon);
        //mImageView.setImageResource(R.drawable.ic_tab_blacklist);

        TAB_WHITE_LIST = bar.newTab()
                .setText(getString(R.string.white_list_tab))//.setIcon(R.drawable.ic_tab_whitelist)
                .setTabListener(new TabListener<WhiteNameListFragment>(this, "WhiteNameList", WhiteNameListFragment.class));
        //TAB_WHITE_LIST.setCustomView(R.layout.tab_bar_item);
        //mTextView = (TextView) TAB_WHITE_LIST.getCustomView().findViewById(R.id.tab_text);
        //mTextView.setText(R.string.white_list_tab);
        //mImageView = (ImageView) TAB_WHITE_LIST.getCustomView().findViewById(R.id.tab_icon);
        //mImageView.setImageResource(R.drawable.ic_tab_whitelist);

        String logtype = getIntent().getType();
        TAB_FIREWALLLOG = bar.newTab()
                .setText(getString(R.string.firewall_log_tab))//.setIcon(R.drawable.ic_tab_call_log)
                .setTabListener(new TabListener<FirewallLogListActivity>(this, "FirewallLogListActivity", FirewallLogListActivity.class));
        //TAB_FIREWALLLOG.setCustomView(R.layout.tab_bar_item);
        //mTextView = (TextView) TAB_FIREWALLLOG.getCustomView().findViewById(R.id.tab_text);
        //mTextView.setText(R.string.firewall_log_tab);
        //mImageView = (ImageView) TAB_FIREWALLLOG.getCustomView().findViewById(R.id.tab_icon);
        //mImageView.setImageResource(R.drawable.ic_tab_call_log);

        bar.addTab(TAB_SETTING);
        bar.addTab(TAB_BLACK_LIST);
        bar.addTab(TAB_WHITE_LIST);
        bar.addTab(TAB_FIREWALLLOG);

        tabsArray = new ArrayList<ActionBar.Tab>(4);
        tabsArray.add(TAB_SETTING);
        tabsArray.add(TAB_BLACK_LIST);
        tabsArray.add(TAB_WHITE_LIST);
        tabsArray.add(TAB_FIREWALLLOG);

        if (icicle != null) {
            bar.setSelectedNavigationItem(icicle.getInt("tabSelected", 0));
        } else {
            setFocusedTab(getIntent());
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tabSelected", getActionBar().getSelectedNavigationIndex());
    }

    
    @Override
    public void onNewIntent(Intent newIntent) {
        setFocusedTab(newIntent);
    }
    
    /**
     * Sets the current tab based on the intent's request type
     *
     * @param recentCallsRequest true is the recent calls tab is desired, false otherwise
     */
    private void setFocusedTab(Intent intent) {

        // Dismiss menu provided by any children activities
        final boolean LogRequest = BlockLog.CONTENT_TYPE.equals(intent.getType());
        if (LogRequest) {
            mPre.edit().putInt("log_type", 0).commit();
        }
        // Tell the children activities that they should ignore any possible saved
        // state and instead reload their state from the parent's intent
        intent.putExtra(EXTRA_IGNORE_STATE, true);

        // Choose the tab based on the inbound intent
        String componentName = intent.getComponent().getClassName();

        String action = intent.getAction();
       if (getClass().getName().equals(componentName)) {
            if (action!= null && action.equals(ACTION_WIDGET_VIP))
                TAB_WHITE_LIST.select();
        	else if ( LogRequest ) {
                TAB_FIREWALLLOG.select();
            } else {
                String blockType = intent.getStringExtra("blocktype");
                if(Name.WHITELIST.equals(blockType)){
                    TAB_WHITE_LIST.select();
                } else if(Name.BLACKLIST.equals(blockType)){
                    TAB_BLACK_LIST.select();
                } else {
                    //remain the status before
                    //TAB_SETTING.select();
                }
            }
        }

        // Tell the children activities that they should honor their saved states
        // instead of the state from the parent's intent
        intent.putExtra(EXTRA_IGNORE_STATE, false);
    }

    private class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Class<T> mClass;
        private final Activity mActivity;
        private Fragment mFragment;
        private final String mTag;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            mTag = tag;
            mActivity = activity;
            mClass = clz;
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }

        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), null);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            //Toast.makeText(FireWallTab.this, "Reselected!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean superResult = mSlideshowGestureDetector.onTouchEvent(ev);
        if (! superResult) {
            superResult = super.dispatchTouchEvent(ev);
        }
        return superResult;
     }
     
    // Inner class
    private class SlideGestureListener implements
            GestureDetector.OnGestureListener {
        public boolean onDown(MotionEvent e) {
            mDirection = INVALID;            
            return false;
        }
 
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX,
               float deltaY) {

            boolean ret = false;
            if ((null == e1)||(null == e2)) {
                return ret;
            }
            int distanceX = (int) e1.getX() - (int) e2.getX();
            int distanceY = (int) e1.getY() - (int) e2.getY();
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);            

            int currentDirection = INVALID;
            if (absDistanceX > GESTURE_MIN_DISTANCE) {
                currentDirection = distanceX > 0 ? NEXT : PREV;
                // check the slope of gesture movement. if x distance is smaller
                // than y distance times GESTURE_SLOPE_THRESHOD, the slope is
                // too
                // steep, and the scroll may not mean to switch slide.
                if (absDistanceX < GESTURE_SLOPE_THRESHOLD * absDistanceY) {
                    currentDirection = INVALID;                    
                }

                if (currentDirection != INVALID) {
                    if (currentDirection != mDirection) {  
                        ActionBar.Tab currentTab = bar.getSelectedTab();
                        int iCurrentTabIndex = tabsArray.indexOf(currentTab);
                        switch (currentDirection) {
                            case NEXT:
                                //if(iCurrentTabIndex < (bar.getTabCount() - 1))
                                tabsArray.get((iCurrentTabIndex + 1) % 4).select();
                                ret = true;
                                break;
                            case PREV:
                                //if(iCurrentTabIndex > 0)
                                tabsArray.get((iCurrentTabIndex + 3) % 4).select();
                                ret = true;
                                break;
                            default:
                                break;
                        }
                        mDirection = currentDirection;
                    }
                }
            }
            return ret;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return false;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
            return;
        }

        public void onShowPress(MotionEvent e) {
            return;
        }
    }
}

