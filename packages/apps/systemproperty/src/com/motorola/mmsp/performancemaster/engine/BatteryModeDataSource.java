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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * our DAO class, maintains database connection, do insert/fetch
 * 
 * @author BNTW34
 */
public class BatteryModeDataSource {
    private static final String LOG_TAG = "BatteryDAO: ";
    // Database fields
    private SQLiteDatabase database;
    private BatterySQLiteHelper dbHelper;
    private String[] allColumns = {
            BatterySQLiteHelper.COLUMN_ID,
            BatterySQLiteHelper.COLUMN_NAME,
            BatterySQLiteHelper.COLUMN_PRESET,
            BatterySQLiteHelper.COLUMN_BRIGHTNESS,
            BatterySQLiteHelper.COLUMN_TIMEOUT,
            BatterySQLiteHelper.COLUMN_WIFI,
            BatterySQLiteHelper.COLUMN_BLUETOOTH,
            BatterySQLiteHelper.COLUMN_MOBILEDATA,
            BatterySQLiteHelper.COLUMN_SYNC,
            BatterySQLiteHelper.COLUMN_RADIO,
            BatterySQLiteHelper.COLUMN_HAPTIC,
            BatterySQLiteHelper.COLUMN_VIBRATION,
            BatterySQLiteHelper.COLUMN_ROTATION
    };

    public BatteryModeDataSource(Context context) {
        dbHelper = new BatterySQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
        Log.e(LOG_TAG, "open: getWritableDatabase");
    }

    public void close() {
        dbHelper.close();
        Log.e(LOG_TAG, "close: ");
    }

