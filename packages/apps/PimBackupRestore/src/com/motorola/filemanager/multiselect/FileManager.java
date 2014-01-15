/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.filemanager.multiselect;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.Theme;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.multiselect.utils.IconifiedText;
import com.motorola.filemanager.multiselect.utils.IconifiedTextListAdapter;

import com.motorola.sdcardbackuprestore.Constants;
import com.motorola.sdcardbackuprestore.Import3rd;
import com.motorola.sdcardbackuprestore.Import3rd2;
import com.motorola.sdcardbackuprestore.R;
import com.motorola.sdcardbackuprestore.SdcardManager;

public class FileManager extends Activity {
  public final static String ACTION_RESTART_ACTIVITY = "com.mot.FileManagerIntents.ACTION_RESTART_ACTIVITY";
  public final static String ACTION_SELECTFILE = "com.mot.FileManagerIntents.ACTION_SELECTFILE";
  public final static String ACTION_SELECTFOLDER = "com.mot.FileManagerIntents.ACTION_SELECTFOLDER";
  public final static String ACTION_MULTISELECTFILE = "com.mot.FileManagerIntents.ACTION_MULTISELECTFILE";
  // USB Host
  public final static String ACTION_USBDISKMOUNTED = "com.motorola.intent.action.USB_DISK_MOUNTED_NOTIFY";
  // USB Host End

  public final static String EXTRA_KEY_RESULT = "FILEPATH";
  public final static String EXTRA_KEY_AUTHINFO = "AUTHINFO";

  public final static String EXTRA_KEY_HOMESYNC_TITLE = "HOMESYNC_TITLE";
  public final static String EXTRA_KEY_HOMESYNC_STEP = "HOMESYNC_STEP";

  public static final String EXTRA_KEY_DISKNUMBER = "DISK_NUMBER";
  public static final String EXTRA_KEY_CURRENT_USBPATH = "CURRENT_USBPATH";
  private static final String EXTRA_KEY_DISK[] = new String[]{"DISK1", "DISK2", "DISK3"};

  public static final String EXTRA_KEY_CONTENT = "CURRENT_CONTENT";

  public static final String HIDEUNKOWNSETTING = "HideUnkownSetting";

  public static final String ID_LOCAL = "Local";
  public static final String ID_EXTSDCARD = "Extcard";
  public static final String ID_REMOTE = "Rmote";
  public static final String ID_MOTOCONNECT = "MC";
  public static final String ID_PROTECTED = "Protected"; // work around to track if protected content.
  public static final String ID_USB = "Usb"; // USB Host

  static private String mCurrentContent = ID_LOCAL;
  private static boolean eMMCEnabled = false;
  public static final int USB_DISK_1 = 1;
  public static final int USB_DISK_2 = 2;
  public static final int USB_DISK_3 = 3;
  static private Context mContext = null;
  static private Activity mInstance = null;
  private View mHomePage;
  private Button mFileChooserCancelButton;
  private int mSelectedUsbIndex = -1;
  private boolean mWiFiDialogDisplaying = false;

  private static final String TAG = "FileManager: ";
  static private final IntentFilter FM_Event_Filter = new IntentFilter(FileManager.ACTION_USBDISKMOUNTED);

  // private String TAG = "FileManager";
  private Handler mHighlightHandler = new Handler();
  // IKSTABLEFIVE-2633 - Start
  private List<IconifiedText> mHomePageStaticItems = new ArrayList<IconifiedText>();
  private List<IconifiedText> mHomePageCurrentList = new ArrayList<IconifiedText>();
  private AbsListView mHomePageListView;
  private IconifiedTextListAdapter mHomePageListAdapter = null;
  public static ArrayList<String> mFilter = null;

