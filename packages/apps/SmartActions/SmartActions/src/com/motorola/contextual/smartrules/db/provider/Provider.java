/*
 * @(#)LocationProvider.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/02/24 NA				  Initial version
 * ACD100        2011/01/11 NA				  Massive revision to accommodate a singleton helper class as well as
 *                                            a SQLiteDatabase.CursorFactory to allow closing the Db instance at the
 *                                            time the cursor closes.
 *
 */
package com.motorola.contextual.smartrules.db.provider;

import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.view.ActiveLocCntView;
import com.motorola.contextual.smartrules.db.table.view.ActiveSettingsView;
import com.motorola.contextual.smartrules.db.table.view.AdoptedSampleListView;
import com.motorola.contextual.smartrules.db.table.view.DistinctConditionView;
import com.motorola.contextual.smartrules.db.table.view.RuleActionView;
import com.motorola.contextual.smartrules.db.table.view.RuleCloneView;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
import com.motorola.contextual.smartrules.db.table.view.RuleView;
import com.motorola.contextual.smartrules.db.table.view.RuleViewCnt;
import com.motorola.contextual.smartrules.db.table.view.TriggerStateCountView;
import com.motorola.contextual.smartrules.db.table.view.ViewBase;
import com.motorola.contextual.smartrules.db.table.view.VisibleEnaAutoRulesCntView;
import com.motorola.contextual.smartrules.util.Util;

/** Exposes to 3rd-party apps mainly the "trail" of captured points.
 *
 *<pre>
 * PACKAGE:
 *   The purpose of this package is to represent the schema of tables available to 
 *   consumers of contextual content.
 *
 * CLASS:
 * 	 extends:
 * 		ContentProvider - to make this easily visible to Motorola and other 3rd-party 
 * 		apps(eventually).
 *   implements:
 *   	UriMatch - to pickup the constants associated with Uri matching.
 *
 * RESPONSIBILITIES:
 *   Encapsulate data into a more generic representation of the data to allow consumers 
 *   (3rd-party apps) to seamlessly integrate that data into their applications.
 *
 * COLABORATORS:
 * 	 Our data will be exposed via the com.motorola.contextual.location package
 * 	 via the .federated.FederatedLocationDateProvider class (which is a front-end for
 *   Aloqa content as well as all the other location-data).
 *
 * USAGE:
 * 	 extend this class, implement the appropriate abstract methods.
 *
 **/
public class Provider extends SQLiteContentProvider implements Constants, DbSyntax {


    private static final String TAG = Provider.class.getSimpleName();

    private static final UriMatcher sUriMatcher;

    private static Context applicationContext;

    /**
     * Returns the value of applicationContext
     * @return processStartedDataCleared
     */
    public static Context getApplicationContext() {
        return applicationContext;
    }

