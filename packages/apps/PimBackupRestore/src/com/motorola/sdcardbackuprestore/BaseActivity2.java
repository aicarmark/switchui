package com.motorola.sdcardbackuprestore;

import java.util.ArrayList;
import java.util.HashMap;

import android.R.integer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.Toast;

public abstract class BaseActivity2 extends ActivityUtility {

    protected boolean mIsContactsChecked = false;
    protected boolean mIsSmsChecked = false;
    protected boolean mIsFileChecked = false;
    protected boolean mIsFolderChecked = false;
    protected String[] checkedNames = null;
    protected String mCurrentDir;
    protected String accountName = null;
    protected String accountType = null;
    protected Uri mSpecificVcardUri = null;
    protected boolean mIsAltSdcard = false;
    protected ArrayList<String> mSelectedPathList;
    protected ArrayList<Account> mAccounts = new ArrayList<Account>();
    protected ArrayList<Integer> mNeedReplaceList = null;
    protected ArrayList<Uri> mUriList = null;
    protected boolean mSpeContacts = false;
    protected boolean mClickVcfFile = false;
    private LayoutInflater mDialogInflater;
    
    private static final String TAG = "BaseActivity II";
    protected static final int DIALOG_PROGRESS_BAR = 0;
    protected static final int DIALOG_EMPTY_STORAGE = 1;
    protected static final int DIALOG_DELETE_OLD_BACKUP = 2;
    protected static final int DIALOG_DIR_NOT_EXIST = 3;
    protected static final int DIALOG_CANCEL_PROGRESS = 4;
    protected static final int DIALOG_PREPARE = 5;
    private static final int CONTACTS_INDEX = 0; 
    private static final int SMS_INDEX = 1; 
    
    private volatile ActivityHandler mActivityHandler;

    private int mMsgLength = 0;
    private int mCurrentType = Constants.NO_ACTION;
    private boolean mIsForeground = false;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mProgressCancelDialog;
    private ProgressDialog mProgressPrepareDialog;
    private AlertDialog mAlertDialog;
    private boolean mIsServiceEnd = false;
    private PowerManager mPowerManager;
    private boolean D = false;
    private SharedPreferences mCaution = null;
    
    public void setCurrentDir(String dateString) {
        this.mCurrentDir = " /" + mConstants.DEFAULT_FOLDER + "/" + dateString;
    }

