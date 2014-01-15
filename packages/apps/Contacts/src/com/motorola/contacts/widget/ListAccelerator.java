/*
 * Copyright (C) 2010 A Hshieh, modified code originally from Google under
 * the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.motorola.contacts.widget;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.AbsListView.OnScrollListener;
import android.graphics.Typeface;

// BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
import java.util.Locale;
import java.util.Arrays;
// END IKCBS-2037 / FID 37309

import com.android.contacts.R;

/**
 * ListAccelerator is meant for embedding {@link ListView}s that contain a large
 * number of items that can be indexed in some fashion. It displays a special
 * scroll bar that allows jumping quickly to indexed sections of the list in
 * touch-mode. Only one child can be added to this view group and it must be a
 * {@link ListView}, with an adapter that is derived from {@link BaseAdapter}.
 *
 * ListAccelerator is a modified build of FastScrollView and functions
 * significantly differently, allowing an alphabet list for quick
 * section-specific navigation. To utilize, use in the same fashion as
 * FastScrollView.
 */
public class ListAccelerator extends FrameLayout implements OnScrollListener,
        OnHierarchyChangeListener {

    final String TAG = "ListAccelerator";

    // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
    //for further language support, accelerator type needs to be defined.
    //for example, define ACCELERATOR_JAPANESE or ACCELERATOR_GREEK
    //default is English
    private static final int ACCELERATOR_ENGLISH = 0;
    private static final int ACCELERATOR_KOREAN = 1;

    private static int mAcceleratorType = ACCELERATOR_ENGLISH;

    char[]  mLetterTable;
    char[]  mMatchingLetterTable;
    int[] mMatchingLetterIndex;

    //first and last letter for non-English
    char FIRST_NON_ENGLISH_LETTER = 0;
    char LAST_NON_ENGLISH_LETTER  = 0;
    // END IKCBS-2037 / FID 37309


    // to remember the letter status, if contact beginning with the letter exist.
    private boolean[] mLetterStatus;

    static final boolean DEBUG = false;

    private String mMeContact;

    private Drawable mCurrentThumb;


    private int mThumbH;
    private int mThumbW;
    private int mThumbY;
    private int mHightlightY;

    private boolean mAlphabetList = true;
    private int mHighlighterSize = 45;
    private static int romanEnglishAlphabetNumberOfLetters = 28;
    private static int koreanHangulAlphabetNumberOfLetters = 24; // Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts

    // Two pages
    private final static int mMinNumberOfContactsForAlphabetList = 7;

    private static int mNumberOfLetters;


    // Alphabet list values.. these dimensions will eventually be added to the
    // dimensions folder
    private final static int mTopBottomMargin = 18;
    private final static int mRightMargin = 18;
    private double mLetterSpacing = 7.8;
    private static int mFontSize = 16;

    private final static int mHighlighterAlpha = 200;
    private final static int mHighlighterBuffer = 33;

    private static int MIN_HEIGHT;

    // Hard coding these for now
    private int mOverlaySize = 104;

    private boolean mDragging;
    private ListView mList;
    private boolean mThumbVisible;
    private int mVisibleItem;
    private Paint mPaint;
    private int mListOffset;

    private Object[] mSections;
    private String mSectionText = "";
    private boolean mDrawOverlay;
    private ScrollFade mScrollFade;
    Bitmap mIndicator = null;

    private Handler mHandler = new Handler();

    private BaseAdapter mListAdapter;
    //Defined the color for alpalist
    private int mListAcceleratorAlphaList;
    private int mListAcceleratorAlphaExist;
    private int mListAcceleratorAlphaNotExist;
    private int mListAcceleratorAlphaSpecial;

    private boolean mChangedBounds;

    public ListAccelerator(Context context) {
        super(context);

        init(context);
    }

    public ListAccelerator(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public ListAccelerator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    /*
    * @Override public void onConfigurationChanged(Configuration newConfig) {
    * super.onConfigurationChanged(newConfig); mDragging = false;
    * mHandler.postDelayed(mScrollFade, 0); }
    */
    private void useThumbDrawable(Context context, Drawable drawable) {
        mCurrentThumb = drawable;
        mChangedBounds = true;
    }

    private void init(Context context) {
        // Get both the scrollbar states drawables
        final Resources res = context.getResources();
        setAppropriateThumb();

        setWillNotDraw(false);

        // Need to know when the ListView is added
        setOnHierarchyChangeListener(this);

        mListAcceleratorAlphaList = context.getResources().getColor(R.color.listAccelerator_alphalist);
        mListAcceleratorAlphaExist = context.getResources().getColor(R.color.listAccelerator_alphaexist);
        mListAcceleratorAlphaNotExist = context.getResources().getColor(R.color.listAccelerator_alphanotexist);
        mListAcceleratorAlphaSpecial = context.getResources().getColor(R.color.listAccelerator_alphaespecial);
        //mOverlayPos = new RectF();

        mScrollFade = new ScrollFade();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(mOverlaySize / 2);
        mPaint.setColor(context.getResources().getColor(R.color.listAccelerator_alphalist));
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        mIndicator = BitmapFactory.decodeResource(getResources(),
                R.drawable.moto_list_accelerator_letter_highlight, options);

        // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
        // if there's Korean locale in the locale setting, display Korean list accelerator
        if( Arrays.asList(Locale.getAvailableLocales()).contains(Locale.KOREA) &&
                    ("SKT".equalsIgnoreCase(android.os.Build.BRAND) ||
                     "KT".equalsIgnoreCase(android.os.Build.BRAND))) {
           mAcceleratorType = ACCELERATOR_KOREAN;
        }
        mMeContact = context.getResources().getString(R.string.user_profile_contacts_list_header);

        switch (mAcceleratorType) {
            case ACCELERATOR_KOREAN:
            {
                // define Korean list accelerator specific configuration here
                // mLetterTable, mMatchingLetterTable, mMatchingLetterIndex,
                // mNumberOfLetters, mFontSize
                // FIRST_NON_ENGLISH_LETTER, LAST_NON_ENGLISH_LETTER
                // #ADGJMPSVZ + 14 Korean consonants = total 24 letters
                mNumberOfLetters = koreanHangulAlphabetNumberOfLetters;
                mFontSize = 18;
                FIRST_NON_ENGLISH_LETTER = '\u3131'; //  first KOREAN consonant kiyeok
                LAST_NON_ENGLISH_LETTER  = '\u314e'; // last KOREAN  consonant hieuh

                final String defaultLocaleString = Locale.getDefault().toString();
                if ("ko".equals(defaultLocaleString) || "ko_KR".equals(defaultLocaleString) ) {
                    mLetterTable = new char[]{'↑',
                                '\u3131', '\u3134', '\u3137', '\u3139', '\u3141', '\u3142', '\u3145',
                                '\u3147', '\u3148', '\u314a', '\u314b', '\u314c', '\u314d', '\u314e',
                                'A','D','G','J','M','P','S','V','Z'};

                    mMatchingLetterIndex = new int[] {0,
                                15, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18, 19, 19, 19,
                                20, 20, 20, 21, 21, 21, 22, 22, 22, 22, 23,
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
                }
                else{
                    mLetterTable = new char[]{'↑',
                                'A','D','G','J','M','P','S','V','Z',
                                '\u3131', '\u3134', '\u3137', '\u3139', '\u3141', '\u3142', '\u3145',
                                '\u3147', '\u3148', '\u314a', '\u314b', '\u314c', '\u314d', '\u314e'};

                    mMatchingLetterIndex = new int[] {0,
                                1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5,
                                6, 6, 6, 7, 7, 7, 8, 8, 8, 8, 9,
                                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
                }

                mMatchingLetterTable = new char[] { '#',
                            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
                            'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                            '\u3131', '\u3134', '\u3137', '\u3139', '\u3141', '\u3142', '\u3145',
                            '\u3147', '\u3148', '\u314a', '\u314b', '\u314c', '\u314d', '\u314e'};


                break;
            }
            case ACCELERATOR_ENGLISH:
            default:
            {
                mNumberOfLetters = romanEnglishAlphabetNumberOfLetters;

                mLetterTable = new char[] {'↑',
                            'A','B','C','D', 'E', 'F', 'G', 'H', 'I','J','K',
                            'L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                            '↓'};

                mMatchingLetterTable = new char[] { '#',
                            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
                            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T','U', 'V', 'W', 'X', 'Y', 'Z',
                            '*'};

                mMatchingLetterIndex = new int[] {0,
                              1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                              12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                              27};

                break;
            }
        }

        mLetterStatus = new boolean[mNumberOfLetters];
        MIN_HEIGHT = mFontSize*mNumberOfLetters;
        // END IKCBS-2037 / FID 37309
    }

    private void removeThumb() {
        mThumbVisible = false;
        // Draw one last time to remove thumb
        invalidate();
    }

    // draws the alphabet list
    private void drawAlphabetList(Canvas canvas) {
        char displayChar = 'A';// the first regular character
        // sets the style
        Paint alphaList = new Paint();
        alphaList.setColor(mListAcceleratorAlphaList);
        alphaList.setStyle(Paint.Style.FILL);
        alphaList.setAntiAlias(true);
        alphaList.setTextAlign(Paint.Align.CENTER);
        alphaList.setTextSize(mFontSize);
        // BEGIN Motorola, w21667 02/02/2012, IKHSS6UPGR-390
        // setFakeBoldText() works for both English and Korean Font
        alphaList.setFakeBoldText(true);
        // END IKHSS6UPGR-390

        // actually draw the letters
        for (int i = 0; i < mNumberOfLetters; i++) {

            String letterToDisplay = "";

            if (i == 0){
                alphaList.setColor(mListAcceleratorAlphaSpecial);
                letterToDisplay = "↑"; // captures exception for the # values
            } else if((i == mNumberOfLetters-1) && (mLetterTable[i] == '↓')){ // Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
                alphaList.setColor(mListAcceleratorAlphaSpecial);
                letterToDisplay = "↓";
            }
            else{
                letterToDisplay += mLetterTable[i]; // Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts

                if(isAlphabetExist(letterToDisplay.charAt(0))){
                    alphaList.setColor(mListAcceleratorAlphaExist);
                }else{
                    alphaList.setColor(mListAcceleratorAlphaNotExist);
                }
            }

            canvas.drawText(letterToDisplay,
                    canvas.getWidth() - mRightMargin,
                    getTopOffset() + mTopBottomMargin + (int)(i*(mLetterSpacing+mFontSize)),
                    alphaList);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (DEBUG) Log.v(TAG, "draw" + ",canvas h=" + canvas.getHeight()
                + ",canvas w=" + canvas.getWidth());

        getSections();

        if (mAlphabetList && canvas.getHeight() > canvas.getWidth()) {
            // note that we don't necessarily want orientation,
            // the canvas is what matters in this specific case.
            drawAlphabetList(canvas);
        }else{
            return;
        }

        if (!mThumbVisible) {
            return; // abort if we dont really need anything else
        }

        final int y = mThumbY;
        final int viewWidth = getWidth();
        final ListAccelerator.ScrollFade scrollFade = mScrollFade;

        // if the fade has started...
        int alpha = -1;
        if (scrollFade.mStarted) {
            alpha = scrollFade.getAlpha();
            if (alpha < ScrollFade.ALPHA_MAX / 2) {
                mCurrentThumb.setAlpha(alpha * 2);
                if (DEBUG) Log.v(TAG, "1. set Alpha=" + alpha * 2);
            }
            int left = viewWidth - (mThumbW * alpha) / ScrollFade.ALPHA_MAX;
            mCurrentThumb.setBounds(left, 0, viewWidth, mThumbH);
            if (DEBUG) Log.v(TAG, "draw setBounds=" + left + ",viewWidth=" + viewWidth
                    + ",mThumbH" + mThumbH);
            mChangedBounds = true;
        }

        canvas.translate(0, y);
        if (DEBUG) Log.v(TAG, "translate=" + y);
        mCurrentThumb.draw(canvas);
        canvas.translate(0, -y);

        // use different highlighter for alphabetical list
        if (mDragging && mDrawOverlay && mAlphabetList) {
            // does some math to figure out the Y value for the highlighter text
            int indicatorYVal = (int) (mHighlighterSize * 1.5)
                    + ((int) (getHeight() - mHighlighterSize * 1.5) * mHightlightY)
                    / getHeight();

            if (mSectionText.length() == 1) {
                // Draw only if it is a single letter
                drawHighlighter(canvas, indicatorYVal);
            } else {
                // Otherwise draw a generic for the worst case scenario
                if (mAlphabetList == true) {
                    mSectionText = "*";
                    drawHighlighter(canvas, indicatorYVal);
                }
            }
        } else if (alpha == 0) {
            scrollFade.mStarted = false;
            removeThumb();
        } else {
            invalidate(getWidth() - mThumbW, y, getWidth(), y + mThumbH);
        }
    }

    // draw Highlighter box for text
    private void drawHighlighter(Canvas canvas, int indicatorYVal) {

        // if char(A-Z) does not exist, we do not show the highlight.
        if (mSectionText.length() > 0) {
            char c = mSectionText.charAt(0);

            // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
            if (((c >= 'A' && c <= 'Z') ||
                 (c >= FIRST_NON_ENGLISH_LETTER && c <= LAST_NON_ENGLISH_LETTER))
                  && !isAlphabetExist(c))
                return;
            // END IKCBS-2037 / FID 37309
        }

        final Paint paint = mPaint;
        paint.setAlpha(mHighlighterAlpha);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(mHighlighterSize);
        canvas.drawBitmap(
                mIndicator,
                canvas.getWidth()
                        - mThumbW
                        - mHighlighterBuffer
                        - getContext()
                                .getResources()
                                .getDimensionPixelSize(
                                        R.dimen.moto_list_accelerator_highlighter_width),
                indicatorYVal
                        - getContext()
                                .getResources()
                                .getDimensionPixelSize(
                                        R.dimen.moto_list_accelerator_highlighter_width)
                        / 2 - mFontSize, paint);
        canvas.drawText(
                mSectionText,
                canvas.getWidth()
                        - mThumbW
                        - mHighlighterBuffer
                        - getContext()
                                .getResources()
                                .getDimensionPixelSize(
                                        R.dimen.moto_list_accelerator_highlighter_width)
                        / 2, indicatorYVal, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        getSections();

        if (DEBUG) Log.v(TAG, "onSizeChanged," + "x=" + w + ",h=" + h + ",oldw="
                + oldw + ",oldh" + oldh);

        if(h > MIN_HEIGHT && w < h && IsAccceratorEnabled()){
            mAlphabetList = true;
            if (DEBUG) Log.v(TAG, "mAlphabetList="+mAlphabetList);
        }else{
            mAlphabetList = false;
            if (DEBUG) Log.v(TAG, "mAlphabetList="+mAlphabetList);
        }

        if(mAlphabetList){
            mList.setFastScrollAlwaysVisible(false);
            mList.setFastScrollEnabled(false);
            h -= getTopOffset();
            mLetterSpacing  = (double)(h-mFontSize*mNumberOfLetters)/mNumberOfLetters;

        } else {

            if (mList != null) {
                mList.setFastScrollAlwaysVisible(false);
                mList.setFastScrollEnabled(true);
            }
        }

        if (DEBUG) Log.v(TAG, "mLetterSpacing="+mLetterSpacing);

        if (mCurrentThumb != null) {
            mCurrentThumb.setBounds(w - mThumbW, 0, w, mThumbH);
            if (DEBUG) Log.v(TAG, "sizechange setBounds left=" + (w - mThumbW)
                    + ",viewWidth=" + w + ",mThumbH" + mThumbH);
        }


    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(mListAdapter != null && mListAdapter instanceof OnScrollListener){
            if (DEBUG) Log.v(TAG, "onScrollStateChanged");
            OnScrollListener listner = (OnScrollListener)mListAdapter;
            listner.onScrollStateChanged(view, scrollState);
            if(mOnScrollListener != null){
                mOnScrollListener.onScrollStateChanged(view, scrollState);
            }
            mThumbVisible = true;
            mCurrentThumb.setAlpha(ScrollFade.ALPHA_MAX);
            invalidate();
        }
    }

    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {

        if (DEBUG) Log.v(TAG, "onScroll," +
                ",firstVisibleItem="+firstVisibleItem+
                ",visibleItemCount=" + visibleItemCount
                + ",totalItemCount=" + totalItemCount + ", mThumbVisible = "+mThumbVisible + ", mDragging = "+mDragging +", mScrollFade.mStarted = "+mScrollFade.mStarted + ", mAlphabetList = "+mAlphabetList);

        if (DEBUG) Log.v(TAG, "scroll enabled=" + view.isFastScrollEnabled());



        if (totalItemCount - visibleItemCount > 0 ) {

            getSections();


             setAppropriateScrollSettings(firstVisibleItem, visibleItemCount,
                    totalItemCount);

            if (mChangedBounds) {
                final int viewWidth = getWidth();
                mCurrentThumb.setBounds(viewWidth - mThumbW, 0, viewWidth,
                        mThumbH);

                if (DEBUG) Log.v(TAG, "onScroll setBounds left=" + (viewWidth - mThumbW)
                        + ",viewWidth=" + viewWidth + ",mThumbH" + mThumbH);
                mChangedBounds = false;
            }
        }
        if(!mAlphabetList){
            mHandler.removeCallbacks(mScrollFade);
            invalidate();
            return;
        }
        if (firstVisibleItem == mVisibleItem) {
            return;
        }
        mVisibleItem = firstVisibleItem;
        if (!mThumbVisible || mScrollFade.mStarted) {
            mThumbVisible = true;
            mCurrentThumb.setAlpha(ScrollFade.ALPHA_MAX);
            if (DEBUG) Log.v(TAG, "5. setAlpha");
        }
        mHandler.removeCallbacks(mScrollFade);
        mScrollFade.mStarted = false;
        if (!mDragging) {
            mHandler.postDelayed(mScrollFade, 800);
        }
        if(mOnScrollListener != null){
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    // choose the indicator type
    private void setAppropriateScrollSettings(int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {

        if (DEBUG) Log.v(TAG, "setAppropriateScrollSettings: firstVisibleItem = " + firstVisibleItem + ", visibleItemCount =" + visibleItemCount + ", totalItemCount = " + totalItemCount);

        if (mAlphabetList == true) {
            // makes the indicator snap to a section.
            // 3 is used for correct position (first item sometimes gets cut
            // off)
            mThumbY = (int)((float)(mFontSize+mLetterSpacing)*
                     (float)(getSectionForPosition(firstVisibleItem+getVisibleOffset()))) + getTopOffset();
        }

        if (DEBUG) Log.v(TAG, "mThumbY="+mThumbY + ", getVisibleOffset() = "+getVisibleOffset());
    }

    // figures out what thumb dragger needs to be drawn
    private void setAppropriateThumb() {
        final Resources res = getContext().getResources();

        useThumbDrawable(
                getContext(),
                res.getDrawable(R.drawable.moto_list_accelerator_indicator_small_outline));
        mThumbW = getContext().getResources().getDimensionPixelSize(
                R.dimen.moto_list_accelerator_thumb_width);
        mThumbH = getContext().getResources().getDimensionPixelSize(
                R.dimen.moto_list_accelerator_thumb_height);

        mChangedBounds = true;
        setWillNotDraw(false);
    }

    // returns the section given a position
    private int getSectionForPosition(int position) {
        if(position < 0)return 0;
        final SectionIndexer baseAdapter = (SectionIndexer) mListAdapter;
        int section = baseAdapter.getSectionForPosition(position);
        if (DEBUG) Log.v(TAG, "section="+section);
        if (section >= 0) {

            String title = (String) mSections[section];
            if (DEBUG) Log.v(TAG, "getSectionForPosition . title="+title);
            if (title.equals(" ") && section == 0) {
                return 0;
            } else if (title.equals(" ") && section == (mSections.length - 1)) {
                return mNumberOfLetters - 1;
            } else {

                char c = title.charAt(0);

                // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
                int index = Arrays.binarySearch(mMatchingLetterTable, c);
                if (index >= 0 && !title.equals(mMeContact)) {
                    return mMatchingLetterIndex[index];
                }
                return -1;
                // END IKCBS-2037 / FID 37309
            }
        }else{
            return -1;
        }
    }

    // detects sections
    private void getSections() {

        Adapter adapter = mList.getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            mListOffset = ((HeaderViewListAdapter) adapter).getHeadersCount();
            adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }

        if (adapter instanceof SectionIndexer && adapter instanceof BaseAdapter) {
            mListOffset = mList.getHeaderViewsCount();
            mListAdapter = (BaseAdapter) adapter;
            mSections = ((SectionIndexer) mListAdapter).getSections();
        }

        // if the sections are malformed or cant be parsed, we won't try to
        // order the list
        int listCount = mList.getCount();

        int sectionLength = 0;

        if (mSections != null) {
            sectionLength = mSections.length;
        } else {
            sectionLength = 0;
        }

        // reset the data
        for(int i = 0; i < mNumberOfLetters; i ++){
            mLetterStatus[i] = false;
        }
        // re-caculate the letter status
        for(int loop = 0; loop < sectionLength; loop++){
            String text = mSections[loop].toString();
            char c = text.charAt(0);

            // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
            if(!text.equals(mMeContact) && ((c >= 'A' && c <= 'Z') ||
               (c >= FIRST_NON_ENGLISH_LETTER && c <= LAST_NON_ENGLISH_LETTER))) {
                int index = Arrays.binarySearch(mMatchingLetterTable, c);
                if (index >= 0) {
                    mLetterStatus[mMatchingLetterIndex[index]] = true;
                }
            }
            // END IKCBS-2037 / FID 37309
        }
        if (DEBUG) Log.v(TAG,"IsAccceratorEnabled()="+IsAccceratorEnabled()+", getWidth() < getHeight()="+(getWidth() < getHeight())+", getHeight() > MIN_HEIGHT"+(getHeight() > MIN_HEIGHT)+", sectionLength > 1"+(sectionLength > 1)+", listCount > mMinNumberOfContactsForAlphabetList"+(listCount > mMinNumberOfContactsForAlphabetList));
        if(IsAccceratorEnabled()
                && getWidth() < getHeight()
                && getHeight() > MIN_HEIGHT
                && sectionLength > 1
                && listCount > mMinNumberOfContactsForAlphabetList) {
            mAlphabetList = true;
            mList.setFastScrollEnabled(false);
            int h = getHeight();
            h -= getTopOffset();
            mLetterSpacing  = (double)(h-mFontSize*mNumberOfLetters)/mNumberOfLetters;
        }else{
            mAlphabetList = false;
            mList.setFastScrollEnabled(true);
        }
        if (DEBUG) Log.v(TAG,"mAlphabetList:"+mAlphabetList);
    }

    public void onChildViewAdded(View parent, View child) {

        if (child instanceof AbsListView) {
            if (DEBUG) Log.v(TAG, "onChildViewAdded, abs list");
            mList = (ListView) child;

            mList.setOnScrollListener(this);
            getSections();
        }else{
            mList = null;
            mListAdapter = null;
            mSections = null;
        }

    }

    public void onChildViewRemoved(View parent, View child) {
        if (child == mList) {
            mList = null;
            mListAdapter = null;
            mSections = null;
        }
    }

    // deal with different touch points, depending on the list type
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if(!mAlphabetList){
            return super.onInterceptTouchEvent(ev);
        }


        // BEGIN IKMAIN-7380. WTH017. 081110. If we reach this event (instead of
        // onTouchEvent) in dragging mode then that
        // must be because the dragging event was interrupted (e.g. by an
        // incoming phone call). There will be no ACTION_UP
        // event so dragging will never get set to false. So, reset dragging to
        // false here.
        if (mDragging)
            mDragging = false;
        if (DEBUG) Log.v(TAG,"mDragging = "+mDragging);
        // END IKMAIN-7380
        if (getWidth() > getHeight() || !mAlphabetList) {
            // if no alphabetical list or landscape
            // (note that getContext().getResources().getOrientation.orientation
            // might not do what we want here)
            if (mThumbVisible && ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (ev.getX() > getWidth() - mThumbW && ev.getY() >= mThumbY
                        && ev.getY() <= mThumbY + mThumbH) {
                    mDragging = true;
                    return true;
                }
            }
            return false;
        } else {
            if (ev.getAction() == MotionEvent.ACTION_DOWN
                    && ev.getX() > getWidth() - mThumbW) {
                mDragging = true;
                if (DEBUG) Log.v(TAG,"mDragging = "+mDragging);
                return true;
            }
            return false;
        }
    }

    private void scrollTo(MotionEvent me) {

        int viewHeight = getHeight();
        viewHeight -= getTopOffset();

        int newmThumbY = (int) me.getY() - mThumbH + 10;
        mHightlightY = newmThumbY;

        if(mHightlightY <= getTopOffset()){
            mHightlightY = getTopOffset();
        }else if(mHightlightY + mThumbH > viewHeight) {
            mHightlightY = getHeight() - mThumbH;
        }

        newmThumbY -= getTopOffset();

        if (newmThumbY < 0) {
            newmThumbY = 0;
        } else if (newmThumbY + mThumbH > viewHeight) {
            newmThumbY = viewHeight - mThumbH;
        }

        float position = (float) newmThumbY / (viewHeight - mThumbH);

        int index = (int)((position)*(mNumberOfLetters-1));
        mDrawOverlay = true;

        // first section
        if(index == 0){

            char toShow = mLetterTable[index];

            StringBuilder strbuild = new StringBuilder();
            strbuild.append(toShow);
            mSectionText = strbuild.toString();

            mList.setSelection(mListOffset);
        }else if(index >= 1 && index <= mNumberOfLetters-1 && mLetterTable[index] != '↓'){ // Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts

            char toShow = mLetterTable[index];

            StringBuilder strbuild = new StringBuilder();
            strbuild.append(toShow);
            mSectionText = strbuild.toString();

            for(int sectionIndex = 0; sectionIndex < mSections.length; sectionIndex++){
                String text = mSections[sectionIndex].toString();

                //Should ignore the "ME" group
                if(text.compareTo(mSectionText) >= 0 && !text.equals(mMeContact)
                    // IKHSS6UPGR-260 FID 37309 List Accelerator for Korean in contacts
                    // When Non English letter is displayed first in order, text.compareTo(mSectionText) is always greater than zero.
                    // Example: '\u3131' (first KOREAN consonant kiyeok) is greater than alphabet. So, alphabet letter is selected, always '\u3131' section is returned first.
                    && ((FIRST_NON_ENGLISH_LETTER==0) || (text.compareTo(mSectionText)<(FIRST_NON_ENGLISH_LETTER - 'Z')))){
                    final SectionIndexer baseAdapter = (SectionIndexer) mListAdapter;

                    // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
                    boolean existMatchingLetter = false;
                    int match_index = Arrays.binarySearch(mMatchingLetterTable, text.charAt(0));
                    if (match_index >= 0) {
                        existMatchingLetter = true;
                    }
                    // END IKCBS-2037 / FID 37309

                    if (text.equals(mSectionText) || existMatchingLetter) { // Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
                        mList.setSelection(baseAdapter
                                .getPositionForSection(sectionIndex)
                                + mListOffset);
                    } else {
                        int nextPosition = baseAdapter
                                .getPositionForSection(sectionIndex);
                        if (nextPosition > 0) {
                            mList.setSelection(nextPosition - 1 + mListOffset);
                        } else {
                            mList.setSelection(mListOffset);
                        }
                    }
                    break;
                }
            }
        } else if (index == mNumberOfLetters-1) {

            char toShow = mLetterTable[index];


            StringBuilder strbuild = new StringBuilder();
            strbuild.append(toShow);
            mSectionText = strbuild.toString();

            mList.setSelection(mListAdapter.getCount()-1
                    + mListOffset);
        }
        if (DEBUG) Log.v(TAG,"mSectionText = "+mSectionText);
    }

    private void cancelFling() {
        // Cancel the list fling
        MotionEvent cancelFling = MotionEvent.obtain(0, 0,
                MotionEvent.ACTION_CANCEL, 0, 0, 0);
        mList.onTouchEvent(cancelFling);
        cancelFling.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if(!mAlphabetList){
            if (DEBUG) Log.v(TAG,"OnTouchEvent - false " +
                    ",x=" + me.getX() +
                    ",y=" + me.getY() +
                    ",Type=" + me.getAction() );

            return super.onTouchEvent(me);
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN) {

            if (DEBUG) Log.v(TAG, "OnTouchEvent, DOWN " + "x=" + me.getX() + ",y="
                    + me.getY());

            if (mAlphabetList) {
                mHightlightY = (int) me.getY() - mThumbH + 10;
                if (me.getX() > getWidth() - mThumbW
                        && mHightlightY >= getTopOffset()) {
                    //v.vibrate(100);

                    if (!mThumbVisible || mScrollFade.mStarted) {
                        mScrollFade.mStarted = false;
                        mThumbVisible = true;
                        mCurrentThumb.setAlpha(ScrollFade.ALPHA_MAX);
                        mCurrentThumb.setBounds(getWidth() - mThumbW, 0,
                                getWidth(), mThumbH);
                        if (DEBUG) Log.v(TAG, "1. Move - setBounds "+
                                ",left" + (getWidth() - mThumbW) );
                        if (DEBUG) Log.v(TAG, "6. setAlpha");
                    }

                    if (me.getY() >= mThumbY && me.getY() <= mThumbY + mThumbH) {
                        // drag actual indicator
                        //v.vibrate(mIndicatorVibrate);
                        mDragging = true;
                    }
                    if (mListAdapter == null && mList != null) {
                        getSections();
                    }
                    cancelFling();
                    if (DEBUG) Log.v(TAG, "mDragging = "+mDragging);
                    if (mDragging) {
                        scrollTo(me);
                    }
                    return true;
                }
            }

            /*
            * else {// use original code for non-alphabetical list if
            * (me.getX() > getWidth() - mThumbW && me.getY() >= mThumbY &&
            * me.getY() <= mThumbY + mThumbH) { mDragging = true;
            * v.vibrate(mIndicatorVibrate); if (mListAdapter == null && mList
            * != null) { getSections(); }
            *
            * cancelFling(); return true; } }
            */
        } else if (me.getAction() == MotionEvent.ACTION_UP) {
            if (DEBUG) Log.v(TAG, "OnTouchEvent, UP " +
                    ",x=" + me.getX() + ",y=" + me.getY());
            if (mDragging) {
                mDragging = false;
                final Handler handler = mHandler;
                handler.removeCallbacks(mScrollFade);
                handler.postDelayed(mScrollFade, 800);
                return true;
            }
        } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
            if (DEBUG) Log.v(TAG, "OnTouchEvent, MOVE " + "x=" + me.getX() + ",y="
                    + me.getY());
            if (mDragging) {
                scrollTo(me);
                return true;
            }
        }

        return super.onTouchEvent(me);
    }

    private boolean isAlphabetExist(char c){

        // BEGIN Motorola, w21667 08/01/2011, IKCBS-2037 / FID 37309 List Accelerator for Korean in contacts
        if((c >= 'A' && c <= 'Z') ||
           (c >= FIRST_NON_ENGLISH_LETTER && c <= LAST_NON_ENGLISH_LETTER)) {
            int index = Arrays.binarySearch(mMatchingLetterTable, c);
            if (index >= 0) {
                return mLetterStatus[mMatchingLetterIndex[index]];
            }
            return false;
        // END IKCBS-2037 / FID 37309
        }else{
            return false;
        }
    }
    // Google's scroll fade

    public class ScrollFade implements Runnable {

        long mStartTime;
        long mFadeDuration;
        boolean mStarted;
        static final int ALPHA_MAX = 255;
        static final long FADE_DURATION = 200;

        void startFade() {
            mFadeDuration = FADE_DURATION;
            mStartTime = SystemClock.uptimeMillis();
            mStarted = true;
        }

        int getAlpha() {
            if (!mStarted) {
                return ALPHA_MAX;
            }
            int alpha;
            long now = SystemClock.uptimeMillis();
            if (now > mStartTime + mFadeDuration) {
                alpha = 0;
            } else {
                alpha = (int) (ALPHA_MAX - ((now - mStartTime) * ALPHA_MAX)
                        / mFadeDuration);
            }
            return alpha;
        }

        public void run() {
            if (!mStarted) {
                startFade();
                invalidate();
            }

            if (getAlpha() > 0) {
                final int y = mThumbY;
                final int viewWidth = getWidth();
                invalidate(viewWidth - mThumbW, y, viewWidth, y + mThumbH);
            } else {
                mStarted = false;
                removeThumb();
            }
        }
    }
    static public interface  Interface {
        public boolean isNeeded();
        public int getTopOffset();
        public int getVisibleOffset();
    }

    boolean IsAccceratorEnabled(){

        if(mListAdapter != null && mListAdapter instanceof Interface){
            Interface acc_inter = (Interface)mListAdapter;
            return acc_inter.isNeeded();
        }else{
            return true;
        }
    }
    int getTopOffset(){

        if(mListAdapter != null && mListAdapter instanceof Interface){
            Interface acc_inter = (Interface)mListAdapter;
            return acc_inter.getTopOffset();
        }else{
            return 0;
        }
    }

    int getVisibleOffset(){

        if(mListAdapter != null && mListAdapter instanceof Interface){
            Interface acc_inter = (Interface)mListAdapter;
            return acc_inter.getVisibleOffset();
        }else{
            return 0;
        }
    }
    public void setOnScrollListener(OnScrollListener l){
        mOnScrollListener = l;
    }
    private OnScrollListener mOnScrollListener = null;
}
