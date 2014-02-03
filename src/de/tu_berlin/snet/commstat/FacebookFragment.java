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


package de.tu_berlin.snet.commstat;

import java.util.Arrays;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.UserSettingsFragment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.tu_berlin.snet.probe.FacebookProbe;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;

public class FacebookFragment extends UserSettingsFragment implements DataListener {

	public static final String TAG = "Facebook";
	public static final String PIPELINE_NAME = "default";
	
	private static final String[] PERMS = new String[] { "user_events",
			"read_mailbox", "read_stream", "user_likes", "user_groups",
			"user_birthday", "user_website", "user_education_history",
			"user_work_history", "user_checkins", "friends_checkins" };
	private FunfManager funfMgr;
	private ServiceConnection funfMgrConn;
	private BasicPipeline pipeline;
	private FacebookProbe fbProbe;
	
	public FacebookFragment() {
		funfMgrConn  = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				funfMgr = ((FunfManager.LocalBinder)service).getManager();
				pipeline = (BasicPipeline) funfMgr.getRegisteredPipeline(PIPELINE_NAME);
				fbProbe = funfMgr.getGson().fromJson(new JsonObject(), FacebookProbe.class);
				Log.i(TAG, "Funf service connected");
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				funfMgr = null;
				pipeline = null;
				Log.i(TAG, "Funf service disconnected");
			}
		};
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setReadPermissions(Arrays.asList(PERMS));
		setSessionStatusCallback(new Session.StatusCallback() {
			
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				Log.d(TAG, "New session state: " + state.toString());
				
				if (state.isOpened() && pipeline.isEnabled()) 
					fbProbe.registerListener(FacebookFragment.this);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		// Bind the service to create the connection with FunfManager
		getActivity().bindService(new Intent(getActivity(), FunfManager.class), funfMgrConn, MainActivity.BIND_AUTO_CREATE);
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unbindService(funfMgrConn);
	}
	
	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		fbProbe.registerPassiveListener(this);
		Log.d(TAG, arg0.toString());
	}

	@Override
	public void onDataReceived(IJsonObject arg0, IJsonObject arg1) {
		onDataCompleted(arg0, arg1);
	}

}
