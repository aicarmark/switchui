 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.view.animation.Transformation;

public class PagedTransitionRotateSmooth extends PagedTransition {
    private static final PagedTransitionRotateSmooth INSTANCE = new PagedTransitionRotateSmooth();

    public static PagedTransitionRotateSmooth getInstance() {
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
                c1.setTranslationX(delta);  //c1.setTranslationX(delta);
                scale = interp_left_current_scale(pro);
                c1.setScaleX(scale);  //c1.setScaleX(scale);
                c1.setScaleY(scale);  //c1.setScaleY(scale);
                //2012-07-20, ChenYidong for SWITCHUI-2365
                c1.setAlpha(interp_left_current_alpha(pro));  //c1.setAlpha(interp_left_current_alpha(pro));
                c1.setPivotX(width/2);
            }

            /* Modified by songshun.zhang for page transiton */
            if (c2 != null && (c1 != c2)) {
            /* Modified end */		
                c2.setTranslationX(delta - width * (0.7f + 0.3f * pro));  //c2.setTranslationX(delta - width * (0.7f + 0.3f * pro));
                c2.setCameraDistance(CAMERA_DISTANCE);
                if (BOUNCE) {
                    //c2.setRotationY(interp_left_next_rotate_y(pro));
                    c2.setRotationY(interp_left_next_rotate_y(pro));
                } else {
                    //c2.setRotationY(interp_left_next_rotate_y_no_bounce(pro));
                    c2.setRotationY(interp_left_next_rotate_y_no_bounce(pro));
                }
                scale = interp_left_next_scale(pro);

                c2.setScaleX(scale);  //c2.setScaleX(scale);
                c2.setScaleY(scale);  //c2.setScaleY(scale);
                //2012-07-20, ChenYidong for SWITCHUI-2365
                c2.setAlpha(interp_left_next_alpha(pro));  //c2.setAlpha(interp_left_next_alpha(pro));
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
                c1.setTranslationX(delta);
                scale = interp_left_current_scale(pro);
                c1.setScaleX(scale);
                c1.setScaleY(scale);
                //2012-07-20, ChenYidong for SWITCHUI-2365
                c1.setAlpha(interp_left_current_alpha(pro));
                c1.setPivotX(width/2);
            }

            if (c2 != null) {
                c2.setTranslationX(delta + width * (0.7f + 0.3f * pro));
                c2.setCameraDistance(CAMERA_DISTANCE);
                if (BOUNCE) {
                    c2.setRotationY(-interp_left_next_rotate_y(pro));
                } else {
                    c2.setRotationY(-interp_left_next_rotate_y_no_bounce(pro));
                }
                scale = interp_left_next_scale(pro);
                c2.setScaleX(scale);
                c2.setScaleY(scale);
                //2012-07-20, ChenYidong for SWITCHUI-2365
                c2.setAlpha(interp_left_next_alpha(pro));
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

    float a1[] = { 0f, 0.6f, 1f };
    float b1[] = { 1f, 0.5f, 0.5f };
    float interp_left_current_scale(float key) {
        return interp(a1, b1, key);
    }

    float a2[] = { 0f, 0.6f, 1f };
    float b2[] = { 1f, 0f, 0f };
    float interp_left_current_alpha(float key) {
        return interp(a2, b2, key);
    }

    float a3[] = { 0f, 0.6f, 1f };
    float b3[] = { 1.2f, 1f, 1f };
    float interp_left_next_scale(float key) {
        return interp(a3, b3, key);
    }

    float a4[] = { 0f, 0.3f, 0.6f, 1f };
    float b4[] = { 0f, 0f, 1f, 1f };
    float interp_left_next_alpha(float key) {
        return interp(a4, b4, key);
    }

    float a5[] = { 0f, 0.3f, 1f };
    float b5[] = { 70f, 70f,-10f };
    float interp_left_next_rotate_y(float key) {
        return interp(a5, b5, key);
    }

    float a5_no_bounce[] = { 0f, 0.3f, 1f };
    float b5_no_bounce[] = { 70f, 70f, 0f };
    float interp_left_next_rotate_y_no_bounce(float key) {
        return interp(a5_no_bounce, b5_no_bounce, key);
    }

    float a6[] = { 0f, 0.7f, 1f };
    float b6[] = { 10f, -3f, 0f };
    float interp_left_next_rotate_y_bounce(float key) {
        return interp(a6, b6, key);
    }

    class MyAnimation extends BounceAnimation {
        protected void applyTransformation(float interpolatedTime,
                Transformation t) {
            if (c2 == null) return;
            if (left) {
                c2.setRotationY(-interp_left_next_rotate_y_bounce(interpolatedTime));
            } else {
                c2.setRotationY(interp_left_next_rotate_y_bounce(interpolatedTime));
            }
        }
    }
}
