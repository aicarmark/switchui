/**
 * FILE: PluginWidgetModelBase.java
 *
 * DESC: The basic class of plugin widget model, which is to manage data parts.
 * 
 * Widget plugin would be formated as Jar / APK packages.
 */

package com.motorola.mmsp.plugin.widget;

import com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public abstract class PluginWidgetModel extends BroadcastReceiver
            implements Runnable {

    protected  PluginWidget mPluginWidget;      
    
    /**
     * The instance implement PluginHostCallback,like Launcher .
     */
    protected  PluginHostCallback mHostAppCB ;     

    
    /**
     * set PluginWidget
     * @param mPluginWidget
     */
    protected void setPluginWidget(PluginWidget pluginWidget) {
        this.mPluginWidget = pluginWidget;
    } 

    /**
     * set host callback
     * @param mHostAppCB
     */
    protected void setHostAppCB(PluginHostCallback mHostAppCB) {
        this.mHostAppCB = mHostAppCB;
    }

    /* (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * load data
     */
    public abstract  void loadModel() ;
    /**
     * destory model
     */
    public abstract  void destoryModel() ;
    
    /**
     * @param isAsync
     */
    public void refresh(Boolean isAsync){
        if (isAsync) {
            if (mHostAppCB != null) {
                mHostAppCB.enqueueBGTask(this);
            }
        } else {
            run();
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        loadModel();

        if (mHostAppCB != null) {
            mHostAppCB.enqueueFGTask(new Runnable() {
                public void run() {
                    notifyLoadFinished();
                    notifyModelChanged();
                }
            });
        }
    }
    
    // Notify all callbacks the scan has been started.
    protected void notifyLoadStarted() {
    	if(mPluginWidget == null){
    		return;
    	}
        HashMap<Integer, PluginWidgetHostView> hostViews= mPluginWidget.mViews;     
        if (hostViews != null && hostViews.size()>0) {
            for(Iterator it = hostViews.entrySet().iterator(); it.hasNext(); ){
                Map.Entry e = (Map.Entry)it.next();
                PluginWidgetHostView hostview=(PluginWidgetHostView)e.getValue();
                PluginWidgetModelListener listener=hostview.getModelListener();
                if(listener !=null){
                    listener.startLoading();
                }
               }
        }        
    }

    // Notify all callbacks the scan has met one plugin.
    protected void notifyLoadFinished() {
    	if(mPluginWidget == null){
    		return;
    	}
        HashMap<Integer, PluginWidgetHostView> hostViews= mPluginWidget.mViews;     
        if (hostViews != null && hostViews.size()>0) {
            for(Iterator it = hostViews.entrySet().iterator(); it.hasNext(); ){
                Map.Entry e = (Map.Entry)it.next();
                PluginWidgetHostView hostview=(PluginWidgetHostView)e.getValue();
                PluginWidgetModelListener listener=hostview.getModelListener();
                if(listener !=null){
                    listener.finishLoading();
                }
               }
        }  
    }
    
    /**
     * do something when model changed 
     */
    protected void notifyModelChanged() {
    	if(mPluginWidget == null){
    		return;
    	}
        HashMap<Integer, PluginWidgetHostView> hostViews= mPluginWidget.mViews;     
        if (hostViews != null && hostViews.size()>0) {
            for(Iterator it = hostViews.entrySet().iterator(); it.hasNext(); ){
                Map.Entry e = (Map.Entry)it.next();
                PluginWidgetHostView hostview=(PluginWidgetHostView)e.getValue();
                PluginWidgetModelListener listener=hostview.getModelListener();
                if(listener !=null){
                    listener.onModelChanged(this);
                }
               }
        }        
    }
    
    /**
     * update view which id is @para
     * @param hostView
     */
    protected void notifyUpdateView(int hostViewId){
    	if(mPluginWidget == null){
    		return;
    	}
    	Integer id = Integer.valueOf(hostViewId);
    	HashMap<Integer, PluginWidgetHostView> hostViews= mPluginWidget.mViews;     
        if (hostViews != null && hostViews.size()>0) {
            for(Iterator it = hostViews.entrySet().iterator(); it.hasNext(); ){
                Map.Entry e = (Map.Entry)it.next();
                if(id.equals(e.getKey())){
                	PluginWidgetHostView hostview=(PluginWidgetHostView)e.getValue();
                	 PluginWidgetModelListener listener=hostview.getModelListener();
                     if(listener !=null){
                         listener.onModelChanged(this);
                     }
                     break;
                }
              }
        }        
    }
    
    /**
     * update views which id is in @para
     * @param hostView
     */
    protected void notifyUpdateView(int[] hostViewIds){
    	for(int i = 0; i < hostViewIds.length; i++){
    		notifyUpdateView(hostViewIds[i]);
    	}
    }
}

