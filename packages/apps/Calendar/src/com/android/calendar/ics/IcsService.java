package com.android.calendar.ics;

import com.android.calendar.R;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.ContentResolver;
import android.content.Intent;
import android.app.IntentService;
import android.text.TextUtils;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.widget.Toast;
import android.net.Uri;
import android.util.Log;
import android.util.Config;
import android.content.Context;
import android.os.Looper;
import android.provider.BaseColumns;
import android.content.ContentUris;
import android.content.res.Resources.NotFoundException;
import android.content.ActivityNotFoundException;

import com.motorola.calendarcommon.vcal.common.VCalConstants;
import com.motorola.calendarcommon.vcal.common.Duration;
import com.motorola.calendarcommon.vcal.common.CalendarEvent;
import com.motorola.calendarcommon.vcal.VCalUtils;
import com.android.calendar.AllInOneActivity;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.Utils;

public class IcsService extends IntentService {
    private static final String TAG = "IcsService";
    private static final boolean DEVELOPMENT = Config.DEBUG;
    private ContentResolver mContentResolver;
    private Handler mHandler;

    private static final String VTODO_ACTION_VIEW = "com.motorola.tasks.action.VIEW_VTODO";

    public IcsService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContentResolver = getContentResolver();
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (null == intent) return;

        String action = intent.getAction();
        if (DEVELOPMENT) {
            Log.v(TAG, "Received action " + action);
        }

        if (action != null) {
            if (action.equals(Intent.ACTION_VIEW)) {
                viewIcs(intent);
            }
        }
    }

    /**
     * Returns the handler we use for displaying toasts.
     * @return The handler instance, creating it if necessary.
     */
    protected Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /**
     * Displays a Toast with the message
     * @param context The Context to execute it.
     * @param messageId The resource ID of the message to display.
     */
    private void showToast(final Context context, final int messageId) {
        // Run it in the UI thread because this service is short-lived.
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
                } catch (NotFoundException e) {
                    Log.w(TAG, "Couldn't find resource toast ID " + messageId);
                }
            }
        });
    }

    /**
     * Show the Calendar UI.
     * @param eventCursor A Cursor on the event we're interested in.
     * @return true if success<br>
     * false if failure
     */
    private boolean invokeCalendarUI(final Cursor eventCursor, final Bundle extras) {
        boolean isSuccess = false;
        final long eventId = eventCursor.getLong(
                eventCursor.getColumnIndexOrThrow(BaseColumns._ID));

        if (DEVELOPMENT) {
            Log.v(TAG, "Intent for Calendar UI MeetingResponse display" +
                    " of Events table row _ID " + Long.valueOf(eventId));
        }


        long startMillis = eventCursor.getLong(
                eventCursor.getColumnIndexOrThrow(Events.DTSTART));
        long endMillis = eventCursor.getLong(
                eventCursor.getColumnIndexOrThrow(Events.DTEND));

        // recurring event doesn't have endMillis
        if (0 == startMillis) {
            Log.e(TAG, "ERROR Invalid start/end time (ms): " +
                    Long.valueOf(startMillis) +  " / " +  Long.valueOf(endMillis));
        } else {
            // for repeat event, end time will be set to 0 by Calenedar Content provider.
            // we need recalculate this value by using DTSTART + DURATION.
            if (endMillis < startMillis) {
                // give default value for 1 hour duration
                endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
                String strDuration = eventCursor.getString(
                        eventCursor.getColumnIndexOrThrow(Events.DURATION));
                if (!TextUtils.isEmpty(strDuration)) {
                    Duration duration = new Duration();
                    try {
                        duration.parse(strDuration);
                        endMillis = startMillis + duration.getMillis();
                    } catch (Exception e) {
                        //Do nothing here. The default value will be used.
                        Log.w(TAG, "Couldn't get EndTime of the event. Set it to StartTime + 1hour.");
                    }
                }
            }
            // intent to launch the event detail view
            Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);

            Intent showUI = new Intent(Intent.ACTION_VIEW, eventUri);
            showUI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            showUI.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
            showUI.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            boolean showEventInfoFullScreen =
                Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
            if (showEventInfoFullScreen) {
                showUI.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                showUI.setClass(this, EventInfoActivity.class);
            } else {
                showUI.setClass(this, AllInOneActivity.class);
            }
            if (extras != null) showUI.putExtras(extras);

            startActivity(showUI);
            isSuccess = true;
         }
         return isSuccess;
    }


    /**
     * Retrieves the ICS attachment described by the Intent data
     * and launches a viewer on the event corresponding to the embedded
     * meeting request.
     *
     * @param intent The Intent that addresses the attachment.
     */
    private void viewIcs(Intent intent) {
        final Uri fileUri = intent.getData();
        if (fileUri == null) {
            if (DEVELOPMENT) {
                Log.v(TAG, "No file to be viewed");
            }
            return;
        }

        CalendarEvent vcalEvent = null;
        try {
            vcalEvent = VCalUtils.parseVcal(this, fileUri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to open the file " + fileUri.toString());
        } catch (IOException e) {
            Log.e(TAG, "I/O error when reading the file " + fileUri.toString());
        }
        if (vcalEvent == null) return;

        if (vcalEvent.isVtodo) {
            if (DEVELOPMENT) {
                Log.v(TAG, "Vtodo type");
                Log.v(TAG, "fileUri is " + fileUri.toString());
            }
            Intent taskIntent = new Intent(VTODO_ACTION_VIEW);
            taskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            taskIntent.addFlags(intent.getFlags());
            taskIntent.setDataAndType(intent.getData(), intent.getType());
            try {
                startActivity(taskIntent);
            } catch (ActivityNotFoundException noActivity) {
                Log.e(TAG, "No Activity is available to view vToDo event");
            }
            return;
        }

        if (DEVELOPMENT) {
            Log.v(TAG, "ICS UID (key): " + vcalEvent.uid);
        }

        Uri eventUri = VCalUtils.pushIntoScratchCalendar(this, vcalEvent);
        if (eventUri != null) {
            Uri.Builder uriBuilder = eventUri.buildUpon();
            // TODO: Consolidate the string constants
            uriBuilder.appendQueryParameter("use_hidden_calendar", String.valueOf(true));
            eventUri = uriBuilder.build();

            Cursor eventCursor = mContentResolver.query(eventUri, null, null, null, null);
            if (eventCursor != null) {
                try {
                    if (eventCursor.moveToFirst()) {
                        Bundle extras = new Bundle();
                        extras.putBoolean(IcsConstants.EXTRA_IMPORT_ICS, true);
                        invokeCalendarUI(eventCursor, extras);
                    }
                } finally {
                    eventCursor.close();
                }
            } else {
                Log.e(TAG, "onStart: Couldn't obtain cursor on Events table.");
            }
        } else {
            Log.e(TAG, "Unknown error while viewing ICS file.");
        }
    }
}
