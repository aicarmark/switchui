/**
 * FILE: PluginHost.java
 *
 * DESC: The basic plugin host.
 * 
 */

package com.motorola.mmsp.plugin.base;
import android.content.Context;
import android.content.pm.ComponentInfo;

import java.util.ArrayList;
import java.util.HashMap;
import android.util.Log;


public class PluginHost implements PluginScanner.PluginScannerCallback {

    public static final String TAG = "Plugin";
    public static final boolean LOGD = true;
    public static boolean bIsDynamic = false;
    
    /**
     * Plugin meta-data
     */
    public static final String PLUGIN_META_DATA = "com.motorola.mmsp.plugin";

    /**
     * Supported host app category
     */
    public static final String PLUGIN_HOST_APP_CATEGORY_HOME = "com.motorola.mmsp.plugin.HOSTHOME";
    public static final String PLUGIN_HOST_APP_CATEGORY_DASHBOARD = "com.motorola.mmsp.plugin.HOSTDASHBOARD";

    /**
     * supported plugin category
     */
    public static final String PLUGIN_CATEGORY_WIDGET = "com.motorola.mmsp.plugin.WIDGET";
    /**
     * supported plugin theme category
     */
    public static final String PLUGIN_CATEGORY_THEME = "com.motorola.mmsp.plugin.THEME";

    /**
     * plugin info in meta-data
     */
    public static final String PLUGIN_INFO = "plugin-info";

    /**
     * Record how many plugins has been installed.
     * These plugins will be scanned out by plugin scanner.
     */
    protected HashMap<String,PluginObject> mInstalled = null;

    /**
     * Plugin scanner.
     */
    protected PluginScanner mScanner = null;

    protected PluginHostCallback mHostCB = null;

    /**
     * Plubin host callback, the abstracted interface which must be implemented
     * by each host app, who wants to support plugin.
     */
    public interface PluginHostCallback {
        /**
         * Drop a heavy task into background thread of host app.
         */
        public void enqueueBGTask(Runnable r);

        /** 
         * Drop a little task(usally ui callback) into foreground thread of host app.
         */
        public void enqueueFGTask(Runnable r);

        /**
         * Usally plugin widget model is storing just data, which would be always staying
         * in memory of host app.
         */
        public Context getModelContext();

        /**
         * For any view of plugin widget must be created in host app activity context.
         * As when some system configuration has been changed, the context will be 
         * re-created by framework automatically.
         */
        public Context getViewContext();
        
        /**
         * called when scan started
         */
        public void onScanStarted();
        
        /**
         * called when scan finished
         */
        public void onScanFinished();
        
        /**
         * @param widgetId
         * @return true if the hostView which widget id is widgetId is in current screen
         */
        public boolean isCurrentPanel(int widgetId);
    }

    /**
     * The constructor of plugin host.
     * @param hostApp Type of HostAPP : defined in PluginHost
     * @param plugin  Type of Plug-in : defined in PluginHost
     * @param cb
     */
    protected PluginHost(String hostApp, String plugin, PluginHostCallback cb) {
        mInstalled = new HashMap<String,PluginObject>();
        mScanner = PluginFactory.makeScanner(hostApp, plugin);
        mHostCB = cb;
    }
    
    /**
     * @param isAnsyc 
     * @return void
     */
    public void scanPlugins(boolean isAnsyc) { 
    	scanPlugins(isAnsyc, false);
    }
    
    /**
     * @param isAnsyc
     * @param isDynamic
     */
    public void scanPlugins(boolean isAnsyc, boolean isDynamic) { 
    	bIsDynamic = isDynamic;
        if (mScanner != null) {
            mScanner.setAysncScan(isAnsyc);
            if (isAnsyc && mHostCB != null) {
                mScanner.addScanCallback(this);
                mScanner.setHostAppCallback(mHostCB);
                mScanner.notifyScanStarted();
                mHostCB.enqueueBGTask((Runnable)mScanner);
            } 
            if (!isAnsyc) {
                mScanner.addScanCallback(this);
                mScanner.setHostAppCallback(mHostCB);
                //scan & load start
                long begin = System.currentTimeMillis();
                mScanner.notifyScanStarted();
                mScanner.scan();
                mScanner.notifyScanFinished();
                //scan & load finish
                long end = System.currentTimeMillis();
                if (LOGD) Log.d(TAG, "scan & load total cost time: " + (end-begin) + "ms");
                
            }
        }
    }

    
    /**
     * @param isAnsyc
     * @param cb
     * @return void
     */
    public void scanPlugins(boolean isAnsyc, PluginScanner.PluginScannerCallback cb, boolean isDynamic) {
        if (mScanner != null) {
            mScanner.addScanCallback(cb);
            scanPlugins(isAnsyc, isDynamic);
        }
    }

    /** ------- implement scanner callback begin */
    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginScanner.PluginScannerCallback#onScanStarted()
     */
    public void onScanStarted() {
    	mHostCB.onScanStarted();
    }
    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginScanner.PluginScannerCallback#onScanOne(android.content.pm.ComponentInfo, com.motorola.mmsp.plugin.base.PluginObject)
     */
    public void onScanOne(ComponentInfo ci,PluginObject po) {
        if (LOGD) Log.d(TAG, "PluginHost onScanOne po:" + po+",ci:"+ci);
        if (ci != null && po != null) {
            mInstalled.put(ci.name, po);
        }
    }
    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginScanner.PluginScannerCallback#onScanCancelled()
     */
    public void onScanCancelled() {}
    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginScanner.PluginScannerCallback#onScanFinished()
     */
    public void onScanFinished() {
    	mHostCB.onScanFinished();
    }
    /** ------- implement scanner callback end */
}

