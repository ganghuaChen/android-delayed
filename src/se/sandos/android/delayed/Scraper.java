package se.sandos.android.delayed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class Scraper extends Thread {
	public final static String BASE_URL = "http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/";
	public final static String Tag = "Scraper";
	
	private static Queue<String> queue = new LinkedList<String>();
	
	private static Thread worker = new Scraper();
	
	public static void queueForParse(String relativeUrl)
	{
		queue.add(relativeUrl);
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
				String url = queue.poll();
				if(url != null) {
					parseTrainPage(url);
				}
			}
		}
	}
	
	/**
	 * Parse stations. End station specifically.
	 * @param relativeUrl
	 */
	private static void parseTrainPage(String relativeUrl)
	{
		String cut = relativeUrl.substring(relativeUrl.indexOf("WapPages/") + 9);

		HttpClient hc = new DefaultHttpClient();
		String url = BASE_URL + cut;
		Log.i(Tag, "Correct url: " + url);

		HttpGet hg = new HttpGet(url);
		
		try {
			HttpResponse hr = hc.execute(hg);
			Log.i(Tag, "Got page: " + hr.getStatusLine());
			InputStream is = hr.getEntity().getContent();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			String s = null;
			while ((s = br.readLine()) != null) {
				Log.i(Tag, s);
			}

		} catch (Throwable e) {
			Log.w(Tag, e.getMessage());
		}

	}
}
