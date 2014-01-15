package com.motorola.sdcardbackuprestore;

public class MsgTag {
    
    // Common Tag
    public static final String LINE_CHANGE = "\r\n";
    public static final String tagBegMsg = "BEGIN:VMSG";
    public static final String tagEndMsg = "END:VMSG";
    public static final String tagMsgVer = "VERSION: 1.1";
    public static final String tagMsgTyp = "X-MESSAGE-TYPE:";
    public static final String tagMATyp = "X-MA-TYPE:";
    public static final String tagSeqTime = "X-SEQ-TIME:";
    public static final String tagIrmsTyp = "X-IRMS-TYPE:";
    public static final String tagSeen = "SEEN:";
    //public static final String tagStatus = "STATUS:";
    //public static final String tagSortIndex = "SORT-INDEX:";
    
    // SMS Tag
    public static final String tagBegEnv = "BEGIN:VENV\n";
    public static final String tagBegBody = "BEGIN:VBODY\n";
    public static final String tagBegCard = "BEGIN:VCARD\n";
    public static final String tagCardVer = "VERSION: 2.1\n";
    public static final String tagEndEnv = "END:VENV\n";
    public static final String tagEndBody = "END:VBODY\n";
    public static final String tagEndCard = "END:VCARD\n";
    public static final String tagDate = "Date ";
    //public static final String tagHref = "Href:";
    public static final String tagTel = "TEL:";
    
    // MMS Tag
    public static final String TAG_THREAD_ID = "THREAD_ID";
    public static final String TAG_DATE = "DATE";
    public static final String TAG_READ = "READ";
    public static final String TAG_MESSAGE_ID = "MESSAGE_ID";
    public static final String TAG_SUBJECT = "SUBJECT";
    public static final String TAG_SUBJECT_CHARSET = "SUBJECT_CHARSET";
    public static final String TAG_CONTENT_TYPE = "CONTENT_TYPE";
    public static final String TAG_CONTENT_LOCATION = "CONTENT_LOCATION";    
    public static final String TAG_EXPIRY = "EXPIRY";
    public static final String TAG_MESSAGE_CLASS = "MESSAGE_CLASS";
    public static final String TAG_MESSAGE_TYPE = "MESSAGE_TYPE";
    public static final String TAG_MMS_VERSION = "MMS_VERSION";
    public static final String TAG_MESSAGE_SIZE = "MESSAGE_SIZE";
    public static final String TAG_PRIORITY = "PRIORITY";
    public static final String TAG_READ_REPORT = "READ_REPORT";
    public static final String TAG_REPORT_ALLOWED = "REPORT_ALLOWED";
    public static final String TAG_STATUS = "STATUS";
    public static final String TAG_RESPONSE_STATUS = "RESPONSE_STATUS";
    public static final String TAG_TRANSACTION_ID = "TRANSACTION_ID";
    public static final String TAG_RETRIEVE_STATUS = "RETRIEVE_STATUS";
    public static final String TAG_RETRIEVE_TEXT = "RETRIEVE_TEXT";
    public static final String TAG_RETRIEVE_TEXT_CHARSET = "RETRIEVE_TEXT_CHARSET";
    public static final String TAG_READ_STATUS = "READ_STATUS";
    public static final String TAG_CONTENT_CLASS = "CONTENT_CLASS";
    public static final String TAG_DELIVERY_REPORT = "DELIVERY_REPORT";
    
    public static final String TAG_ADDR_CONTACT_ID = "CONTACT_ID";
    public static final String TAG_ADDR_ADDRESS = "ADDRESS";
    public static final String TAG_ADDR_TYPE = "TYPE";
    public static final String TAG_ADDR_CHARSET = "CHARSET";
    public static final String TAG_BEGIN_ADDRS = "BEGIN:ADDRS";
    public static final String TAG_END_ADDRS = "END:ADDRS";
    public static final String TAG_BEGIN_ADDR = "BEGIN:ADDR";
    public static final String TAG_END_ADDR = "END:ADDR";
    
        
    public static final String TAG_BEGIN_PARTS = "BEGIN:PARTS";
    public static final String TAG_END_PARTS = "END:PARTS";
    public static final String TAG_BEGIN_PART = "BEGIN:PART";
    public static final String TAG_END_PART = "END:PART";
    public static final String TAG_SEQ = "SEQ";
    public static final String TAG_PART_CONTENT_TYPE = "CONTENT_TYPE";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_CHARSET = "CHARSET";
    public static final String TAG_CONTENT_DISPOSITION = "CONTENT_DISPOSITION";
    public static final String TAG_FILENAME = "FILENAME";
    public static final String TAG_CONTENT_ID = "CONTENT_ID";
    public static final String TAG_PART_CONTENT_LOCATION = "CONTENT_LOCATION";
    public static final String TAG_PATH = "PATH";
    public static final String TAG_TEXT = "TEXT";
}
