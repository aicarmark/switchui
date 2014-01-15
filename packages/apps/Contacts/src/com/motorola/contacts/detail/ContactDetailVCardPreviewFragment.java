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
import com.android.contacts.util.ContactBadgeUtil;
import com.android.contacts.util.DataStatus;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.widget.TransitionAnimationView;
import com.android.internal.telephony.ITelephony;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.OrganizationData;
import com.android.vcard.VCardEntry.PhoneData;
import com.android.vcard.VCardEntry.PhotoData;
import com.android.vcard.VCardEntry.EmailData;
import com.android.vcard.VCardEntry.PostalData;
import com.android.vcard.VCardEntry.ImData;
import com.android.vcard.VCardEntry.NicknameData;
import com.android.vcard.VCardEntry.NoteData;
import com.android.vcard.VCardEntry.WebsiteData;

import com.motorola.contacts.activities.ContactDetailVCardPreviewActivity;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.BitmapFactory.Options;
// BEGIN Motorola, FTR 36344, IKHSS6-8444
import com.motorola.contacts.util.MEDialer;
// END IKHSS6-8444

public class ContactDetailVCardPreviewFragment extends ContactDetailFragment {

    private static final String TAG = "ContactDetailVCardPreviewFragment";

    private static final String KEY_USER_CHECKED_IDS = "UserCheckedIds";

    private Button mOKButton;
    private Button mCancelButton;

    private VCardEntry mContactStruct = null;
    private List<ContentValues> mNameContentValuesList = new ArrayList<ContentValues>();
    private List<ContentValues> mPhotoContentValuesList = new ArrayList<ContentValues>();
    private List<ContentValues> mOrganizationContentValuesList = new ArrayList<ContentValues>();
    private boolean mIsCreateFromSavedState = false;


    public ContactDetailVCardPreviewFragment() {
        // Explicit constructor for inflation
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mView = inflater.inflate(R.layout.moto_contact_detail_vcard_preview_fragment, container, false);

        mInflater = inflater;

        mStaticPhotoView = (ImageView) mView.findViewById(R.id.photo);

        mListView = (ListView) mView.findViewById(android.R.id.list);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
//        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(true);
        mListView.setOnScrollListener(mVerticalScrollListener);

        // Don't set it to mListView yet.  We do so later when we bind the adapter.
        mEmptyView = mView.findViewById(android.R.id.empty);

        mAlphaLayer = mView.findViewById(R.id.alpha_overlay);
        mTouchInterceptLayer = mView.findViewById(R.id.touch_intercept_overlay);

        mQuickFixButton = (Button) mView.findViewById(R.id.contact_quick_fix);

        mOKButton = (Button) mView.findViewById(R.id.ok);
        mOKButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ContactDetailVCardPreviewActivity)mContext).importVCard();
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

        if (mContactStruct != null) {
            bindData();
        }

        return mView;
    }

    public void previewVCard(VCardEntry contactStruct) {
        if (contactStruct == null ) {
            Log.e(TAG,"contactStruct is null.");
            return;
        }

        mContactStruct = contactStruct;
        bindData();
    }

    protected void bindData() {
        if (mView == null) {
            return;
        }

        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }

        if (mTransitionAnimationRequested) {
            TransitionAnimationView.startAnimation(mView, mContactStruct == null);
            mTransitionAnimationRequested = false;
        }

        if (mContactStruct == null) {
            mView.setVisibility(View.INVISIBLE);
            return;
        }

        // Figure out if the contact has social updates or not
        mContactHasSocialUpdates = false;//social updates is false for vcardpreivew

        // Setup the photo if applicable
        if (mStaticPhotoView != null) {
            // The presence of a static photo view is not sufficient to determine whether or not
            // we should show the photo. Check the mShowStaticPhoto flag which can be set by an
            // outside class depending on screen size, layout, and whether the contact has social
            // updates or not.
            if (mShowStaticPhoto) {
                mStaticPhotoView.setVisibility(View.VISIBLE);
                setPhoto(mStaticPhotoView);
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
            mAdapter = new VcardPreviewAdapter();
            mListView.setAdapter(mAdapter);
        }

        // Restore {@link ListView} state if applicable because the adapter is now populated.
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
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

        //mWritableRawContactIds.clear(); cdxw84 for pass build

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);

        // Build up method entries
        if (mContactStruct == null) {
            return;
        }

        addPhoneEntries();
        addEmailEntries();
        addPostalEntries();
        addIMEntries();
        addNicknameEntries();
        addNoteEntries();
        addWebsiteEntries();
        addEventEntries();
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

//        flattenList(mSipEntries);
        flattenList(mPostalEntries);
        flattenList(mEventEntries);
