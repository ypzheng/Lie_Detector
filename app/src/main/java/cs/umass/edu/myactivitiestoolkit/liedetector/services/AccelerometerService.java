package cs.umass.edu.myactivitiestoolkit.liedetector.services;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.communication.MHLClientFilter;
import cs.umass.edu.myactivitiestoolkit.liedetector.processing.Filter;
import cs.umass.edu.myactivitiestoolkit.liedetector.steps.OnStepListener;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.liedetector.steps.StepDetector;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.client.MobileIOClient;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;

/**
 * This service is responsible for collecting the accelerometer data on
 * the phone. It is an ongoing foreground service that will run even when your
 * application is not running. Note, however, that a process of your application
 * will still be running! The sensor service will receive sensor events in the
 * {@link #onSensorChanged(SensorEvent)} method defined in the {@link SensorEventListener}
 * interface.
 * <br><br>
 * <b>ASSIGNMENT 0 (Data Collection & Visualization)</b> :
 *      In this assignment, you will display and visualize the accelerometer readings
 *      and send the data to the server. In {@link #onSensorChanged(SensorEvent)},
 *      you should send the data to the main UI using the method
 *      {@link #broadcastAccelerometerReading(long, float[])}. You should also
 *      use the {@link #client} object to send data to the server. You can
 *      confirm it works by checking that both the local and server-side plots
 *      are updating (make sure your html script is running on your machine!).
 * <br><br>
 *
 * <b>ASSIGNMENT 1 (Step Detection)</b> :
 *      In this assignment, you will detect steps using the accelerometer sensor. You
 *      will design both a local step detection algorithm and a server-side (Python)
 *      step detection algorithm. Your algorithm should look for peaks and account for
 *      the fact that humans generally take steps every 0.5 - 2.0 seconds. Your local
 *      and server-side algorithms may be functionally identical, or you may choose
 *      to take advantage of other Python tools/libraries to improve performance.
 *      Call your local step detection algorithm from {@link #onSensorChanged(SensorEvent)}.
 *      <br><br>
 *      To listen for messages from the server,
 *      register a {@link MessageReceiver} with the {@link #client} and override
 *      the {@link MessageReceiver#onMessageReceived(JSONObject)} method to handle
 *      the message appropriately. The data will be received as a {@link JSONObject},
 *      which you can parse to acquire the step count reading.
 *      <br><br>
 *      We have provided you with the reading computed by the Android built-in step
 *      detection algorithm as an example and a ground-truth reading that you may
 *      use for comparison. Note that although the built-in algorithm has empirically
 *      been shown to work well, it is not perfect and may be sensitive to the phone
 *      orientation. Also note that it does not update the step count immediately,
 *      so don't be surprised if the step count increases suddenly by a lot!
 *  <br><br>
 *
 * <b>ASSIGNMENT 2 (Activity Detection)</b> :
 *      In this assignment, you will classify the user's activity based on the
 *      accelerometer data. The only modification you should make to the mobile
 *      app is to register a listener which will parse the activity from the acquired
 *      {@link org.json.JSONObject} and update the UI. The real work, that is
 *      your activity detection algorithm, will be running in the Python script
 *      and acquiring data from the server.
 *
 * @author Sean Noran
 *
 * @see android.app.Service
 * @see <a href="http://developer.android.com/guide/components/services.html#Foreground">
 * Foreground Service</a>
 * @see SensorEventListener#onSensorChanged(SensorEvent)
 * @see SensorEvent
 * @see MobileIOClient
 */
public class AccelerometerService extends SensorService implements SensorEventListener {

    /** Used during debugging to identify logs by class */
    private static final String TAG = AccelerometerService.class.getName();

    /** Sensor Manager object for registering and unregistering system sensors */
    private SensorManager mSensorManager;

    /** Manages the physical accelerometer sensor on the phone. */
    private Sensor mAccelerometerSensor;

    /** Android built-in step detection sensor **/
    private Sensor mStepSensor;

    /** Defines your step detection algorithm. **/
    private final StepDetector stepDetector;

    /**
     * The step count as predicted by the Android built-in step detection algorithm.
     */
    private int androidStepCount = 0;

    /**
     * The step count as predicted by your server-side step detection algorithm.
     */
    private int serverStepCount = 0;

