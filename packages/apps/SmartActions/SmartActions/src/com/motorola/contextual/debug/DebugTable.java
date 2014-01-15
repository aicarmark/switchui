package com.motorola.contextual.debug;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.provider.BaseColumns;
import android.util.Log;

import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.TupleBase;
import com.motorola.contextual.smartrules.util.Util;

public class DebugTable extends TableBase {


    private static final String TABLE_NAME = DebugTable.class.getSimpleName();
    private static final String TAG = TABLE_NAME;

    public static final String DEBUG_DB_URI_STR =
    				"content://com.motorola.contextual.analytics/debug";
    public static final Uri DEBUG_DB_URI = Uri.parse(DEBUG_DB_URI_STR);

    private static final String EXTRA_VALUES = "values";
    private static final String COMPONENT = "com.motorola.contextual.smartrules";
    private static final String RULE_UPDATER = "com.motorola.contextual.rulesupdater";
    private static final String PII_INFO = "PII";
    
    // The List of Action/Trigger publishers w/ privacy information.
    private static List<String> listPIIPublisher = Arrays.asList(
    		"sendmessage",
    		"notification",
    		"launchwebsite",
    		"autosms",
    		"VipRinger",
    		"location",
    		"missedcallsensor",
    		"calendareventsensor"); 

    /** These are the possible values for the "direction" column.
     */
    public interface Direction {
        /** the request you received was inbound to your component */
        public static final String IN = "in";
        /** this is a request you sent to another external component */
        public static final String OUT = "out";
        /** this is a debugging record for your component */
        public static final String INTERNAL = "internal";
    }

    public interface Columns extends BaseColumns {

        /** Direction of data flow in a component Values are In, Out, Internal */
        final String DIRECTION  	= "direction";
        /** date-time value when then record is created */
        final String TIME_STAMP 	= "timestamp";
        /** Unique identifier for a mode within the mode manager
         * (mode Manager RuleTable.Columns.Key field) */
        final String MODE_KEY		= "modekey";
        /** Unique identifier for a component */
        final String COMP_KEY		= "compkey";
        /** Unique identifier for an instance of a component */
        final String COMP_INST_KEY	= "compinstkey";
        /** Unique identifier for the component from which the data was received or sent to */
        final String FROM_TO		= "fromto";
        /** State information for each component. This could be either an internal state of
         * the component or the state data that it received or is sending */
        final String STATE			= "state";
        /** Only used by action components when a settings request is sent or response is received.
         * A unique id that can connect a request to a response */
        final String REQUEST_ID		= "requestid";
        /** Values  Pass, Fail, Error */
        final String REQUEST_STATUS	= "reqstatus";
        /** reserved for the URI that your component received or sent - if applicable*/
        final String DATA			= "data";
        /** Can store intents or other data that can help in debugging */
        final String DATA2			= "data2";
        final String DATA3			= "data3";
        final String DATA4			= "data4";
        final String DATA5			= "data5";

        final String[] NAMES = {_ID, DIRECTION, TIME_STAMP, MODE_KEY, COMP_KEY,
                                COMP_INST_KEY, FROM_TO, STATE, REQUEST_ID, REQUEST_STATUS,
                                DATA, DATA2, DATA3, DATA4, DATA5
                               };
    }


    public static class Tuple extends TupleBase {

        private String direction;
        private String timeStamp;
        private String modeKey;
        private String compKey;
        private String compInstKey;
        private String fromTo;
        private String state;
        private String requestId;
        private String reqStatus;
        private String data;
        private String data2;
        private String data3;
        private String data4;
        private String data5;



