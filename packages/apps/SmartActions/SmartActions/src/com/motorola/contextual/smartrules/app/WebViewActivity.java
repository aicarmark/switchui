/*
 * @(#)WebViewActivity.java
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
import com.motorola.contextual.smartrules.R;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * This class creates a web view to display the help and about content.
 *
 * <code><pre>
 * CLASS:
 *  extends Activity
 *
 * implements
 *  Constants - for the constants used
 *
 * RESPONSIBILITIES:
 *  create a web view to show the help and about content.
 *
 * COLLABORATORS:
 *  None
 *
 * </pre></code>
 */
public class WebViewActivity extends Activity implements Constants {
	
	private static final String TAG = WebViewActivity.class.getSimpleName();
	
	public interface REQUIRED_PARMS {
	    String CATEGORY = PACKAGE + ".category";
	    String LAUNCH_URI = PACKAGE + ".launchuri";
	}
	
	public interface WebViewCategory {
		final int HELP = 0;
	}

	/**
     * The main webView object
     */
    private WebView mWebView;
    private int category = 0;
    private String launchUri = null;
    
    @Override
    public void onCreate(Bundle bundle) {
    	
    	super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    	setContentView(R.layout.webview_layout);
        setProgressBarIndeterminateVisibility(true);
        
        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
 		ab.show();
 		
    	if(getIntent().getExtras() == null) {
    		finish();
    	}
    	else {
    		category = getIntent().getIntExtra(REQUIRED_PARMS.CATEGORY, WebViewCategory.HELP);
			launchUri = getIntent().getStringExtra(REQUIRED_PARMS.LAUNCH_URI);
			
			if(launchUri == null) {
				finish();
			}
			else {
	    		if(category == WebViewCategory.HELP) 
	    			setTitle(R.string.help);
	    		else
	    			finish();
	    			
	    		mWebView = (WebView) findViewById(R.id.webview_content);
	    	
	    	 	WebSettings webSettings = mWebView.getSettings();
	    	 	if(webSettings == null)
	    	 		Log.e(TAG, "mWebView.getSettings() returned null");
	    	 	else
	    	 		webSettings.setJavaScriptEnabled(true);
	
	    	 	// start displaying the webpage
	    	 	mWebView.loadUrl(launchUri);
	    	 	mWebView.setWebViewClient(new MyWebViewClient(this));
			}
    	}
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		    case android.R.id.home:
                        finish();
			return true;
		    default:
			return super.onOptionsItemSelected(item);
		}
	} 

      // BEGIN Motorola, hcv368, 21/11/2012, IKMAINJB-5514
      /** onBackPressed()
       *  handles the back button press on the phone.
       */
      @Override
      public void onBackPressed() {
          handleBackButtonPress();
      }

      private void handleBackButtonPress() {
          if (mWebView.canGoBack()) {
              mWebView.goBack();
              return;
          }
          finish();
      }
      //END IKMAINJB-5514.
}
