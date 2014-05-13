package com.aureole.timetable;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.aureole.timetable.logics.TimeTableLogic;

public class TimeTableAppWidgetProvider extends AppWidgetProvider {

    private static DBHelper getDatabaseHelper(Context context) {
        DBHelper dbh = null;
        if (dbh == null) {
            dbh = new DBHelper(context);
        }
        return dbh;
    }
    
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
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 1000, createClockTickIntent(context));
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
 
            Calendar today = Calendar.getInstance();
            
            long nowTimeInMs = today.getTimeInMillis();
            
            int size = prefs.getInt(widgetKey + "size", 0);
            long lastTime = prefs.getLong(widgetKey + "lastTime", 0);
            //if (size == 0 || lastTime == 0 || nowTimeInMs > lastTime) {
                
                // request new time table
                Calendar day = (Calendar) today.clone();
                List<String> timetable = requestTimeTable(context, String.valueOf(id), day);
                
                size = timetable.size();
                Editor editor = prefs.edit();
                editor.putInt(widgetKey + "size", size);
                
                for (int j = 0; j < size; j++) {
                    long time = setDay(day, timetable.get(j)).getTimeInMillis();
                    editor.putLong(widgetKey + "time" + j, time);
                    if (j == size - 1) {
                        editor.putLong(widgetKey + "lastTime", time);
                    }
                }
                editor.apply();

            //}
            
            long[] times = new long[size];
            
            int nextTrainIndex = 0;
            for (int j = 0; j < times.length; j++) {
                times[j] = prefs.getLong(widgetKey + "time" + j, 0);
                if (nowTimeInMs > times[j]) {
                    nextTrainIndex = j + 1;
                }
            }
            String counter = "--";
            
            if (nextTrainIndex < times.length) {
            
                long diff = (times[nextTrainIndex] - nowTimeInMs) / 1000;
                counter = String.format(Locale.JAPAN, "%02d:%02d", diff / 60, diff % 60);
            }
                
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main_widget);
            views.setTextViewText(R.id.widget_station_name, prefs.getString(widgetKey + "name", ""));
            views.setTextViewText(R.id.widget_timer, counter);
        
            appWidgetManager.updateAppWidget(appWidgetId, views);
            // Tell the AppWidgetManager to perform an update on the current app widget
            
        }
    }
    
    private List<String> requestTimeTable(Context context, String id, Calendar day) {
        DBHelper db = getDatabaseHelper(context);
        TimeTableLogic logic = new TimeTableLogic(db);
        return logic.getNextTrainTime(String.valueOf(id), day);
    }
    
    private Calendar setDay(Calendar day, String time) {
        Calendar newDay = (Calendar) day.clone();
        String[] times = time.split(":");
        int hour = Integer.parseInt(times[0]);
        int minute = Integer.parseInt(times[1]);
        if (hour >= 24) {
            newDay.add(Calendar.DATE, 1);
            newDay.set(Calendar.HOUR_OF_DAY, hour-24);
        } else {
            newDay.set(Calendar.HOUR_OF_DAY, hour);
        }
        newDay.set(Calendar.MINUTE, minute);
        newDay.set(Calendar.SECOND, 0);
        newDay.set(Calendar.MILLISECOND, 0);
        Log.i("day", String.valueOf(newDay.getTimeInMillis()));
        return newDay;
    }
}