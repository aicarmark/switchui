/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * 01/19/2011, MTKG67, GB UPMERGE, Port ISTABLEFOUR-392 ActionBar background not repeating
 */

package com.motorola.firewall;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.util.ArrayList;

/**
 * ActionBar is a widget used to display a group of image only or text only buttons at the
 * bottom of an activity screen. ActionBar supports image only or text only buttons based on
 * the type of button added for the first time. Same type of buttons only are allowed subsequently
 * for that instance. If user initially plans to use ActionBar for one type of buttons and
 * later wants to convert to other type, they need to remove all previous buttons before adding
 * new type of buttons.
 */

public class MotoActionBar extends LinearLayout {

    private LinearLayout.LayoutParams mButtonLayoutParams;
    private LinearLayout.LayoutParams mSeparatorLayoutParams;

    private static final int ACTIONBAR_BUTTON_TEXT_MAX_LINES = 2;

    public static final int ACTIONBAR_BOTTOM = 0;
    public static final int ACTIONBAR_RIGHT = 1;

    // default is set to bottom position
    private int mPosition = ACTIONBAR_BOTTOM;

    private Drawable mActionBarBkg;
    private Context context;
    private LayerDrawable mActionBarLayerDrawable;

    public MotoActionBar(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public MotoActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPosition = ACTIONBAR_BOTTOM;
        mContext = context;
        init();
    }

