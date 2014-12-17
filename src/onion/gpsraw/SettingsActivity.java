package onion.gpsraw;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String unitOfLength="pref_unitoflength";
	public static final String locationFormat="pref_locationformat";
	public static final String mantissaDigits="pref_mantissadigits";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(android.os.Build.VERSION.SDK_INT>=11) {
			addUpToActionbar();
		}
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		updateSummary(getPreferenceScreen());
		
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void addUpToActionbar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int menuId=item.getItemId();
		if(menuId==android.R.id.home) {
			this.finish();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
