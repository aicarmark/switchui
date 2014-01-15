/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.FileManager;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import android.widget.Toast;

import com.motorola.FileManager.storage.MotoEnvironment;
import com.motorola.FileManager.utils.IconifiedText;
import com.motorola.FileManager.utils.IconifiedTextListAdapter;

public class FileManager extends Activity {
    public final static String ACTION_RESTART_ACTIVITY = "com.mot.FileManagerIntents.ACTION_RESTART_ACTIVITY";
    public final static String ACTION_SELECTFILE = "com.mot.FileManagerIntents.ACTION_SELECTFILE";
    public final static String ACTION_SELECTFOLDER = "com.mot.FileManagerIntents.ACTION_SELECTFOLDER";
    // USB Host
    public final static String ACTION_USBDISKMOUNTED = "com.motorola.intent.action.USB_DISK_MOUNTED_NOTIFY";
    // USB Host End

    public final static String EXTRA_KEY_RESULT = "FILEPATH";
    public final static String EXTRA_KEY_AUTHINFO = "AUTHINFO";

    public final static String EXTRA_KEY_HOMESYNC_TITLE = "HOMESYNC_TITLE";
    public final static String EXTRA_KEY_HOMESYNC_STEP = "HOMESYNC_STEP";

    public static final String EXTRA_KEY_DISKNUMBER = "DISK_NUMBER";
    public static final String EXTRA_KEY_CURRENT_USBPATH = "CURRENT_USBPATH";
    // private static final String EXTRA_KEY_DISK[] = new String[]{"DISK1",
    // "DISK2", "DISK3"};

    public static final String EXTRA_KEY_CONTENT = "CURRENT_CONTENT";

    public static final String HIDEUNKOWNSETTING = "HideUnkownSetting";

    public static final String ID_REMOTE = "Remote";
    public static final String ID_MOTOCONNECT = "MC";
    public static final String ID_PROTECTED = "Protected"; // work around to
							   // track if protected
							   // content.
    public static final String ID_USB = "Usb"; // USB Host
    public static final String ID_VMMFILEMANAGER = "VMMFileManager"; // VMM
								     // FileManager
								     // IKCBS-2656

    public final static String MEDIA_TYPE_SDCARD = "SDCARD";
    public final static String MEDIA_TYPE_FLASH = "FLASH";

    public static String mCurrentContent = Util.ID_EXTSDCARD;
    // private static boolean mEMMCPrimary= false;
    // private static boolean mInternalStoragePrimary = false;
    public static final int USB_DISK_1 = 1;
    public static final int USB_DISK_2 = 2;
    public static final int USB_DISK_3 = 3;
    static private Context mContext = null;
    static private Activity mInstance = null;
    private View mHomePage;

    public static boolean iEmmc = false;

    public static final int INTERNALSTORAGE = 0;
    public static final int SDCARD = 1;
    private static int mSearchOption = INTERNALSTORAGE;
    private static final String SEARCHOPTION = "search option";
    private static final String SETTINGS = "settings";

    private static final String TAG = "FileManager: ";
    // static private final IntentFilter FM_Event_Filter = new
    // IntentFilter(FileManager.ACTION_USBDISKMOUNTED);

    // private String TAG = "FileManager";
    private Handler mHighlightHandler = new Handler();
    // IKSTABLEFIVE-2633 - Start
    private List<IconifiedText> mHomePageStaticItems = new ArrayList<IconifiedText>();
    private List<IconifiedText> mHomePageCurrentList = new ArrayList<IconifiedText>();
    private AbsListView mHomePageListView;
    private IconifiedTextListAdapter mHomePageListAdapter = null;
    public static StorageManager sm;
    public static String internalStoragePath = null;
    public static String externalStoragePath = null;

    @Override
    protected void onCreate(Bundle icicle) {
	super.onCreate(icicle);
    ActionBar bar = getActionBar();
    bar.setDisplayHomeAsUpEnabled(true);
	initVar(this, this);
	setContentView(R.layout.file_manager);
	// setTitle(R.string.app_name);
	mHomePage = findViewById(R.id.home_page);
	sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
    if (sm == null) {
        Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
    }
	iEmmc = isEmmc(mContext);

	initHomePage();
	mHomePage.setVisibility(View.VISIBLE);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
       switch (item.getItemId()) 
       {        
          case android.R.id.home:            
             finish();           
             return true;        
          default:            
             return super.onOptionsItemSelected(item);    
       }
    }

