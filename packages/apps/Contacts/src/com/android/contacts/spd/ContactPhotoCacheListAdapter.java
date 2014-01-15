/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.android.contacts.spd;

import com.android.contacts.R;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author fit009c
 */
public abstract class ContactPhotoCacheListAdapter implements ListAdapter {

    private static final int QUERY_CONTACT = 1001;
    private static final int QUERY_PHOTO = 1002;

    private static final String[] PHOTO_PROJECTION = new String[] {
        Photo._ID,
        Photo.PHOTO
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case QUERY_CONTACT:
                    if (null != cursor) {
                        ContactCookie contactCookie = (ContactCookie)cookie;
                        ContactCacheData data;

                        if (mContactCache.containsKey(contactCookie.key)) {
                            data = mContactCache.get(contactCookie.key);
                            mContactObserver.unregisterCursor(data.cursor);
                            data.cursor.close();
                        } else {
                            data = new ContactCacheData();
                            mContactCache.put(contactCookie.key, data);
                        }

                        data.cursor = cursor;
                        addViewToContactCache(contactCookie.key, contactCookie.view);
                        for (View v : data.views) {
                            updateViewForContactInfo(contactCookie.key, v, cursor);
                        }
                        mContactObserver.registerCursor(cursor);
                    }
                    break;

                case QUERY_PHOTO:
                    if (null != cursor) {
                        PhotoCookie photoCookie = (PhotoCookie)cookie;
                        PhotoCacheData data;

                        if (mPhotoCache.containsKey(photoCookie.id)) {
                            data = mPhotoCache.get(photoCookie.id);
                            mPhotoObserver.unregisterCursor(data.cursor);
                            if (null != data.bitmap)
                                data.bitmap.recycle();
                            data.cursor.close();
                        } else {
                            data = new PhotoCacheData();
                            mPhotoCache.put(photoCookie.id, data);
                        }

                        data.cursor = cursor;
                        data.bitmap = null;
                        if (cursor.moveToFirst()) {
                            byte[] photoData = cursor.getBlob(cursor.getColumnIndex(Photo.PHOTO));
                            if (null != photoData)
                                data.bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
                        }
                        addViewToPhotoCache(photoCookie.id, photoCookie.view);
                        for (View v : data.views)
                            updateViewForPhoto(v, data.bitmap);
                        mPhotoObserver.registerCursor(cursor);
                    }
                    break;
            }
        }
    }

    private class ContactObserver extends MultipleContentObserver {

        @Override
        protected void onContentChange(Cursor cursor) {
            if (null == cursor)
                return;

            for (String key : mContactCache.keySet()) {
                ContactCacheData data = mContactCache.get(key);
                if (cursor.equals(data.cursor)) {
                    if (!cursor.isClosed() && cursor.requery()) {
                        for (View view : data.views) {
                            updateViewForContactInfo(key, view, cursor);
                        }
                    } else {
                        mContactCache.remove(key);
                    }
                    break;
                }
            }
        }
    }

    private class ContactCacheData {
        Cursor cursor;
        ArrayList<View> views = new ArrayList<View>();
    }

    private class ContactCookie {
        String key;
        View view;
    }

    private class PhotoObserver extends MultipleContentObserver {

        @Override
        protected void onContentChange(Cursor cursor) {
            if (null == cursor)
                return;

            for (long id : mPhotoCache.keySet()) {
                PhotoCacheData data = mPhotoCache.get(id);
                if (cursor.equals(data.cursor)) {
                    if (!cursor.isClosed() && cursor.requery()) {
                        data.bitmap = null;

                        if (cursor.moveToFirst()) {
                            byte[] photoData = cursor.getBlob(cursor.getColumnIndex(Photo.PHOTO));
                            if (null != photoData)
                                data.bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
                        }

                        for (View v : data.views) {
                            updateViewForPhoto(v, data.bitmap);
                        }
                    } else {
                        mPhotoCache.remove(id);
                    }
                    break;
                }
            }
        }
    }

    private class PhotoCacheData {
        Cursor cursor;
        Bitmap bitmap;
        ArrayList<View> views = new ArrayList<View>();
    }

    private class PhotoCookie {
        long id;
        View view;
    }

    protected class QueryData {
        Uri uri;
        String[] projection;
        String selection;
        String[] selectionArgs;
        String orderBy;
    }

    public static abstract class MultipleContentObserver {

        private class Observer extends ContentObserver {

            public Observer () {
                super(new Handler());
            }

            @Override
            public void onChange(boolean selfChange) {
                Cursor cursor = null;
                WeakReference<Cursor> ref = mObservables.get(this);
                if (null != ref)
                    cursor = ref.get();
                onContentChange(cursor);
            }
        }

        HashMap<Observer, WeakReference<Cursor>> mObservables = new HashMap<Observer, WeakReference<Cursor>>();

        public void registerCursor(Cursor cursor) {
            if ((null == cursor) || (cursor.isClosed()))
                return;

            for (Observer key : mObservables.keySet()) {
                Cursor value = mObservables.get(key).get();
                if ((null != value) && (value.equals(cursor)))
                    return;
            }

            Observer observer = new Observer();
            cursor.registerContentObserver(observer);
            mObservables.put(observer, new WeakReference(cursor));
        }

        public void unregisterCursor(Cursor cursor) {
            if (null == cursor)
                return;

            for (Observer key : mObservables.keySet()) {
                Cursor value = mObservables.get(key).get();
                if ((null != value) && (value.equals(cursor))) {
                    value.unregisterContentObserver(key);
                    mObservables.remove(key);
                    break;
                }
            }
        }

        protected abstract void onContentChange(Cursor cursor);
    }

    private QueryHandler mQueryHandler = null;
    private ContactObserver mContactObserver = new ContactObserver();
    private HashMap<View, String> mContactViewMap = new HashMap<View, String>();
    private HashMap<String, ContactCacheData> mContactCache = new HashMap<String, ContactCacheData>();
    private PhotoObserver mPhotoObserver = new PhotoObserver();
    private HashMap<View, Long> mPhotoViewMap = new HashMap<View, Long>();
    private HashMap<Long, PhotoCacheData> mPhotoCache = new HashMap<Long, PhotoCacheData>();

    protected Context mContext = null;

    protected abstract View createItemView(int position, View view, ViewGroup viewGroup);
    protected abstract String getItemKey(int position);
    protected abstract QueryData getContactQueryData(int position);
    protected abstract void updateViewForContact(String key, View view, Cursor cursor);
    protected abstract void updateViewForPhoto(View view, Bitmap bitmap);

    public ContactPhotoCacheListAdapter(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        mContext = context;
        mQueryHandler = new QueryHandler(mContext.getContentResolver());
    }

    public final View getView(int i, View view, ViewGroup vg) {

        View itemView = createItemView(i, view, vg);
        String key = getItemKey(i);

        if (itemView.equals(view)) {
            // the view has been reused; remove from contact and photo chache
            removeViewFromContactCache(itemView);
        }

        if (mContactCache.containsKey(key)) {
            ContactCacheData data = mContactCache.get(key);
            updateViewForContactInfo(key, itemView, data.cursor);
            addViewToContactCache(key, itemView);
        } else {
            QueryData queryData = getContactQueryData(i);
            if (null != queryData) {
                updateViewForContactInfo(key, itemView, null);
                ContactCookie cookie = new ContactCookie();
                cookie.key = key;
                cookie.view = itemView;
                mQueryHandler.startQuery(
                        QUERY_CONTACT,
                        cookie,
                        queryData.uri,
                        queryData.projection,
                        queryData.selection,
                        queryData.selectionArgs,
                        queryData.orderBy);
            }
        }

        return itemView;
    }

    public final void clearCache() {
        for (String key : mContactCache.keySet()) {
            ContactCacheData data = mContactCache.get(key);
            mContactObserver.unregisterCursor(data.cursor);
            data.cursor.close();
        }
        mContactCache.clear();
        mContactViewMap.clear();

        for (long key : mPhotoCache.keySet()) {
            PhotoCacheData data = mPhotoCache.get(key);
            mPhotoObserver.unregisterCursor(data.cursor);
            data.cursor.close();
            if (null != data.bitmap)
                data.bitmap.recycle();
        }
        mPhotoCache.clear();
        mPhotoViewMap.clear();
    }

    private void updateViewForContactInfo(String key, View view, Cursor cursor) {
        updateViewForContact(key, view, cursor);
        if ((null != cursor) && cursor.moveToFirst()) {
            String contactName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
            if(contactName.compareTo(mContext.getString(R.string.voicemail)) != 0) {
                removeViewFromPhotoCache(view);
                try {
                    long photoId = cursor.getLong(cursor.getColumnIndexOrThrow(Contacts.PHOTO_ID));
                    if (mPhotoCache.containsKey(photoId)) {
                        PhotoCacheData data = mPhotoCache.get(photoId);
                        updateViewForPhoto(view, data.bitmap);
                        addViewToPhotoCache(photoId, view);
                    } else {
                        updateViewForPhoto(view, null);
                        PhotoCookie cookie = new PhotoCookie();
                        cookie.id = photoId;
                  	    cookie.view = view;
                        mQueryHandler.startQuery(
                            QUERY_PHOTO,
                            cookie,
                            ContentUris.withAppendedId(Data.CONTENT_URI, photoId),
                            PHOTO_PROJECTION,
                            null, null, null);
                    }
                } catch (IllegalArgumentException e) {
                    updateViewForPhoto(view, null);
                }
            }
        } else {
//            updateViewForPhoto(view, null);
        }
    }

    private void addViewToContactCache(String key, View view) {
        if ((null == key) || (null == view))
            return;

        ContactCacheData data = mContactCache.get(key);
        if (null == data)
            return;

        data.views.add(view);
        mContactViewMap.put(view, key);
    }

    private void removeViewFromContactCache(View view) {
        if (null == view)
            return;

        String number = mContactViewMap.get(view);
        if (null == number)
            return;

        mContactViewMap.remove(view);
        ContactCacheData data = mContactCache.get(number);
        if (null == data)
            return;

        for (View v : data.views) {
            if (v.equals(view)) {
                data.views.remove(view);
                break;
            }
        }
    }

    private void addViewToPhotoCache(long id, View view) {
        if (null == view)
            return;

        PhotoCacheData data = mPhotoCache.get(id);
        if (null == data)
            return;

        data.views.add(view);
        mPhotoViewMap.put(view, id);
    }

    private void removeViewFromPhotoCache(View view) {
        if (null == view)
            return;

        Long id = mPhotoViewMap.get(view);
        if (null == id)
            return;

        mPhotoViewMap.remove(view);
        PhotoCacheData data = mPhotoCache.get(id);
        if (null == data)
            return;

        for (View v : data.views) {
            if (v.equals(view)) {
                data.views.remove(view);
                break;
            }
        }
    }
}
