/*
 * @(#)AddRuleList.java
 *
 * (c) COPYRIGHT 2011 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/01/23 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.list;

import com.motorola.contextual.smartrules.db.CursorToList;
import com.motorola.contextual.smartrules.db.CursorToListException;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesimporter.FileUtil;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;
import com.motorola.contextual.smartrules.widget.ParcelableArrayListMap;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/** 
 * Class for showing a list in the add a new rule. 
 * 
 *<pre>
 * CLASS:
 * 	extends RuleListBase to encapsulate a basic rule list
 * 	implements Parcelable - to allow bundling.
 *
 * RESPONSIBILITIES:
 * - handles the Rule list.
 *
 * COLABORATORS:
 * 	RuleListBase - base class
 *  all of the com.motorola.contextual.smartrules.db. classes, 
 *
 * USAGE:
 *  This is somewhat standalone, see each method.
 *
 **/
public class AddRuleList extends RuleListBase implements Parcelable {

	private static final long serialVersionUID = -4179742353101267366L;

	private static final String TAG = RuleListBase.class.getSimpleName();
	
	/** basic constructor
	 */
	public AddRuleList() {
		super();
	}
		
	/** constructor - cloning another list
	 * @param parcelableMap - list to clone
	 */
	public AddRuleList(ParcelableArrayListMap parcelableMap) {
		super(parcelableMap);
	}
		
	/** constructor - cloning another list
	 * @param base - list to clone
	 */
	public AddRuleList(RuleListBase base) {
		super(base);
	}

	/** constructor - from a parcel
	 * @param in - parcel from which to reconstruct the instance.
	 */
	public AddRuleList(Parcel in) {
		super(in);
	}
		
