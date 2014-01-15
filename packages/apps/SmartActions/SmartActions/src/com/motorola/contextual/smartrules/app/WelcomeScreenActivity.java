/*
` * @(#)WelcomeScreenActivity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * TQRB48        2012/05/11 NA                Initial version
 * ACD100        2012/05/14 NA                Code Review comments
 * NCBQ76        2012/08/22 IKCORE8-7545      This screen force closes consistently because of broken constructor
 *
 */
package com.motorola.contextual.smartrules.app;

import java.util.List;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;
import com.motorola.contextual.smartrules.util.Util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

/** This activity displays a full-screen layout that appears at
 *  initial launch of the Smart Actions app
 *
 *<code><pre>
 * CLASS:
 *  extends Activity - standard Android Activity
 *
 * RESPONSIBILITIES:
 *  Show the user introductory text and branding.
 *  Directs the user towards initial action.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class WelcomeScreenActivity extends Activity {

    private static final int CLIP_LEVEL_MAX = 10000;

    /** Animation durations
     */
    private static interface AnimDurations {
        long BG_IN              = 430;
        long TITLE_IN           = 600;
        long TITLE_OUT          = 533;
        long TITLE_STRIPE_IN    = 533;
        long TEXT_IN            = 533;
        long BUTTON_IN          = 733;
    }

    /** Delay times between animations
     */
    private static interface DelayDurations {
        long PAUSE_00 = 500;
        long PAUSE_01 = 500;
        long PAUSE_02 = 700;
        long PAUSE_03 = 533;
        long PAUSE_04 = 2000;
        long PAUSE_05 = 150;
        long PAUSE_06 = 700;
    }

    private boolean mFirstTime;
    private static boolean mTeachScreenShown;

    private View mBackgroundView;

    private View mMotoLogo;

    private View mTopTitle;
    private View mTopTitleTextH;
    private View mTopTitleTextV;

    private View mTopTitleStripe;
    private View mBottomTitle;
    private View mInfoText;
    private View mLearnMoreButton;
    private View mGetStartedButton;
    private View mGetStartedButtonSolo;

    private ValueAnimator mFadeBGImage;
    private ValueAnimator mMotoLogoHorizIn;
    private ValueAnimator mMotoLogoHorizOut;
    private ValueAnimator mTopTitleHorizIn;
    private ValueAnimator mTopTitleVertOut;
    private ValueAnimator mTopTitleStripeIn;
    private ValueAnimator mFadeText;
    private ValueAnimator mFadeGetStartedBtn;
    private ValueAnimator mFadeLearnMoreBtn;
    private ValueAnimator mFadeLearnMoreBtnSolo;

    /** onCreate()
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.welcome_screen);

        mFirstTime = true;

        mBackgroundView     = findViewById(R.id.bg_image);
        mMotoLogo           = findViewById(R.id.title_moto_logo);
        mTopTitleTextH      = findViewById(R.id.title_red_text_h);
        mTopTitleTextV      = findViewById(R.id.title_red_text_v);
        mTopTitleStripe     = findViewById(R.id.title_red_stripe);
        mTopTitle           = findViewById(R.id.title_red);
        mBottomTitle        = findViewById(R.id.title_white);
        mInfoText           = findViewById(R.id.info_text);

        mLearnMoreButton        = findViewById(R.id.learn_more_button);
        mGetStartedButton       = findViewById(R.id.get_started_button);
        mGetStartedButtonSolo   = findViewById(R.id.get_started_button_2);

        mLearnMoreButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                doLearnMore();
            }
        });

        mGetStartedButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                doGetStarted();
            }
        });

        mGetStartedButtonSolo.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                doGetStarted();
            }
        });
    }

    /** onResume()
     *  Very simple because
     *      - fixed orientation (specified in manifest)
     *      - animation plays at first start only
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Check the number of rules
        // If rules were created while paused then we no longer need this screen.
        if (getNumberRules(this.getApplicationContext()) > 0)
            this.finish();
    }

    /** onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear dynamically allocated listeners and animators.
        mLearnMoreButton.setOnClickListener(null);
        mGetStartedButton.setOnClickListener(null);
        mGetStartedButtonSolo.setOnClickListener(null);

        mTopTitleHorizIn.removeAllListeners();

        mFadeBGImage            = null;
        mMotoLogoHorizIn        = null;
        mMotoLogoHorizOut       = null;
        mTopTitleHorizIn        = null;
        mTopTitleVertOut        = null;
        mTopTitleStripeIn       = null;
        mFadeText               = null;
        mFadeGetStartedBtn      = null;
        mFadeLearnMoreBtn       = null;
        mFadeLearnMoreBtnSolo   = null;
    }

    /** Custom animation interpolater for smooth ease-in/out behaviors
     */
    private class CustomEaseIn implements TimeInterpolator {
        public float getInterpolation(float input) {
            try {
                return  1 - ((float)Math.pow(2, -10 * input));
            } catch (Exception e) {
                return 0;
            }

        }
    }

    // string constants used when saving state of teach text shown.
    private static final String WELCOME_SCREEN_PREFS = "WELCOME_SCREEN_PREFS";
    private static final String TEACH_SCREEN_SHOWN = "TEACH_SCREEN_SHOWN";

    @Override
    protected void onStop() {
        // save the state to mark that teach text has already been shown.
        SharedPreferences wecomeScreenPrefs = getSharedPreferences(WELCOME_SCREEN_PREFS, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = wecomeScreenPrefs.edit();
        editor.putBoolean(TEACH_SCREEN_SHOWN, mTeachScreenShown);
        editor.apply();

        super.onStop();
    }

    /** onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();

        // If this is our first call to onStart in this instancing then play the start animation
        if (mFirstTime) {
            resetLayout();

            SharedPreferences wecomeScreenPrefs = getSharedPreferences(WELCOME_SCREEN_PREFS, Activity.MODE_PRIVATE);
            mTeachScreenShown = wecomeScreenPrefs.getBoolean(TEACH_SCREEN_SHOWN, false);;

            AnimatorSet animSet = new AnimatorSet();

            // Animation - Fade-in the background image
            mFadeBGImage = ObjectAnimator.ofFloat(mBackgroundView, "alpha", 0f, 1f);
            mFadeBGImage.setDuration(AnimDurations.BG_IN);
            animSet.play(mFadeBGImage)
                   .after(DelayDurations.PAUSE_05);

            // Animation - Mask-in horizontally the Motorola logo
            mMotoLogoHorizIn = ObjectAnimator.ofInt(((ImageView)mMotoLogo).getDrawable(),
                                                    "level", 0, CLIP_LEVEL_MAX);
            mMotoLogoHorizIn.setDuration(AnimDurations.TITLE_IN);
            mMotoLogoHorizIn.setInterpolator(new CustomEaseIn());
            animSet.play(mMotoLogoHorizIn)
                   .after(DelayDurations.PAUSE_06);

            // Animation - Mask-out horizontally the Motorola logo
            mMotoLogoHorizOut = ObjectAnimator.ofInt(((ImageView)mMotoLogo).getDrawable(),
                                                    "level", CLIP_LEVEL_MAX, 0);
            mMotoLogoHorizOut.setDuration(AnimDurations.TITLE_OUT);
            mMotoLogoHorizOut.setInterpolator(new CustomEaseIn());
            animSet.play(mMotoLogoHorizOut)
                   .after(DelayDurations.PAUSE_05)
                   .after(mMotoLogoHorizIn);

            // Animation - Mask-in horizontally the red title bar
            mTopTitleHorizIn = ObjectAnimator.ofInt(((ImageView)mTopTitleTextH).getDrawable(),
                                                    "level", 0, CLIP_LEVEL_MAX);
            mTopTitleHorizIn.setDuration(AnimDurations.TITLE_IN);
            mTopTitleHorizIn.setInterpolator(new CustomEaseIn());
            animSet.play(mTopTitleHorizIn)
                   .after(DelayDurations.PAUSE_00)
                   .after(mMotoLogoHorizOut);

            mTopTitleHorizIn.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    // Switch the visibility of our horizontal and vertically animating title bars
                    // at the end of the animation
                    mTopTitleTextH.setVisibility(View.INVISIBLE);
                    ((ImageView)mTopTitleTextV).getDrawable().setLevel(CLIP_LEVEL_MAX);
                    mTopTitleTextV.setVisibility(View.VISIBLE);

                    // Make the white title bar visible beneath the red
                    mBottomTitle.setVisibility(View.VISIBLE);
                }
            });

            // Animation - Mask-out vertically the red title bar
            mTopTitleVertOut = ObjectAnimator.ofInt(((ImageView)mTopTitleTextV).getDrawable(),
                                                    "level", CLIP_LEVEL_MAX, 0);
            mTopTitleVertOut.setDuration(AnimDurations.TITLE_OUT);
            mTopTitleVertOut.setInterpolator(new CustomEaseIn());
            animSet.play(mTopTitleVertOut)
                   .after(DelayDurations.PAUSE_00)
                   .after(mTopTitleHorizIn);

            // Animation - Mask-in vertically the red stripe
            mTopTitleStripeIn = ObjectAnimator.ofInt(((ImageView)mTopTitleStripe).getDrawable(),
                                                     "level", 0, CLIP_LEVEL_MAX);
            mTopTitleStripeIn.setDuration(AnimDurations.TITLE_STRIPE_IN);
            mTopTitleStripeIn.setInterpolator(new CustomEaseIn());
            animSet.play(mTopTitleStripeIn)
                   .after(DelayDurations.PAUSE_01)
                   .after(mTopTitleVertOut);

            // Animation - Fade-in the text
            mFadeText = ObjectAnimator.ofFloat(mInfoText, "alpha", 0f, 1f);
            mFadeText.setDuration(AnimDurations.TEXT_IN);
            mFadeText.setInterpolator(new CustomEaseIn());
            animSet.play(mFadeText)
                   .after(DelayDurations.PAUSE_02)
                   .after(mTopTitleStripeIn);

            // Animation - Fade-in the buttons
            mFadeGetStartedBtn = ObjectAnimator.ofFloat(mLearnMoreButton,
                                                        "alpha", 0f, 1f);
            mFadeGetStartedBtn.setDuration(AnimDurations.BUTTON_IN);
            mFadeGetStartedBtn.setInterpolator(new CustomEaseIn());
            animSet.play(mFadeGetStartedBtn)
                   .after(DelayDurations.PAUSE_03)
                   .after(mFadeText);

            mFadeLearnMoreBtn = ObjectAnimator.ofFloat(mGetStartedButton,
                                                       "alpha", 0f, 1f);
            mFadeLearnMoreBtn.setDuration(AnimDurations.BUTTON_IN);
            mFadeLearnMoreBtn.setInterpolator(new CustomEaseIn());
            animSet.play(mFadeLearnMoreBtn)
                   .after(DelayDurations.PAUSE_03)
                   .after(mFadeText);

            mFadeLearnMoreBtnSolo = ObjectAnimator.ofFloat(mGetStartedButtonSolo,
                                                           "alpha", 0f, 1f);
            mFadeLearnMoreBtnSolo.setDuration(AnimDurations.BUTTON_IN);
            mFadeLearnMoreBtnSolo.setInterpolator(new CustomEaseIn());
            animSet.play(mFadeLearnMoreBtnSolo)
                   .after(DelayDurations.PAUSE_03)
                   .after(mFadeText);

            // Play the assembled animation sequence
            animSet.start();
            mFirstTime = false;
        }
    }

    /** onBackPressed()
     *    Overridden so that back does not go back to the main/parent
     *    activity but one entry further back in the stack.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /** Reset dynamic layout items to initial visibility
     */
    private void resetLayout() {
        mMotoLogo.setVisibility(View.VISIBLE);
        mTopTitle.setVisibility(View.VISIBLE);
        mTopTitleTextH.setVisibility(View.VISIBLE);
        mTopTitleTextV.setVisibility(View.INVISIBLE);
        mTopTitleStripe.setVisibility(View.VISIBLE);
        mBottomTitle.setVisibility(View.INVISIBLE);

        mBackgroundView.setAlpha(0f);
        mInfoText.setAlpha(0f);
        mLearnMoreButton.setAlpha(0f);
        mGetStartedButton.setAlpha(0f);
        mGetStartedButtonSolo.setAlpha(0f);

        ((ImageView)mMotoLogo).getDrawable().setLevel(0);
        ((ImageView)mTopTitleTextH).getDrawable().setLevel(0);
        ((ImageView)mTopTitleTextV).getDrawable().setLevel(0);
        ((ImageView)mTopTitleStripe).getDrawable().setLevel(0);

        setSingleButton(false);
    }

    /** Reset dynamic layout items to initial visibility
     */
    private void setSingleButton(boolean useSingle) {
        if (useSingle) {
            mLearnMoreButton.setVisibility(View.GONE);
            mGetStartedButton.setVisibility(View.GONE);

            mGetStartedButtonSolo.setVisibility(View.VISIBLE);
        } else {
            mLearnMoreButton.setVisibility(View.VISIBLE);
            mGetStartedButton.setVisibility(View.VISIBLE);

            mGetStartedButtonSolo.setVisibility(View.GONE);
        }
    }

    /** Launch the help activity
     */
    private void doLearnMore() {
        Util.showHelp(this);
    }

    /** Implement "get started" button behavior which goes to the rule builder,
     *  sometimes showing a dialog first
     */
    private void doGetStarted() {
        if(mTeachScreenShown) {
            AddRuleListActivity.startRuleBuilder(this);
        } else {
            launchTeachText();
        }
    }

    /** Launch the teach text dialog
     */
    private void launchTeachText() {
        // Create a dialog fragment to be shown
        // HSHIEH: Instead, for this behavior, we should just be using an activity+sharedpref. At this point, further refactoring might be risky, however.
        DialogFragment teachDialog = TeachDialogFragment.newInstance();
        teachDialog.show(getFragmentManager(), "Teach Screen");
    }

    /**
     * Create the teach text dialog
     */
    public static class TeachDialogFragment extends DialogFragment {

        //Constructor for the TeachDialogFragment
        static TeachDialogFragment newInstance() {
            return new TeachDialogFragment();
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Necessary to make the dialog properly full-screen
            setStyle(DialogFragment.STYLE_NO_FRAME,android.R.style.Theme_Translucent);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.teach_screen, container, false);
            // Set the button to exit the dialog
            Button button = (Button)view.findViewById(R.id.next_button);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mTeachScreenShown = true;
                    AddRuleListActivity.startRuleBuilder(getActivity());
                    dismiss();
                }
            });
            return view;
        }
    }

    /* Look up the rule list and return its length
     */
    protected int getNumberRules(Context context) {
        int numRules = 0;

        List<Rule> ruleList = null;
        ruleList = new UiAbstractionLayer().fetchLandingPageRulesList(context);
        if (ruleList != null)
            numRules = ruleList.size();

        return numRules;
    }

}
