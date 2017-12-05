package cs.umass.edu.myactivitiestoolkit.liedetector.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.view.fragments.HeartRateFragment;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.HRSensorReading;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.PPGSensorReading;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.HeartRateCameraView;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.PPGEvent;
import cs.umass.edu.myactivitiestoolkit.liedetector.ppg.PPGListener;
import cs.umass.edu.myactivitiestoolkit.liedetector.processing.FFT;
import cs.umass.edu.myactivitiestoolkit.liedetector.processing.Filter;
import cs.umass.edu.myactivitiestoolkit.liedetector.util.Interpolator;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Photoplethysmography service. This service uses a {@link HeartRateCameraView}
 * to collect PPG data using a standard camera with continuous flash. This is where
 * you will do most of your work for this assignment.
 * <br><br>
 * <b>ASSIGNMENT (PHOTOPLETHYSMOGRAPHY)</b> :
 * In {@link #onSensorChanged(PPGEvent)}, you should smooth the PPG reading using
 * a {@link Filter}. You should send the filtered PPG reading both to the server
 * and to the {@link HeartRateFragment}
 * for visualization. Then call your heart rate detection algorithm, buffering the
 * readings if necessary, and send the bpm measurement back to the UI.
 * <br><br>
 * EXTRA CREDIT:
 *      Follow the steps outlined <a href="http://www.marcoaltini.com/blog/heart-rate-variability-using-the-phones-camera">here</a>
 *      to acquire a cleaner PPG signal. For additional extra credit, you may also try computing
 *      the heart rate variability from the heart rate, as they do.
 *
 * @author Sean Noran
 *
 * @see HeartRateCameraView
 * @see PPGEvent
 * @see PPGListener
 * @see Filter
 * @see MobileIOClient
 * @see PPGSensorReading
 * @see Service
 */
public class PPGService extends SensorService implements PPGListener
{
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = PPGService.class.getName();

    /* Surface view responsible for collecting PPG data and displaying the camera preview. */
    private HeartRateCameraView mPPGSensor;

    @Override
    protected void start() {
        Log.d(TAG, "START");
        mPPGSensor = new HeartRateCameraView(getApplicationContext(), null);

        WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        //surface view dimensions and position specified where service intent is called
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        //display the surface view as a stand-alone window
        winMan.addView(mPPGSensor, params);
        mPPGSensor.setZOrderOnTop(true);

        // only once the surface has been created can we start the PPG sensor
        mPPGSensor.setSurfaceCreatedCallback(new HeartRateCameraView.SurfaceCreatedCallback() {
            @Override
            public void onSurfaceCreated() {
                mPPGSensor.start(); //start recording PPG
            }
        });

        super.start();
    }

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.PPG_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        if (mPPGSensor != null)
            mPPGSensor.stop();
        if (mPPGSensor != null) {
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).removeView(mPPGSensor);
        }
        broadcastMessage(Constants.MESSAGE.PPG_SERVICE_STOPPED);
    }

    @Override
    protected void registerSensors() {
        mPPGSensor.registerListener(this);
    }

    @Override
    protected void unregisterSensors() {

    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.PPG_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.ppg_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_whatshot_white_48dp;
    }

    private final Filter filter = new Filter(4);

    private final FFT fft = new FFT(16);

    private final double[] heartRateData = new double[16];

    private final long[] heartRateTimestamps = new long[16];

    private int hrIndex = 0;

    /**
     * This method is called each time a PPG sensor reading is received.
     * <br><br>
     * You should smooth the data using {@link Filter} and then send the filtered data both
     * to the server and the main UI for real-time visualization. Run your algorithm to
     * detect heart beats, calculate your current bpm and send the bmp measurement to the
     * main UI. Additionally, it may be useful for you to send the peaks you detect to
     * the main UI, using {@link #broadcastPeak(long, double)}. The plot is already set up
     * to draw these peak points upon receiving them.
     *
     * @param event The PPG sensor reading, wrapping a timestamp and mean red value.
     *
     * @see PPGEvent
     * @see PPGSensorReading
     * @see HeartRateCameraView#onPreviewFrame(byte[], Camera)
     * @see MobileIOClient
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onSensorChanged(PPGEvent event) {
        //TODO: Smooth the signal using a Butterworth / exponential smoothing filter
        double value = filter.getFilteredValues((float) event.value)[0];
        //send the data to the UI fragment for visualization
        broadcastPPGReading(event.timestamp, value);
        //TODO: Send the filtered mean red value to the server
        client.sendSensorReading(new PPGSensorReading(userID, "MOBILE", "", event.timestamp, value));
        //TODO: Buffer data if necessary for your algorithm
        //TODO: Call your heart beat and bpm detection algorithm
        displayHeartRate(event.timestamp, value);
    }

    /**
     * Broadcasts the PPG reading to other application components, e.g. the main UI.
     * @param ppgReading the mean red value.
     */
    public void broadcastPPGReading(final long timestamp, final double ppgReading) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.PPG_DATA, ppgReading);
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.setAction(Constants.ACTION.BROADCAST_PPG);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts the current heart rate in BPM to other application components, e.g. the main UI.
     * @param bpm the current beats per minute measurement.
     */
    public void broadcastBPM(final int bpm) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.HEART_RATE, bpm);
        intent.setAction(Constants.ACTION.BROADCAST_HEART_RATE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts the current heart rate in BPM to other application components, e.g. the main UI.
     * @param timestamp the current beats per minute measurement.
     */
    public void broadcastPeak(final long timestamp, final double value) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.PPG_PEAK_TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.PPG_PEAK_VALUE, value);
        intent.setAction(Constants.ACTION.BROADCAST_PPG_PEAK);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    //TODO FIX THIS SHIT UP
    private double last;
    private long lastPeak;
    private double peakHeight;
    private boolean increasing;

    public void displayHeartRate(long timestamp, double meanRed)
    {
        if(checkPeak(meanRed))
        {
            broadcastPeak(timestamp, meanRed);
            if(lastPeak == 0)
                lastPeak = timestamp;
            else if(timestamp - lastPeak > 333){
                int bpm = calculateBPM(timestamp);
                heartRateData[hrIndex] = bpm;
                heartRateTimestamps[hrIndex++] = timestamp;
                if (hrIndex >= heartRateData.length){
                    hrIndex=0;
                    double[] fftBufferI = new double[16]; //TODO: Interpolate
                    double[] interpolatedHRData = Interpolator.linearInterpolate(heartRateTimestamps, heartRateData, 16);
                    fft.fft(interpolatedHRData, fftBufferI);
                    Log.d(TAG, " : " + interpolatedHRData[0]);
                }
                broadcastBPM(bpm);
                client.sendSensorReading(new HRSensorReading(userID, "MOBILE", "", timestamp, bpm));
                lastPeak = timestamp;
            }
        }
    }

    public int calculateBPM(long newPeak){
        return (int)(60000/(newPeak - lastPeak));
    }

    public boolean checkPeak(double current){
        if(increasing && current <= last){
            peakHeight = last;
            this.increasing = false;
        } else if(!increasing && current >= last){
            this.increasing = true;

            last = current;
            return checkBounds(current);
        }
        last = current;
        return false;
    }

    public boolean checkBounds(double current)
    {
        double minPeak = .01;
        return (current < peakHeight - minPeak);
    }
}