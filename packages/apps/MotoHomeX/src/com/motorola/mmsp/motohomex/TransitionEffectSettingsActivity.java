package com.motorola.mmsp.motohomex;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.motorola.mmsp.motohomex.R;

public class TransitionEffectSettingsActivity extends PreferenceActivity implements
		Preference.OnPreferenceChangeListener {
	private static final String WORKSPACE_TRANSITION_EFFECT = "workspace_transition_effect";
	private static final String APPS_TRANSITION_EFFECT = "apps_transition_effect";
	private ListPreference mWorkspaceTransitionEffect;
	private ListPreference mAppsTransitionEffect;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.home_transition_effect);

		int effect;

		effect = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(this)
				.getString(WORKSPACE_TRANSITION_EFFECT, "0"));

		mWorkspaceTransitionEffect =
				(ListPreference) findPreference(WORKSPACE_TRANSITION_EFFECT);

		mWorkspaceTransitionEffect.setValue(String.valueOf(effect));
		final CharSequence[] entries = mWorkspaceTransitionEffect.getEntries();
		mWorkspaceTransitionEffect.setSummary(entries[effect]);

		mWorkspaceTransitionEffect.setOnPreferenceChangeListener(this);


		effect = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(this)
				.getString(APPS_TRANSITION_EFFECT, "0"));

		mAppsTransitionEffect =
				(ListPreference) findPreference(APPS_TRANSITION_EFFECT);

		mAppsTransitionEffect.setValue(String.valueOf(effect));
		mAppsTransitionEffect.setSummary(
				mAppsTransitionEffect.getEntries()[effect]);

		mAppsTransitionEffect.setOnPreferenceChangeListener(this);

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (WORKSPACE_TRANSITION_EFFECT.equals(key)) {
            final CharSequence[] entries = mWorkspaceTransitionEffect.getEntries();
            int effect = Integer.parseInt((String) newValue);
            preference.setSummary(entries[effect]);
        } else if (APPS_TRANSITION_EFFECT.equals(key)) {
            final CharSequence[] entries = mAppsTransitionEffect.getEntries();
            int effect = Integer.parseInt((String) newValue);
            preference.setSummary(entries[effect]);
        }

		return true;
	}

}
