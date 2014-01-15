/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static com.motorola.devicestatistics.DevStatPrefs.DEVSTATS_BATTCAP_TIME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.RawContacts;
import android.util.Base64;
import android.util.Log;

//import android.provider.Gmail;


/**
 * Provides statistics of MO & MT for calls and SMS
 */

public class DevStatUtils implements DeviceStatsConstants {
    final static String TAG = "DevStatUtils";
    final static Uri SMS_SENT_FOLDER = Uri.parse("content://sms/sent");
    final static Uri SMS_INBOX_FOLDER = Uri.parse("content://sms/inbox");
    final static int UPTO_1_MINS_INDEX = 0;
    final static int UPTO_10_MINS_INDEX = 1;
    final static int UPTO_30_MINS_INDEX = 2;
    final static int UPTO_1_HRS_INDEX = 3;
    final static int MORE_1_HRS_INDEX = 4;

    private static final String US_ASCII_CHARACTER_SET = "US-ASCII";
    private static final String INVALID_ENCODED_STRING = "URLENCODE_EXCEPTION";
    private static final boolean DUMP = GLOBAL_DUMP;

    private static final long BATTCAP_PERIODICITY_MS = 7 * DAY_IN_MILLIS;

    private static int getDurCatagory(double dur) {
        int catagory = 0;
        if (dur <= 1) catagory = UPTO_1_MINS_INDEX;
        else if (dur > 1.0 && dur <= 10.0) catagory = UPTO_10_MINS_INDEX;
        else if (dur > 10.0 && dur <= 30.0) catagory = UPTO_30_MINS_INDEX;
        else if (dur > 30.0 && dur <= 60.0) catagory = UPTO_1_HRS_INDEX;
        else catagory = MORE_1_HRS_INDEX;

        return catagory;
    }

    /**
     * This method collects MO call details; if call logs are not cleared at this point
     * @param ctx context object
     * @return String Containing Call details
     */
    public static boolean addCallDetails(DsCheckinEvent event, Context ctx) {
        final int DUR_CATAGORY = 5;
        final long ONE_DAY_MS = 24 * 3600 * 1000L;
        // Get LastCheckin Time
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long lastCITime = prefs.getDevStatLastDailyCheckinTime(0L);

        if(lastCITime == 0)
            lastCITime = System.currentTimeMillis() - ONE_DAY_MS;

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("(").append(Calls.TYPE).append("=").append(Integer.toString(Calls.OUTGOING_TYPE)).append(")");
        sb.append(" OR ");
        sb.append("(").append(Calls.TYPE).append("=").append(Integer.toString(Calls.INCOMING_TYPE)).append(")");
        sb.append(")");
        sb.append(" AND (" + Calls.DATE + " > " + lastCITime + ")");
        String filter = sb.toString();

        sb.setLength(0);

        boolean didOneMO = false; 
        boolean didOneMT = false;
        long totalCallDuration = 0;
        Cursor cursor = ctx.getContentResolver().query(Calls.CONTENT_URI,
                new String[]{Calls.DATE, Calls.DURATION, Calls.TYPE}, filter, null, null);
        if (cursor != null) {
            int count = cursor.getCount();
            if ((count > 0) && cursor.moveToFirst()) {
                int durIndex = cursor.getColumnIndexOrThrow(Calls.DURATION);
                int typeIndex = cursor.getColumnIndexOrThrow(Calls.TYPE);
                int nMOCalls = 0, nMTCalls = 0;

                int[] moDuration = new int[DUR_CATAGORY];
                int[] mtDuration = new int[DUR_CATAGORY];
                do {
                    int callType = cursor.getInt(typeIndex);
                    long callDuration = cursor.getLong(durIndex);
                    totalCallDuration+=callDuration;
                    int index = getDurCatagory(callDuration/(60.0));
                    if (callType == Calls.OUTGOING_TYPE) {
                        moDuration[index]++;
                        didOneMO = true;
                        nMOCalls++;
                    }else if (callType == Calls.INCOMING_TYPE) {
                        mtDuration[index]++;
                        didOneMT = true;
                        nMTCalls++;
                    }
                } while(cursor.moveToNext());

                if (didOneMO || didOneMT) {
                    event.setValue("callmo", nMOCalls);
                    event.setValue("callmt", nMTCalls);

                    // Checkin only if data is present;
                    if (didOneMO) {
                        event.setValue("moc1", moDuration[UPTO_1_MINS_INDEX]);
                        event.setValue("moc1-10", moDuration[UPTO_10_MINS_INDEX] );
                        event.setValue("moc10-30", moDuration[UPTO_30_MINS_INDEX] );
                        event.setValue("moc30-60", moDuration[UPTO_1_HRS_INDEX] );
                        event.setValue("moc60+", moDuration[MORE_1_HRS_INDEX] );
                    }

                    if (didOneMT) {
                        event.setValue("mtc1", mtDuration[UPTO_1_MINS_INDEX]);
                        event.setValue("mtc1-10", mtDuration[UPTO_10_MINS_INDEX]);
                        event.setValue("mtc10-30", mtDuration[UPTO_30_MINS_INDEX]);
                        event.setValue("mtc30-60", mtDuration[UPTO_1_HRS_INDEX]);
                        event.setValue("mtc60+", mtDuration[MORE_1_HRS_INDEX]);
                    }
                    event.setValue("ttlcalldur", totalCallDuration);
               }
            }
            cursor.close();
        }
        return (didOneMO || didOneMT) ? true : false;
    }

