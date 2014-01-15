package com.motorola.contacts.group;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;


/**
 * Only have a single Looper (and its associated thread for any app).
 * It will get destroyed when the app goes out of scope. All Handlers
 * in the app should use this looper.
 *
 * The Looper's associated thread will have a priority of
 * {@link Process#THREAD_PRIORITY_BACKGROUND}.
 */
public class MyLooper {

    private static android.os.Looper sLooper;

    /**
     * Get an instance to MyLooper.
     * @return an instance to the singleton looper.
     */
    public synchronized static Looper singleton() {
        if (sLooper == null) {
            HandlerThread thread = new HandlerThread(MyLooper.class.getName(),
                    Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            sLooper = thread.getLooper();
        }
        return sLooper;
    }

    public synchronized static Looper singleton(int priority) {
        if (sLooper == null) {
            HandlerThread thread = new HandlerThread(MyLooper.class.getName(), priority);
            thread.start();
            sLooper = thread.getLooper();
        }
        return sLooper;
    }
}
