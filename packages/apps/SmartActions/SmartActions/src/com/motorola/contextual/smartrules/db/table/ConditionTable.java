/*
 * @(#)ConditionTable.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/10/28 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;


import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.util.AndroidUtil;
import com.motorola.contextual.smartrules.util.Util;


/** This class allows access and updates to the Condition table. Basically, it abstracts a
 * the ConditionTable tuple instance.
 *
 * The Condition table is used here to hold a rule.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TableBase which provides basic table inserts, deletes, etc.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, update, fetch  Condition Table records
 *  Converts cursor of ConditionTable records to a ConditionTuple.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ConditionTable extends ModalTable implements RuleTable.SuggState, Constants {

    private static final String TAG = ConditionTable.class.getSimpleName();

    /** This is the name of the table in the database */
    public static final String TABLE_NAME 			= "Condition";

    /** Currently not used, but could be for joining this table with other tables. */
    public static final String SQL_REF 	 			= " c";

    /** Name of the column in any child table, that reference this table's _id column. */
    public static final String COL_FK_ROW_ID    	= (FK+TABLE_NAME+Columns._ID);


    /** Possible values for ENABLED column
     */
    public interface Enabled {
        final int ENABLED 		= 1;
        final int DISABLED 		= 0;
    }

    /** Possible values for CONDITION_MET column
     */
    public interface CondMet {
    	final int COND_MET = 1;
    	final int COND_NOT_MET = 0;
    }
    
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME+"/");

    public static interface Columns extends TableBase.Columns {

        /** _id foreign key of Rule table record*/
        public static final String PARENT_FKEY					= RuleTable.COL_FK_ROW_ID;
        /** 0=inactive(visually disconnected), 1=active(visually connected) */
        public static final String ENABLED						= "EnabledCond";
        /** 0=accepted, 1=unread, 2=read */
        public static final String SUGGESTED_STATE				= "SuggStateCond";
        /** Suggested XML &ltREASON&gt is the only tag at this time . */
        public static final String SUGGESTED_REASON				= "SuggReasonCond";
        /** Whether or not if VSM considers this condition to be true or false 0=not met, 1=met*/
        public static final String CONDITION_MET				= "ConditionMet";
        /** State Publisher key for this State publisher. This is should be a namespace like com.motorola.locationsensor  */
        public static final String CONDITION_PUBLISHER_KEY				= "StatePubKey";
        /** flag indicating whether the precondition has states, stateless(0) or stateful (1).
         * for example, the precondition "at home" is stateful - you're either at home or not at home.
         * However, a missed call precondition is not stateful (has no beginning or end or state). */
        public static final String MODAL						= "Modal";
        /** Sensor or state machine name. This should be a common name like "Location Sensor",
         * used for debugging */
        public static final String SENSOR_NAME					= "SensorName";
        /** Intent to fire to change or customize the state (Activity to launch), set to null if no customization.  */
        public static final String ACTIVITY_INTENT				= "StateActIntent";
        /** State for which this condition will activate.    
         *  NOTE: This column is deprecated as of version 35 of the DB. In the new Condition Publisher
         *  architecture, CONDITION_CONFIG holds all the configuration information 
         *   */
        @Deprecated
        public static final String TARGET_STATE					= "TargetState";
        /** Condition description  for user's view of what the description of the condition is that needs to be met
         *   in order to fire this rule. */
        public static final String CONDITION_DESCRIPTION		= "ConditionDesc";
        /** State Recognition Engine Rule Syntax       
         *  NOTE: This column is deprecated as of version 35 of the DB. In the new Condition Publisher
         * architecture, CONDITION_CONFIG holds all the configuration information   */
        @Deprecated
        public static final String STATE_SYNTAX					= "StateSyntax";
        /** see java.util.Date.getTime() - created date / time of the condition */
        public static final String CREATED_DATE_TIME 			= "StateCreatedDT";
        /** see java.util.Date.getTime() - last fail date / time of the condition */
        public static final String LAST_FAIL_TIME 				= "CondFailDT";
        /** Failure Message */
        public static final String FAILURE_MESSAGE         		= "CondFailMsg";        
        /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
        public static final String ICON 						= "Icon";
        /** Config details .CONFIG maintains the User configuration of Action
         *  for example, Wifi=on or Bluetooth=Off etc.*/
        public static final String CONDITION_CONFIG                       = "CondConfig";
        /** Validity of the condition publisher. If this publisher is available
         * this field is set up to Valid, else it is set to Invalid */
        public static final String CONDITION_VALIDITY              = "CondValidity";
        /** Market Url to download this action publisher */
        public static final String CONDITION_MARKET_URL                       = "CondMarketUrl";
    }
    private static final String[] COLUMN_NAMES = {Columns._ID, Columns.PARENT_FKEY, 
    	Columns.ENABLED, Columns.SUGGESTED_STATE,
    	Columns.SUGGESTED_REASON, Columns.CONDITION_MET,
	Columns.CONDITION_PUBLISHER_KEY, Columns.MODAL, Columns.SENSOR_NAME,
    	Columns.ACTIVITY_INTENT, Columns.TARGET_STATE, Columns.CONDITION_DESCRIPTION,
    	Columns.STATE_SYNTAX, Columns.CREATED_DATE_TIME, 
	Columns.LAST_FAIL_TIME, Columns.FAILURE_MESSAGE, Columns.ICON,
        Columns.CONDITION_CONFIG, Columns.CONDITION_VALIDITY, Columns.CONDITION_MARKET_URL
       };
    
    public static String[] getColumnNames() {
    	return Util.copyOf(COLUMN_NAMES);
    }
    
    
    /** alternate index on the Rule foreign key column */
    public static final String PARENT_FK_INDEX_NAME =
        TABLE_NAME+Columns.PARENT_FKEY+INDEX;

    public static final String CREATE_PARENT_FKEY_INDEX =
        CREATE_INDEX
        .replace("ixName", 	PARENT_FK_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.PARENT_FKEY);


    /** alternate index on the Sensor Name column */
    public static final String SENSOR_NAME_COLUMN_INDEX_NAME =
        TABLE_NAME+Columns.SENSOR_NAME+INDEX;

    public static final String CREATE_SENSOR_NAME_COLUMN_INDEX =
        CREATE_INDEX
        .replace("ixName", 	SENSOR_NAME_COLUMN_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.SENSOR_NAME);

    /** alternate index on the Publisher key column */
    public static final String PUBLISHER_KEY_COLUMN_INDEX_NAME =
        TABLE_NAME+Columns.CONDITION_PUBLISHER_KEY+INDEX;

    public static final String CREATE_PUBLISHER_KEY_COLUMN_INDEX =
        CREATE_INDEX
        .replace("ixName", 	PUBLISHER_KEY_COLUMN_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.CONDITION_PUBLISHER_KEY);


    /** SQL statement to create the Table */
    public static final String CREATE_TABLE_SQL =
        CREATE_TABLE +
        TABLE_NAME + " (" +
        Columns._ID						+ PKEY_TYPE								+ CONT +
        Columns.PARENT_FKEY				+ KEY_TYPE+ NOT_NULL					+ CONT +
        Columns.ENABLED					+ INTEGER_TYPE							+ CONT +
        Columns.SUGGESTED_STATE			+ INTEGER_TYPE+DEFAULT+RuleTable.SuggState.ACCEPTED + CONT +
        Columns.SUGGESTED_REASON		+ TEXT_TYPE								+ CONT +
        Columns.CONDITION_MET			+ INTEGER_TYPE+DEFAULT+CondMet.COND_NOT_MET + CONT +
        Columns.CONDITION_PUBLISHER_KEY			+ TEXT_TYPE								+ CONT +
        Columns.MODAL					+ INTEGER_TYPE+DEFAULT+Modality.UNKNOWN	+ CONT +
        Columns.SENSOR_NAME				+ TEXT_TYPE								+ CONT +
        Columns.ACTIVITY_INTENT			+ TEXT_TYPE								+ CONT +
        Columns.TARGET_STATE			+ TEXT_TYPE								+ CONT +
        Columns.CONDITION_DESCRIPTION	+ TEXT_TYPE								+ CONT +
        Columns.STATE_SYNTAX			+ TEXT_TYPE								+ CONT +
        Columns.CREATED_DATE_TIME		+ DATE_TIME_TYPE 						+ CONT +
        Columns.LAST_FAIL_TIME			+ DATE_TIME_TYPE						+ CONT +
        Columns.FAILURE_MESSAGE			+ TEXT_TYPE								+ CONT +
        Columns.ICON					+ TEXT_TYPE								+ CONT +
        Columns.CONDITION_CONFIG		+ TEXT_TYPE								+ CONT +
        Columns.CONDITION_VALIDITY	    + TEXT_TYPE								+ CONT +
        Columns.CONDITION_MARKET_URL    + TEXT_TYPE								+ CONT +

        FOREIGN_KEY +" ("+Columns.PARENT_FKEY+
        ") "+ REFERENCES +RuleTable.TABLE_NAME+" ("+RuleTable.Columns._ID+")" +
        ")";


    /** Basic constructor
     */
    public ConditionTable() {
        super();
    }


    /** Get the table name for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    /** gets the foreign key column for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getFkColName() {
        return COL_FK_ROW_ID;
    }


    /** converts to ContentValues
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toContentValues(com.motorola.contextual.smartrules.db.table.TupleBase)
     * @param _tuple - instance to convert to content values.
     * @return content values for this instance
     */
    @Override
    public <T extends TupleBase> ContentValues toContentValues(T _tuple) {

        ContentValues args = new ContentValues();
        if (_tuple instanceof ConditionTuple) {
	        ConditionTuple tuple = (ConditionTuple) _tuple;
	
	        if (tuple.get_id() > 0) {
	            args.put(Columns._ID, 						tuple.get_id());
	        }
	
	        args.put(Columns.PARENT_FKEY, 					tuple.getParentFkey());
	        args.put(Columns.ENABLED, 						tuple.getEnabled());
	        args.put(Columns.SUGGESTED_STATE, 				tuple.getSuggState());
	        args.put(Columns.SUGGESTED_REASON,				tuple.getSuggReason());	
	        args.put(Columns.CONDITION_MET,                 tuple.getCondMet());
	        args.put(Columns.CONDITION_PUBLISHER_KEY, 				tuple.getPublisherKey());
	        args.put(Columns.MODAL,	 						tuple.getModality());
	        args.put(Columns.SENSOR_NAME, 					tuple.getSensorName());
	        args.put(Columns.ACTIVITY_INTENT, 				tuple.getActivityIntent());
	        args.put(Columns.TARGET_STATE, 					tuple.getTargetState());
	        args.put(Columns.CONDITION_DESCRIPTION, 		tuple.getDescription());
	        args.put(Columns.STATE_SYNTAX,					tuple.getStateSyntax());
	        args.put(Columns.ICON,    						tuple.getIcon());
	        args.put(Columns.CREATED_DATE_TIME,				tuple.getCreatedDateTime());
	        args.put(Columns.LAST_FAIL_TIME, 				tuple.getLastFailDateTime());
	        args.put(Columns.FAILURE_MESSAGE, 				tuple.getCondFailMsg());
	        args.put(Columns.CONDITION_CONFIG,				tuple.getConfig());
	        args.put(Columns.CONDITION_VALIDITY,			tuple.getValidity());
	        args.put(Columns.CONDITION_MARKET_URL,			tuple.getMarketUrl());
        }
        return args;
    }


    /** returns a tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursort - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    @SuppressWarnings("unchecked")
    public ConditionTuple toTuple(Cursor cursor, int[] tupleColumnNumbersIndex) {

        return toTuple(cursor, tupleColumnNumbersIndex, 0);
    }


    /** returns a tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursort - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     * @param ix - current index position in tupleColumnNumbersIndex to start.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    private ConditionTuple toTuple(Cursor cursor, int[] tupleColNos, int ix) {

        ConditionTuple tuple = new ConditionTuple(

            // NOTE: The order of these must match the physical order in the CREATE_TABLE syntax above, as well as the constructor
            cursor.getLong(tupleColNos[ix++]), 		// _id
            cursor.getLong(tupleColNos[ix++]), 		// parent foreign key
            cursor.getInt(tupleColNos[ix++]), 		// enabled
            cursor.getInt(tupleColNos[ix++]),		// suggState
            cursor.getString(tupleColNos[ix++]),	// suggReason.
            cursor.getInt(tupleColNos[ix++]),		// condMet
            cursor.getString(tupleColNos[ix++]),	// publisherKey
            cursor.getInt(tupleColNos[ix++]), 		// modality
            cursor.getString(tupleColNos[ix++]),	// sensorName
            cursor.getString(tupleColNos[ix++]),	// activityIntent
            cursor.getString(tupleColNos[ix++]),	// targetState
            cursor.getString(tupleColNos[ix++]),	// description
            cursor.getString(tupleColNos[ix++]),	// stateSyntax
            cursor.getLong(tupleColNos[ix++]),		// date created
            cursor.getLong(tupleColNos[ix++]),		// date failed
            cursor.getString(tupleColNos[ix++]),	// failure message
            cursor.getString(tupleColNos[ix++]),	// icons
            cursor.getString(tupleColNos[ix++]),	// Config details
            cursor.getString(tupleColNos[ix++]),	// Validity
            cursor.getString(tupleColNos[ix++]) 	// Market Url
        );

        if(ix != tupleColNos.length) {
            throw new UnsupportedOperationException("tupleColNos length = "+tupleColNos.length+" and ix = "+ix+" do not match");
        }

        return tuple;
    }


    /** gets column numbers so that data can be extracted from a cursor
     *
     * @param cursor - cursor to be parsed to s
     * @param sqlRef - reference prefix for the table name like "g." if needed.
     * @return int array of column numbers.
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getColumnNumbers(android.database.Cursor, java.lang.String, java.lang.String)
     */
    public int[] getColumnNumbers(Cursor cursor, String sqlRef) {

        return TableBase.getColumnNumbers(cursor, COLUMN_NAMES);
    }


    /** required by TableBase */
    public Uri getTableUri() {
        return CONTENT_URI;
    }


    /** package manager constants for Actions */
    public interface PkgMgrConstants {
        String CATEGORY 		= "com.motorola.smartactions.intent.category.CONDITION_PUBLISHER";
        String PUB_KEY 			= "com.motorola.smartactions.publisher_key";
        String TYPE_KEY 		= "com.motorola.smartactions.action_type";
    }


    /** NOTE, THE MODALITY METADATA CONSTANTS HAVE YET TO BE PLACED INTO THE SMART PROFILE MANIFEST.
     * THIS ROUTINE WILL NOT WORK UNTIL THAT IS DONE. Also, the TYPE_KEY constant in PkgMgrConstants
     * MUST ALSO BE UPDATED TO MATCH THE METADATA KEY FOR THE TYPE.
     *  populates the Modality Column in this table from the package manager
     *
     * @param context - context for data operations.
     */
    @SuppressWarnings("unused")
    private void populateModalityColumn(final Context context) {

        final String selection = Columns.MODAL+EQUALS+Modality.UNKNOWN;
        final AndroidUtil util = new AndroidUtil();

        ProcessCursorSet set = new ProcessCursorSet();
        set.processSetInThread(context, selection, new CursorRowHandler() {

            private SQLiteManager db = null;

            public void onCursorRow(Cursor cursor) {

                ConditionTuple t = toTuple(cursor);
                ResolveInfo info = util.findPkgMgrEntry(context,
                                                        PkgMgrConstants.CATEGORY, PkgMgrConstants.PUB_KEY, t.getPublisherKey());
                if (info == null) {
                    Log.e(TAG, "Missing metadata for type in AndroidManifest: "+t.getPublisherKey()+
                          " this will cause system failures.");
                } else {
                    android.os.Bundle metaData = info.activityInfo.metaData;
                    if (metaData == null) metaData = info.serviceInfo.metaData;
                    if (metaData != null) {
                        //CharSequence labelSeq = info.loadLabel(AndroidUtil.getPkgMgr());
                        String modality = metaData.getString(PkgMgrConstants.TYPE_KEY);
                        t.setModality(convertPkgMgrModalityType(modality));
                        if (ConditionTable.this.update(db, t) < 1)
                            Log.e(TAG, ".populateModalityColumn - Failed to update:"+t.toString());
                    }
                }
            }

            public void onBeforeFirstRow() {
                /** open the Db */
                db = SQLiteManager.openForWrite(context, TAG+".1");
                if (db == null || !db.isOpen())
                    throw new IllegalStateException(DB_OPEN_ERROR);
            }

            public void onFinally() {
                /** close the Db */
                if(db != null && db.isOpen())
                    db.close(TAG+".1");
            }

            public void onAfterLastRow() {
                // noting to do here

            }
        });

    }

    /** deletes all conditions for 1 rule.
    *
    * @param context - context
    * @param rule_id - _id of the rule table used as a foreign key in the condition table
    */
   public static void deleteAllConditions(Context context, long rule_id) {

       // delete all conditions
       new ConditionTable().massDelete(context,
		ConditionTable.Columns.PARENT_FKEY+EQUALS+rule_id);
   }
}
