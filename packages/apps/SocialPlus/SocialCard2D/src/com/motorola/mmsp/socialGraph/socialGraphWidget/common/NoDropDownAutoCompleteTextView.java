package com.motorola.mmsp.socialGraph.socialGraphWidget.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class NoDropDownAutoCompleteTextView extends AutoCompleteTextView {
	public NoDropDownAutoCompleteTextView(Context context) {
		super(context);
	}
	public NoDropDownAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public NoDropDownAutoCompleteTextView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void showDropDown() {
		
	}
}
