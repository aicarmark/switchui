/**
 * FILE: PluginWidgetHostId.java
 *
 * DESC: Manager the id of plugin widget. This class could guarantee 
 * 		 plugin widget to get unique id. 
 */
package com.motorola.mmsp.plugin.widget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;


import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class PluginWidgetHostId {
	private final String TAG = "PluginWidgetHost";
	
	/**
	 * The file which plugin widget state is saved or restored. 
	 */
	private final static String SETTING_FILE = "/pluginwidget.xml";
	
	/**
	 * The temporary file which plugin widget state is saved or restored.
	 */
	private final static String SETTING_TEM_FILE = SETTING_FILE + ".tmp";
	
	/**
	 * Plugin widget id is not valid.
	 */
	private final static int VALID = -1;
	
	/**
	 * The next plugin widget id.
	 */
	private int mNextPluginWidgetId = VALID + 1;
	
	/**
	 * Context for PluginWidgetHostId.
	 */
	private Context mContext;
	
	/**
	 * The array list of PluginWidgetId.
	 */
	private ArrayList<PluginWidgetId> mPluginWidgetIds = new ArrayList<PluginWidgetId>();
	
	
	/**
	 * @param context
	 */
	public PluginWidgetHostId(Context context) {
		mContext = context;
		loadStateLocked();
	}
	
	/**
	 * @return
	 */
	private File savedStateTempFile() {
		//return new File(mContext.getApplicationContext().getFilesDir().getAbsolutePath() + SETTING_TEM_FILE);
	    return new File(mContext.getFilesDir().getAbsolutePath() + SETTING_TEM_FILE);
	}
	
	/**
	 * @return
	 */
	private File savedStateRealFile() {
		//return new File(mContext.getApplicationContext().getFilesDir().getAbsolutePath() + SETTING_FILE);
	    return new File(mContext.getFilesDir().getAbsolutePath() + SETTING_FILE);
	}
	
	/**
	 * Load plugin widget state from a local file {@link #SETTING_FILE}.
	 */
	private void loadStateLocked() {
		File temp = savedStateTempFile();
		File real = savedStateRealFile();
		if (real.exists()) {
			readStateFromFileLocked(real);
	        if (temp.exists()) {	                
	            temp.delete();
	        }
		} else if (temp.exists()) {
			readStateFromFileLocked(temp);	           
	        temp.renameTo(real);
		}  
	 }
	
	/**
	 * Save plugin widget state to a local file {@link #SETTING_FILE}.
	 */
	private void saveStateLocked() {
        File temp = savedStateTempFile();
        File real = savedStateRealFile();
        if (!real.exists()) {
            try {
                real.createNewFile();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (temp.exists()) {
            temp.delete();
        }
        if (!writeStateToFileLocked(temp)) {
            Log.w(TAG, "Failed to persist new settings");
            return;
        }        
        real.delete();       
        temp.renameTo(real);
    }
	
	/**
	 * Read plugin widget state from a local file.
	 * 
	 * @param file The local file.
	 */
	private void readStateFromFileLocked(File file) {
		FileInputStream stream = null;
		boolean success = false;
		try {
			stream = new FileInputStream(file);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(stream, null);
			int type;			
			do {
				type = parser.next();
				if (type == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					if ("g".equals(tag)) {
						int id = Integer.parseInt(parser.getAttributeValue(null,"id"), 16);
						if (id >= mNextPluginWidgetId) {
							mNextPluginWidgetId = id + 1;
						}
						String key = parser.getAttributeValue(null, "key");
						PluginWidgetId pluginWidgetId = new PluginWidgetId(id, key);
						mPluginWidgetIds.add(pluginWidgetId);						
					}
				}
			} while (type != XmlPullParser.END_DOCUMENT);
			success = true;			
		} catch (NullPointerException e) {
            Log.w(TAG, "failed parsing " + file, e);
        } catch (NumberFormatException e) {
            Log.w(TAG, "failed parsing " + file, e);
        } catch (XmlPullParserException e) {
        	Log.w(TAG, "failed parsing " + file, e);
        } catch (IOException e) {
        	Log.w(TAG, "failed parsing " + file, e);
        } catch (IndexOutOfBoundsException e) {
        	Log.w(TAG, "failed parsing " + file, e);
        }
        
        try {
        	if (stream != null) {
        		stream.close();
        	}
        } catch (IOException e) {
        	// Ignore
        }

	}
	
	/**
	 * Write plugin widget state to a local file.
	 * 
	 * @param file The local file.
	 * @return File writing result.
	 */
	private boolean writeStateToFileLocked(File file) {
        FileOutputStream stream = null;
        int N;

        try {
            stream = new FileOutputStream(file, false);
            XmlSerializer out =Xml.newSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, true);
            out.startTag(null, "gs");

            N = mPluginWidgetIds.size();
            for (int i=0; i<N; i++) {
                PluginWidgetId id = mPluginWidgetIds.get(i);
                out.startTag(null, "g");
                out.attribute(null, "id", Integer.toHexString(id.mId));
                out.attribute(null, "key", id.mPackageName);
                out.endTag(null, "g");
            }

            out.endTag(null, "gs");
            out.endDocument();
            stream.close();
            return true;
        } catch (IOException e) {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            return false;
        }
        
       
    }
	
	/**
	 * Look up plugin widget id.
	 * 
	 * @param pluginWidgetId Plugin widget id.
	 * @return Plugin widget id which has been found. 
	 */
	private PluginWidgetId lookupPluginWidgetIdLocked(int pluginWidgetId) {
        final int N = mPluginWidgetIds.size();
        for (int i=0; i<N; i++) {
        	PluginWidgetId id = mPluginWidgetIds.get(i);
            if (id.mId == pluginWidgetId ) {
                return id;
            }
        }
        return null;
    }

	/**
	 * Delete plugin widget id from array list. 
	 * 
	 * @param id Plugin widget id.
	 */
	private void deleteAppWidgetLocked(PluginWidgetId id) {
		mPluginWidgetIds.remove(id);
		/* 
		 * Some thing need to be finished to update the app
		 * It will be finished 
		 */
	}
            
	/**
	 * Remove the plugin widget id.
	 * 
	 * @param pluginWidgetId Plugin widget id which has been allocated before.
	 */
	public void deletePluginWidgetId(int pluginWidgetId) {
		synchronized (mPluginWidgetIds) {
			PluginWidgetId id = lookupPluginWidgetIdLocked(pluginWidgetId);
	        if (id != null) {
	        	deleteAppWidgetLocked(id);
	            saveStateLocked();
	        }
	    }
	} 	
	
	/**
	 * Generate an unique id for the plugin widget.
	 * 
	 * @param name Plugin widget package name.
	 * @return Plugin widget id.
	 */
	public int allocatePluginWidgetId(String name) {
		synchronized (mPluginWidgetIds) {
			if(name == null || "".equals(name)){
				return -1;
			}
			int id = mNextPluginWidgetId++;
			PluginWidgetId pluginWidgetId = new PluginWidgetId(id, name);
			mPluginWidgetIds.add(pluginWidgetId);
			saveStateLocked();
			return id; 	
		}		
	}
	
	/**
	 * Get plugin widget package name from plugin widget id.
	 * 
	 * @param id Plugin widget id.
	 * @return Plugin widget package name.
	 */
	public String getPluginWidgetKey(int id) {
		PluginWidgetId pluginWidgetId = lookupPluginWidgetIdLocked(id);
		if (pluginWidgetId == null){
			return null;
		}
		return pluginWidgetId.mPackageName.toString();
	}
	
	/**
	 * Get plugin widget ids.
	 * 
	 * @return Plugin widget id array list.
	 */
	public ArrayList<PluginWidgetId> getPluginWidgetIds() {
		return mPluginWidgetIds;
	}
	
	/**
	 * Plugin widget id class including plugin widget id and plugin widget package name.
	 */
	public class PluginWidgetId {
		public int mId;
		public String mPackageName;
		 
		PluginWidgetId(int id, String name) {
			mId = id;
			mPackageName = name;
		}
		
		public int getId(){
			return mId;
		}
		
		public String getPackageName(){
			return mPackageName;
		}
	}

}
