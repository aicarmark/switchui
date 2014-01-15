/*
 * @(#)LaunchAppActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * This class allows the user to select the app to be launched as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select the app.
 *     Sends the intent containing the selected app to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class LaunchAppActivity extends Activity implements Constants {

    public static final String TAG = TAG_PREFIX + LaunchAppActivity.class.getSimpleName();
    public static final int PICK_APP = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        Intent filterIntent = new Intent(Intent.ACTION_MAIN, null);
        filterIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        pickIntent.putExtra(Intent.EXTRA_INTENT, filterIntent);
        pickIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.launch_an_application));
        startActivityForResult(pickIntent, PICK_APP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_APP) {
            if (data != null) {
                PackageManager pm = this.getPackageManager();
                String label = "";
                List<ResolveInfo> list = pm.queryIntentActivities(data, 0);
                if (list != null && list.size() > 0) {
                    ResolveInfo resolveInfo = list.get(0);
                    CharSequence l = resolveInfo.loadLabel(pm);
                    label = l.toString();
                    if (LOG_INFO) Log.i(TAG, "Application label is " + label);
                }
                Intent intent = new Intent();
                intent.putExtra(EXTRA_CONFIG, LaunchApp.getConfig(null, data.getComponent().flattenToString()));
                intent.putExtra(EXTRA_DESCRIPTION, label);
                intent.putExtra(EXTRA_RULE_ENDS, false);
                setResult(RESULT_OK, intent);
            }
            finish();
        }
    }
}
