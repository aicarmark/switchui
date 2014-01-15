package com.motorola.contacts.activities;

import com.android.contacts.activities.DialtactsActivity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.contacts.R;
import com.android.contacts.spd.Utils;

public class CreateDialerShortcut extends ListActivity {
    private static final String TAG = "CreateDialerShortcut";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if(DBG) log("On Create called !!! ");
        // Create an array of Strings, that will be put to our ListActivity
        // MOT Calling Code - IKSTABLEFIVE-6467
        String[] mStrings;
        boolean mExcludeVoicemail = getResources().getBoolean(R.bool.ftr_36927_exclude_voicemail);
        if(Utils.isVvmAvailable(this) || mExcludeVoicemail){
            mStrings = new String[]{getString(R.string.shortcutRecCallsList),
                getString(R.string.shortcutDialer)};
        }else {
            mStrings = new String[]{getString(R.string.shortcutRecCallsList),
                getString(R.string.shortcutDialer), getString(R.string.voicemail)};
        }
        // MOT Calling Code - IKSTABLEFIVE-6467 - End

        // Create an ArrayAdapter, that will actually make the Strings above appear in the ListView
        this.setListAdapter(new ArrayAdapter<String>(this,
                         android.R.layout.simple_list_item_1, mStrings));

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DBG) log("On Resume called !!! ");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id){
        super.onListItemClick(l, v, position, id);

        if(CDBG) log("onListItemClick called !!! "+ position);

        final boolean isRestoreAfterWipe = false;

        // based on item selected we formulate specific Intent
        // and load them with specific Extras
        switch (position){

            case 0:
            {
                // This should do the basic handling of creating an intent,
                // adding extras, and assigning an icon
                Intent shortcutIntent = new Intent("android.intent.action.VIEW");
                shortcutIntent.setType("vnd.android.cursor.dir/calls");
                // We DON'T want to restore contact shortcuts after a device is wiped
                /* to-pass-build, Xinyu Liu/dcjf34 */ 
                shortcutIntent.putExtra("com.motorola.blur.home.EXTRA_NO_RESTORE",
                    !isRestoreAfterWipe);

                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcutRecCallsList));
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(this,
                        R.drawable.ic_launcher_recent_calls));
                setResult(RESULT_OK, intent);
                finish();
                if(CDBG) log("onListItemClick finished !!! "+ position);
                break;
            }
            case 1:
            {
                Intent shortcutIntent = new Intent("android.intent.action.DIAL", Uri.parse("tel:"));
                // We DON'T want to restore contact shortcuts after a device is wiped
                /* to-pass-build, Xinyu Liu/dcjf34 */ 
                shortcutIntent.putExtra("com.motorola.blur.home.EXTRA_NO_RESTORE",
                    !isRestoreAfterWipe);

                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcutDialer));

                // IKSTABLETWO-6372 Change icon
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(this,
                        R.drawable.ic_launcher_dialpad));
                setResult(RESULT_OK, intent);
                finish();
                if(CDBG) log("onListItemClick finished !!! "+ position);
                break;
            }
            // MOT Calling Code - IKSTABLEFIVE-6467
            case 2:
            {
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
                if(CDBG) log("onListItemClick finished !!! "+ position);
                break;
            }
            // MOT Calling Code - IKSTABLEFIVE-6467 - End
            default:
                Log.w(TAG, "onListItemClick seems unhandled item, No action !!! "+ position);
                break;
        }
    }

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
}

