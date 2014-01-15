/*
 * @(#)CalendarDialogListAdapter.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/08/22 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

import java.util.ArrayList;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.graphics.Color;

/**
 * This class implements the Adapter for calendar selection dialog's list view.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends BaseAdapter
 *      Implements OnClickListener
 *      Implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * This class is responsible for implementing the Adapter for calendar selection dialog's list view.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CalendarDialogListAdapter extends BaseAdapter implements
    OnClickListener, CalendarEventSensorConstants {

    /**
     * Assigned ID to "All Calendars" item in list
     */
    public static final Integer ALL_CALENDARS_ID = Integer.valueOf(-1);

    /**
     * TAG for logging the messages
     */
    private static final String TAG = "CalendarDialogListAdapter";

    /**
     * Constant for defining the {@link Message.what} for query complete
     */
    private static final int MSG_QUERY_COMPLETE = 1;

    /**
     * The default id of phone calendar
     */
    // On JB the calendar application doesn't have device calendar. On ICS id 1
    // was reserved for device calendar. The display name and account name are
    // localized for device calendar. As of now making this -2 so that the code
    // written for device calendar doesn't get executed. In future this shall be
    // changed to 1 when device calendar gets added to calendar app
    public static final int PHONE_CALENDAR_ID = -2;

    /**
     * Reference to the layout inflater
     */
    private LayoutInflater mInflater;

    /**
     * Reference to {@link Resources} instance
     */
    private Resources mResources;

    /**
     *
     * ArrayList for holding the calendar attributes read from Calendars table
     */
    private ArrayList<CalendarAttributes> mCalendarsAttrs = new ArrayList<CalendarAttributes>();

    /**
     * ArrayList for holding the IDs of the selected calendars
     */
    private ArrayList<Integer> mSelectedCalendarIds = new ArrayList<Integer>();

    /**
     * ArrayList for holding the IDs of the selected calendars for current
     * interaction with calendar selection dialogue
     */
    private ArrayList<Integer> mSelectedCalendarIdsTemp = new ArrayList<Integer>();

    /**
     * Reference to the {@link ArrayList} of {@link Integer} containing the
     * preselected calendar ids
     */
    private ArrayList<Integer> mPreSelectedCalendarIds;

    /**
     * ArrayList for holding the calendar attributes temporarily till the time
     * these get copied in the main thread
     */
    private ArrayList<CalendarAttributes> mCalendarAttrsTemp;

    /**
     * Boolean to identify that current interaction with dialogue is temporary
     * because "Done" button is not pressed
     */
    private boolean mTempMode = false;

    /**
     * Reference to the observer of the dialog
     */
    private CalendarDialogObserver mObserver;

    /**
     * Reference to the handler to current thread
     */
    private Handler mHandler;

    /**
     * Boolean to identify if initialization is over
     */
    private boolean mInitialized;

    /**
     * Constructor
     *
     * @param context
     *            - context of current activity
     * @param selectedCalendarIds
     *            - ArrayList containing the IDs of selected calendars. Can be
     *            passed as null.
     */
    public CalendarDialogListAdapter(Context context,
                                     CalendarDialogObserver observer,
                                     ArrayList<Integer> selectedCalendarIds) {
        mObserver = observer;
        mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResources = context.getResources();
        mHandler = new CalendarDialogHandler();
        mPreSelectedCalendarIds = selectedCalendarIds;
        mCalendarsAttrs.add(new CalendarAttributes(ALL_CALENDARS_ID, 0,
                            mResources.getString(R.string.all_calendars), ""));
        Thread workerThread = new Thread(new WorkerRunnable(context));
        workerThread.start();
    }

    /**
     * This class implements {@link Runnable} for performing the operation of
     * querying the calendar database in a secondary thread
     *
     */
    private class WorkerRunnable implements Runnable {

        /**
         * Reference to Context
         */
        private Context mContext;

        /**
         * Constructor
         *
         * @param context
         *            - the activity's context.
         * @param selectedCalendarIds
         *            - list of preselected calendar Ids. This can be passed as
         *            null also.
         */
        public WorkerRunnable(Context context) {
            mContext = context;
        }

        public void run() {
            Cursor cursor = null;
            String selection = Calendars.VISIBLE + EQUALS_TO + INVERTED_COMMA
                               + VISIBLE_CALENDARS_ONLY + INVERTED_COMMA;
            try {
                cursor = mContext.getContentResolver()
                         .query(Calendars.CONTENT_URI,
                                new String[] { Calendars._ID,
                                               Calendars.CALENDAR_COLOR,
                                               Calendars.CALENDAR_DISPLAY_NAME,
                                               Calendars.ACCOUNT_NAME
                                             }, selection,
                                null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idColumnIndex = cursor
                                        .getColumnIndexOrThrow(Calendars._ID);
                    int colorColumnIndex = cursor
                                           .getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
                    int displayNameColumnIndex = cursor
                                                 .getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
                    int syncAccountColumnIndex = cursor
                                                 .getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
                    mCalendarAttrsTemp = new ArrayList<CalendarAttributes>();
                    do {
                        CalendarAttributes attrs = new CalendarAttributes(
                            cursor.getInt(idColumnIndex),
                            cursor.getInt(colorColumnIndex),
                            cursor.getString(displayNameColumnIndex),
                            cursor.getString(syncAccountColumnIndex));
                        if (attrs.calendarId == PHONE_CALENDAR_ID) {
                            attrs.calendarDisplayName = mResources
                                                        .getString(R.string.calendar_phone_display_name);
                            attrs.syncAccount = mResources
                                    .getString(R.string.calendar_phone_account_name);
                        }
                        mCalendarAttrsTemp.add(attrs);
                    } while (cursor.moveToNext());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, "CalendarDialogListAdapter failed to read "
                      + Calendars.CONTENT_URI + " with selection "
                      + selection);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            mHandler.sendEmptyMessage(MSG_QUERY_COMPLETE);
        }
    }

    /**
     * This class handles the messages send from a secondary thread. The
     * messages are handled in the main thread
     *
     */
    private class CalendarDialogHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_QUERY_COMPLETE: {
                if (mCalendarAttrsTemp != null) {
                    for (CalendarAttributes tempAttrs : mCalendarAttrsTemp) {
                        CalendarAttributes attrs = new CalendarAttributes(
                            tempAttrs.calendarId, tempAttrs.calendarColor,
                            tempAttrs.calendarDisplayName,
                            tempAttrs.syncAccount);
                        mCalendarsAttrs.add(attrs);
                    }
                    mCalendarAttrsTemp.clear();
                }
                if (mPreSelectedCalendarIds == null
                        || mPreSelectedCalendarIds.isEmpty()) {
                    if (!mInitialized) {
                        // First time initialization
                        for (CalendarAttributes attrs : mCalendarsAttrs) {
                            mSelectedCalendarIds.add(Integer
                                    .valueOf(attrs.calendarId));
                        }
                    } else {
                        validateCalendarIds(mSelectedCalendarIds);
                    }
                } else {
                    validateCalendarIds(mPreSelectedCalendarIds);
                    mSelectedCalendarIds.clear();
                    for (Integer id : mPreSelectedCalendarIds) {
                        mSelectedCalendarIds
                                .add(Integer.valueOf(id.intValue()));
                    }
                }
                if (mTempMode) {
                    validateCalendarIds(mSelectedCalendarIdsTemp);
                }
                mInitialized = true;
                mObserver.onInitComplete();
                notifyDataSetChanged();
                if (LOG_INFO) {
                    Log.i(TAG, "handleMessage query complete executed");
                }
                break;
            }
            default: {
                super.handleMessage(msg);
            }
            }
        }

    }

    /**
     * This method validates the calendar ids contained in the list against the
     * calendars available in Calendar database
     *
     * @param calendarIds
     */
    private void validateCalendarIds(ArrayList<Integer> calendarIds) {
        // Validate the selected calendar ids
        if (calendarIds != null) {
            for (int index = 0; index < calendarIds.size(); index++) {
                Integer id = calendarIds.get(index);
                boolean exists = false;
                // To check if selected calendar has been deleted
                for (CalendarAttributes attrs : mCalendarsAttrs) {
                    if (attrs.calendarId == id) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    calendarIds.remove(index);
                    index--;
                }
            }
        }
    }

    /**
     * This class acts as data structure for holding the attributes of selected
     * calendars
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class is responsible for providing data structure for holding the attributes of selected calendars
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private static class CalendarAttributes {

        /**
         * ID of the selected calendar
         */
        public int calendarId;

        /**
         * Color of the selected calendar
         */
        public int calendarColor;

        /**
         * Display name of the selected calendar
         */
        public String calendarDisplayName;

        /**
         * Sync account name of the selected calendar
         */
        public String syncAccount;

        /**
         * Constructor
         *
         * @param id
         *            - ID of the selected calendar
         * @param color
         *            - Color of the selected calendar
         * @param displayName
         *            - Display name of the selected calendar
         * @param account
         *            - Sync account name of the selected calendar
         */
        public CalendarAttributes(int id, int color, String displayName,
                                  String account) {
            calendarId = id;
            calendarColor = color;
            calendarDisplayName = displayName;
            syncAccount = account;
        }
    }

    /**
     * This class acts as tag for each item in list view
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class is responsible for providing tag for each item of list view
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private static class Holder {

        /**
         * Reference to view which draws the colored rectangle for the calendar
         */
        public View colorView;

        /**
         * Reference to view which draws the display name of the calendar
         */
        public TextView displayNameView;

        /**
         * Reference to view which draws the sync account name of the calendar
         */
        public TextView syncAccountView;

        /**
         * Reference to the check box for selecting the calendar
         */
        public CheckBox checkbox;

        /**
         * The calendar id of the calendar shown by the view
         */
        public int calendarId;
    }

    public int getCount() {
        return mCalendarsAttrs.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ArrayList<Integer> selectedCalendarIds;
        if (!mTempMode) {
            if (LOG_DEBUG) {
                Log.d(TAG, "getView not in temp mode");
            }
            mSelectedCalendarIdsTemp.clear();
            for (Integer id : mSelectedCalendarIds) {
                mSelectedCalendarIdsTemp.add(Integer.valueOf(id.intValue()));
            }
            selectedCalendarIds = mSelectedCalendarIds;
        } else {
            selectedCalendarIds = mSelectedCalendarIdsTemp;
        }
        if (selectedCalendarIds.isEmpty()) {
            mObserver.onAllItemsUnChecked();
        }
        CalendarAttributes calAttrs = mCalendarsAttrs.get(position);
        if (LOG_DEBUG) {
            Log.d(TAG, "getView calAttrs.mCalendarId = " + calAttrs.calendarId);
            Log.d(TAG, "getView calAttrs.mCalendarDisplayName = "
                  + calAttrs.calendarDisplayName);
        }
        if (convertView != null) {
            Holder holder = (Holder) convertView.getTag();
            if (selectedCalendarIds.contains(ALL_CALENDARS_ID)
                    || selectedCalendarIds.contains(calAttrs.calendarId)) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "getView setting checkbox to true1");
                }
                holder.checkbox.setChecked(true);
            } else {
                if (LOG_DEBUG) {
                    Log.d(TAG, "getView setting checkbox to false1");
                }
                holder.checkbox.setChecked(false);
            }
            if (position == 0) {
                holder.colorView.setVisibility(View.INVISIBLE);
                holder.syncAccountView.setVisibility(View.GONE);
                holder.displayNameView.setTextColor(Color.WHITE);
                holder.displayNameView.setText(calAttrs.calendarDisplayName);
            } else {
                holder.colorView.setVisibility(View.VISIBLE);
                holder.syncAccountView.setVisibility(View.VISIBLE);
                holder.colorView.setBackgroundColor(calAttrs.calendarColor);
                holder.displayNameView.setTextColor(Color.WHITE);
                holder.displayNameView.setText(calAttrs.calendarDisplayName);
                holder.syncAccountView.setTextColor(Color.WHITE);
                holder.syncAccountView.setText(calAttrs.syncAccount);

            }
            holder.calendarId = calAttrs.calendarId;
            return convertView;
        }
        View view = mInflater.inflate(R.layout.calendar_list_dialog_item, null);
        Holder holder = new Holder();
        holder.colorView = (View) view
                           .findViewById(R.id.calendar_dialog_list_item_color);
        holder.displayNameView = (TextView) view
                                 .findViewById(R.id.calendar_dialog_list_item_display_name);
        holder.syncAccountView = (TextView) view
                                 .findViewById(R.id.calendar_dialog_list_item_sync_account);
        holder.checkbox = (CheckBox) view
                          .findViewById(R.id.calendar_dialog_list_item_checkbox);
        view.setTag(holder);
        view.setOnClickListener(this);
        holder.checkbox.setOnClickListener(this);
        if (position == 0) {
            holder.colorView.setVisibility(View.INVISIBLE);
            holder.syncAccountView.setVisibility(View.GONE);
            holder.displayNameView.setTextColor(Color.WHITE);
            holder.displayNameView.setText(calAttrs.calendarDisplayName);
        } else {
            holder.colorView.setVisibility(View.VISIBLE);
            holder.syncAccountView.setVisibility(View.VISIBLE);
            holder.colorView.setBackgroundColor(calAttrs.calendarColor);
            holder.displayNameView.setTextColor(Color.WHITE);
            holder.displayNameView.setText(calAttrs.calendarDisplayName);
            holder.syncAccountView.setTextColor(Color.WHITE);
            holder.syncAccountView.setText(calAttrs.syncAccount);

        }
        holder.calendarId = calAttrs.calendarId;
        if (selectedCalendarIds.contains(ALL_CALENDARS_ID)
                || selectedCalendarIds.contains(calAttrs.calendarId)) {
            if (LOG_DEBUG) {
                Log.d(TAG, "getView setting checkbox to true2");
            }
            holder.checkbox.setChecked(true);
        } else {
            if (LOG_DEBUG) {
                Log.d(TAG, "getView setting checkbox to false2");
            }
            holder.checkbox.setChecked(false);
        }
        return view;
    }


    public void onClick(View view) {
        mTempMode = true;
        switch (view.getId()) {
        case R.id.calendar_dialog_list_item_checkbox: {
            View listItemView = (View) view.getParent();
            Holder holder = (Holder) listItemView.getTag();
            if (holder.checkbox.isChecked()) {
                updateCalendarIdsTemp(holder.calendarId, true);
            } else {
                updateCalendarIdsTemp(holder.calendarId, false);
            }
            break;
        }
        case R.id.calendar_dialog_list_item_layout: {
            Holder holder = (Holder) view.getTag();
            if (holder.checkbox.isChecked()) {
                holder.checkbox.setChecked(false);
                updateCalendarIdsTemp(holder.calendarId, false);
            } else {
                holder.checkbox.setChecked(true);
                updateCalendarIdsTemp(holder.calendarId, true);
            }
            break;
        }
        }
        notifyDataSetChanged();
    }

    /**
     * This method either adds or removes a calendar ID from
     * {@link mSelectedCalendarIdsTemp}. If id equals to
     * {@link ALL_CALENDARS_ID}, and add is false
     * {@link mSelectedCalendarIdsTemp} is cleared.
     *
     * @param id
     *            - the calendar id
     * @param add
     *            - true if id has to be added, false otherwise
     */
    public void updateCalendarIdsTemp(Integer id, boolean add) {
        if (LOG_INFO) {
            Log.i(TAG, "updateCalendarIdsTemp add = " + add + " id " + id);
        }
        if (id == null) {
            return;
        }
        if (add) {
            // size - 2 is done because if user selects each calendar one by one
            // then All Calendars item shall get checked automatically.
            // mCalendarAttrs stores attributes for All Calendars item also
            // hence size - 2 is checked.
            if (id.equals(ALL_CALENDARS_ID)
                    || (mSelectedCalendarIdsTemp.size() == mCalendarsAttrs
                        .size() - 2)) {
                for (CalendarAttributes attrs : mCalendarsAttrs) {
                    if (!mSelectedCalendarIdsTemp.contains(attrs.calendarId)) {
                        mSelectedCalendarIdsTemp.add(Integer
                                                     .valueOf(attrs.calendarId));
                    }
                }
            } else {
                if (!mSelectedCalendarIdsTemp.contains(id)) {
                    mSelectedCalendarIdsTemp
                    .add(Integer.valueOf(id.intValue()));
                }
            }
            mObserver.onItemChecked();
        } else {
            if (id.equals(ALL_CALENDARS_ID)) {
                mSelectedCalendarIdsTemp.clear();
                mObserver.onAllItemsUnChecked();
            } else {
                mSelectedCalendarIdsTemp.remove(id);
                mSelectedCalendarIdsTemp.remove(ALL_CALENDARS_ID);
                if (mSelectedCalendarIdsTemp.isEmpty()) {
                    mObserver.onAllItemsUnChecked();
                }
            }
        }
        if (LOG_DEBUG) {
            for (Integer selectedId : mSelectedCalendarIdsTemp) {
                Log.d(TAG, "updateCalendarIdsTemp selectedId = " + selectedId);
            }
        }
    }

    /**
     * Copies IDs of selected calendars from {@link mSelectedCalendarIdsTemp} to
     * {@link mSelectedCalendarIds} and changes the mode to normal mode
     *
     * @return ArrayList holding the IDs of selected calendars
     */
    public ArrayList<Integer> operationCompleted() {
        mTempMode = false;
        mSelectedCalendarIds.clear();
        for (Integer id : mSelectedCalendarIdsTemp) {
            mSelectedCalendarIds.add(Integer.valueOf(id.intValue()));
        }
        return mSelectedCalendarIds;
    }

    /**
     * Sets the mode to normal mode and refreshes the list
     */
    public void dialogCancelled() {
        mTempMode = false;
        notifyDataSetChanged();
    }

    /**
     * Returns the display name for selected calendars
     *
     * @param context
     *            - application's context
     *
     * @return the display name of selected calendars. If selected calendar is
     *         not found, "0" is returned
     */
    public final String getSelectedCalendarName(Context context) {
        if (mSelectedCalendarIds.contains(ALL_CALENDARS_ID)) {
            return mCalendarsAttrs.get(0).calendarDisplayName;
        } else if (mSelectedCalendarIds.size() == 1) {
            int id = mSelectedCalendarIds.get(0);
            for (CalendarAttributes attrs : mCalendarsAttrs) {
                if (attrs.calendarId == id) {
                    return attrs.calendarDisplayName;
                }
            }
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(mSelectedCalendarIds.size()).append(SPACE)
                    .append(context.getString(R.string.selected));
            return builder.toString();
        }
        return String.valueOf(0);
    }

    /**
     * This method returns the number of calendars selected by user in calendars
     * dialog. This method shall be called only after onInitComplete has been
     * called on observer
     *
     * @return - the size of mSelectedCalendarIds. This can be zero if calendar
     *         selected by user is either deleted or hided
     */
    public final int getNumberOfSelectedCalendars() {
        return mSelectedCalendarIds.size();
    }

    /**
     * Method to find out if adapter has been initialized or not
     *
     * @return
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * This method reinitializes the adapter
     *
     * @param context
     *            - application's context
     * @param selectedCalendarIds
     *            - the list of selected calendar ids
     */
    public void reinitialize(Context context,
            ArrayList<Integer> selectedCalendarIds) {
        mPreSelectedCalendarIds = selectedCalendarIds;
        mCalendarsAttrs.clear();
        mCalendarsAttrs.add(new CalendarAttributes(ALL_CALENDARS_ID, 0,
                mResources.getString(R.string.all_calendars), ""));
        Thread workerThread = new Thread(new WorkerRunnable(context));
        workerThread.start();
    }

}
