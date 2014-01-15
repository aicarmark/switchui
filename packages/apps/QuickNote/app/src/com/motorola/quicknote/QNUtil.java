/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Utility functions for Quick Note
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created.
 * 
 * 
 *****************************************************************************************/

/**
 * Utilites for QuickNote!!!
 */

package com.motorola.quicknote;

import java.lang.Math;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.text.format.DateFormat;
import android.view.WindowManager;
import android.widget.Toast;
import android.util.Log;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.StringBuffer;
import java.nio.charset.Charset;

import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.QNConstants;

public class QNUtil {

    private static boolean mLibLoaded = false;

    private static void loadLibrary(Context context) {
        if(!mLibLoaded && !isTextLoad(context)) {
        	/*2012-12-18, add by amt_sunzhao for SWITCHUITWO-337 */
            //System.loadLibrary("qnutil");
        	/*2012-12-18, add end*/
            mLibLoaded = true;
        }
    }

    /**************************
     * Local Functions
     **************************/
    
    /**************************
     * APIs.
     **************************/
    /*  Try to use String.format() as little as possible, because it creates a
     *  new Formatter every time you call it, which is very inefficient.
     *  Reusing an existing Formatter more than tripled the speed of
     *  makeTimeString().
     *  This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
     */
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(
                secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);
        
        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }
    
    /**
     * 
     * @param context
     * @return True if current orientation is landscape..
     */
    public static boolean is_landscape(Context context) {
        return (Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation);
    }
    
    /**
     * return screen width (in pixel)
     * @param act
     * @return
     */
    public static int screen_width(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }
    
    /**
     * See Screen_width()
     * @param context
     * @return
     */
    public static int screen_height(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }
    
    /**
     * Check that this uri indicates raw data or database data.
     * @return : true (it's DB uri : scheme is "content")
     */
    public static boolean is_DBUri(Uri uri) {
        QNDev.qnAssert(null != uri);
        if (null == uri) { return false; }

        return uri.getScheme().equals("content");
    }

    /**
     * Get size(width, height) of given image. 
     * @param imagefile : image file path
     * @param out : out[0] : width of image / out[1] : height of image
     * @return
     *   false if image cannot be decode.
     *   true if success
     */
    // out[0] : width / out[1] : height
    public static boolean  image_size(final String imagefile, int[] out) {
        QNDev.qnAssert(null != imagefile);
        if (null == imagefile) { return false; }

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagefile, opt);
        if(opt.outWidth <= 0 || opt.outHeight <= 0 || null == opt.outMimeType) {
            return false;
        }
        out[0] = opt.outWidth;
        out[1] = opt.outHeight;
        return true;
    }    
    
    public static int px_from_dp(Context context, int dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);        
    }
    
    /**
     * Get dp value based on given cell number..
     * usually 1<= sell <=4
     * @param cell : number of cell occupied in home.
     * @return
     */
    public static int home_cell_dp(int cell) {
        return cell*74-2; // from android document..
    }

    /**
     * Calculate rectangle(out[]). This is got by shrinking rectangle(width,height) to bound rectangle(boundW, boundH) with fixed ratio.
     * If input rectangle is included in bound, then input rectangle itself will be returned.(we don't need to adjust)
     * @param boundW : width of bound rect
     * @param boundH : height of bound rect
     * @param width : width of rect to be shrunk
     * @param height : height of rect to be shrunk
     * @param out : calculated value [ out[0](width) out[1](height) ]
     * @return : false(not shrunk) / true(shrunk)
     */
    public static float  shrink_fixed_ratio(int boundW, int boundH, int width, int height, int[] out) {
        // Check size of picture..
        float rw = (float)width / (float)boundW , // width ratio
              rh = (float)height / (float)boundH ; // height ratio

        
        // check whether shrinking is needed or not.
        if(rw <= 1.0f && rh <= 1.0f) {
            // we don't need to shrink
            out[0] = width;
            out[1] = height;
            return (float)1;
        } else {
            // shrinking is essential.
            float ratio = (rw < rh)? rh: rw; // choose maxmum
            // integer-type-casting(rounding down) guarantees that value cannot be greater than bound!!
            out[0] = (int) ( (float)width / ratio);
            out[1] = (int) ( (float)height / ratio );
            return ratio;
        }
    }

    /**
     * Check whether given file path is absolute path or not.
     * @param filepath
     * @return
     */
    public static boolean is_absolute_path(String filepath) {
        QNDev.qnAssert(null != filepath);
        if (null == filepath) { return false;}
        return filepath.charAt(0) == '/';
    }


    /**
     * Make fixed-ration-bounded-bitmap with file.
     * if (0 >= boundW || 0 >= boundH), original-size-bitmap is trying to be created.
     * @param fpath : image file path (absolute path)
     * @param boundW : bound width
     * @param boundH : bound height
     * @return : null (fail)
     */
    public static Bitmap decode_image_file_for_sketch(String fpath, int boundW, int boundH) {
        QNDev.qnAssert(null != fpath && is_absolute_path(fpath));
        if (null == fpath) { return null; }

        try {
            BitmapFactory.Options opt = null;
            if(0 < boundW && 0 < boundH) { 
                int[] imgsz = new int[2]; // image size : [0]=width / [1] = height
                if(false == QNUtil.image_size(fpath, imgsz) ) { return null; }
                
                int[] bsz = new int[2]; // adjusted bitmap size
                boolean bShrink = QNUtil.shrink_fixed_ratio(boundW, boundH, imgsz[0], imgsz[1], bsz) > 1 ? true : false;            
                
                opt = new BitmapFactory.Options();
                opt.inDither = false;
                opt.inMutable = true;
                if(bShrink) {
                    // To save memory we need to control sampling rate. (based on width!)
                    // for performance reason, we use power of 2.
                    QNDev.qnAssert(0 < bsz[0]);
                    int sample_size = 1;
                    while( 1 < imgsz[0]/(bsz[0]*sample_size) ) {
                        sample_size*=2;
                    }
                    
                    // shrinking based on width ratio!!
                    // NOTE : width-based-shrinking may make 1-pixel error in height side!
                    // (This is not Math!! And we are using integer!!! we cannot make it exactly!!!)
                    // So, View size of this Board should be set as bitmap size, after real bitmap is created.
                    opt.inScaled = true;
                    opt.inSampleSize = sample_size;
                    opt.inDensity = imgsz[0]/sample_size;
                    opt.inTargetDensity = bsz[0];
                }
            }
            Bitmap new_bm = BitmapFactory.decodeFile(fpath, opt);
            QNDev.logd("QNUtil", "QNUtil.decode_image_file_for_sketch().decodeFile, memory usage:");
            QNDev.logd("QNUtil", "new_bm=" + new_bm.getByteCount() + " bytes" + " width=" + 
                             new_bm.getWidth() + " height=" + new_bm.getHeight());
            return new_bm;
        } catch (Exception e) {
            return null;
        } catch (java.lang.OutOfMemoryError me) {
            return null;
        }
    }
    

    /**
     * Make fixed-ration-bounded-bitmap with file.
     * if (0 >= boundW || 0 >= boundH), original-size-bitmap is trying to be created.
     * @param fpath : image file path (absolute path)
     * @param boundW : bound width
     * @param boundH : bound height
     * @return : null (fail)
     */
    public static Bitmap decode_image_file(String fpath, int boundW, int boundH, boolean autoScaled) {
        boolean needResize = false;
        int ideaWidth = 0;
        int ideaHeight = 0;

        QNDev.qnAssert(null != fpath && is_absolute_path(fpath));
        if (null == fpath) { return null; }

        try {
            BitmapFactory.Options opt = null;
            if(0 < boundW && 0 < boundH) { 
                int[] imgsz = new int[2]; // image size : [0]=width / [1] = height
                if(false == QNUtil.image_size(fpath, imgsz) ) { return null; }
                
                int[] bsz = new int[2]; // adjusted bitmap size
                float sample_size_float = QNUtil.shrink_fixed_ratio(boundW, boundH, imgsz[0], imgsz[1], bsz);
                ideaWidth = (int)((float)imgsz[0]/sample_size_float);
                ideaHeight = (int)((float)imgsz[1]/sample_size_float);

                opt = new BitmapFactory.Options();
                opt.inDither = false;
                opt.inMutable = true;
                if(sample_size_float >= 2) {
                    // To save memory we need to control sampling rate. (based on width!)
                    //Returns the double conversion of the most negative (closest to negative infinity) integer value which is greater than the argument.
                    int sample_size = (int)Math.ceil((double)sample_size_float);
                    //sample_size should be Power of 2
                    sample_size = powerOf2(sample_size);
                    opt.inSampleSize = sample_size;
                    int resizedWidth =  imgsz[0]/sample_size;
 
                    opt.inScaled = false;
                    if (resizedWidth > bsz[0]) {
                      /*
                        1. output image is bigger then the screen width, so need to resize it to tartget Density
                        2.if it is for imageView, we suggest not to scale the image by this way,
                          otherwise ImageView may scaled the image again because of the difference between your image's DPI and screen DPI.
                          it may result in the image in imageView is smaller or bigger then you expected
                      */
                      if (autoScaled) {
                        opt.inDensity = resizedWidth;
                        opt.inTargetDensity = bsz[0];
                        opt.inScaled = true;
                      } else {
                        needResize = true;
                      }
                    }
                    opt.inPurgeable=true; //if necessary purge pixels into disk
                    opt.inMutable=true;
                } else if ((sample_size_float > 1) && (sample_size_float < 2)) {
                    needResize = true;
                }
            }
            Bitmap new_bm = BitmapFactory.decodeFile(fpath, opt);
            if (needResize && (ideaWidth > 0) && (ideaHeight > 0)) {
               new_bm = Bitmap.createScaledBitmap (new_bm, ideaWidth, ideaHeight, false);
            }
            return new_bm;
        } catch (Exception e) {
            QNDev.logd("QNUtil", "decode_image_file() caught exception." + e.toString());
            return null;
        } catch (java.lang.OutOfMemoryError me) {
            QNDev.logd("QNUtil", "decode_image_file() caught OutOfMemoryError." + me.toString());
            return null;
        }
    }


    public static int powerOf2( int num) {
        if((num&(num-1)) == 0) {
           //it is power of 2
           return num;
        } else {
          do {
             num = num - 1;
          } while ((num&(num-1)) != 0);
          return num;
        }
    }

    public static Uri buildUri(String uristring) {
        //QNDev.qnAssert(null != uristring);
        if(null != uristring && uristring.length() > 0) {
            return Uri.parse(uristring);
        } else {
            return null;
        }
    }
    
    /** 
     * Get database uri
     * @param context
     * @param bucket_uri
     * @param abs_path
     * @return
     */
    private static Uri databaseUri(Context context, Uri bucket_uri, String abs_path) {
        Uri dburi = null;
        String[] columns = new String[]{BaseColumns._ID, "_data"};
        Cursor c = context.getContentResolver().query(bucket_uri, columns, "_data = '" + abs_path + "'", null, null);
        if(null != c) {
//            QNDev.qnAssert(null != c);
            if(c.moveToFirst()){
                dburi = ContentUris.withAppendedId(bucket_uri, c.getLong(c.getColumnIndex(BaseColumns._ID)) );
            }
            c.close();
        }        
        return dburi;
    }
    /**
     * Get uri of MediaStore from file path.
     * @param context
     * @param abs_path
     * @return
     */
    public static Uri mediaStore_uri(Context context, String abs_path) {
        Uri muri = null;

        muri = databaseUri(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, abs_path);
        if(null != muri) { return muri; }
        muri = databaseUri(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, abs_path);
        if(null != muri) { return muri; }
        
        return muri;
        // Video is not supported yet.
    }
    /**
     * number of registered row in MediaStore that '_data' field is same with 'fpath' 
     * @param fpath : MUST BE ABSOLUTE path
     * @return : count (success) / -1 (fail or invalid parameter)
     */
    public static int count_in_MediaStore(Context context, File file, String mimetype) {
        QNDev.qnAssert(null != file && null != context && null != context.getContentResolver() );
        if ((null == file) || (null == context) || null == context.getContentResolver()) { return -1;}
        
        ContentResolver resolver = context.getContentResolver();
        String[] columns = new String[1];
        columns[0] = "_data";

        int count = -1;
        Cursor c = null;
        try {
            if(mimetype.startsWith("image/")) {
                c = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, "_data = '" + file.getAbsolutePath() + "'", null, null);
                if(null != c) {count = c.getCount(); }
            } else if (mimetype.startsWith("audio/")) {
                c = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, "_data = '" + file.getAbsolutePath() + "'", null, null);
                if(null != c) {count = c.getCount(); }
            } else {
                QNDev.qnAssert(false); // Other is not supported yet!! - ex video!
            }
        } catch (Exception e) {
            QNDev.logi("[Quicknote]", "Query error: " + e.toString());
        } finally {
            if(null != c) { c.close(); }
            return count;
        }
    }

    public static boolean delete_from_MediaStore(Context context, Uri media_store_uri) {
        QNDev.qnAssert(null != context && null != media_store_uri && media_store_uri.getScheme().equals("content"));
        if ( null == context || null == media_store_uri ) { return false; }

        boolean ret = false;
        Cursor cursor = context.getContentResolver().query(media_store_uri, null, null, null, null);
        if(null != cursor) {
            if(cursor.moveToFirst()){
                if(1 != context.getContentResolver().delete(media_store_uri, null, null) ) { QNDev.qnAssert(false); }
                ret = true;
            }
            cursor.close();
        }
        return ret;
    }
    
    /**
     * Register give file to MediaStore.
     * If this file is already registered, it will be tried to be updated. 
     * If not, it will be newly inserted!.
     * The reason why registering to image store is complex is due to "Thumbnail Image"!!!
     * @param context : context 
     * @param file : file to register. This should be a file that can be registered into MediaStore.
     * @param mimetype : mimetype of this file
     * @param tempdir : 
     *   Temporary directory this is used in this function. If this is null than default temp directory is used.
     * @return : Uri of MediaStore (newly registered one) : null (fail)
     */
    public static Uri register_to_MediaStore(Context context, File file, String mimetype, File tempdir) {
        if(null == file || null == context || null == context.getContentResolver() || !file.exists()) {
            return null;
        }
        
        ContentResolver resolver = context.getContentResolver();
        
        /**
         * If there is already row that indicates same file, try to delete it!!!
         * [NOTE] : 
         *   Using 'update' in resolver makes inconsistency with thumbnail and original image.
         *   (MediaStore doesn't support thumbnail update interface...)
         *   So, this way SHOULD BE taken (delete -> insert instead of update! )
         */
        ContentValues values = null;
        Uri content_uri = null;
        if(mimetype.startsWith("image/")) {
            content_uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            values = new ContentValues(4);
            values.put(MediaStore.Images.Media.TITLE, file.getName());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Images.Media.MIME_TYPE, mimetype);
            values.put("_data", file.getAbsolutePath());
        } else if(mimetype.startsWith("audio/")) {
            content_uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            values = new ContentValues(1);
            values.put("_data", file.getAbsolutePath());
        } else {
            QNDev.qnAssert(false); // not supported yet for others.
            values = new ContentValues(0);
        }
        
        Uri muri = null;
        boolean b_need_new_register = false;
        
        int count = count_in_MediaStore(context, file, mimetype);
        if(count < 0) { 
            return null; 
        } else if( 0 == count) {
            // this is new one!!
            b_need_new_register = true;
        } else if(0 < count && file.getAbsolutePath().startsWith(QNConstants.SKETCH_DIRECTORY)) {
            // this block code is to update sketch with same file name!  other media files do not need replace media DB.
            // This algorithm requires only one-existing-row for one file!
            QNDev.qnAssert(1 == count);
            
            { // Just scope
                Cursor c = resolver.query(content_uri, null, "_data = '" + file.getAbsolutePath() + "'", null, null);
                if (c == null) { return null;}
                QNDev.qnAssert(null != c);
                if(c.moveToFirst()){
                    muri = ContentUris.withAppendedId(content_uri, c.getLong(c.getColumnIndex(QNColumn._ID.column())) );
                }
                else{ QNDev.qnAssert(false); }
                c.close();              
            }
            
            File swaptemp = null;
            
            try {
                File td = (null == tempdir)? new File(QNConstants.TEMP_DIRECTORY): tempdir;
                QNDev.qnAssert(td.isDirectory());
                
                swaptemp = File.createTempFile("__xx", "xx__", td );
                // this may make unexpected delete... so.. comment out!!
                //swaptemp.deleteOnExit(); // a kind of ensurenece!!
                
                // some media applicaton may not release newly created file description. 
                // So, we need to be error-tolerable!!!
                if(file.renameTo(swaptemp) ) {
//                    resolver.delete(content_uri, "_data = '" + file.getAbsolutePath() + "'", null);
                    resolver.delete(muri, null, null);
                    if( !swaptemp.renameTo(file) ) { QNDev.qnAssert(false); }
                    // delete swapping-temp file.
                    if(swaptemp.exists()) { swaptemp.delete(); }
                    b_need_new_register = true;
                }
                if(null != swaptemp && swaptemp.exists() ) {
                    swaptemp.delete();
                }
            } catch (Exception e) {
                QNDev.qnAssert(false);
                if(null != swaptemp && swaptemp.exists() ) {
                    swaptemp.delete();
                }
                return null;
            }
        }

        if(b_need_new_register) {
            try {
               muri = resolver.insert(content_uri, values);
            } catch ( Exception e) {
               ;//catch the media register issue, but do nothing
            }
            if(null != muri) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        }
        return muri;
    }
    
    /**
     * Get bucket id of given directory from MediaStore.
     * @context : context
     * @param directory : directory path name (Absolute path)
     * @return : success : directory bucket id. / fail : null 
     */
    public static String mediaStore_directory_bucket(Context context, final String directory) {
        String  bucket_id = null; 

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("distinct", "true").build();
        Cursor cur = context.getContentResolver().query(uri, new String[] {
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DATA,
        }, null, null, null);
        
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            String  id = null, 
                    data = null;
                    
            while (!cur.isAfterLast()) {
                id = cur.getString(0);
                data = cur.getString(1);
                // NOTE : If there is subfolder in sketch..
                if( data.startsWith(directory) && 
                        !data.substring(directory.length()).contains("/") ) {
                    // found!!!
                    bucket_id = id;
                    break;
                }
                cur.moveToNext();
            }
            cur.close();
        }
        return bucket_id;
    }

    public static boolean save_image(File file, Bitmap bm, CompressFormat format) {
        QNDev.qnAssert(null != file && null != bm && null != format);
        if (null == file || null == bm || null == format) { return false;}

        try {
            if(!file.exists()) { file.createNewFile(); }
            else if( !file.canWrite() ) { 
                //QNDev.qnAssert(false); // for easy checking exceptional case!
                QNDev.log("Exception: No SD card.");
                return false;
            }
            // store bitmap into given file!!
            FileOutputStream outstream = new FileOutputStream(file);
            bm.compress(format, 100, outstream);
            outstream.close();
            return true;
        } catch (Exception e) {
            QNDev.log("Exception: No SD card.");
            //QNDev.qnAssert(false); // to catch exceptional case easily.
            return false;
        }
    }
    
    /**
     * get JPEG file of current framebuffer.
     * File is created with temporary name in given directory.
     * [NOTE] Deleting file is fully up to user!!!
     * @param directory : absolute-directory-path. It should end with "/" (last '/' means directory!!) 
     * @return : JPEG image file of current framebuffer.
     */
    public static File fScreenshot(Context context, String directory) {
        QNDev.qnAssert(null != directory && is_absolute_path(directory));
        String fileName = createNewFileName(directory+"ss", ".png"); 
        File f = new File(fileName);
        QNDev.log("fScreenshot"+ "save the screen capture in file: "+f);
        if(null != f) {
            screenshot(context, f.getPath());
        }
        return f;
    }
    
    /**
     * get Bitmap of current framebuffer. 
     * @return : Bitmap of current framebuffer.
     */
    public static void screenshot(Context context, String filepath) {
         return;
/*
        boolean isLandScape = false;
        if( is_landscape(context) ) {
           isLandScape = true;
        }
        boolean gotted = nativeScreenshot(filepath);
        if (!gotted) {
            QNDev.log("screenshot"+"Warning: bm from framebuffer is null!!!");
            return;
        }

        if( isLandScape) {
            QNDev.log("screenshot"+"it is landscape, so retate the image captured");
            // we should rotated captured bitmap if it is lanscape when we try to get the framebuffer.
            File file = new File(filepath);
            if (file.exists()) {
              try {
                 Bitmap bm = BitmapFactory.decodeFile(filepath);
                 file.delete();
                 Bitmap newbm = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), bm.getConfig());
                 Canvas canvas = new Canvas(newbm);
                 Matrix m = new Matrix();
                 float bm_cx = ((float)bm.getWidth())/2.0f,
                 bm_cy = ((float)bm.getHeight())/2.0f,
                 newbm_cx = ((float)newbm.getWidth())/2.0f,
                 newbm_cy = ((float)newbm.getHeight())/2.0f;
                 m.preRotate(-90.0f, bm_cx, bm_cy);
                 m.postTranslate(newbm_cx - bm_cx, newbm_cy - bm_cy);
                 canvas.drawBitmap(bm, m, new Paint());
                 bm.recycle();
                 save_image(file, newbm, CompressFormat.JPEG);
             } catch (Exception ex) {
                 QNDev.log("screenshot"+"retoate image failed, ex = "+ex);
                 return;
             } catch (java.lang.OutOfMemoryError e) {
                 QNDev.log("screenshot"+"retation image failed for out of Memeory, e = "+e);
                 return;
             }
           }
        } //end if landscape 
*/
    }
    
    /**
     * Fill rectangle area with given color on bitmap.
     * [Remark] in current version, only ARGB_8888 bitmap type is supported
     * @param bm : bitmap to set
     * @param area : area to fill (this should be included in 'bm''s one.
     * @param color : color to fill
     * @return : false (fail) / true (success)
     */
    // fill specific area of bitmap with given color 
    public static boolean set_Bitmap(Bitmap bm, final Rect area, int color) {

       if (null != bm) {
          Rect rect = new Rect(0, 0, bm.getWidth(), bm.getHeight());
          // area should be contained in bitmap area.
          if(! rect.contains(area)) { return false; }
          return nativeSet_Bitmap(bm, area, color);
        } else {
          return false;
        }

    }
    
    /**
     * Copy rectangle area of bitmap to other bitmap (masking color is applied)
     * [Remark] in current version, only ARGB_8888 bitmap type is supported
     * @param dst : destination bitmap (copied to)
     * @param src : source bitmap (copied from)
     * @param rectdst : area to be copied to (should be included in destination bitmap)
     * @param rectsrc : area to be copied from (should be included in source bitmap)
     * @param maskColor : masking color. This color is ignored when copy area.
     * @return : false(fail) / true(success)
     */
    public static boolean copy_Bitmap(Bitmap dst, final Bitmap src, 
                                final Rect rectdst, final Rect rectsrc, 
                                int maskColor) {
        if ((null == dst) || (null == src)) { return false;}

        // nothing-to-copy-case
        if( 0 == rectsrc.width() || 0 == rectsrc.height() ) { return true; }

        // get rectangle of bitmap.
        Rect rd = new Rect(0, 0, dst.getWidth(), dst.getHeight());
        Rect rs = new Rect(0, 0, src.getWidth(), src.getHeight());
        
        // check prerequisite
        if(Bitmap.Config.ARGB_8888 != dst.getConfig()
           || dst.getConfig() != src.getConfig()
           || rectdst.width() != rectsrc.width()
           || rectdst.height() != rectsrc.height()
           || rectsrc.width() < 0 || rectsrc.height() < 0 
           // each copying-rectangle should be included in bitmap area!
           || !rd.contains(rectdst)
           || !rs.contains(rectsrc) ) {
            return false;
        }
        
        return nativeCopy_Bitmap(dst, src, rectdst, rectsrc, maskColor);
    }
    
    /**
     * Avoid specific color in the bitmap.
     * Bitmap 'bm' may be changed if it has color that is same with 'color'
     * The pixel that has 'color' will be changed to nearest similar color.
     * @param bm
     * @param color
     * @return
     */
    /* Comment by a22977 to pass TTUPG build 
	 * public static boolean avoid_color(Bitmap bm, int color) {
        QNDev.qnAssert(null != bm && bm.getWidth() > 0 && bm.getHeight() > 0);
        // until now only ARGB_8888 is supported.
        if(Bitmap.Config.ARGB_8888 != bm.getConfig()) { return false; }
        else { return nativeAvoid_color(bm, color); }
    }*/
    
    public static boolean is_storage_mounted(Context context)
    {
        //check the internal memory
        String sdState = getInternalStorageState(context); //Environment.getExternalStorageState();

        QNDev.log("is_storage_mounted: mount state = "+sdState);
        if(Environment.MEDIA_MOUNTED.equals(sdState)) {
            return true;
        }

        String sdState_2 = null;
        for (int i = 0; i < 3; i++) { 
            sdState_2 = getInternalStorageState(context);
            if ((sdState_2 != null) && sdState_2.equals(sdState) ) {
                break;
            }
            sdState = sdState_2;
        }
        if(Environment.MEDIA_MOUNTED.equals(sdState)) {
            return true;
        }
        
        return false;
    }
    
    public static long available_storage_space(Context context) {
        //File path = Environment.getExternalStorageDirectory();
        //check the internal memory
        String path = getInternalStoragePath(context);
        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static boolean checkStorageCard(Context  context) {
        if(!QNUtil.is_storage_mounted(context)) {
            Toast.makeText(context, R.string.no_sd_createnote,
                    Toast.LENGTH_LONG).show();
            return false;
        } else if (QNUtil.available_storage_space(context) <= QNConstants.MIN_STORAGE_REQUIRED) {
            Toast.makeText(context, R.string.not_enough_free_space_sd,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        else {
            return true;
        }
    }


   
    /**
     * generate file name for new created file.
     * @return new file name with format "dir/ss2010-03-19_16-43-41_961.jpg"
     */
    public static String createNewFileName(String prefix, String ext) {
        StringBuffer newFileName = new StringBuffer();
        long currDate = System.currentTimeMillis();
        String timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
        
        newFileName.append(prefix);
        newFileName.append(timeString);
        newFileName.append(ext);
        
        File newFile = new File(newFileName.toString());

        while (newFile.exists()) {
            newFileName.delete(0, newFileName.length() - 1);
            currDate = System.currentTimeMillis();
            timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
            newFileName.append(prefix);
            newFileName.append(timeString);
            newFileName.append(ext);
            newFile = new File(newFileName.toString());
        }
        return newFileName.toString();

    }


    /**
     * generate file name for new created picture.
     * @return new file name with format "/sdcard/quicknote/image/2010-03-19_16-43-41_961.jpg"
     */
    public static String createNewImageName() {
        StringBuffer newFileName = new StringBuffer();
        long currDate = System.currentTimeMillis();
        String ext = ".jpg";
        String timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
        
        // newFileName.append("file://");
        newFileName.append(QNConstants.IMAGE_DIRECTORY);
        newFileName.append(timeString);
        newFileName.append(ext);
        
        File newFile = new File(newFileName.toString());

        while (newFile.exists()) {
            newFileName.delete(0, newFileName.length() - 1);
            currDate = System.currentTimeMillis();
            timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
//            newFileName.append("file://");
            newFileName.append(QNConstants.IMAGE_DIRECTORY);
            newFileName.append(timeString);
            newFileName.append(ext);
            newFile = new File(newFileName.toString());
        }
        return newFileName.toString();
    }

    /**
     * generate file name for new created text.
     * @return new file name with format "/sdcard/quicknote/text/2010-03-19_16-43-41_961.txt"
     */
    public static String createNewTextName() {
        StringBuffer newFileName = new StringBuffer();
        long currDate = System.currentTimeMillis();
        String ext = ".txt";
        String timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
        
        // newFileName.append("file://");
        newFileName.append(QNConstants.TEXT_DIRECTORY);
        newFileName.append(timeString);
        newFileName.append(ext);
        
        File newFile = new File(newFileName.toString());

        while (newFile.exists()) {
            newFileName.delete(0, newFileName.length() - 1);
            currDate = System.currentTimeMillis();
            timeString = DateFormat.format("yyyy-MM-dd_kk-mm-ss", currDate).toString()
                                        + "_" + (currDate%1000);
//            newFileName.append("file://");
            newFileName.append(QNConstants.TEXT_DIRECTORY);
            newFileName.append(timeString);
            newFileName.append(ext);
            newFile = new File(newFileName.toString());
        }
        return newFileName.toString();
    }

    public static Uri prepareTextNote() {
        QNDev.log("prepareTextNote() start");
        String fileName = createNewTextName();
        File textFile = new File(fileName);
        if (! textFile.exists()) { 
            try {
                textFile.createNewFile(); 
            } catch (Exception e) {
                QNDev.log(e.getMessage());
                return null;
            }
        }
        QNDev.log("prepareTextNote() end");
        return Uri.parse("file://" + fileName);
    }


    //get storage path
    public static String getInternalStoragePath(Context context){
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            QNDev.log("storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());     
            return Environment.getExternalStorageDirectory().toString();
        }

        StorageVolume[] volumes = sm.getVolumeList();

        for (StorageVolume sv: volumes) {
            // only monitor non emulated and non usb disk storage
            if (sv.isEmulated()) {
                String path = sv.getPath();
                return path;
            }
        }
        //error case
        QNDev.log("storage error -2: Fail to get internal memory path! use default: "+Environment.getExternalStorageDirectory().toString());
        return Environment.getExternalStorageDirectory().toString();
    }

   //get volumnState
   public static String getInternalStorageState(Context context) {
        String path = getInternalStoragePath(context);
        try {
            IMountService mountService = IMountService.Stub.asInterface(ServiceManager
                    .getService("mount"));
            return mountService.getVolumeState(path);
        } catch (Exception rex) {
            return Environment.MEDIA_REMOVED;
        }
    }

    public static void initDirs(Context context) {
        QNDev.log("initDirs() start");
        // /sdcard/quicknote
        loadLibrary(context);

        QNConstants.NOTE_DIRECTORY = getInternalStoragePath(context) + "/quicknote/";
        QNConstants.SKETCH_DIRECTORY = QNConstants.NOTE_DIRECTORY+ "sketch/";
        QNConstants.SKETCH_TEMP_FILE_DIRECTORY = QNConstants.NOTE_DIRECTORY+ ".sketchtmp/";
        QNConstants.TEMP_DIRECTORY = QNConstants.NOTE_DIRECTORY+ "tmp/";
        QNConstants.IMAGE_DIRECTORY = QNConstants.NOTE_DIRECTORY + "image/";
        QNConstants.SNAPSHOT_DIRECTORY = QNConstants.NOTE_DIRECTORY+ "snapshot/";
        QNConstants.TEXT_DIRECTORY = QNConstants.NOTE_DIRECTORY+ "text/";

        String path = QNConstants.NOTE_DIRECTORY;
        File d = new File(path);
        if(!d.exists()) { d.mkdirs(); }

        if(!isTextLoad(context)) {
            // /sdcard/quicknote/image
            path = QNConstants.IMAGE_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }

            // /sdcard/quicknote/snapshot
            path = QNConstants.SNAPSHOT_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }

            // /sdcard/quicknote/temp
            path = QNConstants.TEMP_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }

            //  /sdcard/quicknote/sketch
            path = QNConstants.SKETCH_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }

            //  /sdcard/quicknote/.sketchtmp
            path = QNConstants.SKETCH_TEMP_FILE_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }
        }

        //  /sdcard/quicknote/text
        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            path = QNConstants.TEXT_DIRECTORY;
            d = new File(path);
            if(!d.exists()) { d.mkdirs(); }
        }

        QNDev.log("initDirs() end");
    }


   //change the string from file:///mnt/sdcard/sketch/filename.png to /mnt/sdcard/sketch/filename.png
   public static String transFilePath(String oldFile) {
       String newFile = null;
       if (null != oldFile) {
          newFile = oldFile.substring(7);
       }
       QNDev.logi("[Quicknote]", "translate oldFile = "+oldFile+" to newFile = "+newFile);
       return newFile;
   }


  public static boolean copyfile(File fromFile, File toFile) {
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
      QNDev.log("QuickNote: copyfile"+"copy file Exception!!! e = "+e);
    } finally {
       if (from != null)
         try {
            from.close();
          } catch (IOException e) {
           QNDev.log("QuickNote: copyfile"+"1- copy file Exception!!! e = "+e);
          }
       if (to != null)
          try {
             to.close();
          } catch (IOException e) {
             QNDev.log("QuickNote: copyfile"+"2- copy file Exception!!! e = "+e);
          }

       return copied;
    }          
 }


 public static String readTextPlainUri(Uri uri, Context context) {
     int length;
     InputStream inputStream = null;
     //int maxLength = 2320000;
     //the max length for EditText is 8000
     int maxLength = 8000;

     String content = null;
     try {
        inputStream = context.getContentResolver().openInputStream(uri);
        length = inputStream.available();
        /*2013-1-21, add by amt_sunzhao for SWITCHUITWO-550 */ 
        boolean fileTooLong = false;
        byte[] b = null;
        if (length > maxLength) {
           QNDev.log("readTextPlainUri"+"file length ( "+length+") > maxLength (2320000)! use maxLength");
           length = maxLength;
           fileTooLong = true;
           Toast.makeText(context, R.string.file_too_long_cut, Toast.LENGTH_SHORT).show();
        }
        
        String charset = Charset.defaultCharset().name();
        byte fileBytes[] = new byte[length];
        inputStream.read(fileBytes);
        if(fileTooLong) {
        	b = Arrays.copyOf(fileBytes, maxLength + 1);
        	b[maxLength] = (byte) inputStream.read();
        }
        if (length > 2 && fileBytes[0] == (byte) 0xFE && fileBytes[1] == (byte) 0xFF) {
            charset = "UTF_16BE";
           content = new String(fileBytes, 2, length - 2, charset);
           QNDev.log("readTextPlainUri"+ "use UTF_16BE encoding");
        } else if (length > 2 && fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xFE) {
            charset = "UTF_16LE";
           content = new String(fileBytes, 2, length - 2, charset);
           QNDev.log("readTextPlainUri"+ "use UTF_16LE encoding");
        } else if (length > 3 && fileBytes[0] == (byte) 0xEF && fileBytes[1] == (byte) 0xBB
                         && fileBytes[2] == (byte) 0xBF) {
            charset = "UTF_8";
           content = new String(fileBytes, 3, length - 3, charset);
           QNDev.log("readTextPlainUri"+ "use UTF_8 encoding");
        } else {
           content = new String(fileBytes);
           byte tmpBytes[] = content.getBytes();
           if (length == tmpBytes.length) {
              for (int i = 0; i < length; i++) {
                  if (tmpBytes[i] != fileBytes[i]) {
                      charset = "GBK";
                       content = new String(fileBytes, charset);
                       QNDev.log("readTextPlainUri"+ "use GBK encoding");
                       break;
                  }
              }
           } else {
              charset = "GBK";
              content = new String(fileBytes, charset);
              QNDev.log("readTextPlainUri"+ "use GBK encoding");
           }
        }

        if(fileTooLong) {
        	// indicate the last character is cut
           if(content.length() == (new String(b, charset)).length()) {
         	  content = content.substring(0, content.length() - 1);
           }
        }
        /*2013-1-21, add end*/ 
     } catch(Exception e) {
        QNDev.log("readTextPlainUri"+ "IOException " + e);
        content = null;
        Toast.makeText(context, R.string.fail_to_read_file_toast, Toast.LENGTH_SHORT).show();
        e.printStackTrace();
        return null;
     }
        return content;
    }


    /** port getDegreesRotated(), rotate() from motorola/cbs/china/packages/apps/CMP/ChinaGallery/src/com
        /motorola/cgallery/ImageManager.java
    **/
    public static int getDegreesRotated(Uri uri) {
       int exif_orientation = 0;
       try {
          ExifInterface exif = new ExifInterface(uri.getPath());
          if(exif != null){
             exif_orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
             switch (exif_orientation) {
                  case ExifInterface.ORIENTATION_ROTATE_90:
                      exif_orientation = 90;
                      break;
                  case ExifInterface.ORIENTATION_ROTATE_180:
                      exif_orientation = 180;
                      break;
                  case ExifInterface.ORIENTATION_ROTATE_270:
                      exif_orientation = 270;
                      break;
                  default:
                      exif_orientation = 0;
                      break;
             }
	     QNDev.log("getDegreesRotated" + "exif orietation is "+exif_orientation);
	  }
       } catch (final Exception ignore) {
          QNDev.log("getDegreesRotated"+ "Ignoring exif error"+ ignore);
       }
       return exif_orientation;
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
      try {
         if (degrees != 0 && (degrees % 360) != 0 && b != null ) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float)b.getWidth() / 2, (float)b.getHeight() / 2);
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            b.recycle();
            return b2;
         } else {
            return b;
         }
      } catch (Exception ex) {
         return null;
      } catch (java.lang.OutOfMemoryError e) {
         return null;
      }
    }


    //set or cancel the alarm
    public static void setAlarm(Context context, Uri qnUri, Long time) {
        QNDev.log("reminder: cancel or enableAlarm...");
        Intent intent = new Intent(context, QNReceiver.class);
        intent.setAction("com.motorola.quicknote.action.ALERT");
        intent.setData(qnUri);
        QNDev.log("reminder: alarm's qnUri = "+qnUri.toString());
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        if ( 0L == time ) {
           //to cancel the alarm
           AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
           am.cancel(sender);
        } else {
           //to set the alarm
           AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
           am.set(AlarmManager.RTC_WAKEUP, time, sender);
        }
    }

    public static void initiateAlarm(Context context) {
        int i, len = QuickNotesDB.getSize(context);
        Long alertTime;

        for(i = 0; i < len; ++ i) {
            Uri uri = QuickNotesDB.getItem(context, i);
            alertTime = (Long)QuickNotesDB.read(context, uri, QNColumn.REMINDER);
            if(alertTime != 0) {
                setAlarm(context, uri, alertTime);
            }
        }
    }

    public static boolean isTextLoad(Context context) {
        return (context.getResources().getInteger(R.integer.text_load_settings) == 1);
    }

    public static void mediaScanFolderOrFile(Context context, File folder, boolean isFolder) {
        Intent intent = null;

        if ((context == null ) || (folder == null))  {return;}

        if (isFolder) {
           intent = new Intent("com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER");
        } else {
           intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        }
        Uri uri = Uri.fromFile(folder);
        intent.setData(uri);
        //QNDev.log("mediaScanFolderOrFile "+folder.toString() + " " + intent.toString());
        try {
            context.sendBroadcast(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }


    /**************************
     * Local Functions
     **************************/
    //****** Natives
    private static native boolean   nativeSet_Bitmap(Bitmap bm, final Rect area, int color);
    private static native boolean   nativeCopy_Bitmap(Bitmap dst, final Bitmap src, 
                                                final Rect rd, final Rect rs, 
                                                int maskColor);
    private static native boolean    nativeScreenshot(String filepath);
    //private static native boolean   nativeAvoid_color(Bitmap bm, int color);
}
