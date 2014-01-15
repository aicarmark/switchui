/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: BatteryStatsAccumulator.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 2, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import android.content.Context;
import android.util.Log;

import com.motorola.devicestatistics.DataAccumulator.IAccumulator;
import com.motorola.devicestatistics.StatsCollector.DataBundle;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.StatsDatabase.CommitLevel;

/**
 * @author bluremployee
 *
 */
/*package*/ class BatteryStatsAccumulator implements IAccumulator {

    private final static boolean DUMP = DevStatUtils.GLOBAL_DUMP;
    private final static String TAG = "BStatsAccumulator";
    private static ScreenStateSaver sScreenState;

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataAccumulator.IAccumulator#canHandleType(int)
     */
    public boolean canHandleType(int type) {
        return type == DataTypes.BATTERY_STATS;
    }

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataAccumulator.IAccumulator#doAccumulate(int, android.content.Context, com.motorola.devicestatistics.StatsCollector.DataBundle)
     */
    public boolean doAccumulate(int type, Context context,
            DataBundle result) {
        final boolean DEBUG_SANITY = true;

        synchronized (BatteryStatsAccumulator.class) {
            if (sScreenState == null) sScreenState = new ScreenStateSaver(context);
        }

        Object o = result.getData();
        if(o == null || !(o instanceof BatteryStatsState)) return false;

        int lastScreenState = sScreenState.getAndSetScreenState();
        if (DUMP) Log.v(TAG, "lastScreenState is " + lastScreenState);

        Log.v(TAG, "Accumulating for type: " + type); // Forcibly log
        BatteryStatsState currentState = (BatteryStatsState)o;

        // Prevent overlap between BatteryStatsAccumulator and StatsUploader
        synchronized (DatabaseIface.sqliteDbLock) {

            StatsDatabase lastDb = StatsDatabase.load(context, StatsDatabase.Type.LAST,
                    new String[] {StatsDatabase.Group.ALL});
            BatteryStatsState lastState = BatteryStatsState.createFromDatabase(lastDb);

            if(!lastState.isValid()) {
                if(DUMP) Log.v(TAG, "Last state from db is invalid, commit current to last" +
                        " and cumulative");
                // commit current to last and cumulative
                StatsDatabase db = StatsDatabase.loadEmpty(context,
                        StatsDatabase.Type.LAST);

                // Commit the empty database structure to the screen_on/screen_off databases on disk
                db.commit(StatsDatabase.Type.SCREEN_ON, CommitLevel.GROUP);
                db.commit(StatsDatabase.Type.SCREEN_OFF, CommitLevel.GROUP);

                currentState.commitToDatabase(db);
                db.commit(StatsDatabase.Type.LAST, CommitLevel.GROUP);
                
                // sanity check cumulative is also invalid
                if(DEBUG_SANITY) {
                    final String GROUP = "default";
                    StatsDatabase cdb = StatsDatabase.load(context,
                            StatsDatabase.Type.CUMULATIVE,
                            new String[]{StatsDatabase.Group.DEFAULT});
                    String val = cdb.getValue(GROUP, GROUP, "trt_rt");
                    if(DUMP) Log.v(TAG, "Sanity check, we should be invalid, trt_rt is " + val);
                }
                db.commit(StatsDatabase.Type.CUMULATIVE, CommitLevel.GROUP);
                // Also clear the reset hint
                BatteryStatsResetHint.readAndResetHint(context);

                lastDb.cleanup();
                db.cleanup();
                return true;
            }

            // commit current to last
            // Note that currentState is modified in the call to diffOnFrameworkReset below.
            // So it needs to be updated in the database (as StatsDatabase.Type.LAST ),
            // before it is modified below
            StatsDatabase db = StatsDatabase.loadEmpty(context,
                    StatsDatabase.Type.LAST);
            currentState.commitToDatabase(db);
            db.commit(StatsDatabase.Type.LAST, CommitLevel.GROUP);

            // If we have a reset hint, then our life is much simpler
            boolean add = BatteryStatsResetHint.readAndResetHint(context);
            if(DUMP) Log.v(TAG, "Reset hint says : " + add); 
            if (add) {
                // When android framework resets its data, the non android framework data in
                // currentState has NOT been reset. Since its added below to the db, currentStats
                // needs to be updated with the diff to lastState for the non-android-framework
                // data
                lastState.diffOnFrameworkReset(currentState);
            }

            if(!add) {
                if(DUMP) Log.v(TAG, "Valid last state - comparing..."); 
                add = lastState.compare(currentState);
                if(DUMP) Log.v(TAG, "Compare returned " + add);
            }else {
                lastState = currentState;
            }

            if(add) {
                addToDatabase(context, StatsDatabase.Type.CUMULATIVE, lastState);

                Integer screenDatabase = null;
                if ( lastScreenState == ScreenStateSaver.ON ) {
                    screenDatabase = StatsDatabase.Type.SCREEN_ON;
                } else if ( lastScreenState == ScreenStateSaver.OFF ) {
                    screenDatabase = StatsDatabase.Type.SCREEN_OFF;
                }

                if ( screenDatabase != null ) {
                    if ( DUMP ) Log.v( TAG, "Accumulating for screendb=" + screenDatabase );
                    addToDatabase(context, screenDatabase, lastState);
                }
            }
            lastDb.cleanup();
            db.cleanup();
        }

        return true;
    }

    private final void addToDatabase(Context context, int databaseId, BatteryStatsState lastState) {
        StatsDatabase accDb = StatsDatabase.load(context,
                databaseId,
                new String[] {StatsDatabase.Group.ALL});
        BatteryStatsState accState = BatteryStatsState.createFromDatabase(accDb);
        accState.addFrom(lastState);
        accDb.resetValues();
        accState.commitToDatabase(accDb);
        accDb.commit(databaseId, CommitLevel.KEY);
        accDb.cleanup();
        // commit cumulative
    }
}

