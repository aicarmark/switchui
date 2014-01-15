package com.motorola.sdcardbackuprestore;

import java.io.File;

import android.net.Uri;
import android.os.FileUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


import android.R.integer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.widget.RemoteViews;

public class BackupRestoreService extends Service implements Runnable {

    private static final String TAG = "BackupRestore Service";

    public static int mBackupDest = -1;
    private static BackupRestoreService mInstance = null;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private boolean mIsProcessing = false;
    private boolean mContactsChecked = false;
    private boolean mSmsChecked = false;
    private boolean mQuickNoteChecked = false;
    private boolean mCalendarChecked = false;
    private boolean mAppChecked = false;
    private boolean mBookmarkChecked = false;
    protected boolean mFileChecked = false;
    protected boolean mFolderChecked = false;
    private String[] checkedNames = null;
    private String mAccountName = null;
    private String mAccountType = null;
    private ArrayList<String> mSelectFileList;
    private Uri[] mUriList = null;
    private ArrayList<Uri> mUriArrayList = null;
    private boolean mIsExportSpeContacts = false;
    private ArrayList<String> GAccount = new ArrayList<String>();
    private ArrayList<String> EAccount = new ArrayList<String>();
    private boolean mLocalChecked = false;
    private boolean mCardChecked = false;
    private boolean mCCardChecked = false;
    private boolean mGCardChecked = false;
    private int mDelOption = Constants.DEL_UNSET;
    private int mAction = Constants.NO_ACTION;
    private Backup mBackupActivity = null;
    private Restore mRestoreActivity = null;
    private Import3rd mImport3rdActivity = null;
    private Backup2 mBackup2Activity = null;
    private Restore2 mRestore2Activity = null;
    private Import3rd2 mImport3rd2Activity = null;
    private ExportByAccount2 mExportByAccount2Activity = null;
    private ExportByAccount mExportByAccountActivity = null;
    private NotificationResultActivity mNotificationResultActivity = null;
    private SMSManager mSmsManager;
    private ContactsManager mContactsManager;
    private QuickNoteManager mQuickNoteManager;
    private CalendarManager mCalendarManager;
    private AppManager mAppManager;
    private BookmarkManager mBookmarkManager;
    private Context mContext;
    private NotificationAgent mNotificationAgent;
    private boolean mFromOutside = false;
    private String mTickerText = null;
    private static int mStorageType = R.string.outer_storage;
    private Notification mNotification = new Notification();
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mInterrupt = false;
    private Constants mConstants = null;
    private ArrayList<File> mContactFileList = new ArrayList<File>();
    private ArrayList<File> mMsgFileList = new ArrayList<File>();
    private ArrayList<File> mQuicknoteFileList = new ArrayList<File>();
    private ArrayList<File> mCalendarFileList = new ArrayList<File>();
    private ArrayList<File> mBookmarkFileList = new ArrayList<File>();
    private ArrayList<Integer> mNeedReplaceList = new ArrayList<Integer>();
    private NotificationItem mCurrentItem;
//    private boolean mIsForeground = true;
    private boolean mPickerLock = true;
 // modified by amt_jiayuanyuan 2013-01-14 SWITCHUITWO-483 begin
    private long bufferSaved = 1011; 
 // modified by amt_jiayuanyuan 2013-01-14 SWITCHUITWO-483 end  

    public static BackupRestoreService getInstance() {
        return mInstance;
    }
    
    public NotificationAgent getNotificationAgent() {
        return mNotificationAgent;
    }
    
    public static void setmStorageType(int st) {
        mStorageType = st;
    }
    
    public boolean getIsProcessing() {
        return mIsProcessing;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // Auto-generated method stub
        return null;
    }
    