    // see superclass for JavaDoc
    @Override
    public boolean onCreate() {
        // superclass instantiates the DbHelper
        super.onCreate();
        applicationContext = getContext().getApplicationContext();
        // if we are running, then we are "loaded" so return true.
        return true;
    }



    
    // see superclass for JavaDoc
    @Override
    public String getType(Uri uri) {

        String result = null;
        switch (sUriMatcher.match(uri)) {

            // views
        case UriMatch.RULE_VIEW:
            return new RuleView()					.getContentType();
        case UriMatch.RULE_VIEW_ID:
            return new RuleView()					.getContentItemType();
        case UriMatch.RULE_ACTION_VIEW:
            return new RuleActionView()				.getContentType();
        case UriMatch.RULE_ACTION_VIEW_ID:
            return new RuleActionView()				.getContentItemType();
        case UriMatch.RULE_CONDITION_VIEW:
            return new RuleConditionView()			.getContentType();
        case UriMatch.RULE_CONDITION_VIEW_ID:
            return new RuleConditionView()			.getContentItemType();
        case UriMatch.RULE_VIEW_CNT:
            return new RuleViewCnt()				.getContentType();
        case UriMatch.RULE_VIEW_CNT_ID:
            return new RuleViewCnt()				.getContentItemType();
        case UriMatch.TRIGGER_STATE_CNT_VIEW:
        	return new TriggerStateCountView()		.getContentType();
        case UriMatch.ACTIVE_SETTINGS_VIEW:
        	return new ActiveSettingsView()			.getContentType();
        case UriMatch.DISTINCT_CONDITION_VIEW:
            return new DistinctConditionView()			.getContentType();
        case UriMatch.DISTINCT_CONDITION_VIEW_ID:
            return new DistinctConditionView()			.getContentItemType();
            // virtual views
        case UriMatch.ACT_LOC_COND_VIEW_CNT:
        	return new RuleConditionView()			.getContentType(); 
        case UriMatch.VISIBLE_ENA_AUTO_RULES_VIEW_CNT:
        	return new RuleTable()					.getContentType();
        case UriMatch.RULE_CLONE_VIEW:
        	return new RuleConditionView()			.getContentType();
        case UriMatch.ADOPTED_SAMPLE_LIST_VIEW:
        	return new RuleTable()					.getContentType();
        	
            // tables
        case UriMatch.RULE:
            return new RuleTable()					.getContentType();
        case UriMatch.RULE_ID:
            return new RuleTable()					.getContentItemType();
        case UriMatch.ACTION:
            return new ActionTable()				.getContentType();
        case UriMatch.ACTION_ID:
            return new ActionTable()				.getContentItemType();
        case UriMatch.CONDITION:
            return new ConditionTable()				.getContentType();
        case UriMatch.CONDITION_ID:
            return new ConditionTable()				.getContentItemType();
        case UriMatch.ICON:
            return new IconTable()				.getContentType();
        case UriMatch.ICON_ID:
            return new IconTable()				.getContentItemType();

        default:
            Log.e(TAG, "URI is not defined"+uri.toString());
            //throw new IllegalArgumentException(TAG+".getType Unknown URI " + uri);
        }
        return result;
    }


    // see superclass for JavaDoc
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        if (LOG_DEBUG) Log.d(TAG, TAG+".query uri="+uri.toString()
        						+" sel="+selection+" proj="+Arrays.toString(projection));

        Cursor result = null;
        int match = sUriMatcher.match(uri);
        long _id = 0;

