/*
 * @(#)LandingPageActivity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * VHJ384        2012/07/15 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.app2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;

public class LandingPageActivity extends Activity implements Constants,
															 LandingPageFragment.Delegate,
															 AddRuleListFragment.Delegate,
															 EditRuleFragment.Delegate,
															 CopyExistingRuleFragment.Delegate {
															 
	public static final String TAG = LandingPageActivity.class.getSimpleName();
	
	private Bundle mReturnResult;
	private Bundle mCurrentRuleExtras;
	
	private LinearLayout mOverlayTopRow;
	private ObjectAnimator mOverlayTopRowYAnim;
	private ObjectAnimator mOverlayTopRowFadeInAnim;
	private ObjectAnimator mOverlayTopRowFadeOutAnim;
	private AnimatorSet mOverlayTopRowAnim;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// setup initial actionbar
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(getString(R.string.app_name));
		
		setContentView(R.layout.main_frame);
		
		loadInitialFragment();
		initializeOverlayIcon();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
	}

	@Override 
	public void onBackPressed() {
		if (getCurrentFragment() instanceof BackKeyListener) {
			((BackKeyListener) getCurrentFragment()).onBackPressed();
		}
	}
	
	public void setReturnResult(Bundle result) {
		mReturnResult = result;
	}
	
	public Bundle getReturnResult() {
		return mReturnResult;
	}
	
	/**
	 * Returns the current fragment from the back stack.
	 * If back stack is empty returns the Landing Page Fragment.
	 * @return current fragment
	 */
	private Fragment getCurrentFragment() {
		Fragment currentFragment = null;
		FragmentManager fragMan = getFragmentManager();
		int numFragments = fragMan.getBackStackEntryCount();
		
		if (numFragments > 0) {
			String fragName = fragMan.getBackStackEntryAt(numFragments - 1).getName();
			currentFragment = fragMan.findFragmentByTag(fragName);
		} else {
			currentFragment = fragMan.findFragmentByTag(LandingPageFragment.TAG);
		}
		return currentFragment;
	}
	
	
	/**
	 * Set up content view and add the initial LandingPageFragment to the view.
	 */
	private void loadInitialFragment() {
		
		Intent startIntent = getIntent();
		Bundle extras = startIntent.getExtras();		
		long ruleId = extras != null ? extras.getLong(PUZZLE_BUILDER_RULE_ID) : DEFAULT_RULE_ID;
    	
		FragmentManager fragMan = getFragmentManager();
		
		FragmentTransaction landingFragTrans = fragMan.beginTransaction();
		if (null == fragMan.findFragmentByTag(LandingPageFragment.TAG)) {
			landingFragTrans.add(R.id.main_frame, LandingPageFragment.newInstance(), LandingPageFragment.TAG);
		}
		landingFragTrans.commit();
		
		// check if a rule has been passed in through the intent
		// if so, launch rule editor
		if (ruleId != DEFAULT_RULE_ID) {
			FragmentTransaction fragTrans = fragMan.beginTransaction();
			
			fragTrans.setCustomAnimations(
					R.animator.fragment_slide_left_enter,
					R.animator.fragment_slide_left_exit,
					R.animator.fragment_slide_right_enter,
					R.animator.fragment_slide_right_exit
				);
			
			Fragment frag = EditRuleFragment.newInstance(extras, RULE_EDIT);
			fragTrans.replace(R.id.main_frame, frag, EditRuleFragment.TAG);
			fragTrans.addToBackStack(EditRuleFragment.TAG);
			fragTrans.commit();
		}	
	}
	
	/**
	 * Called from LandingPageFragment when the user selects the 
	 * Add Rule Button from the ActionBar.
	 */
	public void showAddRuleList(int numVisibleRules) {
		invalidateOptionsMenu();
		FragmentManager fragMan = getFragmentManager();
		FragmentTransaction fragTrans = fragMan.beginTransaction();

		fragTrans.setCustomAnimations(
			R.animator.fragment_slide_left_enter,
			R.animator.fragment_slide_left_exit,
			R.animator.fragment_slide_right_enter,
			R.animator.fragment_slide_right_exit
		);

		Fragment frag = AddRuleListFragment.newInstance(numVisibleRules);
		fragTrans.replace(R.id.main_frame, frag, AddRuleListFragment.TAG);
		
		fragTrans.addToBackStack(AddRuleListFragment.TAG);
		fragTrans.commit();
	}
	
	/**
	 * Called from LandingPageFragment and AddRuleListFragment
	 * when the user clicks on an existing rule to edit.
	 */
	public void showRuleEditorForRule(Bundle extras, int requestCode, boolean useVerticalTransition) {
		invalidateOptionsMenu();
		FragmentManager fragMan = getFragmentManager();
		FragmentTransaction fragTrans = fragMan.beginTransaction();

		int enterAnimatorResId = -1;
		int exitAnimatorResId = -1;
		if (useVerticalTransition) {
			enterAnimatorResId = R.animator.fragment_slide_top_enter;
			exitAnimatorResId = R.animator.fragment_slide_top_exit;
		} else {
			enterAnimatorResId = R.animator.fragment_slide_left_enter;
			exitAnimatorResId = R.animator.fragment_slide_left_exit;
		}
		
		fragTrans.setCustomAnimations(
			enterAnimatorResId,
			exitAnimatorResId,
			R.animator.fragment_slide_right_enter,
			R.animator.fragment_slide_right_exit
		);

		Fragment frag = EditRuleFragment.newInstance(extras, requestCode);
		fragTrans.replace(R.id.main_frame, frag, EditRuleFragment.TAG);
		
		fragTrans.addToBackStack(EditRuleFragment.TAG);
		fragTrans.commit();
	}

	/**
	 * Called from AddRuleListFragment when user selects
	 * copy existing rule from the menu button.
	 * 
	 * Opens the CopyExistingRuleFragment.
	 */
	public void showCopyExistingRule() {
		invalidateOptionsMenu();
		FragmentManager fragMan = getFragmentManager();
		FragmentTransaction fragTrans = fragMan.beginTransaction();

		fragTrans.setCustomAnimations(
			R.animator.fragment_slide_left_enter,
			R.animator.fragment_slide_left_exit,
			R.animator.fragment_slide_right_enter,
			R.animator.fragment_slide_right_exit
		);

		Fragment frag = CopyExistingRuleFragment.newInstance();
		fragTrans.replace(R.id.main_frame, frag, CopyExistingRuleFragment.TAG);
		
		fragTrans.addToBackStack(CopyExistingRuleFragment.TAG);
		fragTrans.commit();
	}

	/**
	 * The transition fragment is used for the LandingPage to
	 * Rule Editor transition and it displays a blank screen
	 * before the Rule Editor slides down into the window.
	 */
	public void showTransitionFragment() {
		FragmentManager fragMan = getFragmentManager();
		FragmentTransaction fragTrans = fragMan.beginTransaction();

		fragTrans.setCustomAnimations(
			R.animator.fragment_slide_left_enter,
			R.animator.fragment_slide_left_exit,
			R.animator.fragment_slide_right_enter,
			R.animator.fragment_slide_right_exit
		);

		Fragment frag = TransitionFragment.newInstance();
		fragTrans.replace(R.id.main_frame, frag, TransitionFragment.TAG);
		fragTrans.addToBackStack(TransitionFragment.TAG);
		fragTrans.commit();
	}
	
	/* ==========================
	 * OVERLAY ANIMATION METHODS
	 * ==========================*/
	
	/**
	 * Called from LandingPageFragment when launching existing rule
	 * into the rule builder.
	 */
	public void startIconOverlayAnimation(Bundle extras, String ruleName, String ruleState, int resId, int sourceY, boolean isActive, boolean isEnabled) {
		
		mCurrentRuleExtras = extras;
		
		float topOffset = 112f;
    	float startY = sourceY - topOffset;
    	
    	// set the icon resource
    	((ImageView) mOverlayTopRow.findViewById(R.id.rule_icon)).setImageResource(resId);
    	
    	// set rule name
    	TextView ruleNameTextView = (TextView) mOverlayTopRow.findViewById(R.id.title_line);
    	ruleNameTextView.setText(ruleName);
    	
    	// set rule state
    	int colorId = isActive ? R.color.active_blue : R.color.disable_gray;
    	TextView ruleStateTextView = (TextView) mOverlayTopRow.findViewById(R.id.description_line);
    	ruleStateTextView.setText(ruleState);
    	ruleStateTextView.setTextColor(getResources().getColor(colorId));
    	ruleStateTextView.setVisibility(View.VISIBLE);
    	
    	LinearLayout iconWrapper = (LinearLayout) mOverlayTopRow.findViewById(R.id.left_wrapper);
    	iconWrapper.setAlpha(isEnabled ? OPAQUE_APLHA_VALUE : FIFTY_PERECENT_ALPHA_VALUE);
    	
    	mOverlayTopRow.setY(startY); // set start Y coordinate
    	mOverlayTopRow.setAlpha(1.0f);
    	mOverlayTopRow.setVisibility(View.VISIBLE); // set visible

    	
    	// set start and end Y coordinates for animation
    	mOverlayTopRowYAnim.setFloatValues(startY, 0f);
    	mOverlayTopRowAnim.start();
    	showTransitionFragment();
	}
	
	/**
	 * Initialize the overlay icon and its corresponding
	 * animator objects.
	 */
	private void initializeOverlayIcon() {

		mOverlayTopRow = (LinearLayout) findViewById(R.id.overlay_top_row);
		
		LinearLayout switchView = (LinearLayout) mOverlayTopRow.findViewById(R.id.on_off_wrapper);
		switchView.setVisibility(View.INVISIBLE);
		
		mOverlayTopRowYAnim = ObjectAnimator.ofFloat(mOverlayTopRow, "y", 0f, 0f);
		mOverlayTopRowFadeInAnim = ObjectAnimator.ofFloat(mOverlayTopRow, "Alpha", 0f, 1f);
		mOverlayTopRowFadeOutAnim = ObjectAnimator.ofFloat(mOverlayTopRow, "Alpha", 1f, 0f);
		mOverlayTopRowFadeOutAnim.setInterpolator(new AccelerateInterpolator());
        
		mOverlayTopRowYAnim.setDuration(300L);
		mOverlayTopRowFadeInAnim.setDuration(10L);
		mOverlayTopRowFadeOutAnim.setDuration(600L);
		
		mOverlayTopRowAnim = new AnimatorSet();
		mOverlayTopRowAnim.playSequentially(mOverlayTopRowFadeInAnim, mOverlayTopRowYAnim, mOverlayTopRowFadeOutAnim);
		
		mOverlayTopRowFadeOutAnim.addListener(new AnimatorListenerAdapter() {
			public void onAnimationStart(Animator arg0) {
				showRuleEditorForRule(mCurrentRuleExtras, RULE_EDIT, true);
			}
			public void onAnimationEnd(Animator arg0) {
        		mOverlayTopRow.setVisibility(View.GONE);
			}
        });
	}
}