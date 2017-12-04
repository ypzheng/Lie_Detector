package cs.umass.edu.myactivitiestoolkit.audio;

import android.annotation.TargetApi;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * Wraps an audio buffer into a sensor reading to send to the server.
 *
 * @author Sean Noran
 *
 * @see SensorReading
 * @see edu.umass.cs.MHLClient.client.MobileIOClient
 */
public class AudioBufferReading extends SensorReading {

    /**
     * The entries in the feature vector.
     */
    private final short[] buffer;

    /**
     * Instantiates an audio buffer reading.
     *
     * @param userID a 10-byte hex string identifying the current user.
     * @param deviceType describes the device.
     * @param deviceID unique device identifier.
     * @param t the timestamp at which the event occurred, in Unix time by convention.
     * @param buffer the microphone data buffer
     */
    public AudioBufferReading(String userID, String deviceType, String deviceID, long t, short[] buffer) {
        super(userID, deviceType, deviceID, "SENSOR_AUDIO", t);
        this.buffer = buffer;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected JSONObject toJSONObject() {
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();
        try {
            data.put("t", timestamp);
            JSONArray audioBuffer = new JSONArray(buffer);
            data.put("values", audioBuffer);
            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}