package cs.umass.edu.myactivitiestoolkit.ppg;

/**
 * Wrapper for a PPG sensor reading. It contains a red value and a timestamp.
 * Note the difference from {@link PPGSensorReading}, which is intended to
 * be sent to the data collection server. This class is only intended for
 * local use.
 *
 * @author Sean Noran
 */
public class PPGEvent {
    public double value;
    public long timestamp;

    public PPGEvent(final double value, final long timestamp){
        this.value = value;
        this.timestamp = timestamp;
    }
}
