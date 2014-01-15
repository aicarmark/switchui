 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.Resources;


import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.util.Log;
/*2012-7-6, add by bvq783 for switchui-1936*/
import android.content.Intent;
import android.view.ViewParent;
/*2012-7-6, add by bvq783 end*/

public class EntryTransitionFly extends EntryTransition{
    private static final EntryTransitionFly INSTANCE = new EntryTransitionFly();
    private View cell = null;
    private Hotseat mHot;
    private SearchDropTargetBar mQSB;
    private ImageView mHotseatBk;
    /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
    //private static final int QSB_SEARCH_BAR = 0;
    /*2012-07-02, Hu ShuAn end*/
    /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
    private Animator cellAnimator = null;
    private Animator hotAnimator = null;
    /*2012-7-2, add by bvq783 end*/
    public EntryTransitionFly() {
        super();
    }

    public static EntryTransitionFly getInstance() {     
        return INSTANCE;
    }


   public void initUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
	   hotseat.setVisibility(View.INVISIBLE);
	   cell.setAlpha(0f); //setVisibility(View.INVISIBLE);
       //mWorkspace.setVisibility(View.INVISIBLE);
	   /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
       /*2012-07-03, add by Hu ShuAn for switchui-1818*/
	   if(!qsb.getIsQSBarHide()) {
       /*2012-07-03, add end*/
		   qsb.setVisibility(View.INVISIBLE);
	   }
	   /*2012-07-02, Hu ShuAn end*/
	   bg.setVisibility(View.INVISIBLE);   	
   }

   public void resetUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
	   qsb.setVisibility(View.VISIBLE);
	   qsb.setAlpha(1f);
	   bg.setVisibility(View.VISIBLE);
	   bg.setAlpha(1f);
       hotseat.setVisibility(View.VISIBLE);
       hotseat.setAlpha(1f);
       cell.setAlpha(1f);
       /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
       cellAnimator = null;
       hotAnimator = null;
       /*2012-7-2, add by bvq783 end*/
   }

   public void endEntryTransition(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
       /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
       /*resetView(qsb);
       resetView(cell);
       resetView(hotseat);
       resetView(bg);*/
       if (cellAnimator != null) {
           cellAnimator.cancel();
           cellAnimator = null;
       }
       if (hotAnimator != null) {
           hotAnimator.cancel();
           hotAnimator = null;
       }
       /*2012-7-2, add by bvq783 end*/
   }

   public void beginEntryTransition(View celllayout, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
	   this.cell = celllayout;
	   this.mQSB = qsb;
	   this.mHotseatBk = bg;
	   this.mHot = hotseat;
       final ValueAnimator ani = ValueAnimator.ofFloat(0f, 1f)
               .setDuration(300);
       /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
       if (cellAnimator != null)
           cellAnimator.cancel();
       cellAnimator = ani;
       /*2012-7-2, add by bvq783 end*/
       /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
       /*2012-07-03, add by Hu ShuAn for switchui-1818*/
       final boolean flag = !mQSB.getIsQSBarHide();
       /*2012-07-03, add end*/
       /*2012-07-02, Hu ShuAn end*/
       
       ani.addUpdateListener(new LauncherAnimatorUpdateListener() {
       	   float scale = 0.7f;
       	   float alpha = 0.5f;

           public void onAnimationUpdate(float a, float b) {
           	//Log.d("July", "update animator, a:"+a+", b:"+b);
        	   cell.setScaleX(b+(1-b)*scale);
        	   cell.setScaleY(b+(1-b)*scale);
        	   cell.setAlpha(b+(1-b)*alpha);
        	   /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
        	   if(flag){
        		   mQSB.setScaleX(b+(1-b)*scale);
        		   mQSB.setScaleY(b+(1-b)*scale);
        		   mQSB.setAlpha(b+(1-b)*alpha);
        	   }
        	   /*2012-07-02, Hu ShuAn end*/
           }        	
       });
       ani.addListener(new AnimatorListener() {
           /*2012-07-05, added by ChenYidong for SWITCHUI-1923*/
           float oldPX;
           float oldPY;
           /*2012-07-05, added end*/
           @Override
           public void onAnimationCancel(Animator animation) {
        	   /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
        	   if(flag){
        		   mQSB.setScaleX(1f);
        		   mQSB.setScaleY(1f);
        		   mQSB.setAlpha(1f);
        	   }
        	   /*2012-07-02, Hu ShuAn end*/
        	   cell.setScaleX(1f);
        	   cell.setScaleY(1f);
               cell.setAlpha(1f);
               /*2012-07-05, added by ChenYidong for SWITCHUI-1923*/
               cell.setPivotX(oldPX);
               cell.setPivotY(oldPY);
               /*2012-07-05, added end*/
           }
           @Override
           public void onAnimationStart(Animator animation) {
               /*2012-07-05, added by ChenYidong for SWITCHUI-1923*/
               oldPX = cell.getPivotX();
               oldPY = cell.getPivotY();
               /*2012-07-05, added end*/
               //v1.setAlpha(0f);
               cell.setVisibility(View.VISIBLE);
               //v2.setAlpha(0f);
           	   float px = 240; //mDragLayer.getWidth()/2f;
           	   float py = 400; //mDragLayer.getHeight()/2f;
               cell.setScaleX(0.7f);
               cell.setScaleY(0.7f);
               cell.setAlpha(0.5f);
           	   cell.setPivotX(px);
           	   cell.setPivotY(py);      	
           	   
           	/*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
           	   if(flag){
           		mQSB.setVisibility(View.VISIBLE);
           		mQSB.setScaleX(0.7f);
           		mQSB.setScaleY(0.7f);
           		mQSB.setAlpha(0.5f);
           		mQSB.setPivotX(px);
           		mQSB.setPivotY(py);
           	   }
           	/*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
           }
           @Override
           public void onAnimationRepeat(Animator animation) {}
           @Override
           public void onAnimationEnd(Animator animation) {
               cell.setScaleX(1f);
               cell.setScaleY(1f);
               cell.setAlpha(1f);
               cell.setVisibility(View.VISIBLE);
               /*2012-07-02, add by Hu ShuAn for SWITCHUI-1689*/
               /*2012-07-05, added by ChenYidong for SWITCHUI-1923*/
               cell.setPivotX(oldPX);
               cell.setPivotY(oldPY);
               /*2012-07-05, added end*/
               if(flag){
            	   mQSB.setScaleX(1f);
            	   mQSB.setScaleY(1f);
            	   mQSB.setVisibility(View.VISIBLE);
            	   mQSB.setAlpha(1f);
               }
               /*2012-07-02, Hu ShuAn end*/
           }
       });
       ani.start();
       showHotSeatAnimation();
   }

   public void showHotSeatAnimation() {
       AnimatorSet set = new AnimatorSet();
       /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
       if (hotAnimator != null)
           hotAnimator.cancel();
       hotAnimator = set;
       /*2012-7-2, add by bvq783 end*/
       int h = mHot.getHeight();
       ObjectAnimator anim1 = ObjectAnimator.ofFloat(mHot, "translationY", 50, -15);
       ObjectAnimator anim2 = ObjectAnimator.ofFloat(mHot, "translationY", -15, 0);
       anim1.setDuration(150);
       anim2.setDuration(300);
       anim1.addUpdateListener(new AnimatorUpdateListener() {
	     public void onAnimationUpdate(ValueAnimator animation) {
		    // TODO Auto-generated method stub
		    long time = animation.getCurrentPlayTime();
            float rat = time/450f;
            float alpha = time/150f;               
            mHotseatBk.setAlpha(rat);
            mHot.setAlpha(alpha);
		  }
       });
       anim2.addUpdateListener(new AnimatorUpdateListener() {
			public void onAnimationUpdate(ValueAnimator animation) {
				// TODO Auto-generated method stub
			   long time = animation.getCurrentPlayTime();
               float rat = (time+150)/450f;
               mHotseatBk.setAlpha(rat);

			}
       });
       set.play(anim2).after(anim1);
       set.addListener(listener);
       set.setStartDelay(50);
       set.start();
   }

   private AnimatorListener listener = new AnimatorListener() {

       @Override
       public void onAnimationEnd(Animator animation) {
           if (mHot!= null) {
        	mHotseatBk.setVisibility(View.VISIBLE);
        	mHotseatBk.setAlpha(1f);
           	mHot.setVisibility(View.VISIBLE);
           	mHot.setAlpha(1f);
           }
           /*2012-7-6, add by bvq783 for switchui-1936*/
           hotAnimator = null;
           ViewParent w = cell.getParent();
           if (w instanceof Workspace) {
               Workspace parent = (Workspace)w;
               Launcher launcher = parent.getLauncher();
               Intent intent = new Intent("com.motorola.mmsp.motohome.unlockfinished");
               launcher.sendBroadcast(intent);
           }           
           /*2012-7-6, add by bvq783 for switchui-1936*/
       }
       @Override
       public void onAnimationStart(Animator animation) {
       	if (mHot!= null) {
       	    //mHotseat.getBackground().setAlpha(0);
       	    //mHotseat.setAlpha(1f);
                 //hotseatbg.setAlpha(1f);
       		mHotseatBk.setVisibility(View.VISIBLE);
       		mHotseatBk.setAlpha(0f);
            mHot.setVisibility(View.VISIBLE);
            mHot.setAlpha(0f);
       	}
       }
       @Override
       public void onAnimationRepeat(Animator animation) {
       }
       @Override
       public void onAnimationCancel(Animator animation) {
           if (mHot!= null) {
        	  mHotseatBk.setVisibility(View.VISIBLE);
        	  mHotseatBk.setAlpha(1f);
           	  mHot.setVisibility(View.VISIBLE);
           	  mHot.setAlpha(1f);
                  /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
                  mHot.setTranslationY(0);
                  /*2012-7-2, add by bvq783 end*/
           }
           hotAnimator = null;
       }
  };
}
