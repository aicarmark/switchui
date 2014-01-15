/*
 * @(#)DatabaseUtilityService.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/08/12  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.net.URISyntaxException;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android_const.provider.TelephonyConst;
import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;

/** This class is responsible for performing database operations like
 * creating, updating and deleting entries.
 * <code><pre>
 *
 * CLASS:
 *  Extends IntentService that handles asynchronous requests using a worker thread
 *
 * RESPONSIBILITIES:
 *  Interacts with the Quick Actions database
 *
 * COLABORATORS:
 *  extends IntentService
 *  implements Constants
 *  uses {@link ActionsDbAdapter} for database operations
 *
 * USAGE:
 *  See each method.
 *
 * </pre></code>
 */

public class DatabaseUtilityService extends IntentService implements Constants {
    private static final String TAG =  TAG_PREFIX + DatabaseUtilityService.class.getSimpleName();

    private static final String PERM_CONDITION_PUBLISHER_ADMIN    = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_ADMIN";

    private static final int AUTO_SMS_NOT_SENT = 0;
    private static final int AUTO_SMS_SENT = 1;

    private static final int WRITE_SUCCESS = 1;
    public static final int FAILED_OP = -1;

    //Vibrate in silent setting is always 1 in ICS
    //Smart Actions is not changing this setting now
    private static final int VIBRATE_IN_SILENT_ON = 1;

    private static final String EQUALS = " = ";
    private static final String AND = " AND ";

    private static final String PDUS = "pdus";

    private static final String SAVE_ENTRIES_AFTER_CALL = "vip_active_during_call";
    private static final String REMOVE_ENTRIES_AFTER_CALL = "vip_inactive_during_call";

    private ActionsDbAdapter mDbAdapter;
    private SmsHelper        mSmsHelper;

    public static final String SUBSCRIBE_COMMAND  = "subscribe_request";
    public static final String SUBSCRIBE_CANCEL  = "cancel_request";
    public static final String COMMAND_EXTRA    = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String REQUEST_ID_EXTRA = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String CONFIG_EXTRA     = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String EXTRA_PUB_KEY   = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_CONSUMER  = "com.motorola.smartactions.intent.extra.CONSUMER";
    public static final String EXTRA_CONSUMER_PACKAGE   = "com.motorola.smartactions.intent.extra.CONSUMER_PACKAGE";
    public static final String CONFIG_MISSED_CALL = "MissedCall=(.*)(1);Version=1.0";    
    public static final String CONSUMER_AUTO_REPLY_TEXT = "com.motorola.contextual.actions.auto_reply_text";
    public static final String CONSUMER_VIP_CALLER = "com.motorola.contextual.actions.vip_caller";
    public static final String PUBLISHER_KEY_MISSED_CALL = "com.motorola.contextual.smartprofile.missedcallsensor";
    public static final String CONDITION_PUBLISHER_CALL_END_EVENT = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_CALL_END_EVENT";

    /**
     * Default constructor
     */
    public DatabaseUtilityService() {
        super("DatabaseUtilityService");
    }

    /**
     * Constructor with name of the worker thread as an argument
     * @param name Name of the worker thread
     */
    public DatabaseUtilityService(String name) {
        super(name);
    }

