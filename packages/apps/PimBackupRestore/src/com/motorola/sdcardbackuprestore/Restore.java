package com.motorola.sdcardbackuprestore;

import java.io.File;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.content.pm.IPackageInstallObserver;

public class Restore extends BaseActivity {

    private static final String TAG = "Restore";
    private static Restore sInstance = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        mOpt = Constants.RESTORE_ACTION;
        super.onCreate(savedInstanceState);
        sInstance = this;
        
    }
    
    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    public static Restore getInstance() {
        return sInstance;
    }

    @Override
	protected void startBackupRestoreService(int delOption) {
        Log.v(TAG, "Enter startBackupRestoreService()");
        Intent serviceIntent = new Intent(this, BackupRestoreService.class);
        
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ACTION, Constants.RESTORE_ACTION);
        bundle.putBoolean(Constants.CONTACTS, mIsContactsChecked);
        bundle.putBoolean(Constants.SMS, mIsSmsChecked);
        bundle.putBoolean(Constants.QUICKNOTE, mIsQuickNoteChecked);
        bundle.putBoolean(Constants.CALENDAR, mIsCalendarChecked);
        bundle.putBoolean(Constants.APP, mIsAppChecked);
        bundle.putBoolean(Constants.BOOKMARK, mIsBookmarkChecked);
        bundle.putInt(Constants.DEL_OPTION, delOption);
        serviceIntent.putExtras(bundle);
        mIsServiceEnd = false;

        startService(serviceIntent);
    }

    @Override
	protected String getProgressMessage(int type) {
        return getString(R.string.restore_to_phone, getTypeName(type)); 
    }

    @Override
	protected String getInsufficientSpaceMsg() {
        return getString(R.string.insufficient_space, 
                getString(R.string.phone));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        sInstance = null;
    }

	@Override
	protected String getEmptyStorageShortMsg(int type) {
		String actionString = null;
		switch (type) {
		case Constants.CONTACTS_ACTION:
			actionString = getString(R.string.contacts);
			break;
		case Constants.SMS_ACTION:
			actionString = getString(R.string.sms);
			break;
		case Constants.QUICKNOTE_ACTION:
			actionString = getString(R.string.quicknote);
			break;
		case Constants.CALENDAR_ACTION:
			actionString = getString(R.string.calendar);
			break;
		case Constants.APP_ACTION:
			actionString = getString(R.string.app);
			break;
		case Constants.BOOKMARK_ACTION:
			actionString = getString(R.string.bookmark);
			break;
		default:
			break;
		}
		return getString(R.string.no_content_found, SdcardManager.hasInternalSdcard(this) ? getString(R.string.all_storage) : getString(R.string.sdcard), actionString);
	}

}
