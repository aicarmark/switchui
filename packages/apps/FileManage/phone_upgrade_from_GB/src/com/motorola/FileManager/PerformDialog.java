package com.motorola.FileManager;

import java.io.File;
import java.io.InputStream;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PerformDialog extends Dialog implements View.OnClickListener {
    //Begin CQG478 liyun, IKDOMINO-2368, 2011-9-9
    public static final String PFM_STOP = "PFM_STOP";
    public static final String PFM_HASSKIPPED = "PFM_HASSKIPPED";
    public static final String PFM_RINTTONE = "PFM_RINTTONE";
    public static final String PFM_SAMEDISK = "PFM_SAMEDISK";
    public static final String PFM_FINISH = "PFM_FINISH";
    //End

    private static final int UPDATE_FILENAME = 1;
    private static final int PERFORM_FINISH = 2;
    private static final int HANDLE_ERROR = 3;
    private static final int OVERWRITE_CONFIRM = 4;
    private static final int COPY_DONE = 5;
    private static final int COPY_FAILED = 6;

    private static final int FAIL_DELETE_RINGTONE = 1;
    private static final int FAIL_MOVE_RINGTONE = 2;
    private static final int FAIL_DISK_FULL = 3;

    private Message mMessage = new Message();

    private DialogHandler mHandler;

    private TextView mText;
    private TextView mTitleText;
    private ImageView mTitleIcon;
    private Button mCancel;
    private Button mYes;
    private Button mNo;
    private CheckBox mCheckBox;
    private TextView mTextOverWrite;
    private LinearLayout mFrame1;
    private LinearLayout mFrame2;
    private LinearLayout mPanel1;
    private LinearLayout mPanel2;

    public String mDestFolder;

    public boolean mStop = false;
    private boolean mOver = false;
    private boolean mOverWriteAll = false;
    private boolean mOverWrite = false;
    public boolean mHasSkiped = false;
    public boolean mRingtone = false;
    public boolean mFinished = false;
    private boolean mSleep;
    public boolean isSameDisk = true;

    public boolean mNeedScanDisk = false;

    private String mFileName;
    private String mRingtoneFileName;
    public int mAction;

    private int mTitleRes;
    private int mIconRes;

    public Activity mActivity;

    private FileInfo[] mSrcList;

    public int mFailReason = 0;
    private CopyThread mCopyThread = null;
    private PowerManager.WakeLock mWakeLock = null;
    private PowerManager mPM = null;

    public PerformDialog(Activity activity) {
        super(activity);
        mHandler = new DialogHandler();
        mActivity = activity;
        mPM = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        if (mPM != null) {
            mWakeLock = mPM.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, "filemanager");
            mWakeLock.setReferenceCounted(false);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_deleting);
        getWindow().setFeatureInt(Window.FEATURE_NO_TITLE, 0);

        mText = (TextView) findViewById(R.id.file_name);
        mCancel = (Button) findViewById(R.id.btn_cancel);
        mCancel.setOnClickListener(this);

        mYes = (Button) findViewById(R.id.btn_yes);
        mYes.setOnClickListener(this);

        mNo = (Button) findViewById(R.id.btn_no);
        mNo.setOnClickListener(this);

        mTitleText = (TextView) findViewById(R.id.title_text);
        mTitleIcon = (ImageView) findViewById(R.id.title_icon);

        mCheckBox = (CheckBox) findViewById(R.id.cb_overwrite_all);
        mTextOverWrite = (TextView) findViewById(R.id.prompt_overwrite);

        mTitleText.setText(mTitleRes);
        mTitleIcon.setImageResource(mIconRes);


        mFrame1 = (LinearLayout) findViewById(R.id.frame1);
        mFrame2 = (LinearLayout) findViewById(R.id.frame2);
        mFrame2.setVisibility(View.GONE);

        mPanel1 = (LinearLayout) findViewById(R.id.buttonPanel1);
        mPanel2 = (LinearLayout) findViewById(R.id.buttonPanel2);
        mPanel2.setVisibility(View.GONE);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	return true;
    }

    public void onClick(View v) {
        synchronized (this) {
            if (mCancel == v) {
                setTitle(R.string.title_dlg_canceling);
                mStop = true;
                if ((mAction == Util.ACTION_COPY) || (mAction == Util.ACTION_MOVE && !isSameDisk)) {
                    if (mCopyThread != null) {
                        mCopyThread.setStop(mStop);
                    }
                mMessage = Message.obtain(mHandler, PERFORM_FINISH);
                mHandler.sendMessageDelayed(mMessage, 20);
                mWakeLock.release();
                }
    
            } else if (mYes == v) {
                mOverWrite = true;
                if (mCheckBox.isChecked()) {
                    mOverWriteAll = true;
                }
                mFrame2.setVisibility(View.GONE);
                mPanel2.setVisibility(View.GONE);

                mFrame1.setVisibility(View.VISIBLE);
                mPanel1.setVisibility(View.VISIBLE);

                // mThread.resume();
                mSleep = false;

            } else if (mNo == v) {
                mFrame2.setVisibility(View.GONE);
                mPanel2.setVisibility(View.GONE);

                mFrame1.setVisibility(View.VISIBLE);
                mPanel1.setVisibility(View.VISIBLE);

                mOverWrite = false;
                // mThread.resume();
                mSleep = false;
            }
        }
    }

    protected void onStart() {
        Thread thread = new ProgressThread(mActivity);
        thread.start();
    }

    public void setTitle(int title) {
        mTitleRes = title;
        if (mTitleText != null)
            mTitleText.setText(title);
    }

    public void setIcon(int icon) {
        mIconRes = icon;
        if (mTitleIcon != null)
            mTitleIcon.setImageResource(icon);
    }

    public void setAction(int action, FileInfo[] list, String destFolder) {
        mAction = action;
        mSrcList = list;
        mDestFolder = destFolder;
        mNeedScanDisk = false;
    }

    public void setFileName(String name) {
        synchronized (this) {
            mFileName = name;
        }

        mMessage = Message.obtain(mHandler, UPDATE_FILENAME);
        mHandler.sendMessageDelayed(mMessage, 20);
    }

    private boolean checkStop() {
        if (mStop) {
            mMessage = Message.obtain(mHandler, PERFORM_FINISH);
            mHandler.sendMessageDelayed(mMessage, 20);
        }
        return mStop;
    }

    private void perform() {
        mStop = false;
        mOver = false;
        mHasSkiped = false;
        mRingtone = false;
        mFailReason = 0;
        boolean sendIntentGallery3D = false;
        File childFile = null;
        Object syn=new Object();
        synchronized (syn) {
        	if (mAction == Util.ACTION_MOVE || mAction == Util.ACTION_COPY) {
        		isSameDisk = Util.isSameDisk(mSrcList[0].getRealPath(), mDestFolder);
        	}
        	if (mAction == Util.ACTION_MOVE && !isSameDisk) {
        		mWakeLock.acquire();
                mCopyThread =new CopyThread(mSrcList,mDestFolder,mHandler,true,this);
                mCopyThread.start();
        	}
        	else if (mAction == Util.ACTION_DELETE || mAction == Util.ACTION_MOVE || mAction == Util.ACTION_HIDEUNHIDE) {

            	boolean result = true;
                mWakeLock.acquire();

                for (int i = 0; i < mSrcList.length; i++) {
                    if (mStop)
                        break;
                    if (mAction == Util.ACTION_DELETE) {
                        // add for Gallery 3D
                        String childPath = null;
                        int child_mimeId = MimeType.MIME_NONE;
                        boolean childResult = false;
                        childPath = mSrcList[i].getRealPath();
                        childFile = new File(childPath);

                        if (childFile.isDirectory()) {
                            result = deleteFolderOrFile(mSrcList[i].getRealPath());
                        } else {

                            childResult = deleteFolderOrFile(mSrcList[i].getRealPath());

                            child_mimeId = Util.getMimeId(mActivity, childPath);

                            if (child_mimeId == MimeType.MIME_IMAGE
                                    || child_mimeId == MimeType.MIME_VIDEO) {

                                if (sendIntentGallery3D == false && childResult == true) {
                                    sendIntentGallery3D = true;
                                }
                            }

                        }

                    } 
                    else if (mAction == Util.ACTION_HIDEUNHIDE)
                    {	String childPath = null;
                    	childPath = mSrcList[i].getRealPath();
                    	childFile = new File(childPath);
                    	if (childFile.isDirectory())
                    		if (childFile.getName() == null){
                    			setFileName(childPath);
                    		} else {
                    			setFileName(childFile.getName());
                    		}
                    		if(mSrcList[i].isHidden()==true) 
                    		{                    		
                    		result = FileSystem.hideFolder(mSrcList[i]);
                    		}
                    		else
                    		{
                    		result = FileSystem.unhideFolder(mSrcList[i]);
                    		}
                    	                    	
                    }

		            else if (mAction == Util.ACTION_MOVE)
                        result = move(mSrcList[i].getRealPath(), mDestFolder);

                   
                    
                    if (mStop){
                        break;
                    }
                    if (mSrcList.length == 1 && !result) {
                        if ((mFailReason != FAIL_DELETE_RINGTONE)
                                && (mFailReason != FAIL_MOVE_RINGTONE)) {
                            mMessage = Message.obtain(mHandler, HANDLE_ERROR);
                            mHandler.sendMessageDelayed(mMessage, 20);
                            mWakeLock.release();
                            return;
                        }
                    }
                }
                if (result) {
                	//modify by amt_xulei for SWITCHUITWO-509 2013-1-11
                    //reason:Need scan folder after move/delete operator
               	    Log.d("","mediaScanFile sd card after move or delete");
                    String needScanPath = "";
                    if (mAction == Util.ACTION_DELETE){
                    	needScanPath = mSrcList[0].getRealPath();
                    	needScanPath = needScanPath.substring(0,needScanPath.lastIndexOf('/'));
                    	Util.updateMediaProvider(mActivity,needScanPath);
                    }
                    else if (mAction == Util.ACTION_MOVE){
                    	needScanPath = mSrcList[0].getRealPath();
                    	needScanPath = needScanPath.substring(0,needScanPath.lastIndexOf('/'));
                    	Util.updateMediaProvider(mActivity,needScanPath);
                    	Util.updateMediaProvider(mActivity,mDestFolder);
                    }
                    //end modify
                }
                mWakeLock.release();
                
                if ((mFailReason != FAIL_DELETE_RINGTONE) && (mFailReason != FAIL_MOVE_RINGTONE)) {
                    mMessage = Message.obtain(mHandler, PERFORM_FINISH);
                    mHandler.sendMessageDelayed(mMessage, 20);

                } else {
                    mRingtone = true;
                    mMessage = Message.obtain(mHandler, HANDLE_ERROR);
                    mHandler.sendMessageDelayed(mMessage, 20);
                }

                if (sendIntentGallery3D) {
                    if (childFile != null) {
                        Log.v("FileManager", "Perform() send intent to Gallery 3D path="
                                + childFile.getParent());
                        Gallery3DScanFolder(childFile.getParentFile());
                    }
                }
              

            } else if (mAction == Util.ACTION_COPY) {
                mWakeLock.acquire();
                mCopyThread =new CopyThread(mSrcList,mDestFolder,mHandler,false,this);
                mCopyThread.start();
            }
        }
    }

    public boolean deleteFolderOrFile(String path) {

        if (checkStop())
            return false;
        int mimeId = MimeType.MIME_NONE;
        try {
            File file = new File(path);

            setFileName(file != null ? file.getName() : path);

            //Begin CQG478 liyun, IKDOMINO-3366, delete a read only file 2011-10-24
            //if (file == null || !file.canWrite())
            if (file == null)
            //End
                return false;

            /*if (file.isFile()) {
                // check if a file is a audio file that has been setted as
                // ringtone.
                mimeId = Util.getMimeId(mActivity, path);
                if ((mimeId == MimeType.MIME_AUDIO) && Util.isRingtoneFile(mActivity, path)) {
                    mFailReason = FAIL_DELETE_RINGTONE;
                    mRingtoneFileName = file.getName();
                    return false;
                }
            }*/

            if (file.isDirectory()) {
                boolean deleteFolder = true;
                boolean sendIntentGallery3D = false;
                String childPath = null;
                File childFile = null;
                int child_mimeId = MimeType.MIME_NONE;
                boolean childResult = false;
                
                File[] children = Util.getFileList(file, true);

                if (children != null && children.length > 0) {
                    for (int i = 0; i < children.length; i++) {
                        // changed for gallery 3D
                        childPath = children[i].getAbsolutePath();
                        childFile = new File(childPath);

                        if (childFile.isDirectory()) {

                            if (!deleteFolderOrFile(children[i].getAbsolutePath())
                                    && (mFailReason != FAIL_DELETE_RINGTONE))
                                return false;
                            else
                                deleteFolder = false;

                        } else {

                            childResult = deleteFolderOrFile(children[i].getAbsolutePath());

                            child_mimeId = Util.getMimeId(mActivity, childPath);

                            if (child_mimeId == MimeType.MIME_IMAGE
                                    || child_mimeId == MimeType.MIME_VIDEO) {

                                if (sendIntentGallery3D == false && childResult == true) {
                                    sendIntentGallery3D = true;
                                }

                            } else {

                                if (!childResult && (mFailReason != FAIL_DELETE_RINGTONE))
                                    return false;
                                else
                                    deleteFolder = false;
                            }
                        }
                    }
                    // send the intent to Gallery 3D
                    if (sendIntentGallery3D) {
                        Log.v("FileManager", "deleteFolderOrFile() send intent to Gallery 3D path="
                                + file.getAbsolutePath());
                        Gallery3DScanFolder(childFile.getParentFile());
                    }

                    if (deleteFolder)
                        return file.delete();
                }
            }

            boolean result = file.delete();

          /*  if (result) {

                if ((mimeId == MimeType.MIME_IMAGE || mimeId == MimeType.MIME_AUDIO || mimeId == MimeType.MIME_VIDEO)) {
                    Util.deleteFromMediaDataBase(mActivity, mimeId, path);
                }

            }*/
            return result;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean move(String srcPath, String destFolder) {
        if (checkStop())
            return false;

        int mimeId = MimeType.MIME_NONE;

        try {
            File srcFile = new File(srcPath);

            setFileName(srcFile != null ? srcFile.getName() : srcPath);
            //Begin CQG478 liyun, IKDOMINO-3366, delete a read only file 2011-10-24
            //if (srcFile == null || !srcFile.exists() || !srcFile.canWrite())
            if (srcFile == null || !srcFile.exists())
            //End            
                return false;

            /*if (srcFile.isFile()) {

                mimeId = Util.getMimeId(mActivity, srcPath);
                if ((mimeId == MimeType.MIME_AUDIO) && Util.isRingtoneFile(mActivity, srcPath)) {
                    mFailReason = FAIL_MOVE_RINGTONE;
                    mRingtoneFileName = srcFile.getName();
                    return false;
                }
            }*/

            File destFile = new File(destFolder);
            if (!destFile.exists()) {
            	if (!destFile.mkdirs()) {
                    return false;
            	}
            }
            if (!destFile.isDirectory())
                return false;
            
            String destPath = destFolder + "/" + srcFile.getName();

            if (srcFile.isDirectory()) {
                File[] children = Util.getFileList(srcFile, true);
                if (children != null && children.length > 0) {
                    for (int i = 0; i < children.length; i++) {
                        if (!move(children[i].getAbsolutePath(), destPath)
                                && (mFailReason != FAIL_MOVE_RINGTONE))
                            return false;
                    }
                    if (mFailReason != FAIL_MOVE_RINGTONE)
                        return srcFile.delete();
                    else
                        return false;
                } else {
                    destFile = new File(destPath);
                    if (destFile.exists() && destFile.isDirectory()) {
                        return srcFile.delete();
                    } else {
                        return srcFile.renameTo(destFile);
                    }
                }
            } else {
                destFile = new File(destPath);
                if (!destFile.exists() || destFile.exists() && confirmOverwrite(destFile)) {

                    boolean result = srcFile.renameTo(destFile);

                   /* if (result) {

                        if (mimeId == MimeType.MIME_IMAGE || mimeId == MimeType.MIME_AUDIO
                                || mimeId == MimeType.MIME_VIDEO) {
                            Util.deleteFromMediaDataBase(mActivity, mimeId, srcPath);
                            Util.mediaScanFile(mActivity, destFile);
                        }
                    }*/

                    return result;
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }



    private boolean confirmOverwrite(File file) {
        if (mOverWriteAll) {
            return true;
        }

        mOverWrite = false;

        mMessage = Message.obtain(mHandler, OVERWRITE_CONFIRM);
        mHandler.sendMessageDelayed(mMessage, 0);

        try {
            // mThread.suspend();
            mSleep = true;
            while (mSleep) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            return false;
        }

        if (!mOverWrite)
            mHasSkiped = true;

        return mOverWrite;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mSleep) {
                onClick(mNo);
            } else {
                onClick(mCancel);
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
        	return true;        	
        }

        return super.onKeyDown(keyCode, event);
    }

    private class DialogHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case OVERWRITE_CONFIRM:
                mFrame1.setVisibility(View.GONE);
                mPanel1.setVisibility(View.GONE);

                mFrame2.setVisibility(View.VISIBLE);
                mPanel2.setVisibility(View.VISIBLE);

                mCheckBox.setChecked(false);
                mTextOverWrite.setText(mActivity.getString(R.string.confirm_overwrite, mFileName));
                break;

            case UPDATE_FILENAME:
                if (mFileName != null) {
                    mText.setText(mFileName);
                }
                break;

            case PERFORM_FINISH:
                if (mOver == false) {
                    dismiss();
                    mOver = true;
                    mFinished = true;
                    mOverWriteAll = false;
                }
                break;

            case HANDLE_ERROR:
                if (mOver == false) {
                    String str = null;

                    if (mFailReason != 0) {
                        if (mFailReason == FAIL_DELETE_RINGTONE)
                            str = mActivity.getString(R.string.fail_delete_ringtone,
                                    mRingtoneFileName);
                        else if (mFailReason == FAIL_MOVE_RINGTONE)
                            str = mActivity.getString(R.string.fail_move_ringtone,
                                    mRingtoneFileName);
                        else if (mFailReason == FAIL_DISK_FULL)
                            str = mActivity.getString(R.string.fail_disk_full, mFileName);

                    } else {
                        if (mAction == Util.ACTION_DELETE)
                            str = mActivity.getString(R.string.fail_delete, mFileName);
                        else if (mAction == Util.ACTION_MOVE)
                            str = mActivity.getString(R.string.fail_move_file, mFileName);
                        else if (mAction == Util.ACTION_COPY)
                            str = mActivity.getString(R.string.fail_copy_file, mFileName);
                    }

                    if (str != null) {
                        Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
                    }

                    //if (mFailReason == FAIL_DELETE_RINGTONE)
                    dismiss();
                    //else
                    //    cancel();
                    mOver = true;
                    mOverWriteAll = false;
                    mFailReason = 0;
                }
                break;

            case COPY_DONE:
            	//modify by amt_xulei for SWITCHUITWO-509 2013-1-11
                //reason:Need scan folder after move/copy operator
            	if (mSrcList == null){
            		return;
            	}
            	if (mAction == Util.ACTION_MOVE){
            		Log.d("","mediaScanFile from move a file to diferent root");
            		String needScanPath = mSrcList[0].getRealPath();
                	needScanPath = needScanPath.substring(0,needScanPath.lastIndexOf('/'));
                	Util.updateMediaProvider(mActivity,needScanPath);
            		Util.updateMediaProvider(mActivity, mDestFolder);
            	}
            	else{
            		if(mSrcList.length == 1 && !mSrcList[0].isDirectory()){
            			Log.d("","mediaScanFile from cp a file");
            			String oldPath = mSrcList[0].getRealPath();
            			String filePath = mDestFolder + oldPath.substring(oldPath.lastIndexOf('/'));
            			File file = new File(filePath);
            			Util.mediaScanFile(mActivity, file);
            		}
            		else{
            			Log.d("","mediaScanFile from cp some files or a folder");
            			Util.updateMediaProvider(mActivity,mDestFolder);
            		}
            	}
            	//end modify
                mMessage = Message.obtain(mHandler, PERFORM_FINISH);
                mHandler.sendMessageDelayed(mMessage, 20);
                mWakeLock.release();
                break;

            case COPY_FAILED:
                mMessage = Message.obtain(mHandler, HANDLE_ERROR);
                mHandler.sendMessageDelayed(mMessage, 20);
                mWakeLock.release();
                break;
            }
        }
    }

    private class ProgressThread extends Thread {

        public ProgressThread(Context context) {
            super("ProgressDialog");
        }

        public void run() {
            perform();
        }
    }

    private void Gallery3DScanFolder(File file) {
        Intent intent = new Intent("com.motorola.action.REFRESH_MEDIA_SETS");
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        try {
            mActivity.sendBroadcast(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public class CopyThread extends Thread {

        private File srcFile;
        private FileInfo[] srcList;
        private String destFolder;
        private Handler handler;
        private PerformDialog activity;
        private boolean stop = false;
        private boolean deleted = false;

        public CopyThread(FileInfo[] pSrcList, String pDestFolder, Handler pHandler,boolean deletedAfterCopy,PerformDialog pActivity) {
            srcList = pSrcList;
            destFolder = pDestFolder;
            handler = pHandler;
            activity=pActivity;
            deleted = deletedAfterCopy;
        }

        public void setStop(boolean pStop) {
            stop = pStop;
        }

        public void run() {
            
            boolean result=false;

            for (int i = 0; i < srcList.length; i++) {
                if (stop)
                    break;

                result = copy(srcList[i].getRealPath(), destFolder);
                if (result && deleted && srcList[i].isDirectory()){
                	result = deleteFolderOrFile(srcList[i].getRealPath());
                }

                if (stop)
                    break;

                if (! stop && !result ) {
                    Message message = Message.obtain(handler, COPY_FAILED);
                    handler.sendMessageDelayed(message, 0);
                    break;
                }
            }
            
            
            if (!stop && result){   
               Message message = Message.obtain(handler, COPY_DONE);
               handler.sendMessageDelayed(message, 0);
            }
                            
        }
        
        
        private boolean copy(String srcPath, String destFolder) {

            try {
                File srcFile = new File(srcPath);
                if (srcFile == null || !srcFile.exists()) {
                	activity.setFileName(srcPath);
                	Log.d("","w23001- src file is null");
                    return false;
                }
                activity.setFileName(srcFile.getName());

                File destFile = new File(destFolder);
                if (!destFile.exists()) {
                	Log.d("","w23001- dest file is not exist");
                	if (!destFile.mkdirs()) {
                        return false;
                	}
                }
                if (!destFile.isDirectory()) {
                	Log.d("","w23001- dest file is not dir");
                    return false;
                }
                String destPath = destFolder + "/" + srcFile.getName();

                if (srcFile.isDirectory()) {
                    File[] children = Util.getFileList(srcFile, true);
                     
                    if (children == null || children.length == 0) {
                    	if (mDestFolder.contains(FileManager.getInternalSd(mActivity))){
                    		if (Util.getFreeDiskSpace(Util.ID_LOCAL, mActivity) < srcFile.length() + 1000) {
                    			activity.mFailReason = FAIL_DISK_FULL;
                    			Log.d("","w23001- internal storage is full");
                    			return false;
                    		}
                    	} else {
                    		if (Util.getFreeDiskSpace(Util.ID_EXTSDCARD, mActivity) < srcFile.length() + 1000) {
                    			activity.mFailReason = FAIL_DISK_FULL;
                    			Log.d("","w23001- external storage is full");
                    			return false;
                    		}
                    	}
                        destFile = new File(destPath);
                        if (!(destFile.exists() && destFile.isDirectory())) {
                            boolean res = destFile.mkdirs();
                            Log.d("","w23001- mkdir fail");
                            return res;
                        }
                    } else {
                        for (int i = 0; i < children.length; i++) {
                            if (!copy(children[i].getAbsolutePath(), destPath))
                                return false;
                        }
                    }
                    return true;
                } else {

                	if (mDestFolder.contains(FileManager.getInternalSd(mActivity))){
                		if (Util.getFreeDiskSpace(Util.ID_LOCAL, mActivity) < srcFile.length() + 1000) {
                			activity.mFailReason = FAIL_DISK_FULL;
                			Log.d("","w23001- internal storage is full");
                			return false;
                		}
                	} else {
                		if (Util.getFreeDiskSpace(Util.ID_EXTSDCARD, mActivity) < srcFile.length() + 1000) {
                			activity.mFailReason = FAIL_DISK_FULL;
                			Log.d("","w23001- external storage is full");
                			return false;
                		}
                	}

                    destFile = new File(destPath);
                    if (!destFile.exists() || destFile.exists() && activity.confirmOverwrite(destFile)) {

                    	/*if (destFile.exists()) {
                            int mimeId = MimeType.MIME_NONE;
                            mimeId = Util.getMimeId(activity.mActivity, destPath);
                            if ((mimeId == MimeType.MIME_IMAGE || mimeId == MimeType.MIME_AUDIO || mimeId == MimeType.MIME_VIDEO)) {
                                Util.deleteFromMediaDataBase(activity.mActivity, mimeId, destPath);
                            }

                        }*/

                         int bytesum = 0;
                         int byteread = 0;
                         InputStream inStream = null;
                         java.io.FileOutputStream fs = null;
                                            
                         try {
                                                
                           inStream = new java.io.FileInputStream(srcFile);
                           fs = new java.io.FileOutputStream(destPath);
                           //Begin CQG478 liyun, IKDOMINO-4646, copy file slowly 2011-11-25
                           byte[] buffer = new byte[1024 * 32];
                           //End


                           while ((byteread = inStream.read(buffer)) != -1 &&
                             !stop) {
                              bytesum += byteread;
                              fs.write(buffer, 0, byteread);
                              sleep(2);
                            }
                            fs.close();
                            fs = null;
                            inStream.close();
                            inStream = null;
                            if (stop) {
                               destFile.delete();
                               return false;
                            }
                         } catch (Exception e) {
                        	Log.d("","w23001- catch  Exception");
                            e.printStackTrace();
                            if (fs != null) {
                               fs.close();
                            }
                            destFile.delete();
                            return false;
                         } finally {
                               if (inStream != null)
                               inStream.close();
                         }
                         if (deleted){
                        	 return srcFile.delete();
                         }
                    }

                    /*if (Util.isMediaFile(destPath)) {
                        Util.mediaScanFile(activity.mActivity, destFile);
                    }*/

                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    @Override
    public void cancel(){
    	super.cancel();
    	mStop = true;
    	if ((mAction == Util.ACTION_COPY) || (mAction == Util.ACTION_MOVE && !isSameDisk)) {
            if (mCopyThread != null) {
                mCopyThread.setStop(mStop);
            }
    	}
    }
    
}
