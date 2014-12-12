package onion.gpsraw;

import java.text.NumberFormat;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.text.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
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

public class MainActivity extends Activity implements android.location.LocationListener {
	private int kirisute_digits=3;
	private String preferred_provider=null;
	private static final String PREFERRED_PROVIDER_LABEL="preferred_provider";
	private ArrayAdapter<String> providerArrayAdapter;
	private String bestProvider;
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
		setSatelliteStatus(setupLocationListener());
	}
	@Override
	public void onPause() {
		detachLocationServices();
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
		int idx=1+((preferred_provider==null)?-1:providers.indexOf(preferred_provider));
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
		View nSatellite=findViewById(R.id.numSatelliteTextView);
		if(searching) {
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
	private CharSequence format60(CharSequence input, CharSequence suffix) {
		Resources r=getResources();
		StringBuilder buf=new StringBuilder(input);
		final String separator=":",period=".";
		int idx;
		if((idx=buf.indexOf(separator))>0) {
			buf.setCharAt(idx, r.getString(R.string.degrees).charAt(0));
		}
		if((idx=buf.indexOf(separator,idx+1))>0) {
			buf.setCharAt(idx, r.getString(R.string.minutes).charAt(0));
		}
		if((idx=buf.indexOf(period,idx+1))>0) {
			buf.setLength(Math.min(idx+1+kirisute_digits,buf.length()));
		}
		buf.append(r.getString(R.string.seconds));
		buf.append(suffix);
		return buf;
	}
	private CharSequence convertLatitude(double latitude) {
		boolean isNorth=latitude>=0.;
		Resources r=getResources();
		String res=r.getString(isNorth?R.string.lat_north_prefix:R.string.lat_south_prefix);
		double alat=Math.abs(latitude);
		return format60(res.concat(Location.convert(alat,Location.FORMAT_SECONDS)),
				r.getString(isNorth?R.string.lat_north:R.string.lat_south));
	}
	private CharSequence convertLongitude(double longitude) {
		boolean isEast=longitude>=0.;
		Resources r=getResources();
		String res=r.getString(isEast?R.string.long_east_prefix:R.string.long_west_prefix);
		double along=Math.abs(longitude);
		return format60(res.concat(Location.convert(along, Location.FORMAT_SECONDS)),
				r.getString(isEast?R.string.long_east:R.string.long_west));
	}
	private void putLocationOnScreen(Location loc) {
		double latitude=loc.getLatitude();
		double longitude=loc.getLongitude();
		double altitude=loc.getAltitude();
		CharSequence lat_string=convertLatitude(latitude);
		setTextViewContent(R.id.latTextView,lat_string);
		CharSequence long_string=convertLongitude(longitude);
		setTextViewContent(R.id.longTextView,long_string);
		
		NumberFormat nf=NumberFormat.getInstance();
		Resources r=getResources();
		if(loc.hasAltitude()) {
			String alt_string=nf.format(altitude)
				.concat(r.getString(R.string.suffix_meters));
			setTextViewContent(R.id.altTextView,alt_string);
		} else {
			setTextViewContent(R.id.altTextView,R.string.placeholder_alt); 
		}
		if(loc.hasAccuracy()) {
			String acc_string=r.getString(R.string.accuracy_prefix)
					.concat(nf.format(loc.getAccuracy()))
					.concat(r.getString(R.string.suffix_meters));
			setTextViewContent(R.id.accuracyTextView,acc_string);
		} else {
			setTextViewContent(R.id.accuracyTextView,R.string.placeholder_acc); 
		}
		Bundle extras;
		String satellitesKey=r.getString(R.string.location_extra_satellites_key);
		if(null!=(extras=loc.getExtras()) && extras.containsKey(satellitesKey)) {
			int satellites=extras.getInt(satellitesKey);
			findViewById(R.id.numSatelliteTextView).setVisibility(View.VISIBLE);
			setTextViewContent(R.id.numSatelliteTextView,nf.format(satellites));
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
		setSatelliteStatus(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId=item.getItemId();
		if(itemId==R.id.action_copy) {
			copyToClipboard();
		} else {
			return super.onMenuItemSelected(featureId,item);
		}
		return true;
	}
	
	private void copyToClipboard() {
		ClipboardManager cm=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		CharSequence locationStr=getLocationText();
		cm.setText(locationStr);
		Toast.makeText(this, locationStr, Toast.LENGTH_SHORT).show();
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
}
