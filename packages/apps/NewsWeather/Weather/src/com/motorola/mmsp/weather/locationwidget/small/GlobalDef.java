package com.motorola.mmsp.weather.locationwidget.small;

import locationwidget.config.DeviceConfig;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.sinaweather.service.WeatherServiceConstant;

public class GlobalDef {

	public static final String AUTHORITY = "com.motorola.mmsp.weather.service.WeatherProvider";
	public final static String LOG_TAG = "WeatherNewsProvider_small";
	public final static boolean DEBUG = true;
	/*** weather part **/
	public final static String CONSTANT_ILLUMINATION = "Cnst Ill";
	public final static String CONSTANT_DARKNESS = "Cnst Dark";
	public final static String EDITION_SINA = "sina";
	public final static String EDITION_ACCU = "accu";
	// receive
	public final static String BROADCAST_CITYLIST_UPDITING = "com.motorola.mmsp.weather.cities.updating";
	public final static String BROADCAST_CITYLIST_UPDATE_FINISH = "com.motorola.mmsp.weather.cities.updated.response";
	public final static String BROADCAST_WEAHER_UPDATE = "com.motorola.mmsp.weather.updated.response";
	public final static String BROADCAST_DETAILVIEWEXIT = "com.motorola.mmsp.motoswitch.action.weatherdetailview.exit";
	// send
	public final static String SERVICE_INTENT_WEATHER = "motorola.intent.action.START_WEATHER_SERVICE";
	
	public static final String ACTION_LOCATION_WIDGET_ENABLE  = "com.motorola.mmsp.locationwidget.enable";
	public static final String ACTION_LOCATION_WIDGET_DISABLE = "com.motorola.mmsp.locationwidget.disable";
	
	// Uri
	public final static String table_forecast = "content://" + AUTHORITY + "/forecast/";
	public final static String table_conditions = "content://" + AUTHORITY + "/conditions";
	public final static String table_city = "content://" + AUTHORITY + "/city";
	public final static String table_setting = "content://" + AUTHORITY + "/setting";
	public final static String table_citylist = "content://" + AUTHORITY + "/citylist";
	public final static String table_appinfo = "content://" + AUTHORITY + "/appinfo";

	public final static Uri uri_forecast = Uri.parse(table_forecast);
	public final static Uri uri_condtions = Uri.parse(table_conditions);
	public final static Uri uri_city = Uri.parse(table_city);
	public final static Uri uri_setting = Uri.parse(table_setting);
	public final static Uri uri_citylist = Uri.parse(table_citylist);
	public final static Uri uri_appinfo = Uri.parse(table_appinfo);
	public final static Uri uri_settingIDs = Uri.withAppendedPath(uri_setting, "weather_cityIds");
	
	public static final Uri uri_widgetCity = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/widget_city");

	/*** rss news part **/
	public static final String KEY_NEWSID_LIST = "newsIdList";
	public static final String KEY_NEWS_SELECT_POS = "newsSelectPosition";
	public static final String KEY_CITY_NAME = "arcsoft.location.city";
	public static final String KEY_WIDGET_ID = "arcsoft.rssNews.widgetId";
	public static final String KEY_CURRENT_FEED = "com.motorola.mmsp.rss.currentfeed";

	public static final String KEY_NEWS_ID = "id_";
	public static final String KEY_NEWS_TOPIC = "topic_";
	public static final String KEY_NEWS_TITLE = "title_";
	public static final String KEY_NEWS_CONTENT = "content_";
	public static final String KEY_NEWS_DATE = "date_";
	public static final String KEY_NEWS_POSITION = "position_";

	public final static String RSS_ORIGIN_KEY = "copyright";
	public final static String RSS_ORIGIN_SINA = "sina";
	public final static String RSS_ORIGIN_ACCU = "google";
	public final static String KEY_NETWORK_STATE = "rss_network_state";
	public final static String RSS_NETWORK_STATE_START = "connect_network_start";
	public final static String RSS_NETWORK_STATE_END = "connect_network_end";
	// receive
	public final static String BROADCAST_RSS_UPDATE = "com.motoroal.mmsp.rssnews.update.response";
	public final static String BROADCAST_RSS_NETWORK_STATE = "com.motorola.mmsp.rssnews.NETWORK_STATE";
	// send
	public final static String SERVICE_INTENT_RSS = "com.motorola.mmsp.rssnews.service.RssService";
	public final static String BROADCAST_GETRSS_SETTING = "com.motorola.mmsp.rssnews.SETTING";

