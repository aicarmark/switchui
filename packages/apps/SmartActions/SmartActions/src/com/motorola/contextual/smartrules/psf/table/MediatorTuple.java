/*
 * @(#)MediatorTuple.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/05/17                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.table;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

/**
* MediatorTuple - Represents a row of the MediatorTable
* CLASS:
*     MediatorTuple
*
* RESPONSIBILITIES:
*  Abstracts a row of the MediatorTable
*
* USAGE:
*     See each method.
*
*/
public class MediatorTuple {

	private String mConsumer, mPublisher;
	private String mState, mConfig;
	private String mConsumerPackage, mPublisherPackage;
	private String mSubscribeRequestIds, mCancelRequestIds;
	private int    mId;
	
    /**
	 * @return the mId
	 */
	public int getId() {
		return mId;
	}

	/**
	 * @param mId the mId to set
	 */
	public void setId(int Id) {
		this.mId = Id;
	}

	private static final String ID_SEPARATOR = "---";

	/**
	 * @return the consumerPackage
	 */
	public String getConsumerPackage() {
		return mConsumerPackage;
	}

	/**
	 * @param consumerPackage the consumerPackage to set
	 */
	public void setConsumerPackage(String consumerPackage) {
		this.mConsumerPackage = consumerPackage;
	}

	/**
	 * @return the publisherPackage
	 */
	public String getPublisherPackage() {
		return mPublisherPackage;
	}

	/**
	 * @param publisherPackage the publisherPackage to set
	 */
	public void setPublisherPackage(String publisherPackage) {
		this.mPublisherPackage = publisherPackage;
	}

	/**
	 *
	 * @return - Consumer of this tuple
	 */
	public String getConsumer() {
		return mConsumer;
	}

	/**
	 * Sets the Consumer of the tuple
	 * @param consumer
	 */
	public void setConsumer(String consumer) {
		this.mConsumer = consumer;
	}

	/**
	 * Returns the publisher of the tuple
	 * @return - Publisher of the tuple
	 */
	public String getPublisher() {
		return mPublisher;
	}

	/**
	 * Sets the publisher of the tuple
	 * @param publisher - publisher of the tuple
	 */
	public void setPublisher(String publisher) {
		this.mPublisher = publisher;
	}

	/**
	 * Returns the state of the tuple.  Note that state can be anything
	 * It need not be limited to just true/false.  Any such requirements
	 * must be taken care by the user of the class
	 * @return - state of the tuple
	 */
	public String getState() {
		return mState;
	}

	/**
	 * Sets the state of the tuple.  Note that state can be anything.
	 * It need not be limited to just true/false.  Any such requirements
	 * must be taken care by the user of the class
	 * @param state
	 */
	public void setState(String state) {
		this.mState = state;
	}

	/**
	 * Returns the config associated with the tuple
	 * @return - config
	 */
	public String getConfig() {
		return mConfig;
	}

	/**
	 * Sets the config associated with the tuple
	 * @param - config
	 */
	public void setConfig(String config) {
		this.mConfig = config;
	}

	/**
	 * Returns the subscribe request ids, which by default
	 * is separated by a defined separator
	 * @return - subscribeRequestIds
	 */
	public String getSubscribeRequestIds() {
		return mSubscribeRequestIds;
	}

	/**
	 * Returns the subscribe request ids as a list
	 * @return - ArrayList<subscribeRequestId>
	 */
	public ArrayList<String> getSubscribeRequestIdsAsList() {
		ArrayList<String> idList = convertStringToList(mSubscribeRequestIds);
		return idList;
	}

	/**
	 * Sets the subscribe request ids, which by default
	 * must be separated by a defined separator
	 * @param subscribeRequestIds
	 */
	public void setSubscribeRequestIds(String subscribeRequestIds) {
		this.mSubscribeRequestIds = subscribeRequestIds;
	}

