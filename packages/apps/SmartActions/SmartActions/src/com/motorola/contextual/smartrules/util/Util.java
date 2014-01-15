/*
 * @(#)Util.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.actions.CpuPowerSaverSupport;
import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity;
import com.motorola.contextual.smartrules.app.WebViewActivity;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;
import com.motorola.contextual.virtualsensor.locationsensor.AppPreferences;


/** This class hosts a number of distinct and various utility routines.
 *
 *<code><pre>
 * CLASS:
 * 	no extends, no implements
 *
 * RESPONSIBILITIES:
 * 	This class is entirely utility routines, all static, nothing instance-based.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  See individual routines.
 *</pre></code>
 */
public class Util implements Constants, DbSyntax {

    private static final String TAG = Util.class.getSimpleName();    
    private interface TIME_CONST{
                int NUM_DAYS = 7;
                int NUM_HOURS = 24;
                int NUM_MINS = 60;
                int NUM_SECS = 60;
                int NUM_MS = 1000;
                }
    
    /** returns a string for an Array of Long.
     *
     * @param array - array of Long to convert to string
     * @return - string value of each element, comma separated
     */
    public static String toCommaDelimitedString(Long[] array) {
        String result ="";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            buf.append(array[i].longValue()+COMMA_SPACE);
        }
        result = buf.toString();
        if (result.endsWith(COMMA_SPACE))
            result = result.substring(0, result.length()-COMMA_SPACE.length());
        return result;
    }

    /** builds a comma-separated list from an array, with no comma after the last value.
     *
     * @param commaSepString - the array to separate with commas
     * @return - comma separated string.
     */
    public static String toCommaDelimitedString(final String[] commaSepString) {

        return 	toCommaDelimitedString(commaSepString, false);
    }

    /** builds a comma-separated list from an array.
     *
     * @param commaSepString - the array to separate with commas
     * @param commaAfterLastString - if true, last element of the array will be followed by
     *    a comma, else it will not be followed by a comma.
     * @return - comma separated string.
     */
    public static String toCommaDelimitedString(final String[] commaSepString,
            boolean commaAfterLastString) {

        String result = "";

        if (commaSepString != null) {
            for (String s: commaSepString) {
                result = result.concat(s.concat(COMMA_SPACE));
            }
            if (!commaAfterLastString && result.endsWith(COMMA_SPACE))
                result = result.substring(0, result.length() - COMMA_SPACE.length());
        }
        if (LOG_DEBUG) Log.d(TAG, result);
        return result;
    }

    /** builds a comma-separated quoted list from an array, with no comma after the last value.
	 *
	 * @param commaSepString - the array to separate with commas
	 * @return - comma separated string with quotes around each string.
	 */
    public static String toCommaDelimitedQuotedString(final String[] commaSepString) {
    	
    	return toCommaDelimitedQuotedString(commaSepString, false);
    }
	 
    /** builds a comma-separated list from an array.
	 *
	 * @param commaSepString - the array to separate with commas
	 * @param commaAfterLastString - if true, last element of the array will be followed by
	 *    a comma, else it will not be followed by a comma.
	 *    
	 * @return - comma separated string with quotes around each string.
	 */
    public static String toCommaDelimitedQuotedString(final String[] commaSepString,
	           boolean commaAfterLastString) {
    	
    	String result = "";

    	if (commaSepString != null) {
    		for (String s: commaSepString) {
    			//elements of String array can be null
    			if(s!=null)
    				result = result.concat(Q).concat(s.concat(Q).concat(COMMA));
    		}
    		if (!commaAfterLastString && result.endsWith(COMMA))
    			result = result.substring(0, result.length() - COMMA.length());
    	}
    	if (LOG_DEBUG) Log.d(TAG, result);
    	return result;
    }
    
    /** Checks if a string is null or empty string or null string
     * 
     * @param input
     * @return  true or false
     */
    public static boolean isNull(String input){
    	boolean result = false;
    	if ( (input == null) || ((input != null) && ((input.length() == 0) || (input.equals("null") )))){
    		result = true;
    	}
    	return result;
    }


    /** returns the index in the array of the value.
     *
     * @param array - array to search
     * @param value - value to search for
     * @return - index of value found or -1 if not found.
     */
    public static int getIndex(Long[] array, long value) {

        int result = -1;
        for (int ix =0; ix<array.length; ix++) {
            if (array[ix].longValue() == value) {
                result = ix;
                break;
            }
        }
        return result;
    }
    /** Returns the long value corresponding to a particular key.
    *
    * @param key
    * @param defaultValue
    * @return
    */
    public static long getLongValue(String strValue, long defaultValue) {
        long value = defaultValue;
         if (strValue != null) {
            try {
                value = Long.parseLong(strValue);
            } catch(NumberFormatException e) {
                Log.w(TAG, "Exception when parsing " + strValue);
            }
        }
         return value;
    }

    // Constants used to construct the help uri
    private static final String HELP_URI_START = "http://www.motorola.com/hc/apps/help.aspx?app=";
    private static final String HELP_URI_VERSION = "&v=";
    private static final String HELP_URI_LOCALE = "&l=";
    
    /** construct the help uri and return to the caller
     * 
     * @param context - Context
     * @return - string that is the help URI like for example
     * 			 http://www.motorola.com/hc/apps/help.aspx?app=100&v=1.0&l=en-us
     */
    public static final String getHelpUri(Context context) {
    	
		String[] packageDetails = getPackageDetails(context);
		Locale locale = Locale.getDefault();
		String language = null;
		if(locale == null)
			language = Locale.ENGLISH.toString();
		else
			language = locale.toString();
		
    	StringBuilder builder = new StringBuilder();
    	builder.append(HELP_URI_START)
    			.append(packageDetails[1])
    			.append(HELP_URI_VERSION)
    			.append(packageDetails[0])
    			.append(HELP_URI_LOCALE)
    			.append(language);
    	String helpUri = builder.toString();

        //HACK: For Chinese language we use the web Uri as per client requirement.
        if(language.startsWith("zh"))
            helpUri = "http://www.motorola.com.cn/smartrules/help.asp?v=3.1&l=zh";

		if(LOG_DEBUG) Log.d(TAG, "helpUri returning as "+helpUri);
		
		DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT,
        		null, null, null,
                SMARTRULES_INTERNAL_DBG_MSG, null, HELP_LAUNCHED,
                Constants.PACKAGE, Constants.PACKAGE);
		
    	return helpUri;
    }
    
    /** fetches and returns the application version string defined in the Android Manifest.
     * 
     * @param context - application context
     * @return - application version string or null in case of exception;
     */
    public static final String[] getPackageDetails(Context context) {
    	ArrayList<String> list = new ArrayList<String>();
		try {
			list.add(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
			list.add(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).packageName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}	
		return list.toArray(new String[0]);
    }
    
    private static final String RULE_PUBLISHER_ACTION   		= "com.motorola.smartactions.intent.action.GET_INFO";
    private static final String RULE_PUBLISHER_CATEGORY = "com.motorola.smartactions.intent.category.RULE_PUBLISHER";
    private static final String RULE_PUBLISHER_META_DATA_PUBKEY = "com.motorola.smartactions.publisher_key";

    /*
     * Returns the List of ResolveInfo containing the Metadata
     */
    private static List<ResolveInfo> getPublisherMetaData(Context context, String intentAction, String intentCategory) {
        List<ResolveInfo> list = null;
        Intent mainIntent = new Intent(intentAction);
        mainIntent.addCategory(intentCategory);
        PackageManager pm = context.getPackageManager();
        if(!intentCategory.equals(RULE_PUBLISHER_CATEGORY)) {
            list = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);
        } else {
            list = pm.queryBroadcastReceivers(mainIntent, PackageManager.GET_META_DATA);
        }
        if(LOG_DEBUG) Log.d(TAG, "list is "+list.toString());
        return list;
    }

    /**
     * Get the package name for the publisher from Publisher Provider
     * @param context application context
     * @param rulePubKey Publisher key
     * @return Package name of the publisher key
     */
    public static final String getPackageNameForRulePublisher(Context context, String rulePubKey) {
        List<ResolveInfo> list = getPublisherMetaData(context, RULE_PUBLISHER_ACTION, RULE_PUBLISHER_CATEGORY);
        String pkgName = context.getPackageName();

        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            Bundle metaData = info.activityInfo.metaData;
            if(metaData == null) continue;
            String publisherKey = metaData.getString(RULE_PUBLISHER_META_DATA_PUBKEY);
            if(publisherKey != null && publisherKey.equals(rulePubKey)) {
               pkgName = info.activityInfo.packageName;
               break;
            }
        }
       return pkgName;
    }
    /** sends a message to the notification manager to display.
     *
     * @param context - context
     * @param content - content that needs to be displayed
     * @param modeName - name of the mode entering or leaving
     * @param showIcon - show the notification icon or not. true when arriving at a location and false otherwise
     */
    public static void sendMessageToNotificationManager(final Context context, int ruleIcon) {

        if(getSharedPrefStateValue(context, RULE_STATUS_NOTIFICATIONS_PREF, TAG)) {
        	if(LOG_DEBUG) Log.d(TAG, "User preference to show rule status in notifications is true");
	        // Decision was to only show the app icon in the notification bar and not the rule icon.
	        // Hence hardcoding it to the app icon here even though the rule icon is being passed from everywhere.
	        //if(ruleIcon < 1)
	        //	ruleIcon = R.drawable.sr_app_ic;
	        ruleIcon = R.drawable.stat_notify_sr_ongoing;
	
	        long when = System.currentTimeMillis();
	
	        String content = RulePersistence.getActiveRulesString(context);
	        if(LOG_DEBUG) Log.d(TAG, "Message content is "+content);
	        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        mNotificationManager.cancel(NOTIF_ID);
	
	        if(content != null) {
	            Notification notification = new Notification(ruleIcon, content, when);
	            notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
	            Intent launchIntent = new Intent(context, LandingPageActivity.class);
	            launchIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | 
	            							Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
	            notification.setLatestEventInfo(context, context.getString(R.string.active_smart_actions), content, contentIntent);
	            mNotificationManager.notify(NOTIF_ID, notification);
	        }
        } 
        else {
        	if(LOG_DEBUG) Log.d(TAG, "User preference to show rule status in notifications is false");
        	return;
        }
    }

    /** clears the ongoing notifications in the notification curtain.
     * 
     * @param context - context
     */
    public static void clearOnGoingNotifications(final Context context) {
    	NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIF_ID);
    }
    
    // Location Shared Preferences controlled via flex to show or hide the Location
    // related options and conditions.
    private static final String SHARED_PREF_NAME = "com.motorola.contextual.smartrules";
    private static final String UILOCATION_PREF = "uilocation";
    
    /** getter - returns the flexed value of the shared preference for Location
     * 
     * @param context - Context
     * @return - true if the location is hidden
     */
	public static boolean hideLocationTrigger(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        boolean hideLocation = sp.getBoolean(UILOCATION_PREF, false);

        Log.d(TAG, "Returning from hideLocationTrigger: "+hideLocation);
        return hideLocation;
	}
	
	/** getter - returns the flexed value of the shared preference for Location
     * 
     * @param context - Context
     * @return - true if the location is hidden
     */
	public static boolean isProcessorSpeedSupported(final Context context) {
        return CpuPowerSaverSupport.retrievePckgSpecificPref(context);
	}
    
    /** Setter - set the shared preference passed in (sharedPrefValue) to the value passed in.
     *
     * @param context - Context
     * @param sharedPrefValue - shared preference string for which the value is to be set
     * @param from - Application calling this setter.
     * @param state - true or false
     */
    public static void setSharedPrefStateValue(final Context context, final String sharedPrefString,
            final String from, boolean state) {
        SharedPreferences sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        sp.edit().putBoolean(sharedPrefString, state).commit();
        if(LOG_DEBUG) Log.d(TAG, "Changing:"+sharedPrefString+" to "+state+" from:"+from);
    }

    /** Getter - returns if the preference passed in (sharedPrefValue) is set to true or false.
     *
     * @param context - Context
     * @param sharedPrefValue - shared preference string for which the value is to be retrieved
     * @param from - Application calling this getter
     * @return - true or false.
     */
    public static boolean getSharedPrefStateValue(final Context context, final String sharedPrefString, final String from) {
        SharedPreferences sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        boolean sharedPrefValue = sp.getBoolean(sharedPrefString, true);
        if(LOG_DEBUG) Log.d(TAG, "Returning:"+sharedPrefString+" = "+sharedPrefValue+" to: "+from);
        return sharedPrefValue;
    }
    
    /** Getter - returns if the preference passed in (sharedPrefValue) is set to true or false.
    *
    * @param context - Context
    * @param sharedPrefValue - shared preference string for which the value is to be retrieved
    * @param from - Application calling this getter
    * @return - true or false; If the Preference is not set, false will be returned
    */
   public static boolean getSharedPrefValue(final Context context, final String sharedPrefString, final String from) {
       SharedPreferences sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
       boolean sharedPrefValue = sp.getBoolean(sharedPrefString, false);
       if(LOG_DEBUG) Log.d(TAG, "Returning:"+sharedPrefString+" = "+sharedPrefValue+" to: "+from);
       return sharedPrefValue;
   }

    /** makes copy of a String array
     * 
     * @param original - original String array
     * @return
     */
	public static final String[] copyOf(final String[] original) {
    	
    	if (original == null) throw new IllegalArgumentException("original array cannot be null");
    	
		String[] result = new String[original.length];		
		for (int i = 0; i < original.length; i++) {
			result[i] = new String(original[i].getBytes());
		}
		return result;
    }

	/**
	 * This method queries PackageManager for all the Activities with "category"
	 * meta-data. The returned activity's meta-data is compared with the input pubKey.
	 *
	 * @param ct - context
	 * @param pubKey - Publisher Key of the Action/Condition
	 * @param category - Category Meta-Data for the PackageManager query.<br>
	 *                  <li> For Actions: category = ActionTable.PkgMgrConstants.CATEGORY</li>
	 *                  <li> For Conditions = ConditionTable.PkgMgrConstants.CATEGORY</li>
	 * @return - true if the action/condition is available
	 */
    public static boolean checkForFeatureAvailability(Context ct, final String pubKey, final String category){

        boolean result = false;

        Intent mainIntent = new Intent(ACTION_GET_CONFIG, null);
        mainIntent.addCategory(category);
        PackageManager pm = ct.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);

        for( ResolveInfo info : list){
            Bundle metaData = info.activityInfo.metaData;

            if(metaData != null)
                result = pubKey.equals(metaData.getString(GENERIC_PUBLISHER_KEY));

                if(result) break;
        }

        return result;
    }
    
    /** copies a file from the input location to the output location.
     * 
     * @param absoluteInputFilePath - file to be copied
     * @param absoluteOutputFilePath - file to be copied to
     */
	public static void copyFile(final String absoluteInputFilePath,
			final String absoluteOutputFilePath) {
		if (LOG_DEBUG) Log.d(TAG, "Reading from: " + absoluteInputFilePath
					+ " Writing to: " + absoluteOutputFilePath);

		FileInputStream in = null;
		FileOutputStream out = null;

		try {
			in = new FileInputStream(absoluteInputFilePath);
			out = new FileOutputStream(absoluteOutputFilePath);

			byte[] buffer = new byte[8192];
			int readLen = 0;

			while ((readLen = in.read(buffer, 0, buffer.length)) != INVALID_KEY) {
				out.write(buffer, 0, readLen);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
    
    /** copies a file from the input file stream  to the output file stream.
     * 
     * @param in - file to be copied
     * @param out - file to be copied to
     */
    public static void copyFile(InputStream in, OutputStream out) throws IOException {  
        byte[] buffer = new byte[1024];  
        int read;  
        while((read = in.read(buffer)) != -1){  
        	if (LOG_DEBUG) Log.d(TAG, "Copying bytes " + read);
          out.write(buffer, 0, read);  
        }  
        if (LOG_INFO) Log.d(TAG, "File Copied succefully from assets to data folder");
    }

    /*
     * Returns the value of the key present in the input string text
     *
     * @param text = String where the key value pair has to be searched
     * @param key  = key to search for
     * return - value of the key present in text
     */
    public static String getValueIn(final String text, String key) {
        String value = "";

        if (text !=null && key !=null && key.length() > 0) {
            key = key + "=";

            int start = text.indexOf(key, 0);

            int end = (start != -1) ? text.indexOf(";", start + key.length()) : -1;

            value = (end != -1) ? text.substring(start + key.length(), end) : "";
        }

        return value;
    }

    /**
     * Returns the last word of publisher key
     * @param key_str
     * @return term_key_str
     */
    public static String getLastSegmentPublisherKey(String key_str){
                String term_key_str = null;
                if(key_str !=null) term_key_str = key_str.substring(key_str.lastIndexOf(".")+1, key_str.length());
                return term_key_str;
    }

    /**
     * Instantiates the static inner AlarmDetails class and sets the required calculated attributes
     * @return AlarmDetails instance with alarm details
     */
    public static AlarmDetails getAlarmTimeForComingSunday(){
        AlarmDetails alarmTime = new AlarmDetails();
        alarmTime.calculateTimerForComingSundayFromCalendar();

        return alarmTime;
    }

    /**
     * This static inner class calculates the Alarm details required for weekly Sunday trigger
     * Static declaration would enable it to be called Outerame.Inner name from an external class
     * if required and grants it same privilege as any other top level class keeping the static
     * nature of calling method intact
     *
     * <code><pre>
     * CLASS:
     * if required and grants it same privilege as any other top level class keeping the static
     * nature of calling method intact
     *
     * <code><pre>
     * CLASS:
     *     Extends : None
     *
     * RESPONSIBILITIES:
     *     Returns calculated values for alarm time to sunday and weekly cycle to rerun
     *
     * COLLABORATORS:
     *     None
     *
     * USAGE:
     *     See each method.
     *
     * </pre></code>
     */
    public static class AlarmDetails{
        public long firstWakeToSunday; // time in ms
        public long weeklyCycles; //time in ms

        /**
         * Calculates the time in ms details to trigger the weekly alarm
         */
        private void calculateTimerForComingSundayFromCalendar(){
                weeklyCycles = TIME_CONST.NUM_DAYS*TIME_CONST.NUM_HOURS*TIME_CONST.NUM_MINS*TIME_CONST.NUM_SECS*TIME_CONST.NUM_MS;
                // weeks time in ms
                long time_to_next_sunday_ms = 0;
                int num_days=0, num_of_hrs=0, num_of_mins=0, num_of_secs=0, num_of_ms=0;
                Calendar calendar = Calendar.getInstance();
                        if(LOG_DEBUG) Log.d(TAG, " Calendar info current time " + calendar.get(Calendar.MONTH)+ " "+
                                        calendar.get(Calendar.DAY_OF_WEEK)+ " "+ calendar.get(Calendar.DATE)+" "+
                                        calendar.get(Calendar.HOUR_OF_DAY) + " "+calendar.get(Calendar.SECOND));

                long cur_time_millis = calendar.getTimeInMillis();
                if(LOG_DEBUG) Log.d(TAG, "  cur_time_millis" + cur_time_millis);

                //calculating the first occurrence of sun around midnight(~1am Sunday), after boot up
                //and hence forth we would update the db weekly around Sunday night
                num_days = Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) ;
                num_of_hrs = TIME_CONST.NUM_HOURS - calendar.get(Calendar.HOUR_OF_DAY) ;
                num_of_mins = TIME_CONST.NUM_MINS - calendar.get(Calendar.MINUTE) ;
                num_of_secs = TIME_CONST.NUM_SECS - calendar.get(Calendar.SECOND) ;
                num_of_ms = TIME_CONST.NUM_MS - calendar.get(Calendar.MILLISECOND);
                time_to_next_sunday_ms = num_of_ms + num_of_secs*TIME_CONST.NUM_MS +
                                         num_of_mins*TIME_CONST.NUM_SECS*TIME_CONST.NUM_MS +
                                         num_of_hrs*TIME_CONST.NUM_MINS*TIME_CONST.NUM_SECS*TIME_CONST.NUM_MS +
                                         num_days*TIME_CONST.NUM_HOURS*TIME_CONST.NUM_MINS*TIME_CONST.NUM_SECS*TIME_CONST.NUM_MS;
                if(LOG_DEBUG) Log.d(TAG, "  time_to_next_sunday_ms " + time_to_next_sunday_ms);
                firstWakeToSunday = System.currentTimeMillis() + time_to_next_sunday_ms;

                //for debug purposes
                        if(LOG_DEBUG){
                                calendar.setTimeInMillis(firstWakeToSunday);
                                Log.d(TAG, " Calendar info of 1st Sun " + calendar.get(Calendar.MONTH)+ " "+
                                        calendar.get(Calendar.DAY_OF_WEEK)+ " "+ calendar.get(Calendar.DATE)+" "+
                                        calendar.get(Calendar.HOUR_OF_DAY) + " "+calendar.get(Calendar.SECOND));
                        }
        }

    } // end of class definition AlarmDetails

    /**
     * Stores the XML version in a shared preference
     *
     * @param context - Application context
     * @param xmlVersion - new XML version
     */
    public static void storeXmlVersion(Context context, float xmlVersion){

        SharedPreferences mXmlVersionPrefs = context.getSharedPreferences(XML_VERSION_SHARED_PREFERENCE,
                                                                             Context.MODE_PRIVATE);

        SharedPreferences.Editor xmlVersionEditor = mXmlVersionPrefs.edit();
        xmlVersionEditor.clear();
        xmlVersionEditor.putFloat(XML_VERSION, xmlVersion);
        xmlVersionEditor.commit();

        if(LOG_DEBUG) Log.d(TAG, "XML version stored = " + xmlVersion);
    }

     /** Starts the Help Activity
      * 
      * @param context - context
      */
     public static void showHelp(final Context context) {
     	Intent intent = new Intent(context, WebViewActivity.class);
     	String helpUri = Util.getHelpUri(context);
 		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
     	intent.putExtra(WebViewActivity.REQUIRED_PARMS.CATEGORY, WebViewActivity.WebViewCategory.HELP);
     	intent.putExtra(WebViewActivity.REQUIRED_PARMS.LAUNCH_URI, helpUri);
     	
     	context.startActivity(intent);
     }

     /** returns a list of icons that are used for rule icon. 
      * 
      * @param context - context
      * @param legacyIcons - when set to true will return the legacy rule icons (same as
      * 	ic_battery_w but with different names)
      * @return list of rule icons
      */
     public static List<String> getRuleIconsList(Context context, boolean legacyIcons) {
    	 String[] icons1 = context.getResources().getStringArray(R.array.rule_icon_items);
    	 
    	 String[] icons2 = null;
    	 if(legacyIcons)
    		 icons2 = context.getResources().getStringArray(R.array.legacy_rule_icons_items);
    	 
    	 List<String> icons = new ArrayList<String>(Arrays.asList(icons1));
    	 if(icons2 != null && icons2.length > 0) {
    		 icons.addAll(Arrays.asList(icons2));
	 }
    	 return icons;
     }
     
     /** Queries the package Manager to see if activity to handle
      * the given intent is available on the device
      *
      * @param context - context
      * @param intent to be queried for
      *
      * @return true if activity is available; false otherwise
      */

     public static boolean isActivityAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();

	    List<ResolveInfo> resolveInfo =
	            packageManager.queryIntentActivities(intent,
	                    PackageManager.MATCH_DEFAULT_ONLY);
	   if (resolveInfo.size() > 0) {
	     return true;
	   }
	   return false;
	 }

     /** Setter - set the shared preference passed in (sharedPrefValue) to the value passed in.
      *   Also makes Broadcast which Location sensor module will listen to
      * @param context - Context
      * @param sharedPrefValue - shared preference string for which the value is to be set
      * @param from - Application calling this setter.
      * @param state - String "1" or "0"
      */
     public static void setLocSharedPrefStateValue(final Context context, final String sharedPrefString,
             final String from, String state) {
         SharedPreferences sp = context.getSharedPreferences(LOC_SENSOR_SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
         sp.edit().putString(sharedPrefString, state).commit();
         if(LOG_DEBUG) Log.d(TAG, "Changing:"+sharedPrefString+" to "+state+" from:"+from);
         // Send Broadcast for Location services
         Util.sendBroadcastStartStopLocService(context,state);
     }

    /** Getter - returns if the preference passed in (sharedPrefValue) is set to true or false.
      *
      * @param context - Context
      * @param sharedPrefValue - shared preference string for which the value is to be retrieved
      * @param from - Application calling this getter
      * @return - true or false.
      */
     public static String getLocSharedPrefStateValue(final Context context, final String sharedPrefString, final String from) {
         SharedPreferences sp = context.getSharedPreferences(LOC_SENSOR_SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
         String sharedPrefValue = sp.getString(sharedPrefString, null);
         if(LOG_DEBUG) Log.d(TAG, "Returning:"+sharedPrefString+" = "+sharedPrefValue+" to: "+from);
         return sharedPrefValue;
     }

     /**
      * If we have the user consent where he has agreed to use Mot Loc service
      * which authorizes us to keep using autonomous wifi scan even when wifi is off
      * @param context
      * @return true if user has consented, false otherwise
      */
     public static boolean isMotLocConsentAvailable(final Context context){
        boolean ret = false;
        String pref_string = Util.getLocSharedPrefStateValue(context, AppPreferences.HAS_USER_LOC_CONSENT, TAG);
        if(pref_string != null){
                 if(pref_string.equalsIgnoreCase(LOC_CONSENT_SET)){
                    ret = true;
                 }
        }
        return ret;
     }

     /**
      * Sends Broadcast to Location provider to start stop scanning for Loc
      * @param loc_consent if "1" broadcast intent to start, if "0" sends broadcast to stop
      */
     public static void sendBroadcastStartStopLocService(final Context context, String loc_consent){
        Intent intent = new Intent(LAUNCH_LOC_SVC_UPON_CONSENT);
        intent.putExtra(AppPreferences.HAS_USER_LOC_CONSENT, loc_consent);
         context.sendBroadcast(intent);
     }

     /**
      * Returns the intent present in a Uri string
      *
      * @param Uri
      * @return Extracted intent from the Uri
      */
    public static Intent getIntent (String uri) {
         Intent intent = null;
         if (uri != null) {
             try {
                 // Convert the uri to an intent to get at the fields
                 intent = Intent.parseUri(uri, 0);
             } catch (URISyntaxException e) {
                 Log.w(TAG, "Exception when retrieving from config");
             }
         }
         return intent;
     }

    /** constructs the intent to launch the rules builder
     * 
     * @param context - context
     * @param ruleID - Rule ID to edit
     * @return intent to launch the rules builder
     */
    public static Intent fetchRulesBuilderIntent(Context context, long ruleID) {
        Intent intent = new Intent(context, EditRuleActivity.class);
        Bundle intentBundle = new Bundle();
        intentBundle.putLong(PUZZLE_BUILDER_RULE_ID, ruleID);
        intentBundle.putBoolean(PUZZLE_BUILDER_RULE_COPY, false);      
        intentBundle.putParcelable(PUZZLE_BUILDER_RULE_INSTANCE, null);
        intent.putExtras(intentBundle);     
        return intent;
    }



}
