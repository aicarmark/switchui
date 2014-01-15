/**
 * FILE: PluginWidgetScrollListener.java
 *
 * DESC: The model listener interface.
 * 
 */

package com.motorola.mmsp.plugin.widget;

import com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback;

public interface PluginWidgetScrollListener {
	/**
	 * the widget host will call this method after creating host view
	 * @param callback
	 * @param widgetId
	 */
	public void createViewCompleted(PluginHostCallback callback, int widgetId);
	
	/**
	 * show animation when device unlock etc.
	 * @param module
	 * @param animationType
	 */
	public void pluginAnimation(int module, int animationType);
    /**
     * Called when scrolling start
     * @param 
     * @return void
     */
    public void startScrolling(boolean isDefault);

    /**
     * @param scrollProgress scrollProgress When scroll to left, current screen is from 0 to 1,
     * right screen is from -1 to 0, when scroll to right, current screen is from 0 to -1, left
     *  screen is from 1 to 0.
     * @param direction
     * @param isDefault
     */
    public void scrolled(float scrollProgress, int direction, boolean isDefault);
    
    /**
     * Called when scrolling by vertical
     * @param scrollProgress
     * @param isDefault
     */
    public void scrolledY(float scrollProgress, boolean isDefault);
    
    /**
     * Called when scrolling finish
     * @param 
     * @return void
     */
    public void finishScrolling(boolean isDefault);
    
}
