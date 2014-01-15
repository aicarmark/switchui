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

package com.android.contacts.editor;

import com.android.contacts.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.ContactEditorAccountsChangedActivity;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.editor.AggregationSuggestionEngine.Suggestion;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.model.GoogleAccountType;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.preference.CT189PreferenceFragment.Prefs;
import com.android.contacts.SimUtility;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.PhoneCapabilityTester;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
import com.motorola.contacts.preference.ContactPreferenceUtilities;
//<!-- MOTOROLA MOD End of IKPIM-491 -->

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
//MOTO MOD BEGIN
import android.provider.ContactsContract.CommonDataKinds.Photo;
//MOTO MOD END
import com.android.contacts.RomUtility;

public class ContactEditorFragment extends Fragment implements
        SplitContactConfirmationDialogFragment.Listener, SelectAccountDialogFragment.Listener,
        AggregationSuggestionEngine.Listener, AggregationSuggestionView.Listener,
        RawContactReadOnlyEditorView.Listener {

    private static final String TAG = "ContactEditorFragment";
    private static final boolean DEBUG = true;
    private static final int LOADER_DATA = 1;
    private static final int LOADER_GROUPS = 2;

    private static final String KEY_URI = "uri";
    private static final String KEY_ACTION = "action";
    private static final String KEY_EDIT_STATE = "state";
    private static final String KEY_RAW_CONTACT_ID_REQUESTING_PHOTO = "photorequester";
    private static final String KEY_VIEW_ID_GENERATOR = "viewidgenerator";
    private static final String KEY_CURRENT_PHOTO_FILE = "currentphotofile";
    private static final String KEY_CONTACT_ID_FOR_JOIN = "contactidforjoin";
    private static final String KEY_CONTACT_WRITABLE_FOR_JOIN = "contactwritableforjoin";
    private static final String KEY_SHOW_JOIN_SUGGESTIONS = "showJoinSuggestions";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NEW_LOCAL_PROFILE = "newLocalProfile";
    private static final String KEY_IS_USER_PROFILE = "isUserProfile";
    //MOTO MOD BEGIN , add to indicate the activity's status
    private boolean isDestroy = false;
    //MOTO MOD END
    public static final String SAVE_MODE_EXTRA_KEY = "saveMode";

    private boolean bOrientation = false;
    /**
     * An intent extra that forces the editor to add the edited contact
     * to the default group (e.g. "My Contacts").
     */
    public static final String INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY = "addToDefaultDirectory";

    public static final String INTENT_EXTRA_NEW_LOCAL_PROFILE = "newLocalProfile";

    public static final String INTENT_EXTRA_EXCLUDE_SIMCARD = "excludesimcardaccount";

    /**
     * Modes that specify what the AsyncTask has to perform after saving
     */
    // TODO: Move this into a common utils class or the save service because the contact and
    // group editors need to use this interface definition
    public interface SaveMode {
        /**
         * Close the editor after saving
         */
        public static final int CLOSE = 0;

        /**
         * Reload the data so that the user can continue editing
         */
        public static final int RELOAD = 1;

        /**
         * Split the contact after saving
         */
        public static final int SPLIT = 2;

        /**
         * Join another contact after saving
         */
        public static final int JOIN = 3;

        /**
         * Navigate to Contacts Home activity after saving.
         */
        public static final int HOME = 4;
    }

    private interface Status {
        /**
         * The loader is fetching data
         */
        public static final int LOADING = 0;

        /**
         * Not currently busy. We are waiting for the user to enter data
         */
        public static final int EDITING = 1;

        /**
         * The data is currently being saved. This is used to prevent more
         * auto-saves (they shouldn't overlap)
         */
        public static final int SAVING = 2;

        /**
         * Prevents any more saves. This is used if in the following cases:
         * - After Save/Close
         * - After Revert
         * - After the user has accepted an edit suggestion
         */
        public static final int CLOSING = 3;

        /**
         * Prevents saving while running a child activity.
         */
        public static final int SUB_ACTIVITY = 4;
    }

    private static final int REQUEST_CODE_JOIN = 0;
    private static final int REQUEST_CODE_CAMERA_WITH_DATA = 1;
    private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 2;
    private static final int REQUEST_CODE_ACCOUNTS_CHANGED = 3;

    private Bitmap mPhoto = null;
    private long mRawContactIdRequestingPhoto = -1;
    private long mRawContactIdRequestingPhotoAfterLoad = -1;

    private final EntityDeltaComparator mComparator = new EntityDeltaComparator();

    private static final File PHOTO_DIR = new File(
            Environment.getExternalStorageDirectory() + "/DCIM/Camera");

    private Cursor mGroupMetaData;

    private File mCurrentPhotoFile;

    // Height/width (in pixels) to request for the photo - queried from the provider.
    private int mPhotoPickSize;

    private Context mContext;
    private String mAction;
    private Uri mLookupUri;
    private Bundle mIntentExtras;
    private Listener mListener;

    private long mContactIdForJoin;
    private boolean mContactWritableForJoin;

    private ContactEditorUtils mEditorUtils;

    private LinearLayout mContent;
    private EntityDeltaList mState;

    private ViewIdGenerator mViewIdGenerator;

    private long mLoaderStartTime;

    private int mStatus;

    private SharedPreferences mPrefs;
    private boolean mAdd189 = false;
    private boolean mIs189FlexEnable = false;
    private int mSelection = 0;

    private AggregationSuggestionEngine mAggregationSuggestionEngine;
    private long mAggregationSuggestionsRawContactId;
    private View mAggregationSuggestionView;

    private ListPopupWindow mAggregationSuggestionPopup;
    /*Modifyed for SWITCHUI-43*/
    private String mCropImageFile;
    private static final String PHOTO_DATE_FORMAT = "'IMG'_yyyyMMdd_HHmmss";
    /*Modifyed for SWITCHUI-43 end*/
    private static final class AggregationSuggestionAdapter extends BaseAdapter {
        private final Activity mActivity;
        private final boolean mSetNewContact;
        private final AggregationSuggestionView.Listener mListener;
        private final List<Suggestion> mSuggestions;

        public AggregationSuggestionAdapter(Activity activity, boolean setNewContact,
                AggregationSuggestionView.Listener listener, List<Suggestion> suggestions) {
            mActivity = activity;
            mSetNewContact = setNewContact;
            mListener = listener;
            mSuggestions = suggestions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Suggestion suggestion = (Suggestion) getItem(position);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            AggregationSuggestionView suggestionView =
                    (AggregationSuggestionView) inflater.inflate(
                            R.layout.aggregation_suggestions_item, null);
            suggestionView.setNewContact(mSetNewContact);
            suggestionView.setListener(mListener);
            suggestionView.bindSuggestion(suggestion);
            return suggestionView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mSuggestions.get(position);
        }

        @Override
        public int getCount() {
            return mSuggestions.size();
        }
    }

    private OnItemClickListener mAggregationSuggestionItemClickListener =
            new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AggregationSuggestionView suggestionView = (AggregationSuggestionView) view;
            suggestionView.handleItemClickEvent();
            mAggregationSuggestionPopup.dismiss();
            mAggregationSuggestionPopup = null;
        }
    };

    private boolean mAutoAddToDefaultGroup;

    private boolean mEnabled = true;
    private boolean mRequestFocus;
    private boolean mNewLocalProfile = false;
    private boolean mIsUserProfile = false;
    private boolean mExcludeSimCard = false;

    private boolean mIsChecking = false;
    private AlertDialog mAlert = null;
    private long mCardRawContactId = -1;

    public ContactEditorFragment() {
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            if (mContent != null) {
                int count = mContent.getChildCount();
                for (int i = 0; i < count; i++) {
                    mContent.getChildAt(i).setEnabled(enabled);
                }
            }
            setAggregationSuggestionViewEnabled(enabled);
            final Activity activity = getActivity();
            if (activity != null) activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mEditorUtils = ContactEditorUtils.getInstance(mContext);
        mIs189FlexEnable = ContactsUtils.isCT189EmailEnabled(mContext);
        loadPhotoPickSize();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAlert != null) {
            mAlert.dismiss();
            // BEGIN Motorola, ODC_001639, 2013-01-17, SWITCHUITWO-541,to avoid when continuous locking and unlocking the prompt box disappears
            mIsChecking = false;
            // END SWITCHUITWO-541
        }
        if (mAggregationSuggestionEngine != null) {
            mAggregationSuggestionEngine.quit();
        }
        mAggregationSuggestionEngine = null;

        // If anything was left unsaved, save it now but keep the editor open.
        if (!getActivity().isChangingConfigurations() && mStatus == Status.EDITING) {
            save(SaveMode.RELOAD);
        }
        /*2012-12-31, add by amt_sunzhao for SWITCHUITWO-388 */ 
        if(null != mContent) {
        	final int count = mContent.getChildCount();
        	for(int i = 0; i < count; i++) {
        		final View child= mContent.getChildAt(i);
        		if((null != child) 
        				&& (child instanceof RawContactEditorView)) {
        			((RawContactEditorView)child).onStop();
        		}
        		/*2013-1-4, add by amt_sunzhao for SWITCHUITWO-441 */ 
        		if((null != child)
        				&& (child instanceof BaseRawContactEditorView)) {
        			BaseRawContactEditorView editor = (BaseRawContactEditorView)child;
        			PhotoEditorView photo = editor.getPhotoEditor();
        			if(null != photo) {
        				EditorListener listener = photo.getmListener();
        				if((null != listener)
        						&& (listener instanceof PhotoEditorListener)) {
        					PhotoEditorListener photoListener = (PhotoEditorListener)listener;
        					ListPopupWindow popup = photoListener.getmListPopupWindow();
        					if(null != popup) {
        						popup.dismiss();
        					}
        				}
        			}
        		}
        		/*2013-1-4, add end*/ 
        	}
        }
        /*2012-12-31, add end*/ 
        
        // BEGIN Motorola, ODC_001639, 2013-01-17, SWITCHUITWO-534
        if(mAccountSelectPopup != null){
        	mAccountSelectPopup.dismiss();
        	mAccountSelectPopup = null;
        }
        //// END SWITCHUITWO-534
    }

    private ListPopupWindow mAccountSelectPopup;// Motorola, ODC_001639, 2013-01-17, SWITCHUITWO-534
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        final View view = inflater.inflate(R.layout.contact_editor_fragment, container, false);

        mContent = (LinearLayout) view.findViewById(R.id.editors);

        setHasOptionsMenu(true);

        // If we are in an orientation change, we already have mState (it was loaded by onCreate)
        if (mState != null) {
            bindEditors();
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Handle initial actions only when existing state missing
        final boolean hasIncomingState = savedInstanceState != null;

        if (!hasIncomingState) {
            if (Intent.ACTION_EDIT.equals(mAction)) {
                getLoaderManager().initLoader(LOADER_DATA, null, mDataLoaderListener);
            } else if (Intent.ACTION_INSERT.equals(mAction)) {
                final Account account = mIntentExtras == null ? null :
                        (Account) mIntentExtras.getParcelable(Intents.Insert.ACCOUNT);
                final String dataSet = mIntentExtras == null ? null :
                        mIntentExtras.getString(Intents.Insert.DATA_SET);

                if (account != null) {
                    if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
                        Log.v(TAG, "onActivityCreated intent account name = "+account.name);
                        final int phoneType = SimUtility.getTypeByAccountName(account.name);
                        final boolean isCardReady = SimUtility.isSimReady(phoneType);
                        if (!SimUtility.getSIMLoadStatus() || !isCardReady
                            || (SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType) <= 0)) {
                            // Card if loading, or not ready or full
                            selectAccountAndCreateContact();
                        } else {
                            // Account specified in Intent
                            createContact(new AccountWithDataSet(account.name, account.type, dataSet));  
                        }                         
                    } else {
                        // Account specified in Intent
                        createContact(new AccountWithDataSet(account.name, account.type, dataSet));
                    }
                } else {
                    // No Account specified. Let the user choose
                    // Load Accounts async so that we can present them
                    selectAccountAndCreateContact();
                }
            } else if (ContactEditorActivity.ACTION_SAVE_COMPLETED.equals(mAction)) {
                // do nothing
            } else throw new IllegalArgumentException("Unknown Action String " + mAction +
                    ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT);
        }
    }

    @Override
    public void onStart() {
        getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        super.onStart();
    }

    public void load(String action, Uri lookupUri, Bundle intentExtras) {
        mAction = action;
        mLookupUri = lookupUri;
        mIntentExtras = intentExtras;
        mAutoAddToDefaultGroup = mIntentExtras != null
                && mIntentExtras.containsKey(INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY);
        mNewLocalProfile = mIntentExtras != null
                && mIntentExtras.getBoolean(INTENT_EXTRA_NEW_LOCAL_PROFILE);
        mExcludeSimCard = mIntentExtras != null
                && mIntentExtras.getBoolean(INTENT_EXTRA_EXCLUDE_SIMCARD);
    }

    public void setListener(Listener value) {
        mListener = value;
    }
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d(TAG,"onConfigurationChanged");
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE 
				|| newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Log.d(TAG,"onConfigurationChanged,orientation change");
			bOrientation = true;
		}
		 
	}

    @Override
    public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
        if (savedState != null) {
            // Restore mUri before calling super.onCreate so that onInitializeLoaders
            // would already have a uri and an action to work with
            mLookupUri = savedState.getParcelable(KEY_URI);
            mAction = savedState.getString(KEY_ACTION);
        }

        

        if (savedState == null) {
            // If savedState is non-null, onRestoreInstanceState() will restore the generator.
            mViewIdGenerator = new ViewIdGenerator();
        } else {
            // Read state from savedState. No loading involved here
            mState = savedState.<EntityDeltaList> getParcelable(KEY_EDIT_STATE);
            mRawContactIdRequestingPhoto = savedState.getLong(
                    KEY_RAW_CONTACT_ID_REQUESTING_PHOTO);
            mViewIdGenerator = savedState.getParcelable(KEY_VIEW_ID_GENERATOR);
            String fileName = savedState.getString(KEY_CURRENT_PHOTO_FILE);
            if (fileName != null) {
                mCurrentPhotoFile = new File(fileName);
            }
            mContactIdForJoin = savedState.getLong(KEY_CONTACT_ID_FOR_JOIN);
            mContactWritableForJoin = savedState.getBoolean(KEY_CONTACT_WRITABLE_FOR_JOIN);
            mAggregationSuggestionsRawContactId = savedState.getLong(KEY_SHOW_JOIN_SUGGESTIONS);
            mEnabled = savedState.getBoolean(KEY_ENABLED);
            mStatus = savedState.getInt(KEY_STATUS);
            mNewLocalProfile = savedState.getBoolean(KEY_NEW_LOCAL_PROFILE);
            mIsUserProfile = savedState.getBoolean(KEY_IS_USER_PROFILE);
        }
    }

    public void setData(ContactLoader.Result data) {
        // If we have already loaded data, we do not want to change it here to not confuse the user
        if (mState != null) {
            Log.v(TAG, "Ignoring background change. This will have to be rebased later");
            return;
        }

        // See if this edit operation needs to be redirected to a custom editor
        ArrayList<Entity> entities = data.getEntities();
        if (entities.size() == 1) {
            Entity entity = entities.get(0);
            ContentValues entityValues = entity.getEntityValues();
            String type = entityValues.getAsString(RawContacts.ACCOUNT_TYPE);
            String dataSet = entityValues.getAsString(RawContacts.DATA_SET);
            AccountType accountType = AccountTypeManager.getInstance(mContext).getAccountType(
                    type, dataSet);
            if (accountType.getEditContactActivityClassName() != null &&
                    !accountType.areContactsWritable()) {
                if (mListener != null) {
                    String name = entityValues.getAsString(RawContacts.ACCOUNT_NAME);
                    long rawContactId = entityValues.getAsLong(RawContacts.Entity._ID);
                    mListener.onCustomEditContactActivityRequested(
                            new AccountWithDataSet(name, type, dataSet),
                            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                            mIntentExtras, true);
                }
                return;
            }
        }

        bindEditorsForExistingContact(data);
    }

    @Override
    public void onExternalEditorRequest(AccountWithDataSet account, Uri uri) {
        mListener.onCustomEditContactActivityRequested(account, uri, null, false);
    }

    private void bindEditorsForExistingContact(ContactLoader.Result data) {
        setEnabled(true);

        mState = EntityDeltaList.fromIterator(data.getEntities().iterator());
        setIntentExtras(mIntentExtras);
        // MOT CHINA, to support replacing number/email for SIM contacts
        for (EntityDelta state : mState) {
            final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
                parsePhonefromExtras(state, mIntentExtras);
                parseEmailfromExtras(state, mIntentExtras);
            }
        }
        mIntentExtras = null;

        // For user profile, change the contacts query URI
        mIsUserProfile = data.isUserProfile();
        boolean localProfileExists = false;

        if (mIsUserProfile) {
            for (EntityDelta state : mState) {
                // For profile contacts, we need a different query URI
                state.setProfileQueryUri();
                // Try to find a local profile contact
                if (HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(state.getValues().getAsString(RawContacts.ACCOUNT_TYPE))) {
                    localProfileExists = true;
                }
            }
            // Editor should always present a local profile for editing
            if (!localProfileExists) {
                final ContentValues values = new ContentValues();
                values.putNull(RawContacts.ACCOUNT_NAME);
                values.putNull(RawContacts.ACCOUNT_TYPE);
                values.putNull(RawContacts.DATA_SET);
                EntityDelta insert = new EntityDelta(ValuesDelta.fromAfter(values));
                insert.setProfileQueryUri();
                mState.add(insert);
            }
        }
        mRequestFocus = true;

        bindEditors();
    }

    /**
     * Merges extras from the intent.
     */
    public void setIntentExtras(Bundle extras) {
        if (extras == null || extras.size() == 0) {
            return;
        }

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        for (EntityDelta state : mState) {
            final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            final String accountName = state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
            final String dataSet = state.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            if (type.areContactsWritable()) {
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
                    final int phoneType = SimUtility.getTypeByAccountName(accountName);
                    if (!SimUtility.isUSIMType(phoneType)) {
                        // MOT CHINA, do not try insert data for normal SIM which will put data into state
                        // Because the typeOverallMax of CardAccountType -> Phone DataKind is 2
                        Log.v(TAG, "DO NOT parse extra when editing a normal SIM contact");
                        break;
                    }
                }
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)
                    || HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(accountType)) {
                    // Do not replace display name of local/card contacts
                    extras.remove(Insert.NAME);
                    extras.remove(Insert.PHONETIC_NAME);
                }
                // Apply extras to the first writable raw contact only
                EntityModifier.parseExtras(mContext, type, state, extras);
                break;
            }
        }
    }

    /**
     * MOT CHINA
     * parse phone extras from the intent to check if need to replace the number of SIM contacts.
     */
    private void parsePhonefromExtras(EntityDelta state, Bundle extras) {
        if (extras == null || extras.size() == 0) {
            return;
        }
        final CharSequence csInputPhoneNumber = extras.getCharSequence(Insert.PHONE);
        if (TextUtils.isEmpty(csInputPhoneNumber)) {
            return;
        }
        final String inputPhoneNumber = SimUtility.buildSimNumber(csInputPhoneNumber.toString());
        if (inputPhoneNumber.startsWith("+")) {
            if (inputPhoneNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH+1) {
                Toast.makeText(mContext, R.string.sim_phone_number_length_error, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (inputPhoneNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH) {
                Toast.makeText(mContext, R.string.sim_phone_number_length_error, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        final String accountName = state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        final String dataSet = state.getValues().getAsString(RawContacts.DATA_SET);
        final AccountType type = accountTypes.getAccountType(accountType, dataSet);
        final DataKind kind = type.getKindForMimetype(Phone.CONTENT_ITEM_TYPE);
        ValuesDelta parseEntry = null;
        final int phoneType = SimUtility.getTypeByAccountName(accountName);
        final boolean isUSIM = SimUtility.isUSIMType(phoneType);
        if (isUSIM) {
            // Do not check this for SIM because SIM only has one number
            parseEntry = EntityModifier.parseExtras(state, kind, extras, Insert.PHONE_TYPE, Insert.PHONE, Phone.NUMBER);
        }
        if (parseEntry == null) {
            // Need replace, get old numbers
            String primaryNumber = null;
            String secondaryNumber = null;
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(Phone.CONTENT_ITEM_TYPE);
            for (ValuesDelta entry : entries) {
                String phoneNumber = entry.getAsString(Phone.NUMBER);
                if (primaryNumber == null) {
                    primaryNumber = phoneNumber;
                } else {
                    secondaryNumber = phoneNumber;
                }
            }
            if (TextUtils.equals(inputPhoneNumber, primaryNumber) || TextUtils.equals(inputPhoneNumber, secondaryNumber)) {
                // Found same number means the number already parsed into msate. Return directly
                return;
            }
            final EntityDelta fState = state;
            if (!isUSIM) { // SIM case
                // display warning dialog of overwriting
                mAlert = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.title_replace_number)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.text_replace_number)
                        .setPositiveButton(R.string.number_replace_replace, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                replaceNumber(fState, inputPhoneNumber, 0);
                            }})
//                      .setNeutralButton(R.string.number_replace_new, new DialogInterface.OnClickListener() {
//                          public void onClick(DialogInterface dialog, int which) {
// To support create new SIM contact
//                          }})
                        .setNegativeButton(R.string.number_replace_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                            }})
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                        }})
                        .show();
            } else { // USIM case
                final CharSequence[] items = {primaryNumber, secondaryNumber};
                mAlert = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.title_replace_number)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // select to be replaced entry
                                mSelection = which;
                            }})
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                replaceNumber(fState, inputPhoneNumber, mSelection);
                            }})
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                            }})
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                            }})
                        .show();
            }
        }
    }

    /**
     * MOT CHINA to replace the number of SIM contacts.
     */
    private void replaceNumber(EntityDelta state, String newNumber, int position) {
        final ArrayList<ValuesDelta> entries = state.getMimeEntries(Phone.CONTENT_ITEM_TYPE);
        int entryIndex = 0;
        for (ValuesDelta entry : entries) {
            if (entryIndex == position) {
                // Replacing the number according to user selection
                entry.put(Phone.NUMBER, newNumber);
                break;
            }
            entryIndex++;
        }
        // Find the KindSectionView for numbers and set new state
        for (int i = 0; i < mContent.getChildCount(); i++) {
            final View childView = mContent.getChildAt(i);
            if (childView instanceof RawContactEditorView) {
                final RawContactEditorView rawContactEditor = (RawContactEditorView) childView;
                final ViewGroup fields = rawContactEditor.getFieldViews();
                if (fields == null) return;
                for (int j = 0; j < fields.getChildCount(); j++) {
                    View child = fields.getChildAt(j);
                    if (child instanceof KindSectionView) {
                        final KindSectionView sectionView = (KindSectionView) child;
                        DataKind kind = sectionView.getKind();
                        if (Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
                            sectionView.setState(kind, state, false, mViewIdGenerator);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * MOT CHINA
     * parse email extras from the intent to check if need to replace the email of SIM contacts.
     */
    private void parseEmailfromExtras(EntityDelta state, Bundle extras) {
        if (extras == null || extras.size() == 0) {
            return;
        }

        final CharSequence csInputEmail = extras.getCharSequence(Insert.EMAIL);
        if (TextUtils.isEmpty(csInputEmail)) {
            return;
        }

        final String accountName = state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        final int phoneType = SimUtility.getTypeByAccountName(accountName);
        final boolean isUSIM = SimUtility.isUSIMType(phoneType);
        if (!isUSIM) { // Email is not supported
            Toast.makeText(mContext, R.string.sim_email_not_support, Toast.LENGTH_SHORT).show();
            return;
        }

        if (SimUtility.getEmailFreeSpace(mContext.getContentResolver()) <= 0) { // No free email space
            Toast.makeText(mContext, R.string.sim_email_full_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final String inputEmail = SimUtility.buildSimString(csInputEmail.toString());
        if (inputEmail.length() > SimUtility.MAX_SIM_EMAIL_LENGTH+1) {
            Toast.makeText(mContext, R.string.sim_email_length_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        final String dataSet = state.getValues().getAsString(RawContacts.DATA_SET);
        final AccountType type = accountTypes.getAccountType(accountType, dataSet);
        final DataKind kind = type.getKindForMimetype(Email.CONTENT_ITEM_TYPE);
        // Parse email from extras
        final ValuesDelta parseEntry = EntityModifier.parseExtras(state, kind, extras, Insert.EMAIL_TYPE, Insert.EMAIL, Email.DATA);
        if (parseEntry == null) {
            // The email in intent could be parsed already. Need to check equal to know if really need to replacing
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(Email.CONTENT_ITEM_TYPE);
            String oldEmail = null;
            for (ValuesDelta entry : entries) {
                oldEmail = entry.getAsString(Email.DATA);
            }
            final EntityDelta fState = state;
            if (!TextUtils.equals(oldEmail, csInputEmail.toString())) { // Replacing email if they doesn't equal
                // display warning dialog of overwriting
                mAlert = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.title_replace_email)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.text_replace_email)
                        .setPositiveButton(R.string.number_replace_replace, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                replaceEmail(fState, inputEmail, 0);
                            }})
//                      .setNeutralButton(R.string.number_replace_new, new DialogInterface.OnClickListener() {
//                          public void onClick(DialogInterface dialog, int which) {
// To support create new SIM contact
//                          }})
                        .setNegativeButton(R.string.number_replace_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                            }})
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                //setResult(RESULT_CANCELED);
                                //finish();
                        }})
                        .show();
            }
        }
    }

    /**
     * MOT CHINA to replace the email of SIM contacts.
     */
    private void replaceEmail(EntityDelta state, String newEmail, int position) {
        final ArrayList<ValuesDelta> entries = state.getMimeEntries(Email.CONTENT_ITEM_TYPE);
        int entryIndex = 0;
        for (ValuesDelta entry : entries) {
            if (entryIndex == position) {
                // Replacing the number according to user selection
                entry.put(Email.DATA, newEmail);
                break;
            }
            entryIndex++;
        }
        // Find the KindSectionView for numbers and set new state
        for (int i = 0; i < mContent.getChildCount(); i++) {
            final View childView = mContent.getChildAt(i);
            if (childView instanceof RawContactEditorView) {
                final RawContactEditorView rawContactEditor = (RawContactEditorView) childView;
                final ViewGroup fields = rawContactEditor.getFieldViews();
                if (fields == null) return;
                for (int j = 0; j < fields.getChildCount(); j++) {
                    View child = fields.getChildAt(j);
                    if (child instanceof KindSectionView) {
                        final KindSectionView sectionView = (KindSectionView) child;
                        DataKind kind = sectionView.getKind();
                        if (Email.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
                            sectionView.setState(kind, state, false, mViewIdGenerator);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void selectAccountAndCreateContact() {
        // If this is a local profile, then skip the logic about showing the accounts changed
        // activity and create a phone-local contact.
        if (mNewLocalProfile) {
            createContact(null);
            return;
        }

        //MOTO MOD BEGIN, IKPIM-899, this is about Remember Account Choice
        /*{
        // If there is no default account or the accounts have changed such that we need to
        // prompt the user again, then launch the account prompt.
        if (mEditorUtils.shouldShowAccountChangedNotification(mContext)) {
            Intent intent = new Intent(mContext, ContactEditorAccountsChangedActivity.class);
            mStatus = Status.SUB_ACTIVITY;
            startActivityForResult(intent, REQUEST_CODE_ACCOUNTS_CHANGED);
        } else {
            // Otherwise, there should be a default account. Then either create a local contact
            // (if default account is null) or create a contact with the specified account.
            AccountWithDataSet defaultAccount = mEditorUtils.getDefaultAccount();
            if (defaultAccount == null) {
                createContact(null);
            } else {
                createContact(defaultAccount);
            }
        } This is what google said, but we Motorola has our own Remember Account Choice, call createContact()*/
        createContact();
        //MOTO MOD END
    }

    /**
     * Create a contact by automatically selecting the first account. If there's no available
     * account, a device-local contact should be created.
     */
    private void createContact() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(mContext).getAccountsWithCard(true); // MOT CHINA
        AccountListFilter accountListFilter = AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITH_CARD;
        // No Accounts available. Create a phone-local contact.
        if (accounts.isEmpty()) {
            createContact(null);
            return;
        }

        //MOTO MOD BEGIN, IKPIM-899, this is about Remember Account Choice
        // We have an account switcher in "create-account" screen, so don't need to ask a user to
        // select an account here.
        //createContact(accounts.get(0));

        //Above is what google said, let's follow our remember account feature shown as below:)

        // In the common case of a single account being writable, auto-select
        // it without showing a dialog.
        if (accounts.size() == 1) {
            createContact(accounts.get(0));
            return;  // Don't show a dialog.
        }
        if(accounts.size() == 2 && mExcludeSimCard){
            for(AccountWithDataSet account : accounts) {
                if(!HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)){
                    createContact(account);
                    return;
                }
            }
        }
        if(mExcludeSimCard) {
            accountListFilter = AccountListFilter.ACCOUNTS_CONTACT_WRITABLE;
        }
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        // If user prefer to use a remembered account
        if (ContactPreferenceUtilities.IfUserChoiceRemembered(mContext) && ContactPreferenceUtilities.GetUserPreferredAccountIndex(mContext, true)!=-1) {
            AccountWithDataSet account = ContactPreferenceUtilities.GetUserPreferredAccountFromSharedPreferences(mContext);
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
                final int phoneType = SimUtility.getTypeByAccountName(account.name);
                final boolean isCardReady = SimUtility.isSimReady(phoneType);
                if (!SimUtility.getSIMLoadStatus() || !isCardReady
                    || (SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType) <= 0 || mExcludeSimCard)) {
                    // Card if loading, or not ready or full
                    SelectAccountDialogFragment.show(getFragmentManager(),
                            ContactEditorFragment.this, R.string.dialog_new_contact_account,
                            accountListFilter, null); // MOT CHINA
                    return;
                }
            }
            createContact(ContactPreferenceUtilities.GetUserPreferredAccountFromSharedPreferences(mContext));
            return;  // Don't show a dialog.
        }
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        //final SelectAccountDialogFragment dialog = new SelectAccountDialogFragment();
        //dialog.setTargetFragment(this, 0);
        //dialog.show(getFragmentManager(), SelectAccountDialogFragment.TAG);
        SelectAccountDialogFragment.show(getFragmentManager(),
                            ContactEditorFragment.this, R.string.dialog_new_contact_account,
                            accountListFilter, null); // MOT CHINA
        //MOTO MOD END, IKPIM-899
    }

    /**
     * Shows account creation screen associated with a given account.
     *
     * @param account may be null to signal a device-local contact should be created.
     */
    private void createContact(AccountWithDataSet account) {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final AccountType accountType =
                accountTypes.getAccountType(account != null ? account.type : null,
                        account != null ? account.dataSet : null);

        if (accountType.getCreateContactActivityClassName() != null) {
            if (mListener != null) {
                mListener.onCustomCreateContactActivityRequested(account, mIntentExtras);
            }
        } else {
            bindEditorsForNewContact(account, accountType);
        }
    }

    /**
     * Removes a current editor ({@link #mState}) and rebinds new editor for a new account.
     * Some of old data are reused with new restriction enforced by the new account.
     *
     * @param oldState Old data being edited.
     * @param oldAccount Old account associated with oldState.
     * @param newAccount New account to be used.
     */
    private void rebindEditorsForNewContact(
            EntityDelta oldState, AccountWithDataSet oldAccount, AccountWithDataSet newAccount) {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        AccountType oldAccountType = accountTypes.getAccountType(
                oldAccount.type, oldAccount.dataSet);
        AccountType newAccountType = accountTypes.getAccountType(
                newAccount.type, newAccount.dataSet);

        if (newAccountType.getCreateContactActivityClassName() != null) {
            Log.w(TAG, "external activity called in rebind situation");
            if (mListener != null) {
                mListener.onCustomCreateContactActivityRequested(newAccount, mIntentExtras);
            }
        } else {
            mState = null;
            bindEditorsForNewContact(newAccount, newAccountType, oldState, oldAccountType);
        }
    }

    private void bindEditorsForNewContact(AccountWithDataSet account,
            final AccountType accountType) {
        bindEditorsForNewContact(account, accountType, null, null);
    }

    private void bindEditorsForNewContact(AccountWithDataSet newAccount,
            final AccountType newAccountType, EntityDelta oldState, AccountType oldAccountType) {
        mStatus = Status.EDITING;

        final ContentValues values = new ContentValues();
        if (newAccount != null) {
            values.put(RawContacts.ACCOUNT_NAME, newAccount.name);
            values.put(RawContacts.ACCOUNT_TYPE, newAccount.type);
            values.put(RawContacts.DATA_SET, newAccount.dataSet);
        } else {
            values.putNull(RawContacts.ACCOUNT_NAME);
            values.putNull(RawContacts.ACCOUNT_TYPE);
            values.putNull(RawContacts.DATA_SET);
        }

        EntityDelta insert = new EntityDelta(ValuesDelta.fromAfter(values));
        if (oldState == null) {
            // Parse any values from incoming intent
            EntityModifier.parseExtras(mContext, newAccountType, insert, mIntentExtras);
        } else {
            EntityModifier.migrateStateForNewContact(mContext, oldState, insert,
                    oldAccountType, newAccountType);
        }

        // Ensure we have some default fields (if the account type does not support a field,
        // ensureKind will not add it, so it is safe to add e.g. Event)
        EntityModifier.ensureKindExists(insert, newAccountType, Phone.CONTENT_ITEM_TYPE);
        EntityModifier.ensureKindExists(insert, newAccountType, Email.CONTENT_ITEM_TYPE);
        // MOT CHINA - Not show Organization in editor by default
        // EntityModifier.ensureKindExists(insert, newAccountType, Organization.CONTENT_ITEM_TYPE);
        EntityModifier.ensureKindExists(insert, newAccountType, Event.CONTENT_ITEM_TYPE);
        EntityModifier.ensureKindExists(insert, newAccountType, StructuredPostal.CONTENT_ITEM_TYPE);

        // Set the correct URI for saving the contact as a profile
        if (mNewLocalProfile) {
            insert.setProfileQueryUri();
        }

        if (mState == null) {
            // Create state if none exists yet
            mState = EntityDeltaList.fromSingle(insert);
        } else {
            // Add contact onto end of existing state
            mState.add(insert);
        }

        mRequestFocus = true;

        bindEditors();
    }

    private void bindEditors() {
        // Sort the editors
        Collections.sort(mState, mComparator);

        // Remove any existing editors and rebuild any visible
        mContent.removeAllViews();

        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        int numRawContacts = mState.size();
        for (int i = 0; i < numRawContacts; i++) {
            // TODO ensure proper ordering of entities in the list
            final EntityDelta entity = mState.get(i);
            final ValuesDelta values = entity.getValues();
            if (!values.isVisible()) continue;

            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            final long rawContactId = values.getAsLong(RawContacts._ID);

            final BaseRawContactEditorView editor;
            if (!type.areContactsWritable()) {
                editor = (BaseRawContactEditorView) inflater.inflate(
                        R.layout.raw_contact_readonly_editor_view, mContent, false);
                ((RawContactReadOnlyEditorView) editor).setListener(this);
            } else {
                editor = (RawContactEditorView) inflater.inflate(R.layout.raw_contact_editor_view,
                        mContent, false);
            }
            if (Intent.ACTION_INSERT.equals(mAction) && numRawContacts == 1) {
                final List<AccountWithDataSet> accounts =
                        AccountTypeManager.getInstance(mContext).getAccountsWithCard(true); // MOT CHINA
                if (accounts.size() > 1 && !mNewLocalProfile) {
                    addAccountSwitcher(mState.get(0), editor);
                } else {
                    disableAccountSwitcher(editor);
                }
            } else {
                disableAccountSwitcher(editor);
            }

            editor.setEnabled(mEnabled);

            mContent.addView(editor);

            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
                mCardRawContactId = rawContactId;
                EntityModifier.ensureKindExists(entity, type, Phone.CONTENT_ITEM_TYPE);
                final int phoneType = SimUtility.getTypeByAccountName(accountName);
                if (SimUtility.isUSIMType(phoneType)) {
                    EntityModifier.ensureKindExists(entity, type, Email.CONTENT_ITEM_TYPE);
                }
            }
            editor.setState(entity, type, mViewIdGenerator, isEditingUserProfile());

            editor.getPhotoEditor().setEditorListener(
                    new PhotoEditorListener(editor, type.areContactsWritable()));
            if (editor instanceof RawContactEditorView) {
                final RawContactEditorView rawContactEditor = (RawContactEditorView) editor;

                final TextFieldsEditorView nameEditor = rawContactEditor.getNameEditor();
                if (mRequestFocus) {
                    nameEditor.requestFocus();
                    mRequestFocus = false;
                }

                if (!HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
                    EditorListener listener = new EditorListener() {

                        @Override
                        public void onRequest(int request) {
                        	/*Modifyed for switchuitwo-472 begin*/
                    //MOTO MOD BEGIN , To avoid the thread is recreated when activity is finishe
                        if (!isDestroy && request == EditorListener.FIELD_CHANGED && !isEditingUserProfile()) {
                                acquireAggregationSuggestions(rawContactEditor);
                            } else if (!isDestroy && request == EditorListener.FIELD_TURNED_EMPTY && !isEditingUserProfile()) {
                            	if(mAggregationSuggestionEngine != null) {
                            		mAggregationSuggestionEngine.reset();
                            	}
                            }
                        /*Modifyed for switchuitwo-472 end*/
                        }
                    //MOTO MOD END
                        @Override
                        public void onDeleteRequested(Editor removedEditor) {
                        }
                    };

                    nameEditor.setEditorListener(listener);

                    final TextFieldsEditorView phoneticNameEditor =
                            rawContactEditor.getPhoneticNameEditor();
                    phoneticNameEditor.setEditorListener(listener);
                    rawContactEditor.setAutoAddToDefaultGroup(mAutoAddToDefaultGroup);
                //MOTO MOD BEGIN
                if (!isDestroy && rawContactId == mAggregationSuggestionsRawContactId) {
                        acquireAggregationSuggestions(rawContactEditor);
                    }
                //MOTO MOD END
                }
            }
        }

        mRequestFocus = false;

        bindGroupMetaData();

        // Show editor now that we've loaded state
        mContent.setVisibility(View.VISIBLE);

        // Refresh Action Bar as the visibility of the join command
        // Activity can be null if we have been detached from the Activity
        final Activity activity = getActivity();
        if (activity != null) activity.invalidateOptionsMenu();

    }

    private void bindGroupMetaData() {
        if (mGroupMetaData == null) {
            return;
        }

        int editorCount = mContent.getChildCount();
        for (int i = 0; i < editorCount; i++) {
            BaseRawContactEditorView editor = (BaseRawContactEditorView) mContent.getChildAt(i);
            editor.setGroupMetaData(mGroupMetaData);
        }
    }

    private void saveDefaultAccountIfNecessary() {
        // Verify that this is a newly created contact, that the contact is composed of only
        // 1 raw contact, and that the contact is not a user profile.
        if (!Intent.ACTION_INSERT.equals(mAction) && mState.size() == 1 &&
                !isEditingUserProfile()) {
            return;
        }

        // Find the associated account for this contact (retrieve it here because there are
        // multiple paths to creating a contact and this ensures we always have the correct
        // account).
        final EntityDelta entity = mState.get(0);
        final ValuesDelta values = entity.getValues();
        String name = values.getAsString(RawContacts.ACCOUNT_NAME);
        String type = values.getAsString(RawContacts.ACCOUNT_TYPE);
        String dataSet = values.getAsString(RawContacts.DATA_SET);

        AccountWithDataSet account = (name == null || type == null) ? null :
                new AccountWithDataSet(name, type, dataSet);
        mEditorUtils.saveDefaultAndAllAccounts(account);
    }

    private void addAccountSwitcher(
            final EntityDelta currentState, BaseRawContactEditorView editor) {
        ValuesDelta values = currentState.getValues();
        final AccountWithDataSet currentAccount = new AccountWithDataSet(
                values.getAsString(RawContacts.ACCOUNT_NAME),
                values.getAsString(RawContacts.ACCOUNT_TYPE),
                values.getAsString(RawContacts.DATA_SET));
        final View accountView = editor.findViewById(R.id.account);
        final View anchorView = editor.findViewById(R.id.account_container);
        accountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	// BEGIN Motorola, ODC_001639, 2013-01-17, SWITCHUITWO-534
//            	final ListPopupWindow popup = new ListPopupWindow(mContext, null);
                mAccountSelectPopup = new ListPopupWindow(mContext, null);
                // END SWITCHUITWO-534
                final AccountsListAdapter adapter =
                        new AccountsListAdapter(mContext,
                        AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITH_CARD, currentAccount); // MOT CHINA
                mAccountSelectPopup.setWidth(anchorView.getWidth());
                mAccountSelectPopup.setAnchorView(anchorView);
                mAccountSelectPopup.setAdapter(adapter);
                mAccountSelectPopup.setModal(true);
                mAccountSelectPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
                mAccountSelectPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        mAccountSelectPopup.dismiss();
                        AccountWithDataSet newAccount = adapter.getItem(position);
                        if (!newAccount.equals(currentAccount)) {
                            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(newAccount.type)) {
                                final int phoneType = SimUtility.getTypeByAccountName(newAccount.name);
                                if (!SimUtility.getSIMLoadStatus()) {
                                    // Card is loading
                                    createCardErrorDialog(R.string.loadsim_text, false).show();
                                } else if (!SimUtility.isSimReady(phoneType)) {
                                    // Card is not available
                                    createCardErrorDialog(R.string.no_card_text, false).show();
                                } else if (SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType) <= 0) {
                                    // Card is full
                                    createCardErrorDialog(R.string.card_no_space, false).show();
                                } else {
                                    rebindEditorsForNewContact(currentState, currentAccount, newAccount);
                                }
                            } else {
                                rebindEditorsForNewContact(currentState, currentAccount, newAccount);
                            }
                        }
                    }
                });
                mAccountSelectPopup.show();
            }
        });
    }

    private Dialog createCardErrorDialog(int resId, boolean dismissEditor) {
        final DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
            }
        };

        final boolean toDismissEditor = dismissEditor;
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.menu_newContact);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(resId);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (toDismissEditor) {
                    ((Activity)mContext).finish();
                }
            }
        });
        // builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(false);
        Dialog ret_dlg =  builder.create();
        ret_dlg.setOnDismissListener(dismissListener);
        return ret_dlg;
    }

    private void disableAccountSwitcher(BaseRawContactEditorView editor) {
        // Remove the pressed state from the account header because the user cannot switch accounts
        // on an existing contact
        final View accountView = editor.findViewById(R.id.account);
        accountView.setBackgroundDrawable(null);
        accountView.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.edit_contact, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // This supports the keyboard shortcut to save changes to a contact but shouldn't be visible
        // because the custom action bar contains the "save" button now (not the overflow menu).
        // TODO: Find a better way to handle shortcuts, i.e. onKeyDown()?
        menu.findItem(R.id.menu_done).setVisible(false);

        // Split only if more than one raw profile and not a user profile
        menu.findItem(R.id.menu_split).setVisible(mState != null && mState.size() > 1 &&
                !isEditingUserProfile());
        // Cannot join a user profile
        menu.findItem(R.id.menu_join).setVisible(!isEditingUserProfile());

        //MOT MOD IKHSS6UPGR-6251, don't show MenuItem 'Save'&'Cancel' on phone
        if (!PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            menu.findItem(R.id.menu_save).setVisible(false);
            menu.findItem(R.id.menu_cancel).setVisible(false);
        }
        //MOT MOD IKHSS6UPGR-6251
        if (mState != null) {
            for (EntityDelta state : mState) {
                ValuesDelta values = state.getValues();
                final String account_type = values.getAsString(RawContacts.ACCOUNT_TYPE);
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account_type)) {
                    menu.findItem(R.id.menu_join).setVisible(false);
                    menu.findItem(R.id.menu_split).setVisible(false);
                }
            }
        }

        int size = menu.size();
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(mEnabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
            case R.id.menu_save: //MOT MOD IKHSS7-4528, add menu save to follow moto's ICS UI guideline
                if(RomUtility.isOutofMemory()){
                    Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    return true;
                    }

                return save(SaveMode.CLOSE);
            case R.id.menu_discard:
                return revert();
            case R.id.menu_split:
                return doSplitContactAction();
            case R.id.menu_join:
                return doJoinContactAction();
            //MOT MOD BEGIN - IKHSS7-4528
            case R.id.menu_cancel: {
                 doRevertAction();
                 }
                 return true;
            //MOT MOD END - IKHSS7-4528
        }
        return false;
    }

    private boolean doSplitContactAction() {
        if (!hasValidState()) return false;

        final SplitContactConfirmationDialogFragment dialog =
                new SplitContactConfirmationDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), SplitContactConfirmationDialogFragment.TAG);
        return true;
    }

    private boolean doJoinContactAction() {
        if (!hasValidState()) {
            return false;
        }

        // If we just started creating a new contact and haven't added any data, it's too
        // early to do a join
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        if (mState.size() == 1 && mState.get(0).isContactInsert()
                && !EntityModifier.hasChanges(mState, accountTypes)) {
            Toast.makeText(getActivity(), R.string.toast_join_with_empty_contact,
                            Toast.LENGTH_LONG).show();
            return true;
        }

        return save(SaveMode.JOIN);
    }

    private void loadPhotoPickSize() {
        Cursor c = mContext.getContentResolver().query(DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                new String[]{DisplayPhoto.DISPLAY_MAX_DIM}, null, null, null);
        try {
            c.moveToFirst();
            mPhotoPickSize = c.getInt(0);
        } finally {
            c.close();
        }
    }

    /**
     * Constructs an intent for picking a photo from Gallery, cropping it and returning the bitmap.
     */
    public Intent getPhotoPickIntent() {
    	/*Modifyed for SWITCHUI-43*/
    	Log.d("Photo", "getPhotoPickIntent !!!! mPhotoPickSize=" + mPhotoPickSize);
    	String fileName = generateTempPhotoFileName();
    	mCropImageFile = pathForCroppedPhoto(mContext, fileName);
    	Uri corpUri = Uri.fromFile(new File(mCropImageFile));
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", mPhotoPickSize/3);
        intent.putExtra("outputY", mPhotoPickSize/3);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, corpUri);
        //intent.putExtra("return-data", true);
        /*Modifyed for SWITCHUI-43 end*/
        return intent;
    }
    /*Modifyed for SWITCHUI-43*/
    public static String pathForCroppedPhoto(Context context, String fileName) {
        final File dir = new File(context.getExternalCacheDir() + "/tmp");
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }
    public static String generateTempPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT);
        return "ContactPhoto-" + dateFormat.format(date) + ".jpg";
    }
    /*Modifyed for SWITCHUI-43 end*/
    /**
     * Check if our internal {@link #mState} is valid, usually checked before
     * performing user actions.
     */
    private boolean hasValidState() {
        return mState != null && mState.size() > 0;
    }

    /**
     * Create a file name for the icon photo using current time.
     */
    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }

    /**
     * Constructs an intent for capturing a photo and storing it in a temporary file.
     */
    public static Intent getTakePickIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    protected void doCropPhoto(File f) {
        try {
            // Add the image to the media store
            MediaScannerConnection.scanFile(
                    mContext,
                    new String[] { f.getAbsolutePath() },
                    new String[] { null },
                    null);

            // Launch gallery to crop the photo
            final Intent intent = getCropImageIntent(Uri.fromFile(f));
            mStatus = Status.SUB_ACTIVITY;
            startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
            Toast.makeText(mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Constructs an intent for image cropping.
     */
    public Intent getCropImageIntent(Uri photoUri) {
    	/*Modifyed for SWITCHUI-43*/
    	String fileName = generateTempPhotoFileName();
    	mCropImageFile = pathForCroppedPhoto(mContext, fileName);
    	Uri corpUri = Uri.fromFile(new File(mCropImageFile));
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", mPhotoPickSize/3);
        intent.putExtra("outputY", mPhotoPickSize/3);
        //intent.putExtra("return-data", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, corpUri);
        /*Modifyed for SWITCHUI-43 end*/
        return intent;
    }

    private void add189Email() {
        //Log.v(TAG, "Entering add189Email()");

        // check every entry for CT-like (189, 153, 133...) numbers
        if (mState != null ) {
            ArrayList<ValuesDelta> ctEmailEntries = new ArrayList<ValuesDelta>();
            //int size = mState.size();
            //Log.v(TAG, "mState.size = "+size+", ctEmails = "+ctEmailEntries.size());

            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
            for (EntityDelta state : mState) {
                ValuesDelta values = state.getValues();
                final String account_type = values.getAsString(RawContacts.ACCOUNT_TYPE);
                final String dataSet = values.getAsString(RawContacts.DATA_SET);
                final AccountType accountType = accountTypes.getAccountType(account_type, dataSet);

                // Walk through entries for each well-known kind
                for (DataKind kind : accountType.getSortedDataKinds()) {
                    final String mimeType = kind.mimeType;

                    // check phone type
                    if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                        //Log.v(TAG, "PHONE type entries: "+entries);
                        if (entries == null) continue;
                        //Log.v(TAG, entries.size()+" numbers");

                        for (ValuesDelta entry : entries) {
                            //Log.v(TAG, "entry.toString = "+entry.toString());

                            String phoneNumber = entry.getAsString(Phone.NUMBER);
                            //Log.v(TAG, "original number = "+phoneNumber);
                            if (TextUtils.isEmpty(phoneNumber)) continue;

                            phoneNumber = phoneNumber.trim();
                        	// strip '-'
                        	phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);

                            String convertNumber = phoneNumber;

                            if (phoneNumber.startsWith("0086")) convertNumber = phoneNumber.substring(4);
                            if (phoneNumber.startsWith("+86")) convertNumber = phoneNumber.substring(3);

                            //Log.v(TAG, "converted number = "+convertNumber);
                            if (!TextUtils.isEmpty(convertNumber)&& convertNumber.length() == 11
                                && (convertNumber.startsWith("189")
                                || convertNumber.startsWith("153")
                                || convertNumber.startsWith("133"))) {
                                // it's CT qualified number
                                String email = convertNumber + "@189.cn";
                                //Log.v(TAG, "it's 189 style number, email: "+email);

                                final ContentValues emailValues = new ContentValues();
                                emailValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                                emailValues.put(Email.TYPE, Email.TYPE_MOBILE);
                                emailValues.put(Email.ADDRESS, email);

                                ValuesDelta emailEntry = ValuesDelta.fromAfter(emailValues);
                                //Log.v(TAG, "new email entry: "+emailEntry.toString());
                        	    boolean existing = false;
                        	    for (ValuesDelta ctToBeAdded : ctEmailEntries) {
                        	    	if (email.equals(ctToBeAdded.getAsString(Email.ADDRESS))) {
                        	    		// already in the list, don't add
                        	    		existing = true;
                        	    		break;
                        	    	}
                        	    }

                        	    if (!existing) {
                        	        ctEmailEntries.add(emailEntry);
                        	        //Log.v(TAG, "new email Entry added");
                        	    } else {
                        	    	//Log.v(TAG, "already exists in ctEmailEntries, skip adding");
                        	    }
                        	}
                        	//Log.v(TAG, "ctEmailEntries.size() = "+ctEmailEntries.size());
                        }
                    } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        // exclude those CT 189 emails already in Contacts
                        final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                        //Log.v(TAG, "EMAIL type entries: "+entries);
                        if (entries == null) continue;
                        //Log.v(TAG, entries.size()+" email");

                        if (entries.size() > 0 && ctEmailEntries.size() > 0) {
                            for (ValuesDelta entry : entries) {
                                //Log.v(TAG, "entry.toString = "+entry.toString());
                                String email = entry.getAsString(Email.ADDRESS);
                                //Log.v(TAG, "original existing email = "+email);

                                if (TextUtils.isEmpty(email)) continue;
                                ArrayList<ValuesDelta> ctEmailEntriesToBeRemoved = new ArrayList<ValuesDelta>();
                                // check if new 189email tobeadded already in the existing emails
                                for (ValuesDelta ctToBeAdded : ctEmailEntries) {
                                    if (email.equals(ctToBeAdded.getAsString(Email.ADDRESS))) {
                                        ctEmailEntriesToBeRemoved.add(ctToBeAdded);
                                    }
                                }

                                if (ctEmailEntriesToBeRemoved.size() > 0) {
                                    // remove according to the ToBeRemovedList
                                    for (ValuesDelta removeEntry : ctEmailEntriesToBeRemoved) {
                                        ctEmailEntries.remove(removeEntry);
                                        //Log.v(TAG, "found existing email, removing from toBeAdded list, size = "+ctEmailEntries.size());
                                    }
                                }
                            }
                        }
                    }
                }

                //Log.v(TAG, "dup check done, toBeAdded 189 emails: "+ctEmailEntries.size());

                // add into the list to be stored
                if (ctEmailEntries.size() > 0) {
                    for (ValuesDelta entry : ctEmailEntries) {
                        state.addEntry(entry);
                    }
                }
            }

        } else {
            Log.v(TAG, "mState = null");
        }
        return;
    }

    /**
     * Saves or creates the contact based on the mode, and if successful
     * finishes the activity.
     */
    public boolean save(int saveMode) {
        if (!hasValidState() || mStatus != Status.EDITING) {
            Log.e(TAG,"save  contact failed saveMode=" + saveMode + " mStatus=" + mStatus);
            return false;
        }

        // If we are about to close the editor - there is no need to refresh the data
        if (saveMode == SaveMode.CLOSE || saveMode == SaveMode.SPLIT) {
            getLoaderManager().destroyLoader(LOADER_DATA);

            // kill the listening thread if existing
            if (mAggregationSuggestionEngine != null) {
                mAggregationSuggestionEngine.quit();
            }
            mAggregationSuggestionEngine = null;
        }

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);

        boolean isCardContact = false;
        for (EntityDelta state : mState) {
            ValuesDelta values = state.getValues();
            final String account_type = values.getAsString(RawContacts.ACCOUNT_TYPE);
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account_type)) {
                isCardContact = true;
            }
        }

        if (mIs189FlexEnable && !isCardContact) {
            // get settings preference
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            mAdd189 = mPrefs.getBoolean(Prefs.KEY_ADD_189, Prefs.KEY_ADD_189_DEFAULT);
            // Log.v(TAG, "doSaveAction(), mAdd189 = "+mAdd189);

            // passs auto189 flag into .buildDiff()
            mState.setAuto189(mAdd189);

            // automatically add 189 email address for CT numbers
            if (mAdd189) {
                add189Email();
            }
        } else {
            // passs auto189 flag-FALSE into .buildDiff()
            mState.setAuto189(mAdd189);
        }

        if (!EntityModifier.hasChanges(mState, accountTypes)) {
            mStatus = Status.SAVING;
            onSaveCompleted(false, saveMode, mLookupUri != null, mLookupUri);
            //MOTOROLA MOD Start: IKTABLETMAIN-4590
            //for the case described in IKTABLETMAIN-4590, need to reset mStatus
            if( (saveMode == SaveMode.RELOAD) && (mLookupUri == null) ) {
                mStatus = Status.EDITING;
            }
            //MOTOROLA MOD End: IKTABLETMAIN-4590
            return true;
        }

        for (EntityDelta state : mState) {
            ValuesDelta values = state.getValues();
            final String account_type = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String account_name = values.getAsString(RawContacts.ACCOUNT_NAME);
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account_type)) {
                final String dataSet = values.getAsString(RawContacts.DATA_SET);
                final AccountType accountType = accountTypes.getAccountType(account_type, dataSet);
                final int phoneType = SimUtility.getTypeByAccountName(account_name);
                final boolean isUsim = SimUtility.isUSIMType(phoneType);
                SimUtility.PeopleInfo pinfo = prepareSIMInfo(state, accountType, isUsim);
                Log.d(TAG, "mIsChecking = " + mIsChecking);
                if (mIsChecking || !validateEntriesBeforeSave(pinfo, isUsim)) {
                    //Validation failed
                    Log.e(TAG, "can not save contacts Validation failed!");
                    return false;
                }
            }
        }

        mStatus = Status.SAVING;

        setEnabled(false);

        // Store account as default account, only if this is a new contact
        saveDefaultAccountIfNecessary();

        // Save contact
        Intent intent = ContactSaveService.createSaveContactIntent(getActivity(), mState,
                SAVE_MODE_EXTRA_KEY, saveMode, isEditingUserProfile(),
                getActivity().getClass(), ContactEditorActivity.ACTION_SAVE_COMPLETED);
        getActivity().startService(intent);
        return true;
    }

    private boolean validateEntriesBeforeSave(SimUtility.PeopleInfo pInfo, boolean isUsim) {
        Log.d(TAG, "Enter validateEntriesBeforeSave(), isUsim = "+isUsim);

        int res_title = R.string.title_invalid_sim_contact;
        int res_message = -1;

        mIsChecking = false;

        if (!TextUtils.isEmpty(pInfo.peopleName)) {
            if (SimUtility.isEngName(pInfo.peopleName)) {
                if (pInfo.peopleName.length() > SimUtility.SIM_NAME_LENGTH_ENG)
                    res_message = R.string.sim_english_name_length_error;
            } else {
                if (pInfo.peopleName.length() > SimUtility.SIM_NAME_LENGTH_CHN)
                    res_message = R.string.sim_chinese_name_length_error;
            }
        }
        if (pInfo.primaryNumber.startsWith("+")) {
            if (pInfo.primaryNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH+1)
                res_message = R.string.sim_phone_number_length_error;
        } else {
            if (pInfo.primaryNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH)
                res_message = R.string.sim_phone_number_length_error;
        }
        if (pInfo.secondaryNumber.startsWith("+")) {
            if (pInfo.secondaryNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH+1)
                res_message = R.string.sim_phone_number_length_error;
        } else {
            if (pInfo.secondaryNumber.length() > SimUtility.MAX_SIM_PHONE_NUMBER_LENGTH)
                res_message = R.string.sim_phone_number_length_error;
        }
        if (!TextUtils.isEmpty(pInfo.primaryEmail)) {
            if (pInfo.primaryEmail.length() > SimUtility.MAX_SIM_EMAIL_LENGTH)
                res_message = R.string.sim_email_length_error;
            // Check email free space when there is email of a new sim contact switched from other accounts
            // For Editing an existing sim contact, the editor listener shall prevent email inputs
            if (mCardRawContactId <= 0) {
                // Switched from other accounts to a new SIM contact
                if (!isUsim) { 
                    // Not USIM, Email is not supported, Discard it
                    pInfo.primaryEmail = "";
                } else if (SimUtility.getEmailFreeSpace(mContext.getContentResolver()) <= 0) {
                    // It's USIM, need to check email free space
                    res_message = R.string.sim_email_full_error;
                }
            }
        }

        if ((!isUsim && TextUtils.isEmpty(pInfo.primaryNumber))
                || (isUsim && TextUtils.isEmpty(pInfo.primaryNumber)
                    && (TextUtils.isEmpty(pInfo.peopleName) || TextUtils.isEmpty(pInfo.primaryEmail))
                    && TextUtils.isEmpty(pInfo.secondaryNumber))) {
            // show warning
            res_title = R.string.title_empty_number;
            res_message = R.string.text_empty_number;
            if (isUsim && TextUtils.isEmpty(pInfo.primaryNumber)
                && TextUtils.isEmpty(pInfo.secondaryNumber)) {
                // if it's USIM without number, the name and email won't be empty together
                res_title = R.string.title_invalid_sim_contact;
                if (TextUtils.isEmpty(pInfo.peopleName)) {
                    // if name is empty then email is not empty, need to input name or number
                    res_message = R.string.text_empty_name_number;
                } else if (TextUtils.isEmpty(pInfo.primaryEmail)){
                    // if email is empty then name is not empty, need to input number or email
                    res_message = R.string.text_empty_number_email;
                }
            }
        }

        if (res_message != -1) {
            mIsChecking = true;
            mAlert = new AlertDialog.Builder(mContext)
                .setTitle(res_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(res_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mIsChecking = false;
                    }})
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    mIsChecking = false;
                }})
            .show();
            return false;
        }
        return true;
    }

    private SimUtility.PeopleInfo prepareSIMInfo(EntityDelta state, AccountType accountType, boolean isUsim) {
        Log.d(TAG, "Entering prepareSIMInfo()");
        SimUtility.PeopleInfo info = new SimUtility.PeopleInfo();

        for (DataKind kind : accountType.getSortedDataKinds()) {
            final String mimeType = kind.mimeType;
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            if (entries == null) continue;

            for (ValuesDelta entry : entries) {
                // An empty Insert must be ignored, because it won't save anything (an example
                // is an empty name that stays empty)
                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    info.peopleName= entry.getAsString(StructuredName.DISPLAY_NAME);
                    //Log.d(TAG, "a18768 peopleName = "+info.peopleName);
                } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    if (TextUtils.isEmpty(info.primaryNumber)) {
                            info.primaryNumber = entry.getAsString(Phone.NUMBER);
                            //Log.d(TAG, "a18768 default primaryNumber = "+info.primaryNumber);
                    } else if (entry.getAsInteger(Data.IS_PRIMARY, 0) != 0) {
                            info.secondaryNumber = info.primaryNumber;
                            info.primaryNumber = entry.getAsString(Phone.NUMBER);
                            //Log.d(TAG, "a18768 user set primaryNumber = "+info.primaryNumber);
                    } else {
                        info.secondaryNumber= entry.getAsString(Phone.NUMBER);
                        //Log.d(TAG, "a18768 secondaryNumber = "+info.secondaryNumber);
                    }
                } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    if (isUsim) {
                        info.primaryEmail= entry.getAsString(Email.ADDRESS);
                    } else {
                        // NOT USIM, don't support Email, Discard it
                        entry.putNull(Email.ADDRESS);
                    }
                    //Log.d(TAG, "a18768 primaryEmail = "+info.primaryEmail);
                }
            }
        }

        // Do not use buildSimName here because it will truncate name which fail length check later
        if (info.peopleName == null) {
            info.peopleName = "";
        } else {
            info.peopleName = info.peopleName.trim();
        }
        info.primaryNumber = SimUtility.buildSimNumber(info.primaryNumber);
        info.secondaryNumber = SimUtility.buildSimNumber(info.secondaryNumber);
        info.primaryEmail = SimUtility.buildSimString(info.primaryEmail);

        return info;
    }

    public static class CancelEditDialogFragment extends DialogFragment {

        public static void show(ContactEditorFragment fragment) {
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
                                ((ContactEditorFragment)getTargetFragment()).doRevertAction();
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            return dialog;
        }
    }

    private boolean revert() {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        if (mState == null || !EntityModifier.hasChanges(mState, accountTypes)) {
            doRevertAction();
        } else {
            CancelEditDialogFragment.show(this);
        }
        return true;
    }

    //MOT MOD IKHSS6UPGR-6251, make doRevertAction public to be called in ContactEditorActivity
    public void doRevertAction() {
        // When this Fragment is closed we don't want it to auto-save
        mStatus = Status.CLOSING;
        if (mListener != null) mListener.onReverted();
    }

    public void doSaveAction() {
        save(SaveMode.CLOSE);
    }

    public void onJoinCompleted(Uri uri) {
        onSaveCompleted(false, SaveMode.RELOAD, uri != null, uri);
    }

    public void onSaveCompleted(boolean hadChanges, int saveMode, boolean saveSucceeded,
            Uri contactLookupUri) {
        Log.d(TAG, "onSaveCompleted(" + saveMode + ", " + contactLookupUri);
        if (hadChanges) {
            if (saveSucceeded) {
                if (saveMode != SaveMode.JOIN) {
                    Toast.makeText(mContext, R.string.contactSavedToast, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, R.string.contactSavedErrorToast, Toast.LENGTH_LONG).show();
            }
        }
        switch (saveMode) {
            case SaveMode.CLOSE:
            case SaveMode.HOME:
                final Intent resultIntent;
                if (saveSucceeded && contactLookupUri != null) {
                    final String requestAuthority =
                            mLookupUri == null ? null : mLookupUri.getAuthority();

                    final String legacyAuthority = "contacts";

                    resultIntent = new Intent();
                    resultIntent.setAction(Intent.ACTION_VIEW);
                    if (legacyAuthority.equals(requestAuthority)) {
                        // Build legacy Uri when requested by caller
                        final long contactId = ContentUris.parseId(Contacts.lookupContact(
                                mContext.getContentResolver(), contactLookupUri));
                        final Uri legacyContentUri = Uri.parse("content://contacts/people");
                        final Uri legacyUri = ContentUris.withAppendedId(
                                legacyContentUri, contactId);
                        resultIntent.setData(legacyUri);
                    } else {
                        // Otherwise pass back a lookup-style Uri
                        resultIntent.setData(contactLookupUri);
                    }

                } else {
                    resultIntent = null;
                }
                // It is already saved, so prevent that it is saved again
                mStatus = Status.CLOSING;
                if (mListener != null) mListener.onSaveFinished(resultIntent);
                break;

            case SaveMode.RELOAD:
            case SaveMode.JOIN:
                if (saveSucceeded && contactLookupUri != null) {
                    // If it was a JOIN, we are now ready to bring up the join activity.
                    if (saveMode == SaveMode.JOIN) {
                        showJoinAggregateActivity(contactLookupUri);
                    }

                    // If this was in INSERT, we are changing into an EDIT now.
                    // If it already was an EDIT, we are changing to the new Uri now
                    mState = null;
                    load(Intent.ACTION_EDIT, contactLookupUri, null);
                    mStatus = Status.LOADING;
                    getLoaderManager().restartLoader(LOADER_DATA, null, mDataLoaderListener);
                }
                break;

            case SaveMode.SPLIT:
                mStatus = Status.CLOSING;
                if (mListener != null) {
                    mListener.onContactSplit(contactLookupUri);
                } else {
                    Log.d(TAG, "No listener registered, can not call onSplitFinished");
                }
                break;
        }
    }

    /**
     * Shows a list of aggregates that can be joined into the currently viewed aggregate.
     *
     * @param contactLookupUri the fresh URI for the currently edited contact (after saving it)
     */
    private void showJoinAggregateActivity(Uri contactLookupUri) {
        if (contactLookupUri == null || !isAdded()) {
            return;
        }

        mContactIdForJoin = ContentUris.parseId(contactLookupUri);
        mContactWritableForJoin = isContactWritable();
        final Intent intent = new Intent(JoinContactActivity.JOIN_CONTACT);
        intent.putExtra(JoinContactActivity.EXTRA_TARGET_CONTACT_ID, mContactIdForJoin);
        startActivityForResult(intent, REQUEST_CODE_JOIN);
    }

    /**
     * Performs aggregation with the contact selected by the user from suggestions or A-Z list.
     */
    private void joinAggregate(final long contactId) {
        Intent intent = ContactSaveService.createJoinContactsIntent(mContext, mContactIdForJoin,
                contactId, mContactWritableForJoin,
                ContactEditorActivity.class, ContactEditorActivity.ACTION_JOIN_COMPLETED);
        mContext.startService(intent);
    }

    /**
     * Returns true if there is at least one writable raw contact in the current contact.
     */
    private boolean isContactWritable() {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        /* MotMod start: IKHSS6-2483: ForceClose: com.android.contacts - x1 */
        if (mState == null) {
            return false;
        }
        /* MotMod end: IKHSS6-2483 */
        int size = mState.size();
        for (int i = 0; i < size; i++) {
            ValuesDelta values = mState.get(i).getValues();
            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            if (type.areContactsWritable()) {
                return true;
            }
        }
        return false;
    }

    private boolean isEditingUserProfile() {
        return mNewLocalProfile || mIsUserProfile;
    }

    public static interface Listener {
        /**
         * Contact was not found, so somehow close this fragment. This is raised after a contact
         * is removed via Menu/Delete (unless it was a new contact)
         */
        void onContactNotFound();

        /**
         * Contact was split, so we can close now.
         * @param newLookupUri The lookup uri of the new contact that should be shown to the user.
         * The editor tries best to chose the most natural contact here.
         */
        void onContactSplit(Uri newLookupUri);

        //MOTOROLA MOD BEGIN, IKPIM-899
        /**
         * User was presented with an account selection and couldn't decide.
         */
        void onAccountSelectorAborted();
        //MOTOROLA MOD END

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(Intent resultIntent);

        /**
         * User switched to editing a different contact (a suggestion from the
         * aggregation engine).
         */
        void onEditOtherContactRequested(
                Uri contactLookupUri, ArrayList<ContentValues> contentValues);

        /**
         * Contact is being created for an external account that provides its own
         * new contact activity.
         */
        void onCustomCreateContactActivityRequested(AccountWithDataSet account,
                Bundle intentExtras);

        /**
         * The edited raw contact belongs to an external account that provides
         * its own edit activity.
         *
         * @param redirect indicates that the current editor should be closed
         *            before the custom editor is shown.
         */
        void onCustomEditContactActivityRequested(AccountWithDataSet account, Uri rawContactUri,
                Bundle intentExtras, boolean redirect);
    }

    private class EntityDeltaComparator implements Comparator<EntityDelta> {
        /**
         * Compare EntityDeltas for sorting the stack of editors.
         */
        @Override
        public int compare(EntityDelta one, EntityDelta two) {
            // Check direct equality
            if (one.equals(two)) {
                return 0;
            }

            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
            String accountType1 = one.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            String dataSet1 = one.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type1 = accountTypes.getAccountType(accountType1, dataSet1);
            String accountType2 = two.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            String dataSet2 = two.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type2 = accountTypes.getAccountType(accountType2, dataSet2);

            // Check read-only
            if (!type1.areContactsWritable() && type2.areContactsWritable()) {
                return 1;
            } else if (type1.areContactsWritable() && !type2.areContactsWritable()) {
                return -1;
            }

            // Check account type
            boolean skipAccountTypeCheck = false;
            boolean isGoogleAccount1 = type1 instanceof GoogleAccountType;
            boolean isGoogleAccount2 = type2 instanceof GoogleAccountType;
            if (isGoogleAccount1 && !isGoogleAccount2) {
                return -1;
            } else if (!isGoogleAccount1 && isGoogleAccount2) {
                return 1;
            } else if (isGoogleAccount1 && isGoogleAccount2){
                skipAccountTypeCheck = true;
            }

            int value;
            if (!skipAccountTypeCheck) {
                if (type1.accountType == null) {
                    return 1;
                }
                value = type1.accountType.compareTo(type2.accountType);
                if (value != 0) {
                    return value;
                } else {
                    // Fall back to data set.
                    if (type1.dataSet != null) {
                        value = type1.dataSet.compareTo(type2.dataSet);
                        if (value != 0) {
                            return value;
                        }
                    } else if (type2.dataSet != null) {
                        return 1;
                    }
                }
            }

            // Check account name
            ValuesDelta oneValues = one.getValues();
            String oneAccount = oneValues.getAsString(RawContacts.ACCOUNT_NAME);
            if (oneAccount == null) oneAccount = "";
            ValuesDelta twoValues = two.getValues();
            String twoAccount = twoValues.getAsString(RawContacts.ACCOUNT_NAME);
            if (twoAccount == null) twoAccount = "";
            value = oneAccount.compareTo(twoAccount);
            if (value != 0) {
                return value;
            }

            // Both are in the same account, fall back to contact ID
            Long oneId = oneValues.getAsLong(RawContacts._ID);
            Long twoId = twoValues.getAsLong(RawContacts._ID);
            if (oneId == null) {
                return -1;
            } else if (twoId == null) {
                return 1;
            }

            return (int)(oneId - twoId);
        }
    }

    /**
     * Returns the contact ID for the currently edited contact or 0 if the contact is new.
     */
    protected long getContactId() {
        if (mState != null) {
            for (EntityDelta rawContact : mState) {
                Long contactId = rawContact.getValues().getAsLong(RawContacts.CONTACT_ID);
                if (contactId != null) {
                    return contactId;
                }
            }
        }
        return 0;
    }

    /**
     * Triggers an asynchronous search for aggregation suggestions.
     */
    public void acquireAggregationSuggestions(RawContactEditorView rawContactEditor) {
        long rawContactId = rawContactEditor.getRawContactId();
        if (mAggregationSuggestionsRawContactId != rawContactId
                && mAggregationSuggestionView != null) {
            mAggregationSuggestionView.setVisibility(View.GONE);
            mAggregationSuggestionView = null;
            mAggregationSuggestionEngine.reset();
        }
        mAggregationSuggestionsRawContactId = rawContactId;

        if (mAggregationSuggestionEngine == null) {
            mAggregationSuggestionEngine = new AggregationSuggestionEngine(getActivity());
            mAggregationSuggestionEngine.setListener(this);
            mAggregationSuggestionEngine.start();
        }

        mAggregationSuggestionEngine.setContactId(getContactId());

        LabeledEditorView nameEditor = rawContactEditor.getNameEditor();
        mAggregationSuggestionEngine.onNameChange(nameEditor.getValues());
    }

    @Override
    public void onAggregationSuggestionChange() {
        if (!isAdded() || mState == null || mStatus != Status.EDITING) {
            return;
        }

        if (mAggregationSuggestionPopup != null && mAggregationSuggestionPopup.isShowing()) {
            mAggregationSuggestionPopup.dismiss();
        }

        if (mAggregationSuggestionEngine.getSuggestedContactCount() == 0) {
            return;
        }

        final RawContactEditorView rawContactView =
                (RawContactEditorView)getRawContactEditorView(mAggregationSuggestionsRawContactId);
        if (rawContactView == null) {
            return; // Raw contact deleted?
        }
        final View anchorView = rawContactView.findViewById(R.id.anchor_view);
        mAggregationSuggestionPopup = new ListPopupWindow(mContext, null);
        mAggregationSuggestionPopup.setAnchorView(anchorView);
        mAggregationSuggestionPopup.setWidth(anchorView.getWidth());
        mAggregationSuggestionPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        mAggregationSuggestionPopup.setModal(true);
        mAggregationSuggestionPopup.setAdapter(
                new AggregationSuggestionAdapter(getActivity(),
                        mState.size() == 1 && mState.get(0).isContactInsert(),
                        this, mAggregationSuggestionEngine.getSuggestions()));
        mAggregationSuggestionPopup.setOnItemClickListener(mAggregationSuggestionItemClickListener);
        mAggregationSuggestionPopup.show();
    }

    @Override
    public void onJoinAction(long contactId, List<Long> rawContactIdList) {
        long rawContactIds[] = new long[rawContactIdList.size()];
        for (int i = 0; i < rawContactIds.length; i++) {
            rawContactIds[i] = rawContactIdList.get(i);
        }
        JoinSuggestedContactDialogFragment dialog =
                new JoinSuggestedContactDialogFragment();
        Bundle args = new Bundle();
        args.putLongArray("rawContactIds", rawContactIds);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        try {
            dialog.show(getFragmentManager(), "join");
        } catch (Exception ex) {
            // No problem - the activity is no longer available to display the dialog
        }
    }

    public static class JoinSuggestedContactDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.aggregation_suggestion_join_dialog_title)
                    .setMessage(R.string.aggregation_suggestion_join_dialog_message)
                    .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ContactEditorFragment targetFragment =
                                        (ContactEditorFragment) getTargetFragment();
                                long rawContactIds[] =
                                        getArguments().getLongArray("rawContactIds");
                                targetFragment.doJoinSuggestedContact(rawContactIds);
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }
    }

    /**
     * Joins the suggested contact (specified by the id's of constituent raw
     * contacts), save all changes, and stay in the editor.
     */
    protected void doJoinSuggestedContact(long[] rawContactIds) {
        if (!hasValidState() || mStatus != Status.EDITING) {
            return;
        }

        mState.setJoinWithRawContacts(rawContactIds);
        save(SaveMode.RELOAD);
    }

    @Override
    public void onEditAction(Uri contactLookupUri) {
        SuggestionEditConfirmationDialogFragment dialog =
                new SuggestionEditConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("contactUri", contactLookupUri);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "edit");
    }

    public static class SuggestionEditConfirmationDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.aggregation_suggestion_edit_dialog_title)
                    .setMessage(R.string.aggregation_suggestion_edit_dialog_message)
                    .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ContactEditorFragment targetFragment =
                                        (ContactEditorFragment) getTargetFragment();
                                Uri contactUri =
                                        getArguments().getParcelable("contactUri");
                                targetFragment.doEditSuggestedContact(contactUri);
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }
    }

    /**
     * Abandons the currently edited contact and switches to editing the suggested
     * one, transferring all the data there
     */
    protected void doEditSuggestedContact(Uri contactUri) {
        if (mListener != null) {
            // make sure we don't save this contact when closing down
            mStatus = Status.CLOSING;
            mListener.onEditOtherContactRequested(
                    contactUri, mState.get(0).getContentValues());
        }
    }

    public void setAggregationSuggestionViewEnabled(boolean enabled) {
        if (mAggregationSuggestionView == null) {
            return;
        }

        LinearLayout itemList = (LinearLayout) mAggregationSuggestionView.findViewById(
                R.id.aggregation_suggestions);
        int count = itemList.getChildCount();
        for (int i = 0; i < count; i++) {
            itemList.getChildAt(i).setEnabled(enabled);
        }
    }

    /**
     * Computes bounds of the supplied view relative to its ascendant.
     */
    private Rect getRelativeBounds(View ascendant, View view) {
        Rect rect = new Rect();
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

        View parent = (View) view.getParent();
        while (parent != ascendant) {
            rect.offset(parent.getLeft(), parent.getTop());
            parent = (View) parent.getParent();
        }
        return rect;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_URI, mLookupUri);
        outState.putString(KEY_ACTION, mAction);

        if (hasValidState()) {
            // Store entities with modifications
            outState.putParcelable(KEY_EDIT_STATE, mState);
        }

        outState.putLong(KEY_RAW_CONTACT_ID_REQUESTING_PHOTO, mRawContactIdRequestingPhoto);
        outState.putParcelable(KEY_VIEW_ID_GENERATOR, mViewIdGenerator);
        if (mCurrentPhotoFile != null) {
            outState.putString(KEY_CURRENT_PHOTO_FILE, mCurrentPhotoFile.toString());
        }
        outState.putLong(KEY_CONTACT_ID_FOR_JOIN, mContactIdForJoin);
        outState.putBoolean(KEY_CONTACT_WRITABLE_FOR_JOIN, mContactWritableForJoin);
        outState.putLong(KEY_SHOW_JOIN_SUGGESTIONS, mAggregationSuggestionsRawContactId);
        outState.putBoolean(KEY_ENABLED, mEnabled);
        outState.putBoolean(KEY_NEW_LOCAL_PROFILE, mNewLocalProfile);
        outState.putBoolean(KEY_IS_USER_PROFILE, mIsUserProfile);
        outState.putInt(KEY_STATUS, mStatus);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mStatus == Status.SUB_ACTIVITY) {
            mStatus = Status.EDITING;
        }

        switch (requestCode) {
            case REQUEST_CODE_PHOTO_PICKED_WITH_DATA: {
                // Ignore failed requests
                if (resultCode != Activity.RESULT_OK) return;
                // As we are coming back to this view, the editor will be reloaded automatically,
                // which will cause the photo that is set here to disappear. To prevent this,
                // we remember to set a flag which is interpreted after loading.
                // This photo is set here already to reduce flickering.
                /*Modifyed for SWITCHUI-43*/
                //mPhoto = data.getParcelableExtra("data");
                mPhoto = BitmapFactory.decodeFile(mCropImageFile);
                /*Modifyed for SWITCHUI-43 end*/
                setPhoto(mRawContactIdRequestingPhoto, mPhoto);
                mRawContactIdRequestingPhotoAfterLoad = mRawContactIdRequestingPhoto;
                mRawContactIdRequestingPhoto = -1;

                break;
            }
            case REQUEST_CODE_CAMERA_WITH_DATA: {
                // Ignore failed requests
                if (resultCode != Activity.RESULT_OK) return;
                doCropPhoto(mCurrentPhotoFile);
                break;
            }
            case REQUEST_CODE_JOIN: {
                // Ignore failed requests
            	//begin add by txbv34 for IKCBSMMCPPRC-1431
            	if (resultCode == Activity.RESULT_CANCELED){
            		/*Intent editIntent = new Intent(mContext, ContactEditorActivity.class);
            		editIntent.setAction(ContactEditorActivity.ACTION_JOIN_COMPLETED);
            		mContext.startactivity(editIntent)*/
            		
            		mStatus = Status.LOADING;
                    getLoaderManager().restartLoader(LOADER_DATA, null, mDataLoaderListener);
            		
            		/*bindEditors();*/
            	}
            	//end add by txbv34 for IKCBSMMCPPRC-1431
                if (resultCode != Activity.RESULT_OK) return;
                if (data != null) {
                    final long contactId = ContentUris.parseId(data.getData());
                    joinAggregate(contactId);
                }
                break;
            }
            case REQUEST_CODE_ACCOUNTS_CHANGED: {
                // Bail if the account selector was not successful.
                if (resultCode != Activity.RESULT_OK) {
                    mListener.onReverted();
                    return;
                }
                // If there's an account specified, use it.
                if (data != null) {
                    AccountWithDataSet account = data.getParcelableExtra(Intents.Insert.ACCOUNT);
                    if (account != null) {
                        createContact(account);
                        return;
                    }
                }
                // If there isn't an account specified, then this is likely a phone-local
                // contact, so we should continue setting up the editor by automatically selecting
                // the most appropriate account.
                createContact();
                break;
            }
        }
    }

    /**
     * Sets the photo stored in mPhoto and writes it to the RawContact with the given id
     */
    private void setPhoto(long rawContact, Bitmap photo) {
        BaseRawContactEditorView requestingEditor = getRawContactEditorView(rawContact);
        if (requestingEditor != null) {
            requestingEditor.setPhotoBitmap(photo);
        } else {
            Log.w(TAG, "The contact that requested the photo is no longer present.");
        }
        /*Added for switchuitwo-576 begin*/
        bindEditors();
        /*Added for switchuitwo-576 end*/
    }

    /**
     * Finds raw contact editor view for the given rawContactId.
     */
    public BaseRawContactEditorView getRawContactEditorView(long rawContactId) {
        for (int i = 0; i < mContent.getChildCount(); i++) {
            final View childView = mContent.getChildAt(i);
            if (childView instanceof BaseRawContactEditorView) {
                final BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                if (editor.getRawContactId() == rawContactId) {
                    return editor;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if there is currently more than one photo on screen.
     */
    private boolean hasMoreThanOnePhoto() {
        int count = mContent.getChildCount();
        int countWithPicture = 0;
        for (int i = 0; i < count; i++) {
            final View childView = mContent.getChildAt(i);
            if (childView instanceof BaseRawContactEditorView) {
                final BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                if (editor.hasSetPhoto()) {
                    countWithPicture++;
                    if (countWithPicture > 1) return true;
                }
            }
        }

        return false;
    }

    /**
     * The listener for the data loader
     */
    private final LoaderManager.LoaderCallbacks<ContactLoader.Result> mDataLoaderListener =
            new LoaderCallbacks<ContactLoader.Result>() {
        @Override
        public Loader<ContactLoader.Result> onCreateLoader(int id, Bundle args) {
            mLoaderStartTime = SystemClock.elapsedRealtime();
            return new ContactLoader(mContext, mLookupUri);
        }

        @Override
        public void onLoadFinished(Loader<ContactLoader.Result> loader, ContactLoader.Result data) {
            final long loaderCurrentTime = SystemClock.elapsedRealtime();
            Log.v(TAG, "Time needed for loading: " + (loaderCurrentTime-mLoaderStartTime));
            if (!data.isLoaded()) {
                // Item has been deleted
                Log.i(TAG, "No contact found. Closing activity");
                if (mListener != null) mListener.onContactNotFound();
                return;
            }

            mStatus = Status.EDITING;
            mLookupUri = data.getLookupUri();
            final long setDataStartTime = SystemClock.elapsedRealtime();
            setData(data);
            final long setDataEndTime = SystemClock.elapsedRealtime();

            // If we are coming back from the photo trimmer, this will be set.
            if (mRawContactIdRequestingPhotoAfterLoad != -1) {
                setPhoto(mRawContactIdRequestingPhotoAfterLoad, mPhoto);
                mRawContactIdRequestingPhotoAfterLoad = -1;
                mPhoto = null;
            }
            Log.v(TAG, "Time needed for setting UI: " + (setDataEndTime-setDataStartTime));
        }

        @Override
        public void onLoaderReset(Loader<ContactLoader.Result> loader) {
        }
    };

    /**
     * The listener for the group meta data loader for all groups.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mContext, Groups.CONTENT_URI);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mGroupMetaData = data;
            bindGroupMetaData();
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    @Override
    public void onSplitContactConfirmed() {
        if (mState == null) {
            // This may happen when this Fragment is recreated by the system during users
            // confirming the split action (and thus this method is called just before onCreate()),
            // for example.
            Log.e(TAG, "mState became null during the user's confirming split action. " +
                    "Cannot perform the save action.");
            return;
        }

        mState.markRawContactsForSplitting();
        save(SaveMode.SPLIT);
    }

    //MOTO MOD BEGIN, IKPIM-899
    /**
     * Account was chosen in the selector. Create a RawContact for this account now
     */
    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) { //for pass build
        if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
            final int phoneType = SimUtility.getTypeByAccountName(account.name);
            if (!SimUtility.getSIMLoadStatus()) {
                // Card is loading
                createCardErrorDialog(R.string.loadsim_text, true).show();
            } else if (!SimUtility.isSimReady(phoneType)) {
                // Card is not available
                createCardErrorDialog(R.string.no_card_text, true).show();
            } else if (SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType) <= 0) {
                // Card is full
                createCardErrorDialog(R.string.card_no_space, true).show();
            } else {
                createContact(account);
            }
        } else {
            createContact(account);
        }
    }

    /**
     * The account selector has been aborted. If we are in "New" mode, we have to close now
     */
    @Override
    public void onAccountSelectorCancelled() {
        if (!hasValidState() && mListener != null) {
            mListener.onAccountSelectorAborted();
        }
    }
    //MOTO MOD END

    private final class PhotoEditorListener
            implements EditorListener, PhotoActionPopup.Listener {
        private final BaseRawContactEditorView mEditor;
        private final boolean mAccountWritable;
        /*2013-1-4, add by amt_sunzhao for SWITCHUITWO-441 */ 
        private ListPopupWindow mListPopupWindow = null;
        /*2013-1-4, add end*/ 

        private PhotoEditorListener(BaseRawContactEditorView editor, boolean accountWritable) {
            mEditor = editor;
            mAccountWritable = accountWritable;
        }

        @Override
        public void onRequest(int request) {
            if (!hasValidState()) return;

            if (request == EditorListener.REQUEST_PICK_PHOTO) {
                // Determine mode
                final int mode;
                if (mAccountWritable) {
                    if (mEditor.hasSetPhoto()) {
                        if (hasMoreThanOnePhoto()) {
                            mode = PhotoActionPopup.MODE_PHOTO_ALLOW_PRIMARY;
                        } else {
                            mode = PhotoActionPopup.MODE_PHOTO_DISALLOW_PRIMARY;
                        }
                    } else {
                        mode = PhotoActionPopup.MODE_NO_PHOTO;
                    }
                } else {
                    if (mEditor.hasSetPhoto() && hasMoreThanOnePhoto()) {
                        mode = PhotoActionPopup.MODE_READ_ONLY_ALLOW_PRIMARY;
                    } else {
                        // Read-only and either no photo or the only photo ==> no options
                        return;
                    }
                }
                /*2013-1-4, add by amt_sunzhao for SWITCHUITWO-441 */ 
                mListPopupWindow = PhotoActionPopup.createPopupMenu(mContext, mEditor.getPhotoEditor(), this, mode);
                if(null != mListPopupWindow) {
                	mListPopupWindow.show();
                }
            }
        }

        public ListPopupWindow getmListPopupWindow() {
			return mListPopupWindow;
		}
        /*2013-1-4, add end*/ 

		@Override
        public void onDeleteRequested(Editor removedEditor) {
            // The picture cannot be deleted, it can only be removed, which is handled by
            // onRemovePictureChosen()
        }

        /**
         * User has chosen to set the selected photo as the (super) primary photo
         */
        @Override
        public void onUseAsPrimaryChosen() {
            // Set the IsSuperPrimary for each editor
            int count = mContent.getChildCount();
            for (int i = 0; i < count; i++) {
                final View childView = mContent.getChildAt(i);
                if (childView instanceof BaseRawContactEditorView) {
                    final BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                    final PhotoEditorView photoEditor = editor.getPhotoEditor();
                    photoEditor.setSuperPrimary(editor == mEditor);
                }
            }
        }

        /**
         * User has chosen to remove a picture
         */
        @Override
        public void onRemovePictureChosen() {
//MOTO MOD BEGIN
            int numRawContacts = mState.size();
            if(mEditor.getPhotoEditor() != null){
                mEditor.getPhotoEditor().resetData();
            }
            else{
                return;
            }

            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
            for (int i = 0; i < numRawContacts; i++) {
                // TODO ensure proper ordering of entities in the list
                final EntityDelta entity = mState.get(i);
                final ValuesDelta values = entity.getValues();
                if (!values.isVisible()) continue;
                final long rawContactId = values.getAsLong(RawContacts._ID);
                final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
                final String dataSet = values.getAsString(RawContacts.DATA_SET);
                final AccountType type = accountTypes.getAccountType(accountType, dataSet);
                if(mEditor.getRawContactId() == rawContactId){
                	//IKCBSMMCPPRC-1622 & 1634 & 1584
//                    mEditor.setState(entity, type, mViewIdGenerator, isEditingUserProfile());
                    mEditor.getPhotoEditor().setEditorListener(
                            new PhotoEditorListener(mEditor, type.areContactsWritable()));
                     return;
                }
            }
//MOTO MOD END
            mEditor.setPhotoBitmap(null);
        }

        /**
         * Launches Camera to take a picture and store it in a file.
         */
        @Override
        public void onTakePhotoChosen() {
            mRawContactIdRequestingPhoto = mEditor.getRawContactId();
            try {
                // Launch camera to take photo for selected contact
                PHOTO_DIR.mkdirs();
                mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());
                final Intent intent = getTakePickIntent(mCurrentPhotoFile);

                mStatus = Status.SUB_ACTIVITY;
                startActivityForResult(intent, REQUEST_CODE_CAMERA_WITH_DATA);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(mContext, R.string.photoPickerNotFoundText,
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Launches Gallery to pick a photo.
         */
        @Override
        public void onPickFromGalleryChosen() {
            mRawContactIdRequestingPhoto = mEditor.getRawContactId();
            try {
                // Launch picker to choose photo for selected contact
                final Intent intent = getPhotoPickIntent();
                mStatus = Status.SUB_ACTIVITY;
                startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(mContext, R.string.photoPickerNotFoundText,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    //MOTO MOD BEGIN
    public void onDestroy() {
        isDestroy = true;
        getLoaderManager().destroyLoader(LOADER_DATA);
        getLoaderManager().destroyLoader(LOADER_GROUPS);
        super.onDestroy();
    }
    //MOTO MOD END
}
