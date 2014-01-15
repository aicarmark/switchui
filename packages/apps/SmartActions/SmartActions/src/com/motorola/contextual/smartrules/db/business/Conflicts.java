/*
 * @(#)Conflicts.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/10/03 NA                Initial Version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.Arrays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.RuleTable.Columns;

/** This class manages rules conflict resolution. Conflicts occur for 2 reasons:
 *<code><pre>
 *
 * 1.) CONFLICTING ACTIONS BETWEEN RULES
 *
 * As a result of a plurality of rules attempting to set the same setting to different states.
 * they result from conflicting actions which are stateful. Stateful actions are those that set
 * settings that persist for a period of time such as screen brightness, WiFi on/off, phone
 * volume, etc. That is, the action of sending an SMS cannot conflict with another action
 * to send an SMS because those are actions are transient, not stateful.	 *
 *
 * 2.) CONFLICTING PRECONDITION AND ACTION IN SAME RULE
 * The conflicts also occur as a result of preconditions or triggers which are stateless
 * firing an action which is stateful. An example: a stateful trigger
 * is arriving and leaving a known location. An example of a stateless trigger is a missed call.
 * There are also stateful and stateless actions (i.e. a setting such as WiFi on or
 * off is stateful).  When the user creates a rule where a stateless trigger fires a stateful
 * action (i.e. a setting), then the action would be immediately reverted and therefore
 * appear to have never been fired. See some examples below.
 *
 *
 * CONFLICTING ACTIONS:
 *
 * There are "stacks" of actions. These stacks are not explicitly a stack, therefore we called
 * them decks instead of stacks.
 *
 * Each "deck" is defined by the action key. An action key is unique to each setting or
 * transient action. For example, the volume setting might have a key value of
 * com.motorola.quickactions.Volume, whereas the WiFi setting may have a key value of
 * com.motorola.quickactions.WiFi. Subsequently, the action key has a 1-to-1 correspondence
 * with the deck (each unique action key represents a deck).
 *
 * The order of the deck is a stack of sorts, in which the highest priority is at the top of
 * the "stack," conversely the lowest priority represents the default setting (which would appear
 * at the bottom of the "stack."  The default setting is the value of the setting prior to any or
 * all modes changing the setting or state.
 *
 * For example, Rule 1 may have one precondition of when arriving at Home set WiFi on. Another rule,
 * Rule 2, may have the preconditions, when at home, and after 10pm go into power saving mode,
 * which includes turning off WiFi. This creates the conflict on the WiFi setting. Does the WiFi stay
 * on or turn off. Most people would clearly say it should turn off. However, making that decision in
 * code in a predictable way -- that is most likely to satisfy the user -- is what conflict resolution
 * is all about.
 *
 * This ConflictResolution class and associated algorithm attempts to resolve conflicts.
 *
 * APPROACH:
 *
 *   This class attempts to use somewhat of a stack approach to determining the order of preference
 *   of the conflict resolution. The stack analogy isn't quite right because modes are not always
 *   pushed and popped from the "stack." That is, they can be inserted in the middle of the stack.
 *   Therefore, we're calling them decks.
 *
 *   Here is a text-based diagram:
 *
 *      Active Modes List				  Action decks List (showing modes)
 *      (one "deck" per active mode)
 *      
 *      Order of Mode Activation:
 *      Mode A - "default" mode           Controlling Mode is topmost
 * 		Mode B - Home Mode                (one deck per active mode action)
 *		Mode C - Sleep Mode            
 *		Mode D - Low Bat Mode
 *
 *      "Default" modes are:                                     (current state)
 *          Wifi - off                                                      (Off)
 *          Ringer - Loud                                        (Silent)     D
 *		Sample Modes & assoc. actions:                               C        C
 *       Conditions: when at home and after 10pm:                    B        B
 * 		   Sleep Mode (C)		    		   "Default" Mode -> |   A   |  | A |  |   |
 *           com.mot.ringer = silent					         +-------+  +---+  +---+   ...
 *           com.mot.wifi = off                deck or Stack ->   Ringer     WiFi    
 *       Conditions: when at home    
 *         Home Mode (B)
 *           set com.mot.ringer = loud         Actions:      Based on publisher   (current state - winner)
 *           set com.mot.wifi = on             deck Ringer   com.mot.ringer (currently silent, via Sleep mode)
 *       Conditions: when battery is low       deck WiFi     com.mot.wifi   (currently off, via Low Bat mode)
 *         Low Battery Mode (D)                  
 *           com.mot.wifi = off               
 *
 *  Diagram Narrative:
 *      Ringer Action Control (deck 1) 
 *         Sleep Mode is the winner - therefore the ringer is silent, 
 *         Sleep Mode wins because: 
 *             Sleep Mode has 2 preconditions (has the most preconditions)
 *             Even if it had only 1 precondition, it would still win because chronologically it was the most
 *                 recent rule trying to control the Ringer volume. 
 *      Wifi State Control (deck 2) 
 *         Low Battery Mode is the winner because low battery  
 *
 *      IF the user were to turn on WiFi manually at this point (with low battery), then the
 *      deck would be flushed (all entries in queue erased) until the next time the rule activates 
 *      again (which could be very soon after) but that is the design decision we've made at this point.
 *
 *  
 * CLASS:
 * 	Extends nothing.
 *
 * RESPONSIBILITIES:
 * 	Determine which action "wins" during a conflict.
 *
 * COLABORATORS:
 * 	SmartRulesService and CheckSettings Activity (user view of conflict resolution)
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class Conflicts extends BroadcastReceiver implements Constants, DbSyntax {

    public static final String CONFLICT_RESOLUTION_ACTION = "com.motorola.contextual.FIRE_RULE";
    private static final String TAG = Conflicts.class.getSimpleName();
    private static final boolean LOG_DEBUG = true;

    /** see class-level @see Conflicts documentation for more information about conflicts */
    public enum Type { ALL_POSSIBLE, ACTIVE_ONLY}

    public static Type convert(int value) {
    	if(Type.class.getEnumConstants() != null)
    		return Type.class.getEnumConstants()[value];
    	else
    		return null;
    }
    ;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (LOG_DEBUG) Log.d(TAG, TAG+".onReceive");
        String action = intent.getAction();
        if(action != null) {
            if (action.equals("com.motorola.contextual.FIRE_RULE")) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String key = bundle.getString(Columns.KEY);
                    if (key != null)
                        fireThreadToActivate(key);

                }
            }
        }
    }


    private void fireThreadToActivate(String key) {

    }


    /** determines if rule is conflicting with another currently active rule.
     *
     * @param key - rule table _id which with a possible conflict with another rule
     * @return > 0 if rule conflicts with another rule. If < 1, no conflict exists.
     * if > 0, the return value is the key of the rule which is currently at the top
     * of the stack (strongest rule).
     */
    public static long isConflictingBetweenRules(Context context, long key) {

        long result = 0;
        String[] selection = {ActionTable.Columns.PARENT_FKEY, ActionTable.Columns.ACTION_PUBLISHER_KEY};
        String orderByClause = null;
        String whereClause = ActionTable.Columns.PARENT_FKEY+EQUALS+key;
        Cursor actions = new ActionTable().fetchWhere(context, selection, whereClause, null, orderByClause, 0);
        Cursor cursor = null;
        if (actions != null)
            try {
                if (actions.moveToFirst()) {
                    cursor = getConflictingActionsCursor(context, /*key,*/ actions.getString(1),
                                                         Conflicts.Type.ACTIVE_ONLY);
                    if (cursor != null) {
                        //TODO: need to check if our key appears as the only item in the list
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
                actions.close();
            }

        return result;
    }

    /** Condition record count */
    public static final String COND_CNT = "CondCnt";
    /** Action table _id field */
    public static final String ACTION_ID = "Action_id";
    /** abbreviation for ActionPublisherKey */
    private static final String ACT_PUBL_KEY = ActionTable.Columns.ACTION_PUBLISHER_KEY;



    /** Second=level Sort-order.  This comment block covers sort-order 1 thru 4.
     * Remember, the one at the top of the cursor is the winner (takes control)
     *
     * <code><pre>
     * The sort-order is as follows:
     *  Sort-Order 1 (descending) -
     *       1 = Manual Rule     (manually activated rules always win over automatically activated)
     *       0 = Automatic Rule   
     *      -1 = Default Rule   
     *      
     *  Sort-Order 2 (descending): orders manually-activated rules ahead of automatically fired rules
     *       IF (manual Rule)
     *          conflicting manual rules, chronological ordering wins (most recent active wins over older) 
     *       ELSE
     *          Set to zero (for either Default Rule or Automatic Rules)
     *          
     *  Sort-Order 3 (descending): orders automatic visible rules ahead of hidden rules       
     *       IF (automatically fired rule)
     *           IF (hidden)
     *              return 0  (no priority over automatic)
     *           ELSE
     *              1
     *       ELSE
     *           Set to zero   
     *              
     *  Sort-Order 4 (descending): orders automatic by precondition count       
     *       IF (automatically fired rule)
     *           IF (hidden)
     *              return 0  (no priority over automatic)
     *           ELSE
     *              return precondition count.
     *       ELSE
     *           Set to zero   
     *              
     *  Sort-Order 5 (descending):        
     *       IF (automatically fired rule)
     *           return time of last rule invocation
     *       ELSE
     *           Set to zero   
     * </pre></code>
     */
    private static final String SORT_ORDER_LEVEL_2 =
        CASE+
           WHEN+ RuleTable.SQL_REF+"."+Columns.RULE_TYPE+ EQUAL+ RuleTable.RuleType.MANUAL+				            
                THEN+
                    RuleTable.SQL_REF+"."+Columns.LAST_ACTIVE_DATE_TIME+
           ELSE+
                "0"+   
        END+ " order2";

    /** Third-level sort order - descending. @see SORT_ORDER_LEVEL_2 for additional details*/
    private static final String SORT_ORDER_LEVEL_3 =
        CASE+
            WHEN+ RuleTable.SQL_REF+"."+Columns.RULE_TYPE+EQUAL+ RuleTable.RuleType.AUTOMATIC+                    
                THEN+
                    CASE + 
                       // handle hidden automatic rules
                       WHEN+ Columns.FLAGS+LIKE+Q+WILD+RuleTable.Flags.INVISIBLE+WILD+Q+
                           THEN+ 
                               "0"+
                       ELSE+
                           "1"+
                       END+
            ELSE+
                "0"+                           	
        END+ " order3";
    
    /** Fourth-level sort order - descending. @see SORT_ORDER_LEVEL_2 for additional details*/
    private static final String SORT_ORDER_LEVEL_4 =
        CASE+
            WHEN+ RuleTable.SQL_REF+"."+Columns.RULE_TYPE+EQUAL+ RuleTable.RuleType.AUTOMATIC+                    
                THEN+
                    "count(c._id)"+
            ELSE+
                "0"+                           	
        END+ " order4";
    
    /** Fifth-level sort order - descending. @see SORT_ORDER_LEVEL_2 for additional details*/
    private static final String SORT_ORDER_LEVEL_5 =
        CASE+
            WHEN+ RuleTable.SQL_REF+"."+Columns.RULE_TYPE+EQUAL+ RuleTable.RuleType.AUTOMATIC+
                 THEN+
            	    RuleTable.SQL_REF+"."+Columns.LAST_ACTIVE_DATE_TIME+
            ELSE+
                 "0"+                           	
        END+ " order5";

    /** This is the progression of testing to arrive at the query to select conflicting actions
     * <code><pre>
     *
     *
     * adb -d shell
     * cd data/data
     * cd (this package) com.motorola.contextual.smartrules/databases
     * sqlite3 smartrules.db
     * .header on
     *
     * -- this works --
     * select r1._id, r1.act, a._id, count(c1._id) as cnt, 'com.motorola.contextual.actions.wifi' as apk
     *   from Rule as r1
     *   inner join Condition as c1
     *   inner join Action as a
     *   where
     *       r1._id=c1.FKRule_id AND
     *       r1._id=a.FKRule_id AND
     *       a.ActPubKey = 'com.motorola.contextual.actions.wifi' AND
     *        r1._id !=             19                             AND    -- this is the rule being checked for conflicts
     *        r1.act = 0                                                  -- will need to change to act = 1 for active rules
     *  GROUP BY r1._id
     *  ORDER by cnt desc;
     *
     *  returns: (first _id is rule._id, second is action._id)
     *
     *  _id|Act|_id|cnt|ActPubKey
     *  ----------------------------------------------------
     *   15|  0| 97|  2|com.motorola.contextual.actions.wifi
     *   16|  0| 98|  2|com.motorola.contextual.actions.wifi
     *    1|  0|  2|  1|com.motorola.contextual.actions.wifi
     *    5|  0| 14|  1|com.motorola.contextual.actions.wifi
     *    9|  0| 29|  1|com.motorola.contextual.actions.wifi
     *
     * btw: don't need quotes around parms if they don't contain spaces.
     *
     *
     * This is what the SQL looks like after parm substitution:
     *
     *  SELECT  r._id,
     *         r.Manual,
     *         CASE  WHEN  r.Manual = 1 THEN  r.LastActDT ELSE 0 END  order1,
     *         r.LastActDT,
     *         r.Act,
     *         r.Name,
     *         a._id AS Action_id,
     *         count( c._id) as CondCnt,
     *         'com.motorola.contextual.actions.wifi'  AS ActPubKey
     *    FROM Rule AS  r
     *      INNER JOIN Action AS  a ON  r._id =  a.FkRule_id
     *      LEFT OUTER JOIN Condition AS  c ON  r._id =  c.FkRule_id
     *    WHERE  r.Ena = 1 AND  r.Act LIKE  '%'  AND  a.ActPubKey =  'com.motorola.contextual.actions.wifi'
     *  GROUP BY  r._id
     *  ORDER BY Manual DESC, order1 DESC, CondCnt DESC
     *
     *</pre><code>
     */
    private static final String CONFLICTING_SQL_SELECT =
        SELECT+
        RuleTable.SQL_REF+"."+Columns._ID							+CONT+
        RuleTable.SQL_REF+"."+Columns.KEY							+CONT+
        RuleTable.SQL_REF+"."+Columns.RULE_TYPE						+CONT+
        RuleTable.SQL_REF+"."+Columns.LAST_ACTIVE_DATE_TIME			+CONT+
        RuleTable.SQL_REF+"."+Columns.ACTIVE						+CONT+
        RuleTable.SQL_REF+"."+Columns.NAME							+CONT+
        RuleTable.SQL_REF+"."+Columns.ICON							+CONT+
        ActionTable.SQL_REF+"."+ActionTable.Columns.STATE_MACHINE_NAME		    +CONT+
        ActionTable.SQL_REF+"."+ActionTable.Columns.ACTION_DESCRIPTION				+CONT+
        ActionTable.SQL_REF+"."+ActionTable.Columns.ACTIVITY_INTENT			    +CONT+
        ActionTable.SQL_REF+"."+ActionTable.Columns.CONFIG          		    +CONT+
        ActionTable.SQL_REF+"."+ActionTable.Columns._ID+AS+ACTION_ID			+CONT+
            "count("+ConditionTable.SQL_REF+"."+ConditionTable.Columns._ID+") as "+COND_CNT +CONT+
            SORT_ORDER_LEVEL_2													+CONT+
            SORT_ORDER_LEVEL_3													+CONT+
            SORT_ORDER_LEVEL_4													+CONT+
            SORT_ORDER_LEVEL_5													+CONT+
            PARM+AS+ACT_PUBL_KEY+
        FROM+
            RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
        INNER_JOIN+
            ActionTable.TABLE_NAME+AS+ActionTable.SQL_REF+
                ON+RuleTable.SQL_REF+"."+Columns._ID+EQUALS+ActionTable.SQL_REF+'.'+
                ActionTable.Columns.PARENT_FKEY+
            LEFT_OUTER_JOIN+
                ConditionTable.TABLE_NAME+AS+ConditionTable.SQL_REF+
                ON+RuleTable.SQL_REF+"."+Columns._ID+EQUALS+ConditionTable.SQL_REF+'.'+
                ConditionTable.Columns.PARENT_FKEY
        ;

    private static final String CONFLICT_SQL =
        CONFLICTING_SQL_SELECT+
        WHERE+
        // rule must be enabled
           RuleTable.SQL_REF+'.'+Columns.ENABLED+EQUALS+RuleTable.Enabled.ENABLED
           +AND+
              // action must be enabled (disabled = not connected visually)
              ActionTable.SQL_REF+'.'+ActionTable.Columns.ENABLED+EQUALS+ActionTable.Enabled.ENABLED
              +AND+
                ActionTable.SQL_REF+'.'+ActionTable.Columns.ACTIVE+EQUALS+ActionTable.Active.ACTIVE
                +AND+
                   RuleTable.SQL_REF+'.'+Columns.ACTIVE+LIKE+PARM
                   +AND+
                   ActionTable.SQL_REF+'.'+ActionTable.Columns.ACTION_PUBLISHER_KEY+EQUALS+PARM+

        GROUP_BY+RuleTable.SQL_REF+"."+Columns._ID+
        ORDER_BY+
            Columns.RULE_TYPE +DESC	+CONT+
            "order2"          +DESC +CONT+
            "order3"          +DESC +CONT+
            "order4"          +DESC +CONT+
            "order5"          +DESC
        ;

    /*
     *
     *
     *
     * some other publisher keys:
    com.motorola.contextual.actions.bluetooth
    com.motorola.contextual.actions.wifi
    com.motorola.contextual.actions.changeringer
    com.motorola.contextual.actions.bluetooth
    com.motorola.contextual.actions.launchapp
    com.motorola.contextual.actions.changeringer
    com.motorola.contextual.actions.screentimeout
    com.motorola.contextual.actions.brightness
    com.motorola.contextual.actions.impresence
    com.motorola.contextual.actions.notification
    com.motorola.contextual.actions.brightness
    com.motorola.contextual.actions.bluetooth
    com.motorola.contextual.actions.gps
    com.motorola.contextual.actions.wifi
    */


    /** gets the actions currently conflicting (currently active modes) with a matching given
     * action publisher key.
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key to check for conflicts
     * @param conflictType - is the type of conflict to check for. The most common would
     *  be the active rule conflicts, which is type: Conflicts.Type.ACTIVE_ONLY
     * @return - cursor containing results or null if something went amuck.
     * <code><pre>
     *
     * if all went well, columns like these are returned:
     *
     *  _id|Act|_id|cnt|ActPubKey
     *  ----------------------------------------------------
     *   15|  0| 97|  2|com.motorola.contextual.actions.wifi
     *   16|  0| 98|  2|com.motorola.contextual.actions.wifi
     *    1|  0|  2|  1|com.motorola.contextual.actions.wifi
     *    5|  0| 14|  1|com.motorola.contextual.actions.wifi
     *    9|  0| 29|  1|com.motorola.contextual.actions.wifi
     *
     * -- the DumpCursor format:
     * 0 {
     *    _id=2
     *     Act=0
     *     Name=At home evening
     *	   Action_id=2
     *     cnt=2
     *     ActPubKey=com.motorola.contextual.actions.wifi
     *    }
     *
     *   where:
     *     _id 		 is the rule _id column
     *     Act		 is the indicator column that the rule/mode is active
     *     _id 		 is the action _id column
     *     cnt		 is the count of the number of Conditions (preconditions) that apply to the rule/mode to activate.
     *     ActPubKey is the action publisher key for the action, which relates to the rule in the _id column
     */
    public static Cursor getConflictingActionsCursor(
        final Context context,
        final String actionPublisherKey,
        final Conflicts.Type conflictType) {

        String conflictTypeStr = "";
        switch (conflictType) {
        case ACTIVE_ONLY:
            conflictTypeStr = Q+ActionTable.Active.ACTIVE+Q; // just active rules
            break;
        case ALL_POSSIBLE:
            conflictTypeStr = Q+LIKE_WILD+Q;  // any value
        }
        String[] selectionArgs = {
            Q+actionPublisherKey+Q,
            conflictTypeStr,
            Q+actionPublisherKey+Q,
        };

        // this is a kluge until Android fixes the Parm ? substitution
        String sql = substitute(CONFLICT_SQL, selectionArgs);
        selectionArgs = null;
        if (LOG_DEBUG) 
        	Log.d(TAG, "getConflictingActions - parms="+Arrays.toString(selectionArgs)+" sql="+sql);
        Cursor result = TableBase.rawQuery(context, false, sql, selectionArgs);
        if (LOG_VERBOSE) 
        	Log.v(TAG, "getConflictingActions - cursor="+
        		(result!=null ? DatabaseUtils.dumpCursorToString(result) : "null"));
        return result;
    }

    /** this is a kluge fix until Android fixes the parm substitution
     *
     * @param sql - sql to substitue in
     * @param parms - sql parms
     * @return
     */
    private static String substitute(final String sql, final String[] parms) {

        String splitOn = "\\?"; //+PARM;
        String[] split = sql.split(splitOn);
        //TODO: this logic could be a problem if the parm appears at the end of the SQL string.
        if (split.length-1 != parms.length)
            throw new IllegalArgumentException("Parm count("+parms.length+") and string counts("+(split.length-1)+") don't match ");

        // parms look good, replace
        StringBuilder b = new StringBuilder();
        // reassemble
        for (int i=0; i<parms.length; i++) {
            b.append(split[i]+parms[i]);
        }
        b.append(split[split.length-1]);

        return b.toString();
    }
}

