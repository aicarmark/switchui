package com.motorola.numberlocation;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import com.motorola.numberlocation.NumberLocationProviderConst;

public class NumberLocationProvider extends ContentProvider {
	private static final String TAG = "NumberLocationProvider";
	
	private NumberLocationDatabaseHelper mdbHelper = null;
	public static final String AUTHORITY = NumberLocationProviderConst.AUTHORITY.toString();
	
	private static final int NUMBER_0 = 100;
	private static final int NUMBER_00 = 101; 
	private static final int NUMBER_130 = 102;
	private static final int NUMBER_131 = 103;
	private static final int NUMBER_132 = 104;
	private static final int NUMBER_133 = 105;
	private static final int NUMBER_134 = 106;
	private static final int NUMBER_135 = 107;
	private static final int NUMBER_136 = 108;
	private static final int NUMBER_137 = 109;
	private static final int NUMBER_138 = 110;
	private static final int NUMBER_139 = 111;
	private static final int NUMBER_145 = 112;
	private static final int NUMBER_147 = 113;
	private static final int NUMBER_150 = 114;
	private static final int NUMBER_151 = 115;
	private static final int NUMBER_152 = 116;
	private static final int NUMBER_153 = 117;
	private static final int NUMBER_155 = 118;
	private static final int NUMBER_156 = 119;
	private static final int NUMBER_157 = 120;
	private static final int NUMBER_158 = 121;
	private static final int NUMBER_159 = 122;
	private static final int NUMBER_180 = 123;
	private static final int NUMBER_186 = 124;
	private static final int NUMBER_187 = 125;
	private static final int NUMBER_188 = 126;
	private static final int NUMBER_189 = 127;
	private static final int CITY = 128;
	private static final int PROVINCE = 129;
	private static final int COUNTRY = 130;
	private static final int UPDATE_INFO = 131;
	private static final int NORMAL_END = 199;

	private static final int NUMBER_0_ROWID = 200;
	private static final int NUMBER_00_ROWID = 201; 
	private static final int NUMBER_130_ROWID = 202;
	private static final int NUMBER_131_ROWID = 203;
	private static final int NUMBER_132_ROWID = 204;
	private static final int NUMBER_133_ROWID = 205;
	private static final int NUMBER_134_ROWID = 206;
	private static final int NUMBER_135_ROWID = 207;
	private static final int NUMBER_136_ROWID = 208;
	private static final int NUMBER_137_ROWID = 209;
	private static final int NUMBER_138_ROWID = 210;
	private static final int NUMBER_139_ROWID = 211;
	private static final int NUMBER_145_ROWID = 212;
	private static final int NUMBER_147_ROWID = 213;
	private static final int NUMBER_150_ROWID = 214;
	private static final int NUMBER_151_ROWID = 215;
	private static final int NUMBER_152_ROWID = 216;
	private static final int NUMBER_153_ROWID = 217;
	private static final int NUMBER_155_ROWID = 218;
	private static final int NUMBER_156_ROWID = 219;
	private static final int NUMBER_157_ROWID = 220;
	private static final int NUMBER_158_ROWID = 221;
	private static final int NUMBER_159_ROWID = 222;
	private static final int NUMBER_180_ROWID = 223;
	private static final int NUMBER_186_ROWID = 224;
	private static final int NUMBER_187_ROWID = 225;
	private static final int NUMBER_188_ROWID = 226;
	private static final int NUMBER_189_ROWID = 227;
	
	private static final int ROWID_END = 299;
	
