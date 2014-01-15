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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import com.android.contacts.model.HardCodedSources;
import com.android.contacts.R;
import com.android.contacts.SimUtility;

//import com.motorola.blur.util.StringUtils;
//import com.motorola.blur.util.SyncConstants;

public class SimSyncAdapterService extends Service {
    protected static final String TAG = "SimSyncSvc";

    public static final String ACTION_SIMSYNCADAPTER_PROGRESS = "com.android.contacts.SimSyncProgress";
    public static final String ACTION_SIMSYNCADAPTER_SYNCDONE = "com.android.contacts.SimSyncDone";
    public static final String SIMSYNCADAPTER_INTENT_EXTRA_TOTAL = "com.android.contacts.SimSyncTotal";
    public static final String SIMSYNCADAPTER_INTENT_EXTRA_SYNED = "com.android.contacts.SimSyncSynced";    

    private static final int SIMSYNCADAPTER_SYNC_NOTIFICATION_ID = 1000;
    private static final int MARK_SIM_LOADING_THRESHOLD_OF_ITEM_SYNCING = 5;    // items being synced, used to flag sim is loading
    private static SyncAdapterImpl sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();
    private static boolean mMarkSimBusy = false;        
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
    	Log.v(TAG, "Entering onBind(), intent = "+intent);
    	return sSyncAdapter.getSyncAdapterBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Entering SimSyncAdapterService onCreate()");
        mContext = getApplicationContext();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapterImpl(mContext);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroying SimSyncAdapterService");

        if (mMarkSimBusy) {
            SimUtility.setSIMLoadStatus(true);
            mMarkSimBusy = false;
        }

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
    	        showNotification(mContext);            
                try {
                    SimSyncAdapterService.performSync(mContext, account, extras,
                            authority, provider, syncResult);
                } catch (OperationCanceledException e) {
                	Log.v(TAG, " exception: "+e);
                }
                
                hideNotification(mContext);            
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

        int type = SimUtility.getTypeByAccountName(account.name);
        // if it is sim sync case, need check if the sim is ready before start the sync
        if (type == TelephonyManager.PHONE_TYPE_CDMA || type == TelephonyManager.PHONE_TYPE_GSM) {
    	     if (!SimUtility.isSimReady(type) || !SimUtility.getSIMLoadStatus()) {
                 Log.v(TAG, "performSync(), sim/uim not available or not loaded to DB yet, don't sync !");
    	         return;
    	     }
        }
        
