package com.motorola.numberlocation;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.numberlocation.NumberLocationProviderConst;
import com.motorola.numberlocation.NumberLocationProvider.cityColumns;
import com.motorola.numberlocation.NumberLocationProvider.countryColumns;
import com.motorola.numberlocation.NumberLocationProvider.number_0Columns;
import com.motorola.numberlocation.NumberLocationProvider.number_1xxColumns;
import com.motorola.numberlocation.NumberLocationProvider.provinceColumns;

public class DatabaseUpdate {

	public static final String NUMBER_LOCATION_PROVIDER = "com.motorola.numberlocation.provider";
	
	private static final int NUMBER_TYPE_UNKNOW = 1;
	private static final int NUMBER_TYPE_MOBILE = 2;
	private static final int NUMBER_TYPE_FIX_DOMESTIC = 3;
	private static final int NUMBER_TYPE_FIX_INTERNATIONAL = 4;

	private static final int FLAG_UNKNOW = 0;
	private static final int FLAG_GENERIC = 1;
	private static final int FLAG_FIX_DOMESTIC = 2;
	private static final int FLAG_MOBILE = 3;
	
	private static final int VERIFY_TYPE_ADD = 1;
	private static final int VERIFY_TYPE_UPDATE = 2;
	private static final int VERIFY_TYPE_DELETE = 3;
	
	private static final int STATUS_SUCCESS = 1;
	private static final String TAG = "DatabaseUpdate";
	private static Context mContext = null;
	public static int mTotalCount = 0;
	public static int mProceedCount = 0;
	
	
	public static boolean mCancelFlag = false;
	
	/**
	 * @return the mCancelFlag
	 */
	public static boolean isCancelFlag() {
		return mCancelFlag;
	}

	/**
	 * @param mCancelFlag the mCancelFlag to set
	 */
	public static void setCancelFlag(boolean flag) {
		mCancelFlag = flag;
	}

	public static String updateDatabase(Context context, String XMLVersion, String dbVersion,
			NumberLocationUpdateData updateData) {
		mContext  = context;
		String version = null;
		if (updateData.getStatus() != STATUS_SUCCESS) {
			Log.d(TAG, "web update status error! status = "
					+ updateData.getStatus());
			return null;
		}

		if (!updateData.getXMLParseVersion().equals(XMLVersion)) {
			Log.d(TAG, "XML Parse version error! version = "
					+ updateData.getXMLParseVersion());
			return null;
		}
		if (!NumberLocationUtilities.checkDatabaseVersion(dbVersion, updateData.getDatabaseVersion())) {
			Log.d(TAG, "database Parse version error! version = "
					+ updateData.getDatabaseVersion());
			return null;
		}
		version = updateData.getDatabaseVersion();
		if (updateData.getNumberChange() != null) {
			doUpdateNumberChange(updateData.getNumberChange());
		}
		if (updateData.getDataManage() != null) {
			doUpdateDataManage(updateData.getDataManage());
		}

		return version;
	}

	private static void doUpdateDataManage(DataManageInfo dataManage) {
		if (dataManage.getDelete() != null) {
			
		}
		if (dataManage.getAdd() != null) {
			
		}
		if (dataManage.getUpdate() != null) {
			
		}
	}

