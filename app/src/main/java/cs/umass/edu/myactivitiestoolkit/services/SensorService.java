package cs.umass.edu.myactivitiestoolkit.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.view.activities.MainActivity;
import edu.umass.cs.MHLClient.client.ConnectionStateHandler;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Base implementation of a foreground sensor service. This includes functionality that is common
 * to all the available services. For instance, the default foreground notification is
 * defined here, but subclasses may override the {@link #getNotification()} method to define a
 * custom notification; alternatively, subclasses may set the notification properties by
 * overriding {@link #getNotificationContentText()}, {@link #getNotificationIconResourceID()}
 * and {@link #getNotificationID()}. This ensures that the only code required in each concrete
 * implementation is relevant to the corresponding sensors and the service related code can be
 * maintained separately. A sensor service is not instantiable.
 */
public abstract class SensorService extends Service implements ConnectionStateHandler {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    protected static final String TAG = SensorService.class.getName();

    /** Responsible for communicating with the data collection server. */
    protected MobileIOClient client;

    /** The user ID required to authenticate the server connection. */
    protected String userID;

    /**
     * Called when the service has been started.
     */
    protected void onServiceStarted(){
        // allow subclasses to execute additional logic after the service has been started
    }

    /**
     * Called when the service has been stopped.
     */
    protected void onServiceStopped(){
        // allow subclasses to execute additional logic after the service has been stopped
    }

    /**
     * Called when the service has been restarted after killed by the OS. Subclasses
     * of {@link SensorService} may want to re-register the sensors and start the
     * service in the foreground again or stop the service if this occurs.
     */
    protected void onServiceRestarted(){
        // allow subclasses to handle the case that the server has been restarted.
    }

    /**
     * Registers the sensors. Subclasses should override this to define how sensors are registered.
     */
    protected abstract void registerSensors();

    /**
     * Unregisters the sensors. Subclasses should override this to define how sensors are unregistered.
     */
    protected abstract void unregisterSensors();

    /**
     * Returns the unique ID of the notification. If a notification with the ID already exists,
     * it will be replaced with the new notification.
     * @return a unique identifier.
     */
    protected abstract int getNotificationID();

    /**
     * Returns content text displayed in the notification.
     * @return the content text of the notification.
     */
    protected abstract String getNotificationContentText();

    /**
     * Returns the resource ID of the notification icon.
     * @return an drawable ID
     */
    protected abstract int getNotificationIconResourceID();

    /**
     * Starts the sensor service in the foreground.
     */
    protected void start(){
        Log.d(TAG, "Service started");
        startForeground(getNotificationID(), getNotification());
        connectToServer();
        registerSensors();
        onServiceStarted();
    }

    /**
     * Stops the sensor service.
     */
    protected void stop(){
        Log.d(TAG, "Service stopped");
        unregisterSensors();

        if (client != null && !ServiceManager.getInstance(this).isSensorServiceRunningExcept(this.getClass())){
            client.disconnect(); //TODO
            Log.d(TAG, "DISCONNECT FROM SERVER");
        }

        stopForeground(true);
        stopSelf();
        onServiceStopped();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            if (intent.getAction().equals(Constants.ACTION.START_SERVICE)) {
                start();
            } else if (intent.getAction().equals(Constants.ACTION.STOP_SERVICE)) {
                stop();
            }
        } else {
            Log.d(TAG, "Service restarted after killed by OS.");
            onServiceRestarted();
        }
        return START_STICKY;
    }

    /**
     * Connects to the data collection server.
     */
    protected void connectToServer(){
        userID = getString(R.string.mobile_health_client_user_id);
        client = MobileIOClient.getInstance(this, userID);
        client.setConnectionStateHandler(this);
        client.connect();
    }

    /**
     * Broadcasts a message to other application components.
     * @param message a message, as defined in {@link Constants.MESSAGE}
     */
    protected void broadcastMessage(int message) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.MESSAGE, message);
        intent.setAction(Constants.ACTION.BROADCAST_MESSAGE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts a status message to other application components. Unlike
     * {@link #broadcastMessage(int)}, you can send an arbitrary string
     * message.
     * @param message a string status message.
     */
    protected void broadcastStatus(String message) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STATUS, message);
        intent.setAction(Constants.ACTION.BROADCAST_STATUS);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Returns the notification displayed during background recording.
     * @return the notification handle.
     */
    protected Notification getNotification(){
        Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
        notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP); //TODO
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(Constants.KEY.NOTIFICATION_ID, getNotificationID());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(this, this.getClass());
        stopIntent.setAction(Constants.ACTION.STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        // notify the user that the foreground service has started
        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(getNotificationContentText())
                .setSmallIcon(getNotificationIconResourceID())
                .setOngoing(true)
                .setVibrate(new long[]{0, 50, 150, 200})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_stop_white_24dp, "Stop Service", stopPendingIntent)
                .setContentIntent(pendingIntent).build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "Connected to server");
    }

    @Override
    public void onConnectionFailed(Exception e) {
        e.printStackTrace();
        Log.d(TAG, "Connection attempt failed.");
    }
}
