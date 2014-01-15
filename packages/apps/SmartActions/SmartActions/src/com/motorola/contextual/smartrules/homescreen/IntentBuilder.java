/*
 * @(#)IntentBuilder.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/18/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;
import com.motorola.contextual.smartrules.util.Util;

/**
 * This class can be used to build pending intent for launching an activity or service. It can build intent for
 * auto and manual rule. There are convenience methods for clearing task stack, building rules
 * editor etc. <code><pre>
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
 * @see com.motorola.contextual.smartrules.homescreen.state.AutoState AutoState
 * @see com.motorola.contextual.smartrules.homescreen.state.ManualState ManualState
 */
public class IntentBuilder {
    private static final int NOT_SET      = 0;
    private Intent           mIntent;
    private int              mFlag        = NOT_SET;
    private int              mRequestCode = 0;
    private boolean          isActivity;

    /**
     * Constructor
     *
     */
    private IntentBuilder() {}

    /**
     * Constructor
     * @param context Context
     * @param clazz The activity/service to be launched from pending intent.
     * @throws IllegalArgumentException If any class other than service or activity is passed
     */
    private IntentBuilder(Context context, Class<?> clazz) throws IllegalArgumentException {
        if(Service.class.isAssignableFrom(clazz))
            isActivity = false;
        else if(Activity.class.isAssignableFrom(clazz))
            isActivity = true;
        else
            throw new IllegalArgumentException("Please provide an activity or service class");
        mIntent = new Intent(context, clazz);
    }

    /**
     * Constructor
     * @param action Intent action
     * @param isActivity True if action launches an activity; False if action launches a service.
     */
    private IntentBuilder(String action, boolean isActivity) {
        this.isActivity = isActivity;
        mIntent = new Intent(action);
    }

    /**
     * Creates an intent builder.
     *
     * @param context Context
     * @param clazz The activity/service to be launched from pending intent.
     * @return IntentBuilder
     * @throws IllegalArgumentException If any class other than service or activity is passed
     */
    public static IntentBuilder create(Context context, Class<?> clazz)
            throws IllegalArgumentException {
        return new IntentBuilder(context, clazz);
    }

    /**
     * Creates an intent builder to launch a service.
     *
     * @param action The intent action to be used.
     * @return IntentBuilder
     */
    public static IntentBuilder createService(String action) {
        return new IntentBuilder(action, false);
    }

    /**
     * Creates an intent builder to launch an activity.
     *
     * @param action The intent action to be used.
     * @return IntentBuilder
     */
    public static IntentBuilder createActivity(String action) {
        return new IntentBuilder(action, true);
    }

    /**
     * Convenience method to create and configure the builder to launch rules editor.
     *
     * @param context context
     * @param ruleId Rule ID
     * @param requestCode Request code
     * @return IntentBuilder
     * @throws IllegalArgumentException If any class other than service or activity is passed
     */
    public static IntentBuilder getRulesEditor(Context context, long ruleId, int requestCode)
            throws IllegalArgumentException {
        return create(context, EditRuleActivity.class)
                .setRuleId(context, ruleId)
                .setRequestCode(requestCode)
                .clearTaskStack();
    }

    /**
     * check if intent is null
     *
     * @return True if intent is null; False otherwise
     */
    private boolean checkNotNull() {
        return mIntent != null;
    }

    /**
     * Convenience method to configure the manual rule.
     *
     * @return IntentBuilder
     */
    private IntentBuilder setManual() {
        if (checkNotNull()) mIntent.putExtra(Constants.MM_DISABLE_RULE, false);
        return this;
    }

    /**
     * Convenience method to configure the auto rule.
     *
     * @return IntentBuilder
     */
    private IntentBuilder setAuto() {
        if (checkNotNull()) mIntent.putExtra(Constants.MM_RULE_STATUS, Constants.FALSE);
        return this;
    }

    /**
     * Convenience method to set parameters to turn on the manual rule.
     *
     * @return IntentBuilder
     */
    public IntentBuilder setManualOn() {
        setManual();
        if (checkNotNull()) mIntent.putExtra(Constants.MM_RULE_STATUS, Constants.TRUE);
        return this;
    }

    /**
     * Convenience method to set parameters to turn off the manual rule.
     *
     * @return IntentBuilder
     */
    public IntentBuilder setManualOff() {
        setManual();
        if (checkNotNull()) mIntent.putExtra(Constants.MM_RULE_STATUS, Constants.FALSE);
        return this;
    }

    /**
     * Convenience method to set parameters to turn on the auto rule.
     *
     * @return IntentBuilder
     */
    public IntentBuilder setAutoOn() {
        setAuto();
        if (checkNotNull()) mIntent.putExtra(Constants.MM_ENABLE_RULE, true);
        return this;
    }

    /**
     * Convenience method to set parameters to turn off the auto rule.
     *
     * @return IntentBuilder
     */
    public IntentBuilder setAutoOff() {
        setAuto();
        if (checkNotNull()) mIntent.putExtra(Constants.MM_DISABLE_RULE, true);
        return this;
    }

    /**
     * Convenience method to set the rule key on which a particular operation should be performed.
     *
     * @return IntentBuilder
     */
    public IntentBuilder setRuleKey(String ruleKey) {
        if (checkNotNull()) mIntent.putExtra(Constants.MM_RULE_KEY, ruleKey);
        return this;
    }

    /**
     * @param flag Flag to set
     * @return IntentBuilder
     */
    public IntentBuilder setFlag(int flag) {
        if (mFlag == NOT_SET)
            mFlag = flag;
        else
            mFlag |= flag;
        return this;
    }

    /**
     * @param requestCode Request Code to set
     * @return IntentBuilder
     */
    public IntentBuilder setRequestCode(int requestCode) {
        this.mRequestCode = requestCode;
        return this;
    }

    /**
     * Convenience method to clear the task stack while launching the intent.
     *
     * @return IntentBuilder
     */
    public IntentBuilder clearTaskStack() {
        setFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return this;
    }

    /**
     * Set the rule id
     *
     * @param ruleId Rule Id
     * @return IntentBuilder
     */
    public IntentBuilder setRuleId(Context context, long ruleId) {
        mIntent = Util.fetchRulesBuilderIntent(context, ruleId);
        
        return this;
    }

    /**
     * Build the pending intent from the intent builder configured so far.
     *
     * @param context context
     * @param flag flags to be used to build the pending intent
     * @return pending intent
     */
    private PendingIntent buildService(Context context, int flag) {
        if (mFlag != NOT_SET) mIntent.setFlags(mFlag);
        return PendingIntent.getService(context, mRequestCode, mIntent, flag);
    }

    /**
     * Build the pending intent from the intent builder configured so far.
     *
     * @param context context
     * @param flag flags to be used to build the pending intent
     * @return pending intent
     */
    private PendingIntent buildActivity(Context context, int flag) {
        if (mFlag != NOT_SET) mIntent.setFlags(mFlag);
        return PendingIntent.getActivity(context, mRequestCode, mIntent, flag);
    }

    /**
     * Build the pending intent from the intent builder configured so far.
     *
     * @param context context
     * @param flag flags to be used to build the pending intent
     * @return pending intent
     */
    public PendingIntent build(Context context, int flag) {
        PendingIntent pIntent;
        if (isActivity)
            pIntent = this.buildActivity(context, flag);
        else
            pIntent = this.buildService(context, flag);
        return pIntent;
    }

}
