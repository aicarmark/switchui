package com.motorola.mmsp.render;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MotoBitmapWrapper {
	public int mId;
	public int mRef;
	public Bitmap mBitmap;
	
	public static final ArrayList<MotoBitmapWrapper> sGlobalBitmaps
						= new ArrayList<MotoBitmapWrapper>(100);
	
	private MotoBitmapWrapper() {
		
	}
	
	public static final int indexOf(int id) {
		for (int i = 0, size = sGlobalBitmaps.size(); i < size; i++) {
			MotoBitmapWrapper wrapper = sGlobalBitmaps.get(i);
			if (wrapper != null && wrapper.mId == id) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static final int indexOf(Bitmap bitmap) {
		for (int i = 0, size = sGlobalBitmaps.size(); i < size; i++) {
			MotoBitmapWrapper wrapper = sGlobalBitmaps.get(i);
			if (wrapper != null && wrapper.mBitmap == bitmap) {
				return i;
			}
		}
		
		return -1;
	}
	
	public final static Bitmap decodeBitmap(Resources res, int id) {
		synchronized (sGlobalBitmaps) {
			int index = indexOf(id);
			MotoBitmapWrapper wrapper = null;
			if (index >= 0) {
				wrapper = sGlobalBitmaps.get(index);
			}
			
			if (wrapper == null) {
				wrapper = new MotoBitmapWrapper();
			}
			if (wrapper.mBitmap == null
					|| wrapper.mBitmap.isRecycled()) {
				wrapper.mId = id;
				wrapper.mBitmap = BitmapFactory.decodeResource(res, id);
				if (index >= 0) {
					sGlobalBitmaps.set(index, wrapper);
				} else {
					sGlobalBitmaps.add(wrapper);
				}
			}
			
			wrapper.mRef++;
			return wrapper.mBitmap;
		}
	}
	
	public final static void recycle(int id) {
		synchronized (sGlobalBitmaps) {
			int index = indexOf(id);
			if (index >= 0) {
				MotoBitmapWrapper wrapper = sGlobalBitmaps.get(index);
				if (wrapper != null) {
					wrapper.mRef--;
					if (wrapper.mRef <= 0) {
						if (wrapper.mBitmap != null && !wrapper.mBitmap.isRecycled()) {
							wrapper.mBitmap.recycle();
						}
						sGlobalBitmaps.remove(index);
					}
				}
			}
		}
	}
	
	public final static void recycleForce(int id) {
		synchronized (sGlobalBitmaps) {
			int index = indexOf(id);
			MotoBitmapWrapper wrapper = sGlobalBitmaps.get(index);
			if (wrapper != null 
					&& wrapper.mBitmap != null 
					&& !wrapper.mBitmap.isRecycled()) {
				wrapper.mBitmap.recycle();
			}
		}
	}
	
	public final static Bitmap reload(Resources res, int id) {
		synchronized (sGlobalBitmaps) {
			int index = indexOf(id);
			if (index >= 0) {
				MotoBitmapWrapper wrapper = sGlobalBitmaps.get(index);
				if (wrapper != null 
						&& wrapper.mBitmap != null
						&& !wrapper.mBitmap.isRecycled()) {
					return wrapper.mBitmap;
				}
			}
			
			if (index >= 0) {
				MotoBitmapWrapper wrapper = sGlobalBitmaps.get(index);
				if (wrapper != null) {
					wrapper.mBitmap = BitmapFactory.decodeResource(res, wrapper.mId);
					return wrapper.mBitmap;
				}
			} else {
				Log.e(MotoConfig.TAG, "" + id + "is not under control!!");
			}
			
			return null;
		}
	}
	
	public static final String getString() {
		synchronized (sGlobalBitmaps) {
			StringBuilder s = new StringBuilder("global bitmap = [");
			for (int i=0,size=sGlobalBitmaps.size(); i<size; i++) {
				MotoBitmapWrapper wrapper = sGlobalBitmaps.get(i);
				if (wrapper != null) {
					s.append("(" + wrapper.hashCode());
					s.append(",id=" + wrapper.mId);
					s.append(",ref=" + wrapper.mRef);
					s.append(",alive=" + (wrapper.mBitmap != null ? !wrapper.mBitmap.isRecycled() : false));
					s.append("),");
				}
				
			}
			s.append("]");
			return s.toString();
		}
	}
}
