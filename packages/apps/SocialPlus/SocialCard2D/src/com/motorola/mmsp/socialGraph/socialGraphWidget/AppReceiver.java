package com.motorola.mmsp.socialGraph.socialGraphWidget;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.MessageUtils;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.dataHandleCenter.SocialMessageReceiver;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphProvider;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.Intents;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel;

public class AppReceiver extends BroadcastReceiver {
	private static final String TAG = "SocialGraphWidget";

	private static HandlerThread mHandlerThread = null;
	private static MsgHandler mMsgHandler = null;

	private static final int MAX_INTENT_NUM = 100;
	private static Intent[] mIntentList = new Intent[MAX_INTENT_NUM];
	private static int mListHead = 0;
	private static int mListTail = 0;

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		Log.d(TAG, "app receive intent" );
		if (mHandlerThread == null) {
			mHandlerThread = new HandlerThread(
					"social graph widget message handler thread");
			mHandlerThread.start();
			mMsgHandler = new MsgHandler(mHandlerThread.getLooper());
		}

		mIntentList[mListTail++] = intent;
		if (mListTail >= (MAX_INTENT_NUM - 1)) {
			mListTail = 0;
		}

		Message message = new Message();
		mMsgHandler.sendMessage(message);
	}

	private void handleMsg() {

		Intent intent = mIntentList[mListHead];
		mIntentList[mListHead++] = null;
		if (mListHead >= (MAX_INTENT_NUM - 1)) {
			mListHead = 0;
		}

		if (intent == null || intent.getAction() == null) {
			return;
		}

		Log.d(TAG, "receive intent" + intent.toString());

		if (intent.getAction().equals(Intents.BROADCAST_SETTING_CHANGE)) {
			Log.d(TAG, "receive intent  type setting change");
			Log.d(TAG, "receive intent extra string "
					+ intent.getStringExtra("key"));
			if (Setting.KEY_AUTOMATIC_SETUP_MODE.equals(intent
					.getStringExtra("key"))) {
				
				// update frequency table
				updateFrequencyTable(mContext);
				// end
				
				RingLayoutModel.getDefaultRingLayoutModel(mContext).mDataReady = false;
				RingLayoutModel.getDefaultRingLayoutModel(mContext).sync();
				RingLayoutModel.getDefaultRingLayoutModel(mContext).mDataReady = true;
				RingLayoutModel.getDefaultRingLayoutModel(mContext).notifyDataLoadFinish();
				Intent i = new Intent(Intents.BROADCAST_WIDGET_MODE_CHANGE);
				mContext.sendBroadcast(i);
			} else if (Setting.KEY_STRETCH_CONTACTS_PICTURES.equals(intent
					.getStringExtra("key"))) {
				boolean bStrech = Setting.getInstance(mContext).isStretchOn();
				RingLayoutModel.getDefaultRingLayoutModel(mContext)
						.setStretchPhoto(bStrech);
				RingLayoutModel.getDefaultRingLayoutModel(mContext)
						.StretchPhotoChange();
			} else if (Setting.KEY_SET_SKIN
					.equals(intent.getStringExtra("key"))) {
				Setting.setSkinToDb(mContext);
			}
			
			Log.d(TAG, "setting change action " + intent.getAction());
			// debug log info
			if (Setting.getInstance(mContext).isAutoMode()) {
				Log.d(TAG, "setting change action is auto mode");
			}// log

			if (Setting.getInstance(mContext).isAutoMode()) {
				if ((Setting.KEY_HIDE_CONTACTS.equals(intent
						.getStringExtra("key")))) {
					Log.d(TAG, "auto mode setting acttion into if");
					updateFrequencyTable(mContext);
					RingLayoutModel.getDefaultRingLayoutModel(mContext).sync();
				}

			} else {
				if (Setting.KEY_WIDGET_MANUAL_CONTACTS.equals(intent
						.getStringExtra("key"))) {
					Log.d(TAG, "manual mode setting acttion into if");

					RingLayoutModel.getDefaultRingLayoutModel(mContext).sync();
				}
			}
		} else if (intent.getAction().equals(
				Intents.BROADCAST_SHORTCUT_UPDATE_ACTION)) {
			
			Bundle bundle = intent.getExtras();
			int type = bundle.getInt("type");
			
			//debug info
			Log.d(TAG, "get intent shortcut update");
			Log.d(TAG, "shortcut update type = " + type);
			//debug
			
			if (((type & Constant.NOTIFY_TYPE_FREQUENCY) != 0)
					&& (Setting.getInstance(mContext).isAutoMode())) {
				Log.d(TAG, "frequency sync shoutcut");
				RingLayoutModel.getDefaultRingLayoutModel(mContext).sync();
			}
			
			if ((type & Constant.NOTIFY_TYPE_CONTACT_CHANGE) != 0) {
				if (!Setting.getInstance(mContext).isAutoMode()) {
					Log.d(TAG, "refresh shoutcut for delete or join person in manul mode");
					RingLayoutModel.getDefaultRingLayoutModel(mContext).refreshContactsInfo(mContext);
				} 
			}
			
			if ((type & Constant.NOTIFY_TYPE_CONTACT_INFO_CHANGE) != 0) {
				Log.d(TAG, "get contact info change");
				// update widget contact info.
				ArrayList<Integer> informationChangedPeople = bundle
						.getIntegerArrayList("informationChangedPeople");
				if (informationChangedPeople != null) {
					
					//debug info
					Log.d(TAG, "information changed peopel num="
							+ informationChangedPeople.size());
					for (int i = 0; i < informationChangedPeople.size(); i++) {
						Log.d(TAG, "changed people id = "
								+ informationChangedPeople.get(i));
					}
					//debug
					
					RingLayoutModel.getDefaultRingLayoutModel(mContext)
							.refreshContactsInfo(mContext,
									informationChangedPeople);

					RingLayoutModel.getDefaultRingLayoutModel(mContext)
							.setHistoryForInformationChange(
									informationChangedPeople);

				} else {
					Log.d(TAG, "information changed people is null");
				}
			}

		}

	}
	
	private void updateFrequencyTable(Context ctx) {
		if (SocialMessageReceiver.queryOutOfBoxStatus(ctx)) {
			ArrayList<Integer> newFrequentlyList = new ArrayList<Integer>();
			ContentValues Contactvalues = new ContentValues();
			
			MessageUtils.getFrequentlyContactedInfoFromPhoneBook4(ctx, Contactvalues,
					newFrequentlyList);
			
			MessageUtils.updateFrequencyToDB(SocialGraphProvider.getDatabase(),
					newFrequentlyList);
			
			MessageUtils.updateContactsInfoToDB(SocialGraphProvider.getDatabase(), Contactvalues.valueSet(), "0");
		}
	}
	
	private class MsgHandler extends Handler {

		public MsgHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				handleMsg();
				break;
			}
		}
	}

}
