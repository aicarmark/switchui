package com.motorola.calendar.share.vcalendar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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

import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import com.android.calendarcommon.ICalendar.FormatException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.ExtendedProperties;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.Attendees;
import android.text.TextUtils;
import android.widget.Toast;
import android.text.format.DateUtils;
import android.graphics.Color;
import android.util.Log;
import android.util.Config;
import android.accounts.Account;

import com.motorola.calendar.share.vcalendar.common.ICalConstants;
import com.motorola.calendar.share.vcalendar.common.CalendarEvent;
import com.motorola.calendar.share.vcalendar.common.CalendarEvent.Attendee;
import com.motorola.calendar.share.vcalendar.common.CalendarEvent.Alarm;
import com.motorola.calendar.share.vcalendar.common.CalendarEvent.ExtProp;
import com.motorola.calendar.share.vcalendar.parser.VCalParser;
import com.motorola.calendar.share.vcalendar.parser.VCalParser_V1;
import com.motorola.calendar.share.vcalendar.parser.VCalParser_V2;
import com.motorola.calendar.share.vcalendar.composer.VCalComposer;
import com.motorola.calendar.share.vcalendar.composer.VCalComposer_V1;
import com.motorola.calendar.share.vcalendar.composer.VCalComposer_V2;

/**
  * This class provides Utilties to parse and compose ical / vcal files
  **/
public class VCalUtils {

    public static final String TAG = "VCalUtils";
    private static final boolean DEVELOPMENT = Config.DEBUG;
    private CalendarEvent mCalendarEvent;
    private long mAccountId = -1;
    private static String DESTINATION_FILE_NAME = "/tmp/tmp_calendar.ics";
    public static final String MMC_DIR = "/sdcard";
    public static final String DESTINATION_DIR = "/.ics/vcal/";
    public static final String VCAL_MIME_TYPE = "text/calendar";
    public static final String SCRATCH_CALENDAR = "__ScrAtCh_CaLeNDaR__";
    public static final String SCRATCH_CALENDAR_NAME = "Local iCalendar";

    public static final int ERROR_CODE_SUCCESS = 1;
    public static final int ERROR_CODE_ACCOUNT_NOT_SETUP=-1;
    public static final int ERROR_CODE_UNKNOWN = -2;
    public static final int ERROR_CODE_NO_CALENDAR_TABLE_SETUP = -3;
    public static final int ERROR_CODE_NO_SDCARD = -4;
    public static final int ERROR_CODE_SDCARD_READ_FAILURE = -5;
    public static final int ERROR_CODE_EVENT_EXISTS = -6;

    public static final int ERROR_CODE_VCAL_DATA_MISSING = -7;
    public static final int ERROR_CODE_CANNOT_MKDIR = -8;
    public static final int ERROR_CODE_CANNOT_CREATE_NEW_FILE = -9;
    public static final int ERROR_CODE_CANNOT_SHARE_VCAL = -10;
    public static final int ERROR_CODE_CANNOT_VIEW_VCAL = -11;

    // Change for icecream to use long for row id
    private long _id_=  -1;
    private String mfilename = null;
    private String mData=null;
    private String mCharsetString = "UTF-8";
    private String mTxVcal = "1.0";
    private static final String CALENDARS_WHERE = Calendars.ACCOUNT_NAME + "='local@phone'";

    private static final String CALENDARS_WHERE_FOR_ACCOUNT_ID = Calendars._ID + "=%d";

    private static final String[] PROJECTION = new String[] { Calendars._ID, };
    private static final String[] CALENDARS_PROJECTION = new String[] {
        Calendars._ID, // 0
        Calendars.CALENDAR_DISPLAY_NAME, // 1
        Calendars.OWNER_ACCOUNT, // 2
        Calendars.CALENDAR_TIME_ZONE, // 3
        // use different reminder array.
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE,
    };

    private static final int CALENDARS_INDEX_ID = 0;
    private static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    private static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    private static final int CALENDARS_INDEX_TIMEZONE = 3;
    private static final int CALENDARS_INDEX_SYNC_ACCOUNT = 4;
    private static final int CALENDARS_INDEX_SYNC_ACCOUNT_TYPE = 5;

    private Context mContext;
    private Uri mEventUri;
    public static final String VCAL_SD_DIR = "/Calendar/";

    // The iCal account info.
    private static final String ICAL_ACCOUNT_TYPE = "com.motorola.icalendar.UNCONNECTED_ACCOUNT";
    private static final String ICAL_ACCOUNT_NAME = "iCalendar";
    private static final Account ICAL_ACCOUNT_INFO = new Account(ICAL_ACCOUNT_NAME, ICAL_ACCOUNT_TYPE);

    /**
     * Constructor
     */
    public VCalUtils(){

    }

    /**
     * Constructor
     * @param context
     */
    public VCalUtils(Context context){
        mContext = context;
    }

