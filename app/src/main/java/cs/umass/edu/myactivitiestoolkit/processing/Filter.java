/**
 * 
 */
package cs.umass.edu.myactivitiestoolkit.processing;

/**
 * <p>This class provides the implementation for Butterworth and Exponential Smoothing Filter</p>
 * 
 * @author cs390mb
 * 
 */
public class Filter {

	private enum FilterType {
		BUTTERWORTH,
		SMOOTHING
	};
	
	private int SAMPLE_RATE = 30;
	
	private FilterType FILTER_TYPE = FilterType.SMOOTHING;
	
	private int SMOOTH_FACTOR = 2;
	
	private double CUTOFF_FREQUENCY = 1.0;
	
	private double ax[] = new double[3];
	private double by[] = new double[3];
	
	private double xv[][] = null;
	private double yv[][] = null;
	
	private double expectedValue[] =null;
	private static final double INVALID = Double.NEGATIVE_INFINITY;
	
	private static final int NUM_ACCEL_FIELDS = 3;

	/**
	 * Use this constructor to use an exponential smoothing filter
	 * @param smoothFactor the factor by which to smooth the data, in the range [0, 1]
	 */
	public Filter(int smoothFactor) {
		FILTER_TYPE = FilterType.SMOOTHING;
		SMOOTH_FACTOR = (smoothFactor>=1?smoothFactor:1);
		expectedValue = new double[NUM_ACCEL_FIELDS];
	}
	
	/**
	 * Use this constructor to use a Butterworth filter.
	 * @param cutoffFrequency the frequency threshold for smoothing.
	 */
	public Filter(double cutoffFrequency) {
		FILTER_TYPE = FilterType.BUTTERWORTH;
		CUTOFF_FREQUENCY = cutoffFrequency;
		xv = new double[NUM_ACCEL_FIELDS][3];
		yv = new double[NUM_ACCEL_FIELDS][3];
		getLPCoefficientsButterworth2Pole(SAMPLE_RATE, CUTOFF_FREQUENCY);
	}
	
	
	/**
	 * Filters the current accelerometer reading.
	 * @param values the accelerometer values along the x, y and z axes
	 * @return the filtered accelerometer values.
	 */
	public double[] getFilteredValues(float... values) {
		double result[] = new double[NUM_ACCEL_FIELDS];
		if(FILTER_TYPE == FilterType.BUTTERWORTH) {
			for (int i = 0; i < values.length; i++){
				result[i] = getButterworthFilteredValue(values[i], i);
			}
//			result[X_INDEX] = getButterworthFilteredValue(values[0], X_INDEX);
//			result[Y_INDEX] = getButterworthFilteredValue(values[1], Y_INDEX);
//			result[Z_INDEX] = getButterworthFilteredValue(values[2], Z_INDEX);
		}
		else if(FILTER_TYPE == FilterType.SMOOTHING) {
			for (int i = 0; i < values.length; i++){
				result[i] = getSmoothedValue(values[i], i);
			}
//			result[X_INDEX] = getSmoothedValue(values[0], X_INDEX);
//			result[Y_INDEX] = getSmoothedValue(values[1], Y_INDEX);
//			result[Z_INDEX] = getSmoothedValue(values[2], Z_INDEX);
		}
		return result;
	}
	
	/**
	 * Filter using butterworth filter
	 * @param sample
	 * @param filterIndex
	 * @return
	 */
	private double getButterworthFilteredValue(double sample, int filterIndex) {
		xv[filterIndex][2] = xv[filterIndex][1]; xv[filterIndex][1] = xv[filterIndex][0];
		xv[filterIndex][0] = sample;
		yv[filterIndex][2] = yv[filterIndex][1]; yv[filterIndex][1] = yv[filterIndex][0];

		yv[filterIndex][0] =   (ax[0] * xv[filterIndex][0] + ax[1] * xv[filterIndex][1] + ax[2] * xv[filterIndex][2]
				- by[1] * yv[filterIndex][0]
						- by[2] * yv[filterIndex][1]);

		return yv[filterIndex][0];
	}
	
	/**
	 * Filter using Smoothing Filter
	 * @param sample
	 * @param filterIndex
	 * @return
	 */
	private double getSmoothedValue(double sample, int filterIndex) {
		if(expectedValue[filterIndex]==INVALID) {
			expectedValue[filterIndex] = sample;
			return expectedValue[filterIndex];
		}
		else {
			expectedValue[filterIndex] += (sample-expectedValue[filterIndex])/SMOOTH_FACTOR;
			return expectedValue[filterIndex];
		}
	}
	
	/**
	 * Get Butterworth 2 Pole LPC Coefficients
	 * @param SAMPLE_RATE
	 * @param cutoff
	 */
	private void getLPCoefficientsButterworth2Pole(int SAMPLE_RATE, double cutoff)
	{
		double PI = 3.1415926535897932385;
		double sqrt2 = 1.4142135623730950488;

		double QcRaw  = (2 * PI * cutoff) / SAMPLE_RATE; // Find cutoff frequency in [0..PI]
		double QcWarp = Math.tan(QcRaw); // Warp cutoff frequency

		double gain = 1 / (1+sqrt2/QcWarp + 2/(QcWarp*QcWarp));
		by[2] = (1 - sqrt2/QcWarp + 2/(QcWarp*QcWarp)) * gain;
		by[1] = (2 - 2 * 2/(QcWarp*QcWarp)) * gain;
		by[0] = 1;
		ax[0] = 1 * gain;
		ax[1] = 2 * gain;
		ax[2] = 1 * gain;
	}
	
	
	
	
	
}
