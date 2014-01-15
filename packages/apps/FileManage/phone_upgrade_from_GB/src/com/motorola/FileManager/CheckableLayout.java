package com.motorola.FileManager;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

    public CheckableLayout(Context context) {
	super(context);
    }

    public CheckableLayout(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
	final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	if (isChecked()) {
	    mergeDrawableStates(drawableState, CHECKED_STATE_SET);
	}
	return drawableState;
    }

    @Override
    public void setChecked(boolean checked) {
	if (mChecked != checked) {
	    mChecked = checked;
	    refreshDrawableState();

	    CheckBox cb = (CheckBox) findViewById(R.id.checkbox);
	    if (cb != null) {
		cb.setChecked(checked);
	    }
	}
    }

    @Override
    public boolean isChecked() {
	// TODO Auto-generated method stub
	return mChecked;
    }

    @Override
    public void toggle() {
	// TODO Auto-generated method stub
	setChecked(!mChecked);
    }
}
