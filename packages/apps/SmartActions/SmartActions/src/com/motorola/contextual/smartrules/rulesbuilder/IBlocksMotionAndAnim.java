/*
 * @(#)BlockMotionAndAnim.java
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


/**
 * 
 * A place holder Interface to hold values that will not change for
 * the duration of Graphics activity. These values would be used for all the Blocks
 * for BlockGestureListener for responding to touch based Gestures
 *
 */
public interface IBlocksMotionAndAnim {

		/*
		 * Callbacks to implement overriding some default behavior for Block user interface gestures.
		 * The events and related gestures are delegated to child and this needs to be implemented
		 * to override the behavior in specific use cases.
		 */
        public void setPressedState (boolean pressedState);
        public void setTapState(boolean tapState);


}
