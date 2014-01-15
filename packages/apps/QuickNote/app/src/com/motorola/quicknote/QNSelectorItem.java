package com.motorola.quicknote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

class QNSelectorItem extends FrameLayout implements QNSelector.Item_Interface {
	private ImageView _v;
	private Drawable _dup;
	private Drawable _ddown;
	private Drawable _bgup;
	private Drawable _bgdown;
	private Drawable _ddetail;
	private int _index;

	public QNSelectorItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClickable(true); // selector item should be clickable!

		_v = new ImageView(context);
		_v.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		_v.setAdjustViewBounds(true);
		_v.setScaleType(ScaleType.FIT_CENTER);
		addView(_v);

		_index = attrs.getAttributeIntValue(QNConstants.XMLNS, "index", -1);

		int rid;
		rid = attrs.getAttributeResourceValue(QNConstants.XMLNS, "src_up", -1);
		if (-1 != rid) {
			_dup = context.getResources().getDrawable(rid);
		}

		rid = attrs
				.getAttributeResourceValue(QNConstants.XMLNS, "src_down", -1);
		if (-1 != rid) {
			_ddown = context.getResources().getDrawable(rid);
		}

		rid = attrs.getAttributeResourceValue(QNConstants.XMLNS, "src_detail",
				-1);
		if (-1 != rid) {
			_ddetail = context.getResources().getDrawable(rid);
		}

		rid = attrs.getAttributeResourceValue(QNConstants.XMLNS, "bg_up", -1);
		if (-1 != rid) {
			_bgup = context.getResources().getDrawable(rid);
		}

		rid = attrs.getAttributeResourceValue(QNConstants.XMLNS, "bg_down", -1);
		if (-1 != rid) {
			_bgdown = context.getResources().getDrawable(rid);
		}

		_v.setImageDrawable(_dup);

	}

	public boolean isContain(int x, int y) {
		/*** For easy test ***/
		Rect r = new Rect();
		r.left = getLeft();
		r.top = getTop();
		r.right = getRight();
		r.bottom = getBottom();
		return r.contains(x, y);
	}

	public int index() {
		return _index;
	}

	public Drawable detail() {
		return _ddetail;
	}

	public void item_touched(boolean bTouched) {
		_v.setImageDrawable(null);
		if (bTouched) {
			if (null != _ddown) {
				_v.setImageDrawable(_ddown);
			}
			if (null != _bgdown) {
				_v.setBackgroundDrawable(_bgdown);
			}
		} else {
			if (null != _dup) {
				_v.setImageDrawable(_dup);
			}
			if (null != _bgup) {
				_v.setBackgroundDrawable(_bgup);
			}
		}
	}
}
