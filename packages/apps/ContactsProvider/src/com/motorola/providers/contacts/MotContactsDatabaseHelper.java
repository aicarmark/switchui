package com.motorola.providers.contacts;

import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;
import android.database.DatabaseUtils;
import com.android.providers.contacts.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import com.android.providers.contacts.ContactsDatabaseHelper;
import com.motorola.providers.contacts.smartdialer.SmartDialerTokenUtils;

public class MotContactsDatabaseHelper {
    private static final String TAG = "MotContactsDatabaseHelper";
    private final Context mContext;

    /**
     * MOTO Contacts DB version ranges:
     * <pre>
     *   0-99        before ics
     *   100-199     ics
     *      101      add back is_restricted fields to contacts and raw_contacts, create restricted view accordingly.101 shall not be invoked in official release as we revert the change of restricted contact removal in ContactsDatabaseHelper.
     * </pre>
     */
    private static final int MOTO_DB_VERSION = 103;//moto db version on ics starts from 100

    public interface Tables {
        //mot db meta infor
        public static final String MOTO_META_INFO = "moto_meta_info";
        //smartdialer feature: smartdialer table name
        public static final String MOTO_EXTENSION_LOOKUP = "moto_extension_lookup";
    }

    //smartdialer feature: smartdialer table columens
    /* Column items in moto_extension_lookup table
     * This table has 16 columns:
     * Column 1 is raw_contactid
     * Column 2 is data1 (Integer type)
     * Column 3 - 16 is data2 - data15 (Text type)
     *
     * Now this table is used to store smartdialer name tokens, and only
     * Column 1 (RAW_CONTACT_ID), Column 2 (DATA1) and Column 3 (DATA2) are used
     *
     * This table could be used to support other features in future.
     */
    public interface MotoExtensionLookupColumns {
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String DATA1 = "data1";
        public static final String DATA2 = "data2";
        public static final String DATA3 = "data3";
        public static final String DATA4 = "data4";
        public static final String DATA5 = "data5";
        public static final String DATA6 = "data6";
        public static final String DATA7 = "data7";
        public static final String DATA8 = "data8";
        public static final String DATA9 = "data9";
        public static final String DATA10 = "data10";
        public static final String DATA11 = "data11";
        public static final String DATA12 = "data12";
        public static final String DATA13 = "data13";
        public static final String DATA14 = "data14";
        public static final String DATA15 = "data15";

        // Re-define for smartdialer using
        public static final String IS_ACRONYM = DATA1;
        public static final String TOKEN = DATA2;
    }

    //calling CNAP and CV feature columns
    public interface MotoCNAPandCVColumns {
        public static final String CNAP_NAME = "cnap_name";
        public static final String CACHED_CONVERT_NUMBER = "cached_convert_number";
    }

    //smartdialer feature: SQLiteStatement
    private SQLiteStatement mSmartDialerTokenInsert;
    private SQLiteStatement mSmartDialerTokenDelete;

    public MotContactsDatabaseHelper(Context context){
        mContext = context;
        mSmartDialerTokenInsert = null;
        mSmartDialerTokenDelete = null;
    }

    //implement this to initialize  moto extension database
    public void onCreate(Context context, SQLiteDatabase db) {
        //create moto extension tables and index here:
        // 1.create moto meta table
        createMotoMetaTable(db, MOTO_DB_VERSION);

        // 2.create table and indexes for smartdialer feature
        createMotoExtensionLookupTableAndIndexes(db);

        // 3.modify Calls table for moto CNAP and CV(convert number 33860) feature
        modifyCallsTableForCNAPandCV(db);

        // 4.create table and trigger for ICE feature
        createIceTablesandTrigger(db);

        // IKMAIN-40288 clean up the dead data related to contact
        // 5.create contact cleanup trigger
        createContactCleanUpTrigger(db);
    }

