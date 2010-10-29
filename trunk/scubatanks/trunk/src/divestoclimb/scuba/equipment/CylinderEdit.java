package divestoclimb.scuba.equipment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.prefs.SyncedPrefsHelper;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;
import divestoclimb.scuba.equipment.widget.MonthYearDatePicker;
import divestoclimb.scuba.equipment.widget.MonthYearDatePickerDialog;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CylinderEdit extends Activity implements View.OnClickListener,
		MonthYearDatePickerDialog.OnDateSetListener {

	public static final String KEY_CYLINDER = "cylinder";
	
	private SharedPreferences mSettings;
	private CylinderORMapper mORMapper;
	private Cylinder mCylinder;
	
	private DateFormat mTestDateFormat;
	
	private Button mLastHydro, mLastViz;
	private EditText mName, mSerial;
	
	private static final int DIALOG_HYDRO = 0, DIALOG_VISUAL = 1;
	private int mLastDialog;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		final Units units;
		Bundle params = icicle != null? icicle: getIntent().getExtras();
		if(params.containsKey(KEY_CYLINDER)) {
			mCylinder = (Cylinder)params.getSerializable(KEY_CYLINDER);
			units = mCylinder.getUnits();
			mORMapper = new CylinderORMapper(this, units);
		} else {
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
			units = new Units(unit);

			mORMapper = new CylinderORMapper(this, units);
			mCylinder = mORMapper.fetchCylinder(params.getLong(CylinderORMapper._ID));
		}
		if(mCylinder.getType() == Cylinder.TYPE_GENERIC) {
			// Clone the cylinder to copy it from generic to specific
			mCylinder = new Cylinder(mCylinder.getUnits(), mCylinder.getInternalVolume(), mCylinder.getServicePressure());
			mCylinder.setType(Cylinder.TYPE_SPECIFIC);
		}

		setContentView(R.layout.cylinder_edit);
		
		mName = (EditText)findViewById(R.id.name);
		mSerial = (EditText)findViewById(R.id.serial);
		
		mLastHydro = (Button)findViewById(R.id.lastHydro);
		mLastViz = (Button)findViewById(R.id.lastViz);
		
		final Button testIntervals = (Button)findViewById(R.id.testIntervals),
				capacityPressure = (Button)findViewById(R.id.capacityPressure),
				delete = (Button)findViewById(R.id.button_delete);
		if(mORMapper.isPhantom(mCylinder)) {
			capacityPressure.setVisibility(View.GONE);
			delete.setVisibility(View.GONE);
			setTitle(R.string.create_cylinder);
		} else {
			setTitle(R.string.edit_cylinder);
		}
		if(! mSettings.getBoolean("override_test_intervals", false)) {
			testIntervals.setVisibility(View.GONE);
		}
		capacityPressure.setOnClickListener(this);
		findViewById(R.id.button_ok).setOnClickListener(this);
		findViewById(R.id.button_cancel).setOnClickListener(this);
		testIntervals.setOnClickListener(this);
		delete.setOnClickListener(this);
		mLastHydro.setOnClickListener(this);
		mLastViz.setOnClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mTestDateFormat = new SimpleDateFormat(mSettings.getString("dateFormat", "MM/yy"));
		mName.setText(mCylinder.getName());
		mSerial.setText(mCylinder.getSerialNumber());
		updateTestDates();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mCylinder.setName(mName.getText().toString());
		mCylinder.setSerialNumber(mSerial.getText().toString());
		outState.putSerializable(KEY_CYLINDER, mCylinder);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		mLastDialog = id;
		final Date d;
		switch(id) {
		case DIALOG_HYDRO:
			d = mCylinder.getLastHydro();
			break;
		case DIALOG_VISUAL:
			d = mCylinder.getLastVisual();
			break;
		default:
			return null;
		}
		final Calendar cal = Calendar.getInstance();
		if(d != null) {
			cal.setTime(d);
		}
		return new MonthYearDatePickerDialog(this, this,
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
	}
	
	private void updateTestDates() {
		mLastHydro.setText(String.format(getString(R.string.last_hydro),
				mCylinder.getLastHydro() == null? "??": mTestDateFormat.format(mCylinder.getLastHydro())));
		mLastViz.setText(String.format(getString(R.string.last_viz),
				mCylinder.getLastVisual() == null? "??": mTestDateFormat.format(mCylinder.getLastVisual())));
	}
	
	@Override
	public void onDateSet(MonthYearDatePicker view, int year, int monthOfYear) {
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, monthOfYear);
		final Date d = cal.getTime();
		switch(mLastDialog) {
		case DIALOG_HYDRO:
			mCylinder.setLastHydro(d);
			break;
		case DIALOG_VISUAL:
			mCylinder.setLastVisual(d);
		}
		updateTestDates();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_cancel:
			finish();
			break;
		case R.id.lastHydro:
			showDialog(DIALOG_HYDRO);
			break;
		case R.id.lastViz:
			showDialog(DIALOG_VISUAL);
			break;
		case R.id.capacityPressure:
			Intent i = new Intent(this, CylinderSizeEdit.class);
			i.putExtra(CylinderSizeEdit.KEY_ACTION, CylinderSizeEdit.ACTION_EDIT);
			i.putExtra(CylinderORMapper._ID, mCylinder.getId());
			startActivity(i);
			break;
		case R.id.button_delete:
			if(! mORMapper.delete(mCylinder)) {
				Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show();
			} else {
				finish();
			}
			break;
		case R.id.testIntervals:
			// TODO
			break;
		case R.id.button_ok:
			mCylinder.setName(mName.getText().toString());
			mCylinder.setSerialNumber(mSerial.getText().toString());
			if(! mORMapper.save(mCylinder)) {
				Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
			} else {
				finish();
			}
		}
	}
}