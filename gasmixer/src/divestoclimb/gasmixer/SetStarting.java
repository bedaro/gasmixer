package divestoclimb.gasmixer;

import divestoclimb.android.widget.BaseNumberSelector;
import divestoclimb.gasmixer.prefs.SyncedPrefsHelper;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Show and edit the starting mix, pressure, and cylinder from the state.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class SetStarting extends Activity implements Button.OnClickListener {

	private BaseNumberSelector mPressureSelector;
	private TextView mPressureUnit, mCylinderDescription;
	private TrimixSelector mGasSelector;
	private SharedPreferences mSettings, mState;
	private Units mUnits;
	private CylinderORMapper mCylORMapper;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.starting);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);
		
		if(mSettings.getBoolean("vdw", false)) {
			setContentView(R.layout.set_start_cylinder);
		} else {
			setContentView(R.layout.set_start);
		}

		int unit;
		if(mSettings.contains("units")) {
			// Android issue 2096 - ListPreference won't work with an integer
			// array for values. Unit values are being stored as Strings then
			// we convert them here for use.
			unit = Integer.valueOf(mSettings.getString("units", "0"));
		} else {
			Cursor c = new SyncedPrefsHelper(this).findSetValue("units");
			unit = c == null? 0: Integer.valueOf(c.getString(c.getColumnIndexOrThrow("units")));
			mSettings.edit().putString("units", Integer.toString(unit)).commit();
		}
		mUnits = new Units(unit);

		mCylORMapper = new CylinderORMapper(this, mUnits);

		mPressureSelector = (BaseNumberSelector)findViewById(R.id.pressure);
		mPressureUnit = (TextView)findViewById(R.id.pressure_unit);
		mGasSelector = (TrimixSelector)findViewById(R.id.mix);
		mCylinderDescription = (TextView)findViewById(R.id.cylinder);

		Button button = (Button)findViewById(R.id.button);
		button.setOnClickListener(this);

		Button cylinderChangeButton = (Button)findViewById(R.id.cylinder_change);
		if(cylinderChangeButton != null) {
			cylinderChangeButton.setOnClickListener(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		final Units u = mUnits;
		final String pressureUnit = getString(u.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric);
		mPressureSelector.setDecimalPlaces(0);
		mPressureSelector.setLimits(0f, new Float(u.pressureTankMax()));
		mPressureSelector.setIncrement(new Float(u.pressureIncrement()));
		mPressureSelector.setValue(mState.getFloat("start_pres", 0));
		mPressureUnit.setText(pressureUnit + ":");
		mGasSelector.setMix(new Mix(mState.getFloat("start_o2", 0.21f), mState.getFloat("start_he", 0)));

		if(mCylinderDescription != null) {
			updateCylinder();
		}
	}

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button:
			Mix m = mGasSelector.getMix();
			mState.edit()
				.putFloat("start_pres", mPressureSelector.getValue())
				.putFloat("start_o2", (float)m.getfO2())
				.putFloat("start_he", (float)m.getfHe())
				.commit();
			setResult(RESULT_OK);
			finish();
			break;
		case R.id.cylinder_change:
			Intent cylinders = new Intent(Intent.ACTION_GET_CONTENT);
			cylinders.setType("vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size");
			startActivityForResult(cylinders, 0);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode == RESULT_OK) {
			Bundle b = intent.getExtras();
			long id = b.getLong("selected");
			mState.edit().putLong("cylinderid", id).commit();
			updateCylinder();
		}
	}

	private void updateCylinder() {
		final Cylinder c = mCylORMapper.fetchCylinder(mState.getLong("cylinderid", -1));
		if(c != null) {
			mCylinderDescription.setText(c.getName());
		}
	}
}