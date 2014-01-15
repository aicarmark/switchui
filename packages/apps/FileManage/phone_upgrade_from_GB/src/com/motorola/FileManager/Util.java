package com.motorola.FileManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.MediaScannerConnection.OnScanCompletedListener;

import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;
import android.content.res.Resources;
import android.telephony.TelephonyManager;


public class Util {
    public final static int VIEW_LIST = 0;
    public final static int VIEW_GRID = 1;
    public final static int VIEW_THUMB = 2;

    public final static int ACTION_DELETE = 1;
    public final static int ACTION_COPY = 2;
    public final static int ACTION_MOVE = 3;
    public final static int ACTION_HIDEUNHIDE = 4;


    public final static int SORT_BY_NONE = 0;
    public final static int SORT_BY_NAME = 1;
    public final static int SORT_BY_TYPE = 2;
    public final static int SORT_BY_SIZE = 3;
    public final static int SORT_BY_DATE = 4;

    public final static int REQUEST_TYPE_COPYCHOICE = 102;
    public final static int REQUEST_TYPE_MOVECHOICE = 103;
    public final static int REQUEST_TYPE_DELETECHOICE = 104;
    public final static int REQUEST_TYPE_GETCOPYDEST = 105;
    public final static int REQUEST_TYPE_GETMOVEDEST = 106;
    public final static int REQUEST_TYPE_HIDEUNHIDE = 107;


    /* set cdma ringtone */
    public static final int   NULL_RINGTONE = 0;
    public static final int   CALL_RINGTONE = 1;
    public static final int   MESSAGE_RINGTONE = 2;
    
    /* network type */
    public static final int  CDMA_NETWORK = 0;
    public static final int  GSM_NETWORK  = 1;
    
    public final static int MSG_PROGRESS_OPENDIR_SHOW = 1;
    public final static int MSG_PROGRESS_OPENDIR_DISMISS = 2;
    public final static int MSG_PROGRESS_SORT_SHOW = 3;
    public final static int MSG_PROGRESS_SORT_DISMISS = 4;
    public final static int MSG_PROGRESS_GRID_SHOW = 5;

    public final static int FOLDER_OPERATION_OPENDIR = 1;
    public final static int FOLDER_OPERATION_REFRESH = 2;
    public final static int FOLDER_CHILD_COUNT_PROGRESS = 400;
    public final static int MAX_AUTO_SORT_LIMITED = 500;

    private final static char REALPATH_SEPERATE_CHAR = '/';

    public final static int TEXT_LIMITE_SIZE = 64000;

    public final static String TAG = "FileManager";

    public final static int PARSE_IS_SYSTEM = 0x0001;
    public final static int PARSE_CHATTY = 0x0002;
    public final static int PARSE_MUST_BE_APK = 0x0004;
    public final static int PARSE_IGNORE_PROCESSES = 0x0008;
    public static final String ID_LOCAL = "Local";
    public static final String ID_EXTSDCARD = "Extcard";

    //Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09     
    public final static int NEED_QUERY_MIME = -1;
    //End
   
    public static boolean validRealPath(String path) {
        return (path != null && path.length() > 0 && path.charAt(0) == REALPATH_SEPERATE_CHAR);
    }

    public static boolean isExist(String path) {
        if (path == null || path.length() == 0)
            return false;

        File file = new File(path);

        try {
            return file.exists();
        } catch (SecurityException e) {
            return false;
        }
    }

    public static boolean isExistFolder(String path) {
        if (path == null || path.length() == 0)
            return false;

        File file = new File(path);

        try {
            return file.exists() && file.isDirectory();
        } catch (SecurityException e) {
            return false;
        }
    }

    public static boolean isExistSDCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
     
    //Begin CQG478 liyun, IKDOMINO-2066, sdcard share 2011-8-24
    public static boolean isShareSDCard() {
        boolean ret = false;
        String state = Environment.getExternalStorageState();

        if (state != null) {
            ret = Environment.MEDIA_SHARED.equals(state);
        }
       
        return ret;
    }
    //End
    public static boolean fileOnSDCard(String path) {
        if (path == null)
            return false;

        String sdcard = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        return path.equals(sdcard) || path.startsWith(sdcard);
    }

