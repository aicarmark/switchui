package com.motorola.numberlocation;

import java.io.File;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.motorola.numberlocation.INumberLocationCallback;
import com.motorola.numberlocation.INumberLocationService;
import com.motorola.numberlocation.R;
import com.motorola.numberlocation.NumberLocationProvider.cityColumns;
import com.motorola.numberlocation.NumberLocationProvider.countryColumns;
import com.motorola.numberlocation.NumberLocationProvider.number_00Columns;
import com.motorola.numberlocation.NumberLocationProvider.number_0Columns;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
//import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
//import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
//import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import java.lang.Math;


public class NumberLocationService extends Service {
    private final String TAG = "NumberLocationService";
    

    private final String CHINESE_CALLING_CODES_PREFIX = "0086";
	
	private Looper mServiceLooper;

	private ServiceHandler mServiceHandler; 
	
//	private WakeLock mWakeLock = null;
        
    private String [] municipalityNameArray = null;
    UpgradeDatabaseTask mUpdateTask = null;
    
    public static class CountryNameTraditionalConvertMapping{
    	String inputName;
    	String mappingName;
        public CountryNameTraditionalConvertMapping(String inputName,
				String mappingName) {
			super();
			this.inputName = inputName;
			this.mappingName = mappingName;
		}
    }
    
    private CountryNameTraditionalConvertMapping [] countryNameTraditionalConvertMappingArray = null;
   
	public String convertCountryName(String name) {
		String outputName = null;
		for (int i = 0; i < countryNameTraditionalConvertMappingArray.length; i++) {
			if (countryNameTraditionalConvertMappingArray[i].inputName
					.equals(name)) {
				outputName = countryNameTraditionalConvertMappingArray[i].mappingName;
				break;
			}
		}
		if (null == outputName)
			outputName = name;
		return outputName;
	}
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

    
	private final String TABLE_NAME_PREFIX = "number_";
	private final String DATABASE_NUBMER_LOCATION_FILENAME = "number_location.db";
	private SQLiteDatabase database_number_location;

	private NumberLocationService mNumberLocationService = this;
	
	private String mApplicationRootPath;
	private String mApplicationDatabasePath;
	
    public SQLiteDatabase getDatabase_number_location() {
		return database_number_location;
	}

	public void setDatabase_number_location(SQLiteDatabase databaseNumberLocation) {
		database_number_location = databaseNumberLocation;
	}

	public NumberLocationService getmNumberLocationService() {
		return mNumberLocationService;
	}

	public void setmNumberLocationService(
			NumberLocationService mNumberLocationService) {
		this.mNumberLocationService = mNumberLocationService;
	}

	public String getmApplicationRootPath() {
		return mApplicationRootPath;
	}

	public void setmApplicationRootPath(String mApplicationRootPath) {
		this.mApplicationRootPath = mApplicationRootPath;
	}

	public String getmApplicationDatabasePath() {
		return mApplicationDatabasePath;
	}

	public void setmApplicationDatabasePath(String mApplicationDatabasePath) {
		this.mApplicationDatabasePath = mApplicationDatabasePath;
	}

	public String getDATABASE_NUBMER_LOCATION_FILENAME() {
		return DATABASE_NUBMER_LOCATION_FILENAME;
	}

	private RemoteCallbackList<INumberLocationCallback> mCallbacks = new RemoteCallbackList<INumberLocationCallback>();  

    private INumberLocationService.Stub mBinder = new INumberLocationService.Stub() {  
    	   
        @Override  
        public void unregisterCallback(INumberLocationCallback cb){  
            if(cb != null) {  
                mCallbacks.unregister(cb);  
            }  
        }  
  
        @Override  
        public void registerCallback(INumberLocationCallback cb){  
            if(cb != null) {  
                mCallbacks.register(cb);  
            }  
        }
        
    	private int getCityIdByName(String city) {
    		final String[] CITY_PROJECTION = new String[] { cityColumns.ID,cityColumns.CITY };
    		final String CITY_WHERE = cityColumns.CITY + " like \"%" + city + "%\"" + " and " + "(flag=1 or flag=2)";
    		ContentResolver cr = getApplicationContext().getContentResolver();
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
    			if(cursor.getCount()>1){
    				cursor.moveToFirst();
    				do{
    					if(checkValidData(city,cursor.getString(1))){
    						city_id = cursor.getInt(0);
    						break;
    					}
    				}while(cursor.moveToNext());
    			}else if(cursor.getCount()==1){
    				if(checkValidData(city,cursor.getString(1)))
    					city_id = cursor.getInt(0);
    			}
    		} catch (SQLiteException se) {
				se.printStackTrace();
				return -1;
			} catch (CursorIndexOutOfBoundsException ciobe) {
				ciobe.printStackTrace();
				return -1;
			} finally {
    			if (cursor != null) {
    				cursor.close();
    			}
    		}
    		return city_id;
    	}
    	
