package com.motorola.numberlocation;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class NumberLocationUtilities {

	private static final String TAG = "NumberLocationUtilities";
    private static GZIPInputStream mGZIPInputStream = null;
    
    public static GZIPInputStream getGZIPInputStream() {
        return mGZIPInputStream;
    }

    public static void setGZIPInputStream(GZIPInputStream InputStream) {
        mGZIPInputStream = InputStream;
    }
    
    public static void closeGZIPInputStream() {
        if(mGZIPInputStream!=null)
            try {
                mGZIPInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

	public static String getNumberLocationDatabaseVersion(Context context) {
		String version = null;

		NumberLocationDatabaseHelper mdbHelper = NumberLocationDatabaseHelper
				.getInstance(context);
		SQLiteDatabase database_number_location = mdbHelper
				.getWritableDatabase();

		String sql = "select * from update_info;";
		Cursor cursor = database_number_location.rawQuery(sql, null);
		if (cursor == null || !cursor.moveToFirst()) {
			Log.d(TAG, "error in get database version.");
			return null;
		}
		if (cursor.getCount() > 0) {
			version = Integer.toString(cursor.getInt(0)) + "."
					+ Integer.toString(cursor.getInt(1)) + "."
					+ Integer.toString(cursor.getInt(2)) + ".build"
					+ cursor.getString(3);
		}
		return version;
	}
	
	public static void setNumberLocationDatabaseVersion(Context context,
			String version) {

		if (version == null)
			return;

		String[] versionArray = version.split("\\.");
		String major_version = versionArray[0];
		String medium_version = versionArray[1];
		String minor_version = versionArray[2];
		String build_timestamp = versionArray[3].substring(5);

		NumberLocationDatabaseHelper mdbHelper = NumberLocationDatabaseHelper
				.getInstance(context);
		SQLiteDatabase database_number_location = mdbHelper
				.getWritableDatabase();

		String sql = "update update_info set major_version=" + major_version
				+ ",medium_version=" + medium_version + ",minor_version="
				+ minor_version + ",build_timestamp=" + build_timestamp+ ";";
		database_number_location.execSQL(sql);
		return;
	}

	public static boolean isSystemLanguageChinese(){
		String location =  Locale.getDefault().getCountry();
		if (location != null && location.equals("CN")) {
			return true;
		}
		return false;
	}
	
	public static boolean isAutoUpdateEnabled(Context cxt){
		boolean autoUpdateEnabel = false;

		SharedPreferences prefs = cxt.getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, 0);
		autoUpdateEnabel = prefs.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);
		return autoUpdateEnabel;
	}
	
    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }
        return null;
    }
    
	public static String checkSDCardUrl(Context cxt) {
        String debugStoragePath = getSDPath();
        if(null == debugStoragePath)
            return NumberLocationConst.WEBUPDATE_URL;
        
        String debugFilePath = debugStoragePath + File.separator + NumberLocationConst.SD_CARD_DEBUG_FILE_NAME;
        
        if ((new File(debugFilePath)).exists()) {
			FileInputStream fIn = null;
			InputStreamReader isr = null;
			XmlPullParserFactory factory;
			XmlPullParser parser;

			try {
                File file = new File(debugFilePath);
				fIn = new FileInputStream(file);
				isr = new InputStreamReader(fIn);
				factory = XmlPullParserFactory.newInstance();
				parser = factory.newPullParser();
				parser.setInput(isr);

				String tagName = null;
				String request_url = null;
				int eventType = parser.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						tagName = parser.getName();
						if (NumberLocationConst.TAG_PROBLEM_CAUSE.equals(tagName)) {
							break;
						}
					} else if (eventType == XmlPullParser.TEXT) {
						if (NumberLocationConst.TAG_REQUEST_URL_PATH.equals(tagName)) {
							request_url = parser.getText();
							return request_url;
						}
					} else if (eventType == XmlPullParser.END_TAG) {
						tagName = parser.getName();
                        if (NumberLocationConst.TAG_NUMBER_LOCATION_DEBUG.equals(tagName)) {
							break;
						}
					}
					eventType = parser.next();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		}
		return NumberLocationConst.WEBUPDATE_URL;
	}
