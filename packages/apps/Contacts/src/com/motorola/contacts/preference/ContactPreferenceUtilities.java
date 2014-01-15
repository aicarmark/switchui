package com.motorola.contacts.preference;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;

public class ContactPreferenceUtilities {
    public static final String TAG = "MotorolaContactPreferenceUtilities";

    public static void SaveUserPreferredAccountToSharedPreferences(Context context,boolean isRemembered, Account account) {

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            if (editor != null) {
                editor.putBoolean("com.motorola.contacts.userPreferRemembered", isRemembered);
                if (account!=null) {
                    editor.putString("com.motorola.contacts.userPreferredAccountType", account.type);
                    editor.putString("com.motorola.contacts.userPreferredAccountName", account.name);
                }
                editor.commit();
            } else {
                Log.e(TAG, "SharedPreferences.Editor is null");
            }

        } else {
            Log.e(TAG, "SharedPreferences sp is null");
        }
    }

    public static AccountWithDataSet GetUserPreferredAccountFromSharedPreferences(Context context) {

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp != null) {
            String acc_type = sp.getString("com.motorola.contacts.userPreferredAccountType",null);
            String acc_name = sp.getString("com.motorola.contacts.userPreferredAccountName",null);
            if (acc_name!=null && acc_type!=null) {
                return new AccountWithDataSet(acc_name,acc_type,null);
            }
        } else {
            Log.e(TAG, "SharedPreferences sp is null");
        }
        return null;
    }

    public static boolean IfUserChoiceRemembered(Context context) {

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp != null) {
            return sp.getBoolean("com.motorola.contacts.userPreferRemembered", false);
        } else {
            Log.e(TAG, "SharedPreferences sp is null");
        }
        return false;
    }

    public static int GetUserPreferredAccountIndex(Context context,boolean writableOnly) {
        List<AccountWithDataSet> mAccounts = GetAvailableAccounts(context,writableOnly);
        Account userAccount = GetUserPreferredAccountFromSharedPreferences(context);
        if (userAccount!=null && mAccounts!=null) {
            for (int i=0;i<mAccounts.size();i++) {
                if (mAccounts.get(i).name.equalsIgnoreCase(userAccount.name) && mAccounts.get(i).type.equalsIgnoreCase(userAccount.type)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static List<AccountWithDataSet> GetAvailableAccounts(Context context,boolean writableOnly) {
        //need get application context instead of context, otherwise you MAY not get proper account type manager instance, a bug in Google design
        AccountTypeManager mAccountTypes = AccountTypeManager.getInstance(context.getApplicationContext());
        List<AccountWithDataSet> mAccounts = mAccountTypes.getAccountsWithCard(writableOnly); // MOT CHINA
        return mAccounts;
    }
}
