package com.motorola.mmsp.rss.util;

import android.content.Context;
import android.util.Log;

import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.R;


public class SettingsUtil {
	private static final String TAG = "SettingsUtil"; 

	public static String longTimeToStringTime(Context context, long time){
		String timeShow = "";
		if(time == RssConstant.Value.ONE_HOUR){
			timeShow = context.getResources().getString(R.string.onehour);
		} else if(time == RssConstant.Value.THREE_HOURS){
			timeShow = context.getResources().getString(R.string.threehours);
		}else if(time == RssConstant.Value.SIX_HOURS){
			timeShow = context.getResources().getString(R.string.sixhours);
		}else if(time == RssConstant.Value.TWELVE_HOURS){
			timeShow = context.getResources().getString(R.string.twelvehours);
		}else if(time == RssConstant.Value.ONE_DAY){
			timeShow = context.getResources().getString(R.string.oneday);
		}else if(time == RssConstant.Value.TWO_DAYS){
			timeShow = context.getResources().getString(R.string.twodays);
		}else if(time == RssConstant.Value.THREE_DAYS){
			timeShow = context.getResources().getString(R.string.threedays);
		}else if(time == RssConstant.Value.ONE_WEEK){
			timeShow = context.getResources().getString(R.string.oneweek);
		}else if(time == RssConstant.Value.ONE_MONTH){
			timeShow = context.getResources().getString(R.string.onemonth);
		}
		return "";
	}
	
	public static long stringTimeToLongTime(String s){
		return 0;
	}
	
	public static int showNameToindex(Context context, String appear){
		int index = 0;
		Log.d(TAG, "appear = " + appear);
		if(appear.equals(context.getResources().getString(R.string.oneday))){
			index = RssConstant.Content.SHOW_ITEMS_ONE_DAY;
		}else if(appear.equals(context.getResources().getString(R.string.twodays))){
			index = RssConstant.Content.SHOW_ITEMS_TWO_DAYS;
		}else if(appear.equals(context.getResources().getString(R.string.threedays))){
			index = RssConstant.Content.SHOW_ITEMS_THREE_DAYS;
		}else if(appear.equals(context.getResources().getString(R.string.oneweek))){
			index = RssConstant.Content.SHOW_ITEMS_ONE_WEEK;
		}else if(appear.equals(context.getResources().getString(R.string.onemonth))){
			index = RssConstant.Content.SHOW_ITEMS_ONE_MONTH;
		}
		return index;
	}
	
	public static int updateNameToindex(Context context, String appear){
		int index = 0;
		if(appear.equals(context.getResources().getString(R.string.oneday))){
			index = RssConstant.Content.UPDATE_FRE_ONE_DAY;
		}else if(appear.equals(context.getResources().getString(R.string.onehour))){
			index = RssConstant.Content.UPDATE_FRE_ONE_HOUR;
		}else if(appear.equals(context.getResources().getString(R.string.threehours))){
			index = RssConstant.Content.UPDATE_FRE_THREE_HOURS;
		}else if(appear.equals(context.getResources().getString(R.string.sixhours))){
			index = RssConstant.Content.UPDATE_FRE_SIX_HOURS;
		}else if(appear.equals(context.getResources().getString(R.string.twelvehours))){
			index = RssConstant.Content.UPDATE_FRE_TWELVE_HOURS;
		}else if(appear.equals(context.getResources().getString(R.string.never))){
			index = RssConstant.Content.UPDATE_FRE_NEVER;
		}
		return index;
	}
	
	public static String indexToShowName(Context context, int index){
		String string = "";
		switch(index){
		case RssConstant.Content.SHOW_ITEMS_ONE_DAY:
			string =  context.getResources().getString(R.string.oneday);
			break;
		case RssConstant.Content.SHOW_ITEMS_TWO_DAYS:
			string =  context.getResources().getString(R.string.twodays);
			break;
		case RssConstant.Content.SHOW_ITEMS_THREE_DAYS:
			string =  context.getResources().getString(R.string.threedays);
			break;
		case RssConstant.Content.SHOW_ITEMS_ONE_WEEK:
			string =  context.getResources().getString(R.string.oneweek);
			break;
		case RssConstant.Content.SHOW_ITEMS_ONE_MONTH:
			string =  context.getResources().getString(R.string.onemonth);
			break;
		}
		return string;
	}
	public static String indexToUpdateName(Context context, int index){
		String string = "";
		switch(index){
		case RssConstant.Content.UPDATE_FRE_ONE_DAY:
			string =  context.getResources().getString(R.string.oneday);
			break;
		case RssConstant.Content.UPDATE_FRE_ONE_HOUR:
			string =  context.getResources().getString(R.string.onehour);
			break;
		case RssConstant.Content.UPDATE_FRE_THREE_HOURS:
			string =  context.getResources().getString(R.string.threehours);
			break;
		case RssConstant.Content.UPDATE_FRE_SIX_HOURS:
			string =  context.getResources().getString(R.string.sixhours);
			break;
		case RssConstant.Content.UPDATE_FRE_TWELVE_HOURS:
			string =  context.getResources().getString(R.string.twelvehours);
			break;
		case RssConstant.Content.UPDATE_FRE_NEVER:
			string =  context.getResources().getString(R.string.never);
			break;
		}
		return string;
	}
	public static long indexToUpdateFrequency(int index){
		long update_frequency = 0;
		switch(index){
		case RssConstant.Content.UPDATE_FRE_ONE_HOUR:
			update_frequency = RssConstant.Value.ONE_HOUR;
			break;
		case RssConstant.Content.UPDATE_FRE_THREE_HOURS:
			update_frequency = RssConstant.Value.THREE_HOURS;
			break;
		case RssConstant.Content.UPDATE_FRE_SIX_HOURS:
			update_frequency = RssConstant.Value.SIX_HOURS;
			break;
		case RssConstant.Content.UPDATE_FRE_TWELVE_HOURS:
			update_frequency = RssConstant.Value.TWELVE_HOURS;
			break;
		case RssConstant.Content.UPDATE_FRE_ONE_DAY:
			update_frequency = RssConstant.Value.ONE_DAY;
			break;
		}
		return update_frequency;
	}
	public static long indexToShowItemFor(int index){
		long showItemfor = 0;
		switch(index){
		case RssConstant.Content.SHOW_ITEMS_ONE_DAY:
			showItemfor = RssConstant.Value.ONE_DAY;
			break;
		case RssConstant.Content.SHOW_ITEMS_TWO_DAYS:
			showItemfor = RssConstant.Value.TWO_DAYS;
			break;
		case RssConstant.Content.SHOW_ITEMS_THREE_DAYS:
			showItemfor = RssConstant.Value.THREE_DAYS;
			break;
		case RssConstant.Content.SHOW_ITEMS_ONE_WEEK:
			showItemfor = RssConstant.Value.ONE_WEEK;
			break;
		case RssConstant.Content.SHOW_ITEMS_ONE_MONTH:
			showItemfor = RssConstant.Value.ONE_MONTH;
			break;
		}
		return showItemfor;
	}
}
