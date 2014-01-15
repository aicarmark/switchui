/**
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 */

package com.motorola.mmsp.performancemaster.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	private String SDPATH;
	private final static String TAG = "batterylefttimelog";

	public String getSDPATH() {
		return SDPATH;
	}

	public void setSDPATH(String sDPATH) {
		SDPATH = sDPATH;
	}

	public FileUtils() {
		SDPATH = Environment.getExternalStorageDirectory() + "/";
		Log.e(TAG, "sd card's directory path:  " + SDPATH);
	}

	public File createSDFile(String fileName) throws IOException {
		File file = new File(SDPATH + fileName);
		if (!file.exists()) {
			file.createNewFile();
			Log.e(TAG, "FileUtils create new log file!");
		}
		return file;
	}

	public File createSDDir(String dirName) {
		File dir = new File(SDPATH + dirName);
		Log.e(TAG,
				"storage device's state :"
						+ Environment.getExternalStorageState());

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Log.e(TAG, "this directory real path is:" + dir.getAbsolutePath());
			Log.e(TAG, "the result of making directory:" + dir.mkdir()
					+ " it is exist? = " + dir.exists());

		}
		return dir;
	}

	public boolean isFileExist(String fileName) {
		File file = new File(SDPATH + fileName);
		Log.e(TAG, "FileUtils isFileExist fileName= " + fileName);
		return file.exists();
	}

	public File write2SDFromInput(String path, String fileName,
			String inputStream) {
		File file = null;
		BufferedWriter bufferedWriter;
		try {
			// File tempf=createSDDir(path);
			// Log.e(TAG,"FileUtils write2SDFromInput directory in the sd card: = "
			// + tempf.exists());

			file = createSDFile(path + fileName);
			Log.e(TAG, "FileUtils write2SDFromInput file in the sd card: = "
					+ file.exists());

			bufferedWriter = new BufferedWriter(new FileWriter(file, true));

			bufferedWriter.write(inputStream);

			bufferedWriter.flush();

			bufferedWriter.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileUtils write2SDFromInput 222");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "FileUtils write2SDFromInput 333");
			e.printStackTrace();
		}

		return file;
	}

}
