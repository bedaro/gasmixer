package divestoclimb.scuba.equipment.storage;

import divestoclimb.scuba.equipment.R;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "ScubaTanks database";

	private static final String DATABASE_NAME = "equipment";
	public static final int DATABASE_VERSION = 1;
	private Context mContext;

	public DatabaseHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = ctx;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		final String[] create_db = mContext.getResources().getStringArray(R.array.db_create);
		for(int i = 0; i < create_db.length; i ++) {
			db.execSQL(create_db[i]);
		}
		populateData(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		int v = oldVersion;
		// First case: database is older than earliest version we can upgrade from
		if(v < 1) {
			Log.w(TAG, "Upgrading database from version " + v + " to 1, which" +
				"will destroy all old data");
			onCreate(db);
			v = 1;
		}
		// If database can be upgraded, check for compatible versions here
		// and do sequential upgrades using the appropriate SQL resources.
	}

	private void populateData(SQLiteDatabase db) {
		final String[] load = mContext.getResources().getStringArray(R.array.db_load);
		for(int i = 0; i < load.length; i ++) {
			db.execSQL(load[i]);
		}
	}
}