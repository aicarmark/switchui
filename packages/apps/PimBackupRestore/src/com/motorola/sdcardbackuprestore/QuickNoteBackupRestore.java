package com.motorola.sdcardbackuprestore;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.motorola.sdcardbackuprestore.VNoteException;


public class QuickNoteBackupRestore {

    private static final String TAG = "QuickNoteBackupRestore";

    public static final String AUTHORITY = "com.motorola.provider.quicknote";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/qnotes");
    
    public static enum QNColumn {
        _ID         (BaseColumns._ID,   "INTEGER PRIMARY KEY",  null),
        TITLE       ("title",           "TEXT",                 ""),
        MIMETYPE    ("mime_type",       "TEXT",                 ""),
        URI         ("uri",             "TEXT",                 ""),    // raw data URI.("file or qntext")
        THUMBURI    ("thumb_uri",       "TEXT",                 ""),    // no use now;store the thumb midia uri, used for mini thumbnails
        ORIGID      ("orig_id",         "LONG",                 null),    // no use now;store the media id, used for micro thumbnails
        BGCOLOR     ("bgcolor",         "INTEGER",              Color.TRANSPARENT),
        REMINDER    ("reminder",        "LONG",                 0),   //store the string of calendar
        WIDGETID    ("widget_id",       "INTEGER",              AppWidgetManager.INVALID_APPWIDGET_ID),
        SPANX       ("span_x",          "INTEGER",              2),
        SPANY       ("span_y",          "INTEGER",              2),
        CALLNUMBER      ("call_number",     "TEXT",                 ""),
        CALLDATEBEGIN   ("call_date_begin", "LONG",                 0),
        CALLDATEEND     ("call_date_end",   "LONG",                 0),
        CALLTYPE        ("call_type",       "INTEGER",              null),
        SUMMARY         ("summary",         "TEXT",                 "");
        
        private String _name;
        private String _type;
        private Object _init_value;
        
        QNColumn(String name, String type, Object init_value) {
            _name = name; _type = type; _init_value = init_value;
        }
        public String column() { return _name; }
        public String type() { return _type; }
        public Object init_value() { return _init_value; }
    }

    public static final String[] EVENT_PROJECTION = new String[] {
        QNColumn._ID.column(),
        QNColumn.TITLE.column(),
        QNColumn.MIMETYPE.column(),
        QNColumn.URI.column(),
        QNColumn.BGCOLOR.column(),
        QNColumn.REMINDER.column(),
        QNColumn.CALLNUMBER.column(),
        QNColumn.CALLDATEBEGIN.column(),
        QNColumn.CALLDATEEND.column(),
        QNColumn.CALLTYPE.column(),
        QNColumn.SUMMARY.column(),
    };
	
   /*vNote format:
    BEGIN:VNOTE
    VERSION:1.1
    BODY:Where is my dog ??
    DCREATED:20061203T181902Z
    LAST-MODIFIED:20061203T181902Z
    END:VNOTE
   */
   public static final String vnoteStartTag = "BEGIN:VNOTE";
   public static final String vnoteEndTag = "END:VNOTE";
   public static final String vnoteVersionTag = "VERSION:1.1";
   public static final String vnoteBodyTag = "BODY:";
   
   private Context mContext = null;
   private Constants mConstants = null;
   public QuickNoteBackupRestore(Context context) {
        mContext = context;
        mConstants = new Constants(mContext);
   }

   /**
     Get all quick notes cursor.
     **/
   public static Cursor queryQuickNote(ContentResolver CR) {
   	Cursor qnCursor = null;
        qnCursor = CR.query(CONTENT_URI, EVENT_PROJECTION, null, null, QNColumn._ID + " DESC");
        return qnCursor;

   }


