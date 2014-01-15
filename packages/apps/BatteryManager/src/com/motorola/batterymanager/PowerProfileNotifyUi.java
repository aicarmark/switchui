/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: PowerProfileNotifyUi.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-15-10       A24178       Created file
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Activity;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;

import com.motorola.batterymanager.R;

public class PowerProfileNotifyUi extends Activity implements
                                            DialogInterface.OnClickListener,
                                            DialogInterface.OnDismissListener {

    private final static int NOTICE_DIALOG_ID = 1;

    @Override
    public void onCreate(Bundle saveState) {
        super.onCreate(saveState);

        showDialog(NOTICE_DIALOG_ID);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog = null;
        if(id == NOTICE_DIALOG_ID) { 
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.battery_manager_setting);
            builder.setMessage(R.string.pwrup_notice_desc);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(R.string.configure_text, this);
            dialog = builder.create();
            dialog.setOnDismissListener(this);
        }
        return dialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            // User is OK with setting, dismiss dialog and go away
            dismissDialog(NOTICE_DIALOG_ID);
            finish();
        }else if(which == DialogInterface.BUTTON_NEGATIVE) {
            // User wants to change profile, launch settings
            dismissDialog(NOTICE_DIALOG_ID);
            Intent actIntent = new Intent(this, BatteryProfileUi.class);
            startActivity(actIntent);
            finish();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if(!isFinishing()) {
            finish();
        }
    }

}

