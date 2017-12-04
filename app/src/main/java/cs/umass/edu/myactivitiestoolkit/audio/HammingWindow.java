package cs.umass.edu.myactivitiestoolkit.audio;

/**
 * Represents a hamming window, used to ensure smoothness in the FFT signal.
 * For an explanation of why we use a Hamming window, see
 * <a href="http://stackoverflow.com/questions/5418951/what-is-the-hamming-window-for">
 * Gareth McCaughan's response</a>.
 *
 * @author CS390MB
 */
public class HammingWindow
{
        /**
         * The data within the window.
         */
        public double[] window;

    /**
     * The
     */
    public int n;

        public HammingWindow(int windowSize)
        {
                n = windowSize;

                // Make a Hamming window
                window = new double[n];
                for(int i = 0; i < n; i++)
                {
                        window[i] = 0.54 - 0.46*Math.cos(2*Math.PI*(double)i/((double)n-1));
                }
        }

        public void applyWindow(double[] buffer)
        {
                for (int i = 0; i < n; i ++)
                {
                        buffer[i] *= window[i];
                }
        }

}