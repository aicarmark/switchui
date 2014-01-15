package com.android.contacts.calllog;

import com.android.contacts.R;

/**
 * Helper for CallLog ViewType.
 */
public class CallLogViewTypeHelper {
    static final int VIEW_OPTION_ALL = 0;
    static final int VIEW_OPTION_MISSED = 1;
    static final int VIEW_OPTION_RECEIVED = 2;
    static final int VIEW_OPTION_OUTGOING = 3;
    static final int VIEW_OPTION_VOICEMAIL = 4;
 
    // This list must match the list found in Contacts/res/values/arrays.xml
    final static int NETWORK_TYPE_ALL  = 0;
    final static int NETWORK_TYPE_CDMA = 1;
    final static int NETWORK_TYPE_GSM  = 2;

    /** Returns true if it is possible to place a call to the given number. */
    public static boolean needResetVoicemailFlag(int viewType) {
        return ((viewType == VIEW_OPTION_VOICEMAIL) || (viewType == VIEW_OPTION_ALL));
    }

    public static boolean needResetMissedCallFlag(int viewType) {
        return ((viewType == VIEW_OPTION_MISSED) || (viewType == VIEW_OPTION_ALL));
    }
	
    public static int setEmptyPrompt(int viewType) {
	int emptyPrompt = R.string.recentCalls_empty;
	switch (viewType) {
		case VIEW_OPTION_MISSED:
			emptyPrompt = R.string.missedCalls_empty;
			break;
	
		case VIEW_OPTION_RECEIVED:
			emptyPrompt = R.string.receivedCalls_empty;
			break;
	
		case VIEW_OPTION_OUTGOING:
			emptyPrompt = R.string.outgoingCalls_empty;
			break;

                case VIEW_OPTION_VOICEMAIL:
                        emptyPrompt = R.string.voicemail_empty;
	}
        return emptyPrompt;
    }

}

