/*
 * @(#)IncomingCall.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2010/11/26 IKINTNETAPP-61    Initial version
 *
 */
package  com.motorola.contextual.smartprofile.sensors.incomingcallsensor;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;
//import com.motorola.contextual.address.AddressingLayout;
import com.motorola.contextual.smartprofile.util.Util;

//Note: Addressing widget has been removed from the codebase.
//      Chips UI needs to be integrated to this precondition for it to work.


/**
 * Rule related strings used for constructing rule
 */
interface IncomingCallRuleConstants {

    // Uri used for editing the precondition
    String INCOMING_CALL_CONNECT_URI_TO_FIRE_STRING = "#Intent;action=android.intent.action.EDIT;" +
            "component=com.motorola.contextual.smartrules/com.motorola.contextual.smartprofile.sensors.incomingcallsensor.IncomingCall;S.CURRENT_MODE=";

    // Incoming Call VS
    String INCOMING_CALL_VIRTUAL_SENSOR_STRING = "com.motorola.contextual.IncomingCall";


    // Phone state "ringing" related rules
    String PHONE_STATE_RINGING_PART1 = "#Intent;action=android.intent.action.PHONE_STATE;.*S.incoming_number=(.*";
    String PHONE_STATE_RINGING_PART2 = ");.*S.state=RINGING.*end ";

    // Phone state "idle" related rules
    String PHONE_STATE_IDLE_PART1 = "THEN #Intent;action=android.intent.action.PHONE_STATE;.*S.incoming_number=(.*";
    String PHONE_STATE_IDLE_PART2 = ");.*S.state=IDLE.*end ";

    // Phone state "offhook" related rules
    String PHONE_STATE_OFFHOOK_PART1 = "THEN #Intent;action=android.intent.action.PHONE_STATE;";
    String PHONE_STATE_OFFHOOK_PART2 = ".*S.state=OFFHOOK.*end ";

    // Constants related to the rule edition
    String PARAMETER_STRING = ";p0=true";
    String INCOMING_CALL_END_STRING = ";end)";
    String INCOMING_CALL_NUMBER_SEPARATOR = "[";
    String INCOMING_CALL_NAME_NUMBER_SEPARATOR = "]";

    // Constants related to the rule description
    String INCOMING_CALL_DESCRIPTION = "Incoming Calls";
    String INCOMING_CALL_DESCRIPTION_PART_1 = "When ";
    String INCOMING_CALL_ANY_NUMBER = ".*";
    String INCOMING_CALL_ANY_NUMBER_TOKEN = "1";
    String CONTACTS_PICK_INTENT = "android.intent.action.PICK";

    // MAX_SUPPORTED_DIGITS_IN_NUMBER - This constraint is needed for matching the rule.
    // Eg : If the number from contacts is more than 10 digits, and if we
    // include all the digits in the rule, the rule may not match if
    // the incoming number is equal to 10 digits.
    // Eg : If the number from contacts, during configuration is
    // 91-1234567890, and the incoming numbet is just 1234567890,
    // the rule will not match, hence stripping is needed
    int MAX_SUPPORTED_DIGITS_IN_NUMBER = 10;
    int  PICK_CONTACT = 1;

    String RULE_TRIGGER_STRING = " TRIGGERS #vsensor;name=com.motorola.contextual.IncomingCall";
    String TRUE_STRING = ";p0=true;end";
    String FALSE_STRING = ";p0=false;end";

}

