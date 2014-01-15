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

import com.android.contacts.model.HardCodedSources;
import com.android.contacts.R;
import com.android.contacts.RomUtility;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.SimUtility;
import com.android.contacts.util.AccountFilterUtil;

import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment containing a contact list used for browsing (as compared to
 * picking a contact with one of the PICK intents).
 */
//MOTO MOD BEGIN,implemnts the interface for Accelerator
public class DefaultContactBrowseListFragment extends ContactBrowseListFragment implements com.motorola.contacts.widget.ListAccelerator.Interface {
//MOTO MOD END
    private static final String TAG = DefaultContactBrowseListFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private TextView mCounterHeaderView;
    private View mSearchHeaderView;
    private View mAccountFilterHeader;
    private FrameLayout mProfileHeaderContainer;
    private View mProfileHeader;
    private Button mProfileMessage;
    private FrameLayout mMessageContainer;
    private TextView mProfileTitle;

    private View mPaddingView;

    /* MOTOROLA MOD Start: IKPIM-965 */
    private FrameLayout mIceHeaderContainer;
    private View mIceHeader;
    private LinearLayout mIceLauncher;
    private FrameLayout mIceMessageContainer;
    private TextView mIceTitle;
    private View mIcePaddingView;
    private Boolean isIceEnabled = false;
    /* MOTOROLA MOD End of IKPIM-965 */

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                        DefaultContactBrowseListFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader() {
        return new ProfileAndContactsLoader(getActivity());
    }

    @Override
    protected void onItemClick(int position, long id) {
        viewContact(getAdapter().getContactUri(position));
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext());
        //MOTO MOD BEGIN, Accelerator
        adapter.setListAcceleratorListener(this);
        //MOTO MOD END
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        mCounterHeaderView = (TextView) getView().findViewById(R.id.contacts_count);

        /* MOTOROLA MOD Start: IKPIM-965 */
        // Create an ICE header and show it
        addInCaseOfEmergencyHeader(inflater);
        if (isIceEnabled) {
            showEmptyIce(false); //do not display ICE initially
        }
        /* MOTOROLA MOD End of IKPIM-965 */

        // Create an empty user profile header and hide it for now (it will be visible if the
        // contacts list will have no user profile).
        addEmptyUserProfileHeader(inflater);
        showEmptyUserProfile(false);

