/**
 * FILE: PluginWidgetProviderInfo.java
 *
 * DESC: Plugin widget provider info, which is to house data info which will be 
 * scanned out.
 *
 * PluginWidgetProviderInfo could be converted to AppWidgetProviderInfo via Parcelable.
 * 
 * Widget plugin would be formated as Jar / APK packages.
 */

package com.motorola.mmsp.plugin.theme;
import com.motorola.mmsp.plugin.base.PluginInfo;

import android.os.Parcel;
import android.os.Parcelable;
import android.content.ComponentName;
import android.content.pm.ComponentInfo;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;


// Describes the meta data for an installed plugin widget provider.
public class PluginThemeProviderInfo extends PluginInfo
                implements Parcelable {    

    /**
     * Identity of this AppWidget component.  This component should be a {@link
     * android.content.BroadcastReceiver}, and it will be sent the AppWidget intents
     * {@link android.appwidget as described in the AppWidget package documentation}.
     *
     * <p>This field corresponds to the <code>android:name</code> attribute in
     * the <code>&lt;receiver&gt;</code> element in the AndroidManifest.xml file.
     */
    public ComponentName provider;

    /**
     * The default height of the widget when added to a host, in dp. The widget will get
     * at least this width, and will often be given more, depending on the host.
     *
     * <p>This field corresponds to the <code>android:minWidth</code> attribute in
     * the AppWidget meta-data file.
     */
    public int minWidth;

    /**
     * The default height of the widget when added to a host, in dp. The widget will get
     * at least this height, and will often be given more, depending on the host.
     *
     * <p>This field corresponds to the <code>android:minHeight</code> attribute in
     * the AppWidget meta-data file.
     */
    public int minHeight;
    /**
     * The label to display to the user in the AppWidget picker.  If not supplied in the
     * xml, the application label will be used.
     *
     * <p>This field corresponds to the <code>android:label</code> attribute in
     * the <code>&lt;receiver&gt;</code> element in the AndroidManifest.xml file.
     */
    public String label;

    /**
     * The icon to display for this AppWidget in the AppWidget picker.  If not supplied in the
     * xml, the application icon will be used.
     *
     * <p>This field corresponds to the <code>android:icon</code> attribute in
     * the <code>&lt;receiver&gt;</code> element in the AndroidManifest.xml file.
     */
    public int icon;


    /**
     * A preview of what the AppWidget will look like after it's configured.
     * If not supplied, the AppWidget's icon will be used.
     *
     * <p>This field corresponds to the <code>android:previewImage</code> attribute in
     * the <code>&lt;receiver&gt;</code> element in the AndroidManifest.xml file.
     */
	public int previewImage;


    /**
     * PluginWidgetProviderInfo
     */
    public PluginThemeProviderInfo() {
    }


    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents() {
        return 0;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "PluginWidgetProviderInfo(provider=" + this.provider + ")";
    }


    /**
     * Parse function for different kinds of plugin meta-data.
     */
    /* (non-Javadoc)
     * @see com.motorola.mmsp.plugin.base.PluginInfo#onParse(android.content.pm.ComponentInfo, android.util.AttributeSet)
     */
    public void onParse(ComponentInfo ci, AttributeSet attrs) {
    	if( ci == null || attrs == null){
    		return;
    	}
    	
		provider=this.cn;
		minWidth=attrs.getAttributeIntValue(null, "minWidth", 0);
	    minHeight=attrs.getAttributeIntValue(null, "minHeight", 0);
        label=attrs.getAttributeValue(null, "label");
        icon=attrs.getAttributeResourceValue(null, "icon", ci.getIconResource()); 
        previewImage =  attrs.getAttributeResourceValue(null, "previewImage", 0);
        Log.i("ss","info : provider="+provider+" | minWidth="+minWidth+" | minHeight="+minHeight+" | label ="+label+" | icon="+icon + " | previewImage="+previewImage);
    }


	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
}

