/*
 * @(#)BootCompleteReceiver.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 * rdq478       2011/09/22  IKSTABLE6-10141     Add workaround for wp service
 */

package com.motorola.contextual.actions;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.motorola.contextual.smartrules.service.DumpDbService;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.smartrules.util.Util.AlarmDetails;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.util.Log;

/**
 * This class handles the com.motorola.intent.action.BOOT_COMPLETE_RECEIVED intent received on power up
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver.
 *
 * RESPONSIBILITIES:
 *     Launches SettingsService
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class BootCompleteReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + BootCompleteReceiver.class.getSimpleName();
    private Context mContext = null;
    private AlarmManager mAlarmMan;
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	mContext = context;
    	String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Null intent or action");
            return;
        }
        if (action.equals(BOOT_COMPLETE_INTENT)) {
            mContext.startService(new Intent(BOOT_COMPLETE_INTENT, null, context,
                                            SettingsService.class));
            Intent dumpDb_bootupServiceIntent = new Intent(mContext, DumpDbService.class);
            dumpDb_bootupServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, DumpDbService.BOOTUP_REQUEST);
            mContext.startService(dumpDb_bootupServiceIntent);
                                                           
            //setup the AlarmManager to wake up weekly and send pending intent to 
            //start service to write to db
            triggerTimerInitServiceForWeeklyDBWrite();

            /*
             * Work around for IKSTABLE6-10141, see WallpaperService.java for detail.
             * This service should be started only if our internal storage is empty
             * and the current wallpaper is not a live wallpaper.
             */
            WallpaperManager wallManager = WallpaperManager.getInstance(mContext);
            if ( wallManager.getWallpaperInfo() == null && !FileUtils.isFileExist(mContext, WP_DIR, WP_DEFAULT) ) {
                Intent wpIntent = new Intent(mContext, WallpaperService.class);
                wpIntent.putExtra(EXTRA_SAVE_DEFAULT, true);
                mContext.startService(wpIntent);
            }
        } else {
            Log.w(TAG, "Action not recognized");
        }

    }
    
    
    /**
     * Calculates the time in ms to first Sun midnight , and sets up weekly recurring 
     * wake and pending intent to start service to write to the debug db
     */
    public void triggerTimerInitServiceForWeeklyDBWrite(){
    	Intent dumpDb_weeklyServiceIntent = new Intent(mContext, DumpDbService.class);
    	dumpDb_weeklyServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, DumpDbService.WEEKLY_UPDATE_REQUEST);
                                   
        mAlarmMan = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(mContext, 0, dumpDb_weeklyServiceIntent, 0);
                   
        AlarmDetails alarmTime = Util.getAlarmTimeForComingSunday();
        //sets recurring and RTC soft wake i.e. would trigger this after first wake past this time,
        //but would not trigger a hard wake at the tick of the time
        // From google docs "This alarm does not wake the device up; if it goes off while the device is asleep, 
        // it will not be delivered until the next time the device wakes up"
        mAlarmMan.setInexactRepeating(AlarmManager.RTC, alarmTime.firstWakeToSunday, alarmTime.weeklyCycles, pi);
     }
}
