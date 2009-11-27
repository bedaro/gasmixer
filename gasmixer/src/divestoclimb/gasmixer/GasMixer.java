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
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Gas Mixer main activity: handles the main layout with all tabs: blend, topup,
 * best mix
 * @author Ben Roberts (divestoclimb@gmail.com)
 *
 */
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
	private Units mUnits;
	
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
		mUnits = new Units(unit);

		final TabHost tabhost = getTabHost();

		final Resources r = getResources();
		tabhost.addTab(tabhost.newTabSpec("tab1")
				.setIndicator(getString(R.string.blend), r.getDrawable(R.drawable.blend_32))
				.setContent(R.id.tab_blend));
		tabhost.addTab(tabhost.newTabSpec("tab2")
				.setIndicator(getString(R.string.topup), r.getDrawable(R.drawable.topup_32))
				.setContent(R.id.tab_topup));
		tabhost.addTab(tabhost.newTabSpec("tab3")
				.setIndicator(getString(R.string.bestmix), r.getDrawable(android.R.drawable.btn_star))
				.setContent(R.id.tab_bestmix));

		final Bundle extras = getIntent().getExtras();

		final View tabcontent = tabhost.getTabContentView(),
			blendTab = tabcontent.findViewById(R.id.tab_blend),
			topupTab = tabcontent.findViewById(R.id.tab_topup),
			bestmixTab = tabcontent.findViewById(R.id.tab_bestmix);
		mBlendDesiredPressure = (NumberSelector)blendTab.findViewById(R.id.desired_pres);
		mBlendDesiredPressureUnit = (TextView)blendTab.findViewById(R.id.desired_pres_unit);
		mDesiredGas = (TrimixSelector)blendTab.findViewById(R.id.desired);
		mDesiredMOD = (TextView)blendTab.findViewById(R.id.desired_mod);
		mDesiredEADENDLabel = (TextView)blendTab.findViewById(R.id.desired_ead_end_label);
		mDesiredEADEND = (TextView)blendTab.findViewById(R.id.desired_ead_end);

		mDesiredGas.setOnMixChangeListener(new TrimixSelector.MixChangeListener() {
			public void onChange(TrimixSelector ts, Mix m) {
				if(m != null) {
					updateModEnd(m);
				}
			}
		});

		mStartingMix = (TextView)blendTab.findViewById(R.id.start_mix);

		NumberSelector.ValueChangedListener updateBestMix = new NumberSelector.ValueChangedListener() {
			public void onChange(NumberSelector ns, Float new_val, boolean from_user) {
				updateBestMix();
			}
		};
		mMaxDepth = (NumberSelector)bestmixTab.findViewById(R.id.maxdepth);
		mMaxDepth.setValueChangedListener(updateBestMix);
		mMaxEnd = (NumberSelector)bestmixTab.findViewById(R.id.maxend);
		mMaxEnd.setValueChangedListener(updateBestMix);
		mMaxDepthUnit = (TextView)bestmixTab.findViewById(R.id.maxdepth_unit);
		mMaxEndUnit = (TextView)bestmixTab.findViewById(R.id.maxend_unit);
		mMaxPo2 = (NumberSelector)bestmixTab.findViewById(R.id.maxpo2);
		mMaxPo2.setValueChangedListener(updateBestMix);
		mBestMixResult = (TextView)bestmixTab.findViewById(R.id.bestmix_result);

		mCylinderDescription = (TextView)topupTab.findViewById(R.id.cylinder);
		mTopupStartPressure = (NumberSelector)topupTab.findViewById(R.id.topup_start_pres);
		mTopupStartPressureUnit = (TextView)topupTab.findViewById(R.id.topup_start_pres_unit);
		mTopupGas = (TrimixSelector)topupTab.findViewById(R.id.topup);
		mTopupFinalPressure = (NumberSelector)topupTab.findViewById(R.id.topup_final_pres);
		
		mTogglePo2 = (ToggleButton)blendTab.findViewById(R.id.button_po2_hi);
		
		initFields(extras);

		mTogglePo2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateModEnd(mDesiredGas.getMix());
			}
		});
		
		// Prepare the action buttons
		final Button edit_start = (Button) blendTab.findViewById(R.id.start_change);
		edit_start.setOnClickListener(this);
		final Button blend = (Button) blendTab.findViewById(R.id.button_blend);
		blend.setOnClickListener(this);
		
		final Button topup = (Button) topupTab.findViewById(R.id.button_topup);
		topup.setOnClickListener(this);
		final Button cylinderChangeButton = (Button)topupTab.findViewById(R.id.cylinder_change);
		if(cylinderChangeButton != null) {
			cylinderChangeButton.setOnClickListener(this);
		}
		
		final Button bestmix_blend = (Button) bestmixTab.findViewById(R.id.bestmix_blend);
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
		final SharedPreferences settings = mSettings, state = mState;
		final Units units = mUnits;

		// Gas computation options
		mPo2Low = settings.getFloat("max_norm_po2", 1.4f);
		mPo2High = settings.getFloat("max_hi_po2", 1.6f);
		mO2IsNarcotic = settings.getBoolean("o2_is_narcotic", true);
		
		// Also retrieve any state preferences that other activities change
		mBlendStartPressure = state.getFloat("start_pres", units.pressureTankLow());
		mBlendStartMix = new Mix(
				state.getFloat("start_o2", 0.21f),
				state.getFloat("start_he", 0f)
		);

		// Units
		Integer unit, last_unit = null;
		try {
			unit = NumberFormat.getIntegerInstance().parse(settings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		if(unit.compareTo(units.getCurrentSystem()) != 0) {
			// The user switched units since we were last loaded.
			last_unit = units.getCurrentSystem();
			units.change(unit);
		}
		
		final Thread t = new Thread() {
			public void run() {
				// Check to make sure a Content Provider for cylinders is available if
				// Van der Waals support is on
				if(mSettings.getBoolean("vdw", true)) {
					if(! testCylinders()) {
						// Van der Waals support is enabled but no activity is on the system
						// to handle cylinder size selection. Show the dialog to let the user
						// resolve the situation
						messageHandler.sendMessage(Message.obtain(messageHandler, MESSAGE_SHOW_SCUBATANKS));
					} else {
						// Make sure we have a valid cylinder ID defined in the state.
						try {
							final long id = mState.getLong("cylinderid", -1);
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
							final Cursor c = getContentResolver().query(CylinderSizeClient.CONTENT_URI, new String[] { CylinderSizeClient._ID }, null, null, null);
							c.moveToFirst();
							messageHandler.sendMessage(Message.obtain(messageHandler, MESSAGE_SAVE_CYLINDERID, new Long(c.getLong(c.getColumnIndexOrThrow(CylinderSizeClient._ID)))));
							
							c.close();
						}
						// Explicitly set the preference value to on, since it may have been
						// undefined before
						messageHandler.sendMessage(Message.obtain(messageHandler, MESSAGE_SAVE_VDW));
					}
				}
			}
		};
		t.start();

		updateUnits(last_unit);
		updateModEnd(mDesiredGas.getMix());
		updateStartMix();
		updateBestMix();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Settings.class));
				return true;
			case R.id.about:
				startActivity(new Intent(this, About.class));
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
						mSettings.edit().putBoolean("vdw", false).commit();
					}
				})
				.create();
		}
		return null;
	}
	
	/**
	 * Our various button handlers
	 * @param v The Button that was pressed
	 */
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.start_change:
			GasMixer.this.startActivity(
					new Intent(this, SetStarting.class)
			);
			break;
		case R.id.button_blend:
			GasMixer.this.startActivity(
					new Intent(this, BlendResult.class));
			break;
		case R.id.cylinder_change:
			Intent cylinders = new Intent(Intent.ACTION_GET_CONTENT);
			cylinders.setType("vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size");
			startActivityForResult(cylinders, 0);
			break;
		case R.id.button_topup:
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
		// Currently the only activity we start that returns a result is CylinderSizes
		if(resultCode == RESULT_OK) {
			final long id = intent.getExtras().getLong("selected");
			mState.edit().putLong("cylinderid", id).commit();
			updateCylinder();
		}
	}
	
	private static final int MESSAGE_SHOW_SCUBATANKS = 1;
	private static final int MESSAGE_SAVE_CYLINDERID = 2;
	private static final int MESSAGE_SAVE_VDW = 3;
	
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MESSAGE_SHOW_SCUBATANKS:
				showDialog(DIALOG_INSTALL_SCUBATANKS);
				break;
			case MESSAGE_SAVE_CYLINDERID:
				mState.edit().putLong("cylinderid", (Long)msg.obj).commit();
				break;
			case MESSAGE_SAVE_VDW:
				mState.edit().putBoolean("vdw", true).commit();
				break;
			}
		}
	};
	
	/**
	 * Test for the ability to fetch cylinder ID's on this system
	 * @return true if the cylinder size activity was found, false otherwise
	 */
	private boolean testCylinders() {
		final Intent cylinderTest = new Intent(Intent.ACTION_GET_CONTENT);
		cylinderTest.setType("vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size");
		return (cylinderTest.resolveActivity(getPackageManager()) != null);
	}

	/**
	 * Initialize all fields in the Activity based on values in the state and the
	 * current units
	 * @param extras Any special parameters passed to the activity that should
	 * override the state
	 */
	private void initFields(Bundle extras) {
		final Units u = mUnits;
		final SharedPreferences state = mState;
		mBlendDesiredPressure.setValue(state.getFloat("desired_pres", u.pressureTankFull()));
		/*mDesiredGas.setMix(new Mix(
				extras != null? extras.getFloat("O2_DESIRED"): state.getFloat("desired_o2", 0.32f),
				extras != null? extras.getFloat("HE_DESIRED"): state.getFloat("desired_he", 0)
		));*/
		mDesiredGas.setMix(new Mix(state.getFloat("desired_o2", 0.32f), state.getFloat("desired_he", 0)));
		mTogglePo2.setChecked(state.getBoolean("po2_high", false));
		mMaxDepth.setValue(state.getFloat("max_depth", new Float(u.depthToxic())));
		mMaxEnd.setValue(state.getFloat("max_end", new Float(u.depthNarcotic())));
		mMaxPo2.setValue(state.getFloat("max_po2", 1.4f));
		mTopupGas.setMix(new Mix(
				state.getFloat("topup_start_o2", 0.32f),
				state.getFloat("topup_start_he", 0)
		));
		mTopupStartPressure.setValue(state.getFloat("topup_start_pres", u.pressureTankLow()));
		mTopupFinalPressure.setValue(state.getFloat("topup_final_pres", u.pressureTankFull()));
		
		if(mCylinderDescription != null) {
			updateCylinder();
		}
	}

	// Write out all Activity values to the state file.
	// We use a state file instead of an instance state Bundle so all Activities in
	// the application can exchange data with it.
	// Called by onPause()
	public void saveState() {
		final Mix desired = mDesiredGas.getMix(), topup_start = mTopupGas.getMix();
		mState.edit()
			.putFloat("desired_pres", (int)Math.floor(mBlendDesiredPressure.getValue()))
			.putFloat("start_pres", mBlendStartPressure)
			.putFloat("desired_o2", (float)desired.getfO2())
			.putFloat("desired_he", (float)desired.getfHe())
			.putFloat("topup_start_o2", (float)topup_start.getfO2())
			.putFloat("topup_start_he", (float)topup_start.getfHe())
			.putBoolean("po2_high", mTogglePo2.isChecked())
			.putFloat("max_depth", mMaxDepth.getValue())
			.putFloat("max_end", mMaxEnd.getValue())
			.putFloat("max_po2", mMaxPo2.getValue())
			.putFloat("topup_start_pres", mTopupStartPressure.getValue())
			.putFloat("topup_final_pres", mTopupFinalPressure.getValue())
			.commit();
	}

	/**
	 * Set everything in the interface that's unit-dependent
	 * @param last_unit The previous unit system that was being used. If set,
	 * also converts all values in the state to the new system of units.
	 */
	public void updateUnits(Integer last_unit) {
		final NumberSelector blend_desired_pressure = mBlendDesiredPressure,
				topup_start_pressure = mTopupStartPressure,
				topup_final_pressure = mTopupFinalPressure,
				max_depth = mMaxDepth,
				max_end = mMaxEnd;
		final Units u = mUnits;
		blend_desired_pressure.setDecimalPlaces(0);
		blend_desired_pressure.setLimits(0f, new Float(u.pressureTankMax()));
		blend_desired_pressure.setIncrement(new Float(u.pressureIncrement()));
		blend_desired_pressure.setNonIncrementValues(u.pressureNonstandard());
		mBlendDesiredPressureUnit.setText(Params.pressure(this, u)+":");

		topup_start_pressure.setDecimalPlaces(0);
		topup_start_pressure.setLimits(0f, new Float(u.pressureTankMax()));
		topup_start_pressure.setIncrement(new Float(u.pressureIncrement()));
		mTopupStartPressureUnit.setText(Params.pressure(this, u)+":");

		topup_final_pressure.setDecimalPlaces(0);
		topup_final_pressure.setLimits(0f, new Float(u.pressureTankMax()));
		topup_final_pressure.setIncrement(new Float(u.pressureIncrement()));
		topup_final_pressure.setNonIncrementValues(u.pressureNonstandard());

		max_depth.setDecimalPlaces(0);
		max_depth.setLimits(0f, new Float(u.depthMax()));
		max_depth.setIncrement(new Float(u.depthIncrement()));
		mMaxDepthUnit.setText(Params.depth(this, u)+":");

		max_end.setDecimalPlaces(0);
		max_end.setLimits(0f, new Float(u.depthMaxNarcotic()));
		max_end.setIncrement(new Float(u.depthIncrement()));
		mMaxEndUnit.setText(Params.depth(this, u)+":");

		if(last_unit != null) {
			// Convert existing values to new units
			max_depth.setValue(u.convertDepth(max_depth.getValue(), last_unit));
			max_end.setValue(u.convertDepth(max_end.getValue(), last_unit));
			blend_desired_pressure.setValue(
					u.convertPressure(blend_desired_pressure.getValue(), last_unit));
			topup_start_pressure.setValue(
					u.convertPressure(topup_start_pressure.getValue(), last_unit));
			topup_final_pressure.setValue(
					u.convertPressure(topup_final_pressure.getValue(), last_unit));
			mBlendStartPressure = u.convertPressure(mBlendStartPressure, last_unit);
		}
	}

	/**
	 * Update the MOD, END and/or EAD fields
	 * @param m The mix to use to compute the MOD/END/EAD
	 */
	private void updateModEnd(Mix m) {
		final Units u = mUnits;
		final NumberFormat nf = NumberFormat.getIntegerInstance();
		final float mod = m.MOD(u, mTogglePo2.isChecked()? mPo2High: mPo2Low);
		mDesiredMOD.setText(nf.format(mod)+" "+Params.depth(this, u));
		if(m.getHe() > 0) {
			mDesiredEADENDLabel.setText(getResources().getString(R.string.end));
			mDesiredEADEND.setText(nf.format(m.END(Math.round(mod), u, mO2IsNarcotic))+" "+Params.depth(this, u));
		} else {
			mDesiredEADENDLabel.setText(getResources().getString(R.string.ead));
			mDesiredEADEND.setText(nf.format(m.EAD(Math.round(mod), u))+" "+Params.depth(this, u));
		}
	}

	private void updateStartMix() {
		final NumberFormat nf = Params.getPressureFormat(mUnits);
		if(mBlendStartPressure == 0) {
			mStartingMix.setText(getResources().getString(R.string.empty_tank));
		} else {
			mStartingMix.setText(String.format(
					getResources().getString(R.string.gas_amount),
					nf.format(mBlendStartPressure),
					Params.pressure(this, mUnits),
					Params.mixFriendlyName(mBlendStartMix, this)
			));
		}
	}
	
	private void updateBestMix() {
		final Float maxdepth = mMaxDepth.getValue(),
			maxend = mMaxEnd.getValue(),
			maxpo2 = mMaxPo2.getValue();
		if(maxdepth != null && maxend != null && maxpo2 != null) {
			mBestMix = Mix.best(
					Math.round(maxdepth),
					Math.round(maxend),
					mUnits,
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
		final long id = mState.getLong("cylinderid", -1);
		if(id != -1) {
			Cursor c = getContentResolver().query(Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
					String.valueOf(id)), new String[] { CylinderSizeClient.NAME }, null, null, null);
			c.moveToFirst();
			mCylinderDescription.setText(c.getString(c.getColumnIndexOrThrow(CylinderSizeClient.NAME)));
			c.close();
		}
	}

}