/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Define constants and common public functions that is used in quicknote.
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 28 : first created.
 * 
 * 
 *****************************************************************************************/

package com.motorola.quicknote;

import java.io.File;

import com.motorola.quicknote.R;
import android.os.Environment;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

final public class QNConstants {
	/******************************
	 * Constants
	 ******************************/
	static final String PACKAGE = "com.motorola.quicknote";
	static final String XMLNS = "http://schemas.android.com/apk/res/com.motorola.quicknote";

	static final String SR_PACKAGE = "com.android.soundrecorder";
	static final String CAMERA_PACKAGE = "com.android.camera";
	static final String INFO_FOR_CAMERA = "qncamera";

	static final String REQUEST_CODE = "request_code";
	static final int REQUEST_CODE_NONE = 0;
	static final int REQUEST_CODE_CAMERA = 5;
	static final int REQUEST_CODE_SOUND_RECORDER = 6;
	static final int REQUEST_CODE_CREATE_NEW_NOTE = 7;
	static final int REQUEST_CODE_QN_SKETCH_EDIT = 8;
	static final int REQUEST_CODE_SKETCHAPP_EDIT = 9;

	// These (intent.. stuffs) should match 'manifest'!!!

	static final String INTENT_ACTION_NEW = "com.motorola.quicknote.action.NEW";
	// BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call
	// quicknote demo.
	static final String INTENT_ACTION_NEW_INCALL = "com.motorola.quicknote.action.NEW_INCALL";
	// END IKCNDEVICS-504
	static final String INTENT_ACTION_EDIT = "com.motorola.quicknote.action.EDIT";
	static final String INTENT_ACTION_SCREENSHOT = "com.motorola.quicknote.action.SCREENSHOT";
	static final String INTENT_ACTION_DETAIL = "com.motorola.quicknote.action.DETAIL";
	static final String INTENT_ACTION_NOTE_CREATED = "com.motorola.quicknote.action.NOTE_CREATED";
	static final String INTENT_ACTION_NOTE_UPDATED = "com.motorola.quicknote.action.NOTE_UPDATED";
	static final String INTENT_ACTION_NOTE_DELETED = "com.motorola.quicknote.action.NOTE_DELETED";
	/*2012-10-8, add by amt_sunzhao for SWITCHUITWOV-246 */ 
	static final String INTENT_ACTION_NOTE_DELETE_TASKS = "com.motorola.quicknote.action.NOTE_DELETE_TASKS";
	static final String KEY_DELETE_TASKS_COUNT = "key.delete.tasks.count";
	/*2012-10-8, add end*/ 
	static final String INTENT_ACTION_NOTE_VIEWED = "com.motorola.quicknote.action.NOTE_VIEWED";
	static final String INTENT_ACTION_APPWIDGET = "com.motorola.quicknote.action.APPWIDGET";
	static final String INTENT_ACTION_FIND_EMPTY_CELL = "com.motorola.quicknote.action.FIND_EMPTY_CELL";
	static final String INTENT_ACTION_REMOVE_WIDGET = "com.motorola.quicknote.action.REMOVE_WIDGET";
	static final String INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED = "com.motorola.quicknote.action.MEDIA_MOUNTED_UNMOUNTED";
	static final String INTENT_ACTION_REMINDER_TIME_OUT = "com.motorola.quicknote.action.REMINDER_TIME_OUT";

	static final String MIME_TYPE_TEXT = "text/plain";
	static final String MIME_TYPE_IMAGE = "image/jpeg";
	static final String MIME_TYPE_VOICE = "audio/amr"; // "audio/mpeg";
	// Name of bundle data keys
	static final String NOTE_URI = "note_uri";
	// id in gridview
	static final String GRID_ID = "grid_id";

	// Minimum free space size of SD to run QN.
    static final long   MIN_STORAGE_REQUIRED = 1024*1024; // at least 1M free space in SD is required to run QN
    
    // below varivaries will be set in initDir in QNUtil.java
    static String NOTE_DIRECTORY;
    static String SKETCH_DIRECTORY;
    static String SKETCH_TEMP_FILE_DIRECTORY;
    static String TEMP_DIRECTORY;
    static String IMAGE_DIRECTORY;
    static String SNAPSHOT_DIRECTORY;
    static String TEXT_DIRECTORY;

	// set the size of the item in gridview
	static final int gdSpanX = 4 / 3;
	static final int gdSpanY = 4 / 3;

