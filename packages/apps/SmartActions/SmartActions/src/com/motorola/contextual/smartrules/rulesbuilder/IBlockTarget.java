/*
 * @(#)IBlockTarget.java
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
 *
 * This is a modified version of a class from the Android Open Source Project.
 * The original copyright and license information follows.
 *
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.motorola.contextual.smartrules.rulesbuilder;


import android.graphics.Rect;

/**
 * Interface defining an object that can receive a view at the end of a drag operation.
 * There can be an array of IBlockTarget attached to BlockLayer and the list defines/control
 * the drop zone where Blocks can be dropped and would remain there. All other visual zones
 * will reject the drop and place Block back to its point of origin/IBlockDrag
 */
public interface IBlockTarget {

    /**
     * Handle an object being dropped on the DropTarget
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
    void onDrop(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                BlockDragView dragView, Object dragInfo);

    void onDragEnter(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                     BlockDragView dragView, Object dragInfo);

    void onDragOver(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                    BlockDragView dragView, Object dragInfo);

    void onDragExit(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                    BlockDragView dragView, Object dragInfo);

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
    boolean acceptDrop(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                       BlockDragView dragView, Object dragInfo);

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
    Rect estimateDropLocation(IBlockDrag source, int x, int y, int xOffset, int yOffset,
                              BlockDragView dragView, Object dragInfo, Rect recycle);

    /**
     * Used to enable trashing of BlockView once it hits the RedZone / Coordinate of Trash
     */
    public void trashBlockView(Object dragInfo);

    // These methods are implemented in Views
    void getHitRect(Rect outRect);
    void getLocationOnScreen(int[] loc);
    int getLeft();
    int getTop();
}
