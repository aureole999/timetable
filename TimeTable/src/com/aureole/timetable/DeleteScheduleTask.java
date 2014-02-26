package com.aureole.timetable;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.androidquery.AQuery;


public class DeleteScheduleTask extends AsyncTask<String, Integer, Integer> {

    private Activity act;
    private ProgressDialog progress;
    private AQuery aq;

    public DeleteScheduleTask(Activity act, DBHelper db) {
        this.act = act;
        this.aq = new AQuery(act);
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
        super.onPreExecute();
    }
    @Override
    protected Integer doInBackground(String... params) {
        ContentResolver contentResolver = aq.getContext().getContentResolver();
        if (contentResolver == null) {
            progress.dismiss();
            return 1;
        }

        try {
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.BEGIN_TRANSACTION, null, null);
            for (int i = 0; i < params.length; i++) {
                contentResolver.delete(Uri.parse("content://com.aureole.timetableProvider/STATION"), "_id = ? ", new String[]{params[i]});
                contentResolver.delete(Uri.parse("content://com.aureole.timetableProvider/LINE"), "STATION_ID = ? ", new String[]{params[i]});
                contentResolver.delete(Uri.parse("content://com.aureole.timetableProvider/STATIONTIME"), "STATION_ID = ? ", new String[]{params[i]});
                publishProgress((int) (((i) / (float) params.length) * 100));
            }
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.SET_TRANSACTION_SUCCESSFUL, null, null);
            progress.dismiss();
            return 0;
        } finally {
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.END_TRANSACTION, null, null);
        }
    }
    
    @Override
    protected void onProgressUpdate(Integer... values) {
        progress.setProgress(values[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Integer result) {
        act.getLoaderManager().restartLoader(0, null, (LoaderCallbacks<Cursor>) act);
        aq.id(R.id.myStationListView).getListView().invalidateViews();
    }
}
