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
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
//import com.android.contacts.group.GroupEditorFragment; //MOT MOD - IKPIM-991: Group redesign feature on ics
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.PhoneCapabilityTester;

//MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
import com.motorola.contacts.group.LocalGroupEditorFragment;
import com.motorola.contacts.group.LocalGroupUtils;
import com.motorola.contacts.groups.GroupAPI.GroupIntents;
import com.motorola.contacts.groups.GroupAPI.GroupIntents.SavingGroup;
//MOT MOD END - IKPIM-991

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.contacts.model.HardCodedSources;
import java.util.ArrayList;
import android.accounts.Account;
import com.android.contacts.SimUtility;
import com.motorola.android.telephony.PhoneModeManager;
import android.telephony.TelephonyManager;


public class GroupEditorActivity extends ContactsActivity
        implements DialogManager.DialogShowingViewActivity {

    private static final String TAG = "GroupEditorActivity";

    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";
    public static final String ACTION_ADD_MEMBER_COMPLETED = "addMemberCompleted";
    public static final String ACTION_REMOVE_MEMBER_COMPLETED = "removeMemberCompleted";

    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    //private GroupEditorFragment mFragment;
    private LocalGroupEditorFragment mFragment;
    //MOT MOD END - IKPIM-991

    private static final int SUBACTIVITY_MULTIPLE_PICKER = 0;

    private DialogManager mDialogManager = new DialogManager(this);

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        String action = getIntent().getAction();

        if (ACTION_SAVE_COMPLETED.equals(action)) {
            finish();
            return;
        }

        setContentView(R.layout.group_editor_activity);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button for saving changes
            // to the group
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar,
                    null);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragment.onDoneClicked();
                }
            });
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }


        //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
        //mFragment = (GroupEditorFragment) getFragmentManager().findFragmentById(
        //        R.id.group_editor_fragment);
        mFragment = (LocalGroupEditorFragment) getFragmentManager().findFragmentById(
                R.id.group_editor_fragment);
        //MOT MOD END - IKPIM-991

        mFragment.setListener(mFragmentListener);
        mFragment.setContentResolver(getContentResolver());

        // NOTE The fragment will restore its state by itself after orientation changes, so
        // we need to do this only for a new instance.
        if (savedState == null) {
            //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
            if(action.equals(SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP)) {
                String groupTitle = getIntent().getStringExtra(SavingGroup.EXTRA_GROUP_QUICKTASK_GROUP_TITLE);
                String emailAddresses = getIntent().getStringExtra(SavingGroup.EXTRA_GROUP_QUICKTASK_EMAIL_ADDRESSES);
                mFragment.load(action, groupTitle, emailAddresses);
            } else if(action.equals(GroupIntents.ACTION_SAVING_GROUP)) {
                String groupTitle = getIntent().getStringExtra(SavingGroup.EXTRA_GROUP_NAME);
                Bundle phone2Name = getIntent().getBundleExtra(SavingGroup.EXTRA_BUNDLE_PHONE2NAME);
                Bundle email2Name = getIntent().getBundleExtra(SavingGroup.EXTRA_BUNDLE_EMAIL2NAME);
                long[] dataIdsArray = getIntent().getLongArrayExtra(SavingGroup.EXTRA_LONG_ARRAY_DATAIDS);
                mFragment.load(action, groupTitle, phone2Name, email2Name, dataIdsArray);
            } else {
                String groupTitle = getIntent().getStringExtra(LocalGroupUtils.EXTRA_EDIT_LOCAL_GROUP_TITLE);
                mFragment.load(action, groupTitle, getIntent().getExtras());
            }
            //MOT MOD END - IKPIM-991
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return mDialogManager.onCreateDialog(id, args);
        } else {
            // Nobody knows about the Dialog
            Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        // If the change could not be saved, then revert to the default "back" button behavior.
        if (!mFragment.save(SaveMode.CLOSE)) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_SAVE_COMPLETED.equals(action)) {
            //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
            LocalGroupUtils.log("GroupEditorActivity::onNewIntent(): saveCompleted! currentTime = "
                        + System.currentTimeMillis());
            LocalGroupUtils.log("The edited groupTitle is: " + intent.getStringExtra(LocalGroupUtils.EXTRA_LOCAL_GROUP_TITLE));

            mFragment.onSaveCompleted(true,
                    intent.getIntExtra(LocalGroupEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.CLOSE),
                    intent.getStringExtra(LocalGroupUtils.EXTRA_LOCAL_GROUP_TITLE));
            //MOT MOD END - IKPIM-991
        }
    }

    //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
    //private final GroupEditorFragment.Listener mFragmentListener =
    //        new GroupEditorFragment.Listener() {
    private final LocalGroupEditorFragment.Listener mFragmentListener =
            new LocalGroupEditorFragment.Listener() {
    //MOT MOD END - IKPIM-991
        @Override
        public void onGroupNotFound() {
            finish();
        }

        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onAccountsNotFound() {
            finish();
        }
        //  MOT MOD BEGIN - IKPIM-1026
        @Override
        public void onAddGroupMembers(ArrayList<String> excludeId){//mtdr83 change for IKCBSMMCPPRC-1310 
            final Intent intent = new Intent("motorola.intent.ACTION_MULTIPLE_PICK", Contacts.CONTENT_URI);
            intent.putExtra("for_group_pick_only", true);
            // exclude the C/G account as we don't support the sim group
            String accountType = HardCodedSources.ACCOUNT_TYPE_CARD;
            int defaultType = TelephonyManager.getDefault().getPhoneType();
            String accountDefault = SimUtility.getAccountNameByType(defaultType);
            String accountC = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_CDMA);
            String accountG = SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_GSM);
            ArrayList<Account> accountsList = new ArrayList<Account>();
            if (PhoneModeManager.isDmds()) {
                // exclude the C/G account
                accountsList.add(new Account(accountC, accountType));
                accountsList.add(new Account(accountG, accountType));
            }
            else {
                accountsList.add(new Account(accountDefault, accountType));
            }
            intent.putParcelableArrayListExtra("exclude_account", accountsList);            
            intent.putStringArrayListExtra("exclude_id", excludeId);   //mtdr83 add for IKCBSMMCPPRC-1310          
            startActivityForResult(intent, SUBACTIVITY_MULTIPLE_PICKER);
            return;
        }
        //  MOT MOD END

        @Override
        public void onSaveFinished(int resultCode, Intent resultIntent) {
            // TODO: Collapse these 2 cases into 1 that will just launch an intent with the VIEW
            // action to see the group URI (when group URIs are supported)
            // For a 2-pane screen, set the activity result, so the original activity (that launched
            // the editor) can display the group detail page
            if (PhoneCapabilityTester.isUsingTwoPanes(GroupEditorActivity.this)) {
                LocalGroupUtils.log("GroupEditorActivity::onSaveFinished() for tablet"); //MOT MOD - IKPIM-991: Group redesign feature on ics
                setResult(resultCode, resultIntent);
            } else if (resultIntent != null) {
                // For a 1-pane screen, launch the group detail page
                LocalGroupUtils.log("GroupEditorActivity::onSaveFinished() for smart phone"); //MOT MOD - IKPIM-991: Group redesign feature on ics
                Intent intent = new Intent(GroupEditorActivity.this, GroupDetailActivity.class);

                //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
                String groupTitle = resultIntent.getStringExtra(LocalGroupUtils.EXTRA_SELECTED_LOCAL_GROUP_TITLE);
                intent.putExtra(LocalGroupUtils.EXTRA_BROWSE_LOCAL_GROUP_TITLE, groupTitle);
                //MOT MOD END - IKPIM-991
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            finish();
        }
    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case SUBACTIVITY_MULTIPLE_PICKER: {
            if (resultCode == RESULT_OK) {
                mFragment.addMembers(data);
            }
            break;
        }
        }
    }
}
