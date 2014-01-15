package android_const.provider;

public class TelephonyConst {
    // Constants used by SMS content Observer
    public static final String SMS_CONTENT_URI      = "content://sms/";
    
    public interface SmsDbColumns {
        public static final String ADDRESS              = "address";
        public static final String PROTOCOL             = "protocol";
        public static final String TYPE                 = "type";
    }
    
    // Indicates Outbound SMS
    public static final String MESSAGE_TYPE_SENT = "2";

    public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
}
