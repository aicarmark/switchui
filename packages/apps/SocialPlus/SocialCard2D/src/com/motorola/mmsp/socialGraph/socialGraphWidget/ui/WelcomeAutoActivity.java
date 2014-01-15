package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;

public class WelcomeAutoActivity extends Activity
      implements View.OnClickListener{
	private static final String TAG = "WelcomeAutoActivity";

	private static final String MISC_KEY = "misc_key";
	private static final String MISC_VALUE = "misc_value";
	private static final String OUT_OF_BOX = "out_of_box";
	private static final Uri MISCVALUE_CONTENT_URI = Uri
			.parse("content://com.motorola.mmsp.socialgraphservice.provider/miscvalue");
	
	public final static String BROADCAST_WIDGET_OUT_OF_BOX = "com.motorola.mmsp.socialGraph.OUT_OF_BOX";

	@Override
       public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);        
        	setContentView(R.layout.welcome_auto_screen);

		findViewById(R.id.finish).setOnClickListener(this);
		findViewById(R.id.back).setOnClickListener(this);
       }
	
	public void onClick(View v) {
		Log.d(TAG, "onClick");
		if (v.getId() == R.id.finish) {
			writeOutOfBoxToDb(getBaseContext());
			Setting.notifyContactUpdate(this);
			Intent intent = new Intent(BROADCAST_WIDGET_OUT_OF_BOX);
			sendBroadcast(intent);
			
			Activity activity = WelcomeActivity.getActivity();
			Log.d(TAG, "activity = " + activity);
			if (activity != null) {
				activity.finish();
			}
			
			finish();
		} else if (v.getId() == R.id.back) {
			/*Intent intent = new Intent();
			intent.setClass(WelcomeAutoActivity.this, WelcomeActivity.class);
			startActivity(intent);*/
			finish();
		}
	}
	

   	
	public static boolean writeOutOfBoxToDb(Context context) {
		ContentValues values = new ContentValues();
		values.put(MISC_KEY, OUT_OF_BOX);
		values.put(MISC_VALUE, 1);

		Cursor c = null;

		try {
			c = context.getContentResolver().query(MISCVALUE_CONTENT_URI,
					new String[] { MISC_VALUE }, MISC_KEY + "=?",
					new String[] { OUT_OF_BOX }, null);

			if (c != null && c.getCount() > 0) {
				context.getContentResolver().update(MISCVALUE_CONTENT_URI,
						values, MISC_KEY + "=?", new String[] { OUT_OF_BOX });
			} else {
				context.getContentResolver().insert(MISCVALUE_CONTENT_URI,
						values);
			}

		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return true;
	}

}
