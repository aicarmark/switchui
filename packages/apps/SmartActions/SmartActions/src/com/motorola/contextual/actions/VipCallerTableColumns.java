/*
 * @(#)VipCallerTableColumns.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2011/08/01  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.provider.BaseColumns;

/**
 * This interface defines columns in the VIP Ringer table present in the database <code><pre>
 * CLASS:
 *   This interface represents columns in the VIP Ringer table present in the database.
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
 *      Used to represent columns in VIP Ringer table.
 *
 * </pre></code>
 **/

public interface VipCallerTableColumns extends BaseColumns {

    /** Internally generated name for a group of numbers. This wont be visible to the world */
    static final String INTERNAL_NAME       = "vipInternalName";

    /** VIP Caller's number */
    static final String NUMBER              = "number";

    /** VIP Caller's name */
    static final String NAME              = "name";

    /** Customized ringer mode for VIP Caller */
    static final String RINGER_MODE         = "ringerMode";

    /** Customized vibrate status for VIP Caller */
    static final String VIBE_STATUS         = "vibeStatus";

    /** Customized ringer volume for VIP Caller */
    static final String RINGER_VOLUME       = "ringerVolume";

    /** Customized ringtone uri for VIP Caller */
    static final String RINGTONE_URI        = "ringtoneUri";

    /** Customized ringtone name for VIP Caller */
    static final String RINGTONE_TITLE      = "ringtoneTitle";

    /** Default ringer mode */
    static final String DEF_RINGER_MODE     = "defRingerMode";

    /** Default vibrate status */
    static final String DEF_VIBE_STATUS     = "defVibeStatus";

    /** Default vibrate setting */
    static final String DEF_VIBE_SETTINGS   = "defVibeSettings";

    /** Default vibrate in silent setting */
    static final String DEF_VIBE_IN_SILENT  = "defVibeInSilent";

    /** Default ringer volume */
    static final String DEF_RINGER_VOLUME   = "defRingerVolume";

    /** Default ringtone URI */
    static final String DEF_RINGTONE_URI    = "defRingtoneUri";

    /** Default ringtone title */
    static final String DEF_RINGTONE_TITLE  = "defRingtoneTitle";

    /** Flag indicating whether the added number is present in the Phonebook or not at the time of rule creation/modification */
    static final String IS_KNOWN            = "is_known";

    /** Config Version */
    static final String CONFIG_VERSION      = "configVersion";
}