	/**
	 * Sets the subscribe request ids
	 * The input list is converted into SEPARATOR separated strings
	 * @param arraySubscribeIds - ArrayList of subscribe ids
	 */
	public void setSubscribeRequestIds(ArrayList<String> arraySubscribeIds) {
		String stringIds = TextUtils.join(ID_SEPARATOR, arraySubscribeIds);
		setSubscribeRequestIds(stringIds);
	}

	/**
	 * Add a request id to the existing subscribe list
	 * @param id - Request id to be added
	 */
	public void addSubscribeRequestId(String id) {
		ArrayList<String> arraySubscribeRequestIds = getSubscribeRequestIdsAsList();
		if (!arraySubscribeRequestIds.contains(id)) {
			arraySubscribeRequestIds.add(id);
			setSubscribeRequestIds(arraySubscribeRequestIds);
		}
	}

	/**
	 * Remove a request id from the existing subscribe list
	 * @param id - Request id to be removed
	 */
	public void removeSubscribeRequestId(String id) {
		ArrayList<String> arraySubscribeRequestIds = getSubscribeRequestIdsAsList();
		if (arraySubscribeRequestIds.remove(id)) {
			setSubscribeRequestIds(arraySubscribeRequestIds);
		}
	}

	/**
	 * Returns the cancel request ids, which by default
	 * is separated by a defined separator
	 * @return - cancelRequestIds
	 */
	public String getCancelRequestIds() {
		return mCancelRequestIds;
	}

	/**
	 * Returns the cancel request ids as a list
	 * @return - ArrayList<cancelRequestId>
	 */
	public ArrayList<String> getCancelRequestIdsAsList() {
		ArrayList<String> idList = convertStringToList(mCancelRequestIds);
		return idList;
	}

	/**
	 * Sets the cancel request ids, which by default
	 * must be separated by a defined separator
	 * @param cancelRequestIds
	 */
	public void setCancelRequestIds(String cancelRequestIds) {
		this.mCancelRequestIds = cancelRequestIds;
	}

	/**
	 * Sets the cancel request ids
	 * The input list is converted into SEPARATOR separated strings
	 * @param arrayCancelIds - ArrayList of cancel ids
	 */
	public void setCancelRequestIds(ArrayList<String> arrayCancelIds) {
		String stringIds = TextUtils.join(ID_SEPARATOR, arrayCancelIds);
		setCancelRequestIds(stringIds);
	}

	/**
	 * Add a request id to the existing cancel list
	 * @param id - Request id to be added
	 */
	public void addCancelRequestId(String id) {
		ArrayList<String> arrayCancelRequestIds = getCancelRequestIdsAsList();
		if (!arrayCancelRequestIds.contains(id)) {
			arrayCancelRequestIds.add(id);
			setCancelRequestIds(arrayCancelRequestIds);
		}
	}

	/**
	 * Remove the request id from the existing cancel request id list
	 * @param id - id to be removed
	 */
	public void removeCancelRequestId(String id) {
		ArrayList<String> arrayCancelRequestIds = getCancelRequestIdsAsList();
		if (arrayCancelRequestIds.remove(id)) {
			setCancelRequestIds(arrayCancelRequestIds);
		}
	}

	/**
	 * Constructor to create MediatorTuple from individual column values
	 * @param consumer
	 * @param publisher
	 * @param config
	 * @param state
	 * @param subscribeRequestIds - subscribe request ids separated by a separator
	 * @param cancelRequestIds - cancel request ids separated by a separator
	 * @param refreshIds - refresh request ids separated by a separator
	 */
    public MediatorTuple(String consumer, String consumerPackage,
		String publisher, String publisherPackage,
		String config, String state,
		String subscribeRequestIds, String cancelRequestIds) {
	setConsumer(consumer);
	setConsumerPackage(consumerPackage);
	setPublisher(publisher);
	setPublisherPackage(publisherPackage);
	setState(state);
	setConfig(config);
	setSubscribeRequestIds(subscribeRequestIds);
	setCancelRequestIds(cancelRequestIds);
    }

