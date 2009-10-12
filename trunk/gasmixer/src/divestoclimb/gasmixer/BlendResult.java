package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.GasSupply;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.CylinderSizeClient;

import Jama.Matrix;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Compute and display the results of a blending operation based on the current
 * state
 * @author Ben Roberts (divestoclimb@gmail.com)
 *
 */
// We begin by converting the initial and desired pressures to
// volumes. If using real gases, this is based on the mixes of each.
// Then, we use linear algebra to compute the amounts of gases needed
// to blend the desired mix.
// The matrix equation we need to solve looks like this:
// [ 1	0	fo2,t ] [ vo2 ]   [ vf*fo2,f-vi*fo2,i ]
// [ 0	1	fhe,t ] [ vhe ] = [ vf*fhe,f-vi*fhe,i ]
// [ 0	0	vn2,t ] [ vt  ]   [ vf*fn2,f-vi*fn2,i ]
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
// - vo2 = volume of O2 to add
// - vhe = volume of He to add
// - vt = volume of top-up gas to add
//
// Once we have the volumes, we can convert these back to pressures.
// Logic for that is in GasSupply.
//
// One invalid solution to the above is if any of the unknowns come
// out negative. If this happens, we have to set that unknown to 0
// and solve for vi instead, and the difference between the solved
// value and the given one is the amount the blender will have to
// drain. Here's an example when setting vo2 to 0:
// [ fo2,i	0	fo2,t ] [ vi  ]   [ vf*fo2,f ]
// [ fhe,i	1	fhe,t ] [ vhe ] = [ vf*fhe,f ]
// [ fn2,i	0	fn2,t ] [ vt  ]   [ vf*fn2,f ]
public class BlendResult extends Activity {

	// Our known parameters
	private float pi, pf;
	private Mix mStart, mDesired, mTopup;
	// The parameters we are solving for
	private float po2, phe, pt, pdrain;
	
