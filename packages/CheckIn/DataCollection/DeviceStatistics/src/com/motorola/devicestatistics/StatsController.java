/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: StatsCollector.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 20, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import android.content.Context;
import android.util.Log;

import com.motorola.devicestatistics.StatsTrigger.ITriggerCb;

/**
 * @author bluremployee
 *
 */
public class StatsController {
    
    private final static boolean DUMP = true;
    private final static String TAG = "StatsController";

    StatsTrigger mTrigger;
    StatsCollector mCollector;
    static StatsController sMe;
    
    private StatsController(Context context) {
        mCollector = new StatsCollector(context);
        mTrigger = new StatsTrigger(new ITriggerCb() {

            public void onFire(int type) {
                if(DUMP) Log.v(TAG, "onFire: for type " + type);
                mCollector.schedule(type, false, null);
            }
            
        });
    }
    
    public void forceTrigger(int type, Runnable cb) {
        if(DUMP) Log.v(TAG, "force trigger for type " + type);
        mCollector.schedule(type, true, cb);
    }

    public static StatsTrigger getTrigger() {
        return sMe != null ? sMe.mTrigger : null;
    }

    public static StatsController getController(Context context) {
        if(sMe == null) {
            sMe = new StatsController(context);
        }
        return sMe;
    }

}

