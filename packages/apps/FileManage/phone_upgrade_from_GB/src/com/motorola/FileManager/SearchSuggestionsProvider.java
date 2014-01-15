/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.FileManager;

import java.io.FileNotFoundException;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;

//public class SearchSuggestionsProvider extends ContentProvider {
public class SearchSuggestionsProvider extends SearchRecentSuggestionsProvider{
  private static final String TAG = "SearchSuggestionsProvider";
  public static String AUTHORITY = "com.motorola.FileManager.SearchSuggestionsProvider";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/searchsuggestions");
  // UriMatcher stuff
  private static final int GET_RECORD = 0;
  private static final int SEARCH_SUGGEST = 1;
  private static final UriMatcher m_sURIMatcher = buildUriMatcher();
  private SearchContentDatabase mDictionary;
  public static final String RECORD_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
      "/com.motorola.FileManager.searchsuggestionsprovider";

  /**
   * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
   */
  private static UriMatcher buildUriMatcher() {
    UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
    // to get record...
    matcher.addURI(AUTHORITY, "searchsuggestions/#", GET_RECORD);
    // to get suggestions...
    matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
    matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

    return matcher;
  }

  @Override
  public boolean onCreate() {
	  Log.d("","w23001- onCreate ");
    mDictionary = new SearchContentDatabase(getContext());
    return true;
  }

  @Override
  public int delete(Uri uri, String s, String[] as) {
	//add by amt_xulei for SWITCHUITWOV-69 2012-9-12
	//reason:when open db,it will update all files path 
    mDictionary.closeDB();
	//end add
    throw new UnsupportedOperationException();
  }

  @Override
  public String getType(Uri uri) {
    // return file extension (uri.lastIndexOf("."))
    if (m_sURIMatcher.match(uri) == SEARCH_SUGGEST) {
      return SearchManager.SUGGEST_MIME_TYPE;
    } else if (m_sURIMatcher.match(uri) == GET_RECORD) {
      return RECORD_MIME_TYPE;
    } else {
      throw new IllegalArgumentException("Unknown URL " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentvalues) {
    // not supported
    throw new UnsupportedOperationException();
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    if (m_sURIMatcher.match(uri) == SEARCH_SUGGEST) {
      if (selectionArgs == null) {
        throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
      }
      Log.d("","w23001- query: " + "uri=" + uri + "," + "selectionArgs[0]=" +
          selectionArgs[0]);
      return getSuggestions(selectionArgs[0]);
    } else if (m_sURIMatcher.match(uri) == GET_RECORD) {
    	Log.d("","w23001- GET_RECORD: " + "uri=" + uri + ",");
      return getRecord(uri);
    } else {
      throw new RuntimeException("Unsupported uri");
    }
  }

  private Cursor getSuggestions(String query) {
    query = query.toLowerCase();
    String[] columns = new String[]{BaseColumns._ID, SearchContentDatabase.KEY_FOLDER,
    /* SearchContentDatabase.KEY_FULL_PATH, */
    SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

    return mDictionary.getPathMatches(query, columns);
  }

  private Cursor getRecord(Uri uri) {
    String rowId = uri.getLastPathSegment();
    String[] columns =
        new String[]{SearchContentDatabase.KEY_FOLDER, SearchContentDatabase.KEY_FULL_PATH};

    return mDictionary.getPath(rowId, columns);
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
    // not supported
    throw new UnsupportedOperationException();
  }
}
