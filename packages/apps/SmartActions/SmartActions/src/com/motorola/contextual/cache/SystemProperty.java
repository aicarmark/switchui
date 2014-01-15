/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        10/19/2012   NA                1.0
 */
package com.motorola.contextual.cache;

import android.content.Context;
import android.provider.Settings;

import com.motorola.contextual.callback.Command;
import com.motorola.contextual.model.SystemPropTaskModel;
import com.motorola.contextual.task.ReadSystemPropTask;

/**
 * <code><pre>
 * CLASS:
 *  Cache of system properties. Cache only airplane mode for now.
 * 
 * RESPONSIBILITIES:
 * 
 * USAGE:
 *  
 * 
 * </pre></code>
 */
public class SystemProperty {
    private static boolean mAirplaneMode;

    private SystemProperty() {}
    
    /**
     * Read the airplane mode property in background.
     * @param ctx Context
     */
    public static void readAirplaneMode(Context ctx) {
        SystemPropTaskModel model = new SystemPropTaskModel(
                ctx.getApplicationContext(), new SetAirplaneModeCommand(),
                Settings.System.AIRPLANE_MODE_ON, Integer.class);
        new ReadSystemPropTask().execute(model);
    }
    
    private static class SetAirplaneModeCommand implements Command {

        public void execute(Object result) {
            SystemProperty.setAirplaneMode((Integer) result == 1);
        }

    }
    
    /**
     * @return the mAirPlaneMode
     */
    public static boolean isAirplaneMode() {
        return mAirplaneMode;
    }

    /**
     * @param state the mAirPlaneMode to set
     */
    public static synchronized void setAirplaneMode(boolean state) {
        SystemProperty.mAirplaneMode = state;
    }
}
