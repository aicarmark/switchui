package com.motorola.quicknote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class QNFindEmptyCellResultReceiver extends BroadcastReceiver {

	private static final String TAG = "FindEmptyCellResultReceiver";

	public static final int SCREEN_NUMBER = 1;
	private Handler _result_handler = null;

	QNFindEmptyCellResultReceiver(Handler handler) {
		_result_handler = handler;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		QNDev.qnAssert(_result_handler != null);

		// int cellX, cellY = 0;
		Bundle b = getResultExtras(true);

		int homeScreen = b.getInt("screen");
		// cellX = b.getInt("cellX");
		// cellY = b.getInt("cellY");

		Message msg = _result_handler.obtainMessage(SCREEN_NUMBER);

		QNDev.qnAssert(msg != null);

		msg.arg1 = homeScreen;
		_result_handler.sendMessage(msg);
	}

	public void setResultHandler(Handler handler) {
		_result_handler = handler;
	}

}
