/*
 * @(#)Sync.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/04/23  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;

/**
 * This class implements the FrameworkAction for Background sync <code><pre>
 * CLASS:
 *     implements FrameworkAction
 *
 * RESPONSIBILITIES:
 *     If Smart Actions FW is old then this class is used to handle commands
 *     that cannot be handled by old framework
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class Sync extends FrameworkAction {
    public static final String SYNC_ACTION_KEY = "com.motorola.contextual.actions.Sync";
    
    public interface State {
        final String ON = "On";
        final String OFF = "Off";
    }
    
    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.sync);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        boolean state = (configIntent != null) ? configIntent.getBooleanExtra(EXTRA_STATE, false) : false;
        return (state) ? context.getString(R.string.enable) : context.getString(R.string.disable);
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        boolean state = configIntent.getBooleanExtra(EXTRA_STATE, true);
        return getConfig(state);
    }

    @Override
    public List<String> getConfigList(Context context) {
        ArrayList<String> configs = new ArrayList<String>();
        configs.add(getConfig(true));
        configs.add(getConfig(false));
        return configs;
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        ArrayList<String> descriptions = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        String actionName = getActionString(context);
        builder.append(actionName).append(SPACE)
        .append(context.getString(R.string.enable));
        descriptions.add(builder.toString());
        builder = new StringBuilder();
        builder.append(actionName).append(SPACE)
        .append(context.getString(R.string.disable));
        descriptions.add(builder.toString());
        return descriptions;
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.hasExtra(EXTRA_STATE);
    }

    /** Returns the state based on the item that was checked
     *
     * @param state
     * @return config string
     */
    public String getConfig(boolean state) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_STATE, state);
        return intent.toUri(0);
    }

}
