/*
 * @(#)PafInitHandler.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 * a21345		 2012/03/26					  Paf changes to support White list,
 * 											  black list, Package install / remove / replaced
 *
 */
package com.motorola.contextual.smartrules.psf.paf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.IntentHandler;
import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTuple;

/** IntentHandler class for handling intents for PAF
 *<code><pre>
 * CLASS:
 *     PafInitHandler Extends IntentHandler
 * Interface:
 * 		PafConstants
 *
 * RESPONSIBILITIES:
 * 		Handles ACTION_PSF_INIT, ACTION_LOCALE_CHANGE, ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED
 * 		ACTION_PACKAGE_REPLACED, ACTION_MY_PACKAGE_REPLACED and updates PublisherProvider
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PafInitHandler extends IntentHandler  implements PafConstants {

    private static final String PERMEISSION_RECEIVE_PUBLISHER_UPDATES = "com.motorola.smartactions.permission.RECEIVE_PUBLISHER_UPDATES";

    private static String TAG = PsfConstants.PSF_PREFIX + PafInitHandler.class.getSimpleName();

    private String publisherUpdateReason;

    public PafInitHandler(Context context, final Intent intent) {
        super(context, intent);
    }

    @Override
    public boolean handleIntent() {
        if (LOG_DEBUG) Log.d(TAG, "Handling intent " + getIntent().toUri(0));
        Intent intent = getIntent();
        String action = intent.getAction();
        String pkgName = null;
        boolean initPsr = false;
        if(action == null) return false;
        if(action.equals(ACTION_PSF_INIT)) {
            handlePsfInit();
            initPsr = true;
        } else if(action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            handleLocaleChange();
            initPsr = true;
        } else if(action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if(!isReplacing) {
                Uri packageData = intent.getData();
                if (LOG_DEBUG) Log.d(TAG, "handleIntent ACTION_PACKAGE_ADDED packageData : " + packageData);
                if(packageData != null) {
                    pkgName = packageData.getSchemeSpecificPart();
                    handlePackageAdded(pkgName);
                    initPsr = true;
                }
            }
        } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if(!isReplacing) {
                Uri packageData = intent.getData();
                if (LOG_DEBUG) Log.d(TAG, "handleIntent ACTION_PACKAGE_REMOVED packageData : " + packageData);
                if(packageData != null) {
                    pkgName = packageData.getSchemeSpecificPart();
                    handlePackageRemoved(pkgName);
                    initPsr = true;
                }
            }
        } else if(action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Uri packageData = intent.getData();
            if(packageData != null) {
                pkgName = packageData.getSchemeSpecificPart();
                if(!pkgName.equals(mContext.getPackageName())) {
                    handlePackageReplaced(pkgName);
                    initPsr = true;
                }
            }
        } else if(action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            handleMyPackageReplaced();
            pkgName = mContext.getPackageName();
            //  Need not initialize PSR as such an initialization would have happend
            //  via PSF_INIT path
        } else if(action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED)) {
            Uri packageData = intent.getData();
            if(packageData != null) {
                pkgName = packageData.getSchemeSpecificPart();
                if(!pkgName.equals(mContext.getPackageName())) {
                    handlePackageDataCleared(pkgName);
                    initPsr = true;
                }
            }
        } else {
            if (LOG_DEBUG) Log.d(TAG, "unhandled action " + action);
        }
        if(initPsr) initializePsr(action, pkgName);
        return true;
    }

    /*
     * When ACTION_PACKAGE_DATA_CLEARED intent is received for the package containing Publishers
     * for action publisher sends ACTION_PUBLISHER_DATA_RESET,
     * for condition publisher sends CONDITION_PUBLISHER_DATA_RESET and
     * for rule publisher sends RULE_PUBLISHER_DATA_RESET
     */
    private void handlePackageDataCleared(String pkgName) {
        List<String> whiteList = new PublisherFilterList(WHITELIST_DEFAULT_XML_FILE_PATH);
        if(whiteList.contains(pkgName)) {
            PublisherList ppPubList = PublisherList.getPublisherList(mContext,
                                      PUBLISHER_PROVIDER_LIST,pkgName);

            ArrayList<String> actionPubList = new ArrayList<String>();
            ArrayList<String> condPubList = new ArrayList<String>();
            ArrayList<String> rulePubList = new ArrayList<String>();

            if(!ppPubList.isEmpty()) {
                for(Entry<String, LocalPublisherTuple> pubEntry: ppPubList.entrySet()) {
                    LocalPublisherTuple pubTuple = pubEntry.getValue();
                    if(pubTuple.getType().equals(LocalPublisherTable.Type.ACTION)) {
                        actionPubList.add(pubEntry.getKey());
                    } else if(pubTuple.getType().equals(LocalPublisherTable.Type.CONDITION)) {
                        condPubList.add(pubEntry.getKey());
                    } else if(pubTuple.getType().equals(LocalPublisherTable.Type.RULE)) {
                        rulePubList.add(pubEntry.getKey());
                    }
                }
                if(LOG_DEBUG) Log.d(TAG,"handlePackageDataCleared : actionPubList = " + actionPubList +
                                        "\n condPubList = " + condPubList + "\n rulePubList = " + rulePubList);
                Intent actPubIntent = new Intent (ACTION_PUBLISHER_DATA_RESET);
                if(actionPubList.size() > 0) {
                    actPubIntent.putExtra(EXTRA_PUBLISHER_KEY_LIST, actionPubList);
                    mContext.sendBroadcast(actPubIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
                }

                Intent condPubIntent = new Intent (CONDITION_PUBLISHER_DATA_RESET);
                if(condPubList.size() > 0) {
                    condPubIntent.putExtra(EXTRA_PUBLISHER_KEY_LIST, condPubList);
                    mContext.sendBroadcast(condPubIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
                }

                Intent rulePubIntent = new Intent (RULE_PUBLISHER_DATA_RESET);
                if(rulePubList.size() > 0) {
                    rulePubIntent.putExtra(EXTRA_PUBLISHER_KEY_LIST, rulePubList);
                    mContext.sendBroadcast(rulePubIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
                }
            }
        }
    }

    /*
     * Handles the ACTION_MY_PACKAGE_REPLACED intent and updates PublisherProvider
     */
    private void handleMyPackageReplaced() {
        String pkgName = mContext.getPackageName();
        handlePackageReplaced(pkgName);
    }

    /*
     * Handles the ACTION_PACKAGE_REPLACED intent for the given package name
     *  and updates PublisherProvider
     */
    private void handlePackageReplaced(String pkgName) {
        List<String> whiteList = new PublisherFilterList(WHITELIST_DEFAULT_XML_FILE_PATH);
        if(whiteList.contains(pkgName)) {
            PublisherList pkgMgrPubList = PublisherList.getPublisherList(mContext,
                                          PACKAGE_MANAGER_LIST,pkgName);
            PublisherList ppPubList = PublisherList.getPublisherList(mContext,
                                      PUBLISHER_PROVIDER_LIST,pkgName);
            Map<String, List<LocalPublisherTuple>> diffPubList = pkgMgrPubList.getDiffOfPublisherList(ppPubList);
            List<LocalPublisherTuple> modifiedPubList = diffPubList.get(PUBLISHER_MODIFIED);
            List<LocalPublisherTuple> insertedPubList = diffPubList.get(PUBLISHER_INSERTED);
            List<LocalPublisherTuple> deletedPubList = diffPubList.get(PUBLISHER_DELETED);
            publisherUpdateReason = PUBLISHER_RELPACED;
            processDiffPublisherListPokeRv(modifiedPubList, insertedPubList, deletedPubList);
        }
    }

    /*
     * Handles the ACTION_PACKAGE_REMOVED intent for the given package name
     *  and updates PublisherProvider
     */
    private void handlePackageRemoved(String pkgName) {
        List<String> whiteList = new PublisherFilterList(WHITELIST_DEFAULT_XML_FILE_PATH);
        if(LOG_DEBUG) Log.d(TAG,"handlePackageRemoved called for pkgName : " + pkgName +
                                " whiteList : " + whiteList);
        if(whiteList.contains(pkgName)) {
            PublisherList ppPubList = PublisherList.getPublisherList(mContext,
                                      PUBLISHER_PROVIDER_LIST,pkgName);
            if(!ppPubList.isEmpty()) {
                ArrayList<LocalPublisherTuple> deletedPubList = new ArrayList<LocalPublisherTuple>();
                for(Entry<String, LocalPublisherTuple> ppPubEntry : ppPubList.entrySet()) {
                    deletedPubList.add(ppPubEntry.getValue());
                }
                publisherUpdateReason = PUBLISHER_UNINSTALLED;
                processDiffPublisherListPokeRv(null, null, deletedPubList);
            }
        }
    }

    /*
     * Handles the ACTION_PACKAGE_ADDED intent for the given package name
     *  and updates PublisherProvider
     */
    private void handlePackageAdded(String pkgName) {
        List<String> whiteList = new PublisherFilterList(WHITELIST_DEFAULT_XML_FILE_PATH);
        if(LOG_DEBUG) Log.d(TAG,"handlePackageAdded called for pkgName : " + pkgName +
                                " whiteList : " + whiteList);
        if(whiteList.contains(pkgName)) {
            PublisherList pkgMgrPubList = PublisherList.getPublisherList(mContext,
                                          PACKAGE_MANAGER_LIST,pkgName);
            ArrayList<LocalPublisherTuple> insertedPubList = new ArrayList<LocalPublisherTuple>();
            for(Entry<String, LocalPublisherTuple> pkgMgrPubEntry : pkgMgrPubList.entrySet()) {
                insertedPubList.add(pkgMgrPubEntry.getValue());
            }
            publisherUpdateReason = PUBLISHER_INSTALLED;
            processDiffPublisherListPokeRv(null, insertedPubList, null);
        }
    }

    /*
     * Initialize PSR
     */
    private void initializePsr(String launchCmd, String pkgName) {
        Intent  intent = new Intent(PsfConstants.ACTION_PSR_INIT);
        intent.putExtra(EXTRA_PACKAGE_NAME, pkgName);
        intent.putExtra(EXTRA_PSR_LAUNCH_COMMAND, launchCmd);
        mContext.sendBroadcast(intent);
    }

    /*
     * Handles ACTION_LOCALE_CHANGE intent and updates the PublisherProvider
     */
    private void handleLocaleChange() {
        PublisherList pkgMgrPubList = PublisherList.getPublisherList(mContext, PACKAGE_MANAGER_LIST);

        ArrayList<LocalPublisherTuple> modifiedPubList = new ArrayList<LocalPublisherTuple>();
        for(Entry<String, LocalPublisherTuple> pkgMgrPubEntry : pkgMgrPubList.entrySet()) {
            LocalPublisherTuple tuple = pkgMgrPubEntry.getValue();
            Log.d(TAG, "handleLocaleChange  " + tuple.getPublisherKey());
            modifiedPubList.add(tuple);
        }
        publisherUpdateReason = LOCALE_CHANGED;
        processDiffPublisherListPokeRv(modifiedPubList, null, null);
    }

    /*
     * This method is called whenever the PublisherProvider is initialized. ACTION_PSF_INIT is
     * sent when PublisherProvider is initialized. This method checks if there is any change between
     * the PackageManger PublisherList and PublisherProvider PublisherList and updates the difference
     * in PublisherProvider
     */
    private void handlePsfInit() {
        PublisherList pkgPubList = PublisherList.getPublisherList(mContext, PACKAGE_MANAGER_LIST);
        PublisherList pubProviderList = PublisherList.getPublisherList(mContext, PUBLISHER_PROVIDER_LIST);
        if(LOG_DEBUG) Log.d(TAG,"handlePsfInit pkgPubList : " + pkgPubList.toString() +
                                " pubProviderList : " + pubProviderList.toString());
        Map<String, List<LocalPublisherTuple>> diffPubList = pkgPubList.getDiffOfPublisherList(pubProviderList);
        List<LocalPublisherTuple> modifiedPubList = diffPubList.get(PUBLISHER_MODIFIED);
        List<LocalPublisherTuple> insertedPubList = diffPubList.get(PUBLISHER_INSERTED);
        List<LocalPublisherTuple> deletedPubList = diffPubList.get(PUBLISHER_DELETED);
        publisherUpdateReason = PUBLISHER_RESTARTED;
        processDiffPublisherListPokeRv(modifiedPubList, insertedPubList, deletedPubList);
    }

    /*
     * This method will be called when PublisherProvider is empty, so it updates the
     * PulisherProvider with the PackageManager PublisherList
     */
    private void insertToPpPublisherList(PublisherList pkgPubList) {
        PublisherProviderUpdator ppUpdator = new PublisherProviderUpdator(mContext);
        for(Entry<String, LocalPublisherTuple> pubEntry : pkgPubList.entrySet()) {
            LocalPublisherTuple pubTuple = pubEntry.getValue();
            ppUpdator.insertPublisher(pubTuple);
        }
    }

    /*
     * This method updates the PulisherProvider with the difference between PackageManager PublisherList
     * and PublisherProvider PublisherList. It also pokes RulesValidator with the list of Publishers
     */
    private void processDiffPublisherListPokeRv(List<LocalPublisherTuple> modifiedPubList,
            List<LocalPublisherTuple> insertedPubList,
            List<LocalPublisherTuple> deletedPubList) {
        if(LOG_DEBUG) Log.d(TAG,"processDiffPublisherListPokeRv called with modifiedPubList : " + modifiedPubList
                                + "\n insertedPubList : " + insertedPubList + "\n deletedPubList : " + deletedPubList);
        PublisherProviderUpdator ppUpdator = new PublisherProviderUpdator(mContext);

        ArrayList<String> actionPubModList = new ArrayList<String>();
        ArrayList<String> actionPubAddList = new ArrayList<String>();
        ArrayList<String> actionPubRemList = new ArrayList<String>();

        ArrayList<String> condPubModList = new ArrayList<String>();
        ArrayList<String> condPubAddList = new ArrayList<String>();
        ArrayList<String> condPubRemList = new ArrayList<String>();

        ArrayList<String> rulePubModList = new ArrayList<String>();
        ArrayList<String> rulePubAddList = new ArrayList<String>();
        ArrayList<String> rulePubRemList = new ArrayList<String>();


        if(modifiedPubList != null && modifiedPubList.size() > 0) {
            for(LocalPublisherTuple pubTuple : modifiedPubList) {
                if(pubTuple.getType().equals(LocalPublisherTable.Type.ACTION)) {
                    if(pubTuple.isBlackListed() == LocalPublisherTable.BlackList.FALSE)
                        actionPubModList.add(pubTuple.getPublisherKey());
                    else
                        actionPubRemList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.CONDITION)) {
                    if(pubTuple.isBlackListed() == LocalPublisherTable.BlackList.FALSE)
                        condPubModList.add(pubTuple.getPublisherKey());
                    else
                        condPubRemList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.RULE)) {
                    if(pubTuple.isBlackListed() == LocalPublisherTable.BlackList.FALSE)
                        rulePubModList.add(pubTuple.getPublisherKey());
                    else
                        rulePubRemList.add(pubTuple.getPublisherKey());
                }
                ppUpdator.updatePublisher(pubTuple);
            }
        }

        if(insertedPubList != null && insertedPubList.size() > 0) {
            for(LocalPublisherTuple pubTuple : insertedPubList) {
                if(pubTuple.getType().equals(LocalPublisherTable.Type.ACTION)) {
                    actionPubAddList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.CONDITION)) {
                    condPubAddList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.RULE)) {
                    rulePubAddList.add(pubTuple.getPublisherKey());
                }
                ppUpdator.insertPublisher(pubTuple);
            }
        }


        if(deletedPubList != null && deletedPubList.size() > 0) {
            for(LocalPublisherTuple pubTuple : deletedPubList) {
                if(pubTuple.getType().equals(LocalPublisherTable.Type.ACTION)) {
                    actionPubRemList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.CONDITION)) {
                    condPubRemList.add(pubTuple.getPublisherKey());
                } else if(pubTuple.getType().equals(LocalPublisherTable.Type.RULE)) {
                    rulePubRemList.add(pubTuple.getPublisherKey());
                }
                ppUpdator.deletePublisher(pubTuple.getPublisherKey());
            }
        }

        Intent actPubUpdIntent = new Intent (ACTION_PUBLISHER_UPDATED);
        if(actionPubModList.size() > 0)
            actPubUpdIntent.putExtra(EXTRA_PUBLISHER_MODIFIED_LIST, actionPubModList);
        if(actionPubAddList.size() > 0)
            actPubUpdIntent.putExtra(EXTRA_PUBLISHER_ADDED_LIST, actionPubAddList);
        if(actionPubRemList.size() > 0)
            actPubUpdIntent.putExtra(EXTRA_PUBLISHER_REMOVED_LIST, actionPubRemList);

        Intent condPubUpdIntent = new Intent (CONDITION_PUBLISHER_UPDATED);
        if(condPubModList.size() > 0)
            condPubUpdIntent.putExtra(EXTRA_PUBLISHER_MODIFIED_LIST, condPubModList);
        if(condPubAddList.size() > 0)
            condPubUpdIntent.putExtra(EXTRA_PUBLISHER_ADDED_LIST, condPubAddList);
        if(condPubRemList.size() > 0)
            condPubUpdIntent.putExtra(EXTRA_PUBLISHER_REMOVED_LIST, condPubRemList);

        Intent rulePubUpdIntent = new Intent (RULE_PUBLISHER_UPDATED);
        if(rulePubModList.size() > 0)
            rulePubUpdIntent.putExtra(EXTRA_PUBLISHER_MODIFIED_LIST, rulePubModList);
        if(rulePubAddList.size() > 0)
            rulePubUpdIntent.putExtra(EXTRA_PUBLISHER_ADDED_LIST, rulePubAddList);
        if(rulePubRemList.size() > 0)
            rulePubUpdIntent.putExtra(EXTRA_PUBLISHER_REMOVED_LIST, rulePubRemList);

        if(actPubUpdIntent.hasExtra(EXTRA_PUBLISHER_MODIFIED_LIST) ||
                actPubUpdIntent.hasExtra(EXTRA_PUBLISHER_ADDED_LIST) ||
                actPubUpdIntent.hasExtra(EXTRA_PUBLISHER_REMOVED_LIST)) {
            actPubUpdIntent.putExtra(EXTRA_PUBLISHER_UPDATED_REASON, publisherUpdateReason);
            mContext.sendBroadcast(actPubUpdIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
        }

        if(condPubUpdIntent.hasExtra(EXTRA_PUBLISHER_MODIFIED_LIST) ||
                condPubUpdIntent.hasExtra(EXTRA_PUBLISHER_ADDED_LIST) ||
                condPubUpdIntent.hasExtra(EXTRA_PUBLISHER_REMOVED_LIST)) {
            condPubUpdIntent.putExtra(EXTRA_PUBLISHER_UPDATED_REASON, publisherUpdateReason);
            mContext.sendBroadcast(condPubUpdIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
        }

        if(rulePubUpdIntent.hasExtra(EXTRA_PUBLISHER_MODIFIED_LIST) ||
                rulePubUpdIntent.hasExtra(EXTRA_PUBLISHER_ADDED_LIST) ||
                rulePubUpdIntent.hasExtra(EXTRA_PUBLISHER_REMOVED_LIST)) {
            rulePubUpdIntent.putExtra(EXTRA_PUBLISHER_UPDATED_REASON, publisherUpdateReason);
            mContext.sendBroadcast(rulePubUpdIntent, PERMEISSION_RECEIVE_PUBLISHER_UPDATES);
        }
    }
}
