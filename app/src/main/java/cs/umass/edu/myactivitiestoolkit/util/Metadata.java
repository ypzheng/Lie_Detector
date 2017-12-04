package cs.umass.edu.myactivitiestoolkit.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Utility class for retrieving application metadata such as the current version.
 */
public class Metadata {
    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }
}
