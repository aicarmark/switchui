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
 * limitations under the License.
 */
package com.android.contacts.list;

import com.android.contacts.R;
import com.android.contacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.util.AccountFilterUtil;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
//MOTO MOD BEGIN
import android.text.TextUtils;
//MOTO MOD END

/**
 * Fragment containing a phone number list for picking.
 */
//MOTO MOD BEGIN
public class PhoneNumberPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter>
        implements OnShortcutIntentCreatedListener ,com.motorola.contacts.widget.ListAccelerator.Interface {
//MOTO MOD END
    private static final String TAG = PhoneNumberPickerFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private OnPhoneNumberPickerActionListener mListener;
    private String mShortcutAction;

    private ContactListFilter mFilter;

    private View mAccountFilterHeader;
    /**
     * Lives as ListView's header and is shown when {@link #mAccountFilterHeader} is set
     * to View.GONE.
     */
    private View mPaddingView;

    private static final String KEY_FILTER = "filter";
    private static final String KEY_SHORTCUTACTION="shortcutAction";     // MOT MOD -- IKHSS6-5763

    /** true if the loader has started at least once. */
    private boolean mLoaderStarted;
    
    /*Added for switchuitwo-372 begin*/
    private boolean isSearching = false;
    /*Added for switchuitwo-372 end*/
    
    private View mSearchHeaderView;// Motorola, ODC_001639, 2013-01-08, SWITCHUITWO-362

    private ContactListItemView.PhotoPosition mPhotoPosition =
            ContactListItemView.DEFAULT_PHOTO_POSITION;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    PhoneNumberPickerFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public PhoneNumberPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DATA_SHORTCUT);

        // Show nothing instead of letting caller Activity show something.
        setHasOptionsMenu(true);
    }

    public void setOnPhoneNumberPickerActionListener(OnPhoneNumberPickerActionListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        View paddingView = inflater.inflate(R.layout.contact_detail_list_padding, null, false);
        mPaddingView = paddingView.findViewById(R.id.contact_detail_list_padding);
        getListView().addHeaderView(paddingView);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        updateFilterHeaderView();

        setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
        
        // BEGIN Motorola, ODC_001639, 2013-01-08, SWITCHUITWO-362
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        //END SWITCHUITWO-362
    }
    
    // BEGIN Motorola, ODC_001639, 2013-01-08, SWITCHUITWO-362
    @Override
    protected void showCount(int partitionIndex, Cursor data) {
    	ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        // In search mode we only display the header if there is nothing found
        if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
            mSearchHeaderView.setVisibility(View.GONE);
        } else {
            TextView textView = (TextView) mSearchHeaderView.findViewById(
                    R.id.totalContactsText);
            ProgressBar progress = (ProgressBar) mSearchHeaderView.findViewById(
                    R.id.progress);
            mSearchHeaderView.setVisibility(View.VISIBLE);
            if (adapter.isLoading()) {
                textView.setText(R.string.search_results_searching);
                progress.setVisibility(View.VISIBLE);
            } else {
                textView.setText(R.string.listFoundAllContactsZero);
                textView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
                progress.setVisibility(View.GONE);
            }
        }
    }
  //END SWITCHUITWO-362

    @Override
    public void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        final ContactListFilter filter = getFilter();
        if (mAccountFilterHeader == null || filter == null) {
            return;
        }
        final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPhone(
                mAccountFilterHeader, filter, false, false);
        /*Modifyed for switchuitwo-372 begin*/
        if (shouldShowHeader && !isSearching) {
        	/*Modifyed for switchuitwo-372 end*/
            mPaddingView.setVisibility(View.GONE);
            mAccountFilterHeader.setVisibility(View.VISIBLE);
        } else {
            mPaddingView.setVisibility(View.VISIBLE);
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mFilter = savedState.getParcelable(KEY_FILTER);
        mShortcutAction = savedState.getString(KEY_SHORTCUTACTION);     // MOT MOD -- IKHSS6-5763
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mFilter);
        outState.putString(KEY_SHORTCUTACTION, mShortcutAction);        // MOT MOD -- IKHSS6-5763
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            if (mListener != null) {
                mListener.onHomeInActionBarSelected();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param shortcutAction either {@link Intent#ACTION_CALL} or
     *            {@link Intent#ACTION_SENDTO} or null.
     */
    public void setShortcutAction(String shortcutAction) {
        this.mShortcutAction = shortcutAction;
    }

    @Override
    protected void onItemClick(int position, long id) {
        final Uri phoneUri;
        if (!isLegacyCompatibilityMode()) {
            PhoneNumberListAdapter adapter = (PhoneNumberListAdapter) getAdapter();
            phoneUri = adapter.getDataUri(position);

        } else {
            LegacyPhoneNumberListAdapter adapter = (LegacyPhoneNumberListAdapter) getAdapter();
            phoneUri = adapter.getPhoneUri(position);
        }

        if (phoneUri != null) {
            pickPhoneNumber(phoneUri);
        } else {
            Log.w(TAG, "Item at " + position + " was clicked before adapter is ready. Ignoring");
        }
    }

    @Override
    protected void startLoading() {
        mLoaderStarted = true;
        super.startLoading();
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            PhoneNumberListAdapter adapter = new PhoneNumberListAdapter(getActivity());
            adapter.setDisplayPhotos(true);
            //MOTO MOD BEGIN
            adapter.setListAcceleratorListener(this);
            //MOTO MOD END
            return adapter;
        } else {
            LegacyPhoneNumberListAdapter adapter = new LegacyPhoneNumberListAdapter(getActivity());
            adapter.setDisplayPhotos(true);
            return adapter;
        }
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();

        final ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        if (!isSearchMode() && mFilter != null) {
            adapter.setFilter(mFilter);
        }

        if (!isLegacyCompatibilityMode()) {
            ((PhoneNumberListAdapter) adapter).setPhotoPosition(mPhotoPosition);
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    public void pickPhoneNumber(Uri uri) {
        if (mShortcutAction == null) {
            mListener.onPickPhoneNumberAction(uri);
        } else {
            if (isLegacyCompatibilityMode()) {
                throw new UnsupportedOperationException();
            }
            ShortcutIntentBuilder builder = new ShortcutIntentBuilder(getActivity(), this);
            builder.createPhoneNumberShortcutIntent(uri, mShortcutAction);
        }
    }

    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
        mListener.onShortcutIntentCreated(shortcutIntent);
    }

    @Override
    public void onPickerResult(Intent data) {
        mListener.onPickPhoneNumberAction(data.getData());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    public ContactListFilter getFilter() {
        return mFilter;
    }

    public void setFilter(ContactListFilter filter) {
        if ((mFilter == null && filter == null) ||
                (mFilter != null && mFilter.equals(filter))) {
            return;
        }

        mFilter = filter;
        if (mLoaderStarted) {
            reloadData();
        }
        updateFilterHeaderView();
    }

    public void setPhotoPosition(ContactListItemView.PhotoPosition photoPosition) {
        mPhotoPosition = photoPosition;
        if (!isLegacyCompatibilityMode()) {
            final PhoneNumberListAdapter adapter = (PhoneNumberListAdapter) getAdapter();
            if (adapter != null) {
                adapter.setPhotoPosition(photoPosition);
            }
        } else {
            Log.w(TAG, "setPhotoPosition() is ignored in legacy compatibility mode.");
        }
    }
//MOTO MOD BEGIN , implemnts the interface for Accelerator
    // if phone only, we shall not display the icon
    public boolean isNeeded(){
        boolean shouldshow = false;
        if (TextUtils.isEmpty(getQueryString()) || !isSearchMode()){
            shouldshow = true;
        }
        return shouldshow;
    }
    public int getTopOffset(){
        return 0;
    }
    public int getVisibleOffset(){
        return -1;
    }
//MOTO MOD END
    /*Added for switchuitwo-372 begin*/
    public void hideAccountFilterHeader() {
    	isSearching = true;
    	if(mAccountFilterHeader != null) {
    		mAccountFilterHeader.setVisibility(View.GONE);
    	}
    }
    public void showAccountFilterHeader() {
    	isSearching = false;
    	//Modifyed for switchuitwo-531 begin
    	//mAccountFilterHeader will be updated when updateFilterHeaderView
    	//if(mAccountFilterHeader != null) {
    		//mAccountFilterHeader.setVisibility(View.VISIBLE);
    	//}
    	//Modifyed for switchuitwo-531 end
    }
    /*Added for switchuitwo-372 end*/
}