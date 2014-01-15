/*
 * @(#)ActionTable.java
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

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.util.AndroidUtil;
import com.motorola.contextual.smartrules.util.Util;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;



/** This class allows access and updates to the Rule table. Basically, it abstracts a
 * the RuleTable tuple instance.
 *
 * The Rule table is used here to hold a rule.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TableBase which provides basic table inserts, deletes, etc.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, update, fetch  Rule Table records
 *  Converts cursor of RuleTable records to a RuleTuple.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ActionTable extends ModalTable implements RuleTable.SuggState, Constants {

    private static final String TAG = ActionTable.class.getSimpleName();

    //TODO: Need to think about dependencies between actions, for example:
    // 			action1 = turn on WiFi
    // 			action2 = send file
    //  should action 2 be dependent on action 1? If so, perhaps the user needs to be able to express
    //          the dependency.

    /** This is the name of the table in the database */
    public static final String TABLE_NAME 			= "Action";

    /** Currently not used, but could be for joining this table with other tables. */
    public static final String SQL_REF 	 			= " a";

    /** Name of the column in any child table, that reference this table's _id column. */
    public static final String COL_FK_ROW_ID    	= (FK+TABLE_NAME+Columns._ID);


    /** settings for @see Columns.ENABLED */
    public interface Enabled {
        final int ENABLED 		= 1;
        final int DISABLED 		= 0;
    }

    /** settings for @see Columns.ACTIVE */
    public interface Active {
        final int ACTIVE 		= 1;
        final int INACTIVE 		= 0;
    }

    /** settings for @see Columns.ON_MODE_EXIT */
    public interface OnModeExit {
        final int ON_ENTER      = 0;
        final int ON_EXIT       = 1;
    }
 
    /** settings for @see Columns.CONFLICT_WINNER_FLAG */
    public interface ConflictWinner {
    	final int LOSER 		= 0;
    	final int WINNER		= 1;
    }

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME+"/");

    public static interface Columns extends TableBase.Columns {

        /** _id foreign key of Rule table record*/
        public static final String PARENT_FKEY					= RuleTable.COL_FK_ROW_ID;
        /** 0=inactive(visually disconnected), 1=active(visually connected), 3=suggested(suggested, but never connected) */
        public static final String ENABLED						= "EnabledAct";
        /** 0=inactive, 1=active(fired action and still active mode) */
        public static final String ACTIVE						= "ActiveAct";
        /** 0=loser of the conflict resolution, 1=winner of conflict resolution */
        public static final String CONFLICT_WINNER_FLAG			= "ConfWinner";
        /** 1=action is fired only upon exit of the mode, 0=means action is fired at beginning of mode */
        public static final String ON_MODE_EXIT					= "OnExitMode";
        /** 0=accepted, 1=unread, 2=read */
        public static final String SUGGESTED_STATE				= "SuggStateAct";
        /** Suggested XML &ltREASON&gt is the only tag at this time . */
        public static final String SUGGESTED_REASON				= "SuggReasonAct";
        
        /** Action description for user's view of what action will be performed when this
         *  rule is invoked. */
        public static final String ACTION_DESCRIPTION			= "ActionDesc";
        /** Action Publisher key for this action provider
         * 			*/
        public static final String ACTION_PUBLISHER_KEY			= "ActPubKey";
        /** flag indicating whether the action has states, stateless(0) or stateful (1) or unknown (-1).
         * for example, the action "set Wifi ON" is stateful - wifi is either at ON or OFF.
         * However, a send SMS action is not stateful (has no beginning or end or state). */
        public static final String MODAL						= "ActModal";
        /** State Machine, eg, 'wifi'
         *  <pre><code>
         * (only supply this if it's a stateful setting, for example, this wouldn't be supplied
         * stored if the action was to make a phone call to a certain number (because calling a
         * number isn't a stateful setting).
         * 		eg1: com.motorola.contextual.wifi
         * 		eg2: com.motorola.contextual.ringer
         *
         * This column is null if the action isn't stateful (like settings)
         */
        public static final String STATE_MACHINE_NAME			= "StateMach";
        /** Target state, this only applies to stateful actions (setting something to a state).
         * This column provides the name (in the local language of the user) of the state to
         * which this action when successful should setting the statemachine to. That is, once
         * this action is fired using the URI_TO_FIRE_ACTION, the state should become the value
         * in this state machine.
         *  <pre><code>
         * 		eg1: eg, 'on' or 'off'
         * 		eg2: eg, 'mute' or 'loud' or '5'
         *
         * This column is null if the action isn't stateful (like settings)
         * See the STATE_MACHINE_NAME field for more information.
         * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
         * architecture, ACTION CONFIG holds the state machine value.
         * */
        @Deprecated
        public static final String TARGET_STATE					= "State";
        /** URI to fire to invoke the desired action.
         * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
         * architecture, CONFIG holds the information which is necessary to launch the Action
         * Publisher with correct configuration.
         * */
        @Deprecated
        public static final String URI_TO_FIRE_ACTION			= "UriToFire";
        /** Intent to fire to change or customize the action, set to null if no configuration required.  */
        public static final String ACTIVITY_INTENT				= "ActionActIntent";
        /** Action Rule Syntax
         * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
         * architecture, CONFIG holds all the configuration information.
         *  */
        @Deprecated
        public static final String ACTION_SYNTAX				= "ActionSyntax";
        /** see java.util.Date.getTime() - last time the action was invoked via the URI */
        public static final String LAST_FIRED_ATTEMPT_TIME 		= "LastFiredDT";
        /** Failure Message */
        public static final String FAILURE_MESSAGE         		= "ActFailMsg";
        /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
        public static final String ICON 						= "Icon";
        /** Config details .CONFIG maintains the User configuration of Action
         *  for example, Wifi=on or Bluetooth=Off etc.*/
        public static final String CONFIG                       = "Config";
        /** Validity of the action publisher. If this publisher is available
         * this field is set up to Valid, else it is set to Invalid */
        public static final String ACTION_VALIDITY              = "ActionValidity";
        /** Market Url to download this action publisher */
        public static final String MARKET_URL                   = "MarketUrl";
        /** Rule Key of the child rule with which this action record is associated with.
         *  This key is valid only for action publishers that have a child rule else should
         *  be null. */
        public static final String CHILD_RULE_KEY				= "ChildRuleKey";

    }
    private static final String[] COLUMN_NAMES = {
    	Columns._ID, Columns.PARENT_FKEY, Columns.ENABLED, 
    	Columns.ACTIVE, Columns.CONFLICT_WINNER_FLAG, 
    	Columns.ON_MODE_EXIT, Columns.SUGGESTED_STATE, Columns.SUGGESTED_REASON,
    	Columns.ACTION_DESCRIPTION, Columns.ACTION_PUBLISHER_KEY, Columns.MODAL,
	Columns.STATE_MACHINE_NAME, Columns.TARGET_STATE, Columns.URI_TO_FIRE_ACTION,
    	Columns.ACTIVITY_INTENT, Columns.ACTION_SYNTAX, Columns.LAST_FIRED_ATTEMPT_TIME, 
	Columns.FAILURE_MESSAGE, Columns.ICON, Columns.CONFIG, Columns.ACTION_VALIDITY,
	Columns.MARKET_URL, Columns.CHILD_RULE_KEY
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


    /** alternate index on the Rule foreign key column */
    public static final String PUBLISHER_KEY_COLUMN_INDEX_NAME =
        TABLE_NAME+Columns.ACTION_PUBLISHER_KEY+INDEX;

    public static final String CREATE_PUBLISHER_KEY_COLUMN_INDEX =
        CREATE_INDEX
        .replace("ixName", 	PUBLISHER_KEY_COLUMN_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.ACTION_PUBLISHER_KEY);


    /** SQL statement to create the Table */
    public static final String CREATE_TABLE_SQL =
        CREATE_TABLE +
        TABLE_NAME + " (" +
        Columns._ID						+ PKEY_TYPE									+ CONT +
        Columns.PARENT_FKEY				+ KEY_TYPE+NOT_NULL							+ CONT +
        Columns.ENABLED					+ INTEGER_TYPE								+ CONT +
        Columns.ACTIVE					+ INTEGER_TYPE								+ CONT +
        Columns.CONFLICT_WINNER_FLAG	+ INTEGER_TYPE+DEFAULT+ConflictWinner.LOSER + CONT +
        Columns.ON_MODE_EXIT			+ INTEGER_TYPE+DEFAULT+OnModeExit.ON_ENTER	+ CONT +
        Columns.SUGGESTED_STATE			+ INTEGER_TYPE+DEFAULT+RuleTable.SuggState.ACCEPTED + CONT +
        Columns.SUGGESTED_REASON		+ TEXT_TYPE									+ CONT +
        Columns.ACTION_DESCRIPTION		+ TEXT_TYPE									+ CONT +
        Columns.ACTION_PUBLISHER_KEY 	+ TEXT_TYPE									+ CONT +
        Columns.MODAL					+ INTEGER_TYPE+DEFAULT+Modality.UNKNOWN		+ CONT +
        Columns.STATE_MACHINE_NAME		+ TEXT_TYPE									+ CONT +
        Columns.TARGET_STATE			+ TEXT_TYPE									+ CONT +
        Columns.URI_TO_FIRE_ACTION		+ TEXT_TYPE									+ CONT +
        Columns.ACTIVITY_INTENT			+ TEXT_TYPE									+ CONT +
        Columns.ACTION_SYNTAX			+ TEXT_TYPE									+ CONT +
        Columns.LAST_FIRED_ATTEMPT_TIME	+ DATE_TIME_TYPE 							+ CONT +
        Columns.FAILURE_MESSAGE			+ TEXT_TYPE									+ CONT +
        Columns.ICON					+ TEXT_TYPE									+ CONT +
        Columns.CONFIG					+ TEXT_TYPE									+ CONT +
        Columns.ACTION_VALIDITY			+ TEXT_TYPE									+ CONT +
        Columns.MARKET_URL				+ TEXT_TYPE									+ CONT +
        Columns.CHILD_RULE_KEY			+ TEXT_TYPE									+ CONT +

        FOREIGN_KEY +" ("+Columns.PARENT_FKEY+
        ") "+ REFERENCES +RuleTable.TABLE_NAME+" ("+RuleTable.Columns._ID+")" +
        ")";


    /** Basic constructor
     */
    public ActionTable() {
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



//	/** updates the record,
//	 *
//	 * @param ctx - context
//	 * @param tuple - tuple to update
//	 * @return - key of the updated record if > 0, or number of rows updated if < 0.
//	 */
//	public long update(Context ctx, ActionTuple tuple) {
//
//		SQLiteManager db = SQLiteManager.openForWrite(ctx, TAG+".1");
//		long updated = 0;
//		try {
//			updated = new ActionTable().update(db, tuple);
//
//		} catch (Exception e) {
//			Log.e(TAG, e.toString());
//			e.printStackTrace();
//
//		} finally {
//			if (db != null)
//				SQLiteManager.closeDbOnly(db, TAG+".1");
//			if (LOG_INFO) Log.i(TAG, "update - after db close="+updated);
//		}
//		return updated;
//	}


    /** converts to ContentValues
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toContentValues(com.motorola.contextual.smartrules.db.table.TupleBase)
     * @param _tuple - instance to convert to content values.
     * @return content values for this instance
     */
    @Override
    public <T extends TupleBase> ContentValues toContentValues(T _tuple) {

        ContentValues args = new ContentValues();
	    if (_tuple instanceof ActionTuple) {
	        ActionTuple tuple = (ActionTuple) _tuple;
	
	        if (tuple.get_id() > 0) {
	            args.put(Columns._ID, 							tuple.get_id());
	        }
	        args.put(Columns.PARENT_FKEY, 						tuple.getParentFk());
	        args.put(Columns.ENABLED,							tuple.getEnabled());
	        args.put(Columns.ACTIVE, 							tuple.getActive());
	        args.put(Columns.CONFLICT_WINNER_FLAG, 				tuple.getConfWinner());
	        args.put(Columns.ON_MODE_EXIT,						tuple.getOnExitModeFlag());
	        args.put(Columns.SUGGESTED_STATE, 					tuple.getSuggState());
	        args.put(Columns.SUGGESTED_REASON, 					tuple.getSuggReason());		
	        args.put(Columns.ACTION_DESCRIPTION, 				tuple.getDescription());
	        args.put(Columns.ACTION_PUBLISHER_KEY, 				tuple.getPublisherKey());
	        args.put(Columns.MODAL, 							tuple.getModality());
	        args.put(Columns.STATE_MACHINE_NAME, 				tuple.getStateMachineName());
	        args.put(Columns.TARGET_STATE, 						tuple.getTargetState());
	        args.put(Columns.URI_TO_FIRE_ACTION, 				tuple.getUri());
	        args.put(Columns.ACTIVITY_INTENT, 					tuple.getActivityIntent());
	        args.put(Columns.ACTION_SYNTAX, 					tuple.getActionSyntax());
	        args.put(Columns.LAST_FIRED_ATTEMPT_TIME,			tuple.getLastFiredDateTime());
	        args.put(Columns.FAILURE_MESSAGE, 					tuple.getActFailMsg());
	        args.put(Columns.ICON,								tuple.getIcon());
	        args.put(Columns.CONFIG,							tuple.getConfig());
	        args.put(Columns.ACTION_VALIDITY,					tuple.getValidity());
	        args.put(Columns.MARKET_URL,						tuple.getMarketUrl());
	        args.put(Columns.CHILD_RULE_KEY, 					tuple.getChildRuleKey());
	    }
	    	
        return args;
    }


    /** returns a tuple from a cursor position.
     *
     * @param cursor - cursor - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    @SuppressWarnings("unchecked")
    public ActionTuple toTuple(final Cursor cursor, int[] tupleColumnNumbersIndex) {

        return toTuple(cursor, tupleColumnNumbersIndex, 0);
    }


    /** returns a tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursor - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     * @param ix - current index position in tupleColumnNumbersIndex to start.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    private ActionTuple toTuple(Cursor cursor, int[] tupleColNos, int ix) {


        ActionTuple tuple = null;
        tuple = new ActionTuple(
            // NOTE: The order of these must match the physical order in the CREATE_TABLE syntax above, as well as the constructor
            cursor.getLong(tupleColNos[ix++]), 			// _id
            cursor.getLong(tupleColNos[ix++]), 			// parentFkey
            cursor.getInt(tupleColNos[ix++]), 			// enabled
            cursor.getInt(tupleColNos[ix++]), 			// active
            cursor.getInt(tupleColNos[ix++]), 			// confWinner Flag
            cursor.getInt(tupleColNos[ix++]), 			// on Mode Exit flag
            cursor.getInt(tupleColNos[ix++]),			// suggState
            cursor.getString(tupleColNos[ix++]),		// suggReason
            cursor.getString(tupleColNos[ix++]),		// description
            cursor.getString(tupleColNos[ix++]),		// action Provider Key
            cursor.getInt(tupleColNos[ix++]),			// action modality
            cursor.getString(tupleColNos[ix++]),		// state machine name
            cursor.getString(tupleColNos[ix++]),		// target state
            cursor.getString(tupleColNos[ix++]),		// uri to fire action
            cursor.getString(tupleColNos[ix++]),		// ACTIVITY_INTENT
            cursor.getString(tupleColNos[ix++]),		// ACTION_SYNTAX
            cursor.getLong(tupleColNos[ix++]),			// lastFiredDateTime
            cursor.getString(tupleColNos[ix++]),		// action failure message
            cursor.getString(tupleColNos[ix++]),		// icons
            cursor.getString(tupleColNos[ix++]),		// Config details
            cursor.getString(tupleColNos[ix++]),		// Validity
            cursor.getString(tupleColNos[ix++]), 		// Market Url
            cursor.getString(tupleColNos[ix++])			// child rule key
        );
        if(ix != tupleColNos.length) {
            throw new UnsupportedOperationException("tupleColNos length = "+
                                                    tupleColNos.length+" and ix = "+ix+" do not match");
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

        String _idColName = getRowIdColName(cursor);


        String[] colNames = {	_idColName,
                                sqlRef+Columns.PARENT_FKEY,
                                sqlRef+Columns.ENABLED,
                                sqlRef+Columns.ACTIVE,
                                sqlRef+Columns.CONFLICT_WINNER_FLAG,
                                sqlRef+Columns.ON_MODE_EXIT,
                                sqlRef+Columns.SUGGESTED_STATE,
                                sqlRef+Columns.SUGGESTED_REASON,
                                sqlRef+Columns.ACTION_DESCRIPTION,
                                sqlRef+Columns.ACTION_PUBLISHER_KEY,
                                sqlRef+Columns.MODAL,
                                sqlRef+Columns.STATE_MACHINE_NAME,
                                sqlRef+Columns.TARGET_STATE,
                                sqlRef+Columns.URI_TO_FIRE_ACTION,
                                sqlRef+Columns.ACTIVITY_INTENT,
                                sqlRef+Columns.ACTION_SYNTAX,
                                sqlRef+Columns.LAST_FIRED_ATTEMPT_TIME,
                                sqlRef+Columns.FAILURE_MESSAGE,
                                sqlRef+Columns.ICON,
                                sqlRef+Columns.CONFIG,
                                sqlRef+Columns.ACTION_VALIDITY,
                                sqlRef+Columns.MARKET_URL,
                                sqlRef+Columns.CHILD_RULE_KEY
                            };
        return TableBase.getColumnNumbers(cursor, colNames);
    }


    /** required by ModalTable */
    public Uri getTableUri() {
        return CONTENT_URI;
    }


    /** package manager constants for Actions */
    /** TODO Craig's comment: These constants are not specific to the ActionTable,
     * 	they are specific to the an action publisher (android manifest for defining an AP).
     * 	Create a class called ActionPublisher, which extends a  base Publisher class, put
     *  this interface inside that ActionPublisher class. Call the interface  ManifestTag,
     *  therefore the users of these constants will reference them as:
     *  ActionPublisher.ManifestTags.CATEGORY */
    public interface PkgMgrConstants {
        String CATEGORY 	= "com.motorola.smartactions.intent.category.ACTION_PUBLISHER";
        String PUB_KEY 		= "com.motorola.smartactions.publisher_key";
        String TYPE_KEY 	= "com.motorola.smartactions.action_type";
    }


    /** populates the Modality Column in this table from the package manager
     *
     * @param context - context for data operations.
     */
    public void populateModalityColumn(final Context context) {

        final String selection = Columns.MODAL+EQUALS+Modality.UNKNOWN;
        final AndroidUtil util = new AndroidUtil();

        ProcessCursorSet set = new ProcessCursorSet();
        set.processSetInThread(context, selection, new CursorRowHandler() {

            private SQLiteManager db = null;

            public void onCursorRow(Cursor cursor) {
                ActionTuple t = toTuple(cursor);
                ResolveInfo info = util.findPkgMgrEntry(context,
                                                        PkgMgrConstants.CATEGORY,
                                                        PkgMgrConstants.PUB_KEY,
                                                        t.getPublisherKey());
                if (info == null) {
                    Log.e(TAG, "Missing metadata for type in AndroidManifest: "
				+t.getPublisherKey()+" this will cause system failures.");
                } else {
                    android.os.Bundle metaData = info.activityInfo.metaData;
                    if (metaData == null) metaData = info.serviceInfo.metaData;
                    if (metaData != null) {
                        CharSequence labelSeq = info.loadLabel(AndroidUtil.getPkgMgr());
                        String modality = metaData.getString(PkgMgrConstants.TYPE_KEY);
                        t.setModality(convertPkgMgrModalityType(modality));
                        // set state machine name if needed
                        if (t.getStateMachineName() == null
					|| t.getStateMachineName().trim().length() < 1)
                            t.setStateMachineName(labelSeq.toString());
                        if (ActionTable.this.update(context, t) < 1)
                            Log.e(TAG, ".populateModalityColumn - Failed " +
						"to update:"+t.toString());
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
                if (LOG_DEBUG) Log.d(TAG, "Closing db==null?"+(db==null)
								+" isOpen="+(db!=null?db.isOpen():"?") );
            }

            public void onAfterLastRow() {
                // nothing to do here

            }
        });
    }

    /** deletes all actions for 1 rule
     *
     * @param context
     * @param join
     */
    public static void deleteAllActions(Context context, long rule_id) {
        // delete all actions
        new ActionTable().massDelete(context,
						ActionTable.Columns.PARENT_FKEY+EQUALS+rule_id);
    }
}