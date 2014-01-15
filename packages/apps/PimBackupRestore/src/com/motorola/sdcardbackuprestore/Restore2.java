package com.motorola.sdcardbackuprestore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Restore2 extends BaseActivity2 {

    private static final String TAG = "Restore2";
    private static Restore2 sInstance = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Restore2 onCreate()");
        mOpt = Constants.RESTORE_ACTION;
        //A16516 add IKQLTTCW-636
        Intent intent = getIntent();

        mIsContactsChecked = intent.getBooleanExtra("contacts", false);
        mIsSmsChecked = intent.getBooleanExtra("sms", false);
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

    public static Restore2 getInstance() {
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
        bundle.putInt(Constants.DEL_OPTION, delOption);
        serviceIntent.putExtras(bundle);
        // A16516 add Constants.isFromOutside
        serviceIntent.putExtra(Constants.isFromOutside, true);

        startService(serviceIntent);
    }

    @Override
	protected String getProgressMessage(int type) {
        return getString(R.string.restore_to_phone, getTypeName(type)); 
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
		return getString(R.string.no_content_found, SdcardManager.hasInternalSdcard(this) ? getString(R.string.all_storage) : getString(R.string.sdcard), actionString);
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

}
