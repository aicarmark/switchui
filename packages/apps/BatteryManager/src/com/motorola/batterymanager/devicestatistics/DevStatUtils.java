/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.RawContacts;

//import android.provider.Gmail;

import com.motorola.batterymanager.Utils;

/**
 * Provides statistics of MO & MT for calls and SMS
 */

public class DevStatUtils {
    final static String TAG = "DevStatUtils";
    final static Uri SMS_SENT_FOLDER = Uri.parse("content://sms/sent");
    final static Uri SMS_INBOX_FOLDER = Uri.parse("content://sms/inbox");
    final static int UPTO_1_MINS_INDEX = 0;
    final static int UPTO_10_MINS_INDEX = 1;
    final static int UPTO_30_MINS_INDEX = 2;
    final static int UPTO_1_HRS_INDEX = 3;
    final static int MORE_1_HRS_INDEX = 4;

    private static int getDurCatagory(long dur) {
        int catagory = 0;
        if (dur <= 1L) catagory = UPTO_1_MINS_INDEX;
        else if (dur > 1L && dur <=10) catagory = UPTO_10_MINS_INDEX;
        else if (dur > 10L && dur <=30) catagory = UPTO_30_MINS_INDEX;
        else if (dur > 30L && dur <=60) catagory = UPTO_1_HRS_INDEX;
        else catagory = MORE_1_HRS_INDEX;

        return catagory;
    }

    /**
     * This method collects MO call details; if call logs are not cleared at this point
     * @param ctx context object
     * @return String Containing Call details
     */
    public static String getCallDetails(Context ctx) {
        final int DUR_CATAGORY = 5;
        StringBuilder sb = new StringBuilder();
        StringBuilder sel = new StringBuilder("(");
        sel.append("(").append(Calls.TYPE).append("=").append(Integer.toString(Calls.OUTGOING_TYPE)).append(")");
        sel.append(" OR ");
        sel.append("(").append(Calls.TYPE).append("=").append(Integer.toString(Calls.INCOMING_TYPE)).append(")");
        sel.append(")");
        String filter = sel.toString();

        boolean didOneMO = false; 
        boolean didOneMT = false;
        Cursor cursor = ctx.getContentResolver().query(Calls.CONTENT_URI,
                new String[]{Calls.DATE, Calls.DURATION, Calls.TYPE}, filter, null, null);
        if (cursor != null) {
            int count = cursor.getCount();
            if ((count > 0) && cursor.moveToFirst()) {
                int durIndex = cursor.getColumnIndexOrThrow(Calls.DURATION);
                int dateIndex = cursor.getColumnIndexOrThrow(Calls.DATE);
                int typeIndex = cursor.getColumnIndexOrThrow(Calls.TYPE);
                int nMOCalls = 0, nMTCalls = 0;

                // Get LastCheckin Time
                DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
                long lastCITime = prefs.getDevStatLastCheckinTime(0L);

                int[] moDuration = new int[DUR_CATAGORY];
                int[] mtDuration = new int[DUR_CATAGORY];
                do {
                    // Get Call time & Type
                    long callTime = cursor.getLong(dateIndex);
                    int callType = cursor.getInt(typeIndex);
                    int index = getDurCatagory(cursor.getLong(durIndex)/60);
                    // Checkin Call details only if call happened after last checkin time
                    if ((lastCITime != 0L) && (callTime > lastCITime)) {
                        if (callType == Calls.OUTGOING_TYPE) {
                            moDuration[index]++;
                            didOneMO = true;
                            nMOCalls++;
                        }else if (callType == Calls.INCOMING_TYPE) {
                            mtDuration[index]++;
                            didOneMT = true;
                            nMTCalls++;

                        }
                    } 
                } while(cursor.moveToNext());

                if (didOneMO || didOneMT) {
                    sb.append("callmo=" + nMOCalls + ";callmt=" + nMTCalls + ";");
                    // Checkin only if data is present;
                    if (didOneMO) {
                        sb.append("moc1=" + moDuration[UPTO_1_MINS_INDEX]  + 
                            ";moc1-10=" + moDuration[UPTO_10_MINS_INDEX] + ";moc10-30=" + 
                            moDuration[UPTO_30_MINS_INDEX] + ";moc30-60=" + moDuration[UPTO_1_HRS_INDEX] +
                            ";moc60+=" + moDuration[MORE_1_HRS_INDEX] + ";");
                    }

                    if (didOneMT) {
                        sb.append("mtc1=" + mtDuration[UPTO_1_MINS_INDEX]  + 
                            ";mtc1-10=" + mtDuration[UPTO_10_MINS_INDEX] + ";mtc10-30=" + 
                            mtDuration[UPTO_30_MINS_INDEX] + ";mtc30-60=" + mtDuration[UPTO_1_HRS_INDEX] +
                            ";mtc60+=" + mtDuration[MORE_1_HRS_INDEX] + ";");
                    }
               }
            }
            cursor.close();
        }
        return (didOneMO || didOneMT)?sb.toString():null;
    }

