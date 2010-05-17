package divestoclimb.gasmixer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;

/**
 * A generic activity for displaying information about the application. Looks up
 * the app name and version, and Linkifies the result text.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class About extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_dialog);
		setTitle(R.string.about);

		ComponentName comp = new ComponentName(this, About.class);
		String version;
		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			version = pinfo.versionName;
		} catch(Exception NameNotFoundException) {
			version = "";
		}
		((TextView)findViewById(R.id.text)).setText(String.format(getResources().getString(R.string.about_text),
				getResources().getString(R.string.app_name), version));
	}
}