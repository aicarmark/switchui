/*
 * @(#)PublisherList.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/03/26                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.paf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTuple;

/** Abstract Class that provides various methods that need to be used by derived class
 * PackageManagerPublisherList and PublisherProviderList
 *<code><pre>
 * CLASS:
 *     PublisherList Extends HashMap<String, LocalPublisherTuple>
 * Interface:
 * 		PafConstants
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		Provides a factory function to create the right type of the publisher list
 *     Contains many utility functions that are of used with the publisher list
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public abstract class PublisherList extends LinkedHashMap<String, LocalPublisherTuple>
    implements PafConstants, DbSyntax {
    private static final long serialVersionUID = -2610255250769908277L;

    private static final String TAG = PublisherList.class.getSimpleName();

    private static final String PACKAGE_MANAGER_LIST = "PACKAGE_MANAGER_LIST";
    private static final String PUBLISHER_PROVIDER_LIST = "PUBLISHER_PROVIDER_LIST";

    protected Context mContext;
    protected List<String> mBlackList;
    protected String mCategory;
    protected String mPackageName;

    protected abstract void populatePublisherList();
    protected abstract void populatePublisherListForPackage();

    /**
     * Constructor to Provides List of LocalPublisherTuple
     * @param context Context of the caller
     */
    protected PublisherList(Context context) {
        mContext = context;
        populatePublisherList();
    }

    /**
     * Constructor to Provides List of LocalPublisherTuple for the given package
     * @param context Context of the caller
     * @param packageName Name of the package
     */
    protected PublisherList(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
        populatePublisherListForPackage();
    }

    /**
     * Factory method to create different category of publisher list
     * @param context - Context of caller
     * @param category - Category of the publisher list
     * @return Instance of PublisherList for the given category
     */
    public static PublisherList getPublisherList(Context context, String category) {
        PublisherList pubList = null;
        if(category.equals(PACKAGE_MANAGER_LIST)) {
            pubList = new PackageManagerPublisherList(context);
        } else if(category.equals(PUBLISHER_PROVIDER_LIST)) {
            pubList = new PublisherProviderList(context);
        } else {
            if(LOG_INFO) Log.i(TAG,"Invalid PublisherList category");
        }

        if(pubList != null) pubList.mCategory = category;
        return pubList;
    }

    /**
     * Factory method to create different category of publisher list for given package
     * @param context - Context of caller
     * @param category - Category of the publisher list
     * @param packageName - Name of the package
     * @return Instance of PublisherList for the given category
     */
    public static PublisherList getPublisherList(Context context, String category, String packageName) {
        PublisherList pubList = null;
        if(category.equals(PACKAGE_MANAGER_LIST)) {
            pubList = new PackageManagerPublisherList(context, packageName);
        } else if(category.equals(PUBLISHER_PROVIDER_LIST)) {
            pubList = new PublisherProviderList(context, packageName);
        } else {
            if(LOG_INFO) Log.i(TAG,"Invalid PublisherList category");
        }

        if(pubList != null) pubList.mCategory = category;
        return pubList;
    }

    /**
     * Use this method to get the publisher list after filtering Black listed publishers
     * @return PublisherList after filtering Black listed publishers
     */
    public PublisherList getFilteredList() {
        PublisherList filteredList = (PublisherList) this.clone();
        int size = filteredList.size();
        for( int index = 0; index < size; index++) {
            LocalPublisherTuple publisherTuple = filteredList.get(index);
            if(publisherTuple != null && publisherTuple.isBlackListed() == LocalPublisherTable.BlackList.TRUE) {
                filteredList.remove(index);
            }
        }
        return filteredList;
    }

    /**
     * Use this method to get the list of Black listed publishers
     * @return List containing the Black listed publishers
     */
    public List<String> getBlackList() {
        return mBlackList;
    }

    /**
     * Use this method to get list of {@link LocalPublisherTuple} for the given
     * package from the PublisherList
     * @param packageName - Name of the package
     * @return list of {@link LocalPublisherTuple}
     */
    public List<LocalPublisherTuple> getPublishersForPackage(String packageName) {
        ArrayList<LocalPublisherTuple> pkgPubList = new ArrayList<LocalPublisherTuple>();
        for(Entry<String, LocalPublisherTuple> pubEntry: this.entrySet()) {
            LocalPublisherTuple pubTuple = pubEntry.getValue();
            if(pubTuple.getPackageName().equals(packageName)) {
                pkgPubList.add(pubTuple);
            }
        }
        return pkgPubList;
    }

    /**
     * Use this method to compare the Publisher list with another PublisherList and
     * provide a Map containing PUBLISHER_MODIFIED, PUBLISHER_INSERTED and PUBLISHER_DELETED
     * List of {@link LocalPublisherTuple}
     *
     * @param comparedPublisherList - Publisher list to compare
     * @return Map containing PUBLISHER_MODIFIED, PUBLISHER_INSERTED and PUBLISHER_DELETED
     * List of {@link LocalPublisherTuple}
     */
    public Map<String, List<LocalPublisherTuple>> getDiffOfPublisherList(PublisherList comparedPublisherList) {
        if(LOG_DEBUG) Log.d(TAG,"getDiffOfPublisherList ThisList : " + this.toString() +
                                " comparedPublisherList : " + comparedPublisherList);
        Map<String, List<LocalPublisherTuple>> diffPubList = new HashMap<String, List<LocalPublisherTuple>>();
        ArrayList<LocalPublisherTuple> modifiedPubList = new ArrayList<LocalPublisherTuple>();
        ArrayList<LocalPublisherTuple> insertedPubList = new ArrayList<LocalPublisherTuple>();
        ArrayList<LocalPublisherTuple> deletedPubList = new ArrayList<LocalPublisherTuple>();

        ArrayList<String> modifiedPubNameList = new ArrayList<String>();
        ArrayList<String> insertedPubNameList = new ArrayList<String>();
        ArrayList<String> deletedPubNameList = new ArrayList<String>();
        diffPubList.put(PUBLISHER_MODIFIED, modifiedPubList);
        diffPubList.put(PUBLISHER_INSERTED, insertedPubList);
        diffPubList.put(PUBLISHER_DELETED, deletedPubList);
        for(Entry<String, LocalPublisherTuple> pubEntry: this.entrySet()) {
            String pubKey = pubEntry.getKey();
            if(comparedPublisherList.containsKey(pubKey)) {
                if(!pubEntry.getValue().equals(comparedPublisherList.get(pubKey))) {
                    modifiedPubList.add(pubEntry.getValue());
                    modifiedPubNameList.add(pubKey);
                }
            } else {
                insertedPubList.add(pubEntry.getValue());
                insertedPubNameList.add(pubKey);
            }
        }
        for(Entry<String, LocalPublisherTuple> pubEntry: comparedPublisherList.entrySet()) {
            String pubKey = pubEntry.getKey();
            if(!this.containsKey(pubKey)) {
                deletedPubList.add(pubEntry.getValue());
                deletedPubNameList.add(pubKey);
            }
        }
        if(LOG_DEBUG) Log.d(TAG,"getDiffOfPublisherList modifiedPubList : " + modifiedPubNameList.toString() +
                                " insertedPubList : " + insertedPubNameList + " deletedPubList : " + deletedPubNameList);
        return diffPubList;
    }
}
