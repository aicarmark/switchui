/*
 * @(#)CursorToList.java
 *
 * (c) COPYRIGHT 2011 MOTOROLA INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/01/23 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.list.ListRow;
import com.motorola.contextual.smartrules.widget.ParcelableArrayListMap;


/** CursorToList generally handles converting cursors to Lists that can be used
 * in widgets which display visual lists like ListAdapter. Using these methods can
 * be somewhat slower than manually converting lists because it uses class comparison
 * to determine field types, which is slower than a specialized method.
 * 
 *<pre>
 * CLASS:
 * 		This is a utility set of routines for converting cursors to ListRow which is
 *  	a HashMap type. Again, this can be slower than a specialized routine due to
 *  	the generic handling of various class and primitive types.
 *
 * RESPONSIBILITIES:
 * 		All the methods here are static, therefore not requiring any instance of this
 * 		class ever to be created.
 * 
 * COLABORATORS:
 * 		N/A
 *
 * USAGE:
 * 		see methods for usage instructions
 *</pre></code>
 **/
public class CursorToList implements Constants {	

	private static final String TAG = "CursorToList";	
	public static final int COLUMN_NOT_IN_DB = -1;
	
	
    /** This converts a cursor to a List Map. This is very useful in displaying
     * a result list that needs to be "tweaked" before it's displayed.
     * 
     * <code><pre>
     * Example:
	 *	Cursor cursor = fetchAll(mDb, FriendTable.TABLE_NAME, true);		
	 *	
	 *	String[]    dbColumnNames = {COL_ROW_ID, COL_NAME, COL_MOBILE_PHONE_NO};
	 *	Class<?>[] 	dbColumnClass = {int.class, String.class, String.class};
	 *	String[]    listColumnNames = dbColumnNames;		
     * 
     *  List<ListRow> list = null;
     *	try {
     *		list = CursorToList.convertToListMap(true,
     *			cursor,	dbColumnNames, dbColumnClass, listColumnNames, 0);
     *	} catch (CursorToListException e) {
     *		e.printStackTrace();
     *	} finally {
     *		cursor.close();
     *	}
     * 
     * </pre></code>
     * @param includeHeader - if true, includes a row at the top, showing column names.
     * @param cursor - database cursor consisting of the dbColumnNames that are 
     * 			desired to be returned in the list.
     * @param dbColumnNames - names of database columns to return (from the cursor)
     * 		to the list. If the dbColumnName doesn't exist in the cursor, the column 
     * 		will be added to the result array anyway, with a default value. This will 
     * 		allow the receiver of the list to modify those values before displaying.
     * @param dbColumnClass - array of classes like int.class, float.class, 
     * 		String.class, etc which mirror the DB column types. This length must equal
     * 		the length of the dbColumnNames, otherwise it will throw CursorToListException.
     * @param listColumnNames - list of column names in the result List. This length must 
     * 		equal the length of the dbColumnNames, otherwise it will throw CursorToListException.
     * @param imageDrawableResc - the integer resource number of a drawable, to use
     * 		if the column name isn't found in the column name list. Set to zero if
     * 		it does not apply. This drawable is the typical icon shown on the left
     * 		side of a list as a visual anchor.
     * @return 
     * 		1.) null if cursor is null. 
     * 		2.) empty list if no rows in cursor
     * 		3.) filled out list if cursor contains rows
     * @throws CursorToListException if
     * 		_dbColumnNames.length != listColumnNames.length or != dbColumnClass.length
     */
	public static List<ListRow> convertToListMap (
			
			boolean 	includeHeader,
			Cursor 		cursor,
			String[]    dbColumnNames,
			Class<?>[] 	dbColumnClass,
			String[]    listColumnNames,
			int 		imageDrawableResc 
			) throws CursorToListException {

		if (cursor == null) 
			return null;
		
		// get the number of columns 
		int size = dbColumnNames.length;
		
		// must be the same sizes 
		if (dbColumnClass.length   != size ||
			listColumnNames.length != size)
			throw new CursorToListException("size of input structures don't match, "+
					" dbColumnNames.length="+dbColumnNames.length+" should be the same as: "+
					" dbColumnClass.length="+dbColumnClass.length+", and: "+ 
					" listColumnNames.length="+listColumnNames.length);
		
		if (LOG_DEBUG) Log.d(TAG, "convertToListMap cursor size="+(cursor==null ? "null" : cursor.getCount()));
		
		// allocate the results List
	    List<ListRow> resultsList = new ArrayList<ListRow>();
	    // allocate the dbColNumbers array for the size of the number of columns in result
	    int[] dbColNumbers = new int[dbColumnNames.length];
	    // fill out dbColNumbers array (for each dbColumn Name, get the column numbers)	    
	    for (int i=0; i< size; i++) {
	    	try {
    			dbColNumbers[i] = cursor.getColumnIndexOrThrow(dbColumnNames[i]);
	    	} catch (IllegalArgumentException e) {
	    		// this is a normal case, see the dbColumnNames parm description.
	    		dbColNumbers[i] = COLUMN_NOT_IN_DB;
	    	}
	    }
	    
	    if (includeHeader) {
	    	// add header to resultsList
	    	// instantiate the row to contain the header
	        ListRow listRow = new ListRow(resultsList);
	    	// populate the row containing the header
		    for (int i=0; i< dbColumnNames.length; i++) {
		    	listRow.put(listColumnNames[i], listColumnNames[i]); 
		    }	    	
		    // add the header row to the results list
			resultsList.add(listRow);		    
	    }

	    // if cursor not empty, process each row.
	    if (cursor.moveToFirst()) {
		    // get each row (tuple) from the cursor.
		    resultsList = loadEachRow(cursor, dbColumnNames, dbColumnClass,
				listColumnNames, dbColNumbers, resultsList, imageDrawableResc);
	    }
	    
		return resultsList;		
	}	

	

