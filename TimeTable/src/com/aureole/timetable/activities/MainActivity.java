package com.aureole.timetable.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.aureole.timetable.DBHelper;
import com.aureole.timetable.R;
import com.aureole.timetable.tasks.DeleteScheduleTask;
import com.commonsware.cwac.loaderex.SQLiteCursorLoader;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnItemLongClickListener {

    private static DBHelper db;
    private final AQuery aq = new AQuery(this);
    private SimpleCursorAdapter adapter;
    private ActionMode mActionMode;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DBHelper(getApplicationContext());
        
        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_activated_1, null, new String[]{"NAME"}, new int[]{android.R.id.text1}, 0);
        aq.id(R.id.myStationListView).adapter(adapter).itemClicked(this).getListView().setOnItemLongClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId  = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // 駅検索画面に遷移
        case R.id.action_newTimer:
            Intent intent = new Intent(this, NewTimerActivity.class);
            startActivityForResult(intent, 1);
            break;

        default:
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
        case 0:
            return new SQLiteCursorLoader(this, db, "SELECT * FROM STATION", new String[]{});
        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        adapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }
    
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.main_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    ListView listView = aq.id(R.id.myStationListView).getListView();
                    List<String> params = new ArrayList<String>();
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        if (listView.getChildAt(i).isActivated()) {
                            params.add(Long.toString(adapter.getItemId(i)));
                        }
                    }
                    
                    new DeleteScheduleTask(MainActivity.this, db).execute(params.toArray(new String[params.size()]));
                    mActionMode.finish();

                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            ListView listView = aq.id(R.id.myStationListView).getListView();
            for (int i = 0; i < listView.getChildCount(); i++) {
                listView.getChildAt(i).setActivated(false);
            }
        }
    };


    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (mActionMode != null) {
            return false;
        }

        mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
        arg1.setActivated(true);
        return true;
    }


    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (mActionMode != null) {
            arg1.setActivated(!arg1.isActivated());
        }
        
        // add widget setting
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            
            // set filter
            SQLiteDatabase readableDatabase = db.getReadableDatabase();
            Cursor curStationFor = readableDatabase.rawQuery("SELECT DISTINCT * FROM (SELECT 0 AS _id, 1 AS CHECK_FLAG, STATION_FOR AS FILTER_NAME FROM STATIONTIME WHERE STATION_ID = ? ) FLT", new String[]{Long.toString(arg3)});
            Cursor curTrainClass = readableDatabase.rawQuery("SELECT DISTINCT * FROM (SELECT 1 AS _id, 1 AS CHECK_FLAG, TRAIN_CLASS AS FILTER_NAME FROM STATIONTIME WHERE STATION_ID = ? ) FLT", new String[]{Long.toString(arg3)});
            AlertDialog.Builder builder = new AlertDialog.Builder(aq.getContext());
            builder.setCancelable(false);
            // 
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.main_widget);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Editor editor = prefs.edit();
            String widgetKey = "widget-" + mAppWidgetId + "-";
            editor.putLong(widgetKey + "id", arg3);
            editor.putString(widgetKey + "name", ((TextView)arg1).getText().toString());
            editor.commit();
            
            appWidgetManager.updateAppWidget(mAppWidgetId, views);
            
            if (curStationFor.getCount() < 2 && curTrainClass.getCount() < 2) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
            
            if (curStationFor.getCount() >= 2) {
                builder.setTitle("行先フィルター").setMultiChoiceItems(curStationFor, "CHECK_FLAG", "FILTER_NAME", null);
                
                builder.setPositiveButton("適用", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        
                    }
                });
                builder.setNegativeButton("キャンセル", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            if (curTrainClass.getCount() > 2) {
                builder.setTitle("種類フィルター").setMultiChoiceItems(curTrainClass, "CHECK_FLAG", "FILTER_NAME", null);
                
                builder.setPositiveButton("適用", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        
                    }
                });
                builder.setNegativeButton("キャンセル", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // refresh view from db
        this.getLoaderManager().restartLoader(0, null, (LoaderCallbacks<Cursor>) this);
        aq.id(R.id.myStationListView).getListView().invalidateViews();
    }
}
