package divestoclimb.scuba.equipment.storage;

import java.io.IOException;

import divestoclimb.android.database.AbsDatabaseHelper;
import divestoclimb.scuba.equipment.R;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends AbsDatabaseHelper {
	private static final String TAG = "ScubaTanks database";

	private static final String DATABASE_NAME = "equipment";
	public static final int DATABASE_VERSION = 2;
	private Context mContext;

	public DatabaseHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = ctx;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		execStatements(db, R.array.db_create);
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
		if(v < 2) {
			execStatements(db, R.array.db_upgrade_1_2);
			v = 2;
		}
	}

	private void populateData(SQLiteDatabase db) {
		// Try to restore from a backup. If this fails, populate with default data instead.
		try {
			// We can't use the typical CylinderORMapper because that goes through the
			// content provider. Doing so would create a recursive call to getReadableDatabase,
			// so we have to use a special ORMapper that directly accesses the database for
			// reads and updates.
			XmlMapper xmlMapper = new XmlMapper(new DirectCylinderORMapper(mContext, db));
			xmlMapper.readCylinders(xmlMapper.getDefaultReader());
			Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_SHORT).show();
		} catch(IOException e) {
			execStatements(db, R.array.db_load);
		}
	}
}