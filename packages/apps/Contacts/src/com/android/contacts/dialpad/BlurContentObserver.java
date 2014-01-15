package com.android.contacts.dialpad;

import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

public abstract class BlurContentObserver extends ContentObserver {
    private static final String TAG = "BlurContentObserver";
    // config timer to filter too much notifications
    private static final int FILTER_TIMER = 3000;  // 3 secs
    private Runnable r = null;
    private Handler mHandler;

    public BlurContentObserver(Handler handler) {
        super(handler);
        mHandler = handler; //save hander reference
    }

    private final class MyRunnable implements Runnable {
        private boolean mSelf;
        public MyRunnable(boolean self) {
            mSelf = self;
        }

        public void run() {
            BlurContentObserver.this.onChangeRealNeeded(mSelf);
        }
    }

    // define this API as final, in case it will be overwritten
    public final void onChange(boolean selfChange) {
        if (mHandler != null) {
            if (r != null) {
                Log.v (TAG, "notification is filtered, handler = " + mHandler);
                mHandler.removeCallbacks(r); //remove previously enqueued notification
            }
            r = new MyRunnable(selfChange);
            mHandler.postDelayed(r, FILTER_TIMER);
        } else {
            onChangeRealNeeded(selfChange);
        }
    }

    // define this API as abstract, it always needs to be overwritten
    abstract public void onChangeRealNeeded(boolean selfChange);
}