    	private boolean checkValidData(String query, String result) {
			if(query==null||result==null)
				return false;
			if(query.equals(result))
				return true;
			if(result.contains("/")){
				String [] resultArray = result.split("/");
				for(int i=0;i<resultArray.length;i++){
					if(resultArray[i].equals(query))
						return true;
				}
			}
			return false;
		}

		private int getNumberByCityId(long cityid) {
    		final String[] NUMBER_0_PROJECTION = new String[] { number_0Columns.ROWID };
    		final String NUMBER_0_WHERE = number_0Columns.CITY_ID + "=" + cityid;
    		ContentResolver cr = getApplicationContext().getContentResolver();
    		Uri cityUri = NumberLocationProviderConst.NUMBER_0_URI;
    		int number = -1;
    		Cursor cursor = null;
    		try {
    			cursor = cr.query(cityUri, NUMBER_0_PROJECTION, NUMBER_0_WHERE, null, null);
    			if (cursor == null || !cursor.moveToFirst()) {
    				Log
    				.d(TAG, "Couldn't find " + Integer.toString((int) cityid)
    						+ " in number 0 table");
    				return number;
    			}
    			number = cursor.getInt(0);
    		} finally {
    			if (cursor != null) {
    				cursor.close();
    			}
    		}
    		return number;
    	}
    	
      
        @Override 
    	public String getNumberByCityLocation(String name) {
    		if(TextUtils.isEmpty(name))
    			return null;
        	long city_id = -1;
    		if ((city_id = getCityIdByName(name)) == -1) {
				Log
				.d(TAG, "getNumberByCityLocation Couldn't find " + name
						+ " in city table");
				return null;
    		}
    		String number = null;
    		int No = getNumberByCityId(city_id);
    		if(No == -1)
    			return null;
    		number = Integer.toString(No);
        	return number;
    	}
        
    	private int getCountryIdByName(String country) {

    		String[] COUNTRY_PROJECTION = null; 
    		String COUNTRY_WHERE = null; 
    		
			if (NumberLocationUtilities.isChinese(country)) {
				COUNTRY_PROJECTION = new String[] { countryColumns.ID, countryColumns.COUNTRY };
				COUNTRY_WHERE = countryColumns.COUNTRY + " like " + "\"%" + country + "%\"";
			} else {
				COUNTRY_PROJECTION = new String[] { countryColumns.ID, countryColumns.COUNTRY_EN };
				COUNTRY_WHERE = countryColumns.COUNTRY_EN + " like " + "\"%" + country + "%\"";
			}
    		Log.d(TAG, "getCountryIdByName country =" + country);
    		ContentResolver cr = getApplicationContext().getContentResolver();
    		Uri countryUri = NumberLocationProviderConst.COUNTRY_URI;
    		int country_id = -1;
    		Cursor cursor = null;
    		try {
    			cursor = cr.query(countryUri, COUNTRY_PROJECTION, COUNTRY_WHERE, null, null);
    			if (cursor == null || !cursor.moveToFirst()) {
    				Log
    						.d(TAG, "Couldn't find " + countryColumns.ID
    								+ " in country table");
    				return country_id;
    			}
    			if(cursor.getCount()>1){
    				cursor.moveToFirst();
    				do{
    					if(checkValidData(country,cursor.getString(1))){
    						country_id = cursor.getInt(0);
    						break;
    					}
    				}while(cursor.moveToNext());
    			}else if(cursor.getCount()==1){
    				if(checkValidData(country,cursor.getString(1)))
    					country_id = cursor.getInt(0);
    			}
			} catch (SQLiteException se) {
				se.printStackTrace();
				return -1;
			} catch (CursorIndexOutOfBoundsException ciobe) {
				ciobe.printStackTrace();
				return -1;
			}finally {
    			if (cursor != null) {
    				cursor.close();
    			}
    		}
    		return country_id;
    	}
    	
