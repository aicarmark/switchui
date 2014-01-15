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

package com.android.contacts;

import java.util.ArrayList;
import java.util.Iterator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.model.HardCodedSources;
import com.android.contacts.service.CardAuthenticatorService;
import com.android.contacts.service.LocalAuthenticatorService;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardStatus;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;

import com.motorola.android.telephony.PhoneModeManager;

public class LoadSimContactsService extends Service{
    private static final String TAG = "LoadSimContactsService";

    private static final int EVENT_QUIT = 1;
    private static final int EVENT_LOAD_ALL_SIM_CONTACTS = 2;
    private static final int EVENT_LOAD_ONE_SIM_CONTACTS = 3;

    private static final int MAX_DETECT_RETRY_TIMES = 10;

    private static LoadSimContactsService sInstance = null;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private Context mContext = null;
    private BroadcastReceiver mIntentReceiver = null;

    public enum State {
        UNKNOWN,
        ABSENT,
        LOCKED,
        LOADED
    }
    State[] mIccState = {State.UNKNOWN, State.UNKNOWN};
    boolean[] mCardLoaded = {false, false};

    /* to-pass-build, Xinyu Liu/dcjf34 */ 
    private static final String ACTION_SECONDARY_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED_2";

    // the first ACTION_SIM_STATE_CHANGED always follows ACTION_ICC_PRECACHE_STATUE_CHANGED
    // we can ignore the first one, but the second one means a "refresh" command, we need load SIM again
    // so the strategy is : load SIM again if there is 2nd ACTION_SIM_STATE_CHANGED comes after this time load start
    boolean[] mCardRefresh = {false, false};  // flag to indicates if need refresh
    int[] mLoadedCount = {0, 0};  // the times we get ACTION_SIM_STATE_CHANGED "LOADED" intent

    // array index
    static int CDMA = 0;
    static int GSM = 1;

