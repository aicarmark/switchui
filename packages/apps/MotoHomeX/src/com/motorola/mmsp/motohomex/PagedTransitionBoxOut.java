 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.view.View;
import android.view.animation.Transformation;

public class PagedTransitionBoxOut extends PagedTransition {
    private static final PagedTransitionBoxOut INSTANCE = new PagedTransitionBoxOut();
    private static final float TRANSITION_ROTATION = 90f;

    public PagedTransitionBoxOut() {
        super();
    }

    public static PagedTransitionBoxOut getInstance() {
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
                        v.getWidth(), v.getHeight())*0.6f;
                rotation *= -1;

                if ( Math.abs(translationX) > 90) {
                    translationX *= 1f + 0.6f*(Math.abs(translationX)-90)/53;
                }

                if ( Math.abs(translationX) < 40) {
                    translationX *= 1f + 0.6f*(40-Math.abs(translationX))/40;
                }

                if (left) {
                    /* add by bvq783 for appmenu and workspace */
                    c1 = v;
                    c2 = pagedView.getPageAt(i + 1);
                    /*add by bvq783 end*/
                    if ( v == c2 ) {
                        v.setRotationY(rotation*1.2f-18);
                    }
                    else {
                        v.setRotationY(rotation);
                    }
                }
                else {
                    /* add by bvq783 for appmenu and workspace */
                    c1 = v;
                    c2 = pagedView.getPageAt(i - 1);
                    /*add by bvq783 end*/
                    if ( v == c2 ) {
                        v.setRotationY(rotation*1.2f+18);
                    }
                    else {
                        v.setRotationY(rotation);
                    }
                }

                v.setTranslationX(translationX);
                /* add by bvq783 for appmenu and workspace */
                c2 = v;
                /*add by bvq783 end*/
            }
        }

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

    float a1[] = { 0f, 3f, 5f, 6f };
    float b1[] = { -18f, 18f, -14f, 0f };
    float interp_left_current_rotate_y(float key) {
        return interp(a1, b1, key);
    }
    class MyAnimation extends BounceAnimation {
        protected void applyTransformation(float interpolatedTime, Transformation t){
            if (c2 == null) return;
            if (left) {
                c2.setRotationY(interp_left_current_rotate_y(interpolatedTime*6f));
            } else {
                c2.setRotationY(-interp_left_current_rotate_y(interpolatedTime*6f));
            }
        }
    }
}
