package com.motorola.contextual.pickers.conditions.location;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.locations.LocConstants;
import com.motorola.contextual.smartprofile.locations.LocationUtils;

/**
 * This class extends {@link CommandHandler} and handles Refresh command
 *
 * @author wkh346
 *
 */
public class LocationRefreshHandler extends CommandHandler implements
        LocConstants {

    private static final String TAG = LocationRefreshHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            sendRefreshResponse(context, intent);
            return SUCCESS;
        }
        return FAILURE;
    }

    /**
     * This method broadcasts {@link Constants#REFRESH_RESPONSE} intent
     *
     * @param context
     *            - the application's context
     * @param intent
     *            - the {@link Constants#REFRESH_RESPONSE} intent
     */
    private void sendRefreshResponse(Context context, Intent intent) {
        Intent refreshIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        refreshIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);
        refreshIntent.putExtra(EXTRA_PUB_KEY, LOCATION_PUBLISHER_KEY);
        refreshIntent.putExtra(EXTRA_RESPONSE_ID,
                intent.getStringExtra(EXTRA_REQUEST_ID));
        refreshIntent.putExtra(EXTRA_STATUS, SUCCESS);
        String config = LocationUtils.getNewConfigFromOldConfig(context,
                intent.getStringExtra(EXTRA_CONFIG));
        String description = LocationUtils.getDescription(context, config);
        String state = LocationUtils.getConfigState(context, config);
        refreshIntent.putExtra(EXTRA_DESCRIPTION, description);
        refreshIntent.putExtra(EXTRA_STATE, state);
        refreshIntent.putExtra(EXTRA_CONFIG, config);
        context.sendBroadcast(refreshIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG, "sendRefreshResponse refresh response send for config "
                    + config + " with state " + state + " with description "
                    + description);
        }
    }

}
