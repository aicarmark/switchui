/* 
 * Copyright (C) 2012, Motorola, Inc, 
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * 
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 9, 2012   MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers.conditions;

import android.content.Intent;

/**
 * This is a helper class used for the trigger intent construction.
 * This fills the name, description, rules in the XML format and the
 * edit(fire) URI, in the Intent URI which is sent back to the Smart 
 * Rules component with the help of its sub-classes.
 * This class is used to isolate rule construction functionality from the 
 * Precondition UI functionality. Individual Preconditions derive from 
 * this to construct their own rules.
 */
public abstract class TriggerIntentConstructor {


    protected String mName;
    protected String mDescription;

    /**
     *  sCurrentMode - this is used in the construction of the rule,  UI - independent.
     */
    protected String mCurrentMode;

    /**
     * sCurrentModeUI - this is used in Fire URI - UI - dependent.
     */
    protected String mCurrentModeUI;

    /**
     * The method to set the name of the Precondition
     * @param inName
     */
    public final void setName(final String inName) {
        mName = inName;
    }
    /**
     * The method to set the description of the Precondition
     * @param desc
     */
    public final void setDescription(final String desc) {
        mDescription = desc;
    }
    /**
     * The method to set the use selected mode of the Precondition
     * The mcurrentMode is used for the rule construction
     * This is different from mCurrentModeUI which is UI dependent
     * Two separate variables are maintained so as to isolate the rule creation
     * completely from UI, thus the rule construction is decoupled from UI string changes
     * @param mode
     */
    public final void setCurrentMode(final String mode) {
        mCurrentMode = mode;
    }
    /**
     *The method to set the use selected mode of the Precondition
     * The mcurrentMode is used for the rule construction
     * This is different from mCurrentModeUI which is UI dependent
     * Two separate variables are maintained so as to isolate the rule creation
     * completely from UI, thus the rule construction is decoupled from UI string changes
     * @param modeUI
     */
    public final void setCurrentModeUI(final String modeUI) {
        mCurrentModeUI = modeUI;
    }
    /**
     * The method to construct the rules and the XML string to be passed to the
     * Smart Rules. To be overridden by individual Preconditions.
     * @param  None
     * @return Intent to be passed to the Smart Rules
     */
    public abstract Intent constructResults();


}
