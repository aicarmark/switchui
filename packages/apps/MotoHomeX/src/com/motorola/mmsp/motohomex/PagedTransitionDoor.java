 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.view.animation.Transformation;

public class PagedTransitionDoor extends PagedTransition {
    private static final PagedTransitionDoor INSTANCE = new PagedTransitionDoor();

    public static PagedTransitionDoor getInstance() {
        return INSTANCE;
    }

    public void pageBeginMoving(PagedView pagedView) {
        super.pageBeginMoving(pagedView);
    }

    public void pageEndMoving(final PagedView pagedView) {
        /*2012-05-22, Chen Yidong for SWITCHUI-1201*/
        super.pageEndMoving(pagedView);
        /*2012-05-22, end*/
    }

    public void screenScrolled(PagedView pagedView, int screenCenter) {
        if (c2!=null) c2.clearAnimation();
        if (overScrolled(pagedView, screenCenter)) return;
        super.screenScrolled(pagedView, screenCenter);
        if (Math.abs(pro)>1.0f) return;

        if (left) {
            /* changed by bvq783 for appmenu and workspace */
            //c1 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage);
            //c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage+1);
            c1 =  pagedView.getPageAt(pagedView.mCurrentPage);
            c2 =  pagedView.getPageAt(pagedView.mCurrentPage+1);
            /* chenged by bvq783 end */

            if (c1 != null) {
                c1.setCameraDistance(CAMERA_DISTANCE);
                c1.setTranslationX(delta);   //setTranslationX(delta);
                c1.setRotationY(interp_left_current_rotate_y(pro));  //setRotationY(interp_left_current_rotate_y(pro));
                c1.setPivotX(0);
            }

            /* Modified by songshun.zhang for page transiton */
            if (c2 != null && (c1 != c2)) {
            /* Modified end */	
                c2.setCameraDistance(CAMERA_DISTANCE);
                c2.setTranslationX(delta-width);  //setTranslationX(delta-width);
                if (BOUNCE) {
                    //c2.setRotationY(interp_left_next_rotate_y(pro));
                    c2.setRotationY(interp_left_next_rotate_y(pro));
                } else {
                    //c2.setRotationY(interp_left_next_rotate_y_no_bounce(pro));
                    c2.setRotationY(interp_left_next_rotate_y_no_bounce(pro));
                }
                c2.setPivotX(width);
            }
        } else {
            /* changed by bvq783 for appmenu and workspace */
            //c1 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage);
            //c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage-1);
            c1 =  pagedView.getPageAt(pagedView.mCurrentPage);
            c2 =  pagedView.getPageAt(pagedView.mCurrentPage-1);
            /* chenged by bvq783 end */
            pro *= -1;

            if (c1 != null) {
                c1.setCameraDistance(CAMERA_DISTANCE);
                c1.setTranslationX(delta);   //c1.setTranslationX(delta);
                c1.setRotationY(-interp_left_current_rotate_y(pro)); //c1.setRotationY(-interp_left_current_rotate_y(pro));
                c1.setPivotX(width);
            }

            if (c2 != null) {
                c2.setCameraDistance(CAMERA_DISTANCE);
                c2.setTranslationX(delta+width);  //c2.setTranslationX(delta+width);
                if (BOUNCE) {
                    //c2.setRotationY(-interp_left_next_rotate_y(pro));
                    c2.setRotationY(-interp_left_next_rotate_y(pro));
                } else {
                    //c2.setRotationY(-interp_left_next_rotate_y_no_bounce(pro));
                    c2.setRotationY(-interp_left_next_rotate_y_no_bounce(pro));
                }
                c2.setPivotX(0);
            }
        }

        if (!BOUNCE) return;

        if (Math.abs(delta) == width) {
            if ( ani == null) {
                ani = new MyAnimation();
            }
            if (!ani.play) {
                ani.reset();
                if (c2 != null)
                    c2.startAnimation(ani);
            }
        }
    }

    float a1[] = { 0f, 0.7f, 1f };
    float b1[] = { 0f, -90f, -90f };
    float interp_left_current_rotate_y(float key) {
        return interp(a1, b1, key);
    }

    float a2[] = { 0f, 0.25f, 1f };
    float b2[] = { -90f, -90f, 5f };
    float interp_left_next_rotate_y(float key) {
        return interp(a2, b2, key);
    }

    float a2_no_bounce[] = { 0f, 0.25f, 1f };
    float b2_no_bounce[] = { -90f, -90f, 0f };
    float interp_left_next_rotate_y_no_bounce(float key) {
        return interp(a2_no_bounce, b2_no_bounce, key);
    }

    float a3[] = { 0f, 0.67f, 1f };
    float b3[] = { 5f, -3f, 0f };
    float interp_left_next_rotate_y_bounce(float key) {
        return interp(a3, b3, key);
    }

    class MyAnimation extends BounceAnimation {
        protected void applyTransformation(float interpolatedTime, Transformation t){
            if (c2 == null) return;
            if (left) {
                c2.setRotationY(interp_left_next_rotate_y_bounce(interpolatedTime));
            } else {
                c2.setRotationY(-interp_left_next_rotate_y_bounce(interpolatedTime));
            }
        }
    }
}
