package com.motorola.mmsp.render.util;

import java.io.Writer;

import com.motorola.mmsp.render.MotoConfig;

import android.util.Log;

public class LogWriter extends Writer {
	private static final String TAG = MotoConfig.TAG;
	private StringBuilder mBuilder = new StringBuilder();

	@Override
	public void close() {
		flushBuilder();
	}

	@Override
	public void flush() {
		flushBuilder();
	}

	@Override
	public void write(char[] buf, int offset, int count) {
		for (int i = 0; i < count; i++) {
			char c = buf[offset + i];
			if (c == '\n') {
				flushBuilder();
			} else {
				mBuilder.append(c);
			}
		}
	}

	private void flushBuilder() {
		if (mBuilder.length() > 0) {
			Log.d(TAG, mBuilder.toString());
			mBuilder.delete(0, mBuilder.length());
		}
	}
}