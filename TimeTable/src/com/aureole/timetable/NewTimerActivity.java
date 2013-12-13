package com.aureole.timetable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class NewTimerActivity extends Activity implements OnQueryTextListener , OnItemClickListener{

    private final AQuery aq = new AQuery(this);
    private ArrayAdapter<String> adapter;
    private List<String> stationIdList = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_new_timer);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        aq.id(R.id.stationListView).adapter(adapter).itemClicked(this);

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_timer, menu);
        MenuItem searchViewItem = menu.findItem(R.id.searchStationBar);
        SearchView searchView = (SearchView) ((View) searchViewItem.getActionView()).findViewById(R.id.searchStation);
        searchView.setOnQueryTextListener(this);
        searchView.requestFocus();
        return true;
    }
    
    @Override
    public boolean onQueryTextChange(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String queryStation) {
        setProgressBarIndeterminateVisibility(Boolean.TRUE);
        String query = "";
        try {
            query = URLEncoder.encode(queryStation, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        aq.ajax("http://transit.loco.yahoo.co.jp/station/time/search?q=" + query, String.class, new AjaxCallback<String>(){
            @Override
            public void callback(String url, String html, AjaxStatus status) {
            	if (status.getCode() == 200) {
	                Document stationSelect = Jsoup.parse(html);
	                // search for station
	                Elements stations = stationSelect.select("#station-plu-select ul li a");
	                adapter.clear();
	                stationIdList.clear();
	                
	                if (stations != null && stations.size() > 0) {
	                    Pattern pattern = Pattern.compile("^\\/station\\/rail\\/(\\d+)\\/.*");
	                    for (Element s : stations) {
	                        Matcher matcher = pattern.matcher(s.attr("href"));
	                        if (matcher.find()) {
	                            stationIdList.add(matcher.group(1));
	                            adapter.add(s.text());
	                        }
	                    }
	                    adapter.notifyDataSetChanged();
	                } else {
	                    Intent intent = new Intent(aq.getContext(), StationDetailActivity.class);
	                    
	                    Pattern pattern = Pattern.compile("/station/top/(\\d+)/");
	                    Matcher matcher = pattern.matcher(stationSelect.select("#station-nav .tab01 a").attr("href"));
	                    if (matcher.find()) {
                            intent.putExtra("StationId", matcher.group(1));
                            intent.putExtra("StationName", stationSelect.select(".staName h1").text());
	                    }
	                    startActivityForResult(intent, 1); 
	                }
            	} else {
            		// network error
            		Log.i("network", "status:" + status.getCode() + " " + status.getError() + " url:" + url);
            	}
            	setProgressBarIndeterminateVisibility(Boolean.FALSE);
            }
        });
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        Toast.makeText(this, stationIdList.get(index), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, StationDetailActivity.class);
        intent.putExtra("StationId", stationIdList.get(index));
        intent.putExtra("StationName", ((TextView)view).getText());
        startActivityForResult(intent, 1);
        
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            this.finish();
        }
    }

}
