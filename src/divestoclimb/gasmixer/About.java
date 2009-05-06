package divestoclimb.gasmixer;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

public class About extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		setTitle(R.string.about);
		TextView about_text = (TextView)findViewById(R.id.about_text);
		Linkify.addLinks(about_text, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
	}

}
