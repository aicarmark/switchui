package com.motorola.quicknote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.DateFormat;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.util.Log;

import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.QNSetReminder;
import com.motorola.quicknote.QNUtil;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Receives com.motorola.quicknote.action.ALERT intents and handles event
 * reminders. The intent URI specifies an alert id in the QuickNote Alerts
 * database table. This class also receives the BOOT_COMPLETED intent so that it
 * can add a status bar notification if there are Calendar event alarms that
 * have not been dismissed. The real work is done in the AlertService class.
 */
public class QNReceiver extends BroadcastReceiver {
	/*2012-12-11, add by amt_sunzhao for SWITCHUITWO-25 */
	/*
	 * android 4.0.3, external storage and internal storage are
	 * mounted at in turn: /mnt/sdcard 
	 */
	private static final String MNT_SDCARD_URI = "file:///mnt/sdcard";
	/*
	 * From JB(4.1.2), mount pointer info changed.
	 * external storage: /storage/sdcard0
	 * internal storage: /storage/sdcard1
	 */
	private static final String STORAGE_PREFIX = "/storage/sdcard";
	private static final String EXTERNAL_STORAGE_URI = "file:///storage/sdcard0";
	private static final String INTERNAL_STORAGE_URI = "file:///storage/sdcard1";
	/*2012-12-11, add end*/

    // We use one notification id for all events so that we don't clutter
    // the notification screen.  It doesn't matter what the id is, as long
    // as it is used consistently everywhere.
    //private static final int NOTIFICATION_ID = 0;

    private static final String TAG = "[QNReceiver]";


