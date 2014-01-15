/*
 * @(#)AutoSmsTableColumns.java
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

import android.provider.BaseColumns;

/**
 * This interface defines columns in the auto SMS table present in the database <code><pre>
 * CLASS:
 *   This interface represents columns in the auto SMS table present in the database.
 *
 *   All constants are static
 *
 * RESPONSIBILITIES:
 *   None - constants only
 *
 * COLABORATORS:
 *       None.
 *
 * USAGE:
 *      Used to represent columns in auto SMS table.
 *
 * </pre></code>
 **/

public interface AutoSmsTableColumns extends BaseColumns {

    /** Internally generated name for all the numbers present in one rule. */
    static final String INTERNAL_NAME       = "internalName";

    /** Number which will receive auto reply */
    static final String NUMBER              = "number";

    /** Flag indication whether to respond to missed calls or texts or both */
    static final String RESPOND_TO          = "respondTo";

    /** Message to be sent */
    static final String MESSAGE             = "message";

    /** Flag indicating whether message has been sent to the corresponding number */
    static final String SENT_FLAG           = "sent_flag";
    
    /** Name for Selected contact case*/
    static final String NAME              = "name";
    
    /** Flag indicating whether the added number is present in the Phonebook or not at the time of rule creation/modification */
    static final String IS_KNOWN            = "is_known";

}
