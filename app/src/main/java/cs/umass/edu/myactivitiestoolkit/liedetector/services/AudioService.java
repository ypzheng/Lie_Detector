package cs.umass.edu.myactivitiestoolkit.liedetector.services;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.audio.AudioBufferReading;
import cs.umass.edu.myactivitiestoolkit.liedetector.audio.MicrophoneRecorder;
import cs.umass.edu.myactivitiestoolkit.liedetector.communication.MHLClientFilter;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import edu.umass.cs.MHLClient.client.MessageReceiver;

public class AudioService extends SensorService implements MicrophoneRecorder.MicrophoneListener {

    /** Used during debugging to identify logs by class */
    @SuppressWarnings("unused")
    private static final String TAG = AudioService.class.getName();

    /** The sensor responsible for collecting audio data from the phone. */
    private MicrophoneRecorder microphoneRecorder;

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.AUDIO_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.AUDIO_SERVICE_STOPPED);
    }

    protected void registerSensors() {
        microphoneRecorder = MicrophoneRecorder.getInstance(this);

        Log.d(TAG, "Starting microphone.");
        microphoneRecorder.registerListener(this);
        microphoneRecorder.startRecording();

    }

    protected void unregisterSensors() {
        if (microphoneRecorder != null) {
            microphoneRecorder.unregisterListener(this);
            microphoneRecorder.stopRecording();
        }
    }

    @Override
    public void onConnected() {
        client.registerMessageReceiver(new MessageReceiver(MHLClientFilter.SPEAKER) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                String speaker;
                try {
                    JSONObject data = json.getJSONObject("data");
                    speaker = data.getString("speaker");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                broadcastSpeaker(speaker);
            }
        });
        super.onConnected();
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.AUDIO_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.audio_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_mic_white_24dp;
    }

    /**
     * Broadcasts spectrogram of audio data.
     * ter@param spectrogram 2d array of values
     */
    public void broadcastSpectrogram(double[][] spectrogram) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.SPECTROGRAM, spectrogram);
        Log.d(TAG, spectrogram.length + ", " + spectrogram[0].length);
//        intent.putExtra(Constants.KEY.WIDTH, spectrogram);
//        intent.putExtra(Constants.KEY.HEIGHT, spectrogram);
        intent.setAction(Constants.ACTION.BROADCAST_SPECTROGRAM);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts speaker.
     */
    public void broadcastSpeaker(String speaker) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.SPEAKER, speaker);
        intent.setAction(Constants.ACTION.BROADCAST_SPEAKER);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    @Override
    public void microphoneBuffer(short[] buffer, int window_size) {
        Log.d(TAG, String.valueOf(buffer.length));
        client.sendSensorReading(new AudioBufferReading(userID, "MOBILE", "", System.currentTimeMillis(), buffer));
//        //convert short[] to double[]:
//        double[] dBuffer = new double[buffer.length];
//        for (int j=0;j<buffer.length;j++) {
//            dBuffer[j] = buffer[j];
//        }
//        //compute spectrogram features
//        double[][] spectrogram = Spectrogram.computeSpectrogram(dBuffer, 100, 50, Window.RECTANGULAR);
//        broadcastSpectrogram(spectrogram);
//        //computes statistical spectrum descriptor, rhythm pattern and rhythm histogram
//        FeatureExtractionOptions opts = new FeatureExtractionOptions();
//        opts.extractRP = false;
//        RealMatrixExt[] m = new FeatureExtractor(MicrophoneRecorder.frequency, 16).extractFeatureSets(buffer, opts);
//        double[] SSDVector = m[0].vectorize();
////        double[] RPVector = m[1].vectorize();
//        double[] RHVector = m[1].vectorize();
//        double[] featureVector = new double[SSDVector.length + RHVector.length];
//        int index=0;
//        for (double v : SSDVector) {
//            featureVector[index++] = v;
//        }
//        Log.d(TAG, SSDVector.length + " SSD features.");
////        for (double v : RPVector) {
////            featureVector[index++] = v;
////        }
////        Log.d(TAG, RPVector.length + " RP features.");
//        for (double v : RHVector) {
//            featureVector[index++] = v;
//        }
//        Log.d(TAG, RHVector.length + " RH features.");
//        Log.d(TAG, "length: " + featureVector.length);
//        client.sendSensorReading(new AudioBufferReading(userID, "", "", System.currentTimeMillis(), featureVector));
//        int windowSize = 200;
//        for (int i = 0; i < buffer.length; i += 200) {
//            double[] mfccFeatures = MFCCFeatureExtractor.computeFeaturesForFrame(buffer, windowSize, i);
//        }
    }
}
