/**
 * FILE: PluginObject.java
 *
 * DESC: The basic plugin object.
 * 
 */

package com.motorola.mmsp.plugin.base;
//import com.motorola.mmsp.pluginframework.widget.*;

public interface PluginObject {
    /**
     * @param info
     * @param cb
     * @return void
     */
    public void attachInfo(PluginInfo info,PluginHost.PluginHostCallback cb);
}

