package com.motorola.numberlocation;


import java.io.File;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;



import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
 
 

import com.motorola.numberlocation.INumberLocationCallback;
import com.motorola.numberlocation.INumberLocationService;
import com.motorola.numberlocation.NumberLocationProvider.countryColumns;
import com.motorola.numberlocation.R;
import com.motorola.numberlocation.NumberLocationProvider.cityColumns;

public class NumberLocationActivity extends Activity implements OnClickListener{
    private static final String TAG = "NumberLocationActivity";


    private INumberLocationService mNumberLocationService;

    private ImageButton btnSearchNumberLocation;
    private ImageButton btnSearchNumberLocationCityCode;
    private ImageButton btnSearchNumberLocationCountryCode;
    private TextView actvNumber;
    private AutoCompleteTextView mActvNumberCityCode;//fwr687
    private AutoCompleteTextView mActvNumberCountryCode;//fwr687

    private boolean isNumberLocationBindOK;
    private ProgressDialog mDownloadProgressDialog;
    private ProgressDialog mDatabaseUpdateProgressDialog;

    private Context mContext;

    private static final String DATABASE_NUBMER_LOCATION_FILENAME = "number_location.db";



    private Handler mHandler = new Handler(){
        Bundle data = null;
        int type = 0;
        int progress = 0;
        int max = 0;

        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NumberLocationConst.ID_ALERT_DIALOG_SHOW_UPDATE_SUCCESSFULLY:
                Builder alertDialogBuilderUpdateSuccessfully = constructUpdateResultDialog(mContext.getText(R.string.title_update_successfully).toString());
                alertDialogBuilderUpdateSuccessfully.show();
                break;
            case NumberLocationConst.ID_ALERT_DIALOG_SHOW_UPDATE_NOT_FOUND:
                Builder alertDialogBuilderUpdateNotFound = constructUpdateResultDialog(mContext.getText(R.string.title_update_not_found).toString());
                alertDialogBuilderUpdateNotFound.show();
                break;
            case NumberLocationConst.ID_ALERT_DIALOG_SHOW_NETWORK_FAILURE:
                Builder alertDialogBuilderNetworkFailure = constructUpdateResultDialog(mContext.getText(R.string.title_network_fail).toString());
                alertDialogBuilderNetworkFailure.show();
                break;
            case NumberLocationConst.ID_ALERT_DIALOG_SHOW_USER_CANCEL:
                Builder alertDialogBuilderUserCancel = constructUpdateResultDialog(mContext.getText(R.string.title_update_user_cancel).toString());
                alertDialogBuilderUserCancel.show();
                break;
            case NumberLocationConst.ID_PROGRESS_DIALOG_UPDATE:
                data = msg.getData();
                type = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE);
                progress = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_DATA);
                if(type == NumberLocationConst.PROGRESS_TYPE_DOWNLOAD){
                    mDownloadProgressDialog.setProgress(progress);
                }else if(type == NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE){
                    mDatabaseUpdateProgressDialog.setProgress(progress);
                }
                break;
            case NumberLocationConst.ID_PROGRESS_DIALOG_SHOW:
                data = msg.getData();
                type = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE);
                if(type == NumberLocationConst.PROGRESS_TYPE_DOWNLOAD){
                    mDownloadProgressDialog.show();
                }else if(type == NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE){
                    mDatabaseUpdateProgressDialog.show();
                }
                break;
            case NumberLocationConst.ID_PROGRESS_DIALOG_DISMISS:
                data = msg.getData();
                type = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE);
                if(type == NumberLocationConst.PROGRESS_TYPE_DOWNLOAD){
                    mDownloadProgressDialog.dismiss();
                }else if(type == NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE){
                    mDatabaseUpdateProgressDialog.dismiss();
                }
                break;
            case NumberLocationConst.ID_PROGRESS_DIALOG_SET_MAX:
                data = msg.getData();
                type = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE);
                max = data.getInt(NumberLocationConst.BUNDLE_PROGRESS_MAX);
                if(type == NumberLocationConst.PROGRESS_TYPE_DOWNLOAD){
                    mDownloadProgressDialog.setMax(max);
                }else if(type == NumberLocationConst.PROGRESS_TYPE_DATABASE_UPDATE){
                    mDatabaseUpdateProgressDialog.setMax(max);
                }
                break;

            //fwr687 begin 	IKSF-7238
            case NumberLocationConst.ID_SEARCH_RESULT_NUM:
                 Bundle data_result_num = msg.getData();
                 int num = data_result_num.getInt(NumberLocationConst.BUNDLE_DATA_RESULT_NUM);
                 int[] location = new int[2] ;
                 mActvNumberCityCode.getLocationInWindow(location);
                 if(num>1)
                     mActvNumberCityCode.setDropDownHeight(location[1]-100);
                 else
                     mActvNumberCityCode.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
               //fwr687 end
            }
        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        setContentView(R.layout.number_location);

        btnSearchNumberLocation = (ImageButton) findViewById(R.id.btnSearch);
        actvNumber = (TextView) findViewById(R.id.actvnumberlocation);
        btnSearchNumberLocation.setOnClickListener(this);

        btnSearchNumberLocationCityCode = (ImageButton) findViewById(R.id.btnSearchCityCode);
        mActvNumberCityCode = (AutoCompleteTextView) findViewById(R.id.actvnumberlocationcitycode);//fwr687
        btnSearchNumberLocationCityCode.setOnClickListener(this);

        btnSearchNumberLocationCountryCode = (ImageButton) findViewById(R.id.btnSearchCountryCode);
        mActvNumberCountryCode = (AutoCompleteTextView) findViewById(R.id.actvnumberlocationcountrycode);//fwr687
        btnSearchNumberLocationCountryCode.setOnClickListener(this);


        bindNumberLocationService();