        // Putting the header view inside a container will allow us to make
        // it invisible later. See checkHeaderViewVisibility()
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        checkHeaderViewVisibility();
    }

    @Override
    public void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
    }

    private void checkHeaderViewVisibility() {
        if (mCounterHeaderView != null) {
            mCounterHeaderView.setVisibility(isSearchMode() ? View.GONE : View.VISIBLE);
        }
        updateFilterHeaderView();

        // Hide the search header by default. See showCount().
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        if (mAccountFilterHeader == null) {
            return; // Before onCreateView -- just ignore it.
        }
        final ContactListFilter filter = getFilter();
        if (filter != null && !isSearchMode()) {
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(
                    mAccountFilterHeader, filter, false, false);
            mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE : View.GONE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        if (!isSearchMode() && data != null) {
            int count = data.getCount();
            if (count != 0) {
                count -= (mUserProfileExists ? 1: 0);
                String format = getResources().getQuantityText(
                        R.plurals.listTotalAllContacts, count).toString();
                // Do not count the user profile in the contacts count
                if (mUserProfileExists) {
                    getAdapter().setContactsCount(String.format(format, count));
                } else {
                    mCounterHeaderView.setText(String.format(format, count));
                }
            } else {
                ContactListFilter filter = getFilter();
                int filterType = filter != null ? filter.filterType
                        : ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS;
                switch (filterType) {
                    case ContactListFilter.FILTER_TYPE_ACCOUNT:
                        if (HardCodedSources.ACCOUNT_LOCAL_DEVICE.equals(filter.accountName)) {
                            mCounterHeaderView.setText(getString(
                                    R.string.listTotalAllContactsZeroGroup, getString(R.string.account_local_device)));
                        } else if (HardCodedSources.ACCOUNT_CARD_C.equals(filter.accountName)) {
                            mCounterHeaderView.setText(getString(
                                    R.string.listTotalAllContactsZeroGroup, getString(R.string.account_card_C)));
                        } else if (HardCodedSources.ACCOUNT_CARD_G.equals(filter.accountName)) {
                            mCounterHeaderView.setText(getString(
                                    R.string.listTotalAllContactsZeroGroup, getString(R.string.account_card_G)));
                        } else if (HardCodedSources.ACCOUNT_CARD.equals(filter.accountName)) {
                            final int phoneType = SimUtility.getTypeByAccountName(filter.accountName);
                            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                                mCounterHeaderView.setText(getString(
                                        R.string.listTotalAllContactsZeroGroup, getString(R.string.account_card_uim)));
                            } else {
                                mCounterHeaderView.setText(getString(
                                        R.string.listTotalAllContactsZeroGroup, getString(R.string.account_card_G)));
                            }
                        } else {
                            mCounterHeaderView.setText(getString(
                                    R.string.listTotalAllContactsZeroGroup, filter.accountName));
                        }
                        break;
                    case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                        mCounterHeaderView.setText(R.string.listTotalPhoneContactsZero);
                        break;
                    case ContactListFilter.FILTER_TYPE_STARRED:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroStarred);
                        break;
                    case ContactListFilter.FILTER_TYPE_CUSTOM:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroCustom);
                        break;
                    default:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZero);
                        break;
                }
            }
        } else {
            ContactListAdapter adapter = getAdapter();
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
            showEmptyUserProfile(false);
        }
    }

    @Override
    protected void setProfileHeader() {
        mUserProfileExists = getAdapter().hasProfile();
        showEmptyUserProfile(!mUserProfileExists && !isSearchMode());
        /* MOTOROLA MOD Start: IKPIM-965 */
        if (isIceEnabled) {
            showEmptyIce(!isSearchMode());
        }
        /* MOTOROLA MOD End of IKPIM-965 */
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

    private void showEmptyUserProfile(boolean show) {
        // Changing visibility of just the mProfileHeader doesn't do anything unless
        // you change visibility of its children, hence the call to mCounterHeaderView
        // and mProfileTitle
        mProfileHeaderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileHeader.setVisibility(show ? View.VISIBLE : View.GONE);
        mCounterHeaderView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        mMessageContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileMessage.setVisibility(show ? View.VISIBLE : View.GONE);

        mPaddingView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * This method creates a pseudo user profile contact. When the returned query doesn't have
     * a profile, this methods creates 2 views that are inserted as headers to the listview:
     * 1. A header view with the "ME" title and the contacts count.
     * 2. A button that prompts the user to create a local profile
     */
    private void addEmptyUserProfileHeader(LayoutInflater inflater) {

        ListView list = getListView();
        // Put a header with the "ME" name and a view for the number of contacts
        // The view is embedded in a frame view since you cannot change the visibility of a
        // view in a ListView without having a parent view.
        mProfileHeaderContainer = new FrameLayout(inflater.getContext());
        mProfileHeader = inflater.inflate(R.layout.user_profile_header, null, false);
        mCounterHeaderView = (TextView) mProfileHeader.findViewById(R.id.contacts_count);
        mProfileTitle = (TextView) mProfileHeader.findViewById(R.id.profile_title);
        mProfileTitle.setAllCaps(true);
        mProfileHeaderContainer.addView(mProfileHeader);
        list.addHeaderView(mProfileHeaderContainer, null, false);

        // Add a selectable view with a message inviting the user to create a local profile
        mMessageContainer = new FrameLayout(inflater.getContext());
        mProfileMessage = (Button)inflater.inflate(R.layout.user_profile_button, null, false);
        mMessageContainer.addView(mProfileMessage);
        list.addHeaderView(mMessageContainer, null, true);

        mProfileMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               if(RomUtility.isOutofMemory()){
                	Toast.makeText(DefaultContactBrowseListFragment.this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                	return ;
                }
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactEditorFragment.INTENT_EXTRA_NEW_LOCAL_PROFILE, true);
                startActivity(intent);
            }
        });

        View paddingViewContainer =
                inflater.inflate(R.layout.contact_detail_list_padding, null, false);
        mPaddingView = paddingViewContainer.findViewById(R.id.contact_detail_list_padding);
        mPaddingView.setVisibility(View.GONE);
        getListView().addHeaderView(paddingViewContainer);
    }

