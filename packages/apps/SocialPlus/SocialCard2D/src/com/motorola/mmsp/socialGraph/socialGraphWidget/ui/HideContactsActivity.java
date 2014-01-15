package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.WaitDialog;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RecipientsAdapter;

public class HideContactsActivity extends Activity {
	private static final String TAG = "SocialGraphWidget";
	
	private ListView list;
	private AutoCompleteTextView mSearchEditText;
	
	RecipientsAdapter adapter = null;
	
	private static final int MENU_PICK_CONTACTS    = 1;
	
	private WaitDialog waitingDialog = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.hide_contacts_activity);
		creatView(null);
		
	}	

	@Override
	protected void onStart() {
		super.onStart();

		creatWaitDialog();
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
		setContentView(R.layout.hide_contacts_activity);
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
		mSearchEditText = (AutoCompleteTextView)findViewById(R.id.text);
		mSearchEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(312) });
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
				Cursor c =null;
				 
				 if(adapter!=null){
					c = adapter.getCursor();
				 }
				if ((c != null) && !c.isAfterLast()) {
					final int contactId = c
							.getInt(RecipientsAdapter.RAW_CONTACT_ID_INDEX);
					if (adapter != null) {
						adapter.setMarked(contactId, check.isChecked());
					}					
				}
			}
		});
		
		final TextView ok = (TextView) findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {			
			//@Override
			public void onClick(View v) {
				Log.d(TAG, "HideContactsActivity OK clicked");
				ArrayList<Integer> hiddenContacts = adapter.getHiddenContacts();
				Setting.getInstance(HideContactsActivity.this).setHideContacts(hiddenContacts);
				finish();
			}
		});
		
		final TextView cancel = (TextView) findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {			
			//@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		final View add = findViewById(R.id.add);
		add.setOnClickListener(new OnClickListener() {			
			//@Override
			public void onClick(View v) {
				Intent intent = new Intent("com.motorola.mmsp.socialGraph.MULTIPLE_CONTACTS_PICKER", null);
				ArrayList<Integer> hiddenContacts = null;
				if (Setting.getInstance(getBaseContext()) != null) {
					hiddenContacts = Setting.getInstance(getBaseContext())
							.getHideContacts();
				}
				intent.putIntegerArrayListExtra("com.motorola.mmsp.socialGraph.HIDDEND_CONTACTS", hiddenContacts);
				startActivityForResult(intent, MENU_PICK_CONTACTS);
			}
		});
	}
	
	private void setAdapter() {
		if (adapter == null) {
			adapter = new RecipientsAdapter(this, waitingDialog);
		} else {
			ArrayList<Integer> oldUnCheckedContacts = null;
			oldUnCheckedContacts = adapter.getUnCheckedContacts();
			adapter = new RecipientsAdapter(this, oldUnCheckedContacts, waitingDialog);
		}
		
		mSearchEditText.setAdapter(adapter);
		String filterText = mSearchEditText.getEditableText().toString();
		if ("".equals(filterText)) {
			filterText = null;
		}
		adapter.getFilter().filter(filterText);
		list.setAdapter(adapter);
	}
	
	private void creatWaitDialog() {
        waitingDialog = new WaitDialog(this);
        waitingDialog.setMessage(getString(R.string.please_wait));
        waitingDialog.setCanceledOnTouchOutside(false);
        waitingDialog
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    	Log.d(TAG, "HideContactsActivity waiting dialog cancel");
                        finish();
                    }
                });
		try {
			if (waitingDialog != null) {
				waitingDialog.show();
			}
		} catch (Exception e) {
		}
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "HideContactsActivity onActivityResult");
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		
		if (requestCode == MENU_PICK_CONTACTS) {
			addHiddenContactsToSetting(this, data);			
		}
		
	}
	
	public static void addHiddenContactsToSetting(Context context, Intent data) {
		ArrayList<Integer> markContactID = data.getIntegerArrayListExtra("com.motorola.mmsp.socialGraph.socialGraphWidget.contactidlist");
		Log.d(TAG, "HideContactsActivity:pickContactsFromPb contacts = " + markContactID.toString());
		
		String where = String.format("%s in (%s)", 
				RawContacts.CONTACT_ID, 
				markContactID.toString().replace("[", "").replace("]", ""));
		Log.d(TAG, "HideContactsActivity:pickContactsFromPb where = " + where);
		
		Cursor c = null;
		try {
			c = context.getContentResolver().query(RawContacts.CONTENT_URI,
							new String[] { RawContacts.CONTACT_ID,
									RawContacts._ID }, where, null, null);

			ArrayList<Integer> hiddenContactID = new ArrayList<Integer>();
			if (c != null) {
				Log.d(TAG, "HideContactsActivity:pickContactsFromPb count "
						+ c.getCount());
				c.moveToFirst();
				for (int i = 0; i < c.getCount(); i++) {
					Log.d(TAG,
							"HideContactsActivity:pickContactsFromPb for "
									+ i);
					Integer contactId = c.getInt(0);
					if (!hiddenContactID.contains(contactId)) {
						hiddenContactID.add(contactId);
					}
					c.moveToNext();
				}
			}

			Log.d(TAG, "HideContactsActivity:pickContactsFromPb rawContacts = "	+ hiddenContactID.toString());
			Setting.getInstance(context).addHideContacts(hiddenContactID);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
}
