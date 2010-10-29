package divestoclimb.scuba.equipment.widget;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import divestoclimb.scuba.equipment.R;
import divestoclimb.scuba.equipment.widget.MonthYearDatePicker.OnDateChangedListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class MonthYearDatePickerDialog extends AlertDialog implements OnClickListener,
OnDateChangedListener {

	private static final String YEAR = "year";
	private static final String MONTH = "month";

	private MonthYearDatePicker mDatePicker;
	private OnDateSetListener mCallBack;
	private Calendar mCalendar;
	private java.text.DateFormat mTitleDateFormat;

	private int mInitialYear;
	private int mInitialMonth;

	/**
	 * The callback used to indicate the user is done filling in the date.
	 */
	public interface OnDateSetListener {
		/**
		 * @param view The view associated with this listener.
		 * @param year The year that was set.
		 * @param monthOfYear The month that was set (0-11) for compatibility
		 *  with {@link java.util.Calendar}.
		 */
		void onDateSet(MonthYearDatePicker view, int year, int monthOfYear);
	}

	/**
	 * @param context The context the dialog is to run in.
	 * @param callBack How the parent is notified that the date is set.
	 * @param year The initial year of the dialog.
	 * @param monthOfYear The initial month of the dialog.
	 */
	public MonthYearDatePickerDialog(Context context,
			OnDateSetListener callBack,
			int year,
			int monthOfYear) {
		super(context);
		init(context, callBack, year, monthOfYear);
	}

	/**
	 * @param context The context the dialog is to run in.
	 * @param theme the theme to apply to this dialog
	 * @param callBack How the parent is notified that the date is set.
	 * @param year The initial year of the dialog.
	 * @param monthOfYear The initial month of the dialog.
	 */
	public MonthYearDatePickerDialog(Context context,
			int theme,
			OnDateSetListener callBack,
			int year,
			int monthOfYear) {
		super(context, theme);
		init(context, callBack, year, monthOfYear);
	}

	private void init(Context context, OnDateSetListener callBack, int year, int monthOfYear) {
		mCallBack = callBack;

		mTitleDateFormat = new SimpleDateFormat("MMMM yyyy");
		mCalendar = Calendar.getInstance();

		mInitialYear = year;
		mInitialMonth = monthOfYear;
		updateTitle(mInitialYear, mInitialMonth);

		setButton(context.getText(R.string.date_time_set), this);
		setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);
		setIcon(R.drawable.ic_dialog_time);

		LayoutInflater inflater =
			(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.date_picker_dialog, null);
		setView(view);
		mDatePicker = (MonthYearDatePicker) view.findViewById(R.id.datePicker);
		mDatePicker.init(mInitialYear, mInitialMonth, this);
	}

	/*@Override
    public void show() {
        super.show();

        // Sometimes the full month is displayed causing the title
        // to be very long, in those cases ensure it doesn't wrap to
        // 2 lines (as that looks jumpy) and ensure we ellipsize the end.
        TextView title = (TextView) findViewById(R.id.alertTitle);
        title.setSingleLine();
        title.setEllipsize(TruncateAt.END);
	}*/

	public void onClick(DialogInterface dialog, int which) {
		if (mCallBack != null) {
			mDatePicker.clearFocus();
			mCallBack.onDateSet(mDatePicker, mDatePicker.getYear(), 
					mDatePicker.getMonth());
		}
	}

	public void onDateChanged(MonthYearDatePicker view, int year,
			int month) {
		updateTitle(year, month);
	}

	public void updateDate(int year, int monthOfYear) {
		mInitialYear = year;
		mInitialMonth = monthOfYear;
		mDatePicker.updateDate(year, monthOfYear);
	}

	private void updateTitle(int year, int month) {
		mCalendar.set(Calendar.YEAR, year);
		mCalendar.set(Calendar.MONTH, month);
		setTitle(mTitleDateFormat.format(mCalendar.getTime()));
	}

	@Override
	public Bundle onSaveInstanceState() {
		Bundle state = super.onSaveInstanceState();
		state.putInt(YEAR, mDatePicker.getYear());
		state.putInt(MONTH, mDatePicker.getMonth());
		return state;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int year = savedInstanceState.getInt(YEAR);
		int month = savedInstanceState.getInt(MONTH);
		mDatePicker.init(year, month, this);
		updateTitle(year, month);
	}
}