    /** 
     * This method collects MO&MT SMS details, if user has not cleared Sent/Inbox Folder
     * @param ctx context object
     */
    public static boolean addSMSDetails(DsCheckinEvent event, Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(SMS_SENT_FOLDER, null, null, null, null);
        int nMO = 0, nMT = 0;
        if (cursor != null) {
            nMO = cursor.getCount();
            cursor.close();
        }

        cursor = ctx.getContentResolver().query(SMS_INBOX_FOLDER, null, null, null, null);
        if (cursor != null) {
            nMT = cursor.getCount();
            cursor.close();
        }

        if ((nMO>0)||(nMT>0)) {
            event.setValue("smsmo", nMO);
            event.setValue("smsmt", nMT);
            return true;
        }
        return false;
    }

    /** 
     * This method collects contacts details; For getting details of blur accounts
     * this method uses blur_source table. If Blur not present, this would return 0 records
     * In that case, it will log total contacts without any segregation.
     * @param ctx context object
     */
    public static boolean getContactsDetails(DsCheckinEvent event, Context ctx) {
        boolean updated = false;
       //TODO: Need to find a way to report contacts per account
       Cursor c1 = null;
       if (c1 != null) {
           if (c1.getCount() > 0 && c1.moveToFirst()) {
               do {
                   long accountId = c1.getLong(1);
                   String prettyName = c1.getString(0);
                   int nContacts = getNumOfContacts(ctx, accountId, prettyName, c1.getString(2));
                   if (nContacts > 0) {
                       CheckinHelper.addNamedSegment(event, "Contacts", "acc", prettyName, "noc",
                               String.valueOf(nContacts) );
                       updated = true;
                   }
               } while (c1.moveToNext());
           }
           c1.close();
       } else {
           Cursor c2 = ctx.getContentResolver().query(RawContacts.CONTENT_URI,
                       null, null, null, null); 
           if (c2 != null) {
              int nRec = c2.getCount();
              if (nRec > 0) {
                  CheckinHelper.addNamedSegment(event, "Contacts", "totalContacts",
                          String.valueOf(nRec) );
                  updated = true;
              }
              c2.close();
           }
       }
       return updated;
   }