    private void init() {
        if (mPosition == ACTIONBAR_RIGHT) {
            setActionBarBackground(ACTIONBAR_RIGHT);
            setOrientation(LinearLayout.VERTICAL);

            //set the layout properties for action bar buttons and image buttons
            mButtonLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            mSeparatorLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT);

        } else if (mPosition == ACTIONBAR_BOTTOM) { 
            setActionBarBackground(ACTIONBAR_BOTTOM);
            setOrientation(LinearLayout.HORIZONTAL);

            //set the layout properties for action bar buttons and image buttons
            mButtonLayoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.FILL_PARENT, 1);
            mSeparatorLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    LinearLayout.LayoutParams.FILL_PARENT);
        }
    }

    /**
     * Sets the position of Action Bar
     * @param pos is the position of Action Bar. Valid values for this parameter
     * are ActionBar.ACTIONBAR_BOTTOM and ActionBar.ACTIONBAR_RIGHT for now.
     * IllegalArgumentException will be thrown for other values.
     * As ActionBar widget's default orientation is horizontal, default value of the
     * pos parameter is ActionBar.ACTIONBAR_BOTTOM.
     * ActionBar widget works as designed when horizontal ActionBar is used at the bottom
     * of the vertical LinearLayout and vertical ActionBar is used at the right edge of
     * horizontal LinearLayout. ActionBar behavior is undefined in other use cases.
     */
    public void setPosition(int pos) {
        if(pos != ACTIONBAR_BOTTOM && pos != ACTIONBAR_RIGHT)
            throw new IllegalArgumentException("Not a valid position value.");
        mPosition = pos;
        if (mPosition == ACTIONBAR_BOTTOM) {
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else if(mPosition == ACTIONBAR_RIGHT) {
            setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        }
        init();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            if (child instanceof Button) {
                ((Button)child).setLayoutParams(mButtonLayoutParams);
            } else if (child instanceof ImageButton) {
                ((ImageButton)child).setLayoutParams(mButtonLayoutParams);
            } else if (child instanceof ImageView) {
                ImageView imgView = (ImageView) child;
                if (mPosition == ACTIONBAR_BOTTOM) {
                    imgView.setImageResource(R.drawable.zz_moto_actionbar_divider);
                } else if (mPosition == ACTIONBAR_RIGHT) {
                    imgView.setImageResource(R.drawable.zz_moto_actionbar_right_divider);
                }
                ((ImageView)child).setLayoutParams(mSeparatorLayoutParams);
            }
        }
    }

    /**
     * Adds text type buttons to ActionBar.
     * This API adds button next to previous button.
     * @param button is standard android Button type. But only supports text.
     * Throws IllegalArgumentException if button is null.
     */
    public void addButton (Button button) {
        if (button == null)
            throw new IllegalArgumentException("button is null and can't be added to ActionBar");

        setButtonParams (button);
        if (getChildCount() > 0) {
            addSeparator(-1);
        }
        button.setBackgroundColor(0x00000000);
        super.addView(button, -1, mButtonLayoutParams);
        setButtonBackgrounds();
    }

    /**
    Set the defaults for action bar button text
    */
    private void setButtonParams (Button button) {
        button.setGravity(Gravity.CENTER);
        button.setLines(ACTIONBAR_BUTTON_TEXT_MAX_LINES);
    }

    /**
     * Adds text type buttons to ActionBar.
     * This API adds button at given index only when index >= 0 and index < current button count.
     * @param button is standard android Button type.
     * @param index where user wants to insert the button.
     * Throws IndexOutofBoundsException if index < 0 or index > current button count;
     * Throws IllegalArgumentException if button is null.
     */
    public void addButton(int index, Button button) {
        if(button == null)
            throw new IllegalArgumentException("button is null and can't be added to ActionBar");
        if ( index < 0 || index > getButtonCount() )
            throw new IndexOutOfBoundsException("index should be >= 0 and <= getButtonCount()");

        setButtonParams(button);
        button.setBackgroundColor(0x00000000);
        if (index == 0 && getButtonCount() == 0) {
            super.addView (button, index, mButtonLayoutParams);
        } else if (index == getButtonCount()) {
            addSeparator(index * 2 - 1);
            super.addView(button, index * 2, mButtonLayoutParams);
        } else {
            super.addView(button, index * 2, mButtonLayoutParams);
            addSeparator(index * 2 + 1);
        }
        setButtonBackgrounds();

    }

    /**
     * Adds image type buttons to ActionBar.
     * This API adds button next to previous button.
     * @param button is standard android ImageButton type.
     * Throws IllegalArgumentException if button is null.
     */
    public void addButton(ImageButton button) {
        if (button == null)
            throw new IllegalArgumentException("button is null and can't be added to ActionBar");

        //Set default image backgrounds
        if (getChildCount() > 0) {
            addSeparator(-1);
        }
        ((ImageButton)button).setBackgroundColor(0x000000);
        super.addView(button, -1, mButtonLayoutParams);
        setButtonBackgrounds();
    }

    /*
     * Adds image type buttons to ActionBar.
     * This API adds button at given index only when index >= 0 and index <= current button count.
     * @param button is standard android ImageButton type.
     * @param index where user wants to insert the button.
     * Throws IndexOutofBoundsException if index < 0 or index > current button count;
     * Throws IllegalArgumentException if button is null.
     */
    public void addButton(int index, ImageButton button) {
        if(button == null)
            throw new IllegalArgumentException("button is null and can't be added to ActionBar");
        if ( index < 0 || index > getButtonCount() )
            throw new IndexOutOfBoundsException("Invalid index");

        //Set the default image button backgrounds
        ((ImageButton)button).setBackgroundColor(0x00000000);

        if (index == 0 && getButtonCount() == 0) {
            super.addView (button, index, mButtonLayoutParams);
        } else if (index == getButtonCount()) {
            addSeparator(index * 2 - 1);
            super.addView(button, index * 2, mButtonLayoutParams);
        } else {
            super.addView(button, index * 2, mButtonLayoutParams);
            addSeparator(index * 2 + 1);
        }
        setButtonBackgrounds();
    }

    /**
     * Removes given button from ActionBar widget. If button is null nothing will be removed and
     * the API will just return without doing anything.
     * @param button is standard android Button type.
     */
    public void removeButton(Button button) {
        if(button == null)
            return;
        removeButton(indexOfChild(button));
        setButtonBackgrounds();
    }

    /**
     * Removes button from ActionBar at a given index. Valid index value should be >= 0 && < current
     * button count on ActionBar widget.
     * @param index is the location where a button on ActionBar will be removed.
     * Throws IndexOutofBoundsException if index < 0 or >= current button count.
     */
    public void removeButton(int index) {
        if ( index < 0 || index >= getButtonCount() )
            throw new IndexOutOfBoundsException("No one is at that index");

        if (index == 0 && getButtonCount() == 1) {
            removeViewAt(index);
        } else if (index == 0 ) {
            removeViewAt(index + 1);
            removeViewAt(index);
        } else {
            removeViewAt(index * 2);
            removeViewAt(index * 2 - 1);
        }
        setButtonBackgrounds();
    }

    /**
     * Removes given button from ActionBar widget. If button is null nothing will be removed and
     * the API will just return without doing anything.
     * @param button is standard android ImageButton type.
     */
    public void removeButton(ImageButton button) {
        if(button == null)
            return;
        removeButton(indexOfChild(button));
        setButtonBackgrounds();
    }

    /**
     * Removes all added buttons on ActionBarWidget.
     */
    public void removeAllButtons() {
        removeAllViews();
    }

    /**
     * This method returns total number buttons added to ActionBar widget.
     * @return button count on ActionBar
     */
    public int getButtonCount () {
        //return the difference between the number of children and number of separators
        return getChildCount() - getChildCount() / 2;
    }

    /**
    Adds the separator in the appropriate index
    */
    private void addSeparator (int index) {
        ImageView buttonSeparator = new ImageView(getContext());
        if (mPosition == ACTIONBAR_BOTTOM) {
            buttonSeparator.setImageResource(R.drawable.zz_moto_actionbar_divider);
        } else if (mPosition == ACTIONBAR_RIGHT) {
            buttonSeparator.setImageResource(R.drawable.zz_moto_actionbar_right_divider);
        }
        buttonSeparator.setLayoutParams(mSeparatorLayoutParams);
        buttonSeparator.setScaleType(ImageView.ScaleType.FIT_XY);
        super.addView(buttonSeparator, index, mSeparatorLayoutParams);
    }

    private void setActionBarBackground(int position) {
        if (position == ACTIONBAR_BOTTOM) {
            mActionBarBkg = mContext.getResources().getDrawable(R.drawable.zz_moto_actionbar_bkg);
        } else if (position == ACTIONBAR_RIGHT) {
            mActionBarBkg = mContext.getResources().getDrawable(R.drawable.zz_moto_actionbar_right_bkg);
        }

        setBackgroundDrawable(mActionBarBkg);
    }

    /**
    Sets the background drawables based on the position of the child in the view tree
    */
    private void setButtonBackgrounds() {
        int index = 0;
        int childCount = getChildCount();
        if (childCount == 1) {
            if (getChildAt(0) instanceof ToggleButton) {
                getChildAt(0).setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg));
            } else {
                getChildAt(0).setBackgroundResource(R.drawable.zz_moto_actionbar_btn);
            }
        } else if(childCount > 1) {
            //find the first button that is not GONE
            int firstButtonIndex = 0, lastButtonIndex = 0;
            for (int i = 0; i < childCount; i+=2) {
                if (getChildAt(i).getVisibility() != View.GONE) {
                    firstButtonIndex = i;
                    break;
                }
            }
            //find the last button that is not GONE
            for (int i = childCount - 1; i > 0; i-=2) {
                if (getChildAt(i).getVisibility() != View.GONE) {
                    lastButtonIndex = i;
                    break;
                }
            }
            if (firstButtonIndex == lastButtonIndex) {
                //only one button is not GONE
                if (getChildAt(firstButtonIndex) instanceof ToggleButton) {
                    getChildAt(firstButtonIndex).setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg));
                } else {
                    getChildAt(firstButtonIndex).setBackgroundResource(R.drawable.zz_moto_actionbar_btn);
                }
            } else {
                //more than one button is not GONE
                if (mPosition == ACTIONBAR_BOTTOM) {
                    if (getChildAt(firstButtonIndex) instanceof ToggleButton) {
                        getChildAt(firstButtonIndex).setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg_left));
                    } else {
                        getChildAt(firstButtonIndex).setBackgroundResource(R.drawable.zz_moto_actionbar_btn_left);
                    }

                    if (getChildAt(lastButtonIndex) instanceof ToggleButton) {
                        getChildAt(lastButtonIndex).setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg_right));
                    } else {
                        getChildAt(lastButtonIndex).setBackgroundResource(R.drawable.zz_moto_actionbar_btn_right);
                    }
                } else if (mPosition == ACTIONBAR_RIGHT) {
                    getChildAt(firstButtonIndex).setBackgroundResource(R.drawable.zz_moto_actionbar_right_btn_right);
                    getChildAt(lastButtonIndex).setBackgroundResource(R.drawable.zz_moto_actionbar_right_btn_left);
                }
                for (int i = firstButtonIndex + 1; i < lastButtonIndex; i++) {
                    if (i % 2 == 0 && getChildAt(i).getVisibility() != View.GONE) {
                        if (mPosition == ACTIONBAR_BOTTOM) {
                            if (getChildAt(i) instanceof ToggleButton) {
                                getChildAt(i).setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg_mid));
                            } else {
                                getChildAt(i).setBackgroundResource(R.drawable.zz_moto_actionbar_btn_mid);
                            }
                        } else if (mPosition == ACTIONBAR_RIGHT) {
                            getChildAt(i).setBackgroundResource(R.drawable.zz_moto_actionbar_right_btn_mid);
                        }
                    }
                }
            }

        }
    }

    protected void onLayout (boolean changed, int l, int t, int r, int b) {

        //Make separators GONE if the child is set to GONE. Note that this methid will be called when
        //a child is set to GONE hence requiring a new layout
        for (int i = 2; i < getChildCount(); i++) {
            if (i % 2 == 0) {
                getChildAt(i - 1).setVisibility(getChildAt(i).getVisibility());
            }
        }

        //Hide the separator before the first non GONE button
        if (getChildCount() > 0 && getChildAt(0).getVisibility() == View.GONE) {
            for (int i = 2; i < getChildCount(); i+=2) {
                if (getChildAt(i).getVisibility() != View.GONE && getChildAt(i - 2).getVisibility() == View.GONE) {
                    getChildAt(i-1).setVisibility(View.GONE);
                    break;
                }
            }
        }
        //Children's position might have been changed. Set backgrounds again!
        setButtonBackgrounds();
        if (mPosition == ACTIONBAR_BOTTOM) {
            setActionBarBackground(ACTIONBAR_BOTTOM);
        } else if(mPosition == ACTIONBAR_RIGHT) {
            setActionBarBackground(ACTIONBAR_RIGHT);
        }

        //call the super class layout to do the layout job
        super.onLayout (changed, l, t, r, b);
    }

    /**
     * {@hide}
     */
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        //android.util.Log.d("ActionBar", "add view3: " + child + " index: " + index);
        //android.util.Log.d("ActionBar", "child view ID: " + child.getId() + " abview id: " + R.id.buttonContainer);

        if (child instanceof ToggleButton) {
            // Left blank
        } else if (child instanceof Button) {
            ((Button)child).setGravity(Gravity.CENTER);
            ((Button)child).setLines(ACTIONBAR_BUTTON_TEXT_MAX_LINES);
            ((Button)child).setBackgroundColor(0x00000000);

        } else if (child instanceof ImageButton) {
            ((ImageButton)child).setBackgroundColor(0x00000000);
        }
        super.addView(child, index, mButtonLayoutParams);
    }

    /**
     * {@hide}
     */
    protected void onFinishInflate () {
        //int count = getChildCount();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ToggleButton) {
                ToggleButton tb = (ToggleButton) child;
                tb.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.actionbar_toggle_bg_mid));
            } else {
                child.setBackgroundResource(R.drawable.zz_moto_actionbar_btn_mid);
            }

            if (!(child instanceof Button) && !(child instanceof ImageButton)) {
                throw new IllegalArgumentException(
                "This widget is designed to contain only buttons and image buttons");
            }

            //add separator after each child
            if (i > 0) {
                addSeparator(i);
                i++;
            }
        }

        setButtonBackgrounds();
        if (mPosition == ACTIONBAR_BOTTOM) {
            setActionBarBackground(ACTIONBAR_BOTTOM);
        } else if(mPosition == ACTIONBAR_RIGHT) {
            setActionBarBackground(ACTIONBAR_RIGHT);
        }
    }
}
