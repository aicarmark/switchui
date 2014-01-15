/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.RomUtility;
import com.android.contacts.calllog.CallLogFragment;
import com.android.contacts.dialpad.DialpadFragment;
import com.android.contacts.interactions.PhoneNumberInteraction;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.PhoneFavoriteFragment;
import com.android.contacts.list.PhoneNumberPickerFragment;
import com.android.contacts.activities.TransactionSafeActivity;
import com.android.contacts.SimUtility;
import com.android.contacts.util.AccountFilterUtil;
import com.android.internal.telephony.ITelephony;
import com.motorola.contacts.util.MEDialer; //MOTO Dialer Code IKHSS6-723

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.UI;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

// Motorola, pxkq63, 2012/1/3, IKHSS7-2761, FID 35964 Call Setting for kor
import com.android.contacts.ContactsApplication;
import com.motorola.android.telephony.PhoneModeManager;
/**
 * The dialer activity that has one tab with the virtual 12key
 * dialer, a tab with recent calls in it, a tab with the contacts and
 * a tab with the favorite. This is the container and the tabs are
 * embedded using intents.
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 */
public class DialtactsActivity extends TransactionSafeActivity {
    private static final String TAG = "DialtactsActivity";

    private static final boolean DEBUG = false;

    /** Used to open Call Setting */
    private static final String PHONE_PACKAGE = "com.android.phone";
    private static final String CALL_SETTINGS_CLASS_NAME =
            "com.android.phone.CallFeaturesSetting";
    private static final String CALL_SETTINGS_CLASS_NAME_DUAL_MODE =
            "com.android.phone.DualCallSettings";
    private static final String CALLLOG_SHORTCUT_COMPONENT =
            "com.android.contacts.CallLogShortcut";

    /**
     * Copied from PhoneApp. See comments in Phone app for more detail.
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    public static final String CALL_ORIGIN_DIALTACTS =
            "com.android.contacts.activities.DialtactsActivity";

    /**
     * Just for backward compatibility. Should behave as same as {@link Intent#ACTION_DIAL}.
     */
    //private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER"; //comment by MOTO

    /** Used both by {@link ActionBar} and {@link ViewPagerAdapter} */
    // MOTO Dialer Code IKHSS7-2637 Start
    public static final int TAB_INDEX_DIALER = 0;
    public static final int TAB_INDEX_CALL_LOG = 1;
    public static final int TAB_INDEX_FAVORITES = 2;
    // MOTO Dialer Code IKHSS7-2637 End

    private static final int TAB_INDEX_COUNT = 3;

    private SharedPreferences mPrefs;

    /** Last manually selected tab index */
    private static final String PREF_LAST_MANUALLY_SELECTED_TAB =
            "DialtactsActivity_last_manually_selected_tab";
    private static final int PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT = TAB_INDEX_DIALER;


    private static final int SUBACTIVITY_ACCOUNT_FILTER = 1;

    // MOTO Dialer Code Start
    private boolean isHandlingNewIntent; // MOTO A wcs077 13280
    private static final String MIRROR_PUBLIC_STATE = "com.motorola.intent.action.mirrormodestate";
    private static final String MIRROR_STATE = "mirror";
    private final BroadcastReceiver mHdmiReceiver = new HdmiBroadcastReceiver();

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static int DBG_LEVEL = 2;
    public static final boolean DBG =
            (DialtactsActivity.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    public static final boolean VDBG =
            (DialtactsActivity.DBG_LEVEL >= 2) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    // use this flag to mark critical log,
    // !!!!!!!!pls attention only very critical log which need debug issue on userbld can use this flag!!!!!!
    public static final boolean CDBG = true;
    //MOTO Dialer Code - IKHSS6-583 - End

    private boolean isContactsSearchEnabled = false;
    // MOTO Dialer Code End

    /**
     * Listener interface for Fragments accommodated in {@link ViewPager} enabling them to know
     * when it becomes visible or invisible inside the ViewPager.
     */
    public interface ViewPagerVisibilityListener {
        public void onVisibilityChanged(boolean visible);
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_INDEX_DIALER:
                    if(DBG) log("DialpadFragment instance created");//TBD, test which fragment created.
                    return new DialpadFragment();
                case TAB_INDEX_CALL_LOG:
                    if(DBG) log("CallLogFragment instance created");
                    return new CallLogFragment();
                case TAB_INDEX_FAVORITES:
                    if(DBG) log("PhoneFavoriteFragment instance created");
                    return new PhoneFavoriteFragment();
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }
    }

//Google 4.0.4 start
    /**
     * True when the app detects user's drag event. This variable should not become true when
     * mUserTabClick is true.
     *
     * During user's drag or tab click, we shouldn't show fake buttons but just show real
     * ActionBar at the bottom of the screen, for transition animation.
     */
//    boolean mDuringSwipe = false;
    /**
     * True when the app detects user's tab click (at the top of the screen). This variable should
     * not become true when mDuringSwipe is true.
     *
     * During user's drag or tab click, we shouldn't show fake buttons but just show real
     * ActionBar at the bottom of the screen, for transition animation.
     */
//    boolean mUserTabClick = false;
//Google 4.0.4 End

    private class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position {@link #onPageSelected(int)}
         * specified.
         */
        private int mNextPosition = -1;

        @Override
        public void onPageScrolled(
                int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if (DEBUG) Log.d(TAG, "onPageSelected: " + position);
            final ActionBar actionBar = getActionBar();

// Google 4.0.4 Start
//            if (mDialpadFragment != null && !mDuringSwipe) {
//                if (DEBUG) {
//                    Log.d(TAG, "Immediately show/hide fake menu buttons. position: "
//                            + position + ", dragging: " + mDuringSwipe);
//                }
//                mDialpadFragment.updateFakeMenuButtonsVisibility(
//                        position == TAB_INDEX_DIALER && !mDuringSwipe);
//            }
//Google 4.0.4 End

            if (mCurrentPosition == position) {
                Log.w(TAG, "Previous position and next position became same (" + position + ")");
            }
            if(DBG) log("onPageSelected: mCurrentPosition = " + mCurrentPosition + " new position = " + position);
            actionBar.selectTab(actionBar.getTabAt(position));
            mNextPosition = position;
        }