    /**
     * Constructor to create a MediatorTuple from a cursor
     * @param cursor - Cursor should be non null and should be pointing to a valid row
     */
    public MediatorTuple(Cursor cursor) {
        this(cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONSUMER)),
		 cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONSUMER_PACKAGE)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.PUBLISHER)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.PUBLISHER_PACKAGE)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONFIG)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.STATE)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.SUBSCRIBE_ID)),
             cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CANCEL_ID)));
        setId(cursor.getInt(cursor.getColumnIndex(MediatorTable.Columns._ID)));
    }

    /**
     * Returns ContentValue for the MediatorTuple
     * @return - ContentValue
     */
    public ContentValues getAsContentValues() {
        ContentValues value = new ContentValues();
        value.put(MediatorTable.Columns.CONSUMER, mConsumer);
        value.put(MediatorTable.Columns.CONSUMER_PACKAGE, mConsumerPackage);
        value.put(MediatorTable.Columns.PUBLISHER, mPublisher);
        value.put(MediatorTable.Columns.PUBLISHER_PACKAGE, mPublisherPackage);
        value.put(MediatorTable.Columns.STATE, (mState != null) ? mState : "");
        value.put(MediatorTable.Columns.CONFIG, mConfig);
        value.put(MediatorTable.Columns.SUBSCRIBE_ID, (mSubscribeRequestIds != null) ? mSubscribeRequestIds : "");
        value.put(MediatorTable.Columns.CANCEL_ID, (mCancelRequestIds != null) ? mCancelRequestIds : "");

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if(o == null) {
            return false;
        }
        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(o instanceof MediatorTuple)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        MediatorTuple lhs = (MediatorTuple) o;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        if(!((mConsumer == null) ? lhs.mConsumer == null :
		mConsumer.equals(lhs.mConsumer))) return false;
        if(!((mConsumerPackage == null) ? lhs.mConsumerPackage == null :
		mConsumerPackage.equals(lhs.mConsumerPackage))) return false;
        if(!((mPublisher == null) ? lhs.mPublisher == null :
		mPublisher.equals(lhs.mPublisher))) return false;
        if(!((mPublisherPackage == null) ? lhs.mPublisherPackage == null :
		mPublisherPackage.equals(lhs.mPublisherPackage))) return false;
        if(!((mState == null) ? lhs.mState == null :
		mState.equals(lhs.mState))) return false;
        if(!((mConfig == null) ? lhs.mConfig == null :
		mConfig.equals(lhs.mConfig))) return false;
        if(!((mSubscribeRequestIds == null) ? lhs.mSubscribeRequestIds == null :
		mSubscribeRequestIds.equals(lhs.mSubscribeRequestIds))) return false;
        if(!((mCancelRequestIds == null) ? lhs.mCancelRequestIds == null :
		mCancelRequestIds.equals(lhs.mCancelRequestIds))) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // Start with a non-zero constant.
        int result = 17;

        // Include a hash for each field.
        result = 31 * result + ((mConsumer == null) ? 0 : mConsumer.hashCode());
        result = 31 * result + ((mConsumerPackage == null) ? 0 : mConsumerPackage.hashCode());
        result = 31 * result + ((mPublisher == null) ? 0 : mPublisher.hashCode());
        result = 31 * result + ((mPublisherPackage == null) ? 0 : mPublisherPackage.hashCode());
        result = 31 * result + ((mState == null) ? 0 : mState.hashCode());
        result = 31 * result + ((mConfig == null) ? 0 : mConfig.hashCode());
        result = 31 * result + ((mSubscribeRequestIds == null) ? 0 : mSubscribeRequestIds.hashCode());
        result = 31 * result + ((mCancelRequestIds == null) ? 0 : mCancelRequestIds.hashCode());

        return result;
    }

    private ArrayList<String> convertStringToList(String stringList) {
	ArrayList<String> listOfIds = new ArrayList<String> ();

	if (stringList != null) {
		String[] arrayList = TextUtils.split(stringList, ID_SEPARATOR);
		Collections.addAll(listOfIds, arrayList);
	}

	return listOfIds;
    }
}
