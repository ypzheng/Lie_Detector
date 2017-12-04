package cs.umass.edu.myactivitiestoolkit.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import edu.umass.cs.MHLClient.sensors.GPSReading;

/**
 * The location service collects GPS data, stores the readings in a local database
 * and sends them to the server.
 */
public class LocationService extends SensorService implements LocationListener {

    /** Used during debugging to identify logs by class */
    private static final String TAG = LocationService.class.getName();

    /**
     * The minimum duration in milliseconds between sensor readings.
     */
    private static final int MIN_TIME = 5000;

    /**
     * Defines the minimum distance in meters between sequential sensor readings.
     */
    private static final float MIN_DISTANCE = 0f;

    /**
     * Manages the GPS sensor.
     */
    private LocationManager locationManager;

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.LOCATION_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.LOCATION_SERVICE_STOPPED);
    }

    @Override
    protected void registerSensors() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //make sure we have permission to access location before requesting the sensor.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "Starting location manager");
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME,
                MIN_DISTANCE,
                this,
                getMainLooper());

    }

    @Override
    protected void unregisterSensors() {
        //make sure we have permission to access location before requesting the sensor.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.LOCATION_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.location_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_location_on_white_48dp;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.toString());
        LocationDAO dao = new LocationDAO(getApplicationContext());
        dao.openWrite();
        dao.insert(new GPSLocation(location.getTime(),location.getLatitude(),location.getLongitude(), location.getAccuracy()));
        dao.close();
        client.sendSensorReading(new GPSReading(userID, "MOBILE", "", location.getTime(), location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
