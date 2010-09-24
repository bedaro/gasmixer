package divestoclimb.gasmixer;

import java.text.NumberFormat;

import divestoclimb.gasmixer.prefs.SyncedPrefsHelper;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.GasSupply;
import divestoclimb.lib.scuba.Localizer;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;

import Jama.Matrix;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Compute and display the results of a blending operation based on the current
 * state
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
// We begin by converting the initial and desired pressures to
// volumes. If using real gases, this is based on the mixes of each.
// Then, we use linear algebra to compute the amounts of gases needed
// to blend the desired mix.
// The matrix equation we need to solve looks like this:
// [ 1	0	fo2,t ] [ vo2,a ]   [ vf*fo2,f-vi*fo2,i ]
// [ 0	1	fhe,t ] [ vhe,a ] = [ vf*fhe,f-vi*fhe,i ]
// [ 0	0	vn2,t ] [ vt,a  ]   [ vf*fn2,f-vi*fn2,i ]
// Where:
// - vi = initial volume of gas in cylinder
// - vf = desired volume
// - fo2,t = fraction of O2 in top-up gas
// - fhe,t = fraction of He in top-up gas
// - fo2,f = desired fraction of O2
// - fhe,f = desired fraction of He
// - fo2,i = starting fraction of O2
// - fhe,i = starting fraction of He
// And the unknowns:
// - vo2,a = volume of O2 to add
// - vhe,a = volume of He to add
// - vt,a = volume of top-up gas to add
//
// Once we have the volumes, we can convert these back to pressures.
// Logic for that is in GasSupply.
//
// One invalid solution to the above is if any of the unknowns come
// out negative. If this happens, we have to set that unknown to 0
// and solve for vi instead, and the difference between the solved
// value and the given one is the amount the blender will have to
// drain. Here's an example when setting vo2 to 0:
// [ fo2,i	0	fo2,t ] [ vi    ]   [ vf*fo2,f ]
// [ fhe,i	1	fhe,t ] [ vhe,a ] = [ vf*fhe,f ]
// [ fn2,i	0	fn2,t ] [ vt,a  ]   [ vf*fn2,f ]
public class BlendResult extends Activity implements AdapterView.OnItemSelectedListener,
		View.OnClickListener {

	// Our known parameters
	private float pi, pf, t;
	private Mix mStart, mDesired, mTopup;
	// The parameters we are solving for
	private double vi, vo2a, vhea, vta;
	private GasSupply have, want;
	private SharedPreferences mSettings, mState;
	private Units mUnits;
	private NumberFormat mPressureFormat;
	private String mPressureUnit;

	private int mBlendMode;
	private static final int BLEND_MODE_PARTIAL_PRESSURE = 0;
	private static final int BLEND_MODE_CONTINUOUS_NITROX = 1;
	private static final int BLEND_MODE_CONTINUOUS_TRIMIX = 2;

	private boolean mSolutionFound;
	private String mResultText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blend_result);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);
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

		mBlendMode = mSettings.getInt("blend_mode", 0);
		mPressureFormat = Params.getPressureFormat(mUnits);
		mPressureUnit = getString(mUnits.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric);

		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));

		Spinner mode = (Spinner)findViewById(R.id.mode);
		mode.setSelection(mBlendMode);
		mode.setOnItemSelectedListener(this);

		// set button listeners
		findViewById(R.id.button_copy).setOnClickListener(this);
		findViewById(R.id.button_close).setOnClickListener(this);

		recalculate();

		// TODO: do I have enough gas button
	}

	/**
	 * Reads the current values from the state, performs the blending
	 * calculations necessary and outputs the result.
	 */
	protected void recalculate() {
		pi = mState.getFloat("start_pres", 0);
		pf = mState.getFloat("desired_pres", 0);
		t = mUnits.convertAbsTemp(mSettings.getFloat("temperature", 294), Units.METRIC);

		// Make Mixes of our gases
		mStart = new Mix(mState.getFloat("start_o2", 0.21f), mState.getFloat("start_he", 0));
		mDesired = new Mix(mState.getFloat("desired_o2", 0.21f), mState.getFloat("desired_he", 0f));
		mTopup = TrimixPreference.stringToMix(mSettings.getString("topup_gas", "0.21 0"));

		boolean real = mSettings.getBoolean("vdw", false);
		Cylinder c;
		CylinderORMapper com = null;
		if(real) {
			com = new CylinderORMapper(this, mUnits);
			c = com.fetchCylinder(mState.getLong("cylinderid", -1)); 
		} else {
			c = new Cylinder(mUnits, mUnits.volumeNormalTank(), (int)mUnits.pressureTankFull());
		}

		have = new GasSupply(c, mStart, (int)pi, ! real, t);
		want = new GasSupply(c, mDesired, (int)pf, ! real, t);

		// Now we're ready. Solve. Solution will be stored in our class
		// variables
		mSolutionFound = solve();

		showResult();
	}

	/**
	 * Reads the volume-based solution stored in member variables and
	 * outputs a step-by-step procedure for how to perform the blending
	 * using the current blend mode.
	 */
	private void showResult() {
		if(! mSolutionFound) {
			mResultText = getString(R.string.result_impossible);
		} else {
			mResultText = String.format(getString(R.string.start_with),
					(pi == 0)? getString(R.string.empty_tank)
					: String.format(getString(R.string.gas_amount),
							mPressureFormat.format(pi),
							mPressureUnit,
							mStart.toString()
					)
			) + "\n";
			switch(mBlendMode) {
			case BLEND_MODE_CONTINUOUS_NITROX:
				mResultText += getContinuousNxSteps();
				break;
			case BLEND_MODE_CONTINUOUS_TRIMIX:
				mResultText += getContinuousTmxSteps();
				break;
			case BLEND_MODE_PARTIAL_PRESSURE:
			default:
				mResultText += getPPSteps();
				break;
			}
		}

		TextView resultView = (TextView) findViewById(R.id.blend_result);
		resultView.setText(mResultText);
		if(mSolutionFound) {
			((TextView) findViewById(R.id.reminder1)).setText(getString(R.string.analyze_warning));
		}
	}

	/**
	 * Computes a volume-based solution to the given gas blending problem.
	 * Reads mStart, mTopup, have, and want.
	 * Stores a result in vi, vo2a, vhea, and vta.
	 * @return true if a solution was found, false if the problem is
	 * impossible to solve 
	 */
	private boolean solve() {
		// Object property caching for performance 
		double fo2i = mStart.getfO2(), fhei = mStart.getfHe(),
			fo2t = mTopup.getfO2(), fhet = mTopup.getfHe(),
			vo2i = have.getO2Amount(), vn2i = have.getN2Amount(),
			vhei = have.getHeAmount(), vo2f = want.getO2Amount(),
			vn2f = want.getN2Amount(), vhef = want.getHeAmount();

		vi = have.getGasAmount();
		double a[][] = { {1, 0, fo2t}, {0, 1, fhet}, {0, 0, 1 - fo2t - fhet} };
		double b[][] = { {vo2f - vo2i}, {vhef - vhei}, {vn2f - vn2i} };
		Matrix aM = new Matrix(a), bM = new Matrix(b);
		Matrix gases = aM.solve(bM);
		vo2a = gases.get(0,0);
		vhea = gases.get(1,0);
		vta = gases.get(2,0);
		// Now handle the conditions where a negative volume was found
		if(vo2a < 0) {
			vo2a = 0;
			a = aM.getArrayCopy();
			a[0][0] = fo2i;
			a[1][0] = fhei;
			a[2][0] = 1 - fo2i - fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake2 = new Matrix(a), bTake2 = new Matrix(b);
			try {
				Matrix take2 = aTake2.solve(bTake2);
				vi = take2.get(0, 0);
				vhea = take2.get(1, 0);
				vta = take2.get(2, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(vhea < 0) {
			vhea = 0;
			a = aM.getArrayCopy();
			a[0][1] = fo2i;
			a[1][1] = fhei;
			a[2][1] = 1 - fo2i - fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake3 = new Matrix(a), bTake3 = new Matrix(b);
			try {
				Matrix take3 = aTake3.solve(bTake3);
				vi = take3.get(1, 0);
				vo2a = take3.get(0, 0);
				vta = take3.get(2, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(vta < 0) {
			vta = 0;
			a = aM.getArrayCopy();
			a[0][2] = fo2i;
			a[1][2] = fhei;
			a[2][2] = 1 - fo2i - fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake4 = new Matrix(a), bTake4 = new Matrix(b);
			try {
				Matrix take4 = aTake4.solve(bTake4);
				vi = take4.get(2, 0);
				vo2a = take4.get(0, 0);
				vhea = take4.get(1, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		// The final checks ensure that vi is within a realistic range.
		// The blender can't drain to a negative volume, and we can't
		// start with any more gas than is already in the cylinder.
		return vi >= 0 && vi <= have.getGasAmount();
	}

	/**
	 * Performs a partial pressure blending and builds the readable
	 * explanation for how to replicate it.
	 * @return A descriptive version of the blend that was done
	 */
	private String getPPSteps() {
		float po2, phe, pt, pdrain;
		final NumberFormat nf = mPressureFormat;
		final String presUnit = mPressureUnit;
		// Convert the volumes built by solve() into pressures by doing the blending
		// operation on have.
		GasSupply blend = have.clone();

		pdrain = (float)blend.drainToGasAmount(vi).getPressure();
		float pretop;
		if(mSettings.getBoolean("he_first", false)) {
			phe = vhea > 0? (float)blend.addHe(vhea).getPressure(): pdrain;
			po2 = vo2a > 0? (float)blend.addO2(vo2a).getPressure(): phe;
			pretop = po2;
		} else {
			po2 = vo2a > 0? (float)blend.addO2(vo2a).getPressure(): pdrain;
			phe = vhea > 0? (float)blend.addHe(vhea).getPressure(): po2;
			pretop = phe;
		}
		pt = vta > 0? (float)blend.addGas(mTopup, vta).getPressure(): pretop;

		String result = "";

		if(pi - pdrain >= Math.pow(10, mUnits.pressurePrecision() * -1) * 0.5) {
			// Drain
			result += "- "+String.format(getString(R.string.result_drain),
					nf.format(pdrain),
					presUnit
			) + "\n";
		}
		if(mSettings.getBoolean("he_first", false)) {
			if(Math.round(phe) > Math.round(pdrain)) {
				result += "- " + String.format(getString(R.string.result_fillto),
						nf.format(phe),
						presUnit,
						getString(R.string.helium)
				) + "\n";
			}
			if(Math.round(po2) > Math.round(phe)) {
				result+= "- " + String.format(getString(R.string.result_fillto),
						nf.format(po2),
						presUnit,
						getString(R.string.oxygen)
				) + "\n";
			}
		} else {
			if(Math.round(po2) > Math.round(pdrain)) {
				result += "- " + String.format(getString(R.string.result_fillto),
						nf.format(po2),
						presUnit,
						getString(R.string.oxygen)
				) + "\n";
			}
			if(Math.round(phe) > Math.round(po2)) {
				result += "- " + String.format(getString(R.string.result_fillto),
						nf.format(phe),
						presUnit,
						getString(R.string.helium)
				) + "\n";
			}
		}
		if(Math.round(pt) > Math.round(pretop)) {
			result += "- " + String.format(getString(R.string.result_fillto),
					nf.format(pt),
					presUnit,
					mTopup.toString()
			) + "\n";
		}
		result += String.format(getString(R.string.result_end),
				String.format(getString(R.string.gas_amount),
						nf.format(pf),
						presUnit,
						mDesired.toString()
				)
		);
		return result;
	}

	private String getContinuousNxSteps() {
		float pnx, phe, pdrain;
		final NumberFormat nf = mPressureFormat;
		final String presUnit = mPressureUnit;
		// Take vo2a and vta and combine them into a single Mix.
		final double vca = vo2a + vta;
		final Mix continuousAdd = new Mix((vo2a + vta * mTopup.getfO2()) / vca, 0);

		// Now do the blend
		GasSupply blend = have.clone();
		pdrain = (float)blend.drainToGasAmount(vi).getPressure();
		// Always add helium first. Although we could use the he_first setting to decide,
		// it's unlikely anyone would want to top up with helium last.
		phe = vhea > 0? (float)blend.addHe(vhea).getPressure(): pdrain;
		pnx = vca > 0? (float)blend.addGas(continuousAdd, vca).getPressure(): phe;
		String result = "";

		if(pi - pdrain >= Math.pow(10, mUnits.pressurePrecision() * -1) * 0.5) {
			// Drain
			result += "- " + String.format(getString(R.string.result_drain),
					nf.format(pdrain),
					presUnit
			) + "\n";
		}
		if(Math.round(phe) > Math.round(pdrain)) {
			result += "- " + String.format(getString(R.string.result_fillto),
					nf.format(phe),
					presUnit,
					getString(R.string.helium)
			)+"\n";
		}
		if(Math.round(pnx) > Math.round(phe)) {
			result+= "- " + String.format(getString(R.string.result_fillto),
					nf.format(pnx),
					presUnit,
					continuousAdd.toString()
			) + "\n";
		}
		result += String.format(getString(R.string.result_end),
				String.format(getString(R.string.gas_amount),
						nf.format(pf),
						presUnit,
						mDesired.toString()
				)
		);
		return result;
	}

	private String getContinuousTmxSteps() {
		float ptmx, pdrain;
		final NumberFormat nf = mPressureFormat;
		final String presUnit = mPressureUnit;
		// Take vo2a and vta and combine them into a single Mix.
		final double vca = vo2a + vhea + vta;
		final Mix continuousAdd = new Mix((vo2a + vta * mTopup.getfO2()) / vca, (vhea + vta * mTopup.getfHe()) / vca);

		// Now do the blend
		GasSupply blend = have.clone();
		pdrain = (float)blend.drainToGasAmount(vi).getPressure();
		ptmx = vca > 0? (float)blend.addGas(continuousAdd, vca).getPressure(): pdrain;
		String result = "";

		if(pi - pdrain >= Math.pow(10, mUnits.pressurePrecision() * -1) * 0.5) {
			// Drain
			result += "- " + String.format(getString(R.string.result_drain),
					nf.format(pdrain),
					presUnit
			) + "\n";
		}
		if(Math.round(ptmx) > Math.round(pdrain)) {
			result += "- " + String.format(getString(R.string.result_fillto),
					nf.format(ptmx),
					presUnit,
					continuousAdd.toString()
			)+"\n";
		}
		result += String.format(getString(R.string.result_end),
				String.format(getString(R.string.gas_amount),
						nf.format(pf),
						presUnit,
						mDesired.toString()
				)
		);
		return result;
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		mSettings.edit().putInt("blend_mode", position).commit();
		mBlendMode = position;
		showResult();
	}

	public void onNothingSelected(AdapterView<?> parent) { }

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.button_copy:
			ClipboardManager c = (ClipboardManager)BlendResult.this.getSystemService(CLIPBOARD_SERVICE);
			c.setText(mResultText);
			break;
		case R.id.button_close:
			finish();
		}
	}
}