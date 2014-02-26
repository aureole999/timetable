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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Resources;
import android.net.Uri;
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
    

    public GetScheduleTask(Activity act, String stationId, Long id) {
        this.act = act;
        this.aq = new AQuery(act);
        this.stationId = stationId;
        this.id = id;
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
        
        ContentResolver contentResolver = aq.getContext().getContentResolver();
        if (contentResolver == null) {
            progress.dismiss();
            return 1;
        }

        try {
            
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.BEGIN_TRANSACTION, null, null);
            
            if (id == null) {
                // write station info
                ContentValues stationValues = new ContentValues();
                stationValues.put("YAHOO_ID", stationId);
                stationValues.put("NAME", act.getTitle().toString());
                stationValues.put("STATION_NAME", act.getTitle().toString());
                id = ContentUris.parseId(contentResolver.insert(Uri.parse("content://com.aureole.timetableProvider/STATION"), stationValues));
            }
            contentResolver.delete(Uri.parse("content://com.aureole.timetableProvider/LINE"), "STATION_ID = ? ", new String[]{id.toString()});
            contentResolver.delete(Uri.parse("content://com.aureole.timetableProvider/STATIONTIME"), "STATION_ID = ? ", new String[]{id.toString()});
            
            String url = "http://transit.loco.yahoo.co.jp/station/time/%s/?gid=%s&kind=%s";
            String[] kind = new String[] { "1", "2", "4" };
            int count = kind.length * params.length;
            for (int i = 0; i < params.length; i++) {
                
                // write line info
                ContentValues lineValues = new ContentValues();
                lineValues.put("STATION_ID", id);
                lineValues.put("YAHOO_ID", params[i]);
                Long lineId = ContentUris.parseId(contentResolver.insert(Uri.parse("content://com.aureole.timetableProvider/LINE"), lineValues));
                
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
                            
                            ContentUris.parseId(contentResolver.insert(Uri.parse("content://com.aureole.timetableProvider/STATIONTIME"), timeValues));
                        }
                    }
                    
                    publishProgress((int) (((i * 3 + j + 1) / (float) count) * 100));
                }
            }
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.SET_TRANSACTION_SUCCESSFUL, null, null);
            return 0;
        } finally {
            contentResolver.call(Uri.parse("content://com.aureole.timetableProvider"), DBHelper.END_TRANSACTION, null, null);
        }
    }
    
    @Override
    protected void onProgressUpdate(Integer... values) {
        progress.setProgress(values[0]);
    }
    
    @Override
    protected void onPostExecute(Integer result) {
        progress.dismiss();
        act.setResult(Activity.RESULT_OK);
        act.finish();
        super.onPostExecute(result);
    }

}