   /**
     Get QuickNote format string by providing QuickNote record cursor and fill subject with the quicknotes 'SUBJECT'. Return null and throw exception if fail.
 * @throws JSONException 
     **/
    public String backupQuickNote(Cursor cursor, StringBuffer subject) throws JSONException   {

        Log.d(TAG, "backupQuickNote");
		
        String dbData = new String();
	    ArrayList<JSONObject> eventAarray = new  ArrayList<JSONObject>();
	    int columnCount = cursor.getColumnCount();
	    JSONObject jo = new JSONObject();
	    for(int i=0;i<columnCount;i++){
	        jo.put(cursor.getColumnName(i), cursor.getString(i));
	        Log.d(TAG, "ColumnName ( "+cursor.getColumnName(i)+")  = "+cursor.getString(i));
	    }
			
        eventAarray.add(jo);
	    JSONArray jsonAarray = new JSONArray(eventAarray);
     	dbData = vnoteStartTag +"\n"+vnoteVersionTag+"\n"+vnoteBodyTag+ jsonAarray.toString()+ "\n"+vnoteEndTag+"\n";
        String title = cursor.getString(cursor.getColumnIndex(QNColumn.TITLE.column()));
        if (null != title) {
           subject.delete(0, subject.length()).append(title);
        }
	    return dbData;
    }


   /**
     Get  the related media file for copy
     **/
    public Uri getFileUri(Cursor cursor) {
        if (cursor == null) {
            Log.d(TAG, "Cannot query the content");
            return null;
        }

	Uri FilePath = Uri.parse(cursor.getString(cursor.getColumnIndex(QNColumn.URI.column())));
        return FilePath;
    }
    

