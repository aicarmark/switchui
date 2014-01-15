/*
 * @(#)BlockLayout.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2011/03/04    NA              Initial version
 *
 */

package com.motorola.contextual.smartrules.rulesbuilder;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * A ViewGroup that coordinates dragging across its descendants.
 *
 * <p> This class used BlockLayer in the Android Launcher activity as a model.
 * It is a bit different in several respects:
 * (1) It extends MyAbsoluteLayout rather than FrameLayout; (2) it implements BlockSource and BlockTarget methods
 * that were done in a separate Workspace class in the Launcher.
 */
public class BlockLayout extends BlockAbsoluteLayout
    						implements IBlockDrag, IBlockTarget{
		
    BlockController mDragController;

    private BlockLayerInterface blockLayerCallback;

    /**
     * Used to create a new BlockLayer from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public BlockLayout (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDragController(BlockController controller) {
        mDragController = controller;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(mDragController != null)
    		return mDragController.dispatchKeyEvent(event);
    	else
    		return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mDragController == null)
        	return super.onInterceptTouchEvent(ev);
        else
        	return mDragController.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mDragController == null)
        	return super.onTouchEvent(ev);
        else
        	return mDragController.onTouchEvent(ev);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
    	if(mDragController == null)
    		return super.dispatchUnhandledMove(focused, direction);
    	else
    		return mDragController.dispatchUnhandledMove(focused, direction);
    }

    /**
     */
// DragSource interface methods

    /**
     * setDragController
     *
     */

    /* setDragController is already defined. See above. */

    /**
     * onDropCompleted
     *
     */

    public void onDropCompleted (View target, boolean success)
    {
        //Do nothing for now, can implement something later if required
    }

    /**
     */
// DropTarget interface implementation

    /**
     * Handle an object being dropped on the DropTarget.
     * This is the where a dragged view gets repositioned at the end of a drag.
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the original
     *          touch happened
     * @param yOffset Vertical offset with the object being dragged where the original
     *          touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     *
     */
    public void onDrop(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                       BlockDragView dragView, Object dragInfo)
    {
        //Upon some design changes/re-usability can be interchanged to View
        ViewGroup v = (ViewGroup) dragInfo;

        int w = v.getWidth ();
        int h = v.getHeight ();
        int left = x - xOffset;
        int top = y - yOffset;
        BlockLayout.LayoutParams lp = new BlockLayout.LayoutParams (w, h, left, top);

        
        this.updateViewLayout(v, lp);

    }

    public void trashBlockView(Object dragInfo) {
        ViewGroup v = (ViewGroup) dragInfo;
        this.getBlockLayerCallback().onDelete(v);
        this.removeView(v);
        this.requestLayout();

    }

    public void onDragEnter(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                            BlockDragView dragView, Object dragInfo)
    {
    }

    public void onDragOver(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                           BlockDragView dragView, Object dragInfo)
    {
    }

    public void onDragExit(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                           BlockDragView dragView, Object dragInfo)
    {
    }

    /**
     * Check if a drop action can occur at, or near, the requested location.
     * This may be called repeatedly during a drag, so any calls should return
     * quickly.
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the
     *            original touch happened
     * @param yOffset Vertical offset with the object being dragged where the
     *            original touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     * @return True if the drop will be accepted, false otherwise.
     */
    public boolean acceptDrop(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                              BlockDragView dragView, Object dragInfo)
    {
        /* TODO implement the logic to allow move or retrace back / simply not call updateViewLayout
        Initially this was returning true, to accept drop on places other than original place of block
        Later upon CXD decision , this functionality was dropped
        In future for things like Re-ordering and repositioning of blocks this HAS TO return true
        This can be coupled with conditions on which true is return to accept drops in particular
        coordinates or some other specific condition 
        */
        return false;
    }

    /**
     * Estimate the surface area where this object would land if dropped at the
     * given location.
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the
     *            original touch happened
     * @param yOffset Vertical offset with the object being dragged where the
     *            original touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     * @param recycle {@link Rect} object to be possibly recycled.
     * @return Estimated area that would be occupied if object was dropped at
     *         the given location. Should return null if no estimate is found,
     *         or if this target doesn't provide estimations.
     */
    public Rect estimateDropLocation(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                                     BlockDragView dragView, Object dragInfo, Rect recycle)
    {
        return null;
    }

    public interface BlockLayerInterface {
        public void onConfigure(View block);
        public void onDelete(View block);
        public void onConnected(View block, boolean connectStatus);
    }

    public BlockLayerInterface getBlockLayerCallback() {
        return blockLayerCallback;
    }

    public void setBlockLayerCallback(BlockLayerInterface blockLayerCallback) {
        this.blockLayerCallback = blockLayerCallback;
    }

} // end class