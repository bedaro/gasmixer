package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TopupResult extends Activity {

	private float pi, pf, fo2i, fhei, fo2t, fhet, fo2f, fhef;

	private SharedPreferences mSettings, mState;

	private String mResultText;
	private Mix mResult;
	private TextView mFinalMOD, mFinalEADENDLabel, mFinalEADEND;
	private ToggleButton mTogglePo2;
	
	// Cached storage of preferences
	private boolean mO2IsNarcotic;
	private float mPo2Low, mPo2High;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.topup_result);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);

		int unit;
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		Units.change(unit);
		
		// Gas computation options
		mPo2Low = mSettings.getFloat("max_norm_po2", 1.4f);
		mPo2High = mSettings.getFloat("max_hi_po2", 1.6f);
		mO2IsNarcotic = mSettings.getBoolean("o2_is_narcotic", true);

		pi   = mState.getFloat("topup_start_pres", 0);
		pf   = mState.getFloat("topup_final_pres", 0);
		fo2i = mState.getFloat("topup_start_o2", 0.21f);
		fhei = mState.getFloat("topup_start_he", 0);

		Mix topup = TrimixPreference.stringToMix(mSettings.getString("topup_gas", "0.21 0"));
		fo2t = topup.getfO2();
		fhet = topup.getfHe();

		solve();

		mResult = new Mix(fo2f, fhef);
		Resources r = getResources();
		mResultText = String.format(r.getString(R.string.topup_result),
				Params.mixFriendlyName(mResult, this)
		);

		TextView resultView = (TextView) findViewById(R.id.result);
		resultView.setText(mResultText);
		TextView reminder1View = (TextView) findViewById(R.id.reminder1);
		reminder1View.setText(
				String.format(r.getString(R.string.topup_reminder),
						Params.mixFriendlyName(topup, this)
				)
		);
		TextView reminder2View = (TextView) findViewById(R.id.reminder2);
		reminder2View.setText(r.getString(R.string.analyze_warning));
		
		mFinalMOD = (TextView)findViewById(R.id.mod);
		mFinalEADENDLabel = (TextView)findViewById(R.id.ead_end_label);
		mFinalEADEND = (TextView)findViewById(R.id.ead_end);
		mTogglePo2 = (ToggleButton)findViewById(R.id.button_po2_hi);
		
		updateModEnd();

		mTogglePo2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateModEnd();
			}
		});

		Button close = (Button) findViewById(R.id.button_close);
		close.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void solve() {
		fo2f = (pi*fo2i+(pf-pi)*fo2t)/pf;
		fhef = (pi*fhei+(pf-pi)*fhet)/pf;
	}
	
	private void updateModEnd() {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		float mod = mResult.MOD(mTogglePo2.isChecked()? mPo2High: mPo2Low);
		mFinalMOD.setText(nf.format(mod)+" "+Params.depth(this));
		if(mResult.getHe() > 0) {
			mFinalEADENDLabel.setText(getResources().getString(R.string.end));
			mFinalEADEND.setText(nf.format(mResult.END(Math.round(mod), mO2IsNarcotic))+" "+Params.depth(this));
		} else {
			mFinalEADENDLabel.setText(getResources().getString(R.string.ead));
			mFinalEADEND.setText(nf.format(mResult.EAD(Math.round(mod)))+" "+Params.depth(this));
		}
	}
}
