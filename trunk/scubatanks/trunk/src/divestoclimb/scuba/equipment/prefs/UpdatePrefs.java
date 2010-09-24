package divestoclimb.scuba.equipment.prefs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdatePrefs extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor e = prefs.edit();
		String action = intent.getAction();
		Bundle b = intent.getExtras();
		if(b.containsKey("originator") && b.getString("originator").equals("divestoclimb.scuba.equipment")) {
			return;
		}
		if(action.equals("divestoclimb.scuba.UNIT_CHANGE")) {
			e.putString("units", Integer.toString(b.getInt("units"))).commit();
		}
	}
}