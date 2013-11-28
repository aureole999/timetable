package com.aureole.timetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

public class StationDetailActivity extends Activity implements OnItemClickListener {

    private final AQuery aq = new AQuery(this);
    private SimpleAdapter adapter;
    private List<Map<String, Object>> stationDetailList;
    private int selectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_station_detail);
        Bundle stationInfo = getIntent().getExtras();
        String stationId = (String) stationInfo.get("StationId");
        String stationName = (String) stationInfo.get("StationName");
        setTitle(stationName);
        // Show the Up button in the action bar.
        setupActionBar();
        stationDetailList = new ArrayList<Map<String, Object>>();
        adapter = new SimpleAdapter(this, stationDetailList, R.layout.list_station_detail, new String[] { "line_name", "line_direction" }, new int[] { R.id.line_name, R.id.line_direction });
        aq.id(R.id.linesListView).adapter(adapter).itemClicked(this);

        getLinesByStation(stationId);

    }

    private void getLinesByStation(String stationId) {
        setProgressBarIndeterminateVisibility(Boolean.TRUE);

        aq.ajax("http://transit.loco.yahoo.co.jp/station/rail/" + stationId + "/", String.class, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String html, AjaxStatus status) {
                if (status.getCode() == 200) {
                    stationDetailList.clear();
                    Elements lines = Jsoup.parse(html).select("#station-line-select dl");
                    Elements linesName = lines.select("dt");
                    for (Element lineName : linesName) {
                        for (Element lineDirection : lineName.parent().select("dd a")) {
                            Map<String, Object> lineInfo = new HashMap<String, Object>();
                            lineInfo.put("line_name", lineName.text());
                            lineInfo.put("line_direction", lineDirection.text());
                            lineInfo.put("line_href", lineDirection.attr("href"));
                            stationDetailList.add(lineInfo);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(aq.getContext(), getString(R.string.network_return_error), Toast.LENGTH_LONG).show();
                }
                setProgressBarIndeterminateVisibility(Boolean.FALSE);
            }
        });
        return;
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {

        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.station_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        if (selectedIndex != -1) {
            Toast.makeText(this, "しばらくお待ちください", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedIndex = index;
        setProgressBarIndeterminateVisibility(Boolean.TRUE);
        String url = (String) stationDetailList.get(index).get("line_href");
        aq.ajax(url, String.class, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String html, AjaxStatus status) {
                if (status.getCode() == 200) {
                    Document document = Jsoup.parse(html);
                    Elements trainType = document.select("#timeNotice1");
                    Elements trainFor = document.select("#timeNotice2");
                    List<String> trainTypeList = new ArrayList<String>(); 
                    List<String> trainForList = new ArrayList<String>(); 
                    for (Element e : trainType.select("dd > dl > dd")) {
                        trainTypeList.add(e.text());
                    }
                    for (Element e : trainFor.select("dd > dl > dd")) {
                        trainForList.add(e.text());
                    }
                    stationDetailList.get(selectedIndex).put("trainType", trainTypeList);
                    stationDetailList.get(selectedIndex).put("trainFor", trainForList);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(aq.getContext(), getString(R.string.network_return_error), Toast.LENGTH_LONG).show();
                }
                setProgressBarIndeterminateVisibility(Boolean.FALSE);
                selectedIndex = -1;
            }
        });
    }

}
