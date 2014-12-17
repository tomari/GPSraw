package onion.gpsraw;

import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.text.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
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

public class MainActivity extends Activity implements android.location.LocationListener {
	private String preferred_provider=null;
	public static final String PREFERRED_PROVIDER_LABEL="preferred_provider";
	private ArrayAdapter<String> providerArrayAdapter;
	private String bestProvider;
	private LocationFormatter locFormatter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		loadPreferences();
		setupLocationProviders();
	}
	private void loadPreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		preferred_provider=shrP.getString(PREFERRED_PROVIDER_LABEL, null);
	}
	private void savePreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor e=shrP.edit();
		e.putString(PREFERRED_PROVIDER_LABEL, preferred_provider);
		e.commit();
	}
	@Override
	public void onResume() {
		super.onResume();
		locFormatter=new LocationFormatter(this);
		setSatelliteStatus(setupLocationListener());
	}
	@Override
	public void onPause() {
		detachLocationServices();
		locFormatter=locFormatter.bye();
		savePreferences();
		super.onPause();
	}
	private String selectBestLocationProvider(LocationManager locationManager) {
		Criteria c=new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setBearingRequired(false);
		c.setCostAllowed(true);
		c.setSpeedRequired(false);
		c.setPowerRequirement(Criteria.NO_REQUIREMENT);
		return locationManager.getBestProvider(c,true);
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
		int idx=(preferred_provider==null)?-1:providers.indexOf(preferred_provider);
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
	private boolean setupLocationProviders() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			bestProvider = selectBestLocationProvider(locationManager);
			// setup spinner to choose a provider
			prepareProviderSpinner(locationManager,bestProvider);
			return true;
		}
		return false;
	}
	private boolean setupLocationListener() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			// set selected provider and register update notifications
			String provider=(preferred_provider==null)?bestProvider:preferred_provider;
			if(provider!=null && locationManager.getProvider(provider)!=null) {
				locationManager.requestLocationUpdates(provider, 0, 0, this);
				return true;
			}
		}
		return false;
	}
	private void detachLocationServices() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			locationManager.removeUpdates(this);
		}
	}
	private void setSatelliteStatus(boolean searching) {
		ProgressBar pbar=(ProgressBar) findViewById(R.id.progressBar);
		pbar.setIndeterminate(searching);
		pbar.setVisibility(searching?View.VISIBLE:View.INVISIBLE);
		if(searching) {
			View nSatellite=findViewById(R.id.numSatelliteTextView);
			nSatellite.setVisibility(View.INVISIBLE);
		}
	}
	private static final int SATELLITE_STATUS_LOCKED=0;
	private static final int SATELLITE_STATUS_SEARCHING=1;
	private static final int SATELLITE_STATUS_UNAVAIL=2;
	private void setSatelliteStatus(int status) {
		if(status==SATELLITE_STATUS_LOCKED) {  setSatelliteStatus(false); }
		else if(status==SATELLITE_STATUS_SEARCHING) { setSatelliteStatus(true); }
		else if(status==SATELLITE_STATUS_UNAVAIL) {
			ProgressBar pbar=(ProgressBar) findViewById(R.id.progressBar);
			pbar.setProgress(0);
			pbar.setIndeterminate(false);
			pbar.setVisibility(View.VISIBLE);
			View nSatellite=findViewById(R.id.numSatelliteTextView);
			nSatellite.setVisibility(View.INVISIBLE);
		}
	}
	private void setTextViewContent(int textviewid, CharSequence str) {
		TextView v=(TextView) findViewById(textviewid);
		v.setText(str);
	}
	private void setTextViewContent(int textviewid, int resourceid) {
		TextView v=(TextView) findViewById(textviewid);
		v.setText(resourceid);
	}
	private void putLocationOnScreen(Location loc) {
		setTextViewContent(R.id.latTextView,locFormatter.convertLatitude(loc));
		setTextViewContent(R.id.longTextView,locFormatter.convertLongitude(loc));
		setTextViewContent(R.id.altTextView,locFormatter.convertAltitude(loc));
		setTextViewContent(R.id.accuracyTextView,locFormatter.convertAccuracy(loc));
		
		CharSequence nSatellites=locFormatter.convertNumSatellites(loc);
		if(nSatellites!=null) {
			findViewById(R.id.numSatelliteTextView).setVisibility(View.VISIBLE);
			setTextViewContent(R.id.numSatelliteTextView,nSatellites);
		} else {
			findViewById(R.id.numSatelliteTextView).setVisibility(View.INVISIBLE);
			setTextViewContent(R.id.numSatelliteTextView,R.string.placeholder_nsatellite);
		}

	}
	@Override
	public void onLocationChanged(Location loc) {
		setSatelliteStatus(false);
		putLocationOnScreen(loc);
	}

	@Override
	public void onProviderDisabled(String name) {
	}

	@Override
	public void onProviderEnabled(String name) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if(status==LocationProvider.OUT_OF_SERVICE) {
			setSatelliteStatus(SATELLITE_STATUS_UNAVAIL);
		} else if(status==LocationProvider.TEMPORARILY_UNAVAILABLE) {
			setSatelliteStatus(true);
		} else if(status==LocationProvider.AVAILABLE) {
			setSatelliteStatus(false);
		}
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
		} else {
			return super.onMenuItemSelected(featureId,item);
		}
		return true;
	}
	
	private void copyToClipboard(CharSequence str) {
		ClipboardManager cm=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setText(str);
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
	private CharSequence getLocationText() {
		TextView latView=(TextView)findViewById(R.id.latTextView);
		TextView longView=(TextView)findViewById(R.id.longTextView);
		return latView.getText().toString().concat(" ").concat(longView.getText().toString());
	}
	private void providerSelected(int index) {
		detachLocationServices();
		if(index>0) {
			preferred_provider=(String)providerArrayAdapter.getItem(index);
		} else { preferred_provider=null; }
		setSatelliteStatus(setupLocationListener());
	}
	public void copyLocation(View v) {
		copyToClipboard(getLocationText());
	}
	public void copyAltitude(View v) {
		TextView altTextView=(TextView)findViewById(R.id.altTextView);
		copyToClipboard(altTextView.getText());
	}
}