/*
        String loc = mContext.getResources().getConfiguration().locale.getCountry();
        if (loc != null && loc.equals("CN")) {
            TextView tv_search_city_code = (TextView) findViewById(R.id.title_search_city_code);
            if (tv_search_city_code == null) {
                return;
            }
            LinearLayout.LayoutParams linearParamsSearchCityCode = (LinearLayout.LayoutParams) tv_search_city_code.getLayoutParams();
            linearParamsSearchCityCode.height = 45;//single line height.
            tv_search_city_code.setLayoutParams(linearParamsSearchCityCode);
            TextView tv_search_country_code = (TextView) findViewById(R.id.title_search_country_code);
            if (tv_search_country_code == null) {
                return;
            }
            LinearLayout.LayoutParams linearParamsSearchCountryCode = (LinearLayout.LayoutParams) tv_search_country_code.getLayoutParams();
            linearParamsSearchCountryCode.height = 45;//single line height.
            tv_search_country_code.setLayoutParams(linearParamsSearchCountryCode);
        }
*/
    }

    /**
     *
     */
    private void initProgressDialog() {
        mDownloadProgressDialog = new ProgressDialog(NumberLocationActivity.this);
        mDownloadProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });
        mDownloadProgressDialog
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (isNumberLocationBindOK && mNumberLocationService != null) {
                            try {
                                mNumberLocationService.cancelUpdateNumberLocationDatabase();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        mDownloadProgressDialog.setTitle(getString(R.string.title_package_downloading));
        mDownloadProgressDialog.setCancelable(true);
        mDownloadProgressDialog.setMax(100);
//		mDownloadProgressDialog.setIcon(android.R.drawable.ic_dialog_alert);
        mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);


        mDatabaseUpdateProgressDialog = new ProgressDialog(NumberLocationActivity.this);
        mDatabaseUpdateProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        mDatabaseUpdateProgressDialog
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (isNumberLocationBindOK && mNumberLocationService != null) {
                    try {
                        mNumberLocationService.cancelUpdateNumberLocationDatabase();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mDatabaseUpdateProgressDialog.setTitle(getString(R.string.title_database_updating));
        mDatabaseUpdateProgressDialog.setCancelable(true);
        mDatabaseUpdateProgressDialog.setMax(0);
//		mDatabaseUpdateProgressDialog.setIcon(android.R.drawable.ic_dialog_alert);
        mDatabaseUpdateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    //fwr687 begin
    @Override
    protected void onResume() {
        super.onResume();
     // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.motorola.numberlocation.database");
        registerReceiver(mIntentReceiver, filter);
        if(NumberLocationDatabaseExists())//fwr687
            initListAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mIntentReceiver");
            String action = intent.getAction();
            if (action.equals("com.motorola.numberlocation.database")) {
                initListAdapter();
            }
        }
    };
  //fwr687 end
  //fwr687 begin
  
    private void initListAdapter() {
        ContentResolver content = getContentResolver();
        try {
            Cursor cityCursor = content.query(NumberLocationProviderConst.CITY_URI, new String[] { cityColumns.ID,
                    cityColumns.CITY }, null, null, null);
            Cursor countryCursor = null;
            Cursor countryCursor_en=null;
          	Cursor []mCursors=new Cursor[2];
			 
			 			countryCursor = content.query(NumberLocationProviderConst.COUNTRY_URI, new String[] {
						countryColumns.ID, countryColumns.COUNTRY }, null, null, null);
			 
						countryCursor_en = content.query(NumberLocationProviderConst.COUNTRY_URI, new String[] {
						countryColumns.ID, countryColumns.COUNTRY_EN }, null, null, null);
						
            startManagingCursor(cityCursor);
            startManagingCursor(countryCursor);
            startManagingCursor(countryCursor_en);
            mCursors[0]=countryCursor;
			 			mCursors[1]=countryCursor_en;
            CityListAdapter cityAdapter = new CityListAdapter(NumberLocationActivity.this, cityCursor);
            mActvNumberCityCode.setAdapter(cityAdapter);
            MergeCursor mergeCursor=new MergeCursor(mCursors);
            CountryListAdapter countryAdapter = new CountryListAdapter(NumberLocationActivity.this, mergeCursor);
            mActvNumberCountryCode.setAdapter(countryAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
  //fwr687 end

    @Override
    protected void onDestroy() {
        if (mNumberLocationService != null) {
            try {
                mNumberLocationService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        unbindNumberLocationService();
        super.onDestroy();
    }

    private ServiceConnection mNumberLocationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mNumberLocationService = INumberLocationService.Stub.asInterface(service);
            try {
                mNumberLocationService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mNumberLocationService = null;
        }
    };

    private void bindNumberLocationService() {
        Log.d(TAG, "bindNumberLocationService");
        if (!isNumberLocationBindOK) {
            isNumberLocationBindOK = this.getApplicationContext().bindService(new Intent(INumberLocationService.class.getName()), mNumberLocationConnection,
                    Context.BIND_AUTO_CREATE);
        }
        Log.i(TAG, "isNumberLocationBindOK: "+isNumberLocationBindOK);
    }

    private void unbindNumberLocationService() {
        Log.d(TAG, "unbindNumberLocationService");
        if (mNumberLocationService != null) {
            if (isNumberLocationBindOK) {
                this.getApplicationContext().unbindService(mNumberLocationConnection);
                isNumberLocationBindOK = false;
            }
            mNumberLocationService = null;
        }
    }

    private INumberLocationCallback.Stub mCallback = new INumberLocationCallback.Stub() {

        public void showResult(int result) throws RemoteException{
            if(result==NumberLocationConst.STATUS_SUCCESS){
                Message msg=new Message();
                msg.what = NumberLocationConst.ID_ALERT_DIALOG_SHOW_UPDATE_SUCCESSFULLY;
                mHandler.sendMessage(msg);
            }
            else if(result==NumberLocationConst.STATUS_NOT_FOUND){//latest version
                Message msg=new Message();
                msg.what = NumberLocationConst.ID_ALERT_DIALOG_SHOW_UPDATE_NOT_FOUND;
                mHandler.sendMessage(msg);
            }
            else if(result==NumberLocationConst.STATUS_NETWORK_FAIL){
                Message msg=new Message();
                msg.what = NumberLocationConst.ID_ALERT_DIALOG_SHOW_NETWORK_FAILURE;
                mHandler.sendMessage(msg);
            }
            else if(result==NumberLocationConst.STATUS_USER_CANCEL){
                Message msg=new Message();
                msg.what = NumberLocationConst.ID_ALERT_DIALOG_SHOW_USER_CANCEL;
                mHandler.sendMessage(msg);
            }
            Log.d(TAG, " result : " + result);
        }

        public void updateProgress(int type,int progress) throws RemoteException {
            Message msg=new Message();
            msg.what = NumberLocationConst.ID_PROGRESS_DIALOG_UPDATE;
            Bundle data = new Bundle();
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_DATA, progress);
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE, type);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }

        public void dismissProgress(int type) throws RemoteException {
            Message msg=new Message();
            msg.what = NumberLocationConst.ID_PROGRESS_DIALOG_DISMISS;
            Bundle data = new Bundle();
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE, type);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }

        public void showProgress(int type) throws RemoteException {
            Message msg=new Message();
            msg.what = NumberLocationConst.ID_PROGRESS_DIALOG_SHOW;
            Bundle data = new Bundle();
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE, type);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }

        @Override
        public void setProgressMax(int type, int value) throws RemoteException {
            Message msg=new Message();
            msg.what = NumberLocationConst.ID_PROGRESS_DIALOG_SET_MAX;
            Bundle data = new Bundle();
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_TYPE, type);
            data.putInt(NumberLocationConst.BUNDLE_PROGRESS_MAX, value);
            msg.setData(data);
            mHandler.sendMessage(msg);

        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btnSearch:
        	 String number = actvNumber.getText().toString(); 
        	 if(TextUtils.isEmpty(number)){
        		 
        		String result=(String) this.getApplicationContext().getText(R.string.title_input_phone_nbumber);
        		 Builder resultDialog =constructEmptyInputDialog(result);
        		 resultDialog.show();
        	 } else {
              showSearchResult(NumberLocationConst.SEARCH_TYPE_LOCATION_BY_NUMBER);
        	 }
            break;
        case R.id.btnSearchCityCode:
        	String cityName = mActvNumberCityCode.getText().toString();
        	if(TextUtils.isEmpty(cityName)){     		 
        		String result=(String) this.getApplicationContext().getText(R.string.title_input_city_name);
        		Builder resultDialog =constructEmptyInputDialog(result);
        		resultDialog.show();
        	} else {
            showSearchResult(NumberLocationConst.SEARCH_TYPE_NUMBER_BY_CITY_LOCATION);
        	}
            break;
        case R.id.btnSearchCountryCode:
        	String countryName = mActvNumberCountryCode.getText().toString();
        	if(TextUtils.isEmpty(countryName)){
        		String result=(String) this.getApplicationContext().getText(R.string.title_input_country_name);
        		Builder resultDialog =constructEmptyInputDialog(result);
        		resultDialog.show();
        		
        	}else {
            showSearchResult(NumberLocationConst.SEARCH_TYPE_NUMBER_BY_COUNTRY_LOCATION);
        	}
            break;
        }
    }

    private void showSearchResult(int searchType) {

        if (isNumberLocationBindOK && mNumberLocationService != null) {
            String result = null;
            String query = null;
            try {
                if(searchType == NumberLocationConst.SEARCH_TYPE_LOCATION_BY_NUMBER){
                    String number = actvNumber.getText().toString();
                    query = "["+number + "]";
                    result = mNumberLocationService.getLocationByNumber(number);
                    if((result==null)||(result==""))
                        result = (String) this.getApplicationContext().getText(R.string.title_search_result_not_found);
                }
                else if(searchType == NumberLocationConst.SEARCH_TYPE_NUMBER_BY_CITY_LOCATION){
                    String name = mActvNumberCityCode.getText().toString();
                    query = "[" + name + "]";
                    result = mNumberLocationService.getNumberByCityLocation(name);
                    if((result==null)||(result==""))
                        result = (String) this.getApplicationContext().getText(R.string.title_search_result_not_found);
                    else
                        result = "0"+ result;
                }
                else if(searchType == NumberLocationConst.SEARCH_TYPE_NUMBER_BY_COUNTRY_LOCATION){
                    String name = mActvNumberCountryCode.getText().toString();
                    query = "[" + name + "]";
                    result = mNumberLocationService.getNumberByCountryLocation(name);
                    if((result==null)||(result==""))
                        result = (String) this.getApplicationContext().getText(R.string.title_search_result_not_found);
                    else
                        result = "00"+ result;
                }
                if (result != null) {
                    Builder resultDialog = constructQueryResultDialog(result, query);
                    resultDialog.show();
                }
            } catch (RemoteException e) {
                Log.v(TAG,
                        "Error in execute mNumberLocationService showSearchResult searchType = "
                                + searchType);
                e.printStackTrace();
            }
        }
    }

    public Builder constructQueryResultDialog(String result, String query) {
        View view = getLayoutInflater().inflate(R.layout.alert_result_dialog, null);
        TextView text_String_query = (TextView) view.findViewById(R.id.string_search_query);
        text_String_query.setText(query);
        TextView text_String_result = (TextView) view.findViewById(R.id.string_search_result);
        text_String_result.setText(result);

        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.title_search_result).setView(view).setPositiveButton(
                this.getApplicationContext().getText(R.string.button_close), null);

    }
    public Builder constructEmptyInputDialog(String result) {
        View view = getLayoutInflater().inflate(R.layout.alert_result_dialog, null);
        TextView text_String_query = (TextView) view.findViewById(R.id.string_search_query);
        text_String_query.setVisibility(View.GONE);
        TextView text_String_result = (TextView) view.findViewById(R.id.string_search_result);
        text_String_result.setGravity(Gravity.LEFT);
        text_String_result.setText(result);

        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.title_search_result).setView(view).setPositiveButton(
                this.getApplicationContext().getText(R.string.button_close), null);

    }

    public Builder constructUpdateResultDialog(String result) {
        View view = getLayoutInflater().inflate(R.layout.alert_update_dialog, null);
        TextView text_String_result = (TextView) view.findViewById(R.id.string_update_result);
        text_String_result.setText(result);

        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.title_update_result).setView(view).setPositiveButton(
                this.getApplicationContext().getText(R.string.button_close), null);

    }

    public void showWarningDialogAndDoUpgrade(String title, String warning) {
        final SharedPreferences prefs = getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE,
                Context.MODE_PRIVATE);
        boolean isSkipWarning = prefs.getBoolean(NumberLocationConst.KEY_SKIP_NETWORK_TRAFFIC_WARNING, false);
        if (!isSkipWarning) {
            View view = getLayoutInflater().inflate(R.layout.alert_warning_dialog, null);
            TextView text_String_result = (TextView) view.findViewById(R.id.string_update_result);
            text_String_result.setText(warning);
            CheckBox checkbox_next_time = (CheckBox) view.findViewById(R.id.CheckBoxNextTime);
            checkbox_next_time.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Editor editor = prefs.edit();
                    editor.putBoolean(NumberLocationConst.KEY_SKIP_NETWORK_TRAFFIC_WARNING, isChecked);
                    editor.commit();
                }
            });

            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(title).setView(view).setNegativeButton(
                    this.getApplicationContext().getText(R.string.btn_reject), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).setPositiveButton(this.getApplicationContext().getText(R.string.btn_accept),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            updateDatabase();
                        }
                    }).show();
        } else {
            updateDatabase();
        }
    }

    private void updateDatabase() {

        if (isNumberLocationBindOK && mNumberLocationService != null) {
            try {
                mNumberLocationService.updateNumberLocationDatabase();
            } catch (RemoteException e) {
                Log.v(TAG,
                        "Error in execute mNumberLocationService updateNumberLocationDatabase!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_update:
            initProgressDialog();
            showWarningDialogAndDoUpgrade(mContext.getResources().getString(R.string.title_network_cost_warning),
                    mContext.getResources().getString(R.string.string_network_cost_warning));
            break;
        case R.id.menu_setting:
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName(this, NumberLocationPreferenceActivity.class.getName());
            startActivity(intent);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    //fwr687 begin
    public  class CityListAdapter extends CursorAdapter implements Filterable {
        public CityListAdapter(Context context, Cursor c) {
            super(context, c);
            mContent = context.getContentResolver();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final TextView view = (TextView) inflater.inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
            view.setText(cursor.getString(1));
            view.setTextColor(context.getResources().getColor(android.R.color.black));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view).setText(cursor.getString(1));
        }

        @Override
        public String convertToString(Cursor cursor) {
            return cursor.getString(1);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if(getFilterQueryProvider() != null)
            {
                Log.e("CityListAdapter", "getFilterQueryProvider()!!!");
                return getFilterQueryProvider().runQuery(constraint);
            }
            StringBuilder buffer = null;
            String[] args = null;

            if(constraint != null)
            {
                Log.e("CityListAdapter", "|" + constraint.toString() + "|");
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(cityColumns.CITY);
                buffer.append(") GLOB ? and flag <> 3");

                args = new String[]{
                        "*"+constraint.toString().toUpperCase()+"*"
                };
            }
            //fwr687 begin 	IKSF-7238
            Cursor cursor = mContent.query(NumberLocationProviderConst.CITY_URI,new String[]{cityColumns.ID,cityColumns.CITY}, buffer == null? null : buffer.toString(),
                    args, cityColumns.CITY);
            if(cursor!=null){
                Message msg=new Message();
                msg.what = NumberLocationConst.ID_SEARCH_RESULT_NUM;
                Bundle data = new Bundle();
                data.putInt(NumberLocationConst.BUNDLE_DATA_RESULT_NUM, cursor.getCount());
                msg.setData(data);
                mHandler.sendMessage(msg);
            }
          //fwr687 end
            return cursor;
        }

        private ContentResolver mContent;
    }

    public static class CountryListAdapter extends CursorAdapter implements Filterable {
        public CountryListAdapter(Context context, Cursor c) {
            super(context, c);
            mContent = context.getContentResolver();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final TextView view = (TextView) inflater.inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
            view.setText(cursor.getString(1));
            view.setTextColor(context.getResources().getColor(android.R.color.black));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view).setText(cursor.getString(1));
        }

        @Override
        public String convertToString(Cursor cursor) {
            return cursor.getString(1);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if(getFilterQueryProvider() != null)
            {
                Log.e("CountryListAdapter", "getFilterQueryProvider()!!!");
                return getFilterQueryProvider().runQuery(constraint);
            }
            StringBuilder buffer = null;
            StringBuilder buffer_en = null;
            String[] args = null;

            if(constraint != null)
            {
                Log.e("CountryListAdapter", "|" + constraint.toString() + "|");
                buffer = new StringBuilder();
                buffer_en = new StringBuilder();
                buffer.append("UPPER(");
			    buffer_en.append("UPPER(");
                buffer.append(countryColumns.COUNTRY);
					      buffer_en.append(countryColumns.COUNTRY_EN);
                buffer.append(") GLOB ?");
                buffer_en.append(") GLOB ?");

                args = new String[]{
                        "*"+constraint.toString().toUpperCase() + "*"
                };
            }
			 Cursor []mCursors=new Cursor[2];
				mCursors[0] = mContent.query(NumberLocationProviderConst.COUNTRY_URI, new String[] { countryColumns.ID,
						countryColumns.COUNTRY }, buffer == null ? null : buffer.toString(), args,
						countryColumns.COUNTRY);
		 
				mCursors[1] = mContent.query(NumberLocationProviderConst.COUNTRY_URI, new String[] { countryColumns.ID,
						countryColumns.COUNTRY_EN }, buffer_en == null ? null : buffer_en.toString(), args,
						countryColumns.COUNTRY_EN);
				MergeCursor mergeCursor=new MergeCursor(mCursors);
				return mergeCursor;
        }

        private ContentResolver mContent;
    }

    private boolean NumberLocationDatabaseExists() {
        try {
            String DATABASE_NUBMER_LOCATION_PATH = getApplicationContext().
            getDatabasePath(DATABASE_NUBMER_LOCATION_FILENAME).getParent();

            String databaseFilename = DATABASE_NUBMER_LOCATION_PATH + "/"
                    + DATABASE_NUBMER_LOCATION_FILENAME;
            File dir = new File(DATABASE_NUBMER_LOCATION_PATH);
            if (!dir.exists())
                return false;
            else{
                if (!(new File(databaseFilename)).exists()){
                    return false;
                }else
                    return true;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
  //fwr687 end
}
