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

package com.motorola.contacts.group;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMemberLoader.GroupEditorQuery;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.GroupEditorActivity;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.WeakAsyncTask;
import com.android.internal.util.Objects;

import com.motorola.contacts.group.LocalGroupAccountLoader;
import com.motorola.contacts.group.LocalGroupAccountMemberData;
import com.motorola.contacts.group.LocalGroupMetaDataLoader;
import com.motorola.contacts.group.LocalGroupMemberLoader;
import com.motorola.contacts.group.LocalGroupMemberLoader.LocalGroupEditorMemberQuery;
import com.motorola.contacts.group.LocalGroupSaveService;
import com.motorola.contacts.group.LocalGroupUtils;
import com.motorola.contacts.group.MyLooper;
import com.motorola.contacts.group.SuggestedLocalGroupMemberListAdapter.SuggestedMember;
import com.motorola.contacts.group.AddGroupNameDialogFragment;
import com.motorola.contacts.groups.GroupAPI;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;
import com.motorola.contacts.preference.ContactPreferenceUtilities;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ref.WeakReference;
// BEGIN Motorola, FTR 36344, IKPIM-1040
import com.motorola.contacts.util.MEContacts;
// END IKPIM-1040
import android.text.InputFilter;
import com.android.contacts.util.Constants;
import com.android.contacts.RomUtility;

public class LocalGroupEditorFragment extends Fragment implements SelectAccountDialogFragment.Listener, AddGroupNameDialogFragment.Listener {
    private static final String TAG = "LocalGroupEditorFragment";

    private static final String LEGACY_CONTACTS_AUTHORITY = "contacts";

    private static final String KEY_ACTION = "action";
    private static final String KEY_STATUS = "status";
    private static final String KEY_GROUP_NAME_IS_READ_ONLY = "groupNameIsReadOnly";
    private static final String KEY_ORIGINAL_GROUP_NAME = "originalGroupName";
    private static final String KEY_MEMBERS_TO_ADD = "membersToAdd";
    private static final String KEY_MEMBERS_TO_REMOVE = "membersToRemove";
    private static final String KEY_MEMBERS_TO_DISPLAY = "membersToDisplay";

    private static final String KEY_RAW_MEMBERS_TO_ADD = "rawMembersToAdd";
    private static final String KEY_GROUP_TITLE = "groupTitle";
    private static final String KEY_EMAIL_ADDRESS = "emailAddress";
    private static final String KEY_EMAIL_NAME = "email2Name";
    private static final String KEY_PHONE_NAME = "phone2Name";
    private static final String KEY_DATA_IDS = "dataIds";


    private static final String CURRENT_EDITOR_TAG = "currentEditorForAccount";
    private static final String EMPTY_STRING = "";
    
    private static final int MULTI_PICK_CONTACT_REQUEST = 0;    

    public static interface Listener {
        /**
         * Group metadata was not found, close the fragment now.
         */
        public void onGroupNotFound();

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(int resultCode, Intent resultIntent);

        /**
         * Fragment is created but there's no accounts set up.
         */
        void onAccountsNotFound();

        public void onAddGroupMembers(ArrayList<String> excludeId);//mtdr83 add for IKCBSMMCPPRC-1310 
    }

    private static final int LOADER_GROUP_METADATA = 1;
    private static final int LOADER_EDITOR_MEMBERS = 2;
    private static final int LOADER_GROUP_ACCOUNT_MEMBER_DATA = 3;
    private static final int LOADER_MEMBER_RAW_DATA = 4;
    private static final int LOADER_PICKED_CONTACTS = 5;

    private static final int LOADER_SELECTED_CONTACTS = 16; // MOT CHINA
    public static final String SAVE_MODE_EXTRA_KEY = "saveMode";

    private static final String MEMBER_RAW_CONTACT_ID_KEY = "rawContactId";
    private static final String MEMBER_LOOKUP_URI_KEY = "memberLookupUri";
    private String GROUP_EXIST_STRING = "";
    private String GROUP_NAME_CANT_EMPTY = "";

    protected static final String[] PROJECTION_CONTACT = new String[] {
        Contacts._ID,                           // 0
        Contacts.DISPLAY_NAME_PRIMARY,          // 1
        Contacts.DISPLAY_NAME_ALTERNATIVE,      // 2
        Contacts.SORT_KEY_PRIMARY,              // 3
        Contacts.STARRED,                       // 4
        Contacts.CONTACT_PRESENCE,              // 5
        Contacts.CONTACT_CHAT_CAPABILITY,       // 6
        Contacts.PHOTO_ID,                      // 7
        Contacts.PHOTO_THUMBNAIL_URI,           // 8
        Contacts.LOOKUP_KEY,                    // 9
        Contacts.PHONETIC_NAME,                 // 10
        Contacts.HAS_PHONE_NUMBER,              // 11
        Contacts.IS_USER_PROFILE,               // 12
    };

    protected static final int CONTACT_ID_COLUMN_INDEX = 0;
    protected static final int CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    protected static final int CONTACT_DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
    protected static final int CONTACT_SORT_KEY_PRIMARY_COLUMN_INDEX = 3;
    protected static final int CONTACT_STARRED_COLUMN_INDEX = 4;
    protected static final int CONTACT_PRESENCE_STATUS_COLUMN_INDEX = 5;
    protected static final int CONTACT_CHAT_CAPABILITY_COLUMN_INDEX = 6;
    protected static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 7;
    protected static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 8;
    protected static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 9;
    protected static final int CONTACT_PHONETIC_NAME_COLUMN_INDEX = 10;
    protected static final int CONTACT_HAS_PHONE_COLUMN_INDEX = 11;
    protected static final int CONTACT_IS_USER_PROFILE = 12;

    protected static final String[] PROJECTION_RAW_CONTACT = new String[] {
        RawContacts._ID,                  // 0
        RawContacts.CONTACT_ID,           // 1
        RawContacts.DISPLAY_NAME_PRIMARY, // 2
        RawContacts.ACCOUNT_TYPE,         // 3
        RawContacts.ACCOUNT_NAME,         // 4
        RawContacts.DATA_SET              // 5
    };

    protected static final int RAWCONTACT_RAW_CONTACT_ID_COLUMN_INDEX = 0;
    protected static final int RAWCONTACT_CONTACT_ID_COLUMN_INDEX = 1;
    protected static final int RAWCONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 2;
    protected static final int RAWCONTACT_ACCOUNTTYPE_COLUMN_INDEX = 3;
    protected static final int RAWCONTACT_ACCOUNTNAME_COLUMN_INDEX = 4;
    protected static final int RAWCONTACT_DATASET_COLUMN_INDEX = 5;

    /**
     * Modes that specify the status of the editor
     */
    public enum Status {
        SELECTING_ACCOUNT, // Account select dialog is showing
        LOADING,    // Loader is fetching the group metadata
        EDITING,    // Not currently busy. We are waiting forthe user to enter data.
        SAVING,     // Data is currently being saved
        CLOSING     // Prevents any more saves
    }

    private Context mContext;
    private String mAction;
    private Bundle mIntentExtras;
    private String mGroupTitle;
    private Listener mListener;

    private Status mStatus;

    private ViewGroup mRootView;
    private ListView mListView;
    private LayoutInflater mLayoutInflater;

    private TextView mGroupNameView;
    private AutoCompleteTextView mAutoCompleteTextView;
    private Bundle mSavedInstanceState;  // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026

