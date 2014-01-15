package com.motorola.calendarcommon.vcal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.TimeZone;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.io.FileNotFoundException;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ParcelFileDescriptor;
import com.android.calendarcommon.ICalendar;
import com.android.calendarcommon.ICalendar.FormatException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.ExtendedProperties;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.Attendees;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.graphics.Color;
import android.util.Log;
import android.util.Config;
import android.accounts.Account;

import com.motorola.calendarcommon.vcal.common.VCalConstants;
import com.motorola.calendarcommon.vcal.common.CalendarEvent;
import com.motorola.calendarcommon.vcal.common.CalendarEvent.Attendee;
import com.motorola.calendarcommon.vcal.common.CalendarEvent.Alarm;
import com.motorola.calendarcommon.vcal.common.CalendarEvent.ExtProp;
import com.motorola.calendarcommon.vcal.parser.VCalParser;
import com.motorola.calendarcommon.vcal.parser.VCalParser_V1;
import com.motorola.calendarcommon.vcal.parser.VCalParser_V2;
import com.motorola.calendarcommon.vcal.composer.VCalComposer;
import com.motorola.calendarcommon.vcal.composer.VCalComposer_V1;
import com.motorola.calendarcommon.vcal.composer.VCalComposer_V2;

/**
 * This class provides Utilties to parse and compose vCal files
 * @hide
 **/
public class VCalUtils {

    public static final String TAG = "VCalUtils";
    private static final boolean DEVELOPMENT = Config.DEBUG;
    public static final String VCAL_MIME_TYPE = "text/calendar";
    public static final String SCRATCH_CALENDAR = "__ScrAtCh_CaLeNDaR__";
    public static final String SCRATCH_CALENDAR_NAME = "Local iCalendar";

    public static final int ERROR_CODE_SUCCESS = 1;
    public static final int ERROR_CODE_UNKNOWN = -2;
    public static final int ERROR_CODE_NO_SDCARD = -3;
    public static final int ERROR_CODE_SDCARD_ACCESS_FAILURE = -4;
    public static final int ERROR_CODE_VCAL_COMPOSE_FAILURE = -5;
    private static final String VCARD_PARAM_SEPARATOR = ";";
    private static final String VCARD_PARAM_ENCODING_QP = "ENCODING=QUOTED-PRINTABLE";
    private static final String VCARD_PARAM_CHARSET = "CHARSET=UTF-8";
    private Uri mSavedFileUri = null;

    private static final String[] CALENDARS_PROJECTION = new String[] {
        Calendars._ID, // 0
    };

    private static final int CALENDARS_INDEX_ID = 0;

    public static final String VCAL_SD_DIR = "/Calendar/";

    // The iCal account info.
    private static final String ICAL_ACCOUNT_TYPE = "com.motorola.icalendar.UNCONNECTED_ACCOUNT";
    private static final String ICAL_ACCOUNT_NAME = "iCalendar";
    private static final Account ICAL_ACCOUNT_INFO = new Account(ICAL_ACCOUNT_NAME, ICAL_ACCOUNT_TYPE);

    //////////////////////////////////////////////////////
    // Add Pubulic API in following section
    /////////////////////////////////////////////////////

    /**
     * Constructor
     */
    public VCalUtils() {
    }

