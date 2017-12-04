package cs.umass.edu.myactivitiestoolkit.util;

import android.util.Log;

public class Interpolator {
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = Interpolator.class.getName();

    public static double[] linearInterpolate(long[] timestamps, double[] data, int nResampled){
        int n = data.length;
        int k = 1;
        int[] correctedTimestamps = new int[timestamps.length];
        for (int i = 0; i < timestamps.length; i++){
            correctedTimestamps[i] = (int)(timestamps[i] - timestamps[0]);
        }
        int timeElapsed = correctedTimestamps[n - 1] - correctedTimestamps[0];
        double interval = timeElapsed / ((double)nResampled);
        double[] interpolatedData = new double[nResampled];
        for (int i = 0; i < interpolatedData.length; i++){
            long deltaT = correctedTimestamps[k] - correctedTimestamps[k-1];
            double s_i = interval * k;
            if (deltaT == 0) {
                interpolatedData[i]=data[k];
            }else {
                double f1 = (s_i - correctedTimestamps[k - 1]) / deltaT;
                double f2 = (correctedTimestamps[k] - s_i) / deltaT;
                interpolatedData[i] = data[k - 1] * f1 + data[k] * f2;
            }
            while (s_i >= correctedTimestamps[k] && k < n-1)
                k++;
        }
        return interpolatedData;
    }
}