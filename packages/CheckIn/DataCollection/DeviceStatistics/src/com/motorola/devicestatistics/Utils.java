/*
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: Utils.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 18, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.devicestatistics;

/**
 * @author A24178
 *
 */

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

public class Utils {
    private final static String LOG_TAG = "DeviceStatUtils";
    private final static String POWER_SUPPLY_PATH = "/sys/class/power_supply";
    private final static String UEVENT_FILENAME = "uevent";
    private final static String CAPACITY_FILENAME = "capacity";
    public final static String FILENAME_SEPARATOR = "/";
    private static String sBatteryPath = null;

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

    static String readSysFile(String filename, int max) {
        FileInputStream sysfile = null;
        byte[] bArray = new byte[max];
        String str = null;

        try {
            sysfile = new FileInputStream(filename);
            int bsize = sysfile.read(bArray);
            str = parseIntoLines(bArray, bsize);
        } catch (FileNotFoundException e) {
            android.util.Log.d(LOG_TAG, "Sys File: File not found" + filename);
        } catch (IOException e) {
            android.util.Log.d(LOG_TAG, "Sys File: Read failed" + filename);
        }catch (IndexOutOfBoundsException e) {
            android.util.Log.d(LOG_TAG, "Sys File: Output parsing failed"
                    + filename);
        }finally {
            if(sysfile != null) {
                try {
                    sysfile.close();
                }catch (Exception e) {}
            }
        }
        return str;
    }

    // In the passed directory location, search and return absolute folder path
    // containing file "type" with contents "Battery"
    public static String getBatteryPath() {

        if (sBatteryPath != null) return sBatteryPath;

        final String typeFileName = "type";
        File myDir = new File(POWER_SUPPLY_PATH);

        File[] subdirs = myDir.listFiles();
        if (subdirs == null) return null;

        for (int i = 0; i < subdirs.length; i++) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                String filePath =  subdirs[i].getPath() +FILENAME_SEPARATOR + typeFileName;

                fileReader = new FileReader(filePath);
                bufferedReader = new BufferedReader(fileReader);

                String type = bufferedReader.readLine();
                if (type != null && type.equals("Battery")) {
                    sBatteryPath = subdirs[i].getPath();
                    return sBatteryPath;
                }
            } catch (FileNotFoundException e1) {
                android.util.Log.d(LOG_TAG, "File not found. file = "+ typeFileName);
            } catch (IOException e1) {
                android.util.Log.d(LOG_TAG, "Read failed for file = " + typeFileName);
            } catch (Exception e1) {
                android.util.Log.d(LOG_TAG, "Exception reading " + typeFileName);
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Exception e) {}
                }
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (Exception e) {}
                }
            }
        }
        return null;
    }

    public static String collectBatteryStats() {
        String dirPath = getBatteryPath();
        if (dirPath != null) {
            String str = readSysFile(dirPath + FILENAME_SEPARATOR + UEVENT_FILENAME, 512);
            return str;
        }
        return null;
    }

    public static String getBattCapacity() {
        String dirPath = getBatteryPath();
        if (dirPath != null) {
            String str = readSysFile(dirPath + FILENAME_SEPARATOR + UEVENT_FILENAME, 512);

            if(str != null) {
                String[] split = str.split("POWER_SUPPLY_CAPACITY=");
                if(split.length > 1) {
                    String[] capacity = split[1].split("\n");
                    if(capacity.length > 0) {
                        return capacity[0];
                    }
                }
            }
        }
        return "Read Failed";
    }

    public static int getCurrentBatteryLevel() {
        final String BATTERY_FILE_NAME = "/sys/devices/platform/cpcap_battery/power_supply/battery/charge_counter";
        int ret_level = -1;

        // read battery level from the charge_counter file and return the level
        try {
             ret_level = readBatteryLevel (BATTERY_FILE_NAME);
        } catch (FileNotFoundException e) {
            String dirPath = getBatteryPath();
            if (dirPath != null) {
                final String ALT_BATT_FILE_NAME = dirPath + FILENAME_SEPARATOR + CAPACITY_FILENAME;
                try {
                    ret_level = readBatteryLevel (ALT_BATT_FILE_NAME);
                } catch (FileNotFoundException e1) {
                    android.util.Log.d(LOG_TAG, "Sys File: File not found " + ALT_BATT_FILE_NAME);
                }
            }
        }
        return ret_level;
    }

    private static int readBatteryLevel (String fileName) throws FileNotFoundException
    {
        int ret_level = -1;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            String level = bufferedReader.readLine();

            if (level != null) {
                // To avoid a "potential tainted data vulnerability" klocwork report.
                // See http://goo.gl/gLa8J
                level = Normalizer.normalize(level, Form.NFKC);

                ret_level = Integer.parseInt(level);

                // Sometimes battery levels are read to be greater than 100. This is due to
                // overcharging, but the battery is ideally 100% only.
                if (ret_level > 100) ret_level = 100;
            }
        } catch (FileNotFoundException e1) {
            throw e1;
        } catch (IOException e1) {
            android.util.Log.d(LOG_TAG, "Sys File: Read failed " + fileName);
        } catch (Exception e1) {
            android.util.Log.d(LOG_TAG, "Sys File: Exception reading " + fileName);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {}
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception e) {}
            }
        }
        return ret_level;
    }
    public static class Log {

        public static void i(String tag, String log) {
            android.util.Log.i(tag, log);
        }

        public static void d(String tag, String log) {
            android.util.Log.d(tag, log);
        }

        public static void d(String tag, String log, Exception ex) {
            android.util.Log.d(tag, log, ex);
        }

        public static void v(String tag, String log) {
            android.util.Log.v(tag, log);
        }

        public static void e(String tag, String log) {
            android.util.Log.e(tag, log);
        }
    }

    // Class that facilitates offloading of processing from the main thread so as to
    // help avoid ANRs
    public static abstract class RunInBackgroundThread implements Runnable {
        private static final String THREAD_NAME = "devstatsBg";
        private static final int THREAD_PRIORITY = Process.THREAD_PRIORITY_DEFAULT +
            Process.THREAD_PRIORITY_LESS_FAVORABLE;

        private static Handler sHandler;

        static {
            // Create a non-main thread with a handler that runs at a slightly
            // lower priority than the main thread
            HandlerThread bgThread = new HandlerThread(THREAD_NAME, THREAD_PRIORITY);

            // start the handler thread so that the looper gets created
            bgThread.start();

            // create a handler on the queue of the newly created thread, that can later be used
            // to post Runnable's to that thread.
            sHandler = new Handler(bgThread.getLooper());
        }

        public RunInBackgroundThread() {
            // Send this object to the queue of the non-main thread, for asynchronous execution
            sHandler.post(this);
        }
    }


    public static final void saveSharedPreferences(final SharedPreferences.Editor edit) {
        new RunInBackgroundThread() {
            @Override
            public void run() {
                edit.commit();
            }
        };
    }

    public static void addNonZeroValues(DsCheckinEvent checkinEvent, String[] keyValues) {
        if (checkinEvent == null || keyValues == null) return;
        int length = keyValues.length;

        for (int i=0; i+1<length; i+=2) {
            String key = keyValues[i];
            String value = keyValues[i+1];

            try {
                if (Long.valueOf(value) == 0) continue;
            } catch (Exception e) {
                android.util.Log.e(LOG_TAG, "addPositiveValues " + value, e);
            }

            checkinEvent.setValue(key, value);
        }
    }
}
