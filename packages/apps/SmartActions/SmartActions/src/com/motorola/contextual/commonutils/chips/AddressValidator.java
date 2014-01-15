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

import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.widget.AutoCompleteTextView.Validator;

/**
 * This class implements validator for Chips UI
 *
 * <code><pre>
 * CLASS:
 *     Implements Validator
 *
 * RESPONSIBILITIES:
 *     Implements validator for Chips UI
 *
 * COLLABORATORS:
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class AddressValidator implements Validator {
    public CharSequence fixText(CharSequence invalidText) {
        return "";
    }

    public boolean isValid(CharSequence text) {
        if (text != null && text.toString().trim().length() > 0) {
            if (AddressUtil.isMessagableNumber(text.toString())) return true;

            Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(text);
            if (tokenized != null && tokenized.length > 0)
                if (AddressUtil.isMessagableNumber(tokenized[0].getAddress())) return true;
        }
        return false;
    }
}
