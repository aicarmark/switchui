package com.motorola.mmsp.socialGraph.socialGraphServiceGED;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import com.motorola.mmsp.socialGraph.socialGraphServiceGED.contactService.RawContactsHandler;

public class SocialDataService extends Service{
	private final String TAG = "SocialGraphService";
	private final static boolean Debug = true;
	private final String VERSION = "2011-06-11";
	
	private Handler mContactHandler = null;
	
	private boolean mCreated = false;

	@Override
	public void onCreate() {
		
		super.onCreate();
		
		if (Debug)
			Log.e(TAG, "SocialGraphService version:" + VERSION);
		
		createContactService();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (Debug)
			Log.e(TAG, "call service on start");
		
		if (!mCreated) {
			createContactService();
		}
         if (intent != null) {
            if("com.motorola.mmsp.intent.action.CONTACTS_CLEAR_ACTION".equals(intent.getAction())){
        	
    		if (mContactHandler != null) {
    			Log.e(TAG, "restart sync with contact");
    			Message myMsg = new Message();
    			myMsg.arg1 = 1;
    			mContactHandler.removeMessages(0);
    			mContactHandler.sendMessageDelayed(myMsg, 2000);
    		}
            }
         }
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
			
		if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
			Log.e(TAG, "begin to destroy");		
        }		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void createContactService() {
		mContactHandler = registerHandler("rawContactHandler",
				new RawContactsHandler(this));

		// for contact updated
		ContactListener myContactListener = new ContactListener(new Handler());
		getContentResolver().registerContentObserver(
				ContactsContract.Contacts.CONTENT_URI, true, myContactListener);
		
		mCreated = true;
		
		if (mContactHandler != null) {
			Log.e(TAG, "restart sync with contact");
			Message myMsg = new Message();
			myMsg.arg1 = 1;
			mContactHandler.removeMessages(0);
			mContactHandler.sendMessageDelayed(myMsg, 2000);
		}

	}
	
	public Handler registerHandler(String myName, Callback myCallback){
		HandlerThread myHandlerThread = new HandlerThread(myName);
		myHandlerThread.start();
		
		return new Handler(myHandlerThread.getLooper(), myCallback);
	}
	
	class ContactListener extends ContentObserver{
	 	
		public ContactListener(Handler handler){
		super(handler);
		}

		public void onChange(boolean selfChange){
			super.onChange(selfChange);
			if(selfChange){			
				Log.e(TAG, "no need to listener contacts");
				return;
			}		
			
			Log.e(TAG, "ContactListener have received the notification from the  contact database");
			
			Message myMsg = new Message();
			myMsg.arg1=1;
			mContactHandler.removeMessages(0);
			mContactHandler.sendMessageDelayed(myMsg, 2000);	
			
		}
    }	

}