    private Filter filter; // <SOLUTION/ A1>

    public AccelerometerService(){
        //<SOLUTION A1>
        filter = new Filter(3);
        //</SOLUTION A1>
        stepDetector = new StepDetector();
    }

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.ACCELEROMETER_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.ACCELEROMETER_SERVICE_STOPPED);
        if (client != null)
            client.unregisterMessageReceivers();
    }

    @Override
    public void onConnected() {
        super.onConnected();

        client.registerMessageReceiver(new MessageReceiver(MHLClientFilter.AVERAGE_ACCELERATION) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                Log.d(TAG, "Received average acceleration from server.");
                try {
                    JSONObject data = json.getJSONObject("data");
                    float average_X = (float)data.getDouble("average_X");
                    float average_Y = (float)data.getDouble("average_Y");
                    float average_Z = (float)data.getDouble("average_Z");
                    broadcastAverageAcceleration(average_X, average_Y, average_Z);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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
     * Register accelerometer sensor listener
     */
    @Override
    protected void registerSensors(){
        // TODO (Assignment 0) : Register the accelerometer sensor using the sensor manager
        //<SOLUTION A0>
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (mSensorManager == null){
            Log.e(TAG, Constants.ERROR_MESSAGES.ERROR_NO_SENSOR_MANAGER);
            Toast.makeText(getApplicationContext(), Constants.ERROR_MESSAGES.ERROR_NO_SENSOR_MANAGER,Toast.LENGTH_LONG).show();
            return;
        }

        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mAccelerometerSensor != null) {
            mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(getApplicationContext(), Constants.ERROR_MESSAGES.ERROR_NO_ACCELEROMETER, Toast.LENGTH_LONG).show();
            Log.w(TAG, Constants.ERROR_MESSAGES.ERROR_NO_ACCELEROMETER);
        }
        //</SOLUTION A0>

        // TODO (Assignment 1) : Register the built-in Android step detector (API 19 or higher)
        //<SOLUTION A1>
        // built-in step detector only available for API level 19 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        //</SOLUTION A1>

        // TODO (Assignment 1) : Register accelerometer with step detector and register on step listener for sending steps to UI
        //<SOLUTION A1>
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
        mSensorManager.registerListener(stepDetector, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        //</SOLUTION A1>
    }

    /**
     * Unregister the sensor listener, this is essential for the battery life!
     */
    @Override
    protected void unregisterSensors() {
        // TODO : Unregister sensors
        //<SOLUTION A0/A1>
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
            mSensorManager.unregisterListener(stepDetector, mAccelerometerSensor);
            mSensorManager.unregisterListener(this, mStepSensor);
        }
        //</SOLUTION A0/A1>
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

    //This method is called when we receive a sensor reading. We will be interested in this method primarily.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //<SOLUTION A1>
            double[] filteredValues = filter.getFilteredValues(event.values[0], event.values[1], event.values[2]);
            client.sendSensorReading(new AccelerometerReading(userID, "MOBILE", "", event.timestamp, (float)filteredValues[0], (float)filteredValues[1], (float)filteredValues[2]));

            float[] floatFilteredValues = new float[]{(float) filteredValues[0], (float) filteredValues[1], (float) filteredValues[2]};
            broadcastAccelerometerReading(event.timestamp, floatFilteredValues);
            //</SOLUTION A1>

        }else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            broadcastAndroidStepCount(androidStepCount++);
        } else {
            Log.w(TAG, Constants.ERROR_MESSAGES.WARNING_SENSOR_NOT_SUPPORTED);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed: " + accuracy);
    }

    /**
     * Broadcasts the accelerometer reading to other application components, e.g. the main UI.
     * @param accelerometerReadings the x, y, and z accelerometer readings
     */
    public void broadcastAccelerometerReading(final long timestamp, final float[] accelerometerReadings) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_DATA, accelerometerReadings);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts the step count computed by the Android built-in step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastAndroidStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_ANDROID_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
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

    /**
     * Broadcasts the step count computed by your server-side step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastAverageAcceleration(float average_X, float average_Y, float average_Z) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.AVERAGE_ACCELERATION, new float[]{average_X, average_Y, average_Z});
        intent.setAction(Constants.ACTION.BROADCAST_AVERAGE_ACCELERATION);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


}
