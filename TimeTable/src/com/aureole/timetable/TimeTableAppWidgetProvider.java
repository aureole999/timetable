package com.aureole.timetable;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
        Log.i("timetable", "Update Widget Start");
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main_widget);
            
            Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            Long id = appWidgetOptions.getLong("1");
            DBHelper dbHelper = new DBHelper(context);
            SQLiteDatabase readableDatabase = dbHelper.getReadableDatabase();
            Calendar today = Calendar.getInstance();
            String yestodayType = "1";
            String todayTime = String.format("%02d:%02d", today.get(Calendar.HOUR_OF_DAY) + 24, today.get(Calendar.MINUTE));
            Cursor query = readableDatabase.query("STATIONTIME", new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {id.toString(), yestodayType, todayTime},null,null,null);
            String nextTime = "";
            int count = query.getCount();
            if (count == 0) {
                query.close();
                String todayType = "1";
                todayTime = String.format("%02d:%02d", today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE));
                Cursor query2 = readableDatabase.query("STATIONTIME", new String[]{"DEPART_TIME"}, "STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[] {id.toString(), todayType, todayTime},null,null,null);
                if (query2.getCount() == 0) {
                    query2.close();
                } else {
                    if (query2.moveToFirst()) {
                        nextTime = query2.getString(0);
                    }
                    query2.close();
                }
            } else {
                if (query.moveToFirst()) {
                    nextTime = query.getString(0);
                }
                query.close();
            }
            readableDatabase.close();
            views.setTextViewText(R.id.widget_station_name, new Date().toString() + "　：　" + nextTime);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}