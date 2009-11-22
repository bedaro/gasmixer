package divestoclimb.scuba.equipment;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

public class Info extends Activity {
	
	// The Activity takes two arguments via its Intent:
	// Specify the string resource to use for the title
	public static final String KEY_TITLE = "title";
	// Specify the string resource to use for the body text
	public static final String KEY_TEXT = "text";
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Bundle params = icicle != null? icicle: getIntent().getExtras();
		setTitle(params.getInt(KEY_TITLE));
		setContentView(R.layout.text_dialog);

		TextView text = (TextView)findViewById(R.id.text);

		text.setText(getText(params.getInt(KEY_TEXT)));
		
		// Make links
		Linkify.addLinks(text, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
	}

}