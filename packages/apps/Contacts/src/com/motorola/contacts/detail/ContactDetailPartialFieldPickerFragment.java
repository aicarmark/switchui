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

package com.motorola.contacts.detail;

import com.android.contacts.Collapser;
import com.android.contacts.Collapser.Collapsible;
import com.android.contacts.ContactLoader;
import com.android.contacts.ContactPresenceIconUtil;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ActionsViewContainer;
import com.android.contacts.GroupMetaData;
import com.android.contacts.NfcHandler;
import com.android.contacts.R;
import com.android.contacts.TypePrecedence;
import com.android.contacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.util.Constants;
import com.android.contacts.util.DataStatus;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.widget.TransitionAnimationView;
import com.android.internal.telephony.ITelephony;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.Intents.UI;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// BEGIN Motorola, FTR 36344, IKHSS6-8444
import com.motorola.contacts.util.MEDialer;
// END IKHSS6-8444

public class ContactDetailPartialFieldPickerFragment extends ContactDetailFragment {

    private static final String TAG = "ContactDetailPartialFieldPickerFragment";

    private static final String KEY_USER_CHECKED_IDS = "UserCheckedIds";

    private Button mOKButton;
    private Button mCancelButton;

    private ArrayList<Integer> mUserCheckedIds = null;
    private List<ContentValues> mNameContentValuesList = new ArrayList<ContentValues>();
    private List<ContentValues> mPhotoContentValuesList = new ArrayList<ContentValues>();
    private List<ContentValues> mOrganizationContentValuesList = new ArrayList<ContentValues>();
    private boolean mIsCreateFromSavedState = false;
    //MOT MOD BEGIN - IKPIM-899, send vCard with vCardContentString as EXTRA
    private String vCardContentString = "";
    //MOT MOD END - IKPIM-899

