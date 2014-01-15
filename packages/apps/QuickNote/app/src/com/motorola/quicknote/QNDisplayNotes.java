package com.motorola.quicknote;

import android.util.Log;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.content.ContentValues;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.Window;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import com.motorola.quicknote.QNConstants._BackgroundRes;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.content.QNContent_Error;
import com.motorola.quicknote.R;


public class QNDisplayNotes extends Activity implements LoaderManager.LoaderCallbacks<Cursor>{
 
   private static final String TAG = "QNDisplayNotes";
   private static final String OBTAIN_BITMAP_THREAD = "obtain bitmap thread";
   
   private GridView mGrid;
   private Cursor  mCursor;
   private int mNum;
   private NoteAdapter mNoteAdapter;
   private static int mThumbLayoutWidth;
   private static int mThumbLayoutHeight;
   private BroadcastReceiver mQuickNotesReceiver = new QuickNotesIntentReceiver();

	private DeleteThread checkUpdate;
	private AlertDialog mProgressDialog = null;
	private ProgressBar mProgressBar = null;
	private TextView mText = null;
	private static final int MSG_INIT_PROGRESS = 1;
	private static final int MSG_UPDATE_PROGRESS = 2;
	private static final int MSG_DISMISS_PROGRESS = 3;
	private static final int MSG_SET_ITEM_IMAGE = 4;
	ArrayList<Boolean> checkOn = new ArrayList<Boolean>();
	ArrayList<Uri> mContentUri = new ArrayList<Uri>();
	final ArrayList<Uri> mDeletedUri = new ArrayList<Uri>();
	private int mCountDeleted = 0;
	private int mCountChecked = 0;
	private ImageButton mExitMultiButton;
	private ImageButton mDeleteButton;
	/*2012-12-12, add by amt_sunzhao for SWITCHUITWO-299 */
	private AlertDialog mDeleteAlertDialog = null;
	/*2012-12-12, add end*/
	private DropdownButton mSelectionButton;

	private static final int SELECTION_NONE = 0;
	private static final int SELECTION_ALL = 1;
	private static final int SELECTION_INCREASE = 2;
	private static final int SELECTION_DECREASE = 3;
	private boolean bDeleting = false;
	/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
	private HandlerThread mObtainBitmapThread = new HandlerThread(OBTAIN_BITMAP_THREAD);
	private ObtainBitmapHandler mObtainBitmapH = null;
	/*2012-9-6, add end*/ 
	/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
	// may has multiply delete notes task at the same time
	private static int sDeleteTasksCount = 0;
	private static Object sLock = new Object();
	/*2012-10-8, add end*/ 

	private class DeleteThread extends Thread {
		private Object[] mA;
		private NoteAdapter mNA;

		public DeleteThread(Object[] a) {
			super("DeleteThread");
			mA = a;
		}

        public DeleteThread(NoteAdapter noteAdapter) {
             mNA = noteAdapter;
        }

		@Override
		public void run() {
			boolean bOngoing;
			int countChecked = mCountChecked;
			
			bDeleting = true;
			/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
			sendDeleteTasksBroadcast(1);
			/*2012-10-8, add end*/ 

			if((bOngoing = loadContentUri())) {
			while (!mContentUri.isEmpty()) {
				Uri deleteUri = mContentUri.remove(0);
				if (QuickNotesDB.delete(QNDisplayNotes.this, deleteUri)) {
					mCountDeleted++;
                   updateProgress(mCountDeleted, countChecked);
					mDeletedUri.add(deleteUri);
				}
			}
			}
				
			bDeleting = false;
				
			if (mCountDeleted > 0 || !bOngoing) {
				try {
				Intent t = new Intent(QNConstants.INTENT_ACTION_NOTE_DELETED);
				// just put a special value (0) here
				t.putExtra(QNConstants.GRID_ID, 0);
				t.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mDeletedUri);
				sendBroadcast(t);
				} catch(Exception e) {
					QNDev.logd(TAG, "Exception:" + e);
				}
				/*2012-9-10, add by amt_sunzhao for SWITCHUITWOV-90 */ 
				//dismissProgress();
				mCountDeleted = 0;
				// finish();
			}
			/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
			sendDeleteTasksBroadcast(-1);
			/*2012-10-8, add end*/ 
			// In any case, delete thread run end, we must close the dialog.
			dismissProgress();
			/*2012-9-10, add end*/ 
		}
		
		/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
		private void sendDeleteTasksBroadcast(final int changeCount) {
			synchronized (sLock) {
				sDeleteTasksCount = sDeleteTasksCount + changeCount;
				Intent deleteTasks = new Intent(QNConstants.INTENT_ACTION_NOTE_DELETE_TASKS);
				deleteTasks.putExtra(QNConstants.KEY_DELETE_TASKS_COUNT, sDeleteTasksCount);
				QNDisplayNotes.this.sendStickyBroadcast(deleteTasks);
				
			}
		}
		/*2012-10-8, add end*/ 

