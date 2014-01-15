package com.motorola.mmsp.rss.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.util.Log;

public class DateTimeUtil {
	static private String TAG = "DateTimeUtil";
	public static Date getDate(String formattedString){
		log(formattedString);
		String patterRrfc822 = "EEE, d MMM yyyy HH:mm:ss";		
		String patterChina = "yyyy-MM-dd HH:mm:ss";
		String isoPatten = "[0-9]{4}-[0-9]{2}-[0-9]{2}[T|\\s*][0-9]{2}:[0-9]{2}:[0-9]{2}(?s).*";
		Date date = null;
		SimpleDateFormat sdf = null;
		if(formattedString.matches(isoPatten)){
			formattedString = isoToChinaFormat(formattedString);
			log(formattedString);
		}
		int index = formattedString.lastIndexOf(" ");
		String s = formattedString.substring(index + 1);		
		String timeZone = s;
		if(!s.startsWith("GMT")){
			timeZone = "GMT";
			timeZone += s;
		}
		log("timeZone = " + timeZone);		
		TimeZone zone = TimeZone.getTimeZone(timeZone);
		log("The display name of Zone is " + zone.getDisplayName());
		sdf = new SimpleDateFormat(patterRrfc822, Locale.US);
		sdf.setTimeZone(zone);
		try {
			date = sdf.parse(formattedString);			
		} catch (ParseException e) {
			log("The time format does not match " + patterRrfc822);
			sdf = new SimpleDateFormat(patterChina);
			sdf.setTimeZone(zone);
			try {
				date = sdf.parse(formattedString);
			} catch (ParseException e1) {
				log("The time format does not match " + patterChina);
			}
		}catch(IllegalArgumentException ie1){
			
		}finally{			
			if(date == null){
				log("Can not parse this time format , so return the current time!");
				date = new Date(System.currentTimeMillis());
			}
		}
		return date;
	}
	
	private static String isoToChinaFormat(String iso){
		String chinaFormat = null;
		String date = iso.substring(0, 10);
		String time = iso.substring(11, 19);
		date += " ";
		date += time;
		chinaFormat = date;
		return chinaFormat;
	}
	private static void log(String out){
//		Log.d(TAG, out);
	}
}
