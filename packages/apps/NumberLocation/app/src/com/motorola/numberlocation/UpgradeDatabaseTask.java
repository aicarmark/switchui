package com.motorola.numberlocation;

import java.io.Reader;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;


import com.motorola.numberlocation.WebUpdateDownload;

public class UpgradeDatabaseTask extends AsyncTask<Void, Integer, Long> {
	private static final String TAG = "UpgradeDatabaseTask";

	   
	private int mStatus = NumberLocationConst.STATUS_SUCCESS;
	private boolean mCancelFlage = false;
	
	private NumberLocationService mNumberLocationService;
	private String mApplicationRootPath;
	private Context mContext = null;
	
	public NumberLocationService getmNumberLocationService() {
		return mNumberLocationService;
	}

	public void setmNumberLocationService(
			NumberLocationService mNumberLocationService) {
		this.mNumberLocationService = mNumberLocationService;
	}

	public String getmApplicationRootPath() {
		return mApplicationRootPath;
	}

	public void setmApplicationRootPath(String mApplicationRootPath) {
		this.mApplicationRootPath = mApplicationRootPath;
	}

	public UpgradeDatabaseTask(Context context,NumberLocationService service, String rootPath) {
		mContext  = context;
		mNumberLocationService = service;
		mApplicationRootPath = rootPath;
	}
/*
	public String getCurrentTimestamp() {
		Calendar todaysDate = new GregorianCalendar();
		int year = todaysDate.get(Calendar.YEAR);
		int month = todaysDate.get(Calendar.MONTH) + 1;
		int day = todaysDate.get(Calendar.DAY_OF_MONTH);
		int hourOfDay = todaysDate.get(Calendar.HOUR_OF_DAY);
		int minute = todaysDate.get(Calendar.MINUTE);
		int second = todaysDate.get(Calendar.SECOND);
		DecimalFormat df = new DecimalFormat("00");
		String result = year + "-" + df.format(month) + "-" + df.format(day) + " " + df.format(hourOfDay) + ":"
				+ df.format(minute) + ":" + df.format(second);
		return result;
	}
*/	
	public String getCurrentTimestampInMillis() {
		Calendar todaysDate = new GregorianCalendar();
		return Long.toString(todaysDate.getTimeInMillis());
	}
	
	void setLastUpgradeTimestamp(String timestamp){

		SharedPreferences prefs = mContext.getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(NumberLocationConst.KEY_LAST_UPGRADE_TIMESTAMP, timestamp);
        e.commit();
	}

	public void showResult(int result) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_SHOW_RESULT);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_RESULT_STATUS, result);
		mContext.startService(i);
	}
	
	public void showProgress(int type) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_SHOW_PROGRESS);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		mContext.startService(i);
	}
	
	public void updateProgress(int type,int progress) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_UPDATE_PROGRESS);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_VALUE, progress);
		mContext.startService(i);
	}
	
	public void dismissProgress(int type) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_DISMISS_PROGRESS);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		mContext.startService(i);
	}
	
	public void setProgressMax(int type, int max) {
		Intent i = new Intent(NumberLocationConst.ACTION_CALL_BACK_SET_PROGRESS_MAX);
		i.setClassName(mContext, NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_TYPE, type);
		i.putExtra(NumberLocationConst.EXTRA_PARAM_PROGRESS_MAX, max);
		mContext.startService(i);
	}
	
	@Override
	protected Long doInBackground(Void... params) {
		// int count = strings.length;
		long totalSize = 0;
		try {
			String databaseVersion = getCurrentDatabaseVersion();
			String debugLocalUpdateFilePath = NumberLocationUtilities.checkSDCardLocalUpdatePath(mContext);
			NumberLocationUpdateData updateData = null;
			if(debugLocalUpdateFilePath != null){
				Reader response = null;
				response = NumberLocationUtilities.getReaderFromLocalUpdatePathFile(mContext, debugLocalUpdateFilePath);
				if(response != null) {
					updateData = WebResponseParser.parseWebResponse(response);
					NumberLocationUtilities.closeGZIPInputStream();
				}
			}
			
			if(updateData == null)
				updateData = WebUpdateDownload.getUpdateData(mContext,databaseVersion, this);
			if(mCancelFlage ==  true){
				dismissProgress(NumberLocationConst.PROGRESS_TYPE_DOWNLOAD);
				return totalSize;
			}
			dismissProgress(NumberLocationConst.PROGRESS_TYPE_DOWNLOAD);
			if (updateData != null) {
				mStatus = updateData.getStatus();
				if (mStatus == NumberLocationConst.STATUS_SUCCESS) {
					String dbVersion = getCurrentDatabaseVersion();
					String XMLVersion = getCurrentXMLVersion();
					DatabaseUpdate.setCancelFlag(mCancelFlage);
					showProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
					String version = DatabaseUpdate.updateDatabase(mContext, XMLVersion, dbVersion, updateData);
					if (mCancelFlage == false) {
						if (version != null)
							NumberLocationUtilities.setNumberLocationDatabaseVersion(mContext, version);
						setLastUpgradeTimestamp(getCurrentTimestampInMillis());
						dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
						showResult(NumberLocationConst.STATUS_SUCCESS);
					} else {
						dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
						showResult(NumberLocationConst.STATUS_USER_CANCEL);
					}
				} else if (mStatus == NumberLocationConst.STATUS_NOT_FOUND) {
					setLastUpgradeTimestamp(getCurrentTimestampInMillis());
					dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
					showResult(NumberLocationConst.STATUS_NOT_FOUND);
				} else if (mStatus == NumberLocationConst.STATUS_NETWORK_FAIL) {
					dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
					showResult(NumberLocationConst.STATUS_NETWORK_FAIL);
				} else if (mStatus == NumberLocationConst.STATUS_USER_CANCEL) {
					dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
					showResult(NumberLocationConst.STATUS_USER_CANCEL);
				}
			}
			return totalSize;
		} catch (WebUpdateException e) {
			if(mStatus == NumberLocationConst.STATUS_NOT_FOUND){
				showResult(NumberLocationConst.STATUS_NOT_FOUND);
				Log.d(TAG, "version is latest! e =" + e);
			}else if(mStatus == NumberLocationConst.STATUS_NETWORK_FAIL){
				showResult(NumberLocationConst.STATUS_NETWORK_FAIL);
			}
			else
				Log.d(TAG, "Web update download error! e =" + e);
			e.printStackTrace();
		}
		return totalSize;
	}

	public void reportProgress(int type,int progress) {
		updateProgress(type,progress);
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
	}

	@Override
	protected void onPostExecute(Long result) {
		dismissProgress(NumberLocationConst.PROGRESS_TYPE_DOWNLOAD);
		dismissProgress(NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE);
		super.onPostExecute(result);
	}

	@Override
	protected void onPreExecute() {
		showProgress(NumberLocationConst.PROGRESS_TYPE_DOWNLOAD);
		super.onPreExecute();
	}

	@Override
	protected void onCancelled() {
		mCancelFlage = true;
		DatabaseUpdate.setCancelFlag(mCancelFlage);
		super.onCancelled();
	}

	// should move to database helper
	private String getCurrentDatabaseVersion() {
		String versionStr = NumberLocationUtilities.getNumberLocationDatabaseVersion(mContext);
		return versionStr; 
		// return null;
	}
	
	// should move to database helper
	private String getCurrentXMLVersion() {
		String versionStr = NumberLocationConst.XML_PARSE_VERSION;
		return versionStr; // temp for debug
		// return null;
	}
}
