package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONException;

import com.motorola.calendar.share.vcalendar.VCalUtils;

import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

public class CalendarManager {
	
	private final static String TAG = "Calendar Manager";
	private final static String BEGIN_EVENT = "BEGIN:VEVENT";
	private final static String END_EVENT = "END:VEVENT";
	private final static String BEGIN_CAL = "BEGIN:VCALENDAR";
	private final static String END_CAL = "END:VCALENDAR";
	
	private Context mContext = null;
	private NotificationAgent mNotificationAgent = null;
	private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private int mTotalCalCounter = 0;
    private int mActualCalCounter = 0;
    private int mActualFileCounter = 0;
    private int mTotalFileCounter = 0;
    private int mCurrentFileCounter = 0;
    private boolean mIsAltSdcard;
	private String mCalString = null;
	private String mFileName = null;
	private String mDirPath = null;
	private boolean mIsFile = false;
	private StringBuilder mSpliceData = null;
    private Constants mConstants = null;
	
	
	public CalendarManager(Context context, NotificationAgent notification) {
		mContext = context;
		mNotificationAgent = notification;
		mConstants = new Constants(context);
	}
	
	public boolean getIsWorking() {
		return mIsWorking;
	}
	
	public int getTotalFileCounter() {
	    return mTotalFileCounter;
	}
	
    public int getTotalCounter() {
        return mTotalCalCounter;
    }   

    public int getActualCounter() {
        return mActualCalCounter;
    }

