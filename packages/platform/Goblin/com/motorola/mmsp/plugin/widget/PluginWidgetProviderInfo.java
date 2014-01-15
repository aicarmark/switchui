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

package com.motorola.mmsp.plugin.widget;
import com.motorola.mmsp.plugin.base.PluginInfo;

import android.os.Parcel;
import android.os.Parcelable;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;


// Describes the meta data for an installed plugin widget provider.
public class PluginWidgetProviderInfo extends PluginInfo
                implements Parcelable {    
    /**
     * Widget is not resizable.
     */
    public static final int RESIZE_NONE             = 0;
    /**
     * Widget is resizable in the horizontal axis only.
     */
    public static final int RESIZE_HORIZONTAL       = 1;
    /**
     * Widget is resizable in the vertical axis only.
     */
    public static final int RESIZE_VERTICAL         = 2;
    /**
     * Widget is resizable in both the horizontal and vertical axes.
     */
    public static final int RESIZE_BOTH = RESIZE_HORIZONTAL | RESIZE_VERTICAL;

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
     * Minimum width (in dp) which the widget can be resized to. This field has no effect if it
     * is greater than minWidth or if horizontal resizing isn't enabled (see {@link #resizeMode}).
     *
     * <p>This field corresponds to the <code>android:minResizeWidth</code> attribute in
     * the AppWidget meta-data file.
     */
    public int minResizeWidth;

    /**
     * Minimum height (in dp) which the widget can be resized to. This field has no effect if it
     * is greater than minHeight or if vertical resizing isn't enabled (see {@link #resizeMode}).
     *
     * <p>This field corresponds to the <code>android:minResizeHeight</code> attribute in
     * the AppWidget meta-data file.
     */
    public int minResizeHeight;

    /**
     * How often, in milliseconds, that this AppWidget wants to be updated.
     * The AppWidget manager may place a limit on how often a AppWidget is updated.
     *
     * <p>This field corresponds to the <code>android:updatePeriodMillis</code> attribute in
     * the AppWidget meta-data file.
     *
     * <p class="note"><b>Note:</b> Updates requested with <code>updatePeriodMillis</code>
     * will not be delivered more than once every 30 minutes.</p>
     */
    public int updatePeriodMillis;

    /**
     * The resource id of the initial layout for this AppWidget.  This should be
     * displayed until the RemoteViews for the AppWidget is available.
     *
     * <p>This field corresponds to the <code>android:initialLayout</code> attribute in
     * the AppWidget meta-data file.
     */
    public int initialLayout;

    /**
     * The activity to launch that will configure the AppWidget.
     *
     * <p>This class name of field corresponds to the <code>android:configure</code> attribute in
     * the AppWidget meta-data file.  The package name always corresponds to the package containing
     * the AppWidget provider.
     */
    public ComponentName configure;

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
     * The view id of the AppWidget subview which should be auto-advanced by the widget's host.
     *
     * <p>This field corresponds to the <code>android:autoAdvanceViewId</code> attribute in
     * the AppWidget meta-data file.
     */
    public int autoAdvanceViewId;

    /**
     * A preview of what the AppWidget will look like after it's configured.
     * If not supplied, the AppWidget's icon will be used.
     *
     * <p>This field corresponds to the <code>android:previewImage</code> attribute in
     * the <code>&lt;receiver&gt;</code> element in the AndroidManifest.xml file.
     */
	public int previewImage;

    /**
     * The rules by which a widget can be resized. See {@link #RESIZE_NONE},
     * {@link #RESIZE_NONE}, {@link #RESIZE_HORIZONTAL},
     * {@link #RESIZE_VERTICAL}, {@link #RESIZE_BOTH}.
     *
     * <p>This field corresponds to the <code>android:resizeMode</code> attribute in
     * the AppWidget meta-data file.
     */
    public int resizeMode;

    /**
     * PluginWidgetProviderInfo
     */
    public PluginWidgetProviderInfo() {
    }

    /**
     * Unflatten the AppWidgetProviderInfo from a parcel.
     */
    public PluginWidgetProviderInfo(Parcel in) {
        if (0 != in.readInt()) {
            this.provider = new ComponentName(in);
        }
        this.minWidth = in.readInt();
        this.minHeight = in.readInt();
        this.minResizeWidth = in.readInt();
        this.minResizeHeight = in.readInt();
        this.updatePeriodMillis = in.readInt();
        this.initialLayout = in.readInt();
        if (0 != in.readInt()) {
            this.configure = new ComponentName(in);
        }
        this.label = in.readString();
        this.icon = in.readInt();
        this.previewImage = in.readInt();
        this.autoAdvanceViewId = in.readInt();
        this.resizeMode = in.readInt();
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(android.os.Parcel out, int flags) {
        if (this.provider != null) {
            out.writeInt(1);
            this.provider.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.minWidth);
        out.writeInt(this.minHeight);
        out.writeInt(this.minResizeWidth);
        out.writeInt(this.minResizeHeight);
        out.writeInt(this.updatePeriodMillis);
        out.writeInt(this.initialLayout);
        if (this.configure != null) {
            out.writeInt(1);
            this.configure.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
        out.writeString(this.label);
        out.writeInt(this.icon);
        out.writeInt(this.previewImage);
        out.writeInt(this.autoAdvanceViewId);
        out.writeInt(this.resizeMode);
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelable.Creator that instantiates AppWidgetProviderInfo objects
     */
    public static final Parcelable.Creator<PluginWidgetProviderInfo> CREATOR
            = new Parcelable.Creator<PluginWidgetProviderInfo>()
    {
        public PluginWidgetProviderInfo createFromParcel(Parcel parcel)
        {
            return new PluginWidgetProviderInfo(parcel);
        }

        public PluginWidgetProviderInfo[] newArray(int size)
        {
            return new PluginWidgetProviderInfo[size];
        }
    };

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
    public void onParse(ComponentInfo ci, AttributeSet attrs, Context pkgContext) {
    	if( ci == null || attrs == null){
    		return;
    	}
        provider=this.cn;
        minWidth=attrs.getAttributeIntValue(null, "minWidth", 0);
        minHeight=attrs.getAttributeIntValue(null, "minHeight", 0);
        int labelTemp =attrs.getAttributeResourceValue(null, "label",0);
        if(labelTemp != 0){
        	label = pkgContext.getString(labelTemp);
        }
        icon=attrs.getAttributeResourceValue(null, "icon", ci.getIconResource()); 
        minResizeWidth = attrs.getAttributeIntValue(null, "minResizeWidth", 0);
        minResizeHeight = attrs.getAttributeIntValue(null, "minResizeHeight", 0);
        updatePeriodMillis = attrs.getAttributeIntValue(null, "updatePeriodMillis", 0);
        initialLayout = attrs.getAttributeResourceValue(null, "initialLayout", 0);
        autoAdvanceViewId =  attrs.getAttributeResourceValue(null, "autoAdvanceViewId", 0);
        previewImage =  attrs.getAttributeResourceValue(null, "previewImage", 0);
        resizeMode =  attrs.getAttributeIntValue(null, "resizeMode", 0); 
		String clsName =  attrs.getAttributeValue(null, "configure");
		if(clsName != null && !clsName.equals("")){
			configure = new ComponentName(ci.packageName, clsName);
		}
    }
}

