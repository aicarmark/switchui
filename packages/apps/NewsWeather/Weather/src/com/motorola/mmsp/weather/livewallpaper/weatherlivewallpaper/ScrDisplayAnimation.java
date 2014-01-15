package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;
import com.motorola.mmsp.weather.R;

import com.motorola.mmsp.render.MotoPicture;
import com.motorola.mmsp.render.gl.MotoGLRenderPlayer;
import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider.WeatherInfo;
import com.motorola.mmsp.render.util.MotoScreenLayout;

public class ScrDisplayAnimation extends ScrDisplayBase {

	private static String TAG = "LiveWallpaperService";
	private boolean mSurfaceChanged;
	public MotoGLRenderPlayer mPlayer;
	private static final int EVERY_FRAME_TIME = 130;
	private SurfaceHolder mSurfaceHolder;
	public String file = null;
	private float mOffset = (float) 0.5;
	private int srcWidth;
	private int srcHeight;
	private Bitmap mPicTextBitmap;
	private Bitmap mOldBitmap;// for recycle
	private final String mSunRise = "6:00AM";
	private final String mSunSet = "6:00PM";
	private boolean visible = false;// LiveWallpaper SurfaceView
	private int rightHeight = 0;
	private int rightWidth = 0;

	public ScrDisplayAnimation() {
		super();
		// mPlayer.setDrawingCacheEnable(false);
		// try {
		// initPlayerRes();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	public void desiredSizeChanged() {

	}

	public void offsetsChanged(float xOffset, float yOffset, float xStep,
			float yStep, int xPixels, int yPixels) {
		if (xStep == 0.0)
			return;
		int offset = (xPixels + 240);
		if (mPlayer != null) {
			// mPlayer.setOffset(offset, 0);
		}

	}

	public void setVideo(String path) {

	}

	public void unInit() {
		// mIsPrepare = false;
		destory();
	}

	protected void drawCube(Canvas canvas) {

	}

	public void start(boolean isPreview) {
		/*
		 * mIsPrepare = true; if (mIsPrepare) {
		 */
		mCubeEngine.configurationChanged();
		mWnp.updateWeatherInfoFromDB(mContext);
		updateVideo(isPreview);
		// mPlayer.play();
		// }
	}

	public void unRegisterTimer() {
		super.unRegisterTimer();
	}

