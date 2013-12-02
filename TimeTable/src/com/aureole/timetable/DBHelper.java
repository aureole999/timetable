package com.aureole.timetable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "timetabledb", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE STATION ( " +
        		"ID INTEGER PRIMARY KEY AUTOINCREMENT" +
        		", YAHOO_ID TEXT" +
        		", NAME TEXT" +
        		", STATION_NAME TEXT" +
        		", POSITION_X REAL" +
        		", POSITION_Y REAL" +
        		", COLOR INTEGER" +
        		" ) ");
        db.execSQL("CREATE TABLE LINE ( " +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", STATION_ID INTEGER" +
                ", YAHOO_ID TEXT" +
                ", NAME TEXT" +
                ", FOR TEXT" +
                ", FILTER INTEGER" +
                " ) ");
        db.execSQL("CREATE TABLE STATIONTIME ( " +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", STATION_ID INTEGER" +
                ", LINE_ID INTEGER" +
                ", DAYTYPE INTEGER" +
                ", DEPARTTIME TEXT" +
                ", TRAINTYPE TEXT" +
                ", TRAINFOR TEXT" +
                ", SPECIAL TEXT" +
                " ) ");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int prevVer, int currVer) {

    }

}