	// set the background of the text note
	public static enum _BackgroundRes {
		GRIDME(R.color.yellow, R.drawable.bg_text_note_gridme_single,
				R.drawable.bg_text_note_gridme,
				R.drawable.bg_microphone_gridme, R.drawable.bg_thb_gridme,
				R.drawable.bg_thb_microphone_gridme), MATHEMATICS(R.color.blue,
				R.drawable.bg_text_note_mathematics_single,
				R.drawable.bg_text_note_mathematics,
				R.drawable.bg_microphone_mathmatics,
				R.drawable.bg_thb_mathematics,
				R.drawable.bg_thb_microphone_mathematics), GRAPHY(
				R.color.orange, R.drawable.bg_text_note_graphy_single,
				R.drawable.bg_text_note_graphy,
				R.drawable.bg_microphone_graphy, R.drawable.bg_thb_graphy,
				R.drawable.bg_thb_microphone_graphy), PROJECT_PAPER(
				R.color.green, R.drawable.bg_text_note_project_paper_single,
				R.drawable.bg_text_note_project_paper,
				R.drawable.bg_microphone_project_paper,
				R.drawable.bg_thb_project_paper,
				R.drawable.bg_thb_microphone_project_paper);

		private int _index;
		private int _text_view_resId;
		private int _text_edit_resId;
		private int _sound_note_icon_resId;
		private int _thumb_resId;
		private int _thumb_snd_resId;

		_BackgroundRes(int index, int text_view_resId, int text_edit_resId,
				int sound_note_icon_resId, int thumb_resId, int thumb_snd_resId) {
			_index = index;
			_text_view_resId = text_view_resId;
			_text_edit_resId = text_edit_resId;
			_sound_note_icon_resId = sound_note_icon_resId;
			_thumb_resId = thumb_resId;
			_thumb_snd_resId = thumb_snd_resId;
		}

		public int column() {
			return _index;
		}

		public int textViewResId() {
			return _text_view_resId;
		}

		public int textEditResId() {
			return _text_edit_resId;
		}

		public int soundNoteIconResId() {
			return _sound_note_icon_resId;
		}

		public int thumbResId() {
			return _thumb_resId;
		}

		public int thumbSndResId() {
			return _thumb_snd_resId;
		}

		// 0 means cannot file...
		public static int find_drawable(int color, String mode, String type) {
			for (_BackgroundRes m : _BackgroundRes.values()) {
				if (m.column() == color) {
					if ("view".equals(mode)) {
						if ("text".equals(type) || "audio".equals(type)) {
							return m.textViewResId();
						} else if ("voice_icon".equals(type)) {
							return m.soundNoteIconResId();
						}
					} else if ("edit".equals(mode)) {
						if ("text".equals(type) || "audio".equals(type)) {
							return m.textEditResId();
						} else if ("voice_icon".equals(type)) {
							return m.soundNoteIconResId();
						}
					} else if ("thumb".equals(mode)) {
						if (type.startsWith("text")) {
							return m.thumbResId();
						} else if (type.startsWith("audio")) {
							return m.thumbSndResId();
						}
					}
				}
			}
            QNDev.qnAssert(false);
            //use default bg:bg_text_note_gridme_single
            Log.d("find_drawable - for error debug"," BG will use the default value. color = "+color+" mode = "+mode+" type = "+type);
            //this error case may happen in
            //1. bota upgrade: in GB version, the color definition is different from ICS;
            // 2. and other error cases unexpected
            if ("view".equals(mode)) {
               if ("text".equals(type) ||  "audio".equals(type)) {
                   return R.drawable.bg_text_note_gridme_single;
               } else if ("voice_icon".equals(type)){
                   return R.drawable.bg_microphone_gridme;
               }
            } else if ("edit".equals(mode)) {
               if ("text".equals(type) ||  "audio".equals(type)) {
                  return R.drawable.bg_text_note_gridme;
               } else  if ("voice_icon".equals(type)){
                  return R.drawable.bg_microphone_gridme;
               }
            } else if ("thumb".equals(mode)) {
               if(type.startsWith("text")) {
                  return R.drawable.bg_thb_gridme;
               } else if(type.startsWith("audio")) {
                  return R.drawable.bg_thb_microphone_gridme;
               }
            }

            return 0;
        }
    }
    
    
    /**************************
     * common public Functions
     **************************/
    /*public static int widget_width(Context context, int cell) {
        Resources res = context.getResources();
        return res.getDimensionPixelSize(R.dimen.cell_width) * cell
                + res.getDimensionPixelSize(R.dimen.workspace_hori_padding) * (cell -1);
    }
    
    public static int widget_height(Context context, int cell) {
        Resources res = context.getResources();
        return res.getDimensionPixelSize(R.dimen.cell_height) * cell
                + res.getDimensionPixelSize(R.dimen.workspace_vert_padding) * (cell -1);
    }*/
}