    private static void initVar(Context context, Activity instance) {
	mContext = context;
	mInstance = instance;
    }

    private void initHomePage() {
	// IKSTABLEFIVE-2633
	mHomePageListView = (AbsListView) findViewById(R.id.home_page_list);
	mHomePageListView.setVisibility(View.VISIBLE);
	mHomePageListView.setTextFilterEnabled(true);
	mHomePageListView.requestFocus();
	mHomePageListView.requestFocusFromTouch();

	mHomePageListAdapter = new IconifiedTextListAdapter(this);
	mHomePageListAdapter.setListItems(mHomePageCurrentList,
		mHomePageListView.hasTextFilter());
	mHomePageListView.setAdapter(mHomePageListAdapter);

	mHomePageStaticItems.clear();
	mHomePageCurrentList.clear();

	if (iEmmc) {
	    IconifiedText internalmemory = new IconifiedText(
		    getString(R.string.internal_storage), getResources()
			    .getDrawable(R.drawable.ic_thb_device));
	    internalmemory.setIsNormalFile(false);
	    mHomePageStaticItems.add(internalmemory);
	}

	IconifiedText sdcard = new IconifiedText(
		getString(R.string.removable_sdcard), getResources()
			.getDrawable(R.drawable.ic_thb_external_sdcard));
	sdcard.setIsNormalFile(false);
	mHomePageStaticItems.add(sdcard);

	mHomePageCurrentList.addAll(mHomePageStaticItems);
	mHomePageListView.setOnItemClickListener(homePageListListener);
	mHomePageListAdapter.notifyDataSetChanged();
    }

