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

package com.android.contacts.activities;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.PhoneCapabilityTester;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class ContactEditorActivity extends ContactsActivity
        implements DialogManager.DialogShowingViewActivity {
    private static final String TAG = "ContactEditorActivity";
    private static final boolean DEBUG = true;
    public static final String ACTION_JOIN_COMPLETED = "joinCompleted";
    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";

    /**
     * Boolean intent key that specifies that this activity should finish itself
     * (instead of launching a new view intent) after the editor changes have been
     * saved.
     */
    public static final String INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED =
            "finishActivityOnSaveCompleted";

    private ContactEditorFragment mFragment;
    private boolean mFinishActivityOnSaveCompleted;
    private int mOrientation;

    private DialogManager mDialogManager = new DialogManager(this);

    @Override
	protected void onResume() {
		super.onResume();
		if (mOrientation != getResources().getConfiguration().orientation){
			Log.v(TAG, "mOrientation changed!");
	        final Intent intent = getIntent();
//	        mFragment.inva
		}
	}
    
	@Override
	protected void onPause() {
		super.onPause();
//		mOrientation = getResources().getConfiguration().orientation;
	}

    //add by txbv34
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d(TAG,"onConfigurationChanged");
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE 
				|| newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Log.d(TAG,"onConfigurationChanged,orientation change");
		}
		 
	}
	
	
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        
        // check availability for the SIM/UIM card contacts
        if (!TextUtils.isEmpty(action)) {
            Uri contactUri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
            if (ContactsUtils.contactIsCardType(ContactEditorActivity.this, contactUri)) {
                if (!ContactsUtils.checkSimAvailable(ContactEditorActivity.this, true)) {
        	        Log.v(TAG, "SIM card not available !");
        	        //finish();
        	        //return;
                }                           	
            }
        }

        // Determine whether or not this activity should be finished after the user is done
        // editing the contact or if this activity should launch another activity to view the
        // contact's details.
        mFinishActivityOnSaveCompleted = intent.getBooleanExtra(
                INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false);

        // The only situation where action could be ACTION_JOIN_COMPLETED is if the
        // user joined the contact with another and closed the activity before
        // the save operation was completed.  The activity should remain closed then.
        if (ACTION_JOIN_COMPLETED.equals(action)) {
            finish();
            return;
        }

        if (ACTION_SAVE_COMPLETED.equals(action)) {
            finish();
            return;
        }

        setContentView(R.layout.contact_editor_activity);

        //MOT MOD IKHSS6UPGR-6251, Show 'Cancel'/'Save' button as custom actionbar
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                // Inflate a custom action bar that contains the "save" button for saving changes
                // to the contact, "cancel" button for revert change
                LayoutInflater inflater = (LayoutInflater) getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                View customActionBarView = inflater.inflate(R.layout.moto_edit_contact_custom_actionbar, new LinearLayout(this), false);
                View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
                saveMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFragment.doSaveAction();
                    }
                });

                View cancelMenuItem = customActionBarView.findViewById(R.id.cancel_menu_item);
                cancelMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFragment.doRevertAction();
                    }
                });

                // Show the custom action bar but hide the home icon and title
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                            ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                            ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setCustomView(customActionBarView);
            } //MOT MOD IKHSS6UPGR-6251
        } else {

            //MOT MOD BEGIN - IKHSS7-4528, follow Moto's ICS UI guideline
            if (Intent.ACTION_INSERT.equals(action)) {
                getActionBar().setTitle(R.string.add_contact_dlg_title);
                getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE |
                           ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
            } else if (Intent.ACTION_EDIT.equals(action)) {
                getActionBar().setTitle(R.string.edit_contact);
                getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE |
                           ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
            }
        }
        /* This is google's action bar, Done button on the top left, do not use this
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button for saving changes
            // to the contact
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragment.doSaveAction();
                }
            });
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }*/
        //MOT MOD END - IKHSS7-4528

        mFragment = (ContactEditorFragment) getFragmentManager().findFragmentById(
                R.id.contact_editor_fragment);
        mFragment.setListener(mFragmentListener);
        Uri uri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
        mFragment.load(action, uri, getIntent().getExtras());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mFragment.setIntentExtras(intent.getExtras());
        } else if (ACTION_SAVE_COMPLETED.equals(action)) {
            Log.d(TAG, "ACTION_SAVE_COMPLETED received intent=" + intent);
            mFragment.onSaveCompleted(true,
                    intent.getIntExtra(ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.CLOSE),
                    intent.getBooleanExtra(ContactSaveService.EXTRA_SAVE_SUCCEEDED, false),
                    intent.getData());
        } else if (ACTION_JOIN_COMPLETED.equals(action)) {
            mFragment.onJoinCompleted(intent.getData());
            /*Added for switchuitwo-95 begin*/
            PeopleActivity.setNeedClearParition(true);
            /*Added for switchuitwo-95 end*/
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) return mDialogManager.onCreateDialog(id, args);

        // Nobody knows about the Dialog
        Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    @Override
    public void onBackPressed() {
        mFragment.save(SaveMode.CLOSE);
    }

    //MOT MOD BEGIN - IKHSS7-4528
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                mFragment.doSaveAction();
            }
            return true;
        }
        return false;
    }
    //MOT MOD END - IKHSS7-4528

    private final ContactEditorFragment.Listener mFragmentListener =
            new ContactEditorFragment.Listener() {
        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onSaveFinished(Intent resultIntent) {
            if (mFinishActivityOnSaveCompleted) {
                setResult(resultIntent == null ? RESULT_CANCELED : RESULT_OK, resultIntent);
            } else if (resultIntent != null) {
                startActivity(resultIntent);
            }
            finish();
        }

        @Override
        public void onContactSplit(Uri newLookupUri) {
        	/*Added for switchuitwo-95 begin*/
        	PeopleActivity.setNeedClearParition(true);
        	/*Added for switchuitwo-95 end*/
            finish();
        }

        //MOT MOD BEGIN, IKPIM-899, for feature: Remember Account Choice
        @Override
        public void onAccountSelectorAborted() {
            finish();
        }
        //MOT MOD END

        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onEditOtherContactRequested(
                Uri contactLookupUri, ArrayList<ContentValues> values) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            intent.putExtra(ContactEditorFragment.INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY, "");

            // Pass on all the data that has been entered so far
            if (values != null && values.size() != 0) {
                intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, values);
            }

            startActivity(intent);
            finish();
        }

        @Override
        public void onCustomCreateContactActivityRequested(AccountWithDataSet account,
                Bundle intentExtras) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.resPackageName,
                    accountType.getCreateContactActivityClassName());
            intent.setAction(Intent.ACTION_INSERT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }
            intent.putExtra(RawContacts.ACCOUNT_NAME, account.name);
            intent.putExtra(RawContacts.ACCOUNT_TYPE, account.type);
            intent.putExtra(RawContacts.DATA_SET, account.dataSet);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        }

        @Override
        public void onCustomEditContactActivityRequested(AccountWithDataSet account,
                Uri rawContactUri, Bundle intentExtras, boolean redirect) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.resPackageName,
                    accountType.getEditContactActivityClassName());
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(rawContactUri);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }

            if (redirect) {
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(intent);
                finish();
            } else {
                startActivity(intent);
            }
        }
    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }
}
