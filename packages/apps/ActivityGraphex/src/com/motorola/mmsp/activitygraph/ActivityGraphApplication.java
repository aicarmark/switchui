package com.motorola.mmsp.activitygraph;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

public class ActivityGraphApplication extends Application {
    public ActivityGraphModel mModel;
    private static final String TAG = "ActivityGraphApplication";
    @Override
    public void onCreate() {
        //VMRuntime.getRuntime().setMinimumHeapSize(4 * 1024 * 1024);
        Log.d(TAG, "onCreate");
        super.onCreate();
        
        mModel = ActivityGraphModel.getInstance();
        mModel.startLoader(this);
        
        Intent logObserverIntent = new Intent(this, ActivityGraphLogService.class);
        startService(logObserverIntent);
        
        // Register intent receivers        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mModel, filter); 
        
        Log.d(TAG, "onCreate finish");
        // Register for changes to the favorites
        //ContentResolver resolver = getContentResolver();
        //resolver.registerContentObserver(AppRank.Ranks.CONTENT_URI, true,
        //        mFavoritesObserver);
    }
    
    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d("ActivityGraphApplication", "onTerminate()");
        unregisterReceiver(mModel);
        mModel = null;
        
        //ContentResolver resolver = getContentResolver();
        //resolver.unregisterContentObserver(mFavoritesObserver);
    }
    
    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            //mModel.startLoader(ActivityGraphApplication.this);
        }
    };
    
    ActivityGraphModel getModel() {
        return mModel;
    }
}
