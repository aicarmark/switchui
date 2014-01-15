//MOTO Dialer Code IKHSS6-3559 - Start
package com.motorola.contacts.activities;

import com.android.contacts.activities.DialtactsActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.contacts.R;

public class VoicemailShortcut extends Activity {
    private static final String TAG = "VoicemailShortcut";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if(DBG) log("On Create called !!! ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DBG) log("On Resume called !!! ");
        // This should do the basic handling of creating an intent,
        // adding extras, and assigning an icon
        Intent shortcutIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
            Uri.fromParts("voicemail", "", null));
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.voicemail));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher_voicemail));  // MOT Calling Code - IKMAIN-20475
        setResult(RESULT_OK, intent);
        finish();
    }

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
}
//MOTO Dialer Code IKHSS6-3559 - End