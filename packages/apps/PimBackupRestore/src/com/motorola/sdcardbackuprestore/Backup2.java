package com.motorola.sdcardbackuprestore;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Backup2 extends BaseActivity2 {

    private static final String TAG = "Backup2";
    private static Backup2 sInstance = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Backup2 onCreate()");
        mOpt = Constants.BACKUP_ACTION;
        // A16516 add IKQLTTCW-636
        Intent intent = getIntent();
        mIsContactsChecked = intent.getBooleanExtra("contacts", false);
        mIsSmsChecked = intent.getBooleanExtra("sms", false);
		mSpeContacts = intent.getBooleanExtra("specific_contacts", false);
		if (mSpeContacts) {
			showDialog(DIALOG_PREPARE);
		}
        Log.v(TAG, "Content from Contacts is " 
               + mIsContactsChecked + " and SMS is " + mIsSmsChecked);

        sInstance = this;
        // A16516 end IKQLTTCW-636

		super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    public static Backup2 getInstance() {
        return sInstance;
    }

    @Override
	protected void startBackupRestoreService(int delOption) {
        Log.v(TAG, "Enter startBackupRestoreService()");
        Intent serviceIntent = null;
        
        if (!mSpeContacts) {
        	serviceIntent = new Intent(Backup2.this, BackupRestoreService.class);
        } else {
			serviceIntent = getIntent();
			serviceIntent.setClass(Backup2.this, BackupRestoreService.class);
		}

        Bundle bundle = serviceIntent.getExtras();
        if (bundle == null) {
        	bundle = new Bundle();
		}
		bundle.putInt(Constants.ACTION, Constants.BACKUP_ACTION);
		bundle.putBoolean(Constants.CONTACTS, mIsContactsChecked);
		bundle.putBoolean(Constants.SMS, mIsSmsChecked);
		bundle.putBoolean(Constants.SPECIFIC_CONTACTS, mSpeContacts);
		bundle.putInt(Constants.DEL_OPTION, delOption);
		serviceIntent.putExtras(bundle);
		// A16516 add IKQLTTCW-636
		serviceIntent.putExtra(Constants.isFromOutside, true);

		startService(serviceIntent);
    }

    @Override
	protected String getProgressMessage(int type) {
        return getString(R.string.backup_to_sdcard, getTypeName(type), getString(mStorageType));
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
		default:
			break;
		}
		return getString(R.string.no_content_found, getString(R.string.phone), actionString);
    }

    @Override
	protected String getInsufficientSpaceMsg() {
        return getString(R.string.insufficient_space, getString(mStorageType));
    }
    

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        sInstance = null;
    }
    
}
