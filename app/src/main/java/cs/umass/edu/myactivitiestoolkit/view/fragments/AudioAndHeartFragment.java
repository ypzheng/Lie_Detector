package cs.umass.edu.myactivitiestoolkit.view.fragments;

/**
 * Created by Andy on 12/4/2017.
 */
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.AudioService;
import cs.umass.edu.myactivitiestoolkit.services.PPGService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.util.PermissionsUtil;

public class AudioAndHeartFragment extends Fragment{

    private static final String TAG = AudioAndHeartFragment.class.getName();

    /** ------------------------Heart Rate Setup----------------------------------**/
    /** Request code required for obtaining camera usage permission for photoplethysmography. **/
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    /** Request code required for obtaining overlay permission for overlaying the camera preview. **/
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 2;

    /** The switch object which toggles the PPG service. **/
    private Switch switchHeartRate;

    /** Displays the heart rate computed by your PPG algorithm. **/
    private TextView txtHeartRate;

    /** The plot which displays the PPG data in real-time. **/
    private XYPlot plot;

    /** The series formatter that defines how the PPG signal should be displayed. **/
    private LineAndPointFormatter ppgSeriesFormatter;

    /** The series formatter that defines how the PPG peaks should be displayed. **/
    private LineAndPointFormatter peakSeriesFormatter;

    /** Defines the lower bound on the PPG signal used for displaying the plot. Although
     * the lower bound on the mean red value is theoretically 0, it is unlikely that we
     * would get such a value when the finger is pressed up against the camera. In fact,
     * we've empirically found that the PPG signal should not fall below 215. If PPG
     * values fall below this threshold, that is perfectly OK, it will just not be visible
     * in the plot. **/
    private static final int PPG_LOWER_BOUND = 215;

    /** Defines the upper bound on the PPG signal used for displaying the plot. The maximum
     * mean red value is 255. **/
    private static final int PPG_UPPER_BOUND = 255;

    /** The number of data points to display in the graph. **/
    private static final int GRAPH_CAPACITY = 100;

    /** The number of points displayed on the plot. This should only ever be less than
     * {@link #GRAPH_CAPACITY} before the plot is fully populated. **/
    private int numberOfPoints = 0;

    /**
     * The queue of the previous {@link #GRAPH_CAPACITY} PPG timestamps.
     */
    private final Queue<Number> ppgTimestamps = new LinkedList<>();
    /**
     * The queue of the previous {@link #GRAPH_CAPACITY} PPG values.
     */
    private final Queue<Number> ppgValues = new LinkedList<>();
    /**
     * The queue of peak timestamps.
     */
    private final Queue<Number> peakTimestamps = new LinkedList<>();
    /**
     * The queue of peak values.
     */
    private final Queue<Number> peakValues = new LinkedList<>();

    /** Reference to the service manager which communicates to the {@link PPGService}. **/
    private ServiceManager serviceManager;

    /** ------------------------Heart Rate Setup End----------------------------------**/

    /** ------------------------Audio Setup----------------------------------**/

    private static final int AUDIO_PERMISSION_REQUEST_CODE = 5;

    /** The switch which toggles the {@link AudioService}. **/
    private Switch switchRecord;

    private TextView txtSpeaker;

    /** The image displaying the audio spectrogram. **/
    private ImageView imgSpectrogram;
    /** ------------------------Audio Setup End----------------------------------**/


