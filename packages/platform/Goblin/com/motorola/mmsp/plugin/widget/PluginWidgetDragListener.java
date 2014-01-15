/**
 * FILE: PluginWidgetDragListener.java
 *
 */

package com.motorola.mmsp.plugin.widget;

public interface PluginWidgetDragListener {
    /**
     * Called when HostView start drag
     * @param 
     * @return void
     */
    public void onDragStart();
    /**
     * Called when HostView moving
     * @param 
     * @return void
     */
    public void onDragMoving();
    /**
     * Called when HostView end drag
     * @param 
     * @return void
     */
    public void onDragEnd();
}
