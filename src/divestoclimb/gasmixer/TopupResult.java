package divestoclimb.gasmixer;

import java.text.NumberFormat;

import divestoclimb.lib.scuba.Mix;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class TopupResult extends Activity {
	
	private float pi, pf, fo2i, fhei, fo2t, fhet, fo2f, fhef;
	
	private NumberFormat nf = NumberFormat.getIntegerInstance();
	
	private GasMixDbAdapter mDbAdapter;
	
	private String mResultText;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.topup_result);
		
		// get our parameters
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			// TODO: handle this
			// This may happen if our app gets restarted from this
			// screen. In that case, we should go back to the
			// GasMixer Activity.
			finish();
		}
		
		pi=extras.getFloat("PARAM_"+nf.format(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_PRESSURE));
		pf=extras.getFloat("PARAM_"+nf.format(UICore.KEY_TOPUP | UICore.KEY_DESIRED | UICore.KEY_PRESSURE));
		fo2i=extras.getFloat("PARAM_"+nf.format(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_OXYGEN))/100;
		fhei=extras.getFloat("PARAM_"+nf.format(UICore.KEY_TOPUP | UICore.KEY_STARTING | UICore.KEY_HELIUM))/100;
		
		mDbAdapter = new GasMixDbAdapter(this);
		mDbAdapter.open();
		
		fo2t=mDbAdapter.fetchSetting("fo2t");
		fhet=mDbAdapter.fetchSetting("fhet");
		
		solve();
		
		Mix topup = new Mix(fo2t, fhet);
		Mix result = new Mix(fo2f, fhef);
		Resources r = getResources();
		mResultText = String.format(r.getString(R.string.topup_result), Params.mixFriendlyName(result, this));
		
		TextView resultView = (TextView) findViewById(R.id.topup_result);
		resultView.setText(mResultText);
		TextView reminderView = (TextView) findViewById(R.id.topup_reminders);
		reminderView.setText(
				String.format(r.getString(R.string.topup_reminder), Params.mixFriendlyName(topup, this))+
				"\n"+
				r.getString(R.string.analyze_warning));
		
		Button close = (Button) findViewById(R.id.button_close);
		close.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void solve() {
		fo2f = (pi*fo2i+(pf-pi)*fo2t)/pf;
		fhef = (pi*fhei+(pf-pi)*fhet)/pf;
	}
}