    	private int getNumberByCountryId(long countryid) {
    		final String[] NUMBER_00_PROJECTION = new String[] { number_00Columns.ROWID };
    		final String NUMBER_00_WHERE = number_00Columns.COUNTRY_ID + "=" + countryid;
    		ContentResolver cr = getApplicationContext().getContentResolver();
    		Uri countryUri = NumberLocationProviderConst.NUMBER_00_URI;
    		int number = -1;
    		Cursor cursor = null;
    		try {
    			cursor = cr.query(countryUri, NUMBER_00_PROJECTION, NUMBER_00_WHERE, null, null);
    			if (cursor == null || !cursor.moveToFirst()) {
    				Log
    				.d(TAG, "Couldn't find " + Integer.toString((int) countryid)
    						+ " in country table");
    				return number;
    			}
    			number = cursor.getInt(0);
    		} finally {
    			if (cursor != null) {
    				cursor.close();
    			}
    		}
    		return number;
    	}
        
        
        @Override 
        public String getNumberByCountryLocation(String name) {
    		if(TextUtils.isEmpty(name))
    			return null;
        	long country_id = -1;
    		if ((country_id = getCountryIdByName(convertCountryName(name))) == -1) {
				Log
				.d(TAG, "getNumberByCountryLocation Couldn't find " + name
						+ " in country table");
				return null;
    		}
    		String number = null;
    		number = Integer.toString(getNumberByCountryId(country_id));
        	return number;
        }
        
        
    	public String getLocationByNumber(String number) {
    		if(TextUtils.isEmpty(number))
    			return null;
			String tableName = null;
			int recNo = 0;
			int numberOfPossible = 0;
			int[] possibleRecNo = null;
			Cursor cursor = null;
			boolean internationalFlag = false;
    		boolean ChineseCityFlag = false;
    		try {
    			if (number.startsWith("+")) {
    				number = "00" + number.substring(1);
    			}
    			if(number.startsWith(CHINESE_CALLING_CODES_PREFIX)){
    				ChineseCityFlag = true;
    			}
    			if(number.startsWith("00", 0)){//should be international number.
    				tableName=TABLE_NAME_PREFIX+"00";
    				numberOfPossible = Math.min(4, number.length()-2);
    				if(numberOfPossible == 0)
    					return null;
    				possibleRecNo = new int [numberOfPossible];
					for (int i = 0; i < numberOfPossible; i++) {
						possibleRecNo[i] = Integer.valueOf(number.substring(2, numberOfPossible + 2 - i));
					}
					internationalFlag = true;
    			}else if(number.startsWith("0", 0)){//should be fix number.
    				tableName=TABLE_NAME_PREFIX+"0";
    				if(number.length()>=2){
    					int tmpJudgeFixCity=Integer.parseInt(String.valueOf(number.charAt(1)));
    					if(((tmpJudgeFixCity==1||tmpJudgeFixCity==2))&&(number.length()>=3)){//should be two number city
    						recNo=Integer.valueOf(number.substring(1, 3));
    					}else if(((tmpJudgeFixCity>2||tmpJudgeFixCity<10))&&(number.length()>=4)){//should be three number city;
    						recNo=Integer.valueOf(number.substring(1, 4));
    					}else{
    						return null;//wrong number!
    					}
    				}else{
    					return null;
    				}
    			}else{//mobile number
    				if(number.length()<7)
    					return null;
    				tableName=TABLE_NAME_PREFIX+number.substring(0, 3);
    				recNo=Integer.valueOf(number.substring(3, 7))+1;//for match rowid.
    			}
    		} catch (SQLiteException se) {
				se.printStackTrace();
				return null;
			} catch (CursorIndexOutOfBoundsException ciobe) {
				ciobe.printStackTrace();
				return null;
			} catch (NumberFormatException e) {
    			e.printStackTrace();
    			return null;//wrong format!
    		}
    		String result = "";
			try {
            	if(!NumberLocationUtilities.isExistTableInNumberLocationDatabase(database_number_location,tableName))
            		return result.trim();
				if (internationalFlag == false) {// not use provider for improve query performance
					String sql = "select province.province,city.city from (city join province on province_id=province.id) where city._id=(select city_id from "
							+ tableName
							+ " where rowid="
							+ Integer.toString(recNo) + ");";
					cursor = database_number_location
							.rawQuery(sql, null);
	    			if (cursor == null || !cursor.moveToFirst()) {
	    				Log.d(TAG, "Couldn't find record in city table");
                        return result.trim();
	    			}
					if (cursor.getCount() > 0) {
						String strProvince = translateChinese(cursor, "province");
						String strCity = translateChinese(cursor, "city");
						boolean checkMunicipalityFlag = false;
						if(strProvince.equals(strCity)){
							checkMunicipalityFlag = checkMunicipality(strCity);
						}
						if(checkMunicipalityFlag)
							result = strCity;
						else
							result = strProvince + strCity;
					}
					return result.trim();
				} else {
					for (int i = 0; i < numberOfPossible; i++) {
                        String sql = null;
						if (NumberLocationUtilities.isSystemLanguageChinese()) {
							sql = "select country from country where _id=(select country_id from " + tableName
									+ " where rowid=" + Integer.toString(possibleRecNo[i]) + ");";
						} else {
							sql = "select country_en from country where _id=(select country_id from " + tableName
									+ " where rowid=" + Integer.toString(possibleRecNo[i]) + ");";
						}
						cursor = database_number_location.rawQuery(sql, null);
						if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) {
							if (cursor != null)
								cursor.close();
							continue;
						}
						if (cursor.getCount() > 0) {
							cursor.moveToFirst();
							String strCountry = null;
							if (NumberLocationUtilities.isSystemLanguageChinese()) {
								strCountry = translateChinese(cursor, "country");
							} else {
								strCountry = translateChinese(cursor, "country_en");
							}
							result = strCountry;
							if (ChineseCityFlag) {
								String nextQueryNumber = number.substring(4);
								String pattern = "(10|([2-9][0-9]))[0-9]*";
								String chinaCity = null;
								if(nextQueryNumber.matches(pattern)){
									nextQueryNumber = "0"+nextQueryNumber;
									chinaCity = getLocationByNumber(nextQueryNumber);
									if (null != chinaCity)
										result = result + chinaCity;
								}else if(nextQueryNumber.startsWith("0")){
								}else{
									chinaCity = getLocationByNumber(nextQueryNumber);
									if (null != chinaCity)
										result = result + chinaCity;
								}
							}
							break;
						}
					}
					return result.trim();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;// search database meet error!
			}finally{
				if(cursor!=null)
					cursor.close();
			}
    	}

        
		private boolean checkMunicipality(String strCity) {
			for (int i = 0; i < municipalityNameArray.length; i++) {
				if (municipalityNameArray[i].equals(strCity.trim()))
					return true;
			}
			return false;
		}
		


