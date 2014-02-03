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
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Patterns;

import com.google.gson.JsonObject;

import de.tu_berlin.snet.commstat.R;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ImpulseProbe;
import edu.mit.media.funf.util.StringUtil;

@DisplayName("Email Probe")
@Schedule.DefaultSchedule(interval=3600, strict=false, opportunistic=true)
public class EmailProbe extends ImpulseProbe {
	
	public static final String TAG = "Email";
	
	@Configurable
	private boolean wifiOnly = true;
	
	private Thread emailThread = null;
	
	@Override
	protected void onEnable() {
		super.onEnable();
		Log.d(TAG, "onEnable");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		
		emailThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				if (!isAvailable()) {
					Log.w(TAG, "Network not available, aborting.");
					return;
				}
				fetchEmail();
			}
		});
		emailThread.start();
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
			if (NetworkInfo.State.CONNECTED.equals(wifiInfo) || NetworkInfo.State.CONNECTING.equals(wifiInfo)) {
				return true;
			}	
		}
		return false;
	}

	private void fetchEmail() {
		SharedPreferences prefs = getContext().getSharedPreferences("default", Context.MODE_MULTI_PROCESS);

		for (int i = 0; i < prefs.getInt("emailCount", 0); i++) {
			try {

				if (!prefs.getBoolean("email" + i, false)) {
					Log.d(TAG, "email" + i + " not enabled.");
					continue;
				}
				SharedPreferences emailPrefs = getContext().getSharedPreferences("email" + i, Context.MODE_MULTI_PROCESS);

				// get keys 
				String username = getContext().getString(R.string.email_username_key);
				String password = getContext().getString(R.string.email_password_key);
				String type = getContext().getString(R.string.email_type_key);
				String host = getContext().getString(R.string.email_host_key);
				String port = getContext().getString(R.string.email_port_key);

				Properties props = new Properties();
				// Self-signed certificates cause error, therefore accept all certificates 
				props.put("mail.imaps.socketFactory.class", "de.tu_berlin.snet.mail.MySSLSocketFactory");
				props.put("mail.pop3s.socketFactory.class", "de.tu_berlin.snet.mail.MySSLSocketFactory");

				Session session = Session.getInstance(props, null);
				session.setDebug(false);
				Store store = session.getStore(emailPrefs.getString(type, null));
				store.connect(emailPrefs.getString(host, null), 
						Integer.parseInt(emailPrefs.getString(port, null)), 
						emailPrefs.getString(username, null), 
						emailPrefs.getString(password, null));

				for (Folder f : store.getDefaultFolder().list("*")) {

					if ((f.getType() & Folder.HOLDS_MESSAGES) == 0)
						continue;

					f.open(Folder.READ_ONLY);
					
					// Message index start at 1 and are not static
					for (int id = emailPrefs.getInt(f.getFullName(), 1); id <= f.getMessageCount(); id++) {
						
						if (id % 100 == 0 && !isAvailable()) {
							Log.w(TAG, "Network not available anymore, aborting.");
							return;
						}	
						sendData(handleMessage(f.getMessage(id)));
						emailPrefs.edit().putInt(f.getFullName(), id+1).commit();			
					}
					f.close(false);
				}
				store.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		stop();
	}
	
	private JsonObject handleMessage(Message m) {
		JsonObject data = null;
		try {
			data = new JsonObject();
			data.addProperty("from", StringUtil.join(extractEmailAddress(m.getFrom()), ","));
			data.addProperty("to", StringUtil.join(extractEmailAddress(m.getAllRecipients()), ","));
			data.addProperty("date", m.getSentDate().getTime());
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	private List<String> extractEmailAddress(Address[] addr) {
		List<String> ls = new ArrayList<String>();
		
		for (Address a : addr) {
			for (String s : a.toString().split("\\s+")) {
				s = s.replaceAll("[<>]*", "");
				if (Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
					// anonymise address
					ls.add(sensitiveData(s));
				}
			}
		}
		if (addr.length != ls.size())
			Log.w(TAG, "extracted Address mismatch!");
		
		return ls;
	}	
}
