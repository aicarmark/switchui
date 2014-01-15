/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 11, 2012	  MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base fragment class for guided pickers.
 * <code><pre>
 *
 * CLASS:
 *  extends Fragment - standard Android fragment
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
public class PickerFragment extends Fragment {

    protected final String SELECTED_ITEM_STR = "current_mode";
    // cjd - making these comments JavaDoc format might be nice.
    //This is most likely a string like mode etc.
    protected Object mSelectedItem;
    //onCreateView returns this view
    protected View mContentView;
    //Launch intent for the picker - used for edit mode
    protected Intent mInputConfigs;
    //Output intent that carries the data that each fragment returns
    protected Intent mOutputConfigs;

    // cjd - since this interface is public, make the comment a JavaDoc comment.
    //Return interface used to communicate between fragment and host activity
    public interface ReturnFromFragment {
        /**
         * Pass a Fragment reference and its Return value to container Activity.
         *
         * @param fragment Fragment returning the value
         * @param returnValue Value to pass up to container Activity
         */
        public void onReturn(Object returnValue, PickerFragment fromFragment);
    }

    protected MultiScreenPickerActivity mHostActivity;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mHostActivity = (MultiScreenPickerActivity) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must extend MultiScreenPickerActivity");
        }
    }

    // cjd - normally public methods have comments
    public void setSelectedItem(final Object item) {
        mSelectedItem = item;
    }

    protected void launchNextFragment(final Fragment nextFragment,
            final int breadCrumbResID, final boolean firstFragment) {
        if (mHostActivity != null) {
            mHostActivity.launchNextFragment(nextFragment, breadCrumbResID, firstFragment);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        // cjd - not sure why this is created as a variable here if never used/referenced.
        //       should it simply be: super.onCreateView(inflater, container, savedInstanceState);
        final View v = super.onCreateView(inflater, container, savedInstanceState);
        // Retrieve saved fragment state, if available
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
        return v;
    }

    /**
     * Override this method to restore prior instance state in onCreateView.
     * This default implementation does nothing.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    protected void restoreInstanceState(final Bundle savedInstanceState) {
    }

    /**
     * Override this method in host activity when instantiating this class to
     * handle the fragment return result.
     * This default implementation does nothing.
     *
     * @param returnValue Value from Fragment
     * @param fragment Fragment returning the value
     */
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
    }
}
