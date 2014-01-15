/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import com.android.calendar.R;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.widget.ViewFlipper;
import android.text.format.DateUtils;

public class KoreanLunar {

    private static final String TAG = "KoreanLunar";
    private static final boolean DEBUG = false;

    private static final int LUNAR_MAX_TABLE = 220;
    private static final int LUNAR_START_YEAR = 1881;
    private static final int LUNAR_END_YEAR = 2099; // Check

    private static boolean mLeapMonth = false;

    private static long[] lunar_dt; // use define
    private static boolean isInitLunarTable = false;

    private static final String[] ls_lunar_table = { "",
    /* 1881 */
    "1212122322121", "1212121221220", "1121121222120", "2112132122122",
            "2112112121220", "2121211212120", "2212321121212", "2122121121210",
            "2122121212120", "1232122121212",
            /* 1891 */
            "1212121221220", "1121123221222", "1121121212220", "1212112121220",
            "2121231212121", "2221211212120", "1221212121210", "2123221212121",
            "2121212212120", "1211212232212",
            /* 1901 */
            "1211212122210", "2121121212220", "1212132112212", "2212112112210",
            "2212211212120", "1221412121212", "1212122121210", "2112212122120",
            "1231212122212", "1211212122210",
            /* 1911 */
            "2121123122122", "2121121122120", "2212112112120", "2212231212112",
            "2122121212120", "1212122121210", "2132122122121", "2112121222120",
            "1211212322122", "1211211221220",
            /* 1921 */
            "2121121121220", "2122132112122", "1221212121120", "2121221212110",
            "2122321221212", "1121212212210", "2112121221220", "1231211221222",
            "1211211212220", "1221123121221",
            /* 1931 */
            "2221121121210", "2221212112120", "1221241212112", "1212212212120",
            "1121212212210", "2114121212221", "2112112122210", "2211211412212",
            "2211211212120", "2212121121210",
            /* 1941 */
            "2212214112121", "2122122121120", "1212122122120", "1121412122122",
            "1121121222120", "2112112122120", "2231211212122", "2121211212120",
            "2212121321212", "2122121121210",
            /* 1951 */
            "2122121212120", "1212142121212", "1211221221220", "1121121221220",
            "2114112121222", "1212112121220", "2121211232122", "1221211212120",
            "1221212121210", "2121223212121",
            /* 1961 */
            "2121212212120", "1211212212210", "2121321212221", "2121121212220",
            "1212112112220", "1223211211221", "2212211212120", "1221212321212",
            "1212122121210", "2112212122120",
            /* 1971 */
            "1211232122212", "1211212122210", "2121121122210", "2212312112212",
            "2212112112120", "2212121232112", "2122121212110", "2212122121210",
            "2112124122121", "2112121221220",
            /* 1981 */
            "1211211221220", "2121321122122", "2121121121220", "2122112112322",
            "1221212112120", "1221221212110", "2122123221212", "1121212212210",
            "2112121221220", "1211231221222",
            /* 1991 */
            "1211211212220", "1221121121220", "1223212112121", "2221212112120",
            "1221221232112", "1212212122120", "1121212212210", "2112132212221",
            "2112112122210", "2211211212210",
            /* 2001 */
            "2221321121212", "2212121121210", "2212212112120", "1232212121212",
            "1212122122110", "2121212322122", "1121121222120", "2112112122120",
            "2211231212122", "2121211212120",
            /* 2011 */
            "2122121121210", "2124212112121", "2122121212120", "1212121223212",
            "1211212221210", "2121121221220", "1212132121222", "1212112121220",
            "2121211212120", "2122321121212",
            /* 2021 */
            "1221212121210", "2121221212120", "1232121221212", "1211212212210",
            "2121123212221", "2121121212220",
            "1212112112220",
            "1221231211221",
            "2212211211220",
            "1212212121210",
            /* 2031 */
            "2123212212121",
            "2112122122120",
            "1211212122232",
            "1211212122210",
            "2121121122120",
            "2212114112212",
            "2212112112120",
            "2212121211210",
            "2212232121211",
            "2122122121210",
            /* 2041 */
            "2112122122120",
            "1231212122122",
            "1211211221220",
            "2121121321222", // the last one is temporary, now there is no real
                                // data.
            "2121121121220",
            "2122112112120",
            "2122141211212",
            "1221221212110",
            "2121221221210",
            "2114121221221",
            // 2051 - 2060
            "2112121212220",
            "1211211241222",
            "1211211212220",
            "1221121121220",
            "1221214112121",
            "2221212112120",
            "1221212211210",
            "2121421212211",
            "2121212212210",
            "2112112212220",
            // 2061 - 2070
            "1213211212221", "2211211212210", "2212121321212",
            "2212121121210",
            "2212212112120",
            "1212232121212",
            "1212122121210",
            "2121212122120",
            "2112312122212",
            "2112112122120",
            // 2071 - 2080
            "2211211232122", "2121211212120", "2122121121210", "2122123212121",
            "2121221212120", "1212121221210",
            "2121321222121",
            "2121121221220",
            "1212112121220",
            "2123211212122",
            // 2081 - 2090
            "2121211211220", "1222121321212", "1221212121210", "2121221212120",
            "1211241221212", "1211212212210", "2121121212220", "1212312112222",
            "1212112112210",
            "2221211231221",
            // 2091 - 2100
            "2212121211220", "1212212121210", "2121214212121", "2112122122120",
            "1211212122210", "2121321212212", "2121121122120", "2212112112210",
            "2223211211212", "2212121211210", };

