/*
 * Copyright (C) 2010, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        05/12/2012 Smart Actions 2.1 Created file
 */

package com.motorola.contextual.pickers.actions;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.UIUtils;
import com.motorola.contextual.pickers.CustomListAdapter;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a list of apps that can handle the given intent action.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - picker fragment base class
 *
 * RESPONSIBILITIES:
 *  This fragment simulates the behavior of system's intent app picker in a fragment.
 *  This can be used by any picker, which would like to display the list of apps that
 *  can handle a given intent action - used by LaunchApp and WallPaper pickers
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class LaunchAppPickerFragment extends PickerFragment implements
        OnClickListener{

    //list items -  list of apps
    private ListItem[] mItems;
    private ListView mListView;

    //intent action and category to be resolved
    private String mIntentAction;
    private String mIntentCategory;
    private String[] mExcludePkgs;
    private String mPromptText;
    private int mListType = ListItem.typeONE;
    private ProgressDialog mProgressDialog;

    /**
    * Keys for saving and restoring instance state bundle.
    */
    private interface Key {
        String INTENT_ACTION = "com.motorola.contextual.pickers.actions.INTENT_ACTION";
        String INTENT_CATEGORY = "com.motorola.contextual.pickers.actions.INTENT_CATEGORY";
        String PROMPT_TEXT = "com.motorola.contextual.pickers.actions.PROMPT_TEXT";
        String EXCLUDE_PACKAGES = "com.motorola.contextual.pickers.actions.EXCLUDE_PACKAGES";
        String LIST_TYPE = "com.motorola.contextual.pickers.actions.LIST_TYPE";
    }

    public static LaunchAppPickerFragment newInstance(final String intentAction,
            final String intentCategory, final String[] excludePkgs, final String prompt, final int listType) {

        Bundle args = new Bundle();

        args.putString(Key.INTENT_ACTION, intentAction);
        args.putString(Key.INTENT_CATEGORY, intentCategory);
        args.putStringArray(Key.EXCLUDE_PACKAGES, excludePkgs);
        args.putString(Key.PROMPT_TEXT, prompt);
        args.putInt(Key.LIST_TYPE, listType);

        LaunchAppPickerFragment f = new LaunchAppPickerFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mIntentAction = getArguments().getString(Key.INTENT_ACTION);
            mIntentCategory = getArguments().getString(Key.INTENT_CATEGORY);
            mExcludePkgs = getArguments().getStringArray(Key.EXCLUDE_PACKAGES);
            mPromptText = getArguments().getString(Key.PROMPT_TEXT);
            mListType = getArguments().getInt(Key.LIST_TYPE, -1);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (mContentView == null) {
            mItems = new ListItem[0]; // Initial filler that will be replaced later in the AsyncTask.
    
            //Do a null check for the required prompt text
            if (mPromptText == null){
                return null;
            }
    
            mContentView = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(mPromptText))
            .setSingleChoiceItems(mItems, -1, this).create().getView();
    
            mListView = (ListView) mContentView.findViewById(R.id.list);
            mProgressDialog = ProgressDialog.show(mHostActivity, "", getString(R.string.please_wait), true);
    
            (new GetAppListTask(this, mListView)).execute(new GetAppListTask.Param(mIntentAction, mIntentCategory, mExcludePkgs, mListType));
        }
        
        return mContentView;
    }

    private static class GetAppListTask extends AsyncTask<GetAppListTask.Param, Void, Void> {
        static class Param {
            // Locally caching these variables to avoid possible race conditions.
            String mIntentAction;
            String mIntentCategory;
            String[] mExcludePkgs;
            int mListType;

            Param(String intentAction, String intentCategory, String[] excludePkgs, int listType) {
                mIntentAction = intentAction;
                mIntentCategory = intentCategory;
                mExcludePkgs = excludePkgs;
                mListType = listType;
            }
        }

        LaunchAppPickerFragment mParent;
        ListItem[] mItems;
        ListView mListView;

        GetAppListTask(LaunchAppPickerFragment parent, ListView listView) {
            mParent = parent;
            mListView = listView;
        }
        
        @Override
        protected Void doInBackground(Param... params) {
            Param p = params[0];
            if (p == null)
                return null;

            mItems = UIUtils.getResolvedIntentListItems(p.mIntentAction, p.mIntentCategory,
                    mParent.mHostActivity, p.mExcludePkgs, p.mListType);

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            // check if fragment is still attached otherwise just return.
            if (!mParent.isAdded()) return;
            
            mParent.mProgressDialog.dismiss();
            
            if (mItems == null || mListView == null) {
                return;
            }

            CustomListAdapter adapter = (CustomListAdapter)mListView.getAdapter();
            if (adapter != null) {
                mParent.mItems = mItems;
                adapter.setItemsListArray(mItems);
                adapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Handles the done bottom button click event
     *
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        if (which < mItems.length) {
            // Just to be safe.
            mSelectedItem = mItems[which];
            mHostActivity.onReturn(mSelectedItem, this);
        }
    }

    /**
    * Restores prior instance state in onCreateView.
    *
    * @param savedInstanceState Bundle containing prior instance state
    */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mIntentAction = savedInstanceState.getString(Key.INTENT_ACTION);
        mIntentCategory = savedInstanceState.getString(Key.INTENT_CATEGORY);
        mPromptText = savedInstanceState.getString(Key.PROMPT_TEXT);
        mExcludePkgs = savedInstanceState.getStringArray(Key.EXCLUDE_PACKAGES);
        mListType = savedInstanceState.getInt(Key.LIST_TYPE, -1);
    }

    /**
    * Saves current instance state
    *
    * @param savedInstanceState Bundle containing prior current state
    */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(Key.INTENT_ACTION, mIntentAction);
        outState.putString(Key.INTENT_CATEGORY, mIntentCategory);
        outState.putString(Key.PROMPT_TEXT, mPromptText);
        outState.putStringArray(Key.EXCLUDE_PACKAGES, mExcludePkgs);
        outState.putInt(Key.LIST_TYPE, mListType);
        super.onSaveInstanceState(outState);
    }

}