   public static boolean getEmailDetails(DsCheckinEvent event, Context ctx) {
       final Uri PROVIDER_URI = Uri.parse("content://com.motorola.blur.setupprovider/providers");
       final Uri ACCOUNT_URI = Uri.parse("content://com.motorola.blur.setupprovider/accounts");

       Cursor providers = ctx.getContentResolver().query(PROVIDER_URI, new String[]{"_id"}, null, null, null);
       boolean didOne = false;
       if (providers != null) {
           if (providers.getCount() > 0 && providers.moveToFirst()) {
               do {
                   String where = "(provider_id = ?)";
                   String[] whereArgs = new String[] { String.valueOf(providers.getLong(0)) };
                   Cursor accounts = ctx.getContentResolver().query(ACCOUNT_URI, 
                           new String[] { "_id", "account_pretty_name" }, where, whereArgs, null);
                   if (accounts != null) {
                       if (accounts.getCount() > 0 && accounts.moveToFirst()) {
                           do {
                               long accountId = accounts.getLong(0);
                               int nEmails = getNumOfEmails(ctx, accountId);
                               if (nEmails > 0) {
                                   CheckinHelper.addNamedSegment(event, "Emails",
                                           "acc", accounts.getString(1),
                                           "noe", String.valueOf(nEmails) );
                                   didOne = true;
                               }
                           } while (accounts.moveToNext());
                       }
                       accounts.close();
                   }
               } while (providers.moveToNext());
           }
           providers.close();
       }
       // Do Gmail accounts as well - for now we get only conversations
       AccountManager am = AccountManager.get(ctx);
       Account[] acc = am.getAccounts();
       ContentResolver cr = ctx.getContentResolver();
       if(acc != null && acc.length > 0) {
           int j = 1;
           for(int i = 0; i < acc.length; ++i) {
               if(acc[i].type.contains("google")) {
                   int numAllConversation = 0;
                   int numSyncConversations = 0;

                   // Get total conversations for this Gmail account
                   Uri GMAIL_LABEL_URI = Uri.parse("content://gmail-ls/" + acc[i].name + "/labels/0");
                   Cursor labels = cr.query(GMAIL_LABEL_URI, null, null, null, null);
                   if(labels != null) {
                       if (labels.getCount() > 0 && labels.moveToFirst()) {
                           int cnameColumn = labels.getColumnIndex("canonicalName");
                           int numConvColumn = labels.getColumnIndex("numConversations");
                           do {
                               String cname = labels.getString(cnameColumn);
                               if (cname.equals("^all")) {
                                   String numConv = labels.getString(numConvColumn);
                                   numAllConversation = Integer.valueOf(numConv);
                                   break;
                               }
                           } while (labels.moveToNext());
                       }
                       labels.close();
                   }

                   // Get Synced-Email count for this Gmail account
                   Uri CONV_GMAIL_URI = Uri.parse("content://gmail-ls/" + acc[i].name + "/conversations");
                   Cursor conv = cr.query(CONV_GMAIL_URI, null, null, null, null);
                   if(conv != null) {
                       numSyncConversations = conv.getCount();
                       conv.close();
                   }

                   // If either one is non-zero, log the details.
                   if ((numSyncConversations > 0) || (numAllConversation > 0)) {
                       CheckinHelper.addNamedSegment(event, "Emails", "acc", "Gmail" + j,
                               "tot", String.valueOf(numAllConversation),
                               "noe", String.valueOf(numSyncConversations));
                       didOne = true;
                       j++;
                   }
               } // end of if:google
           }
       }
       return didOne;
   }

   private static int getNumOfContacts(Context ctx, long accountId, String pName, String acName) {
       String where = null;
       String[] whereArgs = null;
       // Blur has accountId as '0'; even unconnected contacts, google is set to
       // '0'; Hence this check.
       if (accountId == 0L && !pName.equals("Motorola")) {
           where = ("(" + RawContacts.ACCOUNT_NAME + "= ?)");
           whereArgs = new String[] { acName };
       } else {
           where = ("(" + RawContacts.SYNC1 + "= ? )");
           whereArgs = new String[] { String.valueOf(accountId) };
       }

       Cursor c = ctx.getContentResolver().query(RawContacts.CONTENT_URI,
               null, where, whereArgs, null);
       int nRec = 0;
       if (c != null) {
           nRec = c.getCount();
           c.close();
       }
       return nRec;
   }

