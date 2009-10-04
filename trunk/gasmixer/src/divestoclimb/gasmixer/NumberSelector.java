package divestoclimb.gasmixer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * A custom component that shows an EditText View surrounded by plus and minus buttons.
 * @author BenR
 *
 */
public class NumberSelector extends LinearLayout implements Button.OnClickListener, TextWatcher {

	public static interface ValueChangedListener {
		/**
		 * Called when the value of the number in the NumberSelector changes
		 * @param ns The NumberSelector containing the number that changed 
		 * @param new_val The new value
		 */
		abstract void onChange(NumberSelector ns, Float new_val);
	}

	private ImageButton mMinusButton, mPlusButton;
	private EditText mEditText;

	protected final int SELECTOR_LAYOUT=R.layout.num_selector;
	
	private Float mIncrement, mLowerLimit, mUpperLimit, mCachedValue;
	private int mDecimalPlaces;
	private ValueChangedListener mValueChangedListener;
	private NumberFormat mNumberFormat;

	public NumberSelector(Context context) {
		super(context);
		initNumberSelector(context);
	}

	public NumberSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		initNumberSelector(context);
		TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NumberSelector);
		
		int textWidth = a.getDimensionPixelSize(R.styleable.NumberSelector_textboxwidth, 0);
		if(textWidth > 0) {
			mEditText.setWidth(textWidth);
		}
		
		mIncrement = a.getFloat(R.styleable.NumberSelector_increment, 1);
		mLowerLimit = a.getFloat(R.styleable.NumberSelector_lowerlimit, 0);
		if(a.hasValue(R.styleable.NumberSelector_upperlimit)) {
			mUpperLimit = a.getFloat(R.styleable.NumberSelector_upperlimit, 0);
		} else {
			mUpperLimit = null;
		}
		setDecimalPlaces(a.getInt(R.styleable.NumberSelector_decimalplaces, 0));
		
		a.recycle();
	}

	/**
	 * Set the value of the text field
	 * @param value The value to set
	 */
	public void setValue(float value) {
		mEditText.setText(mNumberFormat.format(value));
	}

	/**
	 * Get the text field's current value
	 * @return The current value of the field, or null if the value is invalid for the
	 * current constraints
	 */
	public Float getValue() {
		try {
			return mNumberFormat.parse(mEditText.getText().toString()).floatValue();
		} catch(ParseException e) {
			return null;
		}
	}

	public void setValueChangedListener(ValueChangedListener l) {
		mValueChangedListener = l;
	}

	/**
	 * Set the amount to increment the value by when the plus or minus button is pressed
	 * @param increment The amount to increment
	 */
	public void setIncrement(float increment) {
		mIncrement = increment;
	}

	/**
	 * Set the number of decimal places the text field allows.
	 * @param places The number of places to allow.
	 */
	public void setDecimalPlaces(int places) {
		mDecimalPlaces = places;
		if(places == 0) {
			mNumberFormat = NumberFormat.getIntegerInstance();
			mEditText.setKeyListener(DigitsKeyListener.getInstance(false, false));
		} else {
			mNumberFormat = new DecimalFormat();
			mNumberFormat.setMaximumFractionDigits(places);
			mNumberFormat.setMinimumFractionDigits(places);
			mEditText.setKeyListener(DigitsKeyListener.getInstance(false, true));
		}
	}

	/**
	 * Set the upper and lower limit for values in the text field.
	 * @param lower_limit The lower limit (or null for no limit)
	 * @param upper_limit The upper limit (or null for no limit)
	 */
	public void setLimits(Float lower_limit, Float upper_limit) {
		mLowerLimit = lower_limit;
		mUpperLimit = upper_limit;
	}

	protected void initNumberSelector(Context context) {
		LayoutInflater i = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		i.inflate(SELECTOR_LAYOUT, this);

		mPlusButton = (ImageButton)findViewById(R.id.plus);
		mMinusButton = (ImageButton)findViewById(R.id.minus);
		mEditText = (EditText)findViewById(R.id.text1);
		// We can't keep the text1 ID because it would conflict with other instances
		// of this class and screw up saving/restoring the EditText's state.
		// Instead we have to give the EditText a random ID, then save and
		// restore it if the Activity is destroyed and recreated.
		setEditTextId(ViewId.generateUnique(getRootView()));

		mPlusButton.setOnClickListener(this);
		mMinusButton.setOnClickListener(this);
		mEditText.addTextChangedListener(this);
	}

	protected void setEditTextId(int id) {
		mEditText.setId(id);
	}

	public static class SavedState extends BaseSavedState {
		int textId;
		
		SavedState(Parcelable superState) {
			super(superState);
		}
		
		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			
			out.writeInt(textId);
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
			textId = in.readInt();
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		ss.textId = mEditText.getId();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());

		setEditTextId(ss.textId);
	}

	public void onClick(View v) {
		Float increment = mIncrement;
		if(v.getId() == R.id.minus) {
			increment *= -1;
		}

		Float current_val = getValue();
		if(current_val == null) {
			current_val = new Float(0);
		}
		// Compute new_val, rounding to the nearest multiple of
		// increment.
		Float new_val = (float)(Math.round(current_val / increment) * increment);
		if(mDecimalPlaces > 0) {
			// This accounts for floating point arithmetic errors by only
			// looking at the necessary number of decimal places for comparison
			// with current_val
			new_val = (float)(Math.round(new_val * Math.pow(10, mDecimalPlaces)) / Math.pow(10, mDecimalPlaces));
		}
		if((new_val - current_val) * increment <= 0) {
			// If we got here, it means one of these things:
			// - new_val == current_val
			// - new_val > current_val && increment < 0
			// - new_val < current_val && increment > 0
			// In the last two cases, increment is needed because the
			// rounding by itself did not change current_val in the
			// direction the user wanted it to change.
			new_val += increment;
		}
		if(mUpperLimit != null) {
			new_val = Math.min(new_val, mUpperLimit);
		}
		if(mLowerLimit != null) {
			new_val = Math.max(new_val, mLowerLimit);
		}
		setValue(new_val);
	}

	/**
	 * Our TextWatcher for the text field. Enforces upper and lower limits on the field,
	 * ensures the new value is parseable, and calls the ValueChangedListener if one is
	 * defined.
	 * @param s The new value of the text field
	 */
	public void afterTextChanged(Editable s) {
		// This check prevents a parseException from being thrown if
		// the user removes all text from the field.
		if(s.length() > 0) {
			try {
				Float newValue = mNumberFormat.parse(s.toString()).floatValue();
				Float validValue = newValue;
				if(mUpperLimit != null) {
					validValue = Math.min(validValue, mUpperLimit);
				}
				if(mLowerLimit != null) {
					validValue = Math.max(validValue, mLowerLimit);
				}
				if(newValue.compareTo(validValue) != 0) {
					setValue(validValue);
				} else {
					mCachedValue = validValue;
					if(mValueChangedListener != null) {
						mValueChangedListener.onChange(this, validValue);
					}
				}
			} catch(ParseException e) {
				setValue(mCachedValue);
			}
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) { }

	public void onTextChanged(CharSequence s, int start, int before,
			int count) { }

}
