package com.motorola.sdcardbackuprestore;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MainUI extends ListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "SDCardB&R MainUI";
    private static final String TITLE = "title";
    private static final String SUMMARY = "summray";
    private static final int INDEX_BACKUP = 0;
    private static final int INDEX_RESTORE = 1;
    private static final int INDEX_IMPORT = 2;
    private static final int INDEX_EXPORTBYACCOUNT = 3;
    SharedPreferences mCaution = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        Context mContext = getApplicationContext();
    	if (SdcardManager.noSdcard(this)) {
            AlertDialog ad = new AlertDialog.Builder(MainUI.this, Constants.DIALOG_THEME)
                .setMessage(getString(R.string.no_sdcard))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
            ad.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            ad.show();
        } else {
        	setContentView(R.layout.main);
    	    ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
    	    HashMap<String, String> backup = new HashMap<String, String>();
    	    backup.put(TITLE, getString(R.string.backup_title));
    	    backup.put(SUMMARY, getString(R.string.backup_summary));
    	    HashMap<String, String> restore = new HashMap<String, String>();
    	    restore.put(TITLE, getString(R.string.restore_title));
    	    restore.put(SUMMARY, getString(R.string.restore_summary));
    	    HashMap<String, String> import3rd = new HashMap<String, String>();
    	    import3rd.put(TITLE, getString(R.string.import3rd_title));
    	    import3rd.put(SUMMARY, getString(R.string.import3rd_summary));
            HashMap<String, String> exportbyaccount = new HashMap<String, String>();
            exportbyaccount.put(TITLE, getString(R.string.ExportByAccount_title));
    	    exportbyaccount.put(SUMMARY, getString(R.string.ExportByAccount_summary));
    	    data.add(backup);
    	    data.add(restore);
    	    data.add(import3rd);
            data.add(exportbyaccount);
    	    ListAdapter listAdapter = new SimpleAdapter(this, data, R.layout.main_ui_list_item_layout,
    	    		new String[]{TITLE, SUMMARY}, 
    	    		new int[]{R.id.title, R.id.summary});
    	    setListAdapter(listAdapter);
    	    mCaution = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_WORLD_READABLE);
    	    int canPhoneMemoryErase = Integer.parseInt(getString(R.string.can_phone_sdcard_erase));
            if (SdcardManager.hasInternalSdcard(this) && !SdcardManager.isSdcardMounted(this, true)) {
                if (canPhoneMemoryErase == 1) {
                    if (!mCaution.getBoolean(Constants.NOT_SHOW_ANYMORE, false)) {
                        showCautionDialog();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.sdcard_is_unavailable), Toast.LENGTH_LONG).show();
                }
    		}
        }
	    
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	// TODO Auto-generated method stub
    	if (position == INDEX_BACKUP) {
			Intent intent = new Intent(this, Backup.class);
			startActivity(intent);
		} else if (position == INDEX_RESTORE) {
			Intent intent = new Intent(this, Restore.class);
			startActivity(intent);
		} else if (position == INDEX_IMPORT) {
			Intent intent = new Intent(this, Import3rd.class);
			startActivity(intent);
		} else if (position == INDEX_EXPORTBYACCOUNT) {
			Intent intent = new Intent(this, ExportByAccount.class);
			startActivity(intent);
		}
    	super.onListItemClick(l, v, position, id);
    }
    
    public void showCautionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View contentView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.caution_dialog, null, false);
        final CheckBox cb = (CheckBox)contentView.findViewById(R.id.caution_marker);
        builder.setTitle(R.string.caution);
        builder.setView(contentView);
        builder.setPositiveButton(R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (cb.isChecked()) {
                    SharedPreferences.Editor editor = mCaution.edit();
                    editor.putBoolean(Constants.NOT_SHOW_ANYMORE, true);
                    editor.commit();
                }
            }
        });
        AlertDialog ad = builder.create();
        ad.setCanceledOnTouchOutside(false);
        ad.show();
    }

}
