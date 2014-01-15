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
 * [QuickNote] Handle UI in sketch
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created.
 * 
 * 
 *****************************************************************************************/

package com.motorola.quicknote;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;
import android.view.MenuInflater;
import android.view.LayoutInflater;

import com.motorola.quicknote.Board.ToolE;
import com.motorola.quicknote.QNAppWidgetConfigure;

//import com.motorola.android.widget.ActionBar;

public class QNSketcher extends QNActivity implements View.OnClickListener, View.OnTouchListener {
	/******************************
	 * Constants
	 ******************************/

	private static final String TAG = "QNSketcher";

	/**
	 * constants that indicates intent data type - first value of intent extra
	 * field!
	 */
	static final int INTENT_DATA_UNKNOWN = 0x00; //
	static final int INTENT_DATA_URI = 0x01; // intent data type is uri
	static final int INTENT_DATA_FILE = 0x02; // intent data type is file path..

	// sketch temp file after this time will be deleted automatically even if it
	// is not used up!
	private static final long _TEMP_FILE_LIFE_MILLS = 1000 * 60 * 60 * 24; // one
																			// day.

	// default sketch name prefix
    //private static final String _DEFAULT_SKETCH_NAME_PREFIX = "mysketch";

	// Keys for bundling!!!
	private static final String _BGFILE = "background_imagefile";

	// Open with Portrait mode for Landscape mode
	private boolean initori = false;
	private boolean ori = false;

    private static final int[] COLOR_BASE = new int[] {
                0xFFFF0000, 0xFFFFFF00,  0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
    private static final int CROSS_BAR_RADIUS = 10;
    private static final int DEFAULT_CUSTOME_COLOR = 0xFF00FFFF;


    public class ColorPreviewView extends View {

        public ColorPreviewView(Context context) {super(context);}

		@Override
		protected void onDraw(Canvas canvas) {
			QNDev.logd(TAG, "ColorPreviewView::onDraw()");
			// if (mTrackingColor) {
			Rect rect = new Rect();
			mPenColorGradientPreview.getDrawingRect(rect);
			canvas.drawRect(rect, mPreviewPaint);
			mPenPreview.setBackgroundColor(mPreviewPaint.getColor());
			// }
		}
	}

    public class ColorGradientPicker extends View {
        public ColorGradientPicker(Context context) {super(context);}

		@Override
		protected void onDraw(Canvas canvas) {
			QNDev.logd(TAG, "ColorGradientPicker::onDraw()");

            Rect rect = new Rect();
            mPenColorGradientPicker.getDrawingRect(rect);
            LinearGradient gradient = new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, COLOR_BASE, null, Shader.TileMode.CLAMP);

			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setShader(gradient);
			paint.setStyle(Paint.Style.FILL);

            canvas.drawRect(rect, paint);
            // draw white-cross
            Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crossPaint.setStyle(Paint.Style.STROKE);
            crossPaint.setColor(android.graphics.Color.WHITE);
            crossPaint.setStrokeWidth(4);
            // defaultly set point to center position
            if (mUseDefaultPoint) {
                mPenColorGradientPicker.getDrawingRect(rect);
                mCurrentPoint.set(rect.left + (rect.right - rect.left)/2, rect.top + (rect.bottom - rect.top)/2);
                _board.pen_color(Board.ColorE.GRADIENT, DEFAULT_CUSTOME_COLOR);
                mUseDefaultPoint = false;
            }
            canvas.drawLine(mCurrentPoint.x - CROSS_BAR_RADIUS, mCurrentPoint.y, 
                            mCurrentPoint.x + CROSS_BAR_RADIUS, mCurrentPoint.y, crossPaint);
            canvas.drawLine(mCurrentPoint.x, mCurrentPoint.y - CROSS_BAR_RADIUS, 
                            mCurrentPoint.x, mCurrentPoint.y + CROSS_BAR_RADIUS, crossPaint);
        }
    }

	// for board's width, height, matching to the values in sketcher.xml
	private static class boardParameters {
		static int width_land;
		static int height_land;
		static int width_port;
		static int height_port;
	}

	// for colors
	private static int _COLOR_POPUP_WIDTH;
	private static int _COLOR_POPUP_HEIGHT;
	private static int _COLOR_POPUP_PORT_TOP; // color popup top position
												// (y-coordinate) in pixel
	private static int _COLOR_POPUP_LAND_LEFT; // color popup top position
												// (y-coordinate) in pixel
	private static int _COLOR_POPUP_QNS_WIDTH; // color QNS width in pixel
	private static int _COLOR_POPUP_QNS_LAND_WIDTH; // color QNS width in pixel
	private static int _COLOR_POPUP_OFFSET; // color indicate offset in pixel
	private static int _COLOR_POPUP_LAND_OFFSET; // color indicate offset in
													// pixel

    
    // which kind of format should we use as an sketch output.
    // It's for easy change sketch output format.
    // Sketch output format will not be changed during runtime!!
    private static enum  _SketchFormatE {
        SKETCH_PNG  (".png", "image/png", CompressFormat.PNG),
        SKETCH_JPG  (".jpg", "image/jpg", CompressFormat.JPEG);
        
        private String          _ext;
        private String          _mime;
        private CompressFormat  _format;
        
        _SketchFormatE(String ext, String mime, CompressFormat format) {
            _ext = ext; _mime = mime; _format = format;
        }
        
        String          File_extention()    { return _ext; }
        String          Mime()              { return _mime; }
        CompressFormat  Format()            { return _format; }
    }  
    
    /*******************
     * We should use PNG as an sketch output format
     * 
     * === [ Tested on Android Eclair version ] ===
     * Because, whenever sketch -> save -> open -> sketch..., 
     * image quality become worse if we use jpeg compression.
     * ( I think this may be 'skia' lib's problem. )
     * 
     * We can test easily this by this way.
     * "Decode file" as Bitmap -> Save Bitmap by using JPEG compress format with 100 quaility 
     *         ^                                       |
     *         +---------------------------------------+
     * As above cycle goes, jpeg image quality is worse.....
     *
     * In case of sketch, above use case is general. 
     * (Sketching -> saving -> open exiting sketch -> edit it -> save ...)
     * So, as a walkaround (even though we should endure spending more disk space), we should choose PNG.
     *******************/
    private static final _SketchFormatE _sketch_format = _SketchFormatE.SKETCH_PNG; 
    
    /***************************************************
     * Constants of other modules.
     * [!NOTE! - Why this is needed!!!]
     *   Skether is tightly coupled with MediaGallery.
     *   In Qilin project, MediaGallery in Mamba is used.
     *   So, if sketcher references MediaGallery member field, 
     *     sketcher should be a part of mamba!.
     *   But, QuickNote - especially Sketch - should be independent module
     *   (QuickNote may be used without Mamba!!)
     *   So, to decouple with Mamba, these hard-coded constants are used
     *   (By using this way, we don't have library-dependency with MediaGallery!)
     ***************************************************/
    
    /********************************************
     *  Other module's constants - START
     ********************************************/
    
    /************ from com.motorola.gallery.GlobalConfig.java ***************/
    private static final String _GALLERY_VIEW_INCLUSION          = "inclusion";
    private static final String _GALLERY_VIEW_TYPE               = "TYPE";
    private static final String _GALLERY_ALBUM_TITLE             = "TITLE";
    private static final String _GALLERY_BUCKET_ID               = "BUCKET_ID";
    private static final String _GALLERY_VIEW_MODE               = "Mode";
    private static final int    _GALLERY_IMAGES_FOLDER_TYPE      = 4;// Album_Browse_Screen
    private static final int    _GALLERY_MODE_SINPIC             = 0;
    
