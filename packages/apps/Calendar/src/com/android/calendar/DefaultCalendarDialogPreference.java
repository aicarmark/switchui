package com.android.calendar;

import com.android.calendar.R;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ListView;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.RadioButton;
import android.content.DialogInterface.OnCancelListener;
import android.text.TextUtils;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract;

public class DefaultCalendarDialogPreference extends DialogPreference {

    private final String TAG = "DefaultCalendarDialogPreference";
    public final static int NONE_DEFAULT_CALENDAR = -1;
    private Context mContext;
    private int mClickedDialogEntryIndex = 0;
    private List<CalendarAccountItem> mCalendarsArray;

    private static final String[] PROJECTION = new String[] {
            Calendars._ID, // 0
            Calendars.CALENDAR_DISPLAY_NAME, // 1
            Calendars.VISIBLE, // 2
            Calendars.ACCOUNT_NAME, // 3
            Calendars.ACCOUNT_TYPE // 4

    };

    // for query
    private static final int CALENDARS_INDEX_ID = 0;

    private static final int CALENDARS_INDEX_DISPLAY_NAME = 1;

    private static final int CALENDARS_INDEX_SELECT = 2;

    private static final int CALENDARS_INDEX_SYNC_ACCOUNT = 3;

    private static final int CALENDARS_INDEX_SYNC_ACCOUNT_TYPE = 4;

    private static final String CALENDARS_WHERE = Calendars.CALENDAR_ACCESS_LEVEL + ">="
            + Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " + Calendars.VISIBLE
            + "=1";

    // inner class for calendar account
    public static class CalendarAccountItem {
        public final String name;
        public final String account;
        public final int id;
        public final String type;

        public CalendarAccountItem(int id, String name, String account,
                String type) {
            this.id = id;
            this.name = name;
            this.account = account;
            this.type = type;
        }
    }

    public DefaultCalendarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public DefaultCalendarDialogPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        Cursor calendarsCursor = null;

