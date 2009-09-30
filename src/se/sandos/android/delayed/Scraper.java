package se.sandos.android.delayed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class Scraper extends Thread {
	public final static String BASE_URL = "http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/";
	public final static String Tag = "Scraper";
	
	private static Queue<Job<?>> queue = new LinkedList<Job<?>>();
	
	private static Thread worker = new Scraper();
	

	public static class Nameurl {
		public String name;
		public String url;
		public Nameurl(String n, String u) { name = n; url = u; }
	}

	public abstract static class Job<T> implements Runnable
	{
		protected T value;
		
		public void execute()
		{
			
		}
		abstract public void run();
	}

	
	public static void queueForParse(final String relativeUrl, final Job<List<Nameurl>> job)
	{
		queue(new Job<List<Nameurl>>(){
			@Override
			public void execute() {
				job.value = parseTrainPage(relativeUrl);
				job.run();
			}

			@Override
			public void run() {
			}
		});
	}
	
	public static void queue(Job<?> job)
	{
		queue.add(job);
		if(!worker.isAlive()) {
			worker.start();
			worker.setPriority(Thread.MIN_PRIORITY);
		} else {
			if(worker.getState() == Thread.State.TIMED_WAITING) {
				worker.interrupt();
			}
		}
	}
	
	public void run()
	{
		while(true) {
			if(queue.isEmpty()) {
				try {
					Thread.sleep(100000);
				} catch (InterruptedException e) {
				}
			}
			
			if(!queue.isEmpty()) {
				Job<?> job = queue.poll();
				if(job != null) {
					job.execute();
					//parseTrainPage(job.);
				}
			}
		}
	}
	
	/**
	 * Parse stations. End station specifically.
	 * @param relativeUrl
	 */
	private static List<Nameurl> parseTrainPage(String relativeUrl)
	{
		String cut = relativeUrl.substring(relativeUrl.indexOf("WapPages/") + 9);

		HttpClient hc = new DefaultHttpClient();
		String url = BASE_URL + cut;
		Log.i(Tag, "Parsing trainpage, full url: " + url);

		HttpGet hg = new HttpGet(url);
		
		try {
			HttpResponse hr = hc.execute(hg);
			Log.i(Tag, "Got page: " + hr.getStatusLine());
			InputStream is = hr.getEntity().getContent();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			
			List<Nameurl> names = new ArrayList<Nameurl>(40);
			while (true) {
				String s = br.readLine();
				if(s == null) {
					break;
				}
				if(s.indexOf("showallstations") != -1) {
					String uri = s.substring(9, s.lastIndexOf("\">"));
					Log.i(Tag, "Url: " + uri);
					
					String stationName = s.substring(s.lastIndexOf("\">") + 2, s.length() - 8);
					stationName = StringEscapeUtils.unescapeHtml(stationName);
					names.add(new Nameurl(stationName, uri));
				} else {
					Log.i(Tag, s);
				}
			}
			Log.i(Tag, "Got names of all stations for " + cut);

			return names;
		} catch (Throwable e) {
			String msg = e.getMessage();
			if(msg == null) {
				msg = "Something happened: " + e;
			}
			Log.w(Tag, msg);
		}

		return new LinkedList<Nameurl>();
	}
}
