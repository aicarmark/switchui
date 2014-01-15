package com.motorola.numberlocation;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NumberLocationDatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "NumberLocationDatabaseHelper";
    private static final String DATABASE_NAME = "number_location.db";
	static final int DATABASE_VERSION = 10;      //it can be changed for upgrade
	static final int DATABASE_BASE_VERSION = 10; //should not change it
	

    private Context mContext;


    private static NumberLocationDatabaseHelper sSingleton = null;
    public static synchronized NumberLocationDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new NumberLocationDatabaseHelper(context);
        }
        return sSingleton;
    }
    
	@Override
	public void onOpen(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		super.onOpen(db);
	}

	public NumberLocationDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
	}
	private void bootstrapDB(SQLiteDatabase db) {//create table, but now table has been created automatically.
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading DB from version " + oldVersion
                + " to " + newVersion);
        if (oldVersion <= DATABASE_BASE_VERSION) {//error happened. ingnore the upgrade.
        	Log.e(TAG, "error in database upgrade!");
            return; // this was lossy
        }
        //do upgrade here
	}

	public boolean insert(String insert) {
		getInstance(mContext).getWritableDatabase().execSQL(insert);
		return true;
	}

	public boolean update(String update) {
		getInstance(mContext).getWritableDatabase().execSQL(update);
		return true;
	}

	public boolean delete(String del) {
		getInstance(mContext).getWritableDatabase().execSQL(del);
		return true;
	}

	public Cursor query(String query) {
		Cursor cur = this.getReadableDatabase().rawQuery(query, null);
		return cur;
	}
	
	public void execute(String execute) {
		getInstance(mContext).getWritableDatabase().execSQL(execute);
		return;
	}	
}
