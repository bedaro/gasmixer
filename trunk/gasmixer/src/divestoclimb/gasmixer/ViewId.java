package divestoclimb.gasmixer;

import java.util.Random;

import android.view.View;

public class ViewId {

	public static int generateUnique(View v) {
		Random r = new Random();
		
		int id;
		do {
			id = r.nextInt();
		} while(id <= 0 || v.findViewById(id) != null);
		return id;
	}
}
