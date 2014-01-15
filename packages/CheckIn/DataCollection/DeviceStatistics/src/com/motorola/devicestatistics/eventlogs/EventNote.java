/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: EventNote.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 3, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics.eventlogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.devicestatistics.DevStatUtils;
import com.motorola.devicestatistics.DeviceStatsConstants;
import com.motorola.devicestatistics.FileLinesReader;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.GpsTracker;
import com.motorola.devicestatistics.StatsUploader;
import com.motorola.devicestatistics.SysClassNetUtils;
import com.motorola.devicestatistics.TimeTracker;
import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.eventlogs.EventConstants.Events;


public class EventNote {

    private final static String LOG_TAG = "EventNote";
    private final static boolean DUMP = true;

    private final static Object sLock = new Object();

    public static void noteEvent(Context context, Intent intent, long now) {
        if(intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        SharedPreferences sp = context.getSharedPreferences(EventConstants.OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spe = sp.edit();
        //TODO: Is spe cleaned up correctly in all cases below?
        synchronized(sLock) {
            if(action.equals(Intent.ACTION_BATTERY_LOW)) {
                String pre = sp.getString(EventConstants.BATTLOW_TIME, null);
                if(pre != null) {
                    pre = pre + "," + now + ";" + getBattLevel();
                }else {
                    pre = now + ";" + getBattLevel();
                }
                spe.putString(EventConstants.BATTLOW_TIME, pre);
                Utils.saveSharedPreferences(spe);
            }else if(action.equals(Intent.ACTION_SHUTDOWN)) {
                String pre = sp.getString(EventConstants.SHUTDOWN_TIME, null);
                int level = getBattLevel();
                if(pre != null) {
                    pre = pre + "," + now + ";" + level;
                }else {
                    pre = now + ";" + level;
                }
                spe.putString(EventConstants.SHUTDOWN_TIME, pre);
                BatteryLogger.forceEvent(sp, spe, level, now);
                TimeTracker.noteShutdown(sp, spe);
                Utils.saveSharedPreferences(spe);
                EventLoggerService.getInstance(context).wakeupEventLogThread(
                        EventLoggerService.WAKEUP_SHUTDOWN);
            }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                createBootupReport(now, sp, spe);
                TimeTracker.noteBootup(sp, spe);
                EventLoggerService.getInstance(context).handleBootComplete( spe );
                Utils.saveSharedPreferences(spe);
            }else if(action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                BatteryLogger.noteEvent(intent, now, sp, spe);
                GpsTracker.noteIntent(context, intent);
            }else if(action.equals(TimeTracker.TRIGGER_ACTION)) {
                TimeTracker.noteTimeout(sp, spe);
                GpsTracker.noteTimeout(context);
            }else if(action.equals(Intent.ACTION_SCREEN_ON)) {
                SysClassNetUtils.updateNetStats(context);
                StatsUploader uploader = new StatsUploader(context);
                uploader.collectWithoutUpload(DataTypes.BATTERY_STATS);
                ScreenState.storeScreenState(context, true);
            }else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
                SysClassNetUtils.updateNetStats(context);
                StatsUploader uploader = new StatsUploader(context);
                uploader.collectWithoutUpload(DataTypes.BATTERY_STATS);
                EventLoggerService.getInstance(context).wakeupEventLogThread(
                        EventLoggerService.WAKEUP_SCREENOFF);
                ScreenState.storeScreenState(context, false);
            }else if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
                context.sendBroadcast( new Intent(DeviceStatsConstants.DEVSTATS_CHECKIN_STATS_ALARM)
                        .putExtra( DeviceStatsConstants.DEVSTATS_CHECKIN_STATS_USB_EXTRA,
                            "1" ) );
            } else if (action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION)) {
                GpsTracker.noteIntent(context, intent);
            }
        }
    }

    private static void createBootupReport(long now, SharedPreferences sp,
            SharedPreferences.Editor spe) {
        StringBuilder sb = new StringBuilder(sp.getString(EventConstants.BOOTUP_TIME, ""));
        Long bootEpochSec = new BootTimeReader().getBootTime();

        // If this boot complete is due to a system server crash, add a "ssrst" report.
        // Otherwise add a btup report below.
        boolean addBootupLog = true;

        if (bootEpochSec != null) {
            Long lastBootTimeSec = sp.getLong(EventConstants.KERNEL_BOOTTIME_REFERENCE, -1);

            if ((long)lastBootTimeSec == (long)bootEpochSec) {
                // If the kernel boot time has not changed wrt that saved earlier in shared
                // preferences, then log a system server restart report.
                if (sb.length()!=0) sb.append('[');
                sb.append("ssrst;").append(now);
                addBootupLog = false;
            } else {
                spe.putLong(EventConstants.KERNEL_BOOTTIME_REFERENCE, bootEpochSec);
            }
        }

        if (addBootupLog) {
            int bootReason = new BootReasonReader().getReason();

            long currentTime = System.currentTimeMillis();
            // find time the device has been up before receiving BOOT_COMPLETED intent
            long timeSinceBoot = SystemClock.elapsedRealtime () - (currentTime - now);
            // time when the phone actually powered up
            long bootupTime = now - timeSinceBoot;

            if (sb.length()!=0) sb.append('[');
            sb.append("btup;").append(bootupTime).append(";").append(bootReason).append(";")
                .append(timeSinceBoot);
        }

        if (DUMP) Log.d(LOG_TAG, " pre = " + sb.toString());
        spe.putString(EventConstants.BOOTUP_TIME, sb.toString());
    }

    public static boolean getEvents(SharedPreferences sp, ILogger logger) {
        boolean found = false;
        int source = EventConstants.Source.RECEIVER;

        synchronized(sLock) {
            String times = sp.getString(EventConstants.SHUTDOWN_TIME, null);
            if(times != null) {
                found = true;
                if(times.contains(",")) {
                    String[] parts = times.split(",");
                    for(int i = 0; i < parts.length; ++i) {
                        logger.log(source, Events.SHUTDOWN, EventConstants.CHECKIN_ID,
                                "shtdn;" + parts[i]);
                    }
                }else {
                    logger.log(source, Events.SHUTDOWN, EventConstants.CHECKIN_ID,
                            "shtdn;" + times);
                }
            }
            times = sp.getString(EventConstants.BATTLOW_TIME, null);
            if(times != null) {
                found = true;
                if(times.contains(",")) {
                    String[] parts = times.split(",");
                    for(int i = 0; i < parts.length; ++i) {
                        logger.log(source, Events.BATTLOW, EventConstants.CHECKIN_ID,
                                "btlow;" + parts[i]);
                    }
                }else {
                    logger.log(source, Events.BATTLOW, EventConstants.CHECKIN_ID,
                            "btlow;" + times);
                }
            }
            times = sp.getString(EventConstants.BOOTUP_TIME, null);
            if(times != null) {
                found = true;
                logger.log(source, Events.BOOTUP, EventConstants.CHECKIN_ID, times);
            }
            SharedPreferences.Editor spe = sp.edit();
            if(found) {
                spe.remove(EventConstants.SHUTDOWN_TIME);
                spe.remove(EventConstants.BATTLOW_TIME);
                spe.remove(EventConstants.BOOTUP_TIME);
            }
            found = BatteryLogger.getEvents(sp, spe, logger) ? true : found;
            if(found) Utils.saveSharedPreferences(spe);
        }
        return found;
    }
    
    public static int getBattLevel() {
        return Utils.getCurrentBatteryLevel();
    }

    /**
     * Class that reads the kernel boot time from /proc/stat
     *
     */
    static final class BootTimeReader extends FileLinesReader {
        private static final String BOOT_TIME_STRING_START = "btime";
        private static final String BOOT_TIME_FILE = "/proc/stat";
        private static final int MAX_LINES_TO_READ = 20;
        private static final int TIME_INDEX = 1;
        private static final String SPACE = " ";

        private Long mBootTimeSec;

        BootTimeReader() {
            // Read up to 20 lines from /proc/stat
            super(BOOT_TIME_FILE,MAX_LINES_TO_READ);
            readLines();
        }

        protected boolean processLine(String line) {
            // An example line that we are looking for is "btime 1326200514"
            if (!line.startsWith(BOOT_TIME_STRING_START)) return true;

            String[] fields = line.split(SPACE);
            try {
                if (fields.length > TIME_INDEX) mBootTimeSec = Long.valueOf(fields[TIME_INDEX]);
            } catch (NumberFormatException e) {
            }

            if (mBootTimeSec==null) {
                Log.e(LOG_TAG, "Error handling " + line + " " + DevStatUtils.getStackTrace());
            }
            return false; // Stop processing additional lines
        }

        final Long getBootTime() {
            return mBootTimeSec;
        }
    };

    /**
     * Class that reads power-up reason from /proc/bootinfo
     *
     */
    static final class BootReasonReader extends FileLinesReader {
        private static final String POWERUP_REASON_FILE = "/proc/bootinfo";
        private static final String BOOT_REASON_STRING_START = "POWERUPREASON";
        private static final int MAX_LINES_TO_READ = 20;
        private static final int POWERUP_REASON_START_OFFSET = 18;
        private static final int POWERUP_REASON_BASE = 16;

        private int mBootReason;

        BootReasonReader() {
            // Read only the first line from /proc/bootinfo
            super(POWERUP_REASON_FILE,MAX_LINES_TO_READ);
            readLines();
        }

        protected boolean processLine(String line) {
            // An example line is "POWERUPREASON : 0x00004000"
            if (!line.startsWith(BOOT_REASON_STRING_START)) return true;
            try {
                mBootReason = Integer.valueOf(line.substring(POWERUP_REASON_START_OFFSET),
                        POWERUP_REASON_BASE);
            } catch (IndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "IndexOutOfBoundsException while reading /proc/bootinfo!");
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "NumberFormatException is thrown when reading file /proc/bootinfo");
            }

            return false; // Stop processing further lines
        }

        final Integer getReason() {
            return mBootReason;
        }
    }
}
