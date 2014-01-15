package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONException;

import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;

import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

public class BookmarkManager {
	
	private final static String BEGIN_EVENT = "BEGIN:VEVENT";
	private final static String END_EVENT = "END:VEVENT";
	private final static String BEGIN_CAL = "BEGIN:VCALENDAR";
	
	private final static String TAG = "Bookmark Manager";
	private final static String ITEM = "Bookmark";
	
	private Context mContext = null;
	private NotificationAgent mNotificationAgent = null;
	private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private int mTotalCounter = 0;
    private int mActualCounter = 0;
    private int mActualFileCounter = 0;
    private int mTotalFileCounter = 0;
    private int mCurrentFileCounter = 0;
    private int mNeednotRestoreDefaultBookmarkNum = 0;
    private boolean mIsAltSdcard;
	private String mFileString = null;
	private String mFileName = null;
	private String mDirPath = null;
	private boolean mIsFile = false;
    private Constants mConstants = null;
    private BookmarkBackupRestore mBookmarkBR;
	
	
	public BookmarkManager(Context context, NotificationAgent notification) {
		mContext = context;
		mNotificationAgent = notification;
		mConstants = new Constants(context);
    	mBookmarkBR = new BookmarkBackupRestore(context);
	}
	
	public boolean getIsWorking() {
		return mIsWorking;
	}
	
	public int getTotalFileCounter() {
	    return mTotalFileCounter;
	}
	
    public int getTotalCounter() {
        return mTotalCounter;
    }   

    public int getActualCounter() {
        return mActualCounter;
    }

