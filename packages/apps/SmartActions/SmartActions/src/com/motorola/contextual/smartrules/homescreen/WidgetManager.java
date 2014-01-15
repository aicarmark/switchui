/*
 * @(#)WidgetManager.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/11/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/**
 * Manages all the widgets. <code><pre>
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
public class WidgetManager {
    private static final String TAG             = WidgetManager.class.getSimpleName();
    private RulesFactory        mRulesFactory;
    private RuleManager         mDefaultManager = new RuleManager();

    /**
     * Constructor
     */
    public WidgetManager() {
        if (Constants.LOG_DEBUG) Log.d(TAG, "WM Created!");
        mRulesFactory = new RulesFactory();
    }

    /**
     * Put the widget in default state
     *
     * @param context context
     * @param widgetId Widget ID
     */
    public void setDefault(Context context, int widgetId) {
        mDefaultManager.deactivate();
        mDefaultManager.addRule(context, widgetId);
    }

    /**
     * Put all the given widgets in default state
     *
     * @param context context
     * @param widgetId Widget IDs
     */
    public void setDefault(Context context, int[] widgetIds) {
        for (int widgetId : widgetIds)
            setDefault(context, widgetId);
    }

    /**
     * Insert a widget/rule map to DB
     *
     * @param ruleKey Rule Key
     * @param ruleId Rule ID
     * @param widgetId Widget ID
     */
    public void mapRule(RuleEntity re) {
        RuleTable.getInstance().addRule(re);

        // This call creates one if RuleManager doesn't exist and adds to cache.
        mRulesFactory.get(re.getRuleKey());
    }

    /**
     * Get all the widgets mapped to the rule
     *
     * @param context context
     * @param ruleKey Rule key
     * @return All the widgets mapped to the rule
     */
    public int[] getWidgets(Context context, String ruleKey) {
        return RuleTable.getInstance().getWidgets(context, ruleKey);
    }

    /**
     * Get the rule manager for the given rule
     *
     * @param context context
     * @param ruleKey Rule key
     * @return Rule manager for the given rule
     */
    public RuleManager getRuleManager(String ruleKey) {
        return mRulesFactory.get(ruleKey);
    }

    /**
     * Update the rule id of a mapped rule.
     *
     * @param context context
     * @param ruleId Rule ID
     * @param widgetId Widget ID
     * @param context
     */
    public void updateRuleId(Context context, long ruleId, int widgetId) {
        RuleTable.getInstance().updateRuleId(context, ruleId, widgetId);
    }

    /**
     * Get all the rule keys. Also adds a rule manager if it doesn't exist.
     *
     * @param context context
     * @return Rule keys
     */
    public String[] getRuleKeysAndSync(Context context) {
        String[] keys = getKeys(context);
        for (String key : keys) {
            mRulesFactory.get(key);
        }
        return keys;
    }

    /**
     * Get all the rule keys.
     *
     * @param context context
     * @return Rule keys
     */
    public String[] getKeys(Context context) {
        return RuleTable.getInstance().getRuleKeys(context);
    }

    /**
     * Check whether the rule exists in DB.
     *
     * @param context context
     * @param deletedKey rule key
     * @return True if rule exists; false otherwise
     */
    public boolean hasRule(Context context, String deletedKey) {
        return RuleTable.getInstance().hasRule(context, deletedKey);
    }

    /**
     * Delete all the widgets mapped to the rule.
     *
     * @param context context
     * @param ruleKey Rule Key
     * @return All the widgets that were mapped to the deleted rule
     */
    public int[] deleteRule(Context context, String deletedKey) {
        mRulesFactory.removeRule(deletedKey);
        return RuleTable.getInstance().deleteRule(context, deletedKey);
    }

    /**
     * Delete the rule/widget mapping from DB
     *
     * @param context context
     * @param appWidgetIds
     */
    public void deleteWidget(Context context, int[] appWidgetIds) {
        String[] keys = RuleTable.getInstance().deleteWidget(context, appWidgetIds);

        // Clean up.
        for (String key : keys)
            if (!this.hasRule(context, key)) mRulesFactory.removeRule(key);
    }

    /**
     * Delete all the given widgets.
     *
     * @param context context
     * @param appWidgetIds Widget IDs
     * @return Rule key(s) mapped to the deleted widget(s)
     */
    public void purge(Context context, int[] appWidgetIds) {
        mRulesFactory.clearCache();
        // setDefault(context, appWidgetIds);
        RuleTable.getInstance().purge(context);
    }

    /**
     * Dumps the content of rule table to system out
     *
     * @param context context
     */
    public void dump(Context context, String tag) {
        RuleTable.getInstance().dump(context, tag);
    }

}