        calendarsCursor = mContext.getContentResolver().query(
                Calendars.CONTENT_URI, PROJECTION, CALENDARS_WHERE, null, null);
        if (calendarsCursor != null) {
            try {
                mCalendarsArray = populateCalendars(calendarsCursor);
                if (mCalendarsArray.size() > 0) {
                    final ArrayAdapter<CalendarAccountItem> accountAdapter = createDefaultCalendarAccountAdapter(
                            mContext, mCalendarsArray);

                    builder.setSingleChoiceItems(accountAdapter,
                            mClickedDialogEntryIndex,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    mClickedDialogEntryIndex = which;
                                    saveDefaultCalendarInfo();
                                    dialog.dismiss();
                                }
                            });
                    builder.setPositiveButton(null, null);
                } else {// no valid calendars
                    builder.setTitle(R.string.no_syncable_calendars)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setMessage(R.string.no_calendars_found)
                            .setPositiveButton(android.R.string.ok, null);
                }
            } finally {
                calendarsCursor.close();
            }
        } else {// No Calendar
            builder.setTitle(R.string.no_syncable_calendars)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.no_calendars_found)
                    .setPositiveButton(android.R.string.ok, null);
        }
    }

    // get the first valid calendars account
    public static CalendarAccountItem getFirstValidCalAccount(Context context) {
        Cursor cursor = null;
        CalendarAccountItem defaultCal = null;
        cursor = context.getContentResolver().query(Calendars.CONTENT_URI,
                PROJECTION, CALENDARS_WHERE, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String type = cursor
                            .getString(CALENDARS_INDEX_SYNC_ACCOUNT_TYPE);
                    String account = null;
                    if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(type)) {
                        // Phone storage calendar
                        account = context.getString(R.string.phone_storage);
                    } else {
                        account = cursor
                                .getString(CALENDARS_INDEX_SYNC_ACCOUNT);
                    }
                    defaultCal = new CalendarAccountItem(
                            cursor.getInt(CALENDARS_INDEX_ID),
                            cursor.getString(CALENDARS_INDEX_DISPLAY_NAME),
                            account, type);

                }
            } finally {
                cursor.close();
            }
        }
        return defaultCal;
    }

    // get calendar account
    public static CalendarAccountItem getCalAccount(Context context,
            String account, String type, int id) {

        if (TextUtils.isEmpty(account)) {
            return null;
        }
        Cursor cursor = null;
        String where = CALENDARS_WHERE + " AND " + Calendars.ACCOUNT_TYPE
                + "=?" + " AND " + Calendars._ID + "=" + id;
        boolean isLocal = CalendarContract.ACCOUNT_TYPE_LOCAL.equals(type);
        CalendarAccountItem accountItem = null;
        String phoneStorageStr = context.getString(R.string.phone_storage);

        cursor = context.getContentResolver().query(Calendars.CONTENT_URI,
                PROJECTION, where, new String[] { type }, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String name = cursor
                            .getString(CALENDARS_INDEX_DISPLAY_NAME);
                    String accountName = cursor
                            .getString(CALENDARS_INDEX_SYNC_ACCOUNT);
                    if (isLocal) {
                        accountItem = new CalendarAccountItem(id, name,
                                phoneStorageStr, type);
                    } else {
                        if (account.equals(accountName)) {
                            accountItem = new CalendarAccountItem(id, name,
                                    account, type);
                        }
                    }
                    Log.d("DefaultCalendarDialogPreference",
                            "getCalAccount result: " + account + " id: " + id);
                }
            } finally {
                cursor.close();
            }
        }
        return accountItem;
    }

    // save default calendar info into calendar preference
    private void saveDefaultCalendarInfo() {
        SharedPreferences prefs = GeneralPreferences
                .getSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        CalendarAccountItem calAccount = mCalendarsArray
                .get(mClickedDialogEntryIndex);

        if (calAccount != null) {
            Log.d(TAG, "Selected account: " + calAccount.account + " id: "
                    + calAccount.id);
            GeneralPreferences.setDefaultCalendarPreferenceValue(editor,
                    calAccount.id, calAccount.account, calAccount.type);
        }
    }

    // get all visible calendars info
    private List<CalendarAccountItem> populateCalendars(Cursor calCursor) {

        List<CalendarAccountItem> calendarsArray = new ArrayList<CalendarAccountItem>();
        String none = mContext.getString(R.string.default_calendar_none_string);
        if (calCursor != null && calCursor.moveToFirst()) {
            // None
            CalendarAccountItem noneItem = new CalendarAccountItem(-1, none,
                    mContext.getString(R.string.no_default_calendars_summary),
                    none);
            calendarsArray.add(noneItem);
            // Other Calendars
            do {
                // Calendar account
                String account = calCursor
                        .getString(CALENDARS_INDEX_SYNC_ACCOUNT);
                // Calendar name
                String name = calCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                // Calendar id
                int id = calCursor.getInt(CALENDARS_INDEX_ID);
                // Calendar type
                String type = calCursor
                        .getString(CALENDARS_INDEX_SYNC_ACCOUNT_TYPE);

                // Phone storage calendar
                if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(type)) {
                    account = mContext.getString(R.string.phone_storage);
                    name = mContext.getString(R.string.phone_calendar_name);
                }
                calendarsArray.add(new CalendarAccountItem(id, name, account, type));

            } while (calCursor.moveToNext());
        }
        return calendarsArray;
    }

    // create the calendar array list adapter
    private final ArrayAdapter<CalendarAccountItem> createDefaultCalendarAccountAdapter(
            final Context context, List<CalendarAccountItem> accounts) {
        final LayoutInflater dialogInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);

        if (prefs != null) {// get default calendar account form preference
            String defaultAccount = prefs.getString(
                   GeneralPreferences.KEY_DEFAULT_CALENDAR_ACCOUNT, null);
            String type = prefs.getString(
                    GeneralPreferences.KEY_DEFAULT_CALENDAR_TYPE, null);
            int id = prefs.getInt(GeneralPreferences.KEY_DEFAULT_CALENDAR_ID,
                    NONE_DEFAULT_CALENDAR);
            int i = 0;
            for (CalendarAccountItem tempAccount : accounts) {
                // get current index of account list
                if (defaultAccount != null && id == tempAccount.id
                        && defaultAccount.equals(tempAccount.account)
                        && type != null && type.equals(tempAccount.type)) {
                    mClickedDialogEntryIndex = i;
                    break;
                }
                i++;
            }
        }
        // bind view
        final ArrayAdapter<CalendarAccountItem> accountAdapter = new ArrayAdapter<CalendarAccountItem>(
                context, R.layout.calendars_default_calendar_item, accounts) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(
                            R.layout.calendars_default_calendar_item, parent,
                            false);
                }
                //change name
                TextView calName = (TextView) convertView
                        .findViewById(R.id.calendar_name);
                TextView accountName = (TextView) convertView
                        .findViewById(R.id.account_name);
                RadioButton radio = (RadioButton) convertView
                        .findViewById(R.id.radio);
                CalendarAccountItem account = this.getItem(position);

                radio.setChecked((position == mClickedDialogEntryIndex));
                calName.setText(account.name);
                accountName.setText(account.account);

                return convertView;
            }
        };
        return accountAdapter;
    }
}
