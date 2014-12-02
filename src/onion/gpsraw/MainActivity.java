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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
		pbar.setVisibility(searching?View.VISIBLE:View.INVISIBLE);
	}
	private void setTextViewContent(int textviewid, CharSequence str) {
		TextView v=(TextView) findViewById(textviewid);
		v.setText(str);
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
			buf.setLength(Math.min(idx+4,buf.length()));
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
		String alt_string;
		if(loc.hasAltitude()) {
			alt_string=nf.format(altitude)
				.concat(r.getString(R.string.suffix_meters));
		} else { alt_string=getResources().getString(R.string.placeholder_alt); }
		setTextViewContent(R.id.altTextView,alt_string);
		String acc_string;
		if(loc.hasAccuracy()) {
			acc_string=r.getString(R.string.accuracy_prefix)
					.concat(nf.format(loc.getAccuracy()))
					.concat(r.getString(R.string.suffix_meters));
		} else { acc_string=getResources().getString(R.string.placeholder_acc); }
		setTextViewContent(R.id.accuracyTextView,acc_string);
		Bundle extras;
		if(null!=(extras=loc.getExtras())) {
			int satellites=extras.getInt("satellites");
			setTextViewContent(R.id.numSatteliteTextView,nf.format(satellites));
		}
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
		Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
	}
	private String getLocationText() {
		TextView latView=(TextView)findViewById(R.id.latTextView);
		TextView longView=(TextView)findViewById(R.id.longTextView);
		return latView.getText().toString().concat(" ").concat(longView.getText().toString());
	}
}