    @Override
    public void onReceive(Context context, Intent intent) {
        QNDev.log(TAG+"reminder: "+intent.getAction());
        Resources res = context.getResources();
        String action = intent.getAction();
        if (action == null ) { return; }

        if ("com.motorola.quicknote.action.ALERT".equals(action)) {
          QNDev.log(TAG+"QuickNote receive the intent from alarm");
          //send the notification to the status bar
          // RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alertstatusbar);

          Uri qnc_uri = intent.getData();       
          if (qnc_uri == null ) {
            return;
          } 
          QNDev.log(TAG+"reminder: alarm uri = "+qnc_uri.toString());

          //check whether this note exists or not
          if (!QuickNotesDB.isValidUri(context, qnc_uri)) {
            //not a valid uri, maybe this note has been delete
            QNDev.log(TAG+"uri = "+qnc_uri+" is not a valid uri, return!");
            return;
          }         

          try {
            //reset the reimider in DB
            QuickNotesDB.write(context, qnc_uri, QNColumn.REMINDER, 0L);  

            long noteId = ContentUris.parseId(qnc_uri);
            QNDev.log(TAG+"reminder: noteid = "+noteId);
            // int position = QuickNotesDB.getIndexByUri(context, qnc_uri);
            // int position = (Integer)QuickNotesDB.read(context, qnc_uri, QNColumn._ID);
            String sPosition = Long.toString(noteId);
            int position = Integer.parseInt(sPosition);
            QNDev.log(TAG+"reminder: position = "+position);

            String title = (String)QuickNotesDB.read(context, qnc_uri, QNColumn.TITLE);
            String type = (String)QuickNotesDB.read(context, qnc_uri, QNColumn.MIMETYPE);

            if (( null != title) && (title.length()>0)) {
               title = shortTitle(title);
               //views.setTextViewText(R.id.title, title);
            } else {
               if  (type.startsWith("text/")) {
                  //it is text note, show the first characters of the text note
                  String uri_string = (String)QuickNotesDB.read(context, qnc_uri, QNColumn.URI);
                  // views.setTextViewText(R.id.title, getTextNoteContentShort(uri_string));
                  title = getTextNoteContentShort(uri_string);
                  
               } else {
                  //other note except text,just show "no titile"
                  // views.setTextViewText(R.id.title, res.getString(R.string.no_title_label));
                  title = res.getString(R.string.no_title_label);
               }
            }
/*
            StringBuilder typeformat  =  new StringBuilder();
            typeformat.append("<");
            if  (type.startsWith("text/")) {
              typeformat.append(context.getString(R.string.type_text)).append(">");
            } else if  (type.startsWith("image/")) {
              typeformat.append(context.getString(R.string.type_image)).append(">");
            } else if  (type.startsWith("audio/")) {
              typeformat.append(context.getString(R.string.type_audio)).append(">");
            } else {
              typeformat.append(context.getString(R.string.type_unknown)).append(">");
            }

            views.setTextViewText(R.id.type, typeformat);
*/
            Calendar c = Calendar.getInstance();

            // StringBuilder timeString = formatTime(context, c);

            // views.setTextViewText(R.id.received_time, timeString);

            StringBuilder typeformat  =  new StringBuilder();
            if(!QNUtil.isTextLoad(context)) {
                typeformat.append("<");
                if  (type.startsWith("text/")) {
                    typeformat.append(context.getString(R.string.type_text)).append(">");
                } else if  (type.startsWith("image/")) {
                    typeformat.append(context.getString(R.string.type_image)).append(">");
                } else if  (type.startsWith("audio/")) {
                    typeformat.append(context.getString(R.string.type_audio)).append(">");
                } else {
                    typeformat.append(context.getString(R.string.type_unknown)).append(">");
                }
                // timeString.insert(0, "\t\t\t");
                // timeString.insert(0, typeformat.toString());
            }
/*
            Notification notification = new Notification();
            notification.contentView = views;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            //notification.flags |= Notification.DEFAULT_SOUND; 
            notification.defaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE;
            // notification.flags |= Notification.DEFAULT_VIBRATE; 
            notification.icon = R.drawable.stat_notify_quick_notes;
*/
            //send the intent to QNAlarmShow
            Intent nIntent = new Intent(context, QNAlarmShow.class);
            nIntent.setFlags (Intent.FLAG_ACTIVITY_NO_HISTORY
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS 
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);  
             nIntent.putExtra("qnUri", qnc_uri.toString());

             QNDev.log(TAG+"reminder: Send to QNAlarmShow with qnUri:"+ qnc_uri.toString());

             //notification.contentIntent = PendingIntent.getActivity(context, position, nIntent, PendingIntent.FLAG_UPDATE_CURRENT);
             //notification.contentIntent = PendingIntent.getActivity(context, 0, nIntent,0);
             //notification.contentIntent = PendingIntent.getActivity(context, 0, nIntent, PendingIntent.FLAG_CANCEL_CURRENT);
             PendingIntent pendingIntent = PendingIntent.getActivity(context, position, nIntent, PendingIntent.FLAG_UPDATE_CURRENT);
             Notification notification = this.createNotification(context, title, typeformat.toString(), pendingIntent);

             NotificationManager nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE); 
             nm.notify(position, notification);
             //if use 1 common status bar
             // nm.notify(0, notification);

             Intent intentUpdate = new Intent(QNConstants.INTENT_ACTION_NOTE_UPDATED);
             intentUpdate.putExtra("from","QNAlertReceiver");
             context.sendBroadcast(intentUpdate);
             // inform the NoteView to update the reminder icon
             Intent intentReminder = new Intent(QNConstants.INTENT_ACTION_REMINDER_TIME_OUT);
             intentReminder.putExtra("qnUri", qnc_uri.toString());
             context.sendBroadcast(intentReminder);

          } catch (Exception e) {
            QNDev.log("QNAlertReceiver"+ "catch exception: "+e);
          }
        } else if ("com.motorola.quicknote.action.QN_RESTORE_COMPLETE".equals(action)) {
           QNDev.log(TAG+"QuickNote receive the intent from SDBackup");
           String uri_string = intent.getStringExtra("Uri");
           Uri event_uri = QNUtil.buildUri(uri_string);

           if (null == event_uri) {
              return;
           }

           //check the prefix
           //checkAndUpdateDB(context, event_uri);
           
           //check the reminder
           long reminder = intent.getLongExtra("reminder", 0L);
           if (reminder == 0L) {
              return;
           }

           QNDev.log(TAG+"enable the alarm for "+event_uri+" at "+reminder);
           QNUtil.setAlarm(context, event_uri, reminder);
           /*2012-12-11, add by amt_sunzhao for SWITCHUITWO-25 */
       } else if ("android.intent.action.BOOT_COMPLETED".equals(action)
    		   || Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
    	   /*
    	    * When boot completed, sdcard may be not mounted, so when sdcard
    	    * mounted, check and update db again.
    	    */
    	   /*2012-12-11, add end*/
           //check the storage place and update the file dir in DataBase
           checkAndUpdateDB(context, null);
       }
    }


    //format the time as: 18:50
    private StringBuilder formatTime(Context context, Calendar calendar) {
       Date myDate = calendar.getTime();
       StringBuilder timeString =  new StringBuilder();
       timeString.append(DateFormat.getTimeFormat(context).format(myDate));
       return timeString;
   }


    private String shortTitle(String title) {
       if (title.length() <= 12) {
           return title;
       } else {
           return title.substring(0,12)+"...";
       }
    }

    private String getTextNoteContentShort(String uri_string)
    {
        if (null == uri_string ){
            return null;
        }

        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            Uri _uri = QNUtil.buildUri(uri_string);
            StringBuffer textContent = new StringBuffer();
            QNDev.log("QNContent_Text: uri=" + _uri);
            // read file content from SD card
            if (_uri != null && _uri.getScheme().equals("file")) {
                File file=new File(_uri.getPath());
                BufferedReader bufReader = null;
                FileReader fileReader = null;
                try {
                	fileReader =new FileReader(file);
                    bufReader=new BufferedReader(fileReader);
                    String line = null;
                    if((line=bufReader.readLine()) != null) {
                        textContent.append(line);
                    }                                       
                } catch (IOException d) {
                    QNDev.log(d.toString());  
                } finally {  
                    if (bufReader != null) {
                        try {
                            bufReader.close();
                        } catch (IOException e1) {}  
                    }
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e1) {}  
                    }
                }  
            }
            if(textContent == null){
            	return null;
            }
            QNDev.log("getTextNoteContentShort->textContent.toString().length()" + textContent.toString().length() + "textContent.toString():" + textContent.toString());           
            if (textContent.toString().length() <= 12) {
                return textContent.toString();
            } else {
                return textContent.toString().substring(0,12)+"...";
            }  
                                  
        } else {
            // cut the "qntext:" prefix
            if (uri_string.length() <= 19) {
                return uri_string.substring(7);
            } else {
                return uri_string.substring(7,19)+"..."; 
            }
        }
    }


    private void checkAndUpdateDB(Context context, Uri contentUri) {
        String targetPref = QNUtil.getInternalStoragePath(context);
        String sFileUri = null;
        if (contentUri != null ) {
          //case 1: if contentUri is not null, then only check this item
          sFileUri = (String)QuickNotesDB.read(context, contentUri, QNColumn.URI);
          comparePref(context, contentUri, targetPref, sFileUri);
          return;
        } else {
          //case 2: check all the DB
          Cursor qnCursor = QuickNotesDB.getQuickNoteDBCursor(context);
          if(null == qnCursor) { return; }
          if(qnCursor.moveToFirst()) {
            do {
              //uriFilePath = Uri.parse(qnCursor.getString(qnCursor.getColumnIndex(QNColumn.URI.column())));
              sFileUri = qnCursor.getString(qnCursor.getColumnIndex(QNColumn.URI.column()));
              Uri uri = ContentUris.withAppendedId(QuickNotesDB.CONTENT_URI, qnCursor.getLong(qnCursor.getColumnIndex(QNColumn._ID.column())) );
              comparePref(context, uri, targetPref, sFileUri);
             //set the reminder because alarm is gone after restarting
             //check the reminder
              long reminder = qnCursor.getLong(qnCursor.getColumnIndex(QNColumn.REMINDER.column()));
              if (0L != reminder ) {
              //set the reminder alarm
              QNUtil.setAlarm(context, uri, reminder);
              }
              //move to next note
              qnCursor.moveToNext();
            } while (!qnCursor.isAfterLast());
          }
          qnCursor.close(); 
          return;
        } 
    }

    private void comparePref(Context context, Uri contentUri, String targetPref, String sFileUri) {
          if (null == sFileUri) { return;}
          //file uri format:file:///mnt/sdcard/sketch/filename.png 
          /*2012-12-11, add by amt_sunzhao for SWITCHUITWO-25 */
          /*int pos = sFileUri.indexOf("/",12);
          String currentPref = sFileUri.substring(7, pos);
          //pref format: /mnt/sdcard
          if (targetPref.equals(currentPref)) {
              //match! 
              return;
          } else {
              //not match, need replace the prefix
              sFileUri = sFileUri.replaceFirst(currentPref, targetPref);
              QuickNotesDB.write(context, contentUri, QNColumn.URI, sFileUri);
              return;
          }*/
          if(null == targetPref) {
        	  return;
          }
          if(targetPref.startsWith(STORAGE_PREFIX)
    			  && sFileUri.startsWith(MNT_SDCARD_URI)) {
    		  boolean external = attemptAdatperPath(context, contentUri, MNT_SDCARD_URI, EXTERNAL_STORAGE_URI, sFileUri);
    		  if(!external) {
    			   attemptAdatperPath(context, contentUri, MNT_SDCARD_URI, INTERNAL_STORAGE_URI, sFileUri);
    		  }
    		  
    	  }
    }
    
    private boolean attemptAdatperPath(final Context context,
    		final Uri contentUri,
    		final String oldPref,
    		final String newPref,
    		final String sFileUri) {
    	boolean bRet = false;
    	final String newFileUri = sFileUri.replaceFirst(oldPref, newPref);
    	final String newFilePath = Uri.parse(newFileUri).getPath();
    	if(new File(newFilePath).exists()) {
    		bRet = QuickNotesDB.write(context, contentUri, QNColumn.URI, newFileUri);
		}
		return bRet;
    }
    /*2012-12-11, add end*/

    private Notification createNotification(Context context,
        String title, String contentText, PendingIntent pending) {
        // Note: the ticker is not shown for notifications in the Holo UX
        Notification.Builder builder = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pending)
            .setSmallIcon(R.drawable.stat_notify_quick_notes)
            .setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true);
        Notification notification = builder.getNotification();
        return notification;
    }
}
