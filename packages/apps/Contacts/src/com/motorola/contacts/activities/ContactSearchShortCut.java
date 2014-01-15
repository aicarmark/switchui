package com.motorola.contacts.activities;



import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.contacts.R;

public class ContactSearchShortCut extends Activity {
    private static final String TAG = "ContactSearchShortCut";


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This should do the basic handling of creating an intent,
        // adding extras, and assigning an icon
        Intent shortcutIntent = new Intent(Intent.ACTION_SEARCH);
        shortcutIntent.setType(ContactsContract.Contacts.CONTENT_TYPE );
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK  );


        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.search_contacts));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher_contact_search));
        setResult(RESULT_OK, intent);
        finish();
    }

}
