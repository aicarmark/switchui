package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Debug;
import android.util.Log;

import com.motorola.contextual.cache.SystemProperty;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

public class Utils {
    public static final String TAG = "LSAPP_Utils";
    public static final String DELIM = "&&";

    /**
     * compute the md5 of a string
     * @param input location string
     * @return
     */
    public static String computeMD5String(String input) {
        String md5 = null;

        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(input.getBytes());
            byte md5digest[] = algorithm.digest();

            // using BigInteger
            BigInteger hashint = new BigInteger(1, md5digest);
            md5 = String.format("%1$032X", hashint);

            // looping through each byte
            final StringBuilder sbMd5Hash = new StringBuilder();
            for (byte element : md5digest) {
                sbMd5Hash.append(Character.forDigit((element >> 4) & 0xf, 16));
                sbMd5Hash.append(Character.forDigit(element & 0xf, 16));
            }

            md5 = sbMd5Hash.toString();
        } catch (NoSuchAlgorithmException nsae) {
            LSAppLog.e(TAG, "computeMD5String : no MD5 algorithm");
        }

        return md5;
    }

    /**
     * get the version string for the package
     * @param context
     * @param pkgname
     * @return
     */
    public static String getVersionString(Context context, String pkgname) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkgname, 0);
            return pkgname + ":" + String.valueOf(pi.versionCode);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Calculate the distance in meters between two Geo Point
     * @param srclat
     * @param srclgt
     * @param dstlat
     * @param dstlgt
     * @return
     */
    public static float distanceTo(double srclat, double srclgt, double dstlat, double dstlgt) {
        float results[] = new float[1];
        Location.distanceBetween(srclat, srclgt, dstlat, dstlgt, results);
        return results[0];  // in meters
    }

    /**
     * comparing float number, http://www.ibm.com/developerworks/java/library/j-jtp0114/
     * return false if equals(no difference), true if differs
     */
    public static boolean compareDouble(double f1, double f2) {
        // only compare real float.
        if ( Double.isInfinite(f1) || Double.isNaN(f1) || Double.isInfinite(f2) || Double.isNaN(f2)) {
            LSAppLog.d(TAG, "compareFloat: bad float number: " + f1 + "::" + f2);
            return true;
        }
        return Double.compare(f1, f2) == 0 ? false : true;
    }

    /**
     * check whether two locations are consolidatable by comparing their lat lng accu
     * @return true if two locations are within each other, false otherwise.
     */
    public static boolean consolidatableLocations(double srclat, double srclgt, long srcaccuracy, double dstlat, double dstlgt, long dstaccuracy) {
        if( Utils.distanceTo(srclat, srclgt, dstlat, dstlgt) <= Math.max(LOCATION_DETECTING_DIST_RADIUS, Math.max(srcaccuracy, dstaccuracy))) {
            return true;
        }
        return false;
    }

    /**
     * convert a set of string into a single string connected by delim &&
     * @param s
     * @return
     */
    public static String convertSetToString(Set<String> s) {
        if (s == null || s.isEmpty()) {
            LSAppLog.d(TAG, "convertSetToString: empty set..return null string");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        // use iterator Iterator iter = set.iterator(); iter.hasNext()
        // String[] sa = (String[])s.toArray(); <- need to give type info when calling toArray();
        // http://www.jroller.com/cropleyb/entry/hashset_toarray_dodgy
        final String[] sa = (String[])s.toArray(new String[0]);
        for (String e : sa) {
            sb.append(e);
            sb.append(DELIM);
        }
        // delete the last DELIM
        sb.delete(sb.length()-DELIM.length(), sb.length());
        return sb.toString();
    }

    /**
     * convert a string array to set, strip the concatenate delim in the string.
     * @param saddr
     * @return Set if saddr is valid, or null.
     */
    public static Set<String> convertStringToSet(String saddr) {
        Set<String> addrset = new HashSet<String>();

        if (saddr == null) {
            return addrset;  // return empty set rather than null!
        }
        StringTokenizer st = new StringTokenizer(saddr, DELIM);
        while (st.hasMoreTokens()) {
            addrset.add(st.nextToken());
        }
        //LSAppLog.d(TAG, "convertStringToSet:" + addrset.toString());
        return addrset;
    }

    /**
     * merge new set to existing set.
     * @return existing set with new set added
     */
    public static <E> Set<E> mergeSets(Set<E> existingset, Set<? extends E> newset) {
        if (existingset == null || newset == null) {
            return existingset;
        }
        try {
            existingset.addAll(newset);
        } catch (Exception e) {
            LSAppLog.e(TAG, "mergeSet: " + e.toString());
        }
        return existingset;
    }

    /**
     * merge two set strings and the merged string as the third out parameter
     * return the expanded set string if the existing set expanded, null if not(a subset of the existing set)
     * @param curaddr: normally the existing data from database
     * @param newaddr: normally the data from this measurement
     */
    public static String mergeSetStrings(String curaddr, String newaddr) {
        boolean modified = false;
        String mergedaddrs = null;
        Set<String> curset = convertStringToSet(curaddr);
        Set<String> newset = convertStringToSet(newaddr);

        // let's keep the member variable addr set untact.
        if (newset != null) {
            for (String s : newset) {
                if (!curset.contains(s)) {
                    curset.add(s);
                    modified = true;
                }
            }
        }
        if (modified) {
            // now we got the complete list, convert back to string for storing in db
            mergedaddrs = convertSetToString(curset);
            LSAppLog.d(TAG, "mergeSets " + "::" + mergedaddrs);
        }
        return mergedaddrs;
    }


    /**
     * checking whether any bouncing cells are poi cells
     * @param src : the hay set, normally the set in db
     * @param dst : the needle to be found, normally the bouncing cells
     * @return
     */
    @Deprecated
    public static boolean jsonStringSubset(String src, String dst) {
        boolean fuzzymatch = false;    // leave for future
        int matchcount, srcsize, dstsize;

        if (src == null || dst == null) {  // no match if any of them are null.
            return false;
        }

        Set<String> srcset = convertStringToSet(src);
        Set<String> dstset = convertStringToSet(dst);

        srcsize = srcset.size();
        dstsize=dstset.size();
        matchcount = 0;

        if (dstsize <= 1) {
            fuzzymatch = false;
        }

        for (String d : dstset) {
            // I supposed to do JSONObject(d) and check cell id...but I hate to construct too many json objects.
            if( ! TelephonyMonitor.isCellJsonValid(d)){
                LSAppLog.d(TAG, "JsonStringSubset : filter out invalid cell json: " +  d);
                continue;
            }

            if (srcset.contains(d)) {
                LSAppLog.d(TAG, "StringSubset :" + dst + " contains celljon ::" + d);
                matchcount++;
                if (!fuzzymatch) {
                    LSAppLog.d(TAG, "StringSubset : single match true : sets ::" + src + " :: " + dst);
                }
            }
        }

        // subset, one match is a subset, no fuzzy logic here.
        if (matchcount > 0 ) {
            LSAppLog.d(TAG, "StringSubset : fuzzy match true : matchcount=" + matchcount + ":minsize="+Math.min(srcsize, dstsize)/2 + " sub sets ::" + src + " == " + dst);
            return true;
        } else {
            LSAppLog.d(TAG, "StringSubset : fuzzy match false : matchcount=" + matchcount + ":minsize="+Math.min(srcsize, dstsize)/2 + " distinct sets ::" + src + " !! " + dst);
            return false;
        }
    }

    /**
     * compare two sets, return how many items are in common
     * @return the no. of items in common.
     */
    public static <E> int getMatchCount(Set<? extends E> dbset, Set<? extends E> curset) {
        int matches = 0;

        if (dbset == null || curset == null || dbset.size() == 0 || curset.size() == 0) {
            return 0;
        }
        //LSAppLog.d(TAG, "matchSets : dbset : " + dbset.toString() + " : curset :" + curset.toString());

        for (E d : curset) {
            if (dbset.contains(d)) {
                //LSAppLog.d(TAG, "matchSets : matches :" + d);
                matches++;
            }
        }
        return matches;
    }

    /**
     * fuzzy match whether runtime curset matches to static db set.
     * match criteria : static dbset size less than 2, full match...otherwise, half
     * @param dbset  static db set
     * @param curset runtime current set
     * @param singleMatch true if one match is sufficent, false if caller need more than one match to turn true.
     * @return true if one match is sufficient upon singleMatch flag set. False otherwise.
     */
    public static boolean fuzzyMatchSets(Set<String> dbset, Set<String> curset, boolean singleMatch) {
        if (dbset == null) {
            return false;
        }
        boolean fuzzymatch = false;
        int matchcount = getMatchCount(dbset, curset);

        if (singleMatch) {  // single match will cause match to be true.
            if (matchcount > 0) {
                LSAppLog.d(TAG, "fuzzyMatchSets : true if single Match : matchecount :" + matchcount);
                fuzzymatch = true;
            } else {
                LSAppLog.d(TAG, "fuzzyMatchSets : false for not any match : matchecount :" + matchcount);
                fuzzymatch = false;
            }
        } else {   // need more than 1 match before assert true
            if (matchcount >= FUZZY_MATCH_MIN || matchcount >= dbset.size()) { // great than 1 to filter out mobile station // 1+dbset.size()/2){
                LSAppLog.d(TAG, "fuzzyMatchSets : True if more than single match : matchecount :" + matchcount);
                fuzzymatch = true;
            } else {
                LSAppLog.d(TAG, "fuzzyMatchSets : False if not more than single : matchecount :" + matchcount);
                fuzzymatch = false;
            }
        }
        return fuzzymatch;
    }

    /**
     * fuzzy match whethere the value in set is contained in the map's value collection.
     * @param set a set of string
     * @param map a map with both key and value are string
     * @return match count of no. of string in set is contained inside the value of map
     */
    public static int fuzzyMatchSetInMapValue(Set<String> set, Map<String, String> map) {
        int matchcount = 0;

        if (set.size() == 0 || map.size() == 0)
            return 0;

        for (String s: set) {
            if (map.containsValue(s)) {
                matchcount++;
                LSAppLog.d(TAG, "fuzzyMatchSetInMapValue : matched: " + s + " Matchcount: " + matchcount);
            }
        }

        return matchcount;
    }

    @Deprecated
    /**
     * checking whether airplane mode is on. really, I need to do content resolver every time ?
     * @return true if airplane mode on, false otherwise.
     */
    // TODO Remove all the legacy calls.
    public static boolean isAirplaneModeOn(Context ctx) {
        return SystemProperty.isAirplaneMode();
    }

    /**
     * check whether a string in a set is also in a string repr of another collection using just indexOf.
     * this is mainly used for matching wifi during the entering...we need to fuzzy match of at least 2, multiple matches to avoid mobile station issues.
     * This is feasible b/c wifi mac is fixed length!
     */
    public static int intersectSetJsonArray(Set< ? extends String> set, String wrap, String jsonString) {
        int matchcnt = 0;
        if (jsonString != null && jsonString.length() > 0) {
            for (String s : set) {
                String wrapstr = s;
                if (wrap != null) {
                    wrapstr = wrap + s + wrap;
                }
                if (jsonString.indexOf(wrapstr) >= 0) {
                    if(LOG_INFO) Log.i(TAG, "intersectSetJsonArray: Match: " + wrapstr);
                    matchcnt++;
                }
            }
        }
        return matchcnt;
    }

    /**
     * get the current method name for logging
     * @return the method name of current execution stack
     */
    public static String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    /**
     * Start profiling the VM, trace file will be at "/sdcard/datetime.trace"
     */
    public static void startProfiling() {
        if (LOG_VERBOSE) {
            String f = ""+new Date().getTime();
            Debug.startMethodTracing(f);
        }
    }

    /**
     * Stop profiling the VM.
     * Note: if you don't stop before the phone or app shuts down, no data is generated.
     */
    public static void stopProfiling() {
        if (LOG_VERBOSE) {
            Debug.stopMethodTracing();
        }
    }

}
