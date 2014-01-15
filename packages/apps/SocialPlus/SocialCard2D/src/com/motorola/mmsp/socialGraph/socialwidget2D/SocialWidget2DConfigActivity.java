package com.motorola.mmsp.socialGraph.socialwidget2D;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import com.motorola.mmsp.socialGraph.Constant;

public class SocialWidget2DConfigActivity extends Activity{
	private static final String TAG = "SocialWidget2DConfigActivity";
	
	private static final String MISC_KEY = "misc_key";
	private static final String MISC_VALUE = "misc_value";
	private static final String OUT_OF_BOX = "out_of_box";
	private static final String FIRST_ADDED = "first_added";
	private static final Uri MISCVALUE_CONTENT_URI = Uri
			.parse("content://com.motorola.mmsp.socialgraphservice.provider/miscvalue");
	
	public final static String BROADCAST_WIDGET_OUT_OF_BOX = "com.motorola.mmsp.socialGraph.OUT_OF_BOX";	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		super.onCreate(savedInstanceState);
		SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		Log.d(TAG, "model.getOutOfBoxStatus():"+model.getOutOfBoxStatus());
        if(!getFirstAddedStatus(this)){
            if (!isSocialInFlex()) {
        	SocialWidget2DUtils.showWelcomeAcitivity(this);//showSettingApp(this,true);
            }
        	writeFirstAddedToDb(this);
        	
        	// model.updateOutOfBoxStatus(this);
			// writeOutOfBoxToDb(this);

			/*
			 * writeOutOfBoxToDb(getBaseContext());
			 * Setting.notifyContactUpdate(this); Intent intent = new
			 * Intent(BROADCAST_WIDGET_OUT_OF_BOX); sendBroadcast(intent);
			 */		
        }
        AppWidgetManager.getInstance(this);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
        setResult(RESULT_OK, resultValue);
        finish();
	}
	
	public boolean writeOutOfBoxToDb(Context context) {
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
			Log.e("zc", e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return true;
	}
	
	public boolean writeFirstAddedToDb(Context context) {
		ContentValues values = new ContentValues();
		values.put(MISC_KEY, FIRST_ADDED);
		values.put(MISC_VALUE, 1);

		Cursor c = null;

		try {
			c = context.getContentResolver().query(MISCVALUE_CONTENT_URI,
					new String[] { MISC_VALUE }, MISC_KEY + "=?",
					new String[] { FIRST_ADDED }, null);

			if (c != null && c.getCount() > 0) {
				context.getContentResolver().update(MISCVALUE_CONTENT_URI,
						values, MISC_KEY + "=?", new String[] { FIRST_ADDED });
			} else {
				context.getContentResolver().insert(MISCVALUE_CONTENT_URI,
						values);
			}

		} catch (Exception e) {
			Log.e("zc", e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return true;
	}
	
	public boolean getFirstAddedStatus(Context context) {
		Cursor c = null;
		int firstAdded = 0;

		boolean firstAddedStatus = false;
		try {
			c = context.getContentResolver().query(Constant.MISCVALUE_CONTENT_URI,
					new String[] { Constant.MISC_VALUE }, Constant.MISC_KEY + "=?",
					new String[] { Constant.FIRST_ADDED }, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				firstAdded = c.getInt(0);
			}

		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
			
			if(firstAdded > 0) {	
				firstAddedStatus = true;
			} else {
				firstAddedStatus = false;
			}
		}
		
		return firstAddedStatus;
	}
	private boolean isSocialInFlex() {
		boolean result = false;
		Log.d(TAG, "isSocialInFlex");
		XmlPullParser parser = null;
		String xmlstr = "";
		XmlPullParserFactory factory;

		try {
			xmlstr = getResultFromCDA("@MOTOFLEX@getHomeApplications",
					SocialWidget2DConfigActivity.this);
			if (TextUtils.isEmpty(xmlstr)) {
				return result;
			}
			factory = XmlPullParserFactory.newInstance();
			parser = factory.newPullParser();
			factory.setNamespaceAware(true);
			parser.setInput(new StringReader(xmlstr));
			final int depth = parser.getDepth();

			int type;
			while (((type = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
				boolean isContainSocial = false;
				if (type != XmlPullParser.START_TAG) {
					continue;
				}

				String name = parser.getName();
				String packageName = "";

				if (name != null && name.equals("appwidget")) {
					for (int j = 0; j < parser.getAttributeCount(); j++) {
						packageName = parser.getAttributeValue(j);
						if (parser.getAttributeName(j).equals(
								"launcher:packageName")
								&& packageName
										.equals("com.motorola.mmsp.socialGraph")) {
                            Log.d(TAG, "isSocialInFlex:true");
							isContainSocial = true;
							break;
						}
					}
				}
				if (isContainSocial) {
					result = true;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean isFlexValid(Context context) {
		try {
			TypedValue isCDA = new TypedValue();
			Resources r = context.getResources();
			r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
			if (isCDA.coerceToString().toString().equalsIgnoreCase("true")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getResultFromCDA(String cmd, Context context) {
		TypedValue contentCDA = new TypedValue();
		String xmlresult = "";
		if (context == null) {
			return xmlresult;
		}
		Resources r = context.getResources();
		try {
			if (isFlexValid(context)) {
				r.getValue(cmd, contentCDA, false);
				xmlresult = contentCDA.coerceToString().toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlresult;
	}
}
