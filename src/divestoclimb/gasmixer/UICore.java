package divestoclimb.gasmixer;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;

import org.openintents.widget.Slider;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class UICore {
	// This is the way to handle lots of widgets!!!
	private HashMap<Integer,Button> mPlusMinusButtonArray;
	private HashMap<Integer,EditText> mEditTextArray;
	private HashMap<Integer,Slider> mSliderArray;
	// These HashMaps are used for widgets to find their properties based
	// on their ID.
	private HashMap<Integer,Integer> mButtonIdKeyMap, mEditTextIdKeyMap, mSliderIdKeyMap;
	
	// A hash of all default values
	private HashMap<Integer, Float> mDefaults;

	// Because we're messing with UI stuff, we need a reference to the
	// parent Activity
	private Activity mA;

	// These are all the keys that can be used to describe something in
	// the code. It could be for a widget or a database key.
	// They are used to identify similar and related objects via
	// bit-string flicking.
	public static final int KEY_OXYGEN=1, KEY_HELIUM=2,
							KEY_DESIRED=4, KEY_STARTING=8,
							KEY_PLUS=16, KEY_MINUS=32,
							KEY_PRESSURE=64, KEY_BLEND=128,
							KEY_TOPUP=256, KEY_BEST=512;
	
	public UICore(Activity a, HashMap<Integer,Integer> buttonIdKeyMap, HashMap<Integer,Integer> editTextIdKeyMap, HashMap<Integer,Integer> sliderIdKeyMap, HashMap<Integer,Float> defaults) {
		Iterator<Integer> i;
		int id;
		
		mA=a;

		mEditTextIdKeyMap=editTextIdKeyMap;
		mEditTextArray=new HashMap<Integer,EditText>();
		i=mEditTextIdKeyMap.keySet().iterator();
		while(i.hasNext()) {
			id=i.next();
			mEditTextArray.put(mEditTextIdKeyMap.get(id), (EditText) mA.findViewById(id));
		}
		
		mButtonIdKeyMap=buttonIdKeyMap;
		mPlusMinusButtonArray = new HashMap<Integer,Button>();
		i=mButtonIdKeyMap.keySet().iterator();
		while(i.hasNext()) { 
			id=i.next();
			mPlusMinusButtonArray.put(mButtonIdKeyMap.get(id), (Button) mA.findViewById(id));
		}

		mSliderIdKeyMap=sliderIdKeyMap;
		mSliderArray=new HashMap<Integer,Slider>();
		i=mSliderIdKeyMap.keySet().iterator();
		while(i.hasNext()) {
			id=i.next();
			mSliderArray.put(mSliderIdKeyMap.get(id), (Slider) mA.findViewById(id));
		}
		
		mDefaults=defaults;
		
		// Set all values to defaults
		i=defaults.keySet().iterator();
		while(i.hasNext()) {
			int key = i.next();
			setField(key, defaults.get(key));
			handlePercentUpdate(key, true);
		}
		
		// Set up listeners
		i=mPlusMinusButtonArray.keySet().iterator();
		while(i.hasNext()) { 
			int key=i.next();
			mPlusMinusButtonArray.get(key).setOnClickListener(mPlusMinusButtonListener);
		}
		
		i=mSliderArray.keySet().iterator();
		while(i.hasNext()) { 
			int key=i.next();
			mSliderArray.get(key).setOnPositionChangedListener(mSliderListener);
		}
		
		i=mEditTextArray.keySet().iterator();
		while(i.hasNext()) {
			int key=i.next();
			MyTextWatcher tw = new MyTextWatcher(key);
			mEditTextArray.get(key).addTextChangedListener(tw);
		}
	}
	
	public HashMap<Integer,Float> getDefaults() {
		return mDefaults;
	}
	
	private Slider.OnPositionChangedListener mSliderListener = new Slider.OnPositionChangedListener() {
		public void onPositionChangeCompleted() { }
			
		public void onPositionChanged(Slider slider, int oldPosition, int newPosition) {
			// This method used to do a lot more, but now we only update our
			// EditText value and call handlePercentUpdate to deal with the
			// logic.
			int slider_keys = mSliderIdKeyMap.get(slider.getId());
			setField(slider_keys, newPosition);
			handlePercentUpdate(slider_keys, false);
		}
	};
	
	private Button.OnClickListener mPlusMinusButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			int key = mButtonIdKeyMap.get(v.getId());
			// The EditText field associated with this +/-
			// button has the same tags except for the _PLUS or
			// _MINUS
			int text_key = key & ~(KEY_PLUS | KEY_MINUS);

			// Determine what the increment is for this type
			// of data and button
			int increment = (key & KEY_PRESSURE) != 0? 100: 1;
			increment *= (key & KEY_MINUS) != 0? -1: 1;
			
			setField(text_key, getField(text_key)+increment);
			handlePercentUpdate(text_key, true);
		}
	};
	
	private class MyTextWatcher implements TextWatcher {
		private int mKey;
		
		public MyTextWatcher(int key) {
			mKey=key;
		}

		public void afterTextChanged(Editable s) {
			handlePercentUpdate(mKey, true);
		}

		// These methods are unused
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) { }
		public void onTextChanged(CharSequence s, int start, int before,
				int count) { }
	}
	
	// Get the value of a EditText field as a float according to formatting rules
	public float getField(int key) {
		float current_val;
		NumberFormat nf=(key & KEY_PRESSURE) != 0? Params.mPressure: Params.mMixPercent;
		try {
			current_val=nf.parse(mEditTextArray.get(key).getText().toString()).floatValue();
		} catch(ParseException e) {
			current_val=0;
		}
		return current_val;
	}
	
	// Set the value of a EditText field according to formatting rules.
	// Returns the new value as a String
	private String setField(int key, float value) {
		NumberFormat nf=(key & KEY_PRESSURE) != 0? Params.mPressure: Params.mMixPercent;
		String new_val=nf.format(value);
		mEditTextArray.get(key).setText(new_val);
		return new_val;
	}
	
	// A method used to handle updates to one of our gas percentage
	// fields. It can be called either through a TextWatcher or
	// manually after using setField() on the key.
	// It performs the following operations:
	// - enforces a lower limit on O2 percent
	// - updates the text of the peer if the sum of the values of
	//   itself and the peer exceed 100%
	// - updates the slider's position to match the new text
	//   value if update_slider is true
	private void handlePercentUpdate(int editTextKeys, boolean update_slider) {
		float text_val=getField(editTextKeys);
		
		// Enforce lower limit. For O2, there's a different lower
		// limit than for everything else (which just enforces positive
		// numbers)
		int lower_limit = (editTextKeys & KEY_OXYGEN) != 0? Params.O2_LOWER_LIMIT: 0;
		if(text_val < lower_limit) {
			setField(editTextKeys, lower_limit);
			handlePercentUpdate(editTextKeys, update_slider);
			return;
		}
		
		// If this is a pressure box, none of the rest applies.
		if((editTextKeys & KEY_PRESSURE) != 0) {
			return;
		}
		// For non-pressure texts, couple with its peer. Don't allow
		// the sum of the two to exceed 100%.
		int couple_key;
		if((editTextKeys & KEY_OXYGEN) != 0) {
			couple_key = editTextKeys & ~KEY_OXYGEN | KEY_HELIUM;
		} else {
			couple_key = editTextKeys & ~KEY_HELIUM | KEY_OXYGEN;
		}
		if(couple_key != 0) {
			float peer_val = getField(couple_key);
			if(text_val + peer_val > 100) {
				setField(couple_key, 100-text_val);
				handlePercentUpdate(couple_key, true);
			}
		}

		// Update the slider's position
		if(update_slider) {
			Slider sl = mSliderArray.get(editTextKeys);
			sl.setPosition(Math.round(text_val));
		}
	}
}
