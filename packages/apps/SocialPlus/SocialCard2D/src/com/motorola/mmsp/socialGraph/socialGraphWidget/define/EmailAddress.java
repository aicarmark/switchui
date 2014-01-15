package com.motorola.mmsp.socialGraph.socialGraphWidget.define;

import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;


public class EmailAddress {
	public static final int ID_COLUMN_INDEX = 0;
	public static final int NAME_COLUMN_INDEX = 1;
	public static final int NUMBER_COLUMN_INDEX = 1;
	public static final int TYPE_COLUMN_INDEX = 2;
	public static final int LABEL_COLUMN_INDEX = 3;
    
	public static final String[] NUMBER_PROJECTION = new String[] {
        Data._ID, //0
        //Contacts.DISPLAY_NAME, //1
        Email.DATA1, //1
        Email.TYPE,   //2
        Email.LABEL,  //3
	};
    
	public long id;
    //public String name;
    public String number;
    public int type;
    public String label;
}