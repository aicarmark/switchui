/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * dfb746                      16/04/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

/**
 * asynchronous work which can be dispatch to the worker thread
 * 
 * @author dfb746
 */
public abstract class Job {
    private final static String TAG = "Job";

    /**
     * used for notifying the job creator
     */
    private JobDoneListener mListener = null;

    public interface JobDoneListener {
        void onJobDone();
    }

    public Job(JobDoneListener listener) {
        mListener = listener;
    }

    /**
     * do the real job in this funtion
     */
    public void doJob() {
        Log.i(TAG, "enter doJob func");
    }

    /**
     * call listener when job finished
     */
    public void jobDone() {
        if (mListener != null) {
            mListener.onJobDone();
        }
    }
}
