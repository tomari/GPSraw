package onion.gpsraw;

import java.text.NumberFormat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.location.GpsSatellite;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SatellitesActivity extends GPSActivity implements GpsStatus.Listener {
	private GpsStatus statusBuf=null;
	private String checkMark;
	private String uncheckMark;
	private NumberFormat nf;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_satellites);
		checkMark=getResources().getString(R.string.checkmark);
		uncheckMark=getResources().getString(R.string.uncheckmark);
		nf=NumberFormat.getInstance();
	}
	@Override
	public void onResume() {
		super.onResume();
		LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(lm!=null) {
			lm.addGpsStatusListener(this);
		}
	}
	@Override
	public void onPause() {
		LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(lm!=null) {
			lm.removeGpsStatusListener(this);
		}
		super.onPause();
	}
	@Override
	protected ProgressBar satelliteProgressBar() {
		return (ProgressBar) findViewById(R.id.satProgressBar);
	}
	@Override
	protected TextView numSatellitesTextView() {
		return (TextView) findViewById(R.id.satNumSatelliteTextView);
	}
	protected TextView latitudeTextView() {
		return (TextView) findViewById(R.id.satLatTextView);
	}
	protected TextView longitudeTextView() {
		return (TextView) findViewById(R.id.satLongTextView);
	}
	protected TextView altitudeTextView() {
		return (TextView) findViewById(R.id.satAltTextView);
	}
	protected TextView accuracyTextView() {
		return (TextView) findViewById(R.id.satAccuracyTextView);
	}
	private void updateTableWithSatellites(Iterable<GpsSatellite> satellites) {
		TableLayout tab=(TableLayout)findViewById(R.id.prntable);
		int row=1;
		for(GpsSatellite sat: satellites) {
			TableRow r=(TableRow) tab.getChildAt(row++);
			if(r==null) { r=makeTableRow(tab); }
			r.setVisibility(View.VISIBLE);
			((TextView)r.getChildAt(0)).setText(Integer.toString(sat.getPrn()));
			((TextView)r.getChildAt(1)).setText(nf.format(sat.getAzimuth()));
			((TextView)r.getChildAt(2)).setText(nf.format(sat.getElevation()));
			((TextView)r.getChildAt(3)).setText(nf.format(sat.getSnr()));
			((TextView)r.getChildAt(4)).setText(sat.hasAlmanac()?checkMark:uncheckMark);
			((TextView)r.getChildAt(5)).setText(sat.hasEphemeris()?checkMark:uncheckMark);
			((TextView)r.getChildAt(6)).setText(sat.usedInFix()?checkMark:uncheckMark);
		}
		for(; row<tab.getChildCount(); row++) {
			tab.getChildAt(row).setVisibility(View.GONE);
		}
	}
	private TableRow makeTableRow(TableLayout tab) {
		TableRow r=new TableRow(this);
		for(int j=1; j<=7; j++) {
			TextView t=makeTextViewForTableRow();
			t.setTextAppearance(this, android.R.style.TextAppearance);
			t.setText(Integer.toString(j));
			r.addView(t);
		}
		r.setBackgroundColor(getResources().getColor(
				((tab.getChildCount()&0x01)>0)?R.color.table_oddrow:
					R.color.table_evenrow));
		tab.addView(r);
		return r;
	}
	@SuppressLint("NewApi")
	private TextView makeTextViewForTableRow() {
		TextView t=new TextView(this);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
			t.setTextIsSelectable(true);
		}
		t.setGravity(Gravity.RIGHT);
		Resources r=getResources();
		t.setPadding(r.getDimensionPixelOffset(R.dimen.padding_table_row), 0, 
				r.getDimensionPixelOffset(R.dimen.padding_table_row), 0);
		return t;
	}
	@Override
	public void onGpsStatusChanged(int status) {
		if(status==GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
			LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
			if(lm!=null) {
				statusBuf=lm.getGpsStatus(statusBuf);
				Iterable<GpsSatellite> sats=statusBuf.getSatellites();
				if(sats!=null) {
					updateTableWithSatellites(sats);
				}
			}
		}
	}

}
