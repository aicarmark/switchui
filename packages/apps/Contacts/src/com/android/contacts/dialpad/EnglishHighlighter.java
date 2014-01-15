/*
 * Copyright (C) 2010, Motorola, Inc,
 *
 * This file is derived in part from code issued under the following license.
 * Changed for MOTO UI requirements
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author khqn86
 */

//MOT calling code- IKPIM-447
package com.android.contacts.dialpad;

import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;//MOT Calling code - IKSTABLETWOV-2767
import android.util.Log;
import android.widget.TextView;

import java.lang.Character;
import java.util.ArrayList;

public class EnglishHighlighter extends Highlighter {
    //MOT Calling Code IKPIM-598
    private static EnglishHighlighter mEnglishHighlighter= null;

    ArrayList<Integer> sepIndexList;    //List to save separators
    boolean  isFirst;   //whether the first time traversal the name string.
    private int sepIndex;  //used for sepIndexArray array.

    private EnglishHighlighter(){
    }
    public static EnglishHighlighter getInstance(){
        if (null == mEnglishHighlighter) {
            mEnglishHighlighter = new EnglishHighlighter();
        }
        return mEnglishHighlighter;
    }
    // END MOT Calling Code IKPIM-598

    public void nameHighlight(Spannable name, String matchStr, int color,
                int text_color) {
        String str = name.toString();
        String orig = str.toUpperCase();
        String pat =  matchStr.toUpperCase();
        int len = pat.length();
        int size = str.length(); //length of orignal string.
        int size1 = size; //length of parsed orignal string.

        if(len == 0) return;//MOT Calling Code - IKMAIN-17655

        //MOT Calling Code - IKPIM-598
        //filter those character(æ œ ß) which can not be normalized,
        //and replace with ae,oe,ss,
        int[] specialArray = new int[size];
        int specialIndex = 0;

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if((ch == '\u00DF') || (ch == '\u0153') || (ch == '\u00E6') || (ch == '\u00c6') || (ch == '\u0152')){
                specialArray[specialIndex] = i+1;
                specialIndex++;
            }
        }
        if(specialArray[0] != 0){
            String tmpStr;
            tmpStr = ((((str.replace("\u00DF", "SS")).replace("\u00E6", "AE")).replace("\u0153", "OE")).replace("\u00c6", "AE")).replace("\u0152", "OE");
            orig = tmpStr.toUpperCase();
            size1 = orig.length();
        }
        //END MOT Calling Code - IKPIM-598

