package com.motorola.mmsp.socialGraph;

import com.motorola.mmsp.socialGraph.socialGraphWidget.ui.RingLayout;

import android.net.Uri;
import android.provider.CallLog.Calls;

public final class Constant {
	public static final Uri HISTORY_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialgraphservice.provider/message");
	public static final Uri FREQUENCY_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialgraphservice.provider/frequency");
	public static final Uri MISCVALUE_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialgraphservice.provider/miscvalue");
	public static final Uri SHORTCUTS_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialGraphWidget.provider/shortcuts");
	public static final Uri CHANGE_HISTORY_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialGraphWidget.provider/change_history");
	public static final Uri SKIN_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialGraphWidget.provider/skin");
	public static final Uri CONFIG_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialGraphWidget.provider/config");
	public static final Uri SKIN_DEFAULT_CONTENT_URI = Uri.parse("content://com.motorola.mmsp.socialGraphWidget.provider/default_skin");
	
	public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
	public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
	public static final Uri CALLS_CONTENT_URI = Calls.CONTENT_URI;
	
	public static final int MESSAGE_TYPE_CALL 		= 0;
	public static final int MESSAGE_TYPE_SMS 		= 1;
	public static final int MESSAGE_TYPE_MMS 		= 2;
	public static final int MESSAGE_TYPE_EMAIL 		= 3;
	
	public static final int MESSAGE_SUBTYPE_IN 		= 1;
	public static final int MESSAGE_SUBTYPE_OUT 	= 2;
	
	public static final int CALL_SUBTYPE_IN			= 1;
	public static final int CALL_SUBTYPE_OUT		= 2;
	public static final int CALL_SUBTYPE_MISSED		= 3;
	
	public static int SHORTCUT_COUNT				= 9;
	public static int SHORTCUT_BIG_COUNT 			= 1;
	public static int SHORTCUT_MEDIUM_COUNT			= 3;
	public static int SHORTCUT_SMALL_COUNT			= 5;
	
	public static final int SHORTCUT_SIZE_BIG 		= RingLayout.SIZE_TYPE_1;
	public static final int SHORTCUT_SIZE_MEDIA		= RingLayout.SIZE_TYPE_2;
	public static final int SHORTCUT_SIZE_SMALL		= RingLayout.SIZE_TYPE_3;
	
	public static final int SCREEN_SIZE_HDPI   = 1;
	public static final int SCREEN_SIZE_MDPI   = 2;
	
	public static final int NOTIFY_TYPE_RECORD				= 0x1;
	public static final int NOTIFY_TYPE_FREQUENCY			= 0x2;
	public static final int NOTIFY_TYPE_CONTACT_INFO_CHANGE				= 0x4;
	public static final int NOTIFY_TYPE_CONTACT_CHANGE		= 0x8;
	public static final int NOTIFY_TYPE_MASS_CHANGE		= 0x10;
	
	

	
	public static final String ACTION_WIDGET_RELEASE = "com.motorola.mmsp.home.ACTION_WIDGET_RELEASE";
	public static final String ACTION_WIDGET_ADDED = "com.motorola.mmsp.home.ACTION_WIDGET_ADDED";
	public final static String BROADCAST_WIDGET_SKIN_CHNAGE = "com.motorola.mmsp.socialGraph.WIDGET_SKIN_CHANGE";
	public final static String BROADCAST_WIDGET_SHORTCUT_CHNAGE = "com.motorola.mmsp.socialGraph.WIDGET_SHORTCUT_CHANGE";
	public final static String BROADCAST_WIDGET_CONTACT_CHNAGE = "com.motorola.mmsp.socialGraph.WIDGET_CONTACT_CHANGE";
	public final static String BROADCAST_WIDGET_OUT_OF_BOX = "com.motorola.mmsp.socialGraph.OUT_OF_BOX";
	public final static String BROADCAST_WIDGET_MODE_CHANGE = "com.motorola.mmsp.socialGraph.WIDGET_MODE_CHANGE";
	public final static String BROADCAST_WIDGET_UPDATE = "com.motorola.mmsp.socialGraph.UPDATE";

	public static final String DRAWBLE_NAME_PREFIX = "com.motorola.mmsp.socialGraph:drawable/";
	public static final String STRING_NAME_PREFIX = "com.motorola.mmsp.socialGraph:string/";
	
	
	public static final String BG_1 = "bg_image_1";
	public static final String BG_2 = "bg_image_2";
	public static final String BG_3 = "bg_image_3";
	public static final String BG_4 = "bg_image_4";
	public static final String BG_5 = "bg_image_5";
	public static final String BG_6 = "bg_image_6";
	public static final String BG_7 = "bg_image_7";
	public static final String BG_8 = "bg_image_8";
	public static final String BG_9 = "bg_image_9";
	public static final String BAR_1 = "bar_bg_1";
	public static final String BAR_2 = "bar_bg_2";
	public static final String BAR_3 = "bar_bg_3";
	public static final String BAR_4 = "bar_bg_4";
	public static final String BAR_5 = "bar_bg_5";
	public static final String BAR_6 = "bar_bg_6";
	public static final String BAR_7 = "bar_bg_7";
	public static final String BAR_8 = "bar_bg_8";
	public static final String BAR_9 = "bar_bg_9";
	
	public static final String[] mBgArray = new String[] { BG_1, BG_2, BG_3,
			BG_4, BG_5, BG_6, BG_7, BG_8, BG_9 };
	public static final String[] mBarArray = new String[] { BAR_1, BAR_2,
			BAR_3, BAR_4, BAR_5, BAR_6, BAR_7, BAR_8, BAR_9 };
	
	
	public static final String SHORTCUT_ID = "_id";
	public static final String SHORTCUT_INDEX = "shortcut_index";
	public static final String SHORTCUT_SIZE = "size";
	public static final String SHORTCUT_CONTACT_ID = "contact_id";
	public static final String SHORTCUT_NAME = "name";
	public static final String SHORTCUT_PHOTO = "photo";	
	
	public static final String MISC_KEY = "misc_key";
	public static final String MISC_VALUE = "misc_value";
	public static final String OUT_OF_BOX = "out_of_box";
	public static final String FIRST_ADDED = "first_added";
	

	
}
