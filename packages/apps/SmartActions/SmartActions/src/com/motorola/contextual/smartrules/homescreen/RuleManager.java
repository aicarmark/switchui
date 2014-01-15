/*
 * @(#)WidgetManager.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * VXMD37        2012/03/11 NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.homescreen.state.AbstractState;
import com.motorola.contextual.smartrules.homescreen.state.AddRuleState;

/**
 * Manages the different states of the widget. It represents the current state of the widget
 * {@link AddRuleState} or {@link com.motorola.contextual.smartrules.homescreen.state.ManualState
 * ManualState} or {@link com.motorola.contextual.smartrules.homescreen.state.AutoState AutoState}.
 * We need to maintain one manager per rule. Use
 * {@link RulesFactory} to do that. <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *  Handles all the operations like on, off, ready etc delegated
 *  from widget provider.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 *
 * @see AddRuleState
 * @see com.motorola.contextual.smartrules.homescreen.state.ManualState ManualState
 * @see com.motorola.contextual.smartrules.homescreen.state.AutoState AutoState
 */
public class RuleManager {

    private static final String    TAG = RuleManager.class.getSimpleName();
    private volatile AbstractState mState;

    /**
     * Constructor
     */
    public RuleManager() {
        mState = AddRuleState.getInstance();
    }

    /**
     * Switch to manual mode.
     */
    public void manual() {
        mState.manual(this);
    }

    /**
     * Switch to auto mode.
     */
    public void auto() {
        mState.auto(this);
    }

    /**
     * Update the widget when the rule is active.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void on(Context context, RuleEntity re) {
        if (Constants.LOG_DEBUG) Log.d(TAG, "on Called - " + mState.getClass().getSimpleName());
        mState.on(context, re);
    }

    /**
     * Update the widget when the rule is disabled.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void off(Context context, RuleEntity re) {
        if (Constants.LOG_DEBUG) Log.d(TAG, "off Called - " + mState.getClass().getSimpleName());
        mState.off(context, re);
    }

    /**
     * Update the widget when the rule is ready.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void ready(Context context, RuleEntity re) {
        mState.ready(context, re);
    }

    /**
     * Show the progress bar while the rule key is syncing.
     *
     * @param context Context
     * @param appWidgetIds Array of widgets to be updated
     */
    public void syncing(Context context, int[] appWidgetIds) {
        mState.syncing(context, appWidgetIds);
    }

    /**
     * Update the widget for add rule operation.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     */
    public void addRule(Context context, int widgetId) {
        mState.addRule(context, widgetId);
    }

    /**
     * Set the current state of the widget.
     *
     * @param state - Current state
     */
    public void setState(AbstractState state) {
        mState = state;
    }

    /**
     * Put the widget in the default state.
     */
    public void deactivate() {
        mState.deactivate(this);
    }

    /**
     * Set the diable state of the widget.
     *
     * @param widgetId - disable widget id
     */
    public void disable(Context context, int widgetId) {
        mState.disable(context, widgetId);
    }

     /**
     * Set the enable state of the widget.
     *
     * @param widgetId - enable widget id
     */
    public void enable(Context context, int widgetId) {
        mState.enable(context, widgetId);
    }
}
