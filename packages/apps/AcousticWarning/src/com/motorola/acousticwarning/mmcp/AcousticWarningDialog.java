/*
 * Copyright (C) 2012 Motorola, Inc.
 * All Rights Reserved
 *
 */

package com.motorola.acousticwarning.mmcp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class AcousticWarningDialog extends Activity {

    private static final boolean DEBUG = true;
    private static final String LOG_TAG = "AcousticWarning";
    private AlertDialog alertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        if (DEBUG) Log.d(LOG_TAG,"onCreate Enter Dialog");

        alertDialog = createAlertDialog();
        alertDialog.show();
    }

    protected AlertDialog createAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.warning_text)
               .setIcon(R.drawable.ic_dialog_warning)
               .setTitle(R.string.title)
               .setCancelable(false)
               // BEGIN IKCBS-2624, 05/12/2011, a20746
               .setOnKeyListener(new DialogInterface.OnKeyListener() {
                   @Override
                   public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                       switch(keyCode){

                       case KeyEvent.KEYCODE_BACK:
                       case KeyEvent.KEYCODE_SEARCH:
                       case KeyEvent.KEYCODE_MENU:
                            return true;
                       default: return false;
                       }
                   }
               })
               // END IKCBS-2624
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       if (DEBUG) Log.d(LOG_TAG,"User acknowledge. Start timer");
                       getApplicationContext().sendBroadcast(
                                               new Intent("com.motorola.acousticwarning.START_TIMER"));
                       finish();
                   }
               });

        return builder.create();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(LOG_TAG,"Dialog dismissed");
        alertDialog.dismiss();
    }

}
