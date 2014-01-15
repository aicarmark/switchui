/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.mmsp.motohomex;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
//Added by e13775 at 19 June 2012 for organize apps' group
import android.view.ViewGroup;
import android.widget.TabWidget;

public class FocusOnlyTabWidget extends TabWidget {
    public FocusOnlyTabWidget(Context context) {
        super(context);
    }

    public FocusOnlyTabWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusOnlyTabWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public View getSelectedTab() {
        final int count = getTabCount();
        for (int i = 0; i < count; ++i) {
            View v = getChildTabViewAt(i);
            if (v.isSelected()) {
                return v;
            }
        }
        return null;
    }

    public int getChildTabIndex(View v) {
        final int tabCount = getTabCount();
        for (int i = 0; i < tabCount; ++i) {
            if (getChildTabViewAt(i) == v) {
                return i;
            }
        }
        return -1;
    }

    public void setCurrentTabToFocusedTab() {
        View tab = null;
        int index = -1;
        final int count = getTabCount();
        for (int i = 0; i < count; ++i) {
            View v = getChildTabViewAt(i);
            if (v.hasFocus()) {
                tab = v;
                index = i;
                break;
            }
        }
        if (index > -1) {
            super.setCurrentTab(index);
            super.onFocusChange(tab, true);
        }
    }
    public void superOnFocusChange(View v, boolean hasFocus) {
        super.onFocusChange(v, hasFocus);
    }

    @Override
    public void onFocusChange(android.view.View v, boolean hasFocus) {
        if (v == this && hasFocus && getTabCount() > 0) {
            getSelectedTab().requestFocus();
            return;
        }
    }

    //Added by e13775 at 19 June 2012 for organize apps' group start
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // Call the super first to set the width of the tab
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        try{
            // Now, we are going to measure the second tab using wrap_content
            // The remained space is for the first tab
	    /*Added by ncqp34 at Jul-18-2012 for group switch*/
	    if(LauncherApplication.mGroupEnable){
                getChildAt(1).measure(ViewGroup.LayoutParams.WRAP_CONTENT, heightMeasureSpec);
                if ((getMeasuredWidth() - getChildAt(1).getMeasuredWidth()) < getChildAt(0).getMeasuredWidth()) {
                    getChildAt(0).getLayoutParams().width = getMeasuredWidth() - getChildAt(1).getMeasuredWidth();
                }
	    }else{
                getChildAt(0).measure(ViewGroup.LayoutParams.WRAP_CONTENT, heightMeasureSpec);
                getChildAt(1).measure(ViewGroup.LayoutParams.WRAP_CONTENT, heightMeasureSpec);
	    }
	    /*ended by ncqp34*/
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
//Added by e13775 at 19 June 2012 for organize apps' group end
