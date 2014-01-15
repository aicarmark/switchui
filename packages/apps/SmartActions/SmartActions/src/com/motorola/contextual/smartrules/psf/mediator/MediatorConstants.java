/*
 * @(#)MediatorConstants.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.mediator.protocol.PublisherProtocolCommands;

/**
 * Interface having constants which are used by the mediator.
 * 
 * PSF=Publisher Sharing Framework
 *
 * <CODE><PRE>
 *
 * INTERFACE:
 *
 * RESPONSIBILITIES:
 * Interface having constants which are used by the mediator.
 *
 * COLABORATORS:
 *     Mediator - Uses the constants present in this interface
 *
 * USAGE:
 *     Contains constants
 *
 * </PRE></CODE>
 */

public interface MediatorConstants extends PsfConstants, ConditionPublisherAction, PublisherProtocolCommands {

    public static final String MEDIATOR_PREFIX = "Med";

    public static final String MEDIATOR_SEPARATOR = "::med::";
    public static final String MEDIATOR_INITIALIZATION_COMPLETE = "mediator_init_complete";

    public static final String PACKAGE = Constants.PACKAGE;

    public static final String EXTRA_CONFIG                  = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String EXTRA_DESCRIPTION             = "com.motorola.smartactions.intent.extra.DESCRIPTION";
    public static final String EXTRA_EVENT                   = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_REQUEST_ID              = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String EXTRA_PUBLISHER_KEY           = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_RESPONSE_ID             = "com.motorola.smartactions.intent.extra.RESPONSE_ID";
    public static final String EXTRA_STATUS                  = "com.motorola.smartactions.intent.extra.STATUS";
    public static final String EXTRA_STATE                   = "com.motorola.smartactions.intent.extra.STATE";
    public static final String EXTRA_CONSUMER                = "com.motorola.smartactions.intent.extra.CONSUMER";
    public static final String EXTRA_CONSUMER_PACKAGE        = "com.motorola.smartactions.intent.extra.CONSUMER_PACKAGE";
    public static final String EXTRA_CONFIG_STATE_MAP        = "com.motorola.smartactions.intent.extra.STATES";
    public static final String EXTRA_CONFIG_ITEMS            = "com.motorola.smartactions.intent.extra.CONFIG_ITEMS";
    public static final String EXTRA_NEW_STATE_TITLE         = "com.motorola.smartactions.intent.extra.NEW_STATE_TITLE";
    public static final String EXTRA_PUBLISHER_KEY_LIST      = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY_LIST";
    public static final String EXTRA_PUBLISHER_REMOVED_LIST  = "com.motorola.smartactions.intent.extra.PUBLISHER_REMOVED_LIST";
    public static final String EXTRA_PUBLISHER_MODIFIED_LIST = "com.motorola.smartactions.intent.extra.PUBLISHER_MODIFIED_LIST";

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    public static final String PACKAGE_PREFIX = "package:";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    //Currently the consumer list is hard coded
    public static final String[] CONSUMERS = {"com.motorola.contextual.smartrules.sacore",
                                 "com.motorola.smartactions.publisher.rule"
                                             };

    //Permissions
    public static final String PERM_CONDITION_PUBLISHER_ADMIN = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_ADMIN";
    public static final String PERM_CONDITION_PUBLISHER = "com.motorola.smartactions.permission.CONDITION_PUBLISHER";
    public static final String PERM_CONDITION_PUBLISHER_USER = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_USER";

}
