/*
 * @(#)PublisherManagerConstants.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/04/12                   Initial version
 *
 */
package com.motorola.contextual.smartrules.publishermanager;


import com.motorola.contextual.smartrules.Constants;

/**
 * Declares constants used across PublisherManager
 * CLASS:
 *     PublisherManagerConstants
 *
 * RESPONSIBILITIES:
 *     Constants used across PublisherManager
 *
 */
public interface PublisherManagerConstants extends Constants {
    String ACTION = "action";
    String RULE = "rule";
    String CONDITION = "condition";

    String ACTION_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_UPDATED";
    String CONDITION_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_UPDATED";
    String RULE_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.RULE_PUBLISHER_UPDATED";

    String EXTRA_PUBLISHER_MODIFIED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_MODIFIED_LIST";
    String EXTRA_PUBLISHER_ADDED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_ADDED_LIST";
    String EXTRA_PUBLISHER_REMOVED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_REMOVED_LIST";
    String EXTRA_PUBLISHER_KEY_LIST ="com.motorola.smartactions.intent.extra.PUBLISHER_KEY_LIST";
    String EXTRA_PUBLISHER_UPDATED_REASON ="com.motorola.smartactions.intent.extra.PUBLISHER_UPDATED_REASON";
    String EXTRA_DATA_CLEARED    = Constants.EXTRA_DATA_CLEARED;

    String LOCALE_CHANGED = "LocaleChanged";

    String ACTION_RULE_VALIDATED = "com.motorola.smartactions.intent.action.RULE_VALIDATED";
    String ACTION_PUBLISHER_REFRESH_TIMEOUT = "com.motorola.smartactions.intent.action.PUBLISHER_REFRESH_TIMEOUT";
    String EXTRA_PUBLISHER_TYPE ="com.motorola.smartactions.intent.extra.PUBLISHER_TYPE";
    int PUBLISHER_COMMON_REFRESH_TIMEOUT_VALUE = 20000;
    
    String XML_UPGRADE         = com.motorola.contextual.smartrules.Constants.XML_UPGRADE;
}