	private static final int CITY_ID = 300;
	private static final int PROVINCE_ID = 301;
	private static final int COUNTRY_ID = 302;
	
	
	private static final UriMatcher URI_MATCHER;
	static
	{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		
		URI_MATCHER.addURI(AUTHORITY, "number_0", NUMBER_0);
		URI_MATCHER.addURI(AUTHORITY, "number_00", NUMBER_00);
		URI_MATCHER.addURI(AUTHORITY, "number_130", NUMBER_130);
		URI_MATCHER.addURI(AUTHORITY, "number_131", NUMBER_131);
		URI_MATCHER.addURI(AUTHORITY, "number_132", NUMBER_132);
		URI_MATCHER.addURI(AUTHORITY, "number_133", NUMBER_133);
		URI_MATCHER.addURI(AUTHORITY, "number_134", NUMBER_134);
		URI_MATCHER.addURI(AUTHORITY, "number_135", NUMBER_135);
		URI_MATCHER.addURI(AUTHORITY, "number_136", NUMBER_136);
		URI_MATCHER.addURI(AUTHORITY, "number_137", NUMBER_137);
		URI_MATCHER.addURI(AUTHORITY, "number_138", NUMBER_138);
		URI_MATCHER.addURI(AUTHORITY, "number_139", NUMBER_139);
		URI_MATCHER.addURI(AUTHORITY, "number_145", NUMBER_145);
		URI_MATCHER.addURI(AUTHORITY, "number_147", NUMBER_147);
		URI_MATCHER.addURI(AUTHORITY, "number_150", NUMBER_150);
		URI_MATCHER.addURI(AUTHORITY, "number_151", NUMBER_151);
		URI_MATCHER.addURI(AUTHORITY, "number_152", NUMBER_152);
		URI_MATCHER.addURI(AUTHORITY, "number_153", NUMBER_153);
		URI_MATCHER.addURI(AUTHORITY, "number_155", NUMBER_155);
		URI_MATCHER.addURI(AUTHORITY, "number_156", NUMBER_156);
		URI_MATCHER.addURI(AUTHORITY, "number_157", NUMBER_157);
		URI_MATCHER.addURI(AUTHORITY, "number_158", NUMBER_158);
		URI_MATCHER.addURI(AUTHORITY, "number_159", NUMBER_159);
		URI_MATCHER.addURI(AUTHORITY, "number_180", NUMBER_180);
		URI_MATCHER.addURI(AUTHORITY, "number_186", NUMBER_186);
		URI_MATCHER.addURI(AUTHORITY, "number_187", NUMBER_187);
		URI_MATCHER.addURI(AUTHORITY, "number_188", NUMBER_188);
		URI_MATCHER.addURI(AUTHORITY, "number_189", NUMBER_189);
		URI_MATCHER.addURI(AUTHORITY, "city", CITY);
		URI_MATCHER.addURI(AUTHORITY, "province", PROVINCE);
		URI_MATCHER.addURI(AUTHORITY, "country", COUNTRY);
		URI_MATCHER.addURI(AUTHORITY, "update_info", UPDATE_INFO);
		
		URI_MATCHER.addURI(AUTHORITY, "number_0/#", NUMBER_0_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_00/#", NUMBER_00_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_130/#", NUMBER_130_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_131/#", NUMBER_131_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_132/#", NUMBER_132_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_133/#", NUMBER_133_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_134/#", NUMBER_134_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_135/#", NUMBER_135_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_136/#", NUMBER_136_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_137/#", NUMBER_137_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_138/#", NUMBER_138_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_139/#", NUMBER_139_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_145/#", NUMBER_145_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_147/#", NUMBER_147_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_150/#", NUMBER_150_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_151/#", NUMBER_151_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_152/#", NUMBER_152_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_153/#", NUMBER_153_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_155/#", NUMBER_155_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_156/#", NUMBER_156_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_157/#", NUMBER_157_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_158/#", NUMBER_158_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_159/#", NUMBER_159_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_180/#", NUMBER_180_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_186/#", NUMBER_186_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_187/#", NUMBER_187_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_188/#", NUMBER_188_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "number_189/#", NUMBER_189_ROWID);
		URI_MATCHER.addURI(AUTHORITY, "city/#", CITY_ID);
		URI_MATCHER.addURI(AUTHORITY, "province/#", PROVINCE_ID);
		URI_MATCHER.addURI(AUTHORITY, "country/#", COUNTRY_ID);

	}

	private static HashMap<Integer, String> buildNumberLocationUriMap() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		map.put(Integer.valueOf(NUMBER_0) , "number_0");
		map.put(Integer.valueOf(NUMBER_00) , "number_00");
		map.put(Integer.valueOf(NUMBER_130) , "number_130");
		map.put(Integer.valueOf(NUMBER_131) , "number_131");
		map.put(Integer.valueOf(NUMBER_132) , "number_132");
		map.put(Integer.valueOf(NUMBER_133) , "number_133");
		map.put(Integer.valueOf(NUMBER_134) , "number_134");
		map.put(Integer.valueOf(NUMBER_135) , "number_135");
		map.put(Integer.valueOf(NUMBER_136) , "number_136");
		map.put(Integer.valueOf(NUMBER_137) , "number_137");
		map.put(Integer.valueOf(NUMBER_138) , "number_138");
		map.put(Integer.valueOf(NUMBER_139) , "number_139");
		map.put(Integer.valueOf(NUMBER_145) , "number_145");
		map.put(Integer.valueOf(NUMBER_147) , "number_147");
		map.put(Integer.valueOf(NUMBER_150) , "number_150");
		map.put(Integer.valueOf(NUMBER_151) , "number_151");
		map.put(Integer.valueOf(NUMBER_152) , "number_152");
		map.put(Integer.valueOf(NUMBER_153) , "number_153");
		map.put(Integer.valueOf(NUMBER_155) , "number_155");
		map.put(Integer.valueOf(NUMBER_156) , "number_156");
		map.put(Integer.valueOf(NUMBER_157) , "number_157");
		map.put(Integer.valueOf(NUMBER_158) , "number_158");
		map.put(Integer.valueOf(NUMBER_159) , "number_159");
		map.put(Integer.valueOf(NUMBER_180) , "number_180");
		map.put(Integer.valueOf(NUMBER_186) , "number_186");
		map.put(Integer.valueOf(NUMBER_187) , "number_187");
		map.put(Integer.valueOf(NUMBER_188) , "number_188");
		map.put(Integer.valueOf(NUMBER_189) , "number_189");

		map.put(Integer.valueOf(CITY) , "city");
		map.put(Integer.valueOf(PROVINCE) , "province");
		map.put(Integer.valueOf(COUNTRY) , "country");
		map.put(Integer.valueOf(UPDATE_INFO) , "update_info");

		map.put(Integer.valueOf(NUMBER_0_ROWID) , "number_0");
		map.put(Integer.valueOf(NUMBER_00_ROWID) , "number_00");
		map.put(Integer.valueOf(NUMBER_130_ROWID) , "number_130");
		map.put(Integer.valueOf(NUMBER_131_ROWID) , "number_131");
		map.put(Integer.valueOf(NUMBER_132_ROWID) , "number_132");
		map.put(Integer.valueOf(NUMBER_133_ROWID) , "number_133");
		map.put(Integer.valueOf(NUMBER_134_ROWID) , "number_134");
		map.put(Integer.valueOf(NUMBER_135_ROWID) , "number_135");
		map.put(Integer.valueOf(NUMBER_136_ROWID) , "number_136");
		map.put(Integer.valueOf(NUMBER_137_ROWID) , "number_137");
		map.put(Integer.valueOf(NUMBER_138_ROWID) , "number_138");
		map.put(Integer.valueOf(NUMBER_139_ROWID) , "number_139");
		map.put(Integer.valueOf(NUMBER_145_ROWID) , "number_145");
		map.put(Integer.valueOf(NUMBER_147_ROWID) , "number_147");
		map.put(Integer.valueOf(NUMBER_150_ROWID) , "number_150");
		map.put(Integer.valueOf(NUMBER_151_ROWID) , "number_151");
		map.put(Integer.valueOf(NUMBER_152_ROWID) , "number_152");
		map.put(Integer.valueOf(NUMBER_153_ROWID) , "number_153");
		map.put(Integer.valueOf(NUMBER_155_ROWID) , "number_155");
		map.put(Integer.valueOf(NUMBER_156_ROWID) , "number_156");
		map.put(Integer.valueOf(NUMBER_157_ROWID) , "number_157");
		map.put(Integer.valueOf(NUMBER_158_ROWID) , "number_158");
		map.put(Integer.valueOf(NUMBER_159_ROWID) , "number_159");
		map.put(Integer.valueOf(NUMBER_180_ROWID) , "number_180");
		map.put(Integer.valueOf(NUMBER_186_ROWID) , "number_186");
		map.put(Integer.valueOf(NUMBER_187_ROWID) , "number_187");
		map.put(Integer.valueOf(NUMBER_188_ROWID) , "number_188");
		map.put(Integer.valueOf(NUMBER_189_ROWID) , "number_189");        

		map.put(Integer.valueOf(CITY_ID) , "city");
		map.put(Integer.valueOf(PROVINCE_ID) , "province");
		map.put(Integer.valueOf(COUNTRY_ID) , "country");

        return map;
    }

	
	//columns from country table
	public interface countryColumns{
        public static final String ID = "_id";
		public static final String COUNTRY = "country";
		public static final String COUNTRY_EN = "country_en";
	}
	
