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

package com.motorola.filemanager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.utils.IconifiedText;
import com.motorola.filemanager.utils.IconifiedTextListAdapter;

public class FileManager extends Activity {
    public final static String ACTION_RESTART_ACTIVITY = "com.mot.FileManagerIntents.ACTION_RESTART_ACTIVITY";
    public final static String ACTION_SELECTFILE = "com.mot.FileManagerIntents.ACTION_SELECTFILE";
    public final static String ACTION_SELECTFOLDER = "com.mot.FileManagerIntents.ACTION_SELECTFOLDER";
    // USB Host
    public final static String ACTION_USBDISKMOUNTED = "com.motorola.intent.action.USB_DISK_MOUNTED_NOTIFY";
    // USB Host End

    public final static String EXTRA_KEY_RESULT = "FILEPATH";
    public final static String EXTRA_KEY_AUTHINFO = "AUTHINFO";

    public static final String EXTRA_KEY_DISKNUMBER = "DISK_NUMBER";
    public static final String EXTRA_KEY_CURRENT_USBPATH = "CURRENT_USBPATH";

    public static final String EXTRA_KEY_CONTENT = "CURRENT_CONTENT";
    public final static String EXTRA_IS_FROM = "isFrom";
    public final static String EXTRA_MYFAVORITE = "myfavorite";

    public static final String HIDEUNKOWNSETTING = "HideUnkownSetting";

    public static final String ID_LOCAL = "Local";
    public static final String ID_EXTSDCARD = "Extcard";
    public static final String ID_REMOTE = "Rmote";
    public static final String ID_PROTECTED = "Protected"; // work around to
							   // track if protected
							   // content.
    public static final String ID_USB = "Usb"; // USB Host

    private static String mCurrentContent = ID_LOCAL;

    public static final int USB_DISK_1 = 1;
    public static final int USB_DISK_2 = 2;
    public static final int USB_DISK_3 = 3;
    private static Context mContext = null;
    private static Activity mInstance = null;

    private View mFileChooserPanel;
    private Button mFileChooserCancelButton;
    private int mSelectedUsbIndex = -1;

    private static final String TAG = "FileManager: ";

    // private String TAG = "FileManager";
    private Handler mHighlightHandler = new Handler();
    // IKSTABLEFIVE-2633 - Start
    private List<IconifiedText> mHomePageStaticItems = new ArrayList<IconifiedText>();
    private List<IconifiedText> mHomePageCurrentList = new ArrayList<IconifiedText>();
    private AbsListView mHomePageListView;
    private IconifiedTextListAdapter mHomePageListAdapter = null;

