
package com.motorola.mmsp.rss.util;

import android.os.Process;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory {

    private int mCounter = 0;
    private final String mName;
    private final int mPriority;

    /**
     * Constructor that uses the default thread priority of
     * android.os.Process.THREAD_PRIORITY_BACKGROUND
     *
     * @param name
     */
    public MyThreadFactory(String name) {
        this(name, android.os.Process.THREAD_PRIORITY_BACKGROUND);
    }

    /**
     * Constructor for the thread factory that specifies the priority
     * of the threads.
     *
     * @param name
     * @param priority The android.os.Process priority
     */
    public MyThreadFactory(String name, int priority) {
        mName = name;
        mPriority = priority;
    }

    public Thread newThread(Runnable runnable) {
        return new PooledThread(runnable, mName + "-" + mCounter++, mPriority);
    }

    public static final class PooledThread extends Thread {

        private final int mPriority;

        /**
         * Constructs a BlurPooledThread.
         * @param name
         * @param priority The priority to run the thread at. The value supplied must be from
         * {@link android.os.Process} and not from java.lang.Thread.
         */
        PooledThread(Runnable runnable, String name, int priority) {
            super(runnable, name);
            mPriority = priority;
        }

        @Override
        public void run() {
            Process.setThreadPriority(mPriority);
            super.run();
        }
    }
}