   /**
     Parse QuickNote format string and add it to QuickNotes database, return subject of this record if success, return null if fail.
 * @throws JSONException 
     **/
    public String restoreQuickNote (Context context, String data,  String fromDir) throws VNoteException, JSONException  {
    	 Log.i(TAG,"restoreQuickNote");
         String subject = null;
         ContentResolver resolver =  context.getContentResolver();
	 int pos = 0;
	 int length = 0;

         // To count the number of "END:VNOTE\n"
         pos = data.indexOf(vnoteStartTag);
         while (-1 != (pos=data.indexOf(vnoteEndTag, pos)))  {
            pos += vnoteEndTag.length();
            length ++;
         }
         Log.d(TAG,"qn  total number = "+length);
         Log.d(TAG, "begin restore event, data.length = " +data.length());
         pos = 0;
         for(int i=0; i<length; i++) {
    	    Log.d(TAG, "i ="+i+" ,  pos = "+pos);
	    Uri event_uri = null;
	    boolean setReminder = false;
	    Long reminder = 0L;
	    String fileUri_string = null;
	    int pos_start = data.indexOf(vnoteBodyTag, pos);
	    Log.d(TAG,"pos_start = "+pos_start);
	    pos_start = pos_start + vnoteBodyTag.length();
	    int pos_end = data.indexOf(vnoteEndTag, pos);
	    Log.d(TAG,"pos_end = "+pos_end);
	    pos_end = pos_end - 1;
	    Log.d(TAG,"need to copy from "+pos_start+" to "+pos_end);
	    String rowData = data.substring(pos_start, pos_end);
	    Log.d(TAG,"get rowData = "+rowData);
	    pos = pos_end +vnoteEndTag.length();
		
	    JSONArray eventsAarray = new JSONArray(rowData);
	    JSONObject event_jo = eventsAarray.getJSONObject(0);
	    @SuppressWarnings("unchecked")
	    Iterator<String> keys = event_jo.keys();
	    ContentValues eventValues = new ContentValues();
	    while (keys.hasNext()) {
	       String key = keys.next();
	       if (key.equalsIgnoreCase(QNColumn._ID.column())) {
	          continue;
	       }
	  
	       String value = event_jo.getString(key);
	       Log.v("exception 3", "3");
	       eventValues.put(key, value);
	       Log.d(TAG,key+" : "+value);
	       //check the reminder
	       if (key.equalsIgnoreCase(QNColumn.REMINDER.column())) {
                  reminder = Long.parseLong(value);
	          if (reminder > 0 ) {
                     Log.d(TAG,"has reminder! reminder = "+reminder);
	             setReminder = true;
	          }
               }

               //get the subject (title)
               if (key.equalsIgnoreCase(QNColumn.TITLE.column())) {
                 Log.d(TAG,"get the title = "+value);
                 subject = value;
	       }
						 
	       //get the file name
	       if (key.equalsIgnoreCase(QNColumn.URI.column())) {
	         Log.d(TAG,"get the file name = "+value);
                 fileUri_string = value;
               }
	  }
			 
	  //copy the attachment from dir to quicknote dir
	  Log.d(TAG,"copy the attachment");
	  //change the string from file:///mnt/sdcard/sketch/filename.png to /mnt/sdcard/sketch/filename.png
	  String fullpath = transFilePath(fileUri_string);
	// modified by amt_jiayuanyuan 2012-12-29 SWITCHUITWO-596 begin
	  if(fullpath.startsWith("/mnt")){
		  fullpath = transFilePathForJB(fullpath);
	  }
   // modified by amt_jiayuanyuan 2012-12-29 SWITCHUITWO-596 end
	  int pos_2 = fullpath.lastIndexOf("/");
	  // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-318 begin
	  //String filedir = convertToInternalPath(fullpath.substring(0, pos_2 + 1));
	  String filedir = fullpath.substring(0, pos_2 + 1);
	  // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-318 end
	  String filename = fullpath.substring(pos_2 + 1);
	  Log.i(TAG,"fullpath = "+fullpath+" filedir = "+filedir+" filename = "+filename);
      File toDir = new File(filedir);
	  if(!toDir.exists()) {
	     //mkdir if the directory does not existe
	     toDir.mkdirs();
	  }
	  // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-318 begin
	  // File toFile = new File(convertToInternalPath(fullpath));
	  File toFile = new File(fullpath);
	  // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-318 end
	  String file_sd = fromDir +"/"+filename;
	  File fromFile = new File(file_sd);
	  if(fromFile.exists()) {
	     //copy fromFile to toDir
	     //if the file exists, overwritten it.
             Log.d(TAG," fromFile = "+fromFile+" toFile = "+toFile);
	     if(toFile.exists()) {
	        //rename the new file
	        Log.d(TAG,"toFile is existed, so create a new name for this file");
	        int pos_3 = fullpath.lastIndexOf(".");
	        String ext = fullpath.substring(pos_3);
	        toFile = new File(createNewFileName(filedir, ext));
	        Log.d(TAG, "new created file name = "+toFile+ "full path name ="+(filedir+toFile));
             }
			 
             if (copyfile(fromFile, toFile)) {
              	//update the item in DB too
     	     	eventValues.put(QNColumn.URI.column(), "file://"+toFile.toString());
                Log.d(TAG,"file copied successfully");
             } else {
                 Log.d(TAG,"Error found! copy failed. Return;");
                 throw new VNoteException("vNote with copying file failed in restoreQuickNote");
             }
	  } else {
		  throw new VNoteException("vNote with copying file failed in restoreQuickNote");
	  }
		      	
      //insert the item to quicknote DB
      event_uri = resolver.insert(CONTENT_URI, eventValues);
           
	  //set the reminder
	  if(setReminder && (null != event_uri)) {
	     Log.i(TAG,"restore to set reminder");
	     //need set the reminder, send a intent to quicknote to set a reminder
             Intent intentReminder = new Intent("com.motorola.quicknote.action.QN_RESTORE_COMPLETE");
	     intentReminder.putExtra("Uri",event_uri.toString());
             intentReminder.putExtra("reminder", reminder);
             context.sendBroadcast(intentReminder);
	  }
			
      } //end for
      Log.d(TAG, "end restore event");
      return subject;
    }


