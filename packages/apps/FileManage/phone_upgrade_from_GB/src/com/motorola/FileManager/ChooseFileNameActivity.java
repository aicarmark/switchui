package com.motorola.FileManager;

import java.util.ArrayList;
import android.view.Window;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

import com.motorola.FileManager.storage.MotoEnvironment;

import android.util.Log;

public class ChooseFileNameActivity extends Activity implements View.OnClickListener,
        AdapterView.OnItemClickListener {
    private final static int REQUEST_TYPE_CROP_IMAGE = 101;

    private final static int DIALOG_NEW_FOLDER = 0x100;

    private static FileManagerAdapter mAdapter;

    private MenuItem mNewFolder;

    private ListView mList;
    private AbsListView mGrid;
    private AbsListView mView;
    private TextView mEmpty;
    //private ImageView mBtnUp;

    private Button mBtnOk;
    private Button mBtnCancel;

    //private TextView mTitleItemText;
    //private ImageView mTitleIcon;
    private ActionBar mBar;
    
    private boolean isEmmc;
    public static final String ID_LOCAL = "Local";
    public static final String ID_EXTSDCARD = "Extcard";
    public static String mRoot;

    private static final String SAVED_DIALOGS_TAG = "android:savedDialogs";
    public static StorageManager sm;
    
    
    private ArrayList<FileInfo> mDirRecord = new ArrayList<FileInfo>();
    private int mCurrentDir = -1;

    /*w23001- add the show folder button*/
    private String[] currentDirs;
    
    private final static int STATE_CHOOSE_DIRECTORY = 0;
    private final static int STATE_CHOOSE_FILE = 1;

    //Begin CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
    public static enum CHOOSE_FILE_ACTION_TYPE {
        ACTION_CHOOSE_FILE,
        ACTION_GET_CONTENT
    }
    private CHOOSE_FILE_ACTION_TYPE mActionType;
    //End

    private int mState = STATE_CHOOSE_DIRECTORY;

    //Begin CQG478 liyun, IKDOMINO-2325, scroll position 2011-9-5
    private boolean mReturned = false;
    private ArrayList<Util.ScrollPosition> mScrollList = new ArrayList<Util.ScrollPosition>();
    //End

    private static char SEPERATOR = '|';
    /** Called when the activity is first created. */

    // handle progress dialog when opening a folder.
    private ArrayList<String> mFilter;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private FileInfo mOpenDirFi;
    private boolean mOpenDirRecord;
    private int mFolderOperation;
    private Util.FilesContainer mFilesContainer=new Util.FilesContainer();
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Util.MSG_PROGRESS_OPENDIR_SHOW:
                if (mOpenDirFi.getChildCount() > Util.FOLDER_CHILD_COUNT_PROGRESS) {
                    mProgressDialog = new ProgressDialog(mContext);
                    switch (mFolderOperation) {
                    case Util.FOLDER_OPERATION_OPENDIR:
                        mProgressDialog.setMessage(getString(R.string.msg_opening_progress));
                        break;
                    case Util.FOLDER_OPERATION_REFRESH:
                        mProgressDialog.setMessage(getString(R.string.msg_refreshing_progress));
                        break;
                    }
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                }
                break;
            case Util.MSG_PROGRESS_OPENDIR_DISMISS:
                if (mOpenDirFi.getChildCount() > Util.FOLDER_CHILD_COUNT_PROGRESS) {
                    mProgressDialog.dismiss();
                }
                switch (mFolderOperation) {
                case Util.FOLDER_OPERATION_OPENDIR:
                    openDirUI(mOpenDirFi, mOpenDirRecord);
                    break;
                case Util.FOLDER_OPERATION_REFRESH:
                    refreshUI();
                    break;
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.choosefilename);
        
        mBar = getActionBar();
        mBar.setDisplayHomeAsUpEnabled(true);

        mContext = this;
        
        mList = (ListView) findViewById(R.id.list);
        mGrid = (AbsListView)findViewById(R.id.grid); //w23001-add the grid feature
        mEmpty = (TextView) findViewById(R.id.empty);

        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);

        mList.setOnItemClickListener(this);
        mGrid.setOnItemClickListener(this);

        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
        
        sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
        }
        
        VirtualFolder.setContext(mContext);
        mView = getView();//w23001-add the grid feature
        if(isEmmc()) {
        	isEmmc = true;
        	showSearchOptionDialog();       
        	return;
        } else {
        	mRoot = ID_EXTSDCARD;
        	openRoot(ID_EXTSDCARD);
        }
        return;
    }
    public void showFloderView(){
    	mReturned = true;
    	String[] parts;
    	String[] subParts;
        String currentpath = mAdapter.getCurrentFile().getRealPath();
        subParts =	currentpath.split("/"); 
        parts = new String[subParts.length - 2];
        for (int i = 0; i < parts.length; i ++) {
        	parts[i] = subParts[i + 2];
        }
        if(isEmmc) { 
            if (mRoot.equals(Util.ID_EXTSDCARD)) {
            	parts[0] = this.getString(R.string.removable_sdcard);
            } else {
            	parts[0] = this.getString(R.string.internal_storage);
            }
        } else {
        	parts[0] = this.getString(R.string.removable_sdcard);
        }
        currentDirs = new String[parts.length];
        //modify by amt_xulei for SWITCHUITWO-7
        //reason:CT JB testload SDcard and internal storage path
        // is /storage/sdcard0 and /storage/sdcard1
//        currentDirs[0] = "/mnt/" + subParts[2];
        currentDirs[0] = "/storage/" + subParts[2];
        //end modify
        for (int i = 1; i < currentDirs.length; i++) {                   
            currentDirs[i] = currentDirs[i - 1] + "/" + parts[i];
        }
        AlertDialog alert =
            createSingleChoiceDiag(R.string.select_folder, parts, parts.length - 1,
                folderViewDialogListener);
        alert.show();
    }   
    
    private void openRoot(String root){    	
    	VirtualFolder.setRoot(root);
    	
    	Intent intent = getIntent();
        String action = intent.getAction();        
        if (action != null) {
            if (action.equals("com.motorola.action.CHOOSE_FILE")) {
                mState = STATE_CHOOSE_FILE;
                mActionType = CHOOSE_FILE_ACTION_TYPE.ACTION_CHOOSE_FILE;
            } else if (action.equals("android.intent.action.GET_CONTENT")) {
                mState = STATE_CHOOSE_FILE;
                mActionType = CHOOSE_FILE_ACTION_TYPE.ACTION_GET_CONTENT;
            }
            //End 
            else if (action.equals("com.motorola.action.CHOOSE_DIRECTORY")) {
                mState = STATE_CHOOSE_DIRECTORY;
            }
        }
        
        if (mState == STATE_CHOOSE_FILE) {
            View v = (View) findViewById(R.id.buttonPanel);
            v.setVisibility(View.GONE);
        }
 
        mAdapter = new FileManagerAdapter(this);
        mAdapter.setResource(R.layout.listitem);
        mAdapter.setShowCountForFolder(false);

        mAdapter.setViewMode(FileManagerActivity.mViewMode);
        mAdapter.updateView();

        FileInfo fi = mAdapter.getRoot();
        if (mRoot == ID_EXTSDCARD){        	
        	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
				if (!isFinishing()) {
					if (sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_SHARED)){
                                AlertDialog dialog = (new AlertDialog.Builder(mContext))
						.setTitle(R.string.removable_sdcard)
						.setMessage(R.string.sd_is_used_by_other)
						.setNeutralButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
							    // @Override
							    public void onClick(
								    DialogInterface dialog,
								    int which) {
							    	finish();
							    }
							}).create();
						dialog.setCanceledOnTouchOutside(false);
				        //dialog.setCancelable(false); 
				        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
				            @Override
				            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

				                if (keyCode == KeyEvent.KEYCODE_BACK) {
				                	finish();
				                    return true;
				                }
				                return false;
				            }
				        });
					dialog.show();					

                     } else {
	                      AlertDialog dialog = (new AlertDialog.Builder(mContext))
							.setTitle(R.string.removable_sdcard)
							.setMessage(R.string.sdcard_not_exist)
							.setNeutralButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
								    // @Override
								    public void onClick(
									    DialogInterface dialog,
									    int which) {
								    	finish();
								    }
								}).create();
					dialog.setCanceledOnTouchOutside(false);
				        //dialog.setCancelable(false); 
				        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
				            @Override
				            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

				                if (keyCode == KeyEvent.KEYCODE_BACK) {
				                	finish();
				                    return true;
				                }
				                return false;
				            }
				        });
					dialog.show();		
		}
				}
				return;
			}
        } else {
        	if (!sm.getVolumeState(FileManager.getInternalSd(mContext)).equals(Environment.MEDIA_MOUNTED)) {
			    if (!isFinishing()) {
			    	AlertDialog dialog = (new AlertDialog.Builder(mContext))
	    					.setTitle(R.string.internal_storage)
	    					.setMessage(
	    						R.string.internal_phone_storage_unmounted)
	    					.setNeutralButton(android.R.string.ok,
	    						new DialogInterface.OnClickListener() {
	    						    // @Override
	    						    public void onClick(
	    							    DialogInterface dialog,
	    							    int which) {
	    						    	finish();
	    						    }
	    						}).create();
			    	dialog.setCanceledOnTouchOutside(false);
			        //dialog.setCancelable(false); 
			        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			            @Override
			            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

			                if (keyCode == KeyEvent.KEYCODE_BACK) {
			                	finish();
			                    return true;
			                }
			                return false;
			            }
			        });
	    			dialog.show();
			    }
			    return;
    		}
        }
        Uri uri = intent.getData();
        if (uri != null) {
            String path = Util.getPathFromFileUri(uri);
            if (path != null) {
                FileInfo file = FileInfo.make(path);
                if (file != null) {
                    fi = file;
                }
            }
        }
        if (mState == STATE_CHOOSE_FILE) {
            if (mActionType == CHOOSE_FILE_ACTION_TYPE.ACTION_GET_CONTENT) {
                if (null != intent.getType()) {
                    mFilter= new ArrayList<String>();
                    mFilter.add(intent.getType());
                }
            } else if (mActionType == CHOOSE_FILE_ACTION_TYPE.ACTION_CHOOSE_FILE) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    parseParam(bundle.getString("FILE_TYPES"));
                }
            }
        }
        View view = (View) findViewById(R.id.buttonPanel);
        if (mState == STATE_CHOOSE_FILE) {
        	view.setVisibility(View.GONE);
                if (mNewFolder != null){
        	    mNewFolder.setVisible(false);
                }
        } else {
            view.setVisibility(View.VISIBLE);
            if (mNewFolder != null){
                mNewFolder.setVisible(true);
            }
        }
        openDir(fi, true);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mNewFolder = menu.add(R.string.menu_new_folder);
        mNewFolder.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mNewFolder.setIcon(R.drawable.ic_menu_new);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        mNewFolder.setEnabled(true);
        mNewFolder.setVisible(true);
        if (mRoot == ID_EXTSDCARD){        	
        	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
        		mNewFolder.setVisible(false);
        	}
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	if (item.getItemId() == android.R.id.home) {
    		if (mRoot == ID_EXTSDCARD){        	
            	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            		finish();
            		return false;
    			}
            }   		
    		if (mAdapter == null || mAdapter.getCurrentFile() == null || Util.isPathOfSDCard(mAdapter.getCurrentFile().getRealPath(), mContext)) {
    			finish();
    		} else {
    			showFloderView();
    		}
        } else if (item == mNewFolder) {
        	if (mRoot == ID_EXTSDCARD){        	
            	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
    				if (!isFinishing()) {
    					if (sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_SHARED)) {
    						(new AlertDialog.Builder(mContext))
    						.setTitle(R.string.removable_sdcard)
    						.setMessage(R.string.sd_is_used_by_other)
    						.setNeutralButton(android.R.string.ok,
    							new DialogInterface.OnClickListener() {
    							    // @Override
    							    public void onClick(
    								    DialogInterface dialog,
    								    int which) {
    							    	finish();
    							    }
    							}).create().show();
    					} else {
	    					(new AlertDialog.Builder(mContext))
	    						.setTitle(R.string.removable_sdcard)
	    						.setMessage(R.string.sdcard_not_exist)
	    						.setNeutralButton(android.R.string.ok,
	    							new DialogInterface.OnClickListener() {
	    							    // @Override
	    							    public void onClick(
	    								    DialogInterface dialog,
	    								    int which) {
	    							    	finish();
	    							    }
	    							}).create().show();
    					}
    				}
    				return false;
    			}
            } 
            showDialog(DIALOG_NEW_FOLDER);
            return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NEW_FOLDER:
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.dialog_new_folder, null);
            final EditText et = (EditText) view.findViewById(R.id.edit_name);
            et.setText("");
            final CheckBox cb = (CheckBox) view.findViewById(R.id.cb_file);
            return new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_new).setTitle(
                    R.string.title_dlg_new).setView(view).setPositiveButton(android.R.string.ok,
                    new OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            FileInfo parent = mAdapter.getCurrentFile();
                            String name = et.getText().toString();
                            if (name == null || name.length() == 0) {
                                Toast.makeText(ChooseFileNameActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            name = Util.removeSpace(name);

                            if (name == null) {
                                Toast.makeText(ChooseFileNameActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //Begin CQG478 liyun, IKDOMINO-1814 check filename lenght 2011-9-14
                            /*if (Util.exceedFileNameMaxLen(name)) {
                                Toast.makeText(
                                        ChooseFileNameActivity.this,
                                        ChooseFileNameActivity.this.getString(
                                                R.string.filename_too_long, name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }*/
                            //End

                            if (!Util.isValidateFileName(name)) {
                                Toast.makeText(
                                        ChooseFileNameActivity.this,
                                        ChooseFileNameActivity.this.getString(
                                                R.string.filename_invalidate, name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String path = parent.getRealPath() + FileInfo.SEPERATOR + name;
                            if (Util.isExistFolder(path)) {
                                Toast.makeText(
                                        ChooseFileNameActivity.this,
                                        ChooseFileNameActivity.this.getString(
                                                R.string.folder_existed, name), Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            FileInfo fi = mAdapter.getFileSystem().createFolderOrFile(parent, path);

                            int prompt;
                            if (cb.isChecked()) {
                                if (fi != null)
                                    prompt = R.string.success_create_file;
                                else
                                    prompt = R.string.fail_create_file;
                            } else {
                                if (fi != null)
                                    prompt = R.string.success_create_folder;
                                else
                                    prompt = R.string.fail_create_folder;
                            }
                            Toast.makeText(ChooseFileNameActivity.this,
                                    ChooseFileNameActivity.this.getString(prompt, name),
                                    Toast.LENGTH_SHORT).show();
                            if (fi != null) {
                                if (!cb.isChecked()) {
                                    openDir(fi, true);
                                } else {
                                    refresh();
                                }
                            }
                        }

                    }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Cancel should not do anything.
                }

            }).create();

        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
        case DIALOG_NEW_FOLDER:
            EditText et = (EditText) dialog.findViewById(R.id.edit_name);
            et.setText("");
            break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileInfo fi = (FileInfo) mAdapter.getItem(position);
        if (fi != null) {
            if (fi.isDirectory()) {
                openDir(fi, true);
            } else if (!fi.isVirtualFolder() && mState == STATE_CHOOSE_FILE) {
                String path = fi.getRealPath();
                if (path != null) {
                    Intent intent = getIntent();
                    Uri uri = null;
                    File hideFile = new File(mAdapter.getCurrentFile().getRealPath());
                    //Begin CQG478 liyun, IKDOMINO-4474, change Uri format 2011-11-18
                    if (mActionType == CHOOSE_FILE_ACTION_TYPE.ACTION_GET_CONTENT) {
                    	if (!hideFile.isHidden()) {
                		if (String.format(getString(R.string.with_drm)).equals("yes") && Util.getExtension(path).equals("dcf")) {
                			DrmManagerClient drmClient = new DrmManagerClient(mContext);
                			String drmType = drmClient.getOriginalMimeType(path);   
                			Log.d("", "cxxlyl-- drm mimetype = " + drmType);
                			uri = Util.getMediaContentUriByFilePath(this, MimeType.getMimeId(drmType), path);
                		} else {
                			uri = Util.getMediaContentUriByFilePath(this, fi.getFileMimeId(), path);
                		}
                        }
                        if (uri == null) {
                            uri = Uri.fromFile(new File(path));
                        }  
                    } else {
                        //Begin CQG478 liyun, IKDOMINO-3397, file encode 2011-10-25
                        //Uri uri = Util.getFileUriFromPath(path);
                        uri = Uri.fromFile(new File(path));
                        //End
                    }
                    //End

                    //Begin kbg374, IKDOMINO-5253, send crop intent when "crop" is not null, 2011-12-16
                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.getString("crop")!= null
                            && extras.getString("crop").equals("true")) {
                        try {
                    		startActivityForResult(getCropImageIntent(uri), REQUEST_TYPE_CROP_IMAGE);
                    	} catch (ActivityNotFoundException e) {
                            Toast.makeText(this, R.string.application_not_available, Toast.LENGTH_LONG).show();
                            return;
                    	}
                    } else {
                        intent.setData(uri);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                    //End
                } else {
                    Toast.makeText(this, R.string.need_choose_file, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //Begin kbg374, IKDOMINO-5253, send crop intent when "crop" is not null, 2011-12-16
    private Intent getCropImageIntent(Uri photoUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");

        Bundle extras = getIntent().getExtras();

        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", extras.getString("crop"));
        intent.putExtra("aspectX", extras.getInt("aspectX"));
        intent.putExtra("aspectY", extras.getInt("aspectY"));
        intent.putExtra("outputX", extras.getInt("outputX"));
        intent.putExtra("outputY", extras.getInt("outputY"));
        intent.putExtra("return-data", extras.getBoolean("return-data"));
        intent.putExtra("noFaceDetection", extras.getBoolean("noFaceDetection"));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, extras.getParcelable(MediaStore.EXTRA_OUTPUT));
        intent.putExtra("outputFormat", extras.getString("outputFormat"));

        return intent;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
        case REQUEST_TYPE_CROP_IMAGE: {
            if (data == null) {
                return;
            }
            setResult(RESULT_OK, data);
            finish();
            break;
        }
        default:
            break;
        }
    }
    //End

    public void onClick(View v) {

        if (v == mBtnOk) {
        	if (mRoot == ID_EXTSDCARD){        	
            	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            		finish();
            		return;
    			}
            }  
            if (mState == STATE_CHOOSE_DIRECTORY) {
                String path = mAdapter.getCurrentFile().getRealPath();
                if (path != null) {
                    Intent intent = getIntent();

                    Uri uri = Util.getFileUriFromPath(path);
                    intent.setData(uri);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(this, R.string.cannot_choose_vfolder, Toast.LENGTH_SHORT).show();
                }
            }

        } else if (v == mBtnCancel) {
            Intent intent = getIntent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    protected void refresh() {
        mFolderOperation = Util.FOLDER_OPERATION_REFRESH;
        mOpenDirFi = mAdapter.getCurrentFile();
        
        if (mOpenDirFi!=null && mOpenDirFi.isDirectory()){
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_SHOW);
            Util.GetChildrenThread thread=new Util.GetChildrenThread(mHandler,mFilesContainer,mOpenDirFi,mFilter,Util.SORT_BY_NONE,true,false);
            //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
            thread.setFiltType(this, mActionType);
            //End
            thread.start();
            }
        
    }

    private void refreshUI() {
    	//mAdapter.setFiles(mFilesContainer.getUnHideFiles());
	    mAdapter.setFiles(mFilesContainer.getFiles());

        //Begin CQG478 liyun, IKDOMINO-1786, return to original position, 2011-8-12
        //mList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        //End

        if (mAdapter.getCount() == 0) {
        	mView.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        } else {
            mEmpty.setVisibility(View.GONE);
            mView.setVisibility(View.VISIBLE);
            mView.requestFocus();
        }
    }
    //modify by amt_xulei for SWITCHUITWO-356
    //reason:this method is only executed once
    @Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
        	if (mRoot == ID_EXTSDCARD){        	
            	if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            		finish();
    	            return false;
    			}
            }
            mReturned = true;
            if (mAdapter == null || mAdapter.getCurrentFile() == null || mAdapter.getCurrentFile().isRoot()) {
            	finish();
            	return true;
            } else {
            	FileInfo fi = mAdapter.getCurrentFile();
                if (fi != null) {
                    FileInfo fiParent = fi.getParent();
                    if (fiParent != null) {
                        openDir(fiParent, true);
                    }
                }
            }

            return true;
        }
		return super.onKeyUp(keyCode, event);
	}
    //end modify
    
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode ==  KeyEvent.KEYCODE_MENU) {
    		Log.d("","w23001- okKeyDown"); 
    		if(mState == STATE_CHOOSE_FILE){
    			return true;
    		}
    	}
        return super.onKeyDown(keyCode, event);
    }

    private void openDir(FileInfo fi, boolean record) {

        if (fi == null || !fi.isDirectory() && !fi.isVirtualFolder()) {
            openDir(mAdapter.getRoot(), true);
            return;
        }

        //Begin CQG478 liyun, IKDOMINO-2325, scroll position 2011-9-5
        int arrSize = mScrollList.size();
        int curDepth = Util.getCurrentDepth(mOpenDirFi);
        View firtView = mView.getChildAt(0);      

        if (!mReturned) {
            if ( arrSize > curDepth) {
                ((Util.ScrollPosition)mScrollList.get(curDepth)).index = mView.getFirstVisiblePosition();
                ((Util.ScrollPosition)mScrollList.get(curDepth)).top = (firtView == null) ? 0 : firtView.getTop(); 
            } else {
                Util.ScrollPosition scrollPos = new Util.ScrollPosition();
                scrollPos.index = mView.getFirstVisiblePosition();
                scrollPos.top = (firtView == null) ? 0 : firtView.getTop(); 
                mScrollList.add(scrollPos);     
            }
        }
        //End

        if (!fi.isVirtualFolder()) {
            //Begin CQG478 liyun, IKDOMINO-2066, sdcard share 2011-8-24
            if (Util.isShareSDCard()) {
                Toast.makeText(this, R.string.sdcard_is_shared, Toast.LENGTH_SHORT).show();
                finish();
                return;
            //End
            } else if (Util.fileOnSDCard(fi.getRealPath()) && !Util.isExistSDCard()) {
                Toast.makeText(this, R.string.sdcard_not_exist, Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else if (!Util.isExistFolder(fi.getRealPath())) {
                Toast.makeText(this, getString(R.string.folder_not_exist, fi.getTitle()),
                        Toast.LENGTH_LONG).show();
                fi = mAdapter.getRoot();
            }
        }

        mOpenDirFi = fi;
        mOpenDirRecord = record;

        if (mOpenDirFi != null && mOpenDirFi.isDirectory()) {
            mFolderOperation = Util.FOLDER_OPERATION_OPENDIR;
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_SHOW);
            mAdapter.setCurrentFile(mOpenDirFi);
            
            Util.GetChildrenThread thread=new Util.GetChildrenThread(mHandler,mFilesContainer,mOpenDirFi,mFilter,Util.SORT_BY_NAME,true,false);
            //CQG478 liyun, IKDOMINO-3368, GET_CONTENT interface 2011-10-24
            thread.setFiltType(this, mActionType);
            //End
            thread.start();
            
        }

    }

    private void openDirUI(FileInfo fi, boolean record) {
    	//mAdapter.setFiles(mFilesContainer.getUnHideFiles());
        mAdapter.setFiles(mFilesContainer.getFiles());      
        mAdapter.notifyDataSetChanged();
        mView.setAdapter(mAdapter);
       
        //Begin CQG478 liyun, IKDOMINO-2325, scroll position 2011-9-5
        if (mReturned) {                    
            mReturned = false;
                    
            if (mScrollList.size() > Util.getCurrentDepth(mOpenDirFi)) {
                Util.ScrollPosition scrollPos = mScrollList.get(Util.getCurrentDepth(mOpenDirFi));
                if (FileManagerActivity.mViewMode == Util.VIEW_LIST) {
                	((ListView) mView).setSelectionFromTop(scrollPos.index, scrollPos.top);
                }
            }
        }
        //End
        if (record) {
            int size = mDirRecord.size();

            if (size > 0 && mCurrentDir < size - 1) {
                for (int i = size - 1; i >= mCurrentDir + 1; i--)
                    mDirRecord.remove(i);
            }
            mDirRecord.add(fi);
            mCurrentDir = mDirRecord.size() - 1;
        }

        if (mAdapter.getCount() == 0) {
        	mView.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        } else {
            mEmpty.setVisibility(View.GONE);
            mView.setVisibility(View.VISIBLE);
            mView.requestFocus();
        }

        if (mState == STATE_CHOOSE_FILE) {
                if (mNewFolder != null ){
        	    mNewFolder.setVisible(false);
                 }
        } else {
                if (mNewFolder != null ){
        	    mNewFolder.setVisible(true);
                }
        } 

        if (!Util.isPathOfSDCard(fi.getRealPath(), mContext)) {        	
        	mBar.setIcon(R.drawable.ic_folder_open);
        	mBar.setTitle(fi.getTitle());
        } else {
    	    if (mRoot.equals(Util.ID_EXTSDCARD)){
    	    	mBar.setIcon(R.drawable.ic_sdcard);
    	    	mBar.setTitle(R.string.location_sdcard);
    	    } else {
    	    	mBar.setIcon(R.drawable.ic_thb_device);
    	    	mBar.setTitle(R.string.internal_storage);
    	    }
        }
    }

    private void parseParam(String param) {

        mFilter= new ArrayList<String>();
        String types = param;

        while (types != null) {
            int index = types.indexOf(SEPERATOR);

            String type;
            if (index != -1) {
                type = types.substring(0, index);
                types = types.substring(index + 1, types.length());
            } else {
                type = types;
                types = null;
            }
            if (isInvalidateType(type)) {
                mFilter.add(type);
            }
        }
    }

    private boolean isInvalidateType(String type) {
        return (type != null && type.length() > 2 && type.startsWith("*."));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    private DialogInterface.OnClickListener folderViewDialogListener =
  	      new DialogInterface.OnClickListener() {
  	        public void onClick(DialogInterface dialog, int item) {
  	          openDir(FileInfo.make(currentDirs[item]), false);
  	          dialog.dismiss();
  	        }
  	   };
  	   private AlertDialog createSingleChoiceDiag(int titleId, CharSequence[] items, int checkedItem,
  	           DialogInterface.OnClickListener listener) {
  			AlertDialog dialog = null;
  			AlertDialog.Builder builder = new AlertDialog.Builder(this);
  			if (titleId > 0) {
  				builder.setTitle(titleId);
  			}
  			builder.setSingleChoiceItems(items, checkedItem, listener);
  			builder.setNeutralButton(R.string.cancle, new DialogInterface.OnClickListener() {
  				public void onClick(DialogInterface dialog, int which) {
  					dialog.dismiss();
  				}
  			});
  			dialog = builder.create();
  			return dialog;
  		}
  	   
  	    private AlertDialog createSearchChoiceDialog(int titleId, CharSequence[] items, int checkedItem,
  	            DialogInterface.OnClickListener listener) {
                        AlertDialog dialog;
	  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	  		if (titleId > 0) {
	  			builder.setTitle(titleId);
	  		}
	  		builder.setSingleChoiceItems(items, checkedItem, listener);
	  		builder.setNeutralButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
	  		public void onClick(DialogInterface dialog, int which) {
	  		    String root = null;
		  		if (FileManager.getSearchOption() == FileManager.INTERNALSTORAGE) {
		  			root = Util.ID_LOCAL;
		  		} else {
		  			root = Util.ID_EXTSDCARD;
		  		}
		  		VirtualFolder.setRoot(root);
	        	FileSystem.setRoot();
		  		mRoot = root;
		  		openRoot(root);
		  		dialog.dismiss();
	  		}
	  		});
	  		dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(false);
                        //dialog.setCancelable(false);
                        
                        //modify by amt_xulei for SWITCHUITWO-356
                        //reason:this method is only executed once
//                        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//                             @Override
//                             public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//
//                                 if (keyCode == KeyEvent.KEYCODE_BACK) {
//                    	             finish();
//                                     return true;
//                                  }
//                                  return false;
//                             }
//                        }); 
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
							
							@Override
							public void onCancel(DialogInterface dialog) {
								finish();
							}
						});
                        //end modify
	  		return dialog;
  	    }
  	      
  	    private DialogInterface.OnClickListener searchOptionDialogListener =
  	    	      new DialogInterface.OnClickListener() {
  	    	        public void onClick(DialogInterface dialog, int item) {
  	    	          FileManager.setmSearchOption(item);
  	    	        }
  	    	      };
  	    
  	    private void showSearchOptionDialog() {
  	        String[] parts = getResources().getStringArray(R.array.search_storage_selector);
  	        AlertDialog dialog =
  	            createSearchChoiceDialog(R.string.menu_multi_choice, parts, FileManager.getSearchOption(),
  	                searchOptionDialogListener);
  	        dialog.show();
  	    }
  	    
  	  public static boolean isEmmc() {      	
			StorageVolume[] volumes = sm.getVolumeList();
			Log.d("","w23001- volume lenght =" + volumes.length);
			if (volumes.length > 1) {
					return true;
			} 
			return false;
      }
  	  
  	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())){
            	Log.d("","w23001- sd is unmounted");
            	finish();
            }
        }
    };
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final Bundle b = savedInstanceState.getBundle(SAVED_DIALOGS_TAG);
        if (null != b) 
            savedInstanceState.remove(SAVED_DIALOGS_TAG);        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
