package com.motorola.mmsp.taskmanager;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;


public class TaskManagerActivity extends TabActivity {

    private static final String TAG = "TaskManager";
    private static final int TAB_INDEX_APPLICATIONS = 0;
    // private static final int TAB_INDEX_PROCESSES = 1;
    private TabHost mTabHost = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    
        mTabHost = getTabHost();
    
        Intent intent0 = new Intent(
            "com.motorola.taskmanager.ApplicationActivity");
        intent0.setClass(this, ApplicationActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("tab_applications").setIndicator(
            getString(R.string.tabTasks),
            getResources().getDrawable(R.drawable.ic_tab_task))
            .setContent(intent0));
    
        Intent intent1 = new Intent("com.motorola.taskmanager.ProcessActivity");
        intent1.setClass(this, ProcessActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("tab_processes").setIndicator(
            getString(R.string.tabProcesses),
            getResources().getDrawable(R.drawable.ic_tab_process)).setContent(intent1));
    
        mTabHost.setCurrentTab(TAB_INDEX_APPLICATIONS);
        // added by amt_sunli 2012-12-24 SWITCHUITWO-329 begin
        Common.setMOLCTModel();
        // added by amt_sunli 2012-12-24 SWITCHUITWO-329 end
        //zengkun added for DPD.B-522 & DPD.B-614
        for (int i = 0; i<2; ++i) {
            View view = mTabHost.getTabWidget().getChildAt(i);
            if (view == null) {
                Common.Log(TAG, "Error! view = null!");
                break;
            }

            ((TextView)view.findViewById(android.R.id.title)).setTextSize(12);
            ((ImageView)view.findViewById(android.R.id.icon)).setPadding(0, -7, 0, 0);
        }
        //zengkun added for DPD.B-522 & DPD.B-614

        Common.Log(TAG, "The default page is about application info.");
    }

}
