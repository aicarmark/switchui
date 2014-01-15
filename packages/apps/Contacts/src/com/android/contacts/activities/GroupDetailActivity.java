/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.group.GroupDetailDisplayUtils;
//import com.android.contacts.group.GroupDetailFragment; //MOT MOD - IKPIM-991: Group redesign feature on ics
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.util.Constants;

//MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
import com.motorola.contacts.group.LocalGroupDetailFragment;
import com.motorola.contacts.group.LocalGroupUtils;
import com.motorola.contacts.activities.GroupConfirmActivity;
import com.motorola.contacts.groups.GroupAPI;
//MOT MOD END - IKPIM-991

import android.app.ActionBar;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.ActivityNotFoundException;

public class GroupDetailActivity extends ContactsActivity {

    private static final String TAG = "GroupDetailActivity";
    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    private static final String MIME_SMS_ADDRESS = "vnd.android.cursor.item/sms-address";
    private static final String MIME_EMAIL_ADDRESS = "vnd.android.cursor.item/email_v2";
    private static final String MIME_CONTACT_EVENT = "vnd.android.cursor.item/contact_event";
    private static final int REQUEST_CONFIRM_GROUP_SMS = 1001;
    private static final int REQUEST_CONFIRM_GROUP_EMAIL = 1002;
    private static final int REQUEST_CONFIRM_GROUP_EVENT = 1003;
    //MOT MOD END - IKPIM-991

    private boolean mShowGroupSourceInActionBar;

    private String mAccountTypeString;
    private String mDataSet;

    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    private Intent mIntent;
    //private GroupDetailFragment mFragment;
    private LocalGroupDetailFragment mFragment;
    //MOT MOD END - IKPIM-991

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // TODO: Create Intent Resolver to handle the different ways users can get to this list.
        // TODO: Handle search or key down

        setContentView(R.layout.group_detail_activity);

        mShowGroupSourceInActionBar = getResources().getBoolean(
                R.bool.config_show_group_action_in_action_bar);

        //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
        //mFragment = (GroupDetailFragment) getFragmentManager().findFragmentById(
        //        R.id.group_detail_fragment);
        mFragment = (LocalGroupDetailFragment) getFragmentManager().findFragmentById(
                R.id.group_detail_fragment);
        //MOT MOD END - IKPIM-991

        mFragment.setListener(mFragmentListener);
        mFragment.setShowGroupSourceInActionBar(mShowGroupSourceInActionBar);

        //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
        LocalGroupUtils.log("GroupDetailActivity::onCreate(), start to loadGroup(), groupTitle = "
                    + getIntent().getStringExtra(LocalGroupUtils.EXTRA_BROWSE_LOCAL_GROUP_TITLE));

        mFragment.loadGroup(getIntent().getStringExtra(LocalGroupUtils.EXTRA_BROWSE_LOCAL_GROUP_TITLE));
        //MOT MOD END - IKPIM-991

        mFragment.closeActivityAfterDelete(true);

