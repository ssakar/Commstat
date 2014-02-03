/*
	Commstat - Funf-based Sensor Application 
	Copyright (C) 2013 Serkan Sakar

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package de.tu_berlin.snet.probe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ImpulseProbe;

@DisplayName("Facebook Probe")
@Schedule.DefaultSchedule(interval=3600, strict=false, opportunistic=true)
public class FacebookProbe extends ImpulseProbe {
	
	public static final String TAG = "Facebook";
	private static final String[] REQUEST_IDS = new String[] { "me",
			"me/friends", "me/inbox", "me/feed", "me/likes", "me/groups",
			"me/pokes", "me/activities", "me/interests", "me/music",
			"me/movies", "me/books", "me/events", "me/checkins" };
	private static final String[] SENSITIVE = new String[] { "id", "name",
			"username", "first_name", "last_name", "link", "caption", "story",
			"message", "picture", "icon", "birthday" };

	@Configurable
	private boolean wifiOnly = true;
	
	private Thread fbThread = null;
	
	@Override
	protected void onEnable() {
		super.onEnable();
		Log.d(TAG, "onEnable");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		
		fbThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				if (!isAvailable()) {
					Log.w(TAG, "Network not available, aborting.");
					return;
				}
				fetchFacebook();
			}
		});
		fbThread.start();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}
	
	@Override
	protected void onDisable() {
		super.onDisable();
		Log.d(TAG, "onDisable");
	}
	
	private boolean isAvailable() {
		ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

		if (!wifiOnly && netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		} else if (wifiOnly) {
			NetworkInfo.State wifiInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
			if (NetworkInfo.State.CONNECTED.equals(wifiInfo) || NetworkInfo.State.CONNECTING.equals(wifiInfo)) 
				return true;
		}
		return false;
	}

	private void fetchFacebook() {
		try {
			SharedPreferences prefs = getContext().getSharedPreferences("default", Context.MODE_MULTI_PROCESS);
			Session session = Session.getActiveSession();
			List<Request> requests = new ArrayList<Request>();
			
			if (session == null) {
				Log.d(TAG, "Facebook Probe not activated.");
				return;
			}
			
			// default is Tue, 01 Jan 2008 00:00:00 GMT
			int since = prefs.getInt("facebook.since", 1199145600);
			int until = (int) (System.currentTimeMillis() / 1000);
			
			Bundle params = new Bundle();
	        params.putInt("since", since);
	        params.putInt("until", until);
	        params.putInt("limit", 5000);
			
			for (final String requestId : REQUEST_IDS) {
				requests.add(new Request(session, requestId, params, null, 
						new Request.Callback() {
					
							public void onCompleted(Response response) {
								GraphObject graphObject = response.getGraphObject();
								FacebookRequestError error = response.getError();

								if (graphObject != null) {
									JSONObject orig = graphObject.getInnerJSONObject();
									JsonObject data = new JsonObject();
									
									anonymizeData(orig);
									data.addProperty(response.getRequest().getGraphPath(), orig.toString());
									sendData(data);
									
								} else if (error != null) {
									Log.e(TAG,"Error: " + error.getErrorMessage());
								}
							}
							
							private Set<String> sensitiveSet = new HashSet<String>(Arrays.asList(SENSITIVE));

							private void anonymizeData(Object o) {
								try {
									if (o instanceof JSONObject) {
										JSONObject obj = (JSONObject) o;
									
										// not needed, may contain sensitive data
										obj.remove("paging");
										obj.remove("actions");
									
										for (Iterator<?> iter = obj.keys(); iter.hasNext();) {
											String key = (String) iter.next();
											Object val = obj.get(key);
											
											if (!(val instanceof String))
												anonymizeData(val);
											
											if (sensitiveSet.contains(key)) 
												obj.put(key, sensitiveData((String) val));
										}
									} else if (o instanceof JSONArray) {
										JSONArray arr = (JSONArray) o;

										for (int i = 0; i < arr.length(); i++)
											anonymizeData(arr.get(i));
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}));
			}
			boolean error = false;
			for (Response r : Request.executeBatchAndWait(requests)) {
				if (r.getError() != null)
					error = true;
			}
			if (!error)
				prefs.edit().putInt("facebook.since", until+1).commit();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		stop();
	}

}
