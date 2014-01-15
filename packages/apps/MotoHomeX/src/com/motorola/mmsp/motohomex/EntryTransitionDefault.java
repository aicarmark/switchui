 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class EntryTransitionDefault extends EntryTransition{
       private static final EntryTransitionDefault INSTANCE = new EntryTransitionDefault();

       public EntryTransitionDefault() {
           super();
       }

       public static EntryTransitionDefault getInstance() {     
           return INSTANCE;
       }

       public void initUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
       	
       }

       public void resetUnlockPage(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
           setEntryTransitionState(cell, qsb, hotseat, bg);
       }

       public void beginEntryTransition(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) { //int num) {
           setEntryTransitionState(cell, qsb, hotseat, bg);
       }

       public void endEntryTransition(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
           setEntryTransitionState(cell, qsb, hotseat, bg);
       }

       private void setEntryTransitionState(View cell, SearchDropTargetBar qsb, Hotseat hotseat, ImageView bg) {
           cell.setVisibility(View.VISIBLE);
           cell.setAlpha(1f);
           cell.setScaleX(1f);
           cell.setScaleY(1f);
           qsb.setVisibility(View.VISIBLE);
           qsb.setAlpha(1f);
           qsb.setScaleX(1f);
           qsb.setScaleY(1f);
           hotseat.setVisibility(View.VISIBLE);
           hotseat.setAlpha(1f);
           bg.setVisibility(View.VISIBLE);
           bg.setAlpha(1f);
       }
}