    AdapterView.OnItemClickListener homePageListListener = new AdapterView.OnItemClickListener() {
    	// @Override
    	public void onItemClick(AdapterView<?> parent, View view, int position,
    		long id) {
    	    IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) parent
    		    .getAdapter();
    	    if (adapter == null) {
    		return;
    	    }
    	    IconifiedText text = (IconifiedText) adapter.getItem(position);
    	    String file = text.getText();
    	    if (file.equals(getString(R.string.internal_storage))) {
    	    	internalStoragePath = getInternalSd(mContext);
    			if (!sm.getVolumeState(internalStoragePath).equalsIgnoreCase(
    				Environment.MEDIA_MOUNTED)) {
    			    if (!isFinishing()) {
    				(new AlertDialog.Builder(mContext))
    					.setTitle(R.string.internal_storage)
    					.setMessage(
    						R.string.internal_phone_storage_unmounted)
    					.setNeutralButton(android.R.string.ok,
    						new DialogInterface.OnClickListener() {
    						    // @Override
    						    public void onClick(
    							    DialogInterface dialog,
    							    int which) {
    						    }
    						}).create().show();
    			    }
    			} else {
    			    mCurrentContent = Util.ID_LOCAL;
    			    mHighlightHandler.postDelayed(new Runnable() {
    				@Override
    				public void run() {
    				    showContentPage();
    				}
    			    }, 100);
    			}
    	    } else if (file.equals(getString(R.string.removable_sdcard))) {
    	    	externalStoragePath = getRemovableSd(mContext);
				if (!sm.getVolumeState(externalStoragePath).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
    				    if (!isFinishing()) {
    				    	if (sm.getVolumeState(externalStoragePath).equalsIgnoreCase(Environment.MEDIA_SHARED)) {
    				    		(new AlertDialog.Builder(mContext))
	    						.setTitle(R.string.removable_sdcard)
	    						.setMessage(R.string.sd_is_used_by_other)
	    						.setNeutralButton(android.R.string.ok,
	    							new DialogInterface.OnClickListener() {
	    							    // @Override
	    							    public void onClick(
	    								    DialogInterface dialog,
	    								    int which) {
	    							    }
	    							}).create().show();
    				    	} else { 				    	
    				    		(new AlertDialog.Builder(mContext))
		    						.setTitle(R.string.removable_sdcard)
		    						.setMessage(R.string.sdcard_not_exist)
		    						.setNeutralButton(android.R.string.ok,
		    							new DialogInterface.OnClickListener() {
		    							    // @Override
		    							    public void onClick(
		    								    DialogInterface dialog,
		    								    int which) {
		    							    }
		    							}).create().show();
    				    	}
    				    }
    				} else {
    					mCurrentContent = Util.ID_EXTSDCARD;
    					mHighlightHandler.postDelayed(new Runnable() {
    					    // @Override
    					    public void run() {
    						showContentPage();
    					    }
    					}, 100);
    	    	}
    	    }
    	}
    };

    @Override
    public void onDestroy() {
	// IKSTABLEFIVE-2633 - Start
	mHomePageCurrentList.clear();
	mHomePageStaticItems.clear();
	mHomePageListView.setAdapter(null);
	// IKSTABLEFIVE-2633 - End
	super.onDestroy();
    }

    private void showContentPage() {
	Intent intent = new Intent(this, FileManagerActivity.class);
	startActivity(intent);
    }

    public static void rtnPickerResult(String filePath, String authInfo) {
	Intent returnIntent = new Intent();
	returnIntent.putExtra(FileManager.EXTRA_KEY_RESULT, filePath);
	if (authInfo != null) {
	    returnIntent.putExtra(FileManager.EXTRA_KEY_AUTHINFO, authInfo);
	}
	rtnPickerResult(returnIntent);
    }

    public static void rtnPickerResult(Intent iData) {
	// FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);

	if (mInstance != null) {
	    mInstance.setResult(RESULT_OK, iData);
	    mInstance.finish();
	}
    }

    static public Context getCurContext() {
	return mContext;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
	switch (keyCode) {
	case KeyEvent.KEYCODE_MENU:
	    Log.d(TAG, "MENU long press");
	    return true;
	}
	return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_MENU) {
	    if (event != null && event.getRepeatCount() > 0) {
		return true;
	    }
	}
	return super.onKeyDown(keyCode, event);
    }

    public static void setmSearchOption(int searchOption) {
	mSearchOption = searchOption;
    }

    public void setSearchOption(int searchOption) {
	setmSearchOption(searchOption);
	SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
	SharedPreferences.Editor editor = setting.edit();
	editor.putInt(SEARCHOPTION, mSearchOption);
	editor.commit();
    }

    public static int getSearchOption() {
	return mSearchOption;
    }
    public static boolean isEmmc(Context mContext) {
    	if (mContext == null){
    		Log.d("","w23001- input context is null");
    		return false;
    	}
    	StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
            return false;
        }
        StorageVolume[] volumes = sm.getVolumeList();
        if (volumes == null) {
        	return false;
        }
		Log.d("","w23001- volume lenght =" + volumes.length);
		if (volumes.length > 1) {
				return true;
		} 
		return false;
    }
    
    public static String getRemovableSd(Context mContext) {
    	if(mContext == null) {
    		Log.d("", "w23001- input context is null");
    		return Environment.getExternalStorageDirectory().toString();
    	}
    	StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
            return Environment.getExternalStorageDirectory().toString();
        }
        
        StorageVolume[] volumes = sm.getVolumeList();
        if (volumes == null) {
        	Log.d("","w23001- volumes is null");
        	return Environment.getExternalStorageDirectory().toString();
        }
        for (StorageVolume sv: volumes) {
            // only monitor non emulated and non usb disk storage
            if (sv.isRemovable()) {
                String path = sv.getPath();
                Log.d("", "w23001-external path = " + path);
                return path;
            }
        }
        //error case
        Log.d("", "w23001- storage error -2: Fail to get internal memory path! use default: "+Environment.getExternalStorageDirectory().toString());
        return Environment.getExternalStorageDirectory().toString();
    }
    
    public static String getInternalSd(Context mContext) {    	
    	if(mContext == null) {
    		Log.d("", "w23001- input context is null");
    		return Environment.getExternalStorageDirectory().toString();
    	}
    	StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
            return Environment.getExternalStorageDirectory().toString();     
        }
        StorageVolume[] volumes = sm.getVolumeList();
        if (volumes == null) {
        	Log.d("","w23001- volumes is null");
        	return Environment.getExternalStorageDirectory().toString();
        }
        for (StorageVolume sv: volumes) {
            // only monitor non emulated and non usb disk storage
            if (!sv.isRemovable()) {
                String path = sv.getPath();
                Log.d("", "w23001-intern path = " + path);
                return path;
            }
        }
        //error case
        Log.d("", "w23001- storage error -2: Fail to get internal memory path! use default: "+Environment.getExternalStorageDirectory().toString());
        return Environment.getExternalStorageDirectory().toString();
    }
}
