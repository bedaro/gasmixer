package divestoclimb.gasmixer;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SetTopup extends Activity {

	private GasMixDbAdapter mDbHelper;
	
	private UICore ui;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_topup);
		setTitle(R.string.topup_mix);
		mDbHelper = new GasMixDbAdapter(this);
		mDbHelper.open();
		
		// Initialize field structures
		HashMap<Integer,Integer>editTextIdKeyMap = new HashMap<Integer,Integer>();
		editTextIdKeyMap.put(R.id.topup_o2, UICore.KEY_TOPUP | UICore.KEY_OXYGEN);
		editTextIdKeyMap.put(R.id.topup_he, UICore.KEY_TOPUP | UICore.KEY_HELIUM);
		
		HashMap<Integer,Integer> sliderIdKeyMap = new HashMap<Integer,Integer>();
		sliderIdKeyMap.put(R.id.slider_topup_o2, UICore.KEY_TOPUP | UICore.KEY_OXYGEN);
		sliderIdKeyMap.put(R.id.slider_topup_he, UICore.KEY_TOPUP | UICore.KEY_HELIUM);
		
		HashMap<Integer,Integer> buttonIdKeyMap = new HashMap<Integer,Integer>();
		buttonIdKeyMap.put(R.id.plus_topup_o2, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.minus_topup_o2, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_OXYGEN);
		buttonIdKeyMap.put(R.id.plus_topup_he, UICore.KEY_TOPUP | UICore.KEY_PLUS | UICore.KEY_HELIUM);
		buttonIdKeyMap.put(R.id.minus_topup_he, UICore.KEY_TOPUP | UICore.KEY_MINUS | UICore.KEY_HELIUM);
		
		// Get current values from database and put into defaults hash
		HashMap<Integer,Float> defaults = new HashMap<Integer, Float>();
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_OXYGEN, mDbHelper.fetchSetting("fo2t")*100);
		defaults.put(UICore.KEY_TOPUP | UICore.KEY_HELIUM, mDbHelper.fetchSetting("fhet")*100);
		
		// Initialize the UI
		ui = new UICore(this, buttonIdKeyMap, editTextIdKeyMap, sliderIdKeyMap, defaults, null);
		
		Button set = (Button) findViewById(R.id.button_topup_set);
		set.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				mDbHelper.saveSetting("fo2t", (float)(ui.getField(UICore.KEY_TOPUP | UICore.KEY_OXYGEN)/100.0), 3);
				mDbHelper.saveSetting("fhet", (float)(ui.getField(UICore.KEY_TOPUP | UICore.KEY_HELIUM)/100.0), 3);
				
				Intent mIntent = new Intent();
				setResult(RESULT_OK, mIntent);
				finish();
			}
		});
	}
}
