package divestoclimb.gasmixer;

import divestoclimb.lib.scuba.Mix;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

public class TrimixSelector extends RelativeLayout implements SeekBar.OnSeekBarChangeListener, NumberSelector.ValueChangedListener {
	
	public static interface MixChangeListener {
		abstract void onChange(TrimixSelector ts, Mix m);
	}
	
	private LayoutInflater mInflater;
	private SeekBar mO2Bar, mHeBar;
	private NumberSelector mO2Field, mHeField;
	
	protected final int SELECTOR_LAYOUT = R.layout.trimix_selector;
	
	private MixChangeListener mMixChangeListener;

	public TrimixSelector(Context context) {
		super(context);
		initTrimixSelector(context);
	}
	public TrimixSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTrimixSelector(context);
	}

	public Mix getMix() {
		Float o2val = mO2Field.getValue(), heval = mHeField.getValue();
		if(o2val == null || heval == null) {
			return null;
		}
		return new Mix((float)(o2val / 100.0), (float)(heval / 100.0));
	}
	
	public void setMix(Mix m) {
		mO2Field.setValue(m.getO2());
		mHeField.setValue(m.getHe());
	}
	
	public void setOnMixChangeListener(MixChangeListener l) {
		mMixChangeListener = l;
	}

	private void initTrimixSelector(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mInflater.inflate(SELECTOR_LAYOUT, this);

		mO2Bar = (SeekBar)findViewById(R.id.slider_o2);
		mHeBar = (SeekBar)findViewById(R.id.slider_he);
		mO2Field = (NumberSelector)findViewById(R.id.number_o2);
		mHeField = (NumberSelector)findViewById(R.id.number_he);

		mO2Bar.setOnSeekBarChangeListener(this);
		mHeBar.setOnSeekBarChangeListener(this);
		mO2Field.setValueChangedListener(this);
		mHeField.setValueChangedListener(this);
	}

	/**
	 * SeekBarChangeListener implementation
	 */
	public void onProgressChanged(SeekBar slider, int newPosition, boolean from_user) {
		NumberSelector field;
		if(slider == mO2Bar) {
			field = mO2Field;
		} else {
			field = mHeField;
		}
		if(from_user) {
			field.setValue(newPosition);
			handlePercentUpdate(field);
		}
	}

	public void onStartTrackingTouch(SeekBar arg0) { }
	public void onStopTrackingTouch(SeekBar arg0) { }

	/**
	 * NumberSelector ValueChangeListener implementation
	 */
	public void onChange(NumberSelector ns, Float new_val) {
		SeekBar sb;
		if(ns == mO2Field) {
			sb = mO2Bar;
		} else {
			sb = mHeBar;
		}
		sb.setProgress(Math.round(new_val));
		if(mMixChangeListener != null) {
			Mix m = getMix();
			if(m != null) {
				mMixChangeListener.onChange(this, m);
			}
		}
	}

	private void handlePercentUpdate(NumberSelector field) {
		NumberSelector other_field;
		if(field == mO2Field) {
			other_field = mHeField;
		} else {
			other_field = mO2Field;
		}
		float field_value = field.getValue();
		if(field_value + other_field.getValue() > 100) {
			other_field.setValue(100 - field_value);
			if(other_field.getValue() > 100 - field_value) {
				// The other field must have run into a limiting
				// constraint with its value. As an alternative,
				// set our value to keep the total at or below 100.
				field.setValue(100 - other_field.getValue());
			}
		}
	}

}
