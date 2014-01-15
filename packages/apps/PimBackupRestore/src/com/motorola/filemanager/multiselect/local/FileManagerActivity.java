/*
 * Copyright (c) 2010 Motorola, Inc. All Rights Reserved The contents of this
 * file are Motorola Confidential Restricted (MCR). Revision history (newest
 * first): Date CR Author Description 2010-03-23 IKSHADOW-2425 A20815 initial
 * 2010-08-02 IKOLYMP-1522 A17501 Refresh list while resuming
 */

package com.motorola.filemanager.multiselect.local;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.BatteryManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.multiselect.FileManager;
import com.motorola.filemanager.multiselect.FileManagerApp;
import com.motorola.filemanager.multiselect.FileManagerContent;
import com.motorola.sdcardbackuprestore.Constants;
import com.motorola.sdcardbackuprestore.Import3rd;
import com.motorola.sdcardbackuprestore.Import3rd2;
import com.motorola.sdcardbackuprestore.R;
import com.motorola.sdcardbackuprestore.SdcardManager;
import com.motorola.filemanager.multiselect.utils.FileUtils;
import com.motorola.filemanager.multiselect.utils.IconifiedText;
import com.motorola.filemanager.multiselect.utils.IconifiedTextListAdapter;
import com.motorola.filemanager.multiselect.utils.MimeTypeUtil;
import com.motorola.filemanager.multiselect.utils.IconifiedTextListAdapter.MultiSelectListener;

import com.android.internal.app.ResolverActivity;

public class FileManagerActivity extends Activity {
  protected static final int REQUEST_CODE_MOVE = 1;
  protected static final int REQUEST_CODE_COPY = 2;

  private static final int CON_MENU_DELETE = 20;
  private static final int CON_MENU_RENAME = 21;
  private static final int CON_MENU_SEND = 22;
  private static final int CON_MENU_OPEN = 23;
  private static final int CON_MENU_MOVE = 24;
  private static final int CON_MENU_COPY = 25;
  private static final int CON_MENU_INFO = 26;
  private static final int CON_MENU_UNZIP = 27;
  private static final int CON_MENU_ZIP = 28;
  private static final int CON_MENU_PRINT = 29;
  private static final int CON_MENU_UPDATE_PACKAGE = 30;

  private static final int OPT_MENU_NEW_FOLDER = 1;
  private static final int OPT_MENU_SELECTALL = 2;
  private static final int OPT_MENU_UNSELECTALL = 3;
  private static final int OPT_MENU_COPY = 4;
  private static final int OPT_MENU_MULTISELECT = 5;
  private static final int OPT_MENU_RENAME = 6;
  private static final int OPT_MENU_OPEN = 7;
  private static final int OPT_MENU_INFO = 8;
  private static final int OPT_MENU_VIEW_SWITCH = 9;
  private static final int OPT_MENU_SORT = 10;
  private static final int OPT_MENU_SORT_BY_NAME = 11;
  private static final int OPT_MENU_SORT_BY_TYPE = 12;
  private static final int OPT_MENU_SORT_BY_TIME = 13;
  private static final int OPT_MENU_SORT_BY_SIZE = 14;
  private static final int OPT_MENU_ZIP = 15;
  private static final int OPT_MENU_SEARCH = 16;

  private static final int DIALOG_NEW_FOLDER = 1;
  private static final int DIALOG_DELETE = 2;
  private static final int DIALOG_RENAME = 3;
  private static final int DIALOG_ZIP = 4;
  private static final int DIALOG_EXTRACT = 5;
  // IKCHNDEV-632--begin---
  private static final int DIALOG_CALL_RINGTONE = 6;
  private static final int DIALOG_MESSAGE_RINGTONE = 7;
  // IKCHNDEV-632--end---
  // IKAROWANA-81 begin tqr743
  private static final int CON_MENU_SET_AS_CALL_RINGTONE = 31;
  private static final int CON_MENU_SET_AS_MESSAGE_RINGTONE = 32;
  // IKAROWANA-81 end  tqr743

  private static final int CON_MENU_OPEN_WITH = 33;

  private static final String BUNDLE_CURRENT_DIRECTORY = "current_directory";
  private static final String BUNDLE_CONTEXT_FILE = "context_file";
  private static final String BUNDLE_CONTEXT_TEXT = "context_text";
  private static final String SEARCHED_DIRECTORY = "searched_directory";
  private static final String ACTION_MOTOROLA_INSTALL_UPDATE_PACKAGE = "android.intent.action.MOTOROLA_INSTALL_UPDATE_PACKAGE";
  private static final String UPDATE_PACKAGE_START = "update";
  private static final String ACTION_VIEW_FOLDER="com.mot.FileManagerIntents.ACTION_VIEWFOLDER";

  private  static final String EXTRA_REPLACE_PREFERRED="replace_old_preferred_activity";

  private static final String SETTINGS = "settings";
  private static final String BACKFROMSEARCH= "back_from_search";
  private static final int RESET_PARAM = 0;
  private static final int HOME_FROM_SEARCH = 1;
  private static final int PAGE_NORMAL_FROM_SEARCH=2;

  private ArrayList<IconifiedText> mDirectoryEntries = new ArrayList<IconifiedText>();
  List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
  List<IconifiedText> mListFile = new ArrayList<IconifiedText>();
  private IconifiedTextListAdapter mFileListAdapter = null;
  private File currentDirectory = new File("");
  private String mSdCardPath = "";
  private String mContextText;
  private File mContextFile = new File("");
  private TextView mEmptyText;
  private TextView mCurrentFolder;
  private ImageView mFolderIcon;
  private View mMultiSelectedPanel;
  private View mMcUploadNCancleview;
  private Button mMcSelectUploadFilesButton;
  private Button mMcUploadButton;
  private Button mMcCancelButton;
  private Button mMultiSelectOkButton;
  private Button mMultiSelectCancelButton;

  private DirectoryScanner mDirectoryScanner;
  private File mPreviousDirectory;
  private ThumbnailLoader mThumbnailLoader;
  private Handler currentHandler;
  private String gSearchString;

  private String[] mInputFileNames = null;
  private String mZipFileName = null;
  private String mTopPath = null;

  // BEGIN MOTOROLA, prmq46, 06/29/2011, IKMAIN-22925 / add function used to remember the current select/open item in the filemanager
  private boolean isFiles=false;
  private int mPosition = 0;
  // END IKMAIN-22925
  private boolean DEVELOPER_MODE = false; // Set to true to enable strict mode
                                          // during development

  private boolean mCreatedFromSearch = false;
  private static final int PAGE_CONTENT_NORMAL = 1;
  private static final int PAGE_CONTENT_SEARCH = 2;
  private SearchDirectory mSearchDirectory = null;
  private int mCurrentContent = PAGE_CONTENT_NORMAL;
  private int mPreviousContent = PAGE_CONTENT_NORMAL;
  private String mCurrentSearchedString = "";
  private String mPreviousSearchedString = "";
  private File mCurrentSearchDirectory = null;
  private File mPreviousSearchDirectory = null;
  private String m_FirstSearchedDirectory = "";
  private boolean mCreatedFromSearchSuggestion = false;


  private static String TAG = "FileManagerActivity: ";
  static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500; // List of
                                                                 // contents is
                                                                 // ready, obj =
                                                                 // DirectoryContents
  static final public int MESSAGE_SET_DELETING_PROGRESS = 501; // Set progress
                                                               // bar, arg1 =
                                                               // current value,
                                                               // arg2 = max
                                                               // value
  static final public int MESSAGE_ICON_CHANGED = 502; // View needs to be
                                                      // redrawn, obj =
                                                      // IconifiedText
  static final public int MESSAGE_COPY_FINISHED = 503;
  //static final public int MESSAGE_COPY_FAILED = 504;
  static final public int MESSAGE_DELETE_FINISHED = 505;
  static final public int MESSAGE_REFRESH = 506;
  static final public int MESSAGE_CANCEL_PROGRESS = 507;
  static final public int MESSAGE_MOVE_FINISHED = 508;
  //static final public int MESSAGE_MOVE_FAILED = 509;
  static final public int MESSAGE_SET_LOADING_PROGRESS = 510;
  static final public int MESSAGE_ZIP_STARTED = 511;
  static final public int MESSAGE_ZIP_SUCCESSFUL = 512;
  static final public int MESSAGE_ZIP_FAILED = 513;
  static final public int MESSAGE_UNZIP_STARTED = 514;
  static final public int MESSAGE_UNZIP_SUCCESSFUL = 515;
  static final public int MESSAGE_UNZIP_FAILED = 516;
  static final public int MESSAGE_ZIP_STOPPED = 517;
  static final public int MESSAGE_UNZIP_STOPPED = 518;
  //static final public int MESSAGE_LOAD_THUMBNAIL_END = 519; When loading big folders, it seems frustrating to wait until full thumbnail loading is done.
  //---IKCHNDEV-632--begin---
  /* network type */
  private static final int  CDMA_NETWORK = 0;
  private static final int  GSM_NETWORK  = 1;
  public static boolean mSupportDualMode = false;
  //---IKCHNDEV-632--end---

  public static final int ARG_DELETE_FAILED = 0;
  public static final int ARG_FILES_DELETED = 1;
  public static final int ARG_FOLDER_DELETED = 2;
  public static final int ARG_FILE_DELETED = 3;
  public static final int ARG_DELETE_MULTIPLE_FAILED = 4;

  Object mDLock = new Object();
  Object mTLock = new Object();

  /** Called when the activity is first created. */
  private Context mContext;

  static public class DirectoryContents {
    List<IconifiedText> listDir;
    List<IconifiedText> listFile;
  }
  private AbsListView mFileListView;
  private ListView mListView;
  private AbsListView mGridView;

  //IKDINARATD-457, a22121, 2011-07-05, start
  private boolean mIsViewFolderAction=false;
  //IKDINARATD-457 end

  private boolean mZipping = false;
  private boolean mUnzipping = false;
  private boolean mIsLowBattery = false;

  @Override
  public void onCreate(Bundle icicle) {
    if (DEVELOPER_MODE) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads()
          .detectDiskWrites().detectNetwork() // or .detectAll() for all
                                              // detectable problems
          .penaltyLog().build());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
          .detectAll().penaltyLog().penaltyDeath().build());
    }
    super.onCreate(icicle);
    mContext = this;

    currentHandler = new Handler() {
      public void handleMessage(Message msg) {
        FileManagerActivity.this.handleMessage(msg);
      }
    };

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.filelist);
    //---IKCHNDEV-632--begin---
    //---IKCHNDEV-3257--begin---
    mSupportDualMode = (SystemProperties.getInt("ro.telephony.secondary_network", -1) != -1);
    FileManagerApp.log(TAG+"mSupportDualMode is "+mSupportDualMode);
    //---IKCHNDEV-3257--end---
    //---IKCHNDEV-632--end---
    mListView = (ListView) findViewById(R.id.file_list);
    mListView.requestFocus();
    mListView.requestFocusFromTouch();
    mListView.setOnFocusChangeListener(focusChangeListener);
    mGridView = (AbsListView) findViewById(R.id.file_grid);
    mGridView.requestFocus();
    mGridView.requestFocusFromTouch();
    mGridView.setOnFocusChangeListener(focusChangeListener);
    updateViewMode();
    mEmptyText = (TextView) findViewById(R.id.empty_text);

    mCurrentFolder = (TextView) findViewById(R.id.current_folder);
    mFolderIcon = (ImageView) findViewById(R.id.folder_view_icon);
    if (FileManagerApp.isShowFolderViewIcon()) {
      findViewById(R.id.folder_view_icon).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.folder_view_icon).setVisibility(View.GONE);
    }

    findViewById(R.id.show_folder_view).setOnClickListener(new View.OnClickListener() {
      // @Override
      public void onClick(View v) {
        FileManagerApp.log(TAG + "Folder view onClick mCurrentContent" +mCurrentContent );
        if (mCurrentContent == PAGE_CONTENT_NORMAL){
            showFolderViewDialog();
        }
      }
    });

    mMultiSelectedPanel = findViewById(R.id.multi_select_panel);
    mMultiSelectOkButton = ((Button) mMultiSelectedPanel.findViewById(R.id.button_multi_confirm));
    mMultiSelectCancelButton = ((Button) mMultiSelectedPanel.findViewById(R.id.button_multi_cancel));
    mMultiSelectOkButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
        	  Log.d(TAG, "Click OK Button!");
            if (mFileListView.getAdapter() != null) {
            	ArrayList<String> selectFilesInfo = ((IconifiedTextListAdapter)(mFileListView.getAdapter())).getSelectFiles();
            	Intent intent = new Intent();
            	Bundle bundle = new Bundle();
            	bundle.putStringArrayList("SELECTED_FILES_INFO", selectFilesInfo);
            	intent.putExtras(bundle);
            	FileManager.rtnPickerResult(intent);
            }
          }
        });
    mMultiSelectCancelButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
        	  Log.d(TAG, "Click Cancel Button!");
				if (FileManagerContent.getActivityGroup() != null) {
					(FileManagerContent.getActivityGroup()).finish();
				}
				finish();
				if (FileManager.getInstance() != null) {
					FileManager.getInstance().finish();
				    }
				if (FileManagerContent.getActivityGroup() != null) {
				    FileManagerContent.getActivityGroup().finish();
				}
				if (Import3rd.getInstance() != null) {
					Import3rd.getInstance().finish();
				}
				if (Import3rd2.getInstance() != null) {
					Import3rd2.getInstance().finish();
				}
          }
        });
    
    mFileListView.setOnCreateContextMenuListener(this);
    mFileListView.setEmptyView(findViewById(R.id.empty));
    mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    String root = FileManagerApp.ROOT;
    File browseto = new File(root);