	//columns from city table
	public interface cityColumns{
        public static final String ID = "_id";
		public static final String CITY = "city";
		public static final String PROVINCE_ID = "province_id";
		public static final String FLAG = "flag";
	}
	
	//columns from province table
	public interface provinceColumns{
		public static final String ID = "id";
		public static final String PROVINCE = "province";
	}
	
	//columns from fix domestic table
	public interface number_0Columns{
		public static final String ROWID = "rowid";
		public static final String CITY_ID = "city_id";
	}
	
	//columns from fix international table
	public interface number_00Columns{
		public static final String ROWID = "rowid";
		public static final String COUNTRY_ID = "country_id";
	}
	
	//columns from mobilie 1xx table
	public interface number_1xxColumns{
		public static final String CITY_ID = "city_id";
	}
	
	//columns from update_info table
	public interface update_infoColumns{
		public static final String BASE_MAJOR_VERSION = "base_major_version";
		public static final String BASE_MINOR_VERSION = "base_minor_version";
		public static final String CURRENT_MAJOR_VERSION = "current_major_version";
		public static final String CURRENT_MINOR_VERSION = "current_minor_version";
		public static final String UPDATE_TIMESTAMP = "update_timestamp";
	}
	
	
	
	@Override
	public String getType(Uri uri) {
		int match = URI_MATCHER.match(uri);
		String table = buildNumberLocationUriMap().get(Integer.valueOf(match));
		String type = null;
		if(match>NORMAL_END&&match<ROWID_END){//means rowid search
			type = "vnd.android.cursor.item" + "/" + table;
		}else if(match > ROWID_END){//means id search
			type = "vnd.android.cursor.item" + "/" + table;
		}else{
			type = "vnd.android.cursor.dir" + "/" + table;
		}
		return type;
	}
    
