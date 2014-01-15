/*
 * @(#)IBlockDrag.java
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

import android.view.View;

import com.motorola.contextual.smartrules.rulesbuilder.BlockController;

/**
 * Interface defining an object where drag operations originate.
 * Similar to Adapter pattern, can assume any BlockLayer object
 */
public interface IBlockDrag {
    void setDragController(BlockController dragger);
    void onDropCompleted(View target, boolean success);
}
