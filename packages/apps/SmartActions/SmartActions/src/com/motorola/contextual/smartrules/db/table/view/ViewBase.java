/*
 * @(#)ViewBase.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db.table.view;


import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.DbUtil;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.table.TableBase;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/** This class is a base class for any view in the system.
 * 
 *<code><pre>
 * CLASS:
 *	Implements DbSyntax to assist in creating queries quickly with no string concat issues.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, fetch, convert to Join.
 *  
 * COLABORATORS:
 * 	JoinBase - encapsulates a joined row of a view.
 *
 * USAGE:
 * 	See each method.
 * 
 *</pre></code>
 */
public abstract class ViewBase implements DbSyntax, Constants {

	private static final String TAG = "ViewBase";

	
	/** forces subclass to implement this method to provide the name of the view 
	 * associated with the subclass. */
	public abstract String getViewName();
		
    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of categories.
     */
	public String getContentType() {
	    return CONTENT_TYPE_PREFIX+getViewName();
	}
	
    /**
     * The MIME type of a {@link #CONTENT_URI} type of a single item.
     */
	public String getContentItemType() {
		
	    return CONTENT_ITEM_TYPE_PREFIX+getViewName();
	}	

	
	/** fetches a set of rows where the where clause is met.
	 * 
	 * @param context - context
	 * @param tuple - subclass tuple
	 * @return -1 if not added or primary key (_id) of added record
	 */
	public Cursor fetchWhere(Context context, String whereClause, String orderByClause) {

		Cursor result = null;
		SQLiteManager db = SQLiteManager.openForRead(context, TAG+".1.5");
		try {			
			return fetchWhere(db, getViewName(), whereClause, orderByClause);
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
			
		} finally {
			if (db != null)
				db.close(TAG+".1.5");
		}	
		return result;		
	}
	
	
	/** fetches records meeting where clause. Caller must close the cursor returned.
	 * 
	 * @param db - SQLiteManager 
	 * @param viewName - name of the view
	 * @param whereClause - where clause to be used for fetch
	 * @return - cursor, which caller must close - confirmed all refs.
	 */
	private static Cursor fetchWhere(
					SQLiteManager db, 
					String viewName, 
					String whereClause, 
					String orderByClause) {

		String[] selectionArgs = null;
		String sql = SELECT_ALL+FROM+viewName;
		if (whereClause != null) {
			// prepend "where" in where clause if necessary
			if (whereClause.indexOf(WHERE) < 0)
				whereClause = WHERE+whereClause;
			// append where clause to SQL
			if (whereClause.length() > 0)
				sql = sql.concat(whereClause);
		}
		sql = sql.concat(ORDER_BY+(orderByClause == null?TableBase.Columns._ID:orderByClause) );
		
		if (LOG_DEBUG) Log.d(TAG, "fetchWhere sql="+ sql);
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		if(cursor != null)
			cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
								  // BEING RETURNED IN THE CURSOR. 		
		if (LOG_DEBUG) Log.d(TAG, "fetchWhere sql="+ sql+",  getCount()="+(cursor!=null?cursor.getCount():"null"));
		return cursor;
	}


	/** fetches records meeting where clause. Caller must close the cursor returned.
	 * 
	 * @param context - Context 
	 * @param selectClause - select clause
	 * @param whereClause - where clause to be used for fetch
	 * @param selectionArgs - arguments for the selection
	 * @param orderByClause - Order By clause
	 * @param closeDb - if true, closes the database reference used to extract the query
	 * @return - cursor, which caller must close - confirmed all refs.
	 */
	public Cursor fetchWhere(final Context context, 
		      					    String selectClause, 
		      			      final String whereClause, 
		      			      final String orderByClause,
		      			      final String[] selectionArgs,
		      			            boolean closeDb) {

		Cursor result = null;
		SQLiteManager db = SQLiteManager.openForRead(context, TAG+".2.1");

		try {			
			result = fetchWhere(db, selectClause, whereClause, selectionArgs, orderByClause);
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
			
		} finally {
			if (db != null && closeDb)
				db.close(TAG+".2.1");
		}	
		return result;
	}
	