        switch (match) {

            // VIEWS
        case UriMatch.RULE_VIEW:
        case UriMatch.RULE_ACTION_VIEW:
        case UriMatch.RULE_CONDITION_VIEW:
        case UriMatch.RULE_VIEW_CNT:
        case UriMatch.TRIGGER_STATE_CNT_VIEW: 
        case UriMatch.ACTIVE_SETTINGS_VIEW:
        case UriMatch.DISTINCT_CONDITION_VIEW:
            // all rows meeting selection
            result = fetchFromView(this.getContext(), uri, projection, selection, 
            							sortOrder, 0, selectionArgs);
            break;
        	
        case UriMatch.RULE_VIEW_ID:
        case UriMatch.RULE_ACTION_VIEW_ID:
        case UriMatch.RULE_CONDITION_VIEW_ID:
        case UriMatch.RULE_VIEW_CNT_ID:
        case UriMatch.DISTINCT_CONDITION_VIEW_ID:
            // one row
            _id = Long.parseLong(uri.getLastPathSegment());
            selection = BaseColumns._ID +"="+_id;
            result = fetchFromView(this.getContext(), uri, projection, selection, 
            							sortOrder, 0, selectionArgs);
            break;

            // Virtual Views
        case UriMatch.ACT_LOC_COND_VIEW_CNT:
        	
        	selection = 
			ConditionTable.Columns.CONDITION_PUBLISHER_KEY
        			+ EQUALS + Q + LOCATION_TRIGGER_PUB_KEY + Q 
				+ AND +
					ConditionTable.Columns.ENABLED 
						+ EQUALS + Q + ConditionTable.Enabled.ENABLED + Q
				+ AND + 
					RuleTable.Columns.ACTIVE + EQUALS + Q + RuleTable.Active.ACTIVE + Q;
        	
        	projection = new String[] {"count (*)"};
        	// Appending RuleConditionView.VIEW_NAME so that this view is used as the real 
        	// view behind the virtual view.
        	uri = Uri.withAppendedPath(uri, RuleConditionView.VIEW_NAME);
            result = fetchFromView(this.getContext(), uri, projection, selection, 
            							null, 0, selectionArgs);
        	break;

        case UriMatch.VISIBLE_ENA_AUTO_RULES_VIEW_CNT:
        	selection = 
        		RuleTable.Columns.ENABLED + EQUALS + Q + RuleTable.Enabled.ENABLED + Q 
				+ AND + RuleTable.Columns.RULE_TYPE + EQUALS 
						+ Q + RuleTable.RuleType.AUTOMATIC + Q
				+ AND + RuleTable.Columns.FLAGS 
						+ IS_NOT_LIKE + Q + WILD + RuleTable.Flags.INVISIBLE + Q
	        	+ AND + RuleTable.Columns.FLAGS 
	        			+ IS_NOT_LIKE + Q + WILD + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q
	        	+ AND + RuleTable.Columns.SOURCE 
	        			+ NOT_EQUAL + Q + RuleTable.Source.DEFAULT + Q;
        	projection = new String[] {"count (*)"};
            result = fetchFromTable(this.getContext(), Schema.RULE_TABLE_CONTENT_URI, 
            							projection, selection, selectionArgs, sortOrder, 0);
        	break;

        case UriMatch.ADOPTED_SAMPLE_LIST_VIEW:
        	if(selectionArgs != null && selectionArgs[0] != null)
        		result = fetchAdoptedSampleList(this.getContext(), selectionArgs);
        	else 
        		Log.e(TAG, "input params selectionArgs is "
    					+(selectionArgs == null ? "null" : Arrays.toString(selectionArgs))
    					+" returning a null cursor to caller");
        	break;
        	
            // TABLES
        case UriMatch.RULE:
        case UriMatch.ACTION:
        case UriMatch.CONDITION:
        case UriMatch.BUCKET_TABLE:

            // all rows meeting selection
            result = fetchFromTable(this.getContext(), uri, projection, selection, 
            							selectionArgs, sortOrder, 0);
            break;

        case UriMatch.RULE_ID:
        case UriMatch.ACTION_ID:
        case UriMatch.CONDITION_ID:
        case UriMatch.BUCKET_TABLE_ID:

            // one row
            _id = Long.parseLong(uri.getLastPathSegment());
            selection = BaseColumns._ID+"="+_id;
            result = fetchFromTable(this.getContext(), uri, projection, selection, 
            							selectionArgs, sortOrder, 1);
            break;


        default:
            Log.e(TAG, TAG+".query - URI not matched:"+uri.toString()+ " sel="+selection);
        }