        public Tuple(Cursor cursor) {

            // _id
            int ix = cursor.getColumnIndex(Columns._ID);
            if (ix > -1)
                this._id = cursor.getLong(ix);

            // direction
            ix = cursor.getColumnIndex(Columns.DIRECTION);
            if (ix > -1)
                this.direction = cursor.getString(ix);

            // timestamp
            ix = cursor.getColumnIndex(Columns.TIME_STAMP);
            if (ix > -1)
                this.timeStamp = cursor.getString(ix);

            // compkey
            ix = cursor.getColumnIndex(Columns.COMP_KEY);
            if (ix > -1)
                this.compKey = cursor.getString(ix);

            // compinstkey
            ix = cursor.getColumnIndex(Columns.COMP_INST_KEY);
            if (ix > -1)
                this.compInstKey = cursor.getString(ix);

            // fromto
            ix = cursor.getColumnIndex(Columns.FROM_TO);
            if (ix > -1)
                this.fromTo = cursor.getString(ix);

            // state
            ix = cursor.getColumnIndex(Columns.STATE);
            if (ix > -1)
                this.state = cursor.getString(ix);

            // requestid
            ix = cursor.getColumnIndex(Columns.REQUEST_ID);
            if (ix > -1)
                this.requestId = cursor.getString(ix);

            // reqstatus
            ix = cursor.getColumnIndex(Columns.REQUEST_STATUS);
            if (ix > -1)
                this.reqStatus = cursor.getString(ix);

            // data
            ix = cursor.getColumnIndex(Columns.DATA);
            if (ix > -1)
                this.data = cursor.getString(ix);

            // data2
            ix = cursor.getColumnIndex(Columns.DATA2);
            if (ix > -1)
                this.data2 = cursor.getString(ix);

            // data3
            ix = cursor.getColumnIndex(Columns.DATA3);
            if (ix > -1)
                this.data3 = cursor.getString(ix);

            // data4
            ix = cursor.getColumnIndex(Columns.DATA4);
            if (ix > -1)
                this.data4 = cursor.getString(ix);

            // data5
            ix = cursor.getColumnIndex(Columns.DATA5);
            if (ix > -1)
                this.data5 = cursor.getString(ix);


        }

        public Tuple(String direction, String timestamp, String modekey,
                     String compkey, String compinstkey, String fromto,
                     String state, String requestid, String reqstatus, String data,
                     String data2, String data3, String data4, String data5) {

            super();
            this.direction = direction;
            this.timeStamp = timestamp;
            this.modeKey = modekey;
            this.compKey = compkey;
            this.compInstKey = compinstkey;
            this.fromTo = fromto;
            this.state = state;
            this.requestId = requestid;
            this.reqStatus = reqstatus;
            this.data = data;
            this.data2 = data2;
            this.data3 = data3;
            this.data4 = data4;
            this.data5 = data5;
        }



        /**
         * @return the direction
         */
        public String getDirection() {
            return direction;
        }



        /**
         * @param direction the direction to set
         */
        public void setDirection(String direction) {
            this.direction = direction;
        }



        /**
         * @return the timestamp
         */
        public String getTimeStamp() {
            return timeStamp;
        }



        /**
         * @param timestamp the timestamp to set
         */
        public void setTimeStamp(String timestamp) {
            this.timeStamp = timestamp;
        }



        /**
         * @return the modekey
         */
        public String getModeKey() {
            return modeKey;
        }



        /**
         * @param modekey the modekey to set
         */
        public void setModekey(String modekey) {
            this.modeKey = modekey;
        }



        /**
         * @return the compkey
         */
        public String getCompKey() {
            return compKey;
        }



        /**
         * @param compkey the compkey to set
         */
        public void setCompKey(String compkey) {
            this.compKey = compkey;
        }



        /**
         * @return the compinstkey
         */
        public String getCompInstKey() {
            return compInstKey;
        }



        /**
         * @param compinstkey the compinstkey to set
         */
        public void setCompInstKey(String compinstkey) {
            this.compInstKey = compinstkey;
        }



        /**
         * @return the fromto
         */
        public String getFromTo() {
            return fromTo;
        }



        /**
         * @param fromto the fromto to set
         */
        public void setFromTo(String fromto) {
            this.fromTo = fromto;
        }



        /**
         * @return the state
         */
        public String getState() {
            return state;
        }



        /**
         * @param state the state to set
         */
        public void setState(String state) {
            this.state = state;
        }



        /**
         * @return the requestid
         */
        public String getRequestId() {
            return requestId;
        }



        /**
         * @param requestid the requestid to set
         */
        public void setRequestId(String requestid) {
            this.requestId = requestid;
        }



        /**
         * @return the reqstatus
         */
        public String getReqStatus() {
            return reqStatus;
        }



        /**
         * @param reqstatus the reqstatus to set
         */
        public void setReqStatus(String reqstatus) {
            this.reqStatus = reqstatus;
        }



        /**
         * @return the data
         */
        public String getData() {
            return data;
        }



        /**
         * @param data the data to set
         */
        public void setData(String data) {
            this.data = data;
        }



