package com.motorola.contextual.smartrules.db.table.view;

import android.net.Uri;

import com.motorola.contextual.smartrules.Constants;

public class RuleCloneView implements Constants {
	
	/** name of the view in the database */
	public static final String VIEW_NAME = RuleCloneView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");  
	
}
