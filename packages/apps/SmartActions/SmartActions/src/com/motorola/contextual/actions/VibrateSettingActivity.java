package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

/**
 * This class extends {@link Activity} class, shows UI and provides methods for
 * allowing the user to configure the vibrate setting
 *
 * @author wkh346
 *
 */
public class VibrateSettingActivity extends Activity implements Constants {

    /**
     * TAG for logging
     */
    @SuppressWarnings("unused")
	private static final String TAG = VibrateSettingActivity.class
            .getSimpleName();

    /**
     * This holds current vibrate setting configured by the user
     */
    private int mVibrateSetting = Volumes.VIBRATE_DONT_ADJUST;

    /**
     * The 'Turn vibrate on' radio button
     */
    private RadioButton mVibrateOnButton;

    /**
     * The 'Turn vibrate off' radio button
     */
    private RadioButton mVibrateOffButton;

    /**
     * The 'Don't adjust vibrate' radio button
     */
    private RadioButton mVibrateDontAdjustButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vibrate_setting);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.volumes);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility();
        Intent incomingIntent = getIntent();
        mVibrateSetting = incomingIntent.getIntExtra(EXTRA_VOL_VIBRATE_SETTING,
                Volumes.VIBRATE_DONT_ADJUST);
        initialize();
    }

    /**
     * This method initializes the variables and views
     */
    private void initialize() {
        LinearLayout vibrateOnLayout = (LinearLayout) findViewById(R.id.vibrate_setting_layout_vibrate_on);
        LinearLayout vibrateOffLayout = (LinearLayout) findViewById(R.id.vibrate_setting_layout_vibrate_off);
        LinearLayout vibrateDontAdjustLayout = (LinearLayout) findViewById(R.id.vibrate_setting_layout_vibrate_dont_adjust);
        vibrateOnLayout.setOnClickListener(mViewClickListener);
        vibrateOffLayout.setOnClickListener(mViewClickListener);
        vibrateDontAdjustLayout.setOnClickListener(mViewClickListener);
        mVibrateOnButton = (RadioButton) findViewById(R.id.vibrate_setting_button_vibrate_on);
        mVibrateOffButton = (RadioButton) findViewById(R.id.vibrate_setting_button_vibrate_off);
        mVibrateDontAdjustButton = (RadioButton) findViewById(R.id.vibrate_setting_button_vibrate_dont_adjust);
        mVibrateOnButton.setOnClickListener(mViewClickListener);
        mVibrateOffButton.setOnClickListener(mViewClickListener);
        mVibrateDontAdjustButton.setOnClickListener(mViewClickListener);
        refreshButtons();

    }

    /**
     * This handles the click events on views in this activity
     */
    private View.OnClickListener mViewClickListener = new View.OnClickListener() {

        public void onClick(View view) {
            switch (view.getId()) {
            case R.id.vibrate_setting_layout_vibrate_on:
            case R.id.vibrate_setting_button_vibrate_on: {
                mVibrateSetting = Volumes.VIBRATE_ON;
                break;
            }
            case R.id.vibrate_setting_layout_vibrate_off:
            case R.id.vibrate_setting_button_vibrate_off: {
                mVibrateSetting = Volumes.VIBRATE_OFF;
                break;
            }
            case R.id.vibrate_setting_layout_vibrate_dont_adjust:
            case R.id.vibrate_setting_button_vibrate_dont_adjust: {
                mVibrateSetting = Volumes.VIBRATE_DONT_ADJUST;
                break;
            }
            }
            refreshButtons();
        }
    };

    /**
     * This method checks/unchecks the radio buttons appropriately
     */
    private void refreshButtons() {
        mVibrateOnButton
                .setChecked((mVibrateSetting == Volumes.VIBRATE_ON) ? true
                        : false);
        mVibrateOffButton
                .setChecked((mVibrateSetting == Volumes.VIBRATE_OFF) ? true
                        : false);
        mVibrateDontAdjustButton
                .setChecked((mVibrateSetting == Volumes.VIBRATE_DONT_ADJUST) ? true
                        : false);
    }

    /**
     * This method sets action bar visibility
     */
    protected void setupActionBarItemsVisibility() {
        int editFragmentOption = EditFragment.EditFragmentOptions.RULES_BUILDER_EDIT_MODE_SAVE_ENABLED;
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction()
                .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_rb_save:
            sendResult(RESULT_OK);
            result = true;
            break;
        case R.id.edit_cancel:
            sendResult(RESULT_CANCELED);
            result = true;
            break;
        }
        return result;
    }

    /**
     * This method sends result to the calling activity
     *
     * @param resultCode
     *            - the result code
     */
    private void sendResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            Intent data = new Intent();
            data.putExtra(EXTRA_VOL_VIBRATE_SETTING, mVibrateSetting);
            setResult(RESULT_OK, data);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
