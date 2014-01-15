package com.android.contacts.service;

import java.util.HashMap;
import java.util.HashSet;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import java.util.ArrayList;

import com.android.contacts.ContactsUtils; 
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.R;
import com.android.contacts.SimUtility;

// SyncAdapter for local contacts
public class LocalSyncAdapterService extends Service {
    protected static final String TAG = "LocalSyncSvc";

    public static final String ACTION_LOCALSYNCADAPTER_PROGRESS = "com.android.contacts.LocalSyncProgress";
    public static final String ACTION_LOCALSYNCADAPTER_SYNCDONE = "com.android.contacts.LocalSyncDone";
    public static final String LOCALSYNCADAPTER_INTENT_EXTRA_TOTAL = "com.android.contacts.LocalSyncTotal";
    public static final String LOCALSYNCADAPTER_INTENT_EXTRA_SYNED = "com.android.contacts.LocalSyncSynced";    

    private static SyncAdapterImpl sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
    	Log.v(TAG, "Entering onBind(), intent = "+intent);
    	return sSyncAdapter.getSyncAdapterBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Entering LocalSyncAdapterService onCreate()");
        mContext = getApplicationContext();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapterImpl(mContext);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroying LocalSyncAdapterService");
        super.onDestroy();
    }

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true /* autoInitialize */);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras,
                String authority, ContentProviderClient provider, SyncResult syncResult) {
            Log.v(TAG, "Entering onPerformSync(), account: "+account+", extras: "+extras+"auth: "+authority+", provider: "+provider+", syncRes: "+syncResult);

            //check if the sync is started explicitly by Contacts App
            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL) && extras.getBoolean(SimUtility.SYNC_EXTRAS_MOTOROLA_NO_DATA_CONNECTION_CHECK)) {            
                try {
                    LocalSyncAdapterService.performSync(mContext, account, extras,
                            authority, provider, syncResult);
                } catch (OperationCanceledException e) {
                    Log.v(TAG, " exception: "+e);
                }
            } else {
            	// triggered by system broadcast, do nothing
            	Log.v(TAG, "Don't sync for not triggered by Contacts app explicitly !");
            }
        }
    }

    /**
     * Integration with system SyncManager; start a contacts
     * sync when we get the signal from the system SyncManager.
     */
    private static void performSync(Context context, Account account, Bundle extras,
            String authority, ContentProviderClient provider, SyncResult syncResult)
    throws OperationCanceledException {
        Log.v(TAG, "performSync(), extras = "+extras);

        boolean need_progress = extras.getBoolean("need_progress");
        // here we need to check for each type of account
        // for now, lets just send the intent for all account types
        Log.v(TAG, "Got notification that something changed in Acct_type: "+account.type+", name:"+account.name+", need_progress = "+need_progress);
        checkForceSync(context, account, need_progress);
    }

    protected static boolean checkForceSync(Context context,  Account account, boolean need_progress) {
        // Delete dirty contacts of local contacts
        long pre_time = System.currentTimeMillis();
        SimUtility.deleteAccountMembersMarked(context.getContentResolver(), HardCodedSources.ACCOUNT_TYPE_LOCAL);
        Log.v(TAG, "Delete account member marked, Operation time: " + (System.currentTimeMillis() - pre_time)/1000 + "s");

        ContactsUtils.deleteGroupsMarked(context.getContentResolver(), HardCodedSources.ACCOUNT_TYPE_LOCAL);
/*
        // fetch raw_contacts' ids with updates since last sync from DB 
        Cursor rawContactIdCursor = null;
        ContentResolver cr = context.getContentResolver();

        int i = 1;
        ArrayList<Long> rawContactIds = new ArrayList<Long>();
        try {
            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
                                          new String[] {RawContacts._ID, RawContacts.DELETED},
                                          RawContacts.ACCOUNT_NAME + " =? AND " + RawContacts.ACCOUNT_TYPE + " =? AND " + RawContacts.DIRTY + " =1",
                                          new String[] {HardCodedSources.ACCOUNT_LOCAL_DEVICE, HardCodedSources.ACCOUNT_TYPE_LOCAL},
                                          null);
            if (rawContactIdCursor != null) {
            	int total = rawContactIdCursor.getCount();
                Log.v(TAG, "rawContactIds(to be synced) contains : "+total);

                ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                int each_batch = 100;
                int nCount = 0;
                while (rawContactIdCursor.moveToNext()) {
                    if (nCount >= each_batch) {
                        Log.v(TAG, "applyBatch operation count : "+nCount);
                        try {
                            cr.applyBatch(ContactsContract.AUTHORITY, operationList);
                        } catch (RemoteException e) {
                            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        } catch (OperationApplicationException e) {
                            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        }
                        operationList.clear();
                        nCount = 0;
                    }

                    Long id = rawContactIdCursor.getLong(0);
                    Log.v(TAG, "sync # "+ i +", id = "+id);
                    rawContactIds.add(id);

                    {
                        int deleted = rawContactIdCursor.getInt(1);
                        if (deleted == 0) {
                            // !! Do NOT clear dirty bit for PIM sync may need it
//                            ContentValues values = new ContentValues();
//                            values.put(RawContacts.DIRTY, 0);
//                            Builder builder = ContentProviderOperation
//                                    .newUpdate(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
//                                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
//                                            .build());
//                            builder.withYieldAllowed(true);
//                            builder.withValues(values);
//                            operationList.add(builder.build());
//                            nCount++;
                        } else {
                            // permenent delete in DB
                            operationList.add(ContentProviderOperation
                                    .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                            .build())
                                    .withYieldAllowed(true)
                                    .build());
                            nCount++;
                        }
                    }

                    // broadcast sync progress to interested activity
                    if (need_progress) {
                        Intent intent = new Intent(ACTION_LOCALSYNCADAPTER_PROGRESS);
                        intent.putExtra(LOCALSYNCADAPTER_INTENT_EXTRA_TOTAL, total);
                        intent.putExtra(LOCALSYNCADAPTER_INTENT_EXTRA_SYNED, i);
                        context.sendBroadcast(intent);
                    }
                    i++;
                }  // while()

                if (operationList.size() > 0) {
                    Log.v(TAG, "applyBatch operation count : "+nCount);
                    try {
                        cr.applyBatch(ContactsContract.AUTHORITY, operationList);
                    } catch (RemoteException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                }

                // all sync done, broadcast notice
                Intent intent = new Intent(ACTION_LOCALSYNCADAPTER_SYNCDONE);
                intent.putExtra(LOCALSYNCADAPTER_INTENT_EXTRA_TOTAL, total);             	           	                
                context.sendBroadcast(intent);
            } else {
            	Log.v(TAG, "rawContactIdCursor = null, no sync will occur !");
            	return false;
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
*/
    	return true;
    }
}
