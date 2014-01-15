/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2010 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * a21747                    09/02/2010                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle; 
import android.os.HandlerThread;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.Window;

import java.util.ArrayList;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.motorola.quicknote.QNDisplayNotes.NoteAdapter;
import com.motorola.quicknote.QNDisplayNotes.ObtainBitmapHandler;
import com.motorola.quicknote.QNDisplayNotes.UIHandler;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.QNDisplayNotes;
import com.motorola.quicknote.R;

public class QNAppWidgetConfigure extends Activity {

	private static final String TAG = "QNAppWidgetConfigure";
	private static final String OBTAIN_BITMAP_WIDGET_THREAD = "obtain bitmap widget thread";
	
	private int mNum;
	private Cursor mCursor;
	// private int mIndex;
	private static Uri mContentUri;
	private GridView mGrid;
	private static final int REQCODE_WIDGET = 2;
	private static final String PREFS_NAME = "QNAppWidgetConfigure";
	private static final String PREF_PREFIX_KEY = "noteIndex";
	private BroadcastReceiver mQuickNotesReceiver = new QuickNotesIntentReceiver();
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int mThumbLayoutWidth;
	private int mThumbLayoutHeight;
	
	/*2012-9-6, add by amt_sunzhao for T810T_P003933 */
	private HandlerThread mObtainBitmapThread = new HandlerThread(OBTAIN_BITMAP_WIDGET_THREAD);
	private ObtainBitmapHandler mObtainBitmapH = null;
	/*2012-9-6, add end*/ 

	public QNAppWidgetConfigure() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

        mThumbLayoutWidth = getResources().getDimensionPixelSize(R.dimen.thumb_layout_width);
        mThumbLayoutHeight= getResources().getDimensionPixelSize(R.dimen.thumb_layout_height);