	/*** home part : widget size ***/
	public static final int WIDGET_INIT_SPANX = DeviceConfig.WIDGET_INIT_SPANX;
	public static final int WIDGET_INIT_SPANY = DeviceConfig.WIDGET_INIT_SPANY;
	public static final String WIDGET_CITYID = "cityIdFor";
	public static final String WIDGET_SPANX = "spanX_";
	public static final String WIDGET_SPANY = "spanY_";
	public static final String WIDGET_TYPE = "widget_type_";
	public static final String WIDGET_TYPE_DEFAULT = "widget_type_small";
	public static final String WIDGET_TYPE_SMALL = "widget_type_small";
	public static final String WIDGET_TYPE_BIG = "widget_type_big";
	// receive
	public final static String BROADCAST_SETWIDGETSIZE = "com.motorola.mmsp.home.ACTION_SET_WIDGET_SIZE";
	public final static String BROADCAST_SETWIDGETSIZE_NEW = "com.motorola.mmsp.motohome.ACTION_SET_WIDGET_SIZE";
	public final static String BROADCAST_HOME_BACKTOHS = "com.motorola.mmsp.home.action.BACKTOHS";
	public final static String BROADCAST_HOME_SWITCH = "android.intent.action.MAIN cat=[android.intent.category.HOME]";

	/**** native ****/
	public final static String CITYIDSHAREDPFE = "com.motorola.mmsp.locationwidget.cityid_sharePrfce";
	public final static String INVALIDCITYID = "invalid_cityid";
	public final static String AUTOCITYID = "auto_city";
	public final static String TEMPUNIT = "temp_unit";

	private static String UNIT_STR_C = "";
	private static String UNIT_STR_F = "";
	public static final int UNIT_C = 0;
	public static final int UNIT_F = 1;

	public static String getTemperatureUnit(int which) {
		if (which == UNIT_C) {
			return UNIT_STR_C;
		} else if (which == UNIT_F) {
			return UNIT_STR_F;
		} else {
			return "";
		}
	}

	/**
	 * load the array with Chinese.
	 */
	public static void init(Context context) {
		UNIT_STR_C = context.getResources().getString(R.string.unit_c);
		UNIT_STR_F = context.getResources().getString(R.string.unit_f);
		completeWeatherString(context);
		// for(int j=0;j<SUNNY.length;j++){
		// if (DEBUG) Log.d("GlobalDef",SUNNY[j]);
		// }
		for (int i = 0; i < WEAHTER_CONDITION.length; i++) {
			for (int j = 0; j < WEAHTER_CONDITION[i].length; j++) {
				if (DEBUG) Log.d("GlobalDef", WEAHTER_CONDITION[i][j]);
			}
		}
	}
	
	// resource day and night
	private static final int[] weatherConditionSrcId = {
		R.drawable.cloudy_4x1,       R.drawable.cloudy_night_4x1,
		R.drawable.fog_4x1 ,         R.drawable.fog_night_4x1,
		R.drawable.rain_4x1,         R.drawable.rain_4x1,
		R.drawable.hazy_4x1,         R.drawable.hazy_night_4x1,
		R.drawable.snow_4x1,         R.drawable.snow_4x1,
		R.drawable.sunny_4x1,        R.drawable.sunny_night_4x1,
		R.drawable.thunderstorms_4x1,R.drawable.thunderstorms_4x1,
		R.drawable.thunderstorms_4x1,R.drawable.thunderstorms_4x1
	};
	//correspond with above
	private static final String[] weatherConditionStr = {
		"cloudy",       "cloudy_night",
		"fog" ,         "fog_night",
		"rain",         "rain_night",
		"sand_storm",   "sand_storm_night",
		"snow",         "snow_night",
		"sunny",        "sunny_night",
		"thunder",      "thunder_night",
		"thunderstorms","thunderstorms_night"
	};
	
	public static void completeWeatherString(Context context) {
		String[] sunny = context.getResources().getStringArray(R.array.sunny);
		String[] cloudy = context.getResources().getStringArray(R.array.cloudy);
		String[] rain = context.getResources().getStringArray(R.array.rain);
		String[] snow = context.getResources().getStringArray(R.array.snow);
		String[] fog = context.getResources().getStringArray(R.array.fog);
		String[] sand_storm = context.getResources().getStringArray(R.array.sand_storm);
		String[] thunder = context.getResources().getStringArray(R.array.thunder);
		WEAHTER_CONDITION = new String[][] { sunny, cloudy, rain, snow, fog, sand_storm, thunder };
	}
	
