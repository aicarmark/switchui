package com.android.contacts.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.content.ContentValues;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.Intents.Insert;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import com.motorola.android.telephony.PhoneModeManager;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
// a23237 begin
import android.app.Dialog;
import android.content.SharedPreferences;
// a23237 end
import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;
import static android.telephony.TelephonyManager.PHONE_TYPE_NONE;
import android.os.SystemProperties;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;

public class TransDialPad extends Activity {
    /** Called when the activity is first created. */
    private static final String TAG = "TransDialPad";
    private static final boolean DBG = true;

    private String phoneNumber;
        private String networkType;

    private ImageButton bothOfCDMAButton;
    private ImageButton bothOfGSMButton;

    private boolean isGSMEnabled = false;
    private boolean isCDMAEnabled = false;

    private int mDefaultPhoneType = PHONE_TYPE_NONE;
    private int mSecondaryPhoneType = PHONE_TYPE_NONE;

    private String mPhoneType = null;
        private AlertDialog mUseDifferentCard;
    private boolean isIPNotSetDialogShown = false;

        // a23237 begin
        private boolean isIpCall = false;
        private static final int NO_CDMA_IP_SET_DIALOG = 100;
        private static final int NO_GSM_IP_SET_DIALOG = 200;
        private String ip_cdma = "";
        private String ip_gsm = "";
        private SharedPreferences shareddata;
        // a23237 end
        private boolean isIntRoamCallBackCall = false;
        private boolean isIntRoamCall = false;
        private boolean isDefaultRoaming = false;
        private boolean isSecondaryRoaming = false;
	 Context mDialogContext = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	 // Wrap our context to inflate list items using correct theme
	 mDialogContext = new ContextThemeWrapper(this,
			                          getResources().getBoolean(R.bool.contacts_dark_ui)
			                          ? com.android.internal.R.style.Theme_Holo_Dialog_Alert
			                          : com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert
			                         );
		
        Intent in = getIntent();
        phoneNumber = in.getStringExtra("phoneNumber");
        networkType = in.getStringExtra("network");

        // a23237 begin
        isIpCall = in.getBooleanExtra("isIpCall", false); // default is false
        Log.v(TAG, "isIpCall = " + isIpCall);
        // a23237 end
        isIntRoamCallBackCall = in.getBooleanExtra(ContactsUtils.IntRoamCallBackCall, false); // default is false
        Log.v(TAG, "isIntRoamCallBackCall = " + isIntRoamCallBackCall);
        isIntRoamCall = in.getBooleanExtra(ContactsUtils.IntRoamCall, false); // default is false
        Log.v(TAG, "isIntRoamCall = " + isIntRoamCall);
        if (isIntRoamCallBackCall || isIntRoamCall) {
            isIpCall = false; // Should not go here. Just for safety
            Log.e(TAG, "Wrong extra info!");
        }

    if (DBG) Log.d(TAG, "Receive phone number equal "+phoneNumber);