   private static int getNumOfEmails(Context ctx, long accountId) {
       final Uri EMAIL_URI = Uri.parse("content://" + 
               "com.android.email.provider/message");
       Cursor c = ctx.getContentResolver().query(EMAIL_URI, 
               new String[] { "count(*) as count" }, 
               "accountKey = " + accountId, null, null);
       int nEmail = 0;
       if (c != null) {
           c.moveToFirst();
           nEmail = c.getInt(0);
           c.close();
       }
       return nEmail;
   }

    public static String getBattTotalCapacity() {
        String uevent = Utils.collectBatteryStats();
        boolean qcom = false;
        if(uevent != null) {
            String[] split = uevent.split("POWER_SUPPLY_CHARGE_FULL_DESIGN=");
            if (split.length == 1) {
                split = uevent.split("POWER_SUPPLY_ENERGY_FULL=");
                qcom = true;
            }
            if(split.length > 1) {
                String[] capacity = split[1].split("\n");
                if(capacity.length > 0) {
                    String ret = capacity[0];
                    try {
                        if (qcom) {
                            // The value read for qualcomm chipsets is sometime negative because
                            // it goes outside the bounds of 32 bit signed number
                            // So convert the number to 32 bit unsigned and divide by 1000000 to
                            // get in units of mAh
                            Long retLong = Long.valueOf(ret);
                            ret = String.valueOf((retLong & 0xffffffffL)/1000000);
                        } else if ("x86".equals(android.os.Build.CPU_ABI)) {
                            // Scorpion mini - Intel values need to be divided by 1000 to get in
                            // units of mAH
                            Long retLong = Long.valueOf(ret);

                            // Sanity check so that value after division is in 100..50000
                            if (retLong >= 100 * 1000 && retLong < 50000 * 1000) {
                                ret = String.valueOf(retLong / 1000);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "BattCap " + ret, e);
                    }
                    return ret;
                }
            }
        }
        return null;        
    }

    public static void addBattCapLog(long checkinTimeSec, ArrayList<DsCheckinEvent> logs, Context ctx) {
        String cap = getBattTotalCapacity();
        if (cap == null) return;

        DevStatPrefs pref = DevStatPrefs.getInstance(ctx);
        String lastCap = pref.getStringSetting(DevStatPrefs.DEVSTATS_BATT_CAP);

        int mAh = 0;
        try {
            mAh = Integer.parseInt(cap);
        }catch(NumberFormatException nfEx) {
            Log.e(TAG, "Error parsing battcap " + cap, nfEx);
            // if current capacity cannot be parsed, don't report anything
            return;
        }

        int oldMAh = -1;
        // if capacity was never reported before, report it now
        boolean report = (lastCap == null);
        if (report == false) {
            try {
                oldMAh = Integer.parseInt(lastCap);

                // if capacity has changed, report it now
                report = (mAh != oldMAh);
            }catch(NumberFormatException nfEx) {
                Log.e(TAG, "Error parsing oldcap " + lastCap, nfEx);
                // if old capacity cannot be parsed, report current capacity now.
                report = true;
            }
        }

        if (report == false) {
            // Check if at least 7 days (ignore DST for now) have elapsed since last battcap report
            report = hasPeriodicEventExpired(pref, DEVSTATS_BATTCAP_TIME, BATTCAP_PERIODICITY_MS);
        }

        if (report) {
            logs.add( CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID, "BattCap",
                    DevStatPrefs.VERSION, checkinTimeSec, "cap", cap ) );

            pref.setStringSetting(DevStatPrefs.DEVSTATS_BATT_CAP, cap);

            notePeriodicEvent(pref, DEVSTATS_BATTCAP_TIME);
        }
    }

