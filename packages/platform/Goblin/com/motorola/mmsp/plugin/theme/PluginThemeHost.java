package com.motorola.mmsp.plugin.theme;

import com.motorola.mmsp.plugin.base.PluginHost;
import com.motorola.mmsp.plugin.base.PluginObject;

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



public class PluginThemeHost extends PluginHost {

    /**
     * @param hostApp
     * @param cb
     */
    public PluginThemeHost(String hostApp, PluginHostCallback cb) {
        super(hostApp, PluginHost.PLUGIN_CATEGORY_THEME, cb);
        if(hostApp == null || cb == null){
        	return;
        }
    }
	
    /**
     * 
     * @return HashMap<String,PluginWidget>
     */
    public HashMap<String, PluginTheme> getPluginThemes() {
        HashMap<String, PluginTheme> widgets = new HashMap<String, PluginTheme>();
        Set set = mInstalled.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()){
            Map.Entry map = (Map.Entry)itr.next();
            widgets.put((String) map.getKey(), (PluginTheme)map.getValue());
        }
        return widgets;
    }
    
    
    /**
     * find PluginWidget whose name is key
     * @param key
     * @return
     */
    public PluginTheme findPluginWidget(String key){
    	 Set set = mInstalled.entrySet();
         Iterator itr = set.iterator();
         while(itr.hasNext()){
             Map.Entry map = ( Map.Entry)itr.next();
             String widget = (String) map.getKey();
             if(widget != null){
                 if(widget.equals(key)){
                	 return (PluginTheme) map.getValue();
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
//    	Log.i("ss","host-updatePluginWidget()");
//    	if(ci != null){
//    		Log.i("ss","host-updatePluginWidget()-1");
//    		String key = ci.name;
//    		PluginObject obj = mScanner.parseOne(ci);
//        	if(obj != null){
//        		Log.i("ss","host-updatePluginWidget()-2");
//            		if(widget != null){
//            			widget.onDestroy();
//            			widget = null;
//            		}
//            		Log.i("ss","host-updatePluginWidget()-3");
//        			mActivated.remove(key);
//        			mActivated.put(key, (PluginTheme) obj);
//        		}
//        		Log.i("ss","host-updatePluginWidget()-4");
//        		mInstalled.remove(key);
//        		mInstalled.put(key, obj);
//        	}
//    	}
    }
    
    /**
     * remove PluginWidget by @key
     * @param key
     */
    public void removePluginWidget(String key){
//    	Log.i("ss","host-removePluginWidget()");
//    	if(key != null || !"".equals(key)){
//    		
//    		if(mActivated.containsKey(key)){
//    			PluginTheme widget = (PluginTheme) mActivated.get(key);
//        		if(widget != null){
//        			widget.onDestroy();
//        			widget = null;
//        		}
//    			mActivated.remove(key);
//    		}
//    		mInstalled.remove(key);
//    	}
    }
}
