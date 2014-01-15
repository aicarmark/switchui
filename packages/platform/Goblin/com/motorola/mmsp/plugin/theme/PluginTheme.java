/**
 * FILE: PluginWidgetBase.java
 *
 * DESC: The basic class of plugin widget, which is to manage widget model and widget view 
 * of plugin widget in MVC mode. Every plugin widget must inherited from this base class.
 * 
 * Widget plugin would be formated as Jar / APK packages.
 */

package com.motorola.mmsp.plugin.theme;

import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;

import com.motorola.mmsp.plugin.base.PluginHost;
import com.motorola.mmsp.plugin.base.PluginInfo;
import com.motorola.mmsp.plugin.base.PluginObject;
import com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback;

public abstract class PluginTheme extends BroadcastReceiver
                    implements PluginObject{ 

    //public final static String ACTION_PLUGIN_UPDATE = "com.motorola.mmsp.plugin.update";
    
    /**
     * The Context create from package name of plug-in widget 
     */
    public Context pkgContext;
    
    /**
     * The instance implement PluginHostCallback,like Launcher .
     */
    protected  PluginHostCallback mHostAppCB ;
    /**
     * The provider info which will be scanned out from scanner.
     */
    public PluginThemeProviderInfo mProvider;

    public void Plugintheme(){
    	mProvider = null;
    	mHostAppCB = null;
    	pkgContext = null;
    }
	/**
	 * 
	 * @param 
	 * @return void
	 */
	void onDestroy() {
		// TODO Auto-generated method stub
		mProvider = null;
	}
	
    /* (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {

    }

    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginObject#attachInfo(com.motorola.mmsp.plugin.base.PluginInfo, com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback)
     */
    public void attachInfo(PluginInfo info,PluginHost.PluginHostCallback cb) {
        if (info != null && info instanceof PluginThemeProviderInfo) {
            mProvider = (PluginThemeProviderInfo)info;
            try {
                pkgContext=cb.getViewContext().createPackageContext(mProvider.cn.getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mHostAppCB=cb;
        }
    }
}

