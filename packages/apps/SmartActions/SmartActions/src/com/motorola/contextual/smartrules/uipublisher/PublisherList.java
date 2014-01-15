/*
 * @(#)PublisherList.java
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import android.content.Context;
import com.motorola.contextual.smartrules.util.PublisherFilterlist;

/**
 *
 * PublisherList will only hold interfaces for  UI consumption, utility/behaviors
 * that are common across publishers.
 * This is not rule specific
 *
 * TODO This can also implement PublisherStateChangeListener thus knowing of latest
 * state for any item in the list
 */
public class PublisherList extends LinkedHashMap<String, Publisher> {

	private static final long serialVersionUID = 255590801843790266L;

	//TODO implement Receiver to listen for App Install/Uninstall
	// and send corresponding DIRTY_LIST event to subscriber

	private PublisherFilterlist instFilterList;

	public PublisherList(){
		initInstFilterList();
	}

	private void initInstFilterList(){
		instFilterList = PublisherFilterlist.getPublisherFilterlistInst();
	}

	//this is where BlackList/Whitelist utility call are
	protected boolean isBlackListed(Context context, String pubKey){
		return instFilterList.isBlacklisted(context, pubKey);
	}

	protected boolean isWhiteListed(Context context, String pckgName){
		return instFilterList.isWhitelisted(context, pckgName);
	}

	public void logPublisherList(){
		Set<Entry<String, Publisher>> set = this.entrySet();
		Iterator<Entry<String, Publisher>> iterator = set.iterator();
		while(iterator.hasNext()){
			Publisher pub = (Publisher)iterator.next().getValue();
			pub.logPublisherData();
		}
	}
	
	/**
	 * Returns a matching Publishers instance for a given Publisher key
	 * Every time a new Publisher object will be returned for any given key
	 * @param pubKey
	 * @return
	 */
	public Publisher getMatchingPublisher(String pubKey){
		Publisher ret;
		ret = this.get(pubKey);
		try {
			ret = (Publisher) ret.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