/**
 * This class displays options for IncomingCall precondition and allows the user to chose the number
 * the incoming call to be received from before firing the rule.
 * This also constructs the rule for IncomingCall precondition and passes to the Rules Builder
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements View.OnClickListener for buttons                                             ,
 *      Implements IncomingCallRuleConstants
 *
 * RESPONSIBILITIES:
 * This class displays options for IncomingCall precondition and allows the user to chose the number
 * the incoming call to be received from before firing the rule.
 * This also constructs the rule for IncomingCall precondition and passes to the Rules Builder
 * Note : Separation of UI functionality from rule construction may not be needed here.
 * Because UI independent, formatted "number" is used for the rule construction.
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
public final class IncomingCall extends Activity implements Constants,
    View.OnClickListener,
    IncomingCallRuleConstants {

    /**
     * mNumberArray - The incoming call number list
     */
    private ArrayList<String> mNumberArray = new ArrayList<String>();


    /**
     * mNameArray - The name corresponding to the incoming call number
     *
     */
    private ArrayList<String> mNameArray = new ArrayList<String>();


    /**
     * Member buttons. Need to be enabled/disabled based on the
     * combination of other buttons/radio buttons' selection.
     * This is to make sure the user enters only valid combinations.
     */
    //private Button mSaveBtn;
    //private Button mCancelBtn;



    /**
     * Member radio buttons and number edit text box.
     * Need to be enabled/disabled based on the
     * combination of other buttons/radio buttons' selection.
     * This is to make sure the user enters only valid combinations.
     */
    private RadioButton mAllCallsBtn;
    private RadioButton mSelectedNosBtn;


    /**
     * Member variables to hold relative layouts.
     * These are needed to select the entire row in the layout, rather
     * than just radio buttons.
     */
    private RelativeLayout mAllCallsLayout;
    private RelativeLayout mSelectedNumbersLayout;
    private LinearLayout   mAddressWidget;

    /**
     * mWidget - Addressing widget needed for displaying the name/number in
     *           the form of Addressing button
     */
//    private AddressingLayout mWidget;

    private final static String TAG = IncomingCall.class.getSimpleName();


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incomingcall);

        // Handle various UI elements
        setTitle(R.string.IncomingCall);

        ViewStub stub = (ViewStub)findViewById(R.id.recipients_editor_smartprofile_stub);
//        if (stub != null) {
//            mWidget = (AddressingLayout) stub.inflate().
//                      findViewById(R.id.recipients_editor_smartprofile);
//        } else {
//            mWidget = (AddressingLayout)findViewById(R.id.recipients_editor_smartprofile);
//        }
//        mWidget.clearContent();

        mAddressWidget = (LinearLayout)findViewById(R.id.address_widget_lo);
        mAddressWidget.setVisibility(View.GONE);

        mAllCallsBtn = (RadioButton)findViewById(R.id.all_incoming_calls_button);
        mAllCallsBtn.setOnClickListener(this);

        mSelectedNosBtn = (RadioButton)findViewById(R.id.selected_numbers_button);
        mSelectedNosBtn.setOnClickListener(this);

        mAllCallsLayout = (RelativeLayout)findViewById(R.id.all_incoming_calls_layout);
        mAllCallsLayout.setClickable(true);
        mAllCallsLayout.setOnClickListener(this);

        mSelectedNumbersLayout= (RelativeLayout)findViewById(R.id.selected_numbers_layout);
        mSelectedNumbersLayout.setClickable(true);
        mSelectedNumbersLayout.setOnClickListener(this);

        // Commented out for ICS