		@Override
		public int updateNumberLocationDatabase() throws RemoteException {
			executeUpdateNumberLocationDatabase();
			return 0;
		}
		
		@Override
		public void cancelUpdateNumberLocationDatabase() throws RemoteException {
			mUpdateTask.cancel(true);
			return;
		}
    };

    

	@Override
	public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return mBinder; 
	}

	@Override
	public void onCreate() {
		super.onCreate();
        Log.v(TAG, "NumberLocationService onCreate");
		mApplicationDatabasePath = getApplicationContext().
		getDatabasePath(DATABASE_NUBMER_LOCATION_FILENAME).getParent();
		File databaseDir = new File(mApplicationDatabasePath);
		mApplicationRootPath = databaseDir.getParent();
		
		database_number_location = openNumberLocationDatabase();
        if(database_number_location!=null)
        	this.sendBroadcast(new Intent("com.motorola.numberlocation.database"));//fwr687
//        mHandler.sendEmptyMessageDelayed(0, 1000);
		
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.
		HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		mServiceLooper = thread.getLooper();
		if (mServiceLooper != null) {
		    mServiceHandler = new ServiceHandler(mServiceLooper);
		}
		
//		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
//		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
//		Log.v(TAG, "mWakeLock.newWakeLock()###");
//		mWakeLock.setReferenceCounted(false);
		
		municipalityNameArray = getResources().getStringArray(R.array.municipalityNameArray);
		
        countryNameTraditionalConvertMappingArray=new CountryNameTraditionalConvertMapping[3];
        countryNameTraditionalConvertMappingArray[0]= new CountryNameTraditionalConvertMapping(
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_cn_1),
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_cn_2));
        countryNameTraditionalConvertMappingArray[1]= new CountryNameTraditionalConvertMapping(
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_cn_en_1),
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_cn_en_2));
        countryNameTraditionalConvertMappingArray[2]= new CountryNameTraditionalConvertMapping(
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_usa_1),
	    		getResources().getString(R.string.string_countryNameTraditionalConvertMapping_usa_2));
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "###NumberLocationService onStartCommand.");
		while (mServiceHandler == null) {
			synchronized (this) {
				try {
					wait(100);
				} catch (InterruptedException e) {
				}
			}
		}

		if (intent == null) {
			Log.e(TAG, "###Intent is null in onStartCommand: ",
					new NullPointerException());
			return Service.START_NOT_STICKY;
		}

		int retCode = super.onStartCommand(intent, flags, startId);
		if (retCode == START_STICKY) {
			if (intent != null) {
				Log.v(TAG, "Starting #" + startId + ": " + intent.getAction());
//				acquireWakelock();
				if (mServiceHandler != null) {
					Message msg = mServiceHandler.obtainMessage();
					msg.arg1 = startId;
					msg.obj = intent;
					mServiceHandler.sendMessage(msg);
				}
			}
		}

		return retCode;
	}



	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		/**
		 * Handle incoming requests.
		 */
		@Override
		public void handleMessage(Message msg) {
			int serviceId = msg.arg1;
			Intent intent = (Intent) msg.obj;
			String action = intent.getAction();
			Bundle extras = null;
			int status = NumberLocationConst.STATUS_SUCCESS;
			int type = NumberLocationConst.PROGRESS_TYPE_NULL;
			int progressVal = 0;
			int max = 0;
			
			Log.d(TAG, "###handleMessage get intent " + action);
			
			try {
				if (NumberLocationConst.ACTION_AUTO_CHECK_SETTING_CHANGED.equals(action)) {
					handleAutoCheckSettingChanged(intent, serviceId);
				} else if (NumberLocationConst.ACTION_AUTO_UPDATE.equals(action)) {
					handleAutoUpdate(intent, serviceId);
				} else if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
					handleTimeChanged(intent, serviceId);
				} else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
					handleBootCompleted(serviceId);
				} else if (NumberLocationConst.ACTION_CALL_BACK_SHOW_RESULT.equals(action)) {
					extras = intent.getExtras();
					if (extras != null) {
						status = extras.getInt(NumberLocationConst.EXTRA_PARAM_RESULT_STATUS,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_RESULT_STATUS);
						onShowResult(status);
					}
				} else if (NumberLocationConst.ACTION_CALL_BACK_SHOW_PROGRESS.equals(action)) {
					extras = intent.getExtras();
					if (extras != null) {
						type = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_TYPE);
						onShowProgress(type);
					}
				} else if (NumberLocationConst.ACTION_CALL_BACK_UPDATE_PROGRESS.equals(action)) {
					extras = intent.getExtras();
					if (extras != null) {
						type = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_TYPE);
						progressVal = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_VALUE,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_VALUE);
						onUpdateProgress(type,progressVal);
					}
				} else if (NumberLocationConst.ACTION_CALL_BACK_DISMISS_PROGRESS.equals(action)) {
					extras = intent.getExtras();
					if (extras != null) {
						type = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_TYPE);
						onDismissProgress(type);
					}
				} else if (NumberLocationConst.ACTION_CALL_BACK_SET_PROGRESS_MAX.equals(action)) {
					extras = intent.getExtras();
					if (extras != null) {
						type = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_TYPE);
						max = extras.getInt(NumberLocationConst.EXTRA_PARAM_PROGRESS_MAX,
								NumberLocationConst.DEFAULT_VALUE_OF_PARAM_PROGRESS_MAX);
						onSetProgressMax(type,max);
					}
				}
			} finally {
//				releaseWakelock();
				// always stop the service after process the message
				// may slow the start, but may reduce some system load.
				Log.d(TAG, "###stopSelf  serviceId=" + serviceId);
				stopSelf(serviceId);
			}
		}
	}

	private void handleBootCompleted(int serviceId) {
		Log.v(TAG, "handleBootCompleted###");
		boolean autoUpdateEnabel = false;
		long interval = 0;

		SharedPreferences prefs = getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, 0);
		autoUpdateEnabel = prefs.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);
		if(autoUpdateEnabel == false)
			return;
		String intervalStr = prefs.getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M);
		interval = Integer.valueOf(intervalStr)*NumberLocationConst.AUTO_UPDATE_INTERVAL_DEFAULT;
		// always deal with the auto-update
		NumberLocationAutoUpdate au = new NumberLocationAutoUpdate(this);
		au.setNextUpdateTime(interval);
		au.startTimer();
	}
	
	private void handleAutoUpdate(Intent intent,int serviceId) {
		Log.v(TAG, "handleAutoUpdate###");
		boolean autoUpdateEnabel = false;
		long interval = 0;
		
		SharedPreferences prefs = getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, 0);
		autoUpdateEnabel = prefs.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);
		if(autoUpdateEnabel == false)
			return;

		if (isServiceBusy()) {
			String intervalStr = prefs.getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M);
			interval = Integer.valueOf(intervalStr)*NumberLocationConst.AUTO_UPDATE_INTERVAL_DEFAULT;
			NumberLocationAutoUpdate au = new NumberLocationAutoUpdate(this);
			au.setNextUpdateTime(interval);
			au.startTimer();
			return;
		}

		Log.i(TAG, "finish auto update to start the client initialized session###");
		executeUpdateNumberLocationDatabase();
		Log.i(TAG, "finish auto update to start the client initialized session###");
	} 
    
    private boolean isServiceBusy() {
		// TODO Auto-generated method stub
		return false;
	}

	//
    // handle the auto-check setting changed which is triggered from Settings 
    //
    private void handleAutoCheckSettingChanged(Intent intent, int serviceId) {
        Log.i(TAG, "auto-check setting changed.###");
        NumberLocationAutoUpdate au = new NumberLocationAutoUpdate(this);
        Bundle extras = intent.getExtras();
        if ((extras != null) && (extras.getBoolean(NumberLocationConst.EXTRA_AUTO_UPDATE_SWITCH,NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE))) {
            au.startTimer();
        } else {
            au.cancelTimer();
        }
    }    
    //
    // handle the time changed
    //
	private void handleTimeChanged(Intent intent, int serviceId) {
		Log.i(TAG, "handle the time changed.###");
		// long curTime = System.currentTimeMillis();
		boolean autoUpdateEnabel = false;
		long interval = NumberLocationConst.AUTO_UPDATE_INTERVAL_DEFAULT;

		SharedPreferences prefs = getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, 0);
		autoUpdateEnabel = prefs.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);
		if(autoUpdateEnabel == false)
			return;
		String intervalStr = prefs.getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M);
		interval = Integer.valueOf(intervalStr)*NumberLocationConst.AUTO_UPDATE_INTERVAL_DEFAULT;

		// update the auto-update alarm
		NumberLocationAutoUpdate au = new NumberLocationAutoUpdate(this);
		au.setNextUpdateTime(interval);
		au.startTimer();
	}
    
    
    
	@Override
	public void onDestroy() {
//        mHandler.removeMessages(0);
        mCallbacks.kill();
        database_number_location.close();
		Log.v(TAG, "Destroying NumberLocationService");
		mServiceLooper.quit();
//		releaseWakelock();
		super.onDestroy();
	}


