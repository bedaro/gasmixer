package divestoclimb.gasmixer.prefs;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.android.widget.NumberPreference;
import divestoclimb.gasmixer.AndroidLocalizer;
import divestoclimb.gasmixer.Params;
import divestoclimb.gasmixer.R;
import divestoclimb.gasmixer.TemperaturePreference;
import divestoclimb.gasmixer.TrimixPreference;
import divestoclimb.lib.scuba.Localizer;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * Preferences activity that shows the current values of complex Preference items
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private TrimixPreference mTopupGasPreference;
	private TemperaturePreference mTemperaturePreference;
	private NumberPreference mMaxPo2Preference, mMaxHighPo2Preference;
	private ListPreference mUnitsPreference;
	private SyncedPrefsHelper mSyncedPrefsHelper;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		addPreferencesFromResource(R.xml.preferences);
		
		mSyncedPrefsHelper = new SyncedPrefsHelper(this);

		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));

		PreferenceScreen screen = getPreferenceScreen();
		mTopupGasPreference = (TrimixPreference)screen.findPreference("topup_gas");
		mTemperaturePreference = (TemperaturePreference)screen.findPreference("temperature");
		mUnitsPreference = (ListPreference)screen.findPreference("units");
		mMaxPo2Preference = (NumberPreference)screen.findPreference("max_norm_po2");
		mMaxHighPo2Preference = (NumberPreference)screen.findPreference("max_hi_po2");
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
		settings.registerOnSharedPreferenceChangeListener(this);
		settings.registerOnSharedPreferenceChangeListener(mSyncedPrefsHelper);
		onSharedPreferenceChanged(settings, "topup_gas");
		onSharedPreferenceChanged(settings, "units");
		// Temperature is automatically set when units is updated, so we don't
		// explicitly call it
		onSharedPreferenceChanged(settings, "max_norm_po2");
		onSharedPreferenceChanged(settings, "max_hi_po2");
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
		settings.unregisterOnSharedPreferenceChangeListener(this);
		settings.unregisterOnSharedPreferenceChangeListener(mSyncedPrefsHelper);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals("topup_gas")) {
			Mix topup = TrimixPreference.stringToMix(sharedPreferences.getString("topup_gas", "0.21 0"));
			if(topup != null) {
				mTopupGasPreference.setSummary(topup.toString());
			}
			return;
		} else if(key.equals("max_norm_po2") || key.equals("max_hi_po2")) {
			NumberPreference pref;
			float defaultValue;
			if(key.equals("max_norm_po2")) {
				pref = mMaxPo2Preference;
				defaultValue = 1.4f;
			} else {
				pref = mMaxHighPo2Preference;
				defaultValue = 1.6f;
			}
			String summary = Params.mPartialPressure.format(sharedPreferences.getFloat(key, defaultValue)) + " " + getString(R.string.abs_pres);
			pref.setSummary(summary);
			return;
		}

		// The rest of the preferences are units-dependent. So look up the current unit system
		int unit;
		try {
			unit = NumberFormat.getIntegerInstance().parse(sharedPreferences.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		final Units units = new Units(unit);

		final boolean newUnits = key.equals("units");
		if(newUnits) {
			// The units system itself changed. We have to redo all the rest of the summaries.
			mUnitsPreference.setSummary(getResources().getStringArray(R.array.units)[unit]);
		}
		if(newUnits || key.equals("temperature")) {
			final float temperature = units.tempAbsToRel(units.convertAbsTemp(sharedPreferences.getFloat("temperature", 294), Units.METRIC));
			final String tempUnit = getString(unit == Units.IMPERIAL? R.string.reltemp_imperial: R.string.reltemp_metric);
			mTemperaturePreference.setSummary(NumberFormat.getIntegerInstance().format(temperature) + " " + tempUnit);
		}
	}
}