//        flattenList(mGroupEntries);
//        flattenList(mRelationEntries);
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

    /** set  photo**/
    private void setPhoto(ImageView photoView) {
        //set photo
        List<PhotoData> photoList = mContactStruct.getPhotoList();
        Bitmap bitmap = null;
        if( photoList != null && photoList.size() > 0 ) {
            for (PhotoData photoData : photoList) {
                if(photoData == null || photoData.getBytes() == null) {
                     Log.w(TAG,"photo is null");
                }
                else {
                    Options options = new Options();
                    options.inPurgeable = true;
                    bitmap = BitmapFactory
                        .decodeByteArray(photoData.getBytes(), 0, photoData.getBytes().length, options);
                    break;
                }
              }
        }
        if(bitmap == null){
            //Moto Mod IKHSS6-4499, Config the dare theme by xml
            bitmap = ContactBadgeUtil.loadDefaultAvatarPhoto(mContext, true, mContext.getResources().getBoolean(R.bool.contacts_dark_ui)); //cdxw84, new API for pass build
        }

        photoView.setImageBitmap(bitmap);
    }

    /**
     * Sets the display name of this contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    private void setDisplayName(TextView textView) {
        if (textView == null) {
            return;
        }
        setDataOrHideIfNone((CharSequence)mContactStruct.getDisplayName(), textView);
    }

    /**
     * Sets the company and job title of this contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    private void setCompanyName(TextView textView) {
        if (textView == null) {
            return;
        }

        List<OrganizationData> organizationList = mContactStruct.getOrganizationList();
        if (organizationList == null || organizationList.size() <= 0)
            return;
        OrganizationData orgData = organizationList.get(0);
        final String company = orgData.getOrganizationName();
        final String title = orgData.getTitle();
        final String combined;
        final boolean displayNameIsOrganization = mContactStruct.getDisplayName().equals(company);
        // We need to show company and title in a combined string. However, if the
        // DisplayName is already the organization, it mirrors company or (if company
        // is empty title). Make sure we don't show what's already shown as DisplayName

        if (TextUtils.isEmpty(company)) {
            combined = displayNameIsOrganization ? null : title;
        } else {
            if (TextUtils.isEmpty(title)) {
                combined = displayNameIsOrganization ? null : company;
            } else {
                if (displayNameIsOrganization) {
                    combined = title;
                } else {
                    combined = getResources().getString(
                            R.string.organization_company_and_title,
                            company, title);
                }
            }
        }

        setDataOrHideIfNone((CharSequence)combined, textView);
    }

    /**
     * Helper function to display the given text in the {@link TextView} or
     * hides the {@link TextView} if the text is empty or null.
     */
    private void setDataOrHideIfNone(CharSequence textToDisplay, TextView textView) {
        if (!TextUtils.isEmpty(textToDisplay)) {
            textView.setText(textToDisplay);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText(null);
            textView.setVisibility(View.GONE);
        }
    }

    /**
     * reads phone related data from vcard content and adds it to view
     **/
    private void addPhoneEntries() {
        List<PhoneData> phoneList = mContactStruct.getPhoneList();
        if (phoneList == null || phoneList.size() <= 0)
            return;

        final Context context = mContext;
        final String mimeType = Phone.CONTENT_ITEM_TYPE;

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final DataKind kind = accountTypes.getKindOrFallback(null, null, mimeType);
        if (kind == null) return;

        for (PhoneData phoneData : phoneList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.phoneLabelsGroup);
            entry.data = phoneData.getNumber();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.type = phoneData.getType();
            // get type string
            entry.typeString = "";
            for (EditType type : kind.typeList) {
                if (type.rawValue == entry.type) {
                   if (type.customColumn == null) {
                       // Non-custom type. Get its description from the resource
                       entry.typeString = context.getString(type.labelRes);
                   } else {
                       // Custom type. Read it from the database
                       entry.typeString = phoneData.getLabel();
                   }
                   break;
               }
            }

            //entry.actionIcon = android.R.drawable.sym_action_call; cdxw84 for pass build, now no actionIcon, but not sure what is instead
            final Intent newIntent;
            if (ContactsUtils.haveTwoCards(getActivity().getApplicationContext())) {
                newIntent = new Intent(ContactsUtils.TRANS_DIALPAD);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra("phoneNumber", entry.data);
            } else {
                newIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                        Uri.fromParts(Constants.SCHEME_TEL, entry.data, null));
            }
            final Intent phoneIntent = mHasPhone ? newIntent : null;
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
                entry.secondaryActionIcon = -1;//R.drawable.ic_text_holo_light;
                entry.secondaryActionDescription = R.string.sms;
            } else if (mHasPhone) {
                entry.intent = phoneIntent;
            } else if (mHasSms) {
                entry.intent = smsIntent;
                //entry.actionIcon = R.drawable.sym_action_sms;    cdxw84 for pass build, now no actionIcon, but not sure what is instead
            } else {
                entry.intent = null;
                //entry.actionIcon = -1; cdxw84 for pass build, now no actionIcon, but not sure what is instead
            }

            entry.isPrimary = false;

            mPhoneEntries.add(entry);
        }
    }

