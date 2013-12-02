package com.aureole.timetable;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;
import com.commonsware.cwac.loaderex.SQLiteCursorLoader;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static DBHelper db;
    private final AQuery aq = new AQuery(this);
    private SimpleCursorAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DBHelper(getApplicationContext());
        
        //Cursor cursor = db.rawQuery("select * from STATION", new String[] {});
        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[]{"NAME"}, new int[]{android.R.id.text1}, 0);
        aq.id(R.id.myStationListView).adapter(adapter);
        getLoaderManager().initLoader(0, null, this);
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
        case R.id.action_newTimer:
            Intent intent = new Intent(this, NewTimerActivity.class);
            startActivity(intent);
            break;

        default:
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (loaderId) {
        case 0:
            // Returns a new CursorLoader
            return new SQLiteCursorLoader(this, db, "SELECT id _id, * FROM STATION", new String[]{});
        default:
            // An invalid id was passed in
            return null;
        }
    }


    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        adapter.changeCursor(cursor);
        
    }


    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
        
    }
}
