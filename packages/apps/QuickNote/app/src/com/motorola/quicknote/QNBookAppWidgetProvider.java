package com.motorola.quicknote;

import java.util.ArrayList;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.os.IBinder;
import android.util.Log;

import com.motorola.quicknote.QuickNotesDB;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.content.QNContent_Error;
import com.motorola.quicknote.R;

public class QNBookAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "QNBookAppWidgetProvider";

    private static String mOldNoteUriString = null;
    private static String mNewNoteUriString = null;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {      
        QNDev.log(TAG+"onUpdate");
        final int N = appWidgetIds.length;
        for (int i=0; i < N; i++) {
           int appWidgetId = appWidgetIds[i];
           String uri_string =  QNAppWidgetConfigure.getUriPref(context, appWidgetId);
           if ((null != mOldNoteUriString) && mOldNoteUriString.equals(uri_string)) {
               //need update the widget configure
               QNAppWidgetConfigure.saveUriPref(context, appWidgetId, mNewNoteUriString);
               uri_string = mNewNoteUriString;
           }
           updateAppWidget(context, appWidgetManager, appWidgetId, uri_string);
              
        }
        //reset the note content uri
        mOldNoteUriString = null;
        mNewNoteUriString = null;
    }    

/*  IKSF-2111: remove service from QuickNote widget
    public static class QNWidgetUpdateService  extends Service {    
        @Override
        public void onStart(Intent intent, int startId) {
            QNDev.log(TAG+"QNWidgetUpdateService : in onStart()");
            ComponentName thisWidget = new ComponentName(this, QNBookAppWidgetProvider.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (null == appWidgetIds) {
               QNDev.log(TAG+"QNWidgetUpdateService: return directly for appWidgetIds is null");
               return; 
            }

            final int N = appWidgetIds.length;
            for (int i=0; i < N; i++) {
               int appWidgetId = appWidgetIds[i];
               String uri_string =  QNAppWidgetConfigure.getUriPref(this, appWidgetId);
               updateAppWidget(this, appWidgetManager, appWidgetId, uri_string);
            }
           
        }
 

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
     }
    

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {      
        QNDev.log(TAG+"onUpdate");
        try {
            Intent intent = new Intent(context, QNWidgetUpdateService.class);
            context.startService(intent);
        } catch (Exception e) {
            QNDev.logd(TAG, "failed to start update service" + e.toString());
        }
    }    
*/

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        QNDev.log("QNBookAppWidgetProvider : onDelete");   
        final int numWidget = appWidgetIds.length;
        for (int i=0; i<numWidget; i++) {
            QNDev.log(TAG+"deleting appWidgetId = "+appWidgetIds[i]);
            QNAppWidgetConfigure.deleteUriPref(context, appWidgetIds[i]);
        }

        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        QNDev.log("QNBookAppWidgetProvider : onDisabled");
