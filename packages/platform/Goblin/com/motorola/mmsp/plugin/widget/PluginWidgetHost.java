package com.motorola.mmsp.plugin.widget;

import com.motorola.mmsp.plugin.base.PluginFactory;
import com.motorola.mmsp.plugin.base.PluginHost;
import com.motorola.mmsp.plugin.base.PluginObject;
import com.motorola.mmsp.plugin.widget.PluginWidgetHostId.PluginWidgetId;

import dalvik.system.DexClassLoader;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



public class PluginWidgetHost extends PluginHost {
    /**
     * Record how many plugins has been activated. These plugins will be activated by host app.
     */
    private HashMap<String, PluginWidget> mActivated = null;
    
    private PluginWidgetHostId mPluginWidgetHostId=null;
    
    public static final int ONRESUME = 0;
    public static final int ONPAUSE = 1;
    public static final int ONDESTROY = 2;

    /**
     * @param hostApp
     * @param cb
     */
    public PluginWidgetHost(String hostApp, PluginHostCallback cb) {
        super(hostApp, PluginHost.PLUGIN_CATEGORY_WIDGET, cb);
        if(hostApp == null || cb == null){
        	return;
        }
        mActivated = new HashMap<String, PluginWidget>();    
        mPluginWidgetHostId=new PluginWidgetHostId(cb.getViewContext().getApplicationContext());
    }
    
