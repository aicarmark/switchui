/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/07/02 Smart Actions 2.1  Initial Version
 */

package com.motorola.contextual.pickers.actions;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.WebsiteLaunch;

import com.motorola.contextual.pickers.OneOffPickerFragment;
import com.motorola.contextual.pickers.UIUtils;
import com.motorola.contextual.smartrules.R;


/**
 * This fragment presents a list of web sites for the users to pick.  The user
 * can also type in the web address directly.
 * <code><pre>
 *
 * CLASS:
 *  extends OneOffPickerFragment
 *
 * RESPONSIBILITIES:
 *  This fragment presents a list of web sites based on the users' browser bookmarks.
 *  The user can either choose from the list or manually enter the address.
 *
 * COLLABORATORS:
 *  LaunchWebsiteActivity.java - the Activity that contains this fragment.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class LaunchWebsitePickerFragment extends OneOffPickerFragment implements Constants {

    private Intent mInputConfigs;
    private Intent mOutputConfigs;
    private TextView mUrlInput;
    private ListView mListView;
    private String mSelectedUri = null;
    private Cursor mCursor;

    private static final String AND = " AND ";
    private static final String IS_NOT_NULL = " IS NOT NULL ";


    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static LaunchWebsitePickerFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        
        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }
        
        LaunchWebsitePickerFragment f = new LaunchWebsitePickerFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setPrompt(getString(R.string.launch_website_prompt));
        setActionString(getString(R.string.iam_done));
        return v;
    }

    @Override
    protected View createCustomContentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bookmark_url_input, container, false);
        mUrlInput = (TextView)v.findViewById(R.id.enter_url);
        
        if (mInputConfigs != null) {
            // edit case
            mUrlInput.setText(mInputConfigs.getStringExtra(EXTRA_URL));
        }
        updateButtonState();
        mUrlInput.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateButtonState();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        
        mListView = (ListView)v.findViewById(R.id.list);
        new GetBookmarksTask(this).execute();

        return v;
    }
    
    private static class GetBookmarksTask extends AsyncTask<Void, Void, Cursor> {

        private LaunchWebsitePickerFragment mParent;
        
        GetBookmarksTask(LaunchWebsitePickerFragment parent) {
            mParent = parent;
        }
        
        @Override
        protected Cursor doInBackground(Void... params) {
            // check if fragment is still attached otherwise just return.
            if (!mParent.isAdded()) return null;
            
            return mParent.getActivity().getContentResolver().query(
                Browser.BOOKMARKS_URI,
                new String[] {
                    Browser.BookmarkColumns._ID, Browser.BookmarkColumns.TITLE, Browser.BookmarkColumns.URL
                },
                Browser.BookmarkColumns.BOOKMARK + " = 1" + AND + Browser.BookmarkColumns.URL + IS_NOT_NULL, null, null);
        }
        
        @Override
        protected void onPostExecute(Cursor bookmarksCursor) {
            // check if fragment is still attached otherwise just return.
            if (!mParent.isAdded()) {
                if (bookmarksCursor != null && !bookmarksCursor.isClosed())
                    bookmarksCursor.close();
                return;
            }
            mParent.setupBookmarks(bookmarksCursor);
        }
        
    }

    protected void setupBookmarks(Cursor bookmarksCursor) {
        if (bookmarksCursor != null && bookmarksCursor.getCount() > 0 && bookmarksCursor.moveToFirst()) {
            mHostActivity.startManagingCursor(bookmarksCursor);

            BookmarkAdapter adapter = new BookmarkAdapter(mHostActivity, bookmarksCursor, true);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> l, View arg1, int position, long arg3) {
                    Cursor c = (Cursor)l.getItemAtPosition(position);
                    if(c != null) {
                        mSelectedUri = c.getString(c.getColumnIndex(Browser.BookmarkColumns.URL));
                        String name = c.getString(c.getColumnIndex(Browser.BookmarkColumns.TITLE));
                        if(!TextUtils.isEmpty(name))
                            mUrlInput.setText(name);
                        else
                            mUrlInput.setText(mSelectedUri);
                    }
                }
            });
            mListView.setAdapter(adapter);
            mListView.requestFocus();
        } else {
            if (bookmarksCursor != null && !bookmarksCursor.isClosed())
                bookmarksCursor.close();
        }
    }
    
    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     *
     * @param title
     */
    private void prepareResultIntent(String title) {
        mOutputConfigs.putExtra(EXTRA_CONFIG, WebsiteLaunch.getConfig((mSelectedUri != null) ? mSelectedUri : title, false));
        mOutputConfigs.putExtra(EXTRA_DESCRIPTION, title);
        mOutputConfigs.putExtra(EXTRA_RULE_ENDS, false);
    }

    /**
     * List adapter to generate the bookmarks for the ListView.
     */
    public static class BookmarkAdapter extends CursorAdapter {

        /**
         * Constructor.
         *
         * @param context - same semantics as CursorAdapter
         * @param c - same semantics as CursorAdapter
         * @param autoRequery - same semantics as CursorAdapter
         */
        public BookmarkAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            // base class newView is an abstract method

            LayoutInflater factory = LayoutInflater.from(context);
            View v = factory.inflate(R.layout.bookmark_item, null);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            // base class bindView is an abstract method

            TextView titleView = (TextView)view.findViewById(R.id.title);
            TextView urlView = (TextView)view.findViewById(R.id.url);

            String title = cursor.getString(cursor.getColumnIndex(Browser.BookmarkColumns.TITLE));
            String url = cursor.getString(cursor.getColumnIndex(Browser.BookmarkColumns.URL));
            titleView.setText(title);

            urlView.setText(url);
        }
    }

    private void updateButtonState() {
        CharSequence text = mUrlInput.getText();
        enableButton(text != null && text.toString().trim().length() > 0);
    }

    @Override
    public void onClick(View v) {
        CharSequence text = mUrlInput.getText();
        if (text != null && text.length() != 0) {
            prepareResultIntent(text.toString());
            mHostActivity.onReturn(mOutputConfigs, this);
        }

        // Force-hide the keyboard.  It doesn't automatically do that.
        UIUtils.hideKeyboard(mHostActivity, mUrlInput);
    }

    /**
     * Closes cursor, if not null.
     *
     * @see android.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroy();
    }
}