    private static final class GetTableAndWhereOutParameter {
        private String table;
        private String where;
		public void setWhere(String where) {
			this.where = where;
		}
		public String getWhere() {
			return where;
		}
		public void setTable(String table) {
			this.table = table;
		}
		public String getTable() {
			return table;
		}
		public void clean(){
			table = null;
			where = null;
		}
    }

	private void createTableNumber_1xx(String tableName) {
		SQLiteDatabase db = mdbHelper.getWritableDatabase();

		db.beginTransaction();
		try {
			String sql_create_table = "CREATE TABLE IF NOT EXISTS " + tableName
					+ " (city_id INT)";
			
			SQLiteStatement stmt_create_table = db
					.compileStatement(sql_create_table);
			stmt_create_table.execute();
			// mdbHelper.execute(sql_create_table);
			// insert 10000 zero records to initialize table.
			String sql_insert_record = "INSERT INTO " + tableName
					+ " VALUES(0)";
			SQLiteStatement stmt_insert_record = db
					.compileStatement(sql_insert_record);
			for (int i = 0; i < 10000; i++) {
				stmt_insert_record.execute();
				// mdbHelper.execute(sql_insert_record);
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
			db.endTransaction();
		}
		db.endTransaction();
	}
	
	private boolean isExistTable(String table) {
		SQLiteDatabase db = mdbHelper.getWritableDatabase();
		boolean result = false;
		if (table == null) {
			return false;
		}
		Cursor cursor = null;
		try {
			String sql = "select count(*) as c from sqlite_master where type ='table' and name ='"
					+ table.trim() + "' ";
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToNext()) {
				int count = cursor.getInt(0);
				if (count > 0) {
					result = true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
    static final GetTableAndWhereOutParameter sGetTableAndWhereParam =
            new GetTableAndWhereOutParameter();

    private void getTableAndWhere(Uri uri, String userWhere,
    		GetTableAndWhereOutParameter out) {
		int match = URI_MATCHER.match(uri);
		
		//deal no table problem only for 1xx
		if(match == -1){
			//there should add a create action;
			String table = uri.getPathSegments().get(0);
			if(!table.startsWith("number_1", 0))
				return;
			if(!isExistTable(table)){
				createTableNumber_1xx(table);
			}
			out.setTable(table);
		}else{
			String table = buildNumberLocationUriMap().get(Integer.valueOf(match));
			if (!TextUtils.isEmpty(table)) {
				out.setTable(table);
			}
		}
    	String where = null;
    	if((match>NORMAL_END&&match<ROWID_END)||(match==-1)){//means rowid search
    		int nRowId = 0;
    		if((uri.getPathSegments().get(0).equals("number_0"))||(uri.getPathSegments().get(0).equals("number_00")))
    			nRowId = Integer.valueOf(uri.getPathSegments().get(1));
    		else
    			nRowId = Integer.valueOf(uri.getPathSegments().get(1))+1;
    		where = "rowid=" + Integer.toString(nRowId);
    	}else if(match > ROWID_END){//means id search
    		where = "_id=" + uri.getPathSegments().get(1);
    	}
    	// Add in the user requested WHERE clause, if needed
    	if (!TextUtils.isEmpty(userWhere)) {
    		if (!TextUtils.isEmpty(where)) {
    			out.setWhere(where + " AND (" + userWhere + ")");
    		} else {
    			out.setWhere(userWhere);
    		}
    	} else {
    		out.setWhere(where);
    	}
//    	Log.d(TAG,"table="+out.getTable()+"where="+out.getWhere());
    }   
    

	@Override
	public boolean onCreate() {
		mdbHelper = NumberLocationDatabaseHelper.getInstance(this.getContext());

		return true;		
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String userWhere, String[] whereArgs, String sortOrder) {
		String groupBy = null;
    	Log.d(TAG,"query userWhere="+userWhere);

		
		getTableAndWhere(uri,userWhere,sGetTableAndWhereParam);
		String table = sGetTableAndWhereParam.getTable();

		if (projection != null) {
			for (int i = 0; i < projection.length; i++) {
				if (projection[i].equals(OpenableColumns.DISPLAY_NAME)) {
					projection[i] = "title AS " + OpenableColumns.DISPLAY_NAME;
				}
			}
		}

		SQLiteDatabase db = mdbHelper.getReadableDatabase();
		Cursor cur = db.query(table, projection, userWhere,
				whereArgs, groupBy, null, sortOrder);
		if (cur != null) {
			cur.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cur;
	}
	
	@Override
	public int delete(Uri uri,String userWhere, String[] whereArgs) {
        SQLiteDatabase db = mdbHelper.getWritableDatabase();

        int count = 0;
        synchronized (sGetTableAndWhereParam) {
        	getTableAndWhere(uri,userWhere,sGetTableAndWhereParam);
        	String table = sGetTableAndWhereParam.getTable();
        	if(!TextUtils.isEmpty(table))
        		count = db.delete(table, sGetTableAndWhereParam.getWhere(), whereArgs);
        }
		return count;
	}


	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowid = 0;
		Uri newUri = null;

		String table =null;
		SQLiteDatabase db = mdbHelper.getWritableDatabase();
		synchronized (sGetTableAndWhereParam) {
			getTableAndWhere(uri,null,sGetTableAndWhereParam);
			table = sGetTableAndWhereParam.getTable();
			if(!TextUtils.isEmpty(table))
				rowid = db.insert(table, "", values);
		}
        if (rowid > 0) {
        	Uri u = Uri.parse(AUTHORITY+"/"+table);
            newUri = ContentUris.withAppendedId(u, rowid);
        }
		if (newUri != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return newUri;
	}


	@Override
	public int update(Uri uri, ContentValues values, String userWhere, String[] whereArgs) {
		SQLiteDatabase db = mdbHelper.getWritableDatabase();

		int count = 0;
		synchronized (sGetTableAndWhereParam) {
			sGetTableAndWhereParam.clean();
			getTableAndWhere(uri,userWhere,sGetTableAndWhereParam);
			String table = sGetTableAndWhereParam.getTable();
			if(!TextUtils.isEmpty(table))
				count = db.update(table, values,sGetTableAndWhereParam.getWhere(), whereArgs);
		}
		return count;
	}

}