    @Override
    public void onDestroy() {      
        if(LOG_INFO) Log.i(TAG, "IntentService onDestroy");
        if (mDbAdapter != null)
            mDbAdapter.close();
        if (mSmsHelper != null)
            mSmsHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String intentAction = intent.getStringExtra(EXTRA_INTENT_ACTION);
        Context context = getApplicationContext();
        if(LOG_INFO) Log.i(TAG, "onHandleIntent called. Action is "+intentAction) ;
        mDbAdapter = new ActionsDbAdapter(context);

        if (intentAction != null) {
            if (intentAction.equals(AUTO_SMS_ACTION_KEY)) {
                handleAutoSms(intent);
            } else if (intentAction.equals(SMS_ACTION_KEY)) {
                handleSendMessage(intent);
            } else if (intentAction.equals(CONDITION_PUBLISHER_CALL_END_EVENT)) {
                handleMissedCallIntent();
                handleCallEndEvent(context);
            } else if (intentAction.equals(TelephonyConst.ACTION_SMS_RECEIVED)) {
                handleSmsIntent(intent);
            } else if (intentAction.equals(VipRinger.VIP_RINGER_ACTION_KEY)) {
                handleVipRinger (context, intent);
            } else if (intentAction.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                if (isVipRingerActionActive()) {
                    if (LOG_INFO) Log.i(TAG, "ACTION_PHONE_STATE_CHANGED");
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phoneState != null) {
                        if(phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                            handleIncomingCallEvent(context, intent);
                        }
                    }
                }
                
                SetVoiceAnnounce va = new SetVoiceAnnounce();
                    va.handleSettingChange(context, intent);
            } else if (intentAction.equals(RINGTONE_ACTION_KEY)) {
                handleRingtone(context, intent);
            }
        }
    }

    /**
     * Method to handle auto reply text activation/deactivation
     * The broadcast intent contains the information about rule start or end.
     * @param intent The broadcast intent
     */
    private void handleAutoSms(Intent intent) {
        boolean isRegister = intent.getBooleanExtra(EXTRA_REGISTER_RECEIVER, false);
        String internalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
        String config = CONFIG_MISSED_CALL;

        if (isRegister) {
            writeIntentDataToDb(intent);
            subscribeCpStateNotify(getApplicationContext(), PUBLISHER_KEY_MISSED_CALL,
			               config, "0", true, CONSUMER_AUTO_REPLY_TEXT);
        } else {
            if(LOG_INFO) Log.i(TAG, "Deleting DB entries with internal name: "+internalName);
            String whereClause = AutoSmsTableColumns.INTERNAL_NAME + EQUALS + QUOTE + internalName + QUOTE;
            mDbAdapter.deleteRecord(AUTO_SMS_TABLE, whereClause, null);
            subscribeCpStateNotify(getApplicationContext(), PUBLISHER_KEY_MISSED_CALL,
		               config, "0", false, CONSUMER_AUTO_REPLY_TEXT);
        }
    }

    /**
     * Method to execute send text message action.
     * The broadcast intent contains the information about the action.
     * @param intent The broadcast intent
     */
    private void handleSendMessage (Intent intent) {
        String numbers = intent.getStringExtra(EXTRA_NUMBER);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String names = intent.getStringExtra(EXTRA_NAME);
        String knownFlags = intent.getStringExtra(EXTRA_KNOWN_FLAG);
        if (LOG_INFO) Log.i(TAG, "Before syncing. Numbers: " + numbers + ", names: " + names + ", flags: " + knownFlags);
        Intent statusIntent = (Intent)intent.getParcelableExtra(EXTRA_STATUS_INTENT);
        if (numbers != null) {
            if (names != null && knownFlags != null) {
                //Known flags will be null in GB version
                numbers = syncNumbers(numbers, names, knownFlags);
            }
            if (LOG_INFO) Log.i(TAG, "After syncing. Numbers: " + numbers);
            sendSms(numbers.split(StringUtils.COMMA_STRING), message, statusIntent);
        } else {
            Log.e(TAG, "address not configured");
            ActionHelper.sendActionStatus(this, statusIntent, false,
                                                   getString(R.string.address_not_configured));
        }
    }

    /**
     * Method to handle VIP ringer activation/deactivation
     * The broadcast intent contains the information about rule start or end.
     * @param intent The broadcast intent
     */
    private void handleVipRinger(Context context, Intent intent) {
        boolean isRegister = intent.getBooleanExtra(EXTRA_REGISTER_RECEIVER, false);
        String config = CONFIG_MISSED_CALL;

        if(LOG_INFO) Log.i(TAG, "onReceive called - VIP Ringer - register flag:" +isRegister+" intent : " + intent.toUri(0) );
        // Stateful action broadcast received. Insert/delete values from DB

        String internalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
        String savedNumber = Persistence.retrieveValue(context, NUMBER_TO_END);
        if (isRegister) {
            if(LOG_INFO) Log.i(TAG, "Populating DB");
            String savedInternalName = Persistence.removeValue(context, REMOVE_ENTRIES_AFTER_CALL);
            if (savedInternalName != null || savedNumber != null) {
                //A call with VIP number is going on.
                //A new rule with VIP Ringer action became active
                //We can't clear the DB now because it holds the default values for revert
                //Save the new configuration at the end of the call
                //Only one rule with VIP action would be active. So we can overwrite in persistence
                if (LOG_INFO) Log.i(TAG, "VIP active. Entries to be added to the DB after call");
                Persistence.commitValue(context, SAVE_ENTRIES_AFTER_CALL, intent.toUri(0));
            } else {
                //At a time only one rule with VIP Caller Mode can be active
                //So clear the DB first and then populate fresh entries
                clearVipCallerTable();
                saveToDB(context, intent);
            }
            subscribeCpStateNotify(getApplicationContext(), PUBLISHER_KEY_MISSED_CALL,
					   config, "0", true, CONSUMER_VIP_CALLER);
        } else {
            Persistence.removeValue(context, SAVE_ENTRIES_AFTER_CALL);
            // If VIP Ringer is active when rule end event came and the call is ongoing then
            // delete the DB entries after call ends
            // provided that the number going to be deleted from the DB is the current caller
            if (savedNumber == null) {
                //All the rules containing VIP Caller Mode have become inactive. Clear DB.
                clearVipCallerTable();
            } else {
                //At a given point of time only one rule containing a particular number can be active.
                //So, we don't need to consider the case where multiple rules containing a number become inactive during call.
                //Store this internal name and remove the DB entry when call ends.
                if (LOG_INFO) Log.i(TAG, "Entries to be deleted after call ends");
                Persistence.commitValue(context, REMOVE_ENTRIES_AFTER_CALL, internalName);
            }
            subscribeCpStateNotify(getApplicationContext(), PUBLISHER_KEY_MISSED_CALL,
					   config, "0", false, CONSUMER_VIP_CALLER);
        }
    }

    /**
     * Method to handle missed call intent
     */
    private void handleMissedCallIntent() {
        if (isAutoSmsActionActive()) {
            //When missed call intent is received get the number from Call Logs DB
            String number = null;
            final String[] EVENT_PROJECTION = new String[] {
                CallLog.Calls.TYPE,
                CallLog.Calls.NUMBER
            };
            String sortOrder = Calls._ID + DbSyntax.DESC + DbSyntax.LIMIT + "1";
            Cursor callCursor = null;
            try {
                callCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, EVENT_PROJECTION, null, null, sortOrder);
                if(callCursor != null) {
                    if(callCursor.moveToFirst()) {
                        int lastCallType = callCursor.getInt(callCursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                        if(lastCallType == CallLog.Calls.MISSED_TYPE) {
                            number = callCursor.getString(callCursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                            number = PhoneNumberUtils.extractNetworkPortion(number);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Not able to find missed call number");
            } finally {
                if (callCursor != null)
                    callCursor.close();
            }

            if(LOG_INFO) Log.i(TAG, "Missed call received from number: "+number);
            if (number != null) {
                sendSmsToSpecialNumbers(number, RESPOND_TO_CALLS, ALL_CONTACTS);
                if (Utils.isKnownContact(getApplicationContext(), number))
                    sendSmsToSpecialNumbers(number, RESPOND_TO_CALLS, KNOWN_CONTACTS);
                sendSmsIfNumberFound(number, RESPOND_TO_CALLS);
            }
        }
    }

    /**
     * Method to handle incoming SMS intent
     * @param intent Incoming SMS broadcast intent
     */
    private void handleSmsIntent(Intent intent) {
        if (isAutoSmsActionActive()) {
            String number = null;

            Object[] messages = (Object[]) intent.getSerializableExtra(PDUS);
            if (messages == null) {
                Log.e(TAG, "Failed to retrieve incoming message from the intent");
                return;
            }

            int length = messages.length;
            if (length <= 0) {
                Log.e(TAG, "Error. Array size <= 0");
                return;
            }

            SmsMessage message = SmsMessage.createFromPdu((byte[])messages[length - 1]);
            if (message == null) {
                Log.e(TAG, "Incoming message is null.");
                return;
            }

            number = message.getOriginatingAddress();
            if (number != null) {
                if(LOG_INFO) Log.i(TAG, "SMS received from "+number);
                // wait 500 million seconds for SMS message being saved to Database
                SystemClock.sleep(500);
                sendSmsToSpecialNumbers(number, RESPOND_TO_TEXTS, ALL_CONTACTS);
                if (Utils.isKnownContact(getApplicationContext(), number))
                    sendSmsToSpecialNumbers(number, RESPOND_TO_TEXTS, KNOWN_CONTACTS);
                sendSmsIfNumberFound(number, RESPOND_TO_TEXTS);
            } else {
                Log.e(TAG, "Incoming number is null");
            }
        }
    }

    /**
     * Method to handle incoming call intent
     * @param context Application context
     * @param intent Incoming call intent
     */
    private void handleIncomingCallEvent(Context context, Intent intent) {

        String number = Utils.formatNumber(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));

        if(Persistence.retrieveValue(context, NUMBER_TO_END) != null) return;

        // Incoming call. Take appropriate action and save the values to DB
        if(LOG_INFO) Log.i(TAG, "onReceive called - RINGING from "+number);

        // Get the corresponding action
        VipRinger vipRinger = (VipRinger)ActionHelper.getAction(context, VipRinger.VIP_RINGER_ACTION_KEY);

        Intent intent1 = readRingerDataForContact(context, number,
                AddressUtil.getContactDisplayName(number, context));
        if(intent1 != null) {
            Persistence.commitValue(context, NUMBER_TO_END, number);
            Persistence.commitValue(context, VIP_RINGER_NO_REVERT_FLAG, false);
            saveToDBDefault(context, intent1);

            //If VIP Ringer is setting the volume then notify Ringer action otherwise
            //Ringer action would consider it as a user change
            Volumes ringer = (Volumes)ActionHelper.getAction(context, Volumes.VOLUMES_ACTION_KEY);
            if (ringer  != null) {
                ringer.setState(context, intent1);
            }

            // Get from DB
            if (vipRinger != null) {
                vipRinger.changeRingerSettings(context, intent1);
            }
            VipRinger.registerForSettingChangesDuringCall(context);
        }
    }

    /**
     * Method to handle call end broadcast
     * @param context Application context
     */
    private void handleCallEndEvent(Context context) {
        if (isVipRingerActionActive()) {
            String numberToEnd = Persistence.retrieveValue(context, NUMBER_TO_END);
            if(numberToEnd == null) return;

            Cursor callCursor = null;
            String lastEventNumber = null;
            try {

                final String[] EVENT_PROJECTION = new String[] {
                    CallLog.Calls.TYPE,
                    CallLog.Calls.NUMBER
                };
                String sortOrder = Calls._ID + DbSyntax.DESC + DbSyntax.LIMIT + "1";
                callCursor = context.getContentResolver().query(
                                 CallLog.Calls.CONTENT_URI,
                                 EVENT_PROJECTION,
                                 null,
                                 null,
                                 sortOrder);
                if(callCursor != null) {
                    if(callCursor.moveToFirst()) {
                        lastEventNumber = callCursor.getString(callCursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                        lastEventNumber = Utils.formatNumber(lastEventNumber);
                        if(!numberToEnd.equals(lastEventNumber)) return;
                        if (LOG_DEBUG) Log.d(TAG, "Number after format : " + lastEventNumber);
                    }
                } else {
                    Log.e(TAG, "callCursor is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(callCursor!= null)
                    callCursor.close();
            }

            Persistence.removeValue(context, NUMBER_TO_END);

            //Call ends. Revert ringer settings
            VipRinger vipRinger = (VipRinger)ActionHelper.getAction(context, VipRinger.VIP_RINGER_ACTION_KEY);
            if(LOG_INFO) Log.i(TAG, "onReceive called - com.motorola.IDLE - " + lastEventNumber);
            // Get from DB
            Intent intent1 = readDefaultDataForContact(context, lastEventNumber,
                    AddressUtil.getContactDisplayName(lastEventNumber, context));
            if ((intent1 != null)) {
                if (!Persistence.retrieveBooleanValue(context, VIP_RINGER_NO_REVERT_FLAG) && vipRinger != null) {
                    //If VIP Ringer is reverting the volume then notify Ringer action otherwise
                    //ringer action would consider it as a user change
                    Volumes ringer = (Volumes)ActionHelper.getAction(context, Volumes.VOLUMES_ACTION_KEY);
                    if (ringer  != null) {
                        ringer.setState(context, intent1);
                    }

                    vipRinger.changeRingerSettings(context, intent1);
                } else {
                    //User has manually changed the Ringer settings during call. No need to revert
                    //Reset value in Persistence
                    Persistence.commitValue(context, VIP_RINGER_NO_REVERT_FLAG, false);
                }
            }

            String savedInternalName = Persistence.removeValue(context, REMOVE_ENTRIES_AFTER_CALL);
            String newVipCallers = Persistence.removeValue(context, SAVE_ENTRIES_AFTER_CALL);
            if (savedInternalName != null) {
                //Rule end event was received during call. Entries to be deleted after call
                if (LOG_INFO) Log.i(TAG, "VIP Caller DB entries being deleted after call");
                clearVipCallerTable();
            }
            if (newVipCallers != null) {
                //Intent was received during call. Values to be saved to the DB now.
                if (LOG_INFO) Log.i(TAG, "VIP Caller DB entries being populated after call");
                try {
                    clearVipCallerTable();
                    Intent intent = Intent.parseUri(newVipCallers, 0);
                    if (intent != null) saveToDB(context, intent);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            VipRinger.deregisterFromSettingChangesDuringCall(context);
        }
    }

    /**
     * Send auto text reply if special characters (all or known numbers) are present in the DB
     *
     * @param number The number from which missed call or text is received.
     * Check the DB to find if auto reply needs to be sent to this number
     * @param respondTo Type of event received i.e. missed call or SMS
     * @param type Type of special character i.e. all numbers or known contacts
     */
    private void sendSmsToSpecialNumbers(String number, int respondTo, String type) {
        if (number != null) {
            String formattedNumber = Utils.formatNumber(number);
            String whereClause = AutoSmsTableColumns.NUMBER + EQUALS + QUOTE + type + QUOTE;
            Cursor cursor = null;
            try {
                cursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, null, whereClause, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            if(LOG_INFO) Log.i(TAG, "Send SMS to "+type+" found in DB. Checking if SMS already sent");
                            String internalName = cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.INTERNAL_NAME));
                            String sentNumbersClause = AutoSmsTableColumns.INTERNAL_NAME + EQUALS + QUOTE + internalName + QUOTE +
                                                       AND + AutoSmsTableColumns.NUMBER + EQUALS + QUOTE + formattedNumber + QUOTE;

                            Cursor sentNumbersCursor = null;
                            try {
                                sentNumbersCursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, new String[] {AutoSmsTableColumns.INTERNAL_NAME},
                                                    sentNumbersClause, null);

                                int respondToFlag = cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.RESPOND_TO));

                                //Check if SMS has already been sent for incoming number. If not, send SMS.
                                if ((sentNumbersCursor == null || sentNumbersCursor.getCount() == 0) &&
                                        (respondToFlag == RESPOND_TO_CALLS_AND_TEXTS || respondToFlag == respondTo)) {
                                    if(LOG_INFO) Log.i(TAG, "Sending msg because "+type+" selected");
                                    String message = cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.MESSAGE));
                                    sendSms(new String[] {number}, message);
                                    //When SMS is sent for all/known numbers, an entry corresponding to the number is added in DB
                                    //This entry indicates that the SMS has been sent and there is no need to send SMS again if
                                    //another missed call or SMS is received from the same number
                                    AutoSmsTuple autoSmsTuple = new AutoSmsTuple(internalName, formattedNumber,
                                            respondToFlag, message, AUTO_SMS_SENT, null, 0);
                                    writeTupleToDb(getApplicationContext(), autoSmsTuple);
                                }
                            } finally {
                                if (sentNumbersCursor != null)
                                    sentNumbersCursor.close();
                            }
                        } while (cursor.moveToNext());
                    } else {
                        if (LOG_INFO) Log.i(TAG,"Send to "+type+" not found in DB");
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        } else {
            Log.e(TAG, "Incoming number is null");
        }
    }

    /**
     * Method to change ringtone setting
     * @param context Caller context
     * @param intent Config intent
     */
    private void handleRingtone(Context context, Intent intent) {
        String uri = intent.getStringExtra(EXTRA_URI);
        boolean status = false;

        if (uri == null) {
            // This check is made for upgrade cases
            status = false;
        } else if (uri.equals(RINGTONE_SILENT)) {
            status = true;
        } else {
            // First check if this ringtone exists in file system
            Uri mediaUri = Uri.parse(uri);
            status = Utils.isMediaPresent(context, mediaUri);
        }

        if(status) {
            Persistence.commitValue(context, RINGTONE_STATE_KEY, uri);

            if (uri.equals(RINGTONE_SILENT)) {
                Settings.System.putString(context.getContentResolver(),
                        Settings.System.RINGTONE, null);
            } else {
                Settings.System.putString(context.getContentResolver(),
                        Settings.System.RINGTONE, uri);
            }
        }

        //Response intent
        Intent statusIntent = new Intent(ACTION_PUBLISHER_EVENT);
        statusIntent.putExtra(EXTRA_EVENT_TYPE, (intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false))
                ? EXTRA_REVERT_RESPONSE : EXTRA_FIRE_RESPONSE);
        statusIntent.putExtra(EXTRA_PUBLISHER_KEY, RINGTONE_ACTION_KEY);
        statusIntent.putExtra(EXTRA_RESPONSE_ID, intent.getStringExtra(EXTRA_RESPONSE_ID));
        statusIntent.putExtra(EXTRA_DEBUG_REQRESP, intent.getStringExtra(EXTRA_DEBUG_REQRESP));
        ActionHelper.sendActionStatus(context, statusIntent, status, null);
    }

    /**
     * Method to find out whether any of the numbers corresponding to a Contact name are present in the Actions DB
     * and send Auto Reply SMS if no number is found while Contact name is present. This means that user
     * modified an existing contact.
     * There may be some code duplication with VIP Caller Mode currently.
     * Once the strategy of keeping the code in a single place is finalized
     * plan to use a pattern - Template Method / Strategy
     * @param name Name of the contact
     * @param number Number to send Auto Reply SMS
     * @param respondTo respondTo field to compare to before sending SMS
     */
    private void sendSMSIfNameFound(String name, String number, int respondTo) {
        Cursor cursor = null;
        Cursor pCur = null;
        Cursor mainCursor = null;
        String whereClause = AutoSmsTableColumns.NAME + EQUALS + QUOTE + name + QUOTE;
        try {
            mainCursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, null, whereClause, null);
            if ((mainCursor != null) && (mainCursor.moveToFirst())) {
                //Find all numbers corresponding to this name. If any of these numbers are found in Actions DB the return null
                whereClause = Contacts.DISPLAY_NAME + EQUALS + QUOTE + name + QUOTE;


                cursor = getContentResolver().query(Contacts.CONTENT_URI,
                                                    new String[] {Contacts._ID}, whereClause, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        pCur = getContentResolver().query(
                                   Phone.CONTENT_URI, new String[] {Phone.NUMBER},
                                   Phone.CONTACT_ID + EQUALS + QUOTE + cursor.getString(cursor.getColumnIndex(Contacts._ID)) + QUOTE,
                                   null, null);

                        if (pCur != null) {
                            if (pCur.moveToFirst()) {
                                //Edited number for the Contact found.
                                do {
                                    String num = Utils.formatNumber(pCur.getString(pCur.getColumnIndex(Phone.NUMBER)));
                                    whereClause = AutoSmsTableColumns.NUMBER + EQUALS + QUOTE + num + QUOTE;
                                    Cursor cur = null;
                                    try {
                                        cur = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, null, whereClause, null);
                                        if(LOG_INFO) Log.i(TAG, "Individual Numbers "+ num);
                                        if (cur != null) {
                                            if (cur.getCount() > 0) {
                                                return;
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error occured while querying name in Actions DB");
                                    } finally {
                                        if (cur != null) {
                                            cur.close();
                                        }
                                    }
                                } while(pCur.moveToNext());
                            }
                        }
                    }
                }

                // Now send SMS to the number corresponding to the name
                do {
                    int respondToFlag = mainCursor.getInt(mainCursor.getColumnIndex(AutoSmsTableColumns.RESPOND_TO));
                    String formattedNumber = Utils.formatNumber(number);
                    //Check if incoming number is present in Quick Actions DB and the sent flag is "Not sent"
                    if ((mainCursor.getInt(mainCursor.getColumnIndex(AutoSmsTableColumns.SENT_FLAG)) == AUTO_SMS_NOT_SENT) &&
                            (respondToFlag == RESPOND_TO_CALLS_AND_TEXTS || respondToFlag == respondTo)) {
                        if(LOG_INFO) Log.i(TAG, "Sending SMS to "+ number);
                        sendSms(new String[] { number },
                                mainCursor.getString(mainCursor.getColumnIndex(AutoSmsTableColumns.MESSAGE)));
                        updateAutoSmsSentFlag(getApplicationContext(),
                                              mainCursor.getString(mainCursor.getColumnIndex(AutoSmsTableColumns.INTERNAL_NAME)),
                                              formattedNumber, AUTO_SMS_SENT);
                    }
                } while (mainCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error occured while querying name in the Contacts DB");
        } finally {
            if (mainCursor != null)
                mainCursor.close();
            if (cursor != null)
                cursor.close();
            if (pCur != null)
                pCur.close();
        }
    }

    /**
     * Send auto text reply if number is found in the DB
     *
     * @param number The number from which missed call or text is received.
     * Check the DB to find if auto reply needs to be sent to this number
     * @param respondTo Type of event received i.e. missed call or SMS
     */
    private void sendSmsIfNumberFound(String number, int respondTo) {
        boolean isKnown = Utils.isKnownContact(getApplicationContext(), number);

        if (number != null) {
            String formattedNumber = Utils.formatNumber(number);
            String whereClause = AutoSmsTableColumns.NUMBER + EQUALS + QUOTE + formattedNumber + QUOTE;
            Cursor cursor = null;
            try {
                cursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, null, whereClause, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        if(LOG_INFO) Log.i(TAG, number+" found in DB. Checking if SMS already sent");
                        if ((cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.IS_KNOWN)) == 1) && !isKnown) {
                            if(LOG_INFO) Log.i(TAG, number + ": Deleted contact");
                            return;
                        }
                        do {
                            int respondToFlag = cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.RESPOND_TO));
                            //Check if incoming number is present in Quick Actions DB and the sent flag is "Not sent"
                            if ((cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.SENT_FLAG)) == AUTO_SMS_NOT_SENT) &&
                                    (respondToFlag == RESPOND_TO_CALLS_AND_TEXTS || respondToFlag == respondTo)) {
                                if(LOG_INFO) Log.i(TAG, "Sending SMS to "+ number);
                                sendSms(new String[] { number },
                                        cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.MESSAGE)));
                                updateAutoSmsSentFlag(getApplicationContext(),
                                                      cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.INTERNAL_NAME)),
                                                      formattedNumber, AUTO_SMS_SENT);
                            }
                        } while (cursor.moveToNext());
                    } else {
                        Log.w(TAG, "Number not found in DB");
                        if (isKnown) {
                            String name = AddressUtil.getContactDisplayName(number, getApplicationContext());
                            // Linking with Contacts
                            sendSMSIfNameFound(name, number, respondTo);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading "+number+" from the Actions db");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        } else {
            Log.e(TAG, "Incoming number is null");
        }
    }

    /**
     * Method to send SMS
     *
     * @param numbers - array of numbers to which SMS needs to be sent
     * @param message - Message being sent
     */
    private void sendSms(String[] numbers, String msg) {
        sendSms(numbers, msg, null);
    }

    /**
     * Method to send SMS
     *
     * @param numbers - array of numbers to which SMS needs to be sent
     * @param message - Message being sent
     */
    private void sendSms(String[] numbers, String msg, Intent statusIntent) {
        if (mSmsHelper == null)
            mSmsHelper = new SmsHelper(this);
        mSmsHelper.send(numbers, msg, statusIntent);
    }

    /**
     * Method to check whether there is any active rule with Auto text reply action
     * @return true if there is any active rule with Auto SMS action, false otherwise
     */
    private boolean isAutoSmsActionActive () {
        Cursor cursor = null;
        try {
            cursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, new String[] {AutoSmsTableColumns.INTERNAL_NAME}, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while checking if Auto SMS is active");
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }

    /**
     * Method to update the sent flag for a number
     * @param context Application context
     * @param internalName Internal name of the tuple in which the number is present
     * @param number Number to be updated
     * @param sentFlag New value of flag
     * @return
     */
    private void updateAutoSmsSentFlag (Context context, String internalName, String number, int sentFlag) {
        String whereClause = AutoSmsTableColumns.INTERNAL_NAME + EQUALS + QUOTE + internalName + QUOTE + AND +
                             AutoSmsTableColumns.NUMBER + EQUALS + QUOTE + number + QUOTE;
        Cursor cursor = null;
        try {
            cursor = mDbAdapter.queryDatabase(AUTO_SMS_TABLE, null, whereClause, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    AutoSmsTuple autoSmsTuple = new AutoSmsTuple(internalName, number,
                            cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.RESPOND_TO)),
                            cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.MESSAGE)), sentFlag,
                            cursor.getString(cursor.getColumnIndex(AutoSmsTableColumns.NAME)),
                            cursor.getInt(cursor.getColumnIndex(AutoSmsTableColumns.IS_KNOWN)));
                    mDbAdapter.updateRow(autoSmsTuple, whereClause, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating Auto SMS Sent flag");
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     *  Utility Method to creates a new record for customized contacts
     *  @param context - Application Context
     */
    private void writeTupleToDb(Context context, BaseTuple tuple) {
        if (tuple == null) {
            Log.e(TAG,"Error : Null tuple being inserted!!!");
            return;
        }

        String internalName = tuple.getKeyValue();

        if (LOG_INFO) Log.i(TAG, "Inserting New Record: Internal Name: " + internalName);

        //update the table
        mDbAdapter.insertRow(tuple);
    }

    /**
     * Method to get the extras from an intent and write the data obtained to the DB.
     * @param intent
     */
    private void writeIntentDataToDb (Intent intent) {
        if (intent != null) {
            String numbers = intent.getStringExtra(EXTRA_NUMBERS);

            if (numbers != null) {
                String internalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
                int respondTo = intent.getIntExtra(EXTRA_RESPOND_TO, 0);
                String message = intent.getStringExtra(EXTRA_SMS_TEXT);
                String names = intent.getStringExtra(EXTRA_NAME);
                String knownFlags = intent.getStringExtra(EXTRA_KNOWN_FLAG);
                String[] nameArr = null;
                String[] knownFlagsArr = null;
                String[] numberList = null;

                if (LOG_INFO) Log.i(TAG, "Populating DB. internalName: "+internalName
                                        +" respondTo: "+respondTo+" message: "+message+" numbers: "+numbers);
                if(names != null)  {
                    nameArr = names.split(StringUtils.COMMA_STRING);
                }
                if(knownFlags != null) {
                    knownFlagsArr = knownFlags.split(StringUtils.COMMA_STRING);
                }
                numberList = numbers.split(StringUtils.COMMA_STRING);

                for (int i = 0; i < numberList.length; i++) {
                    String number = numberList[i].trim();
                    AutoSmsTuple autoSmsTuple = null;
                    if (number.equals(ALL_CONTACTS) || number.equals(KNOWN_CONTACTS)) {
                        autoSmsTuple = new AutoSmsTuple(internalName, number, respondTo, message, AUTO_SMS_NOT_SENT, null, 0);
                    } else {

                        number = Utils.formatNumber(number);

                        int knownFlag = 0;
                        try {
                            knownFlag = (knownFlagsArr != null) ? Integer.parseInt(knownFlagsArr[i].trim()) : 0;
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "NumberFormatException while parsing known flag. Setting flag to 0");
                        }

                        autoSmsTuple = new AutoSmsTuple(internalName, number, respondTo, message, AUTO_SMS_NOT_SENT,
                                                        ((nameArr !=null) ? nameArr[i].trim() : null), knownFlag);
                    }
                    writeTupleToDb(getApplicationContext(), autoSmsTuple);
                }
            } else {
                Log.e(TAG, "No numbers to be written to DB");
            }
        }
    }

    /**
    *
    * @param ct
    */
   public static void subscribeCpStateNotify(Context ct, String pubKey, String config, String ruleKey, 
		   									 boolean register, String consumer){

       String command = register?SUBSCRIBE_COMMAND:SUBSCRIBE_CANCEL;

       Intent intent = new Intent(com.motorola.contextual.smartrules.Constants.ACTION_CONDITION_PUBLISHER_REQUEST);
       intent.putExtra(COMMAND_EXTRA, command);

       intent.putExtra(REQUEST_ID_EXTRA, ruleKey);
       intent.putExtra(CONFIG_EXTRA, config);
       intent.putExtra(EXTRA_PUB_KEY, pubKey);
       intent.putExtra(EXTRA_CONSUMER, consumer);
       intent.putExtra(EXTRA_CONSUMER_PACKAGE, com.motorola.contextual.smartrules.Constants.PACKAGE);

       if (LOG_INFO) Log.i(TAG, "Command:" + command + ", pubKey" + pubKey);

       ct.sendBroadcast(intent, PERM_CONDITION_PUBLISHER_ADMIN);
   }
    /**
     * Method to check whether there is any active rule with VIP Ringer action
     * @return true if there is any active rule with VIP Ringer action, false otherwise
     */
    private boolean isVipRingerActionActive() {
        Cursor cursor = null;
        try {
            cursor = mDbAdapter.queryDatabase(VIP_CALLER_TABLE, new String[] {VipCallerTableColumns.INTERNAL_NAME}, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while checking if VIP Ringer action is active");
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }

    /**
     * Method to check if a number is present in VIP Ringer DB
     * and read Ringer data containing that number
     * @param context
     * @param number
     * @return
     */
    private Intent readRingerDataForContact(Context context, String number, String name) {
        Intent returnIntent = null;
        VipRingerTuple vipRingerTuple = null;
        boolean isKnown = Utils.isKnownContact(context, number);
        if (LOG_INFO) Log.i(TAG, "Checking DB for name: "+name+" or number: "+number);
        String whereClause = VipCallerTableColumns.NUMBER + EQUALS + QUOTE + number + QUOTE;

        vipRingerTuple = (VipRingerTuple) readFromDb(context, VIP_CALLER_TABLE, null, whereClause, null);

        if(vipRingerTuple == null) {
            //Number not found in VIP Ringer DB. It is possible that user edited the number of the contact.
            //Check if the name is present in VIP Ringer DB.
            //This is needed only for contacts present in the Phonebook.
            if (isKnown)
                vipRingerTuple = checkContactName(context, name);
        } else {
            //Number found in DB. If Known flag is 0. Then it is raw number.
            //If known flag is 1 then it should be a Phonebook contact (isKnown == true). Check with incoming number.
            if (vipRingerTuple.getIsKnown() == 1 && !isKnown) {
                //If contact is deleted after creating the rule then known flag in the DB is 1 while isKnown is false
                //Contact should not be considered VIP in this case
                vipRingerTuple = null;
            }
        }
        if (vipRingerTuple != null) {
            returnIntent = new Intent();
            returnIntent.setAction(ACTION_EXT_RINGER_CHANGE);
            returnIntent.putExtra(EXTRA_INTERNAL_NAME, vipRingerTuple.getInternalName());
            returnIntent.putExtra(EXTRA_RINGER_MODE, vipRingerTuple.getRingerMode());
            returnIntent.putExtra(EXTRA_VIBE_STATUS, vipRingerTuple.getVibStatus());
            double configVersion = vipRingerTuple.getConfigVersion();
            int ringerVolume = 0;
            int volumeLevel = 0;
            // If this was a config saved in the old format, update it.
            if (configVersion == INITIAL_VERSION) {
                ringerVolume = VipRinger.convertRingerVolume(context, vipRingerTuple.getRingerVolume());
                volumeLevel = VipRinger.convertRingerVolumeToVolumeLevel(vipRingerTuple.getRingerVolume(),
                                                                         VipRinger.getMaxRingerVolume(context));
            } else {
                ringerVolume = VipRinger.convertVolumeLevelToRingerVolume(context, vipRingerTuple.getRingerVolume());
                volumeLevel = vipRingerTuple.getRingerVolume();
            }

            returnIntent.putExtra(EXTRA_RINGER_VOLUME, ringerVolume);
            returnIntent.putExtra(EXTRA_VOLUME_LEVEL, volumeLevel);
            returnIntent.putExtra(EXTRA_URI, vipRingerTuple.getRingtoneUri());
            returnIntent.putExtra(EXTRA_TITLE, vipRingerTuple.getRingtoneTitle());
            returnIntent.putExtra(EXTRA_NUMBER, vipRingerTuple.getNumber());
            returnIntent.putExtra(EXTRA_NAME, vipRingerTuple.getName());
            returnIntent.putExtra(EXTRA_KNOWN_FLAG, vipRingerTuple.getIsKnown());
            returnIntent.putExtra(EXTRA_CONFIG_VERSION, VipRinger.getConfigVersion());
        }
        return returnIntent;
    }

    /**
     * Method to check if a number is present in VIP Ringer DB
     * and read default Ringer data containing that number
     * @param context
     * @param number
     * @return
     */
    private Intent readDefaultDataForContact(Context context, String number, String name) {
        Intent returnIntent = null;
        VipRingerTuple vipRingerTuple = null;
        boolean isKnown = Utils.isKnownContact(context, number);
        String whereClause = VipCallerTableColumns.NUMBER + EQUALS + QUOTE+ number + QUOTE;

        vipRingerTuple = (VipRingerTuple) readFromDb(context, VIP_CALLER_TABLE, null, whereClause, null);

        if (vipRingerTuple == null) {
            if (isKnown)
                vipRingerTuple = checkContactName(context, name);
        } else {
            if (vipRingerTuple.getIsKnown() == 1 && !isKnown) {
                vipRingerTuple = null;
            }
        }
        if(vipRingerTuple != null) {
            returnIntent = new Intent();
            boolean vibStatus = VipRinger.isVibrateOn(vipRingerTuple.getDefVibSettings());
            returnIntent.setAction(ACTION_EXT_RINGER_CHANGE);
            returnIntent.putExtra(EXTRA_INTERNAL_NAME, vipRingerTuple.getInternalName());
            returnIntent.putExtra(EXTRA_RINGER_MODE, vipRingerTuple.getDefRingerMode());
            returnIntent.putExtra(EXTRA_RINGER_VOLUME, vipRingerTuple.getDefRingerVolume());
            returnIntent.putExtra(EXTRA_URI, vipRingerTuple.getDefRingtoneUri());
            returnIntent.putExtra(EXTRA_TITLE, vipRingerTuple.getDefRingtoneTitle());
            returnIntent.putExtra(EXTRA_VIBE_STATUS, vibStatus);
        }
        return returnIntent;
    }

    /**
     * Method to check if a number is present in the DB and read data from the tuple containing that number
     * @param context
     * @param tuple
     * @param number
     * @return
     */
    private BaseTuple readFromDb(Context context, String tableName, String[] projection,
                                 String whereClause, String[] args) {
        Cursor cursor = null;
        BaseTuple tuple = null;

        try {
            cursor = mDbAdapter.queryDatabase(tableName, projection, whereClause, args);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    tuple = VipRingerTuple.extractDataFromCursor(cursor);
                    if (LOG_DEBUG) Log.d(TAG, "Move to first succeeded");
                }
            } else {
                Log.e(TAG, "Unable to retrive the details from DB ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return tuple;
    }

    /**
     * Method to save the data contained in the intent to DB. This method saves the default values of Ringer.
     * @param context
     * @param number
     * @param intent
     */
    private void saveToDBDefault(Context context, Intent intent) {

        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        String ringtoneUri = Settings.System.getString(context.getContentResolver(), Settings.System.RINGTONE);
        Ringtone ringtoneName = (ringtoneUri != null) ? RingtoneManager.getRingtone(context,
                                Uri.parse(ringtoneUri)) : null;

        VipRingerTuple vipRingerTuple = new VipRingerTuple(intent.getStringExtra(EXTRA_INTERNAL_NAME),
                intent.getStringExtra(EXTRA_NUMBER), intent.getStringExtra(EXTRA_NAME),
                AudioManager.RINGER_MODE_NORMAL,
                intent.getBooleanExtra(EXTRA_VIBE_STATUS, false),
                intent.getIntExtra(EXTRA_VOLUME_LEVEL,VipRinger.VOLUME_LEVEL_MAX),
                intent.getStringExtra(EXTRA_URI), intent.getStringExtra(EXTRA_TITLE), audioManager.getRingerMode(),
                VipRinger.getCurrentVibrateSetting(context),
                VIBRATE_IN_SILENT_ON, audioManager.getStreamVolume(AudioManager.STREAM_RING), ringtoneUri,
                (ringtoneName != null) ? ringtoneName.getTitle(context) : null, intent.getIntExtra(EXTRA_KNOWN_FLAG, 0),
                VipRinger.getConfigVersion());

        String whereClause = VipCallerTableColumns.NUMBER + EQUALS + QUOTE + intent.getStringExtra(EXTRA_NUMBER) + QUOTE;
        writeTupleToDb(context, vipRingerTuple, true, whereClause);
    }

    /**
     * Method to save the data contained in the intent to the DB.
     * This method doesn't store the default values
     * @param context
     * @param intent
     */
    private void saveToDB(Context context, Intent intent) {

        String numbers = intent.getStringExtra(EXTRA_NUMBER);
        String names = intent.getStringExtra(EXTRA_NAME);
        String knownFlags = intent.getStringExtra(EXTRA_KNOWN_FLAG);

        if (LOG_INFO) Log.i(TAG, "VIPRinger -  saveToDB : " + numbers);

        if(numbers != null && names != null && knownFlags != null) {
            String numArr[] = numbers.split(StringUtils.COMMA_STRING);
            String nameArr[] = names.split(StringUtils.COMMA_STRING);
            String knownFlagsArr[] = knownFlags.split(StringUtils.COMMA_STRING);

            for (int i=0; i<numArr.length; i++) {

                String num = Utils.formatNumber(numArr[i].trim());
                if (LOG_DEBUG) Log.d(TAG, "VIPRinger -   saveToDB - n : " + num);

                int knownFlag = 0;
                try {
                    knownFlag = Integer.parseInt(knownFlagsArr[i].trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "NumberFormatException while parsing known flag. Setting flag to 0");
                }

                AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
                VipRingerTuple vipRingerTuple = new VipRingerTuple(intent.getStringExtra(EXTRA_INTERNAL_NAME), num,
                        nameArr[i].trim(), AudioManager.RINGER_MODE_NORMAL,
                        intent.getBooleanExtra(EXTRA_VIBE_STATUS, false), VipRinger.getVolumeLevel(intent, audioManager),
                        intent.getStringExtra(EXTRA_URI), intent.getStringExtra(EXTRA_TITLE), knownFlag,
                        VipRinger.getConfigVersion());

                boolean update = checkIfNumberExists(context, vipRingerTuple, num);
                writeTupleToDb(context, vipRingerTuple, update, null);
            }
        }
    }

    /**
     * Utility Method to creates a new record or update an existing record in Actions DB
     * @param context
     * @param tuple
     * @param isEdit
     * @param whereClause
     * @return
     */
    private int writeTupleToDb(Context context, BaseTuple tuple, boolean isEdit, String whereClause) {
        int responseCode = FAILED_OP;
        if (tuple == null) {
            Log.e(TAG,"Error : Null parameter!!!");
            return responseCode;
        }
        String internalName = tuple.getKeyValue();

        if (LOG_DEBUG) Log.d(TAG, "writeTupleToDb Entry");

        if (isEdit) {
            //Try to get the existing values from the db
            Cursor cursor = null;
            try {
                cursor = mDbAdapter.queryDatabase(tuple.getTableName(), null, null, null);
                if (cursor != null) {
                    if (LOG_DEBUG) Log.d(TAG, "Number of modes returned :" + cursor.getCount());
                    if (cursor.getCount() != 0 && cursor.moveToFirst()) {

                        if (LOG_INFO) Log.i(TAG, " Updating Record: " +
                                                " Internal Name: " + internalName + " : " + tuple + " : " +
                                                whereClause);


                        mDbAdapter.updateRow(tuple, whereClause, null);

                        responseCode = WRITE_SUCCESS;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        if (responseCode != WRITE_SUCCESS) {
            // No existing entry or Edit failed. Try adding new entry

            if (LOG_INFO) Log.i(TAG, "Inserting New Record: Internal Name: " + internalName);

            //update the table
            mDbAdapter.insertRow(tuple);

            responseCode = WRITE_SUCCESS;
        }

        return responseCode;
    }

    /**
     * Method to check if a number is present in the DB
     * @param context
     * @param tuple
     * @param number
     * @return
     */
    private boolean checkIfNumberExists (Context context, BaseTuple tuple, String number) {
        Cursor cursor = null;
        try {
            cursor = mDbAdapter.queryDatabase(tuple.getTableName(), null, null, null);
            if (LOG_DEBUG) Log.d("VIPRingerTuple", "checkIfNumberExists : " +   " : " + number + " : "  + cursor);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String newNumber = cursor.getString(cursor.getColumnIndexOrThrow(VipCallerTableColumns.NUMBER));

                    if (LOG_DEBUG) Log.d("VIPRingerTuple", "checkIfNumberExists : " + newNumber + " : " + number);
                    if ((newNumber != null) && (newNumber.contains(number))) {
                        return true;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to check if a number is present in the Actions DB");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return false;
    }

    /**
     * Method to find out whether any of the numbers corresponding to a Contact name are present in the Actions DB
     * and return VipRingerTuple object if no number is found while Contact name is present. This means that user
     * modified an existing contact.
     * @param context Application context
     * @param name Name of the contact
     * @return VipRingerTuple object with supplied name
     */
    private VipRingerTuple checkContactName(Context context, String name) {
        String whereClause = VipCallerTableColumns.NAME + EQUALS + QUOTE + name + QUOTE;
        VipRingerTuple vipRingerTuple = (VipRingerTuple) readFromDb(context, VIP_CALLER_TABLE, null, whereClause, null);
        if (vipRingerTuple != null) {
            //Find all numbers corresponding to this name. If any of these numbers are found in Actions DB the return null
            whereClause = Contacts.DISPLAY_NAME + EQUALS + QUOTE + name + QUOTE;
            Cursor cursor = null;
            Cursor pCur = null;
            try {
                cursor = getContentResolver().query(Contacts.CONTENT_URI,
                                                    new String[] {Contacts._ID}, whereClause, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        pCur = getContentResolver().query(
                                   Phone.CONTENT_URI, new String[] {Phone.NUMBER},
                                   Phone.CONTACT_ID + EQUALS + QUOTE + cursor.getString(cursor.getColumnIndex(Contacts._ID)) + QUOTE,
                                   null, null);

                        if (pCur != null) {
                            if (pCur.moveToFirst()) {
                                //Edited number for the Contact found.
                                do {
                                    String num = Utils.formatNumber(pCur.getString(pCur.getColumnIndex(Phone.NUMBER)));
                                    whereClause = VipCallerTableColumns.NUMBER + EQUALS + QUOTE + num + QUOTE;
                                    Cursor cur = null;
                                    try {
                                        cur = mDbAdapter.queryDatabase(VIP_CALLER_TABLE, null, whereClause, null);
                                        if (cur != null) {
                                            if (cur.getCount() > 0)
                                                return null;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error occured while querying name in Actions DB");
                                    } finally {
                                        if (cur != null) {
                                            cur.close();
                                        }
                                    }
                                } while(pCur.moveToNext());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error occured while querying name in the Contacts DB");
            } finally {
                if (cursor != null)
                    cursor.close();
                if (pCur != null)
                    pCur.close();
            }
        }
        return vipRingerTuple;
    }

    /**
     * Method to clear the VIP Caller table
     */
    private void clearVipCallerTable () {
        if(LOG_INFO) Log.i(TAG, "Clearing VIP Caller DB");
        mDbAdapter.deleteRecord(VIP_CALLER_TABLE, null, null);
    }

    /**
     * Method that synchronizes input numbers with Contacts and returns them
     * @param numbers Input numbers
     * @param names Names
     * @param knownFlags Known flags corresponding to the numbers
     * @return Synchronized numbers
     */
    private String syncNumbers (String numbers, String names, String knownFlags) {
        Utils.NameNumberInfo syncedContacts = Utils.getSyncedContactAttributes(getApplicationContext(), numbers,
                names, knownFlags, StringUtils.COMMA_STRING);
        if (syncedContacts != null) {
            return syncedContacts.getNumbers();
        }
        return numbers;
    }

}
