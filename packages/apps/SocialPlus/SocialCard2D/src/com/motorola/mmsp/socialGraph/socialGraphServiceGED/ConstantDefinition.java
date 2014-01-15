/*
 * it define all the constant needed by the service
 */

package com.motorola.mmsp.socialGraph.socialGraphServiceGED;

import android.provider.ContactsContract.Contacts;

import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphProvider;

public final class ConstantDefinition{
	
	/**
     * Set this to 'true' to enable as much SocialGraph logging as possible.
     * Do not check-in with it set to 'true'!
     */
    public static final boolean SOCIAL_GRAPH_SOCIAL_LOGD = false;
    
	//external
    //notify UI to update
	public static final String SHORTCUT_UPDATE_ACTION 				= "com.motorola.mmsp.intent.action.SHORTCUT_UPDATE_ACTION";
	
	//intent for start the mms.sms service
	public static final String SOCIAL_SERVICE_ACTION			= "com.motorola.mmsp.intent.action.SOCIAL_SERVICE_ACTION";
	//intent for start the call log service
	//public static final String CALLLOG_SERVICE_ACTION			= "com.motorola.mmsp.intent.action.CALLLOG_SERVICE_ACTION";		    
	
    //Message updated notification from sms.mms, call, contacts to social graph service    
    public static final String NOTIFICATION_ACTION_CALL 		= "com.motorola.mmsp.intent.action.CALL_ACTION";
    public static final String NOTIFICATION_ACTION_CONTACTS 	= "com.motorola.mmsp.intent.action.CONTACTS_ACTION";
    public static final String NOTIFICATION_ACTION_MESSAGE 		= "com.motorola.mmsp.intent.action.MESSAGE_ACTION";
    
	//call action types
	public static final int MESSAGE_ACTION_CALL_INSERT 					= 0x101;
	public static final int MESSAGE_ACTION_CALL_DELETE					= 0x102;
	
	//message action types
	public static final int MESSAGE_ACTION_SMS_IN						= 0x201;
	public static final int MESSAGE_ACTION_SMS_SENT						= 0x202;
	public static final int MESSAGE_ACTION_SMS_DELETE					= 0x203;
	public static final int MESSAGE_ACTION_SMS_UPDATE					= 0x204;
	public static final int MESSAGE_ACTION_MMS_IN						= 0x205;
	public static final int MESSAGE_ACTION_MMS_SENT						= 0x206;
	public static final int MESSAGE_ACTION_MMS_DELETE					= 0x207;
	public static final int MESSAGE_ACTION_MMS_UPDATE					= 0x208;
	
	//contact action types
	public static final int MESSAGE_ACTION_CONTACT_INSERT				= 0x303;
	public static final int MESSAGE_ACTION_CONTACT_DELETE				= 0x304;
	public static final int MESSAGE_ACTION_CONTACT_DATA_INSERT			= 0x305;
	public static final int MESSAGE_ACTION_CONTACT_DATA_DELETE			= 0x306;
	public static final int MESSAGE_ACTION_CONTACT_DATA_UPDATE			= 0x307;
	public static final int MESSAGE_ACTION_CONTACT_PHOTO_UPDATE			= 0x308;
	public static final int MESSAGE_ACTION_CONTACT_JOIN					= 0x309;
	public static final int MESSAGE_ACTION_CONTACT_RAW_CONTACT_NAME     = 0x30A;
	
	public static final int MESSAGE_ACTION_CONTACT_MASS					= 0x3E7;
	
	//Message main type definition
	//They are used in the database
	public static final int MESSAGE_TYPE_CALL					= 1;
	public static final int MESSAGE_TYPE_MESSAGE				= 2;
	public static final int MESSAGE_TYPE_CONTACT				= 3;
	
	//Message sub type definition
	//They are used in the database
	public static final int MESSAGE_TYPE_CALL_INCOMING 			= 1;
	public static final int MESSAGE_TYPE_CALL_OUTGOING 			= 2;
	public static final int MESSAGE_TYPE_CALL_MISSED 			= 3;
	public static final int MESSAGE_TYPE_SMS_INBOX 				= 1;
	public static final int MESSAGE_TYPE_SMS_SENTBOX			= 2;
	public static final int MESSAGE_TYPE_MMS_INBOX				= 1;
	public static final int MESSAGE_TYPE_MMS_SENTBOX			= 2;
	
	//contact mime type	
	//they are defined in the contact database
	//public static final int EMAILADDRESS_MIMETYPE 				= 1;  //reserved
	public static final int MIMETYPE_PHOTOID 					= 4;
	public static final int MIMETYPE_PNONENUMBER 				= 5;
	public static final int MIMETYPE_DISPLAYNAME 				= 6;
	public static final int MIMETYPE_NICKNAME 				= 8;
	
	public static final String ACTION 				= "action";
	public static final String MIME_TYPE			= "mime_type";
	public static final String NEW_DATA				= "new_data";
	public static final String OLD_DATA				= "old_data";

	public static final String COLUMN_TOTAL_COUNT	= "total_count";
	public static final String COLUMN_CURRENT_POS	= "current_pos";
	
	public static final int TOTAL_DAY				= 8;
	public static final String MISC_TOTAL_DAY = "total_day";
	public static final int MISC_TOTAL_DAY_DEFAULT 		= 8;
	public static final int MISC_TOTAL_DAY_MAX 		= 20;
	public static final int MISC_TOTAL_DAY_MIN 		= 1;
	
	public static final String MISC_OUT_OF_BOX = "out_of_box";
	public static final String MISC_FIRST_ADDED = "first_added";
	
	public final static String DB_NAME = "/data/data/com.motorola.mmsp.socialGraphService/databases/"+SocialGraphProvider.DATABASE_NAME;
	public static final String[] COLUMNS = new String[] {
        Contacts._ID, // ..........................................0
        Contacts.DISPLAY_NAME, // .................................1
        Contacts.STARRED, // ......................................2
        Contacts.PHOTO_URI, // ....................................3
        Contacts.LOOKUP_KEY, // ...................................4
        Contacts.CONTACT_PRESENCE, // .............................5
        Contacts.CONTACT_STATUS, // ...............................6
    };
}
