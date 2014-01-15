/*
/* Copyright (C) 2009, Motorola India, Inc.,
/* All Rights Reserved
/* Class name: PhoneNumberUtilsExtension.java
/* Description: This class format number depending up on flex settings.
/* In this calss we have flex check, if the flex is true we won't format the number.
/* If the flex is flase we will call the PhoneNumberUtils.formatNumber to format the Number
/* depending up on the locale settings.
/*
/*
/*
/* Modification History:
/**********************************************************
/* Date                Author          Comments
/* 17 Mar 2011         jrb768          Created
/**********************************************************
/*/

package com.android.contacts.spd;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
//import com.motorola.android.provider.MotorolaSettings;


public class PhoneNumberUtilsExt
{

    public static String formatNumber(Context context, String source) {

//        boolean hyphenStatus = MotorolaSettings.getInt(context.getContentResolver(),
//                                            "hyphenation_feature_enabled", 0)!= 1 ? false : true;
//        if(hyphenStatus){
//            return source;
//        }
//        else {
            return PhoneNumberUtils.formatNumber(source);
//        }
    }
}
