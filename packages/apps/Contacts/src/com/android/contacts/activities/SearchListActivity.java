package com.android.contacts.activities;

import android.app.ListActivity;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;  
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;  
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;  
import android.widget.TextView.OnEditorActionListener;
import android.widget.ImageButton;
import android.widget.ListView;

import com.android.contacts.R;

public class SearchListActivity extends ListActivity {
    private static final String TAG = "SearchListActivity";
    private EditText    mKeyword;
    private ImageButton mButton;
    private InputMethodManager mIM;
    
    private OnClickListener mListener = new OnClickListener() {
        public void onClick(View v) {
            mKeyword.setText("");
        }       
    };
    
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        
        mIM = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mKeyword = (EditText)findViewById(R.id.search);
        if (null == mKeyword) {
            mButton = null;
            Log.i(TAG, "Your content does not have a EditText whose id attribute is "
                    + "'R.id.search', so it will be a normal ListActivity");
        } else {
            mKeyword.setOnEditorActionListener(new OnEditorActionListener() {  
                @Override  
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {  
                    //Log.v(TAG, "onEditorAction(), v="+v+", actionId="+actionId);
                    switch(actionId){  
                          case EditorInfo.IME_ACTION_DONE: 
                               //Log.v(TAG, "IME_ACTION_DONE");
                               break;
                          case EditorInfo.IME_ACTION_SEARCH: 
                               //Log.v(TAG, "IME_ACTION_SEARCH");
                               if (mIM != null ) {
                                   mIM.hideSoftInputFromWindow(mKeyword.getWindowToken(), 0);
                               }
                               break;
                          default:
                               break;
                    }
                    return true;  
                }  
            });  

            SearchListView list = (SearchListView)getListView();
            list.attachSearchBar(mKeyword);
            mButton = (ImageButton)findViewById(R.id.search_go_btn);
        }
        
        if (null == mButton) {
            Log.i(TAG, "Your content does not have a promting ImageButton " +
                    "whose id is" + "'R.id.search_go_btn'");
        } else {
            mButton.setOnClickListener(mListener);
        }
    }
}
