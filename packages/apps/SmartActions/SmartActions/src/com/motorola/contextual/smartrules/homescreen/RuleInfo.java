/*
 * @(#)RuleInfo.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/17/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.content.Intent;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/**
 * Represent the current state of the remote rule (in the smart actions app)
 * <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class RuleInfo {
    private int enabled;
    private int active;
    private int manual;

    /**
     * Constructor
     * @param enabled 1 - enabled; 0 otherwise
     * @param active 1 - active; 0 otherwise
     * @param manual 1 - manual; 0 otherwise
     */
    public RuleInfo(int enabled, int active, int manual) {
        this.enabled = enabled;
        this.active = active;
        this.manual = manual;
    }

    /**
     * Constructor
     * @param intent Intent which contains the rule info as extras
     */
    public RuleInfo(Intent intent) {
        this(intent.getIntExtra(RuleTable.Columns.ENABLED, Constants.INVALID_KEY),
            intent.getIntExtra(RuleTable.Columns.ACTIVE, Constants.INVALID_KEY),
            intent.getIntExtra(RuleTable.Columns.RULE_TYPE, Constants.INVALID_KEY));
    }

    /**
     * @return the enabled
     */
    public int getEnabled() {
        return enabled;
    }

    /**
     * @return the active
     */
    public int getActive() {
        return active;
    }

    /**
     * @return the manual
     */
    public int getManual() {
        return manual;
    }

    /**
     * @return true if enabled; false otherwise
     */
    public boolean isEnabled() {
        return enabled == 1;
    }

    /**
     * @return true if active; false otherwise
     */
    public boolean isActive() {
        return active == 1;
    }

    /**
     * @return true if manual; false otherwise
     */
    public boolean isManual() {
        return manual == 1;
    }
}
