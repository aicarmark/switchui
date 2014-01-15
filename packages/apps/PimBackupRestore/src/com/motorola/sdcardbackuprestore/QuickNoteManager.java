package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONException;
import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

public class QuickNoteManager {
	
	private final static String TAG = "QuickNote Manager";
	
	private Context mContext = null;
	private NotificationAgent mNotificationAgent = null;
	private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private int mTotalQuickNoteCounter = 0;
    private int mActualQuickNoteCounter = 0;
    private int mActualFileCounter = 0;
    private int mTotalFileCounter = 0;
    private int mCurrentFileCounter = 0;
    private boolean mIsAltSdcard;
	private File mQNAttach = null;
	private String mQNString = null;
	private String mFileName = null;
	private String mDirPath = null;
	private boolean mIsFile = false;
    private Constants mConstants = null;
    private QuickNoteBackupRestore mQNBackupRestore = null;
	
	
	public QuickNoteManager(Context context, NotificationAgent notification) {
		mContext = context;
		mNotificationAgent = notification;
		mConstants = new Constants(context);
		mQNBackupRestore = new QuickNoteBackupRestore(context);
	}
	
	public boolean getIsWorking() {
		return mIsWorking;
	}
	
	public int getTotalFileCounter() {
	    return mTotalFileCounter;
	}
	
    public int getTotalCounter() {
        return mTotalQuickNoteCounter;
    }   

    public int getActualCounter() {
        return mActualQuickNoteCounter;
    }
    
