/*
 * Copyright (c) 2011 Motorola, Inc. All Rights Reserved The contents of this
 * file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.filemanager;

import android.app.ActivityGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;

import com.motorola.filemanager.local.FileManagerActivity;
import com.motorola.filemanager.samba.RemoteFileManager;

public class FileManagerContent extends ActivityGroup {
    public final static String ACTION_COPY = "com.mot.FileManagerIntents.ACTION_COPY";
    public final static String ACTION_PASTE = "com.mot.FileManagerIntents.ACTION_PASTE";
    public final static String ACTION_CUT = "com.mot.FileManagerIntents.ACTION_CUT";
    public final static String ACTION_DELETE = "com.mot.FileManagerIntents.ACTION_DELETE";
    public final static String ACTION_TRANSFERFINISHED = "com.mot.FileManagerIntents.ACTION_TRANSFERFINISHED";
    public final static String ACTION_SHOWDOWNLOADPAGE = "com.mot.FileManagerIntents.ACTION_SHOWDOWNLOADPAGE";
    public final static String ACTION_SELECTFILE = "com.mot.FileManagerIntents.ACTION_SELECTFILE";
    public final static String ACTION_SELECTFOLDER = "com.mot.FileManagerIntents.ACTION_SELECTFOLDER";
    // USB Host
    public final static String ACTION_USBDISKMOUNTED = "com.motorola.intent.action.USB_DISK_MOUNTED_NOTIFY";
    // USB Host End
    public final static String ACTION_MCUPLOADFROMEMMC = "com.mot.FileManagerIntents.ACTION_MCUPLOADFROMEMMC";
    public final static String ACTION_MCUPLOADFROMSDCARD = "com.mot.FileManagerIntents.ACTION_MCUPLOADFROMSDCARD";
    public final static String ACTION_MCSELECTUPLOADFILES = "com.mot.FileManagerIntents.ACTION_MCSELECTUPLOADFILES";
    public final static String ACTION_MCSTARTUPLOAD = "com.mot.FileManagerIntents.MCACTION_STARTUPLOAD";
    public final static String ACTION_MCOPENDIRECTORY = "com.mot.FileManagerIntents.ACTION_MCOPENDIRECTORY";

    public final static String EXTRA_KEY_PASTEREASON = "PASTEREASON";
    public final static String EXTRA_VAL_REASONCOPY = "REASONCOPY";
    public final static String EXTRA_VAL_REASONCUT = "REASONCUT";

    public final static String EXTRA_KEY_TRANSFERRESULT = "TRANSFERRESULT";

    public final static String EXTRA_KEY_SOURCEFILENAME = "SOURCEFILENAME";
    public final static String EXTRA_KEY_DESTFILENAME = "DESTFILENAME";

    public static final String EXTRA_KEY_MC_UPLOAD_DIR = "MC_UPLOAD_DIR";

    private static final String PREFIX_SMB_FILE = "smb://";

    static private String mCurrentContent = null;
    static private Context mContext = null;
    static private ActivityGroup mInstance = null;
    private Intent mPasteIntent = null;
    static private View mMoveNCancelview = null;
    private Button mPasteButton;
    private Button mCancelButton;
    private FrameLayout mContentPage;
    private View mFileChooserPanel;
    private Button mFileChooserCancelButton;

    private static final String TAG = "FileManagerContent: ";

    @Override
    protected void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	mContext = this;
	mInstance = this;
	
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.file_manager_content);
	mContentPage = (FrameLayout) findViewById(R.id.content);
	if (FileManagerApp.getLaunchMode() == FileManagerApp.NORMAL_MODE) {
	    mMoveNCancelview = findViewById(R.id.move_n_cancel);
	    mPasteButton = (Button) findViewById(R.id.paste_button);
	    mPasteButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		public void onClick(View v) {
		    if (mCurrentContent == null) {
			FileManagerApp
				.log(TAG
					+ "mCurrentContent is null in pasteButton's onClick");
			cancelPaste();
			return;
		    } else if (mCurrentContent.equals(FileManager.ID_LOCAL)) {
			mPasteIntent
				.setClass(
					mContext,
					com.motorola.filemanager.local.FileManagerActivity.class);
		    } else if (mCurrentContent.equals(FileManager.ID_REMOTE)) {
			mPasteIntent
				.setClass(
					mContext,
					com.motorola.filemanager.samba.RemoteFileManager.class);
		    } else if (mCurrentContent.equals(FileManager.ID_EXTSDCARD)) {
			mPasteIntent
				.setClass(
					mContext,
					com.motorola.filemanager.local.FileManagerActivity.class);
		    } else if (mCurrentContent.equals(FileManager.ID_USB)) {
			mPasteIntent
				.setClass(
					mContext,
					com.motorola.filemanager.local.FileManagerActivity.class);
		    }
		    FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
		    getLocalActivityManager().startActivity(mCurrentContent,
			    mPasteIntent);
		    cancelPaste();
		}
	    });
	    mCancelButton = (Button) findViewById(R.id.cancel_button);
	    mCancelButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		public void onClick(View v) {
		    cancelPaste();
		}
	    });
	    showPasteButton();
	} else {
	    findViewById(R.id.move_n_cancel).setVisibility(View.GONE);
	}
	mFileChooserPanel = findViewById(R.id.file_chooser_panel);
	mFileChooserCancelButton = (Button) findViewById(R.id.file_chooser_cancel);
	mFileChooserCancelButton.setOnClickListener(new View.OnClickListener() {
	    // @Override
	    public void onClick(View v) {
		finish();
	    }
	});

	initContentPage();

	IntentFilter iFilt = new IntentFilter();
	iFilt.addAction(FileManagerContent.ACTION_TRANSFERFINISHED);
	registerReceiver(mBroadCaseReveiver, iFilt);
    }

    private void initContentPage() {
	Intent intent = getIntent();
	String intentAction = intent.getAction();

	if (intentAction != null) {
	    switch (FileManagerApp.getLaunchMode()) {
	    case FileManagerApp.SELECT_FILE_MODE:
	    case FileManagerApp.SELECT_FOLDER_MODE:
		mFileChooserPanel.setVisibility(View.VISIBLE);
		break;
	    case FileManagerApp.SELECT_GET_CONTENT:
		break;
	    case FileManagerApp.NORMAL_MODE:
		mFileChooserPanel.setVisibility(View.GONE);
		break;
	    }

	    mCurrentContent = intent
		    .getStringExtra(FileManager.EXTRA_KEY_CONTENT);
	    Intent launchIntent = null;
	    if (FileManager.ID_LOCAL.equals(mCurrentContent)) {
		launchIntent = new Intent(this, FileManagerActivity.class);
	    } else if (FileManager.ID_REMOTE.equals(mCurrentContent)) {
		launchIntent = new Intent(this, RemoteFileManager.class);
	    } else if (FileManager.ID_EXTSDCARD.equals(mCurrentContent)) {
		launchIntent = new Intent(this, FileManagerActivity.class);
	    } else if (FileManager.ID_USB.equals(mCurrentContent)) {
		launchIntent = new Intent(this, FileManagerActivity.class);
	    } else {
		// didn't match any screen we know so exit
		finish();
		return;
	    }
	    launchIntent.setAction(FileManager.ACTION_RESTART_ACTIVITY);
	    Window w = getLocalActivityManager().startActivity(mCurrentContent,
		    launchIntent);
	    showContentPage(w);
	}
    }

    private void showContentPage(Window w) {
	mContentPage.setVisibility(View.VISIBLE);
	View wd = w != null ? w.getDecorView() : null;
	if (wd != null) {
	    wd.setVisibility(View.VISIBLE);
	    wd.setFocusableInTouchMode(true);
	    ((ViewGroup) wd)
		    .setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
	    mContentPage.removeAllViews();
	    mContentPage.addView(wd, new ViewGroup.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT,
		    ViewGroup.LayoutParams.MATCH_PARENT));
	    wd.requestFocus();
	} else {
	    // error creating view, exit
	    FileManagerApp.log(TAG + "Could not activate view, exiting");
	    finish();
	}
    }

    static public Context getCurContext() {
	return mContext;
    }

    static public ActivityGroup getActivityGroup() {
	return mInstance;
    }

    @Override
    public void onDestroy() {
	try {
	    unregisterReceiver(mBroadCaseReveiver);
	} catch (IllegalArgumentException e) {
	    FileManagerApp.log(TAG + "Could not unregister BroadCastReceiver");
	}
	super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
	String intentAction = intent.getAction();
	if (intentAction == null) {
	    // no action needed, Intent is null
	    FileManagerApp.log(TAG + "Received a null Intent in onNewIntent");
	} else if (intentAction.equals(ACTION_COPY)) {
	    FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE);
	    FileManagerApp.setPasteFiles(intent
		    .getStringArrayListExtra(EXTRA_KEY_SOURCEFILENAME));
	    showPasteButton();
	} else if (intentAction.equals(ACTION_CUT)) {
	    FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
	    FileManagerApp.setPasteFiles(intent
		    .getStringArrayListExtra(EXTRA_KEY_SOURCEFILENAME));
	    showPasteButton();
	} else if (intentAction.equals(ACTION_SHOWDOWNLOADPAGE)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD, -1);
	    Intent intent1 = new Intent(this, FileManagerActivity.class);
	    intent1.setAction(ACTION_SHOWDOWNLOADPAGE);
	    getLocalActivityManager().startActivity(FileManager.ID_LOCAL,
		    intent1);
	} else if (intentAction.equals(ACTION_MCUPLOADFROMEMMC)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD, -1);
	    intent.setClass(this, FileManagerActivity.class);
	    Window w = getLocalActivityManager().startActivity(
		    FileManager.ID_LOCAL, intent);
	    showContentPage(w);
	} else if (intentAction.equals(ACTION_MCUPLOADFROMSDCARD)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD, -1);
	    intent.setClass(this, FileManagerActivity.class);
	    Window w = getLocalActivityManager().startActivity(
		    FileManager.ID_EXTSDCARD, intent);
	    showContentPage(w);
	} else if (intentAction.equals(ACTION_MCOPENDIRECTORY)) {
	    FileManagerApp.setPhoneStoragePath(
		    FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD, -1);
	    mCurrentContent = FileManager.ID_LOCAL;
	    intent.setClass(this, FileManagerActivity.class);
	    Window w = getLocalActivityManager().startActivity(
		    FileManager.ID_LOCAL, intent);
	    showContentPage(w);
	}
    }

    private BroadcastReceiver mBroadCaseReveiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String intentAction = intent.getAction();
	    if (intentAction == null) {
		// Intent is null, no action needed
		FileManagerApp.log(TAG
			+ "Received a null Intent in BroadcastReceiver");
	    } else if (intentAction.equals(ACTION_TRANSFERFINISHED)) {
		String destination = findDestAct(intent);
		if (destination.equals(FileManager.ID_LOCAL)) {
		    intent.setClass(
			    mContext,
			    com.motorola.filemanager.local.FileManagerActivity.class);
		} else if (destination.equals(FileManager.ID_EXTSDCARD)) {
		    intent.setClass(
			    mContext,
			    com.motorola.filemanager.local.FileManagerActivity.class);
		} else if (destination.equals(FileManager.ID_REMOTE)) {
		    intent.setClass(
			    mContext,
			    com.motorola.filemanager.samba.RemoteFileManager.class);
		} else if (destination.equals(FileManager.ID_USB)) {
		    intent.setClass(
			    mContext,
			    com.motorola.filemanager.local.FileManagerActivity.class);
		} else {
		    // doesn't match any screen we know. do not process.
		    return;
		}
		// Send copy finished or error message to destination now
		getLocalActivityManager().startActivity(destination, intent);
	    }
	}
    };

    private String findDestAct(Intent intent) {
	String destFileName = intent.getStringExtra(EXTRA_KEY_DESTFILENAME);
	if (destFileName != null) {
	    if (destFileName.startsWith(FileManagerApp.SD_CARD_EXT_DIR)) {
		return FileManager.ID_EXTSDCARD;
	    } else if (destFileName.startsWith(FileManagerApp.SD_CARD_DIR)) {
		return FileManager.ID_LOCAL;
	    } else if (destFileName.startsWith(PREFIX_SMB_FILE)) {
		return FileManager.ID_REMOTE;
	    }
	}
	return FileManager.ID_USB;
    }

    private void showPasteButton() {
	Intent intent = new Intent(this,
		com.motorola.filemanager.FileManagerContent.class);
	if (FileManagerApp.getPasteMode() == FileManagerApp.COPY_MODE) {
	    mPasteButton.setText(R.string.paste_button);
	    intent.putExtra(EXTRA_KEY_PASTEREASON, EXTRA_VAL_REASONCOPY);
	} else if (FileManagerApp.getPasteMode() == FileManagerApp.MOVE_MODE) {
	    mPasteButton.setText(R.string.move_button);
	    intent.putExtra(EXTRA_KEY_PASTEREASON, EXTRA_VAL_REASONCUT);
	} else {
	    // do not show paste button since intent is null
	    FileManagerApp.log(TAG + "Not in Paste Mode in showPasteButton");
	    mMoveNCancelview.setVisibility(View.GONE);
	    return;
	}
	intent.setAction(ACTION_PASTE);
	intent.putStringArrayListExtra(
		FileManagerContent.EXTRA_KEY_SOURCEFILENAME,
		FileManagerApp.getPasteFiles());
	mPasteIntent = intent;
	mCancelButton.setText(android.R.string.cancel);
	mMoveNCancelview.setVisibility(View.VISIBLE);
    }

    private void cancelPaste() {
	mPasteIntent = null;
	if (mMoveNCancelview != null) {
	    mMoveNCancelview.setVisibility(View.GONE);
	} else {
	    FileManagerApp.log(TAG
		    + "Launch mode is not normal, mMoveNCancelview is null");
	}
	FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
	FileManagerApp.setPasteFiles(null);
    }

    public static void hidePasteButton() {
	if (mMoveNCancelview != null) {
	    mMoveNCancelview.setVisibility(View.GONE);
	} else {
	    FileManagerApp.log(TAG
		    + "Launch mode is not normal, mMoveNCancelview is null");
	}
    }

    public static void displayPasteButton() {
	mMoveNCancelview.setVisibility(View.VISIBLE);
    }

    // temporarily use to build success in eclipse
    // temp fix for porting. Remove comments later
    private static final String ACTION_MEDIA_SCANNER_SCAN_FOLDER = "com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER";

    public void mediaScanFolder(String folder) {
	if (folder != null) {
	    if (folder.endsWith("/")) {
		// For scanning folders, MediaScanner does not like to have a
		// trailing "/"
		// So if the input string has a trailing "/" remove it
		folder = folder.substring(0, (folder.length() - 1));
	    }
	    Intent intent = new Intent(ACTION_MEDIA_SCANNER_SCAN_FOLDER,
		    Uri.parse(folder));
	    FileManagerApp.log(TAG + "mediaScanFolder " + folder + " "
		    + intent.toString());
	    try {
		this.sendBroadcast(intent);
	    } catch (android.content.ActivityNotFoundException ex) {
		ex.printStackTrace();
	    }
	}
    }

    public void mediaScanFile(String newFileName) {
	// Scan the file , in case it is not in directory
	Intent myIntent = new Intent(
		android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
		Uri.parse(newFileName));
	try {
	    this.sendBroadcast(myIntent);
	} catch (android.content.ActivityNotFoundException ex) {
	    ex.printStackTrace();
	}
	FileManagerApp.log(TAG + "mediaScanFile " + newFileName + " "
		+ myIntent.toString());
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
}
