/*
 * @(#)RuleEntity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/10/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.RuleTable;

import android.content.Context;
import android.content.Intent;

/**
 * Data object for a mapped rule
 * <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *  Represents the mapping between a rule and widgets.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class RuleEntity {
    private long    mRuleId;
    private String  mRuleKey;
    private int[]   mWidgetId;
    private String mRuleName;
    private String mRuleIcon;
    private Context mContext;

    /**
     * Constructor
     *
     * @param ruleId Rule ID
     * @param ruleKey Rule Key
     * @param widgetId Widget ID
     */
    public RuleEntity(long ruleId, String ruleKey, int widgetId) {
        this(ruleId, ruleKey, new int[] { widgetId }, null);
    }

    /**
     * Constructor
     *
     * @param ruleId Rule ID
     * @param ruleKey Rule Key
     * @param widgetId Widget IDs
     */
    public RuleEntity(long ruleId, String ruleKey, int[] widgetId) {
        this(ruleId, ruleKey, widgetId, null);
    }

    /**
     * Constructor
     * @param ruleId Rule ID
     * @param ruleKey Rule Key
     * @param widgetId Widget ID
     * @param context Context
     */
    public RuleEntity(long ruleId, String ruleKey, int widgetId, Context context) {
        this(ruleId, ruleKey, new int[] { widgetId }, context);
    }

    /**
     * Constructor
     * @param ruleId Rule ID
     * @param ruleKey Rule Key
     * @param widgetId Widget IDs
     * @param context Context
     */
    public RuleEntity(long ruleId, String ruleKey, int[] widgetId, Context context) {
        this.mRuleId = ruleId;
        this.mRuleKey = ruleKey;
        this.mWidgetId = widgetId;
        this.mContext = context;
    }

    /**
     * Constructor
     * @param intent Parse all the info from this intent
     * @param context Context
     */
    public RuleEntity(Intent intent, Context context) {
        this(intent.getLongExtra(RuleTable.Columns._ID, Constants.INVALID_KEY), intent
            .getStringExtra(RuleTable.Columns.KEY), intent.getIntExtra(Constants.EXTRA_RESPONSE_ID,
            Constants.INVALID_KEY), context);
        this.mRuleName = intent.getStringExtra(RuleTable.Columns.NAME);
        this.mRuleIcon = intent.getStringExtra(RuleTable.Columns.ICON);
    }

    /** Constructor
     * @param intent
     */
    public RuleEntity(Intent intent) {
        this(intent, null);
    }

    /**
     * @return the ruleId
     */
    public long getRuleId() {
        return mRuleId;
    }

    /**
     * @return the ruleKey
     */
    public String getRuleKey() {
        return mRuleKey;
    }

    /**
     * @return the widgetId
     */
    public int[] getWidgetIds() {
        return mWidgetId;
    }

    /**
     * @return the mContext
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * @param mWidgetId the mWidgetId to set
     */
    public void setWidgetId(int[] mWidgetId) {
        this.mWidgetId = mWidgetId;
    }

    /**
     * @return the mRuleName
     */
    public String getRuleName() {
        return mRuleName;
    }

    /**
     * @return the mRuleIcon
     */
    public String getRuleIcon() {
        return mRuleIcon;
    }

    /** hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mRuleKey == null) ? 0 : mRuleKey.hashCode());
        return result;
    }

}
