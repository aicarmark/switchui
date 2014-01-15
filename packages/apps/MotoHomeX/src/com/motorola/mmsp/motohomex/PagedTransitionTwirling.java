 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.view.animation.Transformation;

public class PagedTransitionTwirling extends PagedTransition {
    private static final PagedTransitionTwirling INSTANCE = new PagedTransitionTwirling();

    public static PagedTransitionTwirling getInstance() {
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
                c1.setTranslationX(0);
                c1.setRotationY(-180*pro);
                scale = 1f+pro*0.5f;
                c1.setScaleX(scale);
                c1.setScaleY(scale);
                c1.setAlpha(1-pro);
            }

            /* Modified by songshun.zhang for page transiton */
            if (c2 != null && (c1 != c2)) {
            /* Modified end */	
                c2.setCameraDistance(CAMERA_DISTANCE);
                c2.setTranslationX(delta-width);
                if (BOUNCE) {
                    c2.setRotationY(180 - 190*pro);
                } else {
                    c2.setRotationY(180 - 180*pro);
                }
                scale = 0.3f+pro*0.7f;
                c2.setScaleX(scale);
                c2.setScaleY(scale);
                c2.setAlpha(pro);
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
                c1.setTranslationX(0);
                c1.setRotationY(180*pro);
                scale = 1f+pro*0.5f;
                c1.setScaleX(scale);
                c1.setScaleY(scale);
                c1.setAlpha(1-pro);
            }

            if (c2 != null) {
                c2.setCameraDistance(CAMERA_DISTANCE);
                c2.setTranslationX(delta+width);
                if (BOUNCE) {
                    c2.setRotationY(190*pro-180);
                } else {
                    c2.setRotationY(180*pro-180);
                }
                scale = 0.3f+pro*0.7f;
                c2.setScaleX(scale);
                c2.setScaleY(scale);
                c2.setAlpha(pro);
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

    float a1[] = { 0f, 0.67f, 1f };
    float b1[] = { -10f, 5f,0f };
    float interp_left_current_rotate_y(float key) {
        return interp(a1, b1, key);
    }

    public class MyAnimation extends BounceAnimation {
        protected void applyTransformation(float interpolatedTime, Transformation t){
            if (c2 == null) return;
            if (left) {
                c2.setRotationY(interp_left_current_rotate_y(interpolatedTime));
            } else {
                c2.setRotationY(-interp_left_current_rotate_y(interpolatedTime));
            }
        }
    }
}
