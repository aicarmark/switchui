/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.mmsp.motohomex;

import android.app.Application;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;

import com.motorola.mmsp.motohomex.R;

import java.lang.ref.WeakReference;
// Added by e13775 at 19 June 2012 for organize apps' group start
import com.motorola.mmsp.motohomex.apps.AppsModel;
// Added by e13775 at 19 June 2012 for organize apps' group end

/*Added by ncqp34 at Jul-17-2012 for group switch*/
import android.os.Build;
import android.util.Log;
/*ended by ncqp34*/

public class LauncherApplication extends Application {
    // Added by e13775 at 19 June 2012 for organize apps' group start
    private AppsModel mAppsModel;
    boolean mHasNavigationBar = false;
    // Added by e13775 at 19 June 2012 for organize apps' group end
    public LauncherModel mModel;
    public IconCache mIconCache;
    private static boolean sIsScreenLarge;
    private static float sScreenDensity;
    private static int sLongPressTimeout = 300;
    private static final String sSharedPreferencesKey = "com.motorola.mmsp.motohomex.prefs";
    WeakReference<LauncherProvider> mLauncherProvider;
    /*Added by ncqp34 at Mar-28 for fake app*/
    public FakeAppModel mFakeModel;
    /*ended by ncqp34*/
    /*2012-6-18, add by bvq783 for switchui-1547*/
    private static boolean mSent = false;
    /*2012-6-18, add end*/

    /*2012-01-05, DJHV83 added for Data Switch*/
    private boolean mDataSwitch = false;
    /*DJHV83 end*/
    //added by amt_wangpeipei 2012/07/11 for switchui-2121 begin
    private Launcher mLauncher;
    //added by amt_wangpeipei 2012/07/11 for switchui-2121 end

    /*Added by ncqp34 at Jul-17-2012 for group switch*/
    public static boolean mGroupEnable = true;
    /*ended by ncqp34*/
    @Override
    public void onCreate() {
        super.onCreate();

        // set sIsScreenXLarge and sScreenDensity *before* creating icon cache
        sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        sScreenDensity = getResources().getDisplayMetrics().density;

        mIconCache = new IconCache(this);
        mModel = new LauncherModel(this, mIconCache);
        /*Added by ncqp34 at Mar-28 for fake app*/
        mFakeModel= new FakeAppModel(this);
        mHasNavigationBar = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        /*ended by ncqp34*/

        // Register intent receivers
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        registerReceiver(mModel, filter);

        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);
        /*2012-7-12, add by bvq783*/
        // wag044 02/13/2012 : ANR on Launcher.onCreate
        // Initialize apps model in background to avoid delaying
        // main thread when it is necessary
        new Thread() {
            public void run() {
                try {
                    getAppsModel();
                } catch (Exception e) {
                    //Log.w(TAG, "Exception when getting apps model " + e);
                }
            }
        }.start();
        /*2012-7-12, add end*/

	/*Added by ncqp34 at Jul-17-2012 for group switch*/
	Log.d("Launcher","MODEL =" + Build.MODEL);
       /*2012-8-15, modify by bvq783 for switchui-2548 as TD may change the model name*/
	//mGroupEnable = !(Build.MODEL.trim().equals("MT781"));//TD model name
       String name = Build.MODEL.trim();
       if (name != null) {
           name = name.substring(0, 2);
           Log.d("Launcher", "the first two string is:"+name);
           mGroupEnable = !(name.equals("MT")); //TD model name
       }
       /*2012-8-15, modify end*/
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        /*2012-8-3, add by bvq783 for plugin*/
        if (mModel != null && mModel.getPluginHost() != null)
            mModel.getPluginHost().onModelDestroy();
        /*2012-8-3, add end*/
        unregisterReceiver(mModel);

        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
    	//added by amt_wangpeipei 2012/07/11 for switchui-2121 begin
    	mLauncher = launcher;
    	//added by amt_wangpeipei 2012/07/11 for switchui-2121 end
        if (mModel != null)
            mModel.initialize(launcher);
        return mModel;
    }
    
    /**
     * added by amt_wangpeipei 2012/07/11 for switchui-2121
     * @return Launcher
     */
    public Launcher getLauncher(){
    	return mLauncher;
    }

    // Modified by e13775 at 19 June 2012 for organize apps' group
    public IconCache getIconCache() {
        return mIconCache;
    }

    LauncherModel getModel() {
        return mModel;
    }

    /*Added by ncqp34 at Mar-28 for fake app*/
    public FakeAppModel getFakeModel() {
        return mFakeModel;
    }
    /*ended by ncqp34*/

    void setLauncherProvider(LauncherProvider provider) {
        mLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    LauncherProvider getLauncherProvider() {
        return mLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return sSharedPreferencesKey;
    }

    public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }

    // Added by e13775 at 19 June 2012 for organize apps' group start
    synchronized public AppsModel getAppsModel() {
        if (mAppsModel == null) {
            mAppsModel = new AppsModel(this);
        }
        return mAppsModel;
    }
    public boolean hasNavigationBar(){
        //Log.d(TAG,"hasNavigationBar ="+mHasNavigationBar);
        //return mHasNavigationBar;
    	return true;
    }
    // Added by e13775 at 19 June 2012 for organize apps' group end

    public static int getLongPressTimeout() {
        return sLongPressTimeout;
    }

    /*2012-6-18, add by bvq783 for switchui-1547*/
    public static boolean isEnterHomeSent() {
        return mSent;
    }

    public static void setEnterHomeSent(boolean b) {
        mSent = b;
    }
    /*2012-6-18, add end*/

    /*2012-01-05, DJHV83 added for Data Switch*/
    public void setDataSwitchEnable(boolean bEnable){
        mDataSwitch = bEnable;
    }

    public boolean getDataSwitchEnable(){
        return mDataSwitch;
    }
    /*DJHV83 end*/
}
