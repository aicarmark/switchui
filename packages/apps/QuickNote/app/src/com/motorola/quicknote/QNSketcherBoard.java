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
 * [QuickNote] Main draw board for sketch.
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created.
 * 
 * 
 *****************************************************************************************/

/*********************************
 *  IMPORATANT NOTICE!! (Strongly recommended!)
 *      Input of Board should be file (not Bitmap instance)!!
 *      If background image has very high resolution, 
 *          keeping original bitmap as an memory instance wastes too much memory!
 *      Board will generate enough small bitmap instance from input file, to show reasonable view to users to save memory.
 *  TODO
 *      Board use ARGB_8888 as a sketch board.. but we just uses 8 color. So, we can use 256 color instead of ARGB_8888 (we can save memory..)
 *      So, if memory is issued, this alogrithm should be modifyed into the way of using lower-depth-color-format 
 *********************************/

package com.motorola.quicknote;

import java.io.IOException;
import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.motorola.quicknote.QNSketcher;


class Board extends View {

    private static final String TAG = "QNSketcherBoard";
    
    /******************************
     * Constants
     ******************************/
    enum ToolE { PEN, ERASER, MOVE, BACKGROUND }
    enum ThicknessE { THICK1, THICK2, THICK3, NULL_THICKESS }
    enum ColorE { BLACK, RED, YELLOW, GREEN, BLUE, GRADIENT, WHITE, MASK_COLOR, NULL_COLOR }
    
    // Color value format is "ARGB", but in bitmap, color format is saved as "ABGR"...<-- Check IT!!!
    //private static final int    _MASK_COLOR = 0xff00dead; 
    private static final int    _MASK_COLOR = android.graphics.Color.TRANSPARENT;
    private static final int    SKETCH_BACKGROUND_COLOR = 0xFFFAFAFA;
    
    // Keys for bundling!!!
    private static final String _BUNDLE_TOOL        = "board_tool";
    private static final String _BUNDLE_PEN_COLOR   = "board_pen_color";
    private static final String _BUNDLE_PEN_THICK   = "board_pen_thick";
    private static final String _BUNDLE_ERASER_THICK= "board_eraser_thick";
    private static final String _BUNDLE_BGCOLOR     = "board_bg_color";
    private static final String _BUNDLE_OX          = "board_ox";
    private static final String _BUNDLE_OY          = "board_oy";
    private static final String _BUNDLE_CMD_INDEX   = "board_cmd_index";
    private static final String _BUNDLE_CMD_LIST    = "board_cmd_list";
    

    /******************************
     * members
     ******************************/    
    // _State keeper..
    private _State_Pen      _state_pen;
    private _State_Pen      _state_eraser;  // eraser is pen with specific color...!!!
    private _State_BGColor  _state_background; // bg color management state. 
    
    /** not used in current UIS */
    //private _State_Move     _state_move;
    
    // _State indicator
    private _State          _state; 
	/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */
    private _State			mActionDownState = null;
	/*2012-10-16, add end*/
    
    
    private String          _bgfile = null;
    private Bitmap          _board_bm = null;
    private Canvas          _board_canvas = null;
    //private Bitmap          _intermid_bm = null;// copied bm from canvas_bm..(mask color is applied)
    private Bitmap          _bg_bm = null;     // background bitmap.
    
    // Picture move is excluded from UIS. So, _ox, _oy are always 0!
    // Keep these two variables for future use.
    private float           _ox, _oy;   // view origin
    public LinkedList<_Cmd> _histories;
    public int             _cmdi;      // command index    
    
    public boolean          ispen = true;
    
    
    /******************************
     * Types
     ******************************/       
    
    /******************************
     * Simple point array. 
     * NOTE : This keeps normalized value. But it returns screen value.
     *       (To support scaling canvas!!! we should use normalized point!.
     */
    private static class _ArrPt implements Parcelable {
        static final int _BASE_SIZE = 20; 
        
        private int     _limit;
        private int     _size;
        
        private float[] _pts;
        
        _ArrPt() {
            _limit = _BASE_SIZE;
            _size = 0;
            _pts = new float[_limit*2];
        }

        void append(Board board, float x, float y) {
            QNDev.qnAssert(_size <= _limit);
            if (_size >= _limit) {
                float[] newpts = new float[_limit*2*2];
                System.arraycopy(_pts, 0, newpts, 0, _limit*2);
                _pts = newpts;
                _limit *= 2;
                
            }
            _pts[_size*2] = board._normalizeX(x);
            _pts[_size*2+1] = board._normalizeY(y);
            _size++;
        }
        
        void    clear() { _size = 0; } 
        
