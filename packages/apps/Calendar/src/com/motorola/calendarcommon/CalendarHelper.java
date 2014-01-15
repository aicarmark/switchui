package com.motorola.calendarcommon;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.net.Uri;

import com.motorola.calendarcommon.vcal.VCalUtils;

/**
 * Calendar helper class for external components.
 */
public class CalendarHelper {
    /**
     * Converts input vCal file to vCal-1.0 format
     *
     * @param context
     *        Application context the application is running in
     *
     * @param fileUri
     *        Uri for vCal file to be converted
     *
     * @return Uri of the new vCal-1.0 file or null if failed to
     *         convert
     *
     * @throws FileNotFoundException
     *         If the provided Uri could not be opened.
     *
     * @throws IOException
     *         If there are I/O errors.
     */
    public static Uri convertVcalToV1(final Context context, final Uri fileUri)
        throws FileNotFoundException, IOException {
        return VCalUtils.vCalV2ToV1(context, fileUri);
    }
}
