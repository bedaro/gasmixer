package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.GasSupply;
import divestoclimb.lib.scuba.Localizer;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Frontend for GasSupply's topup capabilities.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class TopupResult extends Activity implements CompoundButton.OnCheckedChangeListener,
		View.OnClickListener {

	private Mix mResult;
	private TextView mFinalMOD, mFinalEADENDLabel, mFinalEADEND;
	private ToggleButton mTogglePo2;
	
	// Cached storage of preferences
	private boolean mO2IsNarcotic;
	private float mPo2Low, mPo2High;
	
	private Units mUnits;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.topup_result);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this),
				state = getSharedPreferences(Params.STATE_NAME, 0);

		int unit;
		try {
			unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);

		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));

		// Gas computation options
		mPo2Low = settings.getFloat("max_norm_po2", 1.4f);
		mPo2High = settings.getFloat("max_hi_po2", 1.6f);
		mO2IsNarcotic = settings.getBoolean("o2_is_narcotic", true);

		Mix topup = TrimixPreference.stringToMix(settings.getString("topup_gas", "0.21 0"));

		boolean real = settings.getBoolean("vdw", false);
		Cylinder c;
		CylinderORMapper com = null;
		if(real) {
			com = new CylinderORMapper(this, mUnits);
			c = com.fetchCylinder(state.getLong("cylinderid", -1)); 
		} else {
			c = new Cylinder(mUnits, mUnits.volumeNormalTank(), (int)mUnits.pressureTankFull());
		}
		GasSupply fill = new GasSupply(c,
				new Mix(state.getFloat("topup_start_o2", 0.21f), state.getFloat("topup_start_he", 0)),
				(int)state.getFloat("topup_start_pres", 0),
				! real,
				mUnits.convertAbsTemp(settings.getFloat("temperature", 294), Units.METRIC)
		);
		mResult = fill.topup(topup, (int)state.getFloat("topup_final_pres", 0)).getMix();

		String resultText = String.format(getString(R.string.topup_result), mResult.toString());

		TextView resultView = (TextView) findViewById(R.id.result);
		resultView.setText(resultText);
		TextView reminder1View = (TextView) findViewById(R.id.reminder1);
		reminder1View.setText(String.format(getString(R.string.topup_reminder),
				topup.toString()
		));
		TextView reminder2View = (TextView) findViewById(R.id.reminder2);
		reminder2View.setText(getString(R.string.analyze_warning));

		mFinalMOD = (TextView)findViewById(R.id.mod);
		mFinalEADENDLabel = (TextView)findViewById(R.id.ead_end_label);
		mFinalEADEND = (TextView)findViewById(R.id.ead_end);
		mTogglePo2 = (ToggleButton)findViewById(R.id.button_po2_hi);

		updateModEnd();

		mTogglePo2.setOnCheckedChangeListener(this);
		findViewById(R.id.button_close).setOnClickListener(this);
	}

	private void updateModEnd() {
		final Units u = mUnits;
		final NumberFormat nf = NumberFormat.getIntegerInstance();
		float mod = mResult.MOD(u, mTogglePo2.isChecked()? mPo2High: mPo2Low);
		final String depthUnit = getString(u.depthUnit() == Units.IMPERIAL? R.string.depth_imperial: R.string.depth_metric);
		mFinalMOD.setText(nf.format(mod) + " " + depthUnit);
		if(mResult.getHe() > 0) {
			mFinalEADENDLabel.setText(getResources().getString(R.string.end));
			mFinalEADEND.setText(nf.format(mResult.END(Math.round(mod), u, mO2IsNarcotic)) + " " + depthUnit);
		} else {
			mFinalEADENDLabel.setText(getResources().getString(R.string.ead));
			mFinalEADEND.setText(nf.format(mResult.EAD(Math.round(mod), u)) + " " + depthUnit);
		}
	}
	
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		updateModEnd();
	}
	
	public void onClick(View v) {
		finish();
	}
}