    public int getActualFileCounter() {
    	return mActualFileCounter;
    }

    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }
    
    public int BackupQuickNote() {
    	Log.v(TAG, "Enter BackupQuickNote()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
            result = Backup();
			mIsWorking = false;
        } else {
			result = R.id.cancel;
		}
        return result;
    }
    
    public int RestoreQuickNote(int action, ArrayList<File> fileList) {
    	int res = R.id.success;
    	if (!mInterrupted) {
			mIsWorking = true;
            res = Restore(action, fileList);
			mIsWorking = false;
            // update the thumbnail
            mediaScanFolder(new File(mConstants.QN_PICTURE_FOLDER_1));
            mediaScanFolder(new File(mConstants.QN_PICTURE_FOLDER_2));
            mediaScanFolder(new File(mConstants.QN_PICTURE_FOLDER_3));
        } else {
            res = R.id.cancel;
        }
    	return res;
    }
    
    private int Restore(int action, ArrayList<File> fileList) {
    	int res = R.id.success;
    	File file = null;
    	File[] files = null;
     	if (fileList == null || fileList.size() == 0) {
			return R.id.empty_storage;
		}
    	if (SdcardManager.noSdcard(mContext)) {
    		return R.id.no_sdcard;
    	}
		if (action == Constants.RESTORE_ACTION) {
			if ((!isOuterQNFileExist() && isInnerQNFileExist())) {
				file = new File(mConstants.SDCARD_PREFIX + "/"+ fileList.get(0).getPath());
			} else if ((!isInnerQNFileExist() && isOuterQNFileExist()) || (isInnerQNFileExist() && isOuterQNFileExist())) {
				file = new File(mConstants.ALT_SDCARD_PREFIX + "/"+ fileList.get(0).getPath());
			} else {
				return R.id.empty_storage;
			}
		}
    	try {
        	if (action == Constants.RESTORE_ACTION) {
        		mQNString = SdcardManager.getFileString(file, "UTF-8");
        		mTotalQuickNoteCounter = getQNNum(mQNString);
        		if (0 == mTotalQuickNoteCounter) {
        			return R.id.sdcard_file_contents_error;
        		}
        		mFileName = file.getName();
        		mDirPath = file.getParent();
        		mIsFile = true;
        		res = importQNFromFile(action, mQNString);
    		}else if (action == Constants.IMPORT3RD_ACTION) {
    			mIsFile = false;
    			files = new File[fileList.size()];
    			files = fileList.toArray(files);
    			res = importQNFromFiles(action, files);
    		}else {
    			Log.v(TAG, "Should not be here!");
    			res = R.id.unknown_error;
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


      mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));
    	return res;
    }
    
    private int importQNFromFile(int action, String fileString) {
    	int result = R.id.unknown_error;
    	if (0 == mTotalQuickNoteCounter) {
			return R.id.sdcard_file_contents_error;
		} else {
            // To send total QuickNote length to Activity progress bar
			if (mIsFile) {
	            mNotificationAgent.sendMsgWithoutNotification(action,
	                    Message.obtain(null, R.id.message_length, mTotalQuickNoteCounter, Constants.QUICKNOTE_ACTION));
			}
            if (action == Constants.RESTORE_ACTION) {
                result = insertQNFileIntoPhone(action, null);
            } else {
                result = insertQNFileIntoPhone(action, mFileName);
            }
        }
    	return result;    	
    }
    
    private int importQNFromFiles(int action, File[] fileList) {
    	int result = R.id.success;
        mTotalFileCounter = fileList.length;
        mCurrentFileCounter = 0;
        if (0 == mTotalFileCounter)  return R.id.empty_storage;
        
        for (int i=0; i<mTotalFileCounter && !mInterrupted; i++) {
            mCurrentFileCounter++;
            try {
                mQNString = SdcardManager.getFileString(fileList[i], "UTF-8");
                mTotalQuickNoteCounter = getQNNum(mQNString);
            }
            catch (NullPointerException e){
	            Log.e(TAG, "Wrong format of QN file");
	            result = R.id.sdcard_file_contents_error;
	            continue;
            } catch (OutOfMemoryError e) {
    			Log.e(TAG, "out of memory", e);
    			result = R.id.out_of_memory_error;
	            continue;
    		} catch (SQLiteException e) {
    			Log.e(TAG, "Export error!", e);
    			result = R.id.write_phone_db_error;
	            continue;
    		} catch (Exception e) {
                if (e instanceof FileNotFoundException) {
                	Log.e(TAG, "FileNotFoundException of an QN file reading in SD card", e);
                	result = R.id.read_sdcard_error;
    			} else if (e instanceof IOException) {
    				Log.e(TAG, "Read VMG file error", e);
    				result = R.id.read_sdcard_error;	            
    			} else if (e instanceof NullPointerException) {
    				Log.e(TAG, "No content in the file", e);
    				result = R.id.empty_storage;
    			} else {
    				Log.e(TAG, e.toString());
    				result = R.id.unknown_error;
    			}
	            continue;
    		}
            mDirPath = fileList[i].getParent();
            result = insertQNFileIntoPhone(action, fileList[i].getName());            
            if( R.id.success == result )  {
                mActualFileCounter++;
            }
        }
        
        Log.d(TAG, "importXXXFromFiles mActualFileCounter="+mActualFileCounter);
    	if(mActualFileCounter == 0){
    		return result;
    	}
    	return R.id.success;
    }
    
    private int insertQNFileIntoPhone(int action, String fileName){
    	int pos1 = 0;
        int pos2 = 0;
        int count = 0;
        int result = R.id.success;
        boolean isFinished = false;
        String sBuffer = "";
        String subject = null;
        String fromDir = mDirPath + "/" + Constants.QN_ATTACH;
        if ( -1==(pos1=mQNString.indexOf(QuickNoteBackupRestore.vnoteStartTag))){
          	return R.id.sdcard_file_contents_error;
        }
        // To send total number of each file to Activity progress bar 
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalQuickNoteCounter, Constants.QUICKNOTE_ACTION));
        while ( true ){
            subject = null;
            if (-1 == (pos2=mQNString.indexOf(QuickNoteBackupRestore.vnoteEndTag, pos1))) {
                isFinished = true;
                break;
            }
            if (null==(sBuffer=mQNString.substring(pos1, pos2+QuickNoteBackupRestore.vnoteEndTag.length()))) {
                result = R.id.sdcard_file_contents_error;
                isFinished = false;
                break;
            }
            if (mInterrupted)  {
                result = R.id.cancel;
                break;
            }
            isFinished = true;
            try {
                subject = mQNBackupRestore.restoreQuickNote(mContext, sBuffer, fromDir);
            } catch (VNoteException e) {
		       	result = R.id.copy_files_failed;
			} catch (JSONException e) {
		        result = R.id.sdcard_file_contents_error;
		        isFinished = false;
			}
            if (subject != null) {
                mActualQuickNoteCounter ++;
			}else{ 
		subject = "";
		}

            Log.d(TAG, "before substring subject:" + subject);
			if ( Constants.COMMON_DISPLAY_LEN < subject.length() ) {
				subject = subject.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
			}
            Log.d(TAG, "after substring subject:" + subject);

            // to show current count and person name in progress bar
            String mesgBody;
            if (action == Constants.RESTORE_ACTION) {
                mesgBody = mContext.getString(R.string.restoring, mContext.getString(R.string.quicknote)) 
                        + "\n\n" + subject;
            } else {
                mesgBody = mContext.getString(R.string.import3rding0, mContext.getString(R.string.quicknote), mCurrentFileCounter, mTotalFileCounter, fileName) 
                        + "\n\n" + subject;
            }
            mNotificationAgent.sendMsgWithoutNotification(action,
                    Message.obtain(null, R.id.under_progress, count, Constants.QUICKNOTE_ACTION, mesgBody));
            NotificationItem item = new NotificationItem();
            item.id = 1;
            item.totalCurrent = count;
            item.totalTotal = mTotalQuickNoteCounter;
            item.direction = action;
            if (item.direction == Constants.RESTORE_ACTION) {
                item.description = mContext.getString(R.string.restoring, 
                        mContext.getString(R.string.quicknote));
            } else {
                if (fileName != null) {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.quicknote), fileName);
                } else {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.quicknote), "");
                    Log.e(TAG, "Miss file name for import3rdParty action type");
                }
            }
            Log.e(TAG, "Description is " + item.description);
            mNotificationAgent.sendNotificationProgress(item);
            count++;
            if (count == mTotalQuickNoteCounter) {
                result = R.id.success;
                break;
            }
            pos1 = pos2+QuickNoteBackupRestore.vnoteEndTag.length()+1;
        }

        if (true==isFinished) {
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
        deleteOldBackupFile();//we do not care the result for SWITCHUITWOV-250 , because the case can not delete the empty dir.
        result = SdcardManager.checkSdcardInitCondition(mContext, mIsAltSdcard, mConstants.DEFAULT_FOLDER);
        if (result != R.id.success) {
			return result;
		}
        File handler;
        if (null == (handler=SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER, mIsAltSdcard))) {
            Log.e(TAG, "Get SD Card unknown error.");
            result = R.id.unknown_error;
            return result;
        }            
        result = backupQuickNote(handler);
        Log.v(TAG, "QuickNote backup complete.");
        return result;
    }
    
    private int backupQuickNote(File dirHandler) {
    	int result = R.id.success;
    	int resultCopy = R.id.success;
    	int readCount = 1;
    	String fileName = Constants.QUICKNOTE_FILE_NAME;
    	StringBuffer subject = new StringBuffer();
    	String subjectStr = "";
    	String serialize = null;
    	Cursor cursor = QuickNoteBackupRestore.queryQuickNote(mContext.getContentResolver());
    	if ((null == cursor) || (false == cursor.moveToFirst())) {
            if (null != cursor) cursor.close();
            return R.id.read_phone_db_error;
        }
    	mTotalQuickNoteCounter = cursor.getCount();
    	mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.message_length, mTotalQuickNoteCounter, Constants.QUICKNOTE_ACTION));
    	mQNAttach = SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER + "/" +Constants.QN_ATTACH, mIsAltSdcard);
		for (int i = 0; !mInterrupted && (i < mTotalQuickNoteCounter); cursor.moveToNext(), i++) {		
			try {
				serialize = mQNBackupRestore.backupQuickNote(cursor, subject);
				resultCopy = copyAttach(cursor);	
				if(resultCopy == R.id.unknown_error){
					continue;
				}
			} catch (JSONException e) {
				cursor.close();
				return R.id.sdcard_file_contents_error;
			}
			
			result = SdcardManager.saveInSdcard(mContext, dirHandler, fileName, serialize, mIsAltSdcard);
			if (result == R.id.success) {
				subjectStr = subject.toString();
				Log.d(TAG, "before substring subjectStr:" + subjectStr);
				if ( Constants.COMMON_DISPLAY_LEN < subjectStr.length() ) {
					subjectStr = subjectStr.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
				}
				Log.d(TAG, "after substring subjectStr:" + subjectStr);

				mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
		                Message.obtain(null, R.id.under_progress, readCount, Constants.QUICKNOTE_ACTION,
		                    mContext.getString(R.string.backuping, mContext.getString(R.string.quicknote))+"\n\n"+subjectStr));
				NotificationItem item = new NotificationItem();
	            item.id = 1;
	            item.totalCurrent = readCount;
	            item.totalTotal = mTotalQuickNoteCounter;
	            item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.quicknote));
	            item.direction = Constants.BACKUP_ACTION;
	            mNotificationAgent.sendNotificationProgress(item);
	            readCount++;
			} else {
                Log.v(TAG, "write SD card return error while backup QuickNote");
                break;
			}
		}
		mActualQuickNoteCounter = readCount - 1;
		cursor.close();
		if (mInterrupted) {
            result = R.id.cancel;
        }
		return result;
    }
    
    private int copyAttach(Cursor cursor) {
        Log.i(TAG, "copyAttach()");
    	int res = R.id.success;
    	String fileName = null;
    	Uri uri = mQNBackupRestore.getFileUri(cursor);
    	File file = new File(uri.getPath());
    	fileName = uri.getLastPathSegment();
    	if (!mQNBackupRestore.copyfile(file, new File(mQNAttach, fileName))) {
            Log.i(TAG, "copyAttach() unknown_error");
    		res = R.id.unknown_error;
		}
    	return res;
    }
   
    public boolean deleteOldBackupFile() {
        Log.i(TAG, "deleteOldBackupFile.");
    	boolean inner_file_res = true;
    	boolean outer_file_res = true;
    	boolean inner_folder_res = true;
    	boolean outer_folder_res = true;
    	File inner_file = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
    			                  , Constants.QUICKNOTE_FILE_NAME);
    	 // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 begin
    	if (inner_file != null && inner_file.exists() && !mIsAltSdcard) {
    		inner_file_res = inner_file.delete();
		}
    	File outer_file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                                  , Constants.QUICKNOTE_FILE_NAME);
    	if (outer_file != null && outer_file.exists() && mIsAltSdcard) {
    		outer_file_res = outer_file.delete();
		}
    	File inner_folder = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                , Constants.QN_ATTACH);
    	if (inner_folder != null && inner_folder.exists() && !mIsAltSdcard) {
    		inner_folder_res = SdcardManager.deleteFolder(inner_folder);
		}
    	File outer_folder = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                , Constants.QN_ATTACH);
    	if (outer_folder != null && outer_folder.exists() && mIsAltSdcard) {
    		outer_folder_res = SdcardManager.deleteFolder(outer_folder);
		}
    	 // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 end
        Log.i(TAG, "deleteOldBackupFile. outer_file_res="+outer_file_res+"  outer_folder_res="+outer_folder_res);
        Log.i(TAG, "deleteOldBackupFile. inner_file_res="+inner_file_res+"  inner_folder_res="+inner_folder_res);

    	if (!inner_file_res || !outer_file_res || !inner_folder_res || !outer_folder_res) {
			return false;
		}
		return true;
    }
 
    private int getQNNum(String fileString) {
    	int msgNum = 0;
    	if (fileString == null) {
			return msgNum;
		}
    	int pos = fileString.indexOf(QuickNoteBackupRestore.vnoteStartTag);
        while( -1 != (pos=fileString.indexOf(QuickNoteBackupRestore.vnoteEndTag, pos)) ) {
            pos += QuickNoteBackupRestore.vnoteEndTag.length();
            msgNum++;
        }
        return msgNum;
    }
    
	public boolean isInnerQNFileExist() {
		File innerQNFile = new File( mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.QUICKNOTE_FILE_NAME );
    	if (innerQNFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
	public boolean isOuterQNFileExist() {
		File innerQNFile = new File( mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.QUICKNOTE_FILE_NAME );
    	if (innerQNFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
	public void mediaScanFolder(File folder) {
		Intent intent = new Intent("com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER");
		Uri uri = Uri.fromFile(folder);
		intent.setData(uri);
		try {
			mContext.sendBroadcast(intent);
		} catch (android.content.ActivityNotFoundException ex) { ex.printStackTrace(); }
		}
	
}
