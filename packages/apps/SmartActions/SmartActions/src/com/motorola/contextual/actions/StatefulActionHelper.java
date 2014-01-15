/*
 * @(#)StatefulActionHelper.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/28  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

/**
 * This class hosts a number of helper functions used by Stateful actions.
 * <code><pre>
 * CLASS:
 *
 *
 * RESPONSIBILITIES:
 *      This class is entirely all static, nothing instance-based.
 *
 * COLABORATORS:
 *      None
 *
 * USAGE:
 *  See individual routines.
 * </pre></code>
 */

public class StatefulActionHelper implements Constants {

    private static final String TAG = TAG_PREFIX + StatefulActionHelper.class.getSimpleName();

    /**
     * Sends setting change intent for stateful actions
     *
     * @param context
     * @param actionKey
     * @param actionString
     * @param settingString
     */
    public static void sendSettingsChange(Context context, String actionKey, String stateDescription) {
        Intent settingsIntent = new Intent(ACTION_PUBLISHER_EVENT);
        settingsIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_STATE_CHANGE);
        settingsIntent.putExtra(EXTRA_PUBLISHER_KEY, actionKey);
        settingsIntent.putExtra(EXTRA_STATE_DESCRIPTION, stateDescription);
        if (LOG_INFO) {
            Log.i(TAG, "sendSettingsChange broadcasting intent = " + settingsIntent.toUri(0));
        }
        context.sendBroadcast(settingsIntent, PERM_ACTION_PUBLISHER_ADMIN);
	}

    /**
     * Register for Settings changes - Content observer backed
     *
     * @param context
     * @param actionKey
     */
    public static void registerForSettingChanges(Context context, String actionKey) {

        Intent serviceIntent = new Intent(context, SettingsService.class);
        serviceIntent.putExtra(EXTRA_REGISTER, true);
        serviceIntent.putExtra(EXTRA_ACTION_KEY, actionKey);
        context.startService(serviceIntent);

    }

    /**
     * Deregister from Settings changes - Content observer backed
     *
     * @param context
     * @param actionKey
     */
    public static void deregisterFromSettingChanges(Context context, String actionKey) {
        Intent serviceIntent = new Intent(context, SettingsService.class);
        serviceIntent.putExtra(EXTRA_REGISTER, false);
        serviceIntent.putExtra(EXTRA_ACTION_KEY, actionKey);
        context.startService(serviceIntent);
    }

    /** Returns the list of stateful actions present in this package. If there are none,
     * then the list is empty.
     *
     * @param context
     * @return
     */
    private static List<String> queryStatefulActionsFromPackageManager(Context context) {
        List<String> list = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(APP_PACKAGE, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

            for (ActivityInfo aInfo : pInfo.activities) {
                if (aInfo.metaData != null) {
                    String actionType = aInfo.metaData.getString(ACTION_TYPE);
                    String actionKey = aInfo.metaData.getString(PUBLISHER_KEY);
                    if (actionType != null && actionKey != null) {
                        if (actionType.equals(STATEFUL))
                            list.add(actionKey);
                    } else {
                        // this is not an error on single apk since in addition to action activities there are
                        // Smart profile and Smart Rules Activities
                    }
                }else {
                    // this is not an error on single apk since in addition to action activities there are
                    // Smart profile and Smart Rules Activities
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "package not found ? this code wouldn't execute if that was the case");
        }
        return list;
    }

    /** Get the actionKey corresponding to a setting broadcast action
     *
     * @param action
     * @return
     */
    public static StatefulAction convertBroadcastActionToActionInterface(Context context, String action) {

        List<String> list = queryStatefulActionsFromPackageManager(context);
        for (String key: list) {
            Action act = ActionHelper.instantiate(context, key);
            if (act == null || !(act instanceof StatefulAction)) {
                //Wrapper classes for FW actions are not StatefulActions
                continue;
            }
            StatefulAction sai = (StatefulAction)act;
            if (sai != null) {
                String bAction = sai.getBroadcastAction();
                if (bAction != null && bAction.equals(action)) {
                    return sai;
                } else if (bAction != null && bAction.contains(BROADCAST_ACTION_DELIMITER)) {
                    StringTokenizer tokenizer = new StringTokenizer(bAction, BROADCAST_ACTION_DELIMITER);
                    while (tokenizer.hasMoreTokens()) {
                        if (tokenizer.nextToken().equals(action))
                            return sai;
                    }
                }
            }
        }

        return null;
    }

    /**
    * Gets the Settings which are observed using ContentObservers
    * @param context
    * @return List of action keys for those settings which are observed using ContentObservers
    */
   public static List<String> getAllContentObserverBackedSettings(Context context) {

       List<String> list = queryStatefulActionsFromPackageManager(context);
       List<String> listActionKeys = new ArrayList<String>();
       for (String key: list) {
           StatefulAction sai = (StatefulAction)ActionHelper.instantiate(context, key);
           if (sai != null) {
        	   String[] setting = sai.getSettingToObserve();
        	   if(setting != null){
        		   for (String s: setting) {
        			   Uri uri = sai.getUriForSetting(s);
        			   if(uri != null){
        				   listActionKeys.add(sai.getActionKey());
        				   break;
        			   }
        		   }
        	   }
               }
       }
       return listActionKeys;
   }
}
