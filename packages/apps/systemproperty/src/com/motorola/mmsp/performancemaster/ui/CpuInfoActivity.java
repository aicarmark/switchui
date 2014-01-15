/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                       04/09/2012                  Initial release 
 *         
 */

package com.motorola.mmsp.performancemaster.ui;

import com.motorola.mmsp.performancemaster.engine.CPUInfo;
import com.motorola.mmsp.performancemaster.engine.InfoBase;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class CpuInfoActivity extends Activity implements View.OnClickListener,
        ViewTreeObserver.OnPreDrawListener {

    private final static String TAG = "CpuInfoActivity";

    private final static int MSG_CPU_FREQ_UPDATE = 0x101;

    private final static int MSG_GRAPHIC_UPDATE = 0x102;

    private final static int MSG_CPU_USAGE_UPDATE = 0x103;

    private final static int CPU_USAGE_LIST_MAX_SIZE = 45;

    private TextView[] mTvArray = null;

    // resource from xml
    private DrawGraphic mDg1, mDg2;

    private TextView mTvModel, mTvCurFreq, mTvMinFreq, mTvMaxFreq, mTvCpuUsrUsage, mTvCpuSysUsage,
            mTvCpuIdleUsage;

    private LinearLayout mLlDashboardGraphic;

    private int mCpuCoreTotal = 0, mCpuUsageListSize = 0;

    private SysPropEngine mEngine = null;

    private CPUInfo mCI = null;

    ArrayList<ArrayList<Float>> mCpuUsageList = null;

    private long mCpuCurFreq = 0, mCpuFreqLastDisplay = 0;

    private DecimalFormat mFormatPercent = new DecimalFormat("##0.0");

    // activity status
    private static final int ACTIVITY_STATUS_RESUME = 0;

    private static final int ACTIVITY_STATUS_PAUSE = 1;

    private static int mActivityStatus = -1;

    // CPU usage index

    private static final int CPU_INDEX_CPU = 0; 
    
    private static final int CPU_INDEX_USR = 1;

    private static final int CPU_INDEX_SYS = 2;

    private static final int CPU_INDEX_IDLE = 3;

    private static final int CPU_INDEX_CPU0 = 4;

    private static final int CPU_INDEX_CPU1 = 5;
    
    //mIronPrimeCT=true, this build for IronPrimeCT.
    private static final String mStrRefModel = "MT788";
    private boolean mIronPrimeCT = false; 

    // process main thread's message
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case CpuInfoActivity.MSG_CPU_FREQ_UPDATE:
                    updateCurFreq(false);
                    break;

                case CpuInfoActivity.MSG_GRAPHIC_UPDATE:
                    updateGraphic();
                    break;

                case CpuInfoActivity.MSG_CPU_USAGE_UPDATE:
                    updateCPUUsageDisplay();
                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cpu_info);

        // init resource id
        initResourceId();

        // register listener
        regCpuInfoListener();

        // init cpu core/data array
        initCpuData();

        // update cpu static info
        updateStaticInfo();
    }

    @Override
    protected void onDestroy() {
        // unregister listener
        unRegCpuInfoListener();
        super.onDestroy();
    }

    @Override
    protected void onResume() {

        mActivityStatus = ACTIVITY_STATUS_RESUME;

        // adjust graphic size
        adjustDrawGraphicSize();

        super.onResume();
    }

    @Override
    protected void onPause() {
        mActivityStatus = ACTIVITY_STATUS_PAUSE;

        super.onPause();
    }

    /**
     * get data from engine
     */
    public void onInfoUpdate(InfoBase infoObj) {

        // update display
        updateDisplay();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivb_arrow: {
                Intent intent = new Intent(CpuInfoActivity.this, CpuRunningApplist.class);
                this.startActivity(intent);
                break;
            }
            default:
                break;
        }
    }

    /**
     * resize draw graphic and dashboard
     */
    public boolean onPreDraw() {
        int nWidthDP = mDg1.getWidth();
        int nHeightDP = mDg1.getHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        
        
        LinearLayout llRefView = (LinearLayout)findViewById(R.id.ll_reference_view);
        RelativeLayout.LayoutParams lpRefView = (RelativeLayout.LayoutParams)
                          llRefView.getLayoutParams();
        RelativeLayout.LayoutParams  lpParent = (RelativeLayout.LayoutParams)
                          mLlDashboardGraphic.getLayoutParams();
        
        nWidthDP = displayMetrics.widthPixels - lpRefView.leftMargin - lpParent.rightMargin + 1;

        // draw size
        Point pSpace = new Point(0, 0);
        Rect rDraw = new Rect(pSpace.x, pSpace.y, nWidthDP - pSpace.x, nHeightDP - pSpace.y);

        if (1 == mCpuCoreTotal) {
            mDg1.setDrawGraphicParams(rDraw, pSpace);
            mDg2.setVisibility(View.GONE);
        } else if (2 == mCpuCoreTotal) {
            mDg1.setDrawGraphicParams(rDraw, pSpace);
            mDg2.setDrawGraphicParams(rDraw, pSpace);
        }

        // adjust dashboard display
        LinearLayout layoutCpuUsage2 = (LinearLayout) findViewById(R.id.ll_cpu2_dashboard_graphic);

        if (1 == mCpuCoreTotal) {
            layoutCpuUsage2.setVisibility(View.GONE);
        }

        mLlDashboardGraphic.getViewTreeObserver().removeOnPreDrawListener(this);

        return false;
    }

    private boolean isIronPrimeCT()
    {    	
    	boolean br = false;    	
    	
    	if (0 == mStrRefModel.compareTo(Build.MODEL.toString())) 
    	{
    		br = true;
    	}
    	
    	Log.e(TAG, "     isIronPrimeCT=" + br);
    	
    	return br;
    }
    
    private void initResourceId() {

        mDg1 = (DrawGraphic) findViewById(R.id.dg_graphic_1);
        mDg2 = (DrawGraphic) findViewById(R.id.dg_graphic_2);

        mTvModel = (TextView) findViewById(R.id.tv_model);

        mTvCurFreq = (TextView) findViewById(R.id.tv_current_freq);
        mTvMinFreq = (TextView) findViewById(R.id.tv_min_freq);
        mTvMaxFreq = (TextView) findViewById(R.id.tv_max_freq);

        mTvCpuUsrUsage = (TextView) findViewById(R.id.tv_cpu_usage_usr);
        mTvCpuSysUsage = (TextView) findViewById(R.id.tv_cpu_usage_sys);
        mTvCpuIdleUsage = (TextView) findViewById(R.id.tv_cpu_usage_idle);

        mLlDashboardGraphic = (LinearLayout) findViewById(R.id.ll_dashboard_graph);

        mTvArray = new TextView[] {
                (TextView) findViewById(R.id.tv_cpuusage1),
                (TextView) findViewById(R.id.tv_cpuusage2)
        };

        ((ImageButton) findViewById(R.id.ivb_arrow)).setOnClickListener(this);
    }

    private boolean updateCpuInfoData() {
        synchronized (mCI) {
            if (null == mCpuUsageList || mCI == null) {
                Log.e(TAG, "null == mCpuUsageList || mCI == null, return");
                return false;
            }

            // get CPU usage
            for (int nCount = 0; nCount < mCpuUsageListSize; nCount++) {

                Float fCpuUsage = mCI.getCpuUsage().get(nCount);

                int nArraySize = mCpuUsageList.get(nCount).size();
                if (nArraySize >= CPU_USAGE_LIST_MAX_SIZE) {
                    mCpuUsageList.get(nCount).remove(nArraySize - 1);
                }

                mCpuUsageList.get(nCount).add(0, fCpuUsage);
            }

            // get CPU freq
            mCpuCurFreq = mCI.getCpuFreq().get(0);
        }

        return true;
    }

    private void updateDisplay() {
        if (updateCpuInfoData())// there is a new data
        {
            // update CPU freq.
            Message msgFreq = new Message();
            msgFreq.what = CpuInfoActivity.MSG_CPU_FREQ_UPDATE;

            // update graphic
            Message msgDraw = new Message();
            msgDraw.what = CpuInfoActivity.MSG_GRAPHIC_UPDATE;

            // update CPU usage and dashboard
            Message msgUsage = new Message();
            msgUsage.what = CpuInfoActivity.MSG_CPU_USAGE_UPDATE;

            if (mActivityStatus == ACTIVITY_STATUS_RESUME) {
                if (mCpuFreqLastDisplay != mCpuCurFreq) {// should't update if
                                                         // no change
                    mHandler.sendMessage(msgFreq);
                }
                mHandler.sendMessage(msgDraw);
                mHandler.sendMessage(msgUsage);

                Log.e(TAG, " send three msg");
            } else if (mActivityStatus == ACTIVITY_STATUS_PAUSE) {
                Log.e(TAG, " send one msg");

                mHandler.sendMessage(msgDraw);
            }
        }
    }

    private InfoListener mCpuInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            updateDisplay();
        }
    };

    private void regCpuInfoListener() {
        mEngine = SysPropEngine.getInstance();

        mCI = (CPUInfo) mEngine.getCpuInfo();
        mCI.registerListener(mCpuInfoListener);
    }

    private void unRegCpuInfoListener() {
        mCI.unregisterListener(mCpuInfoListener);
        mCI = null;
        mEngine = null;
    }

    private void initCpuData() {
    	//this application used for IronPrimeCT
        mIronPrimeCT =  isIronPrimeCT();
        
       //  how many graphic should draw
        if (mIronPrimeCT) //ironPrimeCT
        {
        	mCpuCoreTotal = 1; 
        }
        else //other
        {
        	mCpuCoreTotal = mCI.getCpuCoreNum(); 
        }

        mCpuUsageListSize = CPU_INDEX_CPU0 + mCpuCoreTotal;

        mCpuUsageList = new ArrayList<ArrayList<Float>>();

        for (int nCount = 0; nCount < mCpuUsageListSize; nCount++) {
            ArrayList<Float> alTemp = new ArrayList<Float>();
            mCpuUsageList.add(alTemp);
        }
    }

    /**
     * adjust draw graphic's size by screen size
     */
    private void adjustDrawGraphicSize() {
        mLlDashboardGraphic.getViewTreeObserver().addOnPreDrawListener(this);
    }

    private void updateStaticInfo() {
        // update model
        String strModel = mCI.getModel();
        if (null == strModel)
        {
            strModel = getResources().getString(R.string.str_cpu_unknown); 
        }        
        mTvModel.setText(strModel);

        // first time to init cpu freq
        updateCurFreq(true);

        // update min/max freq
        mTvMinFreq.setText(getResources().getString(R.string.str_cpu_min_freq)
                + (mCI.getCpuFreqRange().get(0)[0] / 1000) + " "
                + getResources().getString(R.string.str_cpu_freq_units));
        mTvMaxFreq.setText(getResources().getString(R.string.str_cpu_max_freq)
                + (mCI.getCpuFreqRange().get(0)[1] / 1000) + " "
                + getResources().getString(R.string.str_cpu_freq_units));

        // update CPU usage
        mTvCpuUsrUsage.setText(getResources().getString(R.string.str_cpu_usage_usr) + " "
                + getResources().getString(R.string.str_percent_symbol));
        mTvCpuSysUsage.setText(getResources().getString(R.string.str_cpu_usage_sys) + " "
                + getResources().getString(R.string.str_percent_symbol));
        mTvCpuIdleUsage.setText(getResources().getString(R.string.str_cpu_usage_idle) + " "
                + getResources().getString(R.string.str_percent_symbol));

        // update CPU1 label, change string "cpu1" to "cpu"
        if (1 == mCpuCoreTotal) {
            ((TextView) findViewById(R.id.tv_cpu1_lael_1)).setText(getResources().getString(
                    R.string.str_cpu_label_1_rename));
        }
    }

    /**
     * this function is used for update CPU freq
     * 
     * @param bFirstInit it should be true if it's the first time to updat cpu
     *            freq
     */
    private void updateCurFreq(boolean bFirstInit) {

        if (bFirstInit)
            mCpuCurFreq = mCI.getCpuFreq().get(0);

        mCpuFreqLastDisplay = mCpuCurFreq;

        String strDisplayFreq = new String(" ");
        strDisplayFreq = mCpuCurFreq / 1000 + getResources().getString(R.string.str_cpu_freq_units);

        // update freq text
        mTvCurFreq.setText(strDisplayFreq);
    }

    /**
     * update CPU usage
     */
    private void updateCPUUsageDisplay() {

        // update usr/sys/idle
        mTvCpuUsrUsage.setText(getResources().getString(R.string.str_cpu_usage_usr) + " "
                + mFormatPercent.format((float) mCpuUsageList.get(CPU_INDEX_USR).get(0))
                + getResources().getString(R.string.str_percent_symbol));
        mTvCpuSysUsage.setText(getResources().getString(R.string.str_cpu_usage_sys) + " "
                + mFormatPercent.format((float) mCpuUsageList.get(CPU_INDEX_SYS).get(0))
                + getResources().getString(R.string.str_percent_symbol));
        mTvCpuIdleUsage.setText(getResources().getString(R.string.str_cpu_usage_idle) + " "
                + mFormatPercent.format((float) mCpuUsageList.get(CPU_INDEX_IDLE).get(0))
                + getResources().getString(R.string.str_percent_symbol));

        // update dashBoard
        for (int i = 0; i < mCpuCoreTotal; ++i) {
            // Round CPU usage
        	
        	Float f = 0.0f;
        	if (mIronPrimeCT)//IronPrimeCT
        	{
        		f = mCpuUsageList.get(i + CPU_INDEX_CPU).get(0) + 0.5f;
        	}
        	else
        	{
        		f = mCpuUsageList.get(i + CPU_INDEX_CPU0).get(0) + 0.5f;
        	}
            
            String strCpuUsage = String.valueOf(f.intValue())
                    + getResources().getString(R.string.str_percent_symbol);
            mTvArray[i].setText(strCpuUsage);
        }
    }

    /*
     * update data on graphic
     */
    private void updateGraphic() {

        // update graphic

        if (1 == mCpuCoreTotal) {
        	
        	if (mIronPrimeCT)//IronPrimeCT
        	{
        		mDg1.updateCpuVector(mCpuUsageList.get(CPU_INDEX_CPU));
        	}
        	else
        	{
        		mDg1.updateCpuVector(mCpuUsageList.get(CPU_INDEX_CPU0));
        	}
            
            mDg1.invalidate();
        } else if (2 == mCpuCoreTotal) {
            mDg1.updateCpuVector(mCpuUsageList.get(CPU_INDEX_CPU0));
            mDg1.invalidate();

            mDg2.updateCpuVector(mCpuUsageList.get(CPU_INDEX_CPU1));
            mDg2.invalidate();
        }
    }
}
