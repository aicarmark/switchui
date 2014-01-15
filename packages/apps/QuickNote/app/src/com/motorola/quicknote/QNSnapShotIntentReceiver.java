package com.motorola.quicknote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class QNSnapShotIntentReceiver extends BroadcastReceiver {
	private static final String _TAG = "QNSnapShotIntentReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		File fnote = null;

        if(!QNUtil.is_storage_mounted(context)){
            Toast.makeText(context, R.string.no_sd_snapshot,
                    Toast.LENGTH_LONG).show();
            return;
        } else if (QNUtil.available_storage_space(context) <= QNConstants.MIN_STORAGE_REQUIRED) {
            Toast.makeText(context, R.string.not_enough_free_space_sd,
                    Toast.LENGTH_LONG).show();
            return;
        }

        QNUtil.initDirs(context);
        try{
            fnote = QNUtil.fScreenshot(context, QNConstants.SNAPSHOT_DIRECTORY);
        } catch (Exception e) {
            QNDev.qnAssert(false); // unexpected!
        }


        Intent snapshot = new Intent(QNConstants.INTENT_ACTION_SCREENSHOT);
        snapshot.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        snapshot.setClassName("com.motorola.quicknote", QNSnapShot.class.getName());
        snapshot.setDataAndType(Uri.parse("file://" + fnote.getAbsolutePath()), "image/jpeg");
        context.startActivity(snapshot);
	}

}
