/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * w21782                    01/12/2010                  added 'null' check routine after get cursor.
 * w21782                    01/12/2010                  added API isMediaFileExist().
 * w21782                    01/08/2010                  added API setMediaUri().
 * w21782                    01/06/2010                  added API addQuickNote().
 * w21782                    12/28/2009                  added API getQuickNoteUri() and setWidgetID().
 * w21782                    12/02/2009                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * BIG RULE: [ Policy is changed!!! ] QuickNoteDB's basic operation -
 * Read/Write/Delete doesn't handle anything about MediaStore!!!!
 * 
 * This class provides common APIs to access Quick Notes DB.
 * 
 */
public class QuickNotesDB {

	private static final String TAG = "QuickNotesDB";

	public static final String AUTHORITY = "com.motorola.provider.quicknote";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/qnotes");

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.quicknote";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.quicknote";

    
    public static enum QNColumn {
        _ID             (BaseColumns._ID,   "INTEGER PRIMARY KEY",  null),
        TITLE           ("title",           "TEXT",                 ""),
        MIMETYPE        ("mime_type",       "TEXT",                 ""),
        URI             ("uri",             "TEXT",                 ""),    // raw data URI.("file or qntext")
        THUMBURI        ("thumb_uri",       "TEXT",                 ""),    // no use now;store the thumb midia uri, used for mini thumbnails
        ORIGID          ("orig_id",         "LONG",                 null),    // no use now;store the media id, used for micro thumbnails
        BGCOLOR         ("bgcolor",         "INTEGER",              Color.TRANSPARENT),
        REMINDER        ("reminder",        "LONG",                 0),   //store the string of calendar
        WIDGETID        ("widget_id",       "INTEGER",              AppWidgetManager.INVALID_APPWIDGET_ID),
        SPANX           ("span_x",          "INTEGER",              2),
        SPANY           ("span_y",          "INTEGER",              2),
        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
        CALLNUMBER      ("call_number",     "TEXT",                 ""),
        CALLDATEBEGIN   ("call_date_begin", "LONG",                 0),
        CALLDATEEND     ("call_date_end",   "LONG",                 0),
        CALLTYPE        ("call_type",       "INTEGER",              null),
        SUMMARY         ("summary",         "TEXT",                 "");
        // END IKCNDEVICS-504
        
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

    private static String textLoadCondition = null;
    private static String getTextLoadCondtion(Context context) {
        if(textLoadCondition == null) {
            if(QNUtil.isTextLoad(context)) {
                textLoadCondition = new String(QNColumn.MIMETYPE.column() + " like 'text%' ");
            }  else {
                textLoadCondition = new String("1=1");
            }
        }
        return textLoadCondition;
    }

    /**
     * 
     * @param context
     * @param qnUri
     * @return : null if 'qnUri' has invalid media uri.
     */
    private static Uri _mediaStore_uri(Context context, Uri filePathUri) {
        Uri mediaStoreUri = null;
//        String filePath = (String) read(context, qnUri, QNColumn.URI);
//        if (filePath != null && filePath.length() > 0) {
//            Uri uri = QNUtil.buildUri(filePath);
            if(null != filePathUri && filePathUri.getScheme().equals("file")) {
                mediaStoreUri = QNUtil.mediaStore_uri(context, filePathUri.getPath());
            }
//        }
        return mediaStoreUri;
    }


    public static int countInQuickNoteDB(Context context, Uri filePathUri) {
         QNDev.log(TAG+"countInQuickNoteDB: filePathUri = "+filePathUri);
         int count = 0;

         if (null == context || null == filePathUri ) { return 0;}


        // count in QuickNote database
        Cursor qnCursor = getQuickNoteDBCursor(context);  
        if(null == qnCursor) { return 0; } // in case of there is no QuickNotes that has given widget ID
        
        Uri uriFilePath;
        if(qnCursor.moveToFirst()) {
          do {
             uriFilePath = Uri.parse(qnCursor.getString(qnCursor.getColumnIndex(QNColumn.URI.column()))); 
           //  QNDev.log(TAG+" get uriFilePath = "+uriFilePath+" == "+filePathUri+" ?");
             if (filePathUri.compareTo(uriFilePath) == 0) {
          //       QNDev.log(TAG+"find a matched record!");
                 count = count +1;
             }
             qnCursor.moveToNext();
          } while (!qnCursor.isAfterLast());
        }
        qnCursor.close();
        QNDev.log(TAG+"matched records: = "+count); 
        return count;
    }

    

