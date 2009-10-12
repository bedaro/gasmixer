package divestoclimb.gasmixer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Generic preferences activity
 * @author Ben Roberts (divestoclimb@gmail.com)
 * [like there's anything to take credit for :)]
 */
public class Settings extends PreferenceActivity {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		addPreferencesFromResource(R.xml.preferences);
	}
}
