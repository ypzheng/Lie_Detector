package cs.umass.edu.myactivitiestoolkit.liedetector.services.msband;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;

import cs.umass.edu.myactivitiestoolkit.liedetector.R;
import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.liedetector.services.SensorService;
import cs.umass.edu.myactivitiestoolkit.liedetector.view.fragments.SettingsFragment;

/**
 * Created by hwl on 12/5/17.
 */

public class BandHeartRateService extends SensorService implements BandHeartRateEventListener {

    /** used for debugging purposes */
    private static final String TAG = BandService.class.getName();

    private BandClient bandClient = null;

    public SettingsFragment settingFrag = new SettingsFragment();
    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STOPPED);
    }

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if(bandClient.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED){
                        broadcastStatus(getString(R.string.status_connected));
                        bandClient.getSensorManager().registerHeartRateEventListener(BandHeartRateService.this);
                    }
                    else bandClient.getSensorManager().requestHeartRateConsent(settingFrag.getActivity(), new HeartRateConsentListener(){
                        @Override
                        public void userAccepted(boolean consentGiven) {
                        }
                    });
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

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {
                    if (params[0].get() != null) {
                        bandClient.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
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

    @Override
    public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {

    }

    /**
     * Registers the sensors. Subclasses should override this to define how sensors are registered.
     */
    @Override
    protected void registerSensors() {
        new HeartRateSubscriptionTask();
    }

    /**
     * Unregisters the sensors. Subclasses should override this to define how sensors are unregistered.
     */
    @Override
    protected void unregisterSensors() {
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

    public void disconnectBand() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    /**
     * Returns the unique ID of the notification. If a notification with the ID already exists,
     * it will be replaced with the new notification.
     *
     * @return a unique identifier.
     */
    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.PPG_SERVICE;
    }

    /**
     * Returns content text displayed in the notification.
     *
     * @return the content text of the notification.
     */
    @Override
    protected String getNotificationContentText() {
        return getString(R.string.ppg_service_notification);
    }

    /**
     * Returns the resource ID of the notification icon.
     *
     * @return an drawable ID
     */
    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_whatshot_white_48dp;
    }
}
