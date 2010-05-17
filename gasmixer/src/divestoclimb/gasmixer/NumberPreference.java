package divestoclimb.gasmixer;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import divestoclimb.android.widget.BaseNumberPreference;

public class NumberPreference extends BaseNumberPreference {

	public NumberPreference(Context context) { super(context, mReader); }
	public NumberPreference(Context context, AttributeSet attrs) { super(context, mReader, attrs); }
	public NumberPreference(Context context, AttributeSet attrs, int defStyle) { super(context, mReader, attrs, defStyle); }

	private static BaseNumberPreference.ResourceReader mReader = new ResourceReader();

	public static class ResourceReader extends NumberSelector.ResourceReader implements BaseNumberPreference.ResourceReader {

		public TypedArray getNPStyledAttributes(Context context, AttributeSet attrs) {
			return context.obtainStyledAttributes(attrs,
					R.styleable.NumberPreference);
			}

		public String readNPUnitLabel(TypedArray a) { return a.getString(R.styleable.NumberPreference_unitLabel); }
	}

}