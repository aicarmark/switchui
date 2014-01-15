package com.motorola.contracts.messaging;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representation of a SMS/MMS handled by Messaging send service. 
 * 
 * @see IMessagingService for more details about sending SMS/MMS messages using
 *      Messaging.
 */
public class Message implements Parcelable {

    /**
     * Text that will be used as message body.
     */
    private String mMessageBody;

    /**
     * Message subject.
     */
    private String mSubject;

    /**
     * A list containing emails and/or phone numbers of this message recipients. 
     */
    private List<String> mRecipientList;

    /**
     * Create an empty message.
     */
    public Message() {
        mRecipientList = new ArrayList<String>();
    }

    /**
     * Add a new recipient for this message.
     * 
     * The recipient must be an email address or a phone number, otherwise 
     * service will not be able to send this message.
     * 
     * @param recipient An email or phone number.
     */
    public void addRecipient(String recipient) {
        mRecipientList.add(recipient);
    }

    /**
     * Returns a list of recipients associated to this class.
     *
     * @return recipients list.
     */
    public List<String> getRecipientList() {
        return mRecipientList;
    }

    /**
     * Get message body associated with this message.
     *
     * @return message body.
     */
    public String getMessageBody() {
        return mMessageBody;
    }

    /**
     * Set message body.
     *
     * @param messageBody
     */
    public void setMessageBody(String messageBody) {
        mMessageBody = messageBody;
    }

    /**
     * Get message subject.
     *
     * @return message subject
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Set subject string for this message.
     *
     * @param subject
     */
    public void setSubject(String subject) {
        mSubject = subject;
    }

    /**
     * @see Parcelable#describeContents()
     */
    public int describeContents() {
        return 0;
    }

    /**
     * @see Parcelable#writeToParcel(Parcel, int)
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMessageBody);
        dest.writeString(mSubject);
        dest.writeStringList(mRecipientList);
    }

    /**
     * @see Parcelable#CREATOR
     */
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        public Message createFromParcel(Parcel in) {
            Message message = new Message();
            message.readFromParcel(in);

            return message;
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    /**
     * Read Message data from a Parcel and stores it on a Message object fields.
     *
     * @param in Parcel with serialized Message data.
     */
    public void readFromParcel(Parcel in) {
        mMessageBody = in.readString();
        mSubject = in.readString();
        mRecipientList.clear();
        in.readStringList(mRecipientList);
    }
}
