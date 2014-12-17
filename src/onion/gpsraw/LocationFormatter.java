package onion.gpsraw;

import java.text.NumberFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class LocationFormatter implements OnSharedPreferenceChangeListener {
	private Context context;
	private int cutoff_digits=3;
	private int unitOfLength=0;
	private NumberFormat nf;
	
	public LocationFormatter(Context context) {
		this.context=context;
		nf=NumberFormat.getInstance();
		loadPreferences();
	}
	public LocationFormatter bye() {
		PreferenceManager.getDefaultSharedPreferences(context)
			.unregisterOnSharedPreferenceChangeListener(this);
		return null;
	}
	private void loadPreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(context);
		String uOL_str=shrP.getString(SettingsActivity.unitOfLength, "0");
		try {
			unitOfLength=Integer.parseInt(uOL_str);
		} catch (NumberFormatException e) {
			unitOfLength=0;
		}
		shrP.registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences shrP, String key) {
		
	}
	
	
	private CharSequence format60(CharSequence input, CharSequence suffix) {
		Resources r=context.getResources();
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
			buf.setLength(Math.min(idx+1+cutoff_digits,buf.length()));
		}
		buf.append(r.getString(R.string.seconds));
		buf.append(suffix);
		return buf;
	}
	public CharSequence convertLatitude(Location loc) {
		double latitude=loc.getLatitude();
		boolean isNorth=latitude>=0.;
		Resources r=context.getResources();
		String res=r.getString(isNorth?R.string.lat_north_prefix:R.string.lat_south_prefix);
		double alat=Math.abs(latitude);
		return format60(res.concat(Location.convert(alat,Location.FORMAT_SECONDS)),
				r.getString(isNorth?R.string.lat_north:R.string.lat_south));
	}
	public CharSequence convertLongitude(Location loc) {
		double longitude=loc.getLongitude();
		boolean isEast=longitude>=0.;
		Resources r=context.getResources();
		String res=r.getString(isEast?R.string.long_east_prefix:R.string.long_west_prefix);
		double along=Math.abs(longitude);
		return format60(res.concat(Location.convert(along, Location.FORMAT_SECONDS)),
				r.getString(isEast?R.string.long_east:R.string.long_west));
	}
	private int m_to_ft(double m) {
		return (int) Math.round(m*3.28084);
	}
	private String getLengthSuffix() {
		return context.getResources().getString(
				(unitOfLength==1)?R.string.suffix_feet:R.string.suffix_meters);
	}
	private int normalizeLength(double length_in_meters) {
		return (unitOfLength==1)?m_to_ft(length_in_meters):
			(int)Math.round(length_in_meters);
	}
	public CharSequence convertAltitude(Location loc) {
		Resources r=context.getResources();
		CharSequence alt_string;
		if(loc.hasAltitude()) {
			int alt=normalizeLength(loc.getAltitude());
			alt_string=nf.format(alt).concat(getLengthSuffix());
		} else {
			alt_string=r.getString(R.string.placeholder_alt); 
		}
		return alt_string;
	}
	public CharSequence convertAccuracy(Location loc) {
		Resources r=context.getResources();
		CharSequence acc_string;
		if(loc.hasAccuracy()) {
			int acc=normalizeLength(loc.getAccuracy());
			acc_string=r.getString(R.string.accuracy_prefix)
					.concat(nf.format(acc))
					.concat(getLengthSuffix());
		} else {
			acc_string=r.getString(R.string.placeholder_acc); 
		}
		return acc_string;
	}
	public CharSequence convertNumSatellites(Location loc) {
		Bundle extras;
		String satellitesKey=context.getResources()
				.getString(R.string.location_extra_satellites_key);
		if(null!=(extras=loc.getExtras()) && extras.containsKey(satellitesKey)) {
			int satellites=extras.getInt(satellitesKey);
			return nf.format(satellites);
		} else {
			return null;
		}
	}
}
