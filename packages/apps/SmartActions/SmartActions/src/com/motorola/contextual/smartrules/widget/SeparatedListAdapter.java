/*
 * @(#)SeparatedListAdapter.java
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

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SimpleAdapter;


/** Provides the ability combine multiple lists into one. Android does not provide 
 * any standard widget for this purpose.
 *
 *<code><pre>
 * CLASS:
 * 	extends BaseAdapter - to provide basic adapter functionality
 *  implements Filterable - to allow the list to be auto-narrowed
 *
 * RESPONSIBILITIES:
 * 	support list creating of a separated list 
 * 					(separator bars -optionally not shown)
 *
 * COLABORATORS:
 *  ArrayAdapter - used as a container for each section and one for all headers.		
 *
 * USAGE:
 *  Any implementors of this class needing auto-narrowing must implement Filterable 
 *  	then call registerFilterListener(this) to get a callback to provide the
 *  	filtering.
 *
 *  Example:  
 *			SeparatedListAdapter mAdapter = null;
 *          ...
 *			mAdapter = new SeparatedListAdapter(mActivity);
 *		    mAdapter.registerFilterListener(this);
 *		    getListView().setTextFilterEnabled(true);	
 *		    if (autoNarrowTemp != null) {
 *		    	mAdapter.setFilterString(autoNarrowTemp);
 *		    	getListView().setFilterText(autoNarrowTemp);
 *		    	autoNarrowTemp = null;
 *		    }
 *  
 *</pre></code>
 **/
public class SeparatedListAdapter extends BaseAdapter implements Filterable { 
	  
//	private static final String TAG = "SeparatedListAdapter";   
	
    private static final String HIDDEN_HEADER_PREFIX = "BlankXYZ";
    private static final int HEADER_TYPE = 0;
    private static final int HEADER_TYPE_COUNT = 1;
    
    /** one adapter per "section" in the list - where each "section" is one separated list. 
     * Externally, sections are referred to as sections, internally, they are referred to
     * as an Adapter or an adapter in the array. */
    private ArrayAdapter<SimpleAdapter> 	mSections = null; 
    private ArrayAdapter<String> 			mHeaders;   
    
    private Context 						mContext;
    private String							mFilterString;    
    private SeparatedListFilter 			mFilter;
    private ViewBinder						mDelegate;
    
    /** array of positions in the list that are not clickable */
    private int[] 							mDisabledPositions = null;    

    /** for implementing the auto-narrowing of the entire list */
    Filter.FilterListener 					mFilterListener = null;
    
    //TODO: Create implementation of removing any given section
    
    
    /** Constructor
     * 
     * @param context - that uses this list
     */
    public SeparatedListAdapter(Context context) {   
    	
    	super();
        mContext = context;
        mHeaders = new ArrayAdapter<String>(context, R.layout.separated_list_header);  
        mSections = new ArrayAdapter<SimpleAdapter>(context, R.layout.display_rules_list_row);

    }  
    
    /** Optional ViewBinder delegate
     * In some cases the ViewBinder may be something other than a Context object.
     *
     * @param delegate
     */
    public void setDelegate(ViewBinder delegate) {
        mDelegate = delegate;
    }