	/**
	 * @param key PluginWidget ClassName
	 * @param pluginWidgetId
	 * @return View
	 */
	public View creatHostView(String key, int pluginWidgetId) {
		PluginWidgetHostView hostView = null;
		if ((key != null) && (!"".equals(key))) {
			PluginWidget widget = null;
			if (PluginHost.bIsDynamic) {
				if (mActivated.containsKey(key)) {
					widget = (PluginWidget) mActivated.get(key);
				} else {
					widget = (PluginWidget) mInstalled.get(key);
					if (widget != null) {

						PluginWidget widgetReal = (PluginWidget) PluginFactory
								.makePluginObject(widget.mProvider, mHostCB,PluginHost.PLUGIN_CATEGORY_WIDGET, true);
						if (widgetReal == null) {
							mPluginWidgetHostId.deletePluginWidgetId(pluginWidgetId);
							return null;
						}
						widget = widgetReal;
						mActivated.put(key, widgetReal);
					}
				}
			} else {
				widget = (PluginWidget) mInstalled.get(key);
				if (widget == null) {
					mPluginWidgetHostId.deletePluginWidgetId(pluginWidgetId);
					return null;
				}
				if (!mActivated.containsKey(key)) {
					mActivated.put(key, widget);
				}
			}
			hostView = widget.createHostView(mHostCB.getViewContext(),pluginWidgetId);
			if (hostView == null) {
				mPluginWidgetHostId.deletePluginWidgetId(pluginWidgetId);
				return null;
			}
			// create plugin widget host model
			if (widget.mModel == null) {
				PluginWidgetModel model = widget.createHostModel(mHostCB.getModelContext());
			} else {
				widget.mModel.notifyUpdateView(pluginWidgetId);
			}
		}
        return hostView;
    }
	
	
    /**
     * 
     * @return HashMap<String,PluginWidget>
     */
    public HashMap<String, PluginWidget> getPluginWidgets() {
        HashMap<String, PluginWidget> widgets = new HashMap<String, PluginWidget>();
        Set set = mInstalled.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()){
            Map.Entry map = (Map.Entry)itr.next();
            widgets.put((String) map.getKey(), (PluginWidget)map.getValue());
        }
        return widgets;
    }
	
	/**
	 * @param key PluginWidget ClassName
	 * @param id
	 * @return void
	 */
	public void removeHostView(String key, int id) {
		if ((key == null) || ("".equals(key))) {
			return;
		}
		PluginWidget widget = (PluginWidget) mActivated.get(key);
		if (widget != null) {
			widget.removeView(id);
			if (widget.mViews.size() == 0) {
				if(PluginHost.bIsDynamic){
					PluginFactory.removeLoder(key);
				}
				mActivated.remove(key);
			}
			mPluginWidgetHostId.deletePluginWidgetId(id);
		}
	}

	
	/**
	 * 
	 * @return HashMap<String,PluginWidget>
	 */
	public HashMap<String, PluginWidget> getPluginWidgetAdded(){
		return mActivated;
	}
	
	   /**
     * Generate an unique id for the plugin widget.
     * 
     * @param name Plugin widget package name.
     * @return Plugin widget id.
     */
    public int allocatePluginWidgetId(String name) {
        return mPluginWidgetHostId.allocatePluginWidgetId(name);      
    }
    
    /**
     * Get plugin widget package name from plugin widget id.
     * 
     * @param id Plugin widget id.
     * @return Plugin widget package name.
     */
    public String getPluginWidgetKey(int id) {
        return mPluginWidgetHostId.getPluginWidgetKey(id);
    }
    
    /**
     * Get plugin widget ids.
     * 
     * @return Plugin widget id array list.
     */
    public ArrayList<PluginWidgetId> getPluginWidgetIds() {
        return mPluginWidgetHostId.getPluginWidgetIds();
    }    
	
	/**
	 * @param state the status
	 */
	public void statusChange(int state){        
        Set set = mActivated.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()){
            Map.Entry map = ( Map.Entry)itr.next();
            PluginWidget widget = (PluginWidget)map.getValue();
            Set set1 = widget.mViews.entrySet();
            Iterator itr1 = set1.iterator();
            while(itr1.hasNext()){
                Map.Entry map1 = ( Map.Entry)itr1.next();
                PluginWidgetHostView view = (PluginWidgetHostView)map1.getValue();
                PluginWidgetStatusListener statusListener = view.getStatusListener();
                if(statusListener == null){
                    continue;
                }                
                switch(state){
                case ONRESUME:
                    statusListener.onResume();
                    break;
                case ONPAUSE:
                    statusListener.onPause();
                    break;
                case ONDESTROY:
                    statusListener.onDestroy();                    
                    break;
                    
                }
            }
        }        
    }
	
	/**
     * 
     * @param 
     * @return void
     */
    public void onDestroy(){
        Set set = mActivated.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()){
            Map.Entry map = ( Map.Entry)itr.next();
            PluginWidget widget = (PluginWidget)map.getValue();
            widget.onDestroy();
			if(PluginHost.bIsDynamic){
				PluginFactory.removeLoder((String) map.getKey());
			}
		}
        mActivated.clear();
    }
    
    /**
     * 
     * @param 
     * @return void
     */
    public void onModelDestroy(){
        Set set = mActivated.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()){
            Map.Entry map = ( Map.Entry)itr.next();
            PluginWidget widget = (PluginWidget)map.getValue();
            widget.onDestroy();
            widget.mModel.destoryModel();
            widget.mModel=null;
        }
        mActivated.clear();
        mPluginWidgetHostId=null;
    }
    
    /**
     * find PluginWidget whose name is key
     * @param key
     * @return
     */
    public PluginWidget findPluginWidget(String key){
    	 Set set = mInstalled.entrySet();
         Iterator itr = set.iterator();
         while(itr.hasNext()){
             Map.Entry map = ( Map.Entry)itr.next();
             String widget = (String) map.getKey();
             if(widget != null){
                 if(widget.equals(key)){
                	 return (PluginWidget) map.getValue();
                 }
             }
         }
         return null;
    }
    
    /**
     * add PluginWidget
     * @param ci
     */
    public void addPluginWidget(ComponentInfo ci){
    	if(ci != null){
    		PluginObject obj = mScanner.parseOne(ci, true);
        	if(obj != null){
        		mInstalled.put(ci.name, obj);
        	}
    	}
    }
    /**
     * update PluginWidget by @ci
     * @param ci
     */
    public void updatePluginWidget(ComponentInfo ci){
    	if(ci != null){
    		String key = ci.name;
    		PluginObject obj = mScanner.parseOne(ci, true);
        	if(obj != null){
        		if(mActivated.containsKey(key)){
        			PluginWidget widget = (PluginWidget) mActivated.get(key);
            		if(widget != null){
            			clearClassLoader(widget, key);
            			widget.onDestroy();
            			widget = null;
            		}
        			mActivated.remove(key);
        			mActivated.put(key, (PluginWidget) obj);
        		}
        		mInstalled.remove(key);
        		mInstalled.put(key, obj);
        	}
    	}
    }
    
    /**
     * remove PluginWidget by @key
     * @param key
     */
    public void removePluginWidget(String key){
    	if(key != null || !"".equals(key)){
    		if(mActivated.containsKey(key)){
    			PluginWidget widget = (PluginWidget) mActivated.get(key);
        		if(widget != null){
        			clearClassLoader(widget, key);
        			widget.onDestroy();
        			widget = null;
        		}
    			mActivated.remove(key);
    		}
    		mInstalled.remove(key);
    	}
    }
    
    public void clearClassLoader(PluginWidget widget, String key){
    	String fileName = widget.mProvider.mSourceDir;
		fileName = (String) fileName.subSequence(fileName.lastIndexOf("/"), fileName.length());
		fileName = fileName.substring(0, fileName.lastIndexOf(".apk")) +".dex";
		String fileDir = mHostCB.getViewContext().getFilesDir().getAbsolutePath();
		String fileString = fileDir+fileName;
		File file = new File(fileString);
		file.delete();
		PluginFactory.removeLoder(key);
    }

    public void setPluginWidgetHostId(PluginWidgetHostId id) {
    	mPluginWidgetHostId = id;
    }
}
