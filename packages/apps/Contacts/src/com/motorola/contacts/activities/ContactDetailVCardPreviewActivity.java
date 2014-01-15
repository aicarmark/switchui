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

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsSearchManager;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.R;
import com.android.contacts.vcard.ImportVCardActivity;
import com.android.contacts.util.PhoneCapabilityTester;

import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.vcard.VCardEntry.OrganizationData;

import com.motorola.contacts.detail.ContactDetailVCardPreviewFragment;
import com.motorola.contacts.vcard.PreviewRequest;
import com.motorola.contacts.vcard.VCardPreviewUtils;

import com.android.vcard.VCardEntry;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ContactDetailVCardPreviewActivity extends ContactsActivity {
    private static final String TAG = "ContactDetailVCardPreviewActivity";

    private static final String KEY_VCARD_PREVIEW_REQUEST = "vcardPreviewRequest";
    private static final String KEY_CURRENT_VCARD_STR = "currentVCardStr";
    private static final String KEY_CURRENT_VCARD_ENTRY_INDEX = "currentVCardEntryIndex";
    private ContactDetailVCardPreviewFragment mFragment = null;

//MOTO MOD BEGIN
    private PreviewRequest mPreviewRequest = null;
    private VCardEntry mCurrentVCardEntry = null;
    private String mCurrentVCardStr = null;
    private int mCurrentVCardEntryIndex = 0;
    private Uri mData = null;
    private ProgressDialog mProgressDialog = null;
    private AsyncTask mVCardParseTask = null;
    private MenuItem mMenuBack = null;
    private MenuItem mMenuNext = null;
    private boolean mNeedSettingMenu= false;
//MOTO MOD END

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

//MOTO MOD BEGIN
        if(savedState != null){
            mPreviewRequest = savedState.getParcelable(KEY_VCARD_PREVIEW_REQUEST);
            mCurrentVCardStr = savedState.getString(KEY_CURRENT_VCARD_STR);
            mCurrentVCardEntryIndex = savedState.getInt(KEY_CURRENT_VCARD_ENTRY_INDEX);
            if(mCurrentVCardStr != null){
                mCurrentVCardEntry = VCardPreviewUtils.parseVCardStr(ContactDetailVCardPreviewActivity.this, mPreviewRequest, mCurrentVCardStr);
                previewVCard();
                return;
            }
        }

        final Intent intent = getIntent();
        mData = intent.getData();
        if (mData == null) {
            Log.e(TAG,"Unable to extract File Uri "
                    + intent);
            finish();
            return;
        }

        showProgressDialog();
        if(mVCardParseTask == null){
            mVCardParseTask = new VCardParseTask();
        }
        mVCardParseTask.execute((Object[])null);
//MOTO MOD END

        Log.i(TAG, getIntent().getData().toString());
    }

