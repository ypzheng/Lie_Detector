package cs.umass.edu.myactivitiestoolkit.liedetector.services.msband;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.SampleRate;

import org.json.JSONException;
import org.json.JSONObject;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.communication.MHLClientFilter;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.HRSensorReading;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.SensorService;
import cs.umass.edu.myactivitiestoolkit.liedetector.steps.OnStepListener;
import cs.umass.edu.myactivitiestoolkit.liedetector.steps.StepDetector;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;
import edu.umass.cs.MHLClient.sensors.GyroscopeReading;
import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * The BandService is responsible for starting and stopping the sensors on the Band and receiving
 * accelerometer and gyroscope data periodically. It is a foreground service, so that the user
 * can close the application on the phone and continue to receive data from the wearable device.
 * Because the {@link BandGyroscopeEvent} also receives accelerometer readings, we only need to
 * register a {@link BandGyroscopeEventListener} and no {@link BandAccelerometerEventListener}.
 * This should be compatible with both the Microsoft Band and Microsoft Band 2.
 *
 * @author Sean Noran
 *
 * @see Service#startForeground(int, Notification)
 * @see BandClient
 * @see BandGyroscopeEventListener
 */
public class BandService extends SensorService implements BandGyroscopeEventListener, BandHeartRateEventListener {

    /** used for debugging purposes */
    private static final String TAG = BandService.class.getName();

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient bandClient = null;

    private StepDetector stepDetector;

    /**
     * The step count as predicted by your server-side step detection algorithm.
     */
    private int serverStepCount = 0;

    public BandService(){
        stepDetector = new StepDetector();
    }

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STOPPED);
    }

    @Override
    public void onConnected() {
        super.onConnected();
        client.registerMessageReceiver(new MessageReceiver(MHLClientFilter.STEP) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                Log.d(TAG, "Received step update from server.");
                try {
                    JSONObject data = json.getJSONObject("data");
                    long timestamp = data.getLong("timestamp");
                    Log.d(TAG, "Step occurred at " + timestamp + ".");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                serverStepCount++;
                broadcastServerStepCount(serverStepCount);
            }
        });
        client.registerMessageReceiver(new MessageReceiver(MHLClientFilter.ACTIVITY) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                Log.d(TAG, "Received activity update from server.");
                String activity;
                try {
                    JSONObject data = json.getJSONObject("data");
                    activity = data.getString("activity");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                Log.d(TAG, "Activity is : " + activity);
                broadcastActivity(activity);
            }
        });
    }

    /**
     * Broadcasts the step count computed by your server-side step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastActivity(String activity) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.ACTIVITY, activity);
        intent.setAction(Constants.ACTION.BROADCAST_ACTIVITY);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    @Override
    public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
        broadcastStatus(bandHeartRateEvent.getHeartRate()+"");
        HRSensorReading reading = new HRSensorReading(userID, "", "", bandHeartRateEvent.getTimestamp(), bandHeartRateEvent.getHeartRate());
        client.sendSensorReading(reading);
    }

    /**
     * Asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors.
     * Errors may arise if the Band does not support the Band SDK version or the Microsoft Health
     * application is not installed on the mobile device.
     **
     * @see com.microsoft.band.BandErrorType#UNSUPPORTED_SDK_VERSION_ERROR
     * @see com.microsoft.band.BandErrorType#SERVICE_ERROR
     * @see BandClient#getSensorManager()
     * @see com.microsoft.band.sensors.BandSensorManager
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    broadcastStatus(getString(R.string.status_connected));
                    bandClient.getSensorManager().registerGyroscopeEventListener(BandService.this, SampleRate.MS16);
                    bandClient.getSensorManager().registerHeartRateEventListener(BandService.this);
                } else {
                    broadcastStatus(getString(R.string.status_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getString(R.string.err_unsupported_sdk_version);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getString(R.string.err_service);
                        break;
                    default:
                        exceptionMessage = getString(R.string.err_default) + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);
                broadcastStatus(exceptionMessage);

            } catch (Exception e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Connects the mobile device to the Microsoft Band
     * @return True if successful, False otherwise
     * @throws InterruptedException if the connection is interrupted
     * @throws BandException if the band SDK version is not compatible or the Microsoft Health band is not installed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                broadcastStatus(getString(R.string.status_not_paired));
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            return true;
        }

        broadcastStatus(getString(R.string.status_connecting));
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    @Override
    protected void registerSensors() {
        // register a listener to receive step events
        stepDetector.registerOnStepListener(new OnStepListener() {
            @Override
            public void onStepCountUpdated(int stepCount) {
                broadcastLocalStepCount(stepCount);
            }

            @Override
            public void onStepDetected(long timestamp, float[] values) {
                broadcastStepDetected(timestamp, values);
            }
        });
        new SensorSubscriptionTask().execute();
    }

    /**
     * Broadcasts the step count computed by your step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastLocalStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_LOCAL_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts a step event to other application components, e.g. the main UI.
     */
    public void broadcastStepDetected(long timestamp, float[] values) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_VALUE, values);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_PEAK);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * unregisters the sensors from the sensor service
     */
    @Override
    public void unregisterSensors() {
        if (bandClient != null) {
            try {
                bandClient.getSensorManager().unregisterAllListeners();
                disconnectBand();
            } catch (BandIOException e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
        }
        if (client != null)
            client.unregisterMessageReceivers();
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.activity_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_running_white_24dp;
    }

    /**
     * disconnects the sensor service from the Microsoft Band
     */
    public void disconnectBand() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        float GRAVITY=9.8f;
        float accelerationX = event.getAccelerationX()*GRAVITY;
        float accelerationY = event.getAccelerationY()*GRAVITY;
        float accelerationZ = event.getAccelerationZ()*GRAVITY;
        float gyroscopeX = event.getAngularVelocityX()*GRAVITY;
        float gyroscopeY = event.getAngularVelocityY()*GRAVITY;
        float gyroscopeZ = event.getAngularVelocityZ()*GRAVITY;
        //TODO: Remove code from starter code
        Object[] data = new Object[]{event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()};
        client.sendSensorReading(new AccelerometerReading(userID, "", "", event.getTimestamp(),
                accelerationX, accelerationY, accelerationZ));
        broadcastAccelerometerReading(event.getTimestamp(),
                accelerationX, accelerationY, accelerationZ);
        client.sendSensorReading(new GyroscopeReading(userID, "", "", event.getTimestamp(),
                gyroscopeX, gyroscopeY, gyroscopeZ));
        String sample = TextUtils.join(",", data);
        Log.d(TAG, sample);
        stepDetector.detectSteps(event.getTimestamp(), accelerationX, accelerationY, accelerationZ);
    }

    //TODO: Remove method from starter code
    /**
     * Broadcasts the accelerometer reading to other application components, e.g. the main UI.
     * @param accelerometerReadings the x, y, and z accelerometer readings
     */
    public void broadcastAccelerometerReading(final long timestamp, final float... accelerometerReadings) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_DATA, accelerometerReadings);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


    /**
     * Broadcasts the step count computed by your server-side step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastServerStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_SERVER_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    private class HeartBeatReading {
    }
}