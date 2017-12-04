package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;

    //TODO: Remove some fields from starter code
    private final Filter mFilter;

    private double previousMagnitude = -1000000;

    private boolean previousPositive;

    /** Stores the timestamp of the previous step. **/
    private long previousStepTimestamp;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    public StepDetector(){
        mFilter = new Filter(3);
        mStepListeners = new ArrayList<>();
        stepCount = 0;
    }

    /**
     * Registers a step listener for handling step events.
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener){
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener){
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners(){
        mStepListeners.clear();
    }

    public void detectSteps(long timestamp_in_milliseconds, float... values){
// TODO: Forgot to convert timestamp in starter code. At least it should be consistent with Python code...
        // TODO: Also forgot to filter data sent to the server the same way as this data stream

        if(timestamp_in_milliseconds - previousStepTimestamp < 500) return;

        double magnitudeSq = 0;
        for (double v : values){
            magnitudeSq += v * v;
        }
        double currentMagnitude = Math.sqrt(magnitudeSq);

        //FYI: the variable that holds the previous point value is initialized to -1000000
        //this block is only for the first point bc the if statement should only be true for that first point,
        //bc we initialized previousMagnitude to -1000000
        if(previousMagnitude == -1000000)
        {
            previousMagnitude = currentMagnitude;
            //not sure whether to initialize this to false or true
            previousPositive = false;
        }

        boolean currentPositive = currentMagnitude > previousMagnitude;

        //ensures that slope is significant enough to be detected as a step, disregards small fluctuations that shouldn't be steps
        if(Math.abs(currentMagnitude - previousMagnitude)>0.5)
        {
            //for ex: if previousMagnitude was positive and currentMagnitude is negative, the direction has changed, which
            //means a step was taken, so increment step counter
            if(!currentPositive && previousPositive)
            {
                //at the end, store current point as previous point so you're ready to compare the next point
                previousPositive = false;
                previousMagnitude = currentMagnitude;
                previousStepTimestamp = timestamp_in_milliseconds;
                onStepDetected(timestamp_in_milliseconds, values);
            } else {
                //at the end, store current point as previous point so you're ready to compare the next point
                previousPositive = currentPositive;
                previousMagnitude = currentMagnitude;
            }
        } else {
            //at the end, store current point as previous point so you're ready to compare the next point
            previousPositive = currentPositive;
            previousMagnitude = currentMagnitude;
        }
    }

    /**
     * Here is where you will receive accelerometer readings, buffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     *
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double[] filteredValues = mFilter.getFilteredValues(event.values);
            long timestamp_in_milliseconds = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);
            float[] floatFilteredValues = new float[filteredValues.length];
            for (int i = 0; i < filteredValues.length; i++){
                floatFilteredValues[i] = (float) filteredValues[i];
            }
            detectSteps(timestamp_in_milliseconds, floatFilteredValues);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    /**
     * This method is called when a step is detected. It updates the current step count,
     * notifies all listeners that a step has occurred and also notifies all listeners
     * of the current step count.
     */
    private void onStepDetected(long timestamp, float[] values){
        stepCount++;
        for (OnStepListener stepListener : mStepListeners){
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
