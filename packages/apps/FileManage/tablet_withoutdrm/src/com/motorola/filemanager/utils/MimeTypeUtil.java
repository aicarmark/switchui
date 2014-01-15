/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.webkit.MimeTypeMap;

import com.motorola.filemanager.R;

public class MimeTypeUtil {
    public static final String TAG_MIMETYPES = "MimeTypes";
    public static final String TAG_TYPE = "type";
    public static final String ATTR_EXTENSION = "extension";
    public static final String ATTR_MIMETYPE = "mimetype";

    private static MimeTypeUtil mInstance;
    private Map<String, String> mMimeTypes;
    private XmlPullParser mXpp;
    private Context mContext;

    public static String getMimeType(Context context, String str) {
        if (mInstance == null) {
            synchronized (MimeTypeUtil.class) {
                new MimeTypeUtil(context);
            }
        }
        return mInstance.getMimeType(str);
    }

    private synchronized void setMInstance(MimeTypeUtil instance) {
        mInstance = instance;
    }

    private MimeTypeUtil(Context context) {
        int eventType = 0;
        mContext = context;
        setMInstance(this);
        mMimeTypes = new HashMap<String, String>();
        mXpp = mContext.getResources().getXml(R.xml.mimetypes);
        if (mXpp == null) {
            return;
        }
        try {
            eventType = mXpp.getEventType();
        } catch (XmlPullParserException e) {
            ;
        }
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag = mXpp.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (tag.equals(TAG_MIMETYPES)) {
                } else if (tag.equals(TAG_TYPE)) {
                    String extension = mXpp.getAttributeValue(null, ATTR_EXTENSION);
                    String mimetype = mXpp.getAttributeValue(null, ATTR_MIMETYPE);
                    // extension.toLowerCase for easier comparing
                    mMimeTypes.put(extension.toLowerCase(), mimetype);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tag.equals(TAG_MIMETYPES)) {
                }
            }
            try {
                eventType = mXpp.next();
            } catch (IOException e) {
                ;
            } catch (XmlPullParserException e) {
                ;
            }
        }
    }

    private String getMimeType(String str) {
        if (str == null) {
            return null;
        }
        int dotPosition = str.lastIndexOf(".");
        String extension = "";
        if (dotPosition != -1) {
            extension = str.substring(dotPosition).toLowerCase();
        }
        // Let's check the official map first. Webkit has a nice
        // extension-to-MIME map.
        // Be sure to remove the first character from the extension, which is
        // the "." character.
        if (extension.length() > 0) {
            String webkitMimeType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));
            if (webkitMimeType != null) {
                // Found one. Let's take it!
                return webkitMimeType;
            }
        }
        String mimetype = mMimeTypes.get(extension);
        return mimetype;
    }
}
