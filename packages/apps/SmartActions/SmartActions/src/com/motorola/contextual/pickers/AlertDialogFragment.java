/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CRGW47        2012/07/05 Smart Actions 2.1 Initial Version
 */

package com.motorola.contextual.pickers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.motorola.contextual.smartrules.R;

/**
 * This class creates a dialog fragment to display some alert messages.
 * <code><pre>
 *
 * CLASS:
 *  extends DialogFragment
 *
 * RESPONSIBILITIES:
 *  Creates a DialogFragment to display alert messages.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
final public class AlertDialogFragment extends DialogFragment {
	
	// cjd - a JavaDoc comment here would be nice
    public static final int DIALOG_ID_ALERT = 1;

    
    
    /**
     * The string for adding and obtaining the resource id of the message
     * to display from set of arguments
     */
    private static final String MESSAGE_ID = "MESSAGE_ID";

    /**
     * The string for adding and obtaining the resource id of the title
     * to display from set of arguments
     */
    private static final String TITLE_ID = "TITLE_ID";

    /**
     * The string for adding and obtaining the title string
     * to display from set of arguments
     */
    private static final String TITLE = "TITLE";

    /**
     * The string for adding and obtaining the resource id of the message
     * to display from set of arguments
     */
    private static final String URL = "URL";

    /**
     * Reference to the context of the activity hosting the dialog fragment
     */
    private Context mContext;

    private WebView mWebView;
    private int mWebViewHeight;

    /**
     * public empty constructor
     */
    public AlertDialogFragment() {
    	super();
    }

    /**
     * This method returns a newly created and initialized instance of
     * dialog fragment
     *
     * @param titleId - the resource id of the dialog title
     * @param messageId - the resource id of the message to display
     * @return - a newly created and initialized instance of dialog fragment
     */
    public static AlertDialogFragment newInstance(int titleId, int messageId) {
        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(TITLE_ID, titleId);
        arguments.putInt(MESSAGE_ID, messageId);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    /**
     * This method returns a newly created and initialized instance of
     * dialog fragment
     *
     * @param title - title of the dialog
     * @param messageId - the resource id of the message to display
     * @return - a newly created and initialized instance of dialog fragment
     */
    public static AlertDialogFragment newInstance(String title, int messageId) {
        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(TITLE, title);
        arguments.putInt(MESSAGE_ID, messageId);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    /**
     * This method returns a newly created and initialized instance of
     * dialog fragment
     *
     * @param titleId - the resource id of the dialog title
     * @param url - the url pointing to the HTML file to be displayed
     * @return - a newly created and initialized instance of dialog fragment
     */
    public static AlertDialogFragment newInstance(int titleId, String url) {
        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(TITLE_ID, titleId);
        arguments.putString(URL, url);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    /**
     * This method returns a newly created and initialized instance of
     * dialog fragment
     *
     * @param title - title of the dialog
     * @param url - the url pointing to the HTML file to be displayed
     * @return - a newly created and initialized instance of dialog fragment
     */
    public static AlertDialogFragment newInstance(String title, String url) {
        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        Bundle arguments = new Bundle();
        //        arguments.putInt(ALERT_DIALOG_ID, dialogId);
        arguments.putString(TITLE, title);
        arguments.putString(URL, url);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog dialog = null;
        // cjd - if these parameters are required, then perhaps checking and "throw new IllegalArgumentException()" might be appropriate 
        int titleId = getArguments().getInt(TITLE_ID);
        String title = getArguments().getString(TITLE);
        int messageId = getArguments().getInt(MESSAGE_ID);
        String url = getArguments().getString(URL);

        if ((messageId != 0) || (url != null)) {
            mContext = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(title != null ? title : mContext.getString(titleId))
            .setIcon(R.drawable.ic_info_details)
            .setView(mWebView)
            .setPositiveButton(mContext.getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });

            if (url != null) {
                mWebView = new WebView(mContext);

                // disable hardware acceleration on webview to avoid white background flash
                mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.loadUrl(url);
                //This is to get the height when the webview shows the initial text
                //This height is used to set the size back to initial state when 'less' link is hit
                mWebView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    public void onLayoutChange(View v, int left, int top, int right,
                            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if(mWebViewHeight == 0) {
                            mWebViewHeight = mWebView.getHeight();
                        }
                    }
                });
                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //Work around to fix the web view height on selecting 'more' & 'less' links
                        final LayoutParams params = mWebView.getLayoutParams();
                        if(mWebViewHeight > 0 && mWebView.getHeight() > mWebViewHeight) {
                            params.height = mWebViewHeight;
                        }else {
                            params.height = LayoutParams.WRAP_CONTENT;
                        }
                        mWebView.setLayoutParams(params);
                    }
                });
                builder.setView(mWebView);
            }
            else if (messageId != 0) {
                builder.setMessage(mContext.getString(messageId));
            }
            // cjd - what if url IS null and messageId = 0? at least a Log.e(); or a comment would be nice.

            dialog = builder.create();
            dialog.show();
        }

        return dialog;
    }
}
