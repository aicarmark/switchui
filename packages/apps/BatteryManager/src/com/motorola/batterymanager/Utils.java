/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: Utils.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 12-06-09       A24178       Removed checkin for Sholes UMTS
 **********************************************************
 */

package com.motorola.batterymanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

//Checkin
//import com.motorola.android.provider.Checkin;

public class Utils {

    private final static String LOG_TAG = "PowerProfileUtils";

    public static String getCurrentTime() {
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(System.currentTimeMillis()));
    }

    private static String parseIntoLines(byte[] array, int size) throws IndexOutOfBoundsException {
        int i = 0;

        while((i < size) && (array[i] != 0)) {
            ++i;
        }

        if(i == 0)
            return null;

        if(i < size) {
            return(new String(array, 0, i) + "\n");
        }else {
            return(new String(array, 0, (size - 1)) + "\n");
        }
    }

    private static String readSysFile(String filename, int max) {
        FileInputStream sysfile;
        byte[] bArray = new byte[max];
        String str = null;

        try {
            sysfile = new FileInputStream(filename);
            int bsize = sysfile.read(bArray);
            str = parseIntoLines(bArray, bsize);
            sysfile.close();
        } catch (FileNotFoundException e) {
            android.util.Log.d(LOG_TAG, "Sys File: File not found" + filename);
        } catch (IOException e) {
            android.util.Log.d(LOG_TAG, "Sys File: Read failed" + filename);
        }catch (IndexOutOfBoundsException e) {
            android.util.Log.d(LOG_TAG, "Sys File: Output parsing failed" 
                    + filename);
        }

        return str;
    }

    public static String collectBatteryStats() {
        String str = readSysFile("/sys/class/power_supply/battery/uevent", 512);
        return str;
    }

    public static void logBattStats() {
        Log.d(LOG_TAG, "Battery Stats: \n" + collectBatteryStats());
    }

    public static String getBattCapacity() {
        String str = readSysFile("/sys/class/power_supply/battery/uevent", 512);

        if(str != null) {
            String[] split = str.split("POWER_SUPPLY_CAPACITY=");
            if(split.length > 1) {
                String[] capacity = split[1].split("\n");
                if(capacity.length > 0) {
                    return capacity[0];
                }
            }
        }
        return "Read Failed";
    }

    public static class Log {

        private final static Uri chkinUri = 
            Uri.parse("content://android.server.checkin_mot/events");

        private static int scheckinLevel = BatteryProfile.DEFAULT_CHECKIN_LEVEL;
        private static ContentResolver scontentResolver = null;
        private static boolean slogflush = true;
        private static ArrayList<ContentValues> sLogs = new ArrayList<ContentValues>();

        public static void i(String tag, String log) {
            if(scheckinLevel <= BatteryProfile.INFO) {
                logtoCheckin(tag, log);
            }
            android.util.Log.i(tag, log);
        }

        public static void d(String tag, String log) {
            if(scheckinLevel <= BatteryProfile.DEBUG) {
                logtoCheckin(tag, log);
            }
            android.util.Log.d(tag, log);
        }
 public static void d(String tag, String log, Exception ex) {
            android.util.Log.d(tag, log, ex);
        }

        public static void v(String tag, String log) {
            if(scheckinLevel <= BatteryProfile.VERBOSE) {
                logtoCheckin(tag, log);
            }
            android.util.Log.v(tag, log);
        }

        public static void e(String tag, String log) {
            if(scheckinLevel <= BatteryProfile.ERROR) {
                logtoCheckin(tag, log);
            }
            android.util.Log.e(tag, log);
        }

        private static void logtoCheckin(String tag, String log) {
            /***
            if(slogflush) {
                if(scontentResolver != null) {
                    ContentValues cv = new ContentValues();
                    cv.put("tag", tag);
                    cv.put("value", log);
                    cv.put("date", System.currentTimeMillis());
                    try {
                        //scontentResolver.insert(chkinUri, cv);
                        Checkin.logEvent(scontentResolver, tag, log);
                    }catch(IllegalArgumentException iaEx) {
                        // Don't crash if there is no Blur 
                    }
                }
            }else {
                ContentValues cv = new ContentValues();
                cv.put("tag", tag);
                cv.put("value", log);
                cv.put("date", System.currentTimeMillis());

                sLogs.add(cv);
            }
            ***/
        }

        public static void flush(ContentResolver cr) {
            if(!sLogs.isEmpty()) {
                scontentResolver = cr;
                /**
                int size = sLogs.size();
                for(int i = 0; i < size; ++i) {
                    try {
                        //cr.insert(chkinUri, sLogs.get(i));
                        String tag = sLogs.get(i).getAsString("tag");
                        String log = sLogs.get(i).getAsString("value");
                        Checkin.logEvent(scontentResolver, tag, log);
                        sLogs.get(i).clear();
                    }catch(IllegalArgumentException iaEx) {
                        // Don't crash if no Blur
                    }
                }
                **/
                sLogs.clear();
            }
        }

        public static void setLogFlush(boolean enabled) {
            slogflush = enabled;
        }

        public static void setCheckinLevel(int level) {
            scheckinLevel = level;
        }

        public static void setContentResolver(ContentResolver cr) {
            scontentResolver = cr;
        }
    }
}

