package com.motorola.mmsp.rss.app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.util.NetworkUtil;

public class FeedCustomConfirmActivity extends RssAppBaseActivity implements OnClickListener {
	private static final String TAG = "FeedCustomConfirmActivity";
	private String mFeedUrl= "";
	private ProgressDialog mProgressDialog;
	private AlertDialog mPromptDialog;
	BroadcastReceiver mBroadcastReceiver  = null;
    private static final String KEY_FEED_CUSTOMINFO = "feedCustomInfo";
    private static final String KEY_FEED_URL = "feedUrl";
    private static final String KEY_CHECK_URL_RESULT = "key_check_url_result";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feedcustomconfirm);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		TextView titleView = (TextView) getWindow().findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.customforaddfeedurl);
		TextView showFeedUrl = (TextView) findViewById(R.id.confirmfeedurl);
		getFeedUrl();
		showFeedUrl.setText(mFeedUrl);
		mBroadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent != null) {
					String action = null;
					action = intent.getAction();
					if(action != null)
					if(RssConstant.Intent.INTENT_RSSAPP_CHECK_URL_COMPLETE.equals(action)){
						if(mProgressDialog != null && mProgressDialog.isShowing()) {
							mProgressDialog.dismiss();
						}
						FeedInfo feedInfo = new FeedInfo();
						getFeedUrl();
						feedInfo.feedUrl = mFeedUrl;
						Bundle custombundle = new Bundle();
						Intent result = new Intent();
						custombundle.putParcelable(KEY_FEED_CUSTOMINFO, feedInfo);
						result.putExtras(custombundle);
						int state = intent.getIntExtra(KEY_CHECK_URL_RESULT, -1);
						if(state == RssConstant.State.STATE_SUCCESS){
								FeedCustomConfirmActivity.this
										.setResult(
												RssConstant.Flag.FEEDCUSTOMCONFIRM_RESULECODE,
												result);
								finish();
						} else {
							showErrorDialog(state);
						}
					} else if(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE.equals(action)) {
						Log.d(TAG, "network is not available");
						if(mProgressDialog != null && mProgressDialog.isShowing()) {
							mProgressDialog.dismiss();
						}				
						showErrorDialog(RssConstant.State.STATE_URL_NOT_AVAILABLE);
					}else if(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART.equals(action)){
						Log.d(TAG ,"Service restart");
						if(mProgressDialog != null && mProgressDialog.isShowing()) {
							mProgressDialog.dismiss();
						}				
						showErrorDialog(RssConstant.State.STATE_SERVICE_RESTART);
					}
				}				
			}
			
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(RssConstant.Intent.INTENT_RSSAPP_CHECK_URL_COMPLETE);
		filter.addAction(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE);
		filter.addAction(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART);
		registerReceiver(mBroadcastReceiver, filter);
	}
	
	private void showErrorDialog(int state) {
		if(mPromptDialog == null) {
			mPromptDialog = new AlertDialog.Builder(this).create();
		}
		int tipId = 0;
		switch(state){
		case RssConstant.State.STATE_CONNECTY_FAILURE:
			tipId = R.string.connect_failure;
			break;
		case RssConstant.State.STATE_URL_NOT_AVAILABLE:
			tipId = R.string.invalid_url;
			break;
		case RssConstant.State.STATE_NETWORK_NOT_AVAILABLE:
			tipId = R.string.network_not_available;
			break;
		case RssConstant.State.STATE_PARSE_XML_FAILURE:
			tipId = R.string.invalid_rss_url;
			break;
		default:
			tipId = R.string.tryagain;
			break;
		}
		mPromptDialog.setTitle(this.getResources().getString(R.string.error));
		mPromptDialog.setMessage(this.getResources().getString(tipId));
		mPromptDialog.setButton(this.getResources().getString(R.string.ok), this);
		mPromptDialog.show();
	}
	
	public void getFeedUrl(){
		Bundle bundle = this.getIntent().getExtras();
		if(bundle != null){
			mFeedUrl = bundle.getString(KEY_FEED_URL);
			}
		}

	public void onSubscribe(View v) {
		/*
		if(!NetworkUtil.isNetworkAvailable(this)){
			showErrorDialog(RssConstant.State.STATE_NETWORK_NOT_AVAILABLE);
			return ;
		}
		*/
		if(mFeedUrl != null){
			String tempUrl = mFeedUrl.trim();
			if(tempUrl.equals("")){
				showErrorDialog(RssConstant.State.STATE_URL_NOT_AVAILABLE);
				return ;
			}else{
				int index = tempUrl.indexOf(' ');
				if(index != -1){
					showErrorDialog(RssConstant.State.STATE_URL_NOT_AVAILABLE);
					return ;
				}
			}
		}else{
			showErrorDialog(RssConstant.State.STATE_URL_NOT_AVAILABLE);
			return ;
		}
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_CHECK_URL);
		intent.putExtra(RssConstant.Key.KEY_CHECK_URL, mFeedUrl);
		startService(intent);
		showProgressDialog();
	}
	
	private void showProgressDialog() {
		if(mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setTitle(this.getResources().getString(R.string.subscribe));
			mProgressDialog.setMessage(this.getResources().getString(R.string.customsubscribe_des));
		}
		mProgressDialog.show();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mBroadcastReceiver);
		super.onDestroy();
	}

	public void onClick(DialogInterface dialog, int which) {
		Log.d(TAG, "on Click");
		if(mPromptDialog != null && mPromptDialog.isShowing()) {
			mPromptDialog.dismiss();
		}
		this.finish();
	}
		
}
