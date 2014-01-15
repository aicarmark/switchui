/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.contacts.dialpad;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
/**
 * SlidingDrawer hides content out of the screen and allows the user to drag a handle
 * to bring the content on screen. SlidingDrawer can be used vertically or horizontally.
 *
 * A special widget composed of two children views: the handle, that the users drags,
 * and the content, attached to the handle and dragged with it.
 *
 * SlidingDrawer should be used as an overlay inside layouts. This means SlidingDrawer
 * should only be used inside of a FrameLayout or a RelativeLayout for instance. The
 * size of the SlidingDrawer defines how much space the content will occupy once slid
 * out so SlidingDrawer should usually use match_parent for both its dimensions.
 *
 * Inside an XML layout, SlidingDrawer must define the id of the handle and of the
 * content:
 *
 */
public class SlidingLayout extends RelativeLayout {

    private static final int GESTURE_MIN_DISTANCE = 100;
    private static final int INVALID = -1;
    private static final int UP = 1;
    private static final int DOWN = 2;

    private GestureDetector mGestureDetector;
    private SlidingDrawer mDrawer;
    protected int mDirection = INVALID;

    public SlidingLayout(Context context) {
        super(context);
    }

    public SlidingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDrawer = (SlidingDrawer)findViewById(com.android.contacts.R.id.drawer);

        mGestureDetector = new GestureDetector(new GestureDetector.OnGestureListener() {
            public boolean onDown(MotionEvent e) {
                if (mDrawer.isOpened() && !mDrawer.isMoving()) {
                    mDirection = INVALID;
                }
                return false;
            }

            public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {

                boolean ret = false;
                int distanceX = (int) e1.getX() - (int) e2.getX();
                int distanceY = (int) e1.getY() - (int) e2.getY();
                int absDistanceX = Math.abs(distanceX);
                int absDistanceY = Math.abs(distanceY);

                if ((absDistanceY > GESTURE_MIN_DISTANCE) && (absDistanceY > absDistanceX)
                        && (mDirection == INVALID)) {
                    int currentDirection = distanceY > 0 ? UP : DOWN;
                    if (currentDirection == DOWN) {
                        if (mDrawer.isOpened() && !mDrawer.isMoving()) {
                            Log.d("SlidingLayout", "onScroll down...");
                            mDrawer.animateClose();
                            ret = true;
                            mDirection = currentDirection;
                        }
                    }
                }
               return ret;
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean ret = false;

                return ret;

            }

            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            public void onLongPress(MotionEvent e) {
                return;
            }

            public void onShowPress(MotionEvent e) {
                return;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = mGestureDetector.onTouchEvent(ev);
        if (!result) {
            return super.dispatchTouchEvent(ev);
        } else {
            return result;
        }
    }
}