    public NotificationItem getCurrentItem() {
        return mCurrentItem;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mConstants = new Constants(getApplicationContext());
        mInstance = this;
        mBackupDest = new Utility(this).getBackupDest();
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {               
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, TAG);        
            
            if (mWakeLock != null) {
                Log.v(TAG, "Acquire WakeLock");
                mWakeLock.acquire();
            }else {
                Log.e(TAG, "Fail to Acquire WakeLock");
            }
        } else {
            Log.e(TAG, "Fail to get powerManager");
        }

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        Thread thr = new Thread(null, this, TAG);
        thr.start();
        mContext = this;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.v(TAG, "onStart, startID : " + startId);
        if (mBackupDest == Constants.INNER_STORAGE) {
            mStorageType = R.string.inner_storage;
        } else if (mBackupDest == Constants.OUTER_STORAGE) {
            mStorageType = R.string.outer_storage;
        }
        if((mContactsManager != null && mContactsManager.getIsWorking()) 
                || (mSmsManager != null && mSmsManager.getIsWorking())
                || (mQuickNoteManager != null && mQuickNoteManager.getIsWorking())
                || (mCalendarManager != null && mCalendarManager.getIsWorking())
                || (mAppManager != null && mAppManager.getIsWorking())
                || (mBookmarkManager != null && mBookmarkManager.getIsWorking())) {
            mIsProcessing = true;
        } else {
            mIsProcessing = false;
            mNotificationAgent = new NotificationAgent();
            mSmsManager = new SMSManager(this, mNotificationAgent);
            mContactsManager = new ContactsManager(this, mNotificationAgent);
            mQuickNoteManager = new QuickNoteManager(this, mNotificationAgent);
            mCalendarManager = new CalendarManager(this, mNotificationAgent);
            mAppManager = new AppManager(this, mNotificationAgent);
            mBookmarkManager = new BookmarkManager(this, mNotificationAgent);
        }
    if (null == intent) {
        return;
        }
        while (mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Got interruptedException: ", e);
                }
            }
        }
        mFromOutside = intent.getBooleanExtra(Constants.isFromOutside, false);
        if (mIsProcessing) {
            Log.e(TAG, "Service is already running...");
            Toast.makeText(this, getString(R.string.service_busy),
                    Toast.LENGTH_SHORT).show();

            // when launch backup/restore from other app but service busy
            // it is required to notify the BaseActivity2 to finish()
            if (mFromOutside == true) {
                Log.d(TAG, "Close transparent activity when busy");                
                Backup2 tmpBackup2Activity = Backup2.getInstance();
                Restore2 tmpRestore2Activity = Restore2.getInstance();
                Import3rd2 tmpImport3rd2Activity = Import3rd2.getInstance();
                ExportByAccount2 tmpExportByAccount2Activity = ExportByAccount2.getInstance();
                Message busyMsg = Message.obtain(null, R.id.service_busy, 0, 0, null);

                if (tmpBackup2Activity != null) {
                    tmpBackup2Activity.sendMsgtoActivity(busyMsg);
                } else if (tmpRestore2Activity != null) {
                    tmpRestore2Activity.sendMsgtoActivity(busyMsg);
                } else if (tmpImport3rd2Activity != null) {
                    tmpImport3rd2Activity.sendMsgtoActivity(busyMsg);
                } else if (tmpExportByAccount2Activity != null) {
                	tmpExportByAccount2Activity.sendMsgtoActivity(busyMsg);
                }
            }
            return;
        }

        Message msg = mServiceHandler.obtainMessage();
        if (null == msg)  return;        
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        boolean hasAvailable = true;
        msg.what = bundle.getInt(Constants.ACTION, Constants.NO_ACTION);
        
        if (intent.getBooleanExtra(Constants.SPECIFIC_CONTACTS, false)) {
            // pick contacts
            mPickerLock = true;
            new MulPickResult(mContext).execute(intent);
            while(mPickerLock) {}
        }
        
        switch(msg.what){
        case Constants.BACKUP_ACTION:
        case Constants.RESTORE_ACTION:
            // for moto default backup/restore
            clearCategorisedFiles();
            Uri[] uris = null;
            if (mUriArrayList != null) {
                uris = mUriArrayList.toArray(new Uri[0]);
            }
            msg.obj = new CheckedItems(false,
                                       bundle.getBoolean(Constants.CONTACTS, false),
                                       bundle.getBoolean(Constants.SMS, false), 
                                       bundle.getBoolean(Constants.QUICKNOTE, false),
                                       bundle.getBoolean(Constants.CALENDAR, false),
                                       bundle.getBoolean(Constants.APP, false),
                                       bundle.getBoolean(Constants.BOOKMARK, false),
                                       uris,
                                       null,
                                       bundle.getInt(Constants.DEL_OPTION, Constants.DEL_UNSET),
                                       null);
            break;
        case Constants.IMPORT3RD_ACTION:
            // for import 3rd party contacts/sms
            hasAvailable = categoriseSelectedFiles(bundle.getStringArrayList(Constants.PATH_LIST));
            msg.obj = new CheckedItems(bundle.getStringArrayList(Constants.PATH_LIST),
                                       bundle.getInt(Constants.DEL_OPTION, Constants.DEL_UNSET),
                                       bundle.getBoolean(Constants.CONTACTS, false),
                                       bundle.getBoolean(Constants.SMS, false),
                                       bundle.getBoolean(Constants.QUICKNOTE, false),
                                       bundle.getBoolean(Constants.CALENDAR, false),
                                       bundle.getBoolean(Constants.APP, false),
                                       bundle.getBoolean(Constants.BOOKMARK, false),
                                       null,
                                       bundle.getString(Constants.ACCOUNT_NAME),
                                       bundle.getString(Constants.ACCOUNT_TYPE));
            break;
        case Constants.EXPORTBYACCOUNT_ACTION:
            // for export contacts from different accounts
            msg.obj = new CheckedItems(bundle.getStringArray(Constants.CHECKED_ACCOUNT_NAMES),
                                       bundle.getInt(Constants.DEL_OPTION,Constants.DEL_UNSET)
                                       );
            break;
        }

        if (mFromOutside) {
            Log.d(TAG, "Activity from outside");
            mBackup2Activity = Backup2.getInstance();
            mRestore2Activity = Restore2.getInstance();
            mImport3rd2Activity = Import3rd2.getInstance();
            mExportByAccount2Activity = ExportByAccount2.getInstance();
        } else {
            Log.d(TAG, "Activity inside the package");
            mBackupActivity = Backup.getInstance();
            mRestoreActivity = Restore.getInstance();
            mImport3rdActivity = Import3rd.getInstance();
            mExportByAccountActivity = ExportByAccount.getInstance();
        }
        if (!hasAvailable) {
            mNotificationAgent.sendMsgWithNotification(Constants.IMPORT3RD_ACTION,
                    Message.obtain(null, R.id.status, R.id.empty_storage, 0, null));
            return;
        }
        mServiceHandler.sendMessage(msg);
    }
    
    /*
     *  if the param file is a folder
     *  categorise files that are in it, do not categorise the files in the sub directory.
     */
    private boolean categoriseSelectedFiles(ArrayList<String> pathList) {
        clearCategorisedFiles();
        boolean hasAvailableBackupFile = false;
        String tmpString = null;
        File tmpFile = null;
        File[] files = null;
        if (pathList == null) return false;
        for (int i = 0; i < pathList.size(); i++) {
            tmpString = pathList.get(i);
            if (tmpString == null || tmpString.equals("")) {
                continue;
            }
            tmpFile = new File(tmpString);
            if (!tmpFile.exists()) {
                continue;
            }
            if (tmpFile.isFile()) {
                if (insertFileByCatagory(tmpFile)) {
                    hasAvailableBackupFile = true;
                }
            } else if (tmpFile.isDirectory()) {
                files = tmpFile.listFiles();
                if (null == files) {
                    continue;
                }
                for (int j = 0; j < files.length; j++) {
                    if (files[j].isFile()) {
                        if (insertFileByCatagory(files[j])) {
                            hasAvailableBackupFile = true;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return hasAvailableBackupFile;
    }

    private void clearCategorisedFiles() {
        if (mContactFileList != null) mContactFileList.clear();
        if (mMsgFileList != null) mMsgFileList.clear();
        if (mQuicknoteFileList != null) mQuicknoteFileList.clear();
        if (mCalendarFileList != null) mCalendarFileList.clear();
        if (mBookmarkFileList != null) mBookmarkFileList.clear();
    }
    
    private boolean insertFileByCatagory(File file) {
        boolean isAvailable = true;
        if (!file.isFile()) return false;
        if (file.getName().endsWith(Constants.vcardExtern)) {
            mContactFileList.add(file);
        } else if (file.getName().endsWith(Constants.vmgExtern)) {
            mMsgFileList.add(file);
        } else if (file.getName().endsWith(Constants.qnExtern)) {
            mQuicknoteFileList.add(file);
        } else if (file.getName().endsWith(Constants.calExtern) || file.getName().endsWith(Constants.calCompatExtern)) {
            mCalendarFileList.add(file);
        //} else if (file.getName().endsWith(Constants.bookmarkExtern)) {
        //    mBookmarkFileList.add(file);
        } else {
            isAvailable = false;
        }
        return isAvailable;
    }
    
    private final class CheckedItems {
        private final boolean contacts;
        private final boolean sms;
        private final boolean quicknote;
        private final boolean calendar;
        private final boolean app;
        private final boolean bookmark;
        private final String[] checked_names;
        private final String path;
        private final ArrayList<String> file_list;
        private final Uri[] uri_list;
        private final int del_option;
        private final String account_name;
        private final String account_type;

         // for moto default backup/restore
        public CheckedItems(boolean o, boolean c, boolean s, boolean q, boolean cal, boolean a, boolean b, Uri[] uriList, String p, int d, String[] cn) {
            if (false == o) {
                contacts = c;
                sms = s;
                quicknote = q;
                calendar = cal;
                app = a;
                bookmark = b;
                path = null;
            } else {
                path = p;
                contacts = false;
                sms = false;
                quicknote = q;
                calendar = cal;
                bookmark = b;
                app = a;
            }
            uri_list = uriList;
            file_list = null;
            checked_names = cn;
            del_option = d;
            account_name = null;
            account_type = null;
        }

        // for import 3rd party contacts/sms
        public CheckedItems(ArrayList<String> fl, int d, boolean con, boolean sm, boolean q, boolean cal, boolean a, boolean b, String[]cn, String an, String at) {
            path = null;
            file_list = fl;
            del_option = d;
            contacts = con;
            sms = sm;
            quicknote = q;
            calendar = cal;
            app = a;
            bookmark = b;
            checked_names = cn;
            account_name = an;
            account_type = at;
            uri_list = null;
        }
        
        // for export by account
        public CheckedItems(String[] checkedNames, int d){
            int size = checkedNames.length;
            checked_names = new String[size];
            for (int i = 0; i < size; i++) {
                checked_names[i] = checkedNames[i];
            }
            file_list = null;
            path = null;
            del_option = d;
            contacts = false;
            sms = false;
            quicknote = false;
            calendar = false;
            app = false;
            bookmark = false;
            account_name = null;
            account_type = null;
            uri_list = null;
        }

    };

    public void run() {
        Looper.prepare();
        mServiceLooper = Looper.myLooper();
        mServiceHandler = new ServiceHandler();
        Looper.loop();
    }

    private final class ServiceHandler extends Handler {
        public void handleMessage(Message msg) {
            Log.d(TAG, "start processing");
            mIsProcessing = true;
            mAction = msg.what;
            HashMap<Integer, int[]> cr = new HashMap<Integer, int[]>();            
            int checkedItems = 0;
            int[] res = new int[Constants.GENUS_NUM];
            int[] actual = new int[Constants.GENUS_NUM];
            int[] total = new int[Constants.GENUS_NUM];
            for (int i : res) {
                i = 0;
            }
            for (int i : actual) {
                i = 0;
            }
            for (int i : total) {
                i = 0;
            }
            
            int res_local = 0;
            int res_card = 0;
            int res_cCard = 0;
            int res_gCard = 0;
            int[] res_gAccount;
            int[] res_eAccount;
            int[] actualCount;
            int[] totalCount;

            switch (mAction) {
            case Constants.BACKUP_ACTION:
            case Constants.RESTORE_ACTION:
                // for moto default backup/restore
                mContactsChecked = ((CheckedItems) msg.obj).contacts;
                mSmsChecked = ((CheckedItems) msg.obj).sms;
                mQuickNoteChecked = ((CheckedItems) msg.obj).quicknote;
                mCalendarChecked = ((CheckedItems) msg.obj).calendar;
                mAppChecked = ((CheckedItems) msg.obj).app;
                mBookmarkChecked = ((CheckedItems) msg.obj).bookmark;
                mUriList = ((CheckedItems) msg.obj).uri_list;
                if (mUriList != null) {
                    mIsExportSpeContacts = true;
                }
                break;
            case Constants.IMPORT3RD_ACTION:
                // for import 3rd party contacts/sms
                mSelectFileList = ((CheckedItems) msg.obj).file_list;
                mContactsChecked = ((CheckedItems) msg.obj).contacts;
                mSmsChecked = ((CheckedItems) msg.obj).sms;
                mQuickNoteChecked = ((CheckedItems) msg.obj).quicknote;
                mCalendarChecked = ((CheckedItems) msg.obj).calendar;
                mAppChecked = ((CheckedItems) msg.obj).app;
                mBookmarkChecked = ((CheckedItems) msg.obj).bookmark;
                mAccountName = ((CheckedItems) msg.obj).account_name;
                mAccountType = ((CheckedItems) msg.obj).account_type;
                break;
            case Constants.EXPORTBYACCOUNT_ACTION:
                //for export contacts from different accounts
                String tmpString;
                int size = ((CheckedItems) msg.obj).checked_names.length;
                checkedNames = new String[size];
                for (int i = 0; i < size; i++) {
                    tmpString = checkedNames[i] = ((CheckedItems) msg.obj).checked_names[i];
                    Log.v(TAG, "CheckedNames[" + i + "]=" + checkedNames[i]);
                    if (tmpString.equals(Constants.LOCAL_ACCOUNT_NAME)) {
                        mLocalChecked = true;
                    } else if (tmpString.equals(Constants.CARD_ACCOUNT_NAME)) {
                        mCardChecked = true;
                    } else if (tmpString.equals(Constants.CCARD_ACCOUNT_NAME)) {
                        mCCardChecked = true;
                    } else if (tmpString.equals(Constants.GCARD_ACCOUNT_NAME)) {
                        mGCardChecked = true;
                    } else if(tmpString.startsWith(Constants.GACCOUNT_PREFIX)) {
                        GAccount.add(tmpString);
                    } else if(tmpString.startsWith(Constants.EACCOUNT_PREFIX)) {
                        EAccount.add(tmpString);
                    } 
                }
                break;
            default:
                break;
            }
            mDelOption = ((CheckedItems) msg.obj).del_option;

            switch (msg.what) {
            case Constants.BACKUP_ACTION:
                if (!mIsExportSpeContacts && isPhoneStorageEmpty()) break;
                if (!mIsExportSpeContacts && !promptDeleteOldBackupFile()) break;
                if (mContactsChecked) {
                    checkedItems += Constants.CONTACTS_ACTION;
                    if (mUriList != null) {
                        //Backup specific contacts
                        res[Constants.INDEX_CONTACT] = mContactsManager.BackupContacts(new ArrayList(Arrays.asList(mUriList)));
                    } else {
                        //Auto Backup
                        res[Constants.INDEX_CONTACT] = mContactsManager.BackupContacts();
                    }
                    actual[Constants.INDEX_CONTACT] = mContactsManager.getActualVcardCounter();
                    total[Constants.INDEX_CONTACT] = mContactsManager.getTotalCounter();
                    Log.d(TAG, "BACKUP: res_1:"+res[Constants.INDEX_CONTACT]);
                    if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                        mInterrupt = true;
                    }
                    cr.put(Constants.INDEX_CONTACT, new int[]{
                            res[Constants.INDEX_CONTACT],
                            actual[Constants.INDEX_CONTACT],
                            total[Constants.INDEX_CONTACT]});
                }

                if (mSmsChecked) {
                    checkedItems += Constants.SMS_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_SMS] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_SMS] = mSmsManager.BackupSMS();
                        actual[Constants.INDEX_SMS] = mSmsManager.getActualCounter();
                        total[Constants.INDEX_SMS] = mSmsManager.getTotalCounter();
                        Log.d(TAG, "BACKUP: res_2:" +res[Constants.INDEX_SMS]);
                        if (res[Constants.INDEX_SMS] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_SMS, new int[]{
                            res[Constants.INDEX_SMS],
                            actual[Constants.INDEX_SMS],
                            total[Constants.INDEX_SMS]});
                }

                if (mQuickNoteChecked) {
                    checkedItems += Constants.QUICKNOTE_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_QUICKNOTE] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.BackupQuickNote();
                        actual[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getActualCounter();
                        total[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getTotalCounter();
                        Log.d(TAG, "BACKUP: res_quickNote:"+res[Constants.INDEX_QUICKNOTE]);
                        if (res[Constants.INDEX_QUICKNOTE] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_QUICKNOTE, new int[]{
                            res[Constants.INDEX_QUICKNOTE],
                            actual[Constants.INDEX_QUICKNOTE],
                            total[Constants.INDEX_QUICKNOTE]});
                }                
                if (mCalendarChecked) {
                    checkedItems += Constants.CALENDAR_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_CALENDAR] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_CALENDAR] = mCalendarManager.BackupCalendar();
                        actual[Constants.INDEX_CALENDAR] = mCalendarManager.getActualCounter();
                        total[Constants.INDEX_CALENDAR] = mCalendarManager.getTotalCounter();
                        Log.d(TAG, "BACKUP: res_quickNote:"+res[Constants.INDEX_CALENDAR]);
                        if (res[Constants.INDEX_CALENDAR] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_CALENDAR, new int[]{
                            res[Constants.INDEX_CALENDAR],
                            actual[Constants.INDEX_CALENDAR],
                            total[Constants.INDEX_CALENDAR]});
                }
                if (mAppChecked) {
                    checkedItems += Constants.APP_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_APP] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_APP] = mAppManager.BackupApp();
                        actual[Constants.INDEX_APP] = mAppManager.getActualCounter();
                        total[Constants.INDEX_APP] = mAppManager.getTotalCounter();
                        if (res[Constants.INDEX_APP] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_APP, new int[]{
                            res[Constants.INDEX_APP],
                            actual[Constants.INDEX_APP],
                            total[Constants.INDEX_APP]});
                }
                if (mBookmarkChecked) {
                    checkedItems += Constants.BOOKMARK_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_BOOKMARK] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_BOOKMARK] = mBookmarkManager.Backup();
                        actual[Constants.INDEX_BOOKMARK] = mBookmarkManager.getActualCounter();
                        total[Constants.INDEX_BOOKMARK] = mBookmarkManager.getTotalCounter();
                        if (res[Constants.INDEX_BOOKMARK] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_BOOKMARK, new int[]{
                            res[Constants.INDEX_BOOKMARK],
                            actual[Constants.INDEX_BOOKMARK],
                            total[Constants.INDEX_BOOKMARK]});
                }
                mNotificationAgent.sendMsgWithNotification(Constants.BACKUP_ACTION,
                        Message.obtain(null, R.id.status, R.id.final_result, checkedItems, cr));
                break;
            case Constants.RESTORE_ACTION:
                if (mContactsChecked) {
                    checkedItems += Constants.CONTACTS_ACTION;
                    mContactFileList.add(new File(mConstants.DEFAULT_FOLDER + "/" + Constants.CONTACTS_FILE_NAME));
                    res[Constants.INDEX_CONTACT] = mContactsManager.RestoreContacts(Constants.RESTORE_ACTION, 
                                        mContactFileList,
                                        null,
                                        null);
                    actual[Constants.INDEX_CONTACT] = mContactsManager.getActualVcardCounter();
                    total[Constants.INDEX_CONTACT] = mContactsManager.getTotalCounter();
                    if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                        mInterrupt = true;
                    }
                    cr.put(Constants.INDEX_CONTACT, new int[]{
                            res[Constants.INDEX_CONTACT],
                            actual[Constants.INDEX_CONTACT],
                            total[Constants.INDEX_CONTACT]});
                }

                if (mSmsChecked) {
                    checkedItems += Constants.SMS_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_SMS] = R.id.cancel;
                    } else {
                        mMsgFileList.add(new File(mConstants.DEFAULT_FOLDER + "/" + Constants.SMS_FILE_NAME));
                        res[Constants.INDEX_SMS] = mSmsManager.RestoreMsg(Constants.RESTORE_ACTION, mMsgFileList);
                        actual[Constants.INDEX_SMS] = mSmsManager.getActualCounter();
                        total[Constants.INDEX_SMS] = mSmsManager.getTotalCounter();
                        if (res[Constants.INDEX_SMS] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_SMS, new int[]{
                            res[Constants.INDEX_SMS],
                            actual[Constants.INDEX_SMS],
                            total[Constants.INDEX_SMS]});
                }

                if (mQuickNoteChecked) {
                    checkedItems += Constants.QUICKNOTE_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_QUICKNOTE] = R.id.cancel;
                    } else {
                        mQuicknoteFileList.add(new File(mConstants.DEFAULT_FOLDER + "/" + Constants.QUICKNOTE_FILE_NAME));
                        res[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.RestoreQuickNote(Constants.RESTORE_ACTION, mQuicknoteFileList);
                        actual[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getActualCounter();
                        total[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getTotalCounter();
                        if (res[Constants.INDEX_QUICKNOTE] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_QUICKNOTE, new int[]{
                            res[Constants.INDEX_QUICKNOTE],
                            actual[Constants.INDEX_QUICKNOTE],
                            total[Constants.INDEX_QUICKNOTE]});
                }
                if (mCalendarChecked) {
                    checkedItems += Constants.CALENDAR_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_CALENDAR] = R.id.cancel;
                    } else {
                        mCalendarFileList.add(new File(mConstants.DEFAULT_FOLDER + "/" + Constants.CALENDAR_FILE_NAME));
                        res[Constants.INDEX_CALENDAR] = mCalendarManager.RestoreCalendar(Constants.RESTORE_ACTION, mCalendarFileList);
                        actual[Constants.INDEX_CALENDAR] = mCalendarManager.getActualCounter();
                        total[Constants.INDEX_CALENDAR] = mCalendarManager.getTotalCounter();
                        Log.d(TAG, "BACKUP: res_quickNote:"+res[Constants.INDEX_CALENDAR]);
                        if (res[Constants.INDEX_CALENDAR] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_CALENDAR, new int[]{
                            res[Constants.INDEX_CALENDAR],
                            actual[Constants.INDEX_CALENDAR],
                            total[Constants.INDEX_CALENDAR]});
                }
                if (mAppChecked) {
                    checkedItems += Constants.APP_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_APP] = R.id.cancel;
                    } else {
                        res[Constants.INDEX_APP] = mAppManager.RestoreApp(Constants.RESTORE_ACTION,
                                mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
                        actual[Constants.INDEX_APP] = mAppManager.getActualCounter();
                        total[Constants.INDEX_APP] = mAppManager.getTotalCounter();
                        Log.d(TAG, "BACKUP: res_app:"+res[Constants.INDEX_APP]);
                        if (res[Constants.INDEX_APP] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_APP, new int[]{
                            res[Constants.INDEX_APP],
                            actual[Constants.INDEX_APP],
                            total[Constants.INDEX_APP]});
                }
                if (mBookmarkChecked) {
                    checkedItems += Constants.BOOKMARK_ACTION;
                    if (mInterrupt) {
                        res[Constants.INDEX_BOOKMARK] = R.id.cancel;
                    } else {
                        mBookmarkFileList.add(new File(mConstants.DEFAULT_FOLDER + "/" + Constants.BOOKMARK_FILE_NAME));
                        res[Constants.INDEX_BOOKMARK] = mBookmarkManager.Restore(Constants.RESTORE_ACTION, mBookmarkFileList);
                        actual[Constants.INDEX_BOOKMARK] = mBookmarkManager.getActualCounter();
                        total[Constants.INDEX_BOOKMARK] = mBookmarkManager.getTotalCounter();
                        if (res[Constants.INDEX_BOOKMARK] == R.id.cancel) {
                            mInterrupt = true;
                        }
                    }
                    cr.put(Constants.INDEX_BOOKMARK, new int[]{
                            res[Constants.INDEX_BOOKMARK],
                            actual[Constants.INDEX_BOOKMARK],
                            total[Constants.INDEX_BOOKMARK]});
                }
                mNotificationAgent.sendMsgWithNotification(Constants.RESTORE_ACTION,
                        Message.obtain(null, R.id.status, R.id.final_result, checkedItems, cr));
                break;
                
            case Constants.IMPORT3RD_ACTION:
                if (true){                    
                    if (mContactsChecked) {
                        checkedItems += Constants.CONTACTS_ACTION;
                        Log.d(TAG, "Only import contacts files from 3rd party");
                        res[Constants.INDEX_CONTACT] = mContactsManager.RestoreContacts(Constants.IMPORT3RD_ACTION, mContactFileList, mAccountName, mAccountType);
                        actual[Constants.INDEX_CONTACT] = mContactsManager.getActualFileCounter();
                        total[Constants.INDEX_CONTACT] = mContactsManager.getTotalFileCounter();
                        if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                            mInterrupt = true;
                        }
                        cr.put(Constants.INDEX_CONTACT, new int[]{
                                res[Constants.INDEX_CONTACT],
                                actual[Constants.INDEX_CONTACT],
                                total[Constants.INDEX_CONTACT]});
                    } 
                    
                    if (mSmsChecked) {
                        checkedItems += Constants.SMS_ACTION;
                        if (mInterrupt) {
                            res[Constants.INDEX_SMS] = R.id.cancel;
                        } else {
                            res[Constants.INDEX_SMS] = mSmsManager.RestoreMsg(Constants.IMPORT3RD_ACTION, mMsgFileList);
                            actual[Constants.INDEX_SMS] = mSmsManager.getActualFileCounter();
                            total[Constants.INDEX_SMS] = mSmsManager.getTotalFileCounter();
                            if (res[Constants.INDEX_SMS] == R.id.cancel) {
                                mInterrupt = true;
                            }
                        }
                        cr.put(Constants.INDEX_SMS, new int[]{
                                res[Constants.INDEX_SMS],
                                actual[Constants.INDEX_SMS],
                                total[Constants.INDEX_SMS]});
                    }
                    
                    if (mQuickNoteChecked) {
                        checkedItems += Constants.QUICKNOTE_ACTION;
                        if (mInterrupt) {
                            res[Constants.INDEX_QUICKNOTE] = R.id.cancel;
                        } else {
                            res[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.RestoreQuickNote(Constants.IMPORT3RD_ACTION, mQuicknoteFileList);
                            actual[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getActualFileCounter();
                            total[Constants.INDEX_QUICKNOTE] = mQuickNoteManager.getTotalFileCounter();
                            if (res[Constants.INDEX_QUICKNOTE] == R.id.cancel) {
                                mInterrupt = true;
                            }
                        }
                        cr.put(Constants.INDEX_QUICKNOTE, new int[]{
                                res[Constants.INDEX_QUICKNOTE],
                                actual[Constants.INDEX_QUICKNOTE],
                                total[Constants.INDEX_QUICKNOTE]});
                    }
                    
                    if (mCalendarChecked) {
                        checkedItems += Constants.CALENDAR_ACTION;
                        if (mInterrupt) {
                            res[Constants.INDEX_CALENDAR] = R.id.cancel;
                        } else {
                            res[Constants.INDEX_CALENDAR] = mCalendarManager.RestoreCalendar(Constants.IMPORT3RD_ACTION, mCalendarFileList);
                            actual[Constants.INDEX_CALENDAR] = mCalendarManager.getActualFileCounter();
                            total[Constants.INDEX_CALENDAR] = mCalendarManager.getTotalFileCounter();
                            if (res[Constants.INDEX_CALENDAR] == R.id.cancel) {
                                mInterrupt = true;
                            }
                        }
                        cr.put(Constants.INDEX_CALENDAR, new int[]{
                                res[Constants.INDEX_CALENDAR],
                                actual[Constants.INDEX_CALENDAR],
                                total[Constants.INDEX_CALENDAR]});
                    }
                    if (mBookmarkChecked) {
                        checkedItems += Constants.BOOKMARK_ACTION;
                        if (mInterrupt) {
                            res[Constants.INDEX_BOOKMARK] = R.id.cancel;
                        } else {
                            res[Constants.INDEX_BOOKMARK] = mBookmarkManager.Restore(Constants.IMPORT3RD_ACTION, mBookmarkFileList);
                            actual[Constants.INDEX_BOOKMARK] = mBookmarkManager.getActualFileCounter();
                            total[Constants.INDEX_BOOKMARK] = mBookmarkManager.getTotalFileCounter();
                            if (res[Constants.INDEX_BOOKMARK] == R.id.cancel) {
                                mInterrupt = true;
                                }
                        }
                        cr.put(Constants.INDEX_BOOKMARK, new int[]{
                                res[Constants.INDEX_BOOKMARK],
                                actual[Constants.INDEX_BOOKMARK],
                                total[Constants.INDEX_BOOKMARK]});
                    }

                    if (!mFromOutside) {
                        mNotificationAgent.sendMsgWithNotification(Constants.IMPORT3RD_ACTION,
                                Message.obtain(null, R.id.status, R.id.final_result, checkedItems, cr));
                    } else {
                        if (mContactsChecked) {
                            mNotificationAgent.sendMsgWithNotification(Constants.IMPORT3RD_ACTION,
                                    Message.obtain(null, R.id.status, R.id.final_result, Constants.CONTACTS_ACTION, cr));
                        } else if (mSmsChecked) {
                            mNotificationAgent.sendMsgWithNotification(Constants.IMPORT3RD_ACTION,
                                    Message.obtain(null, R.id.status, R.id.final_result, Constants.SMS_ACTION, cr));
                        } else {
                            Log.e(TAG, "There is something wrong for reaching here");
                        }
                    }
                    break;
                }
                break;
            case Constants.EXPORTBYACCOUNT_ACTION:
                int isReady = mContactsManager.getReadyToBackup();
                if (isReady != R.id.success) {
                    break;
                }
                if (!mFromOutside) {
                } else {
                    mExportByAccount2Activity.setCurrentDir(mContactsManager.getDateString());
                }
                if (mLocalChecked) {
                    res[Constants.INDEX_CONTACT] = mContactsManager.BackupLocalContacts();
                    actual[Constants.INDEX_CONTACT] = mContactsManager.getActualVcardCounterArray()[0];
                    total[Constants.INDEX_CONTACT] = mContactsManager.getTotalCounterArray()[0];
                    if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                        mInterrupt = true;
                    }
                }
                
                if (mCardChecked) {
                    if (mInterrupt == true) {
                        res[Constants.INDEX_CONTACT] = R.id.cancel;
                    } else {
                        res_card = mContactsManager.BackupCardContacts();
                        if (res_card == R.id.cancel) {
                            mInterrupt = true;
                        }
                        if (res[Constants.INDEX_CONTACT] != R.id.success) {
                            res[Constants.INDEX_CONTACT] = res_card;
                        }
                        actual[Constants.INDEX_CONTACT] += mContactsManager.getActualVcardCounterArray()[0];
                        total[Constants.INDEX_CONTACT] += mContactsManager.getTotalCounterArray()[0];
                    }
                }
                
                if (mCCardChecked) {
                    if (mInterrupt == true) {
                        res[Constants.INDEX_CONTACT] = R.id.cancel;
                    } else {
                        res_cCard = mContactsManager.BackupCCardContacts();
                        if (res_cCard == R.id.cancel) {
                            mInterrupt = true;
                        }
                        if (res[Constants.INDEX_CONTACT] != R.id.success) {
                            res[Constants.INDEX_CONTACT] = res_cCard;
                        }
                        actual[Constants.INDEX_CONTACT] += mContactsManager.getActualVcardCounterArray()[0];
                        total[Constants.INDEX_CONTACT] += mContactsManager.getTotalCounterArray()[0];
                    }
                }

                if (mGCardChecked) {
                    if (mInterrupt == true) {
                        res[Constants.INDEX_CONTACT] = R.id.cancel;
                    } else {
                        res_gCard = mContactsManager.BackupGCardContacts();                    
                        if (res_gCard == R.id.cancel) {
                            mInterrupt = true;
                        }
                        if (res[Constants.INDEX_CONTACT] != R.id.success) {
                            res[Constants.INDEX_CONTACT] = res_gCard;
                        }
                        actual[Constants.INDEX_CONTACT] += mContactsManager.getActualVcardCounterArray()[0];
                        total[Constants.INDEX_CONTACT] += mContactsManager.getTotalCounterArray()[0];
                    }
                }
                
                if (GAccount.size() != 0) {
                    if (mInterrupt == true) {
                        res[Constants.INDEX_CONTACT] = R.id.cancel;
                    } else {
                        res_gAccount = mContactsManager.BackupGAccountContacts((String[])GAccount.toArray(new String[0]));
                        actualCount = mContactsManager.getActualVcardCounterArray();
                        totalCount = mContactsManager.getTotalCounterArray();
                        for(int i=0;i<res_gAccount.length;i++){
                            if (res[Constants.INDEX_CONTACT] != R.id.success) {
                                res[Constants.INDEX_CONTACT] = res_gAccount[i];
                                if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                                    mInterrupt = true;
                                }
                            }
                            actual[Constants.INDEX_CONTACT] += actualCount[i];
                            total[Constants.INDEX_CONTACT] += totalCount[i];
                        }
                    }
                }
                
                if (EAccount.size() != 0) {
                    if (mInterrupt == true) {
                        res[Constants.INDEX_CONTACT] = R.id.cancel;
                    } else {
                        res_eAccount = mContactsManager.BackupEAccountContacts((String[])EAccount.toArray(new String[0]));
                        actualCount = mContactsManager.getActualVcardCounterArray();
                        totalCount = mContactsManager.getTotalCounterArray();
                        for(int i=0;i<res_eAccount.length;i++){
                            if (res[Constants.INDEX_CONTACT] != R.id.success) {
                                res[Constants.INDEX_CONTACT] = res_eAccount[i];
                                if (res[Constants.INDEX_CONTACT] == R.id.cancel) {
                                    mInterrupt = true;
                                }
                            }
                            actual[Constants.INDEX_CONTACT] += actualCount[i];
                            total[Constants.INDEX_CONTACT] += totalCount[i];
                        }
                    }
                }
                cr.put(Constants.INDEX_CONTACT, new int[]{
                        res[Constants.INDEX_CONTACT],
                        actual[Constants.INDEX_CONTACT],
                        total[Constants.INDEX_CONTACT]});
                mNotificationAgent.sendMsgWithNotification(Constants.EXPORTBYACCOUNT_ACTION, 
                        Message.obtain(null, R.id.status, R.id.final_result, Constants.CONTACTS_ACTION, cr));
                
                break;
            default:
                Log.e(TAG, "Should NOT be here! mAction = " + mAction);
                break;            
            }

            stopSelf();
        }
    };

    //1 - only contact; 2 - only sms; 3 - contacts & sms
    private boolean isPhoneStorageEmpty() {
        int flags = Constants.NO_ACTION; // 0
        if (mContactsChecked && mContactsManager.isContactsDBEmpty()) {
            flags += Constants.CONTACTS_ACTION; // 1
        }
        if (mSmsChecked && mSmsManager.isSmsDBEmpty()) {
            flags += Constants.SMS_ACTION; // 2
        }
        if (flags != Constants.NO_ACTION) {
            mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                    Message.obtain(null, R.id.confirmation, R.id.empty_storage,flags)); 
            return true;
        }
        return false;
    }
    
    private boolean promptDeleteOldBackupFile() {
        mNeedReplaceList.clear();
        if (mDelOption == Constants.DEL_UNSET) {
            File oldContactFile=new File(mConstants.SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.CONTACTS_FILE_NAME);
            File oldMsgFile=new File(mConstants.SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.SMS_FILE_NAME);
            File oldAltContactFile=new File(mConstants.ALT_SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.CONTACTS_FILE_NAME);
            File oldAltMsgFile=new File(mConstants.ALT_SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.SMS_FILE_NAME);
            File oldQNFile=new File(mConstants.SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.QUICKNOTE_FILE_NAME);
            File oldAltQNFile=new File(mConstants.ALT_SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.QUICKNOTE_FILE_NAME);
            File oldCalFile=new File(mConstants.SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.CALENDAR_FILE_NAME);
            File oldAltCalFile=new File(mConstants.ALT_SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.CALENDAR_FILE_NAME);
            File oldBMFile=new File(mConstants.SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.BOOKMARK_FILE_NAME);
            File oldAltBMFile=new File(mConstants.ALT_SDCARD_PREFIX+"/"+mConstants.DEFAULT_FOLDER+"/"+Constants.BOOKMARK_FILE_NAME);
            
            if( mContactsChecked && (oldContactFile.exists() || oldAltContactFile.exists()) )  mNeedReplaceList.add(Constants.CONTACTS_ACTION);
            if( mSmsChecked && (oldMsgFile.exists() || oldAltMsgFile.exists()) )  mNeedReplaceList.add(Constants.SMS_ACTION);
            if( mQuickNoteChecked && (oldQNFile.exists() || oldAltQNFile.exists()) )  mNeedReplaceList.add(Constants.QUICKNOTE_ACTION);
            if( mCalendarChecked && (oldCalFile.exists() || oldAltCalFile.exists()) )  mNeedReplaceList.add(Constants.CALENDAR_ACTION);
            if( mAppChecked && (oldApkFileExist() || oldAltApkFileExist()) )  mNeedReplaceList.add(Constants.APP_ACTION);
            if( mBookmarkChecked && (oldBMFile.exists() || oldAltBMFile.exists()) )  mNeedReplaceList.add(Constants.BOOKMARK_ACTION);
            
            if( mNeedReplaceList.size() != 0 ){  // checked and ask user if it hopes to replace old file or not?
                mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                        Message.obtain(null, R.id.confirmation, R.id.exist_old_backup_file, 0, mNeedReplaceList));
                return false;
            }            
            return true;
        }

        if (mDelOption == Constants.DEL_YES) {            
            if( mSmsChecked || mContactsChecked || mQuickNoteChecked 
                    || mCalendarChecked || mAppChecked || mBookmarkChecked){
                return true;
            }
            return false;
        }
        
        if (mDelOption == Constants.DEL_CANCEL)  return false;
        
        return false;  // should not be here
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        mInstance = null;
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.v(TAG, "Release WakeHold");
            mWakeLock.release();
            mWakeLock = null;
        }

        // Make sure thread has started before telling it to quit.
        while (mServiceLooper == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Got interruptedException: ", e);
                }
            }
        }
        mIsProcessing = false;
        if (mContactsChecked || mFileChecked || mFolderChecked
            || mLocalChecked || mCCardChecked || mGCardChecked
            || GAccount.size()!=0 || EAccount.size()!=0
            || mCardChecked
            ) {
            mContactsManager.setInterrupted(true);
        }
        if (mSmsChecked || mFileChecked || mFolderChecked) {
            mSmsManager.setInterrupted(true);
        }
        if (mQuickNoteChecked || mFileChecked || mFolderChecked) {
            mQuickNoteManager.setInterrupted(true);
        }
        if (mCalendarChecked || mFileChecked || mFolderChecked) {
            mCalendarManager.setInterrupted(true);
        }
        if (mAppChecked || mFileChecked || mFolderChecked) {
            mAppManager.setInterrupted(true);
        }
        if (mBookmarkChecked || mFileChecked || mFolderChecked) {
            mBookmarkManager.setInterrupted(true);
        }
        mServiceLooper.quit();
    }

    public final class NotificationAgent {
        NotificationManager mNotificationManager;

        public NotificationAgent() {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public void showNotificationDialog(NotificationItem item) {
            
        }
        
        public void sendMsgWithNotification(int action, Message msg) {
            Log.v(TAG, "Enter sendMsgWithNotification()");
            BaseActivity tmpInstance = null;
            BaseActivity activity = null;
            BaseActivity2 tmpInstance2 = null;
            BaseActivity2 activity2 = null;

            if (mFromOutside) {
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance2 = Backup2.getInstance();
                    activity2 = mBackup2Activity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance2 = Restore2.getInstance();
                    activity2 = mRestore2Activity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance2 = Import3rd2.getInstance();
                    activity2 = mImport3rd2Activity;
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance2 = ExportByAccount2.getInstance();
                    activity2 = mExportByAccount2Activity;
                } else {
                    Log.e(TAG, "In sendMsgWithNotification, Wrong action type");
                    return;
                }
                if (tmpInstance2 != null && tmpInstance2.equals(activity2) && activity2 != null && activity2.getForegroundStatus()) {
                    activity2.sendMsgtoActivity(msg);
                } else {
                    sendNotification(action, msg);
                    // notify Activity to remove progress bar
                    mNotificationAgent.sendMsgWithoutNotification(action,
                            Message.obtain(null, R.id.status, R.id.remove_dialog, 0, null));
                }
            } else {
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance = Backup.getInstance();
                    activity = mBackupActivity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance = Restore.getInstance();
                    activity = mRestoreActivity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance = Import3rd.getInstance();
                    activity = mImport3rdActivity;
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance = ExportByAccount.getInstance();
                    activity = mExportByAccountActivity;
                } else {
                    Log.e(TAG, "In sendMsgWithNotification, Wrong action type");
                    return;
                }


                if (tmpInstance != null && tmpInstance.equals(activity) && activity != null && activity.getForegroundStatus()) {
                    activity.sendMsgtoActivity(msg);
                } else {
                    sendNotification(action, msg);
                    // notify Activity to remove progress bar
                    mNotificationAgent.sendMsgWithoutNotification(action,
                            Message.obtain(null, R.id.status, R.id.remove_dialog, 0, null));
                }
            }
        }

        public void sendMsgWithoutNotification(int action, Message msg) {
            Log.v(TAG, "Enter sendMsgWithoutNotification()");
            BaseActivity tmpInstance = null;
            BaseActivity activity = null;
            BaseActivity2 tmpInstance2 = null;
            BaseActivity2 activity2 = null;

            if (mFromOutside == true) {
                Log.d(TAG, "Enter mFromOutside path");
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance2 = Backup2.getInstance();
                    activity2 = mBackup2Activity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance2 = Restore2.getInstance();
                    activity2 = mRestore2Activity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance2 = Import3rd2.getInstance();
                    activity2 = mImport3rd2Activity;
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance2 = ExportByAccount2.getInstance();
                    activity2 = mExportByAccount2Activity;
                } else {
                    Log.e(TAG, "In sendMsgWithoutNotification, Wrong action type");
                    return;
                }

                if (tmpInstance2 != null && tmpInstance2.equals(activity2) && activity2 != null && activity2.getForegroundStatus()) {
                    Log.v(TAG, "Before activity.sendMsgtoActivity(msg)");
                    activity2.sendMsgtoActivity(msg);
                } else {
                    Log.e(TAG, "Activity was destroyed I, do nothing!");
                }
            } else {
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance = Backup.getInstance();
                    activity = mBackupActivity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance = Restore.getInstance();
                    activity = mRestoreActivity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance = Import3rd.getInstance();
                    activity = mImport3rdActivity;
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance = ExportByAccount.getInstance();
                    activity = mExportByAccountActivity;
                } else {
                    Log.e(TAG, "In sendMsgWithoutNotification, Wrong action type");
                    return;
                }

                if (tmpInstance != null && tmpInstance.equals(activity) && activity != null && activity.getForegroundStatus()) {
                // if user press menu button, current activity will be hidden in background 
                // instead of being destroyed. In this case, some msgs such as progress length 
                // should keep sending to the activity.
                    Log.v(TAG, "Before activity.sendMsgtoActivity(msg)");
                    activity.sendMsgtoActivity(msg);
                } else {
                // do nothing
                    Log.e(TAG, "Activity was destroyed, do nothing!");
                }
            }
        }

        private void sendNotification(int action, Message msg) {
            Log.v(TAG, "Enter sendNotification()");
            String tickerText = "";
            String conTitle = "";
            String complete = "";
            String storageType = "";

            complete = getString(R.string.popup_complete_result_title);
            if (action == Constants.BACKUP_ACTION) {
                conTitle = getString(R.string.backup_content, complete);
            } else if (action == Constants.RESTORE_ACTION) {
                conTitle = getString(R.string.restore_content, complete);
            } else if (action == Constants.IMPORT3RD_ACTION) {
                if (mSelectFileList != null) {
                    int isAltSdcard = SdcardManager.isAltSdcard(mContext, mSelectFileList.get(0));
                    if (isAltSdcard == SdcardManager.INNER) {
                        storageType = getString(R.string.inner_storage);
                    } else if (isAltSdcard == SdcardManager.OUTTER) {
                        storageType = getString(R.string.outer_storage);
                } else {
                    storageType = "";
                }
                } else {
                    storageType = "";
                }
             // modified by amt_jiayuanyuan 2013-01-21 SWITCHUITWO-553 begin
                conTitle = getString(R.string.import3rd_content, storageType, complete);
                Log.d("jiayy","conTitle = "+conTitle);
             // modified by amt_jiayuanyuan 2013-01-21 SWITCHUITWO-553 end
            } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                conTitle = getString(R.string.export_content, complete);
            } else {
                Log.v("sendNotification()", "Shouldn't be here");
            }
            tickerText = getShowMsg(msg.arg1, msg.obj);
            Log.i(TAG, "Enter sendNotification() tickerText="+tickerText);

            String conText = tickerText;
            int icon = R.drawable.stat_notify_data_backup;
            long when = System.currentTimeMillis();
            //mNotification.icon = icon;
            mNotification.tickerText = tickerText;
            mNotification.when = when;
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            Intent intent = new Intent(Constants.ACTION_RESULT);
            if (intent != null) {
                intent.setClassName(Constants.THIS_PACKAGE_NAME,
                        NotificationResultActivity.class.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.POPUP_COMPLETE_INFO, tickerText);
                intent.putExtra(Constants.ACTION_TYPE, action);
            }
            
            NotificationResultActivity tmpObj = NotificationResultActivity.getInstance();
            // modified by amt_jiayuanyuan 2012-12-11 SWITCHUITWO-256 begin
            if (tmpObj != null) {
                tmpObj.finish();
               //startActivity(intent);
            } 
            startActivity(intent);
            // modified by amt_jiayuanyuan 2012-12-11 SWITCHUITWO-256 end
            PendingIntent conIntent = PendingIntent.getActivity(mContext, 0,
                    intent, Intent.FLAG_ACTIVITY_NEW_TASK);
            // modified by amt_jiayuanyuan 2013-1-4 SWITCHUITWO-245 begin
            mNotification.icon = icon;
            mIsProcessing = false;
           // modified by amt_jiayuanyuan 2013-1-4 SWITCHUITWO-245 end
            mNotification.setLatestEventInfo(mContext, conTitle, conText, conIntent);
            mNotificationManager.notify(Constants.NOTIFICATION_ID, mNotification);
        }

        public void sendNotificationProgress(NotificationItem item) {
            Log.e(TAG, "Enter sendNotificationProgress()");
            BaseActivity tmpInstance = null;
            BaseActivity activity = null;
            BaseActivity2 tmpInstance2 = null;
            BaseActivity2 activity2 = null;
            int action = item.direction;
            mCurrentItem = item;

            if (mFromOutside == true) {
                Log.d(TAG, "Enter mFromOutside path");
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance2 = Backup2.getInstance();
                    activity2 = mBackup2Activity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance2 = Restore2.getInstance();
                    activity2 = mRestore2Activity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance2 = Import3rd2.getInstance();
                    activity2 = mImport3rd2Activity;
                    if (activity2 == null) { Log.e(TAG, "Yeap, Activity is NULL"); } 
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance2 = ExportByAccount2.getInstance();
                    activity2 = mExportByAccount2Activity;
                } else {
                    Log.e(TAG, "Wrong action type");
                    return;
                }
                if (tmpInstance2 != null && activity2 != null && tmpInstance2.equals(activity2) && activity2.getForegroundStatus()) {
                    // do nothing
                    Log.v(TAG, "Inside actvity, need not show progress bar on Notification");
                } else {
                    Log.e(TAG, "At background, show progress bar on Notification!");
                    showNotificationProgress(item);
                }
            } else {
                if (action == Constants.BACKUP_ACTION) {
                    tmpInstance = Backup.getInstance();
                    activity = mBackupActivity;
                } else if (action == Constants.RESTORE_ACTION) {
                    tmpInstance = Restore.getInstance();
                    activity = mRestoreActivity;
                } else if (action == Constants.IMPORT3RD_ACTION) {
                    tmpInstance = Import3rd.getInstance();
                    activity = mImport3rdActivity;
                } else if (action == Constants.EXPORTBYACCOUNT_ACTION) {
                    tmpInstance = ExportByAccount.getInstance();
                    activity = mExportByAccountActivity;
                } else {
                    Log.e(TAG, "Wrong action type");
                    return;
                }

                if (activity != null && tmpInstance != null && tmpInstance.equals(activity) && activity.getForegroundStatus()) {
                    // do nothing
                    Log.v(TAG, "Inside actvity, need not show progress bar on Notification");
                } else {
                    Log.e(TAG, "At background, show progress bar on Notification!");
                    showNotificationProgress(item);
                }
            }
        }

        public void showNotificationProgress(NotificationItem item) {
            Log.e(TAG, "Enter showNotificationProgress()");

            // Build the RemoteView object
            RemoteViews expandedView = new RemoteViews(Constants.THIS_PACKAGE_NAME,
                    R.layout.status_bar_ongoing_event_progress_bar);

            expandedView.setTextViewText(R.id.description, item.description);

            expandedView.setProgressBar(R.id.progress_bar, item.totalTotal, item.totalCurrent,
                    item.totalTotal == -1);

            String buffer = "";
            if (item.totalTotal <= 0) {
                buffer = "0%";
            } else {
                long progress = item.totalCurrent * 100 / item.totalTotal;
                // modified by amt_jiayuanyuan 2013-01-14 SWITCHUITWO-483 begin
                if(bufferSaved != progress)
                {
                	bufferSaved = progress;
                } else {
                	return ;
                }
                // modified by amt_jiayuanyuan 2013-01-14 SWITCHUITWO-483 end
                StringBuilder sb = new StringBuilder();
                sb.append(progress);
                sb.append('%');
                buffer = sb.toString();
            }

            expandedView.setTextViewText(R.id.progress_text, buffer);

            // Build the notification object
            if (item.direction == Constants.BACKUP_ACTION || item.direction == Constants.EXPORTBYACCOUNT_ACTION) {
                mNotification.icon = android.R.drawable.stat_sys_upload;
                expandedView.setImageViewResource(R.id.appIcon, android.R.drawable.stat_sys_upload);
            } else {
                mNotification.icon = android.R.drawable.stat_sys_download;
                expandedView.setImageViewResource(R.id.appIcon, android.R.drawable.stat_sys_download);
            }

            mNotification.tickerText = ""; //it will enable the download or upload animated image
            mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
            mNotification.contentView = expandedView;
            mNotificationResultActivity = NotificationResultActivity.getInstance();
            if (mNotificationResultActivity != null) {
                mNotificationResultActivity.updateActivity(Message.obtain(null, R.id.update_notification_activity, item));
            }
            Log.i(TAG, "Enter showNotificationProgress() mTickerText="+mTickerText);
            Intent intent = new Intent(Constants.ACTION_ONGOING);
            if (intent != null) {
                intent.setClassName(Constants.THIS_PACKAGE_NAME,
                        NotificationResultActivity.class.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.TOTAL, item.totalTotal);
                intent.putExtra(Constants.CURRENT, item.totalCurrent);
                intent.putExtra(Constants.POPUP_COMPLETE_INFO, mTickerText);
                intent.putExtra(Constants.ACTION_TYPE, item.direction);
                intent.putExtra(Constants.DESCRIPTION, item.description);
            }
            mNotification.contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
            mNotificationManager.notify(Constants.NOTIFICATION_ID, mNotification);        
        }

    }

    private boolean oldApkFileExist() {
        File innerFolder = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        if (!innerFolder.exists() || !innerFolder.isDirectory()) {
            return false;
        } else {
            File[] list = innerFolder.listFiles();
            if (list == null) {
                return false;
            } else {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getName().endsWith(".apk")) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    private boolean oldAltApkFileExist() {
        File outerFolder = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        if (!outerFolder.exists() || !outerFolder.isDirectory()) {
            return false;
        } else {
            File[] list = outerFolder.listFiles();
            if (list == null) {
                return false;
            } else {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getName().endsWith(".apk")) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    public String ignorePathPrefix(String path) {
        String showString = "";
        int index = -1;
        index = path.indexOf(mConstants.SDCARD_PREFIX);
        if (index != -1) {
            showString = path.substring(index + mConstants.SDCARD_PREFIX.length() - 1);
        } else {
    		if (mConstants.HAS_INTERNAL_SDCARD) {
        		index = path.indexOf(mConstants.ALT_SDCARD_PREFIX);
    			if (index != -1) {
    				showString = path.substring(index + mConstants.ALT_SDCARD_PREFIX.length() - 1);
    			} else {
    				showString = path;
    			}
			}
        }
        return showString;
    }
    
    private String getShowMsg(int msgPara, Object obj) {
        String tickerText = null;
        switch (msgPara) {
        case R.id.final_result:
            tickerText = getFinalResultMessage((HashMap<Integer, int[]>)obj);
            break;
        case R.id.cancel:
            tickerText = getString(R.string.cancel);
            break;
        case R.id.no_sdcard:
            tickerText = getString(R.string.no_sdcard);
            break;
        case R.id.read_sdcard_error:
            tickerText = getString(R.string.read_sdcard_error);
            break;
        case R.id.read_phone_db_error:
            tickerText = getString(R.string.read_phone_db_error);
            break;
        case R.id.sdcard_read_only:
            tickerText = getString(R.string.sdcard_readonly);
            break;
        case R.id.insufficient_space:
            tickerText = getString(R.string.insufficient_space, getString(mStorageType));
            break;
        case R.id.out_of_memory_error:
            tickerText = getString(R.string.out_of_memory_error);
            break;
        case R.id.write_sdcard_error:
            tickerText = getString(R.string.write_sdcard_error);
            break;
        case R.id.write_phone_db_error:
            tickerText = getString(R.string.write_phone_db_error);
            break;
        case R.id.empty_storage:
            tickerText = getEmptyStorageShortMsg(obj == null ? 0 : (Integer)obj); 
            break;
        case R.id.sdcard_file_contents_error:
            tickerText = getString(R.string.sdcard_file_contents_error);
            break;
        default:
            tickerText = getString(R.string.unknown_error);
            Log.d(TAG, "send notification with unkonwn error" );
        }
        return tickerText;
    }
    
    private String getActionString(int operationType) {
        String actionString = null;
        switch (operationType) {
        case Constants.BACKUP_ACTION:
            actionString = getString(R.string.backup);
            break;
        case Constants.RESTORE_ACTION:
            actionString = getString(R.string.restore);
            break;
        case Constants.IMPORT3RD_ACTION:
            actionString = getString(R.string.import3rd);
            break;
        case Constants.EXPORTBYACCOUNT_ACTION:
            actionString = getString(R.string.export);
            break;
        default:
            break;
        }
        return actionString;
    }
    
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
        case Constants.APP_ACTION:
            actionString = getString(R.string.app);
            break;
        case Constants.BOOKMARK_ACTION:
            actionString = getString(R.string.bookmark);
            break;
        default:
            break;
        }
        //return getString(R.string.no_content_found, getString(mStorageType), actionString);
        return getString(R.string.no_content_found, SdcardManager.hasInternalSdcard(this) ? getString(R.string.all_storage) : getString(R.string.sdcard), actionString);
    }
    
    private String getFinalResultMessage(HashMap<Integer, int[]> obj) {
        StringBuilder finalString = new StringBuilder();
        StringBuilder failedItemList = new StringBuilder();
        StringBuilder successItemList = new StringBuilder();
        boolean nextIsFirstSuccess = true;
        boolean nextIsFirstFailed = true;
        int[] tmp;

        String actionString = getActionString(mAction);
        String path = "";
        String from_or_to = "";
        String storageType = "";
        String isfile = "";
        switch (mAction) {
        case Constants.BACKUP_ACTION:
            path = "/" + mConstants.DEFAULT_FOLDER;
            from_or_to = getString(R.string.to);
            storageType = getString(mStorageType);
            break;
        case Constants.RESTORE_ACTION:
            path = "";
            from_or_to = "";
            storageType = "";
            break;
        case Constants.IMPORT3RD_ACTION:
            path = "";
            from_or_to = getString(R.string.from);
            storageType = getString(mStorageType);
            isfile =" " + getString(R.string.files);
            break;
        default:
            break;
        }
        
        if (null != (tmp = obj.get(Constants.INDEX_CONTACT))) {
            if (tmp[0] == R.id.success) {
                nextIsFirstSuccess = false;
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                nextIsFirstSuccess = false;
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                nextIsFirstFailed = false;
                failedItemList.append("- ");
                failedItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, getShowMsg(tmp[0], Constants.CONTACTS_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_SMS))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.sms) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.sms) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.sms) + isfile, getShowMsg(tmp[0], Constants.SMS_ACTION)));
            }
        }
        if (null != (tmp = obj.get(Constants.INDEX_QUICKNOTE))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, getShowMsg(tmp[0], Constants.QUICKNOTE_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_CALENDAR))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.calendar) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.calendar) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.calendar) + isfile, getShowMsg(tmp[0], Constants.CALENDAR_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_APP))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.app) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.app) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.app) + isfile, getShowMsg(tmp[0], Constants.APP_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_BOOKMARK))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, getShowMsg(tmp[0], Constants.BOOKMARK_ACTION)));
            } 
        }
        if (successItemList.length() != 0) {
            finalString.append(getString(R.string.final_success_result, actionString, from_or_to,
                    storageType, path, successItemList.toString()));
        }
        if (failedItemList.length() != 0) {
            if (successItemList.length() != 0) {
                finalString.append("\r\n");
            }
            finalString.append(getString(R.string.final_failed_result, actionString, failedItemList.toString()));
        }
        return finalString.toString();
    }
    
    public class MulPickResult extends AsyncTask<Intent, Void, Void> {

        private Context mContext;
        private int count = 0;

        public MulPickResult(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... params) {
            Intent data = params[0];
            // platform's multipicker  - contacts based
            ArrayList<Uri> selected_contact_uris = new ArrayList<Uri>();
            try {
                if(getMultiplePickResult(mContext, data, selected_contact_uris)) {
                    mUriArrayList = selected_contact_uris;
                    count = mUriArrayList.size();
                    mPickerLock = false;
                    Log.v(TAG, "will backup contacts : " + count);
                } else {
                    Log.v(TAG, "pick contacts error!!");
                }
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("MulPickResult", "Exception while pick contacts");
            } finally {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("MulPickResult", "Enter onPostExecute()");
            mPickerLock = false;
        }
    }

    
    public static boolean getMultiplePickResult(Context context, Intent data, ArrayList<Uri> selected_contact_uris) {

        Boolean isResultIncluded = data.getBooleanExtra(Constants.SELECTTION_RESULT_INCLUDED, false);
        if (!isResultIncluded) { //get uri via the multiple picker session id
            Log.v(TAG, "get uri via the multiple picker session id");
            final long sessionId = data.getLongExtra(Constants.SELECTION_RESULT_SESSION_ID, -1);
            if (sessionId == -1) {
                Log.e(TAG, "error in the sessionId: " + sessionId);
                return false;
            } else {
                final String[] LOOKUP_URI_PROJECTION = new String[] {
                      "data"
                };
                Uri sessionUri = ContentUris.withAppendedId(Uri.withAppendedPath(
                        Uri.parse("content://com.motorola.contacts.list.ContactMultiplePickerResult/results"),"session_id"),
                        sessionId);

                final Cursor cursor = context.getContentResolver().query(sessionUri, LOOKUP_URI_PROJECTION, null, null, null);
                try{
                    if (cursor == null || !cursor.moveToFirst()) {
                        Log.e(TAG, "error in reading the multiplepicker uri: " + sessionUri);
                    }

                    //IKTABLETMAIN-518 original code will skip the 1st item directly
                    for(;!cursor.isAfterLast(); cursor.moveToNext()) {
                        Uri lookup_uri = Uri.parse(cursor.getString(0));
                        selected_contact_uris.add(lookup_uri);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } else { //retrieve the lookups from the result directly
            Log.v(TAG, "get uri from multiple picker directly");
            ArrayList<Uri> uris = data.getParcelableArrayListExtra(Constants.SELECTED_CONTACTS);
            if (uris != null) {
                selected_contact_uris.addAll(uris);
            }
        }

        Log.v(TAG, "the return count = " + selected_contact_uris.size());
        return true;
    }
    
}
