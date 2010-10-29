package divestoclimb.scuba.equipment;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import divestoclimb.android.database.CursorSet;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.scuba.equipment.storage.CylinderORMapper;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

/**
 * Broadcast receiver that checks for any cylinders which are due for a hydro or visual
 * inspection in the current month. Creates a status bar notification containing its
 * findings, if any matching cylinders were found
 */
public class CheckCylinders extends BroadcastReceiver {

	private static final int NOTIFICATION_ID = 1;

	private static final int MONTHLY_ALARM_CODE = 1;

	public static final String BROADCAST_ACTION = "divestoclimb.scuba.equipment.CHECK_CYLINDERS";

	/**
	 * Ensures the next monthly check is scheduled with the system
	 * @param context
	 */
	public static void scheduleNext(Context context) {
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		if(! settings.getBoolean("notifications", true)) {
			return;
		}
		Intent i = new Intent();
		i.setAction(BROADCAST_ACTION);
		PendingIntent pi = PendingIntent.getBroadcast(context, MONTHLY_ALARM_CODE, i, 0);

		// Compute the time for the morning of the first day of next month
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 0, 8, 0, 0);
		cal.add(Calendar.MONTH, 1);

		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		if(! settings.getBoolean("notifications", true)) {
			return;
		}
		try {
			if(intent.getAction().equals("android.intent.action.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE")) {
				// All we need to do is reschedule the alarm
				return;
			}
			final int hydroIntervalYears = settings.getInt("hydro_interval_years", 5),
				visualIntervalMonths = settings.getInt("visual_interval_months", 12);
			Cylinder.setDefHydroInterval(hydroIntervalYears);
			Cylinder.setDefVisualInterval(visualIntervalMonths);
			final CylinderORMapper orMapper = new CylinderORMapper(context);
			final ContentResolver r = context.getContentResolver();

			// Keep count of the number of cylinders requiring hydros and visuals as well as
			// a record of the ID's of each. This prevents double-counting a cylinder which
			// needs a hydro as well as a visual, since the visual inspection is implied in
			// a hydro.
			HashSet<Long> expired = new HashSet<Long>();
			int expiringHydros = 0, expiringVisuals = 0;
			String lastHydroName = null, lastVisualName = null;
			// bypass ORMapper methods since we're doing highly specific queries. This is
			// a choice favoring performance.
			final Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - hydroIntervalYears);
			// First query: cylinders which do not have overridden hydro expiration intervals.
			// This is a simple SQL query, all results are expired and get added to the set.
			Cursor c = r.query(CylinderORMapper.CONTENT_URI, null,
					"ifNull(" + CylinderORMapper.HYDRO_INTERVAL_YEARS + ",-1)=-1 and " +
					CylinderORMapper.LAST_HYDRO + "=?",
					new String[] { orMapper.formatDate(cal.getTime()) }, null);
			for(Cylinder cyl : new CursorSet<Cylinder>(c, orMapper)) {
				expired.add(cyl.getId());
				lastHydroName = cyl.getName();
				expiringHydros ++;
			}
			// Second query: cylinders with overridden hydro expiration intervals. We have to
			// load these cylinders into objects to test if they expire or not.
			c = r.query(CylinderORMapper.CONTENT_URI, null,
					"ifNull(" + CylinderORMapper.HYDRO_INTERVAL_YEARS + ",-1)!=-1", null, null);
			for(Cylinder cyl : new CursorSet<Cylinder>(c, orMapper)) {
				if(cyl.doesHydroExpireThisMonth()) {
					expired.add(cyl.getId());
					lastHydroName = cyl.getName();
					expiringHydros ++;
				}
			}

			cal.setTime(new Date());
			cal.add(Calendar.MONTH, -1 * visualIntervalMonths);

			// First visual query: cylinders which do not have overridden visual expiration
			// intervals.
			// The results are added to the expired map if they are not already present
			c = r.query(CylinderORMapper.CONTENT_URI, null,
					"ifNull(" + CylinderORMapper.VISUAL_INTERVAL_MONTHS + ", -1)=-1 and " +
					CylinderORMapper.LAST_VISUAL + "=?",
					new String[] { orMapper.formatDate(cal.getTime()) }, null);
			for(Cylinder cyl : new CursorSet<Cylinder>(c, orMapper)) {
				if(expired.add(cyl.getId())) {
					lastVisualName = cyl.getName();
					expiringVisuals ++;
				}
			}
			// Second visual query: cylinders which have overridden visual expiration intervals.
			// Test each one individually
			c = r.query(CylinderORMapper.CONTENT_URI, null,
					"ifNull(" + CylinderORMapper.VISUAL_INTERVAL_MONTHS + ", -1)!=-1", null, null);
			for(Cylinder cyl : new CursorSet<Cylinder>(c, orMapper)) {
				if(! expired.contains(cyl.getId()) && cyl.doesVisualExpireThisMonth()) {
					expired.add(cyl.getId());
					lastVisualName = cyl.getName();
					expiringVisuals ++;
				}
			}
			
			if(expiringHydros + expiringVisuals == 0) {
				return;
			}

			// Rules for notification text:
			// - ticker shows generic message if more than one cylinder needs work, otherwise
			//   it's the same as contentText
			// - contentText shows the number of required hydros and visuals for the month.
			//   if only one of either category is needed, the name of that cylinder is shown
			//   instead of the number
			// - If no cylinders require a hydro, only required visuals are shown. The reverse
			//   is also true regarding showing hydros when no visuals are required (so 
			//   "0 need hydro" is an impossible case)
			String tickerText = null, contentTitle = context.getString(R.string.notification_title),
				contentText = null;
			if(expiringHydros > 0) {
				if(expiringHydros == 1) {
					contentText = String.format(context.getString(R.string.one_needs_hydro), lastHydroName);
					tickerText = contentText;
				} else {
					contentText = String.format(context.getString(R.string.multiple_need_hydro), expiringHydros);
					tickerText = contentTitle;
				}
			}

			if(expiringVisuals > 0) {
				String t;
				if(expiringVisuals == 1) {
					t = String.format(context.getString(R.string.one_needs_visual), lastVisualName);
					if(tickerText == null) {
						tickerText = t;
					} else {
						tickerText = contentTitle;
					}
				} else {
					t = String.format(context.getString(R.string.multiple_need_visual), expiringVisuals);
					tickerText = contentTitle;
				}
				if(contentText != null) {
					contentText = String.format(context.getString(R.string.hydro_and_visual), contentText, t);
				} else {
					contentText = t;
				}
			}
			// Now we can send the notification
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationMgr = (NotificationManager)context.getSystemService(ns);

			final Notification n = new Notification(R.drawable.status_icon, tickerText, System.currentTimeMillis());
			n.flags |= Notification.FLAG_AUTO_CANCEL;
			// TODO allow customizable notification sound?
			n.defaults |= Notification.DEFAULT_SOUND;
			final Intent nIntent = new Intent(context, CylinderSizes.class);
			final PendingIntent cIntent = PendingIntent.getActivity(context, 0, nIntent, 0);
			n.setLatestEventInfo(context, contentTitle, contentText, cIntent);
			notificationMgr.notify(NOTIFICATION_ID, n);

		} catch(RuntimeException e) {
			throw(e);
		} finally {
			scheduleNext(context);
		}
	}

}