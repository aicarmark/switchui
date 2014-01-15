package com.motorola.mmsp.weather.sinaweather.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class WebServiceHelper {
	
	public static InputStream getConnect(Context context,String url_string) throws ParseException {
		InputStream in = null;
		Log.d("WeatherService", WeatherUtils.getTID() + " WebServiceHelper->getConnect " + url_string);
		/*
		 * boolean bRoaming = isRoaming(context); Log.v("WebServiceHelper",
		 * "isRoaming = "+bRoaming);
		 * 
		 * SharedPreferences sp = PersistenceData.getSharedPreferences(context);
		 * boolean roamingSetting =
		 * sp.getBoolean(PersistenceData.SETTING_ROAMING, false); if
		 * (roamingSetting == false && bRoaming) { return in; }
		 */

		HttpPost httpRequest = new HttpPost(url_string);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("str", "moto"));

		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, WeatherServiceConstant.SECOND_PER_MINUTE * WeatherServiceConstant.MILLISECOND_PER_SECOND);
			httpRequest.setParams(httpParams);
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
			in = httpResponse.getEntity().getContent();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		
		if (in == null) {
			Log.d("WeatherService", WeatherUtils.getTID() + "WebServiceHelper->getConnect() in is null, try another connection method ");
			String proxyHost = null; 
			String defaultProxyHost = null;
			int proxyPort = -1;
			int defaultProxyPort = -1;
			try {
				proxyHost = android.net.Proxy.getHost(context);
				defaultProxyHost = android.net.Proxy.getDefaultHost();
				proxyPort = android.net.Proxy.getPort(context);
				defaultProxyPort = android.net.Proxy.getDefaultPort();
				
				Log.d("WeatherService", WeatherUtils.getTID() + "proxyHost = " + proxyHost + "proxyPort = " + proxyPort);
				Log.d("WeatherService", WeatherUtils.getTID() + "defaultProxyHost = " + defaultProxyHost + "defaultProxyPort = " + defaultProxyPort);
				
				if (proxyHost != null) {
					Log.d("WeatherService", WeatherUtils.getTID() + "try first connection method, proxy is not null ");
					java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, 
							                              new InetSocketAddress(proxyHost, proxyPort));

					Log.d("WeatherService", WeatherUtils.getTID() + "URL(url_string).openConnection");
					URLConnection con = new URL(url_string).openConnection(p);
					con.setConnectTimeout(WeatherServiceConstant.SECOND_PER_MINUTE * WeatherServiceConstant.MILLISECOND_PER_SECOND);
					Log.d("WeatherService", WeatherUtils.getTID() + "con.getInputStream(), timeout 1 min");
					in = con.getInputStream();

				} else {
					Log.d("WeatherService", WeatherUtils.getTID() + "try first connection method, proxy is null ");
					URLConnection con = new URL(url_string).openConnection();
					con.setConnectTimeout(WeatherServiceConstant.SECOND_PER_MINUTE * WeatherServiceConstant.MILLISECOND_PER_SECOND);
					in = con.getInputStream();
				}
				
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
		} 
		
		if (in != null) {
			Log.d("WeatherService", WeatherUtils.getTID() + "WebServiceHelper->getConnect() end, content from internet is not null ");
		} else {
			Log.d("WeatherService", WeatherUtils.getTID() + "WebServiceHelper->getConnect() end, content from internet is null ");
		}

                return in;
    }
	
    public static final class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String detailMessage) {
            super(detailMessage);
        }

        public ParseException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
	
	public static boolean isRoaming(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {   
	       NetworkInfo info = connectivity.getActiveNetworkInfo();
	       if(info == null || !info.isConnected()){      
		        return false;      
		    }      
		    if(info.isRoaming()){      
		        return true;      
		    }   
		}
	    return false;
	}
	
	public static boolean isConnect(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.v("WeatherService", WeatherUtils.getTID() + "WebServiceHelper isConnect connectivity = " + connectivity);
        if (connectivity != null) {        	
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            Log.v("WeatherService", WeatherUtils.getTID() + "WebServiceHelper isConnect info = " + info);
            if (info != null) {
            	Log.v("WeatherService", WeatherUtils.getTID() + "WebServiceHelper isConnect info.getState = " + info.getState());
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;   
                }
            }
        }
        return false;   
    }  
}