    private String mAccountName;
    private String mAccountType;
    private String mDataSet;
    private Bundle mPhone2name;
    private Bundle mEmail2name;
    private String mEmailAddresses;
    private ArrayList<Long> mDataIds = new ArrayList<Long>();
    private ArrayList<Long> mContactIds = new ArrayList<Long>();
    private CreateContactsTask mCreateContactsTask ;
    private GroupInsertHelper mGroupInsertHelper;
    private CheckMemberTask mCheckMemberTask;
    private long [] mDataIdsArray;
    ArrayList<ContentProviderOperation> mOperationList = new ArrayList<ContentProviderOperation>();
    private Handler mUiHander =  new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //LocalGroupEditorFragment.this.showDialog(msg.what);
        }
    };

    private boolean mGroupNameIsReadOnly = false;
    private String mOriginalGroupName = "";
    private int mLastGroupEditorId;

    private MemberListAdapter mMemberListAdapter;
    private ContactPhotoManager mPhotoManager;

    private ContentResolver mContentResolver;
    private SuggestedLocalGroupMemberListAdapter mAutoCompleteAdapter;

    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
    private ArrayList<Long> mListMembersContactIdToAdd = new ArrayList<Long>();
    private ArrayList<Long> mListMembersContactIdToRemove = new ArrayList<Long>();
    private ArrayList<Long> mListMembersContactIdToDisplay = new ArrayList<Long>();
    // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

    // gfj763: sort accountType ----------------------
    private ArrayList<RawMember> mListRawMembersToAdd = new ArrayList<RawMember>();
    private static final String LOADER_ARG_MEMBER_IDS = "memberContactIds";
    private static final String LOADER_ARG_RAW_MEMBER_LIST = "rawMemberList";
    private static final String LOADER_ARG_ACCOUNT_MEMBER_LIST = "accountMemberList";
    private static final String LOADER_ARG_PICKED_SESSION_ID="session_id";
    private static final boolean DEBUG = true;
    // -----------------------------------------------

    public LocalGroupEditorFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mLayoutInflater = inflater;
        mRootView = (ViewGroup) inflater.inflate(R.layout.group_editor_fragment, container, false);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mPhotoManager = ContactPhotoManager.getInstance(mContext);
        mMemberListAdapter = new MemberListAdapter(mContext, R.layout.group_member_item, null); // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        GROUP_EXIST_STRING = mContext.getString(R.string.group_exist);
        GROUP_NAME_CANT_EMPTY = mContext.getString(R.string.group_name_cant_empty);
        if (savedInstanceState != null) {
            mSavedInstanceState = savedInstanceState; // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
            // Just restore from the saved state.  No loading.
            onRestoreInstanceState(savedInstanceState);
            if(mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
               mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP)) {
                mGroupInsertHelper = new GroupInsertHelper(MyLooper.singleton());
                mCreateContactsTask = new CreateContactsTask(this);
                return;
            }
            if (mStatus == Status.SELECTING_ACCOUNT) {
                // Account select dialog is showing.  Don't setup the editor yet.
            } else if (mStatus == Status.LOADING) {
                startGroupMetaDataLoader();
            } else {
                setupEditorForAccount();
            }
        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            startGroupMetaDataLoader();
        } else if (Intent.ACTION_INSERT.equals(mAction)) {
            setupEditorForAccount();
        } else if (mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP)) {
            //If group name is empty, will let user to input group name
            if ((mGroupTitle == null) ||
                ((mGroupTitle != null) && TextUtils.isEmpty(mGroupTitle.trim()))) {
                Toast.makeText(mContext, GROUP_NAME_CANT_EMPTY, Toast.LENGTH_SHORT).show();
                AddGroupNameDialogFragment.show(getFragmentManager(), this, R.string.new_group);
                return;
            }

            //If group name exists, will let user to input group name
            if( isGroupNameExisted() ){
                Toast.makeText(mContext, GROUP_EXIST_STRING, Toast.LENGTH_SHORT).show();
                AddGroupNameDialogFragment.show(getFragmentManager(), this, R.string.new_group);
                return;
            }

            mGroupInsertHelper = new GroupInsertHelper(MyLooper.singleton());
            mCreateContactsTask = new CreateContactsTask(this);

            processGroupMember();

        } else if (mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP)) {
            AddGroupNameDialogFragment.show(getFragmentManager(), this, R.string.new_group);
        } else {
            throw new IllegalArgumentException("Unknown Action String " + mAction +
                    ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT);
        }
    }

    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        destroyLoader();
        mMemberListAdapter = null;
    }

    private void destroyLoader() {
        if (getLoaderManager().getLoader(LOADER_GROUP_METADATA) != null) {
            getLoaderManager().destroyLoader(LOADER_GROUP_METADATA);
        }

        if (getLoaderManager().getLoader(LOADER_EDITOR_MEMBERS) != null) {
            getLoaderManager().destroyLoader(LOADER_EDITOR_MEMBERS);
        }
    }
    // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

    private void startGroupMetaDataLoader() {
        mStatus = Status.LOADING;
        // gfj763: if groupTitle is empty, no need to initLoader for editor
        if (TextUtils.isEmpty(mGroupTitle)) {
            Log.i(TAG, "Group title is null or empty: " + mGroupTitle + ", Closing activity now.");
            if (mListener != null) {
                mListener.onGroupNotFound();
            }
            return;
        }
        getLoaderManager().initLoader(LOADER_GROUP_METADATA, null,
                mGroupMetaDataLoaderListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_GROUP_TITLE, mGroupTitle);

        outState.putSerializable(KEY_STATUS, mStatus);

        outState.putBoolean(KEY_GROUP_NAME_IS_READ_ONLY, mGroupNameIsReadOnly);
        outState.putString(KEY_ORIGINAL_GROUP_NAME, mOriginalGroupName);

        // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
        outState.putLongArray(KEY_MEMBERS_TO_ADD, LocalGroupUtils.convertToLongArray(mListMembersContactIdToAdd));
        outState.putLongArray(KEY_MEMBERS_TO_REMOVE, LocalGroupUtils.convertToLongArray(mListMembersContactIdToRemove));
        outState.putLongArray(KEY_MEMBERS_TO_DISPLAY, LocalGroupUtils.convertToLongArray(mListMembersContactIdToDisplay));
        // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

        outState.putParcelableArrayList(KEY_RAW_MEMBERS_TO_ADD, mListRawMembersToAdd);
        if(mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
           mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP)) {

            outState.putString(KEY_EMAIL_ADDRESS, mEmailAddresses);
            outState.putBundle(KEY_EMAIL_NAME, mEmail2name);
            outState.putBundle(KEY_PHONE_NAME, mPhone2name);
            outState.putLongArray(KEY_DATA_IDS, mDataIdsArray);
        }
    }

    private void onRestoreInstanceState(Bundle state) {
        mAction = state.getString(KEY_ACTION);
        mGroupTitle = state.getString(KEY_GROUP_TITLE);

        mStatus = (Status) state.getSerializable(KEY_STATUS);

        mGroupNameIsReadOnly = state.getBoolean(KEY_GROUP_NAME_IS_READ_ONLY);
        mOriginalGroupName = state.getString(KEY_ORIGINAL_GROUP_NAME);

        // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
        mListMembersContactIdToAdd = LocalGroupUtils.convertToLongList(state.getLongArray(KEY_MEMBERS_TO_ADD));
        mListMembersContactIdToRemove = LocalGroupUtils.convertToLongList(state.getLongArray(KEY_MEMBERS_TO_REMOVE));
        mListMembersContactIdToDisplay = LocalGroupUtils.convertToLongList(state.getLongArray(KEY_MEMBERS_TO_DISPLAY));
        // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

        mListRawMembersToAdd = state.getParcelableArrayList(KEY_RAW_MEMBERS_TO_ADD);
        if(mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
           mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP)) {

            mEmailAddresses = state.getString(KEY_EMAIL_ADDRESS);
            mEmail2name = state.getBundle(KEY_EMAIL_NAME);
            mPhone2name = state.getBundle(KEY_PHONE_NAME);
            mDataIdsArray = state.getLongArray(KEY_DATA_IDS);
            if(mDataIdsArray != null) {
                mDataIds.clear();
                for (Long dataId : mDataIdsArray) {
                    mDataIds.add(dataId);
                }
            }
        }
    }

    public void setContentResolver(ContentResolver resolver) {
        mContentResolver = resolver;
        if (mAutoCompleteAdapter != null) {
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
        }
    }

    private void selectAccount() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(mContext).getAccounts(true /* writeable */);
        // No Accounts available
        if (accounts.isEmpty()) {
            Log.e(TAG, "No accounts were found.");
            //MOT MOD BEGIN IKMAIN-37353
            //If no account in device, we will save contact to null
            Message msg = mGroupInsertHelper.obtainMessage();
            msg.what = GroupInsertHelper.MESSAGE_INSERT_UNKNOWN;
            Account rememberredAccount = null;
            msg.obj = rememberredAccount;
            mGroupInsertHelper.sendMessage(msg);
            //MOT MOD END IKMAIN-37353
            return;
        }

        // In the common case of a single account being writable, auto-select
        // it without showing a dialog.
        if (accounts.size() == 1) {
            mAccountName = accounts.get(0).name;
            mAccountType = accounts.get(0).type;
            mDataSet = accounts.get(0).dataSet;

        Message msg = mGroupInsertHelper.obtainMessage();
        msg.what = GroupInsertHelper.MESSAGE_INSERT_UNKNOWN;
        Account rememberredAccount = new Account(mAccountName, mAccountType);
        msg.obj = rememberredAccount;
        mGroupInsertHelper.sendMessage(msg);
        return;

        }

        if (ContactPreferenceUtilities.IfUserChoiceRemembered(mContext) && ContactPreferenceUtilities.GetUserPreferredAccountIndex(mContext, true)!=-1) {
            AccountWithDataSet userPreAcc = ContactPreferenceUtilities.GetUserPreferredAccountFromSharedPreferences(mContext);

        Message msg = mGroupInsertHelper.obtainMessage();
        msg.what = GroupInsertHelper.MESSAGE_INSERT_UNKNOWN;
        msg.obj = userPreAcc;
        mGroupInsertHelper.sendMessage(msg);

        } else {
            mStatus = Status.SELECTING_ACCOUNT;
            SelectAccountDialogFragment.show(getFragmentManager(), this,
                    R.string.dialog_new_group_account, AccountListFilter.ACCOUNTS_GROUP_WRITABLE, null);
        }
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        mAccountName = account.name;
        mAccountType = account.type;
        mDataSet = account.dataSet;

        Message msg = mGroupInsertHelper.obtainMessage();
        msg.what = GroupInsertHelper.MESSAGE_INSERT_UNKNOWN;
        Account rememberredAccount = new Account(mAccountName, mAccountType);
        msg.obj = rememberredAccount;
        mGroupInsertHelper.sendMessage(msg);

    }

    @Override
    public void onAccountSelectorCancelled() {
        if (mListener != null) {
            // Exit the fragment because we cannot continue without selecting an account
            mListener.onGroupNotFound();
        }
    }

    private AccountType getAccountType() {
        return AccountTypeManager.getInstance(mContext).getAccountType(mAccountType, mDataSet);
    }

    @Override
    public void onGroupNameSet(String groupName) {
        mGroupTitle = groupName;
        if ((mGroupTitle != null) && TextUtils.isEmpty(mGroupTitle.trim())) {
            Toast.makeText(mContext, GROUP_NAME_CANT_EMPTY, Toast.LENGTH_SHORT).show();
            AddGroupNameDialogFragment.show(getFragmentManager(), this, R.string.new_group);
            return;
        }


        if( isGroupNameExisted() ){
            Toast.makeText(mContext, GROUP_EXIST_STRING, Toast.LENGTH_SHORT).show();
            int resultCode = Activity.RESULT_CANCELED;
            Intent resultIntent = null;
            if (mListener != null) {
                mListener.onSaveFinished(resultCode, resultIntent); // resultCode = -1
            }
            return;
        }

        if (mGroupInsertHelper==null) {
            mGroupInsertHelper = new GroupInsertHelper(MyLooper.singleton());
        }
        mCreateContactsTask = new CreateContactsTask(this);

        processGroupMember();

    }

    @Override
    public void onGroupNameCancelled() {
        if (mListener != null) {
            // Exit the fragment because we cannot continue without selecting an account
            mListener.onGroupNotFound();
        }
    }

    private void processGroupMember() {
        if (mCheckMemberTask != null) {
            mCheckMemberTask.cancel(true);
        }
        mCheckMemberTask = new CheckMemberTask(mContext, mGroupTitle,LocalGroupEditorFragment.this);
        mCheckMemberTask.execute();
    }

    /**
     * Sets up the editor based on the group's account name and type.
     */
    private void setupEditorForAccount() {
        final boolean editable = true;  // gfj763: suppose all member could be added to local group
        boolean isNewEditor = false;
        mMemberListAdapter.setIsGroupMembershipEditable(editable);

        // Since this method can be called multiple time, remove old editor if the editor type
        // is different from the new one and mark the editor with a tag so it can be found for
        // removal if needed
        View editorView;
        int newGroupEditorId =
                editable ? R.layout.local_group_editor_view : R.layout.external_group_editor_view;
        if (newGroupEditorId != mLastGroupEditorId) {
            View oldEditorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (oldEditorView != null) {
                mRootView.removeView(oldEditorView);
            }
            editorView = mLayoutInflater.inflate(newGroupEditorId, mRootView, false);
            editorView.setTag(CURRENT_EDITOR_TAG);
            mAutoCompleteAdapter = null;
            mLastGroupEditorId = newGroupEditorId;
            isNewEditor = true;
        } else {
            editorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (editorView == null) {
                throw new IllegalStateException("Group editor view not found");
            }
        }

        mGroupNameView = (TextView) editorView.findViewById(R.id.group_name);
        mGroupNameView.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(Constants.GROUPNAME_MAX_LENGTH) });
        mAutoCompleteTextView = (AutoCompleteTextView) editorView.findViewById(
                R.id.add_member_field);

        mListView = (ListView) editorView.findViewById(android.R.id.list);
        mListView.setAdapter(mMemberListAdapter);

        // Setup the autocomplete adapter (for contacts to suggest to add to the group) based on the
        // account name and type. For groups that cannot have membership edited, there will be no
        // autocomplete text view.
        if (mAutoCompleteTextView != null) {
            mAutoCompleteAdapter = new SuggestedLocalGroupMemberListAdapter(mContext,
                    android.R.layout.simple_dropdown_item_1line);
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
            mAutoCompleteTextView.setAdapter(mAutoCompleteAdapter);
            mAutoCompleteTextView.setThreshold(1); // MOTO MOD, PIM-Contacts, gfj763, IKHSS7-1534
            mAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SuggestedMember member = (SuggestedMember) view.getTag();
                    if (member == null) {
                        return; // just in case
                    }
                    addMember(member.getContactId());  // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
                    // Clear out the text field
                    mAutoCompleteTextView.setText("");
                }
            });
            // Update the exempt list.  (mListToDisplay might have been restored from the saved
            // state.)
            mAutoCompleteAdapter.updateExistingMembersList(mListMembersContactIdToDisplay); // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
        }

        // If the group name is ready only, don't let the user focus on the field.
        if(mStatus == Status.EDITING){
            mGroupNameView.setFocusable(!mGroupNameIsReadOnly);
        }

        if(isNewEditor) {
            mRootView.addView(editorView);
            // We need to re-query members based on display list when we rotate screen
            if ((mSavedInstanceState != null) && ((mStatus == Status.EDITING) || (mStatus == Status.SAVING))) {
                queryGroupEditorMembers();
            }
        }

        if (mStatus != mStatus.SAVING) {
            mStatus = Status.EDITING;
        }
    }

    public void addMembers(final Intent intent){

        Boolean isResultIncluded = intent.getBooleanExtra(Resultable.SELECTTION_RESULT_INCLUDED, false);
        StringBuilder ContactIdListBuilder = new StringBuilder();

        Uri uri = null;

        Log.i(TAG, "result included : " + isResultIncluded);

        if(!isResultIncluded){//send the multiple picker session id as the uri paramerter, contacts provider will handle it
            final long sessionId = intent.getLongExtra(Resultable.SELECTION_RESULT_SESSION_ID, -1);
            if(sessionId == -1){
                Log.e(TAG, "error in the sessionId : " + sessionId);
                return;
            }else{
                startPickedContactsLoader(sessionId);
            }
        }else{//retrieve the lookups from the result directly
            ArrayList<Uri> selected_contact_uris = intent.getParcelableArrayListExtra(Resultable.SELECTED_CONTACTS);
            if (selected_contact_uris.size() == 0) {//if the user doesn't picked any contacts, return directly.
                return;
            }

            int index = 0;
            mContactIds.clear();
            while (index < selected_contact_uris.size()) {
                if (index != 0)
                    ContactIdListBuilder.append(',');
                Uri lookup_uri = selected_contact_uris.get(index);
                final List<String> pathSegments = lookup_uri.getPathSegments();
                final int segmentCount = pathSegments.size();
                if(segmentCount >= 4){

                    String contact_id = pathSegments.get(3);
                    long id = 0;
                    try{

                     id = Long.valueOf(contact_id);
                    }catch(java.lang.NumberFormatException e){
                        id = 0;
                    }

                    if(id != 0 && !mContactIds.contains(id)){
                        mContactIds.add(id);
                    }

                }else{
                    Log.e(TAG, "error in the lookup uri: " + lookup_uri);
                }
                index++;
            }

            String newAddMemberIdArgs = buildSelectionArgsFromContactIds();

            Log.e(TAG, newAddMemberIdArgs);
            addGroupMemberFromPicker(mContactIds);


            /*
            if (!TextUtils.isEmpty(newAddMemberIdArgs)) {
                startMemberRawDataLoader(newAddMemberIdArgs, mListRawMembersToAdd);
            }*/
        }
    }

    public void load(String action, String groupTitle, Bundle intentExtras) {
        mAction = action;
        mGroupTitle = groupTitle;
        mIntentExtras = intentExtras;
    }

    public void load(String action, String groupTitle, String emailAddresses) {
        mAction = action;
        mGroupTitle = groupTitle;
        mEmailAddresses = emailAddresses;
        mPhone2name = new Bundle();
    }

    public void load(String action, String groupTitle, Bundle phone2Name, Bundle email2Name, long [] dataIdsArray) {
        mAction = action;
        mGroupTitle = groupTitle;
        if(phone2Name == null) {
            mPhone2name = new Bundle();
        } else {
            mPhone2name = phone2Name;
        }
        if(email2Name == null) {
            mEmail2name = new Bundle();
        } else {
            mEmail2name = email2Name;
        }
        if(dataIdsArray != null) {
            mDataIdsArray = dataIdsArray;
            mDataIds.clear();
            for (Long dataId : dataIdsArray) {
                mDataIds.add(dataId);
            }
        }
    }

    private void bindGroupMetaData(Cursor cursor) {
        if ( (null == cursor) || (!cursor.moveToFirst()) ) {
            Log.i(TAG, "Group not found with title: " + mGroupTitle + ", Closing activity now.");
            // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKHSS6UPGR-3667
            // We can't finish GroupEditorActivity for callback intent need to be handled.
            /*
            if (mListener != null) {
                mListener.onGroupNotFound();
            }
            */
            // MOTO MOD END, PIM-Contacts, gfj763, IKHSS6UPGR-3667
            return;
        }

        mOriginalGroupName = cursor.getString(LocalGroupMetaDataLoader.TITLE);

        // gfj763: we define if one group is read-only, the aggregation group is read-only.
        boolean is_group_read_only = false;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            is_group_read_only = (cursor.getInt(LocalGroupMetaDataLoader.IS_READ_ONLY) == 1);
            mGroupNameIsReadOnly |= is_group_read_only;
            if (mGroupNameIsReadOnly)
                break;
        }

        setupEditorForAccount();

        // Setup the group metadata display
        mGroupNameView.setText(mOriginalGroupName);
    }

    // gfj763: bind account information with related members
    public void bindGroupAccountWithMember(Cursor cursor) {
        LocalGroupAccountMemberData groupAccountMemberList;
        LocalGroupAccountMemberData motoLocalGroupAccountMemberList;
        ArrayList<LocalGroupAccountMemberData> groupAccountMemberListArray = new ArrayList<LocalGroupAccountMemberData>();

        String groupTitle = null;
        if(mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
           mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP) ) {
            groupTitle = mGroupTitle;
        } else {
            groupTitle = getGroupTitle();
        }

        if (null == cursor || cursor.getCount() <= 0) {
            Log.i(TAG, " No valid account information got from selected members !");
        } else {
            log(" Valid account number: " + cursor.getCount());
        }

        // Create moto local group first, no matter exist membership or not, and insert it to groupAccount Array.
        // MOT MOD BEGIN - IKHSS6-1470: group redesign 2nd release, to add new mimetype for localgroup
        motoLocalGroupAccountMemberList = new LocalGroupAccountMemberData
               (groupTitle, GroupAPI.LOCAL_GROUP_ACCOUNT_TYPE,
                       GroupAPI.LOCAL_GROUP_ACCOUNT_NAME, null, false);
        // MOT MOD END - IKHSS6-1470

