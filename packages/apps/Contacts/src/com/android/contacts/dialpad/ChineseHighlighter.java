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

import com.android.contacts.dialpad.HanziToPinyin;
import com.android.contacts.dialpad.HanziToPinyin.Token;

//import com.android.providers.contacts.HanziToPinyin;
//import com.android.providers.contacts.HanziToPinyin.Token;

import android.util.Log;

import java.lang.Character;
import java.util.ArrayList;

public class ChineseHighlighter extends Highlighter{
    static final int CJK_START_CHAR = 0x4E00;
    static final int CJK_END_CHAR = 0x9FA5;

    ArrayList<Integer> sepIndexList;    //List to save separators
    boolean  isFirst;   //whether the first time traversal the name string.
    private int sepIndex;  //used for sepIndexArray array.

    //MOT Calling Code - IKPIM-598
    private static ChineseHighlighter mChineseHighlighter = null;
    private ChineseHighlighter(){
    }
    public static ChineseHighlighter getInstance(){
        if (null == mChineseHighlighter) {
            mChineseHighlighter = new ChineseHighlighter();
        }
        return mChineseHighlighter;
    }
    //END MOT Calling Code - IKPIM-598

    public void nameHighlight(Spannable name, String matchStr, int color,
                    int text_color) {
        String str = name.toString().toUpperCase();
        String matchedStr = matchStr.toUpperCase();
        int len = matchedStr.length();
        int size = str.length();

        if(len == 0) return; //MOT Calling Code - IKMAIN-17655
        sepIndexList = new ArrayList<Integer>();
        isFirst = true;
        sepIndex = 0;
        recursivingHighlight(name, color, text_color, str, matchedStr, len, size);
    }

	private void recursivingHighlight(Spannable name, int color,
			int text_color, String str, String matchedStr, int len, int size) {
        StringBuilder acronymStrB = new StringBuilder(size);
        StringBuilder stripStrB = new StringBuilder(size * 10);

        int[] acroArray = new int[size];
        int[] stripArray = new int[size * 10];
        int[] acroStripArray = new int[size];
        boolean canAcro = true;

        int acroIndex = 0;
        int stripIndex = 0;
        int pos = 0;
        int curSepIndex = 0;
        if (!isFirst) {
            curSepIndex= sepIndexList.get(sepIndex);
            if (curSepIndex >= size) return;
        }
        String invertName = str.substring(curSepIndex) + str.substring(0, curSepIndex);

        for (int i = 0; i < size; i++) {
            char c = invertName.charAt(i);

            if ( (c >= CJK_START_CHAR) && (c <= CJK_END_CHAR)) {
                // if the char is Chinese char, then convert it to pinyin
                // only considering the standard pinyin
                ArrayList<Token> tokens = HanziToPinyin.getInstance().get(String.valueOf(c));
                if (tokens.size() >0){
                    Token token = tokens.get(0);
                    String pinyinChar = token.target;
                    char ch;
                    // if canAcro is true, then this Chinese is separator;
                    // so need to record its 1st char of pinyin string
                    ch = pinyinChar.charAt(0);
                    acronymStrB.append(ch);
                    acroArray[acroIndex] = i;
                    acroStripArray[acroIndex] = stripIndex;
                    acroIndex++;

                    // if this character covert to pinyin successfully,
                    // append it to the stripped pinyinName string;
                    // record the original position of each char of this pinyin string
                    // in the original name string for the stripped full pinyin name string

                    int length = pinyinChar.length();
                    for (int j = 0; j < length; j++) {
                        ch = pinyinChar.charAt(j);
                        stripStrB.append(ch);
                        stripArray[stripIndex] = i;
                        stripIndex++;
                    }
                    if (isFirst) sepIndexList.add(i);
                    canAcro = true;
                }
            } else if ((c == ' ') || (c =='.') || (c == ',')) { // MOT Calling Code - IKMAIN-18641
                // ", . space" are considered as separator
                if (isFirst) sepIndexList.add(i);
                canAcro = true;
            } else if  (Character.isLetterOrDigit(c)) { // MOT Calling Code - IKMAIN-18641
                // only "A~Z,a~z,0~9" are valid char to compare, all other symbols will ignore
                // for the stripped full pinyin name string
                //MOT Calling Code - IKPIM-598
                //also support compare those extended characters.
                //use normalize to parse them.
                char NormalizedCh = coreLang2Alpha(c);
                stripStrB.append(NormalizedCh);
                //END MOT Calling Code - IKPIM-598
                stripArray[stripIndex] = i;

                if (canAcro) {
                    if (isFirst) sepIndexList.add(i);
                    acronymStrB.append(NormalizedCh);//MOT Calling Code - IKPIM-598
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
        String acronymStr = acronymStrB.toString();
        String stripStr = stripStrB.toString();

        //MOT Calling Code - IKPIM-598
        if(SmartDialerUtil.noAlphaChar(matchedStr)){ // MOT Calling Code - IKMAIN-18641
            acronymStr = PhoneNumberUtils.convertKeypadLettersToDigits(acronymStr);
            stripStr = PhoneNumberUtils.convertKeypadLettersToDigits(stripStr);
        }
        //END MOT Calling Code - IKPIM-598

        pos = 0;
        // compare acronymStr firstly
        while ((pos < acronymStr.length())
                  && !acronymStr.regionMatches(pos, matchedStr, 0, len)) {
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
            // compare stripStr then
            pos = 0;
            while ( (pos < acronymStr.length()) &&
                     !stripStr.regionMatches(acroStripArray[pos], matchedStr, 0, len)) {
                pos++;
            }
            if (pos < acronymStr.length()) {
                //MOT Calling Code - IKPIM-598
                try {
                    // matched in the stripStr, and set styledname
                    int startPos = acroArray[pos];
                    int endPos = stripArray[acroStripArray[pos] + len - 1] + 1;
                    if (endPos > size - curSepIndex) {
                        name.setSpan(new BackgroundColorSpan(color), 0, endPos -(size - curSepIndex), 0);
                        name.setSpan(new ForegroundColorSpan(text_color), 0, endPos -(size - curSepIndex), 0);
                        name.setSpan(new BackgroundColorSpan(color), startPos + curSepIndex, size, 0);
                        name.setSpan(new ForegroundColorSpan(text_color), startPos + curSepIndex, size, 0);
                    } else {
                        name.setSpan(new BackgroundColorSpan(color), startPos, endPos, 0);
                        name.setSpan(new ForegroundColorSpan(text_color), startPos, endPos, 0);
                    }
                } catch (IndexOutOfBoundsException e) {
                    // IndexOutOfBoundsException
                }
                //END MOT Calling Code - IKPIM-598
                return;
            } else if (sepIndex <= SAFE_RECURSE_TIMES && sepIndex < sepIndexList.size() - 1) {
            	sepIndex ++;
                recursivingHighlight(name, color, text_color, str, matchedStr, len, size);
            } else return;
        }
        //isFirst = false;
    }
}