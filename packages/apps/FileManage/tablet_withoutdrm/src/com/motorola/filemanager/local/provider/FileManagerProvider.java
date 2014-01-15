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

package com.motorola.filemanager.local.provider;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.MediaColumns;

import com.motorola.filemanager.utils.MimeTypeUtil;

public class FileManagerProvider extends ContentProvider {
    private static final String MIME_TYPE_PREFIX = "content://com.motorola.filemanager/mimetype/";
    public static final String AUTHORITY = "com.motorola.filemanager";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        // not supported
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return MimeTypeUtil.getMimeType(getContext(), uri.toString());
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        // not supported
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        if (uri.toString().startsWith(MIME_TYPE_PREFIX)) {
            MatrixCursor c =
                    new MatrixCursor(new String[]{MediaColumns.DATA, MediaColumns.MIME_TYPE});
            // data = absolute path = uri - content://authority/mimetype
            String data = uri.toString().substring(20 + AUTHORITY.length());
            String mimeType = MimeTypeUtil.getMimeType(getContext(), data);
            c.addRow(new String[]{data, mimeType});
            return c;
        } else {
            throw new RuntimeException("Unsupported uri");
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uri.toString().startsWith(MIME_TYPE_PREFIX)) {
            int m = ParcelFileDescriptor.MODE_READ_ONLY;
            if (mode.equalsIgnoreCase("rw")) {
                m = ParcelFileDescriptor.MODE_READ_WRITE;
            }

            File f = new File(uri.toString().substring(20 + AUTHORITY.length()));
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, m);
            return pfd;
        } else {
            throw new RuntimeException("Unsupported uri");
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
        // not supported
        return 0;
    }
}