        public void setCurrentPosition(int position) {
            if(DBG) log("setCurrentPosition: position = " + position);
            mCurrentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) { //TBD, test with setCurrentTab.
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if(DBG) log("onPageScrollStateChanged: mCurrentPosition = " + mCurrentPosition + " mNextPosition = " + mNextPosition);
                    //MOT Dialer Code Start - IKHSS6-1679
                    if ((mCurrentPosition >= 0) && (mNextPosition >= 0) && (mCurrentPosition != mNextPosition)) {
// Google 4.0.4 Start
//                    // Interpret IDLE as the end of migration (both swipe and tab click)
//                    mDuringSwipe = false;
//                    mUserTabClick = false;
// Google 4.0.4 End

                        sendFragmentVisibilityChange(mCurrentPosition, false);
                        sendFragmentVisibilityChange(mNextPosition, true);
                        mCurrentPosition = mNextPosition;
                        invalidateOptionsMenu();
                    }
                    //MOT Dialer Code End - IKHSS6-1679
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING: {
// Google 4.0.4 Start
//                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_DRAGGING");
//                    mDuringSwipe = true;
//                    mUserTabClick = false;
//
//                    if (mCurrentPosition == TAB_INDEX_DIALER) {
//                        sendFragmentVisibilityChange(TAB_INDEX_DIALER, false);
//                        sendFragmentVisibilityChange(TAB_INDEX_CALL_LOG, true);
//                        invalidateOptionsMenu();
//                    }
//                    break;
// Google 4.0.4 End
                }
                case ViewPager.SCROLL_STATE_SETTLING: {
// Google 4.0.4 Start
//                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_SETTLING");
//                    mDuringSwipe = true;
//                    mUserTabClick = false;
//                    break;
// Google 4.0.4 End
                }
                default:
                    break;
            }
        }
    }

    private String mFilterText;

    /** Enables horizontal swipe between Fragments. */
    private ViewPager mViewPager;
    private final PageChangeListener mPageChangeListener = new PageChangeListener();
    private DialpadFragment mDialpadFragment;
    private CallLogFragment mCallLogFragment;
    private PhoneFavoriteFragment mPhoneFavoriteFragment;

    private final ContactListFilterListener mContactListFilterListener =
            new ContactListFilterListener() {
        @Override
        public void onContactListFilterChanged() {
            boolean doInvalidateOptionsMenu = false;

            if (mPhoneFavoriteFragment != null && mPhoneFavoriteFragment.isAdded()) {
                mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
                doInvalidateOptionsMenu = true;
            }

            if (mSearchFragment != null && mSearchFragment.isAdded()) {
                mSearchFragment.setFilter(mContactListFilterController.getFilter());
                doInvalidateOptionsMenu = true;
            } else {
                Log.w(TAG, "Search Fragment isn't available when ContactListFilter is changed");
            }

            if (doInvalidateOptionsMenu) {
                invalidateOptionsMenu();
            }
        }
    };

    private final TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) Log.d(TAG, "onTabUnselected(). tab: " + tab);
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
// Google 4.0.4 Start
//            if (DEBUG) {
//                Log.d(TAG, "onTabSelected(). tab: " + tab + ", mDuringSwipe: " + mDuringSwipe);
//            }
//            // When the user swipes the screen horizontally, this method will be called after
//            // ViewPager.SCROLL_STATE_DRAGGING and ViewPager.SCROLL_STATE_SETTLING events, while
//            // when the user clicks a tab at the ActionBar at the top, this will be called before
//            // them. This logic interprets the order difference as a difference of the user action.
//            if (!mDuringSwipe) {
//                if (mDialpadFragment != null) {
//                    if (DEBUG) Log.d(TAG, "Immediately hide fake buttons for tab selection case");
//                    mDialpadFragment.updateFakeMenuButtonsVisibility(false);
//                }
//               mUserTabClick = true;
//            }
// Google 4.0.4 End

            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }

            // During the call, we don't remember the tab position.
            if (!DialpadFragment.phoneIsInUse()) {
                // Remember this tab index. This function is also called, if the tab is set
                // automatically in which case the setter (setCurrentTab) has to set this to its old
                // value afterwards
                mLastManuallySelectedFragment = tab.getPosition();
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) Log.d(TAG, "onTabReselected");
        }
    };

    /**
     * Fragment for searching phone numbers. Unlike the other Fragments, this doesn't correspond
     * to tab but is shown by a search action.
     */
    private PhoneNumberPickerFragment mSearchFragment;
    /**
     * True when this Activity is in its search UI (with a {@link SearchView} and
     * {@link PhoneNumberPickerFragment}).
     */
    private boolean mInSearchUi;
    private SearchView mSearchView;
    /*Modifyed for switchuitwo-455 begin*/
    private PopupMenu popupMenu;
    private final OnClickListener mFilterOptionClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(popupMenu == null) {
            	popupMenu = new PopupMenu(DialtactsActivity.this, view);
            	final Menu menu = popupMenu.getMenu();
                popupMenu.inflate(R.menu.dialtacts_search_options);
                final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
                filterOptionMenuItem.setOnMenuItemClickListener(mFilterOptionsMenuItemClickListener);
                final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
                addContactOptionMenuItem.setIntent(
                        new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
            } 
            popupMenu.show();
        }
        /*Modifyed for switchuitwo-455 end*/
    };

    /**
     * The index of the Fragment (or, the tab) that has last been manually selected.
     * This value does not keep track of programmatically set Tabs (e.g. Call Log after a Call)
     */
    private int mLastManuallySelectedFragment;

    private ContactListFilterController mContactListFilterController;
    private OnMenuItemClickListener mFilterOptionsMenuItemClickListener =
            new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
        	if(RomUtility.isOutofMemory()){
            	Toast.makeText(DialtactsActivity.this, R.string.rom_full, Toast.LENGTH_LONG).show();
            	return true;
            }
            AccountFilterUtil.startAccountFilterActivityForResult(
                    DialtactsActivity.this, SUBACTIVITY_ACCOUNT_FILTER);
            return true;
        }
    };

    private OnMenuItemClickListener mSearchMenuItemClickListener =
            new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            enterSearchUi();
            return true;
        }
    };

    /**
     * Listener used when one of phone numbers in search UI is selected. This will initiate a
     * phone call using the phone number.
     */
    private final OnPhoneNumberPickerActionListener mPhoneNumberPickerActionListener =
            new OnPhoneNumberPickerActionListener() {
                @Override
                public void onPickPhoneNumberAction(Uri dataUri) {
                      // Specify call-origin so that users will see the previous tab instead of
                    // CallLog screen (search UI will be automatically exited).
                    PhoneNumberInteraction.startInteractionForPhoneCall(
                            DialtactsActivity.this, dataUri,
                            CALL_ORIGIN_DIALTACTS, MEDialer.DialFrom.CONTACTSEARCH); //MOTO Dialer Code IKHSS6-723
                }

                @Override
                public void onShortcutIntentCreated(Intent intent) {
                    Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
                }

                @Override
                public void onHomeInActionBarSelected() {
                    exitSearchUi();
                }
    };

    /**
     * Listener used to send search queries to the phone search fragment.
     */
    private final OnQueryTextListener mPhoneSearchQueryTextListener =
            new OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    View view = getCurrentFocus();
                    if (view != null) {
                        hideInputMethod(view);
                        view.clearFocus();
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Show search result with non-empty text. Show a bare list otherwise.
                    if (mSearchFragment != null) {
                        mSearchFragment.setQueryString(newText, true);
                    }
                    return true;
                }
    };

    /**
     * Listener used to handle the "close" button on the right side of {@link SearchView}.
     * If some text is in the search view, this will clean it up. Otherwise this will exit
     * the search UI and let users go back to usual Phone UI.
     *
     * This does _not_ handle back button.
     */
    private final OnCloseListener mPhoneSearchCloseListener =
            new OnCloseListener() {
                @Override
                public boolean onClose() {
                    if (!TextUtils.isEmpty(mSearchView.getQuery())) {
                        mSearchView.setQuery(null, true);
                    }
                    return true;
                }
    };

    private final View.OnLayoutChangeListener mFirstLayoutListener
            = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            v.removeOnLayoutChangeListener(this); // Unregister self.
            addSearchFragment();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        if(CDBG) log("onCreate enter");
        super.onCreate(icicle);

        final Intent intent = getIntent();
        fixIntent(intent);

        setContentView(R.layout.dialtacts_activity);

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.addListener(mContactListFilterListener);

        //Mot Dialer Code Start - IKHSS6UPGR-7991
        isContactsSearchEnabled = getResources().getBoolean(R.bool.config_enable_contacts_search_in_phone);
        //Google add layout change listener, in order to add search fragment,
        //if contact search is enabled, no need to add search fragment, so will
        //not add this listener.
        if (!isContactsSearchEnabled) {
            findViewById(R.id.dialtacts_frame).addOnLayoutChangeListener(mFirstLayoutListener);
        }
        //Mot Dialer Code End - IKHSS6UPGR-7991

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new ViewPagerAdapter(getFragmentManager()));
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        // Setup the ActionBar tabs (the order matches the tab-index contants TAB_INDEX_*)
        setupDialer();
        setupCallLog();
        setupFavorites();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        // Load the last manually loaded tab
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mLastManuallySelectedFragment = mPrefs.getInt(PREF_LAST_MANUALLY_SELECTED_TAB,
                PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT);
        if (mLastManuallySelectedFragment >= TAB_INDEX_COUNT) {
            // Stored value may have exceeded the number of current tabs. Reset it.
            mLastManuallySelectedFragment = PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT;
        }

        setCurrentTab(intent);

        if (UI.FILTER_CONTACTS_ACTION.equals(intent.getAction())
                && icicle == null) {
            setupFilterText(intent);
        }

        //MOTO Dialer Code Start
        // for hdmi
        IntentFilter hdmiFilter = new IntentFilter(MIRROR_PUBLIC_STATE);
        if(DBG) log("registerReceiver");
        registerReceiver(mHdmiReceiver, hdmiFilter);
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        // The activity was launched for the first time
        isHandlingNewIntent = false; // MOTO A wcs077 13280

        //MOTO Dialer Code start - IKHSS7-21952
        //If screen rotate, activity call onCreate() to create a new instance.
        //In that case it is not the first launch, so restore to last manually selected tab
        if(icicle != null) {
           final int previousItemIndex = mViewPager.getCurrentItem();
           mViewPager.setCurrentItem(mLastManuallySelectedFragment, false /* smoothScroll */);
           if (previousItemIndex != mLastManuallySelectedFragment) {
               sendFragmentVisibilityChange(previousItemIndex, false);
           }
           mPageChangeListener.setCurrentPosition(mLastManuallySelectedFragment);
           sendFragmentVisibilityChange(mLastManuallySelectedFragment, true);
        }
        ///MOTO Dialer Code end - IKHSS7-21952
        if(DBG) log("Phone State is idle...displaying dialer screen");
        if(CDBG) log("onCreate exit");
        //MOTO Dialer Code End

        // MOT CHINA
        SimUtility.queryAllSimType(getContentResolver());
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (mPhoneFavoriteFragment != null) {
            mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
            if(DBG) log("onStart favorite fragment set filter");  //TBD, test whether get created.
        }
        if (mSearchFragment != null) {
            mSearchFragment.setFilter(mContactListFilterController.getFilter());
            if(DBG) log("onStart search fragment set filter");
        }

