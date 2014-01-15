package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

import android.R.integer;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.ContentUris;

import com.android.vcard2.exception.VCardInvalidLineException;
import com.motorola.con2vcard.VCardManager;
import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ContactsManager {

    private static final String TAG = "Contacts Manager";
    
    public static final String EXCHANGE_ACCOUNT_AUTHORITY = "com.motorola.motosync.provider.AccountContentProvider";
    public static final Uri EXCHANGE_ACCOUNT_CONTENT_URI = Uri.parse("content://" 
        + EXCHANGE_ACCOUNT_AUTHORITY + "/Account");

    private static final String vcardStartTag = "BEGIN:VCARD";
    private static final String vcardEndTag = "END:VCARD";
    private static final String tagVcardStart = "BEGIN:VCARD\n";    
    private static final String RAW_CONTACT_ACCOUNT = "local-contacts";
    
	private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private boolean mIsAltSdcard;
    private Context mContext = null;
    private File mContactsDir = null;
    private NotificationAgent mNotificationAgent = null;
    private String mVcardString = "";
    private int mActualVcardCounter = 0;
    private int mActualFileCounter = 0;
    private int mTotalCounter  = 0;
    private int mTotalFileCounter = 0;
    private int mCurrentFileCounter = 0;
    private String mDateString = null;

	private int[] mActualVcardCounterArray = null;
    private int[] mTotalCounterArray = null;
    
    private int BATCH_COMMIT_INTERVAL = 50;
    private static int BATCH_OPERATION_MAX = 300;
    private Constants mConstants = null;
    private String mPhotoFilePath = null;
    
    private ArrayList<Integer> mContactIdList = null;
    private ArrayList<Uri> mUriList = null;
    private HashMap<Long, String> mDisplayName = new HashMap<Long, String>();  // <contact_id, display_name>
    private boolean mIsExportSpeContacts = false;
    
    
    public ContactsManager(Context context, NotificationAgent notification) {
        mContext = context;
        mNotificationAgent = notification;
        mConstants = new Constants(context);
    }

	public boolean getIsWorking() {
		return mIsWorking;
	}

    public String getDateString() {
		return mDateString;
	}
	
    public int getTotalCounter() {
        return mTotalCounter;
    }
    
    public int getTotalFileCounter() {
        return mTotalFileCounter;
    }

    public int getActualVcardCounter() {
        return mActualVcardCounter;
    }

    public int getActualFileCounter() {
        return mActualFileCounter;
    }
    
        
    public int[] getActualVcardCounterArray() {
		return mActualVcardCounterArray;
	}

	public int[] getTotalCounterArray() {
		return mTotalCounterArray;
	}

	public File getContactsDir(){
    	return mContactsDir;
    }

    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }

    public int BackupContacts() {
        return BackupContacts(null);
    }
    
    public int BackupContacts(ArrayList<Uri> uriList) {
    	Log.v(TAG, "Enter BackupContacts()");
    	if (uriList == null) {
			mIsExportSpeContacts = false;
		} else {
			mIsExportSpeContacts = true;
		}
        int result = R.id.success;
        mUriList = uriList;
        long pre_time = System.currentTimeMillis();
        if (!mInterrupted) {
			mIsWorking = true;
            result = Backup();
			mIsWorking = false;
        }
        Log.d(TAG + " Operation Time", "Backup Operation time: " + (System.currentTimeMillis() - pre_time)/1000 + "s");
        return result;
    }
    
    public int BackupLocalContacts(){
    	Log.v(TAG, "Enter BackupLocalContacts()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, new String[]{Constants.LOCAL_ACCOUNT_NAME}, Constants.LOCAL_CONTACT)[0];
			mIsWorking = false;
		}
        return result;
    }
    
    public int BackupCardContacts(){
    	Log.v(TAG, "Enter BackupCCardContacts()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, new String[]{Constants.CARD_ACCOUNT_NAME}, Constants.CARD_CONTACT)[0];
			mIsWorking = false;
		}
        return result;
    }
    
    public int BackupCCardContacts(){
    	Log.v(TAG, "Enter BackupCCardContacts()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, new String[]{Constants.CCARD_ACCOUNT_NAME}, Constants.CCARD_CONTACT)[0];
			mIsWorking = false;
		}
        return result;
    }
    
    public int BackupGCardContacts(){
    	Log.v(TAG, "Enter BackupGCardContacts()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, new String[]{Constants.GCARD_ACCOUNT_NAME}, Constants.GCARD_CONTACT)[0];
			mIsWorking = false;
		}
        return result;
    }
    
    public int[] BackupGAccountContacts(String[] accNames){
    	Log.v(TAG, "Enter BackupGCardContacts()");
        int[] result = new int[accNames.length];
        for (int i : result) {
			i = R.id.success;
		}
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, accNames, Constants.GACCOUNT_CONTACT);
			mIsWorking = false;
		}
        return result;
    }
    
    public int[] BackupEAccountContacts(String[] accNames){
    	Log.v(TAG, "Enter BackupGCardContacts()");
    	int[] result = new int[accNames.length];
        for (int i : result) {
			i = R.id.success;
		}
        if (!mInterrupted) {
			mIsWorking = true;
			result = backupContactsByAccount(mContactsDir, accNames, Constants.EACCOUNT_CONTACT);
			mIsWorking = false;
		}
        return result;
    }
    
    

    /* 
     * if action is restore:
     *     the element of pathlist is just combined by folder name and file name, and pathlist size is 1
     * if action is import:
     *     the element of pathlist is combined by sdcard prefix, folder name and file name
     */
    public int RestoreContacts(int action, ArrayList<File> fileList, String accName, String accType) {
        Log.v(TAG, "Enter RestoreContacts()");
        int result = R.id.success;
        long pre_time = System.currentTimeMillis();
        if (!mInterrupted) {
			mIsWorking = true;
       		result = Restore(action, fileList, accName, accType);
       		mVcardString = null;
			mIsWorking = false;
            if( Constants.IMPORT3RD_ACTION != action ) {  // for default restore action
//                mNotificationAgent.sendMsgWithNotification(action,
//                        Message.obtain(null, R.id.status, result, Constants.CONTACTS_ACTION));
            } else {  // imprt3rd file or folder action
                // to request Activity to remove progress bar
//                mNotificationAgent.sendMsgWithoutNotification(action,
//                        Message.obtain(null, R.id.status, R.id.remove_dialog, Constants.CONTACTS_ACTION, null));
            }
        } else {
            result = R.id.cancel;
        }
        Log.d(TAG + " Operation Time", "All apply batch time: " + VCardManager.mAllApplyBatchTime + "s");
        Log.d(TAG + " Operation Time", "Restore Operation time: " + (System.currentTimeMillis() - pre_time)/1000 + "s");
        return result;
    }
    
    private int Backup() {
        Log.v(TAG, "Enter Backup()");
        int result = R.id.success;
        if (BackupRestoreService.mBackupDest == -1) {
            BackupRestoreService.mBackupDest = new Utility(mContext).getBackupDest();
        }
        if (BackupRestoreService.mBackupDest == Constants.OUTER_STORAGE) {
            mIsAltSdcard = true;
        } else if (BackupRestoreService.mBackupDest == Constants.INNER_STORAGE) {
            mIsAltSdcard = false;
        } else {
            return R.id.no_sdcard;
        }
        // to delete previous backuped Contacts file
        if (!mIsExportSpeContacts && !deleteOldBackupFile()) return R.id.unknown_error;
        result = SdcardManager.checkSdcardInitCondition(mContext, mIsAltSdcard, mConstants.DEFAULT_FOLDER);
        if (result != R.id.success) {
			return result;
		}
        File handler;
        if (null == (handler=SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER, mIsAltSdcard))) {
            Log.e(TAG, "Create backup folder unknown error.");
            result = R.id.unknown_error;
            return result;
        }            
        mPhotoFilePath = handler.getAbsolutePath()+ "/" + Constants.CONTACTS_PHOTO_PATH; 
        if (null == SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER + "/" + Constants.CONTACTS_PHOTO_PATH, mIsAltSdcard)) {
            Log.e(TAG, "Create contact photo folder unknown error.");
            result = R.id.unknown_error;
            return result;
        }
        result = backupContacts(handler);
        Log.v(TAG, "Contacts backup complete.");
        return result;
    }
    
    
    public int getReadyToBackup(){
    	Log.v(TAG, "Enter BackupByAccount()");
        int result = R.id.success;
        BackupRestoreService.mBackupDest = new Utility(mContext).getBackupDest();
        if (BackupRestoreService.mBackupDest == Constants.OUTER_STORAGE) {
        	mIsAltSdcard = true;
		} else if (BackupRestoreService.mBackupDest == Constants.INNER_STORAGE) {
			mIsAltSdcard = false;
		} else {
			return R.id.no_sdcard;
		}      	
        mDateString = getCurrentDate();
        if (null == SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER, mIsAltSdcard)) {
            Log.e(TAG, "Get SD Card unknown error.");
            result = R.id.unknown_error;
            return result;
        }
        if (null == (mContactsDir=SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER + "/" + mDateString, mIsAltSdcard))) {
            Log.e(TAG, "Get SD Card unknown error.");
            result = R.id.unknown_error;
            return result;
        }
        if (null == SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER + "/" + mDateString + "/" + Constants.CONTACTS_PHOTO_PATH, mIsAltSdcard)) {
            Log.e(TAG, "Get SD Card unknown error.");
            result = R.id.unknown_error;
            return result;
        }
        Log.v(TAG, "Get ready to backup.");
        
        return result;
    }
    
    private int Restore(int action, ArrayList<File> fileList, String accName, String accType) {
        Log.v(TAG, "Enter Restore()");
        int result = R.id.success;
        File contactsFile = null;
        String filePath = null;
        File[] files = null;
        if (fileList == null || fileList.size() == 0) {
			return R.id.empty_storage;
		}
        if (SdcardManager.noSdcard(mContext)) {
        	return R.id.no_sdcard;
        }
        
        if (action == Constants.RESTORE_ACTION) {
        	  // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 begin
        	if (fileList != null) {
    			if (isInnerVcardFileExist() && !isOuterVcardFileExist()) {
    				filePath = mConstants.SDCARD_PREFIX + "/" + fileList.get(0).getPath(); //see the comments of RestoreContacts()
    			} else if ((isOuterVcardFileExist() && !isInnerVcardFileExist()) || (isInnerVcardFileExist() && isOuterVcardFileExist())) {
    				filePath = mConstants.ALT_SDCARD_PREFIX + "/" + fileList.get(0).getPath();
    			} else {
    				return R.id.empty_storage;
    			}
			} else {
				return R.id.unknown_error;
			}
            contactsFile = new File(filePath);
            if( !contactsFile.exists() )  return R.id.empty_storage;
            // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 end
		}
        
        if (fileList == null || fileList.size() == 0) {
        	result = R.id.empty_storage;
         } else {
            if(action  == Constants.RESTORE_ACTION) {  // for specific Contact file only
                files = new File[] {contactsFile};
                mPhotoFilePath = contactsFile.getParent() + "/" + Constants.CONTACTS_PHOTO_PATH;                
                result = importOneContactFile(action, files, accName, accType);
                Log.d(TAG, "Result after importOneContactFile() implemented:" + result);
            } else if (action == Constants.IMPORT3RD_ACTION) {
            	files = new File[fileList.size()];
            	files = fileList.toArray(files);
                result = importContactFiles(action, files, accName, accType);  // for specific Contact Folder only
                Log.d(TAG, "Result after importOneContactFolder() implemented:" + result);
            }
        }
    
        return result;
    }

    private int[] backupContactsByAccount(File dirHandle,String[] accountNames, int account_type){
    	int size = accountNames.length;
    	int[] result = new int[size];
    	mActualVcardCounterArray = new int[size];
    	mTotalCounterArray = new int[size];
        for(int m=0;m<accountNames.length;m++){
        	String fileName = getFileName(account_type, accountNames[m]);
            result[m] = R.id.success;
            int readCount = 1;

            // to delete previous backuped Contacts file
            String selection = RawContacts.ACCOUNT_TYPE + "='" + getAccountType(account_type)
            				+ "' AND " + RawContacts.ACCOUNT_NAME + "='" + getAccountName(accountNames[m])
            				+ "' AND " + RawContacts.DELETED + "=0";         
            Cursor cursor = mContext.getContentResolver().query(RawContacts.CONTENT_URI, null, selection, null, null);
            
            if (null == cursor) {
                Log.i(TAG, "getAllContacts - empty CDB");
                if (null != cursor) cursor.close();
                result[m] = R.id.read_phone_db_error;
                continue;
            }
            mDisplayName = getDisplayName(cursor);
            
            mTotalCounterArray[m] = mDisplayName.size();
            Log.d(TAG, "Total number of Contacts will be backup to Vcard file:" + mTotalCounterArray[m]);
            
            mNotificationAgent.sendMsgWithoutNotification(Constants.EXPORTBYACCOUNT_ACTION,
                    Message.obtain(null, R.id.message_length, mTotalCounterArray[m], Constants.CONTACTS_ACTION));

            VCardManager vm = VCardManager.getInstance(mContext, true, true, false, 
            		getAccountName(accountNames[m]), getAccountType(account_type));
            
            mPhotoFilePath = dirHandle.getAbsolutePath() + "/" + Constants.CONTACTS_PHOTO_PATH;            
            vm.setPhotoPath(mPhotoFilePath);
            Iterator iter = mDisplayName.entrySet().iterator();
            Map.Entry entry;
            long contact_id;
            for (int i = 0; !mInterrupted && (i < mTotalCounterArray[m]) && iter.hasNext(); cursor.moveToNext(), i++) {
                entry = (Map.Entry)iter.next();
                contact_id = Long.valueOf(String.valueOf(entry.getKey()));
                String vc = vm.getVCard(contact_id);
                String personName = mDisplayName.get(contact_id);
                if (personName == null) {
                    personName = mContext.getString(R.string.contacts);
                }
				Log.d(TAG, "before substring personName:" + personName);
				if ( Constants.COMMON_DISPLAY_LEN < personName.length() ) {
					personName = personName.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
				}
				Log.d(TAG, "after substring personName:" + personName);

                Log.d(TAG, "Now it's backuping vcard for " + personName);
                result[m] = SdcardManager.saveInSdcard(mContext, dirHandle, fileName, vc, mIsAltSdcard);
                if (result[m] == R.id.success) {
                    mNotificationAgent.sendMsgWithoutNotification(Constants.EXPORTBYACCOUNT_ACTION,
                        Message.obtain(null, R.id.under_progress, readCount, Constants.CONTACTS_ACTION,
                            mContext.getString(R.string.exporting, mContext.getString(R.string.contacts))+"\n\n"+personName));

                    // A16516 add IKQLTTCW-1384
                    NotificationItem item = new NotificationItem();
                    item.id = 1;
                    item.totalCurrent = readCount;
                    item.totalTotal = mTotalCounterArray[m];
                    item.description = mContext.getString(R.string.exporting, mContext.getString(R.string.contacts));
                    item.direction = Constants.EXPORTBYACCOUNT_ACTION;
                    Log.v(TAG, "Indication progress bar backup contacts, current is "
                            + item.totalCurrent + " total is " + item.totalTotal);
                    Log.v(TAG, "Description is " + item.description);
                    mNotificationAgent.sendNotificationProgress(item);
                    // A16516 end IKQLTTCW-1384

                    Log.v(TAG, "Contacts backup count2:"+readCount);
                    readCount++;
                } else {
                    Log.v(TAG, "write SD card return error while backup Contacts");
                    break;
                }
            }
            mActualVcardCounterArray[m] = readCount-1;
            cursor.close();
            
            if (mInterrupted) {
                result[m] = R.id.cancel;
            }
        }
        File contact_photo_folder = new File(mPhotoFilePath);
        if (contact_photo_folder.exists() && contact_photo_folder.isDirectory()) {
            File[] files = contact_photo_folder.listFiles();
            if (files == null || files.length == 0) {
                contact_photo_folder.delete();
            }
        }
        return result;
    }
    
    private String getFileName(int account_type,String accountName){
    	String fileName = null;
    	String tmpString = null;
    	switch(account_type){
    	case Constants.LOCAL_CONTACT:
    		fileName = Constants.LOCAL_CONTACTS_FILE_NAME;
    		break;
    	case Constants.CCARD_CONTACT:
    		fileName = Constants.CCARD_CONTACTS_FILE_NAME;
    		break;
    	case Constants.GCARD_CONTACT:
    		fileName = Constants.GCARD_CONTACTS_FILE_NAME;
    		break;
    	case Constants.GACCOUNT_CONTACT:
    		tmpString = accountName.replace(':', '_');
    		fileName =  tmpString.substring(0, tmpString.indexOf("@")) + Constants.vcardExtern;
    		break;
    	case Constants.EACCOUNT_CONTACT:
    		tmpString = accountName.replace(':', '_');
    		fileName =  tmpString.substring(0, tmpString.indexOf("@")) + Constants.vcardExtern;
    		break;
    	case Constants.CARD_CONTACT:
            if (Constants.PHONE_TYPE == TelephonyManager.PHONE_TYPE_CDMA) {
            	fileName = Constants.CCARD_CONTACTS_FILE_NAME; 
            } else {
            	fileName = Constants.GCARD_CONTACTS_FILE_NAME;
            }
    		break;
    	}
    	return fileName;
    }
    
    private String getAccountType(int account_type){
    	String accountType = null;
    	switch(account_type){
    	case Constants.LOCAL_CONTACT:
    		accountType = Constants.LOCAL_ACCOUNT_TYPE;
    		break;
    	case Constants.CCARD_CONTACT:
    		accountType = Constants.CCARD_TYPE;
    		break;
    	case Constants.GCARD_CONTACT:
    		accountType = Constants.GCARD_TYPE;
    		break;
    	case Constants.GACCOUNT_CONTACT:    		
    		accountType = Constants.GOOGLE_ACCOUNT_TYPE;
    		break;
    	case Constants.EACCOUNT_CONTACT:
    		accountType = Constants.EXCHANGE_ACCOUNT_TYPE;
    		break;
    	case Constants.CARD_CONTACT:
            if (Constants.PHONE_TYPE == TelephonyManager.PHONE_TYPE_CDMA) {
            	accountType = Constants.CCARD_TYPE; 
            } else {
            	accountType = Constants.GCARD_TYPE;
            }
            break;
    	}
    	return accountType;
    }
    
    private String getAccountName(String accountName){
    	return accountName.substring(accountName.indexOf(":") + 1);
    }
    
    private HashMap<Long, String> getDisplayName(Cursor cursor) {
    	if ((null == cursor)) {
            Log.i(TAG, "getAllContacts - empty CDB");
            return null;
        }
    	HashMap<Long, String> display_name = new HashMap<Long, String>();
    	int col;
    	long contact_id;
    	String displayName;
    	while (cursor.moveToNext()) {
    		col = cursor.getColumnIndex(RawContacts.CONTACT_ID);
    		contact_id = cursor.getLong(col);
    		col = cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY);
    		displayName = cursor.getString(col);
    		display_name.put(contact_id, displayName == null ? "" : displayName);
		}
    	return display_name;
    }
    
    private HashMap<Long, String> getDisplayName(ArrayList<Uri> uri_list) {
    	Log.d(TAG, "Enter getUris()");
    	if (uri_list == null) {
			return null;
		}
    	HashMap<Long, String> display_name = new HashMap<Long, String>();
    	Cursor cursor = null;
    	String contact_id;
    	int col;
    	for (int i = 0; i < uri_list.size(); i++) {
    		contact_id = uri_list.get(i).getLastPathSegment();
    		cursor = mContext.getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact_id), new String[]{RawContacts.DISPLAY_NAME_PRIMARY}, null, null, null);
    		if (!cursor.moveToNext()) {
    		    continue;
    		}
    		col = cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY);
    		if (col != -1) {
				String displayName = cursor.getString(col);
	    		display_name.put(Long.parseLong(contact_id), displayName);
			}
    		cursor.close();
		}
    	return display_name;
    }
    
    
    private int backupContacts(File dirHandle) {
        Log.v(TAG, "Enter backupContacts()");
        String fileName = null;
        if (mIsExportSpeContacts) {
			fileName = getCurrentDate() + "_" + mUriList.size() + Constants.vcardExtern;
		} else {
			fileName = Constants.CONTACTS_FILE_NAME;
		}
        int result = R.id.success;
        int readCount = 1;

        if (mUriList == null) {
            String selection = RawContacts.ACCOUNT_NAME + "='" + RAW_CONTACT_ACCOUNT +
                                     "' AND " + RawContacts.DELETED + "=0";         
            Cursor cursor = mContext.getContentResolver().query(RawContacts.CONTENT_URI, null, selection, null, RawContacts.CONTACT_ID);
            if (null == cursor) {
                Log.i(TAG, "getAllContacts - empty CDB");
                return R.id.read_phone_db_error;
            }
            mDisplayName = getDisplayName(cursor);
            cursor.close();
		} else {
			mDisplayName = getDisplayName(mUriList);
		}
        
        mTotalCounter = mDisplayName.size();
        Log.d(TAG, "Total number of Contacts will be backup to Vcard file:" + mTotalCounter);
        
        mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.message_length, mTotalCounter, Constants.CONTACTS_ACTION));

	    VCardManager vm = VCardManager.getInstance(mContext, true, true, false, 
			RAW_CONTACT_ACCOUNT, Constants.LOCAL_ACCOUNT_TYPE);
        
	    mPhotoFilePath = dirHandle.getAbsolutePath() + "/" + Constants.CONTACTS_PHOTO_PATH; 
	    vm.setPhotoPath(mPhotoFilePath);
	    Iterator iter = mDisplayName.entrySet().iterator();
	    Map.Entry entry;
	    long contact_id;
        for (int i = 0; !mInterrupted && (i < mTotalCounter) && iter.hasNext(); i++) {
        	entry = (Map.Entry)iter.next();
        	contact_id = Long.valueOf(String.valueOf(entry.getKey()));
            String vc = vm.getVCard(contact_id);
            
            if(null == vc){
                Log.e(TAG, "vc is null ,continue next");
                continue;
            }
            String personName = mDisplayName.get(contact_id);
            if (personName == null) {
				personName = mContext.getString(R.string.contacts);
			}
            Log.d(TAG, "before substring personName:" + personName);
			if ( Constants.COMMON_DISPLAY_LEN < personName.length() ) {
				personName = personName.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
			}
            Log.d(TAG, "after substring personName:" + personName);

            
            Log.d(TAG, "Now it's backuping vcard for " + personName);
            
            result = SdcardManager.saveInSdcard(mContext, dirHandle, fileName, vc, mIsAltSdcard);
            if (result == R.id.success) {
                mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                    Message.obtain(null, R.id.under_progress, readCount, Constants.CONTACTS_ACTION,
                        mContext.getString(R.string.backuping, mContext.getString(R.string.contacts))+"\n\n"+personName));

                // A16516 add IKQLTTCW-1384
                NotificationItem item = new NotificationItem();
                item.id = 1;
                item.totalCurrent = readCount;
                item.totalTotal = mTotalCounter;
                item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.contacts));
                item.direction = Constants.BACKUP_ACTION;
                Log.v(TAG, "Indication progress bar backup contacts, current is "
                        + item.totalCurrent + " total is " + item.totalTotal);
                Log.v(TAG, "Description is " + item.description);
                mNotificationAgent.sendNotificationProgress(item);
                // A16516 end IKQLTTCW-1384

                Log.v(TAG, "Contacts backup count:"+readCount);
                readCount++;
            } else {
                Log.v(TAG, "write SD card return error while backup Contacts");
                break;
            }
        }
        mActualVcardCounter = readCount-1;
        
        if (mInterrupted) {
            result = R.id.cancel;
        }
        File contact_photo_folder = new File(mPhotoFilePath);
        if (contact_photo_folder.exists() && contact_photo_folder.isDirectory()) {
            File[] files = contact_photo_folder.listFiles();
            if (files == null || files.length == 0) {
                contact_photo_folder.delete();
            }
        }
        Log.d(TAG, "return of backupContacts() method");
        return result;
    }

    private int importOneContactFile(int action, File[] fileList, String accName, String accType) {
        Log.v(TAG, "Enter importOneContactFile()");
        String fileName = null;
        int contactsLen = 0;  
        int pos = 0;  
        int fileLen = 1;
        int result = R.id.success;
        
        fileName = fileList[0].getName();
        if( R.id.success != (result=readVcardFileContents(fileList[0].getPath())) ){
            Log.e(TAG, "Read Vcard file error with ID:"+result);
            return result;
        } else if ( ((!mVcardString.startsWith(vcardStartTag))&&(!mVcardString.startsWith(tagVcardStart))) ){
            Log.e(TAG, "Wrong format of Vcard file reading");
            result = R.id.sdcard_file_contents_error;
            return result;
        }

        try{
            // To count the number of "END:VCARD\n"
            pos = mVcardString.indexOf(vcardStartTag);
            while( (-1 != (pos=mVcardString.indexOf(vcardEndTag, pos))) && !mInterrupted ) {
                pos += vcardEndTag.length();
                contactsLen++;
            }
        } catch( NullPointerException e){
            Log.v(TAG, "Wrong format of Vcard file");
            result = R.id.sdcard_file_contents_error;
            return result;
        }
        
        if( mInterrupted ){
            Log.d(TAG, "importOneContactFile() detected mInterrupted");
            return R.id.cancel;
        }

        mTotalCounter = contactsLen; 
        Log.d(TAG, "number of vCards: " + contactsLen);
        if( 0 == contactsLen ){ // No Vcard end tag, it's a wrong Vcard file
            result = R.id.sdcard_file_contents_error;
            return result;
        } else {
            // To send total Vcard length to Activity progress bar 
            mNotificationAgent.sendMsgWithoutNotification(action,
                    Message.obtain(null, R.id.message_length, contactsLen, Constants.CONTACTS_ACTION));
            // A16516 add IKQLTTCW-1384
            if (action == Constants.RESTORE_ACTION) {
                result = insertVcardFileIntoPhone(action, null, accName, accType);
            } else {
                result = insertVcardFileIntoPhone(action, fileName, accName, accType);
            }            
            // A16516 end IKQLTTCW-1384
        }

        return result;
    }

    private int importContactFiles(int action, File[] fileList, String accName, String accType) {
        Log.v(TAG, "Enter importContactFiles()");
        String filePath = null;
        String fileName = null;
        int result = R.id.empty_storage;
        int cfLen = 0;
        int finalResult = 0;
        int pos;
        boolean everSuccess = false;
        
        // to count number of valid VCF files
        cfLen = fileList.length;
        mTotalFileCounter = cfLen;
        mCurrentFileCounter = 0;
        if (0 == cfLen)  return R.id.empty_storage;
        
        for (int i=0; i<cfLen && !mInterrupted; i++) {
            mTotalCounter = 0;
            mCurrentFileCounter++;
            filePath = fileList[i].getPath();
            fileName = fileList[i].getName();
            Log.v(TAG, "vcard " + i + " " + fileName);
            mPhotoFilePath = fileList[i].getParent() + "/" + Constants.CONTACTS_PHOTO_PATH;  
    
            try {
                if( R.id.success != (result=readVcardFileContents(filePath)) ){
                    Log.e(TAG, "Read Vcard file error with ID:"+result);
                    return result;
                } else if ((!mVcardString.startsWith(vcardStartTag))&&(!mVcardString.startsWith(tagVcardStart)) ){
                    Log.e(TAG, "This is not a Vcard format file");
                    result = R.id.sdcard_file_contents_error;
                    continue;
                } else {
                 // To count the number of "END:VCARD\n"
                    pos = mVcardString.indexOf(vcardStartTag);
                    while( (-1 != (pos=mVcardString.indexOf(vcardEndTag, pos))) && !mInterrupted ) {
                        pos += vcardEndTag.length();
                        mTotalCounter++;
                    }
                }
            } catch(NullPointerException e){
                Log.e(TAG, "Wrong format of VCF file");
                result = R.id.sdcard_file_contents_error;
                mNotificationAgent.sendMsgWithoutNotification(action,
                        Message.obtain(null, R.id.under_progress, i, Constants.CONTACTS_ACTION, null));
                continue;
            }

            // To insert the Contacts into phone
            result = insertVcardFileIntoPhone(action, fileName, accName, accType);
            
            if( R.id.success == result )  {
                everSuccess = true;
                mActualFileCounter++;
            } else if( R.id.cancel == result ) {
                return R.id.cancel;
            } else {
                finalResult = result; 
            }
        }
        
        if( mInterrupted ){
            Log.d(TAG, "importOneContactFolder() detected mInterrupted");
            return R.id.cancel;
        } else {
            // to show 100% in progress bar
            mNotificationAgent.sendMsgWithoutNotification(action,
                    Message.obtain(null, R.id.under_progress, cfLen, Constants.CONTACTS_ACTION, ""));
        }
        
        Log.d(TAG, "importXXXFromFiles mActualFileCounter="+mActualFileCounter);
    	if(mActualFileCounter == 0){
    		return result;
    	}
    	
        if( R.id.write_phone_db_error == finalResult )  result = R.id.write_phone_db_error;
        if( true == everSuccess )  result = R.id.success;
        
        return result;
    }
    
    private int insertVcardFileIntoPhone(int action, String fileName, String accName, String accType) {
        int pos1 = 0;
        int pos2 = 0;
        int result = R.id.success;
        boolean containValidVcard = false;
        boolean isFinished = false;
        String sBuffer = "";
        int addCount = 0;    
        int opNum = 0 ;
        int count = 0;
        VCardManager vm = VCardManager.getInstance(mContext, true, true, false, accName, accType);
        vm.setPhotoPath(mPhotoFilePath);
        // To send total number of each file to Activity progress bar 
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalCounter, Constants.CONTACTS_ACTION));
        try {
            if ( -1==(pos1=mVcardString.indexOf(vcardStartTag)) )  return R.id.sdcard_file_contents_error;
            ArrayList<Uri> commitUri = null;
            while ( true ){
                if (-1 == (pos2=mVcardString.indexOf(vcardEndTag, pos1))) {
                    if (addCount > 0) {
                    	//  Log.v(TAG, "Reach the end of importing and commit");
                        commitUri = vm.commitAddedContacts(mContext.getContentResolver());
                        if (commitUri == null) {
                            Log.e(TAG, "Commit error at the end");
                            result = R.id.write_phone_db_error;
                            isFinished = false;
                            break;
                        }
                    } 
                    isFinished = true;
                    break;
                }
                
                if (null==(sBuffer=mVcardString.substring(pos1, pos2+vcardEndTag.length()))) {
                    result = R.id.sdcard_file_contents_error;
                    isFinished = false;
                    break;
                }
                Log.d(TAG, "Vcard String: " + sBuffer);

                if (mInterrupted)  {
                    if (addCount > 0) vm.commitAddedContacts(mContext.getContentResolver());
                    break;
                }

                try {
                	opNum = vm.addNoCommit(sBuffer, accName, accType);
				} catch (VCardInvalidLineException e) {
					e.printStackTrace();
					pos1 = pos2+vcardEndTag.length()+1;
					continue;
				}
                if (opNum < 0) {
                    if (addCount > 0) vm.commitAddedContacts(mContext.getContentResolver());
                    result = R.id.write_phone_db_error;
                    isFinished = false;
                    break;
                } else {
                    addCount++;
                    String personName = vm.constructDisplayName(
                            vm.getFamilyName(), vm.getMiddleName(), vm.getGivenName());

                    //  Log.d(TAG, "before substring personName:" + personName);
					if ( Constants.COMMON_DISPLAY_LEN < personName.length() ) {
						personName = personName.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
					}
	                //Log.d(TAG, "after substring personName:" + personName);

	               // Log.d(TAG, "Vcard String: " + sBuffer);

                    mActualVcardCounter ++;
                    // to show current count and person name in progress bar
                    String mesgBody;
                    if (action == Constants.RESTORE_ACTION) {
                        mesgBody = mContext.getString(R.string.restoring, mContext.getString(R.string.contacts)) 
                                + "\n\n" + personName;
                    } else {
                        mesgBody = mContext.getString(R.string.import3rding0, mContext.getString(R.string.contacts), mCurrentFileCounter, mTotalFileCounter, fileName) 
                                + "\n\n" + personName;
                    }
                    mNotificationAgent.sendMsgWithoutNotification(action,
                        Message.obtain(null, R.id.under_progress, count, Constants.CONTACTS_ACTION, mesgBody));
                    containValidVcard = true;

                    // A16516 add IKQLTTCW-1384
                    NotificationItem item = new NotificationItem();
                    item.id = 1;
                    item.totalCurrent = count;
                    item.totalTotal = mTotalCounter;
                    item.direction = action;
                    Log.e(TAG, "Restore contacts, current is " + item.totalCurrent
                            + " total is " + item.totalTotal + " action is " + item.direction);
                    if (item.direction == Constants.RESTORE_ACTION) {
                        item.description = mContext.getString(R.string.restoring, 
                                mContext.getString(R.string.contacts));
                    } else {
                        if (fileName != null) {
                            item.description = mContext.getString(R.string.import3rding, 
                                    mContext.getString(R.string.contacts), fileName);
                        } else {
                            item.description = mContext.getString(R.string.import3rding, 
                                    mContext.getString(R.string.contacts), "");
                            Log.e(TAG, "Miss file name for import3rdParty action type");
                        }
                    }
                    Log.e(TAG, "Description is " + item.description);
                    mNotificationAgent.sendNotificationProgress(item);
                    // A16516 end IKQLTTCW-1384
                }
                count++;
                pos1 = pos2+vcardEndTag.length()+1;
                if (addCount >= BATCH_COMMIT_INTERVAL || opNum >= BATCH_OPERATION_MAX ) {
                //    Log.d(TAG, "Call commitAddedContacts");
                    commitUri = vm.commitAddedContacts(mContext.getContentResolver());
                    addCount = 0;
                    opNum = 0;
                    if (commitUri == null) {
                        Log.e(TAG, "Commit error at the middle");
                        result = R.id.write_phone_db_error;
                        isFinished = false;
                        break;
                    } 
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vcard file format exception: ", e);
            result = R.id.sdcard_file_contents_error;
            isFinished = false;
        }

        if (true==isFinished) {
            Log.d(TAG, "isFinished == true");
            if (true!=containValidVcard) {
                result = R.id.unknown_error;
            } else {
                result = R.id.success;
            }
        } else {
            if (mInterrupted) {
                Log.d(TAG, "mInterrupted detected");
                return R.id.cancel;
            } else {
                Log.d(TAG, "importing error, breaking ...");
            }
        }
        
        return result;
    }

    private int readVcardFileContents(String fileName) {
        int result = R.id.success;
        byte[] b = null;
        FileInputStream in = null;
        
        try {
            in = new FileInputStream(fileName);
            int needReadLength = in.available();
            b = new byte[needReadLength];
            if (needReadLength > 0) {
				in.read(b);
			}
            in.close();
            
            int encodingResult = VCardManager.guessEncoding(b);
            if ((encodingResult & VCardManager.ENCODING_MAY_GBK) != 0) {
                 Log.d(TAG, "GBK");
                 mVcardString = new String(b , "GBK");
                 // to delete ";CHARSET=GB2312" in order the lib won't misunderstand the encoding style
                 String expr = ";[Cc][Hh][Aa][Rr][Ss][Ee][Tt]=[Gg][Bb]2312";
                 mVcardString = mVcardString.replaceAll(expr, "");
            } else if ((encodingResult & VCardManager.ENCODING_MAY_ASCII) != 0){
                 Log.d(TAG, "ISO-8859-1");
                 mVcardString = new String(b, "ISO-8859-1");
            } else {
                 Log.d(TAG, "UTF8");
                 mVcardString = new String(b, "UTF8");
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode: charset");
            mVcardString = new String(b);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:", e);
            result = R.id.read_sdcard_error;
        } catch (IOException e) {
            try {
                if (null != in) in.close();
            } catch (IOException e1) {
                // do nothing
            }
            Log.e(TAG, "Read VCF file error", e);
            result = R.id.read_sdcard_error;
        } catch (Exception e) {
            Log.e(TAG, "Got exception:", e);
            result = R.id.unknown_error;
        } catch (OutOfMemoryError e) {
            mVcardString = null;
            System.gc();
            Log.e(TAG, "Read Vcard File OutOfMemoryException");
            result = R.id.out_of_memory_error;
        }

        return result;
    }
    
    public String stringReplaceAll(String src, String search, String replace) {
        int len1 = search.length();
        int len2 = replace.length();
        int pos = 0;
        
        StringBuffer strbuf = new StringBuffer(src);
        
        while( -1 != (pos = strbuf.indexOf(search, pos)) ) {
            strbuf.replace(pos, pos+len1, replace);
            Log.d(TAG, "Replaced on position:" + pos);
            pos += len2;
        }
        
        return strbuf.toString();
    }
    
    private Cursor exportContacts() {
        Log.v(TAG, "Enter exportContacts()");
        
        String selection = RawContacts.ACCOUNT_NAME + "='" + RAW_CONTACT_ACCOUNT +
        "' AND " + RawContacts.DELETED + "=0";

        return mContext.getContentResolver().query(RawContacts.CONTENT_URI, null, 
                selection, null, null);
    }
    
    public boolean isContactsDBEmpty() {
        Cursor ContactsCursor = exportContacts();
        if (ContactsCursor != null) {
            if (ContactsCursor.getCount() != 0) {
                ContactsCursor.close();
                return false;
            } else {
                ContactsCursor.close();
                return true;
            }
        } else {
            return true;
        }
    }
    
    public boolean isInnerVcardFileExist() {
    	File vInnerCardFile = new File( mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.CONTACTS_FILE_NAME );
    	if (vInnerCardFile.exists()) {
			return true;
		} else {
			return false;
		}
    }
    
    public boolean isOuterVcardFileExist() {
    	File vOuterCardFile = new File( mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                + "/" + Constants.CONTACTS_FILE_NAME );
    	if (vOuterCardFile.exists()) {
			return true;
		} else {
			return false;
		}
    }    
    
    public boolean isVcardFileNotExist() {
        if (isInnerVcardFileExist() || isOuterVcardFileExist()) return false;
        else return true;
    }
    
    public String getCurrentDate() {
		  Date currentTime = new Date();
		  SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd-HH-mm-ss");
		  String dateString = formatter.format(currentTime);
		  return dateString;
		 }
    
    public boolean deleteOldBackupFile() {
    	boolean inner_res = true;
    	boolean outer_res = true;
    	File inner_file = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
    			                  , Constants.CONTACTS_FILE_NAME);
    	 // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 begin
    	if (inner_file != null && inner_file.exists() && !mIsAltSdcard) {
        	inner_res = inner_file.delete();
		}
    	File outer_file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                                  , Constants.CONTACTS_FILE_NAME);
    	
    	if (outer_file != null && outer_file.exists() && mIsAltSdcard) {
        	outer_res = outer_file.delete();
		}
    	 // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 end
    	if (!inner_res || !outer_res) {
			return false;
		}
    	File contact_photo_folder = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.CONTACTS_PHOTO_PATH);
    	if (contact_photo_folder.exists() && contact_photo_folder.isDirectory()) {
    	    if (!Utility.deleteDirFile(contact_photo_folder, true, null)) {
                return false;
            }
        }
		return true;
    }
    
    public static ArrayList<Long> queryForAllRawContactIds(ContentResolver cr, long contactId, boolean forExportOnly, HashMap<Long, String> uri_displayname) {
		Cursor rawContactIdCursor = null;
		ArrayList<Long> rawContactIds = new ArrayList<Long>();
		Uri uri = RawContacts.CONTENT_URI;
		int col;
		if (forExportOnly) {
			uri = uri.buildUpon().appendQueryParameter("for_export_only", "1")
					.build();
		}
		try {
			rawContactIdCursor = cr.query(uri,
					new String[] { RawContacts._ID, RawContacts.DISPLAY_NAME_PRIMARY }, RawContacts.CONTACT_ID
							+ "=" + contactId, null, null);
			if (rawContactIdCursor != null) {
				while (rawContactIdCursor.moveToNext()) {
					rawContactIds.add(rawContactIdCursor.getLong(0));
					col = rawContactIdCursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY);
					uri_displayname.put(rawContactIdCursor.getLong(0), rawContactIdCursor.getString(col));
				}
			}
		} finally {
			if (rawContactIdCursor != null) {
				rawContactIdCursor.close();
			}
		}
		return rawContactIds;
	}

}
