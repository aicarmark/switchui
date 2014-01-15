package com.motorola.con2vcard;

//import com.android.vcard.VCardComposer.OneEntryHandler;

import android.content.Context;

public class ComposerHandler /*implements OneEntryHandler*/{
	
	private String mVC = null;

	public boolean onEntryCreated(String vcard) {
		mVC = vcard;
		return true;
	}

	public boolean onInit(Context context) {
		mVC = null;
		return true;
	}

	public void onTerminate() {
		
	}
	
	public String getVC() {
		return mVC;
	}
}

