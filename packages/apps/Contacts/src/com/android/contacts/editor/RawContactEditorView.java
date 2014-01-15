/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.editor;

import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.android.internal.util.Objects;
import com.motorola.android.telephony.PhoneModeManager;

import android.content.Context;
import android.content.Entity;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;

/**
 * Custom view that provides all the editor interaction for a specific
 * {@link Contacts} represented through an {@link EntityDelta}. Callers can
 * reuse this view and quickly rebuild its contents through
 * {@link #setState(EntityDelta, AccountType, ViewIdGenerator)}.
 * <p>
 * Internal updates are performed against {@link ValuesDelta} so that the
 * source {@link Entity} can be swapped out. Any state-based changes, such as
 * adding {@link Data} rows or changing {@link EditType}, are performed through
 * {@link EntityModifier} to ensure that {@link AccountType} are enforced.
 */
public class RawContactEditorView extends BaseRawContactEditorView {
	static final String TAG = "RawContactEditorView";
	
    private LayoutInflater mInflater;

    private View mPhotoStub;
    private StructuredNameEditorView mName;
    private PhoneticNameEditorView mPhoneticName;
    private GroupMembershipView mGroupMembershipView;

    private ViewGroup mFields;

    private ImageView mAccountIcon;
    private TextView mAccountTypeTextView;
    private TextView mAccountNameTextView;

    private Button mAddFieldButton;

    private long mRawContactId = -1;
    private boolean mAutoAddToDefaultGroup = true;
    private Cursor mGroupMetaData;
    private DataKind mGroupMembershipKind;
    private EntityDelta mState;

    private boolean mPhoneticNameAdded;
    
    /*2012-12-31, add by amt_sunzhao for SWITCHUITWO-388 */ 
    private PopupMenu mPopupMenu = null;
    /*2012-12-31, add end*/ 