	private static String[][] WEAHTER_CONDITION  = null;//{SUNNY,CLOUDY,RAIN,SNOW,FOG,SAND_STORM,THUNDER};
	
//	private static final String[] SUNNY = {"sunny","1","2","3","24","30","31","32","33","34","晴"};
//	private static final String[] CLOUDY = {"cloudy","4","5","6","7","8","35","36","38","多云","阴"};
//	private static final String[] RAIN = {"rain","12","13","14","18","26","29","39","40","41","42",
//		"阵雨","雨夹雪","小雨","中雨","大雨","暴雨","大暴雨","特大暴雨","小到中雨","中到大雨","大到暴雨",
//		"冻雨","冰雨","冰雹"};
//	private static final String[] SNOW = {"snow","19","20","21","22","23","25","43","44",
//		"阵雪","小雪","中雪","大雪","小到中雪","中到大雪","大到暴雪","暴雪"};
//	private static final String[] FOG = {"fog","11","雾","暴雾","浓雾","霾","烟"};
//	private static final String[] SAND_STORM = {"sand_storm","37",
//		"沙尘暴","浮尘","扬沙","强沙尘暴"};
//	private static final String[] THUNDER = {"thunder","15","16","17","41","42",
//		"雷阵雨","雷阵雨伴有冰雹","雷阵雨并伴有冰雹"};
//	
	
	
	public static int getSrcIdWithWeatherCondition(String weather) {
		if (weather == null) {
			return R.drawable.sunny_4x1;
		}
		int index = 0;
		String temp = null;
		int length = weatherConditionStr.length;
		while (index < length) {
			temp = weatherConditionStr[index];
			if (weather.equals(temp)) {
				return weatherConditionSrcId[index];
			}
			index++;
		}
		return R.drawable.sunny_4x1;
	}
	
	public GlobalDef(Context context)
	{
		completeWeatherString(context);
		
		Log.i(LOG_TAG, "GlobalDef");
		
		String[] sunny = context.getResources().getStringArray(R.array.sunny);
		String[] cloudy = context.getResources().getStringArray(R.array.cloudy);
		String[] rain = context.getResources().getStringArray(R.array.rain);
		String[] snow = context.getResources().getStringArray(R.array.snow);
		String[] fog = context.getResources().getStringArray(R.array.fog);
		String[] sand_storm = context.getResources().getStringArray(R.array.sand_storm);
		String[] thunder = context.getResources().getStringArray(R.array.thunder);
		WEAHTER_CONDITION = new String[][] { sunny, cloudy, rain, snow, fog, sand_storm, thunder };
	}
	
	public static String getWeatherConditionWithIndex(String weatherType) {
		if (null == weatherType || WEAHTER_CONDITION == null) {
			return "sunny";
		}
		if (weatherType.equals("41") || weatherType.equals("42")) {
			return "thunderstorms";
		}
		int length1 = WEAHTER_CONDITION.length;
		for (int i = 0; i < length1; i++) {
			int length2 = WEAHTER_CONDITION[i].length;
			for (int j = 1; j < length2; j++) {
				if (weatherType.equals(WEAHTER_CONDITION[i][j])) {
					return WEAHTER_CONDITION[i][0];
				}
			}
		}
		return "sunny";
	}
	
