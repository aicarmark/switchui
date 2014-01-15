/*
 * @(#)MissedCallActivity.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2010/11/26 NA                Initial version
 * a18491        2011/3/16  IKINTNETAPP-52    Missed Call reset functionality
 *                                            updation
 * a18491        2011/4/15  IKINTNETAPP-179   Multiple contact support of
 *                                            missed call precondition
 * a21034        2011/5/9   IKINTNETAPP-208   Minor fixes around Recent call list activity
 *                                            launch intent
 *
 */
package com.motorola.contextual.smartprofile.sensors.missedcallsensor;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.MenuItem;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressEditTextView;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.commonutils.chips.AddressValidator;
import com.motorola.contextual.commonutils.chips.RecipientAdapter;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/*
 * Missed Call Constants
 */
interface MissedCallConstants {

    String MISSED_CALLS_CONFIG_PERSISTENCE = "MissedCallConfig";

    String MISSED_CALLS_PUB_KEY = "com.motorola.contextual.smartprofile.missedcallsensor";

    String EXTRA_MISSED_CALLS_ACTION = "com.motorola.contextual.smartrules.intent.extra.MISSED_CALLS_ACTION";

    String MISSED_CALLS_MAX_ID_PERSISTENCE = "MissedCallMaxId";

    String MISSED_CALLS_OBSERVER_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCallObserverStateMonitor";

    String MISSED_CALLS_CONFIG_STRING = "MissedCall=";

    String MISSED_CALLS_NAME = "MissedCall";

    String MISSED_CALLS_VERSION = "1.0";

    String MISSED_CALLS_CONFIG_VERSION = Constants.CONFIG_VERSION;

    String OPEN_B = "(";
    String CLOSE_B = ")";

    int NUMBER_EXTRACTED = 1;
    int FINISH_ACTIVITY = 2;

    String MISSED_CALLS_OLD_COUNT_SEPARATOR = ":";
    String MISSED_CALLS_OLD_NAME_NUMBER_SEPARATOR = "-";
    String MISSED_CALLS_OLD_NUMBER_SEPARATOR = "\\[";
    String MISSED_CALL_ANY_NUMBER = ".*";
    String POSSIBLE_VALUE_ONE = "1";
    String MISSED_CALL_NUMBER_SEPARATOR = "[";

    // MAX_SUPPORTED_DIGITS_IN_NUMBER - This constraint is needed for matching
    // the rule.
    // Eg : If the number from contacts is more than 10 digits, and if we
    // include all the digits in the rule, the rule may not match if
    // the incoming number is equal to 10 digits.
    // Eg : If the number from contacts, during configuration is
    // 91-1234567890, and the incoming number is just 1234567890,
    // the rule will not match, hence stripping is needed
    int MAX_SUPPORTED_DIGITS_IN_NUMBER = 10;

