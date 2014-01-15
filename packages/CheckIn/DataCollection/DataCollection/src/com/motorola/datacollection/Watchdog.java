package com.motorola.datacollection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

/**
 * Class that helps to limit the impact of infinite force close loops, like that in
 * IKSTABLE6-21021, which caused 33000+ production phones to enter a force close loop
 * that persisted even after a power cycle
 */
public final class Watchdog implements Thread.UncaughtExceptionHandler {
    private static final String TAG = SystemDependency.TAG;
    private static final boolean DUMP = SystemDependency.DUMP;
    private static final int SAFETY_LOOP_COUNT = 100;
    private static final String KERNEL_BOOT_TIME_FILE = "/proc/stat";
    private static final String BOOT_TIME_PREFIX = "btime";
    private static final String SPACE = " ";
    private static final int BOOT_TIME_FIELD_INDEX = 1;
    private static final String FIRST_BOOT_TIME_PROPERTY = "ro.runtime.firstboot";
    private static final String DISABLE_WATCHDOG_PREF = "DisableWatchdog";
    private static final String PREVIOUS_CRASHES_ARRAY_PREF = "PreviousCrashes";
    private static final String BOOT_TIME_PREF_MS = "BootTime";
    private static final boolean IS_PRODUCTION_BUILD =
            "userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE) ? false : true;

    // Give 60 seconds for the shared preference read on background thread to complete
    private static final int INITIALIZE_TIMEOUT_MS = 60000;

    // Maximum of 2 crashes allowed every 12 hours within a boot cycle
    private static final long CRASH_DURATION_MS = 12 * 60 * 60 * 1000L; //12 hours
    private static final int CRASH_HISTORY_LEN = 2;

    static private Watchdog sInstance;
    private boolean mAppDisabled;
    private boolean mDisableWatchdog;
    private long[] mPreviousCrashes;
    private Context mContext;
    private final Thread.UncaughtExceptionHandler mOldExceptionHandler;
    private long mBootTime;
    private final CountDownLatch mInitializedLatch;

    /**
     * Get the singleton instance of the Watchdog class. This method is not exposed to other classes
     * @return The singleton instance of the Watchdog class
     */
    private synchronized static final Watchdog getInstance() {
        if (sInstance == null) sInstance = new Watchdog();
        return sInstance;
    }