        int     size() { return _size; }
        int     limit() { return _limit; }
        float   x(Board board, int i) { return board._normal_to_canvasX(_pts[i*2]); }
        float   y(Board board, int i) { return board._normal_to_canvasY(_pts[i*2+1]); }
        
        /****** Implement interface ********/
        public int describeContents() { return 0;  }
        
        // To support parcelable!!
        _ArrPt(Parcel in) {
            _limit = in.readInt();
            _pts = new float[_limit*2];
            _size = in.readInt();
            in.readFloatArray(_pts);
        }
        
        // Implements parcelable!!
        // NOTE First byte should be type of command!!
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(_limit);
            dest.writeInt(_size);
            dest.writeFloatArray(_pts);
        }
        
        public static final Parcelable.Creator<_ArrPt> CREATOR
            = new Parcelable.Creator<_ArrPt>() {
                public _ArrPt createFromParcel(Parcel in) {
                    return new _ArrPt(in);
                }
            
                public _ArrPt[] newArray(int size) {
                    return new _ArrPt[size];
                }
        };
    }
    
    /*********************************
     * _State  
     */
    private abstract class _State {
        private final ToolE     _tool;
        
        _State(ToolE tool) { _tool = tool; } 
        ColorE      color() { return ColorE.NULL_COLOR; }
        ThicknessE  thickness() { return ThicknessE.NULL_THICKESS; }
        ColorE      color(ColorE color) { return ColorE.NULL_COLOR; }
        ThicknessE  thickness(ThicknessE thick) { return ThicknessE.NULL_THICKESS; }
        ToolE       tool() { return _tool; }
        
        
        boolean onTouch(MotionEvent me) {
            switch(me.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    _clean_obsolete_history();
                } break;
                
                case MotionEvent.ACTION_MOVE: {
                } break;
                
                case MotionEvent.ACTION_UP: {
                } break;
            }
            return true;
        }
    }
    
    private class _State_Pen extends _State {
        private ColorE          _color;
        private ThicknessE      _thickness;
        private _ArrPt          _pts;
        private boolean         mIsClearMode = false;
        private Paint           mPaint;
        private boolean         mUsingCustomizeColor = false;
        private int             mCustomizeColor = 0xFF000000;

        _State_Pen(ToolE tool) {
            super(tool);
            _color = ColorE.BLACK;
            _thickness = ThicknessE.THICK2;
            if (tool == ToolE.ERASER) {
                mIsClearMode = true;
            }
        }
        
        @Override
        ColorE      color(ColorE color) { ColorE sv = _color; mUsingCustomizeColor = false; _color = color; return sv; }

        void        color(ColorE color, int colorInt) { 
            mUsingCustomizeColor = true;
            _color = color; 
            mCustomizeColor = colorInt;
        }

        @Override
        ThicknessE  thickness(ThicknessE thickness) { ThicknessE sv = _thickness; _thickness = thickness; return sv; }

        @Override
        ColorE      color() { return _color; }

        @Override
        ThicknessE  thickness() { return _thickness; }

        @Override
        boolean onTouch(MotionEvent me) {
            super.onTouch(me);
            
            float screen_x = _canvas_to_bscreen(me.getX()),
                  screen_y = _canvas_to_bscreen(me.getY());
            // For safety reason, lets use '+2'
            // '+2' covers all case with enough safety.
            int   thick_radius = (int)( _map_thickness(_thickness) + 2 );
            
            switch(me.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    // allocate new _ArrPt instance
                    _pts = new _ArrPt();
                    _pts.append(Board.this, screen_x, screen_y);
                    if (mUsingCustomizeColor) {
                        mPaint = _android_paint(_thickness, mCustomizeColor);
                    } else {
                        mPaint = _android_paint(_thickness, _color);
                    }
                    if (mIsClearMode) {
                        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    }
                    if (null != _board_canvas)
                        _board_canvas.drawPoint(screen_x, screen_y, mPaint);
                                   invalidate((int)(me.getX() - thick_radius), 
                                (int)(me.getY() - thick_radius),
                                (int)(me.getX() + thick_radius),
                                (int)(me.getY() + thick_radius));
                    } break;
                
                case MotionEvent.ACTION_MOVE: {
					/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */ 
                    if(null == _pts) {
                    	return true;
                    }
					/*2012-10-16, add end*/ 
                    QNDev.qnAssert(_pts.size() > 0);
                    if (null != _board_canvas)
                    	if(mPaint == null){
                    		QNDev.log("_State_Pen->onTouch->MotionEvent.ACTION_MOVE ->mPaint == null");
                    		return true;
                    	}
                      _board_canvas.drawLine(_pts.x(Board.this, _pts.size()-1), _pts.y(Board.this, _pts.size()-1), 
                            screen_x, screen_y, mPaint);
                    /*
                    if ( mevt.getHistorysize() > 0) {
                        _board_canvas.drawLine(mevt.getX(mevt.getHistorysize()), mevt.getY(mevt.getHistorysize()), 
                                        mevt.getX(), mevt.getY(), _paint);
                    }
                    */
                    
                    // Find invalidate rectangel..
                    int minx, miny, maxx, maxy;
                    if(screen_x > _pts.x(Board.this, _pts.size()-1)) {
                        minx = (int)_bscreen_to_canvas(_pts.x(Board.this, _pts.size()-1)) - thick_radius;
                        maxx = (int)_bscreen_to_canvas(screen_x) + thick_radius;
                    } else {
                        maxx = (int)_bscreen_to_canvas(_pts.x(Board.this, _pts.size()-1)) + thick_radius;
                        minx = (int)_bscreen_to_canvas(screen_x) - thick_radius;
                    }
                    
                    if(screen_y > _pts.y(Board.this, _pts.size()-1)) {
                        miny = (int)_bscreen_to_canvas(_pts.y(Board.this, _pts.size()-1)) - thick_radius;
                        maxy = (int)_bscreen_to_canvas(screen_y) + thick_radius;
                    } else {
                        maxy = (int)_bscreen_to_canvas(_pts.y(Board.this, _pts.size()-1)) + thick_radius;
                        miny = (int)_bscreen_to_canvas(screen_y) - thick_radius;
                    }
                    
                    _pts.append(Board.this, screen_x, screen_y);
                    invalidate(minx, miny, maxx, maxy);
                } break;
                
                case MotionEvent.ACTION_UP: {
                    //_pts.append(screen_x, screen_y);
                    //_board_canvas.drawPoint(screen_x, screen_y, _android_paint());
                    QNDev.qnAssert(null != _pts);
						/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */ 
                    if(null == _pts) {
                    	return true;
                    }
						/*2012-10-16, add end*/ 
                    if(_pts.size() > 0) {
                        _Cmd_DrawLines cmd = new _Cmd_DrawLines();
                        cmd.color(mUsingCustomizeColor ? mCustomizeColor : map_color(_color));
                        cmd.thickness(_map_thickness(_thickness));
                        cmd.pts(_pts);
                        cmd.setClearMode(mIsClearMode);
                        _add_to_history(cmd);
                    }
                    invalidate();
                } break;
                
                default:
                    ;
            }
            return true;
        }      
           
    }
    
    private class _State_BGColor extends _State {
        private ColorE  _color;
        
        _State_BGColor() {
            super(ToolE.BACKGROUND);
            _color = ColorE.NULL_COLOR;
        }
        
        @Override
        ColorE      color() { return _color; }
        @Override
        ColorE      color(ColorE color) { 
            ColorE sv = _color; 
            _color = color;
            invalidate();   // entire canvas should be redrawn 
            return sv; 
        }
    }    
    
    
    /** Not used in current UIS
    // Move is not used -- it is excluded from UIS!!! ==> We don't care about This!!
    private class _State_Move extends _State {
        private float _px, _py,     // previous x, previous y
                      _pox, _poy;   // previous _ox, _oy. 
        
        _State_Move() {
            super(ToolE.MOVE);
        }
        
        @Override
        boolean onTouch(MotionEvent me) {
            super.onTouch(me);
            
            float screen_x = _canvas_to_bscreen(me.getX()),
                  screen_y = _canvas_to_bscreen(me.getY());
            
            switch(me.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    _pox = _ox; _poy = _oy;
                    _px = screen_x; _py = screen_y;
                } break;
                
                case MotionEvent.ACTION_MOVE: {
                    _ox -= screen_x - _px;
                    _oy -= screen_y - _py;
                    if (_ox < 0) { _ox = 0; }
                    else if ( _ox > (_board_bm.getWidth() - getWidth()) ) { _ox = _board_bm.getWidth() - getWidth(); }
                    if (_oy < 0) { _oy = 0; }
                    else if (_oy > (_board_bm.getHeight() - getHeight()) ) { _oy = _board_bm.getHeight() - getHeight(); }
                    _px = me.getX() + _ox; _py = me.getY() + _oy;
                    invalidate();
                } break;

                case MotionEvent.ACTION_UP: {
                    _Cmd_Move cmd = new _Cmd_Move();
                    cmd.Pos_delta(_ox - _pox, _oy - _poy);
                    _add_to_history(cmd);
                }
                
                default:
                    ;
            }
            return true;            
        }
    }
    */

    /*********************************
     * Commands.  
     */

    private abstract class _Cmd implements Parcelable {
        abstract boolean execute(Board board);

        // Implementation of Parcelable
        public int describeContents() { return 0;  }

    }

    private class _Cmd_DrawLines extends _Cmd {
        // _pts has normalized values.
        private _ArrPt  _pts;
        private float   _thickness;
        private int     _color;
        private boolean mIsClearMode = false;
        
        _Cmd_DrawLines(){}
        
        void pts(_ArrPt pts) { _pts = pts; }
        void color(int color) { _color = color; }
        void thickness(float thickness) { _thickness = thickness; }
        void setClearMode(boolean isClearMode) {
            mIsClearMode = isClearMode;
        }

        @Override
        boolean execute(Board board) {
            QNDev.qnAssert(null != _pts);
            if (null == _pts || null == board || null == board._board_canvas)
                return false;

            Paint paint = null;
            if(_pts.size()>0)
            {
                //draw a piont firstly,otherwise the dot will be missed 
                paint = _android_paint(_thickness, _color);
                if (mIsClearMode) {
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }
                board._board_canvas.drawPoint(_pts.x(board, 0), _pts.y(board, 0), paint);
            }
            for(int j=0; j<_pts.size()-1; j++) {
                    //draw a piont firstly,otherwise the dot will be missed
                    //board._board_canvas.drawPoint(_pts.x(board, j), _pts.y(board, j),_android_paint(_thickness, _color));      
                    board._board_canvas.drawLine(_pts.x(board, j), _pts.y(board, j), 
                                                 _pts.x(board, j+1), _pts.y(board, j+1), 
                                                 paint);
             }
             return true;
        }
        
        // To support parcelable!!
        _Cmd_DrawLines(Parcel in) {
            _thickness = in.readFloat();
            QNDev.log("Parcel read thick " + _thickness);
            _color = in.readInt();
            _pts = in.readParcelable(null);
        }
        
        // Implements parcelable!!
        // NOTE First byte should be type of command!!
        public void writeToParcel(Parcel dest, int flags) {
            // This is mandatory.. this should be head of data!!!
            QNDev.log("Parcel type 0 / thick " + _thickness);
            dest.writeFloat(_thickness);
            dest.writeInt(_color);
            dest.writeParcelable(_pts, flags);
        }
        
        public final Parcelable.Creator<_Cmd> CREATOR
            = new Parcelable.Creator<_Cmd>() {
                public _Cmd createFromParcel(Parcel in) {
                    return new _Cmd_DrawLines(in);
                }
            
                public _Cmd[] newArray(int size) {
                    return new _Cmd_DrawLines[size];
                }
        };
        
    }
    
    /** Not used...
    private class _Cmd_Move extends _Cmd {
        float   _dx, _dy;
        void Pos_delta(float dx, float dy) { _dx = dx; _dy = dy; }
        @Override
        boolean Execute() {
            _ox += _dx; _oy += _dy;
            return true;
        }
    }
    **/
  
    
    /**************************
     * Local Functions.
     **************************/
    private void _local_init(Context context) {
        
        // Instanciate state.
        _state_pen = new _State_Pen(ToolE.PEN);
        _state_eraser = new _State_Pen(ToolE.ERASER);
        //_state_eraser.color(ColorE.MASK_COLOR);
        _state_background = new _State_BGColor();
        
        /** Not used in current UIS */
        //_state_move = new _State_Move();
        
        _state = _state_pen;
        
        // initial setup.
        _ox = _oy = 0;
        _cmdi = 0;
        _histories = new LinkedList<_Cmd>();
        
    }

    
    // obsolete
    //private float Min(float a, float b) { if(a > b) { return b; } else { return a; } }
    //private float Max(float a, float b) { if(a > b) { return a; } else { return b; } }
    
    // bscreen : board screen.
    // Move is not used. So, let's keep make it simple
    private float _canvas_to_bscreen(float x) { return x /* + _ox */; }
    private float _bscreen_to_canvas(float x) { return x /* - _ox */; }
    
    // normalize into 1x1 canvas. -- NOTE : We don't consider about moved screen.. --need to fix if move should be supported.
    private float _normalizeX(float x) {
        if(null != _board_bm) {
            return x / _board_bm.getWidth(); 
        } else {
            //in real case, it should not be here!!!
            return 1;
        }
    }

    private float _normalizeY(float y) {
        if(null != _board_bm) {
            return y / _board_bm.getHeight();
        } else {
            //in real case, it should not be here!!!
            return 1;
        }
    }

    private float _normal_to_canvasX(float x) { 
        if(null != _board_bm) {
            return x*_board_bm.getWidth(); 
        } else {
            //in real case, it should not be here!!!
            return x;
        }
    }

    private float _normal_to_canvasY(float y) {
        if(null != _board_bm) {
            return y*_board_bm.getHeight(); 
        } else {
            //in real case, it should not be here!!
            return y;
        }
    }
    
    
    private float _map_thickness(final ThicknessE thickness) {
        float tmp = 0;
        switch(thickness) {
            case THICK1:
                tmp = 2; 
                break;
            case THICK2:
                tmp = 6; 
                break;
            case THICK3:
                tmp = 10; 
                break;
            default: 
                tmp = 6; 
                break;
        }
        if(ispen) {
            return tmp;
        } else {
            // eraser, make it more thicker than pen
            return 4 * (tmp + 1);
        }
    }
    
    private void _back_to_undo_base() {
        // clear canvas.
        if (null != _board_bm) {
            _board_bm.eraseColor(android.graphics.Color.TRANSPARENT);
        }
        _ox = _oy = 0;
    }
    
    private void _add_to_history(_Cmd cmd) {
        _histories.addLast(cmd);
        _cmdi++;
    }
    
    private void _clean_obsolete_history() {
        QNDev.qnAssert(_cmdi <= _histories.size());
        if(_cmdi < _histories.size()) {
            for(int i=_histories.size() - _cmdi; i>0; i--) {
                _histories.removeLast();
            }
        }
    }
    
    private Paint _android_paint(ThicknessE thickness, ColorE color) { 
        return _android_paint(_map_thickness(thickness), map_color(color)); 
    }
    
    private Paint _android_paint(ThicknessE thickness, int color) { 
        return _android_paint(_map_thickness(thickness), color); 
    }

    private Paint _android_paint(float thickness, int color) {
        Paint paint = new Paint();
        paint.setStrokeWidth(thickness);
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        return paint;
    }
    

    /**************************
     * Overriding.
     **************************/
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        ((QNSketcher)mContext).hidePanel();
			/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */
        //return _state.onTouch(me);
        // prevent from changing _state from in cycle(action down to up)
        boolean bRet = false;
        if(MotionEvent.ACTION_DOWN == me.getAction()) {
        	mActionDownState = _state;
        	bRet = _state.onTouch(me);
        } else if(MotionEvent.ACTION_UP == me.getAction()){
        	bRet = mActionDownState.onTouch(me);
        	mActionDownState = null;
        } else {
        	if((null != _state)
        			&& _state == mActionDownState) {
        		bRet = _state.onTouch(me);
        	} else {
        		bRet = true;
        	}
        }
        return bRet;
			/*2012-10-16, add end*/
    }
    
	/*2012-10-16, add by amt_sunzhao for SWITCHUITWOV-254 */
    public boolean isTouching() {
    	return null != mActionDownState;
    }
    /*2012-10-16, add end*/
    @Override
    protected void onDraw(Canvas canvas) {
      QNDev.logd(TAG, "QNSketcherBoard:onDraw(), canvas=" + canvas);
      try {
        if(null != canvas) {
            super.onDraw(canvas);
            
            // nothing to do..when _board_bm == null
            if(null == _board_bm) { return; }
            
            Rect src = new Rect(),
            clip = new Rect(); // clip bound
       
            if( !canvas.getClipBounds(clip) ) {
                // there is no clip bound.. clip is entire canvas...
                clip.left = clip.top = 0;
                //clip.right = canvas.getWidth();
                //clip.bottom = canvas.getHeight();
                clip.right = _board_bm.getWidth();
                clip.bottom = _board_bm.getHeight();
            }
                        
            src.left = (int)_canvas_to_bscreen((float)clip.left);
            src.top = (int)_canvas_to_bscreen((float)clip.top);
            src.right = src.left + clip.width();
            src.bottom = src.top + clip.height();
            
            //QNDev.qnAssert(src.left >=0 && src.top >=0
            //                && src.right <= canvas.getWidth()
            //                && src.bottom <= canvas.getHeight() );   

/*
            if(ColorE.NULL_COLOR != _state_background.color()) {
                paint.setColor(map_color(_state_background.color()));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(clip, paint);
            }
*/
            if(null != _bg_bm) {
              try {
                canvas.drawBitmap(_bg_bm, src, clip, null);
              } catch (Exception e) {
                 //do nothing;
              }
            }/* else {       
                Paint paint = new Paint();
                paint.setColor(SKETCH_BACKGROUND_COLOR);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(clip, paint);
            }*/

            /*
            if(!QNUtil.set_Bitmap(_intermid_bm, src, android.graphics.Color.TRANSPARENT)) {
                QNDev.qnAssert(false); // undexpected
            }*/
/*
            _intermid_bm.eraseColor(android.graphics.Color.TRANSPARENT);
            //if(!QNUtil.copy_Bitmap(_intermid_bm, _board_bm, src, src, _MASK_COLOR)) {
            if(!QNUtil.copy_Bitmap(_intermid_bm, _board_bm, src, src, android.graphics.Color.TRANSPARENT)) {
                QNDev.qnAssert(false); // unexpected
            }
            canvas.drawBitmap(_intermid_bm, src, clip, paint);
*/

            canvas.drawBitmap(_board_bm, src, clip, null);

            ((QNSketcher)mContext).updateButton();
/*
            if (_cmdi <= 0) {
                //btnDiscard.setEnabled(false);
                QNSketcher.mBottom_btn_discard.setEnabled(false);
            } else {
                //btnDiscard.setEnabled(true);
                QNSketcher.mBottom_btn_discard.setEnabled(true);
            }
*/
        }
      } catch (Exception e) {
         QNDev.log("QNSketcherBoard:onDraw catch exception:  " + e);
         return;
      }
    }    
    
    

    
    /**************************
     * APIs.
     **************************/
    Board(Context context) {
        super(context);

        mContext = context;
        _local_init(context);
    }

    private Bitmap resizeBitmap (Bitmap srcBmp, int width, int height, int rotation) {
        if (srcBmp == null ) return null;
        int targetWidth = srcBmp.getWidth();
        int targetHeight = srcBmp.getHeight();
        Bitmap tempBmp = null;

        if (rotation == ExifInterface.ORIENTATION_ROTATE_90 ||
            rotation == ExifInterface.ORIENTATION_ROTATE_180 ||
            rotation == ExifInterface.ORIENTATION_ROTATE_270) {
            Matrix matrix = new Matrix();
            int degree = 0;
            switch (rotation) {
              case ExifInterface.ORIENTATION_ROTATE_90:
                degree = 90;
                break;
              case ExifInterface.ORIENTATION_ROTATE_180:
                degree = 180;
                break;
              case ExifInterface.ORIENTATION_ROTATE_270:
                degree = 270;
                break;
              default:
                break;
            }
            matrix.postRotate(degree);
            tempBmp = Bitmap.createBitmap(srcBmp, 0, 0, targetWidth, targetHeight, matrix, true);
        } else {
            tempBmp = srcBmp;
        }
        return tempBmp;
    }

    /**
     * Setting up board. Allocating resources for board, Decoding background image if needed etc...
     * @param bgfile : 
     *   background image file (This SHOULD BE ABSOLUTE file path).
     *   This can be null, if background image is not used!
     * @param maxWidth : maximum width of board can be 
     * @param maxHeight : maximum height of board can be
     * @return : false(fail)
     */
    boolean setup(String bgfile, int maxWidth, int maxHeight) {
        QNDev.logd(TAG, "setup(), bgfile=" + bgfile + " maxWidth=" + maxWidth + " maxHeight=" + maxHeight);
        // setup board.
        try {
            if ((null == bgfile) || !(new File(bgfile)).exists())  {
                // if no background file (photo or screen shot), use default
                // do not decode bg resource to save memory
/*                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                _bg_bm = BitmapFactory.decodeResource(mContext.getResources(), 
                                                      R.drawable.bg_text_note_paper_single, options);
*/
            } else {
                QNDev.qnAssert(QNUtil.is_absolute_path(bgfile));
                int rotation = ExifInterface.ORIENTATION_NORMAL;
                try {
                    ExifInterface exif = new ExifInterface(bgfile);
                    rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    QNDev.logd(TAG, "setup(), rotation=" + rotation);
                } catch (IOException e) {
                  QNDev.log("Error reading Exif information, probably not a jpeg.");
                }
                if (ExifInterface.ORIENTATION_NORMAL == rotation || ExifInterface.ORIENTATION_UNDEFINED == rotation) {
                    _bg_bm = QNUtil.decode_image_file_for_sketch(bgfile, maxWidth, maxHeight);
                } else {
                    _bg_bm = QNUtil.decode_image_file_for_sketch(bgfile, maxHeight, maxWidth);
                }
                _bg_bm = resizeBitmap(_bg_bm, maxWidth, maxHeight, rotation);
            }
            if (null != _bg_bm) {
                // back to default density
                _bg_bm.setDensity(android.util.TypedValue.DENSITY_DEFAULT);
                _board_bm = Bitmap.createBitmap(_bg_bm.getWidth(), _bg_bm.getHeight(), Bitmap.Config.ARGB_8888);
            } else {
                _board_bm = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
            }
            if (null != _board_bm) {
                _board_canvas = new Canvas(_board_bm);
                //_intermid_bm = Bitmap.createBitmap(_board_bm.getWidth(), _board_bm.getHeight(), _board_bm.getConfig());
            }
            _bgfile = bgfile;

            QNDev.logd(TAG, "after setup(), memory usage:");
            QNDev.logd(TAG, "_bg_bm=" + _bg_bm.getByteCount() + " bytes" + " width=" + 
                             _bg_bm.getWidth() + " height=" + _bg_bm.getHeight());
            QNDev.logd(TAG, "_board_bm=" + _board_bm.getByteCount() + " bytes" + " width=" + 
                             _board_bm.getWidth() + " height=" + _board_bm.getHeight());
            return true;
       }  catch (Exception ex) {
            return false;
       } catch (java.lang.OutOfMemoryError e) {
            return false;
       }
    }
    
    
    /**
     * Create output bitmap(result of sketch/edited image)
     * @param width : see 'height' 
     * @param height :
     *   height of result image. 
     *   If background image is smaller than required size(based on 'width' and 'height', 
     *     than original size is preserved. (that is, not scaled!)
     *   But, background image size is larger than required size, it will be shrunk to adjust to the size.
     *   In case of color background, it operates same with large-background-case!.
     * @return
     *   null if fails.
     */    
    Bitmap create_output(int width, int height) {
        QNDev.logd(TAG, "create_output()");
        /***********************************
         * FIXME : Memory shortage or optimization is not considered yet!!!
         ***********************************/
        if(0 >= width || 0>= height) { return null; }
            
        try {
            if (_bg_bm != null) {
                Canvas canvas = new Canvas(_bg_bm);
                canvas.drawBitmap(_board_bm, 0, 0, null);
                return _bg_bm;
            } else {
                Bitmap bm = Bitmap.createBitmap(_board_bm.getWidth(), _board_bm.getHeight(), Bitmap.Config.ARGB_8888);
                bm.eraseColor(SKETCH_BACKGROUND_COLOR); 
                Canvas canvas = new Canvas(bm);
                canvas.drawBitmap(_board_bm, 0, 0, null);
                return bm;
            }
        } catch (Exception e) {
            QNDev.logd(TAG, "create_output() caught exception:" + e.toString());
            return null;
        } catch (java.lang.OutOfMemoryError me) {
             return null;
        }

    }


    int         board_width() { return (null == _board_bm)? -1: _board_bm.getWidth(); }
    int         board_height() { return (null == _board_bm)? -1: _board_bm.getHeight(); }
    ColorE      color(ColorE color) { return _state.color(color); }
    ColorE      color() { return _state.color(); }
    ThicknessE  thickness(ThicknessE thickness) { return _state.thickness(thickness); }
    ThicknessE  thickness() { return _state.thickness(); }
    
    ColorE      pen_color()     { return _state_pen.color(); }
    ThicknessE  pen_thick()     { return _state_pen.thickness(); }
    ThicknessE  eraser_thick()  { return _state_eraser.thickness(); }
    ColorE      bg_color()      { return _state_background.color(); }
    
    ColorE      pen_color(ColorE color)         { return _state_pen.color(color); }
    void        pen_color(ColorE color, int colorInt) {
        if (color == ColorE.GRADIENT) {
            _state_pen.color(color, colorInt);
        }
    }
    ThicknessE  pen_thick(ThicknessE thick)     { return _state_pen.thickness(thick); }
    ThicknessE  eraser_thick(ThicknessE thick)  { return _state_eraser.thickness(thick); }
    ColorE      bg_color(ColorE color)          { return _state_background.color(color); }

    ToolE       tool(ToolE tool) {
        ToolE sv = _state.tool();
        switch(tool) {
            case PEN:
                _state = _state_pen;
                break;
            case ERASER:
                _state = _state_eraser;
                break;
            /** Not used in current UIS */
            //case MOVE:      _state = _state_move;       break;
            case BACKGROUND:
                _state = _state_background;
                break;
            default:
                QNDev.qnAssert(false);
        }
        return sv;
    }
    ToolE       tool() { return _state.tool(); }
    String      bgfile() { return _bgfile; }

    /**
     * Get memento of this instance... 
     * @return : null (fail).
     */
    void store(Bundle out) {
        QNDev.logd(TAG, "store()");
        out.putString(_BUNDLE_TOOL,         _state.tool().toString());
        out.putString(_BUNDLE_PEN_COLOR,    _state_pen.color().toString());
        out.putString(_BUNDLE_PEN_THICK,    _state_pen.thickness().toString());
        out.putString(_BUNDLE_ERASER_THICK, _state_eraser.thickness().toString());
        out.putString(_BUNDLE_BGCOLOR,      _state_background.color().toString());
        
        out.putFloat (_BUNDLE_OX,           _ox);
        out.putFloat (_BUNDLE_OY,           _oy);
        out.putInt   (_BUNDLE_CMD_INDEX,    _cmdi);
        
        QNDev.log("QNSketchBoard"+"store _cmdi = "+_cmdi+ "  _histories.size = "+_histories.size());
        _Cmd[] cmds = new _Cmd[_histories.size()];
        _histories.toArray(cmds);
        out.putParcelableArray(_BUNDLE_CMD_LIST, (Parcelable[])cmds);
    }
    
    /**
     * @param mt : memento getting from "Memorize()"
     * @return : false(fail)
     */
    void restore(Bundle in) {
        QNDev.logd(TAG, "restore()");
        // Before calling this function, board should be setup...
        //QNDev.qnAssert(null != _board_bm && _board_bm.getWidth() > 0 && _board_bm.getHeight() > 0);
        if (null == _board_bm || _board_bm.getWidth() <= 0 || _board_bm.getHeight() <= 0) {
           return;
        }
        
        // NOTE : Let's skip null-checking here to simplify code!!
        _state_background.color(ColorE.valueOf(in.getString(_BUNDLE_BGCOLOR)));
        _state_eraser.thickness(ThicknessE.valueOf(in.getString(_BUNDLE_ERASER_THICK)));
        _state_pen.thickness(ThicknessE.valueOf(in.getString(_BUNDLE_PEN_THICK)));
        _state_pen.color(ColorE.valueOf(in.getString(_BUNDLE_PEN_COLOR)));
        tool(ToolE.valueOf(in.getString(_BUNDLE_TOOL)));
        
        _ox = in.getFloat(_BUNDLE_OX, 0);
        _oy = in.getFloat(_BUNDLE_OY, 0);
        _cmdi = in.getInt(_BUNDLE_CMD_INDEX, 0);
        
        _Cmd[] cmds = (_Cmd[])in.getParcelableArray(_BUNDLE_CMD_LIST);
        if (null == cmds) { return; }

        //firstly restore all the drawing
        for(_Cmd c : cmds) {
            // execute again!! restore!!
            c.execute(this);
            _histories.addLast(c);
        }

        //check whether need to withdraw some lines
        if (_cmdi < _histories.size()) {
           QNDev.log("QNSketchBoard"+"restore, need withdraw some lines");
            _back_to_undo_base();
            ListIterator<_Cmd>   itr = _histories.listIterator();
            _Cmd   cmd;
            int    i = _cmdi;
            while(i-- > 0) {
               QNDev.qnAssert(itr.hasNext());
               cmd = itr.next();
               cmd.execute(this);
            }
            invalidate();            
        }
       
        // status is changed!!
        invalidate();
    }
    
    /**
     * Clean board...!! (background is kept)
     */
    boolean clean() {
        if(null != _board_bm) {
            _board_bm.eraseColor(map_color(ColorE.MASK_COLOR));
            _histories.clear();
            _cmdi = 0;
            invalidate();
        }
        return true;
    }
    
    void destroy() {
        /* 
         * To return unused memory immediately...
         * Changing orientation repeatedly may raise "OutOfMemory" Exception,
         *  because board fails to allocate Bitmap.
         * To avoid this, we should return unused memory immediately when destroyed 
         */
        _histories.clear();
        if(null != _board_bm) { _board_bm.recycle(); _board_bm = null;}
        //if(null != _intermid_bm) { _intermid_bm.recycle(); _intermid_bm = null;}
        if(null != _bg_bm) { _bg_bm.recycle(); _bg_bm = null;}
    }
    
    boolean undo() {
        if(_cmdi <= 0) { return false; }
        QNDev.qnAssert( (0<=_cmdi) && (_cmdi <= _histories.size()) );
        _cmdi--;

        _back_to_undo_base();
        
        ListIterator<_Cmd>   itr = _histories.listIterator();
        _Cmd                 cmd;
        int                 i = _cmdi;
        while(i-- > 0) {
            QNDev.qnAssert(itr.hasNext());
            cmd = itr.next();
            cmd.execute(this);
        }
        invalidate();
        return true;
    }
    
    boolean redo() {
        if(_cmdi >= _histories.size()) { return false; }
        QNDev.qnAssert( (0<=_cmdi) && (_cmdi <= _histories.size()) );
        
        ListIterator<_Cmd>   itr = _histories.listIterator();
        _Cmd                 cmd;
        int                 i=0;
        while(itr.hasNext()) {
            cmd = itr.next();
            if(i == _cmdi) {
                cmd.execute(this);
                break;
            }
            i++;
        }
        _cmdi++;
        invalidate();
        return true;
    }    

    static int map_color(final ColorE color) {
        switch(color) {
            case BLACK:     return android.graphics.Color.BLACK;
            case RED:       return 0xFFEC1C24;
            case YELLOW:    return 0xFFFFDE17;
            case GREEN:     return 0xFF00A14B;
            case BLUE:      return 0xFF00AEEF;
            case WHITE:     return android.graphics.Color.WHITE;
            case MASK_COLOR: return _MASK_COLOR;
            default:         return android.graphics.Color.RED;
        }
    }
    

    
}
