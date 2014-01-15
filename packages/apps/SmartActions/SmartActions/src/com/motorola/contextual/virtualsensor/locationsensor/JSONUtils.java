/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2011/02/27 IKCTXTAW-201		   Initial version
 */

package com.motorola.contextual.virtualsensor.locationsensor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

/**
 *<code><pre>
 * CLASS:
 *  Util functions to handle JSON document.
 *
 * RESPONSIBILITIES:
 * 	encapsulate, persistance, instantiation of JSON objects.
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public class JSONUtils {
    public static final String TAG = "LSAPP_UtilsJSON";

    /**
     * find whether a json array contains a json object with certain key.
     * the comparison is based on the string value of passed in key. If key is not provided, use the entire object's string value.
     * return true if found, false otherwise.
     * wifissid=[{"wifibssid":"00:14:6c:14:ec:fa","wifissid":"PInternet"},{...}, ... ]
     */
    public static int indexOfJSONObject(JSONArray jsonarray, JSONObject jsonobj, String key) {
        String objstr = null;
        if (key == null) {
            objstr = jsonobj.toString();
        } else {
            try {
                objstr = jsonobj.getString(key);
            } catch (JSONException e) {
                objstr = null;
                LSAppLog.e(TAG, "findJSONObject:  get key Exception: " + e.toString());
            }
        }

        // java is f* verbose...no expressive power!
        if (objstr != null) {
            objstr = objstr.trim();
            if (objstr.length() == 0) {
                return -1;
            }
        } else {
            LSAppLog.d(TAG, "findJSONObject:  empty key string! no found. ");
            return -1;
        }

        int size = jsonarray.length();
        JSONObject entry = null;
        String entrystr = null;
        for (int i=0; i<size; i++) {
            try {
                entry = jsonarray.getJSONObject(i);
                if (key == null) {
                    entrystr = entry.toString();
                } else {
                    entrystr = entry.getString(key);
                }
                if (entrystr != null) {
                    entrystr = entrystr.trim();
                }

                if (objstr.equals(entrystr)) {
                    LSAppLog.d(TAG, "findJSONObject: match :" + objstr);
                    return i;   // return immediately
                }
            } catch (JSONException e) {
                LSAppLog.e(TAG, "findJSONObject: getJSONObject Exception: " + e.toString());
                continue;
            }
        }
        return -1;
    }

    /**
     * convert json array string to jsonarray
     */
    public static JSONArray getJsonArray(String jsonstr) {
        JSONArray curjsons = null;
        if (jsonstr == null) {
            return null;
        }
        try {
            curjsons = new JSONArray(jsonstr);  // convert string back to json array
        } catch (JSONException e) {
            LSAppLog.e(TAG, "getJSONArray:" + e.toString());
        }
        return curjsons;
    }

    /**
     * merge the new json array into the existing json array.
     * @return existing json array with newjson array added
     */
    public static JSONArray mergeJsonArrays(JSONArray existingjsons, JSONArray newjsons, boolean updatess) {
        if (existingjsons == null)
            return newjsons;
        if (newjsons == null)
            return existingjsons;

        JSONObject newobj = null;
        String bssid = null;
        for (int i=0; i<newjsons.length(); i++) {
            try {
                newobj = newjsons.getJSONObject(i);
                bssid = newobj.getString(LS_JSON_WIFIBSSID);
                if(bssid == null) {
                    continue;  // bad new json object, skip
                }
                int idx = indexOfJSONObject(existingjsons, newobj, LS_JSON_WIFIBSSID);
                if(idx < 0) { // not found, insert newjson obj into json array
                    existingjsons.put(newobj);
                } else if(updatess) { // we need to update signal strength with the scan from discover
                    if(newobj.has(LS_JSON_WIFISS)) {
                        int newss = newobj.getInt(LS_JSON_WIFISS);
                        JSONObject oldobj = existingjsons.getJSONObject(idx);
                        oldobj.put(LS_JSON_WIFISS, newss);
                        LSAppLog.d(TAG, "mergeJsonArrays: update ss: " + newss + " : " + oldobj.toString());
                    }
                }
            } catch (JSONException e) {
                LSAppLog.e(TAG, "mergeJSONArrays: getJSONObject Exception: " + e.toString());
                continue;
            }
        }
        return existingjsons;
    }

    /**
     * merge two jsonarray string and return one json array string
     * curstr is the current json array in string format, newstr is the to be merged json array in string format.
     */
    public static String mergeJsonArrayStrings(String curstr, String newstr) {
        JSONArray curjsons = null;
        JSONArray newjsons = null;

        LSAppLog.d(TAG, "mergeJSONArrays:" + curstr + " =+= " + newstr);

        // merge shortcut, if either one is null, return the other.
        if (curstr == null)
            return newstr;
        if (newstr == null)
            return curstr;

        try {
            curjsons = new JSONArray(curstr);  // convert string back to json array
            newjsons = new JSONArray(newstr);
        } catch (JSONException e) {
            LSAppLog.e(TAG, "mergeJSONArrays:" + e.toString());
            return curstr;   // return the original curstr, no merge.
        }

        mergeJsonArrays(curjsons, newjsons, true);  // update ss using scanned ssid from discovery.

        return curjsons.toString();
    }

    /**
     * fuzzy match whether runtime cur wifi ssid jsonarray matches to static db wifi ssid jsonarray
     * match criteria : turn to positive if single match exist. can be more sophisticated.
     * wifissid=[{"wifibssid":"00:14:6c:14:ec:fa","wifissid":"PInternet"},{...}, ... ]
     * @param dbJsonStr  static db set
     * @param curJsonStr runtime current set
     * @return true if two array has common object, false otherwise.
     */
    @Deprecated
    public static boolean fuzzyMatchJsonArrays(String dbJsonStr, String curJsonStr, String key) {
        LSAppLog.d(TAG, "fuzzyMatchJSONArrays : dbsdbjsonstret : " + dbJsonStr + " : curjsonstr :" +curJsonStr);
        if (dbJsonStr == null || curJsonStr == null) {
            return false;    // no match if either of them is null.
        }

        JSONArray dbjsons = null;
        JSONArray curjsons = null;
        try {
            dbjsons = new JSONArray(dbJsonStr);  // convert string back to json array
            curjsons = new JSONArray(curJsonStr);
        } catch (JSONException e) {
            LSAppLog.e(TAG, "mergeJSONArrays:" + e.toString());
            return false;   // no merge if either is wrong
        }

        boolean match = false;
        JSONObject curobj = null;
        for (int i=0; i<curjsons.length(); i++) {
            try {
                curobj = curjsons.getJSONObject(i);
            } catch (JSONException e) {
                LSAppLog.e(TAG, "mergeJSONArrays: getJSONObject Exception: " + e.toString());
                continue;  // skip this entry if can not construct object.
            }

            if(indexOfJSONObject(dbjsons, curobj, key) >= 0) {
                match = true;
                break;
            }
        }
        return match;
    }

    /**
     * get set of values from JSONArray with key, if key is null, get the string of each entire json object.
     * when you are using json, you are dealing with immutable string, no need Generic.
     * @return a set of values
     */
    public static Set<String> getValueSetFromJsonArray(JSONArray jsonarray, String key) {
        Set<String> valset = new HashSet<String>();
        if (jsonarray == null) {
            return valset;
        }

        JSONObject curobj = null;
        String valstr = null;
        for (int i=0; i<jsonarray.length(); i++) {
            try {
                curobj = jsonarray.getJSONObject(i);
                if (key == null) {
                    valstr = curobj.toString();
                } else {
                    valstr = curobj.getString(key);
                }
                valset.add(valstr);
                //LSAppLog.d(TAG, "getValueSetFromJSONArray: " + valstr);
            } catch (JSONException e) {
                LSAppLog.e(TAG, "getValueSetFromJSONArray: Exception: " + e.toString());
                continue;  // skip this entry if can not construct object.
            }
        }
        return valset;
    }

    /**
     * convert json object array to hash map with key is bssid and val is ssid. Json Array string format as follows.
     *   wifissid=[{"wifibssid":"00:14:6c:14:ec:fa","wifissid":"PInternet", "ss":-80, }, {...}, ... ]
     * @param outmap is defined by the caller and must not be null, contains the converted map
     * @param bagssid is defined by the caller and must not be null, contains the dup ssid name set.
     */
    public static void convertJSonArrayToMap(String ssidjsonarray, Map<String, String> outmap, Set<String> bagssid, Map<String, Integer> wifissmap) {
        JSONArray jsonarray = JSONUtils.getJsonArray(ssidjsonarray);

        if (jsonarray == null || outmap == null) {
            LSAppLog.d(TAG, "convertJSonArrayToMap: null jsonarray or map, return null");
            return;
        }

        JSONObject curobj = null;
        String bssid = null;
        String ssid = null;
        int ss = 0;

        for (int i=0; i<jsonarray.length(); i++) {
            bssid = null;
            ssid = null;
            ss = 0;
            try {
                curobj = jsonarray.getJSONObject(i);
                if(curobj.has(LS_JSON_WIFIBSSID))
                    bssid = curobj.getString(LS_JSON_WIFIBSSID);
                if(curobj.has(LS_JSON_WIFISSID))
                    ssid = curobj.getString(LS_JSON_WIFISSID);
                if(curobj.has(LS_JSON_WIFISS))
                    ss = curobj.getInt(LS_JSON_WIFISS);

                outmap.put(bssid, ssid);   // wifi bssid,ssid map

                if(bagssid != null && outmap.containsValue(ssid)) {
                    bagssid.add(ssid);   // populate bag ssid if exist. map containn value.
                }

                if(wifissmap != null && ss < 0) { // valid ss must be < 0
                    if(wifissmap.get(ssid) == null || wifissmap.get(ssid) < ss) { // replace with bigger value.
                        wifissmap.put(ssid, ss);  // use ssid a key	for signal strength treemap
                    }
                }
                //LSAppLog.d(TAG, "convertJSonArrayToMap: " + bssid + "::" + ssid + "::" + ss);
            } catch (Exception e) {  // catch both JSONException and Map related exception.
                LSAppLog.e(TAG, "convertJSonArrayToMap: Exception: " + e.toString());
                continue;  // skip this entry if can not construct object.
            }
        }
        LSAppLog.d(TAG, "convertJSonArrayToMap: " + ssidjsonarray + "::" + outmap.toString());
        return;
    }
}
