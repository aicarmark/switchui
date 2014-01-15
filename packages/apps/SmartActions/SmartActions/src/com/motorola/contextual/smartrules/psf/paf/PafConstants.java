/*
 * @(#)PafConstants.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/03/26                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.paf;

import com.motorola.contextual.smartrules.psf.PsfConstants;

/**
 * Declares constants used across PAF
 * INTERFACE:
 *     PafConstanrs extends PsfConstants
 *
 * RESPONSIBILITIES:
 *     Constants used across PAF
 *
 */

public interface PafConstants extends PsfConstants {

    String ACTION_PAF_PUBLISHER_UPDATER = "com.motorola.contextual.smartrules.intent.action.PAF_PUBLISHER_UPDATER";
    String EXTRA_PUBLISHER_INFO_LIST = "com.motorola.contextual.smartrules.intent.extra.PUBLISHER_INFO_LIST";


    String PUBLISHER_DELETED = "PUBLISHER_DELETED";
    String PUBLISHER_INSERTED = "PUBLISHER_INSERTED";
    String PUBLISHER_MODIFIED = "PUBLISHER_MODIFIED";
    String PUBKEY_DELETED = ":deleted";
    String PUBKEY_NEW = ":new";
    String PUBKEY_MODIFIED = ":modified";
    String WHITELIST_DEFAULT_XML_FILE_PATH = "/system/etc/smartactions/com.motorola.smartactions_whitelist.xml";
    String BLACKLIST_DEFAULT_XML_FILE_PATH = "/system/etc/smartactions/com.motorola.smartactions_blacklist.xml";
    String PUBLISHER_PROVIDER_LIST = "PUBLISHER_PROVIDER_LIST";
    String PACKAGE_MANAGER_LIST = "PACKAGE_MANAGER_LIST";

    String ACTION_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_UPDATED";
    String CONDITION_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_UPDATED";
    String RULE_PUBLISHER_UPDATED = "com.motorola.smartactions.intent.action.RULE_PUBLISHER_UPDATED";

    String ACTION_PUBLISHER_DATA_RESET = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_DATA_RESET";
    String CONDITION_PUBLISHER_DATA_RESET = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_DATA_RESET";
    String RULE_PUBLISHER_DATA_RESET = "com.motorola.smartactions.intent.action.RULE_PUBLISHER_DATA_RESET";


    String EXTRA_PUBLISHER_MODIFIED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_MODIFIED_LIST";
    String EXTRA_PUBLISHER_ADDED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_ADDED_LIST";
    String EXTRA_PUBLISHER_REMOVED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_REMOVED_LIST";
    String EXTRA_PUBLISHER_KEY_LIST ="com.motorola.smartactions.intent.extra.PUBLISHER_KEY_LIST";
    String EXTRA_PUBLISHER_UPDATED_REASON ="com.motorola.smartactions.intent.extra.PUBLISHER_UPDATED_REASON";

    String PUBLISHER_INSTALLED = "PublisherInstalled";
    String PUBLISHER_UNINSTALLED = "PublisherUnInstalled";
    String PUBLISHER_RELPACED = "PublisherReplaced";
    String PUBLISHER_RESTARTED = "PublisherReStarted";
    String LOCALE_CHANGED = "LocaleChanged";
}