	/** write to a parcel
	 * @param parcel - parcel in which to write this instance.
	 * @param flags - parcel flags.
	 */
	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);	
	}	
			
	/** CREATOR to be used for parcel reconstruction.
	 */	
	public static final Parcelable.Creator<AddRuleList> CREATOR = 
		new Parcelable.Creator<AddRuleList>() {

			public AddRuleList createFromParcel(Parcel in) {
				return new AddRuleList(in);
			}

			public AddRuleList[] newArray(int size) {
				return new AddRuleList[size];
			}
	};	

	/** gets the data for the requested list based on the listRow type.
	 * 
	 * @param context - context
	 * @param listRowType - @see ListRowInterface
	 * @return - a new instance of this list.
	 */	
	public static AddRuleList getData(Context context, int listRowType) {
				
		Cursor cursor = RulePersistence.getVisibleRulesCursor(context, listRowType);
		
		return new AddRuleList(parseCursorToList(cursor));
	}
	
	/** gets the data for the requested list based on the whereClause.
	 * 
	 * @param context - context
	 * @param whereClause - where clause
	 * @return - a new instance of this list.
	 */
	public static AddRuleList getData(Context context,
								final String whereClause) {
		
    	return new AddRuleList(fetchData(context, whereClause));
	}	
	
	/** function to pull the actual data from the database.
	 * 
	 * @param context - context
	 * @param whereClause - where clause to pull the content.
	 * @return - instance of RuleListBase, which can be then cloned into an 
	 * instance of this list.
	 */
	public static RuleListBase fetchData(Context context, 
					final String whereClause) {
	
		Cursor cursor = RulePersistence.getDisplayRulesCursor(context, whereClause);		

		return parseCursorToList(cursor);		
	}
	
	/** parses the passed in cursor to a rule list 
	 * @param context TODO
	 * @param cursor - cursor
	 *
	 * @return a RuleListBase list of rules
	 */
	public static RuleListBase parseCursorToList(Cursor cursor) {		
		RuleListBase list = null;
		if(cursor == null) {
			Log.e(TAG, "Cursor returned is null");
		} else {
			try {
				if(cursor.moveToFirst()) {
					list = new AddRuleList(CursorToList.toParcelableArrayListMap(false,
							cursor, ListRowInterface.RELATE_DB_COLUMNS,
							ListRowInterface.objectCorrelationClasses, ListRowInterface.RELATE_DB_COLUMNS, 0));
				}
			}
			catch (CursorToListException e) {
				e.printStackTrace();
			} finally {
				if(! cursor.isClosed())
					cursor.close();
			}
		}
		return list;		
	}		
	
    /** 
     * This is called by a method in the base class for every row.
     * The output of this method is to put these values/keys into the listRow.
     * 
     * 	LIST_ROW_TYPE
     *	LIST_LINE_1_KEY 	
     *	LIST_LINE_2_KEY 	
     *	LIST_LINE_3_KEY
     *  RULE_ICON_KEY 	
     * 
     * @param context - context
     * @param listRow - as pulled in from the getData() process
     * @param listRowType - type of list for which this customize is called
     * @return - listRow as passed in
     */
    public ListRow customizeRow(Context context, ListRow listRow, int listRowType) {
        byte[] iconBlob = (byte[]) listRow.get(IconTable.Columns.ICON);
        /* This is actually defensive coding; Rule should not get stuck in this state.
         * However, this is just in case some users db is in a bad state before upgrading
         * to version with IKJBREL1-9108/IKMAINJB-2756 fix
         */
        if (TableBase.Validity.INVALID.equals((String) listRow.get(RuleTable.Columns.VALIDITY))){
            Rule rule = RulePersistence.fetchRuleOnly(context, (String) listRow.get(RuleTable.Columns.KEY));
            if (rule != null)
                rule.setValidity(RulesValidatorInterface.updateRuleValidity(context, (String) listRow.get(RuleTable.Columns.KEY)));
        }
        switch(listRowType) {
        case ListRowInterface.LIST_ROW_TYPE_AUTO:
        case ListRowInterface.LIST_ROW_TYPE_MANUAL:
            // set row type
            listRow.put(LIST_ROW_TYPE_KEY, listRowType);
            listRow.put(LIST_LINE_1_KEY, listRow.get(RuleTable.Columns.NAME));
            listRow.put(LIST_LINE_2_KEY, listRow.get(RuleTable.Columns.RULE_TYPE));
            listRow.put(LIST_LINE_3_KEY, listRow.get(RulePersistence.FAIL_COUNT));
            listRow.put(RULE_ICON_KEY, listRow.get(RuleTable.Columns.ICON));
            if(iconBlob == null) {
                String rulePub = (String) listRow.get(RuleTable.Columns.PUBLISHER_KEY);
                long ruleId = ((Long) listRow.get(RuleTable.Columns._ID)).longValue();
                String resName = (String) listRow.get(RuleTable.Columns.ICON);
                iconBlob = IconPersistence.getIconBlob(context, ruleId, rulePub, resName);
                listRow.put(IconTable.Columns.ICON, iconBlob);
            }
            break;

        case ListRowInterface.LIST_ROW_TYPE_BLANK:
        case ListRowInterface.LIST_ROW_TYPE_SAMPLES:
        case ListRowInterface.LIST_ROW_TYPE_SUGGESTIONS:
            // set row type
            listRow.put(LIST_ROW_TYPE_KEY, listRowType);
            listRow.put(LIST_LINE_1_KEY, listRow.get(RuleTable.Columns.NAME));
            listRow.put(LIST_LINE_2_KEY, FileUtil.getDescTag(listRow.get(RuleTable.Columns.DESC).toString(), XmlConstants.SHORT));
            listRow.put(LIST_LINE_3_KEY, listRow.get(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT));
            listRow.put(RULE_ICON_KEY, listRow.get(RuleTable.Columns.ICON));
            if(iconBlob == null) {
                String rulePub = (String) listRow.get(RuleTable.Columns.PUBLISHER_KEY);
                long ruleId = ((Long) listRow.get(RuleTable.Columns._ID)).longValue();
                String resName = (String) listRow.get(RuleTable.Columns.ICON);
                iconBlob = IconPersistence.getIconBlob(context, ruleId, rulePub, resName);
                listRow.put(IconTable.Columns.ICON, iconBlob);
            }
            break;
        }
	    return listRow;
    }
}