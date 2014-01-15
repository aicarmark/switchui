/*
 * @(#)SmartActionsBackupRestoreAgent
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2012/03/12 IKHSS7-12449      Initial version
 */

package com.motorola.contextual.smartrules.rulesimporter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;


/** This class implements the Backup Manager Call Back function which will 
 * help to Backup and Restore the data.
 *
 *<code><pre>
 * CLASS:
 *
 * RESPONSIBILITIES:
 *  a. Restores the application data 
 *  b. Backs up the application data
 *
 * COLLABORATORS:
 *  None
 *
 * USAGE:
 *  see each method
 *
 *</pre></code>
 */

public class SmartActionsBackupRestoreAgent extends BackupAgent  implements Constants {
	
	public static final String TAG = SmartActionsBackupRestoreAgent.class.getSimpleName();
	
    // Key to which the data is associated.
    static final String HEADER = "com.contextual.smartrules.backupmyrules";
   
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
             ParcelFileDescriptor newState) throws IOException {
    	     if (LOG_INFO) Log.i(TAG, "onBackup Called");
    	     
    	     // Get the XML for User, Suggestions which are not accepted.
             String xmlStringToServer = new RulesExporter().
             				startRulesExporter(this, null, null, null, null);
                 	     
             if (LOG_DEBUG) Log.d(TAG, "onBackup  Data : "+xmlStringToServer);
            	 
             if (xmlStringToServer != null) {
            	 try {
                     byte[] stringBytes = xmlStringToServer.getBytes("UTF8");
                     if (LOG_INFO) Log.i(TAG, "onBackup  Data Size : "+stringBytes.length);
                     data.writeEntityHeader(HEADER, stringBytes.length);
                     data.writeEntityData(stringBytes, stringBytes.length);
            	 } catch (UnsupportedEncodingException e){
                     Log.e(TAG, "UTF8 Encoding", e);
            	 }
             }else{ 
            	// If we set the size like -1, then onRestore itself won't
            	// be called during restore. This helps to tackle with the usecases
                // if there are no Rules to be backed up. Ex: ClearData
               if (LOG_INFO) Log.d(TAG, "Data in the server cleared !!! ");
            	data.writeEntityHeader(HEADER, -1);
             }
    }

   
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
    		if (LOG_INFO) Log.i(TAG, "onRestore Called");

    		// Read the data 
            while (data.readNextHeader()) {
                String key = data.getKey();
                int dataSize = data.getDataSize();
                
                if (LOG_INFO) Log.d(TAG,"Data size"+dataSize);

                if (key.equals(HEADER)) {
                	
                	if (LOG_INFO) Log.i(TAG, "Got the data from Cloud");
                	
                    // Get the data 
                    byte[] buffer = new byte[dataSize];
                    // reads the entire entity at once
                    data.readEntityData(buffer, 0, dataSize); 
                    
                    try {
                    	String xml = new String(buffer, "UTF8");
                    	 
                    	if (LOG_DEBUG) Log.d(TAG,"XML from cloud : "+xml);
                    	
                    	// Notify Rules Importer to import the restored rules
                        Intent intent = new Intent(LAUNCH_CANNED_RULES);
                        intent.putExtra(IMPORT_TYPE, XmlConstants.ImportType.RESTORE_MYRULES);
                        FileUtil.writeToInternalStorage(this,xml,
														MYRULES_RESTORED_FILENAME);
                        this.sendBroadcast(intent);   
                    }catch (UnsupportedEncodingException e){
                        Log.e(TAG, "UTF8 Encoding", e);
                    }             
                } else  {
                    // a key we recognize but wish to discard
                	if (LOG_INFO) Log.i(TAG,"Skip Data");
                    data.skipEntityData();
                } // ... etc.
           }
    } 
}
