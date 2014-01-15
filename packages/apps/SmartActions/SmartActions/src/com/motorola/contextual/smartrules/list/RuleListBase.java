/*
 * @(#)RuleListBase.java
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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.Iterator;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.widget.ParcelableArrayListMap;

/** This is a base class for implementing different rule list types.
 * 
 *<pre>
 * CLASS:
 *  extends ParcelableArrayListMap - to manage the list map
 *  implements ListRowInterface - ListRow constants
 *  implements DbSyntax - for database SQL language constants.
 *  Parcelable - to allow bundling.
 *
 * RESPONSIBILITIES:
 * - manages different rule list types.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 *
 **/
public abstract class RuleListBase extends ParcelableArrayListMap 
										implements ListRowInterface, DbSyntax, Parcelable {		
	
	/** each subclass must implement a customizeRow method to handle any customization 
	 * of the raw data that otherwise wouldn't look pretty in the row.	
	 * 
	 * @param context - context
	 * @param listRow - list row of the given row to customize
	 * @return - output of the customized row (listRow altered)
	 */
    public abstract ListRow customizeRow(Context context, ListRow listRow, int listRowType);
		
	private static final long serialVersionUID = 2504236679584225720L;
		
    private ParcelableArrayListMap map = null;
    
	/** constructor, should not be called directly except by subclasses, but needs to 
	 * be public for Parcel support. This possibly could be protected, but not sure 
	 * if that would work for parceling issues.
	 */
	public RuleListBase() {
		super();
	} 
	
	/** constructor
	 * 
	 * @param m - list to clone into this list.
	 */
	public RuleListBase(final ParcelableArrayListMap m) {
		super();
		this.map = m;
		if (m != null)
			this.addAll(m);
	}
	
	/** Constructor from a parcel. 
	 * 
	 * @param parcel
	 */
	public RuleListBase(Parcel parcel) {
		super(parcel);  
	}
	
	/** get map used in constructor - or returns null
	 * 
	 * @return if this instance was constructed from a ParcelableArrayListMap
	 */
    public ParcelableArrayListMap getMapFromConstructor() {
		return map;
	}
    
	/** writes this instance to a parcel. This is handy for preserving the list
	 * during pause/resume operations.
	 */
	public void writeToParcel(Parcel dest, int flags) {		
		super.writeToParcel(dest, flags);
	}
					
    /** generic customize List method - which simply calls customizeRow for each row.
     *  
     * @param context - context
     * @return for convenience, returns this instance 
     */
    public RuleListBase customizeList(Context context, int listRowType) {

    	Iterator<ListRow> iter = this.iterator();
    	
    	while (iter.hasNext()) {    		
            ListRow listRow = iter.next();           
            customizeRow(context, listRow, listRowType); 
    	}    	
    	return this;
    }
		
	/** binds this list to the ListView using a SimpleAdapter. 
	 * 
	 * @param context - context
	 * @param listView  - ListView to be bound
	 */
    public void bindListAdapter(Context context, ListView listView) {   	
	    // Now create a simple cursor adapter and set it to display
	    listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);        
	    listView.setAdapter(bindListToAdapter(context) );
    }

	/** binds this list to the Landing Page ListView using a SimpleAdapter. 
	 * 
	 * @param context - context
	 */
    public SimpleAdapter bindLandingPageListToAdapter(Context context) {
    	final String [] from = {
    			RuleListBase.LIST_LINE_1_KEY,
				RuleListBase.LIST_LINE_2_KEY,
				RuleListBase.LIST_LINE_3_KEY,
				RuleListBase.RULE_ICON_KEY
    	};
    	
    	int[] to = new int[] {
    			R.id.first_line,
    			R.id.second_line,
    			R.id.status_line,
    			R.id.rule_icon
    	};
    	
    	SimpleAdapter adapter = bindToAdapter(context, R.layout.mm_list_row, from, to); 	
    	return adapter;
    }
    
    /** binds the list View to the Adapter, returns the SimpleAdapter.
     * 
     *  @param context - context
     *  @return - SimpleAdapter instance for this list.
     */
	public SimpleAdapter bindListToAdapter(Context context) {
		
		final String [] from  = {  
					RuleListBase.LIST_LINE_1_KEY,
					RuleListBase.LIST_LINE_2_KEY,
					RuleListBase.LIST_LINE_3_KEY,
					RuleListBase.RULE_ICON_KEY
			}; 
	    
	    // and an array of the fields we want to bind those fields to 
	    int[] to = new int[] {
	    		R.id.display_rules_first_line,
	    		R.id.display_rules_second_line,
	    		R.id.added_line,
	    		R.id.display_rules_mode_icon
	    		};
		    
	    SimpleAdapter adapter = bindToAdapter(context, 
	    		R.layout.display_rules_list_row, from, to);	
	    return adapter;
	}
    	
    /** Instantiates the SimpleAdapter. This was broken out to allow overrides as necessary.
     * 
     * @param context
     * @param resourceIdForRowLayout - Resource id for the row layout
     * @param from - String array of names in the ListRow of String key values to map data from.
     * @param to - resource ids of widgets to bind the "from" list to.
     * @return - SimpleAdapter instance.
     */
    public SimpleAdapter bindToAdapter(Context context, 
    						int resourceIdForRowLayout, String[] from, int[] to) {
    	
    	return new SimpleAdapter(context, this, resourceIdForRowLayout, from, to);
    }	
}