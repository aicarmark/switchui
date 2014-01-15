package com.motorola.firewall;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.ListFragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.CursorLoader; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.internal.telephony.CallerInfo;

import com.motorola.firewall.FireWall.Name;

public class NameListActivity extends ListFragment {

    // Menu item ids
    public static final int MENU_DELETE_ALL = Menu.FIRST;
    public static final int MENU_DELETE = Menu.FIRST + 1;
    public static final int MENU_INSERT = Menu.FIRST + 2;
    public static final int MENU_PICK_PIM = Menu.FIRST + 3;
    public static final int MENU_PICK_RECALL = Menu.FIRST + 4;
    public static final int MENU_PICK_NAME = Menu.FIRST + 5;
    public static final int MENU_EDIT = Menu.FIRST + 6;

    private static final int PATTERN_ALERT = 1;
    private static final int NAME_ALERT = 2;
    private static final int UNKNOW_NUMBER_ALERT = 3;
    private static final int DUP_NUMBER = 4;
    private static final int INVALID_NUMBER_ALERT = 5;
    private static final int NAME_NONUMBER_ALERT = 6;
    private static final int DUP_NAME = 7;
    private static final int DELETE_ALL_ALERT = 8;
    private Activity mParentActivity;
    
    private static final int QUERY_TOKEN = 53;

    private static final String TAG = "NameList";
    private static final String[] PROJECTION = new String[] {
            Name._ID, // 0
            Name.PHONE_NUMBER_KEY, // 1
            Name.CACHED_NAME, // 2
            Name.CACHED_NUMBER_TYPE, // 3
            Name.CACHED_NUMBER_LABEL, // 4
            Name.BLOCK_TYPE, // 5
            Name.FOR_CALL, // 6
            Name.FOR_SMS   // 7
    };

    private static final String[] RECENTPROJECTION = new String[] {
            Calls._ID, // 0
            Calls.NUMBER, // 1
            Calls.CACHED_NAME, // 2
            Calls.CACHED_NUMBER_TYPE, // 3
            Calls.CACHED_NUMBER_LABEL // 4
    };

    private static final String[] NEW_PHONE_PROJECTION = new String[] {
        Phone._ID, //0
        Phone.DISPLAY_NAME, //1
        Phone.TYPE, //2
        Phone.LABEL, //3
        Phone.NUMBER, //4
        Phone.CONTACT_ID, // 5
    };
    
    private static final String[] CONTACTS_PROJECTION = new String[] {
        Contacts._ID, //0
        Contacts.DISPLAY_NAME, //1
        Contacts.HAS_PHONE_NUMBER, //2
        Contacts.LOOKUP_KEY
    };

    private static final String[] mlist = new String[] {
        Name.WHITELIST,
        Name.WHITEPATTERN,
        Name.BLACKLIST,
        Name.BLACKPATTERN,
        Name.WHITENAME,
        Name.BLACKNAME,
        Name.WHITE,
        Name.BLACK
    };
    static final int PERSON_ID_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;
    static final int PHONE_TYPE_COLUMN_INDEX = 2;
    static final int LABEL_COLUMN_INDEX = 3;
    static final int MATCHED_NUMBER_COLUMN_INDEX = 4;
    static final int HAS_PHONE_NUMBER_INDEX = 2;
    static final int LOOKUP_KEY_INDEX = 3;
    
    private static final int PICK_CONTACT = 1;
    private static final int PICK_RECENTCALL = 2;
    private static final int PICK_CONTACT_NAME = 3;

    private static final int COLUMN_INDEX_NUMBER = 1;
    private static final int COLUMN_INDEX_NAME = 2;
    private static final int COLUMN_INDEX_NUMBER_TYPE = 3;
    private static final int COLUMN_INDEX_NUMBER_LABEL = 4;
    private static final int COLUMN_INDEX_BLOCK_TYPE = 5;
    private static final int COLUMN_INDEX_FOR_CALL = 6;
    private static final int COLUMN_INDEX_FOR_SMS = 7;

    private String mblock_type;
    private String mselecting_string;
    private Cursor mCursor;
    private String mSortorder;

    private static final int INVALID = -1;

    NumberlistAdapter mNumberAdapter;
    private NumberQueryHandler mNumberQueryHandler;

    @Override
    public void onAttach (Activity activity){
        super.onAttach(activity);
        mParentActivity = activity;
    }

    public NameListActivity(String blocktype){
        super();
        mblock_type = blocktype;
    }

