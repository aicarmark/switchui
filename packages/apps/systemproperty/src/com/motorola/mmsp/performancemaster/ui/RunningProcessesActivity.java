/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.mmsp.performancemaster.ui;

import android.app.ActionBar;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.content.pm.ApplicationInfo;

import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.text.Html;

import android.text.format.Formatter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import android.view.ViewGroup;

import android.widget.BaseAdapter;

import android.widget.AdapterView;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.ArrayList;

import java.util.List;


import com.motorola.mmsp.performancemaster.engine.MemInfo;
import com.motorola.mmsp.performancemaster.engine.RunningStateInfo;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.engine.RunningStateInfo.MergedItem;
import com.motorola.mmsp.performancemaster.R;

public class RunningProcessesActivity extends Activity
        implements OnClickListener {
    protected static final int MSG_MEMINFO_UPDATE = 0x102;
    protected static final int MSG_PROCESS_UPDATE = 0x103;
    private final static int GREEN = 1;
    private RunningStateInfo mRunningStateInfo = null;
    private SysPropEngine engine = null;
    private MemInfo mMemInfo = null;
    private PercentView mRamPercentView;
    private ArrayList<RunningStateInfo.MergedItem> mCurrentBackgroundItems = null,
            mCurrentMergedItems = null;

    private long mRamTotal, mRamFree;

    private static boolean mListCheck = true;

    private ListView mListView = null, mUserListView = null, mSystemListView = null;

    private TabHost mTabHost;
    private ServiceListAdapter mServiceListAdapter; // the stay-process
    // listadapter
    private UserListAdapter mUserListAdapter; // userlist adapter
    private SystemListAdapter mSystemListAdapter; // system listadapter
    private boolean isRunningProcess;

    private TextView mNoAppTextView;

    private Button mClearAPPButton, mSelectButton;
    private ArrayList<RunningStateInfo.MergedItem> mSystemItems = new ArrayList<MergedItem>();
    private ArrayList<RunningStateInfo.MergedItem> mUserItems = new ArrayList<MergedItem>();

    private final static String TAG = "RunningProcessesActivity";

    private Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case RunningProcessesActivity.MSG_MEMINFO_UPDATE:

                    synchronized (mMemInfo) {
                        mRamTotal = mMemInfo.getRamTotal();
                        mRamFree = mMemInfo.getRamFree();
                    }

                    mRamTotal = mRamTotal * 1024;
                    mRamFree = mRamFree * 1024;

                    Log.e(TAG, " RunningProcessesActivity MSG_MEMINFO_UPDATE mRamFree =" + mRamFree);
                    refreshHeadUi(isRunningProcess);
                    ramviewSetText();

                    break;

                case RunningProcessesActivity.MSG_PROCESS_UPDATE:
                    Log.e(TAG, " RunningProcessesActivity MSG_PROCESS_UPDATE ");

                    mCurrentBackgroundItems = mRunningStateInfo.getCurrentBackgroundItems();
                    mCurrentMergedItems = mRunningStateInfo.getCurrentMergedItems();

                    View mlistprogressbar = (View) findViewById(R.id.listprogressbar);
                    mlistprogressbar.setVisibility(View.GONE);

                    onRefreshUi();
                    ramviewSetText();

                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }
    };

    private void ramviewSetText() {

        String mram_free_total = getResources().getString(R.string.ram_free_total,
                Formatter.formatShortFileSize(this, mRamFree),
                Formatter.formatShortFileSize(this, mRamTotal));

        TextView mramclickTextView = (TextView) findViewById(R.id.ramfreetotalbig);
        mramclickTextView.setText(mram_free_total);

        long mUsedPerText = ((mRamTotal - mRamFree) * 100)
                / mRamTotal;
        long mPerView = ((mRamTotal - mRamFree) * 360) / mRamTotal;
        mRamPercentView.setUsedPerText(mPerView, mUsedPerText + "%");
    }

    private InfoListener mMemInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();
            msg.what = RunningProcessesActivity.MSG_MEMINFO_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

    private InfoListener mRunningStateInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();
            msg.what = RunningProcessesActivity.MSG_PROCESS_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

    private void regRunningStateInfoInit() {
        SysPropEngine.setContext(getApplicationContext());
        // MemInfo update
        engine = SysPropEngine.getInstance();

        mMemInfo = (MemInfo) engine.getMemInfo();

        mRunningStateInfo = (RunningStateInfo) engine.getRunningStateInfo();
    }

    OnCheckedChangeListener checkListener = new OnCheckedChangeListener() {

        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked)
        {
            int i = 0;
            int mListSize = mListView.getCount();
            ((RunningStateInfo.MergedItem) mListView.getItemAtPosition((Integer) buttonView
                    .getTag())).isChecked = isChecked;

            Log.e(TAG,
                    "onCheckedChanged 111 mListSize = " + mListSize + " buttonView.getTag()"
                            + (Integer) buttonView.getTag() + " isChecked =" + isChecked);

            for (i = 0; i < mListSize; i++) {
                if (!((RunningStateInfo.MergedItem) mListView.getItemAtPosition(i)).isChecked)
                    break;
            }
            Log.e(TAG,
                    "onCheckedChanged mListSize = " + mListSize + " i ="
                            + i);
            if (i == mListSize) {
                Button mView = (Button) findViewById(R.id.btn_sel_all);

                mView.setText(getString(R.string.deselect_all));
                mListCheck = true;

            }

            for (i = 0; i < mListSize; i++) {
                if (((RunningStateInfo.MergedItem) mListView.getItemAtPosition((i))).isChecked)
                    break;
            }
            Log.e(TAG,
                    "onCheckedChanged 222 mListSize = " + mListSize + " i ="
                            + i);
            if (i == mListSize) {
                Button mView = (Button) findViewById(R.id.btn_sel_all);
                mListCheck = false;

                mView.setText(getString(R.string.select_all));

            }

            refreshHeadUi(isRunningProcess);
            ramviewSetText();
        }
    };

    /**
     * @param ai ask the setting to resolve the process
     */

    void handleAction(final RunningStateInfo.MergedItem mMergedItem) {
        String pkgName = mMergedItem.mPackageInfo.packageName;
        if(pkgName == null){
            Log.e(TAG,
                    "handleAction pkgName == null " );
            return;  
        }
           

        Log.e(TAG,
                "handleAction pkgName = " + pkgName );

        Intent it = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",
                Uri.fromParts("package", pkgName, null));

        List<ResolveInfo> acts = getPackageManager().queryIntentActivities(it, 0);

        if (acts.size() > 0) {
            startActivity(it);
        } else {
            Log.d(TAG, "Failed to resolve activity for InstalledAppDetails");
        }

    }

    public class ViewHolder {
        public View rootView;
        public ImageView icon;
        public TextView name;

        public TextView mSizeView;
        public TextView uptime;
        public boolean isChecked;

        public ViewHolder(View v, boolean mShowBackground, int position) {
            rootView = v;
            icon = (ImageView) v.findViewById(R.id.icon);
            name = (TextView) v.findViewById(R.id.name);

            mSizeView = (TextView) v.findViewById(R.id.size);

            isChecked = true;

            v.setTag(this);
        }

        public void bind(RunningStateInfo state, RunningStateInfo.BaseItem item) {

            if (item.mPackageInfo == null && item instanceof RunningStateInfo.MergedItem) {

                item.mPackageInfo = ((RunningStateInfo.MergedItem) item).mProcess.mPackageInfo;
                item.mDisplayLabel = ((RunningStateInfo.MergedItem) item).mProcess.mDisplayLabel;
            }
            name.setText(item.mDisplayLabel);

            if (item.mPackageInfo != null) {
                icon.setImageDrawable(item.mPackageInfo.loadIcon(getPackageManager()));
            }
            icon.setVisibility(View.VISIBLE);

            String size = item.mSizeStr != null ? item.mSizeStr : "";

            mSizeView.setText(size);

        }
    }

    class SystemListAdapter extends BaseAdapter {
        final RunningStateInfo mState;
        final LayoutInflater mInflater;
        boolean mShowBackground = true;

        // ArrayList<RunningStateInfo.MergedItem> mItems = new
        // ArrayList<MergedItem>();

        SystemListAdapter(RunningStateInfo state) {
            mState = state;
            mInflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

        }

        public int getCount() {
            return mSystemItems.size();
        }

        @Override
        public boolean isEmpty() {
            return mState.hasData() && mSystemItems.size() == 0;
        }

        public Object getItem(int position) {
            return mSystemItems.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent, position);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent, int position) {
            View v = mInflater.inflate(R.layout.running_processes_item, parent, false);
            new ViewHolder(v, mShowBackground, position);
            return v;
        }

        public void bindView(View view, int position) {
            Log.e(TAG, "SystemListAdapter bindView  position=  " + position + " mItems.size ="
                    + mSystemItems.size());

            if (position >= mSystemItems.size()) {
                // List must have changed since we last reported its
                // size... ignore here, we will be doing a data changed
                // to refresh the entire list.
                return;
            }
            ViewHolder vh = (ViewHolder) view.getTag();

            RunningStateInfo.MergedItem item = mSystemItems.get(position);

            vh.bind(mState, item);

        }
    }

    private void refreshItems() {
        synchronized (mRunningStateInfo.mLock) {
            if (mCurrentMergedItems == null) {
                Log.e(TAG, " refreshItems mCurrentMergedItems == null ");
                return;
            }

            Log.e(TAG, " refreshItems mUserItems()= " + mUserItems.size()
                    + " mCurrentMergedItems.size() =" + mCurrentMergedItems.size());
            if (mUserItems.size() > 0)
                mUserItems.clear();
            if (mSystemItems.size() > 0)
                mSystemItems.clear();

            for (int i = 0; i < mCurrentMergedItems.size(); i++) {

                Log.e(TAG, " refreshItems i= " + i
                        + " mProcess.mRunningProcessInfo."
                        + mCurrentMergedItems.get(i).mProcess.mProcessName);

                RunningStateInfo.MergedItem item = mCurrentMergedItems.get(i);
                
                Log.e(TAG, " refreshItems i= " + i
                        + " item.issystem = "
                        + item.issystem);

                if (!item.issystem)
                    mUserItems.add(item);
                else
                    mSystemItems.add(item);
            }
        }
    }

    class UserListAdapter extends BaseAdapter {
        final RunningStateInfo mState;
        final LayoutInflater mInflater;
        boolean mShowBackground = true;

        // ArrayList<RunningStateInfo.MergedItem> mItems = new
        // ArrayList<MergedItem>();

        UserListAdapter(RunningStateInfo state) {
            mState = state;
            mInflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

        }

        public int getCount() {
            Log.e(TAG, "UserListAdapter getCount  "
                    + " mUserItems.size() =" + mUserItems.size());
            return mUserItems.size();
        }

        @Override
        public boolean isEmpty() {
            Log.e(TAG, "UserListAdapter isEmpty mState.hasData()= " + mState.hasData()
                    + " mUserItems.size() =" + mUserItems.size());
            return mState.hasData() && mUserItems.size() == 0;
        }

        public Object getItem(int position) {
            return mUserItems.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent, position);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent, int position) {
            View v = mInflater.inflate(R.layout.running_processes_item, parent, false);
            new ViewHolder(v, mShowBackground, position);
            return v;
        }

        public void bindView(View view, int position) {
            Log.e(TAG, "UserListAdapter bindView  position=  " + position + " mUserItems.size ="
                    + mUserItems.size());

            if (position >= mUserItems.size()) {
                // List must have changed since we last reported its
                // size... ignore here, we will be doing a data changed
                // to refresh the entire list.
                return;
            }
            ViewHolder vh = (ViewHolder) view.getTag();

            RunningStateInfo.MergedItem item = mUserItems.get(position);

            vh.bind(mState, item);

        }
    }

    class ServiceListAdapter extends BaseAdapter {
        final RunningStateInfo mState;
        final LayoutInflater mInflater;
        boolean mShowBackground = true;
        ArrayList<RunningStateInfo.MergedItem> mItems = new ArrayList<MergedItem>();

        ServiceListAdapter(RunningStateInfo state) {
            mState = state;
            mInflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            refreshItems(false);
        }

        void refreshItems(boolean isEndPocess) {

            int appsize = 0;
            synchronized (mState.mLock) {
                if (mCurrentBackgroundItems == null)
                    return;
                appsize = mCurrentBackgroundItems.size();
            }
            Log.e(TAG, "ServiceListAdapter refreshItems  appsize=  " + appsize);

            if (appsize == 0) {

                mNoAppTextView.setText(getString(R.string.no_apps));
                mNoAppTextView.setVisibility(View.VISIBLE);
                mClearAPPButton.setEnabled(false);
                mSelectButton.setEnabled(false);

            } else {

                mNoAppTextView.setVisibility(View.GONE);
                mClearAPPButton.setEnabled(true);
                mSelectButton.setEnabled(true);

            }

            if (isEndPocess) {
                int mItemsize = mItems.size();
                for (int temp = 0; temp < mItemsize; temp++) {
                    if (mItems.get(temp).isChecked) {
                        mItems.remove(temp);
                        temp--;
                        mItemsize--;
                    }
                }
            } else {
                if (mItems.size() > 0)
                    mItems.clear();

                int itemsize = 0;
                int mBackgroudItemsize = 0;
                synchronized (mState.mLock) {
                    mBackgroudItemsize = mCurrentBackgroundItems.size();
                    for (int i = 0; i < mBackgroudItemsize; i++) {
                        RunningStateInfo.MergedItem mTempItem = mCurrentBackgroundItems.get(i);
                        mItems.add(mTempItem);
                        if (mTempItem.isChecked) {
                            itemsize++;
                        }

                    }
                }

                if (itemsize == mBackgroudItemsize) {
                    Button mView = (Button) findViewById(R.id.btn_sel_all);

                    mView.setText(getString(R.string.deselect_all));
                }

            }

        }

        public int getCount() {

            return mItems.size();
        }

        @Override
        public boolean isEmpty() {

            return mState.hasData() && mItems.size() == 0;
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent, position);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent, int position) {
            View v = mInflater.inflate(R.layout.running_processes_item, parent, false);
            new ViewHolder(v, mShowBackground, position);
            return v;
        }

        public void bindView(View view, int position) {
            Log.e(TAG, "ServiceListAdapter bindView  position=  " + position + " mItems.size ="
                    + mItems.size());

            if (position >= mItems.size()) {
                // List must have changed since we last reported its
                // size... ignore here, we will be doing a data changed
                // to refresh the entire list.
                return;
            }
            ViewHolder vh = (ViewHolder) view.getTag();
            CheckBox mCheckBox;

            RunningStateInfo.MergedItem item = mItems.get(position);

            mCheckBox = (CheckBox) view.findViewById(R.id.ckb_clearmem);
            mCheckBox.setTag(position);

            mCheckBox.setChecked(item.isChecked);
            mCheckBox.setClickable(true);
            mCheckBox.setOnCheckedChangeListener(checkListener);
            mCheckBox.setVisibility(View.VISIBLE);

            vh.bind(mState, item);

        }
    }

    /*
     * refresh the head
     */

    private void refreshHeadUi(boolean isRunningApp) {

        long tempsize = 0;
        for (int i = 0; i < mCurrentBackgroundItems.size(); i++) {
            RunningStateInfo.MergedItem item = mCurrentBackgroundItems.get(i);

            if (item.isChecked) {
                tempsize += item.mProcess.mSize;
            }
        }

        String sizeStr = Formatter.formatShortFileSize(
                this, tempsize);
        Log.e(TAG, "refreshHeadUi  sizeStr = " + sizeStr);

        StringBuilder m_ProcessStr = new StringBuilder();
        if (isRunningApp) {
            m_ProcessStr.append(getResources().getText(R.string.process_head_ram))
                    .append(": <font color='#33B5E5'>")
                    .append(String.valueOf(sizeStr) + "</font>")
                    .append(" ")
                    .append(getResources().getText(R.string.process_back_ram));
        } else {
            m_ProcessStr.append(getResources().getText(R.string.stay_resident_head));
        }

        TextView mbackTextView = (TextView) findViewById(R.id.rambacksize);

        mbackTextView.setText(Html.fromHtml(m_ProcessStr.toString()));
        /*
         * TextView mbackgroudappsbigView = (TextView)
         * findViewById(R.id.backgroudappsbig); String mbackgroudapps =
         * getResources().getString(R.string.backgroudapps,
         * mCurrentMergedItems.size());
         * mbackgroudappsbigView.setText(mbackgroudapps); TextView
         * mrunningappsbigView = (TextView) findViewById(R.id.runningappsbig);
         * String mrunningapps = getResources().getString(R.string.runningapps,
         * mCurrentBackgroundItems.size()); //
         * mrunningappsbigView.setText(mrunningapps);
         */
    }

    /*
     * isRunningProcess to refresh the list
     */

    void refreshListUi(boolean isRunningProcess) {
        if (isRunningProcess) {
            Log.e(TAG, "refreshListUi  refreshListUi dataChanged = " + isRunningProcess);

            mServiceListAdapter.refreshItems(false);
            
            mServiceListAdapter.notifyDataSetChanged();
            setListViewHeight(mListView);
            
        } else {
            Log.e(TAG, "refreshListUi  refreshListUi dataChanged = " + isRunningProcess);

            // mUserListAdapter.refreshItems();

            refreshItems();
            Log.e(TAG, "refreshListUi  mUserItems.size() = " + mUserItems.size());
            if (mUserItems.size() == 0)
            {
                View mView = (View) findViewById(R.id.user_process_title);
                mView.setVisibility(View.GONE);
                mView = (View) findViewById(R.id.userlist);
                mView.setVisibility(View.GONE);
                mUserListAdapter.notifyDataSetChanged();

                
            }else{
                mUserListAdapter.notifyDataSetChanged();
                setListViewHeight(mUserListView);
               
                
                
                View mView = (View) findViewById(R.id.user_process_title);
                mView.setVisibility(View.VISIBLE);
                mView = (View) findViewById(R.id.userlist);
                mView.setVisibility(View.VISIBLE);
                

            }
            Log.e(TAG, "refreshListUi  before  setListViewHeight(mSystemListView)  ");
            mSystemListAdapter.notifyDataSetChanged();
            setListViewHeight(mSystemListView);
            

        }

    }

    /*
     * set listview height to display the list
     */

    public  void setListViewHeight(ListView listView) {
        BaseAdapter listAdapter = (BaseAdapter) listView.getAdapter();
        int listAdaptersize = 0;
        if ((listAdapter == null) || ((listAdaptersize = listAdapter.getCount())== 0)) {
            return;
        }
        int totalHeight = 0;

        Log.e(TAG, "setListViewHeight mlistAdaptersize = " + listAdaptersize);
        
        View listItem = listAdapter.getView(0, null, listView);
        listItem.measure(0, 0);
        
        totalHeight = listItem.getMeasuredHeight()*listAdaptersize;
        Log.e(TAG, "setListViewHeight listItem.getMeasuredHeight() = " + listItem.getMeasuredHeight() + " totalHeight =" + totalHeight);
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        Log.e(TAG, "setListViewHeight listView.getDividerHeight() = " + listView.getDividerHeight());
        params.height = totalHeight + (listView.getDividerHeight() * (listAdaptersize - 1));
        Log.e(TAG, "setListViewHeight params.height = " + params.height);
        listView.setLayoutParams(params);
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.menu_item: {
                this.finish();
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.running_processes_view);

        // init action bar
        initActionBar();

        mListView = (ListView) findViewById(R.id.list);
        mUserListView = (ListView) findViewById(R.id.userlist);
        mSystemListView = (ListView) findViewById(R.id.systemlist);

        mNoAppTextView = (TextView) findViewById(R.id.textView_no_App);

        mSystemListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                RunningStateInfo.MergedItem mMergedItem = (RunningStateInfo.MergedItem) mSystemListView
                        .getItemAtPosition(position);
                handleAction(mMergedItem);

            }
        });
        mUserListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                RunningStateInfo.MergedItem mMergedItem = (RunningStateInfo.MergedItem) mUserListView
                        .getItemAtPosition(position);
                handleAction(mMergedItem);

            }
        });

        regRunningStateInfoInit();

        Log.e(TAG, "TabContentFactory  tabContentFactory  ");
        mServiceListAdapter = new ServiceListAdapter(mRunningStateInfo);
        mListView.setAdapter(mServiceListAdapter);

        mUserListAdapter = new UserListAdapter(mRunningStateInfo);
        mUserListView.setAdapter(mUserListAdapter);

        mSystemListAdapter = new SystemListAdapter(mRunningStateInfo);
        mSystemListView.setAdapter(mSystemListAdapter);

        isRunningProcess = true;

        mTabHost = (TabHost) findViewById(R.id.tabhost);
        mTabHost.setup();
        TabHost.TabContentFactory tabContentFactory = new TabHost.TabContentFactory() {
            public View createTabContent(String tag)
            {

                return (View) findViewById(R.id.stayprocess); // get the
                // tabcontent view
            }
        };

        mTabHost.addTab(mTabHost
                .newTabSpec("RunningApplication")
                .setIndicator(
                        getResources().getText(R.string.running_application),
                        null)
                .setContent(tabContentFactory));

        mTabHost.addTab(mTabHost
                .newTabSpec("StayresidentApplication")
                .setIndicator(
                        getResources().getText(R.string.stayresident_application),
                        null)
                .setContent(tabContentFactory));
        Log.e(TAG, "TabContentFactory  tabContentFactory 111 ");
        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

            public void onTabChanged(String tabId) {
                if (tabId != null) {
                    // System.out.println("CMPMG inside onTabchanged Previous Tab "+musicStoreDetail);
                    if (tabId.equals("RunningApplication")) {
                        // Under New releases TAB ATT supports Tracks
                        isRunningProcess = true;
                        Log.e(TAG, "onTabChanged  before mRunningStateInfo.hasData()   ");
                        onRefreshUi();

                        if (!mRunningStateInfo.hasData()) {
                            Log.e(TAG, "onTabChanged  mRunningStateInfo.hasData()   ");
                            View mlistprogressbar = (View) findViewById(R.id.listprogressbar);
                            mlistprogressbar.setVisibility(View.VISIBLE);
                        }
                        View mView = (View) findViewById(R.id.app_clearbutton);
                        mView.setVisibility(View.VISIBLE);
                        mView = (View) findViewById(R.id.list);
                        mView.setVisibility(View.VISIBLE);
                        mView = (View) findViewById(R.id.abovebuttonline);
                        mView.setVisibility(View.VISIBLE);
                        mView = (View) findViewById(R.id.user_process_title);
                        mView.setVisibility(View.GONE);
                        mView = (View) findViewById(R.id.system_process_title);
                        mView.setVisibility(View.GONE);
                        mView = (View) findViewById(R.id.userlist);
                        mView.setVisibility(View.GONE);
                        mView = (View) findViewById(R.id.systemlist);
                        mView.setVisibility(View.GONE);

                        // refreshHeadUi(isRunningProcess);

                    }
                    else if (tabId.equals("StayresidentApplication")) {
                        isRunningProcess = false;
                        // mAdapter.setShowBackground(false);
                        Log.e(TAG, "onTabChanged  StayresidentApplication  ");
                        onRefreshUi();

                        View mView = (View) findViewById(R.id.app_clearbutton);
                        mView.setVisibility(View.GONE);
                        mView = (View) findViewById(R.id.list);
                        mView.setVisibility(View.GONE);
                        mView = (View) findViewById(R.id.abovebuttonline);
                        mView.setVisibility(View.GONE);

                        mView = (View) findViewById(R.id.system_process_title);
                        mView.setVisibility(View.VISIBLE);

                        mView = (View) findViewById(R.id.systemlist);
                        mView.setVisibility(View.VISIBLE);

                        mNoAppTextView.setVisibility(View.GONE);

                    }

                }
            }
        });

        mClearAPPButton = ((Button) findViewById(R.id.btn_clear));

        mClearAPPButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Log.e(TAG, " RunningProcessesActivity mClearAPPButton  ");

                mRunningStateInfo.runningEndProcess();
                setEndProcessHead();

                Button mView = (Button) findViewById(R.id.btn_sel_all);

                mView.setText(getString(R.string.select_all));

                mServiceListAdapter.refreshItems(true);
                mServiceListAdapter.notifyDataSetChanged();
                setListViewHeight(mListView);
                

            }
        });

        mSelectButton = ((Button) findViewById(R.id.btn_sel_all));

        if (mListCheck) {
            mSelectButton.setText(getString(R.string.deselect_all));
        } else {
            mSelectButton.setText(getString(R.string.select_all));
        }

        mSelectButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                setAllSelection(!mListCheck);
            }
        });
        Log.e(TAG, " RunningProcessesActivity mListCheck =" + mListCheck);
        // setAllSelection(!mListCheck);

        mRamPercentView = (PercentView) findViewById(R.id.ramviewbig);
        mRamPercentView.initLabelView(GREEN); // green

        Log.e(TAG, " RunningProcessesActivity ");
        synchronized (mMemInfo) {

            mRamTotal = mMemInfo.getRamTotal();
            mRamFree = mMemInfo.getRamFree();

        }
        mRamTotal = mRamTotal * 1024;
        mRamFree = mRamFree * 1024;

        Log.e(TAG, " RunningProcessesActivity 111");
        mCurrentBackgroundItems = mRunningStateInfo.getCurrentBackgroundItems();
        mCurrentMergedItems = mRunningStateInfo.getCurrentMergedItems();
        Log.e(TAG, " RunningProcessesActivity 222");
        onRefreshUi();
        Log.e(TAG, " RunningProcessesActivity 333");
        ramviewSetText();
        Log.e(TAG, " RunningProcessesActivity 444");

    }

    private void initActionBar() {
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel < 11) {
            Log.e(TAG, "initActionBar() return");
            return;
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.custom_actionbar_layout, null);

            ((View) customActionBarView.findViewById(R.id.menu_item)).setOnClickListener(this);
            TextView textView = (TextView) customActionBarView.findViewById(R.id.tv_title);
            textView.setText(getResources().getString(R.string.str_action_bar_back_to_memory));

            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                            ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }
    }

    private void setEndProcessHead() {
        long tempsize = 0;
        for (int i = 0; i < mCurrentBackgroundItems.size(); i++) {
            RunningStateInfo.MergedItem item = mCurrentBackgroundItems.get(i);
            Log.e(TAG, "refreshHeadUi  mProcessName = " + item.mProcess.mProcessName + " mSize ="
                    + item.mProcess.mSize);
            if (item.isChecked) {
                tempsize += item.mProcess.mSize;
            }

        }

        String sizeStr = Formatter.formatShortFileSize(
                this, tempsize);
        Log.e(TAG, "setEndProcessHead  sizeStr = " + sizeStr + " tempsize =" + tempsize);

        StringBuilder m_ProcessStr = new StringBuilder();

        m_ProcessStr.append(getResources().getText(R.string.process_head_ram))
                    .append(": <font color='#33B5E5'>")
                    .append(String.valueOf(sizeStr) + "</font>")
                    .append(" ")
                    .append(getResources().getText(R.string.process_back_ram));

        TextView mbackTextView = (TextView) findViewById(R.id.rambacksize);

        mbackTextView.setText(Html.fromHtml(m_ProcessStr.toString()));

    }

    /**
     * @param selected it is for if the item selected true--->yes
     */
    void setAllSelection(boolean selected) {

        int mtotalCount = mListView.getCount();

        Button mView = (Button) findViewById(R.id.btn_sel_all);

        if (selected) {
            mView.setText(getString(R.string.deselect_all));
        } else {
            mView.setText(getString(R.string.select_all));
        }

        for (int i = 0; i < mtotalCount; i++) {
            RunningStateInfo.MergedItem mProcessItem = (RunningStateInfo.MergedItem) mListView
                    .getItemAtPosition(i);
            mProcessItem.isChecked = selected;

        }
        Log.e(TAG, "setAllSelection  mtotalCount = " + mtotalCount + " selected =" + selected);
        mListCheck = selected;

        refreshListUi(isRunningProcess);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "RunningProcessesActivity onPause start");
        engine.getMemInfo().unregisterListener(mMemInfoListener);
        // mRunningStateInfo.disableUpdate();
        engine.getRunningStateInfo().unregisterListener(mRunningStateInfoListener);

        Log.e(TAG, "RunningProcessesActivity onPause end");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "RunningProcessesActivity onResume start");
        engine.getMemInfo().registerListener(mMemInfoListener);

        engine.getRunningStateInfo().registerListener(mRunningStateInfoListener);
        // mRunningStateInfo.updateAgain();

        Log.e(TAG, "RunningProcessesActivity onResume end");
    }

    public void onRefreshUi() {

        refreshListUi(isRunningProcess);
        refreshHeadUi(isRunningProcess);
    }
}