/* MOTOROLA MOD Start: IKPIM-965 */
    /**
     * This method creates an In case of Emergency contact. When the returned query doesn't have
     * a profile, this methods creates 2 views that are inserted as headers to the listview:
     * 1. A header view with the "ICE" title and the contacts count.
     * 2. A button that prompts the user to lunch ICE
     */
    private void addInCaseOfEmergencyHeader(LayoutInflater inflater) {

        final PackageManager packageManager = getContext().getPackageManager();
        final Intent iceStartIntent = new Intent("com.motorola.contacts.ACTION_VIEW_ICE_ACTIONS");
        ResolveInfo resolveInfo = packageManager.resolveActivity(iceStartIntent,0);
        if (resolveInfo !=null && resolveInfo.activityInfo != null) {
            isIceEnabled = true;
        } else {
            isIceEnabled = false;
            return;
        }

        ListView list = getListView();
        // Put a header with the "ME" name and a view for the number of contacts
        // The view is embedded in a frame view since you cannot change the visibility of a
        // view in a ListView without having a parent view.
        mIceHeaderContainer = new FrameLayout(inflater.getContext());
        mIceHeader = inflater.inflate(R.layout.moto_ice_header, null, false);

        //Moto: we will display the numbers on google ME profile session header
        //mCounterHeaderView = (TextView) mIceHeader.findViewById(R.id.contacts_count);
        mIceTitle = (TextView) mIceHeader.findViewById(R.id.profile_title);
        mIceTitle.setAllCaps(true);
        mIceHeaderContainer.addView(mIceHeader);
        list.addHeaderView(mIceHeaderContainer, null, false);

        // Add a selectable view with a message inviting the user to create a local profile
        mIceMessageContainer = new FrameLayout(inflater.getContext());
        mIceLauncher = (LinearLayout)inflater.inflate(R.layout.moto_ice_launcher_item, null, false);
        ((ImageView) mIceLauncher.findViewById(R.id.moto_ice_item_icon)).setImageDrawable(resolveInfo.loadIcon(packageManager));
        ((TextView) mIceLauncher.findViewById(R.id.moto_ice_item_text)).setText(resolveInfo.loadLabel(packageManager));
        mIceMessageContainer.addView(mIceLauncher);
        list.addHeaderView(mIceMessageContainer, null, true);

        mIceLauncher.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                iceStartIntent.putExtra("isLockMode", false);
                startActivity(iceStartIntent);
            }
        });

        View paddingViewContainer =
            inflater.inflate(R.layout.contact_detail_list_padding, null, false);
        mIcePaddingView = paddingViewContainer.findViewById(R.id.contact_detail_list_padding);
        mIcePaddingView.setVisibility(View.GONE);
        getListView().addHeaderView(paddingViewContainer);
    }

    private void showEmptyIce(boolean show) {
        if (isIceEnabled==false) {
            return; // this should not happen as Ice feature is not enabled
        }
        // Changing visibility of just the mProfileHeader doesn't do anything unless
        // you change visibility of its children, hence the call to mCounterHeaderView
        // and mProfileTitle
        mIceHeaderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mIceHeader.setVisibility(show ? View.VISIBLE : View.GONE);
        //Ice does not display numbers : mCounterHeaderView.setVisibility(show ? View.VISIBLE : View.GONE);
        mIceTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        mIceMessageContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mIceLauncher.setVisibility(show ? View.VISIBLE : View.GONE);

        mIcePaddingView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
/* MOTOROLA MOD End of IKPIM-965 */
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
        if(isIceEnabled){
            return -7;
        }
        else {
            return -4;
        }
    }
//MOTO MOD END
}
