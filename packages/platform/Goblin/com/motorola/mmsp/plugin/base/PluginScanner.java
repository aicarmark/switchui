/**
 * FILE: PluginScanner.java
 *
 * DESC: The scanner is to scan out all plugins according to given host app and 
 * plugin categoty.
 * 
 */

package com.motorola.mmsp.plugin.base;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.content.Context;
import android.content.ComponentName;
import android.util.Xml;
import android.util.AttributeSet;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.List;

public class PluginScanner implements Runnable {
    // The category of host app which is to identify which app wants plugins.
    protected String mHostAppCategory;
    // The category of plugin.
    protected String mPluginCategory;

    // The callback implement.
    protected ArrayList<PluginScannerCallback> mCallbacks;

    // The host app callback.
    protected PluginHost.PluginHostCallback mHostAppCB;

    private boolean mAnsycScan = false;

    /**
     * The scanner callback which is to notify scanning status.
     */
    public interface PluginScannerCallback {
        public void onScanStarted();
        public void onScanOne(ComponentInfo ci,PluginObject po);
        public void onScanCancelled();
        public void onScanFinished();
    }

    /**
     * @param hostApp
     * @param plugin
     */
    public PluginScanner(String hostApp, String plugin) {
        mHostAppCategory = hostApp;
        mPluginCategory  = plugin;
        mCallbacks = new ArrayList<PluginScannerCallback>();
    }

    // Notify all callbacks the scan has been started.
    protected void notifyScanStarted() {
        final int count = mCallbacks.size();
        for (int i=0; i<count; i++) {
            mCallbacks.get(i).onScanStarted();
        }
    }

    // Notify all callbacks the scan has met one plugin.
    protected void notifyScanOne(ComponentInfo ci,PluginObject po) {
        if (po != null) {
            final int count = mCallbacks.size();
            for (int i=0; i<count; i++) {
                mCallbacks.get(i).onScanOne(ci,po);
            }
        }
    }

    // Notify all callbacks the scan has been cancelled.
    protected void notifyScanCancelled() {
        final int count = mCallbacks.size();
        for (int i=0; i<count; i++) {
            mCallbacks.get(i).onScanCancelled();
        }
        // reset scanner
        reset();
    }

    // Notify all callbacks the scan has been finished.
    protected void notifyScanFinished() {
        final int count = mCallbacks.size();
        for (int i=0; i<count; i++) {
            mCallbacks.get(i).onScanFinished();
        }
        // reset scanner
        reset();
    }

    private void reset() {
        // reset the host app callback.
        //setHostAppCallback(null);
        // remove all callbacks as scanner has been finished.
        mCallbacks.clear();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        scan();

        if (mHostAppCB != null) {
            mHostAppCB.enqueueFGTask(new Runnable() {
                public void run() {
                    notifyScanFinished();
                }
            });
        }
    }

    /**
     * @param ci
     * @return PluginObject
     */
    public PluginObject parseOne(ComponentInfo ci, boolean real) {
    	if(ci == null){
    		return null;
    	}
        if (PluginHost.LOGD) Log.d(PluginHost.TAG, "PluginScan parseOne appInfo:" + ci.applicationInfo
            + ", sourceDir:" + ci.applicationInfo.sourceDir + ", psd:" + ci.applicationInfo.publicSourceDir
            + ", metaData:" + ci.metaData + ", className:" + ci.name);

        XmlResourceParser parser = null;
        PluginObject obj = null;
        try {
            Context pkgContext = mHostAppCB.getModelContext().createPackageContext(
                ci.packageName, Context.CONTEXT_RESTRICTED);
            PackageManager pkgManager = pkgContext.getPackageManager();

            parser = ci.loadXmlMetaData(pkgManager, PluginHost.PLUGIN_META_DATA);
            if (parser == null) {
                Log.d(PluginHost.TAG, "PluginScan no meta-data");
                return null;
            }

            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT &&
                (type != XmlPullParser.START_TAG)) {
            }

            String nodeName = parser.getName();
            if (!PluginHost.PLUGIN_INFO.equals(nodeName)) {
                throw new XmlPullParserException(
                    "Meta-data does not start with plugin-info tag");
            }

            //obj = parseOne(ci, attrs);
            PluginInfo info = (PluginInfo) PluginFactory.makePluginInfo(mHostAppCategory, mPluginCategory);
            info.cn = new ComponentName(ci.packageName, ci.name);
            info.mSourceDir = ci.applicationInfo.sourceDir != null ? 
                ci.applicationInfo.sourceDir : ci.applicationInfo.publicSourceDir;
            if (PluginHost.LOGD) Log.d(PluginHost.TAG, "PluginScan parseOne cn:" + info.cn + 
                ", sourceDir:" + info.mSourceDir);
            info.onParse(ci, attrs, pkgContext);
            
            

            // new object
            obj = (PluginObject) PluginFactory.makePluginObject(info, mHostAppCB, mPluginCategory, real);

        } catch (Exception e) {
            Log.e(PluginHost.TAG, "PluginScan parse one met exception:" + e);
        } finally {
            if (parser != null) parser.close();
        }

