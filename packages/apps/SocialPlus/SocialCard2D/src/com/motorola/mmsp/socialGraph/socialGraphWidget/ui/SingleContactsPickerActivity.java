package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RecipientsSinglePickerAdapter;

public class SingleContactsPickerActivity extends Activity {
	private static final String TAG = "SocialGraphWidget";
	private ListView list;
	private AutoCompleteTextView mSearchEditText;
	RecipientsSinglePickerAdapter adapter;
	
	private ArrayList<Integer> mSelectedContactIDs;
	private static final String KEY_CHOOSEN_CONTACTS = "choosenContacts";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_contacts_picker_activity);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		mSelectedContactIDs = getIntent().getIntegerArrayListExtra(KEY_CHOOSEN_CONTACTS);
		creatView(null);
		
		adapter = new RecipientsSinglePickerAdapter(this, mSelectedContactIDs);
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "SingleContactsPickerActivity onStart");
		super.onStart();
		setAdapter();		
	}

	@Override
	protected void onDestroy() {
		if (adapter != null) {
			adapter.changeCursor(null);
		}
		super.onDestroy();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.single_contacts_picker_activity);
		creatView(mSearchEditText.getText().toString());
		setAdapter();
		
		Configuration config = getResources().getConfiguration();
		boolean isKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
				
		if (isKeyboardOpen) {
			Log.d(TAG, "key board is on");
			mSearchEditText.setFocusableInTouchMode(true);
			mSearchEditText.requestFocus();
		} else {
			Log.d(TAG, "key board is off");
			mSearchEditText.clearFocus();
		}
	}
	
	private void creatView(CharSequence text) {
		mSearchEditText = (AutoCompleteTextView) findViewById(R.id.text);
		mSearchEditText
				.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
						312) });
		mSearchEditText.setImeOptions(EditorInfo.IME_ACTION_NONE);
		
		if (text != null) {
			mSearchEditText.setText(text);
		}
		
		list = (ListView) findViewById(R.id.list);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				Uri uri = null;
				if (id > 0) {
					uri = getSelectedUri(position);
				}
				Intent returnIntent = new Intent(SingleContactsPickerActivity.this, ContactsManageActivity.class);
				setResult(RESULT_OK, returnIntent.setData(uri));
				finish();

			}
		});
	}
	
	private void setAdapter() {		
		mSearchEditText.setAdapter(adapter);
		String filterText = mSearchEditText.getEditableText().toString();
		if ("".equals(filterText)) {
			filterText = null;
		}
		adapter.getFilter().filter(filterText);
		list.setAdapter(adapter);
	}

	/**
	 * Build the {@link Uri} for the given {@link ListView} position, which can
	 * be used as result when in {@link #MODE_MASK_PICKER} mode.
	 */
	private Uri getSelectedUri(int position) {
		if (position == ListView.INVALID_POSITION) {
			throw new IllegalArgumentException("Position not in list bounds");
		}
		final long id = adapter.getItemId(position);
		return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
	}
}