    /************ from com.motorola.gallery.ImageManager.java ***************/
    private static final int    _GALLERY_INCLUDE_IMAGES          = (1 << 0);

    /********************************************
     *  Other module's constants - END
     ********************************************/
    
    private enum _RunningModeE {
        SKETCH_NEW,
        SKETCH_EDIT,
        SKETCH_APP,
    }
    
    private enum _ReqCodeE{
        IMAGE_PICK  (1),
        NOTE_EDIT   (2);
        
        private final int _code;
        
        _ReqCodeE(int code) { _code = code; }
        int code() { return _code; }
    }
    
    /******************************
     * members
     ******************************/
    
    /** Called when the activity is first created. */
    private Board         _board = null;
    private _RunningModeE _runmode = null;
    private String        _bgfile = null;
    
    private Bundle        _stored_bundle = null;

	private boolean mTrackingColor = false;
	private Paint mPreviewPaint = null;
	private Point mCurrentPoint = new Point();
	private boolean mUseDefaultPoint = true;

	private ImageButton mBtnUndo;
	private ImageButton mBtnRedo;
	private ImageButton mBtnPen;
	private View mPenPreview;
	private ImageButton mBtnEraser;
	private View mEraserPreview;
	private ImageButton mBtnSave;
	private ImageButton mBtnDiscard;

	private ImageButton mPenBlack;
	private ImageButton mPenRed;
	private ImageButton mPenYellow;
	private ImageButton mPenGreen;
	private ImageButton mPenBlue;
	private ImageButton mPenGradient;

	private ImageButton mPenThickness1;
	private ImageButton mPenThickness2;
	private ImageButton mPenThickness3;
	private ImageButton mEraserThickness1;
	private ImageButton mEraserThickness2;
	private ImageButton mEraserThickness3;

	private LinearLayout mPenMenu;
	private LinearLayout mPenColorGradientBar;
	private ColorPreviewView mPenColorGradientPreview;
	private ColorGradientPicker mPenColorGradientPicker;
	private LinearLayout mEraserMenu;
	private ImageButton mClosePenPanelButton;
	private ImageButton mCloseEraserPanelButton;

	/**************************
	 * Local Functions
	 **************************/

