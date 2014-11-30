package onion.gpsraw;

import java.text.NumberFormat;

import android.app.Activity;
import android.text.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity implements android.location.LocationListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setupLocationListener();
	}
	@Override
	public void onPause() {
		detachLocationServices();
		super.onPause();
	}
	
	private void setupLocationListener() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}
	private void detachLocationServices() {
		LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(this);
	}
	private void setSatteliteStatus(boolean searching) {
		ProgressBar pbar=(ProgressBar) findViewById(R.id.progressBar);
		pbar.setIndeterminate(searching);
	}
	private void setTextViewContent(int textviewid, String str) {
		TextView v=(TextView) findViewById(textviewid);
		v.setText(str);
	}
	private String format60(String input) {
		Resources r=getResources();
		char arr[]=input.toCharArray();
		int i=0, cutoffLen=arr.length;
		for(; i<arr.length; i++) {
			if(arr[i] == ':') { arr[i]=r.getString(R.string.degrees).charAt(0); break; } }
		for(; i<arr.length; i++) {
			if(arr[i] == ':') { arr[i]=r.getString(R.string.minutes).charAt(0); break; } }
		for(; i<arr.length; i++) {
			if(arr[i]=='.') {
				cutoffLen=Math.min(i+3,arr.length-1);
			}
		}
		arr[cutoffLen]=r.getString(R.string.seconds).charAt(0);
		return new String(arr).substring(0,cutoffLen+1);
	}
	private String convertLatitude(double latitude) {
		String res=getResources().getString((latitude>=0.)?R.string.lat_north:R.string.lat_south);
		double alat=Math.abs(latitude);
		return format60(res.concat(Location.convert(alat,Location.FORMAT_SECONDS)));
	}
	private String convertLongitude(double longitude) {
		String res=getResources().getString((longitude>=0.)?R.string.long_east:R.string.long_west);
		double along=Math.abs(longitude);
		return format60(res.concat(Location.convert(along, Location.FORMAT_SECONDS)));
	}
	private void putLocationOnScreen(Location loc) {
		double latitude=loc.getLatitude();
		double longitude=loc.getLongitude();
		double altitude=loc.getAltitude();
		String lat_string=convertLatitude(latitude);
		String long_string=convertLongitude(longitude);
		String alt_string=NumberFormat.getInstance().format(altitude)
				.concat(getResources().getString(R.string.alt_meters));
		setTextViewContent(R.id.latTextView,lat_string);
		setTextViewContent(R.id.longTextView,long_string);
		setTextViewContent(R.id.altTextView,alt_string);
	}
	@Override
	public void onLocationChanged(Location loc) {
		setSatteliteStatus(false);
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
		setSatteliteStatus(true);
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
		cm.setText(getLocationText());
	}
	private String getLocationText() {
		TextView latView=(TextView)findViewById(R.id.latTextView);
		TextView longView=(TextView)findViewById(R.id.longTextView);
		return latView.getText().toString().concat(" ").concat(longView.getText().toString());
	}
}