	public static boolean chineseValid(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		byte[] b;
		for (int i = 0; i < length; i++) {
			b = str.substring(i).getBytes();
			if ((b[0] & 0xff) > 128) {
				return true;
			}
		}
		return false;
	}

//	public static int getTopicIconId(String category, String origin) {
//		if (RSS_ORIGIN_SINA.equals(origin)) {
//			return getSinaTopicIconId(category);
//		} else {
//			return getAccuTopicIconId(category);
//		}
//	}
	
//	public static int getAccuTopicIconId(String category) {
//		if (category == null) {
//			return R.drawable.topic_accu_l;
//		} else if (category.equals(TOPIC_BUSINESS)) {
//			return R.drawable.topic_accu_b;
//		} else if (category.equals(TOPIC_SCI_TECH)) {
//			return R.drawable.topic_accu_t;
//		} else if (category.equals(TOPIC_ENTERTAIMENT)) {
//			return R.drawable.topic_accu_e;
//		} else if (category.equals(TOPIC_SPORTS)) {
//			return R.drawable.topic_accu_s;
//		} else if (category.equals(TOPIC_LOCATION)) {
//			return R.drawable.topic_accu_l;
//		} else if (category.equals(TOPIC_TOP_STORIES)) {
//			return R.drawable.topic_accu_h;
//		} else if (category.equals(TOPIC_WORLD)) {
//			return R.drawable.topic_accu_w;
//		} else if (category.equals(TOPIC_NATION)) {
//			return R.drawable.topic_accu_n;
//		}
//		return R.drawable.topic_accu_l;// default
//	}

//	public static int getSinaTopicIconId(String category) {
//		if (category == null) {
//			return R.drawable.topic_accu_l;
//		} else if (category.equals(TOPIC_BUSINESS)) {
//			return R.drawable.topic_sina_b;
//		} else if (category.equals(TOPIC_SCI_TECH)) {
//			return R.drawable.topic_sina_t;
//		} else if (category.equals(TOPIC_ENTERTAIMENT)) {// no supply
//			return R.drawable.topic_accu_l;
//		} else if (category.equals(TOPIC_SPORTS)) {
//			return R.drawable.topic_sina_s;
//		} else if (category.equals(TOPIC_READING)) {
//			return R.drawable.topic_sina_r;
//		} else if (category.equals(TOPIC_CAR)) {
//			return R.drawable.topic_sina_a;
//		} else if (category.equals(TOPIC_WOMAN)) {
//			return R.drawable.topic_sina_woman;
//		} else if (category.equals(TOPIC_MAN)) {
//			return R.drawable.topic_sina_man;
//		} else if (category.equals(TOPIC_MILE)) {
//			return R.drawable.topic_sina_mile;
//		} else if (category.equals(TOPIC_EDUCATION_HOT)) {
//			return R.drawable.topic_sina_ed;
//		} else if (category.equals(TOPIC_ASTRO)) {
//			return R.drawable.topic_sina_astro;
//		}
//		return R.drawable.topic_accu_l;// default
//	}
	//topic name in the rss database
	//google
//	private static final String TOPIC_LOCATION       = "l";
//	private static final String TOPIC_TOP_STORIES    = "h";
//	private static final String TOPIC_WORLD          = "w";
//	private static final String TOPIC_NATION         = "n";
//	//common
//	private static final String TOPIC_BUSINESS       = "b";//财经
//	private static final String TOPIC_SCI_TECH       = "t";//科技
//	private static final String TOPIC_ENTERTAIMENT   = "e";//
//	private static final String TOPIC_SPORTS         = "s";//体育
//	//sina
//	private static final String TOPIC_READING        = "r";//读书
//	private static final String TOPIC_CAR            = "a";//汽车
//	private static final String TOPIC_WOMAN          = "woman";//女性
//	private static final String TOPIC_MAN            = "man";//男性
//	private static final String TOPIC_MILE           = "mile";//军事
//	private static final String TOPIC_EDUCATION_HOT  = "ed";//教育
//	private static final String TOPIC_ASTRO          = "astro";//星座
	
	/**
	 * The method is protected by "try catch".*/
	public static Cursor query(Context context, Uri uri, String selection, String[] selectionargs, String sortOrder) {
		Cursor c = null;
		if (context == null || uri == null) {
			return c;
		}
		ContentResolver cr = context.getContentResolver();
		try {
			c = cr.query(uri, null, selection, selectionargs, sortOrder);
		} catch (Exception e) {
			e.printStackTrace();
			if (DEBUG) Log.e(LOG_TAG, "query datebase error");
			if (c != null) {
				c.close();
				c = null;
			}
		}
		return c;
	}
	
	private static String[] filterOldString = {"'"};
    private static String[] filterNewString = {"%27"};

	/**
	 * when news insert to database, the "'" will change to "%27", we should change them back
	 */
	public static String reverseFilter(String oldString) {
		if (oldString == null) {
			return null;
		}
		// if (DEBUG)Log.d(LOG_TAG,"old content = "+oldString);
		String newString = oldString;
		int length = filterNewString.length;
		for (int i = 0; i < length; i++) {
			newString = newString.replace(filterNewString[i], filterOldString[i]);
		}
		// if (DEBUG)Log.d(LOG_TAG,"old content = "+newString);
		return newString;
	}
	
	public static class MyLog {
		public static void v(String tag, String value) {
			if (DEBUG)Log.v(tag, value);
		}
		public static void d(String tag, String value) {
			if (DEBUG)Log.d(tag, value);
		}
		public static void i(String tag, String value) {
			if (DEBUG)Log.i(tag, value);
		}
		public static void w(String tag, String value) {
			if (DEBUG)Log.w(tag, value);
		}
		public static void e(String tag, String value) {
			if (DEBUG)Log.e(tag, value);
		}
	}
}