    public RawContactEditorView(Context context) {
        super(context);
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        View view = getPhotoEditor();
        if (view != null) {
            view.setEnabled(enabled);
        }

        if (mName != null) {
            mName.setEnabled(enabled);
        }

        if (mPhoneticName != null) {
            mPhoneticName.setEnabled(enabled);
        }

        if (mFields != null) {
            int count = mFields.getChildCount();
            for (int i = 0; i < count; i++) {
                mFields.getChildAt(i).setEnabled(enabled);
            }
        }

        if (mGroupMembershipView != null) {
            mGroupMembershipView.setEnabled(enabled);
        }

        mAddFieldButton.setEnabled(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPhotoStub = findViewById(R.id.stub_photo);
        mName = (StructuredNameEditorView)findViewById(R.id.edit_name);
        mName.setDeletable(false);

        mPhoneticName = (PhoneticNameEditorView)findViewById(R.id.edit_phonetic_name);
        mPhoneticName.setDeletable(false);

        mFields = (ViewGroup)findViewById(R.id.sect_fields);

        mAccountIcon = (ImageView) findViewById(R.id.account_icon);
        mAccountTypeTextView = (TextView) findViewById(R.id.account_type);
        mAccountNameTextView = (TextView) findViewById(R.id.account_name);

        mAddFieldButton = (Button) findViewById(R.id.button_add_field);
        mAddFieldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddInformationPopupWindow();
            }
        });
    }

    /**
     * Set the internal state for this view, given a current
     * {@link EntityDelta} state and the {@link AccountType} that
     * apply to that state.
     */
    @Override
    public void setState(EntityDelta state, AccountType type, ViewIdGenerator vig,
            boolean isProfile) {

        mState = state;

        // Remove any existing sections
        mFields.removeAllViews();

        // Bail if invalid state or account type
        if (state == null || type == null) return;

        setId(vig.getId(state, null, null, ViewIdGenerator.NO_VIEW_INDEX));

        // Make sure we have a StructuredName and Organization
        EntityModifier.ensureKindExists(state, type, StructuredName.CONTENT_ITEM_TYPE);
        // MOT CHINA - Not show Organization in editor by default
        // EntityModifier.ensureKindExists(state, type, Organization.CONTENT_ITEM_TYPE);

        ValuesDelta values = state.getValues();
        mRawContactId = values.getAsLong(RawContacts._ID);
        
        String accountTypeAsc = values.getAsString(RawContacts.ACCOUNT_TYPE);        

        // Fill in the account info
        if (isProfile) {
            String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
            if (TextUtils.isEmpty(accountName)) {
                mAccountNameTextView.setVisibility(View.GONE);
                mAccountTypeTextView.setText(R.string.local_profile_title);
            } else {
                CharSequence accountType = type.getDisplayLabel(mContext);
                if (HardCodedSources.ACCOUNT_LOCAL_DEVICE.equals(accountName)) {
                    accountType = mContext.getString(R.string.local_name);
                    accountName = mContext.getString(R.string.account_local_device);
                }
                mAccountTypeTextView.setText(mContext.getString(R.string.external_profile_title,
                        accountType));
                mAccountNameTextView.setText(accountName);
            }
        } else {
            String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
            CharSequence accountType = type.getDisplayLabel(mContext);
            if (TextUtils.isEmpty(accountType)) {
                accountType = mContext.getString(R.string.account_phone);
            }
            if (HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(accountTypeAsc)) {
                mAccountNameTextView.setVisibility(View.VISIBLE);
                mAccountNameTextView.setText(mContext.getString(R.string.account_local_device));
                mAccountTypeTextView.setText(mContext.getString(R.string.local_name));
            } else if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
                mAccountNameTextView.setVisibility(View.VISIBLE);
                int phoneType, str_id, str_id_cap;
                phoneType = SimUtility.getTypeByAccountName(accountName);
                if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                    if (PhoneModeManager.isDmds()) {
                        str_id_cap = R.string.account_card_C_cap;
                        str_id = R.string.account_card_C;
                    } else {
                        str_id_cap = R.string.account_card_uim_cap;
                        str_id = R.string.account_card_uim;
                    }
                } else {
                    str_id_cap = R.string.account_card_G_cap;
                    str_id = R.string.account_card_G;
                }
                final int free = SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType);
                final int capacity = SimUtility.getCapacity(mContext.getContentResolver(), phoneType);
                int used = capacity - free;
                if (capacity <= 0 || used < 0 ) {
                    mAccountNameTextView.setText(mContext.getString(str_id));
                } else {
                    mAccountNameTextView.setText(mContext.getString(str_id_cap, used, capacity));
                }
                mAccountTypeTextView.setText(accountType);
            } else {
                if (!TextUtils.isEmpty(accountName)) {
                    mAccountNameTextView.setVisibility(View.VISIBLE);
                    mAccountNameTextView.setText(
                            mContext.getString(R.string.from_account_format, accountName));
                } else {
                    // Hide this view so the other text view will be centered vertically
                    mAccountNameTextView.setVisibility(View.GONE);
                }
                mAccountTypeTextView.setText(
                        mContext.getString(R.string.account_type_format, accountType));
            }
        }
        mAccountIcon.setImageDrawable(type.getDisplayIcon(mContext));
        if (PhoneModeManager.isDmds()) {
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
                final String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
                final int phoneType = SimUtility.getTypeByAccountName(accountName);
                int iconRes;
                if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                    iconRes = R.drawable.ic_launcher_card_c;
                } else {
                    iconRes = R.drawable.ic_launcher_card_g;
                }
                mAccountIcon.setImageDrawable(mContext.getResources().getDrawable(iconRes));
            }
        }

        // Show photo editor when supported
        // Log.v(TAG, "accountTypeAsc = "+accountTypeAsc+", PhotoEditor = "+getPhotoEditor()+", getPhotoType = "+type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE));
        EntityModifier.ensureKindExists(state, type, Photo.CONTENT_ITEM_TYPE);
        if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {       
        	// card accounts don't support photo 	       
        	if (type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) == null) {
        	    mPhotoStub.setVisibility(View.GONE);	
        	}        	        	            
            //setHasPhotoEditor(false);
            //getPhotoEditor().setEnabled(false);
        } else {
            setHasPhotoEditor((type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null));
            getPhotoEditor().setEnabled(isEnabled());        	
        }
        // Log.v(TAG, "mName="+mName+", isEnabled = "+isEnabled()+", PhotoEditor = "+getPhotoEditor());
        mName.setEnabled(isEnabled());

        mPhoneticName.setEnabled(isEnabled());

        // Show and hide the appropriate views
        mFields.setVisibility(View.VISIBLE);
        mName.setVisibility(View.VISIBLE);
        mPhoneticName.setVisibility(View.VISIBLE);

        mGroupMembershipKind = type.getKindForMimetype(GroupMembership.CONTENT_ITEM_TYPE);
        if (mGroupMembershipKind != null && isProfile == false) {
            mGroupMembershipView = (GroupMembershipView)mInflater.inflate(
                    R.layout.item_group_membership, mFields, false);
            mGroupMembershipView.setKind(mGroupMembershipKind);
            mGroupMembershipView.setEnabled(isEnabled());
        }

        // Create editor sections for each possible data kind
        for (DataKind kind : type.getSortedDataKinds()) {
            // Skip kind of not editable
            if (!kind.editable) continue;

            final String mimeType = kind.mimeType;
            // Log.v(TAG, "mimeType = "+mimeType+", kind = "+kind);
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for structured name
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                mName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME),
                        primary, state, false, vig);
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
                    // Ensure name doesn't exeed max sim name length
                    int originalNameLength = 0;
                    int max_length = 0;
                    if (primary != null) {
                        String originalName = primary.getAsString(StructuredName.DISPLAY_NAME);
                        if (originalName != null) {
                            originalNameLength = originalName.length();
                            if (SimUtility.isEngName(originalName))
                                max_length = SimUtility.SIM_NAME_LENGTH_ENG;
                            else
                                max_length = SimUtility.SIM_NAME_LENGTH_CHN;
                            // Do not trunk because it may fail CTA, some CTA SIM has longer name
                            // ValuesDelta simEntry = primary;
                            // simEntry.put(StructuredName.DISPLAY_NAME, originalName.substring(0, max_length));
                            // mName.setValues(
                            //        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME),
                            //        simEntry, state, false, vig);
                        }
                    }

                    // Add listener to ensure input name can not exceed max sim name length
                    // But only add it when original length is less than max_length because some CTA SIM has longer name
                    if (originalNameLength <= max_length) {
                        final EntityDelta fState = state;
                        final ViewIdGenerator fVig= vig;
                        final DataKind fKind = type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME);
                        EditorListener nameListener = new EditorListener() {
                            String inputName;
                            ValuesDelta entry = primary;
                            @Override
                            public void onRequest(int request) {
                                if (request == EditorListener.FIELD_CHANGED) {
                                    inputName = mName.getValues().getAsString(StructuredName.DISPLAY_NAME);
                                    int max_length = 0;
                                    if (SimUtility.isEngName(inputName))
                                        max_length = SimUtility.SIM_NAME_LENGTH_ENG;
                                    else
                                        max_length = SimUtility.SIM_NAME_LENGTH_CHN;
                                    if (inputName.length() > max_length) {
                                        Toast.makeText(mContext, R.string.sim_name_length_error, Toast.LENGTH_SHORT).show();
                                        entry.put(StructuredName.DISPLAY_NAME, inputName.substring(0, max_length));
                                        mName.setValues(fKind, entry, fState, false, fVig);
                                    }
                                }
                            }

                            @Override
                            public void onDeleteRequested(Editor removedEditor) {
                            }
                        };
                        mName.setEditorListener(nameListener);
                    }
                }
                mPhoneticName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME),
                        primary, state, false, vig);
            } else if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
            	  // Log.v(TAG, "mimeType = Photo, getPhotoEditor = "+getPhotoEditor());
                // Handle special case editor for photos
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                getPhotoEditor().setValues(kind, primary, state, false, vig);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                if (mGroupMembershipView != null) {
                    mGroupMembershipView.setState(state);
                }
