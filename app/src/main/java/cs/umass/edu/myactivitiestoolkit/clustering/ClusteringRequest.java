package cs.umass.edu.myactivitiestoolkit.clustering;

import android.annotation.TargetApi;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * Wraps a clustering request which will be sent to the server in order to notify
 * your Python script which algorithm you would like to perform.
 *
 * @author CS390MB
 *
 * @see SensorReading
 */
public class ClusteringRequest extends SensorReading {

    /** The clustering algorithm. **/
    private final String algorithm;

    private final double[] latitudes, longitudes;

    private final int k;

    /**
     * Instantiates a PPG reading.
     * @param userID a 10-byte hex string identifying the current user.
     * @param deviceType describes the device.
     * @param deviceID unique device identifier.
     * @param locations the locations to cluster
     * @param t the timestamp at which the event occurred, in Unix time by convention.
     * @param algorithm the clustering algorithm.
     * @param k The number of clusters if algorithm is K-Means. If the algorithm is Mean-Shift, this is ignored.
     */
    public ClusteringRequest(String userID, String deviceType, String deviceID, long t, GPSLocation[] locations, String algorithm, int k){
        super(userID, deviceType, deviceID, "SENSOR_CLUSTERING_REQUEST", t);

        this.algorithm = algorithm;
        this.k = k;
        this.latitudes = new double[locations.length];
        this.longitudes = new double[locations.length];
        for (int i = 0; i < locations.length; i++){
            latitudes[i] = locations[i].latitude;
            longitudes[i] = locations[i].longitude;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected JSONObject toJSONObject(){
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();

        try {
            JSONArray latitudeArray = new JSONArray(latitudes);
            JSONArray longitudeArray = new JSONArray(longitudes);
            data.put("t", timestamp);
            data.put("longitudes", longitudeArray);
            data.put("latitudes", latitudeArray);
            data.put("algorithm", algorithm);
            data.put("k", k);

            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
