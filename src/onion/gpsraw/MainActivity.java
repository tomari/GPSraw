package onion.gpsraw;

import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.text.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends GPSActivity {
	private ArrayAdapter<String> providerArrayAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		// setup spinner to choose a provider
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		try {
			prepareProviderSpinner(locationManager,getBestProvider());
		} catch(NullPointerException ignored) {}
	}
	private void savePreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor e=shrP.edit();
		e.putString(PREFERRED_PROVIDER_LABEL, getPreferredProvider());
		e.commit();
	}
	@Override
	public void onResume() {
		super.onResume();
	}
	@Override
	public void onPause() {
		savePreferences();
		super.onPause();
	}
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void prepareActionBarSpinner(ArrayAdapter<String> adapter, int selectedIndex) {
		ActionBar actionbar=getActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				providerSelected(itemPosition);
				return false;
			}
		});
		if(selectedIndex>0) {
			actionbar.setSelectedNavigationItem(selectedIndex);
		}
	}
	private void prepareProviderSpinner(LocationManager lm,String bestProvider) {
		Spinner sp=(Spinner)findViewById(R.id.providerSpinner);
		List<String> providers=lm.getProviders(true);
		providerArrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,providers);
		providerArrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
		providerArrayAdapter.insert(getResources().getString(R.string.provider_auto).concat(bestProvider), 0);
		int idx=(getPreferredProvider()==null)?-1:providers.indexOf(getPreferredProvider());
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB ) {
			providerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(providerArrayAdapter);
			if(idx>0) { sp.setSelection(idx+1,false); }
			sp.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					providerSelected(position);
				}
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		} else {
			sp.setVisibility(View.GONE);
			prepareActionBarSpinner(providerArrayAdapter,idx);
		}
	}
	@Override
	protected ProgressBar satelliteProgressBar() {
		return (ProgressBar) findViewById(R.id.progressBar);
	}
	@Override
	protected TextView numSatellitesTextView() {
		return (TextView)findViewById(R.id.numSatelliteTextView);
	}
	@Override
	protected TextView latitudeTextView() {
		return (TextView) findViewById(R.id.latTextView);
	}
	@Override
	protected TextView longitudeTextView() {
		return (TextView) findViewById(R.id.longTextView);
	}
	@Override
	protected TextView altitudeTextView() {
		return (TextView) findViewById(R.id.altTextView);
	}
	@Override
	protected TextView accuracyTextView() {
		return (TextView) findViewById(R.id.accuracyTextView);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId=item.getItemId();
		if(itemId==R.id.action_settings) {
			Intent intent=new Intent(this,SettingsActivity.class);
			startActivity(intent);
		} else if(itemId==R.id.action_satellites) {
			Intent intent=new Intent(this,SatellitesActivity.class);
			startActivity(intent);
		} else {
			return super.onMenuItemSelected(featureId,item);
		}
		return true;
	}
	
	private void copyToClipboard(CharSequence str) {
		try {
			ClipboardManager cm=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(str);
			Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
		} catch (NullPointerException e) {}
	}
	private CharSequence getLocationText() {
		TextView latView=(TextView)findViewById(R.id.latTextView);
		TextView longView=(TextView)findViewById(R.id.longTextView);
		return latView.getText().toString().concat(" ").concat(longView.getText().toString());
	}
	private void providerSelected(int index) {
		if(index>0) {
			setPreferredProvider((String)providerArrayAdapter.getItem(index));
		} else { setPreferredProvider(null); }
	}
	public void copyLocation(View v) {
		copyToClipboard(getLocationText());
	}
	public void copyAltitude(View v) {
		TextView altTextView=(TextView)findViewById(R.id.altTextView);
		copyToClipboard(altTextView.getText());
	}
}
