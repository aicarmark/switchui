/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/13 Smart Actions 2.1 Initial Version
 * XPR643        2012/08/07 Smart Actions 2.2 New architecture for data I/O
 */
package com.motorola.contextual.pickers.conditions.missedcall;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.pickers.ContactsChooserFragment;
import com.motorola.contextual.pickers.ContactsChooserFragment.ContactsInfo;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.PublisherLaunchHandler;

import com.motorola.contextual.smartrules.R;

/**
 * This activity presents a missed calls UI flow.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - Fragment display and transition activity
 *
 * RESPONSIBILITIES:
 *  Present a missed calls UI flow.
 *
 * COLLABORATORS:
 *  MissedCallsWhoFragment.java - Chooses the contacts to interact with
 *  MissedCallsNumberFragment.java - Chooses the threshold number of missed calls
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class MissedCallActivity extends MultiScreenPickerActivity
        implements MissedCallConstants, PublisherLaunchHandler, Constants, 
        ContactsChooserFragment.ContactsChooserCallback,
        MissedCallsWhoFragment.MissedCallsWhoCallback,
        MissedCallsNumberFragment.MissedCallsNumberCallback {
    protected static final String TAG = MissedCallActivity.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving phone numbers */
        String NUMBERS = "com.motorola.contextual.pickers.actions.KEY_NUMBERS";
        /** Saved instance state key for saving contact names */
        String NAMES = "com.motorola.contextual.pickers.actions.KEY_NAMES";
        /** Saved instance state key for saving whether each contact exists in phonebook */
        String PHONEBOOK_CONTACT_FLAGS = "com.motorola.contextual.pickers.actions.KEY_PHONEBOOK_CONTACT_FLAGS";
        /** Saved instance state key for saving contacts to populate ContactsChooserFragment */
        String CONTACTS_STRING = "com.motorola.contextual.pickers.actions.KEY_CONTACTS_STRING";
        /** Saved instance state key for saving list of phone numbers */
        String NUMBERS_LIST = "com.motorola.contextual.pickers.actions.KEY_NUMBERS_LIST";
        /** Saved instance state key for saving list of names */
        String NAMES_LIST = "com.motorola.contextual.pickers.actions.KEY_NAMES_LIST";
        /** Saved instance state key for saving threshold number of missed calls */
        String MISSED_CALLS = "com.motorola.contextual.pickers.actions.KEY_MISSED_CALLS";
    }

    /** Default threshold for number of missed calls to trigger rule actions */
    private static int DEFAULT_MISSED_CALLS = MissedCallsNumberFragment.Selection.DEFAULT_MISSED_CALLS.getNumberOfMissedCalls();

    /** Flag whether Activity is ready to finish */
    private boolean mIsDone = false;
    /** Flag to ensure that the Done button is processed only once */
    private boolean mIsFinishing = false;

    /**
     * Fragment holder and associated text.
     */
    private class FragmentInfo {
        /** Next fragment to launch after handling any prior fragment's return value */
        public Fragment frag = null;
        /** Next fragment's title question text resource ID */
        public int nameResId = -1;
    };
    private FragmentInfo mNextFragInfo = null;

    /** Contact names and phone numbers */
    private ContactsInfo mConInfo = null;

    /** Names list corresponding to incoming call numbers */
    private ArrayList<String> mNamesList = null;
    /** Incoming call numbers list */
    private ArrayList<String> mNumbersList = null;
    /** Threshold number of calls missed to triggering this rule condition; default 3 */
    private int mMissedCalls = DEFAULT_MISSED_CALLS;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.MissedCall_title));
        mNextFragInfo = new FragmentInfo();
        mConInfo = new ContactsInfo();
        if (savedInstanceState == null) {
            // Initial activity launch
            final Intent intent = getIntent();
            mNamesList = new ArrayList<String>();
            mNumbersList = new ArrayList<String>();

            // Launch first Fragment upon initial Activity launch
            if (intent == null) {
                onFirstConfig();
            } else {
                onEditConfig();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.NUMBERS, mConInfo.getPhoneNumbers());
        outState.putString(Key.NAMES, mConInfo.getNames());
        outState.putString(Key.PHONEBOOK_CONTACT_FLAGS, mConInfo.getKnownFlags());
        outState.putString(Key.CONTACTS_STRING, mConInfo.getContactsString());
        outState.putStringArrayList(Key.NUMBERS_LIST, mNumbersList);
        outState.putStringArrayList(Key.NAMES_LIST, mNamesList);
        outState.putInt(Key.MISSED_CALLS, mMissedCalls);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mConInfo.setPhoneNumbers(savedInstanceState.getString(Key.NUMBERS));
        mConInfo.setNames(savedInstanceState.getString(Key.NAMES));
        mConInfo.setKnownFlags(savedInstanceState.getString(Key.PHONEBOOK_CONTACT_FLAGS));
        mConInfo.setContactsString(savedInstanceState.getString(Key.CONTACTS_STRING));
        mNumbersList = savedInstanceState.getStringArrayList(Key.NUMBERS_LIST);
        mNamesList = savedInstanceState.getStringArrayList(Key.NAMES_LIST);
        mMissedCalls = savedInstanceState.getInt(Key.MISSED_CALLS, DEFAULT_MISSED_CALLS);
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onEditConfig() {
        // edit case
        String config = getIntent().getStringExtra(EXTRA_CONFIG);
        String numberConfig = null;

        if ((config != null) && (config.contains(MISSED_CALLS_CONFIG_STRING))) {
            if (LOG_DEBUG)
                Log.d(TAG,
                      " Edit " + config + ":"
                      + config.length());

            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            String value = profileConfig.getValue(MISSED_CALLS_NAME);
            if(value == null) return;
            config = value.replace(MISSED_CALLS_CONFIG_STRING, "");

            numberConfig = config.substring((OPEN_B).length(), config.indexOf(CLOSE_B));
            String frequency = config.substring(config.indexOf(CLOSE_B+OPEN_B)+(CLOSE_B+OPEN_B).length());
            frequency = frequency.substring(0, frequency.indexOf(CLOSE_B));
            try {
                // Extract threshold number of missed calls
                mMissedCalls = Integer.parseInt(frequency);
            } catch (NumberFormatException e) {
                // Use default threshold number of missed calls
                Log.w(TAG, "Invalid missed calls threshold. Using default: " +
                        String.valueOf(mMissedCalls));
            }

            if (LOG_INFO) {
                Log.i(TAG, " Edit - index : " + Integer.valueOf(frequency));
            }
            // If numberConfig equals ".*", the user chose "Any" option
            if (numberConfig.equals(MISSED_CALL_ANY_NUMBER)) {
                mConInfo.setContactsString(MISSED_CALL_ANY_NUMBER);
            } else {
                // The user has chosen a specific number previously.
                // The Edit URI stores the data as : (Name:Frequency-Number)
                // OR (.*:Frequency)
                extractNameAndNumber(numberConfig);
                // Populate names and phone numbers
                mConInfo.setContactsString(convertToContactsWidgetString(mNamesList, mNumbersList));
                // Determine whether each phone number belongs to a contact in phonebook
                mConInfo.computeKnownFlagsFromContactsString();
            }
        }

        // Existing rule action
        final MissedCallsWhoFragment.Selection sel = MissedCallsWhoFragment
                .Selection.fromContactsString(mConInfo.getContactsString());
        int checkedListItem = -1;
        if (sel != null) {
            checkedListItem = sel.ordinal();
        }
        
        launchNextFragment(MissedCallsWhoFragment.newInstance(R.string.missed_calls_question_who, checkedListItem), 
                R.string.missed_calls_question_who, true);
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onFirstConfig() {
        // New rule action
        launchNextFragment(MissedCallsWhoFragment.newInstance(R.string.missed_calls_question_who, -1), 
                R.string.missed_calls_question_who, true);
    }

    // Copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity
    /**
     *  Handler for asynchronous messages.
    */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FINISH_ACTIVITY: {
                    finish();
                    break;
                }
            }
        }
    };

    /**
     * Parses the CURRENT_MODE info from the edit URI and extracts the previously
     * configured names, phone numbers.
     *
     * @param nameList Names list; non-null
     * @param numList Phone numbers list; non-null
     * @param numberConfig Holds the edit information
     */
    private void extractNameAndNumber(final String numberConfig) {
        // Method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

       StringTokenizer st = new StringTokenizer(numberConfig, OR_STRING);
        if (LOG_DEBUG) {
            Log.d(TAG, "extractNameAndNumber : " + numberConfig);
        }
        while (st.hasMoreTokens()) {
            mNumbersList.add(st.nextToken());
        }
        mNamesList = getNamesFromNumbers(getApplicationContext(), mNumbersList);
    }

    /**
     * Builds name list from number list.
     *
     * @param context Context for operations
     * @param numberList List of phone numbers
     * @return nameList List of names
     */
    static ArrayList<String> getNamesFromNumbers(Context context, ArrayList<String> numberList) {
        // Method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        ArrayList<String> nameList = new ArrayList<String>();
        Cursor pCur = null;

        if((numberList.size() == 1) && (numberList.get(0).equals(MISSED_CALL_ANY_NUMBER))) {
            nameList.add(numberList.get(0));
        }
        // Take care of linking with contacts here
        for(String number : numberList) {
            try {
                pCur = context.getContentResolver().query(
                        Phone.CONTENT_URI, new String[] {Phone.CONTACT_ID, Phone.NUMBER, Phone.DISPLAY_NAME},
                        null,
                        null, null);

                boolean nameFound = false;
                if ((pCur != null) && (pCur.moveToFirst()))  {
                      do {
                          if (LOG_DEBUG)
                              Log.d(TAG, "extractNameAndNumber : " + pCur.getString(pCur.getColumnIndex(Phone.CONTACT_ID))
                                    + " : " + pCur.getString(pCur.getColumnIndex(Phone.NUMBER))
                                    + " : " + pCur.getString(pCur.getColumnIndex(Phone.DISPLAY_NAME)));

                          if(number.contains(convertNumber(pCur.getString(pCur.getColumnIndex(Phone.NUMBER))))) {
                              //Edited number for the Contact found.
                              nameList.add(pCur.getString(pCur.getColumnIndex(Phone.DISPLAY_NAME)));
                              nameFound = true;
                              break;
                          }
                      } while(pCur.moveToNext());
                      if(nameFound == false) {
                          nameList.add(number);
                      }
                } else {
                    nameList.add(number);
                }
            } catch (Exception e) {
                e.printStackTrace();
                nameList.add(number);
            } finally {
                if(pCur != null)
                    pCur.close();
            }

        }
        return nameList;
    }

    /**
     * Converts a number provided by the addressing widget to a
     * rule-friendly format.
     *
     * @param number Phone number from addressing widget
     * @return Converted phone number using rule format
     */
    private static String convertNumber(String number) {
        // Method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        // If the number and the name are same, dont need to extract network
        // portion
        number = PhoneNumberUtils.extractNetworkPortion(number);

        if (LOG_DEBUG) {
            Log.d(TAG, "FormatNumber after format : " + number);
        }

        if (number != null && number.length() > MAX_SUPPORTED_DIGITS_IN_NUMBER) {
            number = number.substring(
                         ((number.length() - MAX_SUPPORTED_DIGITS_IN_NUMBER)),
                         (number.length()));
        }

        number = AddressUtil.cleanUpPhoneNumber(number, AddressUtil.SPECIAL_CHAR);
        return number;
    }

    /**
     * Converts names and phone numbers to the contact widget string format.
     *
     * @param nameList Names list; non-null
     * @param numList Phone numbers list; non-null
     * @return Contacts widget line string; null if empty
     */
    private String convertToContactsWidgetString(final List<String> nameList, final List<String> numList) {
        // Adapted method updateWidget copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCall.
        String contactsStr = null;
        final StringBuilder sb = new StringBuilder();

        // Build phone numbers string
        for (int index = 0; index < numList.size(); index++) {
            sb.append(numList.get(index));
            sb.append(((index != numList.size() - 1) ? "," : ""));
        }
        mConInfo.setPhoneNumbers(sb.toString());

        // Build names string
        sb.setLength(0);
        for (int index = 0; index < nameList.size(); index++) {
            sb.append(nameList.get(index));
            sb.append(((index != nameList.size() - 1) ? "," : ""));
        }
        mConInfo.setNames(sb.toString());

        // Build contacts widget string
        sb.setLength(0);
        final List<String> contactsList = AddressUtil.getFormattedAddresses(
                mConInfo.getNames(), mConInfo.getPhoneNumbers(), StringUtils.COMMA_STRING);
        if ((contactsList != null) && !contactsList.isEmpty()) {
            for (final String contact : contactsList) {
                sb.append(contact).append(StringUtils.COMMA_STRING);
            }
            contactsStr = sb.toString();
        }
        return contactsStr;
    }

    /**
     * Handles return from AutoReplyTextWhoFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleMissedCallsWhoFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            final String position = (String) returnValue;
            final MissedCallsWhoFragment.Selection selection =
                    MissedCallsWhoFragment.Selection.valueOf(position);
            switch (selection) {
            case ALL_INCOMING:
            {
                mConInfo.setContactsString(MISSED_CALL_ANY_NUMBER);
                // Set up next fragment
                final int checkedListItem = MissedCallsNumberFragment.Selection.fromNumberOfMissedCalls(mMissedCalls).ordinal();
                mNextFragInfo.nameResId = R.string.missed_calls_question_num;
                mNextFragInfo.frag = MissedCallsNumberFragment.newInstance(mNextFragInfo.nameResId,
                        R.string.iam_done, checkedListItem);
                break;
            }
            case SPECIFIC_CONTACTS:
            {
                if (mConInfo.getContactsString() == null) {
                    mConInfo.computeContactsString();
                }
                mNextFragInfo.nameResId = R.string.missed_calls_question_contacts;
                mNextFragInfo.frag = ContactsChooserFragment.newInstance(mNextFragInfo.nameResId,
                        R.string.continue_prompt, mConInfo.getContactsString());
                break;
            }
            default:
                Log.w(TAG, "Unrecognized selection from "
                        + fragment.getClass().getSimpleName() + ": "
                        + selection.name());
                break;
            }
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from ContactsChooserFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleContactsChooserFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            mConInfo.setContactsString((String) returnValue);
            // Compute CSV lists for phone numbers, names, and known phonebook contacts flags
            mConInfo.computeCsvListsFromContactsString();

            // Set up next default fragment
            final int checkedListItem = MissedCallsNumberFragment.Selection.fromNumberOfMissedCalls(mMissedCalls).ordinal();
            mNextFragInfo.nameResId = R.string.missed_calls_question_num;
            mNextFragInfo.frag = MissedCallsNumberFragment.newInstance(mNextFragInfo.nameResId, R.string.iam_done, checkedListItem);
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from MissedCallsNumberFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleMissedCallsNumberFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            final String position = (String) returnValue;
            final MissedCallsNumberFragment.Selection selection =
                    MissedCallsNumberFragment.Selection.valueOf(position);
            mMissedCalls = selection.getNumberOfMissedCalls();
            mIsDone = true;
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Returns values from fragments.
     * Passes a fragment reference and its return value to container Activity.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    @Override
    public void onReturn(final Object returnValue, final PickerFragment fragment) {
        if (fragment == null) {
            Log.w(TAG, "null return fragment");
        } else if (returnValue == null) {
            Log.w(TAG, "null return value");
        } else {
            fragment.handleResult(returnValue, fragment);
        }

        // Launch next Fragment, pop Fragment, or set result and finish Activity
        if (mNextFragInfo.frag == null) {
            if (mIsDone) {
                // Don't process the Done button more than once
                if (!mIsFinishing) {
                    mIsFinishing = true;
                    // Compute and set Activity result in the background before finish
                    launchSaveThread();
                } else {
                    if (LOG_DEBUG) {
                        Log.d(TAG, "Invalid event. Done pressed multiple times in quick succession");
                    }
                }

            } else {
                // Pop Fragment off back stack, or finish if no back stack
                final FragmentManager fragmentMan = getFragmentManager();
                if (fragmentMan.getBackStackEntryCount() > 0) {
                    fragmentMan.popBackStack();
                } else {
                    Log.e(TAG, "Bad state: Cannot return activity result");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        } else {
            // Display next Fragment
            launchNextFragment(mNextFragInfo.frag, mNextFragInfo.nameResId, false);
            mNextFragInfo.frag = null;
            mNextFragInfo.nameResId = -1;
        }
    }

    /**
     * Starts background thread to compute result and finish.
     */
    private void launchSaveThread() {
        // Adapted method setupSaveBtn() copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        String phoneName = "";
        String phoneNumber = "";
        if (mConInfo.getContactsString().equals(MISSED_CALL_ANY_NUMBER)) {
            mNumbersList.clear();
            mNamesList.clear();
            mNumbersList.add(MISSED_CALL_ANY_NUMBER);
            mNamesList.add(MISSED_CALL_ANY_NUMBER);
        } else {
            // Specific people
            phoneName = AddressUtil.getNamesAsString(mConInfo.getContactsString(), MISSED_CALL_NUMBER_SEPARATOR);
            phoneNumber = AddressUtil.getNumbersAsString(mConInfo.getContactsString(), MISSED_CALL_NUMBER_SEPARATOR);
        }
        // Construct the rule to send to the rules builder
        final Thread t = new Thread(new FormatNumber(phoneNumber, phoneName));
        t.start();
    }

    /**
     * Enum of data types for FormatNumber.
     */
    private static enum DataType {
        NAME, NUMBER;
    }

    /**
     * This class implements the runnable for normalizing a phone number of format
     * 847-123-2344 to 8471232344. Digits in excess of the final 10 are stripped off.
     * For example, 919123456789 is stripped down to 9123456789.
     * Since such formatting is involved, they are handled in a separate thread.
     */
    private final class FormatNumber implements Runnable, Constants {
        // Adapted class copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        /** Phone numbers list */
        private String mFormatNumber;
        /** Names list */
        private String mFormatName;

        /**
         * Constructs with given phone numbers and corresponding names.
         *
         * @param number Phone numbers list; non-null
         * @param name Names list; non-null
         */
        private FormatNumber(final String numbers, final String names) {
            mFormatNumber = numbers;
            mFormatName = names;
        }

        /**
         * Fills the names and phone numbers lists for use in rule creation.
         *
         * @param formatString Data in a string list
         * @param type Data type
         */
        void fillArray(final String stringList, final DataType type) {
            String formatString = stringList;
            int startIndex = 0;
            int currentIndex = 0;
            // Add all data items except the final one
            while ((currentIndex = formatString.indexOf(MISSED_CALL_NUMBER_SEPARATOR)) > -1) {
                final String dataStr = formatString.substring(startIndex, currentIndex);
                if (type == DataType.NUMBER) {
                    if (LOG_DEBUG) {
                        Log.d(TAG, "Parsed number : " + dataStr);
                    }
                    mNumbersList.add(dataStr);
                } else {
                    mNamesList.add(dataStr);
                }
                formatString = formatString.substring(currentIndex + 1);
                if (LOG_DEBUG) {
                    Log.d(TAG, "Remaining number : " + formatString);
                }
            }
            // Add final data item
            if (type == DataType.NUMBER) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "Parsed number : " + formatString);
                }
                mNumbersList.add(formatString);
            } else {
                mNamesList.add(formatString);
            }
        }

        /**
         * Required by Runnable interface.
         * Formats phone numbers and corresponding names.
         */
        public final synchronized void run() {
            if ((!mNumbersList.isEmpty()
                        && !mNumbersList.get(0).equals(MISSED_CALL_ANY_NUMBER))
                    || mNumbersList.isEmpty()) {
                mNumbersList.clear();
                mNamesList.clear();
                fillArray(mFormatName, DataType.NAME);
                fillArray(mFormatNumber, DataType.NUMBER);
            }
            // Construct the rules and send the results
            // back to the rules builder.
            sendResults();
            if(mHandler != null) {
                mHandler.sendEmptyMessage(FINISH_ACTIVITY);
            }
        }
    }

    /**
     * Sends XML string to the rules builder.
     */
    private void sendResults() {
        // Adapted method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        Intent returnIntent = new Intent();
        StringBuilder allNumbersBuf = new StringBuilder();

        StringBuilder descBuffer = new StringBuilder();
        descBuffer.append(mMissedCalls).append(BLANK_SPC);
        if (mMissedCalls == 1)
            descBuffer.append(getString(R.string.call));
        else
            descBuffer.append(getString(R.string.calls));

        descBuffer.append(BLANK_SPC)
        .append(getString(R.string.missed_call_from)).append(BLANK_SPC);
        descBuffer = getDescBuffer(getApplicationContext(), descBuffer, mNamesList, mNumbersList);

        int size = mNumbersList.size();
        for (int index = 0; index < size; index++) {

            if (mNumbersList.get(index).equals(MISSED_CALL_ANY_NUMBER)) {
                allNumbersBuf.append(mNumbersList.get(index));
            } else {
                // The set of all numbers concatenated are needed for rule
                // editing functionality
                allNumbersBuf
                .append(mNumbersList.get(index))
                .append(index != (size - 1) ? OR_STRING : "");
            }
        }

        String allNumbers = allNumbersBuf.toString();

        populateIntentFields(returnIntent, descBuffer,
                             allNumbers, mMissedCalls);
    }

    /**
     * Generates the description string.
     *
     * @param context Context for operations
     * @param descBuffer Result buffer to contain description; non-null
     * @param nameArray List of names
     * @param numberArray List of phone numbers
     * @return Same description buffer
     */
    static final StringBuilder getDescBuffer(final Context context, final StringBuilder descBuffer,
            final ArrayList<String> nameArray, final ArrayList<String> numberArray) {
        // Method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        String orSplitString = BLANK_SPC + context.getString(R.string.or);
        int size = numberArray.size();
        int validCount = 0;
        for (int index = 0; index < size; index++) {
            if (!Util.isDuplicate(nameArray, index)) {
                validCount++;
            }
        }
        if (validCount <= 2) {
            for (int index = 0; index < size; index++) {
                if (!Util.isDuplicate(nameArray, index)) {
                    descBuffer
                    .append((numberArray.get(index)
                             .equals(MISSED_CALL_ANY_NUMBER)) ? context.getString(R.string.call_from_any_number)
                            : nameArray.get(index)).append(
                                index != (size - 1) ? orSplitString + BLANK_SPC : "");
                }
            }
        } else {
            descBuffer.append(nameArray.get(0)).append(orSplitString)
            .append(BLANK_SPC).append(validCount - 1).append(BLANK_SPC)
            .append(context.getString(R.string.others));
        }
        return descBuffer;
    }

    /**
     * Populates the fields of the result Intent for Condition Builder module,
     * and sets the activity result.
     *
     * @param returnIntent Result intent; non-null
     * @param descBuffer Description
     * @param allNumbers Phone numbers
     * @param frequency Threshold number of missed calls
     */
    private final void populateIntentFields(final Intent returnIntent,
                                            final StringBuilder descBuffer,
                                            final String allNumbers,
                                            final int frequency) {
        // Method copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallActivity

        String newDescription = descBuffer.toString();
        String orString = BLANK_SPC + getString(R.string.or);
        String orStringWithSpaceAtEnd = orString + BLANK_SPC;
        if (newDescription.endsWith(orStringWithSpaceAtEnd)) {
            newDescription = newDescription
                             .substring(0,
                                        (newDescription.length() - orStringWithSpaceAtEnd
                                         .length()));
        } else if (newDescription.endsWith(orString)) {
            newDescription = newDescription.substring(0,
                             (newDescription.length() - orString.length()));
        }

        String config = MISSED_CALLS_CONFIG_STRING+OPEN_B+allNumbers+CLOSE_B+OPEN_B+frequency+CLOSE_B;
        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        profileConfig.addNameValuePair(MISSED_CALLS_CONFIG_VERSION, MISSED_CALLS_VERSION);

        returnIntent.putExtra(EXTRA_CONFIG, profileConfig.getConfigString());
        returnIntent.putExtra(EXTRA_DESCRIPTION, newDescription);
        setResult(RESULT_OK, returnIntent);
    }
}
