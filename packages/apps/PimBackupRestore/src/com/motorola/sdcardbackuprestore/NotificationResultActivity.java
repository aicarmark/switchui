package com.motorola.sdcardbackuprestore;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import android.app.NotificationManager;



public class NotificationResultActivity extends AlertActivity implements
		DialogInterface.OnClickListener {
	private static final String TAG = "NotificationResultActivity";
	private static final boolean D = true;
	private static NotificationResultActivity mInstance = null;
	private AlertController.AlertParams mPara;
	private View mView = null;
	private TextView mLine1View;
	private TextView mLineType;
	private int mWhichDialog;
	private String mCompleteResultInfo = null;
	private int mActionType;
	private int total = -1;
	private ActivityHandler mHandler;
	private ProgressBar mProgressBar;
	private TextView mTextView;
	private String mDescription = "";

	public static NotificationResultActivity getInstance() {
		return mInstance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInstance = this;
		mHandler = new ActivityHandler();
		Intent intent = getIntent();
		String action = null;
		mDescription = intent.getStringExtra(Constants.DESCRIPTION);
		int current = intent.getIntExtra(Constants.CURRENT, -1);
		int total = intent.getIntExtra(Constants.TOTAL, -1);
		mActionType = intent.getIntExtra(Constants.ACTION_TYPE,	Constants.NO_ACTION);
		action = intent.getAction();
		Log.i(TAG, "onCreate() intent action="+action);
		if (action.equals(Constants.ACTION_ONGOING))
			mWhichDialog = Constants.DIALOG_ONGOING;
		else if (action.equals(Constants.ACTION_RESULT))
			mWhichDialog = Constants.DIALOG_COMPLETE;
		else
			Log.e(TAG, "not a valid intent action");

		if (mWhichDialog == Constants.DIALOG_COMPLETE) {
			mCompleteResultInfo = intent
					.getStringExtra(Constants.POPUP_COMPLETE_INFO);
		}
		Log.i(TAG, "onCreate() mCompleteResultInfo="+mCompleteResultInfo);
		// Set up the "dialog"
		setUpDialog(mWhichDialog);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		BackupRestoreService brService = BackupRestoreService.getInstance();
		if (brService != null) {
			updateActivity(Message.obtain(null, R.id.update_notification_activity, brService.getCurrentItem()));
		}
	}
	
	@Override
	protected void onPause() {
		Log.v("NotificationActivity", "onPause()");
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (D)
			Log.d(TAG, "onDestroy()");
		super.onDestroy();
	}

	private void setUpDialog(int dialogType) {
		mPara = mAlertParams;
		mPara.mIconId = android.R.drawable.ic_dialog_info;
		if (dialogType == Constants.DIALOG_ONGOING) {
			Log.e(TAG, "<<<<<< Enter config view DIALOG_ONGOING");
			mPara.mTitle = getString(R.string.operation_is_doing);
			mPara.mPositiveButtonText = getString(R.string.yes);
			mPara.mPositiveButtonListener = this;
			mPara.mNegativeButtonText = getString(R.string.no);
			mPara.mNegativeButtonListener = this;
		} else if (dialogType == Constants.DIALOG_COMPLETE) {
			Log.e(TAG, "<<<<<< Enter config view DIALOG_COMPLETE");
			if (mActionType == Constants.BACKUP_ACTION) {
				mPara.mTitle = getString(R.string.backup)
						+ getString(R.string.popup_complete_result_title);
			} else if (mActionType == Constants.RESTORE_ACTION) {
				mPara.mTitle = getString(R.string.restore)
						+ getString(R.string.popup_complete_result_title);
			} else if (mActionType == Constants.IMPORT3RD_ACTION) {
				mPara.mTitle = getString(R.string.import3rd)
						+ getString(R.string.popup_complete_result_title);
			} else if (mActionType == Constants.EXPORTBYACCOUNT_ACTION) {
				mPara.mTitle = getString(R.string.ExportByAccount_title)
						+ getString(R.string.popup_complete_result_title);
			}
			mPara.mPositiveButtonText = getString(android.R.string.ok);
			mPara.mPositiveButtonListener = this;
		} else {
			Log.e(TAG, "Unknow dialog type to show");
		}

		mPara.mView = createView(dialogType);
		setupAlert();
	}
	
	private View createView(int dialogType) {
		if (dialogType == Constants.DIALOG_ONGOING) {
			mView = getLayoutInflater().inflate(R.layout.notification_event_with_progress_bar, null);
			Log.v("createView", "no progress bar");
		} else if (dialogType == Constants.DIALOG_COMPLETE) {
			mView = getLayoutInflater().inflate(R.layout.notification_event, null);
		}
		mProgressBar = (ProgressBar)mView.findViewById(R.id.progress_bar);
		mTextView = (TextView)mView.findViewById(R.id.description);
		
		String tmp;
		Log.i(TAG, "createView() dialogType="+dialogType);
		Log.i(TAG, "createView() mCompleteResultInfo="+mCompleteResultInfo);
		if (dialogType == Constants.DIALOG_ONGOING) {
			Log.e(TAG, ">>>>>>>>  DIALOG_ONGOING");
			mLine1View = (TextView) mView.findViewById(R.id.line_view);
			mLineType = (TextView)mView.findViewById(R.id.line_type);
			tmp = getString(R.string.popup_cancel_ongoing_body);
			mLineType.setText(mDescription);
			mLine1View.setText(tmp);
		} else if (dialogType == Constants.DIALOG_COMPLETE) {
			Log.e(TAG, ">>>>>>>>  DIALOG_COMPLETE");
			mLine1View = (TextView) mView.findViewById(R.id.line_view);
			mLineType = (TextView)mView.findViewById(R.id.line_type);
			if (mCompleteResultInfo != null) {
				tmp = mCompleteResultInfo;
			} else {
				tmp = "";
			}
			mLine1View.setText(tmp);
		} else {
			Log.e(TAG, "Wrong dialog type to show view text");
		}

		return mView;
	}
	
	public void updateActivity(Message msg) {
		mHandler.sendMessage(msg);
	}

	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			if (mWhichDialog == Constants.DIALOG_ONGOING) {
				Log.e(TAG, "Start stop service");
				stopService(new Intent(NotificationResultActivity.this,
						BackupRestoreService.class));
				NotificationManager notMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				if (notMgr != null) {
					notMgr.cancel(Constants.NOTIFICATION_ID);
					Log.v(TAG, "notMgr.cancel called");
				}
			}
			if (mWhichDialog == Constants.DIALOG_COMPLETE) {
				NotificationManager notMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				if (notMgr != null) {
					notMgr.cancel(Constants.NOTIFICATION_ID);
					Log.v(TAG, "notMgr.cancel called");
				}
			}
			if (Backup2.getInstance() != null) {
				Backup2.getInstance().finish();
			}
			if (Restore2.getInstance() != null) {
				Restore2.getInstance().finish();
			}
			if (Import3rd2.getInstance() != null) {
				Import3rd2.getInstance().finish();
			}
			if (ExportByAccount2.getInstance() != null) {
				ExportByAccount2.getInstance().finish();
			}
			
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			Log.e(TAG, "On click BUTTON_NEGATIVE");
			break;
		}
		finish();
	}
	
	private final class ActivityHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == R.id.update_notification_activity) {
				NotificationItem item = (NotificationItem)msg.obj;
				if (item == null || mProgressBar == null || mTextView == null) {
					return;
				}
				if (total == -1 || total != item.totalTotal) {
					total = item.totalTotal;
					mProgressBar.setMax(total);
				}
				if (mDescription != null && mDescription != "" && !mDescription.equals(item.description)) {
					mDescription = item.description;
					mLineType.setText(mDescription);
				}
				int current = item.totalCurrent;
				mProgressBar.setProgress(current);
				String buffer = "";
	            if (total <= 0) {
	                buffer = "0%";
	            } else {
	                long progress = current * 100 / total;
	                StringBuilder sb = new StringBuilder();
	                sb.append(progress);
	                sb.append("%    ");
	                sb.append(current);
	                sb.append('/');
	                sb.append(total);
	                buffer = sb.toString();
	            }
	            mTextView.setText(buffer);
			}
		}
		
	}

}