//MOTO MODE BEGIN
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_VCARD_PREVIEW_REQUEST, mPreviewRequest);
        outState.putString(KEY_CURRENT_VCARD_STR, mCurrentVCardStr);
        outState.putInt(KEY_CURRENT_VCARD_ENTRY_INDEX, mCurrentVCardEntryIndex);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Enter onDestroy()");
        dismissProgressDialog();
        cancelVCardParseTask();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.moto_vcard_preview, menu);

        mMenuBack = menu.findItem(R.id.menu_back);
        mMenuNext = menu.findItem(R.id.menu_next);
        if(mCurrentVCardEntryIndex == 0){
            mMenuBack.setEnabled(false);
        }
        if(mPreviewRequest != null && mCurrentVCardEntryIndex == (mPreviewRequest.entryCount -1)){
            mMenuNext.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_back: {
                if( mCurrentVCardEntryIndex > 0){
                    mCurrentVCardEntryIndex--;
                    if(mCurrentVCardEntryIndex == 0){
                        mMenuBack.setEnabled(false);
                    }
                    if(!mMenuNext.isEnabled()){
                        mMenuNext.setEnabled(true);
                    }
                    showProgressDialog();
                    cancelVCardParseTask();
                    mVCardParseTask = new VCardParseTask();
                    mVCardParseTask.execute((Object[])null);
                }
                return true;
            }
            case R.id.menu_next: {
                if( mCurrentVCardEntryIndex < mPreviewRequest.entryCount - 1){
                    mCurrentVCardEntryIndex++;
                    if(mCurrentVCardEntryIndex == mPreviewRequest.entryCount - 1){
                        mMenuNext.setEnabled(false);
                    }
                    if(!mMenuBack.isEnabled()){
                        mMenuBack.setEnabled(true);
                    }
                    showProgressDialog();
                    cancelVCardParseTask();
                    mVCardParseTask = new VCardParseTask();
                    mVCardParseTask.execute((Object[])null);
                }
                return true;
            }
        }
        return false;
    }

    private void previewVCard() {
        if (mCurrentVCardEntry == null) {
            Toast.makeText(ContactDetailVCardPreviewActivity.this,
                    R.string.not_able_view_save_vcf,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(mFragment == null){
            setContentView(R.layout.moto_contact_detail_vcard_preview_activity);

            mFragment = (ContactDetailVCardPreviewFragment) getFragmentManager().findFragmentById(
                    R.id.contact_detail_fragment);
        }
        //MOT MOD BEGIN - IKPIM-899, show display name and company name at ActionBar if it's phone
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(mCurrentVCardEntry.getDisplayName());
            List<OrganizationData> organizationList = mCurrentVCardEntry.getOrganizationList();
            String companyName;
            if (organizationList != null && organizationList.size() != 0) {
                String company = organizationList.get(0).getOrganizationName();
                String title = organizationList.get(0).getTitle();
                if (TextUtils.isEmpty(company)) {
                    companyName = title;
                } else if (TextUtils.isEmpty(title)) {
                    companyName = company;
                } else {
                    companyName = (ContactDetailVCardPreviewActivity.this).getString(
                            R.string.organization_company_and_title, company, title);
                }
                actionBar.setSubtitle(companyName);
            } else {
                actionBar.setSubtitle(null);
            }
        }
        //MOT MOD END - IKPIM-899
        mFragment.previewVCard(mCurrentVCardEntry);
    }

    private synchronized void showProgressDialog() {
        if(mProgressDialog == null){
            mProgressDialog = ProgressDialog.show(ContactDetailVCardPreviewActivity.this, "",
                    getString(R.string.reading_vcard_message), true);

            // Permit user press backkey to exit wait state
            mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (KeyEvent.KEYCODE_BACK == keyCode) {
                        dismissProgressDialog();
                        cancelVCardParseTask();
                        if (!isFinishing()) {
                            finish();
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }else{
            mProgressDialog.show();
        }
    }

    private synchronized void dismissProgressDialog() {
        if (mProgressDialog != null && !isFinishing()) {
            Log.d(TAG, "Start to dismiss progressDialog.");
            mProgressDialog.dismiss();
        }
    }

    private synchronized void cancelVCardParseTask() {
        if (mVCardParseTask != null && !mVCardParseTask.isCancelled()) {
            mVCardParseTask.cancel(true);
        }
    }

    public  void importVCard() {
        Intent intent = new Intent("com.motorola.action.import3rd");
        ArrayList<String> mSelectedPathList = new ArrayList<String>();
        //add by txbv34 for IKCBSMMCPPRC-1499
        if(mData == null){
        	Log.d(TAG, "importVCard: mData == null");
        	return;	
        }
        if (mData.getScheme().equals("file")){
            mSelectedPathList.add(mData.getPath());
            intent.putStringArrayListExtra("path_list", mSelectedPathList);
        }else {
            intent.setDataAndType(mData, "text/vcard");
        }
        intent.putExtra("contacts", true);
        startActivity(intent);
    }

    private class VCardParseTask extends AsyncTask<Void, Void, VCardEntry> {
        protected VCardEntry doInBackground(Void... params) {
            Log.d(TAG, "Enter doInBackground(): start parseVCard.");
            if(mPreviewRequest == null){
                mPreviewRequest = VCardPreviewUtils.constructPreviewRequest(ContactDetailVCardPreviewActivity.this, mData);
                if(!mNeedSettingMenu){//need to set the menu "back" and "next" state when constructing PreviewRequest for the 1st time.
                    mNeedSettingMenu = true;
                }
            }

            if(mPreviewRequest != null){
                mCurrentVCardStr = VCardPreviewUtils.getVCardStrAt(ContactDetailVCardPreviewActivity.this, mPreviewRequest.uri, mCurrentVCardEntryIndex);
                mCurrentVCardEntry = VCardPreviewUtils.parseVCardStr(ContactDetailVCardPreviewActivity.this, mPreviewRequest, mCurrentVCardStr);
            }else{
                Log.e(TAG, "error in retrieve mPreviewRequest when parsing vcard file " + mData);
            }
            return mCurrentVCardEntry;
        }

        protected void onPostExecute(VCardEntry result) {
            Log.d(TAG,"Enter onPostExecute().");
            if(mNeedSettingMenu){//set the menu "back" and "next" state only for the 1st time
                if(mCurrentVCardEntryIndex == 0 && mMenuBack != null){
                    mMenuBack.setEnabled(false);
                }
                if(mPreviewRequest != null && mCurrentVCardEntryIndex == (mPreviewRequest.entryCount -1) && mMenuNext!=null){
                    mMenuNext.setEnabled(false);
                }
                mNeedSettingMenu = false;
            }
            dismissProgressDialog();
            previewVCard();
        }
    }

//MOTO MODE END
}
