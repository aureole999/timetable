package com.aureole.timetable;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;

public class TimetableProvider extends ContentProvider {

    DBHelper databaseHelper;
    SQLiteDatabase db = null;
    private static final UriMatcher sMatcher;
    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI("com.aureole.timetableProvider", "STATION", 1);
        sMatcher.addURI("com.aureole.timetableProvider", "LINE", 2);
        sMatcher.addURI("com.aureole.timetableProvider", "STATIONTIME", 3);
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        if (db != null) {
            return null;
        }
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("STATIONTIME");
        Cursor c = qb.query(db, columns, selection, selectionArgs, null, null, null);
        return c;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }
    
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        // TODO Auto-generated method stub
        return super.bulkInsert(uri, values);
    }
    
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        return super.call(method, arg, extras);
    }
    
    

}
