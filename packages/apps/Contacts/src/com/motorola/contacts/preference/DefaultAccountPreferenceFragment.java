package com.motorola.contacts.preference;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;

public class DefaultAccountPreferenceFragment extends PreferenceFragment implements
         OnPreferenceClickListener, SelectAccountDialogFragment.Listener {

    public static final String TAG = "DefaultAccountPreferenceFragment";
    public static final String KEY_CONTACT_STORAGE_PREFERENCE = "contact_storage_preference";
    private Preference mPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //SelectAccountDialogFragment.show(getFragmentManager(),
        //                    DefaultAccountPreferenceFragment.this, R.string.dialog_new_contact_account,
        //                    AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, null);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_account_selection);

        // Get a reference to the preferences
        mPreference = getPreferenceScreen().findPreference(KEY_CONTACT_STORAGE_PREFERENCE);
        if (mPreference == null) {
            Log.e(TAG, "can't find preference: contact_storage_preference");
            return;
        }
        mPreference.setOnPreferenceClickListener(this);
        List<AccountWithDataSet> mAccounts = ContactPreferenceUtilities.GetAvailableAccounts(
                mPreference.getContext(), true);
        if (mAccounts != null && mAccounts.size() > 0) {
            mPreference.setEnabled(true);
        }else {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPreference.setSummary(getSummary(mPreference.getContext()));
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        /*final SelectAccountDialogFragment dialog = new SelectAccountDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), SelectAccountDialogFragment.TAG);*/
        SelectAccountDialogFragment.show(getFragmentManager(),
                            DefaultAccountPreferenceFragment.this, R.string.dialog_new_contact_account,
                            AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITH_CARD, null); // MOT CHINA
        return true;
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        mPreference.setSummary(getSummary(mPreference.getContext()));   //MOT MOD, NullPointException
        getActivity().finish(); //MOT MOD, when button OK in SelectAccountDialogFragment is pressed, finish activity
        return;
    }

    public void onAccountSelectorCancelled(){
        getActivity().finish(); //MOT MOD, when button OK in SelectAccountDialogFragment is pressed, finish activity
        return;
    };

    private CharSequence getSummary(Context mContext) {

        if (ContactPreferenceUtilities.IfUserChoiceRemembered(mContext)
                && ContactPreferenceUtilities.GetUserPreferredAccountIndex(mContext, true)!= -1){
            Account userPreAcc = ContactPreferenceUtilities.GetUserPreferredAccountFromSharedPreferences(mContext);
            if (userPreAcc!=null) {
                if (HardCodedSources.ACCOUNT_LOCAL_DEVICE.equals(userPreAcc.name)) {
                    return getString(R.string.account_local_device);
                } else if (HardCodedSources.ACCOUNT_CARD_C.equals(userPreAcc.name)) {
                    return getString(R.string.account_card_C);
                } else if (HardCodedSources.ACCOUNT_CARD_G.equals(userPreAcc.name)) {
                    return getString(R.string.account_card_G);
                } else if (HardCodedSources.ACCOUNT_CARD.equals(userPreAcc.name)) {
                    final int phoneType = SimUtility.getTypeByAccountName(userPreAcc.name);
                    if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                        return getString(R.string.account_card_uim);
                    } else {
                        return getString(R.string.account_card_G);
                    }
                } else {
                    return userPreAcc.name;
                }
            }
        }
        return mContext.getString(R.string.contact_storage_choose_account);
    }

}
