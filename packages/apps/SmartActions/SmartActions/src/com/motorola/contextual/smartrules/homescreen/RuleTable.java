/*
 * @(#)RuleTable.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        Apr 17, 2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;

/**
 * Logical representation to rule table
 * <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class RuleTable implements
        DbSyntax,
        Constants {

    /** This is the name of the table in the database */
    public static final String TABLE_NAME        = "RULE_TABLE";

    public static final String CREATE_SCRIPT = CREATE_TABLE + SPACE + TABLE_NAME + LP
                                                         + Columns.RULE_ID + INTEGER_TYPE + CONT
                                                         + Columns.WIDGET_ID + PKEY_TYPE + CONT
                                                         + Columns.RULE_KEY + TEXT_TYPE + RP;

    private static final String TAG = RuleTable.class.getSimpleName();
    private static RuleTable   sInstance;

    private RuleTable() {}

    public static RuleTable getInstance() {
        if (sInstance == null) sInstance = new RuleTable();
        return sInstance;
    }

    /**
     * Add a rule/widget map to DB
     *
     * @param re Rule entity
     */
    public void addRule(RuleEntity re) {
        SQLiteDatabase db = DBHelper.getInstance(re.getContext()).getWritableDatabase();
        try {
        	db.beginTransaction();
        	for (int wid : re.getWidgetIds()) {
                ContentValues values = new ContentValues();
                values.put(Columns.RULE_KEY, re.getRuleKey());
                values.put(Columns.RULE_ID, re.getRuleId());
                values.put(Columns.WIDGET_ID, wid);
                
                db.insert(TABLE_NAME, null, values);
        	}
        	db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
    }

    /**
     * Gets all the widgets mapped to the rule
     *
     * @param context context
     * @param ruleKey Rule key
     * @return All the widget ids mapped to rule key
     */
    public int[] getWidgets(Context context, String ruleKey) {
        int[] widgets = new int[0];
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        Cursor cursor = null;
        try {
        	db.beginTransaction();
	        cursor = db.query(TABLE_NAME, COLS, Columns.RULE_KEY + "=\'" + ruleKey + "\'", null,
	            null, null, null);
	        int widgetIndex = cursor.getColumnIndex(Columns.WIDGET_ID);
	        int arrayIndex = 0;
	        if (cursor != null) {
                widgets = new int[cursor.getCount()];
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    widgets[arrayIndex++] = cursor.getInt(widgetIndex);
                    cursor.moveToNext();
                }
            }
	        db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
            if (cursor != null && !cursor.isClosed()) 
            	cursor.close();
        }
        return widgets;
    }

    /**
     * Update the rule id for all the widgets mapped to the rule.
     *
     * @param context context
     * @param ruleId Rule ID
     * @param widgetId Widget ID.
     */
    public void updateRuleId(Context context, long ruleId, int widgetId) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
        	db.beginTransaction();
	        ContentValues values = new ContentValues();
	        values.put(Columns.RULE_ID, ruleId);
	        db.update(TABLE_NAME, values, Columns.WIDGET_ID + "=" + widgetId, null);
	        db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
    }

    /**
     * Get all the mapped rule keys.
     *
     * @param context context
     * @return all the mapped rule keys.
     */
    public String[] getRuleKeys(Context context) {
        String sql = "SELECT DISTINCT " + Columns.RULE_KEY + " FROM " + TABLE_NAME;
        ArrayList<String> list = getList(context, sql);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Get the result list for the given sql query.
     *
     * @param context context
     * @param sql Sql query
     * @return Resultant list
     */
    private ArrayList<String> getList(Context context, String sql) {
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        ArrayList<String> list = new ArrayList<String>();
        Cursor cursor = null;
        try {
        	db.beginTransaction();
        	cursor = db.rawQuery(sql, null);
        	if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(Columns.RULE_KEY);
                    for (int i = 0; i < cursor.getCount(); i++) {
                        list.add(cursor.getString(columnIndex));
                        cursor.moveToNext();
                    }
                }
        	}
        	db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
            if (cursor != null && !cursor.isClosed()) 
            	cursor.close();
        }
        return list;
    }

    /**
     * Delete all the widgets mapped to the rule.
     *
     * @param context context
     * @param ruleKey Rule Key
     * @return All the widgets that were mapped to the deleted rule
     */
    public int[] deleteRule(Context context, String ruleKey) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        int widgets[] = null;
        try {
        	db.beginTransaction();
        	widgets = this.getWidgets(context, ruleKey);
        	if (widgets.length > 0)
        		db.delete(TABLE_NAME, Columns.RULE_KEY + EQUALS + "\'" + ruleKey + "\'", null);
        	db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
        return widgets;
    }

    /**
     * Check whether the rule exists in DB.
     *
     * @param context context
     * @param deletedKey rule key
     * @return True if rule exists; false otherwise
     */
    public boolean hasRule(Context context, String deletedKey) {
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
        	db.beginTransaction();
	        cursor = db.query(TABLE_NAME, COLS, Columns.RULE_KEY + EQUALS + "\'" + deletedKey
	                + "\'", null, null, null, null);
	        if (cursor != null) 
	        	count = cursor.getCount();
	        db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
            if (cursor != null && !cursor.isClosed()) 
            	cursor.close();
        }
        return count > 0;
    }

    /**
     * Delete all the given widgets.
     *
     * @param context context
     * @param appWidgetIds Widget IDs
     * @return Rule key(s) mapped to the deleted widget(s)
     */
    public String[] deleteWidget(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return new String[0];
        StringBuilder inArgs = new StringBuilder();
        String[] keys = getRuleKeys(context, appWidgetIds);
        for (int i = 0; i < appWidgetIds.length; i++) {
            inArgs.append(appWidgetIds[i] + "");
            if (i > 0) inArgs.append(",");
        }
        String sql = DELETE + FROM + TABLE_NAME + WHERE + Columns.WIDGET_ID + IN + LP
                + inArgs.toString() + RP;
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
        	db.beginTransaction();
        	db.execSQL(sql);
        	db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
        return keys;
    }

    /**
     * Clear the local DB.
     *
     *  @param context context
     */
    public void purge(Context context) {
        String sql = DELETE + FROM + TABLE_NAME;
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
        	db.beginTransaction();
        	db.execSQL(sql);
        	db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
    }

    /**
     * Get all the rule key(s) mapped to the given widget(s)
     *
     * @param context context
     * @param appWidgetIds Widget IDs
     * @return Rule keys
     */
    public String[] getRuleKeys(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return new String[0];
        StringBuilder inArgs = new StringBuilder();
        for (int i = 0; i < appWidgetIds.length; i++) {
            inArgs.append(appWidgetIds[i] + "");
            if (i > 0) inArgs.append(",");
        }
        String sql = SELECT + Columns.RULE_KEY + FROM + TABLE_NAME + WHERE + Columns.WIDGET_ID + IN
                + LP + inArgs.toString() + RP;
        ArrayList<String> list = getList(context, sql);
        return list.toArray(new String[list.size()]);

    }

    /**
     * Dumps the content of rule table.
     *
     * @param context context
     * @param tag TAG to be used in LOG
     */
    public void dump(Context context, String tag) {
        if(tag == null) tag = TAG;
        String sql = SELECT_ALL + FROM + TABLE_NAME;
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        Cursor c = null;
        try {
        	db.beginTransaction();
	        c = db.rawQuery(sql, null);
	        if (c != null) {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    StringBuilder sb = new StringBuilder();
                    for (String col : COLS)
                        sb.append(getData(c, c.getColumnIndex(col)) + SPACE);
                    Log.d(tag, sb.toString());
                    c.moveToNext();
                }
	        }
	        db.setTransactionSuccessful();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	db.endTransaction();
        	if (c != null && !c.isClosed()) 
        		c.close();
        }

    }

    /**
     * Get the string equivalent of the data stored in specified column index
     *
     * @param c cursor
     * @param columnIndex column index
     * @return string equivalent of the data
     */
    private String getData(Cursor c, int columnIndex) {
        switch (c.getType(columnIndex)) {
            case Cursor.FIELD_TYPE_INTEGER:
                return c.getInt(columnIndex) + "";
            case Cursor.FIELD_TYPE_FLOAT:
                return c.getFloat(columnIndex) + "";
            case Cursor.FIELD_TYPE_STRING:
                return c.getString(columnIndex) + "";
            default:
                return "";
        }
    }

    /**
     * Columns
     *
     *<code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     *
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     *</pre></code>
     */
    public interface Columns {
        public final static String RULE_ID   = "RULE_ID";
        public final static String RULE_KEY  = "RULE_KEY";
        public final static String WIDGET_ID = "WIDGET_ID";
    }

    private static final String[] COLS = { Columns.RULE_ID, Columns.RULE_KEY, Columns.WIDGET_ID };
}