    /**
     * The receiver listens for sensor data from the {@link PPGService}. This includes
     * the raw PPG sensor data as well as peaks detected by your algorithm and the
     * current bpm measurement.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)) {
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.PPG_SERVICE_STOPPED){
                        switchHeartRate.setChecked(false);
                    }
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_HEART_RATE)) {
                    int heartRate = intent.getIntExtra(Constants.KEY.HEART_RATE, -1);
                    if (heartRate != -1)
                        displayHeartRate(heartRate);
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_PPG)){
                    long timestamp = intent.getLongExtra(Constants.KEY.TIMESTAMP, -1);
                    double ppg = intent.getDoubleExtra(Constants.KEY.PPG_DATA, -1);

                    ppgTimestamps.add(timestamp);
                    ppgValues.add(ppg);
                    if (numberOfPoints >= GRAPH_CAPACITY) {
                        ppgTimestamps.poll();
                        ppgValues.poll();
                        while (peakTimestamps.size() > 0 && (peakTimestamps.peek().longValue() < ppgTimestamps.peek().longValue())){
                            peakTimestamps.poll();
                            peakValues.poll();
                        }
                    }
                    else
                        numberOfPoints++;

                    //updatePlot();

                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_PPG_PEAK)){
                    long timestamp = intent.getLongExtra(Constants.KEY.PPG_PEAK_TIMESTAMP, -1);
                    double value = intent.getDoubleExtra(Constants.KEY.PPG_PEAK_VALUE, -1);
                    if (timestamp > 0) {
                        peakTimestamps.add(timestamp);
                        peakValues.add(value);
                    }
                }else if (intent.getAction().equals(Constants.ACTION.BROADCAST_SPECTROGRAM)){
                    double[][] spectrogram = (double[][]) intent.getSerializableExtra(Constants.KEY.SPECTROGRAM);
                    //updateSpectrogram(spectrogram);
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_SPEAKER)){
                    final String speaker = intent.getStringExtra(Constants.KEY.SPEAKER);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtSpeaker.setText(speaker);
                        }
                    });
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_audio_heart, container, false);

        //AUDIO START************************
        switchRecord = (Switch) view.findViewById(R.id.switchMicrophone);
        switchRecord.setChecked(serviceManager.isServiceRunning(AudioService.class));
        switchRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                if (enable){

                    requestPermissions();

                } else {
                    serviceManager.stopSensorService(AudioService.class);
                }
            }
        });
        imgSpectrogram = (ImageView) view.findViewById(R.id.imgSpectrogram);
        txtSpeaker = (TextView) view.findViewById(R.id.txtSpeaker);

        //AUDIO END***************************
        txtHeartRate = (TextView) view.findViewById(R.id.txtHeartRate);


        switchHeartRate = (Switch) view.findViewById(R.id.switchHeartRate);
        switchHeartRate.setChecked(serviceManager.isServiceRunning(PPGService.class));
        switchHeartRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                if (enabled){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions();
                    else
                        onVideoPermissionsGranted();
                }else{
                    serviceManager.stopSensorService(PPGService.class);
                }
            }
        });

        /*// initialize plot and set plot parameters
        plot = (XYPlot) view.findViewById(R.id.plot);
        plot.setRangeBoundaries(PPG_LOWER_BOUND, PPG_UPPER_BOUND, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.SUBDIVIDE, 4);
        plot.getGraph().getDomainOriginLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getLayoutManager().remove(plot.getLegend());
        plot.setLinesPerDomainLabel(1);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                return toAppendTo.append("");
            }
            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });
        plot.setPlotPaddingBottom(-150); //TODO: This isn't device-dependent, and may need to be changed.

        // set formatting parameters for each signal (PPG and PPG peaks)
        ppgSeriesFormatter = new LineAndPointFormatter(Color.RED, null, ContextCompat.getColor(getActivity(), R.color.background_heart_rate), null);
        peakSeriesFormatter = new LineAndPointFormatter(null, Color.BLUE, null, null);
        peakSeriesFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10)); //enlarge the peak points
