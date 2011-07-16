package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.android.widget.NumberPreference;
import divestoclimb.lib.scuba.Units;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class TemperaturePreference extends NumberPreference {
	
	private Units mUnits;

	public TemperaturePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setUnits();
	}
	
	// Not sure if I need these overrides...
	public TemperaturePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public TemperaturePreference(Context context) {
		this(context, null);
	}
	
	protected void setUnits() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);
	}

	@Override
	public void setValue(float value) {
		// Value comes in as a relative temperature in the current unit
		// system. Convert it to Kelvin.
		super.setValue(Units.convertAbsTemp(mUnits.tempRelToAbs(value), mUnits.getCurrentSystem(), Units.METRIC));
	}

	@Override
	public float getValue() {
		// The superclass getValue() will return a temperature in Kelvin.
		// Convert it to a relative temperaute in the current unit system.
		return mUnits.tempAbsToRel(mUnits.convertAbsTemp(super.getValue(), Units.METRIC));
	}
	
	@Override
	protected void onBindDialogView(View view) {
		setUnits();
		mUnitLabel.setText(
				getContext().getString(mUnits.relTempUnit() == Units.IMPERIAL?
						R.string.reltemp_imperial:
							R.string.reltemp_metric)
		+ ":");
		super.onBindDialogView(view);
	}
}