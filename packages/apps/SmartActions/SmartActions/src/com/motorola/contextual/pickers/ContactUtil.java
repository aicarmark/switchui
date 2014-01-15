/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/29 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

import com.motorola.contextual.commonutils.StringUtils;

/**
 * This utility class aides the construction of contacts widget strings.
 * <code><pre>
 *
 * CLASS:
 *  extends Object - standard Android object
 *
 * RESPONSIBILITIES:
 *  Provide contacts widget string construction methods.
 *
 * COLLABORATORS:
 *  ContactsChooserFragment - Presents contacts widget UI.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class ContactUtil {
    protected static final String TAG = ContactUtil.class.getSimpleName();

    /**
     * Separators for constructing a contacts widget string.
     */
    public interface ContactDelimiter {
        /** Separator that ends an individual contact description */
        String SEPARATOR = StringUtils.COMMA_STRING;
        /** Delimiter at beginning of contact name */
        String NAME_BEGIN = "\"";
        /** Delimiter at end of contact name */
        String NAME_END = "\"";
        /** Delimiter at beginning of contact phone number */
        String NUMBER_BEGIN = "<";
        /** Delimiter at end of contact phone number */
        String NUMBER_END = ">";
    }

    /** Flag value that indicates a contact who exists in the phonebook */
    public static final String CONTACT_KNOWN = "1";

    /**
     * Constructs an individual contact input string, complete with terminating separator.
     * <P>
     * For instance: "Contact Name" <5551234567>,
     * In the above example, double quotes surround the contact name,
     * angle brackets surround the phone number, and a comma always terminates the pair.
     *
     * @param sb Result is appended to this StringBuilder; must not be null
     * @param name Contact name
     * @param num Contact phone number
     * @return The same modified StringBuilder that was passed in an a parameter
     */
    public static StringBuilder appendContactString(final StringBuilder sb, final String name,
            final String num) {
        sb.append(ContactDelimiter.NAME_BEGIN);
        sb.append(name);
        sb.append(ContactDelimiter.NAME_END);
        sb.append(ContactDelimiter.NUMBER_BEGIN);
        sb.append(num);
        sb.append(ContactDelimiter.NUMBER_END);
        sb.append(ContactDelimiter.SEPARATOR);
        return sb;
    }

    /**
     * Constructs an individual contact input string, complete with terminating separator.
     * <P>
     * For instance: "Contact Name" <5551234567>,
     * In the above example, double quotes surround the contact name,
     * angle brackets surround the phone number, and a comma always terminates the pair.
     *
     * @param name Contact name
     * @param num Contact phone number
     * @return Contact input string
     */
    public static String getContactString(final String name, final String num) {
        final StringBuilder sb = new StringBuilder();
        return appendContactString(sb, name, num).toString();
    }

    /**
     * Builds contacts widget-compatible string by merging a names list string
     * and a phone numbers list string (comma-separated values).
     *
     * @param namesList Contact names list string (comma-separated values)
     * @param phoneNumbersList Contact phone numbers list string (comma-separated values)
     * @return Contacts widget-compatible string
     */
    public static String buildContactsString(final String namesList, final String phoneNumbersList) {
        StringBuilder sbContacts = new StringBuilder();
        if ((namesList != null) && (phoneNumbersList != null)) {
            // Separate individual names
            final String[] namesArr = namesList.split(ContactDelimiter.SEPARATOR);
            // Separate individual phone numbers
            final String[] numbersArr = phoneNumbersList.split(ContactDelimiter.SEPARATOR);
            // Construct individual name-number pairs
            for (int index = 0; index < numbersArr.length; ++index) {
                final String num = numbersArr[index];
                final boolean hasPhone = !num.trim().isEmpty();
                if (hasPhone) {
                    String name = num;
                    if (index < namesArr.length) {
                        name = namesArr[index];
                    }
                    sbContacts.append(getContactString(name, num));
                }
            }
        }
        return sbContacts.toString();
    }
}
