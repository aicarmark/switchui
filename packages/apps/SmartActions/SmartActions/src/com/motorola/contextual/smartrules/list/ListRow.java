/*
 * @(#)ListRow.java
 *
 * (c) COPYRIGHT 2011 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/01/23 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.list;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.widget.ParcelableArrayListMap;

/** This class abstracts a data row to be displayed in a ListView. These
 * ListViews are typically rule lists, but could also be other lists.
 * Each row is represented by a HashMap<String, Object>, and these elements are
 * comparable for sorting. 
 * 
 *<pre>
 * CLASS:
 * 	extends HashMap<String, Object>
 *	implements Comparable for sorting.
 *  implements ListRowInterface mainly to access those constants
 *
 * RESPONSIBILITIES:
 * - handles displaying of a data row in a ListView.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *   See each method.
 *
 **/

public class ListRow extends HashMap<String, Object> implements Comparable<ListRow>, 
																ListRowInterface,
																Constants {
	
	private static final String TAG = ListRow.class.getSimpleName();
	
	private static final long serialVersionUID = -5885039510068384983L;

	private static final int HASH_MAP_INITIAL_ELEMENT_SIZE = 
								ListRowInterface.RELATE_DB_COLUMNS.length;

	ParcelableArrayListMap mMemberOfArrayList = null;
	List<ListRow> mMemberOfList = null;
	
	/** basic constructor
	 */
	public ListRow() {
		super();
	}
	
	/** constructor
	 * 
	 * @param memberOf - member of parent list. This is required for sorting. 
	 */
	public ListRow(ParcelableArrayListMap memberOf) {
		
		super();
		mMemberOfArrayList = memberOf;
	}
	
	
	/** constructor
	 * 
	 * @param list - member of parent list
	 */
	public ListRow(List<ListRow> list) {
		
		super(HASH_MAP_INITIAL_ELEMENT_SIZE);
		mMemberOfList = list;
	}
	
	
	/** used for sorting lists by whatever column the list is desired to be sorted in.
	 * 
	 * @param another - the other ListRow being compared to 
	 * @return +1 if greater, -1 if < or zero if equal
	 * @see java.lang.compareTo 
	 */	
	public int compareTo(ListRow another) {
		
		if (mMemberOfArrayList == null) return 0;
		if (another == null) return +1;
		
		Object oAnother = another.get(mMemberOfArrayList.getSortColumn());
		if (oAnother == null) return +1;
		
		Object oThis = this.get(mMemberOfArrayList.getSortColumn());
		if (oThis == null) return -1;

		if (oThis instanceof String) {
			
			return ((String) oThis).compareTo((String)oAnother);
			
		} else if (oThis instanceof Long) { 
			
			return ((Long) oThis).compareTo((Long)oAnother);
			
		} else if (oThis instanceof Integer) { 
			
			return ((Integer) oThis).compareTo((Integer)oAnother);
			
		} else 
			
			return 0;
	}
    		
    /** dumps a List<Map<String, Object>> to the Log.
     * 
     * @param prefix - help to find the statements in the log
     */
	public void debug(String prefix){

		if (! PRODUCTION_MODE) { 		
			if (prefix == null)prefix = "";
			prefix = prefix+" ";
			
			if (LOG_INFO) Log.i(TAG, prefix+"dump ListRow: size="+this.size());
	
			Set<String> keys = this.keySet();
			int i =0; 
	    	for (String key : keys) {
	    		String s = null;
	    		if (this.get(key) != null)
	    			s = this.get(key).toString();
	    		if (LOG_INFO) Log.i(TAG, prefix+" "+(++i)+":"+key+"\t\t="+ s);    		
	    	}
		}
    }	
}

