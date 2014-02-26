package com.aureole.timetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
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
    private String stationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_station_detail);
        Bundle stationInfo = getIntent().getExtras();
        stationId = (String) stationInfo.get("StationId");
        String stationName = (String) stationInfo.get("StationName");
        setTitle(stationName);
        // Show the Up button in the action bar.
        setupActionBar();
        stationDetailList = new ArrayList<Map<String, Object>>();
        adapter = new SimpleAdapter(this, stationDetailList, android.R.layout.simple_list_item_multiple_choice, new String[] { "line_name_direction" }, new int[] { android.R.id.text1 });
        aq.id(R.id.linesListView).adapter(adapter).itemClicked(this).getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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
                            lineInfo.put("line_name_direction", lineName.text() + " " + lineDirection.text());
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (aq.id(R.id.linesListView).getListView().getCheckedItemCount() > 0) {
            getMenuInflater().inflate(R.menu.station_detail_selected, menu);
        } else {
            getMenuInflater().inflate(R.menu.station_detail, menu);
        }
        return true;
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
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.confirm:
                // get schedule from checked station-lines
                SparseBooleanArray itemPositions = aq.id(R.id.linesListView).getListView().getCheckedItemPositions();
                List<String> lineIdList = new ArrayList<String>();
                for (int i = 0; i < stationDetailList.size(); i++) {
                    if (itemPositions.get(i)) {
                        Pattern pattern = Pattern.compile("gid=(\\d+)");
                        Matcher matcher = pattern.matcher((String) stationDetailList.get(i).get("line_href"));
                        if (matcher.find()) {
                            lineIdList.add(matcher.group(1));
                        } else {
                            // unexpected error
                        }
                    }
                }
                if (lineIdList.size() > 0) {
                    new GetScheduleTask(this, stationId, null).execute(lineIdList.toArray(new String[lineIdList.size()]));
                } else {
                    // unexpected error
                }
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    public void onChkboxClicked(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        if (selectedIndex != -1) {
            Toast.makeText(this, getString(R.string.network_busy), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean showFilterFlag = false;

        // show destination and train class (Fast, Express, etc) filter
        if (aq.id(R.id.linesListView).getListView().getCheckedItemPositions().get(index) && showFilterFlag) {
            selectedIndex = index;
            setProgressBarIndeterminateVisibility(Boolean.TRUE);
            String url = (String) stationDetailList.get(selectedIndex).get("line_href");
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(aq.getContext());
                    builder.setPositiveButton("ok", new OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedIndex = -1;
                            invalidateOptionsMenu();
                        }
                    });
                    builder.setNegativeButton("cancel", new OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            aq.id(R.id.linesListView).getListView().setItemChecked(selectedIndex, false);
                            selectedIndex = -1;
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
    
                }
            });
        
        } else {
            
            // refresh menu bar
            invalidateOptionsMenu();
        }

    }

}
