/*
 * @(#)ActionPublisherList.java
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
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.util.Util;

/**
 * The entire list of Action publishers that need to be shown that
 * can be added to a Rule
 * 
*<code><pre>
 * CLASS:
 *  None.
 *  
* RESPONSIBILITIES:
* This class is a Master list of all Actions available to SmartActions
* Whitelist/Blacklist is implemented here
*
* COLABORATORS:
* Context
*
* USAGE:
* 	See each method.
*</pre></code>
*/
public class ActionPublisherList extends PublisherList implements Constants, 
										RulesBuilderConstants, DbSyntax{
	private static final String TAG = ActionPublisherList.class.getSimpleName();
	private static final long serialVersionUID = 1188160896543517212L;
	private Context mContext;
	
	/**
	 * This creates and initializes list of All available Action publishers
	 * @param context
	 */
	public ActionPublisherList(Context context){
		mContext = context;
		initActionListFromPublisherProvider();
	}

	/** 
	 * This method gets the list of all valid action publishers from Publisher provider DB.
	 */
    private void initActionListFromPublisherProvider() {
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
                       AND + PublisherProviderInterface.Columns.TYPE + EQUALS + Q + PublisherProviderInterface.Type.ACTION + Q;
        try {
            cursor = cr.query(PublisherProviderInterface.CONTENT_URI, projection, where, null, null);

            if(cursor != null && cursor.moveToFirst()) {
                do {
			pubKey = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PUBLISHER_KEY));
			
			// There is a hardcoded entry in Publisher Provider DB for the Sync publisher key.
			// This should not be listed in available actions list as it is a hidden publisher.
			if(pubKey.equalsIgnoreCase(SYNC_PUB_KEY)) continue;

			if(pubKey.equalsIgnoreCase(PROCESSOR_SPD_PUB_KEY)) {
	            if(	!Util.isProcessorSpeedSupported(mContext)) {
	                continue;
	            }
	        }
			icon = cursor.getBlob(cursor.getColumnIndex(PublisherProviderInterface.Columns.ICON));
			stateType = cursor.getInt(cursor.getColumnIndex(PublisherProviderInterface.Columns.STATE_TYPE));

		            ActionPublisher actPub = new ActionPublisher(null);
		            actPub.activityPkgUri = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PACKAGE));
		            actPub.blockId = count++;
		            actPub.blockName = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.DESCRIPTION));
		            actPub.image_drawable = IconPersistence.getIconDrawableFromBlob(mContext, icon);
		            actPub.isAction = true;
		            actPub.publisherKey = pubKey;
		            actPub.marketUrl = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.MARKET_LINK));
		            actPub.stateType = (stateType == ModalTable.Modality.STATEFUL)?STATEFUL:STATELESS;
		            actPub.validity = TableBase.Validity.VALID;
		            activityIntent = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.ACTIVITY_INTENT));
		            if(activityIntent != null) {
			            actPub.intentUriString = Publisher.getActivityIntentForPublisher(
	                            actPub.activityPkgUri,
	                            activityIntent).toUri(0);
		            } else {
		            	actPub.intentUriString = null;
		            }
		            this.put(pubKey, actPub);
                    if(LOG_VERBOSE) Log.v(TAG, " Adding " + pubKey + " to Actionlist" );
                } while (cursor.moveToNext());
            }

        } catch(Exception e) {
            Log.e(TAG, "Exception in query " + e, e);
        } finally {
            if(cursor != null) cursor.close();
        }
    }



}
