package com.motorola.mmsp.rss.service.parse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.util.NetworkUtil;

public class IconLoader extends Thread{
	private static final String TAG = "IconLoader";
	private Context mContext;
	private int feedId;
	private String iconUrl;
	private ImageView mImageView;
	private Handler mHandler;
	private IconLoadFinishListener mIconLoadFinishListener;
	private static final int MAX_ICON_SIZE = 50 * 1000;
	
	public IconLoader(Context context, int feedId, String iconUrl, ImageView imageview){
		mContext = context;
		this.feedId = feedId;
		this.iconUrl = iconUrl;	
		mImageView = imageview;
		mHandler = new Handler(context.getMainLooper());
	}
	
	public void run(){
		Log.d(TAG, "IconLoader run, start fetch image icon");
		ContentValues values = new ContentValues();
		try {
			String where = RssConstant.Content._ID + "=" + feedId;
			values.put(RssConstant.Content.FEED_ICON, getImage(iconUrl));
			mContext.getContentResolver().update(RssConstant.Content.FEED_URI, values, where, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			mIconLoadFinishListener.IconLoadFinish(feedId);
		}
	}
	
	
	public static byte[] getImage(String urlpath) throws Exception {
		InputStream in = NetworkUtil.getImageStream(urlpath);
		if(in != null){
			Log.d(TAG, "fetched the image input stream");
			return readStream(in);
		}else{
			Log.d(TAG, "fetched no image input stream");
			return null;
		}
	}
	
	
	public static byte[] readStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		while ((len = inStream.read(buffer)) != -1) {
			outstream.write(buffer, 0, len);
		}
		outstream.close();
		inStream.close();
		Log.d(TAG, "size is " + outstream.toByteArray().length);
		if(outstream.toByteArray().length > MAX_ICON_SIZE) {
			return null;
		}		
		return outstream.toByteArray();
	}
	
	public void setLoadFinishListener(IconLoadFinishListener listener) {
		mIconLoadFinishListener = listener;
	}
	
	public interface IconLoadFinishListener {
		public void IconLoadFinish(int feedId);
	}
}
