/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.quicknote;

import com.motorola.quicknote.QNAppWidgetConfigure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Displays a list of all activities matching the incoming
 * {@link Intent#EXTRA_INTENT} query, along with any injected items.
 */
public class QNWelcome extends Activity {

	private static int REQCODE_WIDGET;
	static private String TAG = "QNWelcome";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	Intent intentActivity = new Intent();
	Intent intent = getIntent();
	
	String from = intent.getStringExtra("from");
        if ((from != null) && (from.equals("widget"))) {          
           String uri_string = intent.getStringExtra("contentUri");
           int widgetId = intent.getIntExtra("widgetId",-1);

           if (null == uri_string) {
              Log.i(TAG, "onCreate: uri_string = null, Error case!");
              finish();
          }

           if ("Special_widget".equals(uri_string)) {          
               //it is a special widget to create note        	   
        	   widgetId = intent.getIntExtra("widgetId",-1);
               if (widgetId != -1) {
                  QNAppWidgetConfigure.saveWidget2Update(this, widgetId);
               }
                               	   
        	   if(QNUtil.isTextLoad(this)) {               	
        		   QNDev.log(TAG+" from Special_widget->isTextLoad");
               		startNoteView();
               } else {              	
            	   QNDev.log(TAG+" from Special_widget->QNNewActivity");
                   intentActivity.setClass(this, QNNewActivity.class);
                   startActivity(intentActivity);
               }                                                
               finish();
               return;
           } else {
               //it is a normail widget
              if (widgetId != -1) {                
                QNAppWidgetConfigure.saveWidget2Update(this, widgetId);
              }
           }
        }else{
        	 //it is not from widget, so just set widget2update = -1
               QNDev.log(TAG+" from other entrance-> quickote, update the configure...");
               QNAppWidgetConfigure.saveWidget2Update(this, -1);
        }
        

        if (QuickNotesDB.getSize(this) <= 0) {        	
        	QNDev.log(TAG+" (QuickNotesDB.getSize(this) <= 0)");
            if(QNUtil.isTextLoad(this)) {            	
                startNoteView();
            } else {           	
                intentActivity.setClass(this, QNNewActivity.class);
                startActivity(intentActivity);
            }
        } else if (getIntent().hasExtra("contentUri")|| getIntent().hasExtra("index")) {        	
            intentActivity.setClass(this, QNNoteView.class);
            intentActivity.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            intentActivity.putExtras(intent.getExtras());
            startActivity(intentActivity);
        } else {        	
            intentActivity.setClass(this, QNDisplayNotes.class);
            intentActivity.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intentActivity);
        }
        finish();
	}

    private void startNoteView(){
    	 boolean preCondition = false;
         if (!QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
             // text note store on phone memory
             preCondition = true;
         } else {
             preCondition = QNUtil.checkStorageCard(this);
         }

         if (preCondition ) {
             QNUtil.initDirs(this);
             Intent intentNoteNew = new Intent(QNConstants.INTENT_ACTION_NEW)
             .setClassName(QNConstants.PACKAGE, QNConstants.PACKAGE + ".QNNoteView")
             .setDataAndType(Uri.parse("qntext:"), QNConstants.MIME_TYPE_TEXT);                
             
             if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                 Uri uriFilePath = QNUtil.prepareTextNote();
                 if (uriFilePath == null) {                         
                     finish();
                     return;
                 }
                 intentNoteNew.setDataAndType(uriFilePath, QNConstants.MIME_TYPE_TEXT);
             }
            
             intentNoteNew.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
             startActivity(intentNoteNew);
         }
    }
}
