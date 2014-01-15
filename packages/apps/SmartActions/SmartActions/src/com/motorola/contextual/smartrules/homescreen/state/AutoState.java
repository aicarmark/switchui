/*
 * @(#)AutoState.java
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
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;
import com.motorola.contextual.smartrules.service.SmartRulesService;

/**
 * Automatic state. <code><pre>
 * CLASS:
 * 	 extends AbstractState
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *      Provides all the functions of automatic state.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class AutoState extends AbstractState {
    private static AutoState sInstance;

    /**
     * Constructor
     */
    private AutoState() {}

    /**
     * Get the singleton instance.
     *
     * @return Auto instance
     */
    public static AutoState getInstance() {
        if (sInstance == null) sInstance = new AutoState();
        return sInstance;
    }

    /**
     * Update the widget when the rule is active.
     */
    @Override
    public void on(Context context, RuleEntity re) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_auto);

        views.setImageViewResource(R.id.imageView1, R.drawable.ic_drivemode_car_on);
        views.setInt(R.id.imageView1, "setBackgroundResource", R.drawable.drive_mode_icon_on_bg);
        views.setTextViewText(R.id.status, context.getText(R.string.auto));
        views.setTextColor(R.id.status, context.getResources().getColor(R.color.active_blue));
        views.setTextViewText(R.id.mode, re.getRuleName());
        views.setImageViewResource(
            R.id.imageView1,
            context.getResources().getIdentifier(re.getRuleIcon(), "drawable",
                context.getPackageName()));

        try {
            IntentBuilder toggle = IntentBuilder.create(context, SmartRulesService.class);
            toggle.setAutoOff().setRuleKey(re.getRuleKey()).setRequestCode(re.hashCode());
            PendingIntent pIntent = toggle.build(context, PendingIntent.FLAG_ONE_SHOT
                    | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.imageView1, pIntent);
            IntentBuilder rulesEditor = IntentBuilder.create(context, EditRuleActivity.class);
            rulesEditor.setRuleId(context, re.getRuleId()).setRequestCode(re.hashCode()).clearTaskStack();
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_auto);

        views.setInt(R.id.imageView1, "setBackgroundResource", R.drawable.drive_mode_icon_off_bg);
        views.setImageViewResource(R.id.imageView1, R.drawable.ic_drivemode_car_off);
        views.setTextViewText(R.id.status, context.getText(R.string.disabled));
        views.setTextColor(R.id.status, context.getResources().getColor(R.color.white_30_alpha));
        views.setTextViewText(R.id.mode, re.getRuleName());
        views.setImageViewResource(
            R.id.imageView1,
            context.getResources().getIdentifier(re.getRuleIcon(), "drawable",
                context.getPackageName()));

        IntentBuilder toggle;
        try {
            toggle = IntentBuilder.create(context, SmartRulesService.class);
            toggle.setAutoOn()
            .setRuleKey(re.getRuleKey())
            .setRequestCode(re.hashCode());

            PendingIntent pIntent = toggle.build(context, PendingIntent.FLAG_ONE_SHOT
                    | PendingIntent.FLAG_UPDATE_CURRENT);

            views.setOnClickPendingIntent(R.id.imageView1, pIntent);

            IntentBuilder rulesEditor = IntentBuilder.create(context, EditRuleActivity.class);
            rulesEditor.setRuleId(context, re.getRuleId()).setRequestCode(re.hashCode()).clearTaskStack();

            pIntent = rulesEditor.build(context, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.ruleEditor, pIntent);

            mgr.updateAppWidget(re.getWidgetIds(), views);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    /**
     * Update the widget when the rule is ready.
     */
    @Override
    public void ready(Context context, RuleEntity re) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_auto);

        views.setTextViewText(R.id.status, context.getText(R.string.auto));
        views.setTextColor(R.id.status, context.getResources().getColor(R.color.white_80_alpha));
        views.setImageViewResource(R.id.imageView1, R.drawable.ic_drivemode_car_on);
        views.setInt(R.id.imageView1, "setBackgroundResource", R.drawable.drive_mode_icon_on_bg);
        views.setTextViewText(R.id.mode, re.getRuleName());
        views.setImageViewResource(
            R.id.imageView1,
            context.getResources().getIdentifier(re.getRuleIcon(), "drawable",
                context.getPackageName()));

        try {
            IntentBuilder toggle = IntentBuilder.create(context, SmartRulesService.class);
            toggle.setAutoOff().setRuleKey(re.getRuleKey()).setRequestCode(re.hashCode());
            PendingIntent pIntent = toggle.build(context, PendingIntent.FLAG_ONE_SHOT
                    | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.imageView1, pIntent);
            IntentBuilder rulesEditor = IntentBuilder.create(context, EditRuleActivity.class);
            rulesEditor.setRuleId(context, re.getRuleId()).setRequestCode(re.hashCode()).clearTaskStack();
            pIntent = rulesEditor.build(context, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.ruleEditor, pIntent);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mgr.updateAppWidget(re.getWidgetIds(), views);
    }
}