/*    mSdCardPath = getSdCardPath();
    FileManagerApp.log(TAG + "onCreate " + mSdCardPath);
    if (!TextUtils.isEmpty(mSdCardPath)) {
      browseto = new File(mSdCardPath);*/
    //IKDINARATD-457, a22121, 2011-07-05, start
    //deal with action of view a folder
    Intent intent = getIntent();
    String action = intent.getAction();
    Uri uri1=null;
    String viewFolderPath=null;
    if ( action!=null && action.equals(ACTION_VIEW_FOLDER)){
        uri1 = intent.getData();
        if (uri1 != null) {
                viewFolderPath= getPathFromFileUri(uri1);
        }
    }
    if(action!=null && action.equals(ACTION_VIEW_FOLDER) &&  uri1!=null && viewFolderPath!=null){
        mIsViewFolderAction=true;
        browseto=new File(viewFolderPath);
    }else{
        mIsViewFolderAction=false;
        mSdCardPath = getSdCardPath();
        if (!TextUtils.isEmpty(mSdCardPath)) {
            browseto = new File(mSdCardPath);
        }
    }
    //IKDINARATD-457 end
    checkStorageState();

    IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SHARED);
    intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    intentFilter.addDataScheme("file");
    registerReceiver(mSDCardReceiver, intentFilter);
  
    IntentFilter BatteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    registerReceiver(mBatteryLevelReceiver, BatteryIntentFilter);
 
    if (icicle != null) {
      String bundleCurrentDir = icicle.getString(BUNDLE_CURRENT_DIRECTORY);
      if (bundleCurrentDir != null) {
        browseto = new File(bundleCurrentDir);
      }
      String bundleContextFile = icicle.getString(BUNDLE_CONTEXT_FILE);
      if (bundleContextFile != null) {
        mContextFile = new File(bundleContextFile);
      }
      mContextText = icicle.getString(BUNDLE_CONTEXT_TEXT);
    }

    Intent theIntent = getIntent();
    FileManagerApp.log(TAG + "onCreate received intent=" + theIntent);
    if (Intent.ACTION_SEARCH.equals(theIntent.getAction())) {
      FileManagerApp.log(TAG + "onCreate action is ACTION_SEARCH ");
      String searchedString = theIntent.getStringExtra(SearchManager.QUERY);
      Bundle appData = theIntent.getBundleExtra(SearchManager.APP_DATA);
      if (appData != null) {
        m_FirstSearchedDirectory = appData.getString(FileManagerActivity.SEARCHED_DIRECTORY);
        if (m_FirstSearchedDirectory == null) {
          if (SdcardManager.hasInternalSdcard(mContext)) {
            if (SdcardManager.isSdcardMounted(mContext, true)) {
              gSearchString = searchedString;
              showSearchOptionDialog();
              FileManagerApp.log(TAG + "Search option is" + FileManagerApp.getSearchOption());
              if (FileManagerApp.getSearchOption() == FileManagerApp.INTERNALSTORAGE) {
                m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_DIR;
              } else {
                m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_EXT_DIR;
              }
            } else {
              m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_DIR;
              (new AlertDialog.Builder(mContext)).setTitle(R.string.sd_card)
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setMessage(R.string.sd_missing_search)
                  .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                  }).create().show();
            }
          } else {
            m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_DIR;
          }
        }
        currentDirectory = new File(m_FirstSearchedDirectory);
      } else {
        m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_DIR;
        currentDirectory = new File(m_FirstSearchedDirectory);
      }
      FileManagerApp.log(TAG + "onCreate action is ACTION_SEARCH searchedString=" + searchedString +
          ",searchedDirectory=" + currentDirectory);
      mCreatedFromSearch = true;
      startSearchDirectory(currentDirectory, searchedString);
    } else if (Intent.ACTION_VIEW.equals(theIntent.getAction())) {
      Uri uri = getIntent().getData();
      if (uri == null) {
        FileManagerApp.log(TAG + "intent is ACTION_VIEW " + "uri is null");
        finish();
        return;
      }
      FileManagerApp.log(TAG + "intent is ACTION_VIEW " + "uri=" + uri);
      Cursor cursor = managedQuery(uri, null, null, null, null);
      if (cursor == null) {
        FileManagerApp.log(TAG + "intent is ACTION_VIEW " + "cursor is null");
        finish();
      } else {
        cursor.moveToFirst();

        // int wIndex =
        // cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FOLDER);
        int dIndex = cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FULL_PATH);
        mCreatedFromSearchSuggestion = true;
        browseto = new File(cursor.getString(dIndex));
        if (browseto.isFile()) {
          FileManagerApp.log(TAG + "opening file " + browseto);
          new OpenFileTask().execute(browseto);
          FileManagerApp.log(TAG + "Search suggestion is clicked. Finish");
          finish();
        } else {
          FileManagerApp.log(TAG + "browse folder " + browseto);
          browseTo(browseto);
        }
        cursor.close();
      }
    } else {
      if (!TextUtils.isEmpty(browseto.getName())) {
        browseTo(browseto);
      }
    }
  }

  private View.OnFocusChangeListener focusChangeListener =
    new View.OnFocusChangeListener() {
      public void onFocusChange(View v, boolean hasFocus) {
        if (mFileListView.isFocused()) {
          outMultiSelectMode();
        }
      }
    };

  private synchronized void emptyList() {
    mDirectoryEntries.clear();
    mFileListView.clearTextFilter();
    updateAdapter(false);
    FileManagerApp.log(TAG + "emptylist");
  }

  private void checkStorageState() {
    if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD) {
    	if (!SdcardManager.isSdcardMounted(this, false)) {
    		mEmptyText.setText(R.string.sd_missing);
		}
    	/*
      if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED)) {
          mEmptyText.setText(R.string.sd_is_used_by_other);
        } else {
          mEmptyText.setText(R.string.sd_missing);
        }
      }
      */
    } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD) {
    	if (!SdcardManager.isSdcardMounted(this, true)) {
    		mEmptyText.setText(R.string.sd_missing);
		}
    	/*
      if (!MotoEnvironment.getExternalAltStorageState().equals(Environment.MEDIA_MOUNTED)) {
        if (MotoEnvironment.getExternalAltStorageState().equals(Environment.MEDIA_SHARED)) {
          mEmptyText.setText(R.string.sd_is_used_by_other);
        } else {
          mEmptyText.setText(R.string.sd_missing);
        }
      }
      */
    }
  }

  private void switchView() {
    if (FileManagerApp.isGridView()) {
      ((FileManagerApp) getApplication()).setGridView(false);
      mFileListView.setVisibility(View.GONE);
      mFileListView = mListView;
    } else {
      ((FileManagerApp) getApplication()).setGridView(true);
      mFileListView.setVisibility(View.GONE);
      mFileListView = mGridView;
    }
    mFileListView.setEmptyView(findViewById(R.id.empty));
    mFileListView.setVisibility(View.VISIBLE);
    if (mCurrentContent == PAGE_CONTENT_SEARCH) {
      // redo search
      startSearchDirectory(currentDirectory, mCurrentSearchedString);
    } else {
      refreshList();
    }
  }

  private void updateViewMode() {
    if (FileManagerApp.isGridView()) {
      mListView.setVisibility(View.GONE);
      mFileListView = mGridView;
    } else {
      mGridView.setVisibility(View.GONE);
      mFileListView = mListView;
    }
    mFileListView.setVisibility(View.VISIBLE);
  }

  private void updateMultiPanel() {
    if (mFileListView.getAdapter() != null) {
      IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) mFileListView.getAdapter();
      if (adapter.isSomeFileSelected()) {
        mMultiSelectedPanel.findViewById(R.id.button_multi_confirm).setEnabled(true);
      } else {
        mMultiSelectedPanel.findViewById(R.id.button_multi_confirm).setEnabled(false);
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    ArrayList<String> filename = null;
    String action = intent.getAction();
    FileManagerApp.log(TAG + "onNewIntent action= " + action);
    if (action == null) {
      return;
    }

    //IKDINARATD-457, a22121, 2011-07-05, start
    Uri uri=null;
    String viewFolderPath=null;
    FileManagerApp.log(TAG+" onNewIntent action="+action);
    if (action.equals(ACTION_VIEW_FOLDER)){
        uri = intent.getData();
        if (uri != null) {
            viewFolderPath= getPathFromFileUri(uri);
        }
        if(viewFolderPath!=null){
          mIsViewFolderAction=true;
          browseTo(new File(viewFolderPath));
        }
    //IKDINARATD-457 end
    } else if (action.equals(FileManager.ACTION_RESTART_ACTIVITY)) {
      updateViewMode();
      FileManagerApp.log(TAG + "onNewIntent " + getSdCardPath());
      checkStorageState();
      if (mCurrentContent == PAGE_CONTENT_SEARCH) {
        // redo search
        startSearchDirectory(currentDirectory, mCurrentSearchedString);
      } else {
        browseTo(new File(getSdCardPath()));
      }
    } else if (action.equals(FileManagerContent.ACTION_PASTE)) {
      filename = intent.getStringArrayListExtra(FileManagerContent.EXTRA_KEY_SOURCEFILENAME);
      if ((filename == null) || (filename.size() == 0)) {
        FileManagerApp.log(TAG + "onNewIntent ACTION_PASTE: filename is null or size is 0");
        return;
      }
      FileManagerApp.log(TAG + "onNewIntent ACTION_PASTE " + filename.get(0) + " path: " +
          currentDirectory.toString());
      if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(intent
          .getStringExtra(FileManagerContent.EXTRA_KEY_PASTEREASON))) {
        showLoadingDialog(getString(R.string.moving));
      }
    } else if (action.equals(FileManagerContent.ACTION_TRANSFERFINISHED)) {
      int transferResult = intent.getIntExtra(FileManagerContent.EXTRA_KEY_TRANSFERRESULT, 0);
      if (FileManagerContent.EXTRA_VAL_REASONCUT.equals(intent
          .getStringExtra(FileManagerContent.EXTRA_KEY_PASTEREASON))) {
        currentHandler.obtainMessage(MESSAGE_MOVE_FINISHED, transferResult, 0).sendToTarget();
      } else {
        currentHandler.obtainMessage(MESSAGE_COPY_FINISHED, transferResult, 0).sendToTarget();
      }
    } else if (Intent.ACTION_SEARCH.equals(action)) {
      FileManagerApp.log(TAG + "onNewIntent action is ACTION_SEARCH ");
      String searchedString = intent.getStringExtra(SearchManager.QUERY);
      FileManagerApp.log(TAG + "onNewIntent action is ACTION_SEARCH searchedString=" +
          searchedString);
      startSearchDirectory(currentDirectory, searchedString);
    } else if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
      if (uri == null){
        FileManagerApp.log(TAG +"onNewIntent:intent is ACTION_VIEW "+"uri is null");
        finish();
        return;
      }
      FileManagerApp.log(TAG +"onNewIntent:intent is ACTION_VIEW "+"uri="+uri);
      Cursor cursor = managedQuery(uri, null, null, null, null);
      if (cursor == null) {
        FileManagerApp.log(TAG +"onNewIntent:intent is ACTION_VIEW "+"cursor is null");
        finish();
      } else {
        cursor.moveToFirst();

        int dIndex = cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FULL_PATH);
        mCreatedFromSearchSuggestion = true;
        File browseto = new File(cursor.getString(dIndex));
        if (browseto.isFile()){
          FileManagerApp.log(TAG +"onNewIntent:opening file "+browseto);
          //openFile(browseto);
          new OpenFileTask().execute(browseto);
          FileManagerApp.log(TAG +"onNewIntent:Search suggestion is clicked. Finish");
          finish();
        } else {
          FileManagerApp.log(TAG +"onNewIntent:browse folder "+browseto);
          browseTo(browseto);
        }
        cursor.close();
      }
    }
    return;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (getBackFromSearch() == HOME_FROM_SEARCH) {
        resetBackFromSearch();
        FileManagerApp.log(TAG+"onResume called. Return to home screen from search page");
        finish();
    } else if (getBackFromSearch() == PAGE_NORMAL_FROM_SEARCH) {
      resetBackFromSearch();
      updateViewMode();
      refreshList();
    } else {
      /*
       * Do not refresh during multi_select mode since it cause issues
       * IKSTABLEFOUR-2461 Besides, if user select multi files pending
       * action(move, delete, cancel etc), better complete action before updating,
       * and refresh is done elsewhere once these actions complete.
       */

      // Refresh current list while resuming, a17501, IKOLYMP-1522
      if (mCurrentContent == PAGE_CONTENT_SEARCH) {
        FileManagerApp.log(TAG + "onResume called, mCurrentContent ==PAGE_CONTENT_SEARCH");
        startSearchDirectory(currentDirectory, mCurrentSearchedString);
      } else {
        FileManagerApp.log(TAG + "onResume called, mCurrentContent ==PAGE_CONTENT_NORMAL");
        if (mFileListView.getAdapter() != null) {
          if (!(((IconifiedTextListAdapter) mFileListView.getAdapter()).isSelectMode())) {
            FileManagerApp.log(TAG + "onResume called, mCurrentContent ==nomulti");
            if ((progressInfoDialog != null) || (loadingDialog != null)) {
              FileManagerApp.log(TAG + "onResume called, operation on-going, do not refreshList");
            } else {
              refreshList();
            }
          } else {
            FileManagerApp.log(TAG + "onResume called, mCurrentContent ==multi");
          }
        } else {
          FileManagerApp.log(TAG + "onResume called, mCurrentContent ==PAGE_CONTENT_NORMAL ADAPTER blank");
          refreshList();
        }
      }
    }
  }

  public void onDestroy() {
    FileManagerApp.log(TAG + "ondestroy called");
    // Stop the scanner.
    stopActivityThread();
    stopSearchThread();
    unregisterReceiver(mSDCardReceiver);
    unregisterReceiver(mBatteryLevelReceiver);
    if (!SdcardManager.hasInternalSdcard(mContext)) {
        if (FileManager.getInstance() != null) {
        	FileManager.getInstance().finish();
    	}
	}
    super.onDestroy();
  }

  private void stopActivityThread() {
    FileManagerApp.log(TAG + "stopActivityThread");
    if (mDirectoryScanner != null) {
      synchronized (mDLock) {
        if (mDirectoryScanner.isAlive()) {
          FileManagerApp.log(TAG + "Dir thread still alive stop");
          mDirectoryScanner.requestStop();
          try {
            mDirectoryScanner.join(200);
          }catch (Exception ex) {
            Log.e(TAG, "Cannot join directory scan thread.", ex);
          }
        }
        mDirectoryScanner = null;
      }
    }
    // dismiss the dialog if it is shown
    if (mThumbnailLoader != null) {
      synchronized (mTLock) {
        if (mThumbnailLoader.isAlive()) {
          FileManagerApp.log(TAG + "Thumb thread still alive stop");
          mThumbnailLoader.requestStop();
        }
        mThumbnailLoader = null;
      }
    }

    mListDir.clear();
    mListFile.clear();
    mEmptyText.setVisibility(View.GONE);
    if (mFileListView != null) {
      emptyList();
    }

    hideLoadingDialog();
    hideProgressInfoDialog();
  }

  Object sLock = new Object();
  private void stopSearchThread() {
    FileManagerApp.log(TAG + "stopSearchThread");
    if (mSearchDirectory != null) {
      synchronized (sLock) {
        if (mSearchDirectory.isAlive()) {
          FileManagerApp.log(TAG + "Sear thread still alive stop");
          mSearchDirectory.requestStop();
        }
        mSearchDirectory = null;
      }
    }
    // dismiss the dialog if it is shown
    hideLoadingDialog();
    mListDir.clear();
    mListFile.clear();
    mEmptyText.setVisibility(View.GONE);
    if (mFileListView != null) {
      emptyList();
    }
  }

  private void showMcUploadNCancleView() {
    mMcUploadNCancleview.setVisibility(View.VISIBLE);
    mMcSelectUploadFilesButton.setVisibility(View.VISIBLE);
    mMcUploadButton.setVisibility(View.GONE);
  }

  private void handleMessage(Message message) {
    switch (message.what) {
      case MESSAGE_SHOW_DIRECTORY_CONTENTS :
        hideLoadingDialog();
        if (message.obj != null) {
          FileManagerApp.log(TAG + "obj not null");
          showDirectoryContents((DirectoryContents) message.obj);
        }
        break;
      case MESSAGE_SET_DELETING_PROGRESS :
        showLoadingDialog(getString(R.string.deleting));
        break;
      case MESSAGE_SET_LOADING_PROGRESS :
        showLoadingDialog(getString(R.string.loading));
        break;
      case MESSAGE_ZIP_STARTED :
        showProgressInfoDialog(getString(R.string.Zipping));
        mZipping = true;
        break;
      case MESSAGE_UNZIP_STARTED :
        showProgressInfoDialog(getString(R.string.Unzipping));
        mUnzipping = true;
        break;
      case MESSAGE_ZIP_SUCCESSFUL :
        hideProgressInfoDialog();
        refreshList();
        break;
      case MESSAGE_UNZIP_SUCCESSFUL :
        hideProgressInfoDialog();
        if (message.obj != null) {
          String newFolderName = (String) message.obj;
          if (newFolderName != null) {
            if((FileManagerContent.getActivityGroup()) != null) {
              ((FileManagerContent) FileManagerContent.getActivityGroup()).mediaScanFolder("file://" + newFolderName);
            } else {
           //call private method to send the mediascan intent
              FileManagerApp.log(TAG +"ActivityGroup() is null,call private mediaScanFolder");
              mediaScanFolder("file://"+ newFolderName);
            }
          } else {
            FileManagerApp.log(TAG + "folder name null in MESSAGE_UNZIP_SUCCESSFUL");
          }
        }
        refreshList();
        break;
      case MESSAGE_ZIP_FAILED :
        hideProgressInfoDialog();
        Toast.makeText(this, R.string.error_zipping_file, Toast.LENGTH_SHORT).show();
        refreshList();
        break;
      case MESSAGE_UNZIP_FAILED :
        hideProgressInfoDialog();
        Toast.makeText(this, R.string.error_unzip_file, Toast.LENGTH_SHORT).show();
        refreshList();
        break;
      case MESSAGE_ZIP_STOPPED :
        hideProgressInfoDialog();
        Toast.makeText(this, R.string.stop_zip_file, Toast.LENGTH_SHORT).show();
        refreshList();
        break;
      case MESSAGE_UNZIP_STOPPED :
        hideProgressInfoDialog();
        Toast.makeText(this, R.string.stop_unzip_file, Toast.LENGTH_SHORT).show();
        refreshList();
        break;
      case MESSAGE_ICON_CHANGED :
        if (mThumbnailLoader != null && ((int) mThumbnailLoader.getId()) == message.arg2) {
                try{
                     mDirectoryEntries.get(message.arg1).setIcon((Drawable) message.obj);
                     updateAdapter(true);
                  }catch(IndexOutOfBoundsException e){
                     FileManagerApp.log(TAG + "Exception" + e.toString());
                  }
        } else if (mThumbnailLoader != null) {
          FileManagerApp.log(TAG + "Thumbnail nochange" + message.arg2 + " " +
              mThumbnailLoader.getId());
        }
        break;
      case MESSAGE_DELETE_FINISHED :
        hideLoadingDialog();
        if (!currentDirectory.exists()) {
          upOneLevel();
        } else {
          refreshList();
        }
        if (message.arg1 == ARG_FILES_DELETED) {
          Toast.makeText(this, R.string.files_delet_sucessfully, Toast.LENGTH_SHORT).show();
        } else if (message.arg1 == ARG_FILE_DELETED) {
          Toast.makeText(this, R.string.file_delet_sucessfully, Toast.LENGTH_SHORT).show();
        } else if (message.arg1 == ARG_FOLDER_DELETED) {
          Toast.makeText(this, R.string.folder_delet_sucessfully, Toast.LENGTH_SHORT).show();
        } else if (message.arg1 == ARG_DELETE_MULTIPLE_FAILED) {
          Toast.makeText(this, R.string.delet_failed_multiple, Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(this, R.string.delet_failed, Toast.LENGTH_SHORT).show();
        }
        break;
      case MESSAGE_REFRESH :
        refreshList();
        break;
      case MESSAGE_CANCEL_PROGRESS :
        hideLoadingDialog();
        break;
     //case MESSAGE_LOAD_THUMBNAIL_END :
       // hideLoadingDialog();
       // break;

    }
  }

  private int mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;
  private void showDirectoryContents(DirectoryContents contents) {
    mDirectoryScanner = null;
    if (mCurrentContent == PAGE_CONTENT_SEARCH) {
      mSearchDirectory = null;
    }
    mListDir = contents.listDir;
    mListFile = contents.listFile;
    mFileListAdapter = new IconifiedTextListAdapter(mContext);
    mFileListAdapter.setListItems(mDirectoryEntries, mFileListView.hasTextFilter());
    mFileListAdapter.setHomePage(false);
    mFileListView.setAdapter(mFileListAdapter);
    mMultiSelectOkButton.setEnabled(false);
    mFileListAdapter.setOnMultiSelectListener(new MultiSelectListener() {
		
		public void onMultiSelectionChanged(int position) {
			// TODO Auto-generated method stub
			mMultiSelectOkButton.setEnabled(mFileListAdapter.isSomeFileSelected());
			if (mFileListAdapter.isSomeFileSelected()) {
				mFileListView.setOnItemClickListener(fileSelectListener);
			} else {
				mFileListView.setOnItemClickListener(fileListListener);
			}
		}
	});

    synchronized (mDLock) {
      mDirectoryEntries.ensureCapacity(mListDir.size() + mListFile.size());
      mDirectoryEntries.clear();
      addAllElements(mDirectoryEntries, mListDir);
      addAllElements(mDirectoryEntries, mListFile);
    }
    mFileListView.setOnItemClickListener(fileListListener);
    mFileListView.setTextFilterEnabled(true);
    if (mDirectoryEntries.isEmpty()) {
      if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
        outMultiSelectMode();
      }
    }

    // BEGIN MOTOROLA, prmq46, 06/29/2011, IKMAIN-22925 / add function used to remember the current select/open item in the filemanager
    if (isFiles) {
      mFileListView.setSelection(mPosition);
      isFiles = false;
    } else {
      selectInList(mPreviousDirectory);
    }
    // END IKMAIN-22925
    if (!mListDir.isEmpty() || !mListFile.isEmpty()) {
      mEmptyText.setVisibility(View.GONE);
    } else {
      mEmptyText.setVisibility(View.VISIBLE);
    }

    String sMatchesFor = getString(R.string.matches_for);
    String sFoundIn = getString(R.string.found_in);

    if (currentDirectory.getAbsolutePath().equals(getSdCardPath())) {
      if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD) {
        if (SdcardManager.hasInternalSdcard(this)) {
          if (mCurrentContent == PAGE_CONTENT_SEARCH) {
            mCurrentFolder.setText(sMatchesFor + " " + "\"" + mCurrentSearchedString + "\"" + " " +
                sFoundIn + " " + getString(R.string.internal_phone_storage));
            mFolderIcon.setVisibility(View.GONE);
            mEmptyText.setText(R.string.empty_search_result);
          } else {
            mCurrentFolder.setText(R.string.internal_phone_storage);
          }
        } else {
          if (mCurrentContent == PAGE_CONTENT_SEARCH) {
            mCurrentFolder.setText(sMatchesFor + " " + "\"" + mCurrentSearchedString + "\"" + " " +
                sFoundIn + " " + getString(R.string.sd_card));
            mFolderIcon.setVisibility(View.GONE);
            mEmptyText.setText(R.string.empty_search_result);
          } else {
            mCurrentFolder.setText(R.string.sd_card);
          }
        }
      } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD) {
        if (mCurrentContent == PAGE_CONTENT_SEARCH) {
          mCurrentFolder.setText(sMatchesFor + " " + "\"" + mCurrentSearchedString + "\"" + " " +
              sFoundIn + " " + getString(R.string.sd_card));
          mFolderIcon.setVisibility(View.GONE);
          mEmptyText.setText(R.string.empty_search_result);
        } else {
          mCurrentFolder.setText(R.string.sd_card);
        }

      } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_USB_DISK) {
        mCurrentFolder.setText(R.string.usb_files);
      }
    } else {
      String str = null;
      int position = 0;
      String directory = "";
      if (mCurrentContent == PAGE_CONTENT_SEARCH) {
        if (mCurrentSearchDirectory != null) {
          str = mCurrentSearchDirectory.getPath();
        }
        if (str != null) {
          position = str.lastIndexOf("/");
          if (position != -1) {
            directory = str.substring(position + 1);
          }
          if (str.equals(FileManagerApp.getInstance(mContext).SD_CARD_DIR)) {
            if (SdcardManager.hasInternalSdcard(this)) {
              FileManagerApp.log(TAG + "getEMMCEnabled is true");
              mCurrentFolder.setText(sMatchesFor + " " + "\"" + mCurrentSearchedString + "\"" +
                  " " + sFoundIn + " " + getString(R.string.internal_phone_storage));
            }
            else{
                mCurrentFolder.setText(sMatchesFor+" "
                  + "\""+mCurrentSearchedString +"\""
                  +" " + sFoundIn + " "
                  +getString(R.string.sd_card));
            }
          }
          else if (str.equals(FileManagerApp.getInstance(mContext).SD_CARD_EXT_DIR)){
              mCurrentFolder.setText(sMatchesFor+" "
                  + "\""+mCurrentSearchedString +"\""
                  +" " + sFoundIn + " "
                  +getString(R.string.sd_card));
          }
          else {
            mCurrentFolder.setText(sMatchesFor + " " + "\"" + mCurrentSearchedString + "\"" + " " +
                sFoundIn + " " + directory);
          }
          mFolderIcon.setVisibility(View.GONE);
          mEmptyText.setText(R.string.empty_search_result);
        }
      } else {
        String directoryName = "";
        mFolderIcon.setVisibility(View.VISIBLE);
        if (currentDirectory != null) {
          directory = currentDirectory.getAbsolutePath();
          directoryName = currentDirectory.getName();
        }
        if (directory.equals(FileManagerApp.getInstance(mContext).SD_CARD_DIR)) {
          if (SdcardManager.hasInternalSdcard(this)) {
            mCurrentFolder.setText(R.string.internal_phone_storage);
          } else {
            mCurrentFolder.setText(R.string.sd_card);
          }
        } else if (directory.equals(FileManagerApp.getInstance(mContext).SD_CARD_EXT_DIR)) {
          mCurrentFolder.setText(R.string.sd_card);
        } else {
          mCurrentFolder.setText(directoryName);
        }

      }
    }

    if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
      mCurrentFolder.setText(R.string.select_file);
    } else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
      mCurrentFolder.setText(R.string.select_folder);
     }

    mThumbnailLoader = new ThumbnailLoader(currentDirectory, mDirectoryEntries, currentHandler);
    if (mThumbnailLoader != null) {
      synchronized (mTLock) {
        mThumbnailLoader.setContext(mContext);
        mThumbnailLoader.start();
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
    outState.putString(BUNDLE_CONTEXT_FILE, mContextFile.getAbsolutePath());
    outState.putString(BUNDLE_CONTEXT_TEXT, mContextText);
  }

  private boolean isRootDirectory(String dir) {
    String rootStr = FileManagerApp.ROOT_DIR;
    if (mCreatedFromSearch) {
      rootStr = m_FirstSearchedDirectory;
    }
    if (dir == null || dir.equals(rootStr)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onSearchRequested() {
    Bundle searchedDirectory = new Bundle();
    searchedDirectory.putString(FileManagerActivity.SEARCHED_DIRECTORY, currentDirectory.getPath());
    startSearch(null, false, searchedDirectory, false);
    return true;
  }

  private boolean upOneLevel() {
    if (mCreatedFromSearch) {
      if (mCurrentContent == PAGE_CONTENT_SEARCH) {
        mPreviousContent = PAGE_CONTENT_NORMAL;
        mPreviousSearchedString = "";
        mPreviousSearchDirectory = null;
        if (mCurrentSearchDirectory != null) {
          String theDirectory = mCurrentSearchDirectory.getPath();
          FileManagerApp.log(TAG + "upOneLevel fromSearch activity, " +
              "mCurrentContent == PAGE_CONTENT_SEARCH" + " mCurrentSearchDirectory=" +
              mCurrentSearchDirectory + "theDirectory=" + theDirectory);
          if (theDirectory == null || theDirectory.equals(m_FirstSearchedDirectory)) {
            FileManagerApp.log(TAG + "upOneLevel fromSearch activity, " +
                "mCurrentContent == PAGE_CONTENT_SEARCH" +
                " theDirectory.equals(m_FirstSearchedDirectory), back to first instance");
            setBackFromSearch(PAGE_NORMAL_FROM_SEARCH);
            finish();
            return true;
          } else {
            FileManagerApp.log(TAG +
                "upOneLevel fromSearch activity, mCurrentContent == PAGE_CONTENT_SEARCH" +
                "Browse to " + mCurrentSearchDirectory);
            browseTo(mCurrentSearchDirectory);
            return true;
          }
        } else {
          FileManagerApp.log(TAG +
              "upOneLevel fromSearch activity, mCurrentContent == PAGE_CONTENT_SEARCH" +
              "mCurrentSearchDirectory is null");
          return false;
        }
      }
      if (mPreviousContent == PAGE_CONTENT_SEARCH) {
        // if current is normal, previous was search results
        if (mPreviousSearchDirectory != null) {
          FileManagerApp.log(TAG + "upOneLevel fromSearch activity, mPreviousSearchDirectory=" +
              mPreviousSearchDirectory + ", mPreviousSearchedString=" + mPreviousSearchedString);
          startSearchDirectory(mPreviousSearchDirectory, mPreviousSearchedString);
          return true;
        } else {
          FileManagerApp.log(TAG + "upOneLevel fromSearch activity, mPreviousSearchDirectory=null");
          // browseTo(currentDirectory);
          return false;
        }
      }
      FileManagerApp.log(TAG +
          "upOneLevel fromSearch activity, mPreviousContent == PAGE_CONTENT_NORMAL");
      mPreviousContent = PAGE_CONTENT_NORMAL;
      mPreviousSearchedString = "";
      mPreviousSearchDirectory = null;
      String theDirectory = null;

      if (currentDirectory != null) {
        theDirectory = currentDirectory.getParent();
      }
      FileManagerApp.log(TAG + "upOneLevel fromSearch activity, " +
          "mCurrentContent/mPreviousContent == PAGE_CONTENT_NORMAL" + " mCurrentSearchDirectory=" +
          mCurrentSearchDirectory + ",theDirectory=" + theDirectory);
      if (theDirectory == null || theDirectory.equals(m_FirstSearchedDirectory)) {
        if (theDirectory != null) {
          FileManagerApp.log(TAG + "upOneLevel fromSearch activity, PAGE_CONTENT_NORMAL," +
              "currentDirectory.getParent()=" + theDirectory + " is root dir");
        } else {
          FileManagerApp.log(TAG + "upOneLevel fromSearch activity, PAGE_CONTENT_NORMAL," +
              "currentDirectory.getParent()=null is root dir");
        }
        finish();
        return true;
      } else {
        if (currentDirectory != null && currentDirectory.getParentFile() != null) {
          browseTo(currentDirectory.getParentFile());
          FileManagerApp
              .log(TAG + "upOneLevel fromSearch activity, PAGE_CONTENT_NORMAL, browsed to " +
                  currentDirectory);
          return true;
        }
      }
    } else {
      FileManagerApp.log(TAG +
          "upOneLevel Normal activity, mPreviousContent == PAGE_CONTENT_NORMAL");
      mPreviousContent = PAGE_CONTENT_NORMAL;
      mPreviousSearchedString = "";
      mPreviousSearchDirectory = null;
      if (mCreatedFromSearchSuggestion == true) {
          FileManagerApp.log(TAG+"upOneLevel, page create from search suggestion," +
              " will back to the page before search started");
          setBackFromSearch(PAGE_NORMAL_FROM_SEARCH);
          finish();
          return false;
      }
      if (currentDirectory != null && isRootDirectory(currentDirectory.getParent())) {
        FileManagerApp.log(TAG + "upOneLevel Normal activity, PAGE_CONTENT_NORMAL," +
            "currentDirectory.getParent()=" + currentDirectory.getParent() + " is root dir");
        finish();
        return false;
      } else {
        if (currentDirectory != null && currentDirectory.getParentFile() != null) {
          browseTo(currentDirectory.getParentFile());
          FileManagerApp.log(TAG + "upOneLevel Normal activity, PAGE_CONTENT_NORMAL, browsed to " +
              currentDirectory);
          return true;
        }
      }
    }
    return false;
  }

  private void browseTo(final File aDirectory) {
    if (aDirectory == null) {
      FileManagerApp.log(TAG + "browseTo null directory, finish the activity");
      finish();
      return;
    }
    // BEGIN MOTOROLA, prmq46, 06/29/2011, IKMAIN-22925 / add function used to remember the current select/open item in the filemanager
    isFiles=false;
    // END IKMAIN-22925
    mCurrentContent = PAGE_CONTENT_NORMAL;
    mCurrentSearchedString = "";
    mCurrentSearchDirectory = null;

    if (aDirectory.equals(currentDirectory)) {
      refreshList();
    } else {
      mPreviousDirectory = currentDirectory;
      currentDirectory = aDirectory;
      refreshList();
    }
    if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
      outMultiSelectMode();
    }
  }

  private class OpenFileTask extends AsyncTask<File, Void, Boolean> {
    File mFile;
    Intent mIntent = null;
    Bitmap mCroppedImage = null;

    protected Boolean doInBackground(File... files) {
      Boolean result = false;
      // BEGIN MOTOROLA, prmq46, 06/29/2011, IKMAIN-22925 / add function used to remember the current select/open item in the filemanager
      isFiles=true;
      // END IKMAIN-22925
      mFile = files[0];
      if (mFile.exists()) {
        if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
          FileManager.rtnPickerResult(mFile.getAbsolutePath(), null);
        } else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
          // return;
        } else {
          result = true;
          mIntent = new Intent(android.content.Intent.ACTION_VIEW);
          Uri data = FileUtils.getUri(mFile);
          String type = MimeTypeUtil.getMimeType(getBaseContext(), mFile.getName());
          // try to get media file content uri
          if (type != null) {
            /*if(type.indexOf("video") == 0) {
              String ext = FileUtils.getExtension(mFile.getName());
              if(FileUtils.needQueryDB(ext.toLowerCase())){
                boolean t = FileUtils.isInAudioDatabase(mContext, mFile.getPath());
                if(t) type = "audio/*";
              }
            }*/
            if (type.indexOf("audio") == 0) {
              long id = getMediaContentIdByFilePath(getBaseContext(), mFile.getAbsolutePath(), 1);
              if (id > 0) {
                data = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                FileManagerApp.log(TAG + "openFile: content uri: " + data + " " + type);
              }
            } else if (type.indexOf("image") == 0) {
              long id = getMediaContentIdByFilePath(getBaseContext(), mFile.getAbsolutePath(), 0);
              if (id > 0) {
                data = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                FileManagerApp.log(TAG + "openFile: content uri: " + data + " " + type);
              }
            }
          }
          mIntent.setDataAndType(data, type);
          mIntent.putExtra("isFrom", "fileManager");
          if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_GET_CONTENT) {
            FileManagerApp.log(TAG + "FileManagerApp.mLaunchMode is SELECT_GET_CONTENT");
            Intent sIntent = ((FileManagerApp) getApplication()).getContentIntent();
            Bundle myExtras = sIntent.getExtras();
            // special case for handle contact pick picture from files
            if (myExtras != null &&
                (myExtras.getParcelable("data") != null || myExtras.getBoolean("return-data"))) {
              mCroppedImage = cropImageFromImageFile(mFile.getPath(), myExtras);
            }
          }
        }
      }
      return result;
    }

    protected void onPostExecute(Boolean result) {
      // Were we in GET_CONTENT mode?
      if (result == false) {
        return;
      }
      if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_GET_CONTENT) {
        /*
         * FileManagerApp .log(TAG +
         * "FileManagerApp.mLaunchMode is SELECT_GET_CONTENT"); Intent sIntent =
         * ((FileManagerApp) getApplication()).getContentIntent(); Bundle
         * myExtras = sIntent.getExtras(); // special case for handle contact
         * pick picture from files if (myExtras != null &&
         * (myExtras.getParcelable("data") != null || myExtras
         * .getBoolean("return-data"))) { Bitmap croppedImage =
         * cropImageFromImageFile(mFile.getPath(), myExtras);
         */
         String packageName = ((FileManagerApp)getApplication()).getCallingPackageName();
         FileManagerApp.log(TAG + "FileManagerApp.onPostExecute: Package Name called us is " + packageName);

         if (mCroppedImage != null) {
          Bundle extras = new Bundle();
          extras.putParcelable("data", mCroppedImage);
          Intent contactIntent = (new Intent()).setAction("inline-data");
          contactIntent.putExtras(extras);
          FileManager.rtnPickerResult(contactIntent);
        }

        else if(packageName != null && packageName.equals("com.android.contacts")) {
          Toast.makeText(getBaseContext(), R.string.crop_Image_error,
          Toast.LENGTH_SHORT).show();
        }
        FileManager.rtnPickerResult(mIntent);
        return;
      }

      try {
        startActivity(mIntent);
      } catch (ActivityNotFoundException e) {
        Toast.makeText(getBaseContext(), R.string.application_not_available,
        Toast.LENGTH_SHORT).show();
      };
    }

  }

  private Bitmap cropImageFromImageFile(String filePath, Bundle myExtras) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(filePath, options);
    int totalSize = (options.outHeight) * (options.outWidth) * 2;
    int downSamplingRate = 1;
    while (totalSize > 2 * 1024 * 1024) {
      downSamplingRate *= 2;
      totalSize /= 4;
    }
    options = new BitmapFactory.Options();
    options.inJustDecodeBounds = false;
    options.inSampleSize = downSamplingRate;
    options.outHeight = 480;
    options.outWidth = 640;
    Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
    if (bitmap != null) {
      Bitmap croppedImage =
          Bitmap.createScaledBitmap(bitmap, myExtras.getInt("outputX", 96),
              myExtras.getInt("outputY", 96), false);
      if (croppedImage != null) {
        FileManagerApp.log(TAG + "croppedImage is: " + croppedImage.getHeight());
        return croppedImage;
      } else {
        FileManagerApp.log(TAG + "croppedImage is null");
      }
    } else {
      FileManagerApp.log(TAG + "srcImage is null");
    }
    return null;
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
    } catch (UnsupportedOperationException ex) {
      ex.printStackTrace();
    } finally {
      if (cs != null) {
        cs.close();
        cs = null;
      }
    }

    return id;
  }

  private void startSearchDirectory(File searchedDirectory, String searchedString) {
    if ((searchedString == null) || (searchedString.length() == 0)) {
      FileManagerApp.log(TAG + "startSearchDirectory searchedString is blank");
      return;
    }
    if (searchedDirectory != null) {
      stopSearchThread();
      FileManagerApp.log(TAG + "startSearchDirectory searchedDirectory is NOT null");
      mSearchDirectory =
          new SearchDirectory(searchedDirectory, this, currentHandler, searchedString);
      if (mSearchDirectory != null) {
        synchronized (sLock) {
          mSearchDirectory.start();
        }
      }
      mCurrentContent = PAGE_CONTENT_SEARCH;
      mCurrentSearchedString = searchedString;
      mCurrentSearchDirectory = searchedDirectory;
      m_FirstSearchedDirectory = searchedDirectory.getPath();
      mPreviousContent = PAGE_CONTENT_NORMAL;
      mPreviousSearchedString = "";
      mPreviousSearchDirectory = null;
    } else {
      FileManagerApp.log(TAG + "startSearchDirectory searchedDirectory is null");
    }
  }

  private void updateAdapter(boolean delay) {
    FileManagerApp.log(TAG + " updateAdapter !");
    if (mFileListAdapter != null) {
      if (delay) {
        currentHandler.postDelayed(FileViewRequestFocusRunnalbe, 1000);
      } else {
        runOnUiThread(FileViewRequestFocusRunnalbe);
      }
    }
  }

  private Runnable FileViewRequestFocusRunnalbe = new Runnable() {
    // @Override
    public void run() {
      if (mFileListAdapter != null) {
        mFileListAdapter.notifyDataSetChanged();
      }
    }
  };

  private void refreshList() {
    // Stop the scanner
    stopActivityThread();
    FileManagerApp.log(TAG + "Refresh List");
    // Start a new scanner
    mDirectoryScanner = new DirectoryScanner(currentDirectory, this, currentHandler);
    if (mDirectoryScanner != null) {
      synchronized (mDLock) {
        mDirectoryScanner.start();
      }
    }
  }

  private void selectInList(File selectFile) {
    IconifiedTextListAdapter la = (IconifiedTextListAdapter) mFileListView.getAdapter();
    if (selectFile == null || la == null) {
      return;
    }
    String filename = selectFile.getName();
    int count = la.getCount();
    for (int i = 0; i < count; i++) {
      IconifiedText it = (IconifiedText) la.getItem(i);
      if (it.getText().equals(filename)) {
        mFileListView.setSelection(i);
        break;
      }
    }
  }

  private void addAllElements(List<IconifiedText> addTo, List<IconifiedText> addFrom) {
    int size = addFrom.size();
    for (int i = 0; i < size; i++) {
      addTo.add(addFrom.get(i));
    }
    updateAdapter(false);
  }

  AdapterView.OnItemClickListener fileSelectListener = new AdapterView.OnItemClickListener() {

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onItemClick() arg2:"+arg2+"  arg3:"+arg3);
		((IconifiedText)mFileListAdapter.getItem(arg2)).setChecked(!((IconifiedText)mFileListAdapter.getItem(arg2)).isChecked());
		mMultiSelectOkButton.setEnabled(mFileListAdapter.isSomeFileSelected());
		if (!mFileListAdapter.isSomeFileSelected()) {
			mFileListView.setOnItemClickListener(fileListListener);
		}
		
		mFileListView.setItemChecked(arg2, mFileListView.isItemChecked(arg2));

	}
};
  
  AdapterView.OnItemClickListener fileListListener = new AdapterView.OnItemClickListener() {
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) parent.getAdapter();
      if (adapter == null) {
        return;
      }
      // BEGIN MOTOROLA, prmq46, 06/29/2011, IKMAIN-22925 / add function used to remember the current select/open item in the filemanager
      mPosition = position;
      // END IKMAIN-22925
      IconifiedText text = (IconifiedText) adapter.getItem(position);
      String file = text.getText();
      String curdir = text.getPathInfo();
      File clickedFile = FileUtils.getFile(curdir);
      if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
        adapter.setItemChecked(position);
        updateMultiPanel();
      } else {
        if (clickedFile != null) {
          if (clickedFile.isDirectory()) {
            if (mCurrentContent == PAGE_CONTENT_SEARCH) {
              FileManagerApp.log(TAG +
                  "onItemClick, mCurrentContent == PAGE_CONTENT_SEARCH, file=" + file);
              mPreviousContent = PAGE_CONTENT_SEARCH;
              mPreviousSearchedString = mCurrentSearchedString;
              mPreviousSearchDirectory = mCurrentSearchDirectory;
            } else {
              mPreviousContent = PAGE_CONTENT_NORMAL;
              mPreviousSearchedString = "";
              mPreviousSearchDirectory = null;
            }
            browseTo(clickedFile);
          } else {
            adapter.setItemChecked(position);
            mMultiSelectOkButton.setEnabled(mFileListAdapter.isSomeFileSelected());
            if (mFileListAdapter.isSomeFileSelected()) {
    			mFileListView.setOnItemClickListener(fileSelectListener);
    		}
          }
        }
      }
    }
  };

  private String getSdCardPath() {
    return FileManagerApp.getPhoneStoragePath();
  }

  /*
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    //IKDINARATD-457, a22121, 2011-07-05, start   
    if (mIsViewFolderAction){
 
        menu.add(0, OPT_MENU_VIEW_SWITCH, 0, R.string.menu_view_grid).setIcon(
            R.drawable.ic_menu_grid_view);
        menu.add(0, OPT_MENU_SORT, 0, R.string.menu_sort_by).setIcon(
            R.drawable.ic_menu_sort_by);
 
       return true;
    }
    //IKDINARATD-457 end
    
    // Create all menus regardless of the state and hide them as needed in
    // onprepareoptions
    menu.add(0, OPT_MENU_NEW_FOLDER, 0, R.string.menu_new_folder).setIcon(
        android.R.drawable.ic_menu_add);
    menu.add(0, OPT_MENU_COPY, 0, R.string.menu_copy_selected).setIcon(R.drawable.ic_menu_copy);
    menu.add(0, OPT_MENU_ZIP, 0, R.string.menu_zip_selected).setIcon(R.drawable.ic_menu_zip);
    menu.add(0, OPT_MENU_RENAME, 0, R.string.menu_rename_selected).setIcon(
        android.R.drawable.ic_menu_edit);
    menu.add(0, OPT_MENU_OPEN, 0, R.string.menu_open_selected).setIcon(R.drawable.ic_menu_view);
    menu.add(0, OPT_MENU_INFO, 0, R.string.menu_info_selected).setIcon(
        android.R.drawable.ic_menu_info_details);
    menu.add(0, OPT_MENU_MULTISELECT, 0, R.string.menu_multi_select).setIcon(
        R.drawable.ic_menu_select_items);
    menu.add(0, OPT_MENU_SELECTALL, 0, R.string.menu_select_all).setIcon(
        R.drawable.ic_menu_select_items);
    menu.add(0, OPT_MENU_UNSELECTALL, 0, R.string.menu_unselect_all).setIcon(
        R.drawable.ic_menu_unselect_items);
    menu.add(0, OPT_MENU_VIEW_SWITCH, 0, R.string.menu_view_grid).setIcon(
        R.drawable.ic_menu_grid_view);
    menu.add(0, OPT_MENU_SORT, 0, R.string.menu_sort_by).setIcon(R.drawable.ic_menu_sort_by);
    menu.add(0, OPT_MENU_SEARCH, 0, R.string.menu_search).setIcon(R.drawable.ic_menu_search);
    return true;
  }
  */

  /*
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (FileManagerApp.getLaunchMode() != FileManagerApp.NORMAL_MODE ||
        mCurrentContent == PAGE_CONTENT_SEARCH) {
      menu.setGroupVisible(0, false);
      return true;
    }
    //IKDINARATD-457, a22121, 2011-07-05, start 
    if (mIsViewFolderAction){
        if (FileManagerApp.isGridView()) {
            menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(R.drawable.ic_menu_list_view);
            menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(R.string.menu_view_list);
        } else {
            menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(R.drawable.ic_menu_grid_view);
            menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(R.string.menu_view_grid);
        }
        return true;
    }
    //IKDINARATD-457 end

    if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
      menu.findItem(OPT_MENU_NEW_FOLDER).setVisible(true);
      menu.findItem(OPT_MENU_MULTISELECT).setVisible(false);
      // Start IKSTABLEFIVE-5633 - Hide Select All Menu if all is selected
      if (((IconifiedTextListAdapter) mFileListView.getAdapter()).isAllFilesSelected()) {
        menu.findItem(OPT_MENU_SELECTALL).setVisible(false);
      } else {
        menu.findItem(OPT_MENU_SELECTALL).setVisible(true);
      }
      // End IKSTABLEFIVE-5633
      if (((IconifiedTextListAdapter) mFileListView.getAdapter()).isSomeFileSelected()) {
        menu.findItem(OPT_MENU_COPY).setVisible(true);
        menu.findItem(OPT_MENU_ZIP).setVisible(true);
        menu.findItem(OPT_MENU_UNSELECTALL).setVisible(true);
      } else {
        menu.findItem(OPT_MENU_COPY).setVisible(false);
        menu.findItem(OPT_MENU_ZIP).setVisible(false);
        menu.findItem(OPT_MENU_UNSELECTALL).setVisible(false);
      }
      if (((IconifiedTextListAdapter) mFileListView.getAdapter()).isSingleFileSelected()) {
        ArrayList<String> selectedFiles = null;
        String[] inputFileNames = null;
        selectedFiles = ((IconifiedTextListAdapter) mFileListView.getAdapter()).getSelectFiles();
        inputFileNames = new String[selectedFiles.size()];
        selectedFiles.toArray(inputFileNames);
      }
      // Approved by CXD that rename, open, info shouldn't be displyed in multiselect 
      menu.findItem(OPT_MENU_RENAME).setVisible(false);
      menu.findItem(OPT_MENU_OPEN).setVisible(false);
      menu.findItem(OPT_MENU_INFO).setVisible(false);
      menu.findItem(OPT_MENU_VIEW_SWITCH).setVisible(false);
      menu.findItem(OPT_MENU_SORT).setVisible(false);
    } else {
      if (mCurrentContent == PAGE_CONTENT_NORMAL) {
        menu.findItem(OPT_MENU_NEW_FOLDER).setVisible(true);
        boolean temp =
            (mFileListView.getAdapter() != null) && (mFileListView.getAdapter().getCount() > 0);
        if ((FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) && temp) {
          menu.findItem(OPT_MENU_MULTISELECT).setVisible(true);
        } else {
          menu.findItem(OPT_MENU_MULTISELECT).setVisible(false);
        }
        menu.findItem(OPT_MENU_SELECTALL).setVisible(false);
        menu.findItem(OPT_MENU_UNSELECTALL).setVisible(false);
        menu.findItem(OPT_MENU_COPY).setVisible(false);
        menu.findItem(OPT_MENU_ZIP).setVisible(false);
        menu.findItem(OPT_MENU_OPEN).setVisible(false);
        menu.findItem(OPT_MENU_INFO).setVisible(false);
        menu.findItem(OPT_MENU_RENAME).setVisible(false);
        menu.findItem(OPT_MENU_VIEW_SWITCH).setVisible(true);
        menu.findItem(OPT_MENU_SORT).setVisible(true);
      }
    }
    if (FileManagerApp.isGridView()) {
      if ((menu.findItem(OPT_MENU_VIEW_SWITCH)) != null) {
        menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(R.drawable.ic_menu_list_view);
        menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(R.string.menu_view_list);
      }
    } else {
      if (mCurrentContent == PAGE_CONTENT_NORMAL) {
        if ((menu.findItem(OPT_MENU_VIEW_SWITCH)) != null) {
          menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(R.drawable.ic_menu_grid_view);
          menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(R.string.menu_view_grid);
        }
      }
    }
    return true;
  }
  */

  /*
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    mContextFile = currentDirectory;
    mContextText = mContextFile.getName();
    switch (item.getItemId()) {
      case OPT_MENU_NEW_FOLDER :
        showDialog(DIALOG_NEW_FOLDER);
        return true;
      case OPT_MENU_COPY :
        if (mFileListView.getAdapter() != null) {
          ArrayList<String> tempCopy =
              ((IconifiedTextListAdapter) (mFileListView.getAdapter())).getSelectFiles();
          copy(tempCopy);
          outMultiSelectMode();
        }
        return true;
      case OPT_MENU_ZIP :
        showDialog(DIALOG_ZIP);
        return true;
      case OPT_MENU_RENAME :
        showDialog(DIALOG_RENAME);
        return true;
      case OPT_MENU_OPEN :
        if (mFileListView.getAdapter() != null) {
          ArrayList<String> tempOpen =
              ((IconifiedTextListAdapter) (mFileListView.getAdapter())).getSelectFiles();
          if (tempOpen.size() > 0) {
            String filename = tempOpen.get(0);
            File tempFile = new File(filename);
            if (tempFile.isDirectory()) {
              browseTo(tempFile);
            } else {
              new OpenFileTask().execute(tempFile);
            }
          }
        }
        return true;
      case OPT_MENU_INFO :
        if (mFileListView.getAdapter() != null) {
          ArrayList<String> tempOpen =
              ((IconifiedTextListAdapter) (mFileListView.getAdapter())).getSelectFiles();
          if (tempOpen.size() > 0) {
            String filename = tempOpen.get(0);
            File tempFile = new File(filename);
            IconifiedText tempItem = IconifiedText.buildIconItem(this, tempFile);
            showItemInfo(tempItem);
          }
        }
        return true;
      case OPT_MENU_SELECTALL :
        ((IconifiedTextListAdapter) mFileListView.getAdapter()).selectAll();
        updateMultiPanel();
        return true;
      case OPT_MENU_UNSELECTALL :
        ((IconifiedTextListAdapter) mFileListView.getAdapter()).unSelectAll();
        updateMultiPanel();
        return true;
      case OPT_MENU_MULTISELECT :
        setMultiSelectMode();
        return true;
      case OPT_MENU_VIEW_SWITCH :
        switchView();
        return true;
      case OPT_MENU_SORT :
        showSortMethodDialog();
        return true;
      case OPT_MENU_SORT_BY_NAME :
        ((FileManagerApp) getApplication()).setSortMode(FileManagerApp.NAMESORT);
        refreshList();
        return true;
      case OPT_MENU_SORT_BY_TYPE :
        ((FileManagerApp) getApplication()).setSortMode(FileManagerApp.TYPESORT);
        refreshList();
        return true;
      case OPT_MENU_SORT_BY_TIME :
        ((FileManagerApp) getApplication()).setSortMode(FileManagerApp.TIMESORT);
        refreshList();
        return true;
      case OPT_MENU_SORT_BY_SIZE :
        ((FileManagerApp) getApplication()).setSortMode(FileManagerApp.SIZESORT);
        refreshList();
        return true;
      case OPT_MENU_SEARCH :
        onSearchRequested();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
  */

  private void setMultiSelectMode() {
    mSelectMode = IconifiedTextListAdapter.MULTI_SELECT_MODE;
    if (!isMcUploadMode()) {
      mMultiSelectedPanel.setVisibility(View.VISIBLE);
    } else {
      if (mFileListView.getAdapter() != null) {
        ((IconifiedTextListAdapter) mFileListView.getAdapter())
            .setFilesOnlySelectMode(true);
      }
    }
    if (mFileListView.getAdapter() != null) {
      ((IconifiedTextListAdapter) mFileListView.getAdapter())
          .setSelectMode(IconifiedTextListAdapter.MULTI_SELECT_MODE);
    }
  }

  private void outMultiSelectMode() {
    if (mFileListView.getAdapter() != null) {
      ((IconifiedTextListAdapter) mFileListView.getAdapter())
          .setFilesOnlySelectMode(false);
      ((IconifiedTextListAdapter) mFileListView.getAdapter())
          .setSelectMode(IconifiedTextListAdapter.NONE_SELECT_MODE);
      ((IconifiedTextListAdapter) mFileListView.getAdapter()).unSelectAll();
    }
//    mMultiSelectedPanel.findViewById(R.id.button_multi_confirm).setEnabled(false);
//    mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;
  }

  MenuItem.OnMenuItemClickListener conMenuListener = new MenuItem.OnMenuItemClickListener() {
    public boolean onMenuItemClick(MenuItem item) {
      AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
      IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) mFileListView.getAdapter();
      if (adapter == null) {
        return false;
      }
      IconifiedText ic = (IconifiedText) adapter.getItem(menuInfo.position);
      mContextText = ic.getPathInfo();
      mContextFile = FileUtils.getFile(ic.getPathInfo());
      switch (item.getItemId()) {
        case CON_MENU_OPEN :
          if (mContextFile.isDirectory()) {
            browseTo(mContextFile);
          } else {
            new OpenFileTask().execute(mContextFile);
          }
          return true;
        case CON_MENU_MOVE :
          ArrayList<String> selectedMove = new ArrayList<String>();
          selectedMove.add(mContextFile.getAbsolutePath());
          move(selectedMove);
          return true;
        case CON_MENU_COPY :
          ArrayList<String> selectedCopy = new ArrayList<String>();
          selectedCopy.add(mContextFile.getAbsolutePath());
          copy(selectedCopy);
          return true;
        case CON_MENU_DELETE :
          showDialog(DIALOG_DELETE);
          return true;
        case CON_MENU_RENAME :
          showDialog(DIALOG_RENAME);
          return true;
        case CON_MENU_SEND :
          new SendFileTask().execute(mContextFile);
          return true;
        case CON_MENU_INFO :
          showItemInfo(ic);
          return true;
        case  CON_MENU_SET_AS_CALL_RINGTONE:  
          if(mSupportDualMode){//dual mode,show dialog to select network
            showDialog(DIALOG_CALL_RINGTONE);
          }else{//single mode,set ringtone directly
            FileUtils.setAs(FileManagerActivity.this, mContextFile, FileUtils.GSM_CALL_RINGTONE);
          }
          return true;
        case CON_MENU_SET_AS_MESSAGE_RINGTONE:   
          if(mSupportDualMode){
            showDialog(DIALOG_MESSAGE_RINGTONE);
          }else{
            FileUtils.setAs(FileManagerActivity.this, mContextFile, FileUtils.GSM_MESSAGE_RINGTONE);
          }
          return true;
        //IKCHNDEV-632--end---
        // IKAROWANA-81 end tqr743	
        case CON_MENU_PRINT :
          printFile(mContextFile);
          return true;
        case CON_MENU_UPDATE_PACKAGE:
          handleUpdateFile(mContextFile);
          return true;
        case CON_MENU_OPEN_WITH:
          openFileWith(mContextFile);
          return true;
      }
      return false;
    }
  };

  public Dialog infoDialog = null;

  private void showItemInfo(IconifiedText item) {
    View view_info = LayoutInflater.from(this).inflate(R.layout.file_info, null);
    final Dialog infoDialog = new AlertDialog.Builder(this).setView(view_info).create();
    infoDialog.setTitle(R.string.file_info_title);

    ((ImageView) view_info.findViewById(R.id.item_info_icon)).setImageDrawable(item.getIcon());

    File file = FileUtils.getFile(item.getPathInfo());

    ((TextView) view_info.findViewById(R.id.item_name)).setText(file.getName());
    ((TextView) view_info.findViewById(R.id.item_type)).setText(item.getTypeDesc(mContext));

    if (file.isDirectory()) {
      infoDialog.setTitle(R.string.file_info_title_dir);
      ((TextView) view_info.findViewById(R.id.item_folder)).setText(this.getResources().getString(
          R.string.folder) +
          file.getParent());
      File[] infolderFiles = file.listFiles();
      int countFolder = 0, countFile = 0;
      if (infolderFiles != null) {
        for (File temp : infolderFiles) {
          if (temp.isDirectory()) {
            countFolder++;
          } else {
            countFile++;
          }
        }
      }
      // IKMAIN-17648 - If contains 1 folder and one file display the right
      // string
      String s_contains = null;
      if ((countFolder > 1 || countFolder == 0) && (countFile > 1 || countFile == 0)) {
        s_contains = this.getResources().getString(R.string.contains, countFolder, countFile);
      } else if (countFolder == 1 && countFile == 1) {
        s_contains =
            this.getResources().getString(R.string.contains_one_folder_one_file, countFolder,
                countFile);
      } else if ((countFolder > 1 || countFolder == 0) && countFile == 1) {
        s_contains =
            this.getResources().getString(R.string.contains_folders_one_file, countFolder,
                countFile);
      } else if (countFolder == 1 && (countFile > 1 || countFile == 0)) {
        s_contains =
            this.getResources().getString(R.string.contains_one_folder_files, countFolder,
                countFile);
      }

      ((TextView) view_info.findViewById(R.id.item_size)).setText(s_contains);
    } else {
      ((TextView) view_info.findViewById(R.id.item_folder)).setText(this.getResources().getString(
          R.string.folder) +
          file.getParent());
      // Calculate the file size. If reminder is more than 0 we should show
      // increase the size by 1, otherwise we should show the size as it is.
      long size = file.length() / 1024;
      long rem = file.length() % 1024;
      if (rem > 0) {
        size++;
      }
      String fileSize = this.getResources().getString(R.string.size_kb, Long.toString(size));
      ((TextView) view_info.findViewById(R.id.item_size)).setText(this.getResources().getString(
          R.string.size) +
          fileSize);
    }

    Date date = new Date(file.lastModified());
    DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mContext);
    DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(mContext);
    String format = dateFormat.format(date) + " " + timeFormat.format(date);
    ((TextView) view_info.findViewById(R.id.item_last_modified)).setText(this.getResources()
        .getString(R.string.last_modified_at) + format);
    /*
     * .getString(R.string.last_modified_at) +
     * this.getResources().getString(R.string.unknown)); } else {
     * ((TextView)view_info
     * .findViewById(R.id.item_last_modified)).setText(this.getResources()
     * .getString(R.string.last_modified_at) + time.toLocaleString()); }
     */

    if (file.canRead()) {
      ((TextView) view_info.findViewById(R.id.read_auth)).setText(R.string.readable_yes);
    } else {
      ((TextView) view_info.findViewById(R.id.read_auth)).setText(R.string.readable_no);
    }

    if (file.canWrite()) {
      ((TextView) view_info.findViewById(R.id.write_auth)).setText(R.string.writable_yes);
    } else {
      ((TextView) view_info.findViewById(R.id.write_auth)).setText(R.string.writable_no);
    }

    if (file.isHidden()) {
      ((TextView) view_info.findViewById(R.id.hidden_status)).setText(R.string.hidden_yes);
    } else {
      ((TextView) view_info.findViewById(R.id.hidden_status)).setText(R.string.hidden_no);
    }

    ((Button) view_info.findViewById(R.id.item_info_ok))
        .setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            if (infoDialog != null) {
              infoDialog.dismiss();
            }
          }
        });
    infoDialog.show();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    AlertDialog dialog = null;
    switch (id) {
      case DIALOG_NEW_FOLDER :
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_new_folder, null);
        final EditText et = (EditText) view.findViewById(R.id.foldername);
        et.setText("");
        dialog =
            new AlertDialog.Builder(this).setTitle(R.string.create_new_folder).setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    createNewFolder(et.getText().toString());
                  }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                  }
                }).create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        break;

      case DIALOG_DELETE :
        dialog =
            new AlertDialog.Builder(this).setTitle(getString(R.string.delet))
                .setIcon(android.R.drawable.ic_dialog_alert).setMessage(" ")
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    ArrayList<String> deletFiles = null;
                    if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
                      if (mFileListView.getAdapter() != null) {
                        deletFiles =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                .getSelectFiles();
                      }
                    } else {
                      deletFiles = new ArrayList<String>();
                      deletFiles.add(mContextFile.getAbsolutePath());
                    }
                    if (deletFiles != null) {
                      deleteSelected(deletFiles);
                    }
                  }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                  }
                }).create();
        break;
      case DIALOG_RENAME :
        inflater = LayoutInflater.from(this);
        view = inflater.inflate(R.layout.dialog_new_folder, null);
        final EditText et2 = (EditText) view.findViewById(R.id.foldername);
        dialog =
            new AlertDialog.Builder(this).setTitle(R.string.menu_rename).setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
                      if (mFileListView.getAdapter() != null) {
                        String selectedFile =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                .getSelectFiles().get(0);
                        renameFileOrFolder(new File(selectedFile), et2.getText().toString());
                      }
                    } else {
                      renameFileOrFolder(mContextFile, et2.getText().toString());
                    }
                  }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                  }
                }).create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        break;
        case DIALOG_CALL_RINGTONE:
           return new AlertDialog.Builder(this)
               .setTitle(R.string.title_dlg_select_network)
               .setItems(R.array.network_type, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    FileManagerApp.log(TAG+" DIALOG_CALL_RINGTONE, which="+which);
                    switch (which){              
                      case CDMA_NETWORK:
                         FileUtils.setAs(FileManagerActivity.this, mContextFile,FileUtils.CDMA_CALL_RINGTONE);
                         break;
                      case GSM_NETWORK:
                         FileUtils.setAs(FileManagerActivity.this, mContextFile ,FileUtils.GSM_CALL_RINGTONE);
                         break;
                      default:
                         break;
                    }
                }
          }).create();
      case DIALOG_MESSAGE_RINGTONE:
           return new AlertDialog.Builder(this)
               .setTitle(R.string.title_dlg_select_network)
               .setItems(R.array.network_type, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {
                   FileManagerApp.log(TAG+" DIALOG_MESSAGE_RINGTONE, which="+which);
                     switch (which){              
                        case CDMA_NETWORK:
                            FileUtils.setAs(FileManagerActivity.this, mContextFile , FileUtils.CDMA_MESSAGE_RINGTONE);
                            break;
                        case GSM_NETWORK:
                            FileUtils.setAs(FileManagerActivity.this, mContextFile , FileUtils.GSM_MESSAGE_RINGTONE);
                            break;
                        default:
                            break;
                      }
                   }
              }).create();
    }
    return dialog;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    super.onPrepareDialog(id, dialog, args);

    switch (id) {
      case DIALOG_NEW_FOLDER :
        EditText et = (EditText) dialog.findViewById(R.id.foldername);
        et.setText("");
        break;
      case DIALOG_DELETE :
        if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
          ((AlertDialog) dialog).setMessage(getString(R.string.really_delete_selected));
        } else {
          if (mContextFile.isDirectory()) {
            ((AlertDialog) dialog).setMessage(getString(R.string.really_delete_folder,
                mContextFile.getName()));
          } else {
            ((AlertDialog) dialog).setMessage(getString(R.string.really_delete,
                mContextFile.getName()));
          }
        }
        break;
      case DIALOG_RENAME :
        et = (EditText) dialog.findViewById(R.id.foldername);
        et.setText(mContextFile.getName());
        TextView tv = (TextView) dialog.findViewById(R.id.foldernametext);
        if (mContextFile.isDirectory()) {
          tv.setText(R.string.folder_name);
        } else {
          tv.setText(R.string.file_name);
        }
        break;
    }
  }

  private void createNewFolder(String foldername) {
    try {
      foldername = foldername.trim();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if(foldername.length() > 64 )//force the name length < 64
    {
       Toast.makeText(this, R.string.name_too_long, Toast.LENGTH_SHORT).show();
       foldername = foldername.substring(0,64);
       FileManagerApp.log(TAG +"the first 64 charaters of foldername = "+foldername);
    }
    if (!TextUtils.isEmpty(foldername)) {
      File file = FileUtils.getFile(currentDirectory, foldername);
      if (file.exists()) {
        Toast.makeText(this, R.string.renaming_folder_exist, Toast.LENGTH_SHORT).show();
        return;
      }
      if (file.mkdirs()) {
        browseTo(file.getParentFile());
      } else {
        Toast.makeText(this, R.string.error_creating_new_folder, Toast.LENGTH_SHORT).show();
      }
    }
    // If Folder name is empty
    else {
      Toast.makeText(this, R.string.renaming_folder_empty, Toast.LENGTH_SHORT).show();
    }
  }

  private void deleteSelected(ArrayList<String> deleteFiles) {
    if ((deleteFiles != null) && (deleteFiles.size() > 0)) {
      // Pop-up deleting progress dialog
      showLoadingDialog(getString(R.string.deleting));
      new DeleteThread(deleteFiles).start();
    }
  }

  private class DeleteThread extends Thread {

    private ArrayList<String> mDeleteFiles;

    public DeleteThread(ArrayList<String> deleteFiles) {
      super();
      mDeleteFiles = deleteFiles;
    }

    @Override
    public void run() {
      boolean result = true;
      boolean folderDeleted = false;
      if (mDeleteFiles == null) {
        FileManagerApp.log(TAG + "DeleteThread mDeleteFiles is null, no operation to be done");
        return;
      }

      File file = null;
      for (String filename : mDeleteFiles) {
        file = new File(filename);
        if (file.exists()) {
          if (file.isDirectory()) {
            folderDeleted = true;
          }
          // checkFileToSetScanFlag(file);
          result = delete(file) && result;
        }
      }
      if (file != null) {
        // scan the parent folder of the file(s) just deleted
        File parentFile = file.getParentFile();
        if (parentFile != null) {
          if((FileManagerContent.getActivityGroup()) != null) {
              ((FileManagerContent) FileManagerContent.getActivityGroup()).mediaScanFolder("file://" 
                                                                                        + parentFile.getAbsolutePath());
          }else {
           //call private method to send the mediascan intent
           FileManagerApp.log(TAG +"ActivityGroup() is null,call private mediaScanFolder");
           mediaScanFolder("file://"+parentFile.getAbsolutePath());
          }
        }
      }

      if (result) {
        if (mDeleteFiles.size() > 1) {
          currentHandler.obtainMessage(MESSAGE_DELETE_FINISHED, ARG_FILES_DELETED, 0).sendToTarget();
        } else if (folderDeleted) {
          currentHandler.obtainMessage(MESSAGE_DELETE_FINISHED, ARG_FOLDER_DELETED, 0).sendToTarget();
        } else {
          currentHandler.obtainMessage(MESSAGE_DELETE_FINISHED, ARG_FILE_DELETED, 0).sendToTarget();
        }
      } else {
        if (mDeleteFiles.size() > 1 || folderDeleted) {
          currentHandler.obtainMessage(MESSAGE_DELETE_FINISHED, ARG_DELETE_MULTIPLE_FAILED, 0)
              .sendToTarget();
        } else {
          currentHandler.obtainMessage(MESSAGE_DELETE_FINISHED, ARG_DELETE_FAILED, 0).sendToTarget();
        }
      }
    }
  }
  
  // temporarily use to build success in eclipse
  // temp fix for porting. Remove comments later
  private static final String ACTION_MEDIA_SCANNER_SCAN_FOLDER =
      "com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER";

  private void mediaScanFolder(String folder) {
    if (folder != null) {
      if (folder.endsWith("/")) {
        // For scanning folders, MediaScanner does not like to have a trailing "/"
        // So if the input string has a trailing "/" remove it
        folder = folder.substring(0, (folder.length()-1));
      }
      Intent intent =
        new Intent(ACTION_MEDIA_SCANNER_SCAN_FOLDER, Uri.parse(folder));
      FileManagerApp.log(TAG + "mediaScanFolder " + folder + " " + intent.toString());
      try {
        this.sendBroadcast(intent);
      } catch (android.content.ActivityNotFoundException ex) {
        ex.printStackTrace();
      }
    }
  }

  private boolean delete(File file) {
    boolean result = true;

    if (file.isDirectory()) {
      File[] list = file.listFiles();
      if (list != null) {
        for (File tempFile : list) {
          result = delete(tempFile) && result;
          // Now that we have deleted the file, if this is media file we need to
          // delete from Media store
          // new DeleteFileFromMediaStoreTask().execute(tempFile);
        }
      }
      if (result) {
        try {
          result = file.delete() && result;
          // Send Intent to media scanner to scan the folder
        } catch (Exception e) {
          e.printStackTrace();
          result = false;
        }
      }
    } else {
      try {
        result = file.delete();
      } catch (Exception e) {
        e.printStackTrace();
        result = false;
      }
    }

    return result;
  }

  private void renameFileOrFolder(File file, String newFileName) {
    // If the user didn't put a name, toast a message letting him know for the
    // error.
    int toast;
    boolean result;
    if (mContextFile.isDirectory()) {
      toast = R.string.renaming_folder_empty;
    } else {
      toast = R.string.renaming_file_empty;
    }
    // Do not allow empty names or extension only names.
    // Names without extensions are allowed
    if (newFileName.isEmpty() || (newFileName.lastIndexOf(".") == 0)) {
      Toast.makeText(FileManagerActivity.this, toast, Toast.LENGTH_SHORT).show();
    } else {
      int filename_len = newFileName.length();
      if(filename_len > 64){
        int ext_index = newFileName.lastIndexOf(".");
        String ext_string = null;
        int ext_length = 0;
        if(ext_index != -1) {
          FileManagerApp.log(TAG +"ext_index != -1,has extension");
          ext_string = newFileName.substring(ext_index);//get the extersion;
          ext_length = ext_string.length();
          FileManagerApp.log(TAG +"ext_string ="+ext_string+" ext_length ="+ext_length);
        }
     
        Toast.makeText(this, R.string.name_too_long, Toast.LENGTH_SHORT).show();
        //three cases need to cut off file name directly.
        //1.it's folder   2.the name hase no "." 3.the "." at the last of name
        if(file.isDirectory() || ext_index == -1 || ext_index == filename_len)
        {
          newFileName = newFileName.substring(0,64);
          FileManagerApp.log(TAG +"newFileName ="+newFileName);
        } else {//has extersion
          if(ext_length > 63)
          {  ext_length = 63; }//extersion length can NOT > 63 characters
          int name_length = 64 - ext_length;
          FileManagerApp.log(TAG +"ext_length ="+ext_length+" name_length ="+name_length);
          ext_string = ext_string.substring(0, ext_length);
          newFileName = newFileName.substring(0, name_length);
          FileManagerApp.log(TAG +"filename is "+newFileName+", extersion is "+ext_string);
          newFileName = newFileName.concat(ext_string);
        }
      }
      if (file != null) {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
          File newFile = FileUtils.getFile(parentFile, newFileName);
          result = rename(file, newFile);
          if (result) { 
            //--------IKCHNDEV-3103----begin-------------------------------------
            // Send Intent to media scanner to scan the folder
            // Scan the parent directory if renamed item is a folder or file
            //--IKCHNDEV-3478---begin------
            if((FileManagerContent.getActivityGroup()) != null){  
              ((FileManagerContent) FileManagerContent.getActivityGroup())
                  .mediaScanFolder("file://" + currentDirectory.toString());
            }
            else {
           //call private method to send the mediascan intent
           FileManagerApp.log(TAG +"ActivityGroup() is null,call private mediaScanFolder");
           mediaScanFolder("file://"+parentFile.getAbsolutePath());
          }
          //--IKCHNDEV-3478---end------
          //--------IKCHNDEV-3103----end-------------------------------------
          }
        }
      }
    }
  }

  private void move(ArrayList<String> files) {
    Intent intent = new Intent(this, com.motorola.filemanager.multiselect.FileManagerContent.class);
    intent.setAction(FileManagerContent.ACTION_CUT);
    intent.putStringArrayListExtra(FileManagerContent.EXTRA_KEY_SOURCEFILENAME, files);
    startActivity(intent);
  }

  private void copy(ArrayList<String> files) {
    Intent intent = new Intent(this, com.motorola.filemanager.multiselect.FileManagerContent.class);
    intent.setAction(FileManagerContent.ACTION_COPY);
    intent.putStringArrayListExtra(FileManagerContent.EXTRA_KEY_SOURCEFILENAME, files);
    startActivity(intent);
  }

  private boolean rename(File oldFile, File newFile) {
    int toast = 0;
    boolean result = false;
    if (newFile.exists()) {
      Toast.makeText(this, R.string.filename_dup, Toast.LENGTH_SHORT).show();
    } else {
      if (oldFile.renameTo(newFile)) {
        if (currentDirectory.exists()) {
          refreshList();
        } else {
          browseTo(newFile);
        }
        if (newFile.isDirectory()) {
          toast = R.string.folder_renamed;
        } else {
          toast = R.string.file_renamed;
        }
        result = true;
      } else {
        if (oldFile.isDirectory()) {
          toast = R.string.error_renaming_folder;
        } else {
          toast = R.string.error_renaming_file;
        }
      }
      Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    return result;
  }

  private class SendFileTask extends AsyncTask<File, Void, Boolean> {
    Intent i = new Intent();
    File mFile;

    protected Boolean doInBackground(File... files) {
      Boolean result = false;
      mFile = files[0];
      if (mFile.exists()) {
        result = true;

        String mimeType = MimeTypeUtil.getMimeType(mContext, mFile.getName());
        long id = 0;
        Uri contentUri = null;
        int idx = -1;
        Uri mediaUri = null;
        i.setAction(Intent.ACTION_SEND);
        i.setType(mimeType);
        if (mimeType != null && (mimeType.indexOf("image") == 0)) {
          idx = 0;
          mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
          id = getMediaContentIdByFilePath(mContext, mFile.getAbsolutePath(), idx);
        } else if (mimeType != null && (mimeType.indexOf("audio") == 0)) {
          idx = 1;
          mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
          id = getMediaContentIdByFilePath(mContext, mFile.getAbsolutePath(), idx);
        } else if (mimeType != null && (mimeType.indexOf("video") == 0)) {
          idx = 2;
          mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
          id = getMediaContentIdByFilePath(mContext, mFile.getAbsolutePath(), idx);
        }

        if (id > 0) {
          contentUri = ContentUris.withAppendedId(mediaUri, id);
        } else {
          contentUri = Uri.fromFile(mFile);
        }

        String filename = mFile.getName();
        i.putExtra(Intent.EXTRA_SUBJECT, filename);
        i.putExtra(Intent.EXTRA_STREAM, contentUri);
        i = Intent.createChooser(i, getString(R.string.menu_send));
      }
      return result;
    }

    protected void onPostExecute(Boolean result) {
      // Were we in GET_CONTENT mode?
      if (result == false) {
        return;
      }
      try {
        startActivity(i);
      } catch (ActivityNotFoundException e) {
        Toast.makeText(getBaseContext(), R.string.send_not_available, Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      //IKDINARATD-457, a22121, 2011-07-05
      if (mIsViewFolderAction){
         finish();
         return true;
      }
      //IKDINARATD-457 end
      if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
        outMultiSelectMode();
        return true;
      } else {
        if (upOneLevel() == true) {
          return true;
        } else {
          FileManagerApp.log(TAG +
              "onKeyDown, upOneLevelreturned false, should close FileManager");
		    if (FileManagerContent.getActivityGroup() != null) {
			    (FileManagerContent.getActivityGroup()).finish();
			}
			if (FileManager.getInstance() != null) {
			    FileManager.getInstance().finish();
			}
          if (Import3rd.getInstance() != null) {
        	  Import3rd.getInstance().finish();
          }

            if (Import3rd2.getInstance() != null) {
                Import3rd2.getInstance().finish();
            }
			
		   finish();	
          return true; // super.onKeyDown(keyCode, event);
        }
      }
    }
    else if (keyCode ==KeyEvent.KEYCODE_MENU){
      if (event!=null){
        if (event.getRepeatCount() >0){
          return true;
        }
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private final BroadcastReceiver mSDCardReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action == null) {
        FileManagerApp.log(TAG + "intent action is null in BroadcastReceiver's onReceive");
        return;
      }

      if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
        if (!FileManagerActivity.this.isFinishing()) {
          mEmptyText.setText(R.string.sd_is_used_by_other);
          refreshList();
        }
      } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
        if (!FileManagerActivity.this.isFinishing()) {
          mEmptyText.setText(R.string.this_folder_is_empty);
          refreshList();
        }
      } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
        if (!FileManagerActivity.this.isFinishing()) {
          if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD) {
            mEmptyText.setText(R.string.internal_phone_storage_unmounted);
          } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD) {
            mEmptyText.setText(R.string.sd_unmounted);
          }
          refreshList();
        }
      }
    }
  };

  private final BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
     @Override
     public void onReceive(Context context, Intent intent) {
       String action = intent.getAction();
       if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
         int level = intent.getIntExtra("level", 0);
         int scale = intent.getIntExtra("scale", 100);
         int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
         int batteryLevel = level * 100 / scale;
         mIsLowBattery = (status != BatteryManager.BATTERY_STATUS_CHARGING && batteryLevel < 15);
         FileManagerApp.log(TAG +"battery level reveiver, the mIsLowBattery = "+mIsLowBattery);
       }
     }
  };

  private void setBackFromSearch(int backFromSearch) {
    SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
    SharedPreferences.Editor editor = setting.edit();
    editor.putInt(BACKFROMSEARCH, backFromSearch);
    editor.commit();
  }

  private void resetBackFromSearch() {
    setBackFromSearch(RESET_PARAM);
  }

  private int getBackFromSearch() {
    SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
    return setting.getInt(BACKFROMSEARCH, 0);
  }

  private String[] currentDirs;
  private File rootDir = new File(FileManagerApp.ROOT_DIR);
  private DialogInterface.OnClickListener folderViewDialogListener =
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
         if (mCreatedFromSearch || mCreatedFromSearchSuggestion) {
            setBackFromSearch(HOME_FROM_SEARCH);
            mCreatedFromSearch = false;
            mCreatedFromSearchSuggestion = false;
         }
         if (item == 0) {
            currentDirectory = rootDir;
            stopActivityThread();
            finish(); // exit Content View and return to Home Page
          } else {
            File file = new File(currentDirs[item]);
            browseTo(file);
          }
          dialog.dismiss();
        }
      };

  private void showFolderViewDialog() {
    String[] parts;
    if (currentDirectory.getAbsolutePath().startsWith(FileManagerApp.ROOT_DIR)) {
      parts =
          currentDirectory.getAbsolutePath().substring(FileManagerApp.ROOT_DIR.length()).split("/");
    } else {
      return;
    }
    String currentpath = currentDirectory.getAbsolutePath();
    mSdCardPath = getSdCardPath();
    FileManagerApp.log(TAG + "in showFolderViewDialog, mSdCardPath=" + mSdCardPath);
    if (mSdCardPath == null || TextUtils.isEmpty(mSdCardPath)) {
      return;
    }
    String[] subparts = null;
    try {
      subparts = currentpath.substring(mSdCardPath.length()).split("/");
      if (subparts.length < 1) {
        return;
      } else {
        parts = new String[2 + subparts.length - 1];
      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    currentDirs = new String[parts.length];
    currentDirs[0] = FileManagerApp.ROOT_DIR;
    currentDirs[1] = mSdCardPath;
    parts[0] = this.getString(R.string.home);
    if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD) {
      if (SdcardManager.hasInternalSdcard(this)) {
        parts[1] = this.getString(R.string.internal_phone_storage);
      } else {
        parts[1] = this.getString(R.string.sd_card);
      }
    } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD) {
      parts[1] = this.getString(R.string.sd_card);
    } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.STORAGE_MODE_EXTENAL_USB_DISK) {
      parts[1] = this.getString(R.string.usb_files);
    }
    for (int i = 2; i < parts.length; i++) {
      parts[i] = subparts[i - 1];
      currentDirs[i] = currentDirs[i - 1] + "/" + parts[i];
    }
    AlertDialog alert =
        createSingleChoiceDiag(R.string.select_folder, parts, parts.length - 1,
            folderViewDialogListener);
    alert.show();
  }

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
    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    if (titleId > 0) {
      builder.setTitle(titleId);
    }
    builder.setSingleChoiceItems(items, checkedItem, listener);
    builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (FileManagerApp.getSearchOption() == FileManagerApp.INTERNALSTORAGE) {
          m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_DIR;
          FileManagerApp.log(TAG +
              "In createSearchChoiceDialog get Search option is in internal storage");
        } else {
          m_FirstSearchedDirectory = FileManagerApp.getInstance(mContext).SD_CARD_EXT_DIR;
          FileManagerApp.log(TAG + "In createSearchChoiceDialog get Search option is in sd card");
        }
        FileManagerApp.log(TAG + "In createSearchChoiceDialog m_FirstSearchedDirectory is: " +
            m_FirstSearchedDirectory);
        currentDirectory = new File(m_FirstSearchedDirectory);
        mCreatedFromSearch = true;
        startSearchDirectory(currentDirectory, gSearchString);
        FileManagerApp.log(TAG + "In createSearchChoiceDialog" + gSearchString);
        gSearchString = null;
        dialog.dismiss();
      }
    });
    dialog = builder.create();
    return dialog;
  }

  private DialogInterface.OnClickListener sortByDialogListener =
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
          ((FileManagerApp) getApplication()).setSortMode(FileManagerApp.NAMESORT + item);
          refreshList();
          dialog.dismiss();
        }
      };

  private DialogInterface.OnClickListener searchOptionDialogListener =
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
          ((FileManagerApp) getApplication()).setSearchOption(item);
          FileManagerApp.log(TAG + "searchOptionDialogListener set item=" + item);
          // dialog.dismiss();
        }
      };

  private void showSortMethodDialog() {
    String[] parts = this.getResources().getStringArray(R.array.sort_by_method_selector);
    AlertDialog dialog =
        createSingleChoiceDiag(R.string.menu_sort_by, parts, FileManagerApp.getSortMode() -
            FileManagerApp.NAMESORT, sortByDialogListener);
    dialog.show();
  }

  private void showSearchOptionDialog() {
    String[] parts = this.getResources().getStringArray(R.array.search_storage_selector);
    AlertDialog dialog =
        createSearchChoiceDialog(R.string.search_folder, parts, FileManagerApp.getSearchOption(),
            searchOptionDialogListener);
    dialog.show();
  }

  private ProgressDialog loadingDialog = null;

  private void showLoadingDialog(String dialog) {
    if (this.isFinishing()) {
      return;
    }
    hideLoadingDialog();
    loadingDialog = new ProgressDialog(this);
    loadingDialog.setMessage(dialog);
    loadingDialog.setIndeterminate(true);
    loadingDialog.setCancelable(false);
    loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        browseTo(mPreviousDirectory);
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

  private void hideLoadingDialog() {
    if (loadingDialog != null) {
      loadingDialog.dismiss();
      loadingDialog = null;
    }
  }

  private ProgressDialog progressInfoDialog = null;

  private void showProgressInfoDialog(String string) {
    if (this.isFinishing()) {
      return;
    }
    hideProgressInfoDialog();
    progressInfoDialog = new ProgressDialog(this);
    progressInfoDialog.setMessage(string);
    progressInfoDialog.setIndeterminate(true);
    progressInfoDialog.setCancelable(true);
    progressInfoDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        if (mFileListView.getAdapter() != null) {
          if (((IconifiedTextListAdapter) mFileListView.getAdapter()).isSelectMode()) {
            outMultiSelectMode();
          }
        }
      }
    });

    if (!isFinishing() && (progressInfoDialog != null)) {
      try {
        progressInfoDialog.show();
      } catch (Exception e) {
        FileManagerApp.log(TAG + "Exception" + e.toString());
        return;
      }
    }
  }

  private void hideProgressInfoDialog() {
    if (progressInfoDialog != null) {
      progressInfoDialog.dismiss();
      progressInfoDialog = null;
      mZipping = false;
      mUnzipping = false;
    }
  }

    private void openFileWith(File aFile) {
        if (!aFile.exists()) {
            Toast.makeText(this, R.string.error_file_does_not_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        Uri data = FileUtils.getUri(aFile);
        String type = MimeTypeUtil.getMimeType(this,aFile.getName());
        /*if(type.indexOf("video") == 0) {
            String ext = FileUtils.getExtension(aFile.getName());
            if(FileUtils.needQueryDB(ext.toLowerCase())){
                boolean t = FileUtils.isInAudioDatabase(mContext, aFile.getPath());
                if(t) type = "audio/*";
            }
        }*/
        intent.setDataAndType(data, type);

        try {
            intent.setClassName("android", ResolverActivity.class.getName()); // force ResolverActivity to handle this
            intent.putExtra(EXTRA_REPLACE_PREFERRED, true);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.application_not_available, Toast.LENGTH_SHORT).show();
        };
    }
  private boolean isOpenWithAvailable(File aFile){
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        Uri data = FileUtils.getUri(aFile);
        String type = MimeTypeUtil.getMimeType(this,aFile.getName());
        intent.setDataAndType(data, type);
        PackageManager pm = getPackageManager();
        List<ResolveInfo>  list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list==null || list.size()<=1)
            return false;
        else
            return true;

  }

  private void handleUpdateFile(final File aFile) {
    if(mIsLowBattery){//battery low level, does not to do system update
        FileManagerApp.log(TAG+"low battery, does not to do system update--");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.Battery_Level_Low);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
        }
       }).create().show();
    } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(R.string.No, null);
        builder.setPositiveButton(R.string.Yes,
          new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
                 FileManagerApp.log(TAG+"UPDATE_PACKAGE path="+aFile.getAbsolutePath());
                 Intent i = new Intent(ACTION_MOTOROLA_INSTALL_UPDATE_PACKAGE);
                 i.putExtra("update_package",aFile.getAbsolutePath());
                 sendBroadcast(i);
                 }
          });
        builder.setMessage(R.string.System_Updates_File);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
  }

  private void printFile(final File file) {
    Intent intent = new Intent("com.motorola.android.intent.action.PRINT");
    intent.setType(MimeTypeUtil.getMimeType(this, file.getName()));
    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.send_not_available, Toast.LENGTH_SHORT).show();
    }
  }

  private boolean isPrintIntentHandled(final File file) {
    boolean return_value = false;
    Intent intent = new Intent("com.motorola.android.intent.action.PRINT");
    intent.setType(MimeTypeUtil.getMimeType(this, file.getName()));
    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
    PackageManager pm = getPackageManager();
    List<ResolveInfo> localeReceivers = pm.queryIntentActivities(intent, 0);
    if (localeReceivers.isEmpty()) {
      FileManagerApp.log(TAG + "isPrintIntentHandled: no, it is not.");
    } else {
      return_value = true;
      FileManagerApp.log(TAG + "isPrintIntentHandled: yes, it is.");
    }
    return (return_value);
  }

  private boolean isMcUploadMode() {
      return false;
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    mFileListView.requestFocus();
  }

  //IKDINARATD-457, a22121, 2011-07-05
  private String getPathFromFileUri(Uri uri) {
      if (uri != null) {
          String path = uri.toString();
          if (path.indexOf("file://") == 0) {
              return path.substring(7);
          }
      }
      return null;
  }
  //IKDINARATD-457 end

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_MENU :
        Log.d(TAG, "MENU long press");
        return true;
    }
    return super.onKeyLongPress(keyCode, event);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.d(TAG, "entered onConfigurationChanged");
    super.onConfigurationChanged(newConfig);
  }

}
