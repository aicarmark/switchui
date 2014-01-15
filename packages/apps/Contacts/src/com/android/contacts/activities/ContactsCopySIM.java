/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.activities;

import java.util.ArrayList;
import java.util.Iterator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.android.telephony.PhoneModeManager;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;
import com.android.contacts.ContactsUtils;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.R;
import com.android.contacts.SimUtility;


/**
 * SIM/Device copy activity for contacts app
 */
public class ContactsCopySIM extends Activity {

    private static final String TAG = "ContactsCopySIM";
    
    public static final String MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD = "com.android.contacts.MultiPickerCopyD2C"; 

    public static final int DEVICE_TO_C_CARD = 0;
    public static final int DEVICE_TO_G_CARD = 1;
    public static final int C_CARD_TO_DEVICE = 2;
    public static final int G_CARD_TO_DEVICE = 3;

    private static final int MULTI_PICK_CONTACT_REQUEST = 0;
    
    private static final int SIM_COPY_BATCH = 20;
    // The maximum number of operations per yield point is 500, set 400 as optimized size
    private static final int MAX_BATCH_SIZE = 400;

    private int mCase = 0;

    private AlertDialog mProgressDialog = null;
    private ProgressBar mProgressBar = null;
    private TextView mText = null;
    private boolean mCReady = false;
    private boolean mGReady = false;
    private boolean isDualMode;
    Context mDialogContext = null;
    String[] mItems;

    private static final int MSG_UPDATE_PROGRESS = 0;
    private static final int MSG_PROMPT_CANCEL = 1;
    private static final int MSG_CONFIRM_DONE = 2;
    private static final int MSG_NO_GROUP = 3;
    private static final int MSG_NO_SPACE = 4;
    private static final int MSG_INIT_PROGRESS = 5;
    private static final int MSG_CONFIRM_INSUFFICIENT_SPACE = 6;

    boolean mCanceled = false;
    private Object [] mA;    

    private class CopyAllThread extends Thread implements OnCancelListener, OnClickListener {
        private Long[] mPersonIds;
        private PowerManager.WakeLock mWakeLock;

        public CopyAllThread() {
            super("CopyAllThread");
            PowerManager powerManager = (PowerManager)ContactsCopySIM.this.getSystemService(
                    Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, "ContactsCopySIM");
        }

        @Override
        public void run() {

            long peopleId;
            ArrayList<Long> personIds = new ArrayList<Long>();

            for (int j = 0; j < mA.length; j++) {
                peopleId = ContentUris.parseId(Uri.parse(mA[j].toString()));
                personIds.add(peopleId);
            }
            mPersonIds = personIds.toArray(new Long[personIds.size()]);

                // get the select people info
            ArrayList<SimUtility.PeopleInfo> peopleList = new ArrayList<SimUtility.PeopleInfo>();
            if (!getSelectPeopleInfo(peopleList, mPersonIds))
            {
                quitNoGroup();
                return;
            }

            int nCount = 0;
            int nSelect = peopleList.size();
            // check SIM free space before copying

            if((mCase == DEVICE_TO_C_CARD)||(mCase == DEVICE_TO_G_CARD)) {
                int nFreeSpace = 0;
                if (mCase == DEVICE_TO_C_CARD) {
                    nFreeSpace = SimUtility.getFreeSpace(getContentResolver(), TelephonyManager.PHONE_TYPE_CDMA);
                }
                if (mCase == DEVICE_TO_G_CARD) {
                    nFreeSpace = SimUtility.getFreeSpace(getContentResolver(), TelephonyManager.PHONE_TYPE_GSM);
                }
                
                if(nFreeSpace == 0) {
                    quitNoSpace();
                    return;
                } else if (nFreeSpace < nSelect) {
                    confirmInsufficientSpace(nFreeSpace);
                    nSelect = nFreeSpace;
                }
            }

            initProgress(nSelect);
            mWakeLock.acquire();
            nCount = bulkCopyContacts(peopleList, nSelect);

            if (mCase == DEVICE_TO_C_CARD) {
                SimUtility.requestSimSync(TelephonyManager.PHONE_TYPE_CDMA);
            }
            else if (mCase == DEVICE_TO_G_CARD) {
                SimUtility.requestSimSync(TelephonyManager.PHONE_TYPE_GSM);
            }
            
            mWakeLock.release();
                                                                
            // mProgressDialog.dismiss();            
            if (nCount < nSelect) {
                promptCancel(nCount, nSelect);
            }
            else {
                confirmDone(nSelect);
            }
        }    
                 
