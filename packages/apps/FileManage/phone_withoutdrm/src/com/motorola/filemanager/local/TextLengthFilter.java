package com.motorola.filemanager.local;

import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;

public class TextLengthFilter implements InputFilter {
    private Handler mHandler;
    private EditText mEt;
    final int max_length = 255;

    TextLengthFilter(Handler h, EditText et) {
	mHandler = h;
	mEt = et;
    }

    public CharSequence filter(CharSequence source, int start, int end,
	    Spanned dest, int dstart, int dend) {
	// TODO Auto-generated method stub
	int destLen = getCharacterNum(dest.toString());

	int sourceLen = getCharacterNum(source.toString());
	if (destLen + sourceLen > max_length) {
	    Message m = mHandler.obtainMessage(
		    FileManagerActivity.MESSAGE_SHAKE_TEXT, mEt);
	    mHandler.sendMessage(m);

	    return "";
	}
	return source;
    }

    public static int getCharacterNum(final String content) {
	if (null == content || content.length() == 0) {
	    return 0;
	} else {
	    return (content.length() + getChineseNum(content));
	}

    }

    public static int getChineseNum(String s) {
	int num = 0;
	char[] myChar = s.toCharArray();
	for (int i = 0; i < myChar.length; i++) {
	    if ((char) (byte) myChar[i] != myChar[i]) {
		num++;
	    }
	}
	return num;
    }

}
