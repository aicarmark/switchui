package com.android.contacts.activities;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SearchListView extends ListView {
    private EditText mKeyword = null;
    private boolean mFiltered = false;

    public SearchListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public SearchListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public SearchListView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    static class SavedState extends BaseSavedState {
        String filter;

        /**
         * Constructor called from {@link AbsListView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            filter = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(filter);
        }

        @Override
        public String toString() {
            return "SearchListView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " filter=" + filter + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        final EditText textFilter = mKeyword;
        if (textFilter != null) {
            Editable filterText = textFilter.getText();
            if (filterText != null) {
                ss.filter = filterText.toString();
            }
        }
        return ss;
    }
    
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        
        setFilterText(ss.filter);
    }
    
    @Override
    public void clearTextFilter() {
        if (null == mKeyword) {
            super.clearTextFilter();
        } else {
            mKeyword.setText("");
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (null == mKeyword) {
            super.onTextChanged(s, start, before, count);
        } else {
            mFiltered = (s.length() != 0);
            ListAdapter adapter = getAdapter();
            if (adapter instanceof Filterable) {

                Filter f = ((Filterable) adapter).getFilter();
                // Filter should not be null when we reach this part
                if (f != null) {
                    f.filter(s, this);
                } else {
                    throw new IllegalStateException("You should not arrive " +
                            "here in setFilterText with a non filterable adapter");
                }
            }
        }
    }

    @Override
    public void setFilterText(String filterText) {
        // normal list view without rubbish bar.
        if (null == mKeyword) {
            super.setFilterText(filterText);
            return;
        } 

        // block the origin popup window while rubbish bar is active
        if (!TextUtils.isEmpty(filterText)) {
            mFiltered = true;
            final int textLen = filterText.length();
            mKeyword.setText(filterText);
            mKeyword.setSelection(textLen);
            onTextChanged(filterText, 0, 0, textLen);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Dispatch in the normal way
        boolean handled = super.dispatchKeyEvent(event);
        if (!handled && mKeyword != null) {
            // If we didn't handle it...
            handled = sendToMyTextFilter(event.getKeyCode(), 1, event);
        }    
        return handled;
    }
        
    //Code from absListview
    boolean sendToMyTextFilter(int keyCode, int count, KeyEvent event) {
        boolean handled = false;
        boolean okToSend = true;
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            okToSend = false;
            break;
        case KeyEvent.KEYCODE_BACK:
            if (mFiltered && event.getAction() == KeyEvent.ACTION_DOWN) {
                handled = true;
                mKeyword.setText("");
            }
            okToSend = false;
            break;
        case KeyEvent.KEYCODE_SPACE:
            // Only send spaces once we are filtered
            okToSend = mFiltered = true;
            break;
        }

        if (okToSend) {

            KeyEvent forwardEvent = event;
            if (forwardEvent.getRepeatCount() > 0) {
                forwardEvent = KeyEvent.changeTimeRepeat(event, event.getEventTime(), 0);
            }

            int action = event.getAction();
            switch (action) {
                case KeyEvent.ACTION_DOWN:
                    handled = mKeyword.onKeyDown(keyCode, forwardEvent);
                    break;

                case KeyEvent.ACTION_UP:
                    handled = mKeyword.onKeyUp(keyCode, forwardEvent);
                    break;

                case KeyEvent.ACTION_MULTIPLE:
                    handled = mKeyword.onKeyMultiple(keyCode, count, event);
                    break;
            }
        }
        return handled;
    }

    public void attachSearchBar(EditText keyword) {
        mKeyword = keyword;
        mKeyword.addTextChangedListener(this);
    }
}