    /**
     * This method unfolds the text by applying following pattern:
     * 1) "\r\n " 2) "\r " 3) "\n " 4) "\r\n\t" 5) "\r\t" 6) "\n\t"
     * pattern "\r\n " is defined by iCalendar spec RFC5545
     * pattern "\r\n\t" and "\n\t" is used Micorsoft Outlook 2007
     * pattern "\n " is used by Mozilla Calendar/Sunbird and KOrganizer
     * @param in The utf8 stream file to be unfolded
     * @return String stream of the iCalendar file which has already
     *         been unfolded
     */
    private static String getUnfoldedText(final InputStream in) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byte0 = in.read();
        while (byte0 != -1) {
            if ((byte0 != '\r') && (byte0 != '\n')) {
                os.write(byte0);
            } else {
                int byte1 = in.read();
                if (byte1 == -1) {
                    os.write(byte0);
                    // break if end of file
                    break;
                } else {
                    if ((byte1 == ' ') || (byte1 == '\t')) {
                        // skip "\n " or "\n\t" or "\r " or "\r\t"
                        byte0 = in.read();
                        continue;
                    } else if (byte0 == '\n') {
                        // byte0 == '\n'
                        // byte1 != ' ' and byte1 != '\t'
                        os.write(byte0);
                        os.write(byte1);
                    } else {
                        // byte0 == '\r'
                        if (byte1 == '\n') {
                            int byte2 = in.read();
                            if (byte2 == -1) {
                                os.write(byte0);
                                os.write(byte1);
                                // break if end of file
                                break;
                            } else if ((byte2 != ' ') && (byte2 != '\t')) {
                                os.write(byte0);
                                os.write(byte1);
                                os.write(byte2);
                            } else {
                                // skip "\r\n " or "\r\n\t"
                            }
                        } else {
                            // byte0 == '\r'
                            // byte1 != '\n'

                            os.write(byte0);
                            os.write(byte1);
                        }
                    }
                }
            }
            byte0 = in.read();
        }

