package com.motorola.mmsp.socialGraph.socialGraphWidget.providers;

import java.util.ArrayList;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.ArrayListCursor;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.Skin;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.SkinResources;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class SocialProvider extends ContentProvider{
	private static final String TAG = "SocialGraph";
	private SQLiteOpenHelper mOpenHelper;
	private static final int URI_TABLE_SHORTCUTS = 1;
	private static final int URI_TABLE_CHANGE_HISTORY = 2;
	private static final int URI_TABLE_SKIN = 3;
	private static final int URI_TABLE_CONFIG = 4;
	private static final int URI_TABLE_DEFAULT_SKIN = 5;
    private static final String AUTHORITY = "com.motorola.mmsp.socialGraphWidget.provider";
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, DatebaseHelper.TABLE_SHORTCUTS, URI_TABLE_SHORTCUTS);
		URI_MATCHER.addURI(AUTHORITY, DatebaseHelper.TABLE_CHANGE_HISTORY, URI_TABLE_CHANGE_HISTORY);	
		URI_MATCHER.addURI(AUTHORITY, DatebaseHelper.TABLE_SKIN, URI_TABLE_SKIN);	
		URI_MATCHER.addURI(AUTHORITY, DatebaseHelper.TABLE_CONFIG + "/*", URI_TABLE_CONFIG);
		URI_MATCHER.addURI(AUTHORITY, DatebaseHelper.TABLE_DEFAULT_SKIN, URI_TABLE_DEFAULT_SKIN);	
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs){
		Log.d(TAG, "SocialProvider delete " + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int affectRows = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_SHORTCUTS:
			affectRows = db.delete(DatebaseHelper.TABLE_SHORTCUTS, 
					selection, selectionArgs);
			break;
			
		case URI_TABLE_CHANGE_HISTORY:
			affectRows = db.delete(DatebaseHelper.TABLE_CHANGE_HISTORY, 
					selection, selectionArgs);
			break;
		
		case URI_TABLE_SKIN:
			affectRows = db.delete(DatebaseHelper.TABLE_SKIN, selection, selectionArgs);
			
		default:
			break;
		}
		
		if (affectRows > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return affectRows;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, "SocialProvider insert " + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Uri myUri = null;
		long rowId = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_SHORTCUTS:
			rowId = db.insert(DatebaseHelper.TABLE_SHORTCUTS, 
					DatebaseHelper.SHORTCUT_INDEX, values);
			break;
		case URI_TABLE_CHANGE_HISTORY:
			rowId = db.insert(DatebaseHelper.TABLE_CHANGE_HISTORY, 
					DatebaseHelper.HISTORY_INDEX, values);
			break;
		case URI_TABLE_SKIN:
			rowId = db.insert(DatebaseHelper.TABLE_SKIN, 
					DatebaseHelper.SKIN_ID, values);
			break;
		default:
			break;
		}
		
		if (rowId > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		
		if (rowId > 0) {
			myUri= ContentUris.withAppendedId(uri, rowId);
		}
		return myUri;
	}

	@Override
	public boolean onCreate() {
		Log.d(TAG, "SocialProvider onCreate ");
		mOpenHelper = DatebaseHelper.getInstance(getContext());
		return (mOpenHelper != null);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(TAG, "SocialProvider query " + uri);
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor cursor = null;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_SHORTCUTS:
			cursor = db.query(DatebaseHelper.TABLE_SHORTCUTS, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		case URI_TABLE_CHANGE_HISTORY:
			cursor = db.query(DatebaseHelper.TABLE_CHANGE_HISTORY, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		case URI_TABLE_SKIN:
			cursor = db.query(DatebaseHelper.TABLE_SKIN, 
					projection, selection, selectionArgs, null, null, sortOrder);	
			break;		
		case URI_TABLE_DEFAULT_SKIN:
			cursor = getDefaultSkin(getContext());	
			break;		
		case URI_TABLE_CONFIG:
			ArrayList<ArrayList> rows = new ArrayList<ArrayList>();
			String param = uri.getLastPathSegment();
			if (param != null && param.equals("count")) {
				int shortcutCount = getContext().getResources().getInteger(R.integer.shortcut_count);
				ArrayList<Object> item = new ArrayList<Object>();
				item.add("count");
				item.add(shortcutCount);
				rows.add(item);
			} else if (param != null && param.equals("mode")) {
				boolean mode = Setting.getInstance(getContext()).isAutoMode();
				ArrayList<Object> item = new ArrayList<Object>();
				item.add("count");
				item.add(mode ? 1 : 0);
				rows.add(item);
			} else if (param != null && param.equals("stretch")) {
				boolean stretch = Setting.getInstance(getContext()).isStretchOn();
				ArrayList<Object> item = new ArrayList<Object>();
				item.add("count");
				item.add(stretch ? 1 : 0);
				rows.add(item);
			}
			cursor = new ArrayListCursor(new String[]{"key", "value"}, rows);
		default:
			break;
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.d(TAG, "SocialProvider update " + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int affectedRows = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_SHORTCUTS:
			affectedRows = db.update(DatebaseHelper.TABLE_SHORTCUTS, 
					values, selection, selectionArgs);
			break;
		case URI_TABLE_CHANGE_HISTORY:
			affectedRows = db.update(DatebaseHelper.TABLE_CHANGE_HISTORY, 
					values, selection, selectionArgs);
			break;
		case URI_TABLE_SKIN:
			affectedRows = db.update(DatebaseHelper.TABLE_SKIN, 
					values, selection, selectionArgs);
			break;
		default:
			break;
		}
		
		if (affectedRows > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return affectedRows;
	}
	
	private Cursor getDefaultSkin(Context context) {
		String[] SKIN_PROJECTION = new String[] {
				DatebaseHelper.SKIN_ID,
				DatebaseHelper.SKIN_BG_1,
				DatebaseHelper.SKIN_BAR_1,
				DatebaseHelper.SKIN_BG_2,
				DatebaseHelper.SKIN_BAR_2,
				DatebaseHelper.SKIN_BG_3,
				DatebaseHelper.SKIN_BAR_3,
				DatebaseHelper.SKIN_BG_4,
				DatebaseHelper.SKIN_BAR_4,
				DatebaseHelper.SKIN_BG_5,
				DatebaseHelper.SKIN_BAR_5,
				DatebaseHelper.SKIN_BG_6,						
				DatebaseHelper.SKIN_BAR_6,
				DatebaseHelper.SKIN_BG_7,
				DatebaseHelper.SKIN_BAR_7,						
				DatebaseHelper.SKIN_BG_8,
				DatebaseHelper.SKIN_BAR_8,
				DatebaseHelper.SKIN_BG_9,
				DatebaseHelper.SKIN_BAR_9,
		};
		ArrayList<ArrayList> rows = new ArrayList<ArrayList>();
		try {
			ArrayList<Skin> skins = SkinResources.getInstance(context).getSkins();
			Skin defaultSkin = skins.get(0);
			for (Skin skin : skins) {
				if (skin != null ) {
					Integer nameId = (Integer)skin.properties.get(Skin.NAME);
					if (R.string.blue == nameId) {
						defaultSkin = skin;
						break;
					}
				}
			}
			ArrayList row = new ArrayList();
			row.add(0);
			row.add(defaultSkin.properties.get(Skin.BG_1));
			row.add(defaultSkin.properties.get(Skin.BAR_1));
			row.add(defaultSkin.properties.get(Skin.BG_2));
			row.add(defaultSkin.properties.get(Skin.BAR_2));
			row.add(defaultSkin.properties.get(Skin.BG_3));
			row.add(defaultSkin.properties.get(Skin.BAR_3));
			row.add(defaultSkin.properties.get(Skin.BG_4));
			row.add(defaultSkin.properties.get(Skin.BAR_4));
			row.add(defaultSkin.properties.get(Skin.BG_5));
			row.add(defaultSkin.properties.get(Skin.BAR_5));
			row.add(defaultSkin.properties.get(Skin.BG_6));
			row.add(defaultSkin.properties.get(Skin.BAR_6));
			row.add(defaultSkin.properties.get(Skin.BG_7));
			row.add(defaultSkin.properties.get(Skin.BAR_7));
			row.add(defaultSkin.properties.get(Skin.BG_8));
			row.add(defaultSkin.properties.get(Skin.BAR_8));
			row.add(defaultSkin.properties.get(Skin.BG_9));
			row.add(defaultSkin.properties.get(Skin.BAR_9));
			rows.add(row);
		} catch (Exception e) {
		}
		return new ArrayListCursor(SKIN_PROJECTION, rows);
	}
}



