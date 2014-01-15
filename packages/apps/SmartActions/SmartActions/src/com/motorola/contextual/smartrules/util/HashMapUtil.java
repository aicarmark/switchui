/*
 * @(#)HashMapUtil.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/06/29 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.util;

import java.util.HashMap;
import java.util.Iterator;

import com.motorola.contextual.smartrules.Constants;

import android.graphics.Bitmap;
import android.util.Log;

/** Util class for hashmaps.
 *
 *<code><pre>
 * CLASS:
 * 	implements Constants.
 *
 * RESPONSIBILITIES:
 * 	extends Hashmap classes for internal use.
 *
 * COLABORATORS:
 *  None.
 *  
 * USAGE:
 * 	None.
 * 
 *</pre></code>
 */
public class HashMapUtil implements Constants {

	private static final String TAG = HashMapUtil.class.getSimpleName();

	/** class to hold the rule icon details like bitmap and the rule icon resource ID
	 */
	public static class RuleIconDetails {
		public Bitmap bitmap = null;
		public int ruleIconResId = -1;
	}
	
	/** Util class for hashmaps of key Long and object Bitmap.
	 *
	 *<code><pre>
	 * CLASS:
	 * 	extends Hashmap.
	 *
	 * RESPONSIBILITIES:
	 * 	overrides the clear and remove methods of Hashmap.
	 *  calls the recycle method for the bitmap.
	 *  calls System.gc() to force a cleanup of memory for bitmap
	 *
	 * COLABORATORS:
	 *  None.
	 *  
	 * USAGE:
	 * 	None.
	 * 
	 *</pre></code>
	 */
	public static class HashMapUtilLong extends HashMap<Long, RuleIconDetails> { 
		private static final long serialVersionUID = -3450391594313305961L;
	
		@Override
		public void clear() {
			Iterator<RuleIconDetails> iter=  this.values().iterator();
			while (iter.hasNext()) {
				RuleIconDetails item = iter.next();
				if(LOG_DEBUG) Log.d(TAG, "in clear for HashMapUtilLong recycling the bitmap "+item.bitmap);
			}
			System.gc();	
			super.clear();
		}

		@Override
		public RuleIconDetails remove(Object key) {
			RuleIconDetails item = this.get(key);
			if(item != null && item.bitmap != null && !item.bitmap.isRecycled()) {
				if(LOG_DEBUG) Log.d(TAG, "in remove() for HashMapUtilLong for key "+key+" recycling the bitmap "+item.bitmap);
			}
			System.gc();
			return super.remove(key);
		}	
	}
}
