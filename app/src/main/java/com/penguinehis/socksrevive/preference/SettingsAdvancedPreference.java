package com.penguinehis.socksrevive.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.penguinehis.socksrevive.R;
import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.config.SettingsConstants;
import com.penguinehis.ultrasshservice.logger.ConnectionStatus;
import com.penguinehis.ultrasshservice.logger.SkStatus;

public class SettingsAdvancedPreference extends PreferenceFragmentCompat
	implements SettingsConstants, SkStatus.StateListener,
		Preference.OnPreferenceChangeListener
{
	private SharedPreferences mPref;

	@Override
    public void onCreatePreferences(Bundle bundle, String s)
	{
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.advanced_settings_preference, s);

		mPref = getPreferenceManager()
			.getDefaultSharedPreferences(getContext());
			
		Settings config = new Settings(getContext());

		/*ListPreference numberMaxThreads = (ListPreference)
			findPreference(MAXIMO_THREADS_KEY);
		numberMaxThreads.setOnPreferenceChangeListener(this);*/
		
		// update views
		getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
	}

	@Override
	public void onResume()
	{
		super.onResume();

		SkStatus.addStateListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		SkStatus.removeStateListener(this);
	}

	@Override
	public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent)
	{
		getView().post(new Runnable() {
			@Override
			public void run() {
				getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
			}
		});
	}
	

	/**
	 * Preference.OnPreferenceChangeListener
	 * Implementação
	 */

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		switch(pref.getKey()) {
			case MODO_DEBUG_KEY:
				boolean isDebug = (boolean) newValue;

				if (isDebug) {
					Toast.makeText(getContext(), "Desative após terminar os testes",
						Toast.LENGTH_SHORT).show();
				}
			break;

			case FILTER_APPS:
				boolean isEnabled = (boolean) newValue;
				
				enableFilterLayout(isEnabled);
			break;
		}

		return true;
	}
	
	private void enableFilterLayout(boolean is) {
		String[] list = {
			FILTER_BYPASS_MODE,
			FILTER_APPS_LIST
		};

		for (String key : list) {
			findPreference(key).setEnabled(is);
		}
	}
	

	/**
	 * Utils
	 */

	public static void setListPreferenceSummary(ListPreference pref, String value) {
		int index = pref.findIndexOfValue(value);
		if (index >= 0) {
			pref.setSummary(pref.getEntries()[index]);
		}
	}
}
