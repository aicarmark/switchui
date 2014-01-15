package com.motorola.mmsp.rss.service.receiver;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.service.manager.RssAlarmManager;
import com.motorola.mmsp.rss.service.parse.RssWorker;

public class RssBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "RSSBroadcastReceiver";
	private static final String CONNECTED = "CONNECTED";
	private static final String KEY_AUTO_UPDATE ="autoUpdate";
	private static final String KEY_WIDGET_ID = "widgetId";
	private static final String KEY_REMAIN_FEEDINFO = "remainFeedInfo";
	private static final String KEY_HAS_REMAIN_INFO = "hasRemainInfo";
	private static final String KEY_STATE = "state";//check add news or update news
	private static final int STATE_ADD_NEWS = 0;
	private static final int STATE_UPDATE_NEWS = 1;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
				Log.d(TAG, "system boot completed");
				context.startService(new Intent(RssConstant.Intent.INTENT_RSSSERVICE_BOOT_COMPLETED));
			} else if (RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE.equals(action)) {
				Log.d(TAG, "network is not available");
				int state = intent.getIntExtra(KEY_STATE, STATE_ADD_NEWS);
				if(state == STATE_ADD_NEWS) {
					Toast.makeText(context, context.getResources().getString(R.string.cannot_add_subscription), Toast.LENGTH_SHORT).show();
				} else if (state == STATE_UPDATE_NEWS) {
					Toast.makeText(context, context.getResources().getString(R.string.network_not_available), Toast.LENGTH_SHORT).show();
				}				
			} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				if ((netInfo.getType() == ConnectivityManager.TYPE_MOBILE)
						|| (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
						|| (netInfo.getType() == ConnectivityManager.TYPE_MOBILE_DUN)) {
					Log.i(TAG, "connectivity_action: " + netInfo);
					if(CONNECTED.equals(netInfo.getState().toString())){
						if(RssWorker.autoRefreshRemainInfo != null && !RssWorker.autoRefreshRemainInfo.isEmpty()) {
							Log.d(TAG, "need refresh remain feed last time");
							for(int widgetId : RssWorker.autoRefreshRemainInfo.keySet()) {
								Intent refreshIntent = new Intent(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH);
								refreshIntent.putExtra(KEY_AUTO_UPDATE, true);
								refreshIntent.putExtra(KEY_WIDGET_ID, widgetId);
								refreshIntent.putParcelableArrayListExtra(KEY_REMAIN_FEEDINFO, RssWorker.autoRefreshRemainInfo.get(widgetId));
								refreshIntent.putExtra(KEY_HAS_REMAIN_INFO, true);
								context.startService(refreshIntent);
							}
							RssWorker.autoRefreshRemainInfo.clear();
						} else {
							Log.d(TAG, "network connected, no need refresh");
						}					
					}
				}
			} else if(Intent.ACTION_TIME_CHANGED.equals(action)){
				Log.d(TAG, "android.intent.action.TIME_SET currentTime : " + new Date(System.currentTimeMillis()).toLocaleString());
				RssAlarmManager.getInstance(context).updateAlarmforNewsCheck();
			}
		}
	}
}
