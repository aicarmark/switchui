package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

//import com.motorola.calendar.vcalutils.vcalendar.VCalendarUtils;
import com.motorola.calendar.share.vcalendar.VCalUtils;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;

public abstract class BaseActivity extends ActivityUtility {

    protected String[] mBackupRestoreItems = new String[Constants.GENUS_NUM];
    protected int[] mItemNum;    
    protected boolean mIsContactsChecked = false;
    protected boolean mIsSmsChecked = false;
    protected boolean mIsQuickNoteChecked = false;
    protected boolean mIsCalendarChecked = false;
    protected boolean mIsAppChecked = false;
    protected boolean mIsBookmarkChecked = false;
    protected boolean mIsSingleFileChecked = false;
    protected boolean mIsMultiFileChecked = false;
    protected boolean[] accountCheckedFlags;
    
    protected boolean mIsLocalContactExisted = false;
    protected boolean mIsCCardContactExisted = false;
    protected boolean mIsGCardContactExisted = false;
    protected ArrayList<String> accountNames = null;
    protected ArrayList<String> checkedNames = new ArrayList<String>();
    protected ArrayList<Account> mAccounts = new ArrayList<Account>();
    protected String accountName = null;
    protected String accountType = null;
    protected boolean[] mCheckedFlags = null;
    protected int itemIndex = 0;
    protected String mCurrentDir;
    protected ArrayList<String> mSelectedPathList;
    protected boolean mIsAltSdcard = false;
    protected ListViewAdapter mListViewAdapter = null;
    protected ListView mListView;
    protected Button mOkButton;
    protected Button mBackButton;

    protected boolean mIsServiceEnd = false;

    private static final String TAG = "BaseActivity";
    private static final int DIALOG_PROGRESS_BAR = 0;
    private static final int DIALOG_EMPTY_STORAGE = 1;
    private static final int DIALOG_DELETE_OLD_BACKUP = 2;
    private static final int DIALOG_DIR_NOT_EXIST = 3;
    private static final int DIALOG_CANCEL_PROGRESS = 4;

    private volatile ActivityHandler mActivityHandler;

