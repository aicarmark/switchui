/**
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * This file was Created by contacts tech team for vcard preview feature on tablet
 */

package com.motorola.contacts.vcard;

import com.android.vcard.VCardConfig;
import com.android.contacts.R;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;


public class VCardPreviewUtils {
    private final static String LOG_TAG = "VCardPreviewUtils";
    /* package */ final static boolean DEBUG = true;

    /* package */ final static int VCARD_VERSION_V21 = 1;
    /* package */ final static int VCARD_VERSION_V30 = 2;

    /* package */ final static String DEFAULT_INTERMEDIATE_CHARSET  = VCardConfig.DEFAULT_INTERMEDIATE_CHARSET;//IKHSS7-4842

    public static class EntryImplementer implements VCardEntryHandler {

        private final ContentResolver mContentResolver;
        private VCardEntry mContactStruct;

        public EntryImplementer(ContentResolver resolver) {
            mContentResolver = resolver;
        }

        public void onStart() {
        }

        public void onEnd() {
        }

        public void onEntryCreated(final VCardEntry contactStruct) {
            mContactStruct = contactStruct;
        }
    }

    public static PreviewRequest constructPreviewRequest(Context context, Uri uri) {
        final ContentResolver resolver = context.getContentResolver();
        VCardEntryCounter counter = null;
        VCardSourceDetector detector = null;
        int vcardVersion = VCARD_VERSION_V21;
        VCardParser vcardParser;
        try {
            boolean shouldUseV30 = false;
            InputStream is = resolver.openInputStream(uri);
            vcardParser = new VCardParser_V21();
            try {
                counter = new VCardEntryCounter();
                detector = new VCardSourceDetector();
                vcardParser.addInterpreter(counter);
                vcardParser.addInterpreter(detector);
                vcardParser.parse(is);
            } catch (VCardVersionException e1) {
                try {
                    is.close();
                } catch (IOException e) {
                }

                shouldUseV30 = true;
                is = resolver.openInputStream(uri);
                vcardParser = new VCardParser_V30();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    vcardParser.addInterpreter(counter);
                    vcardParser.addInterpreter(detector);
                    vcardParser.parse(is);
                } catch (VCardVersionException e2) {
                    throw new VCardException("vCard with unspported version.");
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }

            vcardVersion = shouldUseV30 ? VCARD_VERSION_V30 : VCARD_VERSION_V21;
        } catch (VCardNestedException e) {
            Log.w(LOG_TAG, "Nested Exception is found (it may be false-positive).");
            // Go through without throwing the Exception, as we may be able to detect the
            // version before it
            return null;
        }catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "FileNotFoundException is found .");
            return null;
        }catch (VCardException e) {
            Log.w(LOG_TAG, "VCardException is found .");
            return null;
        }catch (IOException e) {
            Log.w(LOG_TAG, "IOException is found .");
            return null;
        }

        return new PreviewRequest(null,
                uri,
                detector.getEstimatedType(),
                detector.getEstimatedCharset(),
                vcardVersion, counter.getCount());
    }

    public static VCardEntry parseVCard(Context context, Uri uri) {
        InputStream input = null;

        try {
            if (DEBUG) {
                Log.v(LOG_TAG, "parseVCard, uri:" + uri);
            }

            PreviewRequest previewRequest = constructPreviewRequest(context, uri);
            if(previewRequest == null){
                return null;
            }
            VCardParser vcardParser;
            VCardEntryConstructor constructor;

            vcardParser = (previewRequest.vcardVersion == VCARD_VERSION_V30 ?
                            new VCardParser_V30(previewRequest.estimatedVCardType) :
                            new VCardParser_V21(previewRequest.estimatedVCardType));

            constructor = new VCardEntryConstructor(previewRequest.estimatedVCardType, previewRequest.account, previewRequest.estimatedCharset);

            EntryImplementer eImplementer = new EntryImplementer(context.getContentResolver());
            constructor.addEntryHandler(eImplementer);

            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
                input = context.getContentResolver().openInputStream(uri);
            else
                input = new FileInputStream(uri.getPath());

            vcardParser.parse(input, constructor);

            Log.d(LOG_TAG, "value of Contact Struct" + eImplementer.mContactStruct);
            return eImplementer.mContactStruct;
        } catch (IOException e) {
            Log.w(LOG_TAG, "exception in parseVCard - IO: " + e);
            return null;
        } catch (VCardException e) {
            Log.w(LOG_TAG, "exception in parseVCard - VCard: " + e);
            return null;
        } catch (Exception e) {
            Log.w(LOG_TAG, "exception in parseVCard - generic: " + e);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static VCardEntry parseVCardStr(Context context, PreviewRequest previewRequest, String vcardStr) {
        InputStream input = null;

        try {
            if (DEBUG) {
                Log.v(LOG_TAG, "parseVCardStr, vcard:" + vcardStr);
            }

            if(previewRequest == null){
                return null;
            }
            VCardParser vcardParser;
            VCardEntryConstructor constructor;

            vcardParser = (previewRequest.vcardVersion == VCARD_VERSION_V30 ?
                            new VCardParser_V30(previewRequest.estimatedVCardType) :
                            new VCardParser_V21(previewRequest.estimatedVCardType));

            constructor = new VCardEntryConstructor(previewRequest.estimatedVCardType, previewRequest.account, previewRequest.estimatedCharset);

            EntryImplementer eImplementer = new EntryImplementer(context.getContentResolver());
            constructor.addEntryHandler(eImplementer);

            input = new ByteArrayInputStream( vcardStr.getBytes(DEFAULT_INTERMEDIATE_CHARSET));//IKHSS7-4842

            vcardParser.parse(input, constructor);

            Log.d(LOG_TAG, "value of Contact Struct" + eImplementer.mContactStruct);
            return eImplementer.mContactStruct;
        } catch (IOException e) {
            Log.w(LOG_TAG, "exception in parseVCard - IO: " + e);
            return null;
        } catch (VCardException e) {
            Log.w(LOG_TAG, "exception in parseVCard - VCard: " + e);
            return null;
        } catch (Exception e) {
            Log.w(LOG_TAG, "exception in parseVCard - generic: " + e);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static String getVCardStrAt(Context context, Uri uri, int pos) {
        InputStream input = null;

        try {
            if (DEBUG) {
                Log.v(LOG_TAG, "getVCardStrAt, uri:" + uri);
            }

            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
                input = context.getContentResolver().openInputStream(uri);
            else
                input = new FileInputStream(uri.getPath());

            BufferedReader reader = new BufferedReader(new InputStreamReader(input, DEFAULT_INTERMEDIATE_CHARSET));//IKHSS7-4842
            StringBuilder sb = new StringBuilder();
            String line;
            int times = 0;
            while ((line = reader.readLine()) != null) {
                if(line.equals("BEGIN:VCARD")){
                    if(times == pos){
                        sb.append(line).append("\n");
                        break;
                    }else{
                        times ++;
                    }
                }
            }
            if(times == pos){
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    if(line.equals("END:VCARD")){
                        break;
                    }
                }
            }
            return sb.toString();
        } catch (IOException e) {
            Log.w(LOG_TAG, "exception in parseVCard - IO: " + e);
            return null;
        } catch (Exception e) {
            Log.w(LOG_TAG, "exception in parseVCard - generic: " + e);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
