/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date           CR                Author       Description
 *  2010-03-23     IKSHADOW-2074     E12758       initial
 */
package com.motorola.filemanager.samba;

import jcifs.smb.NtlmPasswordAuthentication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.samba.service.SambaTransferHandler;

public class SambaReceiver extends BroadcastReceiver {
    private String EXTRA_AUTH = "AUTHINFO";

    @Override
    public void onReceive(Context context, Intent intent) {
	SambaExplorer.log("SambaReceiver onReceive" + intent);
	String action = intent.getAction();
	if ((action != null)
		&& (action.equals(DownloadServer.ACTION_FILE_PICKER))) {
	    SambaExplorer.log("SambaReceiver ACTION_FILE_PICKER");
	    startFilePickerMode(context, intent);
	} else {
	    SambaExplorer.log("SambaReceiver undefined intent");
	}
    }

    private void startFilePickerMode(Context context, Intent intent) {
	Bundle extras = intent.getExtras();
	String mAuthInfo = extras.getString(EXTRA_AUTH);
	if (mAuthInfo != null) {
	    SambaTransferHandler.provideLoginCredentials(null,
		    SambaDeviceList.getHostAccount(mAuthInfo),
		    SambaDeviceList.getHostPwd(mAuthInfo));
	} else {
	    SambaTransferHandler
		    .setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
	}
	SambaExplorer.log("startService startFilePickerMode");
	// args.putInt(DownloadServer.DOWNLOAD_MODE,
	// DownloadServer.SAMBA_FILE_PICKER);

	context.startService(new Intent(context, DownloadServer.class)
		.putExtras(extras));
    }
}
