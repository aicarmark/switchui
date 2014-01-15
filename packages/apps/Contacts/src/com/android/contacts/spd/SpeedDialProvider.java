/*
 * Copyright (C) 2011, Motorola, Inc,
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.spd;

// BEGIN Motorola, Aug-03-2011, IKPIM-282
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;


import java.util.Arrays;
import java.util.List;

/*
 *  This provider implements a limited interface to access speed dial list.
 *  1. Query projection is not really accepted. A fixed projection set is used.
 *  2. Where is not really accepted. It will be ignored if assigned.
 *  3. Insert will not work if ID is not assigned.
 */

public class SpeedDialProvider extends ContentProvider {
    private static final String TAG = "SpeedDialProvider";
    private static final String AUTHORITY = "com.android.contacts.spd";
    private static final Uri CONTENT_URI=Uri.parse("content://" + AUTHORITY);
    private final static Uri SPD_URI = Uri.withAppendedPath(CONTENT_URI, "spd");
    private final static Uri SPD_NUMBER_URI = Uri.withAppendedPath(SPD_URI, "number");
    private final static Uri SPD_FIRST_NEXT_AVAILABLE_URI = Uri.withAppendedPath(SPD_URI, "nextavailable");

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String ID = "ID";
    private static final String NUMBER = "NUMBER";
    private static final String LOCK = "LOCK";

    private static final String[] SPD_PROJECTS={ID, NUMBER, LOCK};
    private static final int COLUMN_SIZE = 3;

    private static final int SPD_BY_ID = 1;
    private static final int SPD_BY_NUMBER = 2;
    private static final int SPD_FIRST_NEXT_AVAILABLE_POS = 3;
    static {
        sURIMatcher.addURI(AUTHORITY, "spd/#", SPD_BY_ID);
        sURIMatcher.addURI(AUTHORITY, "spd/number/*", SPD_BY_NUMBER);
        sURIMatcher.addURI(AUTHORITY, "spd/nextavailable", SPD_FIRST_NEXT_AVAILABLE_POS);
    }
    private static final String CONTENT_TYPE = "vnd.android.cursor.item/motorola.speeddial";

    @Override
    public boolean onCreate() {
        SpeedDialStorage.initSetupComplete(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int position = -1;
        String number = null;

        int match = sURIMatcher.match(uri);
        switch (match) {
            case SPD_BY_ID: {
                position = Integer.parseInt(uri.getLastPathSegment());
                number = SpeedDialStorage.getSpeedDialNumberByPos(position);
                break;
            }

            case SPD_BY_NUMBER: {
                number = PhoneNumberUtils.stripSeparators(uri.getLastPathSegment());
                if (TextUtils.isEmpty(number)) {
                    throw new IllegalArgumentException("URI: " + uri + " Empty parameter");
                } else {
                    position = SpeedDialStorage.getSpeedDialPosByNumber(getContext(), number);
                }
                break;
            }

            case SPD_FIRST_NEXT_AVAILABLE_POS: {
                number = "";
                position = SpeedDialStorage.firstEmptySpeedDialPos();
                if (-1 == position) {
                    position = SpeedDialStorage.firstEditablePos();
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        MatrixCursor cursor;
        if (number == null || position == -1) {
            cursor = new MatrixCursor(SPD_PROJECTS, 0);
        } else {
            final int locked = 0;
            cursor = new MatrixCursor(SPD_PROJECTS, 1);
            cursor.newRow()
                  .add(Integer.toString(position))
                  .add(number)
                  .add(Integer.toString(locked));
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case SPD_BY_ID:
            case SPD_BY_NUMBER:
            case SPD_FIRST_NEXT_AVAILABLE_POS:
                return CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (SPD_BY_ID != sURIMatcher.match(uri)) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int position = Integer.parseInt(uri.getLastPathSegment());
        String number = PhoneNumberUtils.stripSeparators(values.getAsString(NUMBER));
        if (true == TextUtils.isEmpty(number)) {
            throw new IllegalArgumentException("URI: " + uri + " Empty values: " + values.toString());
        }
        if (false == TextUtils.isEmpty(SpeedDialStorage.getSpeedDialNumberByPos(position))) {
            return null;
        }
        if (SpeedDialStorage.setSpeedDial(getContext(), position, number)) {
            notifyChange();
            return ContentUris.withAppendedId(SPD_URI, position);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (SPD_BY_ID != sURIMatcher.match(uri)) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int position = Integer.parseInt(uri.getLastPathSegment());
        String number = PhoneNumberUtils.stripSeparators(values.getAsString(NUMBER));
        if (true == TextUtils.isEmpty(number)) {
            throw new IllegalArgumentException("URI: " + uri + " Empty values: " + values.toString());
        }
        if (true == TextUtils.isEmpty(SpeedDialStorage.getSpeedDialNumberByPos(position))) {
            return 0;
        }
        int count = 0;
        if (SpeedDialStorage.setSpeedDial(getContext(), position, number)) {
            notifyChange();
            count = 1;
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (SPD_BY_ID != sURIMatcher.match(uri)) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int position = Integer.parseInt(uri.getLastPathSegment());
        int count = 0;
        if (SpeedDialStorage.deleteSpeedDialByPos(getContext(), position)) {
            notifyChange();
            count = 1;
        }
        return count;
    }

    protected void notifyChange() {
        getContext().getContentResolver().notifyChange(CONTENT_URI, null, false);
    }

}
// END IKPIM-282

