package com.aureole.timetable.logics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.database.Cursor;

import com.aureole.timetable.DBHelper;
import com.aureole.timetable.utils.Holiday;

public class TimeTableLogic {
    private DBHelper db;
    
    public TimeTableLogic(DBHelper db) {
        this.db = db;
    }
    
    private String formatToHourMinute(Calendar day, boolean plus24) {
        return String.format(Locale.JAPAN, "%02d:%02d", day.get(Calendar.HOUR_OF_DAY) + (plus24 ? 24 : 0), day.get(Calendar.MINUTE));
    }
    
    private String formatToHourMinute(Calendar day) {
        return formatToHourMinute(day, false);
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
    
    public boolean isPrevDay(String stationId, Calendar day) {
        day = (Calendar) day.clone();
        day.add(Calendar.DATE, -1);
        String yestodayType = getDayType(day);
        String yestodayTime = formatToHourMinute(day, true);
        Cursor rawQuery = db.getReadableDatabase().rawQuery("SELECT COUNT(1) FROM STATIONTIME WHERE STATION_ID = ? AND DAY_TYPE = ? AND DEPART_TIME > ?", new String[]{stationId, yestodayType, yestodayTime});
        int r = 0;
        if (rawQuery.moveToFirst()) {
            r = rawQuery.getInt(0);
        }
        rawQuery.close();
        return r > 0;
    }
    
    public List<String> getNextTrainTime(String stationId, Calendar day) {
        String dayType;
        if (isPrevDay(stationId, day)) {
            day.add(Calendar.DATE, -1);
            dayType = getDayType(day);
        } else {
            dayType = getDayType(day);
        }
        
        Cursor rawQuery = db.getReadableDatabase().rawQuery("SELECT DEPART_TIME FROM STATIONTIME WHERE STATION_ID = ? AND DAY_TYPE = ? ORDER BY DEPART_TIME ASC", new String[]{stationId, dayType});
        
        List<String> r = new ArrayList<String>();
        while (rawQuery.moveToNext()) {
            r.add(rawQuery.getString(0));
        }
        rawQuery.close();
        return r;
        
    }

}
