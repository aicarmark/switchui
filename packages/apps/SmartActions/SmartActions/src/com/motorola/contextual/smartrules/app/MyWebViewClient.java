/*
 * @(#)MyWebViewClient.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * csd053        2011/06/09 NA                Initial Version
 *
 */
package com.motorola.contextual.smartrules.app;

import com.motorola.contextual.smartrules.Constants;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This class provides a web view client for the web view activites..
 *
 * <code><pre>
 * CLASS:
 *  extends WebViewClient
 *
 *  implements
 *   None
 *
 * RESPONSIBILITIES:
 *  provides the interface to handle loading of a URL into a web view.
 *
 * COLLABORATORS:
 *  None
 *
 * </pre></code>
 */
public class MyWebViewClient extends WebViewClient implements Constants {

	private static final String TAG = MyWebViewClient.class.getSimpleName();
	
	private static final String URL_3GP_END = ".3gp";
	private static final String VIDEO_3GPP_TYPE = "video/3gpp";
	
	private Activity mActivity = null;
	
	/** constructor
	 * 
	 * @param activity - activity instance
	 */
	public MyWebViewClient(Activity activity) {
		mActivity = activity;
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.endsWith(URL_3GP_END)) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(Uri.parse(url), VIDEO_3GPP_TYPE);
            if(LOG_DEBUG) Log.d(TAG, " intent:" + intent);
            mActivity.startActivity(intent);
            return true;
        } 
		view.loadUrl(url);
		return true;
	}
	
    public void onPageFinished(WebView view, String url) {
    	mActivity.setProgressBarIndeterminateVisibility(false);
    }
    
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
    	mActivity.setProgressBarIndeterminateVisibility(true);
        super.onPageStarted(view, url, favicon);
    }
}