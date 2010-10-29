package divestoclimb.scuba.equipment;

import divestoclimb.android.widget.BaseNumberSelector;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.prefs.SyncedPrefsHelper;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;
import divestoclimb.scuba.equipment.widget.NumberSelector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class CylinderSizeEdit extends Activity implements View.OnClickListener,
		BaseNumberSelector.ValueChangedListener, CompoundButton.OnCheckedChangeListener {

	public static final String KEY_ACTION = "action";
	public static final int ACTION_NEW = 1;
	public static final int ACTION_EDIT = 2;
	
	public static final String KEY_NEWTYPE = "type";
	public static final String KEY_CYLINDER = "cylinder";

	private Cylinder mCylinder;
	private SharedPreferences mSettings;
	private boolean mInitialized = false;

	private EditText mName;
	private NumberSelector mInternalVolume, mCapacity, mServPressure;
	private TextView mInternalVolumeUnit, mCapacityUnit, mServPressureUnit;
	private ToggleButton mToggleUnit;
	
	private CylinderORMapper mORMapper;
	private Units mUnits;
	
	// Coupling parameters. See comments in onChange
	private int mUserLastSetting = 0;
	private static final int SETTING_CAPACITY = 1;
	private static final int SETTING_VOLUME = 2;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle params = icicle != null? icicle: getIntent().getExtras();
		if(params.containsKey(KEY_CYLINDER)) {
			mCylinder = (Cylinder)params.getSerializable(KEY_CYLINDER);
			mUnits = mCylinder.getUnits();
			mORMapper = new CylinderORMapper(this, mUnits);
		} else {
			int action = params.getInt(KEY_ACTION);

			int unit;
			if(icicle == null) {
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
			} else {
				unit = icicle.getBoolean("unit_metric")? Units.METRIC: Units.IMPERIAL;
			}
			mUnits = new Units(unit);

			mORMapper = new CylinderORMapper(this, mUnits);
			
			if(action == ACTION_EDIT) {
				mCylinder = mORMapper.fetchCylinder(params.getLong(CylinderORMapper._ID));
			} else {
				mCylinder = new Cylinder(mUnits, mUnits.volumeToCapacity(mUnits.volumeNormalTank()), (int)Math.round(mUnits.pressureTankFull()));
				mCylinder.setType(params.getInt(KEY_NEWTYPE));
			}
		}

		setContentView(R.layout.cylinder_size_edit);

		mName = (EditText)findViewById(R.id.name);
		mInternalVolume = (NumberSelector)findViewById(R.id.internal_volume);
		mInternalVolume.setValueChangedListener(this);
		mInternalVolumeUnit = (TextView)findViewById(R.id.internal_volume_unit);
		mCapacity = (NumberSelector)findViewById(R.id.capacity);
		mCapacity.setValueChangedListener(this);
		mCapacityUnit = (TextView)findViewById(R.id.capacity_unit);
		mServPressure = (NumberSelector)findViewById(R.id.serv_pressure);
		mServPressure.setValueChangedListener(this);
		mServPressureUnit = (TextView)findViewById(R.id.serv_pressure_unit);

		mToggleUnit = (ToggleButton)findViewById(R.id.metric_toggle);
		mToggleUnit.setOnCheckedChangeListener(this);
		
		if(mCylinder.getType() == Cylinder.TYPE_SPECIFIC) {
			findViewById(R.id.name_prompt).setVisibility(View.GONE);
			mName.setVisibility(View.GONE);
		}

		final Button ok = (Button)findViewById(R.id.button_ok),
				delete = (Button)findViewById(R.id.button_delete);
		// delete is only visible when editing a generic. When creating
		// a specific, ok says "Next"
		if(mORMapper.isPhantom(mCylinder)) {
			delete.setVisibility(View.GONE);
			if(mCylinder.getType() == Cylinder.TYPE_SPECIFIC) {
				ok.setText(R.string.next);
			}
			setTitle(R.string.create_cylinder_size);
		} else if(mCylinder.getType() == Cylinder.TYPE_GENERIC) {
			delete.setOnClickListener(this);
			setTitle(R.string.edit_cylinder_size);
		} else {
			delete.setVisibility(View.GONE);
			setTitle(R.string.edit_cylinder_size);
		}
		ok.setOnClickListener(this);
		findViewById(R.id.button_cancel).setOnClickListener(this);
		findViewById(R.id.button_help).setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Integer unit = Integer.valueOf(mSettings.getString("units", "0")),
			last_unit = mUnits.getCurrentSystem();
		if(unit != last_unit) {
			mUnits.change(unit);
		} else {
			last_unit = null;
		}
		updateUnits(last_unit);
		mToggleUnit.setChecked(unit == Units.METRIC);
		if(! mInitialized) {
			mInternalVolume.setValue(mUnits.capacityToVolume(mCylinder.getInternalVolume()));
			if(mCylinder.getType() == Cylinder.TYPE_GENERIC) {
				mName.setText(mCylinder.getName());
			}
			mCapacity.setValue(mCylinder.getVdwCapacity());
			mServPressure.setValue(mCylinder.getServicePressure());
			mInitialized = true;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mCylinder.getType() == Cylinder.TYPE_GENERIC) {
			mCylinder.setName(mName.getText().toString());
		}
		outState.putSerializable(KEY_CYLINDER, mCylinder);
	}

	private void updateUnits(Integer last_unit) {
		Units u = mUnits;
		mServPressure.setDecimalPlaces(0);
		mServPressure.setLimits(0f, new Float(u.pressureTankMax()));
		mServPressure.setIncrement(new Float(u.pressureIncrement()));
		mServPressure.setNonIncrementValues(u.pressureNonstandard());
		mServPressureUnit.setText(getText(u.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric));

		mInternalVolume.setDecimalPlaces(u.volumePrecision());
		mInternalVolume.setLimits(0f, null);
		mInternalVolume.setIncrement(u.volumeIncrement());
		mInternalVolumeUnit.setText(getText(u.volumeUnit() == Units.IMPERIAL? R.string.volume_imperial: R.string.volume_metric));

		mCapacity.setDecimalPlaces(u.capacityPresision());
		mCapacity.setLimits(0f, null);
		mCapacity.setIncrement(u.capacityIncrement());
		mCapacityUnit.setText(getText(u.capacityUnit() == Units.IMPERIAL? R.string.capacity_imperial: R.string.capacity_metric));

		if(last_unit != null) {
			// Convert existing values to new units
			mCylinder.setServicePressure((int)Math.round(u.convertPressure(mCylinder.getServicePressure(), last_unit)));
			mCylinder.setInternalVolume(u.convertCapacity(mCylinder.getInternalVolume(), last_unit));
			mServPressure.setValue(mCylinder.getServicePressure());
			mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
			mCapacity.setValue(mCylinder.getVdwCapacity());
		}
	}

	@Override
	public void onClick(View v) {
		final Intent i;
		switch(v.getId()) {
		case R.id.button_delete:
			if(! mORMapper.delete(mCylinder)) {
				Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show();
			} else {
				finish();
			}
			break;
		case R.id.button_cancel:
			finish();
			break;
		case R.id.button_help:
			i = new Intent(this, Info.class);
			i.putExtra(Info.KEY_TITLE, R.string.help_edit);
			i.putExtra(Info.KEY_TEXT, R.string.help_edit_text);
			startActivity(i);
			break;
		case R.id.button_ok:
			if(mCylinder.getType() == Cylinder.TYPE_SPECIFIC && mORMapper.isPhantom(mCylinder)) {
				// Proceed to step 2 of creation
				i = new Intent(this, CylinderEdit.class);
				i.putExtra(CylinderEdit.KEY_CYLINDER, mCylinder);
				startActivity(i);
				finish();
			} else {
				if(mCylinder.getType() == Cylinder.TYPE_GENERIC) {
					mCylinder.setName(mName.getText().toString());
				}
				if(! mORMapper.save(mCylinder)) {
					Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
				} else {
					finish();
				}
			}
		}
	}

	@Override
	public void onChange(BaseNumberSelector ns, Float new_val, boolean from_user) {
		if(! from_user) {
			return;
		}
		final Units u = mUnits;
		// This function assumes the user is going to set either volume or
		// capacity, and is always going to select service pressure. So the
		// capacity and volume NumberSelectors are coupled to each other.
		// Service pressure is more complicated...
		switch(ns.getId()) {
		case R.id.capacity:
			mCylinder.setVdwCapacity(new_val);
			mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
			if(from_user) {
				mUserLastSetting = SETTING_CAPACITY;
			}
			break;
		case R.id.internal_volume:
			mCylinder.setInternalVolume(u.volumeToCapacity(new_val));
			mCapacity.setValue(mCylinder.getVdwCapacity());
			if(from_user) {
				mUserLastSetting = SETTING_VOLUME;
			}
			break;
		case R.id.serv_pressure:
			mCylinder.setServicePressure((int)Math.round(new_val));
			// When the service pressure changes, we need to know if the user
			// wants us to adjust internal volume or capacity to compensate.
			// To handle this, we've been keeping track of the last of the two
			// that the user set manually (mUserLastSetting) so we can make a
			// good guess which one the user doesn't want us to touch.
			switch(mUserLastSetting) {
			case SETTING_VOLUME:
				// We're good. The Cylinder object keeps internal volume constant
				// when the pressure changes. All we have to do is update the
				// capacity value in the UI.
				mCapacity.setValue(mCylinder.getVdwCapacity());
				break;
			case SETTING_CAPACITY:
				// Changing the service pressure above implicitly modified the
				// cylinder's capacity. Set the capacity back to what the user
				// dialed in.
				mCylinder.setVdwCapacity(mCapacity.getValue());
				mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
				break;
			default:
				// The user has done nothing except update the service pressure.
				// Great...we use the current unit system to guess what the user
				// wants based on our knowledge of measuring conventions in each
				// system.
				switch(u.getCurrentSystem()) {
				case Units.IMPERIAL:
					// Assume the user wants to keep capacity constant
					mCylinder.setVdwCapacity(mCapacity.getValue());
					mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
					break;
				case Units.METRIC:
				default:
					// Assume the user wants to keep internal volume constant
					mCapacity.setValue(mCylinder.getVdwCapacity());
				}
			}
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		final int last_unit = mUnits.getCurrentSystem(),
				unit = isChecked? Units.METRIC: Units.IMPERIAL;
		if(unit != last_unit) {
			mUnits.change(unit);
			updateUnits(last_unit);
		}
	}
}