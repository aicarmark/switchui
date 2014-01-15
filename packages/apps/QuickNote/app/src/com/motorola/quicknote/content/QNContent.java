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
 * [QuickNote] Interface for Content Viewer
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created.
 * 
 * 
 *****************************************************************************************/


package com.motorola.quicknote.content;

import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;

import com.motorola.quicknote.QNActivity;
import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.R;
import com.motorola.quicknote.QuickNotesDB;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.QNUtil;


/**
 * This should be 'interface' NOT 'abstract'. 
 * Becuase, each content handler should be able to inherite from existing android module.
 * integer RETURN convention
 *    false : fail
 *    true : success
 */

public abstract class QNContent
{
    protected Uri     _uri        = null;
    private String  _mimetype   = null;

    // indicator : where the file is corrupted or not. default value is true
    public boolean  fUsable  = true;
    //for voice seekbar
    public boolean mSeeking = false;

    /**
     * Register listener called when state of content is changed. 
     */
    public interface OnStateListener {
        abstract public void OnState(NotestateE from, NotestateE to, boolean pausedByIncomingCall);
    }
    
    public enum SharingTypeE {
        MESSAGE, EMAIL
    }
    
    public enum BitmapTypeE {
        SCREEN_WIDGET_BITMAP, GRID_VIEW_BITMAP
    }
    protected BitmapTypeE mBitmapType = BitmapTypeE.GRID_VIEW_BITMAP;
    
    /**
     * We can assume that 'NotestateE' will not be contained in Intent. So we use 'enum' type instead of 'final static integer'
     * State machine
     *                                                                               +<-------- Start() -------+
     *                                                                               |                         |
     * [IDLE] ---Setup() ---> [SETTINGUP] ---------> [SETUP] ---- Start() ----> [STARTED] --- Pause() ---> [PAUSED]
     *    |                                             |                            |                         |
     *    +<---------------------------- Stop() --------+----------------------------+-------------------------+
     *    
     * At any error case, content should be in ERROR state.
     */
    public enum NotestateE {
        IDLE,       // stopped or initial state.
        SETTINGUP,  // It may be take time... that is, Async call my be used....
        SETUP,      // prepare to start - it can be started at any time.
        STARTED, 
        PAUSED,
        ERROR,      // error state.
    }

    
    /**
     * get android 'View' class instance. 
     * This view should be able to handle - draw, play etc... -  note contents.
     * returned View is reference of unique view instance that belongs to this interface instance.
     * This may require content setup (Setup()) before calling 'EditView' because, lookNfeel may use content's property..
     * @param context : owner context. This SHOULD NOT BE null
     * @param parent : parent view. newly added view should be attached to this parent.
     * @return : 'null' means there is no view associated..    
     */
    abstract public View noteView(Context context, ViewGroup parent);

    public View detailView(Context context, ViewGroup parent, Uri fileUri) {
        return null;
    }

    public View updateNoteView(Context context, ViewGroup parent, Uri newFileUri) {
          return null;
    }

    
    /**
     * @param owner_activity : if there is owner activity, this should be set. (this can be null)
     * @param owner_context : owner context. This SHOULD NOT BE null
     * @param widgetWidth width of widget.
     * @param widgetHeight height of widget.
     * @return bitmap for home widget.
     */
    abstract public Bitmap widgetBitmap(Context owner_context, int width, int height);
    abstract public Bitmap widgetBitmap(Context owner_context, int width, int height, int bgcolor);


    /**
     * get the thumbnail for the image
     */
   public Bitmap widgetThumbBitmap(ContentResolver cr, Long origId, int width, int height) {
        return null;
    }


    /** return the TextNote content
     */ 
    abstract public CharSequence getTextNoteContent();

    public CharSequence getTextNoteContentShort() {
        return null;
    }

    /**
     * clean generated instance (There is no 'Destructor' in Java)
     * (ex. stop played audio / disconnect socket )
     */ 
    abstract public boolean close();

    
    public NotestateE state() { return NotestateE.IDLE; }
    public void setBitmapType(BitmapTypeE type) {
        mBitmapType = type;
    }
    
    /**
     * Give updating chance to '_qnc'
     * @return
     */
    public boolean trigger_update() { return false; }
   
    /**
     * Get Uri of this QNContent
     */
    public Uri      uri() { return _uri; }
    public String   mimetype() { return _mimetype; }
    
    /*****************************************
     * Getting content's attribute -- START
     */
    
    /** 
     * @return : return sharing type array
     *           'null' means, this contents cannot be shared!!!
     */
    public SharingTypeE[] shared_by() { return null; }
    
    /**
     * This contents should have title?? or not!!
     */
    public boolean isTitle() { return true; }
    
    /**
     * Background color is available??
     */
    abstract public boolean isBGColor();
    
    /**
     * contents can be played??
     */
    public boolean isPlayable() { return false; }
    
    /**
     * Getting content's attribute -- START
     *****************************************/
    
    /**
     * Default is "Nothing happened!"
     */
    public QNContent register_OnStateListener(OnStateListener listener) { return this; }
    
    /**
     * (ex. play/stop audio)
     */
    public boolean setup() { return false; }
    public boolean start() { return false; }
    public boolean pause() { return false; }
    public boolean stop()  { return false; }
    
    /********************************
     * Not Public...
     *******************************/
    protected QNContent(String mimetype, Uri uri) {
        QNDev.qnAssert(null != uri && null != mimetype);
        _uri = uri; _mimetype = mimetype;
    }
    
}
