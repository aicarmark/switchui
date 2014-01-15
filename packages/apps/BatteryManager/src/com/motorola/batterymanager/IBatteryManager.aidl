/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager;

/**
 * Interface definitions for BatteryManager
 */

interface IBatteryManager {
    /**
     * This API is used to change data connection to ON or OFF irrespective of current BM mode
     * settings. User of this API has to make sure to reset back to BM mode settings, once their
     * respective operation(s) are completed. Failure to restore back to BM mode settings would 
     * keep data ON/OFF always (based on the request made) until the handset is power cycled.
     *
     * NOTE:
     * 1. DATA_OFF_ALWAYS takes priority over DATA_ON_ALWAYS.
     * 2. Request to DATA_OFF_ALWAYS & DATA_ON_ALWAYS MUST follow by RESET_DATA_OFF_ALWAYS or
     *    RESET_DATA_ON_ALWAYS respectively, to restore back Battery Manager settings.
     * 3. Based on the "persistent" flag in the request, data settings would get preserved
     *    across power cycles, if reset is not triggered in the same power up.
     * 4. "persistent" flag to be consistent in request and while resetting.
     *    Eg.,
     *    Request Call: changeDataSettings(DATA_OFF_ALWAYS, NO_DURATION, true);
     *    Reset   Call: changeDataSettings(RESET_DATA_OFF_ALWAYS, NO_DURATION, true);
     *
     * 5. In case of multiple calls to DATA_OFF_ALWAYS, data continues to remain OFF until
     *    all clients triggers RESET_DATA_OFF_ALWAYS.
     * 6. In case of multiple clients triggering DATA_ON_ALWAYS, data continue to remain ON until
     *    all clients request for RESET_DATA_ON_ALWAYS.
     * 7. In case of multiple clients triggering DATA_ON_ALWAYS & DATA_OFF_ALWAYS, data continue 
     *    to remain OFF until all client request for RESET_DATA_OFF_ALWAYS.
     *    Then, if any pending clients for DATA_ON_ALWAYS, data continue to remain ON until
     *    clients sends RESET_DATA_ON_ALWAYS.
     * 8. Passing "duration" will reset to BatteryManager mode settings once the time elapses.
     *    So no need to trigger RESET_DATA_ON_ALWAYS. Note(1) apply here as well.
     * 9. Broadcast intent BatteryManagerDefs.ACTION_BM_STATE_CHANGED  will be posted
     *    on BM State change, which includes mode and data connection state.
     *    Extra ("KEY_BM_MODE") would contain current Battery Manager Mode.
     *       Value: BatteryManagerDefs.Mode
     *    Extra ("KEY_DATA_CONNECTION") would contain current data state.
     *       Value: BatteryManagerDefs.DataConnection
     *
     * Following table shows a combination of calls from mutiple clients in the order of Client1,
     * Client2 & Client3 and the data connection status.
     *
     * |-----------------|-----------------|-----------------|
     * |   Client1       |   Client2       |   Client3       |
     * |-----------------|-----------------|-----------------|
     * | DATA_OFF_ALWAYS | DATA_OFF_ALWAYS | DATA_OFF_ALWAYS |
     * | (Data --> OFF)  | (Data --> OFF)  |  (Data --> OFF) |
     * |-----------------|-----------------|-----------------|
     * | DATA_OFF_ALWAYS | DATA_OFF_ALWAYS | DATA_ON_ALWAYS  |
     * | (Data --> OFF)  | (Data --> OFF)  |  (Data --> OFF) |
     * |-----------------|-----------------|-----------------|
     * | DATA_OFF_ALWAYS | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  |
     * | (Data --> OFF)  | (Data --> OFF)  |  (Data --> OFF) |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  |
     * | (Data --> ON)   | (Data --> ON)   | (Data --> ON)   |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  | DATA_OFF_ALWAYS |
     * | (Data --> ON)   | (Data --> ON)   | (Data --> OFF)  |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_OFF_ALWAYS | DATA_OFF_ALWAYS |
     * | (Data --> ON)   | (Data --> OFF)  | (Data --> OFF)  |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  |
     * | with Duration   | with Duration   | with Duration   |
     * | (Data --> ON)   | (Data --> ON)   | (Data --> ON)   |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  |
     * | with Duration   |                 |                 |
     * | (Data --> ON)   | (Data --> ON)   | (Data --> ON)   |
     * |-----------------|-----------------|-----------------|
     * | DATA_ON_ALWAYS  | DATA_ON_ALWAYS  | DATA_OFF_ALWAYS |
     * | with Duration   |                 |                 |
     * | (Data --> ON)   | (Data --> ON)   | (Data --> OFF)  |
     * |-----------------|-----------------|-----------------|
     *
     * Following table shows the behaviour on Reset for the above request:
     *
     * |-----------------------|-----------------------|-----------------------|
     * |   Client1             |   Client2             |   Client3             |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_OFF_ALWAYS | RESET_DATA_OFF_ALWAYS | RESET_DATA_OFF_ALWAYS |
     * | (Data --> OFF)        | (Data --> OFF)        | (Data --> ON)         |
     * |                       |                       | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_OFF_ALWAYS | RESET_DATA_OFF_ALWAYS | RESET_DATA_ON_ALWAYS  |
     * | (Data --> OFF)        | (Data --> ON)         | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_OFF_ALWAYS | RESET_DATA_ON_ALWAYS  | RESET_DATA_ON_ALWAYS  |
     * | (Data --> ON)         | (Data --> ON)         | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_ON_ALWAYS  | RESET_DATA_ON_ALWAYS  | RESET_DATA_ON_ALWAYS  |
     * | (Data --> ON)         | (Data --> ON)         | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_ON_ALWAYS  | RESET_DATA_ON_ALWAYS  | RESET_DATA_OFF_ALWAYS |
     * | (Data --> OFF)        | (Data --> OFF)        | (Data --> ON)         |
     * |                       |                       | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | RESET_DATA_ON_ALWAYS  | RESET_DATA_OFF_ALWAYS | RESET_DATA_OFF_ALWAYS |
     * | (Data --> OFF)        | (Data --> OFF)        | (Data --> ON)         |
     * |                       |                       | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | TIMER EXPIRES         | TIMER EXPIRES         | TIMER EXPIRES         |
     * | (Data --> ON)         | (Data --> ON)         | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | TIMER EXPIRES         | RESET_DATA_ON_ALWAYS  | RESET_DATA_ON_ALWAYS  |
     * | (Data --> ON)         | (Data --> ON)         | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     * | TIMER EXPIRES         | RESET_DATA_ON_ALWAYS  | RESET_DATA_OFF_ALWAYS |
     * | (Data --> OFF)        | (Data --> OFF)        | (Data --> ON)         |
     * |                       |                       | Moves to BM Mode      |
     * |-----------------------|-----------------------|-----------------------|
     *
     * @param state DataSettings.DATA_ON_ALWAYS
     *              DataSettings.DATA_OFF_ALWAYS
     *              DataSettings.RESET_DATA_ON_ALWAYS
     *              DataSettings.RESET_DATA_OFF_ALWAYS
     * @param duration Time(in seconds) until when data is to be keep ON.
     *        If no duration, then use BatteryManagerDefs.NO_DURATION
     *        eg. duration=120; will keep data ON for next 2 minutes.
     * @param persistent  true : If request is persistent across power cycle.
     *                    false: If request is only for this power up.
     * @return none
     */
    void changeDataSettings(int state, long duration, boolean persistent);

