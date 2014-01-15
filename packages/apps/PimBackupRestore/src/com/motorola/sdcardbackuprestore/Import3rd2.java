package com.motorola.sdcardbackuprestore;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;
import android.content.ContentResolver;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;



public class Import3rd2 extends BaseActivity2 {

    private static final String TAG = "Import3rd2";
    private static Import3rd2 sInstance = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        mOpt = Constants.IMPORT3RD_ACTION;
        // A16516 add IKQLTTCW-636
        Intent intent = getIntent();
        if (intent.getAction().equals("android.intent.action.VIEW")) {
        	// for clicking vcf file in File Manager
        	mClickVcfFile = true;
        	mSpecificVcardUri = intent.getData();
        	if (mSpecificVcardUri != null) {
            	if (mSelectedPathList == null) {
            		mSelectedPathList = new ArrayList<String>();
				}
                if (mSpecificVcardUri.getScheme().equals("file")){
                    mSelectedPathList.add(mSpecificVcardUri.getPath());
                    checkIfSdcard();
                } else {
                    String path = saveTempFile(mSpecificVcardUri);
                    if(path != null){
                        mSelectedPathList.add(path);
                        mSpecificVcardUri = Uri.parse("file://"+path);
                    }
                    mIsAltSdcard = false;
                }
            	mIsContactsChecked = true;
			}
        } else {
        	// for importing vcf or vmg file
            mSelectedPathList = intent.getStringArrayListExtra(Constants.PATH_LIST);
            if(mSelectedPathList != null && mSelectedPathList.get(0) != null &&
                mSelectedPathList.get(0).endsWith("vcf")){
                mClickVcfFile = true;
                mIsContactsChecked = true;
                checkIfSdcard();
            }
            mIsContactsChecked = intent.getBooleanExtra("contacts", false);
            if(mIsContactsChecked && mSelectedPathList == null){
                Uri mData = intent.getData();
                if(mData != null){
                    String path = saveTempFile(mData);
                    if(path != null){
                        mSelectedPathList = new ArrayList<String>();
                        mSelectedPathList.add(path);
                        mSpecificVcardUri = Uri.parse("file://"+path);
                        mClickVcfFile = true;
                        mIsAltSdcard = false;
                    }
                }
            }
            mIsSmsChecked = intent.getBooleanExtra("sms", false);
            accountName = intent.getStringExtra("accName");
            accountType = intent.getStringExtra("accType");
		}
        sInstance = this;
        // A16516 end IKQLTTCW-636
        super.onCreate(savedInstanceState);        
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    public static Import3rd2 getInstance() {
        return sInstance;
    }

    public void checkIfSdcard(){
        if (mSelectedPathList != null && mSelectedPathList.size() != 0) {
            String path = mSelectedPathList.get(0);
            int isAltSdcard = SdcardManager.isAltSdcard(this, path);
            if (isAltSdcard == SdcardManager.INNER) {
                mIsAltSdcard = false;
            } else if (isAltSdcard == SdcardManager.OUTTER) {
                mIsAltSdcard = true;
            }
        }
    }

    public String saveTempFile(Uri vcardUri){
        if(mConstants== null){
            mConstants = new Constants(this);
        }
        String tempDir = mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER;
        String tempFile = tempDir + "/attached.vcf";
        byte[] buffer = new byte[1024];
        ContentResolver resolver = this.getContentResolver();
        InputStream is = null;
        File fl = null;
        FileOutputStream os = null;
        try{
            is = resolver.openInputStream(vcardUri);
            File dir = new File(tempDir);
            if(!dir.exists()) {dir.mkdirs();}
            fl = new File(tempFile);
            if(!fl.exists()) {fl.createNewFile();}

            os = new FileOutputStream(fl);
            while(is.available() > 0){
                int readNumber = is.read(buffer);
                if(readNumber > 0){
                    os.write(buffer, 0, readNumber);
                } else{
                    break;
                }
            }
        }catch (FileNotFoundException e) {
            tempFile = null;
        }catch (IOException e) {
            tempFile = null;
        }finally{
            if(is != null){
                try{
                    is.close();
                } catch (IOException e) {
                }
            }
            if(os != null){
                try{
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return tempFile;
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
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ACTION, Constants.IMPORT3RD_ACTION);
        bundle.putStringArrayList(Constants.PATH_LIST, mSelectedPathList);
        bundle.putString(Constants.ACCOUNT_NAME, accountName);
        bundle.putString(Constants.ACCOUNT_TYPE, accountType);
        bundle.putInt(Constants.DEL_OPTION, delOption);
        // A16516 add IKQLTTCW-636
        bundle.putBoolean(Constants.CONTACTS, mIsContactsChecked);
        bundle.putBoolean(Constants.SMS, mIsSmsChecked);
        // A16516 end IKQLTTCW-636
        serviceIntent.putExtras(bundle);

        // A16516 add IKQLTTCW-636
        serviceIntent.putExtra(Constants.isFromOutside, true);

        startService(serviceIntent);
    }

    @Override
	protected String getProgressMessage(int type) {
        return getString(R.string.import3rd_from_sd, getTypeName(type)); 
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
			actionString = getString(R.string.available_backup_file);
			break;
		}
		return getString(R.string.no_content_found, getString(mStorageType), actionString);
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
