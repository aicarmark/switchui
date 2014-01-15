/*
 * @(#)AbstractDetailComposer.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2011/03/16 NA		          Initial version
 *
 */
package  com.motorola.contextual.smartprofile;

import android.content.Context;
import com.motorola.contextual.smartprofile.Constants;


/**
 * This is an abstract class which helps to compose details of the publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * This class helps to compose details of publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */


public abstract class AbstractDetailComposer implements  Constants
{
    @SuppressWarnings("unused")
	private final static String LOG_TAG = AbstractDetailComposer.class.getSimpleName();

    /**
     * The method gets the description correspoding to the config passed in
     * @param context
     * @param config
     * @return description
     */
    public String getDescription(Context context, String config) {
        return null;

    }

    /**
     * The method gets config items XML
     * @param context
     * @return config item XML
     */
    public  String getConfigItems(Context context) {
        return null;
    }

    /**
     * The method gets name of publisher
     * @param context
     * @return name of the condition publisher
     */
    public  String getName(Context context) {
        return null;
    }

    /**
     * This method gets current state for condition publisher which
     * is used to be sent in "subscribe/refresh" response.
     * @param context
     * @param config - configuration for which current state is requested
     * @return state
     */
    public  String getCurrentState(Context context, String config) {
        return FALSE;
    }

    /**
     * This method gets updated config for condition publisher which
     * is used to be sent in "refresh" response.
     * @param context
     * @param config - current config
     * @return updated config
     */
    public  String getUpdatedConfig(Context context, String config) {
        return null;
    }
    
    /**
     * This method validates config for condition publisher which
     * is used to be sent in "refresh" response.
     * @param context
     * @param config - current config
     * @return true/false - valid/invalid
     */
    public  boolean validateConfig(Context context, String config) {
        return false;
    }
}