    public ContactDetailPartialFieldPickerFragment() {
        // Explicit constructor for inflation
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mUserCheckedIds = savedInstanceState.getIntegerArrayList(KEY_USER_CHECKED_IDS);
            mIsCreateFromSavedState = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //remember user checked items when SaveInstanceState
        if(mUserCheckedIds == null){
            mUserCheckedIds = new ArrayList<Integer>();
        }
        mUserCheckedIds.clear();
        if(mListView != null && mAdapter!=null){
            SparseBooleanArray checkStates = mListView.getCheckedItemPositions();
            int adapterSize = mAdapter.getCount();
            for (int i = 0; i < adapterSize; i++) {
                if(checkStates.get(i)){
                    mUserCheckedIds.add(i);
                }
            }
        }
        outState.putIntegerArrayList (KEY_USER_CHECKED_IDS, mUserCheckedIds);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mView = inflater.inflate(R.layout.moto_contact_detail_partial_field_picker_fragment, container, false);

        mInflater = inflater;

        mStaticPhotoView = (ImageView) mView.findViewById(R.id.photo);

        mListView = (ListView) mView.findViewById(android.R.id.list);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
//        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(true);
        mListView.setOnScrollListener(mVerticalScrollListener);

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Don't set it to mListView yet.  We do so later when we bind the adapter.
        mEmptyView = mView.findViewById(android.R.id.empty);

        mAlphaLayer = mView.findViewById(R.id.alpha_overlay);
        mTouchInterceptLayer = mView.findViewById(R.id.touch_intercept_overlay);

        mQuickFixButton = (Button) mView.findViewById(R.id.contact_quick_fix);

        mOKButton = (Button) mView.findViewById(R.id.ok);
        mOKButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareContact();
                ((Activity)mContext).finish();
            }
        });
        mCancelButton = (Button) mView.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)mContext).finish();
            }
        });


        mView.setVisibility(View.INVISIBLE);

        if (mContactData != null) {
            bindData();
            Log.e("zb", "bindData in onCreateView");
        }

        return mView;
    }

    protected void bindData() {
        if (mView == null) {
            return;
        }

        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }

        if (mTransitionAnimationRequested) {
            TransitionAnimationView.startAnimation(mView, mContactData == null);
            mTransitionAnimationRequested = false;
        }

        if (mContactData == null) {
            mView.setVisibility(View.INVISIBLE);
            return;
        }

        // Figure out if the contact has social updates or not
        mContactHasSocialUpdates = !mContactData.getStreamItems().isEmpty();

        // Setup the photo if applicable
        if (mStaticPhotoView != null) {
            // The presence of a static photo view is not sufficient to determine whether or not
            // we should show the photo. Check the mShowStaticPhoto flag which can be set by an
            // outside class depending on screen size, layout, and whether the contact has social
            // updates or not.
            if (mShowStaticPhoto) {
                mStaticPhotoView.setVisibility(View.VISIBLE);
                ContactDetailDisplayUtils.setPhoto(mContext, mContactData, mStaticPhotoView);
            } else {
                mStaticPhotoView.setVisibility(View.GONE);
            }
        }

        // Build up the contact entries
        buildEntries();

        // Collapse similar data items for select {@link DataKind}s.
        Collapser.collapseList(mPhoneEntries);
        Collapser.collapseList(mSmsEntries);
        Collapser.collapseList(mEmailEntries);
        Collapser.collapseList(mPostalEntries);
        Collapser.collapseList(mImEntries);

        mIsUniqueNumber = mPhoneEntries.size() == 1;
        mIsUniqueEmail = mEmailEntries.size() == 1;

        // Make one aggregated list of all entries for display to the user.
        setupFlattenedList();

        if (mAdapter == null) {
            mAdapter = new PartialFieldPickerViewAdapter();
            mListView.setAdapter(mAdapter);
        }

        if(!mIsCreateFromSavedState){//check all the items defautly when ContactDetailPartialFieldPickerFragment is created
            if((mAdapter!=null) && (mListView!=null)){
                int adapterSize = mAdapter.getCount();
                for(int i = 0; i < adapterSize; i ++){
                    final ViewEntry entry = (ViewEntry)mAdapter.getItem(i);
                    if(!(entry instanceof DetailViewEntry)) continue;
                    mListView.setItemChecked(i, true);
                }
            }
        }else{//this is restored from saving state, for example orientation/config change
            // Restore {@link ListView} state if applicable because the adapter is now populated.
            if (mListState != null) {
                mListView.onRestoreInstanceState(mListState);
                mListState = null;

                if((mAdapter!=null) && (mListView!=null)){
                    for (int i = 0; i < mUserCheckedIds.size(); i++) {
                        mListView.setItemChecked(mUserCheckedIds.get(i), true);
                    }
                }
            }
        }

        mAdapter.notifyDataSetChanged();

        mListView.setEmptyView(mEmptyView);

        mView.setVisibility(View.VISIBLE);
    }

    /**
     * Build up the entries to display on the screen.
     */
    private final void buildEntries() {
        mHasPhone = PhoneCapabilityTester.isPhone(mContext);
        mHasSms = PhoneCapabilityTester.isSmsIntentRegistered(mContext);
        mHasSip = PhoneCapabilityTester.isSipPhone(mContext);

        // Clear out the old entries
        mAllEntries.clear();

        mRawContactIds.clear();

        mPrimaryPhoneUri = null;
        mNumPhoneNumbers = 0;

        //mWritableRawContactIds.clear(); cdxw84, no mWritableRawContactIds now, for pass build

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);

        // Build up method entries
        if (mContactData == null) {
            return;
        }

        ArrayList<String> groups = new ArrayList<String>();
        for (Entity entity: mContactData.getEntities()) {
            final ContentValues entValues = entity.getEntityValues();
            final String accountType = entValues.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = entValues.getAsString(RawContacts.DATA_SET);
            final long rawContactId = entValues.getAsLong(RawContacts._ID);

            if (!mRawContactIds.contains(rawContactId)) {
                mRawContactIds.add(rawContactId);
            }
            AccountType type = accountTypes.getAccountType(accountType, dataSet);
            //if (type == null || !type.readOnly) {
            //    mWritableRawContactIds.add(rawContactId);
            //} cdxw84, no mWritableRawContactIds now, for pass build

            for (NamedContentValues subValue : entity.getSubValues()) {
                final ContentValues entryValues = subValue.values;
                entryValues.put(Data.RAW_CONTACT_ID, rawContactId);

                final long dataId = entryValues.getAsLong(Data._ID);
                final String mimeType = entryValues.getAsString(Data.MIMETYPE);
                if (mimeType == null) continue;

                if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    Long groupId = entryValues.getAsLong(GroupMembership.GROUP_ROW_ID);
                    if (groupId != null) {
                        handleGroupMembership(groups, mContactData.getGroupMetaData(), groupId);
                    }
                    continue;
                }

                final DataKind kind = accountTypes.getKindOrFallback(
                        accountType, dataSet, mimeType);
                if (kind == null) continue;

                final DetailViewEntry entry = DetailViewEntry.fromValues(mContext, mimeType, kind,
                        dataId, entryValues, mContactData.isDirectoryEntry(),
                        mContactData.getDirectoryId());

                final boolean hasData = !TextUtils.isEmpty(entry.data);
                Integer superPrimary = entryValues.getAsInteger(Data.IS_SUPER_PRIMARY);
                final boolean isSuperPrimary = superPrimary != null && superPrimary != 0;

                //build mPhotoContentValuesList
                if(Photo.CONTENT_ITEM_TYPE.equals(mimeType)){
                     mPhotoContentValuesList.add(entryValues);
                }

                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    //build mNameContentValuesList
                    mNameContentValuesList.add(entryValues);
                } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build phone entries
                    mNumPhoneNumbers++;
                    String phoneNumberE164 =
                            entryValues.getAsString(PhoneLookup.NORMALIZED_NUMBER);
                    //MOT MOD BEGIN IKHSS7-2038
                    //To Support New API PhoneNumberUtilsExt.formatNumber
                    entry.data = PhoneNumberUtilsExt.formatNumber(mContext,
                            entry.data, phoneNumberE164, mDefaultCountryIso);
                    //MOT MOD END IKHSS7-2038
                    final Intent intent;
                    if (ContactsUtils.haveTwoCards(mContext)) {
                        intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("phoneNumber", entry.data);
                    } else {
                        intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                Uri.fromParts(Constants.SCHEME_TEL, entry.data, null));
                    }
                    final Intent phoneIntent = mHasPhone ? intent : null;
                    final Intent smsIntent = mHasSms ? new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_SMSTO, entry.data, null)) : null;

                    // BEGIN Motorola, FTR 36344, IKHSS6-8444
                    if (phoneIntent != null) {
                        MEDialer.onDial(mContext, phoneIntent, MEDialer.DialFrom.CONTACTS);
                    }
                    // END IKHSS6-8444

                    // Configure Icons and Intents. Notice actionIcon is already set to the phone
                    if (mHasPhone && mHasSms) {
                        entry.intent = phoneIntent;
                        entry.secondaryIntent = smsIntent;
                        entry.secondaryActionIcon = kind.iconAltRes;
                        entry.secondaryActionDescription = kind.iconAltDescriptionRes;
                    } else if (mHasPhone) {
                        entry.intent = phoneIntent;
                    } else if (mHasSms) {
                        entry.intent = smsIntent;
                        //entry.actionIcon = kind.iconAltRes; cdxw84, no actionIcon, for pass build
                    } else {
                        entry.intent = null;
                        //entry.actionIcon = -1; cdxw84, no actionIcon, for pass build
                    }

                    // Remember super-primary phone
                    if (isSuperPrimary) mPrimaryPhoneUri = entry.uri;

                    entry.isPrimary = isSuperPrimary;
                    mPhoneEntries.add(entry);
                } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build email entries
                    entry.intent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_MAILTO, entry.data, null));
                    entry.isPrimary = isSuperPrimary;
                    mEmailEntries.add(entry);

                    // When Email rows have status, create additional Im row
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        final String imMime = Im.CONTENT_ITEM_TYPE;
                        final DataKind imKind = accountTypes.getKindOrFallback(accountType, dataSet,
                                imMime);
                        final DetailViewEntry imEntry = DetailViewEntry.fromValues(mContext, imMime,
                                imKind, dataId, entryValues, mContactData.isDirectoryEntry(),
                                mContactData.getDirectoryId());
                        buildImActions(mContext, imEntry, entryValues); //cdxw84, add mContext, for pass build
                        imEntry.applyStatus(status, false);
                        mImEntries.add(imEntry);
                    }
                } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build postal entries
                    entry.maxLines = 4;
                    entry.intent = new Intent(
                            Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(entry.data)));
                    mPostalEntries.add(entry);
                } else if (Im.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build IM entries
                    buildImActions(mContext, entry, entryValues); //cdxw84, add mContext, for pass build

                    // Apply presence and status details when available
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        entry.applyStatus(status, false);
                    }
                    mImEntries.add(entry);
                } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // Organizations are not shown. The first one is shown in the header
                    // and subsequent ones are not supported anymore
                    //build mOrganizationContentValuesList
                    mOrganizationContentValuesList.add(entryValues);
                } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build nickname entries
                    final boolean isNameRawContact =
                        (mContactData.getNameRawContactId() == rawContactId);

                    final boolean duplicatesTitle =
                        isNameRawContact
                        && mContactData.getDisplayNameSource() == DisplayNameSources.NICKNAME;

                    if (!duplicatesTitle) {
                        entry.uri = null;
                        mNicknameEntries.add(entry);
                    }
                } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build note entries
                    entry.uri = null;
                    entry.maxLines = 100;
                    mNoteEntries.add(entry);
                } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build Website entries
                    entry.uri = null;
                    entry.maxLines = 10;
                    try {
                        WebAddress webAddress = new WebAddress(entry.data);
                        entry.intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(webAddress.toString()));
                    } catch (ParseException e) {
                        Log.e(TAG, "Couldn't parse website: " + entry.data);
                    }
                    mWebsiteEntries.add(entry);
                } else if (SipAddress.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build SipAddress entries
                    entry.uri = null;
                    entry.maxLines = 1;
                    if (mHasSip) {
                        final Intent intent;
                        if (ContactsUtils.haveTwoCards(mContext)) {
                            intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("phoneNumber", entry.data);
                        } else {
                            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                    Uri.fromParts(Constants.SCHEME_SIP, entry.data, null));
                        }
                        entry.intent = intent;
                        // BEGIN Motorola, FTR 36344, IKHSS6-8444
                        MEDialer.onDial(mContext, entry.intent, MEDialer.DialFrom.CONTACTS);
                        // END IKHSS6-8444
                    } else {
                        entry.intent = null;
                        //entry.actionIcon = -1; cdxw84, no actionIcon, for pass build
                    }
                    mSipEntries.add(entry);
                    // TODO: Now that SipAddress is in its own list of entries
                    // (instead of grouped in mOtherEntries), consider
                    // repositioning it right under the phone number.
                    // (Then, we'd also update FallbackAccountType.java to set
                    // secondary=false for this field, and tweak the weight
                    // of its DataKind.)
                } else if (Event.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    entry.data = DateUtils.formatDate(mContext, entry.data);
                    entry.uri = null;
                    mEventEntries.add(entry);
                } else if (Relation.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    entry.intent = new Intent(Intent.ACTION_SEARCH);
                    entry.intent.putExtra(SearchManager.QUERY, entry.data);
                    entry.intent.setType(Contacts.CONTENT_TYPE);
                    mRelationEntries.add(entry);
                } else {
                    // Handle showing custom rows
                    entry.intent = new Intent(Intent.ACTION_VIEW);
                    entry.intent.setDataAndType(entry.uri, entry.mimetype);

                    // Use social summary when requested by external source
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    final boolean hasSocial = kind.actionBodySocial && status != null;
                    if (hasSocial) {
                        entry.applyStatus(status, true);
                    }

                    if (hasSocial || hasData) {
                        // If the account type exists in the hash map, add it as another entry for
                        // that account type
                        if (mOtherEntriesMap.containsKey(type)) {
                            List<DetailViewEntry> listEntries = mOtherEntriesMap.get(type);
                            listEntries.add(entry);
                        } else {
                            // Otherwise create a new list with the entry and add it to the hash map
                            List<DetailViewEntry> listEntries = new ArrayList<DetailViewEntry>();
                            listEntries.add(entry);
                            mOtherEntriesMap.put(type, listEntries);
                        }
                    }
                }
            }
        }

        if (!groups.isEmpty()) {
            DetailViewEntry entry = new DetailViewEntry();
            Collections.sort(groups);
            StringBuilder sb = new StringBuilder();
            int size = groups.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(groups.get(i));
            }
            entry.mimetype = GroupMembership.MIMETYPE;
            entry.kind = mContext.getString(R.string.groupsLabel);
            entry.data = sb.toString();
            mGroupEntries.add(entry);
        }
    }

    /**
     * Collapse all contact detail entries into one aggregated list with a {@link HeaderViewEntry}
     * at the top.
     */
    private void setupFlattenedList() {
        // All contacts should have a header view (even if there is no data for the contact).
        mAllEntries.add(new HeaderViewEntry());

//        addPhoneticName();

        flattenList(mPhoneEntries);
        flattenList(mSmsEntries);
        flattenList(mEmailEntries);
        flattenList(mImEntries);
        flattenList(mNicknameEntries);
        flattenList(mWebsiteEntries);

//        addNetworks();

        flattenList(mSipEntries);
        flattenList(mPostalEntries);
        flattenList(mEventEntries);
//        flattenList(mGroupEntries);
        flattenList(mRelationEntries);
        flattenList(mNoteEntries);
    }

    /**
     * Iterate through {@link DetailViewEntry} in the given list and add it to a list of all
     * entries. Add a {@link KindTitleViewEntry} at the start if the length of the list is not 0.
     * Add {@link SeparatorViewEntry}s as dividers as appropriate. Clear the original list.
     */
    private void flattenList(ArrayList<DetailViewEntry> entries) {
        int count = entries.size();

        // Add a title for this kind by extracting the kind from the first entry
        if (count > 0) {
            String kind = entries.get(0).kind;
            mAllEntries.add(new KindTitleViewEntry(kind.toUpperCase()));
        }

        // Add all the data entries for this kind
        for (int i = 0; i < count; i++) {
            // For all entries except the first one, add a divider above the entry
            if (i != 0) {
                mAllEntries.add(new SeparatorViewEntry());
            }
            mAllEntries.add(entries.get(i));
        }

        // Clear old list because it's not needed anymore.
        entries.clear();
    }

    private final class PartialFieldPickerViewAdapter extends ContactDetailFragment.ViewAdapter {

        protected View getHeaderEntryView(View convertView, ViewGroup parent) {
            final int desiredLayoutResourceId = R.layout.detail_header_contact_without_updates;
            View result = null;
            HeaderViewCache viewCache = null;

            // Only use convertView if it has the same layout resource ID as the one desired
            // (the two can be different on wide 2-pane screens where the detail fragment is reused
            // for many different contacts that do and do not have social updates).
            if (convertView != null) {
                viewCache = (HeaderViewCache) convertView.getTag();
                if (viewCache.layoutResourceId == desiredLayoutResourceId) {
                    result = convertView;
                }
            }

            // Otherwise inflate a new header view and create a new view cache.
            if (result == null) {
                result = mInflater.inflate(desiredLayoutResourceId, parent, false);
                viewCache = new HeaderViewCache(result, desiredLayoutResourceId);
                result.setTag(viewCache);
            }

            ContactDetailDisplayUtils.setDisplayName(mContext, mContactData,
                    viewCache.displayNameView);
            ContactDetailDisplayUtils.setCompanyName(mContext, mContactData, viewCache.companyView);

            // Set the photo if it should be displayed
            if (viewCache.photoView != null) {
                ContactDetailDisplayUtils.setPhoto(mContext, mContactData, viewCache.photoView);
            }

            // Set the starred state if it should be displayed
            final CheckBox favoritesStar = viewCache.starredView;
            if (favoritesStar != null) {
                ContactDetailDisplayUtils.setStarred(mContactData, favoritesStar);
                favoritesStar.setClickable(false);
            }

            return result;
        }

        protected View getDetailEntryView(int position, View convertView, ViewGroup parent) {
            final DetailViewEntry entry = (DetailViewEntry) getItem(position);
            final View v;
            final DetailViewCache viewCache;

            // Check to see if we can reuse convertView
            if (convertView != null) {
                v = convertView;
                viewCache = (DetailViewCache) v.getTag();
            } else {
                // Create a new view if needed
                v = mInflater.inflate(R.layout.moto_contact_detail_partial_field_picker_list_item, parent, false);

                // Cache the children
                viewCache = new DetailViewCache(v, null, null);
                v.setTag(viewCache);
            }

            bindDetailView(position, v, entry);
            // BEGIN Motorola, ODC_001639, 2012-12-21, SWITCHUITWO-341
            viewCache.secondaryActionButton.setVisibility(View.GONE);
            viewCache.secondaryActionDivider.setVisibility(View.GONE);
            // END SWITCHUITWO-341
            return v;
        }

    }

    private boolean shareContact() {
        if (mContactData == null) return false;

        String vcard = buildContact2Vcard();
        if((vcard == null) || vcard.equals("")) return false;

        final String lookupKey = mContactData.getLookupKey();
        Uri shareUri = null;
        if(mContactData.isUserProfile()){//current contact is profile contact
            shareUri = Uri.withAppendedPath(Profile.CONTENT_URI, "as_partial_field_vcard");

            // User is sharing the profile.  We don't want to force the receiver to have
            // the highly-privileged READ_PROFILE permission, so we need to request a
            // pre-authorized URI from the provider.
            shareUri = getPreAuthorizedUri(shareUri);
        }else{
            shareUri = Uri.withAppendedPath(Contacts.CONTENT_URI, "as_partial_field_vcard");
            shareUri = Uri.withAppendedPath(shareUri, lookupKey);
        }

        ContentValues vcardContentVal = new ContentValues();
        vcardContentVal.put("partial_field_vcard", vcard );
        mContext.getContentResolver().insert(shareUri, vcardContentVal);

        boolean returnBackDirectly = ((Activity) mContext).getIntent().getBooleanExtra("INTENTEXTRA_BOOLEAN_PARTIAL_FIELD_PICKER_RETURN_BACK", false);
        if(returnBackDirectly){// return back directly, for msg insert vcard
            Intent intent = new Intent();
            //MOT MOD BEGIN - IKPIM-899, send vCard with vCardContentString as EXTRA
            intent.putExtra(Intent.EXTRA_TEXT, vCardContentString);
            //MOT MOD END - IKPIM-899
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            intent.setType(Contacts.CONTENT_VCARD_TYPE);
            ((Activity) mContext).setResult(Activity.RESULT_OK, intent);
            return true;
        }

        // MOT CHINA
        boolean sendviaSMS = ((Activity) mContext).getIntent().getBooleanExtra("SEND_VIA_SMS", false);
        boolean sendviaMMS = ((Activity) mContext).getIntent().getBooleanExtra("SEND_VIA_MMS", false);
        if (sendviaSMS || sendviaMMS){
            Intent intent = null;
            if (sendviaSMS) {
                intent = new Intent("com.motorola.message.intent.action.SEND_CONTACT_TEXT");
                intent.putExtra(Intent.EXTRA_TEXT, vCardContentString);
            } else { // sendviaMMS
                intent = new Intent("com.motorola.message.intent.action.SEND_CONTACT_VCARD");
            }
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            intent.setType(Contacts.CONTENT_VCARD_TYPE);
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        // END of MOT CHINA

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        //MOT MOD BEGIN - IKPIM-899, send vCard with vCardContentString as EXTRA
        intent.putExtra(Intent.EXTRA_TEXT, vCardContentString);
        //MOT MOD END - IKPIM-899

        // Launch chooser to share contact via
        final CharSequence chooseTitle = mContext.getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            mContext.startActivity(chooseIntent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private String buildContact2Vcard() {
        final Map<String, List<ContentValues>> contentValuesListMap = new HashMap<String, List<ContentValues>>();

        //MOT MOD BEGIN - IKPIM-899, send vCard with vCardContentString as EXTRA
        final String carriageReturn = "\r\n";
        final String space = " : ";

        StringBuffer res = new StringBuffer();
        if(mContactData.getDisplayName() != null) {
            res.append(mContactData.getDisplayName());
            res.append(carriageReturn);
        }
        if(ContactDetailDisplayUtils.getCompany(mContext, mContactData) != null) {
            res.append(ContactDetailDisplayUtils.getCompany(mContext, mContactData));
            res.append(carriageReturn);
        }
        //MOT MOD END - IKPIM-899

        //Add mNameContentValuesList
        if(mNameContentValuesList.size() > 0 ){
            contentValuesListMap.put(StructuredName.CONTENT_ITEM_TYPE, mNameContentValuesList);
        }
        //Add mPhotoContentValuesList
        if(mPhotoContentValuesList.size() > 0 ){
            contentValuesListMap.put(Photo.CONTENT_ITEM_TYPE, mPhotoContentValuesList);
        }
        //Add mOrganizationContentValuesList
        if(mOrganizationContentValuesList.size() > 0 ){
            contentValuesListMap.put(Organization.CONTENT_ITEM_TYPE, mOrganizationContentValuesList);
        }

        if((mAdapter!=null) && (mListView!=null)){
            int adapterSize = mAdapter.getCount();
            SparseBooleanArray checkStates = mListView.getCheckedItemPositions();
            for(int i = 0; i < adapterSize; i ++){
                if(!checkStates.get(i)) continue;
                final ViewEntry entry = (ViewEntry)mAdapter.getItem(i);
                if(!(entry instanceof DetailViewEntry)) continue;
                ContentValues contentValues = ((DetailViewEntry)entry).contentValues;
                String key = contentValues.getAsString(Data.MIMETYPE);
                if (key != null) {
                    List<ContentValues> contentValuesList =
                            contentValuesListMap.get(key);
                    if (contentValuesList == null) {
                        contentValuesList = new ArrayList<ContentValues>();
                        contentValuesListMap.put(key, contentValuesList);
                    }
                    contentValuesList.add(contentValues);
                }

                //MOT MOD BEGIN - IKPIM-899, send vCard with vCardContentString as EXTRA
                //build vcard content string
                res.append((((DetailViewEntry)entry).kind) + " "  + ((DetailViewEntry)entry).typeString + space);
                if (((DetailViewEntry)entry).mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
                    res.append(PhoneNumberUtilsExt.formatNumber(mContext, ((DetailViewEntry)entry).data));
                } else {
                    res.append(((DetailViewEntry)entry).data);
                }
                res.append(carriageReturn);
                //MOT MOD END - IKPIM-899
            }
        }

        vCardContentString = res.toString(); //MOT MOD - IKPIM-899 send vCard with vCardContentString as EXTRA
        String vcard = null;
        if(contentValuesListMap.size() > 0){
            final VCardComposer composer =
                    new VCardComposer(mContext, VCardConfig.VCARD_TYPE_DEFAULT, false);
            vcard = composer.buildVCard(contentValuesListMap);
        }
        return vcard;
    }

    /**
     * Calls into the contacts provider to get a pre-authorized version of the given URI.
     */
    private Uri getPreAuthorizedUri(Uri uri) {
        Bundle uriBundle = new Bundle();
        uriBundle.putParcelable(ContactsContract.Authorization.KEY_URI_TO_AUTHORIZE, uri);
        Bundle authResponse = mContext.getContentResolver().call(
                ContactsContract.AUTHORITY_URI,
                ContactsContract.Authorization.AUTHORIZATION_METHOD,
                null,
                uriBundle);
        if (authResponse != null) {
            return (Uri) authResponse.getParcelable(
                    ContactsContract.Authorization.KEY_AUTHORIZED_URI);
        } else {
            return uri;
        }
    }
}
