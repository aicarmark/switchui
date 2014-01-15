package com.motorola.contextual.rule;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class Util {

    private final static String TAG = Util.class.getSimpleName();

    /** Checks if a string is null or empty string or null string
     *
     * @param input
     * @return  true or false
     */
    public static boolean isNull(String input){
        boolean result = false;
        if ( (input == null) || ((input != null) && ((input.length() <= 0) || (input.equals("null") )))){
            result = true;
        }
        return result;
    }

    /** Setter - set the shared preference passed in (sharedPrefValue) to the value passed in.
    *
    * @param context - Context
    * @param sharedPrefValue - shared preference string for which the value is to be set
    * @param state - true or false
    */
   public static void setSharedPrefStateValue(final Context context, final String sharedPrefString, boolean state) {
       SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
       sp.edit().putBoolean(sharedPrefString, state).apply();
       if(Constants.LOG_DEBUG) Log.d(TAG , "Setting:"+sharedPrefString+" to "+state);
   }

   /** Getter - returns if the preference passed in (sharedPrefValue) is set to true or false.
    *
    * @param context - Context
    * @param sharedPrefValue - shared preference string for which the value is to be retrieved
    * @return - true or false.
    */
   public static boolean getSharedPrefStateValue(final Context context, final String sharedPrefString) {
       SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
       boolean sharedPrefValue = sp.getBoolean(sharedPrefString, false);
       if(Constants.LOG_DEBUG) Log.d(TAG, "Fetching: "+sharedPrefString+" = "+sharedPrefValue);
       return sharedPrefValue;
   }

   /** Setter - set the shared preference passed in (sharedPrefValue) to the value passed in.
   *
   * @param context - Context
   * @param sharedPrefValue - shared preference string for which the value is to be set
   * @param value - true or false
   */
  public static void setSharedPrefIntValue(final Context context, final String sharedPrefString, int value) {
      SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
      sp.edit().putInt(sharedPrefString, value).apply();
      if(Constants.LOG_DEBUG) Log.d(TAG , "Setting:"+sharedPrefString+" to "+value);
  }

  /** Getter - returns if the preference passed in (sharedPrefValue) is set to true or false.
   *
   * @param context - Context
   * @param sharedPrefValue - shared preference string for which the value is to be retrieved
   * @return - true or false.
   */
  public static int getSharedPrefIntValue(final Context context, final String sharedPrefString) {
      SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
      int sharedPrefValue = sp.getInt(sharedPrefString, 0);
      if(Constants.LOG_DEBUG) Log.d(TAG, "Fetching: "+sharedPrefString+" = "+sharedPrefValue);
      return sharedPrefValue;
  }

  /**
   *
   * @param source - source string
   * @param x - defines the right most number of characters to return
   * @return - returns right most characters of the given length
   */
  public static String getLastXChars(String source, int x) {

      if (Util.isNull(source)) return "";

      int extra = source.length() - x;
      return (extra <= 0)? source:source.substring(extra);
  }

  /**
   * Get the Strings that needs to be translated We need to replace all
   * translatable strings in Name, Description and Suggested Reason For
   * example "Send Text to 12345" will be represented as
   * "rulesxml_descSendText,12345"
   *
   * @param ct
   *            - context
   * @param str
   *            - input string
   * @return - updated string
   */
  public static String getTranslatedString(Context ct, String str) {
      if (str != null) {
          String[] nameArray = str.split(",");
          StringBuffer buf = new StringBuffer();
          for (String elem : nameArray) {
              if (elem.startsWith(ct.getString(R.string.rulesxml_prefix)))
                  try{
                      buf.append(ct.getString(ct.getResources().getIdentifier(
                          elem, "string", ct.getPackageName())));
                  } catch (NotFoundException e){
                      Log.e(TAG, "Resource NotFoundException for=" + elem);
                      e.printStackTrace();
                  }
              else
                  buf.append(elem);
          }
          str = buf.toString();
          if(Constants.LOG_VERBOSE) Log.i(TAG, str);

      }
      return str;
  }

  /**
   * We need to update the existing xml with translated fields like -
   * name, desc, etc
   *
   * @param ct - context
   * @param ruleXml - rule xml
   * @return - update rule xml
   */
  public static String updateLocalizableFields(Context ct, String ruleXml) {
      String prefix = ct.getString(R.string.rulesxml_prefix);
      String suffix = ct.getString(R.string.rulesxml_suffix);

      int startIndx = 0;
      // do this till we keep finding prefix
      while((startIndx = ruleXml.indexOf(prefix)) != -1){

          int endIndx = ruleXml.indexOf(suffix, startIndx) + suffix.length();
          if(endIndx == -1) return ruleXml;

          String res = ruleXml.substring(startIndx, endIndx);
          if(Constants.LOG_VERBOSE) Log.d(TAG,"res="+res);
          String localizedStr = Util.getTranslatedString(ct, res);
          if(Constants.LOG_VERBOSE) Log.d(TAG,"loc="+localizedStr);
          ruleXml = ruleXml.replaceAll(res, localizedStr);
      }

      return ruleXml;
  }
}
