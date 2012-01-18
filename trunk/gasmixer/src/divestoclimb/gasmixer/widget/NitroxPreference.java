package divestoclimb.gasmixer.widget;

import divestoclimb.lib.scuba.Mix;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A preference for selecting a Nitrox mix. The result is stored as a float.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class NitroxPreference extends DialogPreference implements MixPreference {
	private NitroxSelector mNitroxSelector;
	
	private Mix mMix;

	public NitroxPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mNitroxSelector = new NitroxSelector(context, attrs);
	}
	
	public NitroxPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public NitroxPreference(Context context) {
		this(context, null);
	}
	
	public void setMix(float fo2) {
		mMix = new Mix((double)fo2 / 100, 0);
		persistFloat(fo2);
	}
	
	public Mix getMix() {
		return mMix;
	}
	
	@Override
	protected View onCreateDialogView() {
		return mNitroxSelector;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mNitroxSelector.setMix(mMix);

	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if(positiveResult) {
			float newFo2 = mNitroxSelector.getMix().getO2();
			if(callChangeListener(newFo2)) {
				setMix(newFo2);
			}
		}
		((ViewGroup)mNitroxSelector.getParent()).removeView(mNitroxSelector);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getFloat(index, 0.21f);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setMix(restoreValue? getPersistedFloat(0.21f): (Float)defaultValue);
	}

	public NitroxSelector getSelector() {
		return mNitroxSelector;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.fo2 = mMix.getO2();
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		setMix(myState.fo2);
	}

	private static class SavedState extends BaseSavedState {
		float fo2;

		public SavedState(Parcel source) {
			super(source);
			fo2 = source.readFloat();
        }

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeFloat(fo2);
        }

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}