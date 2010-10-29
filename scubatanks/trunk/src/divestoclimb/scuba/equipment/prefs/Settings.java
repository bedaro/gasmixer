package divestoclimb.scuba.equipment.prefs;

import java.text.SimpleDateFormat;
import java.util.Date;

import divestoclimb.scuba.equipment.R;
import divestoclimb.scuba.equipment.widget.NumberPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private NumberPreference mHydroInterval, mVisualInterval;
	private ListPreference mUnitsPreference, mDateFormat;
	private SyncedPrefsHelper mSyncedPrefsHelper;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.preferences);
		
		mSyncedPrefsHelper = new SyncedPrefsHelper(this);
		
		PreferenceScreen screen = getPreferenceScreen();
		mHydroInterval = (NumberPreference)screen.findPreference("hydro_interval_years");
		mVisualInterval = (NumberPreference)screen.findPreference("visual_interval_months");
		mUnitsPreference = (ListPreference)screen.findPreference("units");
		mDateFormat = (ListPreference)screen.findPreference("date_format");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
		settings.registerOnSharedPreferenceChangeListener(this);
		settings.registerOnSharedPreferenceChangeListener(mSyncedPrefsHelper);
		
		onSharedPreferenceChanged(settings, "hydro_interval_years");
		onSharedPreferenceChanged(settings, "visual_interval_months");
		onSharedPreferenceChanged(settings, "units");
		onSharedPreferenceChanged(settings, "date_format");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
		settings.unregisterOnSharedPreferenceChangeListener(this);
		settings.unregisterOnSharedPreferenceChangeListener(mSyncedPrefsHelper);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals("hydro_interval_years")) {
			mHydroInterval.setSummary(String.format("%d %s", sharedPreferences.getInt(key, 5), getString(R.string.years)));
		} else if(key.equals("visual_interval_months")) {
			mVisualInterval.setSummary(String.format("%d %s", sharedPreferences.getInt(key, 12), getString(R.string.months)));
		} else if(key.equals("units")) {
			mUnitsPreference.setSummary(getResources().getStringArray(R.array.units)[Integer.valueOf(sharedPreferences.getString(key, "0"))]);
		} else if(key.equals("date_format")) {
			String format = sharedPreferences.getString(key, "MM/yy");
			SimpleDateFormat fmt = new SimpleDateFormat(format);
			String fmtLabels[] = getResources().getStringArray(R.array.date_formats),
				fmts[] = getResources().getStringArray(R.array.date_format_values),
				formatLabel = "";
			for(int i = 0; i < fmtLabels.length; i ++) {
				if(fmts[i].equals(format)) {
					formatLabel = fmtLabels[i];
					break;
				}
			}
			mDateFormat.setSummary(formatLabel + " (" + fmt.format(new Date()) + ")");
		}
	}
}