    /** Adds a section to the list. Essentially each section is a separate list by itself,
     * which corresponds to one adapter in the adapter array list.
     * 
     * @param section Name of the section being added. Either null or "" will 
     * 				hide this header (header will not be seen in the list).
     * @param adapter List adapter to use for this
     * @return - section name used
     */
    public String addSection(final String _section, final SimpleAdapter adapter) { 
    	
    	String sectionName = _section;
    	
    	// handle hidden section headers (passed in as null or "")
    	if (_section == null || _section.trim().length() < 1) {
    		sectionName = HIDDEN_HEADER_PREFIX+mSections.getCount();
    		this.mHeaders.add(sectionName);   
    	} 
    	else {	
    		this.mHeaders.add(_section);   
    	}
        this.mSections.add(adapter);
        return sectionName;        
    }   

            
    /** get the adapter for a given section
     * 
     * @param sectionIx - must be between 0 and mAdapter.size()-1.
     * @return - adapter
     */
    public SimpleAdapter getAdapter(int sectionIx) {

    	return mSections.getItem(sectionIx);
    }
    
    
    /** This is the opposite of getSectionPosition. It returns the absolute list 
     * position given the relative position (section index and relative 
     * position within that section.
     * 
     * @param sectionIx - section for which the absolute position is needed.
     * @param relativePosition - relative position within the section[sectionIx] for which
     * 				to find the absolute position
     * @return absolute position or -1 if section or position doesn't exist.
     */
    public int getAbsolutePosition(int sectionIx, int relativePosition) {
    	
    	// if sectionIx outside the size of the list bounds (< 0 or > list size), return -1;
    	if (sectionIx < 0) 						return -1;
    	if (sectionIx >= mSections.getCount()) 	return -1;
    	
    	int result = 0;    	
    	for (int section=0; section<=sectionIx; section++) {
    		// count the row if visible
    		if (! mHeaders.getItem(section).startsWith(HIDDEN_HEADER_PREFIX))
    			result++;
    		// if i is not yet to the section we are targeting, add the full size 
    		//    of the section, else add the relative position
    		if (sectionIx > section)
    			result = result + getAdapter(section).getCount();
    		else
    			result = result + relativePosition;
    	}
    	return result;    	
    }
  
    
    /** clears the list */
    public void clear() {
    	this.mHeaders.clear();
    	this.mSections.clear();
    }
    
    
    
    /** gets the item at the absolute position. 
     * 
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int absolutePosition) {   
    	
    	RelativePosition relativePosition = new RelativePosition(absolutePosition);
    	if (relativePosition.isHeader()) 
    		return mHeaders.getItem(relativePosition.getAdapterIx());
    	else
    		return mSections.getItem(relativePosition.getAdapterIx()).getItem(relativePosition.getPosition());    	
    }   

    
    /** Required method for BaseAdapter.
     *  
     * returns a count of all the visible rows in all the sections, 
     * including visible headers.
     * 
     * @see android.widget.Adapter#getCount()
     */
    public int getCount() { 
    	
        // total together all sections, plus one for each section header   
        int total = 0;   
        for (int i=0; i< mSections.getCount(); i++){
        	total += mSections.getItem(i).getCount();
        	total += (mHeaders.getItem(i).startsWith(HIDDEN_HEADER_PREFIX)? 0 : 1); 
        }
        return total;   
    }   

    
    /** Required method for BaseAdapter.
     * 
     * gets a count of all the view types. 
     * 
     * @see android.widget.BaseAdapter#getViewTypeCount()
     */
    @Override    
    public int getViewTypeCount() {

    	// start at 1 because header type counts as 1 type.
        int total = HEADER_TYPE_COUNT;   
        for (int i=0; i< mSections.getCount(); i++){
        	// Each section can have 1 or many view types, in our case only 1 is used for this app.
            total += mSections.getItem(i).getViewTypeCount();   
        }
        return total;   
    }   
      
    
    /** Required by BaseAdapter.
     * returns the type number or index for the provided
     *  position in the list.
     *  
	 * <CODE><PRE>
	 * Note that there are typically mSections.size+1 view types in any list. 
	 * The view types are as follows: 
	 * 
	 * 			View Type 0 - any header row
	 * 			View Type 1 - the first section view 
	 * 			View Type 2 - the second section view 
	 * 					.....
	 * </PRE></CODE>
     * @see android.widget.BaseAdapter#getItemViewType(int)
     * 
     * @param absolutePosition - is the absolute position in the list
     * @return view type number for the given section in absolutePosition.
     */
    @Override    
    public int getItemViewType(int absolutePosition) {   

    	int result;
    	RelativePosition relativePosition = new RelativePosition(absolutePosition);
    	
    	if (relativePosition.isHeader()) {
    		result = HEADER_TYPE;
    	}
    	else {
    		// initialize at 1
	    	result = HEADER_TYPE_COUNT;    	
	    	for (int i=0; i<relativePosition.getAdapterIx(); i++) {
	    		// in our case, there should only be 1 type per section, 
	    		//		but this code allows for multiple types per section
	    		result += mSections.getItem(i).getViewTypeCount();
	    	}
    	}
    	return result;    	    	
    }   
  
    
    /** Required by BaseAdapter - Returns if the items are selectable or not
     * @return always false
     */
    public boolean areAllItemsSelectable() {   
        return false;   
    }   
  
    
    /** Returns isEnabled true/false for a given absolute position. Headers are not 
     * enabled, all other rows typically enabled.
     * 
     * @see android.widget.BaseAdapter#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int absolutePosition) {
    	
    	boolean result = true;
    	int i = -1;
    	if (mDisabledPositions != null) {
    		while (result && (++i < mDisabledPositions.length) ) {
        		if (absolutePosition == mDisabledPositions[i]) {
        			result = false;
        		}
    		}
    	} 
    	if (result) {
    		// if type is not header type, return true.
    		result = (getItemViewType(absolutePosition) != HEADER_TYPE); 
    	}
        return result;
    }   
  
            
    
    /** returns the view for the given position. 
     * 
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     * 
     * @param absolutePosition in the list
     * @param convertView - view to reuse if possible
     * @param parent - the eventual parent of the view
     * @return view to be used for this row.
     */
    public View getView(int absolutePosition, View convertView, ViewGroup parent) {   

    	RelativePosition relativePosition = new RelativePosition(absolutePosition);    	    			

        View resultView = null;
        if (relativePosition.isHeader()) {
        	resultView = mHeaders.getView(relativePosition.getAdapterIx(), convertView, parent);
        } else {	
            SimpleAdapter adapter = getAdapter(relativePosition.getAdapterIx());   
    		resultView = adapter.getView(relativePosition.getPosition(), convertView, parent);   
        	setViewValue( absolutePosition, resultView, adapter.getItem(relativePosition.getPosition()));
        }
    	return resultView;
    }   

    
	/** Sets the view value. This is to override the default operation of the list. 
	 * Typically to set images, checkboxes, etc
	 * 
	 * @param view view to operate under
	 * @param data current data for this view
	 */
	public void setViewValue(int absolutePosition, View view, Object data ) {
		
		if (mContext instanceof ViewBinder) {
			((ViewBinder)mContext).setViewValue(absolutePosition, view, data);
		} else if (mDelegate != null) {
            mDelegate.setViewValue(absolutePosition, view, data);
		}
	}

	
	/** Interface to use for this adapter
	 *
	 */
	public interface ViewBinder {
		public void setViewValue(int position, View view, Object data );
	}


