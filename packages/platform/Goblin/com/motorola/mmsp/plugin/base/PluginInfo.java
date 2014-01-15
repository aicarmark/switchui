/**
 * FILE: PluginInfo.java
 *
 * DESC: The plugin information retrieved from meta-data.
 * 
 */

package com.motorola.mmsp.plugin.base;

import android.content.pm.ComponentInfo;
import android.util.AttributeSet;
import android.content.ComponentName;
import android.content.Context;

public class PluginInfo {
    /**
     * ComponentName
     */
    public ComponentName cn;
    /**
     * The full path of the installed plugin APK.
     */
    public String mSourceDir;    

    public PluginInfo() {}

    /**
     * Parse function for different kinds of plugin meta-data.
     * @param ci
     * @param attrs
     * @return void
     */
    public void onParse(ComponentInfo ci, AttributeSet attrs, Context pkgContext) {
    }
}

