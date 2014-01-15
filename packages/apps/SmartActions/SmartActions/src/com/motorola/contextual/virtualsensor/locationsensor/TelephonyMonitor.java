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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.TelephonyManagerWrapper;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.LogRecord;


/**
 *<code><pre>
 * CLASS:
 *  implements Telephony netowrk monitor
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *   * @link http://www.devx.com/wireless/Article/40524/1954
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public final class TelephonyMonitor implements MotoTelephonyListener {

    private static final String TAG = "LSAPP_TelMon";
    private static final int BOUNCING_CELL_SIZE = 5;

    private static final String[] 	CAPTIONS_GSM =  {"CntryISO", "NetOp", "NetTyp", "Cid", "Lac", "SigASU", "dBm"};
    private static final String[] 	CAPTIONS_CDMA = {"CntryISO", "NetOp", "NetTyp", "SysId", "BaseStnId", "BaseStnLat", "BaseStnLng", "NetId", "SigASU", "dBm"};
    private final static int CID_IDX = 3;       // the index of cid is 4th element in the caption
    private final static int BASESTN_IDX = 4;   // the index of basestnid is 5th element in the caption
    private static final String CID_CAPTION = CAPTIONS_GSM[CID_IDX];          // the idx 3 caption is Cid
    private static final String BASEID_CAPTION = CAPTIONS_CDMA[BASESTN_IDX];  // the idx 4
    
    private Context mContext;
    private LocationSensorManager mLSMan;
    private Handler mSensorManHdl;
    private Handler mDetectionHdl;
    private TelephonyManagerWrapper mTelMan;

    private MotoTelephonyStateListener 	mTelephonyListener = null;
    private	Values 						mCurValue;
    private	JSONObject 					mCurValueJson = null;
    private	JSONObject 					mLastValueJson = null;

    private List<String> mBouncingCells = new CopyOnWriteArrayList<String>();  // bounded cache size

    /**
     * Constructor
     * @param context
     * @param hdl
     */
    public TelephonyMonitor(LocationSensorManager lsman, Handler caphdl, Handler dethdl) {
        mContext = lsman;
        mLSMan = lsman;
        mSensorManHdl = caphdl;
        mDetectionHdl = dethdl;

        mTelMan = (TelephonyManagerWrapper) SystemWrapper.getSystemService(mContext, Context.TELEPHONY_SERVICE);

        mTelephonyListener 	= new MotoTelephonyStateListener();

        // the cached current network info data value
        mCurValue = new Values();
    }

    /**
     * register listener to get cell tower change event.
     */
    public void startTelMon() {
        mTelephonyListener.registerListener(mContext, this);
        LSAppLog.d(TAG, "startTelMon :: register TelMon Listener");
    }

    /**
     * remove listener to stop
     */
    public void stopTelMon() {
        mTelephonyListener.unregisterListener();
        LSAppLog.d(TAG, "stopTelMon :: unregister TelMon Listener");
    }

    /**
     * get the Telephony data state.
     * only connected state is taken as good state.
     */
    public boolean isDataConnectionGood() {
        return mTelMan.getDataState() == TelephonyManager.DATA_CONNECTED;
    }

    /**
     * get the phone device Id
     */
    public String getPhoneDeviceId() {
        return mTelMan.getDeviceId();
    }

    /**
     * bouncing cells are nearby cells that overlap at a location.
     * even though you not move, you got all the overlapped nearby cells through cell tower change
     * events, that is the purpose for bouncing cell cache to de-bounce those false positive
     * "cell tower changed" events.
     * we limited bouncing cell size, so overflow ones get evicted.
     * @param cells , e.g. {"Lac":"21988","CntryISO":"us","NetTyp":"GSM","NetOp":"310410","Cid":"66330845"}
     */
    private void shiftAddBouncingCells(String cells) {
        if (!mBouncingCells.isEmpty() && mBouncingCells.size() >= BOUNCING_CELL_SIZE) {
            mBouncingCells.remove(0);  // remove the oldest one on top.
        }
        mBouncingCells.add(cells);
    }

    /**
     * populate bouncing cache. This is called upon start up when we calibrated to the latest location from db.
     * @param cellset is && delimited string
     */
    public void populateBouncingCells(String dbcells) {
        Set<String> cellset = Utils.convertStringToSet(dbcells);  // create a set every cell push, expensive ?
        for (String cell : cellset) {
            shiftAddBouncingCells(cell);
        }
    }

    /**
     * the registered listener call back
     */
    public void onCellTowerChanged(GsmCellLocation location) {
        if (mTelMan == null)
            mTelMan = (TelephonyManagerWrapper) SystemWrapper.getSystemService(mContext, Context.TELEPHONY_SERVICE);

        // do not filter out invalid cell.
        // if (validGSMCellLocation(location) == false) { return; }

        // cache the cur to last before updating the cur.
        mLastValueJson = mCurValueJson;

        mCurValue.mNetworkCountryIso = mTelMan.getNetworkCountryIso();
        mCurValue.mNetworkOperator 	= mTelMan.getNetworkOperator();

        mCurValue.mGsm		= true;
        mCurValue.mCellId 	= location.getCid();
        try {
            mCurValue.mLac 	= location.getLac();
        } catch (Exception e) {
            mCurValue.mLac = -1;
        }

        mCurValueJson = mCurValue.getAsJSONObject();

        if (mLastValueJson != null) {
            LSAppLog.pd(TAG, "onCellTowerChanged:: GSM :: cur_val : " + mLastValueJson.toString() + " new_val: " + mCurValueJson.toString());
        } else {
            LSAppLog.pd(TAG, "onCellTowerChanged:: GSM :: cur_val : " + mCurValueJson.toString());
        }
        onChangeValues();
    }

    /**
     * the registered listener call back
     */
    public void onCellTowerChanged(CdmaCellLocation location) {
        if (mTelMan == null)
            mTelMan = (TelephonyManagerWrapper) SystemWrapper.getSystemService(mContext, Context.TELEPHONY_SERVICE);

        // cache the cur to last before updating the cur.
        mLastValueJson = mCurValueJson;

        mCurValue.mNetworkCountryIso 	= mTelMan.getNetworkCountryIso();
        mCurValue.mNetworkOperator 		= mTelMan.getNetworkOperator();

        mCurValue.mCdma					= true;
        mCurValue.mSystemId 			= location.getSystemId();
        mCurValue.mBaseStationId 		= location.getBaseStationId();
        mCurValue.mBaseStationLat 		= location.getBaseStationLatitude();
        mCurValue.mBaseStationLong		= location.getBaseStationLongitude();
        mCurValue.mNetworkId			= location.getNetworkId();
        mCurValue.mCellId				= 0;  // cdma has no cell id.

        mCurValueJson = mCurValue.getAsJSONObject();

        LSAppLog.pd(TAG, "onCellTowerChanged CDMA :: " + mCurValueJson.toString());

        onChangeValues();
    }

    /**
     * This snippet from
     * 		PhoneStateIntentReceiver.java:
     * 			For GSM, dBm = -113 + 2*asu
     * 				- ASU=0 means "-113 dBm or less"
     * 				- ASU=31 means "-51 dBm or greater"
     * 					Current signal strength in dBm ranges from -113 - -51dBm
     * 			Assume signal strength range from 0 to 31.
     */
    public void onSignalStrengthChangedSignificantly(int asu) {
        mCurValue.mSignalStrength = asu;
        mCurValue.mdbm = -113 + 2*asu;

        //onChangeValues();   // do not monitor signall strength for now
    }

    /**
     * the call back of registered on cell tower change event. process the cell tower change event.
     */
    private void onChangeValues() {
        boolean filter = false;
        int cellidx = 0;

        if( mCurValueJson == null){
            return;   // just to fix KW complain, paranoid programming
        }

        mLSMan.getMetricsLogger().logRawCellTowerChange(mCurValueJson.toString());

        // first, check whether we have seen the cell in our cache, filtering bouncing algorithm
        if ((cellidx = mBouncingCells.indexOf(mCurValueJson.toString())) >= 0) {
            filter = true;
            mBouncingCells.remove(cellidx);
            LSAppLog.d(TAG, "Telmon : celltower changed..cell has seen...filter out : " + mCurValueJson.toString());
        } else {
            if (null == mLSMan.getPendingIntent()) {
                // clear bouncing map if there is no pending timer
                LSAppLog.d(TAG, "Telmon : celltower changed...new cell...clear bouncing cells upon new cell and no pending timer ");
                emptyBouncingCells();
            }
        }

        //mBouncingCellsMap.put(mCurValueJson.toString(), Long.valueOf(nowtime));
        shiftAddBouncingCells(mCurValueJson.toString());

        // first, check whether cell tower really change, push to LSMan only when really changed
        if (mLastValueJson != null && mCurValueJson != null 
            && mCurValueJson.toString() != null && mCurValueJson.toString().equals(mLastValueJson.toString())
                // && mLastNBCellTowers != null && 0 == TelephonyMonitor.compareNBCellSet(mCurNBCellTowers, mLastNBCellTowers)
           ) {
            // push cell tower change notification to location sensor manager
            // no de-bounce here as assuming cell tower change wont be too intense
            filter = true;
            LSAppLog.d(TAG, "Telmon: celltower did not change based on last cell info...filter out" + mCurValueJson.toString());
        }

        if (filter == false || mCurValue.mCellId == -1) {   // let invalid cell get through
            LSAppLog.d(TAG, "Telmon : celltower changed....new cell..start start tracking !" + mCurValueJson.toString());
            sendNotification(mSensorManHdl, LocationSensorManager.Msg.CELLTOWER_CHANGED);
        }
        sendNotification(mDetectionHdl, LocationDetection.Msg.START_DETECTION);
    }

    // this is called after 15 min timer expired....restart new map monitoring
    private void emptyBouncingCells() {
        LSAppLog.pd(TAG, "emptyBouncingCells :: ");
        mBouncingCells.clear();
    }

    /**
     * Bouncing cell:
     * We recvd cell tower change events even when we are not moving. This might due to cell signal changes or other factor.
     * Those type of cell change event cause measurements to bounce between locations...and we want to de-bounce them.
     * get all current cells json str by creating a new set...too many set objects ?
     * @return  return the set of cell Ids (union of the current cell Id we were in prior to the bounce as well
     *  as all nearby cells). We won't be able to distinguish between the current cell Id we were in prior to the bounce
     *  from the other nearby cell Ids, however.
     */
    public Set<String> getBouncingCells() {
        // construct hashset containing the unique elements in the specified collection.
        HashSet<String> cellset = new HashSet<String>(mBouncingCells);

        // use iterator.remove while iterating over the set. do not use set.remove !
        for( Iterator<String> it = cellset.iterator(); it.hasNext(); ) {
            String s = it.next();
            if( ! isCellJsonValid(s)){   // remove the invalid cell.
                it.remove();  // remove the last object returned by next call from the iterator from the collection.
            }
        }
        LSAppLog.d(TAG, "getBouncingCells :: " + cellset.toString());
        return cellset;
    }

    /**
     * if celljson string is for gsm and there is a -1 unknown cell, then it is a invalid cell cell
     * for cdma, celljson is alway valid.
     * pre-condition : celljson must be a valid string.
     * celljson={"Lac":"21984","CntryISO":"us","NetTyp":"GSM","NetOp":"310410","Cid":"66618553"}
     * celljson={"NetTyp":"CDMA","BaseStnLat":"474524","NetOp":"31000","BaseStnId":"6259","BaseStnLng":"-1686648","CntryISO":"us","NetId":"54","SysId":"4"}
     */
    public static boolean isCellJsonValid(String celljson){
        boolean valid = true;  // default set to valid

        try{
            JSONObject celljsonobj = new JSONObject(celljson);  // do not need to check
            if( (celljsonobj.has(CID_CAPTION) && celljsonobj.getInt(CID_CAPTION) == -1 ) ||
                (celljsonobj.has(BASEID_CAPTION) && celljsonobj.getInt(BASEID_CAPTION) == -1)
            ){
                valid = false;   // -1, set false
            }
        }catch(JSONException e){
            LSAppLog.e(TAG, "isCellJsonValid : " + e.toString());
        }

        return valid;
    }

    /**
     * check whether a cell tower change event contains valid information.
     */
    public boolean isGsmCellLocationValid(GsmCellLocation location) {
        boolean valid = true;

        // first, filter out invalid cell change event
        if (location.getCid() <= 0) {
            LSAppLog.d(TAG, "cell changed GSM :: but invalid cell id -1, filter out...");
            valid = false;
        }
        if (location.getLac() == 0xFFFE || location.getLac() == 0) {
            LSAppLog.d(TAG, "cell changed GSM :: but invalid lac either 0xFFFE or 0b..filter out");
            valid = false;
        }

        return valid;
    }

    /**
     * @return cell tower json value string
     */
    public String getValueJSONString() {
        return mCurValueJson == null ? null : mCurValueJson.toString();
    }

    public Values getCurrentLocationValue() {
        return mCurValue;
    }

    // send notification to location sensor manager
    private void sendNotification(Handler handle, int what) {
        //LSAppLog.i(TAG, "sendNotification :: " + what);
        Message msg = handle.obtainMessage();
        msg.what = what;
        //msg.obj = obj;
        //msg.setData(data);
        handle.sendMessage(msg);
    }


    /**
     * encapsulate cell tower metadata into values class.
     */
    public static final class Values {

        public String 	mNetworkCountryIso 	= "";
        public String 	mNetworkOperator 	= "";

        public int 		mSignalStrength 	= 0;
        public int 		mdbm				= 0;

        // GSM
        public boolean	mGsm				= false;
        public int 		mCellId 			= 0;
        public int 		mLac 				= 0;

        // CDMA
        public boolean	mCdma				= false;
        public int 		mSystemId			= 0;
        public int 		mBaseStationId		= 0;
        public int 		mBaseStationLat		= 0;
        public int 		mBaseStationLong	= 0;
        public int 		mNetworkId			= 0;


        /** size */
        public int size() {
            if (mGsm)
                return CAPTIONS_GSM.length;
            else if (mCdma)
                return CAPTIONS_CDMA.length;
            else
                return 0;
        }

        /**
         * GSM capture metadata
         */
        public String getCaption(int ix) {
            if (mGsm)
                return CAPTIONS_GSM[ix];
            else if (mCdma)
                return CAPTIONS_CDMA[ix];
            else
                return "??";

        }

        /**
         *
         * <pre>
         * 0 - networkCountryIso
         * 1 - networkOperator
         * 2 - Cell Id
         * 3 - mLac
         * 4 - Signal Strength
         * 5 - dBm
         *
         */
        public String getAsString(int ix) {
            String result = "";

            if (mGsm) {
                if (ix <0 || ix>(CAPTIONS_GSM.length-1))
                    throw new IllegalArgumentException("GSM - Index of:"+ix+" is invalid, only 0 thru "+
                                                       (CAPTIONS_GSM.length-1)+" allowed");

                switch (ix) {
                case 0:
                    result = mNetworkCountryIso;
                    break;
                case 1:
                    result = mNetworkOperator;
                    break;
                case 2:
                    result = "GSM";
                    break;
                case 3:
                    result = mCellId+"";
                    break;
                case 4:
                    result = mLac+"";
                    break;
                case 5:
                    result = mSignalStrength+"";
                    break;
                case 6:
                    result = mdbm+"";
                    break;
                }
            } else if (mCdma) {
                if (ix <0 || ix>(CAPTIONS_CDMA.length-1))
                    throw new IllegalArgumentException("CDMA - Index of:"+ix+" is invalid, only 0 thru "+
                                                       (CAPTIONS_CDMA.length-1)+" allowed");

                switch (ix) {
                case 0:
                    result = mNetworkCountryIso;
                    break;
                case 1:
                    result = mNetworkOperator;
                    break;
                case 2:
                    result = "CDMA";
                    break;
                case 3:
                    result = mSystemId+"";
                    break;
                case 4:
                    result = mBaseStationId+"";
                    break;
                case 5:
                    result = mBaseStationLat+"";
                    break;
                case 6:
                    result = mBaseStationLong+"";
                    break;
                case 7:
                    result = mNetworkId+"";
                    break;
                case 8:
                    result = mSignalStrength+"";
                    break;
                case 9:
                    result = mdbm+"";
                    break;
                }

            }

            if (result == null)
                result = "";
            return result;
        }

        /* encap values into JSON object */
        public JSONObject getAsJSONObject() {
            int idx = 0;
            JSONObject jsonobj = new JSONObject();

            try {
                for (idx=0; idx<size()-2; idx++)  // do not include last two signal strength value
                    jsonobj.put(getCaption(idx), getAsString(idx));
            } catch (JSONException e) {
                LSAppLog.e(TAG, "getAsJSONObject() Error: " + e.toString());
            }
            return jsonobj;
        }

    }

    /**
     * nearby cell tower information. Tri-Angulate algorithm. Relies on Android totally for now.
     */
    @Deprecated
    static final class CellTowersNearby { // extends ArrayList<NeighboringCellInfo> {
        private static final long serialVersionUID = -2792819514428226677L;

        final static class CellTowerIdValue implements Comparable<CellTowerIdValue> {
            //private NeighboringCellInfo cellInfo;
            public int cid;
            public int lac;

            public CellTowerIdValue(int cid, int lac, NeighboringCellInfo cellInfo) {
                this.cid = cid;
                this.lac = lac;
                //this.cellInfo = cellInfo;
            }

            // this method called to ensure hashset uniqueness!!!
            public int compareTo(CellTowerIdValue o) {
                //LSAppLog.i(TAG, "CellTower NBs : compareTo :: this : " + cid + " : " + lac + " object : " + o.cid + " : " + o.lac);
                if (cid == o.cid && lac == o.lac)
                    return 0;
                else
                    return 1;
            }

            /*
            public String[] getDetailValues() {
            	String[] result = new String[4];
            	result[0] = cellInfo.getCid()+"";
            	result[1] = cellInfo.getPsc()+"";
            	result[2] = cellInfo.getRssi()+"";
            	result[3] = cellInfo.getNetworkType()+"";
            	return result;
            }
            */

            @Override
            public int hashCode() {
                return (int)(cid * 100 + lac);
            }

            @Override
            public boolean equals(Object o) {
                if ( this == o ) return true;
                if ( !(o instanceof CellTowerIdValue) ) return false;

                if (0 == compareTo((CellTowerIdValue)o))
                    return true;
                else
                    return false;
            }
        }

        private Set<CellTowerIdValue> mNBCellSet = new HashSet<CellTowerIdValue>();

        @Deprecated
        private CellTowersNearby() {
            super();
        }

        @Deprecated
        public void updateNBCellSet(List<NeighboringCellInfo> collection) {
            try {
                Iterator<NeighboringCellInfo> iter = collection.iterator();
                //LSAppLog.i(TAG, "updateNBCells started!!! ");

                while (iter.hasNext()) {
                    NeighboringCellInfo cinfo = iter.next();
                    mNBCellSet.add(new CellTowerIdValue(cinfo.getCid(), cinfo.getLac(), cinfo));
                    //LSAppLog.i(TAG, "updateNBCells :: new NB ::" + cinfo.getCid() + " :: " + cinfo.getLac());
                }

                //LSAppLog.i(TAG, "updateNBCells end!!! ");
            } catch (Exception e) {
                LSAppLog.e(TAG, "updateNBCells :: Exception : " + e.toString());
            }
        }

        @Deprecated
        public Set<CellTowerIdValue> getNBCellSet() {
            return mNBCellSet;
        }

        public String[] getDetailValues() {
            // N/A
            return null;
        }

        public String getLoggingRecordName() {
            return null;
        }

        public String[] getAsStringArray() {
            return null;
        }
    }


    @SuppressWarnings( { "unchecked", "unused" })
    private void captureNeighboringCells() {

        if (mTelMan == null)
            mTelMan = (TelephonyManagerWrapper) SystemWrapper.getSystemService(mContext, Context.TELEPHONY_SERVICE);

        try {
            Method m = mTelMan.getClass().getMethod("getNeighboringCellInfo", new Class[] {});
            m.setAccessible(true);
            Object o = m.invoke(mTelMan, new Object[] {});
            if (o instanceof ArrayList<?>) {

                ArrayList<NeighboringCellInfo> arrayList = (ArrayList<NeighboringCellInfo>) o;
                if (arrayList != null && arrayList.size() >0) {
                    Iterator iter = arrayList.iterator();
                    while (iter.hasNext()) {
                        NeighboringCellInfo n = (NeighboringCellInfo)iter.next();
                        //if (LOG_DEBUG) Log.i(TAG, TAG+".capture xxx cid="+n.getCid()+" lac="+n.getLac()+" typ="+n.getNetworkType()+" pac="+n.getPsc()+" rss="+n.getRssi());
                    }
                }
            }
        } catch (Exception e) {
            LSAppLog.e(TAG, "Couldn't call updateProviders" + e.toString());
        }
    }


    /**
     * reserved for future use.
     */
    @SuppressWarnings("unused")
    private class CellTowerNearby {
        private NeighboringCellInfo cellInfo;

        public CellTowerNearby(NeighboringCellInfo cellInfo) {
            super();
            this.cellInfo = cellInfo;
        }

        public void addRow(LogRecord record) {
            // TODO Auto-generated method stub
        }

        public String[] getDetailValues() {
            String[] result = new String[4];
            result[0] = cellInfo.getCid()+"";
            result[1] = cellInfo.getPsc()+"";
            result[2] = cellInfo.getRssi()+"";
            result[3] = cellInfo.getNetworkType()+"";
            return result;
        }

        public String getLoggingRecordName() {
            return CellTowerNearby.class.getSimpleName();
        }

        public String[] getAsStringArray() {
            // TODO: Write this
            return null;
        }

    }

}
