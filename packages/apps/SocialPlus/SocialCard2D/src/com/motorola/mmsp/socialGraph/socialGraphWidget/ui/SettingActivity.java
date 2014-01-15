package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Contact;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.WaitDialog;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.Intents;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel.ModelListener;

public class SettingActivity extends PreferenceActivity {

	private static final int REQUEST_CODE_MULTI_PICKER = 1;
	
	CheckBoxPreference automatic_setup_mode;
	Preference hide_contacts;
	Preference manage_contacts;
	//ListPreference history_settings;
	CheckBoxPreference stretch_contacts_pictures;
	Preference set_skin;
	//YesNoPreference widget_reset;
	OnSharedPreferenceChangeListener listener;
	
	private ModelListener mModelListener = new ModelListener() {
		public void onImageStretchChange(boolean bStretch) {
			Log.v("zc", "zc~~~~~mModelListener onImageStretchChange");
		}

		public void onContactChange(int pos, Contact contact, int size,
				boolean mode) {
			Log.v("zc", "zc~~~~~mModelListener onContactChange");
		}

		public void onAllContactChange(HashMap<Integer, Contact> contacts,
				HashMap<Integer, Integer> sizes, boolean mode) {
			Log.v("zc", "zc~~~~~mModelListener onAllContactChange");
		}

		public void onDataLoadFinish() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Log.v("zc", "zc~~~~~mModelListener onDataLoadFinish");
					refreshItems();
				}
			});
		}
	};
	
	private Handler mHandler = new Handler();
	
	private WaitDialog mWaitingDialog = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("zc", "zc~~~~~onCreate");
		addPreferencesFromResource(R.xml.setting);
		automatic_setup_mode = (CheckBoxPreference)findPreference("automatic_setup_mode");
		hide_contacts = (Preference)findPreference("hide_contacts");
		manage_contacts = (Preference)findPreference("manage_contacts");
		//history_settings = (ListPreference)findPreference("history_settings");
		stretch_contacts_pictures = (CheckBoxPreference)findPreference("stretch_contacts_pictures");
	
		set_skin = (Preference)findPreference("set_skin");
		//widget_reset = (YesNoPreference)findPreference("widget_reset");
		listener = new OnSharedPreferenceChangeListener() {			
			//@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (Setting.KEY_AUTOMATIC_SETUP_MODE.equals(key)) {
					RingLayoutModel.getDefaultRingLayoutModel(getBaseContext()).mDataReady = false;
				}
				refreshItems();
			}
		};
		
		mWaitingDialog = new WaitDialog(SettingActivity.this);
		mWaitingDialog.setMessage(getString(R.string.please_wait));
		mWaitingDialog.setCanceledOnTouchOutside(false);
		mWaitingDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				});

		try {
			if (mWaitingDialog != null) {
				mWaitingDialog.show();
			}
		} catch (Exception e) {
		}
		
		new Thread(new Runnable() {
			public void run() {
				RingLayoutModel.getDefaultRingLayoutModel(getBaseContext())
						.addListener(mModelListener);
				mHandler.post(new Runnable() {
					public void run() {
						try {
							if (mWaitingDialog != null) {
								mWaitingDialog.dismiss();
							}
						} catch (Exception e) {
						}
					}
				});
			}
		}).start();
		
		automatic_setup_mode.getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v("zc", "zc~~~~~onResume");
		initItems();		
	}

	@Override
	protected void onDestroy() {
		Log.v("zc", "zc~~~~~onDestroy");
		RingLayoutModel.getDefaultRingLayoutModel(this).removeListener(mModelListener);
		automatic_setup_mode.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("zc", "zc~~~~~onActivityResult");
		try {
			if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
				mWaitingDialog.dismiss();
				mWaitingDialog = null;
			}
		} catch (Exception e) {
		}
		
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_MULTI_PICKER) {
			HideContactsActivity.addHiddenContactsToSetting(this, data);
		}
	}
	
	private void initItems() {
		Log.v("zc", "zc~~~~~initItems");
		refreshItems();
		if (hide_contacts != null) {
			hide_contacts.setOnPreferenceClickListener(new OnPreferenceClickListener() {				
				//@Override
						public boolean onPreferenceClick(Preference preference) {
							new Thread(new Runnable() {
								public void run() {
									Log.v("zc", "zc~~~~~hide_contacts,onPreferenceClick");
									ArrayList<Integer> hiddenContacts = Setting
											.getInstance(SettingActivity.this)
											.getHideContacts();
									if (hiddenContacts != null) {
										ArrayList<Integer> validIds = Contact
												.getContactIdsExistInDb(
														SettingActivity.this,
														hiddenContacts);
										if (validIds == null
												|| validIds.size() <= 0) {
											Intent intent = new Intent(
													Intents.ACTIVITY_CONTACT_MULTI_PICKER);
											startActivityForResult(intent,
													REQUEST_CODE_MULTI_PICKER);
										} else {
											Intent intent = new Intent(
													Intents.ACTIVITY_HIDE_CONTACTS);
											startActivity(intent);
										}
									}

									mHandler.post(new Runnable() {
										public void run() {
											try {
												if (mWaitingDialog != null) {
													mWaitingDialog.dismiss();
												}
											} catch (Exception e) {
											}
										}
									});

								}
							}).start();

							mWaitingDialog = new WaitDialog(
									SettingActivity.this);
							mWaitingDialog.setMessage(getString(R.string.please_wait));
							mWaitingDialog.setCanceledOnTouchOutside(false);
							mWaitingDialog
									.setOnCancelListener(new DialogInterface.OnCancelListener() {

										@Override
										public void onCancel(
												DialogInterface dialog) {
											finish();
										}
									});
							try {
								if (mWaitingDialog != null) {
									mWaitingDialog.show();
								}
							} catch (Exception e) {
							}

							return true;
						}
					});
		}
		if (manage_contacts != null) {
			manage_contacts.setOnPreferenceClickListener(new OnPreferenceClickListener() {				
				//@Override
				public boolean onPreferenceClick(Preference preference) {
					Log.v("zc", "zc~~~~~manage_contacts,onPreferenceClick");
					Intent intent = new Intent(Intents.ACTIVITY_CONTACTS_MANAGE);
					startActivity(intent);
					return true;
				}
			});
		}		
		
		if (set_skin != null) {
			set_skin.setOnPreferenceClickListener(new OnPreferenceClickListener() {				
				//@Override
				public boolean onPreferenceClick(Preference preference) {
					Log.v("zc", "zc~~~~~set_skin,onPreferenceClick");
					Intent intent = new Intent(Intents.ACTIVITY_SET_SKIN);
					startActivity(intent);
					return true;
				}
			});			
		}

	}
	


	private void refreshItems() {
		Log.v("zc", "zc~~~~~refreshItems");
		if (automatic_setup_mode != null) {
			boolean bAutoMode = automatic_setup_mode.getSharedPreferences()
					.getBoolean(automatic_setup_mode.getKey(), false);
			Log.v("zc", "zc~~~~~refreshItems,bAutoMode,"+bAutoMode);
			automatic_setup_mode.setEnabled(RingLayoutModel
					.getDefaultRingLayoutModel(this).mDataReady);
			Log.v("zc", "zc~~~~~refreshItems,mDataReady,"+RingLayoutModel
					.getDefaultRingLayoutModel(this).mDataReady);
			automatic_setup_mode.setChecked(bAutoMode);
			
			if (bAutoMode) {
				if (hide_contacts != null) {
					hide_contacts.setEnabled(RingLayoutModel
							.getDefaultRingLayoutModel(this).mDataReady);
					Log.v("zc", "zc~~~~~hide_contacts,mDataReady,"+RingLayoutModel
							.getDefaultRingLayoutModel(this).mDataReady);
				}
				if (manage_contacts != null) {
					manage_contacts.setEnabled(false);
				}
				//Setting.notifyContactUpdate(this);
			} else {
				if (hide_contacts != null) {
					hide_contacts.setEnabled(false);
				}
				if (manage_contacts != null) {
					manage_contacts.setEnabled(RingLayoutModel
							.getDefaultRingLayoutModel(this).mDataReady);
					Log.v("zc", "zc~~~~~manage_contacts,mDataReady,"+RingLayoutModel
							.getDefaultRingLayoutModel(this).mDataReady);
				}
			}
		}

		if (stretch_contacts_pictures != null) {
			boolean bOn = stretch_contacts_pictures.getSharedPreferences()
					.getBoolean(stretch_contacts_pictures.getKey(), false);
			Log.v("zc", "zc~~~~~stretch_contacts_pictures,bOn,"+bOn);
			stretch_contacts_pictures.setChecked(bOn);
			if (bOn) {
				if (set_skin != null) {
					set_skin.setEnabled(false);
				}
			} else {
				if (set_skin != null) {
					set_skin.setEnabled(true);
				}
			}
		}
		
	}
}
