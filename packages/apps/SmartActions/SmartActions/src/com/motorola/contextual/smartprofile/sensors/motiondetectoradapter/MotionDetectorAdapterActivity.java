/*
 * @(#)MotionDetectorAdapterActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2010/11/26 NA                Initial version
 * a18491        2011/2/18  NA                Incorporated first set of
 *                                            review comments.
 */
package  com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.motorola.contextual.smartprofile.Constants;


/*
 * Motion constants
 */
interface MotionConstants {

    String STILL = "Motion=Still;Version=1.0";

    String MOTION_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.motiondetectoradapter.MotionDetectorAdapterStateMonitor";

    String MOTION_PERSISTENCE = "MotionConfig";

    String MOTION_PUB_KEY = "com.motorola.contextual.Motion";

    String NEW_MOTION_CONFIG_PREFIX = "Motion=";
}


/**
 * This class displays options for Motion detector precondition and allows the user to chose one
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends Activity
 *     Implements MotionConstants, Constants
 *
 * RESPONSIBILITIES:
 *      This class displays options for Motion detector precondition and allows the user to chose one
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     None
 *
 * </PRE></CODE>
 */
public final class MotionDetectorAdapterActivity extends Activity implements Constants, MotionConstants  {

    private final static String TAG = MotionDetectorAdapterActivity.class.getSimpleName();
    public static final int START_APP = 1;
    private final static String MOTION_PACKAGE = "com.motorola.contextual.Motion";
    private final static String MOTION_ACTIVITY = "com.motorola.contextual.Motion.MotionTrigger";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent pickIntent = new Intent();
        pickIntent.setComponent(new ComponentName(MOTION_PACKAGE, MOTION_ACTIVITY));

        startActivityForResult(pickIntent, START_APP);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_APP) {
            if (data != null) {
                Intent returnIntent = new Intent();

                returnIntent.putExtra(EXTRA_CONFIG, STILL);
                returnIntent.putExtra(EXTRA_DESCRIPTION, data.getStringExtra(EVENT_DESC));
                setResult(RESULT_OK, returnIntent);

                if(LOG_INFO) Log.i(TAG, "config "+ STILL + " : " + data.getStringExtra(EVENT_DESC));
                setResult(RESULT_OK, returnIntent);
            }
            finish();
        }
    }
}