        // We want the UP affordance but no app icon.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_SHOW_HOME);
        }
    }

    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    //private final GroupDetailFragment.Listener mFragmentListener =
    //        new GroupDetailFragment.Listener() {
    private final LocalGroupDetailFragment.Listener mFragmentListener =
            new LocalGroupDetailFragment.Listener() {
    //MOT MOD END - IKPIM-991
        @Override
        public void onGroupSizeUpdated(String size) {
            getActionBar().setSubtitle(size);
        }

        @Override
        public void onGroupTitleUpdated(String title) {
            getActionBar().setTitle(title);
        }

        @Override
        public void onAccountTypeUpdated(String accountTypeString, String dataSet) {
            mAccountTypeString = accountTypeString;
            mDataSet = dataSet;
            invalidateOptionsMenu();
        }

        //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
        @Override
        public void onEditRequested(String groupTitle) {
            final Intent intent = new Intent(GroupDetailActivity.this, GroupEditorActivity.class);
            intent.setAction(Intent.ACTION_EDIT);
            intent.putExtra(LocalGroupUtils.EXTRA_EDIT_LOCAL_GROUP_TITLE, groupTitle);
            startActivity(intent);
        }

        @Override
        public void onSmsRequested(String groupTitle) {
            final Intent intent = new Intent(GroupDetailActivity.this, GroupConfirmActivity.class);
            intent.putExtra(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_AGG_GROUP_NAME, groupTitle);
            intent.setAction(GroupAPI.GroupIntents.ACTION_CONFIRM_GROUP);
            intent.setType(MIME_SMS_ADDRESS);
            startActivityForResult(intent, REQUEST_CONFIRM_GROUP_SMS);
        }

        @Override
        public void onEmailRequested(String groupTitle) {
            final Intent intent = new Intent(GroupDetailActivity.this, GroupConfirmActivity.class);
            intent.putExtra(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_AGG_GROUP_NAME, groupTitle);
            intent.setAction(GroupAPI.GroupIntents.ACTION_CONFIRM_GROUP);
            intent.setType(MIME_EMAIL_ADDRESS);
            startActivityForResult(intent, REQUEST_CONFIRM_GROUP_EMAIL);
        }

        @Override
        public void onEventRequested(String groupTitle) {
            final Intent intent = new Intent(GroupDetailActivity.this, GroupConfirmActivity.class);
            intent.putExtra(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_AGG_GROUP_NAME, groupTitle);
            intent.setAction(GroupAPI.GroupIntents.ACTION_CONFIRM_GROUP);
            intent.setType(MIME_CONTACT_EVENT);
            startActivityForResult(intent, REQUEST_CONFIRM_GROUP_EVENT);
        }
        //MOT MOD END - IKPIM-991

        @Override
        public void onContactSelected(Uri contactUri) {
            Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
            intent.putExtra(ContactDetailActivity.INTENT_KEY_FINISH_ACTIVITY_ON_UP_SELECTED, true);
            startActivity(intent);
        }

        @Override
        public void onOptionMenuUpdated() {
            invalidateOptionsMenu();
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mShowGroupSourceInActionBar) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.group_source, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mShowGroupSourceInActionBar) {
            return false;
        }
        MenuItem groupSourceMenuItem = menu.findItem(R.id.menu_group_source);
        if (groupSourceMenuItem == null) {
            return false;
        }
        final AccountTypeManager manager = AccountTypeManager.getInstance(this);
        final AccountType accountType =
                manager.getAccountType(mAccountTypeString, mDataSet);
        if (TextUtils.isEmpty(mAccountTypeString)
                || TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            groupSourceMenuItem.setVisible(false);
            return false;
        }
        View groupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(this);
        GroupDetailDisplayUtils.bindGroupSourceView(this, groupSourceView,
                mAccountTypeString, mDataSet);
        groupSourceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI,
                        mFragment.getGroupId());
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setClassName(accountType.resPackageName, accountType.getViewGroupActivity());
                startActivity(intent);
            }
        });
        groupSourceMenuItem.setActionView(groupSourceView);
        groupSourceMenuItem.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, PeopleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            return;
        }
            String[] dataList = data.getStringArrayExtra(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_STRING_ARRAY_CONFIRMED_DATALIST);
        switch (requestCode) {
            case REQUEST_CONFIRM_GROUP_SMS:
                // MOT CHINA Change intent interface
                mIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"+formAddresses(dataList)));
                //mIntent.putExtra("address", formAddresses(dataList));
                break;
            case REQUEST_CONFIRM_GROUP_EMAIL:
                mIntent = new Intent(Intent.ACTION_SENDTO);
                mIntent.setData(Uri.fromParts(Constants.SCHEME_MAILTO, formAddresses(dataList), null));
                break;
            case REQUEST_CONFIRM_GROUP_EVENT:
                mIntent = new Intent(Intent.ACTION_INSERT);
                mIntent.putExtra(Intent.EXTRA_EMAIL, formAddresses(dataList));
                mIntent.setType("vnd.android.cursor.item/event");
                break;
        }
        try{
            startActivity(mIntent);
        } catch(ActivityNotFoundException e){
            Log.e(TAG,"ActivityNotFoundException E=" + e +" Intent=" + mIntent);
        }
    }

    private String formAddresses(String[] dataList) {
        StringBuilder sb = new StringBuilder();
        for (String address : dataList) {
            if(sb.length() > 0){
                sb.append(',');
            }
            sb.append(address);
            //sb.append(','); // MOT CHINA
        }
        return sb.toString();
    }
    //MOT MOD END - IKPIM-991
}
