package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import divestoclimb.gasmixer.prefs.SyncedPrefsHelper;
import divestoclimb.gasmixer.widget.TrimixPreference;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.GasSupply;
import divestoclimb.lib.scuba.Localizer;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;

import Jama.Matrix;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
public class BlendResult extends ListActivity implements AdapterView.OnItemSelectedListener,
		View.OnClickListener {

	// Our known parameters
	private float pi, pf, t;
	private Mix mStart, mDesired, mTopup, mRich;
	// The parameters we are solving for
	private double vi, vo2a, vhea, vta;
	// The starting pressure for the blend, accounting for draining
	private float mStartPressure;
	// have is the GasSupply the user entered.
	// want is the GasSupply the user desires at the end.
	private GasSupply have, want;
	// This keeps track of whether or not an actual cylinder size is
	// being used, or if it's being simulated (which can happen in ideal
	// blending mode)
	private boolean isCylinderReal;
	private SharedPreferences mSettings, mState;
	private Units mUnits;
	private NumberFormat mPressureFormat, mCapacityFormat;
	private String mPressureUnit;
	private CharSequence mCapacityUnit;

	private View mResultFooterView, mStartView, mImpossibleView, mCopyButton;
	private TextView mStartPressureView, mStartMixView, mResultView;

	private int mBlendMode;
	private static final int BLEND_MODE_PARTIAL_PRESSURE = 0;
	private static final int BLEND_MODE_CONTINUOUS_NITROX = 1;
	private static final int BLEND_MODE_CONTINUOUS_TRIMIX = 2;

	private boolean mSolutionFound;

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
		mCapacityFormat = Params.getCapacityFormat(mUnits);
		mPressureUnit = getString(mUnits.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric);
		mCapacityUnit = getText(mUnits.capacityUnit() == Units.IMPERIAL? R.string.capacity_imperial: R.string.capacity_metric);

		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));

		Spinner mode = (Spinner)findViewById(R.id.mode);
		mode.setSelection(mBlendMode);
		mode.setOnItemSelectedListener(this);

		// Inflate the header and footer views for the list
		LayoutInflater li = getLayoutInflater();
		mStartView = li.inflate(R.layout.blend_result_line, null);
		mStartPressureView = (TextView)mStartView.findViewById(R.id.pressure);
		mStartMixView = (TextView)mStartView.findViewById(R.id.gas);
		mStartMixView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
		mResultFooterView = li.inflate(R.layout.blend_result_footer, null);
		mResultView = (TextView)mResultFooterView.findViewById(R.id.final_result);
		mImpossibleView = li.inflate(R.layout.blend_result_impossible, null);

		// set button listeners
		mCopyButton = findViewById(R.id.button_copy);
		mCopyButton.setOnClickListener(this);
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
		mRich = new Mix(mSettings.getFloat("rich_gas", 100) / 100, 0);
		if(mTopup == null) {
			// Not sure how this happens, but to someone it did
			mTopup = new Mix(0.21f, 0);
			Toast.makeText(this, R.string.topup_read_error, Toast.LENGTH_LONG);
		}

		boolean real = mSettings.getBoolean("vdw", false);
		// TODO resolve the content provider and fetch cylinder even if
		// ideal, for showing volumes
		Cylinder c = null;
		CylinderORMapper com = null;
		if(real) {
			com = new CylinderORMapper(this, mUnits);
			c = com.fetchCylinder(mState.getLong("cylinderid", -1));
			isCylinderReal = true;
		}
		if(! real || c == null) {
			c = new Cylinder(mUnits, mUnits.volumeNormalTank(), (int)mUnits.pressureTankFull());
			isCylinderReal = false;
		}

		have = new GasSupply(c, mStart, (int)pi, ! real, t);
		want = new GasSupply(c, mDesired, (int)pf, ! real, t);

		// Now we're ready. Solve. Solution will be stored in our class
		// variables
		mSolutionFound = solve();

		showResult();
	}
	
	private class BlendStep {
		public Float pressure;
		public int volume;
		public Mix mix;

		public BlendStep(Float pressure, int volume, Mix mix) {
			this.pressure = pressure;
			this.volume = volume;
			this.mix = mix;
		}
		
		public CharSequence toCharSequence() {
			SpannableStringBuilder builder;
			if(pressure != null) {
				builder = new SpannableStringBuilder(String.format(getString(R.string.result_fillto),
						mPressureFormat.format(pressure),
						mPressureUnit,
						mix.toString()));
					if(isCylinderReal) {
						builder.append(" (" + mCapacityFormat.format(volume) + " ")
							.append(mCapacityUnit)
							.append(")");
					}
			} else {
				builder = new SpannableStringBuilder(isCylinderReal? "(" + mCapacityFormat.format(volume) + " ": "")
					.append(mCapacityUnit)
					.append(" " + mix.toString() + (isCylinderReal? ")": ""));
			}
			
			return builder;
		}
	}
	
	public class BlendStepAdapter extends ArrayAdapter<BlendStep> {

		public BlendStepAdapter(Context context, int textViewResourceId,
				List<BlendStep> objects) {
			super(context, textViewResourceId);
			add(null);
			for(BlendStep s : objects) {
				add(s);
			}
			if(mSolutionFound) {
				add(null);
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(position == 0) {
				return mSolutionFound? mStartView: mImpossibleView;
			} else if(position == getCount() - 1 && mSolutionFound) {
				return mResultFooterView;
			}
			View row = getLayoutInflater().inflate(R.layout.blend_result_line, null);

			BlendStep data = getItem(position);
			TextView pressure = (TextView)row.findViewById(R.id.pressure);
			pressure.setText(data.pressure != null? mPressureFormat.format(data.pressure) + " " + mPressureUnit: "");

			TextView volume = null;
			if(isCylinderReal) {
				volume = (TextView)row.findViewById(R.id.volume);
				// Imperial capacity units are styled, so we must treat the unit as a CharSequence
				volume.setText("+" + mCapacityFormat.format(data.volume) + " ");
				volume.append(mCapacityUnit);
			}

			TextView gas = (TextView)row.findViewById(R.id.gas);
			gas.setText(data.mix.toString());

			if(data.pressure == null) {
				// This is not a discrete operation. Make it italic.
				if(volume != null) {
					volume.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
				}
				gas.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
			}

			// TODO change color of row based on mix type?
			return row;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return mSolutionFound;
		}
	}

	/**
	 * Reads the volume-based solution stored in member variables and
	 * outputs a step-by-step procedure for how to perform the blending
	 * using the current blend mode.
	 */
	private void showResult() {
		List<BlendStep> steps;
		// Disable the "Copy This" button if there's no solution to copy
		mCopyButton.setEnabled(mSolutionFound);
		if(mSolutionFound) {
			GasSupply start = have.clone();

			if(mStartPressure < pi) {
				// Needed to drain some gas
				mStartPressureView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC), Typeface.BOLD_ITALIC);
			} else {
				mStartPressureView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
			}
			if(mStartPressure > 0) {
				mStartMixView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
				mStartMixView.setText(mStart.toString());
			}

			mStartPressureView.setText(mPressureFormat.format(mStartPressure) + " " + mPressureUnit);
			
			mResultView.setText(mDesired.toString());

			switch(mBlendMode) {
			case BLEND_MODE_CONTINUOUS_NITROX:
				steps = getContinuousNxSteps(start);
				break;
			case BLEND_MODE_CONTINUOUS_TRIMIX:
				steps = getContinuousTmxSteps(start);
				break;
			case BLEND_MODE_PARTIAL_PRESSURE:
			default:
				steps = getPPSteps(start);
				break;
			}
		} else {
			steps = new ArrayList<BlendStep>();
		}
		setListAdapter(new BlendStepAdapter(this, R.layout.blend_result_line, steps));
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
			fo2r = mRich.getfO2(),
			vo2i = have.getO2Amount(), vn2i = have.getN2Amount(),
			vhei = have.getHeAmount(), vo2f = want.getO2Amount(),
			vn2f = want.getN2Amount(), vhef = want.getHeAmount();

		vi = have.getGasAmount();
		double a[][] = { {fo2r, 0, fo2t}, {0, 1, fhet}, {1 - fo2r, 0, 1 - fo2t - fhet} };
		double b[][] = { {vo2f - vo2i}, {vhef - vhei}, {vn2f - vn2i} };
		Matrix aM = new Matrix(a), bM = new Matrix(b);
		try {
			Matrix gases = aM.solve(bM);
			vo2a = gases.get(0,0);
			vhea = gases.get(1,0);
			vta = gases.get(2,0);
		} catch(RuntimeException e) {
			// Can happen if the topup gas is heliox or helium and nitrogen
			// needs to be added
			return false;
		}
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
		if(vi < 0 || vi > have.getGasAmount()) {
			return false;
		}
		float pdrain = (float)have.drainToGasAmount(vi).getPressure();
		mStartPressure = pi;
		if(pi - pdrain >= Math.pow(10, mUnits.pressurePrecision() * -1) * 0.5) {
			// Need to drain the gas
			mStartPressure = pdrain;
		}
		return true;
	}

	/**
	 * Performs a partial pressure blending and builds the readable
	 * explanation for how to replicate it.
	 * @return A descriptive version of the blend that was done
	 */
	private List<BlendStep> getPPSteps(GasSupply supply) {
		float po2, phe, pt, pdrain = (float)supply.getPressure();
		
		float pretop;
		List<BlendStep> steps = new ArrayList<BlendStep>(4);
		if(mSettings.getBoolean("he_first", false)) {
			phe = vhea > 0? (float)supply.addHe(vhea).getPressure(): pdrain;
			if(Math.round(phe) > Math.round(pdrain)) {
				steps.add(new BlendStep(phe, (int)Math.round(vhea), new Mix(0, 1)));
			}
			po2 = vo2a > 0? (float)supply.addO2(vo2a).getPressure(): phe;
			if(Math.round(po2) > Math.round(phe)) {
				steps.add(new BlendStep(po2, (int)Math.round(vo2a), mRich));
			}
			pretop = po2;
		} else {
			po2 = vo2a > 0? (float)supply.addO2(vo2a).getPressure(): pdrain;
			if(Math.round(po2) > Math.round(pdrain)) {
				steps.add(new BlendStep(po2, (int)Math.round(vo2a), mRich));
			}
			phe = vhea > 0? (float)supply.addHe(vhea).getPressure(): po2;
			if(Math.round(phe) > Math.round(po2)) {
				steps.add(new BlendStep(phe, (int)Math.round(vhea), new Mix(0, 1)));
			}
			pretop = phe;
		}
		pt = vta > 0? (float)supply.addGas(mTopup, vta).getPressure(): pretop;
		if(Math.round(pt) > Math.round(pretop)) {
			steps.add(new BlendStep(pt, (int)Math.round(vta), mTopup));
		}
		return steps;
	}

	private List<BlendStep> getContinuousNxSteps(GasSupply supply) {
		float pnx, phe, pdrain = (float)supply.getPressure();
		// Take vo2a and vta and combine them into a single Mix.
		final double vca = vo2a + vta;
		final Mix continuousAdd = new Mix((vo2a + vta * mTopup.getfO2()) / vca, 0);

		// Now do the blend
		List<BlendStep> steps = new ArrayList<BlendStep>(4);

		// Always add helium first. Although we could use the he_first setting to decide,
		// it's unlikely anyone would want to top up with helium last.
		phe = vhea > 0? (float)supply.addHe(vhea).getPressure(): pdrain;
		pnx = vca > 0? (float)supply.addGas(continuousAdd, vca).getPressure(): phe;

		if(Math.round(phe) > Math.round(pdrain)) {
			steps.add(new BlendStep(phe, (int)Math.round(vhea), new Mix(0, 1)));
		}
		if(Math.round(pnx) > Math.round(phe)) {
			if(vo2a > 0) {
				steps.add(new BlendStep(null, (int)Math.round(vo2a), new Mix(1, 0)));
			}
			if(vta > 0) {
				steps.add(new BlendStep(null, (int)Math.round(vta), mTopup));
			}
			steps.add(new BlendStep(pnx, (int)Math.round(vca), continuousAdd));
		}
		return steps;
	}

	private List<BlendStep> getContinuousTmxSteps(GasSupply supply) {
		float ptmx, pdrain = (float)supply.getPressure();
		// Take vo2a and vta and combine them into a single Mix.
		final double vca = vo2a + vhea + vta;
		final Mix continuousAdd = new Mix((vo2a + vta * mTopup.getfO2()) / vca, (vhea + vta * mTopup.getfHe()) / vca);

		// Now do the blend
		List<BlendStep> steps = new ArrayList<BlendStep>(4);

		ptmx = vca > 0? (float)supply.addGas(continuousAdd, vca).getPressure(): pdrain;

		if(Math.round(ptmx) > Math.round(pdrain)) {
			if(vo2a > 0) {
				steps.add(new BlendStep(null, (int)Math.round(vo2a), new Mix(1, 0)));
			}
			if(vhea > 0) {
				steps.add(new BlendStep(null, (int)Math.round(vhea), new Mix(0, 1)));
			}
			if(vta > 0) {
				steps.add(new BlendStep(null, (int)Math.round(vta), mTopup));
			}
			steps.add(new BlendStep(ptmx, (int)Math.round(vca), continuousAdd));
		}
		return steps;
	}
	
	// ItemSelected listener for the blend mode spinner
	
	// Workaround for Android bug that causes onItemSelected to fire during layout
	private boolean firstSelection = true;

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if(firstSelection) {
			firstSelection = false;
			return;
		}
		mSettings.edit().putInt("blend_mode", position).commit();
		mBlendMode = position;
		showResult();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) { }

	// Get the CharSequence representation of the given step
	private CharSequence getStep(int step) {
		NumberFormat nf = mPressureFormat;
		String presUnit = mPressureUnit;
		if(step == 0) {
			return String.format(getString(R.string.start_with),
					mStartPressure > 0? String.format(getString(R.string.gas_amount),
							nf.format(mStartPressure),
							presUnit,
							mStart.toString())
					: getString(R.string.empty_tank));
		} else if(getListView().getCount() - 1 == step) {
			return String.format(getString(R.string.result_end), String.format(getString(R.string.gas_amount),
							nf.format(pf),
							presUnit,
					mDesired.toString()));
		}
		return ((BlendStep)getListView().getItemAtPosition(step)).toCharSequence();
	}

	private Toast mActiveMessage = null;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if(mActiveMessage != null) {
			mActiveMessage.cancel();
		}
		mActiveMessage = Toast.makeText(this, getStep(position), Toast.LENGTH_LONG);
		mActiveMessage.show();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_copy:
			SpannableStringBuilder b = new SpannableStringBuilder();
			for(int i = 0; i < getListView().getCount(); i ++) {
				b.append("- ")
					.append(getStep(i))
					.append("\n");
			}
			ClipboardManager c = (ClipboardManager)BlendResult.this.getSystemService(CLIPBOARD_SERVICE);
			c.setText(b);
			break;
		case R.id.button_close:
			finish();
		}
	}
}