    private static boolean _delete_from_MediaStore(Context context, Uri qnUri, String mimeType, Uri filePathUri) {
        boolean ret = false;
        //only delete it if it is the last note which is referenced this media resource
        if (countInQuickNoteDB(context, filePathUri) > 1) {
           return false;
        }
        Uri mediastore_uri = _mediaStore_uri(context, filePathUri);
        QNDev.log("Delete from MediaStore : " + ((null == mediastore_uri)? "null": mediastore_uri.toString()) );
        if(null != mediastore_uri) { 
            ret = QNUtil.delete_from_MediaStore(context, mediastore_uri);
            QNDev.qnAssert(true == ret);
            
            /* Delete the voice file from SD card
               this is a workaround solution
               for the voice note, we delete the solid file from the SD card again
               because after above steps, it only delete the record from the audio_meta table
           */
//           String mimeType = (String)read(context, qnUri, QNColumn.MIMETYPE);
           if  (mimeType.startsWith("audio/")) {          
//               Uri uri = QNUtil.buildUri((String)read(context, qnUri, QNColumn.URI));
               if (filePathUri != null ) {
                  String filePath =  filePathUri.getPath();
                  QNDev.log(TAG+"Voice Note! deleting "+filePath+" from SD card !"); 
                  File file = new File(filePath);
                  try  {  // File.delete can throw a security exception
                    if ( !file.delete()) {
                       QNDev.log(TAG+" failed to delete "+filePath);
                    }
                  } catch (Exception e) {
                      QNDev.log(TAG+"Exception: failed to delete "+filePath+" e:"+e);
                 }
              }
          }  //end of audio delete
           /*2012-12-14, add by amt_sunzhao for SWITCHUITWO-303 */
        } else {
        	if(null != filePathUri) {
        		final String path = filePathUri.getPath();
        		final File file = new File(path);
        		if(null != file) {
        			ret = file.delete();
        		}
        	}
        }
        /*2012-12-14, add end*/
        return ret;
    }
        
    
    public static boolean is_qnuri_type(Uri uri) {
        QNDev.qnAssert(null != uri);
        if (uri != null) {
            return uri.toString().startsWith(CONTENT_URI.toString());
        }
        return false;
    }
    
    public static boolean isValidUri(Context context, Uri uri) {
        if (uri == null) return false;
        try {
            QNDev.logd(TAG, "query with the provided uri");
            Cursor qncursor = context.getContentResolver().query(uri, null, getTextLoadCondtion(context), null, null);
            if (( null == qncursor ) || !qncursor.moveToFirst()) {
                return false;
            } else {
                qncursor.close();
                return true;
            }
        } catch (NullPointerException e) {
            //This is a Workround Solution.
            //Sometimes when delete multiple quicknotes, a NullPointerException is thrown out.
            //The root cause seems like that the quicknotes are being deleted from DB while
            //the widget queries it. So it is a synchronization problem and the uri passed in
            //should be invalid.
            QNDev.logd(TAG, "invalid uri : " + e.toString());
            return false;
        }
    }
    