/*	
	private void releaseWakelock() {
		if((mWakeLock!=null)&&(mWakeLock.isHeld())){
			mWakeLock.release();
			mWakeLock = null;
			Log.v(TAG, "mWakeLock.release()###");
		}else if(mWakeLock!=null){
			mWakeLock = null;
		}
	}
*/	
	/*	
	public void acquireWakelock() {
		if((mWakeLock!=null)&&(!mWakeLock.isHeld())){
			mWakeLock.acquire();
			Log.v(TAG, "mWakeLock.acquire()###");
		}else{
			Log.v(TAG, "mWakeLock has been held or unavilable. acquire failed.###");
			if(mWakeLock == null){
				PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
				mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
				Log.v(TAG, "mWakeLock.newWakeLock()###");
				mWakeLock.setReferenceCounted(false);
				mWakeLock.acquire();
				Log.v(TAG, "mWakeLock.acquire()###");
			}
		}
	}
*/		
/*	
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
*/
	public void onShowResult(int result) {
		int i = mCallbacks.beginBroadcast();
		while (i > 0) {
			i--;
			try {
				mCallbacks.getBroadcastItem(i).showResult(result);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mCallbacks.finishBroadcast();
	}

	public void onShowProgress(int type) {
		int i = mCallbacks.beginBroadcast();
		while (i > 0) {
			i--;
			try {
				mCallbacks.getBroadcastItem(i).showProgress(type);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	public void onUpdateProgress(int type,int progress) {
		int i = mCallbacks.beginBroadcast();
		while (i > 0) {
			i--;
			try {
				mCallbacks.getBroadcastItem(i).updateProgress(type,progress);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	public void onDismissProgress(int type) {
		int i = mCallbacks.beginBroadcast();
		while (i > 0) {
			i--;
			try {
				mCallbacks.getBroadcastItem(i).dismissProgress(type);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	public void onSetProgressMax(int type, int max) {
		int i = mCallbacks.beginBroadcast();
		while (i > 0) {
			i--;
			try {
				mCallbacks.getBroadcastItem(i).setProgressMax(type,max);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mCallbacks.finishBroadcast();
	}
	
    
	public String translateChinese(Cursor cur, String columnName) {
		String sn = null;
		try {
			int column = cur.getColumnIndex(columnName);
			byte bytes[] = cur.getBlob(column);
			sn = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			new AlertDialog.Builder(this).setTitle("result").setMessage("wrong encode!")
			.setPositiveButton("close", null).show();
			e.printStackTrace();
		}
		return sn;
	}
	

    public static void unZipStream(InputStream fileInputStream, String outPathString)throws Exception {  
    	android.util.Log.v("XZip", "UnZipFolder(String, String)");  
    	java.util.zip.ZipInputStream inZip = new java.util.zip.ZipInputStream(fileInputStream);  
    	java.util.zip.ZipEntry zipEntry;  
    	String szName = "";  
    	
    	while ((zipEntry = inZip.getNextEntry()) != null) {  
    		szName = zipEntry.getName();  
    		if (zipEntry.isDirectory()) {  
    			// get the folder name of the widget  
    			szName = szName.substring(0, szName.length() - 1);  
    			java.io.File folder = new java.io.File(outPathString + java.io.File.separator + szName);  
                if(false == folder.mkdirs())
                    break;  
    		} else {  
    			java.io.File file = new java.io.File(outPathString + java.io.File.separator + szName);  
                if(false == file.createNewFile())
                    break;  
    			// get the output stream of the file  
    			java.io.FileOutputStream out = new java.io.FileOutputStream(file);  
    			int len;  
    			byte[] buffer = new byte[BUFF_SIZE];  
    			// read (len) bytes into buffer  
    			while ((len = inZip.read(buffer)) != -1) {  
    				// write (len) byte from buffer at the position 0  
    				out.write(buffer, 0, len);  
    				out.flush();  
    			}  
    			out.close();  
    		}  
    	}//end of while  
    	inZip.close();  
    }//end of func  
    
	private SQLiteDatabase openNumberLocationDatabase() {
		try {
			String DATABASE_NUBMER_LOCATION_PATH = getApplicationContext().
			getDatabasePath(DATABASE_NUBMER_LOCATION_FILENAME).getParent();
			
			String databaseFilename = DATABASE_NUBMER_LOCATION_PATH + "/"
					+ DATABASE_NUBMER_LOCATION_FILENAME;
			File dir = new File(DATABASE_NUBMER_LOCATION_PATH);
			if (!dir.exists())
                if(false == dir.mkdir())
                    return null;
			if (!(new File(databaseFilename)).exists()){
				InputStream is = getResources().openRawResource(
						R.raw.number_location);
                unZipStream(is, DATABASE_NUBMER_LOCATION_PATH);
			}
			SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(
					databaseFilename, null);
			return database;
        }catch (Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }

	/**
	 * 
	 */
	public void executeUpdateNumberLocationDatabase() {
		mUpdateTask = new UpgradeDatabaseTask(getApplicationContext(), mNumberLocationService, mApplicationRootPath);
		mUpdateTask.execute();
	}


    	
}
