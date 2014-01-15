/*
 * @(#)DisplayConditionsActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385      2011/01/27    NA				  Initial version
 *
 */

package com.motorola.contextual.smartrules.rulesbuilder;

import java.net.URISyntaxException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent.ILocationPicker;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.IRulesBuilderPublisher;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.uipublisher.PublisherList;
import com.motorola.contextual.smartrules.util.Util;

/** This class displays list of Conditions. The user selects triggers from this list to build a 
 * rule.
 *
 * CLASS:
 *    Extends ListActivity
 *    Implements Constants
 *    Implements RulesBuilderConstants
 *    Implements ILocationPicker
 * 
 * RESPONSIBILITIES:
 * 
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	see methods for usage instructions
 *
 */
public class DisplayConditionsActivity extends ListActivity implements Constants,
																	   RulesBuilderConstants,
																	   ILocationPicker{

    private static final String TAG = DisplayConditionsActivity.class.getSimpleName();
    private static String mLastPublisherSelected;
    private static final int PROCESS_CONDITION_LIST_ITEM = 100;
    private boolean mActivityCurrentlyVisible = true;
    private Context mContext = null;
    private Intent 	mIntentForDialog = null;
    private static ConditionPublisherList mCondPubList;
	@SuppressWarnings("unused")
	private static IRulesBuilderPublisher pubCallback;
    private static PublisherList mGreyCondPubList;
    private static String[] mKeys;

    
    public void onCreate(Bundle savedInstanceState) {

        if(LOG_DEBUG) Log.d(TAG, "In onCreate()");
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.rb_display_actions_list);

        ActionBar ab = getActionBar();
        ab.setTitle(R.string.triggers);
        ab.setDisplayHomeAsUpEnabled(true);
 		ab.show();

        TextView prompt = (TextView) findViewById(R.id.prompt);
        prompt.setText(Html.fromHtml(getString(R.string.triggers_prompt)));

        if (mCondPubList != null && mCondPubList.size() > 0)
        	showConditionsList();
        else
        	showErrorMessage(getString(R.string.no_avail_triggers));
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_DEBUG) Log.d(TAG, "In onDestroy()");
        super.onDestroy();
    }
    
    /** onResume()
     */
    @Override
    public void onResume() {
        if(LOG_DEBUG) Log.d(TAG, "In onResume");
        super.onResume();
        
        if (!this.mActivityCurrentlyVisible){
	        if ((LocationConsent.getSecuritySettingsScreenLaunchedFlag()) )
	        	LocationConsent.onBackPressInSecuritySettings(mContext, null);
        }
        this.mActivityCurrentlyVisible = true;
    }
  
    /** onPause()
     */
    @Override
    public void onPause(){
    	if(LOG_DEBUG) Log.d(TAG, "In onPause");
    	  this.mActivityCurrentlyVisible = false;
          super.onPause();
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	} 
	
    /** Displays an error message if the cursor was null or empty (returned from Smart Rules DB provider)
    *
    * @param errorMsg - Message to be displayed to the user.
    */
   private void showErrorMessage(String errorMsg) {

       RelativeLayout errRl  	= null;
       TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
       if(errTextView != null) {
           errRl = (RelativeLayout) errTextView.getParent();
           errRl.setVisibility(View.VISIBLE);
           errTextView.setText(errorMsg);
       }
   }
   
    /** Displays all available conditions*/
    private void showConditionsList() {
        ListView lv= (ListView)findViewById(android.R.id.list);
        lv.setAdapter(new ConditionAdapter(this, mCondPubList));
    }
    
    private static synchronized void updatePublisherSelected(String  publisher) {
    	mLastPublisherSelected = publisher;
    }

    public static class ConditionAdapter extends BaseAdapter {
        private Context mContext;

        public ConditionAdapter(Context c, ConditionPublisherList condList) {
            mContext = c;
        }
        
        public int getCount() {
           return mCondPubList.size();
        }
        
        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final LinearLayout listRow = (LinearLayout) ((Activity) mContext).getLayoutInflater().inflate(R.layout.rb_list_row, null);
            final TextView nameField = (TextView) listRow.findViewById(R.id.list_text);
            final ImageView imageIcon = (ImageView) listRow.findViewById(R.id.list_image);
            
            String pubKey = mKeys[position];
            Publisher pubInfo = (Publisher)mCondPubList.get(pubKey);
            if(pubInfo != null){
	        if ( pubInfo.getBlockUsageSuggestion() != null) {
                    nameField.setText(pubInfo.getBlockUsageSuggestion());
                } else {
                    nameField.setText(pubInfo.getBlockName());
                }
                    imageIcon.setImageDrawable(pubInfo.getMutateImageDrawable());
	            if ( mGreyCondPubList.containsKey(pubKey)){
	            	nameField.setTextColor(mContext.getResources().getColor(R.color.disable_pub_gray));
	                imageIcon.setBackgroundResource(R.drawable.add_icon_background_disabled);
	            	imageIcon.setAlpha(TWENTY_FIVE_PERCENT_ALPHA_VALUE);
	            } else {
	            	nameField.setTextColor(mContext.getResources().getColor(R.color.first_line));
	                imageIcon.setBackgroundResource(R.drawable.add_icon_background);
	                imageIcon.setAlpha(EIGHTY_PERCENT_ALPHA_VALUE);
	            }
            }            
            return listRow;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position,
                                   long id) {
        super.onListItemClick(l, v, position, id); 
        
        try {
        	String pubKey = mKeys[position];
            Publisher pubInfo = (Publisher)mCondPubList.get(pubKey);
            if(pubInfo !=null){
	            updatePublisherSelected(pubKey);
	            String blockIntentUriString = pubInfo.getIntentUriString();
	            mIntentForDialog = Intent.parseUri(blockIntentUriString, 0);
                mIntentForDialog.putExtra(EXTRA_VERSION, CONDITION_PUBLISHER_VERSION);
	            if ( !mGreyCondPubList.containsKey(pubKey)){
	            	if (pubInfo.getPublisherKey().equals(LOCATION_TRIGGER)){
	            		//if consent is not there, show the consent dialog
	                    if(!Util.isMotLocConsentAvailable(mContext)){
	                    	LocationConsent.showLocationWiFiAutoscanDialog(this);
	                    }else{
	                    	LocationConsent.startRelevantActivity(this, mIntentForDialog);
	                    }
		            }else{
		                    //for all other Triggers other than Loc
		                    startActivityForResult(mIntentForDialog,PROCESS_CONDITION_LIST_ITEM);
		            }
	            }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            
	            case PROCESS_CONDITION_LIST_ITEM: {
	                data.putExtra(CONDITION_PUB_SEL, mLastPublisherSelected);
	                setResult(RESULT_OK, data);
	                finish();
	                break;
	            }
            }
        }
    }
    
    /** Create a dialog for the corresponding id
     *  @param id
     */
    protected Dialog onCreateDialog(int id) {
       	Dialog dialog;
       	
       	switch(id) {
	       	case EditRuleActivity.DialogId.DIALOG_LOCATION_CONSENT_ID:
	       		dialog = LocationConsent.showLocationConsentDialog(this, null);
	       		break;
	       	case EditRuleActivity.DialogId.DIALOG_LOCATION_WIFI_ENABLE_CONSENT_ID:
	       		dialog = LocationConsent.showWifiTurnOnDialog(this, null);
	       		break;
	       	case EditRuleActivity.DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID:
                dialog = LocationConsent.showAirplaneModeLocationCorrelationDialog(this);
                break;
	       	case EditRuleActivity.DialogId.DIALOG_LOC_WIFI_AUTOSCAN_CONSENT_ID:
                dialog = LocationConsent.showWiFiLocationAutoscanConsentDialog(this, mIntentForDialog, null);
                break;
	       	
	       	default:
	       		dialog = null;
       	}
        return dialog;
    }
    
    /** Interface function for ILocationPicker*/
    public void invokeLocationPicker() {
	    LocationConsent.setLocationPickerLaunchedFlag(true);
	    startActivityForResult(LocationConsent.getIntent(),PROCESS_CONDITION_LIST_ITEM );
    }
  
	public static void setPubCallback(IRulesBuilderPublisher pubCallback) {
		DisplayConditionsActivity.pubCallback = pubCallback;
		mCondPubList = pubCallback.fetchConditionPublisherList();
		mKeys = mCondPubList.keySet().toArray(new String[mCondPubList.size()]);
		mGreyCondPubList = pubCallback.fetchConditionListToGrey(mCondPubList);
	}
}
