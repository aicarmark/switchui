/*
 * @(#)BaseState.java
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
package com.motorola.contextual.smartrules.homescreen.state;

import android.content.Context;

import com.motorola.contextual.smartrules.homescreen.RuleEntity;
import com.motorola.contextual.smartrules.homescreen.RuleManager;

/**
 * Defines the widget state.
 *<code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *      Defines the widget state.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public interface BaseState {

    /** Update the widget for add rule operation.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     */
    public void addRule(Context context, int appWidgetId);

    /** Switch to manual mode.
     */
    public void manual(RuleManager ruleContext);

    /** Switch to auto mode.
     */
    public void auto(RuleManager ruleContext);

    /** Update the widget when the rule is being synced.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     */
    public void syncing(Context context, int[] appWidgetIds);

    /** Update the widget when the rule is active.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void on(Context context, RuleEntity re);

    /** Update the widget when the rule is disabled.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void off(Context context, RuleEntity re);

    /** Update the widget when the rule is ready.
     *
     * @param context - Context
     * @param appWidgetIds - Array of widgets to be updated
     * @param ruleKey - Rule key to be used during toggle operation.
     */
    public void ready(Context context, RuleEntity re);

    /** Put the widget in the default state.
     */
    public void deactivate(RuleManager ruleContext);
}
