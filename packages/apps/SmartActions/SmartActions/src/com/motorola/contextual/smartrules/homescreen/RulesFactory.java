/*
 * @(#)RulesFactory.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/14/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import java.util.HashMap;

import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/**
 * Maintains one {@link RuleManager} per rule id. This can be used if we have of take care to
 * multiple rules and need to reuse the {@link RuleManager} whenever possible. Say we have 2
 * instances of rule x and 1 instance of rule y, only one rule manager is allocated to manage both
 * instances of x and one for y. <code><pre>
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
 *
 * @see RuleManager
 */
public class RulesFactory {
    private static final String          TAG           = RulesFactory.class.getSimpleName();
    private HashMap<String, RuleManager> mRuleManagers = new HashMap<String, RuleManager>();

    public RulesFactory() {}

    /**
     * Get the allocated {@link RuleManager}. If a rule manager is not allocated yet, then a
     * new one is allocated.
     *
     * @param ruleKey Rule key
     * @return Rule manager
     */
    public RuleManager get(String ruleKey) {
        if (!mRuleManagers.containsKey(ruleKey)) {
            mRuleManagers.put(ruleKey, new RuleManager());
            if (Constants.LOG_DEBUG) Log.d(TAG, "New rule manager added for " + ruleKey);
        }
        return mRuleManagers.get(ruleKey);
    }

    /**
     * Remove the allocated rule manager
     *
     * @param ruleKey rule key
     */
    public void removeRule(String ruleKey) {
        mRuleManagers.remove(ruleKey);
    }

    /**
     * Clear all allocated rule managers
     */
    public void clearCache() {
        mRuleManagers.clear();
    }
}