		// set layout
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// set the special grid format /layout and set the adapter
		mCursor = QuickNotesDB.getQuickNoteDBCursor(this);
		mNum = mCursor.getCount();
		if (mNum == 0) {
			// use default page
           QNDev.log(TAG+"widgetConfigure: no note, so create a blank widget directly");
           //save the selected note index into prfex
            saveUriPref(QNAppWidgetConfigure.this, mAppWidgetId, "Special_widget");

           // Push widget update to surface with newly set prefix
           AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(QNAppWidgetConfigure.this);
           QNBookAppWidgetProvider.updateAppWidget(QNAppWidgetConfigure.this, appWidgetManager,
                    mAppWidgetId, "Special_widget");

           // Make sure we pass back the original appWidgetId
           Intent resultValue = new Intent();
           resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
           setResult(RESULT_OK, resultValue);
           finish();
           return;
        } else {
            //nNum != 0
            layoutSelect(mNum);
			/*2012-9-6, add by amt_sunzhao for T810T_P003933 */            
    		mObtainBitmapThread.start();
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 	
    		UIHandler UIHandler = new UIHandler(mGrid);
			/*2012-9-18, add end*/ 
			/*2012-9-15, add by amt_sunzhao for SWITCHUITWOV-190 */ 
    		mObtainBitmapH = new ObtainBitmapHandler(mObtainBitmapThread.getLooper(), UIHandler, mGrid);
			/*2012-9-15, add end*/ 
			/*2012-9-6, add end*/
        }

	}

	private void layoutSelect(int qnNum) {
		// set special grid layout according to the size of QN DB
		// if (qnNum == 0) {
		// setContentView(R.layout.display_notes_gridview_0);
		// } else if (qnNum == 1) {
		// //show the 1 note in the middle together with the special Adding icon
		// setContentView(R.layout.display_notes_gridview_1);
		// } else if (qnNum == 2) {
		// //show the 2 notes in the middle together with the special Adding
		// icon
		// setContentView(R.layout.display_notes_gridview_2);
		// } else {
		// setContentView(R.layout.display_notes_gridview);
		// }

		setContentView(R.layout.display_notes_gridview);

		mGrid = (GridView) findViewById(R.id.gridview);

		mGrid.setAdapter(new NoteAdapter(this));

        mGrid.setOnItemClickListener(new OnItemClickListener() 
        {
            public void onItemClick(AdapterView parent, 
            View v, int position, long id) 
            {
              if (position == 0) {
                QNDev.log(TAG+"create a blank widget");
                //save the selected note index into prfex
                saveUriPref(QNAppWidgetConfigure.this, mAppWidgetId, "Special_widget");
 
                // Push widget update to surface with newly set prefix
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(QNAppWidgetConfigure.this);
                QNBookAppWidgetProvider.updateAppWidget(QNAppWidgetConfigure.this, appWidgetManager,
                    mAppWidgetId, "Special_widget");

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();

              }else {
                mContentUri = QuickNotesDB.getItem(QNAppWidgetConfigure.this, position - 1);
                QNDev.log(TAG+"return from widgetReview with OK: contentUri = "+mContentUri);
                /*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
                if(null == mContentUri) {
                	return;
                }
                /*2012-10-8, add end*/ 
                //save the selected note index into prfex
                saveUriPref(QNAppWidgetConfigure.this, mAppWidgetId, mContentUri.toString());
 
                // Push widget update to surface with newly set prefix
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(QNAppWidgetConfigure.this);
                QNBookAppWidgetProvider.updateAppWidget(QNAppWidgetConfigure.this, appWidgetManager,
                mAppWidgetId, mContentUri.toString());

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
              }
            }
        });    

        registerIntentReceivers(); 
  
    }

	@Override
	public void onDestroy() {
		QNDev.log(TAG + " onDestroy()");
		try {
			mCursor.close();
			unregisterReceiver(mQuickNotesReceiver);
		} catch (Exception e) {
			// do nothing
		}
		/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
		mObtainBitmapThread.quit();
		/*2012-9-6, add end*/ 
		super.onDestroy();
	}

    private void registerIntentReceivers() {
        IntentFilter qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_CREATED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_UPDATED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_DELETED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        /*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_DELETE_TASKS);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        /*2012-10-8, add end*/ 
    } 
     

	private class QuickNotesIntentReceiver extends BroadcastReceiver {
		private final String TAG = "[QNDisplayNotes:QuickNotesIntentReceiver]";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			QNDev.log(TAG + " intent action : " + action);
			/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
			if(QNConstants.INTENT_ACTION_NOTE_DELETE_TASKS.equals(action)) {
				final int deleteTasksCount = intent.getIntExtra(QNConstants.KEY_DELETE_TASKS_COUNT, -1);
				if(0 != deleteTasksCount) {
					Toast.makeText(QNAppWidgetConfigure.this, R.string.data_processing_wait, Toast.LENGTH_SHORT).show();
					finish();
				}
			} else {
				((NoteAdapter) mGrid.getAdapter()).notifyDataSetChanged();
			}
			/*2012-10-8, add end*/ 
		} // end of onReceive()
	}

	/**
	 * Adapter the notes to the gridView
	 */
    private class NoteAdapter extends BaseAdapter 
    {
		private LayoutInflater mInflater;
		private Context mContext;

		public NoteAdapter(Context c) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = getLayoutInflater();
			mContext = c;
		}

		// ---returns the number of notes in QuickNotesDB
		public int getCount() {
			return (mNum + 1);
		}

		// ---returns a note id
		public Object getItem(int position) {
			QNDev.log(TAG + " getItem(" + position + ")");
			return position;
		}

		// ---returns the ID of the note
		public long getItemId(int position) {
			return position;
		}

		// ---returns an ImageView view---
        public View getView(int position, View convertView, ViewGroup parent) 
        {  
            QNDisplayNotes.ViewHolder viewHolder;
			/*2012-9-13, add by amt_sunzhao for SWITCHUITWOV-175 */ 
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
            /*int vhPostion = -1;
			if(null != convertView) {
				viewHolder = (QNDisplayNotes.ViewHolder) convertView.getTag();
				if(null != viewHolder) {
					vhPostion = viewHolder.postion;
				}
			}
            if (convertView == null
            		|| (position != vhPostion)){
			*/
			if (convertView == null) {
			/*2012-9-18, add end*/ 
			/*2012-9-13, add end*/  
               // set the layout and return it
               convertView = mInflater.inflate(R.layout.display_notes_item, null);
               RelativeLayout layout = (RelativeLayout)convertView.findViewById(R.id.main_notes);
               layout.setLayoutParams(new GridView.LayoutParams(mThumbLayoutWidth, mThumbLayoutHeight));
  
               //create a ViewHolder and store references to the childeren views that we want to bind date to
               viewHolder = new QNDisplayNotes.ViewHolder();
               viewHolder.image = (ImageView)convertView.findViewById(R.id.image);
               viewHolder.text = (TextView)convertView.findViewById(R.id.text);
               viewHolder.note_title = (TextView)convertView.findViewById(R.id.note_title);
               viewHolder.separator = (ImageView)convertView.findViewById(R.id.separator);
               viewHolder.selection = (ImageView) convertView.findViewById(R.id.selection);
               viewHolder.image_error = (ImageView)convertView.findViewById(R.id.image_error);
               viewHolder.text_error = (TextView)convertView.findViewById(R.id.text_error);
               viewHolder.image_reminder = (ImageView)convertView.findViewById(R.id.image_reminder);
               // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
               viewHolder.image_incall = (ImageView)convertView.findViewById(R.id.image_incall);
				// END IKCNDEVICS-504
				viewHolder.layout_width = mThumbLayoutWidth;
				viewHolder.layout_height = mThumbLayoutHeight;
				viewHolder.separator.setVisibility(View.GONE);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (QNDisplayNotes.ViewHolder) convertView.getTag();
				// reset all the views in it
				viewHolder.image.setVisibility(View.GONE);
				viewHolder.text.setVisibility(View.GONE);
				viewHolder.note_title.setVisibility(View.GONE);
				viewHolder.separator.setVisibility(View.GONE);
				viewHolder.separator.setVisibility(View.GONE);
				viewHolder.image_error.setVisibility(View.GONE);
				viewHolder.text_error.setVisibility(View.GONE);
				viewHolder.image_reminder.setVisibility(View.GONE);
				// BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote:
				// in-call quicknote demo.
				viewHolder.image_incall.setVisibility(View.GONE);
				// END IKCNDEVICS-504
			}

			// Bind the data efficentely with the holder
			if (position == 0) {
				// The first position 0 is to place the QN function icon
				// viewHolder.image.setBackgroundResource(R.drawable.ic_thumb_add_new);
				// viewHolder.image.setImageDrawable(null);
                ((RelativeLayout)viewHolder.image.getParent()).setBackgroundColor(getResources().getColor(R.color.transparent));
                viewHolder.image.setImageDrawable(getResources().getDrawable(R.drawable.bg_thb_create_new));
                viewHolder.image.setVisibility(View.VISIBLE);
            } else {
                Uri uri;
                int NoteId = position -1; 
                QNDev.log(TAG+"total gridview num = "+getCount()+" current position = "+position+" NoteId= "+ NoteId);
					/*2012-9-6, add by amt_sunzhao for T810T_P003933 */                 
					QNDisplayNotes.itemMatch(QNAppWidgetConfigure.this, mCursor, NoteId, viewHolder, mObtainBitmapH);
					/*2012-9-6, add end*/ 
             }

             return convertView;
          }  //end of getView
          
      }  // end of NoteAdapter 

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
            //set special grid layout according to the size of QN DB
            //port-land layout has differet format, so need reset the layout
            layoutSelect(mNum);
    }    

    // Write the prefix to the SharedPreferences object for this widget
    static void saveUriPref(Context context, int appWidgetId, String contentUri_string) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, contentUri_string);
        QNDev.log(TAG+"saveUriPref: appWidgetId = "+appWidgetId+" contentUri = "+contentUri_string);
        /*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-259 */ 
        //prefs.commit();
        prefs.apply();
        /*2012-10-8, add end*/ 
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String getUriPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String prefix = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        return prefix;
    }


    static void deleteUriPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        QNDev.log(TAG+"deleteUriPref: appWidgetId = "+appWidgetId);
        prefs.commit();
    }

   public static void saveWidget2Update(Context context, int appWidgetId) {
       SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
       prefs.putInt(PREF_PREFIX_KEY + "Widget2Update", appWidgetId);
       QNDev.log(TAG+"saveWidget2Update: appWidgetId = "+appWidgetId);
       prefs.commit();
   }

	public static int getWidget2Update(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		int prefix = prefs.getInt(PREF_PREFIX_KEY + "Widget2Update", -1);
		QNDev.log(TAG + "getWidget2Update: appWidgetId = " + prefix);
		return prefix;
	}

}