	private void initPlayerRes() {
		try {
			int srcWidthId = mContext.getResources().getIdentifier(
					"com.motorola.mmsp.weather:integer/width", null, null);
			if (srcWidthId > 0) {
				srcWidth = mContext.getResources().getInteger(srcWidthId);
			} else {
				srcWidth = 480;
			}

			int srcHeightId = mContext.getResources().getIdentifier(
					"com.motorola.mmsp.weather:integer/height", null, null);
			if (srcHeightId > 0) {
				srcHeight = mContext.getResources().getInteger(srcHeightId);
			} else {
				srcHeight = 854;
			}
			Resources res = mContext.getResources();
			int resId = res.getIdentifier("com.motorola.mmsp.weather:xml/"
					+ file, null, null);
			android.util.Log.i(TAG, "file = " + file + " res=" + res
					+ ", resId=" + resId);
			mPlayer.setRes(res, resId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateVideo(boolean isPreview) {
		WeatherInfo info = mWnp.getWeatherInfo();
		String cityName = info.city;
		if (info.city == null || info.city.equals("") 
				|| info.condition == null || info.condition.equals("")) {
			// use default
			file = "livewallpaper_weather_sunny";
		} else {
			// set daytime or night
			String timeZone = info.timeZone;
			TimeZone tz = null;
			if (timeZone == null || timeZone.equals("")) {// sina
				tz = TimeZone.getDefault();
			} else {// accu
				if (timeZone.contains("-")) {
					tz = TimeZone.getTimeZone("GMT" + timeZone);
				} else {
					tz = TimeZone.getTimeZone("GMT+" + timeZone);
				}
			}

			Calendar calendar = Calendar.getInstance(tz);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(calendar.MINUTE);
			String mF1 = "HH:mm";
			boolean isSuRise = isDayOrNight(mSunRise, hour, minute, mF1); // true
			boolean isSuSet = isDayOrNight(mSunSet, hour, minute, mF1);// false
			String strDayTime = null;
			String weatherType = mCubeEngine.getWeatherTypeStr(info);

			int dayNight = 0;
			if (isSuRise && !isSuSet) {
				// weather.mInfomation.mDayNight = WeatherInfo.DAY;
				strDayTime = "day";
			} else {
				strDayTime = "night";
			}

			if ((weatherType.equals("sunny") || weatherType.equals("cloudy"))
					&& strDayTime.equals("night")) {
				file = "livewallpaper_weather_" + weatherType + "_"
						+ strDayTime;
			} else {
				file = "livewallpaper_weather_" + weatherType;
			}
		}
		Log.d(TAG, "ScrDisplayPicture1 file:" + file);
		if ((!isSameFile(file))) {
			super.mCurPlayFile = file;
			if (mForeignPackage != null && mForeignPackage.getContext() != null) {
				//
			} else if (mForeignPackage == null) {
				Log.d(TAG, "ScrDisplayPicture1 the mForeignPackage is null.");
			} else {
				Log.d(TAG, "ScrDisplayPicture1 the context is null.");
			}
			initVideo(isPreview);
		} else {
			if (mPlayer == null) {
				mPlayer = new MotoGLRenderPlayer();
				initPlayerRes();
				Log.d(TAG, "init @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@1111");
			}
			drawCityName();
			play(isPreview);
		}
	}

	private void play(boolean isPreview) {
		mSurfaceChanged = true;
		if (mPlayer != null) {
			mPlayer.setSurfaceHolder(mSurfaceHolder);
			mPlayer.setRect(mSurfaceHolder != null ? mSurfaceHolder
					.getSurfaceFrame() : null);
		}
		MotoScreenLayout layout = new MotoScreenLayout();
		int height = mContext.getResources().getDisplayMetrics().widthPixels;
		int width = mContext.getResources().getDisplayMetrics().heightPixels;
		layout.convert(srcWidth, srcHeight, width, height,
				MotoScreenLayout.SCALE_MODE_UPPER_LEFT_CUT_LONG_EDGE);
		int screen = mContext.getResources().getInteger(R.integer.screen);
		Log.d(TAG, "screen is " + screen + " isPreview is " + isPreview);
		if (screen == 1 && isPreview) {
			// land
			if (mPlayer.getRootPicture() != null) {
				mPlayer.getRootPicture().setX(0);
				mPlayer.getRootPicture().setY(width);
				mPlayer.getRootPicture().setScaleX(layout.getScaleX());
				mPlayer.getRootPicture().setScaleY(layout.getScaleY());
				mPlayer.getRootPicture().setRotate(-90);
				mPlayer.getRootPicture().setRotatePx(0);
				mPlayer.getRootPicture().setRotatePy(0);
			}
			// screen = 0;
		} else {
			//modify by amt_xulei for SWITCHUI-2888 EG808T_P004560 2012-9-24
			if (mSurfaceHolder.getSurfaceFrame().width() > mSurfaceHolder.getSurfaceFrame().height()){
				Log.d(TAG, "surfaceHolder width and height is wrong");
				mSurfaceHolder.getSurfaceFrame().right = rightWidth == 0 ? 540 : rightWidth;
				mSurfaceHolder.getSurfaceFrame().bottom = rightHeight == 0 ? 960 : rightHeight;
				mPlayer.setSurfaceHolder(mSurfaceHolder);
				mPlayer.setRect(mSurfaceHolder.getSurfaceFrame());
			}
			else{
				if (rightWidth == 0){
					rightHeight = mSurfaceHolder.getSurfaceFrame().height();
					rightWidth = mSurfaceHolder.getSurfaceFrame().width();
				}
			}
			//end modify by amt_xulei for SWITCHUI-2888 EG808T_P004560 2012-9-24
			layout = new MotoScreenLayout();
			width = mContext.getResources().getDisplayMetrics().widthPixels;
			height = mContext.getResources().getDisplayMetrics().heightPixels;
			layout.convert(srcWidth, srcHeight, width, height,
					MotoScreenLayout.SCALE_MODE_UPPER_LEFT_CUT_LONG_EDGE);
			if (mPlayer.getRootPicture() != null) {
				mPlayer.getRootPicture().setX(layout.getX());
				mPlayer.getRootPicture().setY(layout.getY());
				mPlayer.getRootPicture().setScaleX(layout.getScaleX());
				mPlayer.getRootPicture().setScaleY(layout.getScaleY());
				if (mPlayer.getRootPicture().getRotate() == -90){
					mPlayer.getRootPicture().setRotate(0);
				}
			}
		}
		if (mPlayer != null) {
			if (MotoGLRenderPlayer.PLAYER_RUNNING != mPlayer.getStatus()) {
				if (this.visible) {
					mPlayer.play();
				}
			}
		}
		if(mOldBitmap != null && !mOldBitmap.isRecycled() && mPlayer.findPictureById(R.id.pic_text) != null && !mOldBitmap.equals(mPlayer.findPictureById(R.id.pic_text).getBitmap())) {
			mOldBitmap.recycle();
			mOldBitmap = null;
		}
	}

	private void drawCityName() {
		TextView tv = mCubeEngine.getText();
		tv.setGravity(Gravity.RIGHT);
		mCubeEngine.updateWeatherTextInfo(tv);
		if(mPicTextBitmap != null) {
			//modify by 002777 for switchui-2230
			mOldBitmap = mPicTextBitmap;
//			mOldBitmap.recycle();
//			mOldBitmap = null;
			//end modify by 002777 for switchui-2230
			mPicTextBitmap = null;
		}
		mPicTextBitmap = Bitmap.createBitmap(256, 128, Bitmap.Config.ARGB_8888);
		Log.d(TAG, "createBitmap ####################");
		mCanvas = new Canvas(mPicTextBitmap);
		tv.layout(0, 0, 256, 128);
		tv.draw(mCanvas);

		MotoPicture pictext = mPlayer.findPictureById(R.id.pic_text);
		if (tv.getVisibility() == View.VISIBLE) {
			pictext.setBitmap(mPicTextBitmap);
		} else {
			pictext.setBitmap(null);
		}
//		if(mOldBitmap != null && !mOldBitmap.isRecycled()) {
//			mOldBitmap.recycle();
//			mOldBitmap = null;
//		}
	}

	private void initVideo(boolean isPreview) {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		if (mPlayer == null) {
			mPlayer = new MotoGLRenderPlayer();
			initPlayerRes();
			Log.d(TAG, "init @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		}
		drawCityName();
		play(isPreview);

	}

	public void destory() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPicTextBitmap.recycle();
			mPlayer = null;
		}
	}

	public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		if (mPlayer != null) {
			mPlayer.setSurfaceHolder(mSurfaceHolder);
			// mPlayer.setRenderTarget(mSurfaceHolder);
		}
	}

	public void setSurfaceVisible(boolean visible) {
		this.visible = visible;
	}

	public void stop() {
		if (mPlayer != null) {
			mPlayer.stop();
		}
	}

	public boolean isDayOrNight(String mFirstTime, int mHour, int mMinute,
			String mFormat2) {
		try {
			String firstTime = mFirstTime.trim();
			long hour;
			long minute;
			if (firstTime.contains("PM")) {
				firstTime = firstTime.replace("PM", "");

				String time[] = firstTime.split(":");
				hour = Integer.valueOf(time[0]) + 12;

				if (time[1].contains("0") && time[1].indexOf("0") == 0) {
					String a = time[1].replace("0", "").trim();
					if ("".equals(a)) {
						minute = 0;
					} else {
						minute = Integer.valueOf(a.trim());
					}
				} else {
					minute = Integer.valueOf(time[1].trim());
				}

			} else {
				firstTime = firstTime.replace("AM", "");
				String time[] = firstTime.split(":");
				hour = Integer.valueOf(time[0]);

				if (time[1].contains("0") && time[1].indexOf("0") == 0) {
					String a = time[1].replace("0", "").trim();
					if ("".equals(a)) {
						minute = 0;
					} else {
						minute = Integer.valueOf(a.trim());
					}
				} else {
					minute = Integer.valueOf(time[1].trim());
				}
			}

			if (hour - mHour < 0 || hour - mHour == 0 && minute - mMinute <= 0) {
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void pause() {
		mPlayer.pause();
		Log.d(TAG, "mPlayer staus = " + mPlayer.getStatus() + "pause ="
				+ mPlayer.PLAYER_PAUSE);
	}
}