package divestoclimb.gasmixer;

import java.text.NumberFormat;

import Jama.Matrix;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

// Use linear algebra to compute the amounts of gases needed
// to blend the desired mix.
// The matrix equation we need to solve looks like this:
// [ 1	0	fo2,t			] [ po2 ]	[ pf*fo2,f-pi*fo2,i							]
// [ 0	1	fhe,t			] [ phe ] = [ pf*fhe,f-pi*fhe,i							]
// [ 0	0	100-fo2,t-fhe,t ] [ pt	]	[ pf*(100-fo2,f-fhe,f)-pi*(100-fo2,i-fhe,i)	]
// Where:
// - pi = initial pressure in cylinder
// - pf = desired pressure
// - fo2,t = fraction of O2 in top-up gas
// - fhe,t = fraction of He in top-up gas
// - fo2,f = desired fraction of O2
// - fhe,f = desired fraction of He
// - fo2,i = starting fraction of O2
// - fhe,i = starting fraction of He
// And the unknowns:
// - po2 = pressure of O2 to add
// - phe = pressure of He to add
// - pt = pressure of top-up gas to add
//
// One invalid solution to the above is if any of the unknowns come
// out negative. If this happens, we have to set that unknown to 0
// and solve for pi instead, and the difference between the solved
// value and the given one is the amount the blender will have to
// drain. Here's an example when setting po2 to 0:
// [ fo2,i				0	fo2,t			] [ pi 	]	[ pf*fo2,f				]
// [ fhe,i				1	fhe,t			] [ phe ] = [ pf*fhe,f				]
// [ 100-fo2,i-fhe,i	0	100-fo2,t-fhe,t	] [ pt	]	[ pf*(100-fo2,f-fhe,f)	]
public class BlendResult extends Activity {
	
	// Our known parameters
	private float pi, pf, fo2t, fhet, fo2f, fhef, fo2i, fhei;
	// The parameters we are solving for
	private float po2, phe, pt, pdrain;

	private GasMixDbAdapter mDbAdapter;
	
	private NumberFormat nf = Params.getPressureFormat();
	
	private String mResultText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.blend_result);
		
		// get our parameters
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			// TODO: handle this
			// This may happen if our app gets restarted from this
			// screen. In that case, we should go back to the
			// GasMixer Activity.
			finish();
		}
		
		pi=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_PRESSURE));
		pf=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_PRESSURE));
		fo2f=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_OXYGEN))/100;
		fhef=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_HELIUM))/100;
		fo2i=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_OXYGEN))/100;
		fhei=extras.getFloat("PARAM_"+nf.format(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_HELIUM))/100;
		
		mDbAdapter = new GasMixDbAdapter(this);
		mDbAdapter.open();
		
		fo2t=mDbAdapter.fetchSetting("fo2t");
		fhet=mDbAdapter.fetchSetting("fhet");
		
		// Now we're ready. Solve. Solution will be stored in our class
		// variables
		solve();
		
		// Make Mixes of our gases
		Mix start=new Mix(fo2i, fhei), desired=new Mix(fo2f, fhef), topup=new Mix(fo2t, fhet);

		// No solution manifests itself as pdrain being greater than pi.
		// This is a side effect of the extra checking occurring in
		// solve(), that the only solution solve() can find is to
		// increase the starting pressure in the tank.
		Resources r = getResources();
		String unit = Units.pressure(this);
		if(pdrain > pi) {
			mResultText=r.getString(R.string.result_impossible);
		} else {
			mResultText=(
					(pi == 0)? r.getString(R.string.result_start_empty)
					: String.format(r.getString(R.string.result_start),
							nf.format(pi),
							unit,
							start.friendlyName(nf, this))
			)+"\n";
			if((pdrain != pi) && ! ((pdrain == -0) && (pi == 0))) {
				mResultText+="- "+String.format(r.getString(R.string.result_drain), nf.format(pdrain), unit)+"\n";
			}
			if(po2 > 0) {
				mResultText+="- "+String.format(r.getString(R.string.result_add), nf.format(po2), unit, r.getString(R.string.oxygen))+"\n";
			}
			if(phe > 0) {
				mResultText+="- "+String.format(r.getString(R.string.result_add), nf.format(phe), unit, r.getString(R.string.helium))+"\n";
			}
			if(pt > 0) {
				mResultText+="- "+String.format(r.getString(R.string.result_topup), nf.format(pt), unit, topup.friendlyName(nf, this))+"\n";
			}
			mResultText+=String.format(r.getString(R.string.result_end), nf.format(pf), unit, desired.friendlyName(nf, this));
		}
		
		TextView resultView = (TextView) findViewById(R.id.blend_result);
		resultView.setText(mResultText);
		
		// set button listeners
		Button copy = (Button) findViewById(R.id.button_copy);
		copy.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				ClipboardManager c = (ClipboardManager)BlendResult.this.getSystemService(CLIPBOARD_SERVICE);
				c.setText(mResultText);
			}
		});
		
		// TODO: do I have enough gas button
	}
	
	private void solve() {
		pdrain=pi;
		double a[][] = { {1, 0, fo2t}, {0, 1, fhet}, {0, 0, 1-fo2t-fhet} };
		double b[][] = { {pf*fo2f-pi*fo2i}, {pf*fhef-pi*fhei}, {pf*(1-fo2f-fhef)-pi*(1-fo2i-fhei)} };
		Matrix aM=new Matrix(a), bM = new Matrix(b);
		Matrix gases=aM.solve(bM);
		po2=(float) gases.get(0,0);
		phe=(float) gases.get(1,0);
		pt=(float) gases.get(2,0);
		// Now handle the conditions where a negative pressure was found
		if(po2 < 0) {
			po2=0;
			a=aM.getArrayCopy();
			a[0][0]=fo2i;
			a[1][0]=fhei;
			a[2][0]=1-fo2i-fhei;
			b[0][0]=pf*fo2f;
			b[1][0]=pf*fhef;
			b[2][0]=pf*(1-fo2f-fhef);
			Matrix aTake2=new Matrix(a), bTake2 = new Matrix(b);
			Matrix take2 = aTake2.solve(bTake2);
			pdrain=(float) take2.get(0, 0);
			phe=(float) take2.get(1, 0);
			pt=(float) take2.get(2, 0);
		}
		if(phe < 0) {
			phe=0;
			a=aM.getArrayCopy();
			a[0][1]=fo2i;
			a[1][1]=fhei;
			a[2][1]=1-fo2i-fhei;
			b[0][0]=pf*fo2f;
			b[1][0]=pf*fhef;
			b[2][0]=pf*(1-fo2f-fhef);
			Matrix aTake3=new Matrix(a), bTake3=new Matrix(b);
			Matrix take3 = aTake3.solve(bTake3);
			pdrain=(float) take3.get(1, 0);
			po2=(float) take3.get(0, 0);
			pt=(float) take3.get(2, 0);
		}
		if(pt < 0) {
			pt=0;
			a=aM.getArrayCopy();
			a[0][2]=fo2i;
			a[1][2]=fhei;
			a[2][2]=1-fo2i-fhei;
			b[0][0]=pf*fo2f;
			b[1][0]=pf*fhef;
			b[2][0]=pf*(1-fo2f-fhef);
			Matrix aTake4=new Matrix(a), bTake4=new Matrix(b);
			Matrix take4 = aTake4.solve(bTake4);
			pdrain=(float) take4.get(2, 0);
			po2=(float) take4.get(0, 0);
			phe=(float) take4.get(1, 0);
		}
	}
}