    public static void addCpuFreqLog(ArrayList<DsCheckinEvent> logs, long checkinTime,
            boolean triggeredByUsb) {
        boolean updated = false;
        String tag = triggeredByUsb ? DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER :
            DevStatPrefs.CHECKIN_EVENT_ID;
        DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent( tag,
                "CpuFreq", DevStatPrefs.VERSION, checkinTime );

        for(int i = 0; i < 2; ++i) {
            String cpuStates = Utils.readSysFile(
                    "/sys/devices/system/cpu/cpu" + i + "/cpufreq/stats/time_in_state", 512);
            if(cpuStates != null) {
                String[] states = cpuStates.split("\n");
                DsSegment segment = null;
                for(int j = 0; states != null && j < states.length; ++j) {
                    String state = states[j];
                    if(state != null) {
                        String[] freqTime = state.split(" ");
                        if(freqTime != null && freqTime.length == 2) {
                            if(segment == null) {
                                segment = CheckinHelper.createNamedSegment(String.valueOf(i));
                                checkinEvent.addSegment(segment);
                                updated = true;
                            }
                            segment.setValue(freqTime[0], freqTime[1]);
                        }
                    }
                }
            }
        }

        if(updated) logs.add(checkinEvent);
    }

    public static final String serializeObject( Object o ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject( o );
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_PADDING|Base64.NO_WRAP );
        } catch ( Exception e ) {
            Log.e( TAG, "serializeObject: " + e.toString() );
            return null;
        } finally {
            try {
                if (oos!=null) oos.close();
            } catch ( Exception e ) {
                Log.e( TAG, "serializeObject close: " + e.toString() );
            }
        }
    }

    static final void saveObjectToFile(Context context, String fileName, Object obj) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(context.openFileOutput( fileName, 0 ));
            oos.writeObject( obj );
        } catch ( Exception e ) {
            Log.e( TAG, Log.getStackTraceString(e) );
        } finally {
            try {
                if ( oos != null ) oos.close();
            } catch ( Exception e ) {
                Log.e( TAG, "saveObjectToFile close: " + e.toString() );
            }
        }
    }

    public static final Object deSerializeObject( String encoded, Class<?> classType ) {
        if ( encoded == null ) return null;

        ObjectInputStream ois = null;
        Object result = null;
        try {
            ois = new ObjectInputStream(
                    new ByteArrayInputStream( Base64.decode( encoded, Base64.NO_PADDING|Base64.NO_WRAP ) ) );
            result = ois.readObject();
            if (result!=null && !result.getClass().equals(classType)) result = null;
        } catch (Exception e) {
            Log.e( TAG, "Exception in deSerializeObject s=" + encoded + " e=" + e );
            result = null;
        } finally {
            try {
                if ( ois != null ) ois.close();
            } catch ( Exception e ) {
                Log.e( TAG, "deSerializeObject close:" + e.toString() );
            }
        }
        return result;
    }

    static final Object loadObjectFromFile( Context context, String fileName, Class<?> classType ) {
        Object result = null;

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream( context.openFileInput( fileName ) );
            result = ois.readObject();
        } catch ( Exception e ) {
            Log.e( TAG, "Ignoring exception in loadObjectFromFile f=" + fileName + " e=" + e );
        } finally {
            try {
                if ( ois != null ) ois.close();
            } catch ( Exception e ) {
                Log.e( TAG, "loadObjectFromFile close:" + e.toString() );
            }
        }

        if ( result != null && !result.getClass().equals(classType) ) {
            result = null;
        }

        return result;
    }

    /**
     * Get the millisecond time at the immediate next midnight
     * @param errorDefault Value to add to current time in case any error is encountered
     * @return Millisecond time at midnight
     */
    static final public long getMillisecsAtMidnight( long errorDefault ) {
        return getMillisecsAtMidnight(System.currentTimeMillis(), errorDefault);
    }

    /**
     * Get the millisecond time at the immediate next midnight of the specified time
     * @param errorDefault Value to add to current time in case any error is encountered
     * @return Millisecond time at midnight
     */
    static final public long getMillisecsAtMidnight( long forTime, long errorDefault ) {
        final long THIRTY_HOURS_MS = 30L * 60L * 60L * 1000L;
        long msToMidnight = 0;
        boolean error = true;

        Calendar calendar = Calendar.getInstance();
        if ( calendar != null ) {
            calendar.setTimeInMillis(forTime);

            calendar.set(
                    calendar.get( Calendar.YEAR ),
                    calendar.get( Calendar.MONTH ),
                    calendar.get( Calendar.DAY_OF_MONTH ),
                    0, 0, 0 ); // at 00:00:00 AM

            // The set above, doesn't change milliseconds. So change it explicitly
            calendar.set(Calendar.MILLISECOND, 0);

            // Move to 00:00 AM tomorrow, handling DST switch as well.
            calendar.add(Calendar.DAY_OF_MONTH,1);

            long newTime = calendar.getTimeInMillis();

            msToMidnight = newTime - forTime;

            // msToMidnight can vary from 1 millisec to 25:00:00.000 (in case of DST switch)
            if ( msToMidnight > 0 && msToMidnight < THIRTY_HOURS_MS ) error = false;
        }

        // If any error was encountered above, let the alarm expire at a default time
        if ( error == true ) msToMidnight = errorDefault;

        return forTime + msToMidnight;
    }

    static public final String getUrlEncodedString(String input) {
        if (input==null) return "NULL";
        try {
            return URLEncoder.encode(input, US_ASCII_CHARACTER_SET);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, input + " " + e);
            return INVALID_ENCODED_STRING;
        }
    }

    public final static String getStackTrace() {
        return Log.getStackTraceString(new Throwable());
    }

    public final static void logException(String tag, Exception exception) {
        Log.e(tag, Log.getStackTraceString(exception));
    }

    public final static void logExceptionMessage(String tag, String message, Exception exception) {
        Log.e(tag, message + " " + Log.getStackTraceString(exception));
    }

    /**
     * Get the pid of the system_server process
     * @param context Android context
     * @return pid of system_server process or INVALID_PID in case or error.
     */
    public final static long getSystemServerPid(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if ( processes != null ) {
            for ( RunningAppProcessInfo app: processes ) {
                if (SYSTEM_PROCESS_NAME.equals(app.processName)) {
                    if (DUMP) Log.d(TAG, "Found system process pid " + app.pid);
                    return app.pid;
                }
            }
        }
        Log.e(TAG, "No system process found");
        return INVALID_PID;
    }

    /**
     * Determine if an event that should happen at a specific frequency should occur now.
     * @param pref A DevStatPrefs instance required for persisting preferences
     * @param prefKey The key to be used for reading event occurrence time
     * @param periodicityMs The periodicity of this event
     * @return true if the duration at which the periodic event should happen, has expired
     */
    private static boolean hasPeriodicEventExpired(DevStatPrefs pref, String prefKey,
            long periodicityMs) {
        String lastReport = pref.getStringSetting(prefKey);
        // If it has never been reported before, report now
        if (lastReport == null) return true;

        long lastReportTimeMs;
        try {
            lastReportTimeMs = Long.parseLong(lastReport);
        } catch (NumberFormatException e) {
            Log.e(TAG, "periodiceventexpired", e);
            // If preferences is invalid, report now
            return true;
        }

        // If at least periodicityMs has elapsed since last report, report now
        return (Math.abs(System.currentTimeMillis() - lastReportTimeMs) >= periodicityMs);
    }

    /**
     * Save the current time as the occurence of the event identified by prefKey
     * @param pref A DevStatPrefs instance required for persisting preferences
     * @param prefKey The key to be used for storing event occurrence time
     */
    private static void notePeriodicEvent(DevStatPrefs pref, String prefKey) {
        pref.setStringSetting(prefKey, String.valueOf(System.currentTimeMillis()));
    }
}