   public boolean copyfile(File fromFile, File toFile) {
   	Log.i(TAG,"copyfile");
        boolean copied = false;
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
           from = new FileInputStream(fromFile);
           to = new FileOutputStream(toFile);
           byte[] buffer = new byte[4096];
           int bytesRead;

          while ((bytesRead = from.read(buffer)) != -1) {
              to.write(buffer, 0, bytesRead); // write
          }
          copied = true;
       } catch (Exception e){
           Log.d(TAG,"copyfile: copy file Exception!!! e = "+e);
       } finally {
           if (from != null) {
              try {
                 from.close();
              } catch (IOException e) {
                 Log.d(TAG, "copyfile: 1- copy file Exception!!! e = "+e);
              }
           }
          if (to != null) {
              try {
                 to.close();
              } catch (IOException e) {
                 Log.d(TAG, "copyfile: 2- copy file Exception!!! e = "+e);
              }
           }

       return copied;
    }
 }


    /**
     * generate file name for attachement.
     * @return new file name with format "/sdcard/quicknote/image/2010-03-19_16-43-41_961.jpg"
     * "/sdcard/quicknote/text/2010-03-19_16-43-41_961.txt" or 
     *"/sdcard/voice/2010-03-19_16-43-41_961.amr"
     */
    private String createNewFileName(String dir, String ext) {
        StringBuffer newFileName = new StringBuffer();
        long currDate = System.currentTimeMillis();
        String timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
        
        // newFileName.append("file://");
        newFileName.append(dir);
        newFileName.append(timeString);
        newFileName.append(ext);
        
        File newFile = new File(newFileName.toString());

        while (newFile.exists()) {
            newFileName.delete(0, newFileName.length() - 1);
            currDate = System.currentTimeMillis();
            timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
//            newFileName.append("file://");
            newFileName.append(dir);
            newFileName.append(timeString);
            newFileName.append(ext);
            newFile = new File(newFileName.toString());
        }
        return newFileName.toString();
    }


   //change the string from file:///mnt/sdcard/sketch/filename.png to /mnt/sdcard/sketch/filename.png
   private String transFilePath(String oldFile) {
       String newFile = null;
       if (null != oldFile) {
          newFile = oldFile.substring(7);
       }
       Log.d(TAG,"translate oldFile = "+oldFile+" to newFile = "+newFile);
       return newFile;
   }
   // modified by amt_jiayuanyuan 2012-12-29 SWITCHUITWO-596 begin
	private String transFilePathForJB(String oldFile) {
		String newFile = null;
		if (null != oldFile) {
			int index = oldFile.indexOf("/", 5);
			newFile = oldFile.substring(index);
			if (SdcardManager.isSdcardMounted(mContext, true)) {
				newFile = "/storage/sdcard0" + newFile;
			} else {
				newFile = "/storage/sdcard1" + newFile;
			}
		}
		Log.d(TAG, "translate oldFile = " + oldFile + " to newFile = "
				+ newFile);
		return newFile;
	}
	// modified by amt_jiayuanyuan 2012-12-29 SWITCHUITWO-596 end
   
   public String convertToInternalPath(String originalPath) {
	   String external_storage_path = getExternalStoragePath(mContext);
	   if (originalPath.contains(external_storage_path)) {
		   return originalPath;
	   } else {
		   if (originalPath.contains(mConstants.SDCARD_PREFIX)) {
			   return originalPath.replace(mConstants.SDCARD_PREFIX, external_storage_path);
		   } else if (originalPath.contains(mConstants.ALT_SDCARD_PREFIX)) {
			   return originalPath.replace(mConstants.ALT_SDCARD_PREFIX, external_storage_path);
		   } else {
			   Log.e(TAG, "invalid path");
			   return null;
		   }
	   }
   }
   
	public String getExternalStoragePath(Context context){
		StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
		if (sm == null) {
			Log.e(TAG, "storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
			return Environment.getExternalStorageDirectory().toString();
		}
		StorageVolume[] volumes = sm.getVolumeList();
		for (StorageVolume sv : volumes) {
			// only monitor non emulated and non usb disk storage
			if (sv.isEmulated()) {
				String path = sv.getPath();
				return path;
			}
		}
		// error case
		Log.e(TAG, "storage error -2: Fail to get internal memory path! use default: " + Environment.getExternalStorageDirectory().toString());
		return Environment.getExternalStorageDirectory().toString();
	}
}
