package com.motorola.contracts.messaging;

/**
 * Intent defines extension constants to {@link android.content.Intent}.
 */
public class Intent {

    /**
     * Intent to bind to Messaging Send Service, used to send SMS/MMS in
     * background.
     *
     * This method can be used with {@link android.content.Intent#CATEGORY_DEFAULT}
     * to send SMS/MMS messages using Messaging Service.
     *
     * The interface to Messaging Send Service is especified through an AIDL.
     *
     * @see IMessagingService to more details about service interface and
     *                        bind options.
     */
    public static final String ACTION_SEND_MESSAGE = "com.motorola.contracts.messaging.intent.action.SEND_MESSAGE";

}
