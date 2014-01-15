/*
 * @(#)AddressEditTextView.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/02/21  NA                  Initial version
 *
 */

package com.motorola.contextual.commonutils.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.widget.ListAdapter;

/**
 * This class customizes methods present in Chips UI RecipientEditTextView.
 *
 * <code><pre>
 * CLASS:
 *     Extends RecipientEditTextView
 *
 * RESPONSIBILITIES:
 *     Is a MultiAutoCompleteTextView used for entering and modifying contacts info
 *
 * COLLABORATORS:
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class AddressEditTextView extends RecipientEditTextView {
    private int mAlternatesLayout;

    private Tokenizer mTokenizer;

    public AddressEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChipDimensions(Drawable chipBackground,
            Drawable chipBackgroundPressed, Drawable invalidChip,
            Drawable chipDelete, Bitmap defaultContact, int moreResource,
            int alternatesLayout, float chipHeight, float padding,
            float chipFontSize, int copyViewRes) {
        super.setChipDimensions(chipBackground, chipBackgroundPressed, invalidChip,
                chipDelete, defaultContact, moreResource, alternatesLayout, chipHeight,
                padding, chipFontSize, copyViewRes);
        mAlternatesLayout = alternatesLayout;
    }

    @Override
    public void setTokenizer(Tokenizer tokenizer) {
        super.setTokenizer(tokenizer);
        mTokenizer = tokenizer;
    }

    @Override
    protected ListAdapter createAlternatesAdapter(RecipientChip chip) {
        String display = chip.getDisplay().toString();
        if (display.contains("/"))
            display = display.replace("/","");
        return new RecipientAlternatesAdapter(getContext(), chip.getContactId(), chip.getDataId(),
                display, mAlternatesLayout, this);
	}

	@Override
	String createAddressText(RecipientEntry entry) {
		String display = entry.getDisplayName();
        String address = entry.getDestination();
        if (AddressUtil.isMessagableNumber(address))
            address = PhoneNumberUtils.stripSeparators(address);
        if (address != null) {
            // Tokenize out the address in case the address already
            // contained the username as well.
            Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
            if (tokenized != null && tokenized.length > 0)
                address = tokenized[0].getAddress();
        }
        if (display != null) {
            // Tokenize out the display name in case the address already
            // contained the username as well.
            Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(display);
            if (tokenized != null && tokenized.length > 0) {
                String tempName = tokenized[0].getName();
                if (!TextUtils.isEmpty(tempName)) {
                    display = tempName;
                }
            }
        }
        if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
            display = null;
        }
        Rfc822Token token = new Rfc822Token(display, address, null);
        String trimmedDisplayText = token.toString().trim();
        int index = trimmedDisplayText.indexOf(",");
        return index < trimmedDisplayText.length() - 1 ? (String) mTokenizer
                .terminateToken(trimmedDisplayText) : trimmedDisplayText;
    }

}
