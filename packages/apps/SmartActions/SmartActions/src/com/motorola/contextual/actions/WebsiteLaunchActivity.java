/*
 * @(#)WebsiteLaunchActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 * a21383       2011/03/22  NA                  V1.1 changes
 *
 */

package com.motorola.contextual.actions;


import com.motorola.contextual.commonutils.*;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This class allows the user to select the website to be launched as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends BaseDialogActivity
 *
 * RESPONSIBILITIES:
 *     Shows a list view with each view element containing a title and URL
 *     Intent containing the selected URL is returned to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class WebsiteLaunchActivity extends PreferenceActivity implements android.widget.AdapterView.OnItemClickListener, Constants, TextWatcher {

    private static final String TAG = TAG_PREFIX + WebsiteLaunchActivity.class.getSimpleName();
    private static final String AND = " AND ";
    private static final String IS_NOT_NULL = " IS NOT NULL ";
    private TimingPreference mRuleTimingPref = null;
    private EditText mEnterText = null;
    private ListView mListView = null;
    private String mSelectedUri = null;
    private boolean mDisableActionBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.time_preference_actions);
        setContentView(R.layout.website);

        mEnterText = (EditText)findViewById(R.id.website_url);
        mEnterText.requestFocus();
        mEnterText.setImeOptions(EditorInfo.IME_ACTION_DONE);


        mRuleTimingPref = (TimingPreference)findPreference(getString(R.string.Timing));
        String[] values = new String[] {
            getString(R.string.launch_beginning),
            getString(R.string.launch_end)
        };
        mRuleTimingPref.setSummaryValues(values);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.open_website);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);


        Intent intent = getIntent();
        Intent configIntent = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
        if (configIntent != null) {
            // edit case
            mEnterText.setText(configIntent.getStringExtra(EXTRA_URL));
            mRuleTimingPref.setSelection(configIntent.getBooleanExtra(EXTRA_RULE_ENDS, false));
        }

        Cursor bCursor = getContentResolver().query(
                             Browser.BOOKMARKS_URI,
                             new String[] {
                                 Browser.BookmarkColumns._ID, Browser.BookmarkColumns.TITLE, Browser.BookmarkColumns.URL
                             },
                             Browser.BookmarkColumns.BOOKMARK + " = 1" + AND + Browser.BookmarkColumns.URL + IS_NOT_NULL, null, null);

        if (bCursor != null && bCursor.getCount() > 0 && bCursor.moveToFirst()) {

            this.startManagingCursor(bCursor);
            BookmarkAdapter adapter = new BookmarkAdapter(this, bCursor, true);
            mListView = (ListView) findViewById(R.id.link_list);
            mListView.setOnItemClickListener(this);
            mListView.setAdapter(adapter);
        } else {
            ImageView bottomDivider = (ImageView) findViewById(R.id.bottom_divider);
            LinearLayout bookmarksLayout = (LinearLayout) findViewById(R.id.bookmarks_container);
            bookmarksLayout.setVisibility(View.GONE);
            bottomDivider.setVisibility(View.VISIBLE);
            if (bCursor != null)
                bCursor.close();
        }
        enableSaveButton();

    }

    @Override
    protected void onPause() {
        mEnterText.removeTextChangedListener(this);
        super.onPause();
        mDisableActionBar = true;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDisableActionBar = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisableActionBar = false;
        mEnterText.addTextChangedListener(this);
    }

    /** onOptionsItemSelected()
      *  handles key presses of ICS action bar items.
      */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_save:
            setResult(RESULT_OK,
                      prepareResultIntent(mEnterText.getText().toString()));
            finish();
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
    }

    /**
     * This method sets up visibility for action bar items.
     * @param enableSaveButton - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if(enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment

        Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction().replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        enableSaveButton();
    }

    /** Required by TextWatcher interface */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    /** Required by TextWatcher interface */
    public void afterTextChanged(Editable s) {

    }

    /** Enables the save button if certain constraints are met
     *
     */
    private void enableSaveButton() {
        setupActionBarItemsVisibility(!StringUtils.isTextEmpty(mEnterText.getText()));
    }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     *
     * @param title
     * @return result intent
     */
    private Intent prepareResultIntent(String title) {
        Intent intent = new Intent();
        if (LOG_INFO) Log.i(TAG, "Website to be launched:" + mSelectedUri);
        intent.putExtra(EXTRA_CONFIG, WebsiteLaunch.getConfig((mSelectedUri != null) ? mSelectedUri : title,
                mRuleTimingPref.getSelection()));
        intent.putExtra(EXTRA_DESCRIPTION, title);
        intent.putExtra(EXTRA_RULE_ENDS, mRuleTimingPref.getSelection());
        return intent;

    }


    public static class BookmarkAdapter extends CursorAdapter {

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


    /** handles the click events in the list
     */
    public void onItemClick(AdapterView<?> l, View arg1, int position, long arg3) {
        Cursor c = (Cursor)l.getItemAtPosition(position);
        if(c != null) {
            mSelectedUri = c.getString(c.getColumnIndex(Browser.BookmarkColumns.URL));
            String name = c.getString(c.getColumnIndex(Browser.BookmarkColumns.TITLE));
            if(name != null)
                mEnterText.setText(name);
            else
                mEnterText.setText(mSelectedUri);
        }


    }
}
