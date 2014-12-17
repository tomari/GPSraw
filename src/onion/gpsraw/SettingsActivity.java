package onion.gpsraw;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static String unitOfLength="pref_unitoflength";
	public static String locationFormat="pref_locationformat";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		updateSummary(getPreferenceScreen());
	}
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		updateSummary(findPreference(key));
	}
	private void updateSummary(Preference p) {
		if(p instanceof PreferenceGroup) {
			PreferenceGroup pGrp=(PreferenceGroup) p;
			int nP=pGrp.getPreferenceCount();
			for(int i=0; i<nP; i++) {
				updateSummary(pGrp.getPreference(i));
			}
		} else if(p instanceof ListPreference) {
			ListPreference listP=(ListPreference)p;
			p.setSummary(listP.getEntry());
		} else if(p instanceof EditTextPreference) {
			EditTextPreference edittextP=(EditTextPreference)p;
			p.setSummary(edittextP.getText());
		}
	}
}
