package com.motorola.contracts.messaging;

import android.net.Uri;
import com.motorola.contracts.messaging.Message;
import com.motorola.contracts.messaging.IMessagingServiceCallback;

/**
 * Interface to Messaging Bound Service, used to send SMS/MMS in background.
 *
 * To bind to this service use an Intent with
 * {@link com.motorola.contracts.messaging.Intent.ACTION_SEND_MESSAGE} action
 * and the default category ({@link android.content.Intent#CATEGORY_DEFAULT}).
 *
 * Ex.:
 *
 * Bind to service:
 *
 * <pre>
 *    public void onCreate(Bundle savedInstanceState) {
 *       ...
 *      Intent serviceIntent = new Intent(com.motorola.contracts.messaging.Intent.ACTION_SEND_MESSAGE);
 *      serviceIntent.addCategory(Intent.CATEGORY_DEFAULT);
 *      serviceConnected = getApplicationContext().bindService(
 *                          serviceIntent, mMessageServiceConnection,
 *                          Context.BIND_AUTO_CREATE);
 *      ...
 *    }
 * </pre>
 *
 * Since {@link android.content.Context#bindService()} is asynchronous, we use
 * the object mMessageServiceConnection that implements
 * {@link android.content.ServiceConnection} to receive bind result and keep
 * a reference to an implementation of this interface.
 *
 * Next, it is shown an example of {@link android.content.ServiceConnection}
 * implementation:
 *
 * <pre>
 *    private ServiceConnection mMessageServiceConnection = new ServiceConnection() {
 *
 *      public void onServiceConnected(ComponentName name, IBinder service) {
 *          Log.i(TAG, "Connected to service");
 *          mMessageService = IMessagingService.Stub.asInterface(service);
 *      }
 *
 *      public void onServiceDisconnected(ComponentName name) {
 *          Log.e(TAG, "Service has unexpectedly disconnected");
 *          mMessageService = null;
 *      }
 *  };
 * </pre>
 *
 * If bind is executed with no errors, onServiceConnected is called having
 * on its parameter list an object that can be used as a stub to access
 * services files. Otherwise, a null service object is returned.
 */
interface IMessagingService {

    /**
     * Send a SMS/MMS message to a list of recipients.
     *
     * A call to this method will generate a send request that will be identified
     * by the integer returned by this method. Since request processing is an
     * asynchronous task, this method requires a callback object. This callback
     * is called to inform the caller of request processing status.
     *
     * To retrieve the Uri generated for each send message rely on
     * {@link com.motorola.contracts.messaging.IMessagingServiceCallback#onSendRequestProcessed}.
     */
    int sendMessage(in Message message, in IMessagingServiceCallback callback, int flags);
}

