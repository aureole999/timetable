package com.aureole.timetable;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.appwidget.AppWidgetManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;

import com.androidquery.AQuery;


public class DeleteScheduleTask extends AsyncTask<String, Integer, Integer> {

    private Activity act;
    private ProgressDialog progress;
    private DBHelper dbhelper;
    private AQuery aq;

    public DeleteScheduleTask(Activity act, DBHelper db) {
        this.act = act;
        this.aq = new AQuery(act);
        this.dbhelper = db;
    }
    
    @Override
    protected void onPreExecute() {
        Resources res = act.getResources();
        progress = new ProgressDialog(act);
        progress.setTitle(res.getString(R.string.delete_progress_dialog_title));
        progress.setMessage(res.getString(R.string.delete_progress_dialog_message));
        progress.setIndeterminate(false);
        progress.setCancelable(false);
        progress.setMax(100);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(act);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(TimeTableAppWidgetProvider.class.getPackage().getName(), TimeTableAppWidgetProvider.class.getName()));
        for (int i = 0; i < appWidgetIds.length; i++) {
            Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetIds[i]);
            appWidgetOptions.putBoolean("disable", true);
            appWidgetManager.updateAppWidgetOptions(appWidgetIds[i], appWidgetOptions);
        }
        super.onPreExecute();
    }
    @Override
    protected Integer doInBackground(String... params) {
        if (dbhelper == null) {
            progress.dismiss();
            return 1;
        }

        SQLiteDatabase db = dbhelper.getWritableDatabase();
        db.beginTransaction();
        try {
            
            for (int i = 0; i < params.length; i++) {
                db.delete("STATION", "_id = ? ", new String[]{params[i]});
                db.delete("LINE", "STATION_ID = ? ", new String[]{params[i]});
                db.delete("STATIONTIME", "STATION_ID = ? ", new String[]{params[i]});
                publishProgress((int) (((i) / (float) params.length) * 100));
            }
            
            db.setTransactionSuccessful();
            progress.dismiss();
            return 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    
    @Override
    protected void onProgressUpdate(Integer... values) {
        progress.setProgress(values[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Integer result) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(act);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(TimeTableAppWidgetProvider.class.getPackage().getName(), TimeTableAppWidgetProvider.class.getName()));
        for (int i = 0; i < appWidgetIds.length; i++) {
            Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetIds[i]);
            appWidgetOptions.putBoolean("disable", false);
            appWidgetManager.updateAppWidgetOptions(appWidgetIds[i], appWidgetOptions);
        }
        act.getLoaderManager().restartLoader(0, null, (LoaderCallbacks<Cursor>) act);
        aq.id(R.id.myStationListView).getListView().invalidateViews();
    }
}
