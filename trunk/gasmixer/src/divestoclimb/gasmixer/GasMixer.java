package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.CylinderSizeClient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
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

public class GasMixer extends TabActivity implements Button.OnClickListener {

	protected SharedPreferences mSettings, mState;

	// Cached storage of preferences
	private boolean mO2IsNarcotic;
	private float mPo2Low, mPo2High, mBlendStartPressure;
	private Mix mBlendStartMix;

	private TrimixSelector mDesiredGas, mTopupGas;
	private NumberSelector mMaxDepth, mMaxEnd, mMaxPo2, mBlendDesiredPressure,
			mTopupStartPressure, mTopupFinalPressure;
	private TextView mMaxDepthUnit, mMaxEndUnit, mBlendDesiredPressureUnit,
			mTopupStartPressureUnit, mDesiredMOD, mDesiredEADENDLabel,
			mDesiredEADEND, mStartingMix, mBestMixResult, mCylinderDescription;
	private ToggleButton mTogglePo2;
	private Mix mBestMix;
	
	private static final int DIALOG_INSTALL_SCUBATANKS = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mState = getSharedPreferences(Params.STATE_NAME, 0);

		if(mSettings.getBoolean("vdw", true) && testCylinders()) {
			setContentView(R.layout.main_cylinder);
		} else {
			setContentView(R.layout.main);
		}

		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		Units.change(unit);

		TabHost tabhost = getTabHost();

