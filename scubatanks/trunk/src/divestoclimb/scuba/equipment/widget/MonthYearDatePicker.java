package divestoclimb.scuba.equipment.widget;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import divestoclimb.scuba.equipment.R;
import divestoclimb.scuba.equipment.widget.NumberPicker.OnChangedListener;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * A DatePicker that only allows picking month and year, no day. Unfortunately
 * the Android DatePicker makes heavy use of internal API's and was not designed
 * to make subclassing easy, so the whole thing had to be copied and modified from
 * AOSP. 
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class MonthYearDatePicker extends FrameLayout {

	private static final int DEFAULT_START_YEAR = 1900;
	private static final int DEFAULT_END_YEAR = 2100;

	/* UI Components */
	private final NumberPicker mMonthPicker;
	private final NumberPicker mYearPicker;

	/**
	 * How we notify users the date has changed.
	 */
	private OnDateChangedListener mOnDateChangedListener;

	private int mMonth;
	private int mYear;

	/**
	 * The callback used to indicate the user changes the date.
	 */
	public interface OnDateChangedListener {

		/**
		 * @param view The view associated with this listener.
		 * @param year The year that was set.
		 * @param monthOfYear The month that was set (0-11) for compatibility
		 *  with {@link java.util.Calendar}.
		 */
		void onDateChanged(MonthYearDatePicker view, int year, int monthOfYear);
	}

	public MonthYearDatePicker(Context context) {
		this(context, null);
	}

	public MonthYearDatePicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MonthYearDatePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.date_picker, this, true);

		mMonthPicker = (NumberPicker) findViewById(R.id.month);
		mMonthPicker.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
		DateFormatSymbols dfs = new DateFormatSymbols();
		String[] months = dfs.getShortMonths();
		mMonthPicker.setRange(1, 12, months);
		mMonthPicker.setSpeed(200);
		mMonthPicker.setOnChangeListener(new OnChangedListener() {
			public void onChanged(NumberPicker picker, int oldVal, int newVal) {

				/* We display the month 1-12 but store it 0-11 so always
				 * subtract by one to ensure our internal state is always 0-11
				 */
				mMonth = newVal - 1;
				if (mOnDateChangedListener != null) {
					mOnDateChangedListener.onDateChanged(MonthYearDatePicker.this, mYear, mMonth);
				}
			}
		});
		mYearPicker = (NumberPicker) findViewById(R.id.year);
		mYearPicker.setSpeed(100);
		mYearPicker.setOnChangeListener(new OnChangedListener() {
			public void onChanged(NumberPicker picker, int oldVal, int newVal) {
				mYear = newVal;
				if (mOnDateChangedListener != null) {
					mOnDateChangedListener.onDateChanged(MonthYearDatePicker.this, mYear, mMonth);
				}
			}
		});

		// attributes
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DatePicker);

		int mStartYear = a.getInt(R.styleable.DatePicker_startYear, DEFAULT_START_YEAR);
		int mEndYear = a.getInt(R.styleable.DatePicker_endYear, DEFAULT_END_YEAR);
		mYearPicker.setRange(mStartYear, mEndYear);

		a.recycle();

		// initialize to current date
		Calendar cal = Calendar.getInstance();
		init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), null);

		// re-order the number pickers to match the current date format
		reorderPickers(months);

		if (!isEnabled()) {
			setEnabled(false);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mMonthPicker.setEnabled(enabled);
		mYearPicker.setEnabled(enabled);
	}

	private void reorderPickers(String[] months) {
		java.text.DateFormat format;
		String order;

		/*
		 * If the user is in a locale where the medium date format is
		 * still numeric (Japanese and Czech, for example), respect
		 * the date format order setting.  Otherwise, use the order
		 * that the locale says is appropriate for a spelled-out date.
		 */

		if (months[0].startsWith("1")) {
			format = DateFormat.getDateFormat(getContext());
		} else {
			format = DateFormat.getMediumDateFormat(getContext());
		}

		if (format instanceof SimpleDateFormat) {
			order = ((SimpleDateFormat) format).toPattern();
		} else {
			// Shouldn't happen, but just in case.
			order = new String(DateFormat.getDateFormatOrder(getContext()));
		}
		/* Remove the 3 pickers from their parent and then add them back in the
		 * required order.
		 */
		LinearLayout parent = (LinearLayout) findViewById(R.id.parent);
		parent.removeAllViews();

		boolean quoted = false;
		boolean didMonth = false, didYear = false;

		for (int i = 0; i < order.length(); i++) {
			char c = order.charAt(i);

			if (c == '\'') {
				quoted = !quoted;
			}

			if (!quoted) {
				if ((c == DateFormat.MONTH || c == 'L') && !didMonth) {
					parent.addView(mMonthPicker);
					didMonth = true;
				} else if (c == DateFormat.YEAR && !didYear) {
					parent.addView (mYearPicker);
					didYear = true;
				}
			}
		}

		// Shouldn't happen, but just in case.
		if (!didMonth) {
			parent.addView(mMonthPicker);
		}
		if (!didYear) {
			parent.addView(mYearPicker);
		}
	}

	public void updateDate(int year, int monthOfYear) {
		mYear = year;
		mMonth = monthOfYear;
		updateSpinners();
		reorderPickers(new DateFormatSymbols().getShortMonths());
	}

	private static class SavedState extends BaseSavedState {

		private final int mYear;
		private final int mMonth;

		/**
		 * Constructor called from {@link DatePicker#onSaveInstanceState()}
		 */
		private SavedState(Parcelable superState, int year, int month) {
			super(superState);
			mYear = year;
			mMonth = month;
		}

		/**
		 * Constructor called from {@link #CREATOR}
		 */
		private SavedState(Parcel in) {
			super(in);
			mYear = in.readInt();
			mMonth = in.readInt();
		}

		public int getYear() {
			return mYear;
		}

		public int getMonth() {
			return mMonth;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(mYear);
			dest.writeInt(mMonth);
		}

		public static final Parcelable.Creator<SavedState> CREATOR =
			new Creator<SavedState>() {

			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	/**
	 * Override so we are in complete control of save / restore for this widget.
	 */
	@Override
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
		dispatchThawSelfOnly(container);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		return new SavedState(superState, mYear, mMonth);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		mYear = ss.getYear();
		mMonth = ss.getMonth();
	}

	/**
	 * Initialize the state.
	 * @param year The initial year.
	 * @param monthOfYear The initial month.
	 * @param dayOfMonth The initial day of the month.
	 * @param onDateChangedListener How user is notified date is changed by user, can be null.
	 */
	public void init(int year, int monthOfYear,
			OnDateChangedListener onDateChangedListener) {
		mYear = year;
		mMonth = monthOfYear;
		mOnDateChangedListener = onDateChangedListener;
		updateSpinners();
	}

	private void updateSpinners() {
		mYearPicker.setCurrent(mYear);

		/* The month display uses 1-12 but our internal state stores it
		 * 0-11 so add one when setting the display.
		 */
		mMonthPicker.setCurrent(mMonth + 1);
	}

	public int getYear() {
		return mYear;
	}

	public int getMonth() {
		return mMonth;
	}
}