    /** 
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int absolutePosition) {   
        return absolutePosition;   
    }   
    
    
    
	/** Required by Filterable implementation. 
	 * Gets any filter set for auto-narrowing of the list. 
	 * 
	 * @see android.widget.Filterable#getFilter()
	 */
	public Filter getFilter() {
		
		if (mFilter == null) {
			mFilter = new SeparatedListFilter();
		}
		return mFilter;		

	}
	
	
	/** getter - for mDisabledPositions
	 * 
	 * @return the disabledPositions
	 */
	public int[] getDisabledPositions() {
		return mDisabledPositions;
	}


	/** setter - for mDisabledPositions
	 * 
	 * @param disabledPositions the disabledPositions to set
	 */
	public void setDisabledPositions(int[] disabledPositions) {
		this.mDisabledPositions = disabledPositions;
	}
	
	
	/** getter for mFilterString  
	 * 
	 * @return the filterString
	 */
	public String getFilterString() {
		
		if (mFilterString == null) 
			return "";
		else
			return mFilterString;
	}


	/** setter for mFilterString 
	 * 
	 * @param filterString the filterString to set
	 */
	public void setFilterString(String filterString) {
		this.mFilterString = filterString;
	}

	
	/** setter for mFilterListener
	 * 
	 * @param filterListener the listener to set
	 */
	public void registerFilterListener(Filter.FilterListener filterListener) {
		this.mFilterListener = filterListener;
	}

	/** setter for mFilterListener - sets it to null. 
	 * unregister the filter listener - passing the listener to allow for future multiple
	 * listeners, currently only support 1 per instance.
	 * 
	 * @param listener - listener registered originally.
	 */
	public void unregisterFilterListener() {
		this.mFilterListener = null;
	}
	
	
	/** this class handles filtering operations (auto-narrowing) for a
	 * SeparatedListAdapter.
	 */
	private class SeparatedListFilter extends Filter {

		/**
		 * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
		 */
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			
			mFilterString = constraint.toString(); 
			
			// return a dummy set of results as we're not using them for anything
			FilterResults resultFilter = new FilterResults();			
			resultFilter.values = null;
			resultFilter.count = 0;
			
			return resultFilter;
		}

		

