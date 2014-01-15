package com.motorola.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SearchRecentSuggestionsProvider;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.util.Log;

import java.lang.String;

import com.motorola.FileManager.R;
import com.motorola.FileManager.utils.IconifiedText;
import com.motorola.FileManager.storage.*;

public class FileManagerActivity extends Activity implements AdapterView.OnItemClickListener,
        View.OnClickListener, Dialog.OnCancelListener, Dialog.OnDismissListener {

    private static final int DIALOG_NEW_FOLDER = 1;
    private static final int DIALOG_DELETE = 2;
    private static final int DIALOG_RENAME = 3;
    private static final int DIALOG_FOLDER_PROPERTY = 4;
    private static final int DIALOG_FILE_PROPERTY = 5;
    private static final int DIALOG_SDCARD_PROPERTY = 6;
    private static final int DIALOG_SORT = 7;
   // private static final int DIALOG_HIDEUNHIDE = 10;

    private static final int MENU_COPY = Menu.FIRST + 1;
    private static final int MENU_MOVE = Menu.FIRST + 2;
    private static final int MENU_OPEN = Menu.FIRST + 3;
    private static final int MENU_DELETE = Menu.FIRST + 4;
    private static final int MENU_RENAME = Menu.FIRST + 5;
    private static final int MENU_DETAIL = Menu.FIRST + 6;
    private static final int MENU_SHARE = Menu.FIRST + 7;
    private static final int MENU_SET_AS = Menu.FIRST + 8;
    private static final int MENU_SET_AS_CALL_RINGTONE = Menu.FIRST + 9;
    private static final int MENU_SET_AS_MESSAGE_RINGTONE = Menu.FIRST + 10;
    //private static final int MENU_HIDE = Menu.FIRST + 11;
    private static final int MENU_UPDATE = Menu.FIRST + 12;

    private static final int MENU_NEW_FOLDER = R.id.MENU_NEW_FOLDER;
    private static final int MENU_MULTI_SELECT = R.id.MENU_MULTI_SELECT;
    private static final int MENU_DELETE_CHOICE = R.id.MENU_DELETE_CHOICE;
    private static final int MENU_MOVE_CHOICE = R.id.MENU_MOVE_CHOICE;
    private static final int MENU_COPY_CHOICE = R.id.MENU_COPY_CHOICE;
    private static final int MENU_SORTING = R.id.MENU_SORTING;
    private static final int MENU_PROPERTY = R.id.MENU_PROPERTY;
    private static final int MENU_VIEW_SWITCH = R.id.MENU_VIEW_SWITCH;
    private static final int MENU_LIST_VIEW = R.id.MENU_LIST_VIEW;
    private static final int MENU_GRID_VIEW = R.id.MENU_GRID_VIEW;
    private static final int MENU_SEARCH = R.id.MENU_SEARCH;
  //  private static final int MENU_HIDEUNHIDE = R.id.MENU_HIDEUNHIDE;
    
    private short DRM_NO_CONSTRAINT = 0x80;    /**< Indicate have no constraint(unlimited constraint), it can use freely */
    private short DRM_END_TIME_CONSTRAINT = 0x08;    /**< Indicate have end time constraint */
    private short DRM_INTERVAL_CONSTRAINT = 0x04;    /**< Indicate have interval constraint */
    private short DRM_COUNT_CONSTRAINT = 0x02;    /**< Indicate have count constraint */
    private short DRM_START_TIME_CONSTRAINT = 0x01;    /**< Indicate have start time constraint */
    private short DRM_NO_PERMISSION = 0x00;    /**< Indicate no rights */

    static final public int MESSAGE_SET_LOADING_PROGRESS = 510;
    static final public int  MESSAGE_SHOW_DIRECTORY_CONTENTS = 511;
	static final public int NEED_REFRESH_FOLDER = 512;
    static final public int SEARCHBOX_FLAG = 268435456;
    public static final int NAMESORT = 300;
    public static final int TYPESORT = 301;
    public static final int TIMESORT = 302;
    public static final int SIZESORT = 303;
    static public class DirectoryContents {
        List<IconifiedText> listDir;
        List<IconifiedText> listFile;
      }
    
    /*w23001- add the search feature*/ 
    public static boolean isSearch = false;
    private String m_FirstSearchedDirectory = "";
    private boolean mSearchCreated = false;
    private boolean mCreatedFromSearchSuggestion = false;
    private String searchedString ="";
    private static final String SEARCHED_DIRECTORY = "searched_directory";
    private List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
    private List<IconifiedText> mListFile = new ArrayList<IconifiedText>();
    private SearchDirectory mSearchDirectory = null;
    private ProgressDialog loadingDialog = null;
    private boolean openedBySearch = false;
    
    /*w23001- add the show folder button*/
    private String[] currentDirs;
    
    public static String mCurrentContent = Util.ID_EXTSDCARD;
    public static String sSDcardTitle = null;
    public static String sInternalSDcardTitle = null;
    public static StorageManager sm;
    
    public static FileManagerAdapter mAdapter;

    private ListView mList;
    private AbsListView mGrid;
    private TextView mEmpty;

    private MenuItem mNewItem;
    private MenuItem mSearchItem;
    private MenuItem mViewItem;
    private MenuItem mSortItem;
    private MenuItem mCopyItem;
    private MenuItem mMoveItem;
    private MenuItem mProItem;
    private MenuItem mDelItem;
    private MenuItem mMultSelect;
    //private MenuItem mHide;

    private ActionBar mActionbar;
    public static int mViewMode = Util.VIEW_LIST;

    private boolean mMultiChoiceMode = false;
    private static final String SAVED_DIALOGS_TAG = "android:savedDialogs";

    private ArrayList<FileInfo> mDirRecord = new ArrayList<FileInfo>();
    private int mCurrentDir = -1;

    private FileInfo mCurrentFile;
    private PerformDialog mPerformDlg;

    private boolean mNeedScanDisk=false;
    private boolean mMediaMountFromMe=false;
    private Locale mLocale;
 
    //Begin CQG478 liyun, IKDOMINO-2325, scroll position 2011-9-5
    private boolean mReturned = false;
    private ArrayList<Util.ScrollPosition> mScrollList = new ArrayList<Util.ScrollPosition>();
    //End

    private Context mContext;
    private DrmManagerClient drmClient;
    public static boolean isDrm = false;
    
    private boolean needRefresh = true;
    
    private static final String UPDATE_PACKAGE_PREFIX = "update";
    public static final String ACTION_INSTALL_UPDATE_PACKAGE = "com.motorola.internal.intent.action.INSTALL_UPDATE_PACKAGE";
    public static boolean isCT = false;
    // handle progress dialog when opening a folder.
    private ProgressDialog mProgressDialog;
    private FileInfo mOpenDirFi;
    private Util.FilesContainer mFilesContainer = new Util.FilesContainer();
    private boolean mOpenDirRecord;
    private int mFolderOperation;
    //modify by amt_xulei for SWITCHUITWOV-294 at 2012-11-5
    //reason:default sort type is same as Gallery
    private int mSortMethod = Util.SORT_BY_DATE;
    private boolean mAscending = false;
    //end modify by amt_xulei for SWITCHUITWOV-294 at 2012-11-5
	public static String mainCard = "";//"sdcard"-外置SD卡为主卡,"external1"-内置SD卡为主卡
	
	//modify by amt_xulei for SWITCHUITWO-425
	//reason:handler onKeyUp after onKeyDown,otherwise don't handler it.
	public boolean isBackKeyPressDown = false;
	//end modify
    
	private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SET_LOADING_PROGRESS :
                showLoadingDialog(getString(R.string.loading));
                break;
            case MESSAGE_SHOW_DIRECTORY_CONTENTS :
            	hideLoadingDialog();
                if (msg.obj != null) {
                  showDirectoryContents((DirectoryContents) msg.obj);
                }
                break;
            case Util.MSG_PROGRESS_OPENDIR_SHOW:
                if (mOpenDirFi.getChildCount() > Util.FOLDER_CHILD_COUNT_PROGRESS) {
                    if (mProgressDialog == null) {
                        mProgressDialog = new ProgressDialog(mContext){
                        	@Override
                            public boolean onTouchEvent(MotionEvent event) {
                            	return true;
                            }
                        };
                    } else {                        
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
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
                //add by amt_xulei for SWITCHUITWO-243
                //reason:need refresh option menu after getChildCount and setFiles to adapter
                refreshOptionMenu();
                //end add
                break;
            case Util.MSG_PROGRESS_SORT_SHOW:

                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage(getString(R.string.msg_sorting_progress));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                break;
            case Util.MSG_PROGRESS_SORT_DISMISS:
                mProgressDialog.dismiss();
                mAdapter.setFiles(mFilesContainer.getFiles());
                mAdapter.notifyDataSetChanged();
		//add by amt_xulei for SWITCHUITWOV-121 2012-9-7
		mAdapter.setViewMode(mViewMode);
                mAdapter.updateView();
		//end add by amt_xulei
                getView().setAdapter(mAdapter);               
                break;
		case NEED_REFRESH_FOLDER:
            		needRefresh = true;
            		refresh();
            		break;
            }
        }
    };
   
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentFile != null) {
        	outState.putString("currentfile", mCurrentFile.getRealPath());
        }
	//add by amt_xulei for SWITCHUITWOV-76 2012-9-10
	//reason:need save last folder path
	if (mOpenDirFi != null){
        	outState.putString("mOpenDirFi", mOpenDirFi.getRealPath());
        }
        outState.putString("mCurrentContent", mCurrentContent);
	//end add
	//add by amt_xulei for SWITCHUITWOV-121 2012-9-7
	outState.putInt("mViewMode",mViewMode);
	//end add
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	//add by amt_xulei for SWITCHUITWOV-76 2012-9-10
	//reason:need read last folder path
	if (savedInstanceState != null){
        	mCurrentContent = savedInstanceState.getString("mCurrentContent");
        }
        else
        {
        	mCurrentContent = FileManager.mCurrentContent;
        }
	//mCurrentContent = FileManager.mCurrentContent;
	//end add        

        setContentView(R.layout.main);
        
        mActionbar = getActionBar();
        if (mActionbar != null) {
            mActionbar.setDisplayHomeAsUpEnabled(true);
        }
        if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
	    	mActionbar.setIcon(R.drawable.ic_sdcard);
	    	mActionbar.setTitle(R.string.location_sdcard);
	    } else {
	    	mActionbar.setIcon(R.drawable.ic_thb_device);
	    	mActionbar.setTitle(R.string.internal_storage);
	    }
        VirtualFolder.setRoot(mCurrentContent);
        FileSystem.setRoot();
        if (String.format(getString(R.string.with_drm)).equals("yes")) {
        	isDrm = true;
        }else {
            isDrm = false;
        }
        mContext = this;
        
        if(FileManager.isEmmc(mContext)) {
        	FileManager.iEmmc = true;
        }
        
        sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d("","w23001-storage error -1: use default path =" + Environment.getExternalStorageDirectory().toString());
        }
        
        VirtualFolder.setContext(mContext);
        mViewMode = Util.VIEW_LIST;
	//add by amt_xulei for SWITCHUITWOV-121 2012-9-7
	if(savedInstanceState != null){
            mViewMode = savedInstanceState.getInt("mViewMode");
        }
	//end add

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
	//add by amt_xulei for SWITCHUITWOV-99 2012-9-7
	//reason:after rename a file,media will scan files.Refresh list after scan finished
	intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
	//end add by amt_xulei        
	intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
        
        mAdapter = new FileManagerAdapter(this);

        mList = (ListView) findViewById(R.id.list);
        mGrid = (AbsListView)findViewById(R.id.grid);
        mEmpty = (TextView) findViewById(R.id.empty);


        mList.setOnItemClickListener(this);
        mList.setOnCreateContextMenuListener(this);
        
        mGrid.setOnItemClickListener(this);
        mGrid.setOnCreateContextMenuListener(this);

        isCT = Util.isCTModel();

	    if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
	    	mActionbar.setIcon(R.drawable.ic_sdcard);
	    	mActionbar.setTitle(R.string.location_sdcard);
	    } else {
	    	mActionbar.setIcon(R.drawable.ic_thb_device);
	    	mActionbar.setTitle(R.string.internal_storage);
	    }

        mNeedScanDisk = false;
        mLocale = getResources().getConfiguration().locale;
        
        sSDcardTitle = getString(R.string.removable_sdcard);
        sInternalSDcardTitle = getString(R.string.internal_storage);
        
        SearchManager searchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        searchManager.setOnDismissListener(searchDismissListener);
       /* if (isDrm) {
        	drmClient = new DrmManagerClient(mContext);
        }*/
        
        FileInfo fi = mAdapter.getRoot(); 
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            isSearch = true;
            mSearchCreated = true;
            searchedString = intent.getStringExtra(SearchManager.QUERY);  
            if (FileManager.iEmmc) {
            	showSearchOptionDialog(); 
            } else {  
                m_FirstSearchedDirectory = FileSystem.Internal_SDCARD_DIR;
                startSearchDirectory(new File(m_FirstSearchedDirectory), searchedString);
            }
            return;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
        	//mCurrentContent = FileManager.ID_EXTSDCARD;
            Uri uri = getIntent().getData();
            if (uri == null) {
              finish();
              return;
            }
            Cursor cursor = managedQuery(uri, null, null, null, null);
            if (cursor == null) {
              finish();
            } else {
              cursor.moveToFirst();
              int dIndex = cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FULL_PATH);
              mCreatedFromSearchSuggestion = true;           
              File browseto = new File(cursor.getString(dIndex));
              if(browseto.getAbsolutePath().contains(FileManager.getInternalSd(mContext))){
            	  mCurrentContent = Util.ID_LOCAL;    	
                  VirtualFolder.setRoot(mCurrentContent);
                  FileSystem.setRoot();
              } else {
            	  mCurrentContent = Util.ID_EXTSDCARD;    	
                  VirtualFolder.setRoot(mCurrentContent);
                  FileSystem.setRoot();
              }
              if (browseto.isFile()) {
                openFile(this, cursor.getString(dIndex));
                finish();
              } else {
            	   openedBySearch = true;
                   openFileOrFolder(FileInfo.make(cursor.getString(dIndex)));
              }
            }
            return;
        }
                      
        Uri uri = intent.getData();
        if (uri != null) {
            String path = Util.getPathFromFileUri(uri);
            if (path != null) {
                FileInfo file = FileInfo.make(path);
                if (file != null) {
                    fi = file;
                    mSortMethod = Util.SORT_BY_DATE;
                    mAscending = false;
                }
            }

        }
       	//modify by amt_xulei for SWITCHUITWOV-76 2012-9-10
	//reason:need open last saved folder
        if(savedInstanceState != null){
            	//String cF = savedInstanceState.getString("currentfile");
            	//if(cF != null){
            	//    mCurrentFile = FileInfo.make(cF);
            	//}
		String cF = savedInstanceState.getString("mOpenDirFi");
            	Log.d("", "savedInstanceState cF = " + cF);
            	if(cF != null){
                	mCurrentFile = FileInfo.make(cF);
                	Log.d("","mCurrentFile="+mCurrentFile);
                	openDir(mCurrentFile,true);
            	}
        }
	else{
		needRefresh = false;
        	openDir(fi, true);
	}
	//end modify
    }

    private SearchManager.OnDismissListener searchDismissListener =
  	      new SearchManager.OnDismissListener() {
  	        //@Override
  	        public void onDismiss() {
  	        }
  	      };
  
  @Override
  protected void onNewIntent(Intent intent) {
	  super.onNewIntent(intent);
  	  if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
          searchedString = intent.getStringExtra(SearchManager.QUERY);
          isSearch = true;
          Log.d("","onNewIntent- action flags = "+ intent.getFlags() + " component = " + intent.getComponent());
          if (intent.getFlags() == SEARCHBOX_FLAG) {
        	  m_FirstSearchedDirectory = mAdapter.getCurrentFile().getRealPath();
          } else {
        	  if (FileManager.iEmmc) {
                   showSearchOptionDialog(); 
              	   return;
              } else {  
                   m_FirstSearchedDirectory = FileSystem.Internal_SDCARD_DIR;
              }
          }
          startSearchDirectory((new File(m_FirstSearchedDirectory)), searchedString);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      	  Uri uri=null;
          uri = intent.getData();
          if (uri == null){
              finish();
              return;
          }
          Cursor cursor = managedQuery(uri, null, null, null, null);
          if (cursor == null) {
              finish();
          } else {
              cursor.moveToFirst();
              int dIndex = cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FULL_PATH);
              File browseto = new File(cursor.getString(dIndex));
              //modify by amt_xulei for SWITCHUITWO-236
              //reason:no need to change FileSystem's root when opening the file from touching
              //auto search result.Because automatic search includes internal storage and SD card.
              if (browseto.isFile()) {
                //new OpenFileTask().execute(browseto);
                openFile(this, cursor.getString(dIndex));
              } else {
            	  if(browseto.getAbsolutePath().contains(FileManager.getInternalSd(mContext))){
            		  mCurrentContent = Util.ID_LOCAL;    	
            		  VirtualFolder.setRoot(mCurrentContent);
            		  FileSystem.setRoot();
            	  } else {
            		  mCurrentContent = Util.ID_EXTSDCARD;    	
            		  VirtualFolder.setRoot(mCurrentContent);
            		  FileSystem.setRoot();
            	  }
                   openFileOrFolder(FileInfo.make(cursor.getString(dIndex)));
              }
              //end modify
              
              //cursor.close();
          }
      }  	
  	  return;
  }
    
    @Override
    public void onStop() {
        
        if (mNeedScanDisk) {
            mMediaMountFromMe=true;
            Util.updateMediaProvider(this);
        }
        super.onStop();

    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        drmClient = null;
        stopSearchThread();
        Util.stopChildrenThread();
        if (mPerformDlg != null){
        	if (mPerformDlg.isShowing()) {
				mPerformDlg.cancel();
			}
        }
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final Bundle b = savedInstanceState.getBundle(SAVED_DIALOGS_TAG);
        if (null != b) 
            savedInstanceState.remove(SAVED_DIALOGS_TAG);        
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        
        mNewItem = menu.findItem(MENU_NEW_FOLDER);
        mSearchItem = menu.findItem(MENU_SEARCH);
        mViewItem = menu.findItem(MENU_VIEW_SWITCH);
        mSortItem = menu.findItem(MENU_SORTING);
        mCopyItem = menu.findItem(MENU_COPY_CHOICE);
        mMoveItem = menu.findItem(MENU_MOVE_CHOICE);
        mProItem = menu.findItem(MENU_PROPERTY);
        mDelItem = menu.findItem(MENU_DELETE_CHOICE);
        mMultSelect = menu.findItem(MENU_MULTI_SELECT);
       // mHide = menu.findItem(MENU_HIDEUNHIDE);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	Log.d("","w23001-onPrepareOptionsMenu enter ");
        super.onPrepareOptionsMenu(menu);
        //mNewItem.setIcon(android.R.drawable.ic_menu_add);
        //mDelItem.setIcon(android.R.drawable.ic_menu_delete);
        //mMoveItem.setIcon(android.R.drawable.ic_menu_send);
        //mSortItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        //mSearchItem.setIcon(android.R.drawable.ic_menu_search);
        mProItem.setIcon(android.R.drawable.ic_menu_info_details);
        int nCount = mAdapter.getCount();

        FileInfo fi = mAdapter.getCurrentFile();
        boolean operate = (fi != null && fi.canOperate());
        mNewItem.setEnabled(operate);
        mSortItem.setEnabled(operate);
        mMultSelect.setEnabled(operate && nCount > 0);
        mProItem.setEnabled(operate);
        mViewItem.setEnabled(operate);
        mSearchItem.setEnabled(operate);        
       // mHide.setEnabled(operate && nCount > 0);
        return true;
    }
    
    /**
     * need refresh option menu after getChildCount and setFiles to adapter
     */
    private void refreshOptionMenu(){
    	Log.d("", "refreshOptionMenu");
    	if (mMultSelect != null){
    		int nCount = mAdapter.getCount();
    		FileInfo fi = mAdapter.getCurrentFile();
    		boolean operate = (fi != null && fi.canOperate());
    		boolean enable = operate && nCount > 0;
    		mMultSelect.setEnabled(enable);
    	}
    }

	private void hideMenuOfActionBar(){
        if (mNewItem == null || mSearchItem == null || mViewItem == null || mSortItem == null || mProItem == null || mMultSelect == null){
    		return;
    	}
        mNewItem.setVisible(false);
        mSearchItem.setVisible(false);
        mViewItem.setVisible(false);
        //mSortItem.setVisible(false);
        mProItem.setVisible(false);
        mMultSelect.setVisible(false);
       // mHide.setVisible(false);
    }
    
    private void showMenuOfActionBar(){
        mNewItem.setVisible(true);
        mSearchItem.setVisible(true);
        mViewItem.setVisible(true);
        mSortItem.setVisible(true);
        mProItem.setVisible(true);
        mMultSelect.setVisible(true);
        //mHide.setVisible(true);
    }

    public static boolean isGridView() {
	return mViewMode == Util.VIEW_GRID;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home: 
        	if (isSearch) {
        		if (mSearchCreated || mCreatedFromSearchSuggestion) {
            		Log.d("","w23001- search create the activity");
            		mSearchCreated = false;
            		mCreatedFromSearchSuggestion = false;
            		isSearch = false;
            		finish();
            	}
            	if (isSearch){
            		Log.d("","w23001-searching");
            		isSearch = false;
            		mReturned = true;
                	stopSearchThread();
            		openDir(mAdapter.getCurrentFile(), true);
            		showMenuOfActionBar();
            	}
        	} else {
                 showFloderView();
        	}
            return true;
        case MENU_LIST_VIEW:
            switchview(Util.VIEW_LIST);
            return true;
        case MENU_GRID_VIEW: 
            switchview(Util.VIEW_GRID);
            return true;
        
        case MENU_NEW_FOLDER:
            showDialog(DIALOG_NEW_FOLDER);
            return true;

        case MENU_DELETE_CHOICE:
            Bundle b = new Bundle();
            b.putInt(ChooseActivity.KEY_ACTION, Util.ACTION_DELETE);
            b.putString(ChooseActivity.KEY_PATH, mAdapter.getCurrentFile().getRealPath());

            Intent intent = new Intent();
            intent.setClass(this, ChooseActivity.class);
            intent.putExtras(b);

            startActivityForResult(intent, Util.REQUEST_TYPE_DELETECHOICE);
            return true;

        case MENU_MOVE_CHOICE:
            b = new Bundle();
            b.putInt(ChooseActivity.KEY_ACTION, Util.ACTION_MOVE);
            b.putString(ChooseActivity.KEY_PATH, mAdapter.getCurrentFile().getRealPath());

            intent = new Intent();
            intent.setClass(this, ChooseActivity.class);
            intent.putExtras(b);

            startActivityForResult(intent, Util.REQUEST_TYPE_MOVECHOICE);
            return true;

        case MENU_COPY_CHOICE:
            b = new Bundle();
            b.putInt(ChooseActivity.KEY_ACTION, Util.ACTION_COPY);
            b.putString(ChooseActivity.KEY_PATH, mAdapter.getCurrentFile().getRealPath());

            intent = new Intent();
            intent.setClass(this, ChooseActivity.class);
            intent.putExtras(b);

            startActivityForResult(intent, Util.REQUEST_TYPE_COPYCHOICE);
            return true;

        case MENU_SORTING:
            showDialog(DIALOG_SORT);
            return true;

        case MENU_PROPERTY:
            showDialog(DIALOG_SDCARD_PROPERTY);
            return true;
        case MENU_SEARCH:
        	onSearchRequested();
            return true;
       /* case MENU_HIDEUNHIDE:
        	b = new Bundle();
            b.putInt(ChooseActivity.KEY_ACTION, Util.ACTION_HIDEUNHIDE);
            b.putString(ChooseActivity.KEY_PATH, mAdapter.getCurrentFile().getRealPath());

            intent = new Intent();
            intent.setClass(this, ChooseActivity.class);
            intent.putExtras(b);

            startActivityForResult(intent, Util.REQUEST_TYPE_HIDEUNHIDE);
            return true;*/
        }
	
        return super.onOptionsItemSelected(item);

    }
    public void switchview(int viewMode){
        if (viewMode == Util.VIEW_LIST) {
    		if (mViewMode == Util.VIEW_LIST) {
    			return;
    		} else {
    			mGrid.setVisibility(View.GONE);
    			mList.setVisibility(View.VISIBLE);
                        mViewItem.setIcon(R.drawable.ic_menu_list_view);   
    			//mViewItem.setTitle(R.string.menu_view_list);
    			mViewMode = Util.VIEW_LIST;
    		}
    	} else {
    		if (mViewMode == Util.VIEW_GRID) {
    			return;
    		} else {
    			mList.setVisibility(View.GONE);
        		mGrid.setVisibility(View.VISIBLE);
                        mViewItem.setIcon(R.drawable.ic_menu_grid_view);
        		//mViewItem.setTitle(R.string.menu_view_grid);
        		mViewMode = Util.VIEW_GRID;
    		}
    	}	
    	 //mEmpty.setText(R.string.empty_folder);
    	 //mEmpty.setVisibility(View.VISIBLE);
    	 mFolderOperation = Util.FOLDER_OPERATION_OPENDIR;
    	 mAdapter.setViewMode(mViewMode);
    	 mAdapter.updateView();
         mOpenDirFi = mAdapter.getCurrentFile();
         if (mOpenDirFi != null && mOpenDirFi.isDirectory()) {
             mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_DISMISS);
         }
    	return;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        /*if (isSearch) {
        	return;
        }*/
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            return;
        }

        mCurrentFile = (FileInfo) mAdapter.getItem(info.position);

        if (mCurrentFile == null) {
            return;
        }

        menu.setHeaderTitle(mCurrentFile.getTitle());
        menu.setHeaderIcon(mCurrentFile.getIcon(this));

        if (!mCurrentFile.isVirtualFolder()) {

            if (!mCurrentFile.isDirectory()) {
            	if(isDrm && Util.getExtension(mCurrentFile.getRealPath()).equals("dcf")) {
            		if (drmClient == null){
            			drmClient = new DrmManagerClient(mContext);
            		}
            		if (DrmStore.RightsStatus.RIGHTS_VALID == drmClient.checkRightsStatus(mCurrentFile.getRealPath(), DrmStore.Action.TRANSFER)) {
            			menu.add(0, MENU_SHARE, 0, R.string.menu_share);
            		}
                        if (DrmStore.RightsStatus.RIGHTS_VALID == drmClient.checkRightsStatus(mCurrentFile.getRealPath(), DrmStore.Action.RINGTONE) 
            			&& DrmStore.RightsStatus.RIGHTS_VALID == drmClient.checkRightsStatus(mCurrentFile.getRealPath(), DrmStore.Action.PLAY) 
            		    && Util.isInAudioDatabase(this, mCurrentFile.getRealPath())) {
            			menu.add(0, MENU_SET_AS_CALL_RINGTONE, 0, R.string.menu_set_as_call_ringtone);
						menu.add(0, MENU_SET_AS_MESSAGE_RINGTONE, 0, R.string.menu_set_as_message_ringtone);
            		}
            	} else {
	            	menu.add(0, MENU_SHARE, 0, R.string.menu_share);
	            
	                int mime = Util.getMimeId(this, mCurrentFile);
					if (mime == MimeType.MIME_IMAGE && Util.isInImagesDatabase(this, mCurrentFile.getRealPath())) {
						menu.add(0, MENU_SET_AS, 0, R.string.menu_set_as);
					}
					/* Ringtone settings will be divided into two parts */
					else if (mime == MimeType.MIME_AUDIO && Util.isInAudioDatabase(this, mCurrentFile.getRealPath())) {
						menu.add(0, MENU_SET_AS_CALL_RINGTONE, 0, R.string.menu_set_as_call_ringtone);
						menu.add(0, MENU_SET_AS_MESSAGE_RINGTONE, 0, R.string.menu_set_as_message_ringtone);
					} 
            	}
            	if (mCurrentFile.getTitle().startsWith(UPDATE_PACKAGE_PREFIX)){
            		menu.add(0, MENU_UPDATE, 0, R.string.menu_update_package);
            	}
            }
            /*else{            	
                if(mCurrentFile.isHidden() == false && !isSearch)               
                	menu.add(0, MENU_HIDE, 0, R.string.menu_hide);              

            }*/
            if (!isSearch) {
	            menu.add(0, MENU_COPY, 0, R.string.menu_copy);
	            menu.add(0, MENU_MOVE, 0, R.string.menu_move);
	            menu.add(0, MENU_RENAME, 0, R.string.menu_rename);
	            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
            }
        }
        menu.add(0, MENU_DETAIL, 0, R.string.menu_detail);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        mCurrentFile = (FileInfo) mAdapter.getItem(info.position);

        if (mCurrentFile == null) {
            return false;
        }
        switch (item.getItemId()) {
        case MENU_OPEN:
            openFileOrFolder(mCurrentFile);
            return true;

        case MENU_SHARE:
            Util.shareFile(this, mCurrentFile.getRealPath());
            return true;

        case MENU_SET_AS:
            int mime = Util.getMimeId(this, mCurrentFile);
            /* wallpaper will be shown dirctly and ringtone will be shown dialog */
            if (mime == MimeType.MIME_IMAGE)
                Util.setAs(this, mCurrentFile, 0);
            return true;

        case MENU_SET_AS_CALL_RINGTONE:
            Util.setAs(this,mCurrentFile, Util.CALL_RINGTONE);            
            return true;

        case MENU_SET_AS_MESSAGE_RINGTONE:
            Util.setAs(this,mCurrentFile, Util.MESSAGE_RINGTONE);               
            return true;

        case MENU_COPY:
            Util.getDestFolder(this, Util.REQUEST_TYPE_GETCOPYDEST);
            return true;

        case MENU_MOVE:
            Util.getDestFolder(this, Util.REQUEST_TYPE_GETMOVEDEST);
            return true;

        case MENU_DELETE:
            showDialog(DIALOG_DELETE);
            return true;
       /* case MENU_HIDE:
            FileSystem.hideFolder(mCurrentFile);
            refresh();
            return true;
*/
        case MENU_RENAME:
            /*if (!mCurrentFile.isDirectory()
                    && Util.isRingtoneFile(this, mCurrentFile.getRealPath())) {
                Toast.makeText(this,
                        getString(R.string.fail_rename_ringtone, mCurrentFile.getTitle()),
                        Toast.LENGTH_SHORT).show();
                return true;
            }*/

            showDialog(DIALOG_RENAME);
            return true;

        case MENU_DETAIL:
			if (mCurrentFile.isDirectory())
				showDialog(DIALOG_FOLDER_PROPERTY);
			else {
				showDialog(DIALOG_FILE_PROPERTY);
			}
            return true;
        case MENU_UPDATE:
            handleUpdateFile(mCurrentFile);
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
                                Toast.makeText(FileManagerActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if(name.length() > 64 )//force the name length < 64
                            {                             
                               name = name.substring(0,64);
                            }

                            name = Util.removeSpace(name);

                            if (name == null) {
                                Toast.makeText(FileManagerActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!Util.isValidateFileName(name)) {
                                Toast.makeText(
                                        FileManagerActivity.this,
                                        FileManagerActivity.this.getString(
                                                R.string.filename_invalidate, name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
			    
			                String path = parent.getRealPath() + FileInfo.SEPERATOR + name;
                            if (Util.isExistFolder(path)) {
                                Toast.makeText(
                                        FileManagerActivity.this,
                                        FileManagerActivity.this.getString(R.string.folder_existed,
                                                name), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(FileManagerActivity.this,
                                    FileManagerActivity.this.getString(prompt, name),
                                    Toast.LENGTH_SHORT).show();
                            if (fi != null) {
                                if (!cb.isChecked()) {
                                    openDir(fi, true);
                                } else {
                                	needRefresh = true;
                                    refresh();
                                }
                            }
                        }

                    }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Cancel should not do anything.
                }

            }).create();

        case DIALOG_DELETE:
            inflater = LayoutInflater.from(this);
            view = inflater.inflate(R.layout.dialog_confirm_delete, null);
            final CheckBox cb1 = (CheckBox) view.findViewById(R.id.cb_subfolder);
            cb1.setVisibility(View.GONE);

            return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_confirm_delete)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(" ").setPositiveButton(
                            android.R.string.ok, new OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    deleteFile(mCurrentFile);
                                }

                            }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel should not do anything.
                        }

                    }).create();

        case DIALOG_RENAME:
            inflater = LayoutInflater.from(this);
            view = inflater.inflate(R.layout.dialog_rename, null);
            final EditText et2 = (EditText) view.findViewById(R.id.edit_name);
            
            return new AlertDialog.Builder(this).setTitle(R.string.menu_rename).setView(view)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            String name = et2.getText().toString();
                            if (name == null || name.length() == 0) {
                                Toast.makeText(FileManagerActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if(name.length() > 64 )//force the name length < 64
                            {                             
                               name = name.substring(0,64);
                            }

                            name = Util.removeSpace(name);

                            if (name == null) {
                                Toast.makeText(FileManagerActivity.this,
                                        R.string.filename_cannot_null, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            //Begin CQG478 liyun, IKDOMINO-1814 check filename lenght 2011-9-14
                            /*if (Util.exceedFileNameMaxLen(name)) {
                                Toast.makeText(
                                        FileManagerActivity.this,
                                        FileManagerActivity.this.getString(
                                                R.string.filename_too_long, name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }*/
                            //End

                            if (!Util.isValidateFileName(name)) {
                                Toast.makeText(
                                        FileManagerActivity.this,
                                        FileManagerActivity.this.getString(
                                                R.string.filename_invalidate, name),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

			    
			    String path = mAdapter.getCurrentFile().getRealPath() + "/" + name;
                            File file = new File(path);
                            if (file.exists()) {
                                if (file.isDirectory() && mCurrentFile.isDirectory()) {
                                    Toast.makeText(FileManagerActivity.this,
                                            getString(R.string.folder_existed, name),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                } else if (file.isFile() && !mCurrentFile.isDirectory()) {
                                    Toast.makeText(FileManagerActivity.this,
                                            getString(R.string.file_existed, name),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            if (FileSystem.renameFileOrFolder(mCurrentFile, name)) {
                                File newFile = new File(path);

                                if (newFile.isDirectory()) {
                                    //Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature 2011-8-2
                                    //File[] list = newFile.listFiles();
                                    File[] list = Util.getFileList(newFile, true);
                                    //End
                                    
                                    if (list != null && list.length != 0){
                                        mNeedScanDisk = true;
                                        //modify by amt_xulei for SWITCHUITWO-509 2013-1-11
                                        //reason:Need scan folder after rename operator
                                    	Util.updateMediaProvider(FileManagerActivity.this,newFile.getPath());
                                    	//end modify
                                    }
                                } else {
                                	//modify by amt_xulei for SWITCHUITWO-426
                                	//reason:use new path instead of old path
//                                    int mimeId = Util.getMimeId(FileManagerActivity.this,
//                                            mCurrentFile.getRealPath());
                                	int mimeId = Util.getMimeId(FileManagerActivity.this,
                                          path);
                                	//end modify
                                    if (mimeId == MimeType.MIME_IMAGE
                                            || mimeId == MimeType.MIME_AUDIO
                                            || mimeId == MimeType.MIME_VIDEO
                                            || (isDrm && Util.getExtension(mCurrentFile.getRealPath()).equals("dcf"))) {
					//modify by amt_xulei for SWITCHUITWOV-99
					//reason:need scan a file and then refresh view automatic                                       
					//Util.updateMediaProvider(FileManagerActivity.this);
					Util.mediaScanFile(FileManagerActivity.this, newFile);
					mHandler.sendEmptyMessageDelayed(NEED_REFRESH_FOLDER, 500);
					//end modify
                                    //Begin CQG478 liyun, IKDOMINO-2398, restrict media scan 2011-9-15
                                    }
                                    
                                   /* mimeId = Util.getMimeId(FileManagerActivity.this, path);
                                    if (mimeId == MimeType.MIME_IMAGE
                                            || mimeId == MimeType.MIME_AUDIO
                                            || mimeId == MimeType.MIME_VIDEO) {
                                        Util.mediaScanFile(FileManagerActivity.this, newFile);
                                    }*/
                                    //End
                                }
                                //needRefresh = true;
                                //refresh();
                                openDir(mAdapter.getCurrentFile(), true);
                            } else {
                                Toast.makeText(
                                        FileManagerActivity.this,
                                        getString(R.string.fail_rename, mCurrentFile.getTitle(),
                                                name), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel should not do anything.
                        }

                    }).create();

        case DIALOG_SORT:
            return new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_sort).setTitle(
                    R.string.title_dlg_sort).setSingleChoiceItems(R.array.sort_type, 3,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            switch (whichButton) {
                            case 0:
                                setSortMethod(Util.SORT_BY_NAME);
                                break;
                            case 1:
                                setSortMethod(Util.SORT_BY_DATE);
                                break;
                            case 2:
                                setSortMethod(Util.SORT_BY_SIZE);
                                break;
                            case 3:
                                setSortMethod(Util.SORT_BY_TYPE);
                                break;
                            }
                            dialog.dismiss();
                        }
                    }).create();

        case DIALOG_FOLDER_PROPERTY:
            inflater = LayoutInflater.from(this);
            String s = getLocaleLanguage();
            if (s.equals("ar")){
            	view = inflater.inflate(R.layout.dialog_folder_property_ar, null);
            } else {
            	view = inflater.inflate(R.layout.dialog_folder_property, null);
            }
            return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_property)
                    .setView(view).setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }

                    }).create();
        case DIALOG_FILE_PROPERTY:
            inflater = LayoutInflater.from(this);
            s = getLocaleLanguage();
            if (s.equals("ar")){
            	view = inflater.inflate(R.layout.dialog_file_property_ar, null);
            } else {
            	view = inflater.inflate(R.layout.dialog_file_property, null);
            }
            return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_property)
                    .setView(view).setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }

                    }).create();

        case DIALOG_SDCARD_PROPERTY:
            inflater = LayoutInflater.from(this);
            s = getLocaleLanguage();
            if (s.equals("ar")){
            	view = inflater.inflate(R.layout.dialog_sd_property_ar, null);
            } else {
                view = inflater.inflate(R.layout.dialog_sd_property, null);
            }
            return new AlertDialog.Builder(this).setTitle(R.string.title_dlg_property)
                    .setView(view).setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
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

        case DIALOG_DELETE:
            //TextView prompt = (TextView) dialog.findViewById(R.id.prompt);
            if (mCurrentFile!=null)
                ((AlertDialog) dialog).setMessage(getString(R.string.confirm_delete, mCurrentFile.getTitle()));

            break;
        case DIALOG_RENAME:
            et = (EditText) dialog.findViewById(R.id.edit_name);
            if (mCurrentFile!=null){
                  et.clearFocus();
                  et.setText(mCurrentFile.getTitle());
                  //Begin CQG478 liyun, IKDOMINO-2103, add cursor at end 2011-8-25
                  et.setSelection(et.getText().toString().length());
                  //End                  
               }
            break;
        case DIALOG_FILE_PROPERTY:
        case DIALOG_FOLDER_PROPERTY:
            if (mCurrentFile!=null)
               showProperty(dialog, mCurrentFile);
            break;
        case DIALOG_SDCARD_PROPERTY:
            showSDCardProperty(dialog);
            break;
        }
    }
    
    //modify by amt_xulei for SWITCHUITWO-293
    //reason:this method is only executed once
    @Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		Log.d("", "back key up");
    		//modify by amt_xulei for SWITCHUITWO-425
    		//reason:handler onKeyUp after onKeyDown,otherwise don't handler it.
    		if (isBackKeyPressDown)
    		{
	    		isBackKeyPressDown = false;
	    	//end modify
	                mReturned = true;
	        	if (mSearchCreated || mCreatedFromSearchSuggestion) {
	        		mSearchCreated = false;
	        		mCreatedFromSearchSuggestion = false;
	        		isSearch = false;
	        		finish();
	        		return true;
	        	}
	        	if (isSearch){
	        		isSearch = false;
	            	stopSearchThread();
	        		openDir(mAdapter.getCurrentFile(), true);
	        	    showMenuOfActionBar();
	        		return true;
	        	}
	            if (mAdapter.getCurrentFile() == null || mAdapter.getCurrentFile().isRoot()) {
	                if (mNeedScanDisk) {
	                    Util.updateMediaProvider(this);
	                }
	                finish();
	            } else {
	            	FileInfo fi = mAdapter.getCurrentFile();
	                if (fi != null) {
	                    FileInfo fiParent = fi.getParent();
	                    if (fiParent != null) {
	                        openDir(fiParent, true);
	                	    showMenuOfActionBar();
	                    }
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
    		if(isSearch){
    			return true;
    		}
    		return super.onKeyDown(keyCode, event);
    	}else if (keyCode == KeyEvent.KEYCODE_BACK) {
    		Log.d("", "back key down");
    		isBackKeyPressDown = true;
    	}else if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (mNeedScanDisk) {
                Util.updateMediaProvider(this);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    public void showFloderView(){
    	mReturned = true;
    	String[] parts;
    	//modify by amt_xulei for SWITCHUITWO-7
        //reason:CT JB testload SDcard and internal storage path
        // is /storage/sdcard0 and /storage/sdcard1
//    	String root = "/mnt";
    	String root = "/storage";
    	//end modify
        String currentpath = mAdapter.getCurrentFile().getRealPath();

        parts =	currentpath.substring(root.length()).split("/");     
        parts[0] = this.getString(R.string.home);
        //parts[1] = this.getString(R.string.removable_sdcard);
        currentDirs = new String[parts.length];
        if(FileManager.isEmmc(mContext) && FileManager.getInternalSd(mContext) != null && FileManager.getInternalSd(mContext).equals(root+ "/" +parts[1])){
        	if (currentDirs.length > 1) {
   	            currentDirs[1] = FileManager.getInternalSd(mContext);
       	    }
        	parts[1] = this.getString(R.string.internal_storage);
        } else {
        	if (currentDirs.length > 1) {
   	            currentDirs[1] = FileManager.getRemovableSd(mContext);
       	    }
        	parts[1] = this.getString(R.string.removable_sdcard);
        }
        if (currentDirs.length > 2 ) {
            for (int i = 2; i < currentDirs.length; i++) {                   
                currentDirs[i] = currentDirs[i - 1] + "/" + parts[i];
            }
        }
        AlertDialog alert =
            createSingleChoiceDiag(R.string.select_folder, parts, parts.length - 1,
                folderViewDialogListener);
        alert.show();
    }
    
    public void onClick(View v) {
    	if (isSearch == true){
    		isSearch = false;
        	stopSearchThread();
    	}
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (mMultiChoiceMode)
            return;
        
        FileInfo fi = (FileInfo) mAdapter.getItem(position);
        if (fi == null) {
        	return;
        }
        if (fi.isDirectory()) {
      	  if (isSearch) {
      	      isSearch = false;
      	      if (mSearchCreated || mCreatedFromSearchSuggestion) {
      	    	  openedBySearch = true;
      	      }
      	      mSearchCreated = false;
      	      mCreatedFromSearchSuggestion = false;
      	      showMenuOfActionBar();
          }
          openFileOrFolder(fi);
  	  } else {
  		openFile(this, fi.getRealPath());
      }
    }

    private void openDir(FileInfo fi, boolean record) {
    	if (fi == null){
    		Log.e("", "w23001- opened fi can't be null!");
    		return;
    	}
    	Log.d("", "w23001- openDir- " + fi.getRealPath());    	
    	if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
    	    String externalPath = FileManager.getRemovableSd(mContext);
    	    try {
	    		if (!sm.getVolumeState(externalPath).equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
		    		Log.d("", "w23001- sd have been unmounted");
		    		finish();
		            return;
	    		}
    	    } catch (Exception e){
    	    	Log.e("", "w23001- storage manager throung the exception!");
    	    	return;
    	    }
    	}
        if (!fi.isDirectory() && !fi.isVirtualFolder()) {
            openDir(mAdapter.getRoot(), true);
            return;
        }
       
        int arrSize = mScrollList.size();
        int curDepth = Util.getCurrentDepth(mOpenDirFi);
        View firtView = getView().getChildAt(0);      

        if (!mReturned) {
            if ( arrSize > curDepth) {
                ((Util.ScrollPosition)mScrollList.get(curDepth)).index = getView().getFirstVisiblePosition();
                ((Util.ScrollPosition)mScrollList.get(curDepth)).top = (firtView == null) ? 0 : firtView.getTop(); 
            } else {
                Util.ScrollPosition scrollPos = new Util.ScrollPosition();
                scrollPos.index = getView().getFirstVisiblePosition();
                scrollPos.top = (firtView == null) ? 0 : firtView.getTop(); 
                mScrollList.add(scrollPos);     
            }
        }
        
       /* if (!fi.isVirtualFolder()) {
            //Begin CQG478 liyun, IKDOMINO-2066, sdcard share 2011-8-24
            if (Util.isShareSDCard()) {            
                fi = mAdapter.getRoot();
                mEmpty.setText(R.string.sdcard_is_shared);
            //End
            } else if (Util.fileOnSDCard(fi.getRealPath()) && !Util.isExistSDCard()) {
                //Toast.makeText(this, R.string.sdcard_not_exist, Toast.LENGTH_SHORT).show();
                fi = mAdapter.getRoot();
                mEmpty.setText(R.string.sdcard_not_exist);
            } else if (!Util.isExistFolder(fi.getRealPath())) {
                Toast.makeText(this, getString(R.string.folder_not_exist, fi.getTitle()),
                        Toast.LENGTH_SHORT).show();
                fi = mAdapter.getRoot();
            }
        }
*/
        mOpenDirFi = fi;
        mOpenDirRecord = record;

        if (mOpenDirFi != null && mOpenDirFi.isDirectory()) {
            mFolderOperation = Util.FOLDER_OPERATION_OPENDIR;
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_SHOW);
            mAdapter.setCurrentFile(mOpenDirFi);

            Util.StartChildrenThread(mHandler, mFilesContainer,
                    mOpenDirFi, null, mSortMethod, mAscending, false);
        }
    }

    private void openDirUI(FileInfo fi, boolean record) {
    	Log.d("", "w23001- openDirUI-enter");
    	
    	if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
    	    String externalPath = FileManager.getRemovableSd(mContext);
    	    try {
	    		if (!sm.getVolumeState(externalPath).equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
		    		Log.d("", "w23001- sd have been unmounted");
		    		finish();
		            return;
	    		}
    	    } catch (Exception e){
    	    	Log.e("", "w23001- storage manager throung the exception!");
    	    	return;
    	    }
    	}    	
        mMultiChoiceMode = false;
        mAdapter.setCurrentFile(fi);         
        mAdapter.setFiles(mFilesContainer.getFiles());
        mAdapter.notifyDataSetChanged();
        //mAdapter.setFiles(mFilesContainer.getUnHideFiles());
	
	//add by amt_xulei for SWITCHUITWOV-121 2012-9-7
	mAdapter.setViewMode(mViewMode);
        mAdapter.updateView();
	//end add amt_xulei
        mAdapter.setChoiceMode(mMultiChoiceMode);
        getView().setAdapter(mAdapter);

        //Begin CQG478 liyun, IKDOMINO-2325, scroll position 2011-9-5
        if (mReturned) {            
            mReturned = false;
                    
            if (mScrollList.size() > Util.getCurrentDepth(mOpenDirFi)) {
                Util.ScrollPosition scrollPos = mScrollList.get(Util.getCurrentDepth(mOpenDirFi));
                if(FileManagerActivity.mViewMode == Util.VIEW_LIST) {
                	((ListView) getView()).setSelectionFromTop(scrollPos.index, scrollPos.top);
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
            getView().setVisibility(View.GONE);
            mEmpty.setText(R.string.empty_folder);
            mEmpty.setVisibility(View.VISIBLE);

        } else {
            mEmpty.setVisibility(View.GONE);
            getView().setVisibility(View.VISIBLE);
            //mList.requestFocus();
            getView().requestFocus();
        }

        if (!Util.isPathOfSDCard(fi.getRealPath(), mContext)) {        	
        	mActionbar.setIcon(R.drawable.ic_folder_open);
        	mActionbar.setTitle(fi.getTitle());
        } else {
	    if (fi.getRealPath().equals(FileManager.getRemovableSd(mContext))){
	    	mActionbar.setIcon(R.drawable.ic_sdcard);
	    	mActionbar.setTitle(R.string.location_sdcard);
	    } else {
	    	mActionbar.setIcon(R.drawable.ic_thb_device);
	    	mActionbar.setTitle(R.string.internal_storage);
	    }

        }
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	int arrSize = mScrollList.size();
        int curDepth = Util.getCurrentDepth(mOpenDirFi);
        View firtView = getView().getChildAt(0);      

        if (!mReturned) {
            if ( arrSize > curDepth) {
                ((Util.ScrollPosition)mScrollList.get(curDepth)).index = getView().getFirstVisiblePosition();
                ((Util.ScrollPosition)mScrollList.get(curDepth)).top = (firtView == null) ? 0 : firtView.getTop(); 
            } else {
                Util.ScrollPosition scrollPos = new Util.ScrollPosition();
                scrollPos.index = getView().getFirstVisiblePosition();
                scrollPos.top = (firtView == null) ? 0 : firtView.getTop(); 
                mScrollList.add(scrollPos);     
            }
        }
    }

    protected void refresh() {
        mFolderOperation = Util.FOLDER_OPERATION_OPENDIR;
        mReturned = true;
        mOpenDirFi = mAdapter.getCurrentFile();
        if (needRefresh && mOpenDirFi != null && mOpenDirFi.isDirectory()) {
            mHandler.sendEmptyMessage(Util.MSG_PROGRESS_OPENDIR_SHOW);
            Util.StartChildrenThread(mHandler, mFilesContainer,
                    mOpenDirFi, null, mSortMethod, mAscending, false);
        }
        needRefresh = true;
    }

    private void refreshUI() {
    	Log.d("", "w23001- refreshUI");
        mAdapter.setFiles(mFilesContainer.getFiles());
        mAdapter.notifyDataSetChanged();
        //mAdapter.setFiles(mFilesContainer.getUnHideFiles());

	//add by amt_xulei for SWITCHUITWOV-121 2012-9-7
	mAdapter.setViewMode(mViewMode);
        mAdapter.updateView();
	//end add by amt_xulei	

        //Begin CQG478 liyun, IKDOMINO-1786, return to original position, 2011-8-12  
        getView().setAdapter(mAdapter);
        //End

        if (mAdapter.getCount() == 0) {
            getView().setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);

        } else {
            mEmpty.setVisibility(View.GONE);
            getView().setVisibility(View.VISIBLE);
            //mList.requestFocus();
            getView().requestFocus();

        }

        if (mOpenDirFi != null) {
            if (Util.isPathOfSDCard(mOpenDirFi.getRealPath(), mContext)) {
	    if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
	    	mActionbar.setIcon(R.drawable.ic_sdcard);
	    	mActionbar.setTitle(R.string.location_sdcard);
	    } else {
	    	mActionbar.setIcon(R.drawable.ic_thb_device);
	    	mActionbar.setTitle(R.string.internal_storage);
	    }

            } else {
            	mActionbar.setIcon(R.drawable.ic_folder_open);
            	mActionbar.setTitle(mOpenDirFi.getTitle());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Util.REQUEST_TYPE_GETCOPYDEST) {
            if (resultCode == RESULT_OK) {
                String path = Util.getPathFromFileUri(data.getData());
                if (path != null) {
                    needRefresh = false;
                    copyFile(mCurrentFile, path);
                }

            }
        } else if (requestCode == Util.REQUEST_TYPE_GETMOVEDEST) {
            if (resultCode == RESULT_OK) {
                String path = Util.getPathFromFileUri(data.getData());
                if (path != null) {
                    needRefresh = false;
                    moveFile(mCurrentFile, path);
                }
            }
        } else if (requestCode == Util.REQUEST_TYPE_MOVECHOICE) {
        	if (data != null){
        		boolean isSameDisk = data.getBooleanExtra(PerformDialog.PFM_SAMEDISK, true);        		
        		if (!isSameDisk) {
                	if (mCurrentContent.equalsIgnoreCase(Util.ID_EXTSDCARD)) {
                		mCurrentContent = Util.ID_LOCAL;
                	} else {
                		mCurrentContent = Util.ID_EXTSDCARD;
                	}
                	VirtualFolder.setRoot(mCurrentContent);
                	FileSystem.setRoot();
                }
        	}
            if (resultCode == RESULT_OK) {                
                //Begin CQG478 liyun, IKDOMINO-2368, 2011-9-9
                //Toast.makeText(this, R.string.success_move_file, Toast.LENGTH_SHORT).show();
                if (data != null) {                
                    if (data.getBooleanExtra(PerformDialog.PFM_HASSKIPPED, false)) {
                        Toast.makeText(this, R.string.success_skip_move_file, Toast.LENGTH_SHORT)
                                .show();
                    }
                    else if (data.getBooleanExtra(PerformDialog.PFM_STOP, false)) {
                        Toast.makeText(this, R.string.success_cancel_move_file, Toast.LENGTH_SHORT)
                                .show();
                    }
                    else if (data.getBooleanExtra(PerformDialog.PFM_FINISH, false)) {
                        Toast.makeText(this, R.string.success_move_file, Toast.LENGTH_SHORT).show();
                    } 
	                //End	
                    
	                String path = Util.getPathFromFileUri(data.getData());
	  
	                if (path != null) {
	                    FileInfo fi = FileInfo.make(path);
	                    if (fi != null) {
	                        openDir(fi, true);
	                    }
	                }
                }
            } else {
                Toast.makeText(this, R.string.success_cancel_move_file, Toast.LENGTH_SHORT).show();
                if (data != null) {
                    String path = Util.getPathFromFileUri(data.getData());
                    if (path != null) {
                        FileInfo fi = FileInfo.make(path);
                        if (fi != null) {
                            openDir(fi, true);
                        }
                    }
               }  
		if (!VirtualFolder.getRoot().equals(mCurrentContent)){
                	VirtualFolder.setRoot(mCurrentContent);
                	FileSystem.setRoot();
                }
            }
        } else if (requestCode == Util.REQUEST_TYPE_COPYCHOICE) {
        	if (data != null){
        		boolean isSameDisk = data.getBooleanExtra(PerformDialog.PFM_SAMEDISK, true);        		
        		if (!isSameDisk) {
                	if (mCurrentContent.equalsIgnoreCase(Util.ID_EXTSDCARD)) {
                		mCurrentContent = Util.ID_LOCAL;
                	} else {
                		mCurrentContent = Util.ID_EXTSDCARD;
                	}
                	VirtualFolder.setRoot(mCurrentContent);
                	FileSystem.setRoot();
                }
        	}
            if (resultCode == RESULT_OK) {
                //Begin CQG478 liyun, IKDOMINO-2368, 2011-9-9
                //Toast.makeText(this, R.string.success_copy_file, Toast.LENGTH_SHORT).show();
                if (data != null) {
                    if (data != null && data.getBooleanExtra(PerformDialog.PFM_HASSKIPPED, false)) {
                         Toast.makeText(this, R.string.success_skip_copy_file, Toast.LENGTH_SHORT)
                                .show();
                    }
                    else if (data.getBooleanExtra(PerformDialog.PFM_STOP, false)){
                        Toast.makeText(this, R.string.success_cancel_copy_file, Toast.LENGTH_SHORT)
                                .show();
                    }
                    else if (data.getBooleanExtra(PerformDialog.PFM_FINISH, false)) {
                        Toast.makeText(this, R.string.success_copy_file, Toast.LENGTH_SHORT).show();
                    }
                
	                String path = Util.getPathFromFileUri(data.getData());
	                if (path != null) {
	                    FileInfo fi = FileInfo.make(path);
	                    if (fi != null) {
	                        openDir(fi, true);
	                    }
	                }
                }
            } else {
                Toast.makeText(this, R.string.success_cancel_copy_file, Toast.LENGTH_SHORT).show();
                if (data != null) {
                    String path = Util.getPathFromFileUri(data.getData());
                    if (path != null) {
                        FileInfo fi = FileInfo.make(path);
                        if (fi != null) {
                            openDir(fi, true);
                        }
                    }
                }
		if (!VirtualFolder.getRoot().equals(mCurrentContent)){
                	VirtualFolder.setRoot(mCurrentContent);
                	FileSystem.setRoot();
                }
            }
        } else if (requestCode == Util.REQUEST_TYPE_DELETECHOICE) {
        	needRefresh = true;
            refresh();
        }
        else if (requestCode == Util.REQUEST_TYPE_HIDEUNHIDE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.success_hide_folder, Toast.LENGTH_SHORT).show();
                String path = Util.getPathFromFileUri(data.getData());
                if (path != null) {
                    FileInfo fi = FileInfo.make(path);
                    if (fi != null) {
                        openDir(fi, true);
                    }
                } else {
                	needRefresh = true;
                	refresh();
                }
            } else {
                Toast.makeText(this, R.string.success_cancel_hide_folder, Toast.LENGTH_SHORT).show();
                needRefresh = true;
                refresh();
            }
        } 

    }

    private void moveFile(FileInfo fi, String destFolder) {

        if (fi != null && checkDestFolder(fi, destFolder)) {
            FileInfo[] list = new FileInfo[1];
            list[0] = fi;
            mPerformDlg = new PerformDialog(this);
            mPerformDlg.setOnDismissListener(this);
            mPerformDlg.setOnCancelListener(this);
            mPerformDlg.setAction(Util.ACTION_MOVE, list, destFolder);
            mPerformDlg.setTitle(R.string.title_dlg_moving);
            mPerformDlg.setIcon(R.drawable.ic_menu_move);
            mPerformDlg.show();
        }

    }

    private void copyFile(FileInfo fi, String destFolder) {
        if (fi != null && checkDestFolder(fi, destFolder)) {
            FileInfo[] list = new FileInfo[1];
            list[0] = fi;
            mPerformDlg = new PerformDialog(this);
            mPerformDlg.setOnDismissListener(this);
            mPerformDlg.setOnCancelListener(this);
            mPerformDlg.setAction(Util.ACTION_COPY, list, destFolder);
            mPerformDlg.setTitle(R.string.title_dlg_copying);
            mPerformDlg.setIcon(R.drawable.ic_menu_copy);
            mPerformDlg.show();
        }
    }

    private void deleteFile(FileInfo fi) {
        if (fi != null) {
            FileInfo[] list = new FileInfo[1];
            list[0] = fi;
            mPerformDlg = new PerformDialog(this);
            mPerformDlg.setOnDismissListener(this);
            mPerformDlg.setOnCancelListener(this);
            mPerformDlg.setAction(Util.ACTION_DELETE, list, null);
            mPerformDlg.setTitle(R.string.title_dlg_deleting);
            mPerformDlg.setIcon(R.drawable.ic_menu_delete);
            mPerformDlg.show();
        }
    }

    private boolean checkDestFolder(FileInfo fi, String destFolder) {
        try {
            File fileSrc = new File(fi.getRealPath());
            File fileDest = new File(destFolder);

            if (fileSrc == null || fileDest == null)
                return false;

            if (fileSrc.isFile()) {
                if (fileDest.equals(fileSrc.getParentFile())) {
                    Toast.makeText(this, R.string.cannot_same_folder, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.cannot_write_folder, destFolder),
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            if (fileDest.equals(fileSrc.getParentFile())) {
                Toast.makeText(this, R.string.cannot_same_folder, Toast.LENGTH_SHORT).show();
                return false;
            }

            //Begin CQG478 liyun, IKDOMINO-3650, sub folder 2011-11-01
            if (Util.isSubFolder(fi.getRealPath(), destFolder)) {
            //End
                Toast.makeText(this, R.string.cannot_sub_folder, Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (Exception e) {
            return false;
        }

        return true;

    }

    public void onDismiss(DialogInterface dialog) {
    	if (!mPerformDlg.isSameDisk) {
        	if (mCurrentContent.equalsIgnoreCase(Util.ID_EXTSDCARD)) {
        		mCurrentContent = Util.ID_LOCAL;
        	} else {
        		mCurrentContent = Util.ID_EXTSDCARD;
        	}
        	VirtualFolder.setRoot(mCurrentContent);
        	FileSystem.setRoot();
        }
        if (dialog.equals(mPerformDlg)) {
            if (mPerformDlg.mAction == Util.ACTION_MOVE) {
                FileInfo fileInfo = FileInfo.make(mPerformDlg.mDestFolder);
                if (fileInfo != null) {
                    openDir(fileInfo, true);
                }

                if (mPerformDlg.mHasSkiped) {
                    Toast.makeText(this, R.string.success_skip_move_file, Toast.LENGTH_SHORT).show();
                }
                else if (mPerformDlg.mStop) {
                    Toast.makeText(this, R.string.success_cancel_move_file, Toast.LENGTH_SHORT).show();
                }
                else if (mPerformDlg.mFinished && !mPerformDlg.mRingtone) {
                    Toast.makeText(this, R.string.success_move_file, Toast.LENGTH_SHORT).show();
                }
            } else if (mPerformDlg.mAction == Util.ACTION_COPY) {
                FileInfo fileInfo = FileInfo.make(mPerformDlg.mDestFolder);
                if (fileInfo != null) {
                    openDir(fileInfo, true);
                }

                if (mPerformDlg.mHasSkiped) {
                    Toast.makeText(this, R.string.success_skip_copy_file, Toast.LENGTH_SHORT).show();
                }
                else if (mPerformDlg.mStop) {
                    Toast.makeText(this, R.string.success_cancel_copy_file, Toast.LENGTH_SHORT).show();
                }
                else if (mPerformDlg.mFinished){
                    Toast.makeText(this, R.string.success_copy_file, Toast.LENGTH_SHORT).show();
                }
            } else if (mPerformDlg.mAction == Util.ACTION_DELETE) {
            	needRefresh = true;
                refresh();
            }

            if (!mNeedScanDisk && mPerformDlg.mNeedScanDisk) {
                mNeedScanDisk = true;
            }
        }
    }

    public void onCancel(DialogInterface dialog) {
        if (dialog.equals(mPerformDlg)) {
            mPerformDlg.mAction = -1;

            if (!mNeedScanDisk && mPerformDlg.mNeedScanDisk) {
                mNeedScanDisk = true;
            }
            needRefresh = true;
            refresh();

        }
    }
    
  
    private void showProperty(Dialog dialog, FileInfo info) {
        TextView name = (TextView) dialog.findViewById(R.id.prop_name);
        if (name != null) {
        	name.setText(info.getTitle());
        }
        boolean isDrmFile = Util.getExtension(info.getRealPath()).equals("dcf");
        TextView type = (TextView) dialog.findViewById(R.id.prop_type);
        if (type != null) {
	        if (info.isDirectory()) {
	            type.setText(R.string.type_folder);
	        } else {
	        	if (isDrm && isDrmFile) {
	        		type.setText(R.string.type_drm);
	        	} else {
		            switch (Util.getMimeId(this, info)) {
		            case MimeType.MIME_AUDIO:
		                type.setText(R.string.type_audio);
		                break;
		            case MimeType.MIME_VIDEO:
		                type.setText(R.string.type_video);
		                break;
		            case MimeType.MIME_IMAGE:
		                type.setText(R.string.type_image);
		                break;
		            case MimeType.MIME_TEXT:
		            //Begin CQG478 liyun, IKDOMINO-2266, add vcf type 2011-9-5
		            case MimeType.MIME_VCF:
		            //End
		                type.setText(R.string.type_text);
		                break;
		            default: {
		                String postfix = Util.getExtension(info.getRealPath());
		                if (postfix != null)
		                    type.setText("." + postfix);	
		                break;
		            }
		            }
	        	}
	        }
        }
        TextView permition = (TextView) dialog.findViewById(R.id.prop_permition);
        if (permition != null){
	        if (!info.isDirectory()){
		        LinearLayout layout_permition = (LinearLayout) dialog.findViewById(R.id.layout_permition);
			    if (layout_permition != null) {    
		            if (isDrm && isDrmFile) {
			                layout_permition.setVisibility(View.GONE);
			    	}  else {
			                layout_permition.setVisibility(View.VISIBLE);
			    		    permition.setText(info.canWrite() ? R.string.permition_writable
			                : R.string.permition_readonly);
			    	}
	            }
	        } else {
	        	permition.setText(info.canWrite() ? R.string.permition_writable
		                : R.string.permition_readonly);
	        }
        }
        
        TextView location = (TextView) dialog.findViewById(R.id.prop_location);
        if (location != null) {
	       /* if(FileManager.iEmmc){
		        if (FileManagerActivity.mCurrentContent.equals(FileManager.ID_EXTSDCARD)){
		        	location.setText(R.string.location_sdcard);
		        } else {
		        	location.setText(R.string.internal_storage);
		        }
	        } else {
	        	location.setText(R.string.location_sdcard);
	        }*/
        	location.setText(info.getParent().getRealPath());
        }
        
        TextView size = (TextView) dialog.findViewById(R.id.prop_size);
        if (info.isDirectory()) {
            File file = new File(info.getRealPath());

            if (file != null) {
                File[] children = Util.getFileList(file, false);//change for hide/unhide
                long fileCount = 0;
                long childrenLen = 0;

                if (children != null) {
                    childrenLen = children.length;
                    for (File child : children) {
                        if (child.isFile())
                            fileCount++;
                    }
                }
                if (size != null) {
                	size.setText(Util.getSizeString(Util.getFolderSize(file)));
                }
                
                TextView contains = (TextView) dialog.findViewById(R.id.prop_contains);   
                if (contains != null) {	                
	                long folderCount = childrenLen - fileCount;
	                if (folderCount < 2) {
	                    if (fileCount < 2) {
	                        contains.setText(String.format(getString(R.string.folder_contains_folder_file), folderCount, fileCount));
	                    } else {
	                        contains.setText(String.format(getString(R.string.folder_contains_folder_files), folderCount, fileCount));
	                    }
	                } else {
	                    if (fileCount < 2) {
	                        contains.setText(String.format(getString(R.string.folder_contains_folders_file), folderCount, fileCount));
	                    } else {
	                        contains.setText(String.format(getString(R.string.folder_contains_folders_files), folderCount, fileCount));
	                    }
	                }
                }
            }
        } else {
        	if (size != null) {
        		size.setText(Util.getSizeString(info.getSize()));
        	}

            LinearLayout duration = (LinearLayout) dialog.findViewById(R.id.layout_duration);
            LinearLayout resolution = (LinearLayout) dialog.findViewById(R.id.layout_resolution);
            if (duration != null && resolution != null){
	            if (isDrm && isDrmFile) {
	            	duration.setVisibility(View.GONE);
	            	resolution.setVisibility(View.GONE);
	            } else {
		            switch (Util.getMimeId(this, info)) {
		            case MimeType.MIME_AUDIO:
		                duration.setVisibility(View.VISIBLE);
		                resolution.setVisibility(View.GONE);
		
		                long time = Util.getAudioDuration(this, info.getRealPath());
		                if (time != -1) {
		                    TextView view = (TextView) dialog.findViewById(R.id.prop_duration);
		                    time /= 1000;
		                    view.setText(String.format("%d:%02d:%02d", time / 3600, (time % 3600) / 60,
		                            (time % 3600) % 60));
		                }
		                break;
		            case MimeType.MIME_VIDEO:
		                duration.setVisibility(View.VISIBLE);
		                resolution.setVisibility(View.GONE);
		
		                time = Util.getVideoDuration(this, info.getRealPath());
		                if (time != -1) {
		                    TextView view = (TextView) dialog.findViewById(R.id.prop_duration);
		                    time /= 1000;
		                    view.setText(String.format("%d:%02d:%02d", time / 3600, (time % 3600) / 60,
		                            (time % 3600) % 60));
		                }
		                break;
		            case MimeType.MIME_IMAGE:
		                duration.setVisibility(View.GONE);
		                resolution.setVisibility(View.VISIBLE);
		                TextView view = (TextView) dialog.findViewById(R.id.prop_resolution);
		                view.setText(Util.getImageResolution(this, info.getRealPath()));
		                break;
		            default:
		                duration.setVisibility(View.GONE);
		                resolution.setVisibility(View.GONE);
		                break;
		            }
	            }
            }
            TextView modified = (TextView) dialog.findViewById(R.id.prop_modified);
            if (modified != null) {
            	modified.setText(new java.sql.Date(info.getDate()).toString());
            }
            
            TextView license = (TextView) dialog.findViewById(R.id.prop_license);            
            if(isDrm && isDrmFile) {
                String text = null;
                if (drmClient == null){
        			drmClient = new DrmManagerClient(mContext);
        		}
                ContentValues  rights = drmClient.getConstraints(info.getRealPath(), DrmStore.Action.PLAY);
            	
            	if (null != rights) {
            		int rightType = rights.getAsInteger("constraintType");            	
	            	int i = 0;
                        Log.d("","w23001-right type = "+ rightType);	            	
                	if ((rightType & DRM_INTERVAL_CONSTRAINT) >0) {
                		i++;
                		String date = rights.getAsString("intervalDate");
                		String time = rights.getAsString("intervalTime");
                		text = String.format(getString(R.string.interval_constraint, i, date + time));
                	}
                        if ((rightType & DRM_COUNT_CONSTRAINT) >0) {
                    	    i++;
                    	    int count = rights.getAsInteger("countNumber");
                    	    if(text != null) {
                    		text = text + "\n" + String.format(getString(R.string.count_constraint, i, count));
                    	     } else {
                    		text = String.format(getString(R.string.count_constraint, i, count));
                    	    }
                	}
                        if ((rightType & DRM_START_TIME_CONSTRAINT) >0) {
                    	    i++;
                    	    String date = rights.getAsString("startDate");
                	    String time = rights.getAsString("startTime");
                    	    if(text != null) {
                    		text = text + "\n" + String.format(getString(R.string.start_time_constraint, i, date + time));
                    	    } else {
                    		text = String.format(getString(R.string.start_time_constraint, i, date + time));
                    	    }
                	}
                        if ((rightType & DRM_END_TIME_CONSTRAINT) >0) {
                    	    i++;
                    	    String date = rights.getAsString("endDate");
                	    String time = rights.getAsString("endTime");
                    	    if(text != null) {
                    		text = text + "\n" + String.format(getString(R.string.end_time_constraint, i, date + time));
                    	    } else {
                    		text = String.format(getString(R.string.end_time_constraint, i, date + time));
                    	    }
                	}      
            	} 
                if (license != null && text != null) {
                	license.setVisibility(View.VISIBLE);
                	license.setText(text);
                } else {
                	license.setVisibility(View.GONE);
                }
            } else {
            	if (license != null) {
            		license.setVisibility(View.GONE);
            	}
            }
        }
    }

    private void showSDCardProperty(Dialog dialog) {
        try {
        	StatFs stat = null;
        	if (mOpenDirFi.getRealPath().contains(FileManager.getRemovableSd(mContext))){
                stat = new StatFs(FileManager.getRemovableSd(mContext));
            } else {
            	stat = new StatFs(FileManager.getInternalSd(mContext));
            }
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            long availableBlocks = stat.getAvailableBlocks();
            
           TextView card_name = (TextView) dialog.findViewById(R.id.card_name);
           TextView card_type = (TextView) dialog.findViewById(R.id.card_type);
           if (mOpenDirFi.getRealPath().contains(FileManager.getRemovableSd(mContext))){
            	card_name.setText(R.string.removable_sdcard);
            	card_type.setText(R.string.type_removealbe_memory);
            } else {
            	card_name.setText(R.string.internal_storage);
            	card_type.setText(R.string.internal_storage);
            }
            TextView usedSpace = (TextView) dialog.findViewById(R.id.prop_used_space);
            usedSpace.setText(Util.getSizeString((totalBlocks - availableBlocks) * blockSize));

            TextView freeSpace = (TextView) dialog.findViewById(R.id.prop_free_space);
            freeSpace.setText(Util.getSizeString(availableBlocks * blockSize));

        } catch (IllegalArgumentException e) {
        	
        }

    }

    private AbsListView getView() {
        // w23001 support grid view
        if (mViewMode == Util.VIEW_LIST) {
        	mGrid.setVisibility(View.GONE);
           return mList;
        } else if (mViewMode == Util.VIEW_GRID){
        	mList.setVisibility(View.GONE);
        	return mGrid;
        }
        return null;
    }

    private void setSortMethod(int method) {

        boolean ascending = true;

        if (method == mSortMethod) {
            ascending = !mAscending;
        }

        mSortMethod = method;
        mAscending = ascending;
        //modify by amt_xulei for SWITCHUITWO-335
        //reason:Guarantee to be sorted file list consistent
//        mFilesContainer.setFiles(mAdapter.getFiles());
        mFilesContainer.setFiles(mAdapter.getCurrentFile().getChildren());
        //end modify
        mHandler.sendEmptyMessage(Util.MSG_PROGRESS_SORT_SHOW);
        Util.SortFilesThread thread = new Util.SortFilesThread(mHandler, mFilesContainer,
                mSortMethod, mAscending);
        thread.start();

    }

    public void openFileOrFolder(FileInfo fi) {
        if (fi != null) {
            if (fi.isDirectory()) {
                openDir(fi, true);

            } else {
                String path = fi.getRealPath();
                if (path != null) {
                    if (!Util.openFile(this, path)) {
                    	needRefresh = true;
                        refresh();
                    }
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!newConfig.locale.equals(mLocale)) {
            finish();
            startActivity(new Intent(this, FileManagerActivity.class));

        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())){
                if (mAdapter != null && !mMediaMountFromMe)
                   openDir(mAdapter.getRoot(), true);
            } 
	    //add by amt_xulei for SWITCHUITWOV-99 2012-9-7
	    //reason:after rename a file,media will scan files.Refresh list after scan finished
	    else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())){
            	Log.d("", "ACTION_MEDIA_SCANNER_FINISHED");
            	//modify by amt_xulei for SWITCHUITWO-296
            	//reason:not need refresh if in search mode
            	if (!isSearch){
            		needRefresh = true;
            		refresh();
            	}
            	//end modify
            }
	    //end add by amt_xulei
	    else {
            	Log.d("","w23001- sd is unmounted");
            	if (mPerformDlg != null ){
	    			if (mPerformDlg.isShowing()) {
	    				mPerformDlg.cancel();
	    			}
	    		}
            	finish();
            }
        }
    };
       
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d("","w23001- onResume");
        /*if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
    	    String externalPath = FileManager.getRemovableSd(mContext);
    	    try {
	    		if (!sm.getVolumeState(externalPath).equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
		    		Log.d("", "w23001- sd have been unmounted");
		    		if (mPerformDlg != null ){
		    			if (mPerformDlg.isShowing()) {
		    				mPerformDlg.cancel();
		    			}
		    		}
		    		finish();
	    		}
    	    } catch (Exception e){
    	    	Log.e("", "w23001- storage manager throung the exception!");
    	    	return;
    	    }
    	}*/
        if(!isSearch){
            refresh();
        }
        mMediaMountFromMe = false;
	
	//add by amt_xulei for SWITCHUITWOV-69 2012-9-12
	//reason:need to clear file path in database if main card has changed
	String mainCard = SystemProperties.get("persist.sys.main.storage");
	if (mainCard != null && !FileManagerActivity.mainCard.equals(mainCard)) {
		// system main storage has changed,need to refresh database
		
		if (!FileManagerActivity.mainCard.equals("")) {
			Log.d("", "mainCard is changed to " + mainCard);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SearchSuggestionsProvider.AUTHORITY,
					SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES);

			suggestions.clearHistory();
		}
		FileManagerActivity.mainCard = mainCard;
	}
	//end add
    }
    
    @Override
    public boolean onSearchRequested() {
      Bundle searchedDirectory = new Bundle();
      if (mAdapter.getCurrentFile() != null){
    	  Log.d("", "cxxlyl--- mAdapter.getCurrentFile() = " + mAdapter.getCurrentFile().getRealPath());
      }
      searchedDirectory.putString(FileManagerActivity.SEARCHED_DIRECTORY, mOpenDirFi.getRealPath());
      startSearch(null, false, searchedDirectory, false);
      return true;
    }
    
    private void startSearchDirectory(File searchedDirectory, String searchedString) {
    	if ((searchedString == null) || (searchedString.length() == 0)) {
    		return;
        }
        if (searchedDirectory != null) {
        	if (mCurrentContent.equals(Util.ID_EXTSDCARD)){
        		if (!sm.getVolumeState(FileManager.getRemovableSd(mContext)).equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
    				if (!isFinishing()) {
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
    				return;
    			}
        	} else {
        		if (!sm.getVolumeState(FileManager.getInternalSd(mContext)).equals(Environment.MEDIA_MOUNTED)) {
    			    if (!isFinishing()) {
    	    				(new AlertDialog.Builder(mContext))
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
	    						}).create().show();
    			    }
    			    return;
        		}
        	}
			stopSearchThread();
			mSearchDirectory = new SearchDirectory(searchedDirectory, this, mHandler, searchedString);
			if (mSearchDirectory != null) {
				synchronized (sLock) {
					mSearchDirectory.start();
				}
			}
        } else {
        	return;
        }
    }
    
    Object sLock = new Object();
    private void stopSearchThread() {
      if (mSearchDirectory != null) {
        synchronized (sLock) {
          if (mSearchDirectory.isAlive()) {
            mSearchDirectory.requestStop();
          }
          mSearchDirectory = null;
        }
      }
      mListDir.clear();
      mListFile.clear();
    }
    
    public void showDirectoryContents(DirectoryContents contents) {
    	FileInfo[] files;
    	ArrayList<FileInfo> files_a = new ArrayList<FileInfo>();
        mSearchDirectory = null;
        mListDir.clear();
        mListFile.clear();
        mListDir = contents.listDir;
        mListFile = contents.listFile;
        
        /*get the dir*/
        for (IconifiedText i : mListDir)  {
        	files_a.add(FileInfo.make(i.getPathInfo()));
        }
        /*get the file*/
        for (IconifiedText i : mListFile)  {
        	files_a.add(FileInfo.make(i.getPathInfo()));
        }
        
        hideMenuOfActionBar();
        mSortItem.setVisible(true);
        mSortItem.setEnabled(true);
        mActionbar.setIcon(R.drawable.ic_menu_search);

        if (Util.isPathOfSDCard(m_FirstSearchedDirectory, mContext)){
        	if (mCurrentContent.equals(Util.ID_EXTSDCARD)) {   
	        	mActionbar.setTitle(getString(R.string.matches_for) + " \"" + searchedString + "\" " + getString(R.string.found_in) + " " 
	                        + getString(R.string.removable_sdcard));
            } else {
            	mActionbar.setTitle(getString(R.string.matches_for) + " \"" + searchedString + "\" " + getString(R.string.found_in) + " " 
                            + getString(R.string.internal_storage));
        	}
        }      
        else if(mAdapter.getCurrentFile() != null) {          
        	mActionbar.setTitle(getString(R.string.matches_for) + " \"" + searchedString + "\" " + getString(R.string.found_in) + " " 
                                + mAdapter.getCurrentFile().getTitle());
        } 

        if (files_a.size() == 0){
                getView().setVisibility(View.GONE);
                mEmpty.setText(R.string.empty_search_result);
                mEmpty.setVisibility(View.VISIBLE);
                return;
        } else {
                getView().setVisibility(View.VISIBLE);
                mEmpty.setVisibility(View.GONE);
        }

        files = new FileInfo[files_a.size()];
        files_a.toArray(files);
        mAdapter.setFiles(files);
        mAdapter.notifyDataSetChanged();
   	    mAdapter.setViewMode(mViewMode);
	    mAdapter.updateView();
        mAdapter.setChoiceMode(mMultiChoiceMode);
        getView().setAdapter(mAdapter);
        return;
      }
    
    private void showLoadingDialog(String dialog) {
        if (this.isFinishing()) {
          return;
        }
        hideLoadingDialog();
        loadingDialog = new ProgressDialog(this){
        	@Override
            public boolean onTouchEvent(MotionEvent event) {
            	return true;
            }
        };
        loadingDialog.setMessage(dialog);
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCancelable(true);
        loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
          	stopSearchThread();
          	isSearch = false;
          	if (!mSearchCreated) {
          	    openDir(mAdapter.getCurrentFile(), true);
          	} else {
          	    finish();
          		mSearchCreated = false;
          	}
          }
        });
        if (!isFinishing() && (loadingDialog != null)) {
          try {
            loadingDialog.show();
          } catch (Exception e) {
            return;
          }
        }
      }
    
    private AlertDialog createSearchChoiceDialog(int titleId, CharSequence[] items, int checkedItem,
            DialogInterface.OnClickListener listener) {
	AlertDialog dialog = null;
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	if (titleId > 0) {
		builder.setTitle(titleId);
	}
	builder.setSingleChoiceItems(items, checkedItem, listener);
	builder.setNeutralButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int which) {
	if (FileManager.getSearchOption() == FileManager.INTERNALSTORAGE) {
		m_FirstSearchedDirectory = FileManager.getInternalSd(mContext);
				mCurrentContent = Util.ID_LOCAL;    	
		        VirtualFolder.setRoot(mCurrentContent);
		        FileSystem.setRoot();
	} else {
		m_FirstSearchedDirectory = FileManager.getRemovableSd(mContext);
	    	    try {
		    		if (!sm.getVolumeState(m_FirstSearchedDirectory).equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
			    		Log.d("", "w23001- sd have been unmounted");
			    		Toast.makeText(mContext, R.string.sdcard_not_exist, Toast.LENGTH_SHORT).show();
			    		finish();
		    		}
	    	    } catch (Exception e){
	    	    	Log.e("", "w23001- storage manager throung the exception!");
	    	    	finish();
	    	    }
				mCurrentContent = Util.ID_EXTSDCARD;    	
		        VirtualFolder.setRoot(mCurrentContent);
		        FileSystem.setRoot();
	}
	File currentDirectory = new File(m_FirstSearchedDirectory);
	startSearchDirectory(currentDirectory, searchedString);
	dialog.dismiss();
	}
	});
	dialog = builder.create();
	dialog.setCanceledOnTouchOutside(false);
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
            createSearchChoiceDialog(R.string.search_folder, parts, FileManager.getSearchOption(),
                searchOptionDialogListener);
        dialog.show();
      }
    
      private void hideLoadingDialog() {
          if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
          }
        }
      
    
      public boolean openFile(Activity activity, String path) {
          File file = new File(path);
          Uri data = null;
          String type = null;
          if (!file.exists()) {
              Toast.makeText(activity, activity.getString(R.string.file_not_exist, path),
                      Toast.LENGTH_LONG).show();
              return false;
          }
          Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
          if (isDrm && Util.getExtension(path).equals("dcf")) {
        	  if (drmClient == null){
      			drmClient = new DrmManagerClient(mContext);
      		  }
        	  type = drmClient.getOriginalMimeType(path);
        	  data = Uri.fromFile(file);
          } else {
	          data = Uri.fromFile(file);
	          type = Util.getMimeType(activity, path);
	           if (type != null) {
	              if (type.indexOf("audio") == 0) {
	              long id = getMediaContentIdByFilePath(mContext, path, 1);
	              if (id > 0) {
	                  data = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
	               }
	            } else if (type.indexOf("image") == 0) {
	            	//modify by amt_xulei for SWITCHUITWO-281
	            	//reason:Use this method to open the picture invalid
//	                long id = getMediaContentIdByFilePath(getBaseContext(), path, 0);
//	                if (id > 0) {
//	                    data = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
//	                }
	            	//end modify
	            }
	          }
          }
          intent.setDataAndType(data, type);
          intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

          try {
        	  activity.startActivity(intent);
          } catch (ActivityNotFoundException e) {
              Toast.makeText(activity, R.string.application_not_available, Toast.LENGTH_LONG).show();
          }
          return true;
      }  

     private DialogInterface.OnClickListener folderViewDialogListener =
         new DialogInterface.OnClickListener() {
    	     public void onClick(DialogInterface dialog, int item) {
    	         if (item == 0) {
    	        	 if (openedBySearch) {
    	        		 openedBySearch = false; 
    	        		 Intent intent = new Intent();
    	        	     intent.setClass(mContext, FileManager.class);
    	        	     startActivity(intent);
    	        	 } 
    	             finish(); // exit Content View and return to Home Page
    	             return;
    	           }
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
     public static long getMediaContentIdByFilePath(Context context, String filePath, int mediaMode) {
 	    long id = 0;
 	    String[] args = new String[]{filePath};
 	    String where = null;
 	    String[] cols = null;
 	    Cursor cs = null;

 	    try {
 	      switch (mediaMode) {
 	        case 0 :// image
 	          where = MediaStore.Images.Media.DATA + " like ? ";
 	          cols = new String[]{MediaStore.Images.Media._ID,};
 	          cs =
 	              context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
 	                  cols, where, args, null);
 	          break;
 	        case 1 :// audio
 	          where = MediaStore.Audio.Media.DATA + " like ? ";
 	          cols = new String[]{MediaStore.Audio.Media._ID,};
 	          cs =
 	              context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
 	                  where, args, null);
 	          break;
 	        case 2 :// video
 	          where = MediaStore.Video.Media.DATA + " like ? ";
 	          cols = new String[]{MediaStore.Video.Media._ID,};
 	          cs =
 	              context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols,
 	                  where, args, null);
 	          break;

 	        default :
 	          return id;
 	      }
 	      if (cs != null && cs.getCount() != 0) {
 	        cs.moveToFirst();
 	        id = cs.getLong(0);
 	      }
 	    }// catch (UnsupportedOperationException ex) {
	/*
	* Begin modify for EG808T_P006487 by fqdw43
	* 12-08-27 UnsupportedOperationException
	*/
		catch (Exception ex) {
	/** end by fqdw43 */
 	      ex.printStackTrace();
 	    } finally {
 	      if (cs != null) {
 	        cs.close();
 	        cs = null;
 	      }
 	    }
 	    return id;
 	  }

    private String getLocaleLanguage() {
    	Locale l = Locale.getDefault();
    	String s= String.format( "%s" , l.getLanguage());
    	Log.d("","language = " + s);
    	return s;
    }
    
    private void handleUpdateFile(final FileInfo aFile) {
    	if(null == aFile){
    		Log.d("", ", handleUpdateFile(): the parameter aFile is null!");
    		return;
    	}
    	if (FileManagerActivity.this.isFinishing()) {    		
    		return;
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setNegativeButton(R.string.btn_no, null);
    	builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {    			
    			Intent i = new Intent(ACTION_INSTALL_UPDATE_PACKAGE);
    			i.putExtra("update_package", aFile.getRealPath()); //the update package's path
    			i.putExtra("from", "com.motorola.filemanager"); //identify ourself to receiver, only fileman is trusted.
    			sendBroadcast(i);
    		}
    	});
    	builder.setMessage(R.string.msg_confirm_system_update);
    	AlertDialog dialog = builder.create();
    	try{
    		dialog.show();
    	}catch(Exception e){
    		return;
    	}
    }
}