	/** This method loads one row of the cursor into a result ListRow type.
	 * 
	 * @param cursor 			- db cursor
	 * @param dbColumnNames		- (input) array of string column names
	 * @param dbColumnClass 	- (input) array of class types
	 * @param listColumnNames 	- (output) array of col names
	 * @param dbColNumbers	  	- (input cursor) column numbers corresponding to the col names
	 * @param resultsList		- (output) ListRow  
	 * @param imageDrawableResc	- Resource number of the drawable to include with the row.
	 * @return same variable as "resultsList" input parm, 
	 * 			after adding fields from cursor.
	 */
	private static List<ListRow> loadEachRow(			
										Cursor 			cursor, 			
										String[]    	dbColumnNames,
										Class<?>[] 		dbColumnClass,
										String[]    	listColumnNames,
										int[] 			dbColNumbers,
										List<ListRow> 	resultsList,
										int 			imageDrawableResc 
														) {		
		while (! cursor.isAfterLast()) {
			
			// allocate row to attached to results list
	        ListRow listRow = new ListRow(resultsList);
		    for (int i=0; i< dbColumnNames.length; i++) {
		    	
		    	if (dbColNumbers[i] == COLUMN_NOT_IN_DB) {
		    		// the image drawable resource number is needed at least in 1 position
			    	listRow.put(listColumnNames[i], imageDrawableResc);
		    	}
		    	else {
		    		// put the column name and value into the list
			    	if (dbColumnClass[i] == int.class) {
			    		listRow.put(listColumnNames[i], cursor.getInt(dbColNumbers[i]));
			    	} else if (dbColumnClass[i] == String.class) {	
			    		listRow.put(listColumnNames[i], cursor.getString(dbColNumbers[i]));
			    	} else if (dbColumnClass[i] == long.class) {
			    		listRow.put(listColumnNames[i], cursor.getLong(dbColNumbers[i]));
			    	} else if (dbColumnClass[i] == float.class) {
			    		listRow.put(listColumnNames[i], cursor.getFloat(dbColNumbers[i]));
			    	} else if (dbColumnClass[i] == double.class) {
			    		listRow.put(listColumnNames[i], cursor.getDouble(dbColNumbers[i]));
				}  else if(dbColumnClass[i] == byte[].class) {
					listRow.put(listColumnNames[i], cursor.getBlob(dbColNumbers[i]));
				}
		    	}
		    }
		    
			// add tuple to results list
			resultsList.add(listRow);
			cursor.moveToNext();
		}
		return resultsList;
	}

	
	
    /** Converts a cursor to a List Map. This is very useful in displaying
     * a result list that needs to be "tweaked" before it's displayed.
     * 
     * @param includeHeader - if true, includes a row at the top, showing column names.
     * @param cursor - database cursor containing the dbColumnNames that are 
     * 			desired to be returned in the list.
     * @param dbColumnNames - names of database columns to return in the list. 
     * 		if the dbColumnName doesn't exist in the cursor, the column will be added
     *  	to the result array anyway, with a default value. This will allow the
     *  	receiver of the list to modify those values before display. 
     * @param dbColumnClass - array of classes like int.class, float.class, 
     * 		String.class, etc which mirror the DB column types. This length must equal
     * 		the length of the dbColumnNames.
     * @param listColumnNames - list of column names in the result List. This length must equal
     * 			the length of the dbColumnNames.
     * @param imageDrawableResc - the integer resource number of a drawable, to use
     * 		if the column name isn't found in the column name list.
     * @return 
     * 		1.) null if cursor is null. 
     * 		2.) empty list if no rows in cursor
     * 		3.) filled out list if cursor contains rows
     * @throws CursorToListException if
     * 		_dbColumnNames.length != listColumnNames.length or != dbColumnClass.length
     */
	public static ParcelableArrayListMap toParcelableArrayListMap (
			
			boolean 	includeHeader,
			Cursor 		cursor,
			String[]    dbColumnNames,
			Class<?>[] 	dbColumnClass,
			String[]    listColumnNames,
			int 		imageDrawableResc 
											) throws CursorToListException {

		List<ListRow> listMap = convertToListMap(
					includeHeader, cursor, dbColumnNames, dbColumnClass, 
					listColumnNames, imageDrawableResc);
		
		ParcelableArrayListMap parcelableArrayListMap = null;
		
		if(listMap != null)
			parcelableArrayListMap = new ParcelableArrayListMap(listMap);
		
		return parcelableArrayListMap;
	}
}