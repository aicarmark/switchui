package com.motorola.FileManager;
import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class ChooseActivity extends Activity implements Dialog.OnDismissListener,
        AdapterView.OnItemClickListener, View.OnClickListener, Dialog.OnCancelListener {
    public final static String KEY_ACTION = "action";
    public final static String KEY_PATH = "path";
    public final static String KEY_SOURCE = "source";

    private static final int DIALOG_DELETE_CHOICE = 7;
    private static final int DIALOG_HIDEUNHIDE_CHOICE = 8;

    private int mAction = 0;
    private static FileManagerAdapter mAdapter;
    private ListView mList;
    private AbsListView mGrid;
    private AbsListView mView;
    //private CheckBox mCBSelect;
    private Button mBtnOk;
    private Button mBtnCancel;
    private ActionBar mBar;
    private static MenuItem mSelected;
    private static MenuItem mUnselected;

    private PerformDialog mPerformDlg;

    // handle progress dialog when opening a folder more than
    // Util.FOLDER_CHILD_COUNT_PROGRESS
    private ProgressDialog mProgressDialog;
    private Context mContext;
    private FileInfo mOpenDirFi;
    private Util.FilesContainer mFilesContainer = new Util.FilesContainer();
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Util.MSG_PROGRESS_OPENDIR_SHOW:
                if (mOpenDirFi.getChildCount() > Util.FOLDER_CHILD_COUNT_PROGRESS) {
                    mProgressDialog = new ProgressDialog(mContext);
                    mProgressDialog.setMessage(getString(R.string.msg_opening_progress));
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                }
                break;
            case Util.MSG_PROGRESS_OPENDIR_DISMISS:
                if (mOpenDirFi.getChildCount() > Util.FOLDER_CHILD_COUNT_PROGRESS) {
                    mProgressDialog.dismiss();
                }
                openFolder();
                break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose);
        
        mBar = getActionBar();
        mBar.setDisplayHomeAsUpEnabled(true);
        mContext = this;
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String path=null;
        
        if (bundle != null) {
            mAction = bundle.getInt(KEY_ACTION);
            path = bundle.getString(KEY_PATH);
        }

        mList = (ListView) findViewById(R.id.list);
        mGrid = (AbsListView)findViewById(R.id.grid); //w23001-add the grid feature
        
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);

        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        

        mPerformDlg = new PerformDialog(this);
        mPerformDlg.setOnDismissListener(this);
        mPerformDlg.setOnCancelListener(this);
        
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);

        VirtualFolder.setContext(mContext);
        if (mAction == Util.ACTION_DELETE) {
            setTitle(R.string.title_delete);
            mBtnOk.setText(R.string.btn_delete);
        } else if (mAction == Util.ACTION_COPY) {
            setTitle(R.string.title_copyto);
            mBtnOk.setText(R.string.btn_copyto);
        } else if (mAction == Util.ACTION_MOVE) {
            setTitle(R.string.title_move);
            mBtnOk.setText(R.string.btn_move);
        }
	  else if (mAction == Util.ACTION_HIDEUNHIDE) {
       	    setTitle(R.string.title_hideunhide);
            mBtnOk.setText(R.string.btn_hide);
        }
        mView = getView();
        mOpenDirFi = FileInfo.make(path);

        if (mOpenDirFi != null && mOpenDirFi.isDirectory()) {
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_SHOW);

            Util.GetChildrenThread thread = null;
            if (mAction == Util.ACTION_HIDEUNHIDE) thread = new Util.GetChildrenThread(mHandler, mFilesContainer,
                    mOpenDirFi, null, Util.SORT_BY_NAME, true, true);
            else thread = new Util.GetChildrenThread(mHandler, mFilesContainer,
                    mOpenDirFi, null, Util.SORT_BY_NAME, true, false);

            thread.start();

        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        mSelected = menu.add(R.string.menu_select_all);
        mSelected.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mSelected.setIcon(R.drawable.ic_menu_select_items);
        
        mUnselected = menu.add(R.string.menu_unselect_all);
        mUnselected.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mUnselected.setIcon(R.drawable.ic_menu_unselect_items);        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mSelected.setEnabled(true);
        mUnselected.setEnabled(true);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
        	Log.d("", "onOptionsItemSelected -back");
		Intent intent = getIntent();
            	setResult(RESULT_CANCELED, intent);
        	finish();
        }
        if (item == mSelected) {
        	Log.d("", "onOptionsItemSelected -select");
        	selectAll(true);
            return true;
        } else if (item == mUnselected) {
        	Log.d("", "onOptionsItemSelected - unselect");
        	selectAll(false);
            return true;
        }
        return true;
    }
    private AbsListView getView() {
        // w23001 support grid view
        if (FileManagerActivity.mViewMode == Util.VIEW_LIST) {
           mList.setVisibility(View.VISIBLE);
           mGrid.setVisibility(View.GONE);
           return mList;
        } else if (FileManagerActivity.mViewMode == Util.VIEW_GRID){
        	mList.setVisibility(View.GONE);
            mGrid.setVisibility(View.VISIBLE);
        	return mGrid;
        }
        return null;
    }


    public void openFolder() {        
    	//if (mAction == Util.ACTION_HIDEUNHIDE) { 
    	mAdapter = new FileManagerAdapter(this);
        mAdapter.setShowCountForFolder(false);
        
        mAdapter.setViewMode(FileManagerActivity.mViewMode);
        mAdapter.updateView();
        mAdapter.setChoiceMode(true);
        mAdapter.setCurrentFile(mOpenDirFi);
        mAdapter.setFiles(mFilesContainer.getFiles());	 
        mAdapter.notifyDataSetChanged();
    	//} else {
    	//	mAdapter.setFiles(mFilesContainer.getUnHideFiles());	
    	//}
        mView.setAdapter(mAdapter);        
        mView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);       

        if (mAction == Util.ACTION_HIDEUNHIDE)
        {
        	for(int i=0;i<mView.getCount();i++)
        	{	FileInfo fi = (FileInfo) mAdapter.getItem(i);
                if (fi == null ) {
                    Log.e("ChooseActivity", "fi is null");
                    return;
                }
        		if (fi.isHidden()) {
        			mView.setItemChecked(i, true);
        		} else {
        			mView.setItemChecked(i, false);
        		}
        	}
        }
        mView.setOnItemClickListener(this);

    }

    public void onClick(View v) {
        if (v == mBtnCancel) {
	    Intent intent = getIntent();
            setResult(RESULT_CANCELED, intent);
            finish();
        } else if (v == mBtnOk) {
            SparseBooleanArray array = mView.getCheckedItemPositions();
            if (array != null) {
                int size = array.size();
                int nCount = 0;

                for (int i = 0; i < size; i++) {
                    if (array.valueAt(i))
                        nCount++;
                }
                //miaoz enable hide none folder
                if (nCount == 0 && mAction != Util.ACTION_HIDEUNHIDE) {
                    Toast.makeText(this, R.string.need_choose_file, Toast.LENGTH_SHORT).show();
                } else {
                    if (mAction == Util.ACTION_DELETE) {
                        showDialog(DIALOG_DELETE_CHOICE);
                    } else if (mAction == Util.ACTION_COPY) {
                        Util.getDestFolder(this, Util.REQUEST_TYPE_GETCOPYDEST);
                    } else if (mAction == Util.ACTION_MOVE) {
                        Util.getDestFolder(this, Util.REQUEST_TYPE_GETMOVEDEST);
                    }
                    else if (mAction == Util.ACTION_HIDEUNHIDE) {
                    	showDialog(DIALOG_HIDEUNHIDE_CHOICE);
		    }
                }
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (dialog.equals(mPerformDlg)) {
            Intent intent = getIntent();
            
            //Begin CQG478 liyun, IKDOMINO-2368, 2011-9-9
            intent.putExtra(PerformDialog.PFM_STOP, mPerformDlg.mStop);
            intent.putExtra(PerformDialog.PFM_HASSKIPPED, mPerformDlg.mHasSkiped);
            intent.putExtra(PerformDialog.PFM_RINTTONE, mPerformDlg.mRingtone);
            intent.putExtra(PerformDialog.PFM_SAMEDISK, mPerformDlg.isSameDisk);
            intent.putExtra(PerformDialog.PFM_FINISH, mPerformDlg.mFinished);
            //End
            Uri uri = Util.getFileUriFromPath(mPerformDlg.mDestFolder);
            if (mPerformDlg.mStop){
            	intent.setData(uri);
                setResult(RESULT_CANCELED, intent);
            }
            else if (mAction == Util.ACTION_HIDEUNHIDE) {
            	setResult(RESULT_OK, intent);
            }
	    else if (mPerformDlg.mDestFolder != null) {
                intent.setData(uri);
                setResult(RESULT_OK, intent);
            }
            
            finish();
        }
    }

    public void onCancel(DialogInterface dialog) {
        if (dialog.equals(mPerformDlg)) {
            Intent intent = getIntent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Util.REQUEST_TYPE_GETMOVEDEST) {
            if (resultCode == RESULT_OK) {
                String path = Util.getPathFromFileUri(data.getData());
                if (path != null) {
                    moveChoice(path);
                }
            }
        } else if (requestCode == Util.REQUEST_TYPE_GETCOPYDEST) {
            if (resultCode == RESULT_OK) {
                String path = Util.getPathFromFileUri(data.getData());
                if (path != null) {
                    copyChoice(path);
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

        case DIALOG_DELETE_CHOICE:
	{
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.dialog_confirm_delete, null);
            final CheckBox cb_folder = (CheckBox) view.findViewById(R.id.cb_subfolder);
            cb_folder.setChecked(false);
            cb_folder.setVisibility(View.GONE);
            return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_confirm_delete)
                    .setView(view).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
                            android.R.string.ok, new OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    FileInfo[] choiceList = getChoiceList();
                                    if (choiceList != null) {
                                        mPerformDlg.setAction(Util.ACTION_DELETE, choiceList, null);
                                        mPerformDlg.setTitle(R.string.title_dlg_deleting);
                                        mPerformDlg.setIcon(R.drawable.ic_menu_delete);
                                        mPerformDlg.show();
                                    }
                                }
                            }

                    ).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel should not do anything.
                        }

                    }).create();
	}
        case DIALOG_HIDEUNHIDE_CHOICE:
       {
        
        	return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_confirm_hide)
                .setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok, new OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                FileInfo[] choiceList = getNeedHideFolderList();
                                if (choiceList != null) {
                                    mPerformDlg.setAction(Util.ACTION_HIDEUNHIDE, choiceList, null);                                    
                                }
                                else {
                                	//use destFolder to do unhide all folder
                                	mPerformDlg.setAction(Util.ACTION_HIDEUNHIDE, choiceList, mOpenDirFi.getRealPath());                                    
                                }
                                mPerformDlg.setTitle(R.string.title_dlg_hide);
                                mPerformDlg.setIcon(R.drawable.ic_menu_hideunhide);
                                mPerformDlg.show();
                            }
                        }

                ).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel should not do anything.
                    }

                }).create();
    	}


        }
        return null;
    }

    private void selectAll(boolean choiced) {

        int n = mAdapter.getCount();

        for (int i = 0; i < n; i++)
        	mView.setItemChecked(i, choiced);
    }

    private void moveChoice(String destFolder) {
        FileInfo[] choiceList = getChoiceList();
        if (choiceList != null && checkDestFolder(destFolder)) {
            mPerformDlg.setAction(Util.ACTION_MOVE, choiceList, destFolder);
            mPerformDlg.setTitle(R.string.title_dlg_moving);
            mPerformDlg.setIcon(R.drawable.ic_menu_move);
            mPerformDlg.show();


        }
    }

    private void copyChoice(String destFolder) {
        FileInfo[] choiceList = getChoiceList();
        if (choiceList != null && checkDestFolder(destFolder)) {
            mPerformDlg.setAction(Util.ACTION_COPY, choiceList, destFolder);
            mPerformDlg.setTitle(R.string.title_dlg_copying);
            mPerformDlg.setIcon(R.drawable.ic_menu_copy);
            mPerformDlg.show();
        }
    }

    private boolean checkDestFolder(String destFolder) {
        try {
            SparseBooleanArray array = mView.getCheckedItemPositions();
            if (array != null) {
                int size = array.size();
                for (int i = 0; i < size; i++) {
                    if (array.valueAt(i)) {
                        FileInfo fi = (FileInfo) mAdapter.getItem(array.keyAt(i));
                        if (fi == null)
                            return false;

                        File fileSrc = new File(fi.getRealPath());
                        File fileDest = new File(destFolder);

                        if (fileSrc == null || fileDest == null)
                            return false;

                        if (fileSrc.isFile()) {
                            if (fileDest.equals(fileSrc.getParentFile())) {
                                Toast.makeText(this, R.string.cannot_same_folder,
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            return true;
                        }

                        if (!fileDest.isDirectory()) {
                            Toast.makeText(this, getString(R.string.invalidate_folder, destFolder),
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        if (!fileDest.canWrite()) {
                            Toast.makeText(this,
                                    getString(R.string.cannot_write_folder, destFolder),
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        if (fileDest.equals(fileSrc.getParentFile())) {
                            Toast.makeText(this, R.string.cannot_same_folder, Toast.LENGTH_SHORT)
                                    .show();
                            return false;
                        }

                        //Begin CQG478 liyun, IKDOMINO-3650, sub folder 2011-11-01
                        if (Util.isSubFolder(fi.getRealPath(), destFolder)) {
                        //End
                            Toast.makeText(this, R.string.cannot_sub_folder, Toast.LENGTH_SHORT)
                                    .show();
                            return false;
                        }

                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private FileInfo[] getChoiceList() {
        SparseBooleanArray array = mView.getCheckedItemPositions();
        if (array != null) {
            int size = array.size();
            int count = 0;
            for (int i = 0; i < size; i++) {
                if (array.valueAt(i)) {
                    count++;
                }
            }
            if (count > 0) {
                FileInfo[] list = new FileInfo[count];
                int j = 0;
                for (int i = 0; i < size; i++) {
                    if (array.valueAt(i)) {
                        list[j++] = (FileInfo) mAdapter.getItem(array.keyAt(i));
                    }
                }
                return list;
            }
        }
        return null;
    }

    //return FileInfo[] attr>HIDDEN is accordingly set or unset with user selection.
    private FileInfo[] getNeedHideFolderList() {
        SparseBooleanArray array = mView.getCheckedItemPositions();
        if (array != null) {
            int size = array.size();            
            int count = 0;
            for (int i = 0; i < size; i++) {
            	FileInfo temp = (FileInfo) mAdapter.getItem(array.keyAt(i));
            	//Begin, kbg374, IKDOMINO-5524, check the bound, 2011-12-30
                if (temp == null ) {
                    Log.e("ChooseActivity", "temp is null");
                    return null;
                }
                //End
                if ( (array.valueAt(i) == true) )  count++;
                else if ( (array.valueAt(i) == false) && (temp.isHidden() == true) ) count++;               
            }
            if (count > 0) {
                FileInfo[] list = new FileInfo[count];
                int j = 0;
                for (int i = 0; i < size; i++) {
                	FileInfo temp = (FileInfo) mAdapter.getItem(array.keyAt(i));
                	//Begin, kbg374, IKDOMINO-5524, check the bound, 2011-12-30
                    if (temp == null ) {
                        Log.e("ChooseActivity", "temp is null");
                        return null;
                    }
                    //End
                    if ( (array.valueAt(i) == true) )  
                    {
                    	temp.setHidden();
                    	list[j++] = temp;
                    	
                    }
                    else if ( (array.valueAt(i) == false) && (temp.isHidden() == true) )
                    {
                    	temp.setNotHidden();
                    	list[j++] = temp;
                    }
                }
                return list;
            }
        }
        
        return null;
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        SparseBooleanArray array = mView.getCheckedItemPositions();
        int count = 0;
        if (array != null) {
            int size = array.size();
            for (int i = 0; i < size; i++) {
                if (array.valueAt(i)) {
                    count++;
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())){
            	Log.d("","w23001- sd is unmounted");
            	if (mPerformDlg != null ){
	    			if (mPerformDlg.isShowing()) {
	    				mPerformDlg.cancel();
	    			}
	    		}
		Intent intent2 = getIntent();
                ChooseActivity.this.setResult(RESULT_CANCELED, intent2);
            	finish();
            }
        }
    };
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mPerformDlg != null){
        	//mPerformDlg.mStop = true;
        	if (mPerformDlg.isShowing()) {
				mPerformDlg.cancel();
			}
        }
    }
}
