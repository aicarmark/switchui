/*
 * @(#)JarFileService.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * w30219        04/04/2012 IKHSS7-19234	  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.util.Util;

/** This is a IntentService class for loading jar files from assets folder into
 *   /data/data/com.motorola.contextual.smartrules/files/ folder.
 *   It also sends out a broadcast message to SmartActionFW to load classes from
 *   the jar file
 *
 *<code><pre>
 * CLASS:
 * 	extends IntentService
 *
 *  implements
 *   Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	copies jar files from assets folder into/data/data/com.motorola.contextual.smartrules/files/
 *  informs SmartActionFW to load classes from the jar file(s)
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class JarFileService extends IntentService implements Constants {

	private static final String TAG = JarFileService.class.getSimpleName();
	private static final Map<String, String> jarFileClassMap =new HashMap<String, String>();
	static {
		jarFileClassMap.put("DataClearJar.xml", "com.motorola.contextual.service.dataclearlistener.DataClearListenerService");
	}
			
			 
	private static final String LAUNCH_PLUGIN_SERVER = "com.motorola.contextual.launch.PLUGIN_SERVER";
	private static final String PLUGIN_SERVER_SERVICE_TYPE = PACKAGE + ".PLUGIN_SERVER_SERVICE_TYPE";
	private static final String EXTRA_XML_FILE_PATH = "com.motorola.contextual.intent.extra.XML_FILE_PATH";
	private static final String DATA_PATH = "/data/data/" + PACKAGE + "/files/";
	 
	private Context mContext = null;
	
	/** default constructor
	 * 
	 */
	public JarFileService() {
		super(TAG);
	}

	/** Constructor
	 * 
	 * @param name
	 */
	public JarFileService(String name) {
		super(name);
	}

	/*
	 * * onHandleIntent()
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		
		if(intent == null || intent.getStringExtra(EXTRA_ACTIONNAME) == null) {
            Log.e(TAG, "inten is null");
            return;
		}
		
		mContext = this;
		
		if (LOG_INFO) Log.i(TAG, "onHandleIntent Intent is " + intent.toUri(0));	
		String action = intent.getStringExtra(EXTRA_ACTIONNAME);
	
        if (action.equals(EXTRA_ACTION_ONLOAD)) {
        	String loadedclass = intent.getStringExtra(EXTRA_CLASSNAME);
        	if (jarFileClassMap.containsValue(loadedclass)) {
        		if (LOG_DEBUG) Log.d(TAG, "Loaded class; Update Shared Pref " + loadedclass);
        		 Util.setSharedPrefStateValue(mContext, loadedclass, TAG, true);
        	}
        } else if (action.equals(EXTRA_ACTION_LOADFILE)) {
	    	//Make sure folder exists and permissions are set
	    	File dataFolder = new File(DATA_PATH);
	    	if(!dataFolder.exists()) {
	    		dataFolder.mkdir();
	    	}
			dataFolder.setReadable(true, false);
			dataFolder.setExecutable(true, false);
	    	
	    	//Read jarfile(s) from assets folder
			Set<String> jarfileList = jarFileClassMap.keySet();
			
	    	for (String filename : jarfileList) {
	    		//See if the class is already loaded
	    		String classToLoad = jarFileClassMap.get(filename);
	    		if (!Util.getSharedPrefValue(mContext, classToLoad, TAG)) {
	    			if (LOG_INFO) Log.i(TAG, classToLoad + " is not loaded yet");
		    		File outfile = new File(DATA_PATH + filename);
		    		if (!outfile.exists()) { 			
		    			if (LOG_DEBUG) Log.d(TAG, "Copying from assets : " + filename);
		    			copyFileFromAssets(filename);
		    		}
					//Send Broadcast
					Intent launchIntent = new Intent(LAUNCH_PLUGIN_SERVER);
					launchIntent.putExtra(PLUGIN_SERVER_SERVICE_TYPE, 3);
					if (LOG_DEBUG) Log.d(TAG, "Intent path is  : " + DATA_PATH + filename);
					launchIntent.putExtra(EXTRA_XML_FILE_PATH, DATA_PATH + filename);
					if (LOG_DEBUG) Log.d(TAG, "Intent is  : " + launchIntent.toUri(0));
					mContext.sendBroadcast(launchIntent);
	    		}
	    	}
        }      
	}	
	
	private void copyFileFromAssets(String filename) {
		if (LOG_DEBUG) Log.d(TAG,"Fn: copyFileFromAssets : filename is " + filename);
		 // Read the file
        InputStream in = null;
        OutputStream out = null;  
        AssetManager assetManager = mContext.getAssets();

        try {
            in = assetManager.open(filename);
            out = new FileOutputStream(DATA_PATH + filename);  
            Util.copyFile(in, out);  
            out.flush();  
            
            //Set permissions
            File outFile = new File(DATA_PATH + filename);
            outFile.setReadable(true, false);
           
        } catch(Exception e) {  
        	e.printStackTrace();
        } finally {
        	if (in != null){
        		try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  
                in = null;  
        	}
        	
        	if (out != null){
        		try {
        			out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  
        		out = null;  
        	}
        }
	}  
	
}