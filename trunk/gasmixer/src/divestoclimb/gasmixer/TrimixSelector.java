package divestoclimb.gasmixer;

import divestoclimb.android.util.ViewId;
import divestoclimb.android.widget.NumberSelector;
import divestoclimb.lib.scuba.Mix;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * Custom view for selecting a SCUBA mix
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class TrimixSelector extends NitroxSelector {

	private SeekBar mHeBar;
	private NumberSelector mHeField;

	public TrimixSelector(Context context) {
		super(context);
	}
	public TrimixSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.TrimixSelector);
		mHeField.setIncrement(a.getInt(R.styleable.TrimixSelector_heIncrement, 5));
		getO2Field().setLimits(Float.valueOf(a.getInt(R.styleable.TrimixSelector_minO2, 5)), 100f);
	}

	@Override
	public Mix getMix() {
		Float o2val = getO2Field().getValue(), heval = mHeField.getValue();
		if(o2val == null || heval == null) {
			return null;
		}
		return new Mix((float)(o2val / 100.0), (float)(heval / 100.0));
	}

	@Override
	public void setMix(Mix m) {
		super.setMix(m);
		mHeField.setValue(m.getHe());
	}

	@Override
	protected void init(Context context) {
		SELECTOR_LAYOUT = R.layout.trimix_selector;
		super.init(context);

		mHeBar = (SeekBar)findViewById(R.id.slider_he);
		// We can't keep the slider ID's because they would conflict with other
		// instances of this class and screw up saving/restoring the SeekBars'
		// states.
		// Instead we have to give each SeekBar a random ID, then save and
		// restore it if the Activity is destroyed and recreated.
		setViewId(mHeBar, ViewId.generateUnique(getRootView()));


		mHeField = (NumberSelector)findViewById(R.id.number_he);
		// Because the NumberSelectors save state too, ditto for them
		setViewId(mHeField, ViewId.generateUnique(getRootView()));

		mHeBar.setOnSeekBarChangeListener(this);
		mHeField.setValueChangedListener(this);
	}
	
	public NumberSelector getHeField() {
		return mHeField;
	}

	/**
	 * SeekBarChangeListener implementation
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		super.onProgressChanged(seekBar, progress, fromUser);
		NumberSelector field;
		if(seekBar == mHeBar) {
			field = mHeField;
			field.setValue(progress);
		} else {
			field = getO2Field();
		}
		if(fromUser) {
			handlePercentUpdate(field);
		}
	}

	/**
	 * NumberSelector ValueChangeListener implementation
	 */
	@Override
	public void onChange(NumberSelector ns, Float new_val, boolean from_user) {
		if(ns == mHeField) {
			mHeBar.setProgress(Math.round(new_val));
		}
		if(from_user) {
			handlePercentUpdate(ns);
		}
		super.onChange(ns, new_val, from_user);
	}

	private void handlePercentUpdate(NumberSelector field) {
		NumberSelector other_field = field == mHeField? getO2Field(): mHeField;
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

	public static class SavedState extends BaseSavedState {
		int heSliderId, heFieldId;

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);

			out.writeInt(heSliderId);
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
			heSliderId = in.readInt();
			heFieldId = in.readInt();
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		ss.heSliderId = mHeBar.getId();
		ss.heFieldId = mHeField.getId();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());

		setViewId(mHeBar, ss.heSliderId);
		setViewId(mHeField, ss.heFieldId);
	}

}