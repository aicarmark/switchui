/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor.dbhelper;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.contextual.virtualsensor.locationsensor.Constants;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

/** This class extends the SQLiteOpenHelper.class.
 *
 *<pre>
 * CLASS:
 * 	Every class that abstracts a table or view uses this class to open the database.
 *
 * RESPONSIBILITIES:
 * 	Opening the database
 * 	Upgrading the database
 * 	Dropping and recreating the database
 *
 *
 * COLABORATORS:
 * Database helper for Location. Designed as a singleton to make sure that all
 * {@link android.content.ContentProvider} users get the same reference.
 * Provides handy methods for maintaining package and accuracy fix.
 */
public class LocationDatabaseHelper extends SQLiteOpenHelper implements DbSyntax {

    private final static String TAG = "LSAPP_DB";

    // database
    public static final String DATABASE_NAME = Constants.DATABASE_NAME;
    //public static final int DATABASE_VERSION = 3;   //  with new columns
    //public static final int DATABASE_VERSION = 4;     // add cell sensor table
    //public static final int DATABASE_VERSION = 5;     // add poi table
    //public static final int DATABASE_VERSION = 6;     // add unique on poi in cellsensor and poi table
    //public static final int DATABASE_VERSION = 7;      // add wifimac, wificonnmac, btmac columns in poi table
    //public static final int DATABASE_VERSION = 8;      // add bestaccuracy col to loctime.
    //public static final int DATABASE_VERSION = 9;      // add mac ssid col to poi table
    //public static final int DATABASE_VERSION = 10;      // add poitype to poi table
    public static final int DATABASE_VERSION = 11;      // add wifissid to loctime table

    private static LocationDatabaseHelper sSingleton = null;

    protected SQLiteDatabase mDb = null;

