package com.motorola.sdcardbackuprestore;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Import3rd extends BaseActivity {

    private static final String TAG = "Import3rd";
    private static Import3rd sInstance = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        mOpt = Constants.IMPORT3RD_ACTION;
        super.onCreate(savedInstanceState);
        sInstance = this;
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    public static Import3rd getInstance() {
        return sInstance;
    }

    @Override
	protected void startBackupRestoreService(int delOption) {
        Log.v(TAG, "Enter startBackupRestoreService()");
        Intent serviceIntent = new Intent(this, BackupRestoreService.class);
        if (mIsAltSdcard) {
			mStorageType = R.string.outer_storage;
		} else {
			mStorageType = R.string.inner_storage;
		}
        setChecked();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ACTION, Constants.IMPORT3RD_ACTION);
        bundle.putStringArrayList(Constants.PATH_LIST, mSelectedPathList);
        bundle.putBoolean(Constants.CONTACTS, mIsContactsChecked);
        bundle.putBoolean(Constants.SMS, mIsSmsChecked);
        bundle.putBoolean(Constants.QUICKNOTE, mIsQuickNoteChecked);
        bundle.putBoolean(Constants.CALENDAR, mIsCalendarChecked);
        bundle.putBoolean(Constants.APP, mIsAppChecked);
        //bundle.putBoolean(Constants.BOOKMARK, mIsBookmarkChecked);
        bundle.putString(Constants.ACCOUNT_NAME, accountName);
        bundle.putString(Constants.ACCOUNT_TYPE, accountType);
        bundle.putInt(Constants.DEL_OPTION, delOption);
        serviceIntent.putExtras(bundle);
        mIsServiceEnd = false;

        startService(serviceIntent);
    }
    
    private void setChecked() {
    	String tmpString = null;
    	File tmpFile = null;
    	File[] files = null;
    	for (int i = 0; i < mSelectedPathList.size(); i++) {
    		tmpString = mSelectedPathList.get(i);
    		if (tmpString == null || tmpString.equals("")) {
				continue;
			}
    		tmpFile = new File(tmpString);
    		if (!tmpFile.exists()) {
				continue;
			}
    		if (tmpFile.isFile()) {
    			setEachItemChecked(tmpFile);
			} else if (tmpFile.isDirectory()) {
				files = tmpFile.listFiles();
                                if (null == files) {
                                    continue;
                                }
				for (int j = 0; j < files.length; j++) {
					if (files[j].isFile()) {
						setEachItemChecked(files[j]);
					} else {
						continue;
					}
				}
			}
		}
    }
    
    private void setEachItemChecked(File file) {
    	if (file.getName().endsWith(Constants.vcardExtern)) {
    		mIsContactsChecked = true;
		} else if (file.getName().endsWith(Constants.vmgExtern)) {
			mIsSmsChecked = true;
		} else if (file.getName().endsWith(Constants.qnExtern)) {
			mIsQuickNoteChecked = true;
		} else if (file.getName().endsWith(Constants.calExtern) || file.getName().endsWith(Constants.calCompatExtern)) {
			mIsCalendarChecked = true;
		} else if (file.getName().endsWith(Constants.bookmarkExtern)) {
			mIsBookmarkChecked = true;
		}
    }
    
    @Override
	protected String getProgressMessage(int type) {
        return getString(R.string.import3rd_from_sd, getTypeName(type)); 
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
		case Constants.BOOKMARK_ACTION:
			actionString = getString(R.string.bookmark);
			break;
		default:
			actionString = getString(R.string.available_backup_file);
			break;
		}
		return getString(R.string.no_content_found, getString(mStorageType), actionString);
	}

}
