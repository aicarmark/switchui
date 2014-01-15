package com.motorola.quicknote;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;


public class DropdownButton extends Button {
	boolean mDropdownable = false;

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
	}

	public DropdownButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (mDropdownable)
			setBackgroundResource(R.drawable.spinner_default_holo_dark);
	}

	public DropdownButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (mDropdownable)
			setBackgroundResource(R.drawable.spinner_default_holo_dark);
	}

	public DropdownButton(Context context) {
		super(context);
		if (mDropdownable)
			setBackgroundResource(R.drawable.spinner_default_holo_dark);
	}

	private PopupMenu mPopupMenu;

	public void setupMenu(int menuId, OnMenuItemClickListener clickListener) {

		mPopupMenu = new PopupMenu(getContext(), this);
		Menu menu = mPopupMenu.getMenu();
		mPopupMenu.getMenuInflater().inflate(menuId, menu);
		mPopupMenu.setOnMenuItemClickListener(clickListener);

		this.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mDropdownable)
					mPopupMenu.show();
			}
		});
	}

	public void setTitle(String title) {
		this.setText(title);
	}

	public PopupMenu getPopupMenu() {
		return mPopupMenu;
	}

	public void setDropdownable(boolean bDropdownable) {
		mDropdownable = bDropdownable;
		if (mDropdownable) {
			setBackgroundResource(R.drawable.spinner_default_holo_dark);
		} else {
			setBackgroundResource(0);
		}
	}

	public boolean getDropdownable() {
		return mDropdownable;
	}
}
