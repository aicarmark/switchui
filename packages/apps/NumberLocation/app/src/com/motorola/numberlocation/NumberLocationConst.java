package com.motorola.numberlocation;

public class NumberLocationConst {
	

	public static final int NUMBER_TYPE_UNKNOW = 1;
	public static final int NUMBER_TYPE_MOBILE = 2;
	public static final int NUMBER_TYPE_FIX_DOMESTIC = 3;
	public static final int NUMBER_TYPE_FIX_INTERNATIONAL = 4;	
	
	//status
	public static final int STATUS_SUCCESS = 1;
	public static final int STATUS_NOT_FOUND = 2;
	public static final int STATUS_NETWORK_FAIL = 3;
	public static final int STATUS_REQ_PARM_ERROR = 4;
	public static final int STATUS_USER_CANCEL = 5;

	//progress dialog type
	public static final int PROGRESS_TYPE_NULL = 0;
	public static final int PROGRESS_TYPE_DOWNLOAD = 1;
	public static final int PROGRESS_TYPE_DATABASE_UPDATE = 2;
	
	//preference
	public static final String NUMBER_LOCATION_PREFERENCE = "number_location";
	
	// Preference keys
	public static final String KEY_PREFERENCES_SETTING = "preferences_setting";
	
	public static final String KEY_NEXT_AUTO_UPDATE_TIME = "next_auto_update_time";
	
	public static final String KEY_AUTO_UPDATE = "preferences_auto_update";
	public static final boolean DEFAULT_VALUE_OF_AUTO_UPDATE = false;
	
	public static final int DEFAULT_VALUE_OF_PARAM_RESULT_STATUS = STATUS_SUCCESS;
	public static final int DEFAULT_VALUE_OF_PARAM_PROGRESS_VALUE = 0;
	public static final int DEFAULT_VALUE_OF_PARAM_PROGRESS_TYPE = PROGRESS_TYPE_DOWNLOAD;
	public static final int DEFAULT_VALUE_OF_PARAM_PROGRESS_MAX = 100;
	
	public static final String KEY_AUTO_UPDATE_INTERVAL = "preferences_set_update_interval";
	public static final String AUTO_UPDATE_INTERVAL_1_M = "1";
	public static final String AUTO_UPDATE_INTERVAL_3_M = "2";
	public static final String AUTO_UPDATE_INTERVAL_6_M = "3";
	
	public static final String KEY_LAST_UPGRADE_TIMESTAMP = "preferences_last_upgrade_timestamp";
	
	public static final String KEY_DATABASE_VERSION = "preferences_database_version";
	
	public static final String KEY_SKIP_NETWORK_TRAFFIC_WARNING = "skip_network_traffic_warning";
	
	//class
	public static final String CLASS_NUMBERLOCATIONSERVICE = "com.motorola.numberlocation.NumberLocationService";
	
	//actions
	public static final String ACTION_AUTO_UPDATE = "com.motorola.numberlocation.AUTO_UPDATE";
	public static final String ACTION_AUTO_CHECK_SETTING_CHANGED = "com.motorola.numberlocation.AUTO_CHECK_SETTING_CHANGED";
	public static final String ACTION_SET_LAST_UPDATE_TIMESTAMP = "com.motorola.numberlocation.SET_LAST_UPDATE_TIMESTAMP";
	
	public static final String ACTION_CALL_BACK_SHOW_RESULT = "com.motorola.numberlocation.CALL_BACK_SHOW_RESULT";
	public static final String ACTION_CALL_BACK_SHOW_PROGRESS = "com.motorola.numberlocation.CALL_BACK_SHOW_PROGRESS";
	public static final String ACTION_CALL_BACK_UPDATE_PROGRESS = "com.motorola.numberlocation.CALL_BACK_UPDATE_PROGRESS";
	public static final String ACTION_CALL_BACK_DISMISS_PROGRESS = "com.motorola.numberlocation.CALL_BACK_DISMISS_PROGRESS";
	public static final String ACTION_CALL_BACK_SET_PROGRESS_MAX = "com.motorola.numberlocation.CALL_BACK_SET_PROGRESS_MAX";

	//extras
	public static final String EXTRA_AUTO_UPDATE_SWITCH = "auto_update_switch";
	public static final String EXTRA_AUTO_UPDATE_INTERVAL_TIME = "auto_update_interval_time";
	public static final String EXTRA_LAST_UPDATE_TIMESTAMP = "last_update_timestamp";	
	
	public static final String EXTRA_PARAM_RESULT_STATUS = "param_result_status";	
	public static final String EXTRA_PARAM_PROGRESS_VALUE = "param_progress_value";	
	public static final String EXTRA_PARAM_PROGRESS_TYPE = "param_progress_type";	
	public static final String EXTRA_PARAM_PROGRESS_MAX = "param_progress_max";	
	

    //bundle data
	public static final String BUNDLE_PROGRESS_DATA = "bundle_progress_data";	
	public static final String BUNDLE_PROGRESS_TYPE = "bundle_progress_type";	
	public static final String BUNDLE_PROGRESS_MAX = "bundle_progress_max";	
	public static final String BUNDLE_DATA_RESULT_NUM = "bundle_data_result_num";
	
	//search type
	public static final int SEARCH_TYPE_NULL = 0;
	public static final int SEARCH_TYPE_LOCATION_BY_NUMBER = 1;
	public static final int SEARCH_TYPE_NUMBER_BY_CITY_LOCATION = 2;
	public static final int SEARCH_TYPE_NUMBER_BY_COUNTRY_LOCATION = 3;
	
	//message id
	public static final int ID_PROGRESS_DIALOG_SHOW = 1;	
	public static final int ID_PROGRESS_DIALOG_UPDATE = 2;	
	public static final int ID_PROGRESS_DIALOG_DISMISS = 3;
	public static final int ID_SEARCH_RESULT_NUM = 4;
	public static final int ID_ALERT_DIALOG_SHOW_UPDATE_SUCCESSFULLY = 11;	
	public static final int ID_ALERT_DIALOG_SHOW_UPDATE_NOT_FOUND = 12;	
	public static final int ID_ALERT_DIALOG_SHOW_NETWORK_FAILURE = 13;	
	public static final int ID_ALERT_DIALOG_SHOW_USER_CANCEL = 14;	
	public static final int ID_PROGRESS_DIALOG_SET_MAX = 15;
    //misc
//	public static final long AUTO_UPDATE_INTERVAL_DEFAULT = 60L * 1000;
    public static final long AUTO_UPDATE_INTERVAL_DEFAULT = 30L * 24 * 3600 * 1000;

//	public static final String WEBUPDATE_URL = "http://219.238.232.14/update.jsp?ver=%s";
	public static final String WEBUPDATE_URL = "http://android.ikno.cn/update.jsp?ver=%s";
//	public static final String WEBUPDATE_URL = "http://android.ikno.cn/update-test.jsp?ver=%s";
	
	public static final String XML_PARSE_VERSION = "1.0.0";
	
	public static final int MAX_DATABASE_TRANSACTION_EVENT = 100;
	public static final int MAX_DATABASE_TRANSACTION_INTERRUPT_SLEEP_TIME = 100;

// for debug test	
	public static final String SD_CARD_DEBUG_FILE_NAME = "number_location_debug.xml";
	public static final String TAG_NUMBER_LOCATION_DEBUG = "number_location_debug";
	public static final String TAG_REQUEST_URL_PATH = "request_url_path";
	public static final String TAG_LOCAL_UPDATE_PATH = "local_update_path";
	public static final String TAG_PROBLEM_CAUSE = "problem_cause";
}
