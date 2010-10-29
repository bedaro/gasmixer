package divestoclimb.scuba.equipment;

import java.io.File;

import divestoclimb.scuba.equipment.prefs.Settings;
import divestoclimb.scuba.equipment.prefs.SyncedPrefsHelper;
import divestoclimb.android.widget.ObjectMappedCursorAdapter;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;
import divestoclimb.scuba.equipment.storage.XmlMapper;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

/**
 * A ListActivity to show a listing of all cylinder sizes.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class CylinderSizes extends TabActivity implements View.OnClickListener,
		ObjectMappedCursorAdapter.ViewBinder<Cylinder>, AdapterView.OnItemClickListener {
	
	private Units mUnits;

	private CylinderORMapper mORMapper;
	private boolean mInitialized = false;
	private SyncedPrefsHelper mSyncedPrefsHelper;
	protected SharedPreferences mSettings;
	private ListView mGenericList, mSpecificList;
	private View mContextMenuList;	// The view the current context menu is for
	private boolean mReturnSelected = false;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.list_cylinders);
		setTitle(R.string.cylinder_list);
		
		// In case this is the first run of the app, schedule our broadcasts
		// with the AlarmManager
		CheckCylinders.scheduleNext(this);
		Backup.scheduleNext(this);
		
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mSyncedPrefsHelper = new SyncedPrefsHelper(this);
		Cylinder.setDefHydroInterval(Math.round(mSettings.getFloat("hydro_interval_years", 5)));
		Cylinder.setDefVisualInterval(Math.round(mSettings.getFloat("visual_interval_months", 12)));

		int unit;
		if(mSettings.contains("units")) {
			// Android issue 2096 - ListPreference won't work with an integer
			// array for values. Unit values are being stored as Strings then
			// we convert them here for use.
			unit = Integer.valueOf(mSettings.getString("units", "0"));
		} else {
			Cursor c = mSyncedPrefsHelper.findSetValue("units");
			unit = c == null? 0: Integer.valueOf(c.getString(c.getColumnIndexOrThrow("units")));
			mSettings.edit().putString("units", Integer.toString(unit)).commit();
		}
		mUnits = new Units(unit);
		
		mORMapper = new CylinderORMapper(this, mUnits);
		
		final TabHost tabhost = getTabHost();
		final Resources res = getResources();
		tabhost.addTab(tabhost.newTabSpec("tab1")
				.setIndicator(getString(R.string.generic), res.getDrawable(R.drawable.tanks_generic))
				.setContent(R.id.tab_generic)
		);
		tabhost.addTab(tabhost.newTabSpec("tab2")
				.setIndicator(getString(R.string.specific), res.getDrawable(R.drawable.tanks_specific))
				.setContent(R.id.tab_specific)
		);

		String action = getIntent().getAction();
		if(action != null && action.equals(Intent.ACTION_GET_CONTENT)) {
			mReturnSelected = true;
			setTitle(R.string.select_cylinder);
		}

		mGenericList = (ListView)findViewById(R.id.list_generic);
		mSpecificList = (ListView)findViewById(R.id.list_specific);
		registerForContextMenu(mGenericList);
		registerForContextMenu(mSpecificList);
		mGenericList.setOnItemClickListener(this);
		mSpecificList.setOnItemClickListener(this);
		mSpecificList.setEmptyView(findViewById(R.id.no_specific));

		// Set up our button listeners
		findViewById(R.id.create_new_generic).setOnClickListener(this);
		findViewById(R.id.create_new_specific).setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(! mInitialized) {
			Cursor generic = mORMapper.fetchCylinders(Cylinder.TYPE_GENERIC);
			startManagingCursor(generic);
			mGenericList.setAdapter(new SimpleCursorAdapter(this,
					android.R.layout.simple_list_item_1,
					generic,
					new String[] { CylinderORMapper.NAME },
					new int [] { android.R.id.text1 }
			));
			
			Cursor specific = mORMapper.fetchCylinders(Cylinder.TYPE_SPECIFIC);
			startManagingCursor(specific);
			mSpecificList.setAdapter(new ObjectMappedCursorAdapter<Cylinder>(this,
					R.layout.specific_cyl_list_item,
					specific,
					new int[] { android.R.id.text1, R.id.serialNumber, R.id.statusIcon, R.id.statusText },
					mORMapper, this
			));
			if(specific.getCount() > 0) {
				// The user has specific cylinders defined,
				// so switch to that tab by default
				getTabHost().setCurrentTab(1);
			}
			mInitialized = true;
		} else {
			((CursorAdapter)mGenericList.getAdapter()).getCursor().requery();
			((CursorAdapter)mSpecificList.getAdapter()).getCursor().requery();
		}
		mUnits.change(Integer.valueOf(mSettings.getString("units", "0")));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.list_cylinders, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		final Intent i;
		final XmlMapper xmlMapper;
		switch(item.getItemId()) {
			case R.id.about:
				i = new Intent(this, About.class);
				startActivity(i);
				return true;
			case R.id.settings:
				i = new Intent(this, Settings.class);
				startActivity(i);
				return true;
			/*case R.id.test_notification:
				i = new Intent();
				i.setAction(CheckCylinders.BROADCAST_ACTION);
				sendBroadcast(i);
				return true;*/
			case R.id.backup:
				try {
					xmlMapper = new XmlMapper(this);
					xmlMapper.writeCylinders(xmlMapper.getDefaultWriter());
					Toast.makeText(this, String.format(getString(R.string.backup_successful),
							new File(XmlMapper.DEFAULT_PATH, XmlMapper.DEFAULT_FILENAME).getCanonicalPath()),
							Toast.LENGTH_SHORT).show();
				} catch(Exception e) {
					Toast.makeText(this, String.format(getString(R.string.backup_error), e.toString()), Toast.LENGTH_LONG).show();
				}
				return true;
			case R.id.restore:
				try {
					xmlMapper = new XmlMapper(this);
					xmlMapper.readCylinders(xmlMapper.getDefaultReader());
					Toast.makeText(this, String.format(getString(R.string.restore_successful),
							new File(XmlMapper.DEFAULT_PATH, XmlMapper.DEFAULT_FILENAME).getCanonicalPath()),
							Toast.LENGTH_SHORT).show();
				} catch(Exception e) {
					//Toast.makeText(this, String.format(getString(R.string.restore_error), e.toString()), Toast.LENGTH_LONG).show();
					throw(new RuntimeException(e));
				}
				return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		mContextMenuList = v;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_cylinders_context, menu);
		if(v == mSpecificList) {
			MenuItem item = menu.findItem(R.id.copy);
			item.setVisible(false).setEnabled(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.edit:
			if(mContextMenuList == mGenericList) {
				editGeneric(info.id);
			} else {
				editSpecific(info.id);
			}
			break;
		case R.id.delete:
			mORMapper.deleteCylinder(info.id);
			break;
		case R.id.copy:
			editSpecific(info.id);
		}
		return super.onContextItemSelected(item);
	}
	
	private void editGeneric(long id) {
		final Intent i = new Intent(this, CylinderSizeEdit.class);
		i.putExtra(CylinderSizeEdit.KEY_ACTION, CylinderSizeEdit.ACTION_EDIT);
		i.putExtra(CylinderORMapper._ID, id);
		startActivity(i);
	}
	
	private void editSpecific(long id) {
		final Intent i = new Intent(this, CylinderEdit.class);
		i.putExtra(CylinderORMapper._ID, id);
		startActivity(i);
	}

	@Override
	public void onClick(View v) {
		// Create new
		final Intent i = new Intent(this, CylinderSizeEdit.class);
		i.putExtra(CylinderSizeEdit.KEY_ACTION, CylinderSizeEdit.ACTION_NEW);
		i.putExtra(CylinderSizeEdit.KEY_NEWTYPE, v.getId() == R.id.create_new_generic? Cylinder.TYPE_GENERIC: Cylinder.TYPE_SPECIFIC);
		startActivity(i);
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		if(mReturnSelected) {
			Bundle b = new Bundle();
			b.putLong("selected", id);
			final Intent i = new Intent();
			i.putExtras(b);
			setResult(RESULT_OK, i);
			finish();
		} else if(l == mGenericList) {
			editGeneric(id);
		} else {
			editSpecific(id);
		}
	}

	@Override
	public void setViewValue(View view, Cylinder obj) {
		switch(view.getId()) {
		case android.R.id.text1:
			((TextView)view).setText(obj.getName());
			break;
		case R.id.serialNumber:
			((TextView)view).setText(obj.getSerialNumber());
			break;
		case R.id.statusIcon:
			ImageView iv = (ImageView)view;
			if(obj.isHydroExpired() || obj.isVisualExpired()) {
				iv.setImageResource(R.drawable.tank_expired);
			} else if(obj.doesHydroExpireThisMonth() || obj.doesVisualExpireThisMonth()) {
				iv.setImageResource(R.drawable.tank_needsinsp);
			} else {
				iv.setImageResource(R.drawable.tank);
			}
			break;
		case R.id.statusText:
			TextView tv = (TextView)view;
			tv.setVisibility(View.VISIBLE);
			if(obj.isHydroExpired()) {
				tv.setText(R.string.hydro_expired);
			} else if(obj.isVisualExpired()) {
				tv.setText(R.string.visual_expired);
			} else if(obj.doesHydroExpireThisMonth()) {
				tv.setText(R.string.needs_hydro);
			} else if(obj.doesVisualExpireThisMonth()) {
				tv.setText(R.string.needs_visual);
			} else {
				tv.setVisibility(View.INVISIBLE);
			}
		}
	}
}