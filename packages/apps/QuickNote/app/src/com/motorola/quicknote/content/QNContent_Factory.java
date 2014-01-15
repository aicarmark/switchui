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
 * [QuickNote] Content Viewer factory
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created - template
 * 
 * 
 *****************************************************************************************/

package com.motorola.quicknote.content;

import java.io.File;
import java.net.URI;

import android.net.Uri;

import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.QNUtil;

/**
 * This is unique public class that can creates instance of 'QNContent_Interface'
 * That is, out-side-of package, each 'QNContent_Err, QNContent_Sound...' can not be created directly.
 * (The only way is by using 'QNContent_Factory')
 */
public class QNContent_Factory
{
    /** width/height(pixel) : size of View that should be get by 'QNContent_Interface.Get_view() */
    public static final QNContent
    Create(String mimeType, Uri uri) {
        QNContent qnc = null;
        if (mimeType == null || uri == null) {
            return (QNContent) new QNContent_Error(mimeType, uri);
        }
		
        if(((!QNDev.STORE_TEXT_NOTE_ON_SDCARD) && 
            (mimeType.startsWith("image/") || mimeType.startsWith("audio/")) 
             && uri.getScheme().equals("file"))
            || ((QNDev.STORE_TEXT_NOTE_ON_SDCARD) && 
            (mimeType.startsWith("image/") || mimeType.startsWith("audio/") || mimeType.startsWith("text/")) 
             && uri.getScheme().equals("file"))) {
            if(uri.getSchemeSpecificPart().equals("//null")){
                return (QNContent) new QNContent_Error(mimeType, uri);
            }

            String filePath = uri.toString();
            String newFilePath = QNUtil.transFilePath(filePath); 
            File mediaFile = new File(newFilePath);
            if(mediaFile.exists() == false){
                return (QNContent) new QNContent_Error(mimeType, uri);
            }
        }

        if(mimeType.startsWith("image/"))
        	qnc = (QNContent) new QNContent_Image(mimeType, uri);
        else if(mimeType.startsWith("audio/"))
        	qnc = (QNContent) new QNContent_Snd(mimeType, uri);
        else if(mimeType.startsWith("text/"))
        	qnc = (QNContent) new QNContent_Text(mimeType, uri);
        else
        	QNDev.qnAssert(false, "Not implemented yet");

        return qnc;
    }

}
