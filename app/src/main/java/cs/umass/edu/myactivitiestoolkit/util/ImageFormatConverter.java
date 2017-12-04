package cs.umass.edu.myactivitiestoolkit.util;

import android.support.annotation.NonNull;

/**
 * This class offers static conversion methods between image data formats. For this class, you
 * will only need to decode RBG image data from NV21 (YUV420SP).
 */
public class ImageFormatConverter {

    /**
     * Decodes NV21 (YUV420SP) format image data to RGBA values.
     * @param rgba the array into which the RGBA values are written.
     * @param yuv420sp the byte array of YUV240SP data.
     * @param width the width of the camera preview.
     * @param height the height of the camera preview.
     */
    public static void decodeYUV420SP(@NonNull int[] rgba, @NonNull byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgba[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }
}