//Google 4.0.4 Start
//        if (mDuringSwipe || mUserTabClick) {
//            if (DEBUG) Log.d(TAG, "reset buggy flag state..");
//            mDuringSwipe = false;
//            mUserTabClick = false;
//        }
//Google 4.0.4 End
    }

    @Override
    public void onDestroy() {
        if(CDBG) log("onDestroy enter");
        super.onDestroy();
        mContactListFilterController.removeListener(mContactListFilterListener);
        if (mHdmiReceiver != null) {
            unregisterReceiver(mHdmiReceiver);
        }
        if(CDBG) log("onDestroy exit");
    }

    /**
     * Add search fragment.  Note this is called during onLayout, so there's some restrictions,
     * such as executePendingTransaction can't be used in it.
     */
    private void addSearchFragment() {
        // In order to take full advantage of "fragment deferred start", we need to create the
        // search fragment after all other fragments are created.
        // The other fragments are created by the ViewPager on the first onMeasure().
        // We use the first onLayout call, which is after onMeasure().

        // Just return if the fragment is already created, which happens after configuration
        // changes.
        if (mSearchFragment != null) return;

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment searchFragment = new PhoneNumberPickerFragment();

        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.dialtacts_frame, searchFragment);
        ft.hide(searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        final View searchViewLayout =
                getLayoutInflater().inflate(R.layout.dialtacts_custom_action_bar, null);
        mSearchView = (SearchView) searchViewLayout.findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(mPhoneSearchQueryTextListener);
        mSearchView.setOnCloseListener(mPhoneSearchCloseListener);
        // Since we're using a custom layout for showing SearchView instead of letting the
        // search menu icon do that job, we need to manually configure the View so it looks
        // "shown via search menu".
        // - it should be iconified by default
        // - it should not be iconified at this time
        // See also comments for onActionViewExpanded()/onActionViewCollapsed()
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setQueryHint(getString(R.string.hint_findContacts));
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(view.findFocus());
                }
            }
        });

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            // Filter option menu should be shown on the right side of SearchView.
            final View filterOptionView = searchViewLayout.findViewById(R.id.search_option);
            filterOptionView.setVisibility(View.VISIBLE);
            filterOptionView.setOnClickListener(mFilterOptionClickListener);
        }

        getActionBar().setCustomView(searchViewLayout,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // This method can be called before onCreate(), at which point we cannot rely on ViewPager.
        // In that case, we will setup the "current position" soon after the ViewPager is ready.
        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;
        if(DBG) log("onAttachFragment, currentPosition = " + currentPosition);//TBD, test

        if (fragment instanceof DialpadFragment) {
            mDialpadFragment = (DialpadFragment) fragment;
            mDialpadFragment.setListener(mDialpadListener);
            if (currentPosition == TAB_INDEX_DIALER) {
                mDialpadFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof CallLogFragment) {
            mCallLogFragment = (CallLogFragment) fragment;
            if (currentPosition == TAB_INDEX_CALL_LOG) {
                mCallLogFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof PhoneFavoriteFragment) {
            mPhoneFavoriteFragment = (PhoneFavoriteFragment) fragment;
            mPhoneFavoriteFragment.setListener(mPhoneFavoriteListener);
            if (mContactListFilterController != null
                    && mContactListFilterController.getFilter() != null) {
                mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
            }
        } else if (fragment instanceof PhoneNumberPickerFragment) {
            mSearchFragment = (PhoneNumberPickerFragment) fragment;
            mSearchFragment.setOnPhoneNumberPickerActionListener(mPhoneNumberPickerActionListener);
            mSearchFragment.setQuickContactEnabled(true);
            mSearchFragment.setDarkTheme(true);
            mSearchFragment.setPhotoPosition(ContactListItemView.PhotoPosition.LEFT);
            if (mContactListFilterController != null
                    && mContactListFilterController.getFilter() != null) {
                mSearchFragment.setFilter(mContactListFilterController.getFilter());
            }
            // Here we assume that we're not on the search mode, so let's hide the fragment.
            //
            // We get here either when the fragment is created (normal case), or after configuration
            // changes.  In the former case, we're not in search mode because we can only
            // enter search mode if the fragment is created.  (see enterSearchUi())
            // In the latter case we're not in search mode either because we don't retain
            // mInSearchUi -- ideally we should but at this point it's not supported.
            mSearchFragment.setUserVisibleHint(false);
            // After configuration changes fragments will forget their "hidden" state, so make
            // sure to hide it.
            if (!mSearchFragment.isHidden()) {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.hide(mSearchFragment);
                transaction.commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onPause() {
        if(CDBG) log("onPause enter");
        super.onPause();
        /*Modifyed for switchuitwo-455 begin*/
        if(popupMenu != null) {
        	popupMenu.dismiss();
        }
        /*Modifyed for switchuitwo-455 end*/
        if(DBG) log("onPause enter, mLastManuallySelectedFragment is " + mLastManuallySelectedFragment);
        if(DBG) log("onPause enter, current selected fragment index is " + mViewPager.getCurrentItem());

        mPrefs.edit().putInt(PREF_LAST_MANUALLY_SELECTED_TAB, mLastManuallySelectedFragment)
                .apply();

        // The activity is leaving, reset the variable.
        isHandlingNewIntent = false; // MOTO A wcs077 13280
        if(CDBG) log("onPause exit");
    }

    private void fixIntent(Intent intent) {
        // This should be cleaned up: the call key used to send an Intent
        // that just said to go to the recent calls list.  It now sends this
        // abstract action, but this class hasn't been rewritten to deal with it.
        if (Intent.ACTION_CALL_BUTTON.equals(intent.getAction())) {
            intent.setDataAndType(Calls.CONTENT_URI, Calls.CONTENT_TYPE);
            intent.putExtra("call_key", true);
            setIntent(intent);
        }
    }

    private void setupDialer() {
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(R.string.dialerLabel);
        tab.setTabListener(mTabListener);
        //tab.setIcon(R.drawable.ic_tab_dialer);
        tab.setText(R.string.dialerLabel);
        getActionBar().addTab(tab);
    }

    private void setupCallLog() {
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(R.string.recentCallsLabel);
        //tab.setIcon(R.drawable.ic_tab_recent);
        tab.setText(R.string.recentCallsLabel);
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
    }

    private void setupFavorites() {
        final Tab tab = getActionBar().newTab();
        //MOT Dialer Code Start - IKHSS6UPGR-7991
        if (isContactsSearchEnabled) {
            tab.setContentDescription(R.string.strequentList); //MOT Calling Code - IKHSS6-3990
            //tab.setIcon(R.drawable.ic_tab_all);
            tab.setText(R.string.strequentList); //MOT Calling Code - IKHSS6-3990
        } else {
            tab.setContentDescription(R.string.people);
            tab.setText(R.string.people);
        }
        //MOT Dialer Code End - IKHSS6UPGR-7991
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
    }

    /**
     * Returns true if the intent is due to hitting the green send key while in a call.
     *
     * @param intent the intent that launched this activity
     * @param recentCallsRequest true if the intent is requesting to view recent calls
     * @return true if the intent is due to hitting the green send key while in a call
     */
    private boolean isSendKeyWhileInCall(final Intent intent,
            final boolean recentCallsRequest) {
        // If there is a call in progress go to the call screen
        if (recentCallsRequest) {
            final boolean callKey = intent.getBooleanExtra("call_key", false);

            try {
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                if (callKey && phone != null && phone.showCallScreen()) {
                    return true;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to handle send while in call", e);
            }
        }

        return false;
    }

    /**
     * Sets the current tab based on the intent's request type
     *
     * @param intent Intent that contains information about which tab should be selected
     */
    private void setCurrentTab(Intent intent) {
        // If we got here by hitting send and we're in call forward along to the in-call activity
        final boolean recentCallsRequest = Calls.CONTENT_TYPE.equals(intent.getType());
        if (isSendKeyWhileInCall(intent, recentCallsRequest)) {
            finish();
            return;
        }

        Log.w(TAG, "lets see whats the cur tab !!! ");

        // Remember the old manually selected tab index so that it can be restored if it is
        // overwritten by one of the programmatic tab selections
        final int savedTabIndex = mLastManuallySelectedFragment;

        final int tabIndex;

        String componentName = intent.getComponent().getClassName();
        if (getClass().getName().equals(componentName)) {
            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
                // launched from history (long-press home) --> nothing to change
                if(DBG) log("launched from history (long-press home) --> nothing to change, return");
                return;
            }
            //MOT Calling Code - IKMAIN-1221(IKSHADOW-6342)
            // CALL_BUTTON intent will launch dialer tab intead of RecentCall tab
            else if (Intent.ACTION_CALL_BUTTON.equals(intent.getAction())) {
                tabIndex = TAB_INDEX_DIALER;
            //MOT Calling Code-End - IKMAIN-1221(IKSHADOW-6342)
            } else if (recentCallsRequest) {
                tabIndex = TAB_INDEX_CALL_LOG;
            } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                tabIndex = TAB_INDEX_DIALER;
            // [ MOTO:Begin C e51185 14922
            // BZ 14922, added conditional check for ACTION_DIAL which Intent was being thrown
            // with data from Home screen, upon pressing ALT+NUM keys
            } else if (Intent.ACTION_DIAL.equals(intent.getAction()) &&
                        (intent.getData() != null) &&
                            ("tel".equals(intent.getData().getScheme())) ) {
                tabIndex = TAB_INDEX_DIALER;
            // ] MOTO:End C
            } else if (isDialIntent(intent)) {
                    // The dialer was explicitly requested
                tabIndex = TAB_INDEX_DIALER;
            } else {
                // Launch Dialer as default for China products
                // tabIndex = mLastManuallySelectedFragment;
                tabIndex = TAB_INDEX_DIALER;
            }
        } else if (CALLLOG_SHORTCUT_COMPONENT.equals(componentName)){
            tabIndex = TAB_INDEX_CALL_LOG;
        } else {
            tabIndex = mLastManuallySelectedFragment;
        }
        
        //comment google code and use moto enhanced rules as above.
        /*if (DialpadFragment.phoneIsInUse() || isDialIntent(intent)) {
            tabIndex = TAB_INDEX_DIALER;
        } else if (recentCallsRequest) {
            tabIndex = TAB_INDEX_CALL_LOG;
        } else {
            tabIndex = mLastManuallySelectedFragment;
        }*/
        final int previousItemIndex = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(tabIndex, false /* smoothScroll */);
        if (previousItemIndex != tabIndex) {
            sendFragmentVisibilityChange(previousItemIndex, false); //TBD, already done in setCurrentItem via mPageChangeListener, seems not need, google bug?
        }
        mPageChangeListener.setCurrentPosition(tabIndex);
        sendFragmentVisibilityChange(tabIndex, true);

        // Restore to the previous manual selection
        mLastManuallySelectedFragment = savedTabIndex;
        if(DBG) log("setCurrentTab, current focus fragment is set as " + mViewPager.getCurrentItem());
        if(DBG) log("setCurrentTab, mLastManuallySelectedFragment is " + mLastManuallySelectedFragment);
//Google 4.0.4 Start
//        mDuringSwipe = false;
//        mUserTabClick = false;
//Google 4.0.4 End
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        setIntent(newIntent);
        fixIntent(newIntent);
        setCurrentTab(newIntent);
        final String action = newIntent.getAction();
        if (UI.FILTER_CONTACTS_ACTION.equals(action)) {
            setupFilterText(newIntent);
        }
        if (mInSearchUi || (mSearchFragment != null && mSearchFragment.isVisible())) {
            exitSearchUi();
        }

        if (mViewPager.getCurrentItem() == TAB_INDEX_DIALER) {
            if (mDialpadFragment != null) {
                mDialpadFragment.configureScreenFromIntent(newIntent);
            } else {
                Log.e(TAG, "DialpadFragment isn't ready yet when the tab is already selected.");
            }
        }
        invalidateOptionsMenu();

        // The activity was asked to handle a new intent
        isHandlingNewIntent = true; // MOTO A wcs077 13280
    }

    /** Returns true if the given intent contains a phone number to populate the dialer with */
    private boolean isDialIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) /*|| ACTION_TOUCH_DIALER.equals(action)*/) { //comment by MOTO
            return true;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && "tel".equals(data.getScheme())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the filter text stored in {@link #setupFilterText(Intent)}.
     * This text originally came from a FILTER_CONTACTS_ACTION intent received
     * by this activity. The stored text will then be cleared after after this
     * method returns.
     *
     * @return The stored filter text
     */
    public String getAndClearFilterText() {
        String filterText = mFilterText;
        mFilterText = null;
        return filterText;
    }

    /**
     * Stores the filter text associated with a FILTER_CONTACTS_ACTION intent.
     * This is so child activities can check if they are supposed to display a filter.
     *
     * @param intent The intent received in {@link #onNewIntent(Intent)}
     */
    private void setupFilterText(Intent intent) {
        // If the intent was relaunched from history, don't apply the filter text.
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }
        String filter = intent.getStringExtra(UI.FILTER_TEXT_EXTRA_KEY);
        if (filter != null && filter.length() > 0) {
            mFilterText = filter;
        }
    }

    @Override
    public void onBackPressed() {
        if (mInSearchUi) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi();
        } else if (isTaskRoot()) {
            // Instead of stopping, simply push this to the back of the stack.
            // This is only done when running at the top of the stack;
            // otherwise, we have been launched by someone else so need to
            // allow the user to go back to the caller.
            moveTaskToBack(false);
        } else {
            super.onBackPressed();
        }
    }

    private DialpadFragment.Listener mDialpadListener = new DialpadFragment.Listener() {
        @Override
        public void onSearchButtonPressed() {
            enterSearchUi();
        }
    };

    private PhoneFavoriteFragment.Listener mPhoneFavoriteListener =
            new PhoneFavoriteFragment.Listener() {
        @Override
        public void onContactSelected(Uri contactUri) {
            PhoneNumberInteraction.startInteractionForPhoneCall(
                    DialtactsActivity.this, contactUri,
                    CALL_ORIGIN_DIALTACTS, MEDialer.DialFrom.FAVORITES); //MOTO Dialer Code IKHSS6-723
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dialtacts_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem addContactActionItem = menu.findItem(R.id.add_contact_tab);
        //final MenuItem fakeMenuItem = menu.findItem(R.id.fake_menu_item); //Google 4.0.4
        Tab tab = getActionBar().getSelectedTab();
        if (isContactsSearchEnabled) searchMenuItem.setTitle(R.string.people); //MOT Dialer Code - IKHSS6UPGR-7991

        if (mInSearchUi) {
            searchMenuItem.setVisible(false);
            addContactActionItem.setVisible(false);
            if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
                filterOptionMenuItem.setVisible(true);
                filterOptionMenuItem.setOnMenuItemClickListener(
                        mFilterOptionsMenuItemClickListener);
                addContactOptionMenuItem.setVisible(true);
                addContactOptionMenuItem.setIntent(
                        new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
            } else {
                // Filter option menu should be not be shown as a overflow menu.
                filterOptionMenuItem.setVisible(false);
                addContactOptionMenuItem.setVisible(false);
            }
            callSettingsMenuItem.setVisible(false);
            //fakeMenuItem.setVisible(false); //Google 4.0.4
        } else {
            final boolean showCallSettingsMenu;
            addContactOptionMenuItem.setVisible(false);
            if (tab != null && tab.getPosition() == TAB_INDEX_DIALER) {
//Google 4.0.4 Start
//                if (DEBUG) {
//                    Log.d(TAG, "onPrepareOptionsMenu(dialer). swipe: " + mDuringSwipe
//                            + ", user tab click: " + mUserTabClick);
//                }
//                if (mDuringSwipe || mUserTabClick) {
//                    // During horizontal movement, we just show real ActionBar menu items.
//                    searchMenuItem.setVisible(true);
//                    searchMenuItem.setOnMenuItemClickListener(mSearchMenuItemClickListener);
//                    showCallSettingsMenu = true;
//
//                    fakeMenuItem.setVisible(ViewConfiguration.get(this).hasPermanentMenuKey());
//                } else {
//Google 4.0.4 End
                    searchMenuItem.setVisible(false);
                    addContactActionItem.setVisible(false);
                    // When permanent menu key is _not_ available, the call settings menu should be
                    // available via DialpadFragment.
                //showCallSettingsMenu = ViewConfiguration.get(this).hasPermanentMenuKey();
                //showCallSettingsMenu = true;//MOT Dialer Code, show overflow menu in action bar when no PermanentMenuKey.
                if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
                    showCallSettingsMenu = false;//ChinaDev
                } else {
                    showCallSettingsMenu = true;
                }
            } else {
                searchMenuItem.setVisible(true);
                searchMenuItem.setOnMenuItemClickListener(mSearchMenuItemClickListener);
                addContactActionItem.setVisible(true);
                addContactActionItem.setIntent(
                        new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
                showCallSettingsMenu = true;
                //fakeMenuItem.setVisible(ViewConfiguration.get(this).hasPermanentMenuKey());//Google 4.0.4
            }

            //Mot Dialer Code Start - IKHSS6UPGR-6255
            //when contacts search is enabled, we do not show filter option in fav tab, as we no longer show
            //contacts list in fav tab
            if (tab != null && tab.getPosition() == TAB_INDEX_FAVORITES && !isContactsSearchEnabled) {
                filterOptionMenuItem.setVisible(true);
                filterOptionMenuItem.setOnMenuItemClickListener(
                        mFilterOptionsMenuItemClickListener);
            } else {
                filterOptionMenuItem.setVisible(false);
            }

            if (showCallSettingsMenu) {
                callSettingsMenuItem.setVisible(true);
                callSettingsMenuItem.setIntent(DialtactsActivity.getCallSettingsIntent());
            } else {
                callSettingsMenuItem.setVisible(false);
            }
        }

        return true;
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        //MOT Dialer Code Start - IKHSS6UPGR-7991
        if (isContactsSearchEnabled) {
            enterSearchUi();
        } else {
            if (mSearchFragment != null && mSearchFragment.isAdded() && !globalSearch) {
                if (mInSearchUi) {
                    if (mSearchView.hasFocus()) {
                        showInputMethod(mSearchView.findFocus());
                    } else {
                        mSearchView.requestFocus();
                    }
                } else {
                    enterSearchUi();
                }
            } else {
                super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
            }
        }
        //MOT Dialer Code End - IKHSS6UPGR-7991
    }

    /**
     * Hides every tab and shows search UI for phone lookup.
     */
    private void enterSearchUi() {
        //MOT Dialer Code Start -IKHSS6UPGR-7991
        if (isContactsSearchEnabled) {
            //launch contacts list search.
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE );
            intent.putExtra("dialersearchMode", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
            startActivity(intent);
        } else {
            if (mSearchFragment == null) {
                // We add the search fragment dynamically in the first onLayoutChange() and
                // mSearchFragment is set sometime later when the fragment transaction is actually
                // executed, which means there's a window when users are able to hit the (physical)
                // search key but mSearchFragment is still null.
                // It's quite hard to handle this case right, so let's just ignore the search key
                // in this case.  Users can just hit it again and it will work this time.
                return;
            }
            if (mSearchView == null) {
                prepareSearchView();
            }

            final ActionBar actionBar = getActionBar();

            final Tab tab = actionBar.getSelectedTab();

            // User can search during the call, but we don't want to remember the status.
            if (tab != null && !DialpadFragment.phoneIsInUse()) {
                mLastManuallySelectedFragment = tab.getPosition();
            }

            mSearchView.setQuery(null, true);

            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

            sendFragmentVisibilityChange(mViewPager.getCurrentItem(), false);

            // Show the search fragment and hide everything else.
            mSearchFragment.setUserVisibleHint(true);
            /*Added for switchuitwo-372 begin*/
            mSearchFragment.hideAccountFilterHeader();
            /*Added for switchuitwo-372 end*/
            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.show(mSearchFragment);
            transaction.commitAllowingStateLoss();
            mViewPager.setVisibility(View.GONE);

            // We need to call this and onActionViewCollapsed() manually, since we are using a custom
            // layout instead of asking the search menu item to take care of SearchView.
            mSearchView.onActionViewExpanded();
            mInSearchUi = true;
        }
        //MOT Dialer Code End -IKHSS6UPGR-6255
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Goes back to usual Phone UI with tags. Previously selected Tag and associated Fragment
     * should be automatically focused again.
     */
    private void exitSearchUi() {
        final ActionBar actionBar = getActionBar();

        // Hide the search fragment, if exists.
        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);
            /*Added for switchuitwo-372 begin*/
            mSearchFragment.showAccountFilterHeader();
            /*Added for switchuitwo-372 end*/
            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.hide(mSearchFragment);
            transaction.commitAllowingStateLoss();
        }

        // We want to hide SearchView and show Tabs. Also focus on previously selected one.
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        sendFragmentVisibilityChange(mViewPager.getCurrentItem(), true);

        // Before exiting the search screen, reset swipe state.
//Google 4.0.4 Start
//        mDuringSwipe = false;
//        mUserTabClick = false;
//Google 4.0.4 End

        mViewPager.setVisibility(View.VISIBLE);

        hideInputMethod(getCurrentFocus());

        // Request to update option menu.
        invalidateOptionsMenu();

        // See comments in onActionViewExpanded()
        mSearchView.onActionViewCollapsed();
        mInSearchUi = false;
    }

    public Fragment getFragmentAt(int position) { // MOTO Dialer Code IKHSS7-2637
        switch (position) {
            case TAB_INDEX_DIALER:
                return mDialpadFragment;
            case TAB_INDEX_CALL_LOG:
                return mCallLogFragment;
            case TAB_INDEX_FAVORITES:
                return mPhoneFavoriteFragment;
            default:
                throw new IllegalStateException("Unknown fragment index: " + position);
        }
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        if(DBG) log("sendFragmentVisibilityChange: position = " + position + " visibility = " + visibility); //TBD, test
        final Fragment fragment = getFragmentAt(position);
        if (fragment instanceof ViewPagerVisibilityListener) {
            ((ViewPagerVisibilityListener) fragment).onVisibilityChanged(visibility);
        }
    }

    /** Returns an Intent to launch Call Settings screen */
    public static Intent getCallSettingsIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        boolean isDualMode = PhoneModeManager.isDmds();
        if (isDualMode) {
            intent.setClassName(PHONE_PACKAGE, CALL_SETTINGS_CLASS_NAME_DUAL_MODE);
        } else {
            intent.setClassName(PHONE_PACKAGE, CALL_SETTINGS_CLASS_NAME);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Motorola, pxkq63, 2012/1/3, IKHSS7-2761, FID 35964 Call Setting for kor
        if(ContactsApplication.CALL_SETTING_FOR_KOR == true){
            intent.setClassName("com.motorola.callFeaturesSetting", "com.motorola.callFeaturesSetting.CallFeaturesSetting");
        }
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
                AccountFilterUtil.handleAccountFilterResult(
                        mContactListFilterController, resultCode, data);
            }
            break;
        }
    }

    // MOTO Dialer Code Start
    // MOTO A bkdp84 bug 21155
    /*private boolean okToShowInCallScreen(Intent intent) {
          return   (
                       Intent.ACTION_MAIN.equals(intent.getAction()) ||
                       (           Intent.ACTION_DIAL.equals(intent.getAction()) &&
                                    intent.getData() == null
                       )
                ) &&
                       !DialpadFragment.isAddCallMode(intent);
    }*/

    @Override
    public void onResume() {
        if(CDBG) log("OnResume enter");
        super.onResume();
        /*if(DBG) log("onResume enter, mLastManuallySelectedFragment is " + mLastManuallySelectedFragment);
        if(DBG) log("onResume enter, current selected fragment index is " + mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == TAB_INDEX_DIALER && okToShowInCallScreen(getIntent())) {
            TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
            if (tm != null && tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) { // MOTO C bkdp84 bug 21155
                if(DBG) log("Phone State not idle");
                // [ MOTO:Begin C wcs077 13280
                // If there is an ongoing call, the activity is in background and the user re-launches the activity,
                // the InCall Screen must be displayed before exiting, but if the resume is because of the back key on
                // InCall Screen, the InCall Screen should not be called again
                if (isHandlingNewIntent == true){
                    showInCallScreen();
                    finish();
                }
                // ] MOTO: End C
            } else {
                boolean isDmds = PhoneModeManager.isDmds();
                if (isDmds) {
                    TelephonyManager tm2 = TelephonyManager.getDefault(false);
                    if (tm2 != null && tm2.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                        if(DBG) log("Phone2 State not idle");
                        if (isHandlingNewIntent == true){
                            showSecInCallScreen();
                            finish();
                        }
                    }
                }
            }
        }*/
        if(CDBG) log("OnResume Leave");
    }
    // ] MOTO: End A

    //Mot Calling code - IKSTABLEFIVE-4820
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(getClass().getName());
        event.setPackageName(getPackageName());

        WindowManager.LayoutParams params = getWindow().getAttributes();
        boolean isFullScreen = (params.width == WindowManager.LayoutParams.MATCH_PARENT) &&
            (params.height == WindowManager.LayoutParams.MATCH_PARENT);
        event.setFullScreen(isFullScreen);

        CharSequence title;
        switch (mViewPager.getCurrentItem()) {
            case TAB_INDEX_FAVORITES:
                title = getString(R.string.people); //MOT Calling Code - IKHSS6-3990
                break;
            case TAB_INDEX_CALL_LOG:
                title = getString(R.string.recentCallsLabel);
                break;
            case TAB_INDEX_DIALER:
            default:
                title = getString(R.string.dialerLabel);
                break;
        }

        if(mInSearchUi == true)
           title = getString(R.string.hint_findContacts); //TBD, need define what should be read out in search UI

        if (!TextUtils.isEmpty(title)) {
            if(DBG) log("dispatchPopulateAccessibilityEvent add text " + title);
            event.getText().add(title);
        }
        return true;
    }
    //Mot Calling code End - IKSTABLEFIVE-4820

    // [ MOTO: Begin: Performance fix.
    /**
     * Dismisses the dialer screen.
     *
     * We don't want the dialer activity get destroyed and then to be re-created from scratch.
     * Instead, we just move it to the back of the activity stack.
     *
     * (Since the Phone application itself is never killed, this means
     * that we would like to keep a single dialer activity instance around for the
     * entire uptime of the device.  This will improve UI responsiveness.)
     */
    /* MOTO D till we figure out why this optimization causes
     * bug 13058 ST:EX:RND:Can't send call from via the number of the message
     * MOTO jxr467 : bug #13050 is fixed by delaying finish() call in TwelveKeyDialer.placeCall() by 1.5 sec
     */
    @Override
    public void finish() { //TBD, may need rebalance with new fragment arch for KPI
        moveTaskToBack(false);
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle BACK
        if(keyCode == KeyEvent.KEYCODE_BACK && isTaskRoot()) {
            // Instead of stopping, simply push this to the back of the stack.
            // This is only done when running at the top of the stack;
            // otherwise, we have been launched by someone else so need to
            // allow the user to go back to the caller.
            moveTaskToBack(false);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }*/ //TBD, seems can be covered by onBackPressed.

    /*static void showInCallScreen() {
        // MOT Calling Code - IKMAIN-22303
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (telephony != null && !telephony.isIdle()) {
                telephony.showCallScreen();
                return;
            }
        // END MOT Calling Code - IKMAIN-22303
        } catch (RemoteException ex) {
            // bringing the inCallScreen failed; continue
        }
    }

    static void showSecInCallScreen() {
        try {
            ITelephony telephony2 = ITelephony.Stub.asInterface(ServiceManager.getService("phone2"));
            if (telephony2 != null && !telephony2.isIdle()) {
                telephony2.showCallScreen();
                return;
            }
        } catch (RemoteException ex) {
            // bringing the inCallScreen failed; continue
        }
    }*/

    private class HdmiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DBG) log("dialer hdmi onReceive");
            String action = intent.getAction();
            if (MIRROR_PUBLIC_STATE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    int conn = extras.getInt(MIRROR_STATE, -1);
                    if (conn == 1) { // Entered on mirror state. Turn on the sensor.
                        if(DBG) log("dialer hdmi Enabled");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                    else if (conn == 0) { // Left the mirror state. Turn off the sensor.
                        if(DBG) log("dialer hdmi Disabled");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                    }
                } else {
                    if(DBG) log("dialer Intent MIRROR_PUBLIC_STATE has no extra MIRROR_STATE: does nothing.");
                }
            }
        }
    }

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
    //MOTO Dialer Code End
}
