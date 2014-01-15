package com.motorola.firewall;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.motorola.firewall.FireWall.Settings;

public class FirewallAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "FirewallAppWidgetProvider";

    private static final String ACTION_WIDGET_VIP = "VipWidget";
    private static final String ACTION_WIDGET_RECEIVER = "ActionReceiverWidget";

    private static final ComponentName THIS_APPWIDGET = new ComponentName(
            "com.motorola.firewall",
            "com.motorola.firewall.FirewallAppWidgetProvider");

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ContentResolver mresolver = context.getContentResolver();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.firewall_appwidget);

       if (FireWallSetting.isVipMode(mresolver)){
            views.setImageViewResource(R.id.on_off, R.drawable.vip_settings_ind_on_l);
            views.setImageViewResource(R.id.widget_viponoff, R.drawable.ic_vip_settings_data_connection_on_l);
        }else {
            views.setImageViewResource(R.id.on_off, R.drawable.vip_settings_ind_off_l);
            views.setImageViewResource(R.id.widget_viponoff, R.drawable.ic_vip_settings_data_connection_off_l);
        }

        Intent configIntent = new Intent(context, FireWallTab.class);
        configIntent.setAction(ACTION_WIDGET_VIP);
        PendingIntent vipPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
        views.setOnClickPendingIntent(R.id.vip, vipPendingIntent);

        Intent activeIntent = new Intent(context, FirewallAppWidgetProvider.class);
        activeIntent.setAction(ACTION_WIDGET_RECEIVER);
        PendingIntent activePendingIntent = PendingIntent.getBroadcast(context, 0, activeIntent, 0);
        views.setOnClickPendingIntent(R.id.vip_on_off, activePendingIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    public void onReceive(Context context, Intent intent) {
        ContentResolver mresolver = context.getContentResolver();

        if(intent.getAction().equals(ACTION_WIDGET_RECEIVER)){
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.firewall_appwidget);
            if (FireWallSetting.isVipMode(mresolver)){
                views.setImageViewResource(R.id.on_off, R.drawable.vip_settings_ind_off_l);
                views.setImageViewResource(R.id.widget_viponoff, R.drawable.ic_vip_settings_data_connection_off_l);

                FireWallSetting.setDBValue(mresolver, Settings.INBLOCK_CALL_ON, FireWallSetting.getDBValue(mresolver, Settings.VINBLOCK_CALL_ON));
                FireWallSetting.setDBValue(mresolver, Settings.INBLOCK_SMS_ON, FireWallSetting.getDBValue(mresolver, Settings.VINBLOCK_SMS_ON));
                FireWallSetting.setDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_BLOCK_CALL_LIST));
                FireWallSetting.setDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_BLOCK_SMS_LIST));

                FireWallSetting.setDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_CALL_REJECT_CDMA));
                FireWallSetting.setDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_SMS_REJECT_CDMA));
                FireWallSetting.setDBValue(mresolver, Settings.IN_CALL_REJECT_GSM, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_CALL_REJECT_GSM));
                FireWallSetting.setDBValue(mresolver, Settings.IN_SMS_REJECT_GSM, FireWallSetting.getDBValue(mresolver,
                                            Settings.VIN_SMS_REJECT_GSM));
                context.stopService(new Intent("com.motorola.firewall.action.STOP"));
                if(
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON)) ||
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON)) ||
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA)) ||
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM)) ||
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA)) ||
                    Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM))
                )
                    context.startService(new Intent("com.motorola.firewall.action.START"));
            }else {
                context.stopService(new Intent("com.motorola.firewall.action.STOP"));
                views.setImageViewResource(R.id.on_off, R.drawable.vip_settings_ind_on_l);
                views.setImageViewResource(R.id.widget_viponoff, R.drawable.ic_vip_settings_data_connection_on_l);

                FireWallSetting.setDBValue(mresolver, Settings.VINBLOCK_CALL_ON, FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON));
                FireWallSetting.setDBValue(mresolver, Settings.VINBLOCK_SMS_ON, FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON));
                FireWallSetting.setDBValue(mresolver, Settings.VIN_BLOCK_CALL_LIST, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_BLOCK_CALL_LIST));
                FireWallSetting.setDBValue(mresolver, Settings.VIN_BLOCK_SMS_LIST, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_BLOCK_SMS_LIST));

                FireWallSetting.setDBValue(mresolver, Settings.VIN_CALL_REJECT_CDMA, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_CALL_REJECT_CDMA));
                FireWallSetting.setDBValue(mresolver, Settings.VIN_SMS_REJECT_CDMA, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_SMS_REJECT_CDMA));
                FireWallSetting.setDBValue(mresolver, Settings.VIN_CALL_REJECT_GSM, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_CALL_REJECT_GSM));
                FireWallSetting.setDBValue(mresolver, Settings.VIN_SMS_REJECT_GSM, FireWallSetting.getDBValue(mresolver,
                                            Settings.IN_SMS_REJECT_GSM));

                FireWallSetting.setDBValue(mresolver, Settings.INBLOCK_CALL_ON, "true");
                FireWallSetting.setDBValue(mresolver, Settings.INBLOCK_SMS_ON, "true");
                FireWallSetting.setDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST, "in_call_whitelist");
                FireWallSetting.setDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST, "in_sms_whitelist");

                FireWallSetting.setDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA, "false");
                FireWallSetting.setDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA, "false");
                FireWallSetting.setDBValue(mresolver, Settings.IN_CALL_REJECT_GSM, "false");
                FireWallSetting.setDBValue(mresolver, Settings.IN_SMS_REJECT_GSM, "false");

                context.startService(new Intent("com.motorola.firewall.action.START"));
                Toast.makeText(context, R.string.vip_mode_on_toast, Toast.LENGTH_LONG).show();
            }

            final AppWidgetManager gm = AppWidgetManager.getInstance(context);
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
        super.onReceive(context, intent);
    }
}