    /** 
     * This method collects MO&MT SMS details, if user has not cleared Sent/Inbox Folder
     * @param ctx context object
     */
    public static String getSMSDetails(Context ctx) {
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

        return ((nMO>0)||(nMT>0))? new String("smsmo="+nMO+";smsmt="+nMT+";"):null;
    }

    /** 
     * This method collects contacts details; For getting details of blur accounts
     * this method uses blur_source table. If Blur not present, this would return 0 records
     * In that case, it will log total contacts without any segregation.
     * @param ctx context object
     */
    public static String getContactsDetails(Context ctx) {
       final Uri CONTENT_URI = Uri.parse("content://com.motorola.blur.contacts.extensions/blur_sources");
       Cursor c1 = ctx.getContentResolver().query(CONTENT_URI, 
               new String[] {"pretty_name", "blur_account_id"}, null, null, null);
       String logStr = null;
       if (c1 != null) {
           boolean didOne = false;
           StringBuilder sb = new StringBuilder();
           if (c1.getCount() > 0 && c1.moveToFirst()) {
               do {
                   long accountId = c1.getLong(1);
                   int nContacts = getNumOfContacts(ctx, accountId);
                   if (nContacts > 0) {
                       sb.append("[ID=Contacts;acc=" + c1.getString(0) + ";noc=" + 
                               nContacts + ";]");
                       didOne = true;
                   }
               } while (c1.moveToNext());
               if (didOne) {
                   logStr = sb.toString();
               }
           }
           c1.close();
       } else {
           Cursor c2 = ctx.getContentResolver().query(RawContacts.CONTENT_URI,
                       null, null, null, null); 
           if (c2 != null) {
              int nRec = c2.getCount();
              if (nRec > 0) {
                  logStr = new String("[ID=Contacts;totalContacts=" + nRec + ";]");
              }
              c2.close();
           }
       }
       return logStr;
   }

   public static String getEmailDetails(Context ctx) {
       final Uri PROVIDER_URI = Uri.parse("content://com.motorola.blur.setupprovider/providers");
       final Uri ACCOUNT_URI = Uri.parse("content://com.motorola.blur.setupprovider/accounts");

       Cursor providers = ctx.getContentResolver().query(PROVIDER_URI, new String[]{"_id"}, null, null, null);
       StringBuilder sb = new StringBuilder();
       boolean didOne = false;
       if (providers != null) {
           if (providers.getCount() > 0 && providers.moveToFirst()) {
               do {
                   String where = new String("(provider_id = ?)");
                   String[] whereArgs = new String[] { String.valueOf(providers.getLong(0)) };
                   Cursor accounts = ctx.getContentResolver().query(ACCOUNT_URI, 
                           new String[] { "_id", "account_pretty_name" }, where, whereArgs, null);
                   if (accounts != null) {
                       if (accounts.getCount() > 0 && accounts.moveToFirst()) {
                           do {
                               long accountId = accounts.getLong(0);
                               int nEmails = getNumOfEmails(ctx, accountId);
                               if (nEmails > 0) {
                                   sb.append("[ID=Emails;acc=" + accounts.getString(1) + ";" +
                                           "noe=" + nEmails + ";]");
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
                      Utils.Log.v(TAG, "Google Account : " + acc[i].name + " : " + acc[i].type);
                      //Uri cu = Uri.parse("content://" + Gmail.AUTHORITY + "/conversations/" + acc[i].name);
                      Uri cu = Uri.parse("content://" + "gmail" + "/conversations/" + acc[i].name);

                      Cursor cursor = cr.query(cu, null, "", null, null);
                      if(cursor != null) {
                          long numConversations = cursor.getCount(); 
                          if(numConversations > 0) {
                              Utils.Log.v(TAG, "Num of conv: " + numConversations);
                              sb.append("[ID=Emails;acc=Gmail" + j + ";" +
                                           "noe=" + numConversations + ";]");
                              didOne = true; 
                              j++;
                          }
                          cursor.close();
                      }
                  }
              }
       }
       return (didOne)?sb.toString():null;
   }

   private static int getNumOfContacts(Context ctx, long accountId) {
       String sel = new String("(" + RawContacts.SYNC1 + "= ? )"); 
       Cursor c = ctx.getContentResolver().query(RawContacts.CONTENT_URI,
               null, sel, new String[]{String.valueOf(accountId)}, null); 
       int nRec = 0;
       if (c != null) {
           nRec = c.getCount();
           c.close();
       }
       return nRec;
   }

   private static int getNumOfEmails(Context ctx, long accountId) {
       final Uri EMAIL_URI = Uri.parse("content://" + 
               "com.motorola.blur.service.email.engine.EmailProvider/messages");
       Cursor c = ctx.getContentResolver().query(EMAIL_URI, 
               new String[] { "count(*) as count" }, 
               "account_id = " + accountId, null, null);
       int nEmail = 0;
       if (c != null) {
           c.moveToFirst();
           nEmail = c.getInt(0);
           c.close();
       }
       return nEmail;
   }
}
