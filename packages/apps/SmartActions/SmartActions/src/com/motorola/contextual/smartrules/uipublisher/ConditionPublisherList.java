/*
 * @(#)ConditionPublisherList.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2012/04/03  NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uipublisher;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.PublisherProviderInterface;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.util.Util;



/**
* The entire list of Action publishers that need to be shown that
* can be added to a Rule
*<code><pre>
* CLASS:
*  None.
*  
* RESPONSIBILITIES:
* This class is a Master list of all Conditions available to SmartActions
* Whitelist/Blacklist is implemented here
*
* COLABORATORS:
* Context
*
* USAGE:
* 	See each method.
*</pre></code>
*/
public class ConditionPublisherList extends PublisherList implements Constants, 
										RulesBuilderConstants, DbSyntax {

	private static final long serialVersionUID = -8763160413360872136L;
	private static final String TAG = ConditionPublisherList.class.getSimpleName();
	private Context mContext;
	
	/**
	 * This initializes and return the list of all available ConditionPublisher
	 * @param context
	 */
	public ConditionPublisherList(Context context){
		mContext = context;
		initConditionListFromPublisherProvider();
	}

	
	/** 
	 * This method gets the list of all valid condition publishers from Publisher provider DB.
	 */
	   private void initConditionListFromPublisherProvider() {
	        ContentResolver cr = mContext.getContentResolver();
	        Cursor cursor = null;
	        String pubKey, activityIntent;
	        byte[] icon;
	        int stateType = 0, count = 0;
	        String[] projection = {PublisherProviderInterface.Columns.PUBLISHER_KEY, PublisherProviderInterface.Columns.STATE_TYPE,
				PublisherProviderInterface.Columns.MARKET_LINK, PublisherProviderInterface.Columns.DESCRIPTION,
				PublisherProviderInterface.Columns.ICON, PublisherProviderInterface.Columns.PACKAGE,
				PublisherProviderInterface.Columns.ACTIVITY_INTENT};
	        String where = PublisherProviderInterface.Columns.BLACKLIST + EQUALS + Q + PublisherProviderInterface.BlackList.FALSE + Q +
	                       AND + PublisherProviderInterface.Columns.TYPE + EQUALS + Q + PublisherProviderInterface.Type.CONDITION + Q;
	        try {
	            cursor = cr.query(PublisherProviderInterface.CONTENT_URI, projection, where, null, null);

	            if(cursor != null && cursor.moveToFirst()) {
	                do {
				pubKey = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PUBLISHER_KEY));
				if( pubKey.equalsIgnoreCase(LOCATION_TRIGGER_PUB_KEY)) {
		            if(Util.hideLocationTrigger(mContext)) {
		                continue;
		            }
		        }
				icon = cursor.getBlob(cursor.getColumnIndex(PublisherProviderInterface.Columns.ICON));
				stateType = cursor.getInt(cursor.getColumnIndex(PublisherProviderInterface.Columns.STATE_TYPE));

				ConditionPublisher condPub = new ConditionPublisher(null);
			            condPub.activityPkgUri = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PACKAGE));
			            condPub.blockId = count++;
			            condPub.blockName = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.DESCRIPTION));
			            condPub.image_drawable = IconPersistence.getIconDrawableFromBlob(mContext, icon);
			            condPub.isAction = false;
			            condPub.publisherKey = pubKey;
			            condPub.marketUrl = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.MARKET_LINK));
			            condPub.stateType = (stateType == 1)?STATEFUL:STATELESS;
			            condPub.blockDescription = mContext.getString(R.string.description);
			            condPub.validity = TableBase.Validity.VALID;
			            activityIntent = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.ACTIVITY_INTENT));
			            if(activityIntent != null)
					condPub.intentUriString = Publisher.getActivityIntentForPublisher(condPub.activityPkgUri, activityIntent).toUri(0);
			            else
					condPub.intentUriString = null;
			            this.put(pubKey, condPub);
	                    if(LOG_VERBOSE) Log.d(TAG, " Adding " + pubKey + " to Conditionlist");
	                } while (cursor.moveToNext());
	            }

	        } catch(Exception e) {
	            Log.e(TAG, "Exception in query " + e, e);
	        } finally {
	            if(cursor != null) cursor.close();
	        }
	    }

	
}