  // IKSTABLEFIVE-2633 - End
  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    ComponentName cn = getCallingActivity();
    initVar(this, this);
//    eMMCEnabled = (SystemProperties.getInt("ro.mot.internalsdcard", 0) != 0);
    eMMCEnabled = SdcardManager.hasInternalSdcard(this);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.file_manager);
    mHomePage = findViewById(R.id.home_page);
    if (getIntent() != null && getIntent().getExtras() != null) {
    	mFilter = getIntent().getExtras().getStringArrayList(Constants.FILE_MANAGER_FILTER);
	} else {
        mFilter = null;
    }

    if (eMMCEnabled) {
        mFileChooserCancelButton = (Button) findViewById(R.id.file_chooser_cancel);
        mFileChooserCancelButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            finish();
            if (Import3rd.getInstance() != null) {
            	Import3rd.getInstance().finish();
			}
            if (Import3rd2.getInstance() != null) {
            	Import3rd2.getInstance().finish();
			}
          }
        });
	}

    FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
    if (!eMMCEnabled) {
    	mCurrentContent = ID_LOCAL;
        // showContentPage need to be called outside onClick to show
        // highlighted item.
        mHighlightHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
            showContentPage();
          }
        }, 100);
	} else {
	    initHomePage();
	    showHomePage();
	}
  }
  
  public static Activity getInstance() {
	  return mInstance;
  }

  private static void initVar(Context context, Activity instance) {
    mContext = context;
    mInstance = instance;
  }
  
  private void initHomePage() {
    // IKSTABLEFIVE-2633
    mHomePageListView = (AbsListView) findViewById(R.id.home_page_list);
    mHomePageListView.setVisibility(View.VISIBLE);
    mHomePageListView.setTextFilterEnabled(true);
    mHomePageListView.requestFocus();
    mHomePageListView.requestFocusFromTouch();

    mHomePageListAdapter = new IconifiedTextListAdapter(this);
    mHomePageListAdapter.setHomePage(true);
    mHomePageListAdapter.setListItems(mHomePageCurrentList, mHomePageListView.hasTextFilter());
    mHomePageListView.setAdapter(mHomePageListAdapter);

    mHomePageStaticItems.clear();
    mHomePageCurrentList.clear();

    if (eMMCEnabled) {
      IconifiedText internalmemory =
          new IconifiedText(getString(R.string.internal_phone_storage),
                            getResources().getDrawable(R.drawable.ic_thb_device));
      internalmemory.setIsNormalFile(false);
      mHomePageStaticItems.add(internalmemory);
    }

    IconifiedText sdcard =
        new IconifiedText(getString(R.string.sd_card),
                          getResources().getDrawable(R.drawable.ic_thb_external_sdcard));
    sdcard.setIsNormalFile(false);
    mHomePageStaticItems.add(sdcard);

    Intent intent = getIntent();
    String intentAction = intent.getAction();
    if (intentAction != null) {
      if (intentAction.equals(ACTION_SELECTFILE) || intentAction.equals(ACTION_MULTISELECTFILE)) {
        FileManagerApp.setIsGridView(false);
        FileManagerApp.setLaunchMode(FileManagerApp.SELECT_FILE_MODE);
        String extraTitle = intent.getStringExtra(EXTRA_KEY_HOMESYNC_TITLE);
        String extraStep = intent.getStringExtra(EXTRA_KEY_HOMESYNC_STEP);
        if (extraTitle == null) {
          findViewById(R.id.homesync_bar).setVisibility(View.GONE);
          FileManagerApp.setShowFolderViewIcon(true);
        } else {
          findViewById(R.id.homesync_bar).setVisibility(View.VISIBLE);
          ((TextView) findViewById(R.id.homesync_title)).setText(extraTitle);
          ((TextView) findViewById(R.id.homesync_step)).setText(extraStep);
          FileManagerApp.setShowFolderViewIcon(false);
          if (isAttDrmContentAvailable()) {
            IconifiedText protectedcontent =
                new IconifiedText(getString(R.string.protected_content), getResources()
                    .getDrawable(R.drawable.ic_launcher_folder_protected_content));
            protectedcontent.setIsNormalFile(false);
            mHomePageStaticItems.add(protectedcontent);
          }
        }
      } else {
        ((FileManagerApp) getApplication()).resetGridViewMode();
        FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);
        if (isAttDrmContentAvailable()) {
          IconifiedText protectedcontent =
              new IconifiedText(getString(R.string.protected_content), getResources().getDrawable(
                  R.drawable.ic_launcher_folder_protected_content));
          protectedcontent.setIsNormalFile(false);
          mHomePageStaticItems.add(protectedcontent);
        }
        FileManagerApp.setShowFolderViewIcon(true);
      }
    }

    // Add the static items to current list before checking for the dynamic
    // elements
    addAllElements(mHomePageCurrentList, mHomePageStaticItems);

    mHomePageListView.setOnItemClickListener(homePageListListener);
    mHomePageListAdapter.notifyDataSetChanged();
  }

  AdapterView.OnItemClickListener homePageListListener = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) parent.getAdapter();
      if (adapter == null) {
        return;
      }
      IconifiedText text = (IconifiedText) adapter.getItem(position);
      String file = text.getText();
      if (file.equals(getString(R.string.internal_phone_storage))) {
        if (!SdcardManager.isSdcardMounted(FileManager.this, false)) {
          if (!isFinishing()) {
            (new AlertDialog.Builder(mContext)).setTitle(R.string.internal_phone_storage)
                .setMessage(R.string.internal_phone_storage_unmounted)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                  }
                }).create().show();
          }
        } else {
          mCurrentContent = ID_LOCAL;
          // showContentPage need to be called outside onClick to show
          // highlighted item.
          mHighlightHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
              showContentPage();
            }
          }, 100);
        }
      } else if (file.equals(getString(R.string.sd_card))) {
        try {
//           if
          // (!MotoEnvironment.getExternalAltStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED))
          // {
          // (new AlertDialog.Builder(mContext))
          // .setTitle(R.string.sd_card)
          // .setMessage(R.string.sd_missing)
          // .setNeutralButton(R.string.ok, new
          // DialogInterface.OnClickListener() {
          // public void onClick(DialogInterface dialog, int which){
          // }
          // })
          // .create().show();
          // } else {
          if (eMMCEnabled) {
            mCurrentContent = ID_EXTSDCARD;
          } else {
            mCurrentContent = ID_LOCAL;
          }
          // showContentPage need to be called outside onClick to show
          // highlighted item.
          mHighlightHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
              showContentPage();
            }
          }, 100);
          // }
        } catch (NoClassDefFoundError e) {
          Toast.makeText(mContext, "NoClassDefFoundError", Toast.LENGTH_SHORT).show();
        }
      }
    }
  };

  private void addAllElements(List<IconifiedText> addTo, List<IconifiedText> addFrom) {
    int size = addFrom.size();
    for (int i = 0; i < size; i++) {
      addTo.add(addFrom.get(i));
    }
  }
  // IKSTABLEFIVE-2633 - End

  @Override
  public void onDestroy() {
    FileManagerApp.log(TAG +"onDestroy "+this);
    // IKSTABLEFIVE-2633 - Start
    mHomePageCurrentList.clear();
    mHomePageStaticItems.clear();
    if (mHomePageListView != null) {
        mHomePageListView.setAdapter(null);
	}
    // IKSTABLEFIVE-2633 - End
  //  FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);
    super.onDestroy();
  }

  private void showHomePage() {
    mHomePage.setVisibility(View.VISIBLE);
    FileManagerApp.getInstance(mContext).setPhoneStoragePath(0, -1);

    TextView homePageTitle = (TextView) findViewById(R.id.home_page_title);
    if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
      homePageTitle.setText(R.string.select_file);
    } else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
      homePageTitle.setText(R.string.select_folder);
    }
  }

  private void showContentPage() {
    if (mCurrentContent.equals(ID_LOCAL)) {
      FileManagerApp.getInstance(mContext).setPhoneStoragePath(FileManagerApp.STORAGE_MODE_EXTENAL_SDCARD, -1);
    } else if (mCurrentContent.equals(ID_EXTSDCARD)) {
      FileManagerApp.getInstance(mContext).setPhoneStoragePath(FileManagerApp.STORAGE_MODE_EXTENAL_ALT_SDCARD, -1);
    }

    Intent intent = new Intent(this, FileManagerContent.class);
    intent.setAction(FileManager.ACTION_RESTART_ACTIVITY);
    intent.putExtra(FileManager.EXTRA_KEY_CONTENT, mCurrentContent);
    if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE ||
        FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
      intent.putExtra(FileManager.EXTRA_KEY_HOMESYNC_TITLE,
          ((TextView) findViewById(R.id.homesync_title)).getText());
      intent.putExtra(FileManager.EXTRA_KEY_HOMESYNC_STEP,
          ((TextView) findViewById(R.id.homesync_step)).getText());
    }
    startActivity(intent);
  }

  public static String getCurrentContent() {
    return mCurrentContent;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // no action needed, not expecting any new intents
    FileManagerApp.log(TAG + "Received a Intent in onNewIntent");
  }

  public static void rtnPickerResult(String filePath, String authInfo) {
    Intent returnIntent = new Intent();
    returnIntent.putExtra(FileManager.EXTRA_KEY_RESULT, filePath);
    if (authInfo != null) {
      returnIntent.putExtra(FileManager.EXTRA_KEY_AUTHINFO, authInfo);
    }
    rtnPickerResult(returnIntent);
  }

  public static void rtnPickerResult(Intent iData) {
    FileManagerApp.setLaunchMode(FileManagerApp.NORMAL_MODE);

    if (mInstance != null) {
        mInstance.setResult(RESULT_OK, iData);
        mInstance.finish();
    }
    if (FileManagerContent.getActivityGroup() != null) {
      (FileManagerContent.getActivityGroup()).finish();
    }
  }

  static public Context getCurContext() {
    return mContext;
  }

  // Check If AT&T DRM intent is there (IKSTABLEFOUR-1454)
  public boolean isAttDrmContentAvailable() {
    boolean isAttDrmContent = true;
    PackageManager pm = getPackageManager();
    try {
      ApplicationInfo appInfo = pm.getApplicationInfo("com.motorola.AttDrmContent", 0);
      if (appInfo != null) {
        Log.d(TAG, "App info" + appInfo.name);
      }
      // if we get to here this mean the app is there and the flag will remain
      // true.
    } catch (NameNotFoundException e) {
      // app was not found
      isAttDrmContent = false;
      Log.d(TAG, " can't find AttDrmContent");
    }
    return isAttDrmContent;
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_MENU :
        Log.d(TAG, "MENU long press");
        return true;
    }
    return super.onKeyLongPress(keyCode, event);
  }

  public boolean getEMMCEnabled() {
    eMMCEnabled = SdcardManager.hasInternalSdcard(this);
    return eMMCEnabled;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU){
      if (event!=null){
        if (event.getRepeatCount() >0){
          return true;
        }
      }
    }
    if (keyCode == KeyEvent.KEYCODE_BACK) {
    	if (Import3rd.getInstance() != null) {
    		Import3rd.getInstance().finish();
		}
    	if (Import3rd2.getInstance() != null) {
    		Import3rd2.getInstance().finish();
		}
	}
    return super.onKeyDown(keyCode, event);
  }
}