        boolean need_progress = extras.getBoolean("need_progress");
        // here we need to check for each type of account
        // for now, lets just send the intent for all account types
        Log.v(TAG, "Got notification that something changed in Acct_type: "+account.type+", name:"+account.name+", need_progress = "+need_progress);
        checkForceSync(context, account, need_progress);     
    }

    protected static boolean checkForceSync(Context context,  Account account, boolean need_progress) {
    	// fetch raw_contacts' ids with updates since last sync from DB 
        Cursor rawContactIdCursor = null;
        int type = SimUtility.getTypeByAccountName(account.name);
        ContentResolver cr = context.getContentResolver();
        
        int i = 1;
        ArrayList<Long> rawContactIds = new ArrayList<Long>();
        try {
            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
                                          new String[] {RawContacts._ID , RawContacts.DELETED, RawContacts.SYNC1, RawContacts.SYNC2, RawContacts.SYNC3, RawContacts.SYNC4},
                                          RawContacts.ACCOUNT_NAME + " =? AND " + RawContacts.ACCOUNT_TYPE + " =? AND " + RawContacts.DIRTY + " =1",
                                          new String[] {account.name, account.type},
                                          null);
            if (rawContactIdCursor != null) {
            	int total = rawContactIdCursor.getCount();
            	mMarkSimBusy = false;
                Log.v(TAG, "rawContactIds(to be synced) contains : "+total);

                if (total > MARK_SIM_LOADING_THRESHOLD_OF_ITEM_SYNCING ) {
                    // flag SIM is busy in syncing, thus some sim utils are not available for using, i.e. getFreeSpace   
                    SimUtility.setSIMLoadStatus(false);
                    mMarkSimBusy = true;
                }
                            	
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

                	boolean is_newed = false;               
                	Long id = rawContactIdCursor.getLong(0);
                	               	
                	Log.v(TAG, "sync # "+ i +", id = "+id);               	
                	               	
                    rawContactIds.add(id);
        
                    SimUtility.PeopleInfo pInfo = new SimUtility.PeopleInfo();                    
                    pInfo.peopleName = rawContactIdCursor.getString(2);
                    pInfo.primaryNumber = rawContactIdCursor.getString(3);
                    pInfo.secondaryNumber = rawContactIdCursor.getString(5);
                    pInfo.primaryEmail = rawContactIdCursor.getString(4);
                    pInfo.diffStatus = SimUtility.DIFF_INIT;
                    pInfo.peopleId = id;                    
                    
                    if (TextUtils.isEmpty(pInfo.peopleName) 
                        && TextUtils.isEmpty(pInfo.primaryNumber) 
                        && TextUtils.isEmpty(pInfo.secondaryNumber) 
                        && TextUtils.isEmpty(pInfo.primaryEmail)) {
                        	// new added
                        	is_newed = true;
                    }
                    
                    if (is_newed) {
                    	// new added     
                        SimUtility.PeopleInfo newInfo = new SimUtility.PeopleInfo();                        	
                        if (!SimUtility.getPeopleInfoById(cr, id, newInfo)) {
                        	Log.v(TAG, "getPeopleInfoById() failed !");                        	
                        	continue;
                        }
                    	Log.v(TAG, "New added SIM entry, name="+pInfo.peopleName+", number="+pInfo.primaryNumber+", new_name="+newInfo.peopleName+", new_number1="+newInfo.primaryNumber+", new_number2="+newInfo.secondaryNumber + ", new_email="+newInfo.primaryEmail);

                        // validating info for storing
                        newInfo.peopleName = SimUtility.buildOldSimName(newInfo.peopleName); 
                        newInfo.primaryNumber = SimUtility.buildSimNumber(newInfo.primaryNumber);
                        newInfo.secondaryNumber = SimUtility.buildSimNumber(newInfo.secondaryNumber);
                        newInfo.primaryEmail = SimUtility.buildSimString(newInfo.primaryEmail);
                        
                        if (TextUtils.isEmpty(newInfo.primaryNumber) && !TextUtils.isEmpty(newInfo.secondaryNumber)) {
                        	//swap 
                        	Log.v(TAG, "swap primary and secondary numbers");
                        	newInfo.primaryNumber = newInfo.secondaryNumber;
                        	newInfo.secondaryNumber = null;                        	
                        }
                    	               	
                    	if ((mMarkSimBusy && SimUtility.getFreeSpace(cr, type, mMarkSimBusy)<=0)
                    	    || (!mMarkSimBusy && SimUtility.getFreeSpace(cr, type)<= 0)) {                    	    	
                    		// no space in card, need delete this from DB to keep consistence
                    		Log.v(TAG, "no space for new adding !");
                            operationList.add(ContentProviderOperation
                                    .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                            .build())
                                    .withYieldAllowed(true)
                                    .build());
                            nCount++;
                            continue;                    		
                    	} else if(!SimUtility.SimCard_AddContact(cr, newInfo, type)) {
                    		Log.v(TAG, "insert new SIM entry failed !!!");                    		
                    		// need delete this from DB or retry by next sync ?
                            operationList.add(ContentProviderOperation
                                    .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                            .build())
                                    .withYieldAllowed(true)
                                    .build());
                            nCount++;
                    		continue;
                    	}
                    	                   	
                    	// update to latest info in DB
                        ContentValues values = new ContentValues();
                        values.put(RawContacts.SYNC1, newInfo.peopleName);     
                        values.put(RawContacts.SYNC2, newInfo.primaryNumber);   
                        values.put(RawContacts.SYNC3, newInfo.primaryEmail);   
                        values.put(RawContacts.SYNC4, newInfo.secondaryNumber);       
                        // update sync flag 
                        values.put(RawContacts.DIRTY, 0);                   
                        Builder builder = ContentProviderOperation
                                .newUpdate(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                        .build());
                        builder.withYieldAllowed(true);
                        builder.withValues(values);
                        operationList.add(builder.build());
                        nCount++;
                    } else {
                        int deleted = rawContactIdCursor.getInt(1);
                        if (deleted == 0) {
                        	// udpated
                            SimUtility.PeopleInfo newInfo = new SimUtility.PeopleInfo();
                            if (!SimUtility.getPeopleInfoById(cr, id, newInfo)) {
                            	Log.v(TAG, "getPeopleInfoById() failed: "+id);
                                // retry by next sync         
                            	continue;
                            }

                            Log.v(TAG, "updated SIM entry, old_name="+pInfo.peopleName+", old_number="+pInfo.primaryNumber+", new_name="+newInfo.peopleName+", new_number="+newInfo.primaryNumber + ", new_email="+newInfo.primaryEmail);
                                       
                            if (SimUtility.isPeopleInfoEqual(pInfo, newInfo)) {
                                Log.v(TAG, "update info equals, no need to sync info! ");
                                // still need to clear the 'dirty' sync bit
                                ContentValues values = new ContentValues();
                                // update sync flag 
                                values.put(RawContacts.DIRTY, 0);        
                                Builder builder = ContentProviderOperation
                                        .newUpdate(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                .build());
                                builder.withYieldAllowed(true);
                                builder.withValues(values);
                                operationList.add(builder.build());
                                nCount++;
                                continue;
                            }
                            
                            // validating the latest info 
                            newInfo.peopleName = SimUtility.buildOldSimName(newInfo.peopleName); 
                            newInfo.primaryNumber = SimUtility.buildSimNumber(newInfo.primaryNumber);
                            newInfo.secondaryNumber = SimUtility.buildSimNumber(newInfo.secondaryNumber);
                            newInfo.primaryEmail = SimUtility.buildSimString(newInfo.primaryEmail);
                            
                            if (TextUtils.isEmpty(newInfo.primaryNumber) && !TextUtils.isEmpty(newInfo.secondaryNumber)) {
                            	//swap 
                            	newInfo.primaryNumber = newInfo.secondaryNumber;
                            	newInfo.secondaryNumber = null;                        	
                            }

                        	// check if the sim contact to be deleted exists in card
                        	if (SimUtility.SimCard_QueryContactExist(cr, pInfo, type)) {
                        		// already existing in card  
                                if (!SimUtility.SimCard_UpdateContact(cr, pInfo, newInfo, type)) {
                                    Log.v(TAG, "update existing SIM entry failed, retry next time !");
                                    // USIM has 2 partitions, each partition has 250 records share 50 email space.
                                    // If email of one partition is full, add email to a record of this partition will fail.
                                    if (!TextUtils.equals(pInfo.primaryEmail, newInfo.primaryEmail)) {
                                        //return EMAIL_PARTITION_FULL_FAILURE;
                                        Log.v(TAG, "update SIM entry failed: EMAIL_PARTITION_FULL_FAILURE !");
                                    }
                                    // retry by next sync      
                                    continue;
                                }  
                                
                    	        // update to latest info in DB
                                ContentValues values = new ContentValues();
                                values.put(RawContacts.SYNC1, newInfo.peopleName);     
                                values.put(RawContacts.SYNC2, newInfo.primaryNumber);   
                                values.put(RawContacts.SYNC3, newInfo.primaryEmail);   
                                values.put(RawContacts.SYNC4, newInfo.secondaryNumber);       
                                // update sync flag 
                                values.put(RawContacts.DIRTY, 0);        
                                
                                Builder builder = ContentProviderOperation
                                        .newUpdate(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                .build());
                                builder.withYieldAllowed(true);
                                builder.withValues(values);
                                operationList.add(builder.build());
                                nCount++;                                                  	                        		
                        	} else {
                        		Log.v(TAG, "sim contact to be updated NOT exists in card, deleting it from phone DB !");
                                operationList.add(ContentProviderOperation
                                        .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id).buildUpon()
                                                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                .build())
                                        .withYieldAllowed(true)
                                        .build());
                                nCount++;
                        	}      	
                       	                        	
                        } else {
                        	// deleted
                        	// check if the sim contact to be deleted exists in card
                        	if (SimUtility.SimCard_QueryContactExist(cr, pInfo, type)) {
                        		// already existing in card  
                                if (!SimUtility.SimCard_DeleteContact(cr, pInfo, type)) {
                                    Log.v(TAG, "delete existing SIM entry failed, retry next time !");
                                    // need retry next sync
                                    continue; 
                                }                        	                        		
                        	} else {
                        		Log.v(TAG, "sim contact to be deleted NOT exists in card, deleting it from phone DB !");
                        	}
                        	                        	
                        	// delete in DB
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
                        Intent intent = new Intent(ACTION_SIMSYNCADAPTER_PROGRESS);
                        intent.putExtra(SIMSYNCADAPTER_INTENT_EXTRA_TOTAL, total);                    	           	
                        intent.putExtra(SIMSYNCADAPTER_INTENT_EXTRA_SYNED, i);
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

                if (mMarkSimBusy) {
                    // flag SIM r/w is done and ready to be used
                    SimUtility.setSIMLoadStatus(true);
                    mMarkSimBusy = false;
                }
                
                // all sync done, broadcast notice
                Intent intent = new Intent(ACTION_SIMSYNCADAPTER_SYNCDONE);
                intent.putExtra(SIMSYNCADAPTER_INTENT_EXTRA_TOTAL, total);                    	           	                
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
            	
    	return true;
    }