        if (LOG_DEBUG) {
            Log.d(TAG, TAG+".query exiting, returning cursor below: uri="+uri.toString());
            DatabaseUtils.dumpCursor(result);
        }
        return result;
    }

    /** fetches the adopted list of sample rules for the rule key passed in the 
     *  selection arguments
     *  
     * @param context - context
     * @param selectionArgs - selection arguments that apply to this query
     * @return - cursor of results read from the DB query
     */
    public Cursor fetchAdoptedSampleList(Context context, String[] selectionArgs) {
        Cursor result = null;
    	String R1 = RuleTable.SQL_REF+"1";
    	String R2 = RuleTable.SQL_REF+'2';
    	
    	// select R2.* 
    	// from Rule R1
	// join Rule R2 on R1.Key = R2.ParentRuleKey
    	// where R1.key = 'com.motorola.contextual.Car%20Rule.1299861367137' 
    	// and R2.flags not like '%s%' and R2.flags not like '%i%';        	
    	String whereClause =  
    		SELECT + R2 + "." + ALL 
			+ FROM  + RuleTable.TABLE_NAME + BLANK_SPC + R1
			+ JOIN + RuleTable.TABLE_NAME + BLANK_SPC + R2  
			+ ON + R1 + "." + RuleTable.Columns.KEY + EQUALS
				+ R2 + "." + RuleTable.Columns.PARENT_RULE_KEY
			+ WHERE + R1 + "." + RuleTable.Columns.KEY 
					+ EQUALS + Q + selectionArgs[0] + Q
				+ AND 
					+ R2 + "." + RuleTable.Columns.FLAGS 
						+ IS_NOT_LIKE + Q + WILD + RuleTable.Flags.INVISIBLE + WILD + Q
				+ AND 
					+ R2 + "." + RuleTable.Columns.FLAGS 
						+ IS_NOT_LIKE + Q + WILD + RuleTable.Flags.SOURCE_LIST_VISIBLE 
							+ WILD + Q;        	

        SQLiteManager db = SQLiteManager.openForWrite(this.getContext(), TAG+".1");
        db.beginTransaction();
        try {
        	result = db.rawQuery(whereClause, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            db.close(TAG+".1");
        }   	        
        return result;
    }
    
    /** fetches a view using the supplied where clause and order by clause.
     * Remember to close the cursor when finished.
     *
     * @param context - context
     * @param Uri - uri from query request
     * @param projection - column names - or null for all column names
     * @param selection - where clause - excluding "where"
     * @param orderByClause - order by clause - excluding "order by"
     * @param limit - max number of records to return or 0 if no limit
	 * @param selectionArgs - arguments that apply to the query	 
     * @return - cursor of results read from the database
     */
    public Cursor fetchFromView(final Context context, final Uri uri,
            final String[] projection, String selection,
            final String orderByClause, int limit,
            final   String[] selectionArgs) {

        Cursor cursor = null;
        String viewName = uri.getLastPathSegment();

        try {
            String fullyQualifiedClassName = 
            			ViewBase.class.getPackage().getName()+"."+viewName;
            Object o = Class.forName(fullyQualifiedClassName).newInstance();

            if (o instanceof ViewBase) {
                ViewBase view =(ViewBase)o;
                // handle null selection
                if (selection == null)
                    selection = "";
                // handle null or provided projection
                String columns = null;
                if (projection != null)
                    columns = Util.toCommaDelimitedString(projection);

                cursor = view.fetch(context, false, columns, selection, 
                						orderByClause, limit, selectionArgs);
                
            } else
                Log.e(TAG, "Cannot fetch - uri="+uri.toString()+" viewName="+viewName+
                      " selection="+selection+" projection="+Arrays.toString(projection));

        } catch (IllegalAccessException e) {
            Log.e(TAG,"Class View ("+viewName+") IllegalAccessException from Uri="+uri);
            e.printStackTrace();
        } catch (InstantiationException e) {
            Log.e(TAG,"Class View ("+viewName+") InstanciationException from Uri="+uri);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(TAG,"Class View ("+viewName+") not found from Uri="+uri);
            e.printStackTrace();
        }
        // cannot close the database otherwise an error occurs - Invalid Statement in fillWindow()
        // cannot close the adapter or else the (external) querying application receives no data.
        return cursor;
    }


    /** fetches a join instance using the supplied where clause and order by clause.
     * Remember to close the cursor when finished.
     *
     * @param context - context
     * @param Uri - uri from query request
     * @param projection - column names - or null for all column names
     * @param selection - where clause - excluding "where"
     * @param orderByClause - order by clause - excluding "order by"
     * @param limit - max number of records to return or 0 if no limit
     * @return - cursor of results read from the database
     */
    public Cursor fetchFromTable(final Context context, final Uri uri,
            final String[] projection,
            final String selection, final String[] selectionArgs,
            final String orderByClause, int limit) {

        Cursor cursor = null;
        String tableName = uri.getLastPathSegment();

        try {
            String fullyQualifiedClassName = 
            		TableBase.class.getPackage().getName()+"."+tableName+"Table";
            Object o = Class.forName(fullyQualifiedClassName).newInstance();

            if (o instanceof TableBase) {
                TableBase table = (TableBase)o;        
                // cannot close Db or else an error occurs - Invalid Statement in fillWindow()
                cursor = table.fetchWhere(context, false, projection,
                                          selection, selectionArgs, orderByClause, limit);
            } else
                Log.e(TAG, "Cannot fetch - uri="+uri.toString()+" tableName="+tableName+
                		" selection="+selection+" selectionArgs="
                			+Arrays.toString(selectionArgs));

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG,"IllegalAccessException ("+tableName+") not found from Uri="+uri);
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.e(TAG,"InstantiationException ("+tableName+") not found from Uri="+uri);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"Class Table ("+tableName+") not found from Uri="+uri);
        }

        return cursor;
    }


    // see superclass for JavaDoc
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        return updateVirtual(uri, values, selection, selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        return deleteVirtual(uri, selection, selectionArgs);
    }


    /** this is pretty much a standard delete method for a content provider. The only
     * difference is that we leverage the table name within the uri param to
     * determine the class name which implements the table to insert into the SQL table.
     *
     * @param <T> defines type of table of type extends TableBase
     * @param uri - standard uri
     * @param selection - standard where clause without the "where".
     * @param selectionArgs - arguments that apply to the
     * @return - number of records deleted or 0.
     */
    private synchronized int deleteVirtual (final Uri uri,
            String selection, final String[] selectionArgs) {

        int result = 0;
        String[] tableNameAndSelection = getTableNameAndSelection(uri, selection);
        String tableName = tableNameAndSelection[0];
        selection = tableNameAndSelection[1];

        try {
            String fullyQualifiedClassName = 
            		TableBase.class.getPackage().getName()+"."+tableName+"Table";
            Object o = Class.forName(fullyQualifiedClassName).newInstance();

            if (o instanceof TableBase) {
                TableBase table = (TableBase)o;
                result = table.massDelete(getContext(), selection);
                if (LOG_DEBUG) Log.d(TAG, "rows deleted = "+result+", uri="+uri+", " +
                		", sel="+selection+", args="+selectionArgs);
            } else {
            	Log.e(TAG, "Cannot delete - uri="+uri.toString()+
            				" selection="+selection+" selectionArgs="
            						+Arrays.toString(selectionArgs));
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // table name didn't match any table
            Log.e(TAG, "Class not found "+tableName+"Table"+"  uri="+uri.toString()+
            			" selection="+selection+" selectionArgs="
            				+Arrays.toString(selectionArgs));
        }
        notifyChange(uri);
        return result;
    }


    // see superclass for JavaDoc
    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	Uri result = null;
        int match = sUriMatcher.match(uri);

        switch (match) {        
	    	case UriMatch.RULE_CLONE_VIEW:
	    		if(values != null) {
		    		long oldRuleId = values.getAsLong(RuleTable.Columns._ID);
		    		String newRuleKey = values.getAsString(RuleTable.Columns.KEY);
		    		String newRuleName = values.getAsString(RuleTable.Columns.NAME);	
		    		boolean isRuleCopy = values.getAsBoolean(IS_CLONE_FOR_COPY_RULE);
		    		
		    		if(oldRuleId > 0 && newRuleKey != null && newRuleName != null) {	    		
			    		Rule rule = 
			    			RulePersistence.fetchFullRule(this.getContext(), oldRuleId);   			
			    		
			    		if(rule != null) {
				    		rule.resetPersistentFields(newRuleKey, newRuleName, 
				    										oldRuleId, isRuleCopy);					
							long newRuleId = 
									new RulePersistence().insert(this.getContext(), rule);					
			                result = Uri.withAppendedPath(uri, newRuleId+"");    	
			    		} else
			    			Log.e(TAG, "fetchFullRule returned null for rule ID "+oldRuleId);
		    		} else {
		    			Log.e(TAG, "Invalid input params when trying to clone a rule" +
		    					" - Passed Params are"
		    					+" oldRuleId = "+oldRuleId
		    					+" newRuleKey = "
		    						+(newRuleKey == null ? "null" : newRuleKey)
		    					+" newRuleName = "
		    						+(newRuleName == null ? "null" : newRuleName));
		    		}
	    		} else
	    			Log.e(TAG, "values passed into the insert is null");
	    		break;
	    	default:
	    		result = insertVirtual(uri, values);
	    		break;
        }    		
        return result;
    }


    /** this is pretty much a standard insert method for a content provider. The only
     * difference is that we leverage the table name within the uri param to
     * determine the class name which implements the table to insert into the SQL table.
     *
     * The other difference is that we check to see if the caller requested the reuse
     * of an already inserted record. See that method below for more information.
     *
     * @param <T> defines type of table of type extends TableBase
     * @return - key value if record found or -1 if not.
     * @param uri - standard uri
     * @param values - standard insert values Collection.
     * @return - the input uri appending the key value inserted (or reused).
     */
    private synchronized Uri insertVirtual(Uri uri, ContentValues values) {

        Uri result = null;
        String tableName = uri.getLastPathSegment();

        try {
            String fullyQualifiedClassName = 
            			TableBase.class.getPackage().getName()+"."+tableName+"Table";
            Object o = Class.forName(fullyQualifiedClassName).newInstance();
            TableBase table = null;
            if (o instanceof TableBase) {
                table = (TableBase)o;

                long key = checkReuseDuplicates(table, values);
                if (key < MIN_VALID_KEY)
                    key = table.insert(getContext(), values);
                result = Uri.withAppendedPath(uri, key+"");
            } 
            else {
	            Log.e(TAG, "Cannot insert - uri="+uri.toString()+" class="
	            		+fullyQualifiedClassName+" values="+values.toString());
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // table name didn't match any table.
            Log.e(TAG,"Class Table ("+tableName+"Table"+") not found from Uri="+uri);
        }

        notifyChange(uri);
        return result;
    }

    /** this is pretty much a standard update method for a content provider. The only
     * difference is that we leverage the table name within the uri param to
     * determine the class name which implements the table to update into the SQL table.
     *
     *
     * @param <T> defines type of table of type extends TableBase
     * @param uri - standard uri
     * @param values - standard insert values Collection.
     * @param selection - standard where clause without the "where".
     * @param selectionArgs - arguments that apply to the
     * @return - the number of rows updated.
     */
    private synchronized int updateVirtual(Uri uri,
            final ContentValues values, String selection, final String[] selectionArgs) {

        int count = 0;
        String[] tableNameAndSelection = getTableNameAndSelection(uri, selection);
        String tableName = tableNameAndSelection[0];
        selection = tableNameAndSelection[1];

        try {
            String fullyQualifiedClassName = 
            			TableBase.class.getPackage().getName()+"."+tableName+"Table";
            Object o = Class.forName(fullyQualifiedClassName).newInstance();
            TableBase table = null;
            if (o instanceof TableBase) {
                table = (TableBase)o;
                count = table.update(getContext(), values, selection, selectionArgs);
                if (LOG_DEBUG) Log.d(TAG, "rows updated = "+count+", uri="+uri+", " +
                		"values="+values+", sel="+selection+", args="+selectionArgs);
            } else {
	            Log.e(TAG, "Cannot update - uri="+uri.toString()+" class="
	            		+fullyQualifiedClassName+" values="+values.toString());
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // table name didn't match any table.
            Log.e(TAG,"Class Table ("+tableName+"Table"+") not found from Uri="+uri);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        notifyChange(uri);
        return count;
    }


    /** this method centralizes sorting out the selection clause and table name
     * provided in the query. That is, the selection clause must be set if a numeric
     * value was provided as the last segment, like this:
     * <pre><code>
     *      uri = .../tablename/_id_value  (i.e. /Rule/21  .. to signify _id record 21)
     *           therefore, the raw query/delete selection clause must be augmented 
     *           like: "_id = 21")
     *
     *      otherwise the uri will look like this:
     *
     *      uri = .../tablename  ... to signify that the selection clause indicates the 
     *      	 narrowing, therefore no augmentation of the selection clause is required.
     * </pre></code>
     *
     * @param uri - uri input from Provider call
     * @param selection - selection clause input from Provider call
     * @return String[0] = table name, String[1] = selection ready for query, delete, etc
     */
    private static String[] getTableNameAndSelection(final Uri uri, final String selection) {

        String[] result = new String[2];

        String tableName = uri.getLastPathSegment();
        String selectionKey = getSelectionIfNumeric(uri);
        if (selectionKey != null) {
            // found a numeric value as the last segment, set table name to last segment - 1
            List<String> segments = uri.getPathSegments();
            tableName = segments.get(segments.size()-2);
            result[1] = selectionKey;
        } else
            result[1] = selection;

        result[0] = tableName;
        return result;
    }


    /** if the last segment of the URI contains a numeric value, it is an _id value
     * for a record to fetch, delete, whatever.
     *
     * @param uri - uri from provider function
     * @return null if last segment isn't numeric, else returns "where" or selection" clause
     * to append to the user's selection parm passed in.
     */
    private static String getSelectionIfNumeric(final Uri uri) {

        String result = null;
        String _id = uri.getLastPathSegment();
        try {
            long key = (Long.parseLong(_id));
            result = TableBase.Columns._ID+EQUALS+key;
            if (LOG_DEBUG) Log.d(TAG," found key="+key+" in "+_id);

        } catch (NumberFormatException e) {
            // ignore, means table name ends uri, which is normal, just return null.
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /** We couldn't seem to find a way to handle this condition as part of the
     * BulkInsert logic (handling inserts that violate a unique-index insertion). Therefore,
     * we created this logic to handle that condition. This condition applies to the Rule
     * table where it is used as both a Rule and owner table. That is, we don't want to 
     * insert duplicate records (containing the same distilled phone number).
     *
     * Therefore, we apply this check to see if the caller requested to re-use an existing 
     * record.
     * If so, the caller inserted a value which will contain a selection (where clause)
     * which will be used to locate the duplicate record. This is mainly designed for the
     * Friend table to ensure we don't insert duplicate owner or friend records. If
     * the request is found in the values, it will be stripped.
     *
     *
     * @param <T> defines type of table of type extends TableBase
     * @param table - table of type extends TableBase
     * @param values - standard insert values Collection.
     * @return - key value if record found or -1 if not.
     */
    private <T extends TableBase> long checkReuseDuplicates(T table, ContentValues values) {

        long result = -1;
        if(values == null) throw new IllegalArgumentException("Values cannot be null for a healthy db");
        
        if (values.containsKey(TableBase.Columns.REUSE_DUPLICATES)) {

            String selection = (String)values.get(TableBase.Columns.REUSE_DUPLICATES);
            values.remove(TableBase.Columns.REUSE_DUPLICATES);
            Cursor c = table.fetchWhere(getContext(), null, selection, null, null, 0);
            if (c != null) {
            	try {
	                if (c.moveToFirst()) {
	                    int ix = c.getColumnIndexOrThrow(TableBase.Columns._ID);
	                    result = c.getLong(ix);
	                }
            	} catch (Exception e){
            		e.printStackTrace();
            	} finally {
            		if (!c.isClosed())
            			c.close(); 
            	}
            }
        }
        return result;
    }



    private interface UriMatch {

        static final int RULE_VIEW 					= 10;
        static final int RULE_VIEW_ID 				= 11;
        static final int RULE_ACTION_VIEW 			= 30;
        static final int RULE_ACTION_VIEW_ID 		= 31;
        static final int RULE_CONDITION_VIEW 		= 40;
        static final int RULE_CONDITION_VIEW_ID 	= 41;
        static final int RULE_VIEW_CNT 				= 60;
        static final int RULE_VIEW_CNT_ID 			= 61;
        static final int TRIGGER_STATE_CNT_VIEW     = 62;
        static final int ACTIVE_SETTINGS_VIEW		= 63;
        static final int DISTINCT_CONDITION_VIEW 	= 64;
        static final int DISTINCT_CONDITION_VIEW_ID = 65;

        // These are Virtual Views (Do not exist in the DB).
        static final int ACT_LOC_COND_VIEW_CNT		= 70;
        static final int VISIBLE_ENA_AUTO_RULES_VIEW_CNT = 71;
        static final int RULE_CLONE_VIEW			= 72;
        static final int ADOPTED_SAMPLE_LIST_VIEW 	= 73;


        static final int RULE						= 140;
        static final int RULE_ID					= 141;
        static final int ACTION 					= 150;
        static final int ACTION_ID 					= 151;
        static final int CONDITION 					= 160;
        static final int CONDITION_ID 				= 161;
        static final int BUCKET_TABLE 				= 180;
        static final int BUCKET_TABLE_ID 			= 181;
        static final int ICON 					= 190;
        static final int ICON_ID 					= 191;

    }



    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // views - rules
        sUriMatcher.addURI(AUTHORITY, RuleView.VIEW_NAME, 					UriMatch.RULE_VIEW);
        sUriMatcher.addURI(AUTHORITY, RuleView.VIEW_NAME+"/#", 				UriMatch.RULE_VIEW_ID);
        sUriMatcher.addURI(AUTHORITY, RuleActionView.VIEW_NAME, 			UriMatch.RULE_ACTION_VIEW);
        sUriMatcher.addURI(AUTHORITY, RuleActionView.VIEW_NAME+"/#", 		UriMatch.RULE_ACTION_VIEW_ID);
        sUriMatcher.addURI(AUTHORITY, RuleConditionView.VIEW_NAME, 			UriMatch.RULE_CONDITION_VIEW);
        sUriMatcher.addURI(AUTHORITY, RuleConditionView.VIEW_NAME+"/#", 	UriMatch.RULE_CONDITION_VIEW_ID);
        sUriMatcher.addURI(AUTHORITY, RuleViewCnt.VIEW_NAME, 				UriMatch.RULE_VIEW_CNT);
        sUriMatcher.addURI(AUTHORITY, RuleViewCnt.VIEW_NAME+"/#", 			UriMatch.RULE_VIEW_CNT_ID);
        sUriMatcher.addURI(AUTHORITY, TriggerStateCountView.VIEW_NAME, 		UriMatch.TRIGGER_STATE_CNT_VIEW);
        sUriMatcher.addURI(AUTHORITY, ActiveSettingsView.VIEW_NAME, 		UriMatch.ACTIVE_SETTINGS_VIEW);
        sUriMatcher.addURI(AUTHORITY, DistinctConditionView.VIEW_NAME, 		UriMatch.DISTINCT_CONDITION_VIEW);
        sUriMatcher.addURI(AUTHORITY, DistinctConditionView.VIEW_NAME+"/#", UriMatch.DISTINCT_CONDITION_VIEW_ID);

        // virtual views
        sUriMatcher.addURI(AUTHORITY, ActiveLocCntView.VIEW_NAME, 				UriMatch.ACT_LOC_COND_VIEW_CNT);
        sUriMatcher.addURI(AUTHORITY, VisibleEnaAutoRulesCntView.VIEW_NAME, 	UriMatch.VISIBLE_ENA_AUTO_RULES_VIEW_CNT);
        sUriMatcher.addURI(AUTHORITY, RuleCloneView.VIEW_NAME, 					UriMatch.RULE_CLONE_VIEW);
        sUriMatcher.addURI(AUTHORITY, AdoptedSampleListView.VIEW_NAME, 			UriMatch.ADOPTED_SAMPLE_LIST_VIEW);

        // tables
        sUriMatcher.addURI(AUTHORITY, RuleTable.TABLE_NAME, 					UriMatch.RULE);
        sUriMatcher.addURI(AUTHORITY, RuleTable.TABLE_NAME+"/#", 				UriMatch.RULE_ID);
        sUriMatcher.addURI(AUTHORITY, ActionTable.TABLE_NAME, 					UriMatch.ACTION);
        sUriMatcher.addURI(AUTHORITY, ActionTable.TABLE_NAME+"/#", 				UriMatch.ACTION_ID);
        sUriMatcher.addURI(AUTHORITY, ConditionTable.TABLE_NAME, 				UriMatch.CONDITION);
        sUriMatcher.addURI(AUTHORITY, ConditionTable.TABLE_NAME+"/#", 			UriMatch.CONDITION_ID);


    }


    /** to notify the changes to the clients that are using the database.
     *
     * @param uri - table uri
     */
    protected void notifyChange(Uri uri) {
        // Note that semantics are changed: notification is for CONTENT_URI, not the specific
        // Uri that was modified.
        getContext().getContentResolver().notifyChange(uri, null,
                true /* syncToNetwork */);
    }

}
