/*
 * @(#)AbstractState.java
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
 * Abstract implementation of the state.
 *<code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements BaseState
 *
 *
 * RESPONSIBILITIES:
 *  Abstract implementation.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public abstract class AbstractState implements
        BaseState {

    @SuppressWarnings("unused")
    private static final String    TAG = AbstractState.class.getSimpleName();

    /** NO OP
     */
    public void manual(RuleManager ruleContext) {}

    /** NO OP
     */
    public void auto(RuleManager ruleContext) {}

    /** NO OP
     */
    public void on(Context context, RuleEntity re) {}

    /** NO OP
     */
    public void off(Context context, RuleEntity re) {}

    /** NO OP
     */
    public void ready(Context context, RuleEntity re) {}

    /** NO OP
     */
    public void syncing(Context context, int[] appWidgetIds) {}

    /** NO OP
     */
    public void addRule(Context context, int widgetId) {}

    /** Switch to default state.
     */
    public void deactivate(RuleManager ruleContext) {
        ruleContext.setState(AddRuleState.getInstance());
    }

    /** NO OP
     */
    public void disable(Context context, int widgetId) {}

    /** NO OP
     */
    public void enable(Context context, int widgetId) {}
}
