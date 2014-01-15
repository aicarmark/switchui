/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.contacts.interactions.PhoneNumberInteraction;
import com.android.contacts.util.Constants;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
// BEGIN Motorola, FTR 36344, IKHSS6-8444
import com.motorola.contacts.util.MEDialer;
// END IKHSS6-8444

/**
 * An interstitial activity used when the user selects a QSB search suggestion using
 * a call button.
 */
public class CallContactActivity extends ContactsActivity implements OnDismissListener {

    public static final String CALL_ORIGIN_CALLCONTACT = "com.android.contacts.CallContactActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri contactUri = getIntent().getData();
        if (contactUri == null) {
            finish();
        }
        // If this method is being invoked with a saved state, rely on Activity
        // to restore it
        if (savedInstanceState != null) {
            return;
        }

        if (Contacts.CONTENT_ITEM_TYPE.equals(getContentResolver().getType(contactUri))) {
            // BEGIN Motorola, FTR 36344, IKHSS6-8444
            PhoneNumberInteraction.startInteractionForPhoneCall(this, contactUri,
                    CALL_ORIGIN_CALLCONTACT, MEDialer.DialFrom.FAVORITES);
            // END IKHSS6-8444
        } else {
            // BEGIN Motorola, FTR 36344, IKHSS6-8444
            final Intent newIntent;
            if (ContactsUtils.haveTwoCards(getApplicationContext())) {
                newIntent = new Intent(ContactsUtils.TRANS_DIALPAD);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra("phoneNumber", contactUri.getLastPathSegment());
            } else {
                newIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED, contactUri);
            }
            MEDialer.onDial(this, newIntent, MEDialer.DialFrom.FAVORITES);
            startActivity(newIntent);
            // END IKHSS6-8444
            finish();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!isChangingConfigurations()) {
            finish();
        }
    }
}