    private int mMsgLength = 0;
    private int mCurrentType = Constants.NO_ACTION;
    private ArrayList<Integer> mNeedReplaceList = null;
    private boolean mIsForeground = false;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mProgressCancelDialog;
    private AlertDialog mAlertDialog;
    private LayoutInflater mDialogInflater;
    private PowerManager mPowerManager;
    private boolean D = false;
    private boolean mIsCalculating = true;
    
    
    public void setCurrentDir(String dateString) {
        this.mCurrentDir = " /" + mConstants.DEFAULT_FOLDER + "/" + dateString;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");        
        mBackupDest = new Utility(this).getBackupDest();
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        if (SdcardManager.noSdcard(this)) {
            showMessage(R.id.no_sdcard, null);
            return;
        }
        mItemNum = new int[mBackupRestoreItems.length];
        mCheckedFlags = new boolean[mBackupRestoreItems.length];
        for (boolean checkFlag : mCheckedFlags) {
            checkFlag = false;
        }
        if (mBackupDest == Constants.INNER_STORAGE) {
            mStorageType = R.string.inner_storage;
        } else if (mBackupDest == Constants.OUTER_STORAGE) {
            mStorageType = R.string.outer_storage;
        }
        BackupRestoreService.setmStorageType(mStorageType);
        switch(mOpt){
        case Constants.BACKUP_ACTION:
            mBackupRestoreItems[0] = getString(R.string.contacts);
            mBackupRestoreItems[1] = getString(R.string.sms);
            mBackupRestoreItems[2] = getString(R.string.quicknote);
            mBackupRestoreItems[3] = getString(R.string.calendar);
            mBackupRestoreItems[4] = getString(R.string.installed_app);
            //mBackupRestoreItems[5] = getString(R.string.bookmark);
            new InitItemNum().execute();
            setContentView(R.layout.checklist);
            mListView = (ListView) findViewById(R.id.list);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                        View view, int position, long id) {
                    if (mItemNum[position] == 0 || mItemNum[position] == -1) {
                        mListView.setItemChecked(position, false);
                    } 
                    mCheckedFlags[(int) position] = mListView.isItemChecked(position);
                    setOkButtonEnable();
                    mListViewAdapter.notifyDataSetChanged();
                }
            });
            mListViewAdapter = new ListViewAdapter();
            mListView.setAdapter(mListViewAdapter);
            mListView.setItemsCanFocus(false);
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            mOkButton = (Button) findViewById(R.id.ok);
            mOkButton.setOnClickListener(mOkButtonListener);
            setOkButtonEnable();
            mBackButton = (Button) findViewById(R.id.back);
            mBackButton.setOnClickListener(mBackButtonListener);
            /*
            mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
                
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // TODO Auto-generated method stub
                    return false;
                }
                
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // TODO Auto-generated method stub
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.action_bar, menu);
                    return true;
                }
                
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    // TODO Auto-generated method stub
                    switch (item.getItemId()) {
                    case R.id.menu_ok:
                        clickOk();
                        break;

                    default:
                        break;
                    }
                    return true;
                }
                
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                        long id, boolean checked) {
                    // TODO Auto-generated method stub
                }
            });
            */
            
            mActivityHandler = new ActivityHandler();
            break;
        case Constants.RESTORE_ACTION:
            mBackupRestoreItems[0] = getString(R.string.contacts);
            mBackupRestoreItems[1] = getString(R.string.sms);
            mBackupRestoreItems[2] = getString(R.string.quicknote);
            mBackupRestoreItems[3] = getString(R.string.calendar);
            mBackupRestoreItems[4] = getString(R.string.app);
            //mBackupRestoreItems[5] = getString(R.string.bookmark);
            setContentView(R.layout.checklist);
            mListView = (ListView) findViewById(R.id.list);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                        View view, int position, long id) {
                    setOkButtonEnable();
                }
            });
            mListView.setAdapter(new ArrayAdapter<String>(this, R.layout.check_text_item, mBackupRestoreItems));
            mListView.setItemsCanFocus(false);
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            mOkButton = (Button) findViewById(R.id.ok);
            mOkButton.setOnClickListener(mOkButtonListener);
            setOkButtonEnable();
            mBackButton = (Button) findViewById(R.id.back);
            mBackButton.setOnClickListener(mBackButtonListener);
            /*
            mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
                
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // TODO Auto-generated method stub
                    return false;
                }
                
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // TODO Auto-generated method stub
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.action_bar, menu);
                    return true;
                }
                
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    // TODO Auto-generated method stub
                    switch (item.getItemId()) {
                    case R.id.menu_ok:
                        clickMenuOk();
                        break;

                    default:
                        break;
                    }
                    return true;
                }
                
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                        long id, boolean checked) {
                    // TODO Auto-generated method stub
                }
            });
            */

            mActivityHandler = new ActivityHandler();
            break;
        case Constants.IMPORT3RD_ACTION:
            mActivityHandler = new ActivityHandler();
            Intent intent = new Intent(this, com.motorola.filemanager.multiselect.FileManager.class);
            startActivityForResult(intent, Constants.IMPORT3RD_ACTION);
            break;
        case Constants.EXPORTBYACCOUNT_ACTION:
            mActivityHandler = new ActivityHandler();
            break;
        }
    }

    public void setOkButtonEnable(){
        if (null != mOkButton && null != mListView) {
            boolean isEnable = false;
            SparseBooleanArray boolArray = null;
            boolArray = mListView.getCheckedItemPositions();
            for (int i = 0; i < mListView.getCount(); i++) {
                if (boolArray.get(i)) {
                    isEnable = true;
                    break;
                }
            }
            mOkButton.setEnabled(isEnable);
        }
    }
    
    class ListViewAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;
        private CheckBox mCheckBox;
        private TextView mTextTitle;
        private TextView mTextSummary;
        private boolean isEnable;

        public ListViewAdapter() {
            mLayoutInflater =(LayoutInflater)getLayoutInflater();
        }

        public int getCount() {
            return mBackupRestoreItems.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

		public View getView(final int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.item_with_title_and_summary, parent, false);
            }
            mCheckBox = (CheckBox) view.findViewById(R.id.check_box);
            mTextTitle = (TextView) view.findViewById(R.id.title);
            mTextSummary = (TextView) view.findViewById(R.id.summary);                    
            mCheckBox.setChecked(mCheckedFlags[position]);
            mTextTitle.setText(mBackupRestoreItems[position]);
            mTextSummary.setText(getSummary(mItemNum[position], position));
            mCheckBox.setClickable(false);
            isEnable = true;
            if (!mIsCalculating && (mItemNum[position] == 0 || mItemNum[position] == -1)) {
                isEnable = false;
            }
            mTextSummary.setEnabled(isEnable);
            mTextTitle.setEnabled(isEnable);
            mCheckBox.setEnabled(isEnable);
            view.setEnabled(isEnable);
            return view;
        }
    }

    private String getSummary(int item_num, int index) {
        String tmpString = null;
        if (mIsCalculating) {
            return getString(R.string.is_calculating);
        }
        switch (index) {
        case Constants.INDEX_CONTACT:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_contact));
            } else {
                tmpString = getString(R.string.items_num_show, 
                        Integer.toString(mItemNum[Constants.INDEX_CONTACT]),
                        getString(R.string.summary_contact));
            }
            break;
        case Constants.INDEX_SMS:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_sms));
            } else {
                tmpString = getString(R.string.items_num_show,
                        Integer.toString(mItemNum[Constants.INDEX_SMS]),
                        getString(R.string.summary_sms));
            }
            break;
        case Constants.INDEX_QUICKNOTE:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_quicknote));
            } else {
                tmpString = getString(R.string.items_num_show,
                        Integer.toString(mItemNum[Constants.INDEX_QUICKNOTE]),
                        getString(R.string.summary_quicknote));
            }
            break;
        case Constants.INDEX_CALENDAR:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_calendar));
            } else {
                tmpString = getString(R.string.items_num_show,
                        Integer.toString(mItemNum[Constants.INDEX_CALENDAR]),
                        getString(R.string.summary_calendar));
            }
            break;
        case Constants.INDEX_BOOKMARK:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_bookmark));
            } else {
                tmpString = getString(R.string.items_num_show,
                        Integer.toString(mItemNum[Constants.INDEX_BOOKMARK]),
                        getString(R.string.summary_bookmark));
            }
            break;
        case Constants.INDEX_APP:
            if (item_num == -1 || item_num == 0) {
                tmpString = getString(R.string.no_item,
                        getString(R.string.summary_app));
            } else {
                tmpString = getString(R.string.items_num_show,
                        Integer.toString(mItemNum[Constants.INDEX_APP]),
                        getString(R.string.summary_app));
            }
            break;
        default:
            Log.e(TAG, "getItemNumStr should not be here");
            break;
        }
        return tmpString;
    }
    
    private void initItemNum() {
        // query Contacts
        String selection = RawContacts.ACCOUNT_NAME + "='"
                + Constants.LOCAL_ACCOUNT_NAME + "' AND " + RawContacts.DELETED + "=0";
        Cursor cursor = getContentResolver().query(
                RawContacts.CONTENT_URI, new String[]{RawContacts.CONTACT_ID}, selection, null, null);
        if (cursor == null) {
            mItemNum[Constants.INDEX_CONTACT] = -1;
        } else {
            HashSet<Long> contact_id_set = new HashSet<Long>();
            int col;
            while (cursor.moveToNext()){
                col = cursor.getColumnIndex(RawContacts.CONTACT_ID);
                if (col != -1) {
                    contact_id_set.add(cursor.getLong(col));
                }
            }
            mItemNum[Constants.INDEX_CONTACT] = contact_id_set.size();
            cursor.close();
            cursor = null;
        }

        // query SMS & MMS
        selection = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_INBOX + " or " + Sms.TYPE
                + "=" + Sms.MESSAGE_TYPE_SENT;
        cursor = getApplicationContext().getContentResolver().query(
                Sms.CONTENT_URI, null, selection, null, null);
        if (cursor == null) {
            mItemNum[Constants.INDEX_SMS] = -1;
        } else {
            mItemNum[Constants.INDEX_SMS] = cursor.getCount();
            cursor.close();
            cursor = null;
        }
        
        selection = "(" + MMSConstants.MSG_BOX + "=" + Sms.MESSAGE_TYPE_INBOX 
            + " or " + MMSConstants.MSG_BOX + "=" + Sms.MESSAGE_TYPE_SENT + ")"
            + " and " + MMSConstants.MMS_TYPE + "<=" + MMSConstants.MMS_TYPE_REPORT;
        cursor = getApplicationContext().getContentResolver().query(
                Mms.CONTENT_URI, null, selection, null, null);
        if (cursor != null) {
            mItemNum[Constants.INDEX_SMS] += cursor.getCount();
            cursor.close();
            cursor = null;
        }

        // query QuickNote
        cursor = QuickNoteBackupRestore.queryQuickNote(getApplicationContext().getContentResolver());
        if (cursor == null) {
            mItemNum[Constants.INDEX_QUICKNOTE] = -1;
        } else {
            mItemNum[Constants.INDEX_QUICKNOTE] = cursor.getCount();
            cursor.close();
            cursor = null;
        }
        
        // query Calendar
        cursor = new VCalUtils().queryCal(this);
        if (cursor == null) {
            mItemNum[Constants.INDEX_CALENDAR] = -1;
        } else {
            mItemNum[Constants.INDEX_CALENDAR] = cursor.getCount();
            cursor.close();
            cursor = null;
        }
        
        //query App
        List<ApplicationInfo> ai= getPackageManager().getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES);
        File folder = new File(Constants.PREINSTALLFILE_PATH);
        File[] fileList = null;
        boolean isPreInstall = false;
        if (folder != null && folder.isDirectory()) {
            fileList = folder.listFiles();
        }
        for (int i = 0; i < ai.size(); i++) {
            isPreInstall = false;
            if ((ai.get(i).flags&ApplicationInfo.FLAG_SYSTEM)==0&&  
                    (ai.get(i).flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)==0) {
                if (fileList != null && fileList.length != 0) {
                    for (int j = 0; j < fileList.length; j++) {
                        if (fileList[j].getName().equals(ai.get(i).packageName + ".md5")) {
                            isPreInstall = true;
                            break;
                        }
                    }
                }
                if (!isPreInstall) {
                    mItemNum[Constants.INDEX_APP]++;
                }
            }
        }
        
        //query bookmark
        //mItemNum[Constants.INDEX_BOOKMARK] = new BookmarkBackupRestore(getApplicationContext())
        //                                            .queryNumOfBookmarkWithoutFolder();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged...");
        super.onConfigurationChanged(newConfig);
    }

    public void sendMsgtoActivity(Message message) {
        Message msg = mActivityHandler.obtainMessage(message.what, message.arg1, message.arg2, message.obj);
        mActivityHandler.sendMessage(msg);
    }

    protected final class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message == null) {
                Log.e(TAG, "In ActivityHandler, message is null.");
                return;
            }

            switch (message.what) {
            case R.id.message_length:
                mMsgLength = message.arg1;
                Log.d(TAG, "Total message count is " + mMsgLength);
                mCurrentType = message.arg2;
                showDialog(DIALOG_PROGRESS_BAR);
                if (mProgressDialog != null) {
                    Log.v(TAG, "mProgressDialog is NOT null");
                    mProgressDialog.setMax(mMsgLength);
                    mProgressDialog.setProgress(0);
                } else {
                    Log.e(TAG, "mProgressDialog is null");
                }
                break;

            case R.id.under_progress:
                Log.d(TAG, "under_progress");
                if (mProgressDialog != null) {
                    mProgressDialog.setProgress(message.arg1);
                    String displayString = "";
                    if (message.obj != null) {
                        displayString = message.obj.toString();
                    }
                    Configuration cf= getResources().getConfiguration();
                    int ori = cf.orientation ;
                    if(ori == Configuration.ORIENTATION_LANDSCAPE){
                        int index = displayString.indexOf('\n');
                        if (index != -1) {
                            displayString = displayString.substring(0, index);
                        }
                    }
                    mProgressDialog.setMessage(displayString);
                }
                break;

            case R.id.confirmation:
                Log.d(TAG, "Show confirmation dialog");
                mCurrentType = message.arg2;
                if (message.arg1 == R.id.empty_storage) {
                    showDialog(DIALOG_EMPTY_STORAGE);
                } else if (message.arg1 == R.id.exist_old_backup_file) {
                    mNeedReplaceList = (ArrayList<Integer>)message.obj;
                    if( mNeedReplaceList != null && mNeedReplaceList.size() != 0) showDialog(DIALOG_DELETE_OLD_BACKUP);
                } else if (message.arg1 == R.id.directory_not_exist) {
                       showDialog(DIALOG_DIR_NOT_EXIST);
                }
                break;

            case R.id.status:
                Log.d(TAG, "Enter R.id.status, result=" + message.arg1);
                mCurrentType = message.arg2;
                if (mProgressDialog != null) { // remove progress dialog first if it still exists
                    removeDialog(DIALOG_PROGRESS_BAR);
                    mProgressDialog = null;
                    Log.d(TAG, "R.id.status, set mProgressDialog=null and removed Progress bar.");
                }
                mIsServiceEnd = true;
                if (message.arg1 == R.id.remove_dialog) { break; }  // this is to remove progress bar with no message sent to user
                showMessage(message.arg1, message.obj);
                break;

            default:
                Log.e(TAG, "unknown operation return.");
                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_PROGRESS_BAR) {
            Log.v(TAG, "enter DIALOG_PROGRESS_BAR case");
            mProgressDialog = new ProgressDialog(this, Constants.DIALOG_THEME);
            if (mProgressDialog == null) {
                Log.v(TAG, "new mProgressDialog returns NULL!!!");
                return null;
            }

            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setTitle(getString(R.string.progressbar_title));
            mProgressDialog.setMessage(getProgressMessage(mCurrentType));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            mProgressDialog.setOnCancelListener(mCancelListener);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            stopService(new Intent(BaseActivity.this, BackupRestoreService.class));
                            removeDialog(DIALOG_PROGRESS_BAR);
                            mProgressDialog = null;
                            showDialog(DIALOG_CANCEL_PROGRESS);
                        }
                    });
            return (Dialog) mProgressDialog;
        } 
        else if (id == DIALOG_DELETE_OLD_BACKUP) {
            Log.v(TAG, "enter DIALOG_DELETE_OLD_BACKUP case");
            mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME).setIcon(R.drawable.sd_card)
                .setMessage(getString(R.string.replace_old_backup_file, getReplaceItemString(mNeedReplaceList)))
                .setPositiveButton(getString(R.string.replace),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            removeDialog(DIALOG_DELETE_OLD_BACKUP);
                            mProgressDialog = null;
                            startBackupRestoreService(Constants.DEL_YES);
                        }
                    })
                .setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            removeDialog(DIALOG_DELETE_OLD_BACKUP);
                            mProgressDialog = null;
                        }
                    }).setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            removeDialog(DIALOG_DELETE_OLD_BACKUP);
                            mProgressDialog = null;
                            }
                    }).create();
            mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            return mAlertDialog;
        } else if (id == DIALOG_DIR_NOT_EXIST) {
            Log.v(TAG, "enter DIC_NOT_EXIST case");
            mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME)
                                .setMessage(getString(R.string.dir_not_exist, mConstants.DEFAULT_FOLDER))
                                .setPositiveButton(getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {                            
                                public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                removeDialog(DIALOG_DIR_NOT_EXIST);
                                }
                            }).create();
            mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            return mAlertDialog;
        } else if (id == DIALOG_CANCEL_PROGRESS) {
            Log.v(TAG, "enter DIALOG_CANCEL_PROGRESS");
            if (!mIsServiceEnd) {
                Log.d(TAG, "show cancel dialog while serivce running.");
                mProgressCancelDialog = new ProgressDialog(this, Constants.DIALOG_THEME);
                mProgressCancelDialog.setCancelable(false);
                mProgressCancelDialog.setTitle(getString(R.string.progressbar_title));
                mProgressCancelDialog.setMessage(getString(R.string.operation_is_cancelling));
                mProgressCancelDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                return (Dialog)mProgressCancelDialog;
            } else {
                Log.e(TAG, "don't show cancel dialog while serivce ended.");
                return null;
            }            
        } else {
            Log.v(TAG, "unknown dialog type");
            return null;
        }
    }

    public boolean getForegroundStatus() {
        return mIsForeground;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        mIsForeground = true;
        if (mIsServiceEnd == false) {
            NotificationManager notMgr = (NotificationManager)getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notMgr != null) notMgr.cancel(1);
        } 
    }

    private class CancelListener
        implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
        public void onClick(DialogInterface dialog, int which) {
            Log.d(TAG, "CancelListener onClick()");
            stopService(new Intent(BaseActivity.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }

        public void onCancel(DialogInterface dialog) {
            Log.d(TAG, "CancelListener onCancel()");
            stopService(new Intent(BaseActivity.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }

        public void onDismiss(DialogInterface dialog) {
            Log.d(TAG, "CancelListener onDismiss()");
            stopService(new Intent(BaseActivity.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }
    }

    private CancelListener mCancelListener = new CancelListener();
    
    public void clickOk() {
          Log.v(TAG, "Click OK button");
          if (SdcardManager.noSdcard(this)) {
              showMessage(R.id.no_sdcard, null);
              return;
          }
          SparseBooleanArray boolArray = null;
          switch(mOpt){
          case 1:
              // for moto default backup              
              boolArray = mListView.getCheckedItemPositions(); 
              if ( null == boolArray )  return;
              mIsContactsChecked = boolArray.get(Constants.INDEX_CONTACT);
              mIsSmsChecked = boolArray.get(Constants.INDEX_SMS);
              mIsQuickNoteChecked = boolArray.get(Constants.INDEX_QUICKNOTE);
              mIsCalendarChecked = boolArray.get(Constants.INDEX_CALENDAR);
              mIsAppChecked = boolArray.get(Constants.INDEX_APP);
              mIsBookmarkChecked = boolArray.get(Constants.INDEX_BOOKMARK);

                // if none is selected, OK button has no response
                if (!mIsContactsChecked && !mIsSmsChecked && !mIsQuickNoteChecked 
                        && !mIsCalendarChecked && !mIsAppChecked && !mIsBookmarkChecked) {
                    Log.v(TAG, "No items checked.");
                    return;
                }
                if (isMemoryEncryptionOn()) {
                    mAlertDialog = new AlertDialog.Builder(BaseActivity.this, Constants.DIALOG_THEME)
                            .setMessage(getString(R.string.encryption_is_enable))
                            .setPositiveButton(getString(R.string.setting), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // TODO Auto-generated method stub
                                    Intent intent = new Intent();
                                    intent.setClassName("com.motorola.android.encryption.settings", 
                                            "com.motorola.android.encryption.settings.EncryptionUserActivity");
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(getString(R.string.proceed), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // TODO Auto-generated method stub
                                    startBackupRestoreService(Constants.DEL_UNSET);
                                }
                            })
                            .create();
                    mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    mAlertDialog.show();
                } else {
                    startBackupRestoreService(Constants.DEL_UNSET);
                }
                break;
            case 2:
                // for moto default restore
                boolArray = mListView.getCheckedItemPositions();
                if ( null == boolArray )  return;                
                mIsContactsChecked = boolArray.get(Constants.INDEX_CONTACT);
                mIsSmsChecked = boolArray.get(Constants.INDEX_SMS);
                mIsQuickNoteChecked = boolArray.get(Constants.INDEX_QUICKNOTE);
                mIsCalendarChecked = boolArray.get(Constants.INDEX_CALENDAR);
                mIsAppChecked = boolArray.get(Constants.INDEX_APP);
                mIsBookmarkChecked = boolArray.get(Constants.INDEX_BOOKMARK);

                // if none is selected, OK button has no response
                if (!mIsContactsChecked && !mIsSmsChecked && !mIsQuickNoteChecked 
                        && !mIsCalendarChecked && !mIsAppChecked && !mIsBookmarkChecked) {
                    Log.v(TAG, "No items checked.");
                    return;
                }
                startBackupRestoreService(Constants.DEL_UNSET);
                break;
            case 3:
                break;
            }
        }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.IMPORT3RD_ACTION) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    mSelectedPathList = bundle.getStringArrayList("SELECTED_FILES_INFO");
                    if (mSelectedPathList != null
                            && mSelectedPathList.size() != 0) {
                        String path = mSelectedPathList.get(0);
						int isAltSdcard = SdcardManager.isAltSdcard(this, path);
						if (isAltSdcard == SdcardManager.INNER) {
							mIsAltSdcard = false;
						} else if (isAltSdcard == SdcardManager.OUTTER) {
							mIsAltSdcard = true;
						}
                    }
                }
                showSelectAccountDialogAndStartService();
            }
        }
    }
    
    protected void showSelectAccountDialogAndStartService() {
        mAccounts = Utility.getAccount(this);
        String dir;
        String fileName;
        if (mSelectedPathList != null) {
            if (Utility.hasVcardFileInPathList(mSelectedPathList)) {
                if (mAccounts.size() == 1) {
                    accountName = mAccounts.get(0).name;
                    accountType = mAccounts.get(0).type;
                    startBackupRestoreService(Constants.DEL_UNSET);
                    return;
                } else {
                    selectAccountDialog();
                    return;
                }
            }
        }
        startBackupRestoreService(Constants.DEL_UNSET);
    }
    
    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        
        public void onClick(DialogInterface dialog, int which) {
            accountName = mAccounts.get(which).name;
            accountType = mAccounts.get(which).type;
            dialog.dismiss();
            startBackupRestoreService(Constants.DEL_UNSET);
        }
    };
    
    protected void selectAccountDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, Constants.DIALOG_THEME);
        Context dialogContext = new ContextThemeWrapper(this, android.R.style.Theme_Holo);
        mDialogInflater = (LayoutInflater)dialogContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ArrayAdapter<Account> accountAdapter = new ArrayAdapter<Account>(this,
                android.R.layout.simple_list_item_2, mAccounts){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {                    
                    convertView = mDialogInflater.inflate(android.R.layout.simple_list_item_2,
                            parent, false);
                }

                final TextView text1 = (TextView)convertView.findViewById(android.R.id.text1);
                final TextView text2 = (TextView)convertView.findViewById(android.R.id.text2);

                final Account account = this.getItem(position);

                text1.setText(Utility.getAccName(BaseActivity.this, account.name));
                text2.setText(Utility.getAccType(BaseActivity.this, account.type));

                return convertView;
            }
        };
        builder.setTitle(R.string.select_account);
        builder.setSingleChoiceItems(accountAdapter, 0, clickListener);
        builder.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                // TODO Auto-generated method stub
                Import3rd.getInstance().finish();
            }
        });
        mAlertDialog = builder.create();
        mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mAlertDialog.show();
    }
    
    protected OnClickListener mOkButtonListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            clickOk();
        }
    };
    
    
    protected OnClickListener mBackButtonListener = new OnClickListener() {
        public void onClick(View v) {
            Log.v(TAG, "Click BACK button");
            finish();
        }
    };

    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        if (mPowerManager != null && mPowerManager.isScreenOn()) {
            mIsForeground = false;
        }
        if (!mIsForeground) {
            BackupRestoreService brService = BackupRestoreService.getInstance();
            if (brService != null) {
                if (brService.getIsProcessing()) {
                    NotificationItem item = brService.getCurrentItem();
                    if (item != null) {
                        brService.getNotificationAgent().showNotificationProgress(item);
                        finish();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    protected String getShowMsg(int msgPara, Object obj) {
        String showMsg = "";
        switch (msgPara) {
        case R.id.final_result:
            showMsg = getFinalResultMessage((HashMap<Integer, int[]>) obj);
            break;
        case R.id.no_sdcard:
            showMsg = getString(R.string.no_sdcard);
            break;
        case R.id.read_sdcard_error:
            showMsg = getString(R.string.read_sdcard_error);
            break;
        case R.id.read_phone_db_error:
            showMsg = getString(R.string.read_phone_db_error);
            break;
        case R.id.write_sdcard_error:
            showMsg = getString(R.string.write_sdcard_error);
            break;
        case R.id.write_phone_db_error:
            showMsg = getString(R.string.write_phone_db_error);
            break;
        case R.id.sdcard_read_only:
            showMsg = getString(R.string.sdcard_readonly);
            break;
        case R.id.insufficient_space:
            showMsg = getInsufficientSpaceMsg();
            break;
        case R.id.empty_storage:
            showMsg = getEmptyStorageShortMsg(obj == null ? 0 : (Integer)obj);
            break;
        case R.id.cancel:
            showMsg = getString(R.string.cancel);
            break;
        case R.id.copy_files_failed:
            showMsg = getString(R.string.copy_files_failed);
            break;
        case R.id.sdcard_file_contents_error:
            showMsg = getString(R.string.sdcard_file_contents_error);
            break;
        case R.id.out_of_memory_error:
            showMsg = getString(R.string.out_of_memory_error);
            break;
        default:
            showMsg = getString(R.string.unknown_error);
            break;
        }
        return showMsg;
    }
    
    private void showMessage(int msgPara, Object obj) {
        Log.v(TAG, "Enter showMessage()");
        String showMsg = "";
        showMsg = getShowMsg(msgPara, obj);
        showMessage(showMsg);
    }

    private void showMessage(String msg) {
        if (mProgressCancelDialog != null) {
            Log.v("mProgressCancelDialog", "mProgressCancelDialog is not null");
            removeDialog(DIALOG_CANCEL_PROGRESS);
            mProgressCancelDialog = null;
        }
        mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME).setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        finish();
                    }
                }).create();
        mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mAlertDialog.show();
    }

    protected String getReplaceItemString(ArrayList<Integer> needReplacList){
        if (needReplacList == null || needReplacList.size() == 0) {
            return "";
        }
        String cr = "";
        StringBuilder content = new StringBuilder();
        for (Integer action : needReplacList) {
            if (content.equals("")) {
                cr = "";
            } else {
                cr = "\r\n";
            }
            switch (action) {
            case Constants.CONTACTS_ACTION:
                content.append(cr).append("- ").append(getString(R.string.contacts));
                break;
            case Constants.SMS_ACTION:
                content.append(cr).append("- ").append(getString(R.string.sms));
                break;
            case Constants.QUICKNOTE_ACTION:
                content.append(cr).append("- ").append(getString(R.string.quicknote));
                break;
            case Constants.CALENDAR_ACTION:
                content.append(cr).append("- ").append(getString(R.string.calendar));
                break;
            case Constants.APP_ACTION:
                content.append(cr).append("- ").append(getString(R.string.app));
                break;    
            case Constants.BOOKMARK_ACTION:
                content.append(cr).append("- ").append(getString(R.string.bookmark));
                break;
            default:
                Log.e("getReplaceItemString()", "should not get here");
                break;
            }
        }
        return content.toString();
    }
    
    protected String getTypeName(int type) {
        String content = "";
        if (type == Constants.CONTACTS_ACTION) {
            content = getString(R.string.contacts);
        } else if (type == Constants.SMS_ACTION) {
            content = getString(R.string.sms);
        } else if (type == Constants.QUICKNOTE_ACTION) {
            content = getString(R.string.quicknote);
        } else if (type == Constants.CALENDAR_ACTION) {
            content = getString(R.string.calendar);
        } else if (type == Constants.APP_ACTION) {
            content = getString(R.string.app);
        } else if (type == Constants.BOOKMARK_ACTION) {
            content = getString(R.string.bookmark);
        } else {
            Log.e(TAG, "Wrong action type in getTypeName : " + type);
        }
        return content;
    }
    
    protected String getFileName(int type) {
        String content = "";
        if (type == Constants.CONTACTS_ACTION) {
            content = "/" + mConstants.DEFAULT_FOLDER+"/"+Constants.CONTACTS_FILE_NAME;
        } else if (type == Constants.SMS_ACTION) {
            content = "/" + mConstants.DEFAULT_FOLDER+"/"+Constants.SMS_FILE_NAME;
        } else if (type == Constants.LOCAL_ACTION
                    || type == Constants.CCARD_ACTION
                    || type == Constants.GCARD_ACTION
                    || type == Constants.GACCOUNT_ACTION
                    || type == Constants.EACCOUNT_ACTION
                    ) {
            content = mCurrentDir;
        } else {
            Log.e(TAG, "Wrong action type in getTypeName : " + type);
        }
        return content;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }
    
    private boolean isMemoryEncryptionOn(){
        //Memory
        String ON = "1";
        String memoryProps = android.os.SystemProperties.get("persist.sys.mot.encrypt.mmc", "");
        if (memoryProps.length() > 0 && memoryProps.startsWith(ON)) {
            return true;  //memory encryption set to ON
        }
        return false;
    }
    
    protected class InitItemNum extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            initItemNum();
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            Log.d("InitItemNum", "Enter onPostExecute()");
            mIsCalculating = false;
            if (mListViewAdapter != null) {
                mListViewAdapter.notifyDataSetChanged();
            }
            
        }
        
    }
    
    abstract protected void startBackupRestoreService(int delOption);

    abstract protected String getProgressMessage(int type);

    abstract protected String getEmptyStorageShortMsg(int type);

    abstract protected String getInsufficientSpaceMsg();

}
