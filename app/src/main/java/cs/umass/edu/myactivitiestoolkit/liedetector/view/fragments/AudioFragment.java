package cs.umass.edu.myactivitiestoolkit.liedetector.view.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.AudioService;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.PPGService;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.msband.BandService;
import cs.umass.edu.myactivitiestoolkit.liedetector.util.PermissionsUtil;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.ServiceManager;

public class AudioFragment extends Fragment {


    @SuppressWarnings("unused")
    /** Used during debugging to identify logs by class */
    private static final String TAG = AudioFragment.class.getName();

    /** Request code required for obtaining audio recording permission. **/
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 5;

    /** The image displaying the audio spectrogram. **/

    /** The switch which toggles the {@link AudioService}. **/
    private Switch switchRecord;

    /** Reference to the service manager which communicates to the {@link PPGService}. **/
    private ServiceManager serviceManager;

    private TextView txtSpeaker;
    private TextView num_truth;
    private TextView num_lie;
    private TextView percent_truth;

    private int value_truth;
    private int value_lie;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio, container, false);
        switchRecord = (Switch) rootView.findViewById(R.id.switchMicrophone);
        switchRecord.setChecked(serviceManager.isServiceRunning(AudioService.class));
        switchRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                if (enable){
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    boolean onMSBand = preferences.getBoolean(getString(R.string.pref_msband_key),
                            getResources().getBoolean(R.bool.pref_msband_default));
                    if (onMSBand){
                        requestPermissions();
                    }
                } else {
                    serviceManager.stopSensorService(AudioService.class);
                    serviceManager.stopSensorService(BandService.class);
                }
            }
        });
        value_lie = 0;
        value_truth = 0;
        txtSpeaker = (TextView) rootView.findViewById(R.id.txtSpeaker);
        num_truth = (TextView) rootView.findViewById(R.id.num_truth);
        num_lie = (TextView) rootView.findViewById(R.id.num_lie);
        percent_truth = (TextView) rootView.findViewById(R.id.percent_truth);
        return rootView;
    }

    /**
     * When the fragment starts, register a {@link #receiver} to receive messages from the
     * {@link AudioService}. The intent filter defines messages we are interested in receiving.
     * <br><br>
     *
     * Unlike the {@link} and {@link HeartRateFragment}, we do not visualize
     * the raw data. For this reason, there is no need to listen for it from the main UI. We
     * would, however, like to display a spectrogram of the audio data. To do this, we listen for
     * {@link Constants.ACTION#BROADCAST_SPECTROGRAM}.
     * <br><br>
     *
     * Lastly to update the state of the audio switch properly, we listen for additional
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
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
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
     * Called when the user has granted audio recording permission.
     */
    public void onPermissionGranted(){
        serviceManager.startSensorService(AudioService.class);
        serviceManager.startSensorService(BandService.class);
        value_truth = 0;
        value_lie = 0;
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};

        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            requestPermissions(permissions, AUDIO_PERMISSION_REQUEST_CODE);
            return;
        }

        onPermissionGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
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
            }
        }
    }

    /**
     * Converts the value to a corresponding heat map color
     * @param minimum the minimum bound on the value
     * @param maximum the maximum bound on the value
     * @param value the value, within the range [minimum, maximum]
     * @return an RGB color identifier
     *
     * @see <a href="http://stackoverflow.com/questions/20792445/calculate-rgb-value-for-a-range-of-values-to-create-heat-map">Aldorado's answer.</a>
     */
    private int heatMap(double minimum, double maximum, double value) {
        double ratio = 2 * (value - minimum) / (maximum - minimum);
        int b = (int) Math.max(0, 255 * (1 - ratio));
        int r = (int) Math.max(0, 255 * (ratio - 1));
        int g = 255 - b - r;
        return (r<<16|g<<8|b|255<<24);
    }

    /**
     * The receiver listens for messages from the {@link AccelerometerService}, e.g. was the
     * service started/stopped, and updates the status views accordingly. It also
     * listens for sensor data and displays the sensor readings to the user.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)) {
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.AUDIO_SERVICE_STOPPED) {
                        switchRecord.setChecked(false);
                    }
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_SPEAKER)) {
                    final String speaker = intent.getStringExtra(Constants.KEY.SPEAKER);
                    if(speaker.equals("truth")){
                        value_truth++;
                    }
                    else{
                        value_lie++;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtSpeaker.setText("Truth or Lie: " + speaker);
                            num_truth.setText("Truth: " + value_truth);
                            num_lie.setText("Lie: " + value_lie);
                            float percent = ((float)value_truth/((float)value_truth+(float)value_lie))*100;
                            percent_truth.setText("Truth: "+ percent+"%");
                        }
                    });
                }

            }

        }
    };

}