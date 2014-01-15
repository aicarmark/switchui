package com.motorola.numberlocation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;
//import java.net.URLConnection;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpStatus;
//import org.apache.http.StatusLine;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.params.HttpClientParams;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.params.HttpConnectionParams;
//import org.apache.http.params.HttpParams;
//import org.apache.http.params.HttpProtocolParams;
//import org.xmlpull.v1.XmlPullParser;
//import org.xmlpull.v1.XmlPullParserException;
//import org.xmlpull.v1.XmlPullParserFactory;

import com.motorola.numberlocation.WebUpdateException;

//import android.content.Context;
//import android.os.Environment;
import android.content.Context;
import android.os.StatFs;
import android.util.Log;
//import android.util.TimeFormatException;


public class WebUpdateDownload {

	private static final String TAG = "WebUpdateDownload";

	private static String mLocalStoragePath = null;
	private static GZIPInputStream mGZIPInputStream = null;

	public static NumberLocationUpdateData getUpdateData(Context cxt, String version, UpgradeDatabaseTask Task)
	throws WebUpdateException {
		Log.d(TAG, ">> getUpdateData : " + version);

		NumberLocationUpdateData UpdateData = null;
		Reader response = null;

		response = queryWebUpdate(cxt,version,Task);
		if (response != null) {
			UpdateData = WebResponseParser.parseWebResponse(response);
		}else{
			UpdateData = new NumberLocationUpdateData();
			UpdateData.setStatus(NumberLocationConst.STATUS_NETWORK_FAIL);
			return UpdateData;
		}
		try {
			mGZIPInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
/*		
		if (UpdateData.getStatus() == NumberLocationConst.STATUS_NOT_FOUND) {
			throw new WebUpdateException("## No new vesion!");
		} else if (UpdateData.getStatus() == NumberLocationConst.STATUS_REQ_PARM_ERROR) {
			throw new WebUpdateException("## request parameter error!");
		}
*/		
		return UpdateData;
	}
    
    public static NumberLocationUpdateData getLocalUpdateData(Context cxt, String version, UpgradeDatabaseTask Task)
    throws WebUpdateException {
        Log.d(TAG, ">> getUpdateData : " + version);
        
        NumberLocationUpdateData UpdateData = null;
        Reader response = null;
        
        response = queryWebUpdate(cxt,version,Task);
        if (response != null) {
            UpdateData = WebResponseParser.parseWebResponse(response);
        }else{
            UpdateData = new NumberLocationUpdateData();
            UpdateData.setStatus(NumberLocationConst.STATUS_NETWORK_FAIL);
            return UpdateData;
        }
        try {
            mGZIPInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*      
        if (UpdateData.getStatus() == NumberLocationConst.STATUS_NOT_FOUND) {
            throw new WebUpdateException("## No new vesion!");
        } else if (UpdateData.getStatus() == NumberLocationConst.STATUS_REQ_PARM_ERROR) {
            throw new WebUpdateException("## request parameter error!");
        }
         */     
        return UpdateData;
    }
    public static String getDownloadDestDir(UpgradeDatabaseTask task) {
//        String ret = Environment.getDownloadCacheDirectory().getPath() + "/nl_update";;
        String ret = task.getmApplicationRootPath() + "/nl_update";
        mLocalStoragePath = ret;
        return  ret;
    }

    
	private static boolean removeDirectory(File file) {
		if (file.exists()) {
			if (file.isFile()) {
                if(false == file.delete())
                    return false;
			} else if (file.isDirectory()) {
				File files[] = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					removeDirectory(files[i]);
				}
			}
            if(false == file.delete())
                return false;
		} else {
			Log.v(TAG, "file not exist!");
			return false;
		}
		return true;
	} 
	
	public static boolean isCanSave(String downloadDstDir,String saveFileName, int contentLength) {

		File base = new File(downloadDstDir);
		try {
			if (base.exists()) {
				Log.v(TAG, "store file under " + downloadDstDir);
				if (!removeDirectory(base)) {
					Log.v(TAG, "failed to remove " + downloadDstDir);
					return false;
				}
			}
			if (!base.mkdir()) {
				Log.v(TAG, "failed to create " + downloadDstDir);
				return false;
			}
		} catch (Exception e) {
			Log.v(TAG, "failed to create " + downloadDstDir + ":" + e);
		}
		
		/*
		 * Check whether there's enough space on the target filesystem to save
		 * the file. Put a bit of margin (in case creating the file grows the
		 * system by a few blocks).
		 */
		StatFs stat = new StatFs(base.getPath());
		int blockSize = stat.getBlockSize();
		for (;;) {
			int availableBlocks = stat.getAvailableBlocks();
			Log.v(TAG, "available space is " + blockSize
					* ((long) availableBlocks - 4));
			Log.v(TAG, "contentLength is " + contentLength);
			if (blockSize * ((long) availableBlocks - 4) >= contentLength) {
				break;
			}
//			stat.restat(base.getPath());
		}
		return true;
	}


    
    public static String downloadFromUrl(String updateURL, UpgradeDatabaseTask task) {
		String fileName = null;
		try {
			
//			File dir = new File(root.getAbsolutePath() + "/number_location");
			// dir.mkdirs();

			URL url = new URL(updateURL); // you can write here any link
			Log.d(TAG, "download url: " + url);


			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.connect();
			
			int httpStatus = httpConnection.getResponseCode();
			Log.d(TAG, "downloaded connection status: " + httpStatus);
			URL realUrl = httpConnection.getURL();
			String filePath = realUrl.getPath();
			int fileIndex= filePath.lastIndexOf("/")+1;
			
			fileName = filePath.substring(fileIndex,filePath.length());
			Log.d(TAG, "downloaded file name: " + fileName);
			
			// this will be useful so that you can show a tipical 0-100%
			// progress bar
			int lenghtOfFile = httpConnection.getContentLength();
			
			// get the destination directory
			String downloadDstDir = getDownloadDestDir(task);
			if(!isCanSave(downloadDstDir, fileName ,lenghtOfFile))
				return null;
			File file = new File(downloadDstDir, fileName);

			// downlod the file
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(file.getAbsolutePath());

			byte data[] = new byte[1024];

			long total = 0;
			int count = 0;
			int progress = 0;
			
			while ((count = input.read(data)) != -1) {
				total += count;
				// publishing the progress....
				progress = (int) (total * 100 / lenghtOfFile);
				task.reportProgress(NumberLocationConst.PROGRESS_TYPE_DOWNLOAD,progress);
				output.write(data, 0, count);
			}

			output.flush();
			output.close();
			input.close();
		} catch(UnknownHostException e){
			e.printStackTrace();
			Log.e(TAG,"error: "+e);
		}catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG,"error: "+e);
		}
		return fileName;
	}

    /*		
	HttpParams params = new BasicHttpParams();                               
	HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);            
	HttpConnectionParams.setSoTimeout(params, 20 * 1000);                    
	HttpConnectionParams.setSocketBufferSize(params, 8192);                  
	HttpClientParams.setRedirecting(params, true); String userAgent =        
		"(Linux; Android)"; HttpProtocolParams.setUserAgent(params,              
				userAgent);                                                              

		HttpClient client = new DefaultHttpClient(params); HttpGet request =     
			new HttpGet(String.format(WEBUPDATE_URL, version)); //                   
			request.setHeader("User-Agent", "(Linux; Android)");                     

			try { HttpResponse response = client.execute(request); StatusLine        
			status = response.getStatusLine(); Log.d(TAG, ">> response status:" +    
					status);                                                                 

			if (status.getStatusCode() != HttpStatus.SC_OK) { throw new              
				IOException("## Unexpected Http status code!"); }                        

			HttpEntity entity = response.getEntity(); 
			responseReader = new InputStreamReader(entity.getContent()); 
			// responseReader = new  InputStreamReader(entity.getContent(), // "GB2312");                     
			} catch (IOException e) { throw new                                      
				WebUpdateException("## web services error!", e); }                       
*/    
	public static Reader queryWebUpdate(Context cxt, String version, UpgradeDatabaseTask task)
			throws WebUpdateException {
		if (version == null) {
			throw new WebUpdateException("## Invalid version!");
		}

		Log.d(TAG, ">> queryWebservice:"
				+ String.format(NumberLocationConst.WEBUPDATE_URL, version));

		Reader responseReader = null;
		
		String webRequestUrl = NumberLocationUtilities.checkSDCardUrl(cxt);
		
        String updateFileName = downloadFromUrl(String.format(webRequestUrl,version), task);
//		String updateFileName = DownloadFromUrl(String.format(NumberLocationConst.WEBUPDATE_URL,
//				version), task);
		if(updateFileName==null)
			return null;
		try {
			FileInputStream fis = new FileInputStream(mLocalStoragePath
					+ File.separator + updateFileName);
			GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(
					fis));
			mGZIPInputStream = zis;
			BufferedReader br = new BufferedReader(new InputStreamReader(zis));
			responseReader = br;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseReader;
	}


}