	/** fetches records meeting where clause. Caller must close the cursor returned.
	 * 
	 * @param db - SQLiteManager reference, cannot be null or this will throw NullPointerException.
	 * @param selectClause - select clause which must be flattened with column names separated by commas 
	 * (except the last column name). It can be prefixed with the SELECT statement or not, it is optional. 
	 * Actually, the selectClause itself may be null or "", in which case all columns are returned.
	 * @param whereClause - where clause to be used for fetch, it may be prefixed with WHERE, but is optional.
	 * If the where clause is not supplied, then all rows are returned.
	 * @param selectionArgs - arguments for the selection, which may be null.
	 * @return - cursor, which caller must close - confirmed all refs.
	 */
	private Cursor fetchWhere(SQLiteManager db, 
						      String selectClause, 
						      String whereClause, 
						final String[] selectionArgs,
							  String orderByClause) {

		//TODO: once tested, this should replace the other fetchWhere private method above
		StringBuilder sql = new StringBuilder();
		if (selectClause == null || selectClause.trim().length() < 1)
			selectClause = SELECT_ALL;
		if (selectClause.indexOf(SELECT)<0)
			selectClause = SELECT+selectClause;
		sql.append(selectClause+FROM+getViewName());
		if (whereClause != null && whereClause.trim().length() > 0) {
			// prepend "where" in where clause if necessary
			if (whereClause.indexOf(WHERE) < 0)
				whereClause = WHERE+whereClause;
			// append where clause to SQL
			sql.append(whereClause);
		}
		if (orderByClause != null) {
			if (orderByClause.indexOf(ORDER) < 0)
				orderByClause = ORDER_BY+orderByClause;
			sql.append(orderByClause);
		}
		if (LOG_DEBUG) Log.d(TAG, "fetchWhere sql="+ sql);
		Cursor cursor = db.rawQuery(sql.toString(), selectionArgs);
		if(cursor != null)
			cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
								  // BEING RETURNED IN THE CURSOR. 		
		if (LOG_DEBUG) Log.d(TAG, "fetchWhere sql="+ sql+",  getCount()="+(cursor!=null?cursor.getCount():"null"));
		return cursor;
	}

    
	/** Returns a cursor of all columns using the specified sql parms.
	 *  
	 * @see fetch(String, String, String, String, int);
	 * 
	 * @param db - instance of SQLiteManager
	 * @param whereClause - where clause
	 * @param orderByClause - orderBy clause
	 * @param limit - limit to x result tuples
	 * @param selectionArgs - arguments that apply to the query	 
	 * @return - cursor fetched, which caller must close - confirmed all refs.
	 */
	public Cursor fetch(SQLiteManager db, String whereClause, 
			String orderByClause, int limit, final String[] selectionArgs) {
		
		return fetch(db, ALL, whereClause, orderByClause, limit, selectionArgs);
	}

	
	/** Be cautious when using this method in that it can return a partial list of field names.
	 * If this cursor is passed to a method expecting a full set of field names, it can break
	 * the downstream function.
	 * 
	 * @param context - context
	 * @param close - if true, db will be closed after query, else close is deferred by using CursorFactory
	 * @param colNames - comma separated list of column names
	 * @param whereClause - where clause
	 * @param orderByClause - orderBy clause
	 * @param limit - limit to x result tuples
	 * @param selectionArgs - arguments that apply to the query	 
	 * @return - cursor fetched, which caller must close - confirmed all.
	 */
	public Cursor fetch(final 	Context context, 
						final   boolean close,
						final 	String colNames, 
						final 	String whereClause, 
						final 	String orderByClause, 
								int limit,
						final   String[] selectionArgs) {
		Cursor result = null;
        SQLiteManager db = SQLiteManager.openForRead(context, !close, TAG+".6");
        if (db != null) {
        	try {
				result = fetch(db, colNames, whereClause, orderByClause, limit, selectionArgs);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
			if (close) db.close("vb.fetch");
        }
        return result;
	}

	
	/** Be cautious when using this method in that it can return a partial list of field names.
	 * If this cursor is passed to a method expecting a full set of field names, it can break
	 * the downstream function.
	 * 
	 * @param db - instance of SQLiteManager, open for read
	 * @param colNames - comma separated list of column names
	 * @param whereClause - where clause
	 * @param orderByClause - orderBy clause
	 * @param limit - limit to x result tuples
	 * @param selectionArgs - arguments that apply to the query	 
	 * @return - cursor fetched, which caller must close - confirmed all.
	 */
	public Cursor fetch(final 	SQLiteManager db, 
								String colNames, 
								String whereClause, 
								String orderByClause, 
								int limit,
								String[] selectionArgs) {

		// prepend where clause
		if (whereClause.trim().length()>0 && whereClause.indexOf(WHERE) < 0)
			whereClause = WHERE+whereClause;
		// use all if col names not specified
		if (colNames == null || colNames.length() < 1)
			colNames = ALL;
		// change orderByClause to empty string to avoid crash.
		if (orderByClause == null)
			orderByClause = "";
		else if (orderByClause.trim().length()>0 && orderByClause.indexOf(ORDER_BY.trim()) <0)
			orderByClause = ORDER_BY+orderByClause;
		String limitStr = "";
		if (limit > 0)
			limitStr = LIMIT+limit;
		
		String sql = (SELECT+colNames+FROM+getViewName()).concat(whereClause)
			   			.concat(orderByClause).concat(limitStr);
		if (LOG_DEBUG) Log.d(TAG, sql);
		
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		if(cursor != null)
			cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
								  // BEING RETURNED VIA THE PROVIDER. 
		if (LOG_DEBUG) {
			if (cursor != null)
				Log.d(TAG, "records fetched size="+cursor.getCount()+" using sql= "+sql);
		}
		return cursor;
	}
	
			
// Commented out by Craig - not used.	
//	/** inserts a series of records from the join. This method simply opens the 
//	 * database for the subclass, requiring only that the subclass implement 
//	 * the construction of the join instance.
//	 * 
//	 * @param context - context
//	 * @param join - tuple of the view tuple 
//	 * @return -1 if not inserted or primary key value of the relate table if inserted.
//	 */
//	public <T extends JoinBase> T insert(Context context, T join) {
//				
//		SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".3"); 
//		T result = join;
//		try {
//			result = insert(context, db, join);
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			
//		} finally {
//			if (db != null)
//				SQLiteManager.close(db,TAG+".3");
//		}
//		return result;
//	}

	/** dump entire view to Log.i
	 */
	public void dumpDebug(Context context, final String prefix) {
		
		Cursor cursor = null;
		SQLiteManager db = SQLiteManager.openForRead(context, TAG+".4"); 
		try {
			String sql = SELECT+ALL+FROM+getViewName();
			cursor = db.rawQuery(sql, null);
			if(cursor != null)
				cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
									  // BEING RETURNED IN THE CURSOR. 
			// Dump all rows
			DbUtil.dumpCursor(prefix+": "+getViewName(), cursor, null);
			
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			if (cursor != null && ! cursor.isClosed())
				cursor.close();
			if (db != null)
				db.close(TAG+".4");
		}
	}
}
