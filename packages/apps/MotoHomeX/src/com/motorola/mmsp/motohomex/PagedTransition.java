 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import java.util.Arrays;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Animation.AnimationListener;

public class PagedTransition {
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    static float CAMERA_DISTANCE = 6500;
    boolean BOUNCE = true;

    int width,height;
    /* add by bvq783 for app menu transition*/
    //CellLayout c1, c2;
    View c1, c2;
    /* add by bvq783 end */
    PagedView pv;
    /*2012-11-27, Added by amt_chenjing for SWITCHUITWO-135*/
    public BounceAnimation ani;
    /*2012-11-27, Add end*/

    int mScreenCenter, delta;
    boolean left;
    float pro, scale, rotate;

    // Camera and Matrix used to determine the final position of a neighboring
    // CellLayout
    private final Matrix mMatrix = new Matrix();
    private final Camera mCamera = new Camera();
    private final float mTempFloat2[] = new float[2];

    /**
     * Due to 3D transformations, if two CellLayouts are theoretically touching
     * each other, on the xy plane, when one is rotated along the y-axis, the
     * gap between them is perceived as being larger. This method computes what
     * offset the rotated view should be translated in order to minimize this
     * perceived gap.
     *
     * @param degrees
     *            Angle of the view
     * @param width
     *            Width of the view
     * @param height
     *            Height of the view
     * @return Offset to be used in a View.setTranslationX() call
     */
    public float getOffsetXForRotation(float degrees, int width, int height) {
        mMatrix.reset();
        mCamera.save();
        mCamera.rotateY(Math.abs(degrees));
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        mMatrix.preTranslate(-width * 0.5f, -height * 0.5f);
        mMatrix.postTranslate(width * 0.5f, height * 0.5f);
        mTempFloat2[0] = width;
        mTempFloat2[1] = height;
        mMatrix.mapPoints(mTempFloat2);
        return (width - mTempFloat2[0]) * (degrees > 0.0f ? 1.0f : -1.0f);
    }

    public void resetTransforms(View v) {
        resetTransforms(v, true);
    }
    public void resetTransforms(View v, boolean left) {
        if (v == null) return;
        v.setTranslationX(0); // setTranslationX(0);
        v.setRotationY(0);  // setRotationY(0)
        /*2012-04-20, Chen Yidong for SWITCHUI-719*/
        if ( v instanceof CellLayout ) {
            ((CellLayout)v).setOverScrollAmount(0, left);
        }
        /*2012-04-20, end*/
        v.setPivotX(v.getMeasuredWidth() / 2);
        v.setPivotY(v.getMeasuredHeight() / 2);
    }

    public boolean overScrolled(PagedView pagedView, int screenCenter) {
        if (pagedView.mOverScrollX < 0
                || pagedView.mOverScrollX > pagedView.mMaxScrollX) {
            int i = pagedView.mOverScrollX < 0 ? 0
                    : pagedView.getPageCount() - 1;
            View v = pagedView.getPageAt(i);
            if (v == null) return true;

            float scrollProgress = pagedView.getScrollProgress(
                    screenCenter, v, i);

            if ( v instanceof CellLayout ) {
                ((CellLayout)v).setOverScrollAmount(Math.abs(scrollProgress), i == 0);
            }

            v.setCameraDistance(CAMERA_DISTANCE);
            int pageWidth = v.getMeasuredWidth();

            v.setTranslationX(0); //setTranslationX(0);
            v.setTranslationY(0);  //setTranslationY(0);
            v.setScaleX(1f);  //setScaleX(1f);
            v.setScaleY(1f);  //setScaleY(1f);
            v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
            v.setAlpha(1f);    //setAlpha(1f);
            //setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
            v.setPivotX(pageWidth * (i == 0 ? TRANSITION_PIVOT : (1 - TRANSITION_PIVOT)));
            return true;
        } else {
            resetTransforms(pagedView.getPageAt(0),true);
            resetTransforms(pagedView.getPageAt(pagedView.getPageCount() - 1),false);
        }
        return false;
    }

