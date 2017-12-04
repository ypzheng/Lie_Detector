package cs.umass.edu.myactivitiestoolkit.ppg;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * Wraps a heart rate sensor reading and defines a JSON structure that allows
 * the reading to be sent to the server.
 *
 * @author Erik Risinger
 *
 * @see SensorReading
 */
public class HRSensorReading extends SensorReading {

    /** The heart rate value. **/
    private final double value;

    /**
     * Instantiates a heart rate reading.
     * @param userID a 10-byte hex string identifying the current user.
     * @param deviceType describes the device
     * @param deviceID unique device identifier
     * @param t the timestamp at which the event occurred, in Unix time by convention.
     * @param value the mean red value
     */
    public HRSensorReading(String userID, String deviceType, String deviceID, long t, double value){
        super(userID, deviceType, deviceID, "SENSOR_HR", t);

        this.value = value;
    }

    @Override
    protected JSONObject toJSONObject(){
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("value", value);

            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