        public void onCancel(DialogInterface dialog) {
             mCanceled = true;
        }        
                 
        public void onClick(DialogInterface dialog, int which) {
            mCanceled = true;
            // mProgressDialog.dismiss();
        }
        
        @Override
        public void finalize() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }        
        }
    }

    private DialogInterface.OnKeyListener mSearchKeyListener = new DialogInterface.OnKeyListener() {
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH: {
                    dialog.dismiss();
                    mProgressDialog = null;  // for the progress dialog reuse
                    finish();
                    return true;
                }
            }
            return false;
    }};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isDualMode = PhoneModeManager.isDmds();

        // Wrap our context to inflate list items using correct theme
        mDialogContext = new ContextThemeWrapper(this,
                                                 getResources().getBoolean(R.bool.contacts_dark_ui)
                                                 ? com.android.internal.R.style.Theme_Holo_Dialog_Alert
                                                 : com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert
                                                 );

        if (!checkSIM())
            return;

        new AlertDialog.Builder(mDialogContext)
            .setTitle(R.string.copysimdevice_title)
            .setItems(mItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    getCopyCase(which);
                    /*
                    Intent mpickIntent = new Intent("com.motorola.action.PICK_MULTIPLE");
                    mpickIntent.setType("vnd.android.cursor.dir/contact");
                    mpickIntent.setClassName("com.android.contacts", "com.android.contacts.activities.MultiplePickActivity"); 
                    */

                    Intent mpickIntent = new Intent("motorola.intent.ACTION_MULTIPLE_PICK", Contacts.CONTENT_URI);    // platform multipicker     
                    Log.v(TAG, "start multiple picker, mCase = " + mCase);
                    if((mCase == DEVICE_TO_C_CARD)||(mCase == DEVICE_TO_G_CARD)) {
                        // Filter out those contacts which can not be exported/shared
                        mpickIntent.putExtra("for_export_only", true);
                    }
                    
                    String accountType = HardCodedSources.ACCOUNT_TYPE_CARD;
                    int defaultType = TelephonyManager.getDefault().getPhoneType();
                    String accountDefault = SimUtility.getAccountNameByType(defaultType);
                    String accountC = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_CDMA);
                    String accountG = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_GSM);                    

                    ArrayList<Account> accountsList = new ArrayList<Account>();
                    if (mCase == DEVICE_TO_C_CARD || mCase == DEVICE_TO_G_CARD) {
					/**JB update SWITCHUITWOV-308 " not display Social network contacts when copy device to sim card " by bphx43 2012-11-12*/
                        accountsList.add(new Account(HardCodedSources.ACCOUNT_LOCAL_DEVICE, HardCodedSources.ACCOUNT_TYPE_LOCAL));
                        mpickIntent.putParcelableArrayListExtra("include_account", accountsList);
					/**end */
                    }
                    else if (mCase == C_CARD_TO_DEVICE) {
                        accountsList.add(new Account(accountC, accountType));
                        mpickIntent.putParcelableArrayListExtra("include_account", accountsList);
                    }
                    else if (mCase == G_CARD_TO_DEVICE) {
                        accountsList.add(new Account(accountG, accountType));
                        mpickIntent.putParcelableArrayListExtra("include_account", accountsList);
                    }
                    
                    startActivityForResult(mpickIntent, MULTI_PICK_CONTACT_REQUEST);
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    finish();
                }})
            .setOnKeyListener(mSearchKeyListener)
            .show();
    }


    @Override
    protected void onDestroy() {
        mCanceled = true;   // kill the thread when the activity force to close
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onDestroy();        
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MULTI_PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            new MulPickResult(this).execute(data);
        } else {
            Log.v(TAG, "NONE-RESULT_OK returned, no data");
            finish();
        }
    }
    
    /**
     * Represents an asynchronous task used to get the multiple picker result
     */
    public class MulPickResult extends AsyncTask<Intent, Void, Void> {

        private Context mContext;
        private int mCount = 0;

        public MulPickResult(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... params) {
            Intent data = params[0];
            // platform's multipicker  - contacts based
            ArrayList<Uri> selected_contact_uris = new ArrayList<Uri>();                    
            if(ContactsUtils.getMultiplePickResult(mContext, data, selected_contact_uris)) {
                mA = selected_contact_uris.toArray();                        
            }
            mCount = selected_contact_uris.size();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mCount > 0) {
                Log.v(TAG, "User select multiple contacts to copy between SIM/Device, count = " + mCount);
                createDialog();
            } else {
                finish();
            }
        }
    }
    
    protected void createDialog() {

        String[] title = null;
        String[] text = null;
        if (isDualMode) {
            title = getResources().getStringArray(R.array.title_copy_progress_items);
            text = getResources().getStringArray(R.array.text_copy_progress_items);
        } else {
            title = getResources().getStringArray(R.array.title_copy_progress_items_single);
            text = getResources().getStringArray(R.array.text_copy_progress_items_single);
        }
                
        CopyAllThread thread = new CopyAllThread();

        LayoutInflater factory = LayoutInflater.from(ContactsCopySIM.this);
        View ProgressView = factory.inflate(R.layout.progressbar_copy, null);

        mProgressDialog = new AlertDialog.Builder(mDialogContext)
            .setTitle(title[mCase])
            .setMessage(text[mCase])
            .setView(ProgressView)
            .setNegativeButton(android.R.string.cancel, thread)
            .setOnCancelListener(thread)
            .setOnKeyListener(mSearchKeyListener)
            .show();

            mProgressBar = (ProgressBar) mProgressDialog.findViewById(R.id.progress_horizontal);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mText = (TextView) mProgressDialog.findViewById(R.id.text);
            mCanceled = false;
            thread.start();                                               
    }    	


    private Dialog createErrorDialog(int resId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mDialogContext);
        builder.setTitle(R.string.copysimdevice_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(resId);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    private boolean checkSIM()
    {
        // we need wait SIM ready before copying, the freespace is 0 when powerup, but it is also 0 when full.
        // so we should not use getFreeSpace to verify if SIM is ready.
        if (!SimUtility.getSIMLoadStatus()) {
            this.createErrorDialog(R.string.loadsim_text).show();
            return false;
        }


        mCReady = SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_CDMA);
        mGReady = SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_GSM);

        if (!mCReady && !mGReady) { // check C/G card available ?
            if (isDualMode) {
                this.createErrorDialog(R.string.no_card_text).show();
            } else {
                this.createErrorDialog(R.string.no_card_text).show();
            }
            return false;
        }

        String[] items = null;
        if (isDualMode) {
            items= getResources().getStringArray(R.array.select_copy_dialog_items);
        } else {
            items= getResources().getStringArray(R.array.select_copy_dialog_items_single);
        }

        if (mCReady && mGReady) {
            mItems = new String[items.length];
            System.arraycopy(items, 0, mItems, 0, items.length);
        }
        else if (mCReady) {
            mItems = new String[2];
            mItems[0] = items[DEVICE_TO_C_CARD];
            mItems[1] = items[C_CARD_TO_DEVICE];
        }
        else if (mGReady) {
            mItems = new String[2];
            mItems[0] = items[DEVICE_TO_G_CARD];
            mItems[1] = items[G_CARD_TO_DEVICE];
        }
        else
            mItems = null;

        return true;

    }

    private void getCopyCase(int which) {
        if (mCReady && mGReady) {
            mCase = which;
        }
        else if (mCReady) {
            if (which == 0)
                mCase = DEVICE_TO_C_CARD;
            if (which == 1)
                mCase = C_CARD_TO_DEVICE;
        }
        else if (mGReady) {
            if (which == 0)
                mCase = DEVICE_TO_G_CARD;
            if (which == 1)
                mCase = G_CARD_TO_DEVICE;
        }
        else
            ;
    }

    private boolean getSelectPeopleInfo(ArrayList<SimUtility.PeopleInfo> peopleList, Long[] PersonIds)
    {
        ContentResolver resolver = getContentResolver();
        int[] freeEmailSpace = new int[1];
        freeEmailSpace[0] = SimUtility.getEmailFreeSpace(resolver);

        for (int i = 0; i < PersonIds.length; i++)
        {
            if (mCanceled) 
                return false;
            long contactId = PersonIds[i];

            if (contactId < 0)  // can not find match contact, move next
                continue;
            SimUtility.getSelectPeopleInfo(resolver, contactId, peopleList, mCase, freeEmailSpace);
        }

        if (peopleList.size() > 0)
            return true;
        else
            return false;
    }


    // nSelect maybe less than the size due to there is no enough space
    private int bulkCopyContacts(ArrayList<SimUtility.PeopleInfo> peopleList, int nSelect)
    {              
        int nCount = 0;
        int nStep = nSelect / 10;    // 10 times to finish copy batch operation
        if (nStep < SIM_COPY_BATCH) {
            nStep = SIM_COPY_BATCH;
        } else if (nStep > MAX_BATCH_SIZE) {
            nStep = MAX_BATCH_SIZE;
        }
        
        String accountName = "";
        String accountType = "";
        
        if (mCase == DEVICE_TO_C_CARD) {
            accountName = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_CDMA);
            accountType = HardCodedSources.ACCOUNT_TYPE_CARD;
        }
        else if (mCase == DEVICE_TO_G_CARD) {
            accountName = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_GSM);
            accountType = HardCodedSources.ACCOUNT_TYPE_CARD;        	
        }
        else { // (mCase == C_CARD_TO_DEVICE || mCase == G_CARD_TO_DEVICE)
            // Copy from SIM to device
            accountName = HardCodedSources.ACCOUNT_LOCAL_DEVICE;
            accountType = HardCodedSources.ACCOUNT_TYPE_LOCAL;         
        }
        Account copyAccount = new Account(accountName, accountType);

        ContentResolver resolver = getContentResolver();
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            
        Iterator<SimUtility.PeopleInfo> it = peopleList.iterator();
        while (!mCanceled && it.hasNext() && (nCount < nSelect)) {
            SimUtility.PeopleInfo info = (SimUtility.PeopleInfo) it.next();
            if (SimUtility.insertOneContacts(operationList, copyAccount, info, false)) {
                nCount++;                
                updateProgress(nCount, nSelect);
            }

            if (nCount % nStep == 0 && operationList != null && operationList.size() > 0) {
                Log.v(TAG, "nCount = " + nCount+", op_size = "+ operationList.size());
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                }
                operationList.clear();
            }
        }
        if (operationList.size() > 0) {
            Log.v(TAG, "applyBtach, left = " + operationList.size());
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }
        return nCount;
    }


    private void quitNoSpace()
    {
        mHandler.sendEmptyMessage(MSG_NO_SPACE);
    }

    private void quitNoGroup()
    {
        mHandler.sendEmptyMessage(MSG_NO_GROUP);
    }

    private void initProgress(int max)
    {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_INIT_PROGRESS, 0, max));
    }

    private void updateProgress(int cur, int max)
    {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS, cur, max));
    }

    private void promptCancel(int cur, int max)
    {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROMPT_CANCEL, cur, max));
    }

    private void confirmDone(int max)
    {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CONFIRM_DONE, 0, max));
    }

    private void confirmInsufficientSpace(int max)
    {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CONFIRM_INSUFFICIENT_SPACE, 0, max));
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mProgressDialog == null) {  // the copy not start or the activity has forced to close
                return;
            }

            switch(msg.what) {
            case MSG_INIT_PROGRESS:
                mProgressBar.setProgress(0);
                mProgressBar.setMax(msg.arg2);
                mText.setText(getString(R.string.copy_progress_copied, 0, msg.arg2));
                break;


            case MSG_UPDATE_PROGRESS:
                mProgressBar.setProgress(msg.arg1);
//              mText.setText(msg.arg1 + " " + getString(R.string.copy_of) + " " + msg.arg2 + " " + getString(R.string.copy_copied));
                mText.setText(getString(R.string.copy_progress_copied, msg.arg1, msg.arg2));
                break;

            case MSG_PROMPT_CANCEL:
                mProgressDialog.dismiss();
                mProgressDialog = null;
                String[] title = null;
                if (isDualMode) {
                    title = getResources().getStringArray(R.array.title_copy_progress_items);
                } else {
                    title = getResources().getStringArray(R.array.title_copy_progress_items_single);
                }
                String canceltext;
//              String canceltext = getString(R.string.copy_cancelled) + "\n" + msg.arg1 + " " + getString(R.string.copy_of) + " "
//                                  + msg.arg2 + " " + getString(R.string.copy_contacts_copied_to) + " ";

                if (mCase == DEVICE_TO_C_CARD) {
                    if (isDualMode) {
                        canceltext = getString(R.string.copy_cancelled_to_ccard, msg.arg1, msg.arg2);
                    } else {
                        canceltext = getString(R.string.copy_cancelled_to_uim_card, msg.arg1, msg.arg2);
                    }
                }
                else if (mCase == DEVICE_TO_G_CARD) {
                    canceltext = getString(R.string.copy_cancelled_to_gcard, msg.arg1, msg.arg2);
                }
                else {
                    canceltext = getString(R.string.copy_cancelled_to_device, msg.arg1, msg.arg2);
                }

                new AlertDialog.Builder(mDialogContext)
                    .setTitle(title[mCase])
                    .setMessage(canceltext)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }})
                    .show();
                break;

            case MSG_CONFIRM_DONE:
                mProgressDialog.dismiss();
                mProgressDialog = null;
                String confirmtext;
                int cped = msg.arg2;
//    	        String confirmtext = getString(R.string.copy_success) + "\n" + msg.arg2 + " " + getString(R.string.copy_contacts_to) + " ";
                if (mCase == DEVICE_TO_C_CARD) {
                    if (isDualMode) {
                        confirmtext = getResources().getQuantityString(
                                        R.plurals.copy_success_to_ccard, cped, cped);
                    } else {
                        confirmtext = getResources().getQuantityString(
                                        R.plurals.copy_success_to_uim_card, cped, cped);
                    }
                }
                else if (mCase == DEVICE_TO_G_CARD) {
                    confirmtext =  getResources().getQuantityString(
                                        R.plurals.copy_success_to_gcard, cped, cped);
                }
                else {
                    confirmtext = getResources().getQuantityString(
                                        R.plurals.copy_success_to_device, cped, cped);
                }
                Toast.makeText(ContactsCopySIM.this, confirmtext, Toast.LENGTH_SHORT).show();

                finish();
                break;

            case MSG_CONFIRM_INSUFFICIENT_SPACE:
                String confirmSpacetext;
                if (mCase == DEVICE_TO_C_CARD)
                    confirmSpacetext = getString(R.string.copy_confirm_to_c_card_part);
                else if (mCase == DEVICE_TO_G_CARD)
                    confirmSpacetext = getString(R.string.copy_confirm_to_g_card_part);
                else
                    confirmSpacetext = getString(R.string.copy_nospace_to_device);
                Toast.makeText(ContactsCopySIM.this, confirmSpacetext, Toast.LENGTH_SHORT).show();
                //finish();
                break;

            case MSG_NO_GROUP:
            	Toast.makeText(ContactsCopySIM.this, R.string.no_contacts_copied, Toast.LENGTH_SHORT).show();        
            	Log.v(TAG, "Toast shown, then finish");    	            
            
                mProgressDialog.dismiss();
                mProgressDialog = null;
                finish();
                break;

            case MSG_NO_SPACE:
                mProgressDialog.dismiss();
                mProgressDialog = null;
                int resId = -1;

                if (mCase == DEVICE_TO_C_CARD)
                    resId = R.string.copy_nospace_to_c_card;
                else if (mCase == DEVICE_TO_G_CARD)
                    resId = R.string.copy_nospace_to_g_card;
                else
                    resId = R.string.copy_nospace_to_device;

                new AlertDialog.Builder(mDialogContext)
                    .setIcon(R.drawable.ic_menu_import_contact)
                    .setTitle(R.string.copy_nospace_title)
                    .setMessage(resId)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }})
                    .show();
                break;
            }
        }
    };

}


