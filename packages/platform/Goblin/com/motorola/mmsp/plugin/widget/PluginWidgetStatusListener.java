/**
 * FILE: PluginWidgetStatusListener.java
 *
 * DESC: The model listener interface.
 * 
 */

package com.motorola.mmsp.plugin.widget;

public interface PluginWidgetStatusListener {
    /**
     * Called when Host status change to  onPause
     * @param 
     * @return void
     */
    public void onPause();
    /**
     * Called when Host status change to  onResume
     * @param 
     * @return void
     */
    public void onResume();
    /**
     * Called when Host status change to  onDestroy
     * @param 
     * @return void
     */
    public void onDestroy();
    
    /**
     * Extend Interface
     * @param 
     * @return void
     */
    public void onShow();
    /**
     * Extend Interface
     * @param 
     * @return void
     */
    public void onHide();
}