/**
     * reads email related data from vcard content and adds it to view
     **/
    private void addEmailEntries() {
        List<EmailData> emailList = mContactStruct.getEmailList();
        if (emailList == null || emailList.size() <= 0)
            return;

        final Context context = mContext;
        final String mimeType = Email.CONTENT_ITEM_TYPE;

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final DataKind kind = accountTypes.getKindOrFallback(null, null, mimeType);
        if (kind == null) return;

        for (EmailData emailData : emailList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.emailLabelsGroup);
            entry.data = emailData.getAddress();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.type = emailData.getType();
            // get type string
            entry.typeString = "";
            for (EditType type : kind.typeList) {
                if (type.rawValue == entry.type) {
                   if (type.customColumn == null) {
                       // Non-custom type. Get its description from the resource
                       entry.typeString = context.getString(type.labelRes);
                   } else {
                       // Custom type. Read it from the database
                       entry.typeString = emailData.getLabel();
                   }
                   break;
               }
            }

            //entry.actionIcon = R.drawable.sym_action_email_holo_light; cdxw84 for pass build, now no actionIcon, but not sure what is instead

            entry.intent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts(Constants.SCHEME_MAILTO, entry.data, null));

            entry.isPrimary = false;
            mEmailEntries.add(entry);
        }
    }

/**
     * reads postal related data from vcard content and adds it to view
     **/
    private void addPostalEntries() {
        List<PostalData> postalList = mContactStruct.getPostalList();
        if ( postalList == null || postalList.size() <= 0)
            return;

        final Context context = mContext;
        final String mimeType = StructuredPostal.CONTENT_ITEM_TYPE;

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final DataKind kind = accountTypes.getKindOrFallback(null, null, mimeType);
        if (kind == null) return;

        for (PostalData postalData : postalList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.postalLabelsGroup);
            entry.data = postalData.getFormattedAddress(VCardConfig.VCARD_TYPE_V30_GENERIC);
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.type = postalData.getType();
            // get type string
            entry.typeString = "";
            for (EditType type : kind.typeList) {
                if (type.rawValue == entry.type) {
                   if (type.customColumn == null) {
                       // Non-custom type. Get its description from the resource
                       entry.typeString = context.getString(type.labelRes);
                   } else {
                       // Custom type. Read it from the database
                       entry.typeString = postalData.getLabel();
                   }
                   break;
               }
            }

            //entry.actionIcon = R.drawable.sym_action_show_map_holo_light; cdxw84 for pass build, now no actionIcon, but not sure what is instead

            entry.maxLines = 4;
            entry.intent = new Intent(Intent.ACTION_VIEW, entry.uri);

            entry.isPrimary = false;
            mPostalEntries.add(entry);
        }
    }

/**
     * reads IM related data from vcard content and adds it to view
     **/
    private void addIMEntries() {
        List<ImData> imList = mContactStruct.getImList();
        if ( imList == null || imList.size() <= 0)
             return;

        final Context context = mContext;
        final String mimeType = Im.CONTENT_ITEM_TYPE;

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final DataKind kind = accountTypes.getKindOrFallback(null, null, mimeType);
        if (kind == null) return;

        for (ImData imData : imList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.imLabelsGroup);
            entry.data = imData.getAddress();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.type = imData.getProtocol();
            // get type string
            entry.typeString = "";
            for (EditType type : kind.typeList) {
                if (type.rawValue == entry.type) {
                   if (type.customColumn == null) {
                       // Non-custom type. Get its description from the resource
                       entry.typeString = context.getString(type.labelRes);
                   } else {
                       // Custom type. Read it from the database
                       entry.typeString = imData.getCustomProtocol();
                   }
                   break;
               }
            }

            //entry.actionIcon = R.drawable.sym_action_talk_holo_light; cdxw84 for pass build, now no actionIcon, but not sure what is instead


            String host = null;
            if (imData.getProtocol() != Im.PROTOCOL_CUSTOM) {
                // Try bringing in a well-known host for specific protocols
                host = ContactsUtils.lookupProviderNameFromId(imData.getProtocol());
            }
            if (host != null && !TextUtils.isEmpty(host) && !TextUtils.isEmpty(entry.data)) {
                final String authority = host.toLowerCase();
                final Uri imUri = new Uri.Builder().scheme(Constants.SCHEME_IMTO).authority(
                        authority).appendPath(entry.data).build();
                entry.intent = new Intent(Intent.ACTION_SENDTO, imUri);
            }
            else {
                entry.intent = null;
            }

            entry.isPrimary = false;
            mImEntries.add(entry);
        }
    }