    //Called when the database has been opened.
    //call back from onOpen() of the database helper of hosted provider
    //just AFTER hosted provider invoke its onOpen
    public void onOpen(ContactsDatabaseHelper delegatedDbHelper, Context context, SQLiteDatabase db) {
        int version = (int) DatabaseUtils.longForQuery(db,
                "select version from " + Tables.MOTO_META_INFO, null);
        /* We should not exceed value of MAXINT for version info */

        if (version < MOTO_DB_VERSION) {
            //For moto internal upgrade
            upgradeMotExtensionTables(delegatedDbHelper, db, version);

            // Save the new version #.
            ContentValues cv = new ContentValues();
            cv.put( "version", MOTO_DB_VERSION );
            db.update( Tables.MOTO_META_INFO, cv, null, null );
        } else if (version > MOTO_DB_VERSION) {
            Log.i(TAG, "Cannot downgrade mot ext contacts database version" +
                     " from " + Integer.valueOf(version) + " down to " +
                     Integer.valueOf(MOTO_DB_VERSION));
        }
    }

    /* Upgrade mot extension tables. */
    public void upgradeMotExtensionTables(ContactsDatabaseHelper delegatedDbHelper, SQLiteDatabase db, int oldVersion)
    {
        if(oldVersion < 101) {//add back the views and fields for restricted contact
            upgradeToMotoVersion101(delegatedDbHelper, db);
            oldVersion = 101;
        }

        if(oldVersion < 102) {//for date format change
            upgradeToMotoVersion102(db);
            oldVersion = 102;
        }

        if(oldVersion < 103) {// IKMAIN-40288 clean up dead data related to contact
	     upgradeToMotoVersion103(db);
	     oldVersion = 103;
	 }		
		
        /*****************************
          //use 'less' operator instead in subsequent upgrade
          else if (oldVersion<103) {
            ... ...
          }
        ******************************/

        if (oldVersion != MOTO_DB_VERSION) {//must update to MOTO_DB_VERSION for each upgrade
            throw new IllegalStateException(
                    "error upgrading the mot ext database to version " + MOTO_DB_VERSION);
        }
    }

    //Implement this to upgrade extensions database
    //call back from onUpgrade() of the database helper of hosted provider
    //just after hosted provider upgraded its database
    //add moto upgrade changes in onUpgrade() when google provider upgrade
    public void onUpgrade(ContactsDatabaseHelper delegatedDbHelper, Context context, SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 600 && newVersion >= 600){
            Log.i(TAG, "Upgrade to ICS from google version " + oldVersion + "to version " + newVersion);

            //BEGIN MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716
            //Some old devices don't have db changes for SmartDialer feature and ICE feature, like Edision(IKHSS6UPGR-1716). create it back here
            createMotoExtensionLookupTableAndIndexes(db);
            createIceTablesandTrigger(db);
            //END MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716

            // rename table blur_meta_info to moto_meta_info when upgrade to ICS
            db.execSQL("ALTER TABLE blur_meta_info RENAME TO " + Tables.MOTO_META_INFO + ";");
        }else{
            Log.i(TAG, "Upgrade from google version " + oldVersion + "to version " + newVersion);
        }

        int motoOldVersion = (int) DatabaseUtils.longForQuery(db,
                "select version from " + Tables.MOTO_META_INFO, null);

        if(motoOldVersion < 90){//motoversion 90 which starts moto ICS upgrade
            upgradeToMotoVersion90(db);
            motoOldVersion = 90;
        }

        if(motoOldVersion < 91){//motoversion 91 which cleans up useless views
            upgradeToMotoVersion91(db);
            motoOldVersion = 91;
        }

        if(motoOldVersion < 92){//motoversion 92 which cleans up useless trigger
            upgradeToMotoVersion92(db);
            motoOldVersion = 92;
        }

        if(motoOldVersion < 93){//motoversion 92 which cleans up useless tables
            upgradeToMotoVersion93(db);
            motoOldVersion = 93;
        }

        if(motoOldVersion < 94){//motoversion 92 which cleans up useless tables
            upgradeToMotoVersion94(db);
            motoOldVersion = 94;
        }