    public static boolean delete(Context context, Uri qnUri) {
        QNDev.qnAssert(null != context && null != qnUri && is_qnuri_type(qnUri));
        if (null == context || null == qnUri ) { return false;}


        // Delete from QuickNote database
        Cursor cursor = context.getContentResolver().query(qnUri, null, getTextLoadCondtion(context), null, null);
        QNDev.qnAssert(null != cursor);
        if (null == cursor) { return false;}

        int count = 0;
        if(cursor.moveToFirst()){
            String mimeType = cursor.getString(cursor.getColumnIndex(QNColumn.MIMETYPE.column()));
            Uri uriFilePath = Uri.parse(cursor.getString(cursor.getColumnIndex(QNColumn.URI.column())));

            //check the reminder
            long reminder = cursor.getLong(cursor.getColumnIndex(QNColumn.REMINDER.column()));
            if (0L != reminder ) {
               //cancel the reminder alarm
               QNDev.log(TAG+"delete note: have reminder, try to cancal the alarm...");
               QNUtil.setAlarm(context, qnUri, 0L);
            }
            //cancel the reminder in status bar if have
            long noteId = ContentUris.parseId(qnUri);
            QNDev.log(TAG+"cancel reminder status: noteid = "+noteId);
            String sPosition = Long.toString(noteId);
            int position = Integer.parseInt(sPosition);
            NotificationManager nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE); 
            nm.cancel(position);   

            if (QNDev.STORE_TEXT_NOTE_ON_SDCARD && mimeType != null && mimeType.startsWith("text/")) {
            	if(QNUtil.is_storage_mounted(context)){
            		 // delete text file
                    if (countInQuickNoteDB(context, uriFilePath) <= 1) {
                       if (uriFilePath.getScheme().startsWith("file")) {
                    	   
                    	   try {
                               String filePathStr = null;
                               if(uriFilePath != null && (filePathStr = uriFilePath.getPath()) != null) {
                    		   File newFile = new File(filePathStr);
                                   if(newFile != null) newFile.delete();
                               }
                           } catch (NullPointerException e) {
                               //This is a Workround Solution.
                               //Sometimes when delete multiple quicknotes, a NullPointerException is thrown out.
                               //The root cause seems like that the quicknotes are being deleted from DB while
                               //the widget queries it. So it is a synchronization problem and the uri passed in
                               //should be invalid.
                               QNDev.logd(TAG, "NullPointerException ");
                           }
                    	   
                    	     
                       }

                       try {
                           //scan the media
                           QNUtil.mediaScanFolderOrFile(context, new File(QNConstants.TEXT_DIRECTORY), true);
                       } catch (NullPointerException e) {
                           //This is a Workround Solution.
			   //Sometimes when delete multiple quicknotes, a NullPointerException is thrown out.
			   //The root cause seems like that the quicknotes are being deleted from DB while
			   //the widget queries it. So it is a synchronization problem and the uri passed in
			   //should be invalid.
			   QNDev.logd(TAG, "when invoke mediaScanFolderOrFile, NullPointerException ");
                       }

                    }
            	}               
            } else {
                if (mimeType != null && !mimeType.startsWith("text/")) {
                    // At first, delete from Media store!!
                    _delete_from_MediaStore(context, qnUri, mimeType, uriFilePath);
                }
            }
            // delete DB record
            count = context.getContentResolver().delete(qnUri, null, null);
        } else {
            // do nothing 
            //QNDev.qnAssert(false); 
            return false;
        }
        cursor.close();
        
        return (1 == count)? true: false;
        
    }
    

    /**
     * Create new empty row
     * @param context
     * @return
     */
    public static Uri insert(Context context) {
        QNDev.qnAssert(null != context);
        if (null == context) { return null;}
        return context.getContentResolver().insert(CONTENT_URI, null);
    }

    public static Uri insert(Context context, ContentValues values, boolean contentUpdated) {
        QNDev.qnAssert(null != context && null != values);
        if (null== context || null == values) { return null;}

        Uri returnUri = null;
        Uri mediaUri = null;
        String mimeType = values.getAsString(QNColumn.MIMETYPE.column());
        Uri contentUri = Uri.parse(values.getAsString(QNColumn.URI.column()));

        if(contentUri.getScheme().equals("file") && mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("text/") ) && contentUpdated) {
//            _delete_from_MediaStore(context, qnUri);

           if (mimeType.startsWith("image/")) {
              // register to media store!!
              mediaUri = QNUtil.register_to_MediaStore(context, new File(contentUri.getPath()),
                                          mimeType, 
                                          new File(QNConstants.TEMP_DIRECTORY));
           } else {
              //it is text
              //scan the media
              QNUtil.mediaScanFolderOrFile(context, new File(contentUri.getPath()), false);

           }
        }

        returnUri = context.getContentResolver().insert(CONTENT_URI, values);

