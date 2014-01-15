 /* @(#)DisplayRuleIconGrid.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385        2011/06/22 NA				  Initial version
 * 
 */

package com.motorola.contextual.smartrules.rulesbuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/** This class displays the grid view for rule icons.
*
* CLASS:
* 
* RESPONSIBILITIES:
* 
* COLABORATORS:
*  None.
*
* USAGE:
* 	see methods for usage instructions
*
*
*/
public class DisplayRuleIconGrid {
	public static class ImageAdapter extends BaseAdapter {
		    private Context context;
		   
	        public ImageAdapter(Context c) {
	            context = c;
	        }

	        public int getCount() {
	            return ruleIconPath.length;
	        }

	        public Object getItem(int position) {
	            return position;
	        }

	        public long getItemId(int position) {
	            return position;
	        }

	        public View getView(int position, View convertView, ViewGroup parent) {
	        	ImageView imageView = null;
	            if (convertView == null) {
	                imageView = new ImageView(context);
	                imageView.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	                imageView.setAdjustViewBounds(false);
	            } else {
	            	if (convertView instanceof ImageView)
	            		imageView = (ImageView) convertView;
	            	else {
	            		imageView = new ImageView(context);
	                    imageView.setLayoutParams(new GridView.LayoutParams(95, 95));
	                    imageView.setAdjustViewBounds(false);
	                    imageView.setPadding(6, 6, 6, 6);
	            	}
	            }
	            String iconPath = ruleIconPath[position];
	            int iconId = context.getResources().getIdentifier(iconPath, "drawable", context.getPackageName());
	            imageView.setImageResource(iconId);
	            return imageView;
	        }
	   }
	  
	  public final static String[] ruleIconPath = {
	  		"ic_default_w",
          "ic_home_w",
          "ic_work_w",
          "ic_sleep_w",
          "ic_driving_w",
          "ic_evening_w",
          "ic_morning_w",
          "ic_settings_w",
          "ic_workout_w",
          "ic_time_w",
          "ic_location_w",
          "ic_battery_w",
          "ic_music_w",
          "ic_quiet_zone_w",
          "ic_calendar_w",
          "ic_school",
          "ic_phone_w"
	  };
}