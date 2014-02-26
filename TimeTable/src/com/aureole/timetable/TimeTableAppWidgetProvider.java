package com.aureole.timetable;

import java.util.Calendar;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class TimeTableAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("com.aureole.timetable.APPWIDGET_UPDATE_SECOND".equals(intent.getAction())) {
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, ids);
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i("timetable", "Widget Provider Enabled");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 1);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 10000, createClockTickIntent(context));
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createClockTickIntent(context));
    }
    
    private PendingIntent createClockTickIntent(Context context) {
        Intent intent = new Intent("com.aureole.timetable.APPWIDGET_UPDATE_SECOND");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
    
    private String getDayType(Calendar cal) {
        if (Holiday.queryHoliday(cal.getTime()) != null) {
            return "4";
        } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return "4";
        } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return "2";
        } else {
            return "1";
        }
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String widgetKey = "widget-" + appWidgetId + "-";
            long id = prefs.getLong(widgetKey + "id", 0);
            if (id == 0) {
                continue;
            }
            long[] times = new long[3];
            for (int j = 0; j < times.length; j++) {
                times[j] = prefs.getLong(widgetKey + "time" + j, 0);
            }
            
            Calendar today = Calendar.getInstance();
            
            long nowTimeInMs = today.getTimeInMillis();
            // get next trains time
            if (nowTimeInMs >= times[0]) {
            
                Calendar yestoday = Calendar.getInstance();
                yestoday.add(Calendar.DATE, -1);
                String yestodayType = getDayType(yestoday);
                String todayTime = String.format(Locale.JAPAN, "%02d:%02d", today.get(Calendar.HOUR_OF_DAY) + 24, today.get(Calendar.MINUTE));
                
                ContentResolver contentResolver = context.getContentResolver();
                Cursor query = contentResolver.query(Uri.parse("content://com.aureole.timetableProvider"), new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {String.valueOf(id), yestodayType, todayTime}, null);
                if (query == null) {
                    continue;
                }
                //Cursor query = readableDatabase.query("STATIONTIME", new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {String.valueOf(id), yestodayType, todayTime},null,null,null);
                
                String nextTime = "";
                int count = query.getCount();
                if (count == 0) {
                    query.close();
                    String todayType = getDayType(today);
                    todayTime = String.format(Locale.JAPAN, "%02d:%02d", today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE));
                    
                    Cursor query2 = contentResolver.query(Uri.parse("content://com.aureole.timetableProvider"), new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {String.valueOf(id), todayType, todayTime}, null);
                    // Cursor query2 = readableDatabase.query("STATIONTIME", new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {String.valueOf(id), todayType, todayTime},null,null,null);
                    if (query2.getCount() == 0) {
                        query2.close();
                    } else {
                        for (int j = 0; j < times.length; j++) {
                            if (query2.moveToNext()) {
                                nextTime = query2.getString(0);
                                today.set(Calendar.HOUR_OF_DAY, Integer.parseInt(nextTime.split(":")[0]));
                                today.set(Calendar.MINUTE, Integer.parseInt(nextTime.split(":")[1]));
                                today.set(Calendar.SECOND, 0);
                                today.set(Calendar.MILLISECOND, 0);
                                times[j] = today.getTimeInMillis();
                            }
                        }
                        query2.close();
                    }
                } else {
                    for (int j = 0; j < times.length; j++) {
                        if (query.moveToNext()) {
                            nextTime = query.getString(0);
                            today.set(Calendar.HOUR_OF_DAY, Integer.parseInt(nextTime.split(":")[0])-24);
                            today.set(Calendar.MINUTE, Integer.parseInt(nextTime.split(":")[1]));
                            today.set(Calendar.SECOND, 0);
                            today.set(Calendar.MILLISECOND, 0);
                            times[j] = today.getTimeInMillis();
                        }
                    }
                    query.close();
                }
                Editor editor = prefs.edit();
                for (int j = 0; j < times.length; j++) {
                    editor.putLong(widgetKey + j, times[j]);
                }
                editor.commit();
            }
            
            long diff = (times[0] - nowTimeInMs) / 1000;
            String counter = String.format(Locale.JAPAN, "%02d:%02d", diff / 60, diff % 60);
            
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main_widget);
            views.setTextViewText(R.id.widget_station_name, prefs.getString(widgetKey + "name", ""));
            views.setTextViewText(R.id.widget_timer, counter);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}