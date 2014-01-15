package com.motorola.sdcardbackuprestore;

import java.io.File;

import android.R.integer;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.telephony.TelephonyManager;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.os.SystemProperties;

public class Constants extends Application{
	public Context mContext = null;
	//public static final boolean HAS_INTERNAL_SDCARD = com.motorola.android.storage.MotoEnvironment.getExternalAltStorageDirectory() != null ? true : false;
    //public static final String SDCARD_PREFIX = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    //public static final String FILE_PREFIX ="file://" + android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    //public static final String ALT_SDCARD_PREFIX = SdcardManager.getStoragePath();
    //public static final String ALT_FILE_PREFIX = "file://" + ALT_SDCARD_PREFIX;

    public static final String[] CT_MODEL = 
    	{
        	"MOT-XT553",
        	"MOT-XT785",
        	"XT785",
        	"XT535",
        	"XT536",
        	"MOT-XT788",
        	"XT788"

    	};
    public static final String[] TD_MODEL = 
	{
    	"MOT-MT788",
    	"MT788"

	};
    public static boolean isCTModel()
    {
    	String cur_model = SystemProperties.get("ro.product.model");
    	
    	 for(String model: Constants.CT_MODEL) {
    			if(model.equalsIgnoreCase(cur_model))
    			    return true;
    		    }
    	 
    	 return false;
    }
    public static boolean isTDModel()
    {
    	String cur_model = SystemProperties.get("ro.product.model");
    	
    	 for(String model: Constants.TD_MODEL) {
    			if(model.equalsIgnoreCase(cur_model))
    			    return true;
    		    }
    	 
    	 return false;
    }
    public static final boolean isCTPhone = isCTModel();
    
    public static final int INNER_STORAGE = 1;
    public static final int OUTER_STORAGE = 2;
    public static final int PHONE_TYPE = TelephonyManager.getDefault().getPhoneType();
    public static final String FILEPATH_PARTS = "attachments";
    public static final String QN_ATTACH = "qn_attachments";
    public static final String CONTACTS_PHOTO_PATH = "contacts_photo_files";
    public static final String APP_FOLDER = "app";
    public static final String SMS_FILE_NAME = "sms.vmg";
    public static final String CONTACTS_FILE_NAME = "contacts.vcf";
    public static final String QUICKNOTE_FILE_NAME = "qnotes.vnt";
    public static final String CALENDAR_FILE_NAME = "calendar.ics";
    public static final String BOOKMARK_FILE_NAME = "bookmark.htm";
    public static final String SUBMIT = "SUBMIT";
    public static final String DELIVER = "DELIVER";
    public static final String DATE2COLUMN = "date2";
    //public static final String QN_PICTURE_FOLDER_1 = SDCARD_PREFIX + "/" + "quicknote/sketch";
    //public static final String QN_PICTURE_FOLDER_2 = SDCARD_PREFIX + "/" + "quicknote/image";
    //public static final String QN_PICTURE_FOLDER_3 = SDCARD_PREFIX + "/" + "quicknote/snapshot";
    public static final String PREINSTALLFILE_PATH = "/data/preinstall_md5";
    public static final String FILE_MANAGER_FILTER = "file_manager_filter";
    public static final int NOTIFICATION_ID = 1;
    
    public static final String SHARED_PREF_NAME = "com.motorola.sdcardbackuprestore.cautions";
    public static final String NOT_SHOW_ANYMORE = "not_show_anymore";
    
    public static final String CONTACTS = "contacts";
    public static final String SMS = "sms";
    public static final String QUICKNOTE = "quicknote";
    public static final String CALENDAR = "calendar";
    public static final String APP = "app";
    public static final String BOOKMARK = "bookmark";
    public static final String LOCAL = "local";
    public static final String CCARD = "cCard";
    public static final String GCARD = "gCard";
    public static final String GACCOUNT = "gAccount";
    public static final String EACCOUNT = "eAccount";
    public static final String SINGLE_FILE = "file";
    public static final String MULTI_FILES = "files";
    public static final String PATH = "path";
    public static final String ACTION = "action";
    public static final String GACCOUNT_PREFIX = "Gmail";
    public static final String EACCOUNT_PREFIX = "Exchange";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String PATH_LIST = "path_list";
    public static final String URI_LIST = "uri_list";
    
    public static final String DEL_OPTION = "del_option";
    public static final String CHECKED_ACCOUNT_NAMES = "checked_account_names";
    public static final String HANDLER = "handler";
    public static final String vmgExtern = ".vmg";
    public static final String vcardExtern = ".vcf";
    public static final String qnExtern = ".vnt";
    public static final String calExtern = ".ics";
    public static final String calCompatExtern = ".vcs";
    public static final String bookmarkExtern = ".htm";
    public static final String appExtern = ".apk";
    public static final String isFromOutside = "is_from_outside";
    public static final int COMMON_DISPLAY_LEN = 10;
    public static final int SMS_DISPLAY_LEN = 10;
    public static final int APP_DISPLAY_LEN = 25;
    public static final int NO_ACTION = 0;
    public static final int BACKUP_ACTION = 1;
    public static final int RESTORE_ACTION = 2;
    public static final int IMPORT3RD_ACTION = 3;
    public static final int EXPORTBYACCOUNT_ACTION = 4;
    
    public static final int INDEX_CONTACT = 0;
    public static final int INDEX_SMS = 1;
    public static final int INDEX_QUICKNOTE = 2;
    public static final int INDEX_CALENDAR = 3;
    public static final int INDEX_APP = 4;
    public static final int INDEX_BOOKMARK = 5;
    