/*        ActionBar abWidget = (ActionBar)findViewById(R.id.abwidget);
        abWidget.setVisibility(View.VISIBLE);
        setupSaveBtn();
        mCancelBtn = new Button(this);
        mCancelBtn.setOnClickListener(this);
        mCancelBtn.setText(getString(R.string.cancel));

        // set the listener for the cancel button
        mCancelBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        abWidget.addButton(mSaveBtn);
        abWidget.addButton(mCancelBtn);
*/
        // Precondition edit case.
        Intent incomingIntent = getIntent();
        handleIncomingIntent(incomingIntent);


    }

    private void handleIncomingIntent(Intent incomingIntent) {
        if (incomingIntent != null) {

            String info = incomingIntent.getStringExtra(CURRENT_MODE);
            if(info != null) {
                if (LOG_DEBUG) Log.d(TAG, " Edit " + info +  ":" + info.length());

                //mSaveBtn.setEnabled(false);
                // If tempString contains ".*", the user has chosen "Any" option
                // previously
                if(info.contains(INCOMING_CALL_ANY_NUMBER)) {

                    mAllCallsBtn.setChecked(true);

                } else {

                    // The user has chosen a specific number previously.
                    // The Edit URI stores the data as : (Name:Number) OR (.*:)
                    // Extract the name
                    mSelectedNosBtn.setChecked(true);

//                    mWidget.clearContent();
//                    mWidget.getAutoComplete().requestFocus();

                    InputMethodManager imm1 = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                    if(imm1 != null)
//                        imm1.showSoftInput(mWidget.getAutoComplete(), InputMethodManager.SHOW_IMPLICIT);
                    mAddressWidget.setVisibility(View.VISIBLE);


                    try {

                        extractNameAndNumber(info);
                        updateWidget();

                        // Extract Number
                        //mSaveBtn.setEnabled(true);

                    } catch (Exception e) {
                        Log.e(TAG, "Incoming Call Edit Exception");
                    }

                }
            } else
                mAllCallsBtn.setChecked(true);

        } else {

            // First time launch, select 'All incoming calls' option by default
            mAllCallsBtn.setChecked(true);
        }

    }
    /**
     * Sets up the Save Button and its listeners
     */
    @SuppressWarnings("unused")
	private void setupSaveBtn()
    {
        /*mSaveBtn = new Button(this);
        mSaveBtn.setText(getString(R.string.save));
        //set the listener for the save button
        mSaveBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // "OK" button selected.
                // Explicitly call LoseFocus function of AddressingLayout to
                // convert any text in the widget to address.
                mWidget.onFocusLost(mWidget.getAutoComplete());

                String phoneName = "";
                String phoneNumber = "";

                if (mSelectedNosBtn.isChecked()) {

                    phoneName = mWidget.getNamesAsString("[");
                    phoneNumber = mWidget.getNumbersAsString("[");

                    if (LOG_INFO) Log.i(TAG, "ok - final " + phoneName + ":" + phoneNumber);

                    // Dont allow the user save if number is not
                    // entered, throw a toast
                    if(Util.isZeroLengthString(phoneName)&& Util.isZeroLengthString(phoneNumber)) {

                        Toast.makeText(IncomingCall.this,
                                       getString(R.string.no_number),Toast.LENGTH_SHORT).show();
                        mWidget.onGainFocus(mWidget.getAutoComplete());
                        return;

                    }

                } else if (mAllCallsBtn.isChecked()) {
                    mNumberArray.clear();
                    mNameArray.clear();
                    mNumberArray.add(INCOMING_CALL_ANY_NUMBER);
                    mNameArray.add(INCOMING_CALL_ANY_NUMBER);
                }
                // Format the number to prepare to construct the rule and send to the
                // rules builder.
                Thread t = new Thread(new FormatNumber(phoneNumber, phoneName));
                t.start();
            }
        });
	*/

    }


    /**
     * This method updates the Addressing widget with the number/name details
     * so the addressing buttons are updated.
     */
    void updateWidget ( ) {

        String numbers = "";
        String names = "";

        if (LOG_INFO) Log.i(TAG, " Edit - Number - "  + mNumberArray.size() + ": Name -" + mNameArray.size());

        StringBuffer buf = new StringBuffer();
        for(int index=0; index<mNumberArray.size(); index++) {
            buf.append(mNumberArray.get(index));
            buf.append(((index!=mNumberArray.size()-1) ? "," : ""));
        }
        numbers = buf.toString();

        buf = new StringBuffer();
        for(int index=0; index<mNameArray.size(); index++) {
            buf.append(mNameArray.get(index));
            buf.append(((index!=mNameArray.size()-1) ? "," : ""));
        }

        names = buf.toString();

//        mWidget.populateFromNumbers(names, numbers, ",");

        if (LOG_INFO) Log.i(TAG, " Edit - Number - "  + numbers + ": Name -" + names);
    }

    /**
     * This method parses the CURRENT_MODE info from the edit URI and
     * extracts the previously configured name and the number
     * @param tempStr - String which holds the edit information
     * 		        The required information is extracted from this.
     * 		        This is not copied locally and can not be final.
     *  @return Returns the number of names/numbers configured
     */
    private final void extractNameAndNumber (String tempStr) {


        String name = "";
        int index = 0;


        if(LOG_DEBUG) Log.d(TAG, "extractNameAndNumber: " + tempStr);

        tempStr = tempStr.replaceAll(REM_REGEX_TO_REPLACE, REM_REGEX_TO_BE_REPLACED);
        tempStr = tempStr.replaceAll(SEM_REGEX_TO_REPLACE, SEM_REGEX_TO_BE_REPLACED);
        tempStr = tempStr.replaceAll(HASH_REGEX_TO_REPLACE, HASH_REGEX_TO_BE_REPLACED);

        // Parse through the string looking for the name separator :
        // " or " (is from strings.xml R.string.or)
        String orSplitString = BLANK_SPC + getString(R.string.or) + BLANK_SPC;
        while(tempStr.contains(orSplitString)) {

            name = tempStr.substring(0, tempStr.indexOf(orSplitString));

            if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: name : " + name);

            name = name.replaceAll(COLON_REGEX_TO_REPLACE, COLON_REGEX_TO_BE_REPLACED);
            name = name.replaceAll(HYPHEN_REGEX_TO_REPLACE, HYPHEN_REGEX_TO_BE_REPLACED);

            // Add the device name to the device list and select list
            mNameArray.add(name);

            index = (tempStr.indexOf(orSplitString) + orSplitString.length());
            tempStr = tempStr.substring(index);

        }

        // Parse through the remaining string looking for the name/number separator :
        // INCOMING_CALL_NAME_ADDRESS_SEPARATOR to extract the last name
        name = tempStr.substring(0, tempStr.indexOf(INCOMING_CALL_NAME_NUMBER_SEPARATOR));

        if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: last name : before" + name );

        name = name.replaceAll(COLON_REGEX_TO_REPLACE, COLON_REGEX_TO_BE_REPLACED);
        name = name.replaceAll(HYPHEN_REGEX_TO_REPLACE, HYPHEN_REGEX_TO_BE_REPLACED);

        mNameArray.add(name);


        if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: last name : " + name );

        tempStr = tempStr.substring(tempStr.indexOf(INCOMING_CALL_NAME_NUMBER_SEPARATOR) + 1);

        if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: addr start : " + tempStr);

        index = 0;

        // Parse through the remaining string looking for the  number separator :
        // INCOMING_CALL_NUMBER_SEPARATOR
        while(tempStr.contains(INCOMING_CALL_NUMBER_SEPARATOR)) {

            name = tempStr.substring(0, tempStr.indexOf(INCOMING_CALL_NUMBER_SEPARATOR));

            name = name.replaceAll(HYPHEN_REGEX_TO_REPLACE, HYPHEN_REGEX_TO_BE_REPLACED);

            // Add to the device address list
            mNumberArray.add(name);
            index = (tempStr.indexOf(INCOMING_CALL_NUMBER_SEPARATOR) + INCOMING_CALL_NUMBER_SEPARATOR.length());

            tempStr = tempStr.substring(index);

            if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: address : " + name);

        }

        tempStr = tempStr.replaceAll(HYPHEN_REGEX_TO_REPLACE, HYPHEN_REGEX_TO_BE_REPLACED);
        mNumberArray.add(tempStr);

        if(LOG_INFO) Log.i(TAG, "extractNameAndNumber: last address  : " + tempStr);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mWidget.closeCursor();
    }

    /**
      * Implement onClick of View.OnClickListener
      * This is used to handle different buttons in the screen
      */
    public final void onClick(View v) {

        switch(v.getId()) {


        case  R.id.all_incoming_calls_button :
        case  R.id.all_incoming_calls_layout :

            mAddressWidget.setVisibility(View.GONE);

            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(mWidget.getAutoComplete().getWindowToken(), 0);

            // All incoming calls button selected.
            if (LOG_INFO) Log.i(TAG, " all_incoming_calls_button ");

            mAllCallsBtn.setChecked(true);

            // Enable "Save", disable "Selected Numbers"
            //mSaveBtn.setEnabled(true);
            mSelectedNosBtn.setChecked(false);
            break;
        case  R.id.selected_numbers_button :
        case  R.id.selected_numbers_layout :

            // "Selected numbers" button selected.
//            mWidget.getAutoComplete().requestFocus();
            InputMethodManager imm1 = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//            if(imm1 != null)
//                imm1.showSoftInput(mWidget.getAutoComplete(), InputMethodManager.SHOW_IMPLICIT);
            mAddressWidget.setVisibility(View.VISIBLE);

            mSelectedNosBtn.setChecked(true);

            // Enable "Save" button, disable "All Numbers"
            //mSaveBtn.setEnabled(true);
            mAllCallsBtn.setChecked(false);
            break;
        }
    }
    /**
     * This method converts the number provided by the addressing widget
     * to a rule friendly form
     * @param number
     * @return converted number
     */
    String convertNumber(String number) {

        // If the number and the name are same, dont need to extract network
        // portion
        if(!mNameArray.contains(number)) {
            number = PhoneNumberUtils.extractNetworkPortion(number);
        }

        if (LOG_INFO) Log.i(TAG, "FormatNumber after format : " + number);

        if(number != null && number.length() > MAX_SUPPORTED_DIGITS_IN_NUMBER) {
            number = number.substring(((number.length()-MAX_SUPPORTED_DIGITS_IN_NUMBER)), (number.length()));
        }

//        number = mWidget.cleanUpPhoneNumber(number);

        return number;
    }

    /**
     * This class implements the runnable for formatting the number
     * The number from the DB will be of the format : 847-123-2344
     * Needs to be formatted to 8471232344.
     * The max number of digits considered is last 10. The excess is stripped off.
     * Eg : 919123456789 is stripped to 9123456789.
     * Since formatting is involved, handled in a different thread.
     *
     */
    @SuppressWarnings("unused")
	private final class FormatNumber implements Runnable, Constants {

        private String mFormatNumber;
        private String mFormatName;

        private final static int NAME = 1;
        private final static int NUMBER = 2;

        /**
         * Constructor which stores the number.
         * @param number
         */
        private FormatNumber(final String number, final String name) {
            mFormatNumber = number;
            mFormatName = name;
        }


        /**
         * This method fills the name and number storage array, which will be
         * further used in rule creation
         * @param formatString
         * @param type
         */
        void fillArray(String formatString, int type) {
            int startIndex = 0, currentIndex = 0;
            while(formatString.contains("[")) {
                currentIndex = formatString.indexOf("[");

                if(type == NUMBER) {

                    String myNumber = formatString.substring(startIndex, currentIndex);

                    if (LOG_INFO) Log.i(TAG, "Parsed number : " + myNumber);
                    mNumberArray.add(myNumber);
                } else {
                    mNameArray.add(formatString.substring(startIndex, currentIndex));
                }
                formatString = formatString.substring((currentIndex+1));
                if (LOG_INFO) Log.i(TAG, "Remaining number : " + formatString);
            }

            if(type == NUMBER) {

                String myNumber = formatString;

                if (LOG_INFO) Log.i(TAG, "Parsed number : " + myNumber);


                mNumberArray.add(myNumber);
            } else {
                mNameArray.add(formatString);
            }
        }

        /**
         * Run method to implement formatting of  the number
         */
        public final void run() {


            if( (!mNumberArray.isEmpty()) && (!mNumberArray.get(0).equals(INCOMING_CALL_ANY_NUMBER)) || (mNumberArray.isEmpty())) {
                mNumberArray.clear();
                mNameArray.clear();
                fillArray(mFormatName, NAME);
                fillArray(mFormatNumber, NUMBER);

            }
            if (LOG_INFO) Log.i(TAG, "FormatNumber after strip : " );

            // We are all set, construct the rules and send the results
            // back to the rules builder.
            sendResults();
            finish();
        }

    }

    /**
     * builds the description string for this iteration
     * @param descBuffer
     * @param index
     * @return modified descBuffer
     */
    private final StringBuffer getDescBuffer(StringBuffer descBuffer, int index){
        String orSplitString = BLANK_SPC + getString(R.string.or);

        if (!Util.isDuplicate(mNameArray, index)) {
            String tempName = (mNumberArray.get(index).equals(INCOMING_CALL_ANY_NUMBER)) ?
                              getString(R.string.call_from_any_number) : mNameArray.get(index);

            descBuffer.append(tempName)
            .append(index!=(mNumberArray.size()-1) ?    orSplitString : "");
        }
        return descBuffer;
    }


    /**
     * This method forms the rule for incoming call ringing state
     * @param strippedNumber
     * @param index
     * @returns the rule for incoming call ringing state
     */
    private final String getVirtualSensorRingingStateRule(String strippedNumber, int index) {

        StringBuilder sBuilder = new StringBuilder();
        // The rule to track Phone state RINGING
        sBuilder.append(PHONE_STATE_RINGING_PART1)
        .append(strippedNumber)
        .append(PHONE_STATE_RINGING_PART2);

        sBuilder.append(RULE_TRIGGER_STRING)
        .append(strippedNumber)
        .append(TRUE_STRING);

        return sBuilder.toString();
    }

    /**
     * This method forms the rule for incoming call ringing state to idle state
     * @param strippedNumber
     * @returns the rule for incoming call ringing state
     */
    private final String getVirtualSensorRingingStateToIdleRule(String strippedNumber) {
		StringBuilder sBuilder = new StringBuilder();

        // Track RINGING -> IDLE state for rule reset
        sBuilder.append(PHONE_STATE_RINGING_PART1)
        .append(strippedNumber)
        .append(PHONE_STATE_RINGING_PART2);

        // The rule to track Phone state IDLE
        sBuilder.append(PHONE_STATE_IDLE_PART1)
        .append(PHONE_STATE_IDLE_PART2)
        .append(RULE_TRIGGER_STRING)
        .append(strippedNumber)
        .append(FALSE_STRING);

		return sBuilder.toString();
    }

    /**
     * This method forms the rule for incoming call ringing state to idle state
     * @param strippedNumber
     * @returns the rule for incoming call ringing state
     */
    private final String getVirtualSensorRingingStateToOffhookRule(String strippedNumber) {
		StringBuilder sBuilder = new StringBuilder();

        // Track RINGING -> OFFHOOK for rule reset
        sBuilder.append(PHONE_STATE_RINGING_PART1)
        .append(strippedNumber)
        .append(PHONE_STATE_RINGING_PART2);


        // The rule to track Phone state OFFHOOK
        sBuilder.append(PHONE_STATE_OFFHOOK_PART1)
        .append(PHONE_STATE_OFFHOOK_PART2)
        .append(RULE_TRIGGER_STRING)
        .append(strippedNumber)
        .append(FALSE_STRING);

		return sBuilder.toString();

    }


    /**
     * Builds the rule for complex virtual sensor for this iteration
     * @param extraTextBuf
     * @param strippedNumber
     * @param index
     * @returns complex virtual sensor rule
     */
    private final String getExtraText(String strippedNumber, int index) {
		StringBuilder sBuilder = new StringBuilder();
        // Actual URI
		sBuilder.append(SENSOR_NAME_START_STRING)
        .append(INCOMING_CALL_VIRTUAL_SENSOR_STRING)
        .append(strippedNumber)
        .append(PARAMETER_STRING)
        .append(INCOMING_CALL_END_STRING)
        .append(index!=(mNumberArray.size()-1) ?    OR_STRING : "");

        return sBuilder.toString();
    }

    /**
     * Forms the xml string of the virtual sensor for the given number
     * This function is called for every number in the list of numbers selected
     * by user.
     * @param rule1
     * @param rule2
     * @param rule3
     * @param mFrequency
     * @param strippedNumber
     * @returns the xml string of the virtual sensor for the given phone number
     */
    private final String getXmlStringForIndex(String rule1, String rule2, String rule3,
    		                           String strippedNumber){

    	StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(strippedNumber);
        String str = sBuilder.toString();
        String ruleSet[] = {
            rule1,
            rule2,
            rule3
        };

        String possibleValues[] = { POSSIBLE_VALUE_FALSE,
                                    POSSIBLE_VALUE_TRUE
                                  };

        // Generate XML from rules.
        String xmlString1 = Util.generateDvsXmlString(str, INCOMING_CALL_VIRTUAL_SENSOR_STRING,
                                INCOMING_CALL_DESCRIPTION, ruleSet,
                                INITIAL_VALUE_FALSE,
                                possibleValues,
                                PERSISTENCY_VALUE_FOREVER);

        xmlString1 = xmlString1.replace(VSENSOR_START_TAG, "");
        xmlString1 = xmlString1.replace(VSENSOR_END_TAG, "");

        if(LOG_INFO) Log.i(TAG, "xmlstring is " + xmlString1);
        return xmlString1;

    }

    /**
     * Populates the fields of Intent returned to Condition Builder module.
     * @param descBuffer
     * @param xmlStringBuf
     * @param extraTextBuf
     * @param strippedNumber
     */
    private final void populateIntentFields(Intent returnIntent,
    		                                StringBuffer descBuffer,
    		                                StringBuffer xmlStringBuf,
    										StringBuffer extraTextBuf,
    										String allNumbers ) {

        String orSplitString = BLANK_SPC + getString(R.string.or) + BLANK_SPC;
        xmlStringBuf.append(VSENSOR_END_TAG);
        String xmlString = xmlStringBuf.toString();

        String newDescription = descBuffer.toString();
        if(newDescription.endsWith(orSplitString)) {
            newDescription = newDescription.substring(0, (newDescription.length()-4));
        }
        returnIntent.putExtra(EVENT_NAME, INCOMING_CALL_DESCRIPTION);
        returnIntent.putExtra(EVENT_DESC, newDescription);


        if (LOG_INFO) Log.i(TAG, "xmlString : " + xmlString);

        // Actual URI
        returnIntent.putExtra(Intent.EXTRA_TEXT, extraTextBuf.toString());
        returnIntent.putExtra(EVENT_TARGET_STATE, allNumbers);
        returnIntent.putExtra(VSENSOR, xmlString);

        setResult(RESULT_OK, returnIntent);
    }

    /**
     * Send XML string to the rules builder
     * @param  None
     * @return None
     */
    private final void sendResults() {
      //  String appendNum = "";
        String strippedNumber = "";
        String orSplitString = BLANK_SPC + getString(R.string.or);
        boolean editUriPopulated = false;
        String editUri = "";
        StringBuilder sBuilder = new StringBuilder();

        StringBuffer descBuffer = new StringBuffer();
        descBuffer.append(getString(R.string.incoming_call_from))
        .append(BLANK_SPC);

        StringBuffer xmlStringBuf = new StringBuffer(VSENSOR_START_TAG);
        StringBuffer extraTextBuf = new StringBuffer();
        StringBuffer allNumbersBuf = new StringBuffer();
        StringBuffer allNamesBuf = new StringBuffer();

        Intent returnIntent = new Intent();
        for(int index=0; index<mNumberArray.size(); index++)  {

            descBuffer = getDescBuffer(descBuffer, index);

            if(!mNumberArray.get(index).equals(INCOMING_CALL_ANY_NUMBER))
                strippedNumber = convertNumber(mNumberArray.get(index));

            String rule1 = getVirtualSensorRingingStateRule(strippedNumber, index);
            String rule2 = getVirtualSensorRingingStateToIdleRule(strippedNumber);
            String rule3 = getVirtualSensorRingingStateToOffhookRule(strippedNumber);
            xmlStringBuf.append(getXmlStringForIndex(rule1, rule2, rule3, strippedNumber));
            extraTextBuf.append(getExtraText(strippedNumber, index));

        	if(LOG_DEBUG) Log.d(TAG, " Rules : " + rule1 + " : " + rule2 + " : " + rule3);

            if(mNumberArray.get(index).equals(INCOMING_CALL_ANY_NUMBER)) {
                // Fire URI
                // The Edit URI stores the data as : (".*" : )
            	sBuilder.append(INCOMING_CALL_CONNECT_URI_TO_FIRE_STRING)
            	.append(mNumberArray.get(index))
            	.append(END_STRING);
            	editUriPopulated = true;
            } else {
                // The set of all device addresses concatenated are needed for rule editing functionality
                // This information will be sent to the Rules Builder when the user first creates the rule
                // and will be retrieved when the user edits the rule then onwards.
                allNumbersBuf.append(mNumberArray.get(index))
                .append(index!=(mNumberArray.size()-1) ?    INCOMING_CALL_NUMBER_SEPARATOR : "");

                // The set of all device names concatenated are needed for rule editing functionality
                // This information will be sent to the Rules Builder when the user first creates the rule
                // and will be retrieved when the user edits the rule then onwards.
                allNamesBuf.append(mNameArray.get(index))
                .append(index!=(mNameArray.size()-1) ?    orSplitString : "");
            }
        }
        String allNames = Util.replaceAllSpecialCharsFromNames(allNamesBuf);
        String allNumbers = Util.replaceAllSpecialCharsFromNumbers(allNumbersBuf);
        if (LOG_INFO) Log.i(TAG, "All Names : " + allNames + "All Numbers : " + allNumbers);

        if(!editUriPopulated) {
            // Fire URI
            // The Edit URI stores the data as : (Name : Number)
        	sBuilder.append(INCOMING_CALL_CONNECT_URI_TO_FIRE_STRING)
        	.append(allNames)
        	.append(INCOMING_CALL_NAME_NUMBER_SEPARATOR)
        	.append(allNumbers)
        	.append(END_STRING);
        }

    	editUri = sBuilder.toString();
    	returnIntent.putExtra(EDIT_URI, editUri);

        if(LOG_DEBUG){
        	Log.d(TAG, " Extra text : " + extraTextBuf.toString());
        	Log.d(TAG, " Edit uri " + editUri);
        }
        populateIntentFields(returnIntent, descBuffer, xmlStringBuf, extraTextBuf, allNumbers);
    }


}