        try {
            return new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("No utf-8 support!");
        }
    }

    public CalendarEvent parseVcal(InputStream is) {
        try {
            String data = getUnfoldedText(is);

            VCalParser mParser;
            if(data.contains("VERSION:1.0")){
                mParser = new VCalParser_V1();
            } else if ( data.contains("VERSION:2.0")){
                mParser = new VCalParser_V2();
            } else {
                if (DEVELOPMENT) {
                    Log.v(TAG,"unsupported version");
                }
                return null;
            }
            
            Collection<CalendarEvent> event = mParser.parse(data);
            if (event != null) {
                 Iterator<CalendarEvent> it = event.iterator();
                 while (it.hasNext()) {
                     mCalendarEvent = it.next();
                     break;
                 }
             }
        } catch (FormatException e) {
                e.printStackTrace();
                return null;
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read ICS file", e);
            return null;
        }
        return mCalendarEvent;
    }

  /**
    * It romoves all tranfer-padding
    * @param text to be unfolded
    * return folded string
    */
    public static String unfoldText(String text) {

        if(!text.contains(";ENCODING=QUOTED-PRINTABLE:")){
            text = text.replaceAll("\r\n[ \t]", "");
        }

       text = handleEncodings(text);
       text = text.replaceAll("\n[ \t]", "");
        return text;
    }

 /**
   * removes tranfer-padding bytes if the text is encoded with quoted printable
   * @param text to be processed
   * return truncated string
   */
    public static String handleEncodings(String text) {

        ByteArrayInputStream ba =new ByteArrayInputStream(text.getBytes());
        final InputStreamReader tmpReader = new InputStreamReader(ba);
        BufferedReader br = new BufferedReader(tmpReader);

        StringBuilder sb = new StringBuilder();
        while(true){
            try {
                String line = br.readLine();
                if(line == null){
                    break;
                }else if(line.contains("ENCODING=QUOTED-PRINTABLE:")){
                    if(line.endsWith("=")){
                        sb.append(line.substring(0,line.length()-1 ));
                        while(true){
                            line = br.readLine();
                            if(line == null){
                                break;
                            }else if(line.endsWith("=")){
                                sb.append(line.substring(0,line.length()-1));
                            }else{
                                sb.append(line).append("\n");
                                break;
                            }
                        }
                    }else{
                        sb.append(line).append("\n");
                    }
                }else{
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * Compose a String based vcalendar if the event's row id is provided
     *
     * @param             _id id of the event
     * @param             ctx
     * @param version     vcal version requested,must be 1.0 or 2.0
     * @return            returns vcal data in string format,returns null
     * if input parameters are invalid
     */

    public String composeVcal(long _id, Context ctx, String version){

        if(_id <0 || ctx == null || version == null){
            if (DEVELOPMENT) {
                Log.v(TAG,"null context");
            }
            return null;
        }
        if (DEVELOPMENT) {
            Log.v(TAG,"Event id :"+_id);
        }
        if(version.equals("1.0") == false &&
                version.equals("2.0") == false){
            if (DEVELOPMENT) {
                Log.v(TAG,"invalid version or vcard");
            }
            return null;
        }

        Uri record_uri = ContentUris.withAppendedId(Events.CONTENT_URI, _id);
        Cursor _cursor = null;
        mData = null;

        try{
            _cursor = ctx.getContentResolver().query(record_uri, null, null, null, null);
            if (_cursor != null && _cursor.moveToFirst()) {
                mCalendarEvent = getCalendarEvent(_cursor, ctx.getContentResolver());
                createCalendarData(version);
                //save();
            }
        }catch(Exception ex){
            Log.w(TAG, "Failed to compose vcal", ex);
        }finally{
            if(_cursor !=null){
                _cursor.close();
            }
        }
        return mData;
    }

    
    public int pushIntoCalendarProvider(Context ctx, long accountId){
        int ret = ERROR_CODE_UNKNOWN;
        Cursor calCursor = null;


        try{
            ContentResolver resolver = ctx.getContentResolver();
            Cursor cursor = resolver.query(Calendars.CONTENT_URI,
                    PROJECTION, null, null , Calendars.DEFAULT_SORT_ORDER);

            boolean noCalendars = ((cursor == null) || (cursor.getCount() == 0));
            if (cursor != null) {
                cursor.close();
            }

            if(noCalendars){
                return ERROR_CODE_ACCOUNT_NOT_SETUP;
            }

            String where;
            if (accountId != -1) {
                where = String
                        .format(CALENDARS_WHERE_FOR_ACCOUNT_ID, accountId);
            } else {
                where = CALENDARS_WHERE;
            }

            calCursor = resolver.query(Calendars.CONTENT_URI,
                    CALENDARS_PROJECTION, where, null, null);

            if (calCursor == null || !calCursor.moveToFirst()) {
                if (calCursor != null)
                    calCursor.close();
                return ERROR_CODE_NO_CALENDAR_TABLE_SETUP ;
            }

            calCursor.moveToFirst();
            mAccountId = calCursor.getLong(calCursor.getColumnIndex("_id"));

            boolean eventExist = false;
            int calendarId = -1;

            if(mCalendarEvent !=null && !TextUtils.isEmpty(mCalendarEvent.uid)){
                    String select = CalendarContract.Events._ID + "=?";
                    String[] selectArgs = new String[] { mCalendarEvent.uid };
                    final String[] projection = new String[]{"_id" ,"calendar_id"};
                    Cursor c = resolver.query(CalendarContract.Events.CONTENT_URI,
                                    projection, select, selectArgs, null);
                    if(c!=null){
                            try{
                                    if(c.getCount() > 0){
                                            //this is further check to find if the found uid is in default
                                            // calendar table
                                            c.moveToFirst();
                                            calendarId = calCursor.getInt(calCursor.getColumnIndex("_id"));
                                            int calendarIdFromEvent = c.getInt(c.getColumnIndex("calendar_id"));
                                            if(calendarId == calendarIdFromEvent){
                                                    eventExist = true;
                                            }
                                    }
                            }catch(Exception ex){
                                    ex.printStackTrace();
                            }
                            c.close();
                    }
            }
            if(eventExist == true){
                    //TODO: we can match other details also before ignoring save
                    return ERROR_CODE_EVENT_EXISTS;
            }
            if(createEventInDb(calCursor, resolver)){
                ret = ERROR_CODE_SUCCESS;
            }


        }catch(Exception ex){
            ex.printStackTrace();
            ret = ERROR_CODE_UNKNOWN;
        }finally{
            if(calCursor!=null){
                calCursor.close();
            }
        }
        return ret;
    }
    
    /**
     * Push this event into the scratch calendar
     *
     * @param ctx
     * @param accountId
     * @return error code if insert fails
     */
    public int pushIntoScratchCalendar(Context ctx) {
        ContentResolver resolver = ctx.getContentResolver();
        //get scratch calendar URI
        final Uri scratchCalURI = getScratchCalendarByName(ctx, SCRATCH_CALENDAR, SCRATCH_CALENDAR_NAME);
        if (scratchCalURI ==  null) {
            return ERROR_CODE_ACCOUNT_NOT_SETUP;
        }

        int ret = ERROR_CODE_UNKNOWN;
        //get a cursor over it
        Cursor scratchCalCursor = resolver.query(scratchCalURI,
                CALENDARS_PROJECTION, null, null, null);
        if (scratchCalCursor != null) {
            try {
                if (scratchCalCursor.moveToFirst() &&
                        createEventInDb(scratchCalCursor, resolver)) {
                    ret = ERROR_CODE_SUCCESS;
                }
            } finally {
                scratchCalCursor.close();
            }
        }
        return ret;
    }

    /**
    * moves cursor the calendar default calendar or to the Active Sync calender
    * if it exists
    * @param cursor to be moved to the default
    */
    private void moveCursorToDefault(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            // TODO: may select a default account such as Exchange or Gmail
            // later
        }
    }

    /**
    * It inserts parsed event to the event table
    * @param calendarCursor
    * @param cr
    */
    private boolean createEventInDb(Cursor calendarCursor,ContentResolver cr){
        boolean ret = false;

        if(mCalendarEvent == null || calendarCursor == null){
            if (DEVELOPMENT) {
                Log.v(TAG,"CalendarEvent is null");
            }
            return ret;
        }

        ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();


        deleteExisting(cr, calendarCursor, mCalendarEvent.uid);
        operationList.add(appendEventsBuilder(calendarCursor));
        appendAttendeesBuilder(operationList, mCalendarEvent.attendees);
        appendAlarmListBuilder(operationList, mCalendarEvent.alarms);
        appendExtentedBuilder(operationList, mCalendarEvent.extendedProperties);

        try {
            ContentProviderResult[] results = cr.applyBatch(CalendarContract.AUTHORITY, operationList);
            mEventUri = results[0].uri;
            ret = true;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void deleteExisting(ContentResolver cr, Cursor calendarCursor, String uid) {
        // change for icecream
        if (!TextUtils.isEmpty(uid)) {
            Uri uri = decorateForScratch(ExtendedProperties.CONTENT_URI);

            // Check whether the VEVENT is stored in db. If yes, delete it by the event _ID.
            Cursor c = cr.query(uri, new String[] {ExtendedProperties.EVENT_ID}/* projection */,
                    ExtendedProperties.NAME + "=? AND " + ExtendedProperties.VALUE + "=?"/* selection */,
                    new String[]{ICalConstants.UID, uid} /* selection arg */, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        String eventId = c.getString(0);
                        cr.delete(Events.CONTENT_URI,
                                Events._ID + "=?" /* selection */,
                                new String[]{eventId}/* selection arg */);
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    /**
    * append operationList with Extended properties
    */
    private void appendExtentedBuilder(
            ArrayList<ContentProviderOperation> operationList,
            Vector<ExtProp> exts) {
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

                    if (ICalConstants.UID.equals(name)) {
                        needInsertUID = false;
                    }
                }
            }
        }

        if (needInsertUID && !TextUtils.isEmpty(mCalendarEvent.uid)) {
            builder = ContentProviderOperation.newInsert(uri);
            builder.withValueBackReference(ExtendedProperties.EVENT_ID, 0); // event_id
            builder.withValue(ExtendedProperties.NAME, ICalConstants.UID); // name
            builder.withValue(ExtendedProperties.VALUE, mCalendarEvent.uid); // value
            operationList.add(builder.build());
        }
    }

    /**
    * append operationList with alarms
    */
    private void appendAlarmListBuilder(
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
    private void appendAttendeesBuilder(ArrayList<ContentProviderOperation> operationList,
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
    private ContentProviderOperation appendEventsBuilder(Cursor calendarCursor) {


        ContentProviderOperation.Builder  builder =
            ContentProviderOperation.newInsert(Events.CONTENT_URI);

        ContentValues eventValues = new ContentValues();
        eventValues.put(Events.CALENDAR_ID,
                calendarCursor.getInt(CALENDARS_INDEX_ID));

        if (!TextUtils.isEmpty(mCalendarEvent.organizer)) {
            eventValues.put(Events.ORGANIZER, mCalendarEvent.organizer);
        }

        // prevent guests from modifying the event
        eventValues.put(Events.GUESTS_CAN_MODIFY, 0);

        if ((mCalendarEvent.attendees != null) && (mCalendarEvent.attendees.size() > 0)) {
            eventValues.put(Events.HAS_ATTENDEE_DATA, 1);
        } else {
            eventValues.put(Events.HAS_ATTENDEE_DATA, 0);
        }

        if (mCalendarEvent.isTransparent)
            eventValues.put(Events.AVAILABILITY, Events.AVAILABILITY_FREE); // transparency
        else
            eventValues.put(Events.AVAILABILITY, Events.AVAILABILITY_BUSY); // transparency

        if (mCalendarEvent.dtEnd != CalendarEvent.FIELD_EMPTY_INT)
            eventValues.put(Events.DTEND, mCalendarEvent.dtEnd); // dtend
        if (mCalendarEvent.dtStart != CalendarEvent.FIELD_EMPTY_INT)
            eventValues.put(Events.DTSTART, mCalendarEvent.dtStart); // dtstart


        if (mCalendarEvent.isAllDay)
            eventValues.put(Events.ALL_DAY, 1); // allDay
        else
            eventValues.put(Events.ALL_DAY, 0); // allDay

        eventValues.put(Events.DESCRIPTION, mCalendarEvent.description); // description

        // Change for icecream
        if (!TextUtils.isEmpty(mCalendarEvent.rrule) || !TextUtils.isEmpty(mCalendarEvent.rdate)) {
            // for recurring event, we rely on duration rather than dtend.
            eventValues.remove(Events.DTEND);

            if (!TextUtils.isEmpty(mCalendarEvent.duration)) {
                eventValues.put(Events.DURATION, mCalendarEvent.duration); // duration
            } else {
                long end = mCalendarEvent.dtEnd;
                long start = mCalendarEvent.dtStart;
                String duration;

                if ((start > 0L) && (end > start)) {
                    if (mCalendarEvent.isAllDay) {
                        long days = (end - start + DateUtils.DAY_IN_MILLIS - 1) / DateUtils.DAY_IN_MILLIS;
                        duration = "P" + days + "D";
                    } else {
                        long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                        duration = "P" + seconds + "S";
                    }
                    eventValues.put(Events.DURATION, duration);
                } else {
                    throw new IllegalStateException("ICal file has incorrect DURATION and DTEND value");
                }
            }
        }

        eventValues.put(Events.EVENT_LOCATION, mCalendarEvent.location);// eventLocation

        if (!TextUtils.isEmpty(mCalendarEvent.tz))
            eventValues.put(Events.EVENT_TIMEZONE, mCalendarEvent.tz); // eventTimezone
        if (!TextUtils.isEmpty(mCalendarEvent.exrule))
            eventValues.put(Events.EXRULE, mCalendarEvent.exrule); // exrule
        if(mCalendarEvent.hasAlarm)
            eventValues.put(Events.HAS_ALARM, 1); // hasAlarm
        else
            eventValues.put(Events.HAS_ALARM, 0); // hasAlarm

        eventValues.put(Events.HAS_EXTENDED_PROPERTIES, mCalendarEvent.hasExtendedProperties); // hasExtendedProperties

        if (!TextUtils.isEmpty(mCalendarEvent.rrule))
            eventValues.put(Events.RRULE, mCalendarEvent.rrule); // rrule

        eventValues.put(Events.STATUS, mCalendarEvent.status); // eventStatus
        eventValues.put(Events.TITLE, mCalendarEvent.summary); // title

        if (!TextUtils.isEmpty(mCalendarEvent.exdate))
            eventValues.put(Events.EXDATE, mCalendarEvent.exdate); // exdate
        if (!TextUtils.isEmpty(mCalendarEvent.rdate))
            eventValues.put(Events.RDATE, mCalendarEvent.rdate); // rdate

        if(!TextUtils.isEmpty(mCalendarEvent.timeZone)){
            if (DEVELOPMENT) {
                Log.v(TAG,"time zone is not null "+mCalendarEvent.timeZone);
            }
            eventValues.put(Events.EVENT_TIMEZONE,mCalendarEvent.timeZone);
        }

        return builder.withValues(eventValues).build();

    }

    /**
     *  Save the vcal file in the filesystem,
     *  with the specified filename.if filename is null
     *  the default location is /tmp/tmp_calendar.vcs
     * @param filename
     * @return returns true for success and false for failure
     */
    public boolean saveToFile(String filename){

        boolean ret = false;
        if(mData == null){
            return ret;
        }

        if(filename == null){
            filename = DESTINATION_FILE_NAME;
        }

        File logfile = new File(filename);
        if(!logfile.exists()){
            try {
                logfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OutputStream outputStream = null;
        Writer writer = null;
        try{
            outputStream = new FileOutputStream(filename);
            writer = new BufferedWriter(new OutputStreamWriter(
                    outputStream, mCharsetString));
            writer.write(mData);
            ret = true;
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }finally{
            if(writer!=null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;

    }

    /**
     * Create a vCal or iCal string steam based on the version
     */
    private void createCalendarData(String version) {
        if ("1.0".equals(version)) {
            VCalComposer ical = new VCalComposer_V1(mCalendarEvent);
            mData = ical.composeVCal();
        } else if ("2.0".equals(version)) {
            VCalComposer ical = new VCalComposer_V2(mCalendarEvent);
            mData = ical.composeVCal();
        } else if (DEVELOPMENT) {
            Log.e(TAG, "version error, unsupported version :" + version);
        }
    }

    /**
    * Reads cursor and populate calendar event structure
    */
    private CalendarEvent getCalendarEvent(Cursor c, ContentResolver resolver) {
        CalendarEvent event = new CalendarEvent();
        int index = c.getColumnIndex(Events._ID);

        _id_ = -1;
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
        // Exchanage uses SYNC_DATA2 for UID. But we don't know which field gmail
        // is using. Instead of guessing the detail implementation of sync
        // engine, we generate UID by ourselves. Hope google can export UID field
        // in later release.
        {
            event.uid = null;
            Cursor cursor = resolver.query(ExtendedProperties.CONTENT_URI,
                    new String[] {ExtendedProperties.VALUE}/* projection */,
                    ExtendedProperties.EVENT_ID + "=? AND " + ExtendedProperties.NAME + "=?"/* selection */,
                    new String[] {String.valueOf(_id_), ICalConstants.UID}/* selection arg */,
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        event.uid = c.getString(0);
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
                values.put(ExtendedProperties.NAME, ICalConstants.UID);
                values.put(ExtendedProperties.VALUE, event.uid);
                // Pretend that we're sync adapter since only sync adapters can operate extended properties
                index = c.getColumnIndex(Events.CALENDAR_ID);
                if (index != -1L) {
                    cursor = resolver.query(Calendars.CONTENT_URI,
                            new String[] {Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE},
                            Calendars._ID + "=?"/* selection */,
                            new String[]{Long.toString(c.getLong(index))}/* selection args*/,
                            null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                String accountName = cursor.getString(0);
                                String accountType = cursor.getString(1);
                                Uri uri = ExtendedProperties.CONTENT_URI.buildUpon()
                                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, String.valueOf(true))
                                        .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
                                            .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
                                resolver.insert(uri, values);
                            }
                        } finally {
                            cursor.close();
                        }
                    }
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
            getAttendees(_id_, resolver, event.attendees);
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
            Vector<CalendarEvent.Attendee> attendees) {
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
                        attendees.add(attendee);
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
        String[] selectionArg = new String[] {String.valueOf(id), ICalConstants.UID};
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
    * It deletes all files in the dir directory dir
    */

    private void deleteVcalFiles( String dirName ) {
    try {
        File directory = new File(dirName);

            File[] files = directory.listFiles();
            for (File file : files)
            {
               if(!file.delete())
        Log.w(TAG, " File deletion failed");
            }
    } catch ( Exception e ) {
        e.printStackTrace();
    }
    }

    /**
     * It saves Calendar data to the filename provided
     *
     * @param filename if filename is null the checksum is used to generate a
     *            file to avoid duplication.
     * @return result of write operation
     */
    public int saveToMMC(String filename, String destDir) {
        int result = ERROR_CODE_UNKNOWN;

        if (TextUtils.isEmpty(mData)) {
            Log.e(TAG, "saveToMMC, missing VCAL data");
            return ERROR_CODE_VCAL_DATA_MISSING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return ERROR_CODE_NO_SDCARD;
        }

        File targetDirectory = new File(MMC_DIR);

        if (!(targetDirectory.exists() && targetDirectory.isDirectory() && targetDirectory
                .canRead()) && !targetDirectory.mkdirs()) {
            return ERROR_CODE_SDCARD_READ_FAILURE;
        }

        deleteVcalFiles(MMC_DIR + DESTINATION_DIR);
        if (TextUtils.isEmpty(destDir)) {
            destDir = DESTINATION_DIR;
        }
        if (TextUtils.isEmpty(filename)) {
            mfilename = getFileName(destDir);
        } else {
            String FILE_NAME_REGEX = "[^a-zA-Z0-9\\.\\-\\_]+";
            filename = Pattern.compile(FILE_NAME_REGEX).matcher(filename).replaceAll(" ").trim();
            int index = filename.lastIndexOf(".");
            if (index <= 0) {
                mfilename = getFileName(destDir);
            } else {
                mfilename = MMC_DIR + destDir + filename.trim();
            }
        }

        OutputStream outputStream = null;
        Writer writer = null;

        try {
            File parentDirectory = new File(MMC_DIR + destDir);
            if (!parentDirectory.exists()) {
                if (!parentDirectory.mkdirs()) {
                    Log.e(TAG, "saveToMMC, Directory creation failed: " + parentDirectory.getPath());
                    return ERROR_CODE_CANNOT_MKDIR;
                }
            }

            File fl;
            int fileCounter = 1;
            fl = new File(mfilename);
            if (fl.exists()) {
                while (fileCounter > 0) {
                    mfilename = MMC_DIR + destDir + fileCounter + "_" + filename;
                    fl = new File(mfilename);
                    if (!fl.exists())
                        break;
                    fileCounter = fileCounter + 1;
                }
            }
            if (!fl.createNewFile()) {
                Log.e(TAG, "saveToMMC, File creation failed");
                return ERROR_CODE_CANNOT_CREATE_NEW_FILE;
            }
            outputStream = new FileOutputStream(fl);
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, mCharsetString));
            writer.write(mData);
            result = ERROR_CODE_SUCCESS;
        } catch (Exception ioe) {
            ioe.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (result < 0) {
            mfilename = null;
        }

        return result;
    }

    public String getSavedFilename() {
        return mfilename;
    }

    public Uri getSavedFileUri() {
        File file = new File(mfilename);
        return Uri.fromFile(file);
    }

    private String getFileName(String destDir) {
        CRC32 crc = new CRC32();
        crc.update(mData.getBytes());
        return MMC_DIR + destDir + String.valueOf(crc.getValue()) + ".ics";
    }

    /**
     * Read event from the event id of the calendar event table and and convert
     * it to ics file and save it to SDcard
     *
     * @param eventId The row id of the event table
     * @param filename should be specified ,if filename is null the checksum of
     *            ical is used as filename
     * @param destDir destination directory location were file will be saved
     */
    public int saveToSDCard(long eventId, String filename, String destDir) {
        composeVcal(eventId, mContext, "2.0");
        return saveToMMC(filename, destDir);
    }

    /**
     * Decodes vcf content uri to byte array
     *
     * @param context
     * @param uri
     * @return
     */
    private static byte[] getVCalDataFromContent(Context context,Uri uri) {
        byte[] data =null;
        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(uri);
            data = new byte[input.available()];
            input.read(data);
            input.close();

        } catch (Exception e) {
            Log.w(TAG, "IOException caught while opening stream", e);
            return null;
        }
        return data;
    }


    /**
     * Returns vcard name as ContactName.vcf
     *
     */
    public static String getVcalName(Context context, Uri uri) {

        // TODO
        return "Jyothi.vcal";
    }

    /**
     * Provides destination file path,where file is saved
     * @return
     */
    public Uri getDestinationFileUri(){
        if(mfilename != null) {
            Uri fileuri =  Uri.parse("file://" + mfilename);
            return fileuri;
        } else {
            return null;
        }

    }

    /**
    * it saves file to the calendar app data files path
    * @param filename of the file to be saved
    */
    private boolean saveToPackageDir(String filename) {
        boolean success = false;
        if(mData == null){
            return success;
        }

        FileOutputStream fOut = null;
        OutputStreamWriter osw = null;

        try {
            fOut = mContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
            osw = new OutputStreamWriter(fOut);
            osw.write(mData);
            osw.flush();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(osw!=null){
                    osw.close();
                }
                if(fOut!=null){
                    fOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    /**
     * returns the UID of the calendar event parsed from the ICS file
     */
    public String getICalUID() {
        return mCalendarEvent != null ? mCalendarEvent.uid : null;
    }

    /**
     * returns the account id (_sync_local_id) from the calendar cursor
     */
    public long getAccountId() {
        return mAccountId;
    }

    public Uri getEventUri() {
        return mEventUri;
    }

    /**
     * Returns true if the parsed ics is vtodo
     *
     * @return
     */
    public boolean isTypeVtodo() {
        return (mCalendarEvent != null && mCalendarEvent.isVtodo);
    }

    /**
     * Invokes Vtodo Viewer if the app is enabled
     *
     * @param ctx
     * @return success if true
     */
    public boolean invokeVtodoUi(Context ctx) {
        // don't support VTODO now
        return false;
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
    private static Uri getScratchCalendarByName(final Context context, String name, String displayName) {

        Uri calendarUri = null;
        //BEGIN mot 2010/09/27 IKMAIN-4462:The event name issue when importing ICS file
        int color = Color.CYAN;
        //END
        ContentResolver contentResolver = context.getContentResolver();
        final Account kAccount = ICAL_ACCOUNT_INFO;

        Uri uri = decorateForScratch(Calendars.CONTENT_URI);

        // (1) Does the scratch calendar already exist?
        final String kSelection = Calendars.ACCOUNT_NAME+ " = ? AND " +
            Calendars.ACCOUNT_TYPE + " = ? AND " +
            Calendars.NAME + " = ?";

        final String[] kSelectionArgs = new String[] { kAccount.name, kAccount.type, name };
        final String[] kProjection = new String[] { Calendars._ID };

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, kProjection, kSelection, kSelectionArgs, null);
            if ((cursor != null) && (cursor.moveToNext())) {
                final long kCalendarId = cursor.getInt(0);
                calendarUri = ContentUris.withAppendedId(uri, kCalendarId);
            } else {
                // (2) No -- create the calendar.
                ContentValues values = new ContentValues();
                values.put(Calendars.ACCOUNT_NAME, kAccount.name);
                values.put(Calendars.ACCOUNT_TYPE, kAccount.type);
                values.put(Calendars.NAME, name);
                values.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
                values.put(Calendars.CALENDAR_COLOR, Integer.valueOf(color));
                values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
                values.put(Calendars.OWNER_ACCOUNT, "local@myDevice.com");
                values.put(Calendars.VISIBLE, 0);
                values.put(Calendars.SYNC_EVENTS, 0);
                TimeZone phoneTimezone = TimeZone.getDefault();
                String timezoneJava = phoneTimezone.getID();
                String phoneTimezoneId = timezoneJava;
                values.put(Calendars.CALENDAR_LOCATION, phoneTimezoneId);  //store default for debug
                values.put(Calendars.CALENDAR_TIME_ZONE, phoneTimezoneId);

                // change for icecream
                // pretend that we're sync adapter since only sync adapters can insert an calendar account
                Uri.Builder uriBuilder = uri.buildUpon();
                uriBuilder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, String.valueOf(true));
                uriBuilder.appendQueryParameter(Calendars.ACCOUNT_NAME, kAccount.name);
                uriBuilder.appendQueryParameter(Calendars.ACCOUNT_TYPE, kAccount.type);
                uri = uriBuilder.build();

                calendarUri = contentResolver.insert(uri, values);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return calendarUri;
    }

    private static Uri decorateForScratch(Uri uri) {
        if (uri != null) {
            Uri.Builder uriBuilder = uri.buildUpon();
            uriBuilder.appendQueryParameter(ICalConstants.MOTO_VISIBILITY, String.valueOf(false));
            uri = uriBuilder.build();
        }

        return uri;
    }

	public String InsertICal(Context mContext2, String string) {		
		
		        try {
		            VCalUtils vc = new VCalUtils();
		            CalendarEvent ce = vc.parseVcal(string);
		            if(ce == null){
//		                showToast(context,blur.res.R.string.unknown_error);
		                return null;
		            }else{
		                int error = vc.pushIntoCalendarProvider(mContext2, -1);
		                if (error != ERROR_CODE_SUCCESS) {
							return null;
						} else {
			                return ce.summary;
						}
		                /*
		                switch(error){
		                    case ERROR_CODE_ACCOUNT_NOT_SETUP:
		                    case ERROR_CODE_NO_CALENDAR_TABLE_SETUP:
		                        showToast(context,blur.res.R.string.setup_account);
		                        break;
		                    case ERROR_CODE_UNKNOWN:
		                        showToast(context,blur.res.R.string.unknown_error);
		                        break;
		                    case ERROR_CODE_EVENT_EXISTS:
		                        showToast(context,blur.res.R.string.event_exists);
		                        break;
		                    case ERROR_CODE_SUCCESS:
		                        showToast(context, blur.res.R.string.saved_to_calendar);
		                        break;
		                    default:
		                        break;
		                }
		                */
		            }
		        }catch(Exception ex){
		                ex.printStackTrace();
//		                showToast(context, blur.res.R.string.not_able_view_save_vcs);
		                return null;
		        }
	}
	
	
	public CalendarEvent parseVcal(String data) {
        try {
            if(TextUtils.isEmpty(data)){
                 Log.v(TAG,"empty iCal");
                return null;
             }
            data = unfoldText(data);            

            VCalParser mParser;
            if(data.contains("VERSION:1.0")){
                mParser = new VCalParser_V1();
            } else if ( data.contains("VERSION:2.0")){
                mParser = new VCalParser_V2();
            } else {
            		Log.v(TAG,"unsupported version");   
                return null;
            }

            Collection<CalendarEvent> event = mParser.parse(data);
            if (event != null) {
                 Iterator<CalendarEvent> it = event.iterator();
                 while (it.hasNext()) {
                     mCalendarEvent = it.next();
                     break;
                 }
             }
        } catch (FormatException e) {
                e.printStackTrace();
                return null;
        } catch (Exception e) {
            Log.w(TAG, "Couldn't read ICS file");
            return null;
        } 
        return mCalendarEvent;
    }

	public Cursor queryCal(Context mContext2) {
		//String where = "_sync_account_type = "+"'com.motorola.calendar' and " + "deleted <> 1";
		String where = "calendar_id = "+"1 and " + "deleted <> 1";
        Cursor cursor = mContext2.getContentResolver().query(CalendarContract.Events.CONTENT_URI, null, where, null, null);
        return cursor;
	}

	public String getICal(Context mContext2, Cursor cursor, StringBuffer subject) {
		mTxVcal = "2.0";
        mData = null;
        try {
           if (cursor != null) {
                mCalendarEvent = getCalendarEvent(cursor, mContext2.getContentResolver());
                if (mCalendarEvent.summary != null && !mCalendarEvent.summary.equals("")) {
                    subject.delete(0, subject.length()).append(mCalendarEvent.summary);
                } else {
               	 subject.delete(0, subject.length()).append("Calendar");
				}
                createCalendarData();
            }
       } catch (Exception e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }
        return mData;
	}
	
	/**
	    * creates a CALENDAR of Version 2.0 format
	    */
	    private void createCalendarData(){
	       if ("2.0".equals(mTxVcal)) {
	           VCalComposer ical = new VCalComposer_V2(mCalendarEvent);
	           mData = ical.composeVCal();
	       }else{	          
	               Log.v(TAG,"mTxVcal error");	           
	       }
	    }

}