     /**
      * Allows the specified apn type to be active, when data is OFF.
      * This is not applicable for default type.
      * This API is honoured only in DATA OFF state.
      * @param type Phone.APN_TYPE_MMS
      * [For now ONLY MMS Type is supported]
      * @return int BatteryManagerDefs.APN_ENABLE_REQUEST_ACCEPTED
      *             BatteryManagerDefs.APN_TYPE_NOT_SUPPORTED
      */
    int enableApnType(String type);

    /**
     * Fetches BM current mode
     * @return BatteryManagerDefs.Mode.PERFORMANCE
     *         BatteryManagerDefs.Mode.NIGHT_SAVER
     *         BatteryManagerDefs.Mode.BATTERY_SAVER
     *         BatteryManagerDefs.Mode.CUSTOM
     */
    int getCurrentBatteryManagerMode();

    /**
     * Fetches data connection status
     * @return BatteryManagerDefs.DataConnection.ON
     *         BatteryManagerDefs.DataConnection.OFF
     */
    int getCurrentDataStatus();

    /**
     * Change current BM mode
     * @param mode mode to which BM has to switch over
     *         BatteryManagerDefs.Mode.PERFORMANCE
     *         BatteryManagerDefs.Mode.NIGHT_SAVER
     *         BatteryManagerDefs.Mode.BATTERY_SAVER
     * @return none
     */
    void changeBatteryManagerMode(int mode);
}
