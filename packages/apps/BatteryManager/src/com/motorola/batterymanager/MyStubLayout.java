/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: MyStubLayout.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Feb 15, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.motorola.batterymanager.R;

/**
 * @author A24178
 *
 */
// package-scope
class MyStubLayout extends LinearLayout {

    /**
     * @param context
     */
    public MyStubLayout(Context context) {
        super(context);
        customInit(context);
    }
    
    public MyStubLayout(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        customInit(context);
    }
    
    private void customInit(Context context) {
        setOrientation(VERTICAL);
        
        LayoutInflater inflater = (LayoutInflater) 
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.customs_stub_view, null);
        addView(v);
    }
}