    public BatteryModeData createBatteryMode(String modeName) {
        if (database == null) {
            Log.e(LOG_TAG, "createBatteryMode db==null");
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(BatterySQLiteHelper.COLUMN_NAME, modeName);
        long insertId = database.insert(BatterySQLiteHelper.TABLE_MODES, null,
                values);
        Cursor cursor = database.query(BatterySQLiteHelper.TABLE_MODES,
                allColumns, BatterySQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);

        cursor.moveToFirst();
        BatteryModeData newBatteryMode = cursorToBatteryModeData(cursor);
        cursor.close();

        Log.e(LOG_TAG, "createBatteryMode modeName=" + modeName);

        return newBatteryMode;
    }

    public BatteryModeData getBatteryMode(long id) {
        if (database == null) {
            Log.e(LOG_TAG, "getBatteryMode database==null");
            return null;
        }

        Cursor cursor = database.query(BatterySQLiteHelper.TABLE_MODES,
                allColumns, BatterySQLiteHelper.COLUMN_ID + " = " + id,
                null, null, null, null);

        cursor.moveToFirst();
        BatteryModeData batteryMode = cursorToBatteryModeData(cursor);
        cursor.close();

        Log.e(LOG_TAG, "getBatteryMode");
        if (batteryMode != null) {
            Log.e(LOG_TAG, "getBatteryMode id=" + id + " name=" + batteryMode.getModeName());
        }

        return batteryMode;
    }
    
    public boolean isModeNameUsed(String modeName) {
        if (database == null) {
            Log.e(LOG_TAG, "isModeNameUsed");
            return false;
        }
        
        String[] selectArgs = new String[1];
        selectArgs[0] = modeName;
        
        Cursor cursor = database.query(BatterySQLiteHelper.TABLE_MODES,
                allColumns,
                BatterySQLiteHelper.COLUMN_NAME + "=?"
                + " and " + BatterySQLiteHelper.COLUMN_PRESET + "=0",
                selectArgs, null, null, null);
        
        cursor.moveToFirst();
        boolean bUsed = !cursor.isAfterLast();
        cursor.close();
        
        return bUsed;        
    }

    public void deleteBatteryMode(BatteryModeData modeData) {
        long id = modeData.getId();

        if (database == null) {
            Log.e(LOG_TAG, "deleteBatteryMode database==null");
            return;
        }

        database.delete(BatterySQLiteHelper.TABLE_MODES, BatterySQLiteHelper.COLUMN_ID
                + " = " + id, null);

        Log.e(LOG_TAG, "BatteryMode deleted with id: " + id
                + " modeName: " + modeData.getModeName());
    }

    public void deleteBatteryMode(long id) {
        if (database == null) {
            Log.e(LOG_TAG, "deleteBatteryMode database == null");
            return;
        }

        database.delete(BatterySQLiteHelper.TABLE_MODES, BatterySQLiteHelper.COLUMN_ID
                + " = " + id, null);

        Log.e(LOG_TAG, "BatteryMode deleted with id: " + id);
    }

    public void modifyBatteryMode(BatteryModeData modeData) {
        long id = modeData.getId();

        ContentValues values = new ContentValues();
        values.put(BatterySQLiteHelper.COLUMN_NAME, modeData.getModeName());
        values.put(BatterySQLiteHelper.COLUMN_PRESET, modeData.getPreset());
        values.put(BatterySQLiteHelper.COLUMN_BRIGHTNESS, modeData.getBrightness());
        values.put(BatterySQLiteHelper.COLUMN_TIMEOUT, modeData.getTimeout());
        values.put(BatterySQLiteHelper.COLUMN_WIFI, modeData.getWiFiOn());
        values.put(BatterySQLiteHelper.COLUMN_BLUETOOTH, modeData.getBluetoothOn());
        values.put(BatterySQLiteHelper.COLUMN_MOBILEDATA, modeData.getMobileDataOn());
        values.put(BatterySQLiteHelper.COLUMN_SYNC, modeData.getSyncOn());
        values.put(BatterySQLiteHelper.COLUMN_RADIO, modeData.getRadioOn());
        values.put(BatterySQLiteHelper.COLUMN_HAPTIC, modeData.getHapticOn());
        values.put(BatterySQLiteHelper.COLUMN_VIBRATION, modeData.getVibrationOn());
        values.put(BatterySQLiteHelper.COLUMN_ROTATION, modeData.getRotationOn());

        if (database == null) {
            Log.e(LOG_TAG, "modifyBatteryMode database==null");
            return;
        }

        database.update(BatterySQLiteHelper.TABLE_MODES, values, BatterySQLiteHelper.COLUMN_ID
                + " = " + id, null);

        Log.e(LOG_TAG, "BatteryMode modify with id: " + id
                + " modeName: " + modeData.getModeName());
    }

    public List<BatteryModeData> getAllBatteryMode() {
        List<BatteryModeData> modeDatas = new ArrayList<BatteryModeData>();

        Cursor cursor = database.query(BatterySQLiteHelper.TABLE_MODES,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BatteryModeData data = cursorToBatteryModeData(cursor);
            modeDatas.add(data);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        Log.e(LOG_TAG, "getAllBatteryMode");
        if (modeDatas != null) {
            Log.e(LOG_TAG, "getAllBatteryMode num=" + modeDatas.size());
        }

        return modeDatas;
    }

    public void deleteAllBatteryMode() {
        if (database == null) {
            Log.e(LOG_TAG, "deleteAllBatteryMode database==null");
            return;
        }

        database.delete(BatterySQLiteHelper.TABLE_MODES, null, null);

        Log.e(LOG_TAG, "deleteAllBatteryMode");
    }

    public List<BatteryModeData> getAllPresetMode() {
        List<BatteryModeData> modeDatas = new ArrayList<BatteryModeData>();

        if (database == null) {
            Log.e(LOG_TAG, "getAllPresetMode database==null");
            return modeDatas;
        }

        Cursor cursor = database.query(BatterySQLiteHelper.TABLE_MODES,
                allColumns, BatterySQLiteHelper.COLUMN_PRESET + " = 1",
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BatteryModeData data = cursorToBatteryModeData(cursor);
            modeDatas.add(data);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        Log.e(LOG_TAG, "getAllPresetMode num=" + modeDatas.size());

        return modeDatas;
    }

    private BatteryModeData cursorToBatteryModeData(Cursor cursor) {
        if (cursor.isAfterLast()) {
            Log.e(LOG_TAG, "cursorToBatteryModeData end return null");
            return null;
        }

        BatteryModeData modeData = new BatteryModeData();
        modeData.setId(cursor.getLong(BatterySQLiteHelper.COLUMN_ID_INDEX));
        modeData.setModeName(cursor.getString(BatterySQLiteHelper.COLUMN_NAME_INDEX));
        modeData.setPreset(cursor.getInt(BatterySQLiteHelper.COLUMN_PRESET_INDEX) == 1);
        modeData.setBrightness(cursor.getInt(BatterySQLiteHelper.COLUMN_BRIGHTNESS_INDEX));
        modeData.setTimeout(cursor.getInt(BatterySQLiteHelper.COLUMN_TIMEOUT_INDEX));
        modeData.setWiFiOn(cursor.getInt(BatterySQLiteHelper.COLUMN_WIFI_INDEX) == 1);
        modeData.setBluetoothOn(cursor.getInt(BatterySQLiteHelper.COLUMN_BLUETOOTH_INDEX) == 1);
        modeData.setMobileDataOn(cursor.getInt(BatterySQLiteHelper.COLUMN_MOBILEDATA_INDEX) == 1);
        modeData.setSyncOn(cursor.getInt(BatterySQLiteHelper.COLUMN_SYNC_INDEX) == 1);
        modeData.setRadioOn(cursor.getInt(BatterySQLiteHelper.COLUMN_RADIO_INDEX) == 1);
        modeData.setHapticOn(cursor.getInt(BatterySQLiteHelper.COLUMN_HAPTIC_INDEX) == 1);
        modeData.setVibrationOn(cursor.getInt(BatterySQLiteHelper.COLUMN_VIBRATION_INDEX) == 1);
        modeData.setRotationOn(cursor.getInt(BatterySQLiteHelper.COLUMN_ROTATION_INDEX) == 1);
        return modeData;
    }
}