    public void showCautionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, Constants.DIALOG_THEME);
        final SharedPreferences caution = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_WORLD_READABLE);
        View contentView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.caution_dialog, null, false);
        final CheckBox cb = (CheckBox)contentView.findViewById(R.id.caution_marker);
        builder.setTitle(R.string.caution);
        builder.setView(contentView);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (cb.isChecked()) {
                    SharedPreferences.Editor editor = caution.edit();
                    editor.putBoolean(Constants.NOT_SHOW_ANYMORE, true);
                    editor.commit();
                }
                startBackupRestoreService(Constants.DEL_UNSET);
            }
        });
        AlertDialog ad = builder.create();
        ad.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ad.setCanceledOnTouchOutside(false);
        ad.setOnKeyListener(new DialogInterface.OnKeyListener() {
            
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    break;
                default:
                    return false;
                }
                return true;
            }
        });
        ad.show();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() BaseActivity2 ");
        mBackupDest = new Utility(this).getBackupDest();
        mCaution = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_WORLD_READABLE);
        if (mBackupDest == Constants.INNER_STORAGE) {
            mStorageType = R.string.inner_storage;
        } else if (mBackupDest == Constants.OUTER_STORAGE) {
            mStorageType = R.string.outer_storage;
        }
        BackupRestoreService.setmStorageType(mStorageType);        
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        if (SdcardManager.noSdcard(this)) {
            showMessage(R.id.no_sdcard, null);
            return;
        }
        switch(mOpt){ 
        case Constants.BACKUP_ACTION:
            if (!mIsContactsChecked && !mIsSmsChecked) {
                Log.v(TAG, "From other app but not define neither Contacts nor SMS.");
                finish();
                return;
            }
            mActivityHandler = new ActivityHandler();
            if (isMemoryEncryptionOn()) {
                mAlertDialog = new AlertDialog.Builder(BaseActivity2.this, Constants.DIALOG_THEME)
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
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {                   
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .create();
                mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                mAlertDialog.show();
                } else {
                    if (mConstants.HAS_INTERNAL_SDCARD && !SdcardManager.isSdcardMounted(this, true)) {
                        if (Integer.parseInt(getString(R.string.can_phone_sdcard_erase)) == 1) {
                            if (!mCaution.getBoolean(Constants.NOT_SHOW_ANYMORE, false)) {
                                showCautionDialog();
                            } else {
                                startBackupRestoreService(Constants.DEL_UNSET);
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.sdcard_is_unavailable), Toast.LENGTH_LONG).show();
                            startBackupRestoreService(Constants.DEL_UNSET);
                        }
                    } else {
                        startBackupRestoreService(Constants.DEL_UNSET);
                    }
                }
            break;
        case Constants.RESTORE_ACTION:
            if (!mIsContactsChecked && !mIsSmsChecked) {
                Log.v(TAG, "From other app but not define neither Contacts nor SMS.");
                finish();
                return;
            }
            mActivityHandler = new ActivityHandler();
            startBackupRestoreService(Constants.DEL_UNSET);
            break;
        case Constants.IMPORT3RD_ACTION:
            // to select files
            if (!mIsContactsChecked && !mIsSmsChecked) {
                Log.v(TAG, "From other app but not define neither Contacts nor SMS.");
                finish();
                return;
            }
            if (mSpecificVcardUri != null && !mSpecificVcardUri.getScheme().equals("file")) {
                showDialogNotSupportUnsavedVCard();
                return;
            }
            mActivityHandler = new ActivityHandler();
            if (mClickVcfFile) {
                showSelectAccountDialogAndStartService();
            } else {
                Intent intent = new Intent(this, com.motorola.filemanager.multiselect.FileManager.class);
                Bundle bundle = new Bundle();
                ArrayList<String> filter = new ArrayList<String>();
                if (mIsContactsChecked) {
                    filter.add(Constants.vcardExtern);
                } else if (mIsSmsChecked) {
                    filter.add(Constants.vmgExtern);
                }
                bundle.putStringArrayList(Constants.FILE_MANAGER_FILTER, filter);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.IMPORT3RD_ACTION);
            }
            break;
        case Constants.EXPORTBYACCOUNT_ACTION:
            //for exporting by account
            if (0 == checkedNames.length) {
                Log.v(TAG, "Neither Account is selected.");
                finish();
                return;
            }
            mActivityHandler = new ActivityHandler();
            if (mConstants.HAS_INTERNAL_SDCARD && !SdcardManager.isSdcardMounted(this, true)) {
                if (Integer.parseInt(getString(R.string.can_phone_sdcard_erase)) == 1) {
                    if (!mCaution.getBoolean(Constants.NOT_SHOW_ANYMORE, false)) {
                        showCautionDialog();
                    } else {
                        startBackupRestoreService(Constants.DEL_UNSET);
                    }
                } else {
                    Toast.makeText(this, getString(R.string.sdcard_is_unavailable), Toast.LENGTH_LONG).show();
                    startBackupRestoreService(Constants.DEL_UNSET);
                }
            } else {
                startBackupRestoreService(Constants.DEL_UNSET);
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Enter onActivityResult()");
        // TODO Auto-generated method stub
        switch (requestCode) {
        case Constants.IMPORT3RD_ACTION:
            if (resultCode == RESULT_OK && data != null) {
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
            break;
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

                text1.setText(Utility.getAccName(BaseActivity2.this, account.name));
                text2.setText(Utility.getAccType(BaseActivity2.this, account.type));

                return convertView;
            }
        };
        builder.setTitle(R.string.select_account);
        builder.setSingleChoiceItems(accountAdapter, 0, clickListener);
        builder.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                // TODO Auto-generated method stub
                if (Import3rd2.getInstance() != null) {
                    Import3rd2.getInstance().finish();
                }
            }
        });
        mAlertDialog = builder.create();
        mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mAlertDialog.show();
    }
    
    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            accountName = mAccounts.get(which).name;
            accountType = mAccounts.get(which).type;
            dialog.dismiss();
            startBackupRestoreService(Constants.DEL_UNSET);
        }
    };
    
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

            case R.id.service_busy:
                Log.d(TAG, "Enter R.id.service_busy, close the transparent activity");
                finish();
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
            if (mProgressPrepareDialog != null) {
                removeDialog(DIALOG_PREPARE);
                mProgressPrepareDialog = null;
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
                            stopService(new Intent(BaseActivity2.this, BackupRestoreService.class));
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
                            startBackupRestoreService(Constants.DEL_CANCEL);
                            // A16516 add
                            finish();
                        }
                    })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {                   
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    }).create();
            mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            return mAlertDialog;
        } else if (id == DIALOG_EMPTY_STORAGE) {
            Log.v(TAG, "enter DIALOG_EMPTY_STORAGE case");
            if (mIsContactsChecked
                    && mIsSmsChecked
                    && mCurrentType != (Constants.CONTACTS_ACTION + Constants.SMS_ACTION)) {
                // show dialog with two buttons, ok and cancel
                mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME).setMessage(getEmptyStorageShortMsg(mCurrentType))
                        .setPositiveButton(getString(android.R.string.yes),    new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,    int whichButton) {
                                        removeDialog(DIALOG_EMPTY_STORAGE);
                                        if (mCurrentType == Constants.CONTACTS_ACTION) {
                                            mIsContactsChecked = false;
                                        } else if (mCurrentType == Constants.SMS_ACTION) {
                                            mIsSmsChecked = false;
                                        }
                                        startBackupRestoreService(Constants.DEL_UNSET);
                                    }
                                })
                        .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,    int whichButton) {
                                        removeDialog(DIALOG_EMPTY_STORAGE);
                                        // A16516 add
                                        finish();
                                        // A16516 end

                                    }
                                })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    public void onCancel(DialogInterface dialog) {
                                        finish();
                                    }
                                }).create();
                mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                return mAlertDialog;
            } else { // show dialog with only one ok button
                mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME).setMessage(
                        getEmptyStorageShortMsg(mCurrentType))
                        .setPositiveButton(getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        removeDialog(DIALOG_EMPTY_STORAGE);
                                        // A16516 add
                                        finish();
                                        // A16516 end
                                    }
                                }).setOnCancelListener(
                                new DialogInterface.OnCancelListener() {
                                    public void onCancel(DialogInterface dialog) {
                                        finish();
                                    }
                                }).create();
                mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                return mAlertDialog;
            }
        } else if (id == DIALOG_DIR_NOT_EXIST) {
            Log.v(TAG, "enter DIC_NOT_EXIST case");
            mAlertDialog = new AlertDialog.Builder(this, Constants.DIALOG_THEME)
                .setMessage(getString(R.string.dir_not_exist, mConstants.DEFAULT_FOLDER))
                .setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {                            
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    removeDialog(DIALOG_DIR_NOT_EXIST);
                    finish();
                }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {                   
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                }).create();
            mAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            return mAlertDialog;
        } else if (id == DIALOG_CANCEL_PROGRESS) {
            Log.v(TAG, "enter DIALOG_CANCEL_PROGRESS");
            if (!mIsServiceEnd) {
                Log.d(TAG, "show cancel dialog while serrice running.");
                mProgressCancelDialog = new ProgressDialog(this, Constants.DIALOG_THEME);
                mProgressCancelDialog.setCancelable(false);
                mProgressCancelDialog.setTitle(getString(R.string.progressbar_title));
                mProgressCancelDialog.setMessage(getString(R.string.operation_is_cancelling));
                mProgressCancelDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                return (Dialog)mProgressCancelDialog;    
            } else {
                Log.e(TAG, "Don't show cancel dialog while service ended.");
                return null;
            }
        } else if (id == DIALOG_PREPARE) {
                Log.d(TAG, "show prepare dialog while it is preparing for backup specific contacts.");
                mProgressPrepareDialog = new ProgressDialog(this, Constants.DIALOG_THEME);
                mProgressPrepareDialog.setCancelable(false);
                mProgressPrepareDialog.setTitle(getString(R.string.progressbar_title));
                mProgressPrepareDialog.setMessage(getString(R.string.prepare_for_backup));
                mProgressPrepareDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                return (Dialog)mProgressPrepareDialog;
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

        // modified by amt_jiayuanyuan 2013-01-17 SWITCHUITWO-515 begin
        if (mIsServiceEnd == false) {
            NotificationManager notMgr = (NotificationManager)getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notMgr != null) notMgr.cancel(1);
        } /*else {
        	Log.d("jiayy", "onResume() mIsServiceEnd finish BaseActivity2 ");
            finish();
        }*/
        // modified by amt_jiayuanyuan 2013-01-17 SWITCHUITWO-515 begin 
    }

    private class CancelListener
        implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
        public void onClick(DialogInterface dialog, int which) {
            Log.d(TAG, "CancelListener onClick()");
            stopService(new Intent(BaseActivity2.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }

        public void onCancel(DialogInterface dialog) {
            Log.d(TAG, "CancelListener onCancel()");
            stopService(new Intent(BaseActivity2.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }

        public void onDismiss(DialogInterface dialog) {
            Log.d(TAG, "CancelListener onDismiss()");
            stopService(new Intent(BaseActivity2.this, BackupRestoreService.class));
            removeDialog(DIALOG_PROGRESS_BAR);
            mProgressDialog = null;
            showDialog(DIALOG_CANCEL_PROGRESS);
        }
    }

    private CancelListener mCancelListener = new CancelListener();
    
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

    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    protected void showMessage(int msgPara, Object obj) {
        String showMsg = "";
        showMsg = getShowMsg(msgPara, obj);
        showMessage(showMsg);
    }
    
    protected String getShowMsg(int msgPara, Object obj) {
        Log.v(TAG, "Enter showMessage()");
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
                .setOnCancelListener(new DialogInterface.OnCancelListener() {                    
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
        mAlertDialog.setCanceledOnTouchOutside(false);
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
        } else if (type == Constants.LOCAL_ACTION) {
            content = getString(R.string.local_contacts);
        } else if (type == Constants.CCARD_ACTION) {
            content = getString(R.string.cCard_contacts);
        } else if (type == Constants.GCARD_ACTION) {
            content = getString(R.string.gCard_contacts);
        } else if (type == Constants.GACCOUNT_ACTION) {
            content = getString(R.string.gAccount_contacts);
        } else if (type == Constants.EACCOUNT_ACTION) {
            content = getString(R.string.eAccount_contacts);
        } else {
            Log.e(TAG, "Wrong action type in getTypeName : " + type);
        }
        return content;
    }
    
    protected String getFileName(int type) {
        String content = "";
        if (type == Constants.CONTACTS_ACTION) {
            content = "/"+mConstants.DEFAULT_FOLDER+"/"+Constants.CONTACTS_FILE_NAME;
        } else if (type == Constants.SMS_ACTION) {
            content = "/"+mConstants.DEFAULT_FOLDER+"/"+Constants.SMS_FILE_NAME;
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
    
    private boolean isMemoryEncryptionOn(){
        //Memory
        String ON = "1";
        String memoryProps = android.os.SystemProperties.get("persist.sys.mot.encrypt.mmc", "");
        if (memoryProps.length() > 0 && memoryProps.startsWith(ON)) {
            return true;  //memory encryption set to ON
        }
        return false;
    }
    
    private void showDialogNotSupportUnsavedVCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, Constants.DIALOG_THEME);
        builder.setTitle(R.string.caution);
        builder.setMessage(R.string.not_support_unsaved_vcard_file);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                finish();
            }
        });
        AlertDialog ad = builder.create();
        ad.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ad.setCanceledOnTouchOutside(false);
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                finish();
            }
        });
        ad.show();
    }

}
