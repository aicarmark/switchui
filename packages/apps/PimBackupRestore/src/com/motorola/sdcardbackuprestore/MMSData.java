package com.motorola.sdcardbackuprestore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.io.UnsupportedEncodingException;

import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Mms.Part;
import android.telephony.TelephonyManager;

import android.R.integer;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class MMSData {
	public static final String TAG = "MMSData";
	
	private PDU mPdu = null;
	private ArrayList<ADDR> mAddrList = null;
	private ArrayList<PART> mPartList = null;	
	private boolean mParsingPDU = false;
	private boolean mParsingAddrs = false;
	private boolean mParsingParts = false;	
	private PART mCurrentPart = null;
	private ADDR mCurrentAddr = null;
	private String mCurrentFilePath = null;
	private HashMap<Long, Long> mOldIdToNewIdMap = new HashMap<Long, Long>();
	
	public MMSData(){
		mPdu = null;
		mPartList = null;
		mParsingPDU = false;
		mParsingParts = false;
		mCurrentPart = null;
		mCurrentFilePath = null;
	}
	
	public class PDU{
		public int thread_id = -1;
		public int date = -1;
		public int message_box = -1;
		public int read = 1;
		public String message_id = null;
		public String subject = null;
		public String subject_charset = null;
		public String content_type = null;
		public String content_location = null;		
		public int expiry = -1;
		public String message_class = null;
		public int message_type = -1;
		public int mms_version = -1;
		public int message_size = -1;
		public int priority = -1;
		public int read_report = -1;
		public int report_allow = -1;
		public int response_status = -1;
		public int status = -1;
		public String transaction_id = null;
		public String retrieve_status = null;
		public String retrieve_text = null;
		public String retrieve_text_charset = null;
		public int read_status = -1;
		public int content_class = -1;
		public int delivery_report = -1;
		public int mode = -1;
		public int seen = -1;
		//public long sort_index = -1;
	}
	public class ADDR{
		public int contact_id = -1;
		public String address = null;
		public int type = -1;
		public int charset = -1;
		public String Serialize(){
			StringBuffer b = new StringBuffer();
			b.append(MsgTag.TAG_BEGIN_ADDR).append(MsgTag.LINE_CHANGE);
			if(contact_id != -1) b.append(buildInt(MsgTag.TAG_ADDR_CONTACT_ID,contact_id));
			if(address != null) b.append(buildString(MsgTag.TAG_ADDR_ADDRESS,address));
			if(type != -1) b.append(buildInt(MsgTag.TAG_ADDR_TYPE,type));
			if(charset != -1) b.append(buildInt(MsgTag.TAG_ADDR_CHARSET,charset));
			b.append(MsgTag.TAG_END_ADDR).append(MsgTag.LINE_CHANGE);
			return b.toString();
		}		
	}
	
	public class PART{
		public int seq = 0;
		public String content_type = null;
		public String name = null;
		public int charset = -1;
		public String content_description = null;
		public String filename = null;
		public String content_id = null;
		public String content_location = null;
		public String path = null;
		public String text = null;
		public Uri uri = null;
		public String Serialize(String partsFilepath, ContentResolver cr){
			StringBuffer b = new StringBuffer();
			b.append(MsgTag.TAG_BEGIN_PART).append(MsgTag.LINE_CHANGE);
			if(seq != -1) b.append(buildInt(MsgTag.TAG_SEQ,seq));
			if(content_type != null) b.append(buildString(MsgTag.TAG_PART_CONTENT_TYPE,content_type));
			if(name != null) b.append(buildString(MsgTag.TAG_NAME,name));
			if(charset != -1) b.append(buildInt(MsgTag.TAG_CHARSET,charset));
			if(content_description != null) b.append(buildString(MsgTag.TAG_CONTENT_DISPOSITION,content_description));
			if(filename != null) b.append(buildString(MsgTag.TAG_FILENAME,filename));
			if(content_id != null) b.append(buildString(MsgTag.TAG_CONTENT_ID,content_id));
			if(content_location != null) b.append(buildString(MsgTag.TAG_CONTENT_LOCATION,content_location));
			if(path != null) b.append(buildString(MsgTag.TAG_PATH,path));
			if(text != null) {
				if(content_type.equals("application/smil") || 
						content_type.equals("text/plain")){
					b.append(buildString(MsgTag.TAG_TEXT,toMyMLFormat(text)));
				}else{
					b.append(buildString(MsgTag.TAG_TEXT,text));
				}
			}
			b.append(MsgTag.TAG_END_PART).append(MsgTag.LINE_CHANGE);
			
			if(path != null && path.length() > 0){
				
				String fileName = path.substring(path.lastIndexOf('/')+1);
				String DestFilePath = partsFilepath+ "/" + fileName ;
				InputStream is = null;
				FileOutputStream os = null;
				int readNum = 0;
				try{
				is = cr.openInputStream(uri);
				os = new FileOutputStream(DestFilePath);
				byte[] buf = new byte[10240];				
				while((readNum = is.read(buf,0,10240)) != -1){
					os.write(buf, 0, readNum);
				}				
				}catch(FileNotFoundException e){
					Log.e(TAG, e.getMessage() + e);
				}catch(IOException e){
					Log.e(TAG, e.getMessage() + e);
				}finally{
					if(is != null) 
					try {
					    is.close();
					} catch (IOException e){
					}
					
					if(os != null) try{
					    os.close();
					} catch(IOException e){
					}
				}
			}
			return b.toString();
		}
	}
		
	private static int getIntValue(Cursor cs, String columnName){
		int columnIndex = cs.getColumnIndexOrThrow(columnName);
		if(!cs.isNull(columnIndex)){
			return cs.getInt(columnIndex);
		}else {
			return -1;
		}
	}
	
	private static long getLongValue(Cursor cs, String columnName){
		int columnIndex = cs.getColumnIndexOrThrow(columnName);
		if(!cs.isNull(columnIndex)){
			return cs.getLong(columnIndex);
		}else {
			return -1;
		}
	}
	
	private static String getStrValue(Cursor cs, String columnName){
		int columnIndex = cs.getColumnIndexOrThrow(columnName);
		if(!cs.isNull(columnIndex)){
			return cs.getString(columnIndex);
		}else {
			return null;
		}
	}
	public  boolean fromPduId(ContentResolver cr, int id){
		Cursor cs = cr.query(Mms.CONTENT_URI, null, Mms._ID + "=" + id, null, null);
		if(cs == null) return false;
		if(cs.getCount()< 0) {
			cs.close();
			return false;
		}		
		cs.moveToFirst();		
		PDU pdu = new PDU();
		pdu.thread_id = getIntValue(cs, Mms.THREAD_ID);
		pdu.date = getIntValue(cs, Mms.DATE);
		pdu.message_box = getIntValue(cs, Mms.MESSAGE_BOX);
		pdu.message_id = getStrValue(cs, Mms.MESSAGE_ID);
		pdu.subject_charset = getStrValue(cs, Mms.SUBJECT_CHARSET);
		pdu.subject = getStrValue(cs, Mms.SUBJECT);
		if(pdu.subject != null 
		    && pdu.subject.length() > 0 
		    && pdu.subject_charset != null
		    && pdu.subject_charset.length() > 0){
		    try{
		        pdu.subject = new String(pdu.subject.getBytes(),
			getCharset(Integer.parseInt(pdu.subject_charset)));
		    } catch(UnsupportedEncodingException e){
		        Log.e(TAG, "UnsupportedEncodingException " + e);
		    }
		}
		pdu.content_type = getStrValue(cs, Mms.CONTENT_TYPE);
		pdu.content_location = getStrValue(cs, Mms.CONTENT_LOCATION);		
		pdu.expiry = getIntValue(cs, Mms.EXPIRY);
		pdu.message_class = getStrValue(cs, Mms.MESSAGE_CLASS);
		pdu.message_type = getIntValue(cs, Mms.MESSAGE_TYPE);
		pdu.mms_version = getIntValue(cs, Mms.MMS_VERSION);
		pdu.message_size = getIntValue(cs, Mms.MESSAGE_SIZE);
		pdu.priority = getIntValue(cs, Mms.PRIORITY);
		pdu.read_report = getIntValue(cs, Mms.READ_REPORT);		
		pdu.report_allow = getIntValue(cs, Mms.REPORT_ALLOWED);
		pdu.response_status = getIntValue(cs, Mms.RESPONSE_STATUS);
		pdu.status = getIntValue(cs, Mms.STATUS);
		pdu.transaction_id = getStrValue(cs, Mms.TRANSACTION_ID);
		pdu.retrieve_status = getStrValue(cs, Mms.RETRIEVE_STATUS);
		pdu.retrieve_text = getStrValue(cs,Mms.RETRIEVE_TEXT);
		pdu.retrieve_text_charset = getStrValue(cs, Mms.RETRIEVE_TEXT_CHARSET);		
		pdu.read_status = getIntValue(cs, Mms.READ_STATUS);
		pdu.content_class = getIntValue(cs, Mms.CONTENT_CLASS);
		pdu.delivery_report = getIntValue(cs, Mms.DELIVERY_REPORT);
		if(SMSManager.getInstance().haveSubIdCol()){
			pdu.mode = getIntValue(cs, "sub_id");
		}else{
			pdu.mode = -1;	
		}

		pdu.seen = getIntValue(cs, Mms.SEEN);
		//pdu.sort_index = getLongValue(cs, Mms.SORT_INDEX);
		cs.close();
		
		mPdu = pdu;
		
		Uri.Builder builder = Mms.CONTENT_URI.buildUpon();
        builder.appendPath(Integer.toString(id)).appendPath("addr");
        Uri addrUri = builder.build();
        
		cs = cr.query(addrUri, null, null, null, null);
		int c = 0;
		if(cs != null) c = cs.getCount();
		if(c > 0) {
			ArrayList<ADDR> addrs = new ArrayList<ADDR>();
			cs.moveToFirst();			
			while(!cs.isAfterLast()){
				ADDR addr = new ADDR();				
				addr.contact_id = getIntValue(cs, Addr.CONTACT_ID);
				addr.address = getStrValue(cs, Addr.ADDRESS);
				addr.type = getIntValue(cs, Addr.TYPE);
				addr.charset = getIntValue(cs, Addr.CHARSET);
				addrs.add(addr);
				cs.moveToNext();
			}
			mAddrList = addrs;
		}
		
		cs.close();	
		
		builder = Mms.CONTENT_URI.buildUpon();
		builder.appendPath(Integer.toString(id)).appendPath("part");
        Uri partUri = builder.build();
        
		cs = cr.query(partUri, null, null, null, null);
		if(cs != null) c = cs.getCount();
		else c = 0;	
		if(c > 0) {
			ArrayList<PART> parts = new ArrayList<PART>();
			cs.moveToFirst();			
			while(!cs.isAfterLast()){
				PART part = new PART();				
				part.content_type = getStrValue(cs, Part.CONTENT_TYPE);
				part.name = getStrValue(cs, Part.NAME);
				part.charset = getIntValue(cs, Part.CHARSET);
				part.content_description = getStrValue(cs, Part.CONTENT_DISPOSITION);
				part.filename = getStrValue(cs, Part.FILENAME);
				part.content_id = getStrValue(cs, Part.CONTENT_ID);
				part.content_location = getStrValue(cs, Part.CONTENT_LOCATION);
				part.path = getStrValue(cs, Part._DATA);
				part.text = getStrValue(cs, Part.TEXT);
				part.uri = Uri.parse("content://mms/part/" + 
						cs.getInt(cs.getColumnIndexOrThrow(Part._ID)));
				parts.add(part);
				cs.moveToNext();
			}
			mPartList = parts;
		}
		
		cs.close();		
		return true;
	}
	
	public String serialize(String path, ContentResolver cr){
		StringBuffer b = new StringBuffer();
		b.append(MsgTag.tagBegMsg).append(MsgTag.LINE_CHANGE);
		b.append(MsgTag.tagMsgVer).append(MsgTag.LINE_CHANGE);
		b.append(MsgTag.tagIrmsTyp + MMSConstants.MMS_STYLE).append(MsgTag.LINE_CHANGE);
		if(mPdu.message_box != -1) b.append(MsgTag.tagMsgTyp).append(mPdu.message_box == 1 ? Constants.DELIVER : Constants.SUBMIT).append(MsgTag.LINE_CHANGE);
		if(mPdu.mode != -1) b.append(MsgTag.tagMATyp).append(mPdu.mode).append(MsgTag.LINE_CHANGE);
		if(mPdu.seen != -1) b.append(MsgTag.tagSeen).append(mPdu.seen).append(MsgTag.LINE_CHANGE);
		//if(mPdu.sort_index != -1) b.append(MsgTag.tagSortIndex).append(mPdu.sort_index).append(MsgTag.LINE_CHANGE);
		if(mPdu.thread_id != -1) b.append(buildInt(MsgTag.TAG_THREAD_ID, mPdu.thread_id));
		if(mPdu.date != -1) b.append(buildInt(MsgTag.TAG_DATE, mPdu.date));
		if(mPdu.message_id != null) b.append(buildString(MsgTag.TAG_MESSAGE_ID, mPdu.message_id));
		if(mPdu.subject != null) b.append(buildString(MsgTag.TAG_SUBJECT, mPdu.subject));
		if(mPdu.subject_charset != null) b.append(buildString(MsgTag.TAG_SUBJECT_CHARSET, mPdu.subject_charset));
		if(mPdu.content_type != null) b.append(buildString(MsgTag.TAG_CONTENT_TYPE, mPdu.content_type));
		if(mPdu.content_location != null) b.append(buildString(MsgTag.TAG_CONTENT_LOCATION, mPdu.content_location));
		if(mPdu.expiry != -1) b.append(buildInt(MsgTag.TAG_EXPIRY, mPdu.expiry));
		if(mPdu.message_class != null) b.append(buildString(MsgTag.TAG_MESSAGE_CLASS, mPdu.message_class));
		if(mPdu.message_type != -1) b.append(buildInt(MsgTag.TAG_MESSAGE_TYPE, mPdu.message_type));
		if(mPdu.mms_version != -1) b.append(buildInt(MsgTag.TAG_MMS_VERSION, mPdu.mms_version));		
		if(mPdu.message_size != -1) b.append(buildInt(MsgTag.TAG_MESSAGE_SIZE, mPdu.message_size));
		if(mPdu.priority != -1) b.append(buildInt(MsgTag.TAG_PRIORITY, mPdu.priority));
		if(mPdu.read_report != -1) b.append(buildInt(MsgTag.TAG_READ_REPORT, mPdu.read_report));
		if(mPdu.report_allow != -1) b.append(buildInt(MsgTag.TAG_REPORT_ALLOWED, mPdu.report_allow));
		if(mPdu.status != -1) b.append(buildInt(MsgTag.TAG_STATUS, mPdu.status));
		if(mPdu.response_status != -1) b.append(buildInt(MsgTag.TAG_RESPONSE_STATUS, mPdu.response_status));
		if(mPdu.transaction_id != null) b.append(buildString(MsgTag.TAG_TRANSACTION_ID, mPdu.transaction_id));
		if(mPdu.retrieve_status != null) b.append(buildString(MsgTag.TAG_RETRIEVE_STATUS, mPdu.retrieve_status));
		if(mPdu.retrieve_text != null) b.append(buildString(MsgTag.TAG_RETRIEVE_TEXT, mPdu.retrieve_text));
		if(mPdu.retrieve_text_charset != null) b.append(buildString(MsgTag.TAG_RETRIEVE_TEXT_CHARSET, mPdu.retrieve_text_charset));		
		if(mPdu.read_status != -1) b.append(buildInt(MsgTag.TAG_READ_STATUS, mPdu.read_status));
		if(mPdu.content_class != -1) b.append(buildInt(MsgTag.TAG_CONTENT_CLASS, mPdu.content_class));
		if(mPdu.delivery_report != -1) b.append(buildInt(MsgTag.TAG_DELIVERY_REPORT, mPdu.delivery_report));
				
		if(mAddrList != null){
			b.append(MsgTag.TAG_BEGIN_ADDRS).append(MsgTag.LINE_CHANGE);
			for(ADDR addr : mAddrList){
				b.append(addr.Serialize());
			}
			b.append(MsgTag.TAG_END_ADDRS).append(MsgTag.LINE_CHANGE);
		} 
		
		if(mPartList != null){
			b.append(MsgTag.TAG_BEGIN_PARTS).append(MsgTag.LINE_CHANGE);
			for(PART part : mPartList){
				b.append(part.Serialize(path+"/"+Constants.FILEPATH_PARTS,cr));
			}
			b.append(MsgTag.TAG_END_PARTS).append(MsgTag.LINE_CHANGE);
		} 
		b.append(MsgTag.tagEndMsg).append(MsgTag.LINE_CHANGE).append(MsgTag.LINE_CHANGE);
		
		return b.toString();
	}
	
	public boolean deSerialize(String absFilePath){
		mCurrentFilePath = absFilePath;
		BufferedReader buf = null;
		try{
		    buf = new BufferedReader(new FileReader(absFilePath));
		}catch(FileNotFoundException e){
		    e.printStackTrace();
		}
		try {
		String line = buf.readLine();
		mParsingPDU = true;
		mParsingParts = false;
		mParsingAddrs = false;
		while(line!=null){
			String[] strArray = line.split(":", 2);
			if( strArray.length == 2 && strArray[0] != null && strArray[1] != null) {			
				parseProperty(strArray[0], strArray[1]);
			}
			line = buf.readLine();
		}
		}catch(IOException e){
			e.printStackTrace();
		}	
		return true;
	}
	
    public boolean deSerialize(String mmsFolder, String vmg) {
        mPdu = null;
        mAddrList = null;
        mPartList = null;
        mCurrentPart = null;
        mCurrentAddr = null;
        mCurrentFilePath = mmsFolder;
        BufferedReader buf = null;
        buf = new BufferedReader(new StringReader(vmg));

        try {
            String line = buf.readLine();
            mParsingPDU = true;
            mParsingParts = false;
            mParsingAddrs = false;
            while (line != null) {
                String[] strArray = line.split(":", 2);
                if (strArray.length == 2 && strArray[0] != null
                        && strArray[1] != null) {
                    parseProperty(strArray[0], strArray[1]);
                }
                line = buf.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
	
	private void parseProperty(String propName, String propValue){
		if(mParsingPDU){
			parsePduProperty(propName, propValue);
		}else if(mParsingAddrs){
			parseAddrsProperty(propName, propValue);
		}else if(mParsingParts){
			parsePartsProperty(propName, propValue);
		}
	}
	

	private void parsePduProperty(String propName, String propValue) {
		
		if(propName.equals("BEGIN") && propValue.equals("VMSG")){
			mPdu = new PDU();
		} else if(propName.equals(MsgTag.TAG_THREAD_ID)){
			mPdu.thread_id = Integer.parseInt(propValue);
		} else if (propName.equals(MsgTag.tagSeen.substring(0, MsgTag.tagSeen.length()-1))) {
			mPdu.seen = Integer.parseInt(propValue);
		//} else if (propName.equals(MsgTag.tagSortIndex.substring(0, MsgTag.tagSortIndex.length()-1))) {
		//	mPdu.sort_index = Long.parseLong(propValue);
		}else if(propName.equals(MsgTag.TAG_DATE)){
			char ch = propValue.charAt(0);
        	if(!Character.isDigit(ch)){
        		try {
        		    long longDate = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH)
				.parse(propValue).getTime()/1000;
        		    mPdu.date = (new Long(longDate)).intValue();
        		} catch (ParseException e1) {
        		    mPdu.date = 0;
        		}
        	} else {   	
        		mPdu.date = Integer.parseInt(propValue);
        	}
		}else if(propName.equals(MsgTag.tagMsgTyp.substring(0, MsgTag.tagMsgTyp.length() - 1))){
			if (propValue.equals(Constants.SUBMIT)) {
				mPdu.message_box = Sms.MESSAGE_TYPE_SENT;
			} else if (propValue.equals(Constants.DELIVER)) {
				mPdu.message_box = Sms.MESSAGE_TYPE_INBOX;
			}
		}else if(propName.equals((MsgTag.tagMATyp).substring(0, MsgTag.tagMATyp.length()-1))){
			if(propValue != null){
				if(propValue.contains("1") || propValue.contains("GSM")){
					mPdu.mode = TelephonyManager.PHONE_TYPE_GSM;
				}else{
					mPdu.mode = TelephonyManager.PHONE_TYPE_CDMA;
				}
			}
		}else if(propName.equals(MsgTag.TAG_MESSAGE_ID)){
			mPdu.message_id = propValue;
		}else if(propName.equals(MsgTag.TAG_SUBJECT)){
			mPdu.subject = propValue;
		}else if(propName.equals(MsgTag.TAG_SUBJECT_CHARSET)){
			mPdu.subject_charset = propValue;
		}else if(propName.equals(MsgTag.TAG_CONTENT_TYPE)){
			mPdu.content_type = propValue;  
		}else if(propName.equals(MsgTag.TAG_CONTENT_LOCATION)){
			mPdu.content_location = propValue;
		}else if(propName.equals(MsgTag.TAG_EXPIRY)){
			mPdu.expiry = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_MESSAGE_CLASS)){
			mPdu.message_class = propValue;
		}else if(propName.equals(MsgTag.TAG_MESSAGE_TYPE)){
			mPdu.message_type = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_MMS_VERSION)){
			mPdu.mms_version = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_MESSAGE_SIZE)){
			mPdu.message_size = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_PRIORITY)){
			mPdu.priority = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_READ_REPORT)){
			mPdu.read_report = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_REPORT_ALLOWED)){
			mPdu.report_allow = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_STATUS)){
			mPdu.status = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_RESPONSE_STATUS)){
			mPdu.response_status = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_TRANSACTION_ID)){
			mPdu.transaction_id = propValue;
		}else if(propName.equals(MsgTag.TAG_RETRIEVE_STATUS)){
			mPdu.retrieve_status = propValue;
		}else if(propName.equals(MsgTag.TAG_RETRIEVE_TEXT)){
			mPdu.retrieve_text = propValue;
		}else if(propName.equals(MsgTag.TAG_RETRIEVE_TEXT_CHARSET)){
			mPdu.retrieve_text_charset = propValue;
		}else if(propName.equals(MsgTag.TAG_READ_STATUS)){
			mPdu.read_status = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_CONTENT_CLASS)){
			mPdu.content_class = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_DELIVERY_REPORT)){
			mPdu.delivery_report = Integer.parseInt(propValue);
		}else if(propName.equals("BEGIN") && propValue.endsWith("ADDRS")){
			mAddrList = new ArrayList<ADDR>();
			mParsingAddrs = true;
			mParsingPDU = false;
		}else if(propName.equals("BEGIN") && propValue.endsWith("PARTS")){
			mPartList = new ArrayList<PART>();
			mParsingAddrs = false;
			mParsingParts = true;
		}			
	}

	private void parseAddrsProperty(String propName, String propValue) {
		if(propName.equals("BEGIN") && propValue.equals("ADDR")){
			mCurrentAddr = new ADDR();
		} else if(propName.equals("END") && propValue.equals("ADDR")){
			mAddrList.add(mCurrentAddr);
		}else if(propName.equals(MsgTag.TAG_ADDR_CONTACT_ID)){
			mCurrentAddr.contact_id = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_ADDR_ADDRESS)){
			mCurrentAddr.address = propValue;
		}else if(propName.equals(MsgTag.TAG_ADDR_TYPE)){
			mCurrentAddr.type = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_ADDR_CHARSET)){
			mCurrentAddr.charset = Integer.parseInt(propValue);
		}else if(propName.equals("BEGIN") && propValue.endsWith("PARTS")){
			mPartList = new ArrayList<PART>();
			mParsingAddrs = false;
			mParsingParts = true;
		}		
	}
	private void parsePartsProperty(String propName, String propValue) {
		if(propName.equals("BEGIN") && propValue.equals("PART")){
			mCurrentPart = new PART();
		} else if(propName.equals("END") && propValue.equals("PART")){
			mPartList.add(mCurrentPart);
		}else if(propName.equals(MsgTag.TAG_SEQ)){
			mCurrentPart.seq = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_PART_CONTENT_TYPE)){
			mCurrentPart.content_type = propValue;
		}else if(propName.equals(MsgTag.TAG_NAME)){
			mCurrentPart.name = propValue;
		}else if(propName.equals(MsgTag.TAG_CHARSET)){
			mCurrentPart.charset = Integer.parseInt(propValue);
		}else if(propName.equals(MsgTag.TAG_CONTENT_DISPOSITION)){
			mCurrentPart.content_description = propValue;
		}else if(propName.equals(MsgTag.TAG_FILENAME)){
			mCurrentPart.filename = propValue;
		}else if(propName.equals(MsgTag.TAG_CONTENT_ID)){
			mCurrentPart.content_id = propValue;
		}else if(propName.equals(MsgTag.TAG_CONTENT_LOCATION)){
			mCurrentPart.content_location = propValue;
		}else if(propName.equals(MsgTag.TAG_PATH)){
			mCurrentPart.path = propValue;
		}else if(propName.equals(MsgTag.TAG_TEXT)){
			if(mCurrentPart.content_type.equals("application/smil")
					|| mCurrentPart.content_type.equals("text/plain")){
				mCurrentPart.text = fromMyMLFormat(propValue);
			}
		}else if(propName.equals("END") && propValue.endsWith("PARTS")){
			mParsingParts = false;
			mParsingPDU = false;
		}		
	}
	
	private String buildString(String tag, String value){
		StringBuffer b = new StringBuffer(tag).append(MMSConstants.PROP_SEP);		
		if(value!=null && value.length()>0){
			b.append(value);
		}
		return b.append(MsgTag.LINE_CHANGE).toString();
	}
	
	private String buildInt(String tag, int value){
		StringBuffer b = new StringBuffer(tag).append(MMSConstants.PROP_SEP);
		b.append(Integer.valueOf(value).toString());
		return b.append(MsgTag.LINE_CHANGE).toString();
	}
	
	private String buildBoolean(String tag, Boolean value){
		StringBuffer b = new StringBuffer(tag).append(MMSConstants.PROP_SEP);
		b.append(value.toString());
		return b.append(MsgTag.LINE_CHANGE).toString();
	}
	
	public void pushIntoContentResolver(Context ct){
		ContentResolver cr = ct.getContentResolver();
		HashSet<String> addrSet = new HashSet<String>();
		ArrayList<String> addrArray = new ArrayList<String>();
		if(mAddrList != null && mAddrList.size() > 0){
        	    for(ADDR addr : mAddrList){
        		//for outgoing message , we only query send addr
        		//for received message , we only query receive addr
        		if( (mPdu.message_type == MMSConstants.MSG_TYPE_REC
        			&& addr.type == MMSConstants.ADDR_TYPE_FROM) 
        			||(mPdu.message_type == MMSConstants.MSG_TYPE_SEND
        			&& addr.type == MMSConstants.ADDR_TYPE_TO)) {
        			addrSet.add(addr.address);
        		}
		    }
		    //for some special case , there will be no TO address for outgoing msg
		    //or no FROM address for received msg, we set the first address by default.
		    if(addrSet.size() == 0){
			addrSet.add(mAddrList.get(0).address);
		    }
    		}
		
		Log.d(TAG, "addrSet : " + addrSet.toString());
		long threadId;
		if (mOldIdToNewIdMap.containsKey(new Long(mPdu.thread_id))) {
            threadId = mOldIdToNewIdMap.get(new Long(mPdu.thread_id));
        } else {
            threadId = Threads.getOrCreateThreadId(ct, addrSet);
            mOldIdToNewIdMap.put(new Long(mPdu.thread_id), threadId);
        }
		Log.d(TAG, "threadId = " + threadId);
		
		ContentValues cv = new ContentValues();		
		cv.put(Mms.THREAD_ID, threadId);
		if(mPdu.date != -1) cv.put(Mms.DATE, mPdu.date);
		if(mPdu.seen != -1) cv.put(Mms.SEEN, mPdu.seen); else cv.put(Mms.SEEN, 1);
		// mms date's measure unit is seconds, sort_index's measure unit is milliseconds, so sort_index = date * 1000
		//if(mPdu.sort_index != -1) cv.put(Mms.SORT_INDEX, mPdu.sort_index); else cv.put(Mms.SORT_INDEX, mPdu.date * 1000);
		if(mPdu.message_box != -1) cv.put(Mms.MESSAGE_BOX, mPdu.message_box);
		if(mPdu.read != -1) cv.put(Mms.READ, mPdu.read);
		if(mPdu.message_id != null) cv.put(Mms.MESSAGE_ID, mPdu.message_id);
		if(mPdu.subject != null) cv.put(Mms.SUBJECT, mPdu.subject);
		if(mPdu.subject_charset != null) cv.put(Mms.SUBJECT_CHARSET, mPdu.subject_charset);
		if(mPdu.content_type != null) cv.put(Mms.CONTENT_TYPE, mPdu.content_type);
		if(mPdu.content_location != null) cv.put(Mms.CONTENT_LOCATION, mPdu.content_location);		
		if(mPdu.expiry != -1) cv.put(Mms.EXPIRY, mPdu.expiry);
		if(mPdu.message_class != null) cv.put(Mms.MESSAGE_CLASS, mPdu.message_class);
		if(mPdu.message_type != -1) cv.put(Mms.MESSAGE_TYPE, mPdu.message_type);
		if(mPdu.mms_version != -1) cv.put(Mms.MMS_VERSION, mPdu.mms_version);
		if(mPdu.message_size != -1) cv.put(Mms.MESSAGE_SIZE, mPdu.message_size);
		if(mPdu.priority != -1) cv.put(Mms.PRIORITY, mPdu.priority);
		if(mPdu.read_report != -1) cv.put(Mms.READ_REPORT, mPdu.read_report);
		if(mPdu.report_allow != -1) cv.put(Mms.REPORT_ALLOWED, mPdu.report_allow);
		if(mPdu.response_status != -1) cv.put(Mms.RESPONSE_STATUS, mPdu.response_status);
		if(mPdu.status != -1) cv.put(Mms.STATUS, mPdu.status);
		if(mPdu.transaction_id != null) cv.put(Mms.TRANSACTION_ID, mPdu.transaction_id);
		if(mPdu.retrieve_status != null) cv.put(Mms.RETRIEVE_STATUS, mPdu.retrieve_status);
		if(mPdu.retrieve_text != null) cv.put(Mms.RETRIEVE_TEXT, mPdu.retrieve_text);
		if(mPdu.retrieve_text_charset != null) cv.put(Mms.RETRIEVE_TEXT_CHARSET, mPdu.retrieve_text_charset);
		if(mPdu.read_status != -1) cv.put(Mms.READ_STATUS, mPdu.read_status);
		if(mPdu.content_class != -1) cv.put(Mms.CONTENT_CLASS, mPdu.content_class);
		if(mPdu.delivery_report != -1) cv.put(Mms.DELIVERY_REPORT, mPdu.delivery_report);
		if(SMSManager.getInstance().haveSubIdCol()){
			if(mPdu.mode != -1) cv.put("sub_id", mPdu.mode);
		}
		
		Uri uri = cr.insert(Mms.CONTENT_URI, cv);
		String msgId = uri.getLastPathSegment();		
		
		if(mAddrList != null && mAddrList.size() > 0){
			Uri.Builder builder = Mms.CONTENT_URI.buildUpon();
			builder.appendPath(msgId).appendPath("addr");
        	Uri addrUri = builder.build();        
        
        	for(ADDR addr : mAddrList){
        		cv = new ContentValues();
        		if(addr.contact_id != -1) cv.put(Addr.CONTACT_ID, addr.contact_id);
        		if(addr.address != null) cv.put(Addr.ADDRESS, addr.address);
        		if(addr.type != -1) cv.put(Addr.TYPE, addr.type);
        		if(addr.charset != -1) cv.put(Addr.CHARSET, addr.charset);
        		cr.insert(addrUri, cv);
        	}
    	}
		
		if(mPartList != null && mPartList.size() > 0){
			Uri.Builder builder = Mms.CONTENT_URI.buildUpon();
			builder.appendPath(msgId).appendPath("part");
        	Uri partUri = builder.build();
        
        	for(PART part : mPartList){
        		cv = new ContentValues();
        		if(part.seq != -1) cv.put(Part.SEQ, part.seq);
        		if(part.content_type != null) cv.put(Part.CONTENT_TYPE, part.content_type);
        		if(part.name != null) cv.put(Part.NAME, part.name);
        		if(part.charset != -1) cv.put(Part.CHARSET, part.charset);
        		if(part.content_description != null) cv.put(Part.CONTENT_DISPOSITION, part.content_description);
        		if(part.filename != null) cv.put(Part.FILENAME, part.filename);
        		if(part.content_id != null) cv.put(Part.CONTENT_ID, part.content_id);
        		if(part.content_location != null) cv.put(Part.CONTENT_LOCATION, part.content_location);
        		if(part.text != null) cv.put(Part.TEXT, part.text);			
        		Uri outUri = cr.insert(partUri, cv);
			
        		if(part.path != null && part.path.length() > 0){
        			String fileName = part.path.substring(part.path.lastIndexOf('/')+1);
        			String sourceFilePath = mCurrentFilePath + "/" +
        			Constants.FILEPATH_PARTS + "/" + fileName;
        			FileInputStream is = null;
        			OutputStream os = null;
        			int readNum = 0;
        			try{
        				is = new FileInputStream(sourceFilePath);
        				os = cr.openOutputStream(outUri);
        				byte[] buf = new byte[10240];				
        				while((readNum = is.read(buf,0,10240)) != -1){
        					os.write(buf, 0, readNum);
        				}
        			}catch(FileNotFoundException e){
        				Log.e(TAG,e.getMessage()+e);
        			}catch(IOException e){
        				Log.e(TAG, e.getMessage() + e);
        			}finally{
        				if(is != null)
        					try {
        						is.close();
        					} catch (IOException e){
        						e.printStackTrace();
        					}

        					if(os != null) try{
        						os.close();
        					} catch(IOException e){
        						e.printStackTrace();
        					}

        			}
        		}
        	}
		}		
	}

	public static int getBackedSMSNumber(File backupRootDir){
		File smsNumFile = new File(backupRootDir, Constants.SMS_FILE_NAME);
		FileInputStream is = null;
		int num = 0;
		if(smsNumFile.exists()){
		    try{
			is = new FileInputStream(smsNumFile);
			int fileLen = is.available();
			if(fileLen > 0){
				byte[] data = new byte[fileLen];
				String smsStr = new String(data);
				int ind = 0 ;
				ind = smsStr.indexOf("BEGIN:VMSG", 0);
				while(ind != -1){
					num ++;
					ind = smsStr.indexOf("BEGIN:VMSG", ind);
				}
			}
		    }catch(FileNotFoundException e){
			e.printStackTrace();
  		    }catch(IOException e){
			e.printStackTrace();
		    }finally{
			if(is != null) {
			    try{
				is.close();
			    }catch(IOException e){
				e.printStackTrace();
			    }
			}
		    }
			
		} 
	return num;
	}
	
	public String getSummary(){
		if(mPartList != null) {
			for(PART part : mPartList){
				if(part.text != null && part.content_type.equalsIgnoreCase("text/plain")){
					return part.text;
				}
			}
		} else if(mPdu.subject != null && mPdu.subject.length() > 0){
			return mPdu.subject;
		}

		return null;	
	}
	public int getThreadId(){
		return mPdu.thread_id;
	}
	
	//avoid multiple lines read problem , we transfer multiple lines to self-format,
	//replace "\r", "\n" , with "&lf;" , "&cr;"
	public static String toMyMLFormat(String str){
		return str.replaceAll("\r", MMSConstants.LF).
		replace("\n", MMSConstants.CR);
	}
	
	public static String fromMyMLFormat(String str){
		return str.replaceAll(MMSConstants.LF, "\r").
		replace(MMSConstants.CR, "\n");
	}
	
	public static String getCharset(int charsetNum){
		String cs = "";
        switch(charsetNum){
        case 0x6A:
            cs = "utf-8";
            break;
        case 0x07EA:
            cs = "big5";
            break;
        default:
            cs = "iso-8859-1";
        }
        Log.d(TAG, "getCharset " + charsetNum + " " + cs);
        return cs;
    }
}