/* MOT CHINA - Not show Organization in editor by default
            } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Create the organization section
                final KindSectionView section = (KindSectionView) mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                section.setTitleVisible(false);
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);

                // If there is organization info for the contact already, display it
                if (!section.isEmpty()) {
                    mFields.addView(section);
                } else {
                    // Otherwise provide the user with an "add organization" button that shows the
                    // EditText fields only when clicked
                    final View organizationView = mInflater.inflate(
                            R.layout.organization_editor_view_switcher, mFields, false);
                    final View addOrganizationButton = organizationView.findViewById(
                            R.id.add_organization_button);
                    final ViewGroup organizationSectionViewContainer =
                            (ViewGroup) organizationView.findViewById(R.id.container);

                    organizationSectionViewContainer.addView(section);

                    // Setup the click listener for the "add organization" button
                    addOrganizationButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Once the user expands the organization field, the user cannot
                            // collapse them again.
                            addOrganizationButton.setVisibility(View.GONE);
                            organizationSectionViewContainer.setVisibility(View.VISIBLE);
                            organizationSectionViewContainer.requestFocus();
                        }
                    });

                    mFields.addView(organizationView);
                }
*/
            } else {
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
                    String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
                    final int phoneType = SimUtility.getTypeByAccountName(accountName);
                    if (!SimUtility.isUSIMType(phoneType)) {
                        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            kind.typeOverallMax = 1;
                        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            continue;
                        }
                    }
                }
                // Otherwise use generic section-based editors
                if (kind.fieldList == null) continue;
                final KindSectionView section = (KindSectionView)mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
                    if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        section.setCardEditor(true, section, Phone.CONTENT_ITEM_TYPE, Phone.NUMBER,
                            SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH, R.string.sim_phone_number_length_error);
                    }  else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        section.setCardEditor(true, section, Email.CONTENT_ITEM_TYPE, Email.ADDRESS,
                            SimUtility.MAX_SIM_EMAIL_LENGTH, R.string.sim_email_length_error);
                    }
                }
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);
                mFields.addView(section);
            }
        }

        if (mGroupMembershipView != null) {
            mFields.addView(mGroupMembershipView);
        }

        updatePhoneticNameVisibility();

        addToDefaultGroupIfNeeded();

        if(!hasNewField()){
            mAddFieldButton.setEnabled(false);
        } else {
            mAddFieldButton.setEnabled(isEnabled());
        }
                            
        // hide 'Add other field" for cards accounts
        if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountTypeAsc)) {
            mAddFieldButton.setVisibility(View.GONE);
        }                
                
    }

    @Override
    public void setGroupMetaData(Cursor groupMetaData) {
        mGroupMetaData = groupMetaData;
        addToDefaultGroupIfNeeded();
        if (mGroupMembershipView != null) {
            mGroupMembershipView.setGroupMetaData(groupMetaData);
        }
    }

    public void setAutoAddToDefaultGroup(boolean flag) {
        this.mAutoAddToDefaultGroup = flag;
    }

    /**
     * If automatic addition to the default group was requested (see
     * {@link #setAutoAddToDefaultGroup}, checks if the raw contact is in any
     * group and if it is not adds it to the default group (in case of Google
     * contacts that's "My Contacts").
     */
    private void addToDefaultGroupIfNeeded() {
        if (!mAutoAddToDefaultGroup || mGroupMetaData == null || mGroupMetaData.isClosed()
                || mState == null) {
            return;
        }

        boolean hasGroupMembership = false;
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta values : entries) {
                Long id = values.getAsLong(GroupMembership.GROUP_ROW_ID);
                if (id != null && id.longValue() != 0) {
                    hasGroupMembership = true;
                    break;
                }
            }
        }

        if (!hasGroupMembership) {
            long defaultGroupId = getDefaultGroupId();
            if (defaultGroupId != -1) {
                ValuesDelta entry = EntityModifier.insertChild(mState, mGroupMembershipKind);
                entry.put(GroupMembership.GROUP_ROW_ID, defaultGroupId);
            }
        }
    }

    /**
     * Returns the default group (e.g. "My Contacts") for the current raw contact's
     * account.  Returns -1 if there is no such group.
     */
    private long getDefaultGroupId() {
        String accountType = mState.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        String accountName = mState.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        String accountDataSet = mState.getValues().getAsString(RawContacts.DATA_SET);
        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
            String name = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            String type = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
            if (name.equals(accountName) && type.equals(accountType)
                    && Objects.equal(dataSet, accountDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (!mGroupMetaData.isNull(GroupMetaDataLoader.AUTO_ADD)
                            && mGroupMetaData.getInt(GroupMetaDataLoader.AUTO_ADD) != 0) {
                    return groupId;
                }
            }
        }
        return -1;
    }

    public TextFieldsEditorView getNameEditor() {
        return mName;
    }

    public TextFieldsEditorView getPhoneticNameEditor() {
        return mPhoneticName;
    }

    // MOT CHINA
    public ViewGroup getFieldViews() {
        return mFields;
    }

    private void updatePhoneticNameVisibility() {
        boolean showByDefault =
                getContext().getResources().getBoolean(R.bool.config_editor_include_phonetic_name);

        if (showByDefault || mPhoneticName.hasData() || mPhoneticNameAdded) {
            mPhoneticName.setVisibility(View.VISIBLE);
            Log.v(TAG, "show mPhoneticName");
        } else {
            mPhoneticName.setVisibility(View.GONE);
            Log.v(TAG, "hide mPhoneticName");
        }
    }

    @Override
    public long getRawContactId() {
        return mRawContactId;
    }

    private boolean hasNewField(){
        boolean bHasNewField = false;
        for (int i = 0; i < mFields.getChildCount(); i++) {
            View child = mFields.getChildAt(i);
            if (child instanceof KindSectionView) {
                final KindSectionView sectionView = (KindSectionView) child;
                // If the section is already visible (has 1 or more editors), then don't offer the
                // option to add this type of field in the popup menu
                if (sectionView.getEditorCount() > 0) {
                    continue;
                }
                DataKind kind = sectionView.getKind();
                // not a list and already exists? ignore
                if ((kind.typeOverallMax == 1) && sectionView.getEditorCount() != 0) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(kind.mimeType)) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(kind.mimeType)
                        && mPhoneticName.getVisibility() == View.VISIBLE) {
                    continue;
                }
                bHasNewField = true;
                break;
            }
        }
        return bHasNewField;
    }

    private void showAddInformationPopupWindow() {
        final ArrayList<KindSectionView> fields =
                new ArrayList<KindSectionView>(mFields.getChildCount());

        /*2012-12-31, add by amt_sunzhao for SWITCHUITWO-388 */ 
        mPopupMenu = new PopupMenu(getContext(), mAddFieldButton);
        final Menu menu = mPopupMenu.getMenu();
        for (int i = 0; i < mFields.getChildCount(); i++) {
            View child = mFields.getChildAt(i);
            if (child instanceof KindSectionView) {
                final KindSectionView sectionView = (KindSectionView) child;
                // If the section is already visible (has 1 or more editors), then don't offer the
                // option to add this type of field in the popup menu
                if (sectionView.getEditorCount() > 0) {
                    continue;
                }
                DataKind kind = sectionView.getKind();
                // not a list and already exists? ignore
                if ((kind.typeOverallMax == 1) && sectionView.getEditorCount() != 0) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(kind.mimeType)) {
                    continue;
                }

                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(kind.mimeType)
                        && mPhoneticName.getVisibility() == View.VISIBLE) {
                    continue;
                }

                menu.add(Menu.NONE, fields.size(), Menu.NONE, sectionView.getTitle());
                fields.add(sectionView);
            }
        }

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final KindSectionView view = fields.get(item.getItemId());
                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(view.getKind().mimeType)) {
                    mPhoneticNameAdded = true;
                    updatePhoneticNameVisibility();
                    mPhoneticName.requestFocus();//mtdr83 add for IKCBSMMCPPRC-1592
                } else {
                    view.addItem();
                }
                if(!hasNewField()){
                    mAddFieldButton.setEnabled(false);
                }
                return true;
            }
        });

        mPopupMenu.show();
    }
    
    public void onStop() {
    	if(null != mPopupMenu) {
    		mPopupMenu.dismiss();
    	}
    }
    /*2012-12-31, add end*/ 
}