    /**
     * Private constructor that creates and initializes the Watchdog class.
     */
    private Watchdog() {
        if (DUMP) Log.d(TAG, "Watchdog constructor");

        // start off in the disabled state.
        mAppDisabled = true;

        mInitializedLatch = new CountDownLatch(1);

        mPreviousCrashes = new long[CRASH_HISTORY_LEN];

        // Get and set the method that gets invoked when an uncaught exception happens in any thread
        mOldExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Get the time at which the Linux kernel booted up.
     * This value does not change when we get to run again after a crash in our process
     * @return The time at which the Linux kernel booted up
     */
    private final long getKernelBootTime() {
        // If cached value is present, return that
        if (mBootTime != 0) return mBootTime;

        try {
            // Parse /proc/stat and read the 6th line, which is like "btime 1329989902"
            BufferedReader reader = new BufferedReader(new FileReader(KERNEL_BOOT_TIME_FILE));
            try {
                for (int i=0; i<SAFETY_LOOP_COUNT; i++) {
                    String line = reader.readLine();
                    if (line == null) break;

                    if (line.startsWith(BOOT_TIME_PREFIX)) {
                        // To avoid a "potential tainted data vulnerability" klocwork report.
                        // See http://goo.gl/gLa8J
                        line = Normalizer.normalize(line, Form.NFKC);

                        mBootTime = Long.parseLong(line.split(SPACE)[BOOT_TIME_FIELD_INDEX]);
                        break;
                    }
                }
            } finally {
                if (reader != null) reader.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading " + KERNEL_BOOT_TIME_FILE, e);
        }

        /* Can't always depend on the following property because it is 0 for a while at powerup */
        if (mBootTime == 0) mBootTime = SystemProperties.getLong(FIRST_BOOT_TIME_PROPERTY, 0);

        if (mBootTime == 0) Log.e(TAG, "Could not find boot time");

        if (DUMP) Log.e(TAG, "Using boot time of " + mBootTime);
        return mBootTime;
    }

    /**
     * Initialize the Watchdog class.
     * This reads the previous history of crashes from sharedpreferences, and eventually determines
     * whether our component should be disabled for this run
     * @param context An android context which is used for reading/writing shared preferences
     */
    public static final void initialize(Context context) {
        try {
            getInstance().initializeImpl(context);
        } catch (Exception e) {
            Log.e(TAG, "initialize", e);
        }
    }

    /**
     * Internal method used to initialize the Watchdog class
     * @param context An android context which is used for reading/writing shared preferences
     */
    private final void initializeImpl(Context context) {
        try {
            if (DUMP) Log.d(TAG, "Watchdog initialize");

            if (context == null) {
                Log.e(TAG, "context must not be null in Watchdog.initialize()");
                return;
            }

            mContext = context.getApplicationContext();
            SharedPreferences pref = mContext.getSharedPreferences(SystemDependency.PREF_FILE,
                    Context.MODE_PRIVATE);

            mDisableWatchdog = pref.getBoolean(DISABLE_WATCHDOG_PREF, false);
            if (mDisableWatchdog == true ) {
                Log.i(TAG, "watchdog is disabled");
                mAppDisabled = false;
                return;
            }

            // Get the time at which the kernel booted up
            long firstBoot = getKernelBootTime();

            // Get the kernel boot time for which crash history is saved in shared preferences
            long savedFirstBoot = pref.getLong(BOOT_TIME_PREF_MS, 0);

            if (firstBoot != savedFirstBoot) {
                // Our process is allowed to run after a power cycle.
                mAppDisabled = false;
            } else {
                Object crashObj = SystemDependency.getObjectFromString(
                        pref.getString(PREVIOUS_CRASHES_ARRAY_PREF, null));

                boolean newDisabled = false;

                // Make sure that the data read from shared preferences is exactly as we expect it
                if (crashObj instanceof long[]) {
                    long[] crashes = (long[]) crashObj;

                    if (crashes.length == CRASH_HISTORY_LEN) {
                        mPreviousCrashes = crashes;
                        // i.e if 2 crashes have happened in 12 hours, then disable ourselves
                        newDisabled = ( getRecentCrashes() == CRASH_HISTORY_LEN );
                    }
                }

                mAppDisabled = newDisabled;
            }
            Log.i(TAG, "At end of initialize, disabled is " + mAppDisabled);
        } finally {
            mInitializedLatch.countDown();
        }
    }

    /**
     * Determine whether our component should be disabled.
     * @return true if it should be disabled, false otherwise
     */
    public static final boolean isDisabled() {
        return getInstance().isDisabledImpl();
    }

    /**
     * Internal method that indicates whether our component should be disabled
     * @return true if it should be disabled, false otherwise
     */
    @SuppressWarnings("all") // Using "unused" instead of "all" gives a warning when DUMP is true
    private final boolean isDisabledImpl() {
        try {
            if ( mInitializedLatch.await(INITIALIZE_TIMEOUT_MS, TimeUnit.MILLISECONDS) == false ) {
                Log.e(TAG, "isDisabledImpl timeout");
                // Fall through and return the mAppDisabled that is initialized by default to true
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted exception", e);
        }

        if (DUMP && mAppDisabled) {
            Log.d(TAG, "Watchdog isDisabled() returning disabled", new RuntimeException());
        }
        return mAppDisabled;
    }

    /**
     * Determine the number of recent crashes in our component
     * @return the number of recent crashes in our component
     */
    private final int getRecentCrashes() {
        int i;
        long currentTime = System.currentTimeMillis();
        for (i=0; i<CRASH_HISTORY_LEN; i++) {
            if (mPreviousCrashes[i] == 0) break;
            if (Math.abs(currentTime - mPreviousCrashes[i]) > CRASH_DURATION_MS) break;
        }
        return i;
    }

    /**
     * Called by the Dalvik VM when an uncaught exception happens in our process
     * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread,
     *   java.lang.Throwable)
     */
    @Override
    public final void uncaughtException(Thread thread, Throwable ex) {
        if (mDisableWatchdog) {
            if (mOldExceptionHandler != null) mOldExceptionHandler.uncaughtException(thread, ex);
            return;
        }

        boolean silentExit = false;
        boolean isMyComponent = false;

        try {
            // Check if the callstack of the crash has our package name
            String callstack = Log.getStackTraceString(ex);
            isMyComponent = callstack.contains(SystemDependency.PACKAGE_COMMON_PREFIX);

            // We are running in the shared com.motorola.process.system process that hosts 20+ other
            // components. Here, we want to process crashes only in our component.
            if (mContext != null && isMyComponent) {

                // If we crash when we are disabled, thats something weird. So exit silently
                silentExit = mAppDisabled;

                // disable ourselves for now, but this process is exiting below
                mAppDisabled = true;

                // Shift and update the history of crashes stored in sharedpreferences

                System.arraycopy(mPreviousCrashes, 0, mPreviousCrashes, 1, CRASH_HISTORY_LEN-1);
                mPreviousCrashes[0] = System.currentTimeMillis();

                SharedPreferences pref = mContext.getSharedPreferences(SystemDependency.PREF_FILE,
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = pref.edit();

                edit.putLong(BOOT_TIME_PREF_MS, getKernelBootTime());
                edit.putString(PREVIOUS_CRASHES_ARRAY_PREF,
                        SystemDependency.getObjectAsString(mPreviousCrashes));

                // don't call apply() here, since we want updates to happen before process exit
                edit.commit();
            }
        } catch (Exception e) {
            try {
                if (!IS_PRODUCTION_BUILD) Log.e(TAG, "In uncaught exception", e);
            } catch (Exception logException) {
            }

            // We were not able to write to shared preferences, or maybe some memory error happened.
            // If an exception happened in getStackTraceString above, we dont even know whether the
            // crash is in our component. So exit silently.
            silentExit = true;
        }

        // On production phones, if the crash is in our component, we want to exit without
        // informing the user
        if (silentExit || (isMyComponent && IS_PRODUCTION_BUILD )) {
            try {
                Log.e(TAG, "Exiting com.motorola.process.system process", ex);
            } catch (Exception e) {
            }

            try {
                Process.killProcess(Process.myPid());
            } catch (Exception e) {
            }

            try {
                System.exit(1);
            } catch (Exception e) {
            };

            return;
        }

        // Pass through to the default exception handler, that displays the force close dialog,
        // and also reports the crash to the blur portal.
        if (mOldExceptionHandler != null) mOldExceptionHandler.uncaughtException(thread, ex);
    }
}

// This class is different between DataCollection and DeviceStatistics
final class SystemDependency {
    /**
     * true if debug logs should be printed to logcat
     */
    final static boolean DUMP = Utilities.LOGD;

    /**
     * A common prefix of the method names in the callstack, that can be used to identify if the
     * callstack is related to our component
     */
    final static String PACKAGE_COMMON_PREFIX = "com.motorola.datacollection";

    /**
     * Tag used for logcat logs
     */
    final static String TAG = "DCE_Watchdog";

    /**
     * A unique shared preferences file name to store crash data.
     * Surprisingly, 2 apks running in the same process should never use the same string for this
     * variable value. Otherwise, one apk will overwrite the sharedpreferences of the the other apk
     */
    final static String PREF_FILE = "DcWatchdog";

    /**
     * Method to convert an ascii string to an object
     * @param encoded The ascii string to be converted
     * @return The converted object
     */
    static final Object getObjectFromString(String encoded) {
        return Utilities.deSerializeObject(encoded);
    }

    /**
     * Method to convert an object to an ascii string
     * @param object to be converted to an ascii string
     * @return converted ascii string
     */
    static final String getObjectAsString(long[] object) {
        return Utilities.serializeObject(object);
    }
}
