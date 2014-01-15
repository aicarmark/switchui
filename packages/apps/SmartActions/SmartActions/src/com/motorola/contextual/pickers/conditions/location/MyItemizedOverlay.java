/*
 * @(#)MyItemizedOverlay.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA				  Initial version
 *
 */
package com.motorola.contextual.pickers.conditions.location;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;


/**
 * This class is used by the mapview activities to lay an overlay for points on to the canvas.
 *
 *<code><pre>
 * CLASS:
 * 	extends ItemizedOverlay.
 *
 * 	implements
 *   None.
 *
 * RESPONSIBILITIES:
 * 	constructs the overlays.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
@SuppressWarnings("rawtypes")
public class MyItemizedOverlay extends ItemizedOverlay {

    private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

    protected Context mContext = null;

    public OnTapListenerIndex mOnTapListenerIndex = null;

    public MyItemizedOverlay(Drawable defaultMarker) {
        super(boundCenterBottom(defaultMarker));
    }

    public MyItemizedOverlay(Context context, Drawable defaultMarker) {
        super(boundCenterBottom(defaultMarker));
        mContext = context;
    }

    public void addOverlay(OverlayItem overlay) {
        mOverlays.add(overlay);
        populate();
    }

    @Override
    protected OverlayItem createItem(int i) {
        return mOverlays.get(i);
    }

    @Override
    public int size() {
        return mOverlays.size();
    }

    @Override
    protected boolean onTap(int index) {
        if(mOnTapListenerIndex != null)
            mOnTapListenerIndex.onTapEvent(index, mOverlays.get(index));
        return true;
    }

    /** draw method to disable the shadow.
     *
     */
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, false);
    }

    /**
     * Interface to handle onTap events for onTap(int index)
     */
    public interface OnTapListenerIndex {

        public void onTapEvent(int index, OverlayItem overlayItem);

    }

    /** Register listener for onTap(int index)
     * 	to unregister pass the listener as null
     *
     * @param listener - listener of type OnTapListenerIndex
     */
    public void registerOnTapListenerIndex(OnTapListenerIndex listener) {

        mOnTapListenerIndex = listener;
    }
}