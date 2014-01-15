/*
 * (c) COPYRIGHT 2010-2011 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date         CR Number         Brief Description
 * ------------- ------------ ----------------- ------------------------------
 * w04917        2010/12/16                     Initial version
 * w04917        2011/01/06                     Include APP_VERSION
 * w04917        2012/02/07   IKHSS7-8262       Disable unused check-in. Need Check-in API rework.
 * w04917        2012/03/22   IKHSS6UPGR-3644   ANR fix and disable unused functions
 */

package com.motorola.contextual.virtualsensor.locationsensor.metrics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class MetricsLogger {

    private Context mContext = null;

    /* Checkin stuffs */
    public static final String VERSION = "1.1";
    public static final String PACKAGE_NAME = MetricsLogger.class.getPackage().getName();
    public static final String CHECKIN_TAG = "MOT_CA_LOC";
    public static final int CHECKIN_BLOCK_SIZE = 1024;
    public static final String CHECKIN_ID_HEAD = "[ID=LocSensorMetrics;pkg=" + PACKAGE_NAME + ";ver="+VERSION+";time=";
    public static final String CHECKIN_ID_DATA = "[ID=LocSensorMetricsData;";
    public static final String CHECKIN_ID_CLOSE = ";]";

    /* Log File stuffs */
    public static final String logRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mot_metrics/";
    public static final String DELIM = "|";
    public static final String logOnFileName = logRoot + "logon.txt";
    public static final String logFileNameCellTowerChanged = logRoot + "cell_tower_changed.txt";
    public static final String logFileNameAlarmTimerLocationExpired = logRoot + "alarm_timer_location_expired.txt";
    public static final String logFileNameWifi = logRoot + "wifi.txt";
    public static final String logFileNameLocationUpdate = logRoot + "location_update.txt";

    public static final int numLogFileCellTowerFields = 3;
    public static final int numLogFileWifiFields = 3;
    public static final int numLogFileLocationUpdateFields = 5;

    /* Constant strings for log file */
    public static final String TYPE_REQUEST = "REQUEST";
    public static final String TYPE_ACTUAL = "ACTUAL";
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_RAW = "RAW";
    public static final String TYPE_DETECTION = "DETECTION";
    public static final String TYPE_DISCOVERY = "DISCOVERY";
    public static final String SWITCH_ON = "ON";
    public static final String SWITCH_OFF = "OFF";

    /* Constant values for accuracy and distance */
    public static final int ACCURACY_25M = 25;
    public static final int ACCURACY_50M = 50;
    public static final int ACCURACY_100M = 100;
    public static final int ACCURACY_500M = 500;
    public static final int ACCURACY_1000M = 1000;
    public static final int ACCURACY_LARGE = 1001;

    public static final int LEAVING_50M = 50;
    public static final int LEAVING_100M = 100;
    public static final int LEAVING_200M = 200;
    public static final int LEAVING_1000M = 1000;
    public static final int LEAVING_2000M = 2000;
    public static final int LEAVING_LARGE = 2001;

    private SystemTime systemTime;
    private long startTimeMs;
    private long wifiStartTimeMs = -1;
    private SharedPreferences metricData;

    private final String PREF_NAME = "locationSensorMetricData";

    private final String COUNT_SERVICE_STARTED = "service_started";
    private final String COUNT_SERVICE_STOPPED = "service_stopped";
    private final String COUNT_MSG_CELLTOWER_CHANGED = "msg_celltower_changed";
    private final String COUNT_RAW_CELLTOWER_CHANGE = "raw_celltower_change";
    private final String COUNT_ALARM_TIMER_LOCATION_EXPIRED = "alarm_timer_location_expired";
    private final String COUNT_REQUEST_WIFI_ON = "request_wifi_on";
    private final String COUNT_REQUEST_WIFI_OFF = "request_wifi_off";
    private final String COUNT_ACTUAL_WIFI_ON = "actual_wifi_on";
    private final String COUNT_ACTUAL_WIFI_OFF = "actual_wifi_off";
    private final String APP_VERSION = "app_version";

    public static final int LOCATION_UPDATE_ZONE_SIZE = 6;
    public static final int[] LOCATION_ZONES = {ACCURACY_25M, ACCURACY_50M, ACCURACY_100M, ACCURACY_500M, ACCURACY_1000M, ACCURACY_LARGE};

    private final int LOCATION_ZONE_25M = 0;
    private final int LOCATION_ZONE_50M = 1;
    private final int LOCATION_ZONE_100M = 2;
    private final int LOCATION_ZONE_500M = 3;
    private final int LOCATION_ZONE_1000M = 4;
    private final int LOCATION_ZONE_LARGE = 5;

    private final String COUNT_DETECTION_REQUEST_LOCATION_UPDATE = "detection_request_location_update";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_25M = "detection_receive_location_update_25m";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_50M = "detection_receive_location_update_50m";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_100M = "detection_receive_location_update_100m";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_500M = "detection_receive_location_update_500m";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_1000M = "detection_receive_location_update_1000m";
    private final String COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_LARGE = "detection_receive_location_update_large";

    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_25M = "discovery_receive_location_update_25m";
    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_50M = "discovery_receive_location_update_50m";
    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_100M = "discovery_receive_location_update_100m";
    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_500M = "discovery_receive_location_update_500m";
    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_1000M = "discovery_receive_location_update_1000m";
    private final String COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_LARGE = "discovery_receive_location_update_large";

    public static final int LEAVING_ZONE_SIZE = 6;
    public static final int[] LEAVING_ZONES = {LEAVING_50M, LEAVING_100M, LEAVING_200M, LEAVING_1000M, LEAVING_2000M, LEAVING_LARGE};

    private final int LEAVING_ZONE_50M = 0;
    private final int LEAVING_ZONE_100M = 1;
    private final int LEAVING_ZONE_200M = 2;
    private final int LEAVING_ZONE_1000M = 3;
    private final int LEAVING_ZONE_2000M = 4;
    private final int LEAVING_ZONE_LARGE = 5;

    private final String COUNT_LEAVING_DISTANCE_50M = "leaving_distance_50m";
    private final String COUNT_LEAVING_DISTANCE_100M = "leaving_distance_100m";
    private final String COUNT_LEAVING_DISTANCE_200M = "leaving_distance_200m";
    private final String COUNT_LEAVING_DISTANCE_1000M = "leaving_distance_1000m";
    private final String COUNT_LEAVING_DISTANCE_2000M = "leaving_distance_2000m";
    private final String COUNT_LEAVING_DISTANCE_LARGE = "leaving_distance_LARGE";

    private final String COUNT_STAY_IN_POI = "stay_in_poi";
    private final String COUNT_SWITCH_POI = "switch_poi";
    private final String COUNT_ENTER_POI = "enter_poi";
    private final String COUNT_LEAVE_POI = "leave_poi";
    private final String COUNT_FALSE_LEAVE_POI = "false_leave_poi"; //thought we left but beacon scan told us otherwise
    private final String COUNT_APPROACHING_POI = "approaching_poi"; //within poi cell but not yet in poi
    private final String COUNT_NOWHERE = "nowhere"; //not in poi and cell not associated with poi
    private final String COUNT_NEW_POI_CELL = "new_poi_cell"; //learned that a new cell is associated with a known poi

    private final String COUNT_BEACON_SCAN = "beacon_scan"; //how many times we initiate beacon scan
    private final String COUNT_WIFI_SCAN_RESULT = "wifi_scan_result"; //how many times we receive WIFI_SCAN_RESULT (regardless of who initiated it)

    private final String COUNT_GEO_ADDRESS_CACHE_HIT = "geo_address_cache_hit";
    private final String COUNT_GEO_ADDRESS_CACHE_MISS = "geo_address_cache_miss";

    private final String TIME_WIFI_ON_DURATION = "time_wifi_on_duration";

    /* Constructor for testing only */
    public MetricsLogger(Context ctx, SystemTime argSystemTime) {
        systemTime = argSystemTime;
        mContext = ctx;
        init();
    }

    public MetricsLogger(Context ctx) {
        systemTime = new SystemTime();
        mContext = ctx;
        init();
    }

    private void init() {
        metricData = mContext.getSharedPreferences(PREF_NAME, 0);
        setServiceStartTime();
    }

    private String getString(String dataKey) {
        return metricData.getString(dataKey, null);
    }
    private void setString(String dataKey, String value) {
        if (value != null && value.length() > 0) {
            SharedPreferences.Editor editor = metricData.edit();
            editor.putString(dataKey, value);
            editor.apply();
        }
    }

    /* Helper functions to retrive/store metric data */
    private int getCount(String dataKey) {
        return metricData.getInt(dataKey, 0);
    }
    private void incrementCount(String dataKey) {
        SharedPreferences.Editor editor = metricData.edit();
        editor.putInt(dataKey, getCount(dataKey) + 1);
        editor.apply();
    }

    private long getTime(String dataKey) {
        return metricData.getLong(dataKey, 0);
    }
    private void addTime(String dataKey, long time) {
        SharedPreferences.Editor editor = metricData.edit();
        editor.putLong(dataKey, getTime(dataKey) + time);
        editor.apply();
    }

    public boolean logToFile() {
        return false;
        /*
        File logOnFile = new File(logOnFileName);
        return (logOnFile.exists());
        */
    }

    public void checkin() {
        /*
        StringBuilder sb = new StringBuilder();

        sb.append(CHECKIN_ID_HEAD);
        sb.append(systemTime.currentTimeMillis());
        sb.append(CHECKIN_ID_CLOSE);

        sb.append(CHECKIN_ID_DATA);
        sb.append(getAppVersion());
        sb.append(DELIM);
        sb.append(getTimeServiceUp());
        sb.append(DELIM);
        sb.append(getCountServiceStarted());
        sb.append(DELIM);
        sb.append(getCountServiceStopped());
        sb.append(DELIM);
        sb.append(getCountMsgCellTowerChanged());
        sb.append(DELIM);
        sb.append(getCountRawCellTowerChange());
        sb.append(DELIM);
        sb.append(getCountAlarmTimerLocationExpired());
        sb.append(DELIM);
        sb.append(getCountRequestWifiOn());
        sb.append(DELIM);
        sb.append(getCountRequestWifiOff());
        sb.append(DELIM);
        sb.append(getCountActualWifiOn());
        sb.append(DELIM);
        sb.append(getCountActualWifiOff());
        sb.append(DELIM);
        sb.append(getTimeWifiOnDuration());
        sb.append(DELIM);
        sb.append(getCountDetectionRequestLocationUpdate());
        sb.append(DELIM);

        int count[] = getCountDetectionReceiveLocationUpdate();
        for (int i = 0; i < LOCATION_UPDATE_ZONE_SIZE; i++) {
            sb.append(count[i]);
            sb.append(DELIM);
        }

        count = getCountDiscoveryReceiveLocationUpdate();
        for (int i = 0; i < LOCATION_UPDATE_ZONE_SIZE; i++) {
            sb.append(count[i]);
            sb.append(DELIM);
        }

        count = getCountLeavingDistance();
        for (int i = 0; i < LEAVING_ZONE_SIZE; i++) {
            sb.append(count[i]);
            sb.append(DELIM);
        }

        sb.append(getCountStayInPoi());
        sb.append(DELIM);
        sb.append(getCountSwitchPoi());
        sb.append(DELIM);
        sb.append(getCountEnterPoi());
        sb.append(DELIM);
        sb.append(getCountLeavePoi());
        sb.append(DELIM);
        sb.append(getCountFalseLeavePoi());
        sb.append(DELIM);
        sb.append(getCountApproachingPoi());
        sb.append(DELIM );
        sb.append(getCountNoWhere());
        sb.append(DELIM);
        //sb.append(getCountNewPoiCell());
        //sb.append(DELIM);
        sb.append(getCountBeaconScan());
        sb.append(DELIM);
        sb.append(getCountWifiScanResult());
        sb.append(DELIM);
        sb.append(getCountGeoAddressCacheHit());
        sb.append(DELIM);
        sb.append(getCountGeoAddressCacheMiss());
        sb.append(CHECKIN_ID_CLOSE);

        String checkinMessage = sb.toString();
        Log.d(CHECKIN_TAG, checkinMessage);

        LocationSensorApp.checkin(mContext, CHECKIN_TAG, checkinMessage);
        */
        reset();
    }

    private void writeToFile(String fileName, String content) {
        if (logToFile()) {
            BufferedWriter bwriter = null;
            boolean append = true;
            try {
                bwriter = new BufferedWriter(new FileWriter(fileName, append));
                bwriter.write(content);
                bwriter.newLine();
                bwriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bwriter != null) {
                    try {
                        bwriter.close();
                    } catch (IOException e) {}
                    bwriter = null;
                }
            }
        }
    }

    public void reset() {
        SharedPreferences.Editor editor = metricData.edit();
        editor.remove(APP_VERSION);
        editor.remove(COUNT_SERVICE_STARTED);
        editor.remove(COUNT_SERVICE_STOPPED);
        editor.remove(COUNT_MSG_CELLTOWER_CHANGED);
        editor.remove(COUNT_RAW_CELLTOWER_CHANGE);
        editor.remove(COUNT_ALARM_TIMER_LOCATION_EXPIRED);
        editor.remove(COUNT_REQUEST_WIFI_ON);
        editor.remove(COUNT_REQUEST_WIFI_OFF);
        editor.remove(COUNT_ACTUAL_WIFI_ON);
        editor.remove(COUNT_ACTUAL_WIFI_OFF);
        editor.remove(COUNT_MSG_CELLTOWER_CHANGED);
        editor.remove(COUNT_RAW_CELLTOWER_CHANGE);
        editor.remove(COUNT_DETECTION_REQUEST_LOCATION_UPDATE);

        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_25M);
        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_50M);
        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_100M);
        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_500M);
        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_1000M);
        editor.remove(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_LARGE);

        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_25M);
        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_50M);
        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_100M);
        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_500M);
        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_1000M);
        editor.remove(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_LARGE);

        editor.remove(COUNT_LEAVING_DISTANCE_50M);
        editor.remove(COUNT_LEAVING_DISTANCE_100M);
        editor.remove(COUNT_LEAVING_DISTANCE_200M);
        editor.remove(COUNT_LEAVING_DISTANCE_1000M);
        editor.remove(COUNT_LEAVING_DISTANCE_2000M);
        editor.remove(COUNT_LEAVING_DISTANCE_LARGE);

        editor.remove(COUNT_STAY_IN_POI);
        editor.remove(COUNT_SWITCH_POI);
        editor.remove(COUNT_ENTER_POI);
        editor.remove(COUNT_LEAVE_POI);
        editor.remove(COUNT_FALSE_LEAVE_POI);
        editor.remove(COUNT_APPROACHING_POI);
        editor.remove(COUNT_NOWHERE);
        editor.remove(COUNT_NEW_POI_CELL);
        editor.remove(COUNT_BEACON_SCAN);
        editor.remove(COUNT_WIFI_SCAN_RESULT);

        editor.remove(COUNT_GEO_ADDRESS_CACHE_HIT);
        editor.remove(COUNT_GEO_ADDRESS_CACHE_MISS);

        editor.remove(TIME_WIFI_ON_DURATION);
        editor.apply();

        setServiceStartTime();
    }
    /* End of Helper functions */

    private void setServiceStartTime() {
        startTimeMs = systemTime.currentTimeMillis();
    }

    public long getTimeServiceUp() {
        return systemTime.currentTimeMillis() - startTimeMs;
    }


    /* Count Service Started */
    public int getCountServiceStarted() {
        return getCount(COUNT_SERVICE_STARTED);
    }
    public void logServiceStarted() {
        incrementCount(COUNT_SERVICE_STARTED);
    }

    /* Count Service Started */
    public int getCountServiceStopped() {
        return getCount(COUNT_SERVICE_STOPPED);
    }
    public void logServiceStopped() {
        incrementCount(COUNT_SERVICE_STOPPED);
    }

    /* Count CELLTOWER_CHANGED Message */
    public int getCountMsgCellTowerChanged() {
        return getCount(COUNT_MSG_CELLTOWER_CHANGED);
    }
    public void logMsgCellTowerChanged(String celljson) {
        incrementCount(COUNT_MSG_CELLTOWER_CHANGED);

        long timestamp = systemTime.currentTimeMillis();
        if (celljson == null) {
            celljson = "null";
        }
        String content = String.valueOf(timestamp) + DELIM + TYPE_MESSAGE + DELIM + celljson;
        writeToFile(logFileNameCellTowerChanged, content);
    }


    /* Count raw celltower change callback from telephony manager */
    public int getCountRawCellTowerChange() {
        return getCount(COUNT_RAW_CELLTOWER_CHANGE);
    }
    public void logRawCellTowerChange(String celljson) {
        incrementCount(COUNT_RAW_CELLTOWER_CHANGE);

        long timestamp = systemTime.currentTimeMillis();
        if (celljson == null) {
            celljson = "null";
        }
        String content = String.valueOf(timestamp) + DELIM + TYPE_RAW + DELIM + celljson;
        writeToFile(logFileNameCellTowerChanged, content);
    }


    /* Count ALARM_TIMER_LOCATION_EXPIRED */
    public int getCountAlarmTimerLocationExpired() {
        return getCount(COUNT_ALARM_TIMER_LOCATION_EXPIRED);
    }
    public void logAlarmTimerLocationExpired() {
        incrementCount(COUNT_ALARM_TIMER_LOCATION_EXPIRED);
        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp);
        writeToFile(logFileNameAlarmTimerLocationExpired, content);
    }


    /* Count how many times wifi was requested to be turned on by the algorithm,
     * which may or may not have resulted in an actual turning on of WIFI
     */
    public int getCountRequestWifiOn() {
        return getCount(COUNT_REQUEST_WIFI_ON);
    }
    public void logRequestWifiOn() {
        incrementCount(COUNT_REQUEST_WIFI_ON);
        logWifiOn();

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_REQUEST + DELIM + SWITCH_ON;
        writeToFile(logFileNameWifi, content);
    }


    /* Count how many times wifi was requested to be turned off by the algorithm,
     * which may or may not have resulted in an actual turning off of WIFI
     */
    public int getCountRequestWifiOff() {
        return getCount(COUNT_REQUEST_WIFI_OFF);
    }
    public void logRequestWifiOff() {
        incrementCount(COUNT_REQUEST_WIFI_OFF);
        logWifiOff();

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_REQUEST + DELIM + SWITCH_OFF;
        writeToFile(logFileNameWifi, content);
    }


    /* Count how many times wifi was actually turned on */
    public int getCountActualWifiOn() {
        return getCount(COUNT_ACTUAL_WIFI_ON);
    }
    public void logActualWifiOn() {
        incrementCount(COUNT_ACTUAL_WIFI_ON);

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_ACTUAL + DELIM + SWITCH_ON;
        writeToFile(logFileNameWifi, content);
    }


    /* Count how many times wifi was actually turned off */
    public int getCountActualWifiOff() {
        return getCount(COUNT_ACTUAL_WIFI_OFF);
    }
    public void logActualWifiOff() {
        incrementCount(COUNT_ACTUAL_WIFI_OFF);

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_ACTUAL + DELIM + SWITCH_OFF;
        writeToFile(logFileNameWifi, content);
    }


    /* WIFI on duration as required by the algorithm */
    private void logWifiOn() {
        if (wifiStartTimeMs == -1) {
            wifiStartTimeMs = systemTime.currentTimeMillis();
        }
    }
    private void logWifiOff() {
        if (wifiStartTimeMs != -1) {
            long wifiOnDuration = systemTime.currentTimeMillis() - wifiStartTimeMs;
            addTime(TIME_WIFI_ON_DURATION, wifiOnDuration);
            wifiStartTimeMs = -1;
        }
    }
    public long getTimeWifiOnDuration() {
        return getTime(TIME_WIFI_ON_DURATION);
    }


    /* Location Manager Updates from Detection Algorithm */
    public int getCountDetectionRequestLocationUpdate() {
        return getCount(COUNT_DETECTION_REQUEST_LOCATION_UPDATE);
    }

    public void logDetectionRequestLocationUpdate() {
        incrementCount(COUNT_DETECTION_REQUEST_LOCATION_UPDATE);
    }

    public int[] getCountDetectionReceiveLocationUpdate() {
        int[] locationUpdate = new int[LOCATION_UPDATE_ZONE_SIZE];

        locationUpdate[LOCATION_ZONE_25M] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_25M);
        locationUpdate[LOCATION_ZONE_50M] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_50M);
        locationUpdate[LOCATION_ZONE_100M] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_100M);
        locationUpdate[LOCATION_ZONE_500M] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_500M);
        locationUpdate[LOCATION_ZONE_1000M] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_1000M);
        locationUpdate[LOCATION_ZONE_LARGE] = getCount(COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_LARGE);

        return locationUpdate;
    }

    public void logDetectionReceiveLocationUpdate(double lat, double lon, int accuracy) {
        String dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_LARGE;

        /* Log accuracy zone */
        if (accuracy <= ACCURACY_25M) {
            dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_25M;
        } else if (accuracy <= ACCURACY_50M) {
            dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_50M;
        } else if (accuracy <= ACCURACY_100M) {
            dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_100M;
        } else if (accuracy <= ACCURACY_500M) {
            dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_500M;
        } else if (accuracy <= ACCURACY_1000M) {
            dataKey = COUNT_DETECTION_RECEIVE_LOCATION_UPDATE_1000M;
        }
        incrementCount(dataKey);

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_DETECTION +
                         DELIM + String.valueOf(lat) +
                         DELIM + String.valueOf(lon) +
                         DELIM + String.valueOf(accuracy);
        writeToFile(logFileNameLocationUpdate, content);
    }


    /* Location Manager Updates from Discovery Algorithm */
    public int[] getCountDiscoveryReceiveLocationUpdate() {
        int[] locationUpdate = new int[LOCATION_UPDATE_ZONE_SIZE];

        locationUpdate[LOCATION_ZONE_25M] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_25M);
        locationUpdate[LOCATION_ZONE_50M] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_50M);
        locationUpdate[LOCATION_ZONE_100M] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_100M);
        locationUpdate[LOCATION_ZONE_500M] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_500M);
        locationUpdate[LOCATION_ZONE_1000M] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_1000M);
        locationUpdate[LOCATION_ZONE_LARGE] = getCount(COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_LARGE);

        return locationUpdate;
    }
    public void logDiscoveryReceiveLocationUpdate(double lat, double lon, int accuracy) {
        String dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_LARGE;

        if (accuracy <= ACCURACY_25M) {
            dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_25M;
        } else if (accuracy <= ACCURACY_50M) {
            dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_50M;
        } else if (accuracy <= ACCURACY_100M) {
            dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_100M;
        } else if (accuracy <= ACCURACY_500M) {
            dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_500M;
        } else if (accuracy <= ACCURACY_1000M) {
            dataKey = COUNT_DISCOVERY_RECEIVE_LOCATION_UPDATE_1000M;
        }

        incrementCount(dataKey);

        long timestamp = systemTime.currentTimeMillis();
        String content = String.valueOf(timestamp) + DELIM + TYPE_DISCOVERY +
                         DELIM + String.valueOf(lat) +
                         DELIM + String.valueOf(lon) +
                         DELIM + String.valueOf(accuracy);
        writeToFile(logFileNameLocationUpdate, content);
    }

    /* Distance between our current location and POI when we detect leaving of POI */
    public int[] getCountLeavingDistance() {
        int[] leave = new int[LEAVING_ZONE_SIZE];
        leave[LEAVING_ZONE_50M] = getCount(COUNT_LEAVING_DISTANCE_50M);
        leave[LEAVING_ZONE_100M] = getCount(COUNT_LEAVING_DISTANCE_100M);
        leave[LEAVING_ZONE_200M] = getCount(COUNT_LEAVING_DISTANCE_200M);
        leave[LEAVING_ZONE_1000M] = getCount(COUNT_LEAVING_DISTANCE_1000M);
        leave[LEAVING_ZONE_2000M] = getCount(COUNT_LEAVING_DISTANCE_2000M);
        leave[LEAVING_ZONE_LARGE] = getCount(COUNT_LEAVING_DISTANCE_LARGE);

        return leave;
    }
    public void logLeavingDistance(int distance) {
        String dataKey = COUNT_LEAVING_DISTANCE_LARGE;

        if (distance <= LEAVING_50M) {
            dataKey = COUNT_LEAVING_DISTANCE_50M;
        } else if (distance <= LEAVING_100M) {
            dataKey = COUNT_LEAVING_DISTANCE_100M;
        } else if (distance <= LEAVING_200M) {
            dataKey = COUNT_LEAVING_DISTANCE_200M;
        } else if (distance <= LEAVING_1000M) {
            dataKey = COUNT_LEAVING_DISTANCE_1000M;
        } else if (distance <= LEAVING_2000M) {
            dataKey = COUNT_LEAVING_DISTANCE_2000M;
        }

        incrementCount(dataKey);
    }

    /* Log different states */
    public int getCountStayInPoi() {
        return getCount(COUNT_STAY_IN_POI);
    }
    public void logStayInPoi() {
        incrementCount(COUNT_STAY_IN_POI);
    }

    public int getCountSwitchPoi() {
        return getCount(COUNT_SWITCH_POI);
    }
    public void logSwitchPoi() {
        incrementCount(COUNT_SWITCH_POI);
    }

    public int getCountEnterPoi() {
        return getCount(COUNT_ENTER_POI);
    }
    public void logEnterPoi() {
        incrementCount(COUNT_ENTER_POI);
    }

    public int getCountLeavePoi() {
        return getCount(COUNT_LEAVE_POI);
    }
    public void logLeavePoi() {
        incrementCount(COUNT_LEAVE_POI);
    }

    public int getCountFalseLeavePoi() {
        return getCount(COUNT_FALSE_LEAVE_POI);
    }
    public void logFalseLeavePoi() {
        incrementCount(COUNT_FALSE_LEAVE_POI);
    }

    public int getCountApproachingPoi() {
        return getCount(COUNT_APPROACHING_POI);
    }
    public void logApproachingPoi() {
        incrementCount(COUNT_APPROACHING_POI);
    }

    public int getCountNoWhere() {
        return getCount(COUNT_NOWHERE);
    }
    public void logNoWhere() {
        incrementCount(COUNT_NOWHERE);
    }

    /* This function is meant to log new celltowers that fall within POI radius, but are not
     * associated with a POI yet. The ALARM_TIMER_LOCATION_EXPIRED code flow is supposed to pick this up,
     * and this log is meant to log exactly that moment - when alarm timer location is expired and new cell
     * tower is learned. This happens in LocationSensorManager.checkLocationMatch(), when it calls
     * mDetection.checkPoiDistance().
     *
     * However, the problem is that LocationDetection.checkPoiDistance() also gets called to learn new poi cells
     * from LocationDetection.checkPoiState() as well, and there's no way to distinguish between the two.
     *
     * So until we change the code flow to distinguish the two, we are commenting this out
     */
    /*
    public int getCountNewPoiCell() {
    	return getCount(COUNT_NEW_POI_CELL);
    }
    public void logNewPoiCell() {
    	incrementCount(COUNT_NEW_POI_CELL);
    }
    */

    /* Count Beacon and WIFI scan */
    public int getCountBeaconScan() {
        return getCount(COUNT_BEACON_SCAN);
    }
    public void logBeaconScan() {
        incrementCount(COUNT_BEACON_SCAN);
    }

    public int getCountWifiScanResult() {
        return getCount(COUNT_WIFI_SCAN_RESULT);
    }
    public void logWifiScanResult() {
        incrementCount(COUNT_WIFI_SCAN_RESULT);
    }


    /* Count GeoAddress Cache Hit/Miss */
    public int getCountGeoAddressCacheHit() {
        return getCount(COUNT_GEO_ADDRESS_CACHE_HIT);
    }
    public void logGeoAddressCacheHit() {
        incrementCount(COUNT_GEO_ADDRESS_CACHE_HIT);
    }

    public int getCountGeoAddressCacheMiss() {
        return getCount(COUNT_GEO_ADDRESS_CACHE_MISS);
    }
    public void logGeoAddressCacheMiss() {
        incrementCount(COUNT_GEO_ADDRESS_CACHE_MISS);
    }

    public String getAppVersion() {
        return getString(APP_VERSION);
    }
    public void setAppVersion(String version) {
        setString(APP_VERSION, version);
    }
}
