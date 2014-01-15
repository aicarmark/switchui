/*
 * @(#)ConflictItem.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/29/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uiabstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.view.ActiveSettingsView;

/** This class defines the attributes needed for a ConflictItem which is part
 * 	of the Conflicts stack and provides methods to operate on the ConflictItem.
 * 
 *<code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * 	implement the methods that can be called by the UI layer.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ConflictItem {
	
	private static final String TAG = ConflictItem.class.getSimpleName();
	
	public long ruleId;
	public String ruleName;
	public String ruleIconResource;
	public byte[] ruleIconBlob;
	public String actionDesc;
	public String modal;
	public String actionPubKey;
	public String actionName;
	public String actionTargetState;
	public String actionActivityIntent;
    public String actionId;
    public String actionConfig;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RULE_ID = "+ruleId)
				.append(" RULE_NAME = "+ruleName)
				.append(" RULE_ICON = "+ruleIconResource)
				.append(" ACTION_DESCRIPTION = "+actionDesc)
				.append(" MODAL = "+modal)
				.append(" ACTION_PUBLISHER_KEY = "+actionPubKey)
				.append(" STATE_MACHINE_NAME = "+actionName)
				.append(" TARGET_STATE = "+actionTargetState)
				.append(" CONFIG = "+actionConfig)
				.append(" ACTIVITY_INTENT = "+actionActivityIntent)
				.append(" ACTION_ID = "+actionId);
		return builder.toString();
	}
	
	/** construct a ConflictElement item for the current cursor row.
	 * @param context TODO
	 * @param cursor - cursor to read values from
	 *
	 * @return a ConflictElement item
	 */
	public static ConflictItem parseIntoConflictItem(Context context, Cursor cursor) {
		ConflictItem item = new ConflictItem();
		item.ruleId = cursor.getLong(cursor.getColumnIndex(RuleTable.Columns._ID));
		item.ruleName = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.NAME));
		item.ruleIconResource = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.ICON));
		item.ruleIconBlob = IconPersistence.getIconBlob(context, item.ruleId);
		item.actionDesc = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.ACTION_DESCRIPTION));
		item.modal = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.MODAL));
		item.actionPubKey = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.ACTION_PUBLISHER_KEY));
		item.actionName = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.STATE_MACHINE_NAME));
		item.actionTargetState = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.TARGET_STATE));
		item.actionActivityIntent = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.ACTIVITY_INTENT));
		item.actionId = cursor.getString(cursor.getColumnIndex(ActiveSettingsView.Columns.ACTION_ID));
		item.actionConfig = cursor.getString(cursor.getColumnIndex(ActionTable.Columns.CONFIG));
		
		return item;
	}
	
	/** returns a Map object instance of the ConflictItem
	 *  
	 * @return Map object instance of ConflictItem
	 */
	public Map<String, Object> getMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(RuleTable.Columns._ID, this.ruleId);
		map.put(RuleTable.Columns.NAME, this.ruleName);
		map.put(RuleTable.Columns.ICON, this.ruleIconResource);
		map.put(IconTable.Columns.ICON, this.ruleIconBlob);
		map.put(ActionTable.Columns.ACTION_DESCRIPTION, this.actionDesc);
		map.put(ActionTable.Columns.ACTION_PUBLISHER_KEY, this.actionPubKey);
		map.put(ActionTable.Columns.STATE_MACHINE_NAME, this.actionName);
		map.put(ActionTable.Columns.CONFIG, this.actionConfig);
		map.put(ActionTable.Columns.ACTIVITY_INTENT, this.actionActivityIntent);
		map.put(ActiveSettingsView.Columns.ACTION_ID, this.actionId);		
		return map;
	}
	
	/** takes a cursor and constructs a List of ConflictItem instances.
	 * 
	 * @param context - context
	 * @param cursor - cursor
	 * @return list of ConflictItem instances
	 */
	public static List<ConflictItem> convertToConflictItemList(Context context, Cursor cursor) {
		ArrayList<ConflictItem> list = new ArrayList<ConflictItem>();		
		if(cursor == null) {
			Log.e(TAG, "Null cursor returned for conflicts");
		} else {
			try {
				if(Constants.LOG_DEBUG) Log.d(TAG, "convertToConflictItemList");
				
				if (cursor.moveToFirst()) {
					if(Constants.LOG_DEBUG) Log.d(TAG, "convertToConflictItemList" + cursor.getCount());
					for (int i = 0; i < cursor.getCount(); i++) {
						ConflictItem conflictItem = ConflictItem.parseIntoConflictItem(context, cursor);
						list.add(conflictItem);
						cursor.moveToNext();
					}
				} else {
					Log.e(TAG, "cursor.moveToFirst failed for conflicts cursor");
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (!cursor.isClosed()) cursor.close();
			}
		}	
		return list;
	}
}