    if (phoneNumber != null) {
        phoneNumber = convertPauseWaitToCommaSemicolon(phoneNumber);
        phoneNumber = PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber);
        phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
    }

    if (DBG) Log.d(TAG, "Receive phone number equal "+phoneNumber);

        boolean isCarDockMode = ContactsUtils.isCarDockMode(getIntent());
        if (!isCarDockMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        setContentView(R.layout.translucent_background);

        bothOfCDMAButton = (ImageButton) findViewById(R.id.bothOfCDMA);
        bothOfGSMButton = (ImageButton) findViewById(R.id.bothOfGSM);
        if (!PhoneModeManager.isDmds()) {
            bothOfCDMAButton.setVisibility(View.GONE);
            bothOfGSMButton.setVisibility(View.GONE);
        }

        bothOfCDMAButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                callByCDMA();
            }
        });

        bothOfGSMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                callByGSM();
        }
    });
    }

    @Override
    protected void onResume() {
    if (DBG) Log.d(TAG,"onResume()...");
    super.onResume();
    updateScreen();
    }

    @Override
    protected void onPause() {
    if (DBG) Log.d(TAG,"onPause()...");
    super.onPause();
    if (mUseDifferentCard != null) {
        mUseDifferentCard.dismiss();
        mUseDifferentCard = null;
    }
    }

    @Override
    protected void onNewIntent(Intent intent) {
    if (DBG) Log.d(TAG,"onNewIntent: intent=" + intent);
    Intent in = intent;
    phoneNumber = in.getStringExtra("phoneNumber");
    networkType = in.getStringExtra("network");
    }

    private void updateScreen(){

    final boolean emergencyNumber =
        (phoneNumber != null) && PhoneNumberUtils.isEmergencyNumber(phoneNumber);

    setPhoneModeInfo();

    // If emergencyNumber and not all cards are available, direct call it.
    if (emergencyNumber){
        if (!(isGSMEnabled&&isCDMAEnabled)){
        callForEmergency();
        return;
        }
    }

    setButtonState();
    /* show warning dialog if directly dialing different from before card*/
    if (isGSMEnabled&&isCDMAEnabled){
        setButtonState();
            if (isIpCall) {
                if (isDefaultRoaming && !isSecondaryRoaming) {
                    // For IP call, if Defaultmgr is roaming, IP call shall be made via SecondaryMgr
                    if (mDefaultPhoneType == PHONE_TYPE_CDMA) {
                        callByGSM();
                    } else {
                        callByCDMA();
                    }
                } else if (!isDefaultRoaming && isSecondaryRoaming) {
                    // For IP call, if SecondaryMgr is roaming, IP call shall be made via Defaultmgr
                    if (mSecondaryPhoneType == PHONE_TYPE_CDMA) {
                        callByGSM();
                    } else {
                        callByCDMA();
                    }
                }
            } else if (isIntRoamCallBackCall) {
                //Only GSM support int roaming call back call
                callByGSM();
            } else if (isIntRoamCall) {
                if (isDefaultRoaming && !isSecondaryRoaming) {
                    if (mDefaultPhoneType == PHONE_TYPE_CDMA) {
                        callByCDMA();
                    } else {
                        callByGSM();
                    }
                } else if (!isDefaultRoaming && isSecondaryRoaming) {
                    if (mSecondaryPhoneType == PHONE_TYPE_CDMA) {
                        callByCDMA();
                    } else {
                        callByGSM();
                    }
                }
            }
    }else if (isGSMEnabled||isCDMAEnabled){
        if (isGSMEnabled){
        if ((mPhoneType != null)&&mPhoneType.equalsIgnoreCase("CDMA")){
            showCardDifferentDialog("GSM");
        }else if ((networkType != null)&&networkType.equalsIgnoreCase("CDMA")){
            callByGSM();
        }else{
            callByGSM();
        }
        return;
        }else if(isCDMAEnabled){
        if ((mPhoneType != null)&&mPhoneType.equalsIgnoreCase("GSM")){
            showCardDifferentDialog("CDMA");
        } else if ((networkType != null)&&networkType.equalsIgnoreCase("GSM")){
            callByCDMA();
        } else{
            callByCDMA();
        }
        return;
        }
    }else{
        callByNoIndicator();
        //finish();
        return;
    }
    }

    private void setPhoneModeInfo() {
        isCDMAEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_CDMA);
        isGSMEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_GSM);
        isDefaultRoaming = false;
        isSecondaryRoaming = false;

        TelephonyManager defaultMgr = TelephonyManager.getDefault();
        mDefaultPhoneType = defaultMgr.getPhoneType();
        isDefaultRoaming = defaultMgr.isNetworkRoaming();
        TelephonyManager secondMgr = null;
        if (PhoneModeManager.isDmds()) {
            /* to-pass-build, Xinyu Liu/dcjf34 */
            secondMgr = null;//TelephonyManager.getDefault(false);
            if (secondMgr != null) {
                isSecondaryRoaming = secondMgr.isNetworkRoaming();
                mSecondaryPhoneType = secondMgr.getPhoneType();
            }
        }
    }


    public static String convertPauseWaitToCommaSemicolon(String input) {
    if (input == null) {
        return input;
    }
    int len = input.length();
    if (len == 0) {
        return input;
    }

    char[] out = input.toCharArray();

    for (int i = 0; i < len; i++) {
        char c = out[i];
        if ((c == 'P') || (c == 'p')){
        out[i] = ',';
        }else if ((c == 'W') || (c == 'w')){
        out[i] = ';';
        }
    }
    return new String(out);
    }


    /* check the call log and find the related code*/
	private void setButtonState() {
		// The follow part is to query calllog and find related card.
		// empty input
		if (TextUtils.isEmpty(phoneNumber))
			return;
		bothOfCDMAButton.setSelected(false);
		bothOfGSMButton.setSelected(false);

		final Uri uri = Uri.withAppendedPath(Calls.CONTENT_FILTER_URI,
				phoneNumber);
		final Cursor cursor = getContentResolver().query(uri, null, null, null,
				CallLog.Calls.DEFAULT_SORT_ORDER);
		if (cursor == null) {
			if (DBG)
				Log.d(TAG, "cursor equal null");
			if (networkType != null) {
				if (networkType.equalsIgnoreCase("CDMA")) {
					mPhoneType = "CDMA";
					bothOfCDMAButton.setSelected(true);
				} else if (networkType.equalsIgnoreCase("GSM")) {
					mPhoneType = "GSM";
					bothOfGSMButton.setSelected(true);
				}
			}
			// Set button disabled or enabled according to phone's state.
		} else {
			if (DBG)
				Log.d(TAG, "cursor not equal null");

			int callType = 3;
			if (cursor.getCount() >= 1) {
				if (DBG)
					Log.d(TAG, "cursor getCount = " + cursor.getCount());
				cursor.moveToNext();
				int column = cursor.getColumnIndex(ContactsUtils.CallLog_NETWORK);
				if (column != -1) callType = cursor.getInt(column);
				if (DBG)
					Log.d(TAG, "callType equals " + callType);
			}
			cursor.close();
			// Just like above, we can filter it by CallLog.Calls.CALLTYPE

			if (callType == TelephonyManager.PHONE_TYPE_GSM) {
				// Highlight this button;
				mPhoneType = "GSM";
				bothOfGSMButton.setSelected(true);
			} else if (callType == TelephonyManager.PHONE_TYPE_CDMA) {
				// Highlight this button;
				mPhoneType = "CDMA";
				bothOfCDMAButton.setSelected(true);
			} else {
				if (networkType != null) {
					if (networkType.equalsIgnoreCase("CDMA")) {
						// mPhoneType = "CDMA";
						bothOfCDMAButton.setSelected(true);
					} else if (networkType.equalsIgnoreCase("GSM")) {
						// mPhoneType = "GSM";
						bothOfGSMButton.setSelected(true);
					}
				}
			}
		}
	}

    private void showCardDifferentDialog(String phoneType){
    bothOfCDMAButton.setVisibility(View.GONE);
    bothOfGSMButton.setVisibility(View.GONE);

    OnCancelListener cancelListener;
    cancelListener = new OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
        if (mUseDifferentCard != null) {
            mUseDifferentCard.dismiss();
            mUseDifferentCard = null;
        }
        finish();
        }};
    DialogInterface.OnDismissListener dimissListener
        = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            if (!isIPNotSetDialogShown) { // Do not finish if another alert dialog is shown
                finish();
            }
        }
        };
    mUseDifferentCard = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(getString(R.string.dial_use_different_card, phoneType))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            /* User clicked OK so do some stuff */
            if (mPhoneType.equalsIgnoreCase("CDMA")){
            callByGSM();
            }else{
            callByCDMA();
            }
            if (!isIPNotSetDialogShown) { // Do not finish if another alert dialog is shown
                finish();
            }
            return;
        }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            /* User clicked OK so do some stuff */
            finish();
            return;
            }
        })
        .setOnCancelListener(cancelListener)
        .create();
    mUseDifferentCard.setOnDismissListener(dimissListener);
    mUseDifferentCard.show();

    }

	private void callByCDMA() {
		if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
			// There is no number entered.
			finish();
			return;
		}
		// a23237 begin
		if (isIpCall) {
			shareddata = getSharedPreferences("IP_PREFIX", MODE_WORLD_READABLE);
			ip_cdma = shareddata.getString("ip_cdma", null);
			if (ip_cdma == null) {
				// IP Prefix for CDMA never setup by the user, so use the
				// default setting.
				ip_cdma = getString(R.string.default_cdma_ip_prefix);
				phoneNumber = ip_cdma + phoneNumber;
			} else if (ip_cdma.length() == 0) {
				// IP Prefix for CDMA is disabled
				showDialog(NO_CDMA_IP_SET_DIALOG);
				return;
			} else {
				phoneNumber = ip_cdma + phoneNumber;
			}
			Log.v(TAG, "callByCDMA: " + phoneNumber);
		}
		// a23237 end
		Intent intent;
		if (isIntRoamCallBackCall) {
			intent = new Intent(
					"com.android.phone.InternationalRoamingCallback");
			intent.putExtra("phoneNumber", phoneNumber);
			intent.putExtra(ContactsUtils.IntRoamCallBackCall, true);
		} else {
			intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts(
					"tel", phoneNumber, null));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (isIntRoamCall) {
				intent.putExtra(ContactsUtils.CALLED_BY,
						ContactsUtils.DIAL_BY_INTL_ROAMING_CALL);
			}
		}
		intent.putExtra("phone", "CDMA");
		// a23237 begin
		if (isIpCall) {
			intent.putExtra("ip_prefix", ip_cdma);
			Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_cdma + ");");
		}
		// a23237 end
		try {
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// don't crash if ip call is not supported.
		}
		finish();
	}

    private void callByGSM() {
		if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
			// There is no number entered.
			finish();
			return;
		}
		// a23237 begin
		if (isIpCall) {
			shareddata = getSharedPreferences("IP_PREFIX", MODE_WORLD_READABLE);
			ip_gsm = shareddata.getString("ip_gsm", null);
			if (ip_gsm == null) {
				// IP Prefix for GSM never setup by the user, so use the default
				// setting.
				int res_id = ContactsUtils
						.getDefaultIPPrefixbyPhoneType(TelephonyManager.PHONE_TYPE_GSM);
				if (res_id != -1) {
					ip_gsm = getString(res_id);
				} else { // For safty
					ip_gsm = getString(R.string.default_gsm_ip_prefix);
				}
				phoneNumber = ip_gsm + phoneNumber;
			} else if (ip_gsm.length() == 0) {
				// IP Prefix for GSM is disabled
				showDialog(NO_GSM_IP_SET_DIALOG);
				return;
			} else {
				phoneNumber = ip_gsm + phoneNumber;
			}
			Log.v(TAG, "callByGSM: " + phoneNumber);
		}
		// a23237 end
		Intent intent;
		if (isIntRoamCallBackCall) {
			intent = new Intent(
					"com.android.phone.InternationalRoamingCallback");
			intent.putExtra("phoneNumber", phoneNumber);
			intent.putExtra(ContactsUtils.IntRoamCallBackCall, true);
		} else {
			intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts(
					"tel", phoneNumber, null));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (isIntRoamCall) {
				intent.putExtra(ContactsUtils.CALLED_BY,
						ContactsUtils.DIAL_BY_INTL_ROAMING_CALL);
			}
		}
		intent.putExtra("phone", "GSM");
		// a23237 begin
		if (isIpCall) {
			intent.putExtra("ip_prefix", ip_gsm);
			Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_gsm + ");");
		}
		// a23237 end
		try {
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// don't crash if ip call is not supported.
		}
		finish();
	}

	private void callByNoIndicator() {
		if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
			// There is no number entered.
			finish();
			return;
		}
		// a23237 begin
		if (isIpCall) {
			if (PhoneModeManager.isDmds()) {
				Log.v(TAG, "isCDMAEnabled = " + isCDMAEnabled
						+ ", isGSMEnabled = " + isGSMEnabled + ".");
				shareddata = getSharedPreferences("IP_PREFIX",
						MODE_WORLD_READABLE);
				if (isCDMAEnabled) {
					ip_cdma = shareddata.getString("ip_cdma", null);
					if (ip_cdma == null) {
						// IP Prefix for CDMA never setup by the user, so use
						// the default setting.
						ip_cdma = getString(R.string.default_cdma_ip_prefix);
						phoneNumber = ip_cdma + phoneNumber;
					} else if (ip_cdma.length() == 0) {
						// IP Prefix for CDMA is disabled
						showDialog(NO_CDMA_IP_SET_DIALOG);
						return;
					} else {
						phoneNumber = ip_cdma + phoneNumber;
					}
					Log.v(TAG, "callByCDMA: " + phoneNumber);
				} else if (isGSMEnabled) {
					ip_gsm = shareddata.getString("ip_gsm", null);
					if (ip_gsm == null) {
						// IP Prefix for GSM never setup by the user, so use the
						// default setting.
						int res_id = ContactsUtils
								.getDefaultIPPrefixbyPhoneType(TelephonyManager.PHONE_TYPE_GSM);
						if (res_id != -1) {
							ip_gsm = getString(res_id);
						} else { // For safty
							ip_gsm = getString(R.string.default_gsm_ip_prefix);
						}
						phoneNumber = ip_gsm + phoneNumber;
					} else if (ip_gsm.length() == 0) {
						// IP Prefix for GSM is disabled
						showDialog(NO_GSM_IP_SET_DIALOG);
						return;
					} else {
						phoneNumber = ip_gsm + phoneNumber;
					}
					Log.v(TAG, "callByGSM: " + phoneNumber);
				} else {
					Log.v(TAG, "Wrong");
				}
			} else {
				shareddata = getSharedPreferences("IP_PREFIX",
						MODE_WORLD_READABLE);
				if (ContactsUtils.getCarrierName(this).equals(
						ContactsUtils.Carrier_CT)) {
					ip_cdma = shareddata.getString("ip_cdma", null);
					if (ip_cdma == null) {
						// IP Prefix never setup by the user, so use the default
						// setting.
						ip_cdma = getString(R.string.default_cdma_ip_prefix);
						phoneNumber = ip_cdma + phoneNumber;
					} else if (ip_cdma.length() == 0) {
						// IP Prefix is disabled
						showDialog(NO_CDMA_IP_SET_DIALOG);
						return;
					} else {
						phoneNumber = ip_cdma + phoneNumber;
					}
				} else {
					ip_gsm = shareddata.getString("ip_gsm", null);
					if (ip_gsm == null) {
						// IP Prefix never setup by the user, so use the default
						// setting.
						int res_id = ContactsUtils
								.getDefaultIPPrefixbyPhoneType(TelephonyManager.PHONE_TYPE_GSM);
						if (res_id != -1) {
							ip_gsm = getString(res_id);
						} else { // For safty
							ip_gsm = getString(R.string.default_gsm_ip_prefix);
						}
						phoneNumber = ip_gsm + phoneNumber;
					} else if (ip_gsm.length() == 0) {
						// IP Prefix is disabled
						showDialog(NO_GSM_IP_SET_DIALOG);
						return;
					} else {
						phoneNumber = ip_gsm + phoneNumber;
					}
				}
				Log.v(TAG, "callBySingleMode: " + phoneNumber);
			}
		}
		// a23237 end
		Intent intent;
		if (isIntRoamCallBackCall) {
			intent = new Intent(
					"com.android.phone.InternationalRoamingCallback");
			intent.putExtra("phoneNumber", phoneNumber);
			intent.putExtra(ContactsUtils.IntRoamCallBackCall, true);
		} else {
			intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts(
					"tel", phoneNumber, null));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (isIntRoamCall) {
				intent.putExtra(ContactsUtils.CALLED_BY,
						ContactsUtils.DIAL_BY_INTL_ROAMING_CALL);
			}
		}
		// a23237 begin
		if (isIpCall) {
			if (PhoneModeManager.isDmds()) {
				Log.v(TAG, "isCDMAEnabled = " + isCDMAEnabled
						+ ", isGSMEnabled = " + isGSMEnabled + ".");
				if (isCDMAEnabled) {
					intent.putExtra("ip_prefix", ip_cdma);
					Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_cdma
							+ ");");
				} else if (isGSMEnabled) {
					intent.putExtra("ip_prefix", ip_gsm);
					Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_gsm
							+ ");");
				} else {
					Log.v(TAG, "Wrong");
				}
			} else {
				if (ContactsUtils.getCarrierName(this).equals(
						ContactsUtils.Carrier_CT)) {
					intent.putExtra("ip_prefix", ip_cdma);
					Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_cdma
							+ ");");
				} else {
					intent.putExtra("ip_prefix", ip_gsm);
					Log.v(TAG, "intent.putExtra(\"ip_prefix\", " + ip_gsm
							+ ");");
				}
			}
		}
		// a23237 end
		try {
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// don't crash if ip call is not supported.
		}
		finish();
	}

	private void callForEmergency() {
		if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
			// There is no number entered.
			finish();
			return;
		}
		Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
				Uri.fromParts("tel", phoneNumber, null));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}

