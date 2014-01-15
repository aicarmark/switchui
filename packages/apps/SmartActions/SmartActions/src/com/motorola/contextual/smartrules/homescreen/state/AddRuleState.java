/*
 * @(#)AddRuleState.java
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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.homescreen.RuleManager;


/**
 * Add rule state.
 *<code><pre>
 * CLASS:
 * 	 extends AbstractState
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *  Provides all the functions of Add rule state.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class AddRuleState extends AbstractState {
    private static AddRuleState sInstance;
    private static final String ADD_RULE_INTENT = "com.motorola.contextual.smartrules.drivingmode.activity";

    /** Constructor
     */
    private AddRuleState(){}

    /**
     *  Get the singleton instance.
     * @return AddRule instance.
     */
    public static AddRuleState getInstance() {
        if(sInstance == null)
            sInstance = new AddRuleState();
        return sInstance;
    }

    /** Update the widget for add rule operation.
     */
    @Override
    public void addRule(Context context, int appWidgetId) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout41);
        Intent launchIntent = new Intent(ADD_RULE_INTENT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        launchIntent.putExtra(Constants.EXTRA_REQUEST_ID, appWidgetId);
        PendingIntent pIntent = PendingIntent.getActivity(context, appWidgetId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.linearLayout1, pIntent);
        views.setViewVisibility(R.id.progressBar, View.GONE);
        views.setViewVisibility(R.id.imageView1, View.VISIBLE);
        views.setTextViewText(R.id.textView2,context.getText(R.string.touch_to_setup));
        mgr.updateAppWidget(appWidgetId, views);
    }

    /** Show the progress bar while the rule key is syncing.
     */
    @Override
    public void syncing(Context context, int[] appWidgetIds) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout41);
        views.setViewVisibility(R.id.progressBar, View.VISIBLE);
        views.setViewVisibility(R.id.imageView1, View.GONE);
        views.setTextViewText(R.id.textView2, context.getText(R.string.widget_sync));
        mgr.updateAppWidget(appWidgetIds, views);
    }

    /** Switch to manual mode.
     */
    @Override
    public void manual(RuleManager ruleContext) {
        ruleContext.setState(ManualState.getInstance());
    }

    /** Switch to auto mode.
     */
    @Override
    public void auto(RuleManager ruleContext) {
        ruleContext.setState(AutoState.getInstance());
    }

    /** NO OP
     */
    @Override
    public void deactivate(RuleManager ruleContext) {}

    /** NO OP
     */
    @Override
    public void disable(Context context, int widgetId) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout41);
        Intent launchIntent = new Intent(ADD_RULE_INTENT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        launchIntent.putExtra(Constants.EXTRA_REQUEST_ID, widgetId);
        PendingIntent pIntent = PendingIntent.getActivity(context, widgetId, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if(pIntent != null)
            pIntent.cancel();
        views.setOnClickPendingIntent(R.id.linearLayout1, pIntent);
        views.setViewVisibility(R.id.progressBar, View.GONE);
        views.setViewVisibility(R.id.imageView1, View.VISIBLE);
        views.setTextViewText(R.id.textView2,context.getText(R.string.touch_to_setup));
        mgr.updateAppWidget(widgetId, views);
    }

    /** NO OP
     */
    @Override
    public void enable(Context context, int widgetId) {
        addRule(context, widgetId);
    }
}
