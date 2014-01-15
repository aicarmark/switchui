package com.android.contacts;

import android.os.StatFs;

public class RomUtility {
	
	public static boolean isOutofMemory() {
		long MIN_SPACE = 1048576;// 1024*1024
		long freeStorage = 0;
		StatFs mDataFileStats = new StatFs("/data");
		mDataFileStats.restat("/data");
		try {
			freeStorage = (long) mDataFileStats.getAvailableBlocks()
					* mDataFileStats.getBlockSize();
		} catch (IllegalArgumentException e) {
		}
		if (freeStorage < MIN_SPACE) {
			return true;
		}
		return false;
	}
	
}