		Resources r = getResources();
		tabhost.addTab(tabhost.newTabSpec("tab1")
				.setIndicator(r.getString(R.string.blend), r.getDrawable(R.drawable.blend_32))
				.setContent(R.id.tab_blend));
		tabhost.addTab(tabhost.newTabSpec("tab2")
				.setIndicator(r.getString(R.string.topup), r.getDrawable(R.drawable.topup_32))
				.setContent(R.id.tab_topup));
		tabhost.addTab(tabhost.newTabSpec("tab3")
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

		mCylinderDescription = (TextView)findViewById(R.id.cylinder);
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
		edit_start.setOnClickListener(this);
		Button blend = (Button) findViewById(R.id.button_blend);
		blend.setOnClickListener(this);
		
		Button topup = (Button) findViewById(R.id.button_topup);
		topup.setOnClickListener(this);
		Button cylinderChangeButton = (Button)findViewById(R.id.cylinder_change);
		if(cylinderChangeButton != null) {
			cylinderChangeButton.setOnClickListener(this);
		}
		
		Button bestmix_blend = (Button) findViewById(R.id.bestmix_blend);
		bestmix_blend.setOnClickListener(this);
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
		SharedPreferences settings = mSettings, state = mState;

		// Gas computation options
		mPo2Low = settings.getFloat("max_norm_po2", 1.4f);
		mPo2High = settings.getFloat("max_hi_po2", 1.6f);
		mO2IsNarcotic = settings.getBoolean("o2_is_narcotic", true);
		
		// Also retrieve any state preferences that other activities change
		mBlendStartPressure = state.getFloat("start_pres", Units.pressureTankLow());
		mBlendStartMix = new Mix(
				state.getFloat("start_o2", 0.21f),
				state.getFloat("start_he", 0f)
		);

		// Units
		Integer unit, last_unit = null;
		try {
			unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		if(unit.compareTo(Units.getCurrentSystem()) != 0) {
			// The user switched units since we were last loaded.
			last_unit = Units.getCurrentSystem();
			Units.change(unit);
		}
		
		// Check to make sure a Content Provider for cylinders is available if
		// Van der Waals support is on
		boolean vdw = settings.getBoolean("vdw", true);
		if(vdw) {
			if(! testCylinders()) {
				// Van der Waals support is enabled but no activity is on the system
				// to handle cylinder size selection. Show the dialog to let the user
				// resolve the situation
				showDialog(DIALOG_INSTALL_SCUBATANKS);
			} else {
				// Make sure we have a valid cylinder ID defined in the state.
				try {
					long id = state.getLong("cylinderid", -1);
					if(id == -1) {
						// No ID in the state
						throw new Exception();
					}
					if(managedQuery(Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
							String.valueOf(id)), null, null, null, null) == null) {
						// The ID is invalid
						throw new Exception();
					}
				} catch(Exception e) {
					// Pick the first available cylinder from the list of cylinder
					// sizes
					Cursor c = getContentResolver().query(CylinderSizeClient.CONTENT_URI, new String[] { CylinderSizeClient._ID }, null, null, null);
					c.moveToFirst();
					SharedPreferences.Editor ed = state.edit();
					ed.putLong("cylinderid", c.getLong(c.getColumnIndexOrThrow(CylinderSizeClient._ID)));
					ed.commit();
					c.close();
				}
			}
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
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_INSTALL_SCUBATANKS:
			return new AlertDialog.Builder(this)
				.setIcon(R.drawable.tank_icon)
				.setTitle(R.string.scubatanks_needed)
				.setMessage(R.string.scubatanks_message)
				.setCancelable(false)
				.setPositiveButton(R.string.download_now, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent findScubaTanks = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:divestoclimb.scuba.equipment"));
						startActivity(findScubaTanks);
					}
				})
				.setNegativeButton(R.string.use_ideal, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences.Editor e = mSettings.edit();
						e.putBoolean("vdw", false);
						e.commit();
					}
				})
				.create();
		}
		return null;
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.start_change:
			GasMixer.this.startActivity(
					new Intent(this, SetStarting.class)
			);
			break;
		case R.id.button_blend:
			saveState();
			GasMixer.this.startActivity(
					new Intent(this, BlendResult.class));
			break;
		case R.id.cylinder_change:
			Intent cylinders = new Intent(Intent.ACTION_GET_CONTENT);
			cylinders.setType("vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size");
			startActivityForResult(cylinders, 0);
			break;
		case R.id.button_topup:
			saveState();
			GasMixer.this.startActivity(
					new Intent(this, TopupResult.class));
			break;
		case R.id.bestmix_blend:
			if(mBestMix != null) {
				mDesiredGas.setMix(mBestMix);
				getTabHost().setCurrentTab(0);
			}
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
	
	// Test for the ability to fetch cylinder ID's on this system
	private boolean testCylinders() {
		Intent cylinderTest = new Intent(Intent.ACTION_GET_CONTENT);
		cylinderTest.setType("vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size");
		return (cylinderTest.resolveActivity(getPackageManager()) != null);
	}

	private void initFields(Bundle extras) {
		SharedPreferences state = mState;
		mBlendDesiredPressure.setValue(state.getFloat("desired_pres", Units.pressureTankFull()));
		mDesiredGas.setMix(new Mix(
				extras != null? extras.getFloat("O2_DESIRED"): state.getFloat("desired_o2", 0.32f),
				extras != null? extras.getFloat("HE_DESIRED"): state.getFloat("desired_he", 0)
		));
		mTogglePo2.setChecked(state.getBoolean("po2_high", false));
		mMaxDepth.setValue(state.getFloat("max_depth", new Float(Units.depthToxic())));
		mMaxEnd.setValue(state.getFloat("max_end", new Float(Units.depthNarcotic())));
		mMaxPo2.setValue(state.getFloat("max_po2", 1.4f));
		mTopupGas.setMix(new Mix(
				state.getFloat("topup_start_o2", 0.32f),
				state.getFloat("topup_start_he", 0)
		));
		mTopupStartPressure.setValue(state.getFloat("topup_start_pres", Units.pressureTankLow()));
		mTopupFinalPressure.setValue(state.getFloat("topup_final_pres", Units.pressureTankFull()));
		
		if(mCylinderDescription != null) {
			updateCylinder();
		}
	}

	public void saveState() {
		SharedPreferences.Editor e = mState.edit();
		e.putFloat("desired_pres", (int)Math.floor(mBlendDesiredPressure.getValue()));
		Mix desired = mDesiredGas.getMix(), topup_start = mTopupGas.getMix();
		e.putFloat("start_pres", mBlendStartPressure);
		e.putFloat("desired_o2", (float)desired.getfO2());
		e.putFloat("desired_he", (float)desired.getfHe());
		e.putFloat("topup_start_o2", (float)topup_start.getfO2());
		e.putFloat("topup_start_he", (float)topup_start.getfHe());
		e.putBoolean("po2_high", mTogglePo2.isChecked());
		e.putFloat("max_depth", mMaxDepth.getValue());
		e.putFloat("max_end", mMaxEnd.getValue());
		e.putFloat("max_po2", mMaxPo2.getValue());
		e.putFloat("topup_start_pres", mTopupStartPressure.getValue());
		e.putFloat("topup_final_pres", mTopupFinalPressure.getValue());
		e.commit();
	}

	public void updateUnits(Integer last_unit) {
		NumberSelector blend_desired_pressure = mBlendDesiredPressure,
				topup_start_pressure = mTopupStartPressure,
				topup_final_pressure = mTopupFinalPressure,
				max_depth = mMaxDepth,
				max_end = mMaxEnd;
		blend_desired_pressure.setDecimalPlaces(0);
		blend_desired_pressure.setLimits(0f, new Float(Units.pressureTankMax()));
		blend_desired_pressure.setIncrement(new Float(Units.pressureIncrement()));
		mBlendDesiredPressureUnit.setText(Params.pressure(this)+":");

		topup_start_pressure.setDecimalPlaces(0);
		topup_start_pressure.setLimits(0f, new Float(Units.pressureTankMax()));
		topup_start_pressure.setIncrement(new Float(Units.pressureIncrement()));
		mTopupStartPressureUnit.setText(Params.pressure(this)+":");

		topup_final_pressure.setDecimalPlaces(0);
		topup_final_pressure.setLimits(0f, new Float(Units.pressureTankMax()));
		topup_final_pressure.setIncrement(new Float(Units.pressureIncrement()));

		max_depth.setDecimalPlaces(0);
		max_depth.setLimits(0f, new Float(Units.depthMax()));
		max_depth.setIncrement(new Float(Units.depthIncrement()));
		mMaxDepthUnit.setText(Params.depth(this)+":");

		max_end.setDecimalPlaces(0);
		max_end.setLimits(0f, new Float(Units.depthMaxNarcotic()));
		max_end.setIncrement(new Float(Units.depthIncrement()));
		mMaxEndUnit.setText(Params.depth(this)+":");

		if(last_unit != null) {
			// Convert existing values to new units
			max_depth.setValue(Units.convertDepth(max_depth.getValue(), last_unit));
			max_end.setValue(Units.convertDepth(max_end.getValue(), last_unit));
			blend_desired_pressure.setValue(
					Units.convertPressure(blend_desired_pressure.getValue(), last_unit));
			topup_start_pressure.setValue(
					Units.convertPressure(topup_start_pressure.getValue(), last_unit));
			topup_final_pressure.setValue(
					Units.convertPressure(topup_final_pressure.getValue(), last_unit));
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