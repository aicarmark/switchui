package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RecipientsMultiplePickerAdapter;

public class MultipleContactsPickerActivity extends Activity {
	private static final String TAG = "SocialGraphWidget";
	private ListView list;
	private AutoCompleteTextView mSearchEditText;
	
	RecipientsMultiplePickerAdapter adapter = null;

	private ArrayList<Integer> mHiddedContactID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.multiple_contacts_picker_activity);
		
		mHiddedContactID = getIntent().getIntegerArrayListExtra(
				"com.motorola.mmsp.socialGraph.HIDDEND_CONTACTS");		

		creatView(null);
		
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "MultipleContactsPickerActivity onStart");
		super.onStart();
		setAdapter();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.multiple_contacts_picker_activity);
		creatView(mSearchEditText.getText().toString());
		
		setAdapterForCfgChanged();
		
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
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Log.d(TAG, "setChecked");
				CheckBox check = (CheckBox) arg1.findViewById(R.id.checkbox);
				check.setChecked(!check.isChecked());
				Cursor c = adapter.getCursor();
				if ((c != null) && !c.isAfterLast()) {
					final int contactId = c
							.getInt(RecipientsMultiplePickerAdapter.RAW_CONTACT_ID_INDEX);
					adapter.setMarked(contactId, check.isChecked());					
				}
			}
		});
		
		final View ok = findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				ArrayList<Integer> hiddenContacts = adapter.getHiddenContacts();
				Intent returnIntent = new Intent(MultipleContactsPickerActivity.this, HideContactsActivity.class);
				returnIntent.putIntegerArrayListExtra("com.motorola.mmsp.socialGraph.socialGraphWidget.contactidlist", hiddenContacts);
				MultipleContactsPickerActivity.this.setResult(Activity.RESULT_OK, returnIntent);
				finish();
			}
		});

		final View cancel = findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				finish();
			}
		});

	}	
	
	private void setAdapter() {
		if (adapter == null) {
			adapter = new RecipientsMultiplePickerAdapter(this,
					mHiddedContactID);
		} else {
			ArrayList<Integer> checkedContacts = adapter.getHiddenContacts();
			adapter = new RecipientsMultiplePickerAdapter(this,
					mHiddedContactID, checkedContacts);
		}
		setAdapterForCfgChanged();
	}
	
	private void setAdapterForCfgChanged() {
		mSearchEditText.setAdapter(adapter);
		String filterText = mSearchEditText.getEditableText().toString();
		if ("".equals(filterText)) {
			filterText = null;
		}
		adapter.getFilter().filter(filterText);
		list.setAdapter(adapter);
	}
	
	@Override
	protected void onDestroy() {
		if (adapter != null) {
			adapter.changeCursor(null);
		}
		super.onDestroy();
	}
}
