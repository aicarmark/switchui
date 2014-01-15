/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                       04/26/2012                  Initial release 
 *         
 */

package com.motorola.mmsp.performancemaster.ui;

import com.motorola.mmsp.performancemaster.engine.CPUInfo;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.CPUInfo.ProcessInfo;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CpuRunningApplist extends Activity implements View.OnClickListener,
        OnItemClickListener {

    private class AppInfoDisplay {
        private Drawable icon; // icon

        private String strApplicationName; // label

        private float fCpuUsage; // process CPU usage

        private String strPackageName; // the package name of this process

        private AppInfoDisplay() {
            this.strApplicationName = "";
            this.strPackageName = "";
            this.fCpuUsage = 0.0f;
            this.icon = null;
        }
    }
    private static final String TAG = "CpuRunningApplist";

    private static final String ACTION_NAME_SDK_VERSION_HIGH_THAN_2_2 = "android.settings.APPLICATION_DETAILS_SETTINGS";

    private static final String ACTION_NAME_SDK_VERSION_EQUAL_2_2 = "pkg";

    private static final String ACTION_NAME_SDK_VERSION_LOW_THAN_2_2 = "com.android.settings.ApplicationPkgName";

    private static final String SCHEME_NAME_SDK_VERSION_HIGH_THAN_2_2 = "package";

    private static final String PACKAGE_NAME_SDK_VERSION_LOW_THAN_2_2 = "com.android.settings";

    private static final String CLASS_NAME_SDK_VERSION_LOW_THAN_2_2 = "com.android.settings.InstalledAppDetails";

    private static final int MSG_UPDATE_PROCESS_INFO = 0x100;

    private final static int DISPLAY_APP_DETAILS_ACTIVITY_REQUEST_CODE = 100;

    private ListView mListView;
    
    private TextView mTextViewNoProcess;

    private ArrayAdapter<AppInfoDisplay> mAaProcess;

    private ArrayList<ProcessInfo> mAllProcessInfo = null;

    private MainTabHost mTabHost;

    private static final String[] mTabTag = {
            "UserProcess", "SysProcess"
    };

    private SysPropEngine mEngine = null;

    private CPUInfo mCI = null;

    private ProgressDialog mDialog = null;

    public void onCreate(Bundle savedInstanceState) {
        // create view
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.cpu_running_app_layout);
        
        //init action bar
        initActionBar();

        // init resource
        initResource();

        // init adapter
        initAdapter();

        // init tabhost
        initTabHost();

        // init engine
        regListener();
    }

    @Override
    protected void onResume() {
        // set read application process information
        mCI.setReadAppProcess(true);

        super.onResume();
    }

    protected void onPause() {
        // remove msg
        mHandler.removeCallbacksAndMessages(MSG_UPDATE_PROCESS_INFO);

        // update read application process information
        mCI.setReadAppProcess(false);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unRegListener();
        super.onDestroy();
    }

    public void onClick(View v) {

       switch (v.getId()) {
           case R.id.menu_item: {
                this.finish();
                break;
            }
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfoDisplay da = (AppInfoDisplay) parent.getItemAtPosition(position);

        handleItemClick(da.strPackageName);
    }

    private InfoListener mCpuInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            synchronized (mCI) {
                // we update listview when process info list has changed.
                if (mCI.isProcessInfoUpdated()) {
                    mAllProcessInfo = mCI.getmAllProcessInfo();
                    Message msg = new Message();
                    msg.what = MSG_UPDATE_PROCESS_INFO;
                    mHandler.sendMessage(msg);

                    Log.e(TAG, " UI: update list display");
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (CpuRunningApplist.DISPLAY_APP_DETAILS_ACTIVITY_REQUEST_CODE == requestCode) {
            Log.e(TAG, "return from activity");
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
        
    private void initActionBar()
    {
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel < 11 )
        {
            Log.e(TAG, "initActionBar() return");
            return;
        }
        
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar 
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.custom_actionbar_layout, null);
            
            ((View)customActionBarView.findViewById(R.id.menu_item)).setOnClickListener(this); 
            TextView textView = (TextView)customActionBarView.findViewById(R.id.tv_title);
            textView.setText(getResources().getString(R.string.str_action_bar_back_to_cpu));
            
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE );
            actionBar.setCustomView(customActionBarView);
        }
    }

    private void regListener() {
        // register CpuInfoListener and init cpu usage array
        SysPropEngine.setContext(getApplicationContext());
        
        mEngine = SysPropEngine.getInstance();
        mCI = mEngine.getCpuInfo();
        
        mCI.registerListener(mCpuInfoListener);
    }

    private void unRegListener() {
        // unRegister CpuInfoListener
        mCI.unregisterListener(mCpuInfoListener);
        mCI = null;
        mEngine = null;
    }

    private void initResource() {
        // init list adapter
        mListView = (ListView) findViewById(R.id.lv_process);
        mTabHost = (MainTabHost) findViewById(R.id.tabhost);

        mListView.setCacheColorHint(0);
        mListView.setOnItemClickListener(this);
        
        mTextViewNoProcess = (TextView)findViewById(R.id.tv_no_process);
    }

    private void initAdapter() {
        mAaProcess = new ArrayAdapter<AppInfoDisplay>(this, R.layout.cpu_running_app_list) {

            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View mListItemView;
                ImageView mIcon;
                TextView mName, mUsage;
                ProgressBar mPBUsage;

                if (convertView == null) {
                    mListItemView = CpuRunningApplist.this.getLayoutInflater().inflate(
                            R.layout.cpu_running_app_list, parent, false);
                } else {
                    mListItemView = convertView;
                }

                if (position >= getCount()) {
                    return mListItemView;
                }

                // update value
                AppInfoDisplay daItem = getItem(position);

                mIcon = (ImageView) mListItemView.findViewById(R.id.iv_application);
                mIcon.setImageDrawable(daItem.icon);

                mName = (TextView) mListItemView.findViewById(R.id.tv_process_name);
                mName.setText(daItem.strApplicationName);

                mUsage = (TextView) mListItemView.findViewById(R.id.tv_cpu_usage);
                mUsage.setText(daItem.fCpuUsage + "%");

                mPBUsage = (ProgressBar) mListItemView.findViewById(R.id.pb_cpu_usage);
                mPBUsage.setMax(100);
                mPBUsage.setProgress((int) daItem.fCpuUsage);

                return mListItemView;
            }
        };
    }

    private void initTabHost() {
        mTabHost.setup();

        TabHost.TabContentFactory tabContentFactory = new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                mListView.setAdapter(mAaProcess);
                mListView.setTextFilterEnabled(true);

                return mListView;
            }
        };

        mTabHost.addTab(mTabHost.newTabSpec(mTabTag[0])
                .setIndicator(getResources().getText(R.string.str_usr_process))
                .setContent(tabContentFactory));

        mTabHost.addTab(mTabHost.newTabSpec(mTabTag[1])
                .setIndicator(getResources().getText(R.string.str_sys_process))
                .setContent(tabContentFactory));

        mTabHost.setCurrentTab(0);

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

            public void onTabChanged(String tabId) {
                if (tabId != null) {

                    if ((mAllProcessInfo != null) && (mAllProcessInfo.size() > 0)) {
                        displayProcessInListView();
                    } else {
                        createIndeterminateDialog();
                    }
                }
            }
        });

        // there is nothing in adapter, should display ProgressDialog
        createIndeterminateDialog();

    }

    private void createIndeterminateDialog() {
    	if (mDialog == null){
            mDialog = new ProgressDialog(this);
            mDialog.setMessage(getResources().getString(R.string.str_dialog_message));
            mDialog.setIndeterminate(true);        
            mDialog.setCancelable(false);
            mDialog.show();
    	}    	
    }

    private void destoryIndeterminateDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROCESS_INFO: {
                    displayProcessInListView();
                    break;
                }

                default:
                    break;
            }
        }
    };

    private void displayProcessInListView() {

        destoryIndeterminateDialog();

        mAaProcess.clear();
        mAaProcess.setNotifyOnChange(false);

        for (int nCount = 0; nCount < mAllProcessInfo.size(); nCount++) {
            ProcessInfo pi = mAllProcessInfo.get(nCount);
            AppInfoDisplay da = new AppInfoDisplay();
            da.icon = pi.icon;
            da.strApplicationName = pi.strApplicationName;
            da.fCpuUsage = pi.fProcessUsage;
            da.strPackageName = pi.strPackageName;

            if ((mTabTag[0].equals(mTabHost.getCurrentTabTag()) && !pi.bSystemProcess)
                    || (mTabTag[1].equals(mTabHost.getCurrentTabTag()) && pi.bSystemProcess)) {
                mAaProcess.add(da);
            }
        }
        
        //there is no process, display info to user
        if (mAaProcess.getCount() == 0)
        {
            findViewById(android.R.id.tabcontent).setVisibility(View.GONE);
            mListView.setVisibility(View.GONE);
            
            findViewById(R.id.ll_no_process).setVisibility(View.VISIBLE);
            mTextViewNoProcess.setText(getResources().getString(R.string.str_no_process));
        }
        else {
            findViewById(R.id.ll_no_process).setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);  
            findViewById(android.R.id.tabcontent).setVisibility(View.VISIBLE);
        }
        mAaProcess.notifyDataSetChanged();

        Log.e(TAG, " displayProcessInListView, count=" + mAaProcess.getCount() + ", tag="
                + mTabHost.getCurrentTabTag());
    }

    private void handleItemClick(String strPkgName) {
        Intent intent = new Intent();

        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= 9) { // phone version is 2.3
            intent.setAction(ACTION_NAME_SDK_VERSION_HIGH_THAN_2_2);
            Uri uri = Uri.fromParts(SCHEME_NAME_SDK_VERSION_HIGH_THAN_2_2, strPkgName, null);
            intent.setData(uri);
        } else {
            final String appPkgName = (apiLevel == 8 ? (ACTION_NAME_SDK_VERSION_EQUAL_2_2)
                    : (ACTION_NAME_SDK_VERSION_LOW_THAN_2_2));

            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName(PACKAGE_NAME_SDK_VERSION_LOW_THAN_2_2,
                    CLASS_NAME_SDK_VERSION_LOW_THAN_2_2);
            intent.putExtra(appPkgName, strPkgName);
        }

        List<ResolveInfo> acts = getPackageManager().queryIntentActivities(intent, 0);

        if (acts.size() > 0) {
            startActivityForResult(intent,
                    CpuRunningApplist.DISPLAY_APP_DETAILS_ACTIVITY_REQUEST_CODE);
        } else {
            Log.e(TAG, " Failed to start setting activity, " + acts.size());
        }
    }

}
