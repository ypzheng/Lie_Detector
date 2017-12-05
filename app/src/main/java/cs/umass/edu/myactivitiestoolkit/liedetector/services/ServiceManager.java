package cs.umass.edu.myactivitiestoolkit.liedetector.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import cs.umass.edu.myactivitiestoolkit.liedetector.constants.Constants;

/**
 * The service manager maintains the application services on the mobile device.
 * It is a singleton class, meaning there is only ever one instance of it.
 *
 * @author Sean Noran
 *
 * @see Service
 * @see SensorService
 */
public class ServiceManager {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = ServiceManager.class.getName();

    /** Singleton instance of the remote sensor manager */
    private static ServiceManager instance;

    /** The application context is used to bind the service manager to the application. **/
    private final Context context;

    /** Returns the singleton instance of the remote sensor manager, instantiating it if necessary. */
    public static synchronized ServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceManager(context.getApplicationContext());
        }

        return instance;
    }

    private ServiceManager(Context context) {
        this.context = context;
    }

    /**
     * Starts the given sensor service.
     * @param serviceClass the reference to the sensor service class.
     * @see SensorService
     */
    public void startSensorService(Class<? extends SensorService> serviceClass){
        Intent startServiceIntent = new Intent(context, serviceClass);
        startServiceIntent.setAction(Constants.ACTION.START_SERVICE);
        context.startService(startServiceIntent);
    }

    /**
     * Stops the given sensor service.
     * @param serviceClass the reference to the sensor service class.
     * @see SensorService
     */
    public void stopSensorService(Class<? extends SensorService> serviceClass){
        Intent startServiceIntent = new Intent(context, serviceClass);
        startServiceIntent.setAction(Constants.ACTION.STOP_SERVICE);
        context.startService(startServiceIntent);
    }

    /**
     * Returns whether the given service is running
     * @param serviceClass a reference to a service class
     * @return true if the service is running, false otherwise
     */
    public boolean isServiceRunning(Class<? extends Service> serviceClass){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether any subclass of {@link SensorService} is running
     * @return true if a sensor service is running, false otherwise
     */
    public boolean isSensorServiceRunningExcept(Class<? extends SensorService> exception){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (!service.service.getClassName().equals(exception.getName()) &&
                    service.service.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
