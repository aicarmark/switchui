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

package com.android.contacts.calllog;

import com.android.contacts.R;
import com.android.contacts.test.NeededForTesting;
import com.google.common.collect.Lists;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * View that draws one or more symbols for different types of calls (missed calls, outgoing etc).
 * The symbols are set up horizontally. As this view doesn't create subviews, it is better suited
 * for ListView-recycling that a regular LinearLayout using ImageViews.
 */
public class NetworkTypeIconsView extends View {
    private List<Integer> mNetworkTypeTypes = Lists.newArrayListWithCapacity(3);
    private Resources mResources;
    private int mWidth;
    private int mHeight;

    public NetworkTypeIconsView(Context context) {
        this(context, null);
    }

    public NetworkTypeIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResources = new Resources(context);
    }

    public void clear() {
    	mNetworkTypeTypes.clear();
        mWidth = 0;
        mHeight = 0;
        invalidate();
    }

    public void add(int networkType) {
    	final Drawable drawable = getNetworkTypeDrawable(networkType);
    	if (null == drawable) return;
    	mNetworkTypeTypes.add(networkType);
        mWidth += drawable.getIntrinsicWidth() + mResources.iconMargin;
        mHeight = Math.max(mHeight, drawable.getIntrinsicHeight());
        invalidate();
    }

    @NeededForTesting
    public int getCount() {
        return mNetworkTypeTypes.size();
    }

    @NeededForTesting
    public int getCallType(int index) {
        return mNetworkTypeTypes.get(index);
    }

    private Drawable getNetworkTypeDrawable(int networkType) {
        switch (networkType) {
            case TelephonyManager.PHONE_TYPE_GSM:
                return mResources.gsmType;
            case TelephonyManager.PHONE_TYPE_CDMA:
                return mResources.cdmaType;
            default:
            	return null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int left = 0;
        for (Integer callType : mNetworkTypeTypes) {
            final Drawable drawable = getNetworkTypeDrawable(callType);
            if (drawable == null) continue;
            final int right = left + drawable.getIntrinsicWidth();
            drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            left = right + mResources.iconMargin;
        }
    }

    private static class Resources {
        public final Drawable gsmType;
        public final Drawable cdmaType;
        public final int iconMargin;

        public Resources(Context context) {
            final android.content.res.Resources r = context.getResources();
            gsmType = r.getDrawable(R.drawable.ic_call_log_list_gsm_call);
            cdmaType = r.getDrawable(R.drawable.ic_call_log_list_cdma_call);
            iconMargin = r.getDimensionPixelSize(R.dimen.call_log_icon_margin);
        }
    }
}
