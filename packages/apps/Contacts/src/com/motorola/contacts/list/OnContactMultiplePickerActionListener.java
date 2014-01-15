package com.motorola.contacts.list;

import android.net.Uri;

import com.android.contacts.list.OnContactPickerActionListener;

public interface OnContactMultiplePickerActionListener extends OnContactPickerActionListener {
    /**
     * Returns the selected contact to the requester.
     */
    void onToggleContactAction(Uri contactUri,boolean isChecked);

    /**
     * Notify container that data loading completed
     */
    void onContactLoadingCompletedAction();

    /**
     * check if select all is supported
     */
    boolean isSelectAllSupported(); //  MOT MOD - IKPIM-1026
}