public static boolean isChinese(String name){
		String regEx = "[\\u4e00-\\u9fa5]";
		try{
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(name);
	if(m.find()){
			return true;
		}
	} catch (Exception e){
		return false;
	}
        return false;
    }

    
    public static String checkSDCardLocalUpdatePath(Context cxt) {
        String debugStoragePath = getSDPath();
        if(null == debugStoragePath)
            return null;
        
        String debugFilePath = debugStoragePath + File.separator + NumberLocationConst.SD_CARD_DEBUG_FILE_NAME;
        
        if ((new File(debugFilePath)).exists()) {
            FileInputStream fIn = null;
            InputStreamReader isr = null;
            XmlPullParserFactory factory;
            XmlPullParser parser;
            
            try {
                File file = new File(debugFilePath);
                fIn = new FileInputStream(file);
                isr = new InputStreamReader(fIn);
                factory = XmlPullParserFactory.newInstance();
                parser = factory.newPullParser();
                parser.setInput(isr);
                
                String tagName = null;
                String localUpdatePath = null;
                int eventType = parser.getEventType();
                
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        tagName = parser.getName();
                        if (NumberLocationConst.TAG_PROBLEM_CAUSE.equals(tagName)) {
                            break;
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (NumberLocationConst.TAG_LOCAL_UPDATE_PATH.equals(tagName)) {
                            localUpdatePath = parser.getText();
                            return localUpdatePath;
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        tagName = parser.getName();
                        if (NumberLocationConst.TAG_NUMBER_LOCATION_DEBUG.equals(tagName)) {
                            break;
                        }
                    }
                    eventType = parser.next();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    

    public static Reader getReaderFromLocalUpdatePathFile(Context cxt, String path) {
        Reader responseReader = null;
        String updateFileName = null;

        File packagePath = new File(path);
        if(!packagePath.exists())
            return null;
        File[] files = packagePath.listFiles();
        if(files.length!=1)
            return null;
        updateFileName = files[0].getName();
        if (updateFileName == null)
            return null;
        
        try {
            FileInputStream fis = new FileInputStream(path + File.separator + updateFileName);
            GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(fis));
            setGZIPInputStream(zis);
            BufferedReader br = new BufferedReader(new InputStreamReader(zis));
            responseReader = br;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseReader;
    }
    
    public static boolean checkDatabaseVersion(String dbVersion,
            String upVersion) {
        String[] targetVersionArray = upVersion.split("\\.");
        String[] sourceVersionArray = dbVersion.split("\\.");
        boolean versionUp = false;
        for (int i = 0; i < 3; i++) {
            int vt = Integer.valueOf(targetVersionArray[i]);
            int vs = Integer.valueOf(sourceVersionArray[i]);
            if (vt > vs) {
                versionUp = true;
                break;
            } else if (vt < vs) {
                versionUp = false;
                break;
            }
        }
        return versionUp;
    }
    
	public static boolean isExistTableInNumberLocationDatabase(SQLiteDatabase database, String tableName) {
		Cursor cursor;
		String checkTableExistSql = "select count(*) from sqlite_master where name='" + tableName + "'";
		cursor = database.rawQuery(checkTableExistSql, null);
		if (cursor == null || !cursor.moveToFirst()) {
			Log.d(TAG, "Couldn't find table name record in sqlite_master table");
			return false;
		}
		if (cursor.getCount() > 0) {
			int colIndex = cursor.getColumnIndex("count(*)");
			int existNum = cursor.getInt(colIndex);
			if (existNum > 0)
				return true;
			else
				return false;
		}
		return false;
	}
}
