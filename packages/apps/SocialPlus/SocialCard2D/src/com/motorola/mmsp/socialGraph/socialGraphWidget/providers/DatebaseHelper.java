package com.motorola.mmsp.socialGraph.socialGraphWidget.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.Skin;

public class DatebaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "SocialGraph";
	static final String DATABASE_NAME = "SocialProvider.db";
	static final int DATABASE_VERSION = 1;
	
	public static final String TABLE_SHORTCUTS = "shortcuts";
	public static final String TABLE_CHANGE_HISTORY = "change_history";
	public static final String TABLE_SKIN = "skin";
	public static final String TABLE_CONFIG = "config";
	public static final String TABLE_DEFAULT_SKIN = "default_skin";
	
	public static final String SHORTCUT_ID = "_id";
	public static final String SHORTCUT_INDEX = "shortcut_index";
	public static final String SHORTCUT_SIZE = "size";
	//public static final String SHORTCUT_RAW_CONTACT_ID = "raw_contact_id";
	public static final String SHORTCUT_CONTACT_ID = "contact_id";
	
	public static final String HISTORY_ID = "_id";
	public static final String HISTORY_TYPE = "type";
	public static final String HISTORY_INDEX = "history_index";
	public static final String HISTORY_DATA1 = "data1";
	public static final String HISTORY_DATA2 = "data2";
	public static final String HISTORY_DATA3 = "data3";
	public static final String HISTORY_DATA4 = "data4";
	
	public static final int HISTORY_UPDATE_SIZE = 1;
	public static final String HISTORY_FORM_SIZE = HISTORY_DATA1;
	public static final String HISTORY_TO_SIZE = HISTORY_DATA2;
	
	public static final int HISTORY_UPDATE_POSITION = 2;
	public static final String HISTORY_FORM_POSITION = HISTORY_DATA1;
	public static final String HISTORY_TO_POSITION = HISTORY_DATA2;
	
	public static final int HISTORY_UPDATE_INFO = 3;
	
	
	public static final String SKIN_ID = "_id";
	public static final String SKIN_BG_1 = Skin.BG_1;
	public static final String SKIN_BG_2 = Skin.BG_2;
	public static final String SKIN_BG_3 = Skin.BG_3;
	public static final String SKIN_BG_4 = Skin.BG_4;
	public static final String SKIN_BG_5 = Skin.BG_5;
	public static final String SKIN_BG_6 = Skin.BG_6;
	public static final String SKIN_BG_7 = Skin.BG_7;
	public static final String SKIN_BG_8 = Skin.BG_8;
	public static final String SKIN_BG_9 = Skin.BG_9;
	public static final String SKIN_BAR_1 = Skin.BAR_1;
	public static final String SKIN_BAR_2 = Skin.BAR_2;
	public static final String SKIN_BAR_3 = Skin.BAR_3;
	public static final String SKIN_BAR_4 = Skin.BAR_4;
	public static final String SKIN_BAR_5 = Skin.BAR_5;
	public static final String SKIN_BAR_6 = Skin.BAR_6;
	public static final String SKIN_BAR_7 = Skin.BAR_7;
	public static final String SKIN_BAR_8 = Skin.BAR_8;
	public static final String SKIN_BAR_9 = Skin.BAR_9;


    private static DatebaseHelper mInstance = null; 
	
	private void createShortcutInfosTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SHORTCUTS + " (" +
        		SHORTCUT_ID + " integer primary key autoincrement, " +
        		SHORTCUT_INDEX + " INTEGER," +
        		SHORTCUT_SIZE + " INTEGER," +
        		SHORTCUT_CONTACT_ID + " INTEGER " +
                ");");
	}
	
	private void createChangeHistoryTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CHANGE_HISTORY + " (" +
				HISTORY_ID + " integer primary key autoincrement, " +
				HISTORY_TYPE + " INTEGER," +
				HISTORY_INDEX + " INTEGER," +
				HISTORY_DATA1 + " TEXT," +
				HISTORY_DATA2 + " TEXT," +
				HISTORY_DATA3 + " TEXT," +
				HISTORY_DATA4 + " TEXT" +
                ");");
	}
	
	private void createSkinTable(SQLiteDatabase db){
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SKIN + " (" +
				SKIN_ID + " integer primary key autoincrement, " +
				SKIN_BG_1 + " INTEGER," +
				SKIN_BAR_1 + " INTEGER," +
				SKIN_BG_2 + " INTEGER," +
				SKIN_BAR_2 + " INTEGER," +
				SKIN_BG_3 + " INTEGER," +
				SKIN_BAR_3 + " INTEGER," +
				SKIN_BG_4 + " INTEGER," +
				SKIN_BAR_4 + " INTEGER," +
				SKIN_BG_5 + " INTEGER," +
				SKIN_BAR_5 + " INTEGER," +
				SKIN_BG_6 + " INTEGER," +
				SKIN_BAR_6 + " INTEGER," +
				SKIN_BG_7 + " INTEGER," +
				SKIN_BAR_7 + " INTEGER," +
				SKIN_BG_8 + " INTEGER," +
				SKIN_BAR_8 + " INTEGER," +
				SKIN_BG_9 + " INTEGER," +
				SKIN_BAR_9 + " INTEGER " +	
				");");		
	}
	
	
	private DatebaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	static synchronized DatebaseHelper getInstance(Context context) {
        if (mInstance == null) {
    		Log.d(TAG, "DatebaseHelper getInstance");
            mInstance = new DatebaseHelper(context);
        }
        return mInstance;
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "DatebaseHelper onCreate");
		createShortcutInfosTable(db);
		createChangeHistoryTable(db);
		createSkinTable(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}