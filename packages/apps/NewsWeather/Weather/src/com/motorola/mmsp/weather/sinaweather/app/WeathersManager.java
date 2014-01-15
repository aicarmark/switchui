package com.motorola.mmsp.weather.sinaweather.app;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;


public class WeathersManager {
	
    public static int UPDATE_RESULT_SUCCESS = 1;
    public static int UPDATE_RESULT_PART_FAILED = 2;
    public static int UPDATE_RESULT_ALL_FAILED = 3;
    
	/*** Singleton object ***/
	private static WeathersManager mInstance = null;
	
	/*** WeatherInfo Cache ***/
	private HashMap<String, WeatherInfo> mCityWeathers = null;
	/*** CityId List Cache ***/
	private ArrayList<String> mCityIds = null;

	private WeathersManager() {
	}

	private WeathersManager(Context context) {
		init(context);
	}

	public static synchronized WeathersManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new WeathersManager(context);
			mInstance.init(context);
		}
		return mInstance;
	}

	private void init(Context context) {
	    Log.d("WeatherWidget", "WeathersManager->init()");
	    //updateWeatherAndCityIdsCacheFromDB(context);
	    mCityIds = PersistenceData.getCityids(context);
	    if (mCityWeathers == null) {
	        mCityWeathers = new HashMap<String, WeatherInfo>();
	    }
	}
	
	public void clearCache() {
		mCityIds.clear();
		mCityWeathers.clear();
	}
	
	public int getCount(Context context) {
		int cityNum = 0;
		if (mCityIds == null || mCityIds.isEmpty()) {
			mCityIds = PersistenceData.getCityids(context);
		}
		if (mCityIds != null && !mCityIds.isEmpty()) {
			cityNum = mCityIds.size();
		}
		Log.d("WeatherWidget", "------ WeathersManager->getCount, cityNum = " + cityNum + "------");
		return cityNum;
	}
	
	public int getCountAlwaysFromDB(Context context) {
		int cityNum = 0;
		ArrayList<String> cityIds = PersistenceData.getCityids(context);
		
		if (cityIds != null && !cityIds.isEmpty()) {
			cityNum = mCityIds.size();
		}
		Log.d("WeatherWidget", "------ WeathersManager->getCountAlwaysFromDB, cityNum = " + cityNum);
		return cityNum;
	}
	
    public int getIndexFromCacheByCityId(Context context, String cityId) {
        int index = -1;
        //mCityIds.indexOf(cityId);
        for (int i=0; i<mCityIds.size(); i++) {
            if (cityId.equals(mCityIds.get(i))) {
                index = i;
                Log.d("WeatherWidget", "Match the city, index = " + index);
                break;
            }
        }
        return index;
    }
    
    public String getCityIdFromCacheByIndex(Context context, int index) {
        String cityId = null;
        if (mCityIds == null || mCityIds.isEmpty()) {
            mCityIds = PersistenceData.getCityids(context);
        }
        if (mCityIds != null && !mCityIds.isEmpty() && index < mCityIds.size()) {
            cityId = mCityIds.get(index);
        }
        return cityId;
    }
    
	public WeatherInfo getWeatherFromCacheByCityId(Context context, String cityId) {
		Log.d("WeatherWidget", "WeatherManager->getWeatherFromCacheByCityId begin, cityId = " + cityId);
		WeatherInfo w = null;
		if (cityId != null) {
			w = mCityWeathers.get(cityId);
			if (w != null) {
				Log.d("WeatherWidget", "weather exist in cache : "+ " temperature = " 
						+ w.mCurrent.mTemperatrue + " condition = "
						+ w.mCurrent.mCondition + " city = "
						+ w.mInfomation.mCity + " adminArea = " 
						+ w.mInfomation.mAdminArea + " cityId = " 
						+ w.mInfomation.mCityId);
			} else {
				Log.d("WeatherWidget", "weather dose not exist in cache, search in DB");
				w = WeatherUtils.getWeatherFromLocalDB(context, cityId);
				if (w != null) {
					Log.d("WeatherWidget", "get weather from local db succesfully");
					w.mInfomation.mRequestCity = cityId;
				} else {
					Log.d("WeatherWidget", "can not get weather from local db");
				}
				Log.d("WeatherWidget", "Cache the weather info");
				mCityWeathers.put(cityId, w);
			}
		}
		Log.d("WeatherWidget", "WeatherManager->getWeatherFromLocalDB end");
		return w;
	}

	public WeatherInfo getWeatherFromCacheByIndex(Context context, int index) {
		WeatherInfo w = null;
		String cityId = getCityIdFromCacheByIndex(context, index);
		if (cityId != null) {
			w = getWeatherFromCacheByCityId(context, cityId);
		}
		return w;
	}

	public void addWeatherToCache(Context context, String cityId) {
		if (cityId != null) {
		    if (mCityIds.contains(cityId)) {
			    addWeatherCacheFromDB(context, cityId);
		    } else {
			    addWeatherCacheFromDB(context, cityId);
			    mCityIds = PersistenceData.getCityids(context);
		    }
		}
	}

	public void deleteWeatherFromCache(Context context, String cityId) {
		if (cityId != null) {
            mCityIds = PersistenceData.getCityids(context);
		    if (mCityIds.contains(cityId)) {
		        mCityIds.remove(cityId);
		    }
		    if (mCityWeathers.containsKey(cityId)) {
		    	mCityWeathers.remove(cityId);
		    }
		}
	}

	private void addWeatherCacheFromDB(Context context, String cityId) {
		WeatherInfo weather = null;
		weather = WeatherUtils.getWeatherFromLocalDB(context, cityId);
		mCityWeathers.put(cityId, weather);
	}
	
	public void updateCityIdsAndWeatherCache(Context context) {		
		ArrayList<String> cityIds = PersistenceData.getCityids(context);
		for (String cityId : cityIds) {
			if (mCityWeathers.get(cityId) == null) {
				addWeatherCacheFromDB(context,cityId);
			}
		}
		ArrayList<String> saveCityIds = (ArrayList<String>)mCityIds.clone();
		if (mCityIds != null) {
		    mCityIds.clear();
		}
		mCityIds = cityIds;
		
		for (String cityId : saveCityIds) {
			if (!mCityIds.contains(cityId)) {
				mCityWeathers.remove(cityId);
			}
		}
	}
	
	public void syncCityListFromPersistenceData(Context context) {
	    if (mCityIds != null) {
	        mCityIds.clear();
	    } 
	    mCityIds = PersistenceData.getCityids(context);
	}
	
    public ArrayList<String> getCityIdsFromCache(Context context) {
        if (mCityIds == null) {
            mCityIds = PersistenceData.getCityids(context);
        }
        return mCityIds;
    }

	public void updateWeatherAndCityIdsCacheFromDB(Context context) {
		if (mCityWeathers == null) {
		    mCityWeathers = new HashMap<String, WeatherInfo>();
		}
		ArrayList<String> cityIds = PersistenceData.getCityids(context);
		if (cityIds != null && !cityIds.isEmpty()) {
		    mCityWeathers.clear();
			for (String cityId : cityIds) {
				addWeatherCacheFromDB(context, cityId);
			}
		}
		if (mCityIds != null) {
            mCityIds.clear();
		}
		mCityIds = cityIds;
	}
}
