/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 * w04917        2012/02/17 IKHSS7-8262       Use the new Checkin API.
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.json.JSONArray;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.SystemClock;
import android.text.format.DateUtils;

import com.motorola.contextual.checkin.CheckinData;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.LocTimeTable.Tuple;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabaseHelper;


/**
 *
 *<code><pre>
 * CLASS:
 *  This singleton class implements location store and location consolidate algorithm.
 *  location consolidate means we replace location with low accuracy by location with high accuracy
 *  in the database if two locations are within the same vicinity.
 *
 * RESPONSIBILITIES:
 * 	accuracy correction and consolidate every day 4am.
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public final class LocationStore {
    private static final String TAG = "LSAPP_Heal";

    private static final long RECENTLY = System.currentTimeMillis() - (30*DateUtils.DAY_IN_MILLIS);  // last month

    private Context mContext;
    private LocationSensorManager mLSMan;
    private AlarmManager mAlarmMan;
    private PendingIntent mTimerExpiredIntent = null;
    private LocationDatabaseHelper mDbHelper;

    private ExecutorService mExecutor;

    private static String[] LOCTIME_DB_COLUMNS = LocationDatabase.LocTimeTable.Columns.getNames();

    private List<CheckInLoc> mCheckinLoc = new ArrayList<CheckInLoc>();

    /**
     * wrap each location into this entry for checkin to database
     * starttime, endtime, and accuname is not used for now...reserved for future use.
     */
    private static class CheckInLoc {
        private double  lat;
        private double  lgt;
        private long	acc;
        private long	count;

        public CheckInLoc (double lat, double lgt, long acc, long count) {
            this.lat = lat;
            this.lgt = lgt;
            this.acc = acc;
            this.count = count;
        }
        public String toString() {
            //return "lat=" + lat + ";lgt=" + lgt + ";stm=" + starttime + ";etm=" + endtime + ";duration=" + count  + ";name=" + accuname + ";count=" + freqcount;
            //return "lat=" + lat + ";lgt=" + lgt + ";stm=" + starttime + ";etm=" + endtime + ";duration=" + count  + ";name=" + accuname;
            return "lat=" + lat + ";lgt=" + lgt + ";acc=" + acc + ";dur=" + count;   // only checkin lat/lgt/acc/dur per Ilya
        }
        public String toStringEnc(String seed) {
            return EncryptUtils.encrypt(seed, this.toString());
            //return "lat=" + EncryptUtils.encrypt(seed, Double.toString(lat)) + ";lgt=" + EncryptUtils.encrypt(seed, Double.toString(lgt)) + ";acc=" + EncryptUtils.encrypt(seed, Long.toString(acc)) + ";dur=" + EncryptUtils.encrypt(seed, Long.toString(count));
        }
    }

    /**
     * VisitedPlace is a class that wraps Location Tuple for code readable.
     */
    private static class VisitedPlace {
        private Tuple mTuple;

        @SuppressWarnings("unused")
        private VisitedPlace() {}
        public VisitedPlace(Tuple t) {
            mTuple = t;
        }

        /**
         * return the location tuple
         */
        public Tuple getLocationTuple() {
            return mTuple;
        }

        /**
         * is other loc and this loc within
         * @param otherLoc   the passed in other loc
         * @return  true if within, false otherwise
         */
        public boolean isLocationWithin(VisitedPlace otherLoc) {

            boolean inside = true;

            // skip rows with 0 lat lng
            if (! Utils.compareDouble(this.mTuple.getLat(), 0.0) || ! Utils.compareDouble(otherLoc.mTuple.getLat(), 0.0) ) {
                LSAppLog.d(TAG, "_Id="+ this.getLocationTuple().get_id() + " : " + otherLoc.getLocationTuple().get_id() + "  zero lat lng");
                return false;
            }

            long maxAccuracyRadius = getMaxAccuracyRadius(otherLoc);
            float distBetween = Utils.distanceTo(mTuple.getLat(), mTuple.getLgt(), otherLoc.getLocationTuple().getLat(), otherLoc.getLocationTuple().getLgt());
            if (distBetween > maxAccuracyRadius) {
                inside = false;
            }

            LSAppLog.d(TAG, " ---- History Entry -----: ");
            LSAppLog.d(TAG, "_Id="+ otherLoc.getLocationTuple().get_id() + " : distance=" + distBetween + ", maxAccuracyRadius=" + maxAccuracyRadius + ", ::" + otherLoc.getLocationTuple().toString());
            return inside;   // yes, they are inside, can be consolidated
        }

        /**
         * compare accuracy radius of two location places and return the max of them
          * @param otherPlace
         * @return
         */
        public long getMaxAccuracyRadius(VisitedPlace otherPlace) {
            return Math.max(TARGET_RADIUS, Math.max(mTuple.getAccuracy(), otherPlace.getLocationTuple().getAccuracy()));
        }

        /**
         * whether this location is consolidable to the other location
         * @param otherPlace  the other location to consolidate to
         * @return  true if consolidable to, false otherwise.
         */
        public boolean isConsolidableTo(VisitedPlace otherPlace) {
            if (otherPlace.getLocationTuple().getAccuracy() <= mTuple.getBestAccuracy()) {
                return true;
            }
            return false;
        }

        /**
         * consolidate this location to the other location by updating accuname and bestaccuracy.
         * @param otherPlace
         */
        public void consolidateTo(VisitedPlace otherPlace, List<Tuple> modlist) {
            if (otherPlace.getLocationTuple().getAccuName().startsWith(Utils.DELIM)) {  // skip lat lng as addr tuples
                LSAppLog.d(TAG, " xxxx consolidated Location with invalid accuname : " + otherPlace.toString());
                return;
            }
            modlist.remove(mTuple);  //before modify and add, remove previously added, if exist.

            mTuple.setAccuName(otherPlace.getLocationTuple().getAccuName());  // update inner's value to best.
            mTuple.setPoiTag(otherPlace.getLocationTuple().getPoiTag());
            mTuple.setBestAccuracy(otherPlace.getLocationTuple().getAccuracy());

            modlist.add(mTuple);

            LSAppLog.d(TAG, " >>>> consolidated Location <<<< : " + mTuple.get_id() +  " ::  "  + mTuple.toString());
        }

        public String toString() {
            return mTuple.toString();
        }
    }

    /**
     * constructor instantiated from location manager
     * @param ctx
     * @param handler  location man handler
     * @param alarm    timer
     */
    @SuppressWarnings("unused")
    private LocationStore() { } // hide default constructor

    /**
     * This is the primary constructor, instantiated by location sensor manager upon start up.
     * When does this get constructed? once at 4am?
     *
     * @param ctx
     */
    public LocationStore(final Context ctx) {
        mContext = ctx;
        mLSMan = (LocationSensorManager)mContext;
        mAlarmMan = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        mDbHelper = LocationDatabaseHelper.getInstance(mContext);

        mExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    /**
     * called from location sensor onstart to setup heal job
     */
    public void scheduleConsolidationJob() {
        long delay = calculateDelaysUntilHealTime();
        long triggertime = SystemClock.elapsedRealtime() + delay;

        mAlarmMan.cancel(mTimerExpiredIntent);
        Intent timeoutIntent = new Intent(ALARM_TIMER_SELF_HEALING_EXPIRED);
        //timeoutIntent.putExtra(LocationSensorManager.ALARM_TIMER_SET_TIME, System.currentTimeMillis());
        mTimerExpiredIntent = PendingIntent.getBroadcast(mContext, 0, timeoutIntent, 0);

        mAlarmMan.setRepeating(AlarmManager.ELAPSED_REALTIME, triggertime, AlarmManager.INTERVAL_DAY, mTimerExpiredIntent);
        LSAppLog.d(TAG, "scheduleHealingJob : repeat timer : " + delay/60000 + " min later...");
    }

    /**
     * save the current location information into location content provider.
     * and broadcast the newly discovered location.
     * Always return the newly saved tuple.
     */
    public Tuple storeDiscoveredLocation(double newlat, double newlgt, long accuracy, long starttime, String poi, String bouncingcelljsons, String wifissid) {
        Uri loctimeuri = LocationSensorProvider.LOCTIME_CONTENT_URI;
        LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation : " + " lat=" + newlat + ",lgt=" + newlgt + ",accuracy="+accuracy);

        ContentResolver cr = mContext.getContentResolver();
        long nowtime = System.currentTimeMillis();
        ContentValues loctimeval = new ContentValues();
        String addr = "";

        Tuple healedtuple = findClosestOverlappingLocation(newlat, newlgt, accuracy);
        if (healedtuple == null || ( accuracy > 0 && healedtuple.getAccuracy() >= accuracy)) {  // dont take when accuracy is bad.
            addr = mLSMan.getLocationAddress(newlat, newlgt);  // get to net only no closest, or closest accu is off.
            LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation : getaddr from Geocode due to low accuracy :: fixAccuracy="+accuracy + ",dbaccuracy=" + (healedtuple == null ? 0:healedtuple.getAccuracy()) + ",GeoAddr="+ addr);
            if (addr.length() == 0 && healedtuple != null) {
                addr = healedtuple.getAccuName();
                LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation : getaddr from database after blank addr:" + addr);
                mLSMan.getMetricsLogger().logGeoAddressCacheMiss();
            } else if (addr.length() > 0) {
                mLSMan.getMetricsLogger().logGeoAddressCacheHit();
            }
        } else {
            addr = healedtuple.getAccuName();
            LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation : getaddr from database :" + addr);
        }

        // if google failed with blank addr and no record in db...not a meaningful location, dont care.
        if (addr.length() == 0) {
            LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation :: not store upon blank addr :: " + newlat + "::" + newlgt);
            addr = Utils.DELIM + newlat + Utils.DELIM + newlgt + Utils.DELIM + accuracy;  // put lat lng as addr
            //return null;
        }

        loctimeval.clear();
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.LAT, newlat);
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.LGT, newlgt);
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.ACCURACY, accuracy);
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.BESTACCURACY, accuracy);  // best accuracy is real accuracy before consolidation.
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.NAME, addr);
        //loctimeval.put(LocationDatabase.LocTimeTable.Columns.ACCUNAME, addr);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.STARTTIME, starttime);  // insert using cell start time
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.ENDTIME, nowtime);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.COUNT, (nowtime-starttime)/DateUtils.MINUTE_IN_MILLIS + 1);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.POITAG, poi);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.CELLJSONVALUE, bouncingcelljsons);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.WIFISSID, wifissid);

        // craig's suggestion
        //        LocTimeTable.Tuple t = new LocTimeTable.Tuple(-1, newlat, newlgt, starttime, nowtime, addr,
        //        		(nowtime-starttime)/DateUtils.MINUTE_IN_MILLIS + 1, accuracy,
        //        		addr, poi, bouncingcelljsons);
        //        new LocTimeTable().insert(mContext, t);

        cr.insert(loctimeuri, loctimeval);
        LSAppLog.d(TAG, "storeAndBroadcastDiscoveredLocation :: inserting new loctime ::" + loctimeval.toString());

        //consolidateLocations(false);  // disable this. not club checkin. XXX Do not do heal any more.

        if (null == healedtuple) {  // fake a tuple contains what we stored in db to broadcast.
            healedtuple = new Tuple();
            healedtuple.setLat(newlat);
            healedtuple.setLgt(newlgt);
            healedtuple.setAccuracy(accuracy);
            healedtuple.setStartTime(starttime);
            healedtuple.setEndTime(nowtime);
            healedtuple.setAccuName(addr);
        }

        LSAppLog.dbg(mContext, DEBUG_INTERNAL, "Location Captured to database :: POI: " + poi + "::" + addr + "::(lat:lgt:accu:time)::" + newlat + "::" + newlgt + "::Accuracy : " + accuracy  + "::time:" + starttime);
        return healedtuple;
    }

    /**
     * save the current location information into location content provider.
     * and broadcast the newly discovered location.
     */
    public Uri storeWifiLocation(long starttime, String bouncingcelljsons, String wifissid) {
        Uri loctimeuri = LocationSensorProvider.LOCTIME_CONTENT_URI;

        ContentResolver cr = mContext.getContentResolver();
        long nowtime = System.currentTimeMillis();
        ContentValues loctimeval = new ContentValues();

        LSAppLog.d(TAG, "storeWifiLocation : "+ bouncingcelljsons + "::" + wifissid);

        loctimeval.clear();
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.LAT, 0);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.LGT, 0);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.ACCURACY, 0);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.BESTACCURACY, 0);  // best accuracy is real accuracy before consolidation.
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.STARTTIME, starttime);  // insert using cell start time
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.ENDTIME, nowtime);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.COUNT, (nowtime-starttime)/DateUtils.MINUTE_IN_MILLIS + 1);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.CELLJSONVALUE, bouncingcelljsons);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.WIFISSID, wifissid);

        Uri entry = cr.insert(loctimeuri, loctimeval);
        if (entry != null) {
            LSAppLog.d(TAG, "storeWifiLocation : "+ entry.getPath());
        }
        return entry;
    }


    /**
     *  only called upon cell tower bouncing push update message after we have discovered the current location.
     *  update the end time of current location entry based on (just starttime)
     * @param starttime   the time when we discover the location
     * @param nowtime     the time when we leave the discovered location
     */
    public void updateCurrentLocationDuration(long starttime, long nowtime) {
        ContentValues values = new ContentValues();
        Uri uri = LocationSensorProvider.LOCTIME_CONTENT_URI;

        ContentResolver cr = mContext.getContentResolver();

        //final String where  = "(" +  LocationDatabase.LocTimeTable.Columns.LAT + " = " + lat + " AND " + " )";
        final String where  = "(" +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " = " + starttime + " )";

        values.clear();
        values.put(LocationDatabase.LocTimeTable.Columns.ENDTIME, nowtime);
        values.put(LocationDatabase.LocTimeTable.Columns.COUNT, (nowtime-starttime)/DateUtils.MINUTE_IN_MILLIS); // in minutes

        try {
            int row = cr.update(uri, values, where, null);
            if (row < 0) {
                LSAppLog.d(TAG, "updateCurrentLocationDuration: did not find last discovered location record:" + where);
            }
            LSAppLog.d(TAG, "updateCurrentLocAccuTime :: update current location :: where " + where + "row=" + row +
                       " starttime : " + starttime );
        } catch (Exception e) {
            LSAppLog.d(TAG, "updateCurrentLocAccuTime :: Exception: " + e.toString());
        }

        return;
    }

    /**
     * get the lastest location info
     */
    public Tuple getLastLocation() {
        Tuple lastloc = null;
        String orderby = "_ID DESC";
        Cursor c = mContext.getContentResolver().query(LocationSensorProvider.LOCTIME_CONTENT_URI, LOCTIME_DB_COLUMNS, null, null, orderby);
        try {
            if (c != null && c.moveToFirst()) {
                lastloc = LocationDatabase.LocTimeTable.toTuple(c);
                LSAppLog.d(TAG, "getLastLocation::" + lastloc.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return lastloc;
    }

    /**
     * invoked from location manager to fix accuracy
     */
    public void consolidateLocations(boolean clubcheckin) {
        LSAppLog.d(TAG, "healLocationAccuracy : submit to executor ");
        try {
            HealTask t = new HealTask(clubcheckin);
            mExecutor.execute(t);  // submit to self heal right now!
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * this function is to find location with best accuracy. This is potentially to fix issue when
     * accuracy was not good at the time when user tag a poi...we can self healing.
     * @param poitag
     * @return
     */
    protected LocationDatabase.LocTimeTable.Tuple findMostAccuracyPoiTuple(String poitag) {
        String rawsql = "select * from loctime where lat!=0 and poitag = ? order by accuname, accuracy asc limit 1";
        return fetchFirstMatchRow(rawsql, poitag);
    }

    /**
     * find the most accurate loc entry using the raw sql statement and the arg
     * @param rawsql
     * @param arg
     * @return   LocTimeTable.Tuple
     */
    private LocationDatabase.LocTimeTable.Tuple fetchFirstMatchRow(String rawsql, String arg) {
        LocationDatabase.LocTimeTable.Tuple bestlocpoi = null;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor c = db.rawQuery(rawsql, new String[] {arg});

        try {
            if (c.moveToFirst()) {
                bestlocpoi = LocationDatabase.LocTimeTable.toTuple(c);
                LSAppLog.d(TAG, "findMostAccuracyLocTuple::" + bestlocpoi.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return bestlocpoi;
    }


    /**
     * this runnable gets run daily between 4-5am to consolidate discovered locations.
     * it loop thru all the discovered locations and consolidate them based on their distances and accuracy radius.
     * the consolidation algorithm takes a pair of locations, and check whether they are overlap based on their accuracy radiuses,
     * if they are overlap, then the two locations get consolidated to the one with higher accuracy.
     * Please refer to the HLD for more details on consolidation algorithm.
     */
    class HealTask implements Runnable {
        private boolean docheckin;
        public HealTask(boolean docheckin) {
            this.docheckin = docheckin;
        }

        public void run() {

            List<VisitedPlace> mLocList = new ArrayList<VisitedPlace>();

            // first, populate all the locations needs to be consolidated
            int freshRows = findRecentLocationForConsolidation(mLocList);

            //Utils.startProfiling();

            // now run thru the list to fix one by one, put the modified tuple to another list
            // freshRows = mLocList.size();
            freshRows = 1;  // IKCTXTAW-170, consolidate only the last location with the history.
            consolidateLocationToBestAccuracy(freshRows, mLocList);

            //int count = 0;
            //for(VisitedPlace e : mLocList){
            //	encryptTupleData(e.mTuple);
            //	count++;
            //}
            //LSAppLog.d(TAG, "consolidate runs: " + count);

            //Utils.stopProfiling();

            LSAppLog.d(TAG, "Consolidation : done consolidate rows:"+freshRows + " with docheckin :" + docheckin);
        }
    }

    /**
     * update the database with the consolidated locations after running consolidation algorithm.
     * update the consolidated accuname and bestaccuracy value.
     */
    private void updateDatabaseAfterConsolidation(List<Tuple> modlist) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues loctimeval = new ContentValues();

        db.beginTransaction();
        try {
            for (Tuple t : modlist) {
                long id = t.get_id();
                String where = "(" +  LocationDatabase.LocTimeTable.Columns._ID + " = " + id + " )";

                loctimeval.clear();
                loctimeval.put(LocationDatabase.LocTimeTable.Columns.ACCUNAME, t.getAccuName());
                loctimeval.put(LocationDatabase.LocTimeTable.Columns.BESTACCURACY, t.getBestAccuracy());
                db.update(LocationDatabase.LocTimeTable.TABLE_NAME, loctimeval, where, null);
                LSAppLog.d(TAG, "updateDatabaseAfterConsolidation entry : " + id + " :: value : " + t.getAccuName());
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LSAppLog.d(TAG, "updateDatabaseAfterConsolidation: exception:" + e.toString());
        } finally {
            db.endTransaction();
        }
    }

    /**
     * find last month's location list for consolidation.
     * return num of rows since yesterday on top, so we can do incremental consolidation algorithm.
     * @param loclist  list of locations to be populated
     * @return num of fresh rows since yesterday.
     */
    private int findRecentLocationForConsolidation(List<VisitedPlace> loclist) {
        int freshRow = 0;
        long fresh = System.currentTimeMillis() - (3*DateUtils.DAY_IN_MILLIS);  // consider last week as fresh for now.

        String where = "( " +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " >= " + RECENTLY + " )";
        String orderby = "_ID DESC";    // lastest entry on top.

        Cursor c = mContext.getContentResolver().query(LocationSensorProvider.LOCTIME_CONTENT_URI, LOCTIME_DB_COLUMNS, where, null, orderby);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                    if (t.getStartTime() > fresh) {
                        freshRow++;
                    }
                    //loclist.add(t);
                    loclist.add(new VisitedPlace(t));
                    LSAppLog.d(TAG, "findRecentLocations :" + t.toString());
                } while (c.moveToNext());
            } else {
                LSAppLog.d(TAG, "findRecentLocations : Empty Loc talbe : " + where);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }

        LSAppLog.d(TAG, "findRecentLocations : freshRow=" + freshRow + ", totalRow="+ loclist.size());
        return freshRow;
    }

    /**
     * Consolidate the newly captured n locations against a list of locations from within a month sorted by time desc.
     * For each new location, The algorithm calculate the distance of it to each of locations in the list, and merge
     * any pair of locations if dist is less than accuracy radius.
     * Update the consolidated accuname and best accuracy after merge.
     * Incremental because we only compare n newly captured locations against history set(m), which is nxm, rather than full set of m!
     *
     * @param freshRows num of newly captured locations that needs to be consolidated.
     * @param loclist   the list of locations to consolidate
     */
    private void consolidateLocationToBestAccuracy(int freshRows, List<VisitedPlace> loclist) {
        int count = 0;
        List<Tuple> modlist = new ArrayList<Tuple>();

        while (loclist.size() > 0 && freshRows > 0) {       // loop until we clean up all fresh locations on top against all the records
            VisitedPlace newLoc = loclist.remove(0);    		  // remove from the top.
            freshRows--;

            LSAppLog.d(TAG, "======= Top Entry =======: ");
            LSAppLog.d(TAG, newLoc.toString());

            // iterator thru the history of location measurement to consolidate
            for (VisitedPlace histLoc : loclist) {
                count++;

                if ( ! newLoc.isLocationWithin(histLoc) ) {
                    continue;
                }

                if (newLoc.isConsolidableTo(histLoc)) {
                    newLoc.consolidateTo(histLoc, modlist);
                } else if (histLoc.isConsolidableTo(newLoc)) {
                    histLoc.consolidateTo(newLoc, modlist);
                } else {
                    LSAppLog.d(TAG, " No Consolidation : " + newLoc.toString() + " :: " + histLoc.toString());
                }

                /** working logic, put here as backup
                 *
                // case 1, when history's actual is better than newloc's real, merge newloc to the best of history
                if (histLoc.getAccuracy() <= newLoc.getBestAccuracy()) {
                    consolidateTwoLocations(newLoc, histLoc, modlist);
                }
                // case 2, when newLoc is really better than the best of history, merge histLoc to newLoc
                else if ( newLoc.getAccuracy() <= histLoc.getBestAccuracy()) {
                    consolidateTwoLocations(histLoc, newLoc, modlist);
                } else {
                    LSAppLog.d(TAG, " No Consolidation : " + newLoc.toString() + " :: " + histLoc.toString());
                }
                */
            } // end of one pass iteration of comparings for one newly captured location.

        } // end of consolidation of all newly captured location against locations in db.

        // after getting the best accuracy, now need to update all the need to be fixed entries in modlist
        if (modlist.size() > 0) {
            updateDatabaseAfterConsolidation(modlist);
        } else {
            LSAppLog.d(TAG, "Consolidation: nothing changed...");
        }

        LSAppLog.d(TAG, "Consolidation : done consolidateLocationToBestAccuracy rows:"+freshRows + " total calls:" + count);
    }

    /**
     * is one location inside another based on the their distance and accuracy radius
     * inside defined as dist(a,b) < Max(Ra, Rb)
     * return true if they are inside, can be consolidated, false otherwise.
     */
    @SuppressWarnings("unused")
    private boolean areLocationsInsideMaxRadius(final Tuple newLoc, final Tuple histLoc) {
        boolean inside = true;

        long maxAccuracyRadius = getMaxAccuracyRadius(histLoc, newLoc);
        float distBetween = Utils.distanceTo(newLoc.getLat(), newLoc.getLgt(), histLoc.getLat(), histLoc.getLgt());
        if (distBetween > maxAccuracyRadius) {
            inside = false;
        }

        LSAppLog.d(TAG, " ---- History Entry -----: ");
        LSAppLog.d(TAG, "_Id="+ histLoc.get_id() + " : distance=" + distBetween + ", maxAccuracyRadius=" + maxAccuracyRadius + ", ::" + histLoc.toString());
        return inside;   // yes, they are inside, can be consolidated
    }

    /**
     * compare accuracy radius of two location spots and return the max of them
     */
    private static long getMaxAccuracyRadius(final Tuple t, final Tuple e) {
        long maxR = Math.max(t.getAccuracy(), e.getAccuracy());  // maxR is the non-modified current entry accuracy
        maxR = Math.max(maxR, TARGET_RADIUS);  // can not go down than 50m, everything within 50m radius considered one location.
        return maxR;
    }

    /**
     * consolidate location fromLoc to location toLoc by updating the fromLoc's accuname and bestaccuracy,
     * and add the modified fromLoc to the modlist for batch updates.
     * @param fromLoc
     * @param toLoc
     */
    @SuppressWarnings("unused")
    private void consolidateTwoLocations(Tuple fromLoc, Tuple toLoc, List<Tuple> modlist ) {

        modlist.remove(fromLoc);  //before modify and add, remove previously added, if exist.

        fromLoc.setAccuName(toLoc.getAccuName());  // update inner's value to best.
        fromLoc.setPoiTag(toLoc.getPoiTag());
        fromLoc.setBestAccuracy(toLoc.getAccuracy());

        modlist.add(fromLoc);

        LSAppLog.d(TAG, " >>>> consolidated Location <<<< : " + fromLoc.get_id() +  " ::  "  + fromLoc.toString());
    }

    /**
     * encrypt data for each col inside a tuple
     */
    void encryptTupleData(Tuple t) {
        String plaintext = t.toString();
        String encoded = EncryptUtils.encrypt(plaintext);
        String decoded = EncryptUtils.decrypt(encoded);
        if (decoded == null || !decoded.equals(plaintext)) {
            LSAppLog.d(TAG, "Encryption Failure: " + plaintext + " !== " + decoded);
        }
    }

    /**
     * Find a location in db that closest to passed in Geo lat/lgt with highest accuracy that is overlap.
     * only called when instantaneous correction in save current location to db.
     * @param lat
     * @param lgt
     * @param accuracy
     * @return null if there is no overlapping location.
     *
     * TODO: narrow the results using lat/long.
     *    which one is better ? adding a where clause in query, or math calculation of 4 floats(lat1/lng1, lat2/lng2).
     */
    protected Tuple findClosestOverlappingLocation(double lat, double lgt, long accuracy) {
        ContentResolver cr = mContext.getContentResolver();
        Tuple bestt = null;
        long maxR = Long.MAX_VALUE;
        float minDist = Float.MAX_VALUE, dist = 0;

        String where = "( " +  LocationDatabase.LocTimeTable.Columns.LAT + " != " + 0 + " AND "
                       +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " >= " + RECENTLY +
                       " )";
        LSAppLog.d(TAG, "findClosestLocation : lat="+lat+",lgt="+lgt+",accuracy="+accuracy+",where="+ where);

        Cursor c = cr.query(LocationSensorProvider.LOCTIME_CONTENT_URI, LOCTIME_DB_COLUMNS, where, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                    dist = Utils.distanceTo(t.getLat(), t.getLgt(), lat, lgt);
                    maxR = Math.max(accuracy, t.getAccuracy());
                    LSAppLog.d(TAG, "findClosestLocation : minDist : " + minDist + " dist:" + dist + " maxR : " + maxR + " lat:lgt:" + t.getLat() + " ::" + t.getLgt() + " ::" + t.toString());
                    if (dist < maxR && dist <= minDist) {
                        //take the db addr only when dist within the maxR radius AND new fix does not have accuracy or accuracy is more off!
                        minDist = dist;
                        bestt = t;
                        LSAppLog.d(TAG, "=== findClosestLocation Hit === : minDist : " + minDist + " lat:lgt:" + t.getLat() + " ::" + t.getLgt() + " ::" + t.toString());
                    }
                } while (c.moveToNext());
            } else {
                LSAppLog.d(TAG, "findClosestLocation : Empty Loc talbe : " + where);
            }
        } catch (Exception e) {
            LSAppLog.d(TAG, "findClosestLocation : exception: " + e.toString());
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return bestt;
    }

    /**
     * collect all the wifis around the locatoin indicated by lat/lng/accu
     * @param wifijsonarray, output stored in this datastructure. a list of wifijsonarray.
     * @param celljsonset, output stored in this datastructur the set contains all the cell jsons.
     * @return merged unioned results in the passed in collection arguments.
     */
    @Deprecated
    protected void collectCellJsonWifiSsid(double lat, double lgt, long accuracy, JSONArray wifijsonarray, Set<String> celljsonset) {
        float dist = 0;
        List<VisitedPlace> mLocList = new ArrayList<VisitedPlace>();
        findRecentLocationForConsolidation(mLocList); // first, populate all the locations needs to be consolidated

        for (VisitedPlace p : mLocList) {
            dist = Utils.distanceTo(p.mTuple.getLat(), p.mTuple.getLgt(), lat, lgt);
            if (p.mTuple.getAccuracy() <= LOCATION_DETECTING_DIST_RADIUS &&
                    dist <= Math.max(Math.max(accuracy, p.mTuple.getAccuracy()), TARGET_RADIUS)) {
                // now merge update passed in collections of cell and wifi
                Utils.mergeSets(celljsonset, Utils.convertStringToSet(p.mTuple.getCellJsonValue()));
                JSONUtils.mergeJsonArrays(wifijsonarray, JSONUtils.getJsonArray(p.mTuple.getWifiSsid()), false); // no update ss
            }
        }
        return;
    }

    /**
     * collect all the wifis around the locatoin indicated by poi's wifi ssid
     * @param poiWifiJsonArray, output stored in this datastructure. a list of wifijsonarray.
     * @param celljsonset, output stored in this datastructur the set contains all the cell jsons.
     * @return merged unioned results in the passed in collection arguments.
     */
    protected void collectCellJsonWifiSsid(String initwifi, JSONArray poiWifiJsonArray, Set<String> celljsonset) {
        Map<String, String> poiWifiMap = new HashMap<String, String>(); // key=bssid
        JSONUtils.convertJSonArrayToMap(initwifi, poiWifiMap, null, null);
        Set<String> poiWifiSet = poiWifiMap.keySet();

        List<VisitedPlace> mLocList = new ArrayList<VisitedPlace>();
        findRecentLocationForConsolidation(mLocList); // first, populate all the locations needs to be consolidated
        int matchcount = 0;
        for (VisitedPlace p : mLocList) {
            matchcount = Utils.intersectSetJsonArray(poiWifiSet, null, p.mTuple.getWifiSsid());
            if (matchcount >= FUZZY_MATCH_MIN) {
                Utils.mergeSets(celljsonset, Utils.convertStringToSet(p.mTuple.getCellJsonValue()));
                JSONUtils.mergeJsonArrays(poiWifiJsonArray, JSONUtils.getJsonArray(p.mTuple.getWifiSsid()), false);  // no update ss
            }
        }
        return;
    }

    /**
     * real time checkin location info upon leaving a location.
     * If the user not opted in, do not check in any location data
     * @param Tuple  location tuple to be checked in.
     * If the passed in location tuple is null, then checkin the last entry from database with the consolidated lat/lng.
     */
    public void checkinUponLeavingLocation(Tuple loctuple) {
        if(!mLSMan.isUserOptedIn()) {   // do not check in location data if user not opted in
            return;
        }

        Tuple besttuple = null;
        Tuple lasttuple = null;
        if (loctuple == null) { // checkin the last entry in db upon null passed in entry.
            lasttuple = getLastLocation();
            // do nothing if no tuple, or location is wifi location.
            if (lasttuple == null || lasttuple.getAccuName() == null) {
                LSAppLog.d(TAG, "checkinUponLeavingLocation :: lastest Location is null...do nothing");
                return;
            }
            LSAppLog.d(TAG, "checkinUponLeavingLocation :: lastest Location: "+lasttuple.toString());

            String rawsql = "select * from loctime where lat!=0 and accuname = ? order by accuname, accuracy asc limit 1";
            besttuple = fetchFirstMatchRow(rawsql, lasttuple.getAccuName());
            if (null == besttuple) {
                LSAppLog.d(TAG, "checkinUponLeavingLocation :: no matching row. Impossible as we just checked lastlocation is valid");
                return;
            }
        } else { // else checkin the passed in location
            besttuple = loctuple;
            lasttuple = loctuple;
        }

        CheckInLoc entry = new CheckInLoc(besttuple.getLat(), besttuple.getLgt(), lasttuple.getAccuracy(), lasttuple.getCount());
        LSAppLog.d(TAG, "checkinUponLeavingLocation: " + entry.toString());

        mCheckinLoc.clear();
        mCheckinLoc.add(entry);

        checkinLocations(mCheckinLoc);
    }

    /**
     * @param locations list of location metadatas to be checked in.
     */
    private void checkinLocations(Collection<CheckInLoc> locations) {
        if (locations != null && !locations.isEmpty()) {
            for (CheckInLoc entry : locations) {
                if (entry != null) {
                    LSAppLog.d(TAG, "checkinData : " + entry.toString());
                    String encstr = entry.toStringEnc(mLSMan.getPhoneDeviceId());
                    if (encstr != null && !encstr.isEmpty()) {
                        CheckinData checkinData = new CheckinData(CHECKIN_TAG, CHECKIN_EVENT_DATA,
                                CHECKIN_EVENT_DATA_VERSION);
                        checkinData.setValue(CHECKIN_FIELD_ENC, encstr);
                        checkinData.checkin(CHECKIN_DATA_URI, mContext);
                    }
                }
            }
        }
    }

    /**
     * populate mcheckinloc list of CheckInLoc for checkin
     * Below sections are for checkin data, should have put it a standalone module
     */
    @SuppressWarnings("unused")
    private List<CheckInLoc> populateCheckinLoc() {
        ContentResolver cr = mContext.getContentResolver();

        long lately = System.currentTimeMillis() - (1*DateUtils.DAY_IN_MILLIS);  // last day
        String where = "( " +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " >= " + lately + " )";

        LSAppLog.d(TAG, "LOCTIME_CONTENT_AGGREGATE_URI : " + LocationSensorProvider.LOCTIME_CONTENT_AGGREGATE_URI.toString());
        //Cursor c = cr.query(LocationSensorProvider.LOCTIME_CONTENT_AGGREGATE_URI, null, where, null, null);
        Cursor c = cr.query(LocationSensorProvider.LOCTIME_CONTENT_URI, LOCTIME_DB_COLUMNS, where, null, null);
        if ( c != null ) {
            mCheckinLoc.clear();
            try {
                if (c.moveToFirst()) {
                    do {
                        Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                        CheckInLoc entry = new CheckInLoc(t.getLat(), t.getLgt(), t.getAccuracy(), t.getCount());
                        LSAppLog.d(TAG, "CheckinLocInfo: " + entry.toString());
                        mCheckinLoc.add(entry);
                    } while (c.moveToNext());
                } else {
                    LSAppLog.d(TAG, "checkinData : Empty Loc talbe : " + where);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        return mCheckinLoc;
    }

    /**
     * calculate how long we need to  sleep before wake up and perform location consolidation.
     * @return  mill seconds so can be used to set timer.
     */
    private long calculateDelaysUntilHealTime() {
        long delay;

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        // give us somewhere between 4-5am.
        if (hour < HEAL_HOUR) {
            delay = HEAL_HOUR - hour;
        } else {
            delay = 24 - hour + HEAL_HOUR;
        }

        LSAppLog.d(TAG, "delayToHealTime : start_hour: " + hour + " delayy_hours :" + delay);

        return delay*DateUtils.HOUR_IN_MILLIS;
    }

    /**
     *  whether current time is daytime, (6am - 10pm),
     *  Coarse grained..it is for use only to keep poking alive timer during day time
     *  to ensure liveless. Anytime between get up and go to bed works.
     * @param curtime
     * @return true in daytime, false in night
     */
    public static boolean isTimeDayTime(long curtime) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);  // 24-hour clock

        if (hour < DAY_TIME_START || hour > DAY_TIME_END)
            return false;
        else
            return true;
    }

    /**
     * this function takes a log every time system restarted...so that we know our service gets killed and restarted by platform.
     */
    protected void logServiceRestarted() {
        ContentValues loctimeval = new ContentValues();

        Uri loctimeuri = LocationSensorProvider.LOCTIME_CONTENT_URI;
        ContentResolver cr = mContext.getContentResolver();

        loctimeval.clear();
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.LAT, 0);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.LGT, 0);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.STARTTIME, System.currentTimeMillis());  // insert using nowtime
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.ENDTIME, System.currentTimeMillis());
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.COUNT, 1);
        loctimeval.put(LocationDatabase.LocTimeTable.Columns.CELLJSONVALUE, "Service Restarted abnormally...last location entry invalid");

        cr.insert(loctimeuri, loctimeval);
        LSAppLog.i(TAG, "logServiceRestarted ::");
        LSAppLog.dbg(mContext, DEBUG_INTERNAL, "....service restarted !!...");
    }
}
