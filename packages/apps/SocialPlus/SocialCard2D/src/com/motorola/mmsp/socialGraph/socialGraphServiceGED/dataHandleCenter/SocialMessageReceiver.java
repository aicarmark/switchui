package com.motorola.mmsp.socialGraph.socialGraphServiceGED.dataHandleCenter;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.motorola.mmsp.socialGraph.socialGraphServiceGED.ConstantDefinition;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.MessageUtils;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.MiscValue;

/*
 * it is the message center, which will receive all the messages and then handle them.
 * 
 * it will receive all messages from email, sms.mms, call log and contact
 * then post all the messages to their related handle function.
 */

public class SocialMessageReceiver extends BroadcastReceiver{
	
	private final String TAG = "SocialMessageReceiver";
	private final boolean Debug = true;

	private Context mContext;
	
	private static final int MAX_INTENT_NUM = 100;	
	private static Intent[] mIntentList = new Intent[MAX_INTENT_NUM];
	private static int mListHead = 0;
	private static int mListTail = 0;
	private static HandlerThread mHandlerThread = null;
	private static MsgHandler mMsgHandler = null;
	
	private static final String MISC_KEY = "misc_key";
	private static final String MISC_VALUE = "misc_value";
	private static final String OUT_OF_BOX = "out_of_box";

	private static int updateType = 0;
	
	private static ArrayList<Integer> mInformationChangedPeople = new ArrayList<Integer>();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
	
		if (queryOutOfBoxStatus(mContext)) {

			if (mHandlerThread == null) {
				mHandlerThread = new HandlerThread(
						"social message handler thread");
				mHandlerThread.start();
				mMsgHandler = new MsgHandler(mHandlerThread.getLooper());
			}

			if (Debug)
				Log.v(TAG, "SocialMessageReceiver onReceive " + intent);

			mIntentList[mListTail++] = intent;
			if (mListTail >= (MAX_INTENT_NUM - 1)) {
				mListTail = 0;
			}

			Message message = new Message();
			mMsgHandler.sendMessage(message);
		}
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

		int pos = intent.getIntExtra(ConstantDefinition.COLUMN_CURRENT_POS, 0);
		int count = intent
				.getIntExtra(ConstantDefinition.COLUMN_TOTAL_COUNT, 0);
		
		if (Debug)
			Log.e(TAG, "social message receiver pos = " + pos + " count = "
					+ count);
		
		if (pos >= count - 1) {
			
			updateType |= MessageUtils.handleContactChanged(mContext, intent,
					mInformationChangedPeople);
			
			if (Debug)
				Log.e(TAG, "update type = " + updateType);
			
			for (int i = 0; i < mInformationChangedPeople.size(); i++) {
				if (Debug)
					Log.e(TAG, "changed people id = "
							+ mInformationChangedPeople.get(i));
			}
			
			Intent broadcast = new Intent(
					ConstantDefinition.SHORTCUT_UPDATE_ACTION);
			
			Bundle bundle = new Bundle();
			if ((updateType & MessageUtils.NOTIFY_TYPE_CONTACT_INFO_CHANGE) != 0) {
				bundle.putIntegerArrayList("informationChangedPeople",
						mInformationChangedPeople);
			}

			bundle.putInt("type", updateType);
			broadcast.putExtras(bundle);
			mContext.sendBroadcast(broadcast);
			updateType = 0;
			mInformationChangedPeople.clear();
			if (Debug)
				Log.e(TAG, "send broadcast SHORTCUT_UPDATE_ACTION");
		}
	}
	
	private class MsgHandler extends Handler {
		
		public MsgHandler(Looper looper){
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
	
	public static boolean queryOutOfBoxStatus(Context ctx) {
		Cursor c = null;
		int outOfBox = 0;

		try {
			c = ctx.getContentResolver().query(MiscValue.CONTENT_URI,
					new String[] { MISC_VALUE }, MISC_KEY + "=?",
					new String[] { OUT_OF_BOX }, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				outOfBox = c.getInt(0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		
		if (outOfBox == 1) {
			return true;
		} else {
			return false;
		}
	}
	
}