    public static final int LOCAL_CONTACT = 1;
    public static final int CCARD_CONTACT = 2;
    public static final int GCARD_CONTACT = 3;
    public static final int GACCOUNT_CONTACT = 4;
    public static final int EACCOUNT_CONTACT = 5;
    public static final int CARD_CONTACT = 6;
    
    public static final String LOCAL_CONTACTS_FILE_NAME = "Phone.vcf";
    public static final String CCARD_CONTACTS_FILE_NAME = "CDMA_card.vcf";
    public static final String GCARD_CONTACTS_FILE_NAME = "GSM_card.vcf";
    
    public static final int CONTACTS_ACTION = 1;
    public static final int SMS_ACTION = 2;
    public static final int QUICKNOTE_ACTION = 3;
    public static final int CALENDAR_ACTION = 4;
    public static final int APP_ACTION = 5;
    public static final int BOOKMARK_ACTION = 6;
    
    public static final int LOCAL_ACTION = 31;
    public static final int CCARD_ACTION = 32;
    public static final int GCARD_ACTION = 33;
    public static final int GACCOUNT_ACTION = 34;
    public static final int EACCOUNT_ACTION = 35;
    public static final int DEL_UNSET = -1;
    public static final int DEL_YES = 1;
    public static final int DEL_NO = 0;
    public static final int DEL_CANCEL = 2;
    public static final String THIS_PACKAGE_NAME = "com.motorola.sdcardbackuprestore";
    public static final String TOTAL = "total";
    public static final String CURRENT = "current";
    public static final String DESCRIPTION = "description";

    // For Message Type
    public static final int OTHER_TYPE = 0;
    public static final int SMS_TYPE = 1;
    public static final int MMS_TYPE = 2;
    
    
    //	For pop-up dialog after click notification
    public static final int DIALOG_UNKNOWN = 0;
    public static final int DIALOG_COMPLETE = 1;
    public static final int DIALOG_ONGOING = 2;

    public static final String ACTION_RESULT = "com.motorola.sdcardbackup.action.result";
    public static final String ACTION_ONGOING = "com.motorola.sdcardbackup.action.ongoing";
    public static final String POPUP_DIALOG_TYPE = "popup_dialog_type";
    public static final String ACTION_TYPE = "action_type";
    public static final String POPUP_COMPLETE_INFO = "popup_complete_information";
    
    // For vCard manager
    static final int KIND_EXCHANGE_PHONE = 10;
    static final int KIND_EXCHANGE_EMAIL = 11;
    static final int KIND_EXCHANGE_EXTENSION_OTHER = 15;
    static final int KIND_EXCHANGE_EXTENSION_ORG = 14;
    static final int KIND_EXCHANGE_EXTENSION_ADDRESS = 13;
    static final int KIND_EXCHANGE_EXTENSION_IM = 12;

    public static final int EXTENSIONS_ID_COLUMN = 0;
    public static final int EXTENSIONS_NAME_COLUMN = 1;
    public static final int EXTENSIONS_VALUE_COLUMN = 2;

    static final String JOBTITLE = "Job Title";
    static final String COMPANY = "Company";
    static final String BIRTHDAY = "Birthday";
    
    //for query accounts in the phone
    public static final String LOCAL_ACCOUNT_NAME = "local-contacts";
    public static final String CARD_ACCOUNT_NAME = "card-contacts";
    public static final String CCARD_ACCOUNT_NAME = "c-contacts";
    public static final String GCARD_ACCOUNT_NAME = "g-contacts";
    
    public static final String LOCAL_ACCOUNT_TYPE = "com.local.contacts";
    public static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    public static final String EXCHANGE_ACCOUNT_TYPE = "com.android.exchange";
    public static final String GCARD_TYPE = "com.card.contacts";
    public static final String CCARD_TYPE = "com.card.contacts";
    
    public static final int GENUS_NUM = 5;
    
    public static final int DIALOG_THEME = android.R.style.Theme_Holo_Dialog;
    
    public static final String SPECIFIC_CONTACTS = "specific_contacts";
    public static final String CONTACTS_URI = "contacts_uri";
    
    // For contacts multiple picker
    public static final String SELECTTION_RESULT_INCLUDED = "com.android.contacts.SelectionResultIncluded";
    public static final String SELECTED_CONTACTS= "com.android.contacts.SelectedContacts";
    public static final String SELECTION_RESULT_SESSION_ID = "com.android.contacts.SelectionResultSessionId";
    

 // for overlay string
    public final String DEFAULT_FOLDER;

    public final String SDCARD_PREFIX;
    public final String FILE_PREFIX;
    public final String ALT_SDCARD_PREFIX;
    public final String ALT_FILE_PREFIX;

    public final String QN_PICTURE_FOLDER_1;
    public final String QN_PICTURE_FOLDER_2;
    public final String QN_PICTURE_FOLDER_3;
    public final boolean HAS_INTERNAL_SDCARD;
    
    public Constants(Context context) {
    	mContext = context;
        // for overlay string
        DEFAULT_FOLDER = isCTPhone?"ct_backup": mContext.getString(R.string.default_folder_name);

        SDCARD_PREFIX = SdcardManager.getStoragePath(mContext, false);
        FILE_PREFIX = "file://" + SDCARD_PREFIX;
        ALT_SDCARD_PREFIX = SdcardManager.getStoragePath(mContext, true);
        ALT_FILE_PREFIX = "file://" + ALT_SDCARD_PREFIX;

        HAS_INTERNAL_SDCARD = SdcardManager.hasInternalSdcard(context);

        QN_PICTURE_FOLDER_1 = SDCARD_PREFIX + "/" + "quicknote/sketch";
        QN_PICTURE_FOLDER_2 = SDCARD_PREFIX + "/" + "quicknote/image";
        QN_PICTURE_FOLDER_3 = SDCARD_PREFIX + "/" + "quicknote/snapshot";
    }
    
}
