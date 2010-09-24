package divestoclimb.scuba.equipment.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;

public class SyncedPrefsHelper implements OnSharedPreferenceChangeListener {

	private Context mContext;

	public SyncedPrefsHelper(Context context) {
		mContext = context;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals("units")) {
			Intent i = new Intent();
			i.setAction("divestoclimb.scuba.UNIT_CHANGE");
			i.putExtra("originator", "divestoclimb.scuba.equipment");
			i.putExtra("units", Integer.valueOf(sharedPreferences.getString("units", "0")));
			mContext.sendBroadcast(i);
		}
	}
	
	private static final Uri[] PEERS = new Uri[] {
		Uri.parse("content://divestoclimb.gasmixer.preferences")
	};
	
	/**
	 * Queries all my peer preference ContentProviders for one with a value for
	 * the given key
	 * @param key
	 * @return A Cursor that contains a value for the requested preference from a peer
	 * application, or null if none could be found.
	 */
	public Cursor findSetValue(String key) {
		for(int i = 0; i < PEERS.length; i ++) {
			Uri p = PEERS[i];
			if(mContext.getPackageManager().resolveContentProvider(p.getAuthority(), 0) == null) {
				continue;
			}
			Cursor c = mContext.getContentResolver().query(p, null, null, null, null);
			int index = c.getColumnIndex(key);
			if(index == -1) {
				continue;
			}
			if(! c.isNull(c.getColumnIndexOrThrow(key))) {
				return c;
			}
		}
		return null;
	}

}