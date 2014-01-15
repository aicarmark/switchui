/*
 * @(#)RuleTable.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 * 
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/10/28 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.service.SmartRulesService;
import com.motorola.contextual.smartrules.util.Util;



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
public class RuleTable extends TableBase implements Constants {

    private static final String TAG = RuleTable.class.getSimpleName();

    /** This is the name of the table in the database */
    public static final String TABLE_NAME 			= "Rule";

    /** Currently not used, but could be for joining this table with other tables. */
    public static final String SQL_REF 	 			= " r";

    /** Name of the foreign key column in other tables, that reference this table. */
    public static final String COL_FK_ROW_ID    	= (FK+TABLE_NAME+Columns._ID);


    /** Possible values for ENABLED column. ENABLED means condition publishers are subscribed.
     * disabled means they are not. Also, disabled state can be entered by the user manually
     * disabling the rule. */
    public interface Enabled {
        /** all suggested rules come in as disabled */
        final int DISABLED 		= 0;
        final int ENABLED 		= 1;
    }

    /** Possible values for RULE_TYPE column.
     * 		- Automatic - the default option, the system controls the rule activation, deactivation.
     *  	- Manual - the rule has no pre-conditions and user has to manually turn on or off the rule.
     *  	- Default - there should be 1 and only 1 default rule which is set to default. All of the actions associated
     * 			with the DEFAULT rule are the default settings
     */
    public interface RuleType {
        /** This is the default value. When set it means the rule has pre-conditions and the sensors
         * 	are created for each pre-condition. The rule is automatically invoked. */
        final int AUTOMATIC = 0;
        /** The rule is a manual rule (user must manually turn it on and off) */
        final int MANUAL    = 1;
        /** there should be 1 and only 1 default rule which is set to default. All of the actions associated
         * with the DEFAULT rule are the default settings. (value that the setting will return to
         * when the mode is exited (i.e. users leaves home and therefore exits their "home" rule) */
        final int DEFAULT   = -1;
    }


    /** Possible values for ACTIVE column. Active means that the mode is active. */
    public interface Active {
        final int ACTIVE 		=  1;   // implies enabled
        final int INACTIVE 		=  0;   // implies enabled
    }

    /** Possible values for FLAGS column */
    public interface Flags {

        /** invisible - never appears on any list. */
        final String INVISIBLE 				= "i";
        final String NON_EDITABLE 			= "n";
        /** visible on suggested or factory list only (not on main rule list).
         * For example, once a rule is inferred, then the corresponding suggested rule becomes
         * SOURCE_LIST_VISIBLE on the suggested list.  */
        final String SOURCE_LIST_VISIBLE 	= "s";
    }

    /** Possible values for SOURCE column
     * <pre><code>
    *  		0=USER,
    *  		1=COMMUNITY,
    *  		2=INFERRED,
    *  		3=SUGGESTED
    *  		4=FACTORY
    *  		5=DEFAULT
    *   	6=CHILD (a child rule is a complex action publisher and is basically a rule
    *   			 associated with an action block)
    */
    public interface Source {
        final int USER 			= 0;
        final int COMMUNITY 	= 1;
        final int INFERRED 		= 2;
        final int SUGGESTED 	= 3;
        final int FACTORY		= 4; // precanned
        final int DEFAULT		= 5;
        final int CHILD			= 6; // child rule
    }
    
    /** Possible values for SUGGESTED_STATE column */
    public interface SuggState {
	final int INVALID = -1;
    	final int ACCEPTED = 0;
    	final int UNREAD = 1;
    	final int READ   = 2;
    }

    /** Possible values for LIFECYCLE column 
     * <pre><code>
     * 	-1=IMMEDIATE (User behavior change type suggestions - rule that runs immediately and then deleted)
     * 	 0=NEVER_EXPIRES (New rule type suggestions - stays in the DB)
     * 	 1=ONE_TIME (One time actions type suggestions - is deleted when the user rejects or accepts)
     * 	 For future use > 1 will mean Date/Time the rule expires.
     * */
    public interface Lifecycle {
        final int IMMEDIATE       = -1; // Dehavior type
        final int NEVER_EXPIRES   = 0; // DEFAULT
        final int ONE_TIME        = 1; // One time action firing
        final int UPDATE_RULE     = 2; // Adding actions/conditions to the existing rule
        final int SWAP_ONE        = 3; // Swap a condition
    }
    
    /**^M
      * Possible values for Actions/Conditions state, for use in debug string
      * to be written to debug viewer db
      * <pre><code>
      * ENABLED_STATE = "Enabled"  status showing enabled
      * DISABLED_STATE = "Disabled"  status showing disabled
      */
    public interface StateForActionsAndConditions{
    	  final String ENABLED_STATE = "Enabled";
    	  final String DISABLED_STATE = "Disabled";
    }

    /** Possible values for SILENT column
     * <pre><code>
     *  0=Show (display this rule name in the notification bar when rule becomes active)
     *  1=NOT_SHOW (do not display this rule name in the notification bar when rule becomes active)
     */
    public interface Silent {
    	final int TELL_USER = 0;
    	final int SILENT = 1;
    }
    
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME+"/");

    public interface Columns extends TableBase.Columns {

        /** @see Enabled above. User turned off/on ALL of the user-created rules, either 0=disabled,
         * 1=enabled - @see Enabled.ENABLED, Enabled.DISABLED */
        public static final String ENABLED 					= "Ena";
        /** user ordered ranking value (float) */
        public static final String RANK 					= "Rank";
        /** unique rule key - column name - this is a string name using a namespace (i.e. com.motorola...)
         * to ensure uniqueness */
        public static final String KEY 						= "Key";
        /** state tracking whether this rule is believed by this component to be currently:
         * suggested (=2), active (=1) or inactive(=0) */
        public static final String ACTIVE 					= "Act";
        /** @see RuleType above. This column controls the type of the rule. 0=Automatic, rule has
         * 	pre-conditions. 1=Manual, rule has no pre-conditions. -1=Default, only one rule with this
         * 	value should be in the DB. */
        public static final String RULE_TYPE				= "Manual";
        /** @see Source above. source of the rule USER=0, COMMUNITY =1, INFERRED =2, SUGGESTED=3 */
        public static final String SOURCE 					= "Source";
        /** Rating as provided by the community for the usefulness of this rule */
        public static final String COMMUNITY_RATING 		= "Rating";
        /** Author - could include &ltname&gt, &ltemail&gt tags, etc. */
        public static final String COMMUNITY_AUTHOR 		= "Author";
        /** Flags can be in any order where: n=not editable, i=invisible (Defaults are "visible editable) */
        public static final String FLAGS 					= "Flags";
        /** name of the rule, typically as entered by the user. */
        public static final String NAME 					= "Name";
        /** description of the rule, typically as entered by the user. */
        public static final String DESC 					= "Desc";
        /** DVS key or other information of interest, should be name-value pair format like DVSKEY=01221, etc. */
        public static final String TAGS 					= "Tags";
        /** XML format and/or conditions between the Conditions, each having the ConditionTable key between to relate each */
        public static final String CONDITIONS				= "Conditions";
        /** Inference logic */
        public static final String INFERENCE_LOGIC			= "InferLogic";
        /** Inference status */
        public static final String INFERENCE_STATUS			= "InferStatus";
        /** Suggested state (Unread or Read) */
        public static final String SUGGESTED_STATE			= "SuggState";
        /** Suggested XML &ltREASON&gt is the only tag at this time . */
        public static final String SUGGESTED_REASON			= "Suggested";
        /** -1=Immediate, 0=Never Expires, 1=One Time */
        public static final String LIFECYCLE				= "Lifecycle";
        /** 0=Show, 1=Do Not Show */
        public static final String SILENT					= "Silent";
        /** Virtual Sensor
         * NOTE: This column is deprecated as of version 35 of the DB as Virtual Sensors
         * are no longer used */
        @Deprecated
        public static final String VSENSOR					= "RuleVSensor";
        /** Rule Syntax */
        public static final String RULE_SYNTAX				= "RuleSyntax";
        /** last date/time rule was "active" see java.util.Date.getTime() */
        public static final String LAST_ACTIVE_DATE_TIME 	= "LastActDT";
        /** last date/time rule was "inactive" see java.util.Date.getTime() */
        public static final String LAST_INACTIVE_DATE_TIME 	= "LastInactDT";
        /** see java.util.Date.getTime() */
        public static final String CREATED_DATE_TIME 		= "CreatedDT";
        /** last date/time rule was "Edited" see java.util.Date.getTime() */
        public static final String LAST_EDITED_DATE_TIME 	= "LastEditedDT";
        /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
        public static final String ICON 					= "RuleIcon";
        /** Rule ID of the Sample Rule (Parent) from which this rule (Child) is derived or to store
         * the incremental count of the sample rule adopted. 
         * NOTE: This column is deprecated as of version 34 of the DB and is replaced
         * by the new column ADOPT_COUNT */
        @Deprecated
        public static final String SAMPLE_FKEY_OR_COUNT		= "SampleFkOrCount";
        /** Unique Publisher key. However unique constraint columns cannot be added via ALTER TABLE.
         * Hence constraint has not be added. */
        public static final String PUBLISHER_KEY			= "PubKey";
        /** Validity of the rule. If all the actions of this rule are available,
         * then this field is set to Valid */
        public static final String VALIDITY                 = "Validity";
        /** Intent to be broadcasted to invoke the UI for this rule. Used by
         *  Suggestions module */
        public static final String UI_INTENT                = "UiIntent";
        /** Rule Key of the Parent from which this child rule is either derived from for
         * 	adopted Sample/Suggestion or for Battery Rules it is the parent to which it is
         *  linked. */
        public static final String PARENT_RULE_KEY          = "ParentRuleKey";
        /** Incremental counter to indicate the number of times the sample or suggested
         *  rule is adopted. The count value is the value of number of times the rule is
         *  adopted and not the number to be used for next adoption. To clarify, this
         *  value must be incremented before used for a new adoption. This column will 
         *  replace the SampleFkOrCount column. */
        public static final String ADOPT_COUNT				= "AdoptCount";

    }
    private static final String[] COLUMN_NAMES = {
    	Columns._ID, Columns.ENABLED, Columns.RANK, Columns.KEY, 
    	Columns.ACTIVE, Columns.RULE_TYPE, Columns.SOURCE,
    	Columns.COMMUNITY_RATING, Columns.COMMUNITY_AUTHOR,
    	Columns.FLAGS, Columns.NAME, Columns.DESC, Columns.TAGS, Columns.CONDITIONS,
    	Columns.INFERENCE_LOGIC, Columns.INFERENCE_STATUS, Columns.SUGGESTED_STATE, 
    	Columns.SUGGESTED_REASON, Columns.LIFECYCLE, Columns.SILENT, Columns.VSENSOR, 
    	Columns.RULE_SYNTAX, Columns.LAST_ACTIVE_DATE_TIME, Columns.LAST_INACTIVE_DATE_TIME,
     	Columns.CREATED_DATE_TIME, Columns.LAST_EDITED_DATE_TIME, Columns.ICON, 
	Columns.SAMPLE_FKEY_OR_COUNT, Columns.PUBLISHER_KEY,
	    Columns.VALIDITY, Columns.UI_INTENT, Columns.PARENT_RULE_KEY, Columns.ADOPT_COUNT

       };
    
    public static String[] getColumnNames() {
    	return Util.copyOf(COLUMN_NAMES);
    }
    

    /** alternate index on the Rule foreign key column */
    public static final String RULE_KEY_INDEX_NAME =
        TABLE_NAME+Columns.KEY+INDEX;

    public static final String CREATE_RULE_KEY_INDEX =
        CREATE_INDEX
        .replace("ixName", 	RULE_KEY_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.KEY);



    /** SQL statement to create the Table */
    public static final String CREATE_TABLE_SQL =
        CREATE_TABLE +
        TABLE_NAME + " (" +
        Columns._ID						+ PKEY_TYPE								+ CONT +
        Columns.RANK					+ REAL_TYPE 							+ CONT +
        Columns.ENABLED					+ INTEGER_TYPE 							+ CONT +
        Columns.KEY						+ TEXT_TYPE + UNIQUE					+ CONT +
        Columns.ACTIVE					+ INTEGER_TYPE 							+ CONT +
        Columns.RULE_TYPE				+ INTEGER_TYPE+DEFAULT+RuleType.AUTOMATIC+ CONT +
        Columns.SOURCE					+ INTEGER_TYPE 							+ CONT +
        Columns.COMMUNITY_RATING		+ INTEGER_TYPE 							+ CONT +
        Columns.COMMUNITY_AUTHOR		+ TEXT_TYPE								+ CONT +
        Columns.FLAGS					+ TEXT_TYPE								+ CONT +
        Columns.NAME					+ TEXT_TYPE								+ CONT +
        Columns.DESC					+ TEXT_TYPE								+ CONT +
        Columns.TAGS					+ TEXT_TYPE								+ CONT +
        Columns.CONDITIONS				+ TEXT_TYPE								+ CONT +
        Columns.INFERENCE_LOGIC 		+ TEXT_TYPE								+ CONT +
        Columns.INFERENCE_STATUS		+ TEXT_TYPE								+ CONT +
        Columns.SUGGESTED_STATE			+ INTEGER_TYPE+DEFAULT+SuggState.ACCEPTED + CONT +
        Columns.SUGGESTED_REASON		+ TEXT_TYPE								+ CONT +
        Columns.LIFECYCLE				+ INTEGER_TYPE+DEFAULT+Lifecycle.NEVER_EXPIRES + CONT +
        Columns.SILENT					+ INTEGER_TYPE+DEFAULT+Silent.TELL_USER	+ CONT +
        Columns.VSENSOR					+ TEXT_TYPE								+ CONT +
        Columns.RULE_SYNTAX				+ TEXT_TYPE								+ CONT +
        Columns.LAST_ACTIVE_DATE_TIME	+ DATE_TIME_TYPE 						+ CONT +
        Columns.LAST_INACTIVE_DATE_TIME	+ DATE_TIME_TYPE 						+ CONT +
        Columns.CREATED_DATE_TIME		+ DATE_TIME_TYPE 						+ CONT +
        Columns.LAST_EDITED_DATE_TIME	+ DATE_TIME_TYPE						+ CONT +
        Columns.ICON					+ TEXT_TYPE 							+ CONT +
        Columns.SAMPLE_FKEY_OR_COUNT	+ KEY_TYPE + NOT_NULL					+ CONT +
        Columns.PUBLISHER_KEY			+ TEXT_TYPE 							+ CONT +
        Columns.VALIDITY				+ TEXT_TYPE 							+ CONT +
        Columns.UI_INTENT				+ TEXT_TYPE 							+ CONT +
        Columns.PARENT_RULE_KEY			+ TEXT_TYPE								+ CONT +
        Columns.ADOPT_COUNT				+ INTEGER_TYPE+DEFAULT+ZERO				+
        ")";


    /** Basic constructor
     */
    public RuleTable() {
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


    /** required by TableBase */
    public Uri getTableUri() {
        return CONTENT_URI;
    }


    /** gets the foreign key column for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getFkColName() {
        return COL_FK_ROW_ID;
    }

    /** deletes the rule from the rule table and the actions, conditions, condition sensors
     * 	and child rules associated with this rule from the corresponding tables.
     * 
     * @param context - Context
     * @param ruleWhereClause - where clause.
     * @return - the number of rows deleted form the rule table.
     */
    @Override
    public int massDelete(Context context, final String ruleWhereClause) {    	
    	int result = 0;
    	if(LOG_DEBUG) Log.d(TAG, "ruleWhereClause = "+ruleWhereClause);

        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".1");
        // remove all or none of the rule using transaction boundary, 
        // let this crash if db is null
        db.beginTransaction();
        try {
        	// Fetch the _id and key for the rule to be deleted from the DB using the
        	// whereClause
        	RuleTuple tuple = new RuleTable().fetch1(context, ruleWhereClause);
        	
        	// Delete all the conditions associated with the rule
        	ConditionTable.deleteAllConditions(context, tuple._id);
        	
        	// Delete all the actions associated with the rule
        	ActionTable.deleteAllActions(context, tuple._id);
        	
            // Delete all child rules
            String whereClause = Columns.PARENT_RULE_KEY + EQUALS + Q + tuple.getKey() + Q
            			+ AND + Columns.SOURCE + EQUALS + Q + Source.CHILD + Q;
            ArrayList<RuleTuple> childRuleTuples = 
            		new RuleTable().fetchList(context, whereClause);
            
            if(childRuleTuples != null && childRuleTuples.size() > 0) {
            	Iterator<RuleTuple> iter = childRuleTuples.iterator();
            	if (iter == null)
            		Log.e(TAG, "nothing to process");
            	else {
            		while (iter.hasNext()) {
            			RuleTuple childRuleTuple = iter.next();
            			boolean isActive = childRuleTuple.getActive() == Active.ACTIVE;
                        
                        if(isActive) {                  
                            if(LOG_DEBUG) Log.d(TAG, "Start SmartRulesService to delete active " +
                                    "           child rule " + childRuleTuple._id );
                            // The rule will be deleted in the service - so that even if the user 
                            // accidentally hits the home key the rule is actually deleted
                            // and does not show up in Ready/Disabled state.
                            Intent serviceIntent = new Intent(context, SmartRulesService.class);
                            serviceIntent.putExtra(MM_RULE_KEY, childRuleTuple.getKey());
                            serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
                            serviceIntent.putExtra(MM_DELETE_RULE, true);
                            context.startService(serviceIntent);
                        } else {
                            if(LOG_DEBUG) Log.d(TAG, "Deleting non-active child rule "+childRuleTuple._id);
                        
                            // Delete all child rule conditions
                            ConditionTable.deleteAllConditions(context, childRuleTuple._id);
                            
                            // Delete all child rule actions
                            ActionTable.deleteAllActions(context, childRuleTuple._id);
                            
                            // Delete the child Rule Table entry
                            whereClause = Columns._ID + EQUALS + childRuleTuple._id;
                            new RuleTable().deleteWhere(db, whereClause);
                        }
            		}
            	}	
            }
                      
            // Delete rule record
            whereClause = Columns._ID + EQUALS + tuple._id;
            result = new RuleTable().deleteWhere(db, whereClause);
            
            // entire delete successful without a crash
            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            db.endTransaction();
            db.close(TAG+".1");
        }   	
    	return result;
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
        if (_tuple instanceof RuleTuple) {
	        RuleTuple tuple = (RuleTuple) _tuple;
	
	        if (tuple.get_id() > 0) {
	            args.put(Columns._ID, 					tuple.get_id());
	        }
	        args.put(Columns.ENABLED, 					tuple.getEnabled());
	        args.put(Columns.RANK, 						tuple.getRank());
	        args.put(Columns.KEY, 						tuple.getKey());
	        args.put(Columns.ACTIVE, 					tuple.getActive());
	        args.put(Columns.RULE_TYPE,					tuple.getRuleType());
	        args.put(Columns.SOURCE, 					tuple.getSource());
	        args.put(Columns.COMMUNITY_RATING,			tuple.getCommunityRating());
	        args.put(Columns.COMMUNITY_AUTHOR,			tuple.getCommunityAuthor());
	        args.put(Columns.FLAGS, 					tuple.getFlags());
	        args.put(Columns.NAME, 						tuple.getName());
	        args.put(Columns.DESC, 						tuple.getDesc() );
	        args.put(Columns.TAGS, 						tuple.getTags() );
	        args.put(Columns.CONDITIONS, 				tuple.getConditions());
	        args.put(Columns.INFERENCE_LOGIC,			tuple.getInferLogic());
	        args.put(Columns.INFERENCE_STATUS,			tuple.getInferStatus());
	        args.put(Columns.SUGGESTED_STATE, 			tuple.getSuggState());
	        args.put(Columns.SUGGESTED_REASON,			tuple.getSuggested());
	        args.put(Columns.LIFECYCLE, 				tuple.getLifecycle());
	        args.put(Columns.SILENT, 					tuple.getSilent());
	        args.put(Columns.VSENSOR,					tuple.getVSensor());
	        args.put(Columns.RULE_SYNTAX,				tuple.getRuleSyntax());
	        args.put(Columns.LAST_ACTIVE_DATE_TIME,		tuple.getLastActiveDateTime());
	        args.put(Columns.LAST_INACTIVE_DATE_TIME,	tuple.getLastInactiveDateTime());
	        args.put(Columns.CREATED_DATE_TIME,			tuple.getCreatedDateTime());
	        args.put(Columns.LAST_EDITED_DATE_TIME, 	tuple.getLastEditedDateTime());
	        args.put(Columns.ICON,						tuple.getIcon());
	        args.put(Columns.SAMPLE_FKEY_OR_COUNT, 		tuple.getSampleFkOrCount());
	        args.put(Columns.PUBLISHER_KEY,				tuple.getPublisherKey());
	        args.put(Columns.VALIDITY,					tuple.getValidity());
	        args.put(Columns.UI_INTENT,					tuple.getUiIntent());
	        args.put(Columns.PARENT_RULE_KEY, 			tuple.getParentRuleKey());
	        args.put(Columns.ADOPT_COUNT, 				tuple.getAdoptCount());
        }
        return args;
    }


    /** returns a tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursort - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here,
     * can be null if select * was performed (all columns exist).
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends TupleBase> T toTuple(Cursor cursor, int[] tupleColumnNumbersIndex) {

        return (T) toTuple(cursor, tupleColumnNumbersIndex, 0);
    }


    /** returns a Rule tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursor - current position used to convert to tuple.
     * @param tupleColNos - contains column numbers which must be in sync here
     * @param ix - current index position in tupleColumnNumbersIndex to start.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    private RuleTuple toTuple(Cursor cursor, int[] tupleColNos, int ix) {


        if (tupleColNos == null) {
            tupleColNos = TableBase.getColumnNumbers(cursor, COLUMN_NAMES);
            ix = 0;
        }

        RuleTuple tuple = new RuleTuple(
            // NOTE: The order of these must match the physical order in the CREATE_TABLE syntax above, as well as the constructor
            cursor.getLong(tupleColNos[ix++]), 				// rule  _id
            cursor.getInt(tupleColNos[ix++]), 				// enabled
            cursor.getFloat(tupleColNos[ix++]),				// user rank
            cursor.getString(tupleColNos[ix++]),			// unique rule key
            cursor.getInt(tupleColNos[ix++]), 				// active
            cursor.getInt(tupleColNos[ix++]), 				// rule type
            cursor.getInt(tupleColNos[ix++]), 				// source
            cursor.getInt(tupleColNos[ix++]), 				// community rating
            cursor.getString(tupleColNos[ix++]),			// community author
            cursor.getString(tupleColNos[ix++]),			// Flags
            cursor.getString(tupleColNos[ix++]),			// Name
            cursor.getString(tupleColNos[ix++]),			// Description
            cursor.getString(tupleColNos[ix++]),			// Tags
            cursor.getString(tupleColNos[ix++]),			// Conditionals
            cursor.getString(tupleColNos[ix++]),			// Infer Logic
            cursor.getString(tupleColNos[ix++]),			// Infer Status
            cursor.getInt(tupleColNos[ix++]),				// Suggested State
            cursor.getString(tupleColNos[ix++]),			// SUGGESTED
            cursor.getLong(tupleColNos[ix++]),				// Lifecycle
            cursor.getInt(tupleColNos[ix++]),				// Silent
            cursor.getString(tupleColNos[ix++]),			// VSENSOR
            cursor.getString(tupleColNos[ix++]),			// RULE_SYNTAX
            cursor.getLong(tupleColNos[ix++]),				// last active date / time
            cursor.getLong(tupleColNos[ix++]),				// last inactive date / time
            cursor.getLong(tupleColNos[ix++]),				// created date / time
            cursor.getLong(tupleColNos[ix++]),				// last edited date / time
            cursor.getString(tupleColNos[ix++]),			// icons
            cursor.getLong(tupleColNos[ix++]),				// Sample Rule FK ID
            cursor.getString(tupleColNos[ix++]),			// Publisher key
            cursor.getString(tupleColNos[ix++]),			// Validity
            cursor.getString(tupleColNos[ix++]),			// Ui Intent
            cursor.getString(tupleColNos[ix++]),			// Parent Rule Key
            cursor.getLong(tupleColNos[ix++])				// Adopt Count
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
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getColumnNumbers
     * 				(android.database.Cursor, java.lang.String, java.lang.String)
     */
    public int[] getColumnNumbers(Cursor cursor, String sqlRef) {

        String[] colNames = new String[COLUMN_NAMES.length];
        colNames[0] = getRowIdColName(cursor);

        for (int i=1; i< colNames.length; i++) {
            colNames[i] = sqlRef+COLUMN_NAMES[i];
        }
        return TableBase.getColumnNumbers(cursor, colNames);
    }



    /* (non-Javadoc)
     * @see com.motorola.contextual.smartrules.db.table.TableBase#insert(android.content.Context, android.content.ContentValues)
     */
    @Override
    public long insert(Context context, ContentValues values) {

        // default the date / time if either the value is null in values, or values is missing key
        if (values.get(Columns.CREATED_DATE_TIME) == null)
            values.put(Columns.CREATED_DATE_TIME, new Date().getTime());
        values.put(Columns.LAST_EDITED_DATE_TIME, 0);
        // default the Flags to empty String if either the value is null in values, or values is missing key
        if (values.get(Columns.FLAGS) == null)
            values.put(Columns.FLAGS, "");
        // default Enabled column to Disabled if not supplied
        if (values.get(Columns.ENABLED) == null)
            values.put(Columns.ENABLED, Enabled.DISABLED);
        // default RuleType column to Automatic if not supplied
        if(values.get(Columns.RULE_TYPE) == null)
            values.put(Columns.RULE_TYPE, RuleType.AUTOMATIC);
        // default Active column to Inactive if not supplied
        if (values.get(Columns.ACTIVE) == null)
            values.put(Columns.ACTIVE, Active.INACTIVE);
        if(values.get(Columns.SAMPLE_FKEY_OR_COUNT) == null)
        	values.put(Columns.SAMPLE_FKEY_OR_COUNT, DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE);
        return super.insert(context, values);
    }


    /** initialize using context rather than Db. This is for debugging the .initialize method.
     *
     * @param context - context
     */
    public static void initialize(final Context context) {
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".0");
        try {
            new RuleTable().initialize(db.getDb(), context);

        } catch (Exception e) {
            Log.e(TAG, ".initialize failed");
            e.printStackTrace();

        } finally {
            if(db != null)
                db.close(TAG+".0");
        }
    }

    /** creates the 1 and only default rule
     * @param db - context
     */
    @Override
    public void initialize(final SQLiteDatabase db, final Context context) {

        // delete the existing record if one exists.
        int d = db.delete(getTableName(), Columns.RULE_TYPE+EQUALS+RuleType.DEFAULT, null);
        if (LOG_DEBUG) Log.d(TAG, "Deleted default:"+d+" records");

        RuleTuple t = new RuleTuple();
        t.setEnabled(Enabled.ENABLED);
        t.setRuleType(RuleType.DEFAULT);
        t.setActive(Active.ACTIVE);
        t.setName(context.getString(R.string.default_rule));
        t.setDesc(context.getString(R.string.default_values_rule));
        t.setSource(Source.DEFAULT);
        t.setKey(DEFAULT_RULE_KEY);
        t.setPublisherKey(null);
        t.setVSensor("null");
        t.setLastActiveDateTime(new Date().getTime());
        t.setFlags("");
        t.setIcon(DEFAULT_RULE_ICON);
        t.setValidity(TableBase.Validity.VALID);
        t.setSampleFkOrCount(DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE);
        t.setAdoptCount(DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE);
        long i = db.insert(getTableName(), null, toContentValues(t));
        if (i < 0)
            Log.e(TAG, "insert failed!");
    }
}