// a23237 begin
   @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder dlg_bld = new AlertDialog.Builder(mDialogContext);
        dlg_bld.setTitle(getString(R.string.no_ip_dlg_title));
        if (id == NO_CDMA_IP_SET_DIALOG) {
            dlg_bld.setMessage(getString(R.string.no_cdma_ip_dlg_msg));
            dlg_bld.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    handleCDMAIPDialogOKPressed();
                }
            });
            dlg_bld.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    handleCDMAIPDialogCancelPressed();
                }
            });
        } else if (id == NO_GSM_IP_SET_DIALOG) {
            dlg_bld.setMessage(getString(R.string.no_gsm_ip_dlg_msg));
            dlg_bld.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    handleGSMIPDialogOKPressed();
                }
            });
            dlg_bld.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    handleGSMIPDialogCancelPressed();
                }
            });
        }
        AlertDialog dialog = dlg_bld.create();
        DialogInterface.OnDismissListener dismisListener =
                    new DialogInterface.OnDismissListener(){
                     public void onDismiss(DialogInterface dialog) {
                                    TransDialPad.this.finish();
                        }
                    };
        dialog.setOnDismissListener(dismisListener);
        isIPNotSetDialogShown = true;
        return dialog;
    }

    protected void handleCDMAIPDialogOKPressed() {
        Log.v(TAG, "handleCDMAIPDialogOKPressed");
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", phoneNumber, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("phone", "CDMA");
        startActivity(intent);
        finish();
    }

    protected void handleGSMIPDialogOKPressed() {
        Log.v(TAG, "handleGSMIPDialogOKPressed");
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", phoneNumber, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("phone", "GSM");
        startActivity(intent);
        finish();
    }

    protected void handleCDMAIPDialogCancelPressed() {
        Log.v(TAG, "handleCDMAIPDialogCancelPressed");
        finish();
    }

    protected void handleGSMIPDialogCancelPressed() {
        Log.v(TAG, "handleGSMIPDialogCancelPressed");
        finish();
    }
// a23237 end

}
