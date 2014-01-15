package com.motorola.contextual.smartprofile.locations;

import com.motorola.contextual.smartprofile.Constants;

public interface LocConstants extends Constants{

	// Zoom level for the map view in Edit Location Activity
    public static final int DEFAULT_MAP_ZOOM_LEVEL = 3;
    public static final int DEFAULT_SATELLITE_ZOOM_LEVEL = 4;

	public static interface InferenceType {
		final int NONE = 0;
		final int BOTH = NONE + 1;
		final int HOME_ONLY = BOTH + 1;
		final int WORK_ONLY = HOME_ONLY + 1;
	}

    // Maximum allowed inferred location is 2
    public static final int MAX_INFERRED_LOCATIONS = 2;

    // ILS Related Strings
    public static final String ILS_LAUNCH_INTENT 		= "com.motorola.contextual.location.ils.IlsShareLocation";
    public static final String SEE_MY_LOC_LAT 			= "com.motorola.contextual.location.ils.latitude";
    public static final String SEE_MY_LOC_LNG 		    = "com.motorola.contextual.location.ils.longitude";
    public static final String SEE_MY_LOC_ACCURACY    	= "com.motorola.contextual.location.ils.accuracy";
    public static final String SEE_MY_LOC_ADDRESS 	    = "com.motorola.contextual.location.ils.address";
    public static final String SEE_MY_LOC_PROVIDER 	    = "com.motorola.contextual.location.ils.providerName";
    public static final String SEE_MY_LOC_BEARING_DEG   = "com.motorola.contextual.location.ils.bearingDegrees";
    public static final String SEE_MY_LOC_SPEED_MSEC	= "com.motorola.contextual.location.ils.speedMSec";
    public static final String SEE_MY_LOC_ALT_METERS	= "com.motorola.contextual.location.ils.altitudeMeters";
    public static final String SEE_MY_LOC_URI           = "com.motorola.contextual.location.ils.uri";
    public static final String SEE_MY_LOC_BITLY         = "com.motorola.contextual.location.ils.bitly";
    public static final String SEE_MY_LOC_NAME          = "com.motorola.contextual.location.ils.name";
    public static final String SEE_MY_LOC_RESP          = "com.motorola.contextual.location.ils.resp";
    public static final String SEE_MY_LOC_CATEGORY      = "com.motorola.contextual.location.ils.category";
    public static final String SEE_MY_LOC_FIX_DATE_TIME	= "com.motorola.contextual.location.ils.fixdatetime";
    public static final int ADD_LOC = 1;
    public static final int DEL_LOC = 2;

    // Location Sensor DB related URI strings
    public static final String LOC_POI_URI = "content://com.motorola.contextual.virtualsensor.locationsensor/poi";
    public static final String LOC_CONSOLIDATE_URI = "content://com.motorola.contextual.virtualsensor.locationsensor/loctime/consolidate";

    // Location Sensor DB Columns
    // LOCTIME Table
    public static final String LOC_ID			= "ID";
    public static final String POITYPE			= "poitype";
    public static final String LAT 				= "Lat";
    public static final String LNG 				= "Lgt";
    public static final String COUNT 			= "Count";
    public static final String POITAG 			= "Poitag";
    public static final String ACCUNAME 		= "Accuname";
    public static final String STARTTIME 		= "StartTime";
    public static final String ENDTIME   		= "EndTime";
    public static final String FREQCOUNT 		= "FreqCount";
    public static final String CELLJSONVALUE	= "CellJsonValue";
    public static final String ACCURACY         = "Accuracy";

    // POI Table
    public static final String POI				= "poi";
    public static final String NAME 			= "name";
    public static final String ADDRESS			= "addr";
    public static final String POI_LAT 			= "lat";
    public static final String POI_LNG 			= "lgt";
    public static final String RADIUS			= "radius";
    public static final String CELLJSONS		= "celljsons";
    public static final String WIFISSID			= "wifissid";

    // Location Intent Data Related
    public static final String LOCDATA			= "locdata";
    public static final String STATUS			= "status";
    public static final String STATUS_LOCATION  = "location";
    public static final String STATUS_ARRIVE    = "arrive";
    public static final String STATUS_LEAVE     = "leave";

    // Location Sensor DB POI Type Strings
    public static final String USER_POI 		= "User";
    public static final String SUGGESTED_POI	= "Suggested";
    public static final String ACCEPTED_POI		= "Accepted";

    // Prefix to be added to the poi name when inserting a location into Location Sensor DB
    public static final String POILOC_TAG_PREFIX = "Location:";

    // Actions of Location Change/Inference/Import
    public static final String LOCATION_CHANGE_ACTION = "com.motorola.intent.action.LOCATION_CHANGE";
    public static final String LOCATION_INFERRED_ACTION = "com.motorola.contextual.smartprofile.location_inferred";
    public static final String LOCATION_IMPORT_ACTION = "com.motorola.contextual.smartprofile.sensors.locationsensor.IMPORT";

    // Location data export related info
    boolean EXPORT_LOCATION_DATA = true;
    //intent to launch Rules Exporter to backup the data to server
    String LAUNCH_RULES_EXPORTER = "com.motorola.contextual.launch.rulesexporter";
    String EXTRA_LOCATION_DATA = "com.motorola.contextual.smartprofile.locations.intent.extra.LOCATION_DATA";

    // Phoenix :: Shared Preference to share data with Rules Exporter
    public static final String LOCATION_SHARED_PREFERENCE = "com.motorola.contextual.locationpreference";
    public static final String LOCATION_XML_CONTENT = "com.motorola.contextual.locationxml";

    //Condition publisher related constants
    public static final String CONFIG_DELIMITER = ";";
    public static final String SELECTED_LOCATIONS_TAGS = "selected_locations_tags";
    public static final String KEY_LOCATION_CONFIGS = "location_configs";
    public static final String KEY_CURRENT_LOCATION_TAG = "current_location_tag";
    public static final String LOCATION_PUBLISHER_KEY = "com.motorola.contextual.smartprofile.location";
    public static final String EQUALS_TO = "=";
    public static final String SINGLE_QUOTE = "'";
    public static final String EXTRA_CURRENT_POITAG = "CURRENT_POITAG";
}
