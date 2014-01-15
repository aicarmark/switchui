package com.android.calendar.confinfo;

import com.android.calendar.R;
import android.os.Bundle;
import java.util.ArrayList;
import android.graphics.Typeface;
import android.content.Intent;
import android.content.Context;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.net.Uri;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.ListActivity;
import android.widget.CursorAdapter;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.os.Parcelable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import android.view.Gravity;

public class ConferenceCallOrganizerInfoActivity extends ListActivity {
    private static final String TAG = "ConfCallOrgInfo";

    public static final String EXTRA_ORGANIZER_EMAIL = "extra_organizer_email";
    private static final String LIST_STATE_KEY = "liststate";
    private static final int QUERY_TOKEN = 100;

    private OrganizerItemAdapter mAdapter = null;
    private String mOrganizerEmail = null;
    private ArrayList<Long> mArrayList = null;
    private QueryHandler mQueryHandler = null;
    //Used to keep track of the scroll state of the list.
    private Parcelable mListState = null;
    private ListView mListView = null;

    final String[] PHONES_PROJECTION = new String[] {
        Phone._ID, //0
        Phone.TYPE, //1
        Phone.LABEL, //2
        Phone.NUMBER, //3
        Phone.DISPLAY_NAME, // 4
    };

    static final int PHONE_ID_COLUMN_INDEX = 0;
    static final int PHONE_TYPE_COLUMN_INDEX = 1;
    static final int PHONE_LABEL_COLUMN_INDEX = 2;
    static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    static final int PHONE_DISPLAY_NAME_COLUMN_INDEX = 4;

    private class QueryHandler extends AsyncQueryHandler {
        private ContentResolver mResolver;

