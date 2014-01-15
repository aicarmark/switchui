/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.PoiTable.Tuple;
import com.motorola.contextual.smartrules.R;


/**
 *<code><pre>
 * CLASS:
 *  This adapter is the only route to access POI information and
 *  implements content observer on Poi configuration changes as well as
 *
 * RESPONSIBILITIES:
 *   The looking up, and update of poi metadata all thru this adapter.
 *
 * COLABORATORS:
 *	POI
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class PoiAdapter {
    public static final String TAG = "LSAPP_POI";

    private final Context mContext;
    private final Handler mHandler;
    private final ContentResolver mResolver;
    private final PoiObserver mPoiObserver;

    public static final Uri URI = LocationSensorProvider.POI_CONTENT_URI;

    private List<Tuple> mPoiList;  // poi list is looked up constantly....populate it here.
    List<String> mHomeWorkSsid;
    private boolean mAllPoisHaveWifi = false;

    private static String[] POI_DB_COLUMNS = LocationDatabase.PoiTable.Columns.getNames();

    public PoiAdapter(Context ctx, Handler hdl) {
        mContext = ctx;
        mHandler = hdl;
        mResolver = ctx.getContentResolver();
        mPoiObserver = new PoiObserver(mHandler);
        // poi list is published to outside, to ensure thread-safe, wrap it with synchronized collection. Note: it only conditional thread-safe for individual ops, not a batch of ops.
        // http://stackoverflow.com/questions/561671/best-way-to-control-concurrent-access-to-java-collections
        mPoiList = Collections.synchronizedList(new ArrayList<Tuple>());   // never null
        mHomeWorkSsid = new ArrayList<String>();
        mAllPoisHaveWifi = false;

        registerPoiObserver();

        //refreshPoiList();  // prepare poi list upon start.
    }

    public class PoiObserver extends ContentObserver {
        public PoiObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfUpdate) {
            if (refreshPoiList()) {
                notifyDetectionPoiChanged();
            }
        }
    }

    private void registerPoiObserver() {
        mResolver.registerContentObserver(URI, true, mPoiObserver);
    }
    public void unregisterPoiObserver() {
        mResolver.unregisterContentObserver(mPoiObserver);
    }

    /**
     * callback of content observer whenever user tag a poi in database.
     * return true if number of row has been added or deleted
     */
    public boolean refreshPoiList() {
        boolean allwifi = true;
        int nrows = mPoiList.size();
        Cursor c = mResolver.query(URI, POI_DB_COLUMNS, null, null, null);  // query the entire table
        if (c != null) {
            mPoiList.clear();
            try {
                if (c.moveToFirst()) {
                    do {
                        Tuple curTuple = LocationDatabase.PoiTable.toTuple(c);
                        populatePoiStrongestSsid(curTuple);   // calculate strongest ssid for each tuple
                        mPoiList.add(curTuple);
                        //if(curTuple.getWifiSsid() == null) {
                        if( !((LocationSensorManager)mContext).mDetection.mDetCtrl.poiHasWifi(curTuple)) {
                            allwifi = false;  // set to false if any poi missing wifi
                        }
                        LSAppLog.d(TAG, "refreshPoiList :: " + mPoiList.get(mPoiList.size()-1).toString());
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                LSAppLog.e(TAG, "refreshPoiList Exception: " + e.toString());
                allwifi = false;
            } finally {
                c.close();
            }
        } else {
            LSAppLog.e(TAG, "refreshPoiList :: null cursor from POI content provider");
        }

        updateHomeWorkSsid();   // update the signal strength
        mAllPoisHaveWifi = allwifi;
        LSAppLog.d(TAG, "refreshPoiList :: exist_rows: " +  nrows + " added_rows: " + mPoiList.size());

        return true;  // IKSTABLE6-4701. Propagate change to detection, even no poi add/del but only lat/lng wifi cell changed.
    }

    /**
     * @return a list of Poi
     */
    public List<Tuple> getPoiList() {
        //return Collections.unmodifiableList(mPoiList);  // outside, read-only
        //return Collections.synchronizedList(mPoiList);
        return mPoiList;
    }

    /**
     * return true if all pois have wifi, false otherwise
     */
    public boolean areAllPoisHaveWifi() {
        return mAllPoisHaveWifi;
    }

    /**
     * return all poi data[poi, lat, lgt, radius, addr, name, cell] with the poi tage
     * @param poitag
     * @return
     */
    public LocationDatabase.PoiTable.Tuple getPoiEntry(String poitag) {
        if (mPoiList.size() == 0 || poitag == null) {
            return null;
        }
        for (Tuple t : mPoiList) {
            if (t.getPoiName().equals(poitag)) {
                return t;
            }
        }
        return null;
    }

    /**
     * LocationDetection calls with upmerged celljson.
     */
    public boolean updatePoi(LocationDatabase.PoiTable.Tuple poituple) {
        return updatePoi(poituple,false);
    }
    /**
     * Do not update the location name
     */
    public boolean updatePoi(LocationDatabase.PoiTable.Tuple poituple, boolean ignoreName) {
        if ( poituple != null) {
            ContentValues value = LocationDatabase.PoiTable.toContentValues(poituple,ignoreName);
            String where = "( " +  LocationDatabase.PoiTable.Columns._ID + " = " + poituple.get_id() +" )";
            mResolver.update(LocationSensorProvider.POI_CONTENT_URI, value, where, null);
            LSAppLog.d(TAG, "updatePoi : entry exist, update value:" + value.toString());
            return true;
        } else {
            LSAppLog.e(TAG, "updatePoi : empty entry : ");
            return false;
        }
    }

    public void notifyDetectionPoiChanged() {
        Message msg = mHandler.obtainMessage();
        msg.what = LocationDetection.Msg.POI_REFRESH;
        LSAppLog.i(TAG, "notifyDetectionPoiChanged...after refresh from POI observer.");
        mHandler.sendMessage(msg);
    }

    /**
     * from the current poi's bssid, populate the strongest ss wifi ssid to be used for PNO
     * @param curtuple
     * @return populate the topssid field in cur tuple
     */
    private List<String> populatePoiStrongestSsid(Tuple curtuple) {
        final Map<String, String> curPoiWifiMap = new HashMap<String, String>();     // to be populated
        final Map<String, Integer> curPoiWifiSSMap = new HashMap<String, Integer>(); // to be populated
        JSONUtils.convertJSonArrayToMap(curtuple.getWifiSsid(), curPoiWifiMap, null, curPoiWifiSSMap);  // populated
        int ssmapsize = curPoiWifiSSMap.size();
        curtuple.clearStrongestSsid();  // clear before adding

        LSAppLog.d(TAG, "populatePoiStrongestSsid:" + curPoiWifiMap.toString() + " " + curPoiWifiSSMap.toString());
        if(ssmapsize > 0) {
            List<String> l = new ArrayList<String>();
            l.addAll(curPoiWifiSSMap.keySet());
            Collections.sort(l, new Comparator<String>() {  // sort ssid's based on signa strength
                public int compare(String s1, String s2) {
                    int v1 = curPoiWifiSSMap.get(s1);
                    int v2 = curPoiWifiSSMap.get(s2);
                    return v2-v1;   // reverse order 3,2,1
                }
            });

            // after sorting, populate strongest ssid, for now, only pick 2 ssid due to PNO restriction.
            int slots = Math.min(PNO_SLOTS, ssmapsize);
            for(int i=0; i< slots; i++) {  // avoid list IndexOutOfBoundsException
                String ssid = l.get(i);    // null will be return if no mapping for the key
                if(ssid != null) {
                    curtuple.addStrongestSsid(ssid);
                }
            }
        }
        LSAppLog.d(TAG, "populatePoiStrongestSsid:" + curtuple.getTopSsid().toString());
        return curtuple.getTopSsid();
    }

    /**
     * check if a poi tuple is home or work based on poi name
     * return true if poi is either home or work, false else.
     */
    private boolean isInferredPoi(Tuple t) {
        String poiname = t.getPoiName();  // the poi column, the second one in table
        String home = mContext.getString(R.string.home_poitag);
        String work = mContext.getString(R.string.work_poitag);
        if(home.equals(poiname) || work.equals(poiname)) {
            return true;
        }
        return false;
    }

    /**
     * get the strongest signal ssid for the poi
     */
    private void updateHomeWorkSsid() {
        mHomeWorkSsid.clear();
        for(Tuple poituple : mPoiList) {
            if(isInferredPoi(poituple)) {
                mHomeWorkSsid.addAll(poituple.getTopSsid());
                LSAppLog.d(TAG, "updateHomeWorkSsid: " + poituple.getName() + " : " + mHomeWorkSsid);
            }
        }
    }

    @Deprecated
    private List<String> getHomeWorkStrongestSsid() {
        return mHomeWorkSsid;
    }

    @Deprecated
    private void setHomeWorkPnoFlag(boolean flag) {
        for(Tuple poituple : mPoiList) {
            if(isInferredPoi(poituple)) {
                poituple.setPnoRegistration(flag);
            }
        }
    }

    /**
     * once poi tuple is registered for pno detection, mark the flag
     * @param t: the poi tuple where the flag to be set. Never be null.
     * @param set: whether the pno registered flag should be set.
     */
    public void setPnoFlag(Tuple t, boolean set) {
        if(t != null) {
            t.setPnoRegistration(set);
        }
    }
}
