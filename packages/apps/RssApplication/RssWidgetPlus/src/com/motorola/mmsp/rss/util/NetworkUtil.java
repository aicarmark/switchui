package com.motorola.mmsp.rss.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkUtil {
	private static final String TAG = "NetworkUtil";
	private static final String CONNECTION_MANAGER_TIMEOUT = "http.connection-manager.timeout";
	private static final int HTTP_TIMEOUT = 1000;
	private static final int CONNECTION_TIMEOUT = 10 * 1000;
	private static final String CONNECTION = "Connection";
	private static final String KEEP_ALIVE = "Keep-Alive";
	
	public static synchronized boolean isNetworkAvailable(Context context) {
		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(cm != null) {
				NetworkInfo[] networkInfo = cm.getAllNetworkInfo();
				if(networkInfo != null) {
					for(int i = 0; i < networkInfo.length; i++) {
						if(networkInfo[i].getState().equals(NetworkInfo.State.CONNECTED)) {
							Log.d(TAG, "network is connected");
							return true;
						}
					}
					Log.d(TAG, "no network is available!");
				}
			} else {
				Log.d(TAG, "ConnectivityManager is null");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static synchronized boolean isUrlValid(String location){
		if(location == null || location.equals("")){
			return false;
		}
		if(!location.startsWith("http://")){
			location = "http://" + location;
		} 
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = null;
		try {
			get = new HttpGet(location);
			HttpParams params = client.getParams();
			if(params == null) {
				params = new BasicHttpParams();
			}
			params.setLongParameter(CONNECTION_MANAGER_TIMEOUT, HTTP_TIMEOUT);//set the timeout of connection manager
			HttpResponse response = client.execute(get);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Log.d(TAG, "url is valid");
				return true;
			} else {
				Log.d(TAG, "url is invalid, response code is " + response.getStatusLine().getStatusCode());
			}
		} catch(Exception e) {
			Log.d(TAG, "exception, url is invalid");
			e.printStackTrace();
			return false;
		} finally {
			if(get != null) {
				get.abort();
			}			
			client.getConnectionManager().shutdown();
		}
		return false;
//		return true;
	}
	
	public static synchronized InputStream getRSSInputStream(String location) {
		String tempUrl = location;
		InputStream in = null;
		if(location == null || location.equals("")){
			return null;
		}
		if(!location.startsWith("http://")){
			location = "http://";
			location += tempUrl;
		}
		boolean useProxy = false;
		try {
			if(useProxy){
				URL url = new URL(location);
				
				java.net.InetAddress addr = InetAddress
					      .getByName("10.11.19.242");
					    InetSocketAddress sa = new InetSocketAddress(addr, 3128);
					    java.net.Proxy proxy = new java.net.Proxy(Proxy.Type.HTTP, sa);
					    Authenticator.setDefault(new Authenticator() {
					     protected PasswordAuthentication getPasswordAuthentication() {
					      return new PasswordAuthentication("",
					      new String("").toCharArray());
					     }
					    });
	            
				URLConnection connection = url.openConnection(proxy);
		        connection.setConnectTimeout(10000);

		        connection.connect();
		        in = connection.getInputStream();
			}else{
				DefaultHttpClient client = new DefaultHttpClient();
				try{
					HttpGet get = new HttpGet(location);
					HttpParams params = client.getParams();
					if(params == null) {
						params = new BasicHttpParams();
					}
					HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);//set timeout for data connection
					HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);//set timeout for waiting for data
					HttpResponse response;
					response = client.execute(get);
					if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						HttpEntity entity = response.getEntity();
						if(entity!= null && entity.isStreaming()) {
							in = entity.getContent();
						}
					} else {
						Log.d(TAG, "request failed, response code is " + response.getStatusLine().getStatusCode());
					}
				} catch(HttpHostConnectException e) {
					Log.d(TAG, "network exception, rss data fetch failed");
					e.printStackTrace();
				} catch(Exception e) {
					Log.d(TAG, "other exceptions, rss data fetch failed");
					e.printStackTrace();
				}finally {
					client.getConnectionManager().closeExpiredConnections();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return in;
	}
	
	public static InputStream getImageStream(String location){
		boolean useProxy = false;
		InputStream inputStream = null;
		if(location == null || (location != null && location.equals(""))){
			return null;
		}
		try{
			if(useProxy){
				URL url = new URL(location);				
				java.net.InetAddress addr = InetAddress.getByName("10.11.19.242");
			    InetSocketAddress sa = new InetSocketAddress(addr, 3128);
			    java.net.Proxy proxy = new java.net.Proxy(Proxy.Type.HTTP, sa);
			    Authenticator.setDefault(new Authenticator() {
				     protected PasswordAuthentication getPasswordAuthentication() {
					      return new PasswordAuthentication("", new String("").toCharArray());
					      }
				     });
	            
				HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(6 * 1000);
				if (conn.getResponseCode() == 200) {
					inputStream = conn.getInputStream();
					return inputStream;
				}
				return null;
			}else{
				DefaultHttpClient client = new DefaultHttpClient();
				try{
					HttpGet get = new HttpGet(location);
					HttpParams params = client.getParams();
					if(params == null) {
						params = new BasicHttpParams();
					}
					HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);//set timeout for data connection
					HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);//set timeout for waiting for data
					HttpResponse response;
					response = client.execute(get);
					if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						HttpEntity entity = response.getEntity();
						if(entity!= null && entity.isStreaming()) {
							inputStream = entity.getContent();
						}
					} else {
						Log.d(TAG, "request failed, response code is " + response.getStatusLine().getStatusCode());
					}
				} catch(Exception e) {
					Log.d(TAG, "exception, rss data fetch failed");
					e.printStackTrace();
				} finally {
					client.getConnectionManager().closeExpiredConnections();
				}
			}
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
		return inputStream;
	}
	
}
