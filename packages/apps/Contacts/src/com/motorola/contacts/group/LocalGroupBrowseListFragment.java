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
 * limitations under the License.
 */

package com.motorola.contacts.group;

import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.motorola.contacts.group.LocalGroupBrowseListAdapter;
import com.motorola.contacts.group.LocalGroupBrowseListAdapter.LocalGroupListItemViewCache;
import com.android.contacts.widget.AutoScrollListView;

import com.motorola.contacts.group.LocalGroupListLoader;
import com.motorola.contacts.group.LocalGroupUtils;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment to display the list of groups.
 */
public class LocalGroupBrowseListFragment extends Fragment
        implements OnFocusChangeListener, OnTouchListener {

    /**
     * Action callbacks that can be sent by a group list.
     */
    public interface OnGroupBrowserActionListener  {

        /**
         * Opens the specified group for viewing.
         *
         * @param groupUri for the group that the user wishes to view.
         */
        void onViewGroupAction(String groupTitle);

    }

    private static final String TAG = "LocalGroupBrowseListFragment";

    private static final int LOADER_GROUPS = 1;

    private Context mContext;
    private Cursor mGroupListCursor;

    private boolean mSelectionToScreenRequested;

    private static final String EXTRA_KEY_GROUP_TITLE = "groups.groupTitle";
//MOTO MOD BEGIN IKPIM-1044 support group list search
    private static final String EXTRA_KEY_GROUP_QUERY_STRING = "groupQueryString";
//MOTO MOD END IKPIM-1044 support group list search

    private View mRootView;
    private AutoScrollListView mListView;
    private TextView mEmptyView;
    //private View mAddAccountsView;
    //private View mAddAccountButton;

    private LocalGroupBrowseListAdapter mAdapter;
    private boolean mSelectionVisible;
    private String mSelectedGroupTitle;

    private int mVerticalScrollbarPosition = View.SCROLLBAR_POSITION_RIGHT;

    private OnGroupBrowserActionListener mListener;

//MOTO MOD BEGIN IKPIM-1044 support group list search
    private String mQueryString;//used to filter groups
//MOTO MOD END IKPIM-1044 support group list search

    public LocalGroupBrowseListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedGroupTitle = savedInstanceState.getString(EXTRA_KEY_GROUP_TITLE);
            if (!TextUtils.isEmpty(mSelectedGroupTitle)) {
                // The selection may be out of screen, if rotated from portrait to landscape,
                // so ensure it's visible.
                mSelectionToScreenRequested = true;
            }
        }

        mRootView = inflater.inflate(R.layout.local_group_browse_list_fragment, null);
        mEmptyView = (TextView)mRootView.findViewById(R.id.empty);

        mAdapter = new LocalGroupBrowseListAdapter(mContext);
        mAdapter.setSelectionVisible(mSelectionVisible);
        mAdapter.setSelectedGroup(mSelectedGroupTitle);

        mListView = (AutoScrollListView) mRootView.findViewById(R.id.list);
        mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocalGroupListItemViewCache groupListItem = (LocalGroupListItemViewCache) view.getTag();
                if (groupListItem != null) {
                    LocalGroupUtils.log("LocalGroupBrowseListFragment::onItemClick(), start to viewGroupByTitle(), groupTitle: " + groupListItem.getGroupTitle());
                    viewGroupByTitle(groupListItem.getGroupTitle());
                }
            }
        });

        mListView.setEmptyView(mEmptyView);
        /*
        mAddAccountsView = mRootView.findViewById(R.id.add_accounts);
        mAddAccountButton = mRootView.findViewById(R.id.add_account_button);
        mAddAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Settings.EXTRA_AUTHORITIES,
                        new String[] { ContactsContract.AUTHORITY });
                startActivity(intent);
            }
        });
        setAddAccountsVisibility(!ContactsUtils.areGroupWritableAccountsAvailable(mContext));
        */
        return mRootView;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (mVerticalScrollbarPosition != position) {
            mVerticalScrollbarPosition = position;
            configureVerticalScrollbar();
        }
    }

    private void configureVerticalScrollbar() {
        mListView.setVerticalScrollbarPosition(mVerticalScrollbarPosition);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        int leftPadding = 0;
        int rightPadding = 0;
        if (mVerticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT) {
            leftPadding = mContext.getResources().getDimensionPixelOffset(
                    R.dimen.list_visible_scrollbar_padding);
        } else {
            rightPadding = mContext.getResources().getDimensionPixelOffset(
                    R.dimen.list_visible_scrollbar_padding);
        }
        mListView.setPadding(leftPadding, mListView.getPaddingTop(),
                rightPadding, mListView.getPaddingBottom());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onStart() {
        getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        super.onStart();
    }

    /**
     * The listener for the group meta data loader for all groups.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            mEmptyView.setText(null);
//MOTO MOD BEGIN IKPIM-1044 support group list search
            String queryString = args != null && args.containsKey(EXTRA_KEY_GROUP_QUERY_STRING)
                    ? args.getString(EXTRA_KEY_GROUP_QUERY_STRING)
                    : null;
//MOTO MOD END IKPIM-1044 support group list search
            return new LocalGroupListLoader(mContext, queryString);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mGroupListCursor = data;
            bindGroupList();
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void bindGroupList() {
        mEmptyView.setText(R.string.noGroups);
        //setAddAccountsVisibility(!ContactsUtils.areGroupWritableAccountsAvailable(mContext));
        if (mGroupListCursor == null) {
            return;
        }
        mAdapter.setCursor(mGroupListCursor);

        if (mSelectionToScreenRequested) {
            mSelectionToScreenRequested = false;
            requestSelectionToScreen();
        }

        mSelectedGroupTitle = mAdapter.getSelectedGroup();

        // For tablet, if there is no group at all, need to update groupDetailFragments optionMenu,
        // dismiss edit & delete menu.
        if (mGroupListCursor.getCount() == 0 || mSelectedGroupTitle == null) {//add mSelectedGroupTitle == null check for IKHSS6UPGR-6673
            mSelectedGroupTitle = "";
        }
//MOTO MOD BEGIN IKPIM-1044 support group list search
         else{//check whether selected group is in the query results, otherwise select the 1st record
            mSelectedGroupTitle = getSelectedGroupOrFirstFoundGroup();
        }
//MOTO MOD END IKPIM-1044 support group list search

        if (mSelectionVisible && !TextUtils.isEmpty(mSelectedGroupTitle)) {
            LocalGroupUtils.log(
                    "LocalGroupBrowseListFragment::bindGroupList(), start to viewGroupByTitle(), groupTitle: "
                    + mSelectedGroupTitle);
            viewGroupByTitle(mSelectedGroupTitle);
        } else if(mSelectionVisible) {
            // For tablet if there is no group at all, need to update groupDetailFragments optionMenu,
            // dismiss edit & delete menu.
            if (mListener != null) mListener.onViewGroupAction(mSelectedGroupTitle);
        }
    }

    public void setListener(OnGroupBrowserActionListener listener) {
        mListener = listener;
    }

    public void setSelectionVisible(boolean flag) {
        mSelectionVisible = flag;
        if (mAdapter != null) {
            mAdapter.setSelectionVisible(mSelectionVisible);
        }
    }

    private void setSelectedGroup(String groupTitle) {
        mSelectedGroupTitle = groupTitle;
        mAdapter.setSelectedGroup(mSelectedGroupTitle);
        mListView.invalidateViews();
    }

    private void viewGroupByTitle(String groupTitle) {
        setSelectedGroup(groupTitle);
        LocalGroupUtils.log("LocalGroupBrowseListFragment::viewGroupByTitle(), groupTitle: "
                + groupTitle);
        if (mListener != null) mListener.onViewGroupAction(groupTitle);
    }

    public void setSelectedGroupTitle(String groupTitle) {
        LocalGroupUtils.log("LocalGroupBrowseListFragment::setSelectedGroupTitle(), start to viewGroupByTitle(), groupTitle: "
                + groupTitle);
        viewGroupByTitle(groupTitle);
        mSelectionToScreenRequested = true;
    }

    protected void requestSelectionToScreen() {
        if (!mSelectionVisible) {
            return; // If selection isn't visible we don't care.
        }
        int selectedPosition = mAdapter.getSelectedGroupPosition();
        if (selectedPosition != -1) {
            mListView.requestPositionToScreen(selectedPosition,
                    true /* smooth scroll requested */);
        }
    }

    private void hideSoftKeyboard() {
        if (mContext == null) {
            return;
        }
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    /**
     * Dismisses the soft keyboard when the list takes focus.
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

    /**
     * Dismisses the soft keyboard when the list is touched.
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mListView) {
            hideSoftKeyboard();
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_KEY_GROUP_TITLE, mSelectedGroupTitle);
    }

    /*
    public void setAddAccountsVisibility(boolean visible) {
        if (mAddAccountsView != null) {
            mAddAccountsView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    */

//MOTO MOD BEGIN IKPIM-1044 support group list search
    /**
     * Used to search the groups when quering
     */
    public void setQueryString(String queryString, boolean delaySelection) {
        // Normalize the empty query.
        if (TextUtils.isEmpty(queryString)) queryString = null;

        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;

            Bundle args;
            if(mQueryString == null){
                args = null;
            }else{
                args = new Bundle();
                args.putString(EXTRA_KEY_GROUP_QUERY_STRING, mQueryString.trim());
            }
            //restart the loader to filter groups
            getLoaderManager().restartLoader(LOADER_GROUPS, args, mGroupLoaderListener);
        }
    }

    /**
     * return selected group if it is in the new query results, otherwise get first found group in the cursor
     */
    private String getSelectedGroupOrFirstFoundGroup() {
        if (TextUtils.isEmpty(mSelectedGroupTitle) || mAdapter == null || mAdapter.getCount() == 0) {
            return "";
        }

        int index = 0;
        while (index < mAdapter.getCount()) {
            //LocalGroupListItem entry = mAdapter.getItem(index);
            if (mSelectedGroupTitle.equals(mAdapter.getItem(index).getTitle())) {
                  return mSelectedGroupTitle;
            }
            index++;
        }
        return mAdapter.getItem(0).getTitle();
    }
//MOTO MOD END IKPIM-1044 support group list search
}
