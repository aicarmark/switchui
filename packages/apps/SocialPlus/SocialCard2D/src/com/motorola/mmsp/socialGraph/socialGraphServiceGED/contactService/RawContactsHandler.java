package com.motorola.mmsp.socialGraph.socialGraphServiceGED.contactService;

import android.content.Context;
import android.content.Intent;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

import com.motorola.mmsp.socialGraph.socialGraphServiceGED.ConstantDefinition;

public class RawContactsHandler implements Callback{
	private final String TAG = "SocialGraphService";
	private final static boolean Debug = true;
	
	public static final String CONTACTS_NOTIFICATION_ACTION = "com.motorola.mmsp.intent.action.CONTACTS_ACTION";

	private Context myContext = null;
    
    public RawContactsHandler(Context context){
    	myContext = context;
    }
    
	//@Override
	public boolean handleMessage(Message msg) {
		NotifyChange();
		return false;

	}
	
	void NotifyChange(){
		if (Debug)
			Log.e(TAG, "raw contact handler notify change");
		Intent myIntent = new Intent(CONTACTS_NOTIFICATION_ACTION);
		myIntent.putExtra("action", ConstantDefinition.MESSAGE_ACTION_CONTACT_DATA_UPDATE);
		myContext.sendBroadcast(myIntent);		
	}
}
