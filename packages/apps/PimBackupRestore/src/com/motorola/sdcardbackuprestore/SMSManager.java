package com.motorola.sdcardbackuprestore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import android.R.integer;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentProviderResult;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.Locale;
import android.content.ContentResolver;

import com.google.android.mms.util.SqliteWrapper;
import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;
import java.util.ArrayList;

public class SMSManager {

    private static final int SmsReadFlag = 1;

    
    private static final String TAG = "SMS Manager";
    private static final String fieldAddress = "address";
    private static final String fieldDate = "date";
    private static final String fieldDate2 = Constants.DATE2COLUMN;
    private static final String fieldBody = "body";
    private static final String fieldType = "type";
    private static final String fieldMode = "sub_id";  
    private static final String fieldRead = "read";
    //private static final String fieldStatus = "status";
    //private static final String fieldHref = "href";
    private static final String fieldSeen = "seen";
    //private static final String fieldSortIndex = "sort_index";
    private static final String SELECT_BY_ID = "_id=?";
    private static final String SMS_STYLE = "SMS";
    private static final String SMS_STYLE_OLD_VER = "MSG";
    
    private Context mContext = null;
    private boolean mIsDualMode = false;
    private boolean mHaveDate2 = false;
    private String mSmsString = "";
    private String mFileName = null;
    private String mDirPath = null;
    private int mActualMsgCounter = 0;
    private int mTotalMsgCounter  = 0;
    private int mActualFileCounter = 0;
    private int mBackedSmsCount = 0;
    private int mBackedMmsCount = 0;
	private int mTotalFileCounter = 0;
    private int mCurrentFileCounter = 0;
    
    private boolean mIsAltSdcard;
    
    private NotificationAgent mNotificationAgent = null;
    private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private boolean mIsFile = true;
    private MessageData mMessageData = null;
    private MMSData mMMSData = new MMSData();

    private ArrayList<ContentProviderOperation> mOPList = new ArrayList<ContentProviderOperation>();
    private ArrayList<Integer> mIndex = new ArrayList<Integer>();
    private ArrayList<ContentValues> mValuesList = new ArrayList<ContentValues>();

    private int BATCH_COMMIT_INTERVAL = 10;//xtp876 for IKCBSMMCPPRC-1775 from 50 to 10
    private String SMS_AUTHORITY = "sms";
    private Constants mConstants = null;
    
    private long mAllApplyBatchTime = 0;
    private static SMSManager mInstance = null;


    class MessageData {
        String sendType;        
        int MAType;    //1 GSM , 2 CDMA
        String phoneNo;
        long dateTime;
        long seqTime;
        String msgBody;
        String displayString;
        int seen;
    }

    public SMSManager(Context context, NotificationAgent notification) {
        mContext = context;
        mNotificationAgent = notification;
        mConstants = new Constants(context);
        mInstance = this;

    }

	 public static SMSManager getInstance() {
        return mInstance;
    }

    public int getActualFileCounter() {
        return mActualFileCounter;
    }
    
    public int getTotalFileCounter() {
        return mTotalFileCounter;
    }
    
    public boolean getIsWorking() {
        return mIsWorking;
    }
    
    public int getTotalCounter() {
        return mTotalMsgCounter;
    }   