	private String mResultText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.blend_result);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this),
				state = getSharedPreferences(Params.STATE_NAME, 0);
		int unit;
		try {
			unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		Units units = new Units(unit);

		NumberFormat nf = Params.getPressureFormat(units);

		pi = state.getFloat("start_pres", 0);
		pf = state.getFloat("desired_pres", 0);

		// Make Mixes of our gases
		mStart=new Mix(state.getFloat("start_o2", 0.21f), state.getFloat("start_he", 0));
		mDesired=new Mix(state.getFloat("desired_o2", 0.21f), state.getFloat("desired_he", 0f));
		mTopup = TrimixPreference.stringToMix(settings.getString("topup_gas", "0.21 0"));

		// Now we're ready. Solve. Solution will be stored in our class
		// variables
		boolean solved = solve(settings, state, units);

		Resources r = getResources();
		String presUnit = Params.pressure(this, units);
		if(! solved) {
			mResultText=r.getString(R.string.result_impossible);
		} else {
			mResultText=String.format(r.getString(R.string.start_with),
					(pi == 0)? r.getString(R.string.empty_tank)
					: String.format(r.getString(R.string.gas_amount),
							nf.format(pi),
							presUnit,
							Params.mixFriendlyName(mStart, this))
			)+"\n";
			if(pi - pdrain >= Math.pow(10, units.pressurePrecision() * -1) * 0.5) {
				// Drain
				mResultText+="- "+String.format(r.getString(R.string.result_drain),
						nf.format(pdrain),
						presUnit
				)+"\n";
			}
			if(Math.round(po2) > Math.round(pdrain)) {
				mResultText+="- "+String.format(r.getString(R.string.result_fillto),
						nf.format(po2),
						presUnit,
						r.getString(R.string.oxygen)
				)+"\n";
			}
			if(Math.round(phe) > Math.round(po2)) {
				mResultText+="- "+String.format(r.getString(R.string.result_fillto),
						nf.format(phe),
						presUnit,
						r.getString(R.string.helium)
				)+"\n";
			}
			if(Math.round(pt) > Math.round(phe)) {
				mResultText+="- "+String.format(r.getString(R.string.result_fillto),
						nf.format(pt),
						presUnit,
						Params.mixFriendlyName(mTopup, this)
				)+"\n";
			}
			mResultText+=String.format(r.getString(R.string.result_end),
					String.format(r.getString(R.string.gas_amount),
							nf.format(pf),
							presUnit,
							Params.mixFriendlyName(mDesired, this)
					)
			);
		}
		
		TextView resultView = (TextView) findViewById(R.id.blend_result);
		resultView.setText(mResultText);
		if(solved) {
			TextView reminderView = (TextView) findViewById(R.id.reminder1);
			reminderView.setText(r.getString(R.string.analyze_warning));
		}
	
		// set button listeners
		Button copy = (Button) findViewById(R.id.button_copy);
		copy.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ClipboardManager c = (ClipboardManager)BlendResult.this
						.getSystemService(CLIPBOARD_SERVICE);
				c.setText(mResultText);
			}
		});
		
		Button close = (Button) findViewById(R.id.button_close);
		close.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		// TODO: do I have enough gas button
	}

	private boolean solve(SharedPreferences settings, SharedPreferences state, Units units) {
		boolean real = settings.getBoolean("vdw", false);
		Cylinder c;
		if(real) {
			Cursor cu = getContentResolver().query(Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
							String.valueOf(state.getLong("cylinderid", -1))
					), null, null, null, null);
			cu.moveToFirst();
			c = CylinderSizeClient.cursorToCylinder(cu, units);
			cu.close();
		} else {
			c = new Cylinder(units, units.volumeNormalTank(), (int)units.pressureTankFull());
		}

		GasSupply have = new GasSupply(c, mStart, (int)pi),
		want = new GasSupply(c, mDesired, (int)pf);
		
		if(! real) {
			have.useIdealGasLaws();
			want.useIdealGasLaws();
		}

		// Object property caching for performance 
		double fo2i = mStart.getfO2(), fhei = mStart.getfHe(),
			fo2t = mTopup.getfO2(), fhet = mTopup.getfHe(),
			vo2i = have.getO2Amount(), vn2i = have.getN2Amount(),
			vhei = have.getHeAmount(), vo2f = want.getO2Amount(),
			vn2f = want.getN2Amount(), vhef = want.getHeAmount();

		double start_vol = have.getGasAmount();
		double a[][] = { {1, 0, fo2t}, {0, 1, fhet}, {0, 0, 1-fo2t-fhet} };
		double b[][] = { {vo2f-vo2i}, {vhef-vhei}, {vn2f-vn2i} };
		Matrix aM=new Matrix(a), bM = new Matrix(b);
		Matrix gases=aM.solve(bM);
		double o2_added=gases.get(0,0),
				he_added = gases.get(1,0),
				topup_added = gases.get(2,0);
		// Now handle the conditions where a negative volume was found
		if(o2_added < 0) {
			o2_added = 0;
			a = aM.getArrayCopy();
			a[0][0] = fo2i;
			a[1][0] = fhei;
			a[2][0] = 1-fo2i-fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake2=new Matrix(a), bTake2 = new Matrix(b);
			try {
				Matrix take2 = aTake2.solve(bTake2);
				start_vol = take2.get(0, 0);
				he_added = take2.get(1, 0);
				topup_added = take2.get(2, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(he_added < 0) {
			he_added = 0;
			a = aM.getArrayCopy();
			a[0][1] = fo2i;
			a[1][1] = fhei;
			a[2][1] = 1-fo2i-fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake3=new Matrix(a), bTake3=new Matrix(b);
			try {
				Matrix take3 = aTake3.solve(bTake3);
				start_vol = take3.get(1, 0);
				o2_added = take3.get(0, 0);
				topup_added = take3.get(2, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(topup_added < 0) {
			topup_added = 0;
			a = aM.getArrayCopy();
			a[0][2] = fo2i;
			a[1][2] = fhei;
			a[2][2] = 1-fo2i-fhei;
			b[0][0] = vo2f;
			b[1][0] = vhef;
			b[2][0] = vn2f;
			Matrix aTake4=new Matrix(a), bTake4=new Matrix(b);
			try {
				Matrix take4 = aTake4.solve(bTake4);
				start_vol = take4.get(2, 0);
				o2_added = take4.get(0, 0);
				he_added = take4.get(1, 0);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(start_vol < 0) {
			// The only solution is to drain to a negative volume? Impossible.
			return false;
		}
		if(start_vol > have.getGasAmount()) {
			// The only solution is to start with more contents than we have? Impossible.
			return false;
		}
		// If we get here, it means we found a solution. Convert the volumes back to
		// pressures by doing the blending operation on have.
		pdrain = (float)have.drainToGasAmount(start_vol).getPressure();
		po2 = o2_added > 0? (float)have.addO2(o2_added).getPressure(): pdrain;
		phe = he_added > 0? (float)have.addHe(he_added).getPressure(): po2;
		pt = topup_added > 0? (float)have.addGas(mTopup, topup_added).getPressure(): phe;
		return true;
	}

}
