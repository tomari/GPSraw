package onion.gpsraw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class GPSActivity extends Activity implements LocationListener {
	private String bestProvider;
	public static final String PREFERRED_PROVIDER_LABEL="preferred_provider";
	private String preferred_provider=null;
	protected static final int SATELLITE_STATUS_LOCKED=0;
	protected static final int SATELLITE_STATUS_SEARCHING=1;
	protected static final int SATELLITE_STATUS_UNAVAIL=2;
	private LocationFormatter locFormatter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadPreferences();
		setupLocationProviders();
	}
	private void loadPreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		preferred_provider=shrP.getString(PREFERRED_PROVIDER_LABEL, null);
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
		super.onPause();
	}
	protected boolean setupLocationListener() {
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
	protected void detachLocationServices() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			locationManager.removeUpdates(this);
		}
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
	private boolean setupLocationProviders() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			bestProvider = selectBestLocationProvider(locationManager);
			return true;
		}
		return false;
	}
	protected ProgressBar satelliteProgressBar() {
		return null;
	}
	protected TextView numSatellitesTextView() {
		return null;
	}
	protected TextView latitudeTextView() {
		return null;
	}
	protected TextView longitudeTextView() {
		return null;
	}
	protected TextView altitudeTextView() {
		return null;
	}
	protected TextView accuracyTextView() {
		return null;
	}
	private void setSatelliteStatus(boolean searching) {
		ProgressBar pbar=satelliteProgressBar();
		if(pbar!=null) {
			pbar.setIndeterminate(searching);
			pbar.setVisibility(searching?View.VISIBLE:View.INVISIBLE);
		}
		if(searching) {
			View nSatellite=numSatellitesTextView();
			if(nSatellite!=null) {
				nSatellite.setVisibility(View.INVISIBLE);
			}
		}
	}
	private void setSatelliteStatus(int status) {
		if(status==SATELLITE_STATUS_LOCKED) {  setSatelliteStatus(false); }
		else if(status==SATELLITE_STATUS_SEARCHING) { setSatelliteStatus(true); }
		else if(status==SATELLITE_STATUS_UNAVAIL) {
			ProgressBar pbar=satelliteProgressBar();
			if(pbar!=null) {
				pbar.setProgress(0);
				pbar.setIndeterminate(false);
				pbar.setVisibility(View.VISIBLE);
			}
			View nSatellite=numSatellitesTextView();
			if(nSatellite!=null) {
				nSatellite.setVisibility(View.INVISIBLE);
			}
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
	private void putLocationOnScreen(Location loc) {
		TextView t;
		t=latitudeTextView(); if(t!=null) { t.setText(locFormatter.convertLatitude(loc)); }
		t=longitudeTextView(); if(t!=null) { t.setText(locFormatter.convertLongitude(loc)); }
		t=altitudeTextView(); if(t!=null) { t.setText(locFormatter.convertAltitude(loc)); }
		t=accuracyTextView(); if(t!=null) { t.setText(locFormatter.convertAccuracy(loc)); }
		
		TextView nSat=numSatellitesTextView();
		if(nSat!=null) {
			CharSequence nSatellites=locFormatter.convertNumSatellites(loc);
			if(nSatellites!=null) {
				nSat.setVisibility(View.VISIBLE);
				nSat.setText(nSatellites);
			} else {
				nSat.setVisibility(View.INVISIBLE);
				nSat.setText(R.string.placeholder_nsatellite);
			}
		}
	}
	protected void setPreferredProvider(String provider) {
		detachLocationServices();
		preferred_provider=provider;
		setSatelliteStatus(setupLocationListener());
	}
	protected String getPreferredProvider() { return preferred_provider; }
	protected String getBestProvider() { return bestProvider; }
}