    public int getActualCounter() {
        return mActualMsgCounter;
    }

    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }

    public int BackupSMS() {
        Log.v(TAG, "Enter BackupSMS()");
        int result = R.id.success;
        if (!mInterrupted) {
            mIsWorking = true;
            result = Backup();
            mIsWorking = false;
        }
        return result;
    }

    public int RestoreMsg(int action, ArrayList<File> file_list) {
        Log.v(TAG, "Enter RestoreMsg()");
        long pre_time = System.currentTimeMillis();
        int result = R.id.success;
        if (!mInterrupted) {
            mIsWorking = true;
            result = Restore(action, file_list);
            mSmsString = null;
            mIsWorking = false;
            
            if( Constants.IMPORT3RD_ACTION != action ){
//                mNotificationAgent.sendMsgWithNotification(action,
//                            Message.obtain(null, R.id.status, result, Constants.SMS_ACTION, counter_result));
            } else {
                // to request Activity to remove progress bar
//                mNotificationAgent.sendMsgWithoutNotification(action,
//                      Message.obtain(null, R.id.status, R.id.remove_dialog, Constants.SMS_ACTION, null));
            }
        } else {
            result = R.id.cancel;
        }
        Log.d(TAG + " Operation Time", "All apply batch time: " + mAllApplyBatchTime + "s");
        Log.d(TAG + " Operation Time", "Restore Operation time: " + (System.currentTimeMillis() - pre_time)/1000 + "s");
        return result;
    }
        
    private boolean isDualModeSetting() throws SQLiteException {
        Cursor cursor = null;
        //Firstly get id column of all records
        String[] projection = new String[] { Sms._ID };
        cursor = mContext.getApplicationContext().getContentResolver().query(
                Sms.CONTENT_URI, projection, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "Get all ID error!");
            throw new SQLiteException();
        }
        
        if (cursor.getCount() == 0) {
            //No record in DB for now, so get all columns directly
            cursor.close();
            cursor = mContext.getApplicationContext().getContentResolver().query(
                    Sms.CONTENT_URI, null, null, null, null);
            if (cursor == null) {
                Log.e(TAG, "Get all records error!");
                throw new SQLiteException();
            }
            if (cursor.getColumnIndex(fieldMode) != -1) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }     
        } else {
            //There are records in DB, so get all columns of the first record
            cursor.moveToFirst();
            String id = cursor.getString(cursor.getColumnIndex(Sms._ID));
            String[] selection_args = new String[] { id };
            cursor.close();
            cursor = mContext.getContentResolver().query(Sms.CONTENT_URI, null,
                    SELECT_BY_ID, selection_args, null);
            if (cursor == null) {
                Log.e(TAG, "Get one record error!");
                throw new SQLiteException();
            }
            if (cursor.getColumnIndex(fieldMode) != -1) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }         
        }   
    }

    private boolean haveDate2Col() throws SQLiteException {
        Cursor cursor = null;
        cursor = mContext.getApplicationContext().getContentResolver().query(
                Sms.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "Get all ID error!");
            throw new SQLiteException();
        } else {
            if (cursor.getColumnIndex(fieldDate2) != -1) {
                cursor.close();
                cursor = null;
                return true;
            } else {
                cursor.close();
                cursor = null;
                return false;
            }
        }
    }
    
    public boolean haveSubIdCol() throws SQLiteException {
        Cursor cursor = null;
        cursor = mContext.getApplicationContext().getContentResolver().query(
                Mms.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "Get all ID error!");
            throw new SQLiteException();
        } else {
            if (cursor.getColumnIndex(fieldMode) != -1) {
                cursor.close();
                cursor = null;
                return true;
            } else {
                cursor.close();
                cursor = null;
                return false;
            }
        }
    }
		
    private int Backup() {
        Log.v(TAG, "Enter Backup()");
        int result = R.id.success;
        Cursor SMSCursor = null;
        Cursor MMSCursor = null;
        mHaveDate2 = haveDate2Col();
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
        deleteOldBackupFile();
        result = SdcardManager.checkSdcardInitCondition(mContext, mIsAltSdcard, mConstants.DEFAULT_FOLDER);
        if (result != R.id.success) {
            return result;
        }
        try {
            mIsDualMode = isDualModeSetting();
            Log.v(TAG, "dual mode setting is : " + mIsDualMode);
            SMSCursor = exportSMS();
            MMSCursor = exportMms();
        } catch (SQLiteException e) {
            result = R.id.read_phone_db_error;
            Log.e(TAG, "Export SMS DB error!", e);
            return result;
        }

        if (SMSCursor != null || MMSCursor != null) {
            if (SMSCursor != null) {
                mBackedSmsCount = SMSCursor.getCount();
            }
            if (MMSCursor != null) {
                mBackedMmsCount = MMSCursor.getCount();
            }
            if (mBackedSmsCount > 0 || mBackedMmsCount > 0) {
                mTotalMsgCounter = mBackedSmsCount + mBackedMmsCount;
                mNotificationAgent.sendMsgWithoutNotification(
                        Constants.BACKUP_ACTION, Message.obtain(null, R.id.message_length, mTotalMsgCounter,
                                Constants.SMS_ACTION, mContext.getString(R.string.backuping)));
				File f = SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER, mIsAltSdcard);
                
                if (mBackedSmsCount != 0) {
                    result = backupSMS(SMSCursor, f);
                    if (result == R.id.no_sdcard) {
                        return result;
                    }
                }
                if (mBackedMmsCount != 0) {
                    if (mBackedSmsCount == 0) {
                        File checkFile = new File(f, Constants.SMS_FILE_NAME);
                        if (checkFile != null ) { 
                            checkFile.delete();
                        }
                    }
                    result = backupMMS(MMSCursor, f);
                    if (result == R.id.no_sdcard) {
                        return result;
                    }
                }
            } else {
                result = R.id.empty_storage;
            }
            if (SMSCursor != null) {
                SMSCursor.close();
            }
            if (MMSCursor != null) {
                MMSCursor.close();
            }
        } else {
            Log.e(TAG, "Get SMS Cursor error.");
            result = R.id.read_phone_db_error;
        }
        Log.v(TAG, "SMS backup complete.");
        return result;
    }
    
    private int Restore(int action, ArrayList<File> fileList) {
        Log.v(TAG, "Enter Restore() Version 2.0");
        int result = R.id.success;
        File file = null;
        if (fileList == null || fileList.size() == 0) {
            return R.id.empty_storage;
        }
        if (SdcardManager.noSdcard(mContext)) {
            return R.id.no_sdcard;
        }
        if (action == Constants.RESTORE_ACTION) {
            // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 begin
            if (isInnerMsgFileExist() && !isOuterMsgFileExist()) {
                file = new File(mConstants.SDCARD_PREFIX + "/" + fileList.get(0).getPath());
            } else if ((isOuterMsgFileExist() && !isInnerMsgFileExist()) || (isOuterMsgFileExist() && isInnerMsgFileExist())) {
                file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + fileList.get(0).getPath());
            } else {
                return R.id.empty_storage;
            }
            // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 end
        }
        try {
            mIsDualMode = isDualModeSetting();
            mHaveDate2 = haveDate2Col();
            if (action == Constants.RESTORE_ACTION) {
                mSmsString = SdcardManager.getFileString(file, "UTF-8");
                mTotalMsgCounter = getMsgNum(mSmsString);
                if (0 == mTotalMsgCounter) {
                    return R.id.sdcard_file_contents_error;
                }
                mFileName = file.getName();
                mDirPath = file.getParent();
                mIsFile = true;
                result = importMsgFromFile(action, mSmsString);
            }else if (action == Constants.IMPORT3RD_ACTION) {
                mIsFile = false;
                result = importMsgFromFiles(action, fileList);
            }else {
                Log.v(TAG, "Should not be here!");
                result = R.id.unknown_error;
            }
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "out of memory", e);
            return R.id.out_of_memory_error;
        } catch (SQLiteException e) {
            Log.e(TAG, "Export SMS DB error!", e);
            return R.id.write_phone_db_error;
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                Log.e(TAG, "FileNotFoundException of an SMS file reading in SD card", e);
                return R.id.read_sdcard_error;
            } else if (e instanceof IOException) {
                Log.e(TAG, "Read VMG file error", e);
                return R.id.read_sdcard_error;                
            } else if (e instanceof NullPointerException) {
                Log.e(TAG, "No content in the file", e);
                return R.id.empty_storage;
            } else {
                Log.e(TAG, e.toString());
                return R.id.unknown_error;
            }
        }
        if (result == R.id.empty_storage) {
            return result;
        }
    // to update SMS thread sorting sequence by the latest receiving/sending time
    try {
            long pre_time = System.currentTimeMillis();
            Uri thread_uri = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, -1);
            ContentResolver cr = mContext.getApplicationContext().getContentResolver();
            SqliteWrapper.delete(mContext, cr, thread_uri, null, null);
            Log.d(TAG + " Operation Time", "Update Msg thread time: " + (System.currentTimeMillis() - pre_time) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Export SMS DB error!", e);
            return R.id.write_phone_db_error;
        }
        return result;
    }
    
    private int importMsgFromFile(int action, String fileString) {
        Log.v(TAG, "Enter importMsgFromFile()");
        int result = R.id.unknown_error;
        Log.v("action", "" + action);
        Log.v("mTotalMsgCounter", "" + mTotalMsgCounter);
        if (0 == mTotalMsgCounter) {
            return R.id.sdcard_file_contents_error;
        } else {
            // To send total Vmsg length to Activity progress bar
            if (mIsFile) {
                mNotificationAgent.sendMsgWithoutNotification(action,
                        Message.obtain(null, R.id.message_length, mTotalMsgCounter, Constants.SMS_ACTION));
            }
           
            // A16516 add IKQLTTCW-1384
            if (action == Constants.RESTORE_ACTION) {
                result = insertVmgFileIntoPhone(action, null);
            } else {
                result = insertVmgFileIntoPhone(action, mFileName);
            }
            // A16516 end IKQLTTCW-1384
        }
        return result;        
    }
    
    private int importMsgFromFiles(int action, ArrayList<File> fileArray) {
        Log.v(TAG, "Enter importMsgFromFolder()");
        int result = R.id.success;
    	mTotalFileCounter = fileArray.size();
        mCurrentFileCounter = 0;
        if (mTotalFileCounter == 0) {
            return R.id.empty_storage;
        }
    	for (int i = 0; i < mTotalFileCounter; i++) {
    	    mCurrentFileCounter++;
            if (fileArray.get(i).getName().endsWith(Constants.vmgExtern)) {
                try {
                    mFileName = fileArray.get(i).getName();
                    mDirPath = fileArray.get(i).getParent();
                    mSmsString = SdcardManager.getFileString(fileArray.get(i), "UTF-8");
					mTotalMsgCounter = getMsgNum(mSmsString);
					result = insertVmgFileIntoPhone(action, mFileName);
                    if (result == R.id.success) mActualFileCounter++;
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "out of memory", e);
                    return R.id.out_of_memory_error;
                } catch (Exception e) {
                    if (e instanceof FileNotFoundException) {
                        Log.e(TAG, "FileNotFoundException of an SMS file reading in SD card", e);
                        return R.id.read_sdcard_error;
                    } else if (e instanceof IOException) {
                        Log.e(TAG, "Read VMG file error", e);
                        return R.id.read_sdcard_error;                        
                    }
                }
            }
            else {
                continue;
            }
        }
        Log.d(TAG, "importMsgFromFiles mActualFileCounter="+mActualFileCounter);
    	if(mActualFileCounter == 0){
    		return result;
    	}
    	return R.id.success;
    }
    
    private Cursor exportSMS() {
        Log.v(TAG, "Enter exportSMS()");
        String[] projection = null;
        if (mIsDualMode) {     
            if (mHaveDate2) {
                projection = new String[] { 
                        Sms._ID, 
                        fieldAddress, 
                        fieldDate,
                        fieldDate2,
                        fieldBody,
                        fieldMode,
                        fieldType,
                        fieldSeen};
            } else {
        projection = new String[] { 
                    Sms._ID, 
                    fieldAddress, 
                    fieldDate,
                    fieldBody,
                    fieldMode,
                    fieldType,
                    fieldSeen};
        }
        } else {
            if (mHaveDate2) {
            projection = new String[] { 
                        Sms._ID, 
                        fieldAddress, 
                        fieldDate,
                        fieldDate2,
                        fieldBody,
                        fieldType,
                        fieldSeen};
        } else {
        projection = new String[] { 
                    Sms._ID, 
                    fieldAddress, 
                    fieldDate,
                    fieldBody,
                    fieldType,
                    fieldSeen};
        }
        }
        
        String param = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_INBOX + " or " + Sms.TYPE + "=" + Sms.MESSAGE_TYPE_SENT;
        Log.v(TAG, "query SMS " + param);
        return mContext.getApplicationContext().getContentResolver().query(
                Sms.CONTENT_URI, projection, param, null, null);  
    }

    private Cursor exportMms() {
        Log.v(TAG, "Enter exportMms()");
        String selection = "(" + MMSConstants.MSG_BOX + "=" + Sms.MESSAGE_TYPE_INBOX 
            + " or " + MMSConstants.MSG_BOX + "=" + Sms.MESSAGE_TYPE_SENT + ")"
            + " and " + MMSConstants.MMS_TYPE + "<=" + MMSConstants.MMS_TYPE_REPORT;
        return mContext.getApplicationContext().getContentResolver().query(
                Mms.CONTENT_URI, null, selection, null, null);  
    }
    
    private int backupSMS(Cursor SMSCursor, File dirHandle) {
        Log.v(TAG, "Enter backupSMS()");
        String strMsgBody = "", boxType = "";
        String hrefString = "";
        int status = -1;
        int MAType = -1;
        String fileName = Constants.SMS_FILE_NAME;
        int modeColumn = -1;
        int idColumn;
        int date2Column = -1;
        int indexOfstrDisplay;
        long dt2 = 0;
        String seqTimeString = "";
        
        if ( null == SMSCursor )  return R.id.unknown_error;
    
        idColumn = SMSCursor.getColumnIndex(Sms._ID);
        
        int numberColumn = SMSCursor.getColumnIndex(fieldAddress);
        int dateColumn = SMSCursor.getColumnIndex(fieldDate);
        if (mHaveDate2) {
            date2Column = SMSCursor.getColumnIndex(fieldDate2);
        }
        int bodyColumn = SMSCursor.getColumnIndex(fieldBody);
        int typeColumn = SMSCursor.getColumnIndex(fieldType);
        if (mIsDualMode) {
            modeColumn = SMSCursor.getColumnIndex(fieldMode);
        }
        int seenColumn = SMSCursor.getColumnIndex(fieldSeen);
        int readCount = 1;
        int result = R.id.success;

        // to delete previous backuped SMS file
        File checkFile = new File(dirHandle, fileName);
        if(checkFile != null ) checkFile.delete();
        SMSCursor.moveToFirst();
        while (!SMSCursor.isAfterLast() && !mInterrupted) {
            Log.v(TAG, "ID " + SMSCursor.getInt(idColumn) + " NO "
                + SMSCursor.getString(numberColumn));

            if (SMSCursor.getInt(typeColumn) == Sms.MESSAGE_TYPE_INBOX) {
                boxType = Constants.DELIVER + "\n";
            } else if (SMSCursor.getInt(typeColumn) == Sms.MESSAGE_TYPE_SENT) {
                boxType = Constants.SUBMIT + "\n";
            } else {
                Log.v(TAG, "message neither inbox or sent box" + " NO " 
                        + SMSCursor.getString(numberColumn));
                SMSCursor.moveToNext();
                continue;
            }
            long dt = SMSCursor.getLong(dateColumn);
            if (date2Column != -1) {
                dt2 = SMSCursor.getLong(date2Column);
            }

            if (mIsDualMode) {
                MAType = SMSCursor.getInt(modeColumn);
            }
            
            if (mHaveDate2) {
                seqTimeString = MsgTag.tagSeqTime + new Long(dt2).toString() + MsgTag.LINE_CHANGE;
            } else {
                seqTimeString = "";
            }
            /*if (SMSCursor.getString(hrefColumn) != null && !SMSCursor.getString(hrefColumn).trim().equals("")) {
                hrefString = MsgTag.LINE_CHANGE + MsgTag.tagHref + SMSCursor.getString(hrefColumn);
            } else {
                hrefString = "";
            }*/
            
              strMsgBody = MsgTag.tagBegMsg + MsgTag.LINE_CHANGE
                + MsgTag.tagMsgVer + MsgTag.LINE_CHANGE
                + MsgTag.tagIrmsTyp + SMS_STYLE + MsgTag.LINE_CHANGE
                + MsgTag.tagMsgTyp + boxType
                + MsgTag.tagMATyp + MAType + MsgTag.LINE_CHANGE
                + seqTimeString
                //+ MsgTag.tagStatus + status + MsgTag.LINE_CHANGE
                + MsgTag.tagSeen + SMSCursor.getInt(seenColumn) + MsgTag.LINE_CHANGE
                //+ MsgTag.tagSortIndex + SMSCursor.getLong(sortindexColumn) + MsgTag.LINE_CHANGE
                + MsgTag.tagBegCard
                + MsgTag.tagCardVer
                + MsgTag.tagTel + SMSCursor.getString(numberColumn)
                + MsgTag.LINE_CHANGE
                + MsgTag.tagEndCard
                + MsgTag.tagBegEnv
                + MsgTag.tagBegBody
                + MsgTag.tagDate + new Long(dt).toString()
                //+ hrefString
                + MsgTag.LINE_CHANGE + (SMSCursor.getString(bodyColumn) == null ? "" : SMSCursor.getString(bodyColumn))
                + MsgTag.LINE_CHANGE 
                + MsgTag.tagEndBody
                + MsgTag.tagEndEnv 
                + MsgTag.tagEndMsg + MsgTag.LINE_CHANGE;

            if (dirHandle != null) {
                Log.v(TAG, "Inside backupsms() new created directory is " + dirHandle.toString());
            } else {
                Log.e(TAG, "Inside backupsms() new created directory is null!");
                result = R.id.write_sdcard_error;
                break;
            }

            result = SdcardManager.saveInSdcard(mContext, dirHandle, fileName, strMsgBody, mIsAltSdcard);
            if (result == R.id.success) {
                String strDisplay = SMSCursor.getString(bodyColumn);
                if (strDisplay == null) {
                    strDisplay = "";
                }
                indexOfstrDisplay = strDisplay.indexOf("\n");
                if ( indexOfstrDisplay != -1 ) {
                    strDisplay = strDisplay.substring(0, indexOfstrDisplay);
                }
                if ( Constants.SMS_DISPLAY_LEN < strDisplay.length() ) {
                    strDisplay = strDisplay.substring(0, Constants.SMS_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
                } else {
                    strDisplay = strDisplay.substring(0, strDisplay.length());
                    if ( indexOfstrDisplay != -1 ) {
                        strDisplay = strDisplay + mContext.getString(R.string.more_string_display);
                    }
                }
                
                mActualMsgCounter ++;
                mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                    Message.obtain(null, R.id.under_progress, readCount, Constants.SMS_ACTION,
                        mContext.getString(R.string.backuping, mContext.getString(R.string.sms))+"\n\n"+strDisplay));

                sendNotificationProgress(readCount, mTotalMsgCounter, Constants.BACKUP_ACTION, null);

                readCount++;
                SMSCursor.moveToNext();
            } else {
                Log.v(TAG, "write SD card return error while backup SMS");
                break;
            }
        }
        if (mInterrupted) {
            result = R.id.cancel;
        }
        return result;
    }

    // Deletes all files and subdirectories under dir. 
    // Returns true if all deletions were successful. 
    // If a deletion fails, the method stops attempting to delete and returns false. 
    public static boolean deleteDir(File dir) {
        boolean isSuccess;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children == null) {
                return dir.delete();
            }
            for (int i=0; i<children.length; i++) {
                if (children[i].isDirectory()) {
                    isSuccess = deleteDir(children[i]);
                    if (!isSuccess) {
                        return false;
                    }
                    
                } else if (children[i].isFile()) {
                    isSuccess = children[i].delete();
                    if (!isSuccess) {
                        return false;
                    }
                }
            }
            isSuccess = dir.delete();
            if (!isSuccess) {
                return false;
            }
         } 
        return true;
         // The directory is now empty so delete it 
         //return dir.delete(); 
     } 
    
    private int backupMMS(Cursor cs, File dirHandle) {
        int result = R.id.success;
        if (cs == null) {
            return R.id.unknown_error;
        }
        int indexOfstrDisplay;
        File parts = new File(dirHandle,Constants.FILEPATH_PARTS);
        parts.mkdirs();
        new Utility(mContext).mediaScanFolder(parts);
        
        String mmsBackupPath = dirHandle.getPath();
        cs.moveToFirst();
        StringBuffer sb = new StringBuffer();
        while(!cs.isAfterLast() && !mInterrupted){
            int id = cs.getInt(cs.getColumnIndexOrThrow(Mms._ID));
            
            MMSData mmsData = new MMSData();
            mmsData.fromPduId(mContext.getContentResolver(), id);
            sb.append(mmsData.serialize(mmsBackupPath, mContext.getContentResolver()));
            if(sb.length() >= MMSConstants.WRITE_BLOCK){
    			result = SdcardManager.saveInSdcard(mContext, new File(mmsBackupPath), Constants.SMS_FILE_NAME, sb.toString(), mIsAltSdcard);
                if (result != R.id.success) {
                    return result;
                }
                sb = new StringBuffer(); 
            }
            String strDisplay = mmsData.getSummary() != null ? mmsData.getSummary() : mContext.getString(R.string.mms_default_string);
            indexOfstrDisplay = strDisplay.indexOf("\n");
            if ( indexOfstrDisplay != -1) {
                strDisplay = strDisplay.substring(0, indexOfstrDisplay);
            }
            if ( Constants.SMS_DISPLAY_LEN < strDisplay.length() ) {
                strDisplay = strDisplay.substring(0, Constants.SMS_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
            } else {
                strDisplay = strDisplay.substring(0, strDisplay.length());
                if ( indexOfstrDisplay != -1 ) {
                    strDisplay = strDisplay + mContext.getString(R.string.more_string_display);
                }
            }
            mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.under_progress, mActualMsgCounter, Constants.SMS_ACTION,
                    mContext.getString(R.string.backuping, mContext.getString(R.string.sms))+"\n\n"+strDisplay));
            sendNotificationProgress(mActualMsgCounter, mTotalMsgCounter, Constants.BACKUP_ACTION, null);

            cs.moveToNext();
            mActualMsgCounter ++;
        }
        
        if(sb.length() > 0){
    		result = SdcardManager.saveInSdcard(mContext, new File(mmsBackupPath), Constants.SMS_FILE_NAME, sb.toString(), mIsAltSdcard);
            if (result != R.id.success) {
                return result;
            }
        }
        
        if (mInterrupted) {
            return R.id.cancel;
        } else {
            return R.id.success;
        }
        
    }
 
    private int parseMMS(StringBuffer subject, String buffer, String dirPath, int action) {
        Log.d(TAG, "Enter parseMMS()");
        String allMMS = buffer;
        if (allMMS == null) return R.id.read_sdcard_error;
        String folderPath = dirPath;
        String fileName = mFileName;
        if (fileName != null) {
            fileName = BackupRestoreService.getInstance().ignorePathPrefix(fileName);
        }
        int startInd = allMMS.indexOf(MsgTag.tagBegMsg);
        int endInd = allMMS.indexOf(MsgTag.tagEndMsg);
        if (mInterrupted) return R.id.success;
        mMMSData.deSerialize(folderPath, allMMS);
        mMMSData.pushIntoContentResolver(mContext);
        subject.delete(0, subject.length());
        subject.append(mMMSData.getSummary() != null ? mMMSData.getSummary()    : mContext.getString(R.string.mms_default_string));
        Log.v("startInd", "startInd = " + startInd);
        startInd = allMMS.indexOf(MsgTag.tagBegMsg, endInd + MsgTag.tagEndMsg.length());
        endInd = allMMS.indexOf(MsgTag.tagEndMsg, startInd + MsgTag.tagBegMsg.length());
        if( mInterrupted ){
            Log.d(TAG, "importOneMMSFolder() detected mInterrupted");
            return R.id.cancel;
        }
        return R.id.success;
    }
    
    private String displayString(String content, int action) {
        String msgBody = null;
        String displayString = null;
        int indexOfcontent = -1;
        if (action == Constants.RESTORE_ACTION) {
            msgBody = mContext.getString(R.string.restoring, mContext.getString(R.string.sms)) 
                    + "\n\n";
        } else if (action == Constants.IMPORT3RD_ACTION) {
			msgBody = mContext.getString(R.string.import3rding0, mContext.getString(R.string.sms), mCurrentFileCounter, mTotalFileCounter, mFileName) 
            + "\n\n";
        }
        indexOfcontent = content.indexOf("\n");
        if ( indexOfcontent != -1) {
            content = content.substring(0, indexOfcontent);
        }
        if ( Constants.SMS_DISPLAY_LEN < content.length() ) {
            displayString = msgBody + content.substring(0, Constants.SMS_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
        } else {
            displayString = msgBody + content.substring(0, content.length());
            if ( indexOfcontent != -1 ) {
                displayString = displayString + mContext.getString(R.string.more_string_display);
            }
        }
        return displayString;
        /*else {
            msgBody = mContext.getString(R.string.import3rding0, mContext.getString(R.string.sms), ": "+mFileName) 
                    + "\n\n" + content;
            if ( Constants.SMS_DISPLAY_LEN < msgBody.length() ) {
                displayString = msgBody.substring(0, Constants.SMS_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
            } else {
                int len = Constants.SMS_DISPLAY_LEN + mContext.getString(R.string.more_string_display).length() - msgBody.length();
                char[] space = new char[len];
                for (int i=0; i<len; i++)  space[i]=' ';
                displayString = msgBody.substring(0, msgBody.length()) + String.valueOf(space) +  String.valueOf(space);
            }
            return displayString;
        }
        */
    }
    private int insertVmgFileIntoPhone(int action, String fileName) {
        Log.d(TAG, "Enter insertVmgFileIntoPhone()");
        int pos1 = 0;
        int pos2 = 0;
        int num = 0;
        int result = R.id.success;
        boolean isFinished = false;
        boolean preIsSMS = false;
        boolean doing = true;
        String sBuffer = "";
        if (fileName != null) {
            fileName = BackupRestoreService.getInstance().ignorePathPrefix(fileName);
        }
        int count = 0;
        int addCount = 0;
        int msgType = Constants.OTHER_TYPE;
        // To send total number of each file to Activity progress bar 
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalMsgCounter, Constants.SMS_ACTION));
        try {
            if( -1==(pos2=mSmsString.indexOf(MsgTag.tagEndMsg, pos1)))  return R.id.sdcard_file_contents_error;
            while ( doing ) {
                sBuffer = mSmsString.substring(pos1, pos2+MsgTag.tagEndMsg.length());
                msgType = getMsgType(sBuffer);
                if (null == sBuffer) {
                    if (addCount > 0) commitOP(mContext.getContentResolver());
                    result = R.id.sdcard_file_contents_error;
                    isFinished = false;
                    break;
                }
                if (mInterrupted) {
                    if (addCount > 0 && msgType == Constants.SMS_TYPE) commitOP(mContext.getContentResolver());
                    break;
                }
                if(msgType == Constants.SMS_TYPE) preIsSMS = true;
                
                if (preIsSMS && msgType == Constants.MMS_TYPE && addCount > 0) {
                    commitOP(mContext.getContentResolver());
                }
                count++;//xtp876 for IKCBSMMCPPRC-1775
                switch (msgType) {
                    case Constants.SMS_TYPE:
                    Log.v("insertVmgFileIntoPhone()", "msgType:SMS");
                    getMessageData(sBuffer);
                    mNotificationAgent.sendMsgWithoutNotification(action,
                            Message.obtain(null, R.id.under_progress, count, Constants.SMS_ACTION, displayString(mMessageData.displayString, action)));
                    sendNotificationProgress(count, mTotalMsgCounter, action, fileName);

					if (!insertSmsDBNotCommit(mMessageData)) {
                        if (addCount > 0) commitOP(mContext.getContentResolver());
                        result = R.id.write_phone_db_error;
                        doing = false;
                        isFinished = false;
                        break;
                    } else {
                        addCount++;
                        mActualMsgCounter ++;
                        Log.d(TAG, "containValidSms = true ; mActualSmsCounter = " + mActualMsgCounter);
                        // Make commit interval
                        if (addCount >= BATCH_COMMIT_INTERVAL) {
                            Log.d(TAG, "Commit insert Message interval");
                            num = commitOP(mContext.getContentResolver());
                            addCount = 0;
                            if (num == 0) {
                                Log.e(TAG, "Commit error at the middle");
                                result = R.id.write_phone_db_error;
                                doing = false;
                                isFinished = false;
                                break;
                            } 
                        }
                    }           
                    pos1 = pos2 + MsgTag.tagEndMsg.length()+1;
                    if( -1==(pos2=mSmsString.indexOf(MsgTag.tagEndMsg, pos1)) ) {
                        // Make commit when reach the end of the .vmg file
                        if (addCount > 0) {
                            Log.d(TAG, "Reach the end of importing and commit");
                            num = commitOP(mContext.getContentResolver());
                            if (num == 0) {
                                Log.e(TAG, "Commit error at the end of import .vmg file");
                                result = R.id.write_phone_db_error;
                                isFinished = false;
                                break;
                            }
                        }
                        doing = false;
                        isFinished = true;
                    }

            break;
        case Constants.MMS_TYPE:
            Log.v("insertVmgFileIntoPhone()", "msgType:MMS");
            StringBuffer subject = new StringBuffer();
            result = parseMMS(subject, sBuffer, mDirPath, action);
            mNotificationAgent.sendMsgWithoutNotification(action, Message.obtain(null, R.id.under_progress, count,
                        Constants.SMS_ACTION, displayString(subject.toString(), action)));
            sendNotificationProgress(count, mTotalMsgCounter, action, fileName);
            mActualMsgCounter ++;
            if (result != R.id.success) {
                if (result == R.id.cancel) {
                    mInterrupted = true;
                }
                doing = false;
                isFinished = false;
                break;
            }
            pos1 = pos2 + MsgTag.tagEndMsg.length()+1;
            if( -1==(pos2=mSmsString.indexOf(MsgTag.tagEndMsg, pos1)) ) {
                doing = false;
                isFinished = true;
                break;
            }
            preIsSMS = false;
            break;
        case Constants.OTHER_TYPE:
            Log.v("insertVmgFileIntoPhone()", "msgType:error");
            doing = false;
            result = R.id.sdcard_file_contents_error;
            break;
        default:
            Log.e(TAG, "getMsgType() Error");
            result = R.id.sdcard_file_contents_error;
            doing = false;
            break;
        }

            }
        } catch (Exception e) {
            Log.e(TAG, "SMS file format Exception: ", e);
            result = R.id.unknown_error;
            isFinished = false;
        }
        
        if (true==isFinished) {
            Log.d(TAG, "isFinished = true");
            result = R.id.success;
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

    private boolean insertSmsDBNotCommit(MessageData mesgData) {
        Log.d(TAG, "Enter insertSmsDBNotCommit()");
        int numOperations = mValuesList.size();
        Log.v(TAG, "numOperations = " + numOperations);
        mIndex.add(numOperations);

        ContentValues values = new ContentValues();
        values.put(fieldAddress, mesgData.phoneNo);
        values.put(fieldDate, mesgData.dateTime);
        if (mHaveDate2) {
	    values.put(fieldDate2, mesgData.seqTime);
	}
        //values.put(fieldDate2, mesgData.seqTime);

        values.put(fieldBody, mesgData.msgBody);
        values.put(fieldSeen, mesgData.seen);

        if (mesgData.sendType.equals(Constants.DELIVER)) {
            values.put(fieldType, Sms.MESSAGE_TYPE_INBOX);
        } else if (mesgData.sendType.equals(Constants.SUBMIT)) {
            values.put(fieldType, Sms.MESSAGE_TYPE_SENT);
        } else {
            Log.e(TAG, "Wrong send rece type is " + mesgData.sendType + "!");
            return false;
        }

        if (mIsDualMode) {
            values.put(fieldMode, mesgData.MAType);
        }

        values.put(fieldRead, SmsReadFlag);
        mValuesList.add(values);

        return true;
    }

    private int commitOP(ContentResolver resolver){
        Log.d(TAG, "Enter commitOP()");
        ContentValues[] values = mValuesList.toArray(new ContentValues[mValuesList.size()]);
        int numInsert = 0;
        numInsert = resolver.bulkInsert(Sms.CONTENT_URI, values);

        mValuesList.clear();
        mIndex.clear();
        return numInsert;
    }

    private void getMessageData(String vmg) {
        int iStart = 0, iEnd = 0;
        mMessageData = new MessageData();
    
        iStart = vmg.indexOf(MsgTag.tagMsgTyp);
        vmg = vmg.substring(iStart + MsgTag.tagMsgTyp.length());
        iEnd = vmg.indexOf("\n");
        mMessageData.sendType = vmg.substring(0, iEnd);
    
        iStart = vmg.indexOf(MsgTag.tagMATyp);
        vmg = vmg.substring(iStart + MsgTag.tagMATyp.length());
        iEnd = vmg.indexOf("\n");
        String strMAType = vmg.substring(0, iEnd);
        if(strMAType != null){
            if(strMAType.startsWith("1") || strMAType.startsWith("GSM")){
                mMessageData.MAType = TelephonyManager.PHONE_TYPE_GSM;
            }else{
                mMessageData.MAType = TelephonyManager.PHONE_TYPE_CDMA;
            }
        }
        Log.v(TAG, "MA type is " + mMessageData.MAType + "\n");
    
        iStart = vmg.indexOf(MsgTag.tagSeqTime);
        if (iStart != -1) {
            vmg = vmg.substring(iStart + MsgTag.tagSeqTime.length());
            if((iEnd = vmg.indexOf("\r\n")) == -1){
                iEnd = vmg.indexOf("\n");
            }            
            String seqDateString = vmg.substring(0, iEnd).trim();
            char ch = seqDateString.charAt(0);
            if(!Character.isDigit(ch)){
                try {
                    mMessageData.seqTime = DateFormat.getDateTimeInstance
                        (DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH).parse(seqDateString).getTime();
                } catch (ParseException e1) {
                    mMessageData.seqTime = 0;
                }
            } else {
                mMessageData.seqTime = Long.parseLong(seqDateString);
            }
        } else {
            mMessageData.seqTime = 0;
        }
        
        /*iStart = vmg.indexOf(MsgTag.tagStatus);
        if (iStart != -1) {
            vmg = vmg.substring(iStart + MsgTag.tagStatus.length());
            iEnd = vmg.indexOf("\r\n");
            mMessageData.status = Integer.parseInt(vmg.substring(0, iEnd).trim());
        } else {
            mMessageData.status = -1;
        }*/
        
        iStart = vmg.indexOf(MsgTag.tagSeen);
        if (iStart != -1) {
            vmg = vmg.substring(iStart + MsgTag.tagSeen.length());
            iEnd = vmg.indexOf("\r\n");
            mMessageData.seen = Integer.parseInt(vmg.substring(0, iEnd).trim());
        } else {
            mMessageData.seen = 1;
        }
        
        /*iStart = vmg.indexOf(MsgTag.tagSortIndex);
        if (iStart != -1) {
            vmg = vmg.substring(iStart + MsgTag.tagSortIndex.length());
            iEnd = vmg.indexOf("\r\n");
            mMessageData.sort_index = Long.parseLong(vmg.substring(0, iEnd).trim());
        } else {
            mMessageData.sort_index = -1;
        }*/
                
        iStart = vmg.indexOf(MsgTag.tagTel);
        vmg = vmg.substring(iStart + MsgTag.tagTel.length());
        iEnd = vmg.indexOf("\n");
        mMessageData.phoneNo = vmg.substring(0, iEnd).trim();
    
        iStart = vmg.indexOf(MsgTag.tagDate);
        if (iStart < 0) {
            //This is to add the position of blank or ":"
            iStart = vmg.indexOf("Date:");
        }
        vmg = vmg.substring(iStart + MsgTag.tagDate.length());
        int gap = 2;        
        if((iEnd = vmg.indexOf("\r\n")) == -1){
            iEnd = vmg.indexOf("\n");
            gap = 1;
        }
        String dateBuf = vmg.substring(0, iEnd);
        char ch = dateBuf.charAt(0);
        if(!Character.isDigit(ch)){
            try {
                mMessageData.dateTime = DateFormat.getDateTimeInstance
                    (DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH).parse(dateBuf).getTime();
            } catch (ParseException e) {
                mMessageData.dateTime = 0;
            }
        } else {
            mMessageData.dateTime = Long.parseLong(dateBuf);
        }
        /*if (mMessageData.sort_index == -1) {
            mMessageData.sort_index = mMessageData.dateTime;
        }*/
        vmg = vmg.substring(iEnd + gap);
        /*iStart = vmg.indexOf(MsgTag.tagHref);
        if (iStart != -1) {
            iEnd = vmg.indexOf(MsgTag.LINE_CHANGE);
            mMessageData.href = vmg.substring(iStart + MsgTag.tagHref.length(), iEnd);
            vmg = vmg.substring(iEnd + MsgTag.LINE_CHANGE.length());
        } else {
            mMessageData.href = "";
        }*/
        iEnd = vmg.indexOf(MsgTag.tagEndBody.substring(0, MsgTag.tagEndBody.length() - 1));
        mMessageData.msgBody = getMsgBodyData(vmg.substring(0, iEnd)); 

        if ( Constants.SMS_DISPLAY_LEN < mMessageData.msgBody.length() ) {
            mMessageData.displayString = mMessageData.msgBody.substring(0, Constants.SMS_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
        } else {
            mMessageData.displayString = mMessageData.msgBody.substring(0, mMessageData.msgBody.length());
        }
    }

    private void sendNotificationProgress (int count, int total, int action, String fileName) {
        NotificationItem item = new NotificationItem();
        item.id = 1;
        item.totalCurrent = count;
        item.totalTotal = total;
        item.direction = action;
        Log.v(TAG, "Backup/restore SMS, current is " + item.totalCurrent + 
                " total is " + item.totalTotal + " action is " + item.direction);
        if (item.direction == Constants.BACKUP_ACTION) {
            item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.sms));
        } else if (item.direction == Constants.RESTORE_ACTION) {
            item.description = mContext.getString(R.string.restoring, mContext.getString(R.string.sms));
        } else {
            if (fileName != null) {
                item.description = mContext.getString(R.string.import3rding, 
                        mContext.getString(R.string.sms), "\n" + fileName);
            } else {
                item.description = mContext.getString(R.string.import3rding, 
                        mContext.getString(R.string.sms), "");
                Log.e(TAG, "Miss file name for import3rdParty action type");
            }
        }
        Log.d(TAG, "Description is " + item.description);
        mNotificationAgent.sendNotificationProgress(item);
        return;
    }

    public boolean isSmsDBEmpty() {
        String[] projection = new String[] { Threads._ID };
        Uri simpleUri = Threads.CONTENT_URI.buildUpon().
            appendQueryParameter("simple", "true").build();
        Cursor cursor = mContext.getApplicationContext().getContentResolver().query(
                simpleUri, projection, null, null, null);
        
        if (cursor != null) {
            if (cursor.getCount() != 0) {
                cursor.close();
                return false;
            } else {
                cursor.close();
                return true;
            }
        } else {
            return true;
        }
    }
    
    public String getMsgBodyData(String str) {
        if (str == null) {
            return "";
        }
        if (str.endsWith("\n")) {
            str = str.substring(0, str.length()-1);
            if (str.endsWith("\r")) {
                str = str.substring(0, str.length()-1);
            }
        }
        return str;
    }
    
    public static int getMsgType(String buffer) {
        if(buffer == null) return Constants.OTHER_TYPE;
        else {
            if(buffer.contains(MsgTag.tagIrmsTyp + SMS_STYLE) || buffer.contains(MsgTag.tagIrmsTyp + SMS_STYLE_OLD_VER)) {
                return Constants.SMS_TYPE;
            } else if (buffer.contains(MsgTag.tagIrmsTyp + MMSConstants.MMS_STYLE)) {
                return Constants.MMS_TYPE;
            } else {
                return Constants.OTHER_TYPE;
            }
        }
    }
    
    public static int getMsgNum(String msgFileString) {
        int msgNum = 0;
        if (msgFileString == null) {
            return msgNum;
        }
        int pos = msgFileString.indexOf(MsgTag.tagBegMsg);
        while( -1 != (pos=msgFileString.indexOf(MsgTag.tagEndMsg, pos)) ) {
            pos += MsgTag.tagEndMsg.length();
            msgNum++;
        }
        return msgNum;
    }
    
    public static int getTotalVMGFile(File dirPath) {
        int total = 0;
        if (dirPath.isDirectory()) {
            File[] fileList = dirPath.listFiles();
            if(fileList == null) return 0;
            for (File file : fileList) {
                if (file.getName().endsWith(Constants.vmgExtern)) {
                    total++;
                }
            }
            return total;
        } else {
            Log.e(TAG, "It's not a dir");
            return -1;
        }
    }
    
    public static int getMsgFileType(File file) {
        //  0:other files;  1:only sms file  2:only mms file
        if (file == null) {
            return Constants.OTHER_TYPE;
        }
        if (file.getName().endsWith(Constants.vmgExtern)) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(file));
                String tmpString = null;
                tmpString = in.readLine();
                while (tmpString == null || tmpString == "") {
                    tmpString = in.readLine();
                }
                if (tmpString.startsWith(MsgTag.tagBegMsg)) {
                    tmpString = in.readLine();
                    if (tmpString.startsWith(MsgTag.TAG_THREAD_ID)) {
                        return Constants.MMS_TYPE;
                    } else {
                        return Constants.SMS_TYPE;
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File Not Found!");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Write Error!");
            }
        }
        return Constants.OTHER_TYPE;
    }
    
    public boolean isInnerMsgFileExist() {
        File innerSmsFile = new File( mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.SMS_FILE_NAME );
        if (innerSmsFile.exists()) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isOuterMsgFileExist() {
        File outerSmsFile = new File( mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.SMS_FILE_NAME );
        if (outerSmsFile.exists()) {
            return true;
        } else {
            return false;
        }
    }
    
    public  boolean isMsgFileExist() {
        if(isInnerMsgFileExist() || isOuterMsgFileExist()) return true ;
        return false;
    }
    
    
    public static void deleteMsgStuff(File rootFolder){        
        File attachmentFolder = new File(rootFolder, Constants.FILEPATH_PARTS);
        deleteDir(attachmentFolder);
        File msgFile = new File(rootFolder,Constants.SMS_FILE_NAME);
        if (msgFile.exists()) {
            msgFile.delete();
        }
    }
    
    public void deleteOldBackupFile() {
        File inner_file = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER);
        // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 begin
        if (inner_file != null && inner_file.exists() && !mIsAltSdcard) {
            deleteMsgStuff(inner_file);
        }
        File outer_file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                                  , Constants.CONTACTS_FILE_NAME);
        if (outer_file != null && outer_file.exists() && mIsAltSdcard) {
            deleteMsgStuff(outer_file);
        }
        // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 end
    }
    
}
