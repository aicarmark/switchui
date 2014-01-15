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

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.motorola.mmsp.performancemaster.engine.CacheListInfo;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.engine.MemInfo;
import com.motorola.mmsp.performancemaster.engine.RunningStateInfo;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.R;

import android.app.Activity;

import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;

import android.os.Message;
import android.text.format.Formatter;
import android.view.View;

import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.Toast;

import android.widget.TextView;




public class MemoryInfoActivity extends Activity {
    private final static String TAG = "MemeryInfoActivity";

    protected static final int MSG_MEMINFO_UPDATE = 0x102;
    protected static final int MSG_PROCESS_UPDATE = 0x103;
    private final static int GREEN = 1;
    private final static int BLUE = 2;
    private final static int YELLOW = 3;
    private SysPropEngine engine = null;
    private DecimalFormat myFormat = new DecimalFormat("##0");
    private DecimalFormat mysdinfoFormat = new DecimalFormat("##0.00");
    private PercentView mRamPercentView, mRomPercentView, mSdPercentView;
    // it is for display ram info
    private TextView mRamTotalView, mRamUsedView, mRamFreeView;
    // it is for display rom info
    private TextView mRomTotalView, mRomUsedView, mRomFreeView;
    // it is for display sd info
    private TextView mSDTotalView, mSDUsedView, mSDFreeView;

    private Button mOneClickImageView, mRamSelectButton, mRomSelectButton;
    private View mRamListView, mRomListView;

    private long mRamTotal;
    private long mRamFree;
    private long mRomTotal;
    private long mRomFree;
    private long mSdTotal = 0;
    private long mSdFree;
    private MemInfo mMemInfo = null;
    private RunningStateInfo mRunningStateInfo = null;
    private CacheListInfo mCacheListInfo = null;
    private ArrayList<RunningStateInfo.MergedItem> mCurrentBackgroundItems = null;
    private String MB = null, GB = null;

    private String mRomFreeStr, mRomTotalStr;
    private Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MemoryInfoActivity.MSG_MEMINFO_UPDATE:
                    Log.e(TAG, " MemeryInfoActivity MSG_MEMINFO_UPDATE ");

                    synchronized (mMemInfo) {
                        mRamTotal = mMemInfo.getRamTotal();
                        mRamFree = mMemInfo.getRamFree();

                        mRomTotal = mMemInfo.getRomTotal();
                        mRomFree = mMemInfo.getRomFree();

                        mSdTotal = mMemInfo.getSdTotal();
                        mSdFree = mMemInfo.getSdFree();
                    }

                    mRamTotal = mRamTotal * 1024;
                    mRamFree = mRamFree * 1024;

                    ramviewSetText();
                    if (mRomTotal > 0) {
                        romviewSetText();
                    }
                    if (mSdTotal > 0) {

                        sdviewSetText();
                    }
                    onRefreshHeadMem();

                    break;
                case MemoryInfoActivity.MSG_PROCESS_UPDATE:

                    mCurrentBackgroundItems = mRunningStateInfo.getCurrentBackgroundItems();
                    Log
                            .e(
                                    TAG,
                                    " MemoryInfoActivity  MSG_PROCESS_UPDATE mCurrentBackgroundItems.size = "
                                            + mCurrentBackgroundItems.size());

