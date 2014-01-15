/*
 * @(#)DBHelper.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/11/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/**
 * Helper class for all DB related operations. <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class DBHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME   = "drivemode.db";
    private final static int    DATABSE_VERSION = 1;
    private static final String TAG             = DBHelper.class.getSimpleName();
    private static DBHelper sDBHelper;


    /**
     * Constructor
     *
     * @param context context
     */
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABSE_VERSION);
        if (Constants.LOG_DEBUG) Log.d(TAG, this.getDatabaseName());
    }

    public static DBHelper getInstance(Context context) {
        if(sDBHelper == null)
            sDBHelper = new DBHelper(context);
        return sDBHelper;
    }

    /**
     * onCreate()
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RuleTable.CREATE_SCRIPT);
    }

    /**
     * onUpgrade() - version 1.0. No upgrade needed now.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