    public static boolean isPathOfSDCard(String path, Context mContext) {
        if (path == null)
            return false;

        //String sdcard = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.equals(FileManager.getRemovableSd(mContext)) || path.equals(FileManager.getInternalSd(mContext))){
            return true;
        }else {
        	return false;
        }
    }

    public static boolean isSamePath(String path1, String path2) {
        if (path1 == null || path2 == null)
            return false;

        if (path1.equals("/") && path2.equals("/"))
            return true;

        if (path1.endsWith("/")) {
            path1 = path1.substring(0, path1.length() - 1);
        }

        if (path2.endsWith("/")) {
            path2 = path2.substring(0, path2.length() - 1);
        }

        return path1.equalsIgnoreCase(path2);
    }

    public static String getExtension(String path) {
        if (path == null) {
            return null;
        }

        int dot = path.lastIndexOf(".");
        if (dot >= 0 && dot < path.length() - 1) {
            return path.substring(dot + 1);
        } else {
            return "";
        }
    }

    public static String getFolderName(String path) {
        if (path == null) {
            return null;
        }

        int n = path.lastIndexOf("/");
        if (n >= 0 && n < path.length() - 1) {
            return path.substring(0, n + 1);
        } else if (n == path.length() - 1) {
            return path;
        } else {
            return null;
        }
    }

    public static String getPathFromFileUri(Uri uri) {
        if (uri != null) {
            String path = uri.toString();
            if (path.indexOf("file://") == 0) {
                return path.substring(7);
            }
        }
        return null;
    }

    public static String getPathFromFileUri(String uri) {
        if (uri != null && uri.indexOf("file://") == 0) {
            return uri.substring(7);
        }
        return null;
    }

    public static Uri getFileUriFromPath(String path) {
        return Uri.parse("file://" + path);
    }

    public static void getDestFolder(Activity activity, int requestcode) {
        Intent intent = new Intent();
        intent.setClass(activity, ChooseFileNameActivity.class);
        activity.startActivityForResult(intent, requestcode);
    }

    public static boolean openFile(Activity activity, String path) {
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(activity, activity.getString(R.string.file_not_exist, path),
                    Toast.LENGTH_LONG).show();
            return false;
        }


       /* long fileSize = file.length();

        if (fileSize == 0) {
            Toast.makeText(activity, R.string.file_size_zero, Toast.LENGTH_SHORT).show();
            return false;
        }*/

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

        Uri data = Uri.fromFile(file);

        //Begin CQG478 liyun, IKDOMINO-3443, MIME type 2011-10-28
        String type = getMimeType(activity, path);
        //End

        Log.d(TAG, "the Type in openFile is: " + type);
        intent.setDataAndType(data, type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.application_not_available, Toast.LENGTH_LONG).show();
        }

        return true;
    }

    public static void shareFile(Activity activity, String path) {
        if (path == null) {
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(activity, activity.getString(R.string.file_not_exist, path),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        //Begin CQG478 liyun, IKDOMINO-3443, MIME type 2011-10-28
        String mimeType = getMimeType(activity, path);
        //End
        if(FileManagerActivity.isDrm && getExtension(path).equals("dcf")){
        	DrmManagerClient drmClient = new DrmManagerClient(activity);
        	mimeType = drmClient.getOriginalMimeType(path);
        	drmClient = null;
        }       

        intent.setType(mimeType);
        

        int mime = MimeType.getMimeId(mimeType);

        String txt = null;
        Uri uri = null;

        uri = Util.getMediaContentUriByFilePath(activity, mime, path);
        if (uri == null) {
            uri = Uri.fromFile(file);
        }

        Log.d("","w23001- share file uri = " + uri + " mimetye = " + mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
/*
        if (mimeType.equals("text/plain")) {
            txt = readTextFile(file);
            intent.putExtra(Intent.EXTRA_TEXT, txt);
        } 
*/
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int sid = 0;

        //Begin CQG478 liyun, IKDOMINO-2266, add vcf type 2011-9-5
        if (mime == MimeType.MIME_TEXT || mime == MimeType.MIME_VCF)
        //End
            sid = R.string.sendText;
        else if (mime == MimeType.MIME_IMAGE)
            sid = R.string.sendImage;
        else if (mime == MimeType.MIME_VIDEO)
            sid = R.string.sendVideo;
        else if (mime == MimeType.MIME_AUDIO)
            sid = R.string.sendAudio;
        else
            sid = R.string.sendUnknown;

        try {
            activity.startActivity(Intent.createChooser(intent, activity.getText(sid)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, R.string.fail_to_share_file, Toast.LENGTH_LONG).show();
        }
    }

    public static int getMimeId(Context context, FileInfo fi) {
        
        //Begin CQG478 liyun, IKDOMINO-3443, MIME type 2011-10-28
        String mimeType = getMimeType(context, fi.getRealPath());
        //End


        return MimeType.getMimeId(mimeType);
    }

    public static int getMimeId(Context context, String fileWithPath) {
        
        //Begin CQG478 liyun, IKDOMINO-3443, MIME type 2011-10-28
        String mimeType = getMimeType(context, fileWithPath);
        //End

        return MimeType.getMimeId(mimeType);
    }

    public static String getMimeType(Context context, String fileWithPath) {
        String ext = getExtension(fileWithPath);
        String mimeType = MimeType.getMimeType(ext);

        if (isMediaFile(fileWithPath) && needQueryDB(ext.toLowerCase())) {
            String t = getMediaMimeTypeByDataBase(context, fileWithPath);
            if (t != null) {
                mimeType = t;
            } else {
                File file = new File(fileWithPath);
                int mediaType = isVideoFile(file);
            	if (mediaType == 0){
            	    mimeType = "audio/*";
            	} 
                else if (mediaType > 0 ) {
                    mimeType = "video/*";
            	}
            }
        }
        return mimeType;
    }
    
    public static boolean needQueryDB(String ext) {
        if (ext.equals("3gpp") || ext.equals("3gp") || ext.equals("rm") || ext.equals("rmvb") || ext.equals("mp4") || ext.equals("ram"))
            return true;
        else
            return false;
    }

    public static void setAs(Activity activity, FileInfo fi,int ringtone_type) {
        File file = new File(fi.getRealPath());
        if (!file.exists()) {
            Toast.makeText(activity, activity.getString(R.string.file_not_exist, fi.getRealPath()),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        String mimeType = getMimeType(activity,fi.getRealPath());
        Log.d(TAG, "setAs, mimeType is : " + mimeType);
        if (MimeType.getMimeId(mimeType) == MimeType.MIME_AUDIO || Util.getExtension(fi.getRealPath()).equals("dcf")) {
            Log.d(TAG, "setAs--,it is a Audio file.");
            if (NULL_RINGTONE!=ringtone_type ) {
                if (!setRingtone(activity, fi.getRealPath(),ringtone_type)) {             
                    Toast.makeText(activity, R.string.fail_disk_full, Toast.LENGTH_SHORT).show();
                }
            }            
            return;        
        }

        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);

        int mime = MimeType.getMimeId(mimeType);
        if (mime != MimeType.MIME_IMAGE) {
            Toast.makeText(activity, R.string.fail_to_set_file, Toast.LENGTH_LONG).show();
            return;
        }

        try {            
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent
                    .createChooser(intent, activity.getText(R.string.setImage)));                            
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, R.string.fail_to_set_file, Toast.LENGTH_LONG).show();
        }
    }


    public static String getMediaMimeTypeByDataBase(Context context, String path) {
        if (path == null)
            return null;
        Log.d (TAG, "Enter getMediaMimeTypeByDataBase");
        Cursor cursor = null;
        String mimeType = null;
        try {
             
            /* MMSP e6355c 2011/5/23 Add image files */
            String[] cols = null;
            String selection = null;
            
            cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.MIME_TYPE,};
            selection = MediaStore.Audio.Media.DATA + " like ?";            
            
            String[] selectArgs = new String[] {getPathInExternalDb(path)};

            cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs,null);

            //Begin CQG478 liyun, IKDOMINO-4703, real mimetype 2011-11-28
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst(); 
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
                mimeType = "audio/*";
            }
            //End

            //Begin CQG478 liyun, IKDOMINO-3144, force close 2011-10-17
            if (mimeType == null) {
                String[] cols1 = null;
                String selection1 = null;
            
                cols1 = new String[] { MediaStore.Video.Media._ID, MediaStore.Video.Media.MIME_TYPE,};
                selection1 = MediaStore.Video.Media.DATA + " like ?";            

                String[] selectArgs1 = new String[] {getPathInExternalDb(path) };
                
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }

                cursor = query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols1, selection1 ,
                    selectArgs1, null);

                //Begin CQG478 liyun, IKDOMINO-4703, real mimetype 2011-11-28
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst(); 
                    int idx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
                    mimeType = "video/*";         
                }
                //End
            }
            //End           
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        Log.d(TAG, "Leaving getMediaMimeTypeByDataBase, mimeType:" + mimeType );
        return mimeType;
    }

    public static long getAudioDuration(Context context, String ring) {
        if (ring == null)
            return -1;
        long duration = -1;
        Cursor cursor = null;
        try {
            String[] cols = new String[] { MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DURATION, };

            String selection = MediaStore.Audio.Media.DATA + " like ?";            
            String[] selectArgs = new String[] { getPathInExternalDb(ring) };

            cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            if (cursor == null || cursor.getCount() == 0) {
                duration = -1;
            } else {
                cursor.moveToFirst();
                duration = cursor.getLong(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return duration;

    }

    public static long getVideoDuration(Context context, String ring) {
        if (ring == null)
            return -1;
        long duration = -1;
        Cursor cursor = null;
        try {
            String[] cols = new String[] { MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DURATION, };

            String selection = MediaStore.Video.Media.DATA + " like ?";            
            String[] selectArgs = new String[] { getPathInExternalDb(ring) };

            cursor = query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            if (cursor == null || cursor.getCount() == 0) {
                duration = -1;
            } else {
                cursor.moveToFirst();
                duration = cursor.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return duration;
    }

    public static String getImageResolution(Context context, String image) {
        if (image == null)
            return "";

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(image, options);
            if (options.outHeight > 0 && options.outWidth > 0) {
                return String.format("%d x %d", options.outWidth, options.outHeight);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "";
    }

    /*
     * public static String getImageResolution(Context context, String image) {
     * String resolution = ""; if (image == null) return resolution; Cursor
     * cursor = null; try { String[] cols = new String[] {
     * MediaStore.Images.Media._ID, MediaStore.Images.Thumbnails.WIDTH,
     * MediaStore.Images.Thumbnails.HEIGHT }; String selection =
     * MediaStore.Images.Media.DATA + " like ?"; String[] selectArgs = new
     * String[] { image }; cursor = query(context,
     * MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, selection,
     * selectArgs, null); if (cursor != null && cursor.getCount() > 0) {
     * cursor.moveToFirst(); resolution = String.valueOf(cursor.getInt(1)) +
     * " x " + String.valueOf(cursor.getInt(2)); } } catch (Exception e) {
     * e.printStackTrace(); } finally { if (cursor != null) { cursor.close();
     * cursor = null; } } return resolution; }
     */
    public static boolean isInAudioDatabase(Context context, String ring) {
        if (ring == null)
            return false;
        Cursor cursor = null;
        boolean isAudio = false;
        try {

            String[] cols = new String[] { MediaStore.Audio.Media._ID, };
            
            String selection = MediaStore.Audio.Media.DATA + " like ?";            
            String[] selectArgs = new String[] {getPathInExternalDb(ring) };

            cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            isAudio = (cursor != null && cursor.getCount() != 0);
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return isAudio;
    }

    public static boolean isInImagesDatabase(Context context, String fileWithPath) {
        if (fileWithPath == null)
            return false;
        Cursor cursor = null;
        boolean isImage = false;
        try {

            String[] cols = new String[] { MediaStore.Images.Media._ID, };
            
            String selection = MediaStore.Images.Media.DATA + " like ?";
            String[] selectArgs = new String[] { getPathInExternalDb(fileWithPath) };

            cursor = query(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            isImage = (cursor != null && cursor.getCount() != 0);
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return isImage;
    }

    public static long getMeidaIdByFilePath(Context context, String filePath) {
        long id = 0;

        String where = MediaStore.Images.Media.DATA + " like ?";
        
        String[] args = new String[] { getPathInExternalDb(filePath) };
        String[] cols = new String[] { MediaStore.Images.Media._ID, };

        Cursor cs = null;

        try {
            cs = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cols, where, args, null);
            if (cs != null && cs.getCount() != 0) {
                cs.moveToFirst();
                id = cs.getLong(0);
            }
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
        } finally {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        return id;
    }

    /*
     * change "=?" to " like ?" for MediaProvider reason,for new captured
     * picture, "=?" doesn't work.
     */
    public static void deleteFromMediaDataBase(Context context, int mimeId, String filename) {

        try {

            ContentResolver cr = context.getContentResolver();
            String[] selectArgs = new String[] { getPathInExternalDb( filename)};
            String where;

            switch (mimeId) {

            case MimeType.MIME_IMAGE:
                where = MediaStore.Images.Media.DATA + " like ?";
                cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, where, selectArgs);
                break;
            case MimeType.MIME_AUDIO:
                where = MediaStore.Audio.Media.DATA + " like ?";
                cr.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, selectArgs);
                break;
            case MimeType.MIME_VIDEO:
                where = MediaStore.Video.Media.DATA + " like ?";
                cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, where, selectArgs);
                break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   /* public static boolean isRingtoneFile(Context context, String ring) {
        if (ring == null)
            return false;
        Cursor cursor = null;
        boolean isRingtone = false;
        try {
            String[] cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, };
           
            String selection = MediaStore.Audio.Media.DATA + " like ?";
            String[] selectArgs = new String[] {getPathInExternalDb(ring) };

            cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            int id = -1;

            if (cursor == null || cursor.getCount() == 0) {
                isRingtone = false;
            } else {
                cursor.moveToFirst();
                id = cursor.getInt(0);

                Uri ringUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri current = android.media.RingtoneManager.getActualDefaultRingtoneUri(context,
                        android.media.RingtoneManager.TYPE_RINGTONE);
                isRingtone = (ringUri != null && current != null && ringUri.equals(current));
                if (!isRingtone && ringUri != null){
                    String SourceUri = Settings.System.getString(context.getContentResolver(), "gsm_ringtone");
                    isRingtone = (SourceUri!=null && ringUri.toString().equals(SourceUri));
                }
                if (!isRingtone && ringUri != null){
                    String SourceUri = Settings.System.getString(context.getContentResolver(), "cdma_message_ringtone");
                    isRingtone = (SourceUri!=null && ringUri.toString().equals(SourceUri));
                }
                if (!isRingtone && ringUri != null){
                    String SourceUri = Settings.System.getString(context.getContentResolver(), "gsm_message_ringtone");
                    isRingtone = (SourceUri!=null && ringUri.toString().equals(SourceUri));
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return isRingtone;

    }
    */
    public static boolean setRingtone(Context context, String ring, int   ringtone_type) {
        if (ring == null)
            return false;
        Cursor cursor = null;
        boolean result = true;
        try {
            String[] cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, };

            String selection = MediaStore.Audio.Media.DATA + " like ?";           
            String[] selectArgs = new String[] { getPathInExternalDb(ring) };

            cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                    selectArgs, null);

            int id = -1;
            Log.d(TAG, "canRingtone: getCount:" +  cursor.getCount() );
            if (cursor == null || cursor.getCount() == 0) {
                result = false;

            } else {
                cursor.moveToFirst();
                id = cursor.getInt(0);

                Uri ringUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Intent intent = null;
                if (ringtone_type == CALL_RINGTONE) {
                	intent = new Intent("AP_SET_CALL_RINGTONE");
                	intent.putExtra("callringUri", ringUri.toString());
                } else if (ringtone_type == MESSAGE_RINGTONE) {
                	intent = new Intent("AP_SET_MMS_RINGTONE");
                	intent.putExtra("mmsringUri", ringUri.toString());
                }
                Log.d("","canRingtone -- uri = " + ringUri.toString() + " action = " + intent.getAction());
                context.sendBroadcast(intent);               
            } 
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return result;
    }

    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        //Begin CQG478 liyun, IKDOMINO-3144, force close 2011-10-17
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //End
    }

    public static long getFolderSize(File file) {
        if (file != null && file.isDirectory()) {
            long totalSize = 0;
            //Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature 2011-8-2
            //File[] children = file.listFiles();
            File[] children = getFileList(file, true);
            //End
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        totalSize += getFolderSize(child);
                    } else {
                        totalSize += child.length();
                    }
                }
            }
            return totalSize;
        } else {
            return 0;
        }
    }

    //Begin CQG478 liyun, IKDOMINO-1814 check filename lenght 2011-9-14
    public static boolean exceedFileNameMaxLen(String name) {
        if (name != null && name.getBytes().length > 255) {                
            return true;
        }  
          
        return false;
    }
    //End

    public static boolean isValidateFileName(String name) {
        //Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature 2011-8-2
	//modify by amt_xulei for SWITCHUITWOV-98 2012-9-7
	//reason:file or folder name cann't end with "."
        if (name.contains("/") || name.contains("\\") || name.endsWith("."))
	//if (name.contains("/") || name.contains("\\"))
	//end modify by amt_xulei
        //if (name.contains("/") || name.contains("\\") || name.charAt(0) == '.')
            return false;
        //End

        return true;
    }

    public static String getSizeString(long size) {
        if (size < 1024) {
            return String.format("%dB", size);
        } else if (size < 1024 * 1024) {
            return String.format("%.1fK", ((float) size) / 1024);
        } else {
            return String.format("%.1fM", ((float) size) / (1024 * 1024));
        }

    }

    public static String removeSpace(String str) {
        if (str == null)
            return null;

        char[] array = str.toCharArray();
        int start = 0;
        int end = array.length - 1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != ' ') {
                start = i;
                break;
            }
            if (i == end)
                return null;
        }

        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] != ' ') {
                end = i;
                break;
            }
        }

        if (start == 0 && end == array.length - 1) {
            return str;
        } else {
            return str.substring(start, end + 1);
        }
    }

    public static boolean isMediaFile(String file) {
        String ext = getExtension(file);
        String mimeType = MimeType.getMimeType(ext);

        int mimeId = MimeType.getMimeId(mimeType);
        if ((mimeId == MimeType.MIME_IMAGE) || (mimeId == MimeType.MIME_VIDEO)
                || (mimeId == MimeType.MIME_AUDIO)) {
            return true;
        } else {
            return false;
        }
    }
    
    //modify by amt_xulei for SWITCHUITWO-509 2013-1-11
    //reason:scan folder method.Need scan folder after move/delete/copy/rename operator
    public static void updateMediaProvider(Activity activity) {
    	String str1 = new String("file://");
    	String str3 = str1 + FileSystem.SDCARD_DIR;
		Uri localUri = Uri.parse(str3);
		Intent localIntent = new Intent("android.intent.action.MEDIA_MOUNTED");
		localIntent.setData(localUri);
		localIntent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
		activity.sendBroadcast(localIntent);
		
		str3 = str1 + FileSystem.Internal_SDCARD_DIR;
		localUri = Uri.parse(str3);
		localIntent.setData(localUri);
		activity.sendBroadcast(localIntent);
    }
    
    public static void updateMediaProvider(Activity activity,String destPath) {
    	String str1 = new String("file://");
    	String scanPath = destPath;
    	if (!destPath.startsWith(str1)){
    		scanPath = str1 + destPath;
    	}
		Log.d("", "scanPath= "+scanPath);
		Uri localUri = Uri.parse(scanPath);
		Intent localIntent = new Intent("android.intent.action.MEDIA_MOUNTED");
		localIntent.setData(localUri);
		localIntent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
		activity.sendBroadcast(localIntent);
    }
    //end modify
    
    //modify by amt_xulei for SWITCHUITWO-426
	//reason:change scan file method,ACTION_MEDIA_SCANNER_SCAN_FILE can only use for external storage
    public static void mediaScanFile(Activity activity, File file) {
//        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        Uri uri = Uri.fromFile(file);
//        intent.setData(uri);
//        try {
//            activity.sendBroadcast(intent);
//        } catch (android.content.ActivityNotFoundException ex) {
//            ex.printStackTrace();
//        }
        Context context = activity.getApplicationContext();
        String[] paths;
        String path;
        String[] mimeTypes;
		path = file.getPath();
		paths = new String[] { path };
		mimeTypes = new String[]{getMimeType(context,path)};
        
        MediaScannerConnection.scanFile(context, paths, mimeTypes, new OnScanCompletedListener()
        {
			@Override
			public void onScanCompleted(String path, Uri uri) {
				Log.d("", "***onScanCompleted***");
			}});
    }
    //end modify
    
    public static long getFreeDiskSpace(String content, Activity activity) {
        StatFs stat = null;
        long blockSize = 0;
        if (content.equals(ID_EXTSDCARD)){
            stat= new StatFs(FileManager.getRemovableSd(activity));
            blockSize = stat.getBlockSize();
        } else if (content.equals(ID_LOCAL)){
        	stat= new StatFs(FileManager.getInternalSd(activity));
        	blockSize = stat.getBlockSize();
        }        
        return stat.getAvailableBlocks() * blockSize;
    }

    public static String readTextFile(File file) {
        int length;
        byte buffer[], tmp_buf[];
        String ret_str = null; // tmp_str;

        try {

            FileInputStream fis = new FileInputStream(file);
            length = fis.available();
            if (length > TEXT_LIMITE_SIZE)
                length = TEXT_LIMITE_SIZE;
            buffer = new byte[length];
            length = fis.read(buffer, 0, length);
            fis.close();

            if (buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF) {
                ret_str = new String(buffer, 2, length - 2, "UTF_16BE");
            } else if (buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) {
                ret_str = new String(buffer, 2, length - 2, "UTF_16LE");
            } else if (buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB
                    && buffer[2] == (byte) 0xBF) {
                ret_str = new String(buffer, 3, length - 3, "UTF_8");
            } else {

                /*
                 * we need to guess the encoding of contents in the buffer but
                 * now, this is a temp solution with higher cost If default
                 * encoding does not work, just use GBK
                 */
                ret_str = new String(buffer);
                tmp_buf = ret_str.getBytes();
                if (length == tmp_buf.length) {
                    while (length-- > 0) {
                        if (buffer[length] != tmp_buf[length]) {
                            ret_str = new String(buffer, "GBK");
                            break;
                        }
                    }
                } else {
                    ret_str = new String(buffer, "GBK");
                }
            }
        } catch (IOException eio) {
            eio.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ret_str == null) {
            return "";
        } else {
            return ret_str;
        }
    }

    public static  Drawable getApkIcon(Context pContext,String archiveFilePath) {
        Drawable icon=null;
        if (!archiveFilePath.endsWith(".apk")){
            return null;
        }

        PackageParser packageParser = new PackageParser(archiveFilePath);
        File sourceFile = new File(archiveFilePath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pkg;
        try {
            pkg = packageParser.parsePackage(sourceFile, archiveFilePath, metrics, 0);
            if (pkg == null) {
                return null;
             }
        } catch (Exception e) {
        	return null;
        }
        
        Resources pRes = pContext.getResources();
        AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(archiveFilePath);
        Resources res = new Resources(assmgr, pRes.getDisplayMetrics(), pRes.getConfiguration());

        if (pkg.applicationInfo.icon != 0) {
            try {
                icon = res.getDrawable(pkg.applicationInfo.icon);
            } catch (Resources.NotFoundException e) {
               
                return null;
            }
        }
        
        return icon;
    }  
    public static boolean matches(FileInfo fi, String pattern) {
        if (fi.isDirectory())
            return true;

        String suffix = pattern.substring(1);

        if (suffix == null)
            return false;

        String name = fi.getTitle();
        return (name.toLowerCase().endsWith(suffix.toLowerCase()));
    }

    //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
    public static boolean isMimeTypeMatch(Context context, FileInfo fi, String type) {
        if (fi.isDirectory())
            return true;
      
        if (type == null || (type != null && type.equals("*/*"))) {
            return true;
        }
    	
        String mimeType = getMimeType(context, fi.getRealPath());
        int index = type.indexOf("/*");
    	
        if (index == -1) {
            if (mimeType.equalsIgnoreCase(type)) {
                return true;
            }
        } else {
            if (mimeType.indexOf(type.substring(0, index)) != -1) {
                return true;
            }
    	}
        return false;    	
    }
    //End

    public static class FilesContainer {
        public FilesContainer() {

        }

        private FileInfo[] mFiles;

        public void setFiles(FileInfo[] files) {
            mFiles = files;
        }

        public FileInfo[] getFiles() {
            return mFiles;
        }

        public FileInfo[] getUnHideFiles() {
        	if(mFiles!=null){
        		int count=0;
        		for(int i=0;i<mFiles.length;i++) if(mFiles[i].isHidden()==false) count++;
        		if(count>0)
        		{	FileInfo[] myfiles=new FileInfo[count];
        			for(int i=0,j=0;i<mFiles.length;i++) if(mFiles[i].isHidden()==false) {myfiles[j]=mFiles[i];j++;}
        			return myfiles;
        		}
        	}
        	return null;
        }


    }

    private static GetChildrenThread mChildrenThread = null;
    public static void stopChildrenThread(){
    	if(mChildrenThread != null) {
            mChildrenThread.stopping();
            try {
                mChildrenThread.interrupt();
                mChildrenThread.join();
            } catch(InterruptedException e) {
            }
            mChildrenThread = null;
        }
    }
    public static void StartChildrenThread(Handler handler, FilesContainer fileList, FileInfo fi,
        ArrayList<String> filter, int sortMethod, boolean ascending, boolean onlyFolder) {
    	stopChildrenThread();
        mChildrenThread = new GetChildrenThread(
            handler, fileList, fi, filter, sortMethod, ascending, onlyFolder);
        mChildrenThread.start();
    }
    public static class GetChildrenThread extends Thread {
        private Handler mHandler;
        private FilesContainer mFilesContainer = new FilesContainer();
        private FileInfo mFi;
        private ArrayList<String> mFilter;
        private int mSortMethod = Util.SORT_BY_NAME;
        private boolean mAscending = true;
        private boolean mOnlyFolder = false;
        private boolean mStopping = false;

        //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
        private ChooseFileNameActivity.CHOOSE_FILE_ACTION_TYPE mFiltType;
        private Context mContext;
        //End

        public GetChildrenThread(Handler handler, FilesContainer fileList, FileInfo fi,
                ArrayList<String> filter, int sortMethod, boolean ascending, boolean onlyFolder) {

            super();
            mHandler = handler;
            mFilesContainer = fileList;
            mSortMethod = sortMethod;
            mAscending = ascending;
            mFi = fi;
            mFilter = filter;
            mOnlyFolder = onlyFolder;
        }

        //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
        public void setFiltType(Context context, ChooseFileNameActivity.CHOOSE_FILE_ACTION_TYPE type) {
            mContext = context;
            mFiltType = type;
        }
        //End

        public void stopping() {
            mStopping = true;
        }
        public void run() {
            Log.d("", "w23001-GetChildrenThread running...");
            FileInfo[] files = null;
            if (mOnlyFolder == false) {files = mFi.getChildren();}
            else {files = mFi.getFolderChildren();}

            if(mStopping) return;
            if (files != null && files.length > 0) {
                if (isFIHModel()) {
                    mFilter = null;
                   Log.d("","FIH Model");
                }
                if (mFilter != null && mFilter.size() > 0) {

                    ArrayList<FileInfo> files_a = new ArrayList<FileInfo>();
                    for (FileInfo info : files) {
                        for (String pattern : mFilter) {
                            //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
                            if (mFiltType == ChooseFileNameActivity.CHOOSE_FILE_ACTION_TYPE.ACTION_GET_CONTENT) {
                                if (isMimeTypeMatch(mContext, info, pattern)) {
                                    files_a.add(info);
                                    break;
                                }
                            } else if (mFiltType == ChooseFileNameActivity.CHOOSE_FILE_ACTION_TYPE.ACTION_CHOOSE_FILE) {
                                if (matches(info, pattern)) {
                                    files_a.add(info);
                                    break;
                                }
                            }
                            if(mStopping) break;
                            //End
                        }
                        if(mStopping) break;
                    }
                    if(mStopping) return;
                    files = new FileInfo[files_a.size()];
                    files_a.toArray(files);

                }
                /*if (files.length > Util.MAX_AUTO_SORT_LIMITED) {
                    mSortMethod = Util.SORT_BY_NONE;
                }*/
                if(mStopping) return;
                Util.Sorter sort = new Util.Sorter(files, mSortMethod, mAscending);
                sort.sort();
            }

            if(mStopping) return;
            mFilesContainer.setFiles(files);
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_DISMISS);
        }
    }

    public static class SortFilesThread extends Thread {
        private Handler mHandler;
        private FilesContainer mFilesContainer = new FilesContainer();
        private int mSortMethod = Util.SORT_BY_NAME;
        private boolean mAscending = true;

        public SortFilesThread(Handler handler, FilesContainer filesContainer, int method,
                boolean ascending) {
            super();
            mHandler = handler;
            mFilesContainer = filesContainer;
            mSortMethod = method;
            mAscending = ascending;
        }

        public void run() {

            if (mFilesContainer.getFiles() != null && mFilesContainer.getFiles().length > 0) {
                /*if (mFilesContainer.getFiles().length > Util.MAX_AUTO_SORT_LIMITED) {
                    mSortMethod = Util.SORT_BY_NONE;
                }*/
                Util.Sorter sort = new Util.Sorter(mFilesContainer.getFiles(), mSortMethod,
                        mAscending);
                sort.sort();
            }
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_SORT_DISMISS);
        }
    }

    public static class Sorter {
        private FileInfo[] mFiles;
        private int mSortMethod = Util.SORT_BY_NAME;
        private boolean mAscending = true;
        private Collator mCollator = null;

        public Sorter(FileInfo[] files, int sortMethod, boolean ascending) {
            mFiles = files;
            mSortMethod = sortMethod;
            mAscending = ascending;
        }

        public FileInfo[] getFiles() {
            return mFiles;
        }

        private int getCount() {
            return mFiles != null ? mFiles.length : 0;
        }

        public void sort() {
            int n = getCount();

            if (n > 1)
                bubbleSort(n);
        }

        private int compare(int left, int right) {
            FileInfo leftFile = mFiles[left];
            FileInfo rightFile = mFiles[right];

            if (leftFile == null || rightFile == null)
                return 0;

            int result = 0;
            // directory will always in front of file
            if (!leftFile.isDirectory() && rightFile.isDirectory()) {
                result = 1;
                if (Util.SORT_BY_NONE == mSortMethod) {
                    return result;
                } else {
                    return mAscending ? result : (0 - result);
                }
            } else if (leftFile.isDirectory() && !rightFile.isDirectory()) {
                result = -1;
                if (Util.SORT_BY_NONE == mSortMethod) {
                    return result;
                } else {
                    return mAscending ? result : (0 - result);
                }
            }

            // hidden directory/file always behind of normal directory/file
            if (leftFile.isDirectory() && rightFile.isDirectory()) {

                if (leftFile.isHidden() && !rightFile.isHidden()) {
                    result = 1;
                    if (Util.SORT_BY_NONE == mSortMethod) {
                        return result;
                    } else {
                        return mAscending ? result : (0 - result);
                    }
                } else if (!leftFile.isHidden() && rightFile.isHidden()) {
                    result = -1;
                    if (Util.SORT_BY_NONE == mSortMethod) {
                        return result;
                    } else {
                        return mAscending ? result : (0 - result);
                    }
                }
            }

            if (!leftFile.isDirectory() && !rightFile.isDirectory()) {
                if (leftFile.isHidden() && !rightFile.isHidden()) {
                    result = 1;
                    if (Util.SORT_BY_NONE == mSortMethod) {
                        return result;
                    } else {
                        return mAscending ? result : (0 - result);
                    }
                } else if (!leftFile.isHidden() && rightFile.isHidden()) {
                    result = -1;
                    if (Util.SORT_BY_NONE == mSortMethod) {
                        return result;
                    } else {
                        return mAscending ? result : (0 - result);
                    }
                }
            }

            switch (mSortMethod) {
            case Util.SORT_BY_NAME:

                if (mCollator == null) {
                    mCollator = java.text.Collator.getInstance();
                }
                result = mCollator.compare(leftFile.getTitle(), rightFile.getTitle());

                // result =
                // leftFile.getTitle().compareToIgnoreCase(rightFile.getTitle());
                break;
            case Util.SORT_BY_SIZE:
                result = (int) (leftFile.getSize() - rightFile.getSize());
                break;
            case Util.SORT_BY_DATE:
                result = (leftFile.getDate() >= rightFile.getDate()) ? 1 : -1;

                break;
            case Util.SORT_BY_TYPE:
                if (!leftFile.isDirectory() && !rightFile.isDirectory()) {
                    String leftExt = Util.getExtension(leftFile.getRealPath());
                    String rightExt = Util.getExtension(rightFile.getRealPath());
                    result = leftExt.compareToIgnoreCase(rightExt);
                }
                break;

            }
            return mAscending ? result : (0 - result);
        }

        private void swap(int left, int right) {
            FileInfo temp = mFiles[left];
            mFiles[left] = mFiles[right];
            mFiles[right] = temp;
        }

        public void quickSort(int left, int right) {
            int i, j;
            i = left;
            j = right;
            int middle = left;
            FileInfo temp = mFiles[left];
            while (true) {
                while ((++i) < right - 1 && compare(i, middle) < 0)
                    ;
                while ((--j) > left && compare(j, middle) > 0)
                    ;
                if (i >= j)
                    break;
                swap(i, j);
            }

            mFiles[left] = mFiles[j];
            mFiles[j] = temp;

            if (left < j)
                quickSort(left, j);

            if (right > i)
                quickSort(i, right);
        }

        public void bubbleSort(int size) {

            for (int i = 0; i < size; i++) {

                for (int j = size - 1; j > i; j--) {

                    if (compare(i, j) > 0) {
                        swap(i, j);

                    }
                }
            }
        }

    }


    /*  IKTU-577  When query external.db ,change file path from "/sdcard/..." to "/mnt/sdcrad/..." */
    public static String getPathInExternalDb(String source) {   
        if (null == source  )   
            return null;   
        
        final String from;
        if (FileSystem.SDCARD_DIR.equals(ID_EXTSDCARD)){
        	from =FileSystem.SDCARD_DIR;
        } else {
        	from =FileSystem.Internal_SDCARD_DIR;
        }
        final String to = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

        if (from.equals(to))
            return source;
        
        StringBuffer bf = new StringBuffer("");   
        
        int index = -1;   
        index = source.indexOf(from);
        if (index !=  -1 && 0==index) {   
            bf.append(to+source.substring(from.length(), source.length())); 
            return bf.toString();   
        }   
        return source;       
    } 
    public static final boolean isDualMode() {
        String dual = SystemProperties.get("ro.telephony.dualmode");
        if( dual != null && dual.equals("yes")){
            return true;
        } else {
            return false;
        }
    } 

    public static final int getSingleMode(Context context) {    //call when phone is single mode
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getPhoneType();
    }  

    //Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature 2011-8-2 
    public static File[] getFileList(File file, boolean includeHide) {
        if (includeHide) {
            try {
                return file.listFiles();
            } catch (Exception e) {
       		Log.d("", "cxxlyl- getFileList catch an exception");
            }
            return null; 
        } else {
            return file.listFiles(new FileFilter() {
                public boolean accept(File pathname) {                      
                    return !pathname.isHidden();
                }
            });
        }
    }
    //End 

    //Begin CQG478 liyun, IKDOMINO-2292, scroll position 2011-9-2
    public static class ScrollPosition {
    	public int index;
    	public int top;
    }
    
    public static  int getCurrentDepth(FileInfo fileInfo) {
    	String tempStr = null;
    	int depth = 0;
    	int tempIndex = 0;
    	
    	if (fileInfo != null) {
    		if (FileSystem.SDCARD_DIR.equals(ID_EXTSDCARD)){
                tempStr = fileInfo.getRealPath().substring(FileSystem.SDCARD_DIR.length());
    		}else{
    			tempStr = fileInfo.getRealPath().substring(FileSystem.Internal_SDCARD_DIR.length());
    		}
    	}
    	
    	if (tempStr != null) {
            tempIndex = tempStr.indexOf("/");
            while (tempIndex != -1) {
                tempIndex = tempStr.indexOf("/", tempIndex + 1);
                depth++;
            }
        }
    	
    	return depth;
    }
    //End

    //Begin CQG478 liyun, IKDOMINO-3650, sub folder 2011-11-01
    public static boolean isSubFolder(String src, String dest)
    {
        if (src == null || dest == null) {
            return false;
        }

        int srcLen = src.length();
        int destLen = dest.length();

        if (srcLen == destLen) {
            if (dest.equals(src)) {
                return true;
            }
            return false;
        } else if (destLen > srcLen) {
            if (dest.startsWith(src + "/")) {
                return true;
            }
            return false;
        } else {
            return false;
        }		
    }
    //End

    //Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
    public static int getMimeIdWithoutQueryDB(final String fileWithPath) {
        String ext = Util.getExtension(fileWithPath);
        String mimeType = MimeType.getMimeType(ext);

        if (Util.isMediaFile(fileWithPath) && Util.needQueryDB(ext.toLowerCase())) {
            return NEED_QUERY_MIME;
        }

        return MimeType.getMimeId(mimeType);
    }

    public static interface ImageCallBack
    {
        public void setImageIcon();
    };
    //End

    //Begin CQG478 liyun, IKDOMINO-4474, change Uri format 2011-11-18
    public static Uri getMediaContentUriByFilePath(Context context, int mimeId, String path) {
        Cursor cursor = null;
        long id = 0;
        Uri retUri = null;
        String selection = null;
        String[] cols =null;
        
        try {         
            String[] selectArgs = new String[] {getPathInExternalDb(path)};

            switch(mimeId) {
                case MimeType.MIME_AUDIO:
                    cols = new String[] { MediaStore.Audio.Media._ID, };
                    selection = MediaStore.Audio.Media.DATA + " like ?";

                    cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection,
                        selectArgs,null);

                    if (cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        id = cursor.getLong(0);
                    }
                    if (id != 0) {
                        retUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                     }
                    break;
                case MimeType.MIME_VIDEO:
                    cols = new String[] { MediaStore.Video.Media._ID,};
                    selection = MediaStore.Video.Media.DATA + " like ?";

                    cursor = query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, selection,
                        selectArgs, null);

                    if (cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        id = cursor.getLong(0);
                    }
                    if (id != 0) {
                        retUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    }
                    break;
                case MimeType.MIME_IMAGE:
                    cols = new String[] { MediaStore.Images.Media._ID,};
                    selection = MediaStore.Images.Media.DATA + " like ?";
                    
                    cursor = query(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, selection,
                            selectArgs, null);

                    if (cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        id = cursor.getLong(0);
                    }
                    if (id != 0) {
                        retUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    }
                    break;
                default:
                    return retUri;
            }
            
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }        
        return retUri;
    }
    //End
    public static int isVideoFile(File mediaFile) { 
        int height = 0;
        try {
            MediaPlayer mp = new MediaPlayer();
            FileInputStream fs;
            FileDescriptor fd;
            fs = new FileInputStream(mediaFile);
            fd = fs.getFD();
            mp.setDataSource(fd);
            mp.prepareAsync(); 
            //mp.prepare();
            height = mp.getVideoHeight();
            mp.release();
        } catch (Exception e) {
            Log.e(TAG, "Exception trying to determine if file is video.", e);
            height = -1; 
        }
        return height;
    }
    
    public static final String[] CT_MODEL = 
    {
        "XT785",
        "MOT-XT785",
        "MOT-XT788"
    };
    
    public static boolean isCTModel()
    {
    	String cur_model = SystemProperties.get("ro.product.model");    	
    	for(String model: CT_MODEL) {
    		if(model.equals(cur_model)){
    			return true;
    		}
    	}    	 
    	return false;
    }

    public static final String[] FIH_MODEL = 
    {
        "XT535",
        "XT536",
        "XT685",
        "XT687",
        "XT682",
        "XT780",
        "XT780_7009"
    };
        
    public static boolean isFIHModel()
    {
       String cur_model = SystemProperties.get("ro.product.model");
       Log.d("","FIH cur_model = " + cur_model);    	
       for(String model: FIH_MODEL) {
    	   if(model.equals(cur_model)){
    		   return true;
    	   }
       }    	 
       return false;
    }
    public static boolean isSameDisk(String src, String dest) {
        String[] sName = null;
        String[] dName = null;

        sName = src.split("/");
        dName = dest.split("/");

        if (sName[2].equals(dName[2])) {
            return true;
        } else {
            return false;
        }
    }
}