//        groupAccountMemberListArray.add(motoLocalGroupAccountMemberList);
//        LocalGroupAccountMemberData motoLocalGroupAccount = groupAccountMemberListArray.get(0);
        LocalGroupAccountMemberData motoLocalGroupAccount = motoLocalGroupAccountMemberList;

        if (cursor != null) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                String groupAccountType = cursor.getString(LocalGroupAccountLoader.RAWCONTACT_ACCOUNT_TYPE_COLUMN_INDEX);
                String groupAccountName = cursor.getString(LocalGroupAccountLoader.RAWCONTACT_ACCOUNT_NAME_COLUMN_INDEX);
                String groupDataSet = cursor.getString(LocalGroupAccountLoader.RAWCONTACT_DATASET_COLUMN_INDEX);

                if (DEBUG) {
                    log("groupAccountType = " + groupAccountType + ", groupAccountName = " +
                            groupAccountName + ", groupDataSet = " + groupDataSet);
                }

                boolean isGroupMemberEditable = LocalGroupUtils.isLocalGroupMembershipEditable(mContext, groupAccountType, groupDataSet);

                if (DEBUG) {
                    if (isGroupMemberEditable) {
                        log("The group membership is editable.");
                    } else {
                        log("The group membership is not editable.");
                    }
                }

                groupAccountMemberList = new LocalGroupAccountMemberData
                              (groupTitle, groupAccountType, groupAccountName, groupDataSet, isGroupMemberEditable);

                for (RawMember member : mListRawMembersToAdd) {
                    long memberRawContactId = member.getRawContactId();
                    String memberAccountType = member.getAccountType();
                    String memberAccountName = member.getAccountName();
                    String memberDataSet = member.getDataSet();

                    if (TextUtils.equals(groupAccountType, memberAccountType) &&
                            TextUtils.equals(groupAccountName, memberAccountName) &&
                            TextUtils.equals(groupDataSet, memberDataSet)) {
                        if (isGroupMemberEditable) {
                            groupAccountMemberList.insertRawContactId(memberRawContactId);
                        } else {
                            motoLocalGroupAccount.insertRawContactId(memberRawContactId);
                        }
                    }
                }

                // All membership belong to the group has finished, append the list
                // if the editable group has valid membership, append it to Array.
                if (!groupAccountMemberList.getRawContactIdList().isEmpty()) {
                    groupAccountMemberListArray.add(groupAccountMemberList);
                }
            }
        } else {
        	groupAccountMemberListArray.add(motoLocalGroupAccountMemberList);
        }

        if (DEBUG) {
            for (LocalGroupAccountMemberData groupData : groupAccountMemberListArray) {
                log("******* Group Account Info *********" );
                log("title:" + groupData.getGroupTitle() + ", accountType: " +  groupData.getAccountType()
                        + ", accountName: " + groupData.getAccountName() + ", editable: " + groupData.isGroupMembershipEditable());

                StringBuilder rawIdSB = new StringBuilder();

                if (groupData.getRawContactIdList().isEmpty()) {
                    log("memberRawId: empty.");
                } else {
                    for (long rawId : groupData.getRawContactIdList()) {
                        rawIdSB.append(String.valueOf(rawId) + ",");
                    }
                    log("memberRawId: " + rawIdSB.toString());
                }
            }
        }

        Activity activity = getActivity();
        // If the activity is not there anymore, then we can't continue with the save process.
        if (activity == null) {
            return;
        }

        Intent saveIntent = null;
        if (Intent.ACTION_INSERT.equals(mAction) ||
            mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
            mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP) ) {
            saveIntent = LocalGroupSaveService.createNewLocalGroupIntent(activity,
                    groupAccountMemberListArray,
                    activity.getClass(),
                    GroupEditorActivity.ACTION_SAVE_COMPLETED);
        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            // if in edit mode, and cursor is null, means no new contact added.
            if (cursor == null) {
                groupAccountMemberListArray = null;
            }

            long[] membersToRemoveArray = LocalGroupUtils.convertToLongArray(mListMembersContactIdToRemove);

            saveIntent = LocalGroupSaveService.createLocalGroupUpdateIntent(activity, mOriginalGroupName,
                    getUpdatedName(), groupAccountMemberListArray, membersToRemoveArray,
                    activity.getClass(), GroupEditorActivity.ACTION_SAVE_COMPLETED);
        } else {
            throw new IllegalStateException("Invalid intent action type " + mAction);
        }

        activity.startService(saveIntent);
    }


    public void setListener(Listener value) {
        mListener = value;
    }

    // gfj763: Temporarily We don't need to judge whether the group membership
    //         is editable or not, all contacts could be added to local group.
    public void onDoneClicked() {
        /*
        if (isGroupMembershipEditable()) {
            save(SaveMode.CLOSE);
        } else {
            // Just revert it.
            doRevertAction();
        }
        */
        save(SaveMode.CLOSE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.edit_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_discard:
                return revert();
//            case R.id.menu_edit_member:
//                return editGroupMember();                
            case R.id.moto_group_add_members:
                if(RomUtility.isOutofMemory()){
                    Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    return true;
                    }

                if(mListener != null){
				    //begin mtdr83 for IKCBSMMCPPRC-1310 
                	ArrayList<String> excludeId = new ArrayList<String>();
                	if(mListMembersContactIdToDisplay != null){
                		Iterator<Long> id =  mListMembersContactIdToDisplay.iterator();
                		Log.d(TAG, "mListMembersContactIdToDisplay = " + mListMembersContactIdToDisplay);
                		while(id.hasNext()){
                			Long temp = id.next();
                			Log.d(TAG, "temp = " + temp);
                			excludeId.add(temp.toString());
                		}
                	}
                    mListener.onAddGroupMembers(excludeId);
					//end mtdr83 for IKCBSMMCPPRC-1310 
                }

                return true;
        }
        return false;
    }
    
