 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import java.util.Arrays;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Animation.AnimationListener;

import android.util.Log;
import android.widget.ImageView;


public class EntryTransition {

    public void beginEntryTransition(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) { //int num) {
    }

    public void endEntryTransition(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
    }


    public void resetView(View v){
        if ( v != null) {
            v.clearAnimation();
            v.setPivotX(v.getMeasuredWidth() / 2);
            v.setPivotY(v.getMeasuredHeight() / 2);
            v.setTranslationX(0);
            v.setTranslationY(0);
            v.setRotationX(0);
            v.setRotationY(0);
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setAlpha(1f);
            v.setRotation(0);
            //v.setVisibility(View.VISIBLE);
        }
    }

    public void initUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
    	
    }

    public void resetUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
    	
    }
}
