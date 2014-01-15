package com.android.calendar.confinfo;

import com.android.calendar.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.database.Cursor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.util.Patterns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.android.calendar.confinfo.ConferenceCallInfo;
import com.android.calendar.confinfo.PhoneProjection;
import com.android.calendar.GeneralPreferences;
import java.util.Locale;

public class ConferenceCallInfoDataMgr {
    /**
     * Don't treat anything with fewer than this many digits as a phone number.
     */
    private static final int PHONE_NUMBER_MINIMUM_DIGITS = 5;

    private static final String[] US_TEL_NUMBER_PATTERN = {
        // examples: +1-847-327-1003, 847 327 1003, 1.847.327.1003, (847)3271003, 8473271003
        "(?:\\+?1[-. ]?)?(?:\\([2-9][0-9]{2}\\)|[2-9][0-9]{2})[-. ]?[2-9][0-9]{2}[-. ]?[0-9]{4}"
    };
    private static final String[] ZH_CN_TEL_NUMBER_PATTERN = {
        /* for land lines, using greedy match to find the number with maximum length */
        // examples: 0086-25-52148483, +86-(25)-52148483, 862552148483
        "(?:\\+|00)?86[-. ]?(?:\\(?:10|2[0-9]|[3-7,9][0-9]{2}|8[0-5,7-9]{2,3}\\)|(?:10|2[0-9]|[3-7,9][0-9]{2}|8[0-5,7-9]{2,3}))[-. ]?[0-9]{8}",
        // examples: 0086-25-5214848, +86-(25)-5214848, 86255214848
        "(?:\\+|00)?86[-. ]?(?:\\(?:10|2[0-9]|[3-7,9][0-9]{2}|8[0-5,7-9]{2,3}\\)|(?:10|2[0-9]|[3-7,9][0-9]{2}|8[0-5,7-9]{2,3}))[-. ]?[0-9]{7}",
        // examples: 025-52148483, (025)-52148483, 02552148483
        "(?:\\(?:010|02[0-9]|0[3-7,9][0-9]{2}|08[0-5,7-9][0-9]{2}|8[0-5,7-9]{2}\\)|(?:010|02[0-9]|0[3-7,9][0-9]{2}|08[0-5,7-9][0-9]{2}|8[0-5,7-9]{2}))[-. ]?[0-9]{8}",
        // examples: 025-5214848, (025)-5214848, 0255214848
        "(?:\\(?:010|02[0-9]|0[3-7,9][0-9]{2}|08[0-5,7-9][0-9]{2}|8[0-5,7-9]{2}\\)|(?:010|02[0-9]|0[3-7,9][0-9]{2}|08[0-5,7-9][0-9]{2}|8[0-5,7-9]{2}))[-. ]?[0-9]{7}",
        /* for mobile phones */
        // examples: 0086-135-6666-8888, +8613566668888
        "(?:\\+|00)?86[-. ]?1[3458][0-9][-. ]?[0-9]{4}[-. ]?[0-9]{4}",
        // examples: (0)135-6666-8888, 013566668888
        "(?:(?:\\(0\\)|0)[-. ]?)?1[3458][0-9][-. ]?[0-9]{4}[-. ]?[0-9]{4}",
    };
    private static final String default_phone_pattern = Patterns.PHONE.pattern();
    private static final String[] DEFAULT_TEL_NUMBER_PATTERN = {
        default_phone_pattern
    };

    private static final MatchFilter sSupportedLocaleMatchFilter = new MatchFilter() {
        public boolean acceptMatch(String s, int start, int end) {
            boolean bTelMatched = true;
            char c1, c2;

            // if the char proceeding the matched string is still a digit, then it's an invalid tel number;
            // if the matched string is proceeded with "#-", "#." or "# ", then it's an invalid tel number;
            // (here # stands for a digit number)
            if (start > 1) {
                c1 = s.charAt(start-1);
                c2 = s.charAt(start-2);
                if (Character.isDigit(c1) ||
                    (((c1 == ' ') || (c1 == '-') || (c1 == '.')) && Character.isDigit(c2))) {
                    bTelMatched = false;
                }
            } else if (start > 0) {
                c1 = s.charAt(start-1);
                if (Character.isDigit(c1)) {
                    bTelMatched = false;
                }
            }

            // if the char right after the matched string is still a digit, then it's an invalid tel number;
            // if "-#", ".#" or " #" is right after the matched string, then it's an invalid tel number;
            // (here # stands for a digit number)
            if (end < (s.length()-1)) {
                c1 = s.charAt(end);
                c2 = s.charAt(end+1);
                if (Character.isDigit(c1) ||
                    (((c1 == ' ') || (c1 == '-') || (c1 == '.')) && Character.isDigit(c2))) {
                    bTelMatched = false;
                }
            } else if (end < s.length()) {
                c1 = s.charAt(end);
                if (Character.isDigit(c1)) {
                    bTelMatched = false;
                }
            }

            return bTelMatched;
        }
    };

