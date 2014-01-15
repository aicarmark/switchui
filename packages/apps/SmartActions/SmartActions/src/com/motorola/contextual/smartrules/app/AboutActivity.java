package com.motorola.contextual.smartrules.app;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.util.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends Activity implements Constants, DbSyntax {
	
	private static final String TAG = AboutActivity.class.getSimpleName();
	
	private Context mContext = null;
	private int numOfVisibleRules = 0;
	
    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        mContext = this;
        
        if(getIntent() != null)
        	numOfVisibleRules = 
        		getIntent().getIntExtra(AddRuleListActivity.NUM_OF_VISIBLE_RULES, 0);
        
        if(LOG_VERBOSE) Log.v(TAG, "in onCreate");
        ActionBar ab = getActionBar();
 		ab.hide();
 		
        LinearLayout versionWrapper = (LinearLayout) findViewById(R.id.about_version_wrapper);
        versionWrapper.setVisibility(View.VISIBLE);
        
		String[] packageDetails = Util.getPackageDetails(mContext);
		String version = getString(R.string.about_version).concat(BLANK_SPC).concat(packageDetails[0]);
		TextView aboutVersion = (TextView) findViewById(R.id.about_version_num);
		aboutVersion.setText(version);
        
		Button learnMoreButton = (Button) findViewById(R.id.learn_more_button);	
        learnMoreButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Util.showHelp(mContext);
			}        	
        });

		Button getStartedButton = (Button) findViewById(R.id.get_started_button);	
		getStartedButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startAddRuleListActivity();
			}        	
        });       
    }
    
    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_VERBOSE) Log.v(TAG, "In onDestroy()");
        super.onDestroy();
    }

    /** starts the Add Rule activity.
     */
    private void startAddRuleListActivity() {
    	Intent ruleIntent = new Intent(mContext, AddRuleListActivity.class);
    	ruleIntent.putExtra(AddRuleListActivity.NUM_OF_VISIBLE_RULES, numOfVisibleRules);
        startActivity(ruleIntent);
    }
}