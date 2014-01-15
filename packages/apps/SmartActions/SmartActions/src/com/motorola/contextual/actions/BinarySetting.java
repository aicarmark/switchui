/*
 * @(#)BinarySetting.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/17  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.motorola.contextual.smartrules.R;

/**
 * Base class for all binary(On/Off) stateful actions <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Provides the default implementation for StatefulAction methods
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public abstract class BinarySetting extends StatefulAction implements Constants {

    protected boolean mOldState;
    protected boolean mState;
    protected String mActionKey;
    protected String mBroadcastAction;

    public String getState(Context context) {
        return Boolean.toString(mState);
    }

    public String getSettingString(Context context) {
        return getUserString(context, mState);
    }

    public String getDefaultSetting(Context context) {
        return getConfig(mOldState);
    }

    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    public void registerForSettingChanges(Context context) {
        return;
    }

    public void deregisterFromSettingChanges(Context context) {
        return;
    }

    /**
     * Returns user readable setting string for given state
     *
     * @param context Caller context
     * @param state Action state
     * @return User readable setting string
     */
    public static String getUserString(Context context, boolean state) {
        return (state) ? context.getString(R.string.on) : context.getString(R.string.off);
    }

    public String[] getSettingToObserve() {
        return null;
    }

    public String getActionKey() {
        return mActionKey;
    }

    public String getBroadcastAction() {
        return mBroadcastAction;
    }

    public Uri getUriForSetting(String setting) {
        return null;
    }

    public String getUpdatedConfig(Context context, Intent configIntent) {
        boolean state = configIntent.getBooleanExtra(EXTRA_STATE, true);
        return getConfig(state);
    }

    public List<String> getConfigList(Context context) {
        ArrayList<String> configs = new ArrayList<String>();
        configs.add(getConfig(true));
        configs.add(getConfig(false));
        return configs;
    }

    public List<String> getDescriptionList(Context context) {
        ArrayList<String> descriptions = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        String actionName = getActionString(context);
        builder.append(actionName).append(SPACE)
                .append(context.getString(R.string.on));
        descriptions.add(builder.toString());
        builder = new StringBuilder();
        builder.append(actionName).append(SPACE)
                .append(context.getString(R.string.off));
        descriptions.add(builder.toString());
        return descriptions;
    }

    public String getDescription(Context context, Intent configIntent) {
        boolean state = (configIntent != null) ? configIntent.getBooleanExtra(EXTRA_STATE, false) : false;
        return (state) ? context.getString(R.string.on) : context.getString(R.string.off);
    }

    public boolean validateConfig(Intent configIntent) {
        return configIntent.hasExtra(EXTRA_STATE);
    }

    /** Returns the config based on the state
     *
     * @param state
     * @return config
     */
    public String getConfig(boolean state) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_STATE, state);
        return intent.toUri(0);
    }

}
