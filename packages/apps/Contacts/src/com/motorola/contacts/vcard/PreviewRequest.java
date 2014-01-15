/**
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * This file was Created by contacts tech team for vcard preview feature on tablet
 */
package com.motorola.contacts.vcard;

import android.accounts.Account;
import android.os.Parcelable;
import android.os.Parcel;
import android.net.Uri;

import com.android.vcard.VCardSourceDetector;

/**
 * Class representing one request for previewing vCard (given as a Uri).
 *
 *
 * Note: This object's accepting only One Uri does NOT mean that
 * there's only one vCard entry inside the instance, as one Uri often has multiple
 * vCard entries inside it.
 */
public class PreviewRequest implements Parcelable{
    /**
     * Can be null (typically when there's no Account available in the system).
     */
    public final Account account;
    /**
     * Uri to be preview.
     */
    public final Uri uri;

    /**
     * Can be {@link VCardSourceDetector#PARSE_TYPE_UNKNOWN}.
     */
    public final int estimatedVCardType;
    /**
     * Can be null, meaning no preferable charset is available.
     */
    public final String estimatedCharset;
    /**
     * Assumes that one Uri contains only one version, while there's a (tiny) possibility
     * we may have two types in one vCard.
     *
     * e.g.
     * BEGIN:VCARD
     * VERSION:2.1
     * ...
     * END:VCARD
     * BEGIN:VCARD
     * VERSION:3.0
     * ...
     * END:VCARD
     *
     * We've never seen this kind of a file, but we may have to cope with it in the future.
     */
    public final int vcardVersion;

    /**
     * The count of vCard entries in {@link #uri}. A receiver of this object can use it
     * when showing the progress of import. Thus a receiver must be able to torelate this
     * variable being invalid because of vCard's limitation.
     *
     * vCard does not let us know this count without looking over a whole file content,
     * which means we have to open and scan over {@link #uri} to know this value, while
     * it may not be opened more than once (Uri does not require it to be opened multiple times
     * and may become invalid after its close() request).
     */
    public final int entryCount;
    public PreviewRequest(Account account,
            Uri uri, int estimatedType, String estimatedCharset,
            int vcardVersion, int entryCount) {
        this.account = account;
        this.uri = uri;
        this.estimatedVCardType = estimatedType;
        this.estimatedCharset = estimatedCharset;
        this.vcardVersion = vcardVersion;
        this.entryCount = entryCount;
    }

    public PreviewRequest(Parcel in) {
        this.account = Account.CREATOR.createFromParcel(in);
        this.uri = Uri.CREATOR.createFromParcel(in);;
        this.estimatedVCardType = in.readInt();
        this.estimatedCharset = in.readString();
        this.vcardVersion = in.readInt();
        this.entryCount = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if(account != null){
            account.writeToParcel(dest, flags);
        }
        if(uri != null){
            uri.writeToParcel(dest, flags);
        }
        dest.writeInt(estimatedVCardType);
        dest.writeString(estimatedCharset);
        dest.writeInt(vcardVersion);
        dest.writeInt(entryCount);
    }

    public static final Creator<PreviewRequest> CREATOR = new Creator<PreviewRequest>() {
        public PreviewRequest createFromParcel(Parcel source) {
            return new PreviewRequest(source);
        }

        public PreviewRequest[] newArray(int size) {
            return new PreviewRequest[size];
        }
    };

}
