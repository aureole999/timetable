package com.aureole.timetable;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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
        Log.i("MYDBACCESS", "delete");
        boolean empty = false;
        if (db == null) {
            db = databaseHelper.getWritableDatabase();
            empty = true;
        }
        int count = 0;
        if (sMatcher.match(uri) == 1) {
            count = db.delete("STATION", "_id = ? ", selectionArgs);
        } else if (sMatcher.match(uri) == 2) {
            count = db.delete("LINE", "STATION_ID = ? ", selectionArgs);
        } else if (sMatcher.match(uri) == 3) {
            count = db.delete("STATIONTIME", "STATION_ID = ? ", selectionArgs);
        }
        if (empty) db = null;
        return count;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i("MYDBACCESS", "insert");
        boolean empty = false;
        if (db == null) {
            db = databaseHelper.getWritableDatabase();
            empty = true;
        }
        long rowId = 0;
        if (sMatcher.match(uri) == 1) {
            rowId = db.insert("STATION", null, values);
        } else if (sMatcher.match(uri) == 2) {
            rowId = db.insert("LINE", null, values);
        } else if (sMatcher.match(uri) == 3) {
            rowId = db.insert("STATIONTIME", null, values);
        }
        if (empty) db = null;
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            Log.i("MYDBACCESS", "insert end");
            return noteUri;
        }
        throw new IllegalArgumentException("Unknown URI" + uri);
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        Log.i("MYDBACCESS", "query");
        if (db != null) {
            return null;
        }
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("STATIONTIME");
        Cursor c = qb.query(db, columns, selection, selectionArgs, null, null, null);
        Log.i("MYDBACCESS", "query end");
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
        if ("beginTransaction".equals(method)) {
            if (db == null) {
                db = databaseHelper.getWritableDatabase();
            }
            db.beginTransaction();
        } else if ("endTransaction".equals(method)) {
            if (db != null) {
                db.endTransaction();
                db = null;
            }
        } else if ("setTransactionSuccessful".equals(method)) {
            if (db != null) {
                db.setTransactionSuccessful();
                db.endTransaction();
                db = null;
            }
        }
        return super.call(method, arg, extras);
    }
    
    

}
