package divestoclimb.gasmixer.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import divestoclimb.android.util.ViewId;
import divestoclimb.android.widget.NumberSelector;
import divestoclimb.android.widget.NumberSelector.ValueChangedListener;
import divestoclimb.gasmixer.R;
import divestoclimb.lib.scuba.Mix;

public class NitroxSelector extends RelativeLayout implements
		OnSeekBarChangeListener, ValueChangedListener {

	public static interface MixChangeListener {
		abstract void onChange(NitroxSelector ns, Mix m);
	}

	private SeekBar mO2Bar;
	private NumberSelector mO2Field;

	protected int SELECTOR_LAYOUT = R.layout.nitrox_selector;

	protected MixChangeListener mMixChangeListener;
	
	public NitroxSelector(Context context) {
		super(context);
		init(context);
	}
	
	public NitroxSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	protected void init(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(SELECTOR_LAYOUT, this);

		mO2Bar = (SeekBar)findViewById(R.id.slider_o2);
		// We can't keep the slider ID's because they would conflict with other
		// instances of this class and screw up saving/restoring the SeekBars'
		// states.
		// Instead we have to give each SeekBar a random ID, then save and
		// restore it if the Activity is destroyed and recreated.
		setViewId(mO2Bar, ViewId.generateUnique(getRootView()));

		mO2Field = (NumberSelector)findViewById(R.id.number_o2);
		// Because the NumberSelectors save state too, ditto for them
		setViewId(mO2Field, ViewId.generateUnique(getRootView()));

		mO2Bar.setOnSeekBarChangeListener(this);
		mO2Field.setValueChangedListener(this);
	}
	
	public NumberSelector getO2Field() {
		return mO2Field;
	}
	
	protected SeekBar getO2Bar() {
		return mO2Bar;
	}
	
	public Mix getMix() {
		Float o2val = mO2Field.getValue();
		if(o2val == null) {
			return null;
		}
		return new Mix((float)(o2val / 100.0), 0);
	}

	public void setMix(Mix m) {
		mO2Field.setValue(m.getO2());
	}

	public void setOnMixChangeListener(MixChangeListener l) {
		mMixChangeListener = l;
	}

	/**
	 * NumberSelector ValueChangeListener implementation
	 */
	@Override
	public void onChange(NumberSelector ns, Float new_val, boolean from_user) {
		if(ns == mO2Field) {
			mO2Bar.setProgress(Math.round(new_val));
		}
		if(mMixChangeListener != null) {
			Mix m = getMix();
			if(m != null) {
				mMixChangeListener.onChange(this, m);
			}
		}
	}

	/**
	 * SeekBarChangeListener implementation
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if(fromUser && seekBar == mO2Bar) {
			mO2Field.setValue(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) { }

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) { }
	
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
		int o2SliderId, o2FieldId;

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);

			out.writeInt(o2SliderId);
			out.writeInt(o2FieldId);
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

		protected SavedState(Parcel in) {
			super(in);
			o2SliderId = in.readInt();
			o2FieldId = in.readInt();
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		ss.o2SliderId = mO2Bar.getId();
		ss.o2FieldId = mO2Field.getId();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());

		setViewId(mO2Bar, ss.o2SliderId);
		setViewId(mO2Field, ss.o2FieldId);
	}

}