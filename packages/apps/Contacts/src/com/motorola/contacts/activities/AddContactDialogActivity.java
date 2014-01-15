/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.motorola.contacts.activities;

import com.motorola.contacts.list.AddContactDialogFragment;
import com.android.contacts.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Contacts.Intents.Insert;
import android.os.Bundle;
import android.util.Log;

public class AddContactDialogActivity extends Activity {
    private static final String TAG = "AddContactDialogActivity";
    private AddContactDialogFragment mFragment;


    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        //MOT MOD BEGIN IKHSS6UPGR-9089
        boolean hasPhone = bundle.keySet().contains(Insert.PHONE);
        boolean hasEmail = bundle.keySet().contains(Insert.EMAIL);
        String data = null;
        if(hasPhone) {
            data = bundle.getString(Insert.PHONE);
        } else if(hasEmail) {
            data = bundle.getString(Insert.EMAIL);
        }
        //MOT MOD END IKHSS6UPGR-9089

        AddContactDialogFragment prev = (AddContactDialogFragment)getFragmentManager().findFragmentByTag("dialog");
        if(prev == null) {
            //MOT MOD BEGIN IKHSS6UPGR-9089
            mFragment = AddContactDialogFragment.newInstance(data, hasPhone);
            //MOT MOD END IKHSS6UPGR-9089
            mFragment.show(getFragmentManager(), "dialog");
        }
    }
}
