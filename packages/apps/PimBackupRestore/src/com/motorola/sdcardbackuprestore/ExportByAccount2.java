package com.motorola.sdcardbackuprestore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ExportByAccount2 extends BaseActivity2 {

	private static final String TAG = "ExportByAccount2";
    private static ExportByAccount2 sInstance = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "ExportByAccount2 onCreate()");
        mOpt = Constants.EXPORTBYACCOUNT_ACTION;
        Intent intent = getIntent();
        checkedNames = intent.getStringArrayExtra(Constants.CHECKED_ACCOUNT_NAMES);
        sInstance = this;
        
        super.onCreate(savedInstanceState);
    }

	@Override
	protected void startBackupRestoreService(int delOption) {
		Log.v(TAG, "Enter startBackupRestoreService()");

        Intent serviceIntent = new Intent(this, BackupRestoreService.class);

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ACTION, Constants.EXPORTBYACCOUNT_ACTION);
        bundle.putStringArray(Constants.CHECKED_ACCOUNT_NAMES, checkedNames);
        bundle.putInt(Constants.DEL_OPTION, delOption);
        serviceIntent.putExtras(bundle);
        // A16516 add IKQLTTCW-636
        serviceIntent.putExtra(Constants.isFromOutside, true);

        startService(serviceIntent);

	}
	
    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    public static ExportByAccount2 getInstance() {
        return sInstance;
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
		return getString(R.string.no_content_found, getString(mStorageType), actionString);
	}

	@Override
	protected String getInsufficientSpaceMsg() {
		 // modified by amt_jiayuanyuan 2013-01-05 SWITCHUITWO-447 begin
		return getString(R.string.insufficient_space, getString(mStorageType));
		 // modified by amt_jiayuanyuan 2013-01-05 SWITCHUITWO-447 end
	}

	@Override
	protected String getProgressMessage(int type) {
		return getString(R.string.export_to_sdcard, getTypeName(type), getString(mStorageType));
	}

}