/**
     * reads email related data from vcard content and adds it to view
     **/
    private void addNicknameEntries() {
        List<NicknameData> nickNameList = mContactStruct.getNickNameList();
        if (nickNameList == null || nickNameList.size() <= 0)
             return;

        final Context context = mContext;
        final String mimeType = Nickname.CONTENT_ITEM_TYPE;

        for (NicknameData nickName : nickNameList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.nicknameLabelsGroup);
            entry.data = nickName.getNickname();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.typeString = "";
            //entry.actionIcon = -1; cdxw84 for pass build, now no actionIcon, but not sure what is instead

            entry.intent = null;

            entry.isPrimary = false;
            mNicknameEntries.add(entry);
        }
    }

/**
     * reads note related data from vcard content and adds it to view
     **/
    private void addNoteEntries() {
        List<NoteData> noteList = mContactStruct.getNotes();
        if (noteList == null || noteList.size() <= 0)
            return;


        final Context context = mContext;
        final String mimeType = Note.CONTENT_ITEM_TYPE;

        for (NoteData  note : noteList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.label_notes);
            entry.data = note.getNote();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.typeString = "";
            //entry.actionIcon = -1; cdxw84 for pass build, now no actionIcon, but not sure what is instead

            entry.intent = null;
            entry.maxLines = 100;
            entry.isPrimary = false;
            mNoteEntries.add(entry);
        }
    }

/**
     * reads website related data from vcard content and adds it to view
     **/
    private void addWebsiteEntries() {
        List<WebsiteData> websiteList = mContactStruct.getWebsiteList();
        if (websiteList == null || websiteList.size() <= 0)
            return;


        final Context context = mContext;
        final String mimeType = Website.CONTENT_ITEM_TYPE;

        for (WebsiteData  website : websiteList) {

            final DetailViewEntry entry = new DetailViewEntry();
            entry.context = context;
            entry.id = 0;
            entry.uri = null;
            entry.mimetype = mimeType;
            entry.kind = context.getString(R.string.websiteLabelsGroup);
            entry.data = website.getWebsite();
            final boolean hasData = !TextUtils.isEmpty(entry.data);
            if(!hasData) continue;

            entry.typeString = "";
            //entry.actionIcon = R.drawable.sym_action_goto_website_holo_light; cdxw84 for pass build, now no actionIcon, but not sure what is instead

            entry.maxLines = 10;
            try {
                    WebAddress webAddress = new WebAddress(entry.data);
                    entry.intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(webAddress.toString()));
                } catch (ParseException e) {
                    Log.e(TAG, "Couldn't parse website: " + entry.data);
                }
            entry.isPrimary = false;
            mWebsiteEntries.add(entry);
        }
    }

/**
     * reads event related data from vcard content and adds it to view
     **/
    private void addEventEntries() {
        String birthday = mContactStruct.getBirthday();
        if (birthday == null )
            return;

        final Context context = mContext;
        final String mimeType = Event.CONTENT_ITEM_TYPE;

        final DetailViewEntry entry = new DetailViewEntry();
        entry.context = context;
        entry.id = 0;
        entry.uri = null;
        entry.mimetype = mimeType;
        entry.kind = context.getString(R.string.eventLabelsGroup);
        entry.data = DateUtils.formatDate(context, birthday);
        final boolean hasData = !TextUtils.isEmpty(entry.data);
        if(!hasData) return;

        entry.typeString = context.getString(R.string.eventTypeBirthday);
        //entry.actionIcon =-1; cdxw84 for pass build, now no actionIcon, but not sure what is instead

        mEventEntries.add(entry);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        //no context menu for vcard preview
    }

    private final class VcardPreviewAdapter extends ContactDetailFragment.ViewAdapter {

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

            setDisplayName(viewCache.displayNameView);
            setCompanyName(viewCache.companyView);

            // Set the photo if it should be displayed
            if (viewCache.photoView != null) {
                setPhoto(viewCache.photoView);
            }

            // Set the starred state if it should be displayed
            final CheckBox favoritesStar = viewCache.starredView;
            if (favoritesStar != null) {
                favoritesStar.setVisibility(View.GONE);
            }

            return result;
        }

    }

}