    private static Time SolarToLunar(Time solar) {
        int i;
        int Year, Month, MonthDay;
        long j, m1, m2;
        long total_day, total_day0, total_day1, total_day2;
        long k11, count, lun_year;

        Time lunarTime = new Time();
        lunarTime.set(solar);

        Year = solar.year;
        Month = solar.month + 1; // month -1 at the end of function.
        MonthDay = solar.monthDay;

        // This condition is not used for android project.
        // But it is remained for the reference of other projects.
        // Exception (Solar 1881 year 1 month)
        if (Year == 1881 && Month == 1 && MonthDay < 30) {
            mLeapMonth = false;
            lunarTime.year = 1881;
            lunarTime.month = 12;
            lunarTime.monthDay = MonthDay + 1;

            return lunarTime;
        }

        // sum of days to 1881-1-30
        // (Solar 1881-1-30 -> Lunar(1881-1-1))
        total_day1 = 686686; // Expression : (long)1880*365 + 1880/4 - 1880/100
                                // + 1880/400 + 30;
        k11 = Year - 1;

        // sum of days to input day
        total_day2 = k11 * 365 + k11 / 4 - k11 / 100 + k11 / 400;

        for (i = 1; i <= Month - 1; i++) {
            total_day2 = total_day2 + getNumOfDays(Year, i);
        }

        total_day2 = total_day2 + MonthDay;

        total_day = total_day2 - total_day1 + 1;

        total_day0 = lunar_dt[1];

        for (i = 1; i < LUNAR_MAX_TABLE - 1; i++) {
            if (total_day <= total_day0) {
                break;
            }
            total_day0 = total_day0 + lunar_dt[i + 1];
        }

        lun_year = i + 1880;

        total_day0 = total_day0 - lunar_dt[i];
        total_day = total_day - total_day0;

        if (ls_lunar_table[i].charAt(12) == '0') {
            count = 12;
        } else {
            count = 13;
        }

        m2 = 0;
        for (j = 1; j <= count; j++) {
            if (ls_lunar_table[i].charAt((int) j - 1) <= '2') {
                m2++;
                m1 = (ls_lunar_table[i].charAt((int) j - 1) - '0') + 28;
                mLeapMonth = false;
            } else {
                m1 = (ls_lunar_table[i].charAt((int) j - 1) - '0') + 26;
                mLeapMonth = true; // Lunar month
            }

            if (total_day <= m1) {
                break;
            }
            total_day = total_day - m1;
        }

        lunarTime.year = (int) lun_year;
        lunarTime.month = (int) m2 - 1; // restore month cause of "Time" month
                                        // [0-11]
        lunarTime.monthDay = (int) total_day;

        return lunarTime;
    }

    private static void initLunarYearDays() {
        int i = 0, j = 0;
        char unit = 0;

        if (isInitLunarTable == true) {
            return;
        }

        lunar_dt = new long[LUNAR_MAX_TABLE];

        for (i = 1; i <= LUNAR_MAX_TABLE - 1; i++) {
            for (j = 1; j <= 12; j++) {// j --> 0
                unit = ls_lunar_table[i].charAt(j - 1);

                switch (unit) {
                case '1':
                case '3':
                    lunar_dt[i] = lunar_dt[i] + 29;
                    break;
                case '2':
                case '4':
                    lunar_dt[i] = lunar_dt[i] + 30;
                    break;
                }
            }

            switch (ls_lunar_table[i].charAt(12)) {
            case '0':
                break;
            case '1':
            case '3':
                lunar_dt[i] = lunar_dt[i] + 29;
                break;
            case '2':
            case '4':
                lunar_dt[i] = lunar_dt[i] + 30;
                break;
            }
        }

        isInitLunarTable = true;
    }

    private static int getNumOfDays(int year, int month) {
        switch (month) {
        case 1:
        case 3:
        case 5:
        case 7:
        case 8:
        case 10:
        case 12:
            return 31;

        case 4:
        case 6:
        case 9:
        case 11:
            return 30;

        case 2:
            if ((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0)) {
                return 29;
            } else {
                return 28;
            }
        }

        return -1;
    }

    public static boolean IsLeapMonth() {
        if (isInitLunarTable == true) {
            return mLeapMonth;
        }

        return false; // default
    }

    public static Time lunarDate(Time mBaseDate) {
        Time solarTime, lunarTime;

        initLunarYearDays(); // only Once

        solarTime = new Time();
        lunarTime = new Time();

        solarTime.set(mBaseDate);

        // check Lunar & Solar
        if (solarTime.year < LUNAR_START_YEAR
                || solarTime.year > LUNAR_END_YEAR) {
            if (DEBUG) {
                android.util.Log.e("CalUtils", "  illegal year: "
                        + solarTime.year);
            }
            return lunarTime;
        }

        lunarTime = SolarToLunar(solarTime);

        if (DEBUG) {
            android.util.Log.e("CalUtils", "  lunar date result: "
                    + lunarTime.year + ", " + lunarTime.month + ", "
                    + lunarTime.monthDay);
        }

        return lunarTime;
    }

    /**
     * Provides functions to calculate Lunar lunar calendar day based on western
     * calendar.
     */
    public static String KoreanLunarDay(Context context, Time time) {
        String dateString;
        StringBuilder dateBuilder = new StringBuilder();
        Time lunardate = KoreanLunar.lunarDate(time);

        dateBuilder.append(lunardate.month + 1).append(".")
                .append(lunardate.monthDay);
        dateString = dateBuilder.toString();

        int str = R.string.lunar;
        if (KoreanLunar.IsLeapMonth() == true) {
            str = R.string.korean_leap;
        }
        String lunarDay = "(" + context.getResources().getText(str)
                + ") " + dateString;
        return lunarDay;
    }

}
