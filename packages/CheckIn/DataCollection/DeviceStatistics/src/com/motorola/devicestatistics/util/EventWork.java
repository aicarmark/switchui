/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: Work.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Dec 31, 2010        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics.util;

import android.content.Intent;

/**
 * @author bluremployee
 *
 */
public abstract class EventWork implements Runnable {

    public Intent mIntent;
    public long mTime;
    
    public EventWork(Intent intent, long time) {
        mIntent = intent;
        mTime = time;
    }
    
    public EventWork(Intent intent) {
        mIntent = intent;
        mTime = System.currentTimeMillis();
    }
    
    // Runnable 
    public void run() {
        processWork();
    }
    
    public abstract void processWork();
}