                    onRefreshClickClearer();

                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }
    };

    /*
     * refresh the mem status
     */

    private void onRefreshHeadMem() {

        if (((mRomFree / 1024) / 1024) / 1024 < 1) {
            mRomFreeStr = Formatter.formatShortFileSize(this, mRomFree);

        } else {

            float mRomFreeF = ((mRomFree / 1024f) / 1024f) / 1024f;
            mRomFreeStr = mysdinfoFormat.format(mRomFreeF) + GB;
        }

        if (((mRomTotal / 1024) / 1024) / 1024 < 1) {
            mRomTotalStr = Formatter.formatShortFileSize(this, mRomTotal);

        } else {

            float mRamTotalF = ((mRomTotal / 1024f) / 1024f) / 1024f;
            mRomTotalStr = mysdinfoFormat.format(mRamTotalF) + GB;
        }

        String mram_free_total = getResources().getString(R.string.ram_free_total,
                Formatter.formatShortFileSize(this, mRamFree),
                Formatter.formatShortFileSize(this, mRamTotal));
        TextView mramclickTextView = (TextView) findViewById(R.id.ramclick);
        mramclickTextView.setText(mram_free_total);

        String mrom_free_total = getResources().getString(R.string.rom_free_total,
                mRomFreeStr,
                mRomTotalStr);
        TextView mromclickTextView = (TextView) findViewById(R.id.romclick);
        mromclickTextView.setText(mrom_free_total);
    }

    /*
     * refresh the click cleaner status
     */

    private void onRefreshClickClearer() {

        View mClearHeader = (View) findViewById(R.id.sysclear_header);
        View mClearProgressBar = (View) findViewById(R.id.sysclear_progressbar);
        mClearHeader.setVisibility(View.VISIBLE);
        mClearProgressBar.setVisibility(View.GONE);

        String mbackgroudapps = getResources().getString(R.string.runningapps,
                mCurrentBackgroundItems.size());
        TextView mbackTextView = (TextView) findViewById(R.id.backgroudapps);
        mbackTextView.setText(mbackgroudapps);

    }

    private void updateMemInfo() {
        synchronized (engine.getMemInfo()) {
            mRamTotal = mMemInfo.getRamTotal();
            mRamFree = mMemInfo.getRamFree();

            mRomTotal = mMemInfo.getRomTotal();
            mRomFree = mMemInfo.getRomFree();

            mSdTotal = mMemInfo.getSdTotal();
            mSdFree = mMemInfo.getSdFree();

        }

        mRamTotal = mRamTotal * 1024;
        mRamFree = mRamFree * 1024;

        mRamTotalView.setText(getString(R.string.ramtotal) + " "
                + Formatter.formatShortFileSize(this, mRamTotal));

        if (((mRomTotal / 1024) / 1024) / 1024 < 1) {

            mRomTotalView.setText(getString(R.string.romtotal)
                    + " " + Formatter.formatShortFileSize(this, mRomTotal));
        } else {
            Log.e(TAG, "MemoryInfoActivity romviewSetText mRomTotal > 1g");
            float mRomTotalF = ((mRomTotal / 1024f) / 1024f) / 1024f;

            mRomTotalView.setText(getString(R.string.romtotal) + " "
                    + mysdinfoFormat.format(mRomTotalF) + GB);
        }

        if (mSdTotal > 0) {
            sdviewSetText();
        } else if (mSdTotal == 0) {
            mSDTotalView.setText(getString(R.string.sdtotal) + " " + "0 " + MB);

            mSDFreeView.setText(getString(R.string.sdfree) + " " + "0 " + MB);
            mSDUsedView.setText(getString(R.string.sdused) + " " + "0 " + MB);

            mSdPercentView.setUsedPerText(0, " ");

        }
        if (mRamTotal > 0) {
            ramviewSetText();
        }

        if (mRomTotal > 0) {
            romviewSetText();
        }

        onRefreshHeadMem();

    }

    /**
     * set ram info
     */
    private void ramviewSetText() {

        mRamFreeView.setText(getString(R.string.ramfree)
                + " " + Formatter.formatShortFileSize(this, mRamFree));
        mRamUsedView.setText(getString(R.string.ramused)
                + " " + Formatter.formatShortFileSize(this, mRamTotal - mRamFree));
        long mUsedPerText = ((mRamTotal - mRamFree) * 100)
                / mRamTotal;

        long mPerView = ((mRamTotal - mRamFree) * 360) / mRamTotal;
        mRamPercentView.setUsedPerText(mPerView, myFormat.format(mUsedPerText) + "%");
    }

    /**
     * set rom info
     */

    private void romviewSetText() {

        if (((mRomFree / 1024) / 1024) / 1024 < 1) {

            mRomFreeView.setText(getString(R.string.romfree)
                    + " " + Formatter.formatShortFileSize(this, mRomFree));
        } else {
            Log.e(TAG, "MemoryInfoActivity romviewSetText mRomFree > 1g");
            float mRomFreeF = ((mRomFree / 1024f) / 1024f) / 1024f;
            mRomFreeView.setText(getString(R.string.romfree) + " "
                    + mysdinfoFormat.format(mRomFreeF) + GB);
        }

        if ((((mRomTotal - mRomFree) / 1024) / 1024) / 1024 < 1) {

            mRomUsedView.setText(getString(R.string.romused)
                    + " " + Formatter.formatShortFileSize(this, (mRomTotal - mRomFree)));

        } else {
            Log.e(TAG, "MemoryInfoActivity romviewSetText mRomUsed > 1g");
            float mRomFreeF = (((mRomTotal - mRomFree) / 1024f) / 1024f) / 1024f;
            mRomUsedView.setText(getString(R.string.romused) + " "
                    + mysdinfoFormat.format(mRomFreeF) + GB);
        }

       long mUsedPerText = ((mRomTotal - mRomFree) * 100)
                / mRomTotal;

        long mPerView = ((mRomTotal - mRomFree) * 360) / mRomTotal;
        mRomPercentView.setUsedPerText(mPerView, myFormat.format(mUsedPerText) + "%");

    }

    /**
     * set sd info
     */

    private void sdviewSetText() {

        if (((mSdTotal / 1024) / 1024) / 1024 < 1) {
            mSDTotalView.setText(getString(R.string.sdtotal)
                    + " " + Formatter.formatShortFileSize(this, mSdTotal));
        } else {
            Log.e(TAG, "MemoryInfoActivity sdviewSetText mSdTotal > 1g");
            float mSdTotalF = ((mSdTotal / 1024f) / 1024f) / 1024f;
            mSDTotalView.setText(getString(R.string.sdtotal) + " "
                    + mysdinfoFormat.format(mSdTotalF) + GB);
        }

        if (((mSdFree / 1024) / 1024) / 1024 < 1) {

            mSDFreeView.setText(getString(R.string.sdfree)
                    + " " + Formatter.formatShortFileSize(this, mSdFree));

        } else {
            Log.e(TAG, "MemoryInfoActivity sdviewSetText mSdFree > 1g");
            float mSdFreeF = ((mSdFree / 1024f) / 1024f) / 1024f;
            mSDFreeView.setText(getString(R.string.sdfree) + " "
                    + mysdinfoFormat.format(mSdFreeF) + GB);
        }

        if ((((mSdTotal - mSdFree) / 1024) / 1024) / 1024 < 1) {

            mSDUsedView.setText(getString(R.string.sdused)
                    + " " + Formatter.formatShortFileSize(this, (mSdTotal - mSdFree)));

        } else {
            Log.e(TAG, "MemoryInfoActivity sdviewSetText mSdUsed > 1g");
            float mSdUsedF = (((mSdTotal - mSdFree) / 1024f) / 1024f) / 1024f;
            mSDUsedView.setText(getString(R.string.sdused) + " "
                    + mysdinfoFormat.format(mSdUsedF) + GB);
        }

        long mUsedPerText = ((mSdTotal - mSdFree) * 100)
                / mSdTotal;

        long mPerView = ((mSdTotal - mSdFree) * 360) / mSdTotal;

        mSdPercentView.setUsedPerText(mPerView, myFormat.format(mUsedPerText) + "%");

    }

    /**
     * init the engine for get meminfo
     */

    private void regMemInfoInit() {
        // MemInfo update
        engine = SysPropEngine.getInstance();

        mMemInfo = (MemInfo) engine.getMemInfo();
        mRunningStateInfo = (RunningStateInfo) engine.getRunningStateInfo();
        mCacheListInfo = (CacheListInfo) engine.getCacheListInfo();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.memoryinfo);

        mRamTotalView = (TextView) findViewById(R.id.idramtotal);
        mRamUsedView = (TextView) findViewById(R.id.idramused);
        mRamFreeView = (TextView) findViewById(R.id.idramfree);

        mRomTotalView = (TextView) findViewById(R.id.idromtotal);
        mRomUsedView = (TextView) findViewById(R.id.idromused);
        mRomFreeView = (TextView) findViewById(R.id.idromfree);

        mSDTotalView = (TextView) findViewById(R.id.idsdtotal);
        mSDUsedView = (TextView) findViewById(R.id.idsdused);
        mSDFreeView = (TextView) findViewById(R.id.idsdfree);
        mRamListView = (View) findViewById(R.id.listramview);

        mRamListView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MemoryInfoActivity.this, RunningProcessesActivity.class);
                startActivity(intent);

            }

        });

        mRomListView = (View) findViewById(R.id.listromview);

        mRomListView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO start the process activity
                Intent intent = new Intent(MemoryInfoActivity.this, CacheListActivity.class);
                startActivity(intent);

            }

        });

        mRamPercentView = (PercentView) findViewById(R.id.ramview);

        mOneClickImageView = (Button) findViewById(R.id.oneclick);

        mOneClickImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Log.e(TAG, " MemoryInfoActivity mOneClickImageView  onClick");
                if (mCurrentBackgroundItems != null) {
                    mRunningStateInfo.oneClickClear();
                    View mClearHeader = (View) findViewById(R.id.sysclear_header);
                    View mClearProgressBar = (View) findViewById(R.id.sysclear_progressbar);
                    mClearHeader.setVisibility(View.VISIBLE);
                    mClearProgressBar.setVisibility(View.GONE);

                    String mbackgroudapps = getResources().getString(R.string.runningapps,
                            0);
                    TextView mbackTextView = (TextView) findViewById(R.id.backgroudapps);
                    mbackTextView.setText(mbackgroudapps);

                }
                mCacheListInfo.clearAllCaches();
                
                Toast.makeText(MemoryInfoActivity.this, getResources().getString(R.string.oneclean_result), Toast.LENGTH_SHORT).show();

            }

        });

        mRamSelectButton = (Button) findViewById(R.id.ramselect);

        mRamSelectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO start the process activity
                Intent intent = new Intent(MemoryInfoActivity.this, RunningProcessesActivity.class);
                startActivity(intent);
            }

        });

        mRomSelectButton = (Button) findViewById(R.id.romselect);

        mRomSelectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO start the process activity
                Intent intent = new Intent(MemoryInfoActivity.this, CacheListActivity.class);
                startActivity(intent);
            }

        });

        mRomPercentView = (PercentView) findViewById(R.id.romview);

        mSdPercentView = (PercentView) findViewById(R.id.sdview);
        MB = getString(R.string.MEM_MB);
        GB = getString(R.string.MEM_GB);

        mRamPercentView.initLabelView(GREEN); // green
        mRomPercentView.initLabelView(BLUE); // blue
        mSdPercentView.initLabelView(YELLOW); // yellow

        regMemInfoInit();

        updateMemInfo();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "MemoryInfoActivity onResume begin");
        engine.getMemInfo().registerListener(mMemInfoListener);

        if (mRunningStateInfo.hasData()) {
            Log.e(TAG, "MemoryInfoActivity onResume ");
            mCurrentBackgroundItems = mRunningStateInfo.getCurrentBackgroundItems();

            onRefreshClickClearer();

        }

        engine.getRunningStateInfo().registerListener(mRunningStateInfoListener);
        mCacheListInfo.onCacheListUpdate();
        // Log.e(TAG, "MemoryInfoActivity onResume end");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Log.e(TAG, "MemoryInfoActivity onPause start");
        engine.getMemInfo().unregisterListener(mMemInfoListener);
        // Log.e(TAG, "MemoryInfoActivity onPause start yes");
        engine.getRunningStateInfo().unregisterListener(mRunningStateInfoListener);
        Log.e(TAG, "MemoryInfoActivity onPause end");
    }

    private InfoListener mMemInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();
            msg.what = MemoryInfoActivity.MSG_MEMINFO_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

    private InfoListener mRunningStateInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = Message.obtain();
            msg.what = MemoryInfoActivity.MSG_PROCESS_UPDATE;
            myHandler.sendMessage(msg);
        }
    };

}
