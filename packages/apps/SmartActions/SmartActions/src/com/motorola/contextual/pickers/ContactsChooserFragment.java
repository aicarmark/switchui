/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/23 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressEditTextView;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.commonutils.chips.AddressValidator;
import com.motorola.contextual.commonutils.chips.RecipientAdapter;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a contacts chooser.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a contacts chooser.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class ContactsChooserFragment extends PickerFragment implements OnClickListener, AdapterView.OnItemLongClickListener, TextWatcher {
    protected static final String TAG = ContactsChooserFragment.class.getSimpleName();

    /**
     * Contact names and numbers.
     */
    public static class ContactsInfo {
        /** User-selected contacts list string that is compatible with the contacts widget */
        private String mContactsString = null;
        /** Contact phone numbers to respond to */
        private String mPhoneNumbers = null;
        /** Contact names corresponding to the phone numbers list */
        private String mNames = null;
        /** Corresponding flags that indicate phonebook contacts */
        private String mKnownFlags = null;

        public void setContactsString(final String contacts) {
            mContactsString = contacts;
        }
        public String getContactsString() {
            return mContactsString;
        }
        /**
         * Computes and sets contacts widget-compatible string from internally
         * set names and phone numbers.
         *
         * @return true if computed from non-null CSV lists for names and phone numbers
         * @return false if null CSV list for names or phone numbers
         */
        public boolean computeContactsString() {
            boolean isValid = false;
            if ((mNames != null) && (mPhoneNumbers != null)) {
                mContactsString = ContactUtil.buildContactsString(mNames, mPhoneNumbers);
                isValid = true;
            } else {
                mContactsString = null;
            }
            return isValid;
        }

        public void setPhoneNumbers(final String numbers) {
            mPhoneNumbers = numbers;
        }
        public String getPhoneNumbers() {
            return mPhoneNumbers;
        }

        public void setNames(final String names) {
            mNames = names;
        }
        public String getNames() {
            return mNames;
        }

        public void setKnownFlags(final String knownFlags) {
            mKnownFlags = knownFlags;
        }
        public String getKnownFlags() {
            return mKnownFlags;
        }

        /**
         * Computes and sets CSV lists for phone numbers, names, and known
         * phonebook contacts flags from internally set contacts widget string.
         *
         * @return true if computed from non-null contacts widget string
         * @return false if null contacts string
         */
        public boolean computeCsvListsFromContactsString() {
            boolean isValid = false;
            if (mContactsString != null) {
                // Extract phone numbers CSV list
                mPhoneNumbers = AddressUtil.getNumbersAsString(mContactsString, StringUtils.COMMA_STRING);
                // Extract names list
                mNames = AddressUtil.getNamesAsString(mContactsString, StringUtils.COMMA_STRING);
                // Determine whether each phone number belongs to a contact in phonebook
                mKnownFlags = AddressUtil.getKnownFlagsAsString(mContactsString, StringUtils.COMMA_STRING);
                isValid = true;
            } else {
                mPhoneNumbers = null;
                mNames = null;
                mKnownFlags = null;
            }
            return isValid;
        }

        /**
         * Computes and sets CSV list for known phonebook contacts flags from
         * internally set contacts widget string.
         *
         * @return true if computed from non-null contacts widget string
         * @return false if null contacts string
         */
        public boolean computeKnownFlagsFromContactsString() {
            boolean isValid = false;
            if (mContactsString != null) {
                // Determine whether each phone number belongs to a contact in phonebook
                mKnownFlags = AddressUtil.getKnownFlagsAsString(mContactsString, StringUtils.COMMA_STRING);
                isValid = true;
            } else {
                mKnownFlags = null;
            }
            return isValid;
        }
    }

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving title */
        String TITLE = "com.motorola.contextual.pickers.actions.KEY_TITLE";
        /** Saved instance state key for saving title resource ID */
        String TITLE_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_TITLE_RESOURCE_ID";
        /** Saved instance state key for saving button text resource ID */
        String BUTTON_TEXT_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_BUTTON_TEXT_RESOURCE_ID";
        /** Saved instance state key for saving contacts list */
        String CONTACTS = "com.motorola.contextual.pickers.actions.KEY_CONTACTS";
    }

    /** Title question for this Fragment */
    private String mTitle = null;

    /** Title question resource ID for this Fragment, if it exists */
    private int mTitleResourceId = -1;

    /** Selected contacts list view */
    protected ListView mListView = null;

    /** Confirmation button text resource ID */
    private int mButtonTextResourceId = R.string.continue_prompt;

    /** List view adapter that populates the user-selected contacts */
    private BaseAdapter mAdapter = null;

    /** Positive confirmation button to save and dismiss this fragment */
    private Button mButtonPositive = null;

    /** Contacts suggestions edit line widget */
    private MultiAutoCompleteTextView mContactsEditLineView = null;

    /** Contacts suggestions edit line autocomplete list adapter */
    private RecipientAdapter mAddressAdapter = null;

    /** Contacts input string first passed into the fragment */
    private String mInitContactsString = null;

    /** Contact data that populates the list view */
    private List<ListItem> mItems = null;

    /** Corresponding individual contact input strings */
    protected List<String> mContactsStrings = null;

    /** Corresponding individual contact phone number strings */
    protected List<String> mContactNumbers = null;

    /** Contact list item context menu */
    private AlertDialog mAlertDialog = null;

    public interface ContactsChooserCallback {
        void handleContactsChooserFragment(Fragment fragment, Object returnValue);
    }

    private ContactsChooserCallback mContactsChooserCallback;

    private static final String TITLE_RES_ID = "TITLE_RES_ID";
    private static final String BUTTON_TEXT_RES_ID = "BUTTON_TEXT_RES_ID";
    private static final String CONTACTS_STRING = "CONTACTS_STRING";

    public static ContactsChooserFragment newInstance(final int titleResId, final int buttonTextResId,
            final String contactsString) {

        Bundle args = new Bundle();
        args.putInt(TITLE_RES_ID, titleResId);
        args.putInt(BUTTON_TEXT_RES_ID, buttonTextResId);
        args.putString(CONTACTS_STRING, contactsString);

        ContactsChooserFragment f = new ContactsChooserFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mContactsChooserCallback = (ContactsChooserCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement ContactsChooserCallback");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mTitleResourceId = args.getInt(TITLE_RES_ID, -1);
            mButtonTextResourceId = args.getInt(BUTTON_TEXT_RES_ID, -1);
            mInitContactsString = args.getString(CONTACTS_STRING);
        }
    }

    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mContactsChooserCallback.handleContactsChooserFragment(fragment, returnValue);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (mContentView == null) {
            // If a title resource ID was supplied, always retrieve directly from resources
            if (mTitleResourceId >= 0) {
                mTitle = getString(mTitleResourceId);
            }

            // Contacts list data
            mItems = new ArrayList<ListItem>();
            // Corresponding contacts strings for result
            mContactsStrings = new ArrayList<String>();
            // Corresponding contacts phone numbers for duplicate exclusion
            mContactNumbers = new ArrayList<String>();

            // Create the actual view to show
            mContentView = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(mTitle))
                    .setNoChoiceItems(mItems, null, this, R.layout.contacts_chooser_list_item)
                    .setPositiveButton(mButtonTextResourceId, this)
                    .create()
                    .getView();
            // Configure the button enabled state
            mButtonPositive = (Button) mContentView.findViewById(R.id.button1);
            mButtonPositive.setEnabled(!mItems.isEmpty());

            // Retrieve the list view
            mListView = (ListView) mContentView.findViewById(R.id.list);
            // Retrieve the list view adapter
            mAdapter = (BaseAdapter) mListView.getAdapter();

            // Show the contacts completion area
            mContentView.findViewById(R.id.contacts_edit_line).setVisibility(View.VISIBLE);

            // Configure the contacts edit line widget
            mContactsEditLineView = (MultiAutoCompleteTextView) mContentView.findViewById(R.id.contacts_completion);
            mContactsEditLineView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
            mContactsEditLineView.setValidator(new AddressValidator());
            mContactsEditLineView.setThreshold(1);
            mAddressAdapter = new RecipientAdapter(mHostActivity, (AddressEditTextView) mContactsEditLineView);
            mContactsEditLineView.setAdapter(mAddressAdapter);
            mContactsEditLineView.setHint(getString(R.string.touch_to_add_contacts));
            // see onDestroy for .removeTextChangedListener
            mContactsEditLineView.addTextChangedListener(this);
            // Edit text field should *always* explicitly request focus for this chooser.
            // In 4.0+, given fragments A and B, assuming A is some fragment nth and B follows A, A starting B will
            // not invoke the soft keyboard. Hence, we will show it implicitly (not SHOW_FORCED) here.
            mContactsEditLineView.requestFocus();
            InputMethodManager inputMgr = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMgr.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Populate initial contacts list, if any
        addInitialContacts();
    }

    /**
     * Dismisses any dialogs.
     *
     * @see android.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        hideSoftInputKeyboard();
    }

    /**
     * Dissociate listeners.
     *
     * @see android.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mContactsEditLineView != null) {
            mContactsEditLineView.removeTextChangedListener(this);
        }
        super.onDestroy();
    }

    /**
     * Restores prior instance state in onCreateView.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mTitle = savedInstanceState.getString(Key.TITLE);
        mTitleResourceId = savedInstanceState.getInt(Key.TITLE_RESOURCE_ID, -1);
        mButtonTextResourceId = savedInstanceState.getInt(Key.BUTTON_TEXT_RESOURCE_ID, -1);
        mInitContactsString = savedInstanceState.getString(Key.CONTACTS);
    }

    /**
     * Saves instance state before this fragment is eligible to be discarded.
     * <P>
     * Note that this is generally not called when a Fragment is added to
     * the back stack. Instead, member variables that are referenced in
     * onCreateView() to restore state should at the very least be updated to
     * reflect the current state just before it is added to a back stack.
     *
     * @param outState Bundle in which to save the current state
     * @see android.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.TITLE, mTitle);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleResourceId);
        outState.putInt(Key.BUTTON_TEXT_RESOURCE_ID, mButtonTextResourceId);
        outState.putString(Key.CONTACTS, getContactsList());
    }

    /**
     * Parses initial contacts to populate list.
     */
    private void addInitialContacts() {
        final String inputContacts = mInitContactsString;
        if (!TextUtils.isEmpty(inputContacts)) {
            // Separate individual names
            final String phoneName = AddressUtil.getNamesAsString(inputContacts, ContactUtil.ContactDelimiter.SEPARATOR);
            final String[] names = phoneName.split(ContactUtil.ContactDelimiter.SEPARATOR);

            // Separate individual numbers
            final String phoneNumber = AddressUtil.getNumbersAsString(inputContacts, ContactUtil.ContactDelimiter.SEPARATOR);
            final String[] numbers = phoneNumber.split(ContactUtil.ContactDelimiter.SEPARATOR);

            // Construct individual name-number pairs for processing
            for (int index = numbers.length - 1; index >= 0; --index) {
                final String num = numbers[index];
                final boolean hasPhone = !num.trim().isEmpty();
                if (hasPhone) {
                    final String name = names[index];
                    addContact(ContactUtil.getContactString(name, num));
                }
            }
        }
    }

    /**
     * Constructs result contacts input string list.
     *
     * @return Contact input string list
     */
    public String getContactsList() {
        final StringBuilder sb = new StringBuilder();
        if (mContactsStrings != null) {
            for (final String contact : mContactsStrings) {
                sb.append(contact);
            }
        }
        return sb.toString();
    }

    /**
     * Required by TextWatcher interface.
     * Does nothing on text change.
     *
     * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
     */
    public void onTextChanged(final CharSequence s, final int start, final int before,
            final int count) {
    }

    /**
     * Required by TextWatcher interface.
     * Does nothing before text change.
     *
     * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
     */
    public void beforeTextChanged(final CharSequence s, final int start, final int count,
            final int after) {
    }

    /**
     * Required by TextWatcher interface.
     * After text change, rejects invalid contact widget input and
     * processes valid data for a single contact. Finally, clears the text
     * to prepare for the next contact entry.
     *
     * @param s Editable object
     * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
     */
    public void afterTextChanged(final Editable s) {
        final String str = s.toString();
        final String strTrim = str.trim();
        final boolean isEmptyStrTrim = strTrim.isEmpty();
        if (strTrim.startsWith(ContactUtil.ContactDelimiter.SEPARATOR)) {
            s.delete(0, ContactUtil.ContactDelimiter.SEPARATOR.length());
        } else if (!isEmptyStrTrim && strTrim.endsWith(ContactUtil.ContactDelimiter.SEPARATOR)) {
            final String inputContact = mContactsEditLineView.getText().toString();
            addContact(inputContact);
            s.clear();
        } else if (isEmptyStrTrim && !str.isEmpty()) {
            s.clear();
        }
    }

    /**
     * Required by OnClickListener interface.
     * Handles click of button.
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     */
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            onPositiveButtonClicked();
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            //Do nothing for contact list items
            break;
        }
    }

    /**
     * Required by AdapterView.OnItemLongClickListener interface.
     * Handles long click of contact list item by showing an option to delete.
     *
     * @param parent The AbsListView where the click happened
     * @param view The view within the AbsListView that was clicked
     * @param position The position of the view in the list
     * @param id The row id of the item that was clicked
     * @return true if long click was consumed; otherwise, false
     * @see android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget.AdapterView, android.view.View, int, long)
     */
    public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        showDeleteContextMenu(view, position);
        return true;
    }

    /**
     * Shows delete context menu for contact item in list.
     *
     * @param v List item view
     * @param position List item position
     */
    private void showDeleteContextMenu(final View v, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mHostActivity);
        final TextView label = (TextView) v.findViewById(R.id.list_item_label);
        builder.setTitle(label.getText());

        final CharSequence[] items = {
                getString(R.string.delete)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            /**
             * Handles contact list item delete selection.
             *
             * @param dialog Dialog that received the click
             * @param which Position of the clicked list item
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(final DialogInterface dialog, final int which) {
                // Remove contact from list
                mItems.remove(position);
                // Remove corresponding contacts string result
                mContactsStrings.remove(position);
                // Remove corresponding contact phone number
                mContactNumbers.remove(position);
                // Update contacts list
                refreshContactsList();
            }
        });
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    /**
     * Handles button click by returning contacts string list.
     */
    private void onPositiveButtonClicked() {
        // Ensure that all state member variable are current,
        // just in case this Fragment is added to the back stack.
        mInitContactsString = getContactsList();
        mHostActivity.onReturn(getContactsList(), this);
    }

    /**
     * Converts contact widget input to a contact, and add to list if valid.
     *
     * @param inputContacts Contacts widget string
     */
    private void addContact(final String inputContact) {
        // Use consolidated intern String reference
        final String input = inputContact.intern();
        // Populate contact info in background thread
        new ContactLookupTask().execute(input);
    }

    /**
     * Refreshes contact list.
     */
    protected void refreshContactsList() {
        // Refresh contact list
        mAdapter.notifyDataSetChanged();
        // Enable finish button, if needed
        mButtonPositive.setEnabled(!mItems.isEmpty());
    }

    /**
     * Hide soft input keyboard, if shown.
     */
    public void hideSoftInputKeyboard() {
        UIUtils.hideSoftInputKeyboard(mHostActivity, mContactsEditLineView);
    }

    /**
     * Asynchronously lookup contact information and then add to UI.
     */
    private class ContactLookupTask extends AsyncTask<String,Void,Boolean> {
        /** Contact widget string */
        private String mWidgetString = null;

        /** Phone number extracted from contact widget string */
        private String mPhoneNumber = null;

        /**
         * Contact name from phonebook, if available;
         * otherwise, name from contact widget string.
         * */
        private String mName = null;

        /** Contact photo URI, if available from phonebook */
        private Uri mImageUri = null;

        /**
         * UI phone number line; if available from phonebook,
         * the phone type label is prepended.
         */
        private String mPhoneText = null;

        /** List view contact information row item */
        private ListItem mContactRowItem = null;

        private int mType = 0;
        private String mCustomLabel = null;

        /**
         * Constructs default.
         */
        public ContactLookupTask() {
        }

        /**
         * Retrieves contact information in background thread.
         *
         * @param contactWidgetStrings Contact widget string
         * @return true if valid phone number
         */
        @Override
        protected Boolean doInBackground(final String... contactWidgetStrings) {
            // check if fragment is still attached otherwise just return.
            if (!ContactsChooserFragment.this.isAdded()) return false;

            Boolean isValid = Boolean.FALSE;
            if ((contactWidgetStrings == null) || (contactWidgetStrings.length == 0)) {
                return isValid;
            }
            mWidgetString = contactWidgetStrings[0];
            // Extract contact name
            mName = AddressUtil.getNamesAsString(mWidgetString, ContactUtil.ContactDelimiter.SEPARATOR);
            // Extract phone number
            mPhoneNumber = AddressUtil.getNumbersAsString(mWidgetString, ContactUtil.ContactDelimiter.SEPARATOR);
            // Determine whether an actual phone number was supplied
            final boolean hasPhone = !mPhoneNumber.trim().isEmpty();
            if (hasPhone) {
                isValid = Boolean.valueOf(hasPhone);
                // Determine whether the phone number belongs to a known contact
                final String knownFlags = AddressUtil.getKnownFlagsAsString(mWidgetString, ContactUtil.ContactDelimiter.SEPARATOR);
                final boolean isContact = knownFlags.trim().equals(ContactUtil.CONTACT_KNOWN);
                // If contact is known, look up the photo URI
                if (isContact) {
                    lookupContactInfo();
                } else {
                    mPhoneText = mPhoneNumber;
                    mName = "";
                }
            }
            return isValid;
        }

        /**
         * Retrieves contact information from the contacts content provider.
         */
        private void lookupContactInfo() {
            // check if fragment is still attached otherwise just return.
            if (!ContactsChooserFragment.this.isAdded()) return;

            final Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(mPhoneNumber));
            final String[] proj = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER,
                    ContactsContract.PhoneLookup.TYPE,
                    ContactsContract.PhoneLookup.LABEL,
                    ContactsContract.Contacts.PHOTO_URI
            };
            final String select = null;
            final String[] selArgs = null;
            final String sort = null;
            final Cursor mCursor = mHostActivity.getContentResolver().query(uri, proj, select, selArgs, sort);
            if (mCursor != null) {
                try {
                    if (mCursor.moveToFirst()) {
                        mName = mCursor.getString(mCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        mPhoneNumber = mCursor.getString(mCursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER));
                        mType = mCursor.getInt(mCursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.TYPE));
                        mCustomLabel = mCursor.getString(mCursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LABEL));
                        final String uriString = mCursor.getString(mCursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));

                        // Convert to Uri from String
                        try {
                            mImageUri = Uri.parse(uriString);
                        } catch (final NullPointerException e) {
                            Log.w(TAG, "Null icon image Uri for item " + mCursor.getPosition());
                        }
                    }
                } catch (Exception ex) {

                } finally {
                    mCursor.close();
                }
            } else {
                Log.e(TAG, "null contacts cursor");
            }
        }

        /**
         * Adds contact row in UI thread.
         *
         * @param isValid True if valid phone number
         */
        @Override
        protected void onPostExecute(final Boolean isValid) {
            // check if fragment is still attached otherwise just return.
            if (!ContactsChooserFragment.this.isAdded()) return;

            if (isValid.booleanValue()) {

                Resources res = null;
                CharSequence typeStr = "";

                try {
                    // Convert phone number type to string resource
                    res = getActivity().getResources();
                    typeStr = ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, mType, mCustomLabel);
                } catch (Exception ex) {
                    Log.d(TAG, "Could not get resources.");
                }

                // Construct list item secondary text description
                // containing phone type and phone number

                final StringBuilder sbDesc = new StringBuilder();
                sbDesc.append(getString(R.string.contacts_type_markup_begin));
                sbDesc.append(typeStr);
                sbDesc.append(getString(R.string.contacts_type_markup_end));
                sbDesc.append(getString(R.string.contacts_type_number_separator));
                sbDesc.append(mPhoneNumber);
                mPhoneText = sbDesc.toString();

                // Construct the contact list view item
                if (mImageUri == null) {
                    mContactRowItem = new ListItem(R.drawable.ic_contact_picture,
                            mName, mPhoneText, ListItem.typeTHREE,
                            null, null);
                } else {
                    mContactRowItem = new ListItem(mName, mPhoneText, ListItem.typeTHREE,
                            null, null, mImageUri);
                }

                // Add contact to list, if needed
                final String number = mPhoneNumber.intern();
                if (mContactNumbers.contains(number)) {
                    // Scroll to existing contact
                    if (mListView != null) {
                        mListView.smoothScrollToPosition(
                                mContactNumbers.indexOf(number));
                    }
                } else {
                    // Add new contact to beginning of list
                    mItems.add(0, mContactRowItem);
                    // Add corresponding contacts string result
                    mContactsStrings.add(0, ContactUtil.getContactString(mName, mPhoneNumber.replace("-", "")));
                    // Add corresponding contact phone number
                    mContactNumbers.add(0, number);
                    // Scroll list to added contact
                    if (mListView != null) {
                        mListView.smoothScrollToPosition(0);
                        refreshContactsList();
                    }
                }


            }
        }
    }
}
