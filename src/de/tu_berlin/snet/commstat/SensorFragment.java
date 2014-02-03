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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.WifiProbe;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.util.StringUtil;

public class SensorFragment extends Fragment implements DataListener {

	public static final String TAG = "Sensor";
	public static final String PIPELINE_NAME = "default";
	public static final String PACKAGE_NAME = "de.tu_berlin.snet.commstat";
	public static final String FIRST_RUN_KEY = "firstRun";

	private TextView statusTextView;
	private TextView infoTextView;
	private SharedPreferences prefs;
	private Handler handle;
	private FunfManager funfMgr;
	private BasicPipeline pipeline;
	private WifiProbe wifiProbe;
	private ServiceConnection funfMgrConn;

	public SensorFragment() {
		handle = new Handler();
		funfMgrConn  = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				funfMgr = ((FunfManager.LocalBinder)service).getManager();
				pipeline = (BasicPipeline) funfMgr.getRegisteredPipeline(PIPELINE_NAME);
				wifiProbe = funfMgr.getGson().fromJson(new JsonObject(), WifiProbe.class);
				wifiProbe.registerPassiveListener(SensorFragment.this);
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

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.sensor_fragment, container, false);

		statusTextView = (TextView) rootView.findViewById(R.id.status);
		infoTextView = (TextView) rootView.findViewById(R.id.info);

		final Switch enable = (Switch) rootView.findViewById(R.id.pipeline);
		enable.setEnabled(false);
		enable.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (enable.isChecked()) {
					funfMgr.enablePipeline(PIPELINE_NAME);
					pipeline = (BasicPipeline) funfMgr.getRegisteredPipeline(PIPELINE_NAME);
				} else {
					funfMgr.disablePipeline(PIPELINE_NAME);
				}
			}
		});
		handle.postDelayed(new Runnable() {

			@Override
			public void run() {
				enable.setChecked(funfMgr.isEnabled(PIPELINE_NAME));
				enable.setEnabled(true);
				reloadStatus();
				reloadInfo();
			}
		}, 2000);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences("default", MainActivity.MODE_MULTI_PROCESS);

		if (prefs.getBoolean(FIRST_RUN_KEY, true)) {
			Builder info = new AlertDialog.Builder(getActivity());
			info.setTitle(R.string.info_title);
			info.setIcon(android.R.drawable.ic_dialog_info);
			info.setCancelable(false);
			info.setMessage(R.string.info_message);
			info.setPositiveButton("OK", new Dialog.OnClickListener () {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			info.create().show();
			prefs.edit().putBoolean(FIRST_RUN_KEY, false).commit();
		}
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
		reloadStatus();
		wifiProbe.registerPassiveListener(this);
		Log.d(TAG, arg0.toString());
	}

	@Override
	public void onDataReceived(IJsonObject arg0, IJsonObject arg1) {
		onDataCompleted(arg0, arg1);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.sensor, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_sync:
			sync(true);
			return true;
		case R.id.action_archive:
			sync(false);
			return true;
		case R.id.action_contact:
			sendMail();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void sendMail() {
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
				"mailto", "ssakar@mailbox.tu-berlin.de", null));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Commstat] ");
		startActivity(Intent.createChooser(emailIntent, "Send email..."));
	}

	private void sync(boolean upload) {
		if (pipeline.isEnabled()) {
			Toast.makeText(getActivity(), R.string.sync_message, Toast.LENGTH_SHORT).show();
			pipeline.onRun(BasicPipeline.ACTION_UPDATE, null);
			pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
			if (upload)
				pipeline.onRun(BasicPipeline.ACTION_UPLOAD, null);
		} else {
			Toast.makeText(getActivity(), R.string.pipeline_disabled, Toast.LENGTH_SHORT).show();
		}
	}

	private void reloadStatus() {
		if (pipeline == null || !pipeline.isEnabled() || getActivity() == null)
			return;

		try {
			String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;
			String EMAIL_COUNT_SQL = TOTAL_COUNT_SQL + " WHERE name LIKE '%EmailProbe' ";
			String FACEBOOK_COUNT_SQL = TOTAL_COUNT_SQL + " WHERE name LIKE '%FacebookProbe' ";
			
			// Query the pipeline db for the count of rows in the data table
			SQLiteDatabase db = pipeline.getDb();
			Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
			mcursor.moveToFirst();
			final int totalCount = mcursor.getInt(0);
			
			mcursor = db.rawQuery(EMAIL_COUNT_SQL, null);
			mcursor.moveToFirst();
			final int emailCount = mcursor.getInt(0);
			
			mcursor = db.rawQuery(FACEBOOK_COUNT_SQL, null);
			mcursor.moveToFirst();
			final int fbCount = mcursor.getInt(0);
			
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					statusTextView.setText(Html.fromHtml("<b> Archive Size: </b>" +
							String.format("%1$,.2f", getArchiveSize()) + " MB" +
							"<br><b>Total Database Entries: </b>" + String.valueOf(totalCount) +
							"<br><b>Email Database Entries: </b>" + String.valueOf(emailCount) +
							"<br><b>Facebook Database Entries: </b>" + String.valueOf(fbCount)));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}

	private double getArchiveSize() {
		String path = Environment.getExternalStorageDirectory() + 
				"/" + PACKAGE_NAME + "/" + PIPELINE_NAME + "/archive";
		File dir = new File(path);
		double size = 0;

		if (!dir.exists())
			return 0;

		for (File file : dir.listFiles()) {
			size += file.length();
		}
		return size/(1024 * 1024);
	}

	private void reloadInfo() {
		if (pipeline == null || getActivity() == null)
			return;

		try {
			final List<String> names = new ArrayList<String>();

			for (JsonElement el : pipeline.getDataRequests()) {
				String probeClassName = el.getAsJsonObject().get(RuntimeTypeAdapterFactory.TYPE).getAsString();
				String[] parts = probeClassName.split("\\.");
				names.add(parts[parts.length - 1].replace("Probe", ""));
			}

			Collections.sort(names);
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					infoTextView.setText(Html.fromHtml(getString(R.string.probe_title) +
							StringUtil.join(names, ", ")));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
}