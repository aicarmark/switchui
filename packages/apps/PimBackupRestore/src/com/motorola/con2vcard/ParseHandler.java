package com.motorola.con2vcard;
import com.android.vcard2.VCardEntry;
import com.android.vcard2.VCardEntryHandler;

public class ParseHandler implements VCardEntryHandler {
	private VCardEntry mContactStruct;
    	
	public ParseHandler() {
		mContactStruct = null;
	}
    
   	public void onEntryCreated(final VCardEntry contactStruct) {
   		mContactStruct = contactStruct;
   	}
    
   	public VCardEntry getContactStruct(){
   		return mContactStruct;
   	}	

	public void onEnd() {
		// TODO Auto-generated method stub		
	}

	public void onStart() {
		// TODO Auto-generated method stub		
	}
}

