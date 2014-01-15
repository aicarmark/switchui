/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2010 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * w21782                    01/18/2010                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote.content;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.motorola.quicknote.QNActivity;
import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.R;

public class QNContent_Error extends QNContent{

    QNContent_Error(String mimetype, Uri uri) {
        super(mimetype, uri);
        super.fUsable = false;
        QNDev.log("Error created."); 
    }

    /************************
     * Implement QNContent_Interface
     */

    @Override
    public boolean close() {
        return true;
    }
    
    @Override
    public NotestateE state() { return NotestateE.ERROR; }

    @Override
    public boolean isBGColor() { return false; }


    @Override
    public View noteView(Context context, ViewGroup parent) {
       return null;
    }

    @Override
    public Bitmap widgetBitmap(Context context, int width, int height) {
       // Bitmap error_bm =  BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_error);
        return null;
        
    }

    @Override
    public Bitmap widgetBitmap(Context context, int width, int height, int bgcolor)
    {
       return widgetBitmap(context, width, height);
    }
  
    @Override
    public CharSequence getTextNoteContent()
    {return "";}
}
