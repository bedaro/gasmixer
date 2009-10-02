package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SetStarting extends Activity {
	
	private NumberSelector mPressureSelector;
	private TextView mPressureUnit;
	private TrimixSelector mGasSelector;
	private Button mButton;
	private SharedPreferences mSettings, mState;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_start);
		setTitle(R.string.starting);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);
		int unit;
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		Units.change(unit);
		
		mPressureSelector = (NumberSelector)findViewById(R.id.pressure);
		mPressureUnit = (TextView)findViewById(R.id.pressure_unit);
		mGasSelector = (TrimixSelector)findViewById(R.id.mix);

		mButton = (Button)findViewById(R.id.button);
		mButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
				SharedPreferences.Editor editor = mState.edit();

				editor.putFloat("start_pres", mPressureSelector.getValue());
				Mix m = mGasSelector.getMix();
				editor.putFloat("start_o2", m.getfO2());
				editor.putFloat("start_he", m.getfHe());
				editor.commit();

				setResult(RESULT_OK);
				finish();
			}
			
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mPressureSelector.setDecimalPlaces(0);
		mPressureSelector.setLimits(0f, new Float(Units.pressureTankMax()));
		mPressureSelector.setIncrement(new Float(Units.pressureIncrement()));
		mPressureSelector.setValue(mState.getFloat("start_pres", 0));
		mPressureUnit.setText(Params.pressure(this)+":");
		mGasSelector.setMix(new Mix(mState.getFloat("start_o2", 0.21f), mState.getFloat("start_he", 0)));
	}
}
