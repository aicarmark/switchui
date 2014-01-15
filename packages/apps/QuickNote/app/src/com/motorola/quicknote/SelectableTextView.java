package com.motorola.quicknote;

import android.app.AlertDialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import android.net.Uri;

import android.text.ClipboardManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;

import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

public class SelectableTextView extends TextView {

	private static final String TAG = "QuickNote.SelectableTextView";
	private int mStart = -1;
	private int mEnd = -1;
	private boolean mSelectable = false;
	public String mSelectedText = null;
	private TextPaint mPaint;

	private QNCallBack mCallBack = null;

	public SelectableTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setSpannableFactory(Spannable.Factory.getInstance());
		mPaint = new TextPaint();
	}

	public SelectableTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSpannableFactory(Spannable.Factory.getInstance());
		mPaint = new TextPaint();
	}

	public SelectableTextView(Context context) {
		super(context);
		setSpannableFactory(Spannable.Factory.getInstance());
		mPaint = new TextPaint();
	}

    @Override protected void onDraw(Canvas canvas) {
        if (mStart != mEnd) {
            super.onDraw(canvas);
            return;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(this.getText().toString());
        Rect bounds = new Rect();      
        int count = getLineCount();
        int[] location = new int[2];
        getLocationInWindow(location);
        int height = ((View)getParent().getParent()).getHeight() - location[1];
        int lineHeight = getLineHeight();
        int page_size = height/lineHeight + 1;
        
        if (count < page_size ) {
            for (int i = count; i < page_size; i++) {
                sb.append("\n");
            }
            count = page_size;
        }
        this.setText(sb.toString());
        mPaint.getTextBounds(this.getText().toString(), 0, this.length(), bounds);
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(76);
        mPaint.setStrokeWidth(1);
        for(int i=0; i < count; i++) {
            int baseline = getLineBounds(i, bounds);
            canvas.drawLine(bounds.left, baseline + 4, bounds.right, baseline + 4, mPaint);
        }
        super.onDraw(canvas);
    }
    
    public boolean getSelectable() {
        return mSelectable;
    }

    public void setSelectable(boolean bSelectable) {
        if (!mSelectable && bSelectable) {
            clearSelection(true);
        }

        if (mSelectable && !bSelectable && (mStart >= 0 && mEnd >=0 && mStart != mEnd)) {
            handleSelection(true);
        }

        mSelectable = bSelectable;
    }

    public void setCallBack(QNCallBack callback) {
        mCallBack = callback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mSelectable) {
            mStart = -1;
            mEnd = -1;
            return super.onTouchEvent(ev);
        }

        boolean result = true;
        int action = ev.getAction();
        QNDev.log(TAG+ "currentpos = "+ getCurrentPosition(ev));
        switch (action) { 
            case MotionEvent.ACTION_DOWN: {
                mStart = getCurrentPosition(ev);
                QNDev.log(TAG + " MotionEvent.ACTION_DOWN start:" + mStart);
            }
                break;

            case MotionEvent.ACTION_MOVE: {
                if (mStart >= 0) {
                    mEnd = getCurrentPosition(ev);
                    if (mEnd >= 0 && mStart != mEnd) {
                        handleSelection(false);
                        QNDev.log(TAG + " MotionEvent.ACTION_MOVE pos:" + mEnd);
                    }
                }
            }
                break;

            case MotionEvent.ACTION_UP: {
                if (mCallBack != null) {
                    mCallBack.exitSelectTextMode();
                }
                if (mStart >= 0) {
                    mEnd = getCurrentPosition(ev);
                    QNDev.log(TAG + " MotionEvent.ACTION_UP end:" + mEnd);
                    handleSelection(true);
                }
            }
                break;

            default: {
                result = super.onTouchEvent(ev);
                break;
            }
        }
        return result;
    }

    private int getCurrentPosition(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        x -= getCompoundPaddingLeft();
        y -= getExtendedPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        if (layout == null) {
            return -1;
        }

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        return off;
    }

    private void handleSelection(boolean bShowContextMenu) {
        if (mStart < 0 || mEnd < 0) {
            return;
        }

        if (mStart == mEnd) {
            clearSelection(false);
            return;
        }

        int minPos = mStart;
        int maxPos = mEnd;

        if (mStart > mEnd) {
            minPos = mEnd;
            maxPos = mStart;
        }

        try {
            Spannable buf= (Spannable)getText();
            clearSpans(buf);

            buf.setSpan(new ForegroundColorSpan(Color.BLACK), minPos, maxPos,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE | Spannable.SPAN_INTERMEDIATE);

            buf.setSpan(new BackgroundColorSpan(Color.rgb(255, 222, 2)),
                    minPos, maxPos, Spannable.SPAN_INCLUSIVE_EXCLUSIVE | Spannable.SPAN_INTERMEDIATE);

            setText(buf, TextView.BufferType.SPANNABLE);
        } catch (Exception e) {
            QNDev.log(TAG+ "setSpan error.");
        }
        
        if (bShowContextMenu) { 
            CharSequence charSeq = getText().subSequence(minPos, maxPos);
            mSelectedText = charSeq.toString();

            showCopyMenu();
            mSelectable = false;
        }
    }

    private void clearSpans(Spannable spannable) {
        ForegroundColorSpan[] fgSpans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);

        for (int i=0; i < fgSpans.length; i++) {
            spannable.removeSpan(fgSpans[i]);
        }

        BackgroundColorSpan[] bgSpans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);

        for (int i=0; i < bgSpans.length; i++) {
            spannable.removeSpan(bgSpans[i]);
        } 
    }

    public void clearSelection(boolean bResetPosition) {
        try {
            Spannable buf = (Spannable)getText();
            clearSpans(buf);
            setText(buf);
        } catch (Exception e) {
            QNDev.log(TAG+ "clearSelection error");
        }

        if (bResetPosition) {
            mStart = -1;
            mEnd = -1;
        }
    }

    public void showCopyMenu() {
        /* Send the clipboard string to other application */
        new AlertDialog.Builder(getContext()).setItems(
                R.array.copypaste_menu_items,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String intentStr = null;
                        switch (which) {
                            case 0:
                                ClipboardManager clip = (ClipboardManager) getContext()
                                        .getSystemService(
                                                Context.CLIPBOARD_SERVICE);
                                if (clip != null) {
                                    clip.setText(mSelectedText);
                                    Toast.makeText(getContext(),
                                            R.string.text_copied,
                                            Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 1:
                                try {
                                    Uri uri = Uri.parse("sms:");
                                    Intent intent = new Intent(
                                            Intent.ACTION_SENDTO, uri);
                                    intent.putExtra("sms_body", mSelectedText);
                                    intent.putExtra("exit_on_send", true);
                                    getContext().startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 2:
                                try {
                                    Intent intent = new Intent();
                                    intent.setAction("com.motorola.createnote");
                                    intent.putExtra("CLIPBOARD", mSelectedText);
                                    getContext().startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 3:
                                try {
                                    String broadcastAddress = String.format(
                                            "%s://%s/%s", "searchWord",
                                            "powerword", mSelectedText);
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW, Uri
                                                    .parse(broadcastAddress));
                                    getContext().startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                break;
                        }

                        clearSelection(true);

                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                clearSelection(true);
            }
        }).show();
    }
}
