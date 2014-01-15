/**
 * FILE: PluginScannerFactory.java
 */

package com.motorola.mmsp.plugin.base;

import com.motorola.mmsp.plugin.theme.PluginTheme;
import com.motorola.mmsp.plugin.theme.PluginThemeHost;
import com.motorola.mmsp.plugin.theme.PluginThemeProviderInfo;
import com.motorola.mmsp.plugin.widget.PluginWidget;
import com.motorola.mmsp.plugin.widget.PluginWidgetHost;
import com.motorola.mmsp.plugin.widget.PluginWidgetModel;
import com.motorola.mmsp.plugin.widget.PluginWidgetProviderInfo;

import android.content.Context;
import android.util.Log;
import android.view.View;

import java.lang.ClassLoader;
import java.io.File;
import java.util.HashMap;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class PluginFactory {

    /**
     * DexClassLoader which is to load plugin inherited object.
     */
    private static HashMap<String, DexClassLoader> mClassLoadMap = new HashMap<String, DexClassLoader>();

    
    /**
     * Create plugin scanner host according to the given host app category and plugin category.
     * @param hostApp Type of HostAPP : PluginHost.PLUGIN_HOST_APP_CATEGORY_HOME , PluginHost.PLUGIN_HOST_APP_CATEGORY_DASHBOARD
     * @param plugin  Type of Plug-in : PluginHost.PLUGIN_CATEGORY_WIDGET 
     * @param cb  PluginHostCallback
     * @return PluginHost
     */
    public static PluginHost makeHost(String hostApp, String plugin,
            PluginHost.PluginHostCallback cb) {
        if (hostApp != null && plugin != null && cb != null) {
            if (PluginHost.PLUGIN_CATEGORY_WIDGET.equals(plugin)) {
                if(cb.getViewContext()!=null && cb.getModelContext()!=null){
                    return (PluginHost) new PluginWidgetHost(hostApp, cb);
                }
                else{
                    if (PluginHost.LOGD){
                        if(cb.getViewContext()==null){
                            Log.e(PluginHost.TAG,"The method of getViewContext() in PluginHostCallback need to implemented in Host Application! ");
                        }
                        if(cb.getModelContext()==null){
                            Log.e(PluginHost.TAG,"The method of getModelContext() in PluginHostCallback need to implemented in Host Application! ");
                        }
                    }                        
                }
            }
            else if(PluginHost.PLUGIN_CATEGORY_THEME.equals(plugin)){
                if(cb.getViewContext()!=null && cb.getModelContext()!=null){
                    return (PluginHost) new PluginThemeHost(hostApp, cb);
                }
                else{
                    if (PluginHost.LOGD){
                        if(cb.getViewContext()==null){
                            Log.e(PluginHost.TAG,"The method of getViewContext() in PluginHostCallback need to implemented in Host Application! ");
                        }
                        if(cb.getModelContext()==null){
                            Log.e(PluginHost.TAG,"The method of getModelContext() in PluginHostCallback need to implemented in Host Application! ");
                        }
                    }                        
                }
            
            }
        }
        return null;
    }

    /**
     * Create plugin scanner according to the given host app category and plugin
     * category.
     */
    
    /**
     * Create plugin scanner according to the given host app category and plugin category.
     * @param hostApp Type of HostAPP : PluginHost.PLUGIN_HOST_APP_CATEGORY_HOME , PluginHost.PLUGIN_HOST_APP_CATEGORY_DASHBOARD
     * @param plugin  Type of Plug-in : PluginHost.PLUGIN_CATEGORY_WIDGET 
     * @return PluginScanner
     */
    public static PluginScanner makeScanner(String hostApp, String plugin) {
        if (hostApp != null && plugin != null) {
            return new PluginScanner(hostApp, plugin);
        }
        return null;
    }

    /**
     * 
     */
    
    /**
     * Create plugin info according to the given host app category and plugin category.
     * @param hostApp Type of HostAPP : PluginHost.PLUGIN_HOST_APP_CATEGORY_HOME , PluginHost.PLUGIN_HOST_APP_CATEGORY_DASHBOARD
     * @param plugin  Type of Plug-in : PluginHost.PLUGIN_CATEGORY_WIDGET 
     * @return PluginInfo
     */
    public static PluginInfo makePluginInfo(String hostApp, String plugin) {
        if (hostApp != null && plugin != null) {
            if (PluginHost.PLUGIN_CATEGORY_WIDGET.equals(plugin)) {
                return (PluginInfo) new PluginWidgetProviderInfo();
            }
            else if(PluginHost.PLUGIN_CATEGORY_THEME.equals(plugin)){
            	return (PluginInfo) new PluginThemeProviderInfo();
            }
        }
        return null;
    }
    
    /**
     * Create plugin object according to the given host app category and plugin category.
     * @param info
     * @param cb
     * @param pluginCategory 
     * @return PluginObject
     */
    public static PluginObject makePluginObject(PluginInfo info, PluginHost.PluginHostCallback cb, String pluginCategory, boolean isCreateReal) {
        if (info != null && cb != null) {
            PluginObject obj = null;
            if(PluginHost.PLUGIN_CATEGORY_WIDGET.equals(pluginCategory)){
            	DexClassLoader cl = null;
                File file = new File(info.mSourceDir);
                String clsName = info.cn.getClassName();
                if(isCreateReal){
                	if (PluginHost.LOGD)
                        Log.d(PluginHost.TAG, "PluginFactory makePluginObject file:" + file + ", clsName:"
                                + clsName + ", cb opt path:"
                                + cb.getViewContext().getFilesDir().getAbsolutePath());
                    if (mClassLoadMap != null && mClassLoadMap.containsKey(clsName)) {
                        cl = mClassLoadMap.get(clsName);
                    } else {
                        cl = new DexClassLoader(file.toString(), cb.getViewContext().getFilesDir()
                                .getAbsolutePath(), null, cb.getViewContext().getClass().getClassLoader());
//                        String ss = "/data/data/com.test/files/com.motorola.mmsp.activityPluginWidget-1.dex";
                        mClassLoadMap.put(clsName, cl);
                        // if (PluginHost.LOGD) Log.d(PluginHost.TAG,
                        // "PluginFactory makePluginObject sys cl:" +
                        // ClassLoader.getSystemClassLoader() + ", new cl:" + cl);
                    }

                    try {
                        Class<?> c = cl.loadClass(clsName);
                        if (PluginHost.LOGD)
                            Log.d(PluginHost.TAG, "PluginFactory makePluginObject loadclass success");
                        obj = (PluginObject) c.newInstance();
                        obj.attachInfo(info, cb);
                        if (PluginHost.LOGD)
                            Log.d(PluginHost.TAG, "PluginFactory makePluginObject newInstance success");
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        Log.d(PluginHost.TAG,
                                "PluginFactory makePluginObject met class not found exception:" + e);
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Log.d(PluginHost.TAG,
                                "PluginFactory makePluginObject met IllegalAccessException exception:" + e);
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Log.d(PluginHost.TAG,
                                "PluginFactory makePluginObject met InstantiationException exception:" + e);
                    }
                }else {
                	PluginWidget widget = new PluginWidget(){

						@Override
						public PluginWidgetModel createModel(
								Context modelContext) {
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public View createView(Context viewContext, int widgetId) {
							// TODO Auto-generated method stub
							return null;
						}
                		
                	}; 
                	obj= (PluginObject)widget;
                	obj.attachInfo(info, cb);
                	
                }
            }else if(PluginHost.PLUGIN_CATEGORY_THEME.equals(pluginCategory)){
            	obj = (PluginObject)new PluginTheme(){
            		
            	};
            	obj.attachInfo(info, cb);
            }
            return obj;
        }
        return null;
    }
    
    public static void removeLoder(String key){
    	mClassLoadMap.remove(key);    	
    }
}
