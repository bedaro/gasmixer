package divestoclimb.scuba.equipment;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.Units;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * A ListActivity to show a listing of all cylinder sizes.
 * @author BenR
 */
public class CylinderSizes extends ListActivity implements OnClickListener {

	protected Button mButtonNew;
	
	private Units mUnits;

	private ContentResolver mContentResolver;
	protected SharedPreferences mSettings;
	private boolean mReturnSelected = false;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.list_cylinders);
		setTitle(R.string.cylinder_size_list);
		
		mContentResolver = getContentResolver();
		
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);

		String action = getIntent().getAction();
		if(action != null && action.compareTo(Intent.ACTION_GET_CONTENT) == 0) {
			mReturnSelected = true;
			setTitle(R.string.select_cylinder);
		}

		registerForContextMenu(getListView());

		// Set up our button listeners
		mButtonNew = (Button)findViewById(R.id.create_new);
		mButtonNew.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		fillData();
	}
	
	// Override the onPrepareOptionsMenu class to remove the menu
	// item for the units system currently in use.
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_cylinders, menu);
		
		if(mUnits.getCurrentSystem() == Units.IMPERIAL) {
			menu.removeItem(R.id.switch_unit_imperial);
		} else if(mUnits.getCurrentSystem() == Units.METRIC) {
			menu.removeItem(R.id.switch_unit_metric);
		}

		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i;
		SharedPreferences.Editor e;
		switch(item.getItemId()) {
			case R.id.about:
				i = new Intent(this, About.class);
				startActivityForResult(i, 0);
				return true;
			case R.id.switch_unit_metric:
				mUnits.change(Units.METRIC);
				e = mSettings.edit();
				e.putString("units", String.valueOf(Units.METRIC));
				e.commit();
				return true;
			case R.id.switch_unit_imperial:
				mUnits.change(Units.IMPERIAL);
				e = mSettings.edit();
				e.putString("units", String.valueOf(Units.IMPERIAL));
				e.commit();
				return true;			
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_cylinders_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.edit:
			edit(info.id);
			break;
		case R.id.delete:
			mContentResolver.delete(
					Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
							String.valueOf(info.id)
					), null, null);
		}
		return super.onContextItemSelected(item);
	}
	
	private void fillData() {
		Cursor c = managedQuery(CylinderSizeClient.CONTENT_URI, null, null, null, null);
		ListAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				c,
				new String[] { CylinderSizeClient.NAME },
				new int [] { android.R.id.text1 }
		);
		setListAdapter(adapter);
	}
	
	private void edit(long id) {
		Intent i = new Intent(this, CylinderEdit.class);
		i.putExtra(CylinderEdit.KEY_ACTION, CylinderEdit.ACTION_EDIT);
		i.putExtra(CylinderSizeClient._ID, id);
		startActivity(i);
	}

	public void onClick(View v) {
		// Create new
		Intent i = new Intent(this, CylinderEdit.class);
		i.putExtra(CylinderEdit.KEY_ACTION, CylinderEdit.ACTION_NEW);
		startActivity(i);
	}

	/**
	 * onListItemClick is called whenever the user taps on an item or clicks the
	 * trackball while the item is selected.
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if(mReturnSelected) {
			Bundle b = new Bundle();
			b.putLong("selected", id);
			Intent i = new Intent();
			i.putExtras(b);
			setResult(RESULT_OK, i);
			finish();
		} else {
			edit(id);
		}
	}
}