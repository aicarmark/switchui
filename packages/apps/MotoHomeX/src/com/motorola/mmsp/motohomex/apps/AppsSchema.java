/*
 * Copyright (C) 2010 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Describes the database schema of the apps database.
 */
public class AppsSchema {

    static final String AUTHORITY = "com.motorola.mmsp.motohomex.apps";
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Describes the columns in the apps table.
     */
    public static final class Apps implements BaseColumns {

        public static final String TABLE_NAME = "apps";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);

        /** MIME type of {@link #CONTENT_URI} providing a directory of app groups. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.blur.apps.app";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single app group. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.blur.apps.app";

        /** String: app component. */
        public static final String COMPONENT = "component";

        /** int: number of times the app has been launched. */
        public static final String COUNT = "count";

        /** long: time stamp when the app was last launched. */
        public static final String TIME = "time";

        /** int: whether the app was downloaded. */
        public static final String DOWNLOADED = "downloaded";

        /** int: whether the app is hidden */
        public static final String HIDDEN = "hidden";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COMPONENT + " TEXT, "
            + COUNT + " INTEGER, "
            + TIME + " INTEGER, "
            + HIDDEN + " INTEGER, "
            + DOWNLOADED + " INTEGER);";

        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_COMPONENT = 1;
        public static final int INDEX_COUNT = 2;
        public static final int INDEX_TIME = 3;
        public static final int INDEX_DOWNLOADED = 4;
        public static final int INDEX_HIDDEN = 5;
        public static final String [] PROJECTION = {
            _ID, COMPONENT, COUNT, TIME, DOWNLOADED, HIDDEN
        };
    }

    /**
     * Describes the columns in the groups table.
     */
    public static final class Groups implements BaseColumns {

        public static final String TABLE_NAME = "groups";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);

        /** MIME type of {@link #CONTENT_URI} providing a directory of app groups. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.blur.apps.group";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single app group. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.blur.apps.group";

        /** Maximum allowed number of groups. */
        public static final int MAX = 100;

        /** String: app group name. */
        public static final String NAME = "name";

        /** int: type of group (see TYPE_* constants). */
        public static final String TYPE = "type";

        /** int: sort index (see SORT_* constants). */
        public static final String SORT = "sort";

        /** int: icon set index (see ICON_SET_* constants). */
        public static final String ICON_SET = "icon_set";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NAME + " TEXT, "
            + TYPE + " INTEGER, "
            + SORT + " INTEGER, "
            + ICON_SET + " INTEGER);";

        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_NAME = 1;
        public static final int INDEX_TYPE = 2;
        public static final int INDEX_SORT = 3;
        public static final int INDEX_ICON_SET = 4;
        public static final String [] PROJECTION = {
            _ID, NAME, TYPE, SORT, ICON_SET,
        };

        /*
         * NOTE: the value of the TYPE_* constants below is significant.
         * It is used as the first comparator for groups, thus putting special
         * groups at the top of the group picker list.
         */
        /** App group type for All Applications. */
        static final int TYPE_ALL_APPS = 0;
        /** App group type for Downloaded. */
        static final int TYPE_DOWNLOADED = 1;
        /** App group type for Frequent. */
        public static final int TYPE_FREQUENTS = 2;

        /** App group type for user group. */
        static final int TYPE_USER = 100;
        /** App group type when user is entering new group name. */
        static final int TYPE_NEW = 101;
        /**add for IKDOMINO-6717 by bphx43 2012-03-21*/
        static final int TYPE_CBS = 200;
	/**end by bphx43*/
        /** Number of "special" group types to ignore when adding app to group. */
        static final int TYPE_NUM_SPECIAL = 3;

        /** Indicates group is sorted alphabetically. */
        static final int SORT_ALPHA = 0;
        /** Indicates group is sorted by most frequently used. */
        static final int SORT_FREQUENTS = 1;
        /** Indicates groups is sorted by predefined order */
        static final int SORT_CARRIER = 2;

        /** Names of sort options in persisted settings, index by constants. */
        static final String [] SORT_NAMES = {
            "alpha", "frequents", "carrier" // NO TRANSLATE
        };

        /** Icon set for All Apps group. */
        static final int ICON_SET_ALL_APPS = 0;
        /** Icon set for Downloaded group. */
        static final int ICON_SET_DOWNLOADED = 1;
        /** Icon set for Frequents group. */
        static final int ICON_SET_FREQUENTS = 2;
        /** Icon set for generic user groups. */
        static final int ICON_SET_USER = 3;

        /** First icon set index that user may select for new folder. */
        static final int ICON_SET_FIRST_USER = ICON_SET_USER;

    }

    /**
     * Describes the columns in the members table.
     */
    public static final class Members implements BaseColumns {

        public static final String TABLE_NAME = "members";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);

        /** MIME type of {@link #CONTENT_URI} providing a directory of app groups. */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.blur.apps.member";
        /** MIME type of a {@link #CONTENT_URI} subdirectory of a single app group. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.blur.apps.member";

        /** long: app id. */
        public static final String APP = "apps_id";

        /** long: app group id. */
        public static final String GROUP = "groups_id";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + APP + " INTEGER REFERENCES " + Apps.TABLE_NAME + "(_id), "
            + GROUP + " INTEGER REFERENCES " + Groups.TABLE_NAME + "(_id));";

        // Projection map to get all columns
        public static final int INDEX_ID = 0;
        public static final int INDEX_APP = 1;
        public static final int INDEX_GROUP = 2;
        public static final String [] PROJECTION = {
            _ID, APP, GROUP,
        };
    }

    /**
     * Describes the columns in the folders table.
     */
    public static final class Folders implements BaseColumns {

        public static final String TABLE_NAME = "folders";
        public static final String ALL = "all";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);
        public static final Uri CONTEN_URI_ALL = Uri.withAppendedPath(CONTENT_URI, ALL);

        /** String: folder name. */
        public static final String NAME = "name";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NAME + " TEXT);";

        public static final String [] PROJECTION_ALL = {
            Folders.TABLE_NAME+"."+BaseColumns._ID,
            Folders.TABLE_NAME+"."+Folders.NAME,
            AppsToFolder.TABLE_NAME+"."+AppsToFolder.COMPONENT
        };

        public static final String SELECTION_ALL = 
            Folders.TABLE_NAME+"."+BaseColumns._ID+"="+Relationship.TABLE_NAME+"."+Relationship.FOLDER+" AND "+
            AppsToFolder.TABLE_NAME+"."+BaseColumns._ID+"="+Relationship.TABLE_NAME+"."+Relationship.APP;

        // Projection map to get all
        public static final int INDEX_ALL_FOLDER_ID = 0;
        public static final int INDEX_ALL_FOLDER_NAME = 1;
        public static final int INDEX_ALL_APP_COMPONENT = 2;
}

    /**
     * Describes the columns in the appstofolder table.
     */
    public static final class AppsToFolder implements BaseColumns {

        public static final String TABLE_NAME = "appstofolder";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);

        /** String: app component. */
        public static final String COMPONENT = "component";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COMPONENT + " TEXT);";
    }

    /**
     * Describes the columns in the relationship table.
     */
    public static final class Relationship implements BaseColumns {

        public static final String TABLE_NAME = "relationship";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AppsSchema.CONTENT_URI, TABLE_NAME);

        /** long: app id. */
        public static final String APP = "app_id";

        /** long: folder id. */
        public static final String FOLDER = "folder_id";

        public static final String CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + APP + " INTEGER REFERENCES " + Apps.TABLE_NAME + "(_id), "
            + FOLDER + " INTEGER REFERENCES " + Folders.TABLE_NAME + "(_id));";
    }

}
