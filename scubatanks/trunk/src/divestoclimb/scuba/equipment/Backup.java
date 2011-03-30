package divestoclimb.scuba.equipment;

import java.util.Calendar;

import divestoclimb.scuba.equipment.storage.XmlMapper;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Backup extends BroadcastReceiver {

	private static final int WEEKLY_ALARM_CODE = 2, RETRY_ALARM_CODE = 3;
	
	public static final String BROADCAST_ACTION = "divestoclimb.action.PERFORM_BACKUP";
	
	private static final String TAG = "divestoclimb.scuba.equipment.Backup";

	public static void scheduleNext(Context context) {
		Intent i = new Intent();
		i.setAction(BROADCAST_ACTION);
		PendingIntent pi = PendingIntent.getBroadcast(context, WEEKLY_ALARM_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);

		// Compute the time for midnight on the upcoming Sunday
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.add(Calendar.WEEK_OF_YEAR, 1);

		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
	}
	
	private static void scheduleRetry(Context context) {
		Intent i = new Intent();
		i.setAction(BROADCAST_ACTION);
		PendingIntent pi = PendingIntent.getBroadcast(context, RETRY_ALARM_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);

		// Compute the time for midnight tomorrow
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		cal.add(Calendar.DAY_OF_YEAR, 1);

		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			XmlMapper xmlMapper = new XmlMapper(context);
			xmlMapper.writeCylinders(xmlMapper.getDefaultWriter());
		} catch(Exception e) {
			Log.w(TAG, "Backup failed: " + e.toString() + ". Will retry tomorrow.");
			scheduleRetry(context);
		} finally {
			scheduleNext(context);
		}
	}
	
	public static class Scheduler extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Backup.scheduleNext(context);
		}

	}

}