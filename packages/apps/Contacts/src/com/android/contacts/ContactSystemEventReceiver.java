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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCard;
import android.telephony.TelephonyManager;
import com.motorola.android.telephony.PhoneModeManager;

public class ContactSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "ContactSystemEventReceiver";

    private static boolean sLoadAll = false;   // make sure always load all firstly to clean up SIM contacts

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Intent received: " + intent);

        String action = intent.getAction();
        if (context.getContentResolver() == null || action == null) {
            Log.e(TAG, "FAILURE unable to get content resolver. Contacts SIM load inactive.");
            return;
        }

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            //TODO check the sim/usim card status
            //XXX
            if (!sLoadAll) {
                context.startService(new Intent(context, LoadSimContactsService.class));
                sLoadAll = true;
            }
        }
        else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                 || action.equals("android.intent.action.SIM_STATE_CHANGED_2") /* to-pass-build, Xinyu Liu/dcjf34 */ 
                 || action.equals(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED)
                 || action.equals(PhoneModeManager.ACTION_SECONDARY_ICC_PRECACHE_STATUS_CHANGED)) {
                updateSimState(context, intent);
        }
        Log.v(TAG, "onReceive() returns, action = "+action);
    }

    private final void updateSimState(Context context, Intent intent) {

        String stateExtra = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
        int defaultType = TelephonyManager.getDefault().getPhoneType();
        int secondaryType = TelephonyManager.PHONE_TYPE_NONE;
        if (defaultType == TelephonyManager.PHONE_TYPE_GSM) {
            secondaryType = TelephonyManager.PHONE_TYPE_CDMA;
        } else if (defaultType == TelephonyManager.PHONE_TYPE_CDMA) {
            secondaryType = TelephonyManager.PHONE_TYPE_GSM;
        } else {
            return;
        }        

        Log.v(TAG, "updateSimState, stateExtra = " + stateExtra);

        if (stateExtra.equals(IccCard.INTENT_VALUE_ICC_LOADED)) {
            Bundle args = new Bundle();
            String action = intent.getAction();
                    
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                || action.equals(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED)) {
                args.putInt(LoadSimContactsService.OPCODE, defaultType);
            }
            else 
                args.putInt(LoadSimContactsService.OPCODE, secondaryType);

            if (!sLoadAll) {
                context.startService(new Intent(context, LoadSimContactsService.class));
                sLoadAll = true;
            }
            else {
                // always start service to query SIM contacts when receive "LOADED" intent.
                // it will not load again if the first load is ok
                context.startService(new Intent(context, LoadSimContactsService.class).putExtras(args));
            }
        } else if (stateExtra.equals(IccCard.INTENT_VALUE_ICC_ABSENT)) {
            // Support SIM hot plug
            Bundle args = new Bundle();
            String action = intent.getAction();
                    
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                || action.equals(PhoneModeManager.ACTION_ICC_PRECACHE_STATUS_CHANGED)) {
                args.putInt(LoadSimContactsService.OPCODE, defaultType);
            }
            else 
                args.putInt(LoadSimContactsService.OPCODE, secondaryType);

            context.startService(new Intent(context, LoadSimContactsService.class).putExtras(args));
        }
    }

}