    public void screenScrolled(PagedView pagedView, int screenCenter) {
        mScreenCenter = pagedView.mCurrentPage*width + width/2;
        /*2012-06-15, Chen Yidong add for SWITCHUI-1515, refer pagedView.getScrollProgress*/
        final int halfScreenSize = pagedView.getMeasuredWidth() / 2;
        int index = pagedView.mCurrentPage;
        int totalDistance = pagedView.getScaledMeasuredWidth(
                pagedView.getPageAt(index)) + pagedView.mPageSpacing;
        width = totalDistance;
        mScreenCenter = pagedView.getChildOffset(index) -
                pagedView.getRelativeChildOffset(index) + halfScreenSize;
        /*2012-06-15, end*/
        if ( screenCenter > mScreenCenter ) {
            left = true;
            /* add by bvq783 for app menu transition*/
            //c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage+1);
            c2 = pagedView.getChildAt(pagedView.mCurrentPage+1);
            /* add by bvq783 end */
        } else if ( screenCenter < mScreenCenter ) {
            left = false;
            /* add by bvq783 for app menu transition*/
            //c2 = (CellLayout) pagedView.getChildAt(pagedView.mCurrentPage-1);
            c2 = pagedView.getChildAt(pagedView.mCurrentPage-1);
            /* add by bvq783 end */
        } else {
            /*2012-05-10, added by Chen Yidong for SWITCHUI-1046*/
            delta = 0;
            pro = 0f;
            /*2012-05-10, added end*/
            return;
        }
        /* add by bvq783 for app menu transition*/
        if(mScreenCenter%2 != 0){
        	mScreenCenter += 1;
        }
        /* add by bvq783 end */
        delta = screenCenter - mScreenCenter;
        pro = delta/(float)width;
    }

    public void pageBeginMoving(PagedView pagedView) {
        pv = pagedView;
        width = pv.getPageAt(0).getMeasuredWidth();
        height = pv.getPageAt(0).getMeasuredHeight();
    }

    /*2012-07-22, Chen Yidong for SWITCHUI-2388*/
    public void pageEndMoving(PagedView pagedView) {
        /*2012-05-22, Chen Yidong for SWITCHUI-1201*/
        if(ani == null || !ani.play){
            //if(pv != null){
            //    for (int i = 0; i < pv.getPageCount(); i++) {
            //        resetView(pv.getPageAt(i));
            //    }
            //}
            resetPagedView(pagedView);
        }
        /*2012-05-22, end*/
    }

    public void resetPagedView(PagedView pagedView) {
        if(pagedView != null){
            for (int i = 0; i < pagedView.getPageCount(); i++) {
                resetView(pagedView.getPageAt(i));
            }
        }
    }
    /*2012-07-22, end*/

    public void resetView(View v){
        if ( v != null) {
            v.clearAnimation();
            v.setTranslationX(0);
            v.setTranslationY(0);
            v.setRotationY(0);
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setAlpha(1f);
            v.setPivotX(v.getMeasuredWidth() / 2);
            v.setPivotY(v.getMeasuredHeight() / 2);
        }
    }

    //Map the value between a[i] and a[i+1] to b[i] and b[i+1]
    static public float interp(float a[], float b[], float key) {

        int i = Arrays.binarySearch(a, key);

        if ( i >= 0 ) {
            return b[i];
        } else {
            i = -(i+1);
        }

        if ( i == 0 ) {
            return b[0];
        } else if ( i == b.length) {
            return b[i-1];
        }

        float value = b[i-1]+ (b[i]- b[i-1])*(key-a[i-1])/(a[i]-a[i-1]);

        return value;
    }

    static class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t = 1f-t;
            return 1f-t*t;
        }
    }

    public class BounceAnimation extends Animation implements AnimationListener {
        public boolean play = false;
        public BounceAnimation() {
            setDuration(500);
            setInterpolator(new ScrollInterpolator());
            setAnimationListener(this);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // TODO Auto-generated method stub
            play = true;
        }
        @Override
        public void onAnimationEnd(Animation animation) {
            // TODO Auto-generated method stub
            play = false;
            /*2012-07-19, ChenYidong changed for SWITCHUI-2334*/
            if(pv != null){
                for (int i = 0; i < pv.getPageCount(); i++) {
                    resetView(pv.getPageAt(i));
                }
            }
            /*2012-07-19, end*/
        }
        @Override
        public void onAnimationRepeat(Animation animation) {
            // TODO Auto-generated method stub

        }
    }
}
