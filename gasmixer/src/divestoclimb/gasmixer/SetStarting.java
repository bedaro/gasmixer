package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.CylinderSizeClient;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SetStarting extends Activity implements Button.OnClickListener {
	
	private NumberSelector mPressureSelector;
	private TextView mPressureUnit, mCylinderDescription;
	private TrimixSelector mGasSelector;
	private SharedPreferences mSettings, mState;
	private Units mUnits;

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
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);

		mPressureSelector = (NumberSelector)findViewById(R.id.pressure);
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
		mPressureSelector.setDecimalPlaces(0);
		mPressureSelector.setLimits(0f, new Float(mUnits.pressureTankMax()));
		mPressureSelector.setIncrement(new Float(mUnits.pressureIncrement()));
		mPressureSelector.setValue(mState.getFloat("start_pres", 0));
		mPressureUnit.setText(Params.pressure(this, mUnits)+":");
		mGasSelector.setMix(new Mix(mState.getFloat("start_o2", 0.21f), mState.getFloat("start_he", 0)));

		if(mCylinderDescription != null) {
			updateCylinder();
		}
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button:
			SharedPreferences.Editor editor = mState.edit();

			editor.putFloat("start_pres", mPressureSelector.getValue());
			Mix m = mGasSelector.getMix();
			editor.putFloat("start_o2", (float)m.getfO2());
			editor.putFloat("start_he", (float)m.getfHe());
			editor.commit();

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
			SharedPreferences.Editor editor = mState.edit();
			editor.putLong("cylinderid", id);
			editor.commit();
			updateCylinder();
		}
	}
	
	private void updateCylinder() {
		long id = mState.getLong("cylinderid", -1);
		if(id != -1) {
			Cursor c = getContentResolver().query(Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
					String.valueOf(id)), new String[] { CylinderSizeClient.NAME }, null, null, null);
			c.moveToFirst();
			mCylinderDescription.setText(c.getString(c.getColumnIndexOrThrow(CylinderSizeClient.NAME)));
			c.close();
		}
	}
}
