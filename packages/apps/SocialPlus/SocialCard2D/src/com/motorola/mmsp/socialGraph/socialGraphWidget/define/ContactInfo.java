package com.motorola.mmsp.socialGraph.socialGraphWidget.define;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.net.Uri;

public class ContactInfo {
	public long    id;
	public String name;
	public Bitmap photo;
	public Uri    uri;
	public ArrayList<PhoneNumber> numbers;
	public ArrayList<EmailAddress> emails;
}

