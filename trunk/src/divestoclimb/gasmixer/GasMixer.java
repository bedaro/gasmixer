package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;

import divestoclimb.lib.scuba.Units;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TabHost;

public class GasMixer extends TabActivity {
	
	// Our database adaptor
	private GasMixDbAdapter mDbHelper;
	
	// Our UI elements
	private UICore ui;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mDbHelper = new GasMixDbAdapter(this);

		// Initialize database settings if they are not already set
		mDbHelper.open();
		if(mDbHelper.fetchSetting("fo2t") == -1) {
			mDbHelper.saveSetting("fo2t", (float)0.21, 3);
		}
		if(mDbHelper.fetchSetting("fhet") == -1) {
			mDbHelper.saveSetting("fhet", 0, 3);
		}

		// Fetch the setting for units
		float unit=mDbHelper.fetchSetting("units");
		if(unit == -1) {
			unit=Units.IMPERIAL;
			mDbHelper.saveSetting("units", Units.IMPERIAL, 1);
		}
		Units.change((int)unit);

		TabHost mTabHost = getTabHost();
		
		mTabHost.addTab(mTabHost.newTabSpec("tab_test1").setIndicator("Blend", getResources().getDrawable(R.drawable.blend_32)).setContent(R.id.tab_blend));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test2").setIndicator("Top-Up", getResources().getDrawable(R.drawable.topup_32)).setContent(R.id.tab_topup));
		// mTabHost.addTab(mTabHost.newTabSpec("tab_test3").setIndicator("Best Mix").setContent(R.id.tab_best));
		
		// Initialize field structures
		HashMap<Integer,Integer>editTextIdKeyMap = new HashMap<Integer,Integer>();
		editTextIdKeyMap.put(R.id.desired_o2, UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_OXYGEN);
		editTextIdKeyMap.put(R.id.desired_he, UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_HELIUM);
		editTextIdKeyMap.put(R.id.start_o2, UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		editTextIdKeyMap.put(R.id.start_he, UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		editTextIdKeyMap.put(R.id.desired_pres, UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);
		editTextIdKeyMap.put(R.id.start_pres, UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		editTextIdKeyMap.put(R.id.topup_start_o2, UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		editTextIdKeyMap.put(R.id.topup_start_he, UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_HELIUM);		
		editTextIdKeyMap.put(R.id.topup_start_pres, UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		editTextIdKeyMap.put(R.id.topup_final_pres, UICore.KEY_TOPUP | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);

		HashMap<Integer,Integer> sliderIdKeyMap = new HashMap<Integer,Integer>();
		sliderIdKeyMap.put(R.id.slider_desired_o2, UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_OXYGEN);
		sliderIdKeyMap.put(R.id.slider_desired_he, UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_HELIUM);
		sliderIdKeyMap.put(R.id.slider_start_o2, UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		sliderIdKeyMap.put(R.id.slider_start_he, UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		sliderIdKeyMap.put(R.id.slider_topup_start_o2, UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		sliderIdKeyMap.put(R.id.slider_topup_start_he, UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		
		HashMap<Integer,Integer> buttonIdKeyMap = new HashMap<Integer,Integer>();
		buttonIdKeyMap.put(R.id.plus_desired_o2, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_DESIRED | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.minus_desired_o2, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_DESIRED | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.plus_desired_he, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_DESIRED | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.minus_desired_he, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_DESIRED | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.plus_start_o2, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.minus_start_o2, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.plus_start_he, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.minus_start_he, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.plus_desired_pres, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.minus_desired_pres, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.plus_start_pres, UICore.KEY_BLEND | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.minus_start_pres, UICore.KEY_BLEND | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.plus_topup_start_o2, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.minus_topup_start_o2, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.plus_topup_start_he, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.minus_topup_start_he, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.plus_topup_start_pres, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.minus_topup_start_pres, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_STARTING | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.plus_topup_final_pres, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);
		buttonIdKeyMap.put(R.id.minus_topup_final_pres, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_DESIRED | UICore.KEY_PRESSURE);

		// Initialize the mDefaults hash
		HashMap<Integer,Float> defaults = new HashMap<Integer, Float>();
		// If an Intent was passed, use those parameters as the desired mix
		Bundle extras = getIntent().getExtras();
		defaults.put(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_OXYGEN,
				extras != null? extras.getFloat("O2_DESIRED"): new Float(32)
			);
		defaults.put(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_HELIUM,
				extras != null? extras.getFloat("HE_DESIRED"): new Float(0)
			);
		defaults.put(UICore.KEY_BLEND | UICore.KEY_DESIRED | UICore.KEY_PRESSURE, new Float(Units.pressureTankFull()));
		defaults.put(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_OXYGEN, new Float(21));
		defaults.put(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_HELIUM, new Float(0));
		defaults.put(UICore.KEY_BLEND | UICore.KEY_STARTING | UICore.KEY_PRESSURE, new Float(0));
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_OXYGEN, new Float(32));
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_HELIUM, new Float(0));
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_PRESSURE, new Float(Units.pressureTankLow()));
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_DESIRED | UICore.KEY_PRESSURE, new Float(Units.pressureTankFull()));
		
		int pressureUnitTexts[] = { R.id.desired_pres_unit,
				R.id.start_pres_unit,
				R.id.topup_start_pres_unit };
		
		// Initialize the UI
		ui = new UICore(this, buttonIdKeyMap, editTextIdKeyMap, sliderIdKeyMap, defaults, pressureUnitTexts);
		
		// Prepare the action buttons
		Button blend = (Button) findViewById(R.id.button_blend);
		blend.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(GasMixer.this, BlendResult.class);

				// Gather all data into the extras of our Intent
				Iterator<Integer> it = ui.getDefaults().keySet().iterator();
				while(it.hasNext()) {
					int key=it.next();
					if((key & UICore.KEY_BLEND) != 0) {
						i.putExtra("PARAM_"+Params.getPressureFormat().format(key), ui.getField(key));
					}
				}
				GasMixer.this.startActivity(i);
			}
		});
		
		Button topup = (Button) findViewById(R.id.button_topup);
		topup.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(GasMixer.this, TopupResult.class);

				// Gather all data into the extras of our Intent
				Iterator<Integer> it = ui.getDefaults().keySet().iterator();
				while(it.hasNext()) {
					int key=it.next();
					if((key & UICore.KEY_TOPUP) != 0) {
						i.putExtra("PARAM_"+NumberFormat.getIntegerInstance().format(key), ui.getField(key));
					}
				}
				GasMixer.this.startActivity(i);
			}
		});
		
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

		if(Units.getCurrentSystem() == Units.IMPERIAL) {
			menu.removeItem(R.id.switch_unit_imperial);
		} else if(Units.getCurrentSystem() == Units.METRIC) {
			menu.removeItem(R.id.switch_unit_metric);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i;
		switch(item.getItemId()) {
			case R.id.set_topup:
				i = new Intent(this, SetTopup.class);
				startActivityForResult(i, 0);
				return true;
			case R.id.about:
				i = new Intent(this, About.class);
				startActivityForResult(i, 0);
				return true;
			case R.id.switch_unit_metric:
				Units.change(Units.METRIC);
				ui.updateUnits(Units.IMPERIAL);
				mDbHelper.saveSetting("units", Units.METRIC, 1);
				return true;
			case R.id.switch_unit_imperial:
				Units.change(Units.IMPERIAL);
				ui.updateUnits(Units.METRIC);
				mDbHelper.saveSetting("units", Units.IMPERIAL, 1);
				return true;			
		}
		return super.onMenuItemSelected(featureId, item);
	}

}