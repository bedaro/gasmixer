package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

public class GasMixer extends TabActivity {

	protected SharedPreferences mSettings, mState;

	// Cached storage of preferences
	private boolean mO2IsNarcotic;
	private float mPo2Low, mPo2High, mBlendStartPressure;
	private Mix mBlendStartMix;
	
	private TabHost mTabHost;

	private TrimixSelector mDesiredGas, mTopupGas;
	private NumberSelector mMaxDepth, mMaxEnd, mMaxPo2, mBlendDesiredPressure,
			mTopupStartPressure, mTopupFinalPressure;
	private TextView mMaxDepthUnit, mMaxEndUnit, mBlendDesiredPressureUnit,
			mTopupStartPressureUnit, mDesiredMOD, mDesiredEADENDLabel,
			mDesiredEADEND, mStartingMix, mBestMixResult;
	private ToggleButton mTogglePo2;
	private Mix mBestMix;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);

		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		Units.change(unit);

		mTabHost = getTabHost();

		Resources r = getResources();
		mTabHost.addTab(mTabHost.newTabSpec("tab1")
				.setIndicator(r.getString(R.string.blend), r.getDrawable(R.drawable.blend_32))
				.setContent(R.id.tab_blend));
		mTabHost.addTab(mTabHost.newTabSpec("tab2")
				.setIndicator(r.getString(R.string.topup), r.getDrawable(R.drawable.topup_32))
				.setContent(R.id.tab_topup));
		mTabHost.addTab(mTabHost.newTabSpec("tab3")
				.setIndicator(r.getString(R.string.bestmix), r.getDrawable(android.R.drawable.btn_star))
				.setContent(R.id.tab_bestmix));

		Bundle extras = getIntent().getExtras();

		mBlendDesiredPressure = (NumberSelector)findViewById(R.id.desired_pres);
		mBlendDesiredPressureUnit = (TextView)findViewById(R.id.desired_pres_unit);
		mDesiredGas = (TrimixSelector)findViewById(R.id.desired);
		mDesiredMOD = (TextView)findViewById(R.id.desired_mod);
		mDesiredEADENDLabel = (TextView)findViewById(R.id.desired_ead_end_label);
		mDesiredEADEND = (TextView)findViewById(R.id.desired_ead_end);

		mDesiredGas.setOnMixChangeListener(new TrimixSelector.MixChangeListener() {
			public void onChange(TrimixSelector ts, Mix m) {
				if(m == null) {
					return;
				}
				updateModEnd(m);
			}
		});

		mStartingMix = (TextView)findViewById(R.id.start_mix);

		NumberSelector.ValueChangedListener updateBestMix = new NumberSelector.ValueChangedListener() {
			public void onChange(NumberSelector ns, Float new_val, boolean from_user) {
				updateBestMix();
			}
		};
		mMaxDepth = (NumberSelector)findViewById(R.id.maxdepth);
		mMaxDepth.setValueChangedListener(updateBestMix);
		mMaxEnd = (NumberSelector)findViewById(R.id.maxend);
		mMaxEnd.setValueChangedListener(updateBestMix);
		mMaxDepthUnit = (TextView)findViewById(R.id.maxdepth_unit);
		mMaxEndUnit = (TextView)findViewById(R.id.maxend_unit);
		mMaxPo2 = (NumberSelector)findViewById(R.id.maxpo2);
		mMaxPo2.setValueChangedListener(updateBestMix);
		mBestMixResult = (TextView)findViewById(R.id.bestmix_result);
		
		mTopupStartPressure = (NumberSelector)findViewById(R.id.topup_start_pres);
		mTopupStartPressureUnit = (TextView)findViewById(R.id.topup_start_pres_unit);
		mTopupGas = (TrimixSelector)findViewById(R.id.topup);
		mTopupFinalPressure = (NumberSelector)findViewById(R.id.topup_final_pres);
		
		mTogglePo2 = (ToggleButton)findViewById(R.id.button_po2_hi);
		
		initFields(extras);

		mTogglePo2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateModEnd(mDesiredGas.getMix());
			}
		});
		
		// Prepare the action buttons
		Button edit_start = (Button) findViewById(R.id.start_change);
		edit_start.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				GasMixer.this.startActivity(
						new Intent(GasMixer.this, SetStarting.class)
				);
			}
		});
		Button blend = (Button) findViewById(R.id.button_blend);
		blend.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				saveState();
				GasMixer.this.startActivity(
						new Intent(GasMixer.this, BlendResult.class));
			}
		});
		
		Button topup = (Button) findViewById(R.id.button_topup);
		topup.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				saveState();
				GasMixer.this.startActivity(
						new Intent(GasMixer.this, TopupResult.class));
			}
		});
		
		Button bestmix_blend = (Button) findViewById(R.id.bestmix_blend);
		bestmix_blend.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if(mBestMix != null) {
					mDesiredGas.setMix(mBestMix);
					mTabHost.setCurrentTab(0);
				}
			}
		});

	}

	@Override
	public void onPause() {
		super.onPause();
		saveState();
	}

	@Override
	public void onResume() {
		super.onResume();

		// When we resume we need to retrieve all preferences in case they
		// changed while we were paused.

		// Gas computation options
		mPo2Low = mSettings.getFloat("max_norm_po2", 1.4f);
		mPo2High = mSettings.getFloat("max_hi_po2", 1.6f);
		mO2IsNarcotic = mSettings.getBoolean("o2_is_narcotic", true);
		
		// Also retrieve any state preferences that other activities change
		mBlendStartPressure = mState.getFloat("start_pres", Units.pressureTankLow());
		mBlendStartMix = new Mix(
				mState.getFloat("start_o2", 0.21f),
				mState.getFloat("start_he", 0f)
		);

		// Units
		Integer unit, last_unit = null;
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		if(unit.compareTo(Units.getCurrentSystem()) != 0) {
			// The user switched units since we were last loaded.
			last_unit = Units.getCurrentSystem();
			Units.change(unit);
		}
		updateUnits(last_unit);
		updateModEnd(mDesiredGas.getMix());
		updateStartMix();
		updateBestMix();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	// Override the onPrepareOptionsMenu class to remove the menu
	// item for the units system currently in use.
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i;
		switch(item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Settings.class));
				return true;
			case R.id.about:
				i = new Intent(this, About.class);
				startActivityForResult(i, 0);
				return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void initFields(Bundle extras) {
		mBlendDesiredPressure.setValue(mState.getFloat("desired_pres", Units.pressureTankFull()));
		mDesiredGas.setMix(new Mix(
				extras != null? extras.getFloat("O2_DESIRED"): mState.getFloat("desired_o2", 0.32f),
				extras != null? extras.getFloat("HE_DESIRED"): mState.getFloat("desired_he", 0)
		));
		mTogglePo2.setChecked(mState.getBoolean("po2_high", false));
		mMaxDepth.setValue(mState.getFloat("max_depth", new Float(Units.depthToxic())));
		mMaxEnd.setValue(mState.getFloat("max_end", new Float(Units.depthNarcotic())));
		mMaxPo2.setValue(mState.getFloat("max_po2", 1.4f));
		mTopupGas.setMix(new Mix(
				mState.getFloat("topup_start_o2", 0.32f),
				mState.getFloat("topup_start_he", 0)
		));
		mTopupStartPressure.setValue(mState.getFloat("topup_start_pres", Units.pressureTankLow()));
		mTopupFinalPressure.setValue(mState.getFloat("topup_final_pres", Units.pressureTankFull()));
	}

	public void saveState() {
		SharedPreferences.Editor e = mState.edit();
		e.putFloat("desired_pres", (int)Math.floor(mBlendDesiredPressure.getValue()));
		Mix desired = mDesiredGas.getMix(), topup_start = mTopupGas.getMix();
		e.putFloat("start_pres", mBlendStartPressure);
		e.putFloat("desired_o2", desired.getfO2());
		e.putFloat("desired_he", desired.getfHe());
		e.putFloat("topup_start_o2", topup_start.getfO2());
		e.putFloat("topup_start_he", topup_start.getfHe());
		e.putBoolean("po2_high", mTogglePo2.isChecked());
		e.putFloat("max_depth", mMaxDepth.getValue());
		e.putFloat("max_end", mMaxEnd.getValue());
		e.putFloat("max_po2", mMaxPo2.getValue());
		e.putFloat("topup_start_pres", mTopupStartPressure.getValue());
		e.putFloat("topup_final_pres", mTopupFinalPressure.getValue());
		e.commit();
	}

	public void updateUnits(Integer last_unit) {
		mBlendDesiredPressure.setDecimalPlaces(0);
		mBlendDesiredPressure.setLimits(0f, new Float(Units.pressureTankMax()));
		mBlendDesiredPressure.setIncrement(new Float(Units.pressureIncrement()));
		mBlendDesiredPressureUnit.setText(Params.pressure(this)+":");

		mTopupStartPressure.setDecimalPlaces(0);
		mTopupStartPressure.setLimits(0f, new Float(Units.pressureTankMax()));
		mTopupStartPressure.setIncrement(new Float(Units.pressureIncrement()));
		mTopupStartPressureUnit.setText(Params.pressure(this)+":");

		mTopupFinalPressure.setDecimalPlaces(0);
		mTopupFinalPressure.setLimits(0f, new Float(Units.pressureTankMax()));
		mTopupFinalPressure.setIncrement(new Float(Units.pressureIncrement()));

		mMaxDepth.setDecimalPlaces(0);
		mMaxDepth.setLimits(0f, new Float(Units.depthMax()));
		mMaxDepth.setIncrement(new Float(Units.depthIncrement()));
		mMaxDepthUnit.setText(Params.depth(this)+":");

		mMaxEnd.setDecimalPlaces(0);
		mMaxEnd.setLimits(0f, new Float(Units.depthMaxNarcotic()));
		mMaxEnd.setIncrement(new Float(Units.depthIncrement()));
		mMaxEndUnit.setText(Params.depth(this)+":");

		if(last_unit != null) {
			// Convert existing values to new units
			mMaxDepth.setValue(Units.convertDepth(mMaxDepth.getValue(), last_unit));
			mMaxEnd.setValue(Units.convertDepth(mMaxEnd.getValue(), last_unit));
			mBlendDesiredPressure.setValue(
					Units.convertPressure(mBlendDesiredPressure.getValue(), last_unit));
			mTopupStartPressure.setValue(
					Units.convertPressure(mTopupStartPressure.getValue(), last_unit));
			mTopupFinalPressure.setValue(
					Units.convertPressure(mTopupFinalPressure.getValue(), last_unit));
			mBlendStartPressure = Units.convertPressure(mBlendStartPressure, last_unit);
		}
	}
	
	private void updateModEnd(Mix m) {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		float mod = m.MOD(mTogglePo2.isChecked()? mPo2High: mPo2Low);
		mDesiredMOD.setText(nf.format(mod)+" "+Params.depth(this));
		if(m.getHe() > 0) {
			mDesiredEADENDLabel.setText(getResources().getString(R.string.end));
			mDesiredEADEND.setText(nf.format(m.END(Math.round(mod), mO2IsNarcotic))+" "+Params.depth(this));
		} else {
			mDesiredEADENDLabel.setText(getResources().getString(R.string.ead));
			mDesiredEADEND.setText(nf.format(m.EAD(Math.round(mod)))+" "+Params.depth(this));
		}
	}

	private void updateStartMix() {
		NumberFormat nf = Params.getPressureFormat();
		if(mBlendStartPressure == 0) {
			mStartingMix.setText(getResources().getString(R.string.empty_tank));
		} else {
			mStartingMix.setText(String.format(
					getResources().getString(R.string.gas_amount),
					nf.format(mBlendStartPressure),
					Params.pressure(this),
					Params.mixFriendlyName(mBlendStartMix, this)
			));
		}
	}
	
	private void updateBestMix() {
		Float maxdepth = mMaxDepth.getValue(),
		maxend = mMaxEnd.getValue(),
		maxpo2 = mMaxPo2.getValue();
		if(maxdepth != null && maxend != null && maxpo2 != null) {
			mBestMix = Mix.best(
					Math.round(maxdepth),
					Math.round(maxend),
					maxpo2,
					mO2IsNarcotic
			);
			if(mBestMix == null) {
				mBestMixResult.setText(getResources().getString(R.string.no_mix));
			} else {
				mBestMixResult.setText(Params.mixFriendlyName(mBestMix, GasMixer.this));
			}
		}
	}

}