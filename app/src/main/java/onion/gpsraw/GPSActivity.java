package onion.gpsraw;

import java.text.NumberFormat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
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
	}
	private void loadPreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		preferred_provider=shrP.getString(PREFERRED_PROVIDER_LABEL, null);
	}
	@Override
	public void onResume() {
		super.onResume();
		locFormatter=new LocationFormatter(this);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestLocationPermission();
		}
		if(setupLocationProviders()) {
			setSatelliteStatus(setupLocationListener());
		}
	}
	@Override
	public void onPause() {
		detachLocationServices();
		locFormatter=locFormatter.bye();
		super.onPause();
	}
	private boolean setupLocationListener() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			// set selected provider and register update notifications
			String provider=(preferred_provider==null)?bestProvider:preferred_provider;
			if(provider!=null && locationManager.getProvider(provider)!=null) {
				try {
					locationManager.requestLocationUpdates(provider, 0, 0, this);
				} catch(SecurityException e) {
					return false;
				}
				return true;
			}
		}
		return false;
	}
	private void detachLocationServices() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(locationManager!=null) {
			try {
				locationManager.removeUpdates(this);
			} catch(SecurityException ignored) { }
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
	protected boolean setupLocationProviders() {
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
	protected TextView speedTextView() { return null; }
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
	private void setTextIfNotNull(TextView t, CharSequence s) {
		try { t.setText(s); } catch(NullPointerException ignored) {}
	}
	private void putLocationOnScreen(Location loc) {
		TextView t;
		setTextIfNotNull(latitudeTextView(),locFormatter.convertLatitude(loc));
		setTextIfNotNull(longitudeTextView(),locFormatter.convertLongitude(loc));
		setTextIfNotNull(altitudeTextView(),locFormatter.convertAltitude(loc));
		setTextIfNotNull(accuracyTextView(),locFormatter.convertAccuracy(loc));
		setTextIfNotNull(speedTextView(),locFormatter.convertSpeed(loc));
		
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

	// Android 6.0 (Marshmallow) permission behavior change
	private final int GPSRAW_LOCATION_PERMISSION = 0x5454;
	@TargetApi(Build.VERSION_CODES.M)
	private void requestLocationPermission() {
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					GPSRAW_LOCATION_PERMISSION);
		}
	}
	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		if(requestCode == GPSRAW_LOCATION_PERMISSION) {
			if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if(setupLocationProviders()) {
					setSatelliteStatus(setupLocationListener());
				}
			}
		}
	}
}
