package cs.umass.edu.myactivitiestoolkit.audio;

import java.util.Arrays;

import cs.umass.edu.myactivitiestoolkit.processing.FFT;

public class MFCCFeatureExtractor {

	
	private static final int FFT_SIZE = 8192;
	private static final int BITRATE = 8000;
    private static final int MFCCS_VALUE = 12;
    private static final int MEL_BANDS = 20;
    
    private static FFT featureFFT = new FFT(FFT_SIZE);
    private static HammingWindow featureWin = new HammingWindow(BITRATE);
    private static MFCC featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, BITRATE);

    /**
     * Computes the MFCC features over the specified frame of the given data buffer.
     * @param data16bit the data buffer. Each data point is a 16-bit primitive (type short).
     * @param size the size of the frame
     * @param index The index into the data buffer indicating the start of the frame.
     * @return an array of MFCC features
     */
    public static double[] computeFeaturesForFrame(short[] data16bit, int size, int index)
	{
		double[] fftBufferR = new double[FFT_SIZE];
        double[] fftBufferI = new double[FFT_SIZE];
        double[] featureCepstrum;

        // Frequency analysis
        Arrays.fill(fftBufferR, 0);
        Arrays.fill(fftBufferI, 0);

        // Convert audio buffer to doubles
        for (int i = 0; i < size; i++)
        {
                fftBufferR[i] = data16bit[index+i];
        }

        // In-place windowing
        featureWin.applyWindow(fftBufferR);

        // In-place FFT
        featureFFT.fft(fftBufferR, fftBufferI);

        // Get MFCCs
        featureCepstrum = featureMFCC.cepstrum(fftBufferR, fftBufferI);
        
        return featureCepstrum;
	}

}
