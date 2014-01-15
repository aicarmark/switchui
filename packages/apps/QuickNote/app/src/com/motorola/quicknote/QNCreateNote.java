/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2010 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * w21782                    01/26/2010                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote;

import android.content.Intent;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.motorola.quicknote.QNUtil;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.content.QNContent_Text;

public class QNCreateNote extends QNActivity{
    private Intent  mIntent;
    private Bundle  mExtras;

    private static final String TAG = "[QNCreateNote]";

    private void doAction(){
        String action = mIntent.getAction();
        Uri noteUri = null;
        StringBuffer noteText = new StringBuffer();
        
       if (Intent.ACTION_SEND.equals(action) && mExtras != null && mExtras.containsKey(Intent.EXTRA_TEXT)) {
            noteText.append(mExtras.getString(Intent.EXTRA_TEXT));
            QNDev.log("Intent.ACTION_SEND extra -EXTRA_TEXT: Text content is " + noteText.toString());
       }else if (Intent.ACTION_SEND.equals(action) && mExtras != null && mExtras.containsKey(Intent.EXTRA_STREAM)) {
            Uri uri = (Uri) mExtras.getParcelable(Intent.EXTRA_STREAM);
            QNDev.log(TAG+"uri = "+uri.toString());
            String text = QNUtil.readTextPlainUri(uri, QNCreateNote.this);
            if (null == text ) {
               return;
            }
            QNDev.log(TAG+"text = "+text);
            noteText.append(text);

            QNDev.log("Intent.ACTION_SEND extra -EXTRA_STREAM: Text content is " + noteText.toString());
        } else if(mExtras != null && mExtras.containsKey("CLIPBOARD")){
            noteText.append(mExtras.getString("CLIPBOARD"));           
        }
        
        Intent newIntent = new Intent(QNConstants.INTENT_ACTION_NEW);
        newIntent.putExtra("from","textShare");
        newIntent.setClass(this, QNNoteView.class);
        newIntent.setPackage(QNConstants.PACKAGE);
        String mimeType = "text/plain";

        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            QNUtil.initDirs(this);

            if(!QNUtil.checkStorageCard(this)) {
                finish();
                return;
            } else {
                try {
                    Uri filePathUri = QNUtil.prepareTextNote();
                    QNDev.log("QNCreateNote: created text file:" + filePathUri);
                    QNContent qnc = QNContent_Factory.Create(mimeType, filePathUri);
                    ((QNContent_Text)qnc).createOrUpdateTextFiles(noteText.toString());
                    newIntent.setDataAndType(filePathUri, mimeType);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT);
                    finish();
                    return;
                }
            }
        } else {
            noteUri = Uri.parse("qntext:" + noteText.toString());
            newIntent.setDataAndType(noteUri, mimeType);
        }

        startActivity(newIntent);    
        finish();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mIntent  = getIntent();
        if (mIntent == null) return;

        QNDev.qnAssert(null != mIntent);
        mExtras = mIntent.getExtras();

        doAction();
    }

}
