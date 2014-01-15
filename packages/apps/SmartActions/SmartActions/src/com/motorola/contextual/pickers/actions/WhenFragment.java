/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/06/19 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;


/**
 * This base class defines the "When your SmartAction starts" and "When your SmartAction ends"
 * picker fragment.  The subclass can specialize the prompt and the action button.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 *  Implements the base class for choosing for the start or end of rule condition.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class WhenFragment extends PickerFragment implements Constants, OnClickListener {


    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving title resource ID */
        String TITLE_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_TITLE_RESOURCE_ID";
        /** Saved instance state key for saving button text resource ID */
        String ACTION_RESOURCE_ID = "com.motorola.contextual.pickers.actions.ACTION_RESOURCE_ID";
    }
    /**
     * This enumeration defines the different states for the when picker.
     */
    enum RuleStates {
        STARTS (R.string.smart_action_starts),
        ENDS (R.string.smart_action_ends);
        //STARTS_ENDS (R.string.smart_action_starts_ends);

        private int mText;

        /**
         * Constructor.
         *
         * @param text - the label for the rule
         */
        RuleStates(final int text) {
            mText = text;
        }

        /**
         * @return the label for the rule
         */
        public int text() {
            return mText;
        }
    }


    /**
     * The ListView that holds the choices.
     */
    protected ListView mListView;
    private int mTitleId;
    private int mActionId;

    /** Callback interface */
    public interface WhenCallback {
        public void handleWhenFragment(Fragment fragment, Object returnValue);
    }
    private WhenCallback mWhenCallback;
    
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static WhenFragment newInstance(final Intent inputConfigs, final Intent outputConfigs, final int title, final int action) {
        
        Bundle args = new Bundle();
        
        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }
    
        args.putInt(Key.TITLE_RESOURCE_ID, title);
        args.putInt(Key.ACTION_RESOURCE_ID, action);
        
        WhenFragment f = new WhenFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mWhenCallback = (WhenCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement WhenCallback");
        }
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
            
            mTitleId = getArguments().getInt(Key.TITLE_RESOURCE_ID, -1);
            mActionId = getArguments().getInt(Key.ACTION_RESOURCE_ID, -1);
        }
    }
    
    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mWhenCallback.handleWhenFragment(fragment, returnValue);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mTitleId < 0 || mActionId < 0){
            return null;
        }

        final ListItem[] items = new ListItem[RuleStates.values().length];

        for (int i = 0; i < items.length; i++) {
            items[i] = new ListItem(null,
                    getString(RuleStates.values()[i].text()), null,
                    ListItem.typeONE, RuleStates.values()[i].ordinal(), null);
        }

        int checkedPos = -1;
        if (mInputConfigs != null) {
            checkedPos = (mInputConfigs.getBooleanExtra(EXTRA_RULE_ENDS, false) ?
                    RuleStates.ENDS.ordinal() : RuleStates.STARTS.ordinal());
        } else {
            checkedPos = RuleStates.STARTS.ordinal();
        }

        final Picker picker = new Picker.Builder(getActivity())
        .setTitle(Html.fromHtml(getString(mTitleId)))
        .setSingleChoiceItems(items, checkedPos, null)
        .setPositiveButton(getString(mActionId), this).create();
        final View v = picker.getView();
        mListView = (ListView) v.findViewById(R.id.list);
        return v;
    }

    /**
     * Required by OnClickListener.
     */
    public void onClick(final DialogInterface dialog, final int which) {
        final boolean isRuleEnds = (mListView.getCheckedItemPosition() == RuleStates.ENDS.ordinal());
        mOutputConfigs.putExtra(EXTRA_RULE_ENDS, isRuleEnds);
        mHostActivity.onReturn(mOutputConfigs, this);
    }

    /**
     * Restores prior instance state in onCreateView.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mTitleId = savedInstanceState.getInt(Key.TITLE_RESOURCE_ID, -1);
        mActionId = savedInstanceState.getInt(Key.ACTION_RESOURCE_ID, -1);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleId);
        outState.putInt(Key.ACTION_RESOURCE_ID, mActionId);
    }
}
