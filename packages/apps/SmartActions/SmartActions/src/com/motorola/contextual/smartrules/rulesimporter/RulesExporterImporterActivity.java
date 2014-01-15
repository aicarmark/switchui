/*
 * @(#)RulesExporterImporterActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/01/18 NA				  Initial version
 * A18172        2011/02/23 NA				  Updated with review comments
 * 											  from Craig
 *
 */

package  com.motorola.contextual.smartrules.rulesimporter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;

public class RulesExporterImporterActivity extends Activity implements
    Constants {

    private RadioGroup rgMode = null;
    private Button bOk = null;

    Intent incomingIntent = null;

    private final static String TAG = RulesExporterImporterActivity.class.
                                      getSimpleName();


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        if(LOG_DEBUG) Log.d(TAG, "in onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.rulesexporterimporter);

        rgMode = (RadioGroup)findViewById(R.id.Mode);
        bOk = (Button)findViewById(R.id.bOK);

        bOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RadioButton rbChecked = (RadioButton)findViewById(
                                            rgMode.getCheckedRadioButtonId());
                userSelectedMode(rbChecked.getText().toString());
                finish();
            }
        });
    }

    /** This method invokes the respective modules based on
     * the Mode selected by the user. Details are explained
     * below
     *
     * @param currentMode
     */
    @SuppressWarnings("unused")
    private void userSelectedMode(String currentMode) {

        String newName = "", newDescription = "";
        if (currentMode.equals(getString(R.string.export_rules))) {
            if (LOG_DEBUG) Log.d(TAG,"Export Rules");
            // Will invoke the Rules exporter to dump the Smart Rules
            // Database into an xml into the SDCARD
            Intent intent = new Intent(LAUNCH_RULES_EXPORTER);
            intent.putExtra(EXTRA_IS_EXPORT_TO_SDCARD, true);
            this.sendBroadcast(intent);
        } else if (currentMode.equals(getString(R.string.import_rules))) {
            if (LOG_DEBUG) Log.d(TAG,"Import Rules");
            // Will invoke the Rules Importer to parse the XML from the SD Card
            // and to insert the parsed data into the Smart Rules database
            Intent intent = new Intent(LAUNCH_CANNED_RULES);
            intent.putExtra(IMPORT_TYPE, XmlConstants.ImportType.TEST_IMPORT);
            this.sendBroadcast(intent);
        }

        setResult(RESULT_OK);
    }
}
