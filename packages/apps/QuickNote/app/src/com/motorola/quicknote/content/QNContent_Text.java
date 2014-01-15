/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2010 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * w21782                    01/13/2010                  override getWidgetImage().
 * w21782                    01/04/2010                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote.content;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
//import android.util.Log;

import com.motorola.quicknote.QNActivity;
import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.QNUtil;
import com.motorola.quicknote.QNSetReminder;
import com.motorola.quicknote.R;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

public class QNContent_Text extends QNContent
{
    private final static String TAG = "QNContent_Text";
    
    private EditText        _et     = null;
    private QNActivity      _owner  = null;
    private int             _reqcode;
    
    QNContent_Text(String mimetype, Uri uri) {
        super(mimetype, uri);
//        QNDev.qnAssert(uri.getScheme().equals("qntext"));
        
    }

    /************************
     * Implement QNContent_Interface
     */
    @Override
    public boolean close() {
        return true;
    }
    
    /* return true: content is really updated
                  false: content is not update because content same as before
      */
    @Override
    public boolean trigger_update() {
        if( null != _et) {
            String newContent = _et.getText().toString();
            if(getTextNoteContent().equals(newContent)) {
           //     _owner.force_onActivityResult(_reqcode, Activity.RESULT_CANCELED, null);
            } else {
                if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                    createOrUpdateTextFiles(newContent);
                } else {
                    Intent i = new Intent();
                    i.setData(Uri.parse("qntext:" + newContent));
             //       _owner.force_onActivityResult(_reqcode, Activity.RESULT_OK, i);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public NotestateE state() { return NotestateE.IDLE; }

    @Override
    public SharingTypeE[] shared_by() {
        SharingTypeE[] st = new SharingTypeE[2];
        st[0] = SharingTypeE.MESSAGE;
        st[1] = SharingTypeE.EMAIL;
        return st;
    }

    @Override
    public boolean isTitle() {
        return true;
    }

    @Override
    public boolean isBGColor() {
        return true;
    }

    
    @Override
    public View noteView(Context context, ViewGroup parent) {
        CharSequence text = getTextNoteContent();
        _et = (EditText)parent.findViewById(R.id.edit_text_content);
        _et.setText(text);
        TextView tv = (TextView)parent.findViewById(R.id.detail_text_content);
        tv.setText(text);

        return tv;
    }
    
    @Override
    public Bitmap widgetBitmap(Context context, int width, int height) { 
        Bitmap widgetBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        widgetBitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(widgetBitmap);
        TextView tv = new TextView(context);
        tv.setTextColor(Color.BLACK);
        tv.layout(0, 0, width, height);
        tv.setText(getTextNoteContent());
        tv.draw(canvas);
        return widgetBitmap;
    }


    @Override
    public Bitmap widgetBitmap(Context context, int width, int height, int bgcolor)
    {
       return widgetBitmap(context, width, height);
    }
   
    @Override
    public CharSequence getTextNoteContent()
    {
        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            StringBuffer textContent = new StringBuffer();
            QNDev.log("QNContent_Text: uri=" + _uri);
            // read file content from SD card
            if (_uri != null && _uri.getScheme().equals("file")) {
                File file=new File(_uri.getPath());
                BufferedReader bufReader = null;
                FileReader fileReader = null;
//                FileInputStream in = null;
                try {
                    fileReader =new FileReader(file);
                    bufReader=new BufferedReader(fileReader);
                    String line = null;
                    if((line=bufReader.readLine()) != null) {
                        textContent.append(line);
                    }
                    while((line=bufReader.readLine()) != null) {
                        textContent.append("\n"); 
                        textContent.append(line);
                    }
/*
                    in = new FileInputStream(_uri.getPath());
                    byte[] tempBytes = new byte[1024];  
                    int byteRead = 0;
                    while ((byteRead = in.read(tempBytes)) != -1) {  
                        textContent.append(new String(tempBytes, 0, byteRead));  
                    }  
*/
                } catch (IOException d) {
                    QNDev.log(d.toString());  
                } finally {  
/*                 if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e1) {}  
                    }
*/
                    if (bufReader != null) {
                        try {
                            bufReader.close();
                        } catch (IOException e1) {}  
                    }
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e1) {}  
                    }
                }  
            }
            return textContent.toString();
        } else {
            // cut the "qntext:" prefix
           return uri().toString().substring(7);
        }
    }


    /* for widget and thumbnails, no need to get the whole text content
       get the maxlenth = 100 chars
     */
    @Override
    public CharSequence getTextNoteContentShort()
    {
        int maxLength = 100; //get 100 chars at most

        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            char[] buffer = new char[maxLength];
            QNDev.log("QNContent_Text: uri=" + _uri);
            // read file content from SD card
            if (_uri != null && _uri.getScheme().equals("file")) {
                File file=new File(_uri.getPath());
                BufferedReader bufReader = null;
                FileReader fileReader = null;
                try {
                    fileReader =new FileReader(file);
                    bufReader=new BufferedReader(fileReader, maxLength);

                    bufReader.read(buffer, 0, maxLength);
                } catch (IOException d) {
                    QNDev.log(d.toString());
                } finally {
                    if (bufReader != null) {
                        try {
                            bufReader.close();
                        } catch (IOException e1) {}
                    }
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e1) {}
                    }
                }
            }
            CharSequence seq = new String(buffer);
            return seq;
        } else {
            // cut the "qntext:" prefix
           return uri().toString().substring(7);
        }
    }



    /* fileName: /sdcard/quicknote/text/2010-03-19_16-43-41_961.txt
      * uri:  qntext: content of text note
      */
    public boolean createOrUpdateTextFiles(String fileContent) {
        BufferedWriter bufWriter = null;
        FileWriter fileWriter = null;
        File writeFile = null;
        try
        {
            writeFile=new File(_uri.getPath());
            if(!writeFile.exists())
            {
                writeFile.createNewFile();   
            }
            fileWriter = new FileWriter(writeFile, false);
            bufWriter=new BufferedWriter(fileWriter);
            fileWriter.write(fileContent);
            fileWriter.flush();

            return true;
        } catch (IOException d) {
            QNDev.log(d.toString());
            return false;
        } finally {  
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e1) {}  
            }
            if (bufWriter != null) {
                try {
                    bufWriter.close();
                } catch (IOException e1) {}  
            }
        }
    }

}
