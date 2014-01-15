/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/31 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.text.InputFilter;
import android.widget.EditText;
import android.content.res.Resources;

import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a message compose edit text line.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a message compose edit text line.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class EditTextFragment extends PickerFragment implements OnClickListener, TextWatcher {
    protected static final String TAG = EditTextFragment.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving title */
        String TITLE = "com.motorola.contextual.pickers.actions.KEY_TITLE";
        /** Saved instance state key for saving title resource ID */
        String TITLE_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_TITLE_RESOURCE_ID";
        /** Saved instance state key for saving button text resource ID */
        String BUTTON_TEXT_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_BUTTON_TEXT_RESOURCE_ID";
        /** Saved instance state key for initial text resource ID */
        String INITIAL_TEXT_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_INITIAL_TEXT_RESOURCE_ID";
        /** Saved instance state key for saving text entry */
        String TEXT = "com.motorola.contextual.pickers.actions.KEY_TEXT";
    }

    /** Title question for this Fragment */
    private String mTitle = null;

    /** Title question resource ID for this Fragment, if it exists */
    private int mTitleResourceId = -1;

    /** Confirmation button text resource ID */
    private int mButtonTextResourceId = R.string.iam_done;

    /** Default initial text when blank edit area is first clicked */
    private int mSignatureResourceId = -1;

    /** Default length of SMS*/
    private static final int SMS_DEFAULT_LENGTH = 160;

    /** User-entered text */
    private String mText = null;

    /** Positive confirmation button to save and dismiss this fragment */
    private Button mButtonPositive = null;

    /** Text edit widget */
    private EditText mEditView = null;

    public interface EditTextCallback {
        void handleEditTextFragment(Fragment fragment, Object returnValue);
    }

    private EditTextCallback mEditTextCallback;
    
    public static EditTextFragment newInstance(final String title, final int buttonTextResId, final String text,
            final int emptyInitResId) {

        Bundle args = new Bundle();
        
        args.putString(Key.TITLE, title);
        args.putInt(Key.BUTTON_TEXT_RESOURCE_ID, buttonTextResId);
        args.putInt(Key.INITIAL_TEXT_RESOURCE_ID, emptyInitResId);
        args.putString(Key.TEXT, text);

        EditTextFragment f = new EditTextFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditTextCallback = (EditTextCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement EditTextCallback");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) { 
            mTitle = getArguments().getString(Key.TITLE);
            mButtonTextResourceId = getArguments().getInt(Key.BUTTON_TEXT_RESOURCE_ID, -1);
            mSignatureResourceId = getArguments().getInt(Key.INITIAL_TEXT_RESOURCE_ID, -1);
            mText = getArguments().getString(Key.TEXT);
        }
    }
    
    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mEditTextCallback.handleEditTextFragment(fragment, returnValue);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (mContentView == null) {
            // If a title resource ID was supplied, always retrieve directly from resources
            if (mTitleResourceId >= 0) {
                mTitle = getString(mTitleResourceId);
            }

            // Empty list data
            final List<ListItem> items = new ArrayList<ListItem>();

            // Create the actual view to show
            mContentView = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(mTitle))
                    .setNoChoiceItems(items, null, null, R.layout.list_item_tap_text)
                    .setPositiveButton(mButtonTextResourceId, this)
                    .create()
                    .getView();

            // Show the edit text area
            mContentView.findViewById(R.id.edit_text).setVisibility(View.VISIBLE);

            // Retrieve the button
            mButtonPositive = (Button) mContentView.findViewById(R.id.button1);

            // Configure the text edit line
            mEditView = (EditText) mContentView.findViewById(R.id.compose);

            // Change editText maxLength based on locale
            Resources res = getActivity().getResources();
            int maxLength = SMS_DEFAULT_LENGTH;
            if (res != null){
                maxLength = res.getInteger(R.integer.SMS_picker_character_limit);
            }
            InputFilter[] filterArray = new InputFilter[1]; //we only have a single filter
            filterArray[0] = new InputFilter.LengthFilter(maxLength);
            mEditView.setFilters(filterArray);

            mEditView.addTextChangedListener(this);
            mEditView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            if (mSignatureResourceId >= 0) {
                mEditView.setOnFocusChangeListener(new OnFocusChangeListener() {
                    public void onFocusChange(final View v, final boolean hasFocus) {
                        final String message = mEditView.getText().toString();
                        if (hasFocus) {
                            if ((message == null) || (message.length() == 0)) {
                                mEditView.setText(mSignatureResourceId);
                            }
                            //Setting the signature is a one time operation
                            //So, remove the focus change listener after adding the signature
                            mEditView.setOnFocusChangeListener(null);
                        }
                    }
                });
            }
            if (mText != null) {
                mEditView.setText(mText);
            }

            // Configure the button enabled state
            mButtonPositive.setEnabled(!getMessage().isEmpty());
        }
        return mContentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        hideSoftInputKeyboard();
    }

    /**
     * Dissociate listeners.
     *
     * @see android.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mEditView != null) {
            mEditView.removeTextChangedListener(this);
            mEditView.setOnFocusChangeListener(null);
        }
        super.onDestroy();
    }

    /**
     * Restores prior instance state in onCreateView.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mTitle = savedInstanceState.getString(Key.TITLE);
        mTitleResourceId = savedInstanceState.getInt(Key.TITLE_RESOURCE_ID, -1);
        mButtonTextResourceId = savedInstanceState.getInt(Key.BUTTON_TEXT_RESOURCE_ID, -1);
        mSignatureResourceId = savedInstanceState.getInt(Key.INITIAL_TEXT_RESOURCE_ID, -1);
        mText = savedInstanceState.getString(Key.TEXT);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.TITLE, mTitle);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleResourceId);
        outState.putInt(Key.BUTTON_TEXT_RESOURCE_ID, mButtonTextResourceId);
        outState.putInt(Key.INITIAL_TEXT_RESOURCE_ID, mSignatureResourceId);
        outState.putString(Key.TEXT, mText);
    }

    /**
     * Retrieves message.
     *
     * @return Message
     */
    public String getMessage() {
        return mEditView.getText().toString().trim();
    }

    /**
     * Required by TextWatcher interface.
     * Does nothing on text change.
     *
     * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
     */
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    /**
     * Required by TextWatcher interface.
     * Does nothing before text change.
     *
     * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
     */
    public void beforeTextChanged(final CharSequence s, final int start, final int count,
            final int after) {
    }

    /**
     * Required by TextWatcher interface.
     * After text change, rejects invalid input and trims white space.
     *
     * @param s Editable object
     * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
     */
    public void afterTextChanged(final Editable s) {
        final boolean isEmptyStrTrim = s.toString().trim().isEmpty();
        // Reject whitespace-only input
        if (isEmptyStrTrim) {
            s.clear();
        }
        mButtonPositive.setEnabled(!isEmptyStrTrim);
    }

    /**
     * Required by OnClickListener interface.
     * Handles click of button.
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     */
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            onPositiveButtonClicked();
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            //Do nothing for nonexistent list items
            break;
        }
    }

    /**
     * Handles button click by returning message.
     */
    private void onPositiveButtonClicked() {
        mHostActivity.onReturn(getMessage(), this);
    }

    /**
     * Hide soft input keyboard, if shown.
     */
    public void hideSoftInputKeyboard() {
        UIUtils.hideSoftInputKeyboard(mHostActivity, mEditView);
    }
}
