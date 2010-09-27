package divestoclimb.scuba.equipment.prefs;

import divestoclimb.android.prefs.AbsSyncedPrefsHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class SyncedPrefsHelper extends AbsSyncedPrefsHelper {

	private static final Uri[] PEERS = new Uri[] {
		Uri.parse("content://divestoclimb.gasmixer.preferences")
	};

	public SyncedPrefsHelper(Context context) {
		super(context, PEERS);
	}

	@Override
	protected Intent prepareIntent(SharedPreferences sharedPreferences,
			String key) {
		Intent i = null;
		if(key.equals("units")) {
			i = new Intent();
			i.setAction("divestoclimb.scuba.UNIT_CHANGE");
			i.putExtra("originator", "divestoclimb.scuba.equipment");
			i.putExtra("units", Integer.valueOf(sharedPreferences.getString("units", "0")));
		}
		return i;
	}

}