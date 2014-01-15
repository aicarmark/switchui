package com.motorola.mmsp.motohomex.apps;

import com.motorola.mmsp.motohomex.R;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class AllAppsDropDownMenu extends ImageView {

    /**
     * The listener that receives notifications when an item is selected.
     */
    private OnItemSelectedListener mOnItemSelectedListener;

    // Only measure this many items to get a decent max width.
    private static final int MAX_ITEMS_MEASURED = 15;
    private Rect mTempRect = new Rect();
    private DropdownPopup mPopup;
    private OnClickListenerSpinner mOnClickListener;
    /*Added by ncqp34 at Jul-12-2012 for switchui-2128*/
    private AllAppsPage mAllAppsPage;
    /*ended by ncqp34*/

    public AllAppsDropDownMenu(Context context) {
        super(context);
    }

    public AllAppsDropDownMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AllAppsDropDownMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPopup = new DropdownPopup(context, attrs, android.R.attr.spinnerStyle);
        mPopup.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menu_dropdown_panel_holo_dark));
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been selected.
     *
     * @param listener The callback that will run
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /*Added by ncqp34 at Jul-12-2012 for switchui-2128*/
    public void setAllAppPage(AllAppsPage appPage){
	mAllAppsPage = appPage;	
    }
    /*ended by ncqp34*/

    @Override
    public boolean performClick() {
        // Close the folder on apps tray
        if (mOnClickListener != null) {
            /*2012-7-17, add by bvq783 for switchui-2270*/
            if (mOnClickListener instanceof AllAppsPage) {
                AllAppsPage page = (AllAppsPage)mOnClickListener;
                if (!page.checkShowSpinner()) {
                    return true;
                }
            }
            /*2012-7-17, add end*/
            mOnClickListener.onClickSpinner();
        }

        boolean handled = super.performClick();

        if (!handled) {
            handled = true;

            if (!mPopup.isShowing()) {
                mPopup.show();
		/*Added by ncqp34 at Jul-12-2012 for switchui-2082*/
		if(mAllAppsPage!=null){
		    //Log.d("dxx","dismiss spinner");
		    mAllAppsPage.dismissSpinnerPopup();
		}
		/*ended by ncqp34*/
            }
        }

        playSoundEffect(SoundEffectConstants.CLICK);

        return handled;
    }

    public void setAdapter(BaseAdapter adapter) {
        mPopup.setAdapter(adapter);
    }

    public void dismiss(){
        mPopup.dismiss();
    }

    public void setOnClickListenerSpinner(OnClickListenerSpinner listener) {
        mOnClickListener = listener;
    }

    private class DropdownPopup extends ListPopupWindow{
        private ListAdapter mAdapter;

        public DropdownPopup(Context context, AttributeSet attrs, int defStyleRes) {
            super(context, attrs, 0, defStyleRes);

            setAnchorView(AllAppsDropDownMenu.this);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    if (mOnItemSelectedListener != null) {
                        mOnItemSelectedListener.onItemSelected(parent, v, position, id);
                    }
                    dismiss();
                }
            });
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
            mAdapter = adapter;
        }

        @Override
        public void show() {
        	/*Added by Hu ShuAn at Jul-16-2012 for switchui-2138*/
        	dismiss();
        	/*ended by Hu ShuAn*/
            final int imageViewPaddingLeft = AllAppsDropDownMenu.this.getPaddingLeft();
            final int imageViewWidth = AllAppsDropDownMenu.this.getWidth();
            final int imageViewPaddingRight = AllAppsDropDownMenu.this.getPaddingRight();
            setContentWidth(Math.max(
                    measureContentWidth(mAdapter, getBackground()),
                    imageViewWidth - imageViewPaddingLeft - imageViewPaddingRight));

            final Drawable background = getBackground();
            int bgOffset = 0;
            if (background != null) {
                background.getPadding(mTempRect);
                bgOffset = -mTempRect.left;
            }
            setHorizontalOffset(bgOffset + imageViewPaddingLeft);
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        int measureContentWidth(ListAdapter adapter, Drawable background) {
            if (adapter == null) {
                return 0;
            }

            int width = 0;
            View itemView = null;
            int itemType = 0;
            final int widthMeasureSpec =
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            final int heightMeasureSpec =
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

            // Make sure the number of items we'll measure is capped. If it's a huge data set
            // with wildly varying sizes, oh well.
            final int end = Math.min(adapter.getCount(), MAX_ITEMS_MEASURED);
            for (int i = 0; i < end; i++) {
                final int positionType = adapter.getItemViewType(i);
                if (positionType != itemType) {
                    itemType = positionType;
                    itemView = null;
                }
                itemView = adapter.getView(i, itemView, getListView());
                if (itemView.getLayoutParams() == null) {
                    itemView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                itemView.measure(widthMeasureSpec, heightMeasureSpec);
                width = Math.max(width, itemView.getMeasuredWidth());
            }

            // Add background padding to measured width
            if (background != null) {
                background.getPadding(mTempRect);
                width += mTempRect.left + mTempRect.right;
            }

            return width;
        }
    }
}

