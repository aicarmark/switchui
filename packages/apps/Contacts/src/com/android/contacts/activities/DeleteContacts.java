/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.util.Log;

import com.motorola.android.telephony.PhoneModeManager;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;

import java.util.ArrayList;
import android.telephony.TelephonyManager;


/**
 * Dialog that clears the call log after confirming with the user
 */
public class DeleteContacts extends ContactsActivity {
    private static final String TAG = "DeleteContacts";
    private Object [] mA;
    private boolean mIsCancel = false;
    private ProgressDialog mProgressDialog = null;      
    Context mDialogContext = null;

    private static final int EVENT_DELETE_DONE = 1;
    private static final int EVENT_UPDATE_PROGRESS = 2;

    private static final int SUBACTIVITY_PICK_CONTACTS_REQUEST = 1;
    
    private static final int SIM_DEL_BATCH = 20;
    // The maximum number of operations per yield point is 500, set 400 as optimized size
    // delete is a simple case, need not set yield as insertOneContacts
    private static final int MAX_BATCH_SIZE = 5; // if batch size was huge, it will block DB query.IKCBSMMCPPRC-1515

    private class DeleteAllThread extends Thread {

        private PowerManager.WakeLock mWakeLock;

        public DeleteAllThread() {
            super("DeleteAllThread");
            PowerManager powerManager = (PowerManager)DeleteContacts.this.getSystemService(
                    Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, "DeleteContacts");
        }

        @Override
        public void run() {

            Log.v(TAG, "start deleting contacts in background thread ...");
            mWakeLock.acquire();

            int nSize = mA.length;
            int nStep = nSize / 10;    // 10 times to finish delete batch operation
            if (nStep < SIM_DEL_BATCH) {
                nStep = SIM_DEL_BATCH;
            } else if (nStep > MAX_BATCH_SIZE) {
                nStep = MAX_BATCH_SIZE;
            }

            ContentResolver resolver = getContentResolver();
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            
            for (int i = 0; (i < nSize && !mIsCancel); ) {
                Uri passedInUri = Uri.parse(mA[i].toString());
                operationList.add(ContentProviderOperation.newDelete(passedInUri).build());
                i++;
                // getContentResolver().delete(passedInUri, null, null);                                
                // mProgressDialog.incrementProgressBy(1);
                mDelDiagHandler.sendMessage(mDelDiagHandler.obtainMessage(EVENT_UPDATE_PROGRESS, i, nSize));
                if (i % nStep == 0) {
                    Log.v(TAG, "applyBtach, i = " + i);
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

            if (PhoneModeManager.isDmds()) {
                // start the C/G sim sync as we maybe have deleted the C/G contacts.
                SimUtility.requestSimSync(TelephonyManager.PHONE_TYPE_CDMA);
                SimUtility.requestSimSync(TelephonyManager.PHONE_TYPE_GSM);
            } else {
                SimUtility.requestSimSync(TelephonyManager.getDefault().getPhoneType());
            }
            Account account = new Account(HardCodedSources.ACCOUNT_LOCAL_DEVICE, HardCodedSources.ACCOUNT_TYPE_LOCAL);
            SimUtility.requestSync(account);

            Log.v(TAG, "delete contacts in thread done");
            mWakeLock.release();                                                                
            mDelDiagHandler.sendEmptyMessage(EVENT_DELETE_DONE);
        }
        
                         
        @Override
        public void finalize() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }        
        }
    }

    private Handler mDelDiagHandler = new Handler() {
        public void handleMessage(Message msg) {
    	      if (mProgressDialog == null) {
    	          return;
    	      }

            switch(msg.what) {
                case EVENT_UPDATE_PROGRESS:
                    mProgressDialog.setProgress(msg.arg1);
                    return;

                case EVENT_DELETE_DONE:
                    Log.v(TAG, "main thread gets: EVENT_DELETE_DONE sent from child thread, dismissing dialog... ");
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                    finish();
                    return;
            }
        }
    };


