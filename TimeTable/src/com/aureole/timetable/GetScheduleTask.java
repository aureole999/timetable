package com.aureole.timetable;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;


public class GetScheduleTask extends AsyncTask<String, Integer, Integer> {

    private AQuery aq;
    private Activity act;
    private ProgressDialog progress;
    private String stationId;
    private Long id;
    private DBHelper dbhelper;
    

    public GetScheduleTask(Activity act, String stationId, Long id, DBHelper db) {
        this.act = act;
        this.aq = new AQuery(act);
        this.stationId = stationId;
        this.id = id;
        this.dbhelper = db;
    }
    
    
    @Override
    protected void onPreExecute() {
        Resources res = act.getResources();
        progress = new ProgressDialog(act);
        progress.setTitle(res.getString(R.string.progress_dialog_title));
        progress.setMessage(String.format(res.getString(R.string.progress_dialog_message), act.getTitle()));
        progress.setIndeterminate(false);
        progress.setCancelable(true);
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
            if (id == null) {
                // write station info
                ContentValues stationValues = new ContentValues();
                stationValues.put("YAHOO_ID", stationId);
                stationValues.put("NAME", act.getTitle().toString());
                stationValues.put("STATION_NAME", act.getTitle().toString());
                id = db.insert("STATION", null, stationValues);
            }
            db.delete("LINE", "STATION_ID = ? ", new String[]{id.toString()});
            db.delete("STATIONTIME", "STATION_ID = ? ", new String[]{id.toString()});
            
            String url = "http://transit.loco.yahoo.co.jp/station/time/%s/?gid=%s&kind=%s";
            String[] kind = new String[] { "1", "2", "4" };
            int count = kind.length * params.length;
            for (int i = 0; i < params.length; i++) {
                
                // write line info
                ContentValues lineValues = new ContentValues();
                lineValues.put("STATION_ID", id);
                lineValues.put("YAHOO_ID", params[i]);
                Long lineId = db.insert("LINE", null, lineValues);
                
                for (int j = 0; j < kind.length; j++) {
                    AjaxCallback<String> cb = new AjaxCallback<String>();
                    cb.url(String.format(url, stationId, params[i], kind[j])).type(String.class);
                    aq.sync(cb);
                    String html = cb.getResult();
                    Document document = Jsoup.parse(html);
                    
                    // get train class and station for
                    Elements trainType = document.select("#timeNotice1");
                    Elements trainFor = document.select("#timeNotice2");
                    Map<String, String> trainClassMap = new HashMap<String, String>(); 
                    Map<String, String> stationForMap = new HashMap<String, String>(); 
                    for (Element e : trainType.select("dd > dl")) {
                        trainClassMap.put(e.select("dt").text().replace("：", ""), e.select("dd").text());
                    }
                    for (Element e : trainFor.select("dd > dl")) {
                        stationForMap.put(e.select("dt").text().replace("：", ""), e.select("dd").text());
                    }
                    
                    Elements tr_hours = document.select("#timetable > table > tbody > tr");
                    for (Element tr_hour : tr_hours) {
                        String hour = tr_hour.id();
                        Elements dl_minutes = tr_hour.select("td.col2 dl");
                        for (Element dl_minute : dl_minutes) {
                            String minute = dl_minute.select("dt").text();
                            String trnCls = dl_minute.select(".trn-cls").text();
                            if (StringUtil.isBlank(trnCls)) {
                                trnCls = "無印";
                            }
                            trnCls = trnCls.replaceAll("[\\[\\]]", "");
                            String staFor = dl_minute.select(".sta-for").text();
                            if (StringUtil.isBlank(staFor)) {
                                staFor = "無印";
                            }
                            // write time info
                            ContentValues timeValues = new ContentValues();
                            timeValues.put("STATION_ID", id);
                            timeValues.put("LINE_ID", lineId);
                            timeValues.put("DAY_TYPE", kind[j]);
                            timeValues.put("DEPART_TIME", String.format("%02d:%02d", Integer.parseInt(hour), Integer.parseInt(minute.replace("◆", ""))));
                            timeValues.put("TRAIN_CLASS", trainClassMap.get(trnCls));
                            timeValues.put("STATION_FOR", stationForMap.get(staFor));
                            timeValues.put("SPECIAL", minute.indexOf("◆")>=0?"◆":"");
                            
                            db.insert("STATIONTIME", null, timeValues);
                        }
                    }
                    
                    publishProgress((int) (((i * 3 + j + 1) / (float) count) * 100));
                }
            }
            db.setTransactionSuccessful();
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
    
    @Override
    protected void onPostExecute(Integer result) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(act);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(TimeTableAppWidgetProvider.class.getPackage().getName(), TimeTableAppWidgetProvider.class.getName()));
        for (int i = 0; i < appWidgetIds.length; i++) {
            Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetIds[i]);
            appWidgetOptions.putBoolean("disable", false);
            appWidgetManager.updateAppWidgetOptions(appWidgetIds[i], appWidgetOptions);
        }
        progress.dismiss();
        act.finish();
        super.onPostExecute(result);
    }

}
