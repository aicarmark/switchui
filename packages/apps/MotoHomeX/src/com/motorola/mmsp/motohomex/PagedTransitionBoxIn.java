 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.view.View;
import android.view.animation.Transformation;

public class PagedTransitionBoxIn extends PagedTransition {
    private static final PagedTransitionBoxIn INSTANCE = new PagedTransitionBoxIn();
    private static final float TRANSITION_ROTATION = 120f;

    public PagedTransitionBoxIn() {
        super();
        BOUNCE = false;
    }

    public static PagedTransitionBoxIn getInstance() {
        return INSTANCE;
    }

    public void screenScrolled(PagedView pagedView, int screenCenter) {
        if (c2!=null) c2.clearAnimation();
        if (overScrolled(pagedView, screenCenter)) return;
        super.screenScrolled(pagedView, screenCenter);
        if (Math.abs(pro)>1.0f) return;

        for (int i = 0; i < pagedView.getPageCount(); i++) {
            View v = pagedView.getPageAt(i);
            if (v != null) {
                float scrollProgress = pagedView.getScrollProgress(
                        screenCenter, v, i);
                if (scrollProgress == 1.0f || scrollProgress == -1.0f) continue;

                v.setCameraDistance(pagedView.mDensity * CAMERA_DISTANCE);

                float rotation = TRANSITION_ROTATION * scrollProgress;
                float translationX = getOffsetXForRotation(rotation,
                        v.getWidth(), v.getHeight());

                if ( Math.abs(translationX) > 150) {
                    translationX *= 1f + 0.3f*(Math.abs(translationX)-150)/90;
                } else if ( Math.abs(translationX) < 70) {
                    translationX *= 1f + 0.3f*(70-Math.abs(translationX))/70;
                }

                if (left) {
                    /* add by bvq783 for appmenu and workspace */
                    c1 = v;
                    c2 = pagedView.getPageAt(pagedView.mCurrentPage + 1);
                    /*add by bvq783 end*/
                    if ( v == c2 && BOUNCE) {
                        v.setRotationY(rotation*1.2f+15);
                        //v.setFastRotationY(rotation*1.2f+15);
                    }
                    else {
                        v.setRotationY(rotation);
                        //v.setFastRotationY(rotation);
                    }
                }
                else {
                    /* add by bvq783 for appmenu and workspace */
                    c1 = v;
                    c2 = pagedView.getPageAt(pagedView.mCurrentPage - 1);
                    /*add by bvq783 end*/
                    if ( v == c2 && BOUNCE) {
                        v.setRotationY(rotation*1.2f-15);
                        //v.setFastRotationY(rotation*1.2f-15);
                    }
                    else {
                        v.setRotationY(rotation);
                        //v.setFastRotationY(rotation);
                    }
                }

                v.setTranslationX(translationX*0.2f);
                /* add by bvq783 for appmenu and workspace */
                c2 = v;
                /*add by bvq783 end*/
            }
        }

        if (!BOUNCE) return;

        if (Math.abs(delta) == width) {
            if ( ani == null) {
                ani = new MyAnimation();
            }

            if (!ani.play) {
                if (c2 != null)
                    c2.startAnimation(ani);
            }
        }
    }

    float a1[] = { 0f, 1f };
    float b1[] = { -15f, 0f };
    float interp_left_current_rotate_y(float key) {
        return interp(a1, b1, key);
    }

    class MyAnimation extends BounceAnimation {
        protected void applyTransformation(float interpolatedTime, Transformation t){
            if (c2 == null) return;
            if (left) {
                c2.setRotationY(-interp_left_current_rotate_y(interpolatedTime));
            } else {
                c2.setRotationY(+interp_left_current_rotate_y(interpolatedTime));
            }
        }
    }
}
