/*
 * @(#)ManualState.java
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
import android.widget.RemoteViews;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.homescreen.IntentBuilder;
import com.motorola.contextual.smartrules.homescreen.RuleEntity;
import com.motorola.contextual.smartrules.service.SmartRulesService;

/**
 * Manual State. <code><pre>
 * CLASS:
 * 	 extends AbstractState
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 * 		Provides all the functions of manual state.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class ManualState extends AbstractState {
    @SuppressWarnings("unused")
    private static final String TAG = ManualState.class.getSimpleName();
    private static ManualState  sInstance;

    /**
     * Constructor
     */
    private ManualState() {}

    /**
     * Get the singleton instance.
     *
     * @return Manual instance.
     */
    public static ManualState getInstance() {
        if (sInstance == null) sInstance = new ManualState();
        return sInstance;
    }

    /**
     * Update the widget when the rule is active.
     */
    @Override
    public void on(Context context, RuleEntity re) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_manual);

        views.setTextViewText(R.id.status, context.getText(R.string.on));
        views.setTextColor(R.id.status, context.getResources().getColor(R.color.active_blue));
        views.setImageViewResource(R.id.imageView1, R.drawable.ic_drivemode_car_on);
        views.setInt(R.id.imageView1, "setBackgroundResource", R.drawable.drive_mode_icon_on_bg);
        views.setTextViewText(R.id.mode, re.getRuleName());
        views.setImageViewResource(
            R.id.imageView1,
            context.getResources().getIdentifier(re.getRuleIcon(), "drawable",
                context.getPackageName()));

        try {
            PendingIntent pIntent = IntentBuilder.create(context, SmartRulesService.class).setManualOff().setRuleKey(
                re.getRuleKey()).setRequestCode(re.hashCode()).build(context,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
            //if (Constants.LOG_DEBUG) Log.d(TAG, "ON -->" + intent.toUri(0));
            views.setOnClickPendingIntent(R.id.imageView1, pIntent);
            views.setOnClickPendingIntent(R.id.imageView1, pIntent);
            pIntent = IntentBuilder.getRulesEditor(context, re.getRuleId(), re.hashCode()).build(
                context, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.ruleEditor, pIntent);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mgr.updateAppWidget(re.getWidgetIds(), views);
    }

    /**
     * Update the widget when the rule is disabled.
     */
    @Override
    public void off(Context context, RuleEntity re) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_manual);
        views.setTextViewText(R.id.status, context.getText(R.string.off));
        views.setTextColor(R.id.status, context.getResources().getColor(R.color.white_30_alpha));
        views.setImageViewResource(R.id.imageView1, R.drawable.ic_drivemode_car_off);
        views.setInt(R.id.imageView1, "setBackgroundResource", R.drawable.drive_mode_icon_off_bg);
        views.setTextViewText(R.id.mode, re.getRuleName());
        views.setImageViewResource(
            R.id.imageView1,
            context.getResources().getIdentifier(re.getRuleIcon(), "drawable",
                context.getPackageName()));

        try {
            PendingIntent pIntent = IntentBuilder.create(context, SmartRulesService.class).setManualOn().setRuleKey(
                re.getRuleKey()).setRequestCode(re.hashCode()).build(context,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.imageView1, pIntent);
            pIntent = IntentBuilder.getRulesEditor(context, re.getRuleId(), re.hashCode()).build(
                context, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.ruleEditor, pIntent);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mgr.updateAppWidget(re.getWidgetIds(), views);
    }
}
