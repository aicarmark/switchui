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

import com.android.contacts.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsSearchManager;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.R;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.util.PhoneCapabilityTester;

import com.motorola.contacts.detail.ContactDetailPartialFieldPickerFragment;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.ArrayList;

public class ContactDetailPartialFieldPickerActivity extends ContactsActivity {
    private static final String TAG = "ContactDetailPartialFieldPickerActivity";

    private ContactDetailPartialFieldPickerFragment mFragment;
    private ContactLoaderFragment mLoaderFragment;
    private ContactLoader.Result mContactData;
    private Uri mLookupUri = null;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.moto_contact_detail_partial_field_picker_activity);

        mFragment = (ContactDetailPartialFieldPickerFragment) getFragmentManager().findFragmentById(
                R.id.contact_detail_fragment);
        mLoaderFragment = (ContactLoaderFragment) getFragmentManager().findFragmentById(
                R.id.loader_fragment);
        mLoaderFragment.setRetainInstance(true);
        mLoaderFragment.setListener(mLoaderFragmentListener);
        mLoaderFragment.loadUri(getIntent().getData(), true); //IKHSS7-2028
        Log.i(TAG, getIntent().getData().toString());
    }

    private final ContactLoaderFragmentListener mLoaderFragmentListener =
            new ContactLoaderFragmentListener() {
        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onDetailsLoaded(final ContactLoader.Result result) {
            if (result == null) {
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    mLoaderFragment.setHasOptionsMenu(false);
                    if(mLookupUri == null){//setData only once
                        mContactData = result;
                        mLookupUri = result.getLookupUri();
                        // Reset contact data
                        if (mFragment != null) {
                            mFragment.setData(mLookupUri, mContactData);
                            //MOT MOD BEGIN - IKPIM-899, show display name and company at ActionBar if it's phone
                            if (!PhoneCapabilityTester.isUsingTwoPanes(ContactDetailPartialFieldPickerActivity.this)) {
                                ActionBar actionBar = getActionBar();
                                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                                actionBar.setTitle(mContactData.getDisplayName());
                                actionBar.setSubtitle(
                                      ContactDetailDisplayUtils.getCompany(ContactDetailPartialFieldPickerActivity.this,
                                      mContactData));
                            }
                            //MOT MOD END - IKPIM-899
                        }
                   }
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
        }
    };

}
