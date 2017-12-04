package cs.umass.edu.myactivitiestoolkit.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Offers static utility functions for handling application permissions.
 */
public class PermissionsUtil {

    /**
     * Check the specified permissions.
     * @param context The context from which the permission status is being requested.
     * @param permissions list of Strings indicating permissions.
     * @return true if ALL permissions are granted, false otherwise.
     */
    public static boolean hasPermissionsGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
