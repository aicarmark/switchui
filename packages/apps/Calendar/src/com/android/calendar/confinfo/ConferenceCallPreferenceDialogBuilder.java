package com.android.calendar.confinfo;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

//MOTO MOD BEIGN IKCBS-2783
//import com.motorola.android.provider.MotoSettings;
//MOTO MOD END IKCBS-2783

import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
//Begin Motorola vnxm46 02/07/2012 IKSPYLA-1849 Hyphenation not working correctly
import java.util.Locale;
//End IKSPYLA-1849
public class ConferenceCallPreferenceDialogBuilder {
    private static Context mContext = null;
    public static boolean checkConferenceInfo(Context ctx, String confNumber, String confId) {
        // now we only check whether conference number is empty or not
        if ((ctx == null) || (confNumber == null) || (confNumber.length() == 0))
            return false;

        return true;
    }

    public static boolean checkConferenceInfo(Context ctx, EditText numberWidget, EditText idWidget) {
        if ((ctx == null) || (numberWidget == null) || (idWidget == null))
            return false;

        return checkConferenceInfo(ctx, numberWidget.getText().toString(), idWidget.getText().toString());
    }

    public static void saveConferenceInfo(Context ctx, EditText numberWidget, EditText idWidget) {
        if ((numberWidget == null) || (idWidget == null))
            return;
        mContext = ctx;

        String newNumber = numberWidget.getText().toString();
        String newId = idWidget.getText().toString();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        String oldNumber = prefs.getString(GeneralPreferences.KEY_CONFERENCE_NUMBER, null);
        String oldId = prefs.getString(GeneralPreferences.KEY_CONFERENCE_ID, null);

        boolean dirty = false;

        if (!TextUtils.equals(newNumber, oldNumber)) {
            if (newNumber != null) {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_NUMBER, newNumber);
            } else {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_NUMBER, "");
            }
            dirty = true;
        }

        if (!TextUtils.equals(newId, oldId)) {
            if (newId != null) {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_ID, newId);
            } else {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_ID, "");
            }
            dirty = true;
        }

        if (dirty) {
            editor.commit();
            new saveConferenceInfoInBackground().execute(newNumber,newId);
        }
    }

    // confNumber and confId are the preloaded conference call info
    public static void buildDialog(final Context ctx, Builder builder, String confNumber, String confId) {
        LayoutInflater inflater = (LayoutInflater) ctx
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater
            .inflate(R.layout.conference_call_preference_dialog_layout, null);

        final EditText numberWidget = (EditText) layout.findViewById(R.id.telephone_number);
        if (numberWidget != null) {
            /* MOTO MOD BEIGN IKCBS-2783 Porting of CR IKCBS-1015
             * Check the Hyphenation feature flag, if flag is enabled
             * We don't call formatNumber, else we call formatting editied number
            */
            boolean hyphenStatus = false;
                    //MotoSettings.getInt(ctx.getContentResolver(),
                     //              "hyphenation_feature_enabled", 0) != 1 ? false : true;

            //Begin Motorola vnxm46 02/07/2012 IKSPYLA-1849 Hyphenation not working correctly
            String language = Locale.getDefault().getDisplayLanguage();
            if(!hyphenStatus && !ctx.getString(R.string.portuguese).equals(language)){
            //End IKSPYLA-1849
                numberWidget.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
            }
            //MOTO END IKCBS-2783

            numberWidget.setKeyListener(DigitsKeyListener.getInstance("0123456789-+(). "));
            numberWidget.setRawInputType(InputType.TYPE_CLASS_PHONE);

            if (confNumber != null) {
                numberWidget.setText(confNumber);
            }
        }

        final EditText idWidget= (EditText) layout.findViewById(R.id.conference_id);
        if (idWidget != null) {
            idWidget.setKeyListener(DigitsKeyListener.getInstance("0123456789-*#,;"));
            idWidget.setRawInputType(InputType.TYPE_CLASS_PHONE);

            if (confId != null) {
                idWidget.setText(confId);
            }
        }

        builder.setView(layout);
        builder.setTitle(R.string.preferred_conference_call_info_dlg_title);

        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // always save user's input info
                    saveConferenceInfo(ctx, numberWidget, idWidget);
                    dialog.dismiss();
                }
        })
        .setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
        });
    }

    // confNumber and confId are the preloaded conference call info
    public static AlertDialog createDialog(Context ctx, String confNumber, String confId) {
        Builder builder = new Builder(ctx);
        buildDialog(ctx, builder, confNumber, confId);
        return builder.create();
    }

    private static void writeConferenceInfoToProvider(Context ctx, String newNumber, String newId) {
        Uri uri = Uri.parse("content://" + CalendarContract.AUTHORITY + "/conference_call_info");
        ContentResolver cr = ctx.getContentResolver();
        ContentValues updateValues = new ContentValues();
        updateValues.put(ConferenceCallInfo.MY_CONFERENCE_NUM, newNumber);
        updateValues.put(ConferenceCallInfo.MY_CONFERENCE_ID, newId);
        cr.update(uri, updateValues, null, null);
    }

    private static class saveConferenceInfoInBackground extends AsyncTask<String, String, String> {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected String doInBackground(String... params) {
            String newNumber = params[0];
            String newId = params[1];
            writeConferenceInfoToProvider(mContext, newNumber, newId);
            return null;
        }
    }
}