    	private boolean loadContentUri() {
    	    boolean ret = true;
            mContentUri.clear();
            for (int i = 1; i < mNA.getCount(); i++) {
                if (checkOn.get(i)) {
                    mContentUri.add((Uri) mNA.getItem(i - 1));
                }
                if(mCountChecked <= 0) {
                    ret = false;
                    break;
                }
                if(i % 32 == 1) initProgress(mCountChecked);
            }
            return ret;
    	}
	}

	private void initProgress(int max) {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_INIT_PROGRESS, 0, max));
	}

	private void updateProgress(int cur, int max) {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS, cur,
				max));
	}

	private void dismissProgress() {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_DISMISS_PROGRESS));
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (mProgressDialog == null) {
				return;
			}

			switch (msg.what) {
			case MSG_INIT_PROGRESS:
				mProgressBar.setProgress(0);
				mProgressBar.setMax(msg.arg2);
				mText.setText(getString(R.string.delete_progress_deleted, 0,
						msg.arg2));
				mProgressDialog.setTitle(R.string.menu_deleteQuickNotes);
				mProgressDialog.setMessage(getString(R.string.delete_progress_text));
				break;
			case MSG_UPDATE_PROGRESS:
				mProgressBar.setProgress(msg.arg1);
				mText.setText(getString(R.string.delete_progress_deleted,
						msg.arg1, msg.arg2));
				mProgressDialog.setTitle(R.string.menu_deleteQuickNotes);
				mProgressDialog.setMessage(getString(R.string.delete_progress_text));
				break;
			case MSG_DISMISS_PROGRESS:
				mProgressDialog.dismiss();
				// finish();
				break;
			default:
				QNDev.logi(TAG, "unknown msg");
				break;
			}
		}
	};

	private void setupView() {
		setContentView(R.layout.display_notes_gridview);

		mExitMultiButton = (ImageButton) findViewById(R.id.exit_btn);
		mDeleteButton = (ImageButton) findViewById(R.id.delete_btn);
		mSelectionButton = (DropdownButton) findViewById(R.id.selection_button);

		mExitMultiButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				ChangeSelection(SELECTION_NONE);
			}

		});
		mDeleteButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				/*mContentUri.clear();

				for (int i = 1; i < mNoteAdapter.getCount(); i++) {
					if (checkOn.get(i)) {
						mContentUri.add((Uri) mNoteAdapter.getItem(i - 1));
					}
				}*/
				/*2012-12-12, add by amt_sunzhao for SWITCHUITWO-299 */
				if(null != mDeleteAlertDialog) {
					 return;
				}
				if (mCountChecked > 0) {
					mDeleteAlertDialog = new AlertDialog.Builder(QNDisplayNotes.this).setTitle(
					/*2012-12-12, add end*/
                            R.string.delete).setMessage(
                            R.string.grid_delete_confirm).setPositiveButton(
                            R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                	/*2012-12-12, add by amt_sunzhao for SWITCHUITWO-299 */
                                	mDeleteAlertDialog = null;
                    				/*2012-12-12, add end*/
                                    // Object[] a = mContentUri.toArray();
                                    LayoutInflater factory = LayoutInflater
                                            .from(QNDisplayNotes.this);
                                    View ProgressView = factory.inflate(
                                            R.layout.progressbar_delete, null);
                                    checkUpdate = new DeleteThread(mNoteAdapter);

											mProgressDialog = new AlertDialog.Builder(
													QNDisplayNotes.this)
													.setIcon(
															android.R.drawable.ic_menu_delete)
													.setTitle(
															R.string.menu_deleteQuickNotes)
													.setMessage(
															R.string.delete_progress_text)
													.setView(ProgressView)
													.setCancelable(false)
													.setOnKeyListener(
															new DialogInterface.OnKeyListener() {
																public boolean onKey(
																		DialogInterface dialog,
																		int keyCode,
																		KeyEvent event) {
																	if (keyCode == KeyEvent.KEYCODE_SEARCH) {
																		return true; // Pretend
																	} else if (keyCode == KeyEvent.KEYCODE_BACK) {
																		mContentUri.clear();
																		mCountChecked = 0;
																		mCountDeleted = 0;

																	}
																	// Any other
																	// keys are
																	// still
																	// processed
																	// as normal
																	return false;
																}
															}).show();

											mProgressBar = (ProgressBar) mProgressDialog
													.findViewById(R.id.progress_horizontal);
                                    mProgressBar.setPadding(30, 0, 0, 0);
											mText = (TextView) mProgressDialog
													.findViewById(R.id.text);
											mText.setPadding(50, 0, 0, 0);

											checkUpdate.start();
										}
                            }).setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                	/*2012-12-12, add by amt_sunzhao for SWITCHUITWO-299 */
                                	mDeleteAlertDialog = null;
                    				/*2012-12-12, add end*/
                                }
                            }).setOnKeyListener(
                            new DialogInterface.OnKeyListener() {
                                public boolean onKey(DialogInterface dialog,
                                        int keyCode, KeyEvent event) {
                                    if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                                        return true; // Pretend we processed it
                                    }
                                    return false; // Any other keys are still processed as normal
                                }
								/*2012-12-12, add by amt_sunzhao for SWITCHUITWO-299 */
                            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
								@Override
                            	public void onCancel(DialogInterface dialog) {
                                	mDeleteAlertDialog = null;
                    				/*2012-12-12, add end*/
                            	}
                            })
                            .show();
				}
			}

		});
		/*2012-12-7, add by amt_sunzhao for SWITCHUITWO-252 */
		setupSelectionButton();
		/*2012-12-7, add end*/

		mGrid = (GridView) findViewById(R.id.gridview);

		mGrid.setAdapter(mNoteAdapter);

        mGrid.setOnItemClickListener(new OnItemClickListener() 
        {
            public void onItemClick(AdapterView parent, 
            View v, int position, long id) 
            {
                if (position == 0) {
                     if (mCountChecked == 0) {
                     if(QNUtil.isTextLoad(QNDisplayNotes.this)) {

                	 boolean preCondition = false;
                   if (!QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                       // text note store on phone memory
                       preCondition = true;
                   } else {
                  	 QNDev.log(TAG+"preCondition = QNUtil.checkSDCard(QNDisplayNotes.this)");
                       preCondition = QNUtil.checkStorageCard(QNDisplayNotes.this);
                       QNDev.log(TAG+"preCondition = " + preCondition);
                   }

                   if (preCondition ) {                                                  
                       Intent intentNoteNew = new Intent(QNConstants.INTENT_ACTION_NEW)
                       .setClassName(QNConstants.PACKAGE, QNConstants.PACKAGE + ".QNNoteView")
                       .setDataAndType(Uri.parse("qntext:"), QNConstants.MIME_TYPE_TEXT);                
                       
                       if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                           Uri uriFilePath = QNUtil.prepareTextNote();
                           if (uriFilePath == null) {                         
                               finish();
                               return;
                           }
                           intentNoteNew.setDataAndType(uriFilePath, QNConstants.MIME_TYPE_TEXT);
                       }
                      
                       intentNoteNew.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                       startActivity(intentNoteNew);
                  }
  					  } else {
      						// create new note
      						Intent intentDetail = new Intent();
                              intentDetail.setClass(QNDisplayNotes.this, QNNewActivity.class);
      						startActivity(intentDetail);
      			  }
					}
				} else {
					if (mCountChecked == 0) {
						// only none selected, go to detail note
                        Intent intentDetail = new Intent(QNConstants.INTENT_ACTION_DETAIL);
                        intentDetail.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                        intentDetail.setClass(QNDisplayNotes.this, QNNoteView.class);
						intentDetail.putExtra("index", position - 1);
						// Uri noteUri =
						// QuickNotesDB.getItem(QNDisplayNotes.this,
						// position-1);
						// intentDetail.putExtra("contentUri",
						// noteUri.toString());
						startActivity(intentDetail);
					} else {
						// in selection mode, click to add/remove item.
						if (checkOn.get(position)) {
							ChangeSelection(SELECTION_DECREASE, position, v);
						} else {
							ChangeSelection(SELECTION_INCREASE, position, v);
						}
					}
				}
			}
		});

		mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position != 0) {
					// update the view

					if (checkOn.get(position)) {
						ChangeSelection(SELECTION_DECREASE, position, view);
					} else {
						ChangeSelection(SELECTION_INCREASE, position, view);
					}

					return true;
				} else {
					return false;
				}
			}

		});

	}
	
	/*2012-12-7, add by amt_sunzhao for SWITCHUITWO-252 */
	private void setupSelectionButton() {
		mSelectionButton.setupMenu(R.menu.selection_menu,
				new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.action_select_all:
							ChangeSelection(SELECTION_ALL);
							break;
						case R.id.action_unselect_all:
							ChangeSelection(SELECTION_NONE);
							break;
						}
						return true;
					}
				});

		// Overriding the default onClick method to add a new behavior to the
		// PopupMenu
		mSelectionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// If all the items are selected, hide the Select All menu item
				if (mSelectionButton.getDropdownable()) {
					if (mCountChecked == mGrid.getCount() - 1) {
                        mSelectionButton.getPopupMenu().getMenu().findItem(
                                R.id.action_select_all).setVisible(false); }
                    else {
                        mSelectionButton.getPopupMenu().getMenu().findItem(
                                R.id.action_select_all).setVisible(true); }
                    
					mSelectionButton.getPopupMenu().show();
				}
			}
		});
	}
	/*2012-12-7, add end*/

	@Override
    protected void onCreate(Bundle savedInstanceState) 
    {

		QNDev.log(TAG + "onCretae");
		super.onCreate(savedInstanceState);

		/**
		 * check the internal storage, if there's no space, then exit thumbnails
		 * just like Galley reference
		 * /packages/apps/Gallery3D/src/com/cooliris/media/ 1. Gallery.java 2.
		 * ImageManager.java 3. Utils.java
		 **/
		if (hasStorage(QNDisplayNotes.this, true) && !SDHasFreeSpace()) {
			QNDev.log(TAG + "internal storage is full, so exit the thumbnails");
            if(Environment.isExternalStorageRemovable()) {
                Toast.makeText(QNDisplayNotes.this, R.string.sdcard_full_exit, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(QNDisplayNotes.this, R.string.internal_full_exit, Toast.LENGTH_SHORT).show();
            }
//			finish();
//			return;
		}


        mThumbLayoutWidth = getResources().getDimensionPixelSize(R.dimen.thumb_layout_width);
        mThumbLayoutHeight = getResources().getDimensionPixelSize(R.dimen.thumb_layout_height);

	
		mCursor = QuickNotesDB.getQuickNoteDBCursor(this);
		if (mCursor == null) {
           Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
        getLoaderManager().initLoader(0, null, this);

		
		/*for (int i = 0; i <= mNum; i++) {
			checkOn.add(i, false);
		}*/
		//TODO
		// set the special grid format /layout and set the adapter
		mNoteAdapter = new NoteAdapter(this);
		setupView();
		/*2012-9-6, add by amt_sunzhao for T810T_P003933 */
		mObtainBitmapThread.start();
		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		UIHandler UIHandler = new UIHandler(mGrid);
		/*2012-9-18, add end*/ 
		/*2012-9-15, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		mObtainBitmapH = new ObtainBitmapHandler(mObtainBitmapThread.getLooper(), UIHandler, mGrid);
		/*2012-9-15, add end*/ 
		/*2012-9-6, add end*/
                QNUtil.initDirs(QNDisplayNotes.this);
		registerIntentReceivers();
	}

	private boolean hasStorage(Context context, boolean requireWriteAccess) {
		boolean mediaMounted = getMountedStat(context);
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			if (requireWriteAccess) {
				boolean writable = checkFsWritable();
				return mediaMounted & writable;
			} else {
				return mediaMounted & true;
			}
        } else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return mediaMounted & true;
		}
		return false;
	}

	private static boolean checkFsWritable() {
		// Create a temporary file to see whether a volume is really writeable.
		// It's important not to put it in the root directory which may have a
		// limit on the number of files.
        String directoryName = Environment.getExternalStorageDirectory().toString() + "/DCIM";
		File directory = new File(directoryName);
		if (!directory.isDirectory()) {
			if (!directory.mkdirs()) {
				return false;
			}
		}
		return directory.canWrite();
	}

	private boolean SDHasFreeSpace() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
		  try {
			  	StatFs stat = new StatFs(Environment.getExternalStorageDirectory().toString());
	      			int availableBlks = stat.getAvailableBlocks();
	      			int minBlks = (5 * 1024 * 1024) / stat.getBlockSize();
	      			if (availableBlks < minBlks) {
	      				return false;
	      			}
	              } catch (java.lang.IllegalArgumentException e) {   
	            	  QNDev.log(TAG + " java.lang.IllegalArgumentException ");
	            	  return false;
	              }
		}
		return true;
	}

	private static final String PEFS_GALLERY3D = "Gallery3DPrefsFile";
	private static final String PEFS_GALLERY3D_MOUNTED = "MediaMounted";

	private boolean getMountedStat(Context context) {
		boolean result = true;
        SharedPreferences settings = context.getSharedPreferences(PEFS_GALLERY3D, 0);
		result = settings.getBoolean(PEFS_GALLERY3D_MOUNTED, true);
		return result;
	}

	@Override
	public void onDestroy() {
	        if(checkOn != null && checkOn.size() > 0){
			checkOn.clear();
		}
		QNDev.log(TAG + " onDestroy()");
		if (mCursor != null) {
			mCursor.close();
			unregisterReceiver(mQuickNotesReceiver);
		}
		/*2012-9-6, add by amt_sunzhao for T810T_P003933 */
		mObtainBitmapThread.quit();
		/*2012-9-6, add end*/
		super.onDestroy();
	}

	private void registerIntentReceivers() {
        IntentFilter qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_CREATED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_UPDATED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_NOTE_DELETED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED);
		registerReceiver(mQuickNotesReceiver, qnFilter);
	}

	private class QuickNotesIntentReceiver extends BroadcastReceiver {
		private final String TAG = "[QNDisplayNotes:QuickNotesIntentReceiver]";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			QNDev.log(TAG + " intent action : " + action);

			if (action == null) {
				((NoteAdapter) mGrid.getAdapter()).notifyDataSetChanged();
				return;
			}
						
			 mCursor.requery(); 
	        int notesNum = mCursor.getCount();	              	
			if (action.equals(QNConstants.INTENT_ACTION_NOTE_CREATED)) {
				
				checkOn.add(mNum + 1, false);
				for (int j = mNum; j >= 1; j--) {
					checkOn.set(j + 1, checkOn.get(j));
				}
				checkOn.set(1, false);
				
				if(mNum + 1 < notesNum){
					for (int j = mNum + 1; j <= notesNum; j++) {
						checkOn.add(j, false);
					}
				}								
			}
			
			mNum = notesNum;	

			if (action.equals(QNConstants.INTENT_ACTION_NOTE_DELETED)) {
				mCountChecked = 0;
                checkOn.clear();
                for (int i = 0; i <= mNum; i++) {
                    checkOn.add(i, false);
                }
                
                ChangeSelection(SELECTION_NONE);
            }
			
			/*2012-9-14, add by amt_sunzhao for SWITCHUITWOV-178 */ 
			/*
			 *  When receive INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED, count of items may
			 *  changed, initialize part of the ArrayList[checkOn] 
			 */
			if(action.equals(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED)) {
				final int checkOnLength = checkOn.size();
				for (int i = 0; i <= mNum; i++) {
					if(i>= checkOnLength){			
						 checkOn.add(i, false);			
					}
				}
			}
			/*2012-9-14, add end*/ 

			((NoteAdapter) mGrid.getAdapter()).notifyDataSetChanged();
		}
	}

	public static class ViewHolder {
		ImageView image;
		TextView text;
		TextView note_title;
		ImageView separator;
		ImageView selection;
		ImageView image_error;
		TextView text_error;
		ImageView image_reminder;
		// BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call
		// quicknote demo.
		ImageView image_incall;
		// END IKCNDEVICS-504
		// set the width & height which will be used to get the image, and the
		// values are referenced from gridview's
		// layout params: layout.setLayoutParams(new
		// GridView.LayoutParams(132,185));
		int layout_width;
		int layout_height;
		/*2012-9-13, add by amt_sunzhao for SWITCHUITWOV-175 */ 
		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		volatile int noteId;
		/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */
		volatile String mimeType;
		/*2012-10-11, add end*/ 
		/*2012-9-18, add end*/ 
		/*2012-9-13, add end*/ 
	}
	/*2012-9-6, add by amt_sunzhao for T810T_P003933 */
	/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
	static class Wrapper {
		String mimeType;
		ViewHolder viewHolder;
	}
	
	static class ImageWrapper extends Wrapper{
	/*2012-10-11, add end*/ 
		String qnc_uri;
		Context context;
		QNContent qnc;
	}
	/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
	static class AudioWrapper extends Wrapper{
	/*2012-10-11, add end*/ 
		Context context;
		QNContent qnc;
		RelativeLayout parentView;
		int bgcolor;
	}
	static class ObtainBitmapHandler extends Handler {
		private UIHandler mUIHandler = null;
		/*2012-9-15, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		private GridView mGv = null;
		private Object mLock = new Object();
		
		public ObtainBitmapHandler(final Looper looper, final UIHandler UIHandler,
				final GridView gv) {
			super(looper);
			mUIHandler = UIHandler;
			mGv = gv;
			/*2012-9-15, add end*/ 
		}
		
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			final Object obj = msg.obj;
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
			final int dlNoteId = msg.what;
			/*2012-9-15, add by amt_sunzhao for SWITCHUITWOV-190 */ 
			/*synchronized (mLock) {
				final int pos = msg.what;
				final int first = mGv.getFirstVisiblePosition();
				final int last = mGv.getLastVisiblePosition();
				// item in position is not visible, don't handle
				if((first > pos)
						&& (pos > last) ){
					return;
				}
			}*/
			/*2012-9-15, add end*/ 
			/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
			Wrapper wr = (Wrapper)obj;
			if((dlNoteId != wr.viewHolder.noteId)
					|| (!wr.mimeType.equals(wr.viewHolder.mimeType))) {
				return;
			}
			/*2012-10-11, add end*/ 
			if(obj instanceof ImageWrapper) {
				final ImageWrapper imgWrapper = (ImageWrapper) obj;
				// download Note is invalid
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				/*if(dlNoteId != imgWrapper.viewHolder.noteId) {
					return;
				}*/
				/*2012-10-11, add end*/ 
				getBitmapForMimeImage(imgWrapper.context, 
						imgWrapper.viewHolder, 
						imgWrapper.qnc_uri, 
						imgWrapper.qnc,
						/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
						dlNoteId,
						imgWrapper.mimeType);
						/*2012-10-11, add end*/ 
			} else if(obj instanceof AudioWrapper) {
				final AudioWrapper audioWrapper = (AudioWrapper) obj;
				// download Note is invalid
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				/*if(dlNoteId != audioWrapper.viewHolder.noteId) {
					return;
				}*/
				/*2012-10-11, add end*/ 
				getBitmapForMimeAudio(audioWrapper.context,
						audioWrapper.viewHolder, 
						audioWrapper.qnc, 
						audioWrapper.parentView,
						audioWrapper.bgcolor,
						/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
						dlNoteId,
						audioWrapper.mimeType);
						/*2012-10-11, add end*/ 
			/*2012-9-18, add end*/ 
			}
		}
		
		@Override
		public boolean sendMessageAtTime(Message msg, long uptimemillis) {
			// TODO Auto-generated method stub
			return super.sendMessageAtTime(msg, uptimemillis);
		}

		private void getBitmapForMimeImage(Context context, ViewHolder viewHolder,
				String qnc_uri, QNContent qnc,
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				final int downloadNoteId,
				final String mimeType) {
				/*2012-10-11, add end*/ 
			/*2012-9-18, add end*/ 
			Bitmap bmp = null;
			String fPath = QNUtil.transFilePath(qnc_uri.toString());
			Uri mediaUri = QNUtil.mediaStore_uri(context, fPath);
			if (mediaUri == null) {
				// this usually happens when the user stays in the thumbnails
			    QNDev.log(TAG+"Hi, this is a very special case and only for the image note!");
//		                errorHandle(viewHolder);
//		                return;
			}
			Long origId = mediaUri == null ? null : ContentUris.parseId(mediaUri);
			QNDev.log(TAG + "fPath = " + fPath + " mediaUri = " + mediaUri);

			// String thumb_uri_string =
			// cursor.getString(cursor.getColumnIndex(QNColumn.THUMBURI.column()));

			if (null != bmp) {bmp.recycle();}
			
			if (null != origId) {
				// Uri thumb_uri = QNUtil.buildUri(thumb_uri_string);
				QNDev.log(TAG + "try to get the thumbnail, origId = " + origId);
			    bmp = qnc.widgetThumbBitmap(context.getContentResolver(), origId, viewHolder.layout_width, viewHolder.layout_height);
			}
			if (null == bmp) {
			   QNDev.log(TAG+"cannot get the thumbnail,try to get the original image!");
			   //cannot get the thumbnail, try to get the original image
			   bmp = qnc.widgetBitmap(context, viewHolder.layout_width, viewHolder.layout_height); 
			   /*2012-12-12, add by amt_sunzhao for SWITCHUITWO-226 */
			   /*
			    * As Thumbnails.getThumbnail in widgetThumbBitmap will add default black background
			    * for no background, so draw black background on canvas first.
			    */
			   /*2012-12-13, add by amt_sunzhao for SWITCHUITWO-314 */
			   if(null != bmp) {
				   final Bitmap bgBmp = Bitmap.createBitmap(viewHolder.layout_width, viewHolder.layout_height, bmp.getConfig());
				   final Canvas c = new Canvas(bgBmp);
				   c.drawColor(Color.BLACK);
				   c.drawBitmap(bmp, 0.0f, 0.0f, null);
				   
				   bmp.recycle();
				   bmp = bgBmp;
			   }
			   /*2012-12-13, add end*/
			   /*2012-12-12, add end*/
			}
			if (null != bmp) {
				// get the bmp by thumbnail or original image
				QNDev.log(TAG + "get the image");
				/*viewHolder.image.setImageBitmap(bmp);
			    viewHolder.image.setBackgroundResource(R.drawable.ic_picture_line_transparent);
				viewHolder.image.setVisibility(View.VISIBLE);*/
				BitmapImageWrapper wr = new BitmapImageWrapper();
				wr.bmp = bmp;
				wr.viewHolder = viewHolder;
				/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				wr.downloadNoteId = downloadNoteId;
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				wr.mimeType = mimeType;
				/*2012-9-18, add end*/ 
				Message msg = mUIHandler.obtainMessage(MSG_SET_BITMAP_IMAGE);
				/*2012-10-11, add end*/ 
				msg.obj = wr;
				mUIHandler.sendMessage(msg);
			} else {
				// file doen't not exist, show error pic
				QNDev.log(TAG + "fail to get the image, show the error png");
				//errorHandle(viewHolder);
				/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				sendErrorHandle(viewHolder, downloadNoteId, mimeType);
				/*2012-10-11, add end*/ 
				/*2012-9-18, add end*/ 
			}
			qnc.close();
		}
		
		private void getBitmapForMimeAudio(Context context,
				ViewHolder viewHolder, 
				QNContent qnc,
				RelativeLayout parentView,
				final int bgcolor,
				/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				final int downloadNoteId,
				final String mimeType) {
				/*2012-10-11, add end*/ 
				/*2012-9-18, add end*/ 
			// audio note
			Bitmap bmp = null;
			if (null != bmp) {bmp.recycle();}
			
			bmp = qnc.widgetBitmap(context, viewHolder.layout_width, viewHolder.layout_height, bgcolor);
			if (null != bmp) {
				/*viewHolder.image.setImageBitmap(bmp);
				viewHolder.image.setVisibility(View.VISIBLE);
				parentView.setBackgroundResource(0);*/
				BitmapAudioWrapper wr = new BitmapAudioWrapper();
				wr.viewHolder = viewHolder;
				wr.parentView = parentView;
				wr.bmp = bmp;
				/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				wr.downloadNoteId = downloadNoteId;
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				wr.mimeType = mimeType;
				/*2012-10-11, add end*/ 
				/*2012-9-18, add end*/ 
				Message msg = mUIHandler.obtainMessage(MSG_SET_BITMAP_AUDIO);
				msg.obj = wr;
				mUIHandler.sendMessage(msg);
			} else {
				// file doen't not exist, show error pic
				//errorHandle(viewHolder);
				/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
				/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
				sendErrorHandle(viewHolder, downloadNoteId, mimeType);
				/*2012-10-11, add end*/ 
				/*2012-9-18, add end*/ 
			}
			qnc.close();
		}
		
		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
		private void sendErrorHandle(ViewHolder viewHolder, final int downloadNoteId,
				final String mimeType) {
				/*2012-10-11, add end*/ 
			Message msg = mUIHandler.obtainMessage(MSG_ERROR_HANDLE);
			ErrorWrapper wr = new ErrorWrapper();
			wr.viewHolder = viewHolder;
			wr.downloadNoteId = downloadNoteId;
			/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
			wr.mimeType = mimeType;
			/*2012-10-11, add end*/ 
			msg.obj = wr;
			/*2012-9-18, add end*/ 
			mUIHandler.sendMessage(msg);
		}
	}
	
	/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
	static class BaseWrapper {
		int downloadNoteId;
		/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
		ViewHolder viewHolder;
		String mimeType;
	}
	
	static class BitmapImageWrapper extends BaseWrapper {
		Bitmap bmp;
	}
	
	static class BitmapAudioWrapper extends BaseWrapper {
		RelativeLayout parentView;
		Bitmap bmp;
	}
	
	static class ErrorWrapper extends BaseWrapper {
	}
	/*2012-10-11, add end*/ 
	/*2012-9-18, add end*/ 
	
	public static final int MSG_SET_BITMAP_IMAGE = 0;
	public static final int MSG_SET_BITMAP_AUDIO = 1;
	public static final int MSG_ERROR_HANDLE = 2;
	static class UIHandler extends Handler {
		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		private GridView mGv = null;
		
		public UIHandler(final GridView gv) {
			super();
			mGv = gv;
		}
		
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			final int what = msg.what;
			final BaseWrapper bw = (BaseWrapper) msg.obj;
			final int pos = bw.downloadNoteId + 1;
			final int first = mGv.getFirstVisiblePosition();
			final int last = mGv.getLastVisiblePosition();
			// item in position is not visible, don't handle
			if((first > pos)
					|| (pos > last)
					/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
					|| (bw.downloadNoteId != bw.viewHolder.noteId)
					|| (!bw.mimeType.equals(bw.viewHolder.mimeType))){
					/*2012-10-11, add end*/ 
				return;
			}
			/*2012-9-18, add end*/ 
			switch (what) {
			case MSG_SET_BITMAP_IMAGE:
				BitmapImageWrapper wrImage = (BitmapImageWrapper) msg.obj;
				wrImage.viewHolder.image.setImageBitmap(wrImage.bmp);
				wrImage.viewHolder.image.setBackgroundResource(R.drawable.ic_picture_line_transparent);
				wrImage.viewHolder.image.setVisibility(View.VISIBLE);
				break;
			case MSG_SET_BITMAP_AUDIO:
				BitmapAudioWrapper wrAudio = (BitmapAudioWrapper) msg.obj;
				wrAudio.viewHolder.image.setImageBitmap(wrAudio.bmp);
				wrAudio.viewHolder.image.setVisibility(View.VISIBLE);
				wrAudio.parentView.setBackgroundResource(0);
				break;
			case MSG_ERROR_HANDLE:
				/*2012-9-21, add by amt_sunzhao for SWITCHUITWOV-219 */ 
				ErrorWrapper eh = (ErrorWrapper) msg.obj;
				ViewHolder viewHolder = eh.viewHolder;
				/*2012-9-21, add end*/ 
				errorHandle(viewHolder);
				break;
			default:
				break;
			}
		}
	}

	/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
	public static void itemMatch(Context context, Cursor cursor, int noteId,
			ViewHolder viewHolder, final ObtainBitmapHandler handler) {
		itemMatch(context, cursor, noteId, viewHolder, false, handler);
	}

	public static void itemMatch(Context context, Cursor cursor, int noteId,
	/*2012-9-18, add end*/ 
			ViewHolder viewHolder, boolean bSelected, final ObtainBitmapHandler handler) {
	/*2012-9-6, add end*/ 
		int ResId = 0;
		Bitmap bmp = null;
		/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
        // set default text and image.
        viewHolder.text.setText("");
        viewHolder.image.setImageDrawable(null);
		/*2012-10-11, add end*/ 

		if (bSelected) {
			viewHolder.selection.setVisibility(View.VISIBLE);
		} else {
			viewHolder.selection.setVisibility(View.GONE);
		}
		
		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
		if (!cursor.moveToPosition(noteId)) {
		/*2012-9-18, add end*/ 
			// Failed to move to the position
			// so just show the error
			errorHandle(viewHolder);
			return;
		}

        String mimeType = cursor.getString(cursor.getColumnIndex(QNColumn.MIMETYPE.column()));
        String qnc_uri =  cursor.getString(cursor.getColumnIndex(QNColumn.URI.column()));
        long reminder = cursor.getLong(cursor.getColumnIndex(QNColumn.REMINDER.column()));
        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
        long incall = cursor.getLong(cursor.getColumnIndex(QNColumn.CALLDATEEND.column()));
        // END IKCNDEVICS-504

        QNContent qnc = QNContent_Factory.Create(mimeType, QNUtil.buildUri(qnc_uri));
		/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
        viewHolder.mimeType = mimeType;
		/*2012-10-11, add end*/ 

		// set the title
        String title = cursor.getString(cursor.getColumnIndex(QNColumn.TITLE.column()));
		if ((null != title) && (title.length() > 0)) {
			viewHolder.note_title.setText(title);
			viewHolder.note_title.setVisibility(View.VISIBLE);
			viewHolder.separator.setVisibility(View.VISIBLE);
		} else {
			// title length is zero, hide this title
			viewHolder.note_title.setVisibility(View.GONE);
			viewHolder.separator.setVisibility(View.GONE);
		}

		// set the reminder icon
		if (0 != reminder) {
			viewHolder.image_reminder.setVisibility(View.VISIBLE);
		}

		// BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call
		// quicknote demo.
		// set the incall icon
		if (0 != incall) {
			viewHolder.image_incall.setVisibility(View.VISIBLE);
		}
		// END IKCNDEVICS-504
		ResId = getBackgroundResId(cursor);
        RelativeLayout parentView = (RelativeLayout) viewHolder.image.getParent();
		parentView.setBackgroundResource(ResId);
        viewHolder.image.setBackgroundResource(0);

		// handle error case
		if ((qnc == null) || (qnc instanceof QNContent_Error)) {
			errorHandle(viewHolder);
			return;
		}

		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
        viewHolder.noteId = noteId;
		/*2012-9-18, add end*/ 
		if (mimeType.startsWith("text/")) {
			// text note
            viewHolder.text.setText(qnc.getTextNoteContentShort());
			viewHolder.text.setVisibility(View.VISIBLE);
			/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
			qnc.close();
		} else if (mimeType.startsWith("image/")) {
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 			
			handler.removeMessages(noteId);
			final Message msg = handler.obtainMessage(noteId);
			/*2012-9-18, add end*/ 
			final ImageWrapper wr = new ImageWrapper();
			wr.context = context.getApplicationContext();
			wr.viewHolder = viewHolder;
			wr.qnc_uri = qnc_uri;
			wr.qnc = qnc;
			/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
			wr.mimeType = mimeType;
			/*2012-10-11, add end*/ 
			msg.obj = wr;
			handler.sendMessage(msg);
			// image note, use thumbnails if it has thumbnails, otherwise use
			// the original bmp
			// Long origId =
			// cursor.getLong(cursor.getColumnIndex(QNColumn.ORIGID.column()));
			/*String fPath = QNUtil.transFilePath(qnc_uri.toString());
			Uri mediaUri = QNUtil.mediaStore_uri(context, fPath);
			if (mediaUri == null) {
				// this usually happens when the user stays in the thumbnails
                QNDev.log(TAG+"Hi, this is a very special case and only for the image note!");
//                errorHandle(viewHolder);
//                return;
			}
            Long origId = mediaUri == null ? null : ContentUris.parseId(mediaUri);
			QNDev.log(TAG + "fPath = " + fPath + " mediaUri = " + mediaUri);

			// String thumb_uri_string =
			// cursor.getString(cursor.getColumnIndex(QNColumn.THUMBURI.column()));

            if (null != bmp) {bmp.recycle();}
            
			if (null != origId) {
				// Uri thumb_uri = QNUtil.buildUri(thumb_uri_string);
				QNDev.log(TAG + "try to get the thumbnail, origId = " + origId);
                bmp = qnc.widgetThumbBitmap(context.getContentResolver(), origId, viewHolder.layout_width, viewHolder.layout_height);
			}
			if (null == bmp) {
               QNDev.log(TAG+"cannot get the thumbnail,try to get the original image!");
               //cannot get the thumbnail, try to get the original image
               bmp = qnc.widgetBitmap(context, viewHolder.layout_width, viewHolder.layout_height); 
			}
			if (null != bmp) {
				// get the bmp by thumbnail or original image
				QNDev.log(TAG + "get the image");
				viewHolder.image.setImageBitmap(bmp);
                viewHolder.image.setBackgroundResource(R.drawable.ic_picture_line_transparent);
				viewHolder.image.setVisibility(View.VISIBLE);
			} else {
				// file doen't not exist, show error pic
				QNDev.log(TAG + "fail to get the image, show the error png");
				errorHandle(viewHolder);
			}*/
		} else if (mimeType.startsWith("audio/")) {
			// audio note			
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
			handler.removeMessages(noteId);
			final Message msg = handler.obtainMessage(noteId);
			/*2012-9-18, add end*/ 
			final AudioWrapper wr = new AudioWrapper();
			wr.context = context;
			wr.parentView = parentView;
			wr.qnc = qnc;
			wr.viewHolder = viewHolder;
			wr.bgcolor = cursor.getInt(cursor.getColumnIndex(QNColumn.BGCOLOR.column()));
			/*2012-10-11, add by amt_sunzhao for SWITCHUITWOV-269 */ 
			wr.mimeType = mimeType;
			/*2012-10-11, add end*/ 
			
			msg.obj = wr;
			handler.sendMessage(msg);
            /*if (null != bmp) {bmp.recycle();}
            
            int bgcolor = cursor.getInt(cursor.getColumnIndex(QNColumn.BGCOLOR.column()));
            bmp = qnc.widgetBitmap(context, viewHolder.layout_width, viewHolder.layout_height, bgcolor);
			if (null != bmp) {
				viewHolder.image.setImageBitmap(bmp);
				viewHolder.image.setVisibility(View.VISIBLE);
                parentView.setBackgroundResource(0);
			} else {
				// file doen't not exist, show error pic
				errorHandle(viewHolder);
			}*/
		}

		//qnc.close();
		/*2012-9-6, add end*/ 
	}

    private static void errorHandle(ViewHolder viewHolder) {
        RelativeLayout parentView = (RelativeLayout) viewHolder.image.getParent();
        parentView.setBackgroundResource(R.drawable.bg_thb_gridme);
        
        viewHolder.note_title.setMaxLines(2); 
        viewHolder.image_error.setVisibility(View.VISIBLE);
        viewHolder.text_error.setVisibility(View.VISIBLE);
    }

	/**
	 * Adapter the notes to the gridView
	 */
   public class NoteAdapter extends BaseAdapter 
    {
		private LayoutInflater mInflater;
		private Context mContext;

		public NoteAdapter(Context c) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = getLayoutInflater();
			mContext = c;
		}

		// ---returns the number of notes in QuickNotesDB
		public int getCount() {
			// count in the special "Add" note in the 1st position
			return (mNum + 1);
		}

		// ---returns a note id
		public Object getItem(int position) {
			QNDev.log(TAG + " getItem(" + position + ")");
			return QuickNotesDB.getItem(QNDisplayNotes.this, position);
		}

		// ---returns the ID of the note
		public long getItemId(int position) {
			return position;
		}

		// ---returns an ImageView view---
		public View getView(int position, View convertView, ViewGroup parent) {
			QNDev.log(TAG + " getView(" + position + ")");
			ViewHolder viewHolder;
			/*2012-9-13, add by amt_sunzhao for SWITCHUITWOV-175 */ 
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
			/*int vhPostion = -1;
			if(null != convertView) {
				viewHolder = (ViewHolder) convertView.getTag();
				if(null != viewHolder) {
					vhPostion = viewHolder.postion;
				}
			}*/
			/*if (convertView == null
					|| (position != vhPostion)) {*/
			if(convertView == null) {
			/*2012-9-18, add end*/ 
			/*2012-9-13, add end*/ 
				// set the layout and return it
               convertView = mInflater.inflate(R.layout.display_notes_item, null);
               RelativeLayout layout = (RelativeLayout)convertView.findViewById(R.id.main_notes);
               layout.setLayoutParams(new GridView.LayoutParams(mThumbLayoutWidth, mThumbLayoutHeight));

				// create a ViewHolder and store references to the childeren
				// views that we want to bind date to
				viewHolder = new ViewHolder();
               viewHolder.image = (ImageView)convertView.findViewById(R.id.image);
               viewHolder.text = (TextView)convertView.findViewById(R.id.text);
               viewHolder.note_title = (TextView)convertView.findViewById(R.id.note_title);
               viewHolder.separator = (ImageView) convertView.findViewById(R.id.separator);
               viewHolder.selection = (ImageView) convertView.findViewById(R.id.selection);
               viewHolder.image_error = (ImageView)convertView.findViewById(R.id.image_error);
               viewHolder.text_error = (TextView)convertView.findViewById(R.id.text_error);
               viewHolder.image_reminder = (ImageView)convertView.findViewById(R.id.image_reminder);
               // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
               viewHolder.image_incall = (ImageView)convertView.findViewById(R.id.image_incall);
               // END IKCNDEVICS-504
               viewHolder.layout_width = mThumbLayoutWidth;
               viewHolder.layout_height = mThumbLayoutHeight;
			   /*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-190 */ 
               //viewHolder.postion = position;
               /*2012-9-18, add end*/ 
               convertView.setTag(viewHolder);
            } else {
               viewHolder = (ViewHolder)convertView.getTag();
               //reset all the views in it
               viewHolder.image.setVisibility(View.GONE);
               viewHolder.text.setVisibility(View.GONE);
               viewHolder.note_title.setVisibility(View.GONE);
               viewHolder.separator.setVisibility(View.GONE);
               viewHolder.selection.setVisibility(View.GONE);
               viewHolder.image_error.setVisibility(View.GONE);
               viewHolder.text_error.setVisibility(View.GONE);
               viewHolder.image_reminder.setVisibility(View.GONE);
               // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
               viewHolder.image_incall.setVisibility(View.GONE);
               // END IKCNDEVICS-504
            }
           
            // Bind the data efficentely with the holder
            if (0 == position) {
                QNDev.log(TAG+"total gridview num = "+getCount()+" current position = "+position+" special position!");
				// The first position 0 is to place the QN function icon

				int ResId;
				if (mCountChecked == 0) {
					ResId = R.drawable.bg_thb_create_new;
				} else {
					ResId = R.drawable.bg_thb_create_new_disabled;
				}
                ((RelativeLayout)viewHolder.image.getParent()).setBackgroundResource(ResId);
				// viewHolder.image.setVisibility(View.VISIBLE);
			} else {
				// the real notes will be placed from position 1, and ordered
				// from the latest to the earlister
				Uri uri;
				int NoteId = position - 1;
                QNDev.log(TAG+"total gridview num = "+getCount()+" current position = "+position+" NoteId= "+ NoteId);
					/*2012-9-6, add by amt_sunzhao for T810T_P003933 */ 
                itemMatch(QNDisplayNotes.this, mCursor, NoteId, viewHolder, checkOn.get(position) ? true : false, mObtainBitmapH);
					/*2012-9-6, add end*/ 
			} // end of position except 0

			return convertView;
		} // end of getView

	} // end of NoteAdapter

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// set special grid layout according to the size of QN DB
		// port-land layout has differet format, so need reset the layout
		// layoutSelect(mNum);
	/*mThumbLayoutWidth = getResources().getDimensionPixelSize(R.dimen.thumb_layout_width);
        mThumbLayoutHeight = getResources().getDimensionPixelSize(R.dimen.thumb_layout_height);
		setupView();*/
		/*2012-12-7, add by amt_sunzhao for SWITCHUITWO-252 */
		setupSelectionButton();
		/*2012-12-7, add end*/
	}
	
	/*2012-9-12, add by amt_sunzhao for SWITCHUITWOV-158 */ 
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		final PopupMenu pm = mSelectionButton.getPopupMenu();
		if(null != pm){
	    	pm.dismiss();
	    }
	}
	/*2012-9-12, add end*/ 

   @Override
   public void onResume() {
       QNDev.log(TAG+ "onResume, ");
       super.onResume();
  
	if (mCursor == null) {
			Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

     	mNum = mCursor.getCount();
		int checkOnLength = checkOn.size();
		for (int i = 0; i <= mNum; i++) {
			if(i>= checkOnLength){			
				 checkOn.add(i, false);			
			}
		}
   
	   /*2012-9-12, add by amt_sunzhao for SWITCHUITWOV-158 */ 
       /*if(null != mSelectionButton.getPopupMenu()){
    	   mSelectionButton.getPopupMenu().dismiss();
       }*/
       /*2012-9-12, add end*/
   }

	public void onBackPressed() {
		if (mCountChecked != 0) {
			ChangeSelection(SELECTION_NONE);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	private void ChangeSelection(int selectionType) {
		ChangeSelection(selectionType, 0, null);
	}

	private boolean ChangeSelection(int selectionType, int position, View view) {
		boolean bChanged = false;
		View itemView = null;

		if (selectionType == SELECTION_NONE) {
//            mContentUri.clear();
            mCountChecked = 0;
//            mCountDeleted = 0;
			mExitMultiButton.setVisibility(View.GONE);
			mDeleteButton.setVisibility(View.GONE);
			mSelectionButton.setDropdownable(false);
            mSelectionButton.setTitle(getResources().getString(R.string.quicknote_app_name));

			// if position is 0, from unselect all action.
			if (position == 0) {
				int offset = mGrid.getFirstVisiblePosition();
				for (int i = 1; i < checkOn.size(); i++) {
					if (checkOn.get(i)) {
						itemView = null;
                        if(i - offset >= 0 && i - offset < mGrid.getChildCount()) {
							itemView = mGrid.getChildAt(i - offset);
						}
						if (itemView != null) {
                            ViewHolder viewHolder = (ViewHolder)itemView.getTag();
							if (viewHolder != null) {
								viewHolder.selection.setVisibility(View.GONE);
							}
						}
						checkOn.set(i, false);
					}
				}
			}
			bChanged = true;
		} else {
			if (selectionType == SELECTION_DECREASE) {
				if (checkOn.get(position) && position < mGrid.getCount()
						&& position > 0) {
					if (view != null) {
						ViewHolder viewHolder = (ViewHolder) view.getTag();
						if (viewHolder != null) {
							viewHolder.selection.setVisibility(View.GONE);
						}
					}
					checkOn.set(position, false);
					mCountChecked--;
					bChanged = true;
				}

				if (mCountChecked == 0) {
					return ChangeSelection(SELECTION_NONE, position, view);
				}
			} else if (selectionType == SELECTION_ALL) {
				mCountChecked = mGrid.getCount() - 1;
				int offset = mGrid.getFirstVisiblePosition();

				for (int i = 1; i < checkOn.size(); i++) {
					if (!checkOn.get(i)) {
						itemView = null;
                        if(i - offset >= 0 && i - offset < mGrid.getChildCount()) {
							itemView = mGrid.getChildAt(i - offset);
						}
						if (itemView != null) {
                            ViewHolder viewHolder= (ViewHolder)itemView.getTag();
                            if(viewHolder != null) {
                                viewHolder.selection.setVisibility(View.VISIBLE);
							}
						}
						checkOn.set(i, true);

						if (!bChanged) {
							bChanged = true;
						}
					}
				}
			} else if (selectionType == SELECTION_INCREASE) {
                if (!checkOn.get(position) && position < mGrid.getCount() && position > 0) {
					mCountChecked++;
					checkOn.set(position, true);
					if (view != null && view.getTag() != null) {
						ViewHolder viewHolder = (ViewHolder) view.getTag();
						viewHolder.selection.setVisibility(View.VISIBLE);
					}
					bChanged = true;
				}
			}

			if (bChanged) {
				mExitMultiButton.setVisibility(View.VISIBLE);
				mDeleteButton.setVisibility(View.VISIBLE);
				mSelectionButton.setDropdownable(true);

				
				
				final String formatString = (mCountChecked > 1) ? getString(R.string.notes_selected):getString(R.string.note_selected);
				final String message = MessageFormat.format(formatString,
						mCountChecked);
				mSelectionButton.setTitle(message);
			}
		}

        if( mCountChecked ==0 || (mCountChecked == 1 && selectionType == SELECTION_INCREASE)) {
            int offset = mGrid.getFirstVisiblePosition();
			View addView = offset == 0 ? mGrid.getChildAt(0) : null;

			if (addView != null) {
				int ResId;
				if (mCountChecked == 0) {
					ResId = R.drawable.bg_thb_create_new;
				} else {
					ResId = R.drawable.bg_thb_create_new_disabled;
				}
				addView.setBackgroundResource(ResId);
			}
		}

		return bChanged;
	}

	static int getBackgroundResId(Cursor cursor) {
		int ResId = 0;
		int bgcolor = -1;
        String mimeType = cursor.getString(cursor.getColumnIndex(QNColumn.MIMETYPE.column()));
        bgcolor = cursor.getInt(cursor.getColumnIndex(QNColumn.BGCOLOR.column()));
        
        ResId = _BackgroundRes.find_drawable(bgcolor, "thumb", mimeType);
        
        return ResId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO Auto-generated method stub
        
        Uri baseUri;
        
        baseUri = QuickNotesDB.CONTENT_URI;
        return new CursorLoader(this, baseUri, null, null, null, QNColumn._ID + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
      //update the thumbnails
    	QNDev.log(TAG+ "onLoadFinished");
    	if(!bDeleting){
	        mCursor.requery(); 
	        int notesNum = mCursor.getCount();
	        if(mNum != notesNum){
	             QNDev.log(TAG+ "onResume, notesNum = " + notesNum);
	             mNum = notesNum;
	             checkOn.clear();
	             for (int i = 0; i <= mNum; i++) {
	                 checkOn.add(i, false);
	             }
	             ChangeSelection(SELECTION_NONE);
	             
	             ((NoteAdapter)mGrid.getAdapter()).notifyDataSetChanged();
	        }
    	}
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
        
    }
}