    private static final MatchFilter sDefaultMatchFilter = new MatchFilter() {
        public boolean acceptMatch(String s, int start, int end) {
            int digitCount = 0;

            for (int i = start; i < end; i++) {
                if (Character.isDigit(s.charAt(i))) {
                    digitCount++;
                    if (digitCount >= PHONE_NUMBER_MINIMUM_DIGITS) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    Context mContext = null;
    SharedPreferences mSharedPrefs = null;

    // indicates which kind of conference call info is being used
    private int mDataSourceFlag = ConferenceCallInfo.DATA_SOURCE_FLAG_INVALID;

    // conference call info from the meeting location
    private String mMeetingConfNumber = null;
    private String mMeetingConfId = null;
    // pattern that used to match conference number in the meeting location
    private String mMeetingNumberPattern = null;
    // the start position of the matched conference number
    private int mMeetingNumberIdx = -1;

    // conference call info from the contact db
    private String mContactConfNumber = null;
    private String mContactPhoneType = null;
    private String mContactDisplayName = null;

    // conference call info that's input by the user manually
    private String mManualConfNumber = null;
    private String mManualConfId = null;

    private interface MatchFilter {
        /**
         *  Examines the character span matched by the pattern and determines
         *  if the match should be accepted as a valid phone number.
         *
         *  @param s        The body of text against which the pattern
         *                  was matched
         *  @param start    The index of the first character in s that was
         *                  matched by the pattern - inclusive
         *  @param end      The index of the last character in s that was
         *                  matched - exclusive
         *
         *  @return         Whether this match should be accepted as a phone number
         */
        boolean acceptMatch(String s, int start, int end);
    };

    public ConferenceCallInfoDataMgr(Context ctx) {
        mContext = ctx;
        mSharedPrefs = GeneralPreferences.getSharedPreferences(ctx);
    }


    private void loadContactConfCallInfo(int contactDataId, int phoneType) {
        mContactDisplayName = null;
        mContactConfNumber = null;
        mContactPhoneType = null;

        ContentResolver cr = mContext.getContentResolver();
        Uri uri = ContentUris.withAppendedId(Phone.CONTENT_URI, contactDataId);

        String selection = Phone.TYPE + " = " + phoneType;
        Cursor c = cr.query(uri, PhoneProjection.PHONES_PROJECTION, selection, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    mContactDisplayName = c.getString(PhoneProjection.sPhoneDisplayNameIdx);
                    mContactConfNumber = c.getString(PhoneProjection.sPhoneNumberIdx);

                    if (phoneType == Phone.TYPE_CUSTOM) {
                        mContactPhoneType =  c.getString(PhoneProjection.sPhoneLabelIdx);
                    } else {
                        mContactPhoneType = mContext.getString(Phone.getTypeLabelResource(phoneType));
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    // return true if successfully find conference call info, otherwise return false
    public boolean parseConferenceCallInfo(String str) {
        if ((str == null) || (str.length() == 0))
            return false;

        Locale l = Locale.getDefault();
        String[] patternArray = DEFAULT_TEL_NUMBER_PATTERN;
        MatchFilter matchFilter = sSupportedLocaleMatchFilter;

        // find conference number first
        if (l.equals(Locale.US)) {
            patternArray = US_TEL_NUMBER_PATTERN;
        } else if (l.equals(Locale.SIMPLIFIED_CHINESE) || l.equals(Locale.CHINA) || l.equals(Locale.PRC)) {
            patternArray = ZH_CN_TEL_NUMBER_PATTERN;
        } else {
            matchFilter = sDefaultMatchFilter;
        }

        if ((patternArray != null) && (matchFilter != null)) {
            for (final String pattern : patternArray) {
                if (pattern == null)
                    continue;

                Pattern p = Pattern.compile(pattern);
                Matcher matcher = p.matcher(str);
                if (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();

                    if (matchFilter.acceptMatch(str, start, end)) {
                        mMeetingConfNumber = matcher.group();
                        // if found conference number, continue to find conference id
                        if (end < str.length()) {
                            str = str.substring(end);
                            p = Pattern.compile("[0-9#\\-\\,\\*]{3,}");
                            matcher = p.matcher(str);
                            if (matcher.find()) {
                                mMeetingConfId = matcher.group();
                            }
                        }
                        mMeetingNumberPattern = pattern;
                        mMeetingNumberIdx = start;
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    public void decodeMeetingConfCallInfo(String eventLocation) {
        mMeetingConfNumber = null;
        mMeetingConfId = null;

        if ((eventLocation != null) && (eventLocation.length() > 0)) {
            // try to find the particular string "Conference call number:" first
            int index = eventLocation.lastIndexOf(mContext.getString(R.string.conference_call_number_colon));
            if (index != -1) {
                String str = eventLocation.substring(index);
                // if we find the particular string but the substring after that doesn't contain a valid
                // conference info, we'll give another chance to search from the beginning.
                if (!parseConferenceCallInfo(str)) {
                    parseConferenceCallInfo(eventLocation);
                }
            } else {
                // if we can't find the particular string then search from the beginning directly.
                parseConferenceCallInfo(eventLocation);
            }
        }
    }

    public void notifySettingChange(ConferenceCallInfoSettingMgr settingMgr) {
        if (settingMgr == null)
            return;

        mDataSourceFlag = settingMgr.getConfCallInfoDataSource();

        switch (mDataSourceFlag) {
            case ConferenceCallInfo.DATA_SOURCE_FLAG_CONTACT_DATA:
                int contactDataId = settingMgr.getContactDataId();
                int phoneType = settingMgr.getContactPhoneType();
                loadContactConfCallInfo(contactDataId, phoneType);
                break;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MANUAL_INPUT:
                mManualConfNumber = settingMgr.getManualConfNumber();
                mManualConfId = settingMgr.getManualConfId();
                break;

            default:
                break;
        }
    }

    public String getMyConfNumber() {
        return mSharedPrefs.getString(GeneralPreferences.KEY_CONFERENCE_NUMBER, null);
    }

    public String getMyConfId() {
        return mSharedPrefs.getString(GeneralPreferences.KEY_CONFERENCE_ID, null);
    }

    public String getMeetingConfNumber() {
        return mMeetingConfNumber;
    }

    public String getMeetingConfId() {
        return mMeetingConfId;
    }

    public String getContactConfNumber() {
        return mContactConfNumber;
    }

    public String getContactPhoneType() {
        return mContactPhoneType;
    }

    public String getContactDisplayName() {
        return mContactDisplayName;
    }

    public String getMeetingNumberPattern() {
        if (isDefaultLocaleSupported()) {
            return mMeetingNumberPattern;
        }

        return null;
    }

    public int getMeetingNumberIndex() {
        if (isDefaultLocaleSupported()) {
            return mMeetingNumberIdx;
        }

        return -1;
    }

    public String getCurrentConfNumber() {

        switch (mDataSourceFlag) {
            case ConferenceCallInfo.DATA_SOURCE_FLAG_CONTACT_DATA:
                return mContactConfNumber;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MY_PREFERENCE:
                return getMyConfNumber();

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MANUAL_INPUT:
                return mManualConfNumber;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MEETING_LOCATION:
                return mMeetingConfNumber;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_AUTO_DETECT:
                // only when the locale is supported we'll return
                // conference number in the meeting location.
                if (isDefaultLocaleSupported()) {
                    return mMeetingConfNumber;
                }
                break;

            default:
                break;
        }

        return null;
    }

    public String getCurrentConfId() {

        switch (mDataSourceFlag) {
            case ConferenceCallInfo.DATA_SOURCE_FLAG_CONTACT_DATA:
                return null;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MY_PREFERENCE:
                return getMyConfId();

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MANUAL_INPUT:
                return mManualConfId;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_MEETING_LOCATION:
                return mMeetingConfId;

            case ConferenceCallInfo.DATA_SOURCE_FLAG_AUTO_DETECT:
                // only when the locale is supported we'll return
                // conference id in the meeting location.
                if (isDefaultLocaleSupported()) {
                    return mMeetingConfId;
                }
                break;

            default:
                break;
        }

        return null;
    }

    /**
     * check whether the default locale is supported
     * Currently it only supports US and China
     *
     * @return true if supporte, false otherwise
     */
    private static boolean isDefaultLocaleSupported() {
        Locale l = Locale.getDefault();

        if (l.equals(Locale.US) ||
            l.equals(Locale.SIMPLIFIED_CHINESE) ||
            l.equals(Locale.CHINA) ||
            l.equals(Locale.PRC)) {
            return true;
        }

        return false;
    }

}