    // Constants related to the mode description
    String MISSED_CALL_DESCRIPTION = "Missed Calls";
}
/**
 * This class displays options for MissedCall precondition and allows the user
 * to chose the number and the number of times missed call to be received before
 * firing the rule.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements View.OnClickListener for buttons                                             ,
 *      Implements MissedCallConstants
 *
 * RESPONSIBILITIES:
 * This class displays options for MissedCall precondition and allows the user to chose the number and
 * the number of times missed call to be received before firing the rule.
 *
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class MissedCallActivity extends Activity implements Constants,
    View.OnClickListener, MissedCallConstants {

    /**
     * mNumberArray - The incoming call number list
     */
    private ArrayList<String> mNumberArray = new ArrayList<String>();

    /**
     * mNameArray - The name corresponding to the incoming call number
     */
    private ArrayList<String> mNameArray = new ArrayList<String>();

    /**
     * mFrequency - Number of times the missed call to be received from the
     * above number before triggering the rule.
     */
    private String mFrequency = POSSIBLE_VALUE_ONE;

    /**
     * Member radio buttons and number edit text box. Need to be
     * enabled/disabled based on the combination of other buttons/radio buttons'
     * selection. This is to make sure the user enters only valid combinations.
     */
    private RadioButton mAllCallsBtn;
    private RadioButton mSelectedNosBtn;

    /**
     * Member variables to hold relative layouts. These are needed to select the
     * entire row in the layout, rather than just radio buttons.
     */
    private RelativeLayout mAllCallsLayout;
    private RelativeLayout mSelectedNumbersLayout;

    private final static String TAG = MissedCallActivity.class.getSimpleName();

    /**
     * mWidget - Addressing widget needed for displaying the name/number in the
     * form of Addressing button
     */
    private MultiAutoCompleteTextView mToView;
    private RecipientAdapter mAddressAdapterTo;

    private Spinner mSpinner;

    /**
     * Boolean to be set to true on clicking done button.
     * This is to ensure that done button is clicked only once
     */
    private boolean isFinishing = false;
    /**
     * Boolean to be set to true when onSaveInstanceState is called.
     */
    private boolean mDisableActionBar = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.missedcall);

        // Handle various UI elements
        mSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.frequency_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new FrequencyOnItemSelectedListener());
        mSpinner.setSelection(2);

        mToView = (MultiAutoCompleteTextView)findViewById(R.id.to);
        mToView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mToView.setValidator(new AddressValidator());
        mToView.setThreshold(1);
        mAddressAdapterTo = new RecipientAdapter(this, (AddressEditTextView) mToView);
        mToView.setAdapter(mAddressAdapterTo);
        mToView.setHint(getString(R.string.touch_here_to_add_contacts));
        mToView.setVisibility(View.GONE);

        mAllCallsBtn = (RadioButton) findViewById(R.id.all_incoming_calls_button);
        mAllCallsBtn.setOnClickListener(this);

        mAllCallsLayout = (RelativeLayout) findViewById(R.id.all_incoming_calls_layout);
        mAllCallsLayout.setClickable(true);
        mAllCallsLayout.setOnClickListener(this);

        mSelectedNumbersLayout = (RelativeLayout) findViewById(R.id.selected_numbers_layout);
        mSelectedNumbersLayout.setClickable(true);
        mSelectedNumbersLayout.setOnClickListener(this);

        mSelectedNosBtn = (RadioButton) findViewById(R.id.selected_numbers_button);
        mSelectedNosBtn.setOnClickListener(this);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.MissedCall);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();

        // Precondition edit case.
        Intent incomingIntent = getIntent();
        handleIncomingIntent(incomingIntent);


        mToView.addTextChangedListener(new TextWatcher() {


            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enableSaveButton();
            }


            public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
            }


            public void afterTextChanged(Editable s) {
            }
        });

        enableSaveButton();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_INFO) Log.i(TAG, "onSaveInstanceState");
        mDisableActionBar = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOG_INFO) Log.i(TAG, "onResume");
        mDisableActionBar = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mToView.getWindowToken(), 0);
        mDisableActionBar = true;
    }

    /**
     * onOptionsItemSelected() handles the key presses in ICS action bar items.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_save:
            if (LOG_INFO)
                Log.i(TAG, "OK button clicked");
            if (!isFinishing) {
                isFinishing = true;
                setupSaveBtn();
            } else {
                if (LOG_DEBUG) Log.d(TAG, "Invalid event. Done pressed multiple times in quick succession");
            }
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
    }

    /**
     * This method sets up visibility for the action bar items.
     *
     * @param enableSaveButton
     *            - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction()
        .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    private void handleIncomingIntent(Intent incomingIntent) {
        if (incomingIntent != null) {

            String config = incomingIntent.getStringExtra(EXTRA_CONFIG);
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
                mFrequency = config.substring(config.indexOf(CLOSE_B+OPEN_B)+(CLOSE_B+OPEN_B).length());

                mFrequency = mFrequency.substring(0, mFrequency.indexOf(CLOSE_B));

                mSpinner.setSelection(Integer.valueOf(mFrequency) - 1);

                if (LOG_INFO)
                    Log.i(TAG, " Edit - index : " + Integer.valueOf(mFrequency));
                // If tempString contains ".*", the user has chosen "Any" option
                // previously
                if (numberConfig.equals(MISSED_CALL_ANY_NUMBER)) {

                    mAllCallsBtn.setChecked(true);

                } else {

                    // The user has chosen a specific number previously.
                    // The Edit URI stores the data as : (Name:Frequency-Number)
                    // OR (.*:Frequency)
                    mSelectedNosBtn.setChecked(true);

                    // Request focus and show the soft keyboard
                    mToView.setVisibility(View.VISIBLE);
                    mToView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.showSoftInput(mToView,
                                           InputMethodManager.SHOW_IMPLICIT);

                    
                    Thread t = new Thread(new ExtractNameAndNumber(numberConfig));
                    t.start();
                    
                }

            } else
                mAllCallsBtn.setChecked(true);

        } else {

            // First time launch, select 'All incoming calls' option by default
            mAllCallsBtn.setChecked(true);
        }

    }

    /**
     *  Defines the handler which receives the asynchronous message after querying database
     *  for the available list of playlists.
    */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            case NUMBER_EXTRACTED: {
	                updateWidget();
	                break;
	            }
	            case FINISH_ACTIVITY: {
	                finish();
	                break;
	            }
            }
        }
    };

    private final class ExtractNameAndNumber implements Runnable, Constants {

        private String mConfig;

        /**
         * Constructor which stores the number.
         *
         * @param number
         */
        private ExtractNameAndNumber(final String config) {
            mConfig = config;
        }

        /**
         * Run method to implement formatting of the number
         */
        public final void run() {

            extractNameAndNumber(mConfig);
            if(mHandler != null)
                mHandler.sendEmptyMessage(NUMBER_EXTRACTED);
        }

        /**
         * This method parses the CURRENT_MODE info from the edit URI and extracts
         * the previously configured device name and the address This also updates
         * the necessary lists for updating in the list view.
         *
         * @param tempStr
         *            - String which holds the edit information The required
         *            information is extracted from this. This is not copied locally
         *            and can not be final.
         */
        private final void extractNameAndNumber(String numberConfig) {
            StringTokenizer st = new StringTokenizer(numberConfig, OR_STRING);

            if (LOG_DEBUG)
                Log.d(TAG, "extractNameAndNumber : " + numberConfig);

            while (st.hasMoreTokens()) {
                mNumberArray.add(st.nextToken());
            }

            mNameArray = getNamesFromNumbers(getApplicationContext(), mNumberArray);
        }
    }

    /**
     * The method gets name list from number list
     * @param context
     * @param numberList
     * @return nameList
     */
    public static final ArrayList<String> getNamesFromNumbers(Context context, ArrayList<String> numberList) {

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
     * Sets up the Save Button and its listeners
     */
    private void setupSaveBtn() {
        String phoneName = "";
        String phoneNumber = "";

        if (mSelectedNosBtn.isChecked()) {

            String inputContacts = mToView.getText().toString();
            phoneName = AddressUtil.getNamesAsString(inputContacts, MISSED_CALL_NUMBER_SEPARATOR);
            phoneNumber = AddressUtil.getNumbersAsString(inputContacts, MISSED_CALL_NUMBER_SEPARATOR);

            // Dont allow the user save if number is not
            // entered, throw a toast
            if (Util.isZeroLengthString(phoneName)
                    && Util.isZeroLengthString(phoneNumber)) {

                Toast.makeText(MissedCallActivity.this, getString(R.string.no_number),
                               Toast.LENGTH_SHORT).show();
                mToView.requestFocus();
                isFinishing = false;
                return;
            }

            if (LOG_DEBUG)
                Log.d(TAG, "ok - final " + phoneName + ":" + phoneNumber);
        } else if (mAllCallsBtn.isChecked()) {
            mNumberArray.clear();
            mNameArray.clear();
            mNumberArray.add(MISSED_CALL_ANY_NUMBER);
            mNameArray.add(MISSED_CALL_ANY_NUMBER);
        }

        // Format the number to prepare to construct the rule and send to the
        // rules builder.
        Thread t = new Thread(new FormatNumber(phoneNumber, phoneName));
        t.start();
    }

    /**
     * This method updates the Addressing widget with the number/name details so
     * the addressing buttons are updated. Addressing widget needs the name and
     * numbers to be formatted as comma seperated strings. The names and numbers
     * from mNameArray and mNumberArray are formatted accordingly and populed in
     * the widget using populateFromNumbers
     */
    void updateWidget() {

        String numbers = "";
        String names = "";

        int size = mNumberArray.size();
        if (LOG_DEBUG)
            Log.d(TAG, " Edit - Number - " + size + ": Name -"
                  + mNameArray.size());

        StringBuilder buf = new StringBuilder();
        for (int index = 0; index < size; index++) {
            buf.append(mNumberArray.get(index));
            buf.append(((index != size - 1) ? "," : ""));
        }
        numbers = buf.toString();

        buf = new StringBuilder();
        size = mNameArray.size();
        for (int index = 0; index < size; index++) {
            buf.append(mNameArray.get(index));
            buf.append(((index != size - 1) ? "," : ""));
        }

        names = buf.toString();

        if(!numbers.isEmpty() && !names.isEmpty()) {
            List<String> addressList = AddressUtil.getFormattedAddresses(names, numbers, StringUtils.COMMA_STRING);
            if (!addressList.isEmpty()) {
                for (String address : addressList) {
                    mToView.append(address + StringUtils.COMMA_STRING);
                }
            }
        }

        if (LOG_DEBUG)
            Log.d(TAG, " Edit - Number - " + numbers + ": Name -" + names);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     *
     * Implement onItemSelected of OnItemSelectedListener in order to retrieve
     * user selected frequency
     *
     */
    private final class FrequencyOnItemSelectedListener implements
        OnItemSelectedListener {

        public final void onItemSelected(AdapterView<?> parent, View view,
                                         int pos, long id) {

            if (!String.valueOf(pos + 1).equals(mFrequency)) {
                mFrequency = String.valueOf(pos + 1);
            }

            if (LOG_INFO)
                Log.i(TAG,
                      " FrequencyOnItemSelectedListener : onItemSelected - pos "
                      + pos + ": Frequency - " + mFrequency);
        }

        public final void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    /**
     * Implement onClick of View.OnClickListener This is used to handle
     * different buttons in the screen
     */
    public final void onClick(View v) {

    	InputMethodManager imm = null;
    	
        switch (v.getId()) {

        case R.id.all_incoming_calls_button:
        case R.id.all_incoming_calls_layout:

            mToView.setVisibility(View.GONE);

            // Hide the soft keyboard
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(mToView.getWindowToken(), 0);

            // All incoming calls button selected.
            if (LOG_INFO)
                Log.i(TAG, " all_incoming_calls_button ");

            mAllCallsBtn.setChecked(true);

            // Enable "Save", disable "Selected Numbers"
            mSelectedNosBtn.setChecked(false);
            enableSaveButton();
            break;
        case R.id.selected_numbers_button:
        case R.id.selected_numbers_layout:

            // Request focus and show the soft keyboard
            mToView.setVisibility(View.VISIBLE);
            mToView.requestFocus();
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(mToView,
                                   InputMethodManager.SHOW_IMPLICIT);


            // "Selected numbers" button selected.
            mSelectedNosBtn.setChecked(true);

            // Enable "Save" button, disable "All Numbers"
            mAllCallsBtn.setChecked(false);
            enableSaveButton();
            break;
        }
    }

    /**
     * This method converts the number provided by the addressing widget to a
     * rule friendly form
     *
     * @param number
     * @return converted number
     */
    static String convertNumber(String number) {

        // If the number and the name are same, dont need to extract network
        // portion
        number = PhoneNumberUtils.extractNetworkPortion(number);

        if (LOG_DEBUG)
            Log.d(TAG, "FormatNumber after format : " + number);

        if (number != null && number.length() > MAX_SUPPORTED_DIGITS_IN_NUMBER) {
            number = number.substring(
                         ((number.length() - MAX_SUPPORTED_DIGITS_IN_NUMBER)),
                         (number.length()));
        }

        number = AddressUtil.cleanUpPhoneNumber(number, AddressUtil.SPECIAL_CHAR);
        return number;
    }

    /**
     * This class implements the runnable for formatting the number The number
     * from the DB will be of the format : 847-123-2344 Needs to be formatted to
     * 8471232344. The max number of digits considered is last 10. The excess is
     * stripped off. Eg : 919123456789 is stripped to 9123456789. Since
     * formatting is involved, handled in a different thread.
     *
     */
    private final class FormatNumber implements Runnable, Constants {

        private String mFormatNumber;
        private String mFormatName;

        private final static int NAME = 1;
        private final static int NUMBER = 2;

        /**
         * Constructor which stores the number.
         *
         * @param number
         */
        private FormatNumber(final String number, final String name) {
            mFormatNumber = number;
            mFormatName = name;
        }

        /**
         * This method fills the name and number storage array, which will be
         * further used in rule creation
         *
         * @param formatString
         * @param type
         */
        void fillArray(String formatString, int type) {
            int startIndex = 0, currentIndex = 0;
            while (formatString.contains(MISSED_CALL_NUMBER_SEPARATOR)) {
                currentIndex = formatString.indexOf(MISSED_CALL_NUMBER_SEPARATOR);

                if (type == NUMBER) {

                    String myNumber = formatString.substring(startIndex,
                                      currentIndex);

                    if (LOG_DEBUG)
                        Log.d(TAG, "Parsed number : " + myNumber);

                    mNumberArray.add(myNumber);

                } else {
                    mNameArray.add(formatString.substring(startIndex,
                                                          currentIndex));
                }
                formatString = formatString.substring((currentIndex + 1));

                if (LOG_DEBUG)
                    Log.d(TAG, "Remaining number : " + formatString);
            }

            if (type == NUMBER) {

                if (LOG_DEBUG)
                    Log.d(TAG, "Parsed number : " + formatString);

                mNumberArray.add(formatString);

            } else {
                mNameArray.add(formatString);
            }

        }

        /**
         * Run method to implement formatting of the number
         */
        public final void run() {

            if ((!mNumberArray.isEmpty())
                    && (!mNumberArray.get(0).equals(MISSED_CALL_ANY_NUMBER))
                    || (mNumberArray.isEmpty())) {
                mNumberArray.clear();
                mNameArray.clear();
                fillArray(mFormatName, NAME);
                fillArray(mFormatNumber, NUMBER);

            }
            // We are all set, construct the rules and send the results
            // back to the rules builder.
            sendResults();
            if(mHandler != null)
                mHandler.sendEmptyMessage(FINISH_ACTIVITY);
        }

    }

    /**
     * Generates the description string
     *
     * @param context
     * @param descBuffer
     * @param nameArray
     * @param numberArray
     * @return - the description buffer
     */
    public static final StringBuilder getDescBuffer(Context context, StringBuilder descBuffer,
            ArrayList<String> nameArray, ArrayList<String> numberArray) {
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
     * Populates the fields of Intent returned to Condition Builder module.
     *
     * @param returnIntent
     * @param descBuffer
     * @param allNumbers
     * @param frequency
     */
    private final void populateIntentFields(Intent returnIntent,
                                            StringBuilder descBuffer,
                                            String allNumbers, int frequency) {
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

    /**
     * Send XML string to the rules builder
     *
     * @param None
     * @return None
     */
    private final void sendResults() {
        Intent returnIntent = new Intent();
        StringBuilder allNumbersBuf = new StringBuilder();

        StringBuilder descBuffer = new StringBuilder();
        descBuffer.append(mFrequency).append(BLANK_SPC);
        if (Integer.valueOf(mFrequency) == 1)
            descBuffer.append(getString(R.string.call));
        else
            descBuffer.append(getString(R.string.calls));

        descBuffer.append(BLANK_SPC)
        .append(getString(R.string.missed_call_from)).append(BLANK_SPC);
        descBuffer = getDescBuffer(getApplicationContext(), descBuffer, mNameArray, mNumberArray);

        int size = mNumberArray.size();
        for (int index = 0; index < size; index++) {

            if (mNumberArray.get(index).equals(MISSED_CALL_ANY_NUMBER)) {
                allNumbersBuf.append(mNumberArray.get(index));
            } else {
                // The set of all numbers concatenated are needed for rule
                // editing functionality
                allNumbersBuf
                .append(mNumberArray.get(index))
                .append(index != (size - 1) ? OR_STRING : "");
            }
        }

        String allNumbers = allNumbersBuf.toString();

        populateIntentFields(returnIntent, descBuffer,
                             allNumbers, Integer.valueOf(mFrequency));
    }

    /**
     * Enables the save button when a set of conditions are met
     */
    private void enableSaveButton() {
        setupActionBarItemsVisibility(mAllCallsBtn.isChecked()
                                      || (mSelectedNosBtn.isChecked() && !StringUtils.isTextEmpty(mToView.getText())));
    }
}