/*    private boolean editGroupMember() {
        Intent mpickIntent = new Intent("motorola.intent.ACTION_MULTIPLE_PICK", Contacts.CONTENT_URI);    // platform multipicker
        // MultiPickerUnselectAll = true : the picker don't select all items
        mpickIntent.putExtra("com.android.contacts.MultiPickerUnselectAll", true);        
        // pass the current contacts array to the multiple picker, the picker will selected these contacts when launch.
        ArrayList<String> selectedContacts = new ArrayList<String>();
        for (Member member : mListToDisplay) {
            selectedContacts.add(String.valueOf(member.getContactId()));
        }
        mpickIntent.putStringArrayListExtra(Resultable.SELECTED_CONTACTS_INCLUDED, selectedContacts);
        startActivityForResult(mpickIntent, MULTI_PICK_CONTACT_REQUEST);
        return true;
    }    

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case MULTI_PICK_CONTACT_REQUEST: {
                new MulPickResult().execute(data);               
                break;
            }
        }
    }*/

    /**
     * Represents an asynchronous task used to get the multiple picker result
     */
/*    public class MulPickResult extends AsyncTask<Intent, Void, Void> {

        ArrayList<Uri> selected_contact_uris = new ArrayList<Uri>();                    
        
        @Override
        protected Void doInBackground(Intent... params) {
            Intent data = params[0];
            // platform's multipicker  - contacts based
            ContactsUtils.getMultiplePickResult(mContext, data, selected_contact_uris);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            String selectedIdArgs = buildSelectedIdList(selected_contact_uris);
            startSelectedContactsLoader(selectedIdArgs);        	
        }
    }
*/

    private boolean revert() {
        if (!hasNameChange() && !hasMembershipChange()) {
            doRevertAction();
        } else {
            CancelEditDialogFragment.show(this);
        }
        return true;
    }

    private void doRevertAction() {
        // When this Fragment is closed we don't want it to auto-save
        mStatus = Status.CLOSING;
        if (mListener != null) mListener.onReverted();
    }

    public static class CancelEditDialogFragment extends DialogFragment {

        public static void show(LocalGroupEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.cancel_confirmation_dialog_title)
                    .setMessage(R.string.cancel_confirmation_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((LocalGroupEditorFragment) getTargetFragment()).doRevertAction();
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            return dialog;
        }
    }

    /**
     * Saves or creates the group based on the mode, and if successful
     * finishes the activity. This actually only handles saving the group name.
     * @return true when successful
     */
    public boolean save(int saveMode) {
        if (!hasValidGroupName() || mStatus != Status.EDITING) {
            log("No valid group name or mStatus is not 'EDITING': mStatus=" + mStatus);
            if ((mGroupNameView != null) && TextUtils.isEmpty(getGroupTitle())) {
                Toast.makeText(mContext, mContext.getString(R.string.group_name_cant_empty), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        // If we are about to close the editor - there is no need to refresh the data
        if (saveMode == SaveMode.CLOSE) {
            // getLoaderManager().destroyLoader(LOADER_EDITOR_MEMBERS); // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
        }

        // If there are no changes, then go straight to onSaveCompleted()
        if (!hasNameChange() && !hasMembershipChange()) {
            String groupTitle = getGroupTitle();
            onSaveCompleted(false, SaveMode.CLOSE, groupTitle);
            return true;
        }

        // gfj763: Local Group not support duplicate group name
        if (hasNameChange() && isGroupNameExisted()) {
            Toast.makeText(mContext, GROUP_EXIST_STRING, Toast.LENGTH_SHORT).show();
            return false;
        }

        mStatus = Status.SAVING;

        Activity activity = getActivity();
        // If the activity is not there anymore, then we can't continue with the save process.
        if (activity == null) {
            return false;
        }
        Intent saveIntent = null;

        if (Intent.ACTION_INSERT.equals(mAction)) {
            // no contacts need to handle, directly create the MOTO agg group
            if (!mListMembersContactIdToAdd.isEmpty()) {
                String newAddMemberIdArgs = LocalGroupUtils.buildSelectionIdList(mListMembersContactIdToAdd); // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
                if (!TextUtils.isEmpty(newAddMemberIdArgs)) {
                    //gfj763: In order to keep loader sequence, init 2 loader sequently.
                    startMemberRawDataLoader(newAddMemberIdArgs, mListRawMembersToAdd);
                }
            } else {
                bindGroupAccountWithMember(null);
            }
        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            // gfj763: judge if there are new contacts joined
            if (!mListMembersContactIdToAdd.isEmpty()) {
                String newAddMemberIdArgs = LocalGroupUtils.buildSelectionIdList(mListMembersContactIdToAdd); // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
                if (!TextUtils.isEmpty(newAddMemberIdArgs)) {
                    startMemberRawDataLoader(newAddMemberIdArgs, mListRawMembersToAdd);
                }
            } else {
                // gfj763: Maybe changed group name, or removed membership.
                bindGroupAccountWithMember(null);
            }
        } else {
            throw new IllegalStateException("Invalid intent action type " + mAction);
        }

        DialogFragment newFragment = ProgressDialogFragment.newInstance(
                   getGroupTitle());
        newFragment.show(getFragmentManager(), "dialog");
        return true;
    }

    public void onSaveCompleted(boolean hadChanges, int saveMode, String groupTitle) {
        boolean success = !TextUtils.isEmpty(groupTitle);
        log("onSaveCompleted(" + saveMode + ", " + groupTitle + ")");
        if (hadChanges) {
            Toast.makeText(mContext, success ? R.string.groupSavedToast :
                    R.string.groupSavedErrorToast, Toast.LENGTH_SHORT).show();
            // BEGIN Motorola, FTR 36344, IKPIM-1040
            if (success) {
                if (mListMembersContactIdToAdd.size() > 0) {
                    long[] addedContactIds = LocalGroupUtils.convertToLongArray(mListMembersContactIdToAdd);
                    MEContacts.onGroupMembership(getActivity().getApplicationContext(),
                        MEContacts.TAG_ID_GROUP_ASN, groupTitle, addedContactIds);
                }

                if (mListMembersContactIdToRemove.size() > 0) {
                    long[] RemovedContactIds = LocalGroupUtils.convertToLongArray(mListMembersContactIdToRemove);
                    MEContacts.onGroupMembership(getActivity().getApplicationContext(),
                        MEContacts.TAG_ID_GROUP_DEA, groupTitle, RemovedContactIds);
                }
            }
            // END IKPIM-1040
        }
        switch (saveMode) {
            case SaveMode.CLOSE:
            case SaveMode.HOME:
                final Intent resultIntent;
                final int resultCode;
                if (success) {
                    resultIntent = new Intent();
                    log("LocalGroupEditorFragment::onSaveCompleted(), groupTitle = " + groupTitle);
                    resultIntent.putExtra(LocalGroupUtils.EXTRA_SELECTED_LOCAL_GROUP_TITLE, groupTitle);
                    resultCode = Activity.RESULT_OK;
                } else {
                    resultCode = Activity.RESULT_CANCELED;
                    resultIntent = null;
                }
                // It is already saved, so prevent that it is saved again
                mStatus = Status.CLOSING;
                if (mListener != null) {
                    log("LocalGroupEditorFragment::onSaveCompleted(), invoke mListener.onSaveFinished, resultCode=" + resultCode);
                    mListener.onSaveFinished(resultCode, resultIntent); // resultCode = -1
                }
                break;
            case SaveMode.RELOAD:
                // TODO: Handle reloading the group list
            default:
                throw new IllegalStateException("Unsupported save mode " + saveMode);
        }
    }

    private boolean hasValidGroupName() {
        return (mGroupNameView != null) && (!TextUtils.isEmpty(getGroupTitle()));
    }

    private boolean hasNameChange() {
        return mGroupNameView != null &&
                !(getGroupTitle().equals(mOriginalGroupName));
    }

    private boolean hasMembershipChange() {
        return mListMembersContactIdToAdd.size() > 0 || mListMembersContactIdToRemove.size() > 0; // MOTO MOD, PIM-Contacts, gfj763, IKPIM-1026
    }

    private boolean isGroupNameExisted() {
        boolean isGroupNameExist = false;

        final String[] COLUMNS = new String[] {
            Groups._ID,
            Groups.TITLE,
        };

        String groupName = null;
        if(mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ||
           mAction.equals(GroupAPI.GroupIntents.ACTION_SAVING_GROUP) ) {
            groupName = mGroupTitle;
        } else {
            groupName = getGroupTitle();
        }
        Cursor cursor = mContentResolver.query(Groups.CONTENT_URI, COLUMNS,
                            Groups.ACCOUNT_TYPE + " NOT NULL AND "
                            + Groups.ACCOUNT_NAME + " NOT NULL AND " + Groups.DELETED + "=0 AND "
                            + Groups.TITLE + "=?", new String[] {groupName}, null);
        try {
            if (null == cursor ) {
                isGroupNameExist = false;
            }

            if ( (null != cursor) && (cursor.getCount() > 0) ) {
                isGroupNameExist = true;
            } else {
                isGroupNameExist = false;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        return isGroupNameExist;
    }

    /**
     * Returns the group's new name or null if there is no change from the
     * original name that was loaded for the group.
     */
    private String getUpdatedName() {
        String groupNameFromTextView = getGroupTitle();
        if (groupNameFromTextView.equals(mOriginalGroupName)) {
            // No name change, so return null
            return null;
        }
        return groupNameFromTextView;
    }

    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
    private void addExistingMembers(List<Long> membersId, Cursor cursor) {
        // Re-create the list to display
        mListMembersContactIdToDisplay.clear();
        mListMembersContactIdToDisplay.addAll(membersId);
        mListMembersContactIdToDisplay.addAll(mListMembersContactIdToAdd);
        mListMembersContactIdToDisplay.removeAll(mListMembersContactIdToRemove);
        //mMemberListAdapter.notifyDataSetChanged();
        mMemberListAdapter.changeCursor(cursor);

        // Update the autocomplete adapter (if there is one) so these contacts don't get suggested
        if (mAutoCompleteAdapter != null) {
            mAutoCompleteAdapter.updateExistingMembersList(membersId);
        }
    }

    private void addMember(long contactId) {
        // Update the display list
        if (mListMembersContactIdToRemove.contains(contactId)) {
            mListMembersContactIdToRemove.remove(contactId);
        } else {
            mListMembersContactIdToAdd.add(contactId);
        }
        mListMembersContactIdToDisplay.add(contactId);
        queryGroupEditorMembers();
        // Update the autocomplete adapter so the contact doesn't get suggested again
        mAutoCompleteAdapter.addNewMember(contactId);
    }

    private void removeMember(long contactId) {
        // If the contact was just added during this session, remove it from the list of
        // members to add
        if (mListMembersContactIdToAdd.contains(contactId)) {
            mListMembersContactIdToAdd.remove(contactId);
        } else {
            // Otherwise this contact was already part of the existing list of contacts,
            // so we need to do a content provider deletion operation
            mListMembersContactIdToRemove.add(contactId);
        }
        // In either case, update the UI so the contact is no longer in the list of
        // members
        mListMembersContactIdToDisplay.remove(contactId);
        queryGroupEditorMembers();

        // Update the autocomplete adapter so the contact can get suggested again
        mAutoCompleteAdapter.removeMember(contactId);
    }

    private void addGroupMemberFromPicker(ArrayList<Long> idList) {
        // No member is picked, no need refresh member list
        if (idList.isEmpty()) {
            return;
        }

        int oldDisplaySize = mListMembersContactIdToDisplay.size();

        for (long id : idList) {
            // if member display list already contain this id, skip
            // else add this member id to display & new add list
            if (mListMembersContactIdToDisplay.contains(id)) {
                continue;
            } else {
                mListMembersContactIdToDisplay.add(id);

                //a contacts maybe just be remove but not save
                //when we need add it again ,we need remove from mListMembersContactIdToRemove
                if(mListMembersContactIdToRemove.contains(id))
                {
                    mListMembersContactIdToRemove.remove(id);
                } else {
                    mListMembersContactIdToAdd.add(id);
                }
            }
        }

        // re-query and refresh the member list if new members added
        if (mListMembersContactIdToDisplay.size() != oldDisplaySize) {
            queryGroupEditorMembers();
        }
    }
    // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

    /**
     * The listener for the group metadata (i.e. group name, account type, and account name) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new LocalGroupMetaDataLoader(mContext, mGroupTitle);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            bindGroupMetaData(data);
            // Load existing members
            getLoaderManager().restartLoader(LOADER_EDITOR_MEMBERS, null,
                    mGroupExistingMemberListLoaderListener);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };
    
/*    private void startSelectedContactsLoader(String memberIdArgs) {
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_MEMBER_IDS, memberIdArgs);
        getLoaderManager().restartLoader(LOADER_SELECTED_CONTACTS, args, mSelectedContactsLoaderListener);
    }*/

    /**
     * The listener to load selected contacs from multiple picker
     */
/*    private final LoaderManager.LoaderCallbacks<Cursor> mSelectedContactsLoaderListener =
            new LoaderCallbacks<Cursor>() {

        private String selectionArgs;
        private final String SELECTION = " _id IN (%s)";

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            selectionArgs = args.getString(LOADER_ARG_MEMBER_IDS);
            if (TextUtils.isEmpty(selectionArgs)) {  // maybe user un-select all
                selectionArgs = "";
            }
            return new CursorLoader(mContext, Contacts.CONTENT_URI,
                    PROJECTION_CONTACT, String.format(SELECTION, selectionArgs), null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<Member> listSelectedMembers = new ArrayList<Member>();        	
            data.moveToPosition(-1);
            while (data.moveToNext()) {            	
                long contactId = data.getLong(CONTACT_ID_COLUMN_INDEX);
                String displayName = data.getString(CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
                String lookupKey = data.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX);
                String photoUri = data.getString(CONTACT_PHOTO_URI_COLUMN_INDEX);            	            	
                listSelectedMembers.add(new Member(lookupKey, contactId, displayName, photoUri));
            }

            // re-calculate the add/remove list
            mListMembersToAdd.clear();         
            mListMembersToRemove.clear();
            for (Member memberSel : listSelectedMembers) {
                if (!mListMembersExisting.contains(memberSel))
                    mListMembersToAdd.add(memberSel);
            }
            for (Member memberExist : mListMembersExisting) {
                if (!listSelectedMembers.contains(memberExist))
                    mListMembersToRemove.add(memberExist);
            }            
            
            // Update the display list
            addExistingMembers();

            getLoaderManager().destroyLoader(LOADER_SELECTED_CONTACTS);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };
*/    

    private String buildSelectedIdList(ArrayList<Uri> memberList) {
        if ((null == memberList) || (memberList.size() == 0)) {
            return null;
        }

        List<String> memberContactIdList = new ArrayList<String>();
        for (Uri member : memberList) {
            memberContactIdList.add(String.valueOf(ContentUris.parseId(member)));
        }
        return TextUtils.join(",", memberContactIdList);
    }

    private void startMemberRawDataLoader(String memberIdArgs, ArrayList<RawMember> rawMemberList) {
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_MEMBER_IDS, memberIdArgs);
        args.putParcelableArrayList(LOADER_ARG_RAW_MEMBER_LIST, rawMemberList);
        getLoaderManager().restartLoader(LOADER_MEMBER_RAW_DATA, args, mContactRawDataLoaderListener);
    }

    private void startPickedContactsLoader(long session) {
        Bundle args = new Bundle();
        args.putLong(LOADER_ARG_PICKED_SESSION_ID, session);
        getLoaderManager().restartLoader(LOADER_PICKED_CONTACTS, args, mPickedContactLoaderListener);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mPickedContactLoaderListener =
            new LoaderCallbacks<Cursor>() {

            private long session_id;

            @Override
            public CursorLoader onCreateLoader(int id, Bundle args) {
                session_id = args.getLong(LOADER_ARG_PICKED_SESSION_ID);

                Uri sessionUri = ContentUris.withAppendedId(Uri.withAppendedPath(
                        Uri.parse("content://com.motorola.contacts.list.ContactMultiplePickerResult/results"),"session_id"),
                        session_id);

                return new CursorLoader(mContext, sessionUri,
                        new String[] {"data"}, null, null, null);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

                mContactIds.clear();

                while(cursor.moveToNext()) {
                    Uri lookup_uri = Uri.parse(cursor.getString(0));

                    final List<String> pathSegments = lookup_uri.getPathSegments();
                    final int segmentCount = pathSegments.size();
                    if(segmentCount >= 4){
                        String contact_id = pathSegments.get(3);
                        long id = 0;
                        try{

                         id = Long.valueOf(contact_id);
                        }catch(java.lang.NumberFormatException e){
                            id = 0;
                        }

                        if(id != 0 && !mContactIds.contains(id)){
                            mContactIds.add(id);
                        }
                    }else{
                        Log.e(TAG, "error in the lookup uri: " + lookup_uri);
                    }
                }

                String newAddMemberIdArgs = buildSelectionArgsFromContactIds();
                Log.e(TAG, newAddMemberIdArgs);
                /*
                if (!TextUtils.isEmpty(newAddMemberIdArgs)) {
                    startMemberRawDataLoader(newAddMemberIdArgs, mListRawMembersToAdd);
                }
                */
                addGroupMemberFromPicker(mContactIds);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {}

        };

    public String buildSelectionArgsFromContactIds() {
        String newAddMemberIdArgs = null;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mContactIds.size(); i++) {
            builder.append(mContactIds.get(i)).append(",");
        }
        if (isEmpty(builder.toString())) {
            newAddMemberIdArgs = EMPTY_STRING;
        } else {
            newAddMemberIdArgs = builder.substring(0, builder.length() - 1);
        }
        return newAddMemberIdArgs;
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mContactRawDataLoaderListener =
        new LoaderCallbacks<Cursor>() {

        private String selectionArgs;
        private ArrayList<RawMember> rawMemberList;
        private final String SELECTION = RawContacts.DELETED + "=0"
                             + " AND " + RawContacts.CONTACT_ID + " IN (%s)";

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            selectionArgs = args.getString(LOADER_ARG_MEMBER_IDS);
            rawMemberList = args.getParcelableArrayList(LOADER_ARG_RAW_MEMBER_LIST);
            return new CursorLoader(mContext, RawContacts.CONTENT_URI,
                    PROJECTION_RAW_CONTACT, String.format(SELECTION, selectionArgs), null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            buildRawMemberList(data, rawMemberList);
            getLoaderManager().destroyLoader(LOADER_MEMBER_RAW_DATA);
            startGroupAccountLoader(selectionArgs);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}

    };

    private void startGroupAccountLoader(String selectionArgs) {
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_MEMBER_IDS, selectionArgs);
        getLoaderManager().restartLoader(LOADER_GROUP_ACCOUNT_MEMBER_DATA, args,
                mGroupAccountLoaderListener);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupAccountLoaderListener =
        new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            String selectionArgs = args.getString(LOADER_ARG_MEMBER_IDS);
            return new LocalGroupAccountLoader(mContext, selectionArgs);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            bindGroupAccountWithMember(data);
            getLoaderManager().destroyLoader(LOADER_GROUP_ACCOUNT_MEMBER_DATA);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}

    };

    private void buildRawMemberList(Cursor cursor, ArrayList<RawMember> rawMemberList) {
        if (null != cursor) {
            cursor.moveToPosition(-1);

            while (cursor.moveToNext()) {
                long contactId = cursor.getLong(RAWCONTACT_CONTACT_ID_COLUMN_INDEX);
                long rawId = cursor.getLong(RAWCONTACT_RAW_CONTACT_ID_COLUMN_INDEX);
                String displayName = cursor.getString(RAWCONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
                String accountType = cursor.getString(RAWCONTACT_ACCOUNTTYPE_COLUMN_INDEX);
                String accountName = cursor.getString(RAWCONTACT_ACCOUNTNAME_COLUMN_INDEX);
                String dataSet = cursor.getString(RAWCONTACT_DATASET_COLUMN_INDEX);

                if (DEBUG) {
                    log("contactId = " + contactId + ", rawId = "
                            + rawId + ", displayName = " + displayName
                            + ", accountType = " + accountType + ", accountName = "
                            + accountName + ", dataSet = " + dataSet);
                }

                rawMemberList.add(new RawMember(contactId, rawId, displayName, accountType,
                        accountName, dataSet));
            }
        }
    }

    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
    /**
     * The loader listener for the list of existing group members.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupExistingMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return LocalGroupMemberLoader.constructLoaderForGroupEditorMemberQuery(mContext, mGroupTitle,
                    mListMembersContactIdToAdd, mListMembersContactIdToRemove);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<Long> listExistingMembers = new ArrayList<Long>();
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                long contactId = data.getLong(LocalGroupEditorMemberQuery.CONTACT_ID_COLUMN_INDEX);
                listExistingMembers.add(contactId);
            }

            data.moveToPosition(-1);

            // Update the display list
            addExistingMembers(listExistingMembers, data);

            // No more updates
            // TODO: move to a runnable
            //getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void queryGroupEditorMembers() {
        getLoaderManager().restartLoader(LOADER_EDITOR_MEMBERS, null,
                mGroupEditorMemberListLoaderListener);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupEditorMemberListLoaderListener =
        new LoaderCallbacks<Cursor>() {

            @Override
            public CursorLoader onCreateLoader(int id, Bundle args) {
                return LocalGroupMemberLoader.constructLoaderForGroupEditorMemberQuery(mContext, mGroupTitle,
                        mListMembersContactIdToAdd, mListMembersContactIdToRemove);

            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                data.moveToPosition(-1);
                // Update the display list
                mMemberListAdapter.changeCursor(data);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {}
    };
    // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

    /**
     * This represents a single raw member of the current group.
     */

    public static class RawMember implements Parcelable {
        private static final RawMember[] EMPTY_ARRAY = new RawMember[0];

        private final long mContactId;
        private final long mRawContactId;
        private final String mDisplayName;
        private final String mAccountType;
        private final String mAccountName;
        private final String mDataSet;

        public RawMember(long contactId, long rawContactId, String displayName, String accountType,
                String accountName, String dataSet) {
            mContactId = contactId;
            mRawContactId = rawContactId;
            mDisplayName = displayName;
            mAccountType = accountType;
            mAccountName = accountName;
            mDataSet = dataSet;

        }

        public long getRawContactId() {
            return mRawContactId;
        }

        public long getContactId() {
            return mContactId;
        }

        public String getDisplayName() {
            return mDisplayName;
        }


        public String getAccountType() {
            return mAccountType;
        }

        public String getAccountName() {
            return mAccountName;
        }

        public String getDataSet() {
            return mDataSet;
        }

        // Parcelable
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mContactId);
            dest.writeLong(mRawContactId);
            dest.writeString(mDisplayName);
            dest.writeString(mAccountType);
            dest.writeString(mAccountName);
            dest.writeString(mDataSet);
        }

        private RawMember(Parcel in) {
            mContactId = in.readLong();
            mRawContactId = in.readLong();
            mDisplayName = in.readString();
            mAccountType = in.readString();
            mAccountName = in.readString();
            mDataSet = in.readString();
        }

        public static final Parcelable.Creator<RawMember> CREATOR = new Parcelable.Creator<RawMember>() {
            public RawMember createFromParcel(Parcel in) {
                return new RawMember(in);
            }

            public RawMember[] newArray(int size) {
                return new RawMember[size];
            }
        };

    }

    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKPIM-1026
    /**
     * This adapter displays a list of members for the current group being edited.
     */
    private final class MemberListAdapter extends ResourceCursorAdapter {

        private boolean mIsGroupMembershipEditable = true;

        public MemberListAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final long contactId = cursor.getLong(LocalGroupEditorMemberQuery.CONTACT_ID_COLUMN_INDEX);
            String lookupKey = cursor.getString(
                    LocalGroupEditorMemberQuery.CONTACT_LOOKUP_KEY_COLUMN_INDEX);
            String displayName = cursor.getString(
                    LocalGroupEditorMemberQuery.CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
            String photoUriStr = cursor.getString(
                    LocalGroupEditorMemberQuery.CONTACT_PHOTO_URI_COLUMN_INDEX);

            QuickContactBadge badge = (QuickContactBadge) view.findViewById(R.id.badge);
            Uri lookupUri = Contacts.getLookupUri(contactId, lookupKey);
            badge.assignContactUri(lookupUri);

            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(displayName);

            View deleteButton = view.findViewById(R.id.delete_button_container);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeMember(contactId);
                    }
                });
            }

            // Moto Mod IKHSS6-4499, Config the dare theme by xml
            Uri photoUri = (photoUriStr != null) ? Uri.parse(photoUriStr) : null;
            mPhotoManager.loadPhoto(badge, photoUri, false, mContext.getResources().getBoolean(R.bool.contacts_dark_ui));

        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setIsGroupMembershipEditable(boolean editable) {
            mIsGroupMembershipEditable = editable;
        }
    }
    // MOTO MOD END, PIM-Contacts, gfj763, IKPIM-1026

    private void log(String logInfo) {
        if (DEBUG) {
            Log.d(TAG, logInfo);
        }
    }

    public class CreateContactsTask extends
          WeakAsyncTask<Integer, Void, Integer, LocalGroupEditorFragment> {
        private WeakReference<ProgressDialog> mProgress;
        private static final int RESULT_SUCCESS = 1;
        private static final int RESULT_FAILURE = 2;
        private Account mAccount = null;

        public CreateContactsTask(LocalGroupEditorFragment target) {
            super(target);
            // TODO Auto-generated constructor stub
        }
        public void setAccount(Account mAccount){
            this.mAccount = mAccount;
        }
        @Override
        protected Integer doInBackground(LocalGroupEditorFragment target,
          Integer ... params) {
           // TODO Auto-generated method stub
            Account account = this.mAccount;
            if(params[0] == 0){
                try{
                   target.mGroupInsertHelper.createNewContacts(account);
                }catch(Exception e){
                    Log.e(TAG, "occur excepition when adding Group:" + e.toString());
                    return RESULT_FAILURE;
                }
            } else {
              return RESULT_FAILURE;
            }
            getContactIdsFromDataIds(mDataIds);

            return RESULT_SUCCESS;
        }
        @Override
        protected void onPreExecute(LocalGroupEditorFragment target) {
            final Context context = mContext;

            mProgress = new WeakReference<ProgressDialog>(ProgressDialog.show(context, null,
                    getActivity().getText(R.string.toast_please_wait)));

            // Before starting this task, start an empty service to protect our
            // process from being reclaimed by the system.
            //context.startService(new Intent(context, EmptyService.class));
        }
        /** {@inheritDoc} */
        @Override
        protected void onPostExecute(LocalGroupEditorFragment target, Integer result) {

            String newAddMemberIdArgs = null;
            StringBuilder builder = new StringBuilder();
            for (int i=0;i<mContactIds.size();i++){
               builder.append(mContactIds.get(i)).append(",");
            }
            if(isEmpty(builder.toString())) {
                newAddMemberIdArgs = EMPTY_STRING;
            } else {
                newAddMemberIdArgs = builder.substring(0, builder.length() - 1);
            }
            Log.i(TAG, "gfd367 contact ids are: "+newAddMemberIdArgs);
                if (!TextUtils.isEmpty(newAddMemberIdArgs)) {
                    startMemberRawDataLoader(newAddMemberIdArgs, mListRawMembersToAdd);
                }

            final Context context = mContext;
            final ProgressDialog progress = mProgress.get();
            dismissDialog(progress);

            // Stop the service that was protecting us
            //context.stopService(new Intent(context, EmptyService.class));

            if (result == RESULT_FAILURE) {
                Log.e(TAG, "Create group failed, Some exception happens while adding a new group");
                //finish();
            }
        }
          void dismissDialog(Dialog dialog) {
            try {
                // Only dismiss when valid reference and still showing
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                Log.w(TAG, "Ignoring exception while dismissing dialog: " + e.toString());
            }
        }
    }

    //===== Group Insert Helper Handler =====
    private class GroupInsertHelper extends Handler {

        public static final int MESSAGE_INSERT_UNKNOWN = 100;

        public static final int SHOW_NO_CONTACTS_CREATED_DIALOG = 0;
        public static final int SHOW_GROUP_SAVED_DIALOG = 1;

        public GroupInsertHelper(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int messageID = 0;
            if((mCreateContactsTask.getStatus().compareTo(AsyncTask.Status.PENDING)) != 0){
               //0 if the ordinal values of this enum constant and o are equal
                mCreateContactsTask = new CreateContactsTask(LocalGroupEditorFragment.this);
            }
            if (msg.what == MESSAGE_INSERT_UNKNOWN) {
              mCreateContactsTask.setAccount((Account)msg.obj);
            }
            mCreateContactsTask.execute(Integer.valueOf(messageID));
        }
        private void createNewContacts(Account account) {
            long dataId;
            ArrayList<Long> dataIds = new ArrayList<Long>();

            // Creating contacts by phone numbers
            if (mPhone2name != null && !mPhone2name.isEmpty()) {
                for (String phoneNum : mPhone2name.keySet()) {
                    dataId = ContentUris.parseId(createSingleContact(account, phoneNum, null));
                    //dataIds.add(dataId);
                    mDataIds.add(dataId);
                }
            }

            // Creating contacts by emails
            if (mEmail2name != null && !mEmail2name.isEmpty()) {
                for (String email : mEmail2name.keySet()) {
                    dataId = ContentUris.parseId(createSingleContact(account, null, email));
                    //dataIds.add(dataId);
                    mDataIds.add(dataId);
                }
            }

        }

        /**
         * @return Data Uri of the new created contact.
         */
        private Uri createSingleContact(Account account, String phoneNum, String email) {
            mOperationList.clear();
            int dataIndex = 0;
            int phoneNumIndex = -1;
            int emailIndex = -1;
            // After applying the batch the first result's Uri is returned so it is important that
            // the RawContact is the first operation that gets inserted into the list
            ContentProviderOperation.Builder builder =
                    ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
            if (account != null) {
                builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
                builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
            } else {
                builder.withValue(RawContacts.ACCOUNT_NAME, null);
                builder.withValue(RawContacts.ACCOUNT_TYPE, null);
            }
            mOperationList.add(builder.build());

            // Add display name
            String displayName = null;
            if (phoneNum != null) {
                displayName = (String)mPhone2name.get(phoneNum);
            } else if (email != null) {
                displayName = (String)mEmail2name.get(email);
            } else {
                Log.w(TAG, "Phone number and Email both are null...");
                return null;
            }
            if (!isEmpty(displayName)) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);

                builder.withValue(StructuredName.DISPLAY_NAME, displayName);
                mOperationList.add(builder.build());
                dataIndex += 1;
            }

            // Add phone number
            if (phoneNum != null) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.TYPE, CommonDataKinds.Phone.TYPE_WORK);
                builder.withValue(Phone.NUMBER, phoneNum);
                mOperationList.add(builder.build());
                phoneNumIndex = dataIndex += 1;
            }

            // Add email
            if (email != null) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                builder.withValue(Email.TYPE, CommonDataKinds.Email.TYPE_WORK);
                builder.withValue(Email.DATA, email);
                mOperationList.add(builder.build());
                emailIndex = dataIndex += 1;
            }


            try {
                ContentProviderResult[] results = mContext.getContentResolver().
                            applyBatch(ContactsContract.AUTHORITY, mOperationList);
                // the second to last result is always the phone number data row. return it's uri so
                // that it can be found later. do null checking for badly behaving
                // ContentResolvers
                return (results == null || results.length == 0 || results[0] == null)
                        ? null
                        : results[phoneNumIndex!=-1 ? phoneNumIndex : emailIndex].uri;
            } catch (RemoteException e) {
                 Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                 return null;
            } catch (OperationApplicationException e) {
                 Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                 return null;
            }
        }

    }
    //================================================================
    //================================================================

    //================================================================
    //================================================================
    //This task is to check whether the members are already in contacts db or not.
    //If yes, put the dataIds into mDataIds
    //If not, put the email address or phone numbers into mEmail2name
    class CheckMemberTask extends WeakAsyncTask<Void, Void, Void,LocalGroupEditorFragment>
    {
        final String[] CHECK_MEMBER_EMAIL_PROJECTION = {
                 ContactsContract.Data._ID, CommonDataKinds.Email.DATA
        };
        final String[] CHECK_MEMBER_NUMBER_PROJECTION = {
                 ContactsContract.Data._ID, CommonDataKinds.Email.DATA
        };


        ProgressDialog mAsyncTaskTransientDialog = null;

        //Bundles that are going to be sent to Contacts App
        Bundle unmatchedEmailBundle = new Bundle();
        long [] matchedEmailsArray;
        Context mContext;
        ArrayList<String> mGroupAddress;
        String mGroupName;
        String mNewAddMemberIdArgs;

        public CheckMemberTask(Context context, String groupName,LocalGroupEditorFragment target ) {
            super(target);
            mContext = context;
            mGroupAddress = null;
            mGroupName = groupName;
            if( mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ) {
                ArrayList<String> groupAddress = new ArrayList<String>();
                for (String tmp:mEmailAddresses.split("\\;")){
                    groupAddress.add(tmp);
                }
                mGroupAddress = groupAddress;
            }
        }

        protected void onPreExecute(LocalGroupEditorFragment target) {

            // Start a progress dialog.
            mAsyncTaskTransientDialog = new ProgressDialog(mContext);
            if (mAsyncTaskTransientDialog != null) {
                mAsyncTaskTransientDialog.setMessage(getActivity().getText(R.string.toast_please_wait));
                mAsyncTaskTransientDialog.show();
            }
        }

        protected Void doInBackground(LocalGroupEditorFragment target,Void... params) {
            if( mAction.equals(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP) ) {
                String addressDataString = getAddressSQLDataString(mGroupAddress);
                if(isEmpty(addressDataString)){
                    return null;
                }

                // Prepare bundle for unmatched addresses. For now, all addresses are considered unmatched.
                for(String address : mGroupAddress){
                    unmatchedEmailBundle.putString(address,"");
                    Log.d(TAG, "Attendees' Email: " + address);
                }

                if (isCancelled()) {
                    return null;
                }

                Cursor cursor = queryContactsByEmailAddress(addressDataString);

                if (isCancelled()) {
                    if (cursor != null){
                        cursor.close();
                    }
                    return null;
                }
                // Prepare bundle for matched addresses. We use the cursor to do that.
                mDataIds.clear();
                if (cursor != null){
                    if(cursor.moveToFirst()){
                        do {
                            mDataIds.add(cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID)));
                            unmatchedEmailBundle.remove(cursor.getString(cursor.getColumnIndex(CommonDataKinds.Email.DATA)));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }
                mEmail2name = unmatchedEmailBundle;
            }

            if( mEmail2name.isEmpty() && mPhone2name.isEmpty() ) {
                getContactIdsFromDataIds(mDataIds);
                //all members are in contacts db, need to get the members' contact ids
                mNewAddMemberIdArgs = null;
                StringBuilder builder = new StringBuilder();
                for (int i=0;i<mContactIds.size();i++){
                    builder.append(mContactIds.get(i)).append(",");
                }
                if(isEmpty(builder.toString())) {
                    mNewAddMemberIdArgs = EMPTY_STRING;
                } else {
                    mNewAddMemberIdArgs = builder.substring(0, builder.length() - 1);
                }
                Log.i(TAG, "gfd367 contact ids are: "+mNewAddMemberIdArgs);
            }
            return null;
        }

        protected void onPostExecute(LocalGroupEditorFragment target,Void result)
        {
            // Dismiss the progress dialog.
            if (mAsyncTaskTransientDialog != null && mAsyncTaskTransientDialog.isShowing()){
                mAsyncTaskTransientDialog.dismiss();
                mAsyncTaskTransientDialog = null;
            }

            if (mGroupInsertHelper==null) {
                mGroupInsertHelper = new GroupInsertHelper(MyLooper.singleton());
            }
            if(mEmail2name.isEmpty() && mPhone2name.isEmpty()) {
                if (!TextUtils.isEmpty(mNewAddMemberIdArgs)) {
                    startMemberRawDataLoader(mNewAddMemberIdArgs, mListRawMembersToAdd);
                }
            } else {
                //some member is not in contact db, need to save them to contact db first
                selectAccount();
            }
        }

        protected void onCancelled() {
            // Dismiss the progress dialog.
            if (mAsyncTaskTransientDialog != null && mAsyncTaskTransientDialog.isShowing()) {
                mAsyncTaskTransientDialog.dismiss();
                mAsyncTaskTransientDialog = null;
                Log.d(TAG,"SaveGroup task onCancelled");
            }
        }


        private String getAddressSQLDataString(ArrayList<String> addresses)
        {
            StringBuilder builder = new StringBuilder();
            for (int i=0;i<addresses.size();i++){
               builder.append(escapeSQLString(addresses.get(i))).append(",");
            }
            if(isEmpty(builder.toString()))
                return EMPTY_STRING;

            return "(" + builder.substring(0, builder.length() - 1) + ")";
        }

        /*
         * Gets the contacts that are associated with the list of emails from all fields
         *  of the current message.
         */
        private Cursor queryContactsByEmailAddress(String addressDataString)
        {
            if ( isEmpty(addressDataString) )
                return null;

            return mContext.getContentResolver().query(
                    CommonDataKinds.Email.CONTENT_URI, CHECK_MEMBER_EMAIL_PROJECTION,
                    CommonDataKinds.Email.DATA + " in " + addressDataString, null, null);
        }

        /*
         * Gets the contacts that are associated with the list of phone numbers from all fields
         *  of the current message.
         */
        private Cursor queryContactsByNumberAddress(String addressDataString)
        {
            if ( isEmpty(addressDataString) )
                return null;

            return mContext.getContentResolver().query(
                    CommonDataKinds.Phone.CONTENT_URI, CHECK_MEMBER_NUMBER_PROJECTION,
                    CommonDataKinds.Phone.DATA + " in " + addressDataString, null, null);
        }
    }


    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0 || str.toString().trim().length() == 0;
    }

    public static String removeSpaces(String str)
    {
        if(str.length() == 0)
            return "";

        StringTokenizer st = new StringTokenizer(str);
        StringBuilder strBuffer = new StringBuilder();
        while (st.hasMoreTokens())
        {
            strBuffer.append(st.nextToken());
            strBuffer.append(" ");
        }

        return strBuffer.toString().trim();
    }

    public static boolean hasOnlyPhoneNumberDigits(String phone) {
        if (phone != null) {
            Pattern pattern = Pattern.compile("^[\\+0-9\\- ]*");
            Matcher matcher = pattern.matcher(phone);
            return matcher.matches();
        }
        return false;
    }

    public static boolean isPhoneNumber(String phone) {
        final int MIN_PHONE_LENGTH = 7;
        final int MAX_PHONE_LENGTH = 50;

        if ((phone != null) &&
            ((phone.length() > MIN_PHONE_LENGTH) &&
             (phone.length() < MAX_PHONE_LENGTH))) {
            Pattern pattern = Pattern.compile("^[\\+0-9\\- ]*");
            Matcher matcher = pattern.matcher(phone);
            return matcher.matches();
        }
        return false;
    }

    public static boolean isEmailAddress(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static String escapeSQLString(String str) {
        if (isEmpty(str))
            return EMPTY_STRING;

        return DatabaseUtils.sqlEscapeString(str);
    }


    private boolean isAnyExistingContacts() {
        return mDataIds != null && mDataIds.size() > 0;
    }

    private boolean isAnyUnknownContacts() {
        return mPhone2name != null && mPhone2name.size() > 0 || mEmail2name != null && mEmail2name.size() > 0;
    }

    /**
     * Builds a value-list, "(n,n,...)" string to be use in an IN expression of
     * a SQL SELECT statement. If an element is a number. it is used as-is,
     * other it is quoted using SQL single quotes.
     *
     * @param values The values to include in the value-list.
     *
     * @return The value list (...)" expression as a String. Returns "()" if the array is empty.
     */
    public static final String buildValueList(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "()";
        }

        StringBuilder expression = new StringBuilder(80);
        expression.append('(');
        for (Object value : values) {
            if (expression.length() > 1) {
                expression.append(',');
            }
            expression.append(value);
        }
        expression.append(')');
        return expression.toString();
    }


    /**
     *
     * @param dataId -1 indicating no dataId.
     * @param rawContactId -1 indicating no rawContactId.
     */
    private void assignMembersToGroup(List<Long> dataIds) {
        //HashMap<Long, Long> contactId2dataIdMap = new LinkedHashMap<Long, Long>();

        String selection = new StringBuilder()
            .append(Data._ID)
            .append(" IN ")
            .append(buildValueList(dataIds))
            .toString();
        Cursor dataCursor = mContext.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] {Data._ID, Data.CONTACT_ID},
                selection, null, null);
        try {
            while ( dataCursor != null && dataCursor.moveToNext() ) {
             //   contactId2dataIdMap.put(dataCursor.getLong(dataCursor.getColumnIndex(Data.CONTACT_ID)),
              //          dataCursor.getLong(dataCursor.getColumnIndex(Data._ID)));
            }
        } finally {
            if ( dataCursor != null ) {
                dataCursor.close();
            }
        }

    }

    /**
     *
     * @param dataId -1 indicating no dataId.
     * @param rawContactId -1 indicating no rawContactId.
     */
    private void getContactIdsFromDataIds(List<Long> dataIds) {
        String selection = new StringBuilder()
            .append(Data._ID)
            .append(" IN ")
            .append(buildValueList(dataIds))
            .toString();
        Cursor dataCursor = mContext.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] {Data._ID, Data.CONTACT_ID},
                selection, null, null);
        try {
            while ( dataCursor != null && dataCursor.moveToNext() ) {
                mContactIds.add(dataCursor.getLong(dataCursor.getColumnIndex(Data.CONTACT_ID)));
              //          dataCursor.getLong(dataCursor.getColumnIndex(Data._ID)));
            }
        } finally {
            if ( dataCursor != null ) {
                dataCursor.close();
            }
        }
    }

    //MOT MOD BEGIN - IKHSS7-1780: strip blank character appear at head and tail position
    /*
     * Get group name, and strip blank appear at head and tail from title view.
     */
    private String getGroupTitle() {
        if (null == mGroupNameView) {
            return null;
        } else {
            return mGroupNameView.getText().toString().trim();
        }
    }
    //MOT MOD END - IKHSS7-1780

    public static class ProgressDialogFragment extends DialogFragment {

        public static ProgressDialogFragment newInstance(String title) {
            ProgressDialogFragment frag = new ProgressDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            frag.setCancelable(false);
            return frag;
        }


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            final String description = getActivity().getString(
                    R.string.moto_group_add_members_saving_message, title);
            final ProgressDialog dialog = new ProgressDialog(getActivity());
            //
            dialog.setTitle(R.string.moto_group_add_members_saving_title);
            dialog.setMessage(description);

            dialog.setIndeterminate(true);
            dialog.setCancelable(false);

            return dialog;
        }
    }

}
