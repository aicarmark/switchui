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

import java.util.ArrayList;
import java.util.List;


import com.android.contacts.ContactPhotoManager;
import com.android.contacts.group.GroupDetailDisplayUtils;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.list.ContactTileAdapter;
import com.android.contacts.list.ContactTileAdapter.DisplayType;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;

import com.motorola.contacts.group.LocalGroupDeletionDialogFragment;
import com.motorola.contacts.group.LocalGroupMemberLoader;
import com.motorola.contacts.group.LocalGroupMetaDataLoader;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.RomUtility;
import android.widget.Toast;
/**
 * Displays the details of a group and shows a list of actions possible for the group.
 */
public class LocalGroupDetailFragment extends Fragment implements OnScrollListener {

    public static interface Listener {
        /**
         * The group title has been loaded
         */
        public void onGroupTitleUpdated(String title);

        /**
         * The number of group members has been determined
         */
        public void onGroupSizeUpdated(String size);

        /**
         * The account type and dataset have been determined.
         */
        public void onAccountTypeUpdated(String accountTypeString, String dataSet);

        /**
         * User decided to go to Edit-Mode
         */
        public void onEditRequested(String groupTitle);

        /**
         * User decided to go to Sms-Mode
         */
        public void onSmsRequested(String groupTitle);

        /**
         * User decided to go to Email-Mode
         */
        public void onEmailRequested(String groupTitle);

        /**
         * User decided to go to Event-Mode
         */
        public void onEventRequested(String groupTitle);

        /**
         * Contact is selected and should launch details page
         */
        public void onContactSelected(Uri contactUri);

        public void onOptionMenuUpdated();
    }

    private static final String TAG = "LocalGroupDetailFragment";

    private static final int LOADER_METADATA = 0;
    private static final int LOADER_MEMBERS = 1;

    private static final int LOADER_LOCAL_GROUP_METADATA = 2;
    private static final int LOADER_LOCAL_GROUP_MEMBER_ID = 3;

    private Context mContext;

    private View mRootView;
    private ViewGroup mGroupSourceViewContainer;
    private View mGroupSourceView;
    private TextView mGroupTitle;
    private TextView mGroupSize;
    private ListView mMemberListView;
    private View mEmptyView;

    private Listener mListener;

    private ContactTileAdapter mAdapter;
    private ContactPhotoManager mPhotoManager;
    private AccountTypeManager mAccountTypeManager;

    private long mGroupId;
    private String mGroupName;
    private String mAccountTypeString;
    private String mDataSet;
    private boolean mIsReadOnly;

    private int mGroupMemberSize;

    private boolean mShowGroupActionInActionBar;
    private boolean mOptionsMenuGroupDeletable;
    private boolean mOptionsMenuGroupPresent;
    private boolean mOptionsMenuGroupHasMember;
    private boolean mCloseActivityAfterDelete;