*/
        return view;
    }

    /**
     * When the fragment starts, register a {@link #receiver} to receive messages from the
     * {@link PPGService}. The intent filter defines messages we are interested in receiving.
     * <br><br>
     *
     * We would like to receive sensor data, so we specify {@link Constants.ACTION#BROADCAST_PPG}.
     * We would also like to receive heart rate estimates, so include {@link Constants.ACTION#BROADCAST_HEART_RATE}.
     * <br><br>
     *
     * To optionally display the peak values you compute, include
     * {@link Constants.ACTION#BROADCAST_PPG_PEAK}.
     * <br><br>
     *
     * Lastly to update the state of the PPG switch properly, we listen for additional
     * messages, using {@link Constants.ACTION#BROADCAST_MESSAGE}.
     *
     * @see Constants.ACTION
     * @see IntentFilter
     * @see LocalBroadcastManager
     * @see #receiver
     */
    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter filter = new IntentFilter();

        //Heart Rate
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        filter.addAction(Constants.ACTION.BROADCAST_PPG);
        filter.addAction(Constants.ACTION.BROADCAST_HEART_RATE);
        filter.addAction(Constants.ACTION.BROADCAST_PPG_PEAK);

        //Audio
        filter.addAction(Constants.ACTION.BROADCAST_SPECTROGRAM);
        filter.addAction(Constants.ACTION.BROADCAST_SPEAKER);

        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * When the fragment stops, e.g. the user closes the application or opens a new activity,
     * then we should unregister the {@link #receiver}.
     */
    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    /**
     * Request permissions required for video recording. These include
     * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        Log.d(TAG, "Requesting permission to use camera...");
        List<String> permissionGroup = new ArrayList<>(Arrays.asList(new String[]{
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
        }));

        String[] permissions = permissionGroup.toArray(new String[permissionGroup.size()]);

        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            FragmentCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            requestPermissions(permissions, AUDIO_PERMISSION_REQUEST_CODE);
            return;
        }
        onPermissionGranted();
        Log.d(TAG, "Permission already granted");
        checkDrawOverlayPermission();
    }


    public void onPermissionGranted(){

        serviceManager.startSensorService(AudioService.class);
    }


    /**
     * Check the draw overlay permission. This is required to run the video recording service in
     * a background service.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (!Settings.canDrawOverlays(getContext().getApplicationContext())) {
            /** if not, construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse(getString(R.string.app_package_identifier_prefix) + getActivity().getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }else{
            onVideoPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.CAMERA:
                                Toast.makeText(getActivity(), "You must grant camera permission to acquire PPG data.", Toast.LENGTH_LONG).show();
                                return;
                            default:
                                return;
                        }
                    }
                }
                checkDrawOverlayPermission();
                break;
            }
            case AUDIO_PERMISSION_REQUEST_CODE: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.RECORD_AUDIO:
                                //TODO: Show status
                                return;
                            default:
                                return;
                        }
                    }
                }
                onPermissionGranted();
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(getContext())) {
                    onVideoPermissionsGranted();
                } else {
                    Toast.makeText(getActivity(), "You must grant overlay permission to run the camera in the background.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Called when the video recording permission has been granted by the user. This
     * is the cue to start the {@link PPGService}.
     */
    private void onVideoPermissionsGranted(){
        Log.d(TAG, "Video permission granted");
        //clearPlotData();
        serviceManager.startSensorService(PPGService.class);
    }

    /**
     * Displays the heart rate as computed by your PPG algorithm.
     * @param heartRate the heart rate measurement in bpm.
     */
    private void displayHeartRate(final int heartRate){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtHeartRate.setText(String.valueOf(heartRate));
            }
        });
    }

    /**
     * Clears the PPG and peak plot data series.
     *//*
    private void clearPlotData(){
        peakTimestamps.clear();
        peakValues.clear();
        ppgTimestamps.clear();
        ppgValues.clear();
        numberOfPoints = 0;
    }

    *//**
     * Updates and redraws the PPG plot, along with peaks detected.
     *//*
    private void updatePlot(){
        XYSeries series1 = new SimpleXYSeries(new ArrayList<>(ppgTimestamps), new ArrayList<>(ppgValues), "PPG");
        XYSeries peaks = new SimpleXYSeries(new ArrayList<>(peakTimestamps), new ArrayList<>(peakValues), "PPG_PEAKS");

        //redraw the plot:
        plot.clear();
        plot.addSeries(series1, ppgSeriesFormatter);
        plot.addSeries(peaks, peakSeriesFormatter);
        plot.redraw();
    }*/


/*
    *//**
     * Converts the spectrogram values into a heat map and projects the pixels onto a bitmap.
     * @param spectrogram the spectrogram values as a 2D array.
     *//*
    private void updateSpectrogram(double[][] spectrogram){
        int width = spectrogram.length;
        int height = spectrogram[0].length;

        int[] rgbValues = new int[width * height];

        double max = 0, min = 0;
        for (int j = 0; j < height; j++) {
            for (double[] row : spectrogram) {
                if (row[j] > max) {
                    max = row[j];
                }
                if (row[j] < min) {
                    min = row[j];
                }
            }
        }
        int counter = 0;
        for (int j = 0; j < height/2; j++) {
            for (double[] row : spectrogram) {
                rgbValues[counter++] = heatMap(min, max, row[j]);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(rgbValues, width, height, Bitmap.Config.ARGB_8888);
        imgSpectrogram.setImageBitmap(bitmap);
    }

    private int heatMap(double minimum, double maximum, double value) {
        double ratio = 2 * (value - minimum) / (maximum - minimum);
        int b = (int) Math.max(0, 255 * (1 - ratio));
        int r = (int) Math.max(0, 255 * (ratio - 1));
        int g = 255 - b - r;
        return (r<<16|g<<8|b|255<<24);
    }*/


}
