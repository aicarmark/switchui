/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * bntw34                      02/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Helper, maintains database create/check existence/open
 * 
 * @author BNTW34
 */
public class BatterySQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_MODES = "modes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PRESET = "preset";
    public static final String COLUMN_BRIGHTNESS = "brightness";
    public static final String COLUMN_TIMEOUT = "timeout";
    public static final String COLUMN_WIFI = "wifi";
    public static final String COLUMN_BLUETOOTH = "bluetooth";
    public static final String COLUMN_MOBILEDATA = "mobiledata";
    public static final String COLUMN_SYNC = "sync";
    public static final String COLUMN_RADIO = "radio";
    public static final String COLUMN_HAPTIC = "haptic";
    public static final String COLUMN_VIBRATION = "vibration";
    public static final String COLUMN_ROTATION = "rotation";

    public static int COLUMN_ID_INDEX = 0;
    public static int COLUMN_NAME_INDEX = 1;
    public static int COLUMN_PRESET_INDEX = 2;
    public static int COLUMN_BRIGHTNESS_INDEX = 3;
    public static int COLUMN_TIMEOUT_INDEX = 4;
    public static int COLUMN_WIFI_INDEX = 5;
    public static int COLUMN_BLUETOOTH_INDEX = 6;
    public static int COLUMN_MOBILEDATA_INDEX = 7;
    public static int COLUMN_SYNC_INDEX = 8;
    public static int COLUMN_RADIO_INDEX = 9;
    public static int COLUMN_HAPTIC_INDEX = 10;
    public static int COLUMN_VIBRATION_INDEX = 11;
    public static int COLUMN_ROTATION_INDEX = 12;

    private static final String DATABASE_NAME = "modes.db";
    private static final int DATABASE_VERSION = 1;
    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_MODES + "( " + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_PRESET
            + " integer, " + COLUMN_BRIGHTNESS
            + " integer, " + COLUMN_TIMEOUT
            + " integer, " + COLUMN_WIFI
            + " integer, " + COLUMN_BLUETOOTH
            + " integer, " + COLUMN_MOBILEDATA
            + " integer, " + COLUMN_SYNC
            + " integer, " + COLUMN_RADIO
            + " integer, " + COLUMN_HAPTIC
            + " integer, " + COLUMN_VIBRATION
            + " integer, " + COLUMN_ROTATION
            + " integer"
            + ");";

    public BatterySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(BatterySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MODES);
        onCreate(db);
    }

}
