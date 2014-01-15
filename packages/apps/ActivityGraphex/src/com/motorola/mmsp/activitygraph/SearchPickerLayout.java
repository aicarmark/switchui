package com.motorola.mmsp.activitygraph;

import android.widget.EditText;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Filter;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;


public class SearchPickerLayout extends LinearLayout
implements TextWatcher,View.OnClickListener {
    
    // private static Context mContext;
    private Button mButtonOk;
    private Button mButtonCancel; 
    private ListView mListView;
    private EditText mSearchEditText;  
    private boolean mMultiCheck;
    private boolean mButtonExist;
    private SearchPickerAdapter mAdapter;
    private Callbacks mCallbacks = null;
    private static final String TAG = "SearchPickerLayout";
    
    public interface Callbacks {
        
        public void buttonOkClick();
        public void buttonCancelClick();
        public void itemClick(int position);
        
    }
    
    public SearchPickerLayout(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        //     mContext = context;
        Log.d(TAG, "SearchPickerLayout(Context context)");
    }
    
    public SearchPickerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }	
    
    public void setCallbacks (Callbacks callbacks) {
        mCallbacks = callbacks;
    }
    
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
    
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
    
    public void afterTextChanged(Editable s) {
        onSearchTextChanged();
    }
    /**
     * Performs filtering of the list based on the search query entered in the
     * search text edit.
     */
    protected void onSearchTextChanged() {
        
        Filter filter = mAdapter.getFilter();
        filter.filter(getTextFilter());
    }
    
    protected void finalize() throws Throwable {
        try {
            Log.d("Layout", "finalize()");
            //mServiceListener.clear();
            //close();
            //bConnected = false;
        } finally {
            super.finalize();
        }
    }
    private String getTextFilter() {
        if (mSearchEditText != null) {
            return mSearchEditText.getText().toString();
        }
        return null;
    }
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == mButtonOk)
            mCallbacks.buttonOkClick();
        else if (v == mButtonCancel)
            mCallbacks.buttonCancelClick();
    }
    
    public void setMultiCheck(boolean multicheck){
        mMultiCheck = multicheck;
    }
    public void setButtonExist(boolean buttonexist){
        mButtonExist = buttonexist;
        mButtonOk = (Button)findViewById(R.id.btnOk);
        mButtonCancel = (Button)findViewById(R.id.btnCancel);
        if(!mButtonExist) {
            mButtonOk.setVisibility(View.GONE);
            mButtonCancel.setVisibility(View.GONE);
        }else {
            mButtonOk.setOnClickListener(this);
            mButtonCancel.setOnClickListener(this);
        }
    }
    
    public void setAdapter(SearchPickerAdapter searchpickeradapter){
        mAdapter = searchpickeradapter;
        
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {  
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {  
                mCallbacks.itemClick(position);
            }
        });
        mSearchEditText = (EditText)findViewById(R.id.searchEdit);
        mSearchEditText.addTextChangedListener(this);
    }
    
    
}