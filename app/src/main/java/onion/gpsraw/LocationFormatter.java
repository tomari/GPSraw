package onion.gpsraw;

import java.text.NumberFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class LocationFormatter implements SharedPreferences.OnSharedPreferenceChangeListener {
	private Context context;
	private int cutoff_digits=3;
	private int unitOfLength=0;
	private int unitOfSpeed=0;
	private int locationFormat=Location.FORMAT_SECONDS;
	private NumberFormat nf;
	private final String[] unitOfSpeed_labels;
	
	public LocationFormatter(Context context) {
		this.context=context;
		nf=NumberFormat.getInstance();
		loadPreferences();
		unitOfSpeed_labels=context.getResources().getStringArray(R.array.unitofspeed_labels);
	}
	public LocationFormatter bye() {
		PreferenceManager.getDefaultSharedPreferences(context)
			.unregisterOnSharedPreferenceChangeListener(this);
		return null;
	}
	private void loadPreferences() {
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(context);
		loadUnitOfLength(shrP);
		loadUnitOfSpeed(shrP);
		loadLocationFormat(shrP);
		loadMantissaDigits(shrP);
		shrP.registerOnSharedPreferenceChangeListener(this);
	}
	private int loadPreference(SharedPreferences shrP, String prefLabel, String defaultValue) {
		String str=shrP.getString(prefLabel,defaultValue);
		int res;
		try { res=Integer.parseInt(str); }
		catch (NumberFormatException e) { res=Integer.parseInt(defaultValue); }
		return res;
	}
	private void loadUnitOfLength(SharedPreferences shrP) {
		unitOfLength=loadPreference(shrP,SettingsActivity.unitOfLength,
				context.getResources().getString(R.string.unitoflength_default));
	}
	private void loadUnitOfSpeed(SharedPreferences shrP) {
		unitOfSpeed=loadPreference(shrP,SettingsActivity.unitOfSpeed,
				context.getResources().getString(R.string.unitofspeed_default));
	}
	private void loadMantissaDigits(SharedPreferences shrP) {
		cutoff_digits=loadPreference(shrP,SettingsActivity.mantissaDigits,
				context.getResources().getString(R.string.mantissadigits_default));
	}
	private void loadLocationFormat(SharedPreferences shrP) {
		locationFormat=loadPreference(shrP,SettingsActivity.locationFormat,
				context.getResources().getString(R.string.locationformat_default));
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences shrP, String key) {
		if(SettingsActivity.unitOfLength.equals(key)) {
			loadUnitOfLength(shrP);
		} else if(SettingsActivity.unitOfSpeed.equals(key)) {
			loadUnitOfSpeed(shrP);
		} else if(SettingsActivity.locationFormat.equals(key)) {
			loadLocationFormat(shrP);
		} else if(SettingsActivity.mantissaDigits.equals(key)) {
			loadMantissaDigits(shrP);
		}
	}
	
	
	private CharSequence format60(CharSequence prefix, CharSequence input, CharSequence suffix) {
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
		final int last_suffixes[]={R.string.degrees,R.string.minutes,R.string.seconds};
		buf.append(r.getString(last_suffixes[locationFormat]));
		if(prefix!=null) { buf.insert(0, prefix); }
		if(suffix!=null) { buf.append(suffix); }
		return buf;
	}
	public CharSequence convertLatitude(Location loc) {
		double latitude=loc.getLatitude();
		boolean isNorth=latitude>=0.;
		Resources r=context.getResources();
		String prefix=null, suffix=null;
		double alat;
		if(locationFormat==Location.FORMAT_DEGREES) {
			alat=latitude;
		} else {
			prefix=r.getString(isNorth?R.string.lat_north_prefix:R.string.lat_south_prefix);
			alat=Math.abs(latitude);
			suffix=r.getString(isNorth?R.string.lat_north:R.string.lat_south);
		}
		return format60(prefix,Location.convert(alat,locationFormat),suffix);
	}
	public CharSequence convertLongitude(Location loc) {
		double longitude=loc.getLongitude();
		boolean isEast=longitude>=0.;
		Resources r=context.getResources();
		String prefix=null,suffix=null;
		double along;
		if(locationFormat==Location.FORMAT_DEGREES) {
			along=longitude;
		} else {
			prefix=r.getString(isEast?R.string.long_east_prefix:R.string.long_west_prefix);
			along=Math.abs(longitude);
			suffix=r.getString(isEast?R.string.long_east:R.string.long_west);
		}
		return format60(prefix,Location.convert(along, locationFormat),suffix);
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
	public CharSequence convertSpeed(Location loc) {
		String spd_string;
		if(loc.hasSpeed()) {
			final float multipliers[]={1.0f, 1.e-3f*3600f, 2.2369362920544f};
			float speed = loc.getSpeed()*multipliers[unitOfSpeed];
			spd_string=nf.format(speed).concat(" ").concat(unitOfSpeed_labels[unitOfSpeed]);
		} else {
			spd_string="";
		}
		return spd_string;
	}
}