    public int getActualFileCounter() {
    	return mActualFileCounter;
    }
    
    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }
    
    public int Backup() {
    	Log.v(TAG, "Enter Backup()");
        int result = R.id.success;
        if (!mInterrupted) {
			mIsWorking = true;
            result = BackupData();
			mIsWorking = false;
        } else {
			result = R.id.cancel;
		}
        return result;
    }
    
    public int Restore(int action, ArrayList<File> fileList) {
    	int res = R.id.success;
    	if (!mInterrupted) {
			mIsWorking = true;
            res = RestoreData(action, fileList);
			mIsWorking = false;
        } else {
            res = R.id.cancel;
        }
    	return res;
    }
    
    private int RestoreData(int action, ArrayList<File> fileList) {
    	int res = R.id.success;
    	File[] files = null;
    	File file = null;
    	if (fileList == null || fileList.size() == 0) {
			return R.id.empty_storage;
		}
    	if (SdcardManager.noSdcard(mContext)) {
    		return R.id.no_sdcard;
    	}
        if (action == Constants.RESTORE_ACTION) {
        	if (isInnerFileExist()) {
        		file = new File(mConstants.SDCARD_PREFIX + "/" + fileList.get(0).getPath());
			} else if (isOuterFileExist()) {
				file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + fileList.get(0).getPath());
			} else {
				return R.id.empty_storage;
			}
		}
    	try {
        	if (action == Constants.RESTORE_ACTION) {
        		mFileString = SdcardManager.getFileString(file, "UTF-8");
        		mTotalCounter = getNum(mFileString);
        		if (0 == mTotalCounter) {
        			return R.id.sdcard_file_contents_error;
        		}
        		mFileName = file.getName();
        		mDirPath = file.getParent();
        		mIsFile = true;
        		res = importFromFile(action, mFileString);
    		}else if (action == Constants.IMPORT3RD_ACTION) {
    			mIsFile = false;
    			mTotalCounter = fileList.size();
    			files = new File[mTotalCounter];
    			files = fileList.toArray(files);
    			res = importFromFiles(action, files);
    		}else {
    			Log.v(TAG, "Should not be here!");
    			res = R.id.unknown_error;
    		}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "out of memory", e);
			return R.id.out_of_memory_error;
		} catch (SQLiteException e) {
			Log.e(TAG, "Export " + ITEM + " DB error!", e);
			return R.id.write_phone_db_error;
		} catch (Exception e) {
            if (e instanceof FileNotFoundException) {
            	Log.e(TAG, "FileNotFoundException of an " + ITEM + " file reading in SD card", e);
                return R.id.read_sdcard_error;
			} else if (e instanceof IOException) {
				Log.e(TAG, "Read " + ITEM + " file error", e);
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
    
    private int importFromFile(int action, String fileString) {
    	int result = R.id.unknown_error;
    	if (0 == mTotalCounter) {
			return R.id.sdcard_file_contents_error;
		} else {
			if (mIsFile) {
	            mNotificationAgent.sendMsgWithoutNotification(action,
	                    Message.obtain(null, R.id.message_length, mTotalCounter, Constants.BOOKMARK_ACTION));
			}
            if (action == Constants.RESTORE_ACTION) {
                result = insertFileIntoPhone(action, null);
            } else {
                result = insertFileIntoPhone(action, mFileName);
            }
        }
    	return result;    	
    }
    
    private int importFromFiles(int action, File[] fileList) {
    	int result = R.id.success;
    	mTotalFileCounter = fileList.length;
        mCurrentFileCounter = 0;
        if (0 == mTotalFileCounter)  return R.id.empty_storage;
        
        for (int i=0; i<mTotalFileCounter && !mInterrupted; i++) {
            mCurrentFileCounter++;
            try {
            	mFileString = SdcardManager.getFileString(fileList[i], "UTF-8");
            	mTotalCounter = getNum(mFileString);
            }
            catch (NullPointerException e){
	            Log.e(TAG, "Wrong format of " + ITEM + " file");
	            result = R.id.sdcard_file_contents_error;
	            continue;
            } catch (OutOfMemoryError e) {
    			Log.e(TAG, "out of memory", e);
    			result = R.id.out_of_memory_error;
	            continue;
    		} catch (SQLiteException e) {
    			Log.e(TAG, "Export " + ITEM + " DB error!", e);
    			result = R.id.write_phone_db_error;
	            continue;
    		} catch (Exception e) {
                if (e instanceof FileNotFoundException) {
                	Log.e(TAG, "FileNotFoundException of an " + ITEM + " file reading in SD card", e);
                	result = R.id.read_sdcard_error;
    			} else if (e instanceof IOException) {
    				Log.e(TAG, "Read " + ITEM + " file error", e);
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
            result = insertFileIntoPhone(action, fileList[i].getName());
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
    
    private int insertFileIntoPhone(int action, String fileName){
    	Log.d(TAG, "Enter insertFileIntoPhone()");
        int result = R.id.success;
        boolean isFinished = false;
        String subject = null;
        int count = 0;
    	mBookmarkBR.initBookMarkTree(mFileString);
		mBookmarkBR.insertFolder();
        ArrayList<ContentValues> cvBookmarkList = mBookmarkBR.getBookmarkValues(false);
    	mNeednotRestoreDefaultBookmarkNum = mBookmarkBR.getNeednotRestoreDefaultBookmarksNum();
    	mActualCounter = mNeednotRestoreDefaultBookmarkNum;
    	// To send total number of each file to Activity progress bar 
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalCounter, Constants.BOOKMARK_ACTION));
        for (int i = 0; i < cvBookmarkList.size(); i++) {
            if (mInterrupted)  {
                result = R.id.cancel;
                break;
            }
            subject = mBookmarkBR.Restore(mContext, cvBookmarkList.get(i));
            if (subject == null || subject.equals("null") || subject.equals("")) {
            	subject = mContext.getString(R.string.bookmark);
            }
            if (subject != null) {
            	if (!mBookmarkBR.isBookmarkFolder(cvBookmarkList.get(i))) {
                	mActualCounter ++;
				}
                isFinished = true;
			} else {
				result = R.id.unknown_error;
				break;
			}
            String body;
            if (action == Constants.RESTORE_ACTION) {
            	body = mContext.getString(R.string.restoring, mContext.getString(R.string.bookmark)) 
                        + "\n\n" + subject;
            } else {
            	body = mContext.getString(R.string.import3rding0, mContext.getString(R.string.bookmark), mCurrentFileCounter, mTotalFileCounter, fileName) 
                        + "\n\n" + subject;
            }
            mNotificationAgent.sendMsgWithoutNotification(action,
                    Message.obtain(null, R.id.under_progress, count, Constants.BOOKMARK_ACTION, body));
            NotificationItem item = new NotificationItem();
            item.id = 1;
            item.totalCurrent = count;
            item.totalTotal = mTotalCounter;
            item.direction = action;
            if (item.direction == Constants.RESTORE_ACTION) {
                item.description = mContext.getString(R.string.restoring, 
                        mContext.getString(R.string.bookmark));
            } else {
                if (fileName != null) {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.bookmark), fileName);
                } else {
                    item.description = mContext.getString(R.string.import3rding, 
                            mContext.getString(R.string.bookmark), "");
                    Log.e(TAG, "Miss file name for import3rdParty action type");
                }
            }
            Log.e(TAG, "Description is " + item.description);
            mNotificationAgent.sendNotificationProgress(item);
            if (!mBookmarkBR.isBookmarkFolder(cvBookmarkList.get(i))) {
                count++;
            }
            if (mNeednotRestoreDefaultBookmarkNum + count == mTotalCounter) {
                result = R.id.success;
                break;
            }
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
    
    private int BackupData() {
    	Log.v(TAG, "Enter BackupData()");
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
        result = backupItem(handler);
        Log.v(TAG, ITEM + " backup complete.");
        return result;
    }
    
    private int backupItem(File dirHandler) {
    	Log.d(TAG, "Enter backupItem");
    	int result = R.id.success;
    	int readCount = 1;
    	int index = -1;
    	String fileName = Constants.BOOKMARK_FILE_NAME;
    	StringBuffer subject = new StringBuffer();
    	String serialize = null;
    	Cursor cursor = mBookmarkBR.queryBookmarkAndFolder(mContext);
    	mBookmarkBR.initAllFolder(mBookmarkBR.getAllFolder(cursor));
    	if ((null == cursor) || (false == cursor.moveToFirst())) {
            if (null != cursor) cursor.close();
            return R.id.read_phone_db_error;
        }
    	mTotalCounter = mBookmarkBR.queryNumOfBookmarkWithoutFolder();
    	mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.message_length, mTotalCounter, Constants.BOOKMARK_ACTION));
		for (int i = 0; !mInterrupted && (i < cursor.getCount()); cursor.moveToNext(), i++) {
	        index = cursor.getColumnIndex(BookmarkConstants.column_folder);
	        if (index != -1 && cursor.getInt(index) != 0) {
	        	continue;
			}
			mBookmarkBR.Backup(cursor, subject);
			mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
	                Message.obtain(null, R.id.under_progress, readCount, Constants.BOOKMARK_ACTION,
	                    mContext.getString(R.string.backuping, mContext.getString(R.string.bookmark))+"\n\n"+subject.toString()));
			NotificationItem item = new NotificationItem();
	        item.id = 1;
	        item.totalCurrent = readCount;
	        item.totalTotal = mTotalCounter;
	        item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.bookmark));
	        item.direction = Constants.BACKUP_ACTION;
	        mNotificationAgent.sendNotificationProgress(item);
	        readCount++;
		}
		serialize = mBookmarkBR.buildBackupString();
		result = SdcardManager.saveInSdcard(mContext, dirHandler, fileName, serialize, mIsAltSdcard);
		mActualCounter = readCount - 1;
		cursor.close();
		if (mInterrupted) {
            result = R.id.cancel;
        }
		return result;
    }
    
    public boolean deleteOldBackupFile() {
    	boolean inner_file_res = true;
    	boolean outer_file_res = true;
    	File inner_file = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
    			                  , Constants.BOOKMARK_FILE_NAME);
    	if (inner_file != null && inner_file.exists()) {
    		inner_file_res = inner_file.delete();
		}
    	File outer_file = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER
                                  , Constants.BOOKMARK_FILE_NAME);
    	if (outer_file != null && outer_file.exists()) {
    		outer_file_res = outer_file.delete();
		}
    	if (!inner_file_res || !outer_file_res) {
			return false;
		}
		return true;
    }
    
    private int getNum(String fileString) {
    	int num = 0;
    	if (fileString == null) {
			return num;
		}
    	int pos = fileString.indexOf(BookmarkConstants.BEGIN_MARK_BOOKMARK);
        while( -1 != (pos=fileString.indexOf(BookmarkConstants.END_MARK_BOOKMARK, pos)) ) {
            pos += BookmarkConstants.END_MARK_BOOKMARK.length();
            num++;
        }
        return num;
    }
    
	public boolean isInnerFileExist() {
		File innerCalFile = new File( mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.BOOKMARK_FILE_NAME );
    	if (innerCalFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
	public boolean isOuterFileExist() {
		File innerCalFile = new File( mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER 
                + "/" + Constants.BOOKMARK_FILE_NAME );
    	if (innerCalFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
    
}
