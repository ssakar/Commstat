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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.media.funf.util.StringUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;

public class EmailFragment extends ListFragment {

	public static final String TAG = "Email";
	
	private ArrayAdapter<String> adapter;
	private List<String> list;
	SharedPreferences prefs;

	public EmailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences("default", Context.MODE_MULTI_PROCESS);
		
		loadAccounts();
		adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, list);
		setListAdapter(adapter);
		
		initSelection();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		CheckedTextView check = (CheckedTextView) v;
		prefs.edit().putBoolean("email" + position, check.isChecked()).commit();

		if (check.isChecked()) {
			Intent intent = new Intent(getActivity(), EmailSettingsActivity.class);
			intent.putExtra(getString(R.string.email_username_key), list.get(position));
			intent.putExtra("position", position);

			startActivity(intent);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.email, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_account:
			addAccount();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveAccounts() {
		Editor editor = prefs.edit();
		editor.putString("accounts", StringUtil.join(list, ";"));
		editor.putInt("emailCount", list.size());
		editor.commit();
	}
	
	private void loadAccounts() {
		String accounts = prefs.getString("accounts", null);

		if (accounts != null) {
			list = new ArrayList<String>(Arrays.asList(accounts.split(";")));
		} else {
			Set<String> set = new HashSet<String>();

			for (Account acc : AccountManager.get(getActivity()).getAccounts()) {
				if (Patterns.EMAIL_ADDRESS.matcher(acc.name).matches()) 
					set.add(acc.name);
			}
			list = new ArrayList<String>(set);
			saveAccounts();
		}
	}

	private void initSelection() {
		for (int i = 0; i < prefs.getInt("emailCount", 0); i++) {
			getListView().setItemChecked(i, prefs.getBoolean("email" + i, false));
		}
	}

	private void addAccount() {
		final EditText input = new EditText(getActivity());

		new AlertDialog.Builder(getActivity())
				.setTitle("Add Account")
				.setMessage(getString(R.string.add_account_message))
				.setView(input)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						list.add(input.getText().toString());
						adapter.notifyDataSetChanged();
						saveAccounts();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				}).show();
	}
}
