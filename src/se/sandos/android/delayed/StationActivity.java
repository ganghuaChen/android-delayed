package se.sandos.android.delayed;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.prefs.PreferencesActivity;
import se.sandos.android.delayed.prefs.Prefs;
import se.sandos.android.delayed.scrape.ScrapeListener;
import se.sandos.android.delayed.scrape.ScraperHelper;
import se.sandos.android.delayed.scrape.ScraperHelper.Nameurl;
import se.sandos.android.delayed.scrape.StationListScraper;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class StationActivity extends ListActivity
{
    private static final String Tag = "StationActivity";

    private static final DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM,
            java.util.Locale.GERMANY);

    private static long lastRefresh = -1;
    
    private static final boolean TRACE = false;

    private List<Map<String, String>> listContent = null;
    private SimpleAdapter sa = null;

    // These are passed to us on creation
    private String url;
    private String name;
    
    private boolean needsInvalidate = false;

    private List<TrainEvent> trainevents = new ArrayList<TrainEvent>();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.liststations);

        Intent i = getIntent();
        Uri uri = i.getData();
        if (uri == null)
        {
            Log.w(Tag, "No data URI!");
            finish();
            return;
        }
        Log.v(Tag, "data: " + uri);
        name = uri.getFragment();
        url = Delayed.getDb(getApplicationContext()).getUrl(this.name);

        if (url == null)
        {
            Log.w(Tag, "Could not find station named " + name);
            finish();
            return;
        }

        setTitle("Delayed: " + name);

        Log.v(Tag, "Created stationact: " + name + " " + url + " " + this);

        if (TRACE)
        {
            Debug.startMethodTracing("list");
        }

        //Run this lengthy operation in the background, so we can access the menu immediately
        Runnable r = new Runnable()
        {
            public void run()
            {
                fetchList();
            }
        };
        new Thread(r).start();

        // We have different-height rows
        getListView().setSmoothScrollbarEnabled(false);
    }

    private synchronized void addEvents(List<TrainEvent> events, boolean updateUI)
    {
        if (listContent == null)
        {
            listContent = new ArrayList<Map<String, String>>();
        }

        for (TrainEvent te : events)
        {
            boolean updated = false;
            if (existsAndUpdate(te) == 2)
            {
                updated = true;
            }
            else if (existsAndUpdate(te) == 1)
            {
                return;
            }

            trainevents.add(te);
            if (!updated)
            {
                needsInvalidate = true;

                Map<String, String> m = new HashMap<String, String>();
                m.put("name", te.toString());
                m.put("track", "Track: " + te.getTrack());
                m.put("number", "Train #: " + te.getNumber());
                m.put("destination", te.getDestination());
                m.put("url", te.getUrl());
                m.put("delayed", te.getDelayed());
                m.put("extra", te.getExtra());
                Date departureDate = te.getDepartureDate();
                if (departureDate != null)
                {
                    m.put("time", Long.toString(departureDate.getTime()));
                }
                else
                {
                    m.put("time", "-");
                }
                listContent.add(m);
            }
        }

        if(updateUI)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    refreshList();
                }
            });
        }
    }

    private void refreshList()
    {
        if (sa == null)
        {
            needsInvalidate = true;
            SimpleAdapter.ViewBinder vb = new SimpleAdapter.ViewBinder()
            {

                public boolean setViewValue(View view, Object data, String textRepresentation)
                {
                    TextView tv = (TextView) view;

                    if (tv.getId() == R.id.Extra)
                    {
                        // data = "majs http://swedroid.org";
                        if (((String) data).length() == 0)
                        {
                            tv.setVisibility(View.GONE);
                        }
                        else
                        {
                            //tv.setAutoLinkMask(Linkify.WEB_URLS);
                            //tv.setLinkTextColor(0xffddddff);
                            tv.setText(Html.fromHtml((String)data));
                            tv.setVisibility(View.VISIBLE);
                        }
                    }
                    else
                    {
                        tv.setText((String) data);
                    }
                    return true;
                }
            };

            sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.eventrow, new String[]
            { "name", "destination", "track", "number", "delayed", "extra" }, new int[]
            { R.id.Time, R.id.Destination, R.id.Track, R.id.TNumber, R.id.Delayed, R.id.Extra });

            sa.setViewBinder(vb);

            setListAdapter(sa);
        }

        Collections.sort(listContent, new Comparator<Map<String, String>>()
        {
            public int compare(Map<String, String> object1, Map<String, String> object2)
            {
                try
                {
                    long time1 = Long.valueOf(object1.get("time")).longValue();
                    long time2 = Long.valueOf(object2.get("time")).longValue();

                    if (time1 < time2)
                    {
                        return -1;
                    }
                    if (time1 > time2)
                    {
                        return 1;
                    }
                }
                catch (Exception e)
                {
                }

                return 0;
            }
        });
        if (!needsInvalidate)
        {
            sa.notifyDataSetChanged();
        }
        else
        {
            sa.notifyDataSetInvalidated();
        }
        needsInvalidate = false;
    }

    @SuppressWarnings("unchecked")
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Object o = l.getAdapter().getItem(position);

        if(!(o instanceof Map))
        {
            return;
        }
        
        Map<String, String> m = (Map<String, String>) o;

        String url = m.get("url");

        if (url == null)
        {
            // Compute URL
            url = StationListScraper.base;
            Calendar c = new GregorianCalendar();
            DecimalFormat dff = new DecimalFormat("00");

            url += "TrainShow.aspx?JF=-1&train=" + c.get(Calendar.YEAR) + dff.format((c.get(Calendar.MONTH) + 1))
                    + dff.format(c.get(Calendar.DATE)) + "," + m.get("number").substring(9);
        }
        else
        {
            url = StationListScraper.domain + url;
        }

        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }

    /**
     * Update our listContent model of the list
     * 
     * @param te
     * @return true if the trainevent existed already and was updated, false if it was not found
     */
    private synchronized int existsAndUpdate(TrainEvent te)
    {
        if (listContent == null || listContent.size() == 0)
        {
            return 0;
        }

        boolean changed = false;

        for (Map<String, String> m : listContent)
        {
            if (m.get("number").equals("Train #: " + te.getNumber()))
            {
                // Update
                String delayed = m.get("delayed");
                if (!delayed.equals(te.getDelayed()))
                {
                    changed = true;
                    m.put("delayed", te.getDelayed());
                }

                String extra = m.get("extra");
                if (!extra.equals(te.getExtra()))
                {
                    changed = true;
                    m.put("extra", te.getExtra());
                }

                if (changed)
                {
                    return 2;
                }
                else
                {
                    return 1;
                }
            }
        }

        return 0;
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        // This probably means the user used a launcher shortcut
        // Only if we set ourselves to singletask/singletop
        // We want one activity per station, really.
        Log.v(Tag, "Reintenting");
    }

    private void fetchList()
    {
        DBAdapter db = Delayed.getDb(getApplicationContext());

        Log.v(Tag, "Name of station: " + name + " " + url);
        final String url = this.url;
        final String name = this.name;

        if (listContent == null || listContent.size() == 0)
        {
            // Fetch from db
            List<TrainEvent> events = db.getStationEvents(name);

            addEvents(events, true);
            trainevents.clear();
        }

        ScraperHelper.scrapeStation(url, name, new ScrapeListener<TrainEvent, Object[]>()
        {
            public void onStatus(String status)
            {
//                 mHandler.dispatchMessage(Message.obtain(mHandler, ScrapeListener.MSG_STATUS, status));
            }

            public void onFinished(final Object[] result)
            {
                if (result == null)
                {
                    // this actually means finished!
                    Delayed.getDb(getApplicationContext()).addTrainEvents(trainevents);

                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            refreshList();
                        
                            lastRefresh = System.currentTimeMillis();
        
                            StringBuffer sb = new StringBuffer();
                            sb.append(name).append(" ");
                            sb.append("updated " + printNow());
                            setTitle(sb.toString());                            
                            
                            if (TRACE)
                            {
                                Debug.stopMethodTracing();
                            }
                        }
                    });
                }
                else
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            // Got a proper destination for some particular train. Mend.
                            @SuppressWarnings("unchecked")
                            List<Nameurl> stations = (List<Nameurl>) result[1];
                            if (stations.size() > 0)
                            {
                                Nameurl nu = stations.get(stations.size() - 1);
            
                                Log.i(Tag, "We got an end destination with value " + nu.name + " and name " + result[0]);
            
                                for (Map<String, String> v : listContent)
                                {
                                    String name = v.get("destination");
                                    if (name != null && name.equals(result[0]))
                                    {
                                        Log.i(Tag, "found match: " + result[0] + " " + nu.name);
                                        v.put("destination", nu.name);
                                        sa.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    });
                }
            }

            public void onPartialResult(final TrainEvent result)
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        List<TrainEvent> evs = new ArrayList<TrainEvent>();
                        evs.add(result);
                        addEvents(evs, false);                 
                    }
                });
            }

            public void onRestart()
            {
                trainevents.clear();
            }

            public void onFail()
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append(name).append(" ");
                        sb.append("Update failure");
                        setTitle(sb.toString());                            
                    }
                });
            };
        });

        runOnUiThread(new Runnable()
        {
            public void run()
            {
                StringBuffer sb = new StringBuffer();
                sb.append(name).append(" ");
                sb.append("Start update...");
                setTitle(sb.toString());                            
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 1, 0, "Ladda om");
        menu.add(0, 2, 0, "Välj station");
        menu.add(0, 3, 0, "Gör till favorit");
        menu.add(0, 4, 0, "Inställningar");
        menu.add(0, 5, 0, "Browser");

        return true;
    }

    private synchronized void clearList()
    {
        if (listContent != null)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    listContent.clear();

                    if (sa == null)
                    {
                        sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.stationrow, new String[]
                        { "name" }, new int[]
                        { R.id.StationName });
                        setListAdapter(sa);
                    }

                    sa.notifyDataSetInvalidated();
                }
            });
        }
    }

    public void onWindowFocusChanged(boolean hasFocus)
    {
        if(hasFocus)
        {
            maybeRefresh();
        }
    }
    
    protected void onResume()
    {
        super.onResume();
        
        maybeRefresh();
    }

    private synchronized void maybeRefresh()
    {
        //Auto-refresh if latest refresh was more than a few minutes ago
        long now = System.currentTimeMillis(); 
        if (listContent != null && (lastRefresh == -1 || (now - lastRefresh) > 1000*120))
        {
            listContent.clear();
            fetchList();
        }
    }

    
    public boolean onOptionsItemSelected(MenuItem mi)
    {
        if (mi.getItemId() == 1)
        {
//            clearList();
            synchronized(this)
            {
                if(listContent != null)
                {
                    listContent.clear();
                }
                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        fetchList();
                    }
                };
                new Thread(r).start();
                return true;
            }
        }

        if (mi.getItemId() == 2)
        {
            Intent intent = new Intent("se.sandos.android.delayed.StationList", null, getApplicationContext(),
                    StationListActivity.class);
            startActivity(intent);
            return true;
        }

        if (mi.getItemId() == 3)
        {
            Prefs.addFavorite(getApplicationContext(), name);
            return true;
        }

        if (mi.getItemId() == 4)
        {
            Intent i = new Intent("se.sandos.android.delayed.Prefs", null, getApplicationContext(),
                    PreferencesActivity.class);
            startActivity(i);
            return true;
        }

        if (mi.getItemId() == 5)
        {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(StationListScraper.domain + url));
            startActivity(i);
            return true;
        }

        return true;
    }
    
    private synchronized String printNow()
    {
        return df.format(new Date());
    }
}