    private class CancelListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Wrap our context to inflate list items using correct theme
        mDialogContext = new ContextThemeWrapper(this,
                                                 getResources().getBoolean(R.bool.contacts_dark_ui)
                                                 ? com.android.internal.R.style.Theme_Holo_Dialog_Alert
                                                 : com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert
                                                 );

        try {
            Intent pickIntent = new Intent("motorola.intent.ACTION_MULTIPLE_PICK", Contacts.CONTENT_URI);    // platform multipicker     

            if (!SimUtility.getSIMLoadStatus()) {
                // Card if loading, or not ready
                String accountType = HardCodedSources.ACCOUNT_TYPE_CARD;
                ArrayList<Account> accountsList = new ArrayList<Account>();
                if (PhoneModeManager.isDmds()) {
                    // exclude the C/G account
                    String accountC = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_CDMA);
                    String accountG = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_GSM);
                    accountsList.add(new Account(accountC, accountType));
                    accountsList.add(new Account(accountG, accountType));
                } else {
                    int defaultType = TelephonyManager.getDefault().getPhoneType();
                    String accountDefault = SimUtility.getAccountNameByType(defaultType);
                    accountsList.add(new Account(accountDefault, accountType));
                }
                pickIntent.putParcelableArrayListExtra("exclude_account", accountsList);
                Log.v(TAG, "SIM is not ready, Exclude SIM contacts !");
            }

            /*    China multipicker
            Intent pickIntent = new Intent("com.motorola.action.PICK_MULTIPLE");
            pickIntent.setType(Contacts.CONTENT_TYPE);
            pickIntent.setClassName("com.android.contacts", "com.android.contacts.activities.MultiplePickActivity");
            pickIntent.putExtra("com.android.contacts.MultiPickerWritableOnly", true);
            */
            startActivityForResult(pickIntent, SUBACTIVITY_PICK_CONTACTS_REQUEST);
        } catch(android.content.ActivityNotFoundException ex) {
            // if no app handles it, do nothing
            Log.v(TAG, "MULTI-PICKER activity not found !");
            finish();
        }
    }

    
    @Override
    protected void onDestroy() {
        mIsCancel = true;   // kill the thread when the activity force to close
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SUBACTIVITY_PICK_CONTACTS_REQUEST && resultCode == RESULT_OK) {
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
            Log.v(TAG, "will delete contacts : " + mCount);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mCount > 0) {
                createDialog();
            } else {
                finish();
            }
        }
    }

   
    protected void createDialog(){

        final int count = mA.length;
        final OnClickListener okListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DeleteAllThread thread = new DeleteAllThread();            	
                mProgressDialog = new ProgressDialog(DeleteContacts.this);
                mProgressDialog.setIcon(android.R.drawable.ic_menu_delete);
                mProgressDialog.setTitle(R.string.menu_deletemulticontacts);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setProgress(0);
                mProgressDialog.setMax(count);
                mProgressDialog.setMessage(getResources().getString(R.string.wait_delete_contact));
                mProgressDialog.setCancelable(true);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Log.v(TAG,"Back key pressed, set flag to stop deleting");
                        mIsCancel = true;
                        return;
                }});

                // TODO: Once we have the API, we should configure this ProgressDialog
                // to only show up after a certain time (e.g. 150ms)
                mProgressDialog.show();
                mIsCancel = false;
                thread.start();
            }
        };
        
        final CancelListener cancelListener = new CancelListener();
        String alertMsg = getResources().getQuantityString(R.plurals.alert_deletemulticontacts, count, count);
        new AlertDialog.Builder(mDialogContext)
            .setTitle(R.string.deleteConfirmation_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(alertMsg)
            .setNegativeButton(android.R.string.cancel, cancelListener)
            .setPositiveButton(android.R.string.ok, okListener)
            .setCancelable(false)
            .show();
    }       

}