    public static String encodeQuotedPrintable(final String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        int index = 0;
        int lineCount = 0;
        byte[] strArray = null;

        try {
            strArray = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Charset " + "UTF-8" + " cannot be used. "
                    + "Try default charset");
            strArray = str.getBytes();
        }
        builder.append(VCARD_PARAM_SEPARATOR);
        builder.append(VCARD_PARAM_ENCODING_QP);
        builder.append(VCARD_PARAM_SEPARATOR);
        builder.append(VCARD_PARAM_CHARSET);
        builder.append(':');
        while (index < strArray.length) {
            builder.append(String.format("=%02X", strArray[index]));
            index += 1;
            lineCount += 3;

            if (lineCount >= 67) {
                // Specification requires CRLF must be inserted before the
                // length of the line
                // becomes more than 76.
                // Assuming that the next character is a multi-byte character,
                // it will become
                // 6 bytes.
                // 76 - 6 - 3 = 67
                builder.append("=\r\n");
                lineCount = 0;
            }
        }
        return builder.toString();
    }

    /**
     * Create vCalendar data stream based on the event id
     *
     * @param context
     *        Appliction context the application is running in
     *
     * @param eventId
     *        Id of the event to be composed
     *
     * @param version
     *        Vcal version requested, must be 1.0 or 2.0
     *
     * @return returns vcal data in utf-8 format,returns null
     *         if input parameters are invalid
     */
    public byte[] composeVcal(final Context context, long eventId, String version) {
        if (eventId <= 0 || context == null || version == null){
            return null;
        }

        if (DEVELOPMENT) {
            Log.v(TAG,"Event id :"+ eventId);
        }

        if(version.equals(VCalParser.VCAL1) == false &&
                version.equals(VCalParser.VCAL2) == false){
            if (DEVELOPMENT) {
                Log.v(TAG,"invalid version or vcard");
            }
            return null;
        }

        byte[] data = null;
        ContentResolver cr = context.getContentResolver();
        Uri record_uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        Cursor _cursor = cr.query(record_uri, null, null, null, null);
        if (_cursor != null) {
            try {
                if (_cursor.moveToFirst()) {
                    CalendarEvent calEvent = getCalendarEvent(_cursor, cr);
                    if (calEvent != null) {
                        data = composeVcal(calEvent, version);
                    }
                }
            } catch (Exception ex) {
                Log.w(TAG, "Failed to compose vcal", ex);
            } finally {
                _cursor.close();
            }
        }

        return data;
    }


    /**
     * Create vCalendar data stream based on the input calendar event
     *
     * @param calEvent
     *        based on which vCal data stream will be generated
     *
     * @param version vCal version requested, must be 1.0 or 2.0
     *
     * @return the data stream of the vCal event with utf8 format
     */
    public static byte[] composeVcal(final CalendarEvent calEvent, String version) {
        if (version == null || calEvent == null) {
            return null;
        }

        VCalComposer composer = null;

        if (VCalParser.VCAL1.equals(version)) {
            composer = new VCalComposer_V1(calEvent);
        } else if (VCalParser.VCAL2.equals(version)) {
            composer = new VCalComposer_V2(calEvent);
        } else if (DEVELOPMENT) {
            Log.e(TAG, "version error, unsupported version :" + version);
        }

        if (composer != null) {
            return composer.composeVCal();
        }

        return null;
    }

    /**
     * Parse vCalendar data stream. Input stream should be in utf8 format.
     *
     * @param is
     *        Input data stream which contains vCal data
     *
     * @return CalendarEvent
     *         Object which contains event info or null if failed to parse
     *
     * @throws IOException
     *         If input stream is closed or some other I/O error occurs.
     */
    public static CalendarEvent parseVcal(InputStream is) throws IOException {
        CalendarEvent calEvent = null;
        try {
            String data = getUnfoldedText(is);
            if (data == null) {
                return null;
            }

            ICalendar.Component vCalendar = ICalendar.parseCalendar(data);
            if (vCalendar != null) {
                ICalendar.Property versionProp = vCalendar.getFirstProperty("VERSION");
                if (versionProp == null) {
                    versionProp = vCalendar.getFirstProperty("version");
                }
                if (versionProp != null) {
                    VCalParser parser = null;
                    String version = versionProp.getValue();
                    if (version != null) {
                        version = version.trim();
                        if (version.equals(VCalParser.VCAL1)) {
                            parser = new VCalParser_V1();
                        } else if (version.equals(VCalParser.VCAL2)) {
                            parser = new VCalParser_V2();
                        } else if (DEVELOPMENT) {
                            Log.v(TAG,"unsupported version");
                        }
                    }

                    if (parser != null) {
                        Collection<CalendarEvent> event = parser.parse(vCalendar);
                        if (event != null) {
                             Iterator<CalendarEvent> it = event.iterator();
                             while (it.hasNext()) {
                                 calEvent = it.next();
                                 break;
                             }
                         }
                    }
                }
            }
        } catch (FormatException e) {
            e.printStackTrace();
        }

        return calEvent;
    }

    /**
     * Parse the vCal file into an CalendarEvent object. File need have utf8 encoding.
     *
     * @param context
     *        Application context in which the application is running
     *
     * @param fileUri
     *        Uri for the file to be parsed
     *
     * @return CalendarEvent
     *         Object which contains the event info or null if failed to parse
     *
     * @throws FileNotFoundException
     *         If the provided URI could not be opened
     *
     * @throws IOException
     *         If there's error when reading the input file
     */
    public static CalendarEvent parseVcal(final Context context, final Uri fileUri)
        throws FileNotFoundException, IOException {
        if (context != null && fileUri != null) {
            ContentResolver resolver = context.getContentResolver();
            InputStream in = resolver.openInputStream(fileUri);
            if (in != null) {
                try {
                    CalendarEvent calEvent = parseVcal(in);
                    if (calEvent == null) {
                        Log.e(TAG, "Couldn't parse ICS file from uri="
                                + fileUri.toString());
                    }
                    return calEvent;
                } finally {
                    in.close();
                }
            }
        } else {
            Log.e(TAG, "context or file Uri is null");
        }

        return null;
    }


    /**
     * Insert calendar event into the scratch calendar
     *
     * @param context
     *        Application context the application is running in
     *
     * @param calEvent
     *        calendar event to be inserted
     *
     * @return the Uri of the newly created row or null if failed
     */
    public static Uri pushIntoScratchCalendar(final Context context, final CalendarEvent calEvent) {
        ContentResolver resolver = context.getContentResolver();
        //get scratch calendar URI
        final Uri scratchCalURI = getScratchCalendarByName(context,
                SCRATCH_CALENDAR, SCRATCH_CALENDAR_NAME);
        if (scratchCalURI !=  null) {
            //get a cursor over it
            Cursor scratchCalCursor = resolver.query(scratchCalURI, CALENDARS_PROJECTION,
                    null, null, null);
            if (scratchCalCursor != null) {
                try {
                    if (scratchCalCursor.moveToFirst()) {
                        return createEventInDb(resolver, scratchCalCursor, calEvent);
                    }
                } finally {
                    scratchCalCursor.close();
                }
            }
        }

        return null;
    }

    /**
     * Insert a calendar event into the calendar database
     *
     * @param context
     *        Application Context the application is running in
     *
     * @param calendarId
     *        Id of the calendar account into which the event will be inserted
     *
     * @param CalendarEvent
     *        Calendar event to be inserted
     *
     * @return the Uri of the newly created row or null if failed
     */
    public static Uri insertEventToDb(final Context context, int calendarId,
            final CalendarEvent calEvent) {
        if (context == null || calendarId <= 0 || calEvent == null) {
            return null;
        }
        ContentResolver cr = context.getContentResolver();

        Uri calUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId);
        Cursor c = cr.query(calUri, CALENDARS_PROJECTION, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return createEventInDb(cr, c, calEvent);
                } else if (DEVELOPMENT) {
                    Log.v(TAG, "Can't access calendar account with Id " + calendarId);
                }
            } finally {
                c.close();
            }
        } else if (DEVELOPMENT) {
            Log.v(TAG, "Can't access calendar account with Id " + calendarId);
        }

        return null;
    }


    /**
     * Export an event from calendar event table to sdcard.
     * The event will be encoded to vCal-2.0 format and saved with utf8 encoding.
     * If saving successfully, getSavedFileUri can be called to get the file uri
     * of the newly saved event.
     *
     * @param context
     *        Application context the application is running in
     *
     * @param eventId
     *        Unique ID of the event to be exported in the calendar event table.
     *
     * @param dirName
     *        Direcotry under which the exported event will be saved. If null,
     *        the event will be saved to the application package direcotry.
     *
     * @param fileName
     *        File name for the saved event. If null, the checksum of the content
     *        will be used as file name.
     *
     * @return VCalUtils.ERROR_CODE_SUCCESS if success
     *         VCalUtils.ERROR_CODE_VCAL_COMPOSE_FAILURE if failed to compose the event
     *         to vCal-2.0 format with utf8 encoding.
     *         VCalUtils.ERROR_CODE_NO_SDCARD if the SDCARD is not mounted with Readable
     *         and Writtable permission.
     *         VCalUtils.ERROR_CODE_SDCARD_ACCESS_FAILURE if cannot create the file on
     *         sdcard.
     */
    public int exportEventToSDCard(final Context context, long eventId, String dirName,
            String fileName) {
        return exportEventToSDCard(context, eventId, dirName, fileName, VCalParser.VCAL1);
    }

    /**
     * Export an event from calendar event table to sdcard.
     * If saving successfully, getSavedFileUri can be called to get the file uri
     * of the newly saved event.
     *
     * @param context
     *        Application context the application is running in
     *
     * @param eventId
     *        Unique ID of the event to be exported in the calendar event table.
     *
     * @param dirName
     *        Direcotry under which the exported event will be saved. If null,
     *        the event will be saved to the application package direcotry.
     *
     * @param fileName
     *        File name for the saved event. If null, the checksum of the content
     *        will be used as file name.
     *
     * @param vCalVersion
     *        vCal version the event will be saved, must be 1.0 or 2.0
     *
     * @return VCalUtils.ERROR_CODE_SUCCESS if success
     *         VCalUtils.ERROR_CODE_VCAL_COMPOSE_FAILURE if failed to compose the event
     *         to vCal format with utf8 encoding.
     *         VCalUtils.ERROR_CODE_NO_SDCARD if the SDCARD is not mounted with Readable
     *         and Writtable permission.
     *         VCalUtils.ERROR_CODE_SDCARD_ACCESS_FAILURE if cannot create the file on
     *         sdcard.
     */
    public int exportEventToSDCard(final Context context, long eventId, String dirName,
            String fileName, String vCalVersion) {
        byte[] content = composeVcal(context, eventId, vCalVersion);
        if (content == null || content.length == 0) {
            Log.e(TAG, "exportToSDCard, failed to compose VCAL data");
            return ERROR_CODE_VCAL_COMPOSE_FAILURE;
        }

//        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//            return ERROR_CODE_NO_SDCARD;
//        }
        int result;
        try {
            mSavedFileUri = saveVCalToFile(context, content, dirName, fileName);
        } catch (IOException e) {
            mSavedFileUri = null;
        }

        if (mSavedFileUri == null) {
            result = ERROR_CODE_SDCARD_ACCESS_FAILURE;
        } else {
            result = ERROR_CODE_SUCCESS;
        }

        return result;
    }

    /**
     * Return the file uri of the saved event which has been exported to sdcard
     * most recently. This method takes effect only after exportToSDCard has been
     * called. Or it will always return null. It only retains the most recent value.
     */
    public Uri getSavedFileUri() {
        // Return a copy instead of returning the private data
        return (mSavedFileUri == null) ? null : mSavedFileUri.buildUpon().build();
    }


    /**
     * Converts vCal-2.0 to vCal-1.0
     *
     * @param context
     *        Application context the application is running in
     *
     * @param fileUri
     *        File Uri for the vCal-2.0 file
     *
     * @return new file Uri for the converted vCal-1.0 file or null if failed to
     *         convert
     *
     * @throws FileNotFoundException
     *         If the provided URI could not be opened.
     *
     * @throws IOException
     *         If there are I/O errors
     */
    public static Uri vCalV2ToV1(final Context context, final Uri fileUri)
        throws FileNotFoundException, IOException {
        if (context == null || fileUri == null) {
            return null;
        }

        Uri v1Uri = null;
        try {
            InputStream in = context.getContentResolver().openInputStream(fileUri);
            if (in != null) {
                try {
                    String data = getUnfoldedText(in);
                    if (data == null) {
                        return null;
                    }

                    ICalendar.Component vCalendar = ICalendar.parseCalendar(data);
                    if (vCalendar != null) {
                        ICalendar.Property versionProp = vCalendar.getFirstProperty("VERSION");
                        if (versionProp == null) {
                            versionProp = vCalendar.getFirstProperty("version");
                        }
                        if (versionProp != null) {
                            String version = versionProp.getValue();
                            if (version != null) {
                                version = version.trim();
                                if (version.equals(VCalParser.VCAL1)) {
                                    // Return directly if it's already an V1 file
                                    v1Uri = fileUri;
                                } else if (version.equals(VCalParser.VCAL2)) {
                                    VCalParser parser = new VCalParser_V2();
                                    Collection<CalendarEvent> event = parser.parse(vCalendar);
                                    if (event != null) {
                                         Iterator<CalendarEvent> it = event.iterator();
                                         while (it.hasNext()) {
                                             byte[] content = composeVcal(it.next(), VCalParser.VCAL1);
                                             String fileName = fileUri.getLastPathSegment();
                                             int index = fileName.lastIndexOf('.');
                                             if (index > 0) {
                                                 fileName = new StringBuilder(fileName.substring(0, index))
                                                     .append("_v1.vcs").toString();
                                             } else {
                                                 fileName = fileName + "_v1.vcs";
                                             }
                                             if (DEVELOPMENT) {
                                                 Log.v(TAG, "fileName for vCal-1.0 is " + fileName);
                                             }
                                             v1Uri = saveVCalToFile(context, content, null, fileName);
                                             break;
                                         }
                                     }
                                } else if (DEVELOPMENT) {
                                    Log.v(TAG,"unsupported version");
                                }
                            }
                        }
                    }
                } finally {
                    in.close();
                }
            }
        } catch (FormatException e) {
            e.printStackTrace();
        }

        return v1Uri;
    }


    //////////////////////////////////////////////////////
    // Add Private API in following section
    /////////////////////////////////////////////////////

    /**
    * append operationList with Extended properties
    */
    private static void appendExtentedBuilder(
            ArrayList<ContentProviderOperation> operationList,
            Vector<ExtProp> exts, String eventUid) {
        // Change for icecream
        boolean needInsertUID = true;
        ContentProviderOperation.Builder builder;

        // Pretend that we're sync adapter since only sync adapters can operate extended properties
        Uri uri = ExtendedProperties.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, String.valueOf(true))
                .appendQueryParameter(Calendars.ACCOUNT_NAME, ICAL_ACCOUNT_NAME)
                    .appendQueryParameter(Calendars.ACCOUNT_TYPE, ICAL_ACCOUNT_TYPE).build();
        if ((exts != null) && (exts.size() > 0)) {
            int size = exts.size();
            for (int i = 0; i < size; i++) {
                String name = exts.get(i).name;
                String value = exts.get(i).value;
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                    builder = ContentProviderOperation.newInsert(uri);
                    builder.withValueBackReference(ExtendedProperties.EVENT_ID, 0); // event_id
                    builder.withValue(ExtendedProperties.NAME, name); // name
                    builder.withValue(ExtendedProperties.VALUE, value); // value
                    operationList.add(builder.build());

                    if (VCalConstants.UID.equals(name)) {
                        needInsertUID = false;
                    }
                }
            }
        }

        if (needInsertUID && !TextUtils.isEmpty(eventUid)) {
            builder = ContentProviderOperation.newInsert(uri);
            builder.withValueBackReference(ExtendedProperties.EVENT_ID, 0); // event_id
            builder.withValue(ExtendedProperties.NAME, VCalConstants.UID); // name
            builder.withValue(ExtendedProperties.VALUE, eventUid); // value
            operationList.add(builder.build());
        }
    }

    /**
    * append operationList with alarms
    */
    private static void appendAlarmListBuilder(
            ArrayList<ContentProviderOperation> operationList,
            Vector<Alarm> alarms) {
        Iterator<CalendarEvent.Alarm> it = alarms.iterator();
        int size = alarms.size();
        if (size <= 0)
            return;

        ContentProviderOperation.Builder builder;
        while (it.hasNext()) {
            CalendarEvent.Alarm att = it.next();
            builder = ContentProviderOperation.newInsert(Reminders.CONTENT_URI);
            builder.withValueBackReference(Reminders.EVENT_ID, 0);// event_id
            builder.withValue(Reminders.METHOD, att.method); // method
            if (att.minutes >= 0) {
                builder.withValue(Reminders.MINUTES, att.minutes); // minutes
            } else {
                builder.withValue(Reminders.MINUTES, (-1)*att.minutes); // minutes
            }
            operationList.add(builder.build());
        }


    }

    /**
    * append operation list with attendees
    */
    private static void appendAttendeesBuilder(ArrayList<ContentProviderOperation> operationList,
            Vector<Attendee> attendees) {

        if (attendees == null || attendees.size() <= 0) {
            return;
        }

        ContentProviderOperation.Builder builder;
        int size = attendees.size();
        for (int i = 0; i < size; i++) {
            builder = ContentProviderOperation.newInsert(Attendees.CONTENT_URI);
            builder.withValueBackReference(Attendees.EVENT_ID, 0);

            builder.withValue(Attendees.ATTENDEE_NAME, attendees.get(i).name); // attendeeName
            builder.withValue(Attendees.ATTENDEE_EMAIL, attendees.get(i).email); // attendeeEmail
            builder.withValue(Attendees.ATTENDEE_STATUS, attendees.get(i).status);// attendeeStatus
            builder.withValue(Attendees.ATTENDEE_RELATIONSHIP, attendees.get(i).relationship);// attendeeRelationship
            builder.withValue(Attendees.ATTENDEE_TYPE, attendees.get(i).type); // attendeeType
            operationList.add(builder.build());
        }

    }

    /**
    * appends event table fields
    */
    private static ContentProviderOperation appendEventsBuilder(final int calendarId,
            final CalendarEvent calEvent) {

        ContentProviderOperation.Builder  builder =
            ContentProviderOperation.newInsert(Events.CONTENT_URI);

        ContentValues eventValues = new ContentValues();
        eventValues.put(Events.CALENDAR_ID, calendarId);

        if (!TextUtils.isEmpty(calEvent.organizer)) {
            eventValues.put(Events.ORGANIZER, calEvent.organizer);
        }

        // prevent guests from modifying the event
        eventValues.put(Events.GUESTS_CAN_MODIFY, 0);

        if ((calEvent.attendees != null) && (calEvent.attendees.size() > 0)) {
            eventValues.put(Events.HAS_ATTENDEE_DATA, 1);
        } else {
            eventValues.put(Events.HAS_ATTENDEE_DATA, 0);
        }

        if (calEvent.isTransparent)
            eventValues.put(Events.AVAILABILITY, Events.AVAILABILITY_FREE); // transparency
        else
            eventValues.put(Events.AVAILABILITY, Events.AVAILABILITY_BUSY); // transparency

        if (calEvent.dtEnd != CalendarEvent.FIELD_EMPTY_INT)
            eventValues.put(Events.DTEND, calEvent.dtEnd); // dtend
        if (calEvent.dtStart != CalendarEvent.FIELD_EMPTY_INT)
            eventValues.put(Events.DTSTART, calEvent.dtStart); // dtstart


        if (calEvent.isAllDay)
            eventValues.put(Events.ALL_DAY, 1); // allDay
        else
            eventValues.put(Events.ALL_DAY, 0); // allDay

        eventValues.put(Events.DESCRIPTION, calEvent.description); // description

        // Change for icecream
        if (!TextUtils.isEmpty(calEvent.rrule) || !TextUtils.isEmpty(calEvent.rdate)) {
            // for recurring event, we rely on duration rather than dtend.
            eventValues.remove(Events.DTEND);

            if (!TextUtils.isEmpty(calEvent.duration)) {
                eventValues.put(Events.DURATION, calEvent.duration); // duration
            } else {
                long end = calEvent.dtEnd;
                long start = calEvent.dtStart;
                String duration;
                
                if ((start > 0L) && (end > start)) {
                    if (calEvent.isAllDay) {
                        long days = (end - start + DateUtils.DAY_IN_MILLIS - 1) / DateUtils.DAY_IN_MILLIS;
                        duration = "P" + days + "D";
                    } else {
                        long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                        duration = "P" + seconds + "S";
                    }
                    eventValues.put(Events.DURATION, duration);
                } else {
                    Log.v(TAG, "STARTIME=" + start + ", endTime=" + end + ",UID:" + calEvent.uid);
                    throw new IllegalStateException("ICal file has incorrect DURATION and DTEND value");
                }
            }
        }

        eventValues.put(Events.EVENT_LOCATION, calEvent.location);// eventLocation

        if (!TextUtils.isEmpty(calEvent.exrule))
            eventValues.put(Events.EXRULE, calEvent.exrule); // exrule
        if (calEvent.hasAlarm)
            eventValues.put(Events.HAS_ALARM, 1); // hasAlarm
        else
            eventValues.put(Events.HAS_ALARM, 0); // hasAlarm

        eventValues.put(Events.HAS_EXTENDED_PROPERTIES, calEvent.hasExtendedProperties); // hasExtendedProperties

        if (!TextUtils.isEmpty(calEvent.rrule))
            eventValues.put(Events.RRULE, calEvent.rrule); // rrule

        eventValues.put(Events.STATUS, calEvent.status); // eventStatus
        eventValues.put(Events.TITLE, calEvent.summary); // title

        if (!TextUtils.isEmpty(calEvent.exdate))
            eventValues.put(Events.EXDATE, calEvent.exdate); // exdate
        if (!TextUtils.isEmpty(calEvent.rdate))
            eventValues.put(Events.RDATE, calEvent.rdate); // rdate

        if(!TextUtils.isEmpty(calEvent.tz)){
            if (DEVELOPMENT) {
                Log.v(TAG,"time zone is not null "+calEvent.tz);
            }
            eventValues.put(Events.EVENT_TIMEZONE,calEvent.tz);
        }

        return builder.withValues(eventValues).build();

    }

    /**
    * Reads cursor and populate calendar event structure
    */
    private CalendarEvent getCalendarEvent(Cursor c, ContentResolver resolver) {
        CalendarEvent event = new CalendarEvent();
        int index = c.getColumnIndex(Events._ID);

        long _id_ = -1;
        if (index != -1) {
            _id_ = c.getInt(index);
        }

        index = c.getColumnIndex(Events.TITLE);
        if (index != -1) {
            String summary = c.getString(index);
           /*
           if(summary !=null){
                  event.summary = summary.replaceAll("\r\n","\\\\n")
                                     .replaceAll("\n","\\\\n");
           }
           */
             event.summary = summary;
        }

        index = c.getColumnIndex(Events.EVENT_LOCATION);
        if (index != -1) {
            event.location = c.getString(index);
        }

        index = c.getColumnIndex(Events.DESCRIPTION);
        if (index != -1) {
             String description = c.getString(index);
             /*
            if(description !=null){
                event.description = description.replaceAll("\r\n","\\\\n")
                                     .replaceAll("\n","\\\\n");
             }
             */
                event.description = description;
        }

        index = c.getColumnIndex(Events.STATUS);
        if (index != -1) {
            event.status = c.getInt(index);
        }

        index = c.getColumnIndex(Events.DTSTART);
        if (index != -1) {
            if (c.isNull(index))
            {
                event.dtStart = CalendarEvent.FIELD_EMPTY_INT;
            }
            else
            {
                event.dtStart = c.getLong(index);
            }
        }

        index = c.getColumnIndex(Events.DTEND);
        if (index != -1) {
            if (c.isNull(index))
            {
                event.dtEnd = CalendarEvent.FIELD_EMPTY_INT;
            }
            else
            {
                event.dtEnd = c.getLong(index);
            }
        }
        // Change for icecream.
        String accountName = null;
        String accountType = null;
        String ownerAccount = null;
        index = c.getColumnIndex(Events.CALENDAR_ID);
        if (index != -1L) {
            Cursor cursor = resolver.query(Calendars.CONTENT_URI,
                    new String[] {Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE, Calendars.OWNER_ACCOUNT},
                    Calendars._ID + "=?"/* selection */,
                    new String[]{Long.toString(c.getLong(index))}/* selection args*/,
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        accountName = cursor.getString(0);
                        accountType = cursor.getString(1);
                        ownerAccount = cursor.getString(2);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        // Exchanage uses SYNC_DATA2 for UID. But we don't know which field gmail
        // is using. Instead of guessing the detail implementation of sync
        // engine, we generate UID by ourselves. Hope google can export UID field
        // in later release.
        {
            event.uid = null;
            Cursor cursor = resolver.query(ExtendedProperties.CONTENT_URI,
                    new String[] {ExtendedProperties.VALUE}/* projection */,
                    ExtendedProperties.EVENT_ID + "=? AND " + ExtendedProperties.NAME + "=?"/* selection */,
                    new String[] {String.valueOf(_id_), VCalConstants.UID}/* selection arg */,
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        event.uid = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }

            // If there's no uid we'll generate one by ourself.
            if (event.uid == null) {
                event.uid = UUID.randomUUID().toString();
                ContentValues values = new ContentValues(2);
                values.put(ExtendedProperties.EVENT_ID, String.valueOf(_id_));
                values.put(ExtendedProperties.NAME, VCalConstants.UID);
                values.put(ExtendedProperties.VALUE, event.uid);
                // Pretend that we're sync adapter since only sync adapters can operate extended properties
                if ((accountName != null) && (accountType != null)) {
                    Uri uri = ExtendedProperties.CONTENT_URI.buildUpon()
                        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, String.valueOf(true))
                            .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
                                .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
                    resolver.insert(uri, values);
                }
            }
        }

        index = c.getColumnIndex(Events.DURATION);
        if (index != -1) {
            event.duration = c.getString(index);
        }
        index = c.getColumnIndex(Events.ALL_DAY);
        if (index != -1) {
            event.isAllDay = c.getInt(index) == 1;

        }

        index = c.getColumnIndex(Events.AVAILABILITY);
        if (index != -1) {
            event.isTransparent = c.getInt(index) == Events.AVAILABILITY_FREE;

        }

        index = c.getColumnIndex(Events.RRULE);
        if (index != -1) {
            event.rrule = c.getString(index);
        }

        index = c.getColumnIndex(Events.EXRULE);
        if (index != -1) {
            event.exrule = c.getString(index);
        }

        index = c.getColumnIndex(Events.EVENT_TIMEZONE);
        if (index != -1) {
            event.tz = c.getString(index);
        }

        index = c.getColumnIndex(Events.HAS_ALARM);
        if (index != -1) {
            event.hasAlarm = c.getInt(index) == 1;

        }

        index = c.getColumnIndex(Events.HAS_EXTENDED_PROPERTIES);
        if (index != -1) {
            event.hasExtendedProperties = c.getInt(index) == 1;

        }

        index = c.getColumnIndex(Events.RDATE);
        if (index != -1) {
            event.rdate = c.getString(index);
        }

        index = c.getColumnIndex(Events.EXDATE);
        if (index != -1) {
            event.exdate = c.getString(index);
        }

        if (_id_ != -1) {
            getAlarms(_id_, resolver, event.alarms);
            getAttendees(_id_, resolver, event.attendees, accountType, ownerAccount);
            getExtProperties(_id_, resolver, event.extendedProperties);
        }

        return event;
    }

    /**
    * fills alarm structure
    */
    private void getAlarm(final long id, ContentResolver resolver, CalendarEvent.Alarm alarm) {
        String sql = CalendarAlerts.EVENT_ID + "=" + id;
        Cursor c = resolver.query(CalendarAlerts.CONTENT_URI, null, sql, null, null);

        if (alarm == null) {
            return;
        }

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    alarm.alarmTime = c.getLong(c.getColumnIndex(CalendarAlerts.ALARM_TIME));
                    alarm.state = c.getInt(c.getColumnIndex(CalendarAlerts.STATE));
                }
            } finally {
                c.close();
            }
        }

        sql = Reminders.EVENT_ID + "=" + id;
        c = resolver.query(Reminders.CONTENT_URI, null, sql, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    alarm.method = c.getInt(c.getColumnIndex(Reminders.METHOD));
                    alarm.minutes = c.getInt(c.getColumnIndex(Reminders.MINUTES));
                }
            } finally {
                c.close();
            }
        }
    }
    /**
    * fills alarm Vector
    */
    private void getAlarms(final long id, ContentResolver resolver,
            Vector<CalendarEvent.Alarm> alarms) {
        String sql = Reminders.EVENT_ID + "=" + id;
        Cursor c = resolver.query(Reminders.CONTENT_URI, null, sql, null, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        CalendarEvent.Alarm alarm = new CalendarEvent.Alarm();
                        int index = c.getColumnIndex(Reminders.MINUTES);
                        if (index != -1) {
                            alarm.minutes = c.getInt(index);
                        }
                        index = c.getColumnIndex(Reminders.METHOD);
                        if (index != -1) {
                            alarm.method = c.getInt(index);
                        }

                        alarms.add(alarm);
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
    }

    /**
    * fills attendees
    */
    private void getAttendees(final long id, ContentResolver resolver,
            Vector<CalendarEvent.Attendee> attendees, String accountType,
            String ownerAccount) {
        String sql = Attendees.EVENT_ID + "=" + id;
        Cursor c = resolver.query(Attendees.CONTENT_URI, null, sql, null, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        CalendarEvent.Attendee attendee = new CalendarEvent.Attendee();
                        int index = c.getColumnIndex(Attendees.ATTENDEE_NAME);
                        if (index != -1) {
                            attendee.name = c.getString(index);
                        }
                        index = c.getColumnIndex(Attendees.ATTENDEE_EMAIL);
                        if (index != -1) {
                            attendee.email = c.getString(index);
                        }
                        index = c.getColumnIndex(Attendees.ATTENDEE_RELATIONSHIP);
                        if (index != -1) {
                            attendee.relationship = c.getInt(index);
                        }
                        index = c.getColumnIndex(Attendees.ATTENDEE_TYPE);
                        if (index != -1) {
                            attendee.type = c.getInt(index);
                        }
                        index = c.getColumnIndex(Attendees.ATTENDEE_STATUS);
                        if (index != -1) {
                            attendee.status = c.getInt(index);
                        }
                        if (DEVELOPMENT) {
                            Log.v(TAG , ": getAttendee=" + attendee.name);
                        }
                        if (!TextUtils.isEmpty(attendee.email)) {
                            // For local account, don't try to share "local@phone" to others
                            if (!CalendarContract.ACCOUNT_TYPE_LOCAL.equals(accountType) ||
                                    !attendee.email.equalsIgnoreCase(ownerAccount)) {
                                attendees.add(attendee);
                            }
                        }
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
    }

    /**
    * fills extended properties
    */
    private void getExtProperties(final long id, ContentResolver resolver,
            Vector<CalendarEvent.ExtProp> extProps) {
        // Change for icecream
        // Filter out UID field since we already included it in the event
        String selection = ExtendedProperties.EVENT_ID + "=? AND " + ExtendedProperties.NAME + "!=?";
        String[] selectionArg = new String[] {String.valueOf(id), VCalConstants.UID};
        Cursor c = resolver.query(ExtendedProperties.CONTENT_URI, null, selection, selectionArg, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String prop;
                    do {
                        CalendarEvent.ExtProp extProp = new CalendarEvent.ExtProp();
                        int index = c.getColumnIndex(ExtendedProperties.NAME);
                        if (index != -1) {
                            extProp.name = c.getString(index);
                            // Only get the one we wanted since many sources can write data to
                            // ExtendedProperties table. Please map this with VCalendarParser.
                            prop = extProp.name.toUpperCase();
                            if(!prop.startsWith("X-") && !prop.equals("DTSTAMP") &&
                                    !prop.equals("SEQUENCE")) {
                                continue;
                            }
                        }
                        index = c.getColumnIndex(ExtendedProperties.VALUE);
                        if (index != -1) {
                            extProp.value = c.getString(index);
                        }
                        if (!TextUtils.isEmpty(extProp.name) &&
                                !TextUtils.isEmpty(extProp.value)) {
                            extProps.add(extProp);
                        }
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * Read one line from the input stream. A line is represented by zero or more bytes
     * followed by '\n'.  The returned line includes the '\n' at the end.
     *
     * @param in
     *        Input stream to be read from
     *
     * @return  the contents of the line or null if no bytes were read before the end
     *          of the input stream has been reached.
     *
     * @throws IOException
     *         If input stream is closed or some other I/O error occurs.
     */
    private static byte[] readLine(final InputStream in) throws IOException {
        int i = in.read();
        if (i == -1) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (i != -1) {
            out.write(i);
            if (i == '\n') break;
            i = in.read();
        }

        return out.toByteArray();
    }

    /**
     * Check the soft line break at the end of the byte array. To pass check, the input
     * line must end with soft break byte followed by '\n' or "\r\n". For example, for
     * Quoted-Printable characters the soft line break is "=\n" or "=\r\n".
     *
     * @param line
     *        the input line to be checked
     *
     * @param c
     *        the char which playing the role as soft break
     *
     * @return the offset where the soft line break begins or -1 if there's no soft line break
     */
    private static int indexOfSoftLineBreak(final byte[] line, char c) {
        int b = (byte)c;
        int length = line.length;

        if (length == 2) {
            if (line[length-1] == '\n' && line[length-2] == b) {
                return 0;
            }
        } else if (length > 2) {
            if (line[length-1] == '\n') {
                if (line[length-2] == b) {
                    return length - 2;
                } else if (line[length-2] == '\r' && line[length-3] == b) {
                    return length - 3;
                }
            }
        }

        return -1;
    }

    /**
     * Removes tranfer-padding bytes if the text is encoded with quoted printable
     *
     * @param in
     *        vCal input stream
     *
     * @return byte arry with transfer-padding bytes of quoted printable being removed
     *
     * @throws IOException
     *         If input stream is closed or some other I/O error occurs.
     */
    private static byte[] handleEncodings(final InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            byte[] line = readLine(in);
            if (line == null) {
                break;
            }

            String strLine = new String(line, "UTF-8");
            if(strLine.contains("ENCODING=QUOTED-PRINTABLE:")){
                int index = indexOfSoftLineBreak(line, '=');
                if (index != -1) {
                    out.write(line, 0, index);
                    while (true) {
                        line = readLine(in);
                        if (line == null) {
                            break;
                        } else {
                            index = indexOfSoftLineBreak(line, '=');
                            if (index != -1) {
                                out.write(line, 0, index);
                            } else {
                                out.write(line, 0, line.length);
                            }
                        }
                    }
                } else {
                    out.write(line, 0, line.length);
                }
            } else {
                out.write(line, 0, line.length);
            }
        }

        return out.toByteArray();
    }


    /**
     * This method unfolds the text by applying following pattern:
     * 1) "\r\n " 2) "\r " 3) "\n " 4) "\r\n\t" 5) "\r\t" 6) "\n\t"
     * pattern "\r\n " is defined by iCalendar spec RFC5545
     * pattern "\r\n\t" and "\n\t" is used Micorsoft Outlook 2007
     * pattern "\n " is used by Mozilla Calendar/Sunbird and KOrganizer
     *
     * @param in The utf8 stream file to be unfolded
     *
     * @return String stream of the iCalendar file which has already
     *         been unfolded
     *
     * @throws IOException
     *         If input stream is closed or some other I/O error occurs.
     */
    private static String getUnfoldedText(final InputStream in) throws IOException {
        try {
            byte[] bytes = handleEncodings(in);
            if (bytes != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                int length = bytes.length;
                for (int i = 0; i < length; ++i) {
                    if (bytes[i] != '\r' && bytes[i] != '\n') {
                        // Output bytes[i] and continue to handle bytes[i+1]
                        os.write(bytes[i]);
                    } else if (i < length -1) {
                        if (bytes[i+1] == ' ' || bytes[i+1] == '\t') {
                            // Skip "\n " or "\n\t" or "\r " or "\r\t",
                            // another +1 is in the for loop.
                            i = i + 1;
                        } else if (bytes[i] == '\n') {
                            // byte[i] == '\n'
                            // byte[i+1] != (' ' and '\t')
                            // Output bytes[i] and continue to handle bytes[i+1]
                            os.write(bytes[i]);
                        } else if (bytes[i+1] == '\n') {
                            // byte[i] == '\r' and byte[i+1] == '\n' now
                            if (i < length -2) {
                                if (bytes[i+2] == ' ' || bytes[i+2] == '\t') {
                                    // Skip "\r\n " or "\r\n\t", another +1 is in the
                                    // for loop.
                                    i = i + 2;
                                } else {
                                    // byte[i] == '\r'
                                    // byte[i+1] == '\n'
                                    // byte[i+2] != (' ' and '\t')
                                    // Output bytes[i] and bytes[i+1], continue to handle byte[i+2]
                                    os.write(bytes[i]);
                                    os.write(bytes[i+1]);
                                    // Another +1 is in the for loop
                                    i = i + 1;
                                }
                            } else {
                                // Skip the "\r\n" at the end of the input stream
                                break;
                            }
                        } else {
                            // byte[i] == '\r'
                            // byte[i+1] != (' ' and '\t' and '\n')
                            // Output byte[i] and continue to handle byte[i+1]
                            os.write(bytes[i]);
                        }
                    }
                }

                return new String(os.toByteArray(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("No utf-8 support!");
        }

        return null;
    }

    /**
     * Get the scratch calendar with the given name. This function retrieves the
     * named invisible calendar or creates it if it doesn't exist.
     *
     * @param context
     * @param name - the name given to the Calendar.
     * @param displayName - the display-name given to the Calendar.
     * @return the item uri for the Calendar, null - have account problem
     */
    private static Uri getScratchCalendarByName(Context context, String name, String displayName) {
        ContentResolver cr = context.getContentResolver();
        final Account kAccount = ICAL_ACCOUNT_INFO;

        //Uri uri = decorateForScratch(Calendars.CONTENT_URI);
        Uri.Builder uriBuilder = Calendars.CONTENT_URI.buildUpon();
        // TODO: Consolidate the string constants
        uriBuilder.appendQueryParameter("use_hidden_calendar", String.valueOf(true));
        Uri uri = uriBuilder.build();

        // (1) Does the scratch calendar already exist?
        final String kSelection = Calendars.ACCOUNT_NAME+ " = ? AND " +
            Calendars.ACCOUNT_TYPE + " = ? AND " +
            Calendars.NAME + " = ?";

        final String[] kSelectionArgs = new String[] { kAccount.name, kAccount.type, name };
        final String[] kProjection = new String[] { Calendars._ID };

        Cursor c = null;
        try {
        	Log.e("test", TAG+":getScratchCalendarByName: uri = "+uri.toString());
            c = cr.query(uri, kProjection, kSelection, kSelectionArgs, null);
            if (c != null && c.moveToFirst()) {
                long calendarId = c.getInt(0);
                return ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId);
            } else {
                // (2) No -- create the calendar.
                ContentValues values = new ContentValues();
                values.put(Calendars.ACCOUNT_NAME, kAccount.name);
                values.put(Calendars.ACCOUNT_TYPE, kAccount.type);
                values.put(Calendars.NAME, name);
                values.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
                values.put(Calendars.CALENDAR_COLOR, Integer.valueOf(Color.CYAN));
                values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
                // Making owner account of scratch calendar the same as local account
                // which will be automatically applied to event organizer when saving
                // a shared local event which has no organizer info.
                values.put(Calendars.OWNER_ACCOUNT, "local@phone");
                values.put(Calendars.VISIBLE, 0);
                values.put(Calendars.SYNC_EVENTS, 0);
                TimeZone phoneTimezone = TimeZone.getDefault();
                String timezoneJava = phoneTimezone.getID();
                String phoneTimezoneId = timezoneJava;
                values.put(Calendars.CALENDAR_LOCATION, phoneTimezoneId);  //store default for debug
                values.put(Calendars.CALENDAR_TIME_ZONE, phoneTimezoneId);

                // change for icecream
                // pretend that we're sync adapter since only sync adapters can insert an calendar account
                uriBuilder = Calendars.CONTENT_URI.buildUpon();
                // TODO: Consolidate the string constants
                uriBuilder.appendQueryParameter("hide_from_user", String.valueOf(true));
                uriBuilder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, String.valueOf(true));
                uriBuilder.appendQueryParameter(Calendars.ACCOUNT_NAME, kAccount.name);
                uriBuilder.appendQueryParameter(Calendars.ACCOUNT_TYPE, kAccount.type);
                uri = uriBuilder.build();

                return cr.insert(uri, values);
            }
        } finally {
            if (c != null) c.close();
        }
    }


    /**
     * Save vCal byte stream to sdcard.
     *
     * @param context
     *        Application context the application is running in
     *
     * @param content
     *        vCal data stream to be saved
     *
     * @param dirName
     *        Direcotry under which the vCal content will be saved. If null,
     *        the content will be saved to the application package direcotry.
     *
     * @param fileName
     *        File name for the content to be saved. If null, the checksum of
     *        the content will be used as file name.
     *
     * @return File Uri of the saved vCal cotent or null failed to save to scard.
     *
     * @throws IOException
     *         If failed to create or write to output file
     */
    static private Uri saveVCalToFile(final Context context, byte[] content, String dirName,
            String fileName) throws IOException {
        if (content == null || content.length == 0) {
            return null;
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName  = generateFileName(content);
        } else {
            String FILE_NAME_REGEX = "[^a-zA-Z0-9\\.\\-\\_]+";
            fileName = Pattern.compile(FILE_NAME_REGEX).matcher(fileName).replaceAll(" ").trim();
        }

        File cacheDir = null;
        if (TextUtils.isEmpty(dirName)) {
            cacheDir = context.getCacheDir();
        } else {
            File rootDir = Environment.getExternalStorageDirectory();
            if (rootDir != null) {
                String absPath = rootDir.getAbsolutePath();
                if (dirName.charAt(0) != '/') {
                    absPath = absPath + '/' + dirName;
                } else {
                    absPath = absPath + dirName;
                }
                cacheDir = new File(absPath);
                if (!cacheDir.exists()) {
                    if (!cacheDir.mkdirs()) {
                        Log.w(TAG, "Unable to create external files directory");
                        return null;
                    }
                }
            }
        }
        Log.v(TAG, "ics file cache: " + cacheDir);
        if (cacheDir != null) {
            OutputStream fos = null;
            try {
                int ICS_FILE_MODE =
                    ParcelFileDescriptor.MODE_READ_WRITE |
                    ParcelFileDescriptor.MODE_WORLD_READABLE |
                    ParcelFileDescriptor.MODE_CREATE |
                    ParcelFileDescriptor.MODE_TRUNCATE;

                final File icsFile = new File(cacheDir, fileName);
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(icsFile, ICS_FILE_MODE);
                if (pfd != null) {
                    fos = new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
                    fos.write(content);
                    return Uri.fromFile(icsFile);
                }
            } finally {
                if (fos != null) {
                   fos.close();
                }
            }
        }

        return null;
    }

    private static String generateFileName(byte[] content) {
        CRC32 crc = new CRC32();
        crc.update(content);
        return String.valueOf(crc.getValue()) + ".vcs";
    }

    /**
     * Insert the calendar event into the calendar account
     *
     * @param cr application content resolver
     * @param calCursor
     *        cursor for the calendar account into which the event will be inserted
     * @param CalendarEvent
     *        calendar event to be inserted
     *
     * @return the URL of the newly created row or null if failed
     */
    public static Uri createEventInDb(final ContentResolver cr, final Cursor calCursor,
            final CalendarEvent calEvent) {
        if (cr == null || calEvent == null || calCursor == null){
            return null;
        }

        ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();

        int calendarId = calCursor.getInt(CALENDARS_INDEX_ID);

        deleteExisting(cr, calendarId, calEvent.uid);
        operationList.add(appendEventsBuilder(calendarId, calEvent));
        appendAttendeesBuilder(operationList, calEvent.attendees);
        appendAlarmListBuilder(operationList, calEvent.alarms);
        appendExtentedBuilder(operationList, calEvent.extendedProperties, calEvent.uid);

        try {
            ContentProviderResult[] results = cr.applyBatch(CalendarContract.AUTHORITY, operationList);
            return results[0].uri;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void deleteExisting(ContentResolver cr, int calendarId, String uid) {
        // change for icecream
        if (!TextUtils.isEmpty(uid)) {
            // Check whether the VEVENT is stored in db. If yes, delete it by the event _ID.
            Cursor c = cr.query(ExtendedProperties.CONTENT_URI,
                    new String[] { ExtendedProperties.EVENT_ID } /* projection */,
                    ExtendedProperties.NAME + "=? AND " + ExtendedProperties.VALUE + "=?" /* selection */,
                    new String[]{ VCalConstants.UID, uid } /* selection arg */, null);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        int eventId = c.getInt(0);
                        cr.delete(Events.CONTENT_URI,
                                Events._ID + "=?" + " AND " + Events.CALENDAR_ID + "=?",
                                new String[] { String.valueOf(eventId), String.valueOf(calendarId) });
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

}
