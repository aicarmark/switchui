/**
 * FILE: PluginWidgetBase.java
 *
 * DESC: The basic class of plugin widget, which is to manage widget model and widget view 
 * of plugin widget in MVC mode. Every plugin widget must inherited from this base class.
 * 
 * Widget plugin would be formated as Jar / APK packages.
 */

package com.motorola.mmsp.plugin.widget;

import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.motorola.mmsp.plugin.base.PluginHost;
import com.motorola.mmsp.plugin.base.PluginInfo;
import com.motorola.mmsp.plugin.base.PluginObject;
import com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback;

public abstract class PluginWidget extends BroadcastReceiver
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
    public PluginWidgetProviderInfo mProvider;

    /**
     * The singleton instance of widget model.
     */
    protected  PluginWidgetModel mModel;

    /**
     * The list of activated widget view.
     */
    protected  HashMap<Integer, PluginWidgetHostView> mViews = new HashMap<Integer, PluginWidgetHostView>();
    
    /**
     * @param modelContext
     * @return PluginWidgetModel
     */
    public abstract PluginWidgetModel createModel(Context modelContext);
    /**
     * @param viewContext
     * @param widgetId 
     * @return View
     */
    public abstract View createView(Context viewContext, int widgetId); 
    /**
     * do something such as free deap when remove @v 
     * @param v View
     */
    public  void deleteView(View v){
    	
    }
     
    /**
     * do something such as free deap when plugin host go onDestory  @v 
     * @param v View
     */
    public void destroyView(View v){
    	Log.e("pluginwidget","********the function destroyView need to implement by the sub-class ***************");
    }
    /**
     * @param viewContext  Host Application View Context
     * @param pluginWidgetId PluginWidgetId
     * @return PluginWidgetHostView
     */
     PluginWidgetHostView createHostView(Context viewContext, int pluginWidgetId){        
        if(mViews.containsKey(pluginWidgetId)) {
         mViews.remove(pluginWidgetId);
        }
        View view= createView(viewContext, pluginWidgetId);
        if(view == null){
        	return null;
        }
        PluginWidgetHostView hostView = new PluginWidgetHostView(this, view); 
        if(hostView != null){
            hostView.setPluginWidgetHostId(pluginWidgetId);
            mViews.put(pluginWidgetId, hostView);
        }
        return hostView;
    }
    
     /**
      * create model
     * @param modelContext
     * @return PluginWidgetModel
     */
    PluginWidgetModel createHostModel(Context modelContext){
        //create plugin widget model
        mModel= createModel(modelContext);
        if(mModel == null){
        	return null;
        }
        mModel.setHostAppCB(mHostAppCB);
        mModel.setPluginWidget(this);
        //enqueue mModel as runnable task into background task of host application 
        mModel.notifyLoadStarted();
        if(mHostAppCB != null){
        	mHostAppCB.enqueueBGTask(mModel);
        }
        return mModel;        
    }
     
    /**
     * remove the item whose id equals @param 
     * @param id
     * @return void
     */
    public void removeView(int id){
    	Integer i = Integer.valueOf(id);
    	PluginWidgetHostView hostView = mViews.get(i);
    	if(hostView != null){
    		deleteView(hostView.getChildAt(0));	
    	}
    	mViews.remove(id);
    	
    	if(mViews.size() == 0){
    		if(mModel != null){
        	    mModel.destoryModel();
        	    mModel.mPluginWidget=null;
    		}
    	    mModel=null;
    	}
    }
    
	/**
	 * 
	 * @param 
	 * @return void
	 */
	void onDestroy() {
		// TODO Auto-generated method stub
		Set set = mViews.entrySet();
        	Iterator itr = set.iterator();
        	while(itr.hasNext()){
            	    Map.Entry map = ( Map.Entry)itr.next();
		    PluginWidgetHostView hostView = (PluginWidgetHostView) map.getValue();
    		    if(hostView != null){
    			destroyView(hostView.getChildAt(0));	
    		    }            	    
        	} 
		mViews.clear();
		if(mModel != null){
			mModel.destoryModel();
			 mModel.mPluginWidget=null;
		}
		mModel=null;
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
        if (info != null && info instanceof PluginWidgetProviderInfo && cb != null) {
            mProvider = (PluginWidgetProviderInfo)info;
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

