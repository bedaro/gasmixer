package divestoclimb.scuba.equipment;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.NumberSelector.ValueChangedListener;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CylinderEdit extends Activity implements OnClickListener, ValueChangedListener, OnCheckedChangeListener {

	public static final String KEY_ACTION="action";
	public static final int ACTION_NEW=1;
	public static final int ACTION_EDIT=2;

	private Cylinder mCylinder;
	private boolean mInitialized = false;

	private int mAction;
	private Long mId;
	private EditText mName;
	private NumberSelector mInternalVolume, mCapacity, mServPressure;
	private TextView mInternalVolumeUnit, mCapacityUnit, mServPressureUnit;
	
	private Units mUnits;
	
	// Coupling parameters. See comments in onChange
	private int mUserLastSetting = 0;
	private static final int SETTING_CAPACITY = 1;
	private static final int SETTING_VOLUME = 2;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Bundle params = icicle != null? icicle: getIntent().getExtras();
		mAction = params.getInt(KEY_ACTION);
		if(mAction == ACTION_EDIT) {
			mId = params.getLong(CylinderSizeClient._ID);
			setTitle(R.string.edit_cylinder);
		} else {
			mId = null;
			setTitle(R.string.create_cylinder);
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		int unit;
		if(icicle == null) {
			// Android issue 2096 - ListPreference won't work with an integer
			// array for values. Unit values are being stored as Strings then
			// we convert them here for use.
			try {
				unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
			} catch(ParseException e) { unit = 0; }
		} else {
			unit = icicle.getBoolean("unit_metric")? Units.METRIC: Units.IMPERIAL;
		}
		mUnits = new Units(unit);

		setContentView(R.layout.edit_cylinder);

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

		ToggleButton toggleUnit = (ToggleButton)findViewById(R.id.metric_toggle);
		toggleUnit.setChecked(unit == Units.METRIC);
		toggleUnit.setOnCheckedChangeListener(this);

		// Restore from saved state if there was one
		if(icicle != null) {
			float vol = icicle.getFloat("volume"),
					pressure = icicle.getFloat("pressure");
			mName.setText(icicle.getString("name"));
			mInternalVolume.setValue(vol);
			mCapacity.setValue(icicle.getFloat("capacity"));
			mServPressure.setValue(pressure);
			mCylinder = new Cylinder(mUnits, vol, (int)pressure);
			mInitialized = true;
		}

		Button ok = (Button)findViewById(R.id.button_ok),
				cancel = (Button)findViewById(R.id.button_cancel),
				help = (Button)findViewById(R.id.button_help),
				delete = (Button)findViewById(R.id.button_delete);
		ok.setOnClickListener(this);
		cancel.setOnClickListener(this);
		help.setOnClickListener(this);
		delete.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUnits(null);
		if(! mInitialized) {
			initFields();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_ACTION, mAction);
		if(mAction == ACTION_EDIT) {
			outState.putLong(CylinderSizeClient._ID, mId);
		}
		outState.putString("name", mName.getText().toString());
		outState.putFloat("volume", mInternalVolume.getValue());
		outState.putFloat("capacity", mCapacity.getValue());
		outState.putFloat("pressure", mServPressure.getValue());
		outState.putBoolean("unit_metric", mUnits.getCurrentSystem() == Units.METRIC);
	}

	private void initFields() {
		Units u = mUnits;
		if(mAction == ACTION_EDIT) {
			Cursor c = getContentResolver().query(
					Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
							String.valueOf(mId)
					), null, null, null, null);
			c.moveToFirst();
			mName.setText(c.getString(c.getColumnIndexOrThrow(CylinderSizeClient.NAME)));
			mCylinder = CylinderSizeClient.cursorToCylinder(c, u);
			c.close();
		} else {
			mCylinder = new Cylinder(u, u.volumeToCapacity(u.volumeNormalTank()), (int)Math.round(u.pressureTankFull()));
		}
		mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
		mCapacity.setValue(mCylinder.getVdwCapacity());
		mServPressure.setValue(mCylinder.getServicePressure());
		mInitialized = true;
	}

	private void updateUnits(Integer last_unit) {
		Units u = mUnits;
		mServPressure.setDecimalPlaces(0);
		mServPressure.setLimits(0f, new Float(u.pressureTankMax()));
		mServPressure.setIncrement(new Float(u.pressureIncrement()));
		mServPressure.setNonIncrementValues(u.pressureNonstandard());
		mServPressureUnit.setText(Params.pressure(this, u));

		mInternalVolume.setDecimalPlaces(u.volumePrecision());
		mInternalVolume.setLimits(0f, null);
		mInternalVolume.setIncrement(u.volumeIncrement());
		mInternalVolumeUnit.setText(Params.volume(this, u));

		mCapacity.setDecimalPlaces(u.capacityPresision());
		mCapacity.setLimits(0f, null);
		mCapacity.setIncrement(u.capacityIncrement());
		mCapacityUnit.setText(Params.capacity(this, u));

		if(last_unit != null) {
			// Convert existing values to new units
			mCylinder.setServicePressure((int)Math.round(u.convertPressure(mCylinder.getServicePressure(), last_unit)));
			mCylinder.setInternalVolume(u.convertCapacity(mCylinder.getInternalVolume(), last_unit));
			mServPressure.setValue(mCylinder.getServicePressure());
			mInternalVolume.setValue(u.capacityToVolume(mCylinder.getInternalVolume()));
			mCapacity.setValue(mCylinder.getVdwCapacity());
		}
	}

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_delete:
			if(mAction == ACTION_EDIT) {
				getContentResolver().delete(
						Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
								String.valueOf(mId)
						), null, null);
			}
			finish();
			break;
		case R.id.button_cancel:
			finish();
			break;
		case R.id.button_help:
			Intent i = new Intent(this, Info.class);
			i.putExtra(Info.KEY_TITLE, R.string.help_edit);
			i.putExtra(Info.KEY_TEXT, R.string.help_edit_text);
			startActivity(i);
			break;
		case R.id.button_ok:
			ContentValues values =
				CylinderSizeClient.cylinderToContentValues(
						mCylinder, mName.getText().toString()
				);
			if(mId == null) {
				// Insert as a new cylinder size
				getContentResolver().insert(CylinderSizeClient.CONTENT_URI, values);
			} else {
				getContentResolver().update(Uri.withAppendedPath(
						CylinderSizeClient.CONTENT_URI,
						String.valueOf(mId)
					), values, null, null);
			}
			finish();
		}
	}

	public void onChange(NumberSelector ns, Float new_val, boolean from_user) {
		if(! from_user) {
			return;
		}
		Units u = mUnits;
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

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int last_unit = mUnits.getCurrentSystem(),
				unit = isChecked? Units.METRIC: Units.IMPERIAL;
		mUnits.change(unit);
		updateUnits(last_unit);
	}
}