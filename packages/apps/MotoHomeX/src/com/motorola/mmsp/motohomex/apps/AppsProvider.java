/*
 * Copyright (C) 2010 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.motorola.mmsp.motohomex.apps.AppsSchema.Apps;
import com.motorola.mmsp.motohomex.apps.AppsSchema.AppsToFolder;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Folders;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Members;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Relationship;
import com.motorola.mmsp.motohomex.util.DatabaseContentProvider;
import com.motorola.mmsp.motohomex.ItemInfo;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.LauncherSettings.Favorites;


public class AppsProvider extends DatabaseContentProvider {

    //-----------------------------------------------------
    // Statics

    private static final String TAG = "Launcher";

    private static final int APPS_DB_VERSION = 3;
    private static final String APPS_DB_NAME = "apps.db";

    private static final String APPS_TABLE_NAME = "apps";

    private static final String AND = " AND ";

    private static final int ALL_APPS = 0;
    private static final int ONE_APP = 1;
    private static final int ALL_GROUPS = 2;
    private static final int ONE_GROUP = 3;
    private static final int ALL_MEMBERS = 4;
    private static final int ONE_MEMBER = 5;
    private static final int ONE_FOLDER = 6;
    private static final int ONE_APPTOFOLDER = 7;
    private static final int ONE_RELATIONSHIP = 8;
    private static final int ALL_FOLDERS = 9;

    // Packages to be updated
    private static String OLD_MSG_PKG = "com.motorola.blur.conversations/com.motorola.blur.conversations.ui.ConversationList";
    private static String NEW_MSG_PKG = "com.motorola.messaging/.activity.ConversationListActivity";
    private static String OLD_EMAIL_PKG = "com.motorola.blur.email/.mailbox.ViewFolderActivity";
    private static String NEW_EMAIL_PKG = "com.motorola.motoemail/com.android.email.activity.Welcome";
    private static String OLD_CALENDAR_PKG = "com.android.calendar/.LaunchActivity";
    private static String NEW_CALENDAR_PKG = "com.motorola.calendar/com.android.calendar.AllInOneActivity";
    private static String OLD_ALARM_PKG = "com.motorola.blur.alarmclock/.AlarmClock";
    private static String NEW_ALARM_PKG = "com.android.deskclock/.AlarmClock";
    private static String OLD_MUSIC_PKG = "com.motorola.blur.music/.DashboardActivity";
    private static String NEW_MUSIC_PKG = "com.motorola.motmusic/.DashboardActivity";
    private static String OLD_TASKS_PKG = "com.motorola.blur.tasks/.TaskListActivity";
    private static String NEW_TASKS_PKG = "com.motorola.tasks/.TaskListMain";
    private static String OLD_BOOKS_PKG = "com.google.android.apps.books/com.google.android.apps.books.app.HomeActivity";
    private static String NEW_BOOKS_PKG = "com.google.android.apps.books/.app.BooksActivity";
    private static String OLD_CONTACTS_PKG = "com.android.contacts/com.android.contacts.DialtactsContactsEntryActivity";
    private static String NEW_CONTACTS_PKG = "com.android.contacts/.activities.PeopleActivity";
    private static String OLD_MYACC_PKG = "com.motorola.blur.setup/com.motorola.blur.settings.AccountsAndServicesPreferenceActivity";
    private static String NEW_MYACC_PKG = "com.motorola.setup.outofbox/.SplashScreenActivity";
    private static String OLD_GALLERY_PKG = "com.motorola.blurgallery/com.motorola.cgallery.Dashboard";
    private static String NEW_GALLERY_PKG = "com.motorola.motgallery/com.motorola.cgallery.Dashboard";
    private static String OLD_PHONE_PKG = "com.android.contacts/com.android.contacts.DialtactsActivity";
    private static String NEW_PHONE_PKG = "com.android.contacts/.activities.DialtactsActivity";
    private static String OLD_SOCIAL_LOCATION_PKG = "com.aloqa.me.client_modules.android_sl_vzw/com.aloqa.me.client_modules.android.gui_client.overview_screen.OverviewScreen";
    private static String NEW_SOCIAL_LOCATION_PKG = "com.aloqa.me.client_modules.android_sl_vzw_phoenix/com.aloqa.me.client_modules.android.gui_client.overview_screen.OverviewScreen";
    private static String OLD_VZW_APPS = "com.gravitymobile.app.hornbill/com.gravitymobile.app.hornbill.HornbillActivity";
    private static String NEW_VZW_APPS = "com.gravitymobile.app.hornbill/com.vzw.odp.LaunchActivity";
    private static String OLD_FETION_PKG = "cn.com.fetion/.android.activities.StartActivity";
    private static String NEW_FETION_PKG = "cn.com.fetion/.android.ui.activities.StartActivity";
    private static String OLD_YOUTUBE = "com.google.android.youtube/com.google.android.youtube.app.froyo.phone.HomeActivity";
    private static String NEW_YOUTUBE = "com.google.android.youtube/.app.honeycomb.Shell$HomeActivity";
    private static String OLD_VIDEOS = "com.google.android.videos/com.google.android.youtube.videos.froyo.VideosActivity";
    private static String NEW_VIDEOS = "com.google.android.videos/com.google.android.youtube.videos.honeycomb.VideosActivity";
    private static String OLD_NUMBER_LOCATION_PKG = "com.motorola.numberlocation/.NumberLocationActivity";
    private static String NEW_NUMBER_LOCATION_PKG = "com.motorola.numberlocation/.NumberLocationFragmentTabsActivity";
    private static String OLD_CARDOCK_PKG = "com.motorola.smartcardock/.main.SmartCarDockMainActivity";
    private static String NEW_CARDOCK_PKG = "com.motorola.china.smartcardock/com.motorola.smartcardock.main.SmartCarDockMainActivity";
    private static String OLD_INTSIG_PKG = "com.intsig.BizCardReader/.MainActivity";
    private static String NEW_INTSIG_PKG = "com.intsig.BizCardReader/com.intsig.camcard.BcrMainActivity";



    /* Added by ncqp34 for fih workspace flex */
    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_MENUGROUP = "menugroup";
    private static final String GROUP_CONTAINER = "groupcontainer";
    /**add for IKDOMINO-6717 by bphx43 2012-03-21*/
    private static final String GROUP_MODEID = "mode_id";
    private ArrayList<String> tempList = new ArrayList<String>();
    private HashMap<String,Long> tempMap = new HashMap<String,Long>();
    /** end by bphx43*/
    /*ended by ncqp34*/


    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Apps.TABLE_NAME, ALL_APPS);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Apps.TABLE_NAME + "/#", ONE_APP);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Groups.TABLE_NAME, ALL_GROUPS);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Groups.TABLE_NAME + "/#", ONE_GROUP);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Members.TABLE_NAME, ALL_MEMBERS);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Members.TABLE_NAME + "/#", ONE_MEMBER);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Folders.TABLE_NAME , ONE_FOLDER);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, AppsToFolder.TABLE_NAME, ONE_APPTOFOLDER);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Relationship.TABLE_NAME, ONE_RELATIONSHIP);
        URI_MATCHER.addURI(AppsSchema.AUTHORITY, Folders.TABLE_NAME + "/" + Folders.ALL, ALL_FOLDERS);
    }

    /** Content types for the indexed URI. */
    private static final String [] sContentTypes = {
        Apps.CONTENT_TYPE,
        Apps.CONTENT_ITEM_TYPE,
        Groups.CONTENT_TYPE,
        Groups.CONTENT_ITEM_TYPE,
        Members.CONTENT_TYPE,
        Members.CONTENT_ITEM_TYPE,
    };

    /** Table names used for the indexed URI. */
    private static final String [] sTableNames = {
        Apps.TABLE_NAME,
        Apps.TABLE_NAME,
        Groups.TABLE_NAME,
        Groups.TABLE_NAME,
        Members.TABLE_NAME,
        Members.TABLE_NAME,
        Folders.TABLE_NAME,
        AppsToFolder.TABLE_NAME,
        Relationship.TABLE_NAME,
        Folders.TABLE_NAME+", "+AppsToFolder.TABLE_NAME+", "+Relationship.TABLE_NAME 
    };

    /** Null-column hack strings used for the indexed URI. */
    private static final String [] sNullColumnHacks = {
        Apps.COMPONENT, null,
        Groups.NAME, null,
        null, null,
        null, null,
        null, null
    };

    /** True if the indexed URI has an ID. */
    private static final boolean [] sUsesId = {
        false, true,
        false, true,
        false, true,
        false, false,
        false, false
    };


    //-----------------------------------------------------
    // Methods

    public AppsProvider() {
        super(APPS_DB_NAME, APPS_DB_VERSION);
    }

    @Override
    protected void bootstrapDatabase(SQLiteDatabase db) {
        db.execSQL("PRAGMA auto_vacuum = 2");
        db.execSQL(Apps.CREATE_STATEMENT);
        db.execSQL(Groups.CREATE_STATEMENT);
        db.execSQL(Members.CREATE_STATEMENT);
        db.execSQL(Folders.CREATE_STATEMENT);
        db.execSQL(AppsToFolder.CREATE_STATEMENT);
        db.execSQL(Relationship.CREATE_STATEMENT);

        // Add special app groups
        int allAppsSortOrder = Groups.SORT_ALPHA;
        Log.d(TAG,"allAppsSortOrder ="+allAppsSortOrder);
        insertDbGroup(db, Groups.TYPE_ALL_APPS, allAppsSortOrder, Groups.ICON_SET_ALL_APPS);
        insertDbGroup(db, Groups.TYPE_FREQUENTS, Groups.SORT_FREQUENTS, Groups.ICON_SET_FREQUENTS);
        insertDbGroup(db, Groups.TYPE_DOWNLOADED, Groups.SORT_ALPHA, Groups.ICON_SET_DOWNLOADED);
        /*Added by ncqp34 at Jul-10-2012 for group flex*/	
        loadFromCDA(db);
	/*ended by ncqp34*/

    }

    @Override
    protected void onDatabaseOpened(SQLiteDatabase db) {
        // Temporary code to update group type for editable groups
        db.execSQL("UPDATE groups SET type=100 WHERE type=3;");
    }

    /**
     * Helper function to execute SQL statement to insert a row for one of the
     * special app groups.
     */
    private void insertDbGroup(SQLiteDatabase db, int type, int sort, int iconSet) {
        db.execSQL("INSERT INTO " + Groups.TABLE_NAME +
                " (" + Groups.TYPE + ", " + Groups.SORT + ", " + Groups.ICON_SET +
                ") VALUES (" + type + ", " + sort + ", " + iconSet + ");");
    }

    @Override
    protected boolean upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
       Log.d(TAG,"upgradeDatabase oldVersion="+oldVersion);
       int version = oldVersion;
       if (version < 2) {
           db.execSQL("ALTER TABLE " + Apps.TABLE_NAME + " ADD COLUMN " + Apps.HIDDEN + ";");
           version = 2;
       }
       if ( version < 3) {
           db.execSQL(Folders.CREATE_STATEMENT);
           db.execSQL(AppsToFolder.CREATE_STATEMENT);
           db.execSQL(Relationship.CREATE_STATEMENT);
           /* recent group converted to frequents group */
           db.execSQL("UPDATE " +Groups.TABLE_NAME +
                           " SET sort=" + Groups.SORT_FREQUENTS +
                           ",  type=" + Groups.TYPE_FREQUENTS + 
                           " WHERE type=1 AND sort=2;");  
           /* download group */
           db.execSQL("UPDATE " +Groups.TABLE_NAME +
                           " SET type=" + Groups.TYPE_DOWNLOADED +   
                           ",  sort=" + Groups.SORT_ALPHA + 
                           " WHERE type=2 AND sort=2;");
           /* recent sort group */
           db.execSQL("UPDATE " +Groups.TABLE_NAME +
                           " SET sort=" + Groups.SORT_ALPHA +                         
                           " WHERE sort=2;");      
           /* carrier sort group */
           db.execSQL("UPDATE " +Groups.TABLE_NAME +
                           " SET sort=" + Groups.SORT_CARRIER +                           
                           " WHERE sort=3;");           
           
           updatePackageNames(db);

           version = 3;
       }
       return true;
    }

    private boolean updatePackageNames(SQLiteDatabase db) {
        Log.d(TAG, "updatePackageNames");
        // Map to upgrade the package of applications
        final HashMap<String, String>packagesToUpgradeMap = new HashMap<String, String>();

        packagesToUpgradeMap.put(OLD_MSG_PKG, NEW_MSG_PKG);
        packagesToUpgradeMap.put(OLD_EMAIL_PKG, NEW_EMAIL_PKG);
        packagesToUpgradeMap.put(OLD_CALENDAR_PKG, NEW_CALENDAR_PKG);
        packagesToUpgradeMap.put(OLD_ALARM_PKG, NEW_ALARM_PKG);
        packagesToUpgradeMap.put(OLD_MUSIC_PKG, NEW_MUSIC_PKG);
        packagesToUpgradeMap.put(OLD_TASKS_PKG, NEW_TASKS_PKG);
        packagesToUpgradeMap.put(OLD_BOOKS_PKG, NEW_BOOKS_PKG);
        packagesToUpgradeMap.put(OLD_CONTACTS_PKG, NEW_CONTACTS_PKG);
        packagesToUpgradeMap.put(OLD_MYACC_PKG, NEW_MYACC_PKG);
        packagesToUpgradeMap.put(OLD_GALLERY_PKG, NEW_GALLERY_PKG);
        packagesToUpgradeMap.put(OLD_PHONE_PKG, NEW_PHONE_PKG);
        packagesToUpgradeMap.put(OLD_SOCIAL_LOCATION_PKG, NEW_SOCIAL_LOCATION_PKG);      
        packagesToUpgradeMap.put(OLD_FETION_PKG, NEW_FETION_PKG);
        packagesToUpgradeMap.put(OLD_NUMBER_LOCATION_PKG, NEW_NUMBER_LOCATION_PKG);
        packagesToUpgradeMap.put(OLD_CARDOCK_PKG, NEW_CARDOCK_PKG);
        packagesToUpgradeMap.put(OLD_INTSIG_PKG, NEW_INTSIG_PKG);


        packagesToUpgradeMap.put(OLD_VZW_APPS, NEW_VZW_APPS);
        packagesToUpgradeMap.put(OLD_YOUTUBE, NEW_YOUTUBE);
        packagesToUpgradeMap.put(OLD_VIDEOS, NEW_VIDEOS);

        Cursor c = null;
        db.beginTransaction();
        try {
            c = db.query(APPS_TABLE_NAME, new String[] {AppsSchema.Apps._ID, AppsSchema.Apps.COMPONENT}, null, null, null, null, null);
            final ContentValues values = new ContentValues();
            final int idIndex = c.getColumnIndex(AppsSchema.Apps._ID);
            final int componentIndex = c.getColumnIndex(AppsSchema.Apps.COMPONENT);
            final Set<String> oldPkgs = packagesToUpgradeMap.keySet();

            while (c.moveToNext()) {
                values.clear();
                String intentUri = c.getString(componentIndex);
                int favoriteId = c.getInt(idIndex);
                if (intentUri != null) {
                    for (String oldPkg : oldPkgs) {
                        if (intentUri.contains(oldPkg)) {
                            intentUri = intentUri.replace(oldPkg, packagesToUpgradeMap.get(oldPkg));
                            values.put(AppsSchema.Apps.COMPONENT, intentUri);
                            Log.d(TAG, "COMPONENT=" + favoriteId + " - replacing " + oldPkg + " by " + packagesToUpgradeMap.get(oldPkg));
                            break;
                        }
                    }
                }

                if (values.size() > 0) {
                    try {
                        String updateWhere = AppsSchema.Apps._ID + "=" + favoriteId;
                        db.update(APPS_TABLE_NAME, values, updateWhere, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Problem upgrading shortcut id = " +  favoriteId + ",exception ==" + e);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLException ex) {
            Log.e(TAG, "Problem while upgrading items" + ex);
            return false;
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = URI_MATCHER.match(uri);
        if (match < 0 && match >= sContentTypes.length) {
            return null;
        }
        return sContentTypes[match];
    }

    @Override
    protected Cursor queryInternal(final SQLiteDatabase db, Uri uri, String [] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        final int match = URI_MATCHER.match(uri);

        if (match < 0 && match >= sTableNames.length) {
            throw new IllegalArgumentException("Unknown URI for query: " + uri.toString());
        }
        qb.setTables(sTableNames[match]);

        if (sUsesId[match]) {
            qb.appendWhere(BaseColumns._ID + '=' + ContentUris.parseId(uri));
        }
        try{//2012-07-21, ChenYidong for SWITCHUI-2400
            return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        }catch(Exception e){
            return null;
        }
    }

    @Override
    protected Uri insertInternal(final SQLiteDatabase db, Uri uri, ContentValues values) {
        final int match = URI_MATCHER.match(uri);
        if (match < 0 && match >= sTableNames.length) {
            throw new IllegalArgumentException("Unknown URI for insert: " + uri.toString());
        }
        final String table = sTableNames[match];
//        Logger.d(TAG, "inserting into ", table, ", values=", values);
        long rowNum = db.insert(table, sNullColumnHacks[match], values);
        if (rowNum <= 0) {
            Log.e(TAG, "insert failed! " + uri.toString());
            return null;
        }
        try{//2012-07-21, ChenYidong for SWITCHUI-2400
            return Uri.withAppendedPath(uri, Long.toString(rowNum));
        }catch(Exception e){
            return null;
        }
    }

    @Override
    protected int updateInternal(final SQLiteDatabase db, Uri uri, ContentValues values,
            String selection, String [] selectionArgs) {
        final int match = URI_MATCHER.match(uri);
        if (match < 0 && match >= sTableNames.length) {
            throw new IllegalArgumentException("Unknown URI for update: " + uri.toString());
        }

        final StringBuilder where = new StringBuilder();
        if (sUsesId[match]) {
            where.append(BaseColumns._ID + '=' + ContentUris.parseId(uri));
        }
        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(AND);
            }
            where.append(selection);
        }

        final String table = sTableNames[match];
//        Logger.d(TAG, "updating ", table, " where ", where, ", values=", values);
        try{//2012-07-21, ChenYidong for SWITCHUI-2400
            return db.update(table, values, where.toString(), selectionArgs);
        }catch(Exception e){
            return 0;
        }
    }

    @Override
    protected int deleteInternal(final SQLiteDatabase db, Uri uri,
            String selection, String [] selectionArgs) {
        final int match = URI_MATCHER.match(uri);
        if (match < 0 && match >= sTableNames.length) {
            throw new IllegalArgumentException("Unknown URI for delete: " + uri.toString());
        }

        final StringBuilder where = new StringBuilder();
        if (sUsesId[match]) {
            where.append(BaseColumns._ID + '=' + ContentUris.parseId(uri));
        }
        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(AND);
            }
            where.append(selection);
        }

        final String table = sTableNames[match];
//        Logger.d(TAG, "deleting from ", table, " where ", where);
        try{//2012-07-21, ChenYidong for SWITCHUI-2400
            return db.delete(table, where.toString(), selectionArgs);
        }catch(Exception e){
            return 0;
        }
    }
    /*Added by ncqp34 at Jul-10-2012 for group flex*/	    
    /**
	 * @hide
	 */
	public String getResultFromCDA(String cmd) {
		Context mContext = getContext();
		TypedValue isCDA = new TypedValue();
		TypedValue contentCDA = new TypedValue();
		Resources r = mContext.getResources();
		String xmlresult = "";
		try {
			r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
			if (isCDA.coerceToString().equals("true")) {
				r.getValue(cmd, contentCDA, false); // cmd ->
				// "@FIHCDA@getWallpaper"
				xmlresult = contentCDA.coerceToString().toString();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!xmlresult.trim().equals("")) {
			return xmlresult;
		} else {
			String NoCDA = "";
			return NoCDA;
		}
	}
	public void loadFromCDA(SQLiteDatabase app_db){
		Context mContext = getContext();
		boolean needCDA = false;
		String xmlString = getResultFromCDA("@MOTOFLEX@getHomeApplications");
		XmlPullParser parser = null;
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		
		ContentResolver cr = mContext.getContentResolver();
		PackageManager packageManager = mContext.getPackageManager();
		/*Added by ncqp34 at Oct-24 for IKDOMINO-3163*/
		/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
		long groupCount = 3;
		/**end by bphx43*/
		/*ended by ncqp34*/
        if (!xmlString.trim().equals("")) {
			needCDA = true;
		}
        try{
        	if (needCDA) {
	        	XmlPullParserFactory factory;
	        	factory = XmlPullParserFactory.newInstance();
	        	parser = factory.newPullParser();
	        	factory.setNamespaceAware(true);
	        	parser.setInput(new StringReader(xmlString));
        	}else{
        		parser = mContext.getResources().getXml(
						R.xml.default_workspace);
        	}
        	AttributeSet attrs = Xml.asAttributeSet(parser);
        	XmlUtils.beginDocument(parser, TAG_FAVORITES);
        	final int depth = parser.getDepth();
        	
        	int type;
        	while (((type = parser.next()) != XmlPullParser.END_TAG || parser
        			.getDepth() > depth)
        			&& type != XmlPullParser.END_DOCUMENT) {
        		if (type != XmlPullParser.START_TAG) {
        			continue;
        		}
			/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
        		ContentValues values = new ContentValues();
			/**end by bphx43*/
        		boolean added = false;
        		final String name = parser.getName();
        		String packageName = "";
        		String className = "";
        		String spanx = "";
        		String spany = "";
        		String iconResId = "";// a.getResourceId(R.styleable.Favorite_icon, 0);
        		String titleResId = "";// a.getResourceId(R.styleable.Favorite_title,0);
        		String uri = "";// a.getString(R.styleable.Favorite_uri);
        		TypedArray a = null;
        		if (needCDA != true)
        			a = mContext.obtainStyledAttributes(attrs,
        					R.styleable.Favorite);
        		if (needCDA) {
					for (int j = 0; j < parser.getAttributeCount(); j++) {
						if (parser.getAttributeName(j).equals("launcher:screen"))
							values.put(Favorites.SCREEN, parser.getAttributeValue(j));
						else if (parser.getAttributeName(j).equals("launcher:x"))
							values.put(Favorites.CELLX, parser.getAttributeValue(j));
						else if (parser.getAttributeName(j).equals("launcher:y"))
							values.put(Favorites.CELLY, parser.getAttributeValue(j));
						else if (parser.getAttributeName(j).equals("launcher:packageName"))
							packageName = parser.getAttributeValue(j);
						else if (parser.getAttributeName(j).equals("launcher:className"))
							className = parser.getAttributeValue(j);
						else if (parser.getAttributeName(j).equals("launcher:spanX"))
							spanx = parser.getAttributeValue(j);
						else if (parser.getAttributeName(j).equals("launcher:spanY"))
							spany = parser.getAttributeValue(j);
						else if (parser.getAttributeName(j).equals("launcher:icon")) {
							iconResId = parser.getAttributeValue(j);
						} else if (parser.getAttributeName(j).equals("launcher:title")) {
							titleResId = parser.getAttributeValue(j);
						} else if (parser.getAttributeName(j).equals("launcher:uri"))
							uri = parser.getAttributeValue(j);
						else if (parser.getAttributeName(j).equals("launcher:modeId")) {
						/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin */
						/**modify for IKDOMINO-6717 by bphx43 2012-03-21*/
							int modeId = Integer.decode(parser.getAttributeValue(j));
							values.put(GROUP_MODEID, modeId);
						/** end by bphx43 */
						/* 2011-03-18, JimmyLi added for menu group */
						} else if (parser.getAttributeName(j).equals("launcher:groupcontainer")) {
							values.put(GROUP_CONTAINER, parser.getAttributeValue(j));
							/*
							 * values.put(Favorites.CONTAINER,
							 * parser.getAttributeValue(j)); if (LOGD)
							 * Log.d(TAG, "loadFavorites container:" +
							 * parser.getAttributeValue(j));
							 * values.put(Favorites.CONTAINER,
							 * Favorites.ITEM_TYPE_GROUP);
							 */
						}
						/* JimmyLi end */
						else if(parser.getAttributeName(j).equals("launcher:iconResource")) {
							iconResId = parser.getAttributeValue(j);
						}
						/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end */
					}
				} else {
					// PoHungLi-End shortcut
					/* Jimmyli end */
					values.put(Favorites.SCREEN, a.getString(R.styleable.Favorite_screen));
					values.put(Favorites.CELLX, a.getString(R.styleable.Favorite_x));
					values.put(Favorites.CELLY, a.getString(R.styleable.Favorite_y));
					/* 2010-10-28, JimmyLi added for switchMode field */
					/*
					 * values.put(Favorites.PROFILE_NUM,
					 * a.getString(R.styleable.Favorite_modeId));
					 */
					/* JimmyLi end */

					/*
					 * 2010-12-06, Jimmyli added according to FIH's for
					 * default CDA wallpaper
					 */
					// PoHungLi-Begin shortcut
				}
				// PoHungLi-End shortcut
				/* JimmyLi end */
				if (TAG_FAVORITE.equals(name)/* && menu group item */) {
					Log.i("Other", "AppsProvider.....values.getAsInteger(GROUP_CONTAINER)..."+values.getAsInteger(GROUP_CONTAINER));
					if (values.getAsInteger(GROUP_CONTAINER) != null) {
						long group_id = values.getAsLong(GROUP_CONTAINER) + 3;
						String component = packageName + "/" + className;
						ComponentName com = ComponentName.unflattenFromString(component);
//						String selection = Apps.COMPONENT;
						String[] where = new String[] { component };
						String selection = Apps.COMPONENT + "=? ";
						// Load apps
						/*Added by ncqp34 at Oct-24 for IKDOMINO-3163*/
						/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
						if(group_id  < groupCount || group_id  == groupCount  ){
							if(tempList.contains(component)){
								Long oldAppId = tempMap.get(component);
								addAppToGroup(this, app_db, oldAppId, group_id);
							}else{
								long appId = addApp(this, app_db, component);
								Log.d("Other", "AppsProvider appId "+appId);
								addAppToGroup(this, app_db, appId, group_id);
								tempMap.put(component, appId);
							}
						}
						tempList.add(component);
						/**end by bphx43*/
						/*ended by ncqp34*/
					}
				} else if (TAG_MENUGROUP.equals(name)) {
					Log.i("Other", "AppsProvider.....flex group + groupCount==" + groupCount);

					/*
					 * Addd into app.db ---table app
					 */
					// Groups.NAME = name , Groups.TYPE =
					// Groups.TYPE_USER;Groups.SORT =
					// Groups.SORT_ALPHA;Groups.ICON_SET =
					// Groups.ICON_SET_USER;
					/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin */
					// handleMenuGroup(values, iconResId, titleResId);
					handleMenuIcon(values, iconResId);
					handleMenuTitle(values, titleResId);
					String gname = values.getAsString(Favorites.TITLE);
					/** add for IKDOMINO-6717 by bphx43 2012-03-21 */
					/*Added by ncqp34 at Jul-25-2012 for switchui-2462*/
					int modeId = 0;
					if(values.getAsInteger(GROUP_MODEID)!=null){
					    modeId = values.getAsInteger(GROUP_MODEID);
					}
					/*ended by ncqp34*/
					int gtype = 0;
					if (modeId == Integer.decode("0xffff")) {
						gtype = Groups.TYPE_CBS;
					} else {
						gtype = Groups.TYPE_USER;
					}
					/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end */
					/** end by bphx43 */
					int gsort = Groups.SORT_ALPHA;
					int giconSet = Groups.ICON_SET_USER;
					/*Added by ncqp34 at Jul-25-2012 for switchui-2462*/
					if(values!= null && values.getAsInteger(Favorites.ICON_RESOURCE)!= null){
					/*ended by ncqp34*/
					/*int*/ giconSet = values.getAsInteger(Favorites.ICON_RESOURCE);
					}
					app_db.execSQL("INSERT INTO " + Groups.TABLE_NAME
							+ " (" + Groups.NAME +", "+ Groups.TYPE + ", "
							+ Groups.SORT + ", " + Groups.ICON_SET
							+ ") VALUES (" + " ' "+gname +" ' " + ", " + gtype + ", "
							+ gsort + ", " + giconSet + ");");
					/*Added by ncqp34 at Oct-24 for IKDOMINO-3163*/
					groupCount ++ ;
					/*ended by ncqp34*/
				}
				/* JimmyLi end */
				
				 /** 2010-12-06, JimmyLi added according to FIH's for default*/		 
				// PoHungLi-Begin shortcut
				if (a != null)
					// PoHungLi-End shortcut
					/* JimmyLi end */
					a.recycle();
				 /* CDA wallpaper*/
			Log.w("Other", "while break");	
			}
			/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
        		tempList.clear();
        		tempMap.clear();
			/**end by bphx43*/
        }catch (XmlPullParserException e) {
			Log.w(TAG, "Got exception parsing favorites.", e);
		} catch (IOException e) {
			Log.w(TAG, "Got exception parsing favorites.", e);
		}
	}
	/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin */
	
	private void handleMenuIcon(ContentValues values, String iconStr) {
		//2012-11-14, modifyed by amt_chenjing for switchui-3067
		iconStr = iconStr == null ? "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic" : iconStr;
		if (iconStr == null || "".equals(iconStr)) {
			iconStr = "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic";
		}
		//2012-11-14, modify end
		Context mContext = getContext();
		String str = iconStr;
		if (str.length() > 4 && "res:".equals(str.substring(0, 4))) {
			str = str.substring(4);
			str = str.trim();
			if ("com".equals(str.substring(0, 3))) {
				int indexOfPackage = str.indexOf(":");
				String packageName = null;
				if (indexOfPackage > 0) {
					packageName = str.substring(0, indexOfPackage);
				}
				Context packageContext;
				try {
					packageContext = mContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);     
		        } catch (NameNotFoundException e) {     
		            Log.d(TAG, "createPackageContext exception: " + e);     
		            e.printStackTrace();
		            values.put(Favorites.ICON_RESOURCE, 3);
		            return;
		        } 
				Resources r = packageContext.getResources();
				int id = r.getIdentifier(str, null, null);
				values.put(Favorites.ICON_RESOURCE, id);
			} else if ("android".equals(str.substring(0, 7))) {
				Resources r = mContext.getResources();
				int id = r.getIdentifier(str, null, null);
				if(id == 0) {
					values.put(Favorites.ICON_RESOURCE, 3);
				} else {
					values.put(Favorites.ICON_RESOURCE, id);
				}
			}
		}
		
	}
	

	private void handleMenuTitle(ContentValues values, String title) {
		String titleName = null;

		if (title != null && !"".equals(title) &&(title.length()>4)&& "res:".equals(title.substring(0,  4))) {
			titleName = title.substring(4);
		}else{
			titleName = title;
		}
		Log.d("DaJin", "titleName: " + titleName);
		values.put(Favorites.TITLE, titleName.trim());
	}
	/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end */
	// handle menu group
			private void handleMenuGroup(ContentValues values, String iconStr, String titleStr) {
				Context mContext = getContext();
				//2012-11-14, modifyed by amt_chenjing for switchui-3067
				iconStr = iconStr == null ? "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic" : iconStr;
				if (iconStr == null || "".equals(iconStr)) {
					iconStr = "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic";
				}
				//2012-11-14, modify end
				titleStr = titleStr == null ? "Other" : titleStr;
				// title is started as "res:", resource of other menu group
				if (titleStr.length() > 4 && "res:".equals(titleStr.substring(0, 4))) {
					// handle icon
					String icon = lookUpMenuGroupTable(titleStr);
					String iconPkg = null;
					int colon = icon.indexOf(':');
					if (colon > 0) {
						iconPkg = icon.substring(0, colon);
					}
					values.put(Favorites.ICON_TYPE, Favorites.ICON_TYPE_RESOURCE);
					values.put(Favorites.ICON_PACKAGE, iconPkg);
					values.put(Favorites.ICON_RESOURCE, icon);

					// handle title
					String title = titleStr.substring(4);
					Resources r = mContext.getResources();
					String titleName = title;
					int titleId = r.getIdentifier(titleName, null, null);
					/**add for 	IKDOMINO-6717 by bphx43 2012-03-21*/
					if (titleId == 0) {
						try {
							titleName = "default";// r.getResourceName(R.string.menugroup_default);
						} catch (NotFoundException e) {
							titleName = "Other";
						}
					}else{
						try{
							titleName = r.getString(titleId);
						}catch(NotFoundException e) {
							titleName = "Other";
						}
					}
					/**end by bphx43*/
					values.put(Favorites.TITLE, titleName);

					// title of carrier menu group
				} else {
					values.put(Favorites.TITLE, titleStr);
					// get icon from CDA
					try {
						Log.i("Other", "iconStr....................." + iconStr);
						Bitmap bitmap = BitmapFactory.decodeFile(iconStr);
						if (bitmap != null) {
							byte[] data = ItemInfo.flattenBitmap(bitmap);
							values.put(Favorites.ICON_TYPE, Favorites.ICON_TYPE_BITMAP);
							values.put(Favorites.ICON, data);
							return;
						}
					} catch (Exception e) {
						Log.e(TAG, "handleMenuGroup could not get icon bitmap from CDA, e:" + e);
					}

					// use default icon as failed to get icon from CDA...
					//2012-11-14, modifyed by amt_chenjing for switchui-3067
					values.put(Favorites.ICON_TYPE, Favorites.ICON_TYPE_RESOURCE);
					values.put(Favorites.ICON_PACKAGE, "com.motorola.mmsp.motohomex.motohome");
					values.put(Favorites.ICON_RESOURCE, "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic"); //
					//2012-11-14, modify end
				}
			}
			/* 2011-03-18, JimmyLi added for menu group */
			/**
			 * For menu group, following fields are necessary. 1. itemType:
			 * ITEM_TYPE_GROUP (the value is 5) 2. title: the title of the menu
			 * group. Both carrier menu group and other menu group; - Carrier menu
			 * group: just a text string, such as: China Telecom, China Unicom
			 * etc... - Other menu group: value will be the resource id, available
			 * text resource ids are:
			 * res:com.motorola.mmsp.motoswitch:string/menugroup_productivity
			 * res:com.motorola.mmsp.motoswitch:string/menugroup_tools
			 * res:com.motorola.mmsp.motoswitch:string/menugroup_entertainment 3.
			 * iconResource: Only meaningful for carrier menu group; 4. modeId: the
			 * editability of this menu group; (put into values in advance...)
			 */
		private String lookUpMenuGroupTable(String titleStr) {
				Log.i("Other", "lookUpMenuGroupTable.....titleStr.." + titleStr);
				// return the icon string of this titlestr identified menu group
				/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
				if(titleStr != null && !"".equals(titleStr)){
					int packageNameIndex = titleStr.indexOf(":");
					int packageNameLastIndex = titleStr.lastIndexOf(":");
					int iconIndex = titleStr.indexOf("/");
					String iconName = titleStr.substring(iconIndex);
					String packageName = titleStr.substring(packageNameIndex+1, packageNameLastIndex);
					return packageName+":drawable"+iconName+"_icon";
				}
					/**end  by bphx43 2012-03-21*/
				//2012-11-14, modifyed by amt_chenjing for switchui-3067
				return "com.motorola.mmsp.motohomex:drawable/ic_launcher_app_group_generic";
				//2012-11-14, modify end
			}

		private long addApp(AppsProvider provider, SQLiteDatabase db, String componentString) {
		        ContentValues values = new ContentValues();
		        values.put(Apps.COMPONENT, componentString);
		        values.put(Apps.COUNT, 0);
		        values.put(Apps.TIME, 0);
		        values.put(Apps.DOWNLOADED, 0);

		        Uri uri = provider.insertInternal(db, Apps.CONTENT_URI, values);
		        return ContentUris.parseId(uri);
		    }

		private void addAppToGroup(AppsProvider provider, SQLiteDatabase db, long appId, long groupId) {
		        ContentValues values = new ContentValues();
		        values.put(Members.APP, appId);
		        values.put(Members.GROUP, groupId);
		        provider.insertInternal(db, Members.CONTENT_URI, values);
		    }
	/*ended by ncqp34*/
}
