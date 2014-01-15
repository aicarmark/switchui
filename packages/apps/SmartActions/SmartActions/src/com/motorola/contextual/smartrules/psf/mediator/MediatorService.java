/*
 * @(#)MediatorService.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import java.util.List;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.mediator.protocol.CancelRequest;
import com.motorola.contextual.smartrules.psf.mediator.protocol.CancelResponse;
import com.motorola.contextual.smartrules.psf.mediator.protocol.IMediatorProtocol;
import com.motorola.contextual.smartrules.psf.mediator.protocol.InitiateRefresh;
import com.motorola.contextual.smartrules.psf.mediator.protocol.ListRequest;
import com.motorola.contextual.smartrules.psf.mediator.protocol.ListResponse;
import com.motorola.contextual.smartrules.psf.mediator.protocol.Notify;
import com.motorola.contextual.smartrules.psf.mediator.protocol.RefreshRequest;
import com.motorola.contextual.smartrules.psf.mediator.protocol.RefreshResponse;
import com.motorola.contextual.smartrules.psf.mediator.protocol.SubscribeRequest;
import com.motorola.contextual.smartrules.psf.mediator.protocol.SubscribeResponse;

/**
 * Intent service to handle various commands/command responses between publishers and SmartActions
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends IntentService
 *
 * RESPONSIBILITIES:
 * Handle command/command response intents
 *
 * COLABORATORS:
 *     MediatorReceiver = starts this service when a command is received
 *     Consumer - Uses the preconditions available across the system
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MediatorService extends IntentService implements MediatorConstants {

    private static final String TAG =  MediatorService.class.getSimpleName();

    /**
     * Default constructor
     */
    public MediatorService() {
        super("MediatorService");
    }

    /**
     * Constructor with name of the worker thread as an argument
     * @param name Name of the worker thread
     */
    public MediatorService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (PsfConstants.LOG_DEBUG || Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Inbound intent " + intent.toUri(0));

        IMediatorProtocol mediatorCommand = createMediatorCommand(intent);
        if (mediatorCommand != null) {
            Context context = getApplicationContext();

            if (!mediatorCommand.processRequest(context, intent)) {
                Log.e(TAG, "Unable to process request");
                return;
            }

            List<Intent> intentsToBroadcast = mediatorCommand.execute(context, intent);
            if (intentsToBroadcast != null) {
                for (Intent intentToBroadcast : intentsToBroadcast) {
                    intentToBroadcast.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                    if (MediatorHelper.isRequestForMotion(intentToBroadcast) &&
                            MediatorHelper.isMDMPresent(context) &&
                            !MediatorHelper.isNewArchMD(context)) {
                        // If the intent is for Motion detector
                        // and the adapter is functional then set this intent
                        // to the package containing the adapter
                        intentToBroadcast.setPackage(PACKAGE);
                    }

                    if (PsfConstants.LOG_DEBUG || Log.isLoggable(TAG,  Log.DEBUG)) Log.d(TAG, "Outbound intent " + intentToBroadcast.toUri(0));
                    context.sendBroadcast(intentToBroadcast, getPermission(intentToBroadcast));
                }
            }
        } else {
            Log.e(TAG, "Cannot create command object");
        }
    }

    /** Returns the permission to be used for the broadcast based on the passed in event
     *
     * @param intent
     * @return
     */
    private static String getPermission(Intent intent) {

        // There is one place in mediator where we mimic cancel requests
        // when consumers get uninstalled.  Here we have like a user of CP
        if (intent.getAction().equals(REQUEST)) {
            return PERM_CONDITION_PUBLISHER_ADMIN;
        }

        // At all other places we either send a request to CP
        // or send a response/asynchronous notify/asynchronous refresh to CP user
        String event = intent.getStringExtra(EXTRA_EVENT_TYPE);
        String permission = null;
        if (event != null) {
            if (event.equals(SUBSCRIBE_EVENT) ||
                    event.equals(CANCEL_EVENT)    ||
                    event.equals(REFRESH_EVENT)   ||
                    event.equals(LIST_EVENT)) {

                // All communication towards Condition Publisher
                permission = PERM_CONDITION_PUBLISHER;

            } else if (event.equals(SUBSCRIBE_RESPONSE_EVENT) ||
                       event.equals(CANCEL_RESPONSE_EVENT)    ||
                       event.equals(REFRESH_RESPONSE_EVENT)   ||
                       event.equals(NOTIFY_EVENT)             ||
                       event.equals(INITIATE_REFRESH)         ||
                       event.equals(LIST_RESPONSE_EVENT)) {

                // All communication towards the users of condition publisher
                permission = PERM_CONDITION_PUBLISHER_USER;
            }
        }
        return permission;
    }

    /**
     * Create a mediator command object based on the incoming intent
     * @param intent Incoming intent
     * @return Mediator command object to handle the intent
     */
    private IMediatorProtocol createMediatorCommand (Intent intent) {
        String action = intent.getAction();
        IMediatorProtocol mediatorCommand = null;

        if (action != null) {

            if (action.equals(REQUEST) ||
                    action.equals(EVENT)) {
                mediatorCommand = getMediatorCommandForEvent(intent);

            } else if(action.equals(RESET) ||
                      (action.equals(UPDATED) &&
                       intent.hasExtra(EXTRA_PUBLISHER_REMOVED_LIST))) {
                mediatorCommand = new PublisherRemoved();

            } else if (action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED) ||
                       action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                mediatorCommand = new ConsumerRemoved(intent);

            } else if (action.equals(ACTION_MEDIATOR_INIT)) {
                mediatorCommand = new MediatorInit();
            }

        } else {
            Log.e(TAG, "Null action, intent:"+(intent.toURI().toString()));        
        }
        return mediatorCommand;
    }

    /**
     * Method to return the type of mediator command needed to handle a particular event
     * @param intent Intent to be handled
     * @return Mediator command
     */
    private IMediatorProtocol getMediatorCommandForEvent (Intent intent) {
        IMediatorProtocol mediatorCommand = null;
        String event = intent.getStringExtra(EXTRA_EVENT);

        if (event != null) {
            if (event.equals(SUBSCRIBE_EVENT)) {
                mediatorCommand = new SubscribeRequest(intent);

            } else if (event.equals(CANCEL_EVENT)) {
                mediatorCommand = new CancelRequest(intent);

            } else if (event.equals(REFRESH_EVENT)) {
                mediatorCommand = new RefreshRequest(intent);

            } else if (event.equals(LIST_EVENT)) {
                mediatorCommand = new ListRequest(intent);

            } else if (event.equals(SUBSCRIBE_RESPONSE_EVENT)) {
                mediatorCommand = new SubscribeResponse(intent);

            } else if (event.equals(CANCEL_RESPONSE_EVENT)) {
                mediatorCommand = new CancelResponse(intent);

            } else if (event.equals(REFRESH_RESPONSE_EVENT)) {
                mediatorCommand = new RefreshResponse(intent);

            } else if (event.equals(LIST_RESPONSE_EVENT)) {
                mediatorCommand = new ListResponse(intent);

            } else if (event.equals(NOTIFY_EVENT)) {
                mediatorCommand = new Notify(intent);

            } else if (event.equals(INITIATE_REFRESH)) {
                mediatorCommand = new InitiateRefresh(intent);

            } else {
                Log.w(TAG, "Unknown event");
            }

        } else {
            Log.e(TAG, "Event is null");
        }

        return mediatorCommand;
    }

}
