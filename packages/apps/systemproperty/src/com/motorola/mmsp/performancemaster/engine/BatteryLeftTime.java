/**
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 */

package com.motorola.mmsp.performancemaster.engine;

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

public class BatteryLeftTime {

	public static final String TAG = "batterylefttimelog";

	public static final String BATTERY_COLLECT_PREFS = "battery_collect_prefs";

	public static final String BATTERY_COLLECT_FIRST_TIME = "battery_collect_first_time";
	public static final String BATTERY_COLLECT_PRE_TIME = "battery_collect_pre_time";
	public static final String BATTERY_COLLECT_TIMES = "battery_collect_times";

	public static final String DISPLAY_USE_RATE = "display_use_rate";
	public static final String WIFI_USE_RATE = "wifi_use_rate";
	public static final String BLUETOOTH_USE_RATE = "bluetooth_use_rate";
	public static final String CELL_STANDBY_USE_RATE = "cell_standby_use_rate";
	public static final String VOICE_CALLS_USE_RATE = "voice_calls_use_rate";
	public static final String ANDROID_SYSTEM_USE_RATE = "android_system_use_rate";
	public static final String PHONE_IDLE_USE_RATE = "phone_idle_use_rate";
	public static final String DATA_CALLS_USE_RATE = "data_calls_use_rate";

	public static final String BATTERY_USE_TIMEALL = "battery_use_timeall";
	public static final String BATTERY_AVERAGEBV = "battery_averagebv";
	public static final String BATTERY_USE_TIMERUNALL = "battery_use_timerunall";

	private final static long COLLECT_PERIOD = 24L * 60L * 60L * 1000L; // 24hrs
	private final static int MAX_TIME_FOR_FULL_BATT = 7200; // 7200 mins =
															// 120hours
	private final static int MIN_TIME_FOR_FULL_BATT = 180; // 180 mins = 3hours
	private final static int MIN_TIMEPOINT_FOR_SAMPLE = 600; // in 10 mins

	private final static float DEFAULT_DISPLAY_USE_RATE = 0.08f; // 8%
	private final static float DEFAULT_WIFI_USE_RATE = 0.04f; // 4%
	private final static float DEFAULT_BLUETOOTH_USE_RATE = 0.01f; // 1%
	private final static float DEFAULT_CELL_STANDBY_USE_RATE = 0.99f; // 99%
	private final static float DEFAULT_VOICE_CALLS_USE_RATE = 0.01f; // 10%
	private final static float DEFAULT_ANDROID_SYSTEM_USE_RATE = 0.1f; // 10%
	private final static float DEFAULT_PHONE_IDLE_USE_RATE = 0.8f; // 80%
	private final static float DEFAULT_DATA_CALLS_USE_RATE = 0.01f; // 1%

	private final static float DEFAULT_BV1_DISPLAY = 0.0015f;
	private final static float DEFAULT_BV1_BACKLIGHT = 0.0020f;
	private final static float DEFAULT_BV2_WIFI = 0.0016f;
	private final static float DEFAULT_BV3_BT = 0.0016f;
	private final static float DEFAULT_BV4_CELL_STANDBY = 0.0002f;
	private final static float DEFAULT_BV5_VOCIL_CALLS = 0.0042f;
	private final static float DEFAULT_BV6_ANDROID_SYSTEM = 0.0015f;
	private final static float DEFAULT_BV7_PHONE_IDLE = 0.0004f;
	private final static float DEFAULT_BV8_DATA_CALLS = 0.0045f;
	private final static float DEFAULT_HOUR_WASTE = 0.0000319f;

	private Context mContext;
	private BatteryUsage mCollectUsage;
	private BatteryModeData mBattMode;

	private long mStartTime;
	private long mStartTimeRun;
	private int mBattLevel; // current batt level percent
	private boolean mBoolCharge; // current charge state
	private boolean mBoolInitBattInfo;
	private boolean mBattModeChange;
	private long mChargingStartTime;