    public int getActualFileCounter() {
    	return mActualFileCounter;
    }
    
    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }
    
    public int BackupCalendar() {
    	Log.v(TAG, "Enter BackupCalendar()");
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
    
    public int RestoreCalendar(int action, ArrayList<File> file_list) {
    	int res = R.id.success;
    	if (!mInterrupted) {
			mIsWorking = true;
            res = Restore(action, file_list);
			mIsWorking = false;
        } else {
            res = R.id.cancel;
        }
    	return res;
    }
    
    private int Restore(int action, ArrayList<File> fileList) {
    	int res = R.id.success;
    	File file = null;
    	File[] files = null;
    	if (!SdcardManager.isSdcardMounted(mContext, true) && !SdcardManager.isSdcardMounted(mContext, false)) {
    		return R.id.no_sdcard;
    	}
        if (action == Constants.RESTORE_ACTION) {
        	 if(isInnerCalFileExist() && !isOuterCalFileExist()){
        		file = new File(mConstants.SDCARD_PREFIX + "/" + fileList.get(0).getPath());
        	}
        	 else if ((!isInnerCalFileExist() && isOuterCalFileExist()) || (isInnerCalFileExist() && isOuterCalFileExist())){
        		file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + fileList.get(0).getPath());	
        	}
        	
        	else{
        		return R.id.empty_storage;
        	}
		}
        if (fileList == null || fileList.size() == 0) {
        	return R.id.empty_storage;
        }
    	try {
        	if (action == Constants.RESTORE_ACTION) {
        		mCalString = SdcardManager.getFileString(file, "UTF-8");
        		Log.v("Cal String", mCalString);
        		mTotalCalCounter = getCalNum(mCalString);
        		if (0 == mTotalCalCounter) {
        			return R.id.sdcard_file_contents_error;
        		}
        		mFileName = file.getName();
        		mDirPath = file.getParent();
        		mIsFile = true;
        		res = importCalFromFile(action, mCalString);
    		}else if (action == Constants.IMPORT3RD_ACTION) {
    			mIsFile = false;
    			mTotalCalCounter = fileList.size();
    			files = new File[fileList.size()];
    			files = fileList.toArray(files);
    			res = importCalFromFiles(action, files);
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
    	return res;
    }
    
    private int importCalFromFile(int action, String fileString) {
    	int result = R.id.unknown_error;
    	if (0 == mTotalCalCounter) {
			return R.id.sdcard_file_contents_error;
		} else {
            // To send total Calendar length to Activity progress bar
			if (mIsFile) {
	            mNotificationAgent.sendMsgWithoutNotification(action,
	                    Message.obtain(null, R.id.message_length, mTotalCalCounter, Constants.CALENDAR_ACTION));
			}
            if (action == Constants.RESTORE_ACTION) {
                result = insertCalFileIntoPhone(action, 1, null);
            } else {
                result = insertCalFileIntoPhone(action, 1, mFileName);
            }
        }
    	return result;    	
    }
    
    private int importCalFromFiles(int action, File[] fileList) {
    	int result = R.id.success;
        mTotalFileCounter = fileList.length;
        mCurrentFileCounter = 0;
        
        for (int i=0; i<mTotalCalCounter && !mInterrupted; i++) {
            mCurrentFileCounter++;
            try {
            	mCalString = SdcardManager.getFileString(fileList[i], "UTF-8");
            	mTotalCalCounter = getCalNum(mCalString);
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
    			Log.e(TAG, "Export SMS DB error!", e);
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
            result = insertCalFileIntoPhone(action, i, fileList[i].getName());
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
    
    private int insertCalFileIntoPhone(int action,int count, String fileName){
    	Log.d(TAG, "Enter insertCalFileIntoPhone()");
        int pos_header_begin = mCalString.indexOf(BEGIN_CAL);
        int pos_event_begin = mCalString.indexOf(BEGIN_EVENT);
        int pos_event_end = mCalString.indexOf(END_EVENT);
        int result = R.id.success;
        boolean isFinished = false;
        String event = "";
        String header = "";
        String subject = null;
        VCalUtils vc = new VCalUtils(mContext);
        if ( -1 == pos_header_begin || -1 == pos_event_begin){
          	return R.id.sdcard_file_contents_error;
        }
        header = mCalString.substring(pos_header_begin, pos_event_begin);
        // To send total number of each file to Activity progress bar 
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalCalCounter, Constants.CALENDAR_ACTION));
        while ( true ){
            if (-1 == pos_event_begin || -1 == pos_event_end) {
            	isFinished = true;
                break;
            }
            if (null==(event=mCalString.substring(pos_event_begin, pos_event_end + END_EVENT.length()))) {
                result = R.id.sdcard_file_contents_error;
                isFinished = false;
                break;
            }
            if (mInterrupted)  {
                result = R.id.cancel;
                break;
            }
            subject = vc.InsertICal(mContext, header + event);           
            if (subject == null || subject.equals("")) {
            	subject = mContext.getString(R.string.calendar);
            }
            if (subject != null) {
            	mActualCalCounter ++;
                isFinished = true;
			} else {
				result = R.id.unknown_error;
				break;
			}

			Log.d(TAG, "before substring subject:" + subject);
			if ( Constants.COMMON_DISPLAY_LEN < subject.length() ) {
				subject = subject.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
			}
			Log.d(TAG, "after substring subject:" + subject);

            String mesgBody;
            if (action == Constants.RESTORE_ACTION) {
                mesgBody = mContext.getString(R.string.restoring, mContext.getString(R.string.calendar)) 
                        + "\n\n" + subject;
            } else {
                mesgBody = mContext.getString(R.string.import3rding0, mContext.getString(R.string.calendar), mCurrentFileCounter, mTotalFileCounter, fileName) 
                        + "\n\n" + subject;
            }
            
            mNotificationAgent.sendMsgWithoutNotification(action,
                    Message.obtain(null, R.id.under_progress, count, Constants.CALENDAR_ACTION, mesgBody));
            NotificationItem item = new NotificationItem();
            item.id = 1;
            item.totalCurrent = count;
            item.totalTotal = mTotalCalCounter;
            item.direction = action;
            if (item.direction == Constants.RESTORE_ACTION) {
                item.description = mContext.getString(R.string.restoring, 
                        mContext.getString(R.string.calendar));
            } else {
                if (fileName != null) {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.calendar), fileName);
                } else {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.calendar), "");
                    Log.e(TAG, "Miss file name for import3rdParty action type");
                }
            }
            Log.e(TAG, "Description is " + item.description);
            mNotificationAgent.sendNotificationProgress(item);
            	count++;
            if (count - 1 == mTotalCalCounter) {
                result = R.id.success;
                break;
            }
            pos_event_begin = pos_event_end + END_EVENT.length() + 1;
            pos_event_end = mCalString.indexOf(END_EVENT, pos_event_begin);
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
    	if (!deleteOldBackupFile()) return R.id.unknown_error;
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
        result = backupCalendar(handler);
        Log.v(TAG, "Calendar backup complete.");
        return result;
    }
    
    private int backupCalendar(File dirHandler) {
    	int result = R.id.success;
    	int readCount = 1;
    	String fileName = Constants.CALENDAR_FILE_NAME;
    	StringBuffer subject = new StringBuffer();
    	String subjectStr = "";
    	String serialize = null;
    	VCalUtils vc = new VCalUtils(mContext);
    	Cursor cursor = vc.queryCal(mContext);
    	if ((null == cursor) || (false == cursor.moveToFirst())) {
            if (null != cursor) cursor.close();
            return R.id.read_phone_db_error;
        }
    	mTotalCalCounter = cursor.getCount();
    	mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.message_length, mTotalCalCounter, Constants.CALENDAR_ACTION));
		for (int i = 0; !mInterrupted && (i < mTotalCalCounter); cursor.moveToNext(), i++) {
			serialize = vc.getICal(mContext, cursor, subject);
			spliceCalData(serialize);
			subjectStr = subject.toString();
			Log.d(TAG, "before substring subjectStr:" + subjectStr);
			if ( Constants.COMMON_DISPLAY_LEN < subjectStr.length() ) {
				subjectStr = subjectStr.substring(0, Constants.COMMON_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
			}
			Log.d(TAG, "after substring subjectStr:" + subjectStr);

			mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
	                Message.obtain(null, R.id.under_progress, readCount, Constants.CALENDAR_ACTION,
	                    mContext.getString(R.string.backuping, mContext.getString(R.string.calendar))+"\n\n"+subjectStr));
			NotificationItem item = new NotificationItem();
	        item.id = 1;
	        item.totalCurrent = readCount;
	        item.totalTotal = mTotalCalCounter;
	        item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.calendar));
	        item.direction = Constants.BACKUP_ACTION;
	        mNotificationAgent.sendNotificationProgress(item);
	        readCount++;
		}
		
		mSpliceData.append("\r\n").append(END_CAL);
		
		result = SdcardManager.saveInSdcard(mContext, dirHandler, fileName, mSpliceData.toString(), mIsAltSdcard);
		mSpliceData = null;
		mActualCalCounter = readCount - 1;
		cursor.close();
		if (mInterrupted) {
            result = R.id.cancel;
        }
		return result;
    }
    
    private void spliceCalData(String data) {    	
    	 
    	if(mSpliceData == null) {
    		mSpliceData = new StringBuilder();
    		mSpliceData.append(data.substring(0,data.indexOf(END_CAL)-1));
    	} else {
    		
    		String tmpString = data.substring(data.indexOf(BEGIN_EVENT),data.indexOf(END_CAL)-1);
        	mSpliceData.append(tmpString);
        	
        	/*
    		tmpString = data.substring(data.indexOf(BEGIN_EVENT));
			mSpliceData.append(tmpString);
			*/
		}
    }
    
    public boolean deleteOldBackupFile() {
    	boolean inner_file_res = true;
    	boolean outer_file_res = true;
    	File inner_file = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
    			                  , Constants.CALENDAR_FILE_NAME);
    	  // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 begin
    	if (inner_file != null && inner_file.exists() && !mIsAltSdcard) {
    		inner_file_res = inner_file.delete();
		}
    	File outer_file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                                  , Constants.CALENDAR_FILE_NAME);
    	if (outer_file != null && outer_file.exists() && mIsAltSdcard) {
    		outer_file_res = outer_file.delete();
		}
    	 // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 end
    	if (!inner_file_res || !outer_file_res) {
			return false;
		}
		return true;
    }
    
    private int getCalNum(String fileString) {
    	int msgNum = 0;
    	if (fileString == null) {
			return msgNum;
		}
    	int pos = fileString.indexOf(BEGIN_EVENT);
        while( -1 != (pos=fileString.indexOf(END_EVENT, pos)) ) {
            pos += END_EVENT.length();
            msgNum++;
        }
        return msgNum;
    }
    
	public boolean isInnerCalFileExist() {
		File innerCalFile = new File( mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.CALENDAR_FILE_NAME );
		Log.d("jiayy","innerCalFile = "+innerCalFile);
    	if (innerCalFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
	public boolean isOuterCalFileExist() {
		File innerCalFile = new File( mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.CALENDAR_FILE_NAME );
    	if (innerCalFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
}