    public static synchronized LocationDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new LocationDatabaseHelper(context);
        }
        return sSingleton;
    }


    /* Private constructor, callers except unit tests should obtain an instance through
    * {@link #getInstance(android.content.Context)} instead.
    */
    protected LocationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);   // this one set the current db version.
    }

    /** handles the onCreate operation - which is only called when the
     * database does not exist.
     *
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     *
     * @param db - SQLiteDatabase instance.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create tables upon creation
        try {
            db.beginTransaction();
            execSql(db, LocationDatabase.CellTable.CREATE_TABLE_SQL);
            execSql(db, LocationDatabase.LocTimeTable.CREATE_TABLE_SQL);
            execSql(db, LocationDatabase.PoiTable.CREATE_TABLE_SQL);

            db.setMaximumSize(DATABASE_SIZE);
            db.setTransactionSuccessful();
            LSAppLog.pd(TAG, "onCreate : creating tables...");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }


    /** This method performs a SQLiteDatabase.execSQL(String sql) operation.
     *
     * @param db - database instance
     * @param sql - sql code.
     */
    protected void execSql(SQLiteDatabase db, String sql) {
        Log.i(TAG, sql);
        db.execSQL(sql);
    }


    /** This method handles database upgrades from a SQL perspective. That is,
     * columns can be added to the database, columns can be initialized, etc.
     *
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     *
     * @param db - SQLiteDatabase instance being upgraded
     * @param oldVersion - version number being upgraded from
     * @param newVersion - version number being upgraded to
     *
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        LSAppLog.pd(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which may destroy old data");

        if (!tableExist(db)) {
            LSAppLog.pd(TAG, "Tables do not exist, create tables rather than upgrading");
            onCreate(db);
            return;
        }

        String updatesql = null;
        try {
            db.beginTransaction();
            if (newVersion > oldVersion) {
                switch (oldVersion) {
                case 4:
                    updatesql = ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCURACY + LONG_TYPE);
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCUNAME + TEXT_TYPE);
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.POITAG + TEXT_TYPE );
                    db.execSQL(LocationDatabase.PoiTable.CREATE_TABLE_SQL);
                    // no break here so that it can cascade up to latest newVersion
                case 5:
                    execSql(db, "DROP TABLE cellsensor;");
                    execSql(db, "DROP TABLE poi;");
                    execSql(db, LocationDatabase.PoiTable.CREATE_TABLE_SQL);
                    // no break here so that it can cascade up to latest newVersion
                case 6:
                    updatesql = ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCURACY + LONG_TYPE);
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCUNAME + TEXT_TYPE);
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.POITAG + TEXT_TYPE );
                    // no break here so that it can cascade up to latest newVersion
                case 7:
                    updatesql = ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.BESTACCURACY + LONG_TYPE);
                    LSAppLog.pd(TAG, updatesql + LocationDatabase.LocTimeTable.Columns.BESTACCURACY + LONG_TYPE);
                    // no break here so that it can cascade up to latest newVersion
                case 8:
                    updatesql = ALTER_TABLE + LocationDatabase.PoiTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.PoiTable.Columns.WIFISSID + TEXT_TYPE);
                    LSAppLog.pd(TAG, "execSQL:" + updatesql + LocationDatabase.PoiTable.Columns.WIFISSID + TEXT_TYPE);
                    // no break here so that it can cascade up to latest newVersion
                case 9:
                    updatesql = ALTER_TABLE + LocationDatabase.PoiTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.PoiTable.Columns.POITYPE + TEXT_TYPE);
                    LSAppLog.pd(TAG, "execSQL:" + updatesql + LocationDatabase.PoiTable.Columns.POITYPE + TEXT_TYPE);
                    // no break here so that it can cascade up to latest newVersion
                case 10:
                    updatesql = ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                    db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.WIFISSID + TEXT_TYPE);
                    LSAppLog.pd(TAG, updatesql + LocationDatabase.LocTimeTable.Columns.WIFISSID + TEXT_TYPE);
                    // no break here so that it can cascade up to latest newVersion
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        /*
        if (oldVersion < 4 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                // add accuracy, accuname, and poitag column
                String updatesql = ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCURACY + LONG_TYPE);
                db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.ACCUNAME + TEXT_TYPE);
                db.execSQL(updatesql + LocationDatabase.LocTimeTable.Columns.POITAG + TEXT_TYPE );

                LSAppLog.i(TAG, "onUpgrade sql :" + updatesql);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }

        } else if ( oldVersion == 4 && newVersion > oldVersion) {
            execSql(db, LocationDatabase.PoiTable.CREATE_TABLE_SQL);
        } else if (oldVersion == 5 && newVersion > oldVersion) {
            execSql(db, "DROP TABLE cellsensor;");
            execSql(db, "DROP TABLE poi;");
            execSql(db, LocationDatabase.PoiTable.CREATE_TABLE_SQL);
        } else if (oldVersion == 6 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                // add accuracy, accuname, and poitag column
                String updatesql = ALTER_TABLE + LocationDatabase.PoiTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(updatesql + LocationDatabase.PoiTable.Columns.WIFIMAC + TEXT_TYPE);
                db.execSQL(updatesql + LocationDatabase.PoiTable.Columns.WIFICONNMAC + TEXT_TYPE);
                db.execSQL(updatesql + LocationDatabase.PoiTable.Columns.BTMAC + TEXT_TYPE );

                LSAppLog.i(TAG, "onUpgrade sql :" + updatesql);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        } else if (oldVersion == 7 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                String addcol =  ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(addcol + LocationDatabase.LocTimeTable.Columns.BESTACCURACY + LONG_TYPE);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        } else if (oldVersion == 8 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                String addcol =  ALTER_TABLE + LocationDatabase.PoiTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(addcol + LocationDatabase.PoiTable.Columns.WIFISSID + TEXT_TYPE);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        } else if (oldVersion == 9 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                String addcol =  ALTER_TABLE + LocationDatabase.PoiTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(addcol + LocationDatabase.PoiTable.Columns.POITYPE + TEXT_TYPE);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        } else if (oldVersion == 10 && newVersion > oldVersion) {
            db.beginTransaction();
            try {
                String addcol =  ALTER_TABLE + LocationDatabase.LocTimeTable.TABLE_NAME + ADD_COLUMN ;
                db.execSQL(addcol + LocationDatabase.LocTimeTable.Columns.WIFISSID + TEXT_TYPE);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        */

        LSAppLog.pd(TAG, "Upgrading database with added columns version: " + oldVersion);
    }

    /**
     * Override on downgrade, default will throw exception, which is bad.
     * Drop all tables 
     */
    @Override
    public void onDowngrade(final SQLiteDatabase db, int oldVersion, final int newVersion) {
        if( oldVersion < newVersion ){
            try{
                LSAppLog.i(TAG, "onDowngrade :" + newVersion + " : " + oldVersion);
                db.execSQL("DROP TABLE IF EXISTS " + LocationDatabase.CellTable.CREATE_TABLE_SQL);
                db.execSQL("DROP TABLE IF EXISTS " + LocationDatabase.LocTimeTable.CREATE_TABLE_SQL);
                db.execSQL("DROP TABLE IF EXISTS " + LocationDatabase.PoiTable.CREATE_TABLE_SQL);
                onCreate(db);
            }catch(SQLException e){
                LSAppLog.e(TAG, "onDowngrade : " + e.toString());
                throw e;
            }
        }
    }

    /**
     * check whether tables exist or not. When upgrade from stable 5 build, db exists with version 8, but tables do not exist.
     * @param db
     * @return true if table exist, false otherwise.
     */
    private boolean tableExist(SQLiteDatabase db) {
        boolean exist = false;
        String tblexist = SELECT + " name " + FROM + " sqlite_master " + WHERE + " name= " + Q + LocationDatabase.LocTimeTable.TABLE_NAME + Q ;
        Cursor c = db.rawQuery(tblexist, null);
        try {
            if (c != null && c.moveToFirst()) {
                exist = true;
                LSAppLog.pd(TAG, "tableExist: tables exist");
            }
        } catch (Exception e) {
            LSAppLog.e(TAG, "tableExist: " + e.toString());
        } finally {
            if (c != null)
                c.close();
        }
        return exist;
    }

    /** handles the drop and recreate operation. This is never called, but
     * is here to allow for support of drop and recreate if it is necessary.
     *
     * @param db - SQLiteDatabase instance
     */
    @SuppressWarnings("unused")
    private void dropAndRecreate(SQLiteDatabase db) {
        // tables
        //db.execSQL("DROP TABLE IF EXISTS " + StateTable.TABLE_NAME);
        //db.execSQL("DROP TABLE IF EXISTS " + MimeTypeTable.TABLE_NAME);
        onCreate(db);
    }

}


