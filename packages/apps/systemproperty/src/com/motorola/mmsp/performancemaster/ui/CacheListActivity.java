/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * Li Shuqian/txvd74           04/12/2012                  Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.text.Html;
import android.text.format.Formatter;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.motorola.mmsp.performancemaster.engine.CacheListInfo;
import com.motorola.mmsp.performancemaster.engine.MemInfo;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.CacheListInfo.CacheInfo;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.R;


public class CacheListActivity extends Activity implements
        View.OnClickListener {

    private final String TAG = "CacheListActivity";
    protected static final int MSG_MEMINFO_UPDATE = 0x102;
    protected static final int MSG_CACHELIST_UPDATE = 0x103;
    private CacheInfoAdapter mCacheInfoAdapter = null;

    private ArrayList<CacheInfo> mCacheInfoList = null;
    private Button mClearButton;

    private ListView mCacheListView;
    private TextView mNoCacheTextView;
    private ProgressDialog mProgressDialog;

    private SysPropEngine engine = null;
    private PercentView mRomPercentView;
    private MemInfo mMemInfo = null;
    private CacheListInfo mCacheListInfo = null;
    private DecimalFormat myrominfoFormat = new DecimalFormat("##0.00");

    private long mRomTotal;
    private long mRomFree;

    private final static int PERCENTVIEW_GREEN = 2;

    private Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case CacheListActivity.MSG_MEMINFO_UPDATE:
                    synchronized (mMemInfo) {
                        mRomTotal = mMemInfo.getRomTotal();
                        mRomFree = mMemInfo.getRomFree();
                    }

                    Log.e(TAG, " CacheListActivity   MSG_MEMINFO_UPDATE  ");
                    if (mRomTotal > 0) {
                        romviewSetText();
                    }

                    break;
                case CacheListActivity.MSG_CACHELIST_UPDATE:

                    Log.e(TAG, " CacheListActivity MSG_CACHELIST_UPDATE ");

                    mCacheInfoList = mCacheListInfo.getCacheInfoList();

                    mCacheInfoAdapter.refreshItems();

                    mCacheInfoAdapter.notifyDataSetChanged();

                    initCashinfoListView();

                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }

                    View mClearHeader = (View) findViewById(R.id.rombigheader);
                    View mClearProgressBar = (View) findViewById(R.id.romclear_progressbar);
                    mClearHeader.setVisibility(View.VISIBLE);
                    mClearProgressBar.setVisibility(View.GONE);

                    Log.e(TAG, " CacheListActivity before CacheInfoAdapter mCacheInfoList.size = "
                            + mCacheInfoList.size());

                    break;

                default:
                break;
        }
        super.handleMessage(msg);
    }
    };

    private InfoListener mMemInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();

            msg.what = CacheListActivity.MSG_MEMINFO_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

    private InfoListener mCacheListInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();

            msg.what = CacheListActivity.MSG_CACHELIST_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

    private void initView() {

        synchronized (mMemInfo) {
            mRomTotal = mMemInfo.getRomTotal();
            mRomFree = mMemInfo.getRomFree();

        }

        Log.e(TAG,
                " CacheListActivity initView mCacheListInfo.isUpdated() = "
                        + mCacheListInfo.isUpdated());

        if (mCacheListInfo.isUpdated()) {
            mCacheInfoList = mCacheListInfo.getCacheInfoList();
            
            if(mCacheInfoList.size()>0){
                mClearButton.setEnabled(true);
            }
            else{
                mClearButton.setEnabled(false);
            }

            mCacheInfoAdapter.refreshItems();
            mCacheInfoAdapter.notifyDataSetChanged();

            initCashinfoListView();
            Log.e(TAG, " CacheListActivity  11 romviewSetText  ");
            if (mRomTotal > 0) {
                romviewSetText();
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }

            View mClearHeader = (View) findViewById(R.id.rombigheader);
            View mClearProgressBar = (View) findViewById(R.id.romclear_progressbar);
            mClearHeader.setVisibility(View.VISIBLE);
            mClearProgressBar.setVisibility(View.GONE);


        } else {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage(this.getText(R.string.app_loading));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.show();

            }
            Log.e(TAG, " CacheListActivity initView 4 ");
            View mClearHeader = (View) findViewById(R.id.rombigheader);
            View mClearProgressBar = (View) findViewById(R.id.romclear_progressbar);
            mClearHeader.setVisibility(View.GONE);
            mClearProgressBar.setVisibility(View.VISIBLE);
            mClearButton.setEnabled(false);
        }

    }

    private void initCashinfoListView() {

        int mCacheListSize = mCacheInfoAdapter.getCount();
        long mtempsizeKb = 0;
        mtempsizeKb = mCacheInfoAdapter.getTotalSize();
        String mCachesize = Formatter.formatShortFileSize(this, mtempsizeKb);

        StringBuilder m_ProcessStr = new StringBuilder();

        m_ProcessStr.append(getResources().getText(R.string.cache_head_rom))
                .append(": <font color='#33B5E5'>")
                .append(String.valueOf(mCachesize) + "</font>")
                .append(" ")
                .append(getResources().getText(R.string.cache_back_rom));

        TextView mbackTextView = (TextView) findViewById(R.id.rombacksize);
        mbackTextView.setText(Html.fromHtml(m_ProcessStr.toString()));

        Log.e(TAG, " CacheListActivity initCashinfoListView mCacheListSize  = " + mCacheListSize);

        if (mCacheListSize > 0) {

            mNoCacheTextView.setVisibility(View.GONE);

            mClearButton.setEnabled(true);

        } else {
            mNoCacheTextView.setText(CacheListActivity.this
                    .getString(R.string.no_cache));
            mNoCacheTextView.setVisibility(View.VISIBLE);

            mClearButton.setEnabled(false);

        }

    }

    private void romviewSetText() {
        
        String mRomFreeStr, mRomTotalStr;
        
        if (((mRomFree / 1024) / 1024) / 1024 < 1) {
            mRomFreeStr = Formatter.formatShortFileSize(this, mRomFree);

        } else {

            float mRomFreeF = ((mRomFree / 1024f) / 1024f) / 1024f;
            mRomFreeStr = myrominfoFormat.format(mRomFreeF) + getString(R.string.MEM_GB);
        }

        if (((mRomTotal / 1024) / 1024) / 1024 < 1) {
            mRomTotalStr = Formatter.formatShortFileSize(this, mRomTotal);

        } else {

            float mRamTotalF = ((mRomTotal / 1024f) / 1024f) / 1024f;
            mRomTotalStr = myrominfoFormat.format(mRamTotalF) + getString(R.string.MEM_GB);
        }

        Log.e(TAG, " CacheListActivity   romviewSetText mRomFree = " + mRomFree);

        String mrom_free_total = getResources().getString(R.string.rom_free_total,
                mRomFreeStr,
                mRomTotalStr);
        TextView mramclickTextView = (TextView) findViewById(R.id.romfreetotalbig);
        mramclickTextView.setText(mrom_free_total);

        long mUsedPerText = ((mRomTotal - mRomFree) * 100)
                / mRomTotal;
        long mPerView = ((mRomTotal - mRomFree) * 360) / mRomTotal;
        Log.e(TAG, " CacheListActivity   romviewSetText mUsedPerText = " + mUsedPerText);
        mRomPercentView.setUsedPerText(mPerView, mUsedPerText + "%");
    }

    private void selectbuildDialog(int mDialogTitalResId, int mDialogTextResId, int mDialogOKResId,
            int mDialogCancelResId, Context paramContext) {
        AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
        mAlertDialog.setTitle(mDialogTitalResId);
        mAlertDialog.setMessage(mDialogTextResId);
        mAlertDialog.setCancelable(false);
        if (mDialogOKResId != -1)
            mAlertDialog.setPositiveButton(mDialogOKResId,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                            engine.getCacheListInfo().unregisterListener(mCacheListInfoListener);
                            synchronized (mCacheListInfo) {
                                
                                if(mCacheInfoList == null)
                                    return;
                                
                                if (mCacheInfoList.size() > 0) {
                                    mCacheInfoList.clear();
                                }
                            }
                            
                            mCacheListInfo.clearAllCaches(); // clear the cache
            
                            mCacheInfoAdapter.refreshItems();
                            if (mCacheInfoAdapter != null) {
                                mCacheInfoAdapter.notifyDataSetChanged();
                            }

                            StringBuilder m_ProcessStr = new StringBuilder();
                            String mCachesize = Formatter.formatShortFileSize(
                                    CacheListActivity.this, 0);
                            m_ProcessStr.append(getResources().getText(R.string.cache_head_rom))
                                    .append(": <font color='#33B5E5'>")
                                    .append(String.valueOf(mCachesize) + "</font>")
                                    .append(" ")
                                    .append(getResources().getText(R.string.cache_back_rom));

                            TextView mbackTextView = (TextView) findViewById(R.id.rombacksize);
                            mbackTextView.setText(Html.fromHtml(m_ProcessStr.toString()));

                            mClearButton.setEnabled(false); // disable the clear
                                                            // button

                            mNoCacheTextView.setVisibility(0);
                            mNoCacheTextView.setText(CacheListActivity.this
                                            .getString(R.string.no_cache));

                        }
                    });
        if (mDialogCancelResId != -1)
            mAlertDialog.setNegativeButton(mDialogCancelResId,
                    new DialogInterface.OnClickListener() {
                        public void onClick(
                                DialogInterface paramDialogInterface,
                                int paramInt) {
                        }
                    });
        mAlertDialog.show();
    }

    public void onClick(View paramView) {
        switch (paramView.getId()) {

            case R.id.oneclean_button:
                selectbuildDialog(R.string.dialogtital,
                        R.string.oneclean_context, R.string.opda_global_ok,
                        R.string.opda_global_cancel, paramView.getContext());
                break;
            case R.id.menu_item: 
                this.finish();
                break;   
            default:
                break;

        }
    }

    class CacheInfoAdapter extends BaseAdapter {

        private Context context;
        private LayoutInflater layoutInflater;
        public ArrayList<CacheInfo> mCacheInfoAdapterList = new ArrayList<CacheInfo>();

        public CacheInfoAdapter(Context paramContext) {
            this.context = paramContext;
            Log.e("CacheListActivity", "CacheInfoAdapter is in");

            this.layoutInflater = LayoutInflater.from(paramContext);
            refreshItems();
        }

        void refreshItems() {
            synchronized (mCacheListInfo) {
                if (mCacheInfoList == null)
                    return;

                if (mCacheInfoAdapterList.size() > 0)
                    mCacheInfoAdapterList.clear();

                for (int i = 0; i < mCacheInfoList.size(); i++) {

                    CacheInfo localCacheInfo = mCacheInfoList.get(i);

                    mCacheInfoAdapterList.add(localCacheInfo);
                }

            }
        }

        public int getCount() {
            Log.e(TAG, "CacheInfoAdapter getCount = " + mCacheInfoAdapterList.size());
            return mCacheInfoAdapterList.size();
        }

        public Object getItem(int paramInt) {
            return mCacheInfoAdapterList.get(paramInt);
        }

        public long getItemId(int paramInt) {
            Log.e(TAG, "CacheInfoAdapter getItemId = " + paramInt);
            return paramInt;
        }

        public ArrayList<CacheInfo> getList() {
            return mCacheInfoAdapterList;
        }

        public long getTotalSize() {
            long l = 0L;
            for (int i = 0;; ++i) {
                if (i >= mCacheInfoAdapterList.size())
                    return l;
                l += ((CacheInfo) mCacheInfoAdapterList.get(i)).getCacheSize();
            }
        }

        public View getView(int paramInt, View paramView, ViewGroup paramViewGroup) {
            Log.e(TAG, " getView paramInt = " + paramInt);

            ViewHolder localViewHolder;
            if (paramView == null) {
                localViewHolder = new ViewHolder();
                paramView = this.layoutInflater.inflate(R.layout.cache_item, null);
                localViewHolder.imageView_Icon = ((ImageView) paramView
                        .findViewById(R.id.CacheItem_imgView));
                localViewHolder.textView_Name = ((TextView) paramView
                        .findViewById(R.id.CacheItem_textView));

                localViewHolder.textView_Cache_size = ((TextView) paramView
                        .findViewById(R.id.CacheItem_textView_size));
                paramView.setTag(localViewHolder);
            }

            if (mCacheInfoAdapterList != null) {
                CacheInfo localCacheInfo = (CacheInfo) mCacheInfoAdapterList.get(paramInt);
                localViewHolder = (ViewHolder) paramView.getTag();

                if (localCacheInfo != null) {
                    localViewHolder.imageView_Icon.setImageDrawable(localCacheInfo.getIcon());
                    localViewHolder.textView_Name.setText(localCacheInfo.getName());
                    localViewHolder.textView_Cache_size
                                .setText(Formatter.formatShortFileSize(this.context, localCacheInfo
                                                .getCacheSize()));
                }
            }
            return paramView;

        }

        public class ViewHolder {
            ImageView imageView_Icon;
            TextView textView_Cache_size;
            TextView textView_Name;
        }
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
            textView.setText(getResources().getString(R.string.str_action_bar_back_to_memory));
            
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }
    }
    

    @Override
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
      
        setContentView(R.layout.romcachelistview);
        //init action bar
        initActionBar();
        SysPropEngine.setContext(getApplicationContext());
        engine = SysPropEngine.getInstance();

        mMemInfo = (MemInfo) engine.getMemInfo();
        mCacheListInfo = (CacheListInfo) engine.getCacheListInfo();

     
        mNoCacheTextView = (TextView) findViewById(R.id.textView_no_Cache);

        mClearButton = (Button) findViewById(R.id.oneclean_button);

        mClearButton.setOnClickListener(this);
        mCacheListView = (ListView) findViewById(R.id.CacheListView_app);
        mCacheInfoAdapter = new CacheInfoAdapter(getApplicationContext());
        mCacheListView.setAdapter(mCacheInfoAdapter);

        mRomPercentView = (PercentView) findViewById(R.id.romviewbig);
        mRomPercentView.initLabelView(PERCENTVIEW_GREEN); // green
        Log.e(TAG, "onCreate is in");
    }

    @Override
    protected void onPause() {
        super.onPause();
        engine.getMemInfo().unregisterListener(mMemInfoListener);
        engine.getCacheListInfo().unregisterListener(mCacheListInfoListener);

        Log.e(TAG, "onpause is in");

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "onResume is in");

        initView();
        engine.getMemInfo().registerListener(mMemInfoListener);
        engine.getCacheListInfo().registerListener(mCacheListInfoListener);
    }

}