        if(motoOldVersion < 101){//upgrade for restricted contact
            upgradeToMotoVersion101(delegatedDbHelper, db);
            motoOldVersion = 101;
        }

        if(motoOldVersion < 102){//upgrade for date formate change
            upgradeToMotoVersion102(db);
            motoOldVersion = 102;
        }

        if(motoOldVersion < 103){// IKMAIN-40288 clean up dead data related to contact
	     upgradeToMotoVersion103(db);
	     motoOldVersion = 103;
	 }
	
        if (motoOldVersion != MOTO_DB_VERSION) {//must update to MOTO_DB_VERSION for each upgrade
            throw new IllegalStateException(
                    "error upgrading the database to version " + MOTO_DB_VERSION);
        }

        // Save the new version #.
        ContentValues cv = new ContentValues();
        cv.put( "version", MOTO_DB_VERSION );
        db.update( Tables.MOTO_META_INFO, cv, null, null );


    }

    /**
     * back up db file
     */
    private void backUpDB(SQLiteDatabase db, int newVersion) {
        try {
            String dbFilePath = db.getPath();
            File dbFile = new File(dbFilePath);

            String backUpFilePath = dbFilePath + ".bakForVer" + newVersion;
            int byteread = 0;

            if (dbFile.exists() && (dbFile.getUsableSpace() > dbFile.length())) {
                // if destination file exists, delete it
                File backUpFile = new File(backUpFilePath);
                if (backUpFile.exists()) {
                    backUpFile.delete();
                }

                InputStream inStream = new FileInputStream(dbFile);
                FileOutputStream fs = new FileOutputStream(backUpFilePath);
                byte[] buffer = new byte[1024];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "backup db file exception" + e);
        }
    }

    private static void createMotoMetaTable(SQLiteDatabase db, int version) {
        db.execSQL("CREATE TABLE " + Tables.MOTO_META_INFO + " (" +
                "version INTEGER" + ");");
        ContentValues values = new ContentValues();
        values.put("version", version);
        db.insert(Tables.MOTO_META_INFO, "version", values);

    }

    //smartdialer feature: smartdialer table columens
    private static void createMotoExtensionLookupTableAndIndexes(SQLiteDatabase db) {
        //BEGIN MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716
        // Added "IF NOT EXISTS" for existing "CREATE TABLE" statement
        // Create smartdialer token table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.MOTO_EXTENSION_LOOKUP + " (" +
                MotoExtensionLookupColumns.RAW_CONTACT_ID + " INTEGER REFERENCES " + ContactsDatabaseHelper.Tables.RAW_CONTACTS + "(" + RawContacts._ID + ") NOT NULL," +
                MotoExtensionLookupColumns.DATA1 + " INTEGER, " +
                MotoExtensionLookupColumns.DATA2 + " TEXT, " +
                MotoExtensionLookupColumns.DATA3 + " TEXT, " +
                MotoExtensionLookupColumns.DATA4 + " TEXT, " +
                MotoExtensionLookupColumns.DATA5 + " TEXT, " +
                MotoExtensionLookupColumns.DATA6 + " TEXT, " +
                MotoExtensionLookupColumns.DATA7 + " TEXT, " +
                MotoExtensionLookupColumns.DATA8 + " TEXT, " +
                MotoExtensionLookupColumns.DATA9 + " TEXT, " +
                MotoExtensionLookupColumns.DATA10 + " TEXT, " +
                MotoExtensionLookupColumns.DATA11 + " TEXT, " +
                MotoExtensionLookupColumns.DATA12 + " TEXT, " +
                MotoExtensionLookupColumns.DATA13 + " TEXT, " +
                MotoExtensionLookupColumns.DATA14 + " TEXT, " +
                MotoExtensionLookupColumns.DATA15 + " TEXT " +
        ");");
        //END MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716

        // Create index for moto_extension_lookup table
        db.execSQL("DROP INDEX IF EXISTS moto_extension_lookup_raw_contact_id_index");
        db.execSQL("CREATE INDEX moto_extension_lookup_raw_contact_id_index ON " + Tables.MOTO_EXTENSION_LOOKUP + " (" +
                MotoExtensionLookupColumns.RAW_CONTACT_ID + ", " +
                MotoExtensionLookupColumns.DATA2 + "," +
                MotoExtensionLookupColumns.DATA1 +
        ");");

    }

    //smartdialer feature
    public void insertNameLookupTokenForSmartDialer(SQLiteDatabase db, long rawContactId, String displayName, ContentValues values) {
        Log.d(TAG, "Enter insertNameLookupTokenForSmartDialer(): rawContactId = " + rawContactId + ", displayName = " + displayName);

        if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(displayName.trim())) {
            Log.i(TAG, "displayName is null or empty. No need to insert smartdialer tokens.");
            return;
        }

        SmartDialerTokenUtils sInstance = SmartDialerTokenUtils.getSmartDialerInstance(mContext, displayName, values);
            if (null != sInstance) {
                Iterator<Map.Entry<String,Integer>> it = sInstance.getSmartDialerToken(displayName);
                if (it != null) {
                    while (it.hasNext()) {
                        Map.Entry mEntry = it.next();
                        insertSmartDialerLookupToken(db, rawContactId, (Integer)mEntry.getValue(), (String)mEntry.getKey());
                    }
                }
            } else {
                Log.e(TAG, "insertNameLookupTokenForSmartDialer(): Failed to get smartdialer instance to generate tokens for rawContactId=["
                        + rawContactId + "], displayName=[" + displayName + "]");
            }

    }

    //smartdialer feature
    private void insertSmartDialerLookupToken(SQLiteDatabase db, long rawContactId, int is_acronym, String token) {
        Log.d(TAG, "Enter insertSmartDialerLookupToken(): rawId = [" + rawContactId + "], is_acronym = [" +
                       is_acronym + "]" + ", token = [" + token + "]");
        if(mSmartDialerTokenInsert == null){
            mSmartDialerTokenInsert = db.compileStatement("INSERT OR IGNORE INTO " + Tables.MOTO_EXTENSION_LOOKUP + "("
                    + MotoExtensionLookupColumns.RAW_CONTACT_ID + "," + MotoExtensionLookupColumns.IS_ACRONYM + ","
                    + MotoExtensionLookupColumns.TOKEN
                    + ") VALUES (?,?,?)");
        }
        mSmartDialerTokenInsert.bindLong(1, rawContactId);
        mSmartDialerTokenInsert.bindLong(2, is_acronym);
        bindString(mSmartDialerTokenInsert, 3, token);
        mSmartDialerTokenInsert.executeInsert();
    }

    //smartdialer feature
    private void bindString(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    //smartdialer feature
    public void deleteNameLookupTokenForSmartDialer(SQLiteDatabase db, long rawContactId) {
        Log.d(TAG, "Enter deleteNameLookupTokenForSmartDialer(): rawId = [" + rawContactId + "]");
        if(mSmartDialerTokenDelete == null){
            mSmartDialerTokenDelete = db.compileStatement("DELETE FROM " + Tables.MOTO_EXTENSION_LOOKUP + " WHERE "
                    + MotoExtensionLookupColumns.RAW_CONTACT_ID + "=?");
        }
        mSmartDialerTokenDelete.bindLong(1, rawContactId);
        mSmartDialerTokenDelete.execute();
    }

    //calling CNAP and CV feature: add cnap_name and cached_convert_number columns
    private static void modifyCallsTableForCNAPandCV(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD cnap_name TEXT;");
        db.execSQL("ALTER TABLE calls ADD cached_convert_number TEXT;");
    }

    //29967 ICE feature
    private static void createIceTablesandTrigger(SQLiteDatabase db) {
        //BEGIN MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716
        // Added "IF NOT EXISTS" for existing "CREATE TABLE" statements
        db.execSQL("CREATE TABLE IF NOT EXISTS " + "ice_contacts (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "no INTEGER NOT NULL,"+
                "contacts INTEGER REFERENCES contacts(_id),"+
                "type INTEGER DEFAULT(1));" );

        db.execSQL("create TABLE IF NOT EXISTS " + "ice_notes (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "no INTEGER NOT NULL,"+
                "note TEXT,"+
                "type INTEGER DEFAULT(-1));" );

        /* IKMAIN-40288
        db.execSQL("CREATE TRIGGER IF NOT EXISTS ice_cleanup DELETE ON contacts"
                + " BEGIN "
                + "   DELETE FROM ice_contacts WHERE contacts = OLD._id;"
                + " END"); */
        //END MOTOROLA, bxmk76, 03/14/2012 IKHSS6UPGR-1716
    }

    //IKMAIN-40288 clean up the dead data related to contact
    private static void createContactCleanUpTrigger(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS ice_cleanup");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS contacts_deleted DELETE ON contacts"
                + " BEGIN "
                + "   DELETE FROM ice_contacts WHERE contacts = OLD._id;"
                + "   DELETE FROM "+ ContactsDatabaseHelper.Tables.SEARCH_INDEX
                + "   WHERE contact_id = OLD._id;"
                + " END");	 
    }

    // IKMAIN-40288 clean up dead data related to contact
    private void upgradeToMotoVersion103(SQLiteDatabase db) {
        createContactCleanUpTrigger(db);
    }
			
    public void upgradeToMotoVersion102(SQLiteDatabase db) {
        try {
            db.execSQL(
                 "UPDATE " + ContactsDatabaseHelper.Tables.DATA +
                 " SET " + Data.DATA1 +
                         " = (select substr(" + Data.DATA1 + ",-4,4)) ||'-'||" +
                         "(SELECT CASE WHEN CAST(SUBSTR(" + Data.DATA1 + ",1,2) AS INTEGER) >= 10 " +
                         "THEN SUBSTR(" + Data.DATA1 + ",1,2) " +
                         "ELSE '0'|| CAST(SUBSTR (" + Data.DATA1 + ",1,2) AS INTEGER) END ) ||'-'|| " +
                         "(SELECT CASE WHEN CAST(SUBSTR(" + Data.DATA1 + ",-7,3) AS INTEGER)>=10 " +
                         "THEN SUBSTR(" + Data.DATA1 + ",-7,2) " +
                         "ELSE '0'|| CAST(SUBSTR(" + Data.DATA1 + ",-6,2) AS INTEGER) END)" +
                         " WHERE " + "raw_contact_id IN " +
                         "(SELECT _id FROM raw_contacts" +
                         " WHERE account_type = 'com.motorola.android.buacontactadapter')" +
                         " AND data.mimetype_id = (SELECT _id FROM mimetypes WHERE "+ "mimetypes.mimetype = '" + Event.CONTENT_ITEM_TYPE + "')" +
                         " AND " + Data.DATA1 + " like '%/%/%'"
            );
        } catch(Exception e) {
                Log.i(TAG, "fail to upgradeToMotoVersion102: " + e.toString());
        }
    }

    //add back the views and fields for restricted contact
    public void upgradeToMotoVersion101(ContactsDatabaseHelper delegatedDbHelper, SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + ContactsDatabaseHelper.Tables.RAW_CONTACTS + " ADD COLUMN " +
                Constants.RawContacts_IS_RESTRICTED + " INTEGER DEFAULT 0");
        } catch(Exception e) {
              //the fields/index might be there already
              //we simply ignore all exception cause official release never invoke this method
                Log.i(TAG, "failed to alter table raw_contacts for restricted contact: " + e.toString());
        }

        try {
            db.execSQL("ALTER TABLE " + ContactsDatabaseHelper.Tables.CONTACTS + " ADD COLUMN " +
                ContactsDatabaseHelper.ContactsColumns.SINGLE_IS_RESTRICTED + " INTEGER NOT NULL DEFAULT 0");

            db.execSQL("CREATE INDEX contacts_restricted_index ON " + ContactsDatabaseHelper.Tables.CONTACTS + " (" +
                ContactsDatabaseHelper.ContactsColumns.SINGLE_IS_RESTRICTED +
            ");");
        } catch(Exception e) {
              //the fields/index might be there already
              //we simply ignore all exception cause official release never invoke this method
                Log.i(TAG, "failed to alter table contacts for restricted contact: " + e.toString());
        }

        delegatedDbHelper.createContactsViews(db);
    }

    private void upgradeToMotoVersion90(SQLiteDatabase db) {
        int motoOldVersion = (int) DatabaseUtils.longForQuery(db,
                "select version from " + Tables.MOTO_META_INFO, null);

        if(motoOldVersion != 8){//version 8 is stable6 version. Now we only consider the case which upgrades from stable6 to ICS
            Log.e(TAG, "Not Upgrade from stable6 to ICS! Old Moto version:" + motoOldVersion);
        }else{
            Log.i(TAG, "Upgrade to ICS from Moto version " + motoOldVersion);
        }

        // 1. backup db firstly when upgrade from stable6
        backUpDB(db, 90);

        // 2. modify Calls table for moto CNAP and CV(convert number 33860) feature and drop table blur_calls_name_type
        if(motoOldVersion >= 7){ // only version after 7 has calling CNAP and CV feature
            // (1).Add two columns CNAP and CV
            modifyCallsTableForCNAPandCV(db);
            // (2).copy data in table blur_calls_name_type to table calls
            db.execSQL("UPDATE calls SET "
                + "cnap_name=(SELECT blur_calls_name_type.cnap_name FROM blur_calls_name_type WHERE blur_calls_name_type._id1=calls._id), "
                + "cached_convert_number=(SELECT blur_calls_name_type.cached_convert_number FROM blur_calls_name_type WHERE blur_calls_name_type._id1=calls._id)"
                + ";");
            // (3).delete table blur_calls_name_type
            db.execSQL("DROP TABLE IF EXISTS blur_calls_name_type;");
        }
    }

    private void upgradeToMotoVersion91(SQLiteDatabase db) {
        //clean up useless views.
        db.execSQL("DROP VIEW IF EXISTS blur_dirty_blur_services_view;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_communication_history;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_communication_history_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_owner_social_network_status;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_owner_social_network_status_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_social_network_status;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_social_network_status_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_photo_data;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_photo_data_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_data;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_data_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_raw_contacts;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_raw_contacts_restricted;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_contacts;");
        db.execSQL("DROP VIEW IF EXISTS blur_view_contacts_restricted;");
    }

    private void upgradeToMotoVersion92(SQLiteDatabase db) {
        //clean up useless triggers
        db.execSQL("DROP TRIGGER IF EXISTS Contacts_delete_2_VisibilityOverriddenContacts;");//for blur hide/show feature
        db.execSQL("DROP TRIGGER IF EXISTS RawContacts_delete_2_BlockedRawContacts;");//for blur sn block feature
        db.execSQL("DROP TRIGGER IF EXISTS calls_name_type_cleanup;");//for moto calling  CNAP and CV feature

    }

    private void upgradeToMotoVersion93(SQLiteDatabase db) {
        //clean up useless tables
        db.execSQL("DROP TABLE IF EXISTS blur_group_fixup;");//for blur sync
        db.execSQL("DROP TABLE IF EXISTS blur_identity_fixup;");//for blur sync

        db.execSQL("DROP TABLE IF EXISTS blocked_rawcontacts;");//no need blur block feature
        db.execSQL("DROP TABLE IF EXISTS blur_sources;");//no need blur source table
        db.execSQL("DROP TABLE IF EXISTS VisibilityOverridden_Contacts;");//no need blur hide/show feature
    }

    private void upgradeToMotoVersion94(SQLiteDatabase db) {
        GroupBota.SingleInstance().run(db);
    }
}
