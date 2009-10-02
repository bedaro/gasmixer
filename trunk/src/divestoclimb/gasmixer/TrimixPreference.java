package divestoclimb.gasmixer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Mix;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class TrimixPreference extends DialogPreference {
	private TrimixSelector mTrimixSelector;
	
	private Mix mMix;
	private String mMixString;
	
	public TrimixPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mTrimixSelector = new TrimixSelector(context, attrs);
	}
	
	public TrimixPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public TrimixPreference(Context context) {
		this(context, null);
	}
	
	private String mixToString(Mix mix) {
		NumberFormat nf = new DecimalFormat(".###");
		return nf.format(mix.getfO2())+" "+nf.format(mix.getfHe());
	}
	
	public static Mix stringToMix(String s) {
		String ss[] = s.split("\\s");
		NumberFormat nf = new DecimalFormat(".###");
		try {
			return new Mix(nf.parse(ss[0]).floatValue(), nf.parse(ss[1]).floatValue());
		} catch(ParseException e) { return null; }
	}
	
	public void setMix(String mixString) {

		mMix = stringToMix(mixString);
		mMixString = mixString;
		
		persistString(mixString);

	}

	public Mix getMix() {
		return mMix;
	}
	
	@Override
	protected View onCreateDialogView() {
		return mTrimixSelector;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mTrimixSelector.setMix(getMix());

	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if(positiveResult) {
			String mixString = mixToString(mTrimixSelector.getMix());
			if(callChangeListener(mixString)) {
				setMix(mixString);
			}
		}
		((ViewGroup)mTrimixSelector.getParent()).removeView(mTrimixSelector);
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setMix(restoreValue? getPersistedString(mMixString): (String)defaultValue);
	}
	
	public TrimixSelector getTrimixSelector() {
		return mTrimixSelector;
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.mixString = mixToString(getMix());
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
		setMix(myState.mixString);
	}
	
	private static class SavedState extends BaseSavedState {
		String mixString;

		public SavedState(Parcel source) {
			super(source);
			mixString = source.readString();
        }

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeString(mixString);
        }

		public SavedState(Parcelable superState) {
			super(superState);
		}

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
