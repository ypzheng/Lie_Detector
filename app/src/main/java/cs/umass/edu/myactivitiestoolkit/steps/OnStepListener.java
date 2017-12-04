package cs.umass.edu.myactivitiestoolkit.steps;

/**
 * Clients may register an OnStepListener to receive step events and step count
 * notifications.
 */
public interface OnStepListener {
    void onStepCountUpdated(int stepCount);
    void onStepDetected(long timestamp, float[] values);
}
