package divestoclimb.gasmixer;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import divestoclimb.android.widget.BaseNumberSelector;

public class NumberSelector extends BaseNumberSelector {

	private static final int LAYOUT = R.layout.num_selector;

	public NumberSelector(Context context) { super(context, mReader); }
	public NumberSelector(Context context, AttributeSet attrs) { super(context, mReader, attrs); }

	private static BaseNumberSelector.ResourceReader mReader = new ResourceReader();

	public static class ResourceReader implements BaseNumberSelector.ResourceReader {
		public int getNSLayout() { return LAYOUT; }
		public int getNSEditTextId() { return R.id.text1; }
		public int getNSMinusButtonId() { return R.id.minus; }
		public int getNSPlusButtonId() { return R.id.plus; }

		public TypedArray getNSStyledAttributes(Context context, AttributeSet attrs) {
			return context.obtainStyledAttributes(attrs,
					R.styleable.NumberSelector);
		}

		public int readNSDecimalPlaces(TypedArray a, int defaultValue) {
			return a.getInt(R.styleable.NumberSelector_decimalplaces, defaultValue);
		}

		public float readNSIncrement(TypedArray a, float defaultValue) {
			return a.getFloat(R.styleable.NumberSelector_increment, defaultValue);
		}

		public float readNSLowerLimit(TypedArray a, float defaultValue) {
			return a.getFloat(R.styleable.NumberSelector_lowerlimit, defaultValue);
		}

		public Float readNSUpperLimit(TypedArray a, Float defaultValue) {
			if(a.hasValue(R.styleable.NumberSelector_upperlimit)) {
				return a.getFloat(R.styleable.NumberSelector_upperlimit, 0);
			} else {
				return defaultValue;
			}
		}

		public int readNSTextBoxWidth(TypedArray a, int defaultValue) {
			return a.getDimensionPixelSize(R.styleable.NumberSelector_textboxwidth, defaultValue);
		}
	};
}