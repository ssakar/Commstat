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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

/*
 * PreferenceFragment is not part of the support libs.
 * Therefore the deprecated PreferenceActivity is used.
 */
@SuppressWarnings("deprecation")
public class EmailSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private int position;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		position = intent.getIntExtra("position", 0);
		
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName("email" + position);
		prefMgr.setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
		
		addPreferencesFromResource(R.layout.email_settings);
		
	//	EditTextPreference usernamePref = (EditTextPreference) findPreference(getString(R.string.email_username_key));
	//	usernamePref.setText(intent.getStringExtra(getString(R.string.email_username_key)));
		
		PreferenceScreen prefScr = getPreferenceScreen();
		for (int i = 0; i < prefScr.getPreferenceCount(); i++) {
			initPref(prefScr.getPreference(i));
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePref(findPreference(key));
	}
	
	private void initPref(Preference pref) {
		if (pref instanceof PreferenceCategory) {
			PreferenceCategory prefCat = (PreferenceCategory) pref;
			for (int i = 0; i < prefCat.getPreferenceCount(); i++) {
				initPref(prefCat.getPreference(i));
			}
		} else {
			updatePref(pref);
		}
	}
	
	private void updatePref(Preference pref) {
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			if (listPref.getEntry() != null)
				pref.setTitle((String) listPref.getEntry());
		} else if (pref instanceof EditTextPreference) {
			EditTextPreference textPref = (EditTextPreference) pref;
			if (textPref.getText() != null && !textPref.getKey().equals(getString(R.string.email_password_key)))
				pref.setTitle(textPref.getText());
		}
	}
}