    // IKSTABLEFIVE-2633 - End
    @Override
    protected void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	mContext = this;
	mInstance = this;

	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.file_manager);

	mFileChooserPanel = findViewById(R.id.file_chooser_panel);
	mFileChooserCancelButton = (Button) findViewById(R.id.file_chooser_cancel);
	mFileChooserCancelButton.setOnClickListener(new View.OnClickListener() {
	    // @Override
	    public void onClick(View v) {
		finish();
	    }
	});

	IntentFilter iFilt = new IntentFilter();
	iFilt.addAction(Intent.ACTION_MEDIA_MOUNTED);
	iFilt.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
	iFilt.addDataScheme("file");
	registerReceiver(mBroadCaseReveiver, iFilt);
	FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);

	initHomePage();
	showHomePage();
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

	if (FileManagerApp.getEMMCEnabled()) {
	    // Do not show Internal phone storage when no EMMC
	    IconifiedText internalmemory = new IconifiedText(
		    getString(R.string.internal_phone_storage), getResources()
			    .getDrawable(R.drawable.ic_thb_device));
	    internalmemory.setIsNormalFile(false);
	    mHomePageStaticItems.add(internalmemory);
	}

	IconifiedText sdcard = new IconifiedText(getString(R.string.sd_card),
		getResources().getDrawable(R.drawable.ic_thb_external_sdcard));
	sdcard.setIsNormalFile(false);
	mHomePageStaticItems.add(sdcard);

	Intent intent = getIntent();
	String intentAction = intent.getAction();
	if (intentAction != null) {
	    if (intentAction.equals(ACTION_SELECTFILE)) {
		FileManagerApp.setIsGridView(false);
		FileManagerApp.setLaunchMode(FileManagerApp.SELECT_FILE_MODE);
		FileManagerApp.setFavoriteMode(false);
		mFileChooserPanel.setVisibility(View.VISIBLE);
	    } else if (intentAction.equals(ACTION_SELECTFOLDER)) {
		FileManagerApp.setIsGridView(false);
		FileManagerApp.setLaunchMode(FileManagerApp.SELECT_FOLDER_MODE);
		String extraFrom = intent.getStringExtra(EXTRA_IS_FROM);
		if ((extraFrom != null) && (extraFrom.equals(EXTRA_MYFAVORITE))) {
		    FileManagerApp.setFavoriteMode(true);
		} else {
		    FileManagerApp.setFavoriteMode(false);
		}
		mFileChooserPanel.setVisibility(View.VISIBLE);
	    } else if (intentAction.equals(Intent.ACTION_GET_CONTENT)) {
		FileManagerApp.log(TAG + "intent is " + intent.getExtras());
		FileManagerApp.setLaunchMode(FileManagerApp.SELECT_GET_CONTENT);
		FileManagerApp.setFavoriteMode(false);
		((FileManagerApp) getApplication()).saveContentIntent(intent);
		((FileManagerApp) getApplication())
			.saveCallingPackageName(getCallingPackage());
	    } else {
		((FileManagerApp) getApplication()).resetGridViewMode();
		FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);
		FileManagerApp.setFavoriteMode(false);
		mFileChooserPanel.setVisibility(View.GONE);
	    }
	}

	// Add the static items to current list before checking for the dynamic
	// elements
	mHomePageCurrentList.addAll(mHomePageStaticItems);

	handleUsbNotifyIntent(); // USB host
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

	    if (file.equals(getString(R.string.internal_phone_storage))) {
		if (!Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED)) {
		    if (!isFinishing()) {
			(new AlertDialog.Builder(mContext))
				.setTitle(R.string.internal_phone_storage)
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
		    mCurrentContent = ID_LOCAL;
		    // showContentPage need to be called outside onClick to show
		    // highlighted item.
		    mHighlightHandler.postDelayed(new Runnable() {
			// @Override
			public void run() {
			    showContentPage();
			}
		    }, 100);
		}

	    } else if (file.equals(getString(R.string.sd_card))) {
		try {
		    mCurrentContent = ID_EXTSDCARD;
		    // showContentPage need to be called outside onClick to show
		    // highlighted item.
		    mHighlightHandler.postDelayed(new Runnable() {
			// @Override
			public void run() {
			    showContentPage();
			}
		    }, 100);
		} catch (NoClassDefFoundError e) {
		    Toast.makeText(mContext, "NoClassDefFoundError",
			    Toast.LENGTH_SHORT).show();
		}
	    } else if (file.contains(getString(R.string.usb_files))) {
		// USB Host
		if (!FileManagerApp.isUsbDiskConnected()) {
		    if (!isFinishing()) {
			(new AlertDialog.Builder(mContext))
				.setTitle(R.string.usbdisk)
				.setMessage(R.string.usbdisk_missing)
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
		    mCurrentContent = ID_USB;
		    try {
			mSelectedUsbIndex = Integer.valueOf(String.valueOf(file
				.charAt(file.length() - 1))) - 1;
			FileManagerApp.log(TAG + "Selected usb index"
				+ mSelectedUsbIndex);
		    } catch (NumberFormatException e) {
			e.printStackTrace();
		    }
		    // showContentPage need to be called outside onClick to show
		    // highlighted item.
		    mHighlightHandler.postDelayed(new Runnable() {
			// @Override
			public void run() {
			    showContentPage();
			}
		    }, 100);
		}
		// USB Host end
	    }
	}
    };

    private BroadcastReceiver mBroadCaseReveiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();
	    if (action == null) {
		FileManagerApp
			.log(TAG
				+ "intent action is null in BroadcastReceiver's onReceive");
		return;
	    }
	    if ((action.equals(Intent.ACTION_MEDIA_MOUNTED))
		    || (action.equals(Intent.ACTION_MEDIA_UNMOUNTED))) {
		FileManagerApp.log(TAG
			+ "Received USB mount intent in BroadcastReceiver()");
		handleUsbNotifyIntent();
	    }
	}
    };

    @Override
    public void onDestroy() {
	unregisterReceiver(mBroadCaseReveiver);
	// IKSTABLEFIVE-2633 - Start
	mHomePageCurrentList.clear();
	mHomePageStaticItems.clear();
	mHomePageListView.setAdapter(null);
	// IKSTABLEFIVE-2633 - End
	super.onDestroy();
    }

    private void showHomePage() {
	FileManagerApp.setPhoneStoragePath(0, -1);

	TextView homePageTitle = (TextView) findViewById(R.id.home_page_title);
	if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
	    homePageTitle.setText(R.string.select_file);
	} else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
	    homePageTitle.setText(R.string.select_folder);
	}
    }

    private void showContentPage() {
	if (mCurrentContent.equals(ID_LOCAL)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD, -1);
	} else if (mCurrentContent.equals(ID_EXTSDCARD)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD, -1);
	} else if (mCurrentContent.equals(ID_USB)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_USB_DISK,
		    mSelectedUsbIndex);
	}

	Intent intent = new Intent(this, FileManagerContent.class);
	intent.setAction(FileManager.ACTION_RESTART_ACTIVITY);
	intent.putExtra(FileManager.EXTRA_KEY_CONTENT, mCurrentContent);

	startActivity(intent);
    }

    public static String getCurrentContent() {
	return mCurrentContent;
    }

    public static Context getCurrentContext() {
	return mContext;
    }

    @Override
    protected void onNewIntent(Intent intent) {
	// no action needed, not expecting any new intents
	FileManagerApp.log(TAG + "Received a Intent in onNewIntent");
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
	FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);
	FileManagerApp.setFavoriteMode(false);

	if (mInstance != null) {
	    mInstance.setResult(RESULT_OK, iData);
	    mInstance.finish();
	}
	if (FileManagerContent.getActivityGroup() != null) {
	    (FileManagerContent.getActivityGroup()).finish();
	}
    }

    private int isUsbDiskAdded(String usbText) {
	FileManagerApp.log(TAG + "mHomePageCurrentList size" + " "
		+ mHomePageCurrentList.size());
	for (int index = 0; index < mHomePageCurrentList.size(); index++) {
	    if (mHomePageCurrentList.get(index).getText().equals(usbText)) {
		return index;
	    }
	}
	return -1;
    }

    private void handleUsbNotifyIntent() {
	FileManagerApp.setUsbDiskConnected(false);
	int usbDiskNum = 0;
	int index = -1;
	for (int i = 0; i < FileManagerApp.MAX_USB_DISK_NUM; i++) {
	    IconifiedText usbdisk = new IconifiedText(
		    getString(R.string.usb_files) + " " + (i + 1),
		    getResources().getDrawable(R.drawable.ic_thb_usb_disk));
	    usbdisk.setIsNormalFile(false);
	    if (FileManagerApp.isUsbDiskMounted(i)) {
		if (isUsbDiskAdded(usbdisk.getText()) == -1) {
		    mHomePageCurrentList.add(usbdisk);
		}
		usbDiskNum++;
		FileManagerApp.log(TAG
			+ "initHomepage(), USB disk is added. Index=" + i);
	    } else {
		FileManagerApp
			.log(TAG + "USB disk removed" + i + " " + usbdisk);
		index = isUsbDiskAdded(usbdisk.getText());
		if (index > -1) {
		    FileManagerApp.log(TAG + "USB disk is found. Index="
			    + index);
		    mHomePageCurrentList.remove(index);
		    mHomePageListAdapter.notifyDataSetChanged();
		}
	    }
	}
	if (usbDiskNum > 0) {
	    FileManagerApp.setUsbDiskConnected(true);
	    FileManagerApp.setUsbDisknum(usbDiskNum);
	    mHomePageListAdapter.notifyDataSetChanged();
	} else {
	    FileManagerApp.setUsbDiskConnected(false);
	    FileManagerApp.setUsbDisknum(usbDiskNum);
	}
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
}
