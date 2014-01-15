/*
 * @(#)TableBase.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 * ACD100 		 2009/11/20 NA				  Conversion for Endive
 *
 */
package com.motorola.contextual.smartrules.db.table;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Base64;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.DbUtil;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.util.Util;

/**This class is a base class for any table in the system.
 *
 *<code><pre>
 * CLASS:
 *  implements DbSyntax to support SQL statement assembly.
 *
 * RESPONSIBILITIES:
 * 	Insert, update, delete, fetch, convert to Tuple.
 *
 * COLABORATORS:
 * 	TupleBase - encapsulates a generic row in the table.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public abstract class TableBase implements DbSyntax, Constants {

    private static final String TAG = "TableBase";

    static final String DB_OPEN_ERROR = "Db open failed here.";

    /** gets the table Uri */
    public abstract Uri getTableUri();
    /** gets the table name */
    public abstract String getTableName();
    /** gets the foreign key row _id column name */
    public abstract String getFkColName();
    /** requires sub classes to implement toContentValues() */
    public abstract <T extends TupleBase> ContentValues toContentValues(T _tuple);
    /** requires sub classes to implement toTuple() */
    public abstract <T extends TupleBase> T toTuple(final Cursor cursor, int[] colNumbers);
    /** requires sub classes to implement getColumnNumbers */
    protected abstract int[] getColumnNumbers(Cursor cursor, String sqlRef);


    /**
     * Possible values for Validity
     * <pre><code>
     * VALID = "Valid"
     * INVALID = "Invalid"
     * INPROGRESS = "InProgress"
     * Validity applies to Rule/Action/Condition publishers. A publisher is marked as valid, 
     * if it is currently available. A publisher is marked as invalid if it is currently
     * unavailable. It is marked as InProgress, if validation is currently in progress.
     */
   public static interface Validity{
	  final String VALID = "Valid";
	  final String INVALID = "Invalid";
	  final String INPROGRESS ="InProgress";
	  final String BLACKLISTED ="Blacklisted";
	  final String UNAVAILABLE ="UnAvailable";
   }


    public static interface Columns extends BaseColumns {

        /** means to ignore duplicates during insert (don't try to insert record if one exists) */
        public static final String REUSE_DUPLICATES = "ReuseDuplicates";
    }

    /** Basic constructor */
    public TableBase() {
        super();
    }

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of categories.
     */
    public String getContentType() {
        return CONTENT_TYPE_PREFIX+getTableName();
    }

    /**
     * The MIME type of a {@link #CONTENT_URI} type of a single item.
     */
    public String getContentItemType() {

        return CONTENT_ITEM_TYPE_PREFIX+getTableName();
    }



    /** gets a where clause for fetching by row id.
     *
     * @param rowId - row (_id) to fetch
     * @return - where clause string to find a given row id (_id).
     */
    public static String getRowIdWhereClause(long rowId) {
        return WHERE+Columns._ID+" = "+rowId;
    }


    /** inserts a row into the given subclass table.
     *
     * @param context - context
     * @param values - values to insert, must not include an _id column.
     * @return - new record inserted key value
     */
    public long insert(Context context, ContentValues values) {

        long result = -1;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".0");

        try {
        	synchronized (db) {
        		result = db.insertOrThrow(this.getTableName(), values);
        	}

        } catch (Exception e) {
            Log.e(TAG, ".insert failed, values="+values.toString());
            e.printStackTrace();

        } finally {
            if(db != null)
                db.close(TAG+".0");
        }
        return result;
    }


    /** inserts a tuple into the given subclass table.
     *
     * @param <T> - tuple type
     * @param context - context
     * @param tuple - subclass tuple
     * @return -1 if not added or primary key (_id) of added record
     */
    public <T extends TupleBase> long insert(Context context, T tuple) {

        long result = 0;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".1");
        try {
        	synchronized (db) {
        		result = insert(db, tuple);
        	}

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();

        } finally {
            if(db != null)
            	db.close(TAG+".1");
        }
        return result;
    }


    /** inserts a table record.
     *
     * Command line example:
     * 		insert into Friend(Name, Phone) values("Fred", "8472221111");
     *
     * @param db - instance of SQLiteManager.
     * @param tuple - tuple to add to the db.
     *
     * @return -1 if not added or primary key (_id) of added record
     */
    public <T extends TupleBase> long insert(SQLiteManager db, T tuple) {

        // insert one row, sync the db.
    	long key = 0;
    	synchronized (db) {
    		key = db.insertOrThrow(this.getTableName(), toContentValues(tuple));
    	}
        tuple.set_id(key);
        return key;
    }


    /** fetches all the records in the table.
     *
     * @param db - database instance
     * @param tableName - table name
     * @param orderById - if true, will order by row _id, else random order
     *
     * @return - cursor, CALLER MUST CLOSE - confirmed all.
     */
    public static Cursor fetchAll(			SQLiteManager db,
                                            String tableName,
                                            boolean orderById) {

        String[] selectionArgs = null;
        String sql = SELECT_ALL+FROM+tableName;
        if (orderById)
            sql = sql.concat(ORDER_BY+Columns._ID+" desc");
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        if(cursor != null)
            cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
        // BEING RETURNED IN THE CURSOR.
        if (LOG_DEBUG) {
			if (cursor != null)
				Log.d(TAG, "fetchAll sql="+ sql+",  getCount()="+cursor.getCount());
		}
        return cursor;
    }


    /** returns array of long values based on a column name provided and a where clause.
     *
     * @param context - context
     * @param whereClause - selection
     * @param longColumnName - column name of a column in the database which is of type INT
     * @return - array of long values found or an empty array if none found.
     */
    public long[] fetchLongArray(Context context, final String whereClause, final String longColumnName) {


        long[] result = null;
        SQLiteManager db = SQLiteManager.openForRead(context, TAG+".67");
        Cursor cursor = fetchWhere(db, longColumnName, whereClause, null, null, 0);
        if (cursor != null)
            try {
                int col = cursor.getColumnIndex(longColumnName);
                int i = 0;
                if (cursor.moveToFirst()) {
                    result = new long[cursor.getCount()];
                    do {
                        result[i++] = cursor.getLong(col);
                    } while (cursor.moveToNext());
                } else
                    result = new long[0];
            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                cursor.close();
                if(db != null)
                    db.close(TAG+".67");
            }
        return result;
    }


    /** Fetches a series of records matching the whereClause.
     *
     * @param context - context
     * @param projection - column array of column names
     * @param whereClause - The whereClause should not include the word "where".
     * @param args - arguments for where clause
     * @param orderByClause - null or an order by clause
     * @param limit - records to limit result set or 0 if no limit
     * @return - cursor of records found.
     */
    public Cursor fetchWhere(Context context,
                             String[] projection, String whereClause, String[] args, String orderByClause, int limit) {

        return fetchWhere(context, true, projection, whereClause, args, orderByClause, limit);
    }

    /** Fetches a series of records using a raw sql statement.
     *
     * @param context - context
     * @param close - true if you want to close the DB (the cursor is not closed), else false.
     * @param rawSql - raw sql statement
     * @param args - arguments for where clause
     * @return - cursor of records found.
     */
    public static Cursor rawQuery(Context context, boolean close, final String rawSql,
                                  final String[] args) {
        Cursor result = null;
        SQLiteManager db = SQLiteManager.openForRead(context, !close, TAG+".65");
        try {
            result = db.rawQuery(rawSql, args);
            if (result != null)
                if (LOG_DEBUG) Log.d(TAG, ".rawQuery where 65 - count="+result.getCount());

        } catch (Exception e) {
            Log.e(TAG, "rawQuery failed on parms="+Arrays.toString(args)+", sql="+rawSql);
            e.printStackTrace();

        } finally {
            if (LOG_DEBUG) Log.d(TAG, ".rawQuery where 65 - close="+close);
            if(db != null && close)
                db.close(TAG+".65");
        }
        return result;

    }

    /** Fetches a series of records matching the whereClause.
     *
     * @param context - context
     * @param close - true if you want to close the DB, else false to defer (not close the db).
     * @param projection - column array of column names
     * @param whereClause - The whereClause should not include the word "where".
     * @param args - arguments for where clause
     * @param orderByClause - null or an order by clause
     * @param limit - records to limit result set or 0 if no limit
     * @return - cursor of records found.
     */
    public Cursor fetchWhere(final Context context, boolean close,
                             final String[] projection, final String whereClause, final String[] args, 
                             final String orderByClause, int limit) {

        Cursor result = null;
        SQLiteManager db = SQLiteManager.openForRead(context, !close, TAG+".6");
        try {
            result = fetchWhere(db, Util.toCommaDelimitedString(projection), 
            		whereClause, args, orderByClause, limit);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (LOG_DEBUG) Log.d(TAG, ".fetch where 6 - close="+close);
            if(db != null)
            	if (close)
            		db.close(TAG+".6");
        }
        return result;
    }


    /** fetches all records meeting the whereClause condition.
     *
     * @param db- database instance
     * @param commaDelimColumns - comma delimited list of column names
     * @param whereClause - where clause
     * @param selectionArgs - selection arguments (replacing "?" in selection with arguments, ordered)
     * This parameter can help speed query results as SQLite can cache the query results faster.
     * @param orderByClause - order by clause
     * @param limit - limit the number of records to this number
     *
     * @return - cursor, CALLER MUST CLOSE - confirmed all
     */
    protected Cursor fetchWhere(	final	SQLiteManager db,
                                    final 	String commaDelimColumns,
                                    String 	whereClause,
                                    final   String[] selectionArgs,
                                    final 	String orderByClause,
                                    int 	limit) {

        String cols = (commaDelimColumns == null || commaDelimColumns.length()<1 ? ALL: commaDelimColumns);
        String sql = SELECT+cols+FROM+getTableName();

        // prefix where clause with "WHERE" if missing.
        if (whereClause != null) {
            if (whereClause.trim().length() > 3 && whereClause.indexOf(WHERE) < 0)
                whereClause = WHERE+whereClause;

            // add whereClause to sql
            sql = sql.concat(whereClause);
        }

        // concat ORDER BY clause, if none, adds order by primary key
        if (orderByClause != null)
            if (orderByClause.length() > 0)
                sql = sql.concat((orderByClause.indexOf(ORDER_BY) > -1 ?
                                  orderByClause:ORDER_BY+orderByClause) );
            else
                sql = sql.concat(ORDER_BY+Columns._ID);

        if (limit > 0)
            sql = sql.concat(LIMIT+limit);

        Cursor cursor = db.rawQuery(sql, selectionArgs);
        if(cursor != null)
            cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
        // BEING RETURNED IN THE CURSOR.
        if (LOG_DEBUG) {
			if (cursor != null)
				Log.d(TAG, "fetchWhere sql="+ sql+",  getCount()="+cursor.getCount());
		}
        return cursor;
    }

    /** fetches list of records using the whereClause.
     *
     * @param context - context
     * @param whereClause - whereClause for the query
     *
     * @return - list of Tuple records found or null
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> ArrayList<T> fetchList(Context context, String whereClause) {
    	ArrayList<T> tupleList = null;
    	SQLiteManager db = SQLiteManager.openForRead(context, TAG+".2");
    	Cursor cursor = null;
    	try {
    		cursor =  this.fetchWhere(db, null, whereClause, null, null, 0);
    		if(cursor == null) {
    			Log.e(TAG, "Cursor fetched is null for "+whereClause);
    		}
    		else {
    			if(cursor.moveToFirst()) {
    				tupleList = new ArrayList<T>(cursor.getCount());
    				do {
    					tupleList.add((T) this.toTuple(cursor));
    				} while (cursor.moveToNext());
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		if(cursor != null && ! cursor.isClosed())
    			cursor.close();
    	}
    	return tupleList;
    }

    /** fetches 1 record using the where clause.
    *
    * @param <T> - return type
    * @param context - context
    * @param whereClause - where clause
    *
    * @return - record found or null
    */
    @SuppressWarnings({ "unchecked" })
	public <T extends TupleBase> T fetch1(Context context, String whereClause) {
    	T tuple = null;
    	Cursor cursor = null;
    	SQLiteManager db = SQLiteManager.openForRead(context, TAG+".2");
        try {
        	cursor =  this.fetchWhere(db, null, whereClause, null, null, 0);
        	if(cursor == null) {
    			Log.e(TAG, "Cursor fetched is null for "+whereClause);
    		}
    		else {
    			if(cursor.moveToFirst()) {
    				tuple = (T) toTuple(cursor);
    			}
    		}
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(db != null)
                db.close(TAG+".2");
            if(cursor != null && ! cursor.isClosed())
    			cursor.close();
        }
    	return tuple;
    }
    
    /** fetches 1 record using the primary key.
     *
     * @param <T> - return type
     * @param context - context
     * @param key - primary key value
     *
     * @return - record found or null
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> T fetch1(Context context, long key) {

        T tuple = null;
        SQLiteManager db = SQLiteManager.openForRead(context, TAG+".2");
        try {
            tuple = (T) fetch1(context, db, Columns._ID, key+"");

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(db != null)
                db.close(TAG+".2");
        }
        return tuple;
    }


    /** fetches 1 record using the primary key.
     *
     * @param <T> - return type
     * @param db - database instance
     * @param keyValue - primary key value
     *
     * @return - record found or null
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> T fetch1(Context context, SQLiteManager db, long keyValue) {

        return (T) fetch1(context, db, Columns._ID, keyValue+"");
    }


    /** fetches 1 record in the table using a supplied key column and match value.
     *
     * @param context - context
     * @param db - database instance
     * @param keyColumn - string containing column name to match with matchValue.
     * @param matchValue - match value to match within the key column
     * @return - null if not found or instance of the class for which this tuple is based upon.
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> T fetch1(Context context,
    						SQLiteManager db, final String keyColumn,  String matchValue) {
    	
        Cursor cursor = null;
        T result = null;
        try {
            if (! matchValue.startsWith(Q))
                matchValue = Q+matchValue+Q;
            cursor = fetchWhere(db, null, keyColumn +EQUALS+ matchValue, null, null, 1);
            if (cursor.moveToFirst())
                result = (T)toTuple(cursor, "");

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return result;
    }

    /** updates a record based on the content values, whereClause and the whereArgs
     *
     * @param <T> - type of TupleBase
     * @param context - context
     * @param values - Content Values
     * @param whereClause - whereClause to be used
     * @param whereArgs - selection arguments
     * @return - number of records updated should be 1 if found or 0 if
     *  whereClause key not found in the database.
     * @throws InvalidDbOpenException - throws if cannot open for update
     */
    public synchronized int update(Context context,
                                   final ContentValues values,
                                   final String whereClause,
                                   final String[] whereArgs)  {
        int result = 0;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".35");
        if (db == null || !db.isOpen())
            throw new IllegalStateException(DB_OPEN_ERROR);
        else
            try {
            	synchronized (db) {
            		result = db.update(getTableName(),
                                   values,
                                   whereClause,
                                   whereArgs
                                  );
            	}
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            } finally {
                if(db != null && db.isOpen())
                    db.close(TAG+".35");
            }

        return result;
    }



    /** updates a record from the tuple primary key using values in the tuple.
     *
     * @param <T> - type of TupleBase
     * @param context - context
     * @param tuple - tuple to update in the database
     *
     * @return - instance of the
     * @throws InvalidDbOpenException - when the Db fails to open
     */
    public synchronized <T extends TupleBase> int update(Context context,
            final T tuple)  {

        int result = 0;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".3");
        if (db == null || !db.isOpen())
            throw new IllegalStateException(DB_OPEN_ERROR+" db="+(db==null?"null":" open="+db.isOpen()+
                                            " locked by me="+db.isDbLockedByCurrentThread()+" others="+db.isDbLockedByOtherThreads()));
        else
            try {
                result = update(db, tuple);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();

            } finally {
                if(db != null && db.isOpen())
                    db.close(TAG+".3");
            }
        return result;
    }


    /** update the tuple, using the tuple instance and the primary key of the tuple.
     *
     * @param db - SQLiteManager instance
     * @param tuple - Tuple to be updated
     *
     * @return - number of records updated, should be 1 if found or 0 if
     *  tuple primary key not found in the database.
     */
    public synchronized <T extends TupleBase> int update(SQLiteManager db, final T tuple) {

        String whereClause = Columns._ID + EQUALS + tuple.get_id();
        String whereArgs[] = null;

        if (LOG_DEBUG) Log.d(TAG,"isLockedbyOther?="+db.isDbLockedByOtherThreads()+" isLockedByMe?="+db.isDbLockedByOtherThreads());

        if(LOG_VERBOSE) Log.v(TAG, "DB update request on = "+tuple.toString());
        int result = 0;
        synchronized (db) {
        	result = db.update(
                         getTableName(),
                         toContentValues(tuple),
                         whereClause,
                         whereArgs
                     );
        }
        if(LOG_DEBUG) Log.d(TAG, "DB update result = "+result);
        return result;
    }


    /** Deletes 1 record in the table using the primary key.
     *
     * @param context - context
     * @param rowId - primary key (_id)
     *
     * @return - 1 if found and deleted, or 0 if not.
     */
    public int delete(Context context, long rowId) {

        int result = 0;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".4");
        try {
        	synchronized (db) {
        		result = deleteRow(db, rowId);
        	}
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(db != null && db.isOpen())
                db.close(TAG+".4");
        }
        return result;
    }


    /** Delete one tuple based on rowId (primary key)
     *
     * @param db - SQLiteManager instance
     * @param rowId id (primary key) of tuple to delete
     *
     * @return - 1 if found and deleted, or 0 if not.
     */
    public int deleteRow(SQLiteManager db, long rowId) {

        String whereClause = Columns._ID + EQUALS + rowId;
        String whereArgs[] = null;
        int deleted = 0;
        synchronized (db) {
        	deleted = db.delete(getTableName(), whereClause, whereArgs);
        }
        return deleted;
    }


    /** converts cursor to tuple.
     *
     * @param context - context
     * @param cursor - cursor to convert to tuple.
     *
     * @return tuple instance of this cursor.
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> T toTuple(	final Cursor cursor) {

        return (T) toTuple(cursor, getColumnNumbers(cursor, ""));
    }

    /** converts cursor to tuple.
     *
     * @param context - context
     * @param cursor - cursor to convert to tuple.
     * @param sqlRef - SQL reference used to join the table. If no join was done, make it null.
     *
     * @return tuple instance of this cursor.
     */
    @SuppressWarnings("unchecked")
    public <T extends TupleBase> T toTuple(	Cursor cursor,
                                            String sqlRef) {

        return (T) toTuple(cursor, getColumnNumbers(cursor, sqlRef));
    }


    /** gets the row id of the primary key either via the foreign key
     * or the standard _id key value. This is important for inheritance.
     *
     * @param cursor - result cursor.
     * @return - string value of the column name used to retrieve the key.
     */
    protected String getRowIdColName(Cursor cursor) {

        String result = null;
        try {
            // check foreign key column name, if not found, throws IllegalArgumentException
            result = getFkColName();
            cursor.getColumnIndexOrThrow(getFkColName());

        } catch (IllegalArgumentException e) {

            // use standard _id column name, which exists in every table
            result = Columns._ID;
            cursor.getColumnIndexOrThrow(Columns._ID);
        }
        return result;
    }


    /** gets the column number array for a given set of column names.
     *
     * @param cursor - cursor to fetch column numbers.
     * @param colNames - array of column names to be used to get column numbers.
     *
     * @return - integer array of column numbers
     */
    public static int[] getColumnNumbers(Cursor cursor, String[] colNames) {

        int[] result = new int[colNames.length];
        for (int i=0; i< result.length; i++) {
            result[i] = cursor.getColumnIndexOrThrow (colNames[i]);
        }
        return result;
    }


    /** delete a series of records using a where clause
     *
     * @param context - context
     * @param whereClause - standard where clause
     * @return 1 if found & deleted, else 0;
     */
    public int massDelete(Context context, final String whereClause) {

        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".5");

        int result = 0;
        if (db != null) 
        	synchronized (db) { 
	            db.beginTransaction();
	
	            try {
	                result = deleteWhere(db, whereClause);
	                db.setTransactionSuccessful();
	
	            } catch (Exception e) {
	                e.printStackTrace();
	
	            } finally {
	                db.endTransaction();
	                db.close(TAG+".5");
	            }
	        }
        return result;
    }



    /** delete a series of records using a where clause.
     *
     * @param db - database instance
     * @param whereClause - standard where clause
     * @return 1 if found & deleted, else 0;
     */
    public int deleteWhere(final SQLiteManager db, String whereClause) {

        String whereArgs[] = null;
        if (whereClause.startsWith(WHERE))
            whereClause = whereClause.replace(WHERE, "");

        int result = 0;
        synchronized (db) {
	        result =
	            db.delete(
	                getTableName(),
	                whereClause,
	                whereArgs);
        }
        return result;
    }


    /** Translates an array of keys from within one table. This is normally used for translating
     * a foreign key to a primary key or vice-versa.
     *
     * @param resultKeyColName - the column name you want as a result - translated from
     * the keyArrayColName
     * @param keyArrayColName - name of the column from which the keyArray is from.
     * @param keyArray - a list of keys from the keyArrayColName to be translated.
     * @return - list of keys - where the result set size equals the keyArray size.
     * The resulting array corresponds to the values in the keyArray.
     * If the key is not found in the table, a -1 is returned for that position.
     */
    public Long[] translateKeys(Context context,
                                final String resultKeyColName,
                                final String keyArrayColName,
                                final Long[] keyArray) {

        final String[] colNames = {resultKeyColName, keyArrayColName};
        final long NOT_FOUND = -1;
        Long[] result = new Long[keyArray.length];

        // initialize result set to all "not found"
        for (int i = 0; i< result.length; i++) {
            result[i] = Long.valueOf(NOT_FOUND);
        }
        String selectSql =
            SELECT+Util.toCommaDelimitedString(colNames)+
            FROM+this.getTableName()+
            WHERE+keyArrayColName+IN+
            LP+
            Util.toCommaDelimitedString(keyArray)+
            RP+
            ORDER_BY+
            keyArrayColName;

        // now, perform the query getting all key pairs.
        SQLiteManager db = SQLiteManager.openForRead(context, TAG+".1");
        Cursor cursor = null;
        try {
            String[] selectionArgs = null;
            cursor = db.rawQuery(selectSql, selectionArgs);
            if(cursor != null) {
                int idCol = cursor.getColumnIndex(resultKeyColName);
                int fkCol = cursor.getColumnIndex(keyArrayColName);

                if (cursor.moveToFirst()) {
                    while (! cursor.isAfterLast()) {
                        long id = cursor.getLong(idCol);
                        long fk = cursor.getLong(fkCol);

                        int ix = Util.getIndex(keyArray, fk);
                        if (ix >= 0 && ix < result.length)
                            result[ix] = Long.valueOf(id);
                        cursor.moveToNext();
                    }
                }
            } else
                Log.e(TAG, NULL_CURSOR);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
            if (db != null)
                db.close(TAG+".1");
        }
        if (LOG_DEBUG) Log.d(TAG, "translate input="+Util.toCommaDelimitedString(keyArray)+
                                " ...  output="+Util.toCommaDelimitedString(result)+
                                " .. using selectSql="+selectSql);

        return result;
    }



    protected Drawable getDrawable(byte[] blob) {
        return null;
    }


    /** dump entire table contents to Log.i
     */
    public void dumpDebug(Context context, final String prefix) {

        if (! PRODUCTION_MODE) {

            Cursor cursor = null;
            SQLiteManager db = SQLiteManager.openForRead(context, TAG+".5");
            try {
                String sql = SELECT+ALL+FROM+getTableName();
                cursor = db.rawQuery(sql, null);
                if(cursor != null) {
                    cursor.moveToFirst(); // THIS STATEMENT IS REQUIRED ELSE THE WRONG NUMBER OF RECORDS IS
                    // BEING RETURNED IN THE CURSOR.
                    // Dump all rows
                    DbUtil.dumpCursor(prefix+": "+getTableName(), cursor, null);
                }
            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (cursor != null && ! cursor.isClosed())
                    cursor.close();
                if (db != null)
                    db.close(TAG+".5");
            }
        }
    }


    protected interface CursorRowHandler {

        public void onBeforeFirstRow();
        public void onCursorRow(final Cursor cursor);
        public void onAfterLastRow();
        public void onFinally();
    }


    protected class ProcessCursorSet {


        /** primary constructor */
        public ProcessCursorSet() {
            super();
        }


        /** convenience method to start the thread */
        public void processSetInThread(final Context context, final String selection,
                                       final CursorRowHandler cursorRowHandler) {

            processSetInThread(context, Thread.MIN_PRIORITY, selection, cursorRowHandler);

        }


        /** run a thread to process all records
         *
         *
         * @param context - context for data access
         * @param threadPriority - @see java.lang.Thread
         * @param selection - where clause without the "where"
         * @param cursorRowHandler - interface to handle each row
         */
        public void processSetInThread(final Context context, int threadPriority, final String selection,
                                       final CursorRowHandler cursorRowHandler) {

            if (cursorRowHandler == null || selection == null || context == null)
                throw new IllegalArgumentException("context, selection and cursorRowHandler parms all must be provided");
            Thread thread = new Thread() {

                public void run() {
                    Cursor cursor = context.getContentResolver().query(getTableUri(),
                                    null, selection, null, null);
                    if (cursor != null)
                        try {
                            if (cursor.moveToFirst()) {
                                cursorRowHandler.onBeforeFirstRow();
                                do {
                                    cursorRowHandler.onCursorRow(cursor);
                                } while (cursor.moveToNext());
                            }
                            cursorRowHandler.onAfterLastRow();

                        } catch (Exception e) {
                            e.printStackTrace();

                        } finally {
                            cursorRowHandler.onFinally();
                            cursor.close();
                        }
                }

            };
            thread.setPriority(threadPriority);
            thread.start();
        }
    }



	public static String encodeAsString(final Drawable d) {

        Bitmap bm = null;
        BitmapDrawable bd = (BitmapDrawable)d;
        bm = bd.getBitmap();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(CompressFormat.JPEG,100,bos);
        String s = Base64.encodeToString(bos.toByteArray(),Base64.DEFAULT);
        bm.recycle();
        return s;

    }




    /** decodes a column in a cursor using a string encoded in Base64 */
    public static Drawable decodeToDrawable(final Cursor cursor, int imageIx) {

        Drawable result = null;
        Bitmap bm = decodeAsString(cursor, imageIx);
        if (bm != null) {
            BitmapDrawable bd = new BitmapDrawable(bm);
            result = bd.getCurrent();
            //bm.recycle();
        }
        return result;
    }


    /** decodes a column in a cursor using a string encoded in Base64 */
    public static Bitmap decodeAsString(final Cursor cursor, int imageIx) {

        Bitmap result = null;

        String imageStr = cursor.getString(imageIx);
        if (imageStr != null && imageStr.length() > 0) {
            byte[] data;
//            try {
                //data = Base64x.decode(imageStr);
                data = Base64.decode(imageStr, Base64.DEFAULT);
                if (data != null && data.length > 0)
                    result = BitmapFactory.decodeByteArray(data, 0, data.length);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        return result;
    }

    /** base method to initialize table */
    public void initialize(final SQLiteDatabase db, final Context context) {

    }
}