    private void showDialog(int dialogId){
        popupDialogFragment.show(this, dialogId, mblock_type, mselecting_string);
    }

    public static class popupDialogFragment extends DialogFragment {
        static final String ARG_DIALOG_ID = "dialogId";
        static final String ARG_BLOCKTYPE_ID = "blocktype";
        static final String ARG_SELECTEDSTR_ID = "selectstring";
        public static void show(NameListActivity fragment, int dialogId, String blocktype) {
            Bundle args = new Bundle();
            popupDialogFragment dialog = new popupDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            args.putInt(ARG_DIALOG_ID, dialogId);
            args.putString(ARG_BLOCKTYPE_ID, blocktype);
            dialog.setArguments(args);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        public static void show(NameListActivity fragment, int dialogId, String blocktype, String selectStr) {
            Bundle args = new Bundle();
            popupDialogFragment dialog = new popupDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            args.putInt(ARG_DIALOG_ID, dialogId);
            args.putString(ARG_BLOCKTYPE_ID, blocktype);
            args.putString(ARG_SELECTEDSTR_ID, selectStr);
            dialog.setArguments(args);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int dialogId= getArguments().getInt(ARG_DIALOG_ID);
            String mblock_type = getArguments().getString(ARG_BLOCKTYPE_ID);
            int mmessageid = 0;
            switch (dialogId) {
            case PATTERN_ALERT:
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_pattern_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case NAME_ALERT:
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_name_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case UNKNOW_NUMBER_ALERT:
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_unknow_number).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case DUP_NUMBER:
                if (mblock_type.equals(mlist[0]) || mblock_type.equals(mlist[6])) {
                    mmessageid = R.string.alert_dialog_dup_number_white_list;
                } else if (mblock_type.equals(mlist[2]) || mblock_type.equals(mlist[7])) {
                    mmessageid = R.string.alert_dialog_dup_number_black_list;
                }
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mmessageid).setCancelable(false).setPositiveButton(
                                R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case INVALID_NUMBER_ALERT:
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_invalid_number).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case DUP_NAME:
                if (mblock_type.equals(mlist[4])) {
                    mmessageid = R.string.alert_dialog_dup_name_white_list;
                } else if (mblock_type.equals(mlist[5])) {
                    mmessageid = R.string.alert_dialog_dup_name_black_list;
                }
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mmessageid).setCancelable(false).setPositiveButton(
                                R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case NAME_NONUMBER_ALERT:
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_name_nonumber).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismiss();
                                    }
                                }).create();
            case DELETE_ALL_ALERT: 
                if (mblock_type.equals(mlist[0])) {
                    mmessageid = R.string.alert_dialog_white_number_delete_all;
                } else if (mblock_type.equals(mlist[1])) {
                    mmessageid = R.string.alert_dialog_white_pattern_delete_all;
                } else if (mblock_type.equals(mlist[2])) {
                    mmessageid = R.string.alert_dialog_black_number_delete_all;
                } else if (mblock_type.equals(mlist[3])) {
                    mmessageid = R.string.alert_dialog_black_pattern_delete_all;
                } else if (mblock_type.equals(mlist[4])) {
                    mmessageid = R.string.alert_dialog_white_name_delete_all;
                } else if (mblock_type.equals(mlist[5])) {
                    mmessageid = R.string.alert_dialog_black_name_delete_all;
                } else if (mblock_type.equals(mlist[6])) {
                    mmessageid = R.string.alert_dialog_white_number_pattern_delete_all;
                } else if (mblock_type.equals(mlist[7])) {
                    mmessageid = R.string.alert_dialog_black_number_pattern_delete_all;
                }
                final String mselecting_string = getArguments().getString(ARG_SELECTEDSTR_ID);;
                return new AlertDialog.Builder(getActivity()).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mmessageid).setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so delete name list*/
                                getActivity().getContentResolver().delete(Name.CONTENT_URI, mselecting_string, null);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so do some stuff */
                                dismiss();
                            }
                        }).create();
        }
        return null;
        }
    }

    private SimpleCursorAdapter.ViewBinder mviewbinder = new SimpleCursorAdapter.ViewBinder() {

        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String name = cursor.getString(COLUMN_INDEX_NAME);
            String mbtype = cursor.getString(COLUMN_INDEX_BLOCK_TYPE);
            switch (columnIndex) {
                case COLUMN_INDEX_NUMBER:
                    if (view instanceof TextView) {
                        String bnumer = PhoneNumberUtils.formatNumber(cursor.getString(columnIndex));
                        ((TextView) view).setText(bnumer);
                    }
                    break;
                case COLUMN_INDEX_NAME:
                    if (view instanceof TextView) {
                        if (TextUtils.isEmpty(name)) {
                            ((TextView) view).setVisibility(View.GONE);
                        } else {
                            ((TextView) view).setText(name);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case COLUMN_INDEX_NUMBER_TYPE:
                     //fall through
                case COLUMN_INDEX_NUMBER_LABEL:
                    if (view instanceof TextView) {
                        if (TextUtils.isEmpty(name) || mbtype.equals(Name.WHITEPATTERN) || mbtype.equals(Name.BLACKPATTERN)) {
                            ((TextView) view).setVisibility(View.GONE);
                        } else {
                            int type = cursor.getInt(COLUMN_INDEX_NUMBER_TYPE);
                            CharSequence label = cursor.getString(COLUMN_INDEX_NUMBER_LABEL);
                            CharSequence mlabel = Phone.getTypeLabel(getResources(), type, label);
                            ((TextView) view).setText(mlabel);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case COLUMN_INDEX_BLOCK_TYPE:
                    if (view instanceof TextView) {
                        String[] mnametitle = getResources().getStringArray(R.array.white_black_list_title);
                        if (mbtype.equals(Name.WHITEPATTERN)) {
                            ((TextView) view).setText(mnametitle[1]);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        } else if (mbtype.equals(Name.BLACKPATTERN)) {
                            ((TextView) view).setText(mnametitle[3]);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        } 
                    }
                    break;
                case COLUMN_INDEX_FOR_CALL:
                    int mCall = cursor.getInt(COLUMN_INDEX_FOR_CALL);
                    if (view instanceof ImageView) {
                    /*2012-09-12, DJHV83 add for Firewall UI modication.*/
                    //Modify the icon only for VIP list.
                    if(mblock_type.equals(mlist[6])){
                        if (mCall == 0) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_vip_call_off);
                        } else {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_vip_call_on);
                        }
                    }else {
                        if (mCall == 0) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_call_off);
                        } else {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_call_on);
                        }
                    }
                    /*2012-09-12 end*/
                    }
                    break;
                case COLUMN_INDEX_FOR_SMS:
                    int mSms = cursor.getInt(COLUMN_INDEX_FOR_SMS);
                    if (view instanceof ImageView) {
                    /*2012-09-12, DJHV83 add for Firewall UI modication.*/
                    //Modify the icon only for VIP list.
                    if(mblock_type.equals(mlist[6])){
                        if (mSms == 0) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_vip_sms_off);
                        } else {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_vip_sms_on);
                        }
                    }else {
                        if (mSms == 0) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_sms_off);
                        } else {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_sms_on);
                        }
                    }
                    /*2012-09-12 end*/
                    }
                    break;
            }
            return true;
        }
    };
    
    final class NumberlistAdapter extends SyncNumberAdapter {
        public NumberlistAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        };
        protected void updateList(CallerInfoQuery ciq, ContactInfo ci) {
            // Check if they are different. If not, don't update.
            if (TextUtils.equals(ciq.name, ci.name) && TextUtils.equals(ciq.numberLabel, ci.label)
                    && ciq.numberType == ci.type) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put(Name.CACHED_NAME, ci.name);
            values.put(Name.CACHED_NUMBER_TYPE, ci.type);
            values.put(Name.CACHED_NUMBER_LABEL, ci.label);
            mParentActivity.getContentResolver().update(
                    Name.CONTENT_URI,
                    values, Name.PHONE_NUMBER_KEY + "='" + ciq.number + "'", null);
        }
    }
    
    
    private static final class NumberQueryHandler extends AsyncQueryHandler {
        private final WeakReference<NameListActivity> mActivity;

        public NumberQueryHandler(Fragment context) {
            super(context.getActivity().getContentResolver());
            mActivity = new WeakReference<NameListActivity>((NameListActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final NameListActivity activity = mActivity.get();

            if (activity != null && !activity.isDetached()) {
                final NameListActivity.NumberlistAdapter callsAdapter = activity.mNumberAdapter;
                callsAdapter.setLoading(false);
                callsAdapter.changeCursor(cursor);
                activity.setHasOptionsMenu(true);
            } else {
                cursor.close();
            }
        }
    }
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        if (intent.getData() == null) {
            intent.setData(Name.CONTENT_URI);
        }

        String[] mnametitle = getResources().getStringArray(R.array.white_black_list_title);
        //mblock_type = intent.getStringExtra("blocktype");
        if (mblock_type == null) {
            Log.e(TAG, "Unknown block type, exiting");
            //dismiss();
            return;
        }

        // Perform a managed query. The Activity will handle closing and
        // requerying the cursor
        // when needed.
        if (mblock_type.equals(mlist[6])) {
            mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.WHITELIST + "'" +
                " OR " + Name.BLOCK_TYPE + " = " + "'" + Name.WHITEPATTERN + "'";
            mSortorder = Name.COMBINED_SORT_ORDER;
        } else if (mblock_type.equals(mlist[7])) {
            mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.BLACKLIST + "'" +
                " OR " + Name.BLOCK_TYPE + " = " + "'" + Name.BLACKPATTERN + "'";
            mSortorder = Name.COMBINED_SORT_ORDER;
        } else {
            mselecting_string = Name.BLOCK_TYPE + " = " + "'" + mblock_type + "'";
            mSortorder = Name.DEFAULT_SORT_ORDER;
        }

        // SimpleCursorAdapter adapter = null;
        String mtitle = null;
        if (mblock_type.equals(mlist[0])) {
            mNumberAdapter = new NumberlistAdapter(mParentActivity, R.layout.number_list_item, null, new String[] {
                    Name.CACHED_NAME, Name.PHONE_NUMBER_KEY, Name.CACHED_NUMBER_TYPE, Name.CACHED_NUMBER_LABEL
            }, new int[] {
                    R.id.Contactname, R.id.Phonenumber, R.id.Contacttype, R.id.Contacttype
            });
            //setContentView(R.layout.white_number);
            mNumberAdapter.setViewBinder(mviewbinder);
            setListAdapter(mNumberAdapter);
            mtitle = mnametitle[0];
        } else if (mblock_type.equals(mlist[2])) {
            mNumberAdapter = new NumberlistAdapter(mParentActivity, R.layout.number_list_item, null, new String[] {
                    Name.CACHED_NAME, Name.PHONE_NUMBER_KEY, Name.CACHED_NUMBER_TYPE, Name.CACHED_NUMBER_LABEL
            }, new int[] {
                    R.id.Contactname, R.id.Phonenumber, R.id.Contacttype, R.id.Contacttype
            });
            //setContentView(R.layout.black_number);
            mNumberAdapter.setViewBinder(mviewbinder);
            setListAdapter(mNumberAdapter);
            mtitle = mnametitle[2];
        } else if (mblock_type.equals(mlist[1])) {
            // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        /*Cursor cursor = managedQuery(intent.getData(), PROJECTION, mselecting_string, null,
                                         mSortorder);*/
        CursorLoader cursorLoader = new CursorLoader(mParentActivity, intent.getData(), PROJECTION, mselecting_string, null, mSortorder);
        Cursor cursor = cursorLoader.loadInBackground();
        // MOT Calling Code End
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(mParentActivity, R.layout.pattern_list_item, cursor,
                    new String[] {
                        Name.PHONE_NUMBER_KEY
                    }, new int[] {
                        R.id.Pattern
                    });
            //setContentView(R.layout.white_pattern);
            setListAdapter(adapter);
            mtitle = mnametitle[1];
        } else if (mblock_type.equals(mlist[3])) {
            // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        /*Cursor cursor = managedQuery(intent.getData(), PROJECTION, mselecting_string, null,
                                         mSortorder);*/
            CursorLoader cursorLoader = new CursorLoader(mParentActivity, intent.getData(), PROJECTION, mselecting_string, null, mSortorder);
        Cursor cursor = cursorLoader.loadInBackground();
        // MOT Calling Code End
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(mParentActivity, R.layout.pattern_list_item, cursor,
                    new String[] {
                        Name.PHONE_NUMBER_KEY
                    }, new int[] {
                        R.id.Pattern
                    });
            //setContentView(R.layout.black_pattern);
            setListAdapter(adapter);
            mtitle = mnametitle[3];
        } if (mblock_type.equals(mlist[6])) {
            mNumberAdapter = new NumberlistAdapter(mParentActivity, R.layout.number_list_item, null, new String[] {
                    Name.CACHED_NAME, Name.PHONE_NUMBER_KEY, Name.CACHED_NUMBER_TYPE, Name.CACHED_NUMBER_LABEL, 
                    Name.BLOCK_TYPE, Name.FOR_CALL, Name.FOR_SMS
            }, new int[] {
                    R.id.Contactname, R.id.Phonenumber, R.id.Contacttype, R.id.Contacttype, 
                    R.id.Contactname, R.id.for_call, R.id.for_sms
            });
            //setContentView(R.layout.white_number);
            mNumberAdapter.setViewBinder(mviewbinder);
            setListAdapter(mNumberAdapter);
            mtitle = mnametitle[0];
        } else if (mblock_type.equals(mlist[7])) {
            mNumberAdapter = new NumberlistAdapter(mParentActivity, R.layout.number_list_item, null, new String[] {
                    Name.CACHED_NAME, Name.PHONE_NUMBER_KEY, Name.CACHED_NUMBER_TYPE, Name.CACHED_NUMBER_LABEL, 
                    Name.BLOCK_TYPE, Name.FOR_CALL, Name.FOR_SMS
            }, new int[] {
                    R.id.Contactname, R.id.Phonenumber, R.id.Contacttype, R.id.Contacttype, 
                    R.id.Contactname, R.id.for_call, R.id.for_sms
            });
            //setContentView(R.layout.black_number);
            mNumberAdapter.setViewBinder(mviewbinder);
            setListAdapter(mNumberAdapter);
            mtitle = mnametitle[2];
        } else {
            Log.e(TAG, "Unknown block type, exiting");
            //finish();
            return;
        }

        // Inform the list we provide context menus for items
        //getListView().setOnCreateContextMenuListener(this);

        mNumberQueryHandler = new NumberQueryHandler(this);
        mParentActivity.setTitle(mtitle);
        setHasOptionsMenu(false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnCreateContextMenuListener(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View nameListLayout = null;
        if (mblock_type.equals(mlist[0])){
            nameListLayout = inflater.inflate(R.layout.white_number, container, false);
        }else if (mblock_type.equals(mlist[2])) {
            nameListLayout = inflater.inflate(R.layout.black_number, container, false);
        }else if (mblock_type.equals(mlist[1])) {
            nameListLayout = inflater.inflate(R.layout.white_pattern, container, false);
        }else if (mblock_type.equals(mlist[3])) {
            nameListLayout = inflater.inflate(R.layout.black_pattern, container, false);
        } if (mblock_type.equals(mlist[6])) {
            nameListLayout = inflater.inflate(R.layout.white_number, container, false);
        } else if (mblock_type.equals(mlist[7])) {
            nameListLayout = inflater.inflate(R.layout.black_number, container, false);
        }
        
        return nameListLayout;
    }

    @Override
    public void onResume() {
        super.onResume(); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
    // The adapter caches looked up numbers, clear it so they will get
        // looked up again.
        if (mNumberAdapter != null) {
            mNumberAdapter.clearCache();
            mNumberAdapter.setLoading(true);
            setHasOptionsMenu(false);
            numberstartQuery();
            mNumberAdapter.mPreDrawListener = null; // Let it restart the thread after
            // next draw
        }
        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        SimpleCursorAdapter mAdapter = (SimpleCursorAdapter) getListAdapter();
        if (mAdapter != null) {
            Cursor cursor = mAdapter.getCursor();
            if (cursor != null) {
                cursor.close();
            }
            mAdapter.changeCursor(null);
            setListAdapter(null);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Black pattern list only need two menu items
        if (mblock_type.equals(mlist[1])) {
            menu.add(0, MENU_INSERT, 0, R.string.menu_insert_white_pattern).setShortcut('3', 'a')
                    .setIcon(android.R.drawable.ic_menu_add); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        } else if (mblock_type.equals(mlist[3])) {
            menu.add(0, MENU_INSERT, 0, R.string.menu_insert_black_pattern).setShortcut('3', 'a')
                    .setIcon(android.R.drawable.ic_menu_add); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        } else if (mblock_type.equals(mlist[0]) || mblock_type.equals(mlist[6])){
            menu.add(0, MENU_PICK_PIM, 0, R.string.menu_pick_pim).setShortcut('5', 'p').setIcon(
                    R.drawable.firewall_menu_pickup_contact);

            menu.add(0, MENU_PICK_RECALL, 0, R.string.menu_pick_recall).setShortcut('6', 'r')
                    .setIcon(R.drawable.firewall_menu_pickup_recentcall);

            menu.add(0, MENU_INSERT, 0, R.string.menu_insert_white_list).setShortcut('3', 'a').setIcon(
                    android.R.drawable.ic_menu_add); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS

        } else if (mblock_type.equals(mlist[2]) || mblock_type.equals(mlist[7])){
            menu.add(0, MENU_PICK_PIM, 0, R.string.menu_pick_pim).setShortcut('5', 'p').setIcon(
                    R.drawable.firewall_menu_pickup_contact);

            menu.add(0, MENU_PICK_RECALL, 0, R.string.menu_pick_recall).setShortcut('6', 'r')
                    .setIcon(R.drawable.firewall_menu_pickup_recentcall);

            menu.add(0, MENU_INSERT, 0, R.string.menu_insert_black_list).setShortcut('3', 'a').setIcon(
                    android.R.drawable.ic_menu_add); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS

        } else if (mblock_type.equals(mlist[4]) || mblock_type.equals(mlist[5])){
            menu.add(0, MENU_PICK_NAME, 0, R.string.menu_pick_pim).setShortcut('5', 'p').setIcon(
                    R.drawable.firewall_menu_pickup_contact);
        }

        menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setShortcut('4', 'd')
                .setIcon(android.R.drawable.ic_menu_delete); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if (getListAdapter().isEmpty()) {
            menu.findItem(MENU_DELETE_ALL).setEnabled(false);
        } else {
            menu.findItem(MENU_DELETE_ALL).setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//      int mcount = getListAdapter().getCount();
        int mcount = 0;
        String select;
        if (TextUtils.equals(mblock_type, Name.BLACK)) {
            select = Name.BLOCK_TYPE + " = '" + Name.BLACKLIST +"'";
        } else {
            select = Name.BLOCK_TYPE + " = '" + Name.WHITELIST +"'";
        } 
        Cursor cursor = mParentActivity.getContentResolver().query(Name.CONTENT_URI, PROJECTION, select, null, null);
        if (cursor != null) {
            mcount = cursor.getCount();
            cursor.close();
        }
        switch (item.getItemId()) {
            case MENU_INSERT:
//                if (reachMax(mcount)) {
//                    return true;
//                }
                // Launch activity to insert a new item
                Intent mIntent = new Intent(Intent.ACTION_INSERT, Name.CONTENT_URI);
                mIntent.setType(FireWall.Name.CONTENT_TYPE);
                mIntent.putExtra("blocktype", mblock_type);
                startActivity(mIntent);
                return true;
            case MENU_DELETE_ALL:
                // Delete Call Name table
                showDialog(DELETE_ALL_ALERT);
                return true;
            case MENU_PICK_PIM:
                if (reachMax(mcount)) {
                    return true;
                }
                startActivityForResult(new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI), PICK_CONTACT);

                return true;
            case MENU_PICK_RECALL:
                if (reachMax(mcount)) {
                    return true;
                }
                startActivityForResult(new Intent(Intent.ACTION_PICK, Calls.CONTENT_URI, mParentActivity, RecentCallsListActivity.class),
                        PICK_RECENTCALL);
                return true;
            case MENU_PICK_NAME:
                if (reachMax(mcount)) {
                    return true;
                }
                startActivityForResult(new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), PICK_CONTACT_NAME);

                return true;
            
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        if (mblock_type.equals(mlist[1]) || mblock_type.equals(mlist[3])) {
            menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_NUMBER) + " " + getString(R.string.pattern_title));
        } else if (mblock_type.equals(mlist[4]) || mblock_type.equals(mlist[5])) {
            menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_NAME));
        } else {
            String mname = cursor.getString(COLUMN_INDEX_NAME);
            if (mname == null) {
                menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_NUMBER));
            } else {
                menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_NUMBER) + " " + mname);
            }

        }

        // Add a menu item to delete the note
        menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_DELETE: {
                // Delete the item that the context menu is for
                Uri nameUri = ContentUris.withAppendedId(Name.CONTENT_URI, info.id);
                mParentActivity.getContentResolver().delete(nameUri, null, null);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT) {
            if (resultCode == mParentActivity.RESULT_OK) {
                // A contact was picked. Here we will just display it
                // to the user.
                Uri cUri = data.getData();
                if (cUri == null) {
                    return;
                }
        // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
                // mCursor = managedQuery(cUri, NEW_PHONE_PROJECTION, null, null, null);
                CursorLoader cursorLoader = new CursorLoader(mParentActivity, cUri, NEW_PHONE_PROJECTION, null, null, null);
            Cursor mCursor = cursorLoader.loadInBackground();
        // MOT Calling Code End
        if (mCursor != null) {
                    if (mCursor.moveToFirst()) {
                        // Write our text back into the provider.
                        String mnumber = mCursor.getString(MATCHED_NUMBER_COLUMN_INDEX);
                        String mnumberkey = PhoneNumberUtils.extractNetworkPortion(mnumber);
                        if (TextUtils.isEmpty(mnumberkey)) {
                            showDialog(INVALID_NUMBER_ALERT);
                        } else if (checkDup (mParentActivity.getContentResolver() ,mnumberkey, mblock_type, false)) {
                            showDialog(DUP_NUMBER);
                        } else {
                            ContentValues values = new ContentValues();
                            if (mblock_type.equals(mlist[6])) {
                                values.put(Name.BLOCK_TYPE, Name.WHITELIST);
                                values.put(Name.FOR_CALL, true);
                                values.put(Name.FOR_SMS, true);
                            } else if (mblock_type.equals(mlist[7])) {
                                values.put(Name.BLOCK_TYPE, Name.BLACKLIST);
                                values.put(Name.FOR_CALL, true);
                                values.put(Name.FOR_SMS, true);
                            } else {
                                values.put(Name.BLOCK_TYPE, mblock_type);
                            }
                            values.put(Name.PHONE_NUMBER_KEY, mnumberkey);
                            values.put(Name.CACHED_NAME, mCursor.getString(NAME_COLUMN_INDEX));
                            values.put(Name.CACHED_NUMBER_TYPE, mCursor.getInt(PHONE_TYPE_COLUMN_INDEX));
                            values.put(Name.CACHED_NUMBER_LABEL, mCursor.getString(LABEL_COLUMN_INDEX));

                            // Commit all of our changes to persistent storage. When the
                            // update completes
                            // the content provider will notify the cursor of the change,
                            // which will
                            // cause the UI to be updated.
                            mParentActivity.getContentResolver().insert(Name.CONTENT_URI, values);
                        }
                    }

                    mCursor.close();
                }
            }
        } else if (requestCode == PICK_RECENTCALL) {
            if (resultCode == Activity.RESULT_OK) {
                // A contact was picked. Here we will just display it
                // to the user.
                Uri cUri = data.getData();
                if (cUri == null) {
                    return;
                }
        // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
                // mCursor = managedQuery(cUri, RECENTPROJECTION, null, null, null);
                CursorLoader cursorLoader = new CursorLoader(mParentActivity, cUri, RECENTPROJECTION, null, null, null);
            Cursor mCursor = cursorLoader.loadInBackground();
        // MOT Calling Code End
        if (mCursor != null) {
                    if (mCursor.moveToFirst()) {
                        // Write our text back into the provider.
                        String mnumber = mCursor.getString(COLUMN_INDEX_NUMBER);
                        if (mnumber.equals(CallerInfo.UNKNOWN_NUMBER) || mnumber.equals(CallerInfo.PRIVATE_NUMBER)) {
                            showDialog(UNKNOW_NUMBER_ALERT);
                        } else {
                            String mnumberkey = PhoneNumberUtils.extractNetworkPortion(mnumber);
                            if (TextUtils.isEmpty(mnumberkey)) {
                                showDialog(INVALID_NUMBER_ALERT);
                            } else if (checkDup (mParentActivity.getContentResolver() ,mnumberkey, mblock_type, false)) {
                                showDialog(DUP_NUMBER);
                            } else {
                                ContentValues values = new ContentValues();
                                if (mblock_type.equals(mlist[6])) {
                                    values.put(Name.BLOCK_TYPE, Name.WHITELIST);
                                    values.put(Name.FOR_CALL, true);
                                    values.put(Name.FOR_SMS, true);
                                } else if (mblock_type.equals(mlist[7])) {
                                    values.put(Name.BLOCK_TYPE, Name.BLACKLIST);
                                    values.put(Name.FOR_CALL, true);
                                    values.put(Name.FOR_SMS, true);
                                } else {
                                    values.put(Name.BLOCK_TYPE, mblock_type);
                                }
                                values.put(Name.PHONE_NUMBER_KEY, mnumberkey);
                                values.put(Name.CACHED_NAME, mCursor.getString(COLUMN_INDEX_NAME));
                                values.put(Name.CACHED_NUMBER_TYPE, mCursor.getInt(COLUMN_INDEX_NUMBER_TYPE));
                                values.put(Name.CACHED_NUMBER_LABEL, mCursor.getString(COLUMN_INDEX_NUMBER_LABEL));

                                // Commit all of our changes to persistent storage. When the
                                // update completes
                                // the content provider will notify the cursor of the change,
                                // which will
                                // cause the UI to be updated.
                                mParentActivity.getContentResolver().insert(Name.CONTENT_URI, values);
                            }
                        }
                    }

                    mCursor.close();
                }
            }
        } else if (requestCode == PICK_CONTACT_NAME) {
            if (resultCode == Activity.RESULT_OK) {
                // A contact was picked. Here we will just display it
                // to the user.
                Uri cUri = data.getData();
                if (cUri == null) {
                    return;
                }
        // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
                // mCursor = managedQuery(cUri, CONTACTS_PROJECTION, null, null, null);
                CursorLoader cursorLoader = new CursorLoader(mParentActivity, cUri, CONTACTS_PROJECTION, null, null, null);
            Cursor mCursor = cursorLoader.loadInBackground();
        // MOT Calling Code End
        if (mCursor != null) {
                    if (mCursor.moveToFirst()) {
                        // Write our text back into the provider.
                        String mname = mCursor.getString(NAME_COLUMN_INDEX);
                        int mhasphone = mCursor.getInt(HAS_PHONE_NUMBER_INDEX);
                        String mlookup = mCursor.getString(LOOKUP_KEY_INDEX);
                        //check whether this contact have at lease one phone number
                        if ( mhasphone == 0 ) {
                            showDialog(NAME_NONUMBER_ALERT);
                        } else if (checkDup (mParentActivity.getContentResolver() ,mlookup, mblock_type, false)) {
                            showDialog(DUP_NAME);
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(Name.BLOCK_TYPE, mblock_type);
                            values.put(Name.CACHED_NAME, mname);
                            values.put(Name.PHONE_NUMBER_KEY, mlookup);
                            // Commit all of our changes to persistent storage. When the
                            // update completes
                            // the content provider will notify the cursor of the change,
                            // which will
                            // cause the UI to be updated.
                            mParentActivity.getContentResolver().insert(Name.CONTENT_URI, values);
                        }
                    }

                    mCursor.close();
                }
            }
        }
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        boolean tabscrolled = false;
        Activity parent = mParentActivity;
        if (parent != null && parent instanceof FireWallTab) {
            if (((FireWallTab)parent).mDirection != INVALID) {
                tabscrolled = true;
            }
        }
        if (! tabscrolled) {
            Uri nameUri = ContentUris.withAppendedId(Name.CONTENT_URI, id);
            Intent mIntent = new Intent(Intent.ACTION_EDIT, nameUri);
            mIntent.putExtra("blocktype", mblock_type);
            startActivity(mIntent);
        } else {
            ((FireWallTab)parent).mDirection = INVALID;
        }
    }
    
    private void numberstartQuery() {
        // Cancel any pending queries
        mNumberQueryHandler.cancelOperation(QUERY_TOKEN);
        mNumberQueryHandler.startQuery(QUERY_TOKEN, null, Name.CONTENT_URI,
                PROJECTION, mselecting_string, null, mSortorder);
    }

    private boolean reachMax(int mcount) {
        if (mblock_type.equals(mlist[1]) || mblock_type.equals(mlist[3])) {
            if (mcount >= Name.MAXBLACKPATTERNS) {
                showDialog(PATTERN_ALERT);
                return true;
            }
        } else {
            if (mcount >= Name.MAXNAMEITEMS) {
                showDialog(NAME_ALERT);
                return true;
            }
        }
        return false;
    }
    
    protected static boolean checkDup(ContentResolver mconres, String phonenumber, String blocktype, boolean is_pattern) {
        
        String mbtype = blocktype;
        if (is_pattern) {
            if (blocktype.equals(Name.BLACK) || blocktype.equals(Name.BLACKNAME)) {
                mbtype = Name.BLACKPATTERN;
            } else if (blocktype.equals(Name.WHITE) || blocktype.equals(Name.WHITENAME)) {
                mbtype = Name.WHITEPATTERN;
            }  
        } else {
            if (blocktype.equals(Name.BLACK) || blocktype.equals(Name.BLACKNAME)) {
                mbtype = Name.BLACKLIST;
            } else if (blocktype.equals(Name.WHITE) || blocktype.equals(Name.WHITENAME)) {
                mbtype = Name.WHITELIST;
            }  
        }
        String selecting_string = Name.BLOCK_TYPE + " = " + "'" + mbtype + "'" + " and "
                + Name.PHONE_NUMBER_KEY + " = " + "'" + phonenumber + "'";

        Cursor phonesCursor = mconres.query(Name.CONTENT_URI,
                PROJECTION, selecting_string, null, null);
        if (phonesCursor != null) {
            if (phonesCursor.moveToFirst()) {
                phonesCursor.close();
                return true;
            } else {
                phonesCursor.close();
                return false;
            }
        } else {
            return false;
        }
    }
}