    public LocalGroupDetailFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);

        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);

        mAdapter = new ContactTileAdapter(activity, mContactTileListener, columnCount,
                DisplayType.GROUP_MEMBERS);

        configurePhotoLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mRootView = inflater.inflate(R.layout.group_detail_fragment, container, false);
        mGroupTitle = (TextView) mRootView.findViewById(R.id.group_title);
        mGroupSize = (TextView) mRootView.findViewById(R.id.group_size);
        mGroupSourceViewContainer = (ViewGroup) mRootView.findViewById(
                R.id.group_source_view_container);
        mEmptyView = mRootView.findViewById(android.R.id.empty);
        mMemberListView = (ListView) mRootView.findViewById(android.R.id.list);
        mMemberListView.setAdapter(mAdapter);

        return mRootView;
    }

    public void loadGroup(String groupTitle) {
        mGroupName = groupTitle;
        startLocalGroupMetadataLoader();
    }

    public void setQuickContact(boolean enableQuickContact) {
        mAdapter.enableQuickContact(enableQuickContact);
    }

    private void configurePhotoLoader() {
        if (mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mMemberListView != null) {
                mMemberListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void setShowGroupSourceInActionBar(boolean show) {
        mShowGroupActionInActionBar = show;
    }

    /**
     * Start the loader to retrieve the metadata for this group.
     */
    private void startLocalGroupMetadataLoader() {
        getLoaderManager().restartLoader(LOADER_LOCAL_GROUP_METADATA, null, mLocalGroupMetadataLoaderListener);
    }

    /**
     * Start the loader to retrieve the list of group members.
     */
    private void startLocalGroupMemberLoader() {
        getLoaderManager().restartLoader(LOADER_LOCAL_GROUP_MEMBER_ID, null, mLocalGroupMemberLoaderListener);
    }

    private final ContactTileAdapter.Listener mContactTileListener =
            new ContactTileAdapter.Listener() {

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            mListener.onContactSelected(contactUri);
        }
    };

    /**
     * The listener for the group metadata loader.
     */

    private final LoaderManager.LoaderCallbacks<Cursor> mLocalGroupMetadataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new LocalGroupMetaDataLoader(mContext, mGroupName);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (null != data && data.getCount() > 0) {
                bindGroupMetaData(data);
                // Retrieve the list of members
                startLocalGroupMemberLoader();
                return;
            }
            // No this title group, will show nothing.
            updateSize(-1);
            updateTitle(null);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the aggregation group member contact Id loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mLocalGroupMemberLoaderListener =
        new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return LocalGroupMemberLoader.constructLoaderForGroupDetailMemberQuery(mContext, mGroupName);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if ( (data != null) && data.getCount() >= 0) {
                updateSize(data.getCount());
            } else {
                updateSize(-1);
                updateTitle(null);
            }
            mAdapter.setContactCursor(data);
            mMemberListView.setEmptyView(mEmptyView);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void bindGroupMetaData(Cursor cursor) {
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            mIsReadOnly |= (cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1);
            if (mIsReadOnly) {
                break;
            }
        }

        updateTitle(mGroupName);
        // Must call invalidate so that the option menu will get updated
        getActivity().invalidateOptionsMenu();
    }

    private void updateTitle(String title) {
        if (mGroupTitle != null) {
            mGroupTitle.setText(title);
        } else {
            mListener.onGroupTitleUpdated(title);
        }
    }

    /**
     * Display the count of the number of group members.
     * @param size of the group (can be -1 if no size could be determined)
     */
    private void updateSize(int size) {
        mGroupMemberSize = size;
        if(isOptionsMenuChanged()) {
            mListener.onOptionMenuUpdated();
        }

        String memberCountString;
        if (size == -1) {
            memberCountString = null;
        } else {
            memberCountString = mContext.getResources().getQuantityString(
                    R.plurals.group_list_num_contacts_in_group, size, size);
        }

        if (mGroupSize != null) {
            mGroupSize.setText(memberCountString);
        } else {
            mListener.onGroupSizeUpdated(memberCountString);
        }
    }

    /**
     * Once the account type, group source action, and group source URI have been determined
     * (based on the result from the {@link Loader}), then we can display this to the user in 1 of
     * 2 ways depending on screen size and orientation: either as a button in the action bar or as
     * a button in a static header on the page.
     */
    private void updateAccountType(final String accountTypeString, final String dataSet) {

        // If the group action should be shown in the action bar, then pass the data to the
        // listener who will take care of setting up the view and click listener. There is nothing
        // else to be done by this {@link Fragment}.
        if (mShowGroupActionInActionBar) {
            mListener.onAccountTypeUpdated(accountTypeString, dataSet);
            return;
        }

        final AccountTypeManager manager = AccountTypeManager.getInstance(getActivity());
        final AccountType accountType =
                manager.getAccountType(accountTypeString, dataSet);

        // Otherwise, if the {@link Fragment} needs to create and setup the button, then first
        // verify that there is a valid action.
        if (!TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            if (mGroupSourceView == null) {
                mGroupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(mContext);
                // Figure out how to add the view to the fragment.
                // If there is a static header with a container for the group source view, insert
                // the view there.
                if (mGroupSourceViewContainer != null) {
                    mGroupSourceViewContainer.addView(mGroupSourceView);
                }
            }

            // Rebind the data since this action can change if the loader returns updated data
            mGroupSourceView.setVisibility(View.VISIBLE);
            GroupDetailDisplayUtils.bindGroupSourceView(mContext, mGroupSourceView,
                    accountTypeString, dataSet);
            mGroupSourceView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, mGroupId);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setClassName(accountType.resPackageName,
                            accountType.getViewGroupActivity());
                    startActivity(intent);
                }
            });
        } else if (mGroupSourceView != null) {
            mGroupSourceView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.view_group, menu);
    }

    public boolean isOptionsMenuChanged() {
        return (mOptionsMenuGroupDeletable != isGroupDeletable() &&
                mOptionsMenuGroupPresent != isGroupPresent()) ||
                mOptionsMenuGroupHasMember != isGroupMemberNotZero();
    }

    public boolean isGroupDeletable() {
        /*
         Need to judge all same title groups are ReadOnly.
         if any one is read-only, the agg group is read-only.
        */
        //return mGroupUri != null && !mIsReadOnly;
        return !TextUtils.isEmpty(mGroupName) && !mIsReadOnly;
    }

    public boolean isGroupPresent() {
        //return mGroupUri != null;
        return !TextUtils.isEmpty(mGroupName);
    }

    public boolean isGroupMemberNotZero() {
        return mGroupMemberSize>0;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible();
        mOptionsMenuGroupPresent = isGroupPresent() && isVisible();
        mOptionsMenuGroupHasMember = isGroupMemberNotZero();

        final MenuItem editMenu = menu.findItem(R.id.menu_edit_group);
        editMenu.setVisible(mOptionsMenuGroupPresent);

        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete_group);
        deleteMenu.setVisible(mOptionsMenuGroupDeletable);

        //MOT MOD BEGIN - IKPIM-991: Group redesign feature on ics
        final MenuItem smsMenu = menu.findItem(R.id.menu_send_sms);
        if(smsMenu != null) {
            smsMenu.setVisible(mOptionsMenuGroupHasMember);
        }

        final MenuItem emailMenu = menu.findItem(R.id.menu_send_email);
        if(emailMenu != null) {
            emailMenu.setVisible(mOptionsMenuGroupHasMember);
        }

        final MenuItem eventMenu = menu.findItem(R.id.menu_create_event);
        if(eventMenu != null) {
            eventMenu.setVisible(mOptionsMenuGroupHasMember);
        }
        //MOT MOD END - IKPIM-991
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(RomUtility.isOutofMemory()){
            Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
            return true;
            }

        switch (item.getItemId()) {
            case R.id.menu_edit_group: {
                if (mListener != null) mListener.onEditRequested(mGroupName);
                break;
            }
            case R.id.menu_delete_group: {
                LocalGroupDeletionDialogFragment.show(getFragmentManager(), mGroupName,
                        mCloseActivityAfterDelete);
                return true;
            }
            case R.id.menu_send_sms: {
                if (mListener != null) mListener.onSmsRequested(mGroupName);
                break;
            }
            case R.id.menu_send_email: {
                if (mListener != null) mListener.onEmailRequested(mGroupName);
                break;
            }
            case R.id.menu_create_event: {
                if (mListener != null) mListener.onEventRequested(mGroupName);
                break;
            }
        }
        return false;
    }

    public void closeActivityAfterDelete(boolean closeActivity) {
        mCloseActivityAfterDelete = closeActivity;
    }

    public long getGroupId() {
        return mGroupId;
    }
    //MOTO MOD BEGIN
    public void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_LOCAL_GROUP_METADATA);
        getLoaderManager().destroyLoader(LOADER_LOCAL_GROUP_MEMBER_ID);
        Cursor c = mAdapter.getContactCursor();
        if(c != null && !c.isClosed()){
             c.close();
         }
        super.onDestroy();
    }
    //MOTO MOD END
}
