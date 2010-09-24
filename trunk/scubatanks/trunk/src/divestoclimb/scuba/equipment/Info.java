package divestoclimb.scuba.equipment;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Info extends Activity {
	
	// The Activity takes two arguments via its Intent:
	// Specify the string resource to use for the title
	public static final String KEY_TITLE = "title";
	// Specify the string resource to use for the body text
	public static final String KEY_TEXT = "text";
	
	private int mTextResId, mTitleResId;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Bundle params = icicle != null? icicle: getIntent().getExtras();
		mTitleResId = params.getInt(KEY_TITLE);
		setTitle(mTitleResId);
		setContentView(R.layout.text_dialog);

		TextView text = (TextView)findViewById(R.id.text);

		mTextResId = params.getInt(KEY_TEXT);
		
		text.setText(getText(mTextResId));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_TITLE, mTitleResId);
		outState.putInt(KEY_TEXT, mTextResId);
	}

}