package com.motorola.contextual.service.dataclearlistener;


import com.motorola.contextual.fw.pluginserver.LoaderClientInterface;
import com.motorola.contextual.fw.pluginserver.LoaderServerCallback;
import com.motorola.contextual.fw.pluginserver.LoaderServerCallback.Event;
import com.motorola.contextual.smartrules.Constants;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


/**
 * 
 * Add the LoaderInterface.jar to your build path.
 * 
 * To generate the jar file 
 * 	1. In the bin directory - create a root directory.
 *  2. Copy from your bin/classes the com directory to the root directory created in step 1
 *  3. Create a Manifest.xml in the following format
 *  	<CLASSLIST>
 *  		<CLASSNAME>com.motorola.helloworld.HelloWorld</CLASSNAME>
 *  	</CLASSLIST>
 *  4. Create the jar file using the command jar cf HelloWorld.jar .
 *  5. Create the dex.jar file using the command 
 *  	dx --dex --output=HelloWordDex.jar HelloWorld.jar
 */
public class DataClearListenerService extends Service implements Constants, LoaderClientInterface {
	
	private static final String TAG = DataClearListenerService.class.getSimpleName();
	private static final String SA_DATA_CLEAR_INTENT = "com.motorola.contextual.smartrules.DATA_CLEAR";
	private static final String SA_PACKAGE_URI = "package:com.motorola.contextual.smartrules";
	private static Context mContext = null;
	    
	private BroadcastReceiver mIntentReceiver = null;

	public void onLoad(Context context, LoaderServerCallback loaderCallback) {
	
		if (LOG_INFO) Log.i(TAG, "Starting DataClearListenerService Intent");
		mContext = context.getApplicationContext();
		
		if (mContext == null) {
			Log.e(TAG, "Context is null; returning");
			return;
		}
		mIntentReceiver = registerDataClearReceiver(mContext);
		
		//Send an intent to the owner of this jar file that this class is up and running
		Intent onLoadIntent = new Intent(ONLOAD_INTENT);
		onLoadIntent.putExtra(EXTRA_CLASSNAME, DataClearListenerService.class.getName());
		context.sendBroadcast(onLoadIntent);
		if (LOG_INFO) Log.i(TAG, "sending Intent " + onLoadIntent.toUri(0));
	}

	public void onEvent(Event event) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {

		return START_STICKY;
	}
    

	/** onCreate
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		if (LOG_INFO) Log.i(TAG, "DataClearListenerService Oncreate called; Registering receiver");
		
		mContext = this;
		mIntentReceiver = registerDataClearReceiver(mContext);
	}

	public void onUnload(Context context) {
		if (LOG_INFO) Log.i(TAG, "onUnload called");
		if(mIntentReceiver != null) {
			context.getApplicationContext().unregisterReceiver(mIntentReceiver);
			mIntentReceiver = null;
		}
		
	}
	
	/** onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (LOG_INFO) Log.i(TAG, "onDestroy called");
		super.onDestroy();
		if(mIntentReceiver != null) {
			mContext.unregisterReceiver(mIntentReceiver);
			mIntentReceiver = null;
		}
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
    
	/** Registers a receiver for receiving the Android intents
	 *  
	 * @param context - context
	 */    
	private BroadcastReceiver registerDataClearReceiver(Context context){
		
		// register the receiver
		BroadcastReceiver receiver = new DataClearReceiver();

		IntentFilter filter = new IntentFilter();	
		filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		filter.addDataScheme("package");
		
		if (LOG_INFO) Log.i(TAG, "registerDataClearReceiver : Registering ACTION_PACKAGE_DATA_CLEARED");
		
		context.registerReceiver(receiver, filter);
		
		if(LOG_DEBUG) Log.d(TAG, "DataClear Service Intent; Registered receiver");
		return receiver;
	}
	
	private class DataClearReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent == null || intent.getAction() == null) {
	        	Log.e(TAG, "Invalid Intent; Returning");
	        	return;
	        }
	        
	        if(LOG_INFO) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));
	        String action = intent.getAction();
	        
	        if (action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED)) {
	        	processDataClearIntent(context, intent);    	
	        } 
		}

		

		private void processDataClearIntent(Context context, Intent intent) {
			String dataclearPackage = intent.getDataString();
        	
			if(LOG_DEBUG) Log.d(TAG, "processDataClearIntent : dataclearPackage is "+ dataclearPackage);
			if (SA_PACKAGE_URI.equals(dataclearPackage)) {
				if(LOG_INFO) Log.i(TAG, "processDataClearIntent : Smartrules data clear; sending intent");
				Intent dataclearIntent = new Intent(SA_DATA_CLEAR_INTENT);
				context.sendBroadcast(dataclearIntent);
			}
		}

	
	}

}