    static final String OPCODE = "op";

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "Toast..., prompt end user wait while clean up DB");
            Toast.makeText(LoadSimContactsService.this, R.string.wait_contacts_db_text, Toast.LENGTH_LONG).show();
        }
    };

    public void registerExternalSIMListener() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED) ||
                            /* to-pass-build, Xinyu Liu/dcjf34 */ 
                            action.equals(ACTION_SECONDARY_SIM_STATE_CHANGED) ||
                            action.equals(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED) ||
                            action.equals(PhoneModeManager.ACTION_SECONDARY_ICC_PRECACHE_STATUS_CHANGED)) {
                        updateSimState(intent);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            iFilter.addAction(ACTION_SECONDARY_SIM_STATE_CHANGED); //Extended for secondary card
            iFilter.addAction(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED);
            iFilter.addAction(PhoneModeManager.ACTION_SECONDARY_ICC_PRECACHE_STATUS_CHANGED); //Extended for secondary card

            registerReceiver(mIntentReceiver, iFilter);
        }
    }

    private final void updateSimState(Intent intent) {
        int index;
        String action = intent.getAction();
        String stateExtra = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);

        Log.v(TAG, "updateSimState, action = " + action);
        Log.v(TAG, "updateSimState, stateExtra = " + stateExtra);

        int defaultType = TelephonyManager.getDefault().getPhoneType();
        if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
            || action.equals(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED)) {
            if (defaultType == TelephonyManager.PHONE_TYPE_GSM)
                index = GSM;
            else 
                index = CDMA;            
        } else {
            if (defaultType == TelephonyManager.PHONE_TYPE_GSM)
                index = CDMA;
            else 
                index = GSM;        	
        }

        if (stateExtra.equals(IccCard.INTENT_VALUE_ICC_LOADED)) {
            mIccState[index] = State.LOADED;
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED) 
                    /* to-pass-build, Xinyu Liu/dcjf34 */ 
                    || action.equals(ACTION_SECONDARY_SIM_STATE_CHANGED)
                    ) {
                mLoadedCount[index]++;
                if (mLoadedCount[index] > 1) {  // ignore the first one
                    mCardRefresh[index] = true;
                }
                Log.v(TAG, "mLoadedCount[index] = " + mLoadedCount[index]);
            }
        } else if (IccCard.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            mIccState[index] = State.ABSENT;
        } else if (IccCard.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            mIccState[index] = State.LOCKED;
        }  else {
            mIccState[index] = State.UNKNOWN;
        }
    }

    private boolean checkSimCardDetected(int type) {
        //Log.v(TAG, "checkSimCardDetected, type = "+type+", _CDMA="+TelephonyManager.PHONE_TYPE_CDMA+", _GSM="+TelephonyManager.PHONE_TYPE_GSM);
        // don't cache the check result, maybe it is pin unlock case, so need check every time.
        return waitSimCardDetected(type);
    }

    private boolean waitSimCardDetected(int type)
    {
        Log.v(TAG, "begin waitSimCardDetected");
        State cardState = State.UNKNOWN;
        int retry = 0;

        while(retry < MAX_DETECT_RETRY_TIMES)
        {
            if (type == TelephonyManager.PHONE_TYPE_CDMA) {
                cardState = mIccState[CDMA];
            }
            else if (type == TelephonyManager.PHONE_TYPE_GSM) {
                cardState = mIccState[GSM];
            }
            else {
                Log.v(TAG, "incorrect type, return");
                return false;
            }

            Log.v(TAG, "cardState =  "+cardState);
            
            if(cardState == State.ABSENT || cardState == State.LOCKED) {
                Log.v(TAG, "intent report SIM state is absent/locked, try times is : " + retry);
                return false;
            }
            // sometimes we need actively query the card state
            else if (SimUtility.isSimBlocked(type)) {
                Log.v(TAG, "detect SIM state is absent/locked, try times is : " + retry);
                return false;
            }
            else {
                SimUtility.querySimType(getContentResolver(), type);
                if (SimUtility.isSimReady(type)) {  
                    return true;
                }
                else {
                    Log.v(TAG, "sim card is not ready, try it again!");
                    try {
                        Thread.sleep(2000); // sleep 2 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    retry++;
                }
            }
        }
        return false;
    }

    private String waitSimCardIccId(int slotPhoneType)
    {
        Log.v(TAG, "begin waitSimCardIccId");
        String iccId = null;        
        int retry = 0;    	
        
        while(retry < MAX_DETECT_RETRY_TIMES)
        {
            iccId = SimUtility.getIccId(slotPhoneType);
            if (!TextUtils.isEmpty(iccId)) {
                Log.v(TAG, "detect SIM card iccid, try times is : " + retry + ", iccid = " + iccId);
                return iccId;
            }
            else {
                Log.v(TAG, "sim card iccid is not ready, try times is : " + retry);
                try {
                    Thread.sleep(2000); // sleep 2 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retry++;
            }
        }
        return null;
    }

    public boolean isSimCardChanged(int slotPhoneType)
    {
        String iccId;
        String savediccId;

        if (slotPhoneType == TelephonyManager.PHONE_TYPE_NONE) {
            iccId = null;
        } else {
            iccId = waitSimCardIccId(slotPhoneType);
        }

        // for USIM unlock case, the iccid is still null once unlock. reload SIM need be called.
        // if (iccId == null) iccId = "";
        if (iccId == null)
            return true;   // as we can not determine the iccid, always regard it is changed.

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String prefsKey = "SAVE_ICCID" + slotPhoneType;
        savediccId = prefs.getString(prefsKey, "");

        Log.v(TAG, "type = " + slotPhoneType + "iccid = " + iccId);
        Log.v(TAG, "type = " + slotPhoneType + "savediccId = " + savediccId);

        // if one of the id is empty, regard it is changed
        if (TextUtils.isEmpty(savediccId) || TextUtils.isEmpty(iccId))
            return true;

        // only savediccId == iccId; and != null, return false;
        return !TextUtils.equals(savediccId, iccId);
    }


    public void setICCID(int slotPhoneType)
    {
        String iccId;

        if (slotPhoneType == TelephonyManager.PHONE_TYPE_NONE) {
            iccId = null;
        } else {
			      iccId = SimUtility.getIccId(slotPhoneType);
        }
        if (iccId == null)
            iccId = "";
        setICCID(slotPhoneType, iccId);
        return;
    }

    public void setICCID(int slotPhoneType, String iccid)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String prefsKey = "SAVE_ICCID" + slotPhoneType;
        Log.v(TAG, "type = " + slotPhoneType + " iccid = " + iccid);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(prefsKey, iccid);
        editor.commit();
        return;
    }

    // clean SIM DB before reload C/G contacts, need consider the performance when delete bulk contacts
    public void cleanSimContacts() {

        boolean b_clean_c = false;
        boolean b_clean_g = false;

        if (PhoneModeManager.isDmds()) {
            if (!checkSimCardDetected(TelephonyManager.PHONE_TYPE_CDMA)) {
                b_clean_c = true;
            }
            else if (isSimCardChanged(TelephonyManager.PHONE_TYPE_CDMA)) {
                b_clean_c = true;
            }
            if (!checkSimCardDetected(TelephonyManager.PHONE_TYPE_GSM)) {
                b_clean_g = true;
            }
            else if (isSimCardChanged(TelephonyManager.PHONE_TYPE_GSM)) {
                b_clean_g = true;
            }
        }
        else {
            int type = TelephonyManager.getDefault().getPhoneType();
            boolean b_clean = false;
            if (!checkSimCardDetected(type)) {
                b_clean = true;
            }
            else if (isSimCardChanged(type)) {
                b_clean = true;
            }
            if (type == TelephonyManager.PHONE_TYPE_CDMA)
                b_clean_c = b_clean;
            else if (type == TelephonyManager.PHONE_TYPE_GSM)
                b_clean_g = b_clean;
        }
        
        boolean CMCC_OT = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs != null) {
            CMCC_OT = prefs.getBoolean("persist.mot.cmcc_ot", false);
        }
        if (CMCC_OT) {
            // IMPORTANT: Must always clean SIM contacts if this is CMCC OT build
            b_clean_g = true;
        }
        Log.d(TAG, "cleanSimContacts b_clean_c = " + b_clean_c + " b_clean_g = " + b_clean_g);

        if (b_clean_c || b_clean_g) {
            mToastHandler.sendMessage(mToastHandler.obtainMessage(0));
            try {
                Thread.sleep(200); // sleep 200ms to let Toast show before the long time function deleteSIMAccountMembers()
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

              if (b_clean_c)
                SimUtility.deleteSIMAccountMembers(getContentResolver(), TelephonyManager.PHONE_TYPE_CDMA);
              if (b_clean_g)
                SimUtility.deleteSIMAccountMembers(getContentResolver(), TelephonyManager.PHONE_TYPE_GSM);
        }
        // clean the SIM contacts marked as "deleted" in phone DB before loade SIM contacts,
        // otherwise the syncDeltaSimContacts() maybe find the "delete" contacts
        SimUtility.deleteAccountMembersMarked(getContentResolver(), HardCodedSources.ACCOUNT_TYPE_CARD);
        return;
    }

    public void loadSimFinished() {
        // set the Local/C/G contacts be "visible" by default
        SimUtility.setAccountsDefaultVisible(getContentResolver());

        // notify the loading sim finished
        SimUtility.setSIMLoadStatus(true);
        
        return;
    }


    public void preloadAccounts()
    {
        AccountManager am = AccountManager.get(mContext);

        // Create local account for on-phone contacts
        Account[] accounts_local = am.getAccountsByType(HardCodedSources.ACCOUNT_TYPE_LOCAL);
        Log.v(TAG, "preloadAccounts(), local = "+accounts_local);
        if (accounts_local == null || accounts_local.length <= 0) {
            addLocalAccount(HardCodedSources.ACCOUNT_LOCAL_DEVICE);
        } else {           
            for (Account account : accounts_local) {
                if (ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY) <= 0) {
                    ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
                }
            }
        }

        // Create C/G account for card contacts
        Account[] accounts_card = am.getAccountsByType(HardCodedSources.ACCOUNT_TYPE_CARD);
        if (accounts_card == null || accounts_card.length <= 0) {
	        Log.v(TAG, "no card account, adding C or G");
            if (PhoneModeManager.isDmds()) {
                    addCardAccount(HardCodedSources.ACCOUNT_CARD_C);
                    addCardAccount(HardCodedSources.ACCOUNT_CARD_G);
            } else {
                addCardAccount(HardCodedSources.ACCOUNT_CARD);
            }
        } 
    }

    public void addLocalAccount(String accountname) {
        // Create a description of the new account
        Bundle options = new Bundle();
        options.putString(LocalAuthenticatorService.OPTIONS_USERNAME, accountname);
        options.putString(LocalAuthenticatorService.OPTIONS_PASSWORD, "titanium");

        // Here's where we tell AccountManager about the new account.  The addAccount
        // method in AccountManager calls the addAccount method in our authenticator
        // service (EasAuthenticatorService)
        AccountManager.get(mContext).addAccount(HardCodedSources.ACCOUNT_TYPE_LOCAL, null, null,
                options, null, null, null);
        Log.v(TAG, "addLocalAccount(): "+ accountname+" done !");       
    }

    public void addCardAccount(String accountname) {
        // Create a description of the new account
        Bundle options = new Bundle();
        options.putString(CardAuthenticatorService.OPTIONS_USERNAME, accountname);
        options.putString(CardAuthenticatorService.OPTIONS_PASSWORD, "titanium");

        // Here's where we tell AccountManager about the new account.  The addAccount
        // method in AccountManager calls the addAccount method in our authenticator
        // service (EasAuthenticatorService)
        AccountManager.get(mContext).addAccount(HardCodedSources.ACCOUNT_TYPE_CARD, null, null,
                options, null, null, null);
        Log.v(TAG, "addCardAccount(): "+ accountname+" done !");             
    }


    @Override
    public void onCreate() {

        Log.v(TAG, "onCreate()");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("LoadSimContactsService", Process.THREAD_PRIORITY_BACKGROUND);
        // HandlerThread thread = new HandlerThread("LoadSimContactsService");
        // thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

        mServiceLooper = thread.getLooper();
        if (mServiceLooper == null) {
            Log.e(TAG, "Creating LoadSimContactsService error");
            return;
        }
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mContext = getBaseContext();
        registerExternalSIMListener();
        sInstance = this;
        // create Local/C/G acount
        preloadAccounts();
        // start the load, it is useful for unlock case.
        SimUtility.setSIMLoadStatus(false);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.v(TAG, "Starting LoadSimContactsService");

        // onStart() method can be passed a null intent, for example, re-start service after process crash
        if (intent == null) {
            mServiceHandler.sendMessage(mServiceHandler.obtainMessage(EVENT_LOAD_ALL_SIM_CONTACTS, startId, 0));
            return;
        }

        Bundle args = intent.getExtras();
        if (args == null) {
            mServiceHandler.sendMessage(mServiceHandler.obtainMessage(EVENT_LOAD_ALL_SIM_CONTACTS, startId, 0));
        }
        else {
            mServiceHandler.sendMessage(mServiceHandler.obtainMessage(EVENT_LOAD_ONE_SIM_CONTACTS, startId, args.getInt(OPCODE)));
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroying LoadSimContactsService");

        loadSimFinished();
        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
        sInstance = null;
        if (mIntentReceiver != null) {
            unregisterReceiver(mIntentReceiver);
            mIntentReceiver = null;
        }
    }

    public static LoadSimContactsService getInstance() {
        return sInstance;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming requests.
         */
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "Handling incoming message: " + msg);

            switch (msg.what) {
                case EVENT_QUIT:
                    getLooper().quit();
                    return;

                case EVENT_LOAD_ALL_SIM_CONTACTS: {
                    // check SIM before load SIM contacts to phone DB, clean up the phone DB if the card is not available
                    // or the card has been changed, as the loadSimContacts() costs time, do both C/G check before load SIM
                    cleanSimContacts();
                    if (PhoneModeManager.isDmds()) {
                        Log.v(TAG, "Load All SIM ");
                        loadSimContacts(TelephonyManager.PHONE_TYPE_CDMA);  //load C card
                        loadSimContacts(TelephonyManager.PHONE_TYPE_GSM);  //load G card
                    } else {
                        Log.v(TAG, "Load C or G card SIM ");
                        int type = TelephonyManager.getDefault().getPhoneType();
                        loadSimContacts(type);
                    }
                    // delete all the local contacts which has makrked as "delete" since it never sync with server
                    SimUtility.deleteAccountMembersMarked(getContentResolver(), HardCodedSources.ACCOUNT_TYPE_LOCAL);
                    stopSelf(msg.arg1);
                    return;
                }
                case EVENT_LOAD_ONE_SIM_CONTACTS:
                    loadSimContacts(msg.arg2);  //load one card
                    stopSelf(msg.arg1);
                    return;
                default:
                    Log.w(TAG, "what=" + msg.what);
                    return;
            } // end switch case
        }// end handleMessage


        private void loadSimContacts(int type)
        {
            Log.v(TAG, "begin loadSimContacts, type = " + type);
            // check if it has been loaded by another onStart() before (in pin unlock case)
            int index = 0;
            if (type == TelephonyManager.PHONE_TYPE_CDMA) index = CDMA;
            else if(type == TelephonyManager.PHONE_TYPE_GSM) index = GSM;
            else return;   // incorrect type

            if (mCardLoaded[index]) {
                // if it is UTK refresh case, need load again even it has been loaded before
                if (mCardRefresh[index]) {  // force refresh
                    Log.v(TAG, "force to refresh ");
                }
                else {
                    Log.v(TAG, "already loaded, return");
                    return;
                }
            }

            // reset the "refresh" flag, it will be set to true if there is new intent comes
            mCardRefresh[index] = false;
            mCardLoaded[index] = false;

            SimUtility.resetSimType(type);
            if (!checkSimCardDetected(type)) {
                // reset the iccid to empty, so next time when card be inserted, it will reload sim contacts
                setICCID(type, "");
                SimUtility.deleteSIMAccountMembers(getContentResolver(), type);
                return;
            }
            SimUtility.querySimType(getContentResolver(), type);

            boolean isSlotChanged = isSimCardChanged(type);
            boolean loadSuccess = false;

            if (isSlotChanged) {
                Log.v(TAG, "reloadAllSimContacts, type = " + type);
                loadSuccess = reloadAllSimContacts(type);
            }
            else {
                Log.v(TAG, "syncDeltaSimContacts, type = " + type);
                // can not skip, as the unlock sim pin will run to here, need query it.
                loadSuccess = syncDeltaSimContacts(type);
            }

            mCardLoaded[index] = loadSuccess;
            setICCID(type);     // update iccid a little later as we may get null if try too early

            Log.v(TAG, "end loadSimContacts, type = " + type);
        }

        private boolean reloadAllSimContacts(int type)
        {
            Log.v(TAG, "begin reloadAllSimContacts, type = " + type);
            Log.v(TAG, "start query SIM contacts....");

            //1. delete all contacts in Phone SIM GROUP before load
            SimUtility.deleteSIMAccountMembers(getContentResolver(), type);

            //2. read SIM contacts from sim cards
            ArrayList<SimUtility.PeopleInfo> simList =
                new ArrayList<SimUtility.PeopleInfo>();
            if(!SimUtility.getSimCardContacts(
                        getContentResolver(),
                        simList, type))
            {
                Log.i(TAG, "read sim card failed!");
                simList.clear();
                return false;
            }

            // 3. add all sim contacts
            SimUtility.bulkInsertSimContacts(getContentResolver(), simList, type);

            Log.v(TAG, "end reloadAllSimContacts, type = " + type);
            return true;
        }

        private boolean syncDeltaSimContacts(int type) {

            Log.v(TAG, "begin syncDeltaSimContacts, type = " + type);

            //1. read SIM contacts from sim cards
            ArrayList<SimUtility.PeopleInfo> simList =
                new ArrayList<SimUtility.PeopleInfo>();
            if(!SimUtility.getSimCardContacts(getContentResolver(), simList, type)) {
                Log.i(TAG, "read sim card failed!");
                simList.clear();
                SimUtility.deleteSIMAccountMembers(getContentResolver(), type);
                return false;
            }

            //2. read SIM members from phone db
            ArrayList<SimUtility.PeopleInfo> peopleList =
                new ArrayList<SimUtility.PeopleInfo>();
            Log.i(TAG, "query sim member  =================== start!");
            SimUtility.getSIMAccountMembers(getContentResolver(), type, peopleList);

            //3. diff the two list
            Log.i(TAG, "before diff");
            Log.i(TAG, "sim list");
            SimUtility.printPeopleInfo(simList);
            Log.i(TAG, "phone list");
            SimUtility.printPeopleInfo(peopleList);

            SimUtility.diffPeopleInfoList(simList, peopleList);

            Log.i(TAG, "end diff");
            Log.i(TAG, "sim list");
            SimUtility.printPeopleInfo(simList);
            Log.i(TAG, "phone list");
            SimUtility.printPeopleInfo(peopleList);

            //4. update the delta to phone db
            Iterator<SimUtility.PeopleInfo> it = peopleList.iterator();
            while(it.hasNext())
            {
                SimUtility.PeopleInfo info = (SimUtility.PeopleInfo) it.next();
                if(info.diffStatus == SimUtility.DIFF_INIT)
                {
                    //delete it from phone db
                    SimUtility.deleteContactInDB(getContentResolver(), info.peopleId);
                }
            }

            // let's use the bulkInsert() to add the new contacts
            ArrayList<SimUtility.PeopleInfo> bulkList =
                new ArrayList<SimUtility.PeopleInfo>();
            it = simList.iterator();
            while(it.hasNext())
            {
                SimUtility.PeopleInfo info = (SimUtility.PeopleInfo) it.next();
                if(info.diffStatus == SimUtility.DIFF_NEW)
                {
                    bulkList.add(info);
                }
            }
            if(bulkList.size() > 0)
            {
                SimUtility.bulkInsertSimContacts(getContentResolver(), bulkList, type);
            }

            Log.v(TAG, "end syncDeltaSimContacts, type = " + type);
            return true;
        }

    }// end class ServiceHandler
}
