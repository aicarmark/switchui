/*
 * Copyright (C) 2010, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 7, 2012	  MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers;

/**
 * Base activity for guided pickers.
 * <code><pre>
 *
 * CLASS:
 *  extends Activity - Activity base class
 *
 * RESPONSIBILITIES:
 *  Hosts common methods & member variables for guided pickers.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */

import java.util.Locale;
import java.util.Stack;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.smartrules.R;

public class MultiScreenPickerActivity extends Activity implements
            PickerFragment.ReturnFromFragment,
            FragmentManager.OnBackStackChangedListener {

    private static final String TAG = MultiScreenPickerActivity.class.getSimpleName();

    public static final String HELP_FILE_SCHEME = "file";
    public static final String HELP_FILE_PATH = "///android_res/raw/";
    public static final String HELP_FILE_EXT = ".html";


    //mCheckedItem - Used for Precondition editing
    protected int mCheckedItem = -1;
    //selected mode string in the edit intent used by some pickers
    protected String mModeSelected;
    //input & output config intents used to pass information between
    //picker fragments
    protected Intent mInputConfigs;
    protected Intent mOutputConfigs;

    protected MenuItem mHelpMenuItem;
    private String mHelpHTML;
    
    protected CharSequence mActionBarTitle;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_screen_picker_container);
        
        final ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        if (!TextUtils.isEmpty(mActionBarTitle)) {
            ab.setTitle(mActionBarTitle);
        }
        
        final Intent intent = getIntent();
        mInputConfigs = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
        mOutputConfigs = new Intent();

        getFragmentManager().addOnBackStackChangedListener(this);
    }

    /**
     * Launches next fragment, used by the guided pickers to transition
     * from one fragment to another.
     * @param nextFragment next fragment to be shown
     * @param breadCrumbResID bread crumb string to be displayed,if any
     * @param firstFragment boolean to determine if this is the
     *  first fragment in the guided approach
     */
    protected void launchNextFragment(final Fragment nextFragment,
            final int breadCrumbResID, final boolean firstFragment) {

        avoidException();

        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(!firstFragment) {
            transaction.setCustomAnimations(
                    R.animator.fragment_slide_left_enter,
                    R.animator.fragment_slide_left_exit,
                    R.animator.fragment_slide_right_enter,
                    R.animator.fragment_slide_right_exit
            );

            transaction.addToBackStack(getString(breadCrumbResID));
        }

        transaction.setBreadCrumbShortTitle(breadCrumbResID);
        transaction.replace(R.id.fragment_container, nextFragment, getString(breadCrumbResID));
        transaction.commit();

        avoidException(nextFragment);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if(getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            }else {
                finish();
            }
            return true;
        case R.id.menu_help:
            final AlertDialogFragment dialogFragment = AlertDialogFragment
                .newInstance(getTitle().toString(), getHelpHTMLFileUrl());
            dialogFragment.show(getFragmentManager(), item.getTitle().toString());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void setActionBarTitle(final CharSequence title) {
        getActionBar().setTitle(title);
    }

    protected void setActionBarIcon(final int resId) {
        getActionBar().setIcon(resId);
    }

    public void onReturn(final Object returnValue, final PickerFragment fromFragment) {
        // Let the child class(es) override the implementation.
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        boolean returnValue = false;
        if (getHelpHTMLFileUrl() != null) {
            final MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.picker_menu, menu);
            mHelpMenuItem = menu.findItem(R.id.menu_help);
            mHelpMenuItem.setVisible(true);
            returnValue = true;
        }

        return returnValue;
    }

    /**
     * @return the mHelpHTML
     */
    public String getHelpHTMLFileUrl() {
        return mHelpHTML;
    }

    /**
     * @param clazz the class name used to construct a URL for the help file
     */
     public void setHelpHTMLFileUrl(final Class<?> clazz) {
        final String filename = clazz.getSimpleName().toLowerCase(Locale.US);
        final Uri.Builder builder = new Uri.Builder()
        .scheme(HELP_FILE_SCHEME)
        .path(HELP_FILE_PATH + filename + HELP_FILE_EXT);
        setHelpHTMLFileUrl(builder.build().toString());
    }

    /**
     * @param mHelpHTML the mHelpHTML to set
     */
    public void setHelpHTMLFileUrl(final String mHelpHTML) {
        this.mHelpHTML = mHelpHTML;
    }

    public class onHelpItemSelected implements View.OnClickListener {
        public void onClick(final View v) {
            if (mHelpMenuItem != null) {
                onOptionsItemSelected(mHelpMenuItem);
            }
        }
    }

    // The following code is used to avoid the
    // "java.lang.IllegalStateException: The specified child already has a
    // parent. You must call removeView() on the child's parent first."
    //
    // I put the code down here in an attempt to isolate this code and not clutter the
    // real logic.
    //
    // The exception occurs intermittently when the popBackStack or popBackStackImmediate
    // method is called. This is typically done from the onBackPressed method. In order to
    // avoid this exception we need to remove the view of the currently displayed fragment
    // from its parent before displaying the next fragment. The mFragmentsStack
    // stack is used to track the fragments. We push all the fragments of an activity on
    // this stack, including the first one. We do not push the first fragment of the
    // activity on the fragment manager's back stack in order to make navigation work as
    // intended. This is why we had to implement our own stack.
    //

    private final Stack<Fragment> mFragmentsStack = new Stack<Fragment>();

    // Removes the view of the currently displayed fragment from its parent
    private void avoidException() {
        if (!mFragmentsStack.isEmpty()) {
            final Fragment f = mFragmentsStack.peek();
            if (f != null) {
                final View child = f.getView();
                if (child != null) {
                    final ViewParent parent = child.getParent();
                    if (parent != null) {
                        ((ViewGroup) parent).removeView(child);
                    }
                }
            }
        }
    }

    // Push a fragment on mFragmentsStack
    private void avoidException(final Fragment f) {
        mFragmentsStack.push(f);
    }

    // Pop a fragment from mFragmentsStack
    public void onBackStackChanged() {
        final int sizeOfBackStack = getFragmentManager()
                .getBackStackEntryCount();
        final int sizeOfFragmentsStack = mFragmentsStack.size();

        if (sizeOfBackStack < (sizeOfFragmentsStack - 1)) {
            if (!mFragmentsStack.isEmpty()) {
                mFragmentsStack.pop();
            }
        }
    }
}