        return returnUri;
    }
    
    public static Object read(Context context, Uri qnUri, QNColumn column) {
        QNDev.qnAssert(null != context && null != qnUri && is_qnuri_type(qnUri));
        Object reto = null;
        Cursor qncursor = null;
        try {
            qncursor = context.getContentResolver().query(qnUri, null, getTextLoadCondtion(context), null, null);
            QNDev.qnAssert(null != qncursor);
            if(qncursor.moveToFirst()){
                if(column.type().equals("TEXT")) {
                    reto = qncursor.getString(qncursor.getColumnIndex(column.column()));
                } else if(column.type().equals("INTEGER")) {
                    reto = qncursor.getInt(qncursor.getColumnIndex(column.column()));
                } else if (column.type().equals("LONG")) {
                    reto = qncursor.getLong(qncursor.getColumnIndex(column.column()));
                } 
            }
        } catch (Exception e) {
            QNDev.log(TAG+ "Read DB error: " + e.toString());
        } finally {
            if (qncursor != null) {
                qncursor.close();
            }
            return reto;
        }
    }


   //return a note content with the  position provided
   public static Uri getItem(Context context, int position) {
        Cursor qncursor = context.getContentResolver().query(CONTENT_URI, null, getTextLoadCondtion(context), null, QNColumn._ID + " DESC");
        QNDev.qnAssert(null != qncursor);
        if (null == qncursor) { return null;}

        Uri uri = null;
        if (qncursor.moveToPosition(position)) { 
            uri = ContentUris.withAppendedId(CONTENT_URI, qncursor.getLong(qncursor.getColumnIndex(QNColumn._ID.column())) );
        }
        qncursor.close();
        return uri;   
   }

   //return the cursor for the QN DB
   public static Cursor getQuickNoteDBCursor(Context context) {
        Cursor qnCursor = null;
        qnCursor = context.getContentResolver().query(CONTENT_URI, null, getTextLoadCondtion(context), null, QNColumn._ID + " DESC");
        QNDev.qnAssert(null != qnCursor);
        return qnCursor;
   }

   //return the total number of existing notes
   public static int getSize(Context context) {
        Cursor qncursor = context.getContentResolver().query(CONTENT_URI, null, getTextLoadCondtion(context), null, null);
        QNDev.qnAssert(null != qncursor);
        if (null == qncursor )  { return -1; }
        int dbSize = qncursor.getCount();
        qncursor.close();
        return dbSize;
   }
   //return the index of the quicknote with the provided URI
   public static int getIndexByUri(Context context, Uri noteUri) {
       long noteId = ContentUris.parseId(noteUri);
       QNDev.logd(TAG, "[getIndexByUri] parse noteId from noteUri, id is " + noteId);
       int index = -1;
       Cursor qncursor = context.getContentResolver().query(CONTENT_URI, null, getTextLoadCondtion(context), null, QNColumn._ID + " DESC");
       QNDev.qnAssert(null != qncursor);
       if (null == qncursor) { return -1; }

       if (qncursor.moveToFirst()) {
           do {
               long id = qncursor.getLong(qncursor.getColumnIndexOrThrow(QNColumn._ID.column()));
               QNDev.logd(TAG, "[getIndexByUri] get noteId from qncursor, id is " + id);
               if (id == noteId) {
                   index = qncursor.getPosition();
                   QNDev.logd(TAG, "[getIndexByUri] the note index is " + index);
                   break;          
               }
           } while (qncursor.moveToNext());
       }      
       qncursor.close();
       return index;
   }
    /**
     * There is one exceptional case. If you want write URI than you should write MIMETYPE first!!.
     * (That is, you can write URI after writing MIMETYPE)
     * @param context
     * @param qnUri
     * @param column
     * @param value
     * @return
     */
    public static boolean write(Context context, Uri qnUri, QNColumn column, Object value) {
        QNDev.qnAssert(null != context && null != qnUri && is_qnuri_type(qnUri));
        if (null == context || null == qnUri ) { return false;}
        int count = 0;
        
        ContentValues v = new ContentValues();

        if(QNColumn.URI == column) {
            // Mime type should be set before writing URI!!!
            String mimeType = (String)read(context, qnUri, QNColumn.MIMETYPE);
            if (mimeType == null || mimeType.length() <= 0) return false;
            // This should be special case..
            // Due to updating MediaStore...
            QNDev.qnAssert(value instanceof String);
            Uri new_uri = Uri.parse((String)value);
            if(new_uri.getScheme().equals("file")) {
                QNDev.qnAssert( ((String)read(context, qnUri, QNColumn.MIMETYPE)).startsWith("image/") 
                        || ((String)read(context, qnUri, QNColumn.MIMETYPE)).startsWith("audio/") );
                  Uri current_uri = QNUtil.buildUri((String)read(context, qnUri, QNColumn.URI));
                  
                  // type of new one and old one should be same!
                  QNDev.qnAssert( null == current_uri 
                               || current_uri.getScheme().equals("file"));
                  // Update should be done only when two raw data are different.
                  if( null == current_uri 
                      || !current_uri.equals(new_uri)) {
                      // NOTE :
                      //  Because MediaStore doesn't support 'Update', We should follow "Delete -> Add".
                      //  It's a kind of walk-around!
                      
                      // NOTE:
                      // According out current SW Design, file scheme  uri should be applied to media-type-note!
                      // This should have media content!!!!
                      
                      // Is this have valild media path?
                      _delete_from_MediaStore(context, qnUri, mimeType, current_uri);
                     
                      // register to media store!!
                      QNUtil.initDirs(context);
                      QNUtil.register_to_MediaStore(context, new File(new_uri.getPath()), 
                                                      (String)read(context, qnUri, QNColumn.MIMETYPE), 
                                                      new File(QNConstants.TEMP_DIRECTORY));
                  }                
            }
        }

        if(column.type().equals("TEXT")) {
            v.put(column.column(), (String)value);
            count = context.getContentResolver().update(qnUri, v, null, null);
        } else if (column.type().equals("INTEGER")) {
            v.put(column.column(), (Integer)value);
            count = context.getContentResolver().update(qnUri, v, null, null);
        } else if (column.type().equals("LONG")) {
            v.put(column.column(), (Long)value);
            count = context.getContentResolver().update(qnUri, v, null, null);
        } else { 
            QNDev.qnAssert(false); // unexpected
        }
        QNDev.qnAssert(1 == count);

        return true;
    }

    /**
     * Get QuickNote DB Uri from it's column.
     * @param context
     * @param widget_id
     * @return
     */
    public static Uri qnUri(Context context, QNColumn column, String value) {
        Cursor qncursor = context.getContentResolver().query(CONTENT_URI, null, 
                                                            column.column() + "=" + value + " and " + getTextLoadCondtion(context),
                                                             null, null);
        if(null == qncursor) { return null; } // in case of there is no QuickNotes that has given widget ID
        Uri qnUri = null;
        if(qncursor.moveToFirst()) {
            QNDev.qnAssert(qncursor.isLast());
            qnUri = ContentUris.withAppendedId(CONTENT_URI, qncursor.getLong(qncursor.getColumnIndex(QNColumn._ID.column())) );
            QNDev.qnAssert(null != qnUri);
        }
        qncursor.close();
        return qnUri;
    }
    /**
     * Copy values of given uri row. ('_ID' column will be excluded from copying)
     * @param context
     * @param qnUri
     * @param columns_excluded : column name that should be excluded in copying
     * @return null (fail).
     */
    public static ContentValues copy_values(Context context, Uri qnUri, String[] columns_excluded) {
        QNDev.qnAssert(null != context && null != qnUri && is_qnuri_type(qnUri));
        if (null == context || qnUri == null ) { return null;}
        
        ContentValues v = null;
        Cursor qncursor = context.getContentResolver().query(qnUri, null, getTextLoadCondtion(context), null, null);
        if (qncursor == null) { return null;}

        QNDev.qnAssert(null != qncursor);

        if(!qncursor.moveToFirst()) { return null; }
        QNDev.qnAssert(qncursor.isLast());
        v = new ContentValues();
        boolean bskip;
        for(QNColumn c : QNColumn.values()) {
            bskip = false;
            if(null != columns_excluded) {
                for(int i = 0; i < columns_excluded.length; i++) {
                    if(columns_excluded[i].equals(c.column())) {
                        bskip = true;
                        break;
                    }
                }
            }
            if(!bskip) {
                if(c.type().equals("TEXT")) {
                    v.put(c.column(), qncursor.getString(qncursor.getColumnIndex(c.column())));
                } else if(c.type().equals("INTEGER")) {
                    v.put(c.column(), qncursor.getInt(qncursor.getColumnIndex(c.column())));
                } else if (c.type().equals("LONG")) {
                    v.put(c.column(), qncursor.getLong(qncursor.getColumnIndex(c.column())));
                } else {
                    QNDev.qnAssert(false); // unexpected
                }
            }
        }
        qncursor.close();
        return v;
    }
    
    public static int[] all_widget_ids(Context context) {
        Cursor c = context.getContentResolver().query(CONTENT_URI, 
                                                        new String[] {QNColumn.WIDGETID.column()}, 
                                                        getTextLoadCondtion(context), null, null);
        int[] ids = null;
        if(null != c) {
            if(c.getCount() > 0) {
                LinkedList<Integer> ll = new LinkedList<Integer>();
                int index = 0;
                int widgetid;
                c.moveToFirst();
                while(!c.isAfterLast()) {
                    widgetid = c.getInt(0);
                    if(AppWidgetManager.INVALID_APPWIDGET_ID != widgetid) {
                        ll.addLast(widgetid);
                    }
                    c.moveToNext(); index++;
                }
                ids = new int [ll.size()];
                index = 0;
                Iterator<Integer> itr = ll.listIterator();
                while(itr.hasNext()) {
                    ids[index++] = itr.next();
                }
            }
            c.close();
        }
        return ids;
    }
}