/*  IKSF-2111: remove service from QuickNote widget
        //stop service when the last quicknote widget is removed from home screen
        Intent intent = new Intent(context, QNWidgetUpdateService.class);
        context.stopService(intent);
*/
        // TODO Auto-generated method stub
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        QNDev.log("QNBookAppWidgetProvider : onEnabled");
        // TODO Auto-generated method stub
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
     synchronized (QNBookAppWidgetProvider.class) {
         
       String action = intent.getAction();

       if (action.equals(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED)
               || action.equals("android.appwidget.action.APPWIDGET_UPDATE")
               || action.equals("com.motorola.quicknote.action.NOTE_DELETED") ) {

             QNDev.log(TAG+"onReceive: intent.action = "+intent.getAction());
             updatingWidgets(context); 
        } else if (action.equals("com.motorola.quicknote.action.NOTE_CREATED")
               || action.equals("com.motorola.quicknote.action.NOTE_VIEWED") ) {
            QNDev.log(TAG+"onReceive: intent.action = "+intent.getAction());
            String noteUriString = intent.getStringExtra(QNConstants.NOTE_URI);
            Uri noteUri = null;
            if (null != noteUriString) {
                int widget2update = QNAppWidgetConfigure.getWidget2Update(context);
                if (widget2update != -1) {
                    QNAppWidgetConfigure.saveUriPref(context, widget2update, noteUriString);
                    QNDev.log(TAG+"save configure: widget( "+widget2update+" ) with note( "+noteUriString+" )");
                    updatingWidgets(context);
                 }
             }   
        } else if (action.equals("com.motorola.quicknote.action.NOTE_UPDATED")) {
            mNewNoteUriString = intent.getStringExtra(QNConstants.NOTE_URI);
            mOldNoteUriString = intent.getStringExtra("old_content_uri");
            if (null == mOldNoteUriString ||null ==  mNewNoteUriString) {
                //reset the valuse
                mNewNoteUriString = null;
                mOldNoteUriString = null;
            }
            updatingWidgets(context);
        } else if  (action.equals("com.motorola.quicknote.action.FORCE_UPDATE_WIDGET")){
        	     QNDev.log(TAG+"onReceive: FORCE_UPDATE_WIDGET ");
             updatingWidgets(context);
        }
     } //end of synchronized()
  
   }

   
   private void updatingWidgets(Context context) {
         ComponentName thisWidget = new ComponentName(context, QNBookAppWidgetProvider.class);
         AppWidgetManager appWidgetmanager = AppWidgetManager.getInstance(context);
         int[] appWidgetIds = appWidgetmanager.getAppWidgetIds(thisWidget);

         QNDev.log(TAG+"onReceive: calling onUpdate(). appWidgetIds = "+ appWidgetIds);
         this.onUpdate(context, appWidgetmanager, appWidgetIds); 
   }


   static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, String uri_string) {
        QNDev.log(TAG+ "updateAppWidget appWidgetId=" + appWidgetId + " uri_string =" + uri_string);
        RemoteViews updateViews = null;
       
        //case 1: special widget
        if ((uri_string == null) || uri_string.equals("Special_widget")){
            if (uri_string == null) {
               QNDev.log(TAG+"updateAppWidet, but uri_string = null");
               QNAppWidgetConfigure.saveUriPref(context, appWidgetId, "Special_widget");
            } 
            QNDev.log(TAG+"updateAppWidget: it is special widget");
            updateViews = buildCreateNewPage(context, null, appWidgetId);
            // Tell the widget manager
            appWidgetManager.updateAppWidget(appWidgetId, updateViews);
           return;
        } 

        //case 2: normal widget
        QNDev.log(TAG+"widgetprovider: it is normal widget");

        Uri contentUri =  QNUtil.buildUri(uri_string);
        if ((contentUri == null) || !QuickNotesDB.isValidUri(context, contentUri)) {
            //uri is null or uri is not valid e.g. this note has been deleted
            //then try to show the 1st note
            QNDev.log(TAG+"case1: content is null or not valid in DB, so reset widget");
            updateViews = resetWidget(context, appWidgetId);
        } else {
           //sometimes, when the widget received the delete intent, the note is still in deleting process, 
           //so the widget will still try to show the note which is in deleting 
           try {
               updateViews = setupView(context, contentUri, appWidgetId);
           } catch (Exception e) {
               QNDev.log(TAG+"case 2: this note has issue, so show the 1st note or the default note");
               updateViews = resetWidget(context, appWidgetId);
           }
        }

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
        return;
    }


  static private RemoteViews resetWidget(Context context, int appWidgetId) {
       RemoteViews updateViews;

       QNDev.log(TAG+"resetWidget");
       Uri contentUri = QuickNotesDB.getItem(context, 0);
       if (contentUri != null) {
           //firstly, try to show the 1st note
           QNDev.log(TAG+"related widget note is deleted or invalid for other reasons, so show the 1 note, noteUri = "+contentUri);
           QNAppWidgetConfigure.saveUriPref(context, appWidgetId, contentUri.toString());
           updateViews = setupView(context, contentUri, appWidgetId);
       } else {
          //show the default widget
          QNDev.log(TAG+"related widget note is deleted and no note exists so, so show the default note");
          QNAppWidgetConfigure.saveUriPref(context, appWidgetId, "Special_widget");
          updateViews = buildCreateNewPage(context, null, appWidgetId);
       }
      
      return updateViews;
  }


  static private RemoteViews setupView(Context context, Uri contentUri, int appWidgetId) {

       RemoteViews updateViews = null;
      
       String[] columns_excluded = new String[2];
        
       columns_excluded[0] =  QNColumn._ID.column();
       columns_excluded[1] =  QNColumn.WIDGETID.column();
       //columns_excluded[2] =  QNColumn.SPANX.column();
       //columns_excluded[3] =  QNColumn.SPANY.column();


       ContentValues cValues = QuickNotesDB.copy_values(context, contentUri, columns_excluded);
       if (cValues == null) {
          QNDev.log(TAG+"Error found! so set error widget");
          updateViews = buildErrorPage(context, contentUri, appWidgetId);
          return updateViews;
        }

       String mimeType = cValues.getAsString(QNColumn.MIMETYPE.column());     
       String title = cValues.getAsString(QNColumn.TITLE.column());
       String qnUri_string = cValues.getAsString(QNColumn.URI.column());
       Uri qnUri = QNUtil.buildUri(qnUri_string);
       QNContent qnc = QNContent_Factory.Create(mimeType, qnUri);

       if ((qnc == null) || (qnc instanceof QNContent_Error)) {
            updateViews = buildErrorPage(context, contentUri, appWidgetId);
       } else {
            QNDev.logd(TAG, "QNContent:qnc type is " + qnc.getClass().getSimpleName());
            if (mimeType.startsWith("text/")) {
                QNDev.log("show text type quicknote");
                updateViews = buildTextPage(context, qnc, contentUri, appWidgetId, cValues);
            } else if (mimeType.startsWith("image/")) {
                updateViews = buildImagePage(context, qnc, contentUri, appWidgetId, cValues);
            } else if (mimeType.startsWith("audio/")) {
                updateViews = buildAudioPage(context, qnc, contentUri, appWidgetId, cValues);
            } else {
                QNDev.log(TAG+ "unknown mimeType");
            }
       }

       //set the reminder flag if have
       long reminder = cValues.getAsLong(QNColumn.REMINDER.column());
       QNDev.log(TAG+"reminder:QNWidget: reminder ="+reminder);
       if (reminder != 0) {
           updateViews.setViewVisibility(R.id.image_reminder, View.VISIBLE);
       } else {
           updateViews.setViewVisibility(R.id.image_reminder, View.GONE);
       }

       // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
       //set in-call flag if have
       long call_date_end = cValues.getAsLong(QNColumn.CALLDATEEND.column());
       if (call_date_end != 0) {
           updateViews.setViewVisibility(R.id.image_incall, View.VISIBLE);
       } else {
           updateViews.setViewVisibility(R.id.image_incall, View.GONE);
       }
       // END IKCNDEVICS-504

       // set the title if have
       if(null != updateViews && null != title) {
           if (title.length() > 0) {
               updateViews.setTextViewText(R.id.note_title, title);
               updateViews.setViewVisibility(R.id.title_part, View.VISIBLE);
               updateViews.setViewVisibility(R.id.separator, View.VISIBLE);
           }  else {
               //title length is zero, hide this title
               updateViews.setViewVisibility(R.id.title_part, View.GONE);
               updateViews.setViewVisibility(R.id.separator, View.GONE);
           }
        }
 
        return updateViews;
  }


  static private RemoteViews buildDefaultPage(Context context, Uri contentUri, int appWidgetId) {
       QNDev.log("set default page");
       RemoteViews defaultView = new RemoteViews(context.getPackageName(), 
                 R.layout.appwidget_remoteview_quicknotebook);
            
       PendingIntent pendingIntent = createPendingIntent(context, contentUri, appWidgetId);
       defaultView.setOnClickPendingIntent(R.id.qnb_main, pendingIntent);

       return defaultView;
   }


  static private RemoteViews  buildCreateNewPage(Context context, Uri contentUri, int appWidgetId) {

       QNDev.log(TAG+"QNAppWidget, buildCreateNewPage to set createNew page");
       RemoteViews defaultView = new RemoteViews(context.getPackageName(), 
                 R.layout.appwidget_remoteview_quicknotebook);
       Intent intent = new Intent("com.motorola.quicknote.action.QUICKNOTE");

       if(QNUtil.isTextLoad(context))
          intent.setClassName("com.motorola.quicknote",  "com.motorola.quicknote.QNWelcome");
       else
          intent.setClassName("com.motorola.quicknote",  "com.motorola.quicknote.QNNewActivity"/*"com.motorola.quicknote.QNNoteView"*/);

       intent.setFlags ( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                         | Intent.FLAG_ACTIVITY_SINGLE_TOP
                         | Intent.FLAG_ACTIVITY_NEW_TASK  );
       intent.putExtra("contentUri","Special_widget");
       intent.putExtra("from","widget");
       intent.putExtra("widgetId",appWidgetId);
       PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);         
       defaultView.setOnClickPendingIntent(R.id.qnb_main, pendingIntent);

       return defaultView;


  }


   static private RemoteViews buildTextPage(Context context, QNContent qnc, Uri contentUri, int appWidgetId, ContentValues cValues) {
       QNDev.log("in buildTextPage");
       RemoteViews textView = null;           
       int bgcolor = cValues.getAsInteger(QNColumn.BGCOLOR.column());

       switch(bgcolor){
           case R.color.blue:
                textView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_text_mathematics);
                break;
           case R.color.green:
                textView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_text_project_paper);
                break;
           case R.color.orange:
                textView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_text_graphy);
                break;
           case R.color.yellow:
           default:  //default should use yellow
                textView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_text_gridme);
                break; 
       }
       textView.setTextColor(R.id.qnb_headpage,Color.BLACK);
       textView.setTextViewText(R.id.qnb_headpage, qnc.getTextNoteContentShort());
           
       PendingIntent pendingIntent = createPendingIntent(context, contentUri, appWidgetId);
       textView.setOnClickPendingIntent(R.id.note_content, pendingIntent);

       return textView;
    }


    static private RemoteViews buildImagePage(Context context, QNContent qnc, Uri contentUri, int appWidgetId, ContentValues cValues) {
        RemoteViews imageView = new RemoteViews(context.getPackageName(), 
                  R.layout.qnb_appwidget_remoteview_image);
        QNDev.log("set image quick note");
            
        int spanX = cValues.getAsInteger(QNColumn.SPANX.column());
        int spanY = cValues.getAsInteger(QNColumn.SPANY.column());
        qnc.setBitmapType(QNContent.BitmapTypeE.SCREEN_WIDGET_BITMAP);
       
        QNDev.log(TAG+"spanx = "+spanX+"  spanY = "+spanY);
        //the real image content is lower than the widget height, so minus the padding
        Bitmap bmp = qnc.widgetBitmap(context, context.getResources().getInteger(R.integer.widget_width) , context.getResources().getInteger(R.integer.widget_height) );
        if (null != bmp) {
            imageView.setImageViewBitmap(R.id.qnb_headpage, bmp);
        } else {
            QNDev.logd(TAG, "error image/sketch media");
            QNDev.logd(TAG, "qn uri is " + ((qnc.uri() != null) ? qnc.uri().toString() : "null"));
            imageView = new RemoteViews(context.getPackageName(), 
                     R.layout.qnb_appwidget_remoteview_error);
        }

        PendingIntent pendingIntent = createPendingIntent(context, contentUri, appWidgetId);
        imageView.setOnClickPendingIntent(R.id.note_content, pendingIntent);

        return imageView;
     }


    static private RemoteViews buildAudioPage(Context context, QNContent qnc, Uri contentUri, int appWidgetId, ContentValues cValues) {
        QNDev.log("set audio quick note");
        RemoteViews audioView = null;    
        int bgcolor = cValues.getAsInteger(QNColumn.BGCOLOR.column());;
        switch(bgcolor){
           case R.color.blue :
                audioView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_voice_mathematics);
                break;
           case R.color.green:
                audioView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_voice_project_paper);
                break;
           case R.color.orange:
                audioView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_voice_graphy);
                break;
           case R.color.yellow:
           default: //default should use yellow
                audioView = new RemoteViews(context.getPackageName(), R.layout.qnb_appwidget_remoteview_voice_gridme);
                break;
         }
         qnc.setup();
         if (!qnc.fUsable) {
              QNDev.logd(TAG, "error audio media");
              audioView = new RemoteViews(context.getPackageName(), 
                        R.layout.qnb_appwidget_remoteview_error);
          }
                        
         PendingIntent pendingIntent = createPendingIntent(context, contentUri, appWidgetId);
         audioView.setOnClickPendingIntent(R.id.note_content, pendingIntent);
      
         qnc.close();

         return audioView;
     }


    static private RemoteViews buildErrorPage(Context context, Uri contentUri, int appWidgetId) {
          RemoteViews errorView = new RemoteViews(context.getPackageName(), 
                        R.layout.qnb_appwidget_remoteview_error);
          PendingIntent pendingIntent = createPendingIntent(context, contentUri, appWidgetId);
          errorView.setOnClickPendingIntent(R.id.note_content, pendingIntent);

          return errorView;
      }

    static  private PendingIntent createPendingIntent(Context context, Uri contentUri, int appWidgetId) {
         Intent intent = new Intent("com.motorola.quicknote.action.QUICKNOTE");
         intent.setClassName("com.motorola.quicknote",  "com.motorola.quicknote.QNNoteView");
         intent.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                          | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                          | Intent.FLAG_ACTIVITY_NEW_TASK   );
         if (contentUri != null) {
            intent.putExtra("contentUri",contentUri.toString());
         }
         intent.putExtra("from","widget");
         intent.putExtra("widgetId",appWidgetId);
         QNDev.log(TAG+"in PendingIntent,  noteUri = "+ contentUri);
         PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);         
         
         return pendingIntent;
      }
        

}
