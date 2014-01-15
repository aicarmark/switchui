package com.motorola.numberlocation;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.util.TimeFormatException;

public class WebUpdateException extends Exception {

	private static final long serialVersionUID = 1L;

	public WebUpdateException(String exp) {
		super(exp);
	}

	public WebUpdateException(String exp, IOException e) {
		super(exp,e);
	}

	public WebUpdateException(String exp, TimeFormatException e) {
		super(exp,e);
	}

	public WebUpdateException(String exp, XmlPullParserException e) {
		super(exp,e);
	}

}