	private SamplePoint mWastePoint1, mWastePoint2; // sample point for batt
													// waste
	private SamplePoint mChargePoint1, mChargePoint2; // sample point for batt
														// charge

	private float mRate1Display; // usage rate per user for display
	private float mRate2WiFi;
	private float mRate3BlueTooth;
	private float mRate4CellStandBy;
	private float mRate5VoiceCalls;
	private float mRate6AndroidSystem;
	private float mRate7PhoneIdle;
	private float mRate8DataCalls;

	private float mBattCollectTimeAll; // collect time total for sample points
	private float mAverageCollectBV; // average batt waste as collect time total
	private float mBattCollectRealRunRate; // Systemrealruntime/Powerontime

	private float mDefaultBV;
	private float mTempBattModeBV; // for current batt mode,if mode
									// change,should calculation

	public class SamplePoint {
		int time;
		int level;
		int timerun;
	}

	public BatteryLeftTime(Context context) {
		mContext = context;
		mCollectUsage = new BatteryUsage();
		mBattMode = new BatteryModeData();

		mChargingStartTime = mStartTime = SystemClock.elapsedRealtime();
		mStartTimeRun = SystemClock.uptimeMillis();
		mWastePoint1 = new SamplePoint();
		mWastePoint2 = new SamplePoint();
		mChargePoint1 = new SamplePoint();
		mChargePoint2 = new SamplePoint();
		mWastePoint1.time = mWastePoint2.time = mChargePoint1.time = mChargePoint2.time = 0;
		mWastePoint1.level = mWastePoint2.level = mChargePoint1.level = mChargePoint2.level = 0;
		mBoolCharge = false;
		mBoolInitBattInfo = false;
		mBattLevel = 101;
		mBattModeChange = true;

		calcUsageRate();
		getAverageBV();
		getBattCollectRealRunRate();
		mDefaultBV = (DEFAULT_BV1_DISPLAY + DEFAULT_BV1_BACKLIGHT * 0.3f)
				* mRate1Display + DEFAULT_BV2_WIFI * mRate2WiFi * 0.5f
				+ DEFAULT_BV3_BT * mRate3BlueTooth * 0.5f
				+ DEFAULT_BV4_CELL_STANDBY * mRate4CellStandBy
				+ DEFAULT_BV5_VOCIL_CALLS * mRate5VoiceCalls
				+ DEFAULT_BV6_ANDROID_SYSTEM * mRate6AndroidSystem
				+ DEFAULT_BV7_PHONE_IDLE * mRate7PhoneIdle
				+ DEFAULT_BV8_DATA_CALLS * mRate8DataCalls * 0.5f;

	}

	private void collectUseRate() {
		long timenow = System.currentTimeMillis(); // not consider user
													// modify the system time
		SharedPreferences sharePrefs = mContext.getSharedPreferences(
				BATTERY_COLLECT_PREFS, 0);
		long pre_collect_time = sharePrefs.getLong(BATTERY_COLLECT_PRE_TIME, 0);
		if (timenow - pre_collect_time > COLLECT_PERIOD) // interval 24hours
		{

			SharedPreferences.Editor editor = sharePrefs.edit();
			editor.putLong(BATTERY_COLLECT_PRE_TIME, timenow);
			editor.commit();

			long first_inatall_time = sharePrefs.getLong(
					BATTERY_COLLECT_FIRST_TIME, 0);
			if (first_inatall_time == 0) {
				editor.putLong(BATTERY_COLLECT_FIRST_TIME, timenow);
				editor.commit();
			}

			int times = sharePrefs.getInt(BATTERY_COLLECT_TIMES, 0);
			times++;
			editor.putInt(BATTERY_COLLECT_TIMES, times);
			editor.commit();

			float rusagerate = mCollectUsage.collectDisplayUsage();
			float pre_rusagerate = sharePrefs.getFloat(DISPLAY_USE_RATE, 0f);
			float new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(DISPLAY_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectWifiUsage();
			pre_rusagerate = sharePrefs.getFloat(WIFI_USE_RATE,
					DEFAULT_WIFI_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(WIFI_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectBluetoothUsage();
			pre_rusagerate = sharePrefs.getFloat(BLUETOOTH_USE_RATE,
					DEFAULT_BLUETOOTH_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(BLUETOOTH_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectCellStandbyUsage();
			pre_rusagerate = sharePrefs.getFloat(CELL_STANDBY_USE_RATE,
					DEFAULT_CELL_STANDBY_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(CELL_STANDBY_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectVoiceCallsUsage();
			pre_rusagerate = sharePrefs.getFloat(VOICE_CALLS_USE_RATE,
					DEFAULT_VOICE_CALLS_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(VOICE_CALLS_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectAndroidSystemUsage();
			pre_rusagerate = sharePrefs.getFloat(ANDROID_SYSTEM_USE_RATE,
					DEFAULT_ANDROID_SYSTEM_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(ANDROID_SYSTEM_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectPhoneIdleUsage();
			pre_rusagerate = sharePrefs.getFloat(PHONE_IDLE_USE_RATE,
					DEFAULT_PHONE_IDLE_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(PHONE_IDLE_USE_RATE, new_rusagerate);
			editor.commit();

			rusagerate = mCollectUsage.collectDataCallsUsage();
			pre_rusagerate = sharePrefs.getFloat(DATA_CALLS_USE_RATE,
					DEFAULT_DATA_CALLS_USE_RATE);
			new_rusagerate = (pre_rusagerate * times + rusagerate)
					/ (float) (times + 1);
			editor.putFloat(DATA_CALLS_USE_RATE, new_rusagerate);
			editor.commit();

			calcUsageRate();

		}
	}

	private void calcUsageRate() {
		SharedPreferences sharePrefs = mContext.getSharedPreferences(
				BATTERY_COLLECT_PREFS, 0);
		int times = sharePrefs.getInt(BATTERY_COLLECT_TIMES, 0);
		float q = 0;
		if (times > 180)
			q = 0.90f;
		else if (times > 60)
			q = 0.70f;
		else if (times > 30)
			q = 0.50f;
		else if (times > 10)
			q = 0.40f;
		else if (times > 5)
			q = 0.30f;
		else if (times > 3)
			q = 0.20f;
		else if (times > 1)
			q = 0.10f;
		else
			q = 0.0f;

		float rusagerate = sharePrefs.getFloat(DISPLAY_USE_RATE,
				DEFAULT_DISPLAY_USE_RATE);
		mRate1Display = DEFAULT_DISPLAY_USE_RATE - DEFAULT_DISPLAY_USE_RATE * q
				+ rusagerate * q;

		rusagerate = sharePrefs.getFloat(WIFI_USE_RATE, DEFAULT_WIFI_USE_RATE);
		mRate2WiFi = DEFAULT_WIFI_USE_RATE - DEFAULT_WIFI_USE_RATE * q
				+ rusagerate * q;

		rusagerate = sharePrefs.getFloat(BLUETOOTH_USE_RATE,
				DEFAULT_BLUETOOTH_USE_RATE);
		mRate3BlueTooth = DEFAULT_BLUETOOTH_USE_RATE
				- DEFAULT_BLUETOOTH_USE_RATE * q + rusagerate * q;

		rusagerate = sharePrefs.getFloat(CELL_STANDBY_USE_RATE,
				DEFAULT_CELL_STANDBY_USE_RATE);
		mRate4CellStandBy = DEFAULT_CELL_STANDBY_USE_RATE
				- DEFAULT_CELL_STANDBY_USE_RATE * q + rusagerate * q;

		rusagerate = sharePrefs.getFloat(VOICE_CALLS_USE_RATE,
				DEFAULT_VOICE_CALLS_USE_RATE);
		mRate5VoiceCalls = DEFAULT_VOICE_CALLS_USE_RATE
				- DEFAULT_VOICE_CALLS_USE_RATE * q + rusagerate * q;

		rusagerate = sharePrefs.getFloat(ANDROID_SYSTEM_USE_RATE,
				DEFAULT_ANDROID_SYSTEM_USE_RATE);
		mRate6AndroidSystem = DEFAULT_ANDROID_SYSTEM_USE_RATE
				- DEFAULT_ANDROID_SYSTEM_USE_RATE * q + rusagerate * q;

		rusagerate = sharePrefs.getFloat(PHONE_IDLE_USE_RATE,
				DEFAULT_PHONE_IDLE_USE_RATE);
		mRate7PhoneIdle = DEFAULT_PHONE_IDLE_USE_RATE
				- DEFAULT_PHONE_IDLE_USE_RATE * q + rusagerate * q;

		rusagerate = sharePrefs.getFloat(DATA_CALLS_USE_RATE,
				DEFAULT_DATA_CALLS_USE_RATE);
		mRate8DataCalls = DEFAULT_DATA_CALLS_USE_RATE
				- DEFAULT_DATA_CALLS_USE_RATE * q + rusagerate * q;

	}

	private void resetStartTime() {
		long timecurrent = SystemClock.elapsedRealtime();
		long timecurrentrun = SystemClock.uptimeMillis();

		mStartTime = timecurrent;
		mStartTimeRun = timecurrentrun;
		Log.d(TAG, "resetStartTime mStartTime: " + mStartTime
				+ " mStartTimeRun" + mStartTimeRun);
	}

	private boolean setNewPoint(int level, int oldLevel) {
		boolean ret = false;

		int timenowS = (int) (SystemClock.elapsedRealtime() / 1000);
		int timerealrun = (int) (SystemClock.uptimeMillis() / 1000);

		String log = new String(" [setNewpoint:timenowS" + timenowS + " at"
				+ (System.currentTimeMillis() / 1000) + " level=" + level
				+ " oldLevel=" + oldLevel + "] ");
		writeLOGTOSD(log);

		if ((level < oldLevel - 1)
				|| (level > oldLevel + 1)
				|| (Math.abs(timenowS - mWastePoint2.time) > MIN_TIMEPOINT_FOR_SAMPLE && level != oldLevel)) {
			setPoint(level, timenowS, timerealrun);
			ret = true;
		}
		return ret;
	}

	private void setPoint(int level, int timepointS, int timerealrun) {

		if (mBoolCharge) {
			mWastePoint1.time = mWastePoint2.time = 0;
			mWastePoint1.level = mWastePoint2.level = 0;
			mWastePoint1.timerun = mWastePoint2.timerun = 0;

			mChargePoint1.time = mChargePoint2.time;
			mChargePoint1.level = mChargePoint2.level;
			mChargePoint2.time = timepointS;
			mChargePoint2.level = level;
			calcChargeTime();

		} else {
			mChargePoint1.time = mChargePoint2.time = 0;
			mChargePoint1.level = mChargePoint2.level = 0;
			mChargePoint1.timerun = mChargePoint2.timerun = 0;
			mWastePoint1.time = mWastePoint2.time;
			mWastePoint1.level = mWastePoint2.level;
			mWastePoint1.timerun = mWastePoint2.timerun;
			mWastePoint2.time = timepointS;
			mWastePoint2.level = level;
			mWastePoint2.timerun = timerealrun;

			if ((mWastePoint1.time != 0) && (mWastePoint1.level != 0)) {
				try {
					int t = mWastePoint2.time - mWastePoint1.time;
					int trun = mWastePoint2.timerun - mWastePoint1.timerun;
					float bv = (float) (mWastePoint1.level - mWastePoint2.level)
							/ (float) t;
					if (t <= 0 || bv <= 0f || trun <= 0) {
						mWastePoint1.time = mWastePoint2.time = 0;
						mWastePoint1.level = mWastePoint2.level = 0;
						mWastePoint1.timerun = mWastePoint2.timerun = 0;
						return;
					}
					calcAverageBV(t, bv, trun);
				} catch (Exception e) {
					Log.e(TAG, "setpoint at the bv value error ");
				}
			}
		}
	}

	private void calcChargeTime() {

	}

	private void calcAverageBV(int time, float bv, int timerun) {

		SharedPreferences sharePrefs = mContext.getSharedPreferences(
				BATTERY_COLLECT_PREFS, 0);
		float batterytimeall = sharePrefs.getFloat(BATTERY_USE_TIMEALL, 0f);
		float averagebv = sharePrefs.getFloat(BATTERY_AVERAGEBV, 0f);
		float batterytimerunall = sharePrefs.getFloat(BATTERY_USE_TIMERUNALL,
				0f);

		float newbatterytimeall = batterytimeall + (float) time;
		float newaveragebv = (averagebv * batterytimeall + bv * (float) time)
				/ newbatterytimeall;
		float newbatterytimerunall = batterytimerunall + (float) timerun;

		SharedPreferences.Editor editor = sharePrefs.edit();
		editor.putFloat(BATTERY_USE_TIMEALL, newbatterytimeall);
		editor.putFloat(BATTERY_AVERAGEBV, newaveragebv);
		editor.putFloat(BATTERY_USE_TIMERUNALL, newbatterytimerunall);
		editor.commit();

		mBattCollectTimeAll = newbatterytimeall;
		mAverageCollectBV = newaveragebv;
		mBattCollectRealRunRate = newbatterytimerunall / mBattCollectTimeAll;

		Log.d(TAG, "calcAverageBV time=" + time + "  bv= " + bv
				+ " mBattCollectTimeAll= " + mBattCollectTimeAll
				+ "   mAverageCollectBV=" + mAverageCollectBV
				+ " batterytimerunall" + batterytimerunall);

		String log = new String("   [setNewpointBV:batterytimeall"
				+ batterytimeall + " averagebv=" + averagebv + " time=" + time
				+ " bv=" + bv + " newbatterytimeall=" + newbatterytimeall
				+ " newaveragebv=" + newaveragebv + " newbatterytimerunall"
				+ newbatterytimerunall + " ]   ");
		writeLOGTOSD(log);

	}

	private void getBattCollectRealRunRate() {
		SharedPreferences sharePrefs = mContext.getSharedPreferences(
				BATTERY_COLLECT_PREFS, 0);
		float batterytimeall = sharePrefs.getFloat(BATTERY_USE_TIMEALL, 0f);
		float batterytimerunall = sharePrefs.getFloat(BATTERY_USE_TIMERUNALL,
				0f);
		if (batterytimerunall != 0f && batterytimeall != 0f) {
			mBattCollectRealRunRate = batterytimerunall / batterytimeall;
		} else {
			mBattCollectRealRunRate = 0.1f;
		}
	}

	private void getAverageBV() {

		SharedPreferences sharePrefs = mContext.getSharedPreferences(
				BATTERY_COLLECT_PREFS, 0);

		mBattCollectTimeAll = sharePrefs.getFloat(BATTERY_USE_TIMEALL, 0f);
		mAverageCollectBV = sharePrefs.getFloat(BATTERY_AVERAGEBV, 0f);
	}

	private float getBV() {

		float bv;

		if (mBattModeChange) {

			int brightness_percentage = mBattMode.getBrightness();
			boolean wifion = mBattMode.getWiFiOn();
			boolean bluetoothon = mBattMode.getBluetoothOn();
			boolean radioon = mBattMode.getRadioOn();
			boolean mobiledataon = mBattMode.getMobileDataOn();
			int timeout = mBattMode.getTimeout();
			boolean syncon = mBattMode.getSyncOn();
			boolean rotationon = mBattMode.getRotationOn();
			boolean vibrationon = mBattMode.getVibrationOn();
			boolean hapticon = mBattMode.getHapticOn();

			Log.d(TAG, "modeName=" + mBattMode.getModeName()
					+ ", brightness = " + brightness_percentage + ", wifion="
					+ wifion + ", bluetoothon=" + bluetoothon + ", radioon="
					+ radioon + ", mobiledataon=" + mobiledataon + ", timeout="
					+ timeout);

			if (-1 == brightness_percentage) {
				brightness_percentage = 40;
			}

			float bv1_display = DEFAULT_BV1_DISPLAY + DEFAULT_BV1_BACKLIGHT
					* (float) brightness_percentage / 100f;
			float bv2_wifi = DEFAULT_BV2_WIFI;
			float bv3_bt = DEFAULT_BV3_BT;
			float bv4_cell_standby = DEFAULT_BV4_CELL_STANDBY;
			float bv5_voice_calls = DEFAULT_BV5_VOCIL_CALLS;
			float bv6_android_system = DEFAULT_BV6_ANDROID_SYSTEM;
			float bv7_phone_idle = DEFAULT_BV7_PHONE_IDLE;
			float bv8_data_calls = DEFAULT_BV8_DATA_CALLS;

			if (!wifion) {
				bv2_wifi = 0f;
			}
			if (!bluetoothon) {
				bv3_bt = 0f;
			}
			if (!radioon) {
				bv4_cell_standby = bv5_voice_calls = 0f;
			}
			if (!mobiledataon) {
				bv8_data_calls = 0f;
			}

			Log.d(TAG, "bv1_display = " + bv1_display + "  bv2_wifi="
					+ bv2_wifi + "  bv3_bt=" + bv3_bt);

			bv = bv1_display * mRate1Display + bv2_wifi * mRate2WiFi + bv3_bt
					* mRate3BlueTooth + bv4_cell_standby * mRate4CellStandBy
					+ bv5_voice_calls * mRate5VoiceCalls + bv6_android_system
					* mRate6AndroidSystem + bv7_phone_idle * mRate7PhoneIdle
					+ bv8_data_calls * mRate8DataCalls;

			Log.d(TAG, "getBV  bv=" + bv);

			if (syncon)
				bv += DEFAULT_HOUR_WASTE;

			if (rotationon)
				bv += DEFAULT_HOUR_WASTE * 0.25f;

			if (vibrationon)
				bv += DEFAULT_HOUR_WASTE * 0.25f;

			if (hapticon)
				bv += DEFAULT_HOUR_WASTE * 0.25f;

			if (-1 == timeout) {
				timeout = 35 * 60 * 1000;
			}
			float backlighttimeout = (float) timeout * DEFAULT_HOUR_WASTE
					* 1.5f / (35 * 60 * 1000f);
			bv += backlighttimeout;

			Log.d(TAG, "getBV  bv=" + bv);
			mTempBattModeBV = bv;
			synchronized (mBattMode) {
				mBattModeChange = false;
			}
			Log.d(TAG, "setBatteryData: set mBattModeChange false");
		} else {
			bv = mTempBattModeBV;
		}

		if (mBattCollectTimeAll > COLLECT_PERIOD / 1000
				&& mAverageCollectBV > 0f) {
			bv = mAverageCollectBV / mDefaultBV * bv;
		}

		Log.d(TAG, "getBV mBattCollectTimeAll= " + mBattCollectTimeAll
				+ "   mAverageCollectBV=" + mAverageCollectBV);
		Log.d(TAG, "getBV mDefaultBV= " + mDefaultBV + "   bv=" + bv);

		return bv;

	}

	private int adjustRemainTime(int remainTime) {

		int max = mBattLevel * MAX_TIME_FOR_FULL_BATT / 100;
		int min = mBattLevel * MIN_TIME_FOR_FULL_BATT / 100;
		int ret = remainTime;
		if (remainTime < min) {
			ret = min;
			Log.d(TAG, "adjustRemainTime: " + ret);
		} else if (remainTime > max) {
			ret = max;
			Log.d(TAG, "adjustRemainTime: " + ret);
		}
		return ret;

	}

	private int getelapsetime() {
		long time = SystemClock.elapsedRealtime();
		long timerun = SystemClock.uptimeMillis();

		long elapsetime = (time - mStartTime) / 1000;
		long elapsetimerun = (timerun - mStartTimeRun) / 1000;

		if (mBattCollectRealRunRate == 0f) {
			Log.e(TAG, "getelapsetime error: ");
			return (int) elapsetime;
		}
		int ret = (int) (elapsetimerun / mBattCollectRealRunRate / 2);

		if (ret > (int) elapsetime * 5) {
			Log.e(TAG, "getelapsetime: ret is max,must be adjust");
			ret = (int) elapsetime * 5;
		} else if (ret < (int) elapsetime / 10) {
			Log.e(TAG, "getelapsetime: ret is min,must be adjust");
			ret = (int) elapsetime / 10;
		}

		return ret;
	}

	public int getRemainTime() {

		if (!mBoolInitBattInfo) {
			Log.e(TAG, "getRemainTime: mBoolInitBattInfo false ");
			return 0;
		}

		// if(!mBoolCharge){
		float bv = getBV();
		if (0f == bv) {
			Log.e(TAG, "getRemainTime: bv 0 ");
			return 0;
		}
		double timeRemain = mBattLevel / bv;
		int elapsetimeS = getelapsetime();

		int timeleftminutes = (int) ((timeRemain - elapsetimeS) / 60f);
		int adjusttimeremainM = timeleftminutes;

		if (mBattLevel > 20) {
			adjusttimeremainM = (int) ((mBattLevel - 10) / bv / 60f);
		} else if (mBattLevel > 2) {
			adjusttimeremainM = (int) ((mBattLevel - 3) / bv / 60f);
		}

		Log.d(TAG, "getRemainTime elapsetimeS: " + elapsetimeS +" timeRemain"+ timeRemain
				+ " timeleftminutes" + timeleftminutes + " adjusttimeRemainM"
				+ adjusttimeremainM +" mBattCollectRealRunRate"+ mBattCollectRealRunRate);

		if (timeleftminutes < adjusttimeremainM) {
			timeleftminutes = adjusttimeremainM;
			Log.d(TAG, "getRemainTime adjust timeleftminutes: "
					+ timeleftminutes);
		}
		return adjustRemainTime(timeleftminutes);
		// }
		// return 0;

	}

	synchronized public void setBattInfo(int level, int plugged) {

		if ((plugged != 0) && (!mBoolCharge)) { // start to charge
			mBoolCharge = true;
			Log.d(TAG, "setBattInfo: " + mBoolCharge);
			mChargingStartTime = SystemClock.elapsedRealtime();

			String log = new String("   [setbattinfo:plugged" + plugged
					+ " timenowS=" + (SystemClock.elapsedRealtime() / 1000)
					+ " at" + (System.currentTimeMillis() / 1000) + " ]   ");
			writeLOGTOSD(log);
		} else if ((plugged == 0) && (mBoolCharge)) { // stop charge
			mBoolCharge = false;
			Log.d(TAG, "setBattInfo: " + mBoolCharge);
			if((SystemClock.elapsedRealtime() - mChargingStartTime) > 2*60*1000){
				resetStartTime();
			}

			String log = new String("   [setbattinfo:plugged" + plugged
					+ " timenowS=" + (SystemClock.elapsedRealtime() / 1000)
					+ " ]   ");
			writeLOGTOSD(log);
		}

		if (mBattLevel != level) {
			Log.d(TAG, "setBattInfo: level" + level + " mBattLevel"
					+ mBattLevel);
			if(setNewPoint(level, mBattLevel)){
				mBattLevel = level;
				resetStartTime();
			}

			if (!mBoolInitBattInfo) {
				mBoolInitBattInfo = true;
			}

			collectUseRate();
		}

		Log.i(TAG, "setBattInfo: level" + level + " mBattLevel" + mBattLevel);

	}

	public void setBatteryData(BatteryModeData data) {
		synchronized (mBattMode) {
			mBattMode = data;
			mBattModeChange = true;
		}
		Log.d(TAG, "setBatteryData: mode change");
	}

	private void writeLOGTOSD(String log) {
		if (false) { // temp log on SD card
			FileUtils filewrite = new FileUtils();
			filewrite.write2SDFromInput("batt", "log.txt", log);
		}
	}

}
