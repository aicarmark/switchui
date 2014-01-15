package com.motorola.quicknote;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/***************************************************************************
 * All item should be View and instanceof QNSelector.Item_Interface
 ***************************************************************************/

class QNSelector extends RelativeLayout {
	/******************************
	 * Constants
	 ******************************/
	// This value should not be changed! this match 'attrs' of quickntoe.
	private final int _MODE_TAB = 0;
	private final int _MODE_SLIDE = 1;

	/******************************
	 * members
	 ******************************/
	private View _cover = null;
	private View _ov = null; // original view (original pressed view) - '_pv'
								// before touch down action
	private View _pv = null; // pressed view
	private OnSelected_Listener _selected_listener = null;
	private OnChanged_Listener _changed_listener = null;
	private ViewGroup _item_root = null;

	private View.OnTouchListener _mode_tab = new View.OnTouchListener() {
		public boolean onTouch(View view, MotionEvent me) {
			View v = _find((int) me.getX(), (int) me.getY());
			// Nothing changed if nothing is touched.

			switch (me.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				if (null == v) {
					return true;
				}
				_touch(_ov, false);
				_touch(_pv, false);
				_ov = _pv = v;
				_touch(v, true);
				if (null != _selected_listener) {
					_selected_listener.onSelect(v);
				}
			}
				break;
			// 'break' is missing intentionally!!
			case MotionEvent.ACTION_MOVE: {
				; // do nothing at this mode.
			}
				break;

			case MotionEvent.ACTION_UP: {
				; // do nothing at this mode
			}

			default:
				;
			}
			return true;
		}
	};

	private View.OnTouchListener _mode_slide = new View.OnTouchListener() {
		public boolean onTouch(View view, MotionEvent me) {
			View v = _find((int) me.getX(), (int) me.getY());

			switch (me.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				if (null == v) {
					return true;
				}
				_ov = _pv;
				_touch(_pv, false);
				_pv = null;
			}
			// 'break' is missing intentionally!!
			case MotionEvent.ACTION_MOVE: {
				if (null == v) {
					return true;
				}
				if (v != _pv) {
					_touch(_pv, false);
					_touch(v, true);
					View from = _pv;
					_pv = v;
					if (null != _changed_listener) {
						_changed_listener.onChange(from, v);
					}
				}
			}
				break;

			case MotionEvent.ACTION_UP: {
				// _pv is selected one !!!
				if (null != _selected_listener) {
					_selected_listener.onSelect(_pv);
				}
			}
				break;

			default:
				;
			}
			return true;
		}
	};

	private View.OnTouchListener _mode = _mode_slide;

	/******************************
	 * Types
	 ******************************/
	interface Item_Interface {
		/**
		 * check whether this item contains point(x,y). [NOTE!] (x,y) should be
		 * based on parent's coordinate.
		 * 
		 * @return
		 */
		public boolean isContain(int x, int y);

		// get item detail view
		public Drawable detail();

		public void item_touched(boolean bTouched);
	}

	// on select is called
	interface OnSelected_Listener {
		void onSelect(View v);
	}

	/**
	 * from, to can be null
	 */
	interface OnChanged_Listener {
		void onChange(View from, View to);
	}

	/**************************
	 * Local Functions
	 **************************/
	/**
	 * _Find View that contains (x,y)
	 */
	private View _find(int x, int y) {
		int N = _item_root.getChildCount();
		View v;
		Rect r = new Rect();
		for (int i = N - 1; i >= 0; i--) {
			v = _item_root.getChildAt(i);
			QNDev.qnAssert(null != v);
			if (_cover == v) {
				continue;
			} // ignore ghost view

			r.left = v.getLeft();
			r.top = v.getTop();
			r.right = v.getRight();
			r.bottom = v.getBottom();
			if (r.contains(x, y)) {
				if (v.isClickable()) {
					if (v instanceof Item_Interface) {
						return ((Item_Interface) v).isContain(x, y) ? v : null;
					} else {
						return v;
					}
				} else {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * _Find View index.
	 */
	private int _find(View in) {
		if (null == in) {
			return -1;
		}
		int N = _item_root.getChildCount();
		View v;
		for (int i = N - 1; i >= 0; i--) {
			v = _item_root.getChildAt(i);
			if (in == v) {
				return i;
			}
		}
		return -1;
	}

	private void _touch(View v, boolean bValue) {
		if (null != v && v instanceof Item_Interface) {
			((Item_Interface) v).item_touched(bValue);
			v.invalidate();
		}
	}

	/**************************
	 * Overriding.
	 **************************/

	/**************************
	 * APIs.
	 **************************/

	public QNSelector(Context context, AttributeSet attrs) {
		super(context, attrs);

		int mode = attrs.getAttributeIntValue(QNConstants.XMLNS,
				"selector_mode", -1);
		switch (mode) {
		case _MODE_TAB: {
			_mode = _mode_tab;
		}
			break;
		case _MODE_SLIDE: {
			_mode = _mode_slide;
		}
			break;
		default:
			_mode = _mode_tab;
		}
	}

	/**
	 * Selector requirs "selector_gv" as an ghostview id and
	 * "selector_item_root" as an item root (ViewGroup)
	 * 
	 * @return
	 */
	boolean init() {
		_cover = new View(getContext());
		_cover.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		_cover.setOnTouchListener(_mode);
		addView(_cover);

		_item_root = (ViewGroup) findViewById(R.id.selector_item_root);
		QNDev.qnAssert(null != _cover && null != _item_root);
		return (null != _item_root) && (null != _cover);
	}

	OnSelected_Listener register_onSelected(OnSelected_Listener listener) {
		OnSelected_Listener sv = _selected_listener;
		_selected_listener = listener;
		return sv;
	}

	OnChanged_Listener register_onChanged(OnChanged_Listener listener) {
		OnChanged_Listener sv = _changed_listener;
		_changed_listener = listener;
		return sv;
	}

	/**
	 * @return : pressed item before touch down action.(original pressed item
	 *         before "ACTION")
	 */
	boolean is_in(View item) {
		if (0 > _find(item)) {
			return false;
		} else {
			return true;
		}
	}

	View original_item() {
		return _ov;
	}

	View pressed() {
		return _pv;
	}

	View detail(View item) {
		if (null == item || !is_in(item)) {
			return null;
		} else {
			if (item instanceof Item_Interface
					&& null != ((Item_Interface) item).detail()) {
				ImageView iv = new ImageView(getContext());
				iv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
				iv.setImageDrawable(((Item_Interface) item).detail());
				return iv;
			} else {
				return null;
			}
		}
	}

	boolean Touch(View item) {
		if (!is_in(item)) {
			return false;
		} else if (item != _pv) {
			_touch(_pv, false);
			_pv = item;
			_touch(_pv, true);
		}
		return true;
	}

	boolean Select(View item) {
		if (Touch(item)) {
			QNDev.qnAssert(item == _pv);
			if (null != _pv && null != _selected_listener) {
				_selected_listener.onSelect(_pv);
			}
			return true;
		} else {
			return false;
		}
	}
}