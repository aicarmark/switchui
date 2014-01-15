package com.motorola.contextual.smartrules.rulesbuilder;


import java.net.URISyntaxException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;

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
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.IRulesBuilderPublisher;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.uipublisher.PublisherList;

public class DisplayActionsActivity extends ListActivity implements Constants {

    private static final String TAG = DisplayActionsActivity.class.getSimpleName();
    private static final int PROCESS_ACTION_LIST_ITEM = 200;
    private static volatile ProgressDialog mProgressDialog = null;
    private static String mLastPublisherSelected;
    private static IRulesBuilderPublisher mPubCallback;
    private static ActionPublisherList mActPubList;
    private static PublisherList mGreyActPubList;
    private static String[] mKeys;
    
    public void onCreate(Bundle savedInstanceState) {
        if(LOG_DEBUG) Log.d(TAG, "In onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rb_display_actions_list);
        
        ActionBar ab = getActionBar();
        ab.setTitle(R.string.actions);
        ab.setDisplayHomeAsUpEnabled(true);
 		ab.show();
        TextView prompt = (TextView) findViewById(R.id.prompt);
        prompt.setText(Html.fromHtml(getString(R.string.actions_prompt)));

        if (mActPubList != null && mActPubList.size() > 0)
        	showActionsList();
        else
        	showErrorMessage(getString(R.string.no_avail_actions));
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_DEBUG) Log.d(TAG, "In onDestroy()");
        super.onDestroy();
        if (mProgressDialog != null){
    		mProgressDialog.dismiss();
    		mProgressDialog = null;
    	}
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
   	
    private void showActionsList() {
        ListView lv= (ListView)findViewById(android.R.id.list);
        lv.setAdapter(new ActionAdapter(this, mActPubList));

    }
    
    private static synchronized void updatePublisherSelected(String  publisher) {
    	mLastPublisherSelected = publisher;
    }

    public static class ActionAdapter extends BaseAdapter {
        private Context mContext;
        public ActionAdapter(Context c, ActionPublisherList acList) {
            mContext = c;

        }

        public int getCount() {
            return mActPubList.size();
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
            Publisher pubInfo = (Publisher)mActPubList.get(pubKey);
            if(pubInfo !=null){
                if ( pubInfo.getBlockUsageSuggestion() != null) {
                    nameField.setText(pubInfo.getBlockUsageSuggestion());
                } else {
                    nameField.setText(pubInfo.getBlockName());
                }
                    imageIcon.setImageDrawable(pubInfo.getMutateImageDrawable());
	            if ( mGreyActPubList.containsKey(pubKey)){
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
      
        Intent myIntent = null;;
        try {
		String pubKey = mKeys[position];
            Publisher pubInfo = (Publisher)mActPubList.get(pubKey);
            if(pubInfo !=null){
	            updatePublisherSelected(pubKey);
			if (pubInfo.getPublisherKey().equals(LAUNCH_APP_PUBLISHER_KEY)){
				mProgressDialog = ProgressDialog.show( this, "", getString(R.string.please_wait), true);
				mProgressDialog.setCancelable(true);
			}
			String blockIntentUriString = pubInfo.getIntentUriString();
	            myIntent = Intent.parseUri(blockIntentUriString, 0);
	            myIntent.putExtra(EXTRA_VERSION, ACTION_PUBLISHER_VERSION);
	            if ( !mGreyActPubList.containsKey(pubKey))
			startActivityForResult(myIntent,PROCESS_ACTION_LIST_ITEM );
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
   
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK) {
        	if (mProgressDialog != null){
        		mProgressDialog.dismiss();
        		mProgressDialog = null;
        	}
        }
      
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            
	            case PROCESS_ACTION_LIST_ITEM: {
	            	
	            	if (mProgressDialog != null){
	            		mProgressDialog.dismiss();
	            		mProgressDialog = null;
	            	}
	                data.putExtra(ACTION_PUB_SEL, mLastPublisherSelected);
	               
	                setResult(RESULT_OK, data);
	                finish();

	                break;
	            }
            }
        }
    }

	public static void setPubCallback(IRulesBuilderPublisher callback) {
		mPubCallback = callback;
		mActPubList = mPubCallback.fetchActionPublisherList();
		mKeys = mActPubList.keySet().toArray(new String[mActPubList.size()]);
		mGreyActPubList = mPubCallback.fetchActionListToGrey(mActPubList);
	}
}
