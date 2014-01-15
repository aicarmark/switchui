/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: ScreenResponseHandler.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Feb 17, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

import android.view.View;
import android.widget.SeekBar;

/**
 * @author A24178
 *
 */

// package-scope used to abstract screen handling classes responsible for 
// preset and custom screens
abstract class ScreenResponseHandler implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    
    public abstract void onClick(View v);
    public abstract void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);
    public abstract void onStartTrackingTouch(SeekBar seekBar);
    public abstract void onStopTrackingTouch(SeekBar seekBar);
}