        return obj;
    }

    /**
     * scan plug in
     */
    public void scan() {
        if (PluginHost.LOGD) Log.d(PluginHost.TAG, "PluginScan mHostAppCB:" + mHostAppCB);
        if (mHostAppCB != null) {
            //scan  start
            long scan_begin = System.currentTimeMillis();
            PackageManager pm = mHostAppCB.getModelContext().getPackageManager();
            Intent intent = new Intent(mHostAppCategory);
            intent.addCategory(mPluginCategory);
            List<ResolveInfo> list = pm.queryBroadcastReceivers(intent, PackageManager.GET_META_DATA);
            //scan  end
            long scan_end = System.currentTimeMillis();
            if (PluginHost.LOGD) Log.d(PluginHost.TAG, "scan cost time: " + (scan_end-scan_begin) + "ms");
            
            if (PluginHost.LOGD) Log.d(PluginHost.TAG, "PluginScan list size:" + list.size());
            //load  start
            long load_begin = System.currentTimeMillis();
            for (int i=0; i<list.size(); i++) {
                ResolveInfo ri = list.get(i);
                final ComponentInfo ci = ri.activityInfo != null ? ri.activityInfo : ri.serviceInfo;
                if (PluginHost.bIsDynamic) {
                	final PluginObject obj = parseOne(ci, false);
                	if (PluginHost.LOGD)  Log.i(PluginHost.TAG,"mAnsycScan = "+mAnsycScan+",mHostAppCB="+mHostAppCB);
                    if (mAnsycScan) {
                        if (mHostAppCB != null) {
                            mHostAppCB.enqueueFGTask(new Runnable() {
                                public void run() {
                                    notifyScanOne(ci,obj);
                                }
                            });
                        }
                    } else {
                        notifyScanOne(ci,obj);
                    }
                }else{
                	final PluginObject obj = parseOne(ci, true);
                	if (PluginHost.LOGD)  Log.i(PluginHost.TAG,"mAnsycScan = "+mAnsycScan+",mHostAppCB="+mHostAppCB);
                    if (mAnsycScan) {
                        if (mHostAppCB != null) {
                            mHostAppCB.enqueueFGTask(new Runnable() {
                                public void run() {
                                    notifyScanOne(ci,obj);
                                }
                            });
                        }
                    } else {
                        notifyScanOne(ci,obj);
                    }
                }
            }
            //load end
            long load_end = System.currentTimeMillis();
            if (PluginHost.LOGD) Log.d(PluginHost.TAG, "load cost time: " + (load_end-load_begin) + "ms");
        }
    }

    /**
     * add callback 
     * @param cb
     */
    public void addScanCallback(PluginScannerCallback cb) {
        if (cb != null) {
            mCallbacks.add(cb);
        }
    }

    /*public void removeScanCallback(PluginScannerCallback cb) {
        if (cb != null) {
            mCallbacks.remove(cb);
        }
    }*/

    /**
     * set host callback
     * @param cb
     */
    public void setHostAppCallback(PluginHost.PluginHostCallback cb) {
        // cb may be null so as to reset it.
        mHostAppCB = cb;
    }

    /**
     * set scanning async or sync
     * @param isAnsyc
     */
    public void setAysncScan(boolean isAnsyc) {
        mAnsycScan = isAnsyc;
    }
}

