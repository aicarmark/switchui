/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.calllog;

import com.android.common.io.MoreCloseables;
import com.android.contacts.ContactsUtils;
import com.android.contacts.LocationServiceManager;
import com.android.contacts.R;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.activities.DialtactsActivity.ViewPagerVisibilityListener;
import com.android.contacts.activities.CallLogActivity.ViewVisibilityListener;
import com.android.contacts.spd.SpeedDialHelper;
import com.android.contacts.spd.SpeedDialPicker;
import com.android.contacts.spd.Utils;
import com.android.contacts.util.EmptyLoader;
import com.android.contacts.util.ThemeUtils;
import com.android.contacts.util.UriUtils;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;
import com.motorola.contacts.util.MEDialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast; //Mot calling code -IKPIM-431
/*Added for switchuitwo-344 begin*/
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
/*Added for switchuitwo-344 end*/
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.provider.Settings;
import com.android.contacts.RomUtility;

/**
 * Displays a list of call log entries.
 */
public class CallLogFragment extends ListFragment implements ViewPagerVisibilityListener,
        CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher, ViewVisibilityListener, OnScrollListener , View.OnCreateContextMenuListener {
        //MOTO Dialer Code - IKHSS6-583 IKHSS6UPGR-4081
    private static final String TAG = "CallLogFragment";

    /**
     * ID of the empty loader to defer other fragments.
     */
    private static final int EMPTY_LOADER_ID = 0;

    private CallLogAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    //private boolean mScrollToTop; // Motorola, July-04-2011, IKMAIN-21909

    private boolean mShowOptionsMenu;
    /** Whether there is at least one voicemail source installed. */
    private boolean mVoicemailSourcesAvailable = false;
    /** Whether we are currently filtering over voicemail. */
    private boolean mShowingVoicemailOnly = false;

    private VoicemailStatusHelper mVoicemailStatusHelper;
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;
    private KeyguardManager mKeyguardManager;

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;
    private boolean mVoicemailStatusFetched;
    //MOTO Dialer Code Start
    private ContactsObserver mContactObserver;
    boolean fragmentVisibilty = false;
    private String mTaskifyContextMenuTitle = null;
    private AlertDialog mTaskFailedDialog;
    private SelectActionModeCallback mSelectCallback;

    public static final String EXTRA_CALL_ID = "CALLLOG_LONG_ID";
    private static final String ACTION_NEW_TASKS = "com.motorola.blur.tasks.ACTION_NEW_TASKIFY";
    private static final int REQUEST_NEW_TASKIFY = 1; // Mot calling code-IKPIM-431
    public static final int RESULT_OK           = -1;

    private static final int CONTEXT_MENU_ITEM_DELETE = 1;
    private static final int CONTEXT_MENU_CALL_CONTACT = 2;
    private static final int MENU_ITEM_ADD_TO_SPD = 3;
    private static final int MENU_ITEM_ADD_TO_CONTACTS = 4;
    private static final int MENU_ITEM_ADD_CALLBACK_REMINDER_TO_TASKS = 5;
    //Mot add end
    private final static String EXTRA_TASKIFY_TITLE = "title";
    private final static String EXTRA_TAKIFY_URI = "uri";
    private final static String EXTRA_TAKIFY_RESULT = "create_taskify_result";
    private static final String TASKIFY_NAME = "com.motorola.tasks"; //MOT Dialer Code - IKHSS6-8475
    private boolean isTaskifyAvailable;
    private boolean isContextMenuOn;

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    //MOTO Dialer Code - IKHSS6-13862
    private final static String ADD_CONTACT_DIALOG="com.motorola.contacts.ACTION_ADD_CONTACT_DIALOG";
    //MOTO Dialer Code End
    private Spinner mfilterSpinner;
    private TextView mEmptyLabel;
    private SharedPreferences mPrefs;
    private static final String PREF_CALL_TYPE = "com.android.contacts.calllog.CallLogFragment.CALL_TYPE";
    private static final String PREF_NETWORK_TYPE = "com.android.contacts.calllog.CallLogFragment.NETWORK_TYPE";
    private LocationServiceManager mLocationServiceManager = null;//ChinaDev
    private Spinner mNetworkSpinner;
    private boolean isTwoCardsEnable;

    @Override
    public void onCreate(Bundle state) {
        if (DBG) {
            log("enter onCreate");
        }
        super.onCreate(state);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity().getContentResolver(), this);
        mKeyguardManager =
                (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        setHasOptionsMenu(true);

        //Moto Dialer Code Start
        mContactObserver = new ContactsObserver();
        getActivity().getContentResolver().registerContentObserver(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, true, mContactObserver);
        isTaskifyAvailable = isPkgInstalled(getActivity(), TASKIFY_NAME);
        isContextMenuOn = getActivity().getResources().getBoolean(R.bool.contextmenu_on);
        //Moto Dialer Code End
        if (DBG) {
            log("exit onCreate");
        }
        if (mLocationServiceManager == null) {
            mLocationServiceManager = new LocationServiceManager(getActivity().getApplicationContext(), null);
        }
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing() || cursor == null) {
            return;
        }
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        if(CDBG) Log.d(TAG, "quer complete and changeCursor");
        // This will update the state of the "Clear call log" menu item.
        getActivity().invalidateOptionsMenu();
        // MOT Calling Code - IKSTABLEFOURV-220
        /*if (mScrollToTop) {
            final ListView listView = getListView();
            if (listView.getFirstVisiblePosition() > 5) {
                listView.setSelection(5);
            }
            listView.smoothScrollToPosition(0);
            if(VDBG) log("smooth Scroll To Position 0");
            mScrollToTop = false;
        }*/
        //commented google code due 1. moto do not return to RC after call end  2. enter call detail UI then back will cause jump
        // MOT Calling Code - IKSTABLEFOURV-220 - End
        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    /**
     * Called by {@link CallLogQueryHandler} after a successful query to voicemail status provider.
     */
    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        updateVoicemailStatusMessage(statusCursor);

        int activeSources = mVoicemailStatusHelper.getNumberActivityVoicemailSources(statusCursor);
        setVoicemailSourcesAvailable(activeSources != 0);
        MoreCloseables.closeQuietly(statusCursor);
        mVoicemailStatusFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    /** Sets whether there are any voicemail sources available in the platform. */
    private void setVoicemailSourcesAvailable(boolean voicemailSourcesAvailable) {
        if (mVoicemailSourcesAvailable == voicemailSourcesAvailable) return;
        mVoicemailSourcesAvailable = voicemailSourcesAvailable;

        Activity activity = getActivity();
        if (activity != null) {
            // This is so that the options menu content is updated.
            activity.invalidateOptionsMenu();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.call_log_fragment, container, false);
        mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
        mStatusMessageView = view.findViewById(R.id.voicemail_status);
        mStatusMessageText = (TextView) view.findViewById(R.id.voicemail_status_message);
        mStatusMessageAction = (TextView) view.findViewById(R.id.voicemail_status_action);

        mfilterSpinner = (Spinner) view.findViewById(R.id.recent_calls_filter_spinner);
        mfilterSpinner.setVisibility(View.VISIBLE);
        mfilterSpinner.setOnItemSelectedListener(mSpinnerListener);
        mEmptyLabel = (TextView) view.findViewById(android.R.id.empty);

        mNetworkSpinner = (Spinner) view.findViewById(R.id.recent_network_filter_spinner);
        mNetworkSpinner.setOnItemSelectedListener(mNetworkSpinnerListener);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());
        mAdapter = new CallLogAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso));
        setListAdapter(mAdapter);
        //getListView().setItemsCanFocus(true);
        ListView listView = getListView();
        //check if use context menu
        if (isContextMenuOn){
          listView.setOnCreateContextMenuListener(this);
        } else {
          mSelectCallback = new SelectActionModeCallback();
          listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
          listView.setMultiChoiceModeListener(mSelectCallback);
        }
        //listView.setOnKeyListener(mThreadListKeyListener);

        getListView().setOnScrollListener(this);//MOT Dialer Code IKHSS6UPGR-4081
    }

    @Override
    public void onStart() {
        if(VDBG) log("onStart enter");
        //mScrollToTop = true;  // MOT Calling Code - IKSTABLEFOURV-220

        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;
        super.onStart();
        if(VDBG) log("onStart exit");
    }

    @Override
    public void onResume() {
        if(VDBG) log("onResume enter");
        super.onResume();
        if (mLocationServiceManager != null) {
            mLocationServiceManager.checkService(true);
        }
        // MOT Calling code IKHSS7-2977
        if(fragmentVisibilty) {
           refreshData(); //MOTO Dialer Code - IKHSS6-583
        }
        // MOT Calling code IKHSS7-2977 -End
        mAdapter.mIsCallBeenPlaced = false; // MOT Calling code - IKSTABLETWO-6716
        isTwoCardsEnable = ContactsUtils.haveTwoCards(getActivity().getApplicationContext());
        if (!isTwoCardsEnable) {
            mNetworkSpinner.setVisibility(View.GONE);
        } else {
            mNetworkSpinner.setVisibility(View.VISIBLE);
        }
        loadSpinnerFilter();
        if(VDBG) log("onResume exit");
    }

    private void updateVoicemailStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            mStatusMessageView.setVisibility(View.GONE);
        } else {
            mStatusMessageView.setVisibility(View.VISIBLE);
            // TODO: Change the code to show all messages. For now just pick the first message.
            final StatusMessage message = messages.get(0);
            if (message.showInCallLog()) {
                mStatusMessageText.setText(message.callLogMessageId);
            }
            if (message.actionMessageId != -1) {
                mStatusMessageAction.setText(message.actionMessageId);
            }
            if (message.actionUri != null) {
                mStatusMessageAction.setVisibility(View.VISIBLE);
                mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().startActivity(
                                new Intent(Intent.ACTION_VIEW, message.actionUri));
                    }
                });
            } else {
                mStatusMessageAction.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        if(VDBG) log("onPause");
        /*Added for switchuitwo-344 begin*/
        if (mfilterSpinner != null) {
			try {
				Class<Spinner> c = (Class<Spinner>) mfilterSpinner.getClass();
				Method onDetachedFromWindow = null;
				onDetachedFromWindow = c.getDeclaredMethod("onDetachedFromWindow",(Class[]) null);
				onDetachedFromWindow.setAccessible(true);
				onDetachedFromWindow.invoke(mfilterSpinner, (Object[]) null);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        /*Added for switchuitwo-344 end*/
        finishMode();
        super.onPause();
        // Kill the requests thread
        mAdapter.stopRequestProcessing();
    }

    @Override
    public void onStop() {
        if(VDBG) log("onStop");
        super.onStop();
        if(fragmentVisibilty) { // MOT Calling code IKHSS7-2977
            updateOnExit();
        }
    }

    @Override
    public void onDestroy() {
        if(VDBG) log("onDestroy");
        super.onDestroy();
        if (mLocationServiceManager != null) {
            mLocationServiceManager = null;
        }
        mAdapter.stopRequestProcessing();
        mAdapter.clearAllCache();//MOTO Dialer Code
        mAdapter.changeCursor(null);
        getActivity().getContentResolver().unregisterContentObserver(mContactObserver);//Moto Dialer Code
    }

    @Override
    public void fetchCalls() {
        int networkType = getNetworkType ();
        if (networkType < CallLogViewTypeHelper.NETWORK_TYPE_ALL 
                || networkType > CallLogViewTypeHelper.NETWORK_TYPE_GSM) {
            Log.e(TAG, "invalid  filter network type " + networkType);
            return;
        }

        if (mShowingVoicemailOnly) {
            mCallLogQueryHandler.fetchVoicemailOnly(networkType);
        } else {
            int callType = getCallType();
            if (callType < CallLogViewTypeHelper.VIEW_OPTION_ALL 
                    || callType > CallLogViewTypeHelper.VIEW_OPTION_VOICEMAIL) {
                Log.e(TAG, "invalid  filter call type " + callType);
                return;
            } else {
                mCallLogQueryHandler.fetchFilterCalls(callType, networkType);
            }
        }
    }

    public void startCallsQuery() {
        mAdapter.setLoading(true);
        if (mShowingVoicemailOnly) {
            mShowingVoicemailOnly = false;
            getActivity().invalidateOptionsMenu();
        }
        fetchCalls();
    }

    private void startVoicemailStatusQuery() {
        mCallLogQueryHandler.fetchVoicemailStatus();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowOptionsMenu) {
            inflater.inflate(R.menu.call_log_options, menu);
        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        if (VDBG) {
            log("enter onCreateContextMenu");
        }
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) menuInfoIn;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfoIn", e);
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(menuInfo.position);

        String number = null;
        String name = null;
        String taskifyTitle = null;
        String formatNumber = null;
        Uri lookupUri = null;
        boolean contactInfoPresent = false;
        if (cursor != null) {
            number = cursor.getString(CallLogQuery.NUMBER);
            name = cursor.getString(CallLogQuery.CACHED_NAME);
            formatNumber = cursor.getString(CallLogQuery.CACHED_FORMATTED_NUMBER);
            lookupUri = UriUtils.parseUriOrNull(cursor.getString(CallLogQuery.CACHED_LOOKUP_URI));
            contactInfoPresent = (UriUtils.parseUriOrNull(cursor.getString(CallLogQuery.CACHED_LOOKUP_URI)) != null);
        } else {
            Log.e(TAG, "Bad Case. No Cursor,We better return from here");
            return;
        }

        boolean isVoicemail = mAdapter.getPhoneNumberHelper().isVoicemailNumber(number);
        boolean isCanBeCalled = mAdapter.getPhoneNumberHelper().canPlaceCallsTo(number);

        if (contactInfoPresent) {
            menu.setHeaderTitle(name);
            mTaskifyContextMenuTitle = name;
            taskifyTitle = name;
        } else {
            menu.setHeaderTitle(TextUtils.isEmpty(formatNumber)?number:formatNumber);
            mTaskifyContextMenuTitle = (TextUtils.isEmpty(formatNumber)?number:formatNumber);
            taskifyTitle = number;
        }

        // add to contact
        if (!contactInfoPresent && !isVoicemail && isCanBeCalled) {
            menu.add(0, MENU_ITEM_ADD_TO_CONTACTS, 0, R.string.recentCalls_addToContact);
        }

        // view contact
        if (contactInfoPresent) {
            Intent intent = new Intent(Intent.ACTION_VIEW,lookupUri);
            menu.add(0, 0, 0, R.string.menu_viewContact).setIntent(intent);
        }

        // Call
        if (isCanBeCalled) {
            Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", number, null));
            menu.add(0, CONTEXT_MENU_CALL_CONTACT, 0,
                    getResources().getString(
                    R.string.recentCalls_callNumber,
                    PhoneNumberUtils.formatNumber(number))).setIntent(intent);
        }

        // edit before call
        if (isCanBeCalled) {
            menu.add(0, R.string.recentCalls_editNumberBeforeCall, 0, R.string.recentCalls_editNumberBeforeCall).
                    setIntent(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)));
        }

        // send sms/mms
        if (mAdapter.getPhoneNumberHelper().canSendSmsTo(number)) {
            menu.add(0, 0, 0, R.string.menu_sendTextMessage).setIntent(new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts("sms", number, null)));
        }

        // add to speed dial
        if ((!isVoicemail) && (isCanBeCalled)) {
           if (Utils.hasSpeedDialByNumber(getActivity(), number)) {
                menu.add(0, MENU_ITEM_ADD_TO_SPD, 0, R.string.menu_deleteFromSpeedDial);
            }
            else {
                menu.add(0, MENU_ITEM_ADD_TO_SPD, 0, R.string.menu_addToSpeedDial);
            }
        }
         // add to Taskify
        if (isTaskifyAvailable){
             Intent intent = new Intent(ACTION_NEW_TASKS);
             intent.putExtra(EXTRA_TASKIFY_TITLE, getString(R.string.call, taskifyTitle));
             intent.putExtra(EXTRA_TAKIFY_URI, "tel:" + number);
             menu.add(0, MENU_ITEM_ADD_CALLBACK_REMINDER_TO_TASKS, 0, R.string.menu_addcallbackremindertotasks).
             setIntent(intent);
        }
        // delete from log
        menu.add(0, CONTEXT_MENU_ITEM_DELETE, 0, R.string.recentCalls_removeFromRecentList);

    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Convert the menu info to the proper type
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfoIn", e);
            return false;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(menuInfo.position);
        String number = null;
        String name = null;
        String numberLable = null;
        boolean contactInfoPresent = false;
        if (cursor != null) {
           number = cursor.getString(CallLogQuery.NUMBER);
           numberLable = Phone.getTypeLabel(getActivity().getResources(),cursor.getInt(CallLogQuery.CACHED_NUMBER_TYPE),
                                   cursor.getString(CallLogQuery.CACHED_NUMBER_LABEL)).toString();
        } else {
            //Bad Case. No Cursor, We better return from here
            return false;
        }
        switch (item.getItemId()) {
            case CONTEXT_MENU_ITEM_DELETE: {
                int groupSize = 1;
                if (mAdapter.isGroupHeader(menuInfo.position)) {
                    groupSize = mAdapter.getGroupSize(menuInfo.position);
                }
                if (cursor != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < groupSize; i++) {
                    if (i != 0) {
                        sb.append(",");
                        cursor.moveToNext();
                    }
                    long id = cursor.getLong(CallLogQuery.ID);
                    sb.append(id);
                }
                getActivity().getContentResolver().delete(Calls.CONTENT_URI_WITH_VOICEMAIL, Calls._ID + " IN (" + sb + ")", null);

              }
              return true;//IKMAIN-13377
            }

            case MENU_ITEM_ADD_TO_SPD: {
                if (item.getTitle().toString().compareTo(getActivity().getString(R.string.menu_addToSpeedDial)) == 0) {
                        Intent intent = new Intent();
                        intent.putExtra(SpeedDialPicker.CALL_NUMBER_TYPE, numberLable);
                        intent.putExtra(SpeedDialPicker.CALL_NUMBER, number);
                        intent.setClass(getActivity(), SpeedDialPicker.class);
                        getActivity().startActivity(intent);
                } else {
                    // Delete number from Speed Dial
                       Utils.unassignSpeedDial(getActivity(), number);
                }

                return true;
            }

            case MENU_ITEM_ADD_TO_CONTACTS: {
                if (cursor != null) {
                      Intent intent = new Intent(ADD_CONTACT_DIALOG); //MOTO Dialer Code - IKHSS6-13862
                      intent.putExtra(Insert.PHONE, number);
                      startActivity(intent);
                } else {
                    Log.e(TAG, "cursor null in onContextItemSelected :-(");
                }
                return true;
            }

            case CONTEXT_MENU_CALL_CONTACT: {
                startActivity(item.getIntent());
                return true;
            }

            case  MENU_ITEM_ADD_CALLBACK_REMINDER_TO_TASKS: {
                 try{
                     startActivityForResult(item.getIntent(), REQUEST_NEW_TASKIFY);
                 } catch (ActivityNotFoundException e) {
                     showTaskFailedDialog();
                     log("can't start Taskify activity/app");
                 }
                 return true;
            }

        }
        return super.onContextItemSelected(item);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mShowOptionsMenu) {
            final MenuItem itemDeleteAll = menu.findItem(R.id.delete_all);
            // Check if all the menu items are inflated correctly. As a shortcut, we assume all
            // menu items are ready if the first item is non-null.
            if (itemDeleteAll != null) {
                itemDeleteAll.setEnabled(mAdapter != null && !mAdapter.isEmpty());
                menu.findItem(R.id.show_voicemails_only).setVisible(
                        mVoicemailSourcesAvailable && !mShowingVoicemailOnly);
                menu.findItem(R.id.show_all_calls).setVisible(
                        mVoicemailSourcesAvailable && mShowingVoicemailOnly);
                SpeedDialHelper.setupOptionMenu_SpdSetup(this, menu);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                if(RomUtility.isOutofMemory()){
                    Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    return true;
                    }

                ClearCallLogDialog.show(getFragmentManager(), getCallType());
                return true;

            case R.id.show_voicemails_only:
                mfilterSpinner.setVisibility(View.GONE);
                int networkType = getNetworkType();
                mEmptyLabel.setText(getString(CallLogViewTypeHelper.setEmptyPrompt(CallLogViewTypeHelper.VIEW_OPTION_ALL), getNetworkPrompt(networkType)));
                mCallLogQueryHandler.fetchVoicemailOnly(networkType);
                mShowingVoicemailOnly = true;
                return true;

            case R.id.show_all_calls:
                mfilterSpinner.setVisibility(View.VISIBLE);
                mfilterSpinner.setSelection(CallLogViewTypeHelper.VIEW_OPTION_ALL);
                if (isTwoCardsEnable) {
                    mNetworkSpinner.setVisibility(View.VISIBLE);
                    mNetworkSpinner.setSelection(CallLogViewTypeHelper.NETWORK_TYPE_ALL);
                }
                mEmptyLabel.setText(getString(CallLogViewTypeHelper.setEmptyPrompt(CallLogViewTypeHelper.VIEW_OPTION_ALL), 
                        getNetworkPrompt(CallLogViewTypeHelper.NETWORK_TYPE_ALL)));
                mCallLogQueryHandler.fetchFilterCalls(CallLogViewTypeHelper.VIEW_OPTION_ALL, CallLogViewTypeHelper.NETWORK_TYPE_ALL);
                mShowingVoicemailOnly = false;
                return true;
            case R.id.add_contact_tab:
            	if(RomUtility.isOutofMemory()){
                	Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                	return true;
                }            	
            default:
                return false;
        }
    }

    public void callSelectedEntry() {
        int position = getListView().getSelectedItemPosition();
        if (position < 0) {
            // In touch mode you may often not have something selected, so
            // just call the first entry to make sure that [send] [send] calls the
            // most recent entry.
            position = 0;
        }
        final Cursor cursor = (Cursor)mAdapter.getItem(position);
        if (cursor != null /*&& cursor.moveToPosition(position)*/) { //TBD, why moto add it
            String number = cursor.getString(CallLogQuery.NUMBER);
            if (!mAdapter.getPhoneNumberHelper().canPlaceCallsTo(number)) {
                // This number can't be called, do nothing
                return;
            }
            Intent intent;
            // If "number" is really a SIP address, construct a sip: URI.
            if (PhoneNumberUtils.isUriNumber(number)) {
                intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                    Uri.fromParts("sip", number, null));
            } else {
                // We're calling a regular PSTN phone number.
                // Construct a tel: URI, but do some other possible cleanup first.
                int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
                if (!number.startsWith("+") &&
                       (callType == Calls.INCOMING_TYPE
                                || callType == Calls.MISSED_TYPE)) {
                    // If the caller-id matches a contact with a better qualified number, use it
                    String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
                    number = mAdapter.getBetterNumberFromContacts(number, countryIso);
                }
                if (ContactsUtils.haveTwoCards(getActivity())) {
                    intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                    intent.putExtra("phoneNumber", number);
                } else {
                    intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                        Uri.fromParts("tel", number, null));
                }
            }
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            MEDialer.onDial(getActivity(), intent, MEDialer.DialFrom.RECENT); // Motorola, FTR 36344, Apr-19-2011, IKPIM-384
            startActivity(intent);
            if(VDBG) log("Calling_Kpi_Debug, RecentCall callEntry");
        }
    }

    @VisibleForTesting
    CallLogAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if(VDBG) log("onVisibilityChanged: visible = " + visible);
        if (mShowOptionsMenu != visible) {
            mShowOptionsMenu = visible;
            // Invalidate the options menu since we are changing the list of options shown in it.
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }

        if (visible && isResumed()) {
            refreshData();
        }

        if (!visible) {
            finishMode();
            updateOnExit();
        }

        fragmentVisibilty = visible;//Moto Dialer Code //TBD, it should be called once rotate so fragmentVisibilty keep correct value after rotate
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        // Mark all entries in the contact info cache as out of date, so they will be looked up
        // again once being shown.
        if(VDBG) log("refreshData!");
        //mAdapter.invalidateCache(); //MOTO comment, change to invalidate while contact DB change.
        mAdapter.invalidatePreDrawListener(); //To make sure the contact/city id query thread can be re-create/start.
        startCallsQuery();
        startVoicemailStatusQuery();
        updateOnEntry();
    }

    /** Removes the missed call notifications. */
    private void removeMissedCallNotifications() {
        try {
            ITelephony telephony =
                    ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (telephony != null) {
                telephony.cancelMissedCallsNotification();
            } else {
                Log.w(TAG, "Telephony service is null, can't call " +
                        "cancelMissedCallsNotification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to clear missed calls notification due to remote exception");
        }
    }

    /** Updates call data and notification state while leaving the call log tab. */
    private void updateOnExit() {
        updateOnTransition(false);
    }

    /** Updates call data and notification state while entering the call log tab. */
    private void updateOnEntry() {
        updateOnTransition(true);
    }

    private void updateOnTransition(boolean onEntry) {
        // We don't want to update any call data when keyguard is on because the user has likely not
        // seen the new calls yet.
        if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // On either of the transitions we reset the new flag and update the notifications.
            // While exiting we additionally consume all missed calls (by marking them as read).
            // This will ensure that they no more appear in the "new" section when we return back.
            mCallLogQueryHandler.markNewCallsAsOld();
            if (CallLogViewTypeHelper.needResetMissedCallFlag(getCallType())) {
                if (!onEntry) {
                    mCallLogQueryHandler.markMissedCallsAsRead();
                }
                removeMissedCallNotifications();
            }
            if (CallLogViewTypeHelper.needResetVoicemailFlag(getCallType())) {
                updateVoicemailNotifications();
            }
        }
    }

    private void updateVoicemailNotifications() {
        Intent serviceIntent = new Intent(getActivity(), CallLogNotificationsService.class);
        serviceIntent.setAction(CallLogNotificationsService.ACTION_UPDATE_NOTIFICATIONS);
        getActivity().startService(serviceIntent);
    }

    //MOTO Dialer Code Start
    private class ContactsObserver extends ContentObserver {

        public ContactsObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            if (DBG) {
                log("ContactsObserver onChange fragmentVisibilty = " + fragmentVisibilty + "isResumed() = " + isResumed() + " selfChange = " + selfChange);
            }
            mAdapter.invalidateCache();
            mAdapter.invalidatePreDrawListener();
            if (fragmentVisibilty && isResumed() && (!selfChange)) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    // MOT Calling code - IKSTABLEFOURV-1933
    /*private String getVMNumber() {
        if (mVoiceMailNumber == null) {
            if (DBG) log("start to query voicemain number");
            TelephonyManager tm = (TelephonyManager)getActivity().getSystemService(
                    Context.TELEPHONY_SERVICE);
            mVoiceMailNumber = tm.getVoiceMailNumber();
        }
        return mVoiceMailNumber;
    }*/
    // MOT Calling code End - IKSTABLEFOURV-1933


/*
// moto add for muti-select mode in rc
// TODO: talk with CXD about the UI and all the needed funtionla
*/
    private class SelectActionModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private MenuItem mCallMenuView;
        private MenuItem mAddMenuView;
        private MenuItem mEditBeforeCallMenuView;
        private MenuItem mAddSpeedDialMenuView;
        private MenuItem mDeleteSpeedDialMenuView;
        private MenuItem mTaskMenuView;
        private MenuItem mVContactMenuView;
        private MenuItem mMessageMenuView;
        //begin add by txbv34 for IKCBSMMCPPRC-1231
        private MenuItem mIpCallMenuItem;
        private MenuItem mAddToBlackListMenuItem;
        private MenuItem mAddToWhiteListMenuItem;  
        private MenuItem mSendNumSMSMenuItem;
        //end add by txbv34 for IKCBSMMCPPRC-1231
        private ActionMode mActionMode;
        private boolean isMutiSelected;

        void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
       }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.calllog_multi_select_menu, menu);
            mCallMenuView = menu.findItem(R.id.call);
            mAddMenuView = menu.findItem(R.id.add);
            mEditBeforeCallMenuView = menu.findItem(R.id.edit);
            mAddSpeedDialMenuView = menu.findItem(R.id.add_speeddial);
            mDeleteSpeedDialMenuView = menu.findItem(R.id.delete_speeddial);
            mTaskMenuView = menu.findItem(R.id.task);
            mVContactMenuView = menu.findItem(R.id.view);
            mMessageMenuView = menu.findItem(R.id.message);
            //begin add by txbv34 for IKCBSMMCPPRC-1231
            mIpCallMenuItem = menu.findItem(R.id.menu_ip_call);
            mAddToBlackListMenuItem = menu.findItem(R.id.menu_add_to_blacklist);
            mAddToWhiteListMenuItem = menu.findItem(R.id.menu_add_to_whitelist);
            mSendNumSMSMenuItem = menu.findItem(R.id.menu_send_via_sms);
            //end add by txbv34 for IKCBSMMCPPRC-1231
            mAdapter.setActionModeStatus(true);

            if (mMultiSelectActionBarView == null) {
                    mMultiSelectActionBarView = (ViewGroup)LayoutInflater.from(getActivity())
                        .inflate(R.layout.calllog_list_multi_select_actionbar, null);
                    mSelectedConvCount =
                        (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((TextView)mMultiSelectActionBarView.findViewById(R.id.title))
                .setText(R.string.select_conversations);

            mActionMode = mode;
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(getActivity())
                    .inflate(R.layout.calllog_list_multi_select_actionbar, null);
                mode.setCustomView(v);

                mSelectedConvCount = (TextView)v.findViewById(R.id.selected_count);
            }
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(VDBG) log("onActionItemClicked entered, isMutiSelected = " + isMutiSelected);
            String number = null;
            String formatNumber = null;  //MOTO Dialer Code - IKHSS7-2091
            Cursor cursor = null;
            Uri lookupUri = null;
            String name = null;
            StringBuilder sbNumber = new StringBuilder();
            StringBuilder tmpId = new StringBuilder();
            ListView listView = getListView();
            int selectedNumbers = listView.getCheckedItemCount() ;
            int adapterSize = mAdapter.getCount();
            SparseBooleanArray checkStates = listView.getCheckedItemPositions();
            if (checkStates != null){
            if (!isMutiSelected){
                 // only one item selected, get the cursor for use
                 // get cursor, lookupUri, number, name and Ids for one item
                 for(int i = 0; i < adapterSize; i ++){
                    if(checkStates.get(i)) {
                       cursor = (Cursor)mAdapter.getItem(i);
                       lookupUri = UriUtils.parseUriOrNull(cursor.getString(CallLogQuery.CACHED_LOOKUP_URI));
                       number = cursor.getString(CallLogQuery.NUMBER);
                       formatNumber = cursor.getString(CallLogQuery.CACHED_FORMATTED_NUMBER);  //MOTO Dialer Code - IKHSS7-2091
                       name = cursor.getString(CallLogQuery.CACHED_NAME);
                       int groupSize = 1;
                       if (mAdapter.isGroupHeader(i)) {
                           groupSize = mAdapter.getGroupSize(i);
                       }
                       for (int j = 0; j < groupSize; j++) {
                             if (j != 0) {
                                 tmpId.append(",");
                                 cursor.moveToNext();
                             }
                             long id = cursor.getLong(CallLogQuery.ID);
                             tmpId.append(id);
                        }
                        break;
                    }
                 }
            } else {
                // more than one items selected, get the numbers for send muti-sms;
                // get the ids for muti-delete
              for(int i = 0; i < adapterSize; i ++){
                  if(!checkStates.get(i)) continue;
                  cursor = (Cursor)mAdapter.getItem(i); 
                   if (cursor != null) {
                       number = cursor.getString(CallLogQuery.NUMBER);
                       if(mAdapter.getPhoneNumberHelper().canSendSmsTo(number)){
                         sbNumber.append(number);
                       }
                       int groupSize = 1;
                       if (mAdapter.isGroupHeader(i)) {
                           groupSize = mAdapter.getGroupSize(i);
                       }
                       for (int j = 0; j < groupSize; j++) {
                            if (j != 0) {
                                tmpId.append(",");
                                cursor.moveToNext();
                            }
                            long id = cursor.getLong(CallLogQuery.ID);
                            tmpId.append(id);
                       }
                      // add "," to the string
                           if(selectedNumbers >1){
                              if(mAdapter.getPhoneNumberHelper().canSendSmsTo(number)){
                                sbNumber.append(",");
                               }
                              tmpId.append(",");
                              selectedNumbers--;
                            }
                 }
            }                        
          }
        }
          if(VDBG) log("onCreateActionMode: number"  + sbNumber.toString());
          if(VDBG) log("onCreateActionMode: id"  + tmpId.toString());
            switch (item.getItemId()) {
            //begin add by txbv34 for IKCBSMMCPPRC-1231
            	case R.id.menu_ip_call:
                    Intent IPintent = new Intent(ContactsUtils.TRANS_DIALPAD);
                    IPintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    IPintent.putExtra("phoneNumber", number);
                    Log.v(TAG, "menu_ip_call number = "+number);
                    IPintent.putExtra("isIpCall", true);
                    startActivity(IPintent);
        		break;           		
            	case R.id.menu_add_to_blacklist:
            		if(RomUtility.isOutofMemory()){
                    	Toast.makeText(getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    	break ;
                    }
            		if (!isMutiSelected){
                        Intent blackintent = new Intent(Intent.ACTION_INSERT);
                        blackintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
                        if(name != null && name.length() > 0){
                        blackintent.putExtra("blocktype", "blackname");
                            Log.d(TAG,"menu_add_to_blacklist,blackname");
                        blackintent.putExtra("blackname", number);
                    	}else{
                            blackintent.putExtra("blocktype", "blacklist");
                            Log.d(TAG,"menu_add_to_blacklist,blacklist");
                            blackintent.putExtra("blacklist", number);
                    	}   
                        Log.v(TAG, "getAddToBlackList number = "+number);
            			startActivity(blackintent);
            		}
            		break;
            	case R.id.menu_add_to_whitelist:
            		if(RomUtility.isOutofMemory()){
                    	Toast.makeText(getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    	break ;
                    }
            		if (!isMutiSelected){
                        Intent whiteintent = new Intent(Intent.ACTION_INSERT);
                        whiteintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
                    	if(name != null && name.length() > 0){
                        whiteintent.putExtra("blocktype", "whitename");
                        whiteintent.putExtra("whitename", number);
                            Log.d(TAG,"menu_add_to_whitelist,blackname");
                    	}else{
                            whiteintent.putExtra("blocktype", "whitelist");
                            whiteintent.putExtra("whitelist", number);
                            Log.d(TAG,"menu_add_to_whitelist,blackname");
                    	}  
                        Log.v(TAG, "getAddToWhiteList number = "+number);
            			startActivity(whiteintent);
            		}
            		break;
            	case R.id.menu_send_via_sms:{
                	Intent SMSintent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"));
                	SMSintent.putExtra("sms_body", number);
                	SMSintent.putExtra("exit_on_sent", true);
                	try {
                		startActivity(SMSintent);
                	} catch (android.content.ActivityNotFoundException e) {
                		// don't crash if not support
                	}
            		break;
            	}
            	//end add by txbv34	 for IKCBSMMCPPRC-1231	
                case R.id.delete:
                	if(RomUtility.isOutofMemory()){
                    	Toast.makeText(getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                    	break ;
                    }
                final StringBuilder sbId = tmpId;
                final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                        getString(R.string.clearCallLogProgress_title),
                        "", true, false);
                     progressDialog.show();
  
                    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        getActivity().getContentResolver().delete(Calls.CONTENT_URI_WITH_VOICEMAIL, Calls._ID + " IN (" + sbId + ")", null);
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                            progressDialog.dismiss();
                            if (getAdapter() != null) {
                                getAdapter().clearAllCache();
                            }
                    }
                  };
                  task.execute();
                  break;

                case R.id.message:
                    if (!isMutiSelected) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO,Uri.fromParts("sms", number, null));
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_SENDTO,Uri.fromParts("sms", sbNumber.toString(), null));
                        startActivity(intent);
                    }
                    break;

                    case R.id.call:
                    if (!isMutiSelected) {
                        Intent intent;
                        if (ContactsUtils.haveTwoCards(getActivity())) {
                            intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("phoneNumber", number);
                        } else {
                            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", number, null));
                        }
                        MEDialer.onDial(getActivity(), intent, MEDialer.DialFrom.RECENT); //MOTO Dialer Code IKHSS6-723
                        startActivity(intent); 
                    }
                    break;

                    case R.id.add:
                    	if(RomUtility.isOutofMemory()){
                        	Toast.makeText(getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
                        	break ;
                        }
                    if (!isMutiSelected) {
                        Intent intent = new Intent(ADD_CONTACT_DIALOG); //MOTO Dialer Code - IKHSS6-13862
                        intent.putExtra(Insert.PHONE, number);
                        startActivity(intent);
                    }
                    break;

                   case R.id.view:
                    if (!isMutiSelected) {
                       Intent intent = new Intent(Intent.ACTION_VIEW,lookupUri);
                       startActivity(intent);    
                    }
                    break;

                    case R.id.edit:
                    if (!isMutiSelected) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null));
                          startActivity(intent);
                    }
                    break;

                    case R.id.add_speeddial:
                    case R.id.delete_speeddial:
                    break;

                    case R.id.task:
                    if (!isMutiSelected) {
                      //MOTO Dialer Code Start - IKHSS7-2091
                      String taskifyTitle = null;
                      if (lookupUri != null) {
                               mTaskifyContextMenuTitle = name;
                               taskifyTitle = name;
                      } else {
                               mTaskifyContextMenuTitle = (TextUtils.isEmpty(formatNumber)?number:formatNumber);
                               taskifyTitle = number; //TO DO, may pass the converted number
                      }
                        Intent intent = new Intent(ACTION_NEW_TASKS);
                               intent.putExtra(EXTRA_TASKIFY_TITLE, getString(R.string.call, taskifyTitle));
                               intent.putExtra(EXTRA_TAKIFY_URI, "tel:" + number);//TO DO, may pass the converted number
                        //MOTO Dialer Code End - IKHSS7-2091
                    try{
                        startActivityForResult(intent, REQUEST_NEW_TASKIFY);
                    } catch (ActivityNotFoundException e) {
                        showTaskFailedDialog();
                        Log.w(TAG, "ActivityNotFoundException can't start Taskify activity/app");
                    }  
                    }
                    break;

                default:
                    break;
            }
            mode.finish();
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
              if(VDBG) log("onDestroyActionMode enter");
              mAdapter.setActionModeStatus(false);
              getListView().clearChoices();
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
                ListView listView = getListView();
                String number = null;
                Cursor  cursor = null;
                boolean contactInfoPresent = false;
                if(VDBG) log("onItemCheckedStateChanged: position " + position + " id= " + id + " checked= " + checked);
                if(VDBG) log("onItemCheckedStateChanged: listView " + listView);

                isMutiSelected = (listView.getCheckedItemCount() == 1)?false:true;
                final int checkedCount = listView.getCheckedItemCount();
                //Set select X in action bar
                mSelectedConvCount.setText(Integer.toString(checkedCount));

                if (isMutiSelected){
                       // more than 1 item are selected , only can see @Delete @SendMessage button
                     mCallMenuView.setVisible(false);
                     mAddMenuView.setVisible(false);
                     mEditBeforeCallMenuView.setVisible(false);
                     mTaskMenuView.setVisible(false);
                     SpeedDialHelper.setupOptionMenu_SpdAddRemove(getActivity(), mAddSpeedDialMenuView,
                     mDeleteSpeedDialMenuView, null, null);
                     mVContactMenuView.setVisible(false);
                     mMessageMenuView.setVisible(true);
                     //begin add by txbv34 for IKCBSMMCPPRC-1231
                     mIpCallMenuItem.setVisible(false);
                     mSendNumSMSMenuItem.setVisible(false);
                     mAddToBlackListMenuItem.setVisible(false);
                     mAddToWhiteListMenuItem.setVisible(false);
                     //end add by txbv34 for IKCBSMMCPPRC-1231
                } else { // only one item is selected

                     int adapterSize = mAdapter.getCount();
                     SparseBooleanArray checkStates = listView.getCheckedItemPositions();
                     if (checkStates != null){
                     for(int i = 0; i < adapterSize; i ++){
                         if(checkStates.get(i)){
                              cursor = (Cursor)mAdapter.getItem(i);
                              break;
                         }
                     }
                    }
                     if (cursor != null) {
                         number = cursor.getString(CallLogQuery.NUMBER);
                         contactInfoPresent = (UriUtils.parseUriOrNull(cursor.getString(CallLogQuery.CACHED_LOOKUP_URI)) != null);
                     }else {
                         Log.w(TAG, "onItemCheckedStateChanged: cursor is null!!!");  
                     }
                     

                     // check if it's Voicemal number
                     boolean isVoicemail = mAdapter.getPhoneNumberHelper().isVoicemailNumber(number);
                     if(VDBG) log("onItemCheckedStateChanged: isVoicemail = " + isVoicemail + "number = " + number);

                     if (mAdapter.getPhoneNumberHelper().canPlaceCallsTo(number)) {
                         // call menu
                         if (cursor != null) {
                             if (cursor.getInt(CallLogQuery.CALL_TYPE) == Calls.VOICEMAIL_TYPE) {  //Only show call for voicemail entry
                                mCallMenuView.setVisible(true);
                             } else {
                                mCallMenuView.setVisible(false);
                             }
                         }

                         //view contact & add to contact menu
                         if (contactInfoPresent) {
                             mVContactMenuView.setVisible(true);
                             mAddMenuView.setVisible(false);
                         } else {
                               mVContactMenuView.setVisible(false);
                               if(!isVoicemail) {
                                    mAddMenuView.setVisible(true);
                               } else {
                                    mAddMenuView.setVisible(false);
                               }
                         }

                         // edit number before call
                         mEditBeforeCallMenuView.setVisible(!isVoicemail);
                         // send message
                         mMessageMenuView.setVisible(mAdapter.getPhoneNumberHelper().canSendSmsTo(number));
                         //begin add by txbv34 for IKCBSMMCPPRC-1231
                         mIpCallMenuItem.setVisible(!isVoicemail);
                         mSendNumSMSMenuItem.setVisible(!isVoicemail);
                         mAddToBlackListMenuItem.setVisible(!isVoicemail);
                         mAddToWhiteListMenuItem.setVisible(!isVoicemail);
                         //end add by txbv34 for IKCBSMMCPPRC-1231
                         //add to task
                         if(VDBG) log("isTaskifyAvailable = " + isTaskifyAvailable);
                         if (isTaskifyAvailable){
                             mTaskMenuView.setVisible(true);
                         } else {
                             mTaskMenuView.setVisible(false);
                         }
                         //speed dial
                         if (cursor != null) {
                             String numberLable = Phone.getTypeLabel(getActivity().getResources(),
                                     cursor.getInt(CallLogQuery.CACHED_NUMBER_TYPE),
                                     cursor.getString(CallLogQuery.CACHED_NUMBER_LABEL)).toString();
                             SpeedDialHelper.setupOptionMenu_SpdAddRemove(getActivity(),
                                         mAddSpeedDialMenuView, mDeleteSpeedDialMenuView, number,
                                         numberLable);
                         }

                      } else {
                         //Invalid number(Unknow or Private etc...)
                         //set all menu to invisible except delete action
                         mCallMenuView.setVisible(false);
                         mAddMenuView.setVisible(false);
                         mEditBeforeCallMenuView.setVisible(false);
                         mTaskMenuView.setVisible(false);
                         SpeedDialHelper.setupOptionMenu_SpdAddRemove(getActivity(), mAddSpeedDialMenuView,
                         mDeleteSpeedDialMenuView, null, null);
                         mVContactMenuView.setVisible(false);
                         mMessageMenuView.setVisible(false);
                         //begin add by txbv34 for IKCBSMMCPPRC-1231
                         mIpCallMenuItem.setVisible(false);
                         mSendNumSMSMenuItem.setVisible(false);
                         mAddToBlackListMenuItem.setVisible(false);
                         mAddToWhiteListMenuItem.setVisible(false);
                         //end add by txbv34 for IKCBSMMCPPRC-1231
                      }
                }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
             // click the item will start the call detail activity.
          // MOTO Calling - IKHSS7-6114 Begin
          int[] layoutLocation = new int[] { -1, -1 };
          v.getLocationInWindow(layoutLocation);
          if(CDBG) Log.d("Calling_dbg","onListItemClick layoutLocation[0]:" + layoutLocation[0] + " layoutLocation[1]:" +layoutLocation[1] + "position: " + position);
          // MOTO Calling - IKHSS7-6114 End

          final CallLogListItemViews views = (CallLogListItemViews) v.getTag();
          IntentProvider intentProvider = (IntentProvider) views.primaryActionView.getTag();
          if (intentProvider != null) {
               getActivity().startActivity(intentProvider.getIntent(getActivity()));
             }
    }

     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         switch (requestCode) {
             case REQUEST_NEW_TASKIFY:
                 if (RESULT_OK == resultCode) {
                     if (data != null) { // Mot Calling code IKPIM-431
                         boolean addToTaskSuccessful = data.getBooleanExtra(EXTRA_TAKIFY_RESULT, false);
                             if (addToTaskSuccessful) {
                                 String toaststr = getString(R.string.add_to_the_tasks_application, mTaskifyContextMenuTitle);
                                 Toast.makeText(getActivity().getApplicationContext(), toaststr, Toast.LENGTH_LONG).show();
                             } else {
                                 showTaskFailedDialog();
                                 Log.w(TAG,"onActivityResult can't start Taskify activity/app");
                             }
                     }
                 }
                 break;
             default :
                 break;
         }
         return;
     }

    private void showTaskFailedDialog(){
         String msg = getString(R.string.failed_to_add_tasks, mTaskifyContextMenuTitle);
         mTaskFailedDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.task_failed).setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(msg).setNegativeButton(R.string.ok, new OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                }}).create();
          mTaskFailedDialog.show();
    }

    private static boolean isPkgInstalled(Context ctx, String packageName){
        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }
    void finishMode(){
        if (mSelectCallback != null) {
            mSelectCallback.finish();
        }
    } 
    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }

    //MOT Dialer Code Start - IKHSS6UPGR-4081
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
        int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mAdapter.onScrollStateChanged(scrollState);
    }
    //MOT Dialer Code End - IKHSS6UPGR-4081

    //MOTO Dialer Code End


    private void loadSpinnerFilter() {
        if (mVoicemailSourcesAvailable) {
            if (VDBG) {
                log("VoicemailSourcesAvailable");
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.recent_call_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
            mfilterSpinner.setAdapter(adapter);
            mfilterSpinner.setSelection(mPrefs.getInt(PREF_CALL_TYPE, CallLogViewTypeHelper.VIEW_OPTION_ALL));
        } else {
            if (VDBG) {
                log("VoicemailSourcesNotAvailable");
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.recent_call_types_without_voicemail, android.R.layout.simple_spinner_item);
             adapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
            mfilterSpinner.setAdapter(adapter);
            mfilterSpinner.setSelection(mPrefs.getInt(PREF_CALL_TYPE, CallLogViewTypeHelper.VIEW_OPTION_ALL));
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.network_types, android.R.layout.simple_spinner_item);
             adapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
            mNetworkSpinner.setAdapter(adapter);
            mNetworkSpinner.setSelection(mPrefs.getInt(PREF_NETWORK_TYPE, CallLogViewTypeHelper.NETWORK_TYPE_ALL));
    }

    private Spinner.OnItemSelectedListener mSpinnerListener=new Spinner.OnItemSelectedListener() {

        public void onNothingSelected(AdapterView<?> parent) {
        }

        public void onItemSelected(
                AdapterView<?> parent, View view, int position, long id) {
            mCallLogQueryHandler.markNewCallsAsOld();
           /* if (CallLogViewTypeHelper.needResetMissedCallFlag(position)) { // Motorola, July-05-2011, IKMAIN-23833
                mCallLogQueryHandler.markMissedCallsAsRead();
            }*/

            // Update empty list label
            mEmptyLabel.setText(getString(CallLogViewTypeHelper.setEmptyPrompt(position), getNetworkPrompt(getNetworkType())));
            mPrefs.edit().putInt(PREF_CALL_TYPE, position).commit();
            finishMode();
            fetchCalls();//Comments: Start Query Here!!
        }
    };

    private int getCallType() {
        int viewType = CallLogViewTypeHelper.VIEW_OPTION_ALL;
        if (mPrefs != null) {
            viewType = mPrefs.getInt(PREF_CALL_TYPE, CallLogViewTypeHelper.VIEW_OPTION_ALL);
        }
        if (mfilterSpinner != null) {
            mfilterSpinner.setSelection(viewType);
        }
        return viewType;
    }

    private Spinner.OnItemSelectedListener mNetworkSpinnerListener = new Spinner.OnItemSelectedListener() {

        public void onNothingSelected(AdapterView<?> parent) {
        }

        public void onItemSelected(
                AdapterView<?> parent, View view, int position, long id) {
            mCallLogQueryHandler.markNewCallsAsOld();
           /* if (CallLogViewTypeHelper.needResetMissedCallFlag(position)) { // Motorola, July-05-2011, IKMAIN-23833
                mCallLogQueryHandler.markMissedCallsAsRead();
            }*/

            // Update empty list label
            mEmptyLabel.setText(getString(CallLogViewTypeHelper.setEmptyPrompt(getCallType()), getNetworkPrompt(position)));
            mPrefs.edit().putInt(PREF_NETWORK_TYPE, position).commit();
            finishMode();
            fetchCalls();//Comments: Start Query Here!!
        }
    };

    private int getNetworkType() {
    	if (!isTwoCardsEnable) return CallLogViewTypeHelper.NETWORK_TYPE_ALL;
        int viewType = CallLogViewTypeHelper.NETWORK_TYPE_ALL;
        if (mPrefs != null) {
            viewType = mPrefs.getInt(PREF_NETWORK_TYPE, CallLogViewTypeHelper.NETWORK_TYPE_ALL);
        }
        if (mNetworkSpinner != null) {
            mNetworkSpinner.setSelection(viewType);
        }
        return viewType;
    }

    private String getNetworkPrompt(int networkType) {
        String emptyPrompt = "";
        switch (networkType) {
        case CallLogViewTypeHelper.NETWORK_TYPE_ALL:
            emptyPrompt = "";
            break;

        case CallLogViewTypeHelper.NETWORK_TYPE_CDMA:
            emptyPrompt = getString(R.string.cdma_call_empty);
            break;

        case CallLogViewTypeHelper.NETWORK_TYPE_GSM:
            emptyPrompt = getString(R.string.gsm_call_empty);
            break;
        }
        return emptyPrompt;
    }

	public LocationServiceManager getmLocationServiceManager() {
		return mLocationServiceManager;
	}

	public boolean isLocationEnable (){
	    return mLocationServiceManager != null && mLocationServiceManager.ismIsLocationBound()
               && (Settings.System.getInt(getActivity().getContentResolver(), "show_location_enable", 1) == 1);
	}
}