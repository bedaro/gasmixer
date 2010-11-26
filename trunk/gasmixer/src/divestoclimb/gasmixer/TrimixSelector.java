package divestoclimb.gasmixer;

import divestoclimb.android.util.ViewId;
import divestoclimb.android.widget.BaseNumberSelector;
import divestoclimb.lib.scuba.Mix;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

/**
 * Custom view for selecting a SCUBA mix
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class TrimixSelector extends RelativeLayout
		implements SeekBar.OnSeekBarChangeListener, BaseNumberSelector.ValueChangedListener {

	public static interface MixChangeListener {
		abstract void onChange(TrimixSelector ts, Mix m);
	}

	private LayoutInflater mInflater;
	private SeekBar mO2Bar, mHeBar;
	private BaseNumberSelector mO2Field, mHeField;

	protected final int SELECTOR_LAYOUT = R.layout.trimix_selector;

	private MixChangeListener mMixChangeListener;

	public TrimixSelector(Context context) {
		super(context);
		initTrimixSelector(context);
	}
	public TrimixSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTrimixSelector(context);
		
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.TrimixSelector);
		mHeField.setIncrement(a.getInt(R.styleable.TrimixSelector_heIncrement, 5));
		mO2Field.setLimits(Float.valueOf(a.getInt(R.styleable.TrimixSelector_minO2, 5)), 100f);
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
		// We can't keep the slider ID's because they would conflict with other
		// instances of this class and screw up saving/restoring the SeekBars'
		// states.
		// Instead we have to give each SeekBar a random ID, then save and
		// restore it if the Activity is destroyed and recreated.
		setViewId(mO2Bar, ViewId.generateUnique(getRootView()));
		setViewId(mHeBar, ViewId.generateUnique(getRootView()));

		mO2Field = (BaseNumberSelector)findViewById(R.id.number_o2);
		mHeField = (BaseNumberSelector)findViewById(R.id.number_he);
		// Because the NumberSelectors save state too, ditto for them
		setViewId(mO2Field, ViewId.generateUnique(getRootView()));
		setViewId(mHeField, ViewId.generateUnique(getRootView()));

		mO2Bar.setOnSeekBarChangeListener(this);
		mHeBar.setOnSeekBarChangeListener(this);
		mO2Field.setValueChangedListener(this);
		mHeField.setValueChangedListener(this);
	}
	
	public BaseNumberSelector getO2Field() {
		return mO2Field;
	}
	
	public BaseNumberSelector getHeField() {
		return mHeField;
	}

	/**
	 * SeekBarChangeListener implementation
	 */
	public void onProgressChanged(SeekBar slider, int newPosition, boolean from_user) {
		BaseNumberSelector field;
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
	public void onChange(BaseNumberSelector ns, Float new_val, boolean from_user) {
		SeekBar sb;
		if(ns == mO2Field) {
			sb = mO2Bar;
		} else {
			sb = mHeBar;
		}
		sb.setProgress(Math.round(new_val));
		if(from_user) {
			handlePercentUpdate(ns);
		}
		if(mMixChangeListener != null) {
			Mix m = getMix();
			if(m != null) {
				mMixChangeListener.onChange(this, m);
			}
		}
	}

	private void handlePercentUpdate(BaseNumberSelector field) {
		BaseNumberSelector other_field;
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

	/**
	 * Set the view ID of the given view, and also update any layout parameters of
	 * sibling views that referenced it by its ID.
	 * @param v The view whose ID should be updated
	 * @param id The new ID to assign the view
	 */
	protected void setViewId(View v, int id) {
		int current_id = v.getId();
		v.setId(id);
		for(int i = 0; i < getChildCount(); i++) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getChildAt(i).getLayoutParams();
			int rules[] = params.getRules();
			for(int j = 0; j < rules.length; j++) {
				if(rules[j] == current_id) {
					params.addRule(j, id);
				}
			}
		}
	}

	public static class SavedState extends BaseSavedState {
		int o2SliderId, heSliderId, o2FieldId, heFieldId;

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);

			out.writeInt(o2SliderId);
			out.writeInt(heSliderId);
			out.writeInt(o2FieldId);
			out.writeInt(heFieldId);
		}

		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};

		private SavedState(Parcel in) {
			super(in);
			o2SliderId = in.readInt();
			heSliderId = in.readInt();
			o2FieldId = in.readInt();
			heFieldId = in.readInt();
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		ss.o2SliderId = mO2Bar.getId();
		ss.heSliderId = mHeBar.getId();
		ss.o2FieldId = mO2Field.getId();
		ss.heFieldId = mHeField.getId();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());

		setViewId(mO2Bar, ss.o2SliderId);
		setViewId(mHeBar, ss.heSliderId);
		setViewId(mO2Field, ss.o2FieldId);
		setViewId(mHeField, ss.heFieldId);
	}

}