/*
    protected static boolean checkForceSync(Context context,  Account account)
    {

        // Get unique sync_services with dirty rows or tombstone rows
        HashSet<String> syncTypes = new HashSet<String>();
 //       String type = SyncConstants.PREFIX_CONTACTS + SyncConstants.SUFFIX_BLUR;
 //       //syncTypes.add(IBlurProtocolHandler.SERVICE_NAME);
 
        String syncType =SyncConstants.PREFIX_CONTACTS + SyncConstants.SUFFIX_BLUR;

        
               
 /*       Cursor c = null;
        try {
        c = context.getContentResolver().query(
            BlurSync.DIRTY_BLURSERVICES_QUERY_URI, 
            new String[]{BlurSync.BLUR_SERVICE_NAME}, null, null, null);
            if (c != null)
            {
                while (c.moveToNext())
                {
                    String service = c.getString(0);
                    if (service != null){
                        syncTypes.add(SyncConstants.PREFIX_CONTACTS  + service);    
                    }
                    
                }
            }

	} finally {
		if (c != null) {
			c.close();
                }
        }
            
        // Force a sync of adapters with dirty rows
        for (String syncType: syncTypes)
        {
            if (Logger.DEVELOPMENT) { //added by LogFinder
                Logger.d(TAG, "Sending Sync broadcast - ", SyncConstants.ACTION_LOCAL_CHANGE, " with type ", syncType);
            } //added by LogFinder
            context.sendBroadcast(
                new Intent(SyncConstants.ACTION_LOCAL_CHANGE)
                .setType(syncType));
        } *
        
        if (Logger.DEVELOPMENT) { //added by LogFinder
            Logger.d(TAG, "Sending Sync broadcast - ", SyncConstants.ACTION_LOCAL_CHANGE, " with type ", syncType);
        } //added by LogFinder
        context.sendBroadcast(
            new Intent(SyncConstants.ACTION_LOCAL_CHANGE)
            .setType(syncType));
     
        return false;
    }
*/    

    private static void showNotification(Context context) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        int icon = android.R.drawable.stat_notify_sync;
        CharSequence tickerText = context.getString(R.string.notification_sim_sync_ticker);
        long when = System.currentTimeMillis();

        CharSequence contentTitle = context.getString(R.string.notification_sim_sync_title);
        CharSequence contentText = context.getString(R.string.notification_sim_sync_text);
        
        Notification notification = new Notification.Builder(context)
                                                    .setAutoCancel(false)
                                                    .setOngoing(true)
                                                    .setSmallIcon(icon)
                                                    .setContentTitle(contentTitle)
                                                    .setContentText(contentText)
                                                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                                                    .getNotification();                                                           
                                                     
        mNotificationManager.notify(SIMSYNCADAPTER_SYNC_NOTIFICATION_ID, notification);
        
        
        
        /*  deprecated     
            Notification notification = new Notification(icon, tickerText, when);
            CharSequence contentTitle = context.getString(R.string.notification_sim_sync_title);
            CharSequence contentText = context.getString(R.string.notification_sim_sync_text);
            Intent notificationIntent = new Intent(context, com.android.contacts.activities.MultiplePickActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            mNotificationManager.notify(SIMSYNCADAPTER_SYNC_NOTIFICATION_ID, notification);
        */

    }
        
    private static void hideNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(SIMSYNCADAPTER_SYNC_NOTIFICATION_ID);
    }
        
}
