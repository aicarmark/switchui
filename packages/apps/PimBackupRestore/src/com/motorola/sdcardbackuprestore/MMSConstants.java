package com.motorola.sdcardbackuprestore;

public class MMSConstants {
	private MMSConstants() {
    }
	
	public static final int MAX_MSG_NUM = 4000;
	public static final int WRITE_BLOCK = 32768; //32*1024
	public static final String MMS_STYLE = "MMS";
	
	public static final String PROP_SEP = ":";
	
	public static final String LF = "&lf;";
	public static final String CR = "&cr;";
	
	public static final String MSG_BOX = "msg_box";
	public static final String MMS_TYPE = "m_type";
	
	public static final int MSG_TYPE_REC = 0x84;
	public static final int MSG_TYPE_SEND = 0x80;
	public static final int MMS_TYPE_REPORT = 0x86;
	
	public static final int ADDR_TYPE_FROM = 0x89;
	public static final int ADDR_TYPE_TO = 0x97;
}
