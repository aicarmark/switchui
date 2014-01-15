/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.contacts.activities;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.list.AccountFilterActivity;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.LegacyContactListAdapter;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.widget.ContextMenuAdapter;
import com.android.contacts.editor.ContactEditorFragment;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;

import android.util.SparseBooleanArray;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.motorola.android.telephony.PhoneModeManager;
import com.motorola.contacts.list.ContactMultiplePickerFragment;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;
import com.motorola.contacts.list.OnContactMultiplePickerActionListener;

import java.util.Set;
import android.accounts.Account;
import com.android.contacts.RomUtility;
import android.widget.Toast;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
                OnCloseListener, OnFocusChangeListener {
    private static final boolean DEBUG = true;
    private static final String TAG = "ContactSelectionActivity";

    private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 1;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
    private static final int MAX_MULTIPICKER_INTENT_RESULT_DATA_LIMIT = 1000;
    private static final int MAX_SESSION_LIFE_CYCLE = 12*60*60*1000;  //session data older than 12 hour will be removed
//<!-- MOTOROLA MOD End of IKPIM-491 -->
    // Delay to allow the UI to settle before making search view visible
    private static final int FOCUS_DELAY = 200;

    //MOT MOD BEGIN - IKHSS6UPGR-3604
    private static final String SHOW_CREATE_NEW_BUTTON = "show_create_new_button";
    private boolean mShowCreateNewButton = true;
    //MOT MOD END - IKHSS6UPGR-3604
    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;
    private SearchView mSearchView;
    /**
     * Can be null. If null, the "Create New Contact" button should be on the menu.
     */
    private View mCreateNewContactButton;

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
    // Task we have running to populate the database.
    AsyncTask<Void, Void, Void> mPopulatingTask;
    private ProgressDialog mWaitingDialog;
//<!-- MOTOROLA MOD End of IKPIM-491 -->

    private boolean mForExportOnly;  //MOT MOD  - IKHSS7-2028
    public static final int MENU_CONTACTS_FILTER = 1;

    private boolean mForGroupPick = false; // MOT MOD - IKPIM-1026
    
    private ArrayList<Account> mIncludeAccount = null;   // MOT CHINA
    private ArrayList<Account> mExcludeAccount = null;   // MOT CHINA     
    private ArrayList<String> mExcludeId = null;   //mtdr83 add for IKCBSMMCPPRC-1310     


    public ContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
        }

        mShowCreateNewButton = getIntent().getBooleanExtra(SHOW_CREATE_NEW_BUTTON, true); //MOT MOD - IKHSS6UPGR-3604
        mForExportOnly = getIntent().getBooleanExtra("for_export_only", false); //MOT MOD - IKHSS7-2028
        mForGroupPick =  getIntent().getBooleanExtra("for_group_pick_only", false);; // MOT MOD - IKPIM-1026
        mIncludeAccount = getIntent().getParcelableArrayListExtra("include_account");
        mExcludeAccount = getIntent().getParcelableArrayListExtra("exclude_account");
        //mtdr83 add for IKCBSMMCPPRC-1310 
        mExcludeId = getIntent().getStringArrayListExtra("exclude_id");
        Log.d(TAG, "mExcludeId = " + mExcludeId);

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Resolve the intent
        final Intent intent = getIntent();

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            finish();
            return;
        }

        configureActivityTitle();
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT) {
            setContentView(R.layout.contact_multiple_picker);
        } else {
            setContentView(R.layout.contact_picker);
        }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }

        prepareSearchViewAndActionBar();

        mCreateNewContactButton = findViewById(R.id.new_contact);
        if (mCreateNewContactButton != null) {
            if (shouldShowCreateNewContactButton()) {
                mCreateNewContactButton.setVisibility(View.VISIBLE);
                mCreateNewContactButton.setOnClickListener(this);
            } else {
                mCreateNewContactButton.setVisibility(View.GONE);
            }
        }

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        final Button select_or_unselect_all = (Button) findViewById(R.id.operation_all);
        if (select_or_unselect_all != null) {
            if(mForGroupPick){
                select_or_unselect_all.setVisibility(View.GONE); //  MOT MOD - IKPIM-1026
            }else{
                select_or_unselect_all.setText(R.string.option_select_all); 
                select_or_unselect_all.setOnClickListener(select_or_unselect_all_ClickListener);
            }
        }

        final Button ok = (Button) findViewById(R.id.ok);
        final Button selectButton = (Button) findViewById(R.id.operation_all);
        if(selectButton != null){
            selectButton.setEnabled(false);
        }
        if (ok != null) {
            ok.setEnabled(false);
            ok.setText(R.string.menu_done);
            ok.setOnClickListener(ok_ClickListener);
        }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

        // MOT CHINA, Ensure SIM type is valid in case app crash
        SimUtility.queryAllSimType(getContentResolver());
    }



    private boolean shouldShowCreateNewContactButton() {
        return ((mActionCode == ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT && mShowCreateNewButton) //MOT MOD IKHSS6UPGR-3604
                || (mActionCode == ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT
                        && !mRequest.isSearchMode()));
    }

    private void prepareSearchViewAndActionBar() {
        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_POSTAL ||
                mRequest.isLegacyCompatibilityMode()) {
            findViewById(R.id.search_view).setVisibility(View.GONE);
            final ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
            }
            return;
        }

        // If ActionBar is available, show SearchView on it. If not, show SearchView inside the
        // Activity's layout.
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            final View searchViewOnLayout = findViewById(R.id.search_view);
            if (searchViewOnLayout != null) {
                searchViewOnLayout.setVisibility(View.GONE);
            }

            final View searchViewContainer = LayoutInflater.from(actionBar.getThemedContext())
                    .inflate(R.layout.custom_action_bar, null);
            mSearchView = (SearchView) searchViewContainer.findViewById(R.id.search_view);

            // In order to make the SearchView look like "shown via search menu", we need to
            // manually setup its state. See also DialtactsActivity.java and ActionBarAdapter.java.
            //<!-- MOTOROLA MOD Start: IKHSS7-3023 --- Disable clear button in contact picker screen-->
            mSearchView.setIconifiedByDefault(false);
            //<!-- MOTOROLA MOD End of IKHSS7-3023-->
            mSearchView.setQueryHint(getString(R.string.hint_findContacts));
            mSearchView.setIconified(false);

            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setOnQueryTextFocusChangeListener(this);

            actionBar.setCustomView(searchViewContainer,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            mSearchView = (SearchView) findViewById(R.id.search_view);
            mSearchView.setQueryHint(getString(R.string.hint_findContacts));
            mSearchView.setOnQueryTextListener(this);

            // This is a hack to prevent the search view from grabbing focus
            // at this point.  If search view were visible, it would always grabs focus
            // because it is the first focusable widget in the window.
            mSearchView.setVisibility(View.INVISIBLE);
            mSearchView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSearchView.setVisibility(View.VISIBLE);
                }
            }, FOCUS_DELAY);
        }

        if (mSearchView != null) {
            mSearchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
            mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_SEARCH);
            // Clear focus and suppress keyboard show-up.
            mSearchView.clearFocus();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button in the layout,
        // try showing a menu for it.
        if (shouldShowCreateNewContactButton() && mCreateNewContactButton == null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.contact_picker_options, menu);
        }
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT) {
            menu.add(0, MENU_CONTACTS_FILTER, 0, R.string.menu_contacts_filter).setIcon(R.drawable.ic_menu_settings_holo_light);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.create_new_contact: {
                if(RomUtility.isOutofMemory()){
                    Toast.makeText(this, R.string.rom_full, Toast.LENGTH_LONG).show();
                    return true;
                    }

                startCreateNewContactActivity();
                return true;
            }
            case MENU_CONTACTS_FILTER: {
                final Intent intent = new Intent(this, AccountFilterActivity.class);                

                if (mIncludeAccount != null)
                    intent.putParcelableArrayListExtra("include_account", mIncludeAccount);
                else if (mExcludeAccount != null)
                    intent.putParcelableArrayListExtra("exclude_account", mExcludeAccount);

                startActivityForResult(intent, SUBACTIVITY_ACCOUNT_FILTER);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);

    }

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
    private boolean configureSelectUnSelectButton(){
        Button targetButton = (Button) findViewById(R.id.operation_all);
        final Button targetOKButton = (Button) findViewById(R.id.ok);
        if ((targetButton==null)||(targetOKButton==null)||(mListFragment==null)||(!(mListFragment instanceof ContactMultiplePickerFragment))||(mListFragment.getListView()==null)||(mListFragment.getAdapter()==null)) {
            return false;
        }

        if (mListFragment.getListView().getCheckedItemCount()>=getTotalContactsCounts()){
            targetButton.setText(R.string.option_unselect_all);
        } else {
            targetButton.setText(R.string.option_select_all);
        }

        //MOT MOD BEGIN - IKHSS6-1308
        if (getTotalContactsCounts() == 0) {
            targetButton.setEnabled(false);
        } else {
            targetButton.setEnabled(true);
        }
        //MOT MOD END - IKHSS6-1308
        //MOT MOD START - IKHSS7-4287
        if (mListFragment.isSearchMode()){
            targetOKButton.setEnabled(true);
            targetOKButton.setText(R.string.menu_done);
            targetOKButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mSearchView.setQuery(null, true);
                        mSearchView.clearFocus();
                        targetOKButton.setEnabled(false);
                    }
            });
        } else {
            targetOKButton.setText(R.string.menu_done);
            targetOKButton.setOnClickListener(ok_ClickListener);
            // even user unselect all, we still can return, it is useful case for remove group member 
            if (mListFragment.getListView().getCheckedItemCount()==0) {
                targetOKButton.setEnabled(false);
            } else {
                targetOKButton.setEnabled(true);
            }
        }
        //MOTO MOD: IKHSS7-4287

        return true;
    }

    private void configureSelectUnSelectButton(Uri contactUri,Boolean isChecked){

        if (configureSelectUnSelectButton() && contactUri!=null ) {
            ((ContactMultiplePickerFragment)mListFragment).mSelectionCache.put(contactUri, isChecked);
        }

    }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    private void configureActivityTitle() {
        if (mRequest.getActivityTitle() != null) {
            setTitle(mRequest.getActivityTitle());
            return;
        }

        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT: {
                setTitle(R.string.contactMultiplePickerActivityTitle);
                break;
            }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                setTitle(R.string.shortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                setTitle(R.string.callShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                setTitle(R.string.messageShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        switch (mActionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setEditMode(true);
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
                fragment.setSearchMode(mRequest.isSearchMode());
                mListFragment = fragment;
                break;
//<!-- MOTOROLA MOD End of IKPIM-491 -->
            }
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT: {
                ContactMultiplePickerFragment fragment = new ContactMultiplePickerFragment();
                fragment.setSearchMode(mRequest.isSearchMode());
                fragment.setForExportOnly(mForExportOnly); //MOT MOD - IKHSS7-2028
                if (mIncludeAccount != null) {   // MOT CHINA
                    fragment.setIncludeAccount(mIncludeAccount);
                }
                else if (mExcludeAccount != null) {
                    fragment.setExcludeAccount(mExcludeAccount);
                } 
                //mtdr83 add for IKCBSMMCPPRC-1310 
                if(mExcludeId != null){
                	fragment.setExcludeId(mExcludeId);
                }
                mListFragment = fragment;
                break;
            }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setShortcutRequested(true);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setShortcutAction(Intent.ACTION_CALL);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setShortcutAction(Intent.ACTION_SENDTO);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                mListFragment = fragment;
                break;
            }

            default:
                throw new IllegalStateException("Invalid action code: " + mActionCode);
        }

        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    public void setupActionListener() {
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        if (mListFragment instanceof ContactMultiplePickerFragment) {
            ((ContactMultiplePickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactMultiplePickerActionListener());
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        } else if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Bundle extras = getIntent().getExtras();
            if (launchAddToContactDialog(extras)) {
                // Show a confirmation dialog to add the value(s) to the existing contact.
                Intent intent = new Intent(ContactSelectionActivity.this,
                        ConfirmAddDetailActivity.class);
                intent.setData(contactLookupUri);
                if (extras != null) {
                    intent.putExtras(extras);
                }
                // Wait for the activity result because we want to keep the picker open (in case the
                // user cancels adding the info to a contact and wants to pick someone else).
                // MOT CHINA, Alaways  launch the full contact editor to support replacing SIM number
                startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
                // startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
            } else {
                // Otherwise launch the full contact editor.
                startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
            }
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        /**
         * Returns true if is a single email or single phone number provided in the {@link Intent}
         * extras bundle so that a pop-up confirmation dialog can be used to add the data to
         * a contact. Otherwise return false if there are other intent extras that require launching
         * the full contact editor.
         */
        private boolean launchAddToContactDialog(Bundle extras) {
            if (extras == null) {
                return false;
            }
            Set<String> intentExtraKeys = extras.keySet();
            int numIntentExtraKeys = intentExtraKeys.size();
            if (numIntentExtraKeys == 2) {
                boolean hasPhone = intentExtraKeys.contains(Insert.PHONE) &&
                        intentExtraKeys.contains(Insert.PHONE_TYPE);
                boolean hasEmail = intentExtraKeys.contains(Insert.EMAIL) &&
                        intentExtraKeys.contains(Insert.EMAIL_TYPE);
                return hasPhone || hasEmail;
            } else if (numIntentExtraKeys == 1) {
                return intentExtraKeys.contains(Insert.PHONE) ||
                        intentExtraKeys.contains(Insert.EMAIL);
            }
            // Having 0 or more than 2 intent extra keys means that we should launch
            // the full contact editor to properly handle the intent extras.
            return false;
        }
    }

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
    private final class ContactMultiplePickerActionListener implements OnContactMultiplePickerActionListener {
        @Override
        public void onCreateNewContactAction() {

        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {

        }

        @Override
        public void onPickContactAction(Uri contactUri) {

        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {

        }

        @Override
        public void onToggleContactAction(Uri contactUri,boolean isChecked){
            configureSelectUnSelectButton(contactUri,isChecked);
        }

        @Override
        public void onContactLoadingCompletedAction() {
            configureSelectUnSelectButton();
        }

        //  MOT MOD BEGIN - IKPIM-1026
        @Override
        public  boolean isSelectAllSupported(){
             return !mForGroupPick;
        }
        // MOT MOD END
    }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickPhoneNumberAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAdapter menuAdapter = mListFragment.getContextMenuAdapter();
        if (menuAdapter != null) {
            return menuAdapter.onContextItemSelected(item);
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //MOTOMOD start: IKHSS7-4287
        if ((newText==null)||newText.equalsIgnoreCase("")){
            if (findViewById(R.id.operation_all)!=null){
                findViewById(R.id.operation_all).setEnabled(false);
            }
            if (findViewById(R.id.ok)!=null){
                findViewById(R.id.ok).setEnabled(false);
            }
        }
        //MOTOMOD end
        mListFragment.setQueryString(newText, true);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // When the search is "committed" by the user, then hide the keyboard so the user can
        // more easily browse the list of results.
        if (mSearchView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
        }
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.search_view: {
                if (hasFocus) {
                    showInputMethod(mSearchView.findFocus());
                }
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_contact: {
                startCreateNewContactActivity();
                break;
            }
        }
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        if(getIntent().getBooleanExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false)) {
            intent.putExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
        }
        if(getIntent().getBooleanExtra(ContactEditorFragment.INTENT_EXTRA_EXCLUDE_SIMCARD, false)) {
            intent.putExtra(ContactEditorFragment.INTENT_EXTRA_EXCLUDE_SIMCARD, true);
        }
        startActivityAndForwardResult(intent);
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    startActivity(data);
                }
                finish();
            }
        } else if (requestCode == SUBACTIVITY_ACCOUNT_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                final ContactListFilter filter = (ContactListFilter)
                        data.getParcelableExtra(AccountFilterActivity.KEY_EXTRA_CONTACT_LIST_FILTER);
                if (filter != null) {
                	// BEGIN Motorola, ODC_001639, 2012-12-17, SWITCHUITWO-177 
                	mSearchView.setQuery(null, false);
                	// END SWITCHUITWO-177
                    ContactMultiplePickerFragment fragment = (ContactMultiplePickerFragment)mListFragment;
                    fragment.setFilter(filter);
                }
            }
        }
    }

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
    private int getTotalContactsCounts(){
        int totalNumber = 0;
        if (mListFragment.getAdapter() != null) {
            for (int i = 0; i < mListFragment.getAdapter().getPartitionCount(); i++) {
                if (mListFragment.getAdapter().getCursor(i) != null) {
                    totalNumber += mListFragment.getAdapter().getCursor(i).getCount();
                }
            }
        }
        return totalNumber;
    }

    OnClickListener select_or_unselect_all_ClickListener = new OnClickListener(){

        @Override
        public void onClick(View v) {
            final Button select_or_unselect_all = (Button) findViewById(R.id.operation_all);
            if (select_or_unselect_all != null) {
                if ((mListFragment == null)
                        || (!(mListFragment instanceof ContactMultiplePickerFragment))
                        || (mListFragment.getListView() == null)
                        || (mListFragment.getAdapter() == null)
                        || (mListFragment.isLoading()==true)) {
                    return;
                }

                if (mListFragment.getListView().getCheckedItemCount() < getTotalContactsCounts()) {
                    for (int i = 0; i < mListFragment.getAdapter().getCount(); i++) {
                        mListFragment.getListView().setItemChecked(i, true);
                        Uri uri;
                        if (mListFragment.isLegacyCompatibilityMode()) {
                            uri = ((LegacyContactListAdapter) mListFragment.getAdapter()).getPersonUri(i);
                        } else {
                            uri = ((ContactListAdapter) mListFragment.getAdapter()).getContactUri(i);
                        }
                        if (uri!=null){
                            ((ContactMultiplePickerFragment)mListFragment).mSelectionCache.put(uri, true);
                        }
                    }
                } else {
                    mListFragment.getListView().clearChoices();
                    mListFragment.getListView().requestLayout();
                    for (int i = 0; i < mListFragment.getAdapter().getCount(); i++) {
                        Uri uri;
                        if (mListFragment.isLegacyCompatibilityMode()) {
                            uri = ((LegacyContactListAdapter) mListFragment.getAdapter()).getPersonUri(i);
                        } else {
                            uri = ((ContactListAdapter) mListFragment.getAdapter()).getContactUri(i);
                        }
                        if (uri!=null){
                            ((ContactMultiplePickerFragment)mListFragment).mSelectionCache.put(uri, false);
                        }
                    }
                }
                configureSelectUnSelectButton();
            }
        }

    };

    OnClickListener ok_ClickListener = new OnClickListener(){

        @Override
        public void onClick(View v) {
            if ((mListFragment == null)
                    || (!(mListFragment instanceof ContactMultiplePickerFragment))
                    || (mListFragment.getListView() == null)
                    || (mListFragment.getAdapter() == null)
                    || (mListFragment.isLoading()==true)) {
                return;
            }

            final SparseBooleanArray userCheckedIds = mListFragment.getListView().getCheckedItemPositions();
            final int totalTargetNumbers = getTotalContactsCounts();
            final long sessionid = System.currentTimeMillis();
            final long expired_session_id = sessionid - MAX_SESSION_LIFE_CYCLE;  //session data older than 12 hour will be removed
            final ContentResolver cr = getContentResolver();

            // Two ways to return the activity results:
            // 1. if the total selection is under the max intent data size limitation, send the data within intent directly
            // 2. otherwise, send the result session id , client need access multiple selection content provider to get all data
            // Notes: there is a bug in mListFragment.getListView().getCheckedItemPositions(), total size may exceed the real selected ids

            ArrayList<Uri> selected_contact_uris = new ArrayList<Uri>();
            final ArrayList<ContentValues> selected_contact_values = new ArrayList<ContentValues>();
            for (int i = 0; i < totalTargetNumbers; i++) {
                if (userCheckedIds.get(i)) {
                    Uri uri;
                    if (mListFragment.isLegacyCompatibilityMode()) {
                        uri = ((LegacyContactListAdapter) mListFragment.getAdapter()).getPersonUri(i);
                    } else {
                        uri = ((ContactListAdapter) mListFragment.getAdapter()).getContactUri(i);
                    }
                    //Log.e("Eric final:" + i + "-", uri.toString() + "\n");
                    selected_contact_uris.add(uri);

                    ContentValues curValues = new ContentValues();
                    curValues.put(Resultable.COLUMN_NAME_REQUESTER, ""); //reserved
                    curValues.put(Resultable.COLUMN_NAME_SESSION_ID, sessionid) ;
                    curValues.put(Resultable.COLUMN_NAME_DATA, uri.toString());
                    selected_contact_values.add(curValues);

                }
            }
            final int userSelectedNumbers = selected_contact_uris.size();

            Log.e(TAG,""+ "User Selection: ("+userSelectedNumbers+"/"+totalTargetNumbers+")");

            if (userSelectedNumbers <= MAX_MULTIPICKER_INTENT_RESULT_DATA_LIMIT) {
                Intent intent = new Intent();
                if (userSelectedNumbers == totalTargetNumbers) {
                    intent.putExtra(Resultable.SELECTION_RESULT_ALL_SELECTED,true);
                } else {
                    intent.putExtra(Resultable.SELECTION_RESULT_ALL_SELECTED,false);
                }
                intent.putExtra(Resultable.SELECTTION_RESULT_INCLUDED,true);
                intent.putParcelableArrayListExtra(Resultable.SELECTED_CONTACTS,selected_contact_uris);
                returnPickerResult(intent);

            } else {

                if (mPopulatingTask != null) {
                    mPopulatingTask.cancel(true);
                    mPopulatingTask = null;
                    if (mWaitingDialog!=null) {
                        mWaitingDialog.dismiss();
                        mWaitingDialog = null;
                    }
                }
                mPopulatingTask = new AsyncTask<Void, Void, Void>(){

                    @Override
                    protected void onPreExecute() {
                        mWaitingDialog = new ProgressDialog(ContactSelectionActivity.this);
                        mWaitingDialog.setMessage(ContactSelectionActivity.this.getText(R.string.toast_please_wait));
                        mWaitingDialog.setIndeterminate(true);
                        mWaitingDialog.setCancelable(true);
                        mWaitingDialog.setOnCancelListener(new OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (mPopulatingTask != null) {
                                    mPopulatingTask.cancel(true);
                                    mPopulatingTask = null;
                                }
                            }
                        });

                        mWaitingDialog.show();
                    }

                    @Override protected Void doInBackground(Void... params) {

                        //delete old sessions
                        cr.delete(Resultable.CONTENT_URI, Resultable.COLUMN_NAME_SESSION_ID+"<?", new String[] {Long.toString(expired_session_id)});

                        //bulk insert new data
                        ContentValues[] bulkValues = new ContentValues[userSelectedNumbers];
                        selected_contact_values.toArray(bulkValues);
                        int numNewAddedRows = cr.bulkInsert(Resultable.CONTENT_URI, bulkValues);
                        Log.w(TAG, "Inserted temp results:"+numNewAddedRows);

                        //Following code is used to test the extreme slow case, 5-6mins to insert 4000 records
//                        for (int j=0;j<selected_contact_values.size();j++){
//                            cr.insert(Resultable.CONTENT_URI, selected_contact_values.get(j));
//                        }

                        return null;
                    }

                    protected void onPostExecute(Void result) {
                        if (mWaitingDialog!=null) {
                            mWaitingDialog.dismiss();
                            mWaitingDialog = null;
                        }

                        //send the intent
                        Intent intent = new Intent();
                        if (userSelectedNumbers == totalTargetNumbers) {
                            intent.putExtra(Resultable.SELECTION_RESULT_ALL_SELECTED,true);
                        } else {
                            intent.putExtra(Resultable.SELECTION_RESULT_ALL_SELECTED,false);
                        }
                        intent.putExtra(Resultable.SELECTTION_RESULT_INCLUDED,false);
                        intent.putExtra(Resultable.SELECTION_RESULT_SESSION_ID,sessionid);
                        returnPickerResult(intent);
                    };

                    @Override protected void onCancelled() {
                        if (mWaitingDialog!=null) {
                            mWaitingDialog.dismiss();
                            mWaitingDialog = null;
                        }
                        //delete canceled sessions? do we need it? stick mode will report performance issue of following execution...
//                        int numRemovedRows = cr.delete(Resultable.CONTENT_URI, Resultable.COLUMN_NAME_SESSION_ID+"=?", new String[] {Long.toString(sessionid)});
//                        Log.w(TAG, "Removed temp results:"+numRemovedRows);
                    }
                };
                mPopulatingTask.execute((Void[])null);
            }
        }

    };
//<!-- MOTOROLA MOD End of IKPIM-491 -->
}
