package com.motorola.mmsp.activitygraph;

import android.net.Uri;
import android.provider.BaseColumns;

public class AppRank {
    static final String AUTHORITY = "com.motorola.appgraphy";
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    static final Uri EXECSQL = Uri.parse(CONTENT_URI+"/execSql");
    static final Uri SET_RANK = Uri.parse(CONTENT_URI + "/set_rank");
    static final Uri SET_RANK_ALL = Uri.parse(CONTENT_URI + "/set_all_rank");
    /**
     * Describes the columns in the apps table.
     */
    public static final class Apps implements BaseColumns {
        
        public static final String TABLE_NAME = "apps";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppRank.CONTENT_URI, TABLE_NAME);
        
        /** MIME type of {@link #CONTENT_URI} providing a directory of apps. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.motorola.appgraphy.apprank.app";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single app. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.motorola.appgraphy.apprank.app";
        
        /** String: app component. */
        public static final String COMPONENT = "component";
        
        /** long: time stamp when the app was last launched. */
        public static final String TIME = "time";
        
        /** int: whether the app was downloaded. */
        public static final String DOWNLOADED = "downloaded";
        
        public static final String CREATE_STATEMENT =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COMPONENT + " TEXT,"
                        + TIME + " INTEGER,"
                        + DOWNLOADED + " INTEGER);";
        
        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_COMPONENT = 1;
        public static final int INDEX_TIME = 2;
        public static final int INDEX_DOWNLOADED = 3;
        public static final String [] PROJECTION = {
            _ID, COMPONENT, TIME, DOWNLOADED,
        };
    }
    
    /**
     * Describes the columns in the ranks table.
     */
    public static final class Ranks implements BaseColumns {
        
        public static final String TABLE_NAME = "ranks";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppRank.CONTENT_URI, TABLE_NAME);
        
        /** MIME type of {@link #CONTENT_URI} providing a directory of rank. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.motorola.appgraphy.apprank.rank";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single rank. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.motorola.appgraphy.apprank.rank";
        
        /** String: app id */
        public static final String APPName="appName";
        
        /** int: hidden or not; 0--false,  1--true */
        public static final String HIDDEN = "hidden";
        
        /** int: Day pass */
        public static final String DAY = "day";
        
        /** int: Current rank */
        public static final String CURRENT_RANK = "current_rank";
        
        /** int: Current count */
        public static final String CURRENT_COUNT = "current_count";
        
        public static final String CREATE_STATEMENT =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + "_id INTEGER PRIMARY KEY,"
                        + APPName + " TEXT,"
                        + HIDDEN + " INTEGER DEFAULT 0,"
                        + DAY + " INTEGER DEFAULT 0,"
                        + CURRENT_COUNT + " INTEGER NOT NULL DEFAULT 0,"
                        + CURRENT_RANK + " INTEGER NOT NULL DEFAULT 0 "
                        + ");";
        
        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_APPName = 1;
        public static final int INDEX_HIDDEN = 2;
        public static final int INDEX_DAY = 3;
        public static final int INDEX_CURRENT_COUNT = 4;
        public static final int INDEX_CURRENT_RANK = 5;
        public static final String [] PROJECTION = {
            _ID, APPName, HIDDEN, CURRENT_COUNT, CURRENT_RANK, DAY,
        };
    }
    
    /**
     * Describes the columns in the manual list table.
     */
    public static final class ManualList implements BaseColumns {
        
        public static final String TABLE_NAME = "manual_config";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppRank.CONTENT_URI, TABLE_NAME);
        
        /** MIME type of {@link #CONTENT_URI} providing a directory of manual lists. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.motorola.appgraphy.apprank.manuallist";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single manual list. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.motorola.appgraphy.apprank.manuallist";
        
        /** int: manual activity index */
        public static final String MANUALINDEX = "manual_index";
        
        /** long: app component name. */
        public static final String APPNAME = "apps_name";
        
        public static final String CREATE_STATEMENT =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + "_id INTEGER PRIMARY KEY,"
                        + MANUALINDEX + " INTEGER,"
                        + APPNAME + " TEXT);";
        
        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_MANUALINDEX = 1;
        public static final int INDEX_APPNAME = 2;
        public static final String [] PROJECTION = {
            _ID, MANUALINDEX, APPNAME,
        };
        
    }
}