        sepIndexList = new ArrayList<Integer>();
        isFirst = true;
        sepIndex = 0;
        recursivingHighlight(name, color, text_color, str, pat, len, size, size1, specialArray);
    }

	private void recursivingHighlight(Spannable name, int color,int text_color,
			String orig, String pat, int len, int size, int size1,int[] specialArray) {

		StringBuilder acronymStrB = new StringBuilder(size);
        int[] acroArray = new int[size1];
        int[] stripArray = new int[size1];
        int[] acroStripArray = new int[size1];

        StringBuilder stripStrB = new StringBuilder(size1);
        int startPos = -1;
        int endPos = -1;
        boolean canAcro = true;

        int acroIndex = 0;
        int stripIndex = 0;
        int pos = 0;
        int curSepIndex = 0;
        if (!isFirst) {
            curSepIndex= sepIndexList.get(sepIndex);
            if (curSepIndex >= size) return;
        }
        String invertName = orig.substring(curSepIndex) + orig.substring(0, curSepIndex);
        for (int i = 0; i < size1; i++) {
            char c = invertName.charAt(i);
            if ((c == ' ') || (c == '.') || (c == ',')) { //MOT Calling Code - IKMAIN-18641
                // ", . space" are considered as separator
            	if (isFirst) sepIndexList.add(i);
                canAcro = true;
            } else if (Character.isLetterOrDigit(c)) { //MOT Calling Code - IKMAIN-18641
                // only "A~Z,a~z,0~9" are valid char to compare, all other symbols will ignore
                // MOT Calling Code - IKPIM-598
                char NormalizedCh = coreLang2Alpha(c);
                stripStrB.append(NormalizedCh);
                // END MOT Calling Code - IKPIM-598
                stripArray[stripIndex] = i;
                if (canAcro || (size1 - curSepIndex == i && curSepIndex > 0)) {
                	if (isFirst) sepIndexList.add(i);
                	acronymStrB.append(NormalizedCh);
                	acroArray[acroIndex] = i;
                    acroStripArray[acroIndex] = stripIndex;
                    acroIndex++;
                }
                stripIndex++;
                canAcro = false;
            }
            // all the other char is special char, which to be stripped
        } //end of for clause

        isFirst = false;
        if (!isFirst) acronymStrB.append(coreLang2Alpha(invertName.charAt(0)));
        String acronymStr = acronymStrB.toString();
        String stripStr = stripStrB.toString();

        if(SmartDialerUtil.noAlphaChar(pat)){ // MOT Calling Code - IKMAIN-18641
        	acronymStr = PhoneNumberUtils.convertKeypadLettersToDigits(acronymStr);
        	stripStr = PhoneNumberUtils.convertKeypadLettersToDigits(stripStr);
        }

        pos = 0;
        // compare acronymStr firstly
        while ((pos < acronymStr.length())
                  && !acronymStr.regionMatches(pos, pat, 0, len)) {
            pos++;
        }

        if (pos < acronymStr.length()) {
            // matched in the acronymStr, and set styledname
            BackgroundColorSpan markupArray[] = new BackgroundColorSpan[len];
            ForegroundColorSpan makeupTextArray[] = new ForegroundColorSpan[len];

            //MOT Calling Code - IKPIM-598
            try {
                for (int i =0; i < len; i++) {
                    markupArray[i] = new BackgroundColorSpan(color);
                    makeupTextArray[i] = new ForegroundColorSpan(text_color);
                    int offset = size - curSepIndex;
                    if (acroArray[pos] + curSepIndex + 1 <= size) {
                        name.setSpan(markupArray[i], acroArray[pos] + curSepIndex, acroArray[pos] + curSepIndex + 1, 0);
                        name.setSpan(makeupTextArray[i], acroArray[pos] + curSepIndex, acroArray[pos] + curSepIndex + 1, 0);
                    } else {
                        name.setSpan(markupArray[i], acroArray[pos]  - offset, acroArray[pos] - offset + 1, 0);
                        name.setSpan(makeupTextArray[i], acroArray[pos]  - offset, acroArray[pos] - offset + 1, 0);
                    }
                    pos++;
                }
            } catch(IndexOutOfBoundsException e) {
                // IndexOutOfBoundsException
            }
            return;
                //END MOT Calling Code - IKPIM-598
        } else {
    		pos = 0;
	        while ((pos < acroIndex) &&!stripStr.regionMatches(acroStripArray[pos], pat, 0, len)) {
	            pos++;
	        }

	        if (pos < acroIndex) {
	            //MOT Calling Code - IKPIM-598
	            startPos = acroArray[pos];
	            endPos = stripArray[acroStripArray[pos] + len - 1] + 1;
	
	            for(int i=0;i<size;i++) {
	                if (specialArray[i] == 0) break;
	                if (specialArray[i] < startPos)  startPos--;
	                if (specialArray[i] < endPos)  endPos--;
	            }
	            try {
	                // matched in the stripStr, and set styledname
		            if (endPos > size1 - curSepIndex){
		            	name.setSpan(new BackgroundColorSpan(color), 0, endPos -(size1 - curSepIndex), 0);
		                name.setSpan(new ForegroundColorSpan(text_color), 0, endPos -(size1 - curSepIndex), 0);
		                name.setSpan(new BackgroundColorSpan(color), startPos + curSepIndex, size1, 0);
		                name.setSpan(new ForegroundColorSpan(text_color), startPos + curSepIndex, size1, 0);
		            } else {
		            	name.setSpan(new BackgroundColorSpan(color), startPos, endPos, 0);
		                name.setSpan(new ForegroundColorSpan(text_color), startPos, endPos, 0);
		            }
	            }catch(IndexOutOfBoundsException e) {
	                // IndexOutOfBoundsException
	            }
	            //END MOT Calling Code - IKPIM-598
                return;
            } else if (sepIndex <= SAFE_RECURSE_TIMES && sepIndex < sepIndexList.size() - 1) {
            	sepIndex ++;
                recursivingHighlight(name, color, text_color, orig, pat, len, size, size1, specialArray);
            } else return;
        }
	}
}
