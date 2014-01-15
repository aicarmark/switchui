// ********************************************************** //
// PROJECT:     APR (Automatic Panic Recording)
// DESCRIPTION: 
//   The purpose of APR is to gather the panics in the device
//   and record statics about those panics to a centralized
//   server, for automated tracking of the quality of a program.
//   To achieve this, several types of messages are required to
//   be sent at particular intervals.  This package is responsible
//   for sending the data in the correct format, at the right 
//   intervals.
// ********************************************************** //
// Change History
// ********************************************************** //
// Author         Date       Tracking  Description
// ************** ********** ********  ********************** //
// Stephen Dickey 03/01/2009 1.0       Initial Version
//
// ********************************************************** //
package com.motorola.motoapr.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 */
public class APRLaunchActivity extends Activity {

	public APRLaunchActivity() {
    }

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	// to debug this, enable the next line.
    	// android.os.Debug.waitForDebugger();
        
        // launch APR.
        startActivity(new Intent(this, APRActivity.class ));
        
        // this finishes off the Welcome Activity.  
        finish();
    }
}