	public static void updateProgress(int type,int progress) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_UPDATE_PROGRESS);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_VALUE, progress);
		mContext.startService(i);
	}
	
	public static void setProgressMax(int type, int max) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_SET_PROGRESS_MAX);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_MAX, max);
		mContext.startService(i);
	}
	
	
	private static int getTotalUpdateRecordCount(NumberChangeInfo numberChange) {
		int count = 0;
		if (numberChange.getDelete() != null) {
			DeleteCollection delete = numberChange.getDelete();
			count += delete.getCount();
		}
		if (numberChange.getAdd() != null) {
			AddCollection add = numberChange.getAdd();
			count += add.getCount();
		}
		if (numberChange.getUpdate() != null) {
			UpdateCollection update = numberChange.getUpdate();
			count += update.getCount();
		}
		return count;
	}
	
	private static void doUpdateNumberChange(NumberChangeInfo numberChange) {
		NumberLocationDatabaseHelper mdbHelper = NumberLocationDatabaseHelper.getInstance(mContext);
		SQLiteDatabase database_number_location = mdbHelper.getWritableDatabase();

		mTotalCount = getTotalUpdateRecordCount(numberChange);
		setProgressMax(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE, mTotalCount);

		database_number_location.beginTransaction();
		try {
			mProceedCount = 0;
			if (numberChange.getDelete() != null) {
				DeleteCollection delete = numberChange.getDelete();
				int count = delete.getCount();
				if (count == 0)
					return;
				ItemsArray items = delete.getItems();
				ItemInfo item = null;
				for (int i = 0; i < count; i++) {
					if (isCancelFlag())
						break;
					item = items.getItem().get(i);
					// Log.d(TAG, "number=" + item.getNumber() + "city=" +
					// item.getCity() + "province+" + item.getProvince()
					// + "numberType=" + item.getNumberType() + "country=" +
					// item.getCountry());
					deleteDatabaseNumberInfo(item);
					mProceedCount++;
					updateProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE, mProceedCount);
					if (mProceedCount % NumberLocationConst.MAX_DATABASE_TRANSACTION_EVENT == 0) {
						database_number_location.setTransactionSuccessful();
						database_number_location.endTransaction();
						SystemClock.sleep(NumberLocationConst.MAX_DATABASE_TRANSACTION_INTERRUPT_SLEEP_TIME);
						database_number_location.beginTransaction();
					}
				}
			}
			if (numberChange.getAdd() != null) {
				AddCollection add = numberChange.getAdd();
				int count = add.getCount();
				if (count == 0)
					return;
				ItemsArray items = add.getItems();
				ItemInfo item = null;
				for (int i = 0; i < count; i++) {
					if (isCancelFlag())
						break;
					item = items.getItem().get(i);
					// Log.d(TAG, "number=" + item.getNumber() + "city=" +
					// item.getCity() + "province+" + item.getProvince()
					// + "numberType=" + item.getNumberType() + "country=" +
					// item.getCountry());
					addDatabaseNumberInfo(item);
					mProceedCount++;
					updateProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE, mProceedCount);
					if (mProceedCount % NumberLocationConst.MAX_DATABASE_TRANSACTION_EVENT == 0) {
						database_number_location.setTransactionSuccessful();
						database_number_location.endTransaction();
						SystemClock.sleep(NumberLocationConst.MAX_DATABASE_TRANSACTION_INTERRUPT_SLEEP_TIME);
						database_number_location.beginTransaction();
					}
				}
			}
			if (numberChange.getUpdate() != null) {
				UpdateCollection update = numberChange.getUpdate();
				int count = update.getCount();
				if (count == 0)
					return;
				ItemsArray items = update.getItems();
				ItemInfo item = null;
				for (int i = 0; i < count; i++) {
					if (isCancelFlag())
						break;
					item = items.getItem().get(i);
					// Log.d(TAG, "number=" + item.getNumber() + "city=" +
					// item.getCity() + "province+" + item.getProvince()
					// + "numberType=" + item.getNumberType() + "country=" +
					// item.getCountry());
					updateDatabaseNumberInfo(item);
					mProceedCount++;
					updateProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE, mProceedCount);
					if (mProceedCount % NumberLocationConst.MAX_DATABASE_TRANSACTION_EVENT == 0) {
						database_number_location.setTransactionSuccessful();
						database_number_location.endTransaction();
						SystemClock.sleep(NumberLocationConst.MAX_DATABASE_TRANSACTION_INTERRUPT_SLEEP_TIME);
						database_number_location.beginTransaction();
					}
				}
			}
			database_number_location.setTransactionSuccessful();
		} finally {
			database_number_location.endTransaction();
		}

	}

	private static boolean deleteDatabaseNumberInfo(ItemInfo item) {
		
		boolean ret = false;
		
		if (verifyNumberInfo(item,VERIFY_TYPE_DELETE) == false)
			return false;
		
		String number = item.getNumber();
		
		int numberType = getNumberType(number);
		long city_id = 0;//0 means not available
		
		if (numberType == NUMBER_TYPE_FIX_DOMESTIC) {
			ret = updateNumber_0(number, city_id);
		} else if (numberType == NUMBER_TYPE_FIX_INTERNATIONAL) {
			ret = updateNumber_00(number, city_id);
		} else if (numberType == NUMBER_TYPE_MOBILE) {
			ret = updateNumber_1xx(number, city_id);
		}
		return ret;
	}

	private static boolean updateDatabaseNumberInfo(ItemInfo item) {
		boolean ret = false;

		if (verifyNumberInfo(item,VERIFY_TYPE_UPDATE) == false)
			return false;

		long province_id = -1;
		long city_id = -1;
		long country_id = -1;
		
		String number = item.getNumber();
		int numberType = getNumberType(number);
		if ((numberType == NUMBER_TYPE_FIX_DOMESTIC)||(numberType == NUMBER_TYPE_MOBILE)) {
			String city = item.getCity();
			String province = item.getProvince();

			if ((province_id = getProvinceIdByName(province)) == -1) {
				province_id = insertProvince(province);
			}

			if ((city_id = getCityIdByName(province_id, city)) == -1) {
				city_id = insertCity(province_id, city,numberType);

			}
		}else if(numberType == NUMBER_TYPE_FIX_INTERNATIONAL){
			String country = item.getCountry();
			if ((country_id = getCountryIdByName(country)) == -1) {
				country_id = insertCountry(country);
			}
		}
		if (numberType == NUMBER_TYPE_FIX_DOMESTIC) {
			ret = updateNumber_0(number, city_id);
		} else if (numberType == NUMBER_TYPE_FIX_INTERNATIONAL) {
			ret = updateNumber_00(number, country_id);
		} else if (numberType == NUMBER_TYPE_MOBILE) {
			ret = updateNumber_1xx(number, city_id);
		}
		return ret;
	}
	
	private static boolean addDatabaseNumberInfo(ItemInfo item) {
		boolean ret = false;
		
		if (verifyNumberInfo(item,VERIFY_TYPE_ADD) == false)
			return false;
		
		long province_id = -1;
		long city_id = -1;
		long country_id = -1;
		
		String number = item.getNumber();
		int numberType = getNumberType(number);
		if ((numberType == NUMBER_TYPE_FIX_DOMESTIC)||(numberType == NUMBER_TYPE_MOBILE)) {
			String city = item.getCity();
			String province = item.getProvince();
			
			if ((province_id = getProvinceIdByName(province)) == -1) {
				province_id = insertProvince(province);
			}
			
			if ((city_id = getCityIdByName(province_id, city)) == -1) {
				city_id = insertCity(province_id, city, numberType);
				
			}
		}else if(numberType == NUMBER_TYPE_FIX_INTERNATIONAL){
			String country = item.getCountry();
			if ((country_id = getCountryIdByName(country)) == -1) {
				country_id = insertCountry(country);
			}
		}
		if (numberType == NUMBER_TYPE_FIX_DOMESTIC) {
			ret = updateNumber_0(number, city_id);
		} else if (numberType == NUMBER_TYPE_FIX_INTERNATIONAL) {
			ret = updateNumber_00(number, country_id);
		} else if (numberType == NUMBER_TYPE_MOBILE) {
			ret = updateNumber_1xx(number, city_id);
		}
		return ret;
	}
	
	private static boolean updateNumber_0(String number, long cityId) {
		int city_id = (int) cityId;
		ContentValues values = new ContentValues();
		values.put(number_0Columns.CITY_ID, city_id);
		char number_0Begin = number.charAt(1);
		String rowId = null;
		if(number_0Begin>'2')
			rowId = number.substring(1, 4);
		else
		    rowId = number.substring(1, 3);
		Uri numberUri = Uri.parse("content://" + NUMBER_LOCATION_PROVIDER + "/"
				+ "number_0" + "/" + rowId);
		ContentResolver cr = mContext.getContentResolver();
		int count = cr.update(numberUri, values, null, null);
		if (count != 1) {
			Log.v(TAG, "update error, update count = " + count);
			return false;
		}
		return true;
	}
	
	private static boolean updateNumber_00(String number, long cityId) {
		int city_id = (int) cityId;
		ContentValues values = new ContentValues();
		values.put(number_0Columns.CITY_ID, city_id);
		char number_0Begin = number.charAt(1);
		String rowId = null;
		if(number_0Begin>2)
			rowId = number.substring(1, 4);
		else
		    rowId = number.substring(1, 3);
		
		Uri numberUri = Uri.parse("content://" + NUMBER_LOCATION_PROVIDER + "/"
				+ "number_00" + "/" + rowId);
		ContentResolver cr = mContext.getContentResolver();
		int count = cr.update(numberUri, values, null, null);
		if (count != 1) {
			Log.v(TAG, "update error, update count = " + count);
			return false;
		}
		return true;
	}

	private static boolean updateNumber_1xx(String number, long cityId) {
		int city_id = (int) cityId;
		ContentValues values = new ContentValues();
		values.put(number_1xxColumns.CITY_ID, city_id);
		String uriPrefix = number.substring(0, 3);
		String rowId = number.substring(3, 7);
		
		Uri numberUri = Uri.parse("content://" + NUMBER_LOCATION_PROVIDER + "/"
				+ "number_" + uriPrefix + "/" + rowId);

		ContentResolver cr = mContext.getContentResolver();
		int count = cr.update(numberUri, values, null, null);
		if (count != 1) {
			Log.v(TAG, "update error, update count = " + count);
			return false;
		}
		return true;
	}




	private static long insertCity(long provinceId, String city, int type) {
		
		int flag = FLAG_UNKNOW;
		
		int province_id = (int) provinceId;
		ContentValues values = new ContentValues();
		values.put(cityColumns.CITY, city);
		values.put(cityColumns.PROVINCE_ID, province_id);
		if(type == NUMBER_TYPE_FIX_DOMESTIC){
			flag = FLAG_FIX_DOMESTIC;
		}else if(type == NUMBER_TYPE_MOBILE){
			flag = FLAG_MOBILE;
		}else{
			flag = FLAG_GENERIC;
		}
		values.put(cityColumns.FLAG, flag);
		Uri cityUri = NumberLocationProviderConst.CITY_URI;
		ContentResolver cr = mContext.getContentResolver();
		Uri newAddUri = cr.insert(cityUri, values);
		return ContentUris.parseId(newAddUri);
	}

	private static long insertProvince(String province) {
		ContentValues values = new ContentValues();
        values.put(provinceColumns.PROVINCE, province);
        Uri provinceUri = NumberLocationProviderConst.PROVINCE_URI;
        ContentResolver cr = mContext.getContentResolver();
        Uri newAddUri = cr.insert(provinceUri, values);
		return ContentUris.parseId(newAddUri);
	}
	
	private static long insertCountry(String country) {
		ContentValues values = new ContentValues();
		values.put(countryColumns.COUNTRY, country);
		Uri countryUri = NumberLocationProviderConst.COUNTRY_URI;
		ContentResolver cr = mContext.getContentResolver();
		Uri newAddUri = cr.insert(countryUri, values);
		return ContentUris.parseId(newAddUri);
	}


	private static int getCityIdByName(long provinceId, String city) {
		int province_id = (int) provinceId;
		final String[] CITY_PROJECTION = new String[] { cityColumns.ID };
		final String CITY_WHERE = cityColumns.CITY + " like " + "\"" + city + "\""
				+ " AND " + cityColumns.PROVINCE_ID + "=" + province_id;
		ContentResolver cr = mContext.getContentResolver();
		Uri cityUri = NumberLocationProviderConst.CITY_URI;
		int city_id = -1;
		Cursor cursor = null;
		try {
			cursor = cr.query(cityUri, CITY_PROJECTION, CITY_WHERE, null, null);
			if (cursor == null || !cursor.moveToFirst()) {
				Log
						.d(TAG, "Couldn't find " + cityColumns.ID
								+ " in city table");
				return city_id;
			}
			city_id = cursor.getInt(0);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return city_id;
	}
	
	private static int getProvinceIdByName(String province) {
		
		final String[] PROVINCE_PROJECTION = new String[] { provinceColumns.ID };
		String PROVINCE_WHERE = "province like \""+ province+"\"";

		ContentResolver cr = mContext.getContentResolver();
		Uri provinceUri = NumberLocationProviderConst.PROVINCE_URI;
		int province_id = -1;
		Cursor cursor = null;
		try {
			cursor = cr.query(provinceUri, PROVINCE_PROJECTION, PROVINCE_WHERE,
					null, null);
			if (cursor == null || !cursor.moveToFirst()) {
				Log.d(TAG, "Couldn't find " + provinceColumns.ID
						+ " in province table");
				return province_id;
			}
			province_id = cursor.getInt(0);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return province_id;
	}
	
	private static int getCountryIdByName(String country) {
		
		final String[] COUNTRY_PROJECTION = new String[] { countryColumns.ID };
		String COUNTRY_WHERE = "country like \""+ country+"\"";
		
		ContentResolver cr = mContext.getContentResolver();
		Uri countryUri = NumberLocationProviderConst.COUNTRY_URI;
		int country_id = -1;
		Cursor cursor = null;
		try {
			cursor = cr.query(countryUri, COUNTRY_PROJECTION, COUNTRY_WHERE,
					null, null);
			if (cursor == null || !cursor.moveToFirst()) {
				Log.d(TAG, "Couldn't find " + countryColumns.ID
						+ " in country table");
				return country_id;
			}
			country_id = cursor.getInt(0);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return country_id;
	}

	private static int getNumberType(String number) {
		int numberType = NUMBER_TYPE_UNKNOW;
		if (number.startsWith("00", 0)) {
			if (number.length()> 2 && number.length()< 7)
				numberType = NUMBER_TYPE_FIX_INTERNATIONAL;
			else
				numberType = NUMBER_TYPE_UNKNOW;
		} else if (number.startsWith("0", 0)) {
			if (number.length()> 2 && number.length()< 5)
				numberType = NUMBER_TYPE_FIX_DOMESTIC;
			else
				numberType = NUMBER_TYPE_UNKNOW;
		} else if (number.startsWith("1", 0)) {
			if (number.length() != 7)
				numberType = NUMBER_TYPE_UNKNOW;
			else
				numberType = NUMBER_TYPE_MOBILE;
		} else {
			numberType = NUMBER_TYPE_UNKNOW;
		}
		return numberType;
	}

	private static boolean verifyNumberInfo(ItemInfo item, int verifyType) {

		String number = item.getNumber();
		int type = NUMBER_TYPE_UNKNOW;
		if (number == null)
			return false;
		type = getNumberType(number);
		if (type == NUMBER_TYPE_UNKNOW)
			return false;
		if (VERIFY_TYPE_DELETE == verifyType)
			return true;
		if ((type == NUMBER_TYPE_MOBILE) || (type == NUMBER_TYPE_FIX_DOMESTIC)) {
			if ((item.getCity() != null) && (item.getProvince() != null))
				return true;
		} else if (type == NUMBER_TYPE_FIX_INTERNATIONAL) {
			if (item.getCountry() != null)
				return true;
		}
		return false;
	}



}