        public QueryHandler(ContentResolver resolver) {
            super(resolver);
            mResolver = resolver;
            if (mResolver == null) {
                throw new IllegalArgumentException("Illegal Context when querying organizer info");
            }
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            if (c == null) {
                // no contacts have the given email address
                Log.d(TAG, "null cursor, no contacts have the given email address");
                mAdapter.changeCursor(null);
            } else {
                Log.d(TAG, "query complete with non-null cursor");

                Cursor c1 = null;
                boolean bClose = true; //used to close c1 whenever there're exceptions
                try {
                    if (!ConferenceCallOrganizerInfoActivity.this.isFinishing()) {
                        if (!c.moveToFirst()) {
                            // no contacts have the given email address
                            mAdapter.changeCursor(null);
                        } else {
                            mArrayList.clear();
                            String where = null;
                            do {
                                // close Cursor to avoid memory leak in the loop
                                if (c1 != null) {
                                    c1.close();
                                    c1 = null;
                                }

                                // check whether the contacts in the set have at least a phone number
                                where = Contacts.HAS_PHONE_NUMBER + "=1 AND " + Contacts._ID + "=" + c.getLong(0);
                                c1 = mResolver.query(Contacts.CONTENT_URI, new String[]{Contacts._ID}, where, null, null);
                                if (c1 != null) {
                                    if (c1.getCount() > 0) {
                                        // if this contact has phone number, recording its _id;
                                        mArrayList.add(c.getLong(0));
                                    }
                                }
                            } while (c.moveToNext());

                            // check the activity state again since we have two database oeprations in this fucntion
                            if (!ConferenceCallOrganizerInfoActivity.this.isFinishing() && !mArrayList.isEmpty()) {
                                StringBuilder selection = new StringBuilder();
                                selection.append(RawContacts.CONTACT_ID + " IN (");
                                while (!mArrayList.isEmpty()) {
                                    selection.append(mArrayList.remove(0) + ",");
                                }
                                // replace the last ',' with ')'
                                selection.setCharAt(selection.length()-1, ')');

                                // close cursor to avoid memory leak
                                if (c1 != null) {
                                    c1.close();
                                    c1 = null;
                                }

                                c1 = mResolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, selection.toString(), null, Contacts.SORT_KEY_PRIMARY);
                                if ((c1 != null) && (c1.getCount() > 0)) {
                                    Log.d(TAG, "got " + c1.getCount() + " number of contacts");
                                }
                                mAdapter.changeCursor(c1);
                                bClose = false;
                                // Now that the cursor is populated again, it's possible to restore the list state
                                if (mListState != null) {
                                    mListView.onRestoreInstanceState(mListState);
                                    mListState = null;
                                }
                            }
                        }
                    }
                } finally {
                    if (bClose && (c1 != null)) c1.close();
                    c.close();
                }
            }
        }
    }

    private void startQuery() {
        Uri uri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(mOrganizerEmail));
        String where = Email.IN_VISIBLE_GROUP + "=1";

        // cancel pending operation
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        // query contacts with given email address
        mQueryHandler.startQuery(QUERY_TOKEN, null, uri, new String[]{Email.CONTACT_ID}, where, null, null);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Intent intent = getIntent();
        mOrganizerEmail = intent.getStringExtra(EXTRA_ORGANIZER_EMAIL);
        if ((mOrganizerEmail == null) || (mOrganizerEmail.length() == 0)) {
            Toast toast = Toast.makeText(this, R.string.null_meeting_organizer, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            setResult(RESULT_CANCELED);
            finish();
        }

        mArrayList = new ArrayList<Long>();

        setContentView(R.layout.conference_call_organizer_info_layout);
        mListView = getListView();
        if (mListView != null) {
            // will draw the divider by ourselves
            mListView.setDividerHeight(0);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }

        mAdapter = new OrganizerItemAdapter(this);
        setListAdapter(mAdapter);

        mQueryHandler = new QueryHandler(getContentResolver());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startQuery();
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        // Save list state in the bundle so we can restore it after the QueryHandler has run
        if (mListView != null) {
            icicle.putParcelable(LIST_STATE_KEY, mListView.onSaveInstanceState());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
        super.onRestoreInstanceState(icicle);
        // Retrieve list state. This will be applied after the QueryHandler has run
        mListState = icicle.getParcelable(LIST_STATE_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // cancel pending operation
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        mAdapter.changeCursor(null);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent();
        final Uri uri;

        if (id > 0) {
            uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
            setResult(RESULT_OK, intent.setData(uri));
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private class OrganizerItemAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public OrganizerItemAdapter(Context context) {
            // don't requery automatically if there're changes,
            // will query it asynchronously
            super(context, null, false);
            mInflater = LayoutInflater.from(context);
        }

        /**
         * Callback on the UI thread when the content observer on the backing cursor fires.
         * Instead of calling requery we need to do an async query so that the requery doesn't
         * block the UI thread for a long time.
         */
        @Override
        protected void onContentChanged() {
            // Start an async query
            Log.v(TAG, "Content has changed, will requery");
            startQuery();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if ((mInflater == null) || (cursor == null))
                return null;

            return  mInflater.inflate(R.layout.conference_call_organizer_info_item, parent, false);
        }

        @Override
        public void bindView(View itemView, Context context, Cursor cursor) {
            if ((itemView == null) || (cursor == null))
                return;

            String displayName = cursor.getString(PHONE_DISPLAY_NAME_COLUMN_INDEX);
            String phoneNumber = cursor.getString(PHONE_NUMBER_COLUMN_INDEX);
            int type = cursor.getInt(PHONE_TYPE_COLUMN_INDEX);
            String label = cursor.getString(PHONE_LABEL_COLUMN_INDEX);

            setText(itemView, R.id.conf_call_organizer_display_name, displayName);
            setText(itemView, R.id.conf_call_organizer_phone_number, phoneNumber);
            setText(itemView, R.id.conf_call_organizer_phone_type,
                            Phone.getTypeLabel(context.getResources(), type, label).toString());
        }

        private void setText(View view, int id, String label) {
            TextView textView = (TextView)view.findViewById(id);
            if (textView == null) {
                Log.d(TAG, "There's no such text view?");
                return;
            }

            if (TextUtils.isEmpty(label)) {
                textView.setText("");
            } else {
                textView.setText(label);
            }
       }
    }
}
