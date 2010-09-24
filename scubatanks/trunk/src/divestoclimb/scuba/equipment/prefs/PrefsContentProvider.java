package divestoclimb.scuba.equipment.prefs;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import divestoclimb.android.content.SharedPreferencesContentProvider;

public class PrefsContentProvider extends SharedPreferencesContentProvider {

	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.equipment.preferences");

	@Override
	protected SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.dir/vnd.divestoclimb.gasmixer.preferences";
	}

	@Override
	public boolean onCreate() {
		return true;
	}

}