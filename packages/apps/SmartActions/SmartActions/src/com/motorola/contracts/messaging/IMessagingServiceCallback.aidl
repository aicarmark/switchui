package com.motorola.contracts.messaging;

import android.net.Uri;
import com.motorola.contracts.messaging.Message;

/**
 * Implements this interface to receive status of a SMS/MMS send request.

 * @see IMessagingService for more details about sending messages using
 *                        Messaging services.
 */
interface IMessagingServiceCallback {

    /**
     * This method is called by Messaging Service in response to a message send
     * request that was processed.
     *
     * On the moment of this method call, the message was not sent yet, it was
     * only included on Messaging database and is ready to be sent by
     * SMS/MMS system.
     *
     * Messaging will generate a different URI for each recipient found in
     * {@link com.motorola.contracts.messaging.Message}. Therefore, this method
     * may be called by service more than one time, passing one
     * of these created URI on each call
     *
     * @param uri of the message copy that will be sent to recipient
     * @param requestId is an integer that identify the send request.
     * @param recipient for whom the message will be sent.
     */
    void onSendRequestProcessed(in Uri uri, int requestId, String recipient);

    /**
     * Method called when Messaging Service cannot process a Send Request
     * due to errors on request or some violated constraint (Max message size,
     * invalid recipientes etc).
     *
     * This method is called for each failed recipient, in case of recipient
     * specific fails, or only one time in case of fail of entire process.
     *
     * @param requestId Id of request which generated the fail.
     * @param recipient In case of a fail specific for a recipient, this param
     *                  holds the recipient address.
     * @param errorNo describe the cause of fail, value for this parameter is
     *                one of constants defined in
     *                {@link com.motorola.contracts.messaging.MessagingServiceConstatns}
     */
    void onSendRequestFailed(int requestId, String recipient, int errorNo);

}