		/**	This method fires a registered listener to do something to publish results
		 * to the user.
		 * 
		 * @see android.widget.Filter#publishResults(java.lang.CharSequence, android.widget.Filter.FilterResults)
		 */
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			
			// call the registered onFilterComplete listener to update the data
			if (mFilterListener != null) 
				mFilterListener.onFilterComplete(0);
			
			// will cause the list to refresh on the list
			notifyDataSetChanged();
			notifyDataSetInvalidated();
		}
	}

	
	/** This class is a container class to keep track of the relative
	 *  section (adapter) and position of an element in this separated list.
	 *  
	 *  An absolute position is the simple row number of the list 
	 *  from zero to (list size-1).
	 *  
	 *  The relative position is the adapter or section number and the relative position
	 *  within that section. For example, the first absolute position of the list 
	 *  (absolute position 0), is relative position adapter (or section) 0, position 0. 
	 */
	public class RelativePosition {
		
	    private static final int SECTION_HEADER_RELATIVE_POSITION = -1;
		 /**  The adapterIx variable tracks the relative section or adapter. */
		private int adapterIx;
	     /**  The "position" variable is the position index within the section - can be set 
	     *  to -1 if the relative position is the header (if the header is visible, 
	     *  some are not). The first item below the header is a relative position 0.*/
		private int position;

		
		/** basic constructor
		 */
		public RelativePosition() {
			super();
			adapterIx = 0;
			position = 0;
		}

		/** constructor using the absolute position.
		 * The absolute position is 0 to row count -1.
		 * The relative positions are (Section, Position)
		 */
		public RelativePosition(int absolutePosition) {
			setRelativePosition(absolutePosition);
		}
		
		/** constructor with section and position
		 * 
		 * @param adapter - section of the separated list. Each section is a SimpleAdapter.
		 * sections are numbered 0 thru (number of sections -1).
		 * @param position - position within section. The positions are -1 (for a header) 
		 * thru (section row count -1).
		 */
		public RelativePosition( int adapter, int position) {
			
			this.adapterIx = adapter;
			this.position = position;
		}

		/** getter - for section index
		 */
		public int getAdapterIx() {
			return adapterIx;
		}
		
		/** setter 
		 * 
		 * @param adapterIx - section to set
		 */
		public void setAdapterIx(int adapterIx) {
			this.adapterIx = adapterIx;			
		}
		
		/** getter - for section position
		 */
		public int getPosition() {
			return position;
		}
		
		/** setter
		 * 
		 * @param position - to be set
		 */
		public void setPosition(int position) {
			this.position = position;
		}

		
		/** returns true if this section/position is a header in the list.
		 * 
		 * @return true if this section/position is a header in the list.
		 */
		public boolean isHeader() {
			return (position == SECTION_HEADER_RELATIVE_POSITION);
		}


		/** returns true if this section has a hidden header in the list.
		 * 
		 * @return true if this section has a hidden header row in the list.
		 */
		public boolean isSectionHeaderHidden() {
			return mHeaders.getItem(adapterIx).startsWith(HIDDEN_HEADER_PREFIX);
		}

		
	    /** debugs a relative position in the list, converts it to
	     *  a string good for debugging.
	     *  
	     * @return - string containing debug info.
	     */
	    public String toString() {
			return " adaptIx/pos="+adapterIx+"/"+position;
	    }

	    
	    /** Sets the relative position in this class for the passed absolute position.
	     * 
	     * Strategy - start from the top of the list, deducting the size of each
	     * section from the absolute position, until arriving at the section and
	     * position of the relative position.
	     * 
	     * @param absolutePosition - absolute position in the list
	     */
	    public void setRelativePosition(int absolutePosition) {
	    	    	    	
	    	int adapterIx = 0;    	    	
	    	int position = absolutePosition;  // initialize it to absolute position  	    	
	    	boolean searching = true;
	    	
	    	while (searching) {
	    		if (! mHeaders.getItem(adapterIx).startsWith(HIDDEN_HEADER_PREFIX))
	    			position--;
	    		if (position - mSections.getItem(adapterIx).getCount() >= 0) {
	    			position = position - mSections.getItem(adapterIx).getCount();
	    			adapterIx++;
	    		} else {
	    			searching = false;
	    		}
	    	}
	    	
	    	this.adapterIx = adapterIx;
	    	this.position = position;
	    }

	}

}  