        /**
         * @return the data2
         */
        public String getData2() {
            return data2;
        }



        /**
         * @param data2 the data2 to set
         */
        public void setData2(String data2) {
            this.data2 = data2;
        }



        /**
         * @return the data3
         */
        public String getData3() {
            return data3;
        }



        /**
         * @param data3 the data3 to set
         */
        public void setData3(String data3) {
            this.data3 = data3;
        }



        /**
         * @return the data4
         */
        public String getData4() {
            return data4;
        }



        /**
         * @param data4 the data4 to set
         */
        public void setData4(String data4) {
            this.data4 = data4;
        }



        /**
         * @return the data5
         */
        public String getData5() {
            return data5;
        }



        /**
         * @param data5 the data5 to set
         */
        public void setData5(String data5) {
            this.data5 = data5;
        }
    }


    @Override
    protected int[] getColumnNumbers(Cursor cursor, String sqlRef) {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * not applicable to external tables
     */
    @Override
    public String getFkColName() {
        return null;
    }


    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    @Override
    public <T extends TupleBase> ContentValues toContentValues(T _tuple) {

        ContentValues args = new ContentValues();
    	if (_tuple instanceof Tuple) {
	        Tuple tuple = (Tuple) _tuple;

	        if (tuple.get_id() > 0) {
	            args.put(Columns._ID, 						tuple.get_id());
	        }
	        args.put(Columns.DIRECTION, 					tuple.getDirection());
	        args.put(Columns.TIME_STAMP, 					tuple.getTimeStamp());
	        args.put(Columns.MODE_KEY, 						tuple.getModeKey());
	        args.put(Columns.COMP_KEY, 						tuple.getCompKey());
	        args.put(Columns.FROM_TO,						tuple.getFromTo());
	        args.put(Columns.STATE,							tuple.getState());
	        args.put(Columns.REQUEST_ID,					tuple.getRequestId());
	        args.put(Columns.REQUEST_STATUS,				tuple.getReqStatus());
	        args.put(Columns.DATA,							tuple.getData());
	        args.put(Columns.DATA2,							tuple.getData2());
	        args.put(Columns.DATA3,							tuple.getData3());
	        args.put(Columns.DATA4,							tuple.getData4());
	        args.put(Columns.DATA5,							tuple.getData5());
    	}
        return args;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T extends TupleBase> T toTuple(Cursor cursor, int[] colNumbers) {
        return (T) new Tuple(cursor);
    }
    
  /** writes the debug data to the debug provider
    *
    * @param context - application context
    * @param direction - DebugTable.Direction.(IN or OUT or Internal)
    * @param state - state of the application
    * @param modeName - data3 field
    * @param ruleKey - Rule key
    * @param fromTo - from and to
    * @param data - data that is being printed
    * @param data2 - data2 field data
    * @param compKey - component key
    * @param compInstKey - component instance key
    */
   public static void writeToDebugViewer(Context context, String direction, String state, String modeName,
		                                String ruleKey, String fromTo, String data, String data2,
                                        String compKey, String compInstKey) {

   	writeToDebugViewer(context, direction, state, modeName, ruleKey, fromTo,
   						data, data2, null, compKey, compInstKey, null, null);
   }

    /** writes the debug data to the debug provider
     *
     * @param context - application context
     * @param direction - DebugTable.Direction.(IN or OUT or Internal)
     * @param modeKey - mode key
     * @param compKey - component key
     * @param compInstKey - component instance key
     * @param fromTo - from and to
     * @param state - state of the application
     * @param requestId - request ID
     * @param requestStatus - request status
     * @param data - data that is being printed
     * @param data2 - data2 field data
     */
    public static void writeToDebugViewer(Context context, String direction, String modeKey,
                                         String compKey, String compInstKey, String fromTo, String state,
                                         String requestId, String requestStatus, String data, String data2) {

    	writeToDebugViewer(context, direction, state, null, modeKey, fromTo,
    						data, data2, null, compKey, compInstKey, requestId, requestStatus);
    }

    /** writes the debug data to the debug provider
     *
     * @param context - application context
     * @param direction - DebugTable.Direction.(IN or OUT or Internal)
     * @param state - state of the application
     * @param modeName - mode for which invoked
     * @param ruleKey - rule key in the Rule table
     * @param fromTo - from and to
     * @param data - data that is being printed
     * @param data2 - data2 field data
     * @param compKey - component key
     * @param compInstKey - component instance key
     * @param requestId - request ID
     * @param requestStatus - request status
     */
    public static void writeToDebugViewer(Context context, String direction,
                                          String state, String modeName,
                                          String ruleKey, String fromTo,
                                          String data, String data2, String data4,
                                          String compKey, String compInstKey,
                                          String requestId, String requestStatus) {

        if(LOG_DEBUG) Log.d(TAG, "writeToDebugViewer: Direction = "+direction+"; state = "+state+"; data3 = "+modeName
        			+"; ruleKey = "+ruleKey+"; from to = "+fromTo+"; data = "+data+"; data2 = " +data2+"; data4 = " +data4
        			+"; compKey = "+compKey+"; compInstKey = "+compInstKey+" requestId = "+requestId
        			+"; requestStatus = "+requestStatus);

        // Always log for Direction.IN and Direction.OUT. But for Direction.INTERNAL only log if
        // LOG_DEBUG is set to true.
        if((direction.equals(Direction.INTERNAL) && LOG_DEBUG)
        		|| (direction.equals(Direction.IN)) || (direction.equals(Direction.OUT))) {
        	
        	// Filter the Privacy information logged in production phones.
        	// Note - On Production phones only logs with Direction - IN/Out and component - Smartrules are allowded.
        	
        	state = filterPIIFromStateField(context, direction, state, compKey, compInstKey, data);
        	
        	// The Components are abusing the Checkin Logs and causing the SmartAction Metrics to break.
        	// Currently Filtering out the Rule Updater key from Checkin in production phones as it is not
        	// adding any value.
        	                               
        	if (ruleKey != null && ruleKey.equals(RULE_UPDATER))
        		return;

		    String[] packageDetails = Util.getPackageDetails(context);
		    
		    try{
		        ContentValues contentValues = new ContentValues();
		        contentValues.put(Columns.DIRECTION, direction);
		        contentValues.put(Columns.MODE_KEY, ruleKey);
		        contentValues.put(Columns.TIME_STAMP, new Date().getTime());
		        contentValues.put(Columns.COMP_KEY, compKey);
		        contentValues.put(Columns.COMP_INST_KEY, compInstKey);
		        contentValues.put(Columns.FROM_TO, fromTo);
		        contentValues.put(Columns.STATE, state);
		        contentValues.put(Columns.REQUEST_ID, requestId);
		        contentValues.put(Columns.REQUEST_STATUS, requestStatus);
		        contentValues.put(Columns.DATA, data);
		        contentValues.put(Columns.DATA2, data2);
		        contentValues.put(Columns.DATA3, modeName);
		        contentValues.put(Columns.DATA4, data4);
		        contentValues.put(Columns.DATA5, packageDetails[0]);
	
		        WriteToDebugViewer(context, contentValues);
		    } catch (Exception e) {
		    	Log.e(TAG, "Exception writing to debug viewer");
		    	e.printStackTrace();
		    }
        }
    }
    
    private static String filterPIIFromStateField(Context context, String direction, String state,
				String compKey, String compInstKey, String data)
    {
    	String mState = state;

    	if (direction.equals(Direction.OUT) &&
    			compKey.equals(COMPONENT) &&
    			listPIIPublisher.contains(data))
    				mState = PII_INFO;

    	return mState;
}

    private static void WriteToDebugViewer(Context context, ContentValues contentValues) {
        Intent svc = new Intent(context, DebugService.class);
        svc.putExtra(EXTRA_VALUES, contentValues);
        context.startService(svc);
    }

    public static class DebugService extends IntentService {

        private boolean mPrioritySet = false;

        public DebugService() {
            super(DebugService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            if (!mPrioritySet) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                mPrioritySet = true;
            }
            ContentValues contentValues = (ContentValues)intent.getParcelableExtra(EXTRA_VALUES);
            if (contentValues != null) {
                try {
                    Uri uri = getContentResolver().insert(DEBUG_DB_URI, contentValues);
                    if(uri == null)
                        Log.e(TAG, "Failure to insert into the DB");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception inserting into Debug Provider" );
                }
            } else {
                Log.e(TAG, "Null Content Values");
            }
        }
    }

    public Uri getTableUri() {
        return DEBUG_DB_URI;
    }
}