    private boolean _check_sdcard() {
        if(!QNUtil.is_storage_mounted(this)) {
            Toast.makeText(this, R.string.no_sd_createsketcher, Toast.LENGTH_LONG).show();
            finish();
            return false;
        } else if (QNUtil.available_storage_space(this) <= QNConstants.MIN_STORAGE_REQUIRED) {
            Toast.makeText(this, R.string.not_enough_free_space_sd_for_sketcher, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }
  
    /**
     * This function is called only at (_Setup_behaviors(...))
     */
/*
    private QNSelector.OnChanged_Listener _Build_color_changed_listener(final QNSelector qns) {
        return new QNSelector.OnChanged_Listener() {
            public void onChange(View from, View to) {
                if(null != to) {
                    QNDev.qnAssert(to instanceof QNSelectorItem);
                    QNDev.qnAssert(_ColorMapE.values().length > 1);
                    if(QNUtil.is_landscape(QNSketcher.this)) {
                        _color_popup.update(((QNSelectorItem)to).detail(),
                            _COLOR_POPUP_LAND_LEFT,
                            301 - (((QNSelectorItem)to).index() + 1) * _COLOR_POPUP_QNS_LAND_WIDTH - _COLOR_POPUP_LAND_OFFSET);
                    } else {
                        _color_popup.update(((QNSelectorItem)to).detail(), 
                            qns.getLeft() + ((QNSelectorItem)to).index() * _COLOR_POPUP_QNS_WIDTH - _COLOR_POPUP_OFFSET,
                            _COLOR_POPUP_PORT_TOP);
                    }
                } else {
                    // 'from' cannot be 'null' in our current algorithm!
                    QNDev.qnAssert(null != from);
                    // Item that is last pressed, is always pressed  
                    qns.Touch(from);
                }
            }
        };        
    }
    
    private QNSelector.OnSelected_Listener _Build_color_selected_listener(final QNSelector qns) {
        return new QNSelector.OnSelected_Listener() {
            public void onSelect(View v) {
                for(_ColorMapE c : _ColorMapE.values()) {
                    if(c.rid() == v.getId()) {
                        if(null != _board) { _board.color(c.color()); }
                        else {qns.Select(qns.pressed());}
                    }
                    _color_popup.dismiss();
                }
            }
        };        
    }

    private QNSelector.OnSelected_Listener _Bg_Build_color_selected_listener(final QNSelector qns) {
        return new QNSelector.OnSelected_Listener() {
            public void onSelect(View v) {
                for(_ColorMapE c : _ColorMapE.values()) {
                    if(c.rid() == v.getId()) {
                        if(null != _board) { 
              _board.color(c.color()); 
              if (null != _bgfile) {
                _bgfile = null;
                            _change_bg();
              } 
            }
                        else {qns.Select(qns.pressed());}
                    }
                    _color_popup.dismiss();
                }
            }
        };        
    }
*/
    // setup button's functionality
    private void setupUiListener(final Board board) {

		mBtnUndo.setOnClickListener(this);
		mBtnRedo.setOnClickListener(this);
		mBtnPen.setOnClickListener(this);
		mBtnEraser.setOnClickListener(this);

		mBtnSave.setOnClickListener(this);
		mBtnDiscard.setOnClickListener(this);

		mPenThickness1.setOnClickListener(this);
		mPenThickness2.setOnClickListener(this);
		mPenThickness3.setOnClickListener(this);

		mEraserThickness1.setOnClickListener(this);
		mEraserThickness2.setOnClickListener(this);
		mEraserThickness3.setOnClickListener(this);

		mPenBlack.setOnClickListener(this);
		mPenRed.setOnClickListener(this);
		mPenYellow.setOnClickListener(this);
		mPenGreen.setOnClickListener(this);
		mPenBlue.setOnClickListener(this);
		mPenGradient.setOnClickListener(this);

		mPenColorGradientPicker.setOnTouchListener(this);
		mPenMenu.setOnTouchListener(this);
		mEraserMenu.setOnTouchListener(this);
		mClosePenPanelButton.setOnClickListener(this);
		mCloseEraserPanelButton.setOnClickListener(this);

		/*
		 * // Behavior of tool selector QNSelector qns =
		 * (QNSelector)findViewById(R.id.tool_selector); qns.init();
		 * qns.register_onSelected(new QNSelector.OnSelected_Listener() { public
		 * void onSelect(View v) { switch(v.getId()) { case
		 * R.id.tool_selector_pen: { if(null != board) { board.tool(ToolE.PEN);
		 * } findViewById(R.id.pen_menu).setVisibility(View.VISIBLE);
		 * findViewById(R.id.eraser_menu).setVisibility(View.GONE);
		 * findViewById(R.id.background_menu).setVisibility(View.GONE);
		 * board.ispen = true; } break;
		 * 
		 * case R.id.tool_selector_eraser: { if(null != board) {
		 * board.tool(ToolE.ERASER); }
		 * findViewById(R.id.pen_menu).setVisibility(View.GONE);
		 * findViewById(R.id.eraser_menu).setVisibility(View.VISIBLE);
		 * findViewById(R.id.background_menu).setVisibility(View.GONE);
		 * board.ispen = false; } break;
		 * 
		 * case R.id.tool_selector_background: { if(null != board) {
		 * board.tool(ToolE.BACKGROUND); }
		 * findViewById(R.id.pen_menu).setVisibility(View.GONE);
		 * findViewById(R.id.eraser_menu).setVisibility(View.GONE);
		 * findViewById(R.id.background_menu).setVisibility(View.VISIBLE); }
		 * break;
		 * 
		 * default: QNDev.qnAssert(false); } } });
		 * 
		 * // Common behavior QNSelector.OnSelected_Listener thick_selected =
		 * new QNSelector.OnSelected_Listener() { public void onSelect(View v) {
		 * for(_ThickMapE t : _ThickMapE.values()) { if(t.rid() == v.getId()) {
		 * if(null != board) { board.thickness(t.thick()); } } } } };
		 * 
		 * // Behavior of Pen tool menu qns =
		 * (QNSelector)findViewById(R.id.pen_menu_thick);
		 * qns.register_onSelected(thick_selected);
		 * 
		 * qns = (QNSelector)findViewById(R.id.pen_menu_color);
		 * qns.register_onChanged(_Build_color_changed_listener(qns));
		 * qns.register_onSelected(_Build_color_selected_listener(qns));
		 * 
		 * qns = (QNSelector)findViewById(R.id.eraser_menu_thick);
		 * qns.register_onSelected(thick_selected);
		 * 
		 * ((ImageButton)findViewById(R.id.background_gallery_btn)).
		 * setOnClickListener(new View.OnClickListener() { public void
		 * onClick(View v) { Intent photo_picker_intent = new
		 * Intent("android.intent.action.PICK");
		 * photo_picker_intent.setType("image/*");
		 * startActivityForResult(photo_picker_intent,
		 * _ReqCodeE.IMAGE_PICK.code()); } });
		 * 
		 * qns = (QNSelector)findViewById(R.id.background_menu_color);
		 * qns.register_onChanged(_Build_color_changed_listener(qns));
		 * qns.register_onSelected(_Bg_Build_color_selected_listener(qns));
		 */
		/****** Behavior of bottom button ***********/
		/*
		 * // ((Button)
		 * findViewById(R.id.bottom_btn_save)).setOnClickListener(new
		 * View.OnClickListener() { mBtnSave.setOnClickListener(new
		 * View.OnClickListener() { public void onClick(View v) { if
		 * (!QNUtil.checkSDCard(QNSketcher.this)) { return; }
		 * 
		 * LayoutInflater factory = LayoutInflater.from(QNSketcher.this); final
		 * View saveView = factory.inflate(R.layout.sketcher_save_dialog, null);
		 * final AlertDialog d = new AlertDialog.Builder(QNSketcher.this)
		 * .setTitle(R.string.save) .setView(saveView) .create(); ((EditText)
		 * saveView.findViewById(R.id.edit)).setText(_default_sketch_name());
		 * ((Button)saveView.findViewById(R.id.btn_ok)).setOnClickListener( new
		 * View.OnClickListener() { public void onClick(View v) { if
		 * (!QNUtil.checkSDCard(QNSketcher.this)) { return; }
		 * 
		 * // final String fpath = _SAVE_DIRECTORY + ((EditText)
		 * d.findViewById(R.id.edit)).getText().toString(); final String fpath =
		 * QNConstants.SKETCH_DIRECTORY +((EditText)
		 * d.findViewById(R.id.edit)).getText().toString() +
		 * _sketch_format.File_extention(); String filename = ((EditText)
		 * d.findViewById(R.id.edit)).getText().toString(); if
		 * (filename.length() == 0) { new AlertDialog.Builder(QNSketcher.this)
		 * .setTitle(R.string.warning) .setMessage(R.string.empty_name)
		 * .setPositiveButton(R.string.ok, null) .show(); d.show(); } else if
		 * (checkIllegalChar(filename)) { new
		 * AlertDialog.Builder(QNSketcher.this) .setTitle(R.string.warning)
		 * .setMessage(R.string.illegal_name) .setPositiveButton(R.string.ok,
		 * null) .show(); d.show(); } else { if( (new File(fpath)).exists() ) {
		 * // Create "File name already exists" warning dialog!!!! new
		 * AlertDialog.Builder(QNSketcher.this) .setTitle(R.string.warning)
		 * .setMessage(R.string.file_exist) .setPositiveButton(R.string.ok, new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int which) { if
		 * (!QNUtil.checkSDCard(QNSketcher.this)) { return; } // Overwrite!!!
		 * _save_sketch(fpath); d.dismiss(); } })
		 * .setNegativeButton(R.string.cancel, new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int which) { ; // nothing to do!!! }
		 * }) .show(); } else { // Write _save_sketch(fpath); d.dismiss(); } } }
		 * });
		 * ((Button)saveView.findViewById(R.id.btn_cancel)).setOnClickListener(
		 * new View.OnClickListener() { public void onClick(View v) {
		 * d.dismiss(); } }); d.show(); } });
		 */

		/*
		 * TODO // ((Button)
		 * findViewById(R.id.bottom_btn_center)).setOnClickListener(new
		 * View.OnClickListener() { mBottom_btn_center.setOnClickListener(new
		 * View.OnClickListener() { public void onClick(View v) { if
		 * (!QNUtil.checkSDCard(QNSketcher.this)) { return; }
		 * 
		 * try { Bitmap bm = board.create_output(board.getWidth(),
		 * board.getHeight()); QNDev.qnAssert(null != bm); File fnote =
		 * _alloc_note_file(); QNDev.qnAssert(!fnote.exists());
		 * fnote.createNewFile(); QNUtil.save_image(fnote, bm,
		 * _sketch_format.Format());
		 * 
		 * Intent intent = new Intent(QNConstants.INTENT_ACTION_NEW);
		 * intent.setClass(QNSketcher.this, QNNoteView.class);
		 * intent.setDataAndType(Uri.parse("file://" + fnote.getAbsolutePath()),
		 * _sketch_format.Mime());
		 * QNDev.log("QNSketcher"+" create a new file named: "+fnote);
		 * if(_RunningModeE.SKETCH_NEW == _runmode) { // QN-> sketch->edit ,Send
		 * to note edit!! startActivityForResult(intent,
		 * QNConstants.REQUEST_CODE_QN_SKETCH_EDIT); return; } else if
		 * (_RunningModeE.SKETCH_APP == _runmode) { //it is not from widget, so
		 * just set widget2update = -1
		 * QNDev.log(TAG+"from sketch app-> QNNoteView, update the configure..."
		 * ); QNAppWidgetConfigure.saveWidget2Update(QNSketcher.this, -1); //
		 * Sketcher App->edit, Send to note edit!!Will return to Sketcher
		 * startActivityForResult(intent,
		 * QNConstants.REQUEST_CODE_SKETCHAPP_EDIT); return; } else
		 * if(_RunningModeE.SKETCH_EDIT == _runmode) { // return.
		 * setResult(RESULT_OK, intent); } else { QNDev.qnAssert(false); }
		 * QNSketcher.this.finish(); } catch (Exception e) {
		 * //QNDev.qnAssert(false); // unexpected! } } });
		 */
		/*
		 * if (_board._cmdi <= 0) { mBtnDiscard.setEnabled(false); } else {
		 * mBtnDiscard.setEnabled(true); }
		 */
	}

	private void _apply_preference(final Board board) {
		/*
		 * //Get the xml/preferences.xml preferences SharedPreferences prefs =
		 * PreferenceManager.getDefaultSharedPreferences(this); // Set pen
		 * preference. board.pen_color(
		 * _ColorMapE.valueOf(prefs.getString("pref_sketch_pen_color",
		 * "BLACK")).color() ); board.pen_thick(
		 * _ThickMapE.valueOf(prefs.getString("pref_sketch_pen_thick",
		 * "MEDIUM")).thick() ); board.tool(Board.ToolE.ERASER);
		 * 
		 * board.eraser_thick(
		 * _ThickMapE.valueOf(prefs.getString("pref_sketch_eraser_thick",
		 * "MEDIUM")).thick() ); board.bg_color(
		 * _ColorMapE.valueOf(prefs.getString("pref_sketch_bg_color",
		 * "WHITE")).color() );
		 */
		board.pen_color(Board.ColorE.BLACK);
		board.pen_thick(Board.ThicknessE.THICK2);

		board.tool(Board.ToolE.ERASER);
		board.eraser_thick(Board.ThicknessE.THICK2);

		board.bg_color(Board.ColorE.NULL_COLOR);
		// Default is 'PEN'
		board.tool(Board.ToolE.PEN);
	}

	/**
	 * Get sketcher temporal files. (actually....files in
	 * '_TEMP_FILE_DIRECTORY')
	 * 
	 * @return
	 */
	private File[] _temp_files() {
        if (!QNUtil.is_storage_mounted(this)) {
            return null;
        } else {
            File d = new File(QNConstants.SKETCH_TEMP_FILE_DIRECTORY);
            //QNDev.qnAssert(null != d && d.exists() && d.canWrite() && d.isDirectory());
            //QNDev.qnAssert(null != d.listFiles());
            return d.listFiles();
        }
    }
	/**
	 * Check that given file is in 'sketcher temp directory' or not.
	 * 
	 * @param fpath
	 *            : ABSOLUTE file path!
	 * @return
	 */
	private boolean _is_temp_file(String fpath) {
		QNDev.qnAssert(QNUtil.is_absolute_path(fpath));
		File[] files = _temp_files();
		if (null != files) {
			for (File f : files) {
				if (fpath.equals(f.getAbsolutePath())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Delete bgfile if '_board' is using temp bg file.
	 * 
	 * @return : true (if file is deleted)
	 */
	private boolean _delete_board_temp_bgfile(Board board) {
        if (!QNUtil.is_storage_mounted(this)) {
            return false;
        } else {
            if(null != board && null != board.bgfile() && _is_temp_file(board.bgfile())) {
                new File(board.bgfile()).delete();
                return true;
            }
        }
		return false;
	}

	/**
	 * Clean sketch temp directory. Policy : all files that are created before
	 * over 1 day, are deleted!
	 */
	private void _clean_temp_directory() {
		long sysmills = System.currentTimeMillis();
		File[] temp_files = _temp_files();
		if (temp_files == null) {
			return;
		}
		for (File f : temp_files) {
			QNDev.qnAssert(sysmills > f.lastModified());
			if ((sysmills - f.lastModified()) > _TEMP_FILE_LIFE_MILLS
					&& f.canWrite()) {
				f.delete();
			}
		}
	}

	private void _destroy_board(Board board) {
		if (null != board) {
			_delete_board_temp_bgfile(board);
			board.destroy();
		}
	}

	/**
	 * Allocate file for save note.
	 * 
	 * @return
	 */
	private File _alloc_note_file() {
		if (!_check_sdcard())
			return null;
		long mills = System.currentTimeMillis();
		final String prefix = QNConstants.SKETCH_DIRECTORY + "sk";
        String fileName = QNUtil.createNewFileName(prefix, _sketch_format.File_extention()); 
		File f = new File(fileName);
		return f;
	}

    /**
     * Generate default file name when save sketch.
     * @return
     */
/*
    private String _default_sketch_name() {
        if (!_check_sdcard())
            return null;
        File d = new File(QNConstants.SKETCH_DIRECTORY);
        if (d == null) { return null;}
        
        final int max_count = 9999;
        File[] flist = d.listFiles();

        for(int i=0; i<max_count; i++) {
            boolean bfound = false;
            String fname = _DEFAULT_SKETCH_NAME_PREFIX + i;
            if (null == flist)
            {
                return fname;
            }
            for(File f : flist) { 
       //         if(f.getName().equals(fname)) { bfound = true; break; }
                if(f.getName().equals(fname + _sketch_format.File_extention())) { bfound = true; break; }
            }
            if(!bfound) { return fname; } 
        }
        QNDev.qnAssert(false);
        return null;
    }
*/
/*    
    private boolean _save_sketch(String outpath) {
        if (!_check_sdcard()) {
            return false;
        }
        Bitmap bm = _board.create_output(_board.getWidth(), _board.getHeight());
        if (null == bm) { return false;}
        QNDev.qnAssert(null != bm);
        File file = new File(outpath);
        QNUtil.save_image(file, bm, _sketch_format.Format());
        QNUtil.register_to_MediaStore(this, file, _sketch_format.Mime(), new File(QNConstants.SKETCH_TEMP_FILE_DIRECTORY));

        return true;
    }
*/
    private Board _Prepare_board(String bgfile, int width, int height) {
        // newly create_board
        //for new product, we can use below log to get width/height for boardParameters 
        QNDev.logd(TAG, "prepare_board: sketch width=" + width + " height=" + height + " bgFile=" + bgfile);
        Board board = new Board(this);
        if (ori && (null == bgfile)){
            ori = false;
            if (!initori && QNUtil.is_landscape(QNSketcher.this)) {
                //first time is portrait, now it is landscape
                board.setup(bgfile, boardParameters.width_port*boardParameters.height_land/boardParameters.height_port, boardParameters.height_land);
            } else if (initori && !QNUtil.is_landscape(QNSketcher.this)) {
                //first time is landscape, now it is portrait
                board.setup(bgfile, boardParameters.width_port, boardParameters.height_land*boardParameters.width_port/boardParameters.width_land);
            } else {
                //first time is landscape, now it is landscape again
                // or first time is portrait, now is it portrait again
                board.setup(bgfile, width, height);
            }
        } else {
            //the first time, so just use the default values
            board.setup(bgfile, width, height);
        }
        board.setLayoutParams(new ViewGroup.LayoutParams(board.board_width(), board.board_height()));
        return board;
        
    }

    private void _change_bg() {
		try {
			ViewGroup vgroup = (ViewGroup) findViewById(R.id.board_container);
			vgroup.removeAllViews();
			Bundle bundle = null;
			int width = 0;
			int height = 0;
			if (null != _board) {
				width = _board.board_width();
				height = _board.board_height();
				bundle = new Bundle();
				_board.store(bundle);
				_destroy_board(_board);
			}

            _board = new Board(this);
            if (initori && QNUtil.is_landscape(QNSketcher.this) || !initori && !QNUtil.is_landscape(QNSketcher.this)) {
                _board.setup(_bgfile,vgroup.getWidth(), vgroup.getHeight());
            }
            else {
                _board.setup(_bgfile,width, height);
            }
            _board.setLayoutParams(new ViewGroup.LayoutParams(_board.board_width(), _board.board_height()));

            if(null != bundle) { 
                _board.restore(bundle); 
            } else { _apply_preference(_board); }
            
            _setup_view(_board);
        } catch (Exception e) {
            QNDev.log(TAG+ "_change_bg catch error: "+e);
		}
	}

	private void _setup_viewbase() {
		setContentView(R.layout.sketcher);

		mBtnSave = (ImageButton) findViewById(R.id.sketch_save);
		mBtnDiscard = (ImageButton) findViewById(R.id.sketch_discard);

		mBtnUndo = (ImageButton) findViewById(R.id.sketch_undo);
		mBtnRedo = (ImageButton) findViewById(R.id.sketch_redo);
		mBtnPen = (ImageButton) findViewById(R.id.sketch_pen);
		mPenPreview = (View) findViewById(R.id.pen_preview);
		mBtnEraser = (ImageButton) findViewById(R.id.sketch_eraser);
		mEraserPreview = (View) findViewById(R.id.eraser_preview);

		mPenMenu = (LinearLayout) findViewById(R.id.pen_menu);
		mPenColorGradientBar = (LinearLayout) findViewById(R.id.pen_color_gradient_bar);
		mEraserMenu = (LinearLayout) findViewById(R.id.eraser_menu);
		mClosePenPanelButton = (ImageButton) findViewById(R.id.close_pen_panel_button);
		mCloseEraserPanelButton = (ImageButton) findViewById(R.id.close_eraser_panel_button);

		mPenBlack = (ImageButton) findViewById(R.id.pen_color_black);
		mPenRed = (ImageButton) findViewById(R.id.pen_color_red);
		mPenYellow = (ImageButton) findViewById(R.id.pen_color_yellow);
		mPenGreen = (ImageButton) findViewById(R.id.pen_color_green);
		mPenBlue = (ImageButton) findViewById(R.id.pen_color_blue);
		mPenGradient = (ImageButton) findViewById(R.id.pen_color_gradient);

		mPenThickness1 = (ImageButton) findViewById(R.id.pen_thick_1);
		mPenThickness2 = (ImageButton) findViewById(R.id.pen_thick_2);
		mPenThickness3 = (ImageButton) findViewById(R.id.pen_thick_3);

		mEraserThickness1 = (ImageButton) findViewById(R.id.eraser_thick_1);
		mEraserThickness2 = (ImageButton) findViewById(R.id.eraser_thick_2);
		mEraserThickness3 = (ImageButton) findViewById(R.id.eraser_thick_3);

		/*
		 * //mActionBar = (RelativeLayout)findViewById(R.id.action_bar);
		 * mBtnSave = new Button(QNSketcher.this);
		 * mBtnSave.setText(R.string.save); mBtnSave.setVisibility(View.GONE);
		 * mActionBar.addButton(mBtnSave);
		 * 
		 * mBottom_btn_center = new Button(QNSketcher.this);
		 * mBottom_btn_center.setText(R.string.create_note);
		 * mActionBar.addButton(mBottom_btn_center);
		 * 
		 * mBtnDiscard = new Button(QNSketcher.this);
		 * mBtnDiscard.setText(R.string.discard);
		 * mActionBar.addButton(mBtnDiscard);
		 * 
		 * _color_popup = new _ColorPopup(this, findViewById(R.id.main));
		 * 
		 * if(_RunningModeE.SKETCH_EDIT == _runmode){
		 * mBottom_btn_center.setText(R.string.update); } else
		 * if(_RunningModeE.SKETCH_APP == _runmode){ // ((ViewGroup)
		 * findViewById(R.id.layout_save)).setVisibility(View.VISIBLE);
		 * mBtnSave.setVisibility(View.VISIBLE); } else
		 * if(_RunningModeE.SKETCH_NEW == _runmode){ // ((ViewGroup)
		 * findViewById(R.id.layout_save)).setVisibility(View.GONE);
		 * mBtnSave.setVisibility(View.GONE); }
		 */
	}

	private void _setup_view(Board board) {
		mPenColorGradientPreview = new ColorPreviewView(this);
        mPenColorGradientBar.addView(mPenColorGradientPreview, 0, new ViewGroup.LayoutParams(55, ViewGroup.LayoutParams.FILL_PARENT));

        mPenColorGradientPicker = new ColorGradientPicker(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
		layoutParams.setMargins(10, 0, 0, 0);
		mPenColorGradientBar.addView(mPenColorGradientPicker, 1, layoutParams);

		if (null != board) {
			ViewGroup vgroup = ((ViewGroup) findViewById(R.id.board_container));
			vgroup.removeAllViews();
			vgroup.addView(board);

			setupUiListener(board);

			updateButton();
			/*
			 * TODO // Set Tool menu QNSelector qns =
			 * (QNSelector)findViewById(R.id.tool_selector); Board.ToolE tl =
			 * board.tool(); for(_ToolMapE tm : _ToolMapE.values()) {
			 * if(tm.tool() == tl) { qns.Select(qns.findViewById(tm.rid()));
			 * break; } }
			 * 
			 * // Set Pen qns = (QNSelector)findViewById(R.id.pen_menu_color);
			 * qns.init(); Board.ColorE c = board.pen_color(); for(_ColorMapE cm
			 * :_ColorMapE.values()) { if( cm.color() == c ) {
			 * qns.Touch(qns.findViewById(cm.rid())); break; } } qns =
			 * (QNSelector)findViewById(R.id.pen_menu_thick); qns.init();
			 * Board.ThicknessE t = board.pen_thick(); for(_ThickMapE tm
			 * :_ThickMapE.values()) { if( tm.thick() == t ) {
			 * qns.Touch(qns.findViewById(tm.rid())); break; } }
			 * 
			 * // Set Eraser qns =
			 * (QNSelector)findViewById(R.id.eraser_menu_thick); qns.init(); t =
			 * board.eraser_thick(); for(_ThickMapE tm :_ThickMapE.values()) {
			 * if( tm.thick() == t ) { qns.Touch(qns.findViewById(tm.rid()));
			 * break; } }
			 * 
			 * // Set Background qns =
			 * (QNSelector)findViewById(R.id.background_menu_color); qns.init();
			 * c = board.bg_color(); for(_ColorMapE cm :_ColorMapE.values()) {
			 * if( cm.color() == c ) { qns.Touch(qns.findViewById(cm.rid()));
			 * break; } }
			 */
		}
	}

	private void discardEdit() {
		if (0 == _board._histories.size()) {
			finish();
			return;
		}
		// show confirm dialog...
		try {
            AlertDialog.Builder discardDialog = new AlertDialog.Builder(QNSketcher.this);
            if(_RunningModeE.SKETCH_EDIT == _runmode){
                QNDev.log(TAG+"it is in sketcher edit mode");
                discardDialog.setIconAttribute(android.R.attr.alertDialogIcon)
                  .setTitle(R.string.discard)
                  .setMessage(R.string.show_discard_dialog_message);
            } else {
                QNDev.log(TAG+"it is in sketcher other mode");
                discardDialog.setIconAttribute(android.R.attr.alertDialogIcon)
                  .setTitle(R.string.discard_exit)
                  .setMessage(R.string.show_discard_dialog_message_exit);
            }

            discardDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //after discard the edit, revert the static values to initial values
         
                    //not clear the mDBReminder for the bug: discard the eidt and enter the edit from the detail again, it won't call oncreate()
                    //mDBReminder = 0L;
               
                    setResult(RESULT_CANCELED);
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ; // nothing to do!!!
                }
            })
            .show();
        } catch (Exception e) {
            //catch exception, do nothing
            QNDev.log(TAG + "discardEdit() catch exception = "+e);
            return;
        }
        
    }

	/**************************
	 * Overriding.
	 **************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		QNDev.logd(TAG, "onCreate()");

		_check_sdcard();

        QNUtil.initDirs(this);

        // get starting information from intent
        Intent intent = getIntent();
        if (intent == null) { return; }


        String action = intent.getAction();
        if (action == null) { return; }
		if (action.equals(QNConstants.INTENT_ACTION_NEW)) {
			_runmode = _RunningModeE.SKETCH_NEW;
		} else if (action.equals(QNConstants.INTENT_ACTION_EDIT)) {
			_runmode = _RunningModeE.SKETCH_EDIT;
		} /*
		 * else if(action.equals("android.intent.action.MAIN")) { _runmode =
		 * _RunningModeE.SKETCH_APP; } else { QNDev.qnAssert(false); }
		 */

        if(null != intent.getData()) {
            Uri uri = intent.getData();
            if( uri.getScheme().equals("file") ) {
                _bgfile = uri.getEncodedPath();
                if (0 == _bgfile.length()) { _bgfile = null; }
            }
        }

        initori = QNUtil.is_landscape(QNSketcher.this);

        //initialize the board parameters
        boardParameters.width_land = getResources().getInteger(R.integer.board_width_land);
        boardParameters.height_land = getResources().getInteger(R.integer.board_height_land);
        boardParameters.width_port = getResources().getInteger(R.integer.board_width_port);
        boardParameters.height_port = getResources().getInteger(R.integer.board_height_port);

		QNDev.logd(TAG, "board_height_port=" + boardParameters.height_port);

		_viewroot(R.id.main);
		_setup_viewbase();

		mPreviewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPreviewPaint.setColor(DEFAULT_CUSTOME_COLOR);
		mPreviewPaint.setStyle(Paint.Style.FILL);
	}

	@Override
	public void onResume() {
		QNDev.logd(TAG, "onResume()");
		super.onResume();
		// update the board, in case the bg file is deleted outside
		if ((null != _bgfile) && (!(new File(_bgfile)).exists())) {
			QNDev.log(TAG + "need update the bg");
			// bgfile is not null, but the real file is not existed
			_bgfile = null;
			_change_bg();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		QNDev.logd(TAG, "onStart()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		QNDev.logd(TAG, "onStop()");

    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(QNUtil.is_storage_mounted(this)) {
            _clean_temp_directory();
        }
        _destroy_board(_board);
    }
    
    @Override
    protected void _onContent_layouted() {
        QNDev.logd(TAG, "_onContent_layouted()");
        super._onContent_layouted();
        ViewGroup vgroup = (ViewGroup)findViewById(R.id.board_container);
        vgroup.removeAllViews();
        
        Bundle bundle = null;
        if(null != _board) {
            bundle = new Bundle();
            _board.store(bundle);
            _destroy_board(_board);
        }
        
        _board = _Prepare_board(_bgfile, vgroup.getWidth(), vgroup.getHeight());
        if(null != bundle) { 
            _board.restore(bundle); 
        } else {
            _apply_preference(_board);
        }
        
        _setup_view(_board);
        
    }      
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ori = true;
        _setup_viewbase();
    }    
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.sketcher_optmenu, menu);
      return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.MENU_OPEN).setEnabled(true);
      
    if (null != _board)
    {
      if (0 == _board._histories.size()) {
        menu.findItem(R.id.MENU_UNDO).setEnabled(false);
        menu.findItem(R.id.MENU_REDO).setEnabled(false);
      } else if (_board._cmdi > 0 && _board._cmdi < _board._histories.size()) {
        menu.findItem(R.id.MENU_UNDO).setEnabled(true);
        menu.findItem(R.id.MENU_REDO).setEnabled(true);
      } else if (_board._cmdi == 0 && _board._histories.size() > 0) {
        menu.findItem(R.id.MENU_UNDO).setEnabled(false);
        menu.findItem(R.id.MENU_REDO).setEnabled(true);
      } else if (_board._cmdi > 0 && _board._cmdi == _board._histories.size()) {
        menu.findItem(R.id.MENU_UNDO).setEnabled(true);
        menu.findItem(R.id.MENU_REDO).setEnabled(false);
      }
    }
        return true;
    }
*/

	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) {
	 * super.onOptionsItemSelected(item); boolean ret = true;
	 * if(item.getItemId() == R.id.MENU_OPEN) { String bucket_id =
	 * QNUtil.mediaStore_directory_bucket(this, QNConstants.SKETCH_DIRECTORY);
	 * if(null == bucket_id || null == picture_id) { // there is no sketch
	 * saved! - popup warning. new AlertDialog.Builder(QNSketcher.this)
	 * .setTitle(R.string.warning) .setMessage(R.string.no_sketch_saved)
	 * .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
	 * public void onClick(DialogInterface dialog, int which) { ; // nothing to
	 * do!!! } }) .show(); // } else { // we found! // Following code are
	 * tightly coupled with ImageGallery!! Intent intent = new
	 * Intent("android.intent.action.PICK"); intent.setType("image/*");
	 * //intent.setClassName("com.motorola.gallery",
	 * "com.motorola.gallery.ImageGallery"); intent.putExtra(_GALLERY_VIEW_TYPE,
	 * _GALLERY_IMAGES_FOLDER_TYPE); intent.putExtra(_GALLERY_ALBUM_TITLE,
	 * "My Sketch"); intent.putExtra(_GALLERY_BUCKET_ID, bucket_id);
	 * intent.putExtra(_GALLERY_VIEW_INCLUSION, _GALLERY_INCLUDE_IMAGES);
	 * intent.putExtra(_GALLERY_VIEW_MODE, _GALLERY_MODE_SINPIC);
	 * startActivityForResult(intent, _ReqCodeE.IMAGE_PICK.code()); // } } else
	 * { ret = false; }
	 * 
	 * return ret; }
	 */
	/*
	 * @Override protected void onActivityResult(int requestCode, int
	 * resultCode, Intent intent) { super.onActivityResult(requestCode,
	 * resultCode, intent);
	 * 
	 * QNDev.log(TAG+"onActivityResult: requestCode = "+requestCode+" resultCode = "
	 * +resultCode);
	 * 
	 * if (resultCode == RESULT_OK) { if(_ReqCodeE.IMAGE_PICK.code() ==
	 * requestCode) { Uri picUri = intent.getData(); if (picUri != null) { //
	 * get file path from picked uri (from media store) Cursor cur =
	 * managedQuery(picUri, null, null, null, null); QNDev.qnAssert(null !=
	 * cur); if (null == cur) {return;}
	 * 
	 * if( cur.moveToFirst() ) { // reset view _bgfile =
	 * cur.getString(cur.getColumnIndex("_data")); _setup_viewbase(); } else {
	 * QNDev.qnAssert(false); // unexpected. } cur.close(); } else {
	 * QNDev.qnAssert(false); // it's not expected } } else
	 * if(QNConstants.REQUEST_CODE_SKETCHAPP_EDIT == requestCode) {
	 * QNDev.logd("QNSketcher", "Return from QNNoteView");//DO nothing return
	 * from QN Edit } else if (requestCode ==
	 * QNConstants.REQUEST_CODE_QN_SKETCH_EDIT) {
	 * QNDev.log(TAG+"return from edit"); String uri_string =
	 * intent.getStringExtra("newNoteUri"); Intent resultValue = new Intent();
	 * resultValue.putExtra("newNoteUri", uri_string); setResult(RESULT_OK,
	 * resultValue); QNDev.log(TAG+"QNSketcher return newNoteUri = "+uri_string+
	 * " to QNNewActivity"); finish(); } else { // unexpected !!
	 * QNDev.qnAssert(false); } } else { //for IKSF-1673, need to update current
	 * board, incase users enter gallery and delete current background and
	 * didn't select new bg
	 * QNDev.log(TAG+"check the background file after return from gallery"); if
	 * (null != _bgfile) { QNDev.log(TAG+"_bgfile is not null, = "+_bgfile); if(
	 * !(new File(_bgfile)).exists() ) { QNDev.log(TAG+
	 * "but _bgfile in galley is not exist, so need to reload the board");
	 * _bgfile = null; _setup_viewbase(); } } } }
	 */
	/*
	 * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { switch
	 * (keyCode) { case KeyEvent.KEYCODE_BACK: _discard_edit(); return true; }
	 * 
	 * return super.onKeyDown(keyCode, event); }
	 */
	@Override
	public void onClick(View v) {
		/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */
		boolean boardTouching = false;
		if(null != _board) {
			boardTouching = _board.isTouching();
		}
		if(boardTouching) {
			return;
		}
		/*2012-10-16, add end*/
		switch (v.getId()) {
		case R.id.sketch_undo:
			hidePanel();
                if(null != _board) { _board.undo(); } 
                break;
            case R.id.sketch_redo:
                hidePanel();
                if(null != _board) { _board.redo(); } 
			break;
		case R.id.sketch_pen:
			if (_board.ispen) {
				// already in pen state, toggle the pallet
				if (mPenMenu.getVisibility() == View.VISIBLE) {
					mPenMenu.setVisibility(View.GONE);
				} else {
					mPenMenu.setVisibility(View.VISIBLE);
				}
			} else {
				// switch from eraser to pen state
				_board.tool(ToolE.PEN);
				_board.ispen = true;
				mBtnPen.setImageResource(R.drawable.sketch_pen_highlight);
                    mBtnEraser.setImageResource(R.drawable.sketch_eraser_not_highlight);
				mPenPreview.setVisibility(View.VISIBLE);
				mEraserPreview.setVisibility(View.GONE);
				if (mEraserMenu.getVisibility() == View.VISIBLE) {
					mEraserMenu.setVisibility(View.GONE);
				}
			}
			break;
		case R.id.sketch_eraser:
			if (_board.ispen) {
				// switch from pen to eraser state
				_board.tool(ToolE.ERASER);
				_board.ispen = false;
				mBtnPen.setImageResource(R.drawable.sketch_pen_not_highlight);
				mBtnEraser.setImageResource(R.drawable.sketch_eraser_highlight);
				mPenPreview.setVisibility(View.GONE);
				mEraserPreview.setVisibility(View.VISIBLE);
				if (mPenMenu.getVisibility() == View.VISIBLE) {
					mPenMenu.setVisibility(View.GONE);
				}
			} else {
				// already in eraser state, toggle the pallet
				if (mEraserMenu.getVisibility() == View.VISIBLE) {
					mEraserMenu.setVisibility(View.GONE);
				} else {
					mEraserMenu.setVisibility(View.VISIBLE);
				}
			}
			break;
		case R.id.close_pen_panel_button:
			if (mPenMenu.getVisibility() == View.VISIBLE) {
				mPenMenu.setVisibility(View.GONE);
			}
			break;
		case R.id.close_eraser_panel_button:
			if (mEraserMenu.getVisibility() == View.VISIBLE) {
				mEraserMenu.setVisibility(View.GONE);
			}
			break;
		case R.id.pen_color_black:
		case R.id.pen_color_red:
		case R.id.pen_color_yellow:
		case R.id.pen_color_green:
		case R.id.pen_color_blue:
			_board.pen_color(mapPenColor(v.getId()));
			updatePalletPenColor();
			if (mPenColorGradientBar.getVisibility() == View.VISIBLE) {
				mPenColorGradientBar.setVisibility(View.GONE);
				mPenMenu.setBackgroundResource(R.drawable.bg_sketch_customize1);
			}
			break;
		case R.id.pen_color_gradient:
                _board.pen_color(Board.ColorE.GRADIENT, mPreviewPaint.getColor());
			updatePalletPenColor();
			if (mPenColorGradientBar.getVisibility() == View.GONE) {
				mPenColorGradientBar.setVisibility(View.VISIBLE);
				mPenMenu.setBackgroundResource(R.drawable.bg_sketch_customize2);
			}
			break;
		case R.id.pen_thick_1:
		case R.id.pen_thick_2:
		case R.id.pen_thick_3:
			_board.pen_thick(mapPenThick(v.getId()));
			updatePalletPenThickness();
			break;
		case R.id.eraser_thick_1:
		case R.id.eraser_thick_2:
		case R.id.eraser_thick_3:
			_board.eraser_thick(mapEraserThick(v.getId()));
			updatePalletEraserThickness();
			break;
		case R.id.sketch_save:
			saveAndBack(false);
			break;
		case R.id.sketch_discard:
			discardEdit();
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		QNDev.logd(TAG, "onTouch() view=" + v + " event=" + event);
		if (v == mPenColorGradientPicker) {
			QNDev.logd(TAG, "onTouch() touched R.id.pen_color_gradient_picker");
			Rect rect = new Rect();
			v.getDrawingRect(rect);

			float currentX = event.getX();
			if (currentX < rect.left) {
				currentX = rect.left;
			}
			if (currentX > rect.right) {
				currentX = rect.right;
			}
			float currentY = rect.top + (rect.bottom - rect.top) / 2; // vertically
            float unit = (currentX - rect.left) / (rect.width());
            mCurrentPoint.set((int)currentX, (int)currentY);
            QNDev.logd(TAG, "onTouch() rect.left=" + rect.left + " rect.width=" + rect.width() + " unit=" + unit);
			int selectedColor = interpColor(unit);
			_board.pen_color(Board.ColorE.GRADIENT, selectedColor);
			mPreviewPaint.setColor(selectedColor);
			QNDev.logd(TAG, "onTouch() interpColor=" + selectedColor);

			/*
			 * switch (event.getAction()) { case MotionEvent.ACTION_DOWN:
			 * mTrackingColor = true; break; case MotionEvent.ACTION_UP:
			 * mTrackingColor = false; break; }
			 */
			mPenColorGradientPreview.invalidate();
            mPenColorGradientPicker.invalidate((int)(currentX - rect.left - 2 * CROSS_BAR_RADIUS),
                                               (int)(currentY - rect.top - 2 * CROSS_BAR_RADIUS),
                                               (int)(currentX - rect.left + 4 * CROSS_BAR_RADIUS),
                                               (int)(currentY - rect.top + 2 * CROSS_BAR_RADIUS));
            return true;
		} else if (v == mPenMenu || v == mEraserMenu) {
			return true;
		}

		return false;
	}

	@Override
	public void onBackPressed() {
		if (mPenMenu.getVisibility() == View.VISIBLE) {
			mPenMenu.setVisibility(View.GONE);
		} else {
			if (_board._histories.size() > 0) {
				saveAndBack(true);
		
               // Toast.makeText(this, R.string.sketch_saved, Toast.LENGTH_SHORT).show();
			} else {
				QNSketcher.this.finish();
			}
		}
	}

	public void hidePanel() {
		if (mPenMenu.getVisibility() == View.VISIBLE) {
			mPenMenu.setVisibility(View.GONE);
		}
		if (mEraserMenu.getVisibility() == View.VISIBLE) {
			mEraserMenu.setVisibility(View.GONE);
		}
	}

	public void updateButton() {
        if (null != _board && _board._histories != null)
        {
            if (0 == _board._histories.size()) {
                mBtnUndo.setEnabled(false);
                mBtnRedo.setEnabled(false);
            } else if (_board._cmdi > 0 && _board._cmdi < _board._histories.size()) {
                mBtnUndo.setEnabled(true);
                mBtnRedo.setEnabled(true);
            } else if (_board._cmdi == 0 && _board._histories.size() > 0) {
                mBtnUndo.setEnabled(false);
                mBtnRedo.setEnabled(true);
            } else if (_board._cmdi > 0 && _board._cmdi == _board._histories.size()) {
                mBtnUndo.setEnabled(true);
                mBtnRedo.setEnabled(false);
            }
        }
        
        updatePalletEraserThickness();
        updatePalletPenThickness();
        updatePalletPenColor();
        if (_board.ispen) {
            mBtnPen.setImageResource(R.drawable.sketch_pen_highlight);
            mBtnEraser.setImageResource(R.drawable.sketch_eraser_not_highlight);
        } else {
            mBtnPen.setImageResource(R.drawable.sketch_pen_not_highlight);
            mBtnEraser.setImageResource(R.drawable.sketch_eraser_highlight);
        }
    }

	private void updatePalletPenColor() {
		mPenBlack.setBackgroundResource(R.color.transparent);
		mPenRed.setBackgroundResource(R.color.transparent);
		mPenYellow.setBackgroundResource(R.color.transparent);
		mPenGreen.setBackgroundResource(R.color.transparent);
		mPenBlue.setBackgroundResource(R.color.transparent);
		mPenGradient.setBackgroundResource(R.color.transparent);
		Board.ColorE selectedColor = _board.pen_color();
		if (selectedColor != Board.ColorE.GRADIENT) {
			mPenPreview.setBackgroundColor(_board.map_color(selectedColor));
		} else {
			mPenPreview.setBackgroundColor(mPreviewPaint.getColor());
		}
		switch (selectedColor) {
		case BLACK:
			mPenBlack.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		case RED:
			mPenRed.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		case YELLOW:
			mPenYellow.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		case GREEN:
			mPenGreen.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		case BLUE:
			mPenBlue.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		case GRADIENT:
			mPenGradient.setBackgroundResource(R.drawable.ic_selected_color);
			break;
		}
	}

	private void updatePalletPenThickness() {
		mPenThickness1.setBackgroundResource(R.color.transparent);
		mPenThickness2.setBackgroundResource(R.color.transparent);
		mPenThickness3.setBackgroundResource(R.color.transparent);
		ViewGroup.LayoutParams layout = mPenPreview.getLayoutParams();
		switch (_board.pen_thick()) {
		case THICK1:
			mPenThickness1.setBackgroundResource(R.drawable.ic_selected_size);
			layout.height = 3;
			break;
		case THICK2:
			mPenThickness2.setBackgroundResource(R.drawable.ic_selected_size);
			layout.height = 6;
			break;
		case THICK3:
			mPenThickness3.setBackgroundResource(R.drawable.ic_selected_size);
			layout.height = 8;
			break;
		}
		mPenPreview.setLayoutParams(layout);
	}

	private void updatePalletEraserThickness() {
		mEraserThickness1.setBackgroundResource(R.color.transparent);
		mEraserThickness2.setBackgroundResource(R.color.transparent);
		mEraserThickness3.setBackgroundResource(R.color.transparent);
		ViewGroup.LayoutParams layout = mEraserPreview.getLayoutParams();
		switch (_board.eraser_thick()) {
		case THICK1:
                mEraserThickness1.setBackgroundResource(R.drawable.ic_selected_size);
                layout.height = 3;
                break;
            case THICK2:
                mEraserThickness2.setBackgroundResource(R.drawable.ic_selected_size);
                layout.height = 6;
                break;
            case THICK3:
                mEraserThickness3.setBackgroundResource(R.drawable.ic_selected_size);
                layout.height = 8;
			break;
		}
		mEraserPreview.setLayoutParams(layout);
	}

	private void saveAndBack(boolean isTure) {
        if (!QNUtil.checkStorageCard(QNSketcher.this))  { return; }
        Bitmap bm = null;
        try {
            bm = _board.create_output(_board.getWidth(), _board.getHeight());
            File fnote = _alloc_note_file();
            fnote.createNewFile();
            QNUtil.save_image(fnote, bm, _sketch_format.Format());
            
            Intent intent = new Intent(QNConstants.INTENT_ACTION_NEW);
            intent.setClass(QNSketcher.this, QNNoteView.class);
            intent.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setDataAndType(Uri.parse("file://" + fnote.getAbsolutePath()), _sketch_format.Mime());
            QNDev.log("QNSketcher"+" create a new file named: "+fnote);
            if(_RunningModeE.SKETCH_NEW == _runmode) {
                // QN-> sketch->edit ,Send to note edit!!
                startActivity(intent);
                //return;
            } else /*if (_RunningModeE.SKETCH_APP == _runmode) {
                //it is not from widget, so just set widget2update = -1
                QNDev.log(TAG+"from sketch app-> QNNoteView, update the configure...");
                QNAppWidgetConfigure.saveWidget2Update(QNSketcher.this, -1);
                // Sketcher App->edit, Send to note edit!!Will return to Sketcher
                startActivityForResult(intent, QNConstants.REQUEST_CODE_SKETCHAPP_EDIT);
                return;
            }  else*/ if(_RunningModeE.SKETCH_EDIT == _runmode) {
                // return to QNNoteView
                setResult(RESULT_OK, intent);
            } else { QNDev.qnAssert(false); }

            QNSketcher.this.finish();
        } catch (Exception e) {
            //QNDev.qnAssert(false); // unexpected!
        } finally {
            if (bm != null) {
                bm.recycle();
                bm = null;
            }
        }
        if(isTure){
        Toast.makeText(this, R.string.sketch_saved, Toast.LENGTH_SHORT).show();
        }
    }

	private Board.ColorE mapPenColor(int viewId) {
		switch (viewId) {
		case R.id.pen_color_black:
			return Board.ColorE.BLACK;
		case R.id.pen_color_red:
			return Board.ColorE.RED;
		case R.id.pen_color_yellow:
			return Board.ColorE.YELLOW;
		case R.id.pen_color_green:
			return Board.ColorE.GREEN;
		case R.id.pen_color_blue:
			return Board.ColorE.BLUE;
		case R.id.pen_color_gradient:
			return Board.ColorE.GRADIENT;
		default:
			return Board.ColorE.BLACK;
		}
	}

	private Board.ThicknessE mapPenThick(int viewId) {
		switch (viewId) {
		case R.id.pen_thick_1:
			return Board.ThicknessE.THICK1;
		case R.id.pen_thick_2:
			return Board.ThicknessE.THICK2;
		case R.id.pen_thick_3:
			return Board.ThicknessE.THICK3;
		default:
			return Board.ThicknessE.THICK2;
		}
	}

	private Board.ThicknessE mapEraserThick(int viewId) {
		switch (viewId) {
		case R.id.eraser_thick_1:
			return Board.ThicknessE.THICK1;
		case R.id.eraser_thick_2:
			return Board.ThicknessE.THICK2;
		case R.id.eraser_thick_3:
			return Board.ThicknessE.THICK3;
		default:
			return Board.ThicknessE.THICK2;
		}
	}

	/**************************
	 * APIs.
	 **************************/
	private int ave(int s, int d, float p) {
		return s + java.lang.Math.round(p * (d - s));
	}

	private int interpColor(float unit) {
		if (unit <= 0) {
			return COLOR_BASE[0];
		}
		if (unit >= 1) {
			return COLOR_BASE[COLOR_BASE.length - 1];
		}

		float p = unit * (COLOR_BASE.length - 1);
		int i = (int) p;
		p -= i;

		// now p is just the fractional part [0...1) and i is the index
		int c0 = COLOR_BASE[i];
		int c1 = COLOR_BASE[i + 1];
		int a = ave(Color.alpha(c0), Color.alpha(c1), p);
		int r = ave(Color.red(c0), Color.red(c1), p);
		int g = ave(Color.green(c0), Color.green(c1), p);
		int b = ave(Color.blue(c0), Color.blue(c1), p);

		return Color.argb(a, r, g, b);
	}

	/**
	 * Allocate temporal file for background image. Client of sketcher can use
	 * this by following sequence. - get allocated file. - write something on
	 * it. - put this file's absolute path in intent. Client that doesn't use
	 * this interface as a way of allocating temporal file, it is fully in
	 * charge of handling file - removing after file is used.
	 * 
	 * @return : null (false)
	 */
	static File alloc_temp_bgimage_file() {
		try {
			File d = new File(QNConstants.SKETCH_TEMP_FILE_DIRECTORY);
            QNDev.qnAssert(null != d && d.exists() && d.canWrite() && d.isDirectory());
            File f = File.createTempFile("___qn_", "___", d);
            QNDev.qnAssert(null != f);
            f.deleteOnExit();
            return f;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkIllegalChar (String str) {
        final char[] ch = {':', '\'', '?', '/', '>', '<', '|', '\\', '*', '"', '\''};
        for (int i =0; i<ch.length; i++)
        {
            if (str.indexOf(ch[i])>=0)
				return true;
		}
		return false;
	}

}
