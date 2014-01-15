/**
 * FILE: PluginWidgetModelListener.java
 *
 * DESC: The model listener interface.
 * 
 */

package com.motorola.mmsp.plugin.widget;

public interface PluginWidgetModelListener {
    /**
     * Called when model start loading
     * @param 
     * @return void
     */
    public void startLoading();
    
    /**
     * Called when model finish loading
     * @param 
     * @return void
     */
    public void finishLoading();
   

    /**
     * @param pluginWidgetModel
     */
    public void onModelChanged(PluginWidgetModel pluginWidgetModel);
}

