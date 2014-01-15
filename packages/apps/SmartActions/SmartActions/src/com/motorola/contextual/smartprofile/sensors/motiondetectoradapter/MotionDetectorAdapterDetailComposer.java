/*
 * @(#)MotionDetectorAdapterDetailComposer.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2011/03/16 NA                Initial version
 *
 */
package  com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

/**
 * This class is  helps to compose details of motion detector adapter
 *
 *
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * This class composes details of the publisher.
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */


public final class MotionDetectorAdapterDetailComposer  implements  Constants, MotionConstants {
    private final static String LOG_TAG = MotionDetectorAdapterDetailComposer.class.getSimpleName();
    private final static String EXTRA_PUBLISH = "com.motorola.contextual.PUBLISH";
    private final static String EXTRA_TIMESTAMP = "com.motorola.contextual.TIMESTAMP";
    private final static String MDM_PACKAGE = "com.motorola.mdmmovenotice";
    private final static String MDM_SERVICE = "com.motorola.mdmmovenotice.MdmMoveNotice";
    private final static String ACTION_STILL = "com.motorola.intent.action.STILL";
    private final static String ACTION_GET_CONFIG = "com.motorola.smartactions.intent.action.GET_CONFIG";
    private final static String CATEGORY_CP = "com.motorola.smartactions.intent.category.CONDITION_PUBLISHER";
    private final static String MOTION_PACKAGE = "com.motorola.contextual.Motion";


    /**
     * Gets description for given config
     *
     * @param context
     * @param config
     * @return description
     */
    public static final String getDescription(Context context, String config) {
        return context.getString(R.string.motion_desc);
    }


    /**
     * Gets config items string for Motion Detector
     *
     * @param context
     * @return config items string
     */
    public  final String getConfigItems(Context context) {

        String supportedMotionDesc[] = new String[] {
            context.getString(R.string.motion_desc)
        };

        String supportedMotionLevels[] = new String[] {STILL};

        return Util.generateXMLStringForList(context, supportedMotionLevels, supportedMotionDesc);

    }

    /**
     * Gets name of publisher
     *
     * @param context
     * @return name
     */
    public final String getName(Context context) {
        return(context.getString(R.string.motion));
    }

    /**
     * Gets current state of Motion Detector
     *
     * @param context
     * @param config
     * @return state
     */
    public static final String getCurrentState(Context context, String config) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STILL);
        Intent intent = context.registerReceiver(null, filter);

        String status = (intent != null) ? TRUE : FALSE;

        if(LOG_INFO) Log.i(LOG_TAG, "getCurrentState - state : " +   " : " + config + " : " +  status);
        return status;
    }

    /** sends a broadcast intent to the motion detector
      * @param context - context
      * @param pubKey -  publisher key
      * @param publish - start or stop listening to the state changes
      * @param timeStamp - current timestamp
      */
    public static void sendBroadcastToPublisher(final Context context, final String pubKey,
            final String publish, final long timeStamp) {
        Intent intent = new Intent();
        intent.setAction(pubKey);
        intent.putExtra(EXTRA_PUBLISH, publish);
        intent.putExtra(EXTRA_TIMESTAMP, timeStamp);
        intent.setPackage(MDM_PACKAGE);
        context.sendBroadcast(intent);
    }

    /**
     * Method to check if the command needs to be converted to the old motion detector interface.
     * If the MDM service is not present it need not be converted
     * @param context
     * @return true if incoming command needs to be converted to new format, false otherwise
     */
    public static boolean isMDMPresent(Context context) {
        if (LOG_DEBUG) Log.d(LOG_TAG, "isConversionNeeded");

        PackageManager pm = context.getPackageManager();
        try {
            pm.getServiceInfo(new ComponentName(MDM_PACKAGE, MDM_SERVICE), PackageManager.GET_META_DATA);
            if (LOG_INFO) Log.i(LOG_TAG, "isConversionNeeded - true");
            return true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method to check if Motion Detector activity has an Activity which is defined with
     * New arch category
     * @param context
     * @return true / false
     */
    public static boolean isNewArchMD(Context context) {
        PackageManager pm = context.getPackageManager();

        Intent newArchIntent = new Intent(ACTION_GET_CONFIG);
        newArchIntent.addCategory(CATEGORY_CP);
        List<ResolveInfo> list = pm.queryIntentActivities(newArchIntent, PackageManager.GET_META_DATA);

        boolean status = false;

        int size = list.size();
        if((list != null) && (size != 0)) {
            for(int i = 0; i < size; i++) {
                if(LOG_DEBUG) Log.d(LOG_TAG, "isNewArchMD : " + list.get(i).activityInfo.name);
                if(list.get(i).activityInfo.packageName.equals(MOTION_PACKAGE)) {
                    status = true;
                    break;
                }
            }
        }


        if(LOG_INFO) Log.i(LOG_TAG, "isNewArchMD : " + status) ;
        return status;
    }

    /** Gets updated config for old config
     * @param context
     * @param config
     * @return updated config
     */
    public static final String getUpdatedConfig(Context context, String config) {

        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : old - " + config);
        if(config.contains(OLD_CONFIG_PREFIX)) {
            String oldConfig = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            if(oldConfig.equals(context.getString(R.string.off))) {
                config = STILL;
            } else {
                config = null;
            }
        } else {
            if(!config.equals(MotionConstants.STILL)) {
                config = null;
            }
        }
        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : new - "  + config);
        return config;
    }
}


