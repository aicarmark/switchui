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

import com.motorola.contextual.smartprofile.Constants;

/**
 * Rule-related strings used for constructing missed calls condition rule
 */
interface MissedCallConstants {
    // Constants copied from com.motorola.contextual.smartprofile.sensors.missedcallsensor.MissedCall.MissedCallConstants

    String MISSED_CALLS_CONFIG_PERSISTENCE = "MissedCallConfig";
    String MISSED_CALLS_PUB_KEY = "com.motorola.contextual.smartprofile.missedcallsensor";
    String EXTRA_MISSED_CALLS_ACTION = "com.motorola.contextual.smartrules.intent.extra.MISSED_CALLS_ACTION";
    String MISSED_CALLS_MAX_ID_PERSISTENCE = "MissedCallMaxId";
    String MISSED_CALLS_OBSERVER_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.missedcall.MissedCallObserverStateMonitor"; //TODO
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