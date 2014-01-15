package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.MessageUtils;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphProvider;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Contact;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.WaitDialog;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel.ModelListener;
import com.motorola.mmsp.socialGraph.socialGraphWidget.ui.RingLayout.OnItemClickListener;

public class ContactsManageActivity extends Activity{
	//private static final String TAG = "SocialGraph";
	private RingLayout ring;
	private RingLayoutModel model;
	private int pos = -1;
	
	private static final String KEY_CHOOSEN_CONTACTS = "choosenContacts";
	private static final String KEY_CHOOSEN_POSITIONS = "choosenPositions";
	private static final String KEY_MEM_POS = "possave";
	
	private static final int REQUEST_CODE_PICK_CONTACT = 1;
	
	private int fromWelcome = 0;
	
	
	private ModelListener mModelListener = new ModelListener() {
		public void onImageStretchChange(boolean bStretch) {

		}

		public void onContactChange(int pos, Contact contact, int size,
				boolean mode) {

		}

		public void onAllContactChange(HashMap<Integer, Contact> contacts,
				HashMap<Integer, Integer> sizes, boolean mode) {

		}

		public void onDataLoadFinish() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {					
					model.cloneFrom(RingLayoutModel.getDefaultRingLayoutModel(getBaseContext()));
					try {
						if (waitingDialog != null) {
							waitingDialog.dismiss();
						}
					} catch (Exception e) {
					}
				}
			});
		}
	};
	
	private Handler mHandler = new Handler();
	
	private WaitDialog waitingDialog = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_manage);
		Log.v("ContactsManageActivity", "zc~~~~~onCreate");
		Intent intent = getIntent();
	    fromWelcome = intent.getIntExtra("welcome", 0); 
		
		ArrayList<Integer> positions = new ArrayList<Integer>();
		ArrayList<Integer> choosenContacts = new ArrayList<Integer>();
		if (savedInstanceState != null) {
			choosenContacts = savedInstanceState.getIntegerArrayList(KEY_CHOOSEN_CONTACTS);
			positions = savedInstanceState.getIntegerArrayList(KEY_CHOOSEN_POSITIONS);
			
			pos = savedInstanceState.getInt(KEY_MEM_POS, -1);
		}
		
		RingLayoutModel.getDefaultRingLayoutModel(this).addListener(mModelListener);
		
		if (!RingLayoutModel.getDefaultRingLayoutModel(this).mDataReady) {
			waitingDialog = new WaitDialog(this);
			waitingDialog.setMessage(getString(R.string.please_wait));
			waitingDialog.setCanceledOnTouchOutside(false);
			waitingDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							Log.v("ContactsManageActivity", "zc~~~~~waitingDialog onCancel");
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

		model = new RingLayoutModel(this);
		model.cloneFrom(RingLayoutModel.getDefaultRingLayoutModel(this));
		
		for (int i = 0; i < choosenContacts.size(); i++) {
			int pos = positions.get(i);
     		Contact contact = new Contact(this, choosenContacts.get(i));
			model.setContact(pos, contact, true);
		}
		
		
		ring = (RingLayout)findViewById(R.id.ringlayout);
		ring.setModel(model);
		ring.setOnItemClickListener(new OnItemClickListener() {	
			//@Override
			public void onItemClick(View v, View item, int pos) {
				ContactsManageActivity.this.pos = pos;
				Intent intent = Contact.getPickContactIntent();
				Log.v("ContactsManageActivity", "zc~~~~~ring OnItemClickListener");
				Set<Integer> posSet = null;
				ArrayList<Integer> choosenContacts = new ArrayList<Integer>();
				posSet = model.contacts.keySet();
				int i = 0;
				for (Integer entry : posSet) {
					choosenContacts.add(i, model.contacts.get(entry).getPerson());
				}
				intent.putIntegerArrayListExtra(KEY_CHOOSEN_CONTACTS, choosenContacts);
				
					startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
			}
		});
		
		TextView textView = (TextView)findViewById(R.id.text);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());

		final View ok = findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {
				onOk();
			}
		});
		
		final View cancel = findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {			
			//@Override
			public void onClick(View v) {
				onCancel();
			}
		});
	}
	
	private void onOk() {
		Log.v("ContactsManageActivity", "zc~~~~~onOk");
		RingLayoutModel.setDefaultRingLayoutModel(this, model);
		
		//new add for save contact info to socialcontacts
		ContentValues Contactvalues = new ContentValues();
		MessageUtils.getContactInfoFromPhoneBook(this, Contactvalues);
		MessageUtils.updateContactsInfoToDB(SocialGraphProvider.getDatabase(), Contactvalues.valueSet(), "1");
		
		if (fromWelcome == 1) {
			WelcomeAutoActivity.writeOutOfBoxToDb(getBaseContext());
			Intent intent = new Intent(
					WelcomeAutoActivity.BROADCAST_WIDGET_OUT_OF_BOX);
			sendBroadcast(intent);
			
			Activity activity = WelcomeActivity.getActivity();
			if (activity != null) {
				activity.finish();
			}
		}
		
		finish();
	}
	
	private void onCancel() {
		Log.v("ContactsManageActivity", "zc~~~~~onCancel");
		/*if (fromWelcome == 1) {
			Intent intent = new Intent();
			intent.setClass(ContactsManageActivity.this, WelcomeActivity.class);
			startActivity(intent);
		}*/
		finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.v("ContactsManageActivity", "zc~~~~~onSaveInstanceState");
		Set<Integer> posSet = null;
		ArrayList<Integer> choosenContacts = new ArrayList<Integer>();
		ArrayList<Integer> positions = new ArrayList<Integer>();

		posSet = model.contacts.keySet();
		int i = 0;
		for (Integer entry : posSet) {
			positions.add(i, entry);
			choosenContacts.add(i, model.contacts.get(entry).getPerson());
		}

		outState.putIntegerArrayList(KEY_CHOOSEN_CONTACTS, choosenContacts);
		outState.putIntegerArrayList(KEY_CHOOSEN_POSITIONS, positions);
		
		outState.putInt(KEY_MEM_POS, pos);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.v("ContactsManageActivity", "zc~~~~~onStart");
		if (Setting.getInstance(this).isAutoMode()) {
			finish();
		}
	}	

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.v("ContactsManageActivity", "zc~~~~~onResume");
		super.onResume();
		model.reLoadContacts(this);
	}	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.v("ContactsManageActivity", "zc~~~~~onDestroy");
		super.onDestroy();
		ring.clearModel();
		RingLayoutModel.getDefaultRingLayoutModel(this).removeListener(mModelListener);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v("ContactsManageActivity", "zc~~~~~onActivityResult"+resultCode);
		if (resultCode == RESULT_OK && data != null) {
			switch (requestCode) {
			case REQUEST_CODE_PICK_CONTACT:
				if (pos != -1) {
					final Uri uriRet = data.getData();
					Contact contact = new Contact(this, uriRet);
					model.setContact(pos, contact, true);
				}
				break;

			default:
				break;
			}
		}
		
		pos = -1;
	}
}
