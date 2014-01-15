package com.motorola.mmsp.taskmanager;



import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ProcessActivity extends ListActivity implements
	View.OnClickListener,DialogInterface.OnCancelListener {
    private static final String TAG = "ProcessActivity";

    private static final int HightPRI = 8;
    public String Confirm_String;

    // Custom Adapter used for managing items in the list
    private ProcessAndTaskAdapter mAppInfoAdapter;

    // list of process info
    private ListView mProcessInfoList;
    // button of force stop
    private Button mKillProcessButton;
    // button of refresh
    private Button mRefreshButton;
    // listen updating of process info
    private packageIntentReceiver mReceiver;
    // package info according to running process info
    List<ProcessAndTaskInfo> mpackagename_pid_list;

    public PackageManager mPm = null;
    public ActivityManager mactivityManager = null;

    // default icon to display when applications info is null
    private static Drawable mDefaultAppIcon;

    // temporary dialog displayed while the application info loads
    private static final int DLG_BASE = 0;
    private static final int DLG_LOADING = DLG_BASE + 1;
    /* if first time launch this activity,we need create list and need not update list on resume */
    private boolean firstLaunchFlag = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	Common.Log(TAG, "onCreate");
	super.onCreate(savedInstanceState);
	/* set main UI */
	setContentView(R.layout.process);
	
	/* set flag of first launch this activity */
	firstLaunchFlag = true;
	showDialog(DLG_LOADING); 

	/* init info */
	mPm = getPackageManager();
	mactivityManager = (ActivityManager) this
		.getSystemService(ACTIVITY_SERVICE);
	mDefaultAppIcon =Resources.getSystem().getDrawable(
                android.R.drawable.sym_def_app_icon);
	/* create package pid map list */
	mpackagename_pid_list = new ArrayList<ProcessAndTaskInfo>();
	createPackagePidMap(mpackagename_pid_list);
	/* list view info */
	mAppInfoAdapter = new ProcessAndTaskAdapter(this, mpackagename_pid_list);
	mProcessInfoList = this.getListView();
	mProcessInfoList.setAdapter(mAppInfoAdapter);
	
	dismissDialog(DLG_LOADING);

	/* button info */
	// mKillProcessButton = ((Button)
	// findViewById(R.id.Button_End_Process));
	// mKillProcessButton.setOnClickListener(this);
	mRefreshButton = ((Button) findViewById(R.id.Button_Refresh_Process));
	mRefreshButton.setOnClickListener(this);

	/* add listener of restart task */
	mReceiver = new packageIntentReceiver();
    }

    @Override
    public void onResume() {
	Common.Log(TAG, "onResume");
	super.onResume();
	// register receiver
	mReceiver.registerReceiver();
	/* task manager wake up form activity stack  */
	if (!firstLaunchFlag)
	{
	    // update processes
	    updateRunningProcessList();  
	}
	firstLaunchFlag = false;	
    }

    @Override
    public void onPause() {
	Common.Log(TAG, "onPause");
	super.onPause();
	// register receiver here
	unregisterReceiver(mReceiver);
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DLG_LOADING) {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dlg.setMessage(getText(R.string.loading));
            dlg.setIndeterminate(true);        
            dlg.setOnCancelListener(this);
            return dlg;
        }
        return null;
    }    
   
    // Finish the activity if the user presses the back button to cancel the activity
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    /*
     * Method implementing functionality of buttons clicked
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View v) {
	/* click the button kill process */
	if (v == mKillProcessButton) {
	    for (int i = 0; i < mProcessInfoList.getAdapter().getCount(); i++) {
		ProcessAndTaskInfo ProcessItem = (ProcessAndTaskInfo) (mProcessInfoList
			.getAdapter().getItem(i));
		if (!ProcessItem.checked)
		    continue;
		Common.killProcess(mactivityManager, ProcessItem);
	    }
	}
	/* click the button refresh */
	else if (v == mRefreshButton) {
	    updateRunningProcessList();
	}
    }

    /* select all of checkbox */
    private void createPackagePidMap(List<ProcessAndTaskInfo> PackPid_Map_List) {
	HashMap<String, String> mAppName_PersistMap = new HashMap<String, String>();

	/* clear the list first */
	PackPid_Map_List.clear();

	List<ActivityManager.RunningAppProcessInfo> ProcessList = mactivityManager
		.getRunningAppProcesses();

	mAppName_PersistMap = Common.createMapTableofApp(mPm, mactivityManager);

	printProcessList(ProcessList);

	/* search process list */
	for (int i = 0; i < ProcessList.size(); i++) {	   
	    /* search package list every process */
	    for (int j = 0; j < ProcessList.get(i).pkgList.length; j++) {
		/* check package name */
		if (isPackageInList(PackPid_Map_List,
			ProcessList.get(i).pkgList[j]))
		    continue;

		/* set value of list item */
		ProcessAndTaskInfo mMap = new ProcessAndTaskInfo();
		
		/* check application name */
		String appName = null;
		ApplicationInfo info = null;

		try {
		    info = mPm.getApplicationInfo(
			    ProcessList.get(i).pkgList[j], 0);
		} catch (NameNotFoundException e) {
		    appName = ProcessList.get(i).pkgList[j];
		    mMap.appicon = mDefaultAppIcon;
		}

		/* if get application failed */
		if (info != null) {
		    if (Common.isAppInList(PackPid_Map_List, info
			    .loadLabel(mPm).toString()))
			continue;
		    appName = info.loadLabel(mPm).toString();
		    mMap.appicon = info.loadIcon(mPm);
		}

                if (appName == null){
                    Common.Log(TAG, "appName is null.");
                    continue;
                }
             
                if(appName.trim().isEmpty()){
                    Common.Log(TAG, "appName is empty.");
                    continue;
                }

		mMap.appname = appName;
		mMap.packagename = ProcessList.get(i).pkgList[j];
		mMap.pid = ProcessList.get(i).pid;
		mMap.importance = ProcessList.get(i).importance;
		mMap.checked = false;
		/* comment below code because do not allow end process,
		 * so no need to check if process is persistent
                if (info == null) {
                    Log(TAG, "application info is null: " + appName);
                    mMap.isPersistent = true;
                }
                else {
        	    if (Common.isPersistentRuningTask(mPm, info,
        			mAppName_PersistMap)) {
        		    mMap.isPersistent = true;
                    } else {
        		    mMap.isPersistent = false;
                    }
                }
                */
                 
                mMap.checkvisibility = false;

                /* add item to the list */
                PackPid_Map_List.add(mMap);
	    }
	}
        /* sort by displayname */
        Comparator<ProcessAndTaskInfo> comp = new DisplayNameCompare();
        Collections.sort(PackPid_Map_List, comp);
    }

    /* check if package is in list */
    private boolean isPackageInList(List<ProcessAndTaskInfo> PackPid_Map_List,
	    String packagename) {
	int i = 0;

	/* check the input parameter */
	if (null == PackPid_Map_List || null == packagename)
	    return false;

	for (i = 0; i < PackPid_Map_List.size(); i++) {
	    if (PackPid_Map_List.get(i).packagename.equals(packagename))
		break;
	}

	if (i >= PackPid_Map_List.size()) {
	    return false;
	} else {
	    Common.Log(TAG, "Package is in the list " + packagename);
	    return true;
	}
    }

    /**
     * Receives notifications when applications are restarted.
     */
    private class packageIntentReceiver extends BroadcastReceiver {
	void registerReceiver() {
	    IntentFilter filter = new IntentFilter(
		    Intent.ACTION_PACKAGE_RESTARTED);
	    filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
	    filter.addDataScheme("package");
	    ProcessActivity.this.registerReceiver(this, filter);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	    Common.Log(TAG, "Received intent broadcaster:" + intent.toString());
	    updateRunningProcessList();
	}
    }

    /* update Running Process into */
    private void updateRunningProcessList() {
	Common.Log(TAG, "updateRunningProcessList");
	showDialog(DLG_LOADING); 
	/* recreate the list of package */
	createPackagePidMap(mpackagename_pid_list);

	/* refresh process UI */
	mAppInfoAdapter.updateList(mpackagename_pid_list);
	dismissDialog(DLG_LOADING);
    }

    /* print Process Info List */
    private void printProcessList(
	    List<ActivityManager.RunningAppProcessInfo> ProcessList) {
	for (int i = 0; i < ProcessList.size(); i++) {
	    Common.Log(TAG, "Process Name " + Integer.toString(i) + " = "
		    + ProcessList.get(i).processName);
	    Common.Log(TAG, "Process Importance = "
		    + Integer.toString(ProcessList.get(i).importance));
	    Common.Log(TAG, "Process pid = "
		    + Integer.toString(ProcessList.get(i).pid));
	    for (int j = 0; j < ProcessList.get(i).pkgList.length; j++) {
		Common.Log(TAG, "Package Name " + Integer.toString(j) + " = "
			+ ProcessList.get(i).pkgList[j]);
	    }
	}
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
	Common.Log(TAG, "OnListItemClick");
	ProcessAndTaskInfo currentItem = (ProcessAndTaskInfo) l.getAdapter()
		.getItem(position);

	/* click the Persistent item of list ,that does not work */
	if (currentItem.isPersistent) {
	    return;
	}

	/* change check of list item */
	currentItem.checked = !currentItem.checked;

	mAppInfoAdapter.updateList();

	return;
    }

    /*
    private int getOmmadjByPid(int pid) {
	String finename = "/proc/" + Integer.toString(pid) + "/oom_adj";
	File file = new File(finename);
	Common.Log(TAG, finename);

	InputStream in = null;
	try {
	    in = new FileInputStream(file);
	} catch (FileNotFoundException e1) {
	    // TODO Auto-generated catch block
	    return 0xff;
	}

	String oom_adj;
	char oom_adj_char[] = new char[10];
	int tempbyte;
	int length = 0;
	try {
	    while ((tempbyte = in.read()) != -1) {
		oom_adj_char[length] = (char) tempbyte;
		length++;
	    }
	    in.close();
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    return 0xff;
	}
	oom_adj = String.valueOf(oom_adj_char, 0, length - 1);
	return Integer.parseInt(oom_adj);
    }*/

  //public boolean IsHightPRI(int pid) {
	//	int oom_adj = getOmmadjByPid(pid);
	//		if (oom_adj > HightPRI)
	//    	return false;
	//	return true;
  //}

}
