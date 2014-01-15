/*
 * @(#)ParcelableArrayListMap.java
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
package com.motorola.contextual.smartrules.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.list.ListRow;

import android.os.Parcel;
import android.os.Parcelable;



/** This class provides a container for ArrayList content that is parcelable to
 *  allow for bundling.
 * 
 *<code><pre>
 * CLASS:
 * 	extends ArrayList - again a container for showing lists in an adapter
 *  implements Parcelable - to allow for bundling of this list content.
 *
 * RESPONSIBILITIES:
 * 	This class simply provides a container for an ArrayList of type ListRow.
 *  This is used in many places in our app to show lists of items in a ListAdapter.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  See individual routines.
 *  
 *</pre></code>
 */
public class ParcelableArrayListMap extends ArrayList<ListRow> implements Parcelable, Constants {

	private static final long serialVersionUID = 1982337175170937169L;

	private String sortCol = "";

	
	/** basic constructor
	 */
	public ParcelableArrayListMap() {
		super();
	}
	
	
	/** constructor providing initial size of the list 
	 * @param i - initial size of the list
	 */
	public ParcelableArrayListMap(int i) {
		super(i);
	}

	
	/** constructor cloning an existing list of list row.
	 * 
	 * @param list - existing ListRow list.
	 */
	public ParcelableArrayListMap(List<ListRow> list) {
		super();
		this.addAll(list);
	}
	
	
	/** reconstructs instance from a parcel.
	 * 
	 * @param parcel
	 */
	public ParcelableArrayListMap(Parcel parcel) {
		
		super();
		int arraySize = parcel.readInt();
		// loop through the List
		for (int i=0; i<arraySize; i++) {
	        ListRow listRow = new ListRow(this);
	        int listRowSize = parcel.readInt();
	        // loop through the map elements 
	        for (int j=0; j<listRowSize; j++) {
	        	String key = parcel.readString();
	        	Object value = parcel.readValue(null);
	        	listRow.put(key, value);
	        }	        
			this.add(listRow);
		}
	}

	
		
	/** standard parcelable creator */
	public static final Parcelable.Creator<ParcelableArrayListMap> CREATOR = 
		new Parcelable.Creator<ParcelableArrayListMap>() {

			public ParcelableArrayListMap createFromParcel(Parcel in) {
				return new ParcelableArrayListMap(in);
			}

			public ParcelableArrayListMap [] newArray(int size) {
				return new ParcelableArrayListMap[size];
			}
	};	
	
	
	/** standard write to parcel.
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeInt(this.size());
        // for each element in the List
		for (int i=0; i<this.size(); i++) {
			
	        Map<String, Object> listRow = this.get(i);
	        
	        dest.writeInt(listRow.size());
	        Object[] keys = listRow.keySet().toArray();
	        Object[] values = listRow.values().toArray();
	        
	        // for each element in the map
	        for (int j=0; j<keys.length; j++) {
	        	String key = (String)keys[j];
	        	dest.writeString(key);
	        	dest.writeValue(values[j]);	        	
	        }
		}
		
	}


	/** required method for those classes implementing Parcelable.
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		return 0;
	}

	
	/** Getter - sortCol
	 * 
	 * @return sortCol
	 */
	public String getSortColumn() {
		
		return sortCol;
	}
	
	
	/** Setter - sortCol
	 * 
	 * @param sortCol to be stored
	 */
	public String setSortColumn(String sortCol) {
		
		return this.sortCol = sortCol;
	}

	
	/** Perform sort function
	 * 
	 * @param name - column name to sort by.
	 */
	public void sortByName(final String name) {

		sortCol = name;
		java.util.Collections.sort(this);
	}
	
	/** dumps the entire ArrayList of ListRow to the Log
	 * 
	 * @param prefix
	 */
	public void dumpDebug(final String prefix) {
		
		if (! PRODUCTION_MODE) { 
		
			Iterator<ListRow> i = this.iterator();
			while (i.hasNext()) {
				ListRow r = i.next();
				r.debug(prefix+" row:"+r+" ");
			